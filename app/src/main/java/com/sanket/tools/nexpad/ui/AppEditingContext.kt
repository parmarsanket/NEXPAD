package com.sanket.tools.nexpad.ui

/**
 * Represents the active contextual editing operation when a screen opens
 * Button Studio in Selection Mode to pick an asset for a specific control.
 *
 * Lifecycle:
 *  1. HUD Editor or Virtual Controller calls NavigationViewModel.beginAssetSelection(...)
 *     before navigating to Button Studio.
 *  2. Button Studio reads this context to show the banner, auto-select the
 *     correct category, and highlight the current asset.
 *  3. On Use This, Button Studio calls NavigationViewModel.commitAssetSelection(assetId)
 *     and pops the backstack.
 *  4. HUD Editor / VirtualController resumes, observes pendingAssetResult != null,
 *     applies the change, then calls NavigationViewModel.consumeAssetResult() to clear.
 *  5. On Back/Cancel (no asset chosen), Button Studio simply pops without calling commit.
 *     pendingAssetResult remains null; the originating screen does nothing.
 */
data class AppEditingContext(
    /** Name of the LayoutProfile being edited (e.g. My FPS Layout). */
    val profileName: String,

    /** GamepadControl.key of the specific control whose appearance is being changed (e.g. RT). */
    val controlKey: String,

    /** The asset ID currently applied to this control. Null means native default rendering.
     *  Used to highlight the Current badge in Button Studio. */
    val currentAssetId: String?,

    /** Which screen opened Button Studio — determines where to return context after selection. */
    val originScreen: OriginScreen,

    /** The asset ID chosen by the user in Button Studio.
     *  Null until Button Studio confirms a selection via commitAssetSelection(). */
    val pendingAssetResult: String? = null
)

/** Identifies which screen initiated the contextual Button Studio launch. */
enum class OriginScreen {
    VIRTUAL_CONTROLLER,
    HUD_EDITOR
}
