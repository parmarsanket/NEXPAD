package com.sanket.tools.nexpad.ui.components.controller

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.time.Duration.Companion.milliseconds

/**
 * Console-grade Orb analog joystick natively translated from the optical reference:
 * - 150dp socket housing with #030304 -> #0a0b0c -> #1a1c1e gradient
 * - 12 graduation dial ticks (30° intervals)
 * - Dynamic deflection gate beam sweeping in real time along travel vector
 * - 2px perimeter neon ring (.lx-ring)
 * - 100dp spherical glass cap (.cap-orb) with dynamic opposite-casting 3D drop shadow
 * - Floating glowing liquid core (.orb-core) that lags inertially behind joystick deflection
 * - Inner containment neon ring (.orb-ring) with inner/outer glow bloom
 * - Dual spherical glass specular lens highlights (.orb-spec) with inverse light catching
 * - Damped return spring kinematics with zero-latency drag tracking
 * - Tap vs drag disambiguation (stationary tap triggers L3/R3 click)
 */
@Composable
fun OrbJoystick(
    isLeft: Boolean,
    isConnected: Boolean,
    viewModel: GamepadViewModel,
    isRgbEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current.density

    // Travel bounds matching reference: MAX = 24px in a 150px housing with 100px cap
    val maxTravelPx = with(LocalDensity.current) { 24.dp.toPx() }
    val tapThresholdPx = with(LocalDensity.current) { 4.dp.toPx() }

    // Instant finger tracking during drag + damped spring overshoot on release
    val animOffsetX = remember { Animatable(0f) }
    val animOffsetY = remember { Animatable(0f) }

    var isDragging by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }
    var decayJob by remember { mutableStateOf<Job?>(null) }

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

    // Default Orb glow: Teal #2FD4B6 (Left) or Hot Pink #FF3185 (Right)
    val glowColor = remember(isRgbEnabled, isLeft) {
        if (isRgbEnabled) {
            if (isLeft) Color(0xFF2FD4B6) else Color(0xFFFF3185)
        } else {
            if (isLeft) Color(0xFF2FD4B6) else Color(0xFFFF3185)
        }
    }

    // Micro-compression: scale down to 0.97 on press (--cs: 0.97)
    val capScaleAnim by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1.0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "orb_cap_scale"
    )

    // Dynamic core glow intensity: 0.70 idle, 1.00 when dragging/active
    val coreBloomAlpha by animateFloatAsState(
        targetValue = if (isDragging || isPressed) 1.0f else 0.70f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 800f),
        label = "orb_core_bloom"
    )

    // Ambient glow bloom behind housing
    val ambientBloomAlpha by animateFloatAsState(
        targetValue = if (isDragging || isPressed) 0.55f else 0.28f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 800f),
        label = "orb_ambient_bloom"
    )

    // Socket base radial gradient: #030304 -> #0a0b0c -> #1a1c1e
    val socketGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF030304),
                Color(0xFF0A0B0C),
                Color(0xFF1A1C1E)
            ),
            center = Offset(0.5f, 0.5f),
            radius = 280f
        )
    }

    // Orb glass cavity radial gradient: #15171a -> #060607
    val orbCavityGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF15171A),
                Color(0xFF060607)
            ),
            center = Offset(0.50f, 0.50f),
            radius = 220f
        )
    }

    val stickKey = remember(isLeft) { if (isLeft) "LSB" else "RSB" }

    // Floating liquid core inertial lag offset (-dxn * 13px, -dyn * 13px)
    val curNormX = (animOffsetX.value / maxTravelPx).coerceIn(-1f, 1f)
    val curNormY = (animOffsetY.value / maxTravelPx).coerceIn(-1f, 1f)

    val coreLagOffsetX by animateFloatAsState(
        targetValue = -curNormX * with(LocalDensity.current) { 13.dp.toPx() },
        animationSpec = spring(dampingRatio = 0.70f, stiffness = 320f),
        label = "orb_core_lag_x"
    )
    val coreLagOffsetY by animateFloatAsState(
        targetValue = -curNormY * with(LocalDensity.current) { 13.dp.toPx() },
        animationSpec = spring(dampingRatio = 0.70f, stiffness = 320f),
        label = "orb_core_lag_y"
    )

    Box(
        modifier = modifier
            .size(150.dp)
            // Ambient neon bloom behind housing
            .drawBehind {
                if (isRgbEnabled) {
                    drawCircle(
                        color = glowColor.copy(alpha = ambientBloomAlpha * 0.40f),
                        radius = size.minDimension / 2f + 10.dp.toPx()
                    )
                }
            }
            .shadow(
                elevation = 12.dp,
                shape = CircleShape,
                ambientColor = if (isRgbEnabled) glowColor.copy(alpha = 0.4f) else Color.Black,
                spotColor = if (isRgbEnabled) glowColor else Color.Black
            )
            .clip(CircleShape)
            .background(socketGradient)
            .border(
                width = 1.dp,
                color = Color.Black.copy(alpha = 0.60f),
                shape = CircleShape
            )
            .pointerInput(isLeft, isCameraMode, cameraSensitivity, density, maxTravelPx) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f
                    val deadzoneRadiusPx = 6.dp.toPx()

                    val startTime = System.currentTimeMillis()
                    var previousTouchX = down.position.x
                    var previousTouchY = down.position.y
                    var previousTimeMs = startTime
                    var currentStickX = 0f
                    var currentStickY = 0f
                    var maxDragDistance = 0f
                    val velocityBuffer = VelocityRingBuffer(8)

                    isDragging = true
                    isPressed = true

                    val initialDownVecX = down.position.x - centerX
                    val initialDownVecY = down.position.y - centerY
                    val initialDist = hypot(initialDownVecX, initialDownVecY)
                    maxDragDistance = initialDist

                    if (isCameraMode) {
                        viewModel.updateRightStick(0f, 0f)
                    } else {
                        val (clampedX, clampedY) = if (initialDist > maxTravelPx) {
                            val angle = atan2(initialDownVecY, initialDownVecX)
                            Pair(cos(angle) * maxTravelPx, sin(angle) * maxTravelPx)
                        } else {
                            Pair(initialDownVecX, initialDownVecY)
                        }

                        coroutineScope.launch {
                            animOffsetX.snapTo(clampedX)
                            animOffsetY.snapTo(clampedY)
                        }

                        val normX = if (initialDist < deadzoneRadiusPx) 0f else (clampedX / maxTravelPx).coerceIn(-1f, 1f)
                        val normY = if (initialDist < deadzoneRadiusPx) 0f else (-clampedY / maxTravelPx).coerceIn(-1f, 1f)

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
                            var finalDeltaX = deltaX
                            var finalDeltaY = deltaY
                            val absX = kotlin.math.abs(deltaX)
                            val absY = kotlin.math.abs(deltaY)
                            if (absX > 3.0f * absY) {
                                finalDeltaY *= 0.5f
                            } else if (absY > 3.0f * absX) {
                                finalDeltaX *= 0.5f
                            }

                            val distPx = hypot(finalDeltaX, finalDeltaY)
                            val distDp = distPx / density

                            if (distDp > 0.15f) {
                                val currentTimeMs = System.currentTimeMillis()
                                val dtSec = ((currentTimeMs - previousTimeMs).coerceAtLeast(1L)) / 1000f
                                previousTimeMs = currentTimeMs

                                velocityBuffer.push(distDp, dtSec)
                                val smoothedSpeed = velocityBuffer.averageSpeed()
                                val stickMagnitude = calculateGamingStickMagnitude(smoothedSpeed, cameraSensitivity)

                                if (stickMagnitude > 0f) {
                                    val dirX = finalDeltaX / distPx
                                    val dirY = finalDeltaY / distPx

                                    val targetStickX = (dirX * stickMagnitude).coerceIn(-1f, 1f)
                                    val targetStickY = (-dirY * stickMagnitude).coerceIn(-1f, 1f)

                                    currentStickX = 0.55f * targetStickX + 0.45f * currentStickX
                                    currentStickY = 0.55f * targetStickY + 0.45f * currentStickY

                                    viewModel.updateRightStick(currentStickX, currentStickY)

                                    val targetOffsetX = (currentStickX * maxTravelPx).coerceIn(-maxTravelPx, maxTravelPx)
                                    val targetOffsetY = (-currentStickY * maxTravelPx).coerceIn(-maxTravelPx, maxTravelPx)
                                    coroutineScope.launch {
                                        animOffsetX.snapTo(targetOffsetX)
                                        animOffsetY.snapTo(targetOffsetY)
                                    }

                                    val decayTimeoutMs = (120L - (smoothedSpeed / 20f).toLong()).coerceIn(50L, 120L)
                                    decayJob?.cancel()
                                    decayJob = coroutineScope.launch {
                                        delay(decayTimeoutMs.milliseconds)
                                        currentStickX *= 0.5f
                                        currentStickY *= 0.5f
                                        viewModel.updateRightStick(currentStickX, currentStickY)
                                        animOffsetX.snapTo(currentStickX * maxTravelPx)
                                        animOffsetY.snapTo(-currentStickY * maxTravelPx)
                                        delay(30.milliseconds)
                                        currentStickX *= 0.2f
                                        currentStickY *= 0.2f
                                        viewModel.updateRightStick(currentStickX, currentStickY)
                                        animOffsetX.snapTo(currentStickX * maxTravelPx)
                                        animOffsetY.snapTo(-currentStickY * maxTravelPx)
                                        delay(25.milliseconds)
                                        currentStickX = 0f
                                        currentStickY = 0f
                                        viewModel.updateRightStick(0f, 0f)
                                        animOffsetX.snapTo(0f)
                                        animOffsetY.snapTo(0f)
                                    }
                                }
                            }
                        } else {
                            val vecX = change.position.x - centerX
                            val vecY = change.position.y - centerY
                            val dist = hypot(vecX, vecY)
                            if (dist > maxDragDistance) maxDragDistance = dist

                            val (clampedX, clampedY) = if (dist > maxTravelPx) {
                                val angle = atan2(vecY, vecX)
                                Pair(cos(angle) * maxTravelPx, sin(angle) * maxTravelPx)
                            } else {
                                Pair(vecX, vecY)
                            }

                            coroutineScope.launch {
                                animOffsetX.snapTo(clampedX)
                                animOffsetY.snapTo(clampedY)
                            }

                            val normX = if (dist < deadzoneRadiusPx) 0f else (clampedX / maxTravelPx).coerceIn(-1f, 1f)
                            val normY = if (dist < deadzoneRadiusPx) 0f else (-clampedY / maxTravelPx).coerceIn(-1f, 1f)

                            if (isLeft) {
                                viewModel.updateLeftStick(normX, normY)
                            } else {
                                viewModel.updateRightStick(normX, normY)
                            }
                        }
                    }

                    // Release handling: check stationary tap (L3/R3 click) vs drag release
                    val touchDuration = System.currentTimeMillis() - startTime
                    if (maxDragDistance <= tapThresholdPx && touchDuration < 300L) {
                        viewModel.updateButton(stickKey, true)
                        coroutineScope.launch {
                            delay(60.milliseconds)
                            viewModel.updateButton(stickKey, false)
                        }
                    }

                    isDragging = false
                    isPressed = false
                    decayJob?.cancel()

                    if (isLeft) {
                        viewModel.updateLeftStick(0f, 0f)
                        coroutineScope.launch {
                            launch { animOffsetX.animateTo(0f, spring(dampingRatio = 0.65f, stiffness = 420f)) }
                            launch { animOffsetY.animateTo(0f, spring(dampingRatio = 0.65f, stiffness = 420f)) }
                        }
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
                                    animOffsetX.snapTo(coastX * maxTravelPx)
                                    animOffsetY.snapTo(-coastY * maxTravelPx)
                                    delay(16.milliseconds)
                                }
                                viewModel.updateRightStick(0f, 0f)
                                animOffsetX.animateTo(0f, spring(dampingRatio = 0.65f, stiffness = 420f))
                                animOffsetY.animateTo(0f, spring(dampingRatio = 0.65f, stiffness = 420f))
                            }
                        } else {
                            viewModel.updateRightStick(0f, 0f)
                            coroutineScope.launch {
                                launch { animOffsetX.animateTo(0f, spring(dampingRatio = 0.65f, stiffness = 420f)) }
                                launch { animOffsetY.animateTo(0f, spring(dampingRatio = 0.65f, stiffness = 420f)) }
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val curOffsetX = animOffsetX.value
        val curOffsetY = animOffsetY.value
        val curDist = hypot(curOffsetX, curOffsetY)
        val curMagnitude = min(1f, curDist / maxTravelPx)
        val curAngleDeg = (atan2(curOffsetX, -curOffsetY) * 180f / Math.PI.toFloat() + 360f) % 360f

        // Stationary Socket Background Canvas: Inset Depth, 12 Ticks, and Directional Gate Arc
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val r = size.minDimension / 2f
            val center = Offset(w / 2f, h / 2f)

            // Deep top inset shadow: inset 0 7px 14px rgba(0,0,0,0.95)
            val topShadowH = h * 0.38f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Black.copy(alpha = 0.95f), Color.Transparent),
                    startY = 0f,
                    endY = topShadowH
                ),
                topLeft = Offset.Zero,
                size = Size(w, topShadowH)
            )

            // Bottom specular rim: inset 0 -3px 5px rgba(255,255,255,0.025)
            val botRimH = h * 0.20f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.025f)),
                    startY = h - botRimH,
                    endY = h
                ),
                topLeft = Offset(0f, h - botRimH),
                size = Size(w, botRimH)
            )

            // 12 Graduation Ticks (.lx-ticks): spaced every 30° at radius r - 12dp to r - 18dp
            val tickOuterR = r - 12.dp.toPx()
            val tickInnerR = tickOuterR - 6.dp.toPx()
            val tickColor = Color.White.copy(alpha = 0.22f)
            for (i in 0 until 12) {
                val tickAngle = (i * 30.0 - 90.0) * (Math.PI / 180.0)
                val cosA = Math.cos(tickAngle).toFloat()
                val sinA = Math.sin(tickAngle).toFloat()
                drawLine(
                    color = tickColor,
                    start = Offset(center.x + cosA * tickInnerR, center.y + sinA * tickInnerR),
                    end = Offset(center.x + cosA * tickOuterR, center.y + sinA * tickOuterR),
                    strokeWidth = 1.8.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Directional Deflection Gate Arc (.lx-gate): 8px swept beam along travel vector
            if (curMagnitude > 0.04f) {
                val gateRadius = r - 3.dp.toPx() - 4.dp.toPx()
                val sweepAngle = 84f
                val startAngle = curAngleDeg - 90f - 42f
                val gateAlpha = (curMagnitude * 0.85f).coerceIn(0f, 0.85f)

                // Wide diffuse beam
                drawArc(
                    color = glowColor.copy(alpha = gateAlpha * 0.45f),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(center.x - gateRadius, center.y - gateRadius),
                    size = Size(gateRadius * 2f, gateRadius * 2f),
                    style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                )
                // Core beam
                drawArc(
                    color = glowColor.copy(alpha = gateAlpha),
                    startAngle = startAngle + 12f,
                    sweepAngle = sweepAngle - 24f,
                    useCenter = false,
                    topLeft = Offset(center.x - gateRadius, center.y - gateRadius),
                    size = Size(gateRadius * 2f, gateRadius * 2f),
                    style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Baseline dynamic drop shadow behind moving cap
            val capR = 50.dp.toPx()
            val shadowOffsetX = curOffsetX * -0.30f
            val shadowOffsetY = 9.dp.toPx() + curOffsetY * -0.30f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.80f),
                        Color.Black.copy(alpha = 0.40f),
                        Color.Transparent
                    ),
                    center = Offset(center.x + shadowOffsetX, center.y + shadowOffsetY),
                    radius = capR + 12.dp.toPx()
                ),
                center = Offset(center.x + shadowOffsetX, center.y + shadowOffsetY),
                radius = capR + 12.dp.toPx()
            )
        }

        // Moving Glass Spherical Cap (.cap-orb): 100dp circle with floating liquid core
        Box(
            modifier = Modifier
                .offset { IntOffset(curOffsetX.roundToInt(), curOffsetY.roundToInt()) }
                .size(100.dp)
                .graphicsLayer {
                    scaleX = capScaleAnim
                    scaleY = capScaleAnim
                }
                .clip(CircleShape)
                .background(orbCavityGradient)
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.06f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Cap Internal Well & Shadows
            Canvas(modifier = Modifier.fillMaxSize()) {
                val capW = size.width
                val capH = size.height

                // Top specular rim: inset 0 3px 3px rgba(255,255,255,0.10)
                drawArc(
                    color = Color.White.copy(alpha = 0.10f),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(1.5.dp.toPx(), 1.5.dp.toPx()),
                    size = Size(capW - 3.dp.toPx(), capH - 3.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx())
                )

                // Inset bottom shadow: inset 0 -8px 14px rgba(0,0,0,0.85)
                val botShadowH = capH * 0.40f
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                        startY = capH - botShadowH,
                        endY = capH
                    ),
                    topLeft = Offset(0f, capH - botShadowH),
                    size = Size(capW, botShadowH)
                )
            }

            // Glowing Liquid Core (.orb-core): 60dp with inertial liquid lag
            Box(
                modifier = Modifier
                    .offset { IntOffset(coreLagOffsetX.roundToInt(), coreLagOffsetY.roundToInt()) }
                    .size(60.dp)
                    .clip(CircleShape)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val coreR = size.minDimension / 2f
                    val coreCenter = Offset(size.width / 2f, size.height / 2f)

                    // Multi-pass diffuse glow simulating blur(4px)
                    // Diffuse outer halo
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                glowColor.copy(alpha = coreBloomAlpha * 0.35f),
                                Color.Transparent
                            ),
                            center = coreCenter,
                            radius = coreR
                        ),
                        center = coreCenter,
                        radius = coreR
                    )
                    // Mid-bloom
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                glowColor.copy(alpha = coreBloomAlpha * 0.65f),
                                glowColor.copy(alpha = coreBloomAlpha * 0.25f),
                                Color.Transparent
                            ),
                            center = coreCenter,
                            radius = coreR * 0.75f
                        ),
                        center = coreCenter,
                        radius = coreR * 0.75f
                    )
                    // Luminous central nucleus
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = coreBloomAlpha * 0.85f),
                                glowColor.copy(alpha = coreBloomAlpha * 0.95f),
                                Color.Transparent
                            ),
                            center = coreCenter,
                            radius = coreR * 0.45f
                        ),
                        center = coreCenter,
                        radius = coreR * 0.45f
                    )
                }
            }

            // Inner Orb Containment Ring (.orb-ring) & Specular Reflections (.orb-spec)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val capW = size.width
                val capH = size.height
                val capR = size.minDimension / 2f

                // Inner Containment Ring (.orb-ring): inset 5px, 2px border, opacity 0.5
                val orbRingRadius = capR - 5.dp.toPx()
                // Outer bloom
                drawCircle(
                    color = glowColor.copy(alpha = 0.25f),
                    radius = orbRingRadius,
                    style = Stroke(width = 6.dp.toPx())
                )
                // Inset bloom
                drawCircle(
                    color = glowColor.copy(alpha = 0.25f),
                    radius = orbRingRadius - 2.dp.toPx(),
                    style = Stroke(width = 4.dp.toPx())
                )
                // Core ring
                drawCircle(
                    color = glowColor.copy(alpha = 0.50f),
                    radius = orbRingRadius,
                    style = Stroke(width = 2.dp.toPx())
                )

                // Dual Spherical Lens Highlights (.orb-spec):
                // 1) Primary top-left light catch moving inversely:
                // ellipse 34% 20% at (34% - dxn * 14%, 22% - dyn * 14%)
                val normX = (curOffsetX / maxTravelPx).coerceIn(-1f, 1f)
                val normY = (curOffsetY / maxTravelPx).coerceIn(-1f, 1f)
                val spec1CenterX = capW * (0.34f - normX * 0.14f)
                val spec1CenterY = capH * (0.22f - normY * 0.14f)

                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.38f),
                            Color.White.copy(alpha = 0.15f),
                            Color.Transparent
                        ),
                        center = Offset(spec1CenterX, spec1CenterY),
                        radius = capW * 0.28f
                    ),
                    topLeft = Offset(spec1CenterX - capW * 0.22f, spec1CenterY - capH * 0.14f),
                    size = Size(capW * 0.44f, capH * 0.28f)
                )

                // 2) Secondary bottom rim specular bounce:
                // ellipse 40% 14% at 55% 94%
                val spec2CenterX = capW * 0.55f
                val spec2CenterY = capH * 0.94f
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.10f),
                            Color.Transparent
                        ),
                        center = Offset(spec2CenterX, spec2CenterY),
                        radius = capW * 0.25f
                    ),
                    topLeft = Offset(spec2CenterX - capW * 0.24f, spec2CenterY - capH * 0.10f),
                    size = Size(capW * 0.48f, capH * 0.20f)
                )
            }
        }

        // Top Overlay Canvas: Outer Neon Ring (.lx-ring, z-index: 6) & Glass Lens (.lx-lens, z-index: 8)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val r = size.minDimension / 2f

            // Outer Neon Ring (.lx-ring): inset 3px, 2px border, opacity 0.45
            val outerRingRadius = r - 3.dp.toPx()
            drawCircle(
                color = glowColor.copy(alpha = 0.22f),
                radius = outerRingRadius,
                style = Stroke(width = 6.dp.toPx())
            )
            drawCircle(
                color = glowColor.copy(alpha = 0.18f),
                radius = outerRingRadius - 2.dp.toPx(),
                style = Stroke(width = 3.dp.toPx())
            )
            drawCircle(
                color = glowColor.copy(alpha = 0.45f),
                radius = outerRingRadius,
                style = Stroke(width = 2.dp.toPx())
            )

            // Outer Glass Lens (.lx-lens): Top-lit 1px chamfer rim
            drawArc(
                color = Color.White.copy(alpha = 0.12f),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                size = Size(w - 2.dp.toPx(), h - 2.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )
            // Left subtle reflection
            drawArc(
                color = Color.White.copy(alpha = 0.06f),
                startAngle = 90f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                size = Size(w - 2.dp.toPx(), h - 2.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )
            // Bottom shadow chamfer
            drawArc(
                color = Color.Black.copy(alpha = 0.30f),
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                size = Size(w - 2.dp.toPx(), h - 2.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )
            // Lower-right glass specular sheen: at 70% 78%
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.06f), Color.Transparent),
                    center = Offset(w * 0.70f, h * 0.78f),
                    radius = w * 0.20f
                ),
                center = Offset(w * 0.70f, h * 0.78f),
                radius = w * 0.20f
            )
        }
    }
}

