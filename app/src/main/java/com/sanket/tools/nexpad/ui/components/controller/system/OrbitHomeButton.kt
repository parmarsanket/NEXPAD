package com.sanket.tools.nexpad.ui.components.controller.system

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
import androidx.compose.runtime.*
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.sanket.tools.nexpad.category.ControlKey
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Native Jetpack Compose implementation of NEXPAD Orbit Home (home-orbit).
 *
 * Faithfully translates the authentic Orbit Home HTML/CSS specification:
 * - Oversized 74dp focal circle matching .sy-orb (96px HTML)
 * - Shared optical lens acrylic dome (.lx-body)
 * - 3-segment dashed orbital ring (.orb-svg: r=28 in 96x96, pathLength=100, stroke-dasharray="26 7.33")
 * - Dynamic 120° cyclic rotation with spring overshoot bounce on press (.sy-orb.pressed: rotate(120deg))
 * - Luminous central core dot (.dot: 12px with glowing radial bloom)
 * - Emissive neon lens ring (.lx-ring) and top specular glass crescent highlight (.lx-lens)
 * - Bespoke orbital resonance outer aura (.drawBehind) with rotating satellite pips
 */
@Composable
fun OrbitHomeButton(
    key: String = ControlKey.GUIDE.key,
    isConnected: Boolean,
    onVibrate: () -> Unit,
    viewModel: GamepadViewModel,
    isRgbEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    // Plunging kinematics
    val scaleAnim by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "orbit_home_scale"
    )
    val pressOffsetYAnim by animateFloatAsState(
        targetValue = if (isPressed) 2f else 0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "orbit_home_offset"
    )

    // Dynamic 120° cyclic rotation matching cubic-bezier(.3, 1.5, .5, 1)
    val orbitRotAnim by animateFloatAsState(
        targetValue = if (isPressed) 120f else 0f,
        animationSpec = spring(dampingRatio = 0.52f, stiffness = 380f),
        label = "orbit_home_rot"
    )

    // Bloom pulse
    val rgbBloomAlpha by animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0.70f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "orbit_home_bloom"
    )

    val currentOnVibrate by rememberUpdatedState(onVibrate)
    val currentViewModel by rememberUpdatedState(viewModel)

    val glowColor = remember(isRgbEnabled) {
        if (isRgbEnabled) Color(0xFF00E5FF) else Color(0xFFF0F3F8)
    }

    val buttonSize = 74.dp

    // Multi-stop convex dome gradient: #232527 0%, #0c0d0e 75%, #000 100%
    val domeGradient = remember {
        Brush.radialGradient(
            colors = listOf(Color(0xFF232527), Color(0xFF0C0D0E), Color(0xFF000000)),
            center = Offset(0.50f, 0.55f),
            radius = 180f
        )
    }

    Box(
        modifier = modifier
            .size(buttonSize)
            .graphicsLayer {
                scaleX = scaleAnim
                scaleY = scaleAnim
            }
            .offset { IntOffset(0, pressOffsetYAnim.dp.roundToPx()) }
            // Bespoke Planetary Orbital Resonance Aura
            .drawBehind {
                if (isRgbEnabled) {
                    val auraR = size.minDimension * 0.85f

                    // 1. Broad celestial ambient corona
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                glowColor.copy(alpha = rgbBloomAlpha * (if (isPressed) 0.65f else 0.35f)),
                                glowColor.copy(alpha = rgbBloomAlpha * 0.15f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = auraR
                        ),
                        radius = auraR,
                        center = center
                    )

                    // 2. Outer dashed orbital resonance track
                    val trackR = size.minDimension * 0.5f + 6.dp.toPx()
                    drawCircle(
                        color = glowColor.copy(alpha = rgbBloomAlpha * (if (isPressed) 0.45f else 0.20f)),
                        radius = trackR,
                        center = center,
                        style = Stroke(
                            width = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(4.dp.toPx(), 8.dp.toPx()), 0f
                            )
                        )
                    )

                    // 3. Three satellite resonance pips rotating along orbit
                    rotate(degrees = orbitRotAnim, pivot = center) {
                        for (i in 0 until 3) {
                            val angleRad = Math.toRadians(i * 120.0)
                            val pipX = center.x + trackR * cos(angleRad).toFloat()
                            val pipY = center.y + trackR * sin(angleRad).toFloat()
                            drawCircle(
                                color = glowColor.copy(alpha = rgbBloomAlpha * (if (isPressed) 0.95f else 0.50f)),
                                radius = 2.dp.toPx(),
                                center = Offset(pipX, pipY)
                            )
                        }
                    }
                }
            }
            .shadow(
                elevation = if (isPressed) 2.dp else 6.dp,
                shape = CircleShape,
                ambientColor = if (isRgbEnabled) glowColor else Color.Black,
                spotColor = if (isRgbEnabled) glowColor else Color.Black
            )
            .clip(CircleShape)
            .background(domeGradient)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.50f),
                        Color.Black.copy(alpha = 0.85f)
                    )
                ),
                shape = CircleShape
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
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val r = size.minDimension / 2f

            // Layer 1: Recessed bottom undercut shadow (.lx-body::box-shadow)
            val botShadowH = h * 0.38f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = if (isPressed) 0.85f else 0.70f)
                    ),
                    startY = h - botShadowH,
                    endY = h
                ),
                topLeft = Offset(0f, h - botShadowH),
                size = Size(w, botShadowH)
            )

            // Layer 2: Outer emissive neon ring (.lx-ring, inset 3px)
            val ringR = r - 3.dp.toPx()
            // Ambient halo bloom
            drawCircle(
                color = glowColor.copy(alpha = if (isPressed) 0.65f else 0.30f),
                radius = ringR,
                style = Stroke(width = 4.dp.toPx())
            )
            // Core sharp stroke
            drawCircle(
                color = glowColor.copy(alpha = rgbBloomAlpha),
                radius = ringR,
                style = Stroke(width = 2.dp.toPx())
            )

            // Layer 3: Dynamic 3-Segment Dashed Orbit Ring (.orb-svg: r=28 in 96x96 space)
            // Arc = 93.6°, Gap = 26.4° (3 cycles = 360°)
            val orbitR = r * (28f / 48f)
            val strokeW = 3.6.dp.toPx()
            val dashAngle = 93.6f
            val gapAngle = 26.4f

            rotate(degrees = orbitRotAnim, pivot = center) {
                // Emissive halo pass for orbit ring
                for (i in 0 until 3) {
                    val startAngle = i * (dashAngle + gapAngle) + gapAngle / 2f
                    drawArc(
                        color = glowColor.copy(alpha = if (isPressed) 0.50f else 0.25f),
                        startAngle = startAngle,
                        sweepAngle = dashAngle,
                        useCenter = false,
                        topLeft = Offset(center.x - orbitR, center.y - orbitR),
                        size = Size(orbitR * 2f, orbitR * 2f),
                        style = Stroke(width = strokeW + 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                // Crisp core stroke for orbit ring
                for (i in 0 until 3) {
                    val startAngle = i * (dashAngle + gapAngle) + gapAngle / 2f
                    drawArc(
                        color = if (isPressed) Color.White else glowColor,
                        startAngle = startAngle,
                        sweepAngle = dashAngle,
                        useCenter = false,
                        topLeft = Offset(center.x - orbitR, center.y - orbitR),
                        size = Size(orbitR * 2f, orbitR * 2f),
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )
                }
            }

            // Layer 4: Luminous Center Core Dot (.dot: 12px in 96x96 = 12.5% diameter)
            val dotR = r * (6f / 48f)
            // Radial dot bloom
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glowColor.copy(alpha = if (isPressed) 0.90f else 0.60f),
                        glowColor.copy(alpha = 0.20f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = dotR * 3f
                ),
                radius = dotR * 3f,
                center = center
            )
            // Solid core dot
            drawCircle(
                color = if (isPressed) Color.White else glowColor,
                radius = dotR,
                center = center
            )

            // Layer 5: Top Specular Glass Crescent Arc (.lx-lens)
            drawArc(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isPressed) 0.08f else 0.16f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = h * 0.38f
                ),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(1.5.dp.toPx(), 1.5.dp.toPx()),
                size = Size(w - 3.dp.toPx(), h - 3.dp.toPx()),
                style = Stroke(width = 1.2.dp.toPx())
            )

            // Layer 6: Lens Sheen Reflection Oval (radial-gradient at 70% 78%)
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.07f), Color.Transparent),
                    center = Offset(w * 0.70f, h * 0.78f),
                    radius = w * 0.28f
                ),
                topLeft = Offset(w * 0.50f, h * 0.60f),
                size = Size(w * 0.38f, h * 0.32f)
            )
        }
    }
}

