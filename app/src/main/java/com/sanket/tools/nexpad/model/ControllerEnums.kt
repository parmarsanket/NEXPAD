package com.sanket.tools.nexpad.model

import com.sanket.tools.nexpad.category.CategoryType
import com.sanket.tools.nexpad.category.ComponentType
import com.sanket.tools.nexpad.category.ControlKey
import com.sanket.tools.nexpad.model.NexpadKeys as K

/**
 * Controller emulation targets supported by NEXPAD server/desktop bridge.
 */
enum class ControllerEmulationMode(
    val id: String,
    val displayName: String,
    val description: String
) {
    GENERIC("generic", "Generic Controller", "Standard XInput / DirectInput baseline"),
    XBOX("xbox", "Xbox 360 Controller", "Full Microsoft XInput gamepad emulation"),
    DUALSENSE("ds4", "DualSense / DualShock 4", "PlayStation DS4 HID emulation with touchpad & motion");

    companion object {
        val DEFAULT = GENERIC

        fun fromId(id: String?): ControllerEmulationMode {
            if (id.isNullOrBlank()) return DEFAULT
            val clean = id.trim().lowercase()
            return entries.firstOrNull { it.id == clean } ?: DEFAULT
        }
    }
}

/**
 * Controller motor blending mode for PC game force telemetry.
 */
enum class RumbleMode(
    val id: String,
    val title: String,
    val subtitle: String
) {
    MIN("min", "Soft Min", "Subtle Floor (Default)"),
    MAX("max", "Peak Force", "1:1 Peak Impact"),
    SMART("smart", "Smart Intent", "Dynamic Stereo Blend"),
    AVG("avg", "Balanced Average", "50/50 Channel Mean");

    companion object {
        val DEFAULT = MIN

        fun fromId(id: String?): RumbleMode {
            if (id.isNullOrBlank()) return DEFAULT
            val clean = id.trim().lowercase()
            return entries.firstOrNull { it.id == clean } ?: DEFAULT
        }
    }
}

/**
 * Haptic mechanical switch synthesis profiles for button clicks.
 */
enum class HapticStyle(
    val id: String,
    val title: String,
    val subtitle: String
) {
    CRISP("crisp", "Crisp Click", "Fast & Sharp"),
    HEAVY("heavy", "Heavy Snap", "Tactile Bump"),
    SOFT("soft", "Soft Tick", "Quiet Tone");

    companion object {
        val DEFAULT = CRISP

        fun fromId(id: String?): HapticStyle {
            if (id.isNullOrBlank()) return DEFAULT
            val clean = id.trim().lowercase()
            return entries.firstOrNull { it.id == clean } ?: DEFAULT
        }
    }
}

/**
 * Analog stick touch tracking and boundary behavior modes.
 */
enum class JoystickMode(
    val id: String,
    val displayName: String
) {
    LOCKED("LOCKED", "Locked"),
    BOX("BOX", "Box Zone"),
    FULL("FULL", "Full Screen");

    companion object {
        val DEFAULT = LOCKED

        fun fromId(id: String?): JoystickMode {
            if (id.isNullOrBlank()) return DEFAULT
            val clean = id.trim().uppercase()
            return entries.firstOrNull { it.id == clean } ?: DEFAULT
        }
    }
}

/**
 * Touchpad acceleration and speed response curves.
 * Mirrors gyro response curves for flexible game handling (sniping micro-aim vs fast turns).
 */
enum class TouchpadCurve(
    val id: String,
    val displayName: String
) {
    POWER("POWER", "Power Curve"),
    LINEAR("LINEAR", "Linear Curve");

    companion object {
        val DEFAULT = POWER

        fun fromId(id: String?): TouchpadCurve {
            if (id.isNullOrBlank()) return DEFAULT
            val clean = id.trim().uppercase()
            return entries.firstOrNull { it.id == clean } ?: DEFAULT
        }
    }
}

/**
 * Clean UI badge display name for ControllerLabelStyle ("Xbox" vs "PS").
 */
