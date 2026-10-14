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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.model.NexpadKeys
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel
import kotlin.math.roundToInt

private val SLAB_WIDTHS_DP = floatArrayOf(54f, 58f, 62f, 65f, 68f, 70f, 72f)

/**
 * Native Jetpack Compose implementation of the NEXPAD VU Slabs Trigger (Trigger D — VU Slabs).
 *
 * Balanced Mobile Ergonomic Layout (Option D: Balanced 22dp/34dp Taper):
 * - Dimensions: 100dp x 92dp — optimized mobile controller ratio matching bumper clearance.
 * - Balanced hull contour: 22dp top corners and 34dp bottom pedal hull for a subtle ergonomic taper without heavy bulging.
 * - Seven illuminated audio meter slabs stacked vertically from bottom hull upward:
 *     * Slabs 0-4: Base neon green (#3FD25A on LT) / magenta (#E055B8 on RT)
 *     * Slab 5: Warning hot amber / orange (#FF8A3D)
 *     * Slab 6: Peak overdrive crimson red (#FF5A4D)
 * - Progressive lighting curve: dim 13% ghost visibility in idle, igniting into full neon bloom on pull.
 * - Recessed optical window (.lx-window, 54dp x 24dp) positioned at top offset 10dp.
 * - True geometry Path rendering for .lx-ring and ambient glow bloom preventing distortion.
 * - Pure optical layout: strictly omits percentage text readout per mobile design directive.
 */
