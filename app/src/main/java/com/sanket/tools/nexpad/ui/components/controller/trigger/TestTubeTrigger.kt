package com.sanket.tools.nexpad.ui.components.controller.trigger

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.model.NexpadKeys
import com.sanket.tools.nexpad.ui.theme.NeonPalette
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Native Jetpack Compose implementation of the NEXPAD Test Tube Trigger (Trigger H — Test Tube).
 *
 * Operates with standard tactile trigger ergonomics matching console triggers:
 * - Immediate responsive press feedback via [detectTapGestures]
 * - Direct digital wire dispatch via [GamepadViewModel.updateButton]
 * - Damped harmonic spring kinematics: dynamic scale compression, vertical displacement plunge,
 *   and smooth liquid fill surge to 100% on press
 * - Cylindrical glass test tube capsule contour (48dp x 98dp, r=24dp)
 * - Rocking meniscus crest at liquid surface with harmonic sway oscillation
 * - Rising floating bubbles animated inside the active liquid column while pressed
 * - Volumetric measurement graduation scale markings running along the right side
 * - Specular vertical glass gloss strip running along the left side
 * - Recessed optical window near top displaying tactical trigger label (LT / RT)
 */
@Composable
fun TestTubeTrigger(
    key: String,
    isConnected: Boolean = true,
    onVibrate: () -> Unit = {},
    viewModel: GamepadViewModel? = null,
    isRgbEnabled: Boolean = true,
    modifier: Modifier = Modifier,
    displayLabel: String? = null,
    isInteractive: Boolean = true
) {
    var isPressed by remember { mutableStateOf(false) }
    val isLeft = key.uppercase() == NexpadKeys.LT
    // HTML source palette: Amber Orange (#FF9F43) for LT, Violet Purple (#BD5CFF) for RT
    val glowColor = if (isLeft) NeonPalette.BrightOrange else NeonPalette.VioletNeon

    val currentOnVibrate by rememberUpdatedState(onVibrate)
    val currentViewModel by rememberUpdatedState(viewModel)

    val widthDp = 48.dp
    val heightDp = 98.dp
    val chassisShape = remember { RoundedCornerShape(24.dp) }
    val chamberShape = remember { RoundedCornerShape(19.dp) }
    val windowShape = remember { RoundedCornerShape(10.dp) }

    // Physical Spring Kinematics — standard console trigger spec
    val scaleAnim = animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "testtube_scale"
    )
    val rgbBloomAlpha = animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0.45f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "testtube_bloom"
    )
    val fillProgress = animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 380f),
        label = "testtube_fill"
    )

    // Meniscus rocking cycle & bubble rising drive
    val infiniteTransition = rememberInfiniteTransition(label = "tube_effects")
    val swayProgress = infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "meniscus_sway"
    )
    val bubbleTime = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bubble_time"
    )

    val baseDomeGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                NeonPalette.MetalBase,
                NeonPalette.MetalShadow,
                NeonPalette.PureBlack
            ),
            center = Offset(0.50f, 0.45f),
            radius = 200f
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
            radius = 200f
        )
    }

    val chassisBorderBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color.Black.copy(alpha = 0.50f),
                Color.Black.copy(alpha = 0.85f)
            )
        )
    }

    val windowBackgroundBrush = remember {
        Brush.radialGradient(
            colors = listOf(
                NeonPalette.MetalDeep,
                NeonPalette.MetalDark
            ),
            center = Offset(0.50f, 0.60f),
            radius = 40f
        )
    }

    val windowBorderBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color.Black.copy(alpha = 0.9f),
                Color.White.copy(alpha = 0.10f)
            )
        )
    }

    val liquidBodyColors = remember(glowColor) {
        listOf(
            glowColor.copy(alpha = 0.55f),
            glowColor.copy(alpha = 0.85f)
        )
    }

    val liquidCoreColors = remember(glowColor) {
        listOf(
            glowColor.copy(alpha = 0.35f),
            glowColor.copy(alpha = 0.70f)
        )
    }

    val glossStripColors = remember {
        listOf(
            Color.White.copy(alpha = 0.35f),
            Color.White.copy(alpha = 0.08f),
            Color.Transparent
        )
    }

    val lensColors = remember {
        listOf(
            Color.White.copy(alpha = 0.16f),
            Color.White.copy(alpha = 0.04f),
            Color.Transparent
        )
    }

    val tubeCapsuleColors = remember(glowColor) {
        arrayListOf(Color.Transparent, Color.Transparent, Color.Transparent)
    }
    val reservoirPoolColors = remember(glowColor) {
        arrayListOf(Color.Transparent, Color.Transparent, Color.Transparent)
    }

    val bubbleOffsetsX = remember { floatArrayOf(0.20f, 0.42f, 0.65f, 0.32f, 0.78f) }
    val bubbleSpeeds = remember { floatArrayOf(1.0f, 1.4f, 0.9f, 1.2f, 1.5f) }
    val bubbleDelays = remember { floatArrayOf(0.0f, 0.25f, 0.5f, 0.75f, 0.1f) }

    val density = LocalDensity.current.density
    val strokeBubblePip = remember(density) { Stroke(width = 1f * density) }
    val strokeChassisRingBloom = remember(density) { Stroke(width = 2.6f * density) }
    val strokeChassisRingCore = remember(density) { Stroke(width = 1.2f * density) }

    Box(
        modifier = modifier
            .size(widthDp, heightDp)
            .graphicsLayer { /* Root RenderNode isolation */ }
            // Outer dynamic RGB aura
            .drawBehind {
                if (isRgbEnabled) {
                    val padX = 10f * density
                    val padY = 8f * density

                    // 1. Elongated Test Tube Capsule Aura
                    tubeCapsuleColors[0] = glowColor.copy(alpha = rgbBloomAlpha.value * (if (isPressed) 0.35f else 0.15f))
                    tubeCapsuleColors[1] = glowColor.copy(alpha = rgbBloomAlpha.value * (if (isPressed) 0.70f else 0.40f))
                    tubeCapsuleColors[2] = Color.Transparent
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = tubeCapsuleColors,
                            startY = -padY,
                            endY = size.height + padY
                        ),
                        topLeft = Offset(-padX, -padY),
                        size = Size(size.width + padX * 2f, size.height + padY * 2f),
                        cornerRadius = CornerRadius(24f * density, 24f * density)
                    )

                    // 2. Rising Bioluminescent Liquid Reservoir Pool (surges with fillProgress)
                    if (fillProgress.value > 0.05f) {
                        val poolH = (size.height * 0.70f) * fillProgress.value
                        val poolTop = size.height - poolH
                        reservoirPoolColors[0] = glowColor.copy(alpha = rgbBloomAlpha.value * fillProgress.value * 0.85f)
                        reservoirPoolColors[1] = glowColor.copy(alpha = rgbBloomAlpha.value * fillProgress.value * 0.30f)
                        reservoirPoolColors[2] = Color.Transparent
                        drawRoundRect(
                            brush = Brush.radialGradient(
                                colors = reservoirPoolColors,
                                center = Offset(center.x, size.height - poolH * 0.5f),
                                radius = size.width * 0.80f
                            ),
                            topLeft = Offset(-padX - 4f * density, poolTop),
                            size = Size(size.width + (padX + 4f * density) * 2f, poolH + padY),
                            cornerRadius = CornerRadius(20f * density, 20f * density)
                        )
                    }

                    // 3. Effervescent Rising Micro-Bubble Glow Pips
                    if (isPressed) {
                        val bubblePhase = bubbleTime.value
                        val bY1 = size.height * (1f - ((bubblePhase * 0.9f) % 1f))
                        val bX1 = center.x - 14f * density + (swayProgress.value * 3f * density)
                        val bY2 = size.height * (1f - (((bubblePhase + 0.4f) * 0.8f) % 1f))
                        val bX2 = center.x + 14f * density - (swayProgress.value * 2f * density)

                        drawCircle(
                            color = Color.White.copy(alpha = rgbBloomAlpha.value * 0.85f),
                            radius = 2.5f * density,
                            center = Offset(bX1, bY1)
                        )
                        drawCircle(
                            color = glowColor.copy(alpha = rgbBloomAlpha.value * 0.70f),
                            radius = 2f * density,
                            center = Offset(bX2, bY2)
                        )
                    }
                }
            }
            // RGB-coordinated shadow placed before motion scaling to prevent CPU shadow tessellation
            .shadow(
                elevation = if (isPressed) 2.dp else 6.dp,
                shape = chassisShape,
                ambientColor = if (isRgbEnabled) glowColor else Color.Black,
                spotColor = if (isRgbEnabled) glowColor else Color.Black
            )
            .graphicsLayer {
                scaleX = scaleAnim.value
                scaleY = scaleAnim.value
            }
            .clip(chassisShape)
            .background(if (isPressed) pressedDomeGradient else baseDomeGradient)
            .border(
                width = 1.dp,
                brush = chassisBorderBrush,
                shape = chassisShape
            )
            .then(
                if (isInteractive) {
                    Modifier.pointerInput(key) {
                        detectTapGestures(
                            onPress = {
                                currentOnVibrate()
                                isPressed = true
                                currentViewModel?.updateButton(key, true)
                                tryAwaitRelease()
                                isPressed = false
                                currentViewModel?.updateButton(key, false)
                            }
                        )
                    }
                } else Modifier
            )
    ) {
        // Layer 1: Recessed Glass Chamber & Dynamic Rising Liquid
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(5.dp)
                .clip(chamberShape)
                .background(NeonPalette.CarbonAbyss)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val currentFill = fillProgress.value
                val w = size.width
                val h = size.height

                // Liquid fill height from bottom
                val liquidHeight = h * currentFill

                if (liquidHeight > 1f) {
                    val liquidTopY = h - liquidHeight

                    // Liquid volume glowing body
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = liquidBodyColors,
                            startY = liquidTopY,
                            endY = h
                        ),
                        topLeft = Offset(0f, liquidTopY),
                        size = Size(w, liquidHeight)
                    )

                    // Liquid emissive bloom core
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = liquidCoreColors,
                            startY = liquidTopY,
                            endY = h
                        ),
                        topLeft = Offset(2f * density, liquidTopY),
                        size = Size(w - 4f * density, liquidHeight)
                    )

                    // Swaying Meniscus Crest (.tt-liq::before)
                    val swayOffsetPx = if (isPressed) swayProgress.value * 3f * density else 0f
                    val meniscusHeight = 6f * density
                    drawOval(
                        color = glowColor,
                        topLeft = Offset(-w * 0.15f + swayOffsetPx, liquidTopY - meniscusHeight / 2f),
                        size = Size(w * 1.3f, meniscusHeight)
                    )
                    // Meniscus bright specular reflection glint
                    drawOval(
                        color = Color.White.copy(alpha = 0.70f),
                        topLeft = Offset(w * 0.20f + swayOffsetPx, liquidTopY - meniscusHeight * 0.35f),
                        size = Size(w * 0.6f, meniscusHeight * 0.6f)
                    )

                    // Rising floating bubbles (.bubs)
                    if (isPressed || currentFill > 0.1f) {
                        val bTime = bubbleTime.value

                        for (i in bubbleOffsetsX.indices) {
                            val progress = ((bTime * bubbleSpeeds[i] + bubbleDelays[i]) % 1.0f)
                            val bY = h - (progress * liquidHeight)
                            val wobble = sin(progress * 6.28f * 2f) * 1.8f
                            val bX = w * bubbleOffsetsX[i] + wobble
                            val bRadius = (1.5f + (i % 2) * 0.8f) * density

                            if (bY in (liquidTopY + bRadius)..h) {
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.75f),
                                    radius = bRadius,
                                    center = Offset(bX, bY),
                                    style = strokeBubblePip
                                )
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.40f),
                                    radius = bRadius * 0.6f,
                                    center = Offset(bX, bY)
                                )
                            }
                        }
                    }
                }

                // Volumetric Measurement Scale Ticks (.tt-scale) on right edge
                val numTicks = 6
                val scaleStartX = w - 8f * density
                val scaleEndX = w - 3f * density
                for (i in 0 until numTicks) {
                    val tickY = 16f * density + i * ((h - 32f * density) / (numTicks - 1))
                    drawLine(
                        color = Color.White.copy(alpha = 0.35f),
                        start = Offset(scaleStartX, tickY),
                        end = Offset(scaleEndX, tickY),
                        strokeWidth = 1.2f * density
                    )
                }

                // Vertical Specular Glass Gloss Strip (.tt-gloss) on left edge
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = glossStripColors
                    ),
                    topLeft = Offset(3f * density, 8f * density),
                    size = Size(3f * density, h - 16f * density),
                    cornerRadius = CornerRadius(1.5f * density, 1.5f * density)
                )
            }
        }

        // Layer 2: Recessed Optical Window centered (.lx-window)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (12).dp)
                .size(36.dp, 20.dp)
                .clip(windowShape)
                .background(windowBackgroundBrush)
                .border(
                    width = 1.dp,
                    brush = windowBorderBrush,
                    shape = windowShape
                ),
            contentAlignment = Alignment.Center
        ) {
            val label = displayLabel ?: key
            Text(
                text = label,
                color = glowColor,
                modifier = Modifier.graphicsLayer {
                    alpha = 0.85f + fillProgress.value * 0.15f
                },
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }

        // Layer 3: Specular Glass Lens & Outer Chassis Ring (.lx-lens & .lx-ring)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val rPx = 24f * density

            // Top specular crescent highlight
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = lensColors,
                    startY = 0f,
                    endY = h * 0.30f
                ),
                topLeft = Offset(1.5f * density, 1.5f * density),
                size = Size(w - 3f * density, h * 0.30f),
                cornerRadius = CornerRadius(rPx, rPx)
            )

            // Outer chassis neon ring (.lx-ring)
            val ringInset = 2f * density
            val ringCorner = rPx - ringInset
            drawRoundRect(
                color = glowColor.copy(alpha = rgbBloomAlpha.value * 0.55f),
                topLeft = Offset(ringInset - 1f, ringInset - 1f),
                size = Size(w - (ringInset - 1f) * 2f, h - (ringInset - 1f) * 2f),
                cornerRadius = CornerRadius(ringCorner, ringCorner),
                style = strokeChassisRingBloom
            )
            drawRoundRect(
                color = glowColor.copy(alpha = if (isPressed) 0.95f else 0.60f),
                topLeft = Offset(ringInset, ringInset),
                size = Size(w - ringInset * 2f, h - ringInset * 2f),
                cornerRadius = CornerRadius(ringCorner, ringCorner),
                style = strokeChassisRingCore
            )
        }
    }
}

/**
 * Static preview delegate for TestTubeTrigger with 0 touch overhead and 0 coroutines.
 */
@Composable
fun StaticTestTubeTrigger(
    key: String = "LT",
    isRgbEnabled: Boolean = true,
    displayLabel: String? = null,
    modifier: Modifier = Modifier
) {
    TestTubeTrigger(
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
