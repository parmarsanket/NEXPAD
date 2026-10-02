package com.sanket.tools.nexpad.ui.components.controller

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.model.NexpadKeys
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel
import kotlin.math.roundToInt

/**
 * NEXPAD Bumper E — Underglow (LB / RB / L1 / R1)
 *
 * Implements the Bumper E "Underglow" design from HTML_TO_COMPOSE_CONTROLLER_BLUEPRINT.md:
 * - Mobile-ergonomic contour (154dp × 54dp): 12dp top outer corner, 14dp top inner,
 *   with an aerodynamic 27dp rounded bottom hull.
 * - Bottom Neon Underglow Bar: Horizontal ground strip (3dp thick, 12dp margin) emitting
 *   radiant downward neon ground illumination.
 * - Dynamic Neon Surge Flood: Linear color wash surges upwards (0% to 100% height) on press,
 *   flooding the dark acrylic body with glowing light.
 * - High-Contrast Centered Typography: Bold glyph (24sp) centered directly on the actuator body.
 * - Optical Glass Lens: Top crescent highlight, specular edge line, and bottom-right sheen.
 * - Physical spring kinematics (stiffness 440, damping 0.68) with haptic feedback.
 */
@Composable
fun UnderglowBumper(
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

    // Outer contour: 14dp top shoulder, 27dp aerodynamic bottom hull
    val bumperShape = remember(isLeft) {
        if (isLeft) {
            RoundedCornerShape(topStart = 12.dp, topEnd = 14.dp, bottomEnd = 27.dp, bottomStart = 20.dp)
        } else {
            RoundedCornerShape(topStart = 14.dp, topEnd = 12.dp, bottomEnd = 20.dp, bottomStart = 27.dp)
        }
    }

    // Kinematic Physics Engine — Damped Harmonic Spring
    val scaleAnim by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "under_bumper_scale"
    )
    val pressOffsetYAnim by animateFloatAsState(
        targetValue = if (isPressed) 2.0f else 0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "under_bumper_offset"
    )

    // Dynamic Neon Flood surge: sweeps from 0% to 100% height on press
    val floodHeightProgress by animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "under_flood_height"
    )

    // Underglow ground bar bloom intensity
    val underglowBloomAlpha by animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0.85f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 600f),
        label = "underglow_bloom"
    )

    val rgbBloomAlpha by animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0.45f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "under_bumper_rgb_bloom"
    )

    // CSS: --glow: #5cf29a (Neon Mint) for LB, Hot Coral for RB
    val neonColor = remember(isRgbEnabled, isLeft) {
        if (isRgbEnabled) {
            if (isLeft) Color(0xFF5CF29A) else Color(0xFFFF5252)
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
                    val padY = 6.dp.toPx()

                    // 1. Ambient upper hull aura
                    drawRoundRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                neonColor.copy(alpha = rgbBloomAlpha * (if (isPressed) 0.45f else 0.25f)),
                                neonColor.copy(alpha = rgbBloomAlpha * 0.10f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = size.width * 0.55f
                        ),
                        topLeft = Offset(-padX, -padY),
                        size = Size(size.width + padX * 2f, size.height + padY * 2f),
                        cornerRadius = CornerRadius(22.dp.toPx(), 22.dp.toPx())
                    )

                    // 2. Automotive Ground-Effect Floor Wash Puddle beneath bottom hull
                    val puddleW = size.width * (0.85f + 0.25f * floodHeightProgress)
                    val puddleH = (22.dp + 12.dp * floodHeightProgress).toPx()
                    val puddleCenterY = size.height + 4.dp.toPx()
                    val puddleLeft = center.x - puddleW / 2f
                    val puddleAlpha = rgbBloomAlpha * underglowBloomAlpha * (if (isPressed) 0.90f else 0.60f)

                    // Soft diffuse ground reflection oval
                    drawOval(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                neonColor.copy(alpha = puddleAlpha * 0.85f),
                                neonColor.copy(alpha = puddleAlpha * 0.35f),
                                Color.Transparent
                            ),
                            center = Offset(center.x, puddleCenterY),
                            radius = puddleW * 0.55f
                        ),
                        topLeft = Offset(puddleLeft, puddleCenterY - puddleH / 2f),
                        size = Size(puddleW, puddleH)
                    )

                    // High-intensity core underglow slit reflection
                    val coreSlitW = puddleW * 0.65f
                    val coreSlitLeft = center.x - coreSlitW / 2f
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = puddleAlpha * 0.90f),
                                neonColor.copy(alpha = puddleAlpha * 0.95f),
                                Color.White.copy(alpha = puddleAlpha * 0.90f),
                                Color.Transparent
                            ),
                            startX = coreSlitLeft,
                            endX = coreSlitLeft + coreSlitW
                        ),
                        start = Offset(coreSlitLeft, size.height),
                        end = Offset(coreSlitLeft + coreSlitW, size.height),
                        strokeWidth = (if (isPressed) 3.5.dp else 2.dp).toPx()
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
        // Multi-Layer Canvas: Undercut shadow, Dynamic Neon Surge Flood, Optical Glass highlights
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

            // Dynamic Neon Surge Flood: Linear gradient surging upwards from the bottom ground bar
            if (floodHeightProgress > 0f) {
                val currentFloodH = h * floodHeightProgress
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            neonColor.copy(alpha = 0.55f * floodHeightProgress)
                        ),
                        startY = h - currentFloodH,
                        endY = h
                    ),
                    topLeft = Offset(0f, h - currentFloodH),
                    size = Size(w, currentFloodH)
                )
            }

            // Top crescent specular sheen (optical glass lens)
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
                topLeft = Offset(2.dp.toPx(), 2.dp.toPx()),
                size = Size(w - 4.dp.toPx(), sheenH)
            )

            // Top edge specular line
            val specStartX = if (isLeft) w * 0.15f else w * 0.12f
            val specEndX = if (isLeft) w * 0.88f else w * 0.85f
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isPressed) 0.12f else 0.45f),
                        Color.White.copy(alpha = if (isPressed) 0.04f else 0.16f)
                    ),
                    startX = specStartX,
                    endX = specEndX
                ),
                start = Offset(specStartX, 3.5f),
                end = Offset(specEndX, 3.5f),
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

        // Direct Centered Typography (.lx-g: 24sp, bold, high contrast over flood wash)
        val labelText = displayLabel ?: key
        Text(
            text = labelText,
            color = neonColor,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            letterSpacing = 2.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.graphicsLayer {
                val glyphScale = if (isPressed) 1.05f else 1.0f
                scaleX = glyphScale
                scaleY = glyphScale
            }
        )

        // Bottom Neon Underglow Bar (.lx-ring in Underglow mode: inset bottom 3dp, left/right 12dp, height 3dp)
        Canvas(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxSize()
        ) {
            val barInsetX = 12.dp.toPx()
            val barBottomY = size.height - 3.dp.toPx()
            val barH = 3.dp.toPx()
            val barW = size.width - barInsetX * 2f

            // Outer wide ground bloom
            drawRoundRect(
                color = neonColor.copy(alpha = (if (isPressed) 0.65f else 0.35f) * underglowBloomAlpha),
                topLeft = Offset(barInsetX - 4.dp.toPx(), barBottomY - 2.dp.toPx()),
                size = Size(barW + 8.dp.toPx(), barH + 4.dp.toPx()),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )

            // Core crisp ground bar
            drawRoundRect(
                color = neonColor.copy(alpha = (if (isPressed) 1.0f else 0.85f) * underglowBloomAlpha),
                topLeft = Offset(barInsetX, barBottomY),
                size = Size(barW, barH),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
        }
    }
}
