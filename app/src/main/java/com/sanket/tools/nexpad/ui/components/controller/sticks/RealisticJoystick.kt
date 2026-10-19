package com.sanket.tools.nexpad.ui.components.controller.sticks

import com.sanket.tools.nexpad.ui.components.controller.special.VelocityRingBuffer
import com.sanket.tools.nexpad.ui.components.controller.special.calculateGamingStickMagnitude
import com.sanket.tools.nexpad.ui.theme.NeonPalette

import android.content.Context
import android.util.Log
import androidx.compose.animation.core.Animatable
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
import androidx.compose.ui.draw.drawBehind
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
    isConnected: Boolean = true,
    viewModel: GamepadViewModel? = null,
    isRgbEnabled: Boolean = true,
    isLocked: Boolean = true,
    isInteractive: Boolean = true,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current.density
    val currentViewModel by rememberUpdatedState(viewModel)

    val animOffsetX = remember { Animatable(0f) }
    val animOffsetY = remember { Animatable(0f) }
    var decayJob by remember { mutableStateOf<Job?>(null) }
    var coastJob by remember { mutableStateOf<Job?>(null) }
    val maxRadius = 80f
    val tapThresholdPx = with(LocalDensity.current) { 8.dp.toPx() }
    val stickKey = remember(isLeft) { if (isLeft) "LSB" else "RSB" }

    val context = LocalContext.current
    val (isCameraMode, cameraSensitivity) = remember(context, isLeft) {
        if (isLeft) {
            Pair(false, 1.0f)
        } else {
            val prefs = com.sanket.tools.nexpad.storage.NexpadPreferences.getInstance(context)
            Pair(
                prefs.isRightStickCameraMode,
                prefs.cameraSensitivity
            )
        }
    }

    val baseGradient = Brush.radialGradient(
        colors = listOf(NeonPalette.MetalSubtle, NeonPalette.MetalAbyss),
        center = Offset(0.5f, 0.5f),
        radius = 250f
    )

    val thumbGradient = Brush.radialGradient(
        colors = listOf(NeonPalette.MetalSteel, NeonPalette.MetalMid),
        center = Offset(0.3f, 0.3f),
        radius = 150f
    )

    val neonColor = if (isLeft) NeonPalette.Cyan else NeonPalette.Magenta
    val stickX = if (viewModel != null) (if (isLeft) viewModel.leftStickX else viewModel.rightStickX) else 0f
    val stickY = if (viewModel != null) (if (isLeft) viewModel.leftStickY else viewModel.rightStickY) else 0f
    val effX = if (isLocked) animOffsetX.value else stickX * maxRadius
    val effY = if (isLocked) animOffsetY.value else -stickY * maxRadius
    val deflectionFraction = (hypot(effX, effY) / maxRadius).coerceIn(0f, 1f)
    val rgbBloomAlpha by animateFloatAsState(
        targetValue = 0.40f + 0.55f * deflectionFraction,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 800f),
        label = "realistic_joystick_bloom"
    )

    val rgbShadow = Modifier.shadow(
        elevation = 10.dp,
        shape = CircleShape,
        ambientColor = if (isRgbEnabled) neonColor.copy(alpha = 0.5f) else Color.Black,
        spotColor = if (isRgbEnabled) neonColor else Color.Black
    )

    Box(
        modifier = modifier
            .size(150.dp)
            .graphicsLayer {
                // Root RenderNode isolation
            }
            .drawBehind {
                if (isRgbEnabled) {
                    val capPos = Offset(center.x + effX, center.y + effY)
                    val disp = hypot(effX, effY)
                    val maxR = size.minDimension * 0.35f
                    val dispFraction = if (maxR > 0f) (disp / maxR).coerceIn(0f, 1f) else 0f

                    // 1. Base socket containment bloom
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                neonColor.copy(alpha = rgbBloomAlpha * 0.35f),
                                neonColor.copy(alpha = rgbBloomAlpha * 0.12f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = size.minDimension * 0.55f
                        ),
                        radius = size.minDimension * 0.55f,
                        center = center
                    )

                    // 2. Deflection comet-plume trailing wake aura
                    val plumeCenter = Offset(
                        center.x + effX * 0.65f,
                        center.y + effY * 0.65f
                    )
                    val plumeRadius = size.minDimension * (0.40f + 0.35f * dispFraction)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0.00f to (if (dispFraction > 0.4f) Color.White else neonColor).copy(alpha = rgbBloomAlpha * (0.50f + 0.35f * dispFraction)),
                                0.40f to neonColor.copy(alpha = rgbBloomAlpha * (0.30f + 0.25f * dispFraction)),
                                1.00f to Color.Transparent
                            ),
                            center = plumeCenter,
                            radius = plumeRadius
                        ),
                        radius = plumeRadius,
                        center = plumeCenter
                    )

                    // 3. High-energy thumbstick cap beacon bloom
                    val capBloomR = 48.dp.toPx() + (12.dp.toPx() * dispFraction)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                (if (dispFraction > 0.6f) Color.White else neonColor).copy(alpha = rgbBloomAlpha * 0.70f),
                                neonColor.copy(alpha = rgbBloomAlpha * 0.25f),
                                Color.Transparent
                            ),
                            center = capPos,
                            radius = capBloomR
                        ),
                        radius = capBloomR,
                        center = capPos
                    )
                }
            }
            .then(rgbShadow)
            .clip(CircleShape)
            .background(baseGradient)
            .border(
                width = 2.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        NeonPalette.CarbonBorder.copy(alpha = 0.6f),
                        NeonPalette.CarbonRim.copy(alpha = 0.9f)
                    )
                ),
                shape = CircleShape
            )
            .then(
                if (isInteractive) {
                    Modifier.pointerInput(isConnected, isLocked, isLeft, isCameraMode, cameraSensitivity, density, maxRadius, tapThresholdPx) {
                if (!isLocked) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    decayJob?.cancel()
                    coastJob?.cancel()
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f
                    val deadzoneRadius = 6f

                    val startTime = System.currentTimeMillis()
                    var maxDragDistance = 0f
                    var previousTouchX = down.position.x
                    var previousTouchY = down.position.y
                    var previousTimeMs = startTime
                    var currentStickX = 0f
                    var currentStickY = 0f
                    var lastSpeed = 0f
                    val velocityBuffer = VelocityRingBuffer(8)

                    val initialDownVecX = down.position.x - centerX
                    val initialDownVecY = down.position.y - centerY
                    val initialDist = hypot(initialDownVecX, initialDownVecY)
                    maxDragDistance = initialDist

                    if (isCameraMode) {
                        // ── Right Stick Camera Mode: Pure relative touch ──
                        currentViewModel?.updateRightStick(0f, 0f)
                    } else {
                        // ── Traditional analog stick steering: Direct vector from center to finger ──
                        val (clampedX, clampedY) = if (initialDist > maxRadius) {
                            val angle = atan2(initialDownVecY, initialDownVecX)
                            Pair(cos(angle) * maxRadius, sin(angle) * maxRadius)
                        } else {
                            Pair(initialDownVecX, initialDownVecY)
                        }

                        coroutineScope.launch {
                            animOffsetX.snapTo(clampedX)
                            animOffsetY.snapTo(clampedY)
                        }

                        val normX = if (initialDist < deadzoneRadius) 0f else (clampedX / maxRadius).coerceIn(-1f, 1f)
                        val normY = if (initialDist < deadzoneRadius) 0f else (-clampedY / maxRadius).coerceIn(-1f, 1f)

                        if (isLeft) {
                            currentViewModel?.updateLeftStick(normX, normY)
                        } else {
                            currentViewModel?.updateRightStick(normX, normY)
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

                                    currentViewModel?.updateRightStick(currentStickX, currentStickY)

                                    // VISUAL THUMB KNOB DEFLECTS PROPORTIONAL TO SPEED AND DIRECTION!
                                    val stickDist = hypot(currentStickX, currentStickY)
                                    val scale = if (stickDist > 1f) 1f / stickDist else 1f
                                    val targetOffsetX = currentStickX * scale * maxRadius
                                    val targetOffsetY = -currentStickY * scale * maxRadius
                                    coroutineScope.launch {
                                        animOffsetX.snapTo(targetOffsetX)
                                        animOffsetY.snapTo(targetOffsetY)
                                    }

                                    // Adaptive stationary watchdog: timeout scales with speed
                                    val decayTimeoutMs = (120L - (smoothedSpeed / 20f).toLong()).coerceIn(50L, 120L)
                                    decayJob?.cancel()
                                    decayJob = coroutineScope.launch {
                                        delay(decayTimeoutMs.milliseconds)
                                        // 3-stage gentle decay
                                        currentStickX *= 0.5f
                                        currentStickY *= 0.5f
                                        currentViewModel?.updateRightStick(currentStickX, currentStickY)
                                        animOffsetX.snapTo(currentStickX * maxRadius)
                                        animOffsetY.snapTo(-currentStickY * maxRadius)
                                        delay(30.milliseconds)
                                        currentStickX *= 0.2f
                                        currentStickY *= 0.2f
                                        currentViewModel?.updateRightStick(currentStickX, currentStickY)
                                        animOffsetX.snapTo(currentStickX * maxRadius)
                                        animOffsetY.snapTo(-currentStickY * maxRadius)
                                        delay(25.milliseconds)
                                        currentStickX = 0f
                                        currentStickY = 0f
                                        currentViewModel?.updateRightStick(0f, 0f)
                                        animOffsetX.snapTo(0f)
                                        animOffsetY.snapTo(0f)
                                        lastSpeed = 0f
                                    }
                                }
                            }
                        } else {
                            // ── Traditional analog stick steering: Direct vector from center to current touch ──
                            val vecX = change.position.x - centerX
                            val vecY = change.position.y - centerY
                            val dist = hypot(vecX, vecY)
                            if (dist > maxDragDistance) maxDragDistance = dist

                            val (clampedX, clampedY) = if (dist > maxRadius) {
                                val angle = atan2(vecY, vecX)
                                Pair(cos(angle) * maxRadius, sin(angle) * maxRadius)
                            } else {
                                Pair(vecX, vecY)
                            }

                            coroutineScope.launch {
                                animOffsetX.snapTo(clampedX)
                                animOffsetY.snapTo(clampedY)
                            }

                            val normX = if (dist < deadzoneRadius) 0f else (clampedX / maxRadius).coerceIn(-1f, 1f)
                            val normY = if (dist < deadzoneRadius) 0f else (-clampedY / maxRadius).coerceIn(-1f, 1f) // Invert Y so up is positive

                            if (isLeft) {
                                currentViewModel?.updateLeftStick(normX, normY)
                            } else {
                                currentViewModel?.updateRightStick(normX, normY)
                            }
                        }
                    }

                    // Release handling: check stationary tap (L3/R3 click) vs drag release
                    val touchDuration = System.currentTimeMillis() - startTime
                    if (maxDragDistance <= tapThresholdPx && touchDuration < 300L) {
                        currentViewModel?.updateButton(stickKey, true)
                        coroutineScope.launch {
                            delay(60.milliseconds)
                            currentViewModel?.updateButton(stickKey, false)
                        }
                    }

                    // On finger lift / cancellation
                    decayJob?.cancel()
                    coastJob?.cancel()
                    if (isLeft) {
                        currentViewModel?.updateLeftStick(0f, 0f)
                        coroutineScope.launch {
                            launch { animOffsetX.animateTo(0f, spring(dampingRatio = 0.65f, stiffness = 420f)) }
                            launch { animOffsetY.animateTo(0f, spring(dampingRatio = 0.65f, stiffness = 420f)) }
                        }
                    } else {
                        val releaseSpeed = velocityBuffer.peakSpeed()
                        if (isCameraMode && releaseSpeed > 400f) {
                            val coastSteps = ((releaseSpeed / 200f).toInt()).coerceIn(3, 7)
                            coastJob = coroutineScope.launch {
                                var coastX = currentStickX
                                var coastY = currentStickY
                                repeat(coastSteps) {
                                    coastX *= 0.68f
                                    coastY *= 0.68f
                                    currentViewModel?.updateRightStick(coastX, coastY)
                                    animOffsetX.snapTo(coastX * maxRadius)
                                    animOffsetY.snapTo(-coastY * maxRadius)
                                    delay(16.milliseconds)
                                }
                                currentViewModel?.updateRightStick(0f, 0f)
                                animOffsetX.animateTo(0f, spring(dampingRatio = 0.65f, stiffness = 420f))
                                animOffsetY.animateTo(0f, spring(dampingRatio = 0.65f, stiffness = 420f))
                            }
                        } else {
                            currentViewModel?.updateRightStick(0f, 0f)
                            coroutineScope.launch {
                                launch { animOffsetX.animateTo(0f, spring(dampingRatio = 0.65f, stiffness = 420f)) }
                                launch { animOffsetY.animateTo(0f, spring(dampingRatio = 0.65f, stiffness = 420f)) }
                            }
                        }
                    }
                }
            }
        } else Modifier
    ),
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
                .offset { IntOffset(effX.roundToInt(), effY.roundToInt()) }
                .size(90.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = CircleShape,
                    ambientColor = if (isRgbEnabled) neonColor.copy(alpha = 0.5f) else Color.Black,
                    spotColor = if (isRgbEnabled) neonColor else Color.Black
                )
                .clip(CircleShape)
                .background(thumbGradient)
                .border(
                    width = 1.5.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(NeonPalette.CarbonMuted.copy(alpha = 0.50f), NeonPalette.CarbonRim)
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
                        color = NeonPalette.Magenta.copy(alpha = 0.65f),
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
    isConnected: Boolean = true,
    onVibrate: () -> Unit = {},
    viewModel: GamepadViewModel? = null,
    isRgbEnabled: Boolean = true,
    modifier: Modifier = Modifier,
    displayLabel: String? = null,
    isInteractive: Boolean = true
) {
    var isPressed by remember { mutableStateOf(false) }

    val scaleAnimState = animateFloatAsState(
        targetValue = if (isPressed) 0.89f else 1.0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 750f),
        label = "stick_btn_scale"
    )
    val pressOffsetYAnimState = animateFloatAsState(
        targetValue = if (isPressed) 3.5f else 0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 750f),
        label = "stick_btn_offset"
    )

    val currentOnVibrate by rememberUpdatedState(onVibrate)
    val currentViewModel by rememberUpdatedState(viewModel)

    val baseGradientPressed = remember {
        Brush.radialGradient(
            colors = listOf(NeonPalette.CarbonPlate, NeonPalette.CarbonShadow),
            center = Offset(0.35f, 0.35f),
            radius = 160f
        )
    }
    val baseGradientNormal = remember {
        Brush.radialGradient(
            colors = listOf(NeonPalette.CarbonHighlight, NeonPalette.CarbonChassis),
            center = Offset(0.35f, 0.35f),
            radius = 160f
        )
    }

    val dishGradientPressed = remember {
        Brush.radialGradient(
            colors = listOf(NeonPalette.CarbonNavy, NeonPalette.PureBlack),
            center = Offset(0.5f, 0.5f),
            radius = 90f
        )
    }
    val dishGradientNormal = remember {
        Brush.radialGradient(
            colors = listOf(NeonPalette.CarbonPlate, NeonPalette.CarbonShadow),
            center = Offset(0.5f, 0.5f),
            radius = 90f
        )
    }

    val borderBrushPressed = remember {
        Brush.verticalGradient(
            colors = listOf(
                NeonPalette.CarbonMuted.copy(alpha = 0.35f),
                NeonPalette.CarbonRim
            )
        )
    }
    val borderBrushNormal = remember {
        Brush.verticalGradient(
            colors = listOf(
                NeonPalette.CarbonMuted.copy(alpha = 0.70f),
                NeonPalette.CarbonRim
            )
        )
    }

    val neonColor = if (isLeft) NeonPalette.Cyan else NeonPalette.Magenta
    val rgbBloomAlphaState = animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 0.45f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "stick_btn_rgb_bloom"
    )
    val accentColor = neonColor

    val density = LocalDensity.current.density
    val guideRingStroke = remember(density) {
        Stroke(
            width = 2.0f * density,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f * density, 4f * density), 0f)
        )
    }
    val bevelRimStroke = remember(density) { Stroke(width = 3f * density) }
    val knurledGripBtnStroke = remember(density) {
        Stroke(
            width = 3.5f * density,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f * density, 5f * density), 0f)
        )
    }
    val dishShadowStroke = remember(density) { Stroke(width = 2f * density) }

    val rgbShadow = Modifier.shadow(
        elevation = if (isPressed) 3.dp else 8.dp,
        shape = CircleShape,
        ambientColor = if (isRgbEnabled) neonColor else Color.Black,
        spotColor = if (isRgbEnabled) neonColor else Color.Black
    )

    val clickBloomStops = remember(neonColor) {
        arrayOf(
            0.00f to Color.Transparent,
            0.35f to Color.Transparent,
            0.75f to Color.Transparent,
            1.00f to Color.Transparent
        )
    }

    Box(
        modifier = modifier
            .size(70.dp)
            .graphicsLayer {
                // Root RenderNode isolation
            }
            .drawBehind {
                if (isRgbEnabled) {
                    val rgbBloomAlpha = rgbBloomAlphaState.value
                    val clickBloomR = size.minDimension * (if (isPressed) 1.05f else 0.85f)
                    // 1. Tactile click pulse bloom
                    clickBloomStops[0] = 0.00f to (if (isPressed) Color.White else neonColor).copy(alpha = rgbBloomAlpha * 0.65f)
                    clickBloomStops[1] = 0.35f to neonColor.copy(alpha = rgbBloomAlpha * 0.30f)
                    clickBloomStops[2] = 0.75f to neonColor.copy(alpha = rgbBloomAlpha * 0.10f)
                    clickBloomStops[3] = 1.00f to Color.Transparent
                    drawCircle(
                        brush = Brush.radialGradient(
                            colorStops = clickBloomStops,
                            center = center,
                            radius = clickBloomR
                        ),
                        radius = clickBloomR,
                        center = center
                    )

                    // 2. Tactile knurled guide ring
                    drawCircle(
                        color = neonColor.copy(alpha = if (isPressed) 0.60f else 0.25f),
                        radius = size.minDimension * 0.52f,
                        center = center,
                        style = guideRingStroke
                    )
                }
            }
            .then(rgbShadow)
            .graphicsLayer {
                scaleX = scaleAnimState.value
                scaleY = scaleAnimState.value
                translationY = pressOffsetYAnimState.value * density
            }
            .clip(CircleShape)
            .background(if (isPressed) baseGradientPressed else baseGradientNormal)
            .border(
                width = 1.5.dp,
                brush = if (isPressed) borderBrushPressed else borderBrushNormal,
                shape = CircleShape
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
        contentAlignment = Alignment.Center
    ) {
        // Outer knurled ring texture + 3D bevel stroke
        Canvas(modifier = Modifier.fillMaxSize()) {
            val minDim = size.minDimension
            // Outer bevel rim
            drawCircle(
                color = Color.White.copy(alpha = if (isPressed) 0.15f else 0.35f),
                radius = minDim / 2f - 2f,
                style = bevelRimStroke
            )

            // Dashed knurled grip ring
            drawCircle(
                color = Color.Black.copy(alpha = 0.6f),
                radius = minDim / 2f - 6f,
                style = knurledGripBtnStroke
            )

            // Inner concave dish shadow ring
            drawCircle(
                color = accentColor.copy(alpha = if (isPressed) 0.8f else 0.35f),
                radius = minDim / 2f - 12f,
                style = dishShadowStroke
            )
        }

        // Inner concave thumb dish
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (isPressed) dishGradientPressed else dishGradientNormal),
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

