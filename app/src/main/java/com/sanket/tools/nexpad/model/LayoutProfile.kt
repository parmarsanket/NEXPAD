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
    val isFlipped: Boolean? = null
)

/**
 * Default Layout 1: Standard Elite (Physical Xbox Asymmetric Ergonomics).
 * Guaranteed zero-cutout margins on all 4 viewport borders.
 */
fun standardElitePositions(): Map<String, Position> {
    val positions = mutableMapOf(
        // Shoulders (Top Left & Top Right): Zero-cutout safe placement
        K.LT to Position(0.100f, 0.140f, scale = 0.92f),
        K.LB to Position(0.100f, 0.285f, scale = 0.82f),
        K.RT to Position(0.900f, 0.140f, scale = 0.92f),
        K.RB to Position(0.900f, 0.285f, scale = 0.82f),

        // Asymmetric Left: Left Stick (Upper Primary) & D-Pad (Lower Secondary)
        K.LS   to Position(0.165f, 0.570f, scale = 1.00f),
        K.DPAD to Position(0.305f, 0.750f, scale = 1.05f),

        // Asymmetric Right: Right Stick (Lower Secondary)
        K.RS   to Position(0.840f, 0.740f, scale = 1.00f),

        // Stick Clicks
        K.LSB  to Position(0.165f, 0.730f, scale = 0.65f),
        K.RSB  to Position(0.840f, 0.570f, scale = 0.65f),

        // Center Console Cluster
        K.GUIDE to Position(0.500f, 0.130f, scale = 0.95f),
        K.BACK  to Position(0.420f, 0.245f, scale = 0.72f),
        K.START to Position(0.500f, 0.245f, scale = 0.72f),
        K.SHARE to Position(0.580f, 0.245f, scale = 0.72f),

        // Tactical Macro Paddles
        K.M2 to Position(0.380f, 0.360f, scale = 0.75f),
        K.M4 to Position(0.450f, 0.360f, scale = 0.75f),
        K.M3 to Position(0.550f, 0.360f, scale = 0.75f),
        K.M1 to Position(0.620f, 0.360f, scale = 0.75f)
    )

    // Face Buttons: Upper-Right Isotropic Diamond Cluster
    positions.putAll(
        LayoutMetrics.createDiamondCluster(
            centerX = 0.705f,
            centerY = 0.520f,
            radiusDp = 52.0f,
            scale = 0.80f
        )
    )

    return positions
}

/**
 * Default Layout 2: PlayStation DualSense Pro (Authentic Symmetric Ergonomics).
 * Symmetric lower thumbsticks, primary D-Pad, geometric face glyphs, and central touchpads.
 */
fun playStationDualSensePositions(): Map<String, Position> {
    val positions = mutableMapOf(
        // DualSense L2 / L1 / R2 / R1 Shoulders
        K.LT to Position(0.100f, 0.140f, scale = 0.92f),
        K.LB to Position(0.100f, 0.285f, scale = 0.82f),
        K.RT to Position(0.900f, 0.140f, scale = 0.92f),
        K.RB to Position(0.900f, 0.285f, scale = 0.82f),

        // Symmetric Dual Sticks along lower sweep
        K.LS to Position(0.290f, 0.740f, scale = 1.00f),
        K.RS to Position(0.710f, 0.740f, scale = 1.00f),

        // Primary Upper D-Pad & Stick Clicks
        K.DPAD to Position(0.160f, 0.520f, scale = 1.05f),
        K.LSB  to Position(0.290f, 0.570f, scale = 0.65f),
        K.RSB  to Position(0.710f, 0.570f, scale = 0.65f),

        // DualSense Central Touchpad
        K.LTP to Position(0.435f, 0.110f, scale = 0.80f),
        K.RTP to Position(0.565f, 0.110f, scale = 0.80f),

        // PS Center System Buttons
        K.GUIDE to Position(0.500f, 0.265f, scale = 0.90f),
        K.BACK  to Position(0.355f, 0.265f, scale = 0.70f), // Create / Share
        K.START to Position(0.645f, 0.265f, scale = 0.70f), // Options

        // Lower Macro Paddles
        K.M1 to Position(0.420f, 0.380f, scale = 0.75f),
        K.M2 to Position(0.580f, 0.380f, scale = 0.75f)
    )

    // PlayStation Face Buttons (Cross, Circle, Square, Triangle)
    positions.putAll(
        LayoutMetrics.createDiamondCluster(
            centerX = 0.840f,
            centerY = 0.520f,
            radiusDp = 52.0f,
            scale = 0.80f
        )
    )

    return positions
}

/**
 * Default Layout 3: FPS Tactical Claw (Competitive Shooter 6-Finger Grip).
 * Fast hair triggers, separated aim/strafe zones, and jump/slide macro paddles.
 */
