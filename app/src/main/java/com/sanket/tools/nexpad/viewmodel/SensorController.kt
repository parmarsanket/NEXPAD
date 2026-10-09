package com.sanket.tools.nexpad.viewmodel

import com.sanket.tools.nexpad.model.GamepadInput
import com.sanket.tools.nexpad.protocol.NexpadProtocol

class SensorController(
    private val inputState: GamepadInput
) {
    // Current active sensor flags (defaults to all axes active)
    @Volatile
    var activeSensorFlags: Byte = NexpadProtocol.SENSOR_FLAG_DEFAULT

    /**
     * Unified 6-Axis Motion dispatcher with independent axis isolation and raw mode testing.
     * When specific axis flags are set in activeSensorFlags, only active axes emit data (others emit 0f).
     */
    fun update6AxisMotion(
        pitch: Float,
        yaw: Float,
        roll: Float,
        accelX: Float,
        accelY: Float,
        accelZ: Float,
        gravityX: Float = 0f,
        gravityY: Float = 0f,
        gravityZ: Float = 0f,
        isGravity: Boolean = false
    ) {
        val flags = activeSensorFlags
        val isPitchActive = NexpadProtocol.isPitchEnabled(flags)
        val isYawActive = NexpadProtocol.isYawEnabled(flags)
        val isRollActive = NexpadProtocol.isRollEnabled(flags)
        val isAxActive = NexpadProtocol.isAccelXEnabled(flags)
        val isAyActive = NexpadProtocol.isAccelYEnabled(flags)
        val isAzActive = NexpadProtocol.isAccelZEnabled(flags)

        // Gyroscope slots: Pitch (X), Yaw (Y), Roll (Z)
        inputState.gyroX = if (isPitchActive) pitch else 0f
        inputState.gyroY = if (isYawActive) yaw else 0f
        inputState.gyroZ = if (isRollActive) roll else 0f

        // Accelerometer slots: Accel X, Accel Y, Accel Z
        inputState.accelX = if (isAxActive) accelX else 0f
        inputState.accelY = if (isAyActive) accelY else 0f
        inputState.accelZ = if (isAzActive) accelZ else 0f

        inputState.gravityX = gravityX
        inputState.gravityY = gravityY
        inputState.gravityZ = gravityZ

        var finalFlags = flags.toInt()
        if (isGravity) {
            finalFlags = finalFlags or NexpadProtocol.SENSOR_FLAG_GRAVITY.toInt()
        } else {
            finalFlags = finalFlags and NexpadProtocol.SENSOR_FLAG_GRAVITY.toInt().inv()
        }
        inputState.sensorFlags = finalFlags.toByte()
    }

    fun updateAccel(x: Float, y: Float, z: Float) {
        val flags = activeSensorFlags
        inputState.accelX = if (NexpadProtocol.isAccelXEnabled(flags)) x else 0f
        inputState.accelY = if (NexpadProtocol.isAccelYEnabled(flags)) y else 0f
        inputState.accelZ = if (NexpadProtocol.isAccelZEnabled(flags)) z else 0f
        inputState.sensorFlags = (activeSensorFlags.toInt() and NexpadProtocol.SENSOR_FLAG_GRAVITY.toInt().inv()).toByte()
    }

    fun update6AxisGyro(x: Float, y: Float, z: Float) {
        val flags = activeSensorFlags
        inputState.gyroX = if (NexpadProtocol.isPitchEnabled(flags)) x else 0f
        inputState.gyroY = if (NexpadProtocol.isYawEnabled(flags)) y else 0f
        inputState.gyroZ = if (NexpadProtocol.isRollEnabled(flags)) z else 0f
    }

    fun updateGravity(x: Float, y: Float, z: Float) {
        inputState.gravityX = x
        inputState.gravityY = y
        inputState.gravityZ = z
        val flags = activeSensorFlags
        inputState.accelX = if (NexpadProtocol.isAccelXEnabled(flags)) x else 0f
        inputState.accelY = if (NexpadProtocol.isAccelYEnabled(flags)) y else 0f
        inputState.accelZ = if (NexpadProtocol.isAccelZEnabled(flags)) z else 0f
        inputState.sensorFlags = (activeSensorFlags.toInt() or NexpadProtocol.SENSOR_FLAG_GRAVITY.toInt()).toByte()
    }
}
