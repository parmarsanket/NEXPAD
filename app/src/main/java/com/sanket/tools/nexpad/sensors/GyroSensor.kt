package com.sanket.tools.nexpad.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.util.Log
import android.view.Surface
import android.view.WindowManager
import kotlin.math.abs

class GyroSensor(
    private val context: Context,
    private val onGravityChanged: (Float, Float, Float) -> Unit,
    private val onAccelChanged: (Float, Float, Float) -> Unit,
    private val onGyroChanged: (Float, Float, Float) -> Unit,
    private val onGameRotationChanged: (Float, Float, Float) -> Unit
) : SensorEventListener {

    companion object {
        private const val SENSOR_DELAY = SensorManager.SENSOR_DELAY_GAME
        private const val TAG = "GyroSensor"
        private const val LOG_INTERVAL_MS = 100L
        private const val CHANGE_THRESHOLD = 0.1f
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val windowManager by lazy { context.getSystemService(Context.WINDOW_SERVICE) as WindowManager }

    private val gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
    private val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val gameRotationSensor =
        sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)

    // MOVED TO CLASS LEVEL so they retain state between sensor ticks
    private var lastLogTime = 0L
    private var lastGyroX = 0f;
    private var lastGyroY = 0f;
    private var lastGyroZ = 0f
    private var lastAccelX = 0f;
    private var lastAccelY = 0f;
    private var lastAccelZ = 0f
    private var lastGravityX = 0f;
    private var lastGravityY = 0f;
    private var lastGravityZ = 0f

    fun start() {
        listOf(gravitySensor, accelSensor, gyroSensor, gameRotationSensor).forEach { sensor ->
            sensor?.let { sensorManager.registerListener(this, it, SENSOR_DELAY) }
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return

        val rotation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display?.rotation ?: Surface.ROTATION_0
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.rotation
        }

        val hwX = event.values[0]
        val hwY = event.values[1]
        val hwZ = event.values[2]

        var x = hwX
        var y = hwY

        // Manual swap for raw sensors
        when (rotation) {
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

        val now = System.currentTimeMillis()
        val shouldLog = now - lastLogTime >= LOG_INTERVAL_MS

        when (event.sensor.type) {
            Sensor.TYPE_GRAVITY -> {
                if (shouldLog && (abs(x - lastGravityX) > CHANGE_THRESHOLD || abs(y - lastGravityY) > CHANGE_THRESHOLD || abs(
                        hwZ - lastGravityZ
                    ) > CHANGE_THRESHOLD)
                ) {
                    Log.d(
                        TAG,
                        "GRAVITY | X=${"%.2f".format(x)} | Y=${"%.2f".format(y)} | Z=${
                            "%.2f".format(hwZ)
                        }"
                    )
                    lastGravityX = x; lastGravityY = y; lastGravityZ = hwZ
                    lastLogTime = now
                }
                onGravityChanged(x, y, hwZ)
            }

            Sensor.TYPE_ACCELEROMETER -> {
                if (shouldLog && (abs(x - lastAccelX) > CHANGE_THRESHOLD || abs(y - lastAccelY) > CHANGE_THRESHOLD || abs(
                        hwZ - lastAccelZ
                    ) > CHANGE_THRESHOLD)
                ) {
                    Log.d(
                        TAG,
                        "ACCEL   | X=${"%.2f".format(x)} | Y=${"%.2f".format(y)} | Z=${
                            "%.2f".format(hwZ)
                        }"
                    )
                    lastAccelX = x; lastAccelY = y; lastAccelZ = hwZ
                    lastLogTime = now
                }
                onAccelChanged(x, y, hwZ)
            }

            Sensor.TYPE_GYROSCOPE -> {
                if (shouldLog && (abs(x - lastGyroX) > CHANGE_THRESHOLD || abs(y - lastGyroY) > CHANGE_THRESHOLD || abs(
                        hwZ - lastGyroZ
                    ) > CHANGE_THRESHOLD)
                ) {
                    Log.d(
                        TAG,
                        "GYRO    | X=${"%.2f".format(x)} | Y=${"%.2f".format(y)} | Z=${
                            "%.2f".format(hwZ)
                        }"
                    )
                    lastGyroX = x; lastGyroY = y; lastGyroZ = hwZ
                    lastLogTime = now
                }
                onGyroChanged(x, y, hwZ)
            }

            Sensor.TYPE_GAME_ROTATION_VECTOR -> {
                val rotationMatrix = FloatArray(9)
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

                val remappedMatrix = FloatArray(9)

                // The correct way to handle display rotation for Rotation Vectors
                when (rotation) {
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

                    else -> System.arraycopy(rotationMatrix, 0, remappedMatrix, 0, 9) // No rotation
                }

                val orientation = FloatArray(3)
                SensorManager.getOrientation(remappedMatrix, orientation)

                val yaw = Math.toDegrees(orientation[0].toDouble()).toFloat()
                val pitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
                val roll = Math.toDegrees(orientation[2].toDouble()).toFloat()

                // Optional: apply logging threshold here too if you want to avoid console spam

                onGameRotationChanged(yaw, pitch, roll)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}