package com.sanket.tools.nexpad.ui.components.controller.trigger

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.graphics.PathEffect
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
 * Native Jetpack Compose implementation of the NEXPAD Target Trigger (Trigger E — Target / Concentric Rings).
 *
 * Translates the concentric circular radar target trigger HTML blueprint into high-performance Compose:
 * - Proportioned 92dp x 92dp compact circular trigger ratio matching bumper vertical profile
 * - Three concentric illuminated target rings igniting progressively from outside-in on pull:
 *     * Ring 0 (Outer, radius 37dp): ignites first from 0.0 to 0.1 pull
 *     * Ring 1 (Middle, radius 28dp): ignites second from 0.3 to 0.4 pull
 *     * Ring 2 (Inner, radius 19dp): ignites third from 0.6 to 0.7 pull
 * - Progressive lighting curve: dim 14% ghost visibility in idle, igniting into full 100% neon bloom
 * - Recessed circular optical eye window (32dp x 32dp, CircleShape) at center
 * - Dynamically reactive typography: glyph brightens from 45% idle alpha to 100% radiant bloom on full pull
 * - Pure optical layout: strictly omits percentage text readout per mobile design directive
 * - Multi-pass emissive neon rings, specular glass reflections, and spring kinematics
 */
@Composable
fun TargetTrigger(
    key: String,
    isConnected: Boolean,
    onVibrate: () -> Unit,
    viewModel: GamepadViewModel,
    isRgbEnabled: Boolean,
    modifier: Modifier = Modifier,
    displayLabel: String? = null
) {
    var isPressed by remember { mutableStateOf(false) }
    val isLeft = key.uppercase() == NexpadKeys.LT

    // Physical Spring Kinematics
    val scaleAnim by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "target_trigger_scale"
    )
    val rgbBloomAlpha by animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0.45f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "target_trigger_rgb_bloom"
    )
    // Dynamic Inward Target Pull Fill Progress (0.0 to 1.0)
    val fillProgress by animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 380f),
        label = "target_trigger_pull_fill"
    )

    // LT = Cyan (#00E5FF), RT = Coral Rose / Hot Pink (#FF5A7A) matching HTML --glow: #ff5a7a
    val neonColor = if (isLeft) Color(0xFF00E5FF) else Color(0xFFFF5A7A)

    // Acrylic convex dome background (#232527 -> #0c0d0e -> #000)
    val baseDomeGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF232527),
                Color(0xFF0C0D0E),
                Color(0xFF000000)
            ),
            center = Offset(0.50f, 0.45f),
            radius = 210f
        )
    }
    val pressedDomeGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF1B1D1E),
                Color(0xFF070808),
                Color(0xFF000000)
            ),
            center = Offset(0.50f, 0.48f),
            radius = 210f
        )
    }

    // Recessed circular eye window gradient (#050506 -> #121314)
    val windowGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF050506),
                Color(0xFF121314)
            ),
            center = Offset(0.50f, 0.60f),
            radius = 70f
        )
    }

    val currentOnVibrate by rememberUpdatedState(onVibrate)
    val currentViewModel by rememberUpdatedState(viewModel)

    val triggerShape = CircleShape
    val windowShape = CircleShape

    Box(
        modifier = modifier
            .size(92.dp, 92.dp)
            .graphicsLayer {
                scaleX = scaleAnim
                scaleY = scaleAnim
            }
            // Outer dynamic RGB aura
            .drawBehind {
                if (isRgbEnabled) {
                    val baseRadius = size.minDimension * 0.5f

                    // 1. Ambient tactical target halo
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                neonColor.copy(alpha = rgbBloomAlpha * (if (isPressed) 0.55f else 0.35f)),
                                neonColor.copy(alpha = rgbBloomAlpha * 0.15f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = size.minDimension * 0.75f
                        ),
                        radius = size.minDimension * 0.75f,
                        center = center
                    )

                    // 2. Concentric Sniper Reticle Rings tightening with pull
                    val reticleTighten = 1f - 0.20f * fillProgress
                    val outerReticleR = (baseRadius + 8.dp.toPx()) * reticleTighten
                    val innerReticleR = (baseRadius + 3.dp.toPx()) * reticleTighten

                    drawCircle(
                        color = neonColor.copy(alpha = rgbBloomAlpha * (if (isPressed) 0.85f else 0.45f)),
                        radius = outerReticleR,
                        center = center,
                        style = Stroke(
                            width = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()), 0f)
                        )
                    )
                    drawCircle(
                        color = neonColor.copy(alpha = rgbBloomAlpha * (if (isPressed) 0.95f else 0.60f)),
                        radius = innerReticleR,
                        center = center,
                        style = Stroke(width = 1.dp.toPx())
                    )

                    // 3. 4-Axis Laser Crosshair Beams (N, S, E, W)
                    val crosshairStart = innerReticleR + 2.dp.toPx()
                    val crosshairLen = (if (isPressed) 16.dp else 8.dp).toPx()
                    val crosshairEnd = crosshairStart + crosshairLen
                    val crosshairAlpha = rgbBloomAlpha * (if (isPressed) 0.90f else 0.50f)

                    // North & South
                    drawLine(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, neonColor.copy(alpha = crosshairAlpha)),
                            startY = center.y - crosshairEnd,
                            endY = center.y - crosshairStart
                        ),
                        start = Offset(center.x, center.y - crosshairEnd),
                        end = Offset(center.x, center.y - crosshairStart),
                        strokeWidth = 2f
                    )
                    drawLine(
                        brush = Brush.verticalGradient(
                            colors = listOf(neonColor.copy(alpha = crosshairAlpha), Color.Transparent),
                            startY = center.y + crosshairStart,
                            endY = center.y + crosshairEnd
                        ),
                        start = Offset(center.x, center.y + crosshairStart),
                        end = Offset(center.x, center.y + crosshairEnd),
                        strokeWidth = 2f
                    )

                    // West & East
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, neonColor.copy(alpha = crosshairAlpha)),
                            startX = center.x - crosshairEnd,
                            endX = center.x - crosshairStart
                        ),
                        start = Offset(center.x - crosshairEnd, center.y),
                        end = Offset(center.x - crosshairStart, center.y),
                        strokeWidth = 2f
                    )
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(neonColor.copy(alpha = crosshairAlpha), Color.Transparent),
                            startX = center.x + crosshairStart,
                            endX = center.x + crosshairEnd
                        ),
                        start = Offset(center.x + crosshairStart, center.y),
                        end = Offset(center.x + crosshairEnd, center.y),
                        strokeWidth = 2f
                    )
                }
            }
            // RGB-coordinated shadow
            .shadow(
                elevation = if (isPressed) 2.dp else 6.dp,
                shape = triggerShape,
                ambientColor = if (isRgbEnabled) neonColor else Color.Black,
                spotColor = if (isRgbEnabled) neonColor else Color.Black
            )
            .clip(triggerShape)
            .background(if (isPressed) pressedDomeGradient else baseDomeGradient)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.50f),
                        Color.Black.copy(alpha = 0.80f)
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
        contentAlignment = Alignment.Center
    ) {
        // LAYER STACK CANVAS: Undercut Shadow + 3 Concentric Target Rings + Outer Neon Ring + Specular Lens
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val center = Offset(w / 2f, h / 2f)
            val outerRadius = size.minDimension / 2f

            // Inset bottom shadow along trigger dome
            val insetH = h * 0.32f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = if (isPressed) 0.85f else 0.70f)),
                    startY = h - insetH,
                    endY = h
                ),
                topLeft = Offset(0f, h - insetH),
                size = Size(w, insetH)
            )

            // Outer chassis neon ring (.lx-ring)
            val ringRadius = outerRadius - 2.5.dp.toPx()
            drawCircle(
                color = neonColor.copy(alpha = rgbBloomAlpha * 0.50f),
                center = center,
                radius = ringRadius,
                style = Stroke(width = 3.5.dp.toPx())
            )
            drawCircle(
                color = neonColor.copy(alpha = if (isPressed) 1.0f else 0.70f),
                center = center,
                radius = ringRadius,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // =========================================================================
            // THREE CONCENTRIC TARGET RINGS (.rg style="--s:...;--i:...")
            // Outside-in progressive lighting curve:
            // opacity = clamp(0.14, (fillProgress - i * 0.30) * 12, 1.0)
            // =========================================================================
            val targetRadii = floatArrayOf(
                37.dp.toPx(), // Ring 0 (Outer: diameter 74dp)
                28.dp.toPx(), // Ring 1 (Middle: diameter 56dp)
                19.dp.toPx()  // Ring 2 (Inner: diameter 38dp)
            )

            for (i in 0..2) {
                val rawOpacity = (fillProgress - i * 0.30f) * 12f
                val ringOpacity = rawOpacity.coerceIn(0.14f, 1.0f)
                val r = targetRadii[i]

                // Emissive bloom pass when ring is ignited (> 25% lit)
                if (ringOpacity > 0.25f) {
                    drawCircle(
                        color = neonColor.copy(alpha = (ringOpacity * 0.40f).coerceAtMost(0.40f)),
                        center = center,
                        radius = r,
                        style = Stroke(width = 4.dp.toPx())
                    )
                }

                // Core crisp target ring filament
                drawCircle(
                    color = neonColor.copy(alpha = ringOpacity),
                    center = center,
                    radius = r,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }

            // Top crescent specular highlight arc (.lx-lens)
            val lensInset = 1.dp.toPx()
            drawArc(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isPressed) 0.06f else 0.14f),
                        Color.White.copy(alpha = if (isPressed) 0.02f else 0.05f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = h * 0.38f
                ),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(lensInset, lensInset),
                size = Size(w - lensInset * 2f, h - lensInset * 2f),
                style = Stroke(width = 1.dp.toPx())
            )

            // Bottom-right specular sheen oval
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.06f), Color.Transparent),
                    center = Offset(w * 0.70f, h * 0.78f),
                    radius = w * 0.22f
                ),
                center = Offset(w * 0.70f, h * 0.78f),
                radius = w * 0.22f
            )
        }

        // =========================================================================
        // RECESSED CIRCULAR OPTICAL EYE WINDOW (.lx-window 32dp x 32dp CircleShape)
        // =========================================================================
        Box(
            modifier = Modifier
                .size(32.dp)
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
            Canvas(modifier = Modifier.fillMaxSize()) {
                val winW = size.width
                val winH = size.height

                // Top lip shadow inside optical cavity
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent),
                        startY = 0f,
                        endY = 5.dp.toPx()
                    ),
                    size = Size(winW, 5.dp.toPx())
                )

                // Perimeter circular vignette (.lx-window::after)
                drawRect(
                    brush = Brush.radialGradient(
                        0.0f to Color.Transparent,
                        0.38f to Color.Transparent,
                        1.0f to Color.Black.copy(alpha = 0.85f),
                        center = Offset(winW / 2f, winH / 2f),
                        radius = winW * 0.50f
                    )
                )
            }

            // Dynamically reactive glyph brightness: 45% at idle to 100% full neon bloom on full pull
            val glyphAlpha = (0.45f + fillProgress * 0.55f).coerceIn(0.45f, 1.0f)

            Text(
                text = displayLabel ?: (if (isLeft) "LT" else "RT"),
                color = neonColor.copy(alpha = glyphAlpha),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                letterSpacing = 0.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
