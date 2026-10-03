package com.sanket.tools.nexpad.ui.components.controller

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
 * Console-grade Spotlight analog joystick natively translated from optical reference:
 * - 150dp socket housing with #030304 -> #0a0b0c -> #1a1c1e gradient
 * - Dynamic Moving Light Pool (.lamp-pool): 62dp radial spotlight that illuminates the floor
 * - Hidden Matrix Floor Dots (.lamp-floor): 14dp micro-dot lattice that only reveals under the moving light pool
 * - Directional Gate Arc (.lx-gate): Conic sweep arc lighting up along travel direction
 * - 76dp Center Puck (.cap-puck) with opposite-casting 3D drop shadow
 * - Puck Neon Containment Ring (.puck-ring) with drag bloom
 * - Central Luminous Core Dot (.puck-dot): 12dp radiant circle with multi-layer bloom
 * - Dynamic Specular Lens Reflection (.puck-lens) shifting with travel
 * - Zero-latency pointer tracking with damped spring return kinematics
 */
@Composable
fun SpotlightJoystick(
    isLeft: Boolean,
    isConnected: Boolean,
    viewModel: GamepadViewModel,
    isRgbEnabled: Boolean,
    isLocked: Boolean = true,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current.density

    // Travel bounds matching reference: MAX = 24px in a 150px housing with 76px puck
    val maxTravelPx = with(LocalDensity.current) { 24.dp.toPx() }
    val tapThresholdPx = with(LocalDensity.current) { 4.dp.toPx() }

    // Instant finger tracking during drag + damped spring return
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

    // Default Spotlight glow: Warm Gold #FFD23F (Left) or Hot Pink #FF3F85 (Right)
    val glowColor = remember(isRgbEnabled, isLeft) {
        if (isLeft) Color(0xFFFFD23F) else Color(0xFFFF3F85)
    }

    // Micro-compression: scale down to 0.97 on press (--cs: 0.97)
    val capScaleAnim by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1.0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "spotlight_cap_scale"
    )

    // Dynamic ring bloom: 0.70 idle, 1.00 when dragging
    val puckRingAlpha by animateFloatAsState(
        targetValue = if (isDragging || isPressed) 1.0f else 0.70f,
        animationSpec = tween(durationMillis = 150),
        label = "spotlight_ring_alpha"
    )

    // Ambient glow bloom behind housing
    val ambientBloomAlpha by animateFloatAsState(
        targetValue = if (isDragging || isPressed) 0.55f else 0.28f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 800f),
        label = "spotlight_ambient_bloom"
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

    // Puck radial gradient: circle at 50% 38%, #34373b 0%, #17181b 55%, #050506 100%
    val puckDomeGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF34373B),
                Color(0xFF17181B),
                Color(0xFF050506)
            ),
            center = Offset(0.50f, 0.38f),
            radius = 160f
        )
    }

    val stickKey = remember(isLeft) { if (isLeft) "LSB" else "RSB" }

    val stickState = if (isLeft) viewModel.leftStickState else viewModel.rightStickState
    val effOffsetX = if (isLocked) animOffsetX.value else stickState.first * maxTravelPx
    val effOffsetY = if (isLocked) animOffsetY.value else -stickState.second * maxTravelPx

    val deflectionFraction = (hypot(effOffsetX, effOffsetY) / maxTravelPx).coerceIn(0f, 1f)
    val rgbBloomAlpha by animateFloatAsState(
        targetValue = 0.40f + 0.55f * deflectionFraction,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 800f),
        label = "spotlight_joystick_bloom"
    )

    Box(
        modifier = modifier
            .size(150.dp)
            .drawBehind {
                if (isRgbEnabled) {
                    val curX = effOffsetX
                    val curY = effOffsetY
                    val curDist = hypot(curX, curY)
                    val defFraction = (curDist / maxTravelPx).coerceIn(0f, 1f)

                    // 1. Ambient projector socket glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                glowColor.copy(alpha = rgbBloomAlpha * 0.35f),
                                glowColor.copy(alpha = rgbBloomAlpha * 0.12f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = size.minDimension * 0.58f
                        ),
                        radius = size.minDimension * 0.58f,
                        center = center
                    )

                    // 2. Reflector dish rim ticks around socket perimeter
                    val rimRadius = size.minDimension * 0.49f
                    for (i in 0 until 16) {
                        val tickRad = Math.toRadians((i * 22.5))
                        val cosT = cos(tickRad).toFloat()
                        val sinT = sin(tickRad).toFloat()
                        val innerR = rimRadius - (if (i % 4 == 0) 6.dp.toPx() else 3.dp.toPx())
                        val outerR = rimRadius + 2.dp.toPx()
                        val tickAlpha = if (i % 4 == 0) 0.55f else 0.25f
                        drawLine(
                            color = glowColor.copy(alpha = rgbBloomAlpha * tickAlpha),
                            start = Offset(center.x + innerR * cosT, center.y + innerR * sinT),
                            end = Offset(center.x + outerR * cosT, center.y + outerR * sinT),
                            strokeWidth = if (i % 4 == 0) 2f else 1f
                        )
                    }

                    // 3. Volumetric spotlight cone cast along stick deflection
                    if (defFraction > 0.05f) {
                        val beamAngleDeg = Math.toDegrees(atan2(curY.toDouble(), curX.toDouble())).toFloat()
                        val coneSpread = 50f - 15f * defFraction
                        val beamStart = beamAngleDeg - coneSpread / 2f
                        val beamLength = size.minDimension * (0.55f + 0.25f * defFraction)

                        drawArc(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    glowColor.copy(alpha = rgbBloomAlpha * (0.60f + 0.35f * defFraction)),
                                    glowColor.copy(alpha = rgbBloomAlpha * 0.20f),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = beamLength
                            ),
                            startAngle = beamStart,
                            sweepAngle = coneSpread,
                            useCenter = true,
                            topLeft = Offset(center.x - beamLength, center.y - beamLength),
                            size = Size(beamLength * 2f, beamLength * 2f)
                        )

                        // Focused core beam laser ray
                        val beamRad = Math.toRadians(beamAngleDeg.toDouble())
                        val cosB = cos(beamRad).toFloat()
                        val sinB = sin(beamRad).toFloat()
                        drawLine(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = rgbBloomAlpha * 0.90f),
                                    glowColor.copy(alpha = rgbBloomAlpha * 0.50f),
                                    Color.Transparent
                                ),
                                start = center,
                                end = Offset(center.x + beamLength * cosB, center.y + beamLength * sinB)
                            ),
                            start = center,
                            end = Offset(center.x + beamLength * cosB, center.y + beamLength * sinB),
                            strokeWidth = 3.5.dp.toPx()
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
            .background(socketGradient, shape = CircleShape)
            .border(
                width = 1.dp,
                color = Color.Black.copy(alpha = 0.60f),
                shape = CircleShape
            )
            .pointerInput(isConnected, isLocked, isLeft, isCameraMode, cameraSensitivity, density, maxTravelPx) {
                if (!isLocked) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f
                    val deadzoneRadiusPx = 6.dp.toPx()

                    val startTime = System.currentTimeMillis()
                    var maxDragDistance = 0f
                    var previousTouchX = down.position.x
                    var previousTouchY = down.position.y
                    var previousTimeMs = startTime
                    var currentStickX = 0f
                    var currentStickY = 0f
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
        val curOffsetX = effOffsetX
        val curOffsetY = effOffsetY
        val curDist = hypot(curOffsetX, curOffsetY)
        val curMagnitude = min(1f, curDist / maxTravelPx)
        val curAngleDeg = (atan2(curOffsetX, -curOffsetY) * 180f / Math.PI.toFloat() + 360f) % 360f
        val curNormX = (curOffsetX / maxTravelPx).coerceIn(-1f, 1f)
        val curNormY = (curOffsetY / maxTravelPx).coerceIn(-1f, 1f)

        // Stationary Socket Background Canvas: Inset Depth, Spotlight Pool, and Floor Dots Matrix
        Canvas(modifier = Modifier.fillMaxSize().clip(CircleShape)) {
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

            // Bottom subtle reflection: inset 0 -3px 5px rgba(255,255,255,0.025)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.025f)),
                    startY = h - 6.dp.toPx(),
                    endY = h
                ),
                topLeft = Offset(0f, h - 6.dp.toPx()),
                size = Size(w, 6.dp.toPx())
            )

            // Current Spotlight Center position tracking thumbstick displacement
            val spotCenter = Offset(center.x + curOffsetX, center.y + curOffsetY)

            // 1. MOVING LIGHT POOL (.lamp-pool): 62dp radius radial gradient, opacity 0.35, screen blend
            val poolRadius = 62.dp.toPx()
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glowColor.copy(alpha = 0.35f),
                        glowColor.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = spotCenter,
                    radius = poolRadius
                ),
                center = spotCenter,
                radius = poolRadius
            )

            // 2. HIDDEN FLOOR DOT MATRIX (.lamp-floor): 14dp lattice masked by 56dp spotlight circle
            val dotSpacingPx = 14.dp.toPx()
            val maskRadiusPx = 56.dp.toPx()
            val maxSocketDistPx = r - 5.dp.toPx()

            val minGridX = ((spotCenter.x - maskRadiusPx - center.x) / dotSpacingPx).toInt() - 1
            val maxGridX = ((spotCenter.x + maskRadiusPx - center.x) / dotSpacingPx).toInt() + 1
            val minGridY = ((spotCenter.y - maskRadiusPx - center.y) / dotSpacingPx).toInt() - 1
            val maxGridY = ((spotCenter.y + maskRadiusPx - center.y) / dotSpacingPx).toInt() + 1

            for (gx in minGridX..maxGridX) {
                for (gy in minGridY..maxGridY) {
                    val dotX = center.x + gx * dotSpacingPx
                    val dotY = center.y + gy * dotSpacingPx

                    val distFromSocket = hypot(dotX - center.x, dotY - center.y)
                    if (distFromSocket > maxSocketDistPx) continue

                    val distFromSpot = hypot(dotX - spotCenter.x, dotY - spotCenter.y)
                    if (distFromSpot < maskRadiusPx) {
                        val factor = (1f - distFromSpot / maskRadiusPx).coerceIn(0f, 1f)
                        val dotRadius = 1.3.dp.toPx()

                        // Core luminous dot
                        drawCircle(
                            color = glowColor.copy(alpha = factor * 0.90f),
                            radius = dotRadius,
                            center = Offset(dotX, dotY)
                        )

                        // Outer radiant halo for dots close to spotlight center
                        if (factor > 0.35f) {
                            drawCircle(
                                color = glowColor.copy(alpha = factor * 0.40f),
                                radius = 2.4.dp.toPx(),
                                center = Offset(dotX, dotY)
                            )
                        }
                    }
                }
            }

            // 3. DIRECTIONAL GATE ARC (.lx-gate)
            if (curMagnitude > 0.05f) {
                val gateSweep = 84f
                val gateStart = (curAngleDeg - 90f - gateSweep / 2f)
                val gateRadius = r - 5.dp.toPx()

                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Transparent,
                            glowColor.copy(alpha = curMagnitude * 0.70f),
                            Color.Transparent
                        ),
                        center = center
                    ),
                    startAngle = gateStart,
                    sweepAngle = gateSweep,
                    useCenter = false,
                    topLeft = Offset(center.x - gateRadius, center.y - gateRadius),
                    size = Size(gateRadius * 2f, gateRadius * 2f),
                    style = Stroke(width = 4.dp.toPx())
                )
            }

            // 4. DYNAMIC OPPOSITE-CASTING DROP SHADOW behind 76dp Center Puck:
            // calc(dx * -0.3) calc(8px + dy * -0.3) 12px rgba(0,0,0,0.8)
            val shadowOffsetX = curOffsetX * -0.30f
            val shadowOffsetY = 8.dp.toPx() + curOffsetY * -0.30f
            val puckR = 38.dp.toPx()

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Black.copy(alpha = 0.80f), Color.Transparent),
                    center = Offset(center.x + shadowOffsetX, center.y + shadowOffsetY),
                    radius = puckR + 12.dp.toPx()
                ),
                center = Offset(center.x + shadowOffsetX, center.y + shadowOffsetY),
                radius = puckR + 12.dp.toPx()
            )
        }

        // 76dp CENTER PUCK CAP (.cap-puck): translates with thumb displacement
        Box(
            modifier = Modifier
                .offset { IntOffset(curOffsetX.roundToInt(), curOffsetY.roundToInt()) }
                .graphicsLayer {
                    scaleX = capScaleAnim
                    scaleY = capScaleAnim
                }
                .size(76.dp)
                .shadow(
                    elevation = 10.dp,
                    shape = CircleShape,
                    ambientColor = if (isRgbEnabled) glowColor.copy(alpha = 0.5f) else Color.Black,
                    spotColor = if (isRgbEnabled) glowColor else Color.Black
                )
                .clip(CircleShape)
                .background(puckDomeGradient)
                .border(1.dp, Color.Black.copy(alpha = 0.60f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val puckW = size.width
                val puckH = size.height
                val puckR = size.minDimension / 2f
                val puckCenter = Offset(puckW / 2f, puckH / 2f)

                // Top specular rim: inset 0 2px 2px rgba(255,255,255,0.10)
                drawArc(
                    color = Color.White.copy(alpha = 0.10f),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(1.5.dp.toPx(), 1.5.dp.toPx()),
                    size = Size(puckW - 3.dp.toPx(), puckH - 3.dp.toPx()),
                    style = Stroke(width = 1.5.dp.toPx())
                )

                // Inset bottom shadow: inset 0 -6px 9px rgba(0,0,0,0.80)
                val botShadowH = puckH * 0.35f
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.80f)),
                        startY = puckH - botShadowH,
                        endY = puckH
                    ),
                    topLeft = Offset(0f, puckH - botShadowH),
                    size = Size(puckW, botShadowH)
                )

                // Puck Neon Containment Ring (.puck-ring): inset 5px
                val ringRadius = puckR - 5.dp.toPx()
                // Soft halo
                drawCircle(
                    color = glowColor.copy(alpha = puckRingAlpha * 0.16f),
                    radius = ringRadius,
                    style = Stroke(width = 3.dp.toPx())
                )
                // Crisp core ring
                drawCircle(
                    color = glowColor.copy(alpha = puckRingAlpha * 0.85f),
                    radius = ringRadius,
                    style = Stroke(width = 1.2.dp.toPx())
                )

                // Central Luminous Core Dot (.puck-dot): 9dp diameter with smooth radial bloom
                val dotRadius = 4.5.dp.toPx()
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glowColor.copy(alpha = 0.85f),
                            glowColor.copy(alpha = 0.35f),
                            Color.Transparent
                        ),
                        center = puckCenter,
                        radius = dotRadius + 6.dp.toPx()
                    ),
                    center = puckCenter,
                    radius = dotRadius + 6.dp.toPx()
                )
                drawCircle(
                    color = glowColor,
                    center = puckCenter,
                    radius = dotRadius
                )

                // Dynamic Cap Specular Lens (.puck-lens): ellipse 42% 24% at calc(50% - dxn * 16%) calc(20% - dyn * 16%)
                val specCenterX = puckW * (0.50f - curNormX * 0.16f)
                val specCenterY = puckH * (0.20f - curNormY * 0.16f)
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.22f), Color.Transparent),
                        center = Offset(specCenterX, specCenterY),
                        radius = puckW * 0.30f
                    ),
                    topLeft = Offset(specCenterX - puckW * 0.21f, specCenterY - puckH * 0.12f),
                    size = Size(puckW * 0.42f, puckH * 0.24f)
                )
            }
        }

        // Top Glass Lens Overlay (.lx-lens & delicate .lx-ring)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val r = size.minDimension / 2f

            // Outer Neon Rim (.lx-ring): single delicate glowing rim at inset 3px (opacity 0.45)
            val outerRingRadius = r - 3.dp.toPx()
            drawCircle(
                color = glowColor.copy(alpha = 0.10f),
                radius = outerRingRadius,
                style = Stroke(width = 3.5.dp.toPx())
            )
            drawCircle(
                color = glowColor.copy(alpha = 0.38f),
                radius = outerRingRadius,
                style = Stroke(width = 1.2.dp.toPx())
            )

            // Outer Glass Lens (.lx-lens)
            drawArc(
                color = Color.White.copy(alpha = 0.12f),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                size = Size(w - 2.dp.toPx(), h - 2.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )
            drawArc(
                color = Color.White.copy(alpha = 0.06f),
                startAngle = 90f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                size = Size(w - 2.dp.toPx(), h - 2.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )
            drawArc(
                color = Color.Black.copy(alpha = 0.30f),
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                size = Size(w - 2.dp.toPx(), h - 2.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )
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
 * Dedicated standalone Spotlight Thumbstick Button (LSB / RSB or L3 / R3).
 * Shares the spotlight floor light pool, matrix dots, and luminous central core dot.
 */
@Composable
fun SpotlightStickButton(
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

    // Damped spring kinematics on button press
    val scaleAnim by animateFloatAsState(
        targetValue = if (isPressed) 0.89f else 1.0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "spotlight_btn_scale"
    )
    val pressOffsetYAnim by animateFloatAsState(
        targetValue = if (isPressed) 2.0f else 0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "spotlight_btn_offset"
    )
    val ringBloomAlpha by animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0.70f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 800f),
        label = "spotlight_btn_ring_bloom"
    )

    val currentOnVibrate by rememberUpdatedState(onVibrate)
    val currentViewModel by rememberUpdatedState(viewModel)

    val glowColor = remember(isRgbEnabled, isLeft) {
        if (isLeft) Color(0xFFFFD23F) else Color(0xFFFF3F85)
    }

    val puckDomeGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF34373B),
                Color(0xFF17181B),
                Color(0xFF050506)
            ),
            center = Offset(0.50f, 0.38f),
            radius = 140f
        )
    }

    val rgbBloomAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 0.45f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "spotlight_stick_btn_bloom"
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
                    val buttonR = size.minDimension * 0.5f

                    // 1. Reflector dish parabolic ambient flare
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                glowColor.copy(alpha = rgbBloomAlpha * (if (isPressed) 0.65f else 0.35f)),
                                glowColor.copy(alpha = rgbBloomAlpha * 0.15f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = size.minDimension * 0.70f
                        ),
                        radius = size.minDimension * 0.70f
                    )

                    // 2. Dual concentric fresnel reflector rings
                    drawCircle(
                        color = glowColor.copy(alpha = rgbBloomAlpha * (if (isPressed) 0.80f else 0.40f)),
                        radius = buttonR + 3.dp.toPx(),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    drawCircle(
                        color = glowColor.copy(alpha = rgbBloomAlpha * (if (isPressed) 0.50f else 0.22f)),
                        radius = buttonR + 7.dp.toPx(),
                        style = Stroke(width = 1.dp.toPx())
                    )

                    // 3. 4 Orthogonal projector alignment notches
                    val notchLen = 5.dp.toPx()
                    val notchStart = buttonR + 2.dp.toPx()
                    val notchEnd = notchStart + notchLen
                    val notchAlpha = rgbBloomAlpha * (if (isPressed) 0.90f else 0.50f)
                    drawLine(glowColor.copy(alpha = notchAlpha), Offset(center.x, center.y - notchEnd), Offset(center.x, center.y - notchStart), strokeWidth = 2f)
                    drawLine(glowColor.copy(alpha = notchAlpha), Offset(center.x, center.y + notchStart), Offset(center.x, center.y + notchEnd), strokeWidth = 2f)
                    drawLine(glowColor.copy(alpha = notchAlpha), Offset(center.x - notchEnd, center.y), Offset(center.x - notchStart, center.y), strokeWidth = 2f)
                    drawLine(glowColor.copy(alpha = notchAlpha), Offset(center.x + notchStart, center.y), Offset(center.x + notchEnd, center.y), strokeWidth = 2f)
                }
            }
            .shadow(
                elevation = if (isPressed) 2.dp else 8.dp,
                shape = CircleShape,
                ambientColor = if (isRgbEnabled) glowColor else Color.Black,
                spotColor = if (isRgbEnabled) glowColor else Color.Black
            )
            .clip(CircleShape)
            .background(puckDomeGradient)
            .border(1.dp, Color.Black.copy(alpha = 0.60f), CircleShape)
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
        // Floor matrix dots and spotlight illumination
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val r = size.minDimension / 2f
            val center = Offset(w / 2f, h / 2f)

            // Top specular arc
            drawArc(
                color = Color.White.copy(alpha = 0.14f),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                size = Size(w - 2.dp.toPx(), h - 2.dp.toPx()),
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Bottom inset shadow
            val botShadowH = h * 0.38f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.80f)),
                    startY = h - botShadowH,
                    endY = h
                ),
                topLeft = Offset(0f, h - botShadowH),
                size = Size(w, botShadowH)
            )

            // Centered Spotlight Light Pool: 32dp radius
            val poolRadius = 32.dp.toPx()
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glowColor.copy(alpha = 0.30f),
                        glowColor.copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = poolRadius
                ),
                center = center,
                radius = poolRadius
            )

            // Floor Matrix Dots
            val dotSpacingPx = 10.dp.toPx()
            val maskRadiusPx = 28.dp.toPx()
            val minGrid = (-maskRadiusPx / dotSpacingPx).toInt() - 1
            val maxGrid = (maskRadiusPx / dotSpacingPx).toInt() + 1

            for (gx in minGrid..maxGrid) {
                for (gy in minGrid..maxGrid) {
                    val dotX = center.x + gx * dotSpacingPx
                    val dotY = center.y + gy * dotSpacingPx
                    val dist = hypot(dotX - center.x, dotY - center.y)
                    if (dist < maskRadiusPx) {
                        val factor = (1f - dist / maskRadiusPx).coerceIn(0f, 1f)
                        drawCircle(
                            color = glowColor.copy(alpha = factor * 0.80f),
                            radius = 1.1.dp.toPx(),
                            center = Offset(dotX, dotY)
                        )
                    }
                }
            }

            // Outer Neon Containment Ring
            val outerRingR = r - 5.dp.toPx()
            drawCircle(
                color = glowColor.copy(alpha = ringBloomAlpha * 0.25f),
                radius = outerRingR,
                style = Stroke(width = 4.dp.toPx())
            )
            drawCircle(
                color = glowColor.copy(alpha = ringBloomAlpha * 0.70f),
                radius = outerRingR,
                style = Stroke(width = 1.8.dp.toPx())
            )

            // Specular lens highlight
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.25f), Color.Transparent),
                    center = Offset(w * 0.36f, h * 0.24f),
                    radius = w * 0.28f
                ),
                topLeft = Offset(w * 0.16f, h * 0.10f),
                size = Size(w * 0.40f, h * 0.28f)
            )
        }

        // Center Puck with Label
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF030304), Color(0xFF121314)),
                        center = Offset(0.50f, 0.60f),
                        radius = 60f
                    )
                )
                .border(1.dp, glowColor.copy(alpha = 0.60f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = displayLabel ?: key,
                color = glowColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
