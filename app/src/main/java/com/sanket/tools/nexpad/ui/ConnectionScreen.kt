package com.sanket.tools.nexpad.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.sanket.tools.nexpad.ui.AppNavigator
import com.sanket.tools.nexpad.ui.components.badge.HeaderStatusPill
import com.sanket.tools.nexpad.ui.components.common.NexpadTopAppBar
import com.sanket.tools.nexpad.ui.components.connection.ActiveSessionCard
import com.sanket.tools.nexpad.ui.components.connection.ConnectionHubStatusCard
import com.sanket.tools.nexpad.ui.components.connection.ConnectionTransportsContent
import com.sanket.tools.nexpad.ui.components.effects.CyberGrid
import com.sanket.tools.nexpad.ui.components.effects.ScanLine
import com.sanket.tools.nexpad.ui.layout.adaptiveLayoutSpec
import com.sanket.tools.nexpad.ui.theme.NeonPalette
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel

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
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(NEXPAD_DESKTOP_DOWNLOAD_URL)).apply {
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
    // Optional compatibility parameters
    manualIp: String = "",
    manualPort: String = "9999",
    showManualIpCard: Boolean = false,
    onManualIpChange: (String) -> Unit = {},
    onManualPortChange: (String) -> Unit = {},
    onToggleManualIpCard: () -> Unit = {}
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            NexpadTopAppBar(
                title = "CONNECTION HUB",
                subtitle = "Hardware Transports & Link Engine",
                onBack = onBack,
                actions = {
                    HeaderStatusPill(
                        isConnected = isConnected,
                        disconnectedText = "Standby",
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            CyberGrid(modifier = Modifier.matchParentSize())
            ScanLine(modifier = Modifier.matchParentSize())

            val layout = adaptiveLayoutSpec(maxWidth, maxHeight)

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
                    onDownloadDesktop = onDownloadDesktop
                )
            }

            if (layout.useTwoPaneLayout) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = layout.horizontalPadding, vertical = layout.verticalPadding),
                    horizontalArrangement = Arrangement.spacedBy(layout.paneSpacing)
                ) {
                    // Left Sticky Pane
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
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
                                onOpenBluetoothSettings = onOpenBluetoothSettings
                            )
                        }
                    }

                    // Right Scrollable Pane
                    Column(
                        modifier = Modifier
                            .weight(1.25f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        transportsComposable()
                        ConnectionDesktopDownloadCard(onDownload = onDownloadDesktop)
                        Spacer(Modifier.height(16.dp))
                    }
                }
            } else {
                // Single Column (Portrait)
                Column(
                    modifier = Modifier
                        .widthIn(max = layout.formMaxWidth)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = layout.horizontalPadding, vertical = layout.verticalPadding),
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
                            onOpenBluetoothSettings = onOpenBluetoothSettings
                        )
                    }
                    transportsComposable()
                    ConnectionDesktopDownloadCard(onDownload = onDownloadDesktop)
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun ConnectionDesktopDownloadCard(
    onDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0F1523).copy(alpha = 0.85f),
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
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(NeonPalette.Cyan.copy(alpha = 0.12f))
                    .border(1.dp, NeonPalette.Cyan.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.DesktopWindows,
                    contentDescription = null,
                    tint = NeonPalette.Cyan,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Need PC Server?",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Download NEXPAD Desktop companion for Windows PC",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF94A3B8),
                        fontSize = 11.5.sp
                    )
                )
            }

            OutlinedButton(
                onClick = onDownload,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = NeonPalette.Cyan,
                    containerColor = NeonPalette.Cyan.copy(alpha = 0.08f)
                ),
                border = BorderStroke(1.dp, NeonPalette.Cyan.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Download",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

