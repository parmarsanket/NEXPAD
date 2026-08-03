package com.sanket.tools.nexpad.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import kotlin.math.abs

data class MotionPacket(
    var timestamp: Long = 0L,

    // Orientation (Yaw, Pitch, Roll)
    var yaw: Float = 0f,
    var pitch: Float = 0f,
    var roll: Float = 0f,
    
    // Quaternion
    var qX: Float = 0f,
    var qY: Float = 0f,
    var qZ: Float = 0f,
    var qW: Float = 1f,

    // Gravity
    var gravityX: Float = 0f,
    var gravityY: Float = 0f,
    var gravityZ: Float = 0f,

    // Linear Acceleration
    var accelX: Float = 0f,
    var accelY: Float = 0f,
    var accelZ: Float = 0f,

    // Calibrated Gyro
    var gyroX: Float = 0f,
    var gyroY: Float = 0f,
    var gyroZ: Float = 0f,

    // Raw Gyro
    var rawGyroX: Float = 0f,
    var rawGyroY: Float = 0f,
    var rawGyroZ: Float = 0f,

    // Bias
    var biasX: Float = 0f,
    var biasY: Float = 0f,
    var biasZ: Float = 0f
)

private fun Float.deadZone(threshold: Float = 0.02f): Float {
    return if (abs(this) < threshold) 0f else this
}

class MotionSensorManager(
    private val context: Context,
    private val onMotionPacket: (MotionPacket) -> Unit
) : SensorEventListener {

    companion object {
        // High sampling rate for professional controller (e.g., ~200Hz)
        private const val SENSOR_DELAY_MICROS = 5000
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
    private val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val uncalibratedGyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED)
    private val gameRotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)

    // Reused arrays (zero allocations during sensor updates)
    private val rotationMatrix = FloatArray(9)
    private val remappedMatrix = FloatArray(9)
    private val orientation = FloatArray(3)
    private val quaternion = FloatArray(4)

    // Single synchronized packet
    private val motion = MotionPacket()
    
    /**
     * NEXPAD always runs in locked landscape. We hardcode ROTATION_90 because:
     * - Some phones report ROTATION_0 even in landscape (their "natural" orientation IS landscape)
     * - Querying the display at start() is unreliable if the Activity hasn't fully rotated yet
     * - All sensor coordinate remapping must be consistent for the Desktop's gyro math
     *
     * Android sensor coordinate system (portrait, ROTATION_0):
     *   X → points right along the short edge
     *   Y → points up along the long edge
     *   Z → points out of the screen
     *
     * After ROTATION_90 remap (landscape, phone turned left):
     *   X' = Y   (the long edge is now horizontal)
     *   Y' = -X  (the short edge is now vertical, inverted)
     *   Z' = Z   (unchanged, still out of screen)
     */
    private val displayRotation = Surface.ROTATION_90

    fun start() {
        listOf(
            gravitySensor,
            accelSensor,
            gyroSensor,
            uncalibratedGyroSensor,
            gameRotationSensor
        ).forEach { sensor ->
            sensor?.let { sensorManager.registerListener(this, it, SENSOR_DELAY_MICROS) }
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return

        motion.timestamp = event.timestamp

        val hwX = event.values[0]
        val hwY = event.values[1]
        val hwZ = event.values[2]

        var x = hwX
        var y = hwY

        // Manual swap for raw sensors based on cached display rotation
        when (displayRotation) {
            Surface.ROTATION_90 -> {
                x = hwY; y = -hwX
            }
            Surface.ROTATION_180 -> {
                x = -hwX; y = -hwY
            }
            Surface.ROTATION_270 -> {
                x = -hwY; y = hwX
            }
        }

        when (event.sensor.type) {
            Sensor.TYPE_GRAVITY -> {
                motion.gravityX = x
                motion.gravityY = y
                motion.gravityZ = hwZ
            }
            Sensor.TYPE_ACCELEROMETER -> {
                motion.accelX = x.deadZone()
                motion.accelY = y.deadZone()
                motion.accelZ = hwZ.deadZone()
            }
            Sensor.TYPE_GYROSCOPE -> {
                motion.gyroX = x.deadZone()
                motion.gyroY = y.deadZone()
                motion.gyroZ = hwZ.deadZone()
            }
            Sensor.TYPE_GYROSCOPE_UNCALIBRATED -> {
                motion.rawGyroX = x
                motion.rawGyroY = y
                motion.rawGyroZ = hwZ

                if (event.values.size >= 6) {
                    // Bias values are also in portrait coordinate space — remap to landscape
                    val hwBiasX = event.values[3]
                    val hwBiasY = event.values[4]
                    motion.biasX = hwBiasY        // X' = Y  (landscape remap)
                    motion.biasY = -hwBiasX       // Y' = -X (landscape remap)
                    motion.biasZ = event.values[5] // Z unchanged
                }
            }
            Sensor.TYPE_GAME_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

                // The correct way to handle display rotation for Rotation Vectors
                when (displayRotation) {
                    Surface.ROTATION_90 -> SensorManager.remapCoordinateSystem(
                        rotationMatrix,
                        SensorManager.AXIS_Y,
                        SensorManager.AXIS_MINUS_X,
                        remappedMatrix
                    )
                    Surface.ROTATION_270 -> SensorManager.remapCoordinateSystem(
                        rotationMatrix,
                        SensorManager.AXIS_MINUS_Y,
                        SensorManager.AXIS_X,
                        remappedMatrix
                    )
                    Surface.ROTATION_180 -> SensorManager.remapCoordinateSystem(
                        rotationMatrix,
                        SensorManager.AXIS_MINUS_X,
                        SensorManager.AXIS_MINUS_Y,
                        remappedMatrix
                    )
                    else -> System.arraycopy(rotationMatrix, 0, remappedMatrix, 0, 9)
                }

                SensorManager.getOrientation(remappedMatrix, orientation)

                motion.yaw = Math.toDegrees(orientation[0].toDouble()).toFloat()
                motion.pitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
                motion.roll = Math.toDegrees(orientation[2].toDouble()).toFloat()
                
                SensorManager.getQuaternionFromVector(quaternion, event.values)
                motion.qW = quaternion[0]
                motion.qX = quaternion[1]
                motion.qY = quaternion[2]
                motion.qZ = quaternion[3]

                // ONLY emit the synchronized packet on the Master Tick (Rotation Vector)
                // Otherwise we spam the network 5x per frame.
                onMotionPacket(motion)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}