fun fpsTacticalClawPositions(): Map<String, Position> {
    val positions = mutableMapOf(
        // Hair Triggers & Instant Bumpers
        K.LT to Position(0.100f, 0.140f, scale = 0.95f), // ADS / Quick-Draw
        K.RT to Position(0.900f, 0.140f, scale = 0.95f), // Hair Fire
        K.LB to Position(0.100f, 0.285f, scale = 0.82f), // Tactical / Grenade
        K.RB to Position(0.900f, 0.285f, scale = 0.82f), // Melee / Ping

        // Strafe Stick & Recoil Aim Stick (Separated for zero thumb clash)
        K.LS to Position(0.165f, 0.580f, scale = 1.05f),
        K.RS to Position(0.840f, 0.740f, scale = 1.05f),

        // Stick Click Sprint & Melee
        K.LSB to Position(0.165f, 0.760f, scale = 0.65f),
        K.RSB to Position(0.840f, 0.570f, scale = 0.65f),

        // Tactical D-Pad for inventory & callouts
        K.DPAD to Position(0.315f, 0.750f, scale = 1.00f),

        // Claw Macro Paddles: Jump & Slide right next to thumb arcs
        K.M1 to Position(0.710f, 0.420f, scale = 0.82f), // Claw Jump (Instant jump-shot)
        K.M2 to Position(0.710f, 0.740f, scale = 0.82f), // Claw Slide / Crouch (Slide-cancel)
        K.M3 to Position(0.290f, 0.420f, scale = 0.82f), // Quick Reload
        K.M4 to Position(0.420f, 0.560f, scale = 0.75f), // Weapon Swap

        // Center Minimal System Controls
        K.GUIDE to Position(0.500f, 0.130f, scale = 0.95f),
        K.BACK  to Position(0.430f, 0.245f, scale = 0.72f),
        K.START to Position(0.570f, 0.245f, scale = 0.72f)
    )

    // Face Buttons in Upper-Right Zone (Zero RS Overlap)
    positions.putAll(
        LayoutMetrics.createDiamondCluster(
            centerX = 0.840f,
            centerY = 0.440f,
            radiusDp = 48.0f,
            scale = 0.75f
        )
    )

    return positions
}

/** Backward compatible alias for FPS layout. */
fun fpsTacticalPositions(): Map<String, Position> = fpsTacticalClawPositions()

/**
 * Default Layout 4: Retro Arcade Fightstick (Authentic 6-Button Fighting Matrix).
 * Authentic Capcom/Vewlix Japanese fightstick layout with heavy punch/kick buttons and 8-way D-Pad.
 */
fun retroArcadeFightstickPositions(): Map<String, Position> = mapOf(
    // 8-Way Arcade Directional Pad & Optional 360 Grappler Stick
    K.DPAD to Position(0.170f, 0.620f, scale = 1.15f),
    K.LS   to Position(0.335f, 0.620f, scale = 1.00f),

    // 6-Button Arcade Fightstick Matrix
    // Top Row (Punches): Light (X), Medium (Y), Heavy (RB)
    K.X  to Position(0.590f, 0.490f, scale = 0.82f),
    K.Y  to Position(0.700f, 0.460f, scale = 0.82f),
    K.RB to Position(0.815f, 0.435f, scale = 0.78f),

    // Bottom Row (Kicks): Light (A), Medium (B), Heavy (RT)
    K.A  to Position(0.590f, 0.740f, scale = 0.82f),
    K.B  to Position(0.700f, 0.710f, scale = 0.82f),
    K.RT to Position(0.815f, 0.685f, scale = 0.78f),

    // Left Shoulder EX / V-Reversal / Assist Buttons
    K.LB to Position(0.100f, 0.140f, scale = 0.82f), // 3-Punch EX / Throw
    K.LT to Position(0.100f, 0.285f, scale = 0.85f), // 3-Kick V-Reversal

    // Fightstick Cabinet Center Controls
    K.BACK  to Position(0.420f, 0.200f, scale = 0.72f), // Coin / Select
    K.START to Position(0.580f, 0.200f, scale = 0.72f), // 1P Start
    K.GUIDE to Position(0.500f, 0.125f, scale = 0.95f),
    K.M1    to Position(0.360f, 0.360f, scale = 0.75f),
    K.M2    to Position(0.460f, 0.360f, scale = 0.75f)
)

/** Backward compatible alias for Retro Arcade layout. */
fun retroArcadePositions(): Map<String, Position> = retroArcadeFightstickPositions()

/**
 * Default Layout 5: Sim Racing & Flight (Throttle, Brake & Sequential Shifters).
 * Large analog throttle & brake triggers, sequential paddle shifters, and steering stick.
 */
