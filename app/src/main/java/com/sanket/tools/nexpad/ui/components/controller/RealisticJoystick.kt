package com.sanket.tools.nexpad.ui.components.controller

import android.content.Context
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel
import kotlinx.coroutines.delay
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun RealisticJoystick(
    isLeft: Boolean,
    isConnected: Boolean,
    viewModel: GamepadViewModel,
    isRgbEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    var thumbOffsetX by remember { mutableFloatStateOf(0f) }
    var thumbOffsetY by remember { mutableFloatStateOf(0f) }
    var decayJob by remember { mutableStateOf<Job?>(null) }
    val maxRadius = 80f
    val coroutineScope = rememberCoroutineScope()

    val context = LocalContext.current
    val (isCameraMode, cameraSensitivity) = remember(context, isLeft) {
        if (isLeft) {
            Pair(false, 1.0f)
        } else {
            val sp = context.getSharedPreferences("nexpad_prefs", Context.MODE_PRIVATE)
            Pair(
                sp.getBoolean("RIGHT_STICK_CAMERA_MODE", false),
                sp.getFloat("CAMERA_SENSITIVITY", 1.0f)
            )
        }
    }

    val density = LocalDensity.current.density
    val baseGradient = Brush.radialGradient(
        colors = listOf(Color(0xFF1F1F1F), Color(0xFF080808)),
        center = Offset(0.5f, 0.5f),
        radius = 250f
    )

    val thumbGradient = Brush.radialGradient(
        colors = listOf(Color(0xFF3D3D3D), Color(0xFF1A1A1A)),
        center = Offset(0.3f, 0.3f),
        radius = 150f
    )

    val rgbShadow = if (isRgbEnabled) {
        Modifier.shadow(15.dp, CircleShape, ambientColor = if (isLeft) Color.Cyan else Color.Magenta, spotColor = if (isLeft) Color.Blue else Color.Red)
    } else {
        Modifier.shadow(10.dp, CircleShape, ambientColor = Color.Black, spotColor = Color.Black)
    }

    Box(
        modifier = modifier
            .size(150.dp)
            .then(rgbShadow)
            .clip(CircleShape)
            .background(baseGradient)
            .pointerInput(isLeft, isCameraMode, cameraSensitivity, density) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startTime = System.currentTimeMillis()
                    var previousTouchX = down.position.x
                    var previousTouchY = down.position.y
                    var previousTimeMs = startTime
                    var currentStickX = 0f
                    var currentStickY = 0f
                    var lastSpeed = 0f
                    val velocityBuffer = VelocityRingBuffer(8)

                    if (isCameraMode) {
                        // ── Right Stick Camera Mode: Pure relative touch ──
                        viewModel.updateRightStick(0f, 0f)
                    }

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id }
                        if (change == null || !change.pressed) break

                        val currentTouchX = change.position.x
                        val currentTouchY = change.position.y
                        val deltaX = currentTouchX - previousTouchX
                        val deltaY = currentTouchY - previousTouchY

                        previousTouchX = currentTouchX
                        previousTouchY = currentTouchY
                        change.consume()

                        if (isCameraMode) {
                            // ── Right Stick Camera Mode: Density-Independent Gaming Ballistics ──
                            var finalDeltaX = deltaX
                            var finalDeltaY = deltaY
                            val absX = kotlin.math.abs(deltaX)
                            val absY = kotlin.math.abs(deltaY)
                            if (absX > 3.0f * absY) {
                                finalDeltaY *= 0.5f // Suppress vertical wobble during horizontal turns
                            } else if (absY > 3.0f * absX) {
                                finalDeltaX *= 0.5f // Suppress horizontal wobble during vertical looks
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

                                val stickMagnitude = calculateGamingStickMagnitude(smoothedSpeed, cameraSensitivity)

                                if (stickMagnitude > 0f) {
                                    val dirX = finalDeltaX / distPx
                                    val dirY = finalDeltaY / distPx

                                    val targetStickX = (dirX * stickMagnitude).coerceIn(-1f, 1f)
                                    val targetStickY = (-dirY * stickMagnitude).coerceIn(-1f, 1f) // Up is positive Y

                                    // Smooth response (EMA) — heavier smoothing absorbs remaining per-frame noise
                                    currentStickX = 0.55f * targetStickX + 0.45f * currentStickX
                                    currentStickY = 0.55f * targetStickY + 0.45f * currentStickY

                                    viewModel.updateRightStick(currentStickX, currentStickY)

                                    // VISUAL THUMB KNOB DEFLECTS PROPORTIONAL TO SPEED AND DIRECTION!
                                    thumbOffsetX = (currentStickX * maxRadius).coerceIn(-maxRadius, maxRadius)
                                    thumbOffsetY = (-currentStickY * maxRadius).coerceIn(-maxRadius, maxRadius)

                                    // Adaptive stationary watchdog: timeout scales with speed
                                    val decayTimeoutMs = (120L - (smoothedSpeed / 20f).toLong()).coerceIn(50L, 120L)
                                    decayJob?.cancel()
                                    decayJob = coroutineScope.launch {
                                        delay(decayTimeoutMs.milliseconds)
                                        // 3-stage gentle decay
                                        currentStickX *= 0.5f
                                        currentStickY *= 0.5f
                                        viewModel.updateRightStick(currentStickX, currentStickY)
                                        thumbOffsetX = (currentStickX * maxRadius)
                                        thumbOffsetY = (-currentStickY * maxRadius)
                                        delay(30.milliseconds)
                                        currentStickX *= 0.2f
                                        currentStickY *= 0.2f
                                        viewModel.updateRightStick(currentStickX, currentStickY)
                                        thumbOffsetX = (currentStickX * maxRadius)
                                        thumbOffsetY = (-currentStickY * maxRadius)
                                        delay(25.milliseconds)
                                        currentStickX = 0f
                                        currentStickY = 0f
                                        viewModel.updateRightStick(0f, 0f)
                                        thumbOffsetX = 0f
                                        thumbOffsetY = 0f
                                        lastSpeed = 0f
                                    }
                                }
                            }
                        } else {
                            // ── Traditional analog stick steering ──
                            var newX = thumbOffsetX + deltaX
                            var newY = thumbOffsetY + deltaY
                            val distance = hypot(newX, newY)

                            if (distance > maxRadius) {
                                val angle = atan2(newY, newX)
                                newX = cos(angle) * maxRadius
                                newY = sin(angle) * maxRadius
                            }

                            thumbOffsetX = newX
                            thumbOffsetY = newY

                            val normalizedX = (newX / maxRadius).coerceIn(-1f, 1f)
                            val normalizedY = (-newY / maxRadius).coerceIn(-1f, 1f) // Invert Y so up is positive

                            if (isLeft) {
                                viewModel.updateLeftStick(normalizedX, normalizedY)
                            } else {
                                viewModel.updateRightStick(normalizedX, normalizedY)
                            }
                        }
                    }

                    // On finger lift / cancellation
                    decayJob?.cancel()
                    if (isLeft) {
                        thumbOffsetX = 0f
                        thumbOffsetY = 0f
                        viewModel.updateLeftStick(0f, 0f)
                    } else {
                        val releaseSpeed = velocityBuffer.peakSpeed()
                        if (isCameraMode && releaseSpeed > 400f) {
                            val coastSteps = ((releaseSpeed / 200f).toInt()).coerceIn(3, 7)
                            coroutineScope.launch {
                                var coastX = currentStickX
                                var coastY = currentStickY
                                repeat(coastSteps) {
                                    coastX *= 0.68f
                                    coastY *= 0.68f
                                    viewModel.updateRightStick(coastX, coastY)
                                    thumbOffsetX = (coastX * maxRadius)
                                    thumbOffsetY = (-coastY * maxRadius)
                                    delay(16.milliseconds)
                                }
                                viewModel.updateRightStick(0f, 0f)
                                thumbOffsetX = 0f
                                thumbOffsetY = 0f
                            }
                        } else {
                            thumbOffsetX = 0f
                            thumbOffsetY = 0f
                            viewModel.updateRightStick(0f, 0f)
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Inner depth shadow
        Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
            drawCircle(
                color = Color.Black.copy(alpha = 0.8f),
                radius = size.minDimension / 2f
            )
        }

        // Thumbstick Cap
        Box(
            modifier = Modifier
                .offset { IntOffset(thumbOffsetX.roundToInt(), thumbOffsetY.roundToInt()) }
                .size(90.dp)
                .shadow(12.dp, CircleShape)
                .clip(CircleShape)
                .background(thumbGradient)
        ) {
            // Thumbstick texture rings
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color.Black.copy(alpha = 0.3f),
                    radius = size.minDimension / 2.5f,
                    style = Stroke(width = 6f)
                )
                drawCircle(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.White.copy(alpha = 0.2f), Color.Transparent)
                    ),
                    radius = size.minDimension / 2.1f,
                    style = Stroke(width = 2f)
                )

                if (isCameraMode) {
                    // Subtle central look reticle dot in camera mode
                    drawCircle(
                        color = Color(0xFFFF007F).copy(alpha = 0.45f),
                        radius = 5f,
                        center = Offset(size.width / 2f, size.height / 2f)
                    )
                }
            }
        }
    }
}

