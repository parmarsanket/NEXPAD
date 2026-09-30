package com.sanket.tools.nexpad.runtime.registry

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.sanket.tools.nexpad.category.CategoryManager
import com.sanket.tools.nexpad.category.CategoryType
import com.sanket.tools.nexpad.category.ControlKey
import com.sanket.tools.nexpad.category.ControllerLabelStyle
import com.sanket.tools.nexpad.model.NexpadKeys as K
import com.sanket.tools.nexpad.ui.components.controller.*
import com.sanket.tools.nexpad.ui.studio.components.*
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel

/**
 * Encapsulated context containing all runtime state, haptics, and geometry
 * needed to render an interactive native controller element.
 */
data class NativeRenderContext(
    val controlKey: ControlKey,
    val key: String,
    val isConnected: Boolean,
    val isRgbEnabled: Boolean,
    val viewModel: GamepadViewModel,
    val onVibrate: () -> Unit = {},
    val sensitivity: Float? = null,
    val labelStyle: ControllerLabelStyle = ControllerLabelStyle.XBOX,
    val modifier: Modifier = Modifier
) {
    val displayLabel: String get() = CategoryManager.getLabelForStyle(key, labelStyle)
}

/**
 * Encapsulated context for rendering a high-performance static preview thumbnail.
 */
data class NativePreviewContext(
    val controlKey: ControlKey,
    val key: String = controlKey.key,
    val labelStyle: ControllerLabelStyle = ControllerLabelStyle.XBOX,
    val modifier: Modifier = Modifier
)

/**
 * OOP Contract: Every native component variant added to the unified Default Family
 * (whether based on internet references, HTML models, or console hardware) implements this interface.
 * Each variant carries a unique [seedCode] for tracking and deterministic selection.
 */
interface NativeElementVariant {
    val id: String
    val controlKey: ControlKey
    val variantName: String
    val seedCode: Int
    val isBaselineDefault: Boolean get() = seedCode % 100 == 1

    @Composable
    fun RenderInteractive(context: NativeRenderContext)

    @Composable
    fun RenderStaticPreview(context: NativePreviewContext)
}

/**
 * Abstract base class providing common properties and convenience helpers for native variants.
 */
abstract class BaseNativeVariant(
    override val id: String,
    override val controlKey: ControlKey,
    override val variantName: String,
    override val seedCode: Int
) : NativeElementVariant

// =========================================================================
// JOYSTICK VARIANTS (LS / RS)
// =========================================================================

object RealisticLeftJoystickVariant : BaseNativeVariant("builtin.default_ls", ControlKey.LS, "Realistic 3D", 101) {
    override val isBaselineDefault: Boolean = true

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        RealisticJoystick(
            isLeft = true,
            isConnected = context.isConnected,
            viewModel = context.viewModel,
            isRgbEnabled = context.isRgbEnabled,
            modifier = context.modifier
        )
    }

    @Composable
    override fun RenderStaticPreview(context: NativePreviewContext) {
        StaticDefaultButtonPreview(
            controlKey = K.LS,
            labelStyle = context.labelStyle,
            modifier = context.modifier
        )
    }
}

object FluxLeftJoystickVariant : BaseNativeVariant("builtin.flux_ls", ControlKey.LS, "Flux Cyber", 102) {
    override val isBaselineDefault: Boolean = false

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        FluxJoystick(
            isLeft = true,
            isConnected = context.isConnected,
            viewModel = context.viewModel,
            isRgbEnabled = context.isRgbEnabled,
            modifier = context.modifier
        )
    }

    @Composable
    override fun RenderStaticPreview(context: NativePreviewContext) {
        StaticFluxJoystick(
            isLeft = true,
            modifier = context.modifier
        )
    }
}

