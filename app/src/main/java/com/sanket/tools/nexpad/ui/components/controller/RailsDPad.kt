package com.sanket.tools.nexpad.ui.components.controller

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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.drawscope.Stroke
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
 * Snappy 3D physical rocker tilt kinematics for Rails D-Pad:
 * UP: rx = +8°, DOWN: rx = -8°
 * RIGHT: ry = +8°, LEFT: ry = -8°
 * Compounded diagonally (e.g. UP + RIGHT: rx = +8°, ry = +8°).
 */
fun calculateRailsTilt(pressedDirs: Set<String>): Pair<Float, Float> {
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
 * Calculates the 2D sliding puck travel offset along the orthogonal rails.
 * Displacement travel is 52dp along active direction axes (vx * 52dp, vy * 52dp).
 */
fun calculateRailsPuckOffset(pressedDirs: Set<String>): Pair<Float, Float> {
    var vx = 0
    if (pressedDirs.contains(K.RIGHT)) vx += 1
    if (pressedDirs.contains(K.LEFT)) vx -= 1

    var vy = 0
    if (pressedDirs.contains(K.DOWN)) vy += 1
    if (pressedDirs.contains(K.UP)) vy -= 1

    val travel = 52f
    return Pair(vx * travel, vy * travel)
}

/**
 * Resolves touch coordinate on 160dp Rails D-Pad stage strictly to the orthogonal recessed rails.
 * Empty spaces (diagonal corner voids), center sliding puck deadzone (14dp), and outer boundary (>72dp)
 * are non-clickable, preventing diagonal clicks and ensuring single-direction activation.
 */
fun resolveRailsTouch(pos: Offset, sizePx: Float): Set<String> {
    val centerX = sizePx / 2f
    val centerY = sizePx / 2f
    val dx = pos.x - centerX
    val dy = pos.y - centerY
    val scale = sizePx / 160f
    val armHalfWidth = 20f * scale
    val armMaxReach = 72f * scale
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
 * High-performance, tactile console-grade Rails D-Pad ("NEXPAD D-Pad — Rails"):
 * - 160dp circular stage with convex acrylic dome body (#232527 -> #0C0D0E -> #000000).
 * - 3D physical rocker tilt kinematics (perspective 420px, rotateX -vy*8°, rotateY vx*8°, 80ms ease).
 * - Recessed orthogonal guide rails (.rail-h and .rail-v) with guide groove lines.
 * - Dynamic light beams (.lit) projecting up to 56dp along active rails with emissive glow bloom.
 * - Four end target LED pips (.ed) at rail terminuses with active neon bloom on press.
 * - Central 40dp sliding tactile puck (.rail-puck) with spring kinematics (overshoot damping 0.55, stiffness 380).
 * - Outer neon ring (.lx-ring, #FFB13F) and top specular optical glass lens (.lx-lens).
 */
@Composable
fun RailsDPad(
    isConnected: Boolean,
    viewModel: GamepadViewModel?,
    isRgbEnabled: Boolean = false,
    onVibrate: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var pressedDirs by remember { mutableStateOf<Set<String>>(emptySet()) }
    val currentlyPressed = pressedDirs

    val currentOnVibrate by rememberUpdatedState(onVibrate)
    val currentViewModel by rememberUpdatedState(viewModel)

    val (targetRx, targetRy) = remember(currentlyPressed) {
        calculateRailsTilt(currentlyPressed)
    }

    val (targetPuckX, targetPuckY) = remember(currentlyPressed) {
        calculateRailsPuckOffset(currentlyPressed)
    }

    val isAnyPressed = currentlyPressed.isNotEmpty()

    // 80ms ease for 3D physical rocker tilt
    val rxAnim by animateFloatAsState(
        targetValue = targetRx,
        animationSpec = tween(durationMillis = 80, easing = FastOutSlowInEasing),
        label = "rails_tilt_rx"
    )
    val ryAnim by animateFloatAsState(
        targetValue = targetRy,
        animationSpec = tween(durationMillis = 80, easing = FastOutSlowInEasing),
        label = "rails_tilt_ry"
    )

    // Plunging spring kinematics for overall stage
    val stageScaleAnim by animateFloatAsState(
        targetValue = if (isAnyPressed) 0.95f else 1.0f,
        animationSpec = tween(durationMillis = 80, easing = FastOutSlowInEasing),
        label = "rails_stage_scale"
    )
    val stageOffsetYAnim by animateFloatAsState(
        targetValue = if (isAnyPressed) 2.0f else 0f,
        animationSpec = tween(durationMillis = 80, easing = FastOutSlowInEasing),
        label = "rails_stage_offset_y"
    )

    // Sliding Puck Spring Kinematics (dampingRatio 0.55, stiffness 380 matches cubic-bezier(.3, 1.5, .5, 1))
    val puckOffsetXAnim by animateFloatAsState(
        targetValue = targetPuckX,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 380f),
        label = "rails_puck_x"
    )
    val puckOffsetYAnim by animateFloatAsState(
        targetValue = targetPuckY,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 380f),
        label = "rails_puck_y"
    )

    // Light Beam Expansion (0 to 56dp, 180ms ease-out)
    val litRightWidth by animateFloatAsState(
        targetValue = if (currentlyPressed.contains(K.RIGHT)) 56f else 0f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "rails_lit_r"
    )
    val litLeftWidth by animateFloatAsState(
        targetValue = if (currentlyPressed.contains(K.LEFT)) 56f else 0f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "rails_lit_l"
    )
    val litDownHeight by animateFloatAsState(
        targetValue = if (currentlyPressed.contains(K.DOWN)) 56f else 0f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "rails_lit_d"
    )
    val litUpHeight by animateFloatAsState(
        targetValue = if (currentlyPressed.contains(K.UP)) 56f else 0f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "rails_lit_u"
    )

    // End LED Opacity Transitions (0.28 to 1.0, 100ms ease)
    val ledUpAlpha by animateFloatAsState(
        targetValue = if (currentlyPressed.contains(K.UP)) 1.0f else 0.28f,
        animationSpec = tween(durationMillis = 100),
        label = "rails_led_u"
    )
    val ledDownAlpha by animateFloatAsState(
        targetValue = if (currentlyPressed.contains(K.DOWN)) 1.0f else 0.28f,
        animationSpec = tween(durationMillis = 100),
        label = "rails_led_d"
    )
    val ledLeftAlpha by animateFloatAsState(
        targetValue = if (currentlyPressed.contains(K.LEFT)) 1.0f else 0.28f,
        animationSpec = tween(durationMillis = 100),
        label = "rails_led_l"
    )
    val ledRightAlpha by animateFloatAsState(
        targetValue = if (currentlyPressed.contains(K.RIGHT)) 1.0f else 0.28f,
        animationSpec = tween(durationMillis = 100),
        label = "rails_led_r"
    )

    // Neon Glow Color: #FFB13F (Warm Amber / Golden Orange)
    val neonColor = remember(isRgbEnabled) {
        if (isRgbEnabled) Color(0xFFFFB13F) else Color(0xFFFFB13F)
    }

    val rgbBloomAlpha by animateFloatAsState(
        targetValue = if (isAnyPressed) 0.95f else 0.45f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "rails_rgb_bloom"
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
            radius = 320f
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
            radius = 320f
        )
    }

    // Rail Puck gradient: radial-gradient(circle at 50% 38%, #34373b, #0c0d0e 75%)
    val puckGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF34373B),
                Color(0xFF16181A),
                Color(0xFF0C0D0E)
            ),
            center = Offset(0.50f, 0.38f),
            radius = 80f
        )
    }

    // Rail channels gradient
    val railHGradient = remember {
        Brush.verticalGradient(
            colors = listOf(Color(0xFF000000), Color(0xFF0B0C0D))
        )
    }
    val railVGradient = remember {
        Brush.horizontalGradient(
            colors = listOf(Color(0xFF000000), Color(0xFF0B0C0D))
        )
    }

    Box(
        modifier = modifier
            .size(160.dp)
            .drawBehind {
                if (isRgbEnabled) {
                    val w = size.width
                    val h = size.height

                    // 1. Central intersection base bloom
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                neonColor.copy(alpha = rgbBloomAlpha * 0.40f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = size.minDimension * 0.45f
                        ),
                        radius = size.minDimension * 0.45f,
                        center = center
                    )

                    // 2. Dual orthogonal laser slide guide track beams (X and Y axes)
                    val isHorizActive = currentlyPressed.contains(K.LEFT) || currentlyPressed.contains(K.RIGHT)
                    val isVertActive = currentlyPressed.contains(K.UP) || currentlyPressed.contains(K.DOWN)

                    // Horizontal laser track
                    val horizAlpha = if (isHorizActive) rgbBloomAlpha * 0.90f else (if (isAnyPressed) 0.20f else 0.40f)
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, neonColor.copy(alpha = horizAlpha * 0.65f), Color.Transparent)
                        ),
                        start = Offset(0f, center.y),
                        end = Offset(w, center.y),
                        strokeWidth = 6.dp.toPx()
                    )
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, (if (isHorizActive) Color.White else neonColor).copy(alpha = horizAlpha), Color.Transparent)
                        ),
                        start = Offset(0f, center.y),
                        end = Offset(w, center.y),
                        strokeWidth = 2.dp.toPx()
                    )

                    // Vertical laser track
                    val vertAlpha = if (isVertActive) rgbBloomAlpha * 0.90f else (if (isAnyPressed) 0.20f else 0.40f)
                    drawLine(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, neonColor.copy(alpha = vertAlpha * 0.65f), Color.Transparent)
                        ),
                        start = Offset(center.x, 0f),
                        end = Offset(center.x, h),
                        strokeWidth = 6.dp.toPx()
                    )
                    drawLine(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, (if (isVertActive) Color.White else neonColor).copy(alpha = vertAlpha), Color.Transparent)
                        ),
                        start = Offset(center.x, 0f),
                        end = Offset(center.x, h),
                        strokeWidth = 2.dp.toPx()
                    )

                    // 3. Travelling sliding puck beacon flare
                    val puckPos = Offset(center.x + puckOffsetXAnim, center.y + puckOffsetYAnim)
                    val puckBloomR = if (isAnyPressed) 32.dp.toPx() else 20.dp.toPx()
                    drawCircle(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0.00f to (if (isAnyPressed) Color.White else neonColor).copy(alpha = rgbBloomAlpha * 0.75f),
                                0.45f to neonColor.copy(alpha = rgbBloomAlpha * 0.35f),
                                1.00f to Color.Transparent
                            ),
                            center = puckPos,
                            radius = puckBloomR
                        ),
                        radius = puckBloomR,
                        center = puckPos
                    )
                }
            }
            .pointerInput(isConnected) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    var activeDirs = emptySet<String>()

                    fun evaluateOffset(pos: Offset) {
                        val newDirs = resolveRailsTouch(pos, size.width.toFloat())
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
            },
        contentAlignment = Alignment.Center
    ) {
        // --- 1. Tiltable 3D Physical Rocker Body Container ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = stageScaleAnim
                    scaleY = stageScaleAnim
                    translationY = stageOffsetYAnim * density
                    rotationX = rxAnim
                    rotationY = ryAnim
                    cameraDistance = 12f * density
                }
                .shadow(
                    elevation = if (isAnyPressed) 3.dp else 8.dp,
                    shape = CircleShape,
                    spotColor = if (isRgbEnabled) neonColor else Color.Black,
                    ambientColor = if (isRgbEnabled) neonColor else Color.Black
                )
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
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = if (isAnyPressed) 0.85f else 0.70f)),
                        startY = h - insetH,
                        endY = h
                    ),
                    topLeft = Offset(0f, h - insetH),
                    size = Size(w, insetH)
                )
            }

            // --- 2. Recessed Orthogonal Guide Rails (.rail-h and .rail-v) ---
            // Horizontal Rail: 132dp x 28dp at center (left 14dp, right 14dp)
            Box(
                modifier = Modifier
                    .size(132.dp, 28.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(railHGradient)
                    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Horizontal 2px guide groove line with 0.20 glow opacity
                Box(
                    modifier = Modifier
                        .size(104.dp, 2.dp)
                        .background(neonColor.copy(alpha = 0.20f))
                )
            }

            // Vertical Rail: 28dp x 132dp at center (top 14dp, bottom 14dp)
            Box(
                modifier = Modifier
                    .size(28.dp, 132.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(railVGradient)
                    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Vertical 2px guide groove line with 0.20 glow opacity
                Box(
                    modifier = Modifier
                        .size(2.dp, 104.dp)
                        .background(neonColor.copy(alpha = 0.20f))
                )
            }

            // --- 3. Dynamic Light Beams (.lit) & End Target LEDs (.ed) ---
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)

                // Dynamic Light Beams (.lit, height/width 3px, neonColor, box-shadow: 0 0 8px var(--glow))
                val beamThickness = 3.dp.toPx()

                // RIGHT BEAM (.lit-r)
                if (litRightWidth > 0f) {
                    val wPx = litRightWidth.dp.toPx()
                    // Glow halo
                    drawRoundRect(
                        color = neonColor.copy(alpha = 0.40f),
                        topLeft = Offset(center.x, center.y - beamThickness),
                        size = Size(wPx, beamThickness * 2f),
                        cornerRadius = CornerRadius(beamThickness, beamThickness)
                    )
                    // Core beam
                    drawRoundRect(
                        color = neonColor,
                        topLeft = Offset(center.x, center.y - beamThickness / 2f),
                        size = Size(wPx, beamThickness),
                        cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
                    )
                }

                // LEFT BEAM (.lit-l)
                if (litLeftWidth > 0f) {
                    val wPx = litLeftWidth.dp.toPx()
                    // Glow halo
                    drawRoundRect(
                        color = neonColor.copy(alpha = 0.40f),
                        topLeft = Offset(center.x - wPx, center.y - beamThickness),
                        size = Size(wPx, beamThickness * 2f),
                        cornerRadius = CornerRadius(beamThickness, beamThickness)
                    )
                    // Core beam
                    drawRoundRect(
                        color = neonColor,
                        topLeft = Offset(center.x - wPx, center.y - beamThickness / 2f),
                        size = Size(wPx, beamThickness),
                        cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
                    )
                }

                // DOWN BEAM (.lit-d)
                if (litDownHeight > 0f) {
                    val hPx = litDownHeight.dp.toPx()
                    // Glow halo
                    drawRoundRect(
                        color = neonColor.copy(alpha = 0.40f),
                        topLeft = Offset(center.x - beamThickness, center.y),
                        size = Size(beamThickness * 2f, hPx),
                        cornerRadius = CornerRadius(beamThickness, beamThickness)
                    )
                    // Core beam
                    drawRoundRect(
                        color = neonColor,
                        topLeft = Offset(center.x - beamThickness / 2f, center.y),
                        size = Size(beamThickness, hPx),
                        cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
                    )
                }

                // UP BEAM (.lit-u)
                if (litUpHeight > 0f) {
                    val hPx = litUpHeight.dp.toPx()
                    // Glow halo
                    drawRoundRect(
                        color = neonColor.copy(alpha = 0.40f),
                        topLeft = Offset(center.x - beamThickness, center.y - hPx),
                        size = Size(beamThickness * 2f, hPx),
                        cornerRadius = CornerRadius(beamThickness, beamThickness)
                    )
                    // Core beam
                    drawRoundRect(
                        color = neonColor,
                        topLeft = Offset(center.x - beamThickness / 2f, center.y - hPx),
                        size = Size(beamThickness, hPx),
                        cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
                    )
                }

                // End Target LEDs (.ed, 8px diameter = 4dp radius at 25dp from edges = 55dp from center)
                val ledRadius = 4.dp.toPx()
                val ledDist = 55.dp.toPx()

                val ledPositions = listOf(
                    Pair(Offset(center.x, center.y - ledDist), ledUpAlpha),
                    Pair(Offset(center.x, center.y + ledDist), ledDownAlpha),
                    Pair(Offset(center.x - ledDist, center.y), ledLeftAlpha),
                    Pair(Offset(center.x + ledDist, center.y), ledRightAlpha)
                )

                ledPositions.forEach { (pos, alpha) ->
                    if (alpha > 0.35f) {
                        // Active glow bloom
                        drawCircle(
                            color = neonColor.copy(alpha = (alpha - 0.28f) * 0.50f),
                            radius = ledRadius + 4.dp.toPx(),
                            center = pos
                        )
                        drawCircle(
                            color = neonColor.copy(alpha = alpha),
                            radius = ledRadius,
                            center = pos
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = (alpha - 0.28f) * 0.70f),
                            radius = ledRadius * 0.5f,
                            center = pos
                        )
                    } else {
                        // Idle pip
                        drawCircle(
                            color = neonColor.copy(alpha = alpha),
                            radius = ledRadius,
                            center = pos
                        )
                    }
                }
            }

            // --- 4. Sliding Tactile Puck (.rail-puck, 40dp x 40dp) ---
            Box(
                modifier = Modifier
                    .offset { IntOffset((puckOffsetXAnim * density).roundToInt(), (puckOffsetYAnim * density).roundToInt()) }
                    .size(40.dp)
                    .shadow(
                        elevation = if (isAnyPressed) 4.dp else 6.dp,
                        shape = CircleShape,
                        spotColor = Color.Black.copy(alpha = 0.55f),
                        ambientColor = Color.Black.copy(alpha = 0.40f)
                    )
                    .clip(CircleShape)
                    .background(puckGradient)
                    .border(1.dp, Color.Black.copy(alpha = 0.60f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val center = Offset(w / 2f, h / 2f)

                    // Puck Inset Rim (inset: 4px, border: 2px solid var(--glow), opacity: 0.8)
                    val ringInset = 4.dp.toPx()
                    val ringRadius = (w / 2f) - ringInset

                    // Outer bloom
                    drawCircle(
                        color = neonColor.copy(alpha = 0.35f),
                        radius = ringRadius,
                        center = center,
                        style = Stroke(width = 4.dp.toPx())
                    )
                    // Core crisp ring
                    drawCircle(
                        color = neonColor.copy(alpha = 0.80f),
                        radius = ringRadius,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )

                    // Central 7dp amber neon pip (radius 3.5dp)
                    val pipRadius = 3.5.dp.toPx()
                    drawCircle(
                        color = neonColor.copy(alpha = 0.40f),
                        radius = pipRadius + 2.dp.toPx(),
                        center = center
                    )
                    drawCircle(
                        color = neonColor,
                        radius = pipRadius,
                        center = center
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.60f),
                        radius = pipRadius * 0.45f,
                        center = center
                    )

                    // Top specular highlight arc
                    drawArc(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.White.copy(alpha = 0.20f), Color.Transparent),
                            startY = 0f,
                            endY = h * 0.45f
                        ),
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                        size = Size(w - 2.dp.toPx(), h - 2.dp.toPx()),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }
        }

        // --- 5. Outer Neon Ring (.lx-ring) & Top Specular Glass Lens (.lx-lens) ---
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val r = size.minDimension / 2f
            val center = Offset(w / 2f, h / 2f)
            val ringInset = 3.dp.toPx()
            val ringR = r - ringInset

            // Outer Neon Ring: border 2px solid var(--glow), opacity 0.35 idle, 1.0 pressed
            val ringAlpha = if (isAnyPressed) 0.85f else 0.35f
            drawCircle(
                color = neonColor.copy(alpha = ringAlpha * 0.40f),
                radius = ringR,
                center = center,
                style = Stroke(width = if (isAnyPressed) 5.dp.toPx() else 3.dp.toPx())
            )
            drawCircle(
                color = neonColor.copy(alpha = ringAlpha),
                radius = ringR,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // Top specular optical glass lens (.lx-lens)
            drawArc(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isAnyPressed) 0.06f else 0.14f),
                        Color.White.copy(alpha = if (isAnyPressed) 0.02f else 0.05f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = h * 0.42f
                ),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                size = Size(w - 2.dp.toPx(), h - 2.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )

            // Bottom-right specular reflection sheen
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.06f), Color.Transparent),
                    center = Offset(w * 0.70f, h * 0.78f),
                    radius = w * 0.28f
                ),
                center = Offset(w * 0.70f, h * 0.78f),
                radius = w * 0.28f
            )

            // Chamfer highlights
            drawArc(
                color = Color.White.copy(alpha = 0.12f),
                startAngle = 200f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(1.5.dp.toPx(), 1.5.dp.toPx()),
                size = Size(w - 3.dp.toPx(), h - 3.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )
            drawArc(
                color = Color.Black.copy(alpha = 0.30f),
                startAngle = 20f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(1.5.dp.toPx(), 1.5.dp.toPx()),
                size = Size(w - 3.dp.toPx(), h - 3.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }
}
