package com.sanket.tools.nexpad.ui.components.controller

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel

@Composable
fun RealisticButton(
    key: String, 
    buttonColor: Color,
    isConnected: Boolean, 
    onVibrate: () -> Unit, 
    viewModel: GamepadViewModel, 
    isRgbEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    val baseGradient = Brush.radialGradient(
        colors = listOf(
            Color(0xFF3A3A3A),
            Color(0xFF151515)
        ),
        center = Offset(0.3f, 0.3f),
        radius = 150f
    )

    val pressedGradient = Brush.radialGradient(
        colors = listOf(
            Color(0xFF222222),
            Color(0xFF000000)
        ),
        center = Offset(0.5f, 0.5f),
        radius = 200f
    )

    val rgbShadow = if (isRgbEnabled) {
        Modifier.shadow(
            elevation = if (isPressed) 20.dp else 10.dp,
            shape = CircleShape,
            ambientColor = buttonColor,
            spotColor = buttonColor
        )
    } else Modifier.shadow(
        elevation = if (isPressed) 2.dp else 8.dp,
        shape = CircleShape,
        ambientColor = Color.Black,
        spotColor = Color.Black
    )

    Box(
        modifier = modifier
            .size(80.dp)
            .then(rgbShadow)
            .clip(CircleShape)
            .background(if (isPressed) pressedGradient else baseGradient)
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
        // Draw the glossy 3D highlight
        Canvas(modifier = Modifier.fillMaxSize().padding(2.dp)) {
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(Color.White.copy(alpha = 0.4f), Color.Transparent),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height * 0.5f)
                ),
                radius = size.minDimension / 2f,
                style = Stroke(width = 4f)
            )
        }

        // Colored Text for the button (e.g. A, B, X, Y)
        Text(
            text = key,
            color = buttonColor.copy(alpha = if (isPressed) 0.6f else 1f),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
