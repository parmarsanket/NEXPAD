package com.sanket.tools.nexpad.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.sanket.tools.nexpad.ui.components.badge.HeaderStatusPill
import com.sanket.tools.nexpad.ui.components.connection.ActiveSessionCard
import com.sanket.tools.nexpad.ui.components.connection.BluetoothDeviceCard
import com.sanket.tools.nexpad.ui.components.connection.ConnectionSection
import com.sanket.tools.nexpad.ui.components.connection.ManualIpCard
import com.sanket.tools.nexpad.ui.components.connection.ServerEntryCard
import com.sanket.tools.nexpad.ui.components.connection.UsbAdbCard
import com.sanket.tools.nexpad.ui.components.connection.UsbAoaCard
import com.sanket.tools.nexpad.ui.components.connection.UsbTetheringCard
import com.sanket.tools.nexpad.ui.theme.NeonPalette
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionScreen(
    navController: NavController,
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
            TopAppBar(
                title = {
                    Text(
                        "Connection Hub",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    HeaderStatusPill(
                        isConnected = isConnected,
                        disconnectedText = "Idle",
                        modifier = Modifier.padding(end = 16.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // -------------------------------------------------------------
            // 1. ACTIVE CONNECTION CARD (Prominently displayed when online)
            // -------------------------------------------------------------
            if (isConnected) {
                ActiveSessionCard(
                    stats = connectionStats,
                    onDisconnect = { viewModel.disconnect() }
                )
            }

            // -------------------------------------------------------------
            // 2. DISCOVERED COMPUTERS (Wi-Fi & USB Tethering)
            // -------------------------------------------------------------
            ConnectionSection(
                icon = Icons.Rounded.Wifi,
                title = "Network Discovery (Wi-Fi / Tethering)",
                subtitle = "Automatic broadcast discovery on local network"
            ) {
                val wifiServers = discoveredServers.filter { !it.isUsbTethering }
                val tetherServers = discoveredServers.filter { it.isUsbTethering }

                if (discoveredServers.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = NeonPalette.DarkCard),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "No computers discovered on LAN yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedButton(
                                onClick = { viewModel.startDiscovery() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonPalette.Cyan),
                                border = BorderStroke(1.dp, NeonPalette.Cyan.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Rescan Network", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                } else {
                    // USB Tethering Servers First
                    tetherServers.forEach { server ->
                        ServerEntryCard(
                            server = server,
                            isTethering = true,
                            onConnect = {
                                Log.d("NEXPAD", "⏱️ [BENCHMARK] User CLICKED Connect (USB Tethering) in ConnectionHub for ${server.name} (${server.ipAddress}:${server.port})")
                                viewModel.connect(server.ipAddress, server.port, server.name)
                            }
                        )
                    }
                    // Wi-Fi Servers
                    wifiServers.forEach { server ->
                        ServerEntryCard(
                            server = server,
                            isTethering = false,
                            onConnect = {
                                Log.d("NEXPAD", "⏱️ [BENCHMARK] User CLICKED Connect (Wi-Fi) in ConnectionHub for ${server.name} (${server.ipAddress}:${server.port})")
                                viewModel.connect(server.ipAddress, server.port, server.name)
                            }
                        )
                    }
                }

                // Manual IP Toggle Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showManualIpCard = !showManualIpCard }) {
                        Icon(
                            if (showManualIpCard) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = null,
                            tint = NeonPalette.Cyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (showManualIpCard) "Hide Manual IP" else "Manual IP Connect",
                            color = NeonPalette.Cyan,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                if (showManualIpCard) {
                    ManualIpCard(
                        manualIp = manualIp,
                        manualPort = manualPort,
                        onIpChange = { manualIp = it },
                        onPortChange = { manualPort = it },
                        onConnect = {
                            val port = manualPort.toIntOrNull() ?: 9999
                            if (manualIp.isNotBlank()) {
                                viewModel.connect(manualIp.trim(), port, "Manual PC")
                            } else {
                                Toast.makeText(context, "Enter a valid IP address", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }

            // -------------------------------------------------------------
            // 3. USB CONNECTIONS (AOA, ADB, Tethering)
            // -------------------------------------------------------------
            ConnectionSection(
                icon = Icons.Rounded.Usb,
                title = "USB Hardware Links",
                subtitle = "Zero-latency physical cable connections"
            ) {
                UsbAoaCard(
                    isAoaAttached = isAoaAttached,
                    isUsbCableConnected = isUsbCableConnected,
                    onConnectAoa = { viewModel.switchToAoaConnection() }
                )

                UsbAdbCard(
                    isUsbCableConnected = isUsbCableConnected,
                    isAdbOn = viewModel.isUsbDebuggingEnabled(),
                    isAdbAvailable = isAdbAvailable,
                    adbServerName = adbServerName,
                    onConnectAdb = { viewModel.switchToAdbConnection() },
                    onOpenDeveloperSettings = {
                        try {
                            val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            Toast.makeText(context, "Developer settings not found", Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                UsbTetheringCard(
                    onOpenTetheringSettings = {
                        try {
                            val intent = Intent().apply {
                                setClassName("com.android.settings", "com.android.settings.TetherSettings")
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            Toast.makeText(context, "Cannot open tethering settings directly", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            // -------------------------------------------------------------
            // 4. BLUETOOTH CLASSIC
            // -------------------------------------------------------------
            ConnectionSection(
                icon = Icons.Rounded.Bluetooth,
                title = "Bluetooth Classic",
                subtitle = "RFCOMM serial wireless link"
            ) {
                if (pairedDevices.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = NeonPalette.DarkCard),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "No paired Bluetooth computers found.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(
                                    onClick = {
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
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonPalette.Cyan),
                                    border = BorderStroke(1.dp, NeonPalette.Cyan.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Reload Paired", style = MaterialTheme.typography.labelMedium)
                                }

                                OutlinedButton(
                                    onClick = {
                                        try {
                                            context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                                        } catch (_: Exception) {}
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Pair in Settings", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                } else {
                    pairedDevices.forEach { device ->
                        @SuppressLint("MissingPermission")
                        val devName = device.name ?: "Unknown Device"
                        val devAddress = device.address

                        BluetoothDeviceCard(
                            name = devName,
                            address = devAddress,
                            onConnect = { viewModel.switchToBluetoothConnection(devAddress, devName) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
