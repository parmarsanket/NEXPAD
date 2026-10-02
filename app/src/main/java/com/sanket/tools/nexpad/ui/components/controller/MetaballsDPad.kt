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
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.sanket.tools.nexpad.model.NexpadKeys as K
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel
import kotlin.math.abs
import kotlin.math.hypot

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
        calculateMetaballsTilt(currentlyPressed)
    }

    val isAnyPressed = currentlyPressed.isNotEmpty()

    // 80ms ease for 3D physical rocker tilt
    val rxAnim by animateFloatAsState(
        targetValue = targetRx,
        animationSpec = tween(durationMillis = 80, easing = FastOutSlowInEasing),
        label = "meta_tilt_rx"
    )
    val ryAnim by animateFloatAsState(
        targetValue = targetRy,
        animationSpec = tween(durationMillis = 80, easing = FastOutSlowInEasing),
        label = "meta_tilt_ry"
    )

    // Plunging spring kinematics for overall stage
    val stageScaleAnim by animateFloatAsState(
        targetValue = if (isAnyPressed) 0.95f else 1.0f,
        animationSpec = tween(durationMillis = 80, easing = FastOutSlowInEasing),
        label = "meta_stage_scale"
    )
    val stageOffsetYAnim by animateFloatAsState(
        targetValue = if (isAnyPressed) 2.0f else 0f,
        animationSpec = tween(durationMillis = 80, easing = FastOutSlowInEasing),
        label = "meta_stage_offset_y"
    )

    // Dynamic satellite retraction springs (58dp -> ~31.9dp on press)
    val upProgress by animateFloatAsState(
        targetValue = if (currentlyPressed.contains(K.UP)) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 340f),
        label = "meta_sat_up"
    )
    val rightProgress by animateFloatAsState(
        targetValue = if (currentlyPressed.contains(K.RIGHT)) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 340f),
        label = "meta_sat_right"
    )
    val downProgress by animateFloatAsState(
        targetValue = if (currentlyPressed.contains(K.DOWN)) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 340f),
        label = "meta_sat_down"
    )
    val leftProgress by animateFloatAsState(
        targetValue = if (currentlyPressed.contains(K.LEFT)) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 340f),
        label = "meta_sat_left"
    )

    // Neon Glow Color: #3FD2C4 (Electric Teal / Turquoise)
    val neonColor = remember(isRgbEnabled) {
        if (isRgbEnabled) Color(0xFF3FD2C4) else Color(0xFF3FD2C4)
    }

    val rgbBloomAlpha by animateFloatAsState(
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

    Box(
        modifier = modifier
            .size(172.dp)
            .drawBehind {
                if (isRgbEnabled) {
                    // 1. Central viscous fluid reservoir
                    val coreRadius = size.minDimension * 0.42f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                neonColor.copy(alpha = rgbBloomAlpha * 0.50f),
                                neonColor.copy(alpha = rgbBloomAlpha * 0.18f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = coreRadius
                        ),
                        radius = coreRadius,
                        center = center
                    )

                    // 2. Viscous satellite fluid fields swelling toward plunging centers
                    val baseDist = 58.dp.toPx()
                    val plungeTravel = 26.dp.toPx()

                    fun drawFluidSatellite(dx: Float, dy: Float, progress: Float) {
                        val isPlunging = progress > 0.05f
                        val dist = baseDist - plungeTravel * progress
                        val satPos = Offset(center.x + dx * dist, center.y + dy * dist)
                        val satR = 24.dp.toPx() + (12.dp.toPx() * progress)
                        val satAlpha = if (isPlunging) rgbBloomAlpha * 0.90f else (if (isAnyPressed) 0.20f else 0.40f)

                        // Viscous connecting bridge to center
                        if (isPlunging) {
                            drawLine(
                                brush = Brush.linearGradient(
                                    colors = listOf(neonColor.copy(alpha = satAlpha * 0.7f), Color.White.copy(alpha = satAlpha * 0.85f)),
                                    start = center,
                                    end = satPos
                                ),
                                start = center,
                                end = satPos,
                                strokeWidth = 20.dp.toPx() * progress
                            )
                        }

                        // Swelling satellite blob aura
                        drawCircle(
                            brush = Brush.radialGradient(
                                colorStops = arrayOf(
                                    0.00f to (if (isPlunging) Color.White else neonColor).copy(alpha = satAlpha * 0.70f),
                                    0.45f to neonColor.copy(alpha = satAlpha * 0.35f),
                                    1.00f to Color.Transparent
                                ),
                                center = satPos,
                                radius = satR * 1.35f
                            ),
                            radius = satR * 1.35f,
                            center = satPos
                        )
                    }

                    drawFluidSatellite(0f, -1f, upProgress)
                    drawFluidSatellite(0f, 1f, downProgress)
                    drawFluidSatellite(-1f, 0f, leftProgress)
                    drawFluidSatellite(1f, 0f, rightProgress)
                }
            }
            .pointerInput(isConnected) {
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

            // --- 2. Dynamic Liquid Neon Metaballs Layer (.goo2) ---
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val baseDistPx = 58.dp.toPx()
                val rCenterPx = 19.dp.toPx() // c0 radius (diameter 38dp)
                val rSatPx = 23.dp.toPx()    // sb radius (diameter 46dp)

                // Current satellite distances
                val satDistances = listOf(
                    Triple(K.UP, upProgress, baseDistPx * (1f - upProgress * 0.45f)),
                    Triple(K.RIGHT, rightProgress, baseDistPx * (1f - rightProgress * 0.45f)),
                    Triple(K.DOWN, downProgress, baseDistPx * (1f - downProgress * 0.45f)),
                    Triple(K.LEFT, leftProgress, baseDistPx * (1f - leftProgress * 0.45f))
                )

                val satOffsets = listOf(
                    Offset(0f, -satDistances[0].third),
                    Offset(satDistances[1].third, 0f),
                    Offset(0f, satDistances[2].third),
                    Offset(-satDistances[3].third, 0f)
                )

                // 1. Draw dynamic organic fluid bridge ONLY for satellites currently plunging inward
                satDistances.forEach { (dir, progress, d) ->
                    if (progress > 0.05f && d < 54.dp.toPx()) {
                        val blend = ((54.dp.toPx() - d) / (54.dp.toPx() - 31.dp.toPx())).coerceIn(0f, 1f)
                        val angle = when (dir) {
                            K.UP -> -90.0
                            K.RIGHT -> 0.0
                            K.DOWN -> 90.0
                            K.LEFT -> 180.0
                            else -> 0.0
                        }

                        val bridgePath = Path()
                        withTransform({
                            rotate(degrees = angle.toFloat(), pivot = center)
                        }) {
                            val wCenter = rCenterPx * (0.60f + 0.35f * blend)
                            val wSat = rSatPx * (0.60f + 0.35f * blend)
                            val wMid = (6.dp.toPx() + 10.dp.toPx() * blend)

                            bridgePath.reset()
                            bridgePath.moveTo(0f, -wCenter)
                            bridgePath.cubicTo(d * 0.35f, -wMid, d * 0.65f, -wMid, d, -wSat)
                            bridgePath.lineTo(d, wSat)
                            bridgePath.cubicTo(d * 0.65f, wMid, d * 0.35f, wMid, 0f, wCenter)
                            bridgePath.close()

                            // Soft outer fluid halo
                            drawPath(
                                bridgePath,
                                color = neonColor.copy(alpha = 0.35f * blend),
                                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                            // Fluid body fill
                            drawPath(bridgePath, color = neonColor.copy(alpha = 0.92f * blend))
                        }
                    }
                }

                // 2. Central fluid circle (.c0, diameter 38dp)
                drawCircle(
                    color = neonColor.copy(alpha = 0.30f),
                    radius = rCenterPx + 2.5.dp.toPx(),
                    center = center
                )
                drawCircle(
                    color = neonColor.copy(alpha = 0.92f),
                    radius = rCenterPx,
                    center = center
                )

                // 3. Satellite fluid circles (.sb, diameter 46dp)
                satDistances.forEachIndexed { index, (_, progress, _) ->
                    val satPos = center + satOffsets[index]
                    val isDirPressed = progress > 0.05f
                    // Outer soft halo
                    drawCircle(
                        color = neonColor.copy(alpha = if (isDirPressed) 0.55f else 0.30f),
                        radius = rSatPx + (if (isDirPressed) 4.dp.toPx() else 2.5.dp.toPx()),
                        center = satPos
                    )
                    // Core fluid circle
                    drawCircle(
                        color = neonColor.copy(alpha = 0.92f),
                        radius = rSatPx,
                        center = satPos
                    )
                }
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
                    val pipRadius = 3.dp.toPx()
                    drawCircle(
                        color = neonColor.copy(alpha = 0.40f),
                        radius = pipRadius + 2.dp.toPx(),
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
                            colors = listOf(Color.White.copy(alpha = 0.16f), Color.Transparent),
                            startY = 0f,
                            endY = size.height * 0.40f
                        ),
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                        size = Size(size.width - 2.dp.toPx(), size.height - 2.dp.toPx()),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }

            // Four Satellite Caps (.cp2.sat, 36dp x 36dp with directional chevrons)
            // 1. UP
            val upDistDp = 58.dp * (1f - upProgress * 0.45f)
            SatelliteCap(
                direction = K.UP,
                isPressed = currentlyPressed.contains(K.UP),
                glowColor = neonColor,
                capGradient = capGradient,
                rotationAngle = 0f,
                modifier = Modifier.offset(0.dp, -upDistDp)
            )

            // 2. RIGHT
            val rightDistDp = 58.dp * (1f - rightProgress * 0.45f)
            SatelliteCap(
                direction = K.RIGHT,
                isPressed = currentlyPressed.contains(K.RIGHT),
                glowColor = neonColor,
                capGradient = capGradient,
                rotationAngle = 90f,
                modifier = Modifier.offset(rightDistDp, 0.dp)
            )

            // 3. DOWN
            val downDistDp = 58.dp * (1f - downProgress * 0.45f)
            SatelliteCap(
                direction = K.DOWN,
                isPressed = currentlyPressed.contains(K.DOWN),
                glowColor = neonColor,
                capGradient = capGradient,
                rotationAngle = 180f,
                modifier = Modifier.offset(0.dp, downDistDp)
            )

            // 4. LEFT
            val leftDistDp = 58.dp * (1f - leftProgress * 0.45f)
            SatelliteCap(
                direction = K.LEFT,
                isPressed = currentlyPressed.contains(K.LEFT),
                glowColor = neonColor,
                capGradient = capGradient,
                rotationAngle = 270f,
                modifier = Modifier.offset(-leftDistDp, 0.dp)
            )
        }

        // --- 4. Outer Neon Ring (.lx-ring) & Top Specular Glass Lens (.lx-lens) ---
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val r = size.minDimension / 2f
            val center = Offset(w / 2f, h / 2f)
            val ringInset = 3.dp.toPx()
            val ringR = r - ringInset

            // Outer Neon Ring: border 2px solid var(--glow), opacity 0.3 idle, 1.0 pressed
            val ringAlpha = if (isAnyPressed) 0.85f else 0.30f
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

            // Chamfer highlights: top 1px rgba(255,255,255,0.12), bottom 1px rgba(0,0,0,0.30)
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

            // Vector Directional Chevron: M6 15 L12 9 L18 15 (viewBox 24x24)
            val chevronScale = 18.dp.toPx() / 24f
            val chevronPath = Path().apply {
                moveTo(center.x + (-6f) * chevronScale, center.y + 3f * chevronScale)
                lineTo(center.x, center.y + (-3f) * chevronScale)
                lineTo(center.x + 6f * chevronScale, center.y + 3f * chevronScale)
            }

            withTransform({
                rotate(degrees = rotationAngle, pivot = center)
            }) {
                // Active bloom shadow
                if (isPressed) {
                    drawPath(
                        path = chevronPath,
                        color = glowColor.copy(alpha = 0.65f),
                        style = Stroke(
                            width = 6f * chevronScale,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
                // Crisp core chevron
                drawPath(
                    path = chevronPath,
                    color = if (isPressed) Color.White else glowColor,
                    style = Stroke(
                        width = 3.4f * chevronScale,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            // Inset highlight arc
            drawArc(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.16f), Color.Transparent),
                    startY = 0f,
                    endY = size.height * 0.45f
                ),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                size = Size(size.width - 2.dp.toPx(), size.height - 2.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }
}
