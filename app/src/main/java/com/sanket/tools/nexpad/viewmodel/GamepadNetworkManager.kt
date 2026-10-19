package com.sanket.tools.nexpad.viewmodel

import android.content.Context
import com.sanket.tools.nexpad.utils.AppLog as Log
import com.sanket.tools.nexpad.model.GamepadFeedback
import com.sanket.tools.nexpad.model.GamepadInput
import com.sanket.tools.nexpad.network.IGamepadConnection
import com.sanket.tools.nexpad.network.NetworkClient
import com.sanket.tools.nexpad.network.AoaAccessoryConnection
import com.sanket.tools.nexpad.network.NxprcSyncServer
import java.util.concurrent.RejectedExecutionException
import kotlinx.coroutines.CoroutineScope
import android.net.wifi.WifiManager
import android.os.PowerManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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
    val latencyFloatMs: Float? = null,
    val jitterMs: Float? = null,
    val packetLossPercent: Float? = null
) {
    /**
     * Real One-Way Controller Input Lag (Phone -> PC).
     * Represents the time for a button/stick input to reach Windows and register in-game.
     * Approximated as half of Round-Trip Ping (RTT).
     */
    val inputLagMs: Long?
        get() {
            val rtt = latencyFloatMs ?: latencyMs?.toFloat() ?: return null
            val oneWay = rtt / 2f
            return if (oneWay < 1.0f) 0L else kotlin.math.round(oneWay).toLong().coerceAtLeast(1L)
        }

    val displayPing: String
        get() {
            val rtt = latencyFloatMs ?: latencyMs?.toFloat() ?: return "--"
            return if (rtt < 1.0f) String.format(java.util.Locale.US, "%.1f", rtt)
            else String.format(java.util.Locale.US, "%.0f", rtt)
        }

    val displayInputLag: String
        get() {
            val rtt = latencyFloatMs ?: latencyMs?.toFloat() ?: return "--"
            val oneWay = rtt / 2f
            return if (oneWay < 1.0f) String.format(java.util.Locale.US, "%.1f", oneWay)
            else String.format(java.util.Locale.US, "%.0f", oneWay)
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
    @Volatile
    private var isClosed = false
    @Volatile
    private var connection: IGamepadConnection = NetworkClient()
    @Volatile
    private var transmitThread: Thread? = null
    private val syncServer = NxprcSyncServer(context)

    init {
        syncServer.start(scope)
    }
    
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
            Log.w("NEXPAD", "Refusing ADB switch: USB Debugging is OFF.")
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

    fun getPairedBluetoothDevices(): List<android.bluetooth.BluetoothDevice> {
        val permission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            android.Manifest.permission.BLUETOOTH_CONNECT
        } else {
            android.Manifest.permission.BLUETOOTH
        }
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return emptyList()
        }
        val bluetoothManager = context.getSystemService(android.bluetooth.BluetoothManager::class.java)
        val adapter = bluetoothManager?.adapter
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
    
    private val _feedbackFlow = MutableSharedFlow<GamepadFeedback>(extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)
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
            _feedbackFlow.tryEmit(feedback)
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
                    _connectionStatus.value = "Disconnected"
                    resetInputState()
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
                latencyMs = kotlin.math.round(latencyMs).toLong().coerceAtLeast(0L),
                latencyFloatMs = latencyMs,
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
        if (serverName != null) {
            _connectedServerName.value = serverName
        }

        // IMMEDIATELY acquire WifiLock & WakeLock so the Wi-Fi radio does not enter 802.11 power-save sleep
        acquireWifiLock()

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

        scope.launch {
            try {
                connection.connect(address, port)
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
        transmitThread = null
        signalPollJob?.cancel()
        signalPollJob = null
        releaseWifiLock()
        connection.disconnect()
        _isConnected.value = false
        _connectionStats.value = ConnectionStats()
        _connectionStatus.value = "Disconnected"
        resetInputState()
    }

    fun close() {
        isClosed = true
        transmitJob?.cancel()
        transmitJob = null
        transmitThread = null
        signalPollJob?.cancel()
        signalPollJob = null
        releaseWifiLock()
        connection.close()
        syncServer.stop()
        _isConnected.value = false
        resetInputState()
        gamepadDispatcher.close()
    }

    fun sendImmediate() {
        if (isClosed || !_isConnected.value) return
        val thread = transmitThread
        if (thread != null) {
            java.util.concurrent.locks.LockSupport.unpark(thread)
        } else {
            try {
                scope.launch(gamepadDispatcher) {
                    try {
                        connection.sendInput(inputState)
                    } catch (_: Exception) {
                        // Ignore silent drops
                    }
                }
            } catch (e: RejectedExecutionException) {
                return
            }
        }
    }

    private fun resetInputState() {
        inputState.btnA = false
        inputState.btnB = false
        inputState.btnX = false
        inputState.btnY = false
        inputState.dpadUp = false
        inputState.dpadDown = false
        inputState.dpadLeft = false
        inputState.dpadRight = false
        inputState.btnL1 = false
        inputState.btnR1 = false
        inputState.btnL3 = false
        inputState.btnR3 = false
        inputState.btnStart = false
        inputState.btnSelect = false
        inputState.btnGuide = false
        inputState.btnShare = false
        inputState.btnScreenshot = false
        inputState.btnM1 = false
        inputState.btnM2 = false
        inputState.btnM3 = false
        inputState.btnM4 = false
        inputState.btnProfile = false
        inputState.btnTurbo = false
        inputState.triggerL2 = 0f
        inputState.triggerR2 = 0f
        inputState.leftStickX = 0f
        inputState.leftStickY = 0f
        inputState.rightStickX = 0f
        inputState.rightStickY = 0f
        inputState.gyroX = 0f
        inputState.gyroY = 0f
        inputState.gyroZ = 0f
        inputState.accelX = 0f
        inputState.accelY = 0f
        inputState.accelZ = 0f
        inputState.gravityX = 0f
        inputState.gravityY = 0f
        inputState.gravityZ = 0f
        inputState.quatX = 0f
        inputState.quatY = 0f
        inputState.quatZ = 0f
        inputState.quatW = 1f
        inputState.linearAccelX = 0f
        inputState.linearAccelY = 0f
        inputState.linearAccelZ = 0f
        inputState.rawGyroX = 0f
        inputState.rawGyroY = 0f
        inputState.rawGyroZ = 0f
        inputState.gyroBiasX = 0f
        inputState.gyroBiasY = 0f
        inputState.gyroBiasZ = 0f
        inputState.sensorFlags = 0
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
                        val cm = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
                        val activeNet = cm?.activeNetwork
                        val caps = activeNet?.let { cm.getNetworkCapabilities(it) }
                        val wifiInfo = caps?.transportInfo as? android.net.wifi.WifiInfo
                        val rssi = wifiInfo?.rssi ?: -127
                        
                        if (rssi > -100 && rssi != -127) { // Valid hardware RSSI
                            val signalLevel = when {
                                rssi >= -50 -> 4
                                rssi >= -60 -> 3
                                rssi >= -67 -> 2
                                rssi >= -75 -> 1
                                else -> 0
                            }
                            
                            var rxSpeed: Int? = wifiInfo?.rxLinkSpeedMbps?.takeIf { it != -1 }
                            var txSpeed: Int? = wifiInfo?.txLinkSpeedMbps?.takeIf { it != -1 }
                            // Fallback if link speed is reported via legacy field
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
            transmitThread = Thread.currentThread()
            try {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO)
                
                // Bluetooth Classic ACL 6-slot timing aligns optimally at 125 Hz (8ms).
                // USB (ADB/AOA) and Wi-Fi run at full 200 Hz (5ms).
                val intervalNanos = if (connection is com.sanket.tools.nexpad.network.BluetoothRfcommConnection) 8_000_000L else 5_000_000L
                
                while (isActive) {
                    connection.sendInput(inputState)
                    
                    val nextTick = System.nanoTime() + intervalNanos
                    val sleepNanos = nextTick - System.nanoTime()
                    
                    if (sleepNanos > 0) {
                        java.util.concurrent.locks.LockSupport.parkNanos(sleepNanos)
                    }
                }
            } finally {
                transmitThread = null
            }
        }
    }

    private var wifiLock: android.net.wifi.WifiManager.WifiLock? = null
    private var wakeLock: android.os.PowerManager.WakeLock? = null

    private fun acquireWifiLock() {
        if (wifiLock?.isHeld == true) return
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
            wifiLock = wifiManager.createWifiLock(
                android.net.wifi.WifiManager.WIFI_MODE_FULL_LOW_LATENCY,
                "Nexpad:LowLatencyLock"
            ).apply {
                setReferenceCounted(false)
                acquire()
            }
            Log.d("NEXPAD", "🔒 WifiLock Acquired: WIFI_MODE_FULL_LOW_LATENCY")
            
            if (wakeLock?.isHeld != true) {
                val powerManager = context.applicationContext.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
                wakeLock = powerManager.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "Nexpad:WakeLock").apply {
                    setReferenceCounted(false)
                    acquire(10 * 60 * 1000L)
                }
                Log.d("NEXPAD", "🔒 WakeLock Acquired (Partial, 10-min safety timeout)")
            }
        } catch (e: Exception) {
            Log.e("NEXPAD", "❌ Failed to acquire locks: ${e.message}")
        }
    }

    fun refreshWakeLock(timeoutMs: Long = 10 * 60 * 1000L) {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
            val powerManager = context.applicationContext.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            wakeLock = powerManager.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "Nexpad:WakeLock").apply {
                setReferenceCounted(false)
                acquire(timeoutMs)
            }
            Log.d("NEXPAD", "🔒 WakeLock Refreshed (Partial, ${timeoutMs / 60000}m timeout)")
        } catch (e: Exception) {
            Log.e("NEXPAD", "❌ Failed to refresh WakeLock: ${e.message}")
        }
    }

    private fun releaseWifiLock() {
        try {
            if (wifiLock?.isHeld == true) {
                wifiLock?.release()
                Log.d("NEXPAD", "🔓 WifiLock Released")
            }
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.d("NEXPAD", "🔓 WakeLock Released")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
