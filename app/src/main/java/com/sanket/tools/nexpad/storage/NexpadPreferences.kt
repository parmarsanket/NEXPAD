package com.sanket.tools.nexpad.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.sanket.tools.nexpad.utils.HapticFeedbackHelper
import com.sanket.tools.nexpad.utils.NexpadPreferenceKeys
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Android-side Centralized Preferences Repository for NEXPAD.
 *
 * Mirrors `DesktopSharedPreferences` on NEXPAD Desktop:
 * - Single point of truth with a private constructor and thread-safe singleton.
 * - High-level typed domain properties and methods.
 * - Native Kotlin StateFlow streams for reactive UI observation.
 * - Completely hides raw SharedPreferences keys and files from UI composables and ViewModels.
 */
class NexpadPreferences private constructor(context: Context) {

    private val appContext = context.applicationContext

    private val prefs: SharedPreferences = appContext.getSharedPreferences(
        NexpadPreferenceKeys.FILE_PREFS,
        Context.MODE_PRIVATE
    )

    private val connectionPrefs: SharedPreferences = appContext.getSharedPreferences(
        NexpadPreferenceKeys.FILE_CONNECTION_PREFS,
        Context.MODE_PRIVATE
    )

    // ─── Reactive StateFlows for Compose UI ─────────────────────────────

    private val _cameraSensitivityFlow = MutableStateFlow(cameraSensitivity)
    val cameraSensitivityFlow: StateFlow<Float> = _cameraSensitivityFlow.asStateFlow()

    private val _rumbleModeFlow = MutableStateFlow(rumbleMode)
    val rumbleModeFlow: StateFlow<com.sanket.tools.nexpad.model.RumbleMode> = _rumbleModeFlow.asStateFlow()

    private val _emulationModeFlow = MutableStateFlow(emulationMode)
    val emulationModeFlow: StateFlow<com.sanket.tools.nexpad.model.ControllerEmulationMode> = _emulationModeFlow.asStateFlow()

    private val _isJoystickSlideGuardEnabledFlow = MutableStateFlow(isJoystickSlideGuardEnabled)
    val isJoystickSlideGuardEnabledFlow: StateFlow<Boolean> = _isJoystickSlideGuardEnabledFlow.asStateFlow()

    private val _isEsportsMultitouchEnabledFlow = MutableStateFlow(isEsportsMultitouchEnabled)
    val isEsportsMultitouchEnabledFlow: StateFlow<Boolean> = _isEsportsMultitouchEnabledFlow.asStateFlow()

    // ─── Controller & Analog Stick Domain APIs ──────────────────────────

    var isRightStickCameraMode: Boolean
        get() = prefs.getBoolean(NexpadPreferenceKeys.KEY_RIGHT_STICK_CAMERA_MODE, false)
        set(value) = prefs.edit { putBoolean(NexpadPreferenceKeys.KEY_RIGHT_STICK_CAMERA_MODE, value) }

    var cameraSensitivity: Float
        get() = prefs.getFloat(NexpadPreferenceKeys.KEY_CAMERA_SENSITIVITY, NexpadPreferenceKeys.DEFAULT_CAMERA_SENSITIVITY)
        set(value) {
            prefs.edit { putFloat(NexpadPreferenceKeys.KEY_CAMERA_SENSITIVITY, value) }
            _cameraSensitivityFlow.value = value
        }

    fun getTouchpadSensitivity(padKey: String): Float {
        val specificSens = prefs.getFloat(NexpadPreferenceKeys.touchpadSensitivityKey(padKey), -1f)
        if (specificSens > 0f) return specificSens
        val globalSens = prefs.getFloat(NexpadPreferenceKeys.KEY_TOUCHPAD_SENSITIVITY, -1f)
        if (globalSens > 0f) return globalSens
        return cameraSensitivity * 2.0f
    }

    fun setTouchpadSensitivity(padKey: String, sensitivity: Float) {
        val upperKey = padKey.uppercase()
        prefs.edit {
            putFloat(NexpadPreferenceKeys.touchpadSensitivityKey(upperKey), sensitivity)
            putFloat(NexpadPreferenceKeys.KEY_TOUCHPAD_SENSITIVITY, sensitivity)
        }
    }

    fun getTouchpadCurve(padKey: String): com.sanket.tools.nexpad.model.TouchpadCurve {
        val specific = prefs.getString(NexpadPreferenceKeys.touchpadCurveKey(padKey), null)
        if (!specific.isNullOrBlank()) return com.sanket.tools.nexpad.model.TouchpadCurve.fromId(specific)
        val global = prefs.getString(NexpadPreferenceKeys.KEY_TOUCHPAD_CURVE, null)
        if (!global.isNullOrBlank()) return com.sanket.tools.nexpad.model.TouchpadCurve.fromId(global)
        return com.sanket.tools.nexpad.model.TouchpadCurve.DEFAULT
    }

    fun setTouchpadCurve(padKey: String, curve: com.sanket.tools.nexpad.model.TouchpadCurve) {
        val upperKey = padKey.uppercase()
        prefs.edit {
            putString(NexpadPreferenceKeys.touchpadCurveKey(upperKey), curve.id)
            putString(NexpadPreferenceKeys.KEY_TOUCHPAD_CURVE, curve.id)
        }
    }

    // ─── Rumble & Navigation Domain APIs ────────────────────────────────

