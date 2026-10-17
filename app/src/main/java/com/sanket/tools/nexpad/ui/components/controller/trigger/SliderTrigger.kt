package com.sanket.tools.nexpad.ui.components.controller.trigger

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Native Jetpack Compose implementation of the NEXPAD Analog Slider Trigger (Trigger F — Slider).
 *
 * Designed for continuous analog input (full 0..255 resolution over NEXPAD binary protocol),
 * physical trigger clips that contact and drag the screen, and custom ergonomics:
 * - Vertical track with glowing active fill bar
 * - Sliding 46dp glass puck handle with dual-layer neon ring and central glowing LED dot
 * - Continuous analog updates: directly invokes [GamepadViewModel.updateTrigger] with exact [0f, 1f]
 * - Damped harmonic spring return: smoothly springs back to 0.0 on release, broadcasting real-time decay
 * - Freedom of sizing: custom height scaling via [heightScale] (controllable from HUD Inspector)
 * - Inversion switch: toggleable pull direction via [isFlipped] (default Top-to-Bottom, or Bottom-to-Top)
 * - Recessed optical window well with bold tactical typography
 * - Multi-pass emissive neon chassis bloom and glass specular crescent highlight
 */
@Composable
fun SliderTrigger(
    key: String,
    isConnected: Boolean = true,
    onVibrate: () -> Unit = {},
    viewModel: GamepadViewModel? = null,
    isRgbEnabled: Boolean = true,
    modifier: Modifier = Modifier,
    displayLabel: String? = null,
    heightScale: Float = 1.0f,
    isFlipped: Boolean = false,
    isInteractive: Boolean = true
) {
    val isLeft = key.uppercase() == NexpadKeys.LT
    // HTML source palette: Amber Gold (#FFB13F) for LT, Hot Coral (#FF5A7A) for RT
    val glowColor = if (isLeft) NeonPalette.AmberHighlight else NeonPalette.HotCoral

    val currentOnVibrate by rememberUpdatedState(onVibrate)
    val currentViewModel by rememberUpdatedState(viewModel)
    val coroutineScope = rememberCoroutineScope()

    // Base dimensions scaled 45% smaller for compact phone ergonomics (42dp x 94dp)
    val widthDp = 42.dp
    val effectiveHeightScale = heightScale.coerceIn(0.5f, 2.5f)
    val baseHeightDp = 94.dp
    val totalHeightDp = (baseHeightDp.value * effectiveHeightScale).dp

    val chassisShape = remember { RoundedCornerShape(21.dp) }
    val windowShape = remember { RoundedCornerShape(7.dp) }

    // Physical travel metrics in Dp (proportional 0.55x)
    val puckDiameterDp = 26.dp
    val puckRadiusDp = puckDiameterDp / 2f
    val windowHeightDp = 14.dp
    val windowWidthDp = 30.dp
    val edgeMarginDp = 7.dp
    val windowClearanceDp = 6.dp

    // Available travel path for the puck center
    val topRestCenterDp = 9.dp + puckRadiusDp
    val bottomRestCenterDp = totalHeightDp - edgeMarginDp - windowHeightDp - windowClearanceDp - puckRadiusDp
    val travelSpanDp = (bottomRestCenterDp - topRestCenterDp).coerceAtLeast(8.dp)

    // Current pull value [0f, 1f]
    var isDragging by remember { mutableStateOf(false) }
    val fillAnim = remember { Animatable(0f) }
    val isPressedExternally = com.sanket.tools.nexpad.ui.components.controller.isControlPressed(key)

    LaunchedEffect(isPressedExternally) {
        if (!isDragging) {
            if (isPressedExternally) {
                fillAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f)
                )
            } else {
                fillAnim.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
        }
    }

    val rgbBloomAlpha = animateFloatAsState(
        targetValue = if (isDragging || isPressedExternally || fillAnim.value > 0.05f) 1.0f else 0.45f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "slider_rgb_bloom"
    )

    val baseDomeGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                NeonPalette.MetalBase,
                NeonPalette.MetalShadow,
                NeonPalette.PureBlack
            ),
            center = Offset(0.50f, 0.55f),
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

    val puckDomeGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                NeonPalette.MetalHighlight,
                NeonPalette.MetalGrit,
                NeonPalette.MetalDeep
            ),
            center = Offset(0.50f, 0.38f),
            radius = 50f
        )
    }

    val windowBackgroundBrush = remember {
        Brush.radialGradient(
            colors = listOf(
                NeonPalette.MetalDeep,
                NeonPalette.MetalDark
            ),
            center = Offset(0.50f, 0.60f),
            radius = 50f
        )
    }

    val windowBorderBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color.Black.copy(alpha = 0.9f),
                Color.White.copy(alpha = 0.08f)
            )
        )
    }

    val windowVignetteColors = remember {
        listOf(Color.Transparent, Color.Black.copy(alpha = 0.80f))
    }

    val puckLensColors = remember {
        listOf(
            Color.White.copy(alpha = 0.22f),
            Color.White.copy(alpha = 0.05f),
            Color.Transparent
        )
    }

    val chassisLensColors = remember {
        listOf(
            Color.White.copy(alpha = 0.16f),
            Color.White.copy(alpha = 0.04f),
            Color.Transparent
        )
    }

    val density = LocalDensity.current
    val strokePuckLens = remember(density) { Stroke(width = 1f * density.density) }
    val strokePuckRingBloom = remember(density) { Stroke(width = 2.2f * density.density) }
    val strokePuckRingCore = remember(density) { Stroke(width = 1.2f * density.density) }
    val strokePuckDotRim = remember(density) { Stroke(width = 0.8f * density.density) }
    val strokeChassisRingBloom = remember(density) { Stroke(width = 2.5f * density.density) }
    val strokeChassisRingCore = remember(density) { Stroke(width = 1.2f * density.density) }

    val channelCapsuleColors = remember(glowColor) {
        arrayListOf(Color.Transparent, Color.Transparent, Color.Transparent)
    }
    val trackGuideColors = remember(glowColor) {
        arrayListOf(Color.Transparent, Color.Transparent, Color.Transparent, Color.Transparent)
    }
    val beaconFlareColors = remember(glowColor) {
        arrayListOf(Color.Transparent, Color.Transparent, Color.Transparent)
    }
    val wingSpikeColors = remember(glowColor) {
        arrayListOf(Color.Transparent, Color.Transparent, Color.Transparent)
    }
    val puckBloomColors = remember(glowColor) {
        arrayListOf(Color.Transparent, Color.Transparent, Color.Transparent)
    }

    Box(
        modifier = modifier
            .size(widthDp, totalHeightDp)
            .graphicsLayer { /* Root RenderNode isolation */ }
            // Outer dynamic RGB aura
            .drawBehind {
                if (isRgbEnabled) {
                    val padX = 10f * density.density
                    val padY = 8f * density.density
                    val curPull = fillAnim.value

                    // 1. Vertical Track Channel Capsule Aura
                    channelCapsuleColors[0] = glowColor.copy(alpha = rgbBloomAlpha.value * (if (isDragging) 0.50f else 0.30f))
                    channelCapsuleColors[1] = glowColor.copy(alpha = rgbBloomAlpha.value * 0.12f)
                    channelCapsuleColors[2] = Color.Transparent
                    drawRoundRect(
                        brush = Brush.radialGradient(
                            colors = channelCapsuleColors,
                            center = center,
                            radius = size.height * 0.55f
                        ),
                        topLeft = Offset(-padX, -padY),
                        size = Size(size.width + padX * 2f, size.height + padY * 2f),
                        cornerRadius = CornerRadius(21f * density.density, 21f * density.density)
                    )

                    // 2. Linear Laser Track Guide Beam
                    val topRestPx = topRestCenterDp.toPx()
                    val travelSpanPx = travelSpanDp.toPx()
                    val bottomRestPx = topRestPx + travelSpanPx
                    val puckY = if (isFlipped) bottomRestPx - curPull * travelSpanPx else topRestPx + curPull * travelSpanPx

                    trackGuideColors[0] = Color.Transparent
                    trackGuideColors[1] = glowColor.copy(alpha = rgbBloomAlpha.value * 0.40f)
                    trackGuideColors[2] = glowColor.copy(alpha = rgbBloomAlpha.value * 0.40f)
                    trackGuideColors[3] = Color.Transparent
                    drawLine(
                        brush = Brush.verticalGradient(
                            colors = trackGuideColors,
                            startY = topRestPx - 10f * density.density,
                            endY = bottomRestPx + 10f * density.density
                        ),
                        start = Offset(center.x, topRestPx - 10f * density.density),
                        end = Offset(center.x, bottomRestPx + 10f * density.density),
                        strokeWidth = 2f * density.density
                    )

                    // 3. Travelling Puck Beacon Flare tracking puck coordinates
                    val beaconRadius = (if (isDragging) 18f else 12f) * density.density
                    val beaconAlpha = rgbBloomAlpha.value * (if (isDragging) 0.90f else 0.55f)

                    beaconFlareColors[0] = Color.White.copy(alpha = beaconAlpha)
                    beaconFlareColors[1] = glowColor.copy(alpha = beaconAlpha * 0.85f)
                    beaconFlareColors[2] = Color.Transparent
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = beaconFlareColors,
                            center = Offset(center.x, puckY),
                            radius = beaconRadius
                        ),
                        center = Offset(center.x, puckY),
                        radius = beaconRadius
                    )

                    // Lateral flare wing spikes
                    val wingLen = (if (isDragging) 14f else 8f) * density.density
                    wingSpikeColors[0] = Color.Transparent
                    wingSpikeColors[1] = glowColor.copy(alpha = beaconAlpha)
                    wingSpikeColors[2] = Color.Transparent
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = wingSpikeColors,
                            startX = center.x - wingLen,
                            endX = center.x + wingLen
                        ),
                        start = Offset(center.x - wingLen, puckY),
                        end = Offset(center.x + wingLen, puckY),
                        strokeWidth = (if (isDragging) 3f else 1.5f) * density.density
                    )
                }
            }
            // RGB-coordinated shadow
            .shadow(
                elevation = if (isDragging) 2.dp else 6.dp,
                shape = chassisShape,
                ambientColor = if (isRgbEnabled) glowColor else Color.Black,
                spotColor = if (isRgbEnabled) glowColor else Color.Black
            )
            .clip(chassisShape)
            .background(baseDomeGradient)
            .border(
                width = 1.dp,
                brush = chassisBorderBrush,
                shape = chassisShape
            )
            .then(
                if (isInteractive) {
                    Modifier.pointerInput(key, isFlipped, travelSpanDp, topRestCenterDp, bottomRestCenterDp) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            isDragging = true
                            currentOnVibrate()

                            val topPx = with(density) { topRestCenterDp.toPx() }
                            val spanPx = with(density) { travelSpanDp.toPx() }

                            fun calculateFill(yPos: Float): Float {
                                val normalized = ((yPos - topPx) / spanPx).coerceIn(0f, 1f)
                                return if (isFlipped) (1.0f - normalized) else normalized
                            }

                            val initialFill = calculateFill(down.position.y)
                            coroutineScope.launch {
                                fillAnim.snapTo(initialFill)
                                currentViewModel?.updateTrigger(key, initialFill)
                            }

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id }
                                if (change == null || !change.pressed) break

                                change.consume()
                                val newFill = calculateFill(change.position.y)
                                coroutineScope.launch {
                                    fillAnim.snapTo(newFill)
                                    currentViewModel?.updateTrigger(key, newFill)
                                }
                            }

                            // On release: Damped harmonic spring back to idle (0f)
                            isDragging = false
                            coroutineScope.launch {
                                fillAnim.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                ) {
                                    currentViewModel?.updateTrigger(key, value)
                                }
                                currentViewModel?.updateTrigger(key, 0f)
                            }
                        }
                    }
                } else Modifier
            )
    ) {
        // Layer 1: Track and Glowing Active Fill Bar
        Canvas(modifier = Modifier.fillMaxSize()) {
            val currentFill = fillAnim.value
            val w = size.width
            val centerX = w / 2f
            val topPx = topRestCenterDp.toPx()
            val bottomPx = bottomRestCenterDp.toPx()
            val spanPx = travelSpanDp.toPx()

            val trackWidthPx = 5f * density.density
            val trackRadiusPx = 2.5f * density.density

            // Outer recessed track groove
            drawRoundRect(
                color = Color.White.copy(alpha = 0.08f),
                topLeft = Offset(centerX - trackWidthPx / 2f, topPx - trackRadiusPx),
                size = Size(trackWidthPx, spanPx + trackRadiusPx * 2f),
                cornerRadius = CornerRadius(trackRadiusPx, trackRadiusPx)
            )

            // Inner dark track depth
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.70f),
                topLeft = Offset(centerX - trackWidthPx / 2f + 1f, topPx - trackRadiusPx + 1f),
                size = Size(trackWidthPx - 2f, spanPx + trackRadiusPx * 2f - 2f),
                cornerRadius = CornerRadius(trackRadiusPx - 1f, trackRadiusPx - 1f)
            )

            // Current puck center Y
            val puckCenterY = if (isFlipped) {
                bottomPx - currentFill * spanPx
            } else {
                topPx + currentFill * spanPx
            }

            // Glowing Active Fill Bar
            if (currentFill > 0.005f) {
                val fillTopY = if (isFlipped) puckCenterY else topPx
                val fillHeight = if (isFlipped) (bottomPx - puckCenterY) else (puckCenterY - topPx)

                // Emissive halo bloom
                drawRoundRect(
                    color = glowColor.copy(alpha = 0.45f),
                    topLeft = Offset(centerX - trackWidthPx / 2f - 2f * density.density, fillTopY),
                    size = Size(trackWidthPx + 4f * density.density, fillHeight),
                    cornerRadius = CornerRadius(trackRadiusPx, trackRadiusPx)
                )

                // Crisp neon core
                drawRoundRect(
                    color = glowColor,
                    topLeft = Offset(centerX - trackWidthPx / 2f, fillTopY),
                    size = Size(trackWidthPx, fillHeight),
                    cornerRadius = CornerRadius(trackRadiusPx, trackRadiusPx)
                )
            }
        }

        // Layer 2: Sliding Puck Handle
        Box(
            modifier = Modifier
                .size(puckDiameterDp)
                .offset {
                    val currentFill = fillAnim.value
                    val puckCenterDp = if (isFlipped) {
                        bottomRestCenterDp - travelSpanDp * currentFill
                    } else {
                        topRestCenterDp + travelSpanDp * currentFill
                    }
                    IntOffset(
                        x = ((widthDp - puckDiameterDp) / 2f).roundToPx(),
                        y = (puckCenterDp - puckRadiusDp).roundToPx()
                    )
                }
                .graphicsLayer { /* Puck RenderNode isolation */ }
                // Active puck bloom
                .drawBehind {
                    if (isRgbEnabled) {
                        puckBloomColors[0] = glowColor.copy(alpha = rgbBloomAlpha.value * 0.60f)
                        puckBloomColors[1] = glowColor.copy(alpha = rgbBloomAlpha.value * 0.25f)
                        puckBloomColors[2] = Color.Transparent
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = puckBloomColors,
                                center = center,
                                radius = size.minDimension * 0.85f
                            ),
                            radius = size.minDimension * 0.85f
                        )
                    }
                }
                .shadow(
                    elevation = if (isDragging) 6.dp else 3.dp,
                    shape = CircleShape,
                    ambientColor = if (isRgbEnabled) glowColor else Color.Black,
                    spotColor = if (isRgbEnabled) glowColor else Color.Black
                )
                .clip(CircleShape)
                .background(puckDomeGradient)
                .border(1.dp, Color.Black.copy(alpha = 0.6f), CircleShape)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val currentFill = fillAnim.value
                val r = size.minDimension / 2f
                val center = Offset(size.width / 2f, size.height / 2f)

                // Specular lens highlight at top
                drawArc(
                    brush = Brush.verticalGradient(
                        colors = puckLensColors
                    ),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(1.5f * density.density, 1.5f * density.density),
                    size = Size(size.width - 3f * density.density, size.height - 3f * density.density),
                    style = strokePuckLens
                )

                // Puck Neon Ring (.puck-ring)
                val ringRadius = r - 2.5f * density.density
                // Outer bloom
                drawCircle(
                    color = glowColor.copy(alpha = 0.40f + currentFill * 0.45f),
                    radius = ringRadius,
                    center = center,
                    style = strokePuckRingBloom
                )
                // Crisp core
                drawCircle(
                    color = glowColor.copy(alpha = 0.85f),
                    radius = ringRadius,
                    center = center,
                    style = strokePuckRingCore
                )

                // Central glowing LED dot (.puck-dot)
                val dotRadius = 2.5f * density.density
                // Bloom
                drawCircle(
                    color = glowColor.copy(alpha = 0.50f + currentFill * 0.50f),
                    radius = dotRadius + 2f * density.density,
                    center = center
                )
                // Core
                drawCircle(
                    color = Color.White.copy(alpha = 0.90f),
                    radius = dotRadius * 0.65f,
                    center = center
                )
                drawCircle(
                    color = glowColor,
                    radius = dotRadius,
                    center = center,
                    style = strokePuckDotRim
                )
            }
        }

        // Layer 3: Recessed Optical Window (.lx-window)
        val windowAlignment = if (isFlipped) Alignment.TopCenter else Alignment.BottomCenter
        val windowOffset = if (isFlipped) {
            Modifier.offset(y = edgeMarginDp)
        } else {
            Modifier.offset(y = -edgeMarginDp)
        }

        Box(
            modifier = Modifier
                .align(windowAlignment)
                .then(windowOffset)
                .size(windowWidthDp, windowHeightDp)
                .clip(windowShape)
                .background(windowBackgroundBrush)
                .border(
                    width = 1.dp,
                    brush = windowBorderBrush,
                    shape = windowShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Optical window edge vignette
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = windowVignetteColors,
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = size.width * 0.55f
                    ),
                    size = size,
                    cornerRadius = CornerRadius(7f * density.density, 7f * density.density)
                )
            }

            val label = displayLabel ?: key
            Text(
                text = label,
                color = glowColor,
                modifier = Modifier.graphicsLayer {
                    alpha = 0.75f + fillAnim.value * 0.25f
                },
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }

        // Layer 4: Specular Glass Lens & Outer Rim (.lx-lens & .lx-ring)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val rPx = 21f * density.density

            // Top specular crescent highlight
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = chassisLensColors,
                    startY = 0f,
                    endY = h * 0.28f
                ),
                topLeft = Offset(1.5f * density.density, 1.5f * density.density),
                size = Size(w - 3f * density.density, h * 0.28f),
                cornerRadius = CornerRadius(rPx, rPx)
            )

            // Outer chassis neon ring (.lx-ring)
            val ringInset = 2f * density.density
            val ringCorner = rPx - ringInset
            // Bloom
            drawRoundRect(
                color = glowColor.copy(alpha = rgbBloomAlpha.value * 0.50f),
                topLeft = Offset(ringInset - 1f, ringInset - 1f),
                size = Size(w - (ringInset - 1f) * 2f, h - (ringInset - 1f) * 2f),
                cornerRadius = CornerRadius(ringCorner, ringCorner),
                style = strokeChassisRingBloom
            )
            // Core
            drawRoundRect(
                color = glowColor.copy(alpha = if (isDragging) 0.90f else 0.55f),
                topLeft = Offset(ringInset, ringInset),
                size = Size(w - ringInset * 2f, h - ringInset * 2f),
                cornerRadius = CornerRadius(ringCorner, ringCorner),
                style = strokeChassisRingCore
            )
        }
    }
}

/**
 * Static preview delegate for SliderTrigger with 0 touch overhead and 0 coroutines.
 */
@Composable
fun StaticSliderTrigger(
    key: String = "LT",
    isRgbEnabled: Boolean = true,
    displayLabel: String? = null,
    modifier: Modifier = Modifier,
    heightScale: Float = 1.0f,
    isFlipped: Boolean = false
) {
    SliderTrigger(
        key = key,
        isConnected = false,
        onVibrate = {},
        viewModel = null,
        isRgbEnabled = isRgbEnabled,
        modifier = modifier,
        displayLabel = displayLabel,
        heightScale = heightScale,
        isFlipped = isFlipped,
        isInteractive = false
    )
}
