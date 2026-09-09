package com.sanket.tools.nexpad.runtime.model

/**
 * Semantic controller targets that visual components can map to.
 * Completely decouples visual presentation from transport/hardware semantics.
 */
sealed interface NexPadControl {
    data class Button(val key: String) : NexPadControl
    data class Stick(val isLeft: Boolean) : NexPadControl
    data class Trigger(val key: String) : NexPadControl
    data class DPad(val direction: String) : NexPadControl
}
