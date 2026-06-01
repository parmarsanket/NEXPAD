package com.sanket.tools.nexpad.ui

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.R
import com.sanket.tools.nexpad.ui.components.RealisticButton
import com.sanket.tools.nexpad.ui.components.RealisticDPad
import com.sanket.tools.nexpad.ui.components.RealisticJoystick
import com.sanket.tools.nexpad.utils.LayoutManager
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel
import kotlin.math.roundToInt

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
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Blank Controller Shell Background
        Image(
            painter = painterResource(id = R.drawable.blank_controller),
            contentDescription = "Controller Shell",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        // Back Button
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(16.dp)) {
            Text("⬅️", fontSize = 24.sp, color = Color.White)
        }

        // Render each mapped component using the 3D Canvas Composables
        profile.positions.forEach { (key, position) ->
            val offsetX = (position.xRatio * screenWidth).roundToInt()
            val offsetY = (position.yRatio * screenHeight).roundToInt()
            
            Box(
                modifier = Modifier.offset { IntOffset(offsetX, offsetY) }
            ) {
                when {
                    key == "L3" -> RealisticJoystick(isLeft = true, isConnected = true, viewModel = viewModel, isRgbEnabled = profile.isRgbEnabled)
                    key == "R3" -> RealisticJoystick(isLeft = false, isConnected = true, viewModel = viewModel, isRgbEnabled = profile.isRgbEnabled)
                    key == "DPAD" -> RealisticDPad(isConnected = true, viewModel = viewModel, isRgbEnabled = profile.isRgbEnabled)
                    key == "A" -> RealisticButton(key = "A", buttonColor = Color(0xFF00C853), isConnected = true, onVibrate = onVibrate, viewModel = viewModel, isRgbEnabled = profile.isRgbEnabled)
                    key == "B" -> RealisticButton(key = "B", buttonColor = Color(0xFFD50000), isConnected = true, onVibrate = onVibrate, viewModel = viewModel, isRgbEnabled = profile.isRgbEnabled)
                    key == "X" -> RealisticButton(key = "X", buttonColor = Color(0xFF2962FF), isConnected = true, onVibrate = onVibrate, viewModel = viewModel, isRgbEnabled = profile.isRgbEnabled)
                    key == "Y" -> RealisticButton(key = "Y", buttonColor = Color(0xFFFFD600), isConnected = true, onVibrate = onVibrate, viewModel = viewModel, isRgbEnabled = profile.isRgbEnabled)
                    key == "GUIDE" -> RealisticButton(key = "X", buttonColor = Color.White, isConnected = true, onVibrate = onVibrate, viewModel = viewModel, isRgbEnabled = profile.isRgbEnabled)
                    else -> RealisticButton(key = key, buttonColor = Color.Gray, isConnected = true, onVibrate = onVibrate, viewModel = viewModel, isRgbEnabled = profile.isRgbEnabled)
                }
            }
        }
    }
}
