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

class GamepadNetworkManager(
    private val scope: CoroutineScope,
    private val context: Context,
    private val getInputState: () -> GamepadInput
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
                    startTransmitting()
                } else {
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
    
    private fun startTransmitting() {
        transmitJob?.cancel()
        transmitJob = scope.launch {
            while (isActive) {
                connection.sendInput(getInputState())
                // 60Hz transmission rate (1000ms / 60 = ~16.6ms)
                delay(16L) 
            }
        }
    }
}
