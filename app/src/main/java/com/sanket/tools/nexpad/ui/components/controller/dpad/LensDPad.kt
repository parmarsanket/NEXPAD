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
import com.sanket.tools.nexpad.ui.theme.NeonPalette
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
    isConnected: Boolean = true,
    viewModel: GamepadViewModel? = null,
    isRgbEnabled: Boolean = true,
    onVibrate: () -> Unit = {},
    modifier: Modifier = Modifier,
    isInteractive: Boolean = true
) {
    var pressedDirs by remember { mutableStateOf<Set<String>>(emptySet()) }
    val externalPressedDirs = com.sanket.tools.nexpad.ui.components.controller.LocalPressedControlKeys.current
    val combinedDirs = pressedDirs + externalPressedDirs
    val currentOnVibrate by rememberUpdatedState(onVibrate)
    val currentViewModel by rememberUpdatedState(viewModel)
    val density = LocalDensity.current.density

    val isUpPressed = combinedDirs.contains(K.UP)
    val isDownPressed = combinedDirs.contains(K.DOWN)
    val isLeftPressed = combinedDirs.contains(K.LEFT)
    val isRightPressed = combinedDirs.contains(K.RIGHT)

    // Physical Rocker Tilt Kinematics matching reference CSS:
    // UP: rx = +8°, DOWN: rx = -8°, RIGHT: ry = +8°, LEFT: ry = -8°
    // transition: transform 0.08s ease
    val targetTiltX = remember(combinedDirs) {
        var tilt = 0f
        if (isUpPressed) tilt += 8.0f
        if (isDownPressed) tilt -= 8.0f
        tilt
    }
    val targetTiltY = remember(combinedDirs) {
        var tilt = 0f
        if (isRightPressed) tilt += 8.0f
        if (isLeftPressed) tilt -= 8.0f
        tilt
    }

    val tiltXAnimState = animateFloatAsState(
        targetValue = targetTiltX,
        animationSpec = tween(durationMillis = 80, easing = FastOutSlowInEasing),
        label = "lens_dpad_tilt_x"
    )
    val tiltYAnimState = animateFloatAsState(
        targetValue = targetTiltY,
        animationSpec = tween(durationMillis = 80, easing = FastOutSlowInEasing),
        label = "lens_dpad_tilt_y"
    )

    // Animated Active Arm Fills (.af: opacity 0.30, transition: opacity 0.1s ease)
    val upFillAlphaState = animateFloatAsState(
        targetValue = if (isUpPressed) 0.30f else 0.0f,
        animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing),
        label = "lens_dpad_up_fill"
    )
    val downFillAlphaState = animateFloatAsState(
        targetValue = if (isDownPressed) 0.30f else 0.0f,
        animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing),
        label = "lens_dpad_down_fill"
    )
    val leftFillAlphaState = animateFloatAsState(
        targetValue = if (isLeftPressed) 0.30f else 0.0f,
        animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing),
        label = "lens_dpad_left_fill"
    )
    val rightFillAlphaState = animateFloatAsState(
        targetValue = if (isRightPressed) 0.30f else 0.0f,
        animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing),
        label = "lens_dpad_right_fill"
    )

    // Animated Chevron Drop-Shadow Glow Bloom (.ch: filter drop-shadow(0 0 3px var(--glow)))
    val upGlowAlphaState = animateFloatAsState(
        targetValue = if (isUpPressed) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing),
        label = "lens_dpad_up_glow"
    )
    val downGlowAlphaState = animateFloatAsState(
        targetValue = if (isDownPressed) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing),
        label = "lens_dpad_down_glow"
    )
    val leftGlowAlphaState = animateFloatAsState(
        targetValue = if (isLeftPressed) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing),
        label = "lens_dpad_left_glow"
    )
    val rightGlowAlphaState = animateFloatAsState(
        targetValue = if (isRightPressed) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing),
        label = "lens_dpad_right_glow"
    )

    // Nord Frost Chrome (#D8DEE9) or Neon Cyan under RGB
    val glowColor = remember(isRgbEnabled) {
        if (isRgbEnabled) NeonPalette.Cyan else NeonPalette.MetalLight
    }

    val hasActivePress = pressedDirs.isNotEmpty()
    val rgbBloomAlphaState = animateFloatAsState(
        targetValue = if (hasActivePress) 0.95f else 0.45f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "lens_dpad_rgb_bloom"
    )

    // Socket radial gradient: circle at 50% 55%: #232527 -> #0c0d0e -> #000000
    val socketGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                NeonPalette.MetalBase,
                NeonPalette.MetalShadow,
                NeonPalette.PureBlack
            ),
            center = Offset(0.50f, 0.55f),
            radius = 320f
        )
    }

    // Cross body radial gradient: cx=75, cy=80, r=85: #26282b -> #0c0d0e -> #000000
    val crossBodyGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                NeonPalette.MetalBase,
                NeonPalette.MetalShadow,
                NeonPalette.PureBlack
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
                NeonPalette.MetalDarkest,
                NeonPalette.MetalGrit
            ),
            center = Offset(0.50f, 0.60f),
            radius = 40f
        )
    }

    val causticStops = remember(glowColor) {
        arrayOf(
            0.00f to Color.Transparent,
            0.35f to Color.Transparent,
            0.70f to Color.Transparent,
            1.00f to Color.Transparent
        )
    }
    val strokeFringe = remember(density) { Stroke(width = 2.0f * density) }
    val strokeCaustic1 = remember(density) { Stroke(width = 2.2f * density) }
    val strokeCaustic2 = remember(density) { Stroke(width = 1.4f * density) }

    val socketBottomShadowColors = remember { listOf(Color.Transparent, Color.Black.copy(alpha = 0.70f)) }
    val strokeSocketRingBloom = remember(density) { Stroke(width = 5f * density) }
    val strokeSocketRingCore = remember(density) { Stroke(width = 2f * density) }

    val crossPath = remember(density) {
        createContouredCrossPath(Size(150f * density, 150f * density), insetDp = 0f)
    }
    val insetNeonPath = remember(density) {
        createContouredCrossPath(Size(150f * density, 150f * density), insetDp = 3.6f)
    }
    val shadowPaths = remember(density) {
        val size150 = Size(150f * density, 150f * density)
        (0..4).map { step ->
            val offY = (2.5f + step * 0.9f) * density
            Path().apply {
                addPath(createContouredCrossPath(size150, insetDp = step * 0.4f), Offset(0f, offY))
            }
        }
    }

    val upChevron = remember(density) {
        Path().apply {
            moveTo(64f * density, 36f * density)
            lineTo(75f * density, 25f * density)
            lineTo(86f * density, 36f * density)
        }
    }
    val downChevron = remember(density) {
        Path().apply {
            moveTo(64f * density, 114f * density)
            lineTo(75f * density, 125f * density)
            lineTo(86f * density, 114f * density)
        }
    }
    val leftChevron = remember(density) {
        Path().apply {
            moveTo(36f * density, 64f * density)
            lineTo(25f * density, 75f * density)
            lineTo(36f * density, 86f * density)
        }
    }
    val rightChevron = remember(density) {
        Path().apply {
            moveTo(114f * density, 64f * density)
            lineTo(125f * density, 75f * density)
            lineTo(114f * density, 86f * density)
        }
    }

    val armCornerRadius = remember(density) { CornerRadius(10f * density, 10f * density) }
    val armSize = remember(density) { Size(46f * density, 46f * density) }

    val strokeCrossBorder = remember(density) { Stroke(width = 1f * density) }
    val strokeNeonBloom = remember(density) { Stroke(width = 5f * density, cap = StrokeCap.Round, join = StrokeJoin.Round) }
    val strokeNeonCore = remember(density) { Stroke(width = 1.8f * density, cap = StrokeCap.Round, join = StrokeJoin.Round) }
    val strokeChevronBloom = remember(density) { Stroke(width = 8f * density, cap = StrokeCap.Round, join = StrokeJoin.Round) }
    val strokeChevronCore = remember(density) { Stroke(width = 4f * density, cap = StrokeCap.Round, join = StrokeJoin.Round) }
    val strokeHubBorder = remember(density) { Stroke(width = 1f * density) }

    val strokeGlassChamfer = remember(density) { Stroke(width = 1.2f * density) }
    val strokeGlassShadow = remember(density) { Stroke(width = 1f * density) }

    val shadowColor = remember { Color.Black.copy(alpha = 0.14f) }
    val crossBorderColor = remember { Color.Black.copy(alpha = 0.50f) }
    val neonBloomColor = remember(glowColor) { glowColor.copy(alpha = 0.22f) }
    val neonCoreColor = remember(glowColor) { glowColor.copy(alpha = 0.85f) }
    val hubBorderColor = remember(glowColor) { glowColor.copy(alpha = 0.35f) }
    val hubPipColor = remember(glowColor) { glowColor.copy(alpha = 0.70f) }

    val glassHighlightColors = remember {
        listOf(
            Color.White.copy(alpha = 0.12f),
            Color.White.copy(alpha = 0.04f),
            Color.Transparent
        )
    }
    val glassSpecularColors = remember {
        listOf(
            Color.White.copy(alpha = 0.06f),
            Color.Transparent
        )
    }

    Box(
        modifier = modifier
            .size(160.dp)
            .graphicsLayer {
                // Root RenderNode isolation
            }
            .drawBehind {
                if (isRgbEnabled) {
                    val tiltXAnim = tiltXAnimState.value
                    val tiltYAnim = tiltYAnimState.value
                    val rgbBloomAlpha = rgbBloomAlphaState.value
                    val causticCenter = Offset(
                        center.x + tiltYAnim * 1.8f,
                        center.y - tiltXAnim * 1.8f
                    )
                    // 1. Primary optical caustic bloom
                    causticStops[0] = 0.00f to Color.White.copy(alpha = if (hasActivePress) 0.65f else 0.25f)
                    causticStops[1] = 0.35f to glowColor.copy(alpha = rgbBloomAlpha * 0.55f)
                    causticStops[2] = 0.70f to glowColor.copy(alpha = rgbBloomAlpha * 0.20f)
                    causticStops[3] = 1.00f to Color.Transparent
                    drawCircle(
                        brush = Brush.radialGradient(
                            colorStops = causticStops,
                            center = causticCenter,
                            radius = size.minDimension * 0.95f
                        ),
                        radius = size.minDimension * 0.95f,
                        center = causticCenter
                    )

                    // 2. Chromatic aberration edge fringe (magenta/cyan prismatic split)
                    val fringeRadius = size.minDimension * 0.54f
                    drawCircle(
                        color = NeonPalette.NeonPink.copy(alpha = if (hasActivePress) 0.45f else 0.18f),
                        radius = fringeRadius + 3f,
                        center = Offset(causticCenter.x - 2f, causticCenter.y),
                        style = strokeFringe
                    )
                    drawCircle(
                        color = NeonPalette.Cyan.copy(alpha = if (hasActivePress) 0.55f else 0.22f),
                        radius = fringeRadius - 1f,
                        center = Offset(causticCenter.x + 2f, causticCenter.y),
                        style = strokeFringe
                    )

                    // 3. Dual concentric optical caustic rings
                    val causticR1 = size.minDimension * 0.48f
                    val causticR2 = size.minDimension * 0.40f
                    drawCircle(
                        color = glowColor.copy(alpha = if (hasActivePress) 0.75f else 0.35f),
                        radius = causticR1,
                        center = causticCenter,
                        style = strokeCaustic1
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = if (hasActivePress) 0.85f else 0.30f),
                        radius = causticR2,
                        center = causticCenter,
                        style = strokeCaustic2
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
            .then(
                if (isInteractive) {
                    Modifier.pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()
                            var activeDirs = emptySet<String>()

                            fun evaluateOffset(pos: Offset) {
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val dx = pos.x - center.x
                                val dy = pos.y - center.y
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
                                    isUp && isRight -> setOf(K.UP, K.RIGHT)
                                    isUp && isLeft  -> setOf(K.UP, K.LEFT)
                                    isDown && isRight -> setOf(K.DOWN, K.RIGHT)
                                    isDown && isLeft  -> setOf(K.DOWN, K.LEFT)
                                    isUp -> setOf(K.UP)
                                    isDown -> setOf(K.DOWN)
                                    isLeft -> setOf(K.LEFT)
                                    isRight -> setOf(K.RIGHT)
                                    else -> emptySet() // Diagonal corners, center hub & out-of-bounds are non-clickable
                                }

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
        // Socket Inset Bottom Shadow: inset 0 -6px 9px rgba(0,0,0,0.70)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val insetH = h * 0.35f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = socketBottomShadowColors,
                    startY = h - insetH,
                    endY = h
                ),
                topLeft = Offset(0f, h - insetH),
                size = Size(w, insetH)
            )

            // Socket Inset Neon Ring (.lx-ring): inset 3px, border 2px solid var(--glow), opacity 0.35
            val r = size.minDimension / 2f
            val ringRadius = r - 3f * density

            drawCircle(
                color = glowColor.copy(alpha = 0.12f),
                radius = ringRadius,
                style = strokeSocketRingBloom
            )
            drawCircle(
                color = glowColor.copy(alpha = 0.35f),
                radius = ringRadius,
                style = strokeSocketRingCore
            )
        }

        // 3D Tilt Container (.lx-tilt): 150dp x 150dp inside 160dp socket
        Box(
            modifier = Modifier
                .size(150.dp)
                .graphicsLayer {
                    rotationX = tiltXAnimState.value
                    rotationY = tiltYAnimState.value
                    cameraDistance = 14f * density
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val upFillAlpha = upFillAlphaState.value
                val downFillAlpha = downFillAlphaState.value
                val leftFillAlpha = leftFillAlphaState.value
                val rightFillAlpha = rightFillAlphaState.value
                val upGlowAlpha = upGlowAlphaState.value
                val downGlowAlpha = downGlowAlphaState.value
                val leftGlowAlpha = leftGlowAlphaState.value
                val rightGlowAlpha = rightGlowAlphaState.value

                // 1. D-Pad 3D Drop Shadow: translate(0, 5) with soft Gaussian blur
                for (i in shadowPaths.indices) {
                    drawPath(
                        path = shadowPaths[i],
                        color = shadowColor
                    )
                }

                // 2. Main D-Pad Contoured Cross Body
                drawPath(
                    path = crossPath,
                    brush = crossBodyGradient
                )
                drawPath(
                    path = crossPath,
                    color = crossBorderColor,
                    style = strokeCrossBorder
                )

                // 3. Inset Neon Ribbon Contour (.lx-neon with feMorphology erode 3px to 5px)
                // Soft neon bloom
                drawPath(
                    path = insetNeonPath,
                    color = neonBloomColor,
                    style = strokeNeonBloom
                )
                // Crisp neon core
                drawPath(
                    path = insetNeonPath,
                    color = neonCoreColor,
                    style = strokeNeonCore
                )

                // 4. Gloss Sheen Overlay (dpGloss)
                drawPath(
                    path = crossPath,
                    brush = glossGradient
                )

                // 5. Active Directional Arm Fills (.af: animated opacity 0.30 when pressed)
                // UP Arm Active Fill (x=52, y=9, w=46, h=46, rx=10)
                if (upFillAlpha > 0f) {
                    drawRoundRect(
                        color = glowColor.copy(alpha = upFillAlpha),
                        topLeft = Offset(52f * density, 9f * density),
                        size = armSize,
                        cornerRadius = armCornerRadius
                    )
                }

                // DOWN Arm Active Fill (x=52, y=95, w=46, h=46, rx=10)
                if (downFillAlpha > 0f) {
                    drawRoundRect(
                        color = glowColor.copy(alpha = downFillAlpha),
                        topLeft = Offset(52f * density, 95f * density),
                        size = armSize,
                        cornerRadius = armCornerRadius
                    )
                }

                // LEFT Arm Active Fill (x=9, y=52, w=46, h=46, rx=10)
                if (leftFillAlpha > 0f) {
                    drawRoundRect(
                        color = glowColor.copy(alpha = leftFillAlpha),
                        topLeft = Offset(9f * density, 52f * density),
                        size = armSize,
                        cornerRadius = armCornerRadius
                    )
                }

                // RIGHT Arm Active Fill (x=95, y=52, w=46, h=46, rx=10)
                if (rightFillAlpha > 0f) {
                    drawRoundRect(
                        color = glowColor.copy(alpha = rightFillAlpha),
                        topLeft = Offset(95f * density, 52f * density),
                        size = armSize,
                        cornerRadius = armCornerRadius
                    )
                }

                // 6. Directional Chevrons (.ch: stroke-width: 4, linecap: round, linejoin: round)
                if (upGlowAlpha > 0f) {
                    drawPath(upChevron, glowColor.copy(alpha = upGlowAlpha * 0.70f), style = strokeChevronBloom)
                }
                drawPath(upChevron, if (upGlowAlpha > 0.5f) Color.White else neonCoreColor, style = strokeChevronCore)

                if (downGlowAlpha > 0f) {
                    drawPath(downChevron, glowColor.copy(alpha = downGlowAlpha * 0.70f), style = strokeChevronBloom)
                }
                drawPath(downChevron, if (downGlowAlpha > 0.5f) Color.White else neonCoreColor, style = strokeChevronCore)

                if (leftGlowAlpha > 0f) {
                    drawPath(leftChevron, glowColor.copy(alpha = leftGlowAlpha * 0.70f), style = strokeChevronBloom)
                }
                drawPath(leftChevron, if (leftGlowAlpha > 0.5f) Color.White else neonCoreColor, style = strokeChevronCore)

                if (rightGlowAlpha > 0f) {
                    drawPath(rightChevron, glowColor.copy(alpha = rightGlowAlpha * 0.70f), style = strokeChevronBloom)
                }
                drawPath(rightChevron, if (rightGlowAlpha > 0.5f) Color.White else neonCoreColor, style = strokeChevronCore)

                // 7. Center Hub Dish & Luminous Pip
                val hubCenter = Offset(75f * density, 75f * density)
                val hubRadius = 12f * density
                val dotRadius = 3f * density

                drawCircle(
                    brush = hubGradient,
                    radius = hubRadius,
                    center = hubCenter
                )
                drawCircle(
                    color = hubBorderColor,
                    radius = hubRadius,
                    center = hubCenter,
                    style = strokeHubBorder
                )
                drawCircle(
                    color = hubPipColor,
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
            val lensInset = 1f * density
            drawArc(
                brush = Brush.verticalGradient(
                    colors = glassHighlightColors,
                    startY = 0f,
                    endY = h * 0.40f
                ),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(lensInset, lensInset),
                size = Size(w - lensInset * 2f, h - lensInset * 2f),
                style = strokeGlassChamfer
            )

            // Bottom Shadow: inset 0 -3px 5px rgba(0,0,0,0.35)
            drawArc(
                color = Color.Black.copy(alpha = 0.30f),
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(lensInset, lensInset),
                size = Size(w - lensInset * 2f, h - lensInset * 2f),
                style = strokeGlassShadow
            )

            // Specular sheen at bottom-right (circle at 70% 78%, rgba(255,255,255,0.06))
            drawCircle(
                brush = Brush.radialGradient(
                    colors = glassSpecularColors,
                    center = Offset(w * 0.70f, h * 0.78f),
                    radius = w * 0.22f
                ),
                center = Offset(w * 0.70f, h * 0.78f),
                radius = w * 0.22f
            )
        }
    }
}

@Composable
fun StaticLensDPad(
    isRgbEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    LensDPad(
        isConnected = false,
        viewModel = null,
        isRgbEnabled = isRgbEnabled,
        onVibrate = {},
        modifier = modifier,
        isInteractive = false
    )
}

