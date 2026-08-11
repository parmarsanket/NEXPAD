package com.sanket.tools.nexpad.viewmodel

import android.content.Context
import com.sanket.tools.nexpad.bluetooth.BluetoothClient
import com.sanket.tools.nexpad.model.GamepadFeedback
import com.sanket.tools.nexpad.model.GamepadInput
import com.sanket.tools.nexpad.network.IGamepadConnection
import com.sanket.tools.nexpad.network.NetworkClient
import kotlinx.coroutines.CoroutineScope
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
                } else {
                    releaseWifiLock()
                    transmitJob?.cancel()
                }
            }
        }
        connection.onStatusChanged = { status ->
            _connectionStatus.value = status
        }
        connection.onDiagnosticLog = { entry ->
            _diagnosticLog.value = (_diagnosticLog.value + entry).takeLast(20)
        }
    }

    fun setConnectionMode(isBluetooth: Boolean) {
        transmitJob?.cancel()
        connection.onFeedbackReceived = null
        connection.onConnectionStateChanged = null
        connection.onStatusChanged = null
        connection.onDiagnosticLog = null
        connection.close()
        _isConnected.value = false
        _diagnosticLog.value = emptyList()

        connection = if (isBluetooth) {
            BluetoothClient(context)
        } else {
            NetworkClient()
        }
        setupConnectionCallbacks()
    }

    fun connect(address: String, port: Int) {
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
        connection.disconnect()
        _isConnected.value = false
    }

    fun startAdvertising() {
        connection.startAdvertising()
    }

    fun close() {
        transmitJob?.cancel()
        connection.close()
        _isConnected.value = false
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
