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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.ui.components.*
import com.sanket.tools.nexpad.utils.LayoutManager
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel
import kotlin.math.roundToInt
import android.os.Vibrator
import android.os.Build
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import android.os.VibrationEffect
import com.sanket.tools.nexpad.utils.LockScreenOrientation

@Composable
fun GamepadScreen(
    viewModel: GamepadViewModel, 
    layoutManager: LayoutManager,
    onBack: () -> Unit,
    onVibrate: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp.value
    val screenHeight = configuration.screenHeightDp.dp.value

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
            val rumbleMode = sharedPref.getString("RUMBLE_MODE", "avg") ?: "avg"
            
            val combinedSpeed = when (rumbleMode) {
                "min" -> minOf(feedback.leftMotorSpeed, feedback.rightMotorSpeed)
                "max" -> maxOf(feedback.leftMotorSpeed, feedback.rightMotorSpeed)
                else -> (feedback.leftMotorSpeed + feedback.rightMotorSpeed) / 2
            }
            
            val rawSpeed = (combinedSpeed * intensityScalar).roundToInt()
            val totalSpeed = rawSpeed.coerceIn(0, 255)
            
            // Quantize to band — floor at bandSize so nonzero input never rounds to 0
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
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
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

        // Render mapped components
        profile.positions.forEach { (key, position) ->
            val offsetX = (position.xRatio * screenWidth).roundToInt()
            val offsetY = (position.yRatio * screenHeight).roundToInt()
            
            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX, offsetY) }
                    .scale(position.scale)
                    .alpha(position.opacity)
            ) {
                when {
                    key == "LS" -> RealisticJoystick(isLeft = true, isConnected = isConnected, viewModel = viewModel, isRgbEnabled = profile.isRgbEnabled)
                    key == "RS" -> RealisticJoystick(isLeft = false, isConnected = isConnected, viewModel = viewModel, isRgbEnabled = profile.isRgbEnabled)
                    key == "DPAD" -> RealisticDPad(isConnected = isConnected, viewModel = viewModel, isRgbEnabled = profile.isRgbEnabled)
                    key == "LT" || key == "RT" -> RealisticTrigger(key = key, isConnected = isConnected, onVibrate = safeOnVibrate, viewModel = viewModel, isRgbEnabled = profile.isRgbEnabled)
                    key == "LB" || key == "RB" -> RealisticBumper(key = key, isConnected = isConnected, onVibrate = safeOnVibrate, viewModel = viewModel, isRgbEnabled = profile.isRgbEnabled)
                    key == "A" -> RealisticButton(key = "A", buttonColor = Color(0xFF00C853), isConnected = isConnected, onVibrate = safeOnVibrate, viewModel = viewModel, isRgbEnabled = profile.isRgbEnabled)
                    key == "B" -> RealisticButton(key = "B", buttonColor = Color(0xFFD50000), isConnected = isConnected, onVibrate = safeOnVibrate, viewModel = viewModel, isRgbEnabled = profile.isRgbEnabled)
                    key == "X" -> RealisticButton(key = "X", buttonColor = Color(0xFF2962FF), isConnected = isConnected, onVibrate = safeOnVibrate, viewModel = viewModel, isRgbEnabled = profile.isRgbEnabled)
                    key == "Y" -> RealisticButton(key = "Y", buttonColor = Color(0xFFFFD600), isConnected = isConnected, onVibrate = safeOnVibrate, viewModel = viewModel, isRgbEnabled = profile.isRgbEnabled)
                    key in listOf("MENU", "VIEW", "XBOX", "SHARE", "SCREENSHOT") -> RealisticSystemButton(key = key, isConnected = isConnected, onVibrate = safeOnVibrate, viewModel = viewModel, isRgbEnabled = profile.isRgbEnabled)
                    key in listOf("M1", "M2", "M3", "M4", "PROFILE", "TURBO") -> RealisticMacroButton(key = key, isConnected = isConnected, onVibrate = safeOnVibrate, viewModel = viewModel, isRgbEnabled = profile.isRgbEnabled)
                    else -> RealisticButton(key = key, buttonColor = Color.Gray, isConnected = isConnected, onVibrate = safeOnVibrate, viewModel = viewModel, isRgbEnabled = profile.isRgbEnabled)
                }
            }
        }
    }
}
