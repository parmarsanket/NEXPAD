package com.sanket.tools.nexpad.ui.components.controller.special

import com.sanket.tools.nexpad.ui.theme.NeonPalette
import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.time.Duration.Companion.milliseconds

/**
 * Console-grade, ergonomic Virtual Touchpad for Twin-Stick & Touch Look gaming schemes.
 *
 * Implements the industry-standard control model popularized by Genshin Impact, Call of Duty: Mobile,
 * and Steam Deck:
 * - [isLeft] = true (LTP / Left Touchpad): Floating dynamic-center movement surface. Touching down anywhere
 *   establishes a virtual pivot; dragging deflects Left Stick (LS X/Y) with sliding-anchor tracking.
 * - [isLeft] = false (RTP / Right Touchpad): Expansive free-touch drag surface. Dragging converts instantaneous
 *   swipe velocity/deltas into Right Stick (RS X/Y) camera deflection, with immediate stop when stationary
 *   and gentle trackball decay on release.
 *
 * Clean, text-free tactile cyber-surface built according to the NEXPAD 7-Layer Display List Pipeline:
 * - Layer 0: Bespoke Kinetic Outer Aura (.drawBehind) with ambient corona, corner brackets & tick notches.
 * - Layer 1: Component Chassis with multi-stop radial gradient & chamfered border.
 * - Layer 2: Tactile Knurling & Precision Laser Radar Range Rings.
 * - Layer 3: Dynamic Capacitive Touch Layer (touch puck bloom, concentric ripples, aiming vector).
 * - Layer 4: Recessed Optical Sensor Well.
 * - Layer 5: Precision Reticle Dot & Ring (pure vector graphics, zero text).
 * - Layer 6: Specular Glass Lens Reflection Arc.
 */

/**
 * Circular buffer averaging the last [capacity] touch samples for stable
 * velocity estimation. Eliminates frame-timing jitter that causes flickering
 * on Android capacitive touchscreens where touch events arrive at inconsistent
 * intervals (4ms, 8ms, 16ms, 20ms).
 */
@Stable
internal class VelocityRingBuffer(private val capacity: Int = 8) {
    private val distances = FloatArray(capacity)
    private val durations = FloatArray(capacity)
    private var head = 0
    private var count = 0

    fun push(distDp: Float, dtSec: Float) {
        distances[head] = distDp
        durations[head] = dtSec.coerceAtLeast(0.001f)
        head = (head + 1) % capacity
        if (count < capacity) count++
    }

    /** Windowed average velocity: totalDist / totalTime over the buffer */
    fun averageSpeed(): Float {
        if (count == 0) return 0f
        var totalDist = 0f
        var totalTime = 0f
        val start = if (count < capacity) 0 else head
        for (i in 0 until count) {
            val idx = (start + i) % capacity
            totalDist += distances[idx]
            totalTime += durations[idx]
        }
        return if (totalTime > 0f) totalDist / totalTime else 0f
    }

    /** Peak instantaneous speed in the window (used for momentum coasting on release) */
    fun peakSpeed(): Float {
        if (count == 0) return 0f
        var maxSpeed = 0f
        val start = if (count < capacity) 0 else head
        for (i in 0 until count) {
            val idx = (start + i) % capacity
            val speed = distances[idx] / durations[idx].coerceAtLeast(0.001f)
            if (speed > maxSpeed) maxSpeed = speed
        }
        return maxSpeed
    }

    fun clear() {
        head = 0
        count = 0
    }
}

/**
 * Professional 5-zone gaming speed-to-distance transfer function.
 *
 * Calibrated Sweet Spots:
 * - Noise Gate: < 8 dp/s -> 0.0 (anti-jitter)
 * - Precision Zone: 8..120 dp/s -> 0.16..0.25 (anti-deadzone floor for sniping / micro-aim)
 * - Linear Tracking Zone: 120..400 dp/s -> 0.25..0.50 (THE GOLDEN SWEET SPOT: 400 dp/s = 0.50 half deflection)
 * - Exponential Acceleration Zone: 400..1200 dp/s -> 0.50..1.00 (Smooth flick curve to full 1.00 deflection)
 * - Saturation Zone: > 1200 dp/s -> 1.00 (clamped maximum)
 */
