package com.sanket.tools.nexpad.ui.components.controller

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel
import kotlin.math.roundToInt

/**
 * Ergonomic 3D shoulder bumper with physical spring compression,
 * textured micro-grip ridges, specular chrome chamfer, and RGB neon bloom.
 */
@Composable
fun RealisticBumper(
    key: String,
    isConnected: Boolean,
    onVibrate: () -> Unit,
    viewModel: GamepadViewModel,
    isRgbEnabled: Boolean,
    modifier: Modifier = Modifier,
    displayLabel: String? = null
) {
    var isPressed by remember { mutableStateOf(false) }
    val isLeft = key.uppercase() == com.sanket.tools.nexpad.model.NexpadKeys.LB

    val bumperShape = remember(isLeft) {
        if (isLeft) {
            RoundedCornerShape(topStart = 38.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 10.dp)
        } else {
            RoundedCornerShape(topStart = 16.dp, topEnd = 38.dp, bottomStart = 10.dp, bottomEnd = 16.dp)
        }
    }

    val scaleAnim by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 650f),
        label = "bumper_scale"
    )
    val pressOffsetYAnim by animateFloatAsState(
        targetValue = if (isPressed) 2.5f else 0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 650f),
        label = "bumper_offset"
    )
    val rgbBloomAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 0.35f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 800f),
        label = "bumper_rgb_bloom"
    )

    val neonColor = if (isLeft) Color(0xFF7C3AED) else Color(0xFF00E5FF)

    val baseGradient = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF3A4454),
                Color(0xFF222934),
                Color(0xFF13171F)
            )
        )
    }

    val pressedGradient = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1D222A),
                Color(0xFF11141A),
                Color(0xFF090B0E)
            )
        )
    }

    Box(
        modifier = modifier
            .size(160.dp, 60.dp)
            .drawBehind {
                if (isRgbEnabled) {
                    drawRoundRect(
                        color = neonColor.copy(alpha = rgbBloomAlpha * 0.40f),
                        size = size.copy(width = size.width + 16.dp.toPx(), height = size.height + 14.dp.toPx()),
                        topLeft = Offset(-8.dp.toPx(), -7.dp.toPx()),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx(), 24.dp.toPx())
                    )
                }
            }
            .graphicsLayer {
                scaleX = scaleAnim
                scaleY = scaleAnim
            }
            .offset { IntOffset(0, pressOffsetYAnim.dp.roundToPx()) }
            .shadow(
                elevation = if (isPressed) 3.dp else 10.dp,
                shape = bumperShape,
                spotColor = if (isRgbEnabled) neonColor else Color.Black,
                ambientColor = if (isRgbEnabled) neonColor else Color.Black
            )
            .clip(bumperShape)
            .background(if (isPressed) pressedGradient else baseGradient)
            .border(
                width = 1.5.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF718096).copy(alpha = if (isPressed) 0.3f else 0.7f),
                        Color(0xFF1A202C)
                    )
                ),
                shape = bumperShape
            )
            .pointerInput(key) {
                detectTapGestures(
                    onPress = {
                        onVibrate()
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
        // Specular Top Bevel & Tactile Grip Texture Lines
        Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
            val startX = if (isLeft) 28f else 12f
            val endX = if (isLeft) size.width - 12f else size.width - 28f

            // Top specular chamfer edge
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isPressed) 0.15f else 0.50f),
                        Color.White.copy(alpha = if (isPressed) 0.05f else 0.20f)
                    ),
                    startX = startX,
                    endX = endX
                ),
                start = Offset(startX, 6f),
                end = Offset(endX, 6f),
                strokeWidth = 2.5f
            )

            // Tactile anti-slip micro ridges (three subtle laser-etched lines)
            val ridgeStartX = if (isLeft) size.width * 0.18f else size.width * 0.35f
            val ridgeEndX = if (isLeft) size.width * 0.65f else size.width * 0.82f
            for (i in 0..2) {
                val y = size.height * 0.68f + (i * 6f)
                drawLine(
                    color = Color.Black.copy(alpha = 0.45f),
                    start = Offset(ridgeStartX, y),
                    end = Offset(ridgeEndX, y),
                    strokeWidth = 2f
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.15f),
                    start = Offset(ridgeStartX, y + 1f),
                    end = Offset(ridgeEndX, y + 1f),
                    strokeWidth = 1f
                )
            }
        }

        val labelColor = if (isRgbEnabled) {
            if (isLeft) Color(0xFFC4B5FD) else Color(0xFF67E8F9)
        } else {
            Color.White.copy(alpha = if (isPressed) 0.70f else 0.95f)
        }

        Text(
            text = displayLabel ?: key,
            color = labelColor,
            fontWeight = FontWeight.Black,
            fontSize = 19.sp,
            style = androidx.compose.ui.text.TextStyle(
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = if (isRgbEnabled) neonColor.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.8f),
                    offset = Offset(0f, 2f),
                    blurRadius = if (isRgbEnabled) 8f else 3f
                )
            )
        )
    }
}
