package com.sanket.tools.nexpad.viewmodel

import com.sanket.tools.nexpad.model.GamepadInput

import com.sanket.tools.nexpad.protocol.NexpadProtocol

class SensorController(
    private val inputState: GamepadInput
) {
    @Volatile
    var requestedSensorMode: Byte = NexpadProtocol.SENSOR_MODE_IMU_6AXIS
        private set

    fun setRequestedSensorMode(mode: Byte) {
        if (requestedSensorMode != mode) {
            requestedSensorMode = mode
            inputState.sensorFlags = mode
            if (mode == NexpadProtocol.SENSOR_MODE_GRAVITY) {
                // Clear IMU slots so no residual gyro/accel data leaks into the packet
                inputState.gyroX = 0f
                inputState.gyroY = 0f
                inputState.gyroZ = 0f
                inputState.accelX = 0f
                inputState.accelY = 0f
                inputState.accelZ = 0f
            } else {
                // Clear gravity slots
                inputState.gravityX = 0f
                inputState.gravityY = 0f
                inputState.gravityZ = 0f
            }
        }
    }

    fun updateAccel(x: Float, y: Float, z: Float) {
        if (requestedSensorMode == NexpadProtocol.SENSOR_MODE_IMU_6AXIS) {
            inputState.accelX = x
            inputState.accelY = y
            inputState.accelZ = z
            inputState.sensorFlags = NexpadProtocol.SENSOR_MODE_IMU_6AXIS
        }
    }

    fun update6AxisGyro(x: Float, y: Float, z: Float) {
        if (requestedSensorMode == NexpadProtocol.SENSOR_MODE_IMU_6AXIS) {
            inputState.gyroX = x
            inputState.gyroY = y
            inputState.gyroZ = z
            inputState.sensorFlags = NexpadProtocol.SENSOR_MODE_IMU_6AXIS
        }
    }

    fun updateGravity(x: Float, y: Float, z: Float) {
        if (requestedSensorMode == NexpadProtocol.SENSOR_MODE_GRAVITY) {
            inputState.gravityX = x
            inputState.gravityY = y
            inputState.gravityZ = z
            inputState.sensorFlags = NexpadProtocol.SENSOR_MODE_GRAVITY
        }
    }
}