internal fun calculateGamingStickMagnitude(
    speedDpPerSec: Float,
    sensitivity: Float,
    curve: com.sanket.tools.nexpad.model.TouchpadCurve = com.sanket.tools.nexpad.model.TouchpadCurve.POWER
): Float {
    val effSpeed = speedDpPerSec * sensitivity.coerceAtLeast(0.1f)
    return when (curve) {
        com.sanket.tools.nexpad.model.TouchpadCurve.LINEAR -> {
            if (effSpeed <= 0.5f) 0f
            else (effSpeed / 600f).coerceIn(0f, 1f)
        }
        com.sanket.tools.nexpad.model.TouchpadCurve.POWER -> {
            when {
                effSpeed < 8f -> 0f
                effSpeed <= 120f -> 0.16f + 0.09f * ((effSpeed - 8f) / 112f)
                effSpeed <= 400f -> 0.25f + 0.25f * ((effSpeed - 120f) / 280f)
                effSpeed <= 1200f -> {
                    val norm = (effSpeed - 400f) / 800f
                    0.50f + 0.50f * norm.toDouble().pow(1.35).toFloat()
                }
                else -> 1.0f
            }
        }
    }
}

// =========================================================================
// LAYER 0 & LAYER 2/6 SHARED CANVAS DRAWING PRIMITIVES
// =========================================================================

private fun DrawScope.drawTouchPadAura(
    auraColor: Color,
    rgbBloomAlpha: Float,
    isDragging: Boolean
) {
    val pad = 14.dp.toPx()

    // 1. Broad Ambient Glass Boundary Corona Glow
    drawRoundRect(
        brush = Brush.radialGradient(
            colors = listOf(
                auraColor.copy(alpha = rgbBloomAlpha * (if (isDragging) 0.55f else 0.32f)),
                auraColor.copy(alpha = rgbBloomAlpha * 0.12f),
                Color.Transparent
            ),
            center = center,
            radius = size.width * 0.70f
        ),
        topLeft = Offset(-pad, -pad),
        size = Size(size.width + pad * 2f, size.height + pad * 2f),
        cornerRadius = CornerRadius(38.dp.toPx(), 38.dp.toPx())
    )

    // 2. 4 Precision Corner Registration L-Brackets (Cyber Pips)
    val bracketAlpha = rgbBloomAlpha * (if (isDragging) 0.90f else 0.55f)
    val bracketLen = 12.dp.toPx()
    val inset = 3.dp.toPx()
    val strokeWidth = 2.dp.toPx()

    // Top-Left Bracket
    drawLine(auraColor.copy(alpha = bracketAlpha), Offset(-inset, -inset), Offset(-inset + bracketLen, -inset), strokeWidth, StrokeCap.Square)
    drawLine(auraColor.copy(alpha = bracketAlpha), Offset(-inset, -inset), Offset(-inset, -inset + bracketLen), strokeWidth, StrokeCap.Square)

    // Top-Right Bracket
    drawLine(auraColor.copy(alpha = bracketAlpha), Offset(size.width + inset, -inset), Offset(size.width + inset - bracketLen, -inset), strokeWidth, StrokeCap.Square)
    drawLine(auraColor.copy(alpha = bracketAlpha), Offset(size.width + inset, -inset), Offset(size.width + inset, -inset + bracketLen), strokeWidth, StrokeCap.Square)

    // Bottom-Left Bracket
    drawLine(auraColor.copy(alpha = bracketAlpha), Offset(-inset, size.height + inset), Offset(-inset + bracketLen, size.height + inset), strokeWidth, StrokeCap.Square)
    drawLine(auraColor.copy(alpha = bracketAlpha), Offset(-inset, size.height + inset), Offset(-inset, size.height + inset - bracketLen), strokeWidth, StrokeCap.Square)

    // Bottom-Right Bracket
    drawLine(auraColor.copy(alpha = bracketAlpha), Offset(size.width + inset, size.height + inset), Offset(size.width + inset - bracketLen, size.height + inset), strokeWidth, StrokeCap.Square)
    drawLine(auraColor.copy(alpha = bracketAlpha), Offset(size.width + inset, size.height + inset), Offset(size.width + inset, size.height + inset - bracketLen), strokeWidth, StrokeCap.Square)

    // 3. Cardinal Edge Telemetry Alignment Notches (subtle laser ticks)
    val tickLen = 5.dp.toPx()
    val midX = size.width / 2f
    val midY = size.height / 2f
    val notchAlpha = rgbBloomAlpha * 0.40f
    drawLine(auraColor.copy(alpha = notchAlpha), Offset(midX, -inset - tickLen), Offset(midX, -inset), strokeWidth)
    drawLine(auraColor.copy(alpha = notchAlpha), Offset(midX, size.height + inset), Offset(midX, size.height + inset + tickLen), strokeWidth)
    drawLine(auraColor.copy(alpha = notchAlpha), Offset(-inset - tickLen, midY), Offset(-inset, midY), strokeWidth)
    drawLine(auraColor.copy(alpha = notchAlpha), Offset(size.width + inset, midY), Offset(size.width + inset + tickLen, midY), strokeWidth)
}

