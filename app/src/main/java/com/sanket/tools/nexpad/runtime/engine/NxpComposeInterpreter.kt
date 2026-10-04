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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.model.NexpadKeys
import com.sanket.tools.nexpad.runtime.model.NexPadControl
import com.sanket.tools.nexpad.runtime.model.NexPadInputTarget
import com.sanket.tools.nexpad.runtime.model.NxpComponentDef
import com.sanket.tools.nexpad.runtime.model.NxpGeometry
import com.sanket.tools.nexpad.ui.components.controller.calculateGamingStickMagnitude
import com.sanket.tools.nexpad.ui.components.controller.VelocityRingBuffer
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import com.sanket.tools.nexpad.category.CategoryManager
import com.sanket.tools.nexpad.category.ControllerLabelStyle

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
    modifier: Modifier = Modifier,
    isInteractive: Boolean = true,
    labelStyle: ControllerLabelStyle = ControllerLabelStyle.XBOX
) {
    val isDpadCross = definition.manifest.defaultControl.equals(NexpadKeys.DPAD, ignoreCase = true) ||
            (definition.manifest.category.equals(NexpadKeys.DPAD, ignoreCase = true) && definition.size.widthDp >= 100) ||
            (assignedControl is NexPadControl.Button && assignedControl.key.equals(NexpadKeys.DPAD, ignoreCase = true))

    if (definition.manifest.category.equals("JOYSTICK", ignoreCase = true) ||
        definition.interaction.type.equals("Joystick", ignoreCase = true)
    ) {
        RenderNxpJoystick(definition, assignedControl, isConnected, inputTarget, modifier, isInteractive)
    } else if (isDpadCross) {
        RenderNxpDPad(definition, isConnected, inputTarget, modifier, isInteractive)
    } else {
        RenderNxpButton(definition, assignedControl, isConnected, inputTarget, modifier, isInteractive, labelStyle)
    }
}