object RealisticRightJoystickVariant : BaseNativeVariant("builtin.default_rs", ControlKey.RS, "Realistic 3D", 201) {
    override val isBaselineDefault: Boolean = true

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        RealisticJoystick(
            isLeft = false,
            isConnected = context.isConnected,
            viewModel = context.viewModel,
            isRgbEnabled = context.isRgbEnabled,
            modifier = context.modifier
        )
    }

    @Composable
    override fun RenderStaticPreview(context: NativePreviewContext) {
        StaticDefaultButtonPreview(
            controlKey = K.RS,
            labelStyle = context.labelStyle,
            modifier = context.modifier
        )
    }
}

object FluxRightJoystickVariant : BaseNativeVariant("builtin.flux_rs", ControlKey.RS, "Flux Cyber", 202) {
    override val isBaselineDefault: Boolean = false

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        FluxJoystick(
            isLeft = false,
            isConnected = context.isConnected,
            viewModel = context.viewModel,
            isRgbEnabled = context.isRgbEnabled,
            modifier = context.modifier
        )
    }

    @Composable
    override fun RenderStaticPreview(context: NativePreviewContext) {
        StaticFluxJoystick(
            isLeft = false,
            modifier = context.modifier
        )
    }
}

object OrbLeftJoystickVariant : BaseNativeVariant("builtin.orb_ls", ControlKey.LS, "Orb Glass", 103) {
    override val isBaselineDefault: Boolean = false

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        OrbJoystick(
            isLeft = true,
            isConnected = context.isConnected,
            viewModel = context.viewModel,
            isRgbEnabled = context.isRgbEnabled,
            modifier = context.modifier
        )
    }

    @Composable
    override fun RenderStaticPreview(context: NativePreviewContext) {
        StaticOrbJoystick(
            isLeft = true,
            modifier = context.modifier
        )
    }
}

object OrbRightJoystickVariant : BaseNativeVariant("builtin.orb_rs", ControlKey.RS, "Orb Glass", 203) {
    override val isBaselineDefault: Boolean = false

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        OrbJoystick(
            isLeft = false,
            isConnected = context.isConnected,
            viewModel = context.viewModel,
            isRgbEnabled = context.isRgbEnabled,
            modifier = context.modifier
        )
    }

    @Composable
    override fun RenderStaticPreview(context: NativePreviewContext) {
        StaticOrbJoystick(
            isLeft = false,
            modifier = context.modifier
        )
    }
}

object CompassLeftJoystickVariant : BaseNativeVariant("builtin.compass_ls", ControlKey.LS, "Compass Nav", 104) {
    override val isBaselineDefault: Boolean = false

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        CompassJoystick(
            isLeft = true,
            isConnected = context.isConnected,
            viewModel = context.viewModel,
            isRgbEnabled = context.isRgbEnabled,
            modifier = context.modifier
        )
    }

    @Composable
    override fun RenderStaticPreview(context: NativePreviewContext) {
        StaticCompassJoystick(
            isLeft = true,
            modifier = context.modifier
        )
    }
}

object CompassRightJoystickVariant : BaseNativeVariant("builtin.compass_rs", ControlKey.RS, "Compass Nav", 204) {
    override val isBaselineDefault: Boolean = false

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        CompassJoystick(
            isLeft = false,
            isConnected = context.isConnected,
            viewModel = context.viewModel,
            isRgbEnabled = context.isRgbEnabled,
            modifier = context.modifier
        )
    }

    @Composable
    override fun RenderStaticPreview(context: NativePreviewContext) {
        StaticCompassJoystick(
            isLeft = false,
            modifier = context.modifier
        )
    }
}

// =========================================================================
// STICK BUTTON VARIANTS (LSB / RSB or L3 / R3)
// =========================================================================

