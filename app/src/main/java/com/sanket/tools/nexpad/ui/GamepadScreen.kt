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

    companion object {
        val EXCLUDED_NON_BUTTON_KEYS = setOf(
            "LS", "RS", "LSB", "RSB", "L3", "R3",
            "LTP", "RTP"
        )
        val NON_SWEEPABLE_KEYS = setOf(
            "START", "BACK", "GUIDE", "GYRO", "SHARE",
            "SELECT", "MENU", "HOME", "CAPTURE"
        )
    }

    fun isNearAny(screenX: Float, screenY: Float, marginPx: Float): Boolean {
        val marginSq = marginPx * marginPx
        for ((key, rect) in bounds) {
            // Sticks and touchpads are never buttons to avoid
            if (EXCLUDED_NON_BUTTON_KEYS.contains(key.uppercase())) {
                continue
            }
            val dx = maxOf(rect.left - screenX, 0f, screenX - rect.right)
            val dy = maxOf(rect.top - screenY, 0f, screenY - rect.bottom)
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
            // Exclude sticks, stick clicks, and touchpads from button hit detection
            if (EXCLUDED_NON_BUTTON_KEYS.contains(key.uppercase())) {
                continue
            }

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
                labelStyle = profile.controllerLabelStyle,
                touchpadCurve = position.touchpadCurve
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
            val bufferPx = with(density) { 24.dp.toPx() } // Safe buffer area around joysticks
            joystickEntries.forEach { (_, pos) ->
                val cx = pos.xRatio * screenWidthPx
                val cy = pos.yRatio * screenHeightPx
                val r = (defaultRadius * pos.scale * responsiveScale) + bufferPx
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
                    sensitivity = ltpPos.sensitivity,
                    curve = com.sanket.tools.nexpad.model.TouchpadCurve.fromId(ltpPos.touchpadCurve)
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
                    sensitivity = rtpPos.sensitivity,
                    curve = com.sanket.tools.nexpad.model.TouchpadCurve.fromId(rtpPos.touchpadCurve)
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


        // 3. Render all interactive buttons, D-pad, triggers, bumpers, menu buttons
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

        // 4. Unified Touch Surface Coordinator (Top Z-Order Touch Interceptor)
        // Master coordinator for all interactive buttons and floating joysticks (BOX & FULL modes).
        val thumbRadiusPx = with(density) { 20.dp.toPx() }
        UnifiedTouchSurface(
            boundsRegistry = boundsRegistry,
            excludedJoystickCenters = excludedJoystickCenters,
            thumbRadiusPx = thumbRadiusPx,
            screenWidthPx = screenWidthPx,
            screenHeightPx = screenHeightPx,
            responsiveScale = responsiveScale,
            density = density,
            lsEntry = lsEntry,
            rsEntry = rsEntry,
            lsFloatX = lsFloatX,
            lsFloatY = lsFloatY,
            rsFloatX = rsFloatX,
            rsFloatY = rsFloatY,
            viewModel = viewModel,
            isSlideGuardEnabled = isSlideGuardEnabled,
            isEsportsMultitouchEnabled = isEsportsMultitouchEnabled,
            onVibrate = onVibrate,
            onLeftStickTouching = { isLeftStickTouching = it },
            onRightStickTouching = { isRightStickTouching = it },
            onPressedKeysChanged = { keys ->
                pressedButtonKeysState.value = keys
            }
        )

        // 5. Render joystick UI elements (LS, RS) — Rendered on top of UnifiedTouchSurface
        // When unlocked (BOX or FULL), joystick widgets are 100% touch-transparent and float dynamically.
        // When locked (LOCKED mode), joystick widgets handle direct touches themselves.
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
 * Unified Multi-Touch Surface Coordinator.
 *
 * Master touch dispatcher for all interactive buttons and floating joysticks (BOX & FULL modes).
 * 
 * Features:
 * 1. Buttons: Fast, zero-latency chord taps and fat-thumb bridging. Fluid button sliding when Esports toggle is ON.
 * 2. Floating Joysticks: Zero-conflict, full-half-screen (FULL) or bounded-box (BOX) touch dragging.
 * 3. Mutual Isolation: Pointers assigned to floating joysticks cannot sweep into buttons; button sweeps cannot hijack joysticks.
 * 4. Locked Mode Bypass: In FIXED/LOCKED mode, touches pass transparently to native joystick components.
 */
@Composable
fun UnifiedTouchSurface(
    boundsRegistry: BoundsRegistry,
    excludedJoystickCenters: List<Pair<Offset, Float>>,
    thumbRadiusPx: Float,
    screenWidthPx: Float,
    screenHeightPx: Float,
    responsiveScale: Float,
    density: androidx.compose.ui.unit.Density,
    lsEntry: Map.Entry<String, com.sanket.tools.nexpad.model.Position>?,
    rsEntry: Map.Entry<String, com.sanket.tools.nexpad.model.Position>?,
    lsFloatX: Animatable<Float, androidx.compose.animation.core.AnimationVector1D>,
    lsFloatY: Animatable<Float, androidx.compose.animation.core.AnimationVector1D>,
    rsFloatX: Animatable<Float, androidx.compose.animation.core.AnimationVector1D>,
    rsFloatY: Animatable<Float, androidx.compose.animation.core.AnimationVector1D>,
    viewModel: GamepadViewModel?,
    isSlideGuardEnabled: Boolean = true,
    isEsportsMultitouchEnabled: Boolean = true,
    onVibrate: () -> Unit,
    onLeftStickTouching: (Boolean) -> Unit,
    onRightStickTouching: (Boolean) -> Unit,
    onPressedKeysChanged: (Set<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val currentBoundsRegistry by rememberUpdatedState(boundsRegistry)
    val currentExcludedJoysticks by rememberUpdatedState(excludedJoystickCenters)
    val currentThumbRadius by rememberUpdatedState(thumbRadiusPx)
    val currentScreenWidthPx by rememberUpdatedState(screenWidthPx)
    val currentIsSlideGuardEnabled by rememberUpdatedState(isSlideGuardEnabled)
    val currentIsEsportsMultitouchEnabled by rememberUpdatedState(isEsportsMultitouchEnabled)
    val currentViewModel by rememberUpdatedState(viewModel)
    val currentOnVibrate by rememberUpdatedState(onVibrate)
    val currentOnLeftStickTouching by rememberUpdatedState(onLeftStickTouching)
    val currentOnRightStickTouching by rememberUpdatedState(onRightStickTouching)
    val currentOnPressedKeysChanged by rememberUpdatedState(onPressedKeysChanged)

    val maxThrowPx = remember(density) { with(density) { 60.dp.toPx() } }
    val marginPx = remember(density) { with(density) { 32.dp.toPx() } }

    val lsPos = lsEntry?.value
    val lsMode = remember(lsPos) {
        if (lsPos == null) JoystickMode.LOCKED
        else JoystickMode.fromId(
            lsPos.joystickMode ?: if (lsPos.isLocked == false) JoystickMode.FULL.id else JoystickMode.LOCKED.id
        )
    }
    val lsHomeX = remember(lsPos, screenWidthPx) { (lsPos?.xRatio ?: 0.11f) * screenWidthPx }
    val lsHomeY = remember(lsPos, screenHeightPx) { (lsPos?.yRatio ?: 0.74f) * screenHeightPx }
    val lsBoxHalfPx = remember(lsMode, lsPos, density, responsiveScale) {
        if (lsMode == JoystickMode.BOX && lsPos != null) {
            val joystickDiamPx = with(density) { 150.dp.toPx() } * lsPos.scale * responsiveScale
            val hitboxScale = lsPos.hitboxScale ?: 1.5f
            (joystickDiamPx * hitboxScale) / 2f
        } else 0f
    }

    val rsPos = rsEntry?.value
    val rsMode = remember(rsPos) {
        if (rsPos == null) JoystickMode.LOCKED
        else JoystickMode.fromId(
            rsPos.joystickMode ?: if (rsPos.isLocked == false) JoystickMode.FULL.id else JoystickMode.LOCKED.id
        )
    }
    val rsHomeX = remember(rsPos, screenWidthPx) { (rsPos?.xRatio ?: 0.895f) * screenWidthPx }
    val rsHomeY = remember(rsPos, screenHeightPx) { (rsPos?.yRatio ?: 0.74f) * screenHeightPx }
    val rsBoxHalfPx = remember(rsMode, rsPos, density, responsiveScale) {
        if (rsMode == JoystickMode.BOX && rsPos != null) {
            val joystickDiamPx = with(density) { 150.dp.toPx() } * rsPos.scale * responsiveScale
            val hitboxScale = rsPos.hitboxScale ?: 1.5f
            (joystickDiamPx * hitboxScale) / 2f
        } else 0f
    }

    var currentGlobalPressed by remember { mutableStateOf(emptySet<String>()) }

    DisposableEffect(Unit) {
        onDispose {
            currentViewModel?.updateLeftStick(0f, 0f)
            currentViewModel?.updateRightStick(0f, 0f)
            currentOnLeftStickTouching(false)
            currentOnRightStickTouching(false)
            for (key in currentGlobalPressed) {
                currentViewModel?.updateButton(key, false)
            }
            currentGlobalPressed = emptySet()
            currentOnPressedKeysChanged(emptySet())
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                var leftStickPointerId: PointerId? = null
                var leftBaseX = 0f
                var leftBaseY = 0f

                var rightStickPointerId: PointerId? = null
                var rightBaseX = 0f
                var rightBaseY = 0f

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
                            currentOnVibrate()
                            currentViewModel?.updateButton(key, true)
                        }
                        for (key in removed) {
                            currentViewModel?.updateButton(key, false)
                        }
                    }
                }

                fun resetLeftStick() {
                    if (leftStickPointerId != null) {
                        leftStickPointerId = null
                        currentOnLeftStickTouching(false)
                        currentViewModel?.updateLeftStick(0f, 0f)
                        coroutineScope.launch {
                            launch { lsFloatX.animateTo(0f, spring(dampingRatio = 0.70f, stiffness = 400f)) }
                            launch { lsFloatY.animateTo(0f, spring(dampingRatio = 0.70f, stiffness = 400f)) }
                        }
                    }
                }

                fun resetRightStick() {
                    if (rightStickPointerId != null) {
                        rightStickPointerId = null
                        currentOnRightStickTouching(false)
                        currentViewModel?.updateRightStick(0f, 0f)
                        coroutineScope.launch {
                            launch { rsFloatX.animateTo(0f, spring(dampingRatio = 0.70f, stiffness = 400f)) }
                            launch { rsFloatY.animateTo(0f, spring(dampingRatio = 0.70f, stiffness = 400f)) }
                        }
                    }
                }

                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)

                        for (change in event.changes) {
                            val pid = change.id
                            val px = change.position.x
                            val py = change.position.y
                            val isDown = !change.previousPressed && change.pressed
                            val isUp = change.previousPressed && !change.pressed

                            if (isDown) {
                                // 1. Check Button Hit-Testing first
                                val hits = currentBoundsRegistry.hitTestTouchDisk(
                                    screenX = px,
                                    screenY = py,
                                    thumbRadiusPx = currentThumbRadius,
                                    excludedJoystickCenters = currentExcludedJoysticks
                                )

                                if (hits.isNotEmpty()) {
                                    change.consume()
                                    activePointers[pid] = hits
                                    initialPointers[pid] = hits
                                    syncButtons()
                                } else {
                                    // 2. Not a button: Check floating joystick activation
                                    val nearButton = currentBoundsRegistry.isNearAny(px, py, marginPx)
                                    val midX = currentScreenWidthPx / 2f

                                    if (!nearButton) {
                                        // Left Stick
                                        if (px < midX && leftStickPointerId == null && lsMode != JoystickMode.LOCKED) {
                                            val inZone = when (lsMode) {
                                                JoystickMode.FULL -> true
                                                JoystickMode.BOX ->
                                                    abs(px - lsHomeX) <= lsBoxHalfPx &&
                                                    abs(py - lsHomeY) <= lsBoxHalfPx
                                                JoystickMode.LOCKED -> false
                                            }
                                            if (inZone) {
                                                leftStickPointerId = pid
                                                leftBaseX = px
                                                leftBaseY = py
                                                change.consume()
                                                currentOnLeftStickTouching(true)
                                                coroutineScope.launch {
                                                    lsFloatX.stop()
                                                    lsFloatY.stop()
                                                    lsFloatX.snapTo(leftBaseX - lsHomeX)
                                                    lsFloatY.snapTo(leftBaseY - lsHomeY)
                                                }
                                                currentViewModel?.updateLeftStick(0f, 0f)
                                            }
                                        }
                                        // Right Stick
                                        else if (px >= midX && rightStickPointerId == null && rsMode != JoystickMode.LOCKED) {
                                            val inZone = when (rsMode) {
                                                JoystickMode.FULL -> true
                                                JoystickMode.BOX ->
                                                    abs(px - rsHomeX) <= rsBoxHalfPx &&
                                                    abs(py - rsHomeY) <= rsBoxHalfPx
                                                JoystickMode.LOCKED -> false
                                            }
                                            if (inZone) {
                                                rightStickPointerId = pid
                                                rightBaseX = px
                                                rightBaseY = py
                                                change.consume()
                                                currentOnRightStickTouching(true)
                                                coroutineScope.launch {
                                                    rsFloatX.stop()
                                                    rsFloatY.stop()
                                                    rsFloatX.snapTo(rightBaseX - rsHomeX)
                                                    rsFloatY.snapTo(rightBaseY - rsHomeY)
                                                }
                                                currentViewModel?.updateRightStick(0f, 0f)
                                            }
                                        }
                                    }
                                }
                            } else if (change.pressed) {
                                // MOVE Events
                                if (pid == leftStickPointerId) {
                                    change.consume()
                                    val deltaX = px - leftBaseX
                                    val deltaY = py - leftBaseY
                                    val dist = hypot(deltaX, deltaY)
                                    val clampedDist = dist.coerceAtMost(maxThrowPx)
                                    val knobX = if (dist < 0.001f) 0f else deltaX / dist * clampedDist
                                    val knobY = if (dist < 0.001f) 0f else deltaY / dist * clampedDist
                                    val deadPx = maxThrowPx * 0.05f
                                    val normX = if (dist < deadPx) 0f else (knobX / maxThrowPx).coerceIn(-1f, 1f)
                                    val normY = if (dist < deadPx) 0f else (-knobY / maxThrowPx).coerceIn(-1f, 1f)
                                    currentViewModel?.updateLeftStick(normX, normY)
                                } else if (pid == rightStickPointerId) {
                                    change.consume()
                                    val deltaX = px - rightBaseX
                                    val deltaY = py - rightBaseY
                                    val dist = hypot(deltaX, deltaY)
                                    val clampedDist = dist.coerceAtMost(maxThrowPx)
                                    val knobX = if (dist < 0.001f) 0f else deltaX / dist * clampedDist
                                    val knobY = if (dist < 0.001f) 0f else deltaY / dist * clampedDist
                                    val deadPx = maxThrowPx * 0.05f
                                    val normX = if (dist < deadPx) 0f else (knobX / maxThrowPx).coerceIn(-1f, 1f)
                                    val normY = if (dist < deadPx) 0f else (-knobY / maxThrowPx).coerceIn(-1f, 1f)
                                    currentViewModel?.updateRightStick(normX, normY)
                                } else if (initialPointers.containsKey(pid)) {
                                    // Button Provenance: Pointer started on a button
                                    if (!currentIsEsportsMultitouchEnabled) {
                                        // Discrete mode: Only keep initial buttons, no sweeping
                                        val hits = currentBoundsRegistry.hitTestTouchDisk(
                                            screenX = px,
                                            screenY = py,
                                            thumbRadiusPx = currentThumbRadius,
                                            excludedJoystickCenters = currentExcludedJoysticks
                                        )
                                        val initial = initialPointers[pid] ?: emptySet()
                                        val preserved = hits.filter { initial.contains(it) }.toSet()
                                        change.consume()
                                        if (preserved.isNotEmpty()) {
                                            activePointers[pid] = preserved
                                        } else {
                                            activePointers.remove(pid)
                                        }
                                        syncButtons()
                                    } else {
                                        val isRightSide = px >= currentScreenWidthPx / 2f
                                        val isSideGuarded = currentIsSlideGuardEnabled &&
                                            (if (isRightSide) rightStickPointerId != null else leftStickPointerId != null)

                                        if (isSideGuarded) {
                                            // Side Guard active: stick is actively touching on this side, suppress acquiring new buttons
                                            val initial = initialPointers[pid] ?: emptySet()
                                            val hits = currentBoundsRegistry.hitTestTouchDisk(
                                                screenX = px,
                                                screenY = py,
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
                                        } else {
                                            // Full rolling sweep
                                            val hits = currentBoundsRegistry.hitTestTouchDisk(
                                                screenX = px,
                                                screenY = py,
                                                thumbRadiusPx = currentThumbRadius,
                                                excludedJoystickCenters = currentExcludedJoysticks
                                            )
                                            val initial = initialPointers[pid] ?: emptySet()
                                            val filteredHits = hits.filter { key ->
                                                initial.contains(key) || !BoundsRegistry.NON_SWEEPABLE_KEYS.contains(key.uppercase())
                                            }.toSet()
                                            change.consume()
                                            if (filteredHits.isNotEmpty()) {
                                                activePointers[pid] = filteredHits
                                            } else {
                                                activePointers.remove(pid)
                                            }
                                            syncButtons()
                                        }
                                    }
                                }
                            } else if (isUp || !change.pressed) {
                                if (pid == leftStickPointerId) {
                                    resetLeftStick()
                                } else if (pid == rightStickPointerId) {
                                    resetRightStick()
                                } else {
                                    initialPointers.remove(pid)
                                    if (activePointers.containsKey(pid)) {
                                        activePointers.remove(pid)
                                        syncButtons()
                                    }
                                }
                            }
                        }

                        // Clean up any stale pointers that are no longer pressed
                        if (leftStickPointerId != null && event.changes.any { it.id == leftStickPointerId && !it.pressed }) {
                            resetLeftStick()
                        }
                        if (rightStickPointerId != null && event.changes.any { it.id == rightStickPointerId && !it.pressed }) {
                            resetRightStick()
                        }
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