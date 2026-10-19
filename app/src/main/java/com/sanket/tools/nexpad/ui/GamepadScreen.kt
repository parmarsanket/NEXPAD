package com.sanket.tools.nexpad.ui

import android.content.Context
import android.content.pm.ActivityInfo
import android.os.SystemClock
import android.widget.Toast
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import com.sanket.tools.nexpad.ui.components.controller.LocalPressedControlKeys
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
import com.sanket.tools.nexpad.ui.components.controller.bumper.RealisticBumper
import com.sanket.tools.nexpad.ui.components.controller.abxy.RealisticButton
import com.sanket.tools.nexpad.ui.components.controller.dpad.RealisticDPad
import com.sanket.tools.nexpad.ui.components.controller.sticks.RealisticJoystick
import com.sanket.tools.nexpad.ui.components.controller.macros.RealisticMacroButton
import com.sanket.tools.nexpad.ui.components.controller.system.RealisticSystemButton
import com.sanket.tools.nexpad.ui.components.controller.trigger.RealisticTrigger
import com.sanket.tools.nexpad.ui.components.controller.special.InbuildTouchpadHalf
import com.sanket.tools.nexpad.utils.ImmersiveSystemBars
import com.sanket.tools.nexpad.utils.LockScreenOrientation
import com.sanket.tools.nexpad.model.JoystickMode
import kotlin.math.pow
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive

data class MotorProfile(
    val tier: Int,        // 1, 2, or 3
    val bandSize: Int,    // Amplitude quantization step
    val minGapMs: Long,   // Minimum time between hardware calls
    val name: String      // For logging
)

@Stable
class BoundsRegistry {
    private val bounds = java.util.concurrent.ConcurrentHashMap<String, Rect>()

    fun put(key: String, rect: Rect) {
        bounds[key] = rect
    }

    fun remove(key: String) {
        bounds.remove(key)
    }

    fun clear() {
        bounds.clear()
    }

    fun isNearAny(screenX: Float, screenY: Float, marginPx: Float, isLeft: Boolean, screenWidthPx: Float): Boolean {
        val marginSq = marginPx * marginPx
        val midX = screenWidthPx / 2f
        for ((_, rect) in bounds) {
            val inHalf = if (isLeft) rect.center.x < midX else rect.center.x >= midX
            if (!inHalf) continue
            val dx = maxOf(abs(screenX - rect.center.x) - rect.width / 2f, 0f)
            val dy = maxOf(abs(screenY - rect.center.y) - rect.height / 2f, 0f)
            if ((dx * dx + dy * dy) <= marginSq) {
                return true
            }
        }
        return false
    }

    fun getExclusionRects(isLeft: Boolean, screenWidthPx: Float): List<Rect> {
        val midX = screenWidthPx / 2f
        return bounds.values.filter { if (isLeft) it.center.x < midX else it.center.x >= midX }
    }

    /**
     * Esports Hitbox Hit-Testing:
     * Evaluates a circular contact patch (representing human thumb contact surface on capacitive glass)
     * against all registered interactive buttons.
     *
     * Features:
     * 1. Joystick Priority Gate: Touches inside excluded joystick circles return empty, preserving joysticks.
     * 2. Slide / Roll Hitbox Detection: Touches within [thumbRadiusPx] of button perimeter register as hits.
     * 3. Fat-Thumb Multi-Button Bridging: If the thumb patch bridges multiple buttons, returns all bridged keys.
     * 4. Integrated 4-Way D-Pad Directional Resolution: Angles resolve to UP, DOWN, LEFT, RIGHT, or diagonals.
     */
    fun hitTestTouchDisk(
        screenX: Float,
        screenY: Float,
        thumbRadiusPx: Float,
        excludedJoystickCenters: List<Pair<Offset, Float>> = emptyList()
    ): Set<String> {
        for ((center, radius) in excludedJoystickCenters) {
            if (hypot(screenX - center.x, screenY - center.y) <= radius) {
                return emptySet()
            }
        }

        val hits = mutableSetOf<String>()
        val thumbRadiusSq = thumbRadiusPx * thumbRadiusPx

        for ((key, rect) in bounds) {
            val isCompositeDpad = key.equals("DPAD", ignoreCase = true)
            val isSymmetricCircle = abs(rect.width - rect.height) < 4f

            if (isCompositeDpad) {
                val cx = rect.center.x
                val cy = rect.center.y
                val distToCenter = hypot(screenX - cx, screenY - cy)
                val dpadRadius = minOf(rect.width, rect.height) / 2f

                if (distToCenter <= dpadRadius + thumbRadiusPx) {
                    val deadzone = dpadRadius * 0.15f
                    if (distToCenter >= deadzone) {
                        val dx = screenX - cx
                        val dy = screenY - cy
                        val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble()))
                        if (angle in -157.5..-22.5) hits.add(com.sanket.tools.nexpad.model.NexpadKeys.UP)
                        if (angle in -67.5..67.5) hits.add(com.sanket.tools.nexpad.model.NexpadKeys.RIGHT)
                        if (angle in 22.5..157.5) hits.add(com.sanket.tools.nexpad.model.NexpadKeys.DOWN)
                        if (angle >= 112.5 || angle <= -112.5) hits.add(com.sanket.tools.nexpad.model.NexpadKeys.LEFT)
                    }
                }
            } else if (isSymmetricCircle) {
                val cx = rect.center.x
                val cy = rect.center.y
                val radius = minOf(rect.width, rect.height) / 2f
                val distToCenter = hypot(screenX - cx, screenY - cy)
                val distToPerimeter = maxOf(distToCenter - radius, 0f)
                if (distToPerimeter <= thumbRadiusPx) {
                    hits.add(key)
                }
            } else {
                val dx = maxOf(rect.left - screenX, 0f, screenX - rect.right)
                val dy = maxOf(rect.top - screenY, 0f, screenY - rect.bottom)
                if ((dx * dx + dy * dy) <= thumbRadiusSq) {
                    hits.add(key)
                }
            }
        }
        return hits
    }
}