/**
 * Dedicated standalone Orb Thumbstick Click Button (LSB / RSB or L3 / R3).
 * Shares the identical Orb glass spherical design language:
 * - Spherical dark glass cavity (#15171a -> #060607)
 * - Centered floating glowing liquid core with radial gradient bloom
 * - Inner containment neon ring with bloom
 * - Inverse specular lens reflection
 * - Damped spring depression on tap (scales to 0.92, offsets 2dp, triggers haptics and game input)
 */
@Composable
fun OrbStickButton(
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
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "orb_stick_btn_scale"
    )
    val pressOffsetYAnim by animateFloatAsState(
        targetValue = if (isPressed) 2.0f else 0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "orb_stick_btn_offset"
    )
    val coreBloomAlpha by animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0.70f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 800f),
        label = "orb_stick_btn_bloom"
    )

    val currentOnVibrate by rememberUpdatedState(onVibrate)
    val currentViewModel by rememberUpdatedState(viewModel)

    val glowColor = remember(isRgbEnabled, isLeft) {
        if (isRgbEnabled) {
            if (isLeft) Color(0xFF2FD4B6) else Color(0xFFFF3185)
        } else {
            if (isLeft) Color(0xFF2FD4B6) else Color(0xFFFF3185)
        }
    }

    val orbCavityGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF15171A),
                Color(0xFF060607)
            ),
            center = Offset(0.50f, 0.50f),
            radius = 180f
        )
    }

    val labelText = displayLabel ?: (if (isLeft) "LSB" else "RSB")

    Box(
        modifier = modifier
            .size(70.dp)
            .drawBehind {
                if (isRgbEnabled) {
                    drawCircle(
                        color = glowColor.copy(alpha = coreBloomAlpha * 0.45f),
                        radius = size.minDimension / 2f + 6.dp.toPx()
                    )
                }
            }
            .graphicsLayer {
                scaleX = scaleAnim
                scaleY = scaleAnim
            }
            .offset { IntOffset(0, pressOffsetYAnim.dp.roundToPx()) }
            .shadow(
                elevation = if (isPressed) 2.dp else 8.dp,
                shape = CircleShape,
                spotColor = if (isRgbEnabled) glowColor else Color.Black,
                ambientColor = if (isRgbEnabled) glowColor.copy(alpha = 0.5f) else Color.Black
            )
            .clip(CircleShape)
            .background(orbCavityGradient)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
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
        // Internal Well Shadows, Liquid Core, and Specular Reflections
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val r = size.minDimension / 2f
            val center = Offset(w / 2f, h / 2f)

            // Top specular arc
            drawArc(
                color = Color.White.copy(alpha = if (isPressed) 0.08f else 0.16f),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                size = Size(w - 2.dp.toPx(), h - 2.dp.toPx()),
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Bottom inset shadow
            val botShadowH = h * 0.40f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = if (isPressed) 0.90f else 0.80f)),
                    startY = h - botShadowH,
                    endY = h
                ),
                topLeft = Offset(0f, h - botShadowH),
                size = Size(w, botShadowH)
            )

            // Liquid Core diffuse glow
            val coreR = r * 0.65f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glowColor.copy(alpha = coreBloomAlpha * 0.85f),
                        glowColor.copy(alpha = coreBloomAlpha * 0.30f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = coreR
                ),
                center = center,
                radius = coreR
            )

            // Inner Containment Ring
            val ringRadius = r - 4.dp.toPx()
            drawCircle(
                color = glowColor.copy(alpha = if (isPressed) 0.50f else 0.25f),
                radius = ringRadius,
                style = Stroke(width = 4.dp.toPx())
            )
            drawCircle(
                color = glowColor.copy(alpha = if (isPressed) 0.90f else 0.60f),
                radius = ringRadius,
                style = Stroke(width = 1.8.dp.toPx())
            )

            // Specular lens highlight
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.35f), Color.Transparent),
                    center = Offset(w * 0.36f, h * 0.24f),
                    radius = w * 0.28f
                ),
                topLeft = Offset(w * 0.16f, h * 0.10f),
                size = Size(w * 0.40f, h * 0.28f)
            )
        }

        // Center Label
        Text(
            text = labelText,
            color = if (isPressed) Color.White else glowColor,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}
