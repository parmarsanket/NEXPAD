package com.sanket.tools.nexpad.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun RealisticJoystick(
    isLeft: Boolean,
    isConnected: Boolean,
    viewModel: GamepadViewModel,
    isRgbEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    var thumbOffsetX by remember { mutableFloatStateOf(0f) }
    var thumbOffsetY by remember { mutableFloatStateOf(0f) }
    val maxRadius = 80f

    val baseGradient = Brush.radialGradient(
        colors = listOf(Color(0xFF1F1F1F), Color(0xFF080808)),
        center = Offset(0.5f, 0.5f),
        radius = 250f
    )

    val thumbGradient = Brush.radialGradient(
        colors = listOf(Color(0xFF3D3D3D), Color(0xFF1A1A1A)),
        center = Offset(0.3f, 0.3f),
        radius = 150f
    )

    val rgbShadow = if (isRgbEnabled) {
        Modifier.shadow(15.dp, CircleShape, ambientColor = if (isLeft) Color.Cyan else Color.Magenta, spotColor = if (isLeft) Color.Blue else Color.Red)
    } else {
        Modifier.shadow(10.dp, CircleShape, ambientColor = Color.Black, spotColor = Color.Black)
    }

    Box(
        modifier = modifier
            .size(150.dp)
            .then(rgbShadow)
            .clip(CircleShape)
            .background(baseGradient),
        contentAlignment = Alignment.Center
    ) {
        // Inner depth shadow
        Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
            drawCircle(
                color = Color.Black.copy(alpha = 0.8f),
                radius = size.minDimension / 2f
            )
        }

        val coroutineScope = rememberCoroutineScope()

        // Thumbstick
        Box(
            modifier = Modifier
                .offset { IntOffset(thumbOffsetX.roundToInt(), thumbOffsetY.roundToInt()) }
                .size(90.dp)
                .shadow(12.dp, CircleShape)
                .clip(CircleShape)
                .background(thumbGradient)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            val buttonName = if (isLeft) "L3" else "R3"
                            android.util.Log.d("NEXPAD_DEBUG", "Single Tap -> Triggered $buttonName")
                            coroutineScope.launch {
                                viewModel.updateButton(buttonName, true)
                                kotlinx.coroutines.delay(100.milliseconds)
                                viewModel.updateButton(buttonName, false)
                            }
                        },
                        onDoubleTap = {
                            val buttonName = if (isLeft) "L3" else "R3"
                            android.util.Log.d("NEXPAD_DEBUG", "Double Tap -> Triggered $buttonName")
                            coroutineScope.launch {
                                viewModel.updateButton(buttonName, true)
                                kotlinx.coroutines.delay(300.milliseconds) // longer hold for double tap
                                viewModel.updateButton(buttonName, false)
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            thumbOffsetX = 0f
                            thumbOffsetY = 0f
                            if (isLeft) viewModel.updateLeftStick(0f, 0f)
                            else viewModel.updateRightStick(0f, 0f)
                        },
                        onDragCancel = {
                            thumbOffsetX = 0f
                            thumbOffsetY = 0f
                            if (isLeft) viewModel.updateLeftStick(0f, 0f)
                            else viewModel.updateRightStick(0f, 0f)
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        var newX = thumbOffsetX + dragAmount.x
                        var newY = thumbOffsetY + dragAmount.y
                        val distance = hypot(newX, newY)

                        if (distance > maxRadius) {
                            val angle = atan2(newY, newX)
                            newX = cos(angle) * maxRadius
                            newY = sin(angle) * maxRadius
                        }

                        thumbOffsetX = newX
                        thumbOffsetY = newY

                        // Normalize to -1.0 to 1.0 for the PC server
                        val normalizedX = newX / maxRadius
                        val normalizedY = -newY / maxRadius // Invert Y so up is positive

                        if (isLeft) {
                            viewModel.updateLeftStick(normalizedX, normalizedY)
                        } else {
                            viewModel.updateRightStick(normalizedX, normalizedY)
                        }
                    }
                }
        ) {
            // Thumbstick texture rings
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color.Black.copy(alpha = 0.3f),
                    radius = size.minDimension / 2.5f,
                    style = Stroke(width = 6f)
                )
                drawCircle(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.White.copy(alpha = 0.2f), Color.Transparent)
                    ),
                    radius = size.minDimension / 2.1f,
                    style = Stroke(width = 2f)
                )
            }
        }
    }
}
