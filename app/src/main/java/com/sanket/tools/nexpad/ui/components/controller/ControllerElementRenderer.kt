package com.sanket.tools.nexpad.ui.components.controller

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.sanket.tools.nexpad.category.ControllerLabelStyle
import com.sanket.tools.nexpad.runtime.engine.NxprcCanvasRenderer
import com.sanket.tools.nexpad.runtime.engine.NxpComposeInterpreter
import com.sanket.tools.nexpad.runtime.model.NexPadControl
import com.sanket.tools.nexpad.runtime.model.asInputTarget
import com.sanket.tools.nexpad.runtime.plugin.RemoteComponentRegistry
import com.sanket.tools.nexpad.runtime.registry.ComponentRegistry
import com.sanket.tools.nexpad.runtime.registry.NativeComponentRegistry
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel

/**
 * Scalable, industry-standard unified renderer for individual controller elements
 * (joysticks, triggers, bumpers, dpad, action buttons, system buttons, touchpads).
 *
 * Dispatches cleanly across:
 * 1. Remote Compose (.nxprc) documents
 * 2. Native Compose elements (Realistic 3D, Flux Cyber, etc. via [NativeComponentRegistry])
 * 3. Dynamic custom NXP JSON component skins
 * 4. Safe baseline native fallback
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
    heightScale: Float = 1.0f,
    isFlipped: Boolean = false,
    isLocked: Boolean = true,
    labelStyle: ControllerLabelStyle = ControllerLabelStyle.XBOX,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isNative = NativeComponentRegistry.isNativeBuiltin(customComponentId)

    val feedback by viewModel.feedbackFlow.collectAsState(initial = null)
    val rumbleIntensity = remember(feedback) {
        val fb = feedback
        if (fb != null && (fb.leftMotorSpeed > 0 || fb.rightMotorSpeed > 0)) {
            maxOf(fb.leftMotorSpeed, fb.rightMotorSpeed) / 255f
        } else 0f
    }

    // 1. Remote Compose (.nxprc) Document
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

    // 2. Built-in Native Compose Elements (Realistic 3D, Flux Cyber, etc.)
    if (isNative) {
        NativeComponentRegistry.RenderNativeElement(
            key = key,
            customComponentId = customComponentId,
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
        return
    }

    // 3. Dynamic Custom NXP JSON Skin
    val customDef = remember(customComponentId) {
        if (customComponentId != null) {
            ComponentRegistry.getInstance(context).getComponent(customComponentId)
        } else null
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

    // 4. Safe Baseline Native Fallback
    NativeComponentRegistry.RenderNativeElement(
        key = key,
        customComponentId = null,
        isConnected = isConnected,
        isRgbEnabled = isRgbEnabled,
        viewModel = viewModel,
        onVibrate = onVibrate,
        sensitivity = sensitivity,
        isLocked = isLocked,
        labelStyle = labelStyle,
        modifier = modifier
    )
}
