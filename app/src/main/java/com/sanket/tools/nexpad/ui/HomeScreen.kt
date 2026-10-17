package com.sanket.tools.nexpad.ui

import com.sanket.tools.nexpad.ui.studio.model.ButtonStudioMode

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
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.ui.AppNavigator
import com.sanket.tools.nexpad.ui.components.badge.HeaderStatusPill
import com.sanket.tools.nexpad.ui.components.button.CommandButton
import com.sanket.tools.nexpad.ui.components.common.NexpadTitle
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
    viewModel: GamepadViewModel,
    navigationViewModel: NavigationViewModel? = null
) {
    val isConnected by viewModel.isConnected.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val discoveredServers by viewModel.discoveredServers.collectAsState()
    val connectionStats by viewModel.connectionStats.collectAsState()
    val isAoaAttached by viewModel.isAoaAttached.collectAsState()
    val isUsbCableConnected by viewModel.isUsbCableConnected.collectAsState()
    val isAdbAvailable by viewModel.isAdbAvailable.collectAsState()
    val adbServerName by viewModel.adbServerName.collectAsState()
    val profiles by layoutManager.profilesFlow.collectAsState()
    val activeProfileName by layoutManager.activeProfileNameFlow.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(connectionStatus) {
        val lower = connectionStatus.lowercase()
        if (lower.contains("failed") || lower.contains("timeout") || lower.contains("error")) {
            snackbarHostState.showSnackbar(
                message = connectionStatus,
                withDismissAction = true
            )
        }
    }

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

    HomeScreenContent(
        isConnected = isConnected,
        connectionStatus = connectionStatus,
        snackbarHostState = snackbarHostState,
        discoveredServers = discoveredServers,
        connectionStats = connectionStats,
        isAoaAttached = isAoaAttached && isUsbCableConnected,
        isUsbCableConnected = isUsbCableConnected,
        isAdbAvailable = isAdbAvailable,
        adbServerName = adbServerName,
        profiles = profiles,
        activeProfileName = activeProfileName,
        onProfileSelected = { selected ->
            if (!selected.name.equals(activeProfileName, ignoreCase = true)) {
                layoutManager.setActiveProfile(selected.name)
            }
        },
        onPlayClick = { selectedProfile ->
            layoutManager.setActiveProfile(selectedProfile.name)
            navigationViewModel?.startGamepadSession(selectedProfile.name)
            navController.navigate(Route.Gamepad(layoutProfileName = selectedProfile.name))
        },
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
        onOpenConnectionHub = { navController.navigate(Route.Connections) },
        onNavigateVirtualController = { navController.navigate(Route.VirtualController) },
        onNavigateConnections = { navController.navigate(Route.Connections) },
        onNavigateButtonStudio = { navController.navigate(Route.ButtonStudio(mode = ButtonStudioMode.VIEWER)) },
        onNavigateSettings = { navController.navigate(Route.Settings) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    isConnected: Boolean = false,
    discoveredServers: List<com.sanket.tools.nexpad.network.DiscoveredServer> = emptyList(),
    connectionStats: com.sanket.tools.nexpad.viewmodel.ConnectionStats = com.sanket.tools.nexpad.viewmodel.ConnectionStats(),
    isAoaAttached: Boolean = false,
    isUsbCableConnected: Boolean = false,
    isAdbAvailable: Boolean = false,
    adbServerName: String? = null,
    profiles: List<com.sanket.tools.nexpad.model.LayoutProfile> = com.sanket.tools.nexpad.model.getDefaultLayoutProfiles(),
    activeProfileName: String = com.sanket.tools.nexpad.model.DefaultProfileType.DEFAULT.profileName,
    onProfileSelected: (com.sanket.tools.nexpad.model.LayoutProfile) -> Unit = {},
    onPlayClick: (com.sanket.tools.nexpad.model.LayoutProfile) -> Unit = {},
    onConnectServer: (com.sanket.tools.nexpad.network.DiscoveredServer) -> Unit = {},
    onConnectAoa: () -> Unit = {},
    onConnectAdb: () -> Unit = {},
    onDisconnectClick: () -> Unit = {},
    onOpenConnectionHub: () -> Unit = {},
    onNavigateVirtualController: () -> Unit = {},
    onNavigateConnections: () -> Unit = {},
    onNavigateButtonStudio: () -> Unit = {},
    onNavigateSettings: () -> Unit = {},
    connectionStatus: String = "Disconnected",
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val windowLayout = remember(configuration.screenWidthDp, configuration.screenHeightDp) {
        adaptiveLayoutSpec(configuration.screenWidthDp.dp, configuration.screenHeightDp.dp)
    }
    val topTitleSize = when (windowLayout.deviceType) {
        com.sanket.tools.nexpad.ui.layout.AdaptiveDeviceType.Tablet -> 44.sp
        com.sanket.tools.nexpad.ui.layout.AdaptiveDeviceType.Foldable -> 38.sp
        com.sanket.tools.nexpad.ui.layout.AdaptiveDeviceType.MidPhone -> 34.sp
        com.sanket.tools.nexpad.ui.layout.AdaptiveDeviceType.SmallPhone -> 28.sp
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    NexpadTitle(
                        fontSize = topTitleSize,
                        letterSpacing = 0.8.sp
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
                        HeaderRow(
                            isConnected = isConnected,
                            connectionStatus = connectionStatus,
                            layout = layout
                        )

                        VShapedPanel(
                            layout = layout,
                            profiles = profiles,
                            activeProfileName = activeProfileName,
                            onProfileSelected = onProfileSelected,
                            onPlayClick = onPlayClick,
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
                        item(key = "title_online_device") {
                            Text(
                                text = "Online Device",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontSize = layout.sectionTitleFontSize
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        item(key = "card_device_hero") {
                            DeviceHeroCard(
                                isConnected = isConnected,
                                servers = discoveredServers,
                                stats = connectionStats,
                                isAoaAttached = isAoaAttached && isUsbCableConnected,
                                isAdbAvailable = isAdbAvailable,
                                adbServerName = adbServerName,
                                onConnectServer = onConnectServer,
                                onConnectAoa = onConnectAoa,
                                onConnectAdb = onConnectAdb,
                                onDisconnectClick = onDisconnectClick,
                                onOpenConnectionHub = onOpenConnectionHub,
                                layout = layout
                            )
                        }

                        item(key = "title_command_center") {
                            Text(
                                text = "Command Center",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontSize = layout.sectionTitleFontSize
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        item(key = "buttons_command_center") {
                            CommandCenterButtons(
                                onNavigateVirtualController = onNavigateVirtualController,
                                onNavigateConnections = onNavigateConnections,
                                onNavigateButtonStudio = onNavigateButtonStudio,
                                onNavigateSettings = onNavigateSettings,
                                layout = layout
                            )
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
                    HeaderRow(
                        isConnected = isConnected,
                        connectionStatus = connectionStatus,
                        layout = layout
                    )

                    VShapedPanel(
                        layout = layout,
                        profiles = profiles,
                        activeProfileName = activeProfileName,
                        onProfileSelected = onProfileSelected,
                        onPlayClick = onPlayClick
                    )

                    Text(
                        text = "Online Device",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = layout.sectionTitleFontSize
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    DeviceHeroCard(
                        isConnected = isConnected,
                        servers = discoveredServers,
                        stats = connectionStats,
                        isAoaAttached = isAoaAttached && isUsbCableConnected,
                        isAdbAvailable = isAdbAvailable,
                        adbServerName = adbServerName,
                        onConnectServer = onConnectServer,
                        onConnectAoa = onConnectAoa,
                        onConnectAdb = onConnectAdb,
                        onDisconnectClick = onDisconnectClick,
                        onOpenConnectionHub = onOpenConnectionHub,
                        layout = layout
                    )

                    Text(
                        text = "Command Center",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = layout.sectionTitleFontSize
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    CommandCenterButtons(
                        onNavigateVirtualController = onNavigateVirtualController,
                        onNavigateConnections = onNavigateConnections,
                        onNavigateButtonStudio = onNavigateButtonStudio,
                        onNavigateSettings = onNavigateSettings,
                        layout = layout
                    )
                }
            }
        }
    }
}

@Composable
fun CommandCenterButtons(
    onNavigateVirtualController: () -> Unit = {},
    onNavigateConnections: () -> Unit = {},
    onNavigateButtonStudio: () -> Unit = {},
    onNavigateSettings: () -> Unit = {},
    modifier: Modifier = Modifier,
    layout: com.sanket.tools.nexpad.ui.layout.AdaptiveLayoutSpec? = null
) {
    val btnHeight = when (layout?.deviceType) {
        com.sanket.tools.nexpad.ui.layout.AdaptiveDeviceType.Tablet -> 144.dp
        com.sanket.tools.nexpad.ui.layout.AdaptiveDeviceType.Foldable -> 126.dp
        com.sanket.tools.nexpad.ui.layout.AdaptiveDeviceType.MidPhone -> 112.dp
        com.sanket.tools.nexpad.ui.layout.AdaptiveDeviceType.SmallPhone -> 98.dp
        null -> 112.dp
    }
    val btnIconSize = when (layout?.deviceType) {
        com.sanket.tools.nexpad.ui.layout.AdaptiveDeviceType.Tablet -> 40.dp
        com.sanket.tools.nexpad.ui.layout.AdaptiveDeviceType.Foldable -> 35.dp
        com.sanket.tools.nexpad.ui.layout.AdaptiveDeviceType.MidPhone -> 32.dp
        com.sanket.tools.nexpad.ui.layout.AdaptiveDeviceType.SmallPhone -> 28.dp
        null -> 32.dp
    }
    val btnFontSize = when (layout?.deviceType) {
        com.sanket.tools.nexpad.ui.layout.AdaptiveDeviceType.Tablet -> 15.5.sp
        com.sanket.tools.nexpad.ui.layout.AdaptiveDeviceType.Foldable -> 14.sp
        com.sanket.tools.nexpad.ui.layout.AdaptiveDeviceType.MidPhone -> 13.sp
        com.sanket.tools.nexpad.ui.layout.AdaptiveDeviceType.SmallPhone -> 11.5.sp
        null -> 12.5.sp
    }
    val spacing = layout?.cardSpacing ?: 16.dp

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing),
            modifier = Modifier.fillMaxWidth()
        ) {
            CommandButton(
                label = "Virtual Controller",
                icon = Icons.Rounded.SportsEsports,
                iconColor = MaterialTheme.colorScheme.primary,
                onClick = onNavigateVirtualController,
                modifier = Modifier.weight(1f),
                height = btnHeight,
                iconSize = btnIconSize,
                labelFontSize = btnFontSize
            )
            CommandButton(
                label = "Connections",
                icon = Icons.Rounded.Hub,
                iconColor = NeonPalette.Cyan,
                onClick = onNavigateConnections,
                modifier = Modifier.weight(1f),
                height = btnHeight,
                iconSize = btnIconSize,
                labelFontSize = btnFontSize
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing),
            modifier = Modifier.fillMaxWidth()
        ) {
            CommandButton(
                label = "Button Studio",
                icon = Icons.Rounded.Palette,
                iconColor = NeonPalette.Purple,
                onClick = onNavigateButtonStudio,
                modifier = Modifier.weight(1f),
                height = btnHeight,
                iconSize = btnIconSize,
                labelFontSize = btnFontSize
            )
            CommandButton(
                label = "Settings",
                icon = Icons.Rounded.Settings,
                iconColor = MaterialTheme.colorScheme.primaryContainer,
                onClick = onNavigateSettings,
                modifier = Modifier.weight(1f),
                height = btnHeight,
                iconSize = btnIconSize,
                labelFontSize = btnFontSize
            )
        }
    }
}

@Composable
fun HeaderRow(
    isConnected: Boolean,
    connectionStatus: String? = null,
    modifier: Modifier = Modifier,
    layout: com.sanket.tools.nexpad.ui.layout.AdaptiveLayoutSpec? = null
) {
    val titleFontSize = layout?.titleFontSize ?: 24.sp
    val subtitleFontSize = layout?.subtitleFontSize ?: 11.5.sp
    val badgeFontSize = layout?.badgeFontSize ?: 11.sp

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column {
            Text(
                text = "Ready to Play",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = titleFontSize
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            if (!isConnected && connectionStatus != null) {
                val lower = connectionStatus.lowercase()
                if (lower.contains("failed") || lower.contains("timeout") || lower.contains("error")) {
                    Text(
                        text = connectionStatus,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = subtitleFontSize
                        ),
                        color = NeonPalette.ErrorRed
                    )
                }
            }
        }

        HeaderStatusPill(
            isConnected = isConnected,
            fontSize = badgeFontSize
        )
    }
}
