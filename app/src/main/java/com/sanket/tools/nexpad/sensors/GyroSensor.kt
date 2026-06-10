package com.sanket.tools.nexpad.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.view.Surface
import android.view.WindowManager


class GyroSensor(
    context: Context,
    private val onGravityChanged: (Float, Float, Float) -> Unit,
    private val onAccelChanged: (Float, Float, Float) -> Unit,
    private val onGyroChanged: (Float, Float, Float) -> Unit
) : SensorEventListener {

    companion object {
        private const val SENSOR_DELAY =
            SensorManager.SENSOR_DELAY_FASTEST
    }

    private val appContext = context.applicationContext

    private val sensorManager =
        appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val windowManager by lazy {
        appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    private val gravitySensor =
        sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)

    private val accelSensor =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val gyroSensor =
        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    fun start() {

        listOf(
            gravitySensor,
            accelSensor,
            gyroSensor
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
                appContext.display?.rotation ?: Surface.ROTATION_0
            } else {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay.rotation
            }

        val hwX = event.values[0]
        val hwY = event.values[1]
        val hwZ = event.values[2]

        var x = hwX
        var y = hwY

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
                onGravityChanged(
                    x,
                    y,
                    hwZ
                )

            Sensor.TYPE_ACCELEROMETER ->
                onAccelChanged(
                    x,
                    y,
                    hwZ
                )

            Sensor.TYPE_GYROSCOPE ->
                onGyroChanged(
                    x,
                    y,
                    hwZ
                )
        }
    }

    override fun onAccuracyChanged(
        sensor: Sensor?,
        accuracy: Int
    ) = Unit

}