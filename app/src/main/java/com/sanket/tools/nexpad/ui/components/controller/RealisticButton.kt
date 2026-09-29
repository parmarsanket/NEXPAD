package com.sanket.tools.nexpad.ui.components.controller

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
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
 * 4-Way Directional 3D Perspective Orientation for Magnified Lens Keycaps.
 * Simulates real spherical keycap convex curvature where glyphs bend away at the cluster's outer horizon:
 * - TOP (Y / Triangle): Shifted upward, upper optical recess, clipped top curvature.
 * - BOTTOM (A / Cross): Shifted downward, lower optical recess, clipped bottom curvature.
 * - LEFT (X / Square): Shifted leftward, left optical recess, clipped left curvature.
 * - RIGHT (B / Circle): Shifted rightward, right optical recess, clipped right curvature.
 * - NONE (System/Macro): Centered convex dome.
 */
enum class ButtonPerspectiveDirection {
    TOP, BOTTOM, LEFT, RIGHT, NONE
}

// 4-Way Horizon Clipping Polynomial Curves (Clips glyph feet/edges along dome curvature)
val BottomHorizonClipShape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    moveTo(0f, 0f)
    lineTo(w, 0f)
    lineTo(w, h * 0.82f)
    lineTo(w * 0.94f, h * 0.86f)
    lineTo(w * 0.88f, h * 0.89f)
    lineTo(w * 0.82f, h * 0.91f)
    lineTo(w * 0.74f, h * 0.93f)
    lineTo(w * 0.64f, h * 0.94f)
    lineTo(w * 0.50f, h * 0.94f)
    lineTo(w * 0.36f, h * 0.94f)
    lineTo(w * 0.26f, h * 0.93f)
    lineTo(w * 0.18f, h * 0.91f)
    lineTo(w * 0.10f, h * 0.88f)
    lineTo(w * 0.04f, h * 0.84f)
    lineTo(0f, h * 0.80f)
    close()
}

val TopHorizonClipShape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    moveTo(0f, h * 0.18f)
    lineTo(w * 0.04f, h * 0.14f)
    lineTo(w * 0.10f, h * 0.11f)
    lineTo(w * 0.18f, h * 0.09f)
    lineTo(w * 0.26f, h * 0.07f)
    lineTo(w * 0.36f, h * 0.06f)
    lineTo(w * 0.50f, h * 0.06f)
    lineTo(w * 0.64f, h * 0.06f)
    lineTo(w * 0.74f, h * 0.07f)
    lineTo(w * 0.82f, h * 0.09f)
    lineTo(w * 0.90f, h * 0.12f)
    lineTo(w * 0.96f, h * 0.16f)
    lineTo(w, h * 0.20f)
    lineTo(w, h)
    lineTo(0f, h)
    close()
}

val LeftHorizonClipShape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    moveTo(w * 0.18f, 0f)
    lineTo(w * 0.14f, h * 0.04f)
    lineTo(w * 0.11f, h * 0.10f)
    lineTo(w * 0.09f, h * 0.18f)
    lineTo(w * 0.07f, h * 0.28f)
    lineTo(w * 0.06f, h * 0.40f)
    lineTo(w * 0.06f, h * 0.50f)
    lineTo(w * 0.06f, h * 0.60f)
    lineTo(w * 0.07f, h * 0.72f)
    lineTo(w * 0.09f, h * 0.82f)
    lineTo(w * 0.12f, h * 0.90f)
    lineTo(w * 0.17f, h * 0.96f)
    lineTo(w * 0.22f, h)
    lineTo(w, h)
    lineTo(w, 0f)
    close()
}

