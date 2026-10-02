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
import com.sanket.tools.nexpad.network.DiscoveredServer
import com.sanket.tools.nexpad.ui.ConnectionScreenContent
import com.sanket.tools.nexpad.ui.components.connection.ActiveSessionCard
import com.sanket.tools.nexpad.ui.components.connection.ConnectionHubStatusCard
import com.sanket.tools.nexpad.ui.components.connection.ConnectionTransportsContent
import com.sanket.tools.nexpad.ui.theme.NEXPADTheme
import com.sanket.tools.nexpad.viewmodel.ConnectionStats
import com.sanket.tools.nexpad.viewmodel.ConnectionType

/**
 * Compose Preview Screenshot Tests for [ConnectionScreenContent] and its relative components:
 * - Full Connection Screen in Portrait (Idle & Connected)
 * - Full Connection Screen in Two-Pane Landscape mode
 * - Relative components: ActiveSessionCard (Wi-Fi & USB AOA), ConnectionHubStatusCard, ConnectionTransportsContent
 */
class ConnectionScreenScreenshotTest {

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF08090C, widthDp = 390, heightDp = 844)
    @Composable
    fun connectionScreenIdlePortraitPreview() {
        val server = DiscoveredServer(
            name = "DESKTOP-GAMING",
            ipAddress = "192.168.1.150",
            port = 9999,
            isUsbTethering = false
        )
        NEXPADTheme {
            ConnectionScreenContent(
                isConnected = false,
                discoveredServers = listOf(server),
                isAoaAttached = false,
                isUsbCableConnected = true,
                isAdbAvailable = true,
                adbServerName = "DESKTOP-ADB-BRIDGE",
                isUsbDebuggingEnabled = true
            )
        }
    }

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF08090C, widthDp = 390, heightDp = 844)
    @Composable
    fun connectionScreenConnectedPortraitPreview() {
        val stats = ConnectionStats(
            transport = ConnectionType.WIFI,
            serverDeviceName = "NEXPAD-GAMING-RIG",
            signalLevel = 4,
            signalDbm = -38,
            rxLinkSpeedMbps = 866,
            txLinkSpeedMbps = 866,
            latencyMs = 1L,
            jitterMs = 0.2f
        )
        NEXPADTheme {
            ConnectionScreenContent(
                isConnected = true,
                connectionStats = stats,
                isAoaAttached = false,
                isUsbCableConnected = false,
                isAdbAvailable = false
            )
        }
    }

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF08090C, widthDp = 840, heightDp = 390)
    @Composable
    fun connectionScreenTwoPaneLandscapePreview() {
        val stats = ConnectionStats(
            transport = ConnectionType.USB,
            serverDeviceName = "NEXPAD-WINUSB-RIG",
            latencyMs = 0L,
            jitterMs = 0.05f
        )
        NEXPADTheme {
            ConnectionScreenContent(
                isConnected = true,
                connectionStats = stats,
                isAoaAttached = true,
                isUsbCableConnected = true,
                isAdbAvailable = true
            )
        }
    }

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF08090C, widthDp = 360, heightDp = 220)
    @Composable
    fun activeSessionCardWifiPreview() {
        val stats = ConnectionStats(
            transport = ConnectionType.WIFI,
            serverDeviceName = "NEXPAD-GAMING-RIG",
            signalLevel = 4,
            signalDbm = -40,
            rxLinkSpeedMbps = 866,
            txLinkSpeedMbps = 866,
            latencyMs = 1L,
            jitterMs = 0.2f
        )
        NEXPADTheme {
            Box(
                modifier = Modifier
                    .background(Color(0xFF08090C))
                    .padding(16.dp)
            ) {
                ActiveSessionCard(
                    stats = stats,
                    onDisconnect = {}
                )
            }
        }
    }

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF08090C, widthDp = 360, heightDp = 220)
    @Composable
    fun activeSessionCardUsbAoaPreview() {
        val stats = ConnectionStats(
            transport = ConnectionType.USB,
            serverDeviceName = "NEXPAD-DIRECT-USB",
            latencyMs = 0L,
            jitterMs = 0.08f
        )
        NEXPADTheme {
            Box(
                modifier = Modifier
                    .background(Color(0xFF08090C))
                    .padding(16.dp)
            ) {
                ActiveSessionCard(
                    stats = stats,
                    onDisconnect = {}
                )
            }
        }
    }

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF08090C, widthDp = 360, heightDp = 180)
    @Composable
    fun connectionHubStatusCardPreview() {
        NEXPADTheme {
            Box(
                modifier = Modifier
                    .background(Color(0xFF08090C))
                    .padding(16.dp)
            ) {
                ConnectionHubStatusCard(
                    isUsbCableConnected = true,
                    isAoaAttached = true,
                    isAdbAvailable = true,
                    onRescan = {},
                    onOpenBluetoothSettings = {}
                )
            }
        }
    }

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF08090C, widthDp = 360, heightDp = 400)
    @Composable
    fun connectionTransportsContentPreview() {
        val server = DiscoveredServer(
            name = "ALEX-GAMING-PC",
            ipAddress = "192.168.1.120",
            port = 9999,
            isUsbTethering = false
        )
        NEXPADTheme {
            Box(
                modifier = Modifier
                    .background(Color(0xFF08090C))
                    .padding(16.dp)
            ) {
                ConnectionTransportsContent(
                    discoveredServers = listOf(server),
                    pairedDevices = emptyList(),
                    manualIp = "192.168.1.100",
                    manualPort = "9999",
                    showManualIpCard = true,
                    isAoaAttached = true,
                    isUsbCableConnected = true,
                    isAdbAvailable = true,
                    adbServerName = "ALEX-ADB-BRIDGE",
                    isUsbDebuggingEnabled = true,
                    onManualIpChange = {},
                    onManualPortChange = {},
                    onToggleManualIpCard = {},
                    onConnectServer = { _, _, _ -> },
                    onConnectAoa = {},
                    onConnectAdb = {},
                    onConnectBluetooth = { _, _ -> },
                    onRescanNetwork = {},
                    onOpenDeveloperSettings = {},
                    onOpenTetheringSettings = {},
                    onReloadBluetoothDevices = {},
                    onOpenBluetoothSettings = {}
                )
            }
        }
    }
}
