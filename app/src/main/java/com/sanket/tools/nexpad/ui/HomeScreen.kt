package com.sanket.tools.nexpad.ui

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DashboardCustomize
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.ui.AppNavigator
import com.sanket.tools.nexpad.ui.components.badge.HeaderStatusPill
import com.sanket.tools.nexpad.ui.components.button.CommandButton
import com.sanket.tools.nexpad.ui.components.effects.CyberGrid
import com.sanket.tools.nexpad.ui.components.effects.ScanLine
import com.sanket.tools.nexpad.ui.components.home.DeviceHeroCard
import com.sanket.tools.nexpad.ui.components.home.VShapedPanel
import com.sanket.tools.nexpad.ui.theme.NeonPalette
import com.sanket.tools.nexpad.utils.LayoutManager
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyColumn
import com.sanket.tools.nexpad.ui.layout.adaptiveLayoutSpec

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: AppNavigator,
    layoutManager: LayoutManager,
    viewModel: GamepadViewModel
) {
    val scrollState = rememberScrollState()

    val isConnected by viewModel.isConnected.collectAsState()
    val discoveredServers by viewModel.discoveredServers.collectAsState()
    val connectionStats by viewModel.connectionStats.collectAsState()
    val isAoaAttached by viewModel.isAoaAttached.collectAsState()
    val isUsbCableConnected by viewModel.isUsbCableConnected.collectAsState()
    val isAdbAvailable by viewModel.isAdbAvailable.collectAsState()
    val adbServerName by viewModel.adbServerName.collectAsState()
    val profiles by layoutManager.profilesFlow.collectAsState()
    val activeProfile = layoutManager.getActiveProfile()

    LaunchedEffect(isConnected) {
        if (!isConnected) {
            viewModel.startDiscovery()
        } else {
            viewModel.stopDiscovery()
        }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.stopDiscovery() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "NEXPAD",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.onBackground,
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.secondaryContainer,
                                    MaterialTheme.colorScheme.tertiaryContainer,
                                    MaterialTheme.colorScheme.tertiary
                                )
                            )
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val layout = adaptiveLayoutSpec(maxWidth, maxHeight)

            CyberGrid(
                modifier = Modifier.matchParentSize()
            )

            ScanLine(
                modifier = Modifier.matchParentSize()
            )

            if (layout.useTwoPaneLayout) {
                // ─────────────────────────────────────────────────────────────
                // LANDSCAPE MODE (Two-Pane)
                // Left Pane: Sticky "Ready to Play" Hero Station
                // Right Pane: Independently Scrollable LazyColumn for Telemetry & Controls
                // ─────────────────────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = layout.horizontalPadding, vertical = layout.verticalPadding),
                    horizontalArrangement = Arrangement.spacedBy(layout.paneSpacing)
                ) {
                    // Sticky Left Station
                    Column(
                        modifier = Modifier
                            .weight(1.05f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(layout.contentSpacing)
                    ) {
                        HeaderRow(isConnected = isConnected)

                        VShapedPanel(
                            profiles = profiles,
                            activeProfileName = activeProfile.name,
                            onProfileSelected = { selected ->
                                if (selected.name != activeProfile.name) {
                                    layoutManager.setActiveProfile(selected.name)
                                }
                            },
                            onPlayClick = { navController.navigate("gamepad") },
                            isCompact = layout.isShortScreen,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        )
                    }

                    // Right Scrollable Pane
                    LazyColumn(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        item {
                            Text(
                                text = "Online Device",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        item {
                            DeviceHeroCard(
                                isConnected = isConnected,
                                servers = discoveredServers,
                                stats = connectionStats,
                                isAoaAttached = isAoaAttached && isUsbCableConnected,
                                isAdbAvailable = isAdbAvailable,
                                adbServerName = adbServerName,
                                onConnectServer = { server ->
                                    Log.d(
                                        "NEXPAD",
                                        "⏱️ [BENCHMARK] User CLICKED Connect (${if (server.isUsbTethering) "USB Tethering" else "Wi-Fi"}) button for ${server.name} (${server.ipAddress}:${server.port})"
                                    )
                                    viewModel.connect(server.ipAddress, server.port, server.name)
                                },
                                onConnectAoa = { viewModel.switchToAoaConnection() },
                                onConnectAdb = { viewModel.switchToAdbConnection() },
                                onDisconnectClick = { viewModel.disconnect() },
                                onOpenConnectionHub = { navController.navigate("connections") }
                            )
                        }

                        item {
                            Text(
                                text = "Command Center",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        item {
                            CommandCenterButtons(navController = navController)
                        }
                    }
                }
            } else {
                // ─────────────────────────────────────────────────────────────
                // PORTRAIT MODE (Single Column)
                // ─────────────────────────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = layout.horizontalPadding, vertical = layout.verticalPadding),
                    verticalArrangement = Arrangement.spacedBy(layout.contentSpacing)
                ) {
                    HeaderRow(isConnected = isConnected)

                    VShapedPanel(
                        profiles = profiles,
                        activeProfileName = activeProfile.name,
                        onProfileSelected = { selected ->
                            if (selected.name != activeProfile.name) {
                                layoutManager.setActiveProfile(selected.name)
                            }
                        },
                        onPlayClick = { navController.navigate("gamepad") }
                    )

                    Text(
                        text = "Online Device",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    DeviceHeroCard(
                        isConnected = isConnected,
                        servers = discoveredServers,
                        stats = connectionStats,
                        isAoaAttached = isAoaAttached && isUsbCableConnected,
                        isAdbAvailable = isAdbAvailable,
                        adbServerName = adbServerName,
                        onConnectServer = { server ->
                            Log.d(
                                "NEXPAD",
                                "⏱️ [BENCHMARK] User CLICKED Connect (${if (server.isUsbTethering) "USB Tethering" else "Wi-Fi"}) button for ${server.name} (${server.ipAddress}:${server.port})"
                            )
                            viewModel.connect(server.ipAddress, server.port, server.name)
                        },
                        onConnectAoa = { viewModel.switchToAoaConnection() },
                        onConnectAdb = { viewModel.switchToAdbConnection() },
                        onDisconnectClick = { viewModel.disconnect() },
                        onOpenConnectionHub = { navController.navigate("connections") }
                    )

                    Text(
                        text = "Command Center",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    CommandCenterButtons(navController = navController)
                }
            }
        }
    }
}

@Composable
private fun CommandCenterButtons(navController: AppNavigator) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            CommandButton(
                label = "Virtual Controller",
                icon = Icons.Rounded.SportsEsports,
                iconColor = MaterialTheme.colorScheme.primary,
                onClick = { navController.navigate("virtual_controller") },
                modifier = Modifier.weight(1f)
            )
            CommandButton(
                label = "Connections",
                icon = Icons.Rounded.Hub,
                iconColor = NeonPalette.Cyan,
                onClick = { navController.navigate("connections") },
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            CommandButton(
                label = "Button Studio",
                icon = Icons.Rounded.Palette,
                iconColor = NeonPalette.Purple,
                onClick = { navController.navigate("button_studio") },
                modifier = Modifier.weight(1f)
            )
            CommandButton(
                label = "HUD Editor",
                icon = Icons.Rounded.DashboardCustomize,
                iconColor = MaterialTheme.colorScheme.secondary,
                onClick = { navController.navigate("editor") },
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            CommandButton(
                label = "Settings",
                icon = Icons.Rounded.Settings,
                iconColor = MaterialTheme.colorScheme.primaryContainer,
                onClick = { navController.navigate("settings") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun HeaderRow(isConnected: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = "Ready to Play",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )

        HeaderStatusPill(
            isConnected = isConnected
        )
    }
}
