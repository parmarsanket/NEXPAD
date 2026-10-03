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
    val heightScale: Float = 1.0f,
    val isFlipped: Boolean = false,
    val isLocked: Boolean = true,
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
    val isRgbEnabled: Boolean = true,
    val modifier: Modifier = Modifier
) {
    val displayLabel: String get() = CategoryManager.getLabelForStyle(key, labelStyle)
}

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
            isLocked = context.isLocked,
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
            isLocked = context.isLocked,
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
            isLocked = context.isLocked,
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
            isLocked = context.isLocked,
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
            isLocked = context.isLocked,
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
            isLocked = context.isLocked,
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
            isLocked = context.isLocked,
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
            isLocked = context.isLocked,
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

object GyroLeftJoystickVariant : BaseNativeVariant("builtin.gyro_ls", ControlKey.LS, "Gyro Gimbal", 105) {
    override val isBaselineDefault: Boolean = false

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        GyroJoystick(
            isLeft = true,
            isConnected = context.isConnected,
            viewModel = context.viewModel,
            isRgbEnabled = context.isRgbEnabled,
            isLocked = context.isLocked,
            modifier = context.modifier
        )
    }

    @Composable
    override fun RenderStaticPreview(context: NativePreviewContext) {
        StaticGyroJoystick(
            isLeft = true,
            modifier = context.modifier
        )
    }
}

object GyroRightJoystickVariant : BaseNativeVariant("builtin.gyro_rs", ControlKey.RS, "Gyro Gimbal", 205) {
    override val isBaselineDefault: Boolean = false

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        GyroJoystick(
            isLeft = false,
            isConnected = context.isConnected,
            viewModel = context.viewModel,
            isRgbEnabled = context.isRgbEnabled,
            isLocked = context.isLocked,
            modifier = context.modifier
        )
    }

    @Composable
    override fun RenderStaticPreview(context: NativePreviewContext) {
        StaticGyroJoystick(
            isLeft = false,
            modifier = context.modifier
        )
    }
}

object SpotlightLeftJoystickVariant : BaseNativeVariant("builtin.spotlight_ls", ControlKey.LS, "Spotlight", 106) {
    override val isBaselineDefault: Boolean = false

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        SpotlightJoystick(
            isLeft = true,
            isConnected = context.isConnected,
            viewModel = context.viewModel,
            isRgbEnabled = context.isRgbEnabled,
            isLocked = context.isLocked,
            modifier = context.modifier
        )
    }

    @Composable
    override fun RenderStaticPreview(context: NativePreviewContext) {
        StaticSpotlightJoystick(
            isLeft = true,
            modifier = context.modifier
        )
    }
}

object SpotlightRightJoystickVariant : BaseNativeVariant("builtin.spotlight_rs", ControlKey.RS, "Spotlight", 206) {
    override val isBaselineDefault: Boolean = false

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        SpotlightJoystick(
            isLeft = false,
            isConnected = context.isConnected,
            viewModel = context.viewModel,
            isRgbEnabled = context.isRgbEnabled,
            isLocked = context.isLocked,
            modifier = context.modifier
        )
    }

    @Composable
    override fun RenderStaticPreview(context: NativePreviewContext) {
        StaticSpotlightJoystick(
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
            label = context.displayLabel,
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
            label = context.displayLabel,
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
            label = context.displayLabel,
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
            label = context.displayLabel,
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
            label = context.displayLabel,
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
            label = context.displayLabel,
            modifier = context.modifier
        )
    }
}

object GyroLeftStickButtonVariant : BaseNativeVariant("builtin.gyro_lsb", ControlKey.LSB, "Gyro Gimbal", 305) {
    override val isBaselineDefault: Boolean = false

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        GyroStickButton(
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
        StaticGyroStickButton(
            isLeft = true,
            label = context.displayLabel,
            modifier = context.modifier
        )
    }
}

object GyroRightStickButtonVariant : BaseNativeVariant("builtin.gyro_rsb", ControlKey.RSB, "Gyro Gimbal", 405) {
    override val isBaselineDefault: Boolean = false

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        GyroStickButton(
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
        StaticGyroStickButton(
            isLeft = false,
            label = context.displayLabel,
            modifier = context.modifier
        )
    }
}

