package com.sanket.tools.nexpad.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * High-definition haptic feedback engine for NEXPAD.
 *
 * Provides physical mechanical switch emulation using modern Android LRA haptic primitives:
 * - API 30+ (Android 11+): VibrationEffect.Composition with PRIMITIVE_CLICK / PRIMITIVE_THUD / PRIMITIVE_TICK
 * - API 29 (Android 10): VibrationEffect.createPredefined(EFFECT_CLICK)
 * - Fallback: Calibrated micro-impulse one-shot (12ms)
 *
 * Backed by "nexpad_prefs" shared preferences:
 * - BUTTON_HAPTICS_ENABLED (Boolean, default: true)
 * - HAPTICS_OFFLINE_ENABLED (Boolean, default: true)
 * - HAPTICS_CLICK_STRENGTH (Float, default: 0.3f, range: 0.1f..1.0f)
 * - HAPTICS_STYLE (String: "crisp", "heavy", "soft", default: "crisp")
 */
class HapticFeedbackHelper(
    context: Context,
    prefsOverride: com.sanket.tools.nexpad.storage.NexpadPreferences? = null,
    vibratorOverride: Vibrator? = null
) {

    private val appContext = context.applicationContext ?: context

    private val nexpadPrefs by lazy {
        prefsOverride ?: com.sanket.tools.nexpad.storage.NexpadPreferences.getInstance(appContext)
    }

    private val vibrator: Vibrator? by lazy {
        vibratorOverride ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = appContext.getSystemService(VibratorManager::class.java)
            vibratorManager?.defaultVibrator ?: appContext.getSystemService(Vibrator::class.java)
        } else {
            appContext.getSystemService(Vibrator::class.java)
        }
    }

    val isButtonHapticsEnabled: Boolean
        get() = nexpadPrefs.isButtonHapticsEnabled

    val isOfflineHapticsEnabled: Boolean
        get() = nexpadPrefs.isHapticsOfflineEnabled

    val clickStrength: Float
        get() = nexpadPrefs.hapticClickStrength.coerceIn(0.1f, 1.0f)

    val hapticStyle: com.sanket.tools.nexpad.model.HapticStyle
        get() = nexpadPrefs.hapticStyle

    /**
     * Checks whether button vibration should fire given the current connection and rumble state.
     * Prevents button clicks from overriding active PC game motor rumble.
     */
    fun canVibrate(isConnected: Boolean, isRumbling: Boolean): Boolean {
        if (vibrator?.hasVibrator() != true) return false
        if (isRumbling) return false
        if (!isButtonHapticsEnabled) return false
        if (!isConnected && !isOfflineHapticsEnabled) return false
        return true
    }

    /**
     * Fires a mechanical tactile click sensation calibrated to the user's strength and style.
     */
    fun performButtonClick(scaleMultiplier: Float = 1.0f) {
        if (!isButtonHapticsEnabled) return
        triggerImpulse(hapticStyle, clickStrength * scaleMultiplier)
    }

    /**
     * Fires an immediate test click for settings UI preview feedback.
     */
    fun performPreviewClick(style: com.sanket.tools.nexpad.model.HapticStyle = hapticStyle, strength: Float = clickStrength) {
        triggerImpulse(style, strength)
    }

    fun performPreviewClick(styleId: String, strength: Float = clickStrength) {
        performPreviewClick(com.sanket.tools.nexpad.model.HapticStyle.fromId(styleId), strength)
    }

    private fun triggerImpulse(style: com.sanket.tools.nexpad.model.HapticStyle, rawStrength: Float) {
        if (vibrator?.hasVibrator() != true) return
        val strength = rawStrength.coerceIn(0.1f, 1.0f)

        try {
            // Modern Android 11+ (API 30+) Rich Haptic Composition
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val primitive = when (style) {
                    com.sanket.tools.nexpad.model.HapticStyle.HEAVY -> VibrationEffect.Composition.PRIMITIVE_THUD
                    com.sanket.tools.nexpad.model.HapticStyle.SOFT -> VibrationEffect.Composition.PRIMITIVE_TICK
                    com.sanket.tools.nexpad.model.HapticStyle.CRISP -> VibrationEffect.Composition.PRIMITIVE_CLICK
                }

                val isSupported = try {
                    vibrator?.areAllPrimitivesSupported(primitive) == true
                } catch (_: Exception) {
                    false
                }

                if (isSupported) {
                    val composition = VibrationEffect.startComposition()
                        .addPrimitive(primitive, strength)
                        .compose()
                    vibrator?.vibrate(composition)
                    return
                }
            }

            // Android 10+ (API 29+) Predefined Click Fallback
            val predefinedEffect = when (style) {
                com.sanket.tools.nexpad.model.HapticStyle.HEAVY -> VibrationEffect.EFFECT_HEAVY_CLICK
                com.sanket.tools.nexpad.model.HapticStyle.SOFT -> VibrationEffect.EFFECT_TICK
                com.sanket.tools.nexpad.model.HapticStyle.CRISP -> VibrationEffect.EFFECT_CLICK
            }
            vibrator?.vibrate(VibrationEffect.createPredefined(predefinedEffect))
        } catch (_: Exception) {
            // Precision micro-impulse fallback (12ms) with amplitude scaling
            try {
                val amplitude = (strength * 255).toInt().coerceIn(1, 255)
                vibrator?.vibrate(VibrationEffect.createOneShot(12, amplitude))
            } catch (_: Exception) {
                // Motor unavailable or hardware busy
            }
        }
    }

    companion object {
        private const val TAG = "NexpadHaptics"
    }
}
