package com.sanket.tools.nexpad.model

import kotlinx.serialization.Serializable

@Serializable
data class LayoutProfile(
    val name: String = "Standard Elite",
    val isDefault: Boolean = false,
    val isRgbEnabled: Boolean = true,
    val positions: Map<String, Position> = standardElitePositions(),
    val description: String = ""
)

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
        "LT" to Position(0.080f, 0.055f, scale = 1.18f),
        "LB" to Position(0.080f, 0.375f, scale = 0.95f),
        "RT" to Position(0.920f, 0.055f, scale = 1.18f),
        "RB" to Position(0.920f, 0.375f, scale = 0.95f),

        // Left Thumbstick & D-Pad
        "LS" to Position(0.115f, 0.740f, scale = 1.05f),
        "DPAD" to Position(0.320f, 0.740f, scale = 1.10f),

        // Right Stick
        "RS" to Position(0.895f, 0.740f, scale = 1.05f),

        // Center System Cluster
        "XBOX" to Position(0.500f, 0.080f, scale = 1.15f),
        "VIEW" to Position(0.430f, 0.220f, scale = 0.70f),
        "MENU" to Position(0.500f, 0.220f, scale = 0.70f),
        "SHARE" to Position(0.570f, 0.220f, scale = 0.70f),

        // Center Macro Cluster
        "M2" to Position(0.380f, 0.350f, scale = 0.75f),
        "M4" to Position(0.460f, 0.350f, scale = 0.75f),
        "M3" to Position(0.540f, 0.350f, scale = 0.75f),
        "M1" to Position(0.620f, 0.350f, scale = 0.75f)
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
fun fpsTacticalPositions(): Map<String, Position> = mapOf(
    "LT" to Position(0.10f, 0.08f, scale = 1.35f),
    "LB" to Position(0.10f, 0.28f, scale = 1.1f),
    "RT" to Position(0.90f, 0.08f, scale = 1.35f),
    "RB" to Position(0.90f, 0.28f, scale = 1.1f),
    "LS" to Position(0.16f, 0.62f, scale = 1.55f),
    "RS" to Position(0.84f, 0.62f, scale = 1.55f),
    "DPAD" to Position(0.32f, 0.76f, scale = 1.35f),
    "Y" to Position(0.68f, 0.60f, scale = 1.1f),
    "X" to Position(0.60f, 0.72f, scale = 1.1f),
    "B" to Position(0.76f, 0.72f, scale = 1.1f),
    "A" to Position(0.68f, 0.84f, scale = 1.1f),
    "XBOX" to Position(0.50f, 0.10f, scale = 1.2f),
    "VIEW" to Position(0.43f, 0.24f, scale = 0.75f),
    "MENU" to Position(0.57f, 0.24f, scale = 0.75f),
    "M1" to Position(0.78f, 0.42f, scale = 0.9f),
    "M2" to Position(0.68f, 0.42f, scale = 0.9f),
    "M3" to Position(0.22f, 0.42f, scale = 0.9f),
    "M4" to Position(0.32f, 0.42f, scale = 0.9f)
)

/** Default Layout 3: MOBA & Action RPG with curved ability arc and skill shortcuts. */
fun mobaActionPositions(): Map<String, Position> = mapOf(
    "LS" to Position(0.16f, 0.68f, scale = 1.55f),
    "RS" to Position(0.50f, 0.75f, scale = 1.4f),
    "DPAD" to Position(0.16f, 0.28f, scale = 1.3f),
    "A" to Position(0.78f, 0.78f, scale = 1.2f),
    "B" to Position(0.88f, 0.64f, scale = 1.15f),
    "X" to Position(0.70f, 0.64f, scale = 1.15f),
    "Y" to Position(0.80f, 0.50f, scale = 1.2f),
    "M1" to Position(0.65f, 0.38f, scale = 0.9f),
    "M2" to Position(0.78f, 0.32f, scale = 0.9f),
    "M3" to Position(0.90f, 0.36f, scale = 0.9f),
    "M4" to Position(0.35f, 0.38f, scale = 0.9f),
    "LB" to Position(0.10f, 0.08f, scale = 1.1f),
    "LT" to Position(0.24f, 0.08f, scale = 1.2f),
    "RB" to Position(0.76f, 0.08f, scale = 1.1f),
    "RT" to Position(0.90f, 0.08f, scale = 1.2f),
    "XBOX" to Position(0.50f, 0.10f, scale = 1.2f),
    "VIEW" to Position(0.43f, 0.24f, scale = 0.75f),
    "MENU" to Position(0.57f, 0.24f, scale = 0.75f)
)

