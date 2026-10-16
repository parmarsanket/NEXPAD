package com.sanket.tools.nexpad.viewmodel

import com.sanket.tools.nexpad.model.GamepadInput
import com.sanket.tools.nexpad.protocol.NexpadProtocol
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SensorController(
    private val inputState: GamepadInput
) {
    private val _requestedSensorMode = MutableStateFlow(NexpadProtocol.SENSOR_MODE_IMU_6AXIS)
    val requestedSensorModeFlow: StateFlow<Byte> = _requestedSensorMode.asStateFlow()
    val requestedSensorMode: Byte get() = _requestedSensorMode.value

    @Volatile
    var isMotionMuted: Boolean = false
        set(value) {
            field = value
            if (value) {
                resetAllSensors()
            }
        }

    fun resetAllSensors() {
        synchronized(inputState) {
            inputState.gyroX = 0f
            inputState.gyroY = 0f
            inputState.gyroZ = 0f
            inputState.accelX = 0f
            inputState.accelY = 0f
            inputState.accelZ = 0f
            inputState.gravityX = 0f
            inputState.gravityY = 0f
            inputState.gravityZ = 0f
            inputState.quatX = 0f
            inputState.quatY = 0f
            inputState.quatZ = 0f
            inputState.quatW = 1f
            inputState.linearAccelX = 0f
            inputState.linearAccelY = 0f
            inputState.linearAccelZ = 0f
            inputState.rawGyroX = 0f
            inputState.rawGyroY = 0f
            inputState.rawGyroZ = 0f
            inputState.gyroBiasX = 0f
            inputState.gyroBiasY = 0f
            inputState.gyroBiasZ = 0f
        }
    }

    fun setRequestedSensorMode(mode: Byte) {
        if (_requestedSensorMode.value != mode) {
            _requestedSensorMode.value = mode
            synchronized(inputState) {
                inputState.sensorFlags = mode
                // Clear all sensor channels on mode transition to avoid residue
                resetAllSensors()
            }
        }
    }

    fun updateAccel(x: Float, y: Float, z: Float) {
        if (isMotionMuted) return
        if (requestedSensorMode == NexpadProtocol.SENSOR_MODE_IMU_6AXIS) {
            synchronized(inputState) {
                inputState.accelX = x
                inputState.accelY = y
                inputState.accelZ = z
                inputState.sensorFlags = NexpadProtocol.SENSOR_MODE_IMU_6AXIS
            }
        }
    }

    fun update6AxisGyro(x: Float, y: Float, z: Float) {
        if (isMotionMuted) return
        if (requestedSensorMode == NexpadProtocol.SENSOR_MODE_IMU_6AXIS) {
            synchronized(inputState) {
                inputState.gyroX = x
                inputState.gyroY = y
                inputState.gyroZ = z
                inputState.sensorFlags = NexpadProtocol.SENSOR_MODE_IMU_6AXIS
            }
        }
    }

    fun updateGravity(x: Float, y: Float, z: Float) {
        if (isMotionMuted) return
        if (requestedSensorMode == NexpadProtocol.SENSOR_MODE_GRAVITY) {
            synchronized(inputState) {
                inputState.gravityX = x
                inputState.gravityY = y
                inputState.gravityZ = z
                inputState.sensorFlags = NexpadProtocol.SENSOR_MODE_GRAVITY
            }
        }
    }

    fun updateGameRotation(qx: Float, qy: Float, qz: Float, qw: Float) {
        if (isMotionMuted) return
        if (requestedSensorMode == NexpadProtocol.SENSOR_MODE_GAME_ROTATION) {
            synchronized(inputState) {
                inputState.quatX = qx
                inputState.quatY = qy
                inputState.quatZ = qz
                inputState.quatW = qw
                inputState.sensorFlags = NexpadProtocol.SENSOR_MODE_GAME_ROTATION
            }
        }
    }

    fun updateLinearGesture(lx: Float, ly: Float, lz: Float, gx: Float, gy: Float, gz: Float, rx: Float = 0f, ry: Float = 0f, rz: Float = 0f) {
        if (isMotionMuted) return
        if (requestedSensorMode == NexpadProtocol.SENSOR_MODE_LINEAR_GESTURE) {
            synchronized(inputState) {
                inputState.linearAccelX = lx
                inputState.linearAccelY = ly
                inputState.linearAccelZ = lz
                inputState.gravityX = gx
                inputState.gravityY = gy
                inputState.gravityZ = gz
                inputState.accelX = gx + lx
                inputState.accelY = gy + ly
                inputState.accelZ = gz + lz
                inputState.gyroX = rx
                inputState.gyroY = ry
                inputState.gyroZ = rz
                inputState.sensorFlags = NexpadProtocol.SENSOR_MODE_LINEAR_GESTURE
            }
        }
    }

    fun updateRawUncalibrated(rx: Float, ry: Float, rz: Float, bx: Float, by: Float, bz: Float) {
        if (isMotionMuted) return
        if (requestedSensorMode == NexpadProtocol.SENSOR_MODE_RAW_UNCALIBRATED) {
            synchronized(inputState) {
                inputState.rawGyroX = rx
                inputState.rawGyroY = ry
                inputState.rawGyroZ = rz
                inputState.gyroBiasX = bx
                inputState.gyroBiasY = by
                inputState.gyroBiasZ = bz
                inputState.sensorFlags = NexpadProtocol.SENSOR_MODE_RAW_UNCALIBRATED
            }
        }
    }
}
