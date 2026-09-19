package com.sanket.tools.nexpad.ui.virtualcontroller

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.sanket.tools.nexpad.model.LayoutProfile
import com.sanket.tools.nexpad.ui.components.common.NexpadConfirmDialog
import com.sanket.tools.nexpad.ui.components.common.NexpadInputDialog
import com.sanket.tools.nexpad.ui.theme.NeonPalette

/**
 * Standardized dialogs for Virtual Controller management (Duplicate, Rename, Reset, Delete, Preset Protection).
 * Built on top of the universal NexpadConfirmDialog and NexpadInputDialog.
 */

@Composable
fun DuplicateLayoutDialog(
    profile: LayoutProfile,
    onConfirm: (newName: String) -> Unit,
    onDismiss: () -> Unit
) {
    NexpadInputDialog(
        title = "Duplicate Layout",
        initialValue = "${profile.name} Copy",
        label = "Layout Name",
        description = "Enter a name for the new custom layout:",
        confirmText = "Duplicate",
        confirmButtonColor = NeonPalette.Cyan,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Composable
fun RenameLayoutDialog(
    profile: LayoutProfile,
    onConfirm: (newName: String) -> Unit,
    onDismiss: () -> Unit
) {
    NexpadInputDialog(
        title = "Rename Custom Layout",
        initialValue = profile.name,
        label = "Layout Name",
        description = "Enter a new name for '${profile.name}':",
        confirmText = "Rename",
        confirmButtonColor = NeonPalette.Cyan,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Composable
fun ResetLayoutDialog(
    profile: LayoutProfile,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    NexpadConfirmDialog(
        title = "Reset to Factory Default",
        message = "Are you sure you want to reset '${profile.name}' to its original factory positions? Any HUD edits made to this profile will be restored.",
        confirmText = "Reset Default",
        confirmButtonColor = NeonPalette.Purple,
        confirmTextColor = Color.White,
        icon = Icons.Rounded.RestartAlt,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Composable
fun DeleteLayoutDialog(
    profile: LayoutProfile,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    NexpadConfirmDialog(
        title = "Delete Custom Layout",
        message = "Are you sure you want to permanently delete '${profile.name}'?",
        confirmText = "Delete",
        confirmButtonColor = MaterialTheme.colorScheme.error,
        confirmTextColor = MaterialTheme.colorScheme.onError,
        icon = Icons.Rounded.Delete,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Composable
fun PresetProtectionDialog(
    preset: LayoutProfile,
    onConfirm: (customName: String) -> Unit,
    onDismiss: () -> Unit
) {
    NexpadInputDialog(
        title = "Built-in Preset",
        initialValue = "${preset.name} Custom",
        label = "New name",
        description = "\"${preset.name}\" is a built-in preset. Editing it will create a personal copy.",
        confirmText = "Create & Edit",
        confirmButtonColor = NeonPalette.Cyan,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}
