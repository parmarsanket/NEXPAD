package com.sanket.tools.nexpad.ui

import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.utils.LayoutManager
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import android.os.Build
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import android.os.VibrationEffect
import com.sanket.tools.nexpad.ui.components.controller.RealisticBumper
import com.sanket.tools.nexpad.ui.components.controller.RealisticButton
import com.sanket.tools.nexpad.ui.components.controller.RealisticDPad
import com.sanket.tools.nexpad.ui.components.controller.RealisticJoystick
import com.sanket.tools.nexpad.ui.components.controller.RealisticMacroButton
import com.sanket.tools.nexpad.ui.components.controller.RealisticSystemButton
import com.sanket.tools.nexpad.ui.components.controller.RealisticTrigger
import com.sanket.tools.nexpad.utils.LockScreenOrientation
import kotlin.math.pow

@Composable
fun GamepadScreen(
    viewModel: GamepadViewModel, 
    layoutManager: LayoutManager,
    navigationViewModel: NavigationViewModel? = null,
    overrideProfileName: String? = null,
    onBack: () -> Unit,
    onVibrate: () -> Unit
) {
    BackHandler(onBack = onBack)
    val activeProfileName by layoutManager.activeProfileNameFlow.collectAsState()
    val profiles by layoutManager.profilesFlow.collectAsState()
    val sessionProfileName by (navigationViewModel?.sessionProfileName ?: remember { kotlinx.coroutines.flow.MutableStateFlow(null) }).collectAsState()

    val profile = remember(overrideProfileName, sessionProfileName, activeProfileName, profiles) {
        val targetName = overrideProfileName ?: sessionProfileName
        if (!targetName.isNullOrBlank()) {
            profiles.find { it.name.equals(targetName, ignoreCase = true) }
                ?: layoutManager.getAllProfiles().find { it.name.equals(targetName, ignoreCase = true) }
                ?: layoutManager.getActiveProfile()
        } else {
            layoutManager.getActiveProfile()
        }
    }
    val isConnected by viewModel.isConnected.collectAsState()
    
    val context = androidx.compose.ui.platform.LocalContext.current
    LockScreenOrientation(
        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    )

    // Initialize vibrator ONCE
    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(android.os.VibratorManager::class.java)
            vibratorManager?.defaultVibrator ?: context.getSystemService(android.os.Vibrator::class.java)!!
        } else {
            context.getSystemService(android.os.Vibrator::class.java)!!
        }
    }

    // ── Motor Hardware Detection ─────────────────────────────────────────
    // Detect the phone's haptic motor quality at init. This runs once and
    // determines how aggressively we can stream amplitude changes.
    //
    // Tier 1 "Legacy"  : API < 26 OR no amplitude control. Binary on/off only.
    //                     (Very old / ultra-cheap phones)
    // Tier 2 "ERM"     : Has amplitude control but no haptic primitives.
    //                     Spinning weight motor, 50-100ms response time.
    //                     (Moto G85, most mid-range phones)
    // Tier 3 "LRA"     : Has amplitude control AND haptic primitives.
    //                     Linear actuator, 5-20ms response time.
    //                     (Samsung S/Note/Z, Pixel, OnePlus flagships)
    // ────────────────────────────────────────────────────────────────────
    data class MotorProfile(
        val tier: Int,        // 1, 2, or 3
        val bandSize: Int,    // Amplitude quantization step
        val minGapMs: Long,   // Minimum time between hardware calls
        val name: String      // For logging
    )
    
    val motorProfile = remember {
        val hasAmplitude = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.hasAmplitudeControl()
        } else false
        
        val hasPrimitives = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // PRIMITIVE_CLICK is universally supported on LRA phones
            try {
                vibrator.arePrimitivesSupported(
                    android.os.VibrationEffect.Composition.PRIMITIVE_CLICK
                )[0]
            } catch (e: Exception) { false }
        } else false
        
        val profile = when {
            !hasAmplitude -> MotorProfile(1, 255, 150L, "Legacy/ERM-basic")
            hasPrimitives -> MotorProfile(3, 10, 30L, "LRA (premium)")
            else          -> MotorProfile(2, 32, 100L, "ERM (mid-range)")
        }
        // android.util.Log.d("NEXPAD_RUMBLE", "Motor detected: ${profile.name} | Tier ${profile.tier} | Band ${profile.bandSize} | MinGap ${profile.minGapMs}ms")
        profile
    }

    var isRumbling by remember { mutableStateOf(false) }
    var rumbleResetJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    val hapticHelper = remember(context) { com.sanket.tools.nexpad.utils.HapticFeedbackHelper(context) }
    
    val safeOnVibrate: () -> Unit = {
        val connected = viewModel.isConnected.value
        if (hapticHelper.canVibrate(connected, isRumbling)) {
            hapticHelper.performButtonClick()
        }
    }

    LaunchedEffect(Unit) {
        val sharedPref = context.getSharedPreferences("nexpad_prefs", android.content.Context.MODE_PRIVATE)
        
        // ── Haptic Streaming Engine ──────────────────────────────────────────
        // Android has NO streaming amplitude API. Every vibrate() call forces
        // the motor to physically brake → stop → spin up again.
        //
        // Solution: Quantize 0-255 into perceptual "bands" sized to match the
        // detected motor's physical capabilities. The motor holds its current
        // band via an infinite waveform. Band transitions are rate-limited to
        // match the motor's spin-up time.
        //
        // Tier 1 (Legacy):  Binary on/off, no amplitude. bandSize=255.
        // Tier 2 (ERM):     8 bands, 100ms gap.  Smooth on mid-range phones.
        // Tier 3 (LRA):     25 bands, 30ms gap.   Near-HD on flagships.
        // ─────────────────────────────────────────────────────────────────────
        
        val bandSize = motorProfile.bandSize
        val minGapMs = motorProfile.minGapMs
        
        var lastAppliedBand = -1
        var lastUpdateTimeMs = 0L
        
        viewModel.feedbackFlow.collect { feedback ->
            val intensityScalar = sharedPref.getFloat("RUMBLE_INTENSITY", 1.0f)
            val rumbleMode = sharedPref.getString("RUMBLE_MODE", "min") ?: "min"
            
            // ── Stage 1: Stereo-to-Mono Downmix ─────────────────────────
            // Controller has 2 motors (heavy left, light right).
            // Phone has 1 motor. Combine intelligently.
            val left = feedback.leftMotorSpeed
            val right = feedback.rightMotorSpeed
            
            val combinedSpeed = when (rumbleMode) {
                "min"   -> minOf(left, right)
                "max"   -> maxOf(left, right)
                "avg"   -> (left + right) / 2
                else    -> {
                    // "smart": Weighted downmix — dominant motor drives feel,
                    // weaker motor adds texture. Preserves game designer intent.
                    ((0.7f * maxOf(left, right) + 0.3f * minOf(left, right))).roundToInt()
                }
            }
            
            val scaledSpeed = (combinedSpeed * intensityScalar).roundToInt().coerceIn(0, 255)
            
            // ── Stage 2: Hardware Dead Zone ──────────────────────────────
            // Phone motors can't physically produce vibration below a threshold.
            // Instead of sending inaudible amplitude, snap to the minimum or zero.
            val motorDeadZone = when (motorProfile.tier) {
                1 -> 0     // Binary, no amplitude control
                3 -> 8     // LRA: precise, low threshold
                else -> 20 // ERM: spinning weight needs minimum voltage
            }
            
            val afterDeadZone = when {
                scaledSpeed == 0 -> 0
                scaledSpeed < motorDeadZone -> motorDeadZone
                else -> scaledSpeed
            }
            
            // ── Stage 3: Perceptual Gamma Curve (Weber-Fechner) ──────────
            // Human vibration perception is logarithmic. A linear 0-255 mapping
            // wastes the bottom range (feels dead) and the top range (feels flat).
            // Gamma 0.55 = square-root-ish curve that expands the low end and
            // compresses the top end for even perceptual distribution.
            val gamma = 0.55
            val totalSpeed = if (afterDeadZone == 0) 0
                             else (255.0 * (afterDeadZone / 255.0).pow(gamma)).roundToInt().coerceIn(1, 255)
            
            // ── Stage 4: Band Quantization ───────────────────────────────
            val band = when {
                totalSpeed == 0 -> 0
                bandSize >= 255 -> totalSpeed.coerceIn(1, 255) // Tier 1: no quantization
                else -> ((totalSpeed + bandSize / 2) / bandSize * bandSize).coerceIn(bandSize, 255)
            }
            
            val now = System.currentTimeMillis()
            val gap = now - lastUpdateTimeMs
            
            val shouldUpdate = when {
                band == 0 && lastAppliedBand != 0 -> true  // OFF: always instant
                band != 0 && lastAppliedBand == 0 -> true  // ON: always instant
                band != lastAppliedBand && gap >= minGapMs -> true  // Band changed + motor ready
                else -> false
            }
            
            if (shouldUpdate) {
                // android.util.Log.d("NEXPAD_RUMBLE", 
                //     if (band > 0) "T${motorProfile.tier} APPLY -> band=$band (raw=$totalSpeed) gap=${gap}ms"
                //     else "T${motorProfile.tier} OFF")
                
                lastAppliedBand = band
                lastUpdateTimeMs = now
                
                if (band > 0) {
                    isRumbling = true
                    when (motorProfile.tier) {
                        1 -> {
                            // Tier 1: No amplitude control. Binary vibration only.
                            vibrator.vibrate(
                                VibrationEffect.createWaveform(longArrayOf(0, 10000), 0)
                            )
                        }
                        else -> {
                            // Tier 2 & 3: Full amplitude waveform
                            vibrator.vibrate(
                                VibrationEffect.createWaveform(
                                    longArrayOf(0, 10000), intArrayOf(0, band), 0
                                )
                            )
                        }
                    }
                } else {
                    isRumbling = false
                    vibrator.cancel()
                }
            }

            // Safety watchdog: kill infinite vibration if packets stop arriving
            if (totalSpeed > 0) {
                rumbleResetJob?.cancel()
                rumbleResetJob = launch {
                    delay(250)
                    if (lastAppliedBand != 0) {
                        // android.util.Log.d("NEXPAD_RUMBLE", "WATCHDOG -> No packets for 250ms, motor killed")
                        lastAppliedBand = 0
                        isRumbling = false
                        vibrator.cancel()
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            rumbleResetJob?.cancel()
            vibrator.cancel()
            navigationViewModel?.clearGamepadSession()
        }
    }
    
    GamepadScreenContent(
        profile = profile,
        isConnected = isConnected,
        viewModel = viewModel,
        onBack = onBack,
        renderElement = { key, position ->
            val isStick = key.equals("LS", ignoreCase = true) || key.equals("RS", ignoreCase = true)
            // On GamepadScreen, joysticks are rendered with isLocked = false so RealisticJoystick
            // purely animates visual deflection from viewModel stick states while JoystickTouchLayer
            // handles the robust half-screen touch events.
            val isLocked = if (isStick) false else (position.isLocked ?: true)
            com.sanket.tools.nexpad.ui.components.controller.ControllerElementRenderer(
                key = key,
                isConnected = isConnected,
                isRgbEnabled = profile.isRgbEnabled,
                viewModel = viewModel,
                onVibrate = safeOnVibrate,
                customComponentId = position.customComponentId,
                sensitivity = position.sensitivity,
                heightScale = position.heightScale ?: 1.0f,
                isFlipped = position.isFlipped ?: false,
                isLocked = isLocked,
                labelStyle = profile.controllerLabelStyle
            )
        }
    )
}

@Composable
fun GamepadScreenContent(
    profile: com.sanket.tools.nexpad.model.LayoutProfile,
    isConnected: Boolean = false,
    viewModel: GamepadViewModel? = null,
    onBack: () -> Unit = {},
    renderElement: @Composable (key: String, position: com.sanket.tools.nexpad.model.Position) -> Unit,
    modifier: Modifier = Modifier
) {
    val lsFloatX = remember { Animatable(0f) }
    val lsFloatY = remember { Animatable(0f) }
    val rsFloatX = remember { Animatable(0f) }
    val rsFloatY = remember { Animatable(0f) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val screenWidthPx = maxOf(constraints.maxWidth, constraints.maxHeight).toFloat()
        val screenHeightPx = minOf(constraints.maxWidth, constraints.maxHeight).toFloat()
        val density = LocalDensity.current

        val lsEntry = profile.positions.entries.firstOrNull { it.key.equals("LS", ignoreCase = true) }
        val rsEntry = profile.positions.entries.firstOrNull { it.key.equals("RS", ignoreCase = true) }

        val joystickEntries = remember(profile.positions) {
            profile.positions.entries.filter { (key, _) ->
                key.equals("LS", ignoreCase = true) || key.equals("RS", ignoreCase = true)
            }
        }
        val nonJoystickEntries = remember(profile.positions) {
            profile.positions.entries.filter { (key, _) ->
                !key.equals("LS", ignoreCase = true) && !key.equals("RS", ignoreCase = true)
            }
        }

        // Dynamically track exact runtime bounding boxes of all non-joystick buttons in container coords
        val buttonBoundsMap = remember { mutableStateMapOf<String, Rect>() }
        LaunchedEffect(profile.positions) {
            buttonBoundsMap.clear()
        }

        // 1. Render joystick UI elements (LS, RS) — placed at bottom Z-order (visual base & knob)
        joystickEntries.forEach { (key, position) ->
            val isLs = key.equals("LS", ignoreCase = true)
            val isRs = key.equals("RS", ignoreCase = true)

            Box(
                modifier = Modifier
                    .layout { measurable, childConstraints ->
                        val placeable = measurable.measure(childConstraints)
                        val extraX = if (isLs) lsFloatX.value else if (isRs) rsFloatX.value else 0f
                        val extraY = if (isLs) lsFloatY.value else if (isRs) rsFloatY.value else 0f
                        val x = (position.xRatio * screenWidthPx - placeable.width / 2f + extraX).roundToInt()
                        val y = (position.yRatio * screenHeightPx - placeable.height / 2f + extraY).roundToInt()
                        layout(placeable.width, placeable.height) {
                            placeable.placeRelative(x, y)
                        }
                    }
                    .scale(position.scale)
                    .alpha(position.opacity)
            ) {
                renderElement(key, position)
            }
        }

        // 2. Independent Half-Screen Joystick Touch Layers (Center-to-Left for LS, Center-to-Right for RS)
        // Both joysticks are independently free in their Z-index; they occupy mutually exclusive screen halves.
        val halfWidthDp = with(density) { (screenWidthPx / 2f).toDp() }

        // Left Stick: handles center to left (x < screenWidth / 2)
        if (lsEntry != null) {
            val lsPos = lsEntry.value
            val lsMode = lsPos.joystickMode ?: if (lsPos.isLocked == true) "LOCKED" else "FULL"
            val leftExclusions = remember(buttonBoundsMap.toMap(), screenWidthPx) {
                buttonBoundsMap.values.filter { it.center.x < screenWidthPx / 2f }
            }
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(halfWidthDp)
                    .align(Alignment.CenterStart)
            ) {
                JoystickTouchLayer(
                    isLeft = true,
                    mode = lsMode,
                    hitboxScale = lsPos.hitboxScale ?: 1.5f,
                    position = lsPos,
                    screenWidthPx = screenWidthPx,
                    screenHeightPx = screenHeightPx,
                    floatX = lsFloatX,
                    floatY = lsFloatY,
                    viewModel = viewModel,
                    isConnected = isConnected,
                    exclusionRects = leftExclusions
                )
            }
        }

        // Right Stick: handles center to right (x >= screenWidth / 2)
        if (rsEntry != null) {
            val rsPos = rsEntry.value
            val rsMode = rsPos.joystickMode ?: if (rsPos.isLocked == true) "LOCKED" else "FULL"
            val rightExclusions = remember(buttonBoundsMap.toMap(), screenWidthPx) {
                buttonBoundsMap.values.filter { it.center.x >= screenWidthPx / 2f }
            }
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(halfWidthDp)
                    .align(Alignment.CenterEnd)
            ) {
                JoystickTouchLayer(
                    isLeft = false,
                    mode = rsMode,
                    hitboxScale = rsPos.hitboxScale ?: 1.5f,
                    position = rsPos,
                    screenWidthPx = screenWidthPx,
                    screenHeightPx = screenHeightPx,
                    floatX = rsFloatX,
                    floatY = rsFloatY,
                    viewModel = viewModel,
                    isConnected = isConnected,
                    exclusionRects = rightExclusions
                )
            }
        }

        // 3. Render all interactive buttons, D-pad, triggers, bumpers, menu buttons — HIGHEST Z-order.
        // Direct button taps are captured by the buttons with absolute priority.
        nonJoystickEntries.forEach { (key, position) ->
            Box(
                modifier = Modifier
                    .layout { measurable, childConstraints ->
                        val placeable = measurable.measure(childConstraints)
                        val x = (position.xRatio * screenWidthPx - placeable.width / 2f).roundToInt()
                        val y = (position.yRatio * screenHeightPx - placeable.height / 2f).roundToInt()
                        layout(placeable.width, placeable.height) {
                            placeable.placeRelative(x, y)
                        }
                    }
                    .scale(position.scale)
                    .alpha(position.opacity)
                    .onGloballyPositioned { coordinates ->
                        buttonBoundsMap[key] = coordinates.boundsInParent()
                    }
            ) {
                renderElement(key, position)
            }
        }
    }
}

