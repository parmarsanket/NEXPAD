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
import androidx.compose.ui.graphics.PathEffect
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
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.time.Duration.Companion.milliseconds

/**
 * Console-grade analog joystick (Flux architecture) with high-fidelity physical optics:
 * - 150dp deep socket well with top-lit recessed depth shadow
 * - 12 graduation dial ticks (30° intervals)
 * - Dynamic deflection gate beam sweeping in real time along travel vector
 * - 2px perimeter neon ring
 * - 96dp moving thumb cap with opposite-casting 3D drop shadow
 * - Inverse-tracking curved specular lens reflection
 * - 46dp concave center dish with neon glowing "L" / "R" glyph
 * - Damped return spring kinematics with zero-latency drag tracking
 * - Tap vs drag disambiguation (tap triggers L3/R3 click)
 */
@Composable
fun FluxJoystick(
    isLeft: Boolean,
    isConnected: Boolean,
    viewModel: GamepadViewModel,
    isRgbEnabled: Boolean,
    isLocked: Boolean = true,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current.density

    // Travel bounds matching CSS: MAX = 24px in a 150px housing with 96px cap
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

    // Default Flux glow: #2FD4B6 (Teal) or custom RGB neon
    val glowColor = remember(isRgbEnabled, isLeft) {
        if (isRgbEnabled) {
            if (isLeft) Color(0xFF2FD4B6) else Color(0xFFFF3185)
        } else {
            Color(0xFF2FD4B6)
        }
    }

    // Tactile micro-compression: scale down to 0.97 on press (matches --cs: 0.97)
    val capScaleAnim by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1.0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "flux_cap_scale"
    )

    // Dynamic neon bloom intensity on cap ring during active touch
    val capBloomAlpha by animateFloatAsState(
        targetValue = if (isDragging || isPressed) 1.0f else 0.70f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 800f),
        label = "flux_cap_bloom"
    )

    // Ambient glow bloom behind housing
    val ambientBloomAlpha by animateFloatAsState(
        targetValue = if (isDragging || isPressed) 0.55f else 0.28f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 800f),
        label = "flux_ambient_bloom"
    )

    // Socket base radial gradient: #030304 -> #090a0b -> #191b1e
    val socketGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF030304),
                Color(0xFF090A0B),
                Color(0xFF191B1E)
            ),
            center = Offset(0.5f, 0.5f),
            radius = 280f
        )
    }

    // Cap surface convex dome gradient: #3d4145 -> #202327 -> #111316 -> #050506
    val capDomeGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF3D4145),
                Color(0xFF202327),
                Color(0xFF111316),
                Color(0xFF050506)
            ),
            center = Offset(0.50f, 0.36f),
            radius = 220f
        )
    }

    // Concave center thumb dish gradient: #020203 -> #0b0c0d -> #161719
    val dishGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF020203),
                Color(0xFF0B0C0D),
                Color(0xFF161719)
            ),
            center = Offset(0.50f, 0.58f),
            radius = 100f
        )
    }

    val stickKey = remember(isLeft) { if (isLeft) "LSB" else "RSB" }
    val glyphLabel = remember(isLeft) { if (isLeft) "L" else "R" }

    val stickState = if (isLeft) viewModel.leftStickState else viewModel.rightStickState
    val effOffsetX = if (isLocked) animOffsetX.value else stickState.first * maxTravelPx
    val effOffsetY = if (isLocked) animOffsetY.value else -stickState.second * maxTravelPx

    val deflectionFraction = (hypot(effOffsetX, effOffsetY) / maxTravelPx).coerceIn(0f, 1f)
    val rgbBloomAlpha by animateFloatAsState(
        targetValue = 0.40f + 0.55f * deflectionFraction,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 800f),
        label = "flux_joystick_bloom"
    )

    Box(
        modifier = modifier
            .size(150.dp)
            .drawBehind {
                if (isRgbEnabled) {
                    val fluxRadius = size.minDimension * 0.52f
                    val curX = effOffsetX
                    val curY = effOffsetY
                    val curDist = hypot(curX, curY)
                    val defFraction = (curDist / maxTravelPx).coerceIn(0f, 1f)

                    // 1. Ambient reactor plasma core bloom
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                glowColor.copy(alpha = rgbBloomAlpha * 0.40f),
                                glowColor.copy(alpha = rgbBloomAlpha * 0.15f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = size.minDimension * 0.60f
                        ),
                        radius = size.minDimension * 0.60f,
                        center = center
                    )

                    // 2. 12-Point Reactor Flux Ring ticks
                    for (i in 0 until 12) {
                        val tickAngleRad = Math.toRadians((i * 30.0))
                        val cosA = cos(tickAngleRad).toFloat()
                        val sinA = sin(tickAngleRad).toFloat()
                        val innerR = fluxRadius - 4.dp.toPx()
                        val outerR = fluxRadius + 4.dp.toPx()
                        drawLine(
                            color = glowColor.copy(alpha = if (defFraction > 0.3f) 0.85f else 0.40f),
                            start = Offset(center.x + innerR * cosA, center.y + innerR * sinA),
                            end = Offset(center.x + outerR * cosA, center.y + outerR * sinA),
                            strokeWidth = 2.0f
                        )
                    }

                    // 3. Sweeping Magnetic Deflection Discharge Arc
                    if (defFraction > 0.08f) {
                        val defAngleDeg = Math.toDegrees(atan2(curY.toDouble(), curX.toDouble())).toFloat()
                        val arcSweep = 50f + 20f * defFraction
                        val arcStart = defAngleDeg - arcSweep / 2f

                        // Outer diffuse magnetic flare
                        drawArc(
                            brush = Brush.radialGradient(
                                colors = listOf(Color.White, glowColor, Color.Transparent),
                                center = Offset(center.x + curX * 0.5f, center.y + curY * 0.5f),
                                radius = size.minDimension * 0.65f
                            ),
                            startAngle = arcStart,
                            sweepAngle = arcSweep,
                            useCenter = false,
                            topLeft = Offset(center.x - fluxRadius - 6f, center.y - fluxRadius - 6f),
                            size = Size((fluxRadius + 6f) * 2f, (fluxRadius + 6f) * 2f),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 8.dp.toPx())
                        )
                        // Core electric arc line
                        drawArc(
                            color = Color.White.copy(alpha = 0.95f),
                            startAngle = arcStart,
                            sweepAngle = arcSweep,
                            useCenter = false,
                            topLeft = Offset(center.x - fluxRadius, center.y - fluxRadius),
                            size = Size(fluxRadius * 2f, fluxRadius * 2f),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f)
                        )
                    }
                }
            }
            .shadow(
                elevation = 12.dp,
                shape = CircleShape,
                ambientColor = if (isRgbEnabled) glowColor.copy(alpha = 0.5f) else Color.Black,
                spotColor = if (isRgbEnabled) glowColor else Color.Black
            )
            .clip(CircleShape)
            .background(socketGradient)
            // Housing 1px border: rgba(0, 0, 0, 0.75)
            .border(
                width = 1.dp,
                color = Color.Black.copy(alpha = 0.75f),
                shape = CircleShape
            )
            .pointerInput(isConnected, isLocked, isLeft, isCameraMode, cameraSensitivity, density, maxTravelPx, tapThresholdPx) {
                if (!isConnected || !isLocked) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f

                    val startTime = System.currentTimeMillis()
                    var previousTouchX = down.position.x
                    var previousTouchY = down.position.y
                    var previousTimeMs = startTime
                    var totalMovedDistance = 0f

                    var currentStickX = 0f
                    var currentStickY = 0f
                    val velocityBuffer = VelocityRingBuffer(8)

                    isDragging = true
                    isPressed = true

                    if (isCameraMode) {
                        viewModel.updateRightStick(0f, 0f)
                    } else {
                        val downVecX = down.position.x - centerX
                        val downVecY = down.position.y - centerY
                        val downDist = hypot(downVecX, downVecY)

                        val (clampedX, clampedY) = if (downDist > maxTravelPx) {
                            val angle = atan2(downVecY, downVecX)
                            Pair(cos(angle) * maxTravelPx, sin(angle) * maxTravelPx)
                        } else {
                            Pair(downVecX, downVecY)
                        }

                        coroutineScope.launch {
                            animOffsetX.snapTo(clampedX)
                            animOffsetY.snapTo(clampedY)
                        }

                        val normX = if (downDist < 4f) 0f else (clampedX / maxTravelPx).coerceIn(-1f, 1f)
                        val normY = if (downDist < 4f) 0f else (-clampedY / maxTravelPx).coerceIn(-1f, 1f)

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

                        val frameMoveDist = hypot(deltaX, deltaY)
                        totalMovedDistance += frameMoveDist

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

                            val normX = if (dist < 4f) 0f else (clampedX / maxTravelPx).coerceIn(-1f, 1f)
                            val normY = if (dist < 4f) 0f else (-clampedY / maxTravelPx).coerceIn(-1f, 1f)

                            if (isLeft) {
                                viewModel.updateLeftStick(normX, normY)
                            } else {
                                viewModel.updateRightStick(normX, normY)
                            }
                        }
                    }

                    // Touch Release: check stationary tap for L3 / R3 button click
                    isDragging = false
                    isPressed = false
                    decayJob?.cancel()

                    if (totalMovedDistance <= tapThresholdPx) {
                        // Stationary tap: trigger thumbstick click (L3/R3)
                        coroutineScope.launch {
                            viewModel.updateButton(stickKey, true)
                            delay(60.milliseconds)
                            viewModel.updateButton(stickKey, false)
                        }
                    }

                    // Damped return spring (matches cubic-bezier(.3, 1.6, .5, 1))
                    coroutineScope.launch {
                        launch {
                            animOffsetX.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(dampingRatio = 0.60f, stiffness = 480f)
                            )
                        }
                        launch {
                            animOffsetY.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(dampingRatio = 0.60f, stiffness = 480f)
                            )
                        }
                    }

                    if (isLeft) {
                        viewModel.updateLeftStick(0f, 0f)
                    } else {
                        viewModel.updateRightStick(0f, 0f)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val curOffsetX = effOffsetX
        val curOffsetY = effOffsetY
        val curDist = hypot(curOffsetX, curOffsetY)
        val curMagnitude = (curDist / maxTravelPx).coerceIn(0f, 1f)
        val curAngleRad = atan2(curOffsetY, curOffsetX)
        val curAngleDeg = Math.toDegrees(curAngleRad.toDouble()).toFloat()

        // Socket Housing Canvas: Ticks, Directional Gate Arc, Outer Neon Ring, Drop Shadow
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val r = size.minDimension / 2f
            val center = Offset(w / 2f, h / 2f)

            // Deep top-lit inset shadow: inset 0 8px 15px rgba(0,0,0,0.96)
            val topInsetH = h * 0.38f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Black.copy(alpha = 0.96f), Color.Transparent),
                    startY = 0f,
                    endY = topInsetH
                ),
                topLeft = Offset.Zero,
                size = Size(w, topInsetH)
            )

            // Bottom subtle specular rim: inset 0 -3px 5px rgba(255,255,255,0.025)
            val botInsetH = h * 0.20f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.025f)),
                    startY = h - botInsetH,
                    endY = h
                ),
                topLeft = Offset(0f, h - botInsetH),
                size = Size(w, botInsetH)
            )

            // 12 Graduation Ticks (.lx-ticks): 30° radial intervals
            val tickOuterR = r - 12.dp.toPx()
            val tickInnerR = tickOuterR - 6.dp.toPx()
            val tickColor = Color.White.copy(alpha = 0.20f)
            for (i in 0 until 12) {
                val tickAngle = (i * 30.0 - 90.0) * (PI / 180.0)
                val cosA = cos(tickAngle).toFloat()
                val sinA = sin(tickAngle).toFloat()
                drawLine(
                    color = tickColor,
                    start = Offset(center.x + cosA * tickInnerR, center.y + sinA * tickInnerR),
                    end = Offset(center.x + cosA * tickOuterR, center.y + sinA * tickOuterR),
                    strokeWidth = 1.8.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Directional Deflection Gate Arc (.lx-gate): swept beam scaled by magnitude
            if (curMagnitude > 0.05f) {
                val gateRadius = r - 5.dp.toPx()
                val gateSweep = 84f
                val gateStart = curAngleDeg - 42f
                val gateAlpha = curMagnitude.coerceIn(0f, 1f) * 0.95f

                // Outer beam bloom
                drawArc(
                    color = glowColor.copy(alpha = gateAlpha * 0.40f),
                    startAngle = gateStart,
                    sweepAngle = gateSweep,
                    useCenter = false,
                    topLeft = Offset(center.x - gateRadius, center.y - gateRadius),
                    size = Size(gateRadius * 2f, gateRadius * 2f),
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                )
                // Core beam
                drawArc(
                    color = glowColor.copy(alpha = gateAlpha),
                    startAngle = gateStart + 10f,
                    sweepAngle = gateSweep - 20f,
                    useCenter = false,
                    topLeft = Offset(center.x - gateRadius, center.y - gateRadius),
                    size = Size(gateRadius * 2f, gateRadius * 2f),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Dynamic Opposite-Casting 3D Drop Shadow behind moving cap (.lx-cap box-shadow):
            // calc(var(--dx) * -0.32) calc(8px + var(--dy) * -0.32) 12px rgba(0,0,0,0.82)
            val capR = 48.dp.toPx()
            val shadowCenterX = center.x + curOffsetX + (-0.32f * curOffsetX)
            val shadowCenterY = center.y + curOffsetY + (8.dp.toPx() - 0.32f * curOffsetY)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Black.copy(alpha = 0.82f), Color.Transparent),
                    center = Offset(shadowCenterX, shadowCenterY),
                    radius = capR + 10.dp.toPx()
                ),
                center = Offset(shadowCenterX, shadowCenterY),
                radius = capR + 10.dp.toPx()
            )
        }

        // Moving Thumbstick Cap (.lx-cap): 96dp circle
        Box(
            modifier = Modifier
                .offset { IntOffset(curOffsetX.roundToInt(), curOffsetY.roundToInt()) }
                .size(96.dp)
                .graphicsLayer {
                    scaleX = capScaleAnim
                    scaleY = capScaleAnim
                }
                .shadow(
                    elevation = 10.dp,
                    shape = CircleShape,
                    ambientColor = if (isRgbEnabled) glowColor.copy(alpha = 0.5f) else Color.Black,
                    spotColor = if (isRgbEnabled) glowColor else Color.Black
                )
                .clip(CircleShape)
                .background(capDomeGradient)
                .border(
                    width = 1.dp,
                    color = Color.Black.copy(alpha = 0.68f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val capW = size.width
                val capH = size.height
                val capR = size.minDimension / 2f
                val capCenter = Offset(capW / 2f, capH / 2f)

                // Inset top specular highlight: inset 0 2px 2px rgba(255,255,255,0.12)
                drawArc(
                    color = Color.White.copy(alpha = 0.12f),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(1.5.dp.toPx(), 1.5.dp.toPx()),
                    size = Size(capW - 3.dp.toPx(), capH - 3.dp.toPx()),
                    style = Stroke(width = 1.5.dp.toPx())
                )

                // Inset bottom shadow: inset 0 -8px 12px rgba(0,0,0,0.84)
                val botShadowH = capH * 0.35f
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.84f)),
                        startY = capH - botShadowH,
                        endY = capH
                    ),
                    topLeft = Offset(0f, capH - botShadowH),
                    size = Size(capW, botShadowH)
                )

                // Knurled Grip (.lx-grip): 48 dashed teeth along cap perimeter
                val gripRadius = capR - 5.dp.toPx()
                drawCircle(
                    color = Color.White.copy(alpha = 0.075f),
                    radius = gripRadius,
                    style = Stroke(
                        width = 4.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.5f, 5f), 0f)
                    )
                )

                // Cap Neon Ring (.lx-cap-ring): inset 13px, blooms brighter on touch
                val capRingRadius = capR - 13.dp.toPx()
                // Outer glow bloom
                drawCircle(
                    color = glowColor.copy(alpha = capBloomAlpha * 0.35f),
                    radius = capRingRadius,
                    style = Stroke(width = if (isDragging || isPressed) 7.dp.toPx() else 4.dp.toPx())
                )
                // Inner inset glow bloom
                drawCircle(
                    color = glowColor.copy(alpha = capBloomAlpha * 0.20f),
                    radius = capRingRadius - 2.dp.toPx(),
                    style = Stroke(width = if (isDragging || isPressed) 5.dp.toPx() else 3.dp.toPx())
                )
                // Core crisp ring
                drawCircle(
                    color = glowColor.copy(alpha = capBloomAlpha * 0.85f),
                    radius = capRingRadius,
                    style = Stroke(width = 2.dp.toPx())
                )

                // Inverse-Tracking Specular Lens Reflection (.lx-cap-lens):
                // moves in opposite direction of deflection (-dxn * 16%, -dyn * 16%)
                val normX = (curOffsetX / maxTravelPx).coerceIn(-1f, 1f)
                val normY = (curOffsetY / maxTravelPx).coerceIn(-1f, 1f)
                val specCenterX = capW * (0.50f - normX * 0.16f)
                val specCenterY = capH * (0.20f - normY * 0.16f)

                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.22f), Color.Transparent),
                        center = Offset(specCenterX, specCenterY),
                        radius = capW * 0.30f
                    ),
                    topLeft = Offset(specCenterX - capW * 0.25f, specCenterY - capH * 0.14f),
                    size = Size(capW * 0.50f, capH * 0.28f)
                )

                // Secondary bottom-right specular sheen
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.05f), Color.Transparent),
                        center = Offset(capW * 0.70f, capH * 0.82f),
                        radius = capW * 0.20f
                    ),
                    center = Offset(capW * 0.70f, capH * 0.82f),
                    radius = capW * 0.20f
                )
            }

            // Concave Center Thumb Dish (.lx-dish): 46dp recessed well
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(dishGradient)
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.95f),
                                Color.White.copy(alpha = 0.055f)
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val dishW = size.width
                    val dishH = size.height

                    // Deep center cavity shadow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.86f)
                            ),
                            center = Offset(dishW / 2f, dishH / 2f),
                            radius = dishW / 2f
                        ),
                        radius = dishW / 2f
                    )

                    // Inset top shadow: inset 0 3px 7px rgba(0,0,0,0.95)
                    val cavityShadowH = dishH * 0.40f
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.95f), Color.Transparent),
                            startY = 0f,
                            endY = cavityShadowH
                        ),
                        topLeft = Offset.Zero,
                        size = Size(dishW, cavityShadowH)
                    )
                }

                // Neon Glyph (.lx-g): "L" / "R" with dual-layer neon bloom matching font-size: 40px
                Text(
                    text = glyphLabel,
                    color = glowColor.copy(alpha = 0.35f),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.offset(0.dp, (-0.5).dp)
                )
                Text(
                    text = glyphLabel,
                    color = glowColor.copy(alpha = 0.65f),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.offset(0.dp, (0.5).dp)
                )
                Text(
                    text = glyphLabel,
                    color = glowColor,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Top Overlay Canvas: Outer Neon Ring (.lx-ring, z-index: 10) & Glass Lens (.lx-lens, z-index: 20)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val r = size.minDimension / 2f

            // Outer Neon Ring (.lx-ring, z-index: 10): inset 3px, 2px border + outer/inner glow
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
                color = glowColor.copy(alpha = 0.48f),
                radius = outerRingRadius,
                style = Stroke(width = 2.dp.toPx())
            )

            // Outer Glass Lens (.lx-lens, z-index: 20): Top-lit specular chamfer
            drawArc(
                color = Color.White.copy(alpha = 0.11f),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                size = Size(w - 2.dp.toPx(), h - 2.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )
            // Left subtle reflection
            drawArc(
                color = Color.White.copy(alpha = 0.055f),
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
                    colors = listOf(Color.White.copy(alpha = 0.055f), Color.Transparent),
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
 * Dedicated standalone Thumbstick Click Button (LSB / RSB or L3 / R3).
 * Shares the identical Flux physical design language:
 * - 70dp tactile cap housing
 * - Deep concave thumb dish
 * - Knurled grip perimeter
 * - Glow ring bloom and damped spring kinematics
 */
@Composable
fun FluxStickButton(
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
        label = "flux_stick_btn_scale"
    )
    val pressOffsetYAnim by animateFloatAsState(
        targetValue = if (isPressed) 3.5f else 0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 750f),
        label = "flux_stick_btn_offset"
    )

    val currentOnVibrate by rememberUpdatedState(onVibrate)
    val currentViewModel by rememberUpdatedState(viewModel)

    val glowColor = remember(isRgbEnabled, isLeft) {
        if (isRgbEnabled) {
            if (isLeft) Color(0xFF2FD4B6) else Color(0xFFFF3185)
        } else {
            Color(0xFF2FD4B6)
        }
    }

    val capDomeGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF3D4145),
                Color(0xFF202327),
                Color(0xFF111316),
                Color(0xFF050506)
            ),
            center = Offset(0.50f, 0.36f),
            radius = 180f
        )
    }

    val dishGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF020203),
                Color(0xFF0B0C0D),
                Color(0xFF161719)
            ),
            center = Offset(0.50f, 0.58f),
            radius = 80f
        )
    }

    val rgbBloomAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 0.45f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "flux_stick_btn_bloom"
    )

    Box(
        modifier = modifier
            .size(70.dp)
            .graphicsLayer {
                scaleX = scaleAnim
                scaleY = scaleAnim
            }
            .offset { IntOffset(0, pressOffsetYAnim.dp.roundToPx()) }
            .drawBehind {
                if (isRgbEnabled) {
                    val coreR = size.minDimension * (if (isPressed) 1.05f else 0.85f)
                    // 1. Turbine core halo
                    drawCircle(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0.00f to (if (isPressed) Color.White else glowColor).copy(alpha = rgbBloomAlpha * 0.65f),
                                0.35f to glowColor.copy(alpha = rgbBloomAlpha * 0.30f),
                                0.75f to glowColor.copy(alpha = rgbBloomAlpha * 0.10f),
                                1.00f to Color.Transparent
                            ),
                            center = center,
                            radius = coreR
                        ),
                        radius = coreR,
                        center = center
                    )

                    // 2. Radiating turbine vanes
                    val vaneR1 = size.minDimension * 0.44f
                    val vaneR2 = size.minDimension * 0.54f
                    val vaneColor = glowColor.copy(alpha = if (isPressed) 0.85f else 0.35f)
                    for (i in 0 until 8) {
                        val angle = Math.toRadians(i * 45.0)
                        val cosA = cos(angle).toFloat()
                        val sinA = sin(angle).toFloat()
                        drawLine(
                            color = vaneColor,
                            start = Offset(center.x + vaneR1 * cosA, center.y + vaneR1 * sinA),
                            end = Offset(center.x + vaneR2 * cosA, center.y + vaneR2 * sinA),
                            strokeWidth = if (isPressed) 2.5f else 1.5f
                        )
                    }
                }
            }
            .shadow(
                elevation = if (isPressed) 2.dp else 8.dp,
                shape = CircleShape,
                ambientColor = if (isRgbEnabled) glowColor else Color.Black,
                spotColor = if (isRgbEnabled) glowColor else Color.Black
            )
            .clip(CircleShape)
            .background(capDomeGradient)
            .border(
                width = 1.dp,
                color = Color.Black.copy(alpha = 0.75f),
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

            // Knurled grip dashed ring
            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                radius = r - 4.dp.toPx(),
                style = Stroke(
                    width = 3.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 4f), 0f)
                )
            )

            // Neon ring bloom
            val ringRadius = r - 10.dp.toPx()
            drawCircle(
                color = glowColor.copy(alpha = if (isPressed) 0.55f else 0.25f),
                radius = ringRadius,
                style = Stroke(width = 4.dp.toPx())
            )
            drawCircle(
                color = glowColor.copy(alpha = if (isPressed) 1.0f else 0.70f),
                radius = ringRadius,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // Center Dish with Glyph
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(dishGradient)
                .border(
                    width = 1.dp,
                    color = Color.Black.copy(alpha = 0.85f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = displayLabel ?: (if (isLeft) "L" else "R"),
                color = if (isPressed) Color.White else glowColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
