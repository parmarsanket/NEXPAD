package com.sanket.tools.nexpad.ui.components.controller.dpad

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min

/**
 * Backward compatibility shape token.
 */
val crossShape = GenericShape { size, _ ->
    val thirdW = size.width / 3f
    val thirdH = size.height / 3f
    val twoThirdW = thirdW * 2f
    val twoThirdH = thirdH * 2f

    moveTo(thirdW, 0f)
    lineTo(twoThirdW, 0f)
    lineTo(twoThirdW, thirdH)
    lineTo(size.width, thirdH)
    lineTo(size.width, twoThirdH)
    lineTo(twoThirdW, twoThirdH)
    lineTo(twoThirdW, size.height)
    lineTo(thirdW, size.height)
    lineTo(thirdW, twoThirdH)
    lineTo(0f, twoThirdH)
    lineTo(0f, thirdH)
    lineTo(thirdW, thirdH)
    close()
}

/**
 * Updates a pre-allocated [path] with an authentic console-grade contoured cross
 * with rounded arm caps and filleted inner corners. Zero in-draw heap allocations.
 */
fun updateContouredCrossPath(path: Path, size: Size) {
    path.reset()
    val minDim = min(size.width, size.height)
    val scale = minDim / 150f
    val offsetX = (size.width - 150f * scale) / 2f
    val offsetY = (size.height - 150f * scale) / 2f

    val topY = 12f
    val bottomY = 138f
    val leftX = 12f
    val rightX = 138f

    val armTopLeftX = 54f
    val armTopRightX = 96f
    val armLeftTopY = 54f
    val armRightBottomY = 96f

    fun sx(x: Float): Float = offsetX + x * scale
    fun sy(y: Float): Float = offsetY + y * scale

    // Contoured cross with rounded arm caps and smooth filleted corners
    path.moveTo(sx(64f), sy(topY))
    path.lineTo(sx(86f), sy(topY))
    path.quadraticTo(sx(armTopRightX), sy(topY), sx(armTopRightX), sy(22f))
    path.lineTo(sx(armTopRightX), sy(46f))
    path.quadraticTo(sx(armTopRightX), sy(armLeftTopY), sx(104f), sy(armLeftTopY))

    path.lineTo(sx(128f), sy(armLeftTopY))
    path.quadraticTo(sx(rightX), sy(armLeftTopY), sx(rightX), sy(64f))
    path.lineTo(sx(rightX), sy(86f))
    path.quadraticTo(sx(rightX), sy(armRightBottomY), sx(128f), sy(armRightBottomY))
    path.lineTo(sx(104f), sy(armRightBottomY))
    path.quadraticTo(sx(armTopRightX), sy(armRightBottomY), sx(armTopRightX), sy(104f))

    path.lineTo(sx(armTopRightX), sy(128f))
    path.quadraticTo(sx(armTopRightX), sy(bottomY), sx(86f), sy(bottomY))
    path.lineTo(sx(64f), sy(bottomY))
    path.quadraticTo(sx(armTopLeftX), sy(bottomY), sx(armTopLeftX), sy(128f))
    path.lineTo(sx(armTopLeftX), sy(104f))
    path.quadraticTo(sx(armTopLeftX), sy(armRightBottomY), sx(46f), sy(armRightBottomY))

    path.lineTo(sx(22f), sy(armRightBottomY))
    path.quadraticTo(sx(leftX), sy(armRightBottomY), sx(leftX), sy(86f))
    path.lineTo(sx(leftX), sy(64f))
    path.quadraticTo(sx(leftX), sy(armLeftTopY), sx(22f), sy(armLeftTopY))
    path.lineTo(sx(46f), sy(armLeftTopY))
    path.quadraticTo(sx(armTopLeftX), sy(armLeftTopY), sx(armTopLeftX), sy(46f))

    path.lineTo(sx(armTopLeftX), sy(22f))
    path.quadraticTo(sx(armTopLeftX), sy(topY), sx(64f), sy(topY))
    path.close()
}

