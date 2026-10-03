package com.sanket.tools.nexpad.ui.components.controller

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
        label = "vu_slabs_scale"
    )
    val pressOffsetYAnim by animateFloatAsState(
        targetValue = if (isPressed) 2.0f else 0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "vu_slabs_offset"
    )
    val rgbBloomAlpha by animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0.45f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "vu_slabs_rgb_bloom"
    )
    // Dynamic Trigger Pull Fill Progress (0.0 to 1.0)
    val fillProgress by animateFloatAsState(
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

    Box(
        modifier = modifier
            .size(100.dp, 92.dp)
            .graphicsLayer {
                scaleX = scaleAnim
                scaleY = scaleAnim
            }
            .offset { IntOffset(0, pressOffsetYAnim.dp.roundToPx()) }
            // Outer dynamic RGB aura
            .drawBehind {
                if (isRgbEnabled) {
                    val padX = 12.dp.toPx()
                    val padY = 8.dp.toPx()

                    val activeVuColor = when {
                        fillProgress > 0.85f -> redOverdrive
                        fillProgress > 0.60f -> orangeOverdrive
                        else -> neonColor
                    }

                    // 1. Tapered Hull Aura with VU color shifting
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                neonColor.copy(alpha = rgbBloomAlpha * (if (isPressed) 0.50f else 0.30f)),
                                activeVuColor.copy(alpha = rgbBloomAlpha * (if (isPressed) 0.75f else 0.40f)),
                                Color.Transparent
                            ),
                            startY = -padY,
                            endY = size.height + padY + 12.dp.toPx() * fillProgress
                        ),
                        topLeft = Offset(-padX, -padY),
                        size = Size(size.width + padX * 2f, size.height + padY * 2f + 12.dp.toPx() * fillProgress),
                        cornerRadius = CornerRadius(24.dp.toPx(), 36.dp.toPx())
                    )

                    // 2. Lateral Equalizer Soundwave Spectrum Wings (4 pairs on left & right)
                    if (fillProgress > 0.15f) {
                        val wingYStart = size.height * 0.40f
                        val wingYStep = 10.dp.toPx()
                        for (i in 0 until 4) {
                            val wingFrac = (fillProgress - (i * 0.2f)).coerceIn(0f, 1f)
                            if (wingFrac > 0f) {
                                val wingLen = (6.dp + 12.dp * wingFrac).toPx()
                                val wingY = wingYStart + i * wingYStep
                                val barColor = when (i) {
                                    3 -> redOverdrive
                                    2 -> orangeOverdrive
                                    else -> neonColor
                                }
                                val barAlpha = rgbBloomAlpha * wingFrac * 0.85f

                                // Left wing
                                drawLine(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(Color.Transparent, barColor.copy(alpha = barAlpha)),
                                        startX = -padX - wingLen,
                                        endX = -padX
                                    ),
                                    start = Offset(-padX - wingLen, wingY),
                                    end = Offset(-padX, wingY),
                                    strokeWidth = 3.dp.toPx()
                                )

                                // Right wing
                                drawLine(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(barColor.copy(alpha = barAlpha), Color.Transparent),
                                        startX = size.width + padX,
                                        endX = size.width + padX + wingLen
                                    ),
                                    start = Offset(size.width + padX, wingY),
                                    end = Offset(size.width + padX + wingLen, wingY),
                                    strokeWidth = 3.dp.toPx()
                                )
                            }
                        }
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
        contentAlignment = Alignment.TopCenter
    ) {
        val ringPath = remember { Path() }

        // LAYER STACK CANVAS: Undercut Shadow + Neon Ring + 7 VU Meter Slabs + Lens Reflections
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Inset bottom shadow along pedal hull
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

            // Multi-pass emissive neon ring (.lx-ring) mapped accurately to 22dp/34dp hull
            val ringInset = 2.5.dp.toPx()
            val ringTopR = 19.5.dp.toPx()
            val ringBotR = 31.5.dp.toPx()
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
                style = Stroke(width = 3.5.dp.toPx())
            )
            // Core crisp filament stroke
            drawPath(
                path = ringPath,
                color = neonColor.copy(alpha = if (isPressed) 1.0f else 0.70f),
                style = Stroke(width = 1.5.dp.toPx())
            )

            // =========================================================================
            // SEVEN AUDIO METER VU SLABS (HTML .vu i Translation)
            // =========================================================================
            val slabHeight = 4.dp.toPx()
            val slabGap = 2.5.dp.toPx()
            val slabRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            val bottomMargin = 10.dp.toPx()

            for (i in 0..6) {
                val slabW = SLAB_WIDTHS_DP[i].dp.toPx()
                val slabColor = when (i) {
                    5 -> orangeOverdrive
                    6 -> redOverdrive
                    else -> neonColor
                }

                // Opacity curve translated directly from W3C CSS blueprint:
                // opacity: clamp(0.13, (fill - i * 0.143) * 16, 1.0)
                val rawOpacity = (fillProgress - i * 0.143f) * 16f
                val slabOpacity = rawOpacity.coerceIn(0.13f, 1.0f)

                val slabLeft = (w - slabW) / 2f
                val slabTop = (h - bottomMargin - slabHeight) - i * (slabHeight + slabGap)

                // Emissive neon bloom pass when slab ignites (> 30% lit)
                if (slabOpacity > 0.30f) {
                    drawRoundRect(
                        color = slabColor.copy(alpha = (slabOpacity * 0.40f).coerceAtMost(0.40f)),
                        topLeft = Offset(slabLeft - 2.dp.toPx(), slabTop - 1.5.dp.toPx()),
                        size = Size(slabW + 4.dp.toPx(), slabHeight + 3.dp.toPx()),
                        cornerRadius = CornerRadius(3.5.dp.toPx(), 3.5.dp.toPx())
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
                        colors = listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent),
                        startY = 0f,
                        endY = 5.dp.toPx()
                    ),
                    size = Size(winW, 5.dp.toPx())
                )

                // Perimeter shadow vignette (.lx-window::after)
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