object RealisticLeftStickButtonVariant : BaseNativeVariant("builtin.default_lsb", ControlKey.LSB, "Realistic 3D", 301) {
    override val isBaselineDefault: Boolean = true

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        RealisticStickButton(
            isLeft = true,
            key = K.LSB,
            isConnected = context.isConnected,
            onVibrate = context.onVibrate,
            viewModel = context.viewModel,
            isRgbEnabled = context.isRgbEnabled,
            displayLabel = context.displayLabel,
            modifier = context.modifier
        )
    }

    @Composable
    override fun RenderStaticPreview(context: NativePreviewContext) {
        StaticDefaultButtonPreview(
            controlKey = K.LSB,
            labelStyle = context.labelStyle,
            modifier = context.modifier
        )
    }
}

object FluxLeftStickButtonVariant : BaseNativeVariant("builtin.flux_lsb", ControlKey.LSB, "Flux Cyber", 302) {
    override val isBaselineDefault: Boolean = false

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        FluxStickButton(
            isLeft = true,
            key = K.LSB,
            isConnected = context.isConnected,
            onVibrate = context.onVibrate,
            viewModel = context.viewModel,
            isRgbEnabled = context.isRgbEnabled,
            displayLabel = context.displayLabel,
            modifier = context.modifier
        )
    }

    @Composable
    override fun RenderStaticPreview(context: NativePreviewContext) {
        StaticFluxStickButton(
            isLeft = true,
            modifier = context.modifier
        )
    }
}

object RealisticRightStickButtonVariant : BaseNativeVariant("builtin.default_rsb", ControlKey.RSB, "Realistic 3D", 401) {
    override val isBaselineDefault: Boolean = true

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        RealisticStickButton(
            isLeft = false,
            key = K.RSB,
            isConnected = context.isConnected,
            onVibrate = context.onVibrate,
            viewModel = context.viewModel,
            isRgbEnabled = context.isRgbEnabled,
            displayLabel = context.displayLabel,
            modifier = context.modifier
        )
    }

    @Composable
    override fun RenderStaticPreview(context: NativePreviewContext) {
        StaticDefaultButtonPreview(
            controlKey = K.RSB,
            labelStyle = context.labelStyle,
            modifier = context.modifier
        )
    }
}

object FluxRightStickButtonVariant : BaseNativeVariant("builtin.flux_rsb", ControlKey.RSB, "Flux Cyber", 402) {
    override val isBaselineDefault: Boolean = false

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        FluxStickButton(
            isLeft = false,
            key = K.RSB,
            isConnected = context.isConnected,
            onVibrate = context.onVibrate,
            viewModel = context.viewModel,
            isRgbEnabled = context.isRgbEnabled,
            displayLabel = context.displayLabel,
            modifier = context.modifier
        )
    }

    @Composable
    override fun RenderStaticPreview(context: NativePreviewContext) {
        StaticFluxStickButton(
            isLeft = false,
            modifier = context.modifier
        )
    }
}

object OrbLeftStickButtonVariant : BaseNativeVariant("builtin.orb_lsb", ControlKey.LSB, "Orb Glass", 303) {
    override val isBaselineDefault: Boolean = false

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        OrbStickButton(
            isLeft = true,
            key = K.LSB,
            isConnected = context.isConnected,
            onVibrate = context.onVibrate,
            viewModel = context.viewModel,
            isRgbEnabled = context.isRgbEnabled,
            displayLabel = context.displayLabel,
            modifier = context.modifier
        )
    }

    @Composable
    override fun RenderStaticPreview(context: NativePreviewContext) {
        StaticOrbStickButton(
            isLeft = true,
            modifier = context.modifier
        )
    }
}

object OrbRightStickButtonVariant : BaseNativeVariant("builtin.orb_rsb", ControlKey.RSB, "Orb Glass", 403) {
    override val isBaselineDefault: Boolean = false

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        OrbStickButton(
            isLeft = false,
            key = K.RSB,
            isConnected = context.isConnected,
            onVibrate = context.onVibrate,
            viewModel = context.viewModel,
            isRgbEnabled = context.isRgbEnabled,
            displayLabel = context.displayLabel,
            modifier = context.modifier
        )
    }

    @Composable
    override fun RenderStaticPreview(context: NativePreviewContext) {
        StaticOrbStickButton(
            isLeft = false,
            modifier = context.modifier
        )
    }
}

