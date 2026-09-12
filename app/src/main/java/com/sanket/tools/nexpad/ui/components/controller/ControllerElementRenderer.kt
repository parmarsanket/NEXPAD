package com.sanket.tools.nexpad.ui.components.controller

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel

import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.sanket.tools.nexpad.runtime.engine.NxprcCanvasRenderer
import com.sanket.tools.nexpad.runtime.engine.NxpComposeInterpreter
import com.sanket.tools.nexpad.runtime.model.NexPadControl
import com.sanket.tools.nexpad.runtime.model.asInputTarget
import com.sanket.tools.nexpad.runtime.plugin.RemoteComponentRegistry
import com.sanket.tools.nexpad.runtime.registry.ComponentRegistry

/**
 * Unified renderer for individual controller elements (joysticks, triggers, bumpers, dpad, buttons).
 * Supports both built-in realistic elements and dynamic custom NXP components.
 */
@Composable
fun ControllerElementRenderer(
    key: String,
    isConnected: Boolean,
    isRgbEnabled: Boolean,
    viewModel: GamepadViewModel,
    onVibrate: () -> Unit = {},
    customComponentId: String? = null
) {
    val context = LocalContext.current
    val isDefaultNative = customComponentId == null || customComponentId.startsWith("builtin.default_")

    val feedback by viewModel.feedbackFlow.collectAsState(initial = null)
    val rumbleIntensity = remember(feedback) {
        val fb = feedback
        if (fb != null && (fb.leftMotorSpeed > 0 || fb.rightMotorSpeed > 0)) {
            maxOf(fb.leftMotorSpeed, fb.rightMotorSpeed) / 255f
        } else 0f
    }

    if (customComponentId != null && customComponentId.startsWith("rc.")) {
        val remoteRegistry = remember { RemoteComponentRegistry.getInstance(context) }
        val remoteDoc = remember(customComponentId) { remoteRegistry.getComponent(customComponentId) }
        if (remoteDoc != null) {
            val targetControl = when {
                key == "LS" -> NexPadControl.Stick(isLeft = true)
                key == "RS" -> NexPadControl.Stick(isLeft = false)
                key == "LT" || key == "RT" -> NexPadControl.Trigger(key)
                key in listOf("UP", "DOWN", "LEFT", "RIGHT") -> NexPadControl.DPad(key)
                else -> NexPadControl.Button(key)
            }
            NxprcCanvasRenderer(
                document = remoteDoc,
                assignedControl = targetControl,
                isConnected = isConnected,
                inputTarget = viewModel.asInputTarget(onVibrate),
                rumbleIntensity = rumbleIntensity
            )
            return
        }
    }

    val customDef = remember(customComponentId, isDefaultNative) {
        if (isDefaultNative) null else ComponentRegistry.getInstance(context).getComponent(customComponentId)
    }

    if (customDef != null) {
        val targetControl = when {
            key == "LS" -> NexPadControl.Stick(isLeft = true)
            key == "RS" -> NexPadControl.Stick(isLeft = false)
            key == "LT" || key == "RT" -> NexPadControl.Trigger(key)
            key in listOf("UP", "DOWN", "LEFT", "RIGHT") -> NexPadControl.DPad(key)
            else -> NexPadControl.Button(key)
        }
        NxpComposeInterpreter(
            definition = customDef,
            assignedControl = targetControl,
            isConnected = isConnected,
            inputTarget = viewModel.asInputTarget(onVibrate)
        )
        return
    }

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
            isRgbEnabled = isRgbEnabled,
            onVibrate = onVibrate
        )
        key in listOf("UP", "DOWN", "LEFT", "RIGHT") -> RealisticDPadButton(
            direction = key,
            isConnected = isConnected,
            onVibrate = onVibrate,
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