private fun DrawScope.drawTouchPadBackgroundMatrix(
    auraColor: Color,
    isRgbEnabled: Boolean,
    stroke1dp: Stroke? = null,
    strokeDashRadar: Stroke? = null
) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val r = size.minDimension / 2f

    val gridColor = if (isRgbEnabled) auraColor else Color.White
    val s1 = stroke1dp ?: Stroke(width = 1.dp.toPx())
    val sDash = strokeDashRadar ?: Stroke(
        width = 1.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()), 0f)
    )

    // Concentric Precision Radar Range Rings (28%, 55%, 80%)
    drawCircle(
        color = gridColor.copy(alpha = 0.08f),
        radius = r * 0.28f,
        center = Offset(cx, cy),
        style = s1
    )
    drawCircle(
        color = gridColor.copy(alpha = 0.06f),
        radius = r * 0.55f,
        center = Offset(cx, cy),
        style = sDash
    )
    drawCircle(
        color = gridColor.copy(alpha = 0.05f),
        radius = r * 0.80f,
        center = Offset(cx, cy),
        style = s1
    )

    // Fine Cardinal Crosshairs with Central Clearance Gap
    val gap = 16.dp.toPx()
    val marginH = 22.dp.toPx()
    val marginV = 22.dp.toPx()
    val crosshairAlpha = 0.07f

    // Horizontal axis
    drawLine(gridColor.copy(alpha = crosshairAlpha), Offset(marginH, cy), Offset(cx - gap, cy), 1.dp.toPx())
    drawLine(gridColor.copy(alpha = crosshairAlpha), Offset(cx + gap, cy), Offset(size.width - marginH, cy), 1.dp.toPx())

    // Vertical axis
    drawLine(gridColor.copy(alpha = crosshairAlpha), Offset(cx, marginV), Offset(cx, cy - gap), 1.dp.toPx())
    drawLine(gridColor.copy(alpha = crosshairAlpha), Offset(cx, cy + gap), Offset(cx, size.height - marginV), 1.dp.toPx())

    // Sub-millimeter graduation ticks along inner perimeter
    val tickSize = 3.dp.toPx()
    val tickAlpha = 0.05f
    val stroke1Px = 1.dp.toPx()
    for (i in 1..3) {
        val frac = i * 0.25f
        val xPos = size.width * frac
        val yPos = size.height * frac
        drawLine(gridColor.copy(alpha = tickAlpha), Offset(xPos, marginV - 4.dp.toPx()), Offset(xPos, marginV - 4.dp.toPx() + tickSize), stroke1Px)
        drawLine(gridColor.copy(alpha = tickAlpha), Offset(xPos, size.height - marginV + 4.dp.toPx() - tickSize), Offset(xPos, size.height - marginV + 4.dp.toPx()), stroke1Px)
        drawLine(gridColor.copy(alpha = tickAlpha), Offset(marginH - 4.dp.toPx(), yPos), Offset(marginH - 4.dp.toPx() + tickSize, yPos), stroke1Px)
        drawLine(gridColor.copy(alpha = tickAlpha), Offset(size.width - marginH + 4.dp.toPx() - tickSize, yPos), Offset(size.width - marginH + 4.dp.toPx(), yPos), stroke1Px)
    }

    // Top Specular Glass Crescent / Lens Sheen Arc (Layer 6)
    drawArc(
        brush = Brush.verticalGradient(
            colors = listOf(Color.White.copy(alpha = 0.12f), Color.Transparent),
            startY = 0f,
            endY = size.height * 0.35f
        ),
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(3.dp.toPx(), 3.dp.toPx()),
        size = Size(size.width - 6.dp.toPx(), size.height * 0.50f),
        style = s1
    )
}

