package com.sanket.tools.nexpad.ui.components.controller.bumper

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
import com.sanket.tools.nexpad.model.NexpadKeys
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel
import kotlin.math.roundToInt

/**
 * NEXPAD Bumper F — Tube (LB / RB / L1 / R1)
 *
 * Implements the Bumper F "Tube" design from HTML_TO_COMPOSE_CONTROLLER_BLUEPRINT.md:
 * - Cylindrical glass tube pill contour (154dp × 54dp, corner radius 27dp).
 * - Dynamic Liquid Surge: Illuminated neon liquid level surges horizontally across the tube
 *   (0% to 100% width) on press with meniscus meniscus tip.
 * - Laboratory Calibration Ticks: Repeating horizontal tick marks (1dp tick every 10dp)
 *   running along the bottom of the tube.
 * - Recessed Optical Window (74dp × 28dp): Centered pill aperture framing the glyph.
 * - Emissive Neon Perimeter Ring & Optical Glass Lens.
 * - Physical spring kinematics (stiffness 440, damping 0.68) with haptic vibration feedback.
 */
@Composable
fun TubeBumper(
    key: String,
    isConnected: Boolean,
    onVibrate: () -> Unit,
    viewModel: GamepadViewModel,
    isRgbEnabled: Boolean,
    modifier: Modifier = Modifier,
    displayLabel: String? = null
) {
    var isPressed by remember { mutableStateOf(false) }
    val isLeft = remember(key) {
        val upper = key.uppercase()
        upper == NexpadKeys.LB || upper == "L1" || upper == "LEFT"
    }

    val bumperShape = remember { RoundedCornerShape(27.dp) }

    // Kinematic Physics Engine — Damped Harmonic Spring
    val scaleAnim by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "tube_bumper_scale"
    )
    val pressOffsetYAnim by animateFloatAsState(
        targetValue = if (isPressed) 2.0f else 0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "tube_bumper_offset"
    )
    val ringBloomAlpha by animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0.70f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 800f),
        label = "tube_ring_bloom"
    )
    val rgbBloomAlpha by animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0.45f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "tube_bumper_rgb_bloom"
    )

    // Dynamic horizontal liquid surge animation: sweeps from 0% to 100% width on press
    val liquidProgress by animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 380f),
        label = "tube_liquid_level"
    )

    // CSS: --glow: #4fa8ff (Electric Sky Blue) for LB, Hot Coral for RB
    val neonColor = remember(isRgbEnabled, isLeft) {
        if (isRgbEnabled) {
            if (isLeft) Color(0xFF4FA8FF) else Color(0xFFFF4F81)
        } else {
            Color(0xFFD8DEE9)
        }
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

    // Recessed 3D window background
    val windowGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF050506),
                Color(0xFF121314)
            ),
            center = Offset(0.50f, 0.60f),
            radius = 120f
        )
    }

    Box(
        modifier = modifier
            .size(154.dp, 54.dp)
            .graphicsLayer {
                scaleX = scaleAnim
                scaleY = scaleAnim
            }
            .offset { IntOffset(0, pressOffsetYAnim.dp.roundToPx()) }
            .drawBehind {
                if (isRgbEnabled) {
                    val padX = 14.dp.toPx()
                    val padY = 8.dp.toPx()

                    // 1. Ambient stadium floor aura
                    drawRoundRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                neonColor.copy(alpha = rgbBloomAlpha * (if (isPressed) 0.50f else 0.30f)),
                                neonColor.copy(alpha = rgbBloomAlpha * 0.12f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = size.width * 0.55f
                        ),
                        topLeft = Offset(-padX, -padY),
                        size = Size(size.width + padX * 2f, size.height + padY * 2f),
                        cornerRadius = CornerRadius(22.dp.toPx(), 22.dp.toPx())
                    )

                    // 2. Ionized Plasma Tube Column
                    val tubeH = 18.dp.toPx()
                    val tubeW = (size.width - 24.dp.toPx()) * (0.45f + 0.55f * liquidProgress)
                    val tubeLeft = center.x - tubeW / 2f

                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                neonColor.copy(alpha = rgbBloomAlpha * (if (isPressed) 0.80f else 0.40f)),
                                Color.White.copy(alpha = rgbBloomAlpha * (if (isPressed) 0.95f else 0.55f)),
                                neonColor.copy(alpha = rgbBloomAlpha * (if (isPressed) 0.80f else 0.40f))
                            ),
                            startX = tubeLeft,
                            endX = tubeLeft + tubeW
                        ),
                        topLeft = Offset(tubeLeft, center.y - tubeH / 2f),
                        size = Size(tubeW, tubeH),
                        cornerRadius = CornerRadius(tubeH / 2f, tubeH / 2f)
                    )

                    // 3. Glowing Electrode Terminal Endcaps
                    val nodeRadius = if (isPressed) 14.dp.toPx() else 8.dp.toPx()
                    val nodeAlpha = rgbBloomAlpha * (if (isPressed) 0.90f else 0.50f)
                    val leftNodeX = tubeLeft
                    val rightNodeX = tubeLeft + tubeW

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = nodeAlpha),
                                neonColor.copy(alpha = nodeAlpha * 0.70f),
                                Color.Transparent
                            ),
                            center = Offset(leftNodeX, center.y),
                            radius = nodeRadius
                        ),
                        center = Offset(leftNodeX, center.y),
                        radius = nodeRadius
                    )

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = nodeAlpha),
                                neonColor.copy(alpha = nodeAlpha * 0.70f),
                                Color.Transparent
                            ),
                            center = Offset(rightNodeX, center.y),
                            radius = nodeRadius
                        ),
                        center = Offset(rightNodeX, center.y),
                        radius = nodeRadius
                    )
                }
            }
            // Outer drop shadow
            .shadow(
                elevation = if (isPressed) 1.dp else 4.dp,
                shape = bumperShape,
                ambientColor = if (isRgbEnabled) neonColor else Color.Black,
                spotColor = if (isRgbEnabled) neonColor else Color.Black
            )
            .clip(bumperShape)
            .background(if (isPressed) pressedDomeGradient else baseDomeGradient)
            // 1px casing rim
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
        // Multi-Layer Canvas: Undercut shadow, Liquid Level Surge, Calibration Ticks, Neon Ring, Lens
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

            // Dynamic Liquid Surge (.tube-liq: sweeps left to right with glowing meniscus)
            if (liquidProgress > 0.01f) {
                val liqW = w * liquidProgress
                // Liquid column body
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.32f),
                            Color.Transparent,
                            neonColor.copy(alpha = 0.75f)
                        ),
                        startY = 0f,
                        endY = h
                    ),
                    topLeft = Offset(0f, 0f),
                    size = Size(liqW, h)
                )
                drawRect(
                    color = neonColor.copy(alpha = 0.55f),
                    topLeft = Offset(0f, 0f),
                    size = Size(liqW, h)
                )

                // Meniscus convex front bubble
                val meniscusRadius = 14.dp.toPx()
                drawCircle(
                    color = neonColor.copy(alpha = 0.90f),
                    center = Offset(liqW, h / 2f),
                    radius = meniscusRadius
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.45f),
                    center = Offset(liqW, h / 2f),
                    radius = meniscusRadius * 0.60f
                )
            }

            // Calibration ruler ticks along bottom (.tube-ticks: 1px tick every 10dp)
            val tickYStart = h - 9.dp.toPx()
            val tickYEnd = h - 3.dp.toPx()
            val tickSpacing = 10.dp.toPx()
            var curTickX = 16.dp.toPx()
            while (curTickX < w - 16.dp.toPx()) {
                drawLine(
                    color = Color.White.copy(alpha = 0.22f),
                    start = Offset(curTickX, tickYStart),
                    end = Offset(curTickX, tickYEnd),
                    strokeWidth = 1.dp.toPx()
                )
                curTickX += tickSpacing
            }

            // Neon perimeter ring — inset 2.5dp with pill radii
            val ringInset = 2.5.dp.toPx()
            val ringRadius = 27.dp.toPx() - ringInset
            // Outer bloom pass
            drawRoundRect(
                color = neonColor.copy(alpha = (if (isPressed) 0.60f else 0.28f) * ringBloomAlpha),
                topLeft = Offset(ringInset, ringInset),
                size = Size(w - ringInset * 2f, h - ringInset * 2f),
                cornerRadius = CornerRadius(ringRadius, ringRadius),
                style = Stroke(width = 4.dp.toPx())
            )
            // Core crisp stroke
            drawRoundRect(
                color = neonColor.copy(alpha = (if (isPressed) 1.0f else 0.70f) * ringBloomAlpha),
                topLeft = Offset(ringInset, ringInset),
                size = Size(w - ringInset * 2f, h - ringInset * 2f),
                cornerRadius = CornerRadius(ringRadius, ringRadius),
                style = Stroke(width = 1.8.dp.toPx())
            )

            // Top crescent specular sheen (optical glass lens)
            val sheenH = h * 0.40f
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
                topLeft = Offset(ringInset, ringInset),
                size = Size(w - ringInset * 2f, sheenH)
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

            // Bottom-right specular sheen circle
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.06f), Color.Transparent),
                    center = Offset(w * 0.70f, h * 0.78f),
                    radius = w * 0.20f
                ),
                center = Offset(w * 0.70f, h * 0.78f),
                radius = w * 0.20f
            )
        }

        // Recessed Optical Window (.lx-window: 74dp × 28dp)
        val windowShape = RoundedCornerShape(14.dp)
        Box(
            modifier = Modifier
                .size(74.dp, 28.dp)
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
                    color = Color.White.copy(alpha = 0.05f),
                    shape = windowShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val winW = size.width
                val winH = size.height

                // Inset top shadow
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.90f), Color.Transparent),
                        startY = 0f,
                        endY = 6.dp.toPx()
                    ),
                    size = Size(winW, 6.dp.toPx())
                )

                // Deep vignette
                drawRect(
                    brush = Brush.radialGradient(
                        0.0f to Color.Transparent,
                        0.32f to Color.Transparent,
                        1.0f to Color.Black.copy(alpha = 0.82f),
                        center = Offset(winW / 2f, winH / 2f),
                        radius = winW * 0.50f
                    )
                )

                // Bottom specular edge line
                drawLine(
                    color = Color.White.copy(alpha = 0.05f),
                    start = Offset(4.dp.toPx(), winH - 0.5f),
                    end = Offset(winW - 4.dp.toPx(), winH - 0.5f),
                    strokeWidth = 1.dp.toPx()
                )
            }

            val labelText = displayLabel ?: key
            Text(
                text = labelText,
                color = neonColor,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