/**
 * Dedicated standalone Thumbstick Button (LSB / RSB or L3 / R3).
 * Provides an ergonomic, tactile direct-click button with an authentic console thumbstick cap design:
 * outer knurled grip ring, concave thumb dish, radial lighting, and haptic feedback.
 */
@Composable
fun RealisticStickButton(
    isLeft: Boolean,
    key: String = if (isLeft) "LSB" else "RSB",
    isConnected: Boolean,
    onVibrate: () -> Unit,
    viewModel: GamepadViewModel,
    isRgbEnabled: Boolean,
    modifier: Modifier = Modifier,
    displayLabel: String? = null
) {
    var isPressed by remember { mutableStateOf(false) }

    val baseGradient = Brush.radialGradient(
        colors = listOf(
            if (isPressed) Color(0xFF222222) else Color(0xFF333333),
            if (isPressed) Color(0xFF0D0D0D) else Color(0xFF141414)
        ),
        center = Offset(0.35f, 0.35f),
        radius = 160f
    )

    val dishGradient = Brush.radialGradient(
        colors = listOf(
            if (isPressed) Color(0xFF111111) else Color(0xFF252525),
            if (isPressed) Color(0xFF000000) else Color(0xFF121212)
        ),
        center = Offset(0.5f, 0.5f),
        radius = 90f
    )

    val accentColor = if (isLeft) Color.Cyan else Color(0xFFFF007F)

    val rgbShadow = if (isRgbEnabled) {
        Modifier.shadow(
            elevation = if (isPressed) 18.dp else 10.dp,
            shape = CircleShape,
            ambientColor = accentColor,
            spotColor = accentColor
        )
    } else {
        Modifier.shadow(
            elevation = if (isPressed) 3.dp else 8.dp,
            shape = CircleShape,
            ambientColor = Color.Black,
            spotColor = Color.Black
        )
    }

    Box(
        modifier = modifier
            .size(70.dp)
            .then(rgbShadow)
            .clip(CircleShape)
            .background(baseGradient)
            .pointerInput(key) {
                detectTapGestures(
                    onPress = {
                        if (isConnected) onVibrate()
                        isPressed = true
                        viewModel.updateButton(key, true)
                        tryAwaitRelease()
                        isPressed = false
                        viewModel.updateButton(key, false)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Outer knurled ring texture + 3D bevel stroke
        Canvas(modifier = Modifier.fillMaxSize()) {
            val minDim = size.minDimension
            // Outer bevel rim
            drawCircle(
                color = Color.White.copy(alpha = if (isPressed) 0.15f else 0.35f),
                radius = minDim / 2f - 2f,
                style = Stroke(width = 3f)
            )

            // Dashed knurled grip ring
            drawCircle(
                color = Color.Black.copy(alpha = 0.6f),
                radius = minDim / 2f - 6f,
                style = Stroke(
                    width = 3.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                )
            )

            // Inner concave dish shadow ring
            drawCircle(
                color = accentColor.copy(alpha = if (isPressed) 0.8f else 0.35f),
                radius = minDim / 2f - 12f,
                style = Stroke(width = 2f)
            )
        }

        // Inner concave thumb dish
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(dishGradient),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = displayLabel ?: key,
                color = if (isPressed) Color.White else accentColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
        }
    }
}

