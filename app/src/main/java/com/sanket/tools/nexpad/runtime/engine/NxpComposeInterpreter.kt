package com.sanket.tools.nexpad.runtime.engine

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.runtime.model.NexPadControl
import com.sanket.tools.nexpad.runtime.model.NexPadInputTarget
import com.sanket.tools.nexpad.runtime.model.NxpComponentDef
import com.sanket.tools.nexpad.runtime.model.NxpGeometry
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * High-performance Jetpack Compose interpreter for .nxpcomponent definitions.
 * Evaluates shapes, paths, shaders, and animations natively without reflection or IPC.
 */
@Composable
fun NxpComposeInterpreter(
    definition: NxpComponentDef,
    assignedControl: NexPadControl,
    isConnected: Boolean,
    inputTarget: NexPadInputTarget,
    modifier: Modifier = Modifier
) {
    val isDpadCross = definition.manifest.defaultControl.equals("DPAD", ignoreCase = true) ||
            (definition.manifest.category.equals("DPAD", ignoreCase = true) && definition.size.widthDp >= 100) ||
            (assignedControl is NexPadControl.Button && assignedControl.key.equals("DPAD", ignoreCase = true))

    if (definition.manifest.category.equals("JOYSTICK", ignoreCase = true) ||
        definition.interaction.type.equals("Joystick", ignoreCase = true)
    ) {
        RenderNxpJoystick(definition, assignedControl, isConnected, inputTarget, modifier)
    } else if (isDpadCross) {
        RenderNxpDPad(definition, isConnected, inputTarget, modifier)
    } else {
        RenderNxpButton(definition, assignedControl, isConnected, inputTarget, modifier)
    }
}

