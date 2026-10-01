package com.sanket.tools.nexpad.ui.components.controller

import android.content.Context
import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.graphicsLayer
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

    val rgbShadow = Modifier.shadow(
        elevation = 10.dp,
        shape = CircleShape,
        ambientColor = Color.Black.copy(alpha = 0.40f),
        spotColor = Color.Black.copy(alpha = 0.55f)
    )

    Box(
        modifier = modifier
            .size(150.dp)
            .then(rgbShadow)
            .clip(CircleShape)
            .background(baseGradient)
            .border(
                width = 2.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF4A5568).copy(alpha = 0.6f),
                        Color(0xFF1A202C).copy(alpha = 0.9f)
                    )
                ),
                shape = CircleShape
            )
            .pointerInput(isLeft, isCameraMode, cameraSensitivity, density) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f
                    val deadzoneRadius = 6f

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
                    } else {
                        // ── Traditional analog stick steering: Direct vector from center to finger ──
                        val downVecX = down.position.x - centerX
                        val downVecY = down.position.y - centerY
                        val downDist = hypot(downVecX, downVecY)

                        val (clampedX, clampedY) = if (downDist > maxRadius) {
                            val angle = atan2(downVecY, downVecX)
                            Pair(cos(angle) * maxRadius, sin(angle) * maxRadius)
                        } else {
                            Pair(downVecX, downVecY)
                        }

                        thumbOffsetX = clampedX
                        thumbOffsetY = clampedY

                        val normX = if (downDist < deadzoneRadius) 0f else (clampedX / maxRadius).coerceIn(-1f, 1f)
                        val normY = if (downDist < deadzoneRadius) 0f else (-clampedY / maxRadius).coerceIn(-1f, 1f)

                        if (isLeft) {
                            viewModel.updateLeftStick(normX, normY)
                        } else {
                            viewModel.updateRightStick(normX, normY)
                        }
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
                            // ── Traditional analog stick steering: Direct vector from center to current touch ──
                            val vecX = change.position.x - centerX
                            val vecY = change.position.y - centerY
                            val dist = hypot(vecX, vecY)

                            val (clampedX, clampedY) = if (dist > maxRadius) {
                                val angle = atan2(vecY, vecX)
                                Pair(cos(angle) * maxRadius, sin(angle) * maxRadius)
                            } else {
                                Pair(vecX, vecY)
                            }

                            thumbOffsetX = clampedX
                            thumbOffsetY = clampedY

                            val normX = if (dist < deadzoneRadius) 0f else (clampedX / maxRadius).coerceIn(-1f, 1f)
                            val normY = if (dist < deadzoneRadius) 0f else (-clampedY / maxRadius).coerceIn(-1f, 1f) // Invert Y so up is positive

                            if (isLeft) {
                                viewModel.updateLeftStick(normX, normY)
                            } else {
                                viewModel.updateRightStick(normX, normY)
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
                .border(
                    width = 1.5.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF718096).copy(alpha = 0.50f), Color(0xFF1A202C))
                    ),
                    shape = CircleShape
                )
        ) {
            // Thumbstick texture rings & 4 ergonomic directional grip nibs
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val minDim = size.minDimension

                // Outer knurled grip ring
                drawCircle(
                    color = Color.Black.copy(alpha = 0.45f),
                    radius = minDim / 2.3f,
                    style = Stroke(
                        width = 4f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f)
                    )
                )

                // Inner concave dish shadow ring
                drawCircle(
                    color = Color.Black.copy(alpha = 0.60f),
                    radius = minDim / 2.7f,
                    style = Stroke(width = 5f)
                )

                // Top specular highlight arc
                drawCircle(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.25f), Color.Transparent)
                    ),
                    radius = minDim / 2.1f,
                    style = Stroke(width = 1.5f)
                )

                // 4 Ergonomic Directional Grip Nibs (12, 3, 6, 9 o'clock)
                val nibDist = minDim / 2.9f
                val nibRadius = 2.5f
                val nibColor = Color.White.copy(alpha = 0.25f)
                drawCircle(color = nibColor, radius = nibRadius, center = Offset(center.x, center.y - nibDist))
                drawCircle(color = nibColor, radius = nibRadius, center = Offset(center.x, center.y + nibDist))
                drawCircle(color = nibColor, radius = nibRadius, center = Offset(center.x - nibDist, center.y))
                drawCircle(color = nibColor, radius = nibRadius, center = Offset(center.x + nibDist, center.y))

                if (isCameraMode) {
                    // Subtle central look reticle dot in camera mode
                    drawCircle(
                        color = Color(0xFFFF007F).copy(alpha = 0.65f),
                        radius = 4f,
                        center = center
                    )
                }
            }
        }
    }
}

/**
 * Dedicated standalone Thumbstick Button (LSB / RSB or L3 / R3).
 * Provides an ergonomic, tactile direct-click button with an authentic console thumbstick cap design:
 * outer knurled grip ring, concave thumb dish, radial lighting, spring kinematics, and haptic feedback.
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

    val scaleAnim by animateFloatAsState(
        targetValue = if (isPressed) 0.89f else 1.0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 750f),
        label = "stick_btn_scale"
    )
    val pressOffsetYAnim by animateFloatAsState(
        targetValue = if (isPressed) 3.5f else 0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 750f),
        label = "stick_btn_offset"
    )

    val currentOnVibrate by rememberUpdatedState(onVibrate)
    val currentViewModel by rememberUpdatedState(viewModel)

    val baseGradient = remember(isPressed) {
        Brush.radialGradient(
            colors = listOf(
                if (isPressed) Color(0xFF222834) else Color(0xFF333E4D),
                if (isPressed) Color(0xFF0D1017) else Color(0xFF141923)
            ),
            center = Offset(0.35f, 0.35f),
            radius = 160f
        )
    }

    val dishGradient = remember(isPressed) {
        Brush.radialGradient(
            colors = listOf(
                if (isPressed) Color(0xFF10141C) else Color(0xFF212836),
                if (isPressed) Color(0xFF000000) else Color(0xFF0F131A)
            ),
            center = Offset(0.5f, 0.5f),
            radius = 90f
        )
    }

    val accentColor = if (isLeft) Color.Cyan else Color(0xFFFF007F)

    val rgbShadow = Modifier.shadow(
        elevation = if (isPressed) 3.dp else 8.dp,
        shape = CircleShape,
        ambientColor = Color.Black.copy(alpha = 0.40f),
        spotColor = Color.Black.copy(alpha = 0.55f)
    )

    Box(
        modifier = modifier
            .size(70.dp)
            .graphicsLayer {
                scaleX = scaleAnim
                scaleY = scaleAnim
            }
            .offset { IntOffset(0, pressOffsetYAnim.dp.roundToPx()) }
            .then(rgbShadow)
            .clip(CircleShape)
            .background(baseGradient)
            .border(
                width = 1.5.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF718096).copy(alpha = if (isPressed) 0.35f else 0.70f),
                        Color(0xFF1A202C)
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

