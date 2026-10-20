package com.sanket.tools.nexpad.utils

/**
 * Centralized, strongly-typed repository of SharedPreferences file names and keys across NEXPAD.
 * Eliminates magic strings, prevents key typos, and provides type-safe key generators and default constants.
 */
object NexpadPreferenceKeys {

    // SharedPreferences Files
    const val FILE_LAYOUTS = "NEXPAD_LAYOUTS_V3"
    const val FILE_PREFS = "nexpad_prefs"
    const val FILE_CONNECTION_PREFS = "nexpad_connection_prefs"

    // Layout Preferences Keys (FILE_LAYOUTS)
    const val KEY_ACTIVE_PROFILE = "active_profile"
    const val KEY_CUSTOM_PROFILE_NAMES = "custom_profile_names"
    const val KEY_PROFILE_ORDER = "profile_order"
    const val KEY_LAYOUT_MIGRATION_VERSION = "layout_migration_version"
    const val DEFAULT_LAYOUT_MIGRATION_VERSION = 0

    const val PREFIX_PROFILE = "profile_"

    /** Key generator for serialized layout JSON in FILE_LAYOUTS. */
    fun profileKey(name: String): String = "$PREFIX_PROFILE${name.trim()}"

    /** Checks if a preference key represents a serialized profile layout JSON. */
    fun isProfileKey(key: String): Boolean = key.startsWith(PREFIX_PROFILE)

    /** Extracts the profile name from a profile preference key. */
    fun extractProfileName(key: String): String = key.substringAfter(PREFIX_PROFILE).trim()

    // Controller & Sensitivity Keys (FILE_PREFS)
    const val KEY_RIGHT_STICK_CAMERA_MODE = "RIGHT_STICK_CAMERA_MODE"
    const val KEY_CAMERA_SENSITIVITY = "CAMERA_SENSITIVITY"
    const val DEFAULT_CAMERA_SENSITIVITY = 1.0f

    const val KEY_TOUCHPAD_SENSITIVITY = "TOUCHPAD_SENSITIVITY"
    fun touchpadSensitivityKey(padKey: String): String = "TOUCHPAD_SENSITIVITY_${padKey.uppercase()}"
    const val KEY_TOUCHPAD_CURVE = "TOUCHPAD_CURVE"
    fun touchpadCurveKey(padKey: String): String = "TOUCHPAD_CURVE_${padKey.uppercase()}"

    // Haptics & Vibration Keys (FILE_PREFS)
    const val KEY_BUTTON_HAPTICS_ENABLED = "BUTTON_HAPTICS_ENABLED"
    const val KEY_HAPTICS_OFFLINE_ENABLED = "HAPTICS_OFFLINE_ENABLED"
    const val KEY_HAPTICS_CLICK_STRENGTH = "HAPTICS_CLICK_STRENGTH"
    const val DEFAULT_HAPTICS_CLICK_STRENGTH = 0.3f
    const val KEY_HAPTICS_STYLE = "HAPTICS_STYLE"
    val DEFAULT_HAPTICS_STYLE = com.sanket.tools.nexpad.model.HapticStyle.DEFAULT.id

    const val KEY_RUMBLE_MODE = "RUMBLE_MODE"
    val DEFAULT_RUMBLE_MODE = com.sanket.tools.nexpad.model.RumbleMode.DEFAULT.id
    const val RUMBLE_MODE_MIN = "min"
    const val RUMBLE_MODE_MAX = "max"
    const val RUMBLE_MODE_SMART = "smart"
    const val RUMBLE_MODE_AVG = "avg"

    // Navigation & Emulation Keys (FILE_PREFS)
    const val KEY_DOUBLE_TAP_BACK_EXIT = "DOUBLE_TAP_BACK_EXIT"
    const val KEY_JOYSTICK_SLIDE_GUARD_ENABLED = "JOYSTICK_SLIDE_GUARD_ENABLED"
    const val KEY_ESPORTS_MULTITOUCH_ENABLED = "ESPORTS_MULTITOUCH_ENABLED"
    const val KEY_EMULATION_MODE = "emulation_mode"
    val DEFAULT_EMULATION_MODE = com.sanket.tools.nexpad.model.ControllerEmulationMode.DEFAULT.id
    const val EMULATION_MODE_GENERIC = "generic"
    const val EMULATION_MODE_XBOX = "xbox"
    const val EMULATION_MODE_DS4 = "ds4"

    // Connection Keys (FILE_CONNECTION_PREFS)
    const val KEY_LAST_MANUAL_IP = "last_manual_ip"
    const val KEY_LAST_MANUAL_PORT = "last_manual_port"
    const val DEFAULT_MANUAL_PORT = "9999"
    const val KEY_COMMUNICATION_PORT = "communication_port"
    const val DEFAULT_COMMUNICATION_PORT = 9999
}
