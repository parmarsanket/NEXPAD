package com.sanket.tools.nexpad.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanket.tools.nexpad.model.GamepadFeedback
import com.sanket.tools.nexpad.model.GamepadInput
import com.sanket.tools.nexpad.network.NetworkClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

import com.sanket.tools.nexpad.network.IGamepadConnection

class GamepadViewModel : ViewModel() {
    private val connection: IGamepadConnection = NetworkClient()
    
    private val _inputState = MutableStateFlow(GamepadInput())
    val inputState: StateFlow<GamepadInput> = _inputState.asStateFlow()

    private val _feedbackFlow = MutableSharedFlow<GamepadFeedback>()
    val feedbackFlow: SharedFlow<GamepadFeedback> = _feedbackFlow.asSharedFlow()

    init {
        connection.onFeedbackReceived = { feedback: GamepadFeedback ->
            viewModelScope.launch {
                _feedbackFlow.emit(feedback)
            }
        }
    }

    private var transmitJob: Job? = null
    
    fun connect(ip: String, port: Int) {
        viewModelScope.launch {
            connection.connect(ip, port)
            startTransmitting()
        }
    }
    
    fun disconnect() {
        transmitJob?.cancel()
        connection.disconnect()
    }
    
    private fun startTransmitting() {
        transmitJob?.cancel()
        transmitJob = viewModelScope.launch {
            while (isActive) {
                connection.sendInput(_inputState.value)
                // 60Hz transmission rate (1000ms / 60 = ~16.6ms)
                delay(16L) 
            }
        }
    }

    fun updateButton(buttonName: String, isPressed: Boolean) {
        _inputState.update { current ->
            when (buttonName) {
                "A" -> current.copy(btnA = isPressed)
                "B" -> current.copy(btnB = isPressed)
                "X" -> current.copy(btnX = isPressed)
                "Y" -> current.copy(btnY = isPressed)
                "UP" -> current.copy(dpadUp = isPressed)
                "DOWN" -> current.copy(dpadDown = isPressed)
                "LEFT" -> current.copy(dpadLeft = isPressed)
                "RIGHT" -> current.copy(dpadRight = isPressed)
                "L1" -> current.copy(btnL1 = isPressed)
                "R1" -> current.copy(btnR1 = isPressed)
                "L2" -> current.copy(triggerL2 = if (isPressed) 1f else 0f)
                "R2" -> current.copy(triggerR2 = if (isPressed) 1f else 0f)
                "L3" -> current.copy(btnL3 = isPressed)
                "R3" -> current.copy(btnR3 = isPressed)
                "START" -> current.copy(btnStart = isPressed)
                "SELECT" -> current.copy(btnSelect = isPressed)
                "GUIDE" -> current.copy(btnGuide = isPressed)
                else -> current
            }
        }
    }

    fun updateLeftStick(x: Float, y: Float) {
        _inputState.update { it.copy(leftStickX = x, leftStickY = y) }
    }

    fun updateRightStick(x: Float, y: Float) {
        _inputState.update { it.copy(rightStickX = x, rightStickY = y) }
    }

    val isGyroSteeringEnabled = MutableStateFlow(false)

    fun toggleGyroSteering() {
        isGyroSteeringEnabled.value = !isGyroSteeringEnabled.value
    }

    fun updateGyro(x: Float, y: Float, z: Float) {
        _inputState.update { 
            // Max tilt for full steering lock. 6.0 m/s^2 is ~40 degrees tilt.
            val maxTilt = 6.0f
            // Y-axis gravity points towards the floor when turning the phone.
            val rawSteering = (y / maxTilt).coerceIn(-1.0f, 1.0f)
            
            val newState = it.copy(gyroX = x, gyroY = y, gyroZ = z)
            if (isGyroSteeringEnabled.value) {
                // User requested raw 1:1 input without anti-deadzone
                newState.leftStickX = rawSteering
            }
            newState
        }
    }
}
