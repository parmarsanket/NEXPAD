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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.sanket.tools.nexpad.model.NexpadKeys as K
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Calculates the snappy 3D physical rocker tilt kinematics for the Four Lenses D-Pad:
 * UP: rx = +8°, DOWN: rx = -8°
 * RIGHT: ry = +8°, LEFT: ry = -8°
 * Compounded diagonally (e.g. UP + RIGHT: rx = +8°, ry = +8°).
 */
fun calculateFourLensesTilt(pressedDirs: Set<String>): Pair<Float, Float> {
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
 * Resolves touch position on the 164dp Four Lenses stage strictly to the four circular lens keys.
 * Empty spaces (diagonal corner voids), center hub deadzone (18dp), and outer bounds (>82dp)
 * are non-clickable, preventing diagonal clicks and ensuring single-direction activation.
 */
fun resolveFourLensesTouch(
    pos: Offset,
    sizePx: Float
): Set<String> {
    val centerX = sizePx / 2f
    val centerY = sizePx / 2f
    val scale = sizePx / 164f
    val keyHitRadius = 28f * scale

    val upCenter = Offset(centerX, 27f * scale)
    val downCenter = Offset(centerX, 137f * scale)
    val leftCenter = Offset(27f * scale, centerY)
    val rightCenter = Offset(137f * scale, centerY)

    val distUp = hypot(pos.x - upCenter.x, pos.y - upCenter.y)
    val distDown = hypot(pos.x - downCenter.x, pos.y - downCenter.y)
    val distLeft = hypot(pos.x - leftCenter.x, pos.y - leftCenter.y)
    val distRight = hypot(pos.x - rightCenter.x, pos.y - rightCenter.y)

    val isUp = distUp <= keyHitRadius
    val isDown = distDown <= keyHitRadius
    val isLeft = distLeft <= keyHitRadius
    val isRight = distRight <= keyHitRadius

    return when {
        isUp && isRight -> if (distUp <= distRight) setOf(K.UP) else setOf(K.RIGHT)
        isUp && isLeft  -> if (distUp <= distLeft) setOf(K.UP) else setOf(K.LEFT)
        isDown && isRight -> if (distDown <= distRight) setOf(K.DOWN) else setOf(K.RIGHT)
        isDown && isLeft  -> if (distDown <= distLeft) setOf(K.DOWN) else setOf(K.LEFT)
        isUp -> setOf(K.UP)
        isDown -> setOf(K.DOWN)
        isLeft -> setOf(K.LEFT)
        isRight -> setOf(K.RIGHT)
        else -> emptySet()
    }
}

/**
 * Discrete circular lens key for the Four Lenses D-Pad cluster:
 * - 54dp convex acrylic dome cap (.lx-body)
 * - 34dp recessed circular dish (.lx-window) with inner vignette
 * - Vector chevron arrow rotated by direction (0° UP, 270° LEFT, 90° RIGHT, 180° DOWN)
 * - Inset 3dp glowing neon ring (.lx-ring) with active bloom
 * - Top specular arc highlight (.lx-lens)
 * - Plunging spring kinematics (scale 0.95, offsetY 2dp)
 */
@Composable
fun FourLensesKey(
    direction: String,
    isPressed: Boolean,
    glowColor: Color,
    isRgbEnabled: Boolean,
    rotationAngle: Float,
    modifier: Modifier = Modifier
) {
    val scaleAnim by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = tween(durationMillis = 80, easing = FastOutSlowInEasing),
        label = "four_lenses_key_scale_$direction"
    )
    val pressOffsetYAnim by animateFloatAsState(
        targetValue = if (isPressed) 2.0f else 0f,
        animationSpec = tween(durationMillis = 80, easing = FastOutSlowInEasing),
        label = "four_lenses_key_offset_$direction"
    )
    val ringBloomAlpha by animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0.70f,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "four_lenses_key_ring_$direction"
    )
    val keyBloomAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 0.45f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "four_lenses_key_rgb_bloom_$direction"
    )

    val baseDomeGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF232527),
                Color(0xFF0C0D0E),
                Color(0xFF000000)
            ),
            center = Offset(0.50f, 0.55f),
            radius = 120f
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
            radius = 120f
        )
    }

    val dishGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF050506),
                Color(0xFF121314)
            ),
            center = Offset(0.50f, 0.60f),
            radius = 80f
        )
    }

    Box(
        modifier = modifier
            .size(54.dp)
            .drawBehind {
                if (isRgbEnabled) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                glowColor.copy(alpha = keyBloomAlpha * 0.50f),
                                glowColor.copy(alpha = keyBloomAlpha * 0.20f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = size.minDimension * 0.85f
                        ),
                        radius = size.minDimension * 0.85f
                    )
                }
            }
            .graphicsLayer {
                scaleX = scaleAnim
                scaleY = scaleAnim
            }
            .offset { IntOffset(0, pressOffsetYAnim.dp.roundToPx()) }
            .shadow(
                elevation = if (isPressed) 1.dp else 4.dp,
                shape = CircleShape,
                ambientColor = if (isRgbEnabled) glowColor else Color.Black,
                spotColor = if (isRgbEnabled) glowColor else Color.Black
            )
            .clip(CircleShape)
            .background(if (isPressed) pressedDomeGradient else baseDomeGradient)
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

            // 1. Inset bottom undercut shadow: inset 0 -6px 9px rgba(0,0,0,.70)
            val insetH = h * 0.35f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = if (isPressed) 0.85f else 0.70f)),
                    startY = h - insetH,
                    endY = h
                ),
                topLeft = Offset(0f, h - insetH),
                size = Size(w, insetH)
            )

            // 2. Pressed top inset shadow: inset 0 3px 8px rgba(0,0,0,.85)
            if (isPressed) {
                val pressedInsetH = h * 0.30f
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent),
                        startY = 0f,
                        endY = pressedInsetH
                    ),
                    topLeft = Offset(0f, 0f),
                    size = Size(w, pressedInsetH)
                )
            }

            // 3. Recessed Dish (.lx-window: 34dp circle centered)
            val dishRadius = 17.dp.toPx()
            drawCircle(
                brush = dishGradient,
                radius = dishRadius,
                center = center
            )
            // Dish inner top shadow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)),
                    center = center,
                    radius = dishRadius
                ),
                radius = dishRadius,
                center = center
            )
            // Dish bottom highlight edge (0 1px 0 rgba(255,255,255,.05))
            drawArc(
                color = Color.White.copy(alpha = 0.05f),
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(center.x - dishRadius, center.y - dishRadius),
                size = Size(dishRadius * 2f, dishRadius * 2f),
                style = Stroke(width = 1.dp.toPx())
            )

            // 4. Directional Chevron Arrow (SVG viewBox 0 0 44 44: M8 30 L22 16 L36 30)
            val s = (34.dp.toPx()) / 44f
            val chevronPath = Path().apply {
                moveTo(center.x - 14f * s, center.y + 7f * s)
                lineTo(center.x, center.y - 7f * s)
                lineTo(center.x + 14f * s, center.y + 7f * s)
            }

            withTransform({
                rotate(degrees = rotationAngle, pivot = center)
            }) {
                val strokeW = 6f * s
                // Emissive bloom on press
                if (isPressed) {
                    drawPath(
                        path = chevronPath,
                        color = glowColor.copy(alpha = 0.75f),
                        style = Stroke(
                            width = strokeW + 3.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
                // Crisp core stroke
                drawPath(
                    path = chevronPath,
                    color = if (isPressed) Color.White else glowColor.copy(alpha = 0.90f),
                    style = Stroke(
                        width = strokeW,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            // 5. Inset Neon Ring (.lx-ring: inset 3px)
            val ringRadius = r - 3.dp.toPx()
            // Outer bloom
            drawCircle(
                color = glowColor.copy(alpha = if (isPressed) 0.55f else 0.22f),
                radius = ringRadius,
                center = center,
                style = Stroke(width = if (isPressed) 6.dp.toPx() else 4.dp.toPx())
            )
            // Crisp core
            drawCircle(
                color = glowColor.copy(alpha = ringBloomAlpha),
                radius = ringRadius,
                center = center,
                style = Stroke(width = if (isPressed) 2.2.dp.toPx() else 1.8.dp.toPx())
            )

            // 6. Top Specular Glass Lens (.lx-lens)
            val lensInset = 1.dp.toPx()
            drawArc(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isPressed) 0.06f else 0.14f),
                        Color.White.copy(alpha = if (isPressed) 0.02f else 0.05f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = h * 0.40f
                ),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(lensInset, lensInset),
                size = Size(w - lensInset * 2f, h - lensInset * 2f),
                style = Stroke(width = 1.2.dp.toPx())
            )

            // Inset bottom shadow arc
            drawArc(
                color = Color.Black.copy(alpha = 0.30f),
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(lensInset, lensInset),
                size = Size(w - lensInset * 2f, h - lensInset * 2f),
                style = Stroke(width = 1.dp.toPx())
            )

            // Bottom-right specular sheen circle
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.06f), Color.Transparent),
                    center = Offset(w * 0.70f, h * 0.78f),
                    radius = w * 0.22f
                ),
                center = Offset(w * 0.70f, h * 0.78f),
                radius = w * 0.22f
            )
        }
    }
}

