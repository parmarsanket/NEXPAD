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
import com.sanket.tools.nexpad.category.CategoryType
import com.sanket.tools.nexpad.category.ControlKey
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
        val loadedDocs by remoteRegistry.loadedComponents.collectAsState()
        val remoteDoc = remember(customComponentId, loadedDocs) { remoteRegistry.getComponent(customComponentId) }
        val K = com.sanket.tools.nexpad.model.NexpadKeys
        if (remoteDoc != null) {
            val targetControl = when {
                key.equals(K.LS, ignoreCase = true) || key.equals("L3", ignoreCase = true) -> NexPadControl.Stick(isLeft = true)
                key.equals(K.RS, ignoreCase = true) || key.equals("R3", ignoreCase = true) -> NexPadControl.Stick(isLeft = false)
                key.equals(K.LT, ignoreCase = true) || key.equals(K.RT, ignoreCase = true) -> NexPadControl.Trigger(key.uppercase())
                key.uppercase() in listOf(K.UP, K.DOWN, K.LEFT, K.RIGHT) -> NexPadControl.DPad(key.uppercase())
                remoteDoc.manifest.category.equals("JOYSTICK", ignoreCase = true) ->
                    NexPadControl.Stick(isLeft = remoteDoc.manifest.defaultControl.uppercase() != K.RS && remoteDoc.manifest.defaultControl.uppercase() != "R3")
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

    val K = com.sanket.tools.nexpad.model.NexpadKeys
    if (customDef != null) {
        val targetControl = when {
            key == K.LS -> NexPadControl.Stick(isLeft = true)
            key == K.RS -> NexPadControl.Stick(isLeft = false)
            key == K.LT || key == K.RT -> NexPadControl.Trigger(key)
            key in listOf(K.UP, K.DOWN, K.LEFT, K.RIGHT) -> NexPadControl.DPad(key)
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

    val ctrl = ControlKey.fromIdentifier(key)
    when (ctrl) {
        ControlKey.LS -> RealisticJoystick(
            isLeft = true,
            isConnected = isConnected,
            viewModel = viewModel,
            isRgbEnabled = isRgbEnabled
        )
        ControlKey.RS -> RealisticJoystick(
            isLeft = false,
            isConnected = isConnected,
            viewModel = viewModel,
            isRgbEnabled = isRgbEnabled
        )
        ControlKey.DPAD -> RealisticDPad(
            isConnected = isConnected,
            viewModel = viewModel,
            isRgbEnabled = isRgbEnabled,
            onVibrate = onVibrate
        )
        ControlKey.UP, ControlKey.DOWN, ControlKey.LEFT, ControlKey.RIGHT -> RealisticDPadButton(
            direction = key,
            isConnected = isConnected,
            onVibrate = onVibrate,
            viewModel = viewModel,
            isRgbEnabled = isRgbEnabled
        )
        ControlKey.LT -> RealisticTrigger(
            key = K.LT,
            isConnected = isConnected,
            onVibrate = onVibrate,
            viewModel = viewModel,
            isRgbEnabled = isRgbEnabled
        )
        ControlKey.RT -> RealisticTrigger(
            key = K.RT,
            isConnected = isConnected,
            onVibrate = onVibrate,
            viewModel = viewModel,
            isRgbEnabled = isRgbEnabled
        )
        ControlKey.LB -> RealisticBumper(
            key = K.LB,
            isConnected = isConnected,
            onVibrate = onVibrate,
            viewModel = viewModel,
            isRgbEnabled = isRgbEnabled
        )
        ControlKey.RB -> RealisticBumper(
            key = K.RB,
            isConnected = isConnected,
            onVibrate = onVibrate,
            viewModel = viewModel,
            isRgbEnabled = isRgbEnabled
        )
        ControlKey.A -> RealisticButton(
            key = K.A,
            buttonColor = Color(0xFF00C853),
            isConnected = isConnected,
            onVibrate = onVibrate,
            viewModel = viewModel,
            isRgbEnabled = isRgbEnabled
        )
        ControlKey.B -> RealisticButton(
            key = K.B,
            buttonColor = Color(0xFFD50000),
            isConnected = isConnected,
            onVibrate = onVibrate,
            viewModel = viewModel,
            isRgbEnabled = isRgbEnabled
        )
        ControlKey.X -> RealisticButton(
            key = K.X,
            buttonColor = Color(0xFF2962FF),
            isConnected = isConnected,
            onVibrate = onVibrate,
            viewModel = viewModel,
            isRgbEnabled = isRgbEnabled
        )
        ControlKey.Y -> RealisticButton(
            key = K.Y,
            buttonColor = Color(0xFFFFD600),
            isConnected = isConnected,
            onVibrate = onVibrate,
            viewModel = viewModel,
            isRgbEnabled = isRgbEnabled
        )
        ControlKey.GUIDE, ControlKey.START, ControlKey.BACK, ControlKey.SHARE -> RealisticSystemButton(
            key = key,
            isConnected = isConnected,
            onVibrate = onVibrate,
            viewModel = viewModel,
            isRgbEnabled = isRgbEnabled
        )
        ControlKey.M1, ControlKey.M2, ControlKey.M3, ControlKey.M4 -> RealisticMacroButton(
            key = key,
            isConnected = isConnected,
            onVibrate = onVibrate,
            viewModel = viewModel,
            isRgbEnabled = isRgbEnabled
        )
        else -> when (ctrl?.categoryType) {
            CategoryType.SYSTEM -> RealisticSystemButton(
                key = key,
                isConnected = isConnected,
                onVibrate = onVibrate,
                viewModel = viewModel,
                isRgbEnabled = isRgbEnabled
            )
            CategoryType.MACROS -> RealisticMacroButton(
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
}
