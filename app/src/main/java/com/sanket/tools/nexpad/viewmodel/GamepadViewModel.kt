package com.sanket.tools.nexpad.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanket.tools.nexpad.model.GamepadInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class GamepadViewModel : ViewModel() {
    private val _inputState = MutableStateFlow(GamepadInput())
    val inputState: StateFlow<GamepadInput> = _inputState.asStateFlow()

    private val networkManager = GamepadNetworkManager(
        scope = viewModelScope,
        getInputState = { _inputState.value }
    )

    private val sensorController = SensorController(
        updateInputState = { updateFunc -> _inputState.update(updateFunc) }
    )

    // Expose flows for the UI
    val isConnected = networkManager.isConnected
    val feedbackFlow = networkManager.feedbackFlow

    fun connect(ip: String, port: Int) = networkManager.connect(ip, port)
    fun disconnect() = networkManager.disconnect()

    fun updateAccel(x: Float, y: Float, z: Float) = sensorController.updateAccel(x, y, z)
    fun update6AxisGyro(x: Float, y: Float, z: Float) = sensorController.update6AxisGyro(x, y, z)

    fun updateButton(buttonName: String, isPressed: Boolean) {
        android.util.Log.d("NEXPAD_DEBUG", "Button $buttonName updated to: $isPressed")
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
}