@Composable
fun VuSlabsTrigger(
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

    // Physical Spring Kinematics
    val scaleAnim = animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "vu_slabs_scale"
    )
    val rgbBloomAlpha = animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0.45f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "vu_slabs_rgb_bloom"
    )
    // Dynamic Trigger Pull Fill Progress (0.0 to 1.0)
    val fillProgress = animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 380f),
        label = "vu_slabs_fill"
    )

    // LT = Neon Audio Green (#3FD25A), RT = Hot Pink / Magenta (#E055B8) matching HTML spec
    val neonColor = if (isLeft) Color(0xFF3FD25A) else Color(0xFFE055B8)

    // Overdrive LED colors from HTML blueprint
    val orangeOverdrive = Color(0xFFFF8A3D)
    val redOverdrive = Color(0xFFFF5A4D)

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

    // Recessed optical window gradient (#050506 -> #121314)
    val windowGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF050506),
                Color(0xFF121314)
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

    // Option D: Balanced Subtle Taper (22dp top, 34dp bottom hull)
    val triggerShape = remember {
        RoundedCornerShape(
            topStart = 22.dp,
            topEnd = 22.dp,
            bottomStart = 34.dp,
            bottomEnd = 34.dp
        )
    }
    val windowShape = remember { RoundedCornerShape(12.dp) }

    val hullAuraColors = remember(neonColor) {
        arrayListOf(Color.Transparent, Color.Transparent, Color.Transparent)
    }
    val leftWingColors = remember(neonColor) {
        arrayListOf(Color.Transparent, Color.Transparent)
    }
    val rightWingColors = remember(neonColor) {
        arrayListOf(Color.Transparent, Color.Transparent)
    }

    val density = LocalDensity.current.density
    val strokeChassisRingBloom = remember(density) { Stroke(width = 3.5f * density) }
    val strokeChassisRingCore = remember(density) { Stroke(width = 1.5f * density) }
    val strokeLensArc = remember(density) { Stroke(width = 1f * density) }

    val ringPath = remember { Path() }

    Box(
        modifier = modifier
            .size(100.dp, 92.dp)
            .graphicsLayer { /* Root RenderNode isolation */ }
            // Outer dynamic RGB aura
            .drawBehind {
                if (isRgbEnabled) {
                    val padX = 12f * density
                    val padY = 8f * density
                    val curPull = fillProgress.value

                    val activeVuColor = when {
                        curPull > 0.85f -> redOverdrive
                        curPull > 0.60f -> orangeOverdrive
                        else -> neonColor
                    }

                    // 1. Tapered Hull Aura with VU color shifting
                    hullAuraColors[0] = neonColor.copy(alpha = rgbBloomAlpha.value * (if (isPressed) 0.50f else 0.30f))
                    hullAuraColors[1] = activeVuColor.copy(alpha = rgbBloomAlpha.value * (if (isPressed) 0.75f else 0.40f))
                    hullAuraColors[2] = Color.Transparent
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = hullAuraColors,
                            startY = -padY,
                            endY = size.height + padY + 12f * density * curPull
                        ),
                        topLeft = Offset(-padX, -padY),
                        size = Size(size.width + padX * 2f, size.height + padY * 2f + 12f * density * curPull),
                        cornerRadius = CornerRadius(24f * density, 36f * density)
                    )

                    // 2. Lateral Equalizer Soundwave Spectrum Wings (4 pairs on left & right)
                    if (curPull > 0.15f) {
                        val wingYStart = size.height * 0.40f
                        val wingYStep = 10f * density
                        for (i in 0 until 4) {
                            val wingFrac = (curPull - (i * 0.2f)).coerceIn(0f, 1f)
                            if (wingFrac > 0f) {
                                val wingLen = (6f + 12f * wingFrac) * density
                                val wingY = wingYStart + i * wingYStep
                                val barColor = when (i) {
                                    3 -> redOverdrive
                                    2 -> orangeOverdrive
                                    else -> neonColor
                                }
                                val barAlpha = rgbBloomAlpha.value * wingFrac * 0.85f

                                // Left wing
                                leftWingColors[0] = Color.Transparent
                                leftWingColors[1] = barColor.copy(alpha = barAlpha)
                                drawLine(
                                    brush = Brush.horizontalGradient(
                                        colors = leftWingColors,
                                        startX = -padX - wingLen,
                                        endX = -padX
                                    ),
                                    start = Offset(-padX - wingLen, wingY),
                                    end = Offset(-padX, wingY),
                                    strokeWidth = 3f * density
                                )

                                // Right wing
                                rightWingColors[0] = barColor.copy(alpha = barAlpha)
                                rightWingColors[1] = Color.Transparent
                                drawLine(
                                    brush = Brush.horizontalGradient(
                                        colors = rightWingColors,
                                        startX = size.width + padX,
                                        endX = size.width + padX + wingLen
                                    ),
                                    start = Offset(size.width + padX, wingY),
                                    end = Offset(size.width + padX + wingLen, wingY),
                                    strokeWidth = 3f * density
                                )
                            }
                        }
                    }
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
        // LAYER STACK CANVAS: Undercut Shadow + Neon Ring + 7 VU Meter Slabs + Lens Reflections
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Inset bottom shadow along pedal hull
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

            // Multi-pass emissive neon ring (.lx-ring) mapped accurately to 22dp/34dp hull
            val ringInset = 2.5f * density
            val ringTopR = 19.5f * density
            val ringBotR = 31.5f * density
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
                color = neonColor.copy(alpha = rgbBloomAlpha.value * 0.55f),
                style = strokeChassisRingBloom
            )
            // Core crisp filament stroke
            drawPath(
                path = ringPath,
                color = neonColor.copy(alpha = if (isPressed) 1.0f else 0.70f),
                style = strokeChassisRingCore
            )

            // =========================================================================
            // SEVEN AUDIO METER VU SLABS (HTML .vu i Translation)
            // =========================================================================
            val slabHeight = 4f * density
            val slabGap = 2.5f * density
            val slabRadius = CornerRadius(2f * density, 2f * density)
            val bottomMargin = 10f * density

            for (i in 0..6) {
                val slabW = SLAB_WIDTHS_DP[i] * density
                val slabColor = when (i) {
                    5 -> orangeOverdrive
                    6 -> redOverdrive
                    else -> neonColor
                }

                // Opacity curve translated directly from W3C CSS blueprint:
                // opacity: clamp(0.13, (fill - i * 0.143) * 16, 1.0)
                val rawOpacity = (fillProgress.value - i * 0.143f) * 16f
                val slabOpacity = rawOpacity.coerceIn(0.13f, 1.0f)

                val slabLeft = (w - slabW) / 2f
                val slabTop = (h - bottomMargin - slabHeight) - i * (slabHeight + slabGap)

                // Emissive neon bloom pass when slab ignites (> 30% lit)
                if (slabOpacity > 0.30f) {
                    drawRoundRect(
                        color = slabColor.copy(alpha = (slabOpacity * 0.40f).coerceAtMost(0.40f)),
                        topLeft = Offset(slabLeft - 2f * density, slabTop - 1.5f * density),
                        size = Size(slabW + 4f * density, slabHeight + 3f * density),
                        cornerRadius = CornerRadius(3.5f * density, 3.5f * density)
                    )
                }

                // Core crisp illuminated slab bar
                drawRoundRect(
                    color = slabColor.copy(alpha = slabOpacity),
                    topLeft = Offset(slabLeft, slabTop),
                    size = Size(slabW, slabHeight),
                    cornerRadius = slabRadius
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
        // RECESSED OPTICAL WINDOW (.lx-window top: 10dp)
        // =========================================================================
        Box(
            modifier = Modifier
                .offset { IntOffset(0, 10.dp.roundToPx()) }
                .size(54.dp, 24.dp)
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
                fontSize = 15.sp,
                letterSpacing = 1.5.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Static preview delegate for VuSlabsTrigger with 0 touch overhead and 0 coroutines.
 */
@Composable
fun StaticVuSlabsTrigger(
    key: String = "LT",
    isRgbEnabled: Boolean = true,
    displayLabel: String? = null,
    modifier: Modifier = Modifier
) {
    VuSlabsTrigger(
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