val RightHorizonClipShape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    moveTo(0f, 0f)
    lineTo(w * 0.82f, 0f)
    lineTo(w * 0.88f, h * 0.04f)
    lineTo(w * 0.92f, h * 0.10f)
    lineTo(w * 0.94f, h * 0.18f)
    lineTo(w * 0.95f, h * 0.28f)
    lineTo(w * 0.96f, h * 0.40f)
    lineTo(w * 0.96f, h * 0.50f)
    lineTo(w * 0.96f, h * 0.60f)
    lineTo(w * 0.95f, h * 0.72f)
    lineTo(w * 0.93f, h * 0.82f)
    lineTo(w * 0.90f, h * 0.90f)
    lineTo(w * 0.85f, h * 0.96f)
    lineTo(w * 0.80f, h)
    lineTo(0f, h)
    close()
}

/**
 * Console-grade 3D mechanical controller button matching authentic NXPRC architecture:
 * - Magnified Lens Acrylic Dome (huge 46sp glyph ~80% of button diameter)
 * - 4-Way Directional 3D Perspective Depth (Y up, A down, X left, B right)
 * - Directional Optical Recess Ellipse simulating spherical horizon drop
 * - Luminous Neon Color Ring with inner/outer bloom
 * - Acrylic Glass Lens with top crescent highlight & specular sheen
 * - Damped Harmonic Oscillator Spring Kinematics (stiffness 440, damping 0.68)
 */
