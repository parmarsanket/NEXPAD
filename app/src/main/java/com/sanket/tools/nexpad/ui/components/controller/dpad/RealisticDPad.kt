package com.sanket.tools.nexpad.ui.components.controller.dpad

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
 * Backward compatibility path generator.
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
 * Futuristic Orbital Quad-Satellite D-Pad.
 *
 * Designed to seamlessly match the sleek, glowing obsidian-disc aesthetic and radiant aura
 * of the action buttons and joysticks. Eliminates outdated blocky cross geometry and fake 3D tilts:
 * - 4 floating aerodynamic obsidian satellite lenses (UP, DOWN, LEFT, RIGHT).
 * - Vibrant glowing neon perimeter rings matching the Action Buttons' aura.
 * - Precision laser-etched illuminated directional chevrons with acrylic lens reflections.
 * - Central cyber reactor core with dynamic energy conduit laser rails.
 * - Continuous 8-way touch & glide gesture input with multi-directional diagonal arc illumination.
 * - Damped mechanical spring tactile depression (zero-allocation hardware pipeline).
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

    val neonColor = if (isRgbEnabled) Color(0xFF00E5FF) else Color(0xFF4ADE80)

    // Snappy Harmonic Spring Kinematics for each Satellite Node
    val upScale = animateFloatAsState(
        targetValue = if (isUpPressed) 0.92f else 1.0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 850f),
        label = "dpad_up_scale"
    )
    val upOffsetY = animateFloatAsState(
        targetValue = if (isUpPressed) 2.5f else 0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 850f),
        label = "dpad_up_offset"
    )
    val upBloomAlpha = animateFloatAsState(
        targetValue = if (isUpPressed) 1.0f else 0.35f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 850f),
        label = "dpad_up_bloom"
    )

    val downScale = animateFloatAsState(
        targetValue = if (isDownPressed) 0.92f else 1.0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 850f),
        label = "dpad_down_scale"
    )
    val downOffsetY = animateFloatAsState(
        targetValue = if (isDownPressed) 2.5f else 0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 850f),
        label = "dpad_down_offset"
    )
    val downBloomAlpha = animateFloatAsState(
        targetValue = if (isDownPressed) 1.0f else 0.35f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 850f),
        label = "dpad_down_bloom"
    )

    val leftScale = animateFloatAsState(
        targetValue = if (isLeftPressed) 0.92f else 1.0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 850f),
        label = "dpad_left_scale"
    )
    val leftOffsetY = animateFloatAsState(
        targetValue = if (isLeftPressed) 2.5f else 0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 850f),
        label = "dpad_left_offset"
    )
    val leftBloomAlpha = animateFloatAsState(
        targetValue = if (isLeftPressed) 1.0f else 0.35f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 850f),
        label = "dpad_left_bloom"
    )

    val rightScale = animateFloatAsState(
        targetValue = if (isRightPressed) 0.92f else 1.0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 850f),
        label = "dpad_right_scale"
    )
    val rightOffsetY = animateFloatAsState(
        targetValue = if (isRightPressed) 2.5f else 0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 850f),
        label = "dpad_right_offset"
    )
    val rightBloomAlpha = animateFloatAsState(
        targetValue = if (isRightPressed) 1.0f else 0.35f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 850f),
        label = "dpad_right_bloom"
    )

    val corePulse = animateFloatAsState(
        targetValue = if (hasActivePress) 1.0f else 0.35f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 850f),
        label = "dpad_core_pulse"
    )

    val density = LocalDensity.current.density

    // Pre-allocated paths & strokes (Zero-allocation rendering loop)
    val upChevronPath = remember { Path() }
    val downChevronPath = remember { Path() }
    val leftChevronPath = remember { Path() }
    val rightChevronPath = remember { Path() }

    val stroke1dp = remember(density) { Stroke(width = 1f * density) }
    val stroke1_5dp = remember(density) { Stroke(width = 1.5f * density) }
    val stroke1_8dp = remember(density) { Stroke(width = 1.8f * density) }
    val stroke2_4dp = remember(density) { Stroke(width = 2.4f * density, cap = StrokeCap.Round, join = StrokeJoin.Round) }
    val stroke3_5dp = remember(density) { Stroke(width = 3.5f * density) }
    val stroke4_5dp = remember(density) { Stroke(width = 4.5f * density, cap = StrokeCap.Round, join = StrokeJoin.Round) }
    val stroke5dp = remember(density) { Stroke(width = 5f * density, cap = StrokeCap.Round) }
    val strokeArc3dp = remember(density) { Stroke(width = 3.2f * density, cap = StrokeCap.Round) }

    // Pre-allocated color arrays for radial bloom
    val bloomColors = remember(neonColor) {
        arrayListOf(Color.Transparent, Color.Transparent, Color.Transparent)
    }

    // Static Precomputed Brushes (matching RealisticButton obsidian caps)
    val baseDomeGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF262A32),
                Color(0xFF111419),
                Color(0xFF07080A)
            ),
            center = Offset(0.50f, 0.45f),
            radius = 180f
        )
    }
    val pressedDomeGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF1B1F26),
                Color(0xFF0B0D11),
                Color(0xFF030405)
            ),
            center = Offset(0.50f, 0.55f),
            radius = 180f
        )
    }
    val coreBevelGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF2E3846),
                Color(0xFF151921),
                Color(0xFF0B0E13)
            ),
            center = Offset(0.50f, 0.45f),
            radius = 90f
        )
    }
    val stageTrackBrush = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF181E28).copy(alpha = 0.40f),
                Color(0xFF0C1016).copy(alpha = 0.25f),
                Color.Transparent
            )
        )
    }

    Box(
        modifier = modifier
            .size(190.dp)
            .graphicsLayer {
                // Root RenderNode isolation
            }
            .pointerInput(isConnected) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    var activeDirs = emptySet<String>()

                    fun evaluateOffset(pos: Offset) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val dx = pos.x - cx
                        val dy = pos.y - cy
                        val dist = hypot(dx, dy)
                        val deadzone = size.width * 0.08f
                        val maxRadius = size.width * 0.52f

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

            val orbitRadius = minDim * 0.285f
            val satelliteRadius = minDim * 0.155f
            val coreRadius = minDim * 0.115f

            val upCenter = Offset(cx, cy - orbitRadius)
            val downCenter = Offset(cx, cy + orbitRadius)
            val leftCenter = Offset(cx - orbitRadius, cy)
            val rightCenter = Offset(cx + orbitRadius, cy)

            // 1. Ambient Background Stage & Concentric Precision Rings
            drawCircle(
                brush = stageTrackBrush,
                radius = minDim * 0.48f,
                center = Offset(cx, cy)
            )
            // Outer guideline ring
            drawCircle(
                color = Color(0xFF283242).copy(alpha = 0.35f),
                radius = orbitRadius,
                center = Offset(cx, cy),
                style = stroke1dp
            )
            // Outer perimeter safety boundary
            drawCircle(
                color = Color(0xFF1E2532).copy(alpha = 0.25f),
                radius = minDim * 0.46f,
                center = Offset(cx, cy),
                style = stroke1dp
            )

            // 2. Cardinal & Diagonal Energy Conduits (Light Tracks)
            // Dark base rails from center to satellites
            drawLine(Color(0xFF141922), Offset(cx, cy), upCenter, strokeWidth = stroke5dp.width, cap = StrokeCap.Round)
            drawLine(Color(0xFF141922), Offset(cx, cy), downCenter, strokeWidth = stroke5dp.width, cap = StrokeCap.Round)
            drawLine(Color(0xFF141922), Offset(cx, cy), leftCenter, strokeWidth = stroke5dp.width, cap = StrokeCap.Round)
            drawLine(Color(0xFF141922), Offset(cx, cy), rightCenter, strokeWidth = stroke5dp.width, cap = StrokeCap.Round)

            // Dynamic Illuminated Energy Surges along active conduits
            fun drawActiveConduit(target: Offset, alpha: Float) {
                if (alpha > 0.40f) {
                    // Outer diffuse aura
                    drawLine(
                        color = neonColor.copy(alpha = alpha * 0.40f),
                        start = Offset(cx, cy),
                        end = target,
                        strokeWidth = stroke5dp.width,
                        cap = StrokeCap.Round
                    )
                    // Crisp laser core
                    drawLine(
                        color = Color.White.copy(alpha = alpha * 0.85f),
                        start = Offset(cx, cy),
                        end = target,
                        strokeWidth = stroke1_8dp.width,
                        cap = StrokeCap.Round
                    )
                }
            }
            drawActiveConduit(upCenter, upBloomAlpha.value)
            drawActiveConduit(downCenter, downBloomAlpha.value)
            drawActiveConduit(leftCenter, leftBloomAlpha.value)
            drawActiveConduit(rightCenter, rightBloomAlpha.value)

            // Diagonal Energy Arc Bridges (Active when two adjacent directions are pressed)
            val arcRect = Size(orbitRadius * 2f, orbitRadius * 2f)
            val arcTopLeft = Offset(cx - orbitRadius, cy - orbitRadius)
            fun drawDiagonalArc(startAngle: Float, active: Boolean) {
                if (active) {
                    drawArc(
                        color = neonColor.copy(alpha = 0.90f),
                        startAngle = startAngle,
                        sweepAngle = 90f,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcRect,
                        style = strokeArc3dp
                    )
                } else {
                    // Subtle inactive diagonal guide marker
                    val rad = Math.toRadians((startAngle + 45.0)).toFloat()
                    val tickX = cx + kotlin.math.cos(rad) * orbitRadius
                    val tickY = cy + kotlin.math.sin(rad) * orbitRadius
                    drawCircle(
                        color = Color(0xFF334155).copy(alpha = 0.40f),
                        radius = 2.dp.toPx(),
                        center = Offset(tickX, tickY)
                    )
                }
            }
            drawDiagonalArc(-90f, isUpPressed && isRightPressed)    // UP-RIGHT
            drawDiagonalArc(0f, isDownPressed && isRightPressed)     // DOWN-RIGHT
            drawDiagonalArc(90f, isDownPressed && isLeftPressed)     // DOWN-LEFT
            drawDiagonalArc(180f, isUpPressed && isLeftPressed)      // UP-LEFT

            // 3. Central Cyber Tactical Core (Reactor Hub)
            // Outer bevel
            drawCircle(
                brush = coreBevelGradient,
                radius = coreRadius,
                center = Offset(cx, cy)
            )
            // Outer core border
            drawCircle(
                color = Color(0xFF4A5568).copy(alpha = 0.50f),
                radius = coreRadius,
                center = Offset(cx, cy),
                style = stroke1_5dp
            )
            // Glowing inner cyber ring
            val pulse = corePulse.value
            drawCircle(
                color = neonColor.copy(alpha = pulse * 0.70f),
                radius = coreRadius * 0.65f,
                center = Offset(cx, cy),
                style = stroke1_8dp
            )
            // Core jewel LED
            drawCircle(
                color = neonColor.copy(alpha = pulse * 0.40f),
                radius = coreRadius * 0.35f,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = if (hasActivePress) Color.White else neonColor.copy(alpha = 0.50f),
                radius = coreRadius * 0.18f,
                center = Offset(cx, cy)
            )

            // 4. Render 4 Floating Obsidian Satellite Nodes (UP, DOWN, LEFT, RIGHT)
            fun drawSatelliteNode(
                centerPos: Offset,
                scale: Float,
                offsetY: Float,
                bloomAlpha: Float,
                isPressed: Boolean,
                drawChevron: (nodeCenter: Offset) -> Unit
            ) {
                val nodeCenter = Offset(centerPos.x, centerPos.y + offsetY * density)
                val r = satelliteRadius * scale

                // A. Ambient RGB Bloom behind node
                if (isRgbEnabled) {
                    val bloomRadius = r * 1.55f
                    bloomColors[0] = neonColor.copy(alpha = bloomAlpha * 0.55f)
                    bloomColors[1] = neonColor.copy(alpha = bloomAlpha * 0.18f)
                    bloomColors[2] = Color.Transparent
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = bloomColors,
                            center = nodeCenter,
                            radius = bloomRadius
                        ),
                        radius = bloomRadius,
                        center = nodeCenter
                    )
                }

                // B. Single-pass soft drop shadow
                val shadowAlpha = if (isPressed) 0.30f else 0.55f
                drawCircle(
                    color = Color.Black.copy(alpha = shadowAlpha),
                    radius = r + 2.dp.toPx(),
                    center = Offset(nodeCenter.x, nodeCenter.y + 3.dp.toPx())
                )

                // C. Obsidian Dome Surface
                drawCircle(
                    brush = if (isPressed) pressedDomeGradient else baseDomeGradient,
                    radius = r,
                    center = nodeCenter
                )

                // D. Inset outer cap edge rim
                drawCircle(
                    color = Color.Black.copy(alpha = 0.60f),
                    radius = r - 0.5.dp.toPx(),
                    center = nodeCenter,
                    style = stroke1dp
                )

                // E. Inner Glowing Neon Ring (matching RealisticButton)
                val ringRadius = r - 3.dp.toPx()
                // Outer diffuse ring bloom
                drawCircle(
                    color = neonColor.copy(alpha = if (isPressed) 0.65f else 0.22f),
                    radius = ringRadius,
                    center = nodeCenter,
                    style = stroke3_5dp
                )
                // Core crisp neon ring
                drawCircle(
                    color = neonColor.copy(alpha = if (isPressed) 1.0f else 0.65f),
                    radius = ringRadius,
                    center = nodeCenter,
                    style = stroke1_8dp
                )

                // F. Acrylic Glass Specular Arc Reflection (Top Crescent)
                val lensInset = 2.dp.toPx()
                drawArc(
                    color = Color.White.copy(alpha = if (isPressed) 0.25f else 0.14f),
                    startAngle = 190f,
                    sweepAngle = 160f,
                    useCenter = false,
                    topLeft = Offset(nodeCenter.x - r + lensInset, nodeCenter.y - r + lensInset),
                    size = Size((r - lensInset) * 2f, (r - lensInset) * 2f),
                    style = stroke1_5dp
                )

                // G. Directional Laser Chevron
                drawChevron(nodeCenter)
            }

            // Directional Chevron Dimensions
            val chW = 8.5f * density
            val chH = 5.5f * density

            // Draw UP Satellite
            drawSatelliteNode(upCenter, upScale.value, upOffsetY.value, upBloomAlpha.value, isUpPressed) { nodeCenter ->
                upChevronPath.reset()
                upChevronPath.moveTo(nodeCenter.x - chW, nodeCenter.y + chH * 0.4f)
                upChevronPath.lineTo(nodeCenter.x, nodeCenter.y - chH * 0.7f)
                upChevronPath.lineTo(nodeCenter.x + chW, nodeCenter.y + chH * 0.4f)

                // Embossed shadow
                drawPath(upChevronPath, color = Color.Black.copy(alpha = 0.75f), style = stroke2_4dp)
                // Active laser glow
                if (isUpPressed) {
                    drawPath(upChevronPath, color = neonColor.copy(alpha = 0.75f), style = stroke4_5dp)
                }
                // Crisp core
                drawPath(upChevronPath, color = if (isUpPressed) Color.White else neonColor, style = stroke2_4dp)
            }

            // Draw DOWN Satellite
            drawSatelliteNode(downCenter, downScale.value, downOffsetY.value, downBloomAlpha.value, isDownPressed) { nodeCenter ->
                downChevronPath.reset()
                downChevronPath.moveTo(nodeCenter.x - chW, nodeCenter.y - chH * 0.4f)
                downChevronPath.lineTo(nodeCenter.x, nodeCenter.y + chH * 0.7f)
                downChevronPath.lineTo(nodeCenter.x + chW, nodeCenter.y - chH * 0.4f)

                drawPath(downChevronPath, color = Color.Black.copy(alpha = 0.75f), style = stroke2_4dp)
                if (isDownPressed) {
                    drawPath(downChevronPath, color = neonColor.copy(alpha = 0.75f), style = stroke4_5dp)
                }
                drawPath(downChevronPath, color = if (isDownPressed) Color.White else neonColor, style = stroke2_4dp)
            }

            // Draw LEFT Satellite
            drawSatelliteNode(leftCenter, leftScale.value, leftOffsetY.value, leftBloomAlpha.value, isLeftPressed) { nodeCenter ->
                leftChevronPath.reset()
                leftChevronPath.moveTo(nodeCenter.x + chH * 0.4f, nodeCenter.y - chW)
                leftChevronPath.lineTo(nodeCenter.x - chH * 0.7f, nodeCenter.y)
                leftChevronPath.lineTo(nodeCenter.x + chH * 0.4f, nodeCenter.y + chW)

                drawPath(leftChevronPath, color = Color.Black.copy(alpha = 0.75f), style = stroke2_4dp)
                if (isLeftPressed) {
                    drawPath(leftChevronPath, color = neonColor.copy(alpha = 0.75f), style = stroke4_5dp)
                }
                drawPath(leftChevronPath, color = if (isLeftPressed) Color.White else neonColor, style = stroke2_4dp)
            }

            // Draw RIGHT Satellite
            drawSatelliteNode(rightCenter, rightScale.value, rightOffsetY.value, rightBloomAlpha.value, isRightPressed) { nodeCenter ->
                rightChevronPath.reset()
                rightChevronPath.moveTo(nodeCenter.x - chH * 0.4f, nodeCenter.y - chW)
                rightChevronPath.lineTo(nodeCenter.x + chH * 0.7f, nodeCenter.y)
                rightChevronPath.lineTo(nodeCenter.x - chH * 0.4f, nodeCenter.y + chW)

                drawPath(rightChevronPath, color = Color.Black.copy(alpha = 0.75f), style = stroke2_4dp)
                if (isRightPressed) {
                    drawPath(rightChevronPath, color = neonColor.copy(alpha = 0.75f), style = stroke4_5dp)
                }
                drawPath(rightChevronPath, color = if (isRightPressed) Color.White else neonColor, style = stroke2_4dp)
            }
        }
    }
}

