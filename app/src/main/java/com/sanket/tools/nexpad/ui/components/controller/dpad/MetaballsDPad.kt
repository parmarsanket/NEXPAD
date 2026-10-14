package com.sanket.tools.nexpad.ui.components.controller.dpad

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.sanket.tools.nexpad.model.NexpadKeys as K
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Snappy 3D physical rocker tilt kinematics for Metaballs D-Pad:
 * UP: rx = +8°, DOWN: rx = -8°
 * RIGHT: ry = +8°, LEFT: ry = -8°
 * Compounded diagonally (e.g. UP + RIGHT: rx = +8°, ry = +8°).
 */
fun calculateMetaballsTilt(pressedDirs: Set<String>): Pair<Float, Float> {
    var vy = 0
    if (pressedDirs.contains(K.DOWN)) vy += 1
    if (pressedDirs.contains(K.UP)) vy -= 1
    val rx = -vy * 8.0f

    var vx = 0
    if (pressedDirs.contains(K.RIGHT)) vx += 1
    if (pressedDirs.contains(K.LEFT)) vx -= 1
    val ry = vx * 8.0f

    return Pair(rx, ry)
}

/**
 * Calculates the displacement offset of a satellite metaball node:
 * Base distance is 58dp. When pressed, it retracts inward by 45% (to ~31.9dp).
 */
fun calculateMetaballSatelliteOffset(direction: String, isPressed: Boolean): Pair<Float, Float> {
    val p = if (isPressed) 1f else 0f
    val factor = 1f - p * 0.45f
    val baseDist = 58f
    val dist = baseDist * factor

    return when (direction) {
        K.UP -> Pair(0f, -dist)
        K.RIGHT -> Pair(dist, 0f)
        K.DOWN -> Pair(0f, dist)
        K.LEFT -> Pair(-dist, 0f)
        else -> Pair(0f, 0f)
    }
}

/**
 * Resolves touch coordinate on 172dp Metaballs D-Pad stage strictly to the four satellite pods.
 * Empty spaces (diagonal corner voids), center hub deadzone (14dp), and outer boundary (>76dp)
 * are non-clickable, preventing diagonal clicks and ensuring single-direction activation.
 */
fun resolveMetaballsTouch(pos: Offset, sizePx: Float): Set<String> {
    val centerX = sizePx / 2f
    val centerY = sizePx / 2f
    val dx = pos.x - centerX
    val dy = pos.y - centerY
    val scale = sizePx / 172f
    val armHalfWidth = 22f * scale
    val armMaxReach = 76f * scale
    val centerDeadzone = 14f * scale

    val isUp = dy in (-armMaxReach)..(-centerDeadzone) && abs(dx) <= armHalfWidth
    val isDown = dy in centerDeadzone..armMaxReach && abs(dx) <= armHalfWidth
    val isLeft = dx in (-armMaxReach)..(-centerDeadzone) && abs(dy) <= armHalfWidth
    val isRight = dx in centerDeadzone..armMaxReach && abs(dy) <= armHalfWidth

    return when {
        isUp && isRight -> if (abs(dy) >= abs(dx)) setOf(K.UP) else setOf(K.RIGHT)
        isUp && isLeft  -> if (abs(dy) >= abs(dx)) setOf(K.UP) else setOf(K.LEFT)
        isDown && isRight -> if (abs(dy) >= abs(dx)) setOf(K.DOWN) else setOf(K.RIGHT)
        isDown && isLeft  -> if (abs(dy) >= abs(dx)) setOf(K.DOWN) else setOf(K.LEFT)
        isUp -> setOf(K.UP)
        isDown -> setOf(K.DOWN)
        isLeft -> setOf(K.LEFT)
        isRight -> setOf(K.RIGHT)
        else -> emptySet()
    }
}

/**
 * High-performance, tactile console-grade Metaballs D-Pad ("D-Pad — Metaballs"):
 * - 172dp circular stage with convex acrylic dome body (#232527 -> #0C0D0E -> #000000).
 * - 3D physical rocker tilt kinematics (perspective 420px, rotateX -vy*8°, rotateY vx*8°, 80ms ease).
 * - Organic liquid neon metaball layer (.goo2):
 *   - Idle: Clean, pristine floating optical pods with glowing cyan halos peeking out from under dark caps.
 *   - Press: Inward spring plunge (58dp -> ~31.9dp) with dynamic liquid neck fusion into central orb.
 * - Central 24dp dark tactile cap with glowing 6dp neon pip.
 * - Four 36dp tactile satellite caps with vector directional chevrons and active bloom on press.
 * - Outer neon ring (.lx-ring, #3FD2C4) and top specular optical glass lens (.lx-lens).
 */
