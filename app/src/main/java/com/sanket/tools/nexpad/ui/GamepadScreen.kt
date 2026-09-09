package com.sanket.tools.nexpad.ui

import android.content.pm.ActivityInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.utils.LayoutManager
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel
import androidx.compose.ui.layout.layout
import kotlin.math.roundToInt
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
    onBack: () -> Unit,
    onVibrate: () -> Unit
) {
    val profile = layoutManager.getActiveProfile()
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
    
    val safeOnVibrate: () -> Unit = remember { {
        if (!isRumbling) {
            onVibrate()
        }
    } }

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
                
                @Suppress("DEPRECATION")
                if (band > 0) {
                    isRumbling = true
                    when (motorProfile.tier) {
                        1 -> {
                            // Tier 1: No amplitude control. Binary vibration only.
                            vibrator.vibrate(longArrayOf(0, 10000), 0)
                        }
                        else -> {
                            // Tier 2 & 3: Full amplitude waveform
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                vibrator.vibrate(VibrationEffect.createWaveform(
                                    longArrayOf(0, 10000), intArrayOf(0, band), 0))
                            } else {
                                vibrator.vibrate(longArrayOf(0, 10000), 0)
                            }
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
    
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val screenWidthPx = maxOf(constraints.maxWidth, constraints.maxHeight).toFloat()
        val screenHeightPx = minOf(constraints.maxWidth, constraints.maxHeight).toFloat()

        // Back Button & Connection Status
        Row(
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Text("⬅️", fontSize = 24.sp, color = MaterialTheme.colorScheme.onBackground)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(if (isConnected) "🟢 Connected" else "🔴 Disconnected", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.labelMedium)
        }

        // Render mapped components with center-based placement
        profile.positions.forEach { (key, position) ->
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
            ) {
                com.sanket.tools.nexpad.ui.components.controller.ControllerElementRenderer(
                    key = key,
                    isConnected = isConnected,
                    isRgbEnabled = profile.isRgbEnabled,
                    viewModel = viewModel,
                    onVibrate = safeOnVibrate,
                    customComponentId = position.customComponentId
                )
            }
        }
    }
}
