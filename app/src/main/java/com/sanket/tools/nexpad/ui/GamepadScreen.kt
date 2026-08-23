package com.sanket.tools.nexpad.ui

import android.content.Context
import android.content.pm.ActivityInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.ui.components.*
import com.sanket.tools.nexpad.utils.LayoutManager
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel
import kotlin.math.roundToInt
import android.os.Vibrator
import android.os.Build
import android.os.VibrationEffect
import com.sanket.tools.nexpad.utils.LockScreenOrientation

@Composable
fun GamepadScreen(
    viewModel: GamepadViewModel, 
    layoutManager: LayoutManager,
    onBack: () -> Unit,
    onVibrate: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp.value
    val screenHeight = configuration.screenHeightDp.dp.value

    val profile = layoutManager.getActiveProfile()
    val isConnected by viewModel.isConnected.collectAsState()
    
    val context = androidx.compose.ui.platform.LocalContext.current
    LockScreenOrientation(
        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    )

    // Initialize vibrator and amplitude control check ONCE outside the flow loop
    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(android.os.VibratorManager::class.java)
            vibratorManager?.defaultVibrator ?: context.getSystemService(android.os.Vibrator::class.java)!!
        } else {
            context.getSystemService(android.os.Vibrator::class.java)!!
        }
    }
    
    val hasAmplitudeControl = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.hasAmplitudeControl()
        } else {
            false
        }
    }

    var isRumbling by remember { mutableStateOf(false) }
    
    val safeOnVibrate: () -> Unit = remember { {
        if (!isRumbling) {
            onVibrate()
        }
    } }

    LaunchedEffect(Unit) {
        val sharedPref = context.getSharedPreferences("nexpad_prefs", android.content.Context.MODE_PRIVATE)
        
        viewModel.feedbackFlow.collect { feedback ->
            val intensityScalar = sharedPref.getFloat("RUMBLE_INTENSITY", 1.0f)
            val totalSpeed = (maxOf(feedback.leftMotorSpeed, feedback.rightMotorSpeed) * intensityScalar).toInt()
            
            isRumbling = totalSpeed > 0
            if (totalSpeed > 0) {
                // Vibrate for 60ms (bridges the 33ms ping gap + 27ms safety margin for dropped packets)
                @Suppress("DEPRECATION")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && hasAmplitudeControl) {
                    vibrator.vibrate(VibrationEffect.createOneShot(60, totalSpeed.coerceIn(1, 255)))
                } else {
                    vibrator.vibrate(60) // Safe fallback for cheap/old phones
                }
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Back Button & Connection Status
        Row(
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Text("⬅️", fontSize = 24.sp, color = MaterialTheme.colorScheme.onBackground)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(if (isConnected) "🟢 Connected" else "🔴 Disconnected", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.labelMedium)
        }

        // Render mapped components
        profile.positions.forEach { (key, position) ->
            val offsetX = (position.xRatio * screenWidth).roundToInt()
            val offsetY = (position.yRatio * screenHeight).roundToInt()
            
            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX, offsetY) }
                    .scale(position.scale)
                    .alpha(position.opacity)
            ) {
                when {
                    key == "LS" -> RealisticJoystick(isLeft = true, isConnected = isConnected, viewModel = viewModel, isRgbEnabled = profile.isRgbEnabled)
                    key == "RS" -> RealisticJoystick(isLeft = false, isConnected = isConnected, viewModel = viewModel, isRgbEnabled = profile.isRgbEnabled)
                    key == "DPAD" -> RealisticDPad(isConnected = isConnected, viewModel = viewModel, isRgbEnabled = profile.isRgbEnabled)
                    key == "LT" || key == "RT" -> RealisticTrigger(key = key, isConnected = isConnected, onVibrate = safeOnVibrate, viewModel = viewModel, isRgbEnabled = profile.isRgbEnabled)
                    key == "LB" || key == "RB" -> RealisticBumper(key = key, isConnected = isConnected, onVibrate = safeOnVibrate, viewModel = viewModel, isRgbEnabled = profile.isRgbEnabled)
                    key == "A" -> RealisticButton(key = "A", buttonColor = Color(0xFF00C853), isConnected = isConnected, onVibrate = safeOnVibrate, viewModel = viewModel, isRgbEnabled = profile.isRgbEnabled)
                    key == "B" -> RealisticButton(key = "B", buttonColor = Color(0xFFD50000), isConnected = isConnected, onVibrate = safeOnVibrate, viewModel = viewModel, isRgbEnabled = profile.isRgbEnabled)
                    key == "X" -> RealisticButton(key = "X", buttonColor = Color(0xFF2962FF), isConnected = isConnected, onVibrate = safeOnVibrate, viewModel = viewModel, isRgbEnabled = profile.isRgbEnabled)
                    key == "Y" -> RealisticButton(key = "Y", buttonColor = Color(0xFFFFD600), isConnected = isConnected, onVibrate = safeOnVibrate, viewModel = viewModel, isRgbEnabled = profile.isRgbEnabled)
                    key in listOf("MENU", "VIEW", "XBOX", "SHARE", "SCREENSHOT") -> RealisticSystemButton(key = key, isConnected = isConnected, onVibrate = safeOnVibrate, viewModel = viewModel, isRgbEnabled = profile.isRgbEnabled)
                    key in listOf("M1", "M2", "M3", "M4", "PROFILE", "TURBO") -> RealisticMacroButton(key = key, isConnected = isConnected, onVibrate = safeOnVibrate, viewModel = viewModel, isRgbEnabled = profile.isRgbEnabled)
                    else -> RealisticButton(key = key, buttonColor = Color.Gray, isConnected = isConnected, onVibrate = safeOnVibrate, viewModel = viewModel, isRgbEnabled = profile.isRgbEnabled)
                }
            }
        }
    }
}
