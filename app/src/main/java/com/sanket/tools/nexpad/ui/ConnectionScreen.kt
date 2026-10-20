package com.sanket.tools.nexpad.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.sanket.tools.nexpad.ui.AppNavigator
import com.sanket.tools.nexpad.ui.components.badge.HeaderStatusPill
import com.sanket.tools.nexpad.ui.components.common.NexpadTopAppBar
import com.sanket.tools.nexpad.ui.components.common.NxIconBox
import com.sanket.tools.nexpad.ui.components.connection.ActiveSessionCard
import com.sanket.tools.nexpad.ui.components.connection.ConnectionHubStatusCard
import com.sanket.tools.nexpad.ui.components.connection.ConnectionTransportsContent
import com.sanket.tools.nexpad.ui.components.connection.ManualIpCard
import com.sanket.tools.nexpad.ui.components.effects.CyberGrid
import com.sanket.tools.nexpad.ui.components.effects.ScanLine
import com.sanket.tools.nexpad.ui.layout.AdaptiveLayoutSpec
import com.sanket.tools.nexpad.ui.layout.adaptiveLayoutSpec
import com.sanket.tools.nexpad.ui.theme.NeonPalette
import com.sanket.tools.nexpad.ui.components.common.NxOutlinedButton
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel
import androidx.core.net.toUri

const val NEXPAD_DESKTOP_DOWNLOAD_URL = "https://github.com/parmarsanket/NEXPAD-Desktop"

@Composable
fun ConnectionScreen(
    navController: AppNavigator,
    viewModel: GamepadViewModel
) {
    val context = LocalContext.current
    val isConnected by viewModel.isConnected.collectAsState()
    val connectionStats by viewModel.connectionStats.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val discoveredServers by viewModel.discoveredServers.collectAsState()
    val isAoaAttached by viewModel.isAoaAttached.collectAsState()
    val isUsbCableConnected by viewModel.isUsbCableConnected.collectAsState()
    val isAdbAvailable by viewModel.isAdbAvailable.collectAsState()
    val adbServerName by viewModel.adbServerName.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopDiscovery()
        }
    }

    LaunchedEffect(connectionStatus) {
        val lower = connectionStatus.lowercase()
        if (lower.contains("failed") || lower.contains("timeout") || lower.contains("error")) {
            snackbarHostState.showSnackbar(
                message = connectionStatus,
                withDismissAction = true
            )
        }
    }

    var pairedDevices by remember { mutableStateOf<List<android.bluetooth.BluetoothDevice>>(emptyList()) }

    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pairedDevices = viewModel.getPairedBluetoothDevices()
        } else {
            Toast.makeText(context, "Bluetooth permission required for pairing list", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.checkAoaAccessory()
        if (!isConnected) {
            viewModel.startDiscovery()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val hasPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
            if (hasPerm) {
                pairedDevices = viewModel.getPairedBluetoothDevices()
            }
        } else {
            pairedDevices = viewModel.getPairedBluetoothDevices()
        }
    }

    val nexpadPrefs = remember(context) {
        com.sanket.tools.nexpad.storage.NexpadPreferences.getInstance(context)
    }
    var manualIp by remember { mutableStateOf(nexpadPrefs.lastManualIp) }
    var manualPort by remember(nexpadPrefs.lastManualPort) { mutableStateOf(nexpadPrefs.lastManualPort) }

    ConnectionScreenContent(
        isConnected = isConnected,
        connectionStats = connectionStats,
        discoveredServers = discoveredServers,
        pairedDevices = pairedDevices,
        isAoaAttached = isAoaAttached,
        isUsbCableConnected = isUsbCableConnected,
        isAdbAvailable = isAdbAvailable,
        adbServerName = adbServerName,
        isUsbDebuggingEnabled = viewModel.isUsbDebuggingEnabled(),
        snackbarHostState = snackbarHostState,
        manualIp = manualIp,
        manualPort = manualPort,
        onManualIpChange = { newIp ->
            manualIp = newIp
            nexpadPrefs.lastManualIp = newIp
        },
        onManualPortChange = { newPort ->
            val digitsOnly = newPort.filter { it.isDigit() }.take(5)
            manualPort = digitsOnly
            nexpadPrefs.lastManualPort = digitsOnly
        },
        onConnectManualIp = {
            if (manualIp.isNotBlank()) {
                val port = manualPort.toIntOrNull() ?: nexpadPrefs.defaultCommunicationPort
                viewModel.connect(manualIp.trim(), port, "Direct IP")
            }
        },
        onConnectServer = { ip, port, name ->
            viewModel.connect(ip, port, name)
        },
        onConnectAoa = { viewModel.switchToAoaConnection() },
        onConnectAdb = { viewModel.switchToAdbConnection() },
        onConnectBluetooth = { address, name ->
            viewModel.switchToBluetoothConnection(address, name)
        },
        onRescanNetwork = { viewModel.startDiscovery() },
        onOpenDeveloperSettings = {
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                context.startActivity(intent)
            } catch (_: Exception) {
                Toast.makeText(context, "Developer settings not found", Toast.LENGTH_SHORT).show()
            }
        },
        onOpenTetheringSettings = {
            try {
                val intent = Intent().apply {
                    setClassName("com.android.settings", "com.android.settings.TetherSettings")
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                Toast.makeText(context, "Cannot open tethering settings directly", Toast.LENGTH_SHORT).show()
            }
        },
        onReloadBluetoothDevices = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val hasPerm = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
                if (!hasPerm) {
                    bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                } else {
                    pairedDevices = viewModel.getPairedBluetoothDevices()
                }
            } else {
                pairedDevices = viewModel.getPairedBluetoothDevices()
            }
        },
        onOpenBluetoothSettings = {
            try {
                context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            } catch (_: Exception) {}
        },
        onDisconnect = { viewModel.disconnect() },
        onDownloadDesktop = {
            try {
                val intent = Intent(Intent.ACTION_VIEW, NEXPAD_DESKTOP_DOWNLOAD_URL.toUri()).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                Toast.makeText(context, "Could not open browser", Toast.LENGTH_SHORT).show()
            }
        },
        onBack = { navController.popBackStack() }
    )
}

