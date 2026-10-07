package com.sanket.tools.nexpad.ui.components.controller.abxy

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel

@Composable
fun GamepadButton(
    text: String,
    isConnected: Boolean,
    onVibrate: () -> Unit,
    viewModel: GamepadViewModel,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    val scaleAnim by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "gp_btn_scale"
    )
    val bloomAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 0.40f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "gp_btn_bloom"
    )

    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .padding(4.dp)
            .size(64.dp)
            .graphicsLayer {
                scaleX = scaleAnim
                scaleY = scaleAnim
            }
            .drawBehind {
                val auraRadius = size.minDimension * (if (isPressed) 1.05f else 0.90f)
                // Dual-layer primary neon pulse in drawBehind
                drawCircle(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.00f to (if (isPressed) Color.White else primaryColor).copy(alpha = bloomAlpha * 0.65f),
                            0.35f to primaryColor.copy(alpha = bloomAlpha * 0.35f),
                            0.75f to primaryColor.copy(alpha = bloomAlpha * 0.12f),
                            1.00f to Color.Transparent
                        ),
                        center = center,
                        radius = auraRadius
                    ),
                    radius = auraRadius,
                    center = center
                )
            }
            .shadow(
                elevation = if (isPressed) 2.dp else 6.dp,
                shape = CircleShape,
                spotColor = primaryColor,
                ambientColor = Color.Black
            )
            .clip(CircleShape)
            .background(
                color = if (isPressed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                shape = CircleShape
            )
            .pointerInput(isConnected) {
                detectTapGestures(
                    onPress = {
                        onVibrate()
                        isPressed = true
                        viewModel.updateButton(text, true)
                        tryAwaitRelease()
                        isPressed = false
                        viewModel.updateButton(text, false)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text, 
            color = if (isPressed) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
    }
}