fun simRacingFlightPositions(): Map<String, Position> {
    val positions = mutableMapOf(
        // Analog Pedals: Right Throttle (RT) & Left Brake (LT)
        K.RT to Position(0.895f, 0.160f, scale = 1.00f), // Progressive Throttle
        K.LT to Position(0.105f, 0.160f, scale = 1.00f), // Progressive Brake

        // Sequential Paddle Shifters
        K.RB to Position(0.895f, 0.320f, scale = 0.82f), // Upshift
        K.LB to Position(0.105f, 0.320f, scale = 0.82f), // Downshift

        // Precision Steering Stick & Cockpit Look
        K.LS to Position(0.175f, 0.660f, scale = 1.15f), // Steering Rack
        K.RS to Position(0.825f, 0.660f, scale = 1.00f), // Cockpit Look

        // Pit Stop & Engine Map D-Pad
        K.DPAD to Position(0.340f, 0.750f, scale = 1.00f),

        // Racing Telemetry Macros
        K.M1 to Position(0.360f, 0.380f, scale = 0.75f), // DRS / Nitrous Boost
        K.M2 to Position(0.450f, 0.380f, scale = 0.75f), // E-Brake / Handbrake
        K.M3 to Position(0.550f, 0.380f, scale = 0.75f), // Rear View Camera
        K.M4 to Position(0.640f, 0.380f, scale = 0.75f), // Pit Limiter

        // Center Controls
        K.GUIDE to Position(0.500f, 0.130f, scale = 0.95f),
        K.BACK  to Position(0.430f, 0.245f, scale = 0.72f), // Telemetry
        K.START to Position(0.570f, 0.245f, scale = 0.72f)  // Pause
    )

    positions.putAll(
        LayoutMetrics.createDiamondCluster(
            centerX = 0.695f,
            centerY = 0.540f,
            radiusDp = 50.0f,
            scale = 0.78f
        )
    )

    return positions
}

/** Backward compatible alias for Racing layout. */
fun racingSimPositions(): Map<String, Position> = simRacingFlightPositions()

/**
 * Default Layout 6: Grand MOBA & RPG (Action RPG Radial Ability Sweep).
 * Primary auto-attack anchor, curved skillshot fanning, and quick potion/spell triggers.
 */
fun grandMobaRpgPositions(): Map<String, Position> = mapOf(
    // 360° Movement Stick & Quick Item D-Pad
    K.LS   to Position(0.170f, 0.650f, scale = 1.15f),
    K.DPAD to Position(0.330f, 0.650f, scale = 1.00f),

    // Radial Ability Fan: Primary Attack (A) anchor with outer skill sweep
    K.A to Position(0.760f, 0.740f, scale = 1.00f), // Primary Attack / Auto-Attack Anchor
    K.X to Position(0.630f, 0.690f, scale = 0.85f), // Skill 1
    K.Y to Position(0.675f, 0.520f, scale = 0.85f), // Skill 2
    K.B to Position(0.800f, 0.540f, scale = 0.85f), // Skill 3 / Dash

    // Ultimate Ability & Potions
    K.RB to Position(0.900f, 0.285f, scale = 0.82f), // Ultimate Ability
    K.LB to Position(0.100f, 0.285f, scale = 0.82f), // Potion / Flask
    K.RT to Position(0.900f, 0.140f, scale = 0.92f), // Target Lock
    K.LT to Position(0.100f, 0.140f, scale = 0.92f), // Secondary Skill

    // Camera / Target Aim Stick
    K.RS to Position(0.900f, 0.740f, scale = 0.85f),

    // Quick Item & Summoner Spell Macros
    K.M4 to Position(0.360f, 0.360f, scale = 0.75f),
    K.M1 to Position(0.450f, 0.360f, scale = 0.75f),
    K.M2 to Position(0.550f, 0.360f, scale = 0.75f),
    K.M3 to Position(0.640f, 0.360f, scale = 0.75f),

    // Center Console
    K.GUIDE to Position(0.500f, 0.130f, scale = 0.95f),
    K.BACK  to Position(0.430f, 0.245f, scale = 0.72f),
    K.START to Position(0.570f, 0.245f, scale = 0.72f)
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
        K.UP    -> Position(0.305f, 0.660f, scale = 0.85f)
        K.DOWN  -> Position(0.305f, 0.840f, scale = 0.85f)
        K.LEFT  -> Position(0.245f, 0.750f, scale = 0.85f)
        K.RIGHT -> Position(0.365f, 0.750f, scale = 0.85f)
        K.LSB   -> Position(0.165f, 0.730f, scale = 0.65f)
        K.RSB   -> Position(0.840f, 0.570f, scale = 0.65f)
        K.LTP   -> Position(0.435f, 0.110f, scale = 0.80f)
        K.RTP   -> Position(0.565f, 0.110f, scale = 0.80f)
        K.SHARE -> Position(0.580f, 0.245f, scale = 0.72f)
        else    -> null
    }
}

/**
 * Returns the non-deletable default layout profiles.
 * Provides 6 distinct, genre-optimized controller layouts with guaranteed zero cutout.
 */
fun getDefaultLayoutProfiles(): List<LayoutProfile> = listOf(
    LayoutProfile(
        name = "Standard Elite",
        isDefault = true,
        labelStyle = "XBOX",
        positions = standardElitePositions(),
        description = "Precision Xbox asymmetric layout with ergonomic thumbstick offsets, dual triggers, and tactical macro paddles."
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
        description = "Competitive 6-finger claw layout with hair triggers, jump/slide macro paddles, and separated aiming arcs."
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
        description = "Ergonomic radial ability sweep with primary attack anchor, directional skillshot triggers, and quick spell macros."
    )
)
