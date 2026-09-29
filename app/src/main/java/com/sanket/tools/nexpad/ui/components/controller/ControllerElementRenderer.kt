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
import com.sanket.tools.nexpad.category.CategoryManager
import com.sanket.tools.nexpad.category.CategoryType
import com.sanket.tools.nexpad.category.ControlKey
import com.sanket.tools.nexpad.category.ControllerLabelStyle
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
    customComponentId: String? = null,
    sensitivity: Float? = null,
    labelStyle: ControllerLabelStyle = ControllerLabelStyle.XBOX
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
                key.equals(K.LSB, ignoreCase = true) || key.equals(K.RSB, ignoreCase = true) || key.equals("L3", ignoreCase = true) || key.equals("R3", ignoreCase = true) -> NexPadControl.Button(key)
                key.equals(K.LS, ignoreCase = true) || key.equals(K.LTP, ignoreCase = true) -> NexPadControl.Stick(isLeft = true)
                key.equals(K.RS, ignoreCase = true) || key.equals(K.RTP, ignoreCase = true) -> NexPadControl.Stick(isLeft = false)
                key.equals(K.LT, ignoreCase = true) || key.equals(K.RT, ignoreCase = true) -> NexPadControl.Trigger(key.uppercase())
                key.uppercase() in listOf(K.UP, K.DOWN, K.LEFT, K.RIGHT) -> NexPadControl.DPad(key.uppercase())
                remoteDoc.manifest.category.equals("JOYSTICK", ignoreCase = true) || remoteDoc.manifest.category.equals("TOUCHPAD", ignoreCase = true) -> {
                    val defCtrl = remoteDoc.manifest.defaultControl.uppercase()
                    val isRight = defCtrl == K.RS || defCtrl == "R3" || defCtrl == K.RSB || defCtrl == K.RTP || defCtrl.contains("RIGHT") || remoteDoc.manifest.id.contains("rtp", ignoreCase = true)
                    NexPadControl.Stick(isLeft = !isRight)
                }
                else -> NexPadControl.Button(key)
            }
            NxprcCanvasRenderer(
                document = remoteDoc,
                assignedControl = targetControl,
                isConnected = isConnected,
                inputTarget = viewModel.asInputTarget(onVibrate),
                rumbleIntensity = rumbleIntensity,
                labelStyle = labelStyle
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
            key == K.LSB || key == K.RSB -> NexPadControl.Button(key)
            key == K.LS || key == K.LTP -> NexPadControl.Stick(isLeft = true)
            key == K.RS || key == K.RTP -> NexPadControl.Stick(isLeft = false)
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
    val displayLabel = CategoryManager.getLabelForStyle(key, labelStyle)
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
        ControlKey.LTP -> RealisticTouchPad(
            isLeft = true,
            isConnected = isConnected,
            viewModel = viewModel,
            onVibrate = onVibrate,
            isRgbEnabled = isRgbEnabled,
            sensitivity = sensitivity
        )
        ControlKey.RTP -> RealisticTouchPad(
            isLeft = false,
            isConnected = isConnected,
            viewModel = viewModel,
            onVibrate = onVibrate,
            isRgbEnabled = isRgbEnabled,
            sensitivity = sensitivity
        )
        ControlKey.LSB -> RealisticStickButton(
            isLeft = true,
            key = K.LSB,
            isConnected = isConnected,
            onVibrate = onVibrate,
            viewModel = viewModel,
            isRgbEnabled = isRgbEnabled,
            displayLabel = displayLabel
        )
        ControlKey.RSB -> RealisticStickButton(
            isLeft = false,
            key = K.RSB,
            isConnected = isConnected,
            onVibrate = onVibrate,
            viewModel = viewModel,
            isRgbEnabled = isRgbEnabled,
            displayLabel = displayLabel
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
            isRgbEnabled = isRgbEnabled,
            displayLabel = displayLabel
        )
        ControlKey.RT -> RealisticTrigger(
            key = K.RT,
            isConnected = isConnected,
            onVibrate = onVibrate,
            viewModel = viewModel,
            isRgbEnabled = isRgbEnabled,
            displayLabel = displayLabel
        )
        ControlKey.LB -> RealisticBumper(
            key = K.LB,
            isConnected = isConnected,
            onVibrate = onVibrate,
            viewModel = viewModel,
            isRgbEnabled = isRgbEnabled,
            displayLabel = displayLabel
        )
        ControlKey.RB -> RealisticBumper(
            key = K.RB,
            isConnected = isConnected,
            onVibrate = onVibrate,
            viewModel = viewModel,
            isRgbEnabled = isRgbEnabled,
            displayLabel = displayLabel
        )
        ControlKey.A -> RealisticButton(
            key = K.A,
            buttonColor = Color(0xFF3FD25A),
            isConnected = isConnected,
            onVibrate = onVibrate,
            viewModel = viewModel,
            isRgbEnabled = isRgbEnabled,
            displayLabel = displayLabel
        )
        ControlKey.B -> RealisticButton(
            key = K.B,
            buttonColor = Color(0xFFE6474E),
            isConnected = isConnected,
            onVibrate = onVibrate,
            viewModel = viewModel,
            isRgbEnabled = isRgbEnabled,
            displayLabel = displayLabel
        )
        ControlKey.X -> RealisticButton(
            key = K.X,
            buttonColor = Color(0xFF3F8FE0),
            isConnected = isConnected,
            onVibrate = onVibrate,
            viewModel = viewModel,
            isRgbEnabled = isRgbEnabled,
            displayLabel = displayLabel
        )
        ControlKey.Y -> RealisticButton(
            key = K.Y,
            buttonColor = Color(0xFFE0A03F),
            isConnected = isConnected,
            onVibrate = onVibrate,
            viewModel = viewModel,
            isRgbEnabled = isRgbEnabled,
            displayLabel = displayLabel
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
                isRgbEnabled = isRgbEnabled,
                displayLabel = displayLabel
            )
        }
    }
}
