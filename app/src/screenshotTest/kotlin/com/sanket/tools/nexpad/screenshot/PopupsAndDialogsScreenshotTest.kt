package com.sanket.tools.nexpad.screenshot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.sanket.tools.nexpad.model.HudElement
import com.sanket.tools.nexpad.model.LayoutTransform
import com.sanket.tools.nexpad.ui.components.common.NexpadConfirmDialog
import com.sanket.tools.nexpad.ui.components.common.NexpadInputDialog
import com.sanket.tools.nexpad.ui.hud.HudButtonPaletteDialog
import com.sanket.tools.nexpad.ui.theme.NeonPalette

/**
 * Compose Preview Screenshot Tests for NEXPAD Dialogs, Modals, and Popup Previews.
 */
class PopupsAndDialogsScreenshotTest {

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF0A0B0E, widthDp = 420, heightDp = 320)
    @Composable
    fun confirmDeleteDialogPreview() {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0B0E))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            NexpadConfirmDialog(
                title = "Delete Profile?",
                message = "Are you sure you want to permanently delete 'Cyberpunk Pro'? This action cannot be undone.",
                confirmText = "Delete",
                cancelText = "Cancel",
                confirmButtonColor = NeonPalette.Red,
                confirmTextColor = Color.White,
                onConfirm = {},
                onDismiss = {}
            )
        }
    }

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF0A0B0E, widthDp = 420, heightDp = 340)
    @Composable
    fun inputRenameDialogPreview() {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0B0E))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            NexpadInputDialog(
                title = "Rename Layout",
                initialValue = "Custom Esports HUD",
                label = "Layout Name",
                placeholder = "Enter custom name",
                description = "Choose a descriptive title for this button layout configuration.",
                confirmText = "Save",
                cancelText = "Cancel",
                confirmButtonColor = NeonPalette.Cyan,
                confirmTextColor = Color.Black,
                onConfirm = {},
                onDismiss = {}
            )
        }
    }

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF0A0B0E, widthDp = 460, heightDp = 620)
    @Composable
    fun buttonPaletteDialogPreview() {
        val sampleElements = mapOf(
            "A" to HudElement("A", LayoutTransform(0.8f, 0.7f)),
            "B" to HudElement("B", LayoutTransform(0.9f, 0.6f)),
            "X" to HudElement("X", LayoutTransform(0.7f, 0.6f)),
            "Y" to HudElement("Y", LayoutTransform(0.8f, 0.5f)),
            "LT" to HudElement("LT", LayoutTransform(0.2f, 0.1f)),
            "RT" to HudElement("RT", LayoutTransform(0.8f, 0.1f))
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0B0E))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            HudButtonPaletteDialog(
                currentElements = sampleElements,
                onToggleControl = {},
                onRestoreAll = {},
                onStandardOnly = {},
                onClearAll = {},
                onOpenStudio = {},
                onDismiss = {}
            )
        }
    }
}
