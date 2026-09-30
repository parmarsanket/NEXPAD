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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
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
 * Console-grade Compass analog joystick natively translated from optical reference:
 * - 150dp socket housing with #030304 -> #0a0b0c -> #1a1c1e gradient
 * - Eight directional pips (45° intervals) around the socket perimeter that illuminate
 *   in high brightness with glow bloom along the nearest travel direction
 * - 92dp spherical cap (.cap-compass) with opposite-casting dynamic 3D drop shadow
 * - Rotating direction indicator triangle (.ind) sweeping in real time along travel vector
 * - Cap neon containment ring (.lx-cap-ring) with drag bloom
 * - Deep center dish (.lx-dish) with dark vignette and bold "L"/"R" glyph
 * - Dynamic specular lens reflection (.lx-cap-lens) that shifts with travel
 * - Zero-latency pointer tracking with damped spring return kinematics
 */
@Composable
fun CompassJoystick(
    isLeft: Boolean,
    isConnected: Boolean,
    viewModel: GamepadViewModel,
    isRgbEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current.density

    // Travel bounds matching reference: MAX = 24px in a 150px housing with 92px cap
    val maxTravelPx = with(LocalDensity.current) { 24.dp.toPx() }
    val tapThresholdPx = with(LocalDensity.current) { 4.dp.toPx() }

    // Finger tracking during drag + damped spring overshoot on release
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

    // Default Compass glow: Ice Blue #5AA8FF (Left) or Coral Pink #FF5A88 (Right)
    val glowColor = remember(isRgbEnabled, isLeft) {
        if (isRgbEnabled) {
            if (isLeft) Color(0xFF5AA8FF) else Color(0xFFFF5A88)
        } else {
            if (isLeft) Color(0xFF5AA8FF) else Color(0xFFFF5A88)
        }
    }

    // Micro-compression: scale down to 0.97 on press (--cs: 0.97)
    val capScaleAnim by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1.0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "compass_cap_scale"
    )

    // Dynamic ring bloom intensity: 0.70 idle, 1.00 when dragging/active
    val capRingAlpha by animateFloatAsState(
        targetValue = if (isDragging || isPressed) 1.0f else 0.70f,
        animationSpec = tween(durationMillis = 150),
        label = "compass_ring_bloom"
    )

    // Ambient glow bloom behind housing
    val ambientBloomAlpha by animateFloatAsState(
        targetValue = if (isDragging || isPressed) 0.55f else 0.28f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 800f),
        label = "compass_ambient_bloom"
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

    // Cap radial gradient: circle at 50% 38%, #34373b 0%, #17181b 55%, #050506 100%
    val capDomeGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF34373B),
                Color(0xFF17181B),
                Color(0xFF050506)
            ),
            center = Offset(0.50f, 0.38f),
            radius = 200f
        )
    }

    // Center dish radial gradient: circle at 50% 60%, #030304 -> #121314
    val dishGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF030304),
                Color(0xFF121314)
            ),
            center = Offset(0.50f, 0.60f),
            radius = 70f
        )
    }

    val stickKey = remember(isLeft) { if (isLeft) "LSB" else "RSB" }

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
            .background(socketGradient, shape = CircleShape)
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
        val curOffsetX = animOffsetX.value
        val curOffsetY = animOffsetY.value
        val curDist = hypot(curOffsetX, curOffsetY)
        val curMagnitude = min(1f, curDist / maxTravelPx)
        val curAngleDeg = (atan2(curOffsetX, -curOffsetY) * 180f / Math.PI.toFloat() + 360f) % 360f
        val curNormX = (curOffsetX / maxTravelPx).coerceIn(-1f, 1f)
        val curNormY = (curOffsetY / maxTravelPx).coerceIn(-1f, 1f)

        // Which compass pip is illuminated (index 0..7 matching 0° North to 315° North-West)
        val activePipIndex = if (curMagnitude > 0.40f) {
            (Math.round(curAngleDeg / 45.0).toInt() % 8)
        } else {
            -1
        }

        // Stationary Socket Background Canvas: Inset Depth, 8 Compass Pips, and Directional Gate Arc
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

            // Eight Directional Compass Pips (.pips i)
            // Distance from center = 62px (scaled to current density)
            val pipDist = 62.dp.toPx()
            val pipRadius = 3.dp.toPx()

            for (i in 0 until 8) {
                // Angle: i = 0 is North (-90° from positive X axis), i = 2 is East (0°), etc.
                val angleRad = Math.toRadians((i * 45.0) - 90.0)
                val pipCenter = Offset(
                    center.x + (pipDist * cos(angleRad)).toFloat(),
                    center.y + (pipDist * sin(angleRad)).toFloat()
                )

                if (i == activePipIndex) {
                    // Active pip: 100% opacity + glowing radial bloom (box-shadow: 0 0 8px 2px var(--glow))
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                glowColor.copy(alpha = 0.60f),
                                glowColor.copy(alpha = 0.20f),
                                Color.Transparent
                            ),
                            center = pipCenter,
                            radius = pipRadius + 6.dp.toPx()
                        ),
                        center = pipCenter,
                        radius = pipRadius + 6.dp.toPx()
                    )
                    drawCircle(
                        color = glowColor,
                        radius = pipRadius,
                        center = pipCenter
                    )
                } else {
                    // Idle pip: 22% opacity
                    drawCircle(
                        color = glowColor.copy(alpha = 0.22f),
                        radius = pipRadius,
                        center = pipCenter
                    )
                }
            }

            // Directional Gate Arc (.lx-gate): sweeps along travel angle
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

            // Dynamic Opposite-Casting Drop Shadow behind Cap (.cap-compass box-shadow):
            // calc(dx * -0.3) calc(8px + dy * -0.3) 12px rgba(0,0,0,0.8)
            val shadowOffsetX = curOffsetX * -0.30f
            val shadowOffsetY = 8.dp.toPx() + curOffsetY * -0.30f
            val capR = 46.dp.toPx()

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Black.copy(alpha = 0.80f), Color.Transparent),
                    center = Offset(center.x + shadowOffsetX, center.y + shadowOffsetY),
                    radius = capR + 12.dp.toPx()
                ),
                center = Offset(center.x + shadowOffsetX, center.y + shadowOffsetY),
                radius = capR + 12.dp.toPx()
            )
        }

        // Spherical Compass Moving Cap (.cap-compass): 92dp diameter
        Box(
            modifier = Modifier
                .offset { IntOffset(curOffsetX.roundToInt(), curOffsetY.roundToInt()) }
                .graphicsLayer {
                    scaleX = capScaleAnim
                    scaleY = capScaleAnim
                }
                .size(92.dp)
                .shadow(
                    elevation = 10.dp,
                    shape = CircleShape,
                    spotColor = if (isRgbEnabled) glowColor else Color.Black,
                    ambientColor = if (isRgbEnabled) glowColor.copy(alpha = 0.4f) else Color.Black
                )
                .clip(CircleShape)
                .background(capDomeGradient)
                .border(1.dp, Color.Black.copy(alpha = 0.60f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Cap Surface Artwork: Inset Shadows, Rotating Pointer, Cap Neon Ring, Lens Sheen
            Canvas(modifier = Modifier.fillMaxSize()) {
                val capW = size.width
                val capH = size.height
                val capR = size.minDimension / 2f
                val capCenter = Offset(capW / 2f, capH / 2f)

                // Top specular rim: inset 0 2px 2px rgba(255,255,255,0.10)
                drawArc(
                    color = Color.White.copy(alpha = 0.10f),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(1.5.dp.toPx(), 1.5.dp.toPx()),
                    size = Size(capW - 3.dp.toPx(), capH - 3.dp.toPx()),
                    style = Stroke(width = 1.5.dp.toPx())
                )

                // Inset bottom shadow: inset 0 -7px 11px rgba(0,0,0,0.80)
                val botShadowH = capH * 0.38f
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.80f)),
                        startY = capH - botShadowH,
                        endY = capH
                    ),
                    topLeft = Offset(0f, capH - botShadowH),
                    size = Size(capW, botShadowH)
                )

                // Rotating Direction Pointer Indicator (.ind + .ind::before)
                // Points in deflection direction with opacity proportional to deflection magnitude
                if (curMagnitude > 0.04f) {
                    rotate(degrees = curAngleDeg, pivot = capCenter) {
                        // Slender optical compass pointer
                        val arrowTopY = 6.dp.toPx()
                        val arrowBottomY = arrowTopY + 7.dp.toPx()
                        val arrowHalfWidth = 4.5.dp.toPx()

                        val arrowPath = Path().apply {
                            moveTo(capCenter.x, arrowTopY)
                            lineTo(capCenter.x - arrowHalfWidth, arrowBottomY)
                            lineTo(capCenter.x + arrowHalfWidth, arrowBottomY)
                            close()
                        }

                        // Glow halo behind arrow
                        drawPath(
                            path = arrowPath,
                            color = glowColor.copy(alpha = curMagnitude * 0.25f),
                            style = Stroke(width = 2.5.dp.toPx())
                        )
                        // Crisp core arrow
                        drawPath(
                            path = arrowPath,
                            color = glowColor.copy(alpha = curMagnitude * 0.95f)
                        )
                    }
                }

                // Cap Neon Ring (.lx-cap-ring): inset 19px
                val capRingRadius = capR - 19.dp.toPx()
                // Soft halo
                drawCircle(
                    color = glowColor.copy(alpha = capRingAlpha * 0.16f),
                    radius = capRingRadius,
                    style = Stroke(width = 3.dp.toPx())
                )
                // Crisp core ring
                drawCircle(
                    color = glowColor.copy(alpha = capRingAlpha * 0.85f),
                    radius = capRingRadius,
                    style = Stroke(width = 1.2.dp.toPx())
                )

                // Dynamic Cap Specular Lens (.lx-cap-lens): shifts opposite to deflection
                // ellipse 42% 24% at calc(50% - dxn * 16%) calc(20% - dyn * 16%)
                val specCenterX = capW * (0.50f - curNormX * 0.16f)
                val specCenterY = capH * (0.20f - curNormY * 0.16f)
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.20f), Color.Transparent),
                        center = Offset(specCenterX, specCenterY),
                        radius = capW * 0.32f
                    ),
                    topLeft = Offset(specCenterX - capW * 0.21f, specCenterY - capH * 0.12f),
                    size = Size(capW * 0.42f, capH * 0.24f)
                )

                // Bottom-right secondary bounce sheen: circle at 70% 82%
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

            // Center Dish (.lx-dish): 38dp diameter with dark vignette and glyph
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(dishGradient)
                    .border(1.dp, Color.White.copy(alpha = 0.06f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Dish inner shadow and dark vignette
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val dishW = size.width
                    val dishH = size.height

                    // Inset top shadow: inset 0 3px 6px rgba(0,0,0,0.9)
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.90f), Color.Transparent),
                            startY = 0f,
                            endY = dishH * 0.50f
                        ),
                        topLeft = Offset.Zero,
                        size = Size(dishW, dishH * 0.50f)
                    )

                    // Vignette circle: transparent 38% -> rgba(0,0,0,0.85) 100%
                    drawCircle(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0.00f to Color.Transparent,
                                0.38f to Color.Transparent,
                                1.00f to Color.Black.copy(alpha = 0.85f)
                            ),
                            center = Offset(dishW / 2f, dishH / 2f),
                            radius = dishW / 2f
                        ),
                        center = Offset(dishW / 2f, dishH / 2f),
                        radius = dishW / 2f
                    )
                }

                // Center Glyph (.lx-g): "L" / "R" elegant optical typography inside 38dp dish
                Text(
                    text = if (isLeft) "L" else "R",
                    color = glowColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
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

            // Outer Glass Lens (.lx-lens): Top chamfer, left reflection, bottom shadow
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
 * Dedicated standalone Compass Thumbstick Click Button (LSB / RSB or L3 / R3).
 * Shares the identical Compass spherical aesthetic:
 * - 8 directional compass pips
 * - Radial dish with neon glyph
 * - Neon cap ring with bloom
 * - Damped spring depression on tap (scales to 0.90, offsets 2dp, triggers haptics and game input)
 */
@Composable
fun CompassStickButton(
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
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "compass_btn_scale"
    )

    val pressOffsetY by animateFloatAsState(
        targetValue = if (isPressed) 2.5f else 0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "compass_btn_offset"
    )

    val ringBloomAlpha by animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0.70f,
        animationSpec = tween(durationMillis = 120),
        label = "compass_btn_ring_bloom"
    )

    val currentOnVibrate by rememberUpdatedState(onVibrate)
    val currentViewModel by rememberUpdatedState(viewModel)

    val glowColor = remember(isRgbEnabled, isLeft) {
        if (isRgbEnabled) {
            if (isLeft) Color(0xFF5AA8FF) else Color(0xFFFF5A88)
        } else {
            if (isLeft) Color(0xFF5AA8FF) else Color(0xFFFF5A88)
        }
    }

    val capDomeGradient = remember {
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

    val dishGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF030304),
                Color(0xFF121314)
            ),
            center = Offset(0.50f, 0.60f),
            radius = 60f
        )
    }

    Box(
        modifier = modifier
            .size(70.dp)
            .graphicsLayer {
                scaleX = scaleAnim
                scaleY = scaleAnim
            }
            .offset { IntOffset(0, pressOffsetY.dp.roundToPx()) }
            .shadow(
                elevation = if (isPressed) 2.dp else 8.dp,
                shape = CircleShape,
                spotColor = if (isRgbEnabled) glowColor else Color.Black,
                ambientColor = if (isRgbEnabled) glowColor.copy(alpha = 0.5f) else Color.Black
            )
            .clip(CircleShape)
            .background(capDomeGradient)
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
        // Internal Well, 8 Pips, Ring, and Lens Highlight
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

            // 8 Compass Pips around button perimeter
            val pipDist = r - 6.dp.toPx()
            val pipRadius = 1.8.dp.toPx()
            for (i in 0 until 8) {
                val angleRad = Math.toRadians((i * 45.0) - 90.0)
                val pipCenter = Offset(
                    center.x + (pipDist * cos(angleRad)).toFloat(),
                    center.y + (pipDist * sin(angleRad)).toFloat()
                )
                drawCircle(
                    color = glowColor.copy(alpha = if (isPressed) 0.65f else 0.30f),
                    radius = pipRadius,
                    center = pipCenter
                )
            }

            // Cap Neon Ring: inset 12px
            val ringRadius = r - 12.dp.toPx()
            drawCircle(
                color = glowColor.copy(alpha = ringBloomAlpha * 0.35f),
                radius = ringRadius,
                style = Stroke(width = 4.dp.toPx())
            )
            drawCircle(
                color = glowColor.copy(alpha = ringBloomAlpha),
                radius = ringRadius,
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

        // Center Dish with Glyph
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(dishGradient)
                .border(1.dp, Color.White.copy(alpha = 0.06f), CircleShape),
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
