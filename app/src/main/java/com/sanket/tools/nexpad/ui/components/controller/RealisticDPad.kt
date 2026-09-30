package com.sanket.tools.nexpad.ui.components.controller

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt

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
 * High-performance, tactile 4-way / 8-way mechanical D-Pad cross.
 * Features 3D physical pivot tilt kinematics, recessed chassis socket,
 * central concave thumb rest cup, directional LED backlighting, and crisp haptics.
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

    // 3D Rocker Tilt Kinematics
    val targetTiltX = remember(pressedDirs) {
        var tilt = 0f
        if (pressedDirs.contains(K.UP)) tilt -= 6.5f
        if (pressedDirs.contains(K.DOWN)) tilt += 6.5f
        tilt
    }
    val targetTiltY = remember(pressedDirs) {
        var tilt = 0f
        if (pressedDirs.contains(K.LEFT)) tilt -= 6.5f
        if (pressedDirs.contains(K.RIGHT)) tilt += 6.5f
        tilt
    }
    val targetScale = if (pressedDirs.isNotEmpty()) 0.96f else 1.0f

    val tiltXAnim by animateFloatAsState(
        targetValue = targetTiltX,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 750f),
        label = "dpad_tilt_x"
    )
    val tiltYAnim by animateFloatAsState(
        targetValue = targetTiltY,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 750f),
        label = "dpad_tilt_y"
    )
    val scaleAnim by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 750f),
        label = "dpad_scale"
    )

    val neonColor = if (isRgbEnabled) Color(0xFF00E5FF) else Color(0xFF4ADE80)

    val surfaceGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF333E4D),
                Color(0xFF1E252F),
                Color(0xFF11151C)
            ),
            center = Offset(0.45f, 0.45f),
            radius = 220f
        )
    }

    Box(
        modifier = modifier
            .size(140.dp)
            // Outer recessed socket bezel in the controller shell
            .drawBehind {
                val center = Offset(size.width / 2f, size.height / 2f)
                val socketRadius = size.minDimension / 2f + 4.dp.toPx()

                // Ambient underglow bloom if RGB enabled
                if (isRgbEnabled) {
                    drawCircle(
                        color = neonColor.copy(alpha = if (pressedDirs.isNotEmpty()) 0.35f else 0.15f),
                        radius = socketRadius + 6.dp.toPx(),
                        center = center
                    )
                }

                // Recessed circular chassis socket
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF0D1017), Color(0xFF06080B)),
                        center = center,
                        radius = socketRadius
                    ),
                    radius = socketRadius,
                    center = center
                )

                // Outer metallic bevel rim
                drawCircle(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF4A5568).copy(alpha = 0.6f), Color(0xFF1A202C).copy(alpha = 0.8f))
                    ),
                    radius = socketRadius,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
            .graphicsLayer {
                rotationX = tiltXAnim
                rotationY = tiltYAnim
                scaleX = scaleAnim
                scaleY = scaleAnim
            }
            .shadow(
                elevation = if (pressedDirs.isNotEmpty()) 4.dp else 10.dp,
                shape = crossShape,
                ambientColor = if (isRgbEnabled) neonColor else Color.Black,
                spotColor = if (isRgbEnabled) neonColor else Color.Black
            )
            .clip(crossShape)
            .background(surfaceGradient)
            .border(
                width = 1.5.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF718096).copy(alpha = 0.65f),
                        Color(0xFF2D3748).copy(alpha = 0.40f),
                        Color(0xFF1A202C)
                    )
                ),
                shape = crossShape
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
                        val deadzone = size.width * 0.12f
                        val maxRadius = size.width * 0.68f

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
            val armW = w / 3f
            val armH = h / 3f

            // 1. Draw Active Arm LED Illumination
            if (pressedDirs.contains(K.UP)) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(neonColor.copy(alpha = 0.55f), Color.Transparent),
                        startY = 0f,
                        endY = armH * 1.2f
                    ),
                    topLeft = Offset(armW, 0f),
                    size = Size(armW, armH)
                )
            }
            if (pressedDirs.contains(K.DOWN)) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, neonColor.copy(alpha = 0.55f)),
                        startY = armH * 1.8f,
                        endY = h
                    ),
                    topLeft = Offset(armW, armH * 2f),
                    size = Size(armW, armH)
                )
            }
            if (pressedDirs.contains(K.LEFT)) {
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(neonColor.copy(alpha = 0.55f), Color.Transparent),
                        startX = 0f,
                        endX = armW * 1.2f
                    ),
                    topLeft = Offset(0f, armH),
                    size = Size(armW, armH)
                )
            }
            if (pressedDirs.contains(K.RIGHT)) {
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, neonColor.copy(alpha = 0.55f)),
                        startX = armW * 1.8f,
                        endX = w
                    ),
                    topLeft = Offset(armW * 2f, armH),
                    size = Size(armW, armH)
                )
            }

            // 2. Beveled cross arm seam lines
            val seamColor = Color.Black.copy(alpha = 0.55f)
            val highlightColor = Color.White.copy(alpha = 0.12f)

            drawLine(color = seamColor, start = Offset(w / 2f, 2f), end = Offset(w / 2f, h - 2f), strokeWidth = 2.5f)
            drawLine(color = highlightColor, start = Offset(w / 2f + 1f, 2f), end = Offset(w / 2f + 1f, h - 2f), strokeWidth = 1f)

            drawLine(color = seamColor, start = Offset(2f, h / 2f), end = Offset(w - 2f, h / 2f), strokeWidth = 2.5f)
            drawLine(color = highlightColor, start = Offset(2f, h / 2f + 1f), end = Offset(w - 2f, h / 2f + 1f), strokeWidth = 1f)

            // 3. Directional Chevrons (▲, ▼, ◀, ▶)
            val normalArrowColor = Color.White.copy(alpha = 0.65f)
            val pressedArrowColor = Color.White
            val arrowSize = 6.5.dp.toPx()

            // UP Arrow
            val upPath = Path().apply {
                moveTo(w / 2f, armH * 0.45f - arrowSize)
                lineTo(w / 2f + arrowSize, armH * 0.45f + arrowSize)
                lineTo(w / 2f - arrowSize, armH * 0.45f + arrowSize)
                close()
            }
            drawPath(
                path = upPath,
                color = if (pressedDirs.contains(K.UP)) pressedArrowColor else normalArrowColor
            )

            // DOWN Arrow
            val downPath = Path().apply {
                moveTo(w / 2f, h - armH * 0.45f + arrowSize)
                lineTo(w / 2f + arrowSize, h - armH * 0.45f - arrowSize)
                lineTo(w / 2f - arrowSize, h - armH * 0.45f - arrowSize)
                close()
            }
            drawPath(
                path = downPath,
                color = if (pressedDirs.contains(K.DOWN)) pressedArrowColor else normalArrowColor
            )

            // LEFT Arrow
            val leftPath = Path().apply {
                moveTo(armW * 0.45f - arrowSize, h / 2f)
                lineTo(armW * 0.45f + arrowSize, h / 2f - arrowSize)
                lineTo(armW * 0.45f + arrowSize, h / 2f + arrowSize)
                close()
            }
            drawPath(
                path = leftPath,
                color = if (pressedDirs.contains(K.LEFT)) pressedArrowColor else normalArrowColor
            )

            // RIGHT Arrow
            val rightPath = Path().apply {
                moveTo(w - armW * 0.45f + arrowSize, h / 2f)
                lineTo(w - armW * 0.45f - arrowSize, h / 2f - arrowSize)
                lineTo(w - armW * 0.45f - arrowSize, h / 2f + arrowSize)
                close()
            }
            drawPath(
                path = rightPath,
                color = if (pressedDirs.contains(K.RIGHT)) pressedArrowColor else normalArrowColor
            )

            // 4. Center Concave Thumb Rest Cup (Spherical depression)
            val centerRadius = armW * 0.36f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF090B0E), Color(0xFF1E252F)),
                    center = Offset(w / 2f, h / 2f),
                    radius = centerRadius
                ),
                radius = centerRadius,
                center = Offset(w / 2f, h / 2f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.15f),
                radius = centerRadius,
                center = Offset(w / 2f, h / 2f),
                style = Stroke(width = 1.2.dp.toPx())
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

    val scaleAnim by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 750f),
        label = "dpad_btn_scale"
    )
    val pressOffsetYAnim by animateFloatAsState(
        targetValue = if (isPressed) 3.dp.value else 0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 750f),
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

    val themeColor = if (isRgbEnabled) Color(0xFF00F0FF) else Color(0xFF4ADE80)
    val shape = RoundedCornerShape(18.dp)

    val currentOnVibrate by rememberUpdatedState(onVibrate)
    val currentViewModel by rememberUpdatedState(viewModel)

    Box(
        modifier = modifier
            .size(72.dp)
            .graphicsLayer {
                scaleX = scaleAnim
                scaleY = scaleAnim
            }
            .offset { IntOffset(0, pressOffsetYAnim.dp.roundToPx()) }
            .shadow(
                elevation = if (isPressed) 3.dp else if (isRgbEnabled) 10.dp else 6.dp,
                shape = shape,
                ambientColor = if (isRgbEnabled) themeColor else Color.Black,
                spotColor = if (isRgbEnabled) themeColor else Color.Black
            )
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
