package com.sanket.tools.nexpad.ui.studio.model

/**
 * Preview style for rendering the 4-button ABXY diamond cluster.
 */
enum class AbxyPreviewStyle {
    REALISTIC_3D,   // Authentic 3D Xbox buttons (Y=Yellow, X=Blue, B=Red, A=Green)
    SCIFI_HEX,      // Cyan Holographic Hexagonal buttons
    CYBER_OCTA,     // Neon Crimson Octagonal buttons
    GAMEPAD_BUTTON  // Minimalist Flat circular buttons using GamepadButton.kt
}

/**
 * Cohesive 4-Button ABXY Diamond Group Theme.
 * ABXY is treated as one unified component of 4 buttons (no individual buttons).
 */
data class AbxyGroupTheme(
    val id: String,
    val name: String,
    val description: String,
    val type: ButtonStudioType,
    val author: String = "NEXPAD Core",
    val previewStyle: AbxyPreviewStyle,
    val skinMap: Map<String, String?> // Maps A, B, X, Y to customComponentId (null = Default 3D)
)

val ABXY_GROUP_THEMES = listOf(
    AbxyGroupTheme(
        id = "group.classic_xbox",
        name = "Classic 3D Xbox ABXY",
        description = "Authentic tactile 3D controller buttons with Emerald Green (A), Crimson Red (B), Cobalt Blue (X), and Canary Yellow (Y) illumination.",
        type = ButtonStudioType.DEFAULT,
        author = "NEXPAD Core",
        previewStyle = AbxyPreviewStyle.REALISTIC_3D,
        skinMap = mapOf("A" to null, "B" to null, "X" to null, "Y" to null)
    ),
    AbxyGroupTheme(
        id = "group.scifi_hex",
        name = "Sci-Fi Hex Hologram ABXY",
        description = "Futuristic cyan holographic hexagonal diamond cluster with high-tech spring bounce tactile feedback.",
        type = ButtonStudioType.SVG,
        author = "NEXPAD Core",
        previewStyle = AbxyPreviewStyle.SCIFI_HEX,
        skinMap = mapOf(
            "A" to "builtin.scifi_hex_a",
            "B" to "builtin.scifi_hex_b",
            "X" to "builtin.scifi_hex_x",
            "Y" to "builtin.scifi_hex_y"
        )
    ),
    AbxyGroupTheme(
        id = "group.cyber_octa",
        name = "Cyber Octa Burst ABXY",
        description = "Neon crimson octagonal diamond cluster designed for competitive twitch action and rapid actuation.",
        type = ButtonStudioType.SVG,
        author = "NEXPAD Core",
        previewStyle = AbxyPreviewStyle.CYBER_OCTA,
        skinMap = mapOf(
            "A" to "builtin.cyber_octa_a",
            "B" to "builtin.cyber_octa_b",
            "X" to "builtin.cyber_octa_x",
            "Y" to "builtin.cyber_octa_y"
        )
    ),
    AbxyGroupTheme(
        id = "group.gamepad_button",
        name = "Minimal Flat Gamepad ABXY",
        description = "Clean, responsive circular buttons using Compose GamepadButton styling with ultra-low visual latency.",
        type = ButtonStudioType.DEFAULT,
        author = "NEXPAD Compose",
        previewStyle = AbxyPreviewStyle.GAMEPAD_BUTTON,
        skinMap = mapOf("A" to null, "B" to null, "X" to null, "Y" to null)
    )
)

/**
 * Cohesive Left/Right Paired Button Theme.
 * Left and Right buttons are treated as one unified pair (98% shared architecture, connection & RGB differentiated).
 */
data class PairedGroupTheme(
    val id: String,
    val name: String,
    val description: String,
    val type: ButtonStudioType,
    val author: String = "NEXPAD Core",
    val leftKey: String,
    val rightKey: String,
    val skinMap: Map<String, String?>
)