object CompassLeftStickButtonVariant : BaseNativeVariant("builtin.compass_lsb", ControlKey.LSB, "Compass Nav", 304) {
    override val isBaselineDefault: Boolean = false

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        CompassStickButton(
            isLeft = true,
            key = K.LSB,
            isConnected = context.isConnected,
            onVibrate = context.onVibrate,
            viewModel = context.viewModel,
            isRgbEnabled = context.isRgbEnabled,
            displayLabel = context.displayLabel,
            modifier = context.modifier
        )
    }

    @Composable
    override fun RenderStaticPreview(context: NativePreviewContext) {
        StaticCompassStickButton(
            isLeft = true,
            modifier = context.modifier
        )
    }
}

object CompassRightStickButtonVariant : BaseNativeVariant("builtin.compass_rsb", ControlKey.RSB, "Compass Nav", 404) {
    override val isBaselineDefault: Boolean = false

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        CompassStickButton(
            isLeft = false,
            key = K.RSB,
            isConnected = context.isConnected,
            onVibrate = context.onVibrate,
            viewModel = context.viewModel,
            isRgbEnabled = context.isRgbEnabled,
            displayLabel = context.displayLabel,
            modifier = context.modifier
        )
    }

    @Composable
    override fun RenderStaticPreview(context: NativePreviewContext) {
        StaticCompassStickButton(
            isLeft = false,
            modifier = context.modifier
        )
    }
}

// =========================================================================
// ACTION BUTTON VARIANTS (A / B / X / Y)
// =========================================================================

class RealisticButtonVariant(
    controlKey: ControlKey,
    private val buttonColor: Color,
    seedCode: Int
) : BaseNativeVariant("builtin.default_${controlKey.key.lowercase()}", controlKey, "Realistic 3D", seedCode) {
    override val isBaselineDefault: Boolean = true

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        RealisticButton(
            key = controlKey.key,
            buttonColor = buttonColor,
            isConnected = context.isConnected,
            onVibrate = context.onVibrate,
            viewModel = context.viewModel,
            isRgbEnabled = context.isRgbEnabled,
            displayLabel = context.displayLabel,
            modifier = context.modifier
        )
    }

    @Composable
    override fun RenderStaticPreview(context: NativePreviewContext) {
        StaticDefaultButtonPreview(
            controlKey = controlKey.key,
            labelStyle = context.labelStyle,
            modifier = context.modifier
        )
    }
}

// =========================================================================
// TRIGGER & BUMPER VARIANTS (LT / RT / LB / RB)
// =========================================================================

object RealisticLeftTriggerVariant : BaseNativeVariant("builtin.default_lt", ControlKey.LT, "Realistic 3D", 2001) {
    override val isBaselineDefault: Boolean = true

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        RealisticTrigger(
            key = K.LT,
            isConnected = context.isConnected,
            onVibrate = context.onVibrate,
            viewModel = context.viewModel,
            isRgbEnabled = context.isRgbEnabled,
            displayLabel = context.displayLabel,
            modifier = context.modifier
        )
    }

    @Composable
    override fun RenderStaticPreview(context: NativePreviewContext) {
        StaticDefaultButtonPreview(
            controlKey = K.LT,
            labelStyle = context.labelStyle,
            modifier = context.modifier
        )
    }
}

object RealisticRightTriggerVariant : BaseNativeVariant("builtin.default_rt", ControlKey.RT, "Realistic 3D", 2002) {
    override val isBaselineDefault: Boolean = true

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        RealisticTrigger(
            key = K.RT,
            isConnected = context.isConnected,
            onVibrate = context.onVibrate,
            viewModel = context.viewModel,
            isRgbEnabled = context.isRgbEnabled,
            displayLabel = context.displayLabel,
            modifier = context.modifier
        )
    }

    @Composable
    override fun RenderStaticPreview(context: NativePreviewContext) {
        StaticDefaultButtonPreview(
            controlKey = K.RT,
            labelStyle = context.labelStyle,
            modifier = context.modifier
        )
    }
}