val com.sanket.tools.nexpad.category.ControllerLabelStyle.shortDisplayName: String
    get() = when (this) {
        com.sanket.tools.nexpad.category.ControllerLabelStyle.XBOX -> "Xbox"
        com.sanket.tools.nexpad.category.ControllerLabelStyle.PLAYSTATION -> "PS"
    }

/**
 * Canonical identifiers for native built-in components, skins, and special actuators.
 */
object BuiltinComponentIds {
    const val BUILTIN_PREFIX = "builtin."
    const val DEFAULT_FAMILY = "builtin.default"
    const val INBUILD_LTP = "builtin.inbuild_ltp"
    const val INBUILD_RTP = "builtin.inbuild_rtp"
    const val INBUILD_PREFIX = "builtin.inbuild_"
    const val DEFAULT_LTP = "builtin.default_ltp"
    const val DEFAULT_RTP = "builtin.default_rtp"
    const val DEFAULT_LT = "builtin.default_lt"
    const val DEFAULT_RT = "builtin.default_rt"
    const val DEFAULT_LB = "builtin.default_lb"
    const val DEFAULT_RB = "builtin.default_rb"
    const val DEFAULT_PREFIX = "builtin.default_"
    const val GYRO = "GYRO"
    const val GYRO_BUTTON = "builtin.gyro_btn"
    const val SLIDER_LT = "builtin.slider_lt"
    const val SLIDER_RT = "builtin.slider_rt"
    const val SLIDER_PREFIX = "builtin.slider_"

    fun isInbuildTouchpad(customComponentId: String?): Boolean =
        customComponentId != null && customComponentId.startsWith(INBUILD_PREFIX)

    fun isSliderTrigger(customComponentId: String?): Boolean =
        customComponentId != null && (
            customComponentId == SLIDER_LT ||
            customComponentId == SLIDER_RT ||
            customComponentId.startsWith(SLIDER_PREFIX) ||
            customComponentId.contains("slider", ignoreCase = true)
        )
}

/**
 * Touchpad rendering and placement classification.
 */
enum class TouchpadType(
    val id: String,
    val displayName: String
) {
    INBUILD("INBUILD", "Full-Surface Ambient Touchpad"),
    DISCRETE("DISCRETE", "Discrete Placed Touchpad");

    companion object {
        val DEFAULT = DISCRETE

        fun fromComponentId(componentId: String?): TouchpadType =
            if (BuiltinComponentIds.isInbuildTouchpad(componentId)) INBUILD else DISCRETE
    }
}

/**
 * Centralized classification and hit-testing helpers for controller touch surfaces,
 * sticks, touchpads, and sweep exclusion boundaries.
 *
 * Eliminates ad-hoc string comparisons across GamepadScreen, HUD Inspector,
 * and custom runtime interpreters.
 */
object ControllerTouchClassifier {
    /** Controls that are analog sticks, touchpads, or stick clicks (excluded from normal button tap/sweep). */
    val EXCLUDED_NON_BUTTON_KEYS: Set<String> = setOf(
        ControlKey.LS.key,
        ControlKey.RS.key,
        ControlKey.LSB.key,
        ControlKey.RSB.key,
        ControlKey.LTP.key,
        ControlKey.RTP.key,
        K.L3,
        K.R3
    )

    /** Critical system and utility buttons protected from rolling multi-touch sweeps. */
    val NON_SWEEPABLE_KEYS: Set<String> = setOf(
        ControlKey.START.key,
        ControlKey.BACK.key,
        ControlKey.GUIDE.key,
        ControlKey.SHARE.key,
        K.SELECT,
        K.MENU,
        K.HOME,
        K.CAPTURE,
        BuiltinComponentIds.GYRO
    )