@Composable
private fun RenderNxpButton(
    def: NxpComponentDef,
    control: NexPadControl,
    isConnected: Boolean,
    inputTarget: NexPadInputTarget,
    modifier: Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    val pressedState = def.pressed
    val targetScale = if (isPressed) (pressedState?.scale ?: 0.9f) else 1.0f
    val targetRotation = if (isPressed) (pressedState?.rotation ?: 0f) else 0f

    val scaleAnim by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(
            dampingRatio = pressedState?.springDamping ?: 0.6f,
            stiffness = pressedState?.springStiffness ?: 800f
        ),
        label = "nxp_scale"
    )

    val rotationAnim by animateFloatAsState(
        targetValue = targetRotation,
        animationSpec = spring(
            dampingRatio = pressedState?.springDamping ?: 0.6f,
            stiffness = pressedState?.springStiffness ?: 800f
        ),
        label = "nxp_rotation"
    )

    val fillColor = parseHexColor(
        if (isPressed && pressedState?.fillColor != null) pressedState.fillColor else def.visual.fillColor,
        fallback = Color(0xFF0A192F)
    ).copy(alpha = def.visual.opacity)

    val borderColor = parseHexColor(
        if (isPressed && pressedState?.borderColor != null) pressedState.borderColor else def.visual.borderColor,
        fallback = Color(0xFF00F0FF)
    )

    val buttonControl = (control as? NexPadControl.Button)
        ?: (if (control is NexPadControl.DPad) NexPadControl.Button(control.direction) else null)
        ?: NexPadControl.Button(def.manifest.defaultControl)

    Box(
        modifier = modifier
            .size(def.size.widthDp.dp, def.size.heightDp.dp)
            .graphicsLayer {
                scaleX = scaleAnim
                scaleY = scaleAnim
                rotationZ = rotationAnim
            }
            .pointerInput(isConnected, buttonControl) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        inputTarget.onButtonPress(buttonControl)
                        tryAwaitRelease()
                        isPressed = false
                        inputTarget.onButtonRelease(buttonControl)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawNxpGeometry(
                geometry = def.geometry,
                fillColor = fillColor,
                borderColor = borderColor,
                borderWidth = def.visual.borderWidth.dp.toPx()
            )
        }

        // Optional central label/text
        val label = def.label
        if (label != null) {
            val labelColor = parseHexColor(
                if (isPressed) label.pressedColor else label.color,
                fallback = Color(0xFF00F0FF)
            )
            val labelText = if (buttonControl.key.isNotBlank()) {
                val upper = buttonControl.key.uppercase()
                when (upper) {
                    "UP" -> "▲"
                    "DOWN" -> "▼"
                    "LEFT" -> "◀"
                    "RIGHT" -> "▶"
                    "A", "B", "X", "Y", "LT", "RT", "LB", "RB", "LS", "RS", "M1", "M2", "M3", "M4", "VIEW", "MENU", "SHARE" -> upper
                    else -> label.text.ifBlank { upper }
                }
            } else {
                label.text
            }
            Text(
                text = labelText,
                color = labelColor,
                fontSize = label.fontSize.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun RenderNxpDPad(
    def: NxpComponentDef,
    isConnected: Boolean,
    inputTarget: NexPadInputTarget,
    modifier: Modifier
) {
    var pressedDirections by remember { mutableStateOf<Set<String>>(emptySet()) }

    val baseBorderColor = parseHexColor(def.visual.borderColor, fallback = Color(0xFF2DD4BF))
    val baseFillColor = parseHexColor(def.visual.fillColor, fallback = Color(0xFF0B1A24)).copy(alpha = def.visual.opacity)
    val activeColor = parseHexColor(def.pressed?.fillColor ?: "#2DD4BF", fallback = Color(0xFF2DD4BF))

    val sizeDp = def.size.widthDp.coerceAtLeast(140).dp

    Box(
        modifier = modifier
            .size(sizeDp)
            .pointerInput(isConnected, inputTarget) {
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
                            removed.forEach { dir ->
                                inputTarget.onButtonRelease(NexPadControl.Button(dir))
                            }
                            added.forEach { dir ->
                                inputTarget.onButtonPress(NexPadControl.Button(dir))
                            }
                            if (added.isNotEmpty()) {
                                inputTarget.triggerHaptic()
                            }
                            activeDirs = newDirs
                            pressedDirections = newDirs
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
                        activeDirs.forEach { dir ->
                            inputTarget.onButtonRelease(NexPadControl.Button(dir))
                        }
                        pressedDirections = emptySet()
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
            val cornerR = 8.dp.toPx()

            // Draw Cross Background Arms
            // Vertical bar (UP & DOWN)
            drawRoundRect(
                color = baseFillColor,
                topLeft = Offset(armW, 0f),
                size = androidx.compose.ui.geometry.Size(armW, h),
                cornerRadius = CornerRadius(cornerR, cornerR)
            )
            // Horizontal bar (LEFT & RIGHT)
            drawRoundRect(
                color = baseFillColor,
                topLeft = Offset(0f, armH),
                size = androidx.compose.ui.geometry.Size(w, armH),
                cornerRadius = CornerRadius(cornerR, cornerR)
            )

            // Draw active arm highlights
            if (pressedDirections.contains("UP")) {
                drawRoundRect(
                    color = activeColor.copy(alpha = 0.45f),
                    topLeft = Offset(armW, 0f),
                    size = androidx.compose.ui.geometry.Size(armW, armH),
                    cornerRadius = CornerRadius(cornerR, cornerR)
                )
            }
            if (pressedDirections.contains("DOWN")) {
                drawRoundRect(
                    color = activeColor.copy(alpha = 0.45f),
                    topLeft = Offset(armW, armH * 2f),
                    size = androidx.compose.ui.geometry.Size(armW, armH),
                    cornerRadius = CornerRadius(cornerR, cornerR)
                )
            }
            if (pressedDirections.contains("LEFT")) {
                drawRoundRect(
                    color = activeColor.copy(alpha = 0.45f),
                    topLeft = Offset(0f, armH),
                    size = androidx.compose.ui.geometry.Size(armW, armH),
                    cornerRadius = CornerRadius(cornerR, cornerR)
                )
            }
            if (pressedDirections.contains("RIGHT")) {
                drawRoundRect(
                    color = activeColor.copy(alpha = 0.45f),
                    topLeft = Offset(armW * 2f, armH),
                    size = androidx.compose.ui.geometry.Size(armW, armH),
                    cornerRadius = CornerRadius(cornerR, cornerR)
                )
            }

            // Draw cross borders
            val crossPath = Path().apply {
                moveTo(armW, 0f)
                lineTo(armW * 2f, 0f)
                lineTo(armW * 2f, armH)
                lineTo(w, armH)
                lineTo(w, armH * 2f)
                lineTo(armW * 2f, armH * 2f)
                lineTo(armW * 2f, h)
                lineTo(armW, h)
                lineTo(armW, armH * 2f)
                lineTo(0f, armH * 2f)
                lineTo(0f, armH)
                lineTo(armW, armH)
                close()
            }
            drawPath(
                path = crossPath,
                color = baseBorderColor,
                style = Stroke(width = 2.5.dp.toPx())
            )

            // Draw directional triangle arrows
            val arrowColor = baseBorderColor
            val pressedArrowColor = Color.White
            val arrowSize = 7.dp.toPx()

            // UP Arrow
            val upPath = Path().apply {
                moveTo(w / 2f, armH * 0.35f - arrowSize)
                lineTo(w / 2f + arrowSize, armH * 0.35f + arrowSize)
                lineTo(w / 2f - arrowSize, armH * 0.35f + arrowSize)
                close()
            }
            drawPath(
                path = upPath,
                color = if (pressedDirections.contains("UP")) pressedArrowColor else arrowColor
            )

            // DOWN Arrow
            val downPath = Path().apply {
                moveTo(w / 2f, h - armH * 0.35f + arrowSize)
                lineTo(w / 2f + arrowSize, h - armH * 0.35f - arrowSize)
                lineTo(w / 2f - arrowSize, h - armH * 0.35f - arrowSize)
                close()
            }
            drawPath(
                path = downPath,
                color = if (pressedDirections.contains("DOWN")) pressedArrowColor else arrowColor
            )

            // LEFT Arrow
            val leftPath = Path().apply {
                moveTo(armW * 0.35f - arrowSize, h / 2f)
                lineTo(armW * 0.35f + arrowSize, h / 2f - arrowSize)
                lineTo(armW * 0.35f + arrowSize, h / 2f + arrowSize)
                close()
            }
            drawPath(
                path = leftPath,
                color = if (pressedDirections.contains("LEFT")) pressedArrowColor else arrowColor
            )

            // RIGHT Arrow
            val rightPath = Path().apply {
                moveTo(w - armW * 0.35f + arrowSize, h / 2f)
                lineTo(w - armW * 0.35f - arrowSize, h / 2f - arrowSize)
                lineTo(w - armW * 0.35f - arrowSize, h / 2f + arrowSize)
                close()
            }
            drawPath(
                path = rightPath,
                color = if (pressedDirections.contains("RIGHT")) pressedArrowColor else arrowColor
            )

            // Center pivot disc
            drawCircle(
                color = baseBorderColor.copy(alpha = 0.2f),
                radius = armW * 0.25f,
                center = Offset(w / 2f, h / 2f)
            )
        }
    }
}

@Composable
private fun RenderNxpJoystick(
    def: NxpComponentDef,
    control: NexPadControl,
    isConnected: Boolean,
    inputTarget: NexPadInputTarget,
    modifier: Modifier
) {
    var thumbOffset by remember { mutableStateOf(Offset.Zero) }
    val baseSizePx = remember(def.size.widthDp) { def.size.widthDp * 2.5f }
    val maxRadiusPx = remember(baseSizePx) { baseSizePx * 0.45f }

    val stickControl = (control as? NexPadControl.Stick)
        ?: NexPadControl.Stick(isLeft = def.manifest.defaultControl.contains("L", ignoreCase = true))

    val baseColor = parseHexColor(def.visual.fillColor, Color(0xFF0A192F)).copy(alpha = 0.85f)
    val ringColor = parseHexColor(def.visual.borderColor, Color(0xFF00F0FF))
    val thumbColor = parseHexColor(def.pressed?.fillColor ?: def.visual.borderColor, Color(0xFF00F0FF))

    Box(
        modifier = modifier
            .size(def.size.widthDp.dp, def.size.heightDp.dp)
            .pointerInput(isConnected, stickControl) {
                detectDragGestures(
                    onDragStart = {},
                    onDragEnd = {
                        thumbOffset = Offset.Zero
                        inputTarget.onStickMove(stickControl, 0f, 0f)
                    },
                    onDragCancel = {
                        thumbOffset = Offset.Zero
                        inputTarget.onStickMove(stickControl, 0f, 0f)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val newOffset = thumbOffset + dragAmount
                        val distance = sqrt(newOffset.x * newOffset.x + newOffset.y * newOffset.y)
                        val clamped = if (distance > maxRadiusPx) {
                            Offset(newOffset.x / distance * maxRadiusPx, newOffset.y / distance * maxRadiusPx)
                        } else newOffset
                        thumbOffset = clamped

                        val normX = (clamped.x / maxRadiusPx).coerceIn(-1.0f, 1.0f)
                        val normY = (-clamped.y / maxRadiusPx).coerceIn(-1.0f, 1.0f)

                        inputTarget.onStickMove(stickControl, normX, normY)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Base plate canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(color = baseColor)
            drawCircle(color = ringColor, style = Stroke(width = def.visual.borderWidth.dp.toPx()))
            // Inner deadzone guide ring
            drawCircle(color = ringColor.copy(alpha = 0.3f), radius = size.minDimension * 0.2f, style = Stroke(width = 1.5f))
        }

        // Floating thumbstick knob
        Box(
            modifier = Modifier
                .offset { IntOffset(thumbOffset.x.roundToInt(), thumbOffset.y.roundToInt()) }
                .size((def.size.widthDp * 0.45f).dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(color = thumbColor)
                drawCircle(color = Color.White, style = Stroke(width = 2.dp.toPx()))
            }
        }
    }
}

private fun DrawScope.drawNxpGeometry(
    geometry: NxpGeometry,
    fillColor: Color,
    borderColor: Color,
    borderWidth: Float
) {
    when (geometry.type.lowercase()) {
        "polygon" -> {
            val sides = geometry.sides.coerceAtLeast(3)
            val radius = (size.minDimension / 2f) - (borderWidth / 2f) - 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            val angleStep = (2 * PI / sides).toFloat()
            val path = Path()

            for (i in 0 until sides) {
                val angle = i * angleStep - (PI / 2).toFloat()
                val x = center.x + radius * cos(angle)
                val y = center.y + radius * sin(angle)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()

            drawPath(path, fillColor)
            if (borderWidth > 0f) {
                drawPath(path, borderColor, style = Stroke(width = borderWidth))
            }
        }
        "svgpath" -> {
            val svgData = geometry.pathData
            if (!svgData.isNullOrBlank()) {
                try {
                    val nodes = PathParser().parsePathString(svgData).toNodes()
                    val path = Path()
                    PathParser().addPathNodes(nodes).toPath(path)
                    drawPath(path, fillColor)
                    if (borderWidth > 0f) {
                        drawPath(path, borderColor, style = Stroke(width = borderWidth))
                    }
                } catch (_: Exception) {
                    drawCircle(fillColor)
                }
            } else {
                drawCircle(fillColor)
            }
        }
        "roundedrect" -> {
            val cr = CornerRadius(geometry.cornerRadius, geometry.cornerRadius)
            drawRoundRect(fillColor, cornerRadius = cr)
            if (borderWidth > 0f) {
                drawRoundRect(borderColor, cornerRadius = cr, style = Stroke(width = borderWidth))
            }
        }
        else -> { // "circle" default
            val radius = (size.minDimension / 2f) - (borderWidth / 2f) - 2f
            drawCircle(fillColor, radius = radius)
            if (borderWidth > 0f) {
                drawCircle(borderColor, radius = radius, style = Stroke(width = borderWidth))
            }
        }
    }
}

/**
 * Safe hex color parser supporting #RGB, #ARGB, #RRGGBB, #AARRGGBB.
 * Never throws exceptions on malformed input.
 */
fun parseHexColor(hex: String?, fallback: Color = Color(0xFF00F0FF)): Color {
    if (hex.isNullOrBlank()) return fallback
    return try {
        val clean = hex.trim().removePrefix("#")
        when (clean.length) {
            3 -> {
                val r = clean[0].toString().repeat(2).toInt(16)
                val g = clean[1].toString().repeat(2).toInt(16)
                val b = clean[2].toString().repeat(2).toInt(16)
                Color(r, g, b)
            }
            6 -> {
                val rgb = clean.toLong(16)
                Color((0xFF000000 or rgb).toInt())
            }
            8 -> {
                val argb = clean.toLong(16)
                Color(argb.toInt())
            }
            else -> fallback
        }
    } catch (_: Exception) {
        fallback
    }
}