object RealisticLeftBumperVariant : BaseNativeVariant("builtin.default_lb", ControlKey.LB, "Realistic 3D", 3001) {
    override val isBaselineDefault: Boolean = true

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        RealisticBumper(
            key = K.LB,
            isConnected = context.isConnected,
            onVibrate = context.onVibrate,
            viewModel = context.viewModel,
            isRgbEnabled = context.isRgbEnabled,
            displayLabel = context.displayLabel,
            modifier = context.modifier
        )
    }

    @Composable
    override fun RenderStaticPreview(context: NativePreviewContext) {
        StaticDefaultButtonPreview(
            controlKey = K.LB,
            labelStyle = context.labelStyle,
            modifier = context.modifier
        )
    }
}

object RealisticRightBumperVariant : BaseNativeVariant("builtin.default_rb", ControlKey.RB, "Realistic 3D", 3002) {
    override val isBaselineDefault: Boolean = true

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        RealisticBumper(
            key = K.RB,
            isConnected = context.isConnected,
            onVibrate = context.onVibrate,
            viewModel = context.viewModel,
            isRgbEnabled = context.isRgbEnabled,
            displayLabel = context.displayLabel,
            modifier = context.modifier
        )
    }

    @Composable
    override fun RenderStaticPreview(context: NativePreviewContext) {
        StaticDefaultButtonPreview(
            controlKey = K.RB,
            labelStyle = context.labelStyle,
            modifier = context.modifier
        )
    }
}

// =========================================================================
// DPAD & TOUCHPAD & SYSTEM & MACRO VARIANTS
// =========================================================================

object RealisticDPadVariant : BaseNativeVariant("builtin.default_dpad", ControlKey.DPAD, "Realistic 3D", 4001) {
    override val isBaselineDefault: Boolean = true

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        RealisticDPad(
            isConnected = context.isConnected,
            viewModel = context.viewModel,
            isRgbEnabled = context.isRgbEnabled,
            onVibrate = context.onVibrate,
            modifier = context.modifier
        )
    }

    @Composable
    override fun RenderStaticPreview(context: NativePreviewContext) {
        StaticDefaultButtonPreview(
            controlKey = K.DPAD,
            labelStyle = context.labelStyle,
            modifier = context.modifier
        )
    }
}

class RealisticDPadButtonVariant(controlKey: ControlKey, seedCode: Int) :
    BaseNativeVariant("builtin.default_${controlKey.key.lowercase()}", controlKey, "Realistic 3D", seedCode) {
    override val isBaselineDefault: Boolean = true

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        RealisticDPadButton(
            direction = controlKey.key,
            isConnected = context.isConnected,
            onVibrate = context.onVibrate,
            viewModel = context.viewModel,
            isRgbEnabled = context.isRgbEnabled,
            modifier = context.modifier
        )
    }

    @Composable
    override fun RenderStaticPreview(context: NativePreviewContext) {
        StaticDefaultButtonPreview(
            controlKey = controlKey.key,
            labelStyle = context.labelStyle,
            modifier = context.modifier
        )
    }
}

object RealisticLeftTouchPadVariant : BaseNativeVariant("builtin.default_ltp", ControlKey.LTP, "Realistic 3D", 6001) {
    override val isBaselineDefault: Boolean = true

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        RealisticTouchPad(
            isLeft = true,
            isConnected = context.isConnected,
            viewModel = context.viewModel,
            onVibrate = context.onVibrate,
            isRgbEnabled = context.isRgbEnabled,
            sensitivity = context.sensitivity,
            modifier = context.modifier
        )
    }

    @Composable
    override fun RenderStaticPreview(context: NativePreviewContext) {
        StaticDefaultButtonPreview(
            controlKey = K.LTP,
            labelStyle = context.labelStyle,
            modifier = context.modifier
        )
    }
}

