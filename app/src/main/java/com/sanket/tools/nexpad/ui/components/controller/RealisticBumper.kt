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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.model.NexpadKeys
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel

/**
 * NEXPAD Mobile-Ergonomic Lens Bumper (LB / RB / L1 / R1)
 *
 * Implements the master engineering architecture from HTML_TO_COMPOSE_CONTROLLER_BLUEPRINT.md:
 * - Scaled & proportioned for smartphone gamepad ergonomics (154dp × 48dp)
 * - Flipped contour: squarer corner (10dp) hugs the outer screen boundary,
 *   aerodynamic rounded curve (26dp) points inward toward thumb reach.
 * - 7-Layer Display List Pipeline:
 *   1. Physical Drop Shadow (cast behind housing)
 *   2. Bezel housing & 1px casing rim
 *   3. Actuator surface dome (convex dark gradient + bottom undercut shadow)
 *   4. Neon Ring (inset 2.5dp, dual-pass halo bloom + core stroke)
 *   5. Magnifier Window (78dp × 30dp pill well with vignette + centered neon glyph)
 *   6. Dynamic press depth (translateY 2px, scale 0.95 with damped harmonic spring)
 *   7. Optical Glass Lens (top specular crescent, rim highlights, bottom-right sheen)
 */
@Composable
fun RealisticBumper(
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

    // Flipped Mobile Ergonomic Shape:
    // Left bumper: squarer (10dp) at screen outer edge, rounded (26dp) toward center
    // Right bumper: rounded (26dp) toward center, squarer (10dp) at screen outer edge
    val bumperShape = remember(isLeft) {
        if (isLeft) {
            RoundedCornerShape(
                topStart = 10.dp,
                topEnd = 26.dp,
                bottomEnd = 26.dp,
                bottomStart = 10.dp
            )
        } else {
            RoundedCornerShape(
                topStart = 26.dp,
                topEnd = 10.dp,
                bottomEnd = 10.dp,
                bottomStart = 26.dp
            )
        }
    }

    // Kinematic Physics Engine — Damped Harmonic Spring (Engine 2)
    val scaleAnim by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "bumper_scale"
    )
    val pressOffsetYAnim by animateFloatAsState(
        targetValue = if (isPressed) 2.0f else 0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "bumper_offset"
    )
    val ringAlphaAnim by animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0.70f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 800f),
        label = "bumper_ring_alpha"
    )
    val ringBloomAnim by animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0.40f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 800f),
        label = "bumper_ring_bloom"
    )

    // CSS: --glow: #a97cf0 (LB Purple / RB Cyan)
    val neonColor = remember(isRgbEnabled, isLeft) {
        if (isRgbEnabled) {
            if (isLeft) Color(0xFFA97CF0) else Color(0xFF00E5FF)
        } else Color(0xFFD8DEE9)
    }

    val rgbBloomAlpha by animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0.45f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "realistic_bumper_rgb_bloom"
    )

    // Body surface dome gradient: radial-gradient(circle at 50% 55%, #232527 0%, #0c0d0e 75%, #000 100%)
    val baseDomeGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF232527),
                Color(0xFF0C0D0E),
                Color(0xFF000000)
            ),
            center = Offset(0.50f, 0.55f),
            radius = 280f
        )
    }

    val currentOnVibrate by rememberUpdatedState(onVibrate)
    val currentViewModel by rememberUpdatedState(viewModel)

    Box(
        modifier = modifier
            .size(154.dp, 48.dp)
            .drawBehind {
                if (isRgbEnabled) {
                    val padX = 14.dp.toPx()
                    val padY = 8.dp.toPx()

                    // 1. Asymmetrical shoulder stadium aura
                    drawRoundRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                neonColor.copy(alpha = rgbBloomAlpha * (if (isPressed) 0.55f else 0.35f)),
                                neonColor.copy(alpha = rgbBloomAlpha * 0.15f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = size.width * 0.55f
                        ),
                        topLeft = Offset(-padX, -padY),
                        size = Size(size.width + padX * 2f, size.height + padY * 2f),
                        cornerRadius = CornerRadius(22.dp.toPx(), 22.dp.toPx())
                    )

                    // 2. Dual corner edge shoulder flares
                    val flareY = center.y
                    val flareLen = if (isPressed) 16.dp.toPx() else 8.dp.toPx()
                    val flareAlpha = rgbBloomAlpha * (if (isPressed) 0.85f else 0.45f)

                    val outerX = if (isLeft) 6.dp.toPx() else size.width - 6.dp.toPx()
                    val innerX = if (isLeft) size.width - 12.dp.toPx() else 12.dp.toPx()
                    val outerDir = if (isLeft) -1f else 1f

                    // Outer shoulder laser flare
                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(neonColor.copy(alpha = flareAlpha), Color.Transparent),
                            start = Offset(outerX, flareY),
                            end = Offset(outerX + outerDir * flareLen, flareY)
                        ),
                        start = Offset(outerX, flareY),
                        end = Offset(outerX + outerDir * flareLen, flareY),
                        strokeWidth = if (isPressed) 3.5.dp.toPx() else 2.dp.toPx()
                    )

                    // Inner rounded shoulder arc flare
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                neonColor.copy(alpha = flareAlpha * 0.80f),
                                Color.Transparent
                            ),
                            center = Offset(innerX, flareY),
                            radius = if (isPressed) 20.dp.toPx() else 12.dp.toPx()
                        ),
                        center = Offset(innerX, flareY),
                        radius = if (isPressed) 20.dp.toPx() else 12.dp.toPx()
                    )
                }
            }
            .shadow(
                elevation = if (isPressed) 1.dp else 4.dp,
                shape = bumperShape,
                ambientColor = if (isRgbEnabled) neonColor else Color.Black,
                spotColor = if (isRgbEnabled) neonColor else Color.Black
            )
            .border(
                width = 1.dp,
                color = Color.Black.copy(alpha = 0.50f),
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
        // Layer 2 & 3: Plunging Actuator Body (.lx-body)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scaleAnim
                    scaleY = scaleAnim
                    translationY = pressOffsetYAnim.dp.toPx()
                }
                .clip(bumperShape)
                .background(baseDomeGradient),
            contentAlignment = Alignment.Center
        ) {
            // Body shadows and highlights
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Top edge highlight: linear-gradient(180deg, rgba(255,255,255,0.07), transparent 42%)
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.07f), Color.Transparent),
                        startY = 0f,
                        endY = h * 0.42f
                    ),
                    size = Size(w, h * 0.42f)
                )

                // Bottom undercut shadow: inset 0 -5px 7px rgba(0,0,0,0.70)
                val undercutH = h * 0.40f
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.70f)),
                        startY = h - undercutH,
                        endY = h
                    ),
                    topLeft = Offset(0f, h - undercutH),
                    size = Size(w, undercutH)
                )

                // Pressed inset shadow: inset 0 3px 7px rgba(0,0,0,0.85)
                if (isPressed) {
                    val pressedH = h * 0.40f
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent),
                            startY = 0f,
                            endY = pressedH
                        ),
                        size = Size(w, pressedH)
                    )
                }
            }

            // Layer 5: Magnifier Window (.lx-window) — 78dp × 30dp (centered)
            val windowShape = RoundedCornerShape(15.dp)
            Box(
                modifier = Modifier
                    .size(78.dp, 30.dp)
                    .clip(windowShape),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val winW = size.width
                    val winH = size.height

                    // Window background: radial-gradient(ellipse at 50% 60%, #050506 0%, #121314 100%)
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF050506), Color(0xFF121314)),
                            center = Offset(winW * 0.50f, winH * 0.60f),
                            radius = winW * 0.60f
                        ),
                        size = Size(winW, winH)
                    )

                    // Inset top shadow: inset 0 2.5px 5px rgba(0,0,0,0.9)
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.90f), Color.Transparent),
                            startY = 0f,
                            endY = 8.dp.toPx()
                        ),
                        size = Size(winW, 8.dp.toPx())
                    )

                    // Vignette: radial-gradient(ellipse at 50% 50%, transparent 32%, rgba(0,0,0,0.82) 100%)
                    drawRect(
                        brush = Brush.radialGradient(
                            0.0f to Color.Transparent,
                            0.32f to Color.Transparent,
                            1.0f to Color.Black.copy(alpha = 0.82f),
                            center = Offset(winW / 2f, winH / 2f),
                            radius = winW / 2f
                        ),
                        size = Size(winW, winH)
                    )

                    // Bottom specular line: 0 1px 0 rgba(255,255,255,0.05)
                    drawLine(
                        color = Color.White.copy(alpha = 0.05f),
                        start = Offset(3.dp.toPx(), winH - 0.5f),
                        end = Offset(winW - 3.dp.toPx(), winH - 0.5f),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Glyph (.lx-g): font-weight: 500, font-size: 20sp, letter-spacing: 1.5sp, color: var(--glow)
                Text(
                    text = displayLabel ?: key,
                    color = neonColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    letterSpacing = 1.5.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Layer 4 & 7: Stationary Overlays (Neon Ring + Glass Lens)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // --- Layer 4: Neon Ring (.lx-ring) ---
            val ringInset = 2.5.dp.toPx()
            val ringPath = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = Rect(ringInset, ringInset, w - ringInset, h - ringInset),
                        topLeft = CornerRadius(if (isLeft) 7.5.dp.toPx() else 23.5.dp.toPx()),
                        topRight = CornerRadius(if (isLeft) 23.5.dp.toPx() else 7.5.dp.toPx()),
                        bottomRight = CornerRadius(if (isLeft) 23.5.dp.toPx() else 7.5.dp.toPx()),
                        bottomLeft = CornerRadius(if (isLeft) 7.5.dp.toPx() else 23.5.dp.toPx())
                    )
                )
            }

            // Outer bloom pass
            drawPath(
                path = ringPath,
                color = neonColor.copy(alpha = (if (isPressed) 0.50f else 0.25f) * ringBloomAnim),
                style = Stroke(width = 5.dp.toPx())
            )
            // Inner bloom pass
            drawPath(
                path = ringPath,
                color = neonColor.copy(alpha = (if (isPressed) 0.40f else 0.18f) * ringBloomAnim),
                style = Stroke(width = 3.dp.toPx())
            )
            // Core crisp stroke: 2px solid var(--glow)
            drawPath(
                path = ringPath,
                color = neonColor.copy(alpha = ringAlphaAnim),
                style = Stroke(width = 2.dp.toPx())
            )

            // --- Layer 7: Glass Lens (.lx-lens) ---
            // Specular sheen circle: circle at 70% 78%, rgba(255,255,255,0.06) 0%, transparent 40%
            drawCircle(
                brush = Brush.radialGradient(
                    0.0f to Color.White.copy(alpha = 0.06f),
                    0.40f to Color.Transparent,
                    1.0f to Color.Transparent,
                    center = Offset(w * 0.70f, h * 0.78f),
                    radius = w * 0.30f
                ),
                center = Offset(w * 0.70f, h * 0.78f),
                radius = w * 0.30f
            )

            // Top specular edge highlight: inset 0 2px 2px rgba(255,255,255,0.10)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.10f), Color.Transparent),
                    startY = 0f,
                    endY = 3.5.dp.toPx()
                ),
                size = Size(w, 3.5.dp.toPx())
            )

            // Specular border lines
            val rtl = if (isLeft) 10.dp.toPx() else 26.dp.toPx()
            val rtr = if (isLeft) 26.dp.toPx() else 10.dp.toPx()
            val rbl = if (isLeft) 10.dp.toPx() else 26.dp.toPx()
            val rbr = if (isLeft) 26.dp.toPx() else 10.dp.toPx()

            // Top specular line: border-top 1px solid rgba(255,255,255,0.12)
            drawLine(
                color = Color.White.copy(alpha = 0.12f),
                start = Offset(rtl, 0.5f),
                end = Offset(w - rtr, 0.5f),
                strokeWidth = 1.dp.toPx()
            )

            // Bottom line: border-bottom 1px solid rgba(0,0,0,0.30)
            drawLine(
                color = Color.Black.copy(alpha = 0.30f),
                start = Offset(rbl, h - 0.5f),
                end = Offset(w - rbr, h - 0.5f),
                strokeWidth = 1.dp.toPx()
            )
        }
    }
}
