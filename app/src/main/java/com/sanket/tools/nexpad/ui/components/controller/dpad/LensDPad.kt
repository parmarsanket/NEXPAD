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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.sanket.tools.nexpad.model.NexpadKeys as K
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min

/**
 * Builds the exact mathematically contoured D-Pad cross path from the SVG specification.
 * Supports an [insetDp] parameter:
 * - When [insetDp] == 0: generates the outer contoured cross boundary (corner radii 10 and 7).
 * - When [insetDp] > 0: generates the exact eroded inset contour for the glowing neon ribbon (feMorphology erode).
 */
fun createContouredCrossPath(size: Size, insetDp: Float = 0f): Path {
    val scale = min(size.width, size.height) / 150f
    val offsetX = (size.width - 150f * scale) / 2f
    val offsetY = (size.height - 150f * scale) / 2f

    val d = insetDp

    val topY = 9f + d
    val bottomY = 141f - d
    val leftX = 9f + d
    val rightX = 141f - d

    val armTopLeftX = 52f + d
    val armTopRightX = 98f - d
    val armLeftTopY = 52f + d
    val armRightBottomY = 98f - d

    fun sx(x: Float): Float = offsetX + x * scale
    fun sy(y: Float): Float = offsetY + y * scale

    return Path().apply {
        // Top arm
        moveTo(sx(62f), sy(topY))
        lineTo(sx(88f), sy(topY))
        quadraticTo(sx(armTopRightX), sy(topY), sx(armTopRightX), sy(19f))
        lineTo(sx(armTopRightX), sy(45f))
        quadraticTo(sx(armTopRightX), sy(armLeftTopY), sx(105f), sy(armLeftTopY))

        // Right arm
        lineTo(sx(131f - d), sy(armLeftTopY))
        quadraticTo(sx(rightX), sy(armLeftTopY), sx(rightX), sy(62f))
        lineTo(sx(rightX), sy(88f))
        quadraticTo(sx(rightX), sy(armRightBottomY), sx(131f - d), sy(armRightBottomY))
        lineTo(sx(105f), sy(armRightBottomY))
        quadraticTo(sx(armTopRightX), sy(armRightBottomY), sx(armTopRightX), sy(105f))

        // Bottom arm
        lineTo(sx(armTopRightX), sy(131f - d))
        quadraticTo(sx(armTopRightX), sy(bottomY), sx(88f), sy(bottomY))
        lineTo(sx(62f), sy(bottomY))
        quadraticTo(sx(armTopLeftX), sy(bottomY), sx(armTopLeftX), sy(131f - d))
        lineTo(sx(armTopLeftX), sy(105f))
        quadraticTo(sx(armTopLeftX), sy(armRightBottomY), sx(45f), sy(armRightBottomY))

        // Left arm
        lineTo(sx(19f + d), sy(armRightBottomY))
        quadraticTo(sx(leftX), sy(armRightBottomY), sx(leftX), sy(88f))
        lineTo(sx(leftX), sy(62f))
        quadraticTo(sx(leftX), sy(armLeftTopY), sx(19f + d), sy(armLeftTopY))
        lineTo(sx(45f), sy(armLeftTopY))
        quadraticTo(sx(armTopLeftX), sy(armLeftTopY), sx(armTopLeftX), sy(45f))

        // Return to top arm
        lineTo(sx(armTopLeftX), sy(19f))
        quadraticTo(sx(armTopLeftX), sy(topY), sx(62f), sy(topY))
        close()
    }
}

val contouredCrossShape = GenericShape { size, _ ->
    addPath(createContouredCrossPath(size))
}

/**
 * Console-grade Lens D-Pad (Optical / Nord Frost Cross) natively translated from user specification:
 * - 160dp circular socket housing with #232527 -> #0c0d0e -> #000 gradient and inset ring
 * - Pure arm & diagonal quadrant hit detection matching exact HTML arm geometries
 * - Snappy 3D physical rocker tilt kinematics: perspective(420px) rotateX(±8°) rotateY(±8°) with 80ms ease
 * - Animated active arm fills (0.30 opacity with 100ms ease)
 * - Animated chevron arrow drop-shadow bloom on press
 * - Recessed center hub dish (#030304 -> #151617) with radiant center dot
 * - Dynamic specular lens reflection (.lx-lens) overlaying the component
 */
