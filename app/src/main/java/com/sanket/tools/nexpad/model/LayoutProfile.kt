package com.sanket.tools.nexpad.model

import com.sanket.tools.nexpad.category.ControlKey
import com.sanket.tools.nexpad.category.ControllerLabelStyle
import com.sanket.tools.nexpad.model.NexpadKeys as K
import kotlinx.serialization.Serializable

@Serializable
data class LayoutProfile(
    val name: String = "Standard Elite",
    val isDefault: Boolean = false,
    val isRgbEnabled: Boolean = true,
    val positions: Map<String, Position> = standardElitePositions(),
    val description: String = "",
    val labelStyle: String = "XBOX"
) {
    val controllerLabelStyle: ControllerLabelStyle
        get() = ControllerLabelStyle.fromId(labelStyle)

    /** Returns positions with all keys normalized to canonical ControlKey identifiers. */
    fun canonicalPositions(): Map<String, Position> {
        val normalized = mutableMapOf<String, Position>()
        positions.forEach { (rawKey, pos) ->
            val canonicalKey = ControlKey.fromIdentifier(rawKey)?.key ?: rawKey.uppercase()
            if (!normalized.containsKey(canonicalKey)) {
                normalized[canonicalKey] = pos
            }
        }
        // Self-heal: If both composite 4-way DPAD and discrete directional buttons exist,
        // industry standard is mutual exclusivity: retain the integrated 4-way DPAD and drop discrete directional buttons.
        if (normalized.containsKey(ControlKey.DPAD.key)) {
            val discreteKeys = ControlKey.DISCRETE_DPAD_KEYS.map { it.key }
            discreteKeys.forEach { normalized.remove(it) }
        }
        // Self-heal: Left Stick (LS) and Left Touchpad (LTP) cannot coexist simultaneously
        if (normalized.containsKey(ControlKey.LS.key) && normalized.containsKey(ControlKey.LTP.key)) {
            normalized.remove(ControlKey.LTP.key)
        }
        // Self-heal: Right Stick (RS) and Right Touchpad (RTP) cannot coexist simultaneously
        if (normalized.containsKey(ControlKey.RS.key) && normalized.containsKey(ControlKey.RTP.key)) {
            normalized.remove(ControlKey.RTP.key)
        }
        return normalized
    }

    fun hasControl(key: String): Boolean {
        val canonical = ControlKey.fromIdentifier(key)?.key ?: key.uppercase()
        return canonicalPositions().containsKey(canonical)
    }

    fun getPosition(key: String): Position? {
        val canonical = ControlKey.fromIdentifier(key)?.key ?: key.uppercase()
        return canonicalPositions()[canonical]
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
    val customComponentId: String? = null,
    val sensitivity: Float? = null,
    val heightScale: Float? = null,
    val isFlipped: Boolean? = null,
    val isLocked: Boolean? = null,
    val joystickMode: String? = null,
    val hitboxScale: Float? = null
)

/**
 * Default Layout 1: Standard Elite (Physical Xbox Asymmetric Ergonomics).
 * Guaranteed zero-cutout margins on all 4 viewport borders.
 */
fun standardElitePositions(): Map<String, Position> {
    val positions = mutableMapOf(
        // Triggers and Bumpers (Top Corners)
        K.LT to Position(0.080f, 0.130f, scale = 1.18f),
        K.LB to Position(0.080f, 0.375f, scale = 0.95f),
        K.RT to Position(0.920f, 0.130f, scale = 1.18f),
        K.RB to Position(0.920f, 0.375f, scale = 0.95f),

        // Left Stick & D-Pad (Integrated 4-Way Cross Pad)
        K.LS   to Position(0.110f, 0.740f, scale = 0.95f),
        K.DPAD to Position(0.330f, 0.740f, scale = 0.96f),

        // Right Stick
        K.RS to Position(0.895f, 0.740f, scale = 0.95f),

        // Center System Cluster
        K.GUIDE to Position(0.500f, 0.130f, scale = 1.15f),
        K.BACK  to Position(0.430f, 0.310f, scale = 0.70f),
        K.START to Position(0.500f, 0.310f, scale = 0.70f),
        K.SHARE to Position(0.570f, 0.310f, scale = 0.70f),
        "GYRO"  to Position(0.500f, 0.500f, scale = 0.85f)
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

/**
 * Default Layout 2: PlayStation DualSense Pro (Authentic Symmetric Ergonomics).
 * Symmetric thumbsticks at the bottom, D-Pad upper-left, face buttons upper-right,
 * and PS/Options/Create trio in the middle band.
 */
fun playStationDualSensePositions(): Map<String, Position> {
    val positions = mutableMapOf(
        // L2 / R2 Analog Triggers (top corners, matching Elite height)
        K.LT to Position(0.080f, 0.130f, scale = 1.15f),
        K.RT to Position(0.920f, 0.130f, scale = 1.15f),

        // L1 / R1 Digital Bumpers (below triggers)
        K.LB to Position(0.080f, 0.360f, scale = 0.90f),
        K.RB to Position(0.920f, 0.360f, scale = 0.90f),

        // D-Pad — primary left thumb control (upper-mid, clear of LS and L1)
        K.DPAD to Position(0.125f, 0.665f, scale = 0.90f),

        // Symmetric DualSense Sticks — lower row, centered side-by-side
        K.LS to Position(0.355f, 0.740f, scale = 0.95f),
        K.RS to Position(0.645f, 0.740f, scale = 0.95f),

        // PS / Create / Options — center mid-band
        K.GUIDE to Position(0.500f, 0.130f, scale = 1.15f), // PS button
        K.BACK  to Position(0.440f, 0.310f, scale = 0.70f), // Create / Share
        K.START to Position(0.560f, 0.310f, scale = 0.70f)  // Options
    )

    // Cross / Circle / Square / Triangle — primary right thumb control (upper-mid, clear of RS)
    positions.putAll(
        LayoutMetrics.createDiamondCluster(
            centerX = 0.875f,
            centerY = 0.665f,
            radiusDp = 50.0f,
            scale = 0.80f
        )
    )

    return positions
}

/**
 * Default Layout 3: FPS Tactical Claw (4-Finger Mobile Claw Grip).
 * Designed for 2 thumbs + 2 index fingers (PUBG / Call of Duty Mobile style):
 * - Left Index: Scope (LT) and Tactical (LB) at top-left.
 * - Left Thumb: Dedicated 360° Movement Stick (LS) lower-left.
 * - Right Index: Claw Action Matrix (A Jump, B Crouch, X Reload, Y Swap) + RT Fire / RB Melee at top-right.
 * - Right Thumb: Pure unhindered Aiming Stick (RS) lower-right.
 */
fun fpsTacticalClawPositions(): Map<String, Position> = mapOf(
    // Left Index Zone: Scope & Tactical
    K.LT to Position(0.080f, 0.130f, scale = 1.15f), // ADS / Scope
    K.LB to Position(0.080f, 0.350f, scale = 0.90f), // Tactical / Grenade

    // Left Thumb Zone: Pure Movement & Utilities
    K.LS to Position(0.140f, 0.720f, scale = 1.02f), // Strafe / Sprint
    K.DPAD to Position(0.340f, 0.720f, scale = 0.95f), // Ping / Inventory

    // Right Index Claw Matrix: High-frequency combat actions
    K.Y to Position(0.660f, 0.180f, scale = 0.85f), // Weapon Swap
    K.X to Position(0.765f, 0.180f, scale = 0.85f), // Reload
    K.A to Position(0.660f, 0.360f, scale = 0.85f), // Jump
    K.B to Position(0.765f, 0.360f, scale = 0.85f), // Crouch / Slide

    // Right Index Shoulder Triggers: Primary Fire & Melee
    K.RT to Position(0.920f, 0.130f, scale = 1.15f), // Primary Fire
    K.RB to Position(0.920f, 0.350f, scale = 0.90f), // Melee / Bash

    // Right Thumb Zone: Dedicated Aim Stick (zero button obstruction)
    K.RS to Position(0.840f, 0.720f, scale = 1.05f),

    // Center Console
    K.GUIDE to Position(0.500f, 0.130f, scale = 1.15f),
    K.BACK  to Position(0.440f, 0.310f, scale = 0.70f), // Map / Scoreboard
    K.START to Position(0.560f, 0.310f, scale = 0.70f)  // Pause / Menu
)

/** Backward compatible alias for FPS layout. */
fun fpsTacticalPositions(): Map<String, Position> = fpsTacticalClawPositions()

/**
 * Default Layout 4: Retro Arcade Fightstick (Authentic 6-Button Vewlix Matrix).
 * 8-Way D-Pad lower-left, clean 3×2 punch/kick grid right, EX assists top-left,
 * minimal center controls (Coin / Start).
 */
fun retroArcadeFightstickPositions(): Map<String, Position> = mapOf(
    // 8-Way Arcade D-Pad — primary directional input, lower-left
    K.DPAD to Position(0.200f, 0.680f, scale = 1.15f),

    // EX / Assist Shoulder Buttons — upper-left (consistent with gamepad: LT trigger top, LB bumper below)
    K.LT to Position(0.080f, 0.130f, scale = 1.00f), // 3-Kick V-Reversal / Assist 2
    K.LB to Position(0.080f, 0.360f, scale = 0.90f), // 3-Punch EX / Assist 1

    // 6-Button Vewlix Grid — authentic 3×2 layout
    // Top row = Light / Medium / Heavy Punch
    K.X  to Position(0.600f, 0.470f, scale = 0.86f), // Light Punch
    K.Y  to Position(0.730f, 0.440f, scale = 0.86f), // Medium Punch
    K.RB to Position(0.870f, 0.430f, scale = 0.85f), // Heavy Punch

    // Bottom row = Light / Medium / Heavy Kick
    K.A  to Position(0.600f, 0.720f, scale = 0.86f), // Light Kick
    K.B  to Position(0.730f, 0.690f, scale = 0.86f), // Medium Kick
    K.RT to Position(0.870f, 0.680f, scale = 0.85f), // Heavy Kick

    // Arcade Cabinet Center: Coin / Select, 1P Start, Home
    K.GUIDE to Position(0.500f, 0.130f, scale = 1.15f),
    K.BACK  to Position(0.440f, 0.310f, scale = 0.70f), // Coin / Select
    K.START to Position(0.560f, 0.310f, scale = 0.70f)  // 1P Start
)

/** Backward compatible alias for Retro Arcade layout. */
fun retroArcadePositions(): Map<String, Position> = retroArcadeFightstickPositions()

/**
 * Default Layout 5: Sim Racing & Flight (Asymmetric Stick-High Cockpit).
 * Based on authentic asymmetric gamepad & flight yoke geometry:
 * - Upper-Left: Steering Yoke / Flight Stick (LS) high for natural thumb reach.
 * - Lower-Left: Rudder / Pit-stop D-Pad (DPAD) below stick.
 * - Outer Left Margin: Analog Brake (LT) and Downshift (LB).
 * - Upper-Right: Cockpit instrument face cluster (X, Y, B, A).
 * - Lower-Right: Camera / Cockpit Look Stick (RS).
 * - Outer Right Margin: Analog Throttle (RT) and Upshift (RB).
 */
fun simRacingFlightPositions(): Map<String, Position> {
    val positions = mutableMapOf(
        // Outer Left Shoulders: Brake on top, Downshift below
        K.LT to Position(0.065f, 0.200f, scale = 0.95f),
        K.LB to Position(0.065f, 0.450f, scale = 0.95f),

        // Upper-Left: Steering Yoke / Flight Stick (LS)
        K.LS to Position(0.245f, 0.280f, scale = 1.02f),

        // Lower-Left: Pit Stop / Rudder D-Pad (DPAD)
        K.DPAD to Position(0.245f, 0.740f, scale = 1.05f),

        // Outer Right Shoulders: Throttle on top, Upshift below
        K.RT to Position(0.935f, 0.200f, scale = 0.95f),
        K.RB to Position(0.935f, 0.450f, scale = 0.95f),

        // Lower-Right: Camera / Cockpit Look Stick (RS)
        K.RS to Position(0.755f, 0.740f, scale = 1.02f),

        // Center Console: Dashboard Controls
        K.GUIDE to Position(0.500f, 0.130f, scale = 1.15f), // Home / Radio
        K.BACK  to Position(0.440f, 0.310f, scale = 0.70f), // Telemetry
        K.START to Position(0.560f, 0.310f, scale = 0.70f)  // Pause / Menu
    )

    // Upper-Right: Face Buttons — Cockpit instrument cluster (Gear/Nitro/Look Back)
    positions.putAll(
        LayoutMetrics.createDiamondCluster(
            centerX = 0.755f,
            centerY = 0.280f,
            radiusDp = 46.0f,
            scale = 0.78f
        )
    )

    return positions
}

/** Backward compatible alias for Racing layout. */
fun racingSimPositions(): Map<String, Position> = simRacingFlightPositions()

/**
 * Default Layout 6: Grand MOBA & RPG (Action RPG Radial Ability Wheel).
 * Designed for League of Legends Wild Rift, Mobile Legends, and Genshin Impact:
 * - Left Side: 360° Movement Stick (LS) + Quick Item / Recall (DPAD) + Potion (LB) / Heal (LT).
 * - Right Side: Large Basic Attack anchor button (A) with radial ability fan (X, Y, B),
 *   crowned by Ultimate Ability (RT) and Target Lock (RB).
 */
fun grandMobaRpgPositions(): Map<String, Position> = mapOf(
    // 360° Movement Stick — far left, smooth analog movement
    K.LS to Position(0.160f, 0.680f, scale = 1.05f),

    // Quick Item / Recall / Shop D-Pad
    K.DPAD to Position(0.360f, 0.680f, scale = 0.95f),

    // Secondary Ability / Heal & Potion / Flask
    K.LT to Position(0.080f, 0.140f, scale = 1.05f), // Heal / Secondary
    K.LB to Position(0.080f, 0.360f, scale = 0.90f), // Potion / Flask

    // Radial Ability Fan around Primary Attack:
    K.A  to Position(0.810f, 0.720f, scale = 1.18f), // Primary Attack / Auto-Attack Anchor
    K.X  to Position(0.690f, 0.760f, scale = 0.88f), // Skill 1 / Tactical
    K.Y  to Position(0.660f, 0.570f, scale = 0.88f), // Skill 2 / Area Effect
    K.RT to Position(0.770f, 0.430f, scale = 1.05f), // Ultimate Ability (Crown Trigger)
    K.B  to Position(0.925f, 0.630f, scale = 0.88f), // Dash / Evade / Dodge
    K.RB to Position(0.920f, 0.340f, scale = 0.88f), // Target Lock / Spell 2

    // Center Console
    K.GUIDE to Position(0.500f, 0.130f, scale = 1.15f),
    K.BACK  to Position(0.440f, 0.300f, scale = 0.70f), // Map / Inventory
    K.START to Position(0.560f, 0.300f, scale = 0.70f)  // Pause / Shop
)

/** Backward compatible alias for MOBA layout. */
fun mobaActionPositions(): Map<String, Position> = grandMobaRpgPositions()

/** Backward compatible alias for default positions. */
fun defaultPositions(): Map<String, Position> = standardElitePositions()

/**
 * Fallback ergonomic default positions for controls when added to a layout that doesn't define them.
 * Handles discrete directional buttons, stick clicks, touchpads, and share controls.
 */
fun getControlDefaultPosition(canonicalKey: String): Position? {
    return defaultPositions()[canonicalKey] ?: when (canonicalKey) {
        K.UP    -> Position(0.320f, 0.650f, scale = 0.85f)
        K.DOWN  -> Position(0.320f, 0.830f, scale = 0.85f)
        K.LEFT  -> Position(0.260f, 0.740f, scale = 0.85f)
        K.RIGHT -> Position(0.380f, 0.740f, scale = 0.85f)
        K.LSB   -> Position(0.210f, 0.540f, scale = 0.80f)
        K.RSB   -> Position(0.790f, 0.540f, scale = 0.80f)
        K.LTP   -> Position(0.180f, 0.680f, scale = 1.0f)
        K.RTP   -> Position(0.820f, 0.680f, scale = 1.0f)
        K.SHARE -> Position(0.580f, 0.245f, scale = 0.72f)
        K.M1    -> Position(0.620f, 0.360f, scale = 0.75f)
        K.M2    -> Position(0.380f, 0.360f, scale = 0.75f)
        K.M3    -> Position(0.550f, 0.360f, scale = 0.75f)
        K.M4    -> Position(0.450f, 0.360f, scale = 0.75f)
        "GYRO"  -> Position(0.500f, 0.500f, scale = 0.85f)
        else    -> null
    }
}

/**
 * Returns the non-deletable default layout profiles.
 * Provides 6 distinct, genre-optimized controller layouts with guaranteed zero cutout.
 */
private val DEFAULT_LAYOUT_PROFILES: List<LayoutProfile> by lazy {
    listOf(
        LayoutProfile(
            name = "Standard Elite",
            isDefault = true,
            labelStyle = "XBOX",
            positions = standardElitePositions(),
            description = "Precision Xbox asymmetric layout with ergonomic thumbstick offsets, dual triggers, and balanced central controls."
        ),
        LayoutProfile(
            name = "PlayStation DualSense Pro",
            isDefault = true,
            labelStyle = "PLAYSTATION",
            positions = playStationDualSensePositions(),
            description = "Authentic PlayStation symmetric layout with DualSense thumbsticks, d-pad primacy, and iconic geometric glyphs."
        ),
        LayoutProfile(
            name = "FPS Tactical Claw",
            isDefault = true,
            labelStyle = "XBOX",
            positions = fpsTacticalClawPositions(),
            description = "Competitive claw layout with hair triggers, separated aiming arcs, and instant tactical response."
        ),
        LayoutProfile(
            name = "Retro Arcade Fightstick",
            isDefault = true,
            labelStyle = "XBOX",
            positions = retroArcadeFightstickPositions(),
            description = "Authentic Japanese 6-button arcade cabinet fightstick grid with heavy punch/kick buttons and 8-way D-Pad."
        ),
        LayoutProfile(
            name = "Sim Racing & Flight",
            isDefault = true,
            labelStyle = "XBOX",
            positions = simRacingFlightPositions(),
            description = "Analog throttle & brake primacy with paddle shifters, steering telemetry stick, and pit-stop controls."
        ),
        LayoutProfile(
            name = "Grand MOBA & RPG",
            isDefault = true,
            labelStyle = "XBOX",
            positions = grandMobaRpgPositions(),
            description = "Ergonomic radial ability sweep with primary attack anchor, directional skillshot triggers, and quick spell controls."
        )
    )
}

fun getDefaultLayoutProfiles(): List<LayoutProfile> = DEFAULT_LAYOUT_PROFILES