/**
 * Dedicated Tactile D-Pad Directional Button for discrete UP, DOWN, LEFT, RIGHT layout controls.
 * Features matching floating obsidian satellite disc with glowing neon ring, laser chevron,
 * and damped spring depression.
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
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 850f),
        label = "dpad_btn_scale"
    )
    val pressOffsetYAnimState = animateFloatAsState(
        targetValue = if (isPressed) 2.5f else 0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 850f),
        label = "dpad_btn_offset"
    )
    val rgbBloomAlphaState = animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0.40f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 500f),
        label = "dpad_btn_rgb_bloom"
    )

    val themeColor = if (isRgbEnabled) Color(0xFF00E5FF) else Color(0xFF4ADE80)
    val currentOnVibrate by rememberUpdatedState(onVibrate)
    val currentViewModel by rememberUpdatedState(viewModel)
    val density = LocalDensity.current.density

    val chevronPath = remember { Path() }
    val stroke1dp = remember(density) { Stroke(width = 1f * density) }
    val stroke1_5dp = remember(density) { Stroke(width = 1.5f * density) }
    val stroke1_8dp = remember(density) { Stroke(width = 1.8f * density) }
    val stroke2_6dp = remember(density) { Stroke(width = 2.6f * density, cap = StrokeCap.Round, join = StrokeJoin.Round) }
    val stroke3_5dp = remember(density) { Stroke(width = 3.5f * density) }
    val stroke4_5dp = remember(density) { Stroke(width = 4.5f * density, cap = StrokeCap.Round, join = StrokeJoin.Round) }

    val baseDomeGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF262A32),
                Color(0xFF111419),
                Color(0xFF07080A)
            ),
            center = Offset(0.50f, 0.45f),
            radius = 180f
        )
    }
    val pressedDomeGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF1B1F26),
                Color(0xFF0B0D11),
                Color(0xFF030405)
            ),
            center = Offset(0.50f, 0.55f),
            radius = 180f
        )
    }

    val bloomColors = remember(themeColor) {
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
                    val r = size.minDimension * 0.75f
                    val bloomAlpha = rgbBloomAlphaState.value
                    bloomColors[0] = themeColor.copy(alpha = bloomAlpha * 0.55f)
                    bloomColors[1] = themeColor.copy(alpha = bloomAlpha * 0.18f)
                    bloomColors[2] = Color.Transparent
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = bloomColors,
                            center = center,
                            radius = r
                        ),
                        radius = r,
                        center = center
                    )
                }
            }
            .shadow(
                elevation = if (isPressed) 2.dp else 6.dp,
                shape = CircleShape,
                ambientColor = if (isRgbEnabled) themeColor else Color.Black,
                spotColor = if (isRgbEnabled) themeColor else Color.Black
            )
            .graphicsLayer {
                scaleX = scaleAnimState.value
                scaleY = scaleAnimState.value
                translationY = pressOffsetYAnimState.value * density
            }
            .clip(CircleShape)
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
        Canvas(modifier = Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f
            val nodeCenter = center

            // 1. Obsidian Dome Base
            drawCircle(
                brush = if (isPressed) pressedDomeGradient else baseDomeGradient,
                radius = r,
                center = nodeCenter
            )

            // 2. Inset outline
            drawCircle(
                color = Color.Black.copy(alpha = 0.60f),
                radius = r - 0.5.dp.toPx(),
                center = nodeCenter,
                style = stroke1dp
            )

            // 3. Glowing Neon Ring
            val ringRadius = r - 3.dp.toPx()
            drawCircle(
                color = themeColor.copy(alpha = if (isPressed) 0.65f else 0.22f),
                radius = ringRadius,
                center = nodeCenter,
                style = stroke3_5dp
            )
            drawCircle(
                color = themeColor.copy(alpha = if (isPressed) 1.0f else 0.65f),
                radius = ringRadius,
                center = nodeCenter,
                style = stroke1_8dp
            )

            // 4. Specular Acrylic Lens Crescent
            val lensInset = 2.dp.toPx()
            drawArc(
                color = Color.White.copy(alpha = if (isPressed) 0.25f else 0.14f),
                startAngle = 190f,
                sweepAngle = 160f,
                useCenter = false,
                topLeft = Offset(nodeCenter.x - r + lensInset, nodeCenter.y - r + lensInset),
                size = Size((r - lensInset) * 2f, (r - lensInset) * 2f),
                style = stroke1_5dp
            )

            // 5. Directional Laser Chevron
            val chW = 10f * density
            val chH = 6.5f * density
            val K = com.sanket.tools.nexpad.model.NexpadKeys

            chevronPath.reset()
            when (direction.uppercase()) {
                K.UP -> {
                    chevronPath.moveTo(nodeCenter.x - chW, nodeCenter.y + chH * 0.4f)
                    chevronPath.lineTo(nodeCenter.x, nodeCenter.y - chH * 0.7f)
                    chevronPath.lineTo(nodeCenter.x + chW, nodeCenter.y + chH * 0.4f)
                }
                K.DOWN -> {
                    chevronPath.moveTo(nodeCenter.x - chW, nodeCenter.y - chH * 0.4f)
                    chevronPath.lineTo(nodeCenter.x, nodeCenter.y + chH * 0.7f)
                    chevronPath.lineTo(nodeCenter.x + chW, nodeCenter.y - chH * 0.4f)
                }
                K.LEFT -> {
                    chevronPath.moveTo(nodeCenter.x + chH * 0.4f, nodeCenter.y - chW)
                    chevronPath.lineTo(nodeCenter.x - chH * 0.7f, nodeCenter.y)
                    chevronPath.lineTo(nodeCenter.x + chH * 0.4f, nodeCenter.y + chW)
                }
                K.RIGHT -> {
                    chevronPath.moveTo(nodeCenter.x - chH * 0.4f, nodeCenter.y - chW)
                    chevronPath.lineTo(nodeCenter.x + chH * 0.7f, nodeCenter.y)
                    chevronPath.lineTo(nodeCenter.x - chH * 0.4f, nodeCenter.y + chW)
                }
                else -> {
                    chevronPath.moveTo(nodeCenter.x - chW, nodeCenter.y + chH * 0.4f)
                    chevronPath.lineTo(nodeCenter.x, nodeCenter.y - chH * 0.7f)
                    chevronPath.lineTo(nodeCenter.x + chW, nodeCenter.y + chH * 0.4f)
                }
            }

            drawPath(chevronPath, color = Color.Black.copy(alpha = 0.75f), style = stroke2_6dp)
            if (isPressed) {
                drawPath(chevronPath, color = themeColor.copy(alpha = 0.75f), style = stroke4_5dp)
            }
            drawPath(chevronPath, color = if (isPressed) Color.White else themeColor, style = stroke2_6dp)
        }
    }
}
