package com.sanket.tools.nexpad.viewmodel

import android.content.Context
import com.sanket.tools.nexpad.model.GamepadFeedback
import com.sanket.tools.nexpad.model.GamepadInput
import com.sanket.tools.nexpad.network.IGamepadConnection
import com.sanket.tools.nexpad.network.NetworkClient
import com.sanket.tools.nexpad.network.AoaAccessoryConnection
import kotlinx.coroutines.CoroutineScope
import android.net.wifi.WifiManager
import android.os.PowerManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.asCoroutineDispatcher
enum class ConnectionType(val displayName: String) {
    WIFI("Wi-Fi"),
    USB_TETHERING("USB Tethering"),
    USB("USB (Direct)"),
    BT("Bluetooth"),
    UNKNOWN("Unknown")
}

data class ConnectionStats(
    val transport: ConnectionType = ConnectionType.UNKNOWN,
    val serverDeviceName: String? = null,
    val signalDbm: Int? = null,
    val signalLevel: Int? = null, // 0 to 4
    val rxLinkSpeedMbps: Int? = null,
    val txLinkSpeedMbps: Int? = null,
    val latencyMs: Long? = null,
    val jitterMs: Float? = null,
    val packetLossPercent: Float? = null
) {
    /**
     * Real One-Way Controller Input Lag (Phone -> PC).
     * Represents the time for a button/stick input to reach Windows and register in-game.
     * Approximated as half of Round-Trip Ping (RTT).
     */
    val inputLagMs: Long?
        get() = latencyMs?.let { rtt ->
            if (rtt <= 0L) 1L else ((rtt + 1) / 2).coerceAtLeast(1L)
        }

    /**
     * Real One-Way Input Jitter (Phone -> PC).
     * Approximated as half of Round-Trip Jitter variance.
     */
    val oneWayJitterMs: Float?
        get() = jitterMs?.let { j ->
            (j / 2f).coerceAtLeast(0f)
        }
}



