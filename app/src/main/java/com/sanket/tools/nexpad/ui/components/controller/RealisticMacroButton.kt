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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel
import kotlin.math.roundToInt

/**
 * Tactile Elite rear macro paddle / programmable shortcut button (M1 - M4).
 * Features spring depression kinematics, metallic chamfer rim, specular sheen, and RGB accent underglow.
 */
@Composable
fun RealisticMacroButton(
    key: String,
    isConnected: Boolean,
    onVibrate: () -> Unit,
    viewModel: GamepadViewModel,
    isRgbEnabled: Boolean,
    modifier: Modifier = Modifier,
    displayLabel: String? = null
) {
    var isPressed by remember { mutableStateOf(false) }

    val scaleAnim by animateFloatAsState(
        targetValue = if (isPressed) 0.91f else 1.0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 750f),
        label = "macro_btn_scale"
    )
    val pressOffsetYAnim by animateFloatAsState(
        targetValue = if (isPressed) 2.5f else 0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 750f),
        label = "macro_btn_offset"
    )

    val currentOnVibrate by rememberUpdatedState(onVibrate)
    val currentViewModel by rememberUpdatedState(viewModel)

    val shape = RoundedCornerShape(16.dp)
    val accentColor = Color(0xFFF59E0B) // Amber gold for Elite rear paddles

    val baseGradient = remember(isPressed) {
        Brush.verticalGradient(
            colors = if (isPressed) {
                listOf(Color(0xFF1E2633), Color(0xFF0F141C))
            } else {
                listOf(Color(0xFF333E4D), Color(0xFF1F2631), Color(0xFF12161E))
            }
        )
    }

    Box(
        modifier = modifier
            .size(80.dp, 40.dp)
            .graphicsLayer {
                scaleX = scaleAnim
                scaleY = scaleAnim
            }
            .offset { IntOffset(0, pressOffsetYAnim.dp.roundToPx()) }
            .shadow(
                elevation = if (isPressed) 2.dp else 4.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.40f),
                spotColor = Color.Black.copy(alpha = 0.55f)
            )
            .clip(shape)
            .background(baseGradient)
            .border(
                width = 1.2.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF718096).copy(alpha = if (isPressed) 0.35f else 0.70f),
                        Color(0xFF1A202C)
                    )
                ),
                shape = shape
            )
            .pointerInput(key) {
                detectTapGestures(
                    onPress = {
                        currentOnVibrate()
                        isPressed = true
                        currentViewModel.updateButton(key, true)
                        tryAwaitRelease()
                        isPressed = false
                        currentViewModel.updateButton(key, false)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Specular top highlight bevel
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isPressed) 0.10f else 0.35f),
                        Color.Transparent
                    )
                ),
                start = Offset(12f, 4f),
                end = Offset(size.width - 12f, 4f),
                strokeWidth = 1.5f
            )
        }

        Text(
            text = displayLabel ?: key,
            color = if (isPressed) Color.White else if (isRgbEnabled) accentColor else Color.White.copy(alpha = 0.85f),
            fontWeight = FontWeight.Black,
            fontSize = 13.sp,
            letterSpacing = 0.5.sp,
            style = androidx.compose.ui.text.TextStyle(
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = if (isRgbEnabled) accentColor.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.8f),
                    offset = Offset(0f, 1.5f),
                    blurRadius = 3f
                )
            )
        )
    }
}
