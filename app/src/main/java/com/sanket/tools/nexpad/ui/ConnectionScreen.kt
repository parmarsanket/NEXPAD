package com.sanket.tools.nexpad.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.sanket.tools.nexpad.ui.AppNavigator
import com.sanket.tools.nexpad.ui.components.badge.HeaderStatusPill
import com.sanket.tools.nexpad.ui.components.common.NexpadTopAppBar
import com.sanket.tools.nexpad.ui.components.connection.ActiveSessionCard
import com.sanket.tools.nexpad.ui.components.connection.ConnectionHubStatusCard
import com.sanket.tools.nexpad.ui.components.connection.ConnectionTransportsContent
import com.sanket.tools.nexpad.ui.layout.adaptiveLayoutSpec
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel

@Composable
fun ConnectionScreen(
    navController: AppNavigator,
    viewModel: GamepadViewModel
) {
    val context = LocalContext.current
    val isConnected by viewModel.isConnected.collectAsState()
    val connectionStats by viewModel.connectionStats.collectAsState()
    val discoveredServers by viewModel.discoveredServers.collectAsState()
    val isAoaAttached by viewModel.isAoaAttached.collectAsState()
    val isUsbCableConnected by viewModel.isUsbCableConnected.collectAsState()
    val isAdbAvailable by viewModel.isAdbAvailable.collectAsState()
    val adbServerName by viewModel.adbServerName.collectAsState()

    var pairedDevices by remember { mutableStateOf<List<android.bluetooth.BluetoothDevice>>(emptyList()) }
    var manualIp by remember { mutableStateOf("") }
    var manualPort by remember { mutableStateOf("9999") }
    var showManualIpCard by remember { mutableStateOf(false) }

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

    Scaffold(
        topBar = {
            NexpadTopAppBar(
                title = "Connection Hub",
                subtitle = "Transports & Pairing Management",
                onBack = { navController.popBackStack() },
                actions = {
                    HeaderStatusPill(
                        isConnected = isConnected,
                        disconnectedText = "Idle",
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
                .padding(innerPadding)
        ) {
            val layout = adaptiveLayoutSpec(maxWidth, maxHeight)

            val transportsComposable = @Composable {
                ConnectionTransportsContent(
                    discoveredServers = discoveredServers,
                    pairedDevices = pairedDevices,
                    manualIp = manualIp,
                    manualPort = manualPort,
                    showManualIpCard = showManualIpCard,
                    isAoaAttached = isAoaAttached,
                    isUsbCableConnected = isUsbCableConnected,
                    isAdbAvailable = isAdbAvailable,
                    adbServerName = adbServerName,
                    isUsbDebuggingEnabled = viewModel.isUsbDebuggingEnabled(),
                    onManualIpChange = { manualIp = it },
                    onManualPortChange = { manualPort = it },
                    onToggleManualIpCard = { showManualIpCard = !showManualIpCard },
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
                    }
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
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (isConnected) {
                            ActiveSessionCard(
                                stats = connectionStats,
                                onDisconnect = { viewModel.disconnect() }
                            )
                        } else {
                            ConnectionHubStatusCard(
                                isUsbCableConnected = isUsbCableConnected,
                                isAoaAttached = isAoaAttached,
                                isAdbAvailable = isAdbAvailable,
                                onRescan = { viewModel.startDiscovery() },
                                onOpenBluetoothSettings = {
                                    try {
                                        context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                                    } catch (_: Exception) {}
                                }
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
                    }
                }
            } else {
                // Single Column (Portrait)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = layout.horizontalPadding, vertical = layout.verticalPadding),
                    verticalArrangement = Arrangement.spacedBy(layout.contentSpacing)
                ) {
                    if (isConnected) {
                        ActiveSessionCard(
                            stats = connectionStats,
                            onDisconnect = { viewModel.disconnect() }
                        )
                    }
                    transportsComposable()
                }
            }
        }
    }
}
