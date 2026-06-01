package com.sanket.tools.nexpad.ui

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
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

data class CropData(val x: Int, val y: Int, val w: Int, val h: Int)

val buttonCrops = mapOf(
    "L2" to CropData(124, 82, 140, 80),
    "L1" to CropData(118, 170, 110, 60),
    "DPAD" to CropData(300, 510, 140, 140),
    "L3" to CropData(200, 340, 150, 150),
    "SELECT" to CropData(400, 380, 50, 50),
    "GUIDE" to CropData(460, 230, 80, 80),
    "START" to CropData(520, 380, 50, 50),
    "R2" to CropData(724, 82, 140, 80),
    "R1" to CropData(740, 170, 110, 60),
    "R3" to CropData(540, 530, 150, 150),
    "ABXY" to CropData(610, 290, 190, 250)
)

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
        // Base controller image as the background shell
        Image(
            painter = painterResource(id = R.drawable.realistic_controller),
            contentDescription = "Controller Shell",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
            alpha = 0.3f // Dim the background so the draggable buttons pop out
        )

        // Back Button
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(16.dp)) {
            Text("⬅️", fontSize = 24.sp)
        }

        // Render each mapped component using the hyper-realistic cropped image portions
        profile.positions.forEach { (key, position) ->
            val offsetX = (position.xRatio * screenWidth).roundToInt()
            val offsetY = (position.yRatio * screenHeight).roundToInt()
            
            Box(
                modifier = Modifier.offset { IntOffset(offsetX, offsetY) }
            ) {
                DraggableGamepadButton(key = key, isConnected = true, onVibrate = onVibrate, viewModel = viewModel, isRgbEnabled = profile.isRgbEnabled)
            }
        }
    }
}

@Composable
fun DraggableGamepadButton(key: String, isConnected: Boolean, onVibrate: () -> Unit, viewModel: GamepadViewModel, isRgbEnabled: Boolean) {
    var isPressed by remember { mutableStateOf(false) }
    val crop = buttonCrops[key]
    
    Box(
        modifier = Modifier
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
        if (crop != null) {
            // Draw the actual highly realistic button from the image!
            CroppedImage(
                imageRes = R.drawable.realistic_controller,
                srcOffsetX = crop.x,
                srcOffsetY = crop.y,
                cropWidth = crop.w,
                cropHeight = crop.h,
                targetWidthDp = (crop.w * 0.4f).toInt(), // Scale it down visually
                targetHeightDp = (crop.h * 0.4f).toInt(),
                modifier = if (isPressed) Modifier.background(Color.White.copy(alpha=0.3f)) else Modifier
            )
        } else {
            // Fallback for buttons not yet mapped with coordinates
            Box(modifier = Modifier.size(60.dp).background(if (isPressed) Color.DarkGray else Color.LightGray, shape = androidx.compose.foundation.shape.CircleShape), contentAlignment = Alignment.Center) {
                Text(key, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}