@Composable
fun LensDPad(
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

    // Physical Rocker Tilt Kinematics matching reference CSS:
    // UP: rx = +8°, DOWN: rx = -8°, RIGHT: ry = +8°, LEFT: ry = -8°
    // transition: transform 0.08s ease
    val targetTiltX = remember(pressedDirs) {
        var tilt = 0f
        if (isUpPressed) tilt += 8.0f
        if (isDownPressed) tilt -= 8.0f
        tilt
    }
    val targetTiltY = remember(pressedDirs) {
        var tilt = 0f
        if (isRightPressed) tilt += 8.0f
        if (isLeftPressed) tilt -= 8.0f
        tilt
    }

    val tiltXAnim by animateFloatAsState(
        targetValue = targetTiltX,
        animationSpec = tween(durationMillis = 80, easing = FastOutSlowInEasing),
        label = "lens_dpad_tilt_x"
    )
    val tiltYAnim by animateFloatAsState(
        targetValue = targetTiltY,
        animationSpec = tween(durationMillis = 80, easing = FastOutSlowInEasing),
        label = "lens_dpad_tilt_y"
    )

    // Animated Active Arm Fills (.af: opacity 0.30, transition: opacity 0.1s ease)
    val upFillAlpha by animateFloatAsState(
        targetValue = if (isUpPressed) 0.30f else 0.0f,
        animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing),
        label = "lens_dpad_up_fill"
    )
    val downFillAlpha by animateFloatAsState(
        targetValue = if (isDownPressed) 0.30f else 0.0f,
        animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing),
        label = "lens_dpad_down_fill"
    )
    val leftFillAlpha by animateFloatAsState(
        targetValue = if (isLeftPressed) 0.30f else 0.0f,
        animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing),
        label = "lens_dpad_left_fill"
    )
    val rightFillAlpha by animateFloatAsState(
        targetValue = if (isRightPressed) 0.30f else 0.0f,
        animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing),
        label = "lens_dpad_right_fill"
    )

    // Animated Chevron Drop-Shadow Glow Bloom (.ch: filter drop-shadow(0 0 3px var(--glow)))
    val upGlowAlpha by animateFloatAsState(
        targetValue = if (isUpPressed) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing),
        label = "lens_dpad_up_glow"
    )
    val downGlowAlpha by animateFloatAsState(
        targetValue = if (isDownPressed) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing),
        label = "lens_dpad_down_glow"
    )
    val leftGlowAlpha by animateFloatAsState(
        targetValue = if (isLeftPressed) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing),
        label = "lens_dpad_left_glow"
    )
    val rightGlowAlpha by animateFloatAsState(
        targetValue = if (isRightPressed) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing),
        label = "lens_dpad_right_glow"
    )

    // Nord Frost Chrome (#D8DEE9) or Neon Cyan under RGB
    val glowColor = remember(isRgbEnabled) {
        if (isRgbEnabled) Color(0xFF00E5FF) else Color(0xFFD8DEE9)
    }

    val hasActivePress = pressedDirs.isNotEmpty()
    val rgbBloomAlpha by animateFloatAsState(
        targetValue = if (hasActivePress) 0.95f else 0.45f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "lens_dpad_rgb_bloom"
    )

    // Socket radial gradient: circle at 50% 55%: #232527 -> #0c0d0e -> #000000
    val socketGradient = remember {
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

    // Cross body radial gradient: cx=75, cy=80, r=85: #26282b -> #0c0d0e -> #000000
    val crossBodyGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF26282B),
                Color(0xFF0C0D0E),
                Color(0xFF000000)
            ),
            center = Offset(0.50f, 0.53f),
            radius = 180f
        )
    }

    // Gloss sheen gradient: #fff 10% -> transparent 45% -> #000 28%
    val glossGradient = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.10f),
                Color.White.copy(alpha = 0.00f),
                Color.Black.copy(alpha = 0.28f)
            )
        )
    }

    // Center hub radial gradient: #030304 -> #151617
    val hubGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF030304),
                Color(0xFF151617)
            ),
            center = Offset(0.50f, 0.60f),
            radius = 40f
        )
    }

    Box(
        modifier = modifier
            .size(160.dp)
            .drawBehind {
                if (isRgbEnabled) {
                    val causticCenter = Offset(
                        center.x + tiltYAnim * 1.8f,
                        center.y - tiltXAnim * 1.8f
                    )
                    // 1. Primary optical caustic bloom
                    drawCircle(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0.00f to Color.White.copy(alpha = if (hasActivePress) 0.65f else 0.25f),
                                0.35f to glowColor.copy(alpha = rgbBloomAlpha * 0.55f),
                                0.70f to glowColor.copy(alpha = rgbBloomAlpha * 0.20f),
                                1.00f to Color.Transparent
                            ),
                            center = causticCenter,
                            radius = size.minDimension * 0.95f
                        ),
                        radius = size.minDimension * 0.95f,
                        center = causticCenter
                    )

                    // 2. Chromatic aberration edge fringe (magenta/cyan prismatic split)
                    val fringeRadius = size.minDimension * 0.54f
                    drawCircle(
                        color = Color(0xFFFF2A85).copy(alpha = if (hasActivePress) 0.45f else 0.18f),
                        radius = fringeRadius + 3f,
                        center = Offset(causticCenter.x - 2f, causticCenter.y),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.0f)
                    )
                    drawCircle(
                        color = Color(0xFF00E5FF).copy(alpha = if (hasActivePress) 0.55f else 0.22f),
                        radius = fringeRadius - 1f,
                        center = Offset(causticCenter.x + 2f, causticCenter.y),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.0f)
                    )

                    // 3. Dual concentric optical caustic rings
                    val causticR1 = size.minDimension * 0.48f
                    val causticR2 = size.minDimension * 0.40f
                    drawCircle(
                        color = glowColor.copy(alpha = if (hasActivePress) 0.75f else 0.35f),
                        radius = causticR1,
                        center = causticCenter,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.2f)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = if (hasActivePress) 0.85f else 0.30f),
                        radius = causticR2,
                        center = causticCenter,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.4f)
                    )
                }
            }
            .shadow(
                elevation = if (hasActivePress) 3.dp else 8.dp,
                shape = CircleShape,
                ambientColor = if (isRgbEnabled) glowColor else Color.Black,
                spotColor = if (isRgbEnabled) glowColor else Color.Black
            )
            .clip(CircleShape)
            .background(socketGradient)
            .border(
                width = 1.dp,
                color = Color.Black.copy(alpha = 0.50f),
                shape = CircleShape
            )
            .pointerInput(isConnected) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    var activeDirs = emptySet<String>()

                    fun evaluateOffset(pos: Offset) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val dx = pos.x - center.x
                        val dy = pos.y - center.y
                        val dist = hypot(dx, dy)
                        val scale = min(size.width, size.height) / 160f
                        val armHalfWidth = 23f * scale
                        val armMaxReach = 75f * scale
                        val centerDeadzone = 14f * scale

                        // Check each arm's exact physical boundaries (matching HTML SVG hit rects):
                        val isUp = dy in (-armMaxReach)..(-centerDeadzone) && abs(dx) <= armHalfWidth
                        val isDown = dy in centerDeadzone..armMaxReach && abs(dx) <= armHalfWidth
                        val isLeft = dx in (-armMaxReach)..(-centerDeadzone) && abs(dy) <= armHalfWidth
                        val isRight = dx in centerDeadzone..armMaxReach && abs(dy) <= armHalfWidth

                        val newDirs: Set<String> = when {
                            // Strictly single cardinal arm resolution:
                            isUp && isRight -> if (abs(dy) >= abs(dx)) setOf(K.UP) else setOf(K.RIGHT)
                            isUp && isLeft  -> if (abs(dy) >= abs(dx)) setOf(K.UP) else setOf(K.LEFT)
                            isDown && isRight -> if (abs(dy) >= abs(dx)) setOf(K.DOWN) else setOf(K.RIGHT)
                            isDown && isLeft  -> if (abs(dy) >= abs(dx)) setOf(K.DOWN) else setOf(K.LEFT)
                            isUp -> setOf(K.UP)
                            isDown -> setOf(K.DOWN)
                            isLeft -> setOf(K.LEFT)
                            isRight -> setOf(K.RIGHT)
                            else -> emptySet() // Diagonal corners, center hub & out-of-bounds are non-clickable
                        }

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
                        evaluateOffset(down.position)
                        while (true) {
                            val event = awaitPointerEvent()
                            val pointer = event.changes.firstOrNull { it.id == down.id }
                            if (pointer == null || !pointer.pressed) break
                            pointer.consume()
                            evaluateOffset(pointer.position)
                        }
                    } finally {
                        activeDirs.forEach { dir -> currentViewModel.updateButton(dir, false) }
                        pressedDirs = emptySet()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Socket Inset Bottom Shadow: inset 0 -6px 9px rgba(0,0,0,0.70)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val insetH = h * 0.35f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.70f)),
                    startY = h - insetH,
                    endY = h
                ),
                topLeft = Offset(0f, h - insetH),
                size = Size(w, insetH)
            )

            // Socket Inset Neon Ring (.lx-ring): inset 3px, border 2px solid var(--glow), opacity 0.35
            val r = size.minDimension / 2f
            val ringRadius = r - 3.dp.toPx()

            drawCircle(
                color = glowColor.copy(alpha = 0.12f),
                radius = ringRadius,
                style = Stroke(width = 5.dp.toPx())
            )
            drawCircle(
                color = glowColor.copy(alpha = 0.35f),
                radius = ringRadius,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // 3D Tilt Container (.lx-tilt): 150dp x 150dp inside 160dp socket
        Box(
            modifier = Modifier
                .size(150.dp)
                .graphicsLayer {
                    rotationX = tiltXAnim
                    rotationY = tiltYAnim
                    cameraDistance = 14f * density
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val crossPath = createContouredCrossPath(size, insetDp = 0f)
                val insetNeonPath = createContouredCrossPath(size, insetDp = 3.6f)
                val scale = min(size.width, size.height) / 150f

                // 1. D-Pad 3D Drop Shadow: translate(0, 5) with soft Gaussian blur
                for (step in 0..4) {
                    val offY = (2.5f + step * 0.9f) * scale
                    val shadowPath = Path().apply {
                        addPath(createContouredCrossPath(size, insetDp = step * 0.4f), Offset(0f, offY))
                    }
                    drawPath(
                        path = shadowPath,
                        color = Color.Black.copy(alpha = 0.14f)
                    )
                }

                // 2. Main D-Pad Contoured Cross Body
                drawPath(
                    path = crossPath,
                    brush = crossBodyGradient
                )
                drawPath(
                    path = crossPath,
                    color = Color.Black.copy(alpha = 0.50f),
                    style = Stroke(width = 1.dp.toPx())
                )

                // 3. Inset Neon Ribbon Contour (.lx-neon with feMorphology erode 3px to 5px)
                // Soft neon bloom
                drawPath(
                    path = insetNeonPath,
                    color = glowColor.copy(alpha = 0.22f),
                    style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                // Crisp neon core
                drawPath(
                    path = insetNeonPath,
                    color = glowColor.copy(alpha = 0.85f),
                    style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // 4. Gloss Sheen Overlay (dpGloss)
                drawPath(
                    path = crossPath,
                    brush = glossGradient
                )

                // 5. Active Directional Arm Fills (.af: animated opacity 0.30 when pressed)
                fun sx(x: Float) = x * scale
                fun sy(y: Float) = y * scale

                // UP Arm Active Fill (x=52, y=9, w=46, h=46, rx=10)
                if (upFillAlpha > 0f) {
                    drawRoundRect(
                        color = glowColor.copy(alpha = upFillAlpha),
                        topLeft = Offset(sx(52f), sy(9f)),
                        size = Size(sx(46f), sy(46f)),
                        cornerRadius = CornerRadius(sx(10f), sy(10f))
                    )
                }

                // DOWN Arm Active Fill (x=52, y=95, w=46, h=46, rx=10)
                if (downFillAlpha > 0f) {
                    drawRoundRect(
                        color = glowColor.copy(alpha = downFillAlpha),
                        topLeft = Offset(sx(52f), sy(95f)),
                        size = Size(sx(46f), sy(46f)),
                        cornerRadius = CornerRadius(sx(10f), sy(10f))
                    )
                }

                // LEFT Arm Active Fill (x=9, y=52, w=46, h=46, rx=10)
                if (leftFillAlpha > 0f) {
                    drawRoundRect(
                        color = glowColor.copy(alpha = leftFillAlpha),
                        topLeft = Offset(sx(9f), sy(52f)),
                        size = Size(sx(46f), sy(46f)),
                        cornerRadius = CornerRadius(sx(10f), sy(10f))
                    )
                }

                // RIGHT Arm Active Fill (x=95, y=52, w=46, h=46, rx=10)
                if (rightFillAlpha > 0f) {
                    drawRoundRect(
                        color = glowColor.copy(alpha = rightFillAlpha),
                        topLeft = Offset(sx(95f), sy(52f)),
                        size = Size(sx(46f), sy(46f)),
                        cornerRadius = CornerRadius(sx(10f), sy(10f))
                    )
                }

                // 6. Directional Chevrons (.ch: stroke-width: 4, linecap: round, linejoin: round)
                val strokeW = 4.dp.toPx()

                // UP Chevron: points="64,36 75,25 86,36"
                val upChevron = Path().apply {
                    moveTo(sx(64f), sy(36f))
                    lineTo(sx(75f), sy(25f))
                    lineTo(sx(86f), sy(36f))
                }
                if (upGlowAlpha > 0f) {
                    drawPath(upChevron, glowColor.copy(alpha = upGlowAlpha * 0.70f), style = Stroke(strokeW + 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
                drawPath(upChevron, if (upGlowAlpha > 0.5f) Color.White else glowColor.copy(alpha = 0.85f), style = Stroke(strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))

                // DOWN Chevron: points="64,114 75,125 86,114"
                val downChevron = Path().apply {
                    moveTo(sx(64f), sy(114f))
                    lineTo(sx(75f), sy(125f))
                    lineTo(sx(86f), sy(114f))
                }
                if (downGlowAlpha > 0f) {
                    drawPath(downChevron, glowColor.copy(alpha = downGlowAlpha * 0.70f), style = Stroke(strokeW + 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
                drawPath(downChevron, if (downGlowAlpha > 0.5f) Color.White else glowColor.copy(alpha = 0.85f), style = Stroke(strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))

                // LEFT Chevron: points="36,64 25,75 36,86"
                val leftChevron = Path().apply {
                    moveTo(sx(36f), sy(64f))
                    lineTo(sx(25f), sy(75f))
                    lineTo(sx(36f), sy(86f))
                }
                if (leftGlowAlpha > 0f) {
                    drawPath(leftChevron, glowColor.copy(alpha = leftGlowAlpha * 0.70f), style = Stroke(strokeW + 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
                drawPath(leftChevron, if (leftGlowAlpha > 0.5f) Color.White else glowColor.copy(alpha = 0.85f), style = Stroke(strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))

                // RIGHT Chevron: points="114,64 125,75 114,86"
                val rightChevron = Path().apply {
                    moveTo(sx(114f), sy(64f))
                    lineTo(sx(125f), sy(75f))
                    lineTo(sx(114f), sy(86f))
                }
                if (rightGlowAlpha > 0f) {
                    drawPath(rightChevron, glowColor.copy(alpha = rightGlowAlpha * 0.70f), style = Stroke(strokeW + 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
                drawPath(rightChevron, if (rightGlowAlpha > 0.5f) Color.White else glowColor.copy(alpha = 0.85f), style = Stroke(strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))

                // 7. Center Hub Dish & Luminous Pip
                val hubCenter = Offset(sx(75f), sy(75f))
                val hubRadius = sx(12f)
                val dotRadius = sx(3f)

                drawCircle(
                    brush = hubGradient,
                    radius = hubRadius,
                    center = hubCenter
                )
                drawCircle(
                    color = glowColor.copy(alpha = 0.35f),
                    radius = hubRadius,
                    center = hubCenter,
                    style = Stroke(width = 1.dp.toPx())
                )
                drawCircle(
                    color = glowColor.copy(alpha = 0.70f),
                    radius = dotRadius,
                    center = hubCenter
                )
            }
        }

        // 8. Top Glass Lens Overlay (.lx-lens)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Inset highlight crescent: inset 0 2px 2px rgba(255,255,255,0.10)
            drawArc(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.12f),
                        Color.White.copy(alpha = 0.04f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = h * 0.40f
                ),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                size = Size(w - 2.dp.toPx(), h - 2.dp.toPx()),
                style = Stroke(width = 1.2.dp.toPx())
            )

            // Bottom Shadow: inset 0 -3px 5px rgba(0,0,0,0.35)
            drawArc(
                color = Color.Black.copy(alpha = 0.30f),
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                size = Size(w - 2.dp.toPx(), h - 2.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )

            // Specular sheen at bottom-right (circle at 70% 78%, rgba(255,255,255,0.06))
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
