package com.sanket.tools.nexpad.ui.components.controller

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.model.NexpadKeys
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel
import kotlin.math.abs
import kotlin.math.cos

/**
 * NEXPAD Bumper G — Flip (LB / RB / L1 / R1)
 *
 * Implements the Bumper G "Flip" design from HTML_TO_COMPOSE_CONTROLLER_BLUEPRINT.md:
 * - Symmetrical pill contour (154dp × 54dp, corner radius 27dp).
 * - 3D Card Flip Kinematics: The button rotates along the horizontal X axis (0° to 180°)
 *   with true 3D perspective camera depth.
 * - Double-Faced Actuator:
 *   - Face A (Resting / 0°-90°): Convex dark dome (#232527) with recessed optical window
 *     (96dp × 32dp) framing a luminous glowing glyph.
 *   - Face B (Active Flip / 90°-180°): High-energy golden neon plate (#FFD23F) with embossed
 *     dark tactical typography (#0A0B0C).
 * - Emissive Perimeter Neon Ring & Optical Glass Lens.
 * - Haptic vibration feedback on press.
 */
@Composable
fun FlipBumper(
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

    val density = LocalDensity.current.density
    val bumperShape = remember { RoundedCornerShape(27.dp) }

    // 3D Horizontal Flip Kinematics: 0° (Face A) to 180° (Face B)
    val flipRotationXAnim by animateFloatAsState(
        targetValue = if (isPressed) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.70f, stiffness = 420f),
        label = "flip_rotation_x"
    )

    val ringBloomAlpha by animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0.70f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 800f),
        label = "flip_ring_bloom"
    )

    val rgbBloomAlpha by animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0.45f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "flip_rgb_bloom"
    )

    // CSS: --glow: #ffd23f (Cyber Gold / Amber Neon) for LB, Coral for RB
    val neonColor = remember(isRgbEnabled, isLeft) {
        if (isRgbEnabled) {
            if (isLeft) Color(0xFFFFD23F) else Color(0xFFFF6B6B)
        } else {
            Color(0xFFD8DEE9)
        }
    }

    val currentOnVibrate by rememberUpdatedState(onVibrate)
    val currentViewModel by rememberUpdatedState(viewModel)

    // Convex dark dome background gradient for Face A
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

    // Recessed 3D window background for Face A
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

    // High-energy radiant neon gradient for Face B
    val neonFaceGradient = remember(neonColor) {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.40f),
                neonColor.copy(alpha = 0.95f),
                neonColor
            )
        )
    }

    Box(
        modifier = modifier
            .size(154.dp, 54.dp)
            .drawBehind {
                if (isRgbEnabled) {
                    val rotRad = Math.toRadians(flipRotationXAnim.toDouble())
                    val cosFactor = abs(cos(rotRad)).toFloat().coerceIn(0.04f, 1f)
                    val isBackFace = abs(flipRotationXAnim % 360f) > 90f

                    // 1. 3D anamorphic vertically-squashed stadium aura
                    val padX = 14.dp.toPx()
                    val halfH = (size.height / 2f + 8.dp.toPx()) * cosFactor
                    drawRoundRect(
                        brush = Brush.radialGradient(
                            colors = if (isBackFace) {
                                listOf(
                                    neonColor.copy(alpha = rgbBloomAlpha * 0.70f),
                                    neonColor.copy(alpha = rgbBloomAlpha * 0.25f),
                                    Color.Transparent
                                )
                            } else {
                                listOf(
                                    neonColor.copy(alpha = rgbBloomAlpha * 0.45f),
                                    neonColor.copy(alpha = rgbBloomAlpha * 0.12f),
                                    Color.Transparent
                                )
                            },
                            center = center,
                            radius = size.width * 0.55f
                        ),
                        topLeft = Offset(-padX, center.y - halfH),
                        size = Size(size.width + padX * 2f, halfH * 2f),
                        cornerRadius = CornerRadius(22.dp.toPx() * cosFactor, 22.dp.toPx() * cosFactor)
                    )

                    // 2. Horizon edge-on laser slit line (peaks at 90° rotation)
                    val edgeOnIntensity = 1f - cosFactor
                    if (edgeOnIntensity > 0.05f) {
                        val slitLength = size.width * (0.85f + 0.25f * edgeOnIntensity)
                        val slitStartX = center.x - slitLength / 2f
                        val slitEndX = center.x + slitLength / 2f

                        drawLine(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    neonColor.copy(alpha = rgbBloomAlpha * edgeOnIntensity * 0.85f),
                                    Color.White.copy(alpha = rgbBloomAlpha * edgeOnIntensity * 0.95f),
                                    neonColor.copy(alpha = rgbBloomAlpha * edgeOnIntensity * 0.85f),
                                    Color.Transparent
                                ),
                                startX = slitStartX,
                                endX = slitEndX
                            ),
                            start = Offset(slitStartX, center.y),
                            end = Offset(slitEndX, center.y),
                            strokeWidth = (2.5f + 3f * edgeOnIntensity).dp.toPx()
                        )
                    }
                }
            }
            .shadow(
                elevation = if (isPressed) 1.dp else 4.dp,
                shape = bumperShape,
                ambientColor = if (isRgbEnabled) neonColor else Color.Black,
                spotColor = if (isRgbEnabled) neonColor else Color.Black
            )
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
        // 3D Flipping Card Container
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationX = flipRotationXAnim
                    cameraDistance = 16f * density
                },
            contentAlignment = Alignment.Center
        ) {
            val labelText = displayLabel ?: key

            if (flipRotationXAnim <= 90f) {
                // ==========================================
                // FACE A: Resting Dark Glass Optical Dome
                // ==========================================
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(bumperShape)
                        .background(baseDomeGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        // Inset bottom shadow
                        val insetH = h * 0.40f
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.70f)),
                                startY = h - insetH,
                                endY = h
                            ),
                            topLeft = Offset(0f, h - insetH),
                            size = Size(w, insetH)
                        )

                        // Top crescent specular sheen (optical glass lens)
                        val sheenH = h * 0.40f
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.14f),
                                    Color.White.copy(alpha = 0.04f),
                                    Color.Transparent
                                ),
                                startY = 0f,
                                endY = sheenH
                            ),
                            topLeft = Offset(2.dp.toPx(), 2.dp.toPx()),
                            size = Size(w - 4.dp.toPx(), sheenH)
                        )
                    }

                    // Optical Window (96dp × 32dp)
                    val windowShape = RoundedCornerShape(16.dp)
                    Box(
                        modifier = Modifier
                            .size(96.dp, 32.dp)
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
                        }

                        Text(
                            text = labelText,
                            color = neonColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            letterSpacing = 2.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // ==========================================
                // FACE B: Active Flipped Radiant Neon Plate
                // ==========================================
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            // Invert 180° so typography and highlights render upright
                            rotationX = 180f
                        }
                        .clip(bumperShape)
                        .background(neonFaceGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        // Specular glass glare reflection across golden plate
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.45f),
                                    Color.White.copy(alpha = 0.10f),
                                    Color.Transparent
                                ),
                                startY = 0f,
                                endY = h * 0.50f
                            ),
                            topLeft = Offset(0f, 0f),
                            size = Size(w, h * 0.50f)
                        )

                        // Inset border frame
                        drawRoundRect(
                            color = Color.Black.copy(alpha = 0.15f),
                            topLeft = Offset(2.dp.toPx(), 2.dp.toPx()),
                            size = Size(w - 4.dp.toPx(), h - 4.dp.toPx()),
                            cornerRadius = CornerRadius(25.dp.toPx(), 25.dp.toPx()),
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }

                    // Tactical Embossed Dark Typography (.ff.b .lx-g: color #0A0B0C)
                    Text(
                        text = labelText,
                        color = Color(0xFF0A0B0C),
                        fontWeight = FontWeight.Black,
                        fontSize = 26.sp,
                        letterSpacing = 2.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Overlay: Emissive Neon Perimeter Ring (.lx-ring)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clip(bumperShape)
        ) {
            val w = size.width
            val h = size.height
            val ringInset = 2.5.dp.toPx()
            val ringRadius = 27.dp.toPx() - ringInset

            drawRoundRect(
                color = neonColor.copy(alpha = (if (isPressed) 0.65f else 0.28f) * ringBloomAlpha),
                topLeft = Offset(ringInset, ringInset),
                size = Size(w - ringInset * 2f, h - ringInset * 2f),
                cornerRadius = CornerRadius(ringRadius, ringRadius),
                style = Stroke(width = 4.dp.toPx())
            )

            drawRoundRect(
                color = neonColor.copy(alpha = (if (isPressed) 1.0f else 0.70f) * ringBloomAlpha),
                topLeft = Offset(ringInset, ringInset),
                size = Size(w - ringInset * 2f, h - ringInset * 2f),
                cornerRadius = CornerRadius(ringRadius, ringRadius),
                style = Stroke(width = 1.8.dp.toPx())
            )
        }
    }
}