@Composable
private fun RenderNxpButton(
    def: NxpComponentDef,
    control: NexPadControl,
    isConnected: Boolean,
    inputTarget: NexPadInputTarget,
    modifier: Modifier,
    isInteractive: Boolean = true,
    labelStyle: ControllerLabelStyle = ControllerLabelStyle.XBOX
) {
    var isPressed by remember { mutableStateOf(false) }
    val currentInputTarget by rememberUpdatedState(inputTarget)

    val pressedState = def.pressed
    val targetScale = if (isPressed) (pressedState?.scale ?: 0.9f) else 1.0f
    val targetRotation = if (isPressed) (pressedState?.rotation ?: 0f) else 0f

    val scaleAnim = if (isInteractive) {
        animateFloatAsState(
            targetValue = targetScale,
            animationSpec = spring(
                dampingRatio = pressedState?.springDamping ?: 0.6f,
                stiffness = pressedState?.springStiffness ?: 800f
            ),
            label = "nxp_scale"
        ).value
    } else 1.0f

    val rotationAnim = if (isInteractive) {
        animateFloatAsState(
            targetValue = targetRotation,
            animationSpec = spring(
                dampingRatio = pressedState?.springDamping ?: 0.6f,
                stiffness = pressedState?.springStiffness ?: 800f
            ),
            label = "nxp_rotation"
        ).value
    } else 0f

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

    val gestureModifier = if (!isInteractive) {
        Modifier
    } else {
        Modifier.pointerInput(buttonControl) {
            detectTapGestures(
                onPress = {
                    isPressed = true
                    currentInputTarget.onButtonPress(buttonControl)
                    tryAwaitRelease()
                    isPressed = false
                    currentInputTarget.onButtonRelease(buttonControl)
                }
            )
        }
    }

    Box(
        modifier = modifier
            .size(def.size.widthDp.dp, def.size.heightDp.dp)
            .graphicsLayer {
                scaleX = scaleAnim
                scaleY = scaleAnim
                rotationZ = rotationAnim
            }
            .then(gestureModifier),
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
                    NexpadKeys.UP -> "▲"
                    NexpadKeys.DOWN -> "▼"
                    NexpadKeys.LEFT -> "◀"
                    NexpadKeys.RIGHT -> "▶"
                    NexpadKeys.A, NexpadKeys.B, NexpadKeys.X, NexpadKeys.Y,
                    NexpadKeys.LT, NexpadKeys.RT, NexpadKeys.LB, NexpadKeys.RB,
                    NexpadKeys.LSB, NexpadKeys.RSB,
                    NexpadKeys.LS, NexpadKeys.RS, NexpadKeys.M1, NexpadKeys.M2,
                    NexpadKeys.M3, NexpadKeys.M4, NexpadKeys.VIEW, NexpadKeys.MENU,
                    NexpadKeys.SHARE -> CategoryManager.getLabelForStyle(upper, labelStyle)
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
    modifier: Modifier,
    isInteractive: Boolean = true
) {
    var pressedDirections by remember { mutableStateOf<Set<String>>(emptySet()) }
    val currentInputTarget by rememberUpdatedState(inputTarget)

    val baseBorderColor = parseHexColor(def.visual.borderColor, fallback = Color(0xFF2DD4BF))
    val baseFillColor = parseHexColor(def.visual.fillColor, fallback = Color(0xFF0B1A24)).copy(alpha = def.visual.opacity)
    val activeColor = parseHexColor(def.pressed?.fillColor ?: "#2DD4BF", fallback = Color(0xFF2DD4BF))

    val sizeDp = def.size.widthDp.coerceAtLeast(140).dp

    val gestureModifier = if (!isInteractive) {
        Modifier
    } else {
        Modifier.pointerInput(Unit) {
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
                            if (angle in -157.5..-22.5) dirs.add(NexpadKeys.UP)
                            if (angle in 22.5..157.5) dirs.add(NexpadKeys.DOWN)
                            if (angle in -67.5..67.5) dirs.add(NexpadKeys.RIGHT)
                            if (angle < -112.5 || angle > 112.5) dirs.add(NexpadKeys.LEFT)
                            dirs
                        }

                        if (newDirs != activeDirs) {
                            val added = newDirs - activeDirs
                            val removed = activeDirs - newDirs
                            removed.forEach { dir ->
                                currentInputTarget.onButtonRelease(NexPadControl.Button(dir))
                            }
                            added.forEach { dir ->
                                currentInputTarget.onButtonPress(NexPadControl.Button(dir))
                            }
                            if (added.isNotEmpty()) {
                                currentInputTarget.triggerHaptic()
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
                            currentInputTarget.onButtonRelease(NexPadControl.Button(dir))
                        }
                        pressedDirections = emptySet()
                    }
                }
        }
    }

    Box(
        modifier = modifier
            .size(sizeDp)
            .then(gestureModifier),
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
            if (pressedDirections.contains(NexpadKeys.UP)) {
                drawRoundRect(
                    color = activeColor.copy(alpha = 0.45f),
                    topLeft = Offset(armW, 0f),
                    size = androidx.compose.ui.geometry.Size(armW, armH),
                    cornerRadius = CornerRadius(cornerR, cornerR)
                )
            }
            if (pressedDirections.contains(NexpadKeys.DOWN)) {
                drawRoundRect(
                    color = activeColor.copy(alpha = 0.45f),
                    topLeft = Offset(armW, armH * 2f),
                    size = androidx.compose.ui.geometry.Size(armW, armH),
                    cornerRadius = CornerRadius(cornerR, cornerR)
                )
            }
            if (pressedDirections.contains(NexpadKeys.LEFT)) {
                drawRoundRect(
                    color = activeColor.copy(alpha = 0.45f),
                    topLeft = Offset(0f, armH),
                    size = androidx.compose.ui.geometry.Size(armW, armH),
                    cornerRadius = CornerRadius(cornerR, cornerR)
                )
            }
            if (pressedDirections.contains(NexpadKeys.RIGHT)) {
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
                color = if (pressedDirections.contains(NexpadKeys.UP)) pressedArrowColor else arrowColor
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
                color = if (pressedDirections.contains(NexpadKeys.DOWN)) pressedArrowColor else arrowColor
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
                color = if (pressedDirections.contains(NexpadKeys.LEFT)) pressedArrowColor else arrowColor
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
                color = if (pressedDirections.contains(NexpadKeys.RIGHT)) pressedArrowColor else arrowColor
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
    modifier: Modifier,
    isInteractive: Boolean = true
) {
    var thumbOffset by remember { mutableStateOf(Offset.Zero) }
    val baseSizePx = remember(def.size.widthDp) { def.size.widthDp * 2.5f }
    val maxRadiusPx = remember(baseSizePx) { baseSizePx * 0.45f }

    val stickControl = (control as? NexPadControl.Stick)
        ?: NexPadControl.Stick(isLeft = def.manifest.defaultControl.contains("L", ignoreCase = true))

    val baseColor = parseHexColor(def.visual.fillColor, Color(0xFF0A192F)).copy(alpha = 0.85f)
    val ringColor = parseHexColor(def.visual.borderColor, Color(0xFF00F0FF))
    val thumbColor = parseHexColor(def.pressed?.fillColor ?: def.visual.borderColor, Color(0xFF00F0FF))

    val context = androidx.compose.ui.platform.LocalContext.current
    val (isCameraMode, cameraSensitivity) = remember(context, stickControl.isLeft, def.manifest) {
        val isTouchpad = def.manifest.category.equals("TOUCHPAD", ignoreCase = true) ||
                def.manifest.defaultControl.contains("RTP", ignoreCase = true) ||
                def.manifest.defaultControl.contains("LTP", ignoreCase = true)
        if (isTouchpad) {
            val sp = context.getSharedPreferences("nexpad_prefs", android.content.Context.MODE_PRIVATE)
            val padKey = if (stickControl.isLeft) "LTP" else "RTP"
            val specificSens = sp.getFloat("TOUCHPAD_SENSITIVITY_$padKey", -1f)
            val sens = if (specificSens > 0f) specificSens else {
                val globalPadSens = sp.getFloat("TOUCHPAD_SENSITIVITY", -1f)
                if (globalPadSens > 0f) globalPadSens else sp.getFloat("CAMERA_SENSITIVITY", 1.0f) * 2.0f
            }
            Pair(true, sens)
        } else if (!stickControl.isLeft) {
            val sp = context.getSharedPreferences("nexpad_prefs", android.content.Context.MODE_PRIVATE)
            Pair(
                sp.getBoolean("RIGHT_STICK_CAMERA_MODE", false),
                sp.getFloat("CAMERA_SENSITIVITY", 1.0f)
            )
        } else {
            Pair(false, 1.0f)
        }
    }
    val coroutineScope = rememberCoroutineScope()
    val density = androidx.compose.ui.platform.LocalDensity.current.density

    val gestureModifier = if (!isInteractive) {
        Modifier
    } else if (isCameraMode) {
        Modifier.pointerInput(isConnected, stickControl, cameraSensitivity, density) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val startTime = System.currentTimeMillis()
                var previousTouchX = down.position.x
                var previousTouchY = down.position.y
                var previousTimeMs = startTime
                var currentStickX = 0f
                var currentStickY = 0f
                var lastSpeed = 0f
                var decayJob: Job? = null
                val velocityBuffer = VelocityRingBuffer(8)
                inputTarget.onStickMove(stickControl, 0f, 0f)

                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id }
                    if (change == null || !change.pressed) break
                    change.consume()

                    val currentTouchX = change.position.x
                    val currentTouchY = change.position.y
                    val deltaX = currentTouchX - previousTouchX
                    val deltaY = currentTouchY - previousTouchY
                    previousTouchX = currentTouchX
                    previousTouchY = currentTouchY

                    var finalDeltaX = deltaX
                    var finalDeltaY = deltaY
                    val absX = kotlin.math.abs(deltaX)
                    val absY = kotlin.math.abs(deltaY)
                    if (absX > 3.0f * absY) {
                        finalDeltaY *= 0.5f // Suppress vertical wobble during horizontal turns
                    } else if (absY > 3.0f * absX) {
                        finalDeltaX *= 0.5f // Suppress horizontal wobble during vertical looks
                    }

                    val distPx = hypot(finalDeltaX, finalDeltaY)
                    val distDp = distPx / density

                    if (distDp > 0.15f) {
                        val currentTimeMs = System.currentTimeMillis()
                        val dtSec = ((currentTimeMs - previousTimeMs).coerceAtLeast(1L)) / 1000f
                        previousTimeMs = currentTimeMs

                        // Push sample into ring buffer for windowed average (eliminates frame-timing jitter)
                        velocityBuffer.push(distDp, dtSec)
                        val smoothedSpeed = velocityBuffer.averageSpeed()
                        lastSpeed = smoothedSpeed

                        val stickMagnitude = calculateGamingStickMagnitude(smoothedSpeed, cameraSensitivity)

                        if (stickMagnitude > 0f) {
                            val dirX = finalDeltaX / distPx
                            val dirY = finalDeltaY / distPx

                            val targetStickX = (dirX * stickMagnitude).coerceIn(-1f, 1f)
                            val targetStickY = (-dirY * stickMagnitude).coerceIn(-1f, 1f)

                            // Smooth response (EMA) — heavier smoothing absorbs remaining per-frame noise
                            currentStickX = 0.55f * targetStickX + 0.45f * currentStickX
                            currentStickY = 0.55f * targetStickY + 0.45f * currentStickY

                            inputTarget.onStickMove(stickControl, currentStickX, currentStickY)

                            thumbOffset = Offset(
                                (currentStickX * maxRadiusPx).coerceIn(-maxRadiusPx, maxRadiusPx),
                                (-currentStickY * maxRadiusPx).coerceIn(-maxRadiusPx, maxRadiusPx)
                            )

                            // Adaptive stationary watchdog: timeout scales with speed
                            val decayTimeoutMs = (120L - (smoothedSpeed / 20f).toLong()).coerceIn(50L, 120L)
                            decayJob?.cancel()
                            decayJob = coroutineScope.launch {
                                delay(decayTimeoutMs)
                                // 3-stage gentle decay
                                currentStickX *= 0.5f
                                currentStickY *= 0.5f
                                inputTarget.onStickMove(stickControl, currentStickX, currentStickY)
                                thumbOffset = Offset(
                                    currentStickX * maxRadiusPx,
                                    -currentStickY * maxRadiusPx
                                )
                                delay(30)
                                currentStickX *= 0.2f
                                currentStickY *= 0.2f
                                inputTarget.onStickMove(stickControl, currentStickX, currentStickY)
                                thumbOffset = Offset(
                                    currentStickX * maxRadiusPx,
                                    -currentStickY * maxRadiusPx
                                )
                                delay(25)
                                currentStickX = 0f
                                currentStickY = 0f
                                inputTarget.onStickMove(stickControl, 0f, 0f)
                                thumbOffset = Offset.Zero
                                lastSpeed = 0f
                            }
                        }
                    }
                }
                decayJob?.cancel()
                val releaseSpeed = velocityBuffer.peakSpeed()
                if (releaseSpeed > 400f) {
                    val coastSteps = ((releaseSpeed / 200f).toInt()).coerceIn(3, 7)
                    coroutineScope.launch {
                        var coastX = currentStickX
                        var coastY = currentStickY
                        repeat(coastSteps) {
                            coastX *= 0.68f
                            coastY *= 0.68f
                            inputTarget.onStickMove(stickControl, coastX, coastY)
                            thumbOffset = Offset(
                                coastX * maxRadiusPx,
                                -coastY * maxRadiusPx
                            )
                            delay(16)
                        }
                        thumbOffset = Offset.Zero
                        inputTarget.onStickMove(stickControl, 0f, 0f)
                    }
                } else {
                    thumbOffset = Offset.Zero
                    inputTarget.onStickMove(stickControl, 0f, 0f)
                }
            }
        }
    } else {
        Modifier.pointerInput(isConnected, stickControl) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            detectDragGestures(
                onDragStart = { downOffset ->
                    val vecX = downOffset.x - centerX
                    val vecY = downOffset.y - centerY
                    val dist = kotlin.math.hypot(vecX, vecY)
                    val clamped = if (dist > maxRadiusPx) {
                        Offset(vecX / dist * maxRadiusPx, vecY / dist * maxRadiusPx)
                    } else Offset(vecX, vecY)
                    thumbOffset = clamped
                    val normX = (clamped.x / maxRadiusPx).coerceIn(-1.0f, 1.0f)
                    val normY = (-clamped.y / maxRadiusPx).coerceIn(-1.0f, 1.0f)
                    inputTarget.onStickMove(stickControl, normX, normY)
                },
                onDragEnd = {
                    thumbOffset = Offset.Zero
                    inputTarget.onStickMove(stickControl, 0f, 0f)
                },
                onDragCancel = {
                    thumbOffset = Offset.Zero
                    inputTarget.onStickMove(stickControl, 0f, 0f)
                },
                onDrag = { change, _ ->
                    change.consume()
                    val vecX = change.position.x - centerX
                    val vecY = change.position.y - centerY
                    val dist = kotlin.math.hypot(vecX, vecY)
                    val clamped = if (dist > maxRadiusPx) {
                        Offset(vecX / dist * maxRadiusPx, vecY / dist * maxRadiusPx)
                    } else Offset(vecX, vecY)
                    thumbOffset = clamped

                    val normX = (clamped.x / maxRadiusPx).coerceIn(-1.0f, 1.0f)
                    val normY = (-clamped.y / maxRadiusPx).coerceIn(-1.0f, 1.0f)

                    inputTarget.onStickMove(stickControl, normX, normY)
                }
            )
        }
    }

    Box(
        modifier = modifier
            .size(def.size.widthDp.dp, def.size.heightDp.dp)
            .then(gestureModifier),
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
