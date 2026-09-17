package com.sanket.tools.nexpad.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Type-safe navigation keys for NEXPAD (Navigation 3).
 * Replaces legacy string routes ("home", "editor", "button_studio?mode=...")
 * with strongly-typed, compile-time verified destinations.
 *
 * Architecture (v2):
 *  - ScreenKey.Editor now carries optional profileName + controlKey so
 *    HudEditorScreen always loads the correct layout and can pre-select
 *    a control without relying on LayoutManager.pendingSelectedKey.
 *  - ScreenKey.ButtonStudio now carries optional controlKey + currentAssetId
 *    so Button Studio can open in the correct category and highlight the
 *    currently applied asset when launched contextually.
 */
@Serializable
sealed interface ScreenKey : NavKey {

    @Serializable
    data object Home : ScreenKey

    @Serializable
    data object Settings : ScreenKey

    @Serializable
    data object Connections : ScreenKey

    /**
     * HUD Editor destination.
     *
     * @param profileName  Name of the layout to load. Null means "use the
     *                     currently active profile" (backward-compatible default).
     * @param controlKey   Optional: CategoryManager control key of the control to
     *                     pre-select after the editor opens (e.g. "RT").
     *                     Null means open with nothing selected.
     */
    @Serializable
    data class Editor(
        val profileName: String? = null,
        val controlKey: String? = null
    ) : ScreenKey

    @Serializable
    data object VirtualController : ScreenKey

    @Serializable
    data object Gamepad : ScreenKey

    /**
     * Button Studio destination.
     *
     * @param mode           "manage" (asset library) or "select" (contextual picker).
     * @param profileName    Layout currently being edited. Empty string = none.
     * @param controlKey     Optional: the specific CategoryManager control key that needs
     *                       a new appearance (e.g. "RT"). Null = manage mode.
     * @param currentAssetId Optional: the asset ID currently applied to controlKey,
     *                       so Button Studio can highlight it as "Current".
     */
    @Serializable
    data class ButtonStudio(
        val mode: String = "viewer",
        val profileName: String = "",
        val controlKey: String? = null,
        val currentAssetId: String? = null
    ) : ScreenKey
}
