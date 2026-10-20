package com.sanket.tools.nexpad

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import com.sanket.tools.nexpad.runtime.registry.ComponentReloadReceiver
import com.sanket.tools.nexpad.sensors.MotionSensorCoordinator
import com.sanket.tools.nexpad.share.ShareManager
import com.sanket.tools.nexpad.ui.NavigationGraph
import com.sanket.tools.nexpad.ui.theme.NEXPADTheme
import com.sanket.tools.nexpad.utils.HapticFeedbackHelper
import com.sanket.tools.nexpad.utils.LayoutManager
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel

class MainActivity : ComponentActivity() {
    
    private lateinit var viewModel: GamepadViewModel
    private lateinit var motionSensorCoordinator: MotionSensorCoordinator
    private lateinit var hapticFeedbackHelper: HapticFeedbackHelper
    private lateinit var layoutManager: LayoutManager
    private var componentReloadReceiver: ComponentReloadReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        // Draw across the camera cutout
        window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        
        // Lock to 120Hz / highest available refresh rate for esports-grade fluid touch response
        requestHighRefreshRate()
        
        viewModel = ViewModelProvider(this)[GamepadViewModel::class.java]
        layoutManager = LayoutManager(this)

        if (intent?.action == android.hardware.usb.UsbManager.ACTION_USB_ACCESSORY_ATTACHED) {
            viewModel.checkAoaAccessory()
        }
        handleIncomingFileIntent(intent)

        // Register broadcast receiver for Desktop ADB push reload
        componentReloadReceiver = ComponentReloadReceiver.register(this, lifecycleScope)

        // We no longer auto-connect on startup. 
        // The user must click the device from the Discovery list to connect.

        // Initialize Sensors via dedicated coordinator
        motionSensorCoordinator = MotionSensorCoordinator(this, viewModel)

        // Setup HD Haptic Engine
        hapticFeedbackHelper = HapticFeedbackHelper(this)

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
                combine(viewModel.isConnected, viewModel.isGamepadScreenActive) { connected, active ->
                    Pair(connected, active)
                }.collect { (connected, active) ->
                    if (connected && active) {
                        motionSensorCoordinator.start()
                    } else {
                        motionSensorCoordinator.stop()
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
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.requestedSensorModeFlow.collect { mode ->
                    motionSensorCoordinator.setMode(mode)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.isGyroEnabled.collect { enabled ->
                motionSensorCoordinator.isGyroMuted = !enabled
            }
        }
    }

    override fun onResume() {
        super.onResume()
        requestHighRefreshRate()
        viewModel.cancelScreenOffDisconnectTimer()
        viewModel.checkAoaAccessory()
        if (viewModel.isConnected.value && viewModel.isGamepadScreenActive.value) {
            motionSensorCoordinator.start()
        }
    }

    private fun requestHighRefreshRate() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val modes = display?.supportedModes ?: emptyArray()
                val maxMode = modes.maxByOrNull { it.refreshRate }
                if (maxMode != null && maxMode.refreshRate >= 90f) {
                    val params = window.attributes
                    params.preferredDisplayModeId = maxMode.modeId
                    params.preferredRefreshRate = maxMode.refreshRate
                    window.attributes = params
                    android.util.Log.d("NEXPAD", "⚡ High Refresh Rate: ${maxMode.refreshRate}Hz (Mode ID: ${maxMode.modeId})")
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                @Suppress("DEPRECATION")
                val modes = windowManager.defaultDisplay?.supportedModes ?: emptyArray()
                val maxMode = modes.maxByOrNull { it.refreshRate }
                if (maxMode != null && maxMode.refreshRate >= 90f) {
                    val params = window.attributes
                    params.preferredDisplayModeId = maxMode.modeId
                    params.preferredRefreshRate = maxMode.refreshRate
                    window.attributes = params
                    android.util.Log.d("NEXPAD", "⚡ High Refresh Rate: ${maxMode.refreshRate}Hz (Mode ID: ${maxMode.modeId})")
                }
            }
        } catch (_: Exception) {}
    }

    override fun onPause() {
        super.onPause()
        motionSensorCoordinator.stop()
    }

    override fun onStop() {
        super.onStop()
        viewModel.startScreenOffDisconnectTimer()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == android.hardware.usb.UsbManager.ACTION_USB_ACCESSORY_ATTACHED) {
            viewModel.checkAoaAccessory()
        }
        handleIncomingFileIntent(intent)
    }

    private fun handleIncomingFileIntent(intent: Intent?) {
        if (intent == null) return
        val uri: Uri? = try {
            when (intent.action) {
                Intent.ACTION_VIEW -> intent.data
                Intent.ACTION_SEND -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(Intent.EXTRA_STREAM)
                    }
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
        if (uri == null) return

        lifecycleScope.launch(Dispatchers.IO) {
            ShareManager.handleIncomingFileUri(
                context = this@MainActivity,
                uri = uri,
                layoutManager = layoutManager
            )
        }
    }

    override fun onDestroy() {
        com.sanket.tools.nexpad.runtime.engine.clearNxprcRenderCaches()
        super.onDestroy()
        componentReloadReceiver?.unregister(this)
    }
}