/**
 * Non-interactive static preview of OrbitHomeButton (idle state).
 */
@Composable
internal fun StaticOrbitHomeButton(
    modifier: Modifier = Modifier,
    isRgbEnabled: Boolean = true
) {
    val glowColor = if (isRgbEnabled) Color(0xFF00E5FF) else Color(0xFFF0F3F8)
    val buttonSize = 74.dp

    val domeGradient = remember {
        Brush.radialGradient(
            colors = listOf(Color(0xFF232527), Color(0xFF0C0D0E), Color(0xFF000000)),
            center = Offset(0.50f, 0.55f),
            radius = 180f
        )
    }

    Box(
        modifier = modifier
            .size(buttonSize)
            .drawBehind {
                val auraR = size.minDimension * 0.85f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glowColor.copy(alpha = 0.35f),
                            glowColor.copy(alpha = 0.15f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = auraR
                    ),
                    radius = auraR,
                    center = center
                )
                val trackR = size.minDimension * 0.5f + 6.dp.toPx()
                drawCircle(
                    color = glowColor.copy(alpha = 0.20f),
                    radius = trackR,
                    center = center,
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(4.dp.toPx(), 8.dp.toPx()), 0f
                        )
                    )
                )
                for (i in 0 until 3) {
                    val angleRad = Math.toRadians(i * 120.0)
                    val pipX = center.x + trackR * cos(angleRad).toFloat()
                    val pipY = center.y + trackR * sin(angleRad).toFloat()
                    drawCircle(
                        color = glowColor.copy(alpha = 0.45f),
                        radius = 2.dp.toPx(),
                        center = Offset(pipX, pipY)
                    )
                }
            }
            .shadow(6.dp, CircleShape, ambientColor = glowColor, spotColor = glowColor)
            .clip(CircleShape)
            .background(domeGradient)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Black.copy(alpha = 0.50f), Color.Black.copy(alpha = 0.85f))
                ),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val r = size.minDimension / 2f

            // Undercut shadow
            val botShadowH = h * 0.38f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.70f)),
                    startY = h - botShadowH,
                    endY = h
                ),
                topLeft = Offset(0f, h - botShadowH),
                size = Size(w, botShadowH)
            )

            // Neon ring
            val ringR = r - 3.dp.toPx()
            drawCircle(glowColor.copy(alpha = 0.30f), radius = ringR, style = Stroke(4.dp.toPx()))
            drawCircle(glowColor.copy(alpha = 0.70f), radius = ringR, style = Stroke(2.dp.toPx()))

            // 3-Segment Dashed Orbit Ring
            val orbitR = r * (28f / 48f)
            val strokeW = 3.6.dp.toPx()
            val dashAngle = 93.6f
            val gapAngle = 26.4f

            for (i in 0 until 3) {
                val startAngle = i * (dashAngle + gapAngle) + gapAngle / 2f
                drawArc(
                    color = glowColor,
                    startAngle = startAngle,
                    sweepAngle = dashAngle,
                    useCenter = false,
                    topLeft = Offset(center.x - orbitR, center.y - orbitR),
                    size = Size(orbitR * 2f, orbitR * 2f),
                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                )
            }

            // Center core dot
            val dotR = r * (6f / 48f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glowColor.copy(alpha = 0.60f), Color.Transparent),
                    center = center,
                    radius = dotR * 3f
                ),
                radius = dotR * 3f,
                center = center
            )
            drawCircle(color = glowColor, radius = dotR, center = center)

            // Top specular arc
            drawArc(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.16f), Color.Transparent),
                    startY = 0f,
                    endY = h * 0.38f
                ),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(1.5.dp.toPx(), 1.5.dp.toPx()),
                size = Size(w - 3.dp.toPx(), h - 3.dp.toPx()),
                style = Stroke(width = 1.2.dp.toPx())
            )

            // Sheen oval
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.07f), Color.Transparent),
                    center = Offset(w * 0.70f, h * 0.78f),
                    radius = w * 0.28f
                ),
                topLeft = Offset(w * 0.50f, h * 0.60f),
                size = Size(w * 0.38f, h * 0.32f)
            )
        }
    }
}
