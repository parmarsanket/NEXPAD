package com.sanket.tools.nexpad.sensors

import android.content.Context
import com.sanket.tools.nexpad.protocol.NexpadProtocol
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel

/**
 * Coordinates physical device IMU motion sensors with the GamepadViewModel,
 * dynamically dispatching motion packets according to the requested sensor protocol mode.
 */
class MotionSensorCoordinator(
    context: Context,
    private val viewModel: GamepadViewModel
) {
    private val motionSensorManager = MotionSensorManager(
        context = context,
        onMotionPacket = ::dispatchMotionPacket
    )

    var isGyroMuted: Boolean
        get() = motionSensorManager.isGyroMuted
        set(value) {
            motionSensorManager.isGyroMuted = value
        }

    fun setMode(mode: Byte) {
        motionSensorManager.setMode(mode)
    }

    fun start() {
        motionSensorManager.setMode(viewModel.requestedSensorMode)
        motionSensorManager.start()
    }

    fun stop() {
        motionSensorManager.stop()
    }

    private fun dispatchMotionPacket(packet: MotionPacket) {
        if (isGyroMuted) return
        when (viewModel.requestedSensorMode) {
            NexpadProtocol.SENSOR_MODE_GRAVITY -> {
                // Mode 0x01: Gravity mode for racing / steering wheel tilt
                if (packet.gravityX != 0f || packet.gravityY != 0f || packet.gravityZ != 0f) {
                    viewModel.updateGravity(packet.gravityX, packet.gravityY, packet.gravityZ)
                } else {
                    viewModel.updateGravity(packet.accelX, packet.accelY, packet.accelZ)
                }
            }
            NexpadProtocol.SENSOR_MODE_GAME_ROTATION -> {
                // Mode 0x02: 3D Orientation Quaternion for Air Mouse / Laser Pointer & VR
                viewModel.updateGameRotation(packet.qX, packet.qY, packet.qZ, packet.qW)
            }
            NexpadProtocol.SENSOR_MODE_LINEAR_GESTURE -> {
                // Mode 0x03: Linear Acceleration for flicks/shakes + Gravity for tilt postures
                val gravX = if (packet.gravityX != 0f) packet.gravityX else packet.accelX
                val gravY = if (packet.gravityY != 0f) packet.gravityY else packet.accelY
                val gravZ = if (packet.gravityZ != 0f) packet.gravityZ else packet.accelZ
                viewModel.updateLinearGesture(
                    packet.linearAccelX, packet.linearAccelY, packet.linearAccelZ,
                    gravX, gravY, gravZ,
                    packet.gyroX, packet.gyroY, packet.gyroZ
                )
            }
            NexpadProtocol.SENSOR_MODE_RAW_UNCALIBRATED -> {
                // Mode 0x04: Pure raw silicon gyro + estimated hardware bias
                viewModel.updateRawUncalibrated(
                    packet.rawGyroX, packet.rawGyroY, packet.rawGyroZ,
                    packet.biasX, packet.biasY, packet.biasZ
                )
            }
            else -> {
                // Mode 0x00: Standard 6-axis IMU (gyro + raw accel) for DS4 and FPS aiming
                viewModel.updateAccel(packet.accelX, packet.accelY, packet.accelZ)
                viewModel.update6AxisGyro(packet.gyroX, packet.gyroY, packet.gyroZ)
            }
        }
    }
}
