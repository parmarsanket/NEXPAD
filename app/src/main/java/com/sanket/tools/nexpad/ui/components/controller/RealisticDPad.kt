package com.sanket.tools.nexpad.ui.components.controller

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel
import kotlin.math.atan2

val crossShape = GenericShape { size, _ ->
    val thirdW = size.width / 3f
    val thirdH = size.height / 3f
    val twoThirdW = thirdW * 2f
    val twoThirdH = thirdH * 2f

    moveTo(thirdW, 0f)
    lineTo(twoThirdW, 0f)
    lineTo(twoThirdW, thirdH)
    lineTo(size.width, thirdH)
    lineTo(size.width, twoThirdH)
    lineTo(twoThirdW, twoThirdH)
    lineTo(twoThirdW, size.height)
    lineTo(thirdW, size.height)
    lineTo(thirdW, twoThirdH)
    lineTo(0f, twoThirdH)
    lineTo(0f, thirdH)
    lineTo(thirdW, thirdH)
    close()
}

@Composable
fun RealisticDPad(
    isConnected: Boolean,
    viewModel: GamepadViewModel,
    isRgbEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    var pressedDir by remember { mutableStateOf<String?>(null) }

    val gradient = Brush.linearGradient(
        colors = listOf(Color(0xFF333333), Color(0xFF111111)),
        start = Offset(0f, 0f),
        end = Offset(200f, 200f)
    )

    val shadow = if (isRgbEnabled) {
        Modifier.shadow(12.dp, crossShape, ambientColor = Color.Green, spotColor = Color.Yellow)
    } else {
        Modifier.shadow(8.dp, crossShape, ambientColor = Color.Black, spotColor = Color.Black)
    }

    Box(
        modifier = modifier
            .size(140.dp)
            .then(shadow)
            .clip(crossShape)
            .background(gradient)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val dx = offset.x - center.x
                        val dy = offset.y - center.y
                        val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble()))
                        
                        val dir = when {
                            angle >= -45 && angle < 45 -> "RIGHT"
                            angle >= 45 && angle < 135 -> "DOWN"
                            angle >= 135 || angle < -135 -> "LEFT"
                            angle >= -135 && angle < -45 -> "UP"
                            else -> null
                        }

                        pressedDir = dir
                        if (dir != null) {
                            when (dir) {
                                "UP" -> viewModel.updateButton("UP", true)
                                "DOWN" -> viewModel.updateButton("DOWN", true)
                                "LEFT" -> viewModel.updateButton("LEFT", true)
                                "RIGHT" -> viewModel.updateButton("RIGHT", true)
                            }
                        }
                        
                        tryAwaitRelease()
                        
                        if (dir != null) {
                            when (dir) {
                                "UP" -> viewModel.updateButton("UP", false)
                                "DOWN" -> viewModel.updateButton("DOWN", false)
                                "LEFT" -> viewModel.updateButton("LEFT", false)
                                "RIGHT" -> viewModel.updateButton("RIGHT", false)
                            }
                        }
                        pressedDir = null
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw cross indentations
            drawLine(
                color = Color.Black.copy(alpha = 0.5f),
                start = Offset(size.width / 2f, 0f),
                end = Offset(size.width / 2f, size.height),
                strokeWidth = 4f
            )
            drawLine(
                color = Color.Black.copy(alpha = 0.5f),
                start = Offset(0f, size.height / 2f),
                end = Offset(size.width, size.height / 2f),
                strokeWidth = 4f
            )
        }
    }
}
