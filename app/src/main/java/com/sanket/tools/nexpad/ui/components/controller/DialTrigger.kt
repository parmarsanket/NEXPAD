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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Native Jetpack Compose implementation of the NEXPAD Dial Trigger (Trigger A — Dial).
 *
 * Translates the 270° radial gauge HTML blueprint into high-performance Compose:
 * - 270° radial gauge sweeping from South-West (135°) over North to South-East (45° / 405°)
 * - Inactive baseline track (stroke-dasharray: 75 100) with rounded end caps
 * - Dynamic active surging gauge arc with emissive bloom halo
 * - Recessed optical window (.lx-window) with high-contrast tactical typography
 * - Acrylic convex dome background with inset undercut shadow
 * - Pure optical gauge layout: strictly omits percentage text readout per mobile design directive
 * - Scaled to compact 92dp x 92dp mobile standard to fit beneath bumpers without crowding controls
 */
@Composable
fun DialTrigger(
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
        label = "dial_trigger_scale"
    )
    val pressOffsetYAnim by animateFloatAsState(
        targetValue = if (isPressed) 2.0f else 0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "dial_trigger_offset"
    )
    val rgbBloomAlpha by animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0.55f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 800f),
        label = "dial_trigger_rgb_bloom"
    )
    // 270° Gauge Fill Progress (0.0 to 1.0)
    val fillProgress by animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 380f),
        label = "dial_trigger_gauge_fill"
    )

    // LT = Cyan (#00E5FF), RT = Hot Pink / Magenta (#E055B8) matching HTML spec
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
            radius = 100f
        )
    }

    val currentOnVibrate by rememberUpdatedState(onVibrate)
    val currentViewModel by rememberUpdatedState(viewModel)

    val triggerShape = CircleShape
    val windowShape = RoundedCornerShape(13.dp)

    Box(
        modifier = modifier
            .size(92.dp, 92.dp)
            .drawBehind {
                if (isRgbEnabled) {
                    val baseR = size.minDimension * 0.5f
                    val curPull = fillProgress

                    // 1. Ambient dial circular halo
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

                    // 2. Circular Tick Graduation Halo (12 ticks around perimeter)
                    val tickRadius = baseR + 4.dp.toPx()
                    for (i in 0 until 12) {
                        val tickAngleDeg = 135.0 + (i * 270.0 / 11.0)
                        val tickRad = Math.toRadians(tickAngleDeg)
                        val cosT = cos(tickRad).toFloat()
                        val sinT = sin(tickRad).toFloat()
                        val innerR = tickRadius
                        val outerR = tickRadius + (if (i == 0 || i == 11) 6.dp.toPx() else 3.5.dp.toPx())
                        val tickAlpha = rgbBloomAlpha * (if (i.toFloat() / 11f <= curPull) 0.90f else 0.30f)

                        drawLine(
                            color = neonColor.copy(alpha = tickAlpha),
                            start = Offset(center.x + innerR * cosT, center.y + innerR * sinT),
                            end = Offset(center.x + outerR * cosT, center.y + outerR * sinT),
                            strokeWidth = if (i == 0 || i == 11) 2.5f else 1.5f
                        )
                    }

                    // 3. Sweeping 270° Gauge Sector Arc
                    val gaugeStart = 135f
                    val gaugeTotalSweep = 270f
                    if (curPull > 0.02f) {
                        val activeSweep = gaugeTotalSweep * curPull
                        drawArc(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = rgbBloomAlpha * curPull * 0.70f),
                                    neonColor.copy(alpha = rgbBloomAlpha * curPull * 0.85f),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = baseR + 8.dp.toPx()
                            ),
                            startAngle = gaugeStart,
                            sweepAngle = activeSweep,
                            useCenter = false,
                            topLeft = Offset(center.x - (baseR + 8.dp.toPx()), center.y - (baseR + 8.dp.toPx())),
                            size = Size((baseR + 8.dp.toPx()) * 2f, (baseR + 8.dp.toPx()) * 2f),
                            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // 4. Pointer Beacon Flare at leading edge of rotary gauge
                        val pointerAngleDeg = gaugeStart + activeSweep
                        val pointerRad = Math.toRadians(pointerAngleDeg.toDouble())
                        val cosP = cos(pointerRad).toFloat()
                        val sinP = sin(pointerRad).toFloat()
                        val ptrX = center.x + (baseR + 8.dp.toPx()) * cosP
                        val ptrY = center.y + (baseR + 8.dp.toPx()) * sinP

                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = rgbBloomAlpha * 0.95f),
                                    neonColor.copy(alpha = rgbBloomAlpha * 0.80f),
                                    Color.Transparent
                                ),
                                center = Offset(ptrX, ptrY),
                                radius = (if (isPressed) 14.dp else 9.dp).toPx()
                            ),
                            center = Offset(ptrX, ptrY),
                            radius = (if (isPressed) 14.dp else 9.dp).toPx()
                        )
                    }
                }
            }
            .graphicsLayer {
                scaleX = scaleAnim
                scaleY = scaleAnim
            }
            .offset { IntOffset(0, pressOffsetYAnim.dp.roundToPx()) }
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
        // LAYER STACK CANVAS: Undercut Shadow + 270° Radial Gauge + Lens Highlights
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Inset bottom shadow along bottom base
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

            // Outer 1px rim outline
            drawCircle(
                color = Color.Black.copy(alpha = 0.50f),
                radius = (w / 2f) - 0.5f,
                style = Stroke(width = 1.dp.toPx())
            )

            // Multi-pass emissive neon ring (.lx-ring)
            val ringInset = 3.dp.toPx()
            val ringRadius = (w / 2f) - ringInset
            drawCircle(
                color = neonColor.copy(alpha = if (isPressed) 0.55f else 0.25f),
                radius = ringRadius,
                style = Stroke(width = 4.dp.toPx())
            )
            drawCircle(
                color = neonColor.copy(alpha = if (isPressed) 1.0f else 0.70f),
                radius = ringRadius,
                style = Stroke(width = 2.dp.toPx())
            )

            // =========================================================================
            // 270° RADIAL GAUGE TRACK (HTML SVG Dial Gauge Translation)
            // =========================================================================
            // Radius of gauge track fits neatly inside outer neon ring
            val gaugeRadius = (w / 2f) - 10.dp.toPx()
            val gaugeCenter = Offset(w / 2f, h / 2f)
            val gaugeTopLeft = Offset(gaugeCenter.x - gaugeRadius, gaugeCenter.y - gaugeRadius)
            val gaugeSize = Size(gaugeRadius * 2f, gaugeRadius * 2f)

            // Inactive track: 270° arc starting at 135° (South-West) sweeping clockwise
            drawArc(
                color = Color.White.copy(alpha = 0.12f),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = gaugeTopLeft,
                size = gaugeSize,
                style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
            )

            // Live active gauge: sweeps dynamically from 135° based on fillProgress
            if (fillProgress > 0.001f) {
                val liveSweep = 270f * fillProgress
                // Outer glow bloom pass
                drawArc(
                    color = neonColor.copy(alpha = (fillProgress * 0.70f).coerceAtMost(0.55f)),
                    startAngle = 135f,
                    sweepAngle = liveSweep,
                    useCenter = false,
                    topLeft = gaugeTopLeft,
                    size = gaugeSize,
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                )
                // Core vivid gauge stroke
                drawArc(
                    color = neonColor,
                    startAngle = 135f,
                    sweepAngle = liveSweep,
                    useCenter = false,
                    topLeft = gaugeTopLeft,
                    size = gaugeSize,
                    style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
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
        // RECESSED OPTICAL WINDOW (.lx-window)
        // =========================================================================
        Box(
            modifier = Modifier
                .offset { IntOffset(0, (-2).dp.roundToPx()) }
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
                fontSize = 17.sp,
                letterSpacing = 1.5.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