/** Default Layout 4: Racing & Simulation with wide analog triggers and paddle shifters. */
fun racingSimPositions(): Map<String, Position> = mapOf(
    "LT" to Position(0.12f, 0.35f, scale = 1.45f),
    "RT" to Position(0.88f, 0.35f, scale = 1.45f),
    "LB" to Position(0.12f, 0.12f, scale = 1.2f),
    "RB" to Position(0.88f, 0.12f, scale = 1.2f),
    "LS" to Position(0.20f, 0.74f, scale = 1.6f),
    "RS" to Position(0.80f, 0.74f, scale = 1.4f),
    "A" to Position(0.65f, 0.70f, scale = 1.1f),
    "B" to Position(0.65f, 0.54f, scale = 1.1f),
    "Y" to Position(0.52f, 0.54f, scale = 1.1f),
    "X" to Position(0.52f, 0.70f, scale = 1.1f),
    "DPAD" to Position(0.38f, 0.74f, scale = 1.3f),
    "M1" to Position(0.36f, 0.42f, scale = 0.85f),
    "M2" to Position(0.45f, 0.42f, scale = 0.85f),
    "M3" to Position(0.55f, 0.42f, scale = 0.85f),
    "M4" to Position(0.64f, 0.42f, scale = 0.85f),
    "XBOX" to Position(0.50f, 0.10f, scale = 1.2f),
    "VIEW" to Position(0.42f, 0.25f, scale = 0.75f),
    "MENU" to Position(0.58f, 0.25f, scale = 0.75f)
)

/** Default Layout 5: Retro Arcade & Fighter with 6-button fightstick grid and 8-way D-Pad. */
fun retroArcadePositions(): Map<String, Position> = mapOf(
    "DPAD" to Position(0.20f, 0.55f, scale = 1.7f),
    "LS" to Position(0.35f, 0.80f, scale = 1.2f),
    "X" to Position(0.63f, 0.44f, scale = 1.15f),
    "Y" to Position(0.75f, 0.40f, scale = 1.15f),
    "RB" to Position(0.87f, 0.44f, scale = 1.15f),
    "A" to Position(0.63f, 0.68f, scale = 1.15f),
    "B" to Position(0.75f, 0.64f, scale = 1.15f),
    "RT" to Position(0.87f, 0.68f, scale = 1.15f),
    "LB" to Position(0.10f, 0.15f, scale = 1.15f),
    "LT" to Position(0.90f, 0.15f, scale = 1.15f),
    "M1" to Position(0.63f, 0.88f, scale = 0.9f),
    "M2" to Position(0.75f, 0.88f, scale = 0.9f),
    "VIEW" to Position(0.42f, 0.20f, scale = 0.8f),
    "MENU" to Position(0.58f, 0.20f, scale = 0.8f),
    "XBOX" to Position(0.50f, 0.08f, scale = 1.2f)
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
        description = "Large analog throttle & brake triggers, steering thumbstick, and paddle shifters."
    ),
    LayoutProfile(
        name = "Retro Arcade & Fighter",
        isDefault = true,
        positions = retroArcadePositions(),
        description = "Classic 6-button arcade fightstick grid with 8-way directional D-pad."
    )
)
