package com.sanket.tools.nexpad.model

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
 * Clean UI badge display name for ControllerLabelStyle ("Xbox" vs "PS").
 */
val com.sanket.tools.nexpad.category.ControllerLabelStyle.shortDisplayName: String
    get() = when (this) {
        com.sanket.tools.nexpad.category.ControllerLabelStyle.XBOX -> "Xbox"
        com.sanket.tools.nexpad.category.ControllerLabelStyle.PLAYSTATION -> "PS"
    }