private fun DrawScope.drawActiveCapacitiveTouch(
    touchX: Float,
    touchY: Float,
    anchorX: Float,
    anchorY: Float,
    activeAlpha: Float,
    rgbBloomAlpha: Float,
    auraColor: Color,
    isRgbEnabled: Boolean,
    stroke2dp: Stroke? = null,
    stroke1_2dp: Stroke? = null
) {
    if (activeAlpha <= 0.01f) return

    val puckCenter = Offset(touchX, touchY)
    val glowColor = if (isRgbEnabled) auraColor else Color.White

    // 1. Dynamic Aim / Deflection Laser Guide Line from Anchor to Touch
    val anchor = Offset(anchorX, anchorY)
    val distAnchor = hypot(touchX - anchorX, touchY - anchorY)
    if (distAnchor > 6.dp.toPx()) {
        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(
                    glowColor.copy(alpha = activeAlpha * 0.15f),
                    glowColor.copy(alpha = activeAlpha * 0.60f)
                ),
                start = anchor,
                end = puckCenter
            ),
            start = anchor,
            end = puckCenter,
            strokeWidth = 1.5.dp.toPx(),
            cap = StrokeCap.Round
        )
        // Anchor origin dot
        drawCircle(
            color = glowColor.copy(alpha = activeAlpha * 0.40f),
            radius = 3.dp.toPx(),
            center = anchor
        )
    }

    // 2. Multi-tier Capacitive Touch Bloom
    val puckRadius = 44.dp.toPx()
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = activeAlpha * 0.60f),
                glowColor.copy(alpha = activeAlpha * rgbBloomAlpha * 0.50f),
                glowColor.copy(alpha = activeAlpha * rgbBloomAlpha * 0.15f),
                Color.Transparent
            ),
            center = puckCenter,
            radius = puckRadius
        ),
        center = puckCenter,
        radius = puckRadius
    )

    // 3. Dual Concentric Capacitive Touch Ripples
    drawCircle(
        color = glowColor.copy(alpha = activeAlpha * 0.85f),
        radius = 16.dp.toPx(),
        center = puckCenter,
        style = stroke2dp ?: Stroke(width = 2.dp.toPx())
    )
    drawCircle(
        color = glowColor.copy(alpha = activeAlpha * 0.45f),
        radius = 28.dp.toPx(),
        center = puckCenter,
        style = stroke1_2dp ?: Stroke(width = 1.2.dp.toPx())
    )

    // 4. Specular Core Contact Dot
    drawCircle(
        color = Color.White.copy(alpha = activeAlpha * 0.95f),
        radius = 3.5.dp.toPx(),
        center = puckCenter
    )
}

// =========================================================================
// INTERACTIVE COMPOSABLE (CLEAN, TEXT-FREE TACTILE SURFACE)
// =========================================================================

