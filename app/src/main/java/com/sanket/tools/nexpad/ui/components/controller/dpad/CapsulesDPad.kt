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
 * Snappy 3D physical rocker tilt kinematics for Capsules D-Pad:
 * UP: rx = +8°, DOWN: rx = -8°
 * RIGHT: ry = +8°, LEFT: ry = -8°
 * Compounded diagonally (e.g. UP + RIGHT: rx = +8°, ry = +8°).
 */
fun calculateCapsulesTilt(pressedDirs: Set<String>): Pair<Float, Float> {
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
 * Resolves touch coordinate on 170dp Capsules D-Pad stage strictly to the four discrete pill capsule keys.
 * Empty spaces (diagonal corner voids), center hub deadzone (17dp), and outer boundary (>85dp)
 * are non-clickable, preventing diagonal clicks and ensuring single-direction activation.
 */
fun resolveCapsulesTouch(pos: Offset, sizePx: Float): Set<String> {
    val centerX = sizePx / 2f
    val centerY = sizePx / 2f
    val dx = pos.x - centerX
    val dy = pos.y - centerY
    val scale = sizePx / 170f
    val armHalfWidth = 23f * scale
    val armMaxReach = 85f * scale
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
 * Discrete rounded pill capsule key for the Capsules D-Pad cluster:
 * - Vertical (46dp x 68dp) or Horizontal (68dp x 46dp) with RoundedCornerShape(23dp).
 * - Convex acrylic dome cap (.lx-body) with inset bottom shadow.
 * - Precision vector chevron arrow rotated by direction (0° UP, 270° LEFT, 90° RIGHT, 180° DOWN).
 * - Inset 3dp glowing neon ring (.lx-ring) with active bloom.
 * - Top specular arc highlight (.lx-lens) and chamfer borders.
 * - Snappy plunging spring kinematics (scale 0.95, offsetY 2dp).
 */
@Composable
fun CapsuleKey(
    direction: String,
    isPressed: Boolean,
    isVertical: Boolean,
    glowColor: Color,
    rotationAngle: Float,
    isRgbEnabled: Boolean = false,
    modifier: Modifier = Modifier
) {
    val scaleAnim by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = tween(durationMillis = 80, easing = FastOutSlowInEasing),
        label = "capsule_key_scale_$direction"
    )
    val pressOffsetYAnim by animateFloatAsState(
        targetValue = if (isPressed) 2.0f else 0f,
        animationSpec = tween(durationMillis = 80, easing = FastOutSlowInEasing),
        label = "capsule_key_offset_$direction"
    )
    val ringBloomAlpha by animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0.70f,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "capsule_key_ring_$direction"
    )
    val keyBloomAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 0.45f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "capsule_key_rgb_bloom_$direction"
    )

    val capsuleShape = remember { RoundedCornerShape(23.dp) }
    val ringShape = remember { RoundedCornerShape(20.dp) }

    val baseDomeGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF232527),
                Color(0xFF0C0D0E),
                Color(0xFF000000)
            ),
            center = Offset(0.50f, 0.55f),
            radius = 140f
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
            radius = 140f
        )
    }

    val capsuleWidth = if (isVertical) 46.dp else 68.dp
    val capsuleHeight = if (isVertical) 68.dp else 46.dp

    Box(
        modifier = modifier
            .size(capsuleWidth, capsuleHeight)
            .drawBehind {
                if (isRgbEnabled) {
                    val pad = 6.dp.toPx()
                    drawRoundRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                glowColor.copy(alpha = keyBloomAlpha * 0.50f),
                                glowColor.copy(alpha = keyBloomAlpha * 0.20f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = size.minDimension * 0.90f
                        ),
                        topLeft = Offset(-pad, -pad),
                        size = Size(size.width + pad * 2, size.height + pad * 2),
                        cornerRadius = CornerRadius(23.dp.toPx() + pad, 23.dp.toPx() + pad)
                    )
                }
            }
            .graphicsLayer {
                scaleX = scaleAnim
                scaleY = scaleAnim
            }
            .offset { IntOffset(0, pressOffsetYAnim.dp.roundToPx()) }
            .shadow(
                elevation = if (isPressed) 2.dp else 6.dp,
                shape = capsuleShape,
                ambientColor = if (isRgbEnabled) glowColor else Color.Black,
                spotColor = if (isRgbEnabled) glowColor else Color.Black
            )
            .clip(capsuleShape)
            .background(if (isPressed) pressedDomeGradient else baseDomeGradient)
            .border(
                width = 1.dp,
                color = Color.Black.copy(alpha = 0.50f),
                shape = capsuleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
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

            // 2. Vector Directional Chevron: M6 15 L12 9 L18 15 (viewBox 24x24)
            // Centered in capsule
            val chevronScale = 24.dp.toPx() / 24f
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
                        color = glowColor.copy(alpha = 0.55f),
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

            // 3. Inset 3dp Glowing Neon Ring (.lx-ring)
            val ringInset = 3.dp.toPx()
            val ringCorner = 20.dp.toPx()
            val ringW = w - ringInset * 2f
            val ringH = h - ringInset * 2f

            // Soft glow halo
            drawRoundRect(
                color = glowColor.copy(alpha = if (isPressed) 0.50f else 0.25f),
                topLeft = Offset(ringInset, ringInset),
                size = Size(ringW, ringH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(ringCorner, ringCorner),
                style = Stroke(width = if (isPressed) 4.dp.toPx() else 3.dp.toPx())
            )
            // Crisp core ring
            drawRoundRect(
                color = glowColor.copy(alpha = ringBloomAlpha),
                topLeft = Offset(ringInset, ringInset),
                size = Size(ringW, ringH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(ringCorner, ringCorner),
                style = Stroke(width = 2.dp.toPx())
            )

            // 4. Specular Optical Glass Lens (.lx-lens)
            // Top crescent chamfer
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isPressed) 0.06f else 0.14f),
                        Color.White.copy(alpha = if (isPressed) 0.02f else 0.05f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = h * 0.45f
                ),
                topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                size = Size(w - 2.dp.toPx(), h * 0.45f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(22.dp.toPx(), 22.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )

            // Bottom-right specular reflection sheen
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.06f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.70f, h * 0.78f),
                    radius = minOf(w, h) * 0.35f
                ),
                center = Offset(w * 0.70f, h * 0.78f),
                radius = minOf(w, h) * 0.35f
            )

            // Chamfer edge highlights: top 1px, bottom 1px
            drawLine(
                color = Color.White.copy(alpha = 0.12f),
                start = Offset(ringCorner, 1.dp.toPx()),
                end = Offset(w - ringCorner, 1.dp.toPx()),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = Color.Black.copy(alpha = 0.30f),
                start = Offset(ringCorner, h - 1.dp.toPx()),
                end = Offset(w - ringCorner, h - 1.dp.toPx()),
                strokeWidth = 1.dp.toPx()
            )
        }
    }
}

