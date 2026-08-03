package com.sanket.tools.nexpad.viewmodel

import com.sanket.tools.nexpad.model.GamepadInput

class SensorController(
    private val inputState: GamepadInput
) {
    // Android App now acts as a dumb client for sensors.
    // It ALWAYS sends the raw hardware Gyro and Accelerometer data to the PC.
    // All configuration (inversion, 2D steering, disabling) will be handled in the Desktop App.

    fun updateAccel(x: Float, y: Float, z: Float) {
        inputState.accelX = x
        inputState.accelY = y
        inputState.accelZ = z
    }

    fun update6AxisGyro(x: Float, y: Float, z: Float) {
        inputState.gyroX = x
        inputState.gyroY = y
        inputState.gyroZ = z
    }
}
