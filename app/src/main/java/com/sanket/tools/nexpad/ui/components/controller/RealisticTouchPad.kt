package com.sanket.tools.nexpad.ui.components.controller

import android.content.Context
import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
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
 * Professional 3-zone gaming speed-to-distance transfer function.
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
    val accentColor = if (isLeft) Color(0xFF00E5FF) else Color(0xFFFF007F)

    // Touch tracking state
    var isDragging by remember { mutableStateOf(false) }
    var touchX by remember { mutableFloatStateOf(0f) }
    var touchY by remember { mutableFloatStateOf(0f) }
    var decayJob by remember { mutableStateOf<Job?>(null) }

    val activeAlpha by animateFloatAsState(
        targetValue = if (isDragging) 1f else 0.4f,
        animationSpec = tween(150),
        label = "activeAlpha"
    )

    val shape = RoundedCornerShape(26.dp)

    val surfaceGradient = Brush.radialGradient(
        colors = listOf(Color(0xFF23252B), Color(0xFF131418), Color(0xFF0B0C0E)),
        center = Offset(0.4f, 0.4f),
        radius = 280f
    )

    val shadowModifier = if (isRgbEnabled) {
        Modifier.shadow(16.dp, shape, ambientColor = accentColor, spotColor = accentColor)
    } else {
        Modifier.shadow(12.dp, shape, ambientColor = Color.Black, spotColor = Color.Black)
    }

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
            .then(shadowModifier)
            .clip(shape)
            .background(surfaceGradient)
            .border(
                BorderStroke(
                    2.dp,
                    if (isRgbEnabled) accentColor else Color(0xFF353C4A)
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
                            val speedDpPerSec = distDp / dtSec
                            lastSpeed = speedDpPerSec

                            val stickMagnitude = calculateGamingStickMagnitude(speedDpPerSec, effectiveSensitivity)

                            if (stickMagnitude > 0f) {
                                val dirX = finalDeltaX / distPx
                                val dirY = finalDeltaY / distPx

                                val targetStickX = (dirX * stickMagnitude).coerceIn(-1f, 1f)
                                val targetStickY = (-dirY * stickMagnitude).coerceIn(-1f, 1f) // Up is positive Y

                                // Smooth response (EMA) to eliminate finger jitter
                                currentStickX = 0.70f * targetStickX + 0.30f * currentStickX
                                currentStickY = 0.70f * targetStickY + 0.30f * currentStickY

                                if (isLeft) {
                                    viewModel.updateLeftStick(currentStickX, currentStickY)
                                } else {
                                    viewModel.updateRightStick(currentStickX, currentStickY)
                                }

                                // Stationary watchdog: smoothly decays when finger stops moving
                                decayJob?.cancel()
                                decayJob = coroutineScope.launch {
                                    delay(40.milliseconds)
                                    currentStickX *= 0.3f
                                    currentStickY *= 0.3f
                                    if (isLeft) {
                                        viewModel.updateLeftStick(currentStickX, currentStickY)
                                    } else {
                                        viewModel.updateRightStick(currentStickX, currentStickY)
                                    }
                                    delay(30.milliseconds)
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
                    if (lastSpeed > 400f) {
                        // Dynamic momentum coasting: faster flick gives longer, smoother coast
                        val coastSteps = ((lastSpeed / 200f).toInt()).coerceIn(3, 7)
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
        // Laser-etched tactile guides and dynamic reticle Canvas
        Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val guideStroke = Stroke(
                width = 1.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
            )

            // Inner guide rings
            drawCircle(
                color = Color.White.copy(alpha = 0.07f * activeAlpha),
                radius = size.minDimension / 3.0f,
                center = center,
                style = guideStroke
            )
            drawCircle(
                color = accentColor.copy(alpha = 0.12f * activeAlpha),
                radius = size.minDimension / 2.2f,
                center = center,
                style = guideStroke
            )

            // Center tactile crosshair
            val tickLen = 8f
            drawLine(
                color = Color.White.copy(alpha = 0.18f),
                start = Offset(center.x - tickLen, center.y),
                end = Offset(center.x + tickLen, center.y),
                strokeWidth = 2f
            )
            drawLine(
                color = Color.White.copy(alpha = 0.18f),
                start = Offset(center.x, center.y - tickLen),
                end = Offset(center.x, center.y + tickLen),
                strokeWidth = 2f
            )

            if (isDragging) {
                val current = Offset(touchX, touchY)

                drawCircle(
                    color = accentColor.copy(alpha = 0.22f),
                    radius = 30f,
                    center = current
                )
                drawCircle(
                    color = accentColor.copy(alpha = 0.75f),
                    radius = 20f,
                    center = current,
                    style = Stroke(
                        width = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f))
                    )
                )
                drawCircle(
                    color = accentColor,
                    radius = 4f,
                    center = current
                )
            }
        }

        // Tactile Header Label
        Text(
            text = if (isLeft) "TOUCH MOVE • LTP" else "TOUCH LOOK • RTP",
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = accentColor.copy(alpha = 0.65f),
            letterSpacing = 1.2.sp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp)
        )
    }
}
