package com.sanket.tools.nexpad.ui.components.controller

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.geometry.Size
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
    isRgbEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val isLeft = key.uppercase() == "LT"

    val triggerShape = if (isLeft) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 8.dp, bottomStart = 50.dp, bottomEnd = 24.dp)
    } else {
        RoundedCornerShape(topStart = 8.dp, topEnd = 16.dp, bottomStart = 24.dp, bottomEnd = 50.dp)
    }

    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF262626), Color(0xFF0A0A0A)),
        startY = 0f,
        endY = 200f
    )

    val rgbShadow = if (isRgbEnabled) {
        val spot = if (isLeft) Color(0xFF00E5FF) else Color(0xFFFF0055)
        val ambient = if (isLeft) Color(0xFF0052CC) else Color(0xFFCC0044)
        Modifier.shadow(16.dp, triggerShape, spotColor = spot, ambientColor = ambient)
    } else {
        Modifier.shadow(12.dp, triggerShape)
    }

    Box(
        modifier = modifier
            .size(100.dp, 160.dp)
            .then(rgbShadow)
            .clip(triggerShape)
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
                size = Size(size.width, size.height / 2),
                topLeft = Offset(0f, size.height / 2)
            )
        }
        val labelColor = if (isRgbEnabled) {
            if (isLeft) Color(0xFF00E5FF).copy(alpha = 0.85f) else Color(0xFFFF0055).copy(alpha = 0.85f)
        } else {
            Color.White.copy(alpha = 0.65f)
        }
        Text(key, color = labelColor, fontWeight = FontWeight.Bold, fontSize = 24.sp, modifier = Modifier.padding(bottom = 20.dp))
    }
}