@Composable
fun RealisticTouchPad(
    isLeft: Boolean,
    isConnected: Boolean = true,
    viewModel: GamepadViewModel? = null,
    onVibrate: () -> Unit = {},
    isRgbEnabled: Boolean = false,
    sensitivity: Float? = null,
    curve: com.sanket.tools.nexpad.model.TouchpadCurve? = null,
    modifier: Modifier = Modifier,
    isInteractive: Boolean = true
) {
    val coroutineScope = rememberCoroutineScope()
    val auraColor = if (isLeft) NeonPalette.Cyan else NeonPalette.Magenta

    // Touch tracking state
    var isDragging by remember { mutableStateOf(false) }
    var touchX by remember { mutableFloatStateOf(0f) }
    var touchY by remember { mutableFloatStateOf(0f) }
    var anchorX by remember { mutableFloatStateOf(0f) }
    var anchorY by remember { mutableFloatStateOf(0f) }
    var decayJob by remember { mutableStateOf<Job?>(null) }

    val rgbBloomAlpha = animateFloatAsState(
        targetValue = if (isDragging) 0.95f else 0.40f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "touchpad_rgb_bloom"
    )

    val activeAlpha = animateFloatAsState(
        targetValue = if (isDragging) 1.0f else 0.0f,
        animationSpec = tween(150),
        label = "touchpad_active_alpha"
    )

    val shape = remember { RoundedCornerShape(26.dp) }
    val innerShape = remember { RoundedCornerShape(18.dp) }

    val density = LocalDensity.current.density
    val stroke1dp = remember(density) { Stroke(width = 1f * density) }
    val stroke1_2dp = remember(density) { Stroke(width = 1.2f * density) }
    val stroke2dp = remember(density) { Stroke(width = 2f * density) }
    val strokeDashRadar = remember(density) {
        Stroke(
            width = 1f * density,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f * density, 4f * density), 0f)
        )
    }

    val surfaceGradient = remember {
        Brush.radialGradient(
            colors = listOf(NeonPalette.MetalBase, NeonPalette.MetalDark, NeonPalette.MetalShadow),
            center = Offset(0.4f, 0.4f),
            radius = 280f
        )
    }
    val sensorWellBrush = remember {
        Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.03f), Color.Transparent),
            radius = 200f
        )
    }

    val context = LocalContext.current
    val padKey = if (isLeft) "LTP" else "RTP"
    val effectiveSensitivity = remember(context, sensitivity, padKey) {
        if (sensitivity != null && sensitivity > 0f) {
            sensitivity
        } else {
            com.sanket.tools.nexpad.storage.NexpadPreferences.getInstance(context).getTouchpadSensitivity(padKey)
        }
    }
    val effectiveCurve = remember(context, curve, padKey) {
        if (curve != null) {
            curve
        } else {
            com.sanket.tools.nexpad.storage.NexpadPreferences.getInstance(context).getTouchpadCurve(padKey)
        }
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                // Root RenderNode isolation
            }
            .size(180.dp)
            // Layer 0: Bespoke Kinetic Outer Aura
            .drawBehind {
                if (isRgbEnabled) {
                    drawTouchPadAura(
                        auraColor = auraColor,
                        rgbBloomAlpha = rgbBloomAlpha.value,
                        isDragging = isDragging
                    )
                }
            }
            .shadow(
                elevation = if (isDragging) 4.dp else 10.dp,
                shape = shape,
                ambientColor = if (isRgbEnabled) auraColor else Color.Black,
                spotColor = if (isRgbEnabled) auraColor else Color.Black
            )
            .clip(shape)
            .background(surfaceGradient)
            .border(
                BorderStroke(
                    2.dp,
                    if (isRgbEnabled) auraColor.copy(alpha = if (isDragging) 0.90f else 0.65f) else NeonPalette.CarbonHighlight
                ),
                shape
            )
            .then(
                if (isInteractive) {
                    Modifier.pointerInput(isLeft, effectiveSensitivity, effectiveCurve, density) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (down.isConsumed) return@awaitEachGesture
                    down.consume()
                    var previousTouchX = down.position.x
                    var previousTouchY = down.position.y

                    var previousTimeMs = System.currentTimeMillis()
                    var currentStickX = 0f
                    var currentStickY = 0f
                    var accumulatedDeltaX = 0f
                    var accumulatedDeltaY = 0f
                    val velocityBuffer = VelocityRingBuffer(8)

                    isDragging = true
                    touchX = down.position.x
                    touchY = down.position.y
                    anchorX = down.position.x
                    anchorY = down.position.y

                    if (isLeft) {
                        viewModel?.updateLeftStick(0f, 0f)
                    } else {
                        viewModel?.updateRightStick(0f, 0f)
                    }

                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                            if (change == null || !change.pressed) {
                                break
                            }

                            val currentTouchX = change.position.x
                            val currentTouchY = change.position.y
                            val deltaX = currentTouchX - previousTouchX
                            val deltaY = currentTouchY - previousTouchY

                            previousTouchX = currentTouchX
                            previousTouchY = currentTouchY
                            change.consume()

                            touchX = currentTouchX
                            touchY = currentTouchY

                            if (isLeft) {
                                val dx = currentTouchX - anchorX
                                val dy = currentTouchY - anchorY
                                // 100% sensitivity boost: travel radius cut to 24dp for rapid 1.0f reach with short movement
                                val baseRadiusDp = 24.dp
                                val maxRadiusPx = baseRadiusDp.toPx()
                                val dist = hypot(dx, dy)
                                if (dist > 0f) {
                                    val sens = effectiveSensitivity.coerceAtLeast(1.0f)
                                    val scaledDist = dist * sens
                                    val clampedDist = scaledDist.coerceAtMost(maxRadiusPx)
                                    val norm = (clampedDist / maxRadiusPx).coerceIn(0f, 1f)
                                    val normX = (dx / dist * norm).coerceIn(-1f, 1f)
                                    val normY = (-dy / dist * norm).coerceIn(-1f, 1f)
                                    viewModel?.updateLeftStick(normX, normY)
                                    // Sliding anchor tracking
                                    val maxTravelPx = maxRadiusPx / sens
                                    if (dist > maxTravelPx) {
                                        anchorX = currentTouchX - (dx / dist * maxTravelPx)
                                        anchorY = currentTouchY - (dy / dist * maxTravelPx)
                                    }
                                }
                            } else {
                                // Directional axis stabilization: suppress minor diagonal cross-talk
                                var stepDeltaX = deltaX
                                var stepDeltaY = deltaY
                                val absX = abs(deltaX)
                                val absY = abs(deltaY)
                                if (absX > 3.0f * absY) {
                                    stepDeltaY *= 0.5f
                                } else if (absY > 3.0f * absX) {
                                    stepDeltaX *= 0.5f
                                }

                                accumulatedDeltaX += stepDeltaX
                                accumulatedDeltaY += stepDeltaY

                                val distPx = hypot(accumulatedDeltaX, accumulatedDeltaY)
                                val distDp = distPx / density

                                // Micro-delta threshold: 0.04f dp ensures creeping thumb movements safely accumulate
                                // across frames without being dropped, delivering smooth micro-aim at any framerate (60Hz, 120Hz).
                                if (distDp >= 0.04f) {
                                    val currentTimeMs = System.currentTimeMillis()
                                    val dtSec = ((currentTimeMs - previousTimeMs).coerceAtLeast(1L)) / 1000f
                                    previousTimeMs = currentTimeMs

                                    velocityBuffer.push(distDp, dtSec)
                                    val smoothedSpeed = velocityBuffer.averageSpeed()

                                    // 100% sensitivity boost: 2x factor enables reaching 1.0f with moderate swipe speeds
                                    val boostSensitivity = effectiveSensitivity * 2.0f
                                    val stickMagnitude = calculateGamingStickMagnitude(smoothedSpeed, boostSensitivity, effectiveCurve)

                                    if (stickMagnitude > 0f) {
                                        val dirX = accumulatedDeltaX / distPx
                                        val dirY = accumulatedDeltaY / distPx

                                        val targetStickX = (dirX * stickMagnitude).coerceIn(-1f, 1f)
                                        val targetStickY = (-dirY * stickMagnitude).coerceIn(-1f, 1f) // Up is positive Y

                                        // Snappy 75/25 responsiveness (faster output reach)
                                        currentStickX = 0.75f * targetStickX + 0.25f * currentStickX
                                        currentStickY = 0.75f * targetStickY + 0.25f * currentStickY

                                        viewModel?.updateRightStick(currentStickX, currentStickY)

                                        val decayTimeoutMs = (120L - (smoothedSpeed / 20f).toLong()).coerceIn(50L, 120L)
                                        decayJob?.cancel()
                                        decayJob = coroutineScope.launch {
                                            delay(decayTimeoutMs)
                                            currentStickX *= 0.5f
                                            currentStickY *= 0.5f
                                            viewModel?.updateRightStick(currentStickX, currentStickY)
                                            delay(30.milliseconds)
                                            currentStickX *= 0.2f
                                            currentStickY *= 0.2f
                                            viewModel?.updateRightStick(currentStickX, currentStickY)
                                            delay(25.milliseconds)
                                            currentStickX = 0f
                                            currentStickY = 0f
                                            viewModel?.updateRightStick(0f, 0f)
                                        }
                                    }

                                    accumulatedDeltaX = 0f
                                    accumulatedDeltaY = 0f
                                }
                            }
                        }
                    } finally {
                        isDragging = false
                        accumulatedDeltaX = 0f
                        accumulatedDeltaY = 0f
                        decayJob?.cancel()
                        if (isLeft) viewModel?.updateLeftStick(0f, 0f)
                        else viewModel?.updateRightStick(0f, 0f)
                    }
                }
            }
        } else Modifier
    ),
        contentAlignment = Alignment.Center
    ) {
        // Layer 4: Recessed Optical Sensor Well
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
                .clip(innerShape)
                .background(sensorWellBrush)
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.07f)), innerShape)
        )

        // Layer 2 & 6: Background Precision Radar Matrix & Top Specular Lens
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawTouchPadBackgroundMatrix(
                auraColor = auraColor,
                isRgbEnabled = isRgbEnabled,
                stroke1dp = stroke1dp,
                strokeDashRadar = strokeDashRadar
            )

            // Layer 3: Dynamic Capacitive Touch Puck & Ripples
            drawActiveCapacitiveTouch(
                touchX = touchX,
                touchY = touchY,
                anchorX = anchorX,
                anchorY = anchorY,
                activeAlpha = activeAlpha.value,
                rgbBloomAlpha = rgbBloomAlpha.value,
                auraColor = auraColor,
                isRgbEnabled = isRgbEnabled,
                stroke2dp = stroke2dp,
                stroke1_2dp = stroke1_2dp
            )
        }

        // Layer 5: Clean Precision Aiming Reticle Dot & Ring (visible when idle or subtle drag)
        if (!isDragging || activeAlpha.value < 0.2f) {
            Canvas(modifier = Modifier.size(16.dp)) {
                val centerPt = Offset(size.width / 2f, size.height / 2f)
                val dotColor = if (isRgbEnabled) auraColor else Color.White
                drawCircle(
                    color = dotColor.copy(alpha = 0.25f),
                    radius = 6.dp.toPx(),
                    center = centerPt,
                    style = stroke1dp
                )
                drawCircle(
                    color = dotColor.copy(alpha = 0.50f),
                    radius = 2.dp.toPx(),
                    center = centerPt
                )
            }
        }
    }
}

// =========================================================================
// STATIC STUDIO PREVIEW COMPOSABLE (CLEAN, TEXT-FREE, ZERO TOUCH OVERHEAD)
// =========================================================================

/**
 * High-performance static preview thumbnail for Button Studio and layout grids.
 * 100% visual parity with the idle state of [RealisticTouchPad].
 * Zero mutable state, zero pointer listeners, zero coroutines.
 */
@Composable
fun StaticRealisticTouchPad(
    isLeft: Boolean,
    isRgbEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    RealisticTouchPad(
        isLeft = isLeft,
        isRgbEnabled = isRgbEnabled,
        modifier = modifier,
        isInteractive = false
    )
}
