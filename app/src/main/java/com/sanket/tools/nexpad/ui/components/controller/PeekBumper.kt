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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel
import kotlin.math.roundToInt

/**
 * NEXPAD Bumper C — Peek (LB / RB)
 *
 * Implements the Bumper C "Peek" design:
 * - Full symmetrical pill capsule contour (154dp × 54dp, corner radius 27dp)
 * - Deep convex acrylic dome body (#232527 -> #0c0d0e -> #000)
 * - Inset glowing neon border ring with emissive halo bloom
 * - Oversized recessed optical magnifier window (124dp × 36dp) with deep radial vignette
 * - Oversized bold peek glyph (42sp, 6sp letter-spacing) that "peeks" through the aperture
 * - Dynamic glyph zoom animation (1.18x expansion on press)
 * - Damped harmonic spring kinematics (translateY 2dp, scale 0.95)
 * - Top specular crescent lens sheen and rim highlight
 */
@Composable
fun PeekBumper(
    key: String,
    isConnected: Boolean,
    onVibrate: () -> Unit,
    viewModel: GamepadViewModel,
    isRgbEnabled: Boolean,
    modifier: Modifier = Modifier,
    displayLabel: String? = null
) {
    var isPressed by remember { mutableStateOf(false) }

    // Full pill shape (27dp radius for 54dp height)
    val bumperShape = remember { RoundedCornerShape(27.dp) }

    // Kinematic Physics Engine — Damped Harmonic Spring
    val scaleAnim by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "peek_bumper_scale"
    )
    val pressOffsetYAnim by animateFloatAsState(
        targetValue = if (isPressed) 2.0f else 0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "peek_bumper_offset"
    )
    val ringBloomAlpha by animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0.70f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 800f),
        label = "peek_ring_bloom"
    )

    // Iconic Peek feature: glyph is 2x smaller in idle (22sp) and zooms 2.0x on press (to 44sp) inside the magnifier aperture
    val glyphScaleAnim by animateFloatAsState(
        targetValue = if (isPressed) 1.5f else 1.0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "peek_glyph_scale"
    )

    // CSS: --glow: #a97cf0
    val neonColor = remember(isRgbEnabled) {
        if (isRgbEnabled) Color(0xFFA97CF0) else Color(0xFFD8DEE9)
    }

    val currentOnVibrate by rememberUpdatedState(onVibrate)
    val currentViewModel by rememberUpdatedState(viewModel)

    // Convex dark dome background gradient
    val baseDomeGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF232527),
                Color(0xFF0C0D0E),
                Color(0xFF000000)
            ),
            center = Offset(0.50f, 0.55f),
            radius = 260f
        )
    }

    val pressedDomeGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF1B1D1E),
                Color(0xFF070808),
                Color(0xFF000000)
            ),
            center = Offset(0.50f, 0.58f),
            radius = 260f
        )
    }

    // Recessed magnifier window gradient
    val windowGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF050506),
                Color(0xFF121314)
            ),
            center = Offset(0.50f, 0.60f),
            radius = 90f
        )
    }

    Box(
        modifier = modifier
            .size(154.dp, 54.dp)
            // Emissive outer halo
            .drawBehind {
                if (isRgbEnabled) {
                    val pad = 6.dp.toPx()
                    drawRoundRect(
                        color = neonColor.copy(alpha = if (isPressed) 0.40f else 0.18f),
                        topLeft = Offset(-pad, -pad),
                        size = Size(size.width + pad * 2, size.height + pad * 2),
                        cornerRadius = CornerRadius(27.dp.toPx() + pad, 27.dp.toPx() + pad)
                    )
                }
            }
            .graphicsLayer {
                scaleX = scaleAnim
                scaleY = scaleAnim
            }
            .offset { IntOffset(0, pressOffsetYAnim.dp.roundToPx()) }
            // Outer drop shadow
            .shadow(
                elevation = if (isPressed) 2.dp else 7.dp,
                shape = bumperShape,
                spotColor = if (isRgbEnabled) neonColor else Color.Black,
                ambientColor = if (isRgbEnabled) neonColor.copy(alpha = 0.5f) else Color.Black
            )
            .clip(bumperShape)
            // Convex dark body
            .background(if (isPressed) pressedDomeGradient else baseDomeGradient)
            // 1px metallic rim outline
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.50f),
                        Color.Black.copy(alpha = 0.80f)
                    )
                ),
                shape = bumperShape
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
        // Multi-Layer Canvas: Undercut shadow, Neon Ring, Glass Specular Sheen
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Inset bottom shadow
            val insetH = h * 0.40f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = if (isPressed) 0.85f else 0.70f)),
                    startY = h - insetH,
                    endY = h
                ),
                topLeft = Offset(0f, h - insetH),
                size = Size(w, insetH)
            )

            // Neon color ring — inset 2.5dp with pill corner radius
            val ringInset = 2.5.dp.toPx()
            val ringRadius = 27.dp.toPx() - ringInset

            // Outer bloom pass
            drawRoundRect(
                color = neonColor.copy(alpha = (if (isPressed) 0.60f else 0.28f) * ringBloomAlpha),
                topLeft = Offset(ringInset, ringInset),
                size = Size(w - ringInset * 2, h - ringInset * 2),
                cornerRadius = CornerRadius(ringRadius, ringRadius),
                style = Stroke(width = 3.5.dp.toPx())
            )

            // Core crisp stroke
            drawRoundRect(
                color = neonColor.copy(alpha = ringBloomAlpha),
                topLeft = Offset(ringInset, ringInset),
                size = Size(w - ringInset * 2, h - ringInset * 2),
                cornerRadius = CornerRadius(ringRadius, ringRadius),
                style = Stroke(width = 1.8.dp.toPx())
            )

            // Glass lens top specular crescent highlight
            val sheenH = h * 0.42f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isPressed) 0.05f else 0.14f),
                        Color.White.copy(alpha = if (isPressed) 0.01f else 0.04f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = sheenH
                ),
                topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                size = Size(w - 2.dp.toPx(), sheenH)
            )

            // Top edge specular line
            val specStartX = w * 0.15f
            val specEndX = w * 0.85f
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isPressed) 0.12f else 0.45f),
                        Color.White.copy(alpha = if (isPressed) 0.04f else 0.16f)
                    ),
                    startX = specStartX,
                    endX = specEndX
                ),
                start = Offset(specStartX, 4f),
                end = Offset(specEndX, 4f),
                strokeWidth = 1.5f
            )
        }

        // Oversized Recessed Magnifier Window (.lx-window)
        val windowShape = RoundedCornerShape(18.dp)
        Box(
            modifier = Modifier
                .size(124.dp, 36.dp)
                .shadow(
                    elevation = 2.dp,
                    shape = windowShape,
                    ambientColor = Color.Black,
                    spotColor = Color.Black
                )
                .clip(windowShape)
                .background(windowGradient)
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.06f),
                    shape = windowShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Recessed inner vignette overlay and 3D bevel
            Canvas(modifier = Modifier.fillMaxSize()) {
                val winW = size.width
                val winH = size.height

                // Inset top shadow
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.92f), Color.Transparent),
                        startY = 0f,
                        endY = 8.dp.toPx()
                    ),
                    size = Size(winW, 8.dp.toPx())
                )

                // Deep vignette
                drawRect(
                    brush = Brush.radialGradient(
                        0.0f to Color.Transparent,
                        0.28f to Color.Transparent,
                        1.0f to Color.Black.copy(alpha = 0.88f),
                        center = Offset(winW / 2f, winH / 2f),
                        radius = winW * 0.55f
                    )
                )

                // Bottom specular edge line
                drawLine(
                    color = Color.White.copy(alpha = 0.06f),
                    start = Offset(6.dp.toPx(), winH - 0.5f),
                    end = Offset(winW - 6.dp.toPx(), winH - 0.5f),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Framed Peek Glyph (.lx-g) with dynamic 2.0x zoom expansion on press
            val labelText = displayLabel ?: key
            Text(
                text = labelText,
                color = neonColor,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                letterSpacing = 5.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer {
                    scaleX = glyphScaleAnim
                    scaleY = glyphScaleAnim
                }
            )
        }
    }
}
