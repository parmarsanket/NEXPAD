package com.sanket.tools.nexpad.utils

import android.content.Context
import android.content.SharedPreferences
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
 * - HAPTICS_CLICK_STRENGTH (Float, default: 0.8f, range: 0.1f..1.0f)
 * - HAPTICS_STYLE (String: "crisp", "heavy", "soft", default: "crisp")
 */
class HapticFeedbackHelper(private val context: Context) {

    private val sharedPref: SharedPreferences by lazy {
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
    }

    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(VibratorManager::class.java)
            vibratorManager?.defaultVibrator ?: context.getSystemService(Vibrator::class.java)!!
        } else {
            context.getSystemService(Vibrator::class.java)!!
        }
    }

    val isButtonHapticsEnabled: Boolean
        get() = sharedPref.getBoolean(PREF_BUTTON_HAPTICS_ENABLED, true)

    val isOfflineHapticsEnabled: Boolean
        get() = sharedPref.getBoolean(PREF_HAPTICS_OFFLINE_ENABLED, false)

    val clickStrength: Float
        get() = sharedPref.getFloat(PREF_HAPTICS_CLICK_STRENGTH, 0.1f).coerceIn(0.1f, 1.0f)

    val hapticStyle: String
        get() = sharedPref.getString(PREF_HAPTICS_STYLE, STYLE_SOFT) ?: STYLE_SOFT

    /**
     * Checks whether button vibration should fire given the current connection and rumble state.
     * Prevents button clicks from overriding active PC game motor rumble.
     */
    fun canVibrate(isConnected: Boolean, isRumbling: Boolean): Boolean {
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
    fun performPreviewClick(style: String = hapticStyle, strength: Float = clickStrength) {
        triggerImpulse(style, strength)
    }

    private fun triggerImpulse(style: String, rawStrength: Float) {
        val strength = rawStrength.coerceIn(0.1f, 1.0f)

        try {
            // Modern Android 11+ (API 30+) Rich Haptic Composition
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val primitive = when (style) {
                    STYLE_HEAVY -> VibrationEffect.Composition.PRIMITIVE_THUD
                    STYLE_SOFT -> VibrationEffect.Composition.PRIMITIVE_TICK
                    else -> VibrationEffect.Composition.PRIMITIVE_CLICK
                }

                val isSupported = try {
                    vibrator.areAllPrimitivesSupported(primitive)
                } catch (_: Exception) {
                    false
                }

                if (isSupported) {
                    val composition = VibrationEffect.startComposition()
                        .addPrimitive(primitive, strength)
                        .compose()
                    vibrator.vibrate(composition)
                    return
                }
            }

            // Android 10+ (API 29+) Predefined Click Fallback
            val predefinedEffect = when (style) {
                STYLE_HEAVY -> VibrationEffect.EFFECT_HEAVY_CLICK
                STYLE_SOFT -> VibrationEffect.EFFECT_TICK
                else -> VibrationEffect.EFFECT_CLICK
            }
            vibrator.vibrate(VibrationEffect.createPredefined(predefinedEffect))
        } catch (_: Exception) {
            // Precision micro-impulse fallback (12ms) with amplitude scaling
            try {
                val amplitude = (strength * 255).toInt().coerceIn(1, 255)
                vibrator.vibrate(VibrationEffect.createOneShot(12, amplitude))
            } catch (_: Exception) {
                // Motor unavailable or hardware busy
            }
        }
    }

    companion object {
        const val PREF_FILE = "nexpad_prefs"
        const val PREF_BUTTON_HAPTICS_ENABLED = "BUTTON_HAPTICS_ENABLED"
        const val PREF_HAPTICS_OFFLINE_ENABLED = "HAPTICS_OFFLINE_ENABLED"
        const val PREF_HAPTICS_CLICK_STRENGTH = "HAPTICS_CLICK_STRENGTH"
        const val PREF_HAPTICS_STYLE = "HAPTICS_STYLE"

        const val STYLE_CRISP = "crisp"
        const val STYLE_HEAVY = "heavy"
        const val STYLE_SOFT = "soft"
    }
}
