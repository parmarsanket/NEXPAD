package com.sanket.tools.nexpad.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Type-safe navigation keys for NEXPAD (Navigation 3).
 * Replaces legacy string routes ("home", "editor", "button_studio?mode=...")
 * with strongly-typed, compile-time verified destinations.
 */
@Serializable
sealed interface ScreenKey : NavKey {

    @Serializable
    data object Home : ScreenKey

    @Serializable
    data object Settings : ScreenKey

    @Serializable
    data object Connections : ScreenKey

    @Serializable
    data object Editor : ScreenKey

    @Serializable
    data object VirtualController : ScreenKey

    @Serializable
    data object Gamepad : ScreenKey

    @Serializable
    data class ButtonStudio(
        val mode: String = "manage",
        val profileName: String = ""
    ) : ScreenKey
}
