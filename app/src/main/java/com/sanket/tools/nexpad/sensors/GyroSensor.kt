package com.sanket.tools.nexpad.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.view.WindowManager
import android.view.Surface
import android.os.Build
import android.hardware.SensorManager

class GyroSensor(
    private val context: Context,
    private val onGravityChanged: (Float, Float, Float) -> Unit,
    private val onAccelChanged: (Float, Float, Float) -> Unit,
    private val onGyroChanged: (Float, Float, Float) -> Unit
) : SensorEventListener {

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

    fun start() {
        gravitySensor?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_FASTEST
            )
        }

        accelSensor?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_FASTEST
            )
        }

        gyroSensor?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_FASTEST
            )
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
        val z = hwZ

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

        when (event.sensor.type) {
            Sensor.TYPE_GRAVITY ->
                onGravityChanged(x, y, z)

            Sensor.TYPE_ACCELEROMETER ->
                onAccelChanged(x, y, z)

            Sensor.TYPE_GYROSCOPE ->
                onGyroChanged(x, y, z)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
