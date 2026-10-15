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
import com.sanket.tools.nexpad.ui.theme.NeonPalette
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
    val scaleAnimState = animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = tween(durationMillis = 80, easing = FastOutSlowInEasing),
        label = "four_lenses_key_scale_$direction"
    )
    val pressOffsetYAnimState = animateFloatAsState(
        targetValue = if (isPressed) 2.0f else 0f,
        animationSpec = tween(durationMillis = 80, easing = FastOutSlowInEasing),
        label = "four_lenses_key_offset_$direction"
    )
    val ringBloomAlphaState = animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0.70f,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "four_lenses_key_ring_$direction"
    )
    val keyBloomAlphaState = animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 0.45f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "four_lenses_key_rgb_bloom_$direction"
    )

    val density = LocalDensity.current.density

    val baseDomeGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                NeonPalette.MetalBase,
                NeonPalette.MetalShadow,
                NeonPalette.PureBlack
            ),
            center = Offset(0.50f, 0.55f),
            radius = 120f
        )
    }

    val pressedDomeGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                NeonPalette.MetalMid,
                NeonPalette.MetalAbyss,
                NeonPalette.PureBlack
            ),
            center = Offset(0.50f, 0.58f),
            radius = 120f
        )
    }

    val dishGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                NeonPalette.MetalDeep,
                NeonPalette.MetalDark
            ),
            center = Offset(0.50f, 0.60f),
            radius = 80f
        )
    }

    val btnBloomColors = remember(glowColor) {
        arrayListOf(Color.Transparent, Color.Transparent, Color.Transparent)
    }

    val chevronPath = remember(density) {
        val s = (34f * density) / 44f
        Path().apply {
            moveTo(-14f * s, 7f * s)
            lineTo(0f, -7f * s)
            lineTo(14f * s, 7f * s)
        }
    }
    val strokeChevronBloom = remember(density) {
        val s = (34f * density) / 44f
        Stroke(
            width = 6f * s + 3f * density,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    }
    val strokeChevronCore = remember(density) {
        val s = (34f * density) / 44f
        Stroke(
            width = 6f * s,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    }
    val strokeDishEdge = remember(density) { Stroke(width = 1f * density) }
    val strokeRingBloomPressed = remember(density) { Stroke(width = 6f * density) }
    val strokeRingBloomIdle = remember(density) { Stroke(width = 4f * density) }
    val strokeRingCorePressed = remember(density) { Stroke(width = 2.2f * density) }
    val strokeRingCoreIdle = remember(density) { Stroke(width = 1.8f * density) }
    val strokeLensChamfer = remember(density) { Stroke(width = 1.2f * density) }
    val strokeLensBottomShadow = remember(density) { Stroke(width = 1f * density) }

    val bottomUndercutColorsPressed = remember { listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)) }
    val bottomUndercutColorsIdle = remember { listOf(Color.Transparent, Color.Black.copy(alpha = 0.70f)) }
    val pressedTopShadowColors = remember { listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent) }
    val dishInnerShadowColors = remember { listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)) }
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
    val specularColors = remember { listOf(Color.White.copy(alpha = 0.06f), Color.Transparent) }

    Box(
        modifier = modifier
            .size(54.dp)
            .graphicsLayer {
                // Root RenderNode isolation
            }
            .drawBehind {
                if (isRgbEnabled) {
                    val keyBloomAlpha = keyBloomAlphaState.value
                    btnBloomColors[0] = glowColor.copy(alpha = keyBloomAlpha * 0.50f)
                    btnBloomColors[1] = glowColor.copy(alpha = keyBloomAlpha * 0.20f)
                    btnBloomColors[2] = Color.Transparent
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = btnBloomColors,
                            center = center,
                            radius = size.minDimension * 0.85f
                        ),
                        radius = size.minDimension * 0.85f
                    )
                }
            }
            .shadow(
                elevation = if (isPressed) 1.dp else 4.dp,
                shape = CircleShape,
                ambientColor = if (isRgbEnabled) glowColor else Color.Black,
                spotColor = if (isRgbEnabled) glowColor else Color.Black
            )
            .graphicsLayer {
                scaleX = scaleAnimState.value
                scaleY = scaleAnimState.value
                translationY = pressOffsetYAnimState.value * density
            }
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
                    colors = if (isPressed) bottomUndercutColorsPressed else bottomUndercutColorsIdle,
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
                        colors = pressedTopShadowColors,
                        startY = 0f,
                        endY = pressedInsetH
                    ),
                    topLeft = Offset(0f, 0f),
                    size = Size(w, pressedInsetH)
                )
            }

            // 3. Recessed Dish (.lx-window: 34dp circle centered)
            val dishRadius = 17f * density
            drawCircle(
                brush = dishGradient,
                radius = dishRadius,
                center = center
            )
            // Dish inner top shadow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = dishInnerShadowColors,
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
                style = strokeDishEdge
            )

            // 4. Directional Chevron Arrow (SVG viewBox 0 0 44 44: M8 30 L22 16 L36 30)
            withTransform({
                translate(center.x, center.y)
                rotate(degrees = rotationAngle, pivot = Offset.Zero)
            }) {
                // Emissive bloom on press
                if (isPressed) {
                    drawPath(
                        path = chevronPath,
                        color = glowColor.copy(alpha = 0.75f),
                        style = strokeChevronBloom
                    )
                }
                // Crisp core stroke
                drawPath(
                    path = chevronPath,
                    color = if (isPressed) Color.White else glowColor.copy(alpha = 0.90f),
                    style = strokeChevronCore
                )
            }

            // 5. Inset Neon Ring (.lx-ring: inset 3px)
            val ringRadius = r - 3f * density
            // Outer bloom
            drawCircle(
                color = glowColor.copy(alpha = if (isPressed) 0.55f else 0.22f),
                radius = ringRadius,
                center = center,
                style = if (isPressed) strokeRingBloomPressed else strokeRingBloomIdle
            )
            // Crisp core
            drawCircle(
                color = glowColor.copy(alpha = ringBloomAlphaState.value),
                radius = ringRadius,
                center = center,
                style = if (isPressed) strokeRingCorePressed else strokeRingCoreIdle
            )

            // 6. Top Specular Glass Lens (.lx-lens)
            val lensInset = 1f * density
            drawArc(
                brush = Brush.verticalGradient(
                    colors = if (isPressed) lensColorsPressed else lensColorsIdle,
                    startY = 0f,
                    endY = h * 0.40f
                ),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(lensInset, lensInset),
                size = Size(w - lensInset * 2f, h - lensInset * 2f),
                style = strokeLensChamfer
            )

            // Inset bottom shadow arc
            drawArc(
                color = Color.Black.copy(alpha = 0.30f),
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(lensInset, lensInset),
                size = Size(w - lensInset * 2f, h - lensInset * 2f),
                style = strokeLensBottomShadow
            )

            // Bottom-right specular sheen circle
            drawCircle(
                brush = Brush.radialGradient(
                    colors = specularColors,
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
    isConnected: Boolean = true,
    viewModel: GamepadViewModel? = null,
    isRgbEnabled: Boolean = true,
    onVibrate: () -> Unit = {},
    modifier: Modifier = Modifier,
    isInteractive: Boolean = true
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

    val tiltXAnimState = animateFloatAsState(
        targetValue = targetTiltX,
        animationSpec = tween(durationMillis = 80, easing = FastOutSlowInEasing),
        label = "four_lenses_tilt_x"
    )
    val tiltYAnimState = animateFloatAsState(
        targetValue = targetTiltY,
        animationSpec = tween(durationMillis = 80, easing = FastOutSlowInEasing),
        label = "four_lenses_tilt_y"
    )

    // Nord Frost Chrome (#D8DEE9) or Neon Cyan under RGB
    val glowColor = remember(isRgbEnabled) {
        if (isRgbEnabled) NeonPalette.Cyan else NeonPalette.MetalLight
    }

    val hasActivePress = pressedDirs.isNotEmpty()
    val rgbBloomAlphaState = animateFloatAsState(
        targetValue = if (hasActivePress) 0.95f else 0.45f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "four_lenses_rgb_bloom"
    )

    val hubGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                NeonPalette.MetalDarkest,
                NeonPalette.MetalGrit
            ),
            center = Offset(0.50f, 0.60f),
            radius = 60f
        )
    }

    val hubBloomColors = remember(glowColor) {
        arrayListOf(Color.Transparent, Color.Transparent)
    }
    val filamentColors = remember(glowColor) {
        arrayListOf(Color.Transparent, Color.Transparent)
    }
    val podBloomColors = remember(glowColor) {
        arrayListOf(Color.Transparent, Color.Transparent, Color.Transparent)
    }

    val hubShadowColors = remember { listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)) }

    Box(
        modifier = modifier
            .size(164.dp)
            .graphicsLayer {
                // Root RenderNode isolation
            }
            .drawBehind {
                if (isRgbEnabled) {
                    val rgbBloomAlpha = rgbBloomAlphaState.value
                    val podR = 27f * density
                    val upCenter = Offset(center.x, podR)
                    val downCenter = Offset(center.x, size.height - podR)
                    val leftCenter = Offset(podR, center.y)
                    val rightCenter = Offset(size.width - podR, center.y)

                    // 1. Central constellation node bloom
                    hubBloomColors[0] = glowColor.copy(alpha = rgbBloomAlpha * 0.45f)
                    hubBloomColors[1] = Color.Transparent
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = hubBloomColors,
                            center = center,
                            radius = size.minDimension * 0.35f
                        ),
                        radius = size.minDimension * 0.35f,
                        center = center
                    )

                    // 2. Connective energy filaments from central hub to satellites
                    var filamentAlpha = if (isUpPressed) rgbBloomAlpha * 0.90f else (if (hasActivePress) 0.18f else 0.40f)
                    filamentColors[0] = glowColor.copy(alpha = filamentAlpha * 0.5f)
                    filamentColors[1] = glowColor.copy(alpha = filamentAlpha)
                    drawLine(
                        brush = Brush.linearGradient(colors = filamentColors, start = center, end = upCenter),
                        start = center,
                        end = upCenter,
                        strokeWidth = if (isUpPressed) 3.5f * density else 1.8f * density
                    )

                    filamentAlpha = if (isDownPressed) rgbBloomAlpha * 0.90f else (if (hasActivePress) 0.18f else 0.40f)
                    filamentColors[0] = glowColor.copy(alpha = filamentAlpha * 0.5f)
                    filamentColors[1] = glowColor.copy(alpha = filamentAlpha)
                    drawLine(
                        brush = Brush.linearGradient(colors = filamentColors, start = center, end = downCenter),
                        start = center,
                        end = downCenter,
                        strokeWidth = if (isDownPressed) 3.5f * density else 1.8f * density
                    )

                    filamentAlpha = if (isLeftPressed) rgbBloomAlpha * 0.90f else (if (hasActivePress) 0.18f else 0.40f)
                    filamentColors[0] = glowColor.copy(alpha = filamentAlpha * 0.5f)
                    filamentColors[1] = glowColor.copy(alpha = filamentAlpha)
                    drawLine(
                        brush = Brush.linearGradient(colors = filamentColors, start = center, end = leftCenter),
                        start = center,
                        end = leftCenter,
                        strokeWidth = if (isLeftPressed) 3.5f * density else 1.8f * density
                    )

                    filamentAlpha = if (isRightPressed) rgbBloomAlpha * 0.90f else (if (hasActivePress) 0.18f else 0.40f)
                    filamentColors[0] = glowColor.copy(alpha = filamentAlpha * 0.5f)
                    filamentColors[1] = glowColor.copy(alpha = filamentAlpha)
                    drawLine(
                        brush = Brush.linearGradient(colors = filamentColors, start = center, end = rightCenter),
                        start = center,
                        end = rightCenter,
                        strokeWidth = if (isRightPressed) 3.5f * density else 1.8f * density
                    )

                    // 3. Discrete satellite lens pod blooms
                    var podBloomR = podR * (if (isUpPressed) 1.55f else 1.25f)
                    podBloomColors[0] = (if (isUpPressed) Color.White else glowColor).copy(alpha = if (isUpPressed) 0.85f else 0.40f)
                    podBloomColors[1] = glowColor.copy(alpha = if (isUpPressed) 0.40f else 0.15f)
                    podBloomColors[2] = Color.Transparent
                    drawCircle(
                        brush = Brush.radialGradient(colors = podBloomColors, center = upCenter, radius = podBloomR),
                        radius = podBloomR,
                        center = upCenter
                    )

                    podBloomR = podR * (if (isDownPressed) 1.55f else 1.25f)
                    podBloomColors[0] = (if (isDownPressed) Color.White else glowColor).copy(alpha = if (isDownPressed) 0.85f else 0.40f)
                    podBloomColors[1] = glowColor.copy(alpha = if (isDownPressed) 0.40f else 0.15f)
                    podBloomColors[2] = Color.Transparent
                    drawCircle(
                        brush = Brush.radialGradient(colors = podBloomColors, center = downCenter, radius = podBloomR),
                        radius = podBloomR,
                        center = downCenter
                    )

                    podBloomR = podR * (if (isLeftPressed) 1.55f else 1.25f)
                    podBloomColors[0] = (if (isLeftPressed) Color.White else glowColor).copy(alpha = if (isLeftPressed) 0.85f else 0.40f)
                    podBloomColors[1] = glowColor.copy(alpha = if (isLeftPressed) 0.40f else 0.15f)
                    podBloomColors[2] = Color.Transparent
                    drawCircle(
                        brush = Brush.radialGradient(colors = podBloomColors, center = leftCenter, radius = podBloomR),
                        radius = podBloomR,
                        center = leftCenter
                    )

                    podBloomR = podR * (if (isRightPressed) 1.55f else 1.25f)
                    podBloomColors[0] = (if (isRightPressed) Color.White else glowColor).copy(alpha = if (isRightPressed) 0.85f else 0.40f)
                    podBloomColors[1] = glowColor.copy(alpha = if (isRightPressed) 0.40f else 0.15f)
                    podBloomColors[2] = Color.Transparent
                    drawCircle(
                        brush = Brush.radialGradient(colors = podBloomColors, center = rightCenter, radius = podBloomR),
                        radius = podBloomR,
                        center = rightCenter
                    )
                }
            }
            .shadow(
                elevation = if (hasActivePress) 3.dp else 8.dp,
                shape = CircleShape,
                ambientColor = if (isRgbEnabled) glowColor else Color.Black,
                spotColor = if (isRgbEnabled) glowColor else Color.Black
            )
            .then(
                if (isInteractive) {
                    Modifier.pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()
                            var activeDirs = emptySet<String>()

                            fun evaluate(pos: Offset) {
                                val newDirs = resolveFourLensesTouch(pos, size.width.toFloat())
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
                                evaluate(down.position)
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val pointer = event.changes.firstOrNull { it.id == down.id }
                                    if (pointer == null || !pointer.pressed) break
                                    pointer.consume()
                                    evaluate(pointer.position)
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
                    rotationX = tiltXAnimState.value
                    rotationY = tiltYAnimState.value
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
                            colors = hubShadowColors,
                            center = c,
                            radius = r
                        ),
                        radius = r
                    )

                    // Central Luminous Pip (6dp diameter at 0.60 opacity)
                    drawCircle(
                        color = glowColor.copy(alpha = 0.20f),
                        radius = 5f * density,
                        center = c
                    )
                    drawCircle(
                        color = glowColor.copy(alpha = 0.60f),
                        radius = 3f * density,
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

@Composable
fun StaticFourLensesDPad(
    isRgbEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    FourLensesDPad(
        isConnected = false,
        viewModel = null,
        isRgbEnabled = isRgbEnabled,
        onVibrate = {},
        modifier = modifier,
        isInteractive = false
    )
}

