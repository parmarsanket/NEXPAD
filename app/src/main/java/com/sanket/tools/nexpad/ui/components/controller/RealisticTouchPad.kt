package com.sanket.tools.nexpad.ui.components.controller

import android.content.Context
import android.util.Log
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
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin
import kotlin.time.Duration.Companion.milliseconds

/**
 * Console-grade, ergonomic Virtual Touchpad for Twin-Stick & Touch Look gaming schemes.
 *
 * Implements the industry standard control model popularized by Genshin Impact, Call of Duty: Mobile,
 * and Steam Deck:
 * - [isLeft] = true (LTP / Left Touchpad): Floating dynamic-center movement surface. Touching down anywhere
 *   establishes a virtual pivot; dragging deflects Left Stick (LS X/Y) with sliding-anchor tracking.
 * - [isLeft] = false (RTP / Right Touchpad): Expansive free-touch drag surface. Dragging converts instantaneous
 *   swipe velocity/deltas into Right Stick (RS X/Y) camera deflection, with immediate stop when stationary
 *   and gentle trackball decay on release.
 *
 * Dedicated standalone buttons (LSB/RSB) handle stick click (L3/R3), keeping touchpad input
 * pure and free of accidental center click triggers.
 */

/**
 * Circular buffer averaging the last [capacity] touch samples for stable
 * velocity estimation. Eliminates frame-timing jitter that causes flickering
 * on Android capacitive touchscreens where touch events arrive at inconsistent
 * intervals (4ms, 8ms, 16ms, 20ms).
 *
 * Instead of single-frame `dist / dt` (which spikes on short frames and dips on long frames),
 * this computes `totalDist / totalTime` over the window — exactly how Synaptics, ELAN,
 * and Apple trackpad firmware smooth velocity.
 */
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
internal fun calculateGamingStickMagnitude(speedDpPerSec: Float, sensitivity: Float): Float {
    val effSpeed = speedDpPerSec * sensitivity.coerceAtLeast(0.1f)
    return when {
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

@Composable
fun RealisticTouchPad(
    isLeft: Boolean,
    isConnected: Boolean,
    viewModel: GamepadViewModel,
    onVibrate: () -> Unit = {},
    isRgbEnabled: Boolean = false,
    sensitivity: Float? = null,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val auraColor = if (isLeft) Color(0xFF00E5FF) else Color(0xFFA97CF0)
    val accentColor = auraColor

    // Touch tracking state
    var isDragging by remember { mutableStateOf(false) }
    var touchX by remember { mutableFloatStateOf(0f) }
    var touchY by remember { mutableFloatStateOf(0f) }
    var decayJob by remember { mutableStateOf<Job?>(null) }

    val rgbBloomAlpha by animateFloatAsState(
        targetValue = if (isDragging) 0.95f else 0.40f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "touchpad_rgb_bloom"
    )

    val activeAlpha by animateFloatAsState(
        targetValue = if (isDragging) 1f else 0f,
        animationSpec = tween(150),
        label = "activeAlpha"
    )

    val shape = RoundedCornerShape(26.dp)

    val surfaceGradient = Brush.radialGradient(
        colors = listOf(Color(0xFF23252B), Color(0xFF131418), Color(0xFF0B0C0E)),
        center = Offset(0.4f, 0.4f),
        radius = 280f
    )

    val context = LocalContext.current
    val density = LocalDensity.current.density
    val padKey = if (isLeft) "LTP" else "RTP"
    val effectiveSensitivity = remember(context, sensitivity, padKey) {
        if (sensitivity != null && sensitivity > 0f) {
            sensitivity
        } else {
            val sp = context.getSharedPreferences("nexpad_prefs", Context.MODE_PRIVATE)
            val specificSens = sp.getFloat("TOUCHPAD_SENSITIVITY_$padKey", -1f)
            if (specificSens > 0f) {
                specificSens
            } else {
                val globalPadSens = sp.getFloat("TOUCHPAD_SENSITIVITY", -1f)
                if (globalPadSens > 0f) {
                    globalPadSens
                } else {
                    // Default base sensitivity: 2.0f (2x default sensitivity)
                    val camSens = sp.getFloat("CAMERA_SENSITIVITY", 1.0f)
                    camSens * 2.0f
                }
            }
        }
    }

    Box(
        modifier = modifier
            .size(180.dp)
            .drawBehind {
                if (isRgbEnabled) {
                    val pad = 14.dp.toPx()
                    drawRoundRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                auraColor.copy(alpha = rgbBloomAlpha * 0.45f),
                                auraColor.copy(alpha = rgbBloomAlpha * 0.18f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = size.width * 0.75f
                        ),
                        topLeft = Offset(-pad, -pad),
                        size = Size(size.width + pad * 2, size.height + pad * 2),
                        cornerRadius = CornerRadius(36.dp.toPx(), 36.dp.toPx())
                    )

                    if (isDragging) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    auraColor.copy(alpha = rgbBloomAlpha * 0.40f),
                                    auraColor.copy(alpha = rgbBloomAlpha * 0.15f),
                                    Color.Transparent
                                ),
                                center = Offset(touchX, touchY),
                                radius = 48.dp.toPx()
                            ),
                            center = Offset(touchX, touchY),
                            radius = 48.dp.toPx()
                        )
                    }
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
                    if (isRgbEnabled) auraColor else Color(0xFF353C4A)
                ),
                shape
            )
            .pointerInput(isLeft, effectiveSensitivity, density) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var previousTouchX = down.position.x
                    var previousTouchY = down.position.y

                    var previousTimeMs = System.currentTimeMillis()
                    var currentStickX = 0f
                    var currentStickY = 0f
                    var lastSpeed = 0f
                    val velocityBuffer = VelocityRingBuffer(8)

                    isDragging = true
                    touchX = down.position.x
                    touchY = down.position.y
                    if (isLeft) {
                        viewModel.updateLeftStick(0f, 0f)
                    } else {
                        viewModel.updateRightStick(0f, 0f)
                    }

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id }
                        if (change == null || !change.pressed) {
                            // Finger lifted up
                            break
                        }

                        val currentTouchX = change.position.x
                        val currentTouchY = change.position.y
                        val deltaX = currentTouchX - previousTouchX
                        val deltaY = currentTouchY - previousTouchY

                        previousTouchX = currentTouchX
                        previousTouchY = currentTouchY
                        change.consume()

                        // ── Both LTP and RTP: Density-Independent Gaming Speed-to-Distance ──
                        touchX = currentTouchX
                        touchY = currentTouchY

                        // Directional axis stabilization: suppress minor diagonal cross-talk
                        var finalDeltaX = deltaX
                        var finalDeltaY = deltaY
                        val absX = kotlin.math.abs(deltaX)
                        val absY = kotlin.math.abs(deltaY)
                        if (absX > 3.0f * absY) {
                            finalDeltaY *= 0.5f // Suppress vertical wobble during horizontal turns
                        } else if (absY > 3.0f * absX) {
                            finalDeltaX *= 0.5f // Suppress horizontal wobble during vertical looks/walks
                        }

                        val distPx = hypot(finalDeltaX, finalDeltaY)
                        val distDp = distPx / density

                        if (distDp > 0.15f) {
                            val currentTimeMs = System.currentTimeMillis()
                            val dtSec = ((currentTimeMs - previousTimeMs).coerceAtLeast(1L)) / 1000f
                            previousTimeMs = currentTimeMs

                            // Push sample into ring buffer for windowed average (eliminates frame-timing jitter)
                            velocityBuffer.push(distDp, dtSec)
                            val smoothedSpeed = velocityBuffer.averageSpeed()
                            lastSpeed = smoothedSpeed

                            val stickMagnitude = calculateGamingStickMagnitude(smoothedSpeed, effectiveSensitivity)

                            if (stickMagnitude > 0f) {
                                val dirX = finalDeltaX / distPx
                                val dirY = finalDeltaY / distPx

                                val targetStickX = (dirX * stickMagnitude).coerceIn(-1f, 1f)
                                val targetStickY = (-dirY * stickMagnitude).coerceIn(-1f, 1f) // Up is positive Y

                                // Smooth response (EMA) — heavier smoothing absorbs remaining per-frame noise
                                currentStickX = 0.55f * targetStickX + 0.45f * currentStickX
                                currentStickY = 0.55f * targetStickY + 0.45f * currentStickY

                                if (isLeft) {
                                    viewModel.updateLeftStick(currentStickX, currentStickY)
                                } else {
                                    viewModel.updateRightStick(currentStickX, currentStickY)
                                }

                                // Adaptive stationary watchdog: timeout scales with speed so fast
                                // dragging never races against the decay timer
                                val decayTimeoutMs = (120L - (smoothedSpeed / 20f).toLong()).coerceIn(50L, 120L)
                                decayJob?.cancel()
                                decayJob = coroutineScope.launch {
                                    delay(decayTimeoutMs)
                                    // 3-stage gentle decay (prevents harsh snap-to-zero flicker)
                                    currentStickX *= 0.5f
                                    currentStickY *= 0.5f
                                    if (isLeft) {
                                        viewModel.updateLeftStick(currentStickX, currentStickY)
                                    } else {
                                        viewModel.updateRightStick(currentStickX, currentStickY)
                                    }
                                    delay(30.milliseconds)
                                    currentStickX *= 0.2f
                                    currentStickY *= 0.2f
                                    if (isLeft) {
                                        viewModel.updateLeftStick(currentStickX, currentStickY)
                                    } else {
                                        viewModel.updateRightStick(currentStickX, currentStickY)
                                    }
                                    delay(25.milliseconds)
                                    currentStickX = 0f
                                    currentStickY = 0f
                                    if (isLeft) {
                                        viewModel.updateLeftStick(0f, 0f)
                                    } else {
                                        viewModel.updateRightStick(0f, 0f)
                                    }
                                    lastSpeed = 0f
                                }
                            }
                        }
                    }

                    // On pointer release
                    isDragging = false
                    decayJob?.cancel()
                    val releaseSpeed = velocityBuffer.peakSpeed()
                    if (releaseSpeed > 400f) {
                        // Dynamic momentum coasting: faster flick gives longer, smoother coast
                        val coastSteps = ((releaseSpeed / 200f).toInt()).coerceIn(3, 7)
                        coroutineScope.launch {
                            var coastX = currentStickX
                            var coastY = currentStickY
                            repeat(coastSteps) {
                                coastX *= 0.68f
                                coastY *= 0.68f
                                if (isLeft) {
                                    viewModel.updateLeftStick(coastX, coastY)
                                } else {
                                    viewModel.updateRightStick(coastX, coastY)
                                }
                                delay(16.milliseconds)
                            }
                            if (isLeft) {
                                viewModel.updateLeftStick(0f, 0f)
                            } else {
                                viewModel.updateRightStick(0f, 0f)
                            }
                        }
                    } else {
                        if (isLeft) {
                            viewModel.updateLeftStick(0f, 0f)
                        } else {
                            viewModel.updateRightStick(0f, 0f)
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Flat stationary trackpad surface
        val innerShape = RoundedCornerShape(18.dp)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
                .clip(innerShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.03f), Color.Transparent),
                        radius = 200f
                    )
                )
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.07f)), innerShape)
        )

        // Glowing touch indicator puck on active finger drag
        if (isDragging || activeAlpha > 0.05f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val puckCenter = Offset(touchX, touchY)
                val puckGlowRadius = 32.dp.toPx()
                // Outer radial bloom
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            (if (isRgbEnabled) auraColor else Color.White).copy(alpha = activeAlpha * 0.55f),
                            (if (isRgbEnabled) auraColor else Color.White).copy(alpha = activeAlpha * 0.18f),
                            Color.Transparent
                        ),
                        center = puckCenter,
                        radius = puckGlowRadius
                    ),
                    center = puckCenter,
                    radius = puckGlowRadius
                )
                // Crisp neon reticle ring
                drawCircle(
                    color = if (isRgbEnabled) auraColor.copy(alpha = activeAlpha * 0.85f) else Color.White.copy(alpha = 0.70f),
                    radius = 12.dp.toPx(),
                    center = puckCenter,
                    style = Stroke(width = 2.dp.toPx())
                )
                // Core specular dot
                drawCircle(
                    color = Color.White.copy(alpha = activeAlpha * 0.95f),
                    radius = 3.dp.toPx(),
                    center = puckCenter
                )
            }
        }

        // Tactile Header Label
        Text(
            text = if (isLeft) "TOUCH MOVE • LTP" else "TOUCH LOOK • RTP",
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = if (isRgbEnabled) auraColor.copy(alpha = if (isDragging) 0.95f else 0.70f) else Color.White.copy(alpha = 0.70f),
            letterSpacing = 1.2.sp,
            style = androidx.compose.ui.text.TextStyle(
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = if (isRgbEnabled) auraColor.copy(alpha = rgbBloomAlpha * 0.75f) else Color.Black.copy(alpha = 0.8f),
                    offset = Offset(0f, 1f),
                    blurRadius = 3f
                )
            ),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp)
        )

        // Tactile Footer
        Text(
            text = "2.0X BALLISTICS",
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Normal,
            color = Color.White.copy(alpha = 0.35f),
            letterSpacing = 0.8.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp)
        )
    }
}