@Composable
fun GamepadScreen(
    viewModel: GamepadViewModel, 
    layoutManager: LayoutManager,
    navigationViewModel: NavigationViewModel? = null,
    overrideProfileName: String? = null,
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val nexpadPrefs = remember(context) {
        com.sanket.tools.nexpad.storage.NexpadPreferences.getInstance(context)
    }
    val doubleTapExitEnabled = remember(nexpadPrefs) {
        nexpadPrefs.isDoubleTapExitEnabled
    }
    var lastBackPressTime by remember { mutableLongStateOf(0L) }
    var backToast by remember { mutableStateOf<Toast?>(null) }

    DisposableEffect(Unit) {
        viewModel.onGamepadScreenActive(true)
        onDispose {
            viewModel.releaseAllInputs()
            viewModel.onGamepadScreenActive(false)
            viewModel.updateLeftStick(0f, 0f)
            viewModel.updateRightStick(0f, 0f)
            backToast?.cancel()
            com.sanket.tools.nexpad.runtime.engine.clearNxprcRenderCaches()
        }
    }

    BackHandler {
        if (!doubleTapExitEnabled) {
            navigationViewModel?.clearGamepadSession()
            onBack()
        } else {
            val now = SystemClock.uptimeMillis()
            if (now - lastBackPressTime < 2000L) {
                backToast?.cancel()
                lastBackPressTime = 0L
                navigationViewModel?.clearGamepadSession()
                onBack()
            } else {
                lastBackPressTime = now
                backToast?.cancel()
                backToast = Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).apply {
                    show()
                }
            }
        }
    }

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
    val isSlideGuardEnabled by nexpadPrefs.isJoystickSlideGuardEnabledFlow.collectAsState()
    val isEsportsMultitouchEnabled by nexpadPrefs.isEsportsMultitouchEnabledFlow.collectAsState()
    
    LockScreenOrientation(
        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    )
    ImmersiveSystemBars()

    // Initialize vibrator ONCE safely
    val vibrator: android.os.Vibrator? = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(android.os.VibratorManager::class.java)
            vibratorManager?.defaultVibrator ?: context.getSystemService(android.os.Vibrator::class.java)
        } else {
            context.getSystemService(android.os.Vibrator::class.java)
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
    val motorProfile = remember {
        val hasAmplitude = vibrator?.hasAmplitudeControl() == true
        
        val hasPrimitives = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // PRIMITIVE_CLICK is universally supported on LRA phones
            try {
                vibrator?.arePrimitivesSupported(
                    android.os.VibrationEffect.Composition.PRIMITIVE_CLICK
                )?.getOrNull(0) == true
            } catch (e: Exception) { false }
        } else false
        
        val profile = when {
            !hasAmplitude -> MotorProfile(1, 255, 150L, "Legacy/ERM-basic")
            hasPrimitives -> MotorProfile(3, 10, 30L, "LRA (premium)")
            else          -> MotorProfile(2, 32, 100L, "ERM (mid-range)")
        }
        profile
    }

    val isRumblingRef = remember { java.util.concurrent.atomic.AtomicBoolean(false) }
    val rumbleJobRef = remember { java.util.concurrent.atomic.AtomicReference<kotlinx.coroutines.Job?>(null) }
    val hapticHelper = remember(context) { com.sanket.tools.nexpad.utils.HapticFeedbackHelper(context) }
    val currentViewModel by rememberUpdatedState(viewModel)

    val safeOnVibrate: () -> Unit = remember(hapticHelper) {
        {
            val connected = currentViewModel.isConnected.value
            if (hapticHelper.canVibrate(connected, isRumblingRef.get())) {
                hapticHelper.performButtonClick()
            }
        }
    }

    LaunchedEffect(Unit) {
        val nexpadPrefs = com.sanket.tools.nexpad.storage.NexpadPreferences.getInstance(context)
        
        val bandSize = motorProfile.bandSize
        val minGapMs = motorProfile.minGapMs
        
        val lastAppliedBand = AtomicInteger(0)
        val lastUpdateTimeMs = AtomicLong(0L)
        val motorOnStartTimeMs = AtomicLong(0L)
        val lastPacketTimeMs = AtomicLong(0L)

        // Single standing watchdog loop checking every 50ms instead of per-packet coroutine launches
        launch(kotlinx.coroutines.Dispatchers.Default) {
            while (isActive) {
                delay(50)
                val now = SystemClock.elapsedRealtime()
                val currentBand = lastAppliedBand.get()
                if (currentBand > 0 && (now - lastPacketTimeMs.get()) > 250) {
                    if (lastAppliedBand.compareAndSet(currentBand, 0)) {
                        rumbleJobRef.getAndSet(null)?.cancel()
                        isRumblingRef.set(false)
                        vibrator?.cancel()
                    }
                }
            }
        }
        
        launch(kotlinx.coroutines.Dispatchers.Default) {
            viewModel.feedbackFlow
                .collect { feedback ->
                    val now = SystemClock.elapsedRealtime()
                    lastPacketTimeMs.set(now)
                    val rumbleMode = nexpadPrefs.rumbleMode
                    
                    // ── Stage 1: Stereo-to-Mono Downmix ─────────────────────────
                    val left = feedback.leftMotorSpeed
                    val right = feedback.rightMotorSpeed
                    
                    val combinedSpeed = when (rumbleMode) {
                        com.sanket.tools.nexpad.model.RumbleMode.MAX   -> maxOf(left, right)
                        com.sanket.tools.nexpad.model.RumbleMode.SMART -> {
                            if (left == 0 || right == 0) maxOf(left, right)
                            else (0.7f * maxOf(left, right) + 0.3f * minOf(left, right)).roundToInt()
                        }
                        com.sanket.tools.nexpad.model.RumbleMode.AVG   -> (left + right) / 2
                        com.sanket.tools.nexpad.model.RumbleMode.MIN   -> minOf(left, right)
                    }
                    
                    val scaledSpeed = combinedSpeed.coerceIn(0, 255)
                    
                    // ── Stage 2: Hardware Dead Zone ──────────────────────────────
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
                    val gamma = 0.55
                    val totalSpeed = if (afterDeadZone == 0) 0
                                     else (255.0 * (afterDeadZone / 255.0).pow(gamma)).roundToInt().coerceIn(1, 255)
                    
                    // ── Stage 4: Band Quantization ───────────────────────────────
                    val band = when {
                        totalSpeed == 0 -> 0
                        bandSize >= 255 -> totalSpeed.coerceIn(1, 255)
                        else -> ((totalSpeed + bandSize / 2) / bandSize * bandSize).coerceIn(bandSize, 255)
                    }
                    
                    val gap = now - lastUpdateTimeMs.get()
                    
                    if (band > 0) {
                        rumbleJobRef.getAndSet(null)?.cancel()
                        val prevBand = lastAppliedBand.get()
                        val shouldUpdate = when {
                            prevBand <= 0 -> true // ON: always instant
                            band != prevBand && gap >= minGapMs -> true // Band changed + motor ready
                            else -> false
                        }
                        
                        if (shouldUpdate) {
                            if (prevBand <= 0) {
                                motorOnStartTimeMs.set(now)
                            }
                            lastAppliedBand.set(band)
                            lastUpdateTimeMs.set(now)
                            isRumblingRef.set(true)
                            
                            when (motorProfile.tier) {
                                1 -> {
                                    vibrator?.vibrate(
                                        VibrationEffect.createWaveform(longArrayOf(0, 10000), 0)
                                    )
                                }
                                else -> {
                                    vibrator?.vibrate(
                                        VibrationEffect.createWaveform(
                                            longArrayOf(0, 10000), intArrayOf(0, band), 0
                                        )
                                    )
                                }
                            }
                        }
                    } else {
                        val prevBand = lastAppliedBand.get()
                        if (prevBand > 0) {
                            val minBurstMs = if (motorProfile.tier == 3) 35L else 50L
                            val elapsed = now - motorOnStartTimeMs.get()
                            if (elapsed < minBurstMs) {
                                rumbleJobRef.getAndSet(null)?.cancel()
                                val job = launch(kotlinx.coroutines.Dispatchers.Default) {
                                    delay(minBurstMs - elapsed)
                                    lastAppliedBand.set(0)
                                    isRumblingRef.set(false)
                                    vibrator?.cancel()
                                }
                                rumbleJobRef.set(job)
                            } else {
                                rumbleJobRef.getAndSet(null)?.cancel()
                                lastAppliedBand.set(0)
                                isRumblingRef.set(false)
                                vibrator?.cancel()
                            }
                        }
                    }
                }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            rumbleJobRef.getAndSet(null)?.cancel()
            vibrator?.cancel()
        }
    }
    
    GamepadScreenContent(
        profile = profile,
        isConnected = isConnected,
        viewModel = viewModel,
        onBack = onBack,
        onVibrate = safeOnVibrate,
        isSlideGuardEnabled = isSlideGuardEnabled,
        isEsportsMultitouchEnabled = isEsportsMultitouchEnabled,
        renderElement = { key, position ->
            val isStick = key.equals(com.sanket.tools.nexpad.category.ControlKey.LS.name, ignoreCase = true) ||
                          key.equals(com.sanket.tools.nexpad.category.ControlKey.RS.name, ignoreCase = true)
            val resolvedMode = if (isStick) {
                com.sanket.tools.nexpad.model.JoystickMode.fromId(
                    position.joystickMode ?: if (position.isLocked == false) com.sanket.tools.nexpad.model.JoystickMode.FULL.id else com.sanket.tools.nexpad.model.JoystickMode.LOCKED.id
                )
            } else null
            val isLocked = if (isStick) (resolvedMode == com.sanket.tools.nexpad.model.JoystickMode.LOCKED) else (position.isLocked ?: true)
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
    onVibrate: () -> Unit = {},
    isSlideGuardEnabled: Boolean = true,
    isEsportsMultitouchEnabled: Boolean = true,
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
        val screenWidthPx = constraints.maxWidth.toFloat()
        val screenHeightPx = constraints.maxHeight.toFloat()
        val density = LocalDensity.current
        val widthDp = screenWidthPx / density.density
        val heightDp = screenHeightPx / density.density
        val responsiveScale = com.sanket.tools.nexpad.model.LayoutMetrics.calculateResponsiveScale(widthDp, heightDp)

        val adjustedPositions = remember(profile.positions, screenWidthPx, screenHeightPx) {
            com.sanket.tools.nexpad.model.LayoutMetrics.adjustClusterPositionsForAspectRatio(
                positions = profile.positions,
                screenWidthPx = screenWidthPx,
                screenHeightPx = screenHeightPx
            )
        }

        val context = androidx.compose.ui.platform.LocalContext.current
        val remoteRegistry = remember(context) { com.sanket.tools.nexpad.runtime.plugin.RemoteComponentRegistry.getInstance(context) }
        val loadedRemoteDocs by remoteRegistry.loadedComponents.collectAsState()

        val lsEntry = adjustedPositions.entries.firstOrNull { it.key.equals(com.sanket.tools.nexpad.category.ControlKey.LS.name, ignoreCase = true) }
        val rsEntry = adjustedPositions.entries.firstOrNull { it.key.equals(com.sanket.tools.nexpad.category.ControlKey.RS.name, ignoreCase = true) }
        val ltpEntry = adjustedPositions.entries.firstOrNull { it.key.equals(com.sanket.tools.nexpad.category.ControlKey.LTP.name, ignoreCase = true) }
        val rtpEntry = adjustedPositions.entries.firstOrNull { it.key.equals(com.sanket.tools.nexpad.category.ControlKey.RTP.name, ignoreCase = true) }

        val touchpadEntries = remember(adjustedPositions, loadedRemoteDocs) {
            adjustedPositions.entries.filter { (key, pos) ->
                val upperKey = key.uppercase()
                upperKey == com.sanket.tools.nexpad.category.ControlKey.LTP.name ||
                upperKey == com.sanket.tools.nexpad.category.ControlKey.RTP.name ||
                upperKey.contains("TOUCHPAD") ||
                pos.customComponentId?.let { id ->
                    id.contains("touchpad", ignoreCase = true) ||
                    id.contains("inbuild_", ignoreCase = true) ||
                    (id.startsWith("rc.") && (remoteRegistry.getComponent(id) ?: loadedRemoteDocs.firstOrNull { it.manifest.id == id })?.let { doc ->
                        doc.manifest.category.equals("TOUCHPAD", ignoreCase = true) ||
                        doc.manifest.id.contains("touchpad", ignoreCase = true) ||
                        doc.manifest.defaultControl.contains("TOUCHPAD", ignoreCase = true) ||
                        doc.manifest.defaultControl.equals("LTP", ignoreCase = true) ||
                        doc.manifest.defaultControl.equals("RTP", ignoreCase = true)
                    } == true)
                } == true
            }
        }
        val hasTouchpad = remember(touchpadEntries) { touchpadEntries.isNotEmpty() }
        val isEsportsSurfaceActive = !hasTouchpad && isEsportsMultitouchEnabled
        val pressedButtonKeysState = remember { mutableStateOf(emptySet<String>()) }

        val joystickEntries = remember(adjustedPositions, loadedRemoteDocs) {
            adjustedPositions.entries.filter { (key, pos) ->
                val upperKey = key.uppercase()
                upperKey == com.sanket.tools.nexpad.category.ControlKey.LS.name ||
                upperKey == com.sanket.tools.nexpad.category.ControlKey.RS.name ||
                upperKey.contains("JOYSTICK") ||
                pos.customComponentId?.let { id ->
                    id.contains("joystick", ignoreCase = true) ||
                    (id.startsWith("rc.") && (remoteRegistry.getComponent(id) ?: loadedRemoteDocs.firstOrNull { it.manifest.id == id })?.let { doc ->
                        doc.manifest.category.equals("JOYSTICK", ignoreCase = true) ||
                        doc.manifest.id.contains("joystick", ignoreCase = true) ||
                        doc.manifest.defaultControl.contains("JOYSTICK", ignoreCase = true) ||
                        doc.manifest.defaultControl.equals("LS", ignoreCase = true) ||
                        doc.manifest.defaultControl.equals("RS", ignoreCase = true)
                    } == true)
                } == true
            }
        }

        var isLeftStickTouching by remember { mutableStateOf(false) }
        var isRightStickTouching by remember { mutableStateOf(false) }

        val isLeftStickActive by remember(joystickEntries, isLeftStickTouching) {
            derivedStateOf {
                isLeftStickTouching ||
                hypot(viewModel?.leftStickX ?: 0f, viewModel?.leftStickY ?: 0f) > 0.05f
            }
        }
        val isRightStickActive by remember(joystickEntries, isRightStickTouching) {
            derivedStateOf {
                isRightStickTouching ||
                hypot(viewModel?.rightStickX ?: 0f, viewModel?.rightStickY ?: 0f) > 0.05f
            }
        }

        val excludedJoystickCenters = remember(joystickEntries, screenWidthPx, screenHeightPx, responsiveScale, density) {
            val list = mutableListOf<Pair<Offset, Float>>()
            val defaultRadius = with(density) { 75.dp.toPx() }
            joystickEntries.forEach { (_, pos) ->
                val cx = pos.xRatio * screenWidthPx
                val cy = pos.yRatio * screenHeightPx
                val r = defaultRadius * pos.scale * responsiveScale
                list.add(Pair(Offset(cx, cy), r))
            }
            list
        }

        val interactiveButtonEntries = remember(adjustedPositions, joystickEntries, touchpadEntries) {
            val nonButtonKeys = (joystickEntries.map { it.key } + touchpadEntries.map { it.key }).toSet()
            adjustedPositions.entries.filter { (key, _) ->
                !nonButtonKeys.contains(key)
            }
        }

        // Dynamically track exact runtime bounding boxes of all interactive buttons in container coords
        val boundsRegistry = remember(profile) { BoundsRegistry() }

        val halfWidthDp = with(density) { (screenWidthPx / 2f).toDp() }

        // 1. INBUILD TOUCHPAD FULL-SURFACE AMBIENT LAYER (LOWEST Z-INDEX)
        // Consumes all empty space: Center-to-Left for LTP, Center-to-Right for RTP.
        // Protected by 16.dp button exclusion buffer zone so interactive buttons have absolute priority.
        // NOTE: Only active for InbuildTouchpad ("builtin.inbuild_ltp" / "builtin.inbuild_rtp").
        // RealisticTouchPad (default), NXPRC, and custom skins render as discrete widgets at their placed positions.
        val inbuildLtp = ltpEntry?.takeIf { it.value.customComponentId?.startsWith("builtin.inbuild_") == true }
        val inbuildRtp = rtpEntry?.takeIf { it.value.customComponentId?.startsWith("builtin.inbuild_") == true }

        if (inbuildLtp != null) {
            val ltpPos = inbuildLtp.value
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(halfWidthDp)
                    .align(Alignment.CenterStart)
            ) {
                InbuildTouchpadHalf(
                    isLeft = true,
                    screenWidthPx = screenWidthPx,
                    screenHeightPx = screenHeightPx,
                    boundsProvider = { boundsRegistry.getExclusionRects(isLeft = true, screenWidthPx = screenWidthPx) },
                    isConnected = isConnected,
                    isRgbEnabled = profile.isRgbEnabled,
                    viewModel = viewModel,
                    sensitivity = ltpPos.sensitivity
                )
            }
        }

        if (inbuildRtp != null) {
            val rtpPos = inbuildRtp.value
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(halfWidthDp)
                    .align(Alignment.CenterEnd)
            ) {
                InbuildTouchpadHalf(
                    isLeft = false,
                    screenWidthPx = screenWidthPx,
                    screenHeightPx = screenHeightPx,
                    boundsProvider = { boundsRegistry.getExclusionRects(isLeft = false, screenWidthPx = screenWidthPx) },
                    isConnected = isConnected,
                    isRgbEnabled = profile.isRgbEnabled,
                    viewModel = viewModel,
                    sensitivity = rtpPos.sensitivity
                )
            }
        }

        // Discrete Touchpad UI elements (LTP, RTP) — Placed at lowest Z-order so user can place buttons over them.
        // Rendered for RealisticTouchPad (default), NXPRC, and custom skins (all non-inbuild touchpads).
        val discreteTouchpadEntries = remember(touchpadEntries) {
            touchpadEntries.filter { (_, pos) ->
                val id = pos.customComponentId
                id == null || !id.startsWith("builtin.inbuild_")
            }
        }

        discreteTouchpadEntries.forEach { (key, position) ->
            DisposableEffect(key) {
                onDispose { boundsRegistry.remove(key) }
            }
            Box(
                modifier = Modifier
                    .layout { measurable, childConstraints ->
                        val placeable = measurable.measure(childConstraints)
                        val safeX = if (position.xRatio.isFinite()) position.xRatio else 0f
                        val safeY = if (position.yRatio.isFinite()) position.yRatio else 0f
                        val x = (safeX * screenWidthPx - placeable.width / 2f).roundToInt()
                        val y = (safeY * screenHeightPx - placeable.height / 2f).roundToInt()
                        layout(placeable.width, placeable.height) {
                            placeable.placeRelative(x, y)
                        }
                    }
                    .scale(position.scale * responsiveScale)
                    .alpha(position.opacity)
                    .onGloballyPositioned { coordinates ->
                        boundsRegistry.put(key, coordinates.boundsInParent())
                    }
            ) {
                renderElement(key, position)
            }
        }


        // Independent Half-Screen Joystick Touch Layers (Center-to-Left for LS, Center-to-Right for RS)
        // Floating joystick is ONLY active for BOX and FULL modes.
        // When LOCKED, the joystick is locked at its home position and handles touches directly without floating.
        if (lsEntry != null) {
            val lsPos = lsEntry.value
            val lsMode = com.sanket.tools.nexpad.model.JoystickMode.fromId(
                lsPos.joystickMode ?: if (lsPos.isLocked == false) com.sanket.tools.nexpad.model.JoystickMode.FULL.id else com.sanket.tools.nexpad.model.JoystickMode.LOCKED.id
            )
            if (lsMode != com.sanket.tools.nexpad.model.JoystickMode.LOCKED) {
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
                        boundsRegistry = boundsRegistry,
                        responsiveScale = responsiveScale,
                        onTouchChanged = { touching -> isLeftStickTouching = touching }
                    )
                }
            }
        }

        // Right Stick: handles center to right (x >= screenWidth / 2)
        if (rsEntry != null) {
            val rsPos = rsEntry.value
            val rsMode = com.sanket.tools.nexpad.model.JoystickMode.fromId(
                rsPos.joystickMode ?: if (rsPos.isLocked == false) com.sanket.tools.nexpad.model.JoystickMode.FULL.id else com.sanket.tools.nexpad.model.JoystickMode.LOCKED.id
            )
            if (rsMode != com.sanket.tools.nexpad.model.JoystickMode.LOCKED) {
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
                        boundsRegistry = boundsRegistry,
                        responsiveScale = responsiveScale,
                        onTouchChanged = { touching -> isRightStickTouching = touching }
                    )
                }
            }
        }

        // 3. Render all interactive buttons, D-pad, triggers, bumpers, menu buttons — HIGHEST Z-order.
        // Direct button taps are captured by the buttons with absolute priority.
        CompositionLocalProvider(LocalPressedControlKeys provides pressedButtonKeysState.value) {
            interactiveButtonEntries.forEach { (key, position) ->
                DisposableEffect(key) {
                    onDispose { boundsRegistry.remove(key) }
                }
                Box(
                    modifier = Modifier
                        .layout { measurable, childConstraints ->
                            val placeable = measurable.measure(childConstraints)
                            val safeX = if (position.xRatio.isFinite()) position.xRatio else 0f
                            val safeY = if (position.yRatio.isFinite()) position.yRatio else 0f
                            val x = (safeX * screenWidthPx - placeable.width / 2f).roundToInt()
                            val y = (safeY * screenHeightPx - placeable.height / 2f).roundToInt()
                            layout(placeable.width, placeable.height) {
                                placeable.placeRelative(x, y)
                            }
                        }
                        .scale(position.scale * responsiveScale)
                        .alpha(position.opacity)
                        .onGloballyPositioned { coordinates ->
                            boundsRegistry.put(key, coordinates.boundsInParent())
                        }
                ) {
                    renderElement(key, position)
                }
            }
        }

        // 4. Esports Multi-Touch Surface Coordinator (Top Z-Order Touch Interceptor)
        // Active ONLY when NO touchpad exists in the layout (hasTouchpad == false).
        // Empowers rolling-thumb sweep/slide and fat-thumb dual chord press across all button hitboxes.
        if (isEsportsSurfaceActive) {
            val thumbRadiusPx = with(density) { 20.dp.toPx() }
            EsportsTouchSurface(
                boundsRegistry = boundsRegistry,
                excludedJoystickCenters = excludedJoystickCenters,
                thumbRadiusPx = thumbRadiusPx,
                screenWidthPx = screenWidthPx,
                isLeftStickActive = isLeftStickActive,
                isRightStickActive = isRightStickActive,
                isSlideGuardEnabled = isSlideGuardEnabled,
                onButtonPressed = { key ->
                    onVibrate()
                    viewModel?.updateButton(key, true)
                },
                onButtonReleased = { key ->
                    viewModel?.updateButton(key, false)
                },
                onPressedKeysChanged = { keys ->
                    pressedButtonKeysState.value = keys
                }
            )
        }

        // Render joystick UI elements (LS, RS) — Rendered on top of EsportsTouchSurface
        // so joystick pointerInput receives touches directly without interference.
        joystickEntries.forEach { (key, position) ->
            val isLs = key.equals("LS", ignoreCase = true)
            val isRs = key.equals("RS", ignoreCase = true)

            Box(
                modifier = Modifier
                    .layout { measurable, childConstraints ->
                        val placeable = measurable.measure(childConstraints)
                        val safeX = if (position.xRatio.isFinite()) position.xRatio else 0f
                        val safeY = if (position.yRatio.isFinite()) position.yRatio else 0f
                        val baseX = (safeX * screenWidthPx - placeable.width / 2f).roundToInt()
                        val baseY = (safeY * screenHeightPx - placeable.height / 2f).roundToInt()
                        layout(placeable.width, placeable.height) {
                            val extraX = if (isLs) lsFloatX.value else if (isRs) rsFloatX.value else 0f
                            val extraY = if (isLs) lsFloatY.value else if (isRs) rsFloatY.value else 0f
                            placeable.placeRelative(baseX + extraX.roundToInt(), baseY + extraY.roundToInt())
                        }
                    }
                    .scale(position.scale * responsiveScale)
                    .alpha(position.opacity)
            ) {
                renderElement(key, position)
            }
        }
    }
}

