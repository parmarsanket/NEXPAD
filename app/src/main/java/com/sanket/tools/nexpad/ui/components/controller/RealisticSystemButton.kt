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
import androidx.compose.foundation.shape.CircleShape
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
import com.sanket.tools.nexpad.category.ControlKey
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel
import kotlin.math.roundToInt

/**
 * Tactile system button (Guide/Home, Start/Menu, Back/View, Share/Capture)
 * featuring console-grade spring compression, metallic chamfer rim, and glowing glyphs.
 */
@Composable
fun RealisticSystemButton(
    key: String,
    isConnected: Boolean,
    onVibrate: () -> Unit,
    viewModel: GamepadViewModel,
    isRgbEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    val scaleAnim by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 750f),
        label = "sys_btn_scale"
    )
    val pressOffsetYAnim by animateFloatAsState(
        targetValue = if (isPressed) 2.5f else 0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 750f),
        label = "sys_btn_offset"
    )

    val currentOnVibrate by rememberUpdatedState(onVibrate)
    val currentViewModel by rememberUpdatedState(viewModel)

    val ctrl = remember(key) { ControlKey.fromIdentifier(key) }
    val isGuide = ctrl == ControlKey.GUIDE

    val rgbBloomAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 0.40f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "sys_btn_rgb_bloom"
    )

    val auraColor = remember(isGuide) {
        if (isGuide) Color(0xFF00E5FF) else Color(0xFF94A3B8)
    }

    val (symbol, symbolColor) = remember(ctrl, key) {
        when (ctrl) {
            ControlKey.GUIDE -> Pair("⨂", Color(0xFF00E5FF))
            ControlKey.START -> Pair("☰", Color.White)
            ControlKey.BACK  -> Pair("⧉", Color.White)
            ControlKey.SHARE -> Pair("⇪", Color.White)
            else -> Pair(key.take(2), Color.White)
        }
    }

    val buttonSize = 60.dp
    val fontSize = 20.sp

    val surfaceGradient = remember(isPressed, isGuide) {
        Brush.radialGradient(
            colors = if (isPressed) {
                listOf(Color(0xFF1E2633), Color(0xFF0F141C))
            } else if (isGuide) {
                listOf(Color(0xFF334155), Color(0xFF1E293B), Color(0xFF0F172A))
            } else {
                listOf(Color(0xFF323D4D), Color(0xFF1F2631), Color(0xFF12161E))
            },
            center = Offset(0.35f, 0.35f),
            radius = 120f
        )
    }

    Box(
        modifier = modifier
            .size(buttonSize)
            .graphicsLayer {
                scaleX = scaleAnim
                scaleY = scaleAnim
            }
            .offset { IntOffset(0, pressOffsetYAnim.dp.roundToPx()) }
            .drawBehind {
                if (isRgbEnabled) {
                    val radius = size.minDimension * 0.95f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                auraColor.copy(alpha = rgbBloomAlpha * 0.55f),
                                auraColor.copy(alpha = rgbBloomAlpha * 0.22f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = radius
                        ),
                        radius = radius
                    )
                }
            }
            .shadow(
                elevation = if (isPressed) 1.dp else if (isGuide) 6.dp else 4.dp,
                shape = CircleShape,
                ambientColor = if (isRgbEnabled) auraColor else Color.Black,
                spotColor = if (isRgbEnabled) auraColor else Color.Black
            )
            .clip(CircleShape)
            .background(surfaceGradient)
            .border(
                width = if (isGuide) 1.5.dp else 1.2.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        (if (isRgbEnabled) auraColor else Color(0xFF718096)).copy(alpha = if (isPressed) 0.35f else 0.70f),
                        Color(0xFF1A202C)
                    )
                ),
                shape = CircleShape
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
        // Specular top highlight ring
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isPressed) 0.10f else 0.30f),
                        Color.Transparent
                    )
                ),
                radius = size.minDimension / 2f - 2f,
                style = Stroke(width = 1.5f)
            )
        }

        Text(
            text = symbol,
            color = if (isPressed) Color.White else if (isRgbEnabled && isGuide) auraColor else symbolColor,
            fontWeight = FontWeight.Black,
            fontSize = fontSize,
            style = androidx.compose.ui.text.TextStyle(
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = if (isRgbEnabled) auraColor.copy(alpha = rgbBloomAlpha * 0.85f) else Color.Black.copy(alpha = 0.8f),
                    offset = Offset(0f, 1.5f),
                    blurRadius = if (isGuide) 8f else 3f
                )
            )
        )
    }
}
