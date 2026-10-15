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
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.model.NexpadKeys
import com.sanket.tools.nexpad.ui.theme.NeonPalette
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel
import kotlin.math.roundToInt

/**
 * Native Jetpack Compose implementation of the NEXPAD Realistic Analog Trigger (LT / RT).
 *
 * Balanced Mobile Ergonomic Layout:
 * - Dimensions: 100dp x 92dp — the optimal sweet spot for mobile touch precision while preserving vertical space.
 * - Flipped upside-down contour: 16dp top corners (flush below bumpers) and 46dp semicircular bottom pedal hull.
 * - Progressive analog fluid meter (.lx-meter) surging vertically from bottom hull upward on press.
 * - Recessed optical window (.lx-window, 68dp x 30dp, r=15dp at top offset 12dp) with tactical typography.
 * - Multi-pass neon lens bloom (.lx-ring) and specular sheen highlights.
 * - Excludes percentage display per user directive.
 */
@Composable
fun RealisticTrigger(
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

    // Ergonomic mobile trigger contour: 16dp top corners, 46dp bottom hull
    val triggerShape = remember {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 46.dp, bottomEnd = 46.dp)
    }
    val windowShape = remember {
        RoundedCornerShape(15.dp)
    }

    // Kinematics: 0.95 scale on press (zero-mobility stationary actuation)
    val scaleAnimState = animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "trigger_scale"
    )
    val rgbBloomAlphaState = animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0.45f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "trigger_rgb_bloom"
    )
    // Analog meter liquid ramp
    val fillProgressState = animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 380f),
        label = "trigger_meter_fill"
    )

    // LT = Cyan, RT = Hot Pink / Magenta (matching HTML --glow: #e055b8 and NEXPAD dual-color palette)
    val neonColor = if (isLeft) NeonPalette.Cyan else NeonPalette.PlayStationPink

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
            radius = 105f
        )
    }

    val currentOnVibrate by rememberUpdatedState(onVibrate)
    val currentViewModel by rememberUpdatedState(viewModel)

    val density = LocalDensity.current.density
    val stroke1_5dp = remember(density) { Stroke(width = 1.5f * density) }
    val stroke2dp = remember(density) { Stroke(width = 2f * density) }
    val stroke3_5dp = remember(density) { Stroke(width = 3.5f * density) }
    val stroke5dp = remember(density) { Stroke(width = 5f * density) }

    val insetShadowColorsPressed = remember { listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)) }
    val insetShadowColorsNormal = remember { listOf(Color.Transparent, Color.Black.copy(alpha = 0.70f)) }
    val specularSheenColors = remember { listOf(Color.White.copy(alpha = 0.06f), Color.Transparent) }
    val windowTopShadowColors = remember { listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent) }
    val windowVignetteColors = remember { listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)) }
    val lensHighlightColors = remember { listOf(Color.Transparent, Color.White.copy(alpha = 0.22f), Color.Transparent) }
    val lensHighlightPressedColors = remember { listOf(Color.Transparent, Color.White.copy(alpha = 0.10f), Color.Transparent) }

    val shieldAuraColors = remember(neonColor) {
        arrayListOf(Color.Transparent, Color.Transparent, Color.Transparent)
    }
    val plumeColors = remember(neonColor) {
        arrayListOf(Color.Transparent, Color.Transparent, Color.Transparent)
    }
    val fluidGradientColors = remember(neonColor) {
        arrayListOf(Color.Transparent, Color.Transparent)
    }

    val chassisBorderBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color.Black.copy(alpha = 0.50f),
                Color.Black.copy(alpha = 0.80f)
            )
        )
    }

    val liquidPath = remember { Path() }
    val ringPath = remember { Path() }

    Box(
        modifier = modifier
            .size(100.dp, 92.dp)
            .graphicsLayer {
                // Root RenderNode isolation
            }
            // Outer dynamic RGB aura
            .drawBehind {
                if (isRgbEnabled) {
                    val fillProgress = fillProgressState.value
                    val rgbBloomAlpha = rgbBloomAlphaState.value
                    val padX = 12f * density
                    val padTop = 6f * density
                    val padBottom = (10f + 16f * fillProgress) * density

                    // 1. Asymmetric Trigger Shield Aura matching trigger contour
                    shieldAuraColors[0] = neonColor.copy(alpha = rgbBloomAlpha * (if (isPressed) 0.55f else 0.32f))
                    shieldAuraColors[1] = neonColor.copy(alpha = rgbBloomAlpha * 0.15f)
                    shieldAuraColors[2] = Color.Transparent
                    drawRoundRect(
                        brush = Brush.radialGradient(
                            colors = shieldAuraColors,
                            center = Offset(center.x, center.y + 6f * density * fillProgress),
                            radius = size.width * 0.65f
                        ),
                        topLeft = Offset(-padX, -padTop),
                        size = Size(size.width + padX * 2f, size.height + padTop + padBottom),
                        cornerRadius = CornerRadius(20f * density, 44f * density)
                    )

                    // 2. Downward Squeeze Thruster Plume along bottom hull
                    if (fillProgress > 0.05f) {
                        val plumeW = size.width * 0.70f
                        val plumeH = 20f * density * fillProgress
                        val plumeY = size.height + 2f * density
                        plumeColors[0] = Color.White.copy(alpha = rgbBloomAlpha * fillProgress * 0.70f)
                        plumeColors[1] = neonColor.copy(alpha = rgbBloomAlpha * fillProgress * 0.90f)
                        plumeColors[2] = Color.Transparent
                        drawOval(
                            brush = Brush.radialGradient(
                                colors = plumeColors,
                                center = Offset(center.x, plumeY),
                                radius = plumeW * 0.50f
                            ),
                            topLeft = Offset(center.x - plumeW / 2f, plumeY - plumeH * 0.30f),
                            size = Size(plumeW, plumeH)
                        )
                    }
                }
            }
            // RGB-coordinated shadow
            .shadow(
                elevation = if (isPressed) 2.dp else 6.dp,
                shape = triggerShape,
                ambientColor = if (isRgbEnabled) neonColor else Color.Black,
                spotColor = if (isRgbEnabled) neonColor else Color.Black
            )
            .graphicsLayer {
                scaleX = scaleAnimState.value
                scaleY = scaleAnimState.value
            }
            .clip(triggerShape)
            // Convex dome surface
            .background(if (isPressed) pressedDomeGradient else baseDomeGradient)
            // 1px chassis border outline
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
                                isPressed = true
                                currentViewModel?.updateButton(key, true)
                                tryAwaitRelease()
                                isPressed = false
                                currentViewModel?.updateButton(key, false)
                            }
                        )
                    }
                } else Modifier
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        // Multi-layer visual canvas: Meter fluid surge, undercut shadow, neon ring, specular lens
        Canvas(modifier = Modifier.fillMaxSize()) {
            val fillProgress = fillProgressState.value
            val rgbBloomAlpha = rgbBloomAlphaState.value
            val w = size.width
            val h = size.height

            // 1. Inset Bottom Undercut Shadow along bottom hull
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

            // 2. Analog Meter (.lx-meter): inset 7dp, top radii 11dp, bottom radii 38dp
            val mInset = 7f * density
            val mh = h - mInset * 2f
            val meterTopR = 11f * density
            val meterBotR = 38f * density

            // Meter fluid surging upward from bottom hull on press (Direct hardware path fill)
            if (fillProgress > 0.001f) {
                val liquidH = mh * fillProgress
                val fluidTopY = (h - mInset) - liquidH
                val currentTopR = if (fluidTopY <= mInset + meterTopR) {
                    ((mInset + meterTopR - fluidTopY) / meterTopR * meterTopR).coerceIn(0f, meterTopR)
                } else 0f

                liquidPath.reset()
                liquidPath.addRoundRect(
                    RoundRect(
                        left = mInset,
                        top = fluidTopY,
                        right = w - mInset,
                        bottom = h - mInset,
                        topLeftCornerRadius = CornerRadius(currentTopR, currentTopR),
                        topRightCornerRadius = CornerRadius(currentTopR, currentTopR),
                        bottomLeftCornerRadius = CornerRadius(meterBotR, meterBotR),
                        bottomRightCornerRadius = CornerRadius(meterBotR, meterBotR)
                    )
                )

                // Fluid vertical gradient fill: glow at bottom, fading toward top
                fluidGradientColors[0] = Color.Transparent
                fluidGradientColors[1] = neonColor.copy(alpha = 0.50f * fillProgress)
                drawPath(
                    path = liquidPath,
                    brush = Brush.verticalGradient(
                        colors = fluidGradientColors,
                        startY = fluidTopY,
                        endY = h - mInset
                    )
                )

                // Emissive top fluid crest line & bloom
                drawLine(
                    color = neonColor.copy(alpha = 0.55f * fillProgress),
                    start = Offset(mInset + currentTopR, fluidTopY),
                    end = Offset(w - mInset - currentTopR, fluidTopY),
                    strokeWidth = stroke5dp.width
                )
                drawLine(
                    color = neonColor.copy(alpha = 1.0f * fillProgress),
                    start = Offset(mInset + currentTopR, fluidTopY),
                    end = Offset(w - mInset - currentTopR, fluidTopY),
                    strokeWidth = stroke2dp.width
                )
            }

            // 3. Neon Lens Ring (.lx-ring): inset 2.5dp, dual-pass stroke bloom
            val ringInset = 2.5f * density
            val ringTopR = 13.5f * density
            val ringBotR = 43.5f * density
            ringPath.reset()
            ringPath.addRoundRect(
                RoundRect(
                    left = ringInset,
                    top = ringInset,
                    right = w - ringInset,
                    bottom = h - ringInset,
                    topLeftCornerRadius = CornerRadius(ringTopR, ringTopR),
                    topRightCornerRadius = CornerRadius(ringTopR, ringTopR),
                    bottomLeftCornerRadius = CornerRadius(ringBotR, ringBotR),
                    bottomRightCornerRadius = CornerRadius(ringBotR, ringBotR)
                )
            )

            // Outer atmospheric bloom stroke
            drawPath(
                path = ringPath,
                color = neonColor.copy(alpha = rgbBloomAlpha * 0.55f),
                style = stroke3_5dp
            )
            // Core crisp filament stroke
            drawPath(
                path = ringPath,
                color = neonColor.copy(alpha = if (isPressed) 1.0f else 0.70f),
                style = stroke1_5dp
            )

            // 4. Glass Lens Highlights (.lx-lens)
            // Top specular line highlight along the top edge
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = if (isPressed) lensHighlightPressedColors else lensHighlightColors
                ),
                start = Offset(w * 0.15f, 2f * density),
                end = Offset(w * 0.85f, 2f * density),
                strokeWidth = stroke1_5dp.width
            )

            // Bottom-right specular sheen circle in the lower hull
            drawCircle(
                brush = Brush.radialGradient(
                    colors = specularSheenColors,
                    center = Offset(w * 0.68f, h * 0.75f),
                    radius = w * 0.24f
                ),
                center = Offset(w * 0.68f, h * 0.75f),
                radius = w * 0.24f
            )
        }

        // 5. Recessed Optical Window (.lx-window): 68dp x 30dp, r=15dp, positioned at top offset 12dp
        Box(
            modifier = Modifier
                .offset(y = 12.dp)
                .size(68.dp, 30.dp)
                .shadow(
                    elevation = 3.dp,
                    shape = windowShape,
                    spotColor = Color.Black,
                    ambientColor = Color.Black
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
            // Window inner vignette and top inset shadow
            Canvas(modifier = Modifier.fillMaxSize()) {
                val winW = size.width
                val winH = size.height

                // Inset shadow at top of window: inset 0 2px 5px rgba(0,0,0,0.9)
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = windowTopShadowColors,
                        startY = 0f,
                        endY = winH * 0.45f
                    ),
                    topLeft = Offset(0f, 0f),
                    size = Size(winW, winH * 0.45f)
                )

                // Edge radial vignette
                drawOval(
                    brush = Brush.radialGradient(
                        colors = windowVignetteColors,
                        center = Offset(winW / 2f, winH / 2f),
                        radius = winW / 2f
                    ),
                    topLeft = Offset(0f, 0f),
                    size = Size(winW, winH)
                )

                // Lower specular highlight line
                drawLine(
                    color = Color.White.copy(alpha = 0.05f),
                    start = Offset(winW * 0.20f, winH - 1f),
                    end = Offset(winW * 0.80f, winH - 1f),
                    strokeWidth = 1f
                )
            }

            // Tactical Typography (.lx-g): bold medium glyph with letter spacing, NO 0%-100% text
            Text(
                text = displayLabel ?: (if (isLeft) "LT" else "RT"),
                color = neonColor,
                fontWeight = FontWeight.Medium,
                fontSize = 18.sp,
                letterSpacing = 1.5.sp
            )
        }
    }
}

/**
 * Static preview delegate for RealisticTrigger with 0 touch overhead and 0 coroutines.
 */
@Composable
fun StaticRealisticTrigger(
    key: String = "LT",
    isRgbEnabled: Boolean = true,
    displayLabel: String? = null,
    modifier: Modifier = Modifier
) {
    RealisticTrigger(
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
