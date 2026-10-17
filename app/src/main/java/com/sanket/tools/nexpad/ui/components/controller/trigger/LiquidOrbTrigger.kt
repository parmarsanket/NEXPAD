package com.sanket.tools.nexpad.ui.components.controller.trigger

import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
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

/**
 * Native Jetpack Compose implementation of the NEXPAD Liquid Orb Trigger (Trigger B — Liquid Orb).
 *
 * Translates the fluid-filled glass sphere HTML blueprint into high-performance Compose:
 * - Sealed glass orb housing (CircleShape) scaled to compact 92dp x 92dp mobile standard
 * - Recessed spherical fluid chamber (.liq-wrap) with deep dark liquid well gradient
 * - Progressive rising fluid level (8% ambient baseline -> 100% full on full press)
 * - Dynamic swaying fluid meniscus crest (.liq::before) that rocks back and forth while held
 * - Recessed optical window (.lx-window) elevated at 34% top position above fluid reservoir
 * - Pure optical layout: strictly omits percentage text readout per mobile design directive
 * - Multi-pass emissive neon lens rings, specular glass reflections, and spring kinematics
 */
@Composable
fun LiquidOrbTrigger(
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
        label = "liquid_orb_scale"
    )
    val rgbBloomAlpha = animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0.45f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "liquid_orb_rgb_bloom"
    )
    // Rising liquid fill level: starts at 14% ambient fluid, capped at 80% (0.80f) on full pull
    // so the fluid meniscus remains clearly visible rocking beneath the top dome!
    val fillProgress = animateFloatAsState(
        targetValue = if (isPressed) 0.80f else 0.14f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 380f),
        label = "liquid_orb_fill"
    )

    // Fluid Meniscus Sway Animation (simulating fluid surface rocking back and forth)
    val infiniteTransition = rememberInfiniteTransition(label = "liquid_sway")
    val swayOffsetX = infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sway_offset_x"
    )
    val swayScaleY = infiniteTransition.animateFloat(
        initialValue = 0.70f,
        targetValue = 1.30f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sway_scale_y"
    )

    // LT = Turquoise / Teal (#3FD2C4), RT = Coral Pink (#FF5376) harmonic neon contrast pair
    val neonColor = if (isLeft) NeonPalette.AquaMint else NeonPalette.HotCoral

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

    // Recessed optical window gradient (#050506 -> #121314)
    val windowGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                NeonPalette.MetalDeep,
                NeonPalette.MetalDark
            ),
            center = Offset(0.50f, 0.60f),
            radius = 100f
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
    val chamberCavityColors = remember {
        listOf(NeonPalette.CarbonDark, NeonPalette.MetalDeep)
    }
    val fluidLayer1Colors = remember(neonColor) {
        listOf(
            neonColor.copy(alpha = 0.75f),
            neonColor.copy(alpha = 0.35f),
            Color.Transparent
        )
    }
    val fluidLayer2Colors = remember(neonColor) {
        listOf(
            neonColor.copy(alpha = 0.50f),
            Color.Transparent
        )
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
            0.32f to Color.Transparent,
            1.0f to Color.Black.copy(alpha = 0.82f)
        )
    }

    val currentOnVibrate by rememberUpdatedState(onVibrate)
    val currentViewModel by rememberUpdatedState(viewModel)

    val triggerShape = CircleShape
    val windowShape = RoundedCornerShape(13.dp)

    val density = LocalDensity.current.density
    val strokeCorona = remember(density) { Stroke(width = 1.5f * density) }
    val strokeRimOutline = remember(density) { Stroke(width = 1f * density) }
    val strokeRingBloom = remember(density) { Stroke(width = 4f * density) }
    val strokeRingCore = remember(density) { Stroke(width = 2f * density) }
    val strokeLensArc = remember(density) { Stroke(width = 1f * density) }

    val poolBloomColors = remember(neonColor) {
        arrayListOf(Color.Transparent, Color.Transparent, Color.Transparent)
    }
    val meniscusColors = remember(neonColor) {
        arrayListOf(Color.Transparent, Color.Transparent, Color.Transparent)
    }

    val chamberPath = remember { Path() }

    Box(
        modifier = modifier
            .size(92.dp, 92.dp)
            .graphicsLayer { /* Root RenderNode isolation */ }
            // Outer dynamic RGB aura
            .drawBehind {
                if (isRgbEnabled) {
                    val baseR = size.minDimension * 0.5f

                    // 1. Buoyant bottom-weighted fluid reservoir pool
                    val fluidH = size.height * fillProgress.value
                    val poolCenterY = size.height - fluidH * 0.45f
                    val dropletW = size.width * (0.80f + 0.15f * swayScaleY.value)
                    val dropletH = fluidH + 16f * density

                    poolBloomColors[0] = neonColor.copy(alpha = rgbBloomAlpha.value * (if (isPressed) 0.65f else 0.35f))
                    poolBloomColors[1] = neonColor.copy(alpha = rgbBloomAlpha.value * 0.18f)
                    poolBloomColors[2] = Color.Transparent
                    drawOval(
                        brush = Brush.radialGradient(
                            colors = poolBloomColors,
                            center = Offset(center.x + swayOffsetX.value, poolCenterY),
                            radius = size.minDimension * 0.65f
                        ),
                        topLeft = Offset(center.x - dropletW / 2f + swayOffsetX.value, poolCenterY - dropletH / 2f),
                        size = Size(dropletW, dropletH)
                    )

                    // 2. Liquid surface tension meniscus wave halo
                    val meniscusY = size.height - fluidH
                    val meniscusW = size.width * 0.70f
                    val meniscusH = 10f * density * swayScaleY.value

                    meniscusColors[0] = Color.White.copy(alpha = rgbBloomAlpha.value * (if (isPressed) 0.80f else 0.40f))
                    meniscusColors[1] = neonColor.copy(alpha = rgbBloomAlpha.value * 0.60f)
                    meniscusColors[2] = Color.Transparent
                    drawOval(
                        brush = Brush.radialGradient(
                            colors = meniscusColors,
                            center = Offset(center.x + swayOffsetX.value, meniscusY),
                            radius = meniscusW * 0.50f
                        ),
                        topLeft = Offset(center.x - meniscusW / 2f + swayOffsetX.value, meniscusY - meniscusH / 2f),
                        size = Size(meniscusW, meniscusH)
                    )

                    // 3. Ambient glass orb containment corona ring
                    drawCircle(
                        color = neonColor.copy(alpha = rgbBloomAlpha.value * (if (isPressed) 0.70f else 0.35f)),
                        radius = baseR + 3f * density,
                        center = center,
                        style = strokeCorona
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
        // LAYER STACK CANVAS: Fluid Chamber + Rising Liquid + Swaying Meniscus + Lens Arc
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Inset bottom shadow along base
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

            // Outer 1px rim outline
            drawCircle(
                color = Color.Black.copy(alpha = 0.50f),
                radius = (w / 2f) - 0.5f,
                style = strokeRimOutline
            )

            // Multi-pass emissive neon ring (.lx-ring)
            val ringInset = 3f * density
            val ringRadius = (w / 2f) - ringInset
            drawCircle(
                color = neonColor.copy(alpha = rgbBloomAlpha.value * 0.55f),
                radius = ringRadius,
                style = strokeRingBloom
            )
            drawCircle(
                color = neonColor.copy(alpha = if (isPressed) 1.0f else 0.70f),
                radius = ringRadius,
                style = strokeRingCore
            )

            // =========================================================================
            // RECESSED SPHERICAL LIQUID CHAMBER (.liq-wrap)
            // =========================================================================
            val chamberInset = 7f * density
            val chamberRadius = (w / 2f) - chamberInset
            val chamberCenter = Offset(w / 2f, h / 2f)

            // Circular clip path isolating liquid inside the chamber
            chamberPath.reset()
            chamberPath.addOval(
                Rect(
                    center = chamberCenter,
                    radius = chamberRadius
                )
            )

            clipPath(chamberPath) {
                // Dark internal cavity well
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = chamberCavityColors,
                        center = chamberCenter,
                        radius = chamberRadius
                    ),
                    radius = chamberRadius,
                    center = chamberCenter
                )

                // ---------------------------------------------------------------------
                // RISING FLUID BODY (.liq)
                // ---------------------------------------------------------------------
                val chamberDiameter = chamberRadius * 2f
                val liquidHeight = chamberDiameter * fillProgress.value
                val liquidTopY = (chamberCenter.y + chamberRadius) - liquidHeight
                val liquidBottomY = chamberCenter.y + chamberRadius

                // Layer 1: Ambient deep fluid bloom
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = fluidLayer1Colors,
                        startY = liquidTopY,
                        endY = liquidBottomY
                    ),
                    topLeft = Offset(chamberCenter.x - chamberRadius, liquidTopY),
                    size = Size(chamberDiameter, liquidHeight)
                )

                // Layer 2: Core luminous fluid radiance
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = fluidLayer2Colors,
                        startY = liquidTopY,
                        endY = liquidTopY + liquidHeight * 0.60f
                    ),
                    topLeft = Offset(chamberCenter.x - chamberRadius, liquidTopY),
                    size = Size(chamberDiameter, liquidHeight * 0.60f)
                )

                // ---------------------------------------------------------------------
                // GLOWING MENISCUS SURFACE CREST (.liq::before with sway animation)
                // ---------------------------------------------------------------------
                val activeSwayX = if (isPressed) swayOffsetX.value * density else 0f
                val activeSwayScaleY = if (isPressed) swayScaleY.value else 1.0f
                val meniscusWidth = chamberDiameter * 1.30f
                val meniscusHeight = 11f * density * activeSwayScaleY

                val meniscusLeft = (chamberCenter.x + activeSwayX) - (meniscusWidth / 2f)
                val meniscusTop = liquidTopY - (meniscusHeight / 2f)

                // Meniscus diffuse bloom halo
                drawOval(
                    color = neonColor.copy(alpha = 0.50f),
                    topLeft = Offset(meniscusLeft, meniscusTop - 3f * density),
                    size = Size(meniscusWidth, meniscusHeight + 6f * density)
                )

                // Meniscus core glowing surface oval
                drawOval(
                    color = neonColor.copy(alpha = 0.95f),
                    topLeft = Offset(meniscusLeft, meniscusTop),
                    size = Size(meniscusWidth, meniscusHeight)
                )

                // Meniscus specular wave crest glint
                val glintWidth = meniscusWidth * 0.50f
                val glintHeight = 3.5f * density * activeSwayScaleY
                drawOval(
                    color = Color.White.copy(alpha = 0.85f),
                    topLeft = Offset(chamberCenter.x + activeSwayX * 0.6f - (glintWidth / 2f), liquidTopY - (glintHeight / 2f)),
                    size = Size(glintWidth, glintHeight)
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
        // ELEVATED OPTICAL WINDOW (.lx-window top: 34%)
        // =========================================================================
        Box(
            modifier = Modifier
                .offset { IntOffset(0, (-13).dp.roundToPx()) }
                .size(48.dp, 26.dp)
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

                // Perimeter shadow vignette (.lx-window::after)
                drawRect(
                    brush = Brush.radialGradient(
                        colorStops = windowVignetteColors,
                        center = Offset(winW / 2f, winH / 2f),
                        radius = winW * 0.50f
                    )
                )
            }

            // High-contrast bold tactical label (LT / RT)
            Text(
                text = displayLabel ?: (if (isLeft) "LT" else "RT"),
                color = neonColor,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                letterSpacing = 1.5.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Static preview delegate for LiquidOrbTrigger with 0 touch overhead and 0 coroutines.
 */
@Composable
fun StaticLiquidOrbTrigger(
    key: String = "LT",
    isRgbEnabled: Boolean = true,
    displayLabel: String? = null,
    modifier: Modifier = Modifier
) {
    LiquidOrbTrigger(
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
