package com.sanket.tools.nexpad.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.view.WindowManager
import android.view.Surface
import android.os.Build
import android.hardware.SensorManager

class GyroSensor(private val context: Context, private val onGyroChanged: (Float, Float, Float) -> Unit) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)

    fun start() {
        gyro?.let {
            // Use SENSOR_DELAY_GAME (approx 20ms delay) for low latency
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_GRAVITY) {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val rotation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.display?.rotation ?: Surface.ROTATION_0
            } else {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay.rotation
            }

            val x = event.values[0]
            var y = event.values[1]
            val z = event.values[2]

            // If the device is flipped 180 degrees into reverse landscape, 
            // the hardware Y axis is physically inverted relative to the user's hands!
            if (rotation == Surface.ROTATION_270) {
                y = -y
            }

            onGyroChanged(x, y, z)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
