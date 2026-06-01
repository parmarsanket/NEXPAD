package com.sanket.tools.nexpad.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.R
import com.sanket.tools.nexpad.model.Position
import com.sanket.tools.nexpad.ui.components.CroppedImage
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
    
    // Fallback UI if we just use standard buttons for now, but absolutely positioned!
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Back Button
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(16.dp)) {
            Text("⬅️", fontSize = 24.sp)
        }

        // Render each mapped component
        profile.positions.forEach { (key, position) ->
            val offsetX = (position.xRatio * screenWidth).roundToInt()
            val offsetY = (position.yRatio * screenHeight).roundToInt()
            
            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX, offsetY) }
            ) {
                // Here we would use CroppedImage for actual images. 
                // For demonstration, we use our highly functional GamepadButton but floating!
                DraggableGamepadButton(key = key, isConnected = true, onVibrate = onVibrate, viewModel = viewModel, isRgbEnabled = profile.isRgbEnabled)
            }
        }
    }
}

@Composable
fun DraggableGamepadButton(key: String, isConnected: Boolean, onVibrate: () -> Unit, viewModel: GamepadViewModel, isRgbEnabled: Boolean) {
    var isPressed by remember { mutableStateOf(false) }
    
    // Simulating CroppedImage for now with text until exact pixels are tuned
    Box(
        modifier = Modifier
            .size(80.dp)
            .background(
                color = if (isPressed) Color.DarkGray else Color.LightGray,
                shape = androidx.compose.foundation.shape.CircleShape
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        if (isConnected) onVibrate()
                        isPressed = true
                        viewModel.updateButton(key, true)
                        tryAwaitRelease()
                        isPressed = false
                        viewModel.updateButton(key, false)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(key, color = Color.White, fontWeight = FontWeight.Bold)
    }
}
