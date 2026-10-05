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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.sanket.tools.nexpad.model.NexpadKeys as K
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Snappy 3D physical rocker tilt kinematics for Disc D-Pad:
 * UP: rx = +8°, DOWN: rx = -8°
 * RIGHT: ry = +8°, LEFT: ry = -8°
 * Compounded diagonally (e.g. UP + RIGHT: rx = +8°, ry = +8°).
 */
fun calculateDiscTilt(pressedDirs: Set<String>): Pair<Float, Float> {
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
 * Calculates the physical displacement offset for the central sliding puck:
 * Moves by (vx * 32dp, vy * 32dp) with spring overshoot kinematics.
 */
fun calculateDiscPuckOffset(pressedDirs: Set<String>): Pair<Float, Float> {
    var vx = 0f
    if (pressedDirs.contains(K.RIGHT)) vx += 1f
    if (pressedDirs.contains(K.LEFT)) vx -= 1f

    var vy = 0f
    if (pressedDirs.contains(K.DOWN)) vy += 1f
    if (pressedDirs.contains(K.UP)) vy -= 1f

    return Pair(vx * 32f, vy * 32f)
}

/**
 * Calculates the dynamic directional arc angle (0° to 360° in Compose Canvas coordinates,
 * where 270° = UP, 0° = RIGHT, 90° = DOWN, 180° = LEFT).
 * Returns null if no direction is currently pressed.
 */
fun calculateDiscGateAngle(pressedDirs: Set<String>): Float? {
    var vx = 0f
    if (pressedDirs.contains(K.RIGHT)) vx += 1f
    if (pressedDirs.contains(K.LEFT)) vx -= 1f

    var vy = 0f
    if (pressedDirs.contains(K.DOWN)) vy += 1f
    if (pressedDirs.contains(K.UP)) vy -= 1f

    if (vx == 0f && vy == 0f) return null

    val rad = atan2(vy.toDouble(), vx.toDouble())
    val deg = Math.toDegrees(rad).toFloat()
    return (deg + 360f) % 360f
}

/**
 * Resolves touch coordinate on 160dp Disc D-Pad stage strictly to the four cardinal directional sectors.
 * Diagonal corner voids, center sliding puck deadzone (17dp), and outer boundary (>76dp)
 * are non-clickable, preventing diagonal clicks and ensuring single-direction activation.
 */
fun resolveDiscTouch(pos: Offset, sizePx: Float): Set<String> {
    val centerX = sizePx / 2f
    val centerY = sizePx / 2f
    val dx = pos.x - centerX
    val dy = pos.y - centerY
    val scale = sizePx / 160f
    val armHalfWidth = 26f * scale
    val armMaxReach = 76f * scale
    val centerDeadzone = 17f * scale

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
 * High-performance, tactile console-grade Disc D-Pad ("D-Pad — Disc"):
 * - 160dp circular stage with convex acrylic dome body and concentric machined grooves.
 * - 3D physical rocker tilt kinematics (perspective 420px, rotateX -vy*8°, rotateY vx*8°, 80ms ease).
 * - Central 34dp sliding puck with 32dp spring displacement and glowing neon pip.
 * - Four directional triangles (UP, DOWN, LEFT, RIGHT) with active neon bloom and drop shadow.
 * - Dynamic 84° directional neon gate arc (.lx-gate) sweeping around the outer rim.
 * - Outer glowing neon ring (.lx-ring) with active press bloom.
 * - Specular optical glass lens (.lx-lens) with top crescent chamfer and bottom-right sheen.
 */
@Composable
fun DiscDPad(
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
        calculateDiscTilt(currentlyPressed)
    }
    val (targetPuckX, targetPuckY) = remember(currentlyPressed) {
        calculateDiscPuckOffset(currentlyPressed)
    }
    val gateAngle = remember(currentlyPressed) {
        calculateDiscGateAngle(currentlyPressed)
    }

    val isAnyPressed = currentlyPressed.isNotEmpty()

    // 80ms ease for 3D physical rocker tilt
    val rxAnim by animateFloatAsState(
        targetValue = targetRx,
        animationSpec = tween(durationMillis = 80, easing = FastOutSlowInEasing),
        label = "disc_tilt_rx"
    )
    val ryAnim by animateFloatAsState(
        targetValue = targetRy,
        animationSpec = tween(durationMillis = 80, easing = FastOutSlowInEasing),
        label = "disc_tilt_ry"
    )

    // Dynamic puck sliding displacement: 140ms with spring overshoot kinematics
    val puckXAnim by animateFloatAsState(
        targetValue = targetPuckX,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 400f),
        label = "disc_puck_x"
    )
    val puckYAnim by animateFloatAsState(
        targetValue = targetPuckY,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 400f),
        label = "disc_puck_y"
    )

    // Plunging spring kinematics for overall stage
    val stageScaleAnim by animateFloatAsState(
        targetValue = if (isAnyPressed) 0.95f else 1.0f,
        animationSpec = tween(durationMillis = 80, easing = FastOutSlowInEasing),
        label = "disc_stage_scale"
    )
    val stageOffsetYAnim by animateFloatAsState(
        targetValue = if (isAnyPressed) 2.0f else 0f,
        animationSpec = tween(durationMillis = 80, easing = FastOutSlowInEasing),
        label = "disc_stage_offset_y"
    )

    // Outer neon ring bloom
    val ringAlphaAnim by animateFloatAsState(
        targetValue = if (isAnyPressed) 1.0f else 0.40f,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "disc_ring_alpha"
    )

    // Directional gate sweep alpha
    val gateAlphaAnim by animateFloatAsState(
        targetValue = if (isAnyPressed) 1.0f else 0f,
        animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing),
        label = "disc_gate_alpha"
    )

    // Neon Glow Color: #E055B8 (Hot Pink / Magenta)
    val neonColor = remember(isRgbEnabled) {
        if (isRgbEnabled) Color(0xFFE055B8) else Color(0xFFE055B8)
    }

    val rgbBloomAlpha by animateFloatAsState(
        targetValue = if (isAnyPressed) 0.95f else 0.45f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "disc_rgb_bloom"
    )

    val density = LocalDensity.current.density

    // Convex Acrylic Dome Base Gradient
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

    // Puck Gradient: radial-gradient(circle at 50% 38%, #34373b, #0c0d0e 75%)
    val puckGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF34373B),
                Color(0xFF161718),
                Color(0xFF0C0D0E)
            ),
            center = Offset(0.50f, 0.38f),
            radius = 60f
        )
    }

    Box(
        modifier = modifier
            .size(160.dp)
            .drawBehind {
                if (isRgbEnabled) {
                    val rimRadius = size.minDimension * 0.52f

                    // 1. Holographic turntable base bloom
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                neonColor.copy(alpha = rgbBloomAlpha * 0.45f),
                                neonColor.copy(alpha = rgbBloomAlpha * 0.16f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = size.minDimension * 0.95f
                        ),
                        radius = size.minDimension * 0.95f,
                        center = center
                    )

                    // 2. Fine holographic turntable rim circle
                    drawCircle(
                        color = neonColor.copy(alpha = if (isAnyPressed) 0.60f else 0.25f),
                        radius = rimRadius,
                        center = center,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.0f)
                    )

                    // 3. Directional angular sweep wedge flare following tilt / gate angle
                    val activeGate = gateAngle
                    if (isAnyPressed && activeGate != null) {
                        val flareRadius = size.minDimension * 0.62f
                        val flareWedgeStart = activeGate - 35f
                        drawArc(
                            brush = Brush.radialGradient(
                                colorStops = arrayOf(
                                    0.00f to Color.White.copy(alpha = 0.85f * gateAlphaAnim),
                                    0.40f to neonColor.copy(alpha = 0.65f * gateAlphaAnim),
                                    1.00f to Color.Transparent
                                ),
                                center = center,
                                radius = flareRadius
                            ),
                            startAngle = flareWedgeStart,
                            sweepAngle = 70f,
                            useCenter = true,
                            topLeft = Offset(center.x - flareRadius, center.y - flareRadius),
                            size = Size(flareRadius * 2f, flareRadius * 2f)
                        )

                        // Intense rim beacon arc at apex of touch vector
                        drawArc(
                            color = Color.White.copy(alpha = 0.95f * gateAlphaAnim),
                            startAngle = activeGate - 18f,
                            sweepAngle = 36f,
                            useCenter = false,
                            topLeft = Offset(center.x - rimRadius, center.y - rimRadius),
                            size = Size(rimRadius * 2f, rimRadius * 2f),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.0f)
                        )
                    }
                }
            }
            .pointerInput(isConnected) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    var activeDirs = emptySet<String>()

                    fun evaluateOffset(pos: Offset) {
                        val newDirs = resolveDiscTouch(pos, size.width.toFloat())
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
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val r = size.minDimension / 2f
                val center = Offset(w / 2f, h / 2f)

                // A. Concentric machined grooves: repeating every 10dp from center outwards
                val grooveIntervalPx = 10.dp.toPx()
                var currentRadius = grooveIntervalPx
                while (currentRadius < r - 4.dp.toPx()) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.035f),
                        radius = currentRadius,
                        center = center,
                        style = Stroke(width = 1.dp.toPx())
                    )
                    currentRadius += grooveIntervalPx
                }

                // B. Inset bottom undercut shadow: inset 0 -6px 9px rgba(0,0,0,.70)
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

                // C. Directional Triangles (.tri)
                val triWidthPx = 16.dp.toPx()
                val triHeightPx = 13.dp.toPx()
                val triMarginPx = 16.dp.toPx()
                val halfWidthPx = triWidthPx / 2f

                // 1) UP Triangle (Pointing UP)
                val isUp = currentlyPressed.contains(K.UP)
                val upPath = Path().apply {
                    moveTo(center.x, triMarginPx)
                    lineTo(center.x - halfWidthPx, triMarginPx + triHeightPx)
                    lineTo(center.x + halfWidthPx, triMarginPx + triHeightPx)
                    close()
                }
                if (isUp) {
                    drawPath(upPath, color = neonColor.copy(alpha = 0.50f), style = Stroke(width = 4.dp.toPx()))
                }
                drawPath(upPath, color = neonColor.copy(alpha = if (isUp) 1.0f else 0.35f))

                // 2) DOWN Triangle (Pointing DOWN)
                val isDown = currentlyPressed.contains(K.DOWN)
                val downPath = Path().apply {
                    moveTo(center.x, h - triMarginPx)
                    lineTo(center.x - halfWidthPx, h - triMarginPx - triHeightPx)
                    lineTo(center.x + halfWidthPx, h - triMarginPx - triHeightPx)
                    close()
                }
                if (isDown) {
                    drawPath(downPath, color = neonColor.copy(alpha = 0.50f), style = Stroke(width = 4.dp.toPx()))
                }
                drawPath(downPath, color = neonColor.copy(alpha = if (isDown) 1.0f else 0.35f))

                // 3) LEFT Triangle (Pointing LEFT)
                val isLeft = currentlyPressed.contains(K.LEFT)
                val leftPath = Path().apply {
                    moveTo(triMarginPx, center.y)
                    lineTo(triMarginPx + triHeightPx, center.y - halfWidthPx)
                    lineTo(triMarginPx + triHeightPx, center.y + halfWidthPx)
                    close()
                }
                if (isLeft) {
                    drawPath(leftPath, color = neonColor.copy(alpha = 0.50f), style = Stroke(width = 4.dp.toPx()))
                }
                drawPath(leftPath, color = neonColor.copy(alpha = if (isLeft) 1.0f else 0.35f))

                // 4) RIGHT Triangle (Pointing RIGHT)
                val isRight = currentlyPressed.contains(K.RIGHT)
                val rightPath = Path().apply {
                    moveTo(w - triMarginPx, center.y)
                    lineTo(w - triMarginPx - triHeightPx, center.y - halfWidthPx)
                    lineTo(w - triMarginPx - triHeightPx, center.y + halfWidthPx)
                    close()
                }
                if (isRight) {
                    drawPath(rightPath, color = neonColor.copy(alpha = 0.50f), style = Stroke(width = 4.dp.toPx()))
                }
                drawPath(rightPath, color = neonColor.copy(alpha = if (isRight) 1.0f else 0.35f))
            }

            // D. Central Sliding Puck (.disc-puck)
            Box(
                modifier = Modifier
                    .offset { IntOffset(puckXAnim.dp.roundToPx(), puckYAnim.dp.roundToPx()) }
                    .size(34.dp)
                    .shadow(
                        elevation = 6.dp,
                        shape = CircleShape,
                        spotColor = Color.Black,
                        ambientColor = Color.Black
                    )
                    .clip(CircleShape)
                    .background(puckGradient)
                    .border(
                        width = 1.dp,
                        color = Color.Black.copy(alpha = 0.60f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val pw = size.width
                    val ph = size.height
                    val pr = size.minDimension / 2f
                    val pcenter = Offset(pw / 2f, ph / 2f)

                    // Puck Inset highlight: inset 0 2px 2px rgba(255,255,255,.10)
                    drawArc(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.White.copy(alpha = 0.20f), Color.Transparent),
                            startY = 0f,
                            endY = ph * 0.45f
                        ),
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                        size = Size(pw - 2.dp.toPx(), ph - 2.dp.toPx()),
                        style = Stroke(width = 1.5.dp.toPx())
                    )

                    // Inset 4dp neon ring (.disc-puck::before)
                    val puckRingInset = 4.dp.toPx()
                    val puckRingR = pr - puckRingInset
                    // Soft glow halo
                    drawCircle(
                        color = neonColor.copy(alpha = 0.35f),
                        radius = puckRingR,
                        center = pcenter,
                        style = Stroke(width = 4.dp.toPx())
                    )
                    // Crisp neon core
                    drawCircle(
                        color = neonColor.copy(alpha = 0.85f),
                        radius = puckRingR,
                        center = pcenter,
                        style = Stroke(width = 2.dp.toPx())
                    )

                    // Center 6dp glowing neon pip (.disc-puck::after)
                    val pipRadius = 3.dp.toPx()
                    drawCircle(
                        color = neonColor.copy(alpha = 0.40f),
                        radius = pipRadius + 2.dp.toPx(),
                        center = pcenter
                    )
                    drawCircle(
                        color = neonColor,
                        radius = pipRadius,
                        center = pcenter
                    )
                }
            }
        }

        // --- 2. Outer Neon Ring (.lx-ring) & Dynamic Directional Gate (.lx-gate) ---
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val r = size.minDimension / 2f
            val center = Offset(w / 2f, h / 2f)
            val ringInset = 3.dp.toPx()
            val ringR = r - ringInset

            // Static Outer Neon Ring: border 2px solid var(--glow)
            // Soft glow halo
            drawCircle(
                color = neonColor.copy(alpha = ringAlphaAnim * 0.30f),
                radius = ringR,
                center = center,
                style = Stroke(width = if (isAnyPressed) 6.dp.toPx() else 4.dp.toPx())
            )
            // Crisp core ring
            drawCircle(
                color = neonColor.copy(alpha = ringAlphaAnim),
                radius = ringR,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // Dynamic Directional Gate Sweep (.lx-gate): 84° illuminated arc at gateAngle
            if (gateAngle != null && gateAlphaAnim > 0.01f) {
                // CSS angle 0° = 12 o'clock, Compose angle 0° = 3 o'clock -> offset by -90°
                val startAngle = (gateAngle - 90f) - 42f
                val sweepAngle = 84f
                val gateRadius = ringR - 1.dp.toPx()

                // Soft outer gate bloom
                drawArc(
                    color = neonColor.copy(alpha = gateAlphaAnim * 0.45f),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(center.x - gateRadius, center.y - gateRadius),
                    size = Size(gateRadius * 2f, gateRadius * 2f),
                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                )

                // Crisp bright gate core
                drawArc(
                    color = neonColor.copy(alpha = gateAlphaAnim * 0.95f),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(center.x - gateRadius, center.y - gateRadius),
                    size = Size(gateRadius * 2f, gateRadius * 2f),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // --- 3. Specular Optical Glass Lens Overlay (.lx-lens) ---
            // Top crescent arc chamfer
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

            // Bottom-right specular reflection sheen: circle at 70% 78%, rgba(255,255,255,.06)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.06f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.70f, h * 0.78f),
                    radius = w * 0.28f
                ),
                center = Offset(w * 0.70f, h * 0.78f),
                radius = w * 0.28f
            )

            // Chamfer edge highlights: top 1px rgba(255,255,255,0.12), bottom 1px rgba(0,0,0,0.30)
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
