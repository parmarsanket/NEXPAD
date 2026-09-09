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
import com.sanket.tools.nexpad.sensors.MotionSensorManager
import com.sanket.tools.nexpad.ui.GamepadScreen
import com.sanket.tools.nexpad.ui.theme.NEXPADTheme
import com.sanket.tools.nexpad.ui.NavigationGraph
import androidx.lifecycle.lifecycleScope
import com.sanket.tools.nexpad.runtime.network.FtpTransferReceiver
import com.sanket.tools.nexpad.runtime.registry.ComponentRegistry
import com.sanket.tools.nexpad.utils.LayoutManager
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel

class MainActivity : ComponentActivity() {
    
    private lateinit var viewModel: GamepadViewModel
    private lateinit var motionSensorManager: MotionSensorManager
    private lateinit var vibrator: Vibrator
    private lateinit var layoutManager: LayoutManager
    private var ftpTransferReceiver: FtpTransferReceiver? = null
    private var reloadReceiver: android.content.BroadcastReceiver? = null

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

        if (intent?.action == android.hardware.usb.UsbManager.ACTION_USB_ACCESSORY_ATTACHED) {
            viewModel.checkAoaAccessory()
        }

        // Start FTP File Transfer Receiver for Desktop Component Transfer
        ftpTransferReceiver = FtpTransferReceiver(this, lifecycleScope).apply { start() }

        // Register broadcast receiver for Desktop ADB push reload
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: android.content.Intent?) {
                ComponentRegistry.getInstance(this@MainActivity).reloadAll()
                android.widget.Toast.makeText(this@MainActivity, "⚡ Components reloaded from Desktop!", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
        reloadReceiver = receiver
        val filter = android.content.IntentFilter("com.sanket.tools.nexpad.RELOAD_COMPONENTS")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(receiver, filter)
        }

        val sharedPref = getSharedPreferences("nexpad_prefs", MODE_PRIVATE)
        
        // We no longer auto-connect on startup. 
        // The user must click the device from the Discovery list to connect.

        // Initialize Sensors
        motionSensorManager = MotionSensorManager(
            context = this,
            onMotionPacket = { packet ->
                // Send filtered Gravity data when available, fallback to raw Accelerometer
                if (packet.gravityX != 0f || packet.gravityY != 0f || packet.gravityZ != 0f) {
                    viewModel.updateGravity(packet.gravityX, packet.gravityY, packet.gravityZ)
                } else {
                    viewModel.updateAccel(packet.accelX, packet.accelY, packet.accelZ)
                }
                
                // Fallback to calibrated gyro if the device doesn't support uncalibrated gyro
                val gX = if (packet.rawGyroX != 0f) packet.rawGyroX else packet.gyroX
                val gY = if (packet.rawGyroY != 0f) packet.rawGyroY else packet.gyroY
                val gZ = if (packet.rawGyroZ != 0f) packet.rawGyroZ else packet.gyroZ
                viewModel.update6AxisGyro(gX, gY, gZ)
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
                    color = MaterialTheme.colorScheme.background
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
        viewModel.checkAoaAccessory()
        motionSensorManager.start()
    }

    override fun onPause() {
        super.onPause()
        motionSensorManager.stop()
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        if (intent.action == android.hardware.usb.UsbManager.ACTION_USB_ACCESSORY_ATTACHED) {
            viewModel.checkAoaAccessory()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ftpTransferReceiver?.stop()
        reloadReceiver?.let {
            try { unregisterReceiver(it) } catch (_: Exception) {}
        }
    }
}