object SpotlightLeftStickButtonVariant : BaseNativeVariant("builtin.spotlight_lsb", ControlKey.LSB, "Spotlight", 306) {
    override val isBaselineDefault: Boolean = false

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        SpotlightStickButton(
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
        StaticSpotlightStickButton(
            isLeft = true,
            label = context.displayLabel,
            modifier = context.modifier
        )
    }
}

object SpotlightRightStickButtonVariant : BaseNativeVariant("builtin.spotlight_rsb", ControlKey.RSB, "Spotlight", 406) {
    override val isBaselineDefault: Boolean = false

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        SpotlightStickButton(
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
        StaticSpotlightStickButton(
            isLeft = false,
            label = context.displayLabel,
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

class LiquidButtonVariant(
    controlKey: ControlKey,
    private val buttonColor: Color,
    seedCode: Int
) : BaseNativeVariant("builtin.liq_${controlKey.key.lowercase()}", controlKey, "Liquid Fill", seedCode) {
    override val isBaselineDefault: Boolean = false
    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        LiquidButton(
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
        StaticLiquidButton(
            controlKey = controlKey.key,
            labelStyle = context.labelStyle,
            modifier = context.modifier
        )
    }
}

class FacetButtonVariant(
    controlKey: ControlKey,
    private val buttonColor: Color,
    seedCode: Int
) : BaseNativeVariant("builtin.facet_${controlKey.key.lowercase()}", controlKey, "Facet Gem", seedCode) {
    override val isBaselineDefault: Boolean = false
    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        FacetButton(
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
        StaticFacetButton(
            controlKey = controlKey.key,
            labelStyle = context.labelStyle,
            modifier = context.modifier
        )
    }
}

class FlipButtonVariant(
    controlKey: ControlKey,
    private val buttonColor: Color,
    seedCode: Int
) : BaseNativeVariant("builtin.flipbtn_${controlKey.key.lowercase()}", controlKey, "Flip Card", seedCode) {
    override val isBaselineDefault: Boolean = false
    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        FlipButton(
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
        StaticFlipButton(
            controlKey = controlKey.key,
            labelStyle = context.labelStyle,
            modifier = context.modifier
        )
    }
}

class RippleButtonVariant(
    controlKey: ControlKey,
    private val buttonColor: Color,
    seedCode: Int
) : BaseNativeVariant("builtin.ripple_${controlKey.key.lowercase()}", controlKey, "Ripple Rings", seedCode) {
    override val isBaselineDefault: Boolean = false
    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        RippleButton(
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
        StaticRippleButton(
            controlKey = controlKey.key,
            labelStyle = context.labelStyle,
            modifier = context.modifier
        )
    }
}


class OrbitButtonVariant(
    controlKey: ControlKey,
    private val buttonColor: Color,
    seedCode: Int
) : BaseNativeVariant("builtin.orbit_${controlKey.key.lowercase()}", controlKey, "Orbit Rings", seedCode) {
    override val isBaselineDefault: Boolean = false
    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        OrbitButton(
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
        StaticOrbitButton(
            controlKey = controlKey.key,
            labelStyle = context.labelStyle,
            modifier = context.modifier
        )
    }
}

class CapsulesButtonVariant(
    controlKey: ControlKey,
    private val buttonColor: Color,
    seedCode: Int
) : BaseNativeVariant("builtin.caps_${controlKey.key.lowercase()}", controlKey, "Capsule Fill", seedCode) {
    override val isBaselineDefault: Boolean = false
    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        CapsulesButton(
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
        StaticCapsulesButton(
            controlKey = controlKey.key,
            labelStyle = context.labelStyle,
            modifier = context.modifier
        )
    }
}

class EclipseButtonVariant(
    controlKey: ControlKey,
    private val buttonColor: Color,
    seedCode: Int
) : BaseNativeVariant("builtin.ecl_${controlKey.key.lowercase()}", controlKey, "Eclipse Disc", seedCode) {
    override val isBaselineDefault: Boolean = false
    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        EclipseButton(
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
        StaticEclipseButton(
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

object ArcLeftBumperVariant : BaseNativeVariant("builtin.arc_lb", ControlKey.LB, "Arc Bumper", 3101) {
    override val isBaselineDefault: Boolean = false

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        ArcBumper(
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
        StaticArcBumper(
            key = context.displayLabel,
            modifier = context.modifier
        )
    }
}

object ArcRightBumperVariant : BaseNativeVariant("builtin.arc_rb", ControlKey.RB, "Arc Bumper", 3102) {
    override val isBaselineDefault: Boolean = false

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        ArcBumper(
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
        StaticArcBumper(
            key = context.displayLabel,
            modifier = context.modifier
        )
    }
}

object LedLeftBumperVariant : BaseNativeVariant("builtin.led_lb", ControlKey.LB, "LED Bar Bumper", 3201) {
    override val isBaselineDefault: Boolean = false

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        LedBumper(
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
        StaticLedBumper(
            key = context.displayLabel,
            modifier = context.modifier
        )
    }
}

object LedRightBumperVariant : BaseNativeVariant("builtin.led_rb", ControlKey.RB, "LED Bar Bumper", 3202) {
    override val isBaselineDefault: Boolean = false

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        LedBumper(
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
        StaticLedBumper(
            key = context.displayLabel,
            modifier = context.modifier
        )
    }
}

object PeekLeftBumperVariant : BaseNativeVariant("builtin.peek_lb", ControlKey.LB, "Peek Bumper", 3301) {
    override val isBaselineDefault: Boolean = false

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        PeekBumper(
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
        StaticPeekBumper(
            key = context.displayLabel,
            modifier = context.modifier
        )
    }
}

object PeekRightBumperVariant : BaseNativeVariant("builtin.peek_rb", ControlKey.RB, "Peek Bumper", 3302) {
    override val isBaselineDefault: Boolean = false

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        PeekBumper(
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
        StaticPeekBumper(
            key = context.displayLabel,
            modifier = context.modifier
        )
    }
}

object RibbedLeftBumperVariant : BaseNativeVariant("builtin.rib_lb", ControlKey.LB, "Ribbed Bumper", 3401) {
    override val isBaselineDefault: Boolean = false

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        RibbedBumper(
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
        StaticRibbedBumper(
            key = context.displayLabel,
            modifier = context.modifier
        )
    }
}

object RibbedRightBumperVariant : BaseNativeVariant("builtin.rib_rb", ControlKey.RB, "Ribbed Bumper", 3402) {
    override val isBaselineDefault: Boolean = false

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        RibbedBumper(
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
        StaticRibbedBumper(
            key = context.displayLabel,
            modifier = context.modifier
        )
    }
}

object UnderglowLeftBumperVariant : BaseNativeVariant("builtin.under_lb", ControlKey.LB, "Underglow Bumper", 3501) {
    override val isBaselineDefault: Boolean = false

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        UnderglowBumper(
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
        StaticUnderglowBumper(
            key = context.displayLabel,
            modifier = context.modifier
        )
    }
}

object UnderglowRightBumperVariant : BaseNativeVariant("builtin.under_rb", ControlKey.RB, "Underglow Bumper", 3502) {
    override val isBaselineDefault: Boolean = false

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        UnderglowBumper(
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
        StaticUnderglowBumper(
            key = context.displayLabel,
            modifier = context.modifier
        )
    }
}

object TubeLeftBumperVariant : BaseNativeVariant("builtin.tube_lb", ControlKey.LB, "Tube Bumper", 3601) {
    override val isBaselineDefault: Boolean = false

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        TubeBumper(
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
        StaticTubeBumper(
            key = context.displayLabel,
            modifier = context.modifier
        )
    }
}

object TubeRightBumperVariant : BaseNativeVariant("builtin.tube_rb", ControlKey.RB, "Tube Bumper", 3602) {
    override val isBaselineDefault: Boolean = false

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        TubeBumper(
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
        StaticTubeBumper(
            key = context.displayLabel,
            modifier = context.modifier
        )
    }
}

object FlipLeftBumperVariant : BaseNativeVariant("builtin.flip_lb", ControlKey.LB, "Flip Bumper", 3701) {
    override val isBaselineDefault: Boolean = false

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        FlipBumper(
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
        StaticFlipBumper(
            key = context.displayLabel,
            modifier = context.modifier
        )
    }
}

object FlipRightBumperVariant : BaseNativeVariant("builtin.flip_rb", ControlKey.RB, "Flip Bumper", 3702) {
    override val isBaselineDefault: Boolean = false

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        FlipBumper(
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
        StaticFlipBumper(
            key = context.displayLabel,
            modifier = context.modifier
        )
    }
}

// =========================================================================
// DIAL TRIGGER VARIANTS (LT / RT)
// =========================================================================

object DialLeftTriggerVariant : BaseNativeVariant("builtin.dial_lt", ControlKey.LT, "Dial Gauge", 2101) {
    override val isBaselineDefault: Boolean = false

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        DialTrigger(
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
        StaticDialTrigger(
            key = context.displayLabel.ifBlank { "LT" },
            modifier = context.modifier
        )
    }
}

object DialRightTriggerVariant : BaseNativeVariant("builtin.dial_rt", ControlKey.RT, "Dial Gauge", 2102) {
    override val isBaselineDefault: Boolean = false

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        DialTrigger(
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
        StaticDialTrigger(
            key = context.displayLabel.ifBlank { "RT" },
            modifier = context.modifier
        )
    }
}

// =========================================================================
// LIQUID ORB TRIGGER VARIANTS (LT / RT)
// =========================================================================

object LiquidOrbLeftTriggerVariant : BaseNativeVariant("builtin.liquid_lt", ControlKey.LT, "Liquid Orb", 2201) {
    override val isBaselineDefault: Boolean = false

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        LiquidOrbTrigger(
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
        StaticLiquidOrbTrigger(
            key = context.displayLabel.ifBlank { "LT" },
            modifier = context.modifier
        )
    }
}

object LiquidOrbRightTriggerVariant : BaseNativeVariant("builtin.liquid_rt", ControlKey.RT, "Liquid Orb", 2202) {
    override val isBaselineDefault: Boolean = false

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        LiquidOrbTrigger(
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
        StaticLiquidOrbTrigger(
            key = context.displayLabel.ifBlank { "RT" },
            modifier = context.modifier
        )
    }
}

// =========================================================================
// VU SLABS TRIGGER VARIANTS (LT / RT)
// =========================================================================

object VuSlabsLeftTriggerVariant : BaseNativeVariant("builtin.vu_lt", ControlKey.LT, "VU Slabs", 2301) {
    override val isBaselineDefault: Boolean = false

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        VuSlabsTrigger(
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
        StaticVuSlabsTrigger(
            key = context.displayLabel.ifBlank { "LT" },
            modifier = context.modifier
        )
    }
}

object VuSlabsRightTriggerVariant : BaseNativeVariant("builtin.vu_rt", ControlKey.RT, "VU Slabs", 2302) {
    override val isBaselineDefault: Boolean = false

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        VuSlabsTrigger(
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
        StaticVuSlabsTrigger(
            key = context.displayLabel.ifBlank { "RT" },
            modifier = context.modifier
        )
    }
}

// =========================================================================
// TARGET TRIGGER VARIANTS (LT / RT)
// =========================================================================

object TargetLeftTriggerVariant : BaseNativeVariant("builtin.target_lt", ControlKey.LT, "Target Crosshair", 2401) {
    override val isBaselineDefault: Boolean = false

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        TargetTrigger(
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
        StaticTargetTrigger(
            key = context.displayLabel.ifBlank { "LT" },
            modifier = context.modifier
        )
    }
}

object TargetRightTriggerVariant : BaseNativeVariant("builtin.target_rt", ControlKey.RT, "Target Crosshair", 2402) {
    override val isBaselineDefault: Boolean = false

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        TargetTrigger(
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
        StaticTargetTrigger(
            key = context.displayLabel.ifBlank { "RT" },
            modifier = context.modifier
        )
    }
}

// =========================================================================
// SLIDER TRIGGER VARIANTS (LT / RT)
// =========================================================================

object SliderLeftTriggerVariant : BaseNativeVariant("builtin.slider_lt", ControlKey.LT, "Analog Slider", 2501) {
    override val isBaselineDefault: Boolean = false

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        SliderTrigger(
            key = K.LT,
            isConnected = context.isConnected,
            onVibrate = context.onVibrate,
            viewModel = context.viewModel,
            isRgbEnabled = context.isRgbEnabled,
            displayLabel = context.displayLabel,
            heightScale = context.heightScale,
            isFlipped = context.isFlipped,
            modifier = context.modifier
        )
    }

    @Composable
    override fun RenderStaticPreview(context: NativePreviewContext) {
        StaticSliderTrigger(
            key = context.displayLabel.ifBlank { "LT" },
            modifier = context.modifier
        )
    }
}

object SliderRightTriggerVariant : BaseNativeVariant("builtin.slider_rt", ControlKey.RT, "Analog Slider", 2502) {
    override val isBaselineDefault: Boolean = false

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        SliderTrigger(
            key = K.RT,
            isConnected = context.isConnected,
            onVibrate = context.onVibrate,
            viewModel = context.viewModel,
            isRgbEnabled = context.isRgbEnabled,
            displayLabel = context.displayLabel,
            heightScale = context.heightScale,
            isFlipped = context.isFlipped,
            modifier = context.modifier
        )
    }

    @Composable
    override fun RenderStaticPreview(context: NativePreviewContext) {
        StaticSliderTrigger(
            key = context.displayLabel.ifBlank { "RT" },
            modifier = context.modifier
        )
    }
}

object NeedleLeftTriggerVariant : BaseNativeVariant("builtin.needle_lt", ControlKey.LT, "Needle Meter", 2601) {
    override val isBaselineDefault: Boolean = false

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        NeedleTrigger(
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
        StaticNeedleTrigger(
            key = context.displayLabel.ifBlank { "LT" },
            modifier = context.modifier
        )
    }
}

object NeedleRightTriggerVariant : BaseNativeVariant("builtin.needle_rt", ControlKey.RT, "Needle Meter", 2602) {
    override val isBaselineDefault: Boolean = false

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        NeedleTrigger(
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
        StaticNeedleTrigger(
            key = context.displayLabel.ifBlank { "RT" },
            modifier = context.modifier
        )
    }
}

object TestTubeLeftTriggerVariant : BaseNativeVariant("builtin.testtube_lt", ControlKey.LT, "Test Tube", 2701) {
    override val isBaselineDefault: Boolean = false

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        TestTubeTrigger(
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
        StaticTestTubeTrigger(
            key = context.displayLabel.ifBlank { "LT" },
            modifier = context.modifier
        )
    }
}

object TestTubeRightTriggerVariant : BaseNativeVariant("builtin.testtube_rt", ControlKey.RT, "Test Tube", 2702) {
    override val isBaselineDefault: Boolean = false

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        TestTubeTrigger(
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
        StaticTestTubeTrigger(
            key = context.displayLabel.ifBlank { "RT" },
            modifier = context.modifier
        )
    }
}

object BloomLeftTriggerVariant : BaseNativeVariant("builtin.bloom_lt", ControlKey.LT, "Bloom Light", 2801) {
    override val isBaselineDefault: Boolean = false

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        BloomTrigger(
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
        StaticBloomTrigger(
            key = context.displayLabel.ifBlank { "LT" },
            modifier = context.modifier
        )
    }
}

object BloomRightTriggerVariant : BaseNativeVariant("builtin.bloom_rt", ControlKey.RT, "Bloom Light", 2802) {
    override val isBaselineDefault: Boolean = false

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        BloomTrigger(
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
        StaticBloomTrigger(
            key = context.displayLabel.ifBlank { "RT" },
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

object LensDPadVariant : BaseNativeVariant("builtin.lens_dpad", ControlKey.DPAD, "Lens Cross", 4101) {
    override val isBaselineDefault: Boolean = false

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        LensDPad(
            isConnected = context.isConnected,
            viewModel = context.viewModel,
            isRgbEnabled = context.isRgbEnabled,
            onVibrate = context.onVibrate,
            modifier = context.modifier
        )
    }

    @Composable
    override fun RenderStaticPreview(context: NativePreviewContext) {
        StaticLensDPad(
            isRgbEnabled = context.isRgbEnabled,
            modifier = context.modifier
        )
    }
}

object FourLensesDPadVariant : BaseNativeVariant("builtin.four_lenses_dpad", ControlKey.DPAD, "Four Lenses", 4102) {
    override val isBaselineDefault: Boolean = false

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        FourLensesDPad(
            isConnected = context.isConnected,
            viewModel = context.viewModel,
            isRgbEnabled = context.isRgbEnabled,
            onVibrate = context.onVibrate,
            modifier = context.modifier
        )
    }

    @Composable
    override fun RenderStaticPreview(context: NativePreviewContext) {
        StaticFourLensesDPad(
            isRgbEnabled = context.isRgbEnabled,
            modifier = context.modifier
        )
    }
}

object DiscDPadVariant : BaseNativeVariant("builtin.disc_dpad", ControlKey.DPAD, "Lens Disc", 4103) {
    override val isBaselineDefault: Boolean = false

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        DiscDPad(
            isConnected = context.isConnected,
            viewModel = context.viewModel,
            isRgbEnabled = context.isRgbEnabled,
            onVibrate = context.onVibrate,
            modifier = context.modifier
        )
    }

    @Composable
    override fun RenderStaticPreview(context: NativePreviewContext) {
        StaticDiscDPad(
            modifier = context.modifier
        )
    }
}

object CapsulesDPadVariant : BaseNativeVariant("builtin.capsules_dpad", ControlKey.DPAD, "Lens Capsules", 4104) {
    override val isBaselineDefault: Boolean = false

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        CapsulesDPad(
            isConnected = context.isConnected,
            viewModel = context.viewModel,
            isRgbEnabled = context.isRgbEnabled,
            onVibrate = context.onVibrate,
            modifier = context.modifier
        )
    }

    @Composable
    override fun RenderStaticPreview(context: NativePreviewContext) {
        StaticCapsulesDPad(
            modifier = context.modifier
        )
    }
}

object MetaballsDPadVariant : BaseNativeVariant("builtin.metaballs_dpad", ControlKey.DPAD, "Lens Metaballs", 4105) {
    override val isBaselineDefault: Boolean = false

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        MetaballsDPad(
            isConnected = context.isConnected,
            viewModel = context.viewModel,
            isRgbEnabled = context.isRgbEnabled,
            onVibrate = context.onVibrate,
            modifier = context.modifier
        )
    }

    @Composable
    override fun RenderStaticPreview(context: NativePreviewContext) {
        StaticMetaballsDPad(
            modifier = context.modifier
        )
    }
}

object RailsDPadVariant : BaseNativeVariant("builtin.rails_dpad", ControlKey.DPAD, "Lens Rails", 4106) {
    override val isBaselineDefault: Boolean = false

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        RailsDPad(
            isConnected = context.isConnected,
            viewModel = context.viewModel,
            isRgbEnabled = context.isRgbEnabled,
            onVibrate = context.onVibrate,
            modifier = context.modifier
        )
    }

    @Composable
    override fun RenderStaticPreview(context: NativePreviewContext) {
        StaticRailsDPad(
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
        StaticRealisticTouchPad(
            isLeft = true,
            isRgbEnabled = context.isRgbEnabled,
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
        StaticRealisticTouchPad(
            isLeft = false,
            isRgbEnabled = context.isRgbEnabled,
            modifier = context.modifier
        )
    }
}

object InbuildLeftTouchPadVariant : BaseNativeVariant("builtin.inbuild_ltp", ControlKey.LTP, "Inbuild Surface", 6003) {
    override val isBaselineDefault: Boolean = false

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        InbuildTouchpad(
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
        StaticInbuildTouchpad(
            isLeft = true,
            isRgbEnabled = context.isRgbEnabled,
            modifier = context.modifier
        )
    }
}

object InbuildRightTouchPadVariant : BaseNativeVariant("builtin.inbuild_rtp", ControlKey.RTP, "Inbuild Surface", 6004) {
    override val isBaselineDefault: Boolean = false

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        InbuildTouchpad(
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
        StaticInbuildTouchpad(
            isLeft = false,
            isRgbEnabled = context.isRgbEnabled,
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

object OrbitHomeVariant : BaseNativeVariant("builtin.orbit_guide", ControlKey.GUIDE, "Orbit Home", 5101) {
    override val isBaselineDefault: Boolean = false

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        OrbitHomeButton(
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
        StaticOrbitHomeButton(
            modifier = context.modifier,
            isRgbEnabled = true
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
        register(InbuildLeftTouchPadVariant)
        register(InbuildRightTouchPadVariant)
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
        register(LiquidButtonVariant(ControlKey.A, Color(0xFF3FD25A), 1101))
        register(LiquidButtonVariant(ControlKey.B, Color(0xFFE6474E), 1102))
        register(LiquidButtonVariant(ControlKey.X, Color(0xFF3F8FE0), 1103))
        register(LiquidButtonVariant(ControlKey.Y, Color(0xFFE0A03F), 1104))
        register(FacetButtonVariant(ControlKey.A, Color(0xFF3FD25A), 1201))
        register(FacetButtonVariant(ControlKey.B, Color(0xFFE6474E), 1202))
        register(FacetButtonVariant(ControlKey.X, Color(0xFF3F8FE0), 1203))
        register(FacetButtonVariant(ControlKey.Y, Color(0xFFE0A03F), 1204))
        register(FlipButtonVariant(ControlKey.A, Color(0xFF3FD25A), 1301))
        register(FlipButtonVariant(ControlKey.B, Color(0xFFE6474E), 1302))
        register(FlipButtonVariant(ControlKey.X, Color(0xFF3F8FE0), 1303))
        register(FlipButtonVariant(ControlKey.Y, Color(0xFFE0A03F), 1304))
        register(RippleButtonVariant(ControlKey.A, Color(0xFF3FD25A), 1401))
        register(RippleButtonVariant(ControlKey.B, Color(0xFFE6474E), 1402))
        register(RippleButtonVariant(ControlKey.X, Color(0xFF3F8FE0), 1403))
        register(RippleButtonVariant(ControlKey.Y, Color(0xFFE0A03F), 1404))
        register(OrbitButtonVariant(ControlKey.A, Color(0xFF3FD25A), 1601))
        register(OrbitButtonVariant(ControlKey.B, Color(0xFFE6474E), 1602))
        register(OrbitButtonVariant(ControlKey.X, Color(0xFF3F8FE0), 1603))
        register(OrbitButtonVariant(ControlKey.Y, Color(0xFFE0A03F), 1604))
        register(CapsulesButtonVariant(ControlKey.A, Color(0xFF3FD25A), 1701))
        register(CapsulesButtonVariant(ControlKey.B, Color(0xFFE6474E), 1702))
        register(CapsulesButtonVariant(ControlKey.X, Color(0xFF3F8FE0), 1703))
        register(CapsulesButtonVariant(ControlKey.Y, Color(0xFFE0A03F), 1704))
        register(EclipseButtonVariant(ControlKey.A, Color(0xFF3FD25A), 1801))
        register(EclipseButtonVariant(ControlKey.B, Color(0xFFE6474E), 1802))
        register(EclipseButtonVariant(ControlKey.X, Color(0xFF3F8FE0), 1803))
        register(EclipseButtonVariant(ControlKey.Y, Color(0xFFE0A03F), 1804))
        register(RealisticSystemButtonVariant(ControlKey.GUIDE, 5001))
        register(RealisticSystemButtonVariant(ControlKey.START, 5002))
        register(RealisticSystemButtonVariant(ControlKey.BACK, 5003))
        register(RealisticSystemButtonVariant(ControlKey.SHARE, 5004))
        register(OrbitHomeVariant)
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

        // --- 5. Gyro Gimbal Variants (3D Tilting Concentric Gimbal Rings & Center Puck) ---
        register(GyroLeftJoystickVariant)
        register(GyroRightJoystickVariant)
        register(GyroLeftStickButtonVariant)
        register(GyroRightStickButtonVariant)

        // --- 6. Spotlight Variants (Moving Light Pool & Hidden Floor Matrix Dots) ---
        register(SpotlightLeftJoystickVariant)
        register(SpotlightRightJoystickVariant)
        register(SpotlightLeftStickButtonVariant)
        register(SpotlightRightStickButtonVariant)

        // --- 7. Lens D-Pad Variant (Full Contoured Cross with 3D Rocker Tilt & Chevrons) ---
        register(LensDPadVariant)

        // --- 8. Four Lenses D-Pad Variant (Four Lenses Cluster with 3D Rocker Tilt & Hub) ---
        register(FourLensesDPadVariant)

        // --- 9. Disc D-Pad Variant (Concentric Grooves, Rocker Tilt, Sliding Puck & Gate) ---
        register(DiscDPadVariant)

        // --- 10. Capsules D-Pad Variant (Four Rounded Pill Capsules with Rocker Tilt & Hub) ---
        register(CapsulesDPadVariant)

        // --- 11. Metaballs D-Pad Variant (Organic Fluid Metaballs with Spring Retraction & Caps) ---
        register(MetaballsDPadVariant)

        // --- 12. Rails D-Pad Variant (Orthogonal Rails, Sliding Puck & Light Beams) ---
        register(RailsDPadVariant)

        // --- 13. Arc Bumper Variants (Curved Bridge Contour & Emissive Neon Halo) ---
        register(ArcLeftBumperVariant)
        register(ArcRightBumperVariant)

        // --- 14. LED Bar Bumper Variants (Asymmetrical Ergonomic Contour & 6-Segment LED Bar) ---
        register(LedLeftBumperVariant)
        register(LedRightBumperVariant)

        // --- 15. Peek Bumper Variants (Oversized Aperture Window & 1.18x Zoom Glyph) ---
        register(PeekLeftBumperVariant)
        register(PeekRightBumperVariant)

        // --- 16. Ribbed Bumper Variants (Tactile Ribbed Knurling & Lower Lightbar Strip) ---
        register(RibbedLeftBumperVariant)
        register(RibbedRightBumperVariant)

        // --- 17. Underglow Bumper Variants (Bottom Neon Ground Bar & Dynamic Surge Flood) ---
        register(UnderglowLeftBumperVariant)
        register(UnderglowRightBumperVariant)

        // --- 18. Tube Bumper Variants (Dynamic Horizontal Liquid Level Surge) ---
        register(TubeLeftBumperVariant)
        register(TubeRightBumperVariant)

        // --- 19. Flip Bumper Variants (3D Horizontal Card Flip Face A / Face B) ---
        register(FlipLeftBumperVariant)
        register(FlipRightBumperVariant)

        // --- 20. Dial Trigger Variants (270° Sweeping Radial Arc Gauge) ---
        register(DialLeftTriggerVariant)
        register(DialRightTriggerVariant)

        // --- 21. Liquid Orb Trigger Variants (Dynamic Rising Fluid & Swaying Meniscus) ---
        register(LiquidOrbLeftTriggerVariant)
        register(LiquidOrbRightTriggerVariant)

        // --- 22. VU Slabs Trigger Variants (7-Slab Audio VU Meter Physics) ---
        register(VuSlabsLeftTriggerVariant)
        register(VuSlabsRightTriggerVariant)

        // --- 23. Target Trigger Variants (3 Concentric Radar Rings Outside-In Ignition) ---
        register(TargetLeftTriggerVariant)
        register(TargetRightTriggerVariant)

        // --- 24. Slider Trigger Variants (Analog Continuous 0..255 Slider Physics) ---
        register(SliderLeftTriggerVariant)
        register(SliderRightTriggerVariant)

        // --- 25. Needle Meter Trigger Variants (Analog Needle Meter Physics) ---
        register(NeedleLeftTriggerVariant)
        register(NeedleRightTriggerVariant)

        // --- 26. Test Tube Trigger Variants (Rising Liquid & Swaying Meniscus Physics) ---
        register(TestTubeLeftTriggerVariant)
        register(TestTubeRightTriggerVariant)

        // --- 27. Bloom Trigger Variants (Expanding Radiant Light Bloom Physics) ---
        register(BloomLeftTriggerVariant)
        register(BloomRightTriggerVariant)
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
        if (id.startsWith("builtin.default_") || id.startsWith("builtin.flux_") || id.startsWith("builtin.orb_") ||
            id.startsWith("builtin.compass_") || id.startsWith("builtin.gyro_") || id.startsWith("builtin.spotlight_") ||
            id.startsWith("builtin.lens_") || id.startsWith("builtin.four_lenses_") || id.startsWith("builtin.disc_") ||
            id.startsWith("builtin.capsules_") || id.startsWith("builtin.metaballs_") || id.startsWith("builtin.rails_") ||
            id.startsWith("builtin.arc_") || id.startsWith("builtin.led_") || id.startsWith("builtin.peek_") ||
            id.startsWith("builtin.rib_") || id.startsWith("builtin.under_") ||
            id.startsWith("builtin.tube_") || id.startsWith("builtin.flip_") ||
            id.startsWith("builtin.dial_") || id.startsWith("builtin.liquid_") ||
            id.startsWith("builtin.vu_") || id.startsWith("builtin.target_") ||
            id.startsWith("builtin.slider_") ||
            id.startsWith("builtin.liq_") || id.startsWith("builtin.facet_") ||
            id.startsWith("builtin.flipbtn_") || id.startsWith("builtin.ripple_") ||
            id.startsWith("builtin.orbit_") ||
            id.startsWith("builtin.caps_") || id.startsWith("builtin.ecl_")
        ) return true
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
        heightScale: Float = 1.0f,
        isFlipped: Boolean = false,
        isLocked: Boolean = true,
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
            heightScale = heightScale,
            isFlipped = isFlipped,
            isLocked = isLocked,
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
