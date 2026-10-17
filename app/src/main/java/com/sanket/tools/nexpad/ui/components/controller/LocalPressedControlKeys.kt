package com.sanket.tools.nexpad.ui.components.controller

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import com.sanket.tools.nexpad.category.ControlKey

/**
 * CompositionLocal holding the set of currently pressed controller element keys
 * driven by the Esports Multi-Touch Surface Coordinator.
 */
val LocalPressedControlKeys = staticCompositionLocalOf<Set<String>> { emptySet() }

/**
 * Industry-grade helper function to query whether a given control key or alias is actively pressed.
 * Matches exact key name, uppercase key name, or canonical ControlKey identifier.
 */
@Composable
fun isControlPressed(key: String): Boolean {
    val pressed = LocalPressedControlKeys.current
    if (pressed.isEmpty()) return false
    if (pressed.contains(key)) return true
    val upper = key.uppercase()
    if (pressed.contains(upper)) return true
    val canonical = ControlKey.fromIdentifier(key)?.key
    return canonical != null && pressed.contains(canonical)
}