/**
 * Console-grade Four Lenses D-Pad (Optical / Nord Frost Cluster) natively translated from user specification:
 * - 164dp x 164dp stage with outer ambient circular halo (.halo: 1px border at 0.16 alpha)
 * - 3D physical rocker tilt kinematics: perspective(420px) rotateX(±8°) rotateY(±8°) with 80ms ease
 * - Central stationary/pivot hub (.hub: 40dp at 62dp, 62dp, #030304 -> #151617) with 6dp glowing pip
 * - Four 54dp discrete circular lens keys (.dkey): UP(55,0), LEFT(0,55), RIGHT(110,55), DOWN(55,110)
 * - Per-key convex acrylic dome, 34dp recessed dish with inner vignette, chevron arrow, and glowing ring
 * - Pure 8-way directional touch resolution with center deadzone and outer containment
 */
@Composable
fun FourLensesDPad(
    isConnected: Boolean,
    viewModel: GamepadViewModel,
    isRgbEnabled: Boolean,
    onVibrate: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var pressedDirs by remember { mutableStateOf<Set<String>>(emptySet()) }
    val currentOnVibrate by rememberUpdatedState(onVibrate)
    val currentViewModel by rememberUpdatedState(viewModel)
    val density = LocalDensity.current.density

    val isUpPressed = pressedDirs.contains(K.UP)
    val isDownPressed = pressedDirs.contains(K.DOWN)
    val isLeftPressed = pressedDirs.contains(K.LEFT)
    val isRightPressed = pressedDirs.contains(K.RIGHT)

    // Snappy 3D physical rocker tilt kinematics:
    // UP: rx = +8°, DOWN: rx = -8°
    // RIGHT: ry = +8°, LEFT: ry = -8°
    // Compounded diagonally (e.g. UP+RIGHT: rx = +8°, ry = +8°)
    val (targetTiltX, targetTiltY) = remember(pressedDirs) {
        calculateFourLensesTilt(pressedDirs)
    }

    val tiltXAnim by animateFloatAsState(
        targetValue = targetTiltX,
        animationSpec = tween(durationMillis = 80, easing = FastOutSlowInEasing),
        label = "four_lenses_tilt_x"
    )
    val tiltYAnim by animateFloatAsState(
        targetValue = targetTiltY,
        animationSpec = tween(durationMillis = 80, easing = FastOutSlowInEasing),
        label = "four_lenses_tilt_y"
    )

    // Nord Frost Chrome (#D8DEE9) or Neon Cyan under RGB
    val glowColor = remember(isRgbEnabled) {
        if (isRgbEnabled) Color(0xFF00E5FF) else Color(0xFFD8DEE9)
    }

    val hasActivePress = pressedDirs.isNotEmpty()
    val rgbBloomAlpha by animateFloatAsState(
        targetValue = if (hasActivePress) 0.95f else 0.45f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "four_lenses_rgb_bloom"
    )

    val hubGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF030304),
                Color(0xFF151617)
            ),
            center = Offset(0.50f, 0.60f),
            radius = 60f
        )
    }

    Box(
        modifier = modifier
            .size(164.dp)
            .drawBehind {
                if (isRgbEnabled) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                glowColor.copy(alpha = rgbBloomAlpha * 0.50f),
                                glowColor.copy(alpha = rgbBloomAlpha * 0.20f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = size.minDimension * 0.95f
                        ),
                        radius = size.minDimension * 0.95f
                    )
                }
            }
            .shadow(
                elevation = if (hasActivePress) 3.dp else 8.dp,
                shape = CircleShape,
                ambientColor = if (isRgbEnabled) glowColor else Color.Black,
                spotColor = if (isRgbEnabled) glowColor else Color.Black
            )
            .pointerInput(isConnected) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    var activeDirs = emptySet<String>()

                    fun evaluate(pos: Offset) {
                        val newDirs = resolveFourLensesTouch(pos, size.width.toFloat())
                        if (newDirs != activeDirs) {
                            val added = newDirs - activeDirs
                            val removed = activeDirs - newDirs
                            removed.forEach { dir -> currentViewModel.updateButton(dir, false) }
                            added.forEach { dir -> currentViewModel.updateButton(dir, true) }
                            if (added.isNotEmpty()) currentOnVibrate()
                            activeDirs = newDirs
                            pressedDirs = newDirs
                        }
                    }

                    try {
                        evaluate(down.position)
                        while (true) {
                            val event = awaitPointerEvent()
                            val pointer = event.changes.firstOrNull { it.id == down.id }
                            if (pointer == null || !pointer.pressed) break
                            pointer.consume()
                            evaluate(pointer.position)
                        }
                    } finally {
                        activeDirs.forEach { dir -> currentViewModel.updateButton(dir, false) }
                        pressedDirs = emptySet()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Outer Ambient Halo (164dp circle, 1px border at 0.16 opacity)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, glowColor.copy(alpha = 0.16f), CircleShape)
        )

        // 3D Tilt Container (.tiltable): perspective(420px) rotateX(rx) rotateY(ry)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationX = tiltXAnim
                    rotationY = tiltYAnim
                    cameraDistance = 8f * density
                }
        ) {
            // Central Stationary/Pivot Hub (.hub: 40dp at 62dp, 62dp)
            Box(
                modifier = Modifier
                    .offset(62.dp, 62.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(hubGradient)
                    .border(1.dp, Color.Black.copy(alpha = 0.60f), CircleShape)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val r = size.minDimension / 2f
                    val c = Offset(size.width / 2f, size.height / 2f)

                    // Inset shadow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                            center = c,
                            radius = r
                        ),
                        radius = r
                    )

                    // Central Luminous Pip (6dp diameter at 0.60 opacity)
                    drawCircle(
                        color = glowColor.copy(alpha = 0.20f),
                        radius = 5.dp.toPx(),
                        center = c
                    )
                    drawCircle(
                        color = glowColor.copy(alpha = 0.60f),
                        radius = 3.dp.toPx(),
                        center = c
                    )
                }
            }

            // Four Circular Lens Keys (.dkey: 54dp each)
            FourLensesKey(
                direction = K.UP,
                isPressed = isUpPressed,
                glowColor = glowColor,
                isRgbEnabled = isRgbEnabled,
                rotationAngle = 0f,
                modifier = Modifier.offset(55.dp, 0.dp)
            )

            FourLensesKey(
                direction = K.LEFT,
                isPressed = isLeftPressed,
                glowColor = glowColor,
                isRgbEnabled = isRgbEnabled,
                rotationAngle = 270f,
                modifier = Modifier.offset(0.dp, 55.dp)
            )

            FourLensesKey(
                direction = K.RIGHT,
                isPressed = isRightPressed,
                glowColor = glowColor,
                isRgbEnabled = isRgbEnabled,
                rotationAngle = 90f,
                modifier = Modifier.offset(110.dp, 55.dp)
            )

            FourLensesKey(
                direction = K.DOWN,
                isPressed = isDownPressed,
                glowColor = glowColor,
                isRgbEnabled = isRgbEnabled,
                rotationAngle = 180f,
                modifier = Modifier.offset(55.dp, 110.dp)
            )
        }
    }
}
