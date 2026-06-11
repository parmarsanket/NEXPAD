package com.sanket.tools.nexpad

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.sanket.tools.nexpad.sensors.GyroSensor
import com.sanket.tools.nexpad.ui.GamepadScreen
import com.sanket.tools.nexpad.ui.theme.NEXPADTheme
import com.sanket.tools.nexpad.ui.NavigationGraph
import com.sanket.tools.nexpad.utils.LayoutManager
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel

class MainActivity : ComponentActivity() {
    
    private lateinit var viewModel: GamepadViewModel
    private lateinit var gyroSensor: GyroSensor
    private lateinit var vibrator: Vibrator
    private lateinit var layoutManager: LayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Hide system bars (Navigation bar and Status bar)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        
        // Draw across the camera cutout
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        
        viewModel = ViewModelProvider(this)[GamepadViewModel::class.java]
        layoutManager = LayoutManager(this)

        val sharedPref = getSharedPreferences("nexpad_prefs", MODE_PRIVATE)
        
        // Initialize Connection Mode
        val isBluetoothMode = sharedPref.getBoolean("BLUETOOTH_MODE", false)
        viewModel.setConnectionMode(isBluetoothMode)
        
        if (!isBluetoothMode) {
            // Start UDP Server if last IP exists
            val lastIp = sharedPref.getString("LAST_IP", "")
            if (!lastIp.isNullOrBlank()) {
                viewModel.connect(lastIp, 9999)
            }
        }

        // Initialize Sensors
        gyroSensor = GyroSensor(
            context = this,
            onGravityChanged = { x, y, z ->
                // Gravity steering is disabled on Android side.
                // Desktop app will process raw Accel/Gyro data instead.
            },
            onAccelChanged = { x, y, z -> viewModel.updateAccel(x, y, z) },
            onGyroChanged = { x, y, z -> viewModel.update6AxisGyro(x, y, z) },
            onGameRotationChanged = { x, y, z ->

            }
        )

        // Setup Vibrator
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }

        enableEdgeToEdge()
        setContent {
            NEXPADTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    NavigationGraph(
                        viewModel = viewModel,
                        layoutManager = layoutManager,
                        context = this@MainActivity,
                        onVibrate = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                            } else {
                                @Suppress("DEPRECATION")
                                vibrator.vibrate(50)
                            }
                        }
                    )
                }
            }
        }

    }

//    private fun vibrateDevice() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
//        } else {
//            @Suppress("DEPRECATION")
//            vibrator.vibrate(50)
//        }
//    }
//
//    private fun triggerRumble(leftMotor: Int, rightMotor: Int) {
//        val intensity = maxOf(leftMotor, rightMotor)
//        if (intensity == 0) {
//            vibrator.cancel()
//            return
//        }
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val amplitude = (intensity.toFloat() / 255f * 255).toInt()
//            vibrator.vibrate(VibrationEffect.createOneShot(200, amplitude))
//        } else {
//            @Suppress("DEPRECATION")
//            vibrator.vibrate(200)
//        }
//    }

    override fun onResume() {
        super.onResume()
        gyroSensor.start()
    }

    override fun onPause() {
        super.onPause()
        gyroSensor.stop()
    }
}