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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import kotlin.math.abs
class GyroSensor(
    private val context: Context,
    private val onGravityChanged: (Float, Float, Float) -> Unit,
    private val onAccelChanged: (Float, Float, Float) -> Unit,
    private val onGyroChanged: (Float, Float, Float) -> Unit,
    private val onGameRotationChanged: (Float, Float, Float) -> Unit
) : SensorEventListener {

    companion object {
        private const val SENSOR_DELAY =
            SensorManager.SENSOR_DELAY_GAME

        private const val TAG = "GyroSensor"
        private const val LOG_INTERVAL_MS = 100L
    }



    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val windowManager by lazy {
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    private val gravitySensor =
        sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)

    private val accelSensor =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val gyroSensor =
        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private val gameRotationSensor =
        sensorManager.getDefaultSensor(
            Sensor.TYPE_GAME_ROTATION_VECTOR
        )
    private var lastLogTime = 0L

    fun start() {

        listOf(
            gravitySensor,
            accelSensor,
            gyroSensor,
            gameRotationSensor
        ).forEach { sensor ->
            sensor?.let {
                sensorManager.registerListener(
                    this,
                    it,
                    SENSOR_DELAY
                )
            }
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {

        event ?: return

        val rotation =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
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

        var lastGyroX = 0f
        var lastGyroY = 0f
        var lastGyroZ = 0f

        var lastAccelX = 0f
        var lastAccelY = 0f
        var lastAccelZ = 0f

        var lastGravityX = 0f
        var lastGravityY = 0f
        var lastGravityZ = 0f

        val CHANGE_THRESHOLD = 0.1f
        when (rotation) {

            Surface.ROTATION_90 -> {
                x = hwY
                y = -hwX
            }

            Surface.ROTATION_180 -> {
                x = -hwX
                y = -hwY
            }

            Surface.ROTATION_270 -> {
                x = -hwY
                y = hwX
            }
        }

        val now = System.currentTimeMillis()
        val shouldLog = now - lastLogTime >= LOG_INTERVAL_MS

        when (event.sensor.type) {

            Sensor.TYPE_GRAVITY -> {

                if (
                    abs(x - lastGravityX) > CHANGE_THRESHOLD ||
                    abs(y - lastGravityY) > CHANGE_THRESHOLD ||
                    abs(hwZ - lastGravityZ) > CHANGE_THRESHOLD
                ) {

                    Log.d(
                        TAG,
                        "GRAVITY | X=${"%.2f".format(x)} | Y=${"%.2f".format(y)} | Z=${"%.2f".format(hwZ)}"
                    )

                    lastGravityX = x
                    lastGravityY = y
                    lastGravityZ = hwZ
                }

                onGravityChanged(
                    x,
                    y,
                    hwZ
                )
            }

            Sensor.TYPE_ACCELEROMETER -> {

                if (
                    abs(x - lastAccelX) > CHANGE_THRESHOLD ||
                    abs(y - lastAccelY) > CHANGE_THRESHOLD ||
                    abs(hwZ - lastAccelZ) > CHANGE_THRESHOLD
                ) {

                    Log.d(
                        TAG,
                        "ACCEL   | X=${"%.2f".format(x)} | Y=${"%.2f".format(y)} | Z=${"%.2f".format(hwZ)}"
                    )

                    lastAccelX = x
                    lastAccelY = y
                    lastAccelZ = hwZ
                }

                onAccelChanged(
                    x,
                    y,
                    hwZ
                )
            }

            Sensor.TYPE_GYROSCOPE -> {

                if (
                    abs(x - lastGyroX) > CHANGE_THRESHOLD ||
                    abs(y - lastGyroY) > CHANGE_THRESHOLD ||
                    abs(hwZ - lastGyroZ) > CHANGE_THRESHOLD
                ) {

                    Log.d(
                        TAG,
                        "GYRO    | X=${"%.2f".format(x)} | Y=${"%.2f".format(y)} | Z=${"%.2f".format(hwZ)}"
                    )

                    lastGyroX = x
                    lastGyroY = y
                    lastGyroZ = hwZ
                }

                onGyroChanged(
                    x,
                    y,
                    hwZ
                )
            }
            Sensor.TYPE_GAME_ROTATION_VECTOR -> {

                val rotationMatrix = FloatArray(9)

                SensorManager.getRotationMatrixFromVector(
                    rotationMatrix,
                    event.values
                )

                val orientation = FloatArray(3)

                SensorManager.getOrientation(
                    rotationMatrix,
                    orientation
                )

                val yaw = Math.toDegrees(
                    orientation[0].toDouble()
                ).toFloat()

                val pitch = Math.toDegrees(
                    orientation[1].toDouble()
                ).toFloat()

                val roll = Math.toDegrees(
                    orientation[2].toDouble()
                ).toFloat()

                Log.d(
                    TAG,
                    "GAME_ROTATION | Yaw=${"%.1f".format(yaw)} | Pitch=${"%.1f".format(pitch)} | Roll=${"%.1f".format(roll)}"
                )

                onGameRotationChanged(
                    yaw,
                    pitch,
                    roll
                )
            }
        }
    }

    override fun onAccuracyChanged(
        sensor: Sensor?,
        accuracy: Int
    ) = Unit
}