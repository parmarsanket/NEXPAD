package com.sanket.tools.nexpad.viewmodel

import com.sanket.tools.nexpad.model.GamepadInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class SensorController(
    private val updateInputState: ((GamepadInput) -> GamepadInput) -> Unit
) {
    val isGyroSteeringEnabled = MutableStateFlow(false)
    val isGyroInverted = MutableStateFlow(false)
    val is6AxisEnabled = MutableStateFlow(false)
    val is6AxisInverted = MutableStateFlow(false)

    fun toggleGyroSteering() {
        isGyroSteeringEnabled.value = !isGyroSteeringEnabled.value
    }

    fun update2DSteering(x: Float, y: Float, z: Float) {
        // Only run 2D steering if 6-axis is disabled
        if (!is6AxisEnabled.value) {
            updateInputState { current ->
                val maxTilt = 6.0f
                val multiplier = if (isGyroInverted.value) -1f else 1f
                val rawSteering = ((x * multiplier) / maxTilt).coerceIn(-1.0f, 1.0f)
                
                if (isGyroSteeringEnabled.value) {
                    current.copy(leftStickX = rawSteering)
                } else {
                    current
                }
            }
        }
    }

    fun updateAccel(x: Float, y: Float, z: Float) {
        if (is6AxisEnabled.value) {
            val mult = if (is6AxisInverted.value) -1f else 1f
            updateInputState { it.copy(accelX = x * mult, accelY = y * mult, accelZ = z * mult) }
        }
    }

    fun update6AxisGyro(x: Float, y: Float, z: Float) {
        if (is6AxisEnabled.value) {
            val mult = if (is6AxisInverted.value) -1f else 1f
            updateInputState { it.copy(gyroX = x * mult, gyroY = y * mult, gyroZ = z * mult) }
        }
    }
}