/**
 * Premium, console-grade mechanical D-Pad cross.
 * Features 3D physical fulcrum pivot kinematics, recessed chassis socket,
 * contoured matte obsidian cross arms, laser-etched illuminated chevrons,
 * central tactile thumb rest dish, directional RGB underglow aura, and crisp haptics.
 */
@Composable
fun RealisticDPad(
    isConnected: Boolean,
    viewModel: GamepadViewModel,
    isRgbEnabled: Boolean,
    onVibrate: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var pressedDirs by remember { mutableStateOf<Set<String>>(emptySet()) }
    val currentOnVibrate by rememberUpdatedState(onVibrate)
    val currentViewModel by rememberUpdatedState(viewModel)

    val K = com.sanket.tools.nexpad.model.NexpadKeys

    val isUpPressed = pressedDirs.contains(K.UP)
    val isDownPressed = pressedDirs.contains(K.DOWN)
    val isLeftPressed = pressedDirs.contains(K.LEFT)
    val isRightPressed = pressedDirs.contains(K.RIGHT)
    val hasActivePress = pressedDirs.isNotEmpty()

    // 3D Physical Fulcrum Rocker Kinematics
    val targetTiltX = remember(pressedDirs) {
        var tilt = 0f
        if (isUpPressed) tilt -= 7.5f
        if (isDownPressed) tilt += 7.5f
        tilt
    }
    val targetTiltY = remember(pressedDirs) {
        var tilt = 0f
        if (isLeftPressed) tilt -= 7.5f
        if (isRightPressed) tilt += 7.5f
        tilt
    }
    val targetPlungeY = remember(pressedDirs) {
        var plunge = 0f
        if (isUpPressed) plunge -= 2.5f
        if (isDownPressed) plunge += 2.5f
        plunge
    }
    val targetScale = if (hasActivePress) 0.96f else 1.0f

    val tiltXAnimState = animateFloatAsState(
        targetValue = targetTiltX,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 850f),
        label = "dpad_tilt_x"
    )
    val tiltYAnimState = animateFloatAsState(
        targetValue = targetTiltY,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 850f),
        label = "dpad_tilt_y"
    )
    val plungeYAnimState = animateFloatAsState(
        targetValue = targetPlungeY,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 850f),
        label = "dpad_plunge_y"
    )
    val scaleAnimState = animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 850f),
        label = "dpad_scale"
    )

    val neonColor = if (isRgbEnabled) Color(0xFF00E5FF) else Color(0xFF4ADE80)
    val rgbBloomAlphaState = animateFloatAsState(
        targetValue = if (hasActivePress) 0.95f else 0.40f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 500f),
        label = "dpad_rgb_bloom"
    )

    val density = LocalDensity.current.density

    // Pre-allocated paths & strokes (Zero-allocation rendering loop)
    val crossPath = remember { Path() }
    val crossBorderStroke = remember(density) { Stroke(width = 1.5f * density) }
    val socketBevelStroke = remember(density) { Stroke(width = 1.8f * density) }
    val centerRestStroke = remember(density) { Stroke(width = 1.4f * density) }

    val upArrowPath = remember { Path() }
    val downArrowPath = remember { Path() }
    val leftArrowPath = remember { Path() }
    val rightArrowPath = remember { Path() }
    val upShadowPath = remember { Path() }
    val downShadowPath = remember { Path() }
    val leftShadowPath = remember { Path() }
    val rightShadowPath = remember { Path() }

    // Reusable color arrays for dynamic radial underglow
    val underglowColors = remember(neonColor) {
        arrayListOf(Color.Transparent, Color.Transparent, Color.Transparent)
    }
    val ambientHubColors = remember(neonColor) {
        arrayListOf(Color.Transparent, Color.Transparent, Color.Transparent)
    }

    // Static Precomputed Brushes
    val socketRadialBrush = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF080B0F),
                Color(0xFF10141B),
                Color(0xFF181D26)
            ),
            center = Offset(0.50f, 0.50f),
            radius = 240f
        )
    }
    val socketBevelBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF5A677B).copy(alpha = 0.70f),
                Color(0xFF2E3846).copy(alpha = 0.45f),
                Color(0xFF13171F)
            )
        )
    }
    val crossSurfaceGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF333A48),
                Color(0xFF222731),
                Color(0xFF15181F),
                Color(0xFF0C0E12)
            ),
            center = Offset(0.48f, 0.46f),
            radius = 220f
        )
    }
    val crossSpecularSheen = remember {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.10f),
                Color.White.copy(alpha = 0.02f),
                Color.Transparent
            ),
            start = Offset(0f, 0f),
            end = Offset(300f, 300f)
        )
    }
    val crossBorderBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF8896AA).copy(alpha = 0.75f),
                Color(0xFF3B4656).copy(alpha = 0.45f),
                Color(0xFF161B22)
            )
        )
    }
    val centerCupGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF060709),
                Color(0xFF11141B),
                Color(0xFF1C222D)
            ),
            center = Offset(0.50f, 0.50f),
            radius = 60f
        )
    }
    val centerRimBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF6B7A8E).copy(alpha = 0.60f),
                Color(0xFF242C38).copy(alpha = 0.40f)
            )
        )
    }

    Box(
        modifier = modifier
            .size(140.dp)
            .graphicsLayer {
                // Root RenderNode isolation
            }
            // 1. Recessed Chassis Socket & Dynamic Directional Underglow Aura
            .drawBehind {
                val center = Offset(size.width / 2f, size.height / 2f)
                val socketRadius = size.minDimension / 2f + 4.dp.toPx()
                val bloomAlpha = rgbBloomAlphaState.value

                // A. Ambient Hub Underglow
                if (isRgbEnabled) {
                    val hubRadius = size.minDimension * 0.58f
                    ambientHubColors[0] = neonColor.copy(alpha = bloomAlpha * 0.45f)
                    ambientHubColors[1] = neonColor.copy(alpha = bloomAlpha * 0.15f)
                    ambientHubColors[2] = Color.Transparent
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = ambientHubColors,
                            center = center,
                            radius = hubRadius
                        ),
                        radius = hubRadius,
                        center = center
                    )

                    // B. Dynamic Directional RGB Underglow Pool flaring under pressed arms
                    val armOffsetPx = size.minDimension * 0.32f
                    fun drawDirectionalUnderglow(dx: Float, dy: Float, isActive: Boolean) {
                        val underglowAlpha = if (isActive) bloomAlpha * 0.90f else (if (hasActivePress) 0.12f else 0.30f)
                        val underglowRadius = size.minDimension * (if (isActive) 0.38f else 0.28f)
                        val poolCenter = Offset(center.x + dx * armOffsetPx, center.y + dy * armOffsetPx)
                        underglowColors[0] = neonColor.copy(alpha = underglowAlpha * 0.70f)
                        underglowColors[1] = neonColor.copy(alpha = underglowAlpha * 0.22f)
                        underglowColors[2] = Color.Transparent
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = underglowColors,
                                center = poolCenter,
                                radius = underglowRadius
                            ),
                            radius = underglowRadius,
                            center = poolCenter
                        )
                    }

                    drawDirectionalUnderglow(0f, -1f, isUpPressed)
                    drawDirectionalUnderglow(0f, 1f, isDownPressed)
                    drawDirectionalUnderglow(-1f, 0f, isLeftPressed)
                    drawDirectionalUnderglow(1f, 0f, isRightPressed)
                }

                // C. Recessed circular chassis socket cavity
                drawCircle(
                    brush = socketRadialBrush,
                    radius = socketRadius,
                    center = center
                )

                // D. Metallic Outer Bevel Bezel Rim
                drawCircle(
                    brush = socketBevelBrush,
                    radius = socketRadius,
                    center = center,
                    style = socketBevelStroke
                )

                // E. Cardinal Laser Axis Tick Accents
                val tickLen = 4.dp.toPx()
                val tickColor = if (isRgbEnabled) neonColor.copy(alpha = if (hasActivePress) 0.65f else 0.35f) else Color.White.copy(alpha = 0.25f)
                drawLine(tickColor, Offset(center.x, center.y - socketRadius), Offset(center.x, center.y - socketRadius + tickLen), strokeWidth = 2f)
                drawLine(tickColor, Offset(center.x, center.y + socketRadius - tickLen), Offset(center.x, center.y + socketRadius), strokeWidth = 2f)
                drawLine(tickColor, Offset(center.x - socketRadius, center.y), Offset(center.x - socketRadius + tickLen, center.y), strokeWidth = 2f)
                drawLine(tickColor, Offset(center.x + socketRadius - tickLen, center.y), Offset(center.x + socketRadius, center.y), strokeWidth = 2f)

                // F. Single-pass hardware drop shadow for the Cross Rocker (zero CPU tessellation / zero stencil)
                val shadowColor = (if (isRgbEnabled) neonColor else Color.Black)
                    .copy(alpha = if (hasActivePress) 0.35f else 0.60f)
                val shadowOffset = if (hasActivePress) 2.dp.toPx() else 5.dp.toPx()
                val armW = size.width * 0.32f
                val armH = size.height * 0.32f
                val armStartX = (size.width - armW) / 2f
                val armStartY = (size.height - armH) / 2f
                val armLen = size.minDimension * 0.88f
                val armReachOffset = (size.minDimension - armLen) / 2f

                drawRoundRect(
                    color = shadowColor,
                    topLeft = Offset(armStartX, armReachOffset + shadowOffset),
                    size = Size(armW, armLen),
                    cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                )
                drawRoundRect(
                    color = shadowColor,
                    topLeft = Offset(armReachOffset, armStartY + shadowOffset),
                    size = Size(armLen, armH),
                    cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                )
            }
            .graphicsLayer {
                rotationX = tiltXAnimState.value
                rotationY = tiltYAnimState.value
                translationY = plungeYAnimState.value * density
                scaleX = scaleAnimState.value
                scaleY = scaleAnimState.value
            }
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
                        val deadzone = size.width * 0.10f
                        val maxRadius = size.width * 0.70f

                        val newDirs = if (dist < deadzone || dist > maxRadius) {
                            emptySet()
                        } else {
                            val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble()))
                            val dirs = mutableSetOf<String>()
                            if (angle in -157.5..-22.5) dirs.add(K.UP)
                            if (angle in 22.5..157.5) dirs.add(K.DOWN)
                            if (angle in -67.5..67.5) dirs.add(K.RIGHT)
                            if (angle < -112.5 || angle > 112.5) dirs.add(K.LEFT)
                            dirs
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
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f
            val minDim = size.minDimension

            // 1. Update contoured cross path (zero heap allocations)
            updateContouredCrossPath(crossPath, size)

            // 2. Draw sculpted matte obsidian dome surface
            drawPath(path = crossPath, brush = crossSurfaceGradient)

            // 3. Draw specular horizon reflection sheen
            drawPath(path = crossPath, brush = crossSpecularSheen)

            // 4. Draw active arm electric illumination surges
            val armW = minDim * 0.32f
            val halfArmW = armW / 2f
            val armReach = minDim * 0.44f
            if (isUpPressed) {
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(neonColor.copy(alpha = 0.60f), Color.Transparent),
                        startY = cy - armReach,
                        endY = cy - armW * 0.3f
                    ),
                    topLeft = Offset(cx - halfArmW, cy - armReach),
                    size = Size(armW, armReach - armW * 0.3f),
                    cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                )
            }
            if (isDownPressed) {
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, neonColor.copy(alpha = 0.60f)),
                        startY = cy + armW * 0.3f,
                        endY = cy + armReach
                    ),
                    topLeft = Offset(cx - halfArmW, cy + armW * 0.3f),
                    size = Size(armW, armReach - armW * 0.3f),
                    cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                )
            }
            if (isLeftPressed) {
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(neonColor.copy(alpha = 0.60f), Color.Transparent),
                        startX = cx - armReach,
                        endX = cx - armW * 0.3f
                    ),
                    topLeft = Offset(cx - armReach, cy - halfArmW),
                    size = Size(armReach - armW * 0.3f, armW),
                    cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                )
            }
            if (isRightPressed) {
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, neonColor.copy(alpha = 0.60f)),
                        startX = cx + armW * 0.3f,
                        endX = cx + armReach
                    ),
                    topLeft = Offset(cx + armW * 0.3f, cy - halfArmW),
                    size = Size(armReach - armW * 0.3f, armW),
                    cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                )
            }

            // 5. Draw authentic metallic bevel border
            drawPath(path = crossPath, brush = crossBorderBrush, style = crossBorderStroke)

            // 6. Draw Directional Chevrons (▲, ▼, ◀, ▶) with embossed drop shadows & glowing cores
            val chevronOffset = minDim * 0.28f
            val chevronW = 7.dp.toPx()
            val chevronH = 5.dp.toPx()
            val chevronThickness = 2.dp.toPx()

            fun drawSculptedChevron(
                arrowPath: Path,
                shadowPath: Path,
                arrowCenter: Offset,
                directionAngle: Float,
                isActive: Boolean
            ) {
                val baseColor = if (isActive) Color.White else Color(0xFFDDE3EC)

                arrowPath.reset()
                arrowPath.moveTo(arrowCenter.x - chevronW, arrowCenter.y + chevronH)
                arrowPath.lineTo(arrowCenter.x, arrowCenter.y - chevronH)
                arrowPath.lineTo(arrowCenter.x + chevronW, arrowCenter.y + chevronH)

                shadowPath.reset()
                shadowPath.moveTo(arrowCenter.x - chevronW, arrowCenter.y + chevronH + 1.5f)
                shadowPath.lineTo(arrowCenter.x, arrowCenter.y - chevronH + 1.5f)
                shadowPath.lineTo(arrowCenter.x + chevronW, arrowCenter.y + chevronH + 1.5f)

                withTransform({
                    rotate(directionAngle, arrowCenter)
                }) {
                    // Embossed shadow
                    drawPath(
                        path = shadowPath,
                        color = Color.Black.copy(alpha = 0.70f),
                        style = Stroke(width = chevronThickness + 0.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    // Neon outer glow when pressed
                    if (isActive) {
                        drawPath(
                            path = arrowPath,
                            color = neonColor.copy(alpha = 0.85f),
                            style = Stroke(width = chevronThickness + 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }
                    // Crisp arrow line
                    drawPath(
                        path = arrowPath,
                        color = baseColor,
                        style = Stroke(width = chevronThickness, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }

            drawSculptedChevron(upArrowPath, upShadowPath, Offset(cx, cy - chevronOffset), 0f, isUpPressed)
            drawSculptedChevron(downArrowPath, downShadowPath, Offset(cx, cy + chevronOffset), 180f, isDownPressed)
            drawSculptedChevron(leftArrowPath, leftShadowPath, Offset(cx - chevronOffset, cy), 270f, isLeftPressed)
            drawSculptedChevron(rightArrowPath, rightShadowPath, Offset(cx + chevronOffset, cy), 90f, isRightPressed)

            // 7. Tactile Center Concave Thumb Cup (Spherical Fulcrum Dish)
            val dishRadius = minDim * 0.16f
            drawCircle(
                brush = centerCupGradient,
                radius = dishRadius,
                center = Offset(cx, cy)
            )
            drawCircle(
                brush = centerRimBrush,
                radius = dishRadius,
                center = Offset(cx, cy),
                style = centerRestStroke
            )
            // Upper specular crescent highlight in dish
            drawArc(
                color = Color.White.copy(alpha = 0.16f),
                startAngle = 190f,
                sweepAngle = 160f,
                useCenter = false,
                topLeft = Offset(cx - dishRadius + 1.5f, cy - dishRadius + 1.5f),
                size = Size((dishRadius - 1.5f) * 2f, (dishRadius - 1.5f) * 2f),
                style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round)
            )
            // Central tactile pivot jewel / LED
            val jewelColor = if (hasActivePress) neonColor else Color(0xFF4A5568)
            drawCircle(
                color = jewelColor.copy(alpha = if (hasActivePress) 0.85f else 0.45f),
                radius = 3.5.dp.toPx(),
                center = Offset(cx, cy)
            )
            drawCircle(
                color = Color.White.copy(alpha = if (hasActivePress) 0.90f else 0.30f),
                radius = 1.5.dp.toPx(),
                center = Offset(cx, cy)
            )
        }
    }
}

/**
 * Dedicated Tactile D-Pad Directional Button for individual UP, DOWN, LEFT, RIGHT layout controls.
 * Features beveled mechanical cap, directional arrow symbol, RGB rim, and damped spring depression.
 */
@Composable
fun RealisticDPadButton(
    direction: String,
    isConnected: Boolean,
    onVibrate: () -> Unit,
    viewModel: GamepadViewModel,
    isRgbEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    val scaleAnimState = animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1.0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 850f),
        label = "dpad_btn_scale"
    )
    val pressOffsetYAnimState = animateFloatAsState(
        targetValue = if (isPressed) 3f else 0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 850f),
        label = "dpad_btn_offset"
    )

    val dirSymbol = run {
        val K = com.sanket.tools.nexpad.model.NexpadKeys
        when (direction.uppercase()) {
            K.UP    -> "▲"
            K.DOWN  -> "▼"
            K.LEFT  -> "◀"
            K.RIGHT -> "▶"
            else    -> direction
        }
    }

    val themeColor = if (isRgbEnabled) Color(0xFF00E5FF) else Color(0xFF4ADE80)
    val shape = RoundedCornerShape(18.dp)

    val rgbBloomAlphaState = animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 0.45f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 500f),
        label = "dpad_btn_rgb_bloom"
    )

    val currentOnVibrate by rememberUpdatedState(onVibrate)
    val currentViewModel by rememberUpdatedState(viewModel)
    val density = LocalDensity.current.density

    val dpadBtnBloomColors = remember(themeColor) {
        arrayListOf(Color.Transparent, Color.Transparent, Color.Transparent)
    }

    Box(
        modifier = modifier
            .size(72.dp)
            .graphicsLayer {
                // Root RenderNode isolation
            }
            .drawBehind {
                if (isRgbEnabled) {
                    val pad = 8.dp.toPx()
                    val rgbBloomAlpha = rgbBloomAlphaState.value
                    dpadBtnBloomColors[0] = themeColor.copy(alpha = rgbBloomAlpha * 0.50f)
                    dpadBtnBloomColors[1] = themeColor.copy(alpha = rgbBloomAlpha * 0.20f)
                    dpadBtnBloomColors[2] = Color.Transparent
                    drawRoundRect(
                        brush = Brush.radialGradient(
                            colors = dpadBtnBloomColors,
                            center = center,
                            radius = size.width * 0.85f
                        ),
                        topLeft = Offset(-pad, -pad),
                        size = Size(size.width + pad * 2, size.height + pad * 2),
                        cornerRadius = CornerRadius(22.dp.toPx(), 22.dp.toPx())
                    )
                }
            }
            .shadow(
                elevation = if (isPressed) 3.dp else 8.dp,
                shape = shape,
                ambientColor = if (isRgbEnabled) themeColor else Color.Black,
                spotColor = if (isRgbEnabled) themeColor else Color.Black
            )
            .graphicsLayer {
                scaleX = scaleAnimState.value
                scaleY = scaleAnimState.value
                translationY = pressOffsetYAnimState.value * density
            }
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = if (isPressed) {
                        listOf(Color(0xFF1E2633), Color(0xFF0F141C))
                    } else {
                        listOf(Color(0xFF323D4D), Color(0xFF1B212B), Color(0xFF101318))
                    }
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF718096).copy(alpha = if (isPressed) 0.35f else 0.70f),
                        Color(0xFF1A202C)
                    )
                ),
                shape = shape
            )
            .pointerInput(direction) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        currentOnVibrate()
                        currentViewModel.updateButton(direction, true)
                        tryAwaitRelease()
                        isPressed = false
                        currentViewModel.updateButton(direction, false)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = dirSymbol,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = if (isPressed) Color.White else themeColor,
                style = androidx.compose.ui.text.TextStyle(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = if (isRgbEnabled) themeColor.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.8f),
                        offset = Offset(0f, 2f),
                        blurRadius = if (isRgbEnabled) 8f else 3f
                    )
                )
            )
            Text(
                text = direction.uppercase(),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (isPressed) Color.White.copy(alpha = 0.95f) else Color.LightGray.copy(alpha = 0.65f)
            )
        }
    }
}
