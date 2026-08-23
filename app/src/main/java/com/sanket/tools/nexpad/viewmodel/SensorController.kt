package com.sanket.tools.nexpad.viewmodel

import com.sanket.tools.nexpad.model.GamepadInput

import com.sanket.tools.nexpad.protocol.NexpadProtocol

class SensorController(
    private val inputState: GamepadInput
) {
    // Android App acts as a client for sensors.
    // When Gravity sensor is available, it sends filtered Gravity in the accel slots with SENSOR_FLAG_GRAVITY.
    // Fallback sends raw Accelerometer data.

    fun updateAccel(x: Float, y: Float, z: Float) {
        inputState.accelX = x
        inputState.accelY = y
        inputState.accelZ = z
        inputState.sensorFlags = 0
    }

    fun update6AxisGyro(x: Float, y: Float, z: Float) {
        inputState.gyroX = x
        inputState.gyroY = y
        inputState.gyroZ = z
    }

    fun updateGravity(x: Float, y: Float, z: Float) {
        inputState.gravityX = x
        inputState.gravityY = y
        inputState.gravityZ = z
        inputState.accelX = x
        inputState.accelY = y
        inputState.accelZ = z
        inputState.sensorFlags = NexpadProtocol.SENSOR_FLAG_GRAVITY
    }
}
