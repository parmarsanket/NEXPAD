package com.sanket.tools.nexpad.model

import com.sanket.tools.nexpad.category.ControlKey
import com.sanket.tools.nexpad.model.NexpadKeys as K
import kotlinx.serialization.Serializable

@Serializable
data class LayoutProfile(
    val name: String = "Standard Elite",
    val isDefault: Boolean = false,
    val isRgbEnabled: Boolean = true,
    val positions: Map<String, Position> = standardElitePositions(),
    val description: String = ""
) {
    /** Returns positions with all keys normalized to canonical ControlKey identifiers. */
    fun canonicalPositions(): Map<String, Position> {
        val normalized = mutableMapOf<String, Position>()
        positions.forEach { (rawKey, pos) ->
            val canonicalKey = ControlKey.fromIdentifier(rawKey)?.key ?: rawKey.uppercase()
            if (!normalized.containsKey(canonicalKey)) {
                normalized[canonicalKey] = pos
            }
        }
        return normalized
    }

    fun toHudElements(): List<HudElement> = canonicalPositions().map { (key, pos) ->
        HudElement.fromPosition(key, pos)
    }

    fun withUpdatedElements(elements: List<HudElement>): LayoutProfile = copy(
        positions = elements.associate {
            val canonical = ControlKey.fromIdentifier(it.controlKey)?.key ?: it.controlKey.uppercase()
            canonical to it.toPosition()
        }
    )
}

@Serializable
data class Position(
    val xRatio: Float,
    val yRatio: Float,
    val scale: Float = 1.0f,
    val opacity: Float = 1.0f,
    val customComponentId: String? = null
)

/** Default Layout 1: Standard Elite matching physical Xbox ergonomics (Aspect-Ratio Corrected). */
fun standardElitePositions(): Map<String, Position> {
    val positions = mutableMapOf(
        // Triggers and Bumpers (Top Corners)
        K.LT to Position(0.080f, 0.055f, scale = 1.18f),
        K.LB to Position(0.080f, 0.375f, scale = 0.95f),
        K.RT to Position(0.920f, 0.055f, scale = 1.18f),
        K.RB to Position(0.920f, 0.375f, scale = 0.95f),

        // Left Stick & D-Pad (Cross & Discrete Buttons)
        K.LS   to Position(0.115f, 0.740f, scale = 1.05f),
        K.DPAD to Position(0.320f, 0.740f, scale = 1.10f),
        K.UP   to Position(0.320f, 0.650f, scale = 0.85f),
        K.DOWN to Position(0.320f, 0.830f, scale = 0.85f),
        K.LEFT to Position(0.260f, 0.740f, scale = 0.85f),
        K.RIGHT to Position(0.380f, 0.740f, scale = 0.85f),

        // Right Stick
        K.RS to Position(0.895f, 0.740f, scale = 1.05f),

        // Center System Cluster
        K.GUIDE to Position(0.500f, 0.080f, scale = 1.15f),
        K.BACK  to Position(0.430f, 0.230f, scale = 0.70f),
        K.START to Position(0.500f, 0.230f, scale = 0.70f),
        K.SHARE to Position(0.570f, 0.230f, scale = 0.70f),

        // Center Macro Cluster
        K.M2 to Position(0.380f, 0.360f, scale = 0.75f),
        K.M4 to Position(0.460f, 0.360f, scale = 0.75f),
        K.M3 to Position(0.540f, 0.360f, scale = 0.75f),
        K.M1 to Position(0.620f, 0.360f, scale = 0.75f)
    )

    // Face Buttons: Aspect-ratio corrected isotropic diamond cluster
    // Center: (0.675f, 0.720f), Radius: 53 dp, Scale: 0.82f -> Adjacent gap ~9.5 dp, zero overlap
    positions.putAll(
        LayoutMetrics.createDiamondCluster(
            centerX = 0.675f,
            centerY = 0.720f,
            radiusDp = 53.0f,
            scale = 0.82f
        )
    )

    return positions
}

/** Backward compatible alias for default positions. */
fun defaultPositions(): Map<String, Position> = standardElitePositions()

/** Default Layout 2: FPS Tactical Pro with quick triggers, elevated sticks & macro paddles. */
fun fpsTacticalPositions(): Map<String, Position> {
    val positions = mutableMapOf(
        K.LT   to Position(0.080f, 0.055f, scale = 1.18f),
        K.LB   to Position(0.080f, 0.375f, scale = 0.95f),
        K.RT   to Position(0.920f, 0.055f, scale = 1.18f),
        K.RB   to Position(0.920f, 0.375f, scale = 0.95f),
        K.LS   to Position(0.120f, 0.680f, scale = 1.10f),
        K.RS   to Position(0.880f, 0.680f, scale = 1.10f),
        K.DPAD to Position(0.330f, 0.760f, scale = 1.05f),
        K.M3   to Position(0.260f, 0.400f, scale = 0.80f),
        K.M4   to Position(0.340f, 0.400f, scale = 0.80f),
        K.M2   to Position(0.660f, 0.400f, scale = 0.80f),
        K.M1   to Position(0.740f, 0.400f, scale = 0.80f),
        K.GUIDE to Position(0.500f, 0.080f, scale = 1.15f),
        K.BACK  to Position(0.440f, 0.220f, scale = 0.70f),
        K.START to Position(0.560f, 0.220f, scale = 0.70f)
    )
    positions.putAll(
        LayoutMetrics.createDiamondCluster(
            centerX = 0.680f,
            centerY = 0.760f,
            radiusDp = 50.0f,
            scale = 0.78f
        )
    )
    return positions
}

