package com.sanket.tools.nexpad.ui.studio.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.sanket.tools.nexpad.category.CategoryManager
import com.sanket.tools.nexpad.category.CategoryType
import com.sanket.tools.nexpad.category.ControlKey
import com.sanket.tools.nexpad.category.ControllerLabelStyle
import com.sanket.tools.nexpad.ui.components.controller.abxy.RealisticButton
import com.sanket.tools.nexpad.ui.components.controller.abxy.resolveButtonColor
import com.sanket.tools.nexpad.ui.components.controller.bumper.RealisticBumper
import com.sanket.tools.nexpad.ui.components.controller.dpad.RealisticDPad
import com.sanket.tools.nexpad.ui.components.controller.dpad.RealisticDPadButton
import com.sanket.tools.nexpad.ui.components.controller.macros.RealisticMacroButton
import com.sanket.tools.nexpad.ui.components.controller.special.RealisticTouchPad
import com.sanket.tools.nexpad.ui.components.controller.special.StaticGyroToggleButton
import com.sanket.tools.nexpad.ui.components.controller.sticks.RealisticJoystick
import com.sanket.tools.nexpad.ui.components.controller.sticks.RealisticStickButton
import com.sanket.tools.nexpad.ui.components.controller.system.RealisticSystemButton
import com.sanket.tools.nexpad.ui.components.controller.trigger.RealisticTrigger

/**
 * Pixel-perfect static preview for native controller elements.
 * Exactly mirrors the visuals, gradients, 3D glossy highlights, and shadows
 * of ui/components/controller/Realistic*.kt without interactive overhead.
 */
@Composable
fun StaticDefaultButtonPreview(
    controlKey: String,
    modifier: Modifier = Modifier,
    labelStyle: ControllerLabelStyle = ControllerLabelStyle.XBOX
) {
    val ctrl = ControlKey.fromIdentifier(controlKey)
    val key = ctrl?.key ?: controlKey.uppercase()
    val displayLabel = CategoryManager.getLabelForStyle(key, labelStyle)

    if (key.equals(com.sanket.tools.nexpad.model.BuiltinComponentIds.GYRO, ignoreCase = true)) {
        StaticGyroToggleButton(
            isRgbEnabled = true,
            labelStyle = labelStyle,
            modifier = modifier
        )
        return
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        when (ctrl) {
            ControlKey.A -> RealisticButton(key = ControlKey.A.key, buttonColor = resolveButtonColor(ControlKey.A.key, labelStyle, displayLabel), displayLabel = displayLabel, isInteractive = false)
            ControlKey.B -> RealisticButton(key = ControlKey.B.key, buttonColor = resolveButtonColor(ControlKey.B.key, labelStyle, displayLabel), displayLabel = displayLabel, isInteractive = false)
            ControlKey.X -> RealisticButton(key = ControlKey.X.key, buttonColor = resolveButtonColor(ControlKey.X.key, labelStyle, displayLabel), displayLabel = displayLabel, isInteractive = false)
            ControlKey.Y -> RealisticButton(key = ControlKey.Y.key, buttonColor = resolveButtonColor(ControlKey.Y.key, labelStyle, displayLabel), displayLabel = displayLabel, isInteractive = false)

            ControlKey.LS -> RealisticJoystick(isLeft = true, isInteractive = false)
            ControlKey.RS -> RealisticJoystick(isLeft = false, isInteractive = false)
            ControlKey.LSB -> RealisticStickButton(isLeft = true, displayLabel = displayLabel, isInteractive = false)
            ControlKey.RSB -> RealisticStickButton(isLeft = false, displayLabel = displayLabel, isInteractive = false)
            ControlKey.LTP -> RealisticTouchPad(isLeft = true, isInteractive = false)
            ControlKey.RTP -> RealisticTouchPad(isLeft = false, isInteractive = false)

            ControlKey.DPAD -> RealisticDPad(isInteractive = false)
            ControlKey.UP, ControlKey.DOWN, ControlKey.LEFT, ControlKey.RIGHT -> RealisticDPadButton(direction = key, isInteractive = false)

            ControlKey.LT -> RealisticTrigger(key = ControlKey.LT.key, displayLabel = displayLabel, isInteractive = false)
            ControlKey.RT -> RealisticTrigger(key = ControlKey.RT.key, displayLabel = displayLabel, isInteractive = false)

            ControlKey.LB -> RealisticBumper(key = ControlKey.LB.key, displayLabel = displayLabel, isInteractive = false)
            ControlKey.RB -> RealisticBumper(key = ControlKey.RB.key, displayLabel = displayLabel, isInteractive = false)

            ControlKey.GUIDE -> RealisticSystemButton(key = ControlKey.GUIDE.key, isInteractive = false)
            ControlKey.START -> RealisticSystemButton(key = ControlKey.START.key, isInteractive = false)
            ControlKey.BACK  -> RealisticSystemButton(key = ControlKey.BACK.key, isInteractive = false)
            ControlKey.SHARE -> RealisticSystemButton(key = ControlKey.SHARE.key, isInteractive = false)

            ControlKey.M1, ControlKey.M2, ControlKey.M3, ControlKey.M4 -> RealisticMacroButton(key = key, displayLabel = displayLabel, isInteractive = false)

            else -> when (ctrl?.categoryType) {
                CategoryType.SYSTEM -> RealisticSystemButton(key = key, isInteractive = false)
                CategoryType.MACROS -> RealisticMacroButton(key = key, displayLabel = displayLabel, isInteractive = false)
                else -> RealisticButton(key = key, buttonColor = Color.Gray, displayLabel = displayLabel.take(3), isInteractive = false)
            }
        }
    }
}
