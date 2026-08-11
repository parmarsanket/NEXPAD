package com.sanket.tools.nexpad.viewmodel

import android.content.Context
import com.sanket.tools.nexpad.model.GamepadFeedback
import com.sanket.tools.nexpad.model.GamepadInput
import com.sanket.tools.nexpad.network.IGamepadConnection
import com.sanket.tools.nexpad.network.NetworkClient
import kotlinx.coroutines.CoroutineScope
import android.net.wifi.WifiManager
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
    USB("USB"),
    BT("Bluetooth"),
    UNKNOWN("Unknown")
}

data class ConnectionStats(
    val transport: ConnectionType = ConnectionType.UNKNOWN,
    val signalDbm: Int? = null,
    val signalLevel: Int? = null, // 0 to 4
    val rxLinkSpeedMbps: Int? = null,
    val txLinkSpeedMbps: Int? = null,
    val latencyMs: Long? = null,
    val jitterMs: Long? = null,
    val packetLossPercent: Float? = null
)



class GamepadNetworkManager(
    private val scope: CoroutineScope,
    private val context: Context,
    private val inputState: GamepadInput
) {
    private var connection: IGamepadConnection = NetworkClient()
    
    private val _feedbackFlow = MutableSharedFlow<GamepadFeedback>()
    val feedbackFlow: SharedFlow<GamepadFeedback> = _feedbackFlow.asSharedFlow()

    private var transmitJob: Job? = null
    
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
                    if (connection is NetworkClient) acquireWifiLock()
                    startTransmitting()
                    startSignalPolling()
                } else {
                    releaseWifiLock()
                    transmitJob?.cancel()
                    signalPollJob?.cancel()
                    _connectionStats.value = ConnectionStats()
                }
            }
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

    private fun detectNetworkType() {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val network = cm.activeNetwork
            val caps = cm.getNetworkCapabilities(network)
            val type = when {
                caps == null -> ConnectionType.WIFI
                caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET) -> ConnectionType.USB
                caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_BLUETOOTH) -> ConnectionType.BT
                else -> ConnectionType.WIFI // Covers Wi-Fi, Cellular, etc.
            }
            _connectionStats.value = _connectionStats.value.copy(transport = type)
        } catch (e: Exception) {
            _connectionStats.value = _connectionStats.value.copy(transport = ConnectionType.WIFI)
        }
    }

    fun connect(address: String, port: Int) {
        detectNetworkType()
        scope.launch {
            try {
                connection.connect(address, port)
            } catch (e: Exception) {
                e.printStackTrace()
                _isConnected.value = false
                _connectionStatus.value = e.message ?: "Connection failed"
            }
        }
    }
    
    fun disconnect() {
        transmitJob?.cancel()
        signalPollJob?.cancel()
        releaseWifiLock()
        connection.disconnect()
        _isConnected.value = false
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
            
            val intervalNanos = 5_000_000L // 200Hz = 5ms
            var nextTick = System.nanoTime()
            
            while (isActive) {
                connection.sendInput(inputState)
                
                nextTick += intervalNanos
                val sleepNanos = nextTick - System.nanoTime()
                
                if (sleepNanos > 0) {
                    delay(sleepNanos / 1_000_000L) // Convert nanos to millis
                } else {
                    nextTick = System.nanoTime() // fell behind — resync, don't stack debt
                }
            }
        }
    }

    private var wifiLock: android.net.wifi.WifiManager.WifiLock? = null

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
        } catch (e: Exception) {
            android.util.Log.e("NEXPAD", "❌ Failed to acquire WifiLock: ${e.message}")
        }
    }

    private fun releaseWifiLock() {
        try {
            if (wifiLock?.isHeld == true) {
                wifiLock?.release()
                android.util.Log.d("NEXPAD", "🔓 WifiLock Released")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
