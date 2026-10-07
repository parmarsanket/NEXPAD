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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    isConnected: Boolean,
    onVibrate: () -> Unit,
    viewModel: GamepadViewModel,
    isRgbEnabled: Boolean,
    modifier: Modifier = Modifier,
    displayLabel: String? = null
) {
    var isPressed by remember { mutableStateOf(false) }
    val isLeft = key.uppercase() == com.sanket.tools.nexpad.model.NexpadKeys.LT

    // Ergonomic mobile trigger contour: 16dp top corners, 46dp bottom hull
    val triggerShape = remember {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 46.dp, bottomEnd = 46.dp)
    }
    val windowShape = remember {
        RoundedCornerShape(15.dp)
    }

    // Kinematics: 0.95 scale on press (zero-mobility stationary actuation)
    val scaleAnim by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "trigger_scale"
    )
    val rgbBloomAlpha by animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0.45f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "trigger_rgb_bloom"
    )
    // Analog meter liquid ramp
    val fillProgress by animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 380f),
        label = "trigger_meter_fill"
    )

    // LT = Cyan, RT = Hot Pink / Magenta (matching HTML --glow: #e055b8 and NEXPAD dual-color palette)
    val neonColor = if (isLeft) Color(0xFF00E5FF) else Color(0xFFE055B8)

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
            radius = 105f
        )
    }

    val currentOnVibrate by rememberUpdatedState(onVibrate)
    val currentViewModel by rememberUpdatedState(viewModel)

    Box(
        modifier = modifier
            .size(100.dp, 92.dp)
            .graphicsLayer {
                scaleX = scaleAnim
                scaleY = scaleAnim
            }
            // Outer dynamic RGB aura
            .drawBehind {
                if (isRgbEnabled) {
                    val padX = 12.dp.toPx()
                    val padTop = 6.dp.toPx()
                    val padBottom = (10.dp + 16.dp * fillProgress).toPx()

                    // 1. Asymmetric Trigger Shield Aura matching trigger contour
                    drawRoundRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                neonColor.copy(alpha = rgbBloomAlpha * (if (isPressed) 0.55f else 0.32f)),
                                neonColor.copy(alpha = rgbBloomAlpha * 0.15f),
                                Color.Transparent
                            ),
                            center = Offset(center.x, center.y + 6.dp.toPx() * fillProgress),
                            radius = size.width * 0.65f
                        ),
                        topLeft = Offset(-padX, -padTop),
                        size = Size(size.width + padX * 2f, size.height + padTop + padBottom),
                        cornerRadius = CornerRadius(20.dp.toPx(), 44.dp.toPx())
                    )

                    // 2. Downward Squeeze Thruster Plume along bottom hull
                    if (fillProgress > 0.05f) {
                        val plumeW = size.width * 0.70f
                        val plumeH = 20.dp.toPx() * fillProgress
                        val plumeY = size.height + 2.dp.toPx()
                        drawOval(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = rgbBloomAlpha * fillProgress * 0.70f),
                                    neonColor.copy(alpha = rgbBloomAlpha * fillProgress * 0.90f),
                                    Color.Transparent
                                ),
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
            .clip(triggerShape)
            // Convex dome surface
            .background(if (isPressed) pressedDomeGradient else baseDomeGradient)
            // 1px chassis border outline
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
        // Multi-layer visual canvas: Meter fluid surge, undercut shadow, neon ring, specular lens
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. Inset Bottom Undercut Shadow along bottom hull
            val insetH = h * 0.32f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = if (isPressed) 0.85f else 0.70f)
                    ),
                    startY = h - insetH,
                    endY = h
                ),
                topLeft = Offset(0f, h - insetH),
                size = Size(w, insetH)
            )

            // 2. Analog Meter (.lx-meter): inset 7dp, top radii 11dp, bottom radii 38dp
            val mInset = 7.dp.toPx()
            val mw = w - mInset * 2f
            val mh = h - mInset * 2f
            val meterTopR = 11.dp.toPx()
            val meterBotR = 38.dp.toPx()

            val meterPath = Path().apply {
                addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
                        left = mInset,
                        top = mInset,
                        right = w - mInset,
                        bottom = h - mInset,
                        topLeftCornerRadius = CornerRadius(meterTopR, meterTopR),
                        topRightCornerRadius = CornerRadius(meterTopR, meterTopR),
                        bottomLeftCornerRadius = CornerRadius(meterBotR, meterBotR),
                        bottomRightCornerRadius = CornerRadius(meterBotR, meterBotR)
                    )
                )
            }

            // Meter fluid surging upward from bottom hull on press
            if (fillProgress > 0.001f) {
                clipPath(meterPath) {
                    val liquidH = mh * fillProgress
                    val fluidTopY = (h - mInset) - liquidH

                    // Fluid vertical gradient fill: glow at bottom, fading toward top
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                neonColor.copy(alpha = 0.50f * fillProgress)
                            ),
                            startY = fluidTopY,
                            endY = h - mInset
                        ),
                        topLeft = Offset(mInset, fluidTopY),
                        size = Size(mw, liquidH)
                    )

                    // Emissive top fluid crest line & bloom
                    val glowWidth = 5.dp.toPx()
                    drawLine(
                        color = neonColor.copy(alpha = 0.55f * fillProgress),
                        start = Offset(mInset, fluidTopY),
                        end = Offset(w - mInset, fluidTopY),
                        strokeWidth = glowWidth
                    )
                    drawLine(
                        color = neonColor.copy(alpha = 1.0f * fillProgress),
                        start = Offset(mInset, fluidTopY),
                        end = Offset(w - mInset, fluidTopY),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }

            // 3. Neon Lens Ring (.lx-ring): inset 2.5dp, dual-pass stroke bloom
            val ringInset = 2.5.dp.toPx()
            val ringTopR = 13.5.dp.toPx()
            val ringBotR = 43.5.dp.toPx()
            val ringPath = Path().apply {
                addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
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
            }

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

            // 4. Glass Lens Highlights (.lx-lens)
            // Top specular line highlight along the top edge
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = if (isPressed) 0.10f else 0.22f),
                        Color.Transparent
                    )
                ),
                start = Offset(w * 0.15f, 2.dp.toPx()),
                end = Offset(w * 0.85f, 2.dp.toPx()),
                strokeWidth = 1.5.dp.toPx()
            )

            // Bottom-right specular sheen circle in the lower hull
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.06f),
                        Color.Transparent
                    ),
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
                        colors = listOf(
                            Color.Black.copy(alpha = 0.85f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = winH * 0.45f
                    ),
                    topLeft = Offset(0f, 0f),
                    size = Size(winW, winH * 0.45f)
                )

                // Edge radial vignette
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.75f)
                        ),
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
