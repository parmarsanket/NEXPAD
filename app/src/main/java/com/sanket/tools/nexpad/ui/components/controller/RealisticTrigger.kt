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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel
import kotlin.math.roundToInt

/**
 * Console-grade analog trigger (LT / RT / L2 / R2) featuring progressive spring depression,
 * ergonomic pedal curvature, metallic chamfer rim, laser-etched knurled grip ribs, and dynamic RGB bloom.
 */
@Composable
fun RealisticTrigger(
    key: String,
    isConnected: Boolean,
    onVibrate: () -> Unit,
    viewModel: GamepadViewModel,
    isRgbEnabled: Boolean,
    modifier: Modifier = Modifier,
    displayLabel: String? = null
) {
    var isPressed by remember { mutableStateOf(false) }
    val isLeft = key.uppercase() == com.sanket.tools.nexpad.model.NexpadKeys.LT

    val triggerShape = remember(isLeft) {
        if (isLeft) {
            RoundedCornerShape(topStart = 22.dp, topEnd = 12.dp, bottomStart = 46.dp, bottomEnd = 20.dp)
        } else {
            RoundedCornerShape(topStart = 12.dp, topEnd = 22.dp, bottomStart = 20.dp, bottomEnd = 46.dp)
        }
    }

    // Kinematics: snappy spring return with damping
    val scaleAnim by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 650f),
        label = "trigger_scale"
    )
    val pressOffsetYAnim by animateFloatAsState(
        targetValue = if (isPressed) 4.5f else 0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 650f),
        label = "trigger_offset"
    )
    val rgbBloomAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 0.40f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 800f),
        label = "trigger_rgb_bloom"
    )

    val neonColor = if (isLeft) Color(0xFF00E5FF) else Color(0xFFFF0055)
    val ambientGlow = if (isLeft) Color(0xFF0052CC) else Color(0xFFCC0044)

    val baseGradient = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF384352),
                Color(0xFF222934),
                Color(0xFF13171F),
                Color(0xFF090B0F)
            )
        )
    }

    val pressedGradient = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1C222B),
                Color(0xFF11141A),
                Color(0xFF090B0E),
                Color(0xFF040507)
            )
        )
    }

    val currentOnVibrate by rememberUpdatedState(onVibrate)
    val currentViewModel by rememberUpdatedState(viewModel)

    Box(
        modifier = modifier
            .size(100.dp, 160.dp)
            .drawBehind {
                if (isRgbEnabled) {
                    drawRoundRect(
                        color = neonColor.copy(alpha = rgbBloomAlpha * 0.42f),
                        size = size.copy(width = size.width + 16.dp.toPx(), height = size.height + 16.dp.toPx()),
                        topLeft = Offset(-8.dp.toPx(), -8.dp.toPx()),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(26.dp.toPx(), 26.dp.toPx())
                    )
                }
            }
            .graphicsLayer {
                scaleX = scaleAnim
                scaleY = scaleAnim
            }
            .offset { IntOffset(0, pressOffsetYAnim.dp.roundToPx()) }
            .shadow(
                elevation = if (isPressed) 4.dp else 12.dp,
                shape = triggerShape,
                spotColor = if (isRgbEnabled) neonColor else Color.Black,
                ambientColor = if (isRgbEnabled) ambientGlow else Color.Black
            )
            .clip(triggerShape)
            .background(if (isPressed) pressedGradient else baseGradient)
            .border(
                width = 1.5.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF718096).copy(alpha = if (isPressed) 0.35f else 0.75f),
                        Color(0xFF2D3748).copy(alpha = 0.50f),
                        Color(0xFF1A202C)
                    )
                ),
                shape = triggerShape
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
        contentAlignment = Alignment.BottomCenter
    ) {
        // Specular Top Crown Bevel & Laser-Etched Knurled Pedal Ribs
        Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
            val w = size.width
            val h = size.height

            // 1. Top specular highlight edge (overhead curved sheen)
            val topSheenStartX = if (isLeft) w * 0.15f else w * 0.25f
            val topSheenEndX = if (isLeft) w * 0.75f else w * 0.85f
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isPressed) 0.15f else 0.55f),
                        Color.White.copy(alpha = if (isPressed) 0.05f else 0.20f)
                    ),
                    startX = topSheenStartX,
                    endX = topSheenEndX
                ),
                start = Offset(topSheenStartX, 8f),
                end = Offset(topSheenEndX, 8f),
                strokeWidth = 2.5f
            )

            // 2. Center recessed scoop shading
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = if (isPressed) 0.55f else 0.35f)
                    )
                ),
                topLeft = Offset(0f, h * 0.40f),
                size = androidx.compose.ui.geometry.Size(w, h * 0.60f)
            )

            // 3. Tactile knurled grip ribs (4 progressive horizontal ridges on trigger pedal)
            val ribStartX = if (isLeft) w * 0.20f else w * 0.28f
            val ribEndX = if (isLeft) w * 0.72f else w * 0.80f
            for (i in 0..3) {
                val y = h * 0.50f + (i * 10.dp.toPx())
                // Deep laser groove
                drawLine(
                    color = Color.Black.copy(alpha = 0.60f),
                    start = Offset(ribStartX, y),
                    end = Offset(ribEndX, y),
                    strokeWidth = 2.5f
                )
                // Highlight ridge edge
                drawLine(
                    color = Color.White.copy(alpha = if (isPressed) 0.10f else 0.25f),
                    start = Offset(ribStartX, y + 1.5f),
                    end = Offset(ribEndX, y + 1.5f),
                    strokeWidth = 1.2f
                )
            }
        }

        val labelColor = if (isRgbEnabled) {
            if (isLeft) Color(0xFF67E8F9) else Color(0xFFFDA4AF)
        } else {
            Color.White.copy(alpha = if (isPressed) 0.75f else 0.95f)
        }

        Text(
            text = displayLabel ?: key,
            color = labelColor,
            fontWeight = FontWeight.Black,
            fontSize = 22.sp,
            style = androidx.compose.ui.text.TextStyle(
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = if (isRgbEnabled) neonColor.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.85f),
                    offset = Offset(0f, 2f),
                    blurRadius = if (isRgbEnabled) 10f else 4f
                )
            ),
            modifier = Modifier.padding(bottom = 18.dp)
        )
    }
}
