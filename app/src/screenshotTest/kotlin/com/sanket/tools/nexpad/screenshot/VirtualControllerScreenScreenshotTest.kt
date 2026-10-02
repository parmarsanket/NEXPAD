package com.sanket.tools.nexpad.screenshot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.sanket.tools.nexpad.model.getDefaultLayoutProfiles
import com.sanket.tools.nexpad.ui.VirtualControllerScreenContent
import com.sanket.tools.nexpad.ui.theme.NEXPADTheme
import com.sanket.tools.nexpad.ui.virtualcontroller.AddCustomLayoutDialog
import com.sanket.tools.nexpad.ui.virtualcontroller.DuplicateLayoutDialog
import com.sanket.tools.nexpad.ui.virtualcontroller.LayoutHubBanner
import com.sanket.tools.nexpad.ui.virtualcontroller.RenameLayoutDialog
import com.sanket.tools.nexpad.ui.virtualcontroller.ResetLayoutDialog

/**
 * Compose Preview Screenshot Tests for [VirtualControllerScreenContent] and its relative components:
 * - Full Virtual Controller Screen in Portrait (Single column grid)
 * - Full Virtual Controller Screen in Landscape (Two-column grid)
 * - Relative components: LayoutHubBanner, AddCustomLayoutDialog, DuplicateLayoutDialog, RenameLayoutDialog, ResetLayoutDialog
 */
class VirtualControllerScreenScreenshotTest {

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF08090C, widthDp = 390, heightDp = 844)
    @Composable
    fun virtualControllerScreenPortraitPreview() {
        NEXPADTheme {
            VirtualControllerScreenContent(
                profiles = getDefaultLayoutProfiles(),
                activeProfileName = "Standard Elite"
            )
        }
    }

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF08090C, widthDp = 840, heightDp = 390)
    @Composable
    fun virtualControllerScreenLandscapeTwoColPreview() {
        NEXPADTheme {
            VirtualControllerScreenContent(
                profiles = getDefaultLayoutProfiles(),
                activeProfileName = "Standard Elite"
            )
        }
    }

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF08090C, widthDp = 420, heightDp = 100)
    @Composable
    fun layoutHubBannerPreview() {
        NEXPADTheme {
            Box(
                modifier = Modifier
                    .background(Color(0xFF08090C))
                    .padding(16.dp)
            ) {
                LayoutHubBanner()
            }
        }
    }

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF08090C, widthDp = 400, heightDp = 500)
    @Composable
    fun addCustomLayoutDialogPreview() {
        NEXPADTheme {
            AddCustomLayoutDialog(
                defaults = getDefaultLayoutProfiles(),
                onDismiss = {},
                onCreate = { _, _, _, _ -> },
                onDesignInStudio = {}
            )
        }
    }

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF08090C, widthDp = 360, heightDp = 260)
    @Composable
    fun duplicateLayoutDialogPreview() {
        val sampleProfile = getDefaultLayoutProfiles().first()
        NEXPADTheme {
            DuplicateLayoutDialog(
                profile = sampleProfile,
                onConfirm = {},
                onDismiss = {}
            )
        }
    }

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF08090C, widthDp = 360, heightDp = 260)
    @Composable
    fun renameLayoutDialogPreview() {
        val sampleProfile = getDefaultLayoutProfiles().first()
        NEXPADTheme {
            RenameLayoutDialog(
                profile = sampleProfile,
                onConfirm = {},
                onDismiss = {}
            )
        }
    }

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF08090C, widthDp = 360, heightDp = 260)
    @Composable
    fun resetLayoutDialogPreview() {
        val sampleProfile = getDefaultLayoutProfiles().first()
        NEXPADTheme {
            ResetLayoutDialog(
                profile = sampleProfile,
                onConfirm = {},
                onDismiss = {}
            )
        }
    }
}