@Composable
fun MetaballsDPad(
    isConnected: Boolean = true,
    viewModel: GamepadViewModel? = null,
    isRgbEnabled: Boolean = false,
    onVibrate: () -> Unit = {},
    modifier: Modifier = Modifier,
    isInteractive: Boolean = true
) {
    var pressedDirs by remember { mutableStateOf<Set<String>>(emptySet()) }
    val currentlyPressed = pressedDirs

    val currentOnVibrate by rememberUpdatedState(onVibrate)
    val currentViewModel by rememberUpdatedState(viewModel)

    val (targetRx, targetRy) = remember(currentlyPressed) {
        calculateMetaballsTilt(currentlyPressed)
    }

    val isAnyPressed = currentlyPressed.isNotEmpty()

    // 80ms ease for 3D physical rocker tilt
    val rxAnimState = animateFloatAsState(
        targetValue = targetRx,
        animationSpec = tween(durationMillis = 80, easing = FastOutSlowInEasing),
        label = "meta_tilt_rx"
    )
    val ryAnimState = animateFloatAsState(
        targetValue = targetRy,
        animationSpec = tween(durationMillis = 80, easing = FastOutSlowInEasing),
        label = "meta_tilt_ry"
    )

    // Plunging spring kinematics for overall stage
    val stageScaleAnimState = animateFloatAsState(
        targetValue = if (isAnyPressed) 0.95f else 1.0f,
        animationSpec = tween(durationMillis = 80, easing = FastOutSlowInEasing),
        label = "meta_stage_scale"
    )
    val stageOffsetYAnimState = animateFloatAsState(
        targetValue = if (isAnyPressed) 2.0f else 0f,
        animationSpec = tween(durationMillis = 80, easing = FastOutSlowInEasing),
        label = "meta_stage_offset_y"
    )

    // Dynamic satellite retraction springs (58dp -> ~31.9dp on press)
    val upProgressState = animateFloatAsState(
        targetValue = if (currentlyPressed.contains(K.UP)) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 340f),
        label = "meta_sat_up"
    )
    val rightProgressState = animateFloatAsState(
        targetValue = if (currentlyPressed.contains(K.RIGHT)) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 340f),
        label = "meta_sat_right"
    )
    val downProgressState = animateFloatAsState(
        targetValue = if (currentlyPressed.contains(K.DOWN)) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 340f),
        label = "meta_sat_down"
    )
    val leftProgressState = animateFloatAsState(
        targetValue = if (currentlyPressed.contains(K.LEFT)) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 340f),
        label = "meta_sat_left"
    )

    // Neon Glow Color: #3FD2C4 (Electric Teal / Turquoise)
    val neonColor = remember(isRgbEnabled) {
        if (isRgbEnabled) Color(0xFF3FD2C4) else Color(0xFF3FD2C4)
    }

    val rgbBloomAlphaState = animateFloatAsState(
        targetValue = if (isAnyPressed) 0.95f else 0.45f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "meta_rgb_bloom"
    )

    val density = LocalDensity.current.density

    // Dome base gradient: radial-gradient(circle at 50% 55%, #232527 0%, #0c0d0e 75%, #000 100%)
    val domeGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF232527),
                Color(0xFF0C0D0E),
                Color(0xFF000000)
            ),
            center = Offset(0.50f, 0.55f),
            radius = 340f
        )
    }

    val pressedDomeGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF1B1D1E),
                Color(0xFF070808),
                Color(0xFF000000)
            ),
            center = Offset(0.50f, 0.58f),
            radius = 340f
        )
    }

    // Tactile cap gradient: radial-gradient(circle at 50% 38%, #2f3134, #0a0b0c 80%)
    val capGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF2F3134),
                Color(0xFF141517),
                Color(0xFF0A0B0C)
            ),
            center = Offset(0.50f, 0.38f),
            radius = 50f
        )
    }

    val coreBloomColors = remember(neonColor) {
        arrayListOf(Color.Transparent, Color.Transparent, Color.Transparent)
    }
    val bridgeColors = remember(neonColor) {
        arrayListOf(Color.Transparent, Color.Transparent)
    }
    val satBloomStops = remember(neonColor) {
        arrayOf(
            0.00f to Color.Transparent,
            0.45f to Color.Transparent,
            1.00f to Color.Transparent
        )
    }

    val undercutColorsPressed = remember { listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)) }
    val undercutColorsIdle = remember { listOf(Color.Transparent, Color.Black.copy(alpha = 0.70f)) }

    val bridgePath = remember { Path() }
    val strokeBridgeHalo = remember(density) { Stroke(width = 6f * density, cap = StrokeCap.Round, join = StrokeJoin.Round) }

    val pipHighlightColors = remember { listOf(Color.White.copy(alpha = 0.16f), Color.Transparent) }
    val strokePipHighlight = remember(density) { Stroke(width = 1f * density) }

    val strokeRingOuterPressed = remember(density) { Stroke(width = 5f * density) }
    val strokeRingOuterIdle = remember(density) { Stroke(width = 3f * density) }
    val strokeRingCore = remember(density) { Stroke(width = 2f * density) }
    val strokeLensArc = remember(density) { Stroke(width = 1f * density) }

    val lensColorsPressed = remember {
        listOf(
            Color.White.copy(alpha = 0.06f),
            Color.White.copy(alpha = 0.02f),
            Color.Transparent
        )
    }
    val lensColorsIdle = remember {
        listOf(
            Color.White.copy(alpha = 0.14f),
            Color.White.copy(alpha = 0.05f),
            Color.Transparent
        )
    }
    val specularSheenColors = remember {
        listOf(
            Color.White.copy(alpha = 0.06f),
            Color.Transparent
        )
    }

    Box(
        modifier = modifier
            .size(172.dp)
            .graphicsLayer {
                // Root RenderNode isolation
            }
            .drawBehind {
                if (isRgbEnabled) {
                    val upProgress = upProgressState.value
                    val downProgress = downProgressState.value
                    val leftProgress = leftProgressState.value
                    val rightProgress = rightProgressState.value
                    val rgbBloomAlpha = rgbBloomAlphaState.value

                    // 1. Central viscous fluid reservoir
                    val coreRadius = size.minDimension * 0.42f
                    coreBloomColors[0] = neonColor.copy(alpha = rgbBloomAlpha * 0.50f)
                    coreBloomColors[1] = neonColor.copy(alpha = rgbBloomAlpha * 0.18f)
                    coreBloomColors[2] = Color.Transparent
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = coreBloomColors,
                            center = center,
                            radius = coreRadius
                        ),
                        radius = coreRadius,
                        center = center
                    )

                    // 2. Viscous satellite fluid fields swelling toward plunging centers
                    val baseDist = 58f * density
                    val plungeTravel = 26f * density

                    // UP
                    var isPlunging = upProgress > 0.05f
                    var dist = baseDist - plungeTravel * upProgress
                    var satPos = Offset(center.x, center.y - dist)
                    var satR = 24f * density + (12f * density * upProgress)
                    var satAlpha = if (isPlunging) rgbBloomAlpha * 0.90f else (if (isAnyPressed) 0.20f else 0.40f)
                    if (isPlunging) {
                        bridgeColors[0] = neonColor.copy(alpha = satAlpha * 0.7f)
                        bridgeColors[1] = Color.White.copy(alpha = satAlpha * 0.85f)
                        drawLine(
                            brush = Brush.linearGradient(colors = bridgeColors, start = center, end = satPos),
                            start = center, end = satPos,
                            strokeWidth = 20f * density * upProgress
                        )
                    }
                    satBloomStops[0] = 0.00f to (if (isPlunging) Color.White else neonColor).copy(alpha = satAlpha * 0.70f)
                    satBloomStops[1] = 0.45f to neonColor.copy(alpha = satAlpha * 0.35f)
                    satBloomStops[2] = 1.00f to Color.Transparent
                    drawCircle(
                        brush = Brush.radialGradient(colorStops = satBloomStops, center = satPos, radius = satR * 1.35f),
                        radius = satR * 1.35f,
                        center = satPos
                    )

                    // DOWN
                    isPlunging = downProgress > 0.05f
                    dist = baseDist - plungeTravel * downProgress
                    satPos = Offset(center.x, center.y + dist)
                    satR = 24f * density + (12f * density * downProgress)
                    satAlpha = if (isPlunging) rgbBloomAlpha * 0.90f else (if (isAnyPressed) 0.20f else 0.40f)
                    if (isPlunging) {
                        bridgeColors[0] = neonColor.copy(alpha = satAlpha * 0.7f)
                        bridgeColors[1] = Color.White.copy(alpha = satAlpha * 0.85f)
                        drawLine(
                            brush = Brush.linearGradient(colors = bridgeColors, start = center, end = satPos),
                            start = center, end = satPos,
                            strokeWidth = 20f * density * downProgress
                        )
                    }
                    satBloomStops[0] = 0.00f to (if (isPlunging) Color.White else neonColor).copy(alpha = satAlpha * 0.70f)
                    satBloomStops[1] = 0.45f to neonColor.copy(alpha = satAlpha * 0.35f)
                    satBloomStops[2] = 1.00f to Color.Transparent
                    drawCircle(
                        brush = Brush.radialGradient(colorStops = satBloomStops, center = satPos, radius = satR * 1.35f),
                        radius = satR * 1.35f,
                        center = satPos
                    )

                    // LEFT
                    isPlunging = leftProgress > 0.05f
                    dist = baseDist - plungeTravel * leftProgress
                    satPos = Offset(center.x - dist, center.y)
                    satR = 24f * density + (12f * density * leftProgress)
                    satAlpha = if (isPlunging) rgbBloomAlpha * 0.90f else (if (isAnyPressed) 0.20f else 0.40f)
                    if (isPlunging) {
                        bridgeColors[0] = neonColor.copy(alpha = satAlpha * 0.7f)
                        bridgeColors[1] = Color.White.copy(alpha = satAlpha * 0.85f)
                        drawLine(
                            brush = Brush.linearGradient(colors = bridgeColors, start = center, end = satPos),
                            start = center, end = satPos,
                            strokeWidth = 20f * density * leftProgress
                        )
                    }
                    satBloomStops[0] = 0.00f to (if (isPlunging) Color.White else neonColor).copy(alpha = satAlpha * 0.70f)
                    satBloomStops[1] = 0.45f to neonColor.copy(alpha = satAlpha * 0.35f)
                    satBloomStops[2] = 1.00f to Color.Transparent
                    drawCircle(
                        brush = Brush.radialGradient(colorStops = satBloomStops, center = satPos, radius = satR * 1.35f),
                        radius = satR * 1.35f,
                        center = satPos
                    )

                    // RIGHT
                    isPlunging = rightProgress > 0.05f
                    dist = baseDist - plungeTravel * rightProgress
                    satPos = Offset(center.x + dist, center.y)
                    satR = 24f * density + (12f * density * rightProgress)
                    satAlpha = if (isPlunging) rgbBloomAlpha * 0.90f else (if (isAnyPressed) 0.20f else 0.40f)
                    if (isPlunging) {
                        bridgeColors[0] = neonColor.copy(alpha = satAlpha * 0.7f)
                        bridgeColors[1] = Color.White.copy(alpha = satAlpha * 0.85f)
                        drawLine(
                            brush = Brush.linearGradient(colors = bridgeColors, start = center, end = satPos),
                            start = center, end = satPos,
                            strokeWidth = 20f * density * rightProgress
                        )
                    }
                    satBloomStops[0] = 0.00f to (if (isPlunging) Color.White else neonColor).copy(alpha = satAlpha * 0.70f)
                    satBloomStops[1] = 0.45f to neonColor.copy(alpha = satAlpha * 0.35f)
                    satBloomStops[2] = 1.00f to Color.Transparent
                    drawCircle(
                        brush = Brush.radialGradient(colorStops = satBloomStops, center = satPos, radius = satR * 1.35f),
                        radius = satR * 1.35f,
                        center = satPos
                    )
                }
            }
            .then(
                if (isInteractive) {
                    Modifier.pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()
                            var activeDirs = emptySet<String>()

                            fun evaluateOffset(pos: Offset) {
                                val newDirs = resolveMetaballsTouch(pos, size.width.toFloat())
                                if (newDirs != activeDirs) {
                                    val added = newDirs - activeDirs
                                    val removed = activeDirs - newDirs
                                    removed.forEach { dir -> currentViewModel?.updateButton(dir, false) }
                                    added.forEach { dir -> currentViewModel?.updateButton(dir, true) }
                                    if (added.isNotEmpty()) currentOnVibrate()
                                    activeDirs = newDirs
                                    pressedDirs = newDirs
                                }
                            }

                            try {
                                evaluateOffset(down.position)
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val pointer = event.changes.firstOrNull { it.id == down.id }
                                    if (pointer == null || !pointer.pressed) break
                                    pointer.consume()
                                    evaluateOffset(pointer.position)
                                }
                            } finally {
                                activeDirs.forEach { dir -> currentViewModel?.updateButton(dir, false) }
                                pressedDirs = emptySet()
                            }
                        }
                    }
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        // --- 1. Tiltable 3D Physical Rocker Body Container ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(
                    elevation = if (isAnyPressed) 3.dp else 8.dp,
                    shape = CircleShape,
                    spotColor = if (isRgbEnabled) neonColor else Color.Black,
                    ambientColor = if (isRgbEnabled) neonColor else Color.Black
                )
                .graphicsLayer {
                    scaleX = stageScaleAnimState.value
                    scaleY = stageScaleAnimState.value
                    translationY = stageOffsetYAnimState.value * density
                    rotationX = rxAnimState.value
                    rotationY = ryAnimState.value
                    cameraDistance = 12f * density
                }
                .clip(CircleShape)
                .background(if (isAnyPressed) pressedDomeGradient else domeGradient)
                .border(
                    width = 1.dp,
                    color = Color.Black.copy(alpha = 0.50f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Background Inset Shadow Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Inset bottom undercut shadow: inset 0 -6px 9px rgba(0,0,0,.70)
                val insetH = h * 0.35f
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = if (isAnyPressed) undercutColorsPressed else undercutColorsIdle,
                        startY = h - insetH,
                        endY = h
                    ),
                    topLeft = Offset(0f, h - insetH),
                    size = Size(w, insetH)
                )
            }

            // --- 2. Dynamic Liquid Neon Metaballs Layer (.goo2) ---
            Canvas(modifier = Modifier.fillMaxSize()) {
                val upProgress = upProgressState.value
                val rightProgress = rightProgressState.value
                val downProgress = downProgressState.value
                val leftProgress = leftProgressState.value

                val center = Offset(size.width / 2f, size.height / 2f)
                val baseDistPx = 58f * density
                val rCenterPx = 19f * density // c0 radius (diameter 38dp)
                val rSatPx = 23f * density    // sb radius (diameter 46dp)
                val maxBridgeDist = 54f * density
                val minBridgeDist = 31f * density
                val bridgeDistDiff = maxBridgeDist - minBridgeDist

                val dUp = baseDistPx * (1f - upProgress * 0.45f)
                val dRight = baseDistPx * (1f - rightProgress * 0.45f)
                val dDown = baseDistPx * (1f - downProgress * 0.45f)
                val dLeft = baseDistPx * (1f - leftProgress * 0.45f)

                // 1. Draw dynamic organic fluid bridge ONLY for satellites currently plunging inward
                // UP (-90°)
                if (upProgress > 0.05f && dUp < maxBridgeDist) {
                    val blend = ((maxBridgeDist - dUp) / bridgeDistDiff).coerceIn(0f, 1f)
                    withTransform({ rotate(degrees = -90f, pivot = center) }) {
                        val wCenter = rCenterPx * (0.60f + 0.35f * blend)
                        val wSat = rSatPx * (0.60f + 0.35f * blend)
                        val wMid = 6f * density + 10f * density * blend
                        bridgePath.reset()
                        bridgePath.moveTo(0f, -wCenter)
                        bridgePath.cubicTo(dUp * 0.35f, -wMid, dUp * 0.65f, -wMid, dUp, -wSat)
                        bridgePath.lineTo(dUp, wSat)
                        bridgePath.cubicTo(dUp * 0.65f, wMid, dUp * 0.35f, wMid, 0f, wCenter)
                        bridgePath.close()
                        drawPath(bridgePath, color = neonColor.copy(alpha = 0.35f * blend), style = strokeBridgeHalo)
                        drawPath(bridgePath, color = neonColor.copy(alpha = 0.92f * blend))
                    }
                }
                // RIGHT (0°)
                if (rightProgress > 0.05f && dRight < maxBridgeDist) {
                    val blend = ((maxBridgeDist - dRight) / bridgeDistDiff).coerceIn(0f, 1f)
                    withTransform({ rotate(degrees = 0f, pivot = center) }) {
                        val wCenter = rCenterPx * (0.60f + 0.35f * blend)
                        val wSat = rSatPx * (0.60f + 0.35f * blend)
                        val wMid = 6f * density + 10f * density * blend
                        bridgePath.reset()
                        bridgePath.moveTo(0f, -wCenter)
                        bridgePath.cubicTo(dRight * 0.35f, -wMid, dRight * 0.65f, -wMid, dRight, -wSat)
                        bridgePath.lineTo(dRight, wSat)
                        bridgePath.cubicTo(dRight * 0.65f, wMid, dRight * 0.35f, wMid, 0f, wCenter)
                        bridgePath.close()
                        drawPath(bridgePath, color = neonColor.copy(alpha = 0.35f * blend), style = strokeBridgeHalo)
                        drawPath(bridgePath, color = neonColor.copy(alpha = 0.92f * blend))
                    }
                }
                // DOWN (90°)
                if (downProgress > 0.05f && dDown < maxBridgeDist) {
                    val blend = ((maxBridgeDist - dDown) / bridgeDistDiff).coerceIn(0f, 1f)
                    withTransform({ rotate(degrees = 90f, pivot = center) }) {
                        val wCenter = rCenterPx * (0.60f + 0.35f * blend)
                        val wSat = rSatPx * (0.60f + 0.35f * blend)
                        val wMid = 6f * density + 10f * density * blend
                        bridgePath.reset()
                        bridgePath.moveTo(0f, -wCenter)
                        bridgePath.cubicTo(dDown * 0.35f, -wMid, dDown * 0.65f, -wMid, dDown, -wSat)
                        bridgePath.lineTo(dDown, wSat)
                        bridgePath.cubicTo(dDown * 0.65f, wMid, dDown * 0.35f, wMid, 0f, wCenter)
                        bridgePath.close()
                        drawPath(bridgePath, color = neonColor.copy(alpha = 0.35f * blend), style = strokeBridgeHalo)
                        drawPath(bridgePath, color = neonColor.copy(alpha = 0.92f * blend))
                    }
                }
                // LEFT (180°)
                if (leftProgress > 0.05f && dLeft < maxBridgeDist) {
                    val blend = ((maxBridgeDist - dLeft) / bridgeDistDiff).coerceIn(0f, 1f)
                    withTransform({ rotate(degrees = 180f, pivot = center) }) {
                        val wCenter = rCenterPx * (0.60f + 0.35f * blend)
                        val wSat = rSatPx * (0.60f + 0.35f * blend)
                        val wMid = 6f * density + 10f * density * blend
                        bridgePath.reset()
                        bridgePath.moveTo(0f, -wCenter)
                        bridgePath.cubicTo(dLeft * 0.35f, -wMid, dLeft * 0.65f, -wMid, dLeft, -wSat)
                        bridgePath.lineTo(dLeft, wSat)
                        bridgePath.cubicTo(dLeft * 0.65f, wMid, dLeft * 0.35f, wMid, 0f, wCenter)
                        bridgePath.close()
                        drawPath(bridgePath, color = neonColor.copy(alpha = 0.35f * blend), style = strokeBridgeHalo)
                        drawPath(bridgePath, color = neonColor.copy(alpha = 0.92f * blend))
                    }
                }

                // 2. Central fluid circle (.c0, diameter 38dp)
                drawCircle(
                    color = neonColor.copy(alpha = 0.30f),
                    radius = rCenterPx + 2.5f * density,
                    center = center
                )
                drawCircle(
                    color = neonColor.copy(alpha = 0.92f),
                    radius = rCenterPx,
                    center = center
                )

                // 3. Satellite fluid circles (.sb, diameter 46dp)
                // UP
                var satPos = Offset(center.x, center.y - dUp)
                var isDirPressed = upProgress > 0.05f
                drawCircle(
                    color = neonColor.copy(alpha = if (isDirPressed) 0.55f else 0.30f),
                    radius = rSatPx + (if (isDirPressed) 4f * density else 2.5f * density),
                    center = satPos
                )
                drawCircle(color = neonColor.copy(alpha = 0.92f), radius = rSatPx, center = satPos)

                // RIGHT
                satPos = Offset(center.x + dRight, center.y)
                isDirPressed = rightProgress > 0.05f
                drawCircle(
                    color = neonColor.copy(alpha = if (isDirPressed) 0.55f else 0.30f),
                    radius = rSatPx + (if (isDirPressed) 4f * density else 2.5f * density),
                    center = satPos
                )
                drawCircle(color = neonColor.copy(alpha = 0.92f), radius = rSatPx, center = satPos)

                // DOWN
                satPos = Offset(center.x, center.y + dDown)
                isDirPressed = downProgress > 0.05f
                drawCircle(
                    color = neonColor.copy(alpha = if (isDirPressed) 0.55f else 0.30f),
                    radius = rSatPx + (if (isDirPressed) 4f * density else 2.5f * density),
                    center = satPos
                )
                drawCircle(color = neonColor.copy(alpha = 0.92f), radius = rSatPx, center = satPos)

                // LEFT
                satPos = Offset(center.x - dLeft, center.y)
                isDirPressed = leftProgress > 0.05f
                drawCircle(
                    color = neonColor.copy(alpha = if (isDirPressed) 0.55f else 0.30f),
                    radius = rSatPx + (if (isDirPressed) 4f * density else 2.5f * density),
                    center = satPos
                )
                drawCircle(color = neonColor.copy(alpha = 0.92f), radius = rSatPx, center = satPos)
            }

            // --- 3. Caps Layer (.capl) ---
            // Central tactile cap (.cp2.c0, 24dp x 24dp at center)
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .shadow(4.dp, CircleShape, spotColor = Color.Black, ambientColor = Color.Black)
                    .clip(CircleShape)
                    .background(capGradient)
                    .border(1.dp, Color.Black.copy(alpha = 0.65f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Central 6dp glowing neon pip
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val pcenter = Offset(size.width / 2f, size.height / 2f)
                    val pipRadius = 3f * density
                    drawCircle(
                        color = neonColor.copy(alpha = 0.40f),
                        radius = pipRadius + 2f * density,
                        center = pcenter
                    )
                    drawCircle(
                        color = neonColor.copy(alpha = 0.85f),
                        radius = pipRadius,
                        center = pcenter
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.75f),
                        radius = pipRadius * 0.45f,
                        center = pcenter
                    )

                    // Top specular highlight arc
                    drawArc(
                        brush = Brush.verticalGradient(
                            colors = pipHighlightColors,
                            startY = 0f,
                            endY = size.height * 0.40f
                        ),
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(1f * density, 1f * density),
                        size = Size(size.width - 2f * density, size.height - 2f * density),
                        style = strokePipHighlight
                    )
                }
            }

            // Four Satellite Caps (.cp2.sat, 36dp x 36dp with directional chevrons)
            // 1. UP
            SatelliteCap(
                direction = K.UP,
                isPressed = currentlyPressed.contains(K.UP),
                glowColor = neonColor,
                capGradient = capGradient,
                rotationAngle = 0f,
                modifier = Modifier.offset {
                    val upDistPx = 58f * density * (1f - upProgressState.value * 0.45f)
                    IntOffset(0, -upDistPx.roundToInt())
                }
            )

            // 2. RIGHT
            SatelliteCap(
                direction = K.RIGHT,
                isPressed = currentlyPressed.contains(K.RIGHT),
                glowColor = neonColor,
                capGradient = capGradient,
                rotationAngle = 90f,
                modifier = Modifier.offset {
                    val rightDistPx = 58f * density * (1f - rightProgressState.value * 0.45f)
                    IntOffset(rightDistPx.roundToInt(), 0)
                }
            )

            // 3. DOWN
            SatelliteCap(
                direction = K.DOWN,
                isPressed = currentlyPressed.contains(K.DOWN),
                glowColor = neonColor,
                capGradient = capGradient,
                rotationAngle = 180f,
                modifier = Modifier.offset {
                    val downDistPx = 58f * density * (1f - downProgressState.value * 0.45f)
                    IntOffset(0, downDistPx.roundToInt())
                }
            )

            // 4. LEFT
            SatelliteCap(
                direction = K.LEFT,
                isPressed = currentlyPressed.contains(K.LEFT),
                glowColor = neonColor,
                capGradient = capGradient,
                rotationAngle = 270f,
                modifier = Modifier.offset {
                    val leftDistPx = 58f * density * (1f - leftProgressState.value * 0.45f)
                    IntOffset(-leftDistPx.roundToInt(), 0)
                }
            )
        }

        // --- 4. Outer Neon Ring (.lx-ring) & Top Specular Glass Lens (.lx-lens) ---
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val r = size.minDimension / 2f
            val center = Offset(w / 2f, h / 2f)
            val ringInset = 3f * density
            val ringR = r - ringInset

            // Outer Neon Ring: border 2px solid var(--glow), opacity 0.3 idle, 1.0 pressed
            val ringAlpha = if (isAnyPressed) 0.85f else 0.30f
            drawCircle(
                color = neonColor.copy(alpha = ringAlpha * 0.40f),
                radius = ringR,
                center = center,
                style = if (isAnyPressed) strokeRingOuterPressed else strokeRingOuterIdle
            )
            drawCircle(
                color = neonColor.copy(alpha = ringAlpha),
                radius = ringR,
                center = center,
                style = strokeRingCore
            )

            // Top specular optical glass lens (.lx-lens)
            drawArc(
                brush = Brush.verticalGradient(
                    colors = if (isAnyPressed) lensColorsPressed else lensColorsIdle,
                    startY = 0f,
                    endY = h * 0.42f
                ),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(1f * density, 1f * density),
                size = Size(w - 2f * density, h - 2f * density),
                style = strokeLensArc
            )

            // Bottom-right specular reflection sheen
            drawCircle(
                brush = Brush.radialGradient(
                    colors = specularSheenColors,
                    center = Offset(w * 0.70f, h * 0.78f),
                    radius = w * 0.28f
                ),
                center = Offset(w * 0.70f, h * 0.78f),
                radius = w * 0.28f
            )

            // Chamfer highlights: top 1px rgba(255,255,255,0.12), bottom 1px rgba(0,0,0,0.30)
            drawArc(
                color = Color.White.copy(alpha = 0.12f),
                startAngle = 200f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(1.5f * density, 1.5f * density),
                size = Size(w - 3f * density, h - 3f * density),
                style = strokeLensArc
            )
            drawArc(
                color = Color.Black.copy(alpha = 0.30f),
                startAngle = 20f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(1.5f * density, 1.5f * density),
                size = Size(w - 3f * density, h - 3f * density),
                style = strokeLensArc
            )
        }
    }
}