class GamepadNetworkManager(
    private val scope: CoroutineScope,
    private val context: Context,
    private val inputState: GamepadInput
) {
    private var connection: IGamepadConnection = NetworkClient()
    
    private fun isUsbDebuggingEnabled(): Boolean {
        return try {
            android.provider.Settings.Global.getInt(
                context.contentResolver,
                android.provider.Settings.Global.ADB_ENABLED,
                0
            ) == 1
        } catch (_: Exception) {
            false
        }
    }

    fun switchToAoaConnection() {
        disconnect()
        connection.close()
        connection = AoaAccessoryConnection(context)
        _connectedServerName.value = "NEXPAD PC (Direct USB)"
        _connectionStats.value = _connectionStats.value.copy(transport = ConnectionType.USB, serverDeviceName = "NEXPAD PC (Direct USB)")
        setupConnectionCallbacks()
        scope.launch { connection.connect("aoa", 0) }
    }

    fun switchToAdbConnection(serverName: String? = null) {
        val resolvedName = serverName ?: "PC via USB ADB"
        if (!isUsbDebuggingEnabled()) {
            android.util.Log.w("NEXPAD", "Refusing ADB switch: USB Debugging is OFF.")
            _connectionStatus.value = "USB Debugging is OFF"
            return
        }
        disconnect()
        connection.close()
        connection = com.sanket.tools.nexpad.network.AdbBridgeConnection(context)
        _connectedServerName.value = resolvedName
        _connectionStats.value = _connectionStats.value.copy(transport = ConnectionType.USB, serverDeviceName = resolvedName)
        setupConnectionCallbacks()
        scope.launch { connection.connect("adb", 0) }
    }
    
    fun switchToUdpConnection() {
        disconnect()
        connection.close()
        connection = NetworkClient()
        _connectionStats.value = _connectionStats.value.copy(transport = ConnectionType.WIFI)
        setupConnectionCallbacks()
    }

    @android.annotation.SuppressLint("MissingPermission")
    fun getPairedBluetoothDevices(): List<android.bluetooth.BluetoothDevice> {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
        val adapter = bluetoothManager?.adapter ?: android.bluetooth.BluetoothAdapter.getDefaultAdapter()
        return if (adapter != null && adapter.isEnabled) {
            adapter.bondedDevices?.toList() ?: emptyList()
        } else {
            emptyList()
        }
    }

    fun switchToBluetoothConnection(deviceAddress: String, deviceName: String? = null) {
        disconnect()
        connection.close()
        connection = com.sanket.tools.nexpad.network.BluetoothRfcommConnection(context)
        _connectedServerName.value = deviceName
        _connectionStats.value = _connectionStats.value.copy(transport = ConnectionType.BT, serverDeviceName = deviceName)
        setupConnectionCallbacks()
        scope.launch { connection.connect(deviceAddress, 0) }
    }
    
    private val _feedbackFlow = MutableSharedFlow<GamepadFeedback>()
    val feedbackFlow: SharedFlow<GamepadFeedback> = _feedbackFlow.asSharedFlow()

    private var transmitJob: Job? = null
    
    private val _connectedServerName = MutableStateFlow<String?>(null)
    val connectedServerName: StateFlow<String?> = _connectedServerName.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _connectionStatus = MutableStateFlow("Disconnected")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    private val _connectionStats = MutableStateFlow(ConnectionStats())
    val connectionStats: StateFlow<ConnectionStats> = _connectionStats.asStateFlow()

    private val _diagnosticLog = MutableStateFlow<List<String>>(emptyList())
    val diagnosticLog: StateFlow<List<String>> = _diagnosticLog.asStateFlow()

    init {
        setupConnectionCallbacks()
    }

    private fun setupConnectionCallbacks() {
        connection.onFeedbackReceived = { feedback: GamepadFeedback ->
            scope.launch {
                _feedbackFlow.emit(feedback)
            }
        }
        connection.onConnectionStateChanged = { connected: Boolean ->
            scope.launch {
                _isConnected.value = connected
                if (connected) {
                    if (connection is NetworkClient) {
                        acquireWifiLock()
                        if (_connectionStats.value.transport == ConnectionType.UNKNOWN) {
                            _connectionStats.value = _connectionStats.value.copy(transport = ConnectionType.WIFI)
                        }
                    }
                    startTransmitting()
                    startSignalPolling()
                } else {
                    releaseWifiLock()
                    transmitJob?.cancel()
                    signalPollJob?.cancel()
                    _connectedServerName.value = null
                    _connectionStats.value = ConnectionStats()
                }
            }
        }
        connection.onServerNameResolved = { name: String ->
            _connectedServerName.value = name
            _connectionStats.value = _connectionStats.value.copy(serverDeviceName = name)
        }
        connection.onStatusChanged = { status ->
            _connectionStatus.value = status
        }
        connection.onDiagnosticLog = { entry ->
            _diagnosticLog.value = (_diagnosticLog.value + entry).takeLast(20)
        }
        connection.onNetworkPerformanceUpdated = { latencyMs, jitterMs, packetLoss ->
            _connectionStats.value = _connectionStats.value.copy(
                latencyMs = latencyMs,
                jitterMs = jitterMs,
                packetLossPercent = packetLoss
            )
        }
    }

    private fun detectNetworkType(address: String? = null) {
        if (address != null && com.sanket.tools.nexpad.network.NetworkInterfaceHelper.isUsbTetheringAddress(address)) {
            _connectionStats.value = _connectionStats.value.copy(transport = ConnectionType.USB_TETHERING)
            return
        }
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val network = cm.activeNetwork
            val caps = cm.getNetworkCapabilities(network)
            val type = when {
                caps == null -> ConnectionType.WIFI
                caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET) -> ConnectionType.USB_TETHERING
                caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_BLUETOOTH) -> ConnectionType.BT
                else -> ConnectionType.WIFI // Covers Wi-Fi, Cellular, etc.
            }
            _connectionStats.value = _connectionStats.value.copy(transport = type)
        } catch (e: Exception) {
            _connectionStats.value = _connectionStats.value.copy(transport = ConnectionType.WIFI)
        }
    }

    fun connect(address: String, port: Int, serverName: String? = null) {
        val t0 = android.os.SystemClock.elapsedRealtime()
        android.util.Log.d("NEXPAD", "⏱️ [BENCHMARK] Step 0: GamepadNetworkManager.connect CALLED for $address:$port ($serverName) on thread ${Thread.currentThread().name}")
        if (serverName != null) {
            _connectedServerName.value = serverName
        }

        // IMMEDIATELY acquire WifiLock & WakeLock so the Wi-Fi radio does not enter 802.11 power-save sleep
        acquireWifiLock()
        android.util.Log.d("NEXPAD", "⏱️ [BENCHMARK] WifiLock & WakeLock acquired immediately at t=${android.os.SystemClock.elapsedRealtime() - t0}ms")

        val isTethering = com.sanket.tools.nexpad.network.NetworkInterfaceHelper.isUsbTetheringAddress(address)
        val initialType = if (isTethering) ConnectionType.USB_TETHERING else ConnectionType.WIFI
        _connectionStats.value = _connectionStats.value.copy(transport = initialType, serverDeviceName = serverName)
        if (connection !is NetworkClient) {
            disconnect()
            connection.close()
            connection = NetworkClient()
            setupConnectionCallbacks()
        }
        detectNetworkType(address)

        (connection as? NetworkClient)?.benchmarkStartTimeMs = t0

        scope.launch {
            try {
                android.util.Log.d("NEXPAD", "⏱️ [BENCHMARK] Calling connection.connect($address, $port) at t=${android.os.SystemClock.elapsedRealtime() - t0}ms")
                connection.connect(address, port)
                android.util.Log.d("NEXPAD", "⏱️ [BENCHMARK] connection.connect($address, $port) returned at t=${android.os.SystemClock.elapsedRealtime() - t0}ms")
            } catch (e: Exception) {
                e.printStackTrace()
                releaseWifiLock()
                _isConnected.value = false
                _connectionStatus.value = e.message ?: "Connection failed"
            }
        }
    }
    
    fun disconnect() {
        transmitJob?.cancel()
        transmitJob = null
        signalPollJob?.cancel()
        signalPollJob = null
        releaseWifiLock()
        connection.disconnect()
        _isConnected.value = false
        _connectionStats.value = ConnectionStats()
        _connectionStatus.value = "Disconnected"
    }

    fun close() {
        transmitJob?.cancel()
        signalPollJob?.cancel()
        releaseWifiLock()
        connection.close()
        _isConnected.value = false
        gamepadDispatcher.close()
    }

    fun sendImmediate() {
        if (_isConnected.value) {
            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    connection.sendInput(inputState)
                } catch (e: Exception) {
                    // Ignore silent drops
                }
            }
        }
    }
    
    private val gamepadDispatcher = java.util.concurrent.Executors.newSingleThreadExecutor { r ->
        Thread(r, "GamepadTransmit")
    }.asCoroutineDispatcher()

    private var signalPollJob: Job? = null

    private fun startSignalPolling() {
        signalPollJob?.cancel()
        signalPollJob = scope.launch {
            while (isActive) {
                if (_connectionStats.value.transport == ConnectionType.WIFI) {
                    try {
                        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
                        val wifiInfo = wifiManager?.connectionInfo
                        val rssi = wifiInfo?.rssi ?: -127
                        
                        if (rssi > -100 && rssi != -127) { // Valid hardware RSSI
                            val signalLevel = when {
                                rssi >= -50 -> 4
                                rssi >= -60 -> 3
                                rssi >= -67 -> 2
                                rssi >= -75 -> 1
                                else -> 0
                            }
                            
                            var rxSpeed: Int? = null
                            var txSpeed: Int? = null
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                rxSpeed = wifiInfo?.rxLinkSpeedMbps
                                txSpeed = wifiInfo?.txLinkSpeedMbps
                                if (rxSpeed == -1) rxSpeed = null
                                if (txSpeed == -1) txSpeed = null
                            }
                            // Fallback for older devices or if Q API fails
                            if (rxSpeed == null && wifiInfo?.linkSpeed != -1) {
                                rxSpeed = wifiInfo?.linkSpeed
                                txSpeed = wifiInfo?.linkSpeed
                            }
                            
                            _connectionStats.value = _connectionStats.value.copy(
                                signalDbm = rssi,
                                signalLevel = signalLevel,
                                rxLinkSpeedMbps = rxSpeed,
                                txLinkSpeedMbps = txSpeed
                            )
                        } else {
                            // Permission likely denied or on a Mobile Hotspot
                            _connectionStats.value = _connectionStats.value.copy(
                                signalDbm = null,
                                signalLevel = null,
                                rxLinkSpeedMbps = null,
                                txLinkSpeedMbps = null
                            )
                        }
                    } catch (e: SecurityException) {
                        _connectionStats.value = _connectionStats.value.copy(
                            signalDbm = null,
                            signalLevel = null,
                            rxLinkSpeedMbps = null,
                            txLinkSpeedMbps = null
                        )
                    }
                } else {
                    _connectionStats.value = _connectionStats.value.copy(
                        signalDbm = null,
                        signalLevel = null,
                        rxLinkSpeedMbps = null,
                        txLinkSpeedMbps = null
                    )
                }
                delay(2000)
            }
        }
    }

    private fun startTransmitting() {
        transmitJob?.cancel()
        transmitJob = scope.launch(gamepadDispatcher) {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO)
            
            // Bluetooth Classic ACL 6-slot timing aligns optimally at 125 Hz (8ms).
            // USB (ADB/AOA) and Wi-Fi run at full 200 Hz (5ms).
            val intervalNanos = if (connection is com.sanket.tools.nexpad.network.BluetoothRfcommConnection) 8_000_000L else 5_000_000L
            var nextTick = System.nanoTime()
            
            while (isActive) {
                connection.sendInput(inputState)
                
                nextTick += intervalNanos
                val sleepNanos = nextTick - System.nanoTime()
                
                if (sleepNanos > 0) {
                    java.util.concurrent.locks.LockSupport.parkNanos(sleepNanos)
                } else {
                    nextTick = System.nanoTime() // fell behind — resync, don't stack debt
                }
            }
        }
    }

    private var wifiLock: android.net.wifi.WifiManager.WifiLock? = null
    private var wakeLock: android.os.PowerManager.WakeLock? = null

    @Suppress("DEPRECATION")
    private fun acquireWifiLock() {
        if (wifiLock?.isHeld == true) return
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
            val lockMode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                android.net.wifi.WifiManager.WIFI_MODE_FULL_LOW_LATENCY
            } else {
                android.net.wifi.WifiManager.WIFI_MODE_FULL_HIGH_PERF
            }
            wifiLock = wifiManager.createWifiLock(lockMode, "Nexpad:LowLatencyLock").apply {
                setReferenceCounted(false)
                acquire()
            }
            android.util.Log.d("NEXPAD", "🔒 WifiLock Acquired: Mode $lockMode")
            
            if (wakeLock?.isHeld != true) {
                val powerManager = context.applicationContext.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
                wakeLock = powerManager.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "Nexpad:WakeLock").apply {
                    setReferenceCounted(false)
                    acquire()
                }
                android.util.Log.d("NEXPAD", "🔒 WakeLock Acquired (Partial)")
            }
        } catch (e: Exception) {
            android.util.Log.e("NEXPAD", "❌ Failed to acquire locks: ${e.message}")
        }
    }

    private fun releaseWifiLock() {
        try {
            if (wifiLock?.isHeld == true) {
                wifiLock?.release()
                android.util.Log.d("NEXPAD", "🔓 WifiLock Released")
            }
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                android.util.Log.d("NEXPAD", "🔓 WakeLock Released")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