/**
 * High-performance, tactile console-grade Capsules D-Pad ("D-Pad — Capsules"):
 * - 170dp stage with 3D physical rocker tilt kinematics (perspective 420px, rotateX -vy*8°, rotateY vx*8°, 80ms ease).
 * - Central stationary/pivot hub (.cp-hub, 38dp x 38dp at 66,66) with inner neon ring.
 * - Four rounded pill capsules (.cp-up, .cp-down, .cp-left, .cp-right) orbiting the center.
 * - Acrylic convex dome with inset shadow, directional chevrons, inset glowing neon rings.
 * - Specular optical glass lens overlays.
 * - 8-Way directional touch resolution (cardinals + diagonals) with 16dp hub deadzone and 85dp outer boundary.
 */
@Composable
fun CapsulesDPad(
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
        calculateCapsulesTilt(currentlyPressed)
    }

    val isAnyPressed = currentlyPressed.isNotEmpty()

    // 80ms ease for 3D physical rocker tilt
    val rxAnim by animateFloatAsState(
        targetValue = targetRx,
        animationSpec = tween(durationMillis = 80, easing = FastOutSlowInEasing),
        label = "capsules_tilt_rx"
    )
    val ryAnim by animateFloatAsState(
        targetValue = targetRy,
        animationSpec = tween(durationMillis = 80, easing = FastOutSlowInEasing),
        label = "capsules_tilt_ry"
    )

    // Neon Glow Color: #B58CFF (Electric Violet / Lavender)
    val neonColor = remember(isRgbEnabled) {
        if (isRgbEnabled) Color(0xFFB58CFF) else Color(0xFFB58CFF)
    }

    val rgbBloomAlpha by animateFloatAsState(
        targetValue = if (isAnyPressed) 0.95f else 0.45f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "capsules_rgb_bloom"
    )

    val density = LocalDensity.current.density

    // Hub Gradient: radial-gradient(circle at 50% 60%, #030304, #151617)
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
            .size(170.dp)
            .drawBehind {
                if (isRgbEnabled) {
                    val w = size.width
                    val h = size.height

                    // 1. Central pivot hub containment aura
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                neonColor.copy(alpha = rgbBloomAlpha * 0.42f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = size.minDimension * 0.35f
                        ),
                        radius = size.minDimension * 0.35f,
                        center = center
                    )

                    // 2. 4-Way Capsule Thruster Plumes
                    fun drawThrusterPlume(isVertical: Boolean, cx: Float, cy: Float, isPressed: Boolean) {
                        val plumeAlpha = if (isPressed) rgbBloomAlpha * 0.90f else (if (isAnyPressed) 0.18f else 0.38f)
                        val plumeW = if (isVertical) 48.dp.toPx() else 70.dp.toPx()
                        val plumeH = if (isVertical) 70.dp.toPx() else 48.dp.toPx()
                        val pad = if (isPressed) 14.dp.toPx() else 8.dp.toPx()
                        val cr = (if (isVertical) plumeW else plumeH) / 2f + pad

                        drawRoundRect(
                            brush = Brush.radialGradient(
                                colorStops = arrayOf(
                                    0.00f to (if (isPressed) Color.White else neonColor).copy(alpha = plumeAlpha * 0.65f),
                                    0.45f to neonColor.copy(alpha = plumeAlpha * 0.30f),
                                    1.00f to Color.Transparent
                                ),
                                center = Offset(cx, cy),
                                radius = (plumeH.coerceAtLeast(plumeW) / 2f) + pad * 1.5f
                            ),
                            topLeft = Offset(cx - plumeW / 2f - pad, cy - plumeH / 2f - pad),
                            size = Size(plumeW + pad * 2f, plumeH + pad * 2f),
                            cornerRadius = CornerRadius(cr, cr)
                        )
                    }

                    // UP, DOWN, LEFT, RIGHT capsule centers
                    val upY = 36.dp.toPx()
                    val downY = h - 36.dp.toPx()
                    val leftX = 36.dp.toPx()
                    val rightX = w - 36.dp.toPx()

                    drawThrusterPlume(true, center.x, upY, pressedDirs.contains(K.UP))
                    drawThrusterPlume(true, center.x, downY, pressedDirs.contains(K.DOWN))
                    drawThrusterPlume(false, leftX, center.y, pressedDirs.contains(K.LEFT))
                    drawThrusterPlume(false, rightX, center.y, pressedDirs.contains(K.RIGHT))
                }
            }
            .shadow(
                elevation = if (isAnyPressed) 3.dp else 8.dp,
                shape = CircleShape,
                ambientColor = if (isRgbEnabled) neonColor else Color.Black,
                spotColor = if (isRgbEnabled) neonColor else Color.Black
            )
            .pointerInput(isConnected) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    var activeDirs = emptySet<String>()

                    fun evaluateOffset(pos: Offset) {
                        val newDirs = resolveCapsulesTouch(pos, size.width.toFloat())
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
        // --- 1. Tiltable 3D Physical Rocker Container ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationX = rxAnim
                    rotationY = ryAnim
                    cameraDistance = 12f * density
                }
        ) {
            // Central Stationary/Pivot Hub (.cp-hub, 38dp x 38dp at 66dp, 66dp)
            Box(
                modifier = Modifier
                    .offset(66.dp, 66.dp)
                    .size(38.dp)
                    .shadow(4.dp, CircleShape, spotColor = Color.Black, ambientColor = Color.Black)
                    .clip(CircleShape)
                    .background(hubGradient),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val center = Offset(w / 2f, h / 2f)

                    // Inset deep shadow: inset 0 3px 6px rgba(0,0,0,0.9)
                    drawCircle(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent),
                            startY = 0f,
                            endY = h * 0.6f
                        ),
                        radius = size.minDimension / 2f
                    )

                    // Inner glowing concentric ring: inset 11px, border 1px solid var(--glow), opacity 0.5
                    val ringRadius = (38.dp.toPx() / 2f) - 11.dp.toPx()
                    drawCircle(
                        color = neonColor.copy(alpha = 0.50f),
                        radius = ringRadius,
                        center = center,
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }

            // --- 2. Four Discrete Rounded Pill Capsules (.cp) ---
            // UP: left 62dp, top 0dp (46dp x 68dp)
            CapsuleKey(
                direction = K.UP,
                isPressed = currentlyPressed.contains(K.UP),
                isVertical = true,
                glowColor = neonColor,
                rotationAngle = 0f,
                isRgbEnabled = isRgbEnabled,
                modifier = Modifier.offset(62.dp, 0.dp)
            )

            // DOWN: left 62dp, top 102dp (46dp x 68dp)
            CapsuleKey(
                direction = K.DOWN,
                isPressed = currentlyPressed.contains(K.DOWN),
                isVertical = true,
                glowColor = neonColor,
                rotationAngle = 180f,
                isRgbEnabled = isRgbEnabled,
                modifier = Modifier.offset(62.dp, 102.dp)
            )

            // LEFT: left 0dp, top 62dp (68dp x 46dp)
            CapsuleKey(
                direction = K.LEFT,
                isPressed = currentlyPressed.contains(K.LEFT),
                isVertical = false,
                glowColor = neonColor,
                rotationAngle = 270f,
                isRgbEnabled = isRgbEnabled,
                modifier = Modifier.offset(0.dp, 62.dp)
            )

            // RIGHT: left 102dp, top 62dp (68dp x 46dp)
            CapsuleKey(
                direction = K.RIGHT,
                isPressed = currentlyPressed.contains(K.RIGHT),
                isVertical = false,
                glowColor = neonColor,
                rotationAngle = 90f,
                isRgbEnabled = isRgbEnabled,
                modifier = Modifier.offset(102.dp, 62.dp)
            )
        }
    }
}
