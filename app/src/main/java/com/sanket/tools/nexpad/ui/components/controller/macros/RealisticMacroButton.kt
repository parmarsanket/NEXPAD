package com.sanket.tools.nexpad.ui.components.controller.macros

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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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

    val rgbBloomAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 0.40f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "macro_btn_rgb_bloom"
    )

    val auraColor = remember(key) {
        if (key.contains("3") || key.contains("4")) Color(0xFF00E5FF) else Color(0xFFFFD600)
    }

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
            .drawBehind {
                if (isRgbEnabled) {
                    val padX = 12.dp.toPx()
                    val padY = 8.dp.toPx()

                    // 1. Stadium Macro Toggle Switch Aura
                    drawRoundRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                auraColor.copy(alpha = rgbBloomAlpha * (if (isPressed) 0.55f else 0.32f)),
                                auraColor.copy(alpha = rgbBloomAlpha * 0.12f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = size.width * 0.55f
                        ),
                        topLeft = Offset(-padX, -padY),
                        size = Size(size.width + padX * 2f, size.height + padY * 2f),
                        cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx())
                    )

                    // 2. 4 Corner Tactical Bracket Pips
                    val bracketAlpha = rgbBloomAlpha * (if (isPressed) 0.90f else 0.45f)
                    val bracketLen = (if (isPressed) 8.dp else 5.dp).toPx()
                    val insetX = 4.dp.toPx()
                    val insetY = 3.dp.toPx()

                    // Top-Left corner bracket
                    drawLine(auraColor.copy(alpha = bracketAlpha), Offset(-insetX, -insetY), Offset(-insetX + bracketLen, -insetY), 2f)
                    drawLine(auraColor.copy(alpha = bracketAlpha), Offset(-insetX, -insetY), Offset(-insetX, -insetY + bracketLen), 2f)

                    // Top-Right corner bracket
                    drawLine(auraColor.copy(alpha = bracketAlpha), Offset(size.width + insetX, -insetY), Offset(size.width + insetX - bracketLen, -insetY), 2f)
                    drawLine(auraColor.copy(alpha = bracketAlpha), Offset(size.width + insetX, -insetY), Offset(size.width + insetX, -insetY + bracketLen), 2f)

                    // Bottom-Left corner bracket
                    drawLine(auraColor.copy(alpha = bracketAlpha), Offset(-insetX, size.height + insetY), Offset(-insetX + bracketLen, size.height + insetY), 2f)
                    drawLine(auraColor.copy(alpha = bracketAlpha), Offset(-insetX, size.height + insetY), Offset(-insetX, size.height + insetY - bracketLen), 2f)

                    // Bottom-Right corner bracket
                    drawLine(auraColor.copy(alpha = bracketAlpha), Offset(size.width + insetX, size.height + insetY), Offset(size.width + insetX - bracketLen, size.height + insetY), 2f)
                    drawLine(auraColor.copy(alpha = bracketAlpha), Offset(size.width + insetX, size.height + insetY), Offset(size.width + insetX, size.height + insetY - bracketLen), 2f)

                    // 3. Central Electric Pulse Slit on press
                    if (isPressed) {
                        val slitW = size.width * 0.75f
                        val slitLeft = center.x - slitW / 2f
                        drawLine(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    auraColor.copy(alpha = rgbBloomAlpha * 0.85f),
                                    Color.White.copy(alpha = rgbBloomAlpha * 0.95f),
                                    auraColor.copy(alpha = rgbBloomAlpha * 0.85f),
                                    Color.Transparent
                                ),
                                startX = slitLeft,
                                endX = slitLeft + slitW
                            ),
                            start = Offset(slitLeft, center.y),
                            end = Offset(slitLeft + slitW, center.y),
                            strokeWidth = 3.dp.toPx()
                        )
                    }
                }
            }
            .shadow(
                elevation = if (isPressed) 1.dp else 4.dp,
                shape = shape,
                ambientColor = if (isRgbEnabled) auraColor else Color.Black,
                spotColor = if (isRgbEnabled) auraColor else Color.Black
            )
            .clip(shape)
            .background(baseGradient)
            .border(
                width = 1.2.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        (if (isRgbEnabled) auraColor else Color(0xFF718096)).copy(alpha = if (isPressed) 0.40f else 0.70f),
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
            color = if (isPressed) Color.White else if (isRgbEnabled) auraColor else Color.White.copy(alpha = 0.85f),
            fontWeight = FontWeight.Black,
            fontSize = 13.sp,
            letterSpacing = 0.5.sp,
            style = androidx.compose.ui.text.TextStyle(
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = if (isRgbEnabled) auraColor.copy(alpha = rgbBloomAlpha * 0.85f) else Color.Black.copy(alpha = 0.8f),
                    offset = Offset(0f, 1.5f),
                    blurRadius = 4f
                )
            )
        )
    }
}
