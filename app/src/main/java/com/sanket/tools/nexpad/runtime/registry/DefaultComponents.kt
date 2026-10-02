package com.sanket.tools.nexpad.runtime.registry

import com.sanket.tools.nexpad.runtime.model.*
import com.sanket.tools.nexpad.category.ControlKey
import com.sanket.tools.nexpad.nxprc.NxprcCategory

object DefaultComponents {

    val SCIFI_HEX_ATTACK = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.scifi_hex_a",
            name = "Sci-Fi Hex Attack",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.BUTTON.id,
            defaultControl = ControlKey.A.key,
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
            category = NxprcCategory.BUTTON.id,
            defaultControl = ControlKey.B.key,
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

    val SCIFI_HEX_Y = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.scifi_hex_y",
            name = "Sci-Fi Hex Y",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.BUTTON.id,
            defaultControl = ControlKey.Y.key,
            description = "Canary yellow holographic hexagonal action button."
        ),
        geometry = NxpGeometry(type = "Polygon", sides = 6, cornerRadius = 10f),
        visual = NxpVisual(
            fillColor = "#261F00",
            opacity = 0.90f,
            borderColor = "#FFD600",
            borderWidth = 2.5f,
            glowColor = "#FFD600",
            glowRadius = 12f
        ),
        pressed = NxpPressedState(
            scale = 0.86f,
            rotation = 3f,
            fillColor = "#FFD600",
            borderColor = "#FFFFFF",
            glowRadius = 20f,
            springDamping = 0.55f,
            springStiffness = 750f
        ),
        label = NxpLabel(
            text = "Y",
            color = "#FFD600",
            pressedColor = "#000000",
            fontSize = 24f
        ),
        size = NxpSize(widthDp = 76, heightDp = 76)
    )

    val SCIFI_HEX_X = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.scifi_hex_x",
            name = "Sci-Fi Hex X",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.BUTTON.id,
            defaultControl = ControlKey.X.key,
            description = "Cobalt blue holographic hexagonal action button."
        ),
        geometry = NxpGeometry(type = "Polygon", sides = 6, cornerRadius = 10f),
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
            rotation = -3f,
            fillColor = "#00B4D8",
            borderColor = "#FFFFFF",
            glowRadius = 20f,
            springDamping = 0.55f,
            springStiffness = 750f
        ),
        label = NxpLabel(
            text = "X",
            color = "#00B4D8",
            pressedColor = "#0D1B2A",
            fontSize = 24f
        ),
        size = NxpSize(widthDp = 76, heightDp = 76)
    )

    val SCIFI_HEX_B = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.scifi_hex_b",
            name = "Sci-Fi Hex B",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.BUTTON.id,
            defaultControl = ControlKey.B.key,
            description = "Crimson red holographic hexagonal action button."
        ),
        geometry = NxpGeometry(type = "Polygon", sides = 6, cornerRadius = 10f),
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

    val CYBER_OCTA_Y = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.cyber_octa_y",
            name = "Cyber Octa Y",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.BUTTON.id,
            defaultControl = ControlKey.Y.key,
            description = "Neon amber octagonal heavy action button."
        ),
        geometry = NxpGeometry(type = "Polygon", sides = 8, cornerRadius = 8f),
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
            pressedColor = "#000000",
            fontSize = 24f
        ),
        size = NxpSize(widthDp = 76, heightDp = 76)
    )

    val CYBER_OCTA_X = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.cyber_octa_x",
            name = "Cyber Octa X",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.BUTTON.id,
            defaultControl = ControlKey.X.key,
            description = "Neon sapphire octagonal action button."
        ),
        geometry = NxpGeometry(type = "Polygon", sides = 8, cornerRadius = 8f),
        visual = NxpVisual(
            fillColor = "#0B1528",
            opacity = 0.90f,
            borderColor = "#3A86FF",
            borderWidth = 2.5f,
            glowColor = "#3A86FF",
            glowRadius = 12f
        ),
        pressed = NxpPressedState(
            scale = 0.86f,
            fillColor = "#3A86FF",
            borderColor = "#FFFFFF",
            glowRadius = 20f
        ),
        label = NxpLabel(
            text = "X",
            color = "#3A86FF",
            pressedColor = "#0B1528",
            fontSize = 24f
        ),
        size = NxpSize(widthDp = 76, heightDp = 76)
    )

    val CYBER_OCTA_A = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.cyber_octa_a",
            name = "Cyber Octa A",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.BUTTON.id,
            defaultControl = ControlKey.A.key,
            description = "Neon turquoise octagonal primary action button."
        ),
        geometry = NxpGeometry(type = "Polygon", sides = 8, cornerRadius = 8f),
        visual = NxpVisual(
            fillColor = "#05201A",
            opacity = 0.90f,
            borderColor = "#00F5D4",
            borderWidth = 2.5f,
            glowColor = "#00F5D4",
            glowRadius = 12f
        ),
        pressed = NxpPressedState(
            scale = 0.86f,
            fillColor = "#00F5D4",
            borderColor = "#FFFFFF",
            glowRadius = 20f
        ),
        label = NxpLabel(
            text = "A",
            color = "#00F5D4",
            pressedColor = "#05201A",
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
            category = NxprcCategory.JOYSTICK.id,
            defaultControl = ControlKey.LS.key,
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
            category = NxprcCategory.BUTTON.id,
            defaultControl = ControlKey.X.key,
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
            category = NxprcCategory.BUTTON.id,
            defaultControl = ControlKey.Y.key,
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
            category = NxprcCategory.BUMPER.id,
            defaultControl = ControlKey.LB.key,
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
            category = NxprcCategory.BUMPER.id,
            defaultControl = ControlKey.RB.key,
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

    val NEON_CYAN_BUMPER_LB = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.neon_cyan_bumper_lb",
            name = "Neon Cyan Bumper LB",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.BUMPER.id,
            defaultControl = ControlKey.LB.key,
            description = "High-tech cyan glowing shoulder bumper with precision microswitch tactile response."
        ),
        geometry = NxpGeometry(type = "RoundedRect", cornerRadius = 14f),
        visual = NxpVisual(
            fillColor = "#061A2B",
            opacity = 0.92f,
            borderColor = "#00F0FF",
            borderWidth = 2.5f,
            glowColor = "#00F0FF",
            glowRadius = 12f
        ),
        pressed = NxpPressedState(scale = 0.92f, fillColor = "#00F0FF", borderColor = "#FFFFFF", glowRadius = 20f),
        label = NxpLabel(text = "LB", color = "#00F0FF", pressedColor = "#000000", fontSize = 18f),
        size = NxpSize(widthDp = 104, heightDp = 50)
    )

    val NEON_CYAN_BUMPER_RB = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.neon_cyan_bumper_rb",
            name = "Neon Cyan Bumper RB",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.BUMPER.id,
            defaultControl = ControlKey.RB.key,
            description = "High-tech cyan glowing shoulder bumper with precision microswitch tactile response."
        ),
        geometry = NxpGeometry(type = "RoundedRect", cornerRadius = 14f),
        visual = NxpVisual(
            fillColor = "#061A2B",
            opacity = 0.92f,
            borderColor = "#00F0FF",
            borderWidth = 2.5f,
            glowColor = "#00F0FF",
            glowRadius = 12f
        ),
        pressed = NxpPressedState(scale = 0.92f, fillColor = "#00F0FF", borderColor = "#FFFFFF", glowRadius = 20f),
        label = NxpLabel(text = "RB", color = "#00F0FF", pressedColor = "#000000", fontSize = 18f),
        size = NxpSize(widthDp = 104, heightDp = 50)
    )

    val STEALTH_CARBON_LB = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.stealth_carbon_lb",
            name = "Stealth Carbon LB",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.BUMPER.id,
            defaultControl = ControlKey.LB.key,
            description = "Matte carbon weave bumper with subtle sky blue chamfered accent lines."
        ),
        geometry = NxpGeometry(type = "RoundedRect", cornerRadius = 12f),
        visual = NxpVisual(
            fillColor = "#1E293B",
            opacity = 0.94f,
            borderColor = "#38BDF8",
            borderWidth = 2f,
            glowColor = "#38BDF8",
            glowRadius = 8f
        ),
        pressed = NxpPressedState(scale = 0.93f, fillColor = "#38BDF8", borderColor = "#FFFFFF"),
        label = NxpLabel(text = "LB", color = "#E2E8F0", pressedColor = "#0F172A", fontSize = 17f),
        size = NxpSize(widthDp = 104, heightDp = 50)
    )

    val STEALTH_CARBON_RB = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.stealth_carbon_rb",
            name = "Stealth Carbon RB",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.BUMPER.id,
            defaultControl = ControlKey.RB.key,
            description = "Matte carbon weave bumper with subtle sky blue chamfered accent lines."
        ),
        geometry = NxpGeometry(type = "RoundedRect", cornerRadius = 12f),
        visual = NxpVisual(
            fillColor = "#1E293B",
            opacity = 0.94f,
            borderColor = "#38BDF8",
            borderWidth = 2f,
            glowColor = "#38BDF8",
            glowRadius = 8f
        ),
        pressed = NxpPressedState(scale = 0.93f, fillColor = "#38BDF8", borderColor = "#FFFFFF"),
        label = NxpLabel(text = "RB", color = "#E2E8F0", pressedColor = "#0F172A", fontSize = 17f),
        size = NxpSize(widthDp = 104, heightDp = 50)
    )

    val CRIMSON_MECHA_LB = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.crimson_mecha_lb",
            name = "Crimson Mecha LB",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.BUMPER.id,
            defaultControl = ControlKey.LB.key,
            description = "Aggressive crimson mecha-style bumper with high-intensity warning edge."
        ),
        geometry = NxpGeometry(type = "RoundedRect", cornerRadius = 14f),
        visual = NxpVisual(
            fillColor = "#1F0A12",
            opacity = 0.92f,
            borderColor = "#F43F5E",
            borderWidth = 2.5f,
            glowColor = "#F43F5E",
            glowRadius = 12f
        ),
        pressed = NxpPressedState(scale = 0.92f, fillColor = "#F43F5E", borderColor = "#FFFFFF", glowRadius = 20f),
        label = NxpLabel(text = "LB", color = "#FDA4AF", pressedColor = "#1F0A12", fontSize = 18f),
        size = NxpSize(widthDp = 104, heightDp = 50)
    )

    val CRIMSON_MECHA_RB = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.crimson_mecha_rb",
            name = "Crimson Mecha RB",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.BUMPER.id,
            defaultControl = ControlKey.RB.key,
            description = "Aggressive crimson mecha-style bumper with high-intensity warning edge."
        ),
        geometry = NxpGeometry(type = "RoundedRect", cornerRadius = 14f),
        visual = NxpVisual(
            fillColor = "#1F0A12",
            opacity = 0.92f,
            borderColor = "#F43F5E",
            borderWidth = 2.5f,
            glowColor = "#F43F5E",
            glowRadius = 12f
        ),
        pressed = NxpPressedState(scale = 0.92f, fillColor = "#F43F5E", borderColor = "#FFFFFF", glowRadius = 20f),
        label = NxpLabel(text = "RB", color = "#FDA4AF", pressedColor = "#1F0A12", fontSize = 18f),
        size = NxpSize(widthDp = 104, heightDp = 50)
    )

    val NEON_PULSE_B = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.neon_pulse_b",
            name = "Neon Pulse B",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.BUTTON.id,
            defaultControl = ControlKey.B.key,
            description = "Futuristic neon crimson circle action button."
        ),
        geometry = NxpGeometry(type = "Circle", cornerRadius = 38f),
        visual = NxpVisual(
            fillColor = "#1A050B",
            opacity = 0.92f,
            borderColor = "#FF1744",
            borderWidth = 2.5f,
            glowColor = "#FF1744",
            glowRadius = 12f
        ),
        pressed = NxpPressedState(scale = 0.88f, fillColor = "#FF1744", borderColor = "#FFFFFF"),
        label = NxpLabel(text = "B", color = "#FF1744", pressedColor = "#FFFFFF", fontSize = 24f),
        size = NxpSize(widthDp = 76, heightDp = 76)
    )

    val PULSE_TRIGGER_LT = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.pulse_trigger_lt",
            name = "Impulse Trigger LT",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.TRIGGER.id,
            defaultControl = ControlKey.LT.key,
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
            category = NxprcCategory.TRIGGER.id,
            defaultControl = ControlKey.RT.key,
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
            category = NxprcCategory.JOYSTICK.id,
            defaultControl = ControlKey.RS.key,
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
            category = NxprcCategory.DPAD.id,
            defaultControl = ControlKey.DPAD.key,
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
        size = NxpSize(widthDp = 140, heightDp = 140)
    )

    val CYBER_DPAD_UP = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.cyber_dpad_up",
            name = "Cyber D-Pad Up",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.DPAD.id,
            defaultControl = ControlKey.UP.key,
            description = "Neon turquoise directional UP cap button."
        ),
        geometry = NxpGeometry(type = "RoundedRect", cornerRadius = 12f),
        visual = NxpVisual(
            fillColor = "#0B1A24",
            opacity = 0.90f,
            borderColor = "#2DD4BF",
            borderWidth = 2f,
            glowColor = "#2DD4BF",
            glowRadius = 10f
        ),
        pressed = NxpPressedState(scale = 0.88f, fillColor = "#2DD4BF", borderColor = "#FFFFFF"),
        label = NxpLabel(text = "▲", color = "#2DD4BF", pressedColor = "#000000", fontSize = 22f),
        size = NxpSize(widthDp = 72, heightDp = 72)
    )

    val CYBER_DPAD_DOWN = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.cyber_dpad_down",
            name = "Cyber D-Pad Down",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.DPAD.id,
            defaultControl = ControlKey.DOWN.key,
            description = "Neon turquoise directional DOWN cap button."
        ),
        geometry = NxpGeometry(type = "RoundedRect", cornerRadius = 12f),
        visual = NxpVisual(
            fillColor = "#0B1A24",
            opacity = 0.90f,
            borderColor = "#2DD4BF",
            borderWidth = 2f,
            glowColor = "#2DD4BF",
            glowRadius = 10f
        ),
        pressed = NxpPressedState(scale = 0.88f, fillColor = "#2DD4BF", borderColor = "#FFFFFF"),
        label = NxpLabel(text = "▼", color = "#2DD4BF", pressedColor = "#000000", fontSize = 22f),
        size = NxpSize(widthDp = 72, heightDp = 72)
    )

    val CYBER_DPAD_LEFT = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.cyber_dpad_left",
            name = "Cyber D-Pad Left",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.DPAD.id,
            defaultControl = ControlKey.LEFT.key,
            description = "Neon turquoise directional LEFT cap button."
        ),
        geometry = NxpGeometry(type = "RoundedRect", cornerRadius = 12f),
        visual = NxpVisual(
            fillColor = "#0B1A24",
            opacity = 0.90f,
            borderColor = "#2DD4BF",
            borderWidth = 2f,
            glowColor = "#2DD4BF",
            glowRadius = 10f
        ),
        pressed = NxpPressedState(scale = 0.88f, fillColor = "#2DD4BF", borderColor = "#FFFFFF"),
        label = NxpLabel(text = "◀", color = "#2DD4BF", pressedColor = "#000000", fontSize = 22f),
        size = NxpSize(widthDp = 72, heightDp = 72)
    )

    val CYBER_DPAD_RIGHT = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.cyber_dpad_right",
            name = "Cyber D-Pad Right",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.DPAD.id,
            defaultControl = ControlKey.RIGHT.key,
            description = "Neon turquoise directional RIGHT cap button."
        ),
        geometry = NxpGeometry(type = "RoundedRect", cornerRadius = 12f),
        visual = NxpVisual(
            fillColor = "#0B1A24",
            opacity = 0.90f,
            borderColor = "#2DD4BF",
            borderWidth = 2f,
            glowColor = "#2DD4BF",
            glowRadius = 10f
        ),
        pressed = NxpPressedState(scale = 0.88f, fillColor = "#2DD4BF", borderColor = "#FFFFFF"),
        label = NxpLabel(text = "▶", color = "#2DD4BF", pressedColor = "#000000", fontSize = 22f),
        size = NxpSize(widthDp = 72, heightDp = 72)
    )

    val NEXUS_ORB_HOME = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.nexus_orb_home",
            name = "Nexus Core Home Guide",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.SYSTEM.id,
            defaultControl = ControlKey.GUIDE.key,
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
            text = "⨂",
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
            category = NxprcCategory.SYSTEM.id,
            defaultControl = ControlKey.START.key,
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
            category = NxprcCategory.MACRO.id,
            defaultControl = ControlKey.M1.key,
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

    // =========================================================================
    // 19 DEFAULT CONTROLLER BUTTONS (Authentic Realistic Xbox Elite Elements)
    // Non-deletable core system controls matching the physical controller layout.
    // =========================================================================

    val DEFAULT_BUTTON_A = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.default_a",
            name = "Default Button A (Green)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.BUTTON.id,
            defaultControl = ControlKey.A.key,
            description = "Standard tactile action button with emerald green illumination."
        ),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(
            fillColor = "#1F1F1F",
            opacity = 0.95f,
            borderColor = "#00C853",
            borderWidth = 2.5f,
            glowColor = "#00C853",
            glowRadius = 10f
        ),
        pressed = NxpPressedState(
            scale = 0.88f,
            fillColor = "#00C853",
            borderColor = "#FFFFFF",
            glowRadius = 18f
        ),
        label = NxpLabel(
            text = "A",
            color = "#00C853",
            pressedColor = "#000000",
            fontSize = 28f
        ),
        size = NxpSize(widthDp = 80, heightDp = 80)
    )

    val DEFAULT_BUTTON_B = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.default_b",
            name = "Default Button B (Red)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.BUTTON.id,
            defaultControl = ControlKey.B.key,
            description = "Standard tactile action button with crimson red illumination."
        ),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(
            fillColor = "#1F1F1F",
            opacity = 0.95f,
            borderColor = "#D50000",
            borderWidth = 2.5f,
            glowColor = "#D50000",
            glowRadius = 10f
        ),
        pressed = NxpPressedState(
            scale = 0.88f,
            fillColor = "#D50000",
            borderColor = "#FFFFFF",
            glowRadius = 18f
        ),
        label = NxpLabel(
            text = "B",
            color = "#D50000",
            pressedColor = "#FFFFFF",
            fontSize = 28f
        ),
        size = NxpSize(widthDp = 80, heightDp = 80)
    )

    val DEFAULT_BUTTON_X = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.default_x",
            name = "Default Button X (Blue)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.BUTTON.id,
            defaultControl = ControlKey.X.key,
            description = "Standard tactile action button with cobalt blue illumination."
        ),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(
            fillColor = "#1F1F1F",
            opacity = 0.95f,
            borderColor = "#2962FF",
            borderWidth = 2.5f,
            glowColor = "#2962FF",
            glowRadius = 10f
        ),
        pressed = NxpPressedState(
            scale = 0.88f,
            fillColor = "#2962FF",
            borderColor = "#FFFFFF",
            glowRadius = 18f
        ),
        label = NxpLabel(
            text = "X",
            color = "#2962FF",
            pressedColor = "#FFFFFF",
            fontSize = 28f
        ),
        size = NxpSize(widthDp = 80, heightDp = 80)
    )

    val DEFAULT_BUTTON_Y = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.default_y",
            name = "Default Button Y (Yellow)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.BUTTON.id,
            defaultControl = ControlKey.Y.key,
            description = "Standard tactile action button with canary yellow illumination."
        ),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(
            fillColor = "#1F1F1F",
            opacity = 0.95f,
            borderColor = "#FFD600",
            borderWidth = 2.5f,
            glowColor = "#FFD600",
            glowRadius = 10f
        ),
        pressed = NxpPressedState(
            scale = 0.88f,
            fillColor = "#FFD600",
            borderColor = "#FFFFFF",
            glowRadius = 18f
        ),
        label = NxpLabel(
            text = "Y",
            color = "#FFD600",
            pressedColor = "#000000",
            fontSize = 28f
        ),
        size = NxpSize(widthDp = 80, heightDp = 80)
    )

    // ── LIQUID FILL BUTTON ─────────────────────────────────────────────────────
    val LIQUID_BUTTON_A = NxpComponentDef(
        manifest = NxpManifest(id = "builtin.liq_a", name = "Liquid Fill Button A (Green)", author = "NEXPAD Core", version = "1.0.0", category = NxprcCategory.BUTTON.id, defaultControl = ControlKey.A.key, description = "Fluid-fill ABXY button with green liquid animation."),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(fillColor = "#1A1A1A", opacity = 0.95f, borderColor = "#3FD25A", borderWidth = 2.5f, glowColor = "#3FD25A", glowRadius = 10f),
        pressed = NxpPressedState(scale = 0.95f, fillColor = "#3FD25A", borderColor = "#FFFFFF", glowRadius = 18f),
        label = NxpLabel(text = "A", color = "#3FD25A", pressedColor = "#000000", fontSize = 28f),
        size = NxpSize(widthDp = 80, heightDp = 80)
    )
    val LIQUID_BUTTON_B = NxpComponentDef(
        manifest = NxpManifest(id = "builtin.liq_b", name = "Liquid Fill Button B (Red)", author = "NEXPAD Core", version = "1.0.0", category = NxprcCategory.BUTTON.id, defaultControl = ControlKey.B.key, description = "Fluid-fill ABXY button with red liquid animation."),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(fillColor = "#1A1A1A", opacity = 0.95f, borderColor = "#E6474E", borderWidth = 2.5f, glowColor = "#E6474E", glowRadius = 10f),
        pressed = NxpPressedState(scale = 0.95f, fillColor = "#E6474E", borderColor = "#FFFFFF", glowRadius = 18f),
        label = NxpLabel(text = "B", color = "#E6474E", pressedColor = "#000000", fontSize = 28f),
        size = NxpSize(widthDp = 80, heightDp = 80)
    )
    val LIQUID_BUTTON_X = NxpComponentDef(
        manifest = NxpManifest(id = "builtin.liq_x", name = "Liquid Fill Button X (Blue)", author = "NEXPAD Core", version = "1.0.0", category = NxprcCategory.BUTTON.id, defaultControl = ControlKey.X.key, description = "Fluid-fill ABXY button with blue liquid animation."),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(fillColor = "#1A1A1A", opacity = 0.95f, borderColor = "#3F8FE0", borderWidth = 2.5f, glowColor = "#3F8FE0", glowRadius = 10f),
        pressed = NxpPressedState(scale = 0.95f, fillColor = "#3F8FE0", borderColor = "#FFFFFF", glowRadius = 18f),
        label = NxpLabel(text = "X", color = "#3F8FE0", pressedColor = "#000000", fontSize = 28f),
        size = NxpSize(widthDp = 80, heightDp = 80)
    )
    val LIQUID_BUTTON_Y = NxpComponentDef(
        manifest = NxpManifest(id = "builtin.liq_y", name = "Liquid Fill Button Y (Yellow)", author = "NEXPAD Core", version = "1.0.0", category = NxprcCategory.BUTTON.id, defaultControl = ControlKey.Y.key, description = "Fluid-fill ABXY button with yellow liquid animation."),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(fillColor = "#1A1A1A", opacity = 0.95f, borderColor = "#E0A03F", borderWidth = 2.5f, glowColor = "#E0A03F", glowRadius = 10f),
        pressed = NxpPressedState(scale = 0.95f, fillColor = "#E0A03F", borderColor = "#FFFFFF", glowRadius = 18f),
        label = NxpLabel(text = "Y", color = "#E0A03F", pressedColor = "#000000", fontSize = 28f),
        size = NxpSize(widthDp = 80, heightDp = 80)
    )

    // ── FACET GEM BUTTON ───────────────────────────────────────────────────────
    val FACET_BUTTON_A = NxpComponentDef(
        manifest = NxpManifest(id = "builtin.facet_a", name = "Facet Gem Button A (Green)", author = "NEXPAD Core", version = "1.0.0", category = NxprcCategory.BUTTON.id, defaultControl = ControlKey.A.key, description = "Crystalline faceted gem button with green facets."),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(fillColor = "#1A1A1A", opacity = 0.95f, borderColor = "#3FD25A", borderWidth = 2.5f, glowColor = "#3FD25A", glowRadius = 10f),
        pressed = NxpPressedState(scale = 0.95f, fillColor = "#3FD25A", borderColor = "#FFFFFF", glowRadius = 18f),
        label = NxpLabel(text = "A", color = "#3FD25A", pressedColor = "#000000", fontSize = 28f),
        size = NxpSize(widthDp = 80, heightDp = 80)
    )
    val FACET_BUTTON_B = NxpComponentDef(
        manifest = NxpManifest(id = "builtin.facet_b", name = "Facet Gem Button B (Red)", author = "NEXPAD Core", version = "1.0.0", category = NxprcCategory.BUTTON.id, defaultControl = ControlKey.B.key, description = "Crystalline faceted gem button with red facets."),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(fillColor = "#1A1A1A", opacity = 0.95f, borderColor = "#E6474E", borderWidth = 2.5f, glowColor = "#E6474E", glowRadius = 10f),
        pressed = NxpPressedState(scale = 0.95f, fillColor = "#E6474E", borderColor = "#FFFFFF", glowRadius = 18f),
        label = NxpLabel(text = "B", color = "#E6474E", pressedColor = "#000000", fontSize = 28f),
        size = NxpSize(widthDp = 80, heightDp = 80)
    )
    val FACET_BUTTON_X = NxpComponentDef(
        manifest = NxpManifest(id = "builtin.facet_x", name = "Facet Gem Button X (Blue)", author = "NEXPAD Core", version = "1.0.0", category = NxprcCategory.BUTTON.id, defaultControl = ControlKey.X.key, description = "Crystalline faceted gem button with blue facets."),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(fillColor = "#1A1A1A", opacity = 0.95f, borderColor = "#3F8FE0", borderWidth = 2.5f, glowColor = "#3F8FE0", glowRadius = 10f),
        pressed = NxpPressedState(scale = 0.95f, fillColor = "#3F8FE0", borderColor = "#FFFFFF", glowRadius = 18f),
        label = NxpLabel(text = "X", color = "#3F8FE0", pressedColor = "#000000", fontSize = 28f),
        size = NxpSize(widthDp = 80, heightDp = 80)
    )
    val FACET_BUTTON_Y = NxpComponentDef(
        manifest = NxpManifest(id = "builtin.facet_y", name = "Facet Gem Button Y (Yellow)", author = "NEXPAD Core", version = "1.0.0", category = NxprcCategory.BUTTON.id, defaultControl = ControlKey.Y.key, description = "Crystalline faceted gem button with yellow facets."),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(fillColor = "#1A1A1A", opacity = 0.95f, borderColor = "#E0A03F", borderWidth = 2.5f, glowColor = "#E0A03F", glowRadius = 10f),
        pressed = NxpPressedState(scale = 0.95f, fillColor = "#E0A03F", borderColor = "#FFFFFF", glowRadius = 18f),
        label = NxpLabel(text = "Y", color = "#E0A03F", pressedColor = "#000000", fontSize = 28f),
        size = NxpSize(widthDp = 80, heightDp = 80)
    )

    // ── FLIP CARD BUTTON ──────────────────────────────────────────────────────
    val FLIP_BUTTON_A = NxpComponentDef(
        manifest = NxpManifest(id = "builtin.flipbtn_a", name = "Flip Card Button A (Green)", author = "NEXPAD Core", version = "1.0.0", category = NxprcCategory.BUTTON.id, defaultControl = ControlKey.A.key, description = "3D card-flip button revealing green face on press."),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(fillColor = "#1A1A1A", opacity = 0.95f, borderColor = "#3FD25A", borderWidth = 2.5f, glowColor = "#3FD25A", glowRadius = 10f),
        pressed = NxpPressedState(scale = 0.95f, fillColor = "#3FD25A", borderColor = "#FFFFFF", glowRadius = 18f),
        label = NxpLabel(text = "A", color = "#3FD25A", pressedColor = "#000000", fontSize = 28f),
        size = NxpSize(widthDp = 80, heightDp = 80)
    )
    val FLIP_BUTTON_B = NxpComponentDef(
        manifest = NxpManifest(id = "builtin.flipbtn_b", name = "Flip Card Button B (Red)", author = "NEXPAD Core", version = "1.0.0", category = NxprcCategory.BUTTON.id, defaultControl = ControlKey.B.key, description = "3D card-flip button revealing red face on press."),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(fillColor = "#1A1A1A", opacity = 0.95f, borderColor = "#E6474E", borderWidth = 2.5f, glowColor = "#E6474E", glowRadius = 10f),
        pressed = NxpPressedState(scale = 0.95f, fillColor = "#E6474E", borderColor = "#FFFFFF", glowRadius = 18f),
        label = NxpLabel(text = "B", color = "#E6474E", pressedColor = "#000000", fontSize = 28f),
        size = NxpSize(widthDp = 80, heightDp = 80)
    )
    val FLIP_BUTTON_X = NxpComponentDef(
        manifest = NxpManifest(id = "builtin.flipbtn_x", name = "Flip Card Button X (Blue)", author = "NEXPAD Core", version = "1.0.0", category = NxprcCategory.BUTTON.id, defaultControl = ControlKey.X.key, description = "3D card-flip button revealing blue face on press."),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(fillColor = "#1A1A1A", opacity = 0.95f, borderColor = "#3F8FE0", borderWidth = 2.5f, glowColor = "#3F8FE0", glowRadius = 10f),
        pressed = NxpPressedState(scale = 0.95f, fillColor = "#3F8FE0", borderColor = "#FFFFFF", glowRadius = 18f),
        label = NxpLabel(text = "X", color = "#3F8FE0", pressedColor = "#000000", fontSize = 28f),
        size = NxpSize(widthDp = 80, heightDp = 80)
    )
    val FLIP_BUTTON_Y = NxpComponentDef(
        manifest = NxpManifest(id = "builtin.flipbtn_y", name = "Flip Card Button Y (Yellow)", author = "NEXPAD Core", version = "1.0.0", category = NxprcCategory.BUTTON.id, defaultControl = ControlKey.Y.key, description = "3D card-flip button revealing yellow face on press."),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(fillColor = "#1A1A1A", opacity = 0.95f, borderColor = "#E0A03F", borderWidth = 2.5f, glowColor = "#E0A03F", glowRadius = 10f),
        pressed = NxpPressedState(scale = 0.95f, fillColor = "#E0A03F", borderColor = "#FFFFFF", glowRadius = 18f),
        label = NxpLabel(text = "Y", color = "#E0A03F", pressedColor = "#000000", fontSize = 28f),
        size = NxpSize(widthDp = 80, heightDp = 80)
    )

    // ── RIPPLE RINGS BUTTON ───────────────────────────────────────────────────
    val RIPPLE_BUTTON_A = NxpComponentDef(
        manifest = NxpManifest(id = "builtin.ripple_a", name = "Ripple Rings Button A (Green)", author = "NEXPAD Core", version = "1.0.0", category = NxprcCategory.BUTTON.id, defaultControl = ControlKey.A.key, description = "Expanding ripple rings button with green accent."),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(fillColor = "#1A1A1A", opacity = 0.95f, borderColor = "#3FD25A", borderWidth = 2.5f, glowColor = "#3FD25A", glowRadius = 10f),
        pressed = NxpPressedState(scale = 0.95f, fillColor = "#3FD25A", borderColor = "#FFFFFF", glowRadius = 18f),
        label = NxpLabel(text = "A", color = "#3FD25A", pressedColor = "#000000", fontSize = 28f),
        size = NxpSize(widthDp = 80, heightDp = 80)
    )
    val RIPPLE_BUTTON_B = NxpComponentDef(
        manifest = NxpManifest(id = "builtin.ripple_b", name = "Ripple Rings Button B (Red)", author = "NEXPAD Core", version = "1.0.0", category = NxprcCategory.BUTTON.id, defaultControl = ControlKey.B.key, description = "Expanding ripple rings button with red accent."),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(fillColor = "#1A1A1A", opacity = 0.95f, borderColor = "#E6474E", borderWidth = 2.5f, glowColor = "#E6474E", glowRadius = 10f),
        pressed = NxpPressedState(scale = 0.95f, fillColor = "#E6474E", borderColor = "#FFFFFF", glowRadius = 18f),
        label = NxpLabel(text = "B", color = "#E6474E", pressedColor = "#000000", fontSize = 28f),
        size = NxpSize(widthDp = 80, heightDp = 80)
    )
    val RIPPLE_BUTTON_X = NxpComponentDef(
        manifest = NxpManifest(id = "builtin.ripple_x", name = "Ripple Rings Button X (Blue)", author = "NEXPAD Core", version = "1.0.0", category = NxprcCategory.BUTTON.id, defaultControl = ControlKey.X.key, description = "Expanding ripple rings button with blue accent."),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(fillColor = "#1A1A1A", opacity = 0.95f, borderColor = "#3F8FE0", borderWidth = 2.5f, glowColor = "#3F8FE0", glowRadius = 10f),
        pressed = NxpPressedState(scale = 0.95f, fillColor = "#3F8FE0", borderColor = "#FFFFFF", glowRadius = 18f),
        label = NxpLabel(text = "X", color = "#3F8FE0", pressedColor = "#000000", fontSize = 28f),
        size = NxpSize(widthDp = 80, heightDp = 80)
    )
    val RIPPLE_BUTTON_Y = NxpComponentDef(
        manifest = NxpManifest(id = "builtin.ripple_y", name = "Ripple Rings Button Y (Yellow)", author = "NEXPAD Core", version = "1.0.0", category = NxprcCategory.BUTTON.id, defaultControl = ControlKey.Y.key, description = "Expanding ripple rings button with yellow accent."),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(fillColor = "#1A1A1A", opacity = 0.95f, borderColor = "#E0A03F", borderWidth = 2.5f, glowColor = "#E0A03F", glowRadius = 10f),
        pressed = NxpPressedState(scale = 0.95f, fillColor = "#E0A03F", borderColor = "#FFFFFF", glowRadius = 18f),
        label = NxpLabel(text = "Y", color = "#E0A03F", pressedColor = "#000000", fontSize = 28f),
        size = NxpSize(widthDp = 80, heightDp = 80)
    )

    // ── ORBIT RINGS BUTTON ────────────────────────────────────────────────────
    val ORBIT_BUTTON_A = NxpComponentDef(
        manifest = NxpManifest(id = "builtin.orbit_a", name = "Orbit Rings Button A (Green)", author = "NEXPAD Core", version = "1.0.0", category = NxprcCategory.BUTTON.id, defaultControl = ControlKey.A.key, description = "Orbiting satellite rings button with green accent."),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(fillColor = "#1A1A1A", opacity = 0.95f, borderColor = "#3FD25A", borderWidth = 2.5f, glowColor = "#3FD25A", glowRadius = 10f),
        pressed = NxpPressedState(scale = 0.95f, fillColor = "#3FD25A", borderColor = "#FFFFFF", glowRadius = 18f),
        label = NxpLabel(text = "A", color = "#3FD25A", pressedColor = "#000000", fontSize = 28f),
        size = NxpSize(widthDp = 80, heightDp = 80)
    )
    val ORBIT_BUTTON_B = NxpComponentDef(
        manifest = NxpManifest(id = "builtin.orbit_b", name = "Orbit Rings Button B (Red)", author = "NEXPAD Core", version = "1.0.0", category = NxprcCategory.BUTTON.id, defaultControl = ControlKey.B.key, description = "Orbiting satellite rings button with red accent."),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(fillColor = "#1A1A1A", opacity = 0.95f, borderColor = "#E6474E", borderWidth = 2.5f, glowColor = "#E6474E", glowRadius = 10f),
        pressed = NxpPressedState(scale = 0.95f, fillColor = "#E6474E", borderColor = "#FFFFFF", glowRadius = 18f),
        label = NxpLabel(text = "B", color = "#E6474E", pressedColor = "#000000", fontSize = 28f),
        size = NxpSize(widthDp = 80, heightDp = 80)
    )
    val ORBIT_BUTTON_X = NxpComponentDef(
        manifest = NxpManifest(id = "builtin.orbit_x", name = "Orbit Rings Button X (Blue)", author = "NEXPAD Core", version = "1.0.0", category = NxprcCategory.BUTTON.id, defaultControl = ControlKey.X.key, description = "Orbiting satellite rings button with blue accent."),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(fillColor = "#1A1A1A", opacity = 0.95f, borderColor = "#3F8FE0", borderWidth = 2.5f, glowColor = "#3F8FE0", glowRadius = 10f),
        pressed = NxpPressedState(scale = 0.95f, fillColor = "#3F8FE0", borderColor = "#FFFFFF", glowRadius = 18f),
        label = NxpLabel(text = "X", color = "#3F8FE0", pressedColor = "#000000", fontSize = 28f),
        size = NxpSize(widthDp = 80, heightDp = 80)
    )
    val ORBIT_BUTTON_Y = NxpComponentDef(
        manifest = NxpManifest(id = "builtin.orbit_y", name = "Orbit Rings Button Y (Yellow)", author = "NEXPAD Core", version = "1.0.0", category = NxprcCategory.BUTTON.id, defaultControl = ControlKey.Y.key, description = "Orbiting satellite rings button with yellow accent."),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(fillColor = "#1A1A1A", opacity = 0.95f, borderColor = "#E0A03F", borderWidth = 2.5f, glowColor = "#E0A03F", glowRadius = 10f),
        pressed = NxpPressedState(scale = 0.95f, fillColor = "#E0A03F", borderColor = "#FFFFFF", glowRadius = 18f),
        label = NxpLabel(text = "Y", color = "#E0A03F", pressedColor = "#000000", fontSize = 28f),
        size = NxpSize(widthDp = 80, heightDp = 80)
    )

    // ── CAPSULE FILL BUTTON ───────────────────────────────────────────────────
    val CAPSULES_BUTTON_A = NxpComponentDef(
        manifest = NxpManifest(id = "builtin.caps_a", name = "Capsule Fill Button A (Green)", author = "NEXPAD Core", version = "1.0.0", category = NxprcCategory.BUTTON.id, defaultControl = ControlKey.A.key, description = "Pill-shaped capsule button flooding green from outer end."),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(fillColor = "#1A1A1A", opacity = 0.95f, borderColor = "#3FD25A", borderWidth = 2.5f, glowColor = "#3FD25A", glowRadius = 10f),
        pressed = NxpPressedState(scale = 0.95f, fillColor = "#3FD25A", borderColor = "#FFFFFF", glowRadius = 18f),
        label = NxpLabel(text = "A", color = "#3FD25A", pressedColor = "#000000", fontSize = 28f),
        size = NxpSize(widthDp = 80, heightDp = 80)
    )
    val CAPSULES_BUTTON_B = NxpComponentDef(
        manifest = NxpManifest(id = "builtin.caps_b", name = "Capsule Fill Button B (Red)", author = "NEXPAD Core", version = "1.0.0", category = NxprcCategory.BUTTON.id, defaultControl = ControlKey.B.key, description = "Pill-shaped capsule button flooding red from outer end."),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(fillColor = "#1A1A1A", opacity = 0.95f, borderColor = "#E6474E", borderWidth = 2.5f, glowColor = "#E6474E", glowRadius = 10f),
        pressed = NxpPressedState(scale = 0.95f, fillColor = "#E6474E", borderColor = "#FFFFFF", glowRadius = 18f),
        label = NxpLabel(text = "B", color = "#E6474E", pressedColor = "#000000", fontSize = 28f),
        size = NxpSize(widthDp = 80, heightDp = 80)
    )
    val CAPSULES_BUTTON_X = NxpComponentDef(
        manifest = NxpManifest(id = "builtin.caps_x", name = "Capsule Fill Button X (Blue)", author = "NEXPAD Core", version = "1.0.0", category = NxprcCategory.BUTTON.id, defaultControl = ControlKey.X.key, description = "Pill-shaped capsule button flooding blue from outer end."),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(fillColor = "#1A1A1A", opacity = 0.95f, borderColor = "#3F8FE0", borderWidth = 2.5f, glowColor = "#3F8FE0", glowRadius = 10f),
        pressed = NxpPressedState(scale = 0.95f, fillColor = "#3F8FE0", borderColor = "#FFFFFF", glowRadius = 18f),
        label = NxpLabel(text = "X", color = "#3F8FE0", pressedColor = "#000000", fontSize = 28f),
        size = NxpSize(widthDp = 80, heightDp = 80)
    )
    val CAPSULES_BUTTON_Y = NxpComponentDef(
        manifest = NxpManifest(id = "builtin.caps_y", name = "Capsule Fill Button Y (Yellow)", author = "NEXPAD Core", version = "1.0.0", category = NxprcCategory.BUTTON.id, defaultControl = ControlKey.Y.key, description = "Pill-shaped capsule button flooding yellow from outer end."),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(fillColor = "#1A1A1A", opacity = 0.95f, borderColor = "#E0A03F", borderWidth = 2.5f, glowColor = "#E0A03F", glowRadius = 10f),
        pressed = NxpPressedState(scale = 0.95f, fillColor = "#E0A03F", borderColor = "#FFFFFF", glowRadius = 18f),
        label = NxpLabel(text = "Y", color = "#E0A03F", pressedColor = "#000000", fontSize = 28f),
        size = NxpSize(widthDp = 80, heightDp = 80)
    )

    // ── ECLIPSE DISC BUTTON ───────────────────────────────────────────────────
    val ECLIPSE_BUTTON_A = NxpComponentDef(
        manifest = NxpManifest(id = "builtin.ecl_a", name = "Eclipse Disc Button A (Green)", author = "NEXPAD Core", version = "1.0.0", category = NxprcCategory.BUTTON.id, defaultControl = ControlKey.A.key, description = "Solar eclipse disc button with green corona ring."),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(fillColor = "#1A1A1A", opacity = 0.95f, borderColor = "#3FD25A", borderWidth = 2.5f, glowColor = "#3FD25A", glowRadius = 10f),
        pressed = NxpPressedState(scale = 0.95f, fillColor = "#3FD25A", borderColor = "#FFFFFF", glowRadius = 18f),
        label = NxpLabel(text = "A", color = "#3FD25A", pressedColor = "#000000", fontSize = 28f),
        size = NxpSize(widthDp = 80, heightDp = 80)
    )
    val ECLIPSE_BUTTON_B = NxpComponentDef(
        manifest = NxpManifest(id = "builtin.ecl_b", name = "Eclipse Disc Button B (Red)", author = "NEXPAD Core", version = "1.0.0", category = NxprcCategory.BUTTON.id, defaultControl = ControlKey.B.key, description = "Solar eclipse disc button with red corona ring."),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(fillColor = "#1A1A1A", opacity = 0.95f, borderColor = "#E6474E", borderWidth = 2.5f, glowColor = "#E6474E", glowRadius = 10f),
        pressed = NxpPressedState(scale = 0.95f, fillColor = "#E6474E", borderColor = "#FFFFFF", glowRadius = 18f),
        label = NxpLabel(text = "B", color = "#E6474E", pressedColor = "#000000", fontSize = 28f),
        size = NxpSize(widthDp = 80, heightDp = 80)
    )
    val ECLIPSE_BUTTON_X = NxpComponentDef(
        manifest = NxpManifest(id = "builtin.ecl_x", name = "Eclipse Disc Button X (Blue)", author = "NEXPAD Core", version = "1.0.0", category = NxprcCategory.BUTTON.id, defaultControl = ControlKey.X.key, description = "Solar eclipse disc button with blue corona ring."),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(fillColor = "#1A1A1A", opacity = 0.95f, borderColor = "#3F8FE0", borderWidth = 2.5f, glowColor = "#3F8FE0", glowRadius = 10f),
        pressed = NxpPressedState(scale = 0.95f, fillColor = "#3F8FE0", borderColor = "#FFFFFF", glowRadius = 18f),
        label = NxpLabel(text = "X", color = "#3F8FE0", pressedColor = "#000000", fontSize = 28f),
        size = NxpSize(widthDp = 80, heightDp = 80)
    )
    val ECLIPSE_BUTTON_Y = NxpComponentDef(
        manifest = NxpManifest(id = "builtin.ecl_y", name = "Eclipse Disc Button Y (Yellow)", author = "NEXPAD Core", version = "1.0.0", category = NxprcCategory.BUTTON.id, defaultControl = ControlKey.Y.key, description = "Solar eclipse disc button with yellow corona ring."),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(fillColor = "#1A1A1A", opacity = 0.95f, borderColor = "#E0A03F", borderWidth = 2.5f, glowColor = "#E0A03F", glowRadius = 10f),
        pressed = NxpPressedState(scale = 0.95f, fillColor = "#E0A03F", borderColor = "#FFFFFF", glowRadius = 18f),
        label = NxpLabel(text = "Y", color = "#E0A03F", pressedColor = "#000000", fontSize = 28f),
        size = NxpSize(widthDp = 80, heightDp = 80)
    )

    val DEFAULT_STICK_LS = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.default_ls",
            name = "Default Left Stick (LS)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.JOYSTICK.id,
            defaultControl = ControlKey.LS.key,
            description = "Dual-ring analog movement stick with textured concave grip and L3 click."
        ),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(
            fillColor = "#151515",
            opacity = 0.92f,
            borderColor = "#00E5FF",
            borderWidth = 2f,
            glowColor = "#00E5FF",
            glowRadius = 10f
        ),
        pressed = NxpPressedState(fillColor = "#00E5FF"),
        interaction = NxpInteraction(type = "Joystick", deadzone = 0.05f),
        size = NxpSize(widthDp = 150, heightDp = 150)
    )

    val DEFAULT_STICK_RS = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.default_rs",
            name = "Default Right Stick (RS)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.JOYSTICK.id,
            defaultControl = ControlKey.RS.key,
            description = "High-precision aiming analog thumbstick with responsive centering and R3 click."
        ),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(
            fillColor = "#151515",
            opacity = 0.92f,
            borderColor = "#FF007F",
            borderWidth = 2f,
            glowColor = "#FF007F",
            glowRadius = 10f
        ),
        pressed = NxpPressedState(fillColor = "#FF007F"),
        interaction = NxpInteraction(type = "Joystick", deadzone = 0.05f),
        size = NxpSize(widthDp = 150, heightDp = 150)
    )

    val DEFAULT_STICK_LSB = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.default_lsb",
            name = "Default Left Stick Button (LSB)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.JOYSTICK.id,
            defaultControl = ControlKey.LSB.key,
            description = "Standalone left thumbstick click button (L3 / LSB) for sprint or special actions."
        ),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(
            fillColor = "#151515",
            opacity = 0.92f,
            borderColor = "#00E5FF",
            borderWidth = 2f,
            glowColor = "#00E5FF",
            glowRadius = 10f
        ),
        pressed = NxpPressedState(scale = 0.90f, fillColor = "#00E5FF", borderColor = "#FFFFFF"),
        label = NxpLabel(text = "LSB", color = "#00E5FF", pressedColor = "#000000", fontSize = 14f),
        size = NxpSize(widthDp = 70, heightDp = 70)
    )

    val DEFAULT_STICK_RSB = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.default_rsb",
            name = "Default Right Stick Button (RSB)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.JOYSTICK.id,
            defaultControl = ControlKey.RSB.key,
            description = "Standalone right thumbstick click button (R3 / RSB) for melee, crouch, or zoom actions."
        ),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(
            fillColor = "#151515",
            opacity = 0.92f,
            borderColor = "#FF007F",
            borderWidth = 2f,
            glowColor = "#FF007F",
            glowRadius = 10f
        ),
        pressed = NxpPressedState(scale = 0.90f, fillColor = "#FF007F", borderColor = "#FFFFFF"),
        label = NxpLabel(text = "RSB", color = "#FF007F", pressedColor = "#000000", fontSize = 14f),
        size = NxpSize(widthDp = 70, heightDp = 70)
    )

    val FLUX_STICK_LS = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.flux_ls",
            name = "Flux Cyber Left Stick (LS)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.JOYSTICK.id,
            defaultControl = ControlKey.LS.key,
            description = "Console-grade cyber analog joystick with dynamic deflection gate arc and 12-tick dial."
        ),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(
            fillColor = "#090A0B",
            opacity = 0.95f,
            borderColor = "#2FD4B6",
            borderWidth = 2f,
            glowColor = "#2FD4B6",
            glowRadius = 12f
        ),
        pressed = NxpPressedState(fillColor = "#2FD4B6"),
        interaction = NxpInteraction(type = "Joystick", deadzone = 0.05f),
        size = NxpSize(widthDp = 150, heightDp = 150)
    )

    val FLUX_STICK_RS = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.flux_rs",
            name = "Flux Cyber Right Stick (RS)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.JOYSTICK.id,
            defaultControl = ControlKey.RS.key,
            description = "Console-grade cyber analog joystick with neon magenta deflection gate arc and 12-tick dial."
        ),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(
            fillColor = "#090A0B",
            opacity = 0.95f,
            borderColor = "#FF3185",
            borderWidth = 2f,
            glowColor = "#FF3185",
            glowRadius = 12f
        ),
        pressed = NxpPressedState(fillColor = "#FF3185"),
        interaction = NxpInteraction(type = "Joystick", deadzone = 0.05f),
        size = NxpSize(widthDp = 150, heightDp = 150)
    )

    val FLUX_STICK_LSB = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.flux_lsb",
            name = "Flux Cyber Left Stick Button (LSB)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.JOYSTICK.id,
            defaultControl = ControlKey.LSB.key,
            description = "Tactile Flux thumbstick click button (L3 / LSB) with knurled grip and cyan neon glow."
        ),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(
            fillColor = "#111316",
            opacity = 0.95f,
            borderColor = "#2FD4B6",
            borderWidth = 2f,
            glowColor = "#2FD4B6",
            glowRadius = 10f
        ),
        pressed = NxpPressedState(scale = 0.89f, fillColor = "#2FD4B6", borderColor = "#FFFFFF"),
        label = NxpLabel(text = "LSB", color = "#2FD4B6", pressedColor = "#000000", fontSize = 14f),
        size = NxpSize(widthDp = 70, heightDp = 70)
    )

    val FLUX_STICK_RSB = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.flux_rsb",
            name = "Flux Cyber Right Stick Button (RSB)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.JOYSTICK.id,
            defaultControl = ControlKey.RSB.key,
            description = "Tactile Flux thumbstick click button (R3 / RSB) with knurled grip and hot-pink neon glow."
        ),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(
            fillColor = "#111316",
            opacity = 0.95f,
            borderColor = "#FF3185",
            borderWidth = 2f,
            glowColor = "#FF3185",
            glowRadius = 10f
        ),
        pressed = NxpPressedState(scale = 0.89f, fillColor = "#FF3185", borderColor = "#FFFFFF"),
        label = NxpLabel(text = "RSB", color = "#FF3185", pressedColor = "#000000", fontSize = 14f),
        size = NxpSize(widthDp = 70, heightDp = 70)
    )

    val ORB_STICK_LS = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.orb_ls",
            name = "Orb Glass Left Stick (LS)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.JOYSTICK.id,
            defaultControl = ControlKey.LS.key,
            description = "Glass sphere analog joystick with glowing liquid core that lags inertially behind movement."
        ),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(
            fillColor = "#030304",
            opacity = 0.95f,
            borderColor = "#2FD4B6",
            borderWidth = 2f,
            glowColor = "#2FD4B6",
            glowRadius = 12f
        ),
        pressed = NxpPressedState(fillColor = "#2FD4B6"),
        interaction = NxpInteraction(type = "Joystick", deadzone = 0.05f),
        size = NxpSize(widthDp = 150, heightDp = 150)
    )

    val ORB_STICK_RS = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.orb_rs",
            name = "Orb Glass Right Stick (RS)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.JOYSTICK.id,
            defaultControl = ControlKey.RS.key,
            description = "Glass sphere analog joystick with glowing liquid core in neon magenta that lags inertially behind movement."
        ),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(
            fillColor = "#030304",
            opacity = 0.95f,
            borderColor = "#FF3185",
            borderWidth = 2f,
            glowColor = "#FF3185",
            glowRadius = 12f
        ),
        pressed = NxpPressedState(fillColor = "#FF3185"),
        interaction = NxpInteraction(type = "Joystick", deadzone = 0.05f),
        size = NxpSize(widthDp = 150, heightDp = 150)
    )

    val ORB_STICK_LSB = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.orb_lsb",
            name = "Orb Glass Left Stick Button (LSB)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.JOYSTICK.id,
            defaultControl = ControlKey.LSB.key,
            description = "Tactile Orb thumbstick click button (L3 / LSB) with spherical glass cavity and cyan liquid core."
        ),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(
            fillColor = "#15171A",
            opacity = 0.95f,
            borderColor = "#2FD4B6",
            borderWidth = 2f,
            glowColor = "#2FD4B6",
            glowRadius = 10f
        ),
        pressed = NxpPressedState(scale = 0.89f, fillColor = "#2FD4B6", borderColor = "#FFFFFF"),
        label = NxpLabel(text = "LSB", color = "#2FD4B6", pressedColor = "#000000", fontSize = 13f),
        size = NxpSize(widthDp = 70, heightDp = 70)
    )

    val ORB_STICK_RSB = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.orb_rsb",
            name = "Orb Glass Right Stick Button (RSB)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.JOYSTICK.id,
            defaultControl = ControlKey.RSB.key,
            description = "Tactile Orb thumbstick click button (R3 / RSB) with spherical glass cavity and hot-pink liquid core."
        ),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(
            fillColor = "#15171A",
            opacity = 0.95f,
            borderColor = "#FF3185",
            borderWidth = 2f,
            glowColor = "#FF3185",
            glowRadius = 10f
        ),
        pressed = NxpPressedState(scale = 0.89f, fillColor = "#FF3185", borderColor = "#FFFFFF"),
        label = NxpLabel(text = "RSB", color = "#FF3185", pressedColor = "#000000", fontSize = 13f),
        size = NxpSize(widthDp = 70, heightDp = 70)
    )

    val COMPASS_STICK_LS = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.compass_ls",
            name = "Compass Left Stick (LS)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.JOYSTICK.id,
            defaultControl = ControlKey.LS.key,
            description = "Compass analog joystick with eight illuminated directional pips and rotating direction pointer indicator."
        ),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(
            fillColor = "#030304",
            opacity = 0.95f,
            borderColor = "#5AA8FF",
            borderWidth = 2f,
            glowColor = "#5AA8FF",
            glowRadius = 12f
        ),
        pressed = NxpPressedState(fillColor = "#5AA8FF"),
        interaction = NxpInteraction(type = "Joystick", deadzone = 0.05f),
        size = NxpSize(widthDp = 150, heightDp = 150)
    )

    val COMPASS_STICK_RS = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.compass_rs",
            name = "Compass Right Stick (RS)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.JOYSTICK.id,
            defaultControl = ControlKey.RS.key,
            description = "Compass analog joystick with eight illuminated directional pips and rotating direction pointer indicator in coral pink."
        ),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(
            fillColor = "#030304",
            opacity = 0.95f,
            borderColor = "#FF5A88",
            borderWidth = 2f,
            glowColor = "#FF5A88",
            glowRadius = 12f
        ),
        pressed = NxpPressedState(fillColor = "#FF5A88"),
        interaction = NxpInteraction(type = "Joystick", deadzone = 0.05f),
        size = NxpSize(widthDp = 150, heightDp = 150)
    )

    val COMPASS_STICK_LSB = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.compass_lsb",
            name = "Compass Left Stick Button (LSB)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.JOYSTICK.id,
            defaultControl = ControlKey.LSB.key,
            description = "Tactile Compass thumbstick click button (L3 / LSB) with perimeter compass pips and ice-blue neon glow."
        ),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(
            fillColor = "#34373B",
            opacity = 0.95f,
            borderColor = "#5AA8FF",
            borderWidth = 2f,
            glowColor = "#5AA8FF",
            glowRadius = 10f
        ),
        pressed = NxpPressedState(scale = 0.89f, fillColor = "#5AA8FF", borderColor = "#FFFFFF"),
        label = NxpLabel(text = "LSB", color = "#5AA8FF", pressedColor = "#000000", fontSize = 12f),
        size = NxpSize(widthDp = 70, heightDp = 70)
    )

    val COMPASS_STICK_RSB = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.compass_rsb",
            name = "Compass Right Stick Button (RSB)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.JOYSTICK.id,
            defaultControl = ControlKey.RSB.key,
            description = "Tactile Compass thumbstick click button (R3 / RSB) with perimeter compass pips and coral-pink neon glow."
        ),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(
            fillColor = "#34373B",
            opacity = 0.95f,
            borderColor = "#FF5A88",
            borderWidth = 2f,
            glowColor = "#FF5A88",
            glowRadius = 10f
        ),
        pressed = NxpPressedState(scale = 0.89f, fillColor = "#FF5A88", borderColor = "#FFFFFF"),
        label = NxpLabel(text = "RSB", color = "#FF5A88", pressedColor = "#000000", fontSize = 12f),
        size = NxpSize(widthDp = 70, heightDp = 70)
    )

    val GYRO_STICK_LS = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.gyro_ls",
            name = "Gyro Gimbal Left Stick (LS)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.JOYSTICK.id,
            defaultControl = ControlKey.LS.key,
            description = "3D dual-gimbal analog joystick with counter-tilting suspension rings around a compact center puck."
        ),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(
            fillColor = "#030304",
            opacity = 0.95f,
            borderColor = "#3FD2FF",
            borderWidth = 2f,
            glowColor = "#3FD2FF",
            glowRadius = 12f
        ),
        pressed = NxpPressedState(fillColor = "#3FD2FF"),
        interaction = NxpInteraction(type = "Joystick", deadzone = 0.05f),
        size = NxpSize(widthDp = 150, heightDp = 150)
    )

    val GYRO_STICK_RS = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.gyro_rs",
            name = "Gyro Gimbal Right Stick (RS)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.JOYSTICK.id,
            defaultControl = ControlKey.RS.key,
            description = "3D dual-gimbal analog joystick with counter-tilting suspension rings in electric magenta."
        ),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(
            fillColor = "#030304",
            opacity = 0.95f,
            borderColor = "#FF3F85",
            borderWidth = 2f,
            glowColor = "#FF3F85",
            glowRadius = 12f
        ),
        pressed = NxpPressedState(fillColor = "#FF3F85"),
        interaction = NxpInteraction(type = "Joystick", deadzone = 0.05f),
        size = NxpSize(widthDp = 150, heightDp = 150)
    )

    val GYRO_STICK_LSB = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.gyro_lsb",
            name = "Gyro Gimbal Left Stick Button (LSB)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.JOYSTICK.id,
            defaultControl = ControlKey.LSB.key,
            description = "Tactile Gyro thumbstick click button (L3 / LSB) with concentric gimbal rings and electric cyan glow."
        ),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(
            fillColor = "#34373B",
            opacity = 0.95f,
            borderColor = "#3FD2FF",
            borderWidth = 2f,
            glowColor = "#3FD2FF",
            glowRadius = 10f
        ),
        pressed = NxpPressedState(scale = 0.89f, fillColor = "#3FD2FF", borderColor = "#FFFFFF"),
        label = NxpLabel(text = "LSB", color = "#3FD2FF", pressedColor = "#000000", fontSize = 12f),
        size = NxpSize(widthDp = 70, heightDp = 70)
    )

    val GYRO_STICK_RSB = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.gyro_rsb",
            name = "Gyro Gimbal Right Stick Button (RSB)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.JOYSTICK.id,
            defaultControl = ControlKey.RSB.key,
            description = "Tactile Gyro thumbstick click button (R3 / RSB) with concentric gimbal rings and electric magenta glow."
        ),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(
            fillColor = "#34373B",
            opacity = 0.95f,
            borderColor = "#FF3F85",
            borderWidth = 2f,
            glowColor = "#FF3F85",
            glowRadius = 10f
        ),
        pressed = NxpPressedState(scale = 0.89f, fillColor = "#FF3F85", borderColor = "#FFFFFF"),
        label = NxpLabel(text = "RSB", color = "#FF3F85", pressedColor = "#000000", fontSize = 12f),
        size = NxpSize(widthDp = 70, heightDp = 70)
    )

    val SPOTLIGHT_STICK_LS = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.spotlight_ls",
            name = "Spotlight Left Stick (LS)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.JOYSTICK.id,
            defaultControl = ControlKey.LS.key,
            description = "Analog joystick with dynamic light pool revealing hidden floor dots under the moving center puck."
        ),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(
            fillColor = "#030304",
            opacity = 0.95f,
            borderColor = "#FFD23F",
            borderWidth = 2f,
            glowColor = "#FFD23F",
            glowRadius = 12f
        ),
        pressed = NxpPressedState(fillColor = "#FFD23F"),
        interaction = NxpInteraction(type = "Joystick", deadzone = 0.05f),
        size = NxpSize(widthDp = 150, heightDp = 150)
    )

    val SPOTLIGHT_STICK_RS = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.spotlight_rs",
            name = "Spotlight Right Stick (RS)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.JOYSTICK.id,
            defaultControl = ControlKey.RS.key,
            description = "Analog joystick with dynamic light pool revealing hidden floor dots under the moving center puck in electric magenta."
        ),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(
            fillColor = "#030304",
            opacity = 0.95f,
            borderColor = "#FF3F85",
            borderWidth = 2f,
            glowColor = "#FF3F85",
            glowRadius = 12f
        ),
        pressed = NxpPressedState(fillColor = "#FF3F85"),
        interaction = NxpInteraction(type = "Joystick", deadzone = 0.05f),
        size = NxpSize(widthDp = 150, heightDp = 150)
    )

    val SPOTLIGHT_STICK_LSB = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.spotlight_lsb",
            name = "Spotlight Left Stick Button (LSB)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.JOYSTICK.id,
            defaultControl = ControlKey.LSB.key,
            description = "Tactile Spotlight thumbstick click button (L3 / LSB) with floor light pool and warm gold glow."
        ),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(
            fillColor = "#34373B",
            opacity = 0.95f,
            borderColor = "#FFD23F",
            borderWidth = 2f,
            glowColor = "#FFD23F",
            glowRadius = 10f
        ),
        pressed = NxpPressedState(scale = 0.89f, fillColor = "#FFD23F", borderColor = "#FFFFFF"),
        label = NxpLabel(text = "LSB", color = "#FFD23F", pressedColor = "#000000", fontSize = 12f),
        size = NxpSize(widthDp = 70, heightDp = 70)
    )

    val SPOTLIGHT_STICK_RSB = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.spotlight_rsb",
            name = "Spotlight Right Stick Button (RSB)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.JOYSTICK.id,
            defaultControl = ControlKey.RSB.key,
            description = "Tactile Spotlight thumbstick click button (R3 / RSB) with floor light pool and electric magenta glow."
        ),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(
            fillColor = "#34373B",
            opacity = 0.95f,
            borderColor = "#FF3F85",
            borderWidth = 2f,
            glowColor = "#FF3F85",
            glowRadius = 10f
        ),
        pressed = NxpPressedState(scale = 0.89f, fillColor = "#FF3F85", borderColor = "#FFFFFF"),
        label = NxpLabel(text = "RSB", color = "#FF3F85", pressedColor = "#000000", fontSize = 12f),
        size = NxpSize(widthDp = 70, heightDp = 70)
    )

    val DEFAULT_STICK_LTP = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.default_ltp",
            name = "Default Left Touchpad (LTP)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.JOYSTICK.id,
            defaultControl = ControlKey.LTP.key,
            description = "Console-grade dynamic floating-center movement touchpad for mobile twin-stick gaming."
        ),
        geometry = NxpGeometry(type = "RoundedRectangle", cornerRadius = 26f),
        visual = NxpVisual(
            fillColor = "#131418",
            opacity = 0.92f,
            borderColor = "#00E5FF",
            borderWidth = 2f,
            glowColor = "#00E5FF",
            glowRadius = 12f
        ),
        pressed = NxpPressedState(scale = 0.98f, fillColor = "#181A20", borderColor = "#FFFFFF"),
        label = NxpLabel(text = "LTP", color = "#00E5FF", pressedColor = "#FFFFFF", fontSize = 16f),
        size = NxpSize(widthDp = 180, heightDp = 180)
    )

    val DEFAULT_STICK_RTP = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.default_rtp",
            name = "Default Right Touchpad (RTP)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.JOYSTICK.id,
            defaultControl = ControlKey.RTP.key,
            description = "Console-grade swipe-to-look camera trackpad with instant stop and momentum decay."
        ),
        geometry = NxpGeometry(type = "RoundedRectangle", cornerRadius = 26f),
        visual = NxpVisual(
            fillColor = "#131418",
            opacity = 0.92f,
            borderColor = "#FF007F",
            borderWidth = 2f,
            glowColor = "#FF007F",
            glowRadius = 12f
        ),
        pressed = NxpPressedState(scale = 0.98f, fillColor = "#181A20", borderColor = "#FFFFFF"),
        label = NxpLabel(text = "RTP", color = "#FF007F", pressedColor = "#FFFFFF", fontSize = 16f),
        size = NxpSize(widthDp = 180, heightDp = 180)
    )

    val DEFAULT_DPAD = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.default_dpad",
            name = "Default D-Pad (Directional Cross)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.DPAD.id,
            defaultControl = ControlKey.DPAD.key,
            description = "Standard 4-way mechanical directional cross pad."
        ),
        geometry = NxpGeometry(type = "Polygon", sides = 8, cornerRadius = 8f),
        visual = NxpVisual(
            fillColor = "#1A1A1A",
            opacity = 0.92f,
            borderColor = "#4ADE80",
            borderWidth = 2f,
            glowColor = "#4ADE80",
            glowRadius = 8f
        ),
        pressed = NxpPressedState(fillColor = "#4ADE80"),
        label = NxpLabel(text = "＋", color = "#4ADE80", pressedColor = "#000000", fontSize = 28f),
        size = NxpSize(widthDp = 140, heightDp = 140)
    )

    val DEFAULT_DPAD_UP = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.default_up",
            name = "Default D-Pad Up (▲)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.DPAD.id,
            defaultControl = ControlKey.UP.key,
            description = "Standard tactile directional UP button."
        ),
        geometry = NxpGeometry(type = "RoundedRect", cornerRadius = 12f),
        visual = NxpVisual(
            fillColor = "#1F1F1F",
            opacity = 0.95f,
            borderColor = "#4ADE80",
            borderWidth = 2f,
            glowColor = "#4ADE80",
            glowRadius = 8f
        ),
        pressed = NxpPressedState(scale = 0.88f, fillColor = "#4ADE80", borderColor = "#FFFFFF"),
        label = NxpLabel(text = "▲", color = "#4ADE80", pressedColor = "#000000", fontSize = 22f),
        size = NxpSize(widthDp = 72, heightDp = 72)
    )

    val DEFAULT_DPAD_DOWN = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.default_down",
            name = "Default D-Pad Down (▼)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.DPAD.id,
            defaultControl = ControlKey.DOWN.key,
            description = "Standard tactile directional DOWN button."
        ),
        geometry = NxpGeometry(type = "RoundedRect", cornerRadius = 12f),
        visual = NxpVisual(
            fillColor = "#1F1F1F",
            opacity = 0.95f,
            borderColor = "#4ADE80",
            borderWidth = 2f,
            glowColor = "#4ADE80",
            glowRadius = 8f
        ),
        pressed = NxpPressedState(scale = 0.88f, fillColor = "#4ADE80", borderColor = "#FFFFFF"),
        label = NxpLabel(text = "▼", color = "#4ADE80", pressedColor = "#000000", fontSize = 22f),
        size = NxpSize(widthDp = 72, heightDp = 72)
    )

    val DEFAULT_DPAD_LEFT = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.default_left",
            name = "Default D-Pad Left (◀)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.DPAD.id,
            defaultControl = ControlKey.LEFT.key,
            description = "Standard tactile directional LEFT button."
        ),
        geometry = NxpGeometry(type = "RoundedRect", cornerRadius = 12f),
        visual = NxpVisual(
            fillColor = "#1F1F1F",
            opacity = 0.95f,
            borderColor = "#4ADE80",
            borderWidth = 2f,
            glowColor = "#4ADE80",
            glowRadius = 8f
        ),
        pressed = NxpPressedState(scale = 0.88f, fillColor = "#4ADE80", borderColor = "#FFFFFF"),
        label = NxpLabel(text = "◀", color = "#4ADE80", pressedColor = "#000000", fontSize = 22f),
        size = NxpSize(widthDp = 72, heightDp = 72)
    )

    val DEFAULT_DPAD_RIGHT = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.default_right",
            name = "Default D-Pad Right (▶)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.DPAD.id,
            defaultControl = ControlKey.RIGHT.key,
            description = "Standard tactile directional RIGHT button."
        ),
        geometry = NxpGeometry(type = "RoundedRect", cornerRadius = 12f),
        visual = NxpVisual(
            fillColor = "#1F1F1F",
            opacity = 0.95f,
            borderColor = "#4ADE80",
            borderWidth = 2f,
            glowColor = "#4ADE80",
            glowRadius = 8f
        ),
        pressed = NxpPressedState(scale = 0.88f, fillColor = "#4ADE80", borderColor = "#FFFFFF"),
        label = NxpLabel(text = "▶", color = "#4ADE80", pressedColor = "#000000", fontSize = 22f),
        size = NxpSize(widthDp = 72, heightDp = 72)
    )

    val LENS_DPAD = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.lens_dpad",
            name = "Lens D-Pad Cross",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.DPAD.id,
            defaultControl = ControlKey.DPAD.key,
            description = "Precision contoured D-Pad cross with 3D rocker tilt kinematics, inset containment ring, active directional fills, and optical glass lens."
        ),
        geometry = NxpGeometry(type = "Cross"),
        visual = NxpVisual(
            fillColor = "#26282B",
            opacity = 0.95f,
            borderColor = "#D8DEE9",
            borderWidth = 2f,
            glowColor = "#D8DEE9",
            glowRadius = 10f
        ),
        pressed = NxpPressedState(fillColor = "#D8DEE9"),
        size = NxpSize(widthDp = 160, heightDp = 160)
    )

    val FOUR_LENSES_DPAD = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.four_lenses_dpad",
            name = "Four Lenses D-Pad",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.DPAD.id,
            defaultControl = ControlKey.DPAD.key,
            description = "Four discrete acrylic optical lens keys orbiting a central hub with 3D rocker tilt kinematics, directional chevrons, and ambient halo."
        ),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(
            fillColor = "#232527",
            opacity = 0.95f,
            borderColor = "#D8DEE9",
            borderWidth = 2f,
            glowColor = "#D8DEE9",
            glowRadius = 10f
        ),
        pressed = NxpPressedState(scale = 0.95f, fillColor = "#D8DEE9"),
        size = NxpSize(widthDp = 164, heightDp = 164)
    )

    val DISC_DPAD = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.disc_dpad",
            name = "Lens Disc D-Pad",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.DPAD.id,
            defaultControl = ControlKey.DPAD.key,
            description = "Concentric grooved circular disc D-Pad with 3D rocker tilt kinematics, directional gate sweep, and central sliding puck."
        ),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(
            fillColor = "#232527",
            opacity = 0.95f,
            borderColor = "#E055B8",
            borderWidth = 2f,
            glowColor = "#E055B8",
            glowRadius = 10f
        ),
        pressed = NxpPressedState(scale = 0.95f, fillColor = "#E055B8"),
        size = NxpSize(widthDp = 160, heightDp = 160)
    )

    val CAPSULES_DPAD = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.capsules_dpad",
            name = "Lens Capsules D-Pad",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.DPAD.id,
            defaultControl = ControlKey.DPAD.key,
            description = "Four discrete rounded pill capsule keys orbiting a central hub with 3D rocker tilt kinematics and directional chevrons."
        ),
        geometry = NxpGeometry(type = "RoundedRect", cornerRadius = 23f),
        visual = NxpVisual(
            fillColor = "#232527",
            opacity = 0.95f,
            borderColor = "#B58CFF",
            borderWidth = 2f,
            glowColor = "#B58CFF",
            glowRadius = 10f
        ),
        pressed = NxpPressedState(scale = 0.95f, fillColor = "#B58CFF"),
        size = NxpSize(widthDp = 170, heightDp = 170)
    )

    val METABALLS_DPAD = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.metaballs_dpad",
            name = "Lens Metaballs D-Pad",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.DPAD.id,
            defaultControl = ControlKey.DPAD.key,
            description = "Organic fluid metaballs D-Pad with 3D rocker tilt kinematics, tactile satellite caps, and spring retraction."
        ),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(
            fillColor = "#232527",
            opacity = 0.95f,
            borderColor = "#3FD2C4",
            borderWidth = 2f,
            glowColor = "#3FD2C4",
            glowRadius = 10f
        ),
        pressed = NxpPressedState(scale = 0.95f, fillColor = "#3FD2C4"),
        size = NxpSize(widthDp = 172, heightDp = 172)
    )

    val RAILS_DPAD = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.rails_dpad",
            name = "Lens Rails D-Pad",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.DPAD.id,
            defaultControl = ControlKey.DPAD.key,
            description = "Orthogonal recessed rails D-Pad with 3D rocker tilt kinematics, sliding tactile puck, and animated light beams."
        ),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(
            fillColor = "#232527",
            opacity = 0.95f,
            borderColor = "#FFB13F",
            borderWidth = 2f,
            glowColor = "#FFB13F",
            glowRadius = 10f
        ),
        pressed = NxpPressedState(scale = 0.95f, fillColor = "#FFB13F"),
        size = NxpSize(widthDp = 160, heightDp = 160)
    )


    val DEFAULT_TRIGGER_LT = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.default_lt",
            name = "Default Left Trigger (LT)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.TRIGGER.id,
            defaultControl = ControlKey.LT.key,
            description = "High-travel analog lens trigger with progressive fluid meter."
        ),
        geometry = NxpGeometry(type = "RoundedRect", cornerRadius = 46f),
        visual = NxpVisual(
            fillColor = "#151515",
            opacity = 0.92f,
            borderColor = "#00E5FF",
            borderWidth = 2.0f,
            glowColor = "#00E5FF",
            glowRadius = 10f
        ),
        pressed = NxpPressedState(scale = 0.95f, fillColor = "#00E5FF", borderColor = "#FFFFFF"),
        interaction = NxpInteraction(type = "Trigger"),
        label = NxpLabel(text = "LT", color = "#00E5FF", pressedColor = "#FFFFFF", fontSize = 18f),
        size = NxpSize(widthDp = 100, heightDp = 92)
    )

    val DEFAULT_TRIGGER_RT = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.default_rt",
            name = "Default Right Trigger (RT)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.TRIGGER.id,
            defaultControl = ControlKey.RT.key,
            description = "Accelerator analog lens trigger with progressive fluid meter."
        ),
        geometry = NxpGeometry(type = "RoundedRect", cornerRadius = 46f),
        visual = NxpVisual(
            fillColor = "#151515",
            opacity = 0.92f,
            borderColor = "#E055B8",
            borderWidth = 2.0f,
            glowColor = "#E055B8",
            glowRadius = 10f
        ),
        pressed = NxpPressedState(scale = 0.95f, fillColor = "#E055B8", borderColor = "#FFFFFF"),
        interaction = NxpInteraction(type = "Trigger"),
        label = NxpLabel(text = "RT", color = "#E055B8", pressedColor = "#FFFFFF", fontSize = 18f),
        size = NxpSize(widthDp = 100, heightDp = 92)
    )

    val DEFAULT_BUMPER_LB = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.default_lb",
            name = "Default Left Bumper (LB)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.BUMPER.id,
            defaultControl = ControlKey.LB.key,
            description = "Aerodynamic shoulder bumper with crisp mechanical microswitch click."
        ),
        geometry = NxpGeometry(type = "RoundedRect", cornerRadius = 16f),
        visual = NxpVisual(
            fillColor = "#1E1E1E",
            opacity = 0.92f,
            borderColor = "#06B6D4",
            borderWidth = 2f,
            glowColor = "#06B6D4",
            glowRadius = 8f
        ),
        pressed = NxpPressedState(scale = 0.94f, fillColor = "#06B6D4", borderColor = "#FFFFFF"),
        label = NxpLabel(text = "LB", color = "#06B6D4", pressedColor = "#000000", fontSize = 18f),
        size = NxpSize(widthDp = 160, heightDp = 60)
    )

    val DEFAULT_BUMPER_RB = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.default_rb",
            name = "Default Right Bumper (RB)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.BUMPER.id,
            defaultControl = ControlKey.RB.key,
            description = "Aerodynamic shoulder bumper with crisp mechanical microswitch click."
        ),
        geometry = NxpGeometry(type = "RoundedRect", cornerRadius = 16f),
        visual = NxpVisual(
            fillColor = "#1E1E1E",
            opacity = 0.92f,
            borderColor = "#06B6D4",
            borderWidth = 2f,
            glowColor = "#06B6D4",
            glowRadius = 8f
        ),
        pressed = NxpPressedState(scale = 0.94f, fillColor = "#06B6D4", borderColor = "#FFFFFF"),
        label = NxpLabel(text = "RB", color = "#06B6D4", pressedColor = "#000000", fontSize = 18f),
        size = NxpSize(widthDp = 160, heightDp = 60)
    )

    val ARC_BUMPER_LB = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.arc_lb",
            name = "Arc Bumper (LB)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.BUMPER.id,
            defaultControl = ControlKey.LB.key,
            description = "Geometric curved bridge bumper with multi-pass neon halo and plunging spring travel."
        ),
        geometry = NxpGeometry(type = "RoundedRect", cornerRadius = 24f),
        visual = NxpVisual(
            fillColor = "#26282B",
            opacity = 0.95f,
            borderColor = "#A97CF0",
            borderWidth = 2f,
            glowColor = "#A97CF0",
            glowRadius = 10f
        ),
        pressed = NxpPressedState(scale = 0.97f, fillColor = "#A97CF0"),
        label = NxpLabel(text = "LB", color = "#A97CF0", pressedColor = "#FFFFFF", fontSize = 19f),
        size = NxpSize(widthDp = 154, heightDp = 56)
    )

    val ARC_BUMPER_RB = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.arc_rb",
            name = "Arc Bumper (RB)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.BUMPER.id,
            defaultControl = ControlKey.RB.key,
            description = "Geometric curved bridge bumper with multi-pass neon halo and plunging spring travel."
        ),
        geometry = NxpGeometry(type = "RoundedRect", cornerRadius = 24f),
        visual = NxpVisual(
            fillColor = "#26282B",
            opacity = 0.95f,
            borderColor = "#A97CF0",
            borderWidth = 2f,
            glowColor = "#A97CF0",
            glowRadius = 10f
        ),
        pressed = NxpPressedState(scale = 0.97f, fillColor = "#A97CF0"),
        label = NxpLabel(text = "RB", color = "#A97CF0", pressedColor = "#FFFFFF", fontSize = 19f),
        size = NxpSize(widthDp = 154, heightDp = 56)
    )

    val LED_BUMPER_LB = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.led_lb",
            name = "LED Bar Bumper (LB)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.BUMPER.id,
            defaultControl = ControlKey.LB.key,
            description = "Asymmetric ergonomic shoulder bumper with optical magnifier window and 6-segment illuminated neon LED bar graph."
        ),
        geometry = NxpGeometry(type = "RoundedRect", cornerRadius = 27f),
        visual = NxpVisual(
            fillColor = "#232527",
            opacity = 0.95f,
            borderColor = "#A97CF0",
            borderWidth = 2f,
            glowColor = "#A97CF0",
            glowRadius = 10f
        ),
        pressed = NxpPressedState(scale = 0.96f, fillColor = "#A97CF0"),
        label = NxpLabel(text = "LB", color = "#A97CF0", pressedColor = "#FFFFFF", fontSize = 16f),
        size = NxpSize(widthDp = 154, heightDp = 54)
    )

    val LED_BUMPER_RB = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.led_rb",
            name = "LED Bar Bumper (RB)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.BUMPER.id,
            defaultControl = ControlKey.RB.key,
            description = "Asymmetric ergonomic shoulder bumper with optical magnifier window and 6-segment illuminated neon LED bar graph."
        ),
        geometry = NxpGeometry(type = "RoundedRect", cornerRadius = 27f),
        visual = NxpVisual(
            fillColor = "#232527",
            opacity = 0.95f,
            borderColor = "#A97CF0",
            borderWidth = 2f,
            glowColor = "#A97CF0",
            glowRadius = 10f
        ),
        pressed = NxpPressedState(scale = 0.96f, fillColor = "#A97CF0"),
        label = NxpLabel(text = "RB", color = "#A97CF0", pressedColor = "#FFFFFF", fontSize = 16f),
        size = NxpSize(widthDp = 154, heightDp = 54)
    )

    val PEEK_BUMPER_LB = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.peek_lb",
            name = "Peek Bumper (LB)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.BUMPER.id,
            defaultControl = ControlKey.LB.key,
            description = "Pill shoulder bumper with oversized optical magnifier aperture window framing an enlarged 1.18x dynamic zoom peek glyph."
        ),
        geometry = NxpGeometry(type = "RoundedRect", cornerRadius = 27f),
        visual = NxpVisual(
            fillColor = "#232527",
            opacity = 0.95f,
            borderColor = "#A97CF0",
            borderWidth = 2f,
            glowColor = "#A97CF0",
            glowRadius = 10f
        ),
        pressed = NxpPressedState(scale = 0.95f, fillColor = "#A97CF0"),
        label = NxpLabel(text = "LB", color = "#A97CF0", pressedColor = "#FFFFFF", fontSize = 16f),
        size = NxpSize(widthDp = 154, heightDp = 54)
    )

    val PEEK_BUMPER_RB = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.peek_rb",
            name = "Peek Bumper (RB)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.BUMPER.id,
            defaultControl = ControlKey.RB.key,
            description = "Pill shoulder bumper with oversized optical magnifier aperture window framing an enlarged 1.18x dynamic zoom peek glyph."
        ),
        geometry = NxpGeometry(type = "RoundedRect", cornerRadius = 27f),
        visual = NxpVisual(
            fillColor = "#232527",
            opacity = 0.95f,
            borderColor = "#A97CF0",
            borderWidth = 2f,
            glowColor = "#A97CF0",
            glowRadius = 10f
        ),
        pressed = NxpPressedState(scale = 0.95f, fillColor = "#A97CF0"),
        label = NxpLabel(text = "RB", color = "#A97CF0", pressedColor = "#FFFFFF", fontSize = 16f),
        size = NxpSize(widthDp = 154, heightDp = 54)
    )

    val RIBBED_BUMPER_LB = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.rib_lb",
            name = "Ribbed Bumper (LB)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.BUMPER.id,
            defaultControl = ControlKey.LB.key,
            description = "Ergonomic shoulder bumper with repeating tactile vertical micro-ribs, elevated optical window, and lower illuminated neon lightbar strip."
        ),
        geometry = NxpGeometry(type = "RoundedRect", cornerRadius = 27f),
        visual = NxpVisual(
            fillColor = "#232527",
            opacity = 0.95f,
            borderColor = "#3FD2FF",
            borderWidth = 2f,
            glowColor = "#3FD2FF",
            glowRadius = 10f
        ),
        pressed = NxpPressedState(scale = 0.95f, fillColor = "#3FD2FF"),
        label = NxpLabel(text = "LB", color = "#3FD2FF", pressedColor = "#FFFFFF", fontSize = 16f),
        size = NxpSize(widthDp = 154, heightDp = 54)
    )

    val RIBBED_BUMPER_RB = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.rib_rb",
            name = "Ribbed Bumper (RB)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.BUMPER.id,
            defaultControl = ControlKey.RB.key,
            description = "Ergonomic shoulder bumper with repeating tactile vertical micro-ribs, elevated optical window, and lower illuminated neon lightbar strip."
        ),
        geometry = NxpGeometry(type = "RoundedRect", cornerRadius = 27f),
        visual = NxpVisual(
            fillColor = "#232527",
            opacity = 0.95f,
            borderColor = "#FF007F",
            borderWidth = 2f,
            glowColor = "#FF007F",
            glowRadius = 10f
        ),
        pressed = NxpPressedState(scale = 0.95f, fillColor = "#FF007F"),
        label = NxpLabel(text = "RB", color = "#FF007F", pressedColor = "#FFFFFF", fontSize = 16f),
        size = NxpSize(widthDp = 154, heightDp = 54)
    )

    val UNDERGLOW_BUMPER_LB = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.under_lb",
            name = "Underglow Bumper (LB)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.BUMPER.id,
            defaultControl = ControlKey.LB.key,
            description = "Ergonomic shoulder bumper with horizontal bottom neon underglow bar and upward dynamic surge flood illumination on press."
        ),
        geometry = NxpGeometry(type = "RoundedRect", cornerRadius = 27f),
        visual = NxpVisual(
            fillColor = "#232527",
            opacity = 0.95f,
            borderColor = "#5CF29A",
            borderWidth = 2f,
            glowColor = "#5CF29A",
            glowRadius = 10f
        ),
        pressed = NxpPressedState(scale = 0.95f, fillColor = "#5CF29A"),
        label = NxpLabel(text = "LB", color = "#5CF29A", pressedColor = "#FFFFFF", fontSize = 16f),
        size = NxpSize(widthDp = 154, heightDp = 54)
    )

    val UNDERGLOW_BUMPER_RB = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.under_rb",
            name = "Underglow Bumper (RB)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.BUMPER.id,
            defaultControl = ControlKey.RB.key,
            description = "Ergonomic shoulder bumper with horizontal bottom neon underglow bar and upward dynamic surge flood illumination on press."
        ),
        geometry = NxpGeometry(type = "RoundedRect", cornerRadius = 27f),
        visual = NxpVisual(
            fillColor = "#232527",
            opacity = 0.95f,
            borderColor = "#FF5252",
            borderWidth = 2f,
            glowColor = "#FF5252",
            glowRadius = 10f
        ),
        pressed = NxpPressedState(scale = 0.95f, fillColor = "#FF5252"),
        label = NxpLabel(text = "RB", color = "#FF5252", pressedColor = "#FFFFFF", fontSize = 16f),
        size = NxpSize(widthDp = 154, heightDp = 54)
    )

    val TUBE_BUMPER_LB = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.tube_lb",
            name = "Tube Bumper (LB)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.BUMPER.id,
            defaultControl = ControlKey.LB.key,
            description = "Cylindrical tube shoulder bumper with dynamic horizontal liquid level surge and laboratory calibration tick marks."
        ),
        geometry = NxpGeometry(type = "RoundedRect", cornerRadius = 27f),
        visual = NxpVisual(
            fillColor = "#232527",
            opacity = 0.95f,
            borderColor = "#4FA8FF",
            borderWidth = 2f,
            glowColor = "#4FA8FF",
            glowRadius = 10f
        ),
        pressed = NxpPressedState(scale = 0.95f, fillColor = "#4FA8FF"),
        label = NxpLabel(text = "LB", color = "#4FA8FF", pressedColor = "#FFFFFF", fontSize = 16f),
        size = NxpSize(widthDp = 154, heightDp = 54)
    )

    val TUBE_BUMPER_RB = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.tube_rb",
            name = "Tube Bumper (RB)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.BUMPER.id,
            defaultControl = ControlKey.RB.key,
            description = "Cylindrical tube shoulder bumper with dynamic horizontal liquid level surge and laboratory calibration tick marks."
        ),
        geometry = NxpGeometry(type = "RoundedRect", cornerRadius = 27f),
        visual = NxpVisual(
            fillColor = "#232527",
            opacity = 0.95f,
            borderColor = "#FF4F81",
            borderWidth = 2f,
            glowColor = "#FF4F81",
            glowRadius = 10f
        ),
        pressed = NxpPressedState(scale = 0.95f, fillColor = "#FF4F81"),
        label = NxpLabel(text = "RB", color = "#FF4F81", pressedColor = "#FFFFFF", fontSize = 16f),
        size = NxpSize(widthDp = 154, heightDp = 54)
    )

    val FLIP_BUMPER_LB = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.flip_lb",
            name = "Flip Bumper (LB)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.BUMPER.id,
            defaultControl = ControlKey.LB.key,
            description = "3D card flip shoulder bumper rotating 180 degrees on press from dark optical window Face A to golden radiant neon plate Face B."
        ),
        geometry = NxpGeometry(type = "RoundedRect", cornerRadius = 27f),
        visual = NxpVisual(
            fillColor = "#232527",
            opacity = 0.95f,
            borderColor = "#FFD23F",
            borderWidth = 2f,
            glowColor = "#FFD23F",
            glowRadius = 10f
        ),
        pressed = NxpPressedState(scale = 0.95f, fillColor = "#FFD23F"),
        label = NxpLabel(text = "LB", color = "#FFD23F", pressedColor = "#0A0B0C", fontSize = 16f),
        size = NxpSize(widthDp = 154, heightDp = 54)
    )

    val FLIP_BUMPER_RB = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.flip_rb",
            name = "Flip Bumper (RB)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.BUMPER.id,
            defaultControl = ControlKey.RB.key,
            description = "3D card flip shoulder bumper rotating 180 degrees on press from dark optical window Face A to golden radiant neon plate Face B."
        ),
        geometry = NxpGeometry(type = "RoundedRect", cornerRadius = 27f),
        visual = NxpVisual(
            fillColor = "#232527",
            opacity = 0.95f,
            borderColor = "#FF6B6B",
            borderWidth = 2f,
            glowColor = "#FF6B6B",
            glowRadius = 10f
        ),
        pressed = NxpPressedState(scale = 0.95f, fillColor = "#FF6B6B"),
        label = NxpLabel(text = "RB", color = "#FF6B6B", pressedColor = "#0A0B0C", fontSize = 16f),
        size = NxpSize(widthDp = 154, heightDp = 54)
    )

    val DIAL_TRIGGER_LT = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.dial_lt",
            name = "Dial Gauge (LT)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.TRIGGER.id,
            defaultControl = ControlKey.LT.key,
            description = "270-degree radial gauge trigger sweeping clockwise around optical window on pull."
        ),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(
            fillColor = "#151515",
            opacity = 0.92f,
            borderColor = "#00E5FF",
            borderWidth = 2.0f,
            glowColor = "#00E5FF",
            glowRadius = 10f
        ),
        pressed = NxpPressedState(scale = 0.95f, fillColor = "#00E5FF", borderColor = "#FFFFFF"),
        interaction = NxpInteraction(type = "Trigger"),
        label = NxpLabel(text = "LT", color = "#00E5FF", pressedColor = "#FFFFFF", fontSize = 17f),
        size = NxpSize(widthDp = 92, heightDp = 92)
    )

    val DIAL_TRIGGER_RT = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.dial_rt",
            name = "Dial Gauge (RT)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.TRIGGER.id,
            defaultControl = ControlKey.RT.key,
            description = "270-degree radial gauge trigger sweeping clockwise around optical window on pull."
        ),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(
            fillColor = "#151515",
            opacity = 0.92f,
            borderColor = "#E055B8",
            borderWidth = 2.0f,
            glowColor = "#E055B8",
            glowRadius = 10f
        ),
        pressed = NxpPressedState(scale = 0.95f, fillColor = "#E055B8", borderColor = "#FFFFFF"),
        interaction = NxpInteraction(type = "Trigger"),
        label = NxpLabel(text = "RT", color = "#E055B8", pressedColor = "#FFFFFF", fontSize = 17f),
        size = NxpSize(widthDp = 92, heightDp = 92)
    )

    val LIQUID_ORB_TRIGGER_LT = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.liquid_lt",
            name = "Liquid Orb (LT)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.TRIGGER.id,
            defaultControl = ControlKey.LT.key,
            description = "Glass sphere trigger with dynamic rising fluid level and swaying meniscus surface."
        ),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(
            fillColor = "#151515",
            opacity = 0.92f,
            borderColor = "#3FD2C4",
            borderWidth = 2.0f,
            glowColor = "#3FD2C4",
            glowRadius = 10f
        ),
        pressed = NxpPressedState(scale = 0.95f, fillColor = "#3FD2C4", borderColor = "#FFFFFF"),
        interaction = NxpInteraction(type = "Trigger"),
        label = NxpLabel(text = "LT", color = "#3FD2C4", pressedColor = "#FFFFFF", fontSize = 17f),
        size = NxpSize(widthDp = 92, heightDp = 92)
    )

    val LIQUID_ORB_TRIGGER_RT = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.liquid_rt",
            name = "Liquid Orb (RT)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.TRIGGER.id,
            defaultControl = ControlKey.RT.key,
            description = "Glass sphere trigger with dynamic rising fluid level and swaying meniscus surface."
        ),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(
            fillColor = "#151515",
            opacity = 0.92f,
            borderColor = "#FF5376",
            borderWidth = 2.0f,
            glowColor = "#FF5376",
            glowRadius = 10f
        ),
        pressed = NxpPressedState(scale = 0.95f, fillColor = "#FF5376", borderColor = "#FFFFFF"),
        interaction = NxpInteraction(type = "Trigger"),
        label = NxpLabel(text = "RT", color = "#FF5376", pressedColor = "#FFFFFF", fontSize = 17f),
        size = NxpSize(widthDp = 92, heightDp = 92)
    )

    val VU_SLABS_TRIGGER_LT = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.vu_lt",
            name = "VU Slabs (LT)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.TRIGGER.id,
            defaultControl = ControlKey.LT.key,
            description = "Seven-slab progressive LED audio meter trigger with balanced 22dp/34dp ergonomic taper, optical window, and dual overdrive tiers."
        ),
        geometry = NxpGeometry(type = "RoundedRect", cornerRadius = 34f),
        visual = NxpVisual(
            fillColor = "#151515",
            opacity = 0.92f,
            borderColor = "#3FD25A",
            borderWidth = 2.0f,
            glowColor = "#3FD25A",
            glowRadius = 10f
        ),
        pressed = NxpPressedState(scale = 0.95f, fillColor = "#3FD25A", borderColor = "#FFFFFF"),
        interaction = NxpInteraction(type = "Trigger"),
        label = NxpLabel(text = "LT", color = "#3FD25A", pressedColor = "#FFFFFF", fontSize = 15f),
        size = NxpSize(widthDp = 100, heightDp = 92)
    )

    val VU_SLABS_TRIGGER_RT = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.vu_rt",
            name = "VU Slabs (RT)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.TRIGGER.id,
            defaultControl = ControlKey.RT.key,
            description = "Seven-slab progressive LED audio meter trigger with balanced 22dp/34dp ergonomic taper, optical window, and dual overdrive tiers."
        ),
        geometry = NxpGeometry(type = "RoundedRect", cornerRadius = 34f),
        visual = NxpVisual(
            fillColor = "#151515",
            opacity = 0.92f,
            borderColor = "#E055B8",
            borderWidth = 2.0f,
            glowColor = "#E055B8",
            glowRadius = 10f
        ),
        pressed = NxpPressedState(scale = 0.95f, fillColor = "#E055B8", borderColor = "#FFFFFF"),
        interaction = NxpInteraction(type = "Trigger"),
        label = NxpLabel(text = "RT", color = "#E055B8", pressedColor = "#FFFFFF", fontSize = 15f),
        size = NxpSize(widthDp = 100, heightDp = 92)
    )

    val TARGET_TRIGGER_LT = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.target_lt",
            name = "Target Crosshair (LT)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.TRIGGER.id,
            defaultControl = ControlKey.LT.key,
            description = "Concentric circular radar target trigger with three rings igniting outside-in on pull."
        ),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(
            fillColor = "#151515",
            opacity = 0.92f,
            borderColor = "#00E5FF",
            borderWidth = 2.0f,
            glowColor = "#00E5FF",
            glowRadius = 10f
        ),
        pressed = NxpPressedState(scale = 0.95f, fillColor = "#00E5FF", borderColor = "#FFFFFF"),
        interaction = NxpInteraction(type = "Trigger"),
        label = NxpLabel(text = "LT", color = "#00E5FF", pressedColor = "#FFFFFF", fontSize = 14f),
        size = NxpSize(widthDp = 92, heightDp = 92)
    )

    val TARGET_TRIGGER_RT = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.target_rt",
            name = "Target Crosshair (RT)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.TRIGGER.id,
            defaultControl = ControlKey.RT.key,
            description = "Concentric circular radar target trigger with three rings igniting outside-in on pull."
        ),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(
            fillColor = "#151515",
            opacity = 0.92f,
            borderColor = "#FF5A7A",
            borderWidth = 2.0f,
            glowColor = "#FF5A7A",
            glowRadius = 10f
        ),
        pressed = NxpPressedState(scale = 0.95f, fillColor = "#FF5A7A", borderColor = "#FFFFFF"),
        interaction = NxpInteraction(type = "Trigger"),
        label = NxpLabel(text = "RT", color = "#FF5A7A", pressedColor = "#FFFFFF", fontSize = 14f),
        size = NxpSize(widthDp = 92, heightDp = 92)
    )

    val SLIDER_TRIGGER_LT = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.slider_lt",
            name = "Analog Slider (LT)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.TRIGGER.id,
            defaultControl = ControlKey.LT.key,
            description = "Continuous 0..255 analog slider trigger with sliding puck handle and spring return."
        ),
        geometry = NxpGeometry(type = "RoundedRect", cornerRadius = 21f),
        visual = NxpVisual(
            fillColor = "#151515",
            opacity = 0.92f,
            borderColor = "#FFB13F",
            borderWidth = 2.0f,
            glowColor = "#FFB13F",
            glowRadius = 10f
        ),
        pressed = NxpPressedState(scale = 0.96f, fillColor = "#FFB13F", borderColor = "#FFFFFF"),
        interaction = NxpInteraction(type = "Trigger"),
        label = NxpLabel(text = "LT", color = "#FFB13F", pressedColor = "#FFFFFF", fontSize = 11f),
        size = NxpSize(widthDp = 42, heightDp = 94)
    )

    val SLIDER_TRIGGER_RT = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.slider_rt",
            name = "Analog Slider (RT)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.TRIGGER.id,
            defaultControl = ControlKey.RT.key,
            description = "Continuous 0..255 analog slider trigger with sliding puck handle and spring return."
        ),
        geometry = NxpGeometry(type = "RoundedRect", cornerRadius = 21f),
        visual = NxpVisual(
            fillColor = "#151515",
            opacity = 0.92f,
            borderColor = "#FF5A7A",
            borderWidth = 2.0f,
            glowColor = "#FF5A7A",
            glowRadius = 10f
        ),
        pressed = NxpPressedState(scale = 0.96f, fillColor = "#FF5A7A", borderColor = "#FFFFFF"),
        interaction = NxpInteraction(type = "Trigger"),
        label = NxpLabel(text = "RT", color = "#FF5A7A", pressedColor = "#FFFFFF", fontSize = 11f),
        size = NxpSize(widthDp = 42, heightDp = 94)
    )

    val NEEDLE_TRIGGER_LT = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.needle_lt",
            name = "Needle Meter (LT)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.TRIGGER.id,
            defaultControl = ControlKey.LT.key,
            description = "Analog meter trigger with swinging needle and illuminated progress arc."
        ),
        geometry = NxpGeometry(type = "RoundedRect", cornerRadius = 54f),
        visual = NxpVisual(
            fillColor = "#151515",
            opacity = 0.92f,
            borderColor = "#5CF29A",
            borderWidth = 2.0f,
            glowColor = "#5CF29A",
            glowRadius = 10f
        ),
        pressed = NxpPressedState(scale = 0.96f, fillColor = "#5CF29A", borderColor = "#FFFFFF"),
        interaction = NxpInteraction(type = "Trigger"),
        label = NxpLabel(text = "LT", color = "#5CF29A", pressedColor = "#FFFFFF", fontSize = 12f),
        size = NxpSize(widthDp = 108, heightDp = 94)
    )

    val NEEDLE_TRIGGER_RT = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.needle_rt",
            name = "Needle Meter (RT)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.TRIGGER.id,
            defaultControl = ControlKey.RT.key,
            description = "Analog meter trigger with swinging needle and illuminated progress arc."
        ),
        geometry = NxpGeometry(type = "RoundedRect", cornerRadius = 54f),
        visual = NxpVisual(
            fillColor = "#151515",
            opacity = 0.92f,
            borderColor = "#FF5C8A",
            borderWidth = 2.0f,
            glowColor = "#FF5C8A",
            glowRadius = 10f
        ),
        pressed = NxpPressedState(scale = 0.96f, fillColor = "#FF5C8A", borderColor = "#FFFFFF"),
        interaction = NxpInteraction(type = "Trigger"),
        label = NxpLabel(text = "RT", color = "#FF5C8A", pressedColor = "#FFFFFF", fontSize = 12f),
        size = NxpSize(widthDp = 108, heightDp = 94)
    )

    val TESTTUBE_TRIGGER_LT = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.testtube_lt",
            name = "Test Tube (LT)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.TRIGGER.id,
            defaultControl = ControlKey.LT.key,
            description = "Glass test tube trigger with rising liquid, sway oscillation, and floating bubbles."
        ),
        geometry = NxpGeometry(type = "RoundedRect", cornerRadius = 24f),
        visual = NxpVisual(
            fillColor = "#151515",
            opacity = 0.92f,
            borderColor = "#FF9F43",
            borderWidth = 2.0f,
            glowColor = "#FF9F43",
            glowRadius = 10f
        ),
        pressed = NxpPressedState(scale = 0.96f, fillColor = "#FF9F43", borderColor = "#FFFFFF"),
        interaction = NxpInteraction(type = "Trigger"),
        label = NxpLabel(text = "LT", color = "#FF9F43", pressedColor = "#FFFFFF", fontSize = 10f),
        size = NxpSize(widthDp = 48, heightDp = 98)
    )

    val TESTTUBE_TRIGGER_RT = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.testtube_rt",
            name = "Test Tube (RT)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.TRIGGER.id,
            defaultControl = ControlKey.RT.key,
            description = "Glass test tube trigger with rising liquid, sway oscillation, and floating bubbles."
        ),
        geometry = NxpGeometry(type = "RoundedRect", cornerRadius = 24f),
        visual = NxpVisual(
            fillColor = "#151515",
            opacity = 0.92f,
            borderColor = "#BD5CFF",
            borderWidth = 2.0f,
            glowColor = "#BD5CFF",
            glowRadius = 10f
        ),
        pressed = NxpPressedState(scale = 0.96f, fillColor = "#BD5CFF", borderColor = "#FFFFFF"),
        interaction = NxpInteraction(type = "Trigger"),
        label = NxpLabel(text = "RT", color = "#BD5CFF", pressedColor = "#FFFFFF", fontSize = 10f),
        size = NxpSize(widthDp = 48, heightDp = 98)
    )

    val BLOOM_TRIGGER_LT = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.bloom_lt",
            name = "Bloom Light (LT)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.TRIGGER.id,
            defaultControl = ControlKey.LT.key,
            description = "Circular trigger with expanding radiant light bloom and bright halo rim."
        ),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(
            fillColor = "#151515",
            opacity = 0.92f,
            borderColor = "#7C9CFF",
            borderWidth = 2.0f,
            glowColor = "#7C9CFF",
            glowRadius = 10f
        ),
        pressed = NxpPressedState(scale = 0.96f, fillColor = "#7C9CFF", borderColor = "#FFFFFF"),
        interaction = NxpInteraction(type = "Trigger"),
        label = NxpLabel(text = "LT", color = "#7C9CFF", pressedColor = "#FFFFFF", fontSize = 12f),
        size = NxpSize(widthDp = 92, heightDp = 92)
    )

    val BLOOM_TRIGGER_RT = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.bloom_rt",
            name = "Bloom Light (RT)",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.TRIGGER.id,
            defaultControl = ControlKey.RT.key,
            description = "Circular trigger with expanding radiant light bloom and bright halo rim."
        ),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(
            fillColor = "#151515",
            opacity = 0.92f,
            borderColor = "#FF6584",
            borderWidth = 2.0f,
            glowColor = "#FF6584",
            glowRadius = 10f
        ),
        pressed = NxpPressedState(scale = 0.96f, fillColor = "#FF6584", borderColor = "#FFFFFF"),
        interaction = NxpInteraction(type = "Trigger"),
        label = NxpLabel(text = "RT", color = "#FF6584", pressedColor = "#FFFFFF", fontSize = 12f),
        size = NxpSize(widthDp = 92, heightDp = 92)
    )

    val DEFAULT_HOME_XBOX = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.default_xbox",
            name = "Default Nexus Guide",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.SYSTEM.id,
            defaultControl = ControlKey.GUIDE.key,
            description = "Iconic centered Xbox guide button with luminous white badge."
        ),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(
            fillColor = "#181818",
            opacity = 0.95f,
            borderColor = "#00F0FF",
            borderWidth = 2f,
            glowColor = "#00F0FF",
            glowRadius = 12f
        ),
        pressed = NxpPressedState(scale = 0.88f, fillColor = "#00F0FF", borderColor = "#FFFFFF"),
        label = NxpLabel(text = "⨂", color = "#00F0FF", pressedColor = "#000000", fontSize = 24f),
        size = NxpSize(widthDp = 60, heightDp = 60)
    )

    val DEFAULT_ORBIT_HOME = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.orbit_guide",
            name = "Orbit Home Button",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.SYSTEM.id,
            defaultControl = ControlKey.GUIDE.key,
            description = "Optical lens system button with 3-segment dashed orbit ring rotating 120 degrees on press and glowing core dot."
        ),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(
            fillColor = "#181818",
            opacity = 0.95f,
            borderColor = "#00F0FF",
            borderWidth = 2f,
            glowColor = "#00F0FF",
            glowRadius = 14f
        ),
        pressed = NxpPressedState(scale = 0.95f, fillColor = "#00F0FF", borderColor = "#FFFFFF"),
        label = NxpLabel(text = "ORB", color = "#00F0FF", pressedColor = "#FFFFFF", fontSize = 16f),
        size = NxpSize(widthDp = 74, heightDp = 74)
    )

    val DEFAULT_SYSTEM_VIEW = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.default_view",
            name = "Default View Button",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.SYSTEM.id,
            defaultControl = ControlKey.BACK.key,
            description = "Xbox View / Back system navigation button."
        ),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(
            fillColor = "#262626",
            opacity = 0.90f,
            borderColor = "#94A3B8",
            borderWidth = 1.5f
        ),
        pressed = NxpPressedState(scale = 0.88f, fillColor = "#94A3B8"),
        label = NxpLabel(text = "⧉", color = "#FFFFFF", pressedColor = "#000000", fontSize = 18f),
        size = NxpSize(widthDp = 60, heightDp = 60)
    )

    val DEFAULT_SYSTEM_MENU = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.default_menu",
            name = "Default Menu Button",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.SYSTEM.id,
            defaultControl = ControlKey.START.key,
            description = "Xbox Menu / Pause system navigation button."
        ),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(
            fillColor = "#262626",
            opacity = 0.90f,
            borderColor = "#94A3B8",
            borderWidth = 1.5f
        ),
        pressed = NxpPressedState(scale = 0.88f, fillColor = "#94A3B8"),
        label = NxpLabel(text = "☰", color = "#FFFFFF", pressedColor = "#000000", fontSize = 18f),
        size = NxpSize(widthDp = 60, heightDp = 60)
    )

    val DEFAULT_SYSTEM_SHARE = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.default_share",
            name = "Default Share Button",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.SYSTEM.id,
            defaultControl = ControlKey.SHARE.key,
            description = "Xbox Share / Screenshot capture system button."
        ),
        geometry = NxpGeometry(type = "Circle"),
        visual = NxpVisual(
            fillColor = "#262626",
            opacity = 0.90f,
            borderColor = "#94A3B8",
            borderWidth = 1.5f
        ),
        pressed = NxpPressedState(scale = 0.88f, fillColor = "#94A3B8"),
        label = NxpLabel(text = "⇪", color = "#FFFFFF", pressedColor = "#000000", fontSize = 18f),
        size = NxpSize(widthDp = 60, heightDp = 60)
    )

    val DEFAULT_MACRO_M1 = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.default_m1",
            name = "Default Macro Paddle M1",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.MACRO.id,
            defaultControl = ControlKey.M1.key,
            description = "Standard ergonomic programmable macro paddle M1."
        ),
        geometry = NxpGeometry(type = "RoundedRect", cornerRadius = 16f),
        visual = NxpVisual(
            fillColor = "#1C1C1E",
            opacity = 0.92f,
            borderColor = "#FACC15",
            borderWidth = 1.5f,
            glowColor = "#FACC15",
            glowRadius = 6f
        ),
        pressed = NxpPressedState(scale = 0.92f, fillColor = "#FACC15", borderColor = "#FFFFFF"),
        label = NxpLabel(text = "M1", color = "#FACC15", pressedColor = "#000000", fontSize = 14f),
        size = NxpSize(widthDp = 80, heightDp = 40)
    )

    val DEFAULT_MACRO_M2 = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.default_m2",
            name = "Default Macro Paddle M2",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.MACRO.id,
            defaultControl = ControlKey.M2.key,
            description = "Standard ergonomic programmable macro paddle M2."
        ),
        geometry = NxpGeometry(type = "RoundedRect", cornerRadius = 16f),
        visual = NxpVisual(
            fillColor = "#1C1C1E",
            opacity = 0.92f,
            borderColor = "#FACC15",
            borderWidth = 1.5f,
            glowColor = "#FACC15",
            glowRadius = 6f
        ),
        pressed = NxpPressedState(scale = 0.92f, fillColor = "#FACC15", borderColor = "#FFFFFF"),
        label = NxpLabel(text = "M2", color = "#FACC15", pressedColor = "#000000", fontSize = 14f),
        size = NxpSize(widthDp = 80, heightDp = 40)
    )

    val DEFAULT_MACRO_M3 = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.default_m3",
            name = "Default Macro Paddle M3",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.MACRO.id,
            defaultControl = ControlKey.M3.key,
            description = "Standard ergonomic programmable macro paddle M3."
        ),
        geometry = NxpGeometry(type = "RoundedRect", cornerRadius = 16f),
        visual = NxpVisual(
            fillColor = "#1C1C1E",
            opacity = 0.92f,
            borderColor = "#FACC15",
            borderWidth = 1.5f,
            glowColor = "#FACC15",
            glowRadius = 6f
        ),
        pressed = NxpPressedState(scale = 0.92f, fillColor = "#FACC15", borderColor = "#FFFFFF"),
        label = NxpLabel(text = "M3", color = "#FACC15", pressedColor = "#000000", fontSize = 14f),
        size = NxpSize(widthDp = 80, heightDp = 40)
    )

    val DEFAULT_MACRO_M4 = NxpComponentDef(
        manifest = NxpManifest(
            id = "builtin.default_m4",
            name = "Default Macro Paddle M4",
            author = "NEXPAD Core",
            version = "1.0.0",
            category = NxprcCategory.MACRO.id,
            defaultControl = ControlKey.M4.key,
            description = "Standard ergonomic programmable macro paddle M4."
        ),
        geometry = NxpGeometry(type = "RoundedRect", cornerRadius = 16f),
        visual = NxpVisual(
            fillColor = "#1C1C1E",
            opacity = 0.92f,
            borderColor = "#FACC15",
            borderWidth = 1.5f,
            glowColor = "#FACC15",
            glowRadius = 6f
        ),
        pressed = NxpPressedState(scale = 0.92f, fillColor = "#FACC15", borderColor = "#FFFFFF"),
        label = NxpLabel(text = "M4", color = "#FACC15", pressedColor = "#000000", fontSize = 14f),
        size = NxpSize(widthDp = 80, heightDp = 40)
    )

    /**
     * All built-in presets: 19 Default Controller Buttons first, followed by Custom/Cyberpunk skins.
     * All entries are non-deletable core presets.
     */
    val ALL_PRESETS = listOf(
        // 19 Default Controller Elements (Always Top Priority)
        DEFAULT_BUTTON_A,
        DEFAULT_BUTTON_B,
        DEFAULT_BUTTON_X,
        DEFAULT_BUTTON_Y,
        DEFAULT_STICK_LS,
        DEFAULT_STICK_RS,
        DEFAULT_STICK_LSB,
        DEFAULT_STICK_RSB,
        DEFAULT_STICK_LTP,
        DEFAULT_STICK_RTP,
        DEFAULT_DPAD,
        DEFAULT_DPAD_UP,
        DEFAULT_DPAD_DOWN,
        DEFAULT_DPAD_LEFT,
        DEFAULT_DPAD_RIGHT,
        DEFAULT_TRIGGER_LT,
        DEFAULT_TRIGGER_RT,
        DEFAULT_BUMPER_LB,
        DEFAULT_BUMPER_RB,
        DEFAULT_HOME_XBOX,
        DEFAULT_ORBIT_HOME,
        DEFAULT_SYSTEM_VIEW,
        DEFAULT_SYSTEM_MENU,
        DEFAULT_SYSTEM_SHARE,
        DEFAULT_MACRO_M1,
        DEFAULT_MACRO_M2,
        DEFAULT_MACRO_M3,
        DEFAULT_MACRO_M4,

        // Flux Cyber Suite
        FLUX_STICK_LS,
        FLUX_STICK_RS,
        FLUX_STICK_LSB,
        FLUX_STICK_RSB,

        // Orb Glass Suite
        ORB_STICK_LS,
        ORB_STICK_RS,
        ORB_STICK_LSB,
        ORB_STICK_RSB,

        // Compass Nav Suite
        COMPASS_STICK_LS,
        COMPASS_STICK_RS,
        COMPASS_STICK_LSB,
        COMPASS_STICK_RSB,

        // Gyro Gimbal Suite
        GYRO_STICK_LS,
        GYRO_STICK_RS,
        GYRO_STICK_LSB,
        GYRO_STICK_RSB,

        // Spotlight Suite
        SPOTLIGHT_STICK_LS,
        SPOTLIGHT_STICK_RS,
        SPOTLIGHT_STICK_LSB,
        SPOTLIGHT_STICK_RSB,

        // Lens D-Pad Variants
        LENS_DPAD,
        FOUR_LENSES_DPAD,
        DISC_DPAD,
        CAPSULES_DPAD,
        METABALLS_DPAD,
        RAILS_DPAD,

        // Arc Bumper Variants
        ARC_BUMPER_LB,
        ARC_BUMPER_RB,

        // LED Bar Bumper Variants
        LED_BUMPER_LB,
        LED_BUMPER_RB,

        // Peek Bumper Variants
        PEEK_BUMPER_LB,
        PEEK_BUMPER_RB,

        // Ribbed Bumper Variants
        RIBBED_BUMPER_LB,
        RIBBED_BUMPER_RB,

        // Underglow Bumper Variants
        UNDERGLOW_BUMPER_LB,
        UNDERGLOW_BUMPER_RB,

        // Tube Bumper Variants
        TUBE_BUMPER_LB,
        TUBE_BUMPER_RB,

        // Flip Bumper Variants
        FLIP_BUMPER_LB,
        FLIP_BUMPER_RB,

        // Dial Trigger Variants
        DIAL_TRIGGER_LT,
        DIAL_TRIGGER_RT,

        // Liquid Orb Trigger Variants
        LIQUID_ORB_TRIGGER_LT,
        LIQUID_ORB_TRIGGER_RT,

        // VU Slabs Trigger Variants
        VU_SLABS_TRIGGER_LT,
        VU_SLABS_TRIGGER_RT,

        // Target Trigger Variants
        TARGET_TRIGGER_LT,
        TARGET_TRIGGER_RT,

        // Slider Trigger Variants
        SLIDER_TRIGGER_LT,
        SLIDER_TRIGGER_RT,

        // Needle Meter Trigger Variants
        NEEDLE_TRIGGER_LT,
        NEEDLE_TRIGGER_RT,

        // Test Tube Trigger Variants
        TESTTUBE_TRIGGER_LT,
        TESTTUBE_TRIGGER_RT,

        // Bloom Trigger Variants
        BLOOM_TRIGGER_LT,
        BLOOM_TRIGGER_RT,

        // Custom Cyber / Sci-Fi Variant Skins
        SCIFI_HEX_ATTACK,
        SCIFI_HEX_Y,
        SCIFI_HEX_X,
        SCIFI_HEX_B,
        CYBER_OCTA_BURST,
        CYBER_OCTA_Y,
        CYBER_OCTA_X,
        CYBER_OCTA_A,
        NEON_DIAMOND_X,
        PLASMA_TRIANGLE_Y,
        NEON_PULSE_B,
        CYBER_BUMPER_LB,
        CYBER_BUMPER_RB,
        NEON_CYAN_BUMPER_LB,
        NEON_CYAN_BUMPER_RB,
        STEALTH_CARBON_LB,
        STEALTH_CARBON_RB,
        CRIMSON_MECHA_LB,
        CRIMSON_MECHA_RB,
        PULSE_TRIGGER_LT,
        PULSE_TRIGGER_RT,
        NEON_MATRIX_JOYSTICK,
        CYBER_VORTEX_RS,
        HOLO_CROSS_DPAD,
        CYBER_DPAD_UP,
        CYBER_DPAD_DOWN,
        CYBER_DPAD_LEFT,
        CYBER_DPAD_RIGHT,
        NEXUS_ORB_HOME,
        TACTICAL_SLIM_MENU,
        MACRO_PILL_M1,
        LIQUID_BUTTON_A, LIQUID_BUTTON_B, LIQUID_BUTTON_X, LIQUID_BUTTON_Y,
        FACET_BUTTON_A, FACET_BUTTON_B, FACET_BUTTON_X, FACET_BUTTON_Y,
        FLIP_BUTTON_A, FLIP_BUTTON_B, FLIP_BUTTON_X, FLIP_BUTTON_Y,
        RIPPLE_BUTTON_A, RIPPLE_BUTTON_B, RIPPLE_BUTTON_X, RIPPLE_BUTTON_Y,
        ORBIT_BUTTON_A, ORBIT_BUTTON_B, ORBIT_BUTTON_X, ORBIT_BUTTON_Y,
        CAPSULES_BUTTON_A, CAPSULES_BUTTON_B, CAPSULES_BUTTON_X, CAPSULES_BUTTON_Y,
        ECLIPSE_BUTTON_A, ECLIPSE_BUTTON_B, ECLIPSE_BUTTON_X, ECLIPSE_BUTTON_Y
    )
}
