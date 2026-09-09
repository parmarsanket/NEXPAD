package com.sanket.tools.nexpad.runtime.registry

import com.sanket.tools.nexpad.runtime.model.*

object DefaultComponents {

    val SCIFI_HEX_ATTACK = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.scifi_hex_a",
            name = "Sci-Fi Hex Attack",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = "BUTTON",
            defaultControl = "A",
            description = "Cyan holographic hexagonal attack button with spring bounce feedback."
        ),
        geometry = NxpGeometry(
            type = "Polygon",
            sides = 6,
            cornerRadius = 10f
        ),
        visual = NxpVisual(
            fillColor = "#0A192F",
            opacity = 0.90f,
            borderColor = "#00F0FF",
            borderWidth = 2.5f,
            glowColor = "#00F0FF",
            glowRadius = 12f
        ),
        pressed = NxpPressedState(
            scale = 0.86f,
            rotation = 3f,
            fillColor = "#00F0FF",
            borderColor = "#FFFFFF",
            glowRadius = 20f,
            springDamping = 0.55f,
            springStiffness = 750f
        ),
        label = NxpLabel(
            text = "A",
            color = "#00F0FF",
            pressedColor = "#0A192F",
            fontSize = 24f
        ),
        size = NxpSize(widthDp = 76, heightDp = 76)
    )

    val CYBER_OCTA_BURST = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.cyber_octa_b",
            name = "Cyber Octa Burst",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = "BUTTON",
            defaultControl = "B",
            description = "Neon crimson octagonal defense/burst button."
        ),
        geometry = NxpGeometry(
            type = "Polygon",
            sides = 8,
            cornerRadius = 8f
        ),
        visual = NxpVisual(
            fillColor = "#1F0A12",
            opacity = 0.90f,
            borderColor = "#FF0055",
            borderWidth = 2.5f,
            glowColor = "#FF0055",
            glowRadius = 12f
        ),
        pressed = NxpPressedState(
            scale = 0.86f,
            rotation = -3f,
            fillColor = "#FF0055",
            borderColor = "#FFFFFF",
            glowRadius = 20f,
            springDamping = 0.55f,
            springStiffness = 750f
        ),
        label = NxpLabel(
            text = "B",
            color = "#FF0055",
            pressedColor = "#FFFFFF",
            fontSize = 24f
        ),
        size = NxpSize(widthDp = 76, heightDp = 76)
    )

    val NEON_MATRIX_JOYSTICK = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.neon_matrix_ls",
            name = "Neon Matrix Analog Stick",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = "JOYSTICK",
            defaultControl = "LS",
            description = "Floating dual-ring cyber analog stick with dynamic deadzone indicators."
        ),
        geometry = NxpGeometry(
            type = "Circle"
        ),
        visual = NxpVisual(
            fillColor = "#0B132B",
            opacity = 0.85f,
            borderColor = "#00F0FF",
            borderWidth = 2f
        ),
        pressed = NxpPressedState(
            fillColor = "#00F0FF"
        ),
        interaction = NxpInteraction(
            type = "Joystick",
            deadzone = 0.05f
        ),
        size = NxpSize(widthDp = 100, heightDp = 100)
    )

    val NEON_DIAMOND_X = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.neon_diamond_x",
            name = "Neon Diamond Reload",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = "BUTTON",
            defaultControl = "X",
            description = "Cyan holographic diamond quick action button."
        ),
        geometry = NxpGeometry(
            type = "Polygon",
            sides = 4,
            cornerRadius = 10f
        ),
        visual = NxpVisual(
            fillColor = "#0D1B2A",
            opacity = 0.90f,
            borderColor = "#00B4D8",
            borderWidth = 2.5f,
            glowColor = "#00B4D8",
            glowRadius = 12f
        ),
        pressed = NxpPressedState(
            scale = 0.86f,
            rotation = 45f,
            fillColor = "#00B4D8",
            borderColor = "#FFFFFF",
            glowRadius = 20f
        ),
        label = NxpLabel(
            text = "X",
            color = "#00B4D8",
            pressedColor = "#0D1B2A",
            fontSize = 24f
        ),
        size = NxpSize(widthDp = 76, heightDp = 76)
    )

    val PLASMA_TRIANGLE_Y = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.plasma_triangle_y",
            name = "Plasma Triangle Heavy",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = "BUTTON",
            defaultControl = "Y",
            description = "Amber plasma power button with explosive kinetic pop."
        ),
        geometry = NxpGeometry(
            type = "Polygon",
            sides = 3,
            cornerRadius = 12f
        ),
        visual = NxpVisual(
            fillColor = "#261700",
            opacity = 0.90f,
            borderColor = "#FFB703",
            borderWidth = 2.5f,
            glowColor = "#FFB703",
            glowRadius = 12f
        ),
        pressed = NxpPressedState(
            scale = 0.86f,
            fillColor = "#FFB703",
            borderColor = "#FFFFFF",
            glowRadius = 20f
        ),
        label = NxpLabel(
            text = "Y",
            color = "#FFB703",
            pressedColor = "#261700",
            fontSize = 24f
        ),
        size = NxpSize(widthDp = 76, heightDp = 76)
    )

    val CYBER_BUMPER_LB = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.cyber_bumper_lb",
            name = "Tactical Bumper LB",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = "BUMPER",
            defaultControl = "LB",
            description = "Sleek aerodynamic left shoulder bumper with neon edge illumination."
        ),
        geometry = NxpGeometry(
            type = "RoundedRect",
            cornerRadius = 16f
        ),
        visual = NxpVisual(
            fillColor = "#111827",
            opacity = 0.88f,
            borderColor = "#7C3AED",
            borderWidth = 2.5f,
            glowColor = "#7C3AED",
            glowRadius = 10f
        ),
        pressed = NxpPressedState(
            scale = 0.92f,
            fillColor = "#7C3AED",
            borderColor = "#FFFFFF"
        ),
        label = NxpLabel(
            text = "LB",
            color = "#A78BFA",
            pressedColor = "#FFFFFF",
            fontSize = 18f
        ),
        size = NxpSize(widthDp = 104, heightDp = 50)
    )

    val CYBER_BUMPER_RB = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.cyber_bumper_rb",
            name = "Tactical Bumper RB",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = "BUMPER",
            defaultControl = "RB",
            description = "Sleek aerodynamic right shoulder bumper with cyan edge illumination."
        ),
        geometry = NxpGeometry(
            type = "RoundedRect",
            cornerRadius = 16f
        ),
        visual = NxpVisual(
            fillColor = "#111827",
            opacity = 0.88f,
            borderColor = "#00F0FF",
            borderWidth = 2.5f,
            glowColor = "#00F0FF",
            glowRadius = 10f
        ),
        pressed = NxpPressedState(
            scale = 0.92f,
            fillColor = "#00F0FF",
            borderColor = "#FFFFFF"
        ),
        label = NxpLabel(
            text = "RB",
            color = "#00F0FF",
            pressedColor = "#000000",
            fontSize = 18f
        ),
        size = NxpSize(widthDp = 104, heightDp = 50)
    )

    val PULSE_TRIGGER_LT = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.pulse_trigger_lt",
            name = "Impulse Trigger LT",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = "TRIGGER",
            defaultControl = "LT",
            description = "High-travel analog trigger with deep haptic feedback."
        ),
        geometry = NxpGeometry(
            type = "RoundedRect",
            cornerRadius = 16f
        ),
        visual = NxpVisual(
            fillColor = "#0D1B2A",
            opacity = 0.90f,
            borderColor = "#3A86FF",
            borderWidth = 2.5f,
            glowColor = "#3A86FF",
            glowRadius = 12f
        ),
        pressed = NxpPressedState(
            scale = 0.90f,
            fillColor = "#3A86FF",
            borderColor = "#FFFFFF"
        ),
        interaction = NxpInteraction(
            type = "Trigger"
        ),
        label = NxpLabel(
            text = "LT",
            color = "#3A86FF",
            pressedColor = "#FFFFFF",
            fontSize = 20f
        ),
        size = NxpSize(widthDp = 92, heightDp = 96)
    )

    val PULSE_TRIGGER_RT = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.pulse_trigger_rt",
            name = "Impulse Trigger RT",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = "TRIGGER",
            defaultControl = "RT",
            description = "Hair-trigger accelerator with instant microswitch response."
        ),
        geometry = NxpGeometry(
            type = "RoundedRect",
            cornerRadius = 16f
        ),
        visual = NxpVisual(
            fillColor = "#1F0A12",
            opacity = 0.90f,
            borderColor = "#FF0055",
            borderWidth = 2.5f,
            glowColor = "#FF0055",
            glowRadius = 12f
        ),
        pressed = NxpPressedState(
            scale = 0.90f,
            fillColor = "#FF0055",
            borderColor = "#FFFFFF"
        ),
        interaction = NxpInteraction(
            type = "Trigger"
        ),
        label = NxpLabel(
            text = "RT",
            color = "#FF0055",
            pressedColor = "#FFFFFF",
            fontSize = 20f
        ),
        size = NxpSize(widthDp = 92, heightDp = 96)
    )

    val CYBER_VORTEX_RS = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.cyber_vortex_rs",
            name = "Cyber Vortex RS Stick",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = "JOYSTICK",
            defaultControl = "RS",
            description = "High-precision right thumbstick with reticle crosshair indicator."
        ),
        geometry = NxpGeometry(
            type = "Circle"
        ),
        visual = NxpVisual(
            fillColor = "#190826",
            opacity = 0.88f,
            borderColor = "#D946EF",
            borderWidth = 2f,
            glowColor = "#D946EF",
            glowRadius = 12f
        ),
        pressed = NxpPressedState(
            fillColor = "#D946EF"
        ),
        interaction = NxpInteraction(
            type = "Joystick",
            deadzone = 0.04f
        ),
        size = NxpSize(widthDp = 100, heightDp = 100)
    )

    val HOLO_CROSS_DPAD = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.holo_cross_dpad",
            name = "Holo Cross D-Pad",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = "DPAD",
            defaultControl = "DPAD",
            description = "Holographic 4-way precision directional pad."
        ),
        geometry = NxpGeometry(
            type = "Polygon",
            sides = 8,
            cornerRadius = 12f
        ),
        visual = NxpVisual(
            fillColor = "#0B1A24",
            opacity = 0.88f,
            borderColor = "#2DD4BF",
            borderWidth = 2f,
            glowColor = "#2DD4BF",
            glowRadius = 12f
        ),
        pressed = NxpPressedState(
            fillColor = "#2DD4BF"
        ),
        label = NxpLabel(
            text = "DPAD",
            color = "#2DD4BF",
            pressedColor = "#000000",
            fontSize = 14f
        ),
        size = NxpSize(widthDp = 120, heightDp = 120)
    )

    val NEXUS_ORB_HOME = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.nexus_orb_home",
            name = "Nexus Core Home Guide",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = "HOME",
            defaultControl = "XBOX",
            description = "Floating orbital nexus guide button with breathing power core."
        ),
        geometry = NxpGeometry(
            type = "Circle"
        ),
        visual = NxpVisual(
            fillColor = "#064E3B",
            opacity = 0.90f,
            borderColor = "#10B981",
            borderWidth = 2.5f,
            glowColor = "#10B981",
            glowRadius = 14f
        ),
        pressed = NxpPressedState(
            scale = 0.85f,
            fillColor = "#10B981",
            borderColor = "#FFFFFF"
        ),
        label = NxpLabel(
            text = "X",
            color = "#FFFFFF",
            pressedColor = "#064E3B",
            fontSize = 24f
        ),
        size = NxpSize(widthDp = 64, heightDp = 64)
    )

    val TACTICAL_SLIM_MENU = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.tactical_menu",
            name = "Tactical Menu Pill",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = "SYSTEM",
            defaultControl = "MENU",
            description = "Minimalist stealth system menu button."
        ),
        geometry = NxpGeometry(
            type = "RoundedRect",
            cornerRadius = 10f
        ),
        visual = NxpVisual(
            fillColor = "#1E293B",
            opacity = 0.85f,
            borderColor = "#94A3B8",
            borderWidth = 1.5f
        ),
        pressed = NxpPressedState(
            scale = 0.90f,
            fillColor = "#94A3B8"
        ),
        label = NxpLabel(
            text = "MENU",
            color = "#E2E8F0",
            pressedColor = "#0F172A",
            fontSize = 11f
        ),
        size = NxpSize(widthDp = 52, heightDp = 30)
    )

    val MACRO_PILL_M1 = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.macro_pill_m1",
            name = "Ergonomic Macro Paddle M1",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = "MACRO",
            defaultControl = "M1",
            description = "Low-latency custom macro paddle with instant actuator click."
        ),
        geometry = NxpGeometry(
            type = "RoundedRect",
            cornerRadius = 12f
        ),
        visual = NxpVisual(
            fillColor = "#18181B",
            opacity = 0.90f,
            borderColor = "#F59E0B",
            borderWidth = 2f,
            glowColor = "#F59E0B",
            glowRadius = 10f
        ),
        pressed = NxpPressedState(
            scale = 0.90f,
            fillColor = "#F59E0B",
            borderColor = "#FFFFFF"
        ),
        label = NxpLabel(
            text = "M1",
            color = "#F59E0B",
            pressedColor = "#000000",
            fontSize = 13f
        ),
        size = NxpSize(widthDp = 58, heightDp = 32)
    )

    val ALL_PRESETS = listOf(
        SCIFI_HEX_ATTACK,
        CYBER_OCTA_BURST,
        NEON_DIAMOND_X,
        PLASMA_TRIANGLE_Y,
        CYBER_BUMPER_LB,
        CYBER_BUMPER_RB,
        PULSE_TRIGGER_LT,
        PULSE_TRIGGER_RT,
        NEON_MATRIX_JOYSTICK,
        CYBER_VORTEX_RS,
        HOLO_CROSS_DPAD,
        NEXUS_ORB_HOME,
        TACTICAL_SLIM_MENU,
        MACRO_PILL_M1
    )
}
