package com.sanket.tools.nexpad

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.sanket.tools.nexpad.runtime.registry.ComponentRegistry
import com.sanket.tools.nexpad.runtime.plugin.RemoteComponentRegistry
import com.sanket.tools.nexpad.sensors.MotionSensorManager
import com.sanket.tools.nexpad.share.ShareManager
import com.sanket.tools.nexpad.ui.NavigationGraph
import com.sanket.tools.nexpad.ui.theme.NEXPADTheme
import com.sanket.tools.nexpad.utils.LayoutManager
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel

class MainActivity : ComponentActivity() {
    
    private lateinit var viewModel: GamepadViewModel
    private lateinit var motionSensorManager: MotionSensorManager
    private lateinit var hapticFeedbackHelper: com.sanket.tools.nexpad.utils.HapticFeedbackHelper
    private lateinit var layoutManager: LayoutManager
    private var reloadReceiver: android.content.BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        // Draw across the camera cutout
        window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        
        viewModel = ViewModelProvider(this)[GamepadViewModel::class.java]
        layoutManager = LayoutManager(this)

        if (intent?.action == android.hardware.usb.UsbManager.ACTION_USB_ACCESSORY_ATTACHED) {
            viewModel.checkAoaAccessory()
        }
        handleIncomingFileIntent(intent)

        // Register broadcast receiver for Desktop ADB push reload
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: android.content.Intent?) {
                lifecycleScope.launch(Dispatchers.IO) {
                    RemoteComponentRegistry.getInstance(this@MainActivity).reloadAll()
                    ComponentRegistry.getInstance(this@MainActivity).reloadAll()
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(this@MainActivity, "⚡ Components reloaded from Desktop!", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        reloadReceiver = receiver
        val filter = android.content.IntentFilter().apply {
            addAction("com.sanket.tools.nexpad.RELOAD_COMPONENTS")
            addAction("com.sanket.tools.nexpad.RELOAD_REMOTE_COMPONENTS")
        }
        ContextCompat.registerReceiver(
            this,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        val sharedPref = getSharedPreferences("nexpad_prefs", MODE_PRIVATE)
        // Ensure Joystick RS defaults to true analog stick behavior, keeping Touchpads (RTP) dedicated
        if (!sharedPref.getBoolean("RS_ANALOG_DEFAULT_MIGRATION_V1", false)) {
            sharedPref.edit()
                .putBoolean("RIGHT_STICK_CAMERA_MODE", false)
                .putBoolean("RS_ANALOG_DEFAULT_MIGRATION_V1", true)
                .apply()
        }
        
        // We no longer auto-connect on startup. 
        // The user must click the device from the Discovery list to connect.

        // Initialize Sensors
        motionSensorManager = MotionSensorManager(
            context = this,
            onMotionPacket = { packet ->
                when (viewModel.requestedSensorMode) {
                    com.sanket.tools.nexpad.protocol.NexpadProtocol.SENSOR_MODE_GRAVITY -> {
                        // Mode 0x01: Gravity mode for racing / steering wheel tilt
                        if (packet.gravityX != 0f || packet.gravityY != 0f || packet.gravityZ != 0f) {
                            viewModel.updateGravity(packet.gravityX, packet.gravityY, packet.gravityZ)
                        } else {
                            viewModel.updateGravity(packet.accelX, packet.accelY, packet.accelZ)
                        }
                    }
                    com.sanket.tools.nexpad.protocol.NexpadProtocol.SENSOR_MODE_GAME_ROTATION -> {
                        // Mode 0x02: 3D Orientation Quaternion for Air Mouse / Laser Pointer & VR
                        viewModel.updateGameRotation(packet.qX, packet.qY, packet.qZ, packet.qW)
                    }
                    com.sanket.tools.nexpad.protocol.NexpadProtocol.SENSOR_MODE_LINEAR_GESTURE -> {
                        // Mode 0x03: Linear Acceleration for flicks/shakes + Gravity for tilt postures
                        val gravX = if (packet.gravityX != 0f) packet.gravityX else packet.accelX
                        val gravY = if (packet.gravityY != 0f) packet.gravityY else packet.accelY
                        val gravZ = if (packet.gravityZ != 0f) packet.gravityZ else packet.accelZ
                        viewModel.updateLinearGesture(
                            packet.linearAccelX, packet.linearAccelY, packet.linearAccelZ,
                            gravX, gravY, gravZ,
                            packet.gyroX, packet.gyroY, packet.gyroZ
                        )
                    }
                    com.sanket.tools.nexpad.protocol.NexpadProtocol.SENSOR_MODE_RAW_UNCALIBRATED -> {
                        // Mode 0x04: Pure raw silicon gyro + estimated hardware bias
                        viewModel.updateRawUncalibrated(
                            packet.rawGyroX, packet.rawGyroY, packet.rawGyroZ,
                            packet.biasX, packet.biasY, packet.biasZ
                        )
                    }
                    else -> {
                        // Mode 0x00: Standard 6-axis IMU (gyro + raw accel) for DS4 and FPS aiming
                        viewModel.updateAccel(packet.accelX, packet.accelY, packet.accelZ)
                        viewModel.update6AxisGyro(packet.gyroX, packet.gyroY, packet.gyroZ)
                    }
                }
            }
        )

        // Setup HD Haptic Engine
        hapticFeedbackHelper = com.sanket.tools.nexpad.utils.HapticFeedbackHelper(this)

        setContent {
            NEXPADTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavigationGraph(
                        viewModel = viewModel,
                        layoutManager = layoutManager,
                        onVibrate = {
                            hapticFeedbackHelper.performButtonClick()
                        }
                    )
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                kotlinx.coroutines.flow.combine(viewModel.isConnected, viewModel.isGamepadScreenActive) { connected, active ->
                    Pair(connected, active)
                }.collect { (connected, active) ->
                    if (connected && active) {
                        motionSensorManager.start()
                    } else {
                        motionSensorManager.stop()
                    }
                    if (connected) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.isGyroEnabled.collect { enabled ->
                motionSensorManager.isGyroMuted = !enabled
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.cancelScreenOffDisconnectTimer()
        viewModel.checkAoaAccessory()
        if (viewModel.isConnected.value && viewModel.isGamepadScreenActive.value) {
            motionSensorManager.start()
        }
    }

    override fun onPause() {
        super.onPause()
        motionSensorManager.stop()
    }

    override fun onStop() {
        super.onStop()
        viewModel.startScreenOffDisconnectTimer()
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == android.hardware.usb.UsbManager.ACTION_USB_ACCESSORY_ATTACHED) {
            viewModel.checkAoaAccessory()
        }
        handleIncomingFileIntent(intent)
    }

    private fun handleIncomingFileIntent(intent: android.content.Intent?) {
        if (intent == null) return
        if (intent.action == android.content.Intent.ACTION_VIEW && intent.data != null) {
            val uri = intent.data ?: return
            lifecycleScope.launch(Dispatchers.IO) {
                ShareManager.handleIncomingFileUri(
                    context = this@MainActivity,
                    uri = uri,
                    layoutManager = layoutManager
                )
            }
        }
    }


    override fun onDestroy() {
        com.sanket.tools.nexpad.runtime.engine.clearNxprcRenderCaches()
        super.onDestroy()
        reloadReceiver?.let {
            try { unregisterReceiver(it) } catch (_: Exception) {}
        }
    }
}