object RealisticRightTouchPadVariant : BaseNativeVariant("builtin.default_rtp", ControlKey.RTP, "Realistic 3D", 6002) {
    override val isBaselineDefault: Boolean = true

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        RealisticTouchPad(
            isLeft = false,
            isConnected = context.isConnected,
            viewModel = context.viewModel,
            onVibrate = context.onVibrate,
            isRgbEnabled = context.isRgbEnabled,
            sensitivity = context.sensitivity,
            modifier = context.modifier
        )
    }

    @Composable
    override fun RenderStaticPreview(context: NativePreviewContext) {
        StaticDefaultButtonPreview(
            controlKey = K.RTP,
            labelStyle = context.labelStyle,
            modifier = context.modifier
        )
    }
}

class RealisticSystemButtonVariant(controlKey: ControlKey, seedCode: Int) :
    BaseNativeVariant("builtin.default_${controlKey.key.lowercase()}", controlKey, "Realistic 3D", seedCode) {
    override val isBaselineDefault: Boolean = true

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        RealisticSystemButton(
            key = controlKey.key,
            isConnected = context.isConnected,
            onVibrate = context.onVibrate,
            viewModel = context.viewModel,
            isRgbEnabled = context.isRgbEnabled,
            modifier = context.modifier
        )
    }

    @Composable
    override fun RenderStaticPreview(context: NativePreviewContext) {
        StaticDefaultButtonPreview(
            controlKey = controlKey.key,
            labelStyle = context.labelStyle,
            modifier = context.modifier
        )
    }
}

class RealisticMacroButtonVariant(controlKey: ControlKey, seedCode: Int) :
    BaseNativeVariant("builtin.default_${controlKey.key.lowercase()}", controlKey, "Realistic 3D", seedCode) {
    override val isBaselineDefault: Boolean = true

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        RealisticMacroButton(
            key = controlKey.key,
            isConnected = context.isConnected,
            onVibrate = context.onVibrate,
            viewModel = context.viewModel,
            isRgbEnabled = context.isRgbEnabled,
            modifier = context.modifier
        )
    }

    @Composable
    override fun RenderStaticPreview(context: NativePreviewContext) {
        StaticDefaultButtonPreview(
            controlKey = controlKey.key,
            labelStyle = context.labelStyle,
            modifier = context.modifier
        )
    }
}

// =========================================================================
// UNIFIED DEFAULT FAMILY (SINGLE SOURCE OF TRUTH FOR ALL DEFAULT COMPONENTS)
// =========================================================================

/**
 * Unified Default Native Family.
 * All built-in console-grade components (whether canonical hardware, Flux Cyber,
 * or upcoming designs referenced from the web) live together in this single family.
 *
 * To add an upcoming design (joystick, button, bumper, etc.):
 * Simply implement [NativeElementVariant] (or subclass [BaseNativeVariant]), assign a seed number,
 * and call [DefaultNativeFamily.register]. No other file changes required.
 */
object DefaultNativeFamily {
    val familyName: String = "Default Native"
    val familyId: String = "builtin.default"

    private val variantsById = mutableMapOf<String, NativeElementVariant>()
    private val variantsByControl = mutableMapOf<ControlKey, MutableList<NativeElementVariant>>()

