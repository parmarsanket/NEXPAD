package com.sanket.tools.nexpad.ui.components.controller.trigger

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.model.NexpadKeys
import com.sanket.tools.nexpad.ui.theme.NeonPalette
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
    isConnected: Boolean = true,
    onVibrate: () -> Unit = {},
    viewModel: GamepadViewModel? = null,
    isRgbEnabled: Boolean = true,
    modifier: Modifier = Modifier,
    displayLabel: String? = null,
    isInteractive: Boolean = true
) {
    var isPressedLocally by remember { mutableStateOf(false) }
    val isPressed = isPressedLocally || com.sanket.tools.nexpad.ui.components.controller.isControlPressed(key)
    val isLeft = key.uppercase() == NexpadKeys.LT

    // Physical Spring Kinematics
    val scaleAnim = animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "target_trigger_scale"
    )
    val rgbBloomAlpha = animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0.45f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "target_trigger_rgb_bloom"
    )
    // Dynamic Inward Target Pull Fill Progress (0.0 to 1.0)
    val fillProgress = animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 380f),
        label = "target_trigger_pull_fill"
    )

    // LT = Cyan (#00E5FF), RT = Coral Rose / Hot Pink (#FF5A7A) matching HTML --glow: #ff5a7a
    val neonColor = if (isLeft) NeonPalette.Cyan else NeonPalette.HotCoral

    // Acrylic convex dome background (#232527 -> #0c0d0e -> #000)
    val baseDomeGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                NeonPalette.MetalBase,
                NeonPalette.MetalShadow,
                NeonPalette.PureBlack
            ),
            center = Offset(0.50f, 0.45f),
            radius = 210f
        )
    }
    val pressedDomeGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                NeonPalette.MetalMid,
                NeonPalette.MetalAbyss,
                NeonPalette.PureBlack
            ),
            center = Offset(0.50f, 0.48f),
            radius = 210f
        )
    }

    // Recessed circular eye window gradient (#050506 -> #121314)
    val windowGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                NeonPalette.MetalDeep,
                NeonPalette.MetalDark
            ),
            center = Offset(0.50f, 0.60f),
            radius = 70f
        )
    }

    val chassisBorderBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color.Black.copy(alpha = 0.50f),
                Color.Black.copy(alpha = 0.80f)
            )
        )
    }

    val insetShadowColorsPressed = remember {
        listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
    }
    val insetShadowColorsNormal = remember {
        listOf(Color.Transparent, Color.Black.copy(alpha = 0.70f))
    }
    val lensColorsPressed = remember {
        listOf(Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.02f), Color.Transparent)
    }
    val lensColorsNormal = remember {
        listOf(Color.White.copy(alpha = 0.14f), Color.White.copy(alpha = 0.05f), Color.Transparent)
    }
    val specularSheenColors = remember {
        listOf(Color.White.copy(alpha = 0.06f), Color.Transparent)
    }
    val windowLipShadowColors = remember {
        listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
    }
    val windowVignetteColors = remember {
        arrayOf(
            0.0f to Color.Transparent,
            0.38f to Color.Transparent,
            1.0f to Color.Black.copy(alpha = 0.85f)
        )
    }

    val currentOnVibrate by rememberUpdatedState(onVibrate)
    val currentViewModel by rememberUpdatedState(viewModel)

    val triggerShape = CircleShape
    val windowShape = CircleShape

    val density = LocalDensity.current.density
    val strokeOuterReticle = remember(density) {
        Stroke(
            width = 1.5f * density,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f * density, 4f * density), 0f)
        )
    }
    val strokeInnerReticle = remember(density) { Stroke(width = 1f * density) }
    val strokeChassisRingBloom = remember(density) { Stroke(width = 3.5f * density) }
    val strokeChassisRingCore = remember(density) { Stroke(width = 1.5f * density) }
    val strokeTargetRingBloom = remember(density) { Stroke(width = 4f * density) }
    val strokeTargetRingCore = remember(density) { Stroke(width = 1.5f * density) }
    val strokeLensArc = remember(density) { Stroke(width = 1f * density) }

    val targetHaloColors = remember(neonColor) {
        arrayListOf(Color.Transparent, Color.Transparent, Color.Transparent)
    }
    val crosshairColorsIn = remember(neonColor) {
        arrayListOf(Color.Transparent, Color.Transparent)
    }
    val crosshairColorsOut = remember(neonColor) {
        arrayListOf(Color.Transparent, Color.Transparent)
    }

    Box(
        modifier = modifier
            .size(92.dp, 92.dp)
            .graphicsLayer { /* Root RenderNode isolation */ }
            // Outer dynamic RGB aura
            .drawBehind {
                if (isRgbEnabled) {
                    val baseRadius = size.minDimension * 0.5f

                    // 1. Ambient tactical target halo
                    targetHaloColors[0] = neonColor.copy(alpha = rgbBloomAlpha.value * (if (isPressed) 0.55f else 0.35f))
                    targetHaloColors[1] = neonColor.copy(alpha = rgbBloomAlpha.value * 0.15f)
                    targetHaloColors[2] = Color.Transparent
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = targetHaloColors,
                            center = center,
                            radius = size.minDimension * 0.75f
                        ),
                        radius = size.minDimension * 0.75f,
                        center = center
                    )

                    // 2. Concentric Sniper Reticle Rings tightening with pull
                    val reticleTighten = 1f - 0.20f * fillProgress.value
                    val outerReticleR = (baseRadius + 8f * density) * reticleTighten
                    val innerReticleR = (baseRadius + 3f * density) * reticleTighten

                    drawCircle(
                        color = neonColor.copy(alpha = rgbBloomAlpha.value * (if (isPressed) 0.85f else 0.45f)),
                        radius = outerReticleR,
                        center = center,
                        style = strokeOuterReticle
                    )
                    drawCircle(
                        color = neonColor.copy(alpha = rgbBloomAlpha.value * (if (isPressed) 0.95f else 0.60f)),
                        radius = innerReticleR,
                        center = center,
                        style = strokeInnerReticle
                    )

                    // 3. 4-Axis Laser Crosshair Beams (N, S, E, W)
                    val crosshairStart = innerReticleR + 2f * density
                    val crosshairLen = (if (isPressed) 16f else 8f) * density
                    val crosshairEnd = crosshairStart + crosshairLen
                    val crosshairAlpha = rgbBloomAlpha.value * (if (isPressed) 0.90f else 0.50f)

                    crosshairColorsIn[0] = Color.Transparent
                    crosshairColorsIn[1] = neonColor.copy(alpha = crosshairAlpha)
                    crosshairColorsOut[0] = neonColor.copy(alpha = crosshairAlpha)
                    crosshairColorsOut[1] = Color.Transparent

                    // North & South
                    drawLine(
                        brush = Brush.verticalGradient(
                            colors = crosshairColorsIn,
                            startY = center.y - crosshairEnd,
                            endY = center.y - crosshairStart
                        ),
                        start = Offset(center.x, center.y - crosshairEnd),
                        end = Offset(center.x, center.y - crosshairStart),
                        strokeWidth = 2f
                    )
                    drawLine(
                        brush = Brush.verticalGradient(
                            colors = crosshairColorsOut,
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
                            colors = crosshairColorsIn,
                            startX = center.x - crosshairEnd,
                            endX = center.x - crosshairStart
                        ),
                        start = Offset(center.x - crosshairEnd, center.y),
                        end = Offset(center.x - crosshairStart, center.y),
                        strokeWidth = 2f
                    )
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = crosshairColorsOut,
                            startX = center.x + crosshairStart,
                            endX = center.x + crosshairEnd
                        ),
                        start = Offset(center.x + crosshairStart, center.y),
                        end = Offset(center.x + crosshairEnd, center.y),
                        strokeWidth = 2f
                    )
                }
            }
            // RGB-coordinated shadow placed before motion scaling to prevent CPU shadow tessellation
            .shadow(
                elevation = if (isPressed) 2.dp else 6.dp,
                shape = triggerShape,
                ambientColor = if (isRgbEnabled) neonColor else Color.Black,
                spotColor = if (isRgbEnabled) neonColor else Color.Black
            )
            .graphicsLayer {
                scaleX = scaleAnim.value
                scaleY = scaleAnim.value
            }
            .clip(triggerShape)
            .background(if (isPressed) pressedDomeGradient else baseDomeGradient)
            .border(
                width = 1.dp,
                brush = chassisBorderBrush,
                shape = triggerShape
            )
            .then(
                if (isInteractive) {
                    Modifier.pointerInput(key) {
                        detectTapGestures(
                            onPress = {
                                currentOnVibrate()
                                isPressedLocally = true
                                currentViewModel?.updateButton(key, true)
                                tryAwaitRelease()
                                isPressedLocally = false
                                currentViewModel?.updateButton(key, false)
                            }
                        )
                    }
                } else Modifier
            ),
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
                    colors = if (isPressed) insetShadowColorsPressed else insetShadowColorsNormal,
                    startY = h - insetH,
                    endY = h
                ),
                topLeft = Offset(0f, h - insetH),
                size = Size(w, insetH)
            )

            // Outer chassis neon ring (.lx-ring)
            val ringRadius = outerRadius - 2.5f * density
            drawCircle(
                color = neonColor.copy(alpha = rgbBloomAlpha.value * 0.50f),
                center = center,
                radius = ringRadius,
                style = strokeChassisRingBloom
            )
            drawCircle(
                color = neonColor.copy(alpha = if (isPressed) 1.0f else 0.70f),
                center = center,
                radius = ringRadius,
                style = strokeChassisRingCore
            )

            // =========================================================================
            // THREE CONCENTRIC TARGET RINGS (.rg style="--s:...;--i:...")
            // Outside-in progressive lighting curve:
            // opacity = clamp(0.14, (fillProgress - i * 0.30) * 12, 1.0)
            // =========================================================================
            val targetRadii = floatArrayOf(
                37f * density, // Ring 0 (Outer: diameter 74dp)
                28f * density, // Ring 1 (Middle: diameter 56dp)
                19f * density  // Ring 2 (Inner: diameter 38dp)
            )

            for (i in 0..2) {
                val rawOpacity = (fillProgress.value - i * 0.30f) * 12f
                val ringOpacity = rawOpacity.coerceIn(0.14f, 1.0f)
                val r = targetRadii[i]

                // Emissive bloom pass when ring is ignited (> 25% lit)
                if (ringOpacity > 0.25f) {
                    drawCircle(
                        color = neonColor.copy(alpha = (ringOpacity * 0.40f).coerceAtMost(0.40f)),
                        center = center,
                        radius = r,
                        style = strokeTargetRingBloom
                    )
                }

                // Core crisp target ring filament
                drawCircle(
                    color = neonColor.copy(alpha = ringOpacity),
                    center = center,
                    radius = r,
                    style = strokeTargetRingCore
                )
            }

            // Top crescent specular highlight arc (.lx-lens)
            val lensInset = 1f * density
            drawArc(
                brush = Brush.verticalGradient(
                    colors = if (isPressed) lensColorsPressed else lensColorsNormal,
                    startY = 0f,
                    endY = h * 0.38f
                ),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(lensInset, lensInset),
                size = Size(w - lensInset * 2f, h - lensInset * 2f),
                style = strokeLensArc
            )

            // Bottom-right specular sheen oval
            drawCircle(
                brush = Brush.radialGradient(
                    colors = specularSheenColors,
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
                        colors = windowLipShadowColors,
                        startY = 0f,
                        endY = 5f * density
                    ),
                    size = Size(winW, 5f * density)
                )

                // Perimeter circular vignette (.lx-window::after)
                drawRect(
                    brush = Brush.radialGradient(
                        colorStops = windowVignetteColors,
                        center = Offset(winW / 2f, winH / 2f),
                        radius = winW * 0.50f
                    )
                )
            }

            // Dynamically reactive glyph brightness: 45% at idle to 100% full neon bloom on full pull
            Text(
                text = displayLabel ?: (if (isLeft) com.sanket.tools.nexpad.category.ControlKey.LT.key else com.sanket.tools.nexpad.category.ControlKey.RT.key),
                color = neonColor,
                modifier = Modifier.graphicsLayer {
                    alpha = (0.45f + fillProgress.value * 0.55f).coerceIn(0.45f, 1.0f)
                },
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                letterSpacing = 0.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Static preview delegate for TargetTrigger with 0 touch overhead and 0 coroutines.
 */
@Composable
fun StaticTargetTrigger(
    key: String = com.sanket.tools.nexpad.category.ControlKey.LT.key,
    isRgbEnabled: Boolean = true,
    displayLabel: String? = null,
    modifier: Modifier = Modifier
) {
    TargetTrigger(
        key = key,
        isConnected = false,
        onVibrate = {},
        viewModel = null,
        isRgbEnabled = isRgbEnabled,
        modifier = modifier,
        displayLabel = displayLabel,
        isInteractive = false
    )
}
