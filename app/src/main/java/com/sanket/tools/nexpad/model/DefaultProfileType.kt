package com.sanket.tools.nexpad.model

/**
 * Strongly-typed enumeration of factory-provided layout profiles.
 * Encapsulates canonical profile names, display titles, and legacy aliases.
 */
enum class DefaultProfileType(
    val profileName: String,
    val legacyAlias: String? = null
) {
    STANDARD_ELITE("Standard Elite", legacyAlias = "Standard"),
    PLAYSTATION_DUALSENSE_PRO("PlayStation DualSense Pro"),
    FPS_TACTICAL_CLAW("FPS Tactical Claw"),
    RETRO_ARCADE_FIGHTSTICK("Retro Arcade Fightstick"),
    SIM_RACING_FLIGHT("Sim Racing & Flight"),
    GRAND_MOBA_RPG("Grand MOBA & RPG");

    companion object {
        /** The factory default fallback profile for fresh installs, resets, and deleted actives. */
        val DEFAULT = STANDARD_ELITE

        /** Set of lowercase canonical names and legacy aliases for O(1) membership checks. */
        val CANONICAL_NAMES: Set<String> by lazy {
            entries.flatMap { listOfNotNull(it.profileName.lowercase(), it.legacyAlias?.lowercase()) }.toSet()
        }

        /**
         * Resolves any raw profile name into its canonical equivalent.
         * Maps legacy aliases (e.g. "Standard" -> "Standard Elite") and trims whitespace.
         * If the profile is custom (not a factory default), returns the trimmed name.
         * If null or blank, returns the default profile name.
         */
        fun resolveCanonicalName(rawName: String?): String {
            if (rawName.isNullOrBlank()) return DEFAULT.profileName
            val trimmed = rawName.trim()
            val match = entries.firstOrNull {
                it.profileName.equals(trimmed, ignoreCase = true) ||
                it.legacyAlias.equals(trimmed, ignoreCase = true)
            }
            return match?.profileName ?: trimmed
        }

        /**
         * Checks whether the given name matches any factory default profile name or legacy alias.
         */
        fun isDefaultProfile(name: String?): Boolean {
            if (name.isNullOrBlank()) return false
            return CANONICAL_NAMES.contains(name.trim().lowercase())
        }
    }
}