    init {
        // --- 1. Baseline Hardware Variants ---
        register(RealisticLeftJoystickVariant)
        register(RealisticRightJoystickVariant)
        register(RealisticLeftStickButtonVariant)
        register(RealisticRightStickButtonVariant)
        register(RealisticLeftTouchPadVariant)
        register(RealisticRightTouchPadVariant)
        register(RealisticDPadVariant)
        register(RealisticDPadButtonVariant(ControlKey.UP, 4002))
        register(RealisticDPadButtonVariant(ControlKey.DOWN, 4003))
        register(RealisticDPadButtonVariant(ControlKey.LEFT, 4004))
        register(RealisticDPadButtonVariant(ControlKey.RIGHT, 4005))
        register(RealisticLeftTriggerVariant)
        register(RealisticRightTriggerVariant)
        register(RealisticLeftBumperVariant)
        register(RealisticRightBumperVariant)
        register(RealisticButtonVariant(ControlKey.A, Color(0xFF3FD25A), 1001))
        register(RealisticButtonVariant(ControlKey.B, Color(0xFFE6474E), 1002))
        register(RealisticButtonVariant(ControlKey.X, Color(0xFF3F8FE0), 1003))
        register(RealisticButtonVariant(ControlKey.Y, Color(0xFFE0A03F), 1004))
        register(RealisticSystemButtonVariant(ControlKey.GUIDE, 5001))
        register(RealisticSystemButtonVariant(ControlKey.START, 5002))
        register(RealisticSystemButtonVariant(ControlKey.BACK, 5003))
        register(RealisticSystemButtonVariant(ControlKey.SHARE, 5004))
        register(RealisticMacroButtonVariant(ControlKey.M1, 7001))
        register(RealisticMacroButtonVariant(ControlKey.M2, 7002))
        register(RealisticMacroButtonVariant(ControlKey.M3, 7003))
        register(RealisticMacroButtonVariant(ControlKey.M4, 7004))

        // --- 2. Flux Cyber Variants (Analog Sticks & Stick Click Buttons) ---
        register(FluxLeftJoystickVariant)
        register(FluxRightJoystickVariant)
        register(FluxLeftStickButtonVariant)
        register(FluxRightStickButtonVariant)

        // --- 3. Orb Glass Variants (Liquid Core Analog Sticks & Stick Click Buttons) ---
        register(OrbLeftJoystickVariant)
        register(OrbRightJoystickVariant)
        register(OrbLeftStickButtonVariant)
        register(OrbRightStickButtonVariant)

        // --- 4. Compass Nav Variants (Directional Pips & Pointer Analog Sticks & Stick Click Buttons) ---
        register(CompassLeftJoystickVariant)
        register(CompassRightJoystickVariant)
        register(CompassLeftStickButtonVariant)
        register(CompassRightStickButtonVariant)
    }

    /**
     * Registers a new native variant into the unified Default Family.
     */
    fun register(variant: NativeElementVariant) {
        variantsById[variant.id] = variant
        variantsByControl.getOrPut(variant.controlKey) { mutableListOf() }.apply {
            if (!contains(variant)) add(variant)
        }
    }

    /**
     * Retrieves a variant by its unique component ID.
     */
    fun getVariant(id: String?): NativeElementVariant? =
        if (id != null) variantsById[id] else null

    /**
     * Retrieves the baseline default variant for a specific control key.
     */
    fun getDefaultVariant(controlKey: ControlKey): NativeElementVariant? =
        variantsByControl[controlKey]?.firstOrNull { it.isBaselineDefault }
            ?: variantsByControl[controlKey]?.firstOrNull()

    /**
     * Returns all variants belonging to a specific control key (e.g. all available joysticks).
     */
    fun getVariantsFor(controlKey: ControlKey): List<NativeElementVariant> =
        variantsByControl[controlKey] ?: emptyList()

    /**
     * Returns all registered native variants.
     */
    fun getAllVariants(): List<NativeElementVariant> = variantsById.values.toList()
}

// =========================================================================
// CENTRAL REGISTRY SINGLETON
// =========================================================================

/**
 * Scalable, industry-standard OOP registry for native Compose controller components.
 * Adheres to the Open-Closed Principle: upcoming variants referenced from web/mockups
 * are simply registered into [DefaultNativeFamily] without modifying any call sites.
 */
object NativeComponentRegistry {

