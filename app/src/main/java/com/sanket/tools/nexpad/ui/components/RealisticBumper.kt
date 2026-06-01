package com.sanket.tools.nexpad.ui.components

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
    isRgbEnabled: Boolean
) {
    var isPressed by remember { mutableStateOf(false) }

    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF424242), Color(0xFF1E1E1E)),
        startY = 0f,
        endY = 100f
    )

    val rgbShadow = if (isRgbEnabled) {
        Modifier.shadow(12.dp, RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp, bottomStart = 10.dp, bottomEnd = 10.dp), spotColor = Color.Cyan, ambientColor = Color.Blue)
    } else {
        Modifier.shadow(8.dp, RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp, bottomStart = 10.dp, bottomEnd = 10.dp))
    }

    Box(
        modifier = Modifier
            .size(160.dp, 60.dp)
            .then(rgbShadow)
            .clip(RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp, bottomStart = 10.dp, bottomEnd = 10.dp))
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
            drawLine(
                color = Color.White.copy(alpha = 0.2f),
                start = Offset(10f, 10f),
                end = Offset(size.width - 10f, 10f),
                strokeWidth = 2f
            )
        }
        Text(key, color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}