@Composable
fun RealisticButton(
    key: String, 
    buttonColor: Color,
    isConnected: Boolean, 
    onVibrate: () -> Unit, 
    viewModel: GamepadViewModel, 
    isRgbEnabled: Boolean,
    modifier: Modifier = Modifier,
    displayLabel: String? = null
) {
    var isPressed by remember { mutableStateOf(false) }

    // Physical Spring Kinematics (stiffness: 440f, damping: 0.68f, press-scale: 0.95f)
    val scaleAnim by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "btn_scale"
    )
    val pressOffsetYAnim by animateFloatAsState(
        targetValue = if (isPressed) 2f else 0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "btn_depth"
    )
    val rgbBloomAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 0.45f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "btn_rgb_bloom"
    )

    val activeLabel = displayLabel ?: key

    // Determine 4-way directional perspective based on face button role
    val perspectiveDirection = remember(key) {
        when {
            key.equals("A", ignoreCase = true) || key.equals("CROSS", ignoreCase = true) || key.equals("DOWN", ignoreCase = true) -> ButtonPerspectiveDirection.BOTTOM
            key.equals("Y", ignoreCase = true) || key.equals("TRIANGLE", ignoreCase = true) || key.equals("UP", ignoreCase = true) -> ButtonPerspectiveDirection.TOP
            key.equals("X", ignoreCase = true) || key.equals("SQUARE", ignoreCase = true) || key.equals("LEFT", ignoreCase = true) -> ButtonPerspectiveDirection.LEFT
            key.equals("B", ignoreCase = true) || key.equals("CIRCLE", ignoreCase = true) || key.equals("RIGHT", ignoreCase = true) -> ButtonPerspectiveDirection.RIGHT
            else -> ButtonPerspectiveDirection.NONE
        }
    }

    val horizonClipShape: Shape? = remember(perspectiveDirection) {
        when (perspectiveDirection) {
            ButtonPerspectiveDirection.BOTTOM -> BottomHorizonClipShape
            ButtonPerspectiveDirection.TOP -> TopHorizonClipShape
            ButtonPerspectiveDirection.LEFT -> LeftHorizonClipShape
            ButtonPerspectiveDirection.RIGHT -> RightHorizonClipShape
            ButtonPerspectiveDirection.NONE -> null
        }
    }

    val baseDomeGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF232527),
                Color(0xFF0C0D0E),
                Color(0xFF000000)
            ),
            center = Offset(0.50f, 0.55f),
            radius = 200f
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
            radius = 200f
        )
    }

    Box(
        modifier = modifier
            .size(80.dp)
            // Ambient RGB Bloom behind button socket
            .drawBehind {
                if (isRgbEnabled) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                buttonColor.copy(alpha = rgbBloomAlpha * 0.55f),
                                buttonColor.copy(alpha = rgbBloomAlpha * 0.22f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = size.minDimension * 1f
                        ),
                        radius = size.minDimension * 1f
                    )
                }
            }
            .graphicsLayer {
                scaleX = scaleAnim
                scaleY = scaleAnim
            }
            .offset { IntOffset(0, pressOffsetYAnim.dp.roundToPx()) }
            // Outer drop shadow: 0 4px 7px rgba(0, 0, 0, 0.55)
            .shadow(
                elevation = if (isPressed) 1.dp else 4.dp,
                shape = CircleShape,
                ambientColor = if (isRgbEnabled) buttonColor else Color.Black,
                spotColor = if (isRgbEnabled) buttonColor else Color.Black
            )
            .clip(CircleShape)
            // .cap base dome: radial-gradient(circle at 50% 55%, #232527 0%, #0c0d0e 75%, #000 100%)
            .background(if (isPressed) pressedDomeGradient else baseDomeGradient)
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
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val r = size.minDimension / 2f

            // 0 0 0 1px rgba(0, 0, 0, 0.5) cap outline
            drawCircle(
                color = Color.Black.copy(alpha = 0.50f),
                radius = r - 0.5.dp.toPx(),
                style = Stroke(width = 1.dp.toPx())
            )

            // Inset bottom shadow: inset 0 -6px 9px rgba(0, 0, 0, 0.70)
            val insetShadowH = h * 0.35f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = if (isPressed) 0.85f else 0.70f)),
                    startY = h - insetShadowH,
                    endY = h
                ),
                topLeft = Offset(0f, h - insetShadowH),
                size = Size(w, insetShadowH)
            )

            // .cap::after: Directional Optical Recess Ellipse
            when (perspectiveDirection) {
                ButtonPerspectiveDirection.BOTTOM -> {
                    val recessW = w * 0.80f
                    val recessH = h * 0.26f
                    val recessLeft = (w - recessW) / 2f
                    val recessBottom = h - (h * 0.04f)
                    val recessTop = recessBottom - recessH

                    drawOval(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.07f),
                                Color.White.copy(alpha = 0.018f),
                                Color(0xFF080A0B).copy(alpha = 0.18f),
                                Color.Black.copy(alpha = 0.52f),
                                Color.Black.copy(alpha = 0.86f)
                            ),
                            center = Offset(w * 0.5f, recessTop + recessH * 0.05f),
                            radius = recessW * 0.5f
                        ),
                        topLeft = Offset(recessLeft, recessTop),
                        size = Size(recessW, recessH)
                    )
                }
                ButtonPerspectiveDirection.TOP -> {
                    val recessW = w * 0.80f
                    val recessH = h * 0.26f
                    val recessLeft = (w - recessW) / 2f
                    val recessTop = h * 0.04f

                    drawOval(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.07f),
                                Color.White.copy(alpha = 0.018f),
                                Color(0xFF080A0B).copy(alpha = 0.18f),
                                Color.Black.copy(alpha = 0.52f),
                                Color.Black.copy(alpha = 0.86f)
                            ),
                            center = Offset(w * 0.5f, recessTop + recessH * 0.95f),
                            radius = recessW * 0.5f
                        ),
                        topLeft = Offset(recessLeft, recessTop),
                        size = Size(recessW, recessH)
                    )
                }
                ButtonPerspectiveDirection.LEFT -> {
                    val recessW = w * 0.26f
                    val recessH = h * 0.80f
                    val recessLeft = w * 0.04f
                    val recessTop = (h - recessH) / 2f

                    drawOval(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.07f),
                                Color.White.copy(alpha = 0.018f),
                                Color(0xFF080A0B).copy(alpha = 0.18f),
                                Color.Black.copy(alpha = 0.52f),
                                Color.Black.copy(alpha = 0.86f)
                            ),
                            center = Offset(recessLeft + recessW * 0.95f, h * 0.5f),
                            radius = recessH * 0.5f
                        ),
                        topLeft = Offset(recessLeft, recessTop),
                        size = Size(recessW, recessH)
                    )
                }
                ButtonPerspectiveDirection.RIGHT -> {
                    val recessW = w * 0.26f
                    val recessH = h * 0.80f
                    val recessLeft = w - recessW - (w * 0.04f)
                    val recessTop = (h - recessH) / 2f

                    drawOval(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.07f),
                                Color.White.copy(alpha = 0.018f),
                                Color(0xFF080A0B).copy(alpha = 0.18f),
                                Color.Black.copy(alpha = 0.52f),
                                Color.Black.copy(alpha = 0.86f)
                            ),
                            center = Offset(recessLeft + recessW * 0.05f, h * 0.5f),
                            radius = recessH * 0.5f
                        ),
                        topLeft = Offset(recessLeft, recessTop),
                        size = Size(recessW, recessH)
                    )
                }
                ButtonPerspectiveDirection.NONE -> {
                    drawCircle(
                        color = Color.Black.copy(alpha = if (isPressed) 0.60f else 0.35f),
                        radius = r - 2.dp.toPx(),
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
            }

            // .ring: Inner Glowing Neon Color Ring
            val ringRadius = r - 3.dp.toPx()
            // Outer bloom
            drawCircle(
                color = buttonColor.copy(alpha = if (isPressed) 0.55f else 0.25f),
                radius = ringRadius,
                style = Stroke(width = 4.dp.toPx())
            )
            // Core crisp ring
            drawCircle(
                color = buttonColor.copy(alpha = if (isPressed) 1.0f else 0.70f),
                radius = ringRadius,
                style = Stroke(width = 2.dp.toPx())
            )

            // Layer #6: Socket Bevel Rim — 1px rgba(255, 255, 255, 0.12) outside neon ring
            drawCircle(
                color = buttonColor.copy(alpha = 0.12f),
                radius = ringRadius + 1.5.dp.toPx(),
                style = if (isPressed)  Stroke(width = 20 .dp.toPx()) else  Stroke(width = 2.dp.toPx())
            )

            // .lens: Acrylic Glass Lens Specular Reflections
            val lensInset = 1.dp.toPx()
            drawArc(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isPressed) 0.06f else 0.14f),
                        Color.White.copy(alpha = if (isPressed) 0.02f else 0.05f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = h * 0.40f
                ),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(lensInset, lensInset),
                size = Size(w - lensInset * 2f, h - lensInset * 2f),
                style = Stroke(width = 1.dp.toPx())
            )

            // Bottom-right specular sheen
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.06f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.70f, h * 0.78f),
                    radius = w * 0.25f
                ),
                center = Offset(w * 0.70f, h * 0.78f),
                radius = w * 0.25f
            )
        }

        // .glyph: Magnified Optical Glyph with Directional Shift & Horizon Clipping
        val psShape = getPlayStationShape(activeLabel)
        val glyphAlpha = if (isPressed) 0.85f else 1.0f

        val glyphOffset = when (perspectiveDirection) {
            ButtonPerspectiveDirection.BOTTOM -> IntOffset(0, 7)
            ButtonPerspectiveDirection.TOP    -> IntOffset(0, -7)
            ButtonPerspectiveDirection.LEFT   -> IntOffset(-7, 0)
            ButtonPerspectiveDirection.RIGHT  -> IntOffset(7, 0)
            ButtonPerspectiveDirection.NONE   -> IntOffset(0, 0)
        }

        val glyphModifier = Modifier
            .offset { glyphOffset }
            .then(if (horizonClipShape != null) Modifier.clip(horizonClipShape) else Modifier)

        if (psShape != null) {
            PlayStationSymbol(
                shape = psShape,
                color = buttonColor.copy(alpha = glyphAlpha),
                size = 40.dp,
                modifier = glyphModifier
            )
        } else {
            Text(
                text = activeLabel,
                color = buttonColor.copy(alpha = glyphAlpha),
                fontSize = 46.sp,
                fontWeight = FontWeight.Bold,
                modifier = glyphModifier
            )
        }
    }
}