    /**
     * Checks if a component ID corresponds to any registered native component in the Default Family.
     * Null or empty IDs default to the built-in realistic style.
     */
    fun isNativeBuiltin(id: String?): Boolean {
        if (id.isNullOrBlank()) return true
        if (id.startsWith("builtin.default_") || id.startsWith("builtin.flux_") || id.startsWith("builtin.orb_")) return true
        return DefaultNativeFamily.getVariant(id) != null
    }

    /**
     * Resolves the active variant for a given control key and optional custom component ID.
     * If the ID is null, blank, or not found, it gracefully resolves to the baseline default variant.
     */
    fun resolveVariant(key: String, customComponentId: String?): NativeElementVariant? {
        val ctrl = ControlKey.fromIdentifier(key) ?: return null
        if (!customComponentId.isNullOrBlank()) {
            val found = DefaultNativeFamily.getVariant(customComponentId)
            if (found != null) return found
        }
        return DefaultNativeFamily.getDefaultVariant(ctrl)
    }

    /**
     * Centralized interactive native element renderer for ControllerElementRenderer and SandboxPreviewModal.
     */
    @Composable
    fun RenderNativeElement(
        key: String,
        customComponentId: String?,
        isConnected: Boolean,
        isRgbEnabled: Boolean,
        viewModel: GamepadViewModel,
        onVibrate: () -> Unit = {},
        sensitivity: Float? = null,
        labelStyle: ControllerLabelStyle = ControllerLabelStyle.XBOX,
        modifier: Modifier = Modifier
    ) {
        val ctrl = ControlKey.fromIdentifier(key) ?: ControlKey.A
        val variant = resolveVariant(key, customComponentId)
        val context = NativeRenderContext(
            controlKey = ctrl,
            key = key,
            isConnected = isConnected,
            isRgbEnabled = isRgbEnabled,
            viewModel = viewModel,
            onVibrate = onVibrate,
            sensitivity = sensitivity,
            labelStyle = labelStyle,
            modifier = modifier
        )

        if (variant != null) {
            variant.RenderInteractive(context)
        } else {
            RenderCategoryFallback(context)
        }
    }

    /**
     * Centralized static preview renderer for StudioGridCard and ButtonStudioScreen.
     */
    @Composable
    fun RenderStaticPreview(
        id: String?,
        controlKey: String,
        labelStyle: ControllerLabelStyle = ControllerLabelStyle.XBOX,
        modifier: Modifier = Modifier
    ) {
        val ctrl = ControlKey.fromIdentifier(controlKey) ?: ControlKey.A
        val variant = resolveVariant(controlKey, id)
        val context = NativePreviewContext(
            controlKey = ctrl,
            key = controlKey,
            labelStyle = labelStyle,
            modifier = modifier
        )

        if (variant != null) {
            variant.RenderStaticPreview(context)
        } else {
            StaticDefaultButtonPreview(
                controlKey = controlKey,
                labelStyle = labelStyle,
                modifier = modifier
            )
        }
    }

    @Composable
    private fun RenderCategoryFallback(context: NativeRenderContext) {
        when (context.controlKey.categoryType) {
            CategoryType.SYSTEM -> RealisticSystemButton(
                key = context.key,
                isConnected = context.isConnected,
                onVibrate = context.onVibrate,
                viewModel = context.viewModel,
                isRgbEnabled = context.isRgbEnabled,
                modifier = context.modifier
            )
            CategoryType.MACROS -> RealisticMacroButton(
                key = context.key,
                isConnected = context.isConnected,
                onVibrate = context.onVibrate,
                viewModel = context.viewModel,
                isRgbEnabled = context.isRgbEnabled,
                modifier = context.modifier
            )
            else -> RealisticButton(
                key = context.key,
                buttonColor = Color.Gray,
                isConnected = context.isConnected,
                onVibrate = context.onVibrate,
                viewModel = context.viewModel,
                isRgbEnabled = context.isRgbEnabled,
                displayLabel = context.displayLabel,
                modifier = context.modifier
            )
        }
    }
}