/**
 * Esports Multi-Touch Surface Coordinator.
 *
 * Active ONLY when NO touchpad exists in the layout (hasTouchpad == false).
 * Intercepts touch events on PointerEventPass.Initial and enables:
 * - Fluid sliding between buttons without lifting the finger (A -> B).
 * - Fat thumb simultaneous multi-button chord pressing (A + B).
 * - Leaves joysticks (LS, RS) 100% untouched and unconsumed.
 */
@Composable
fun EsportsTouchSurface(
    boundsRegistry: BoundsRegistry,
    excludedJoystickCenters: List<Pair<Offset, Float>>,
    thumbRadiusPx: Float,
    screenWidthPx: Float,
    isLeftStickActive: Boolean = false,
    isRightStickActive: Boolean = false,
    isSlideGuardEnabled: Boolean = true,
    onButtonPressed: (String) -> Unit,
    onButtonReleased: (String) -> Unit,
    onPressedKeysChanged: (Set<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentBoundsRegistry by rememberUpdatedState(boundsRegistry)
    val currentExcludedJoysticks by rememberUpdatedState(excludedJoystickCenters)
    val currentThumbRadius by rememberUpdatedState(thumbRadiusPx)
    val currentScreenWidthPx by rememberUpdatedState(screenWidthPx)
    val currentIsLeftStickActive by rememberUpdatedState(isLeftStickActive)
    val currentIsRightStickActive by rememberUpdatedState(isRightStickActive)
    val currentIsSlideGuardEnabled by rememberUpdatedState(isSlideGuardEnabled)
    val currentOnButtonPressed by rememberUpdatedState(onButtonPressed)
    val currentOnButtonReleased by rememberUpdatedState(onButtonReleased)
    val currentOnPressedKeysChanged by rememberUpdatedState(onPressedKeysChanged)

    var currentGlobalPressed by remember { mutableStateOf(emptySet<String>()) }

    DisposableEffect(Unit) {
        onDispose {
            for (key in currentGlobalPressed) {
                currentOnButtonReleased(key)
            }
            currentGlobalPressed = emptySet()
            currentOnPressedKeysChanged(emptySet())
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                val activePointers = mutableMapOf<PointerId, Set<String>>()
                val initialPointers = mutableMapOf<PointerId, Set<String>>()

                fun syncButtons() {
                    val newGlobal = mutableSetOf<String>()
                    for (set in activePointers.values) {
                        newGlobal.addAll(set)
                    }

                    val added = newGlobal - currentGlobalPressed
                    val removed = currentGlobalPressed - newGlobal

                    if (added.isNotEmpty() || removed.isNotEmpty()) {
                        currentGlobalPressed = newGlobal
                        currentOnPressedKeysChanged(newGlobal)

                        for (key in added) {
                            currentOnButtonPressed(key)
                        }
                        for (key in removed) {
                            currentOnButtonReleased(key)
                        }
                    }
                }

                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)

                        for (change in event.changes) {
                            val pid = change.id
                            val isDown = !change.previousPressed && change.pressed
                            val isUp = change.previousPressed && !change.pressed

                            if (isDown) {
                                val hits = currentBoundsRegistry.hitTestTouchDisk(
                                    screenX = change.position.x,
                                    screenY = change.position.y,
                                    thumbRadiusPx = currentThumbRadius,
                                    excludedJoystickCenters = currentExcludedJoysticks
                                )

                                if (hits.isNotEmpty()) {
                                    change.consume()
                                    activePointers[pid] = hits
                                    initialPointers[pid] = hits
                                    syncButtons()
                                }
                            } else if (change.pressed) {
                                val px = change.position.x
                                val isRightSide = px >= currentScreenWidthPx / 2f
                                val isSideGuarded = currentIsSlideGuardEnabled && (if (isRightSide) currentIsRightStickActive else currentIsLeftStickActive)

                                if (isSideGuarded) {
                                    // Active Joystick Slide Guard: Suppress continuous slide / sweep into adjacent buttons on active stick half.
                                    if (activePointers.containsKey(pid)) {
                                        // Pointer started directly on a button on this side: allow holding it, but NEVER acquire new buttons via slide
                                        val initial = initialPointers[pid] ?: emptySet()
                                        val hits = currentBoundsRegistry.hitTestTouchDisk(
                                            screenX = change.position.x,
                                            screenY = change.position.y,
                                            thumbRadiusPx = currentThumbRadius,
                                            excludedJoystickCenters = currentExcludedJoysticks
                                        )
                                        val preserved = hits.filter { initial.contains(it) }.toSet()
                                        change.consume()
                                        if (preserved.isNotEmpty()) {
                                            activePointers[pid] = preserved
                                        } else {
                                            activePointers.remove(pid)
                                        }
                                        syncButtons()
                                    }
                                    // Pointers not in activePointers (e.g. dragging joystick or neutral space) are strictly ignored
                                } else {
                                    // Guard not active on this side: full Esports rolling sweep
                                    val hits = currentBoundsRegistry.hitTestTouchDisk(
                                        screenX = change.position.x,
                                        screenY = change.position.y,
                                        thumbRadiusPx = currentThumbRadius,
                                        excludedJoystickCenters = currentExcludedJoysticks
                                    )
                                    if (hits.isNotEmpty() || activePointers.containsKey(pid)) {
                                        change.consume()
                                        if (hits.isNotEmpty()) {
                                            activePointers[pid] = hits
                                        } else {
                                            activePointers.remove(pid)
                                        }
                                        syncButtons()
                                    }
                                }
                            } else if (isUp || !change.pressed) {
                                initialPointers.remove(pid)
                                if (activePointers.containsKey(pid)) {
                                    activePointers.remove(pid)
                                    syncButtons()
                                }
                            }
                        }

                        // Clean up any stale pointers that are no longer pressed
                        val unpressedIds = activePointers.keys.filter { id ->
                            event.changes.any { it.id == id && !it.pressed }
                        }
                        if (unpressedIds.isNotEmpty()) {
                            for (id in unpressedIds) {
                                activePointers.remove(id)
                                initialPointers.remove(id)
                            }
                            syncButtons()
                        }
                    }
                }
            }
    )
}