/**
 * Satellite tactile cap for the Metaballs D-Pad:
 * 36dp circular cap with directional chevron, drop shadow, and active neon bloom.
 */
@Composable
fun SatelliteCap(
    direction: String,
    isPressed: Boolean,
    glowColor: Color,
    capGradient: Brush,
    rotationAngle: Float,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current.density
    val chevronPath = remember(density) {
        val cx = 18f * density
        val cy = 18f * density
        val s = (18f * density) / 24f
        Path().apply {
            moveTo(cx - 6f * s, cy + 3f * s)
            lineTo(cx, cy - 3f * s)
            lineTo(cx + 6f * s, cy + 3f * s)
        }
    }
    val strokeChevronBloom = remember(density) {
        val s = (18f * density) / 24f
        Stroke(
            width = 6f * s,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    }
    val strokeChevronCore = remember(density) {
        val s = (18f * density) / 24f
        Stroke(
            width = 3.4f * s,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    }
    val strokeCapHighlight = remember(density) { Stroke(width = 1f * density) }
    val capHighlightColors = remember { listOf(Color.White.copy(alpha = 0.16f), Color.Transparent) }

    Box(
        modifier = modifier
            .size(36.dp)
            .shadow(
                elevation = if (isPressed) 2.dp else 4.dp,
                shape = CircleShape,
                spotColor = Color.Black.copy(alpha = 0.55f),
                ambientColor = Color.Black.copy(alpha = 0.40f)
            )
            .clip(CircleShape)
            .background(capGradient)
            .border(
                width = 1.dp,
                color = if (isPressed) glowColor.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.65f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)

            withTransform({
                rotate(degrees = rotationAngle, pivot = center)
            }) {
                // Active bloom shadow
                if (isPressed) {
                    drawPath(
                        path = chevronPath,
                        color = glowColor.copy(alpha = 0.65f),
                        style = strokeChevronBloom
                    )
                }
                // Crisp core chevron
                drawPath(
                    path = chevronPath,
                    color = if (isPressed) Color.White else glowColor,
                    style = strokeChevronCore
                )
            }

            // Inset highlight arc
            drawArc(
                brush = Brush.verticalGradient(
                    colors = capHighlightColors,
                    startY = 0f,
                    endY = size.height * 0.45f
                ),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(1f * density, 1f * density),
                size = Size(size.width - 2f * density, size.height - 2f * density),
                style = strokeCapHighlight
            )
        }
    }
}

@Composable
fun StaticMetaballsDPad(
    isRgbEnabled: Boolean = false,
    modifier: Modifier = Modifier
) {
    MetaballsDPad(
        isConnected = false,
        viewModel = null,
        isRgbEnabled = isRgbEnabled,
        onVibrate = {},
        modifier = modifier,
        isInteractive = false
    )
}