/** Default Layout 3: MOBA & Action RPG with curved ability arc and skill shortcuts. */
fun mobaActionPositions(): Map<String, Position> = mapOf(
    K.LT   to Position(0.080f, 0.055f, scale = 1.18f),
    K.LB   to Position(0.080f, 0.375f, scale = 0.95f),
    K.RT   to Position(0.920f, 0.055f, scale = 1.18f),
    K.RB   to Position(0.920f, 0.375f, scale = 0.95f),
    K.LS   to Position(0.120f, 0.720f, scale = 1.15f),
    K.DPAD to Position(0.320f, 0.720f, scale = 1.05f),
    K.A    to Position(0.730f, 0.800f, scale = 0.90f),
    K.X    to Position(0.620f, 0.740f, scale = 0.82f),
    K.Y    to Position(0.660f, 0.560f, scale = 0.82f),
    K.B    to Position(0.760f, 0.600f, scale = 0.85f),
    K.RS   to Position(0.895f, 0.720f, scale = 1.05f),
    K.M4   to Position(0.360f, 0.360f, scale = 0.75f),
    K.M1   to Position(0.450f, 0.360f, scale = 0.75f),
    K.M2   to Position(0.550f, 0.360f, scale = 0.75f),
    K.M3   to Position(0.640f, 0.360f, scale = 0.75f),
    K.GUIDE to Position(0.500f, 0.080f, scale = 1.15f),
    K.BACK  to Position(0.430f, 0.220f, scale = 0.70f),
    K.START to Position(0.570f, 0.220f, scale = 0.70f)
)

/** Default Layout 4: Racing & Simulation with wide analog triggers and paddle shifters. */
fun racingSimPositions(): Map<String, Position> {
    val positions = mutableMapOf(
        K.LT   to Position(0.080f, 0.055f, scale = 1.18f),
        K.LB   to Position(0.080f, 0.375f, scale = 0.95f),
        K.RT   to Position(0.920f, 0.055f, scale = 1.18f),
        K.RB   to Position(0.920f, 0.375f, scale = 0.95f),
        K.LS   to Position(0.120f, 0.740f, scale = 1.15f),
        K.DPAD to Position(0.320f, 0.740f, scale = 1.05f),
        K.RS   to Position(0.895f, 0.740f, scale = 1.10f),
        K.M1   to Position(0.360f, 0.380f, scale = 0.75f),
        K.M2   to Position(0.450f, 0.380f, scale = 0.75f),
        K.M3   to Position(0.550f, 0.380f, scale = 0.75f),
        K.M4   to Position(0.640f, 0.380f, scale = 0.75f),
        K.GUIDE to Position(0.500f, 0.080f, scale = 1.15f),
        K.BACK  to Position(0.430f, 0.220f, scale = 0.70f),
        K.START to Position(0.570f, 0.220f, scale = 0.70f)
    )
    positions.putAll(
        LayoutMetrics.createDiamondCluster(
            centerX = 0.675f,
            centerY = 0.720f,
            radiusDp = 52.0f,
            scale = 0.82f
        )
    )
    return positions
}

/** Default Layout 5: Retro Arcade & Fighter with 6-button fightstick grid and 8-way D-Pad. */
fun retroArcadePositions(): Map<String, Position> = mapOf(
    K.LT   to Position(0.080f, 0.055f, scale = 1.18f),
    K.LB   to Position(0.080f, 0.375f, scale = 0.95f),
    K.DPAD to Position(0.140f, 0.720f, scale = 1.15f),
    K.LS   to Position(0.340f, 0.720f, scale = 1.05f),
    K.X    to Position(0.580f, 0.550f, scale = 0.82f),
    K.Y    to Position(0.690f, 0.520f, scale = 0.82f),
    K.RB   to Position(0.820f, 0.500f, scale = 0.75f),
    K.A    to Position(0.580f, 0.780f, scale = 0.82f),
    K.B    to Position(0.690f, 0.750f, scale = 0.82f),
    K.RT   to Position(0.820f, 0.820f, scale = 0.75f),
    K.RS   to Position(0.895f, 0.260f, scale = 0.85f),
    K.M1   to Position(0.360f, 0.360f, scale = 0.75f),
    K.M2   to Position(0.460f, 0.360f, scale = 0.75f),
    K.BACK  to Position(0.430f, 0.180f, scale = 0.75f),
    K.START to Position(0.570f, 0.180f, scale = 0.75f),
    K.GUIDE to Position(0.500f, 0.080f, scale = 1.15f)
)

/** Returns the 5 non-deletable default layout profiles. */
fun getDefaultLayoutProfiles(): List<LayoutProfile> = listOf(
    LayoutProfile(
        name = "Standard Elite",
        isDefault = true,
        positions = standardElitePositions(),
        description = "Precision Xbox layout with dual triggers, bumpers, and center macro cluster."
    ),
    LayoutProfile(
        name = "FPS Tactical Pro",
        isDefault = true,
        positions = fpsTacticalPositions(),
        description = "Instant hair-trigger response, elevated sticks, and quick slide/jump paddles."
    ),
    LayoutProfile(
        name = "MOBA & Action RPG",
        isDefault = true,
        positions = mobaActionPositions(),
        description = "Ergonomic ability attack arc, targeted skillshots, and quick item macros."
    ),
    LayoutProfile(
        name = "Racing & Simulation",
        isDefault = true,
        positions = racingSimPositions(),
        description = "Large analog throttle & brake triggers, steering stick, and paddle shifters."
    ),
    LayoutProfile(
        name = "Retro Arcade & Fighter",
        isDefault = true,
        positions = retroArcadePositions(),
        description = "Classic 6-button arcade fightstick grid with 8-way directional D-pad."
    )
)
