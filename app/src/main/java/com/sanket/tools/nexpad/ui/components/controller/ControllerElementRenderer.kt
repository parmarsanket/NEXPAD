package com.sanket.tools.nexpad.ui.components.controller

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel

/**
 * Unified renderer for individual controller elements (joysticks, triggers, bumpers, dpad, buttons).
 * Eliminates redundant layout branching across GamepadScreen and HudEditorScreen.
 */
@Composable
fun ControllerElementRenderer(
    key: String,
    isConnected: Boolean,
    isRgbEnabled: Boolean,
    viewModel: GamepadViewModel,
    onVibrate: () -> Unit = {}
) {
    when {
        key == "LS" -> RealisticJoystick(
            isLeft = true,
            isConnected = isConnected,
            viewModel = viewModel,
            isRgbEnabled = isRgbEnabled
        )
        key == "RS" -> RealisticJoystick(
            isLeft = false,
            isConnected = isConnected,
            viewModel = viewModel,
            isRgbEnabled = isRgbEnabled
        )
        key == "DPAD" -> RealisticDPad(
            isConnected = isConnected,
            viewModel = viewModel,
            isRgbEnabled = isRgbEnabled
        )
        key == "LT" || key == "RT" -> RealisticTrigger(
            key = key,
            isConnected = isConnected,
            onVibrate = onVibrate,
            viewModel = viewModel,
            isRgbEnabled = isRgbEnabled
        )
        key == "LB" || key == "RB" -> RealisticBumper(
            key = key,
            isConnected = isConnected,
            onVibrate = onVibrate,
            viewModel = viewModel,
            isRgbEnabled = isRgbEnabled
        )
        key == "A" -> RealisticButton(
            key = "A",
            buttonColor = Color(0xFF00C853),
            isConnected = isConnected,
            onVibrate = onVibrate,
            viewModel = viewModel,
            isRgbEnabled = isRgbEnabled
        )
        key == "B" -> RealisticButton(
            key = "B",
            buttonColor = Color(0xFFD50000),
            isConnected = isConnected,
            onVibrate = onVibrate,
            viewModel = viewModel,
            isRgbEnabled = isRgbEnabled
        )
        key == "X" -> RealisticButton(
            key = "X",
            buttonColor = Color(0xFF2962FF),
            isConnected = isConnected,
            onVibrate = onVibrate,
            viewModel = viewModel,
            isRgbEnabled = isRgbEnabled
        )
        key == "Y" -> RealisticButton(
            key = "Y",
            buttonColor = Color(0xFFFFD600),
            isConnected = isConnected,
            onVibrate = onVibrate,
            viewModel = viewModel,
            isRgbEnabled = isRgbEnabled
        )
        key in listOf("MENU", "VIEW", "XBOX", "SHARE", "SCREENSHOT") -> RealisticSystemButton(
            key = key,
            isConnected = isConnected,
            onVibrate = onVibrate,
            viewModel = viewModel,
            isRgbEnabled = isRgbEnabled
        )
        key in listOf("M1", "M2", "M3", "M4", "PROFILE", "TURBO") -> RealisticMacroButton(
            key = key,
            isConnected = isConnected,
            onVibrate = onVibrate,
            viewModel = viewModel,
            isRgbEnabled = isRgbEnabled
        )
        else -> RealisticButton(
            key = key,
            buttonColor = Color.Gray,
            isConnected = isConnected,
            onVibrate = onVibrate,
            viewModel = viewModel,
            isRgbEnabled = isRgbEnabled
        )
    }
}