val TRIGGER_GROUP_THEMES = listOf(
    PairedGroupTheme(
        id = "group.triggers_classic",
        name = "Classic 3D Dual Triggers",
        description = "Authentic analog shoulder triggers with full progressive travel. Left (LT) glows Electric Cyan, Right (RT) glows Neon Crimson.",
        type = ButtonStudioType.DEFAULT,
        author = "NEXPAD Core",
        leftKey = "LT",
        rightKey = "RT",
        skinMap = mapOf("LT" to null, "RT" to null)
    ),
    PairedGroupTheme(
        id = "group.triggers_impulse",
        name = "Impulse Cyber Triggers",
        description = "High-travel analog triggers with deep haptic microswitch response (builtin.pulse_trigger_lt & builtin.pulse_trigger_rt).",
        type = ButtonStudioType.SVG,
        author = "NEXPAD Core",
        leftKey = "LT",
        rightKey = "RT",
        skinMap = mapOf("LT" to "builtin.pulse_trigger_lt", "RT" to "builtin.pulse_trigger_rt")
    ),
    PairedGroupTheme(
        id = "group.triggers_minimal",
        name = "Minimal Flat Dual Triggers",
        description = "Low-profile digital triggers designed for minimal visual occlusion and instant actuation.",
        type = ButtonStudioType.DEFAULT,
        author = "NEXPAD Compose",
        leftKey = "LT",
        rightKey = "RT",
        skinMap = mapOf("LT" to null, "RT" to null)
    )
)

val BUMPER_GROUP_THEMES = listOf(
    PairedGroupTheme(
        id = "group.bumpers_classic",
        name = "Classic 3D Dual Bumpers",
        description = "Authentic ergonomic shoulder bumpers with crisp mechanical microswitch click. Left (LB) glows Electric Violet, Right (RB) glows Neon Cyan.",
        type = ButtonStudioType.DEFAULT,
        author = "NEXPAD Core",
        leftKey = "LB",
        rightKey = "RB",
        skinMap = mapOf("LB" to null, "RB" to null)
    ),
    PairedGroupTheme(
        id = "group.bumpers_cyber",
        name = "Tactical Cyber Bumpers",
        description = "Sleek aerodynamic composite bumpers with glowing edge illumination (builtin.cyber_bumper_lb & builtin.cyber_bumper_rb).",
        type = ButtonStudioType.SVG,
        author = "NEXPAD Core",
        leftKey = "LB",
        rightKey = "RB",
        skinMap = mapOf("LB" to "builtin.cyber_bumper_lb", "RB" to "builtin.cyber_bumper_rb")
    ),
    PairedGroupTheme(
        id = "group.bumpers_minimal",
        name = "Minimal Flat Dual Bumpers",
        description = "High-tactility rounded digital bumper caps with instant touch response.",
        type = ButtonStudioType.DEFAULT,
        author = "NEXPAD Compose",
        leftKey = "LB",
        rightKey = "RB",
        skinMap = mapOf("LB" to null, "RB" to null)
    )
)

val STICK_GROUP_THEMES = listOf(
    PairedGroupTheme(
        id = "group.sticks_classic",
        name = "Classic 3D Dual Sticks",
        description = "Authentic textured concave thumbsticks with full 360° analog range. Left (LS) features Cyan RGB ring, Right (RS) features Magenta RGB ring.",
        type = ButtonStudioType.DEFAULT,
        author = "NEXPAD Core",
        leftKey = "LS",
        rightKey = "RS",
        skinMap = mapOf("LS" to null, "RS" to null)
    ),
    PairedGroupTheme(
        id = "group.sticks_cyber",
        name = "Neon Matrix & Cyber Vortex Sticks",
        description = "Dual-ring holographic cyber analog sticks (builtin.neon_matrix_ls and builtin.cyber_vortex_rs) with active reticle deadzone indicators.",
        type = ButtonStudioType.SVG,
        author = "NEXPAD Core",
        leftKey = "LS",
        rightKey = "RS",
        skinMap = mapOf("LS" to "builtin.neon_matrix_ls", "RS" to "builtin.cyber_vortex_rs")
    ),
    PairedGroupTheme(
        id = "group.sticks_minimal",
        name = "Minimal Flat Dual Sticks",
        description = "Compact low-latency analog joysticks with fluid multi-touch tracking and minimal HUD occlusion.",
        type = ButtonStudioType.DEFAULT,
        author = "NEXPAD Compose",
        leftKey = "LS",
        rightKey = "RS",
        skinMap = mapOf("LS" to null, "RS" to null)
    )
)
