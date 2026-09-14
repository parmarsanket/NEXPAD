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
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel
import kotlin.math.atan2
import kotlin.math.hypot

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
 * Supports continuous sliding across arms with diagonal roll and haptic feedback.
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

    val gradient = Brush.linearGradient(
        colors = listOf(Color(0xFF2C2C2C), Color(0xFF141414)),
        start = Offset(0f, 0f),
        end = Offset(200f, 200f)
    )

    val activeColor = if (isRgbEnabled) Color(0xFF00F0FF) else Color(0xFF4ADE80)
    val shadow = if (isRgbEnabled) {
        Modifier.shadow(12.dp, crossShape, ambientColor = Color.Green, spotColor = Color.Yellow)
    } else {
        Modifier.shadow(8.dp, crossShape, ambientColor = Color.Black, spotColor = Color.Black)
    }

    Box(
        modifier = modifier
            .size(140.dp)
            .then(shadow)
            .clip(crossShape)
            .background(gradient)
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
                        val maxRadius = size.width * 0.65f

                        val newDirs = if (dist < deadzone || dist > maxRadius) {
                            emptySet()
                        } else {
                            val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble()))
                            val dirs = mutableSetOf<String>()
                            if (angle in -157.5..-22.5) dirs.add("UP")
                            if (angle in 22.5..157.5) dirs.add("DOWN")
                            if (angle in -67.5..67.5) dirs.add("RIGHT")
                            if (angle < -112.5 || angle > 112.5) dirs.add("LEFT")
                            dirs
                        }

                        if (newDirs != activeDirs) {
                            val added = newDirs - activeDirs
                            val removed = activeDirs - newDirs
                            removed.forEach { dir -> viewModel.updateButton(dir, false) }
                            added.forEach { dir -> viewModel.updateButton(dir, true) }
                            if (added.isNotEmpty()) onVibrate()
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
                        activeDirs.forEach { dir -> viewModel.updateButton(dir, false) }
                        pressedDirs = emptySet()
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val armW = w / 3f
            val armH = h / 3f

            // 1. Draw Active Arm Highlights
            if (pressedDirs.contains("UP")) {
                drawRect(
                    color = activeColor.copy(alpha = 0.40f),
                    topLeft = Offset(armW, 0f),
                    size = Size(armW, armH)
                )
            }
            if (pressedDirs.contains("DOWN")) {
                drawRect(
                    color = activeColor.copy(alpha = 0.40f),
                    topLeft = Offset(armW, armH * 2f),
                    size = Size(armW, armH)
                )
            }
            if (pressedDirs.contains("LEFT")) {
                drawRect(
                    color = activeColor.copy(alpha = 0.40f),
                    topLeft = Offset(0f, armH),
                    size = Size(armW, armH)
                )
            }
            if (pressedDirs.contains("RIGHT")) {
                drawRect(
                    color = activeColor.copy(alpha = 0.40f),
                    topLeft = Offset(armW * 2f, armH),
                    size = Size(armW, armH)
                )
            }

            // 2. Draw cross indentations / bevel grooves
            drawLine(
                color = Color.Black.copy(alpha = 0.5f),
                start = Offset(w / 2f, 0f),
                end = Offset(w / 2f, h),
                strokeWidth = 3f
            )
            drawLine(
                color = Color.Black.copy(alpha = 0.5f),
                start = Offset(0f, h / 2f),
                end = Offset(w, h / 2f),
                strokeWidth = 3f
            )

            // 3. Directional Triangle Arrows (▲, ▼, ◀, ▶)
            val normalArrowColor = Color.White.copy(alpha = 0.70f)
            val pressedArrowColor = Color.White
            val arrowSize = 6.dp.toPx()

            // UP Arrow
            val upPath = Path().apply {
                moveTo(w / 2f, armH * 0.45f - arrowSize)
                lineTo(w / 2f + arrowSize, armH * 0.45f + arrowSize)
                lineTo(w / 2f - arrowSize, armH * 0.45f + arrowSize)
                close()
            }
            drawPath(
                path = upPath,
                color = if (pressedDirs.contains("UP")) pressedArrowColor else normalArrowColor
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
                color = if (pressedDirs.contains("DOWN")) pressedArrowColor else normalArrowColor
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
                color = if (pressedDirs.contains("LEFT")) pressedArrowColor else normalArrowColor
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
                color = if (pressedDirs.contains("RIGHT")) pressedArrowColor else normalArrowColor
            )

            // 4. Center Pivot Depression Disc
            drawCircle(
                color = Color.Black.copy(alpha = 0.35f),
                radius = armW * 0.28f,
                center = Offset(w / 2f, h / 2f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.10f),
                radius = armW * 0.28f,
                center = Offset(w / 2f, h / 2f),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }
    }
}

/**
 * Dedicated Tactile D-Pad Directional Button for individual UP, DOWN, LEFT, RIGHT layout controls.
 * Features beveled mechanical cap, directional arrow symbol, RGB rim, and spring depression.
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
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 800f),
        label = "dpad_btn_scale"
    )

    val dirSymbol = when (direction.uppercase()) {
        "UP" -> "▲"
        "DOWN" -> "▼"
        "LEFT" -> "◀"
        "RIGHT" -> "▶"
        else -> direction
    }

    val themeColor = if (isRgbEnabled) Color(0xFF00F0FF) else Color(0xFF4ADE80)
    val borderColor = if (isPressed) Color.White else themeColor.copy(alpha = 0.85f)
    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .size(72.dp)
            .graphicsLayer {
                scaleX = scaleAnim
                scaleY = scaleAnim
            }
            .shadow(
                elevation = if (isPressed) 2.dp else if (isRgbEnabled) 10.dp else 6.dp,
                shape = shape,
                ambientColor = if (isRgbEnabled) themeColor else Color.Black,
                spotColor = if (isRgbEnabled) themeColor else Color.Black
            )
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = if (isPressed) {
                        listOf(themeColor.copy(alpha = 0.35f), Color(0xFF1E293B))
                    } else {
                        listOf(Color(0xFF262626), Color(0xFF141414))
                    }
                )
            )
            .border(2.dp, borderColor, shape)
            .pointerInput(isConnected, direction) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        onVibrate()
                        viewModel.updateButton(direction, true)
                        tryAwaitRelease()
                        isPressed = false
                        viewModel.updateButton(direction, false)
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
                color = if (isPressed) Color.White else themeColor
            )
            Text(
                text = direction.uppercase(),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (isPressed) Color.White.copy(alpha = 0.9f) else Color.LightGray.copy(alpha = 0.6f)
            )
        }
    }
}