    var rumbleMode: com.sanket.tools.nexpad.model.RumbleMode
        get() = com.sanket.tools.nexpad.model.RumbleMode.fromId(prefs.getString(NexpadPreferenceKeys.KEY_RUMBLE_MODE, NexpadPreferenceKeys.DEFAULT_RUMBLE_MODE))
        set(value) {
            prefs.edit { putString(NexpadPreferenceKeys.KEY_RUMBLE_MODE, value.id) }
            _rumbleModeFlow.value = value
        }

    var isDoubleTapExitEnabled: Boolean
        get() = prefs.getBoolean(NexpadPreferenceKeys.KEY_DOUBLE_TAP_BACK_EXIT, true)
        set(value) = prefs.edit { putBoolean(NexpadPreferenceKeys.KEY_DOUBLE_TAP_BACK_EXIT, value) }

    var isJoystickSlideGuardEnabled: Boolean
        get() = prefs.getBoolean(NexpadPreferenceKeys.KEY_JOYSTICK_SLIDE_GUARD_ENABLED, true)
        set(value) {
            prefs.edit { putBoolean(NexpadPreferenceKeys.KEY_JOYSTICK_SLIDE_GUARD_ENABLED, value) }
            _isJoystickSlideGuardEnabledFlow.value = value
        }

    var isEsportsMultitouchEnabled: Boolean
        get() = prefs.getBoolean(NexpadPreferenceKeys.KEY_ESPORTS_MULTITOUCH_ENABLED, true)
        set(value) {
            prefs.edit { putBoolean(NexpadPreferenceKeys.KEY_ESPORTS_MULTITOUCH_ENABLED, value) }
            _isEsportsMultitouchEnabledFlow.value = value
        }

    var emulationMode: com.sanket.tools.nexpad.model.ControllerEmulationMode
        get() = com.sanket.tools.nexpad.model.ControllerEmulationMode.fromId(prefs.getString(NexpadPreferenceKeys.KEY_EMULATION_MODE, NexpadPreferenceKeys.DEFAULT_EMULATION_MODE))
        set(value) {
            prefs.edit { putString(NexpadPreferenceKeys.KEY_EMULATION_MODE, value.id) }
            _emulationModeFlow.value = value
        }

    // ─── Haptics Engine Domain APIs ─────────────────────────────────────

    var isButtonHapticsEnabled: Boolean
        get() = prefs.getBoolean(NexpadPreferenceKeys.KEY_BUTTON_HAPTICS_ENABLED, true)
        set(value) = prefs.edit { putBoolean(NexpadPreferenceKeys.KEY_BUTTON_HAPTICS_ENABLED, value) }

    var isHapticsOfflineEnabled: Boolean
        get() = prefs.getBoolean(NexpadPreferenceKeys.KEY_HAPTICS_OFFLINE_ENABLED, false)
        set(value) = prefs.edit { putBoolean(NexpadPreferenceKeys.KEY_HAPTICS_OFFLINE_ENABLED, value) }

    var hapticClickStrength: Float
        get() = prefs.getFloat(NexpadPreferenceKeys.KEY_HAPTICS_CLICK_STRENGTH, NexpadPreferenceKeys.DEFAULT_HAPTICS_CLICK_STRENGTH)
        set(value) = prefs.edit { putFloat(NexpadPreferenceKeys.KEY_HAPTICS_CLICK_STRENGTH, value) }

    var hapticStyle: com.sanket.tools.nexpad.model.HapticStyle
        get() = com.sanket.tools.nexpad.model.HapticStyle.fromId(prefs.getString(NexpadPreferenceKeys.KEY_HAPTICS_STYLE, NexpadPreferenceKeys.DEFAULT_HAPTICS_STYLE))
        set(value) = prefs.edit { putString(NexpadPreferenceKeys.KEY_HAPTICS_STYLE, value.id) }

    // ─── Direct IP & Network Port Domain APIs ───────────────────────────

    var defaultCommunicationPort: Int
        get() = prefs.getInt(NexpadPreferenceKeys.KEY_COMMUNICATION_PORT, NexpadPreferenceKeys.DEFAULT_COMMUNICATION_PORT)
        set(value) {
            prefs.edit { putInt(NexpadPreferenceKeys.KEY_COMMUNICATION_PORT, value) }
            lastManualPort = value.toString()
        }

    var lastManualIp: String
        get() = connectionPrefs.getString(NexpadPreferenceKeys.KEY_LAST_MANUAL_IP, "") ?: ""
        set(value) = connectionPrefs.edit { putString(NexpadPreferenceKeys.KEY_LAST_MANUAL_IP, value) }

    var lastManualPort: String
        get() = connectionPrefs.getString(NexpadPreferenceKeys.KEY_LAST_MANUAL_PORT, defaultCommunicationPort.toString())
            ?: defaultCommunicationPort.toString()
        set(value) = connectionPrefs.edit { putString(NexpadPreferenceKeys.KEY_LAST_MANUAL_PORT, value) }

    companion object {
        @Volatile
        private var instance: NexpadPreferences? = null

        /**
         * Returns the application-wide singleton instance of NexpadPreferences.
         */
        fun getInstance(context: Context): NexpadPreferences {
            return instance ?: synchronized(this) {
                instance ?: NexpadPreferences(context).also { instance = it }
            }
        }
    }
}
