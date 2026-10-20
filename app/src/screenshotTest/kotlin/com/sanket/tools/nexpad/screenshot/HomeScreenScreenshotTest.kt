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
import com.sanket.tools.nexpad.network.DiscoveredServer
import com.sanket.tools.nexpad.ui.CommandCenterButtons
import com.sanket.tools.nexpad.ui.HeaderRow
import com.sanket.tools.nexpad.ui.HomeScreenContent
import com.sanket.tools.nexpad.ui.theme.NEXPADTheme
import com.sanket.tools.nexpad.viewmodel.ConnectionStats
import com.sanket.tools.nexpad.viewmodel.ConnectionType

/**
 * Compose Preview Screenshot Tests for [HomeScreenContent] and its relative components:
 * - Full Home Screen in Portrait mode (Idle & Connected)
 * - Full Home Screen in Landscape Two-Pane mode
 * - Relative components: HeaderRow, CommandCenterButtons
 */
class HomeScreenScreenshotTest {

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF08090C, widthDp = 390, heightDp = 844)
    @Composable
    fun homeScreenPortraitIdlePreview() {
        NEXPADTheme {
            HomeScreenContent(
                isConnected = false,
                discoveredServers = emptyList(),
                connectionStats = ConnectionStats(),
                profiles = getDefaultLayoutProfiles(),
                activeProfileName = "Standard Elite"
            )
        }
    }

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF08090C, widthDp = 390, heightDp = 844)
    @Composable
    fun homeScreenPortraitConnectedPreview() {
        val server = DiscoveredServer(
            name = "NEXPAD-GAMING-RIG",
            ipAddress = "192.168.1.100",
            port = 9999,
            isUsbTethering = false
        )
        val stats = ConnectionStats(
            transport = ConnectionType.WIFI,
            serverDeviceName = "NEXPAD-GAMING-RIG",
            signalLevel = 4,
            signalDbm = -42,
            rxLinkSpeedMbps = 866,
            txLinkSpeedMbps = 866,
            latencyMs = 1L,
            jitterMs = 0.2f
        )
        NEXPADTheme {
            HomeScreenContent(
                isConnected = true,
                discoveredServers = listOf(server),
                connectionStats = stats,
                profiles = getDefaultLayoutProfiles(),
                activeProfileName = "Standard Elite"
            )
        }
    }

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF08090C, widthDp = 840, heightDp = 390)
    @Composable
    fun homeScreenLandscapeTwoPanePreview() {
        val stats = ConnectionStats(
            transport = ConnectionType.USB,
            serverDeviceName = "NEXPAD-GAMING-RIG",
            latencyMs = 0L,
            jitterMs = 0.1f
        )
        NEXPADTheme {
            HomeScreenContent(
                isConnected = true,
                isAoaAttached = true,
                isUsbCableConnected = true,
                connectionStats = stats,
                profiles = getDefaultLayoutProfiles(),
                activeProfileName = "Standard Elite"
            )
        }
    }

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF08090C, widthDp = 360, heightDp = 60)
    @Composable
    fun headerRowConnectedPreview() {
        NEXPADTheme {
            Box(
                modifier = Modifier
                    .background(Color(0xFF08090C))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                HeaderRow(isConnected = true)
            }
        }
    }

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF08090C, widthDp = 360, heightDp = 60)
    @Composable
    fun headerRowIdlePreview() {
        NEXPADTheme {
            Box(
                modifier = Modifier
                    .background(Color(0xFF08090C))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                HeaderRow(isConnected = false)
            }
        }
    }

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF08090C, widthDp = 360, heightDp = 180)
    @Composable
    fun commandCenterButtonsPreview() {
        NEXPADTheme {
            Box(
                modifier = Modifier
                    .background(Color(0xFF08090C))
                    .padding(16.dp)
            ) {
                CommandCenterButtons()
            }
        }
    }
}