@Composable
private fun BoxScope.JoystickTouchLayer(
    isLeft: Boolean,
    mode: JoystickMode,
    hitboxScale: Float,
    position: com.sanket.tools.nexpad.model.Position,
    screenWidthPx: Float,
    screenHeightPx: Float,
    floatX: Animatable<Float, androidx.compose.animation.core.AnimationVector1D>,
    floatY: Animatable<Float, androidx.compose.animation.core.AnimationVector1D>,
    viewModel: GamepadViewModel?,
    isConnected: Boolean,
    boundsRegistry: BoundsRegistry,
    responsiveScale: Float = 1.0f,
    onTouchChanged: ((Boolean) -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    DisposableEffect(isLeft) {
        onDispose {
            onTouchChanged?.invoke(false)
        }
    }

    // Joystick home center in screen coordinates
    val homeX = remember(position, screenWidthPx) { position.xRatio * screenWidthPx }
    val homeY = remember(position, screenHeightPx) { position.yRatio * screenHeightPx }

    // Maximum knob travel (PUBG/CoD maxRadius)
    val maxThrowPx = remember(density) { with(density) { 60.dp.toPx() } }

    // BOX mode: half-size of the square activation region (centered on home)
    val resolvedJoystickMode = mode
    val boxHalfPx = remember(resolvedJoystickMode, hitboxScale, position, density, responsiveScale) {
        if (resolvedJoystickMode == JoystickMode.BOX) {
            val joystickDiamPx = with(density) { 150.dp.toPx() } * position.scale * responsiveScale
            (joystickDiamPx * hitboxScale) / 2f
        } else 0f
    }

    // Snug button aura safety margin (red area in diagram)
    val marginPx = remember(density) { with(density) { 16.dp.toPx() } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(resolvedJoystickMode, homeX, homeY, boxHalfPx, screenWidthPx, isLeft) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)

                    // 1. Skip if a higher Z-order control (e.g. any interactive button) already claimed this touch
                    if (down.isConsumed) return@awaitEachGesture

                    // Convert local half-box coords (0 .. screenWidth/2) to full screen coords
                    val screenX = if (isLeft) down.position.x else down.position.x + screenWidthPx / 2f
                    val screenY = down.position.y

                    // 2. Universal element boundary exclusion check:
                    val nearButton = boundsRegistry.isNearAny(screenX, screenY, marginPx, isLeft, screenWidthPx)
                    if (nearButton) return@awaitEachGesture

                    // 3. Activation zone gate (floating joystick only active for FULL or BOX)
                    val inZone = when (resolvedJoystickMode) {
                        com.sanket.tools.nexpad.model.JoystickMode.FULL -> true
                        com.sanket.tools.nexpad.model.JoystickMode.BOX  -> abs(screenX - homeX) <= boxHalfPx &&
                                                                            abs(screenY - homeY) <= boxHalfPx
                        com.sanket.tools.nexpad.model.JoystickMode.LOCKED -> false
                    }
                    if (!inZone) return@awaitEachGesture

                    // Claim touch
                    down.consume()
                    onTouchChanged?.invoke(true)

                    try {
                        // In floating modes, base snaps to finger and locks during drag
                        val baseX = screenX
                        val baseY = screenY

                        coroutineScope.launch {
                            floatX.stop()
                            floatY.stop()
                            floatX.snapTo(baseX - homeX)
                            floatY.snapTo(baseY - homeY)
                        }

                        // Zero deflection at touch down
                        if (isLeft) viewModel?.updateLeftStick(0f, 0f)
                        else        viewModel?.updateRightStick(0f, 0f)

                        // Drag loop
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                            if (change == null || !change.pressed) break
                            change.consume()

                            val currentScreenX = if (isLeft) change.position.x else change.position.x + screenWidthPx / 2f
                            val currentScreenY = change.position.y

                            // Vector from fixed base origin to finger
                            val deltaX = currentScreenX - baseX
                            val deltaY = currentScreenY - baseY
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

                            val deadPx = maxThrowPx * 0.05f
                            val normX = if (dist < deadPx) 0f else (knobX / maxThrowPx).coerceIn(-1f, 1f)
                            val normY = if (dist < deadPx) 0f else (-knobY / maxThrowPx).coerceIn(-1f, 1f)

                            if (isLeft) viewModel?.updateLeftStick(normX, normY)
                            else        viewModel?.updateRightStick(normX, normY)
                        }

                        // Release — spring base back to home
                        coroutineScope.launch {
                            launch { floatX.animateTo(0f, spring(dampingRatio = 0.70f, stiffness = 400f)) }
                            launch { floatY.animateTo(0f, spring(dampingRatio = 0.70f, stiffness = 400f)) }
                        }
                    } finally {
                        if (isLeft) viewModel?.updateLeftStick(0f, 0f)
                        else        viewModel?.updateRightStick(0f, 0f)
                        onTouchChanged?.invoke(false)
                    }
                }
            }
    )
}