package com.sanket.tools.nexpad.ui

import android.content.Context
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
    val isGyroSteeringEnabled by viewModel.isGyroSteeringEnabled.collectAsState()
    
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.feedbackFlow.collect { feedback ->
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            val totalSpeed = (feedback.leftMotorSpeed + feedback.rightMotorSpeed) / 2
            if (totalSpeed > 0) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(100, (totalSpeed / 65535f * 255).toInt().coerceIn(1, 255)))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(100)
                }
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
    ) {
        // Back Button & Gyro Toggle
        Row(
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Text("⬅️", fontSize = 24.sp, color = Color.White)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(if (isConnected) "🟢 Connected" else "🔴 Disconnected", color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(32.dp))
            Text("Gyro Steering: ", color = Color.White, fontWeight = FontWeight.Bold)
            Switch(
                checked = isGyroSteeringEnabled,
                onCheckedChange = { viewModel.toggleGyroSteering() },
                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E676), checkedTrackColor = Color.DarkGray)
            )
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
                    key == "LT" || key == "RT" -> RealisticTrigger(key = key, isConnected = isConnected, onVibrate = onVibrate, viewModel = viewModel, isRgbEnabled = profile.isRgbEnabled)
                    key == "LB" || key == "RB" -> RealisticBumper(key = key, isConnected = isConnected, onVibrate = onVibrate, viewModel = viewModel, isRgbEnabled = profile.isRgbEnabled)
                    key == "A" -> RealisticButton(key = "A", buttonColor = Color(0xFF00C853), isConnected = isConnected, onVibrate = onVibrate, viewModel = viewModel, isRgbEnabled = profile.isRgbEnabled)
                    key == "B" -> RealisticButton(key = "B", buttonColor = Color(0xFFD50000), isConnected = isConnected, onVibrate = onVibrate, viewModel = viewModel, isRgbEnabled = profile.isRgbEnabled)
                    key == "X" -> RealisticButton(key = "X", buttonColor = Color(0xFF2962FF), isConnected = isConnected, onVibrate = onVibrate, viewModel = viewModel, isRgbEnabled = profile.isRgbEnabled)
                    key == "Y" -> RealisticButton(key = "Y", buttonColor = Color(0xFFFFD600), isConnected = isConnected, onVibrate = onVibrate, viewModel = viewModel, isRgbEnabled = profile.isRgbEnabled)
                    key in listOf("MENU", "VIEW", "XBOX", "SHARE", "SCREENSHOT") -> RealisticSystemButton(key = key, isConnected = isConnected, onVibrate = onVibrate, viewModel = viewModel, isRgbEnabled = profile.isRgbEnabled)
                    key in listOf("M1", "M2", "M3", "M4", "PROFILE", "TURBO") -> RealisticMacroButton(key = key, isConnected = isConnected, onVibrate = onVibrate, viewModel = viewModel, isRgbEnabled = profile.isRgbEnabled)
                    else -> RealisticButton(key = key, buttonColor = Color.Gray, isConnected = isConnected, onVibrate = onVibrate, viewModel = viewModel, isRgbEnabled = profile.isRgbEnabled)
                }
            }
        }
    }
}
