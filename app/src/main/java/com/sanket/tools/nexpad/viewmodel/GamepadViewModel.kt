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
    
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    
    fun connect(ip: String, port: Int) {
        viewModelScope.launch {
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
                "LB" -> current.copy(btnL1 = isPressed)
                "RB" -> current.copy(btnR1 = isPressed)
                "LT" -> current.copy(triggerL2 = if (isPressed) 1f else 0f)
                "RT" -> current.copy(triggerR2 = if (isPressed) 1f else 0f)
                "L3" -> current.copy(btnL3 = isPressed)
                "R3" -> current.copy(btnR3 = isPressed)
                "MENU" -> current.copy(btnStart = isPressed)
                "VIEW" -> current.copy(btnSelect = isPressed)
                "XBOX" -> current.copy(btnGuide = isPressed)
                "SHARE" -> current.copy(btnShare = isPressed)
                "SCREENSHOT" -> current.copy(btnScreenshot = isPressed)
                "M1" -> current.copy(btnM1 = isPressed)
                "M2" -> current.copy(btnM2 = isPressed)
                "M3" -> current.copy(btnM3 = isPressed)
                "M4" -> current.copy(btnM4 = isPressed)
                "PROFILE" -> current.copy(btnProfile = isPressed)
                "TURBO" -> current.copy(btnTurbo = isPressed)
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
    val isGyroInverted = MutableStateFlow(false)
    val is6AxisEnabled = MutableStateFlow(false)

    fun toggleGyroSteering() {
        isGyroSteeringEnabled.value = !isGyroSteeringEnabled.value
    }

    fun update2DSteering(x: Float, y: Float, z: Float) {
        // Only run 2D steering if 6-axis is disabled
        if (!is6AxisEnabled.value) {
            _inputState.update { 
                val maxTilt = 6.0f
                val multiplier = if (isGyroInverted.value) 1f else -1f
                val rawSteering = ((y * multiplier) / maxTilt).coerceIn(-1.0f, 1.0f)
                
                val newState = it.copy()
                if (isGyroSteeringEnabled.value) {
                    newState.leftStickX = rawSteering
                }
                newState
            }
        }
    }

    fun updateAccel(x: Float, y: Float, z: Float) {
        if (is6AxisEnabled.value) {
            _inputState.update { it.copy(accelX = x, accelY = y, accelZ = z) }
        }
    }

    fun update6AxisGyro(x: Float, y: Float, z: Float) {
        if (is6AxisEnabled.value) {
            _inputState.update { it.copy(gyroX = x, gyroY = y, gyroZ = z) }
        }
    }
}
