package com.sanket.tools.nexpad.screenshot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.sanket.tools.nexpad.model.LayoutProfile
import com.sanket.tools.nexpad.model.getDefaultLayoutProfiles
import com.sanket.tools.nexpad.ui.components.home.DeviceCardState
import com.sanket.tools.nexpad.ui.components.home.DeviceHeroCard
import com.sanket.tools.nexpad.ui.components.home.VShapedPanel
import com.sanket.tools.nexpad.ui.hud.HudTopBar
import com.sanket.tools.nexpad.ui.virtualcontroller.LayoutProfileCard
import com.sanket.tools.nexpad.viewmodel.ConnectionStats
import com.sanket.tools.nexpad.viewmodel.ConnectionType

/**
 * Compose Preview Screenshot Tests for NEXPAD Screen-level UIs, Telemetry Cards, and HUD Navigation.
 */
class ScreensUiScreenshotTest {

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF08090C, widthDp = 500, heightDp = 300)
    @Composable
    fun deviceHeroCardSearchingPreview() {
        Box(
            modifier = Modifier
                .background(Color(0xFF08090C))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            DeviceHeroCard(
                isConnected = false,
                servers = emptyList(),
                stats = ConnectionStats(),
                isAoaAttached = false,
                isAdbAvailable = false,
                adbServerName = null,
                onConnectServer = {},
                onConnectAoa = {},
                onConnectAdb = {},
                onDisconnectClick = {},
                onOpenConnectionHub = {}
            )
        }
    }

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF08090C, widthDp = 500, heightDp = 300)
    @Composable
    fun deviceHeroCardConnectedPreview() {
        val stats = ConnectionStats(
            transport = ConnectionType.WIFI,
            serverDeviceName = "NEXPAD-GAMING-RIG",
            signalLevel = 4,
            signalDbm = -42,
            rxLinkSpeedMbps = 866,
            txLinkSpeedMbps = 866,
            latencyMs = 1L,
            jitterMs = 0.2f,
            packetLossPercent = 0.0f
        )

        Box(
            modifier = Modifier
                .background(Color(0xFF08090C))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            DeviceHeroCard(
                isConnected = true,
                servers = emptyList(),
                stats = stats,
                isAoaAttached = false,
                isAdbAvailable = false,
                adbServerName = null,
                onConnectServer = {},
                onConnectAoa = {},
                onConnectAdb = {},
                onDisconnectClick = {},
                onOpenConnectionHub = {}
            )
        }
    }

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF08090C, widthDp = 480, heightDp = 180)
    @Composable
    fun layoutProfileCardPreview() {
        val sampleProfile = LayoutProfile(
            name = "Cyberpunk Esports Elite",
            isDefault = false,
            isRgbEnabled = true,
            description = "Low-latency custom 120 FPS trigger & bumper configuration"
        )

        Box(
            modifier = Modifier
                .background(Color(0xFF08090C))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            LayoutProfileCard(
                profile = sampleProfile,
                isActive = true,
                isDragging = false,
                onSetActive = {},
                onPlay = {},
                onEditHud = {},
                onOpenStudio = {},
                onDuplicate = {},
                onRename = {},
                onShare = {},
                onReset = {},
                onDelete = {}
            )
        }
    }

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF08090C, widthDp = 500, heightDp = 380)
    @Composable
    fun vShapedPanelPreview() {
        Box(
            modifier = Modifier
                .background(Color(0xFF08090C))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            VShapedPanel(
                profiles = getDefaultLayoutProfiles(),
                activeProfileName = "Standard Elite",
                onProfileSelected = {},
                onPlayClick = {}
            )
        }
    }

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF08090C, widthDp = 700, heightDp = 100)
    @Composable
    fun hudTopBarPreview() {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF08090C))
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            HudTopBar(
                profileName = "Cyberpunk Esports Elite",
                isDefault = false,
                hasUnsavedChanges = true,
                onBack = {},
                onOpenPalette = {},
                onSave = {}
            )
        }
    }
}