@Composable
fun ConnectionScreenContent(
    isConnected: Boolean = false,
    connectionStats: com.sanket.tools.nexpad.viewmodel.ConnectionStats = com.sanket.tools.nexpad.viewmodel.ConnectionStats(),
    discoveredServers: List<com.sanket.tools.nexpad.network.DiscoveredServer> = emptyList(),
    pairedDevices: List<android.bluetooth.BluetoothDevice> = emptyList(),
    isAoaAttached: Boolean = false,
    isUsbCableConnected: Boolean = false,
    isAdbAvailable: Boolean = false,
    adbServerName: String? = null,
    isUsbDebuggingEnabled: Boolean = false,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onConnectServer: (String, Int, String) -> Unit = { _, _, _ -> },
    onConnectAoa: () -> Unit = {},
    onConnectAdb: () -> Unit = {},
    onConnectBluetooth: (String, String) -> Unit = { _, _ -> },
    onRescanNetwork: () -> Unit = {},
    onOpenDeveloperSettings: () -> Unit = {},
    onOpenTetheringSettings: () -> Unit = {},
    onReloadBluetoothDevices: () -> Unit = {},
    onOpenBluetoothSettings: () -> Unit = {},
    onDisconnect: () -> Unit = {},
    onDownloadDesktop: () -> Unit = {},
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    manualIp: String = "",
    manualPort: String = "9999",
    showManualIpCard: Boolean = true,
    onManualIpChange: (String) -> Unit = {},
    onManualPortChange: (String) -> Unit = {},
    onToggleManualIpCard: () -> Unit = {},
    onConnectManualIp: () -> Unit = {}
) {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val windowLayout = remember(windowInfo.containerSize, density) {
        val (widthDp, heightDp) = with(density) {
            windowInfo.containerSize.width.toDp() to windowInfo.containerSize.height.toDp()
        }
        adaptiveLayoutSpec(widthDp, heightDp)
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            NexpadTopAppBar(
                title = "CONNECTION HUB",
                subtitle = "Hardware Transports & Link Engine",
                onBack = onBack,
                titleFontSize = windowLayout.titleFontSize,
                subtitleFontSize = windowLayout.subtitleFontSize,
                actions = {
                    HeaderStatusPill(
                        isConnected = isConnected,
                        disconnectedText = "Standby",
                        fontSize = windowLayout.badgeFontSize,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            CyberGrid(modifier = Modifier.matchParentSize())
            ScanLine(modifier = Modifier.matchParentSize())

            val layout = windowLayout

            val transportsComposable = @Composable {
                ConnectionTransportsContent(
                    discoveredServers = discoveredServers,
                    pairedDevices = pairedDevices,
                    isAoaAttached = isAoaAttached,
                    isUsbCableConnected = isUsbCableConnected,
                    isAdbAvailable = isAdbAvailable,
                    adbServerName = adbServerName,
                    isUsbDebuggingEnabled = isUsbDebuggingEnabled,
                    onConnectServer = onConnectServer,
                    onConnectAoa = onConnectAoa,
                    onConnectAdb = onConnectAdb,
                    onConnectBluetooth = onConnectBluetooth,
                    onRescanNetwork = onRescanNetwork,
                    onOpenDeveloperSettings = onOpenDeveloperSettings,
                    onOpenTetheringSettings = onOpenTetheringSettings,
                    onReloadBluetoothDevices = onReloadBluetoothDevices,
                    onOpenBluetoothSettings = onOpenBluetoothSettings,
                    onDownloadDesktop = onDownloadDesktop,
                    layout = layout
                )
            }

            if (layout.useTwoPaneLayout) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            horizontal = layout.horizontalPadding,
                            vertical = layout.verticalPadding
                        ),
                    horizontalArrangement = Arrangement.spacedBy(layout.paneSpacing)
                ) {
                    // Left Sticky Pane
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .imePadding()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (isConnected) {
                            ActiveSessionCard(
                                stats = connectionStats,
                                onDisconnect = onDisconnect
                            )
                        } else {
                            ConnectionHubStatusCard(
                                isUsbCableConnected = isUsbCableConnected,
                                isAoaAttached = isAoaAttached,
                                isAdbAvailable = isAdbAvailable,
                                onRescan = onRescanNetwork,
                                onOpenBluetoothSettings = onOpenBluetoothSettings,
                                layout = layout
                            )
                        }
                    }

                    // Right Scrollable Pane
                    Column(
                        modifier = Modifier
                            .weight(1.25f)
                            .fillMaxHeight()
                            .imePadding()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        transportsComposable()
                        ManualIpCard(
                            manualIp = manualIp,
                            manualPort = manualPort,
                            onIpChange = onManualIpChange,
                            onPortChange = onManualPortChange,
                            onConnect = onConnectManualIp,
                            layout = layout
                        )
                        ConnectionDesktopDownloadCard(
                            onDownload = onDownloadDesktop,
                            layout = layout
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                }
            } else {
                // Single Column (Portrait)
                Column(
                    modifier = Modifier
                        .widthIn(max = layout.formMaxWidth)
                        .fillMaxWidth()
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(
                            horizontal = layout.horizontalPadding,
                            vertical = layout.verticalPadding
                        ),
                    verticalArrangement = Arrangement.spacedBy(layout.contentSpacing)
                ) {
                    if (isConnected) {
                        ActiveSessionCard(
                            stats = connectionStats,
                            onDisconnect = onDisconnect
                        )
                    } else {
                        ConnectionHubStatusCard(
                            isUsbCableConnected = isUsbCableConnected,
                            isAoaAttached = isAoaAttached,
                            isAdbAvailable = isAdbAvailable,
                            onRescan = onRescanNetwork,
                            onOpenBluetoothSettings = onOpenBluetoothSettings,
                            layout = layout
                        )
                    }
                    transportsComposable()
                    ManualIpCard(
                        manualIp = manualIp,
                        manualPort = manualPort,
                        onIpChange = onManualIpChange,
                        onPortChange = onManualPortChange,
                        onConnect = onConnectManualIp,
                        layout = layout
                    )
                    ConnectionDesktopDownloadCard(
                        onDownload = onDownloadDesktop,
                        layout = layout
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun ConnectionDesktopDownloadCard(
    onDownload: () -> Unit,
    modifier: Modifier = Modifier,
    layout: AdaptiveLayoutSpec? = null
) {
    val cardPadding = layout?.cardPadding ?: 16.dp
    val iconBoxSize = layout?.iconBoxSize ?: 42.dp
    val iconSize = layout?.iconSize ?: 22.dp
    val titleFontSize = layout?.cardTitleFontSize ?: 15.sp
    val bodyFontSize = layout?.bodyFontSize ?: 11.5.sp
    val buttonHeight = layout?.buttonHeight ?: 36.dp
    val buttonFontSize = layout?.buttonFontSize ?: 12.sp

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = NeonPalette.CardSurface.copy(alpha = 0.85f),
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                listOf(
                    NeonPalette.Cyan.copy(alpha = 0.45f),
                    NeonPalette.Purple.copy(alpha = 0.25f),
                    Color.White.copy(alpha = 0.05f)
                )
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(cardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
//
            NxIconBox(
                iconBoxSize = iconBoxSize,
                iconSize = iconSize,
                imageVector = Icons.Rounded.DesktopWindows,
                color = NeonPalette.Cyan
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Need PC Server?",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = titleFontSize,
                        letterSpacing = 0.5.sp
                    ),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Download NEXPAD Desktop companion for Windows PC",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = NeonPalette.SlateText,
                        fontSize = bodyFontSize
                    ),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            NxOutlinedButton(
                onClick = onDownload,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = NeonPalette.Cyan,
                    containerColor = NeonPalette.Cyan.copy(alpha = 0.08f)
                ),
                borderColors = NeonPalette.Cyan.copy(alpha = 0.6f),
                modifier = Modifier.height(buttonHeight),
                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                iconModifier = Modifier.size(iconSize.coerceAtMost(16.dp)),
                text = "Download",
                style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = buttonFontSize
                    )
            )
        }
    }
}

