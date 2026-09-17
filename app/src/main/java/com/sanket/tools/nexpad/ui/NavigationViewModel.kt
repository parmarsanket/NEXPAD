package com.sanket.tools.nexpad.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
        _editingContext.value = AppEditingContext(
            profileName = profileName,
            controlKey = controlKey,
            currentAssetId = currentAssetId,
            originScreen = originScreen,
            pendingAssetResult = null
        )
    }

    /**
     * Called by Button Studio when the user confirms their asset choice.
     * Sets [AppEditingContext.pendingAssetResult] to the chosen asset ID and
     * pops the backstack — the originating screen will then read and consume
     * this result.
     */
    fun commitAssetSelection(assetId: String) {
        _editingContext.value = _editingContext.value?.copy(
            pendingAssetResult = assetId
        )
    }

    /**
     * Read and clear the pending asset result.
     * Call this from the originating screen after applying the skin change.
     * Returns null if the user cancelled (Back without selecting).
     */
    fun consumeAssetResult(): String? {
        val result = _editingContext.value?.pendingAssetResult
        _editingContext.value = null
        return result
    }

    /**
     * Cancel the current editing context without applying any change.
     * Called when Button Studio pops with no selection.
     */
    fun cancelAssetSelection() {
        _editingContext.value = null
    }
}
