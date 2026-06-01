package com.sanket.tools.nexpad.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel

@Composable
fun RealisticTrigger(
    key: String,
    isConnected: Boolean,
    onVibrate: () -> Unit,
    viewModel: GamepadViewModel,
    isRgbEnabled: Boolean
) {
    var isPressed by remember { mutableStateOf(false) }

    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF222222), Color(0xFF000000)),
        startY = 0f,
        endY = 200f
    )

    val rgbShadow = if (isRgbEnabled) {
        Modifier.shadow(16.dp, RoundedCornerShape(bottomStart = 50.dp, bottomEnd = 50.dp, topStart = 10.dp, topEnd = 10.dp), spotColor = Color.Magenta, ambientColor = Color.Red)
    } else {
        Modifier.shadow(12.dp, RoundedCornerShape(bottomStart = 50.dp, bottomEnd = 50.dp, topStart = 10.dp, topEnd = 10.dp))
    }

    Box(
        modifier = Modifier
            .size(100.dp, 160.dp)
            .then(rgbShadow)
            .clip(RoundedCornerShape(bottomStart = 50.dp, bottomEnd = 50.dp, topStart = 10.dp, topEnd = 10.dp))
            .background(if (isPressed) Color.Black else Color.Transparent)
            .background(gradient)
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
        contentAlignment = Alignment.BottomCenter
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                color = Color.Black.copy(alpha = 0.5f),
                size = androidx.compose.ui.geometry.Size(size.width, size.height / 2),
                topLeft = Offset(0f, size.height / 2)
            )
        }
        Text(key, color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold, fontSize = 24.sp, modifier = Modifier.padding(bottom = 20.dp))
    }
}
