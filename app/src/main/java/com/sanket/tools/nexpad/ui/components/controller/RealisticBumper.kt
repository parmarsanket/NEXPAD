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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel

@Composable
fun RealisticBumper(
    key: String,
    isConnected: Boolean,
    onVibrate: () -> Unit,
    viewModel: GamepadViewModel,
    isRgbEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val isLeft = key.uppercase() == "LB"

    val bumperShape = if (isLeft) {
        RoundedCornerShape(topStart = 40.dp, topEnd = 14.dp, bottomStart = 14.dp, bottomEnd = 8.dp)
    } else {
        RoundedCornerShape(topStart = 14.dp, topEnd = 40.dp, bottomStart = 8.dp, bottomEnd = 14.dp)
    }

    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF424242), Color(0xFF1A1A1A)),
        startY = 0f,
        endY = 100f
    )

    val rgbShadow = if (isRgbEnabled) {
        val spot = if (isLeft) Color(0xFF7C3AED) else Color(0xFF00E5FF)
        val ambient = if (isLeft) Color(0xFFA78BFA) else Color(0xFF10B981)
        Modifier.shadow(12.dp, bumperShape, spotColor = spot, ambientColor = ambient)
    } else {
        Modifier.shadow(8.dp, bumperShape)
    }

    Box(
        modifier = modifier
            .size(160.dp, 60.dp)
            .then(rgbShadow)
            .clip(bumperShape)
            .background(if (isPressed) Color(0xFF111111) else Color.Transparent)
            .background(if (isPressed) Brush.verticalGradient(listOf(Color.Black, Color.DarkGray)) else gradient)
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
        Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
            val startX = if (isLeft) 24f else 10f
            val endX = if (isLeft) size.width - 10f else size.width - 24f
            drawLine(
                color = Color.White.copy(alpha = 0.25f),
                start = Offset(startX, 10f),
                end = Offset(endX, 10f),
                strokeWidth = 2f
            )
        }
        val labelColor = if (isRgbEnabled) {
            if (isLeft) Color(0xFFA78BFA) else Color(0xFF00E5FF)
        } else {
            Color.White.copy(alpha = 0.75f)
        }
        Text(key, color = labelColor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}
