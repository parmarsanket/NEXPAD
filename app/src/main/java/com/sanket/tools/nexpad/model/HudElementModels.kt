package com.sanket.tools.nexpad.model

import com.sanket.tools.nexpad.runtime.model.NxpComponentDef

/**
 * High-level categories for gamepad controls.
 * Enforces strict skin-compatibility so an analog stick skin cannot be applied to a button.
 */
enum class ControlCategory(val displayName: String) {
    BUTTON("ABXY Buttons"),
    JOYSTICK("Thumbsticks"),
    DPAD("Directional Pad"),
    TRIGGER("Triggers"),
    BUMPER("Bumpers"),
    HOME("Home / Guide"),
    SYSTEM("System Buttons"),
    MACRO("Macro Paddles")
}

/**
 * Strongly typed enumeration of all 19 standard controller controls supported by NEXPAD.
 */
enum class GamepadControl(
    val key: String,
    val displayName: String,
    val category: ControlCategory
) {
    // Face Buttons
    A("A", "Button A", ControlCategory.BUTTON),
    B("B", "Button B", ControlCategory.BUTTON),
    X("X", "Button X", ControlCategory.BUTTON),
    Y("Y", "Button Y", ControlCategory.BUTTON),

    // Analog Sticks
    LS("LS", "Left Stick (LS)", ControlCategory.JOYSTICK),
    RS("RS", "Right Stick (RS)", ControlCategory.JOYSTICK),

    // Directional Cross & Discrete Buttons
    DPAD("DPAD", "D-Pad", ControlCategory.DPAD),
    UP("UP", "D-Pad Up", ControlCategory.DPAD),
    DOWN("DOWN", "D-Pad Down", ControlCategory.DPAD),
    LEFT("LEFT", "D-Pad Left", ControlCategory.DPAD),
    RIGHT("RIGHT", "D-Pad Right", ControlCategory.DPAD),

    // Triggers
    LT("LT", "Left Trigger (LT)", ControlCategory.TRIGGER),
    RT("RT", "Right Trigger (RT)", ControlCategory.TRIGGER),

    // Bumpers
    LB("LB", "Left Bumper (LB)", ControlCategory.BUMPER),
    RB("RB", "Right Bumper (RB)", ControlCategory.BUMPER),

    // Home / Guide
    XBOX("XBOX", "Xbox Guide", ControlCategory.HOME),

    // System Buttons
    VIEW("VIEW", "View / Back", ControlCategory.SYSTEM),
    MENU("MENU", "Menu / Start", ControlCategory.SYSTEM),
    SHARE("SHARE", "Share / Capture", ControlCategory.SYSTEM),

    // Rear Macro Paddles
    M1("M1", "Paddle M1", ControlCategory.MACRO),
    M2("M2", "Paddle M2", ControlCategory.MACRO),
    M3("M3", "Paddle M3", ControlCategory.MACRO),
    M4("M4", "Paddle M4", ControlCategory.MACRO);

    companion object {
        private val keyMap = entries.associateBy { it.key.uppercase() }

        fun fromKey(key: String): GamepadControl? = keyMap[key.uppercase()]
    }
}

/**
 * Normalized 2D transform for positioning controls on any display ratio.
 */
data class LayoutTransform(
    val xRatio: Float,
    val yRatio: Float,
    val scale: Float = 1.0f,
    val opacity: Float = 1.0f
)

/**
 * Unified representation of a button's visual skin.
 */
sealed interface LayoutSkin {
    /** Authentic 3D realistic controller button from ui/components/controller/ */
    data object NativeDefault : LayoutSkin

    /** Vector, SVG, or dynamic Compose plugin loaded from ComponentRegistry */
    data class CustomComponent(val def: NxpComponentDef) : LayoutSkin
}

/**
 * Concrete placed HUD element representing one controller control on screen.
 */
data class HudElement(
    val control: GamepadControl,
    val transform: LayoutTransform,
    val skinId: String? = null // null indicates LayoutSkin.NativeDefault
) {
    fun toPosition(): Position = Position(
        xRatio = transform.xRatio,
        yRatio = transform.yRatio,
        scale = transform.scale,
        opacity = transform.opacity,
        customComponentId = skinId
    )

    companion object {
        fun fromPosition(key: String, position: Position): HudElement? {
            val control = GamepadControl.fromKey(key) ?: return null
            return HudElement(
                control = control,
                transform = LayoutTransform(
                    xRatio = position.xRatio,
                    yRatio = position.yRatio,
                    scale = position.scale,
                    opacity = position.opacity
                ),
                skinId = position.customComponentId
            )
        }
    }
}