@Composable
private fun BoxScope.JoystickTouchLayer(
    isLeft: Boolean,
    mode: String,
    hitboxScale: Float,
    position: com.sanket.tools.nexpad.model.Position,
    screenWidthPx: Float,
    screenHeightPx: Float,
    floatX: Animatable<Float, androidx.compose.animation.core.AnimationVector1D>,
    floatY: Animatable<Float, androidx.compose.animation.core.AnimationVector1D>,
    viewModel: GamepadViewModel?,
    isConnected: Boolean,
    exclusionRects: List<Rect>
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val currentExclusionRects by rememberUpdatedState(exclusionRects)

    // Joystick home center in screen coordinates
    val homeX = remember(position, screenWidthPx) { position.xRatio * screenWidthPx }
    val homeY = remember(position, screenHeightPx) { position.yRatio * screenHeightPx }

    // Maximum knob travel (PUBG/CoD maxRadius)
    val maxThrowPx = remember(density) { with(density) { 60.dp.toPx() } }
    val joystickRadiusPx = remember(position.scale, density) { with(density) { 75.dp.toPx() } * position.scale }

    // BOX mode: half-size of the square activation region (centered on home)
    val boxHalfPx = remember(mode, hitboxScale, position, density) {
        if (mode == "BOX") {
            val joystickDiamPx = with(density) { 150.dp.toPx() } * position.scale
            (joystickDiamPx * hitboxScale) / 2f
        } else 0f
    }

    // Snug button aura safety margin (red area in diagram)
    val marginPx = remember(density) { with(density) { 16.dp.toPx() } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(mode, homeX, homeY, boxHalfPx, screenWidthPx, isLeft) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)

                    // 1. Skip if a higher Z-order control (e.g. any interactive button) already claimed this touch
                    if (down.isConsumed) return@awaitEachGesture

                    // Convert local half-box coords (0 .. screenWidth/2) to full screen coords
                    val screenX = if (isLeft) down.position.x else down.position.x + screenWidthPx / 2f
                    val screenY = down.position.y

                    // 2. Button safety exclusion check:
                    // Inside the button and its snug surrounding aura (the red area), joysticks must NOT trigger.
                    val nearButton = currentExclusionRects.any { rect ->
                        val isRound = abs(rect.width - rect.height) < 8f
                        if (isRound) {
                            val r = (maxOf(rect.width, rect.height) / 2f) + marginPx
                            val dx = screenX - rect.center.x
                            val dy = screenY - rect.center.y
                            (dx * dx + dy * dy) <= (r * r)
                        } else {
                            screenX >= (rect.left - marginPx) && screenX <= (rect.right + marginPx) &&
                            screenY >= (rect.top - marginPx) && screenY <= (rect.bottom + marginPx)
                        }
                    }
                    if (nearButton) return@awaitEachGesture

                    // 3. Activation zone gate
                    val inZone = when (mode) {
                        "FULL" -> true
                        "BOX"  -> abs(screenX - homeX) <= boxHalfPx &&
                                  abs(screenY - homeY) <= boxHalfPx
                        "LOCKED" -> hypot(screenX - homeX, screenY - homeY) <= joystickRadiusPx
                        else   -> true
                    }
                    if (!inZone) return@awaitEachGesture

                    // Claim touch
                    down.consume()

                    // In floating modes, base snaps to finger; in LOCKED mode, base stays at home
                    val baseX = screenX
                    val baseY = screenY

                    if (mode != "LOCKED") {
                        coroutineScope.launch {
                            floatX.stop()
                            floatY.stop()
                            floatX.snapTo(baseX - homeX)
                            floatY.snapTo(baseY - homeY)
                        }
                    }

                    // Compute initial deflection
                    val originX = if (mode == "LOCKED") homeX else baseX
                    val originY = if (mode == "LOCKED") homeY else baseY

                    val initDist = hypot(screenX - originX, screenY - originY)
                    val deadPx = maxThrowPx * 0.05f
                    if (initDist < deadPx) {
                        if (isLeft) viewModel?.updateLeftStick(0f, 0f)
                        else        viewModel?.updateRightStick(0f, 0f)
                    } else {
                        val clampedDist = initDist.coerceAtMost(maxThrowPx)
                        val invDist = 1f / initDist
                        val knobX = (screenX - originX) * invDist * clampedDist
                        val knobY = (screenY - originY) * invDist * clampedDist
                        val normX = (knobX / maxThrowPx).coerceIn(-1f, 1f)
                        val normY = (-knobY / maxThrowPx).coerceIn(-1f, 1f)
                        if (isLeft) viewModel?.updateLeftStick(normX, normY)
                        else        viewModel?.updateRightStick(normX, normY)
                    }

                    // Drag loop
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id }
                        if (change == null || !change.pressed) break
                        change.consume()

                        val currentScreenX = if (isLeft) change.position.x else change.position.x + screenWidthPx / 2f
                        val currentScreenY = change.position.y

                        // Vector from origin (fixed base or home) to finger
                        val deltaX = currentScreenX - originX
                        val deltaY = currentScreenY - originY
                        val dist   = hypot(deltaX, deltaY)

                        // Base is LOCKED at touch-down point in floating mode (never drifts during drag)
                        // Knob tracks finger up to maxThrowPx, then clamps to the rim
                        val clampedDist = dist.coerceAtMost(maxThrowPx)
                        val knobX: Float
                        val knobY: Float
                        if (dist < 0.001f) {
                            knobX = 0f
                            knobY = 0f
                        } else {
                            val invDist = 1f / dist
                            knobX = deltaX * invDist * clampedDist
                            knobY = deltaY * invDist * clampedDist
                        }

                        val normX = if (dist < deadPx) 0f else (knobX / maxThrowPx).coerceIn(-1f, 1f)
                        val normY = if (dist < deadPx) 0f else (-knobY / maxThrowPx).coerceIn(-1f, 1f)

                        if (isLeft) viewModel?.updateLeftStick(normX, normY)
                        else        viewModel?.updateRightStick(normX, normY)
                    }

                    // Release — zero output, spring base back to home
                    if (isLeft) viewModel?.updateLeftStick(0f, 0f)
                    else        viewModel?.updateRightStick(0f, 0f)

                    if (mode != "LOCKED") {
                        coroutineScope.launch {
                            launch { floatX.animateTo(0f, spring(dampingRatio = 0.70f, stiffness = 400f)) }
                            launch { floatY.animateTo(0f, spring(dampingRatio = 0.70f, stiffness = 400f)) }
                        }
                    }
                }
            }
    )
}