    /** Returns true if the key or component corresponds to a left or right touchpad. */
    fun isTouchpad(key: String?, customComponentId: String? = null): Boolean {
        if (key.isNullOrBlank()) return false
        val ctrl = ControlKey.fromIdentifier(key)
        if (ctrl == ControlKey.LTP || ctrl == ControlKey.RTP || ctrl?.componentType == ComponentType.TOUCHPAD) {
            return true
        }
        val upper = key.uppercase()
        if (upper == ControlKey.LTP.key || upper == ControlKey.RTP.key || upper.contains("TOUCHPAD")) {
            return true
        }
        if (customComponentId != null) {
            if (BuiltinComponentIds.isInbuildTouchpad(customComponentId) ||
                customComponentId.contains("touchpad", ignoreCase = true) ||
                customComponentId.contains("ltp", ignoreCase = true) ||
                customComponentId.contains("rtp", ignoreCase = true)
            ) {
                return true
            }
        }
        return false
    }

    /** Returns true if the key or component corresponds to a left or right analog stick. */
    fun isJoystick(key: String?, customComponentId: String? = null): Boolean {
        if (key.isNullOrBlank()) return false
        val ctrl = ControlKey.fromIdentifier(key)
        if (ctrl == ControlKey.LS || ctrl == ControlKey.RS || ctrl?.componentType == ComponentType.JOYSTICK) {
            return true
        }
        val upper = key.uppercase()
        if (upper == ControlKey.LS.key || upper == ControlKey.RS.key || upper.contains("JOYSTICK") || upper.contains("STICK")) {
            return true
        }
        if (customComponentId != null && customComponentId.contains("joystick", ignoreCase = true)) {
            return true
        }
        return false
    }

    /** Returns true if the key represents a non-button element (stick or touchpad). */
    fun isNonButton(key: String): Boolean {
        val ctrl = ControlKey.fromIdentifier(key)
        if (ctrl?.componentType == ComponentType.JOYSTICK || ctrl?.componentType == ComponentType.TOUCHPAD) {
            return true
        }
        val upper = key.uppercase()
        return EXCLUDED_NON_BUTTON_KEYS.contains(upper) ||
                upper.contains("TOUCHPAD") ||
                upper.contains("STICK")
    }

    /** Returns true if the key or customComponentId represents the Left Touchpad (LTP). */
    fun isLeftTouchpad(key: String?, customComponentId: String? = null): Boolean {
        if (key != null) {
            val ctrl = ControlKey.fromIdentifier(key)
            if (ctrl == ControlKey.LTP || key.equals(ControlKey.LTP.key, ignoreCase = true)) return true
        }
        if (customComponentId != null) {
            if (customComponentId == BuiltinComponentIds.INBUILD_LTP ||
                customComponentId == BuiltinComponentIds.DEFAULT_LTP ||
                customComponentId.contains("ltp", ignoreCase = true)
            ) return true
        }
        return false
    }

    /** Returns true if the key or customComponentId represents the Right Touchpad (RTP). */
    fun isRightTouchpad(key: String?, customComponentId: String? = null): Boolean {
        if (key != null) {
            val ctrl = ControlKey.fromIdentifier(key)
            if (ctrl == ControlKey.RTP || key.equals(ControlKey.RTP.key, ignoreCase = true)) return true
        }
        if (customComponentId != null) {
            if (customComponentId == BuiltinComponentIds.INBUILD_RTP ||
                customComponentId == BuiltinComponentIds.DEFAULT_RTP ||
                customComponentId.contains("rtp", ignoreCase = true)
            ) return true
        }
        return false
    }

    /** Returns true if the key is non-sweepable (system/menu key). */
    fun isNonSweepable(key: String): Boolean =
        NON_SWEEPABLE_KEYS.contains(key.uppercase()) ||
        ControlKey.fromIdentifier(key)?.categoryType == CategoryType.SYSTEM

    /** Returns true if the customComponentId represents an analog slider trigger (e.g. SLIDER_LT / SLIDER_RT). */
    fun isSliderTrigger(customComponentId: String?): Boolean =
        BuiltinComponentIds.isSliderTrigger(customComponentId)
}

val ControlKey.isTouchpad: Boolean
    get() = this == ControlKey.LTP || this == ControlKey.RTP || this.componentType == ComponentType.TOUCHPAD

val ControlKey.isJoystick: Boolean
    get() = this == ControlKey.LS || this == ControlKey.RS || this.componentType == ComponentType.JOYSTICK

