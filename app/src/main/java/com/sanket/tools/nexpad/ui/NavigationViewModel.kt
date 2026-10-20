package com.sanket.tools.nexpad.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Graph-scoped ViewModel that carries the editing context between screens.
 *
 * Instantiated once at the NavigationGraph level so that Button Studio,
 * HUD Editor and Virtual Controller all share the same instance. This
 * replaces the former LayoutManager.pendingSelectedKey sideband.
 *
 * Usage pattern:
 *   (1) Caller sets context and navigates to ButtonStudio.
 *   (2) ButtonStudio reads [editingContext] to configure its UI.
 *   (3) ButtonStudio calls [commitAssetSelection] on confirmation.
 *   (4) Caller observes [editingContext] on resume, calls [consumeAssetResult].
 */
class NavigationViewModel : ViewModel() {

    private val _editingContext = MutableStateFlow<AppEditingContext?>(null)

    /**
     * The currently active editing context.
     * Non-null while a contextual Button Studio session is in progress.
     * Automatically nulled after [consumeAssetResult] is called.
     */
    val editingContext: StateFlow<AppEditingContext?> = _editingContext.asStateFlow()

    /**
     * Begin a contextual asset-selection session.
     * Call this BEFORE navigating to ButtonStudio.
     */
    fun beginAssetSelection(
        profileName: String,
        controlKey: String,
        currentAssetId: String?,
        originScreen: OriginScreen
    ) {
        val trimmedProfile = profileName.trim()
        val trimmedKey = controlKey.trim()
        _editingContext.update {
            AppEditingContext(
                profileName = trimmedProfile,
                controlKey = trimmedKey,
                currentAssetId = currentAssetId?.trim(),
                originScreen = originScreen,
                pendingAssetResult = null
            )
        }
    }

    /**
     * Called by Button Studio when the user confirms their asset choice.
     * Sets [AppEditingContext.pendingAssetResult] to the chosen asset ID and
     * pops the backstack — the originating screen will then read and consume
     * this result.
     */
    fun commitAssetSelection(assetId: String) {
        val trimmed = assetId.trim()
        _editingContext.update { current ->
            current?.copy(pendingAssetResult = trimmed)
        }
    }

    /**
     * Read and clear the pending asset result atomically.
     * Call this from the originating screen after applying the skin change.
     * Returns null if the user cancelled (Back without selecting).
     */
    fun consumeAssetResult(): String? {
        var result: String? = null
        _editingContext.update { current ->
            result = current?.pendingAssetResult
            null
        }
        return result
    }

    /**
     * Cancel the current editing context without applying any change.
     * Called when Button Studio pops with no selection.
     */
    fun cancelAssetSelection() {
        _editingContext.update { null }
    }

    // ── Transient Gamepad Session (Industry ViewModel Approach) ───────────
    private val _sessionProfileName = MutableStateFlow<String?>(null)

    /**
     * The profile name for the currently active gameplay/preview session.
     * When non-null, GamepadScreen uses this layout without mutating the
     * user's globally active profile in LayoutManager.
     */
    val sessionProfileName: StateFlow<String?> = _sessionProfileName.asStateFlow()

    /**
     * Start a temporary gameplay session for [profileName].
     */
    fun startGamepadSession(profileName: String?) {
        _sessionProfileName.update { profileName?.trim() }
    }

    /**
     * Clear the session once the Gamepad screen is exited.
     */
    fun clearGamepadSession() {
        _sessionProfileName.update { null }
    }
}
