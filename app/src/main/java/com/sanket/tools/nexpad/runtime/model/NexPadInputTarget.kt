package com.sanket.tools.nexpad.runtime.model

import com.sanket.tools.nexpad.viewmodel.GamepadViewModel

/**
 * Hot-path input callback interface executed directly on the UI thread.
 * Guarantees zero allocation and microsecond latency for 120Hz continuous input.
 */
interface NexPadInputTarget {
    fun onButtonPress(control: NexPadControl.Button)
    fun onButtonRelease(control: NexPadControl.Button)
    fun onStickMove(stick: NexPadControl.Stick, normX: Float, normY: Float)
    fun onTriggerMove(trigger: NexPadControl.Trigger, pressure: Float)
    fun triggerHaptic(type: String = "light")
}

/**
 * Connects any NexPadInputTarget call directly into the real-time GamepadViewModel pipeline.
 */
fun GamepadViewModel.asInputTarget(onVibrate: () -> Unit = {}): NexPadInputTarget {
    return object : NexPadInputTarget {
        override fun onButtonPress(control: NexPadControl.Button) {
            onVibrate()
            updateButton(control.key, true)
        }

        override fun onButtonRelease(control: NexPadControl.Button) {
            updateButton(control.key, false)
        }

        override fun onStickMove(stick: NexPadControl.Stick, normX: Float, normY: Float) {
            if (stick.isLeft) {
                updateLeftStick(normX, normY)
            } else {
                updateRightStick(normX, normY)
            }
        }

        override fun onTriggerMove(trigger: NexPadControl.Trigger, pressure: Float) {
            if (pressure > 0.5f) {
                onVibrate()
                updateButton(trigger.key, true)
            } else {
                updateButton(trigger.key, false)
            }
        }

        override fun triggerHaptic(type: String) {
            onVibrate()
        }
    }
}

/**
 * Sandboxed input target for UI studio and preview modals.
 * Captures events for telemetry without dispatching network packets.
 */
class SandboxInputTarget(
    private val onStateChange: (String) -> Unit = {},
    private val onAxisChange: (Float, Float) -> Unit = { _, _ -> },
    private val onHapticTriggered: () -> Unit = {}
) : NexPadInputTarget {
    override fun onButtonPress(control: NexPadControl.Button) {
        onStateChange("PRESSED (${control.key})")
        onHapticTriggered()
    }

    override fun onButtonRelease(control: NexPadControl.Button) {
        onStateChange("RELEASED (${control.key})")
    }

    override fun onStickMove(stick: NexPadControl.Stick, normX: Float, normY: Float) {
        onAxisChange(normX, normY)
        onStateChange("STICK ${if (stick.isLeft) "LS" else "RS"}: X=%.2f, Y=%.2f".format(normX, normY))
    }

    override fun onTriggerMove(trigger: NexPadControl.Trigger, pressure: Float) {
        onStateChange("TRIGGER ${trigger.key}: %.2f".format(pressure))
        if (pressure > 0.5f) onHapticTriggered()
    }

    override fun triggerHaptic(type: String) {
        onHapticTriggered()
    }
}
