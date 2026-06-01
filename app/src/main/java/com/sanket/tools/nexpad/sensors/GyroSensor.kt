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
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
    private val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    fun start() {
        // Use SENSOR_DELAY_GAME for low latency
        gravitySensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        accelSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        gyroSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val rotation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display?.rotation ?: Surface.ROTATION_0
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.rotation
        }

        var x = event.values[0]
        var y = event.values[1]
        var z = event.values[2]

        // Fix hardware axis inversion on 180 flip
        if (rotation == Surface.ROTATION_270) {
            y = -y
        }

        when (event.sensor.type) {
            Sensor.TYPE_GRAVITY -> onGravityChanged(x, y, z)
            Sensor.TYPE_ACCELEROMETER -> onAccelChanged(x, y, z)
            Sensor.TYPE_GYROSCOPE -> onGyroChanged(x, y, z)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
