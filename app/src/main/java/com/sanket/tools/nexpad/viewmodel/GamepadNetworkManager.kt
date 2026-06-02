package com.sanket.tools.nexpad.viewmodel

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
    private val getInputState: () -> GamepadInput
) {
    private val connection: IGamepadConnection = NetworkClient()
    
    private val _feedbackFlow = MutableSharedFlow<GamepadFeedback>()
    val feedbackFlow: SharedFlow<GamepadFeedback> = _feedbackFlow.asSharedFlow()

    private var transmitJob: Job? = null
    
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    init {
        connection.onFeedbackReceived = { feedback: GamepadFeedback ->
            scope.launch {
                _feedbackFlow.emit(feedback)
            }
        }
    }

    fun connect(ip: String, port: Int) {
        scope.launch {
            try {
                connection.connect(ip, port)
                _isConnected.value = true
                startTransmitting()
            } catch (e: Exception) {
                e.printStackTrace()
                _isConnected.value = false
            }
        }
    }
    
    fun disconnect() {
        transmitJob?.cancel()
        connection.disconnect()
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
