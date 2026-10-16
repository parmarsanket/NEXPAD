package com.sanket.tools.nexpad.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import androidx.compose.runtime.Stable
import kotlin.math.abs

@Stable
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

    // Linear Acceleration (excluding gravity)
    var linearAccelX: Float = 0f,
    var linearAccelY: Float = 0f,
    var linearAccelZ: Float = 0f,

    // Raw Gyro
    var rawGyroX: Float = 0f,
    var rawGyroY: Float = 0f,
    var rawGyroZ: Float = 0f,

    // Bias
    var biasX: Float = 0f,
    var biasY: Float = 0f,
    var biasZ: Float = 0f
)

class MotionSensorManager(
    context: Context,
    private val onMotionPacket: (MotionPacket) -> Unit
) : SensorEventListener {

    companion object {
        // High sampling rate for professional controller (e.g., ~200Hz)
        private const val SENSOR_DELAY_MICROS = 5000
    }

    private val sensorManager = context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
    private val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val uncalibratedGyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED)
    private val linearAccelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
    private val gameRotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)

    // Single synchronized packet
    private val motion = MotionPacket()

    private val registeredSensors = mutableSetOf<Sensor>()
    private var isRunning = false
    private var currentMode: Byte = com.sanket.tools.nexpad.protocol.NexpadProtocol.SENSOR_MODE_IMU_6AXIS

    @Volatile
    var isGyroMuted: Boolean = false
        set(value) {
            field = value
            if (value) {
                motion.gyroX = 0f
                motion.gyroY = 0f
                motion.gyroZ = 0f
                motion.rawGyroX = 0f
                motion.rawGyroY = 0f
                motion.rawGyroZ = 0f
                motion.accelX = 0f
                motion.accelY = 0f
                motion.accelZ = 0f
                motion.gravityX = 0f
                motion.gravityY = 0f
                motion.gravityZ = 0f
                motion.linearAccelX = 0f
                motion.linearAccelY = 0f
                motion.linearAccelZ = 0f
                motion.qX = 0f
                motion.qY = 0f
                motion.qZ = 0f
                motion.qW = 1f
                motion.biasX = 0f
                motion.biasY = 0f
                motion.biasZ = 0f
            }
            synchronized(this) {
                if (isRunning) {
                    updateActiveSensors()
                }
            }
        }
    
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

    private var sensorThread: android.os.HandlerThread? = null
    private var sensorHandler: android.os.Handler? = null

    private var lastSentTimestamp = 0L

    fun setMode(mode: Byte) {
        synchronized(this) {
            if (currentMode != mode) {
                currentMode = mode
                if (isRunning) {
                    updateActiveSensors()
                }
            }
        }
    }

    fun start() {
        synchronized(this) {
            isRunning = true
            if (sensorThread == null) {
                sensorThread = android.os.HandlerThread("NexpadSensorThread", android.os.Process.THREAD_PRIORITY_URGENT_DISPLAY)
                sensorThread?.start()
                sensorHandler = android.os.Handler(sensorThread!!.looper)
            }
            updateActiveSensors()
        }
    }

    fun stop() {
        synchronized(this) {
            isRunning = false
            sensorManager.unregisterListener(this)
            registeredSensors.clear()
            sensorThread?.quitSafely()
            sensorThread = null
            sensorHandler = null
        }
    }

    @Synchronized
    private fun updateActiveSensors() {
        if (!isRunning || isGyroMuted) {
            if (registeredSensors.isNotEmpty()) {
                sensorManager.unregisterListener(this)
                registeredSensors.clear()
            }
            return
        }

        val handler = sensorHandler ?: return

        val targetSensors = when (currentMode) {
            com.sanket.tools.nexpad.protocol.NexpadProtocol.SENSOR_MODE_GRAVITY -> {
                listOfNotNull(gravitySensor ?: accelSensor)
            }
            com.sanket.tools.nexpad.protocol.NexpadProtocol.SENSOR_MODE_GAME_ROTATION -> {
                if (gameRotationSensor != null) listOf(gameRotationSensor)
                else listOfNotNull(gyroSensor, accelSensor)
            }
            com.sanket.tools.nexpad.protocol.NexpadProtocol.SENSOR_MODE_LINEAR_GESTURE -> {
                listOfNotNull(linearAccelSensor, gravitySensor ?: accelSensor)
            }
            com.sanket.tools.nexpad.protocol.NexpadProtocol.SENSOR_MODE_RAW_UNCALIBRATED -> {
                listOfNotNull(uncalibratedGyroSensor ?: gyroSensor)
            }
            else -> { // SENSOR_MODE_IMU_6AXIS (0x00)
                listOfNotNull(gyroSensor, accelSensor)
            }
        }.toSet()

        if (registeredSensors == targetSensors) return

        // Unregister sensors that are no longer needed
        val toRemove = registeredSensors - targetSensors
        for (s in toRemove) {
            sensorManager.unregisterListener(this, s)
        }

        // Register newly needed sensors
        val toAdd = targetSensors - registeredSensors
        for (s in toAdd) {
            sensorManager.registerListener(this, s, SENSOR_DELAY_MICROS, handler)
        }

        registeredSensors.clear()
        registeredSensors.addAll(targetSensors)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return

        if (isGyroMuted) {
            return
        }

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
                motion.accelX = x
                motion.accelY = y
                motion.accelZ = hwZ
            }
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                motion.linearAccelX = x
                motion.linearAccelY = y
                motion.linearAccelZ = hwZ
            }
            Sensor.TYPE_GYROSCOPE -> {
                motion.gyroX = -x
                motion.gyroY = -y
                motion.gyroZ = -hwZ
            }
            Sensor.TYPE_GYROSCOPE_UNCALIBRATED -> {
                motion.rawGyroX = -x
                motion.rawGyroY = -y
                motion.rawGyroZ = -hwZ

                if (event.values.size >= 6) {
                    // Bias values are in portrait space — remap to landscape with matching signs:
                    // rawGyroX = -hwY  ==> biasX = -hwBiasY  ==> cleanX = rawGyroX - biasX = -(hwY - hwBiasY) = gyroX
                    // rawGyroY =  hwX  ==> biasY =  hwBiasX  ==> cleanY = rawGyroY - biasY =  (hwX - hwBiasX) = gyroY
                    // rawGyroZ = -hwZ  ==> biasZ = -hwBiasZ  ==> cleanZ = rawGyroZ - biasZ = -(hwZ - hwBiasZ) = gyroZ
                    val hwBiasX = event.values[3]
                    val hwBiasY = event.values[4]
                    motion.biasX = -hwBiasY
                    motion.biasY = hwBiasX
                    motion.biasZ = -event.values[5]
                }
            }
            Sensor.TYPE_GAME_ROTATION_VECTOR -> {
                val qRawX = event.values[0]
                val qRawY = event.values[1]
                val qRawZ = event.values[2]
                val qRawW = if (event.values.size >= 4) event.values[3] else {
                    val sumSq = qRawX * qRawX + qRawY * qRawY + qRawZ * qRawZ
                    if (sumSq < 1f) kotlin.math.sqrt(1f - sumSq) else 0f
                }
                // ROTATION_90 remap: q' = q * q_rotZ(-90 deg)
                val k = 0.70710678f
                motion.qX = k * (qRawX + qRawY)
                motion.qY = k * (qRawY - qRawX)
                motion.qZ = k * (qRawZ - qRawW)
                motion.qW = k * (qRawW + qRawZ)
            }
        }

        // Emit the synchronized packet on updates from whichever sensor is currently driving the mode
        if (event.sensor.type == Sensor.TYPE_GYROSCOPE || 
            event.sensor.type == Sensor.TYPE_GYROSCOPE_UNCALIBRATED ||
            event.sensor.type == Sensor.TYPE_GRAVITY ||
            event.sensor.type == Sensor.TYPE_GAME_ROTATION_VECTOR ||
            event.sensor.type == Sensor.TYPE_LINEAR_ACCELERATION) {
            if (event.timestamp != lastSentTimestamp) {
                lastSentTimestamp = event.timestamp
                onMotionPacket(motion)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}