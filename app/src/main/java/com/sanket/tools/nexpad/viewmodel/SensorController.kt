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
            val sx = safeFloat(x)
            val sy = safeFloat(y)
            val sz = safeFloat(z)
            synchronized(inputState) {
                inputState.accelX = sx
                inputState.accelY = sy
                inputState.accelZ = sz
                inputState.sensorFlags = NexpadProtocol.SENSOR_MODE_IMU_6AXIS
            }
        }
    }

    fun update6AxisGyro(x: Float, y: Float, z: Float) {
        if (isMotionMuted) return
        if (requestedSensorMode == NexpadProtocol.SENSOR_MODE_IMU_6AXIS) {
            val sx = safeFloat(x)
            val sy = safeFloat(y)
            val sz = safeFloat(z)
            synchronized(inputState) {
                inputState.gyroX = sx
                inputState.gyroY = sy
                inputState.gyroZ = sz
                inputState.sensorFlags = NexpadProtocol.SENSOR_MODE_IMU_6AXIS
            }
        }
    }

    fun updateGravity(x: Float, y: Float, z: Float) {
        if (isMotionMuted) return
        if (requestedSensorMode == NexpadProtocol.SENSOR_MODE_GRAVITY) {
            val sx = safeFloat(x)
            val sy = safeFloat(y)
            val sz = safeFloat(z)
            synchronized(inputState) {
                inputState.gravityX = sx
                inputState.gravityY = sy
                inputState.gravityZ = sz
                inputState.sensorFlags = NexpadProtocol.SENSOR_MODE_GRAVITY
            }
        }
    }

    fun updateGameRotation(qx: Float, qy: Float, qz: Float, qw: Float) {
        if (isMotionMuted) return
        if (requestedSensorMode == NexpadProtocol.SENSOR_MODE_GAME_ROTATION) {
            val sqx = safeFloat(qx)
            val sqy = safeFloat(qy)
            val sqz = safeFloat(qz)
            val sqw = safeFloat(qw, fallback = 1f)
            val lenSq = sqx * sqx + sqy * sqy + sqz * sqz + sqw * sqw
            val isNormalized = lenSq > 1e-4f && lenSq < 4f

            synchronized(inputState) {
                if (isNormalized) {
                    inputState.quatX = sqx
                    inputState.quatY = sqy
                    inputState.quatZ = sqz
                    inputState.quatW = sqw
                } else {
                    inputState.quatX = 0f
                    inputState.quatY = 0f
                    inputState.quatZ = 0f
                    inputState.quatW = 1f
                }
                inputState.sensorFlags = NexpadProtocol.SENSOR_MODE_GAME_ROTATION
            }
        }
    }

    fun updateLinearGesture(
        lx: Float, ly: Float, lz: Float,
        gx: Float, gy: Float, gz: Float,
        rx: Float = 0f, ry: Float = 0f, rz: Float = 0f
    ) {
        if (isMotionMuted) return
        if (requestedSensorMode == NexpadProtocol.SENSOR_MODE_LINEAR_GESTURE) {
            val slx = safeFloat(lx)
            val sly = safeFloat(ly)
            val slz = safeFloat(lz)
            val sgx = safeFloat(gx)
            val sgy = safeFloat(gy)
            val sgz = safeFloat(gz)
            val srx = safeFloat(rx)
            val sry = safeFloat(ry)
            val srz = safeFloat(rz)

            synchronized(inputState) {
                inputState.linearAccelX = slx
                inputState.linearAccelY = sly
                inputState.linearAccelZ = slz
                inputState.gravityX = sgx
                inputState.gravityY = sgy
                inputState.gravityZ = sgz
                inputState.accelX = sgx + slx
                inputState.accelY = sgy + sly
                inputState.accelZ = sgz + slz
                inputState.gyroX = srx
                inputState.gyroY = sry
                inputState.gyroZ = srz
                inputState.sensorFlags = NexpadProtocol.SENSOR_MODE_LINEAR_GESTURE
            }
        }
    }

    fun updateRawUncalibrated(rx: Float, ry: Float, rz: Float, bx: Float, by: Float, bz: Float) {
        if (isMotionMuted) return
        if (requestedSensorMode == NexpadProtocol.SENSOR_MODE_RAW_UNCALIBRATED) {
            val srx = safeFloat(rx)
            val sry = safeFloat(ry)
            val srz = safeFloat(rz)
            val sbx = safeFloat(bx)
            val sby = safeFloat(by)
            val sbz = safeFloat(bz)

            synchronized(inputState) {
                inputState.rawGyroX = srx
                inputState.rawGyroY = sry
                inputState.rawGyroZ = srz
                inputState.gyroBiasX = sbx
                inputState.gyroBiasY = sby
                inputState.gyroBiasZ = sbz
                inputState.sensorFlags = NexpadProtocol.SENSOR_MODE_RAW_UNCALIBRATED
            }
        }
    }

    companion object {
        private const val TAG = "NexpadSensorController"

        private fun safeFloat(v: Float, fallback: Float = 0f): Float {
            return if (v.isFinite()) v else fallback
        }
    }
}
