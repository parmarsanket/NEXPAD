package com.sanket.tools.nexpad.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.sanket.tools.nexpad.network.DiscoveredServer
import com.sanket.tools.nexpad.viewmodel.ConnectionType
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel

private object ConnPalette {
    val Cyan = Color(0xFF00E5FF)
    val Green = Color(0xFF39FF14)
    val Purple = Color(0xFFB400FF)
    val Red = Color(0xFFFF3366)
    val Amber = Color(0xFFFFB300)
    val DarkCard = Color(0xFF141923)
    val BorderDark = Color(0x3300E5FF)
}

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
                    // Status Pill
                    Row(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (isConnected) ConnPalette.Green.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.08f))
                            .border(
                                1.dp,
                                if (isConnected) ConnPalette.Green.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.15f),
                                RoundedCornerShape(50)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isConnected) ConnPalette.Green else Color.Gray)
                        )
                        Text(
                            if (isConnected) "Connected" else "Idle",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isConnected) ConnPalette.Green else Color.Gray
                        )
                    }
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
                        colors = CardDefaults.cardColors(containerColor = ConnPalette.DarkCard),
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
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ConnPalette.Cyan),
                                border = BorderStroke(1.dp, ConnPalette.Cyan.copy(alpha = 0.5f)),
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
                                android.util.Log.d("NEXPAD", "⏱️ [BENCHMARK] User CLICKED Connect (USB Tethering) in ConnectionHub for ${server.name} (${server.ipAddress}:${server.port})")
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
                                android.util.Log.d("NEXPAD", "⏱️ [BENCHMARK] User CLICKED Connect (Wi-Fi) in ConnectionHub for ${server.name} (${server.ipAddress}:${server.port})")
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
                            tint = ConnPalette.Cyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (showManualIpCard) "Hide Manual IP" else "Manual IP Connect",
                            color = ConnPalette.Cyan,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                if (showManualIpCard) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = ConnPalette.DarkCard),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, ConnPalette.Cyan.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Direct IP Fallback", style = MaterialTheme.typography.titleSmall, color = Color.White)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = manualIp,
                                    onValueChange = { manualIp = it },
                                    label = { Text("PC IP Address") },
                                    placeholder = { Text("192.168.1.10") },
                                    modifier = Modifier.weight(2f),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ConnPalette.Cyan,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )
                                OutlinedTextField(
                                    value = manualPort,
                                    onValueChange = { manualPort = it },
                                    label = { Text("Port") },
                                    placeholder = { Text("9999") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ConnPalette.Cyan,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )
                            }
                            Button(
                                onClick = {
                                    val port = manualPort.toIntOrNull() ?: 9999
                                    if (manualIp.isNotBlank()) {
                                        viewModel.connect(manualIp.trim(), port, "Manual PC")
                                    } else {
                                        Toast.makeText(context, "Enter a valid IP address", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ConnPalette.Cyan)
                            ) {
                                Text("Connect via Direct IP", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
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
                // Direct USB (AOA) Mode
                val isAoaReady = isAoaAttached && isUsbCableConnected
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ConnPalette.DarkCard),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, if (isAoaReady) ConnPalette.Cyan.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Rounded.Cable, contentDescription = null, tint = ConnPalette.Cyan, modifier = Modifier.size(24.dp))
                                Column {
                                    Text("USB Kernel Direct (AOA)", style = MaterialTheme.typography.titleMedium, color = Color.White)
                                    Text("Sub-millisecond driverless USB pipe", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            BadgePill(
                                text = when {
                                    isAoaReady -> "Accessory Ready"
                                    isUsbCableConnected -> "Waiting Handshake"
                                    else -> "No USB Wire"
                                },
                                isPositive = isAoaReady
                            )
                        }

                        if (isAoaReady) {
                            Button(
                                onClick = { viewModel.switchToAoaConnection() },
                                modifier = Modifier.fillMaxWidth().height(42.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ConnPalette.Cyan)
                            ) {
                                Text("Connect (AOA)", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Text(
                                if (!isUsbCableConnected) "Connect phone to PC via USB cable." else "Plug phone into PC via USB. When NEXPAD Desktop sends the AOA handshake, this option lights up.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // USB ADB Reverse Bridge
                val isAdbOn = viewModel.isUsbDebuggingEnabled()
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ConnPalette.DarkCard),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, if (isAdbAvailable) ConnPalette.Cyan.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Rounded.Usb, contentDescription = null, tint = ConnPalette.Green, modifier = Modifier.size(24.dp))
                                Column {
                                    Text("USB ADB Port Bridge", style = MaterialTheme.typography.titleMedium, color = Color.White)
                                    Text(
                                        if (isAdbAvailable) "${adbServerName ?: "Desktop PC"} • Port 9999" else "Port forwarding (127.0.0.1:9999)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            BadgePill(
                                text = when {
                                    !isUsbCableConnected -> "No USB Wire"
                                    !isAdbOn -> "Debugging OFF"
                                    isAdbAvailable -> "Ready (${adbServerName ?: "PC"})"
                                    else -> "Waiting Desktop"
                                },
                                isPositive = isAdbAvailable
                            )
                        }

                        when {
                            !isUsbCableConnected -> {
                                Text(
                                    "Connect phone to PC via USB cable to enable ADB port forwarding.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            !isAdbOn -> {
                                OutlinedButton(
                                    onClick = {
                                        try {
                                            val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                                            context.startActivity(intent)
                                        } catch (_: Exception) {
                                            Toast.makeText(context, "Developer settings not found", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ConnPalette.Amber),
                                    border = BorderStroke(1.dp, ConnPalette.Amber.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Enable USB Debugging in Settings", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                            isAdbAvailable -> {
                                Button(
                                    onClick = { viewModel.switchToAdbConnection() },
                                    modifier = Modifier.fillMaxWidth().height(42.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = ConnPalette.Cyan)
                                ) {
                                    Text("Connect (ADB)", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                            else -> {
                                Text(
                                    "USB cable connected & debugging active. Waiting for NEXPAD Desktop to start ADB bridge...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // USB Tethering Shortcut
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ConnPalette.DarkCard),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Link, contentDescription = null, tint = ConnPalette.Cyan, modifier = Modifier.size(24.dp))
                            Column {
                                Text("USB Tethering Adapter", style = MaterialTheme.typography.titleMedium, color = Color.White)
                                Text("Private low-jitter 100Mbps virtual LAN", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                try {
                                    val intent = Intent().apply {
                                        setClassName("com.android.settings", "com.android.settings.TetherSettings")
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    Toast.makeText(context, "Cannot open tethering settings directly", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Settings", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
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
                        colors = CardDefaults.cardColors(containerColor = ConnPalette.DarkCard),
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
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ConnPalette.Cyan),
                                    border = BorderStroke(1.dp, ConnPalette.Cyan.copy(alpha = 0.5f)),
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

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = ConnPalette.DarkCard),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, ConnPalette.Cyan.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(ConnPalette.Cyan.copy(alpha = 0.12f))
                                            .border(1.dp, ConnPalette.Cyan.copy(alpha = 0.4f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Rounded.Bluetooth, contentDescription = null, tint = ConnPalette.Cyan, modifier = Modifier.size(20.dp))
                                    }
                                    Column {
                                        Text(devName, style = MaterialTheme.typography.titleMedium, color = Color.White)
                                        Text(devAddress, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Button(
                                    onClick = { viewModel.switchToBluetoothConnection(devAddress, devName) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = ConnPalette.Cyan)
                                ) {
                                    Text("Connect (BT)", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// -------------------------------------------------------------
// Sub-Composables
// -------------------------------------------------------------

@Composable
private fun ActiveSessionCard(
    stats: com.sanket.tools.nexpad.viewmodel.ConnectionStats,
    onDisconnect: () -> Unit
) {
    val transportLabel = when (stats.transport) {
        ConnectionType.WIFI -> "Wi-Fi LAN"
        ConnectionType.USB_TETHERING -> "USB Tethering"
        ConnectionType.USB -> "USB Hardware"
        ConnectionType.BT -> "Bluetooth Classic"
        ConnectionType.UNKNOWN -> "Connected"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ConnPalette.DarkCard),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, ConnPalette.Green.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(ConnPalette.Green.copy(alpha = 0.15f))
                            .border(1.dp, ConnPalette.Green.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Computer, contentDescription = null, tint = ConnPalette.Green, modifier = Modifier.size(24.dp))
                    }
                    Column {
                        Text(
                            stats.serverDeviceName ?: "Connected PC",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            transportLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = ConnPalette.Green
                        )
                    }
                }

                Button(
                    onClick = onDisconnect,
                    colors = ButtonDefaults.buttonColors(containerColor = ConnPalette.Red.copy(alpha = 0.2f)),
                    border = BorderStroke(1.dp, ConnPalette.Red.copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Disconnect", color = ConnPalette.Red, fontWeight = FontWeight.Bold)
                }
            }

            // Real-time Metrics 4-grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                MetricItem(label = "PING (RTT)", value = "${stats.latencyMs ?: "--"} ms")
                MetricItem(label = "INPUT LAG", value = "${stats.inputLagMs ?: "--"} ms")
                MetricItem(label = "JITTER", value = "${stats.oneWayJitterMs?.let { "%.1f".format(it) } ?: "--"} ms")
                MetricItem(
                    label = "LOSS",
                    value = "${stats.packetLossPercent?.let { "%.0f%%".format(it * 100) } ?: "0%"}"
                )
            }
        }
    }
}

@Composable
private fun MetricItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
    }
}

@Composable
private fun ConnectionSection(
    icon: ImageVector,
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = ConnPalette.Cyan, modifier = Modifier.size(20.dp))
            Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
        }
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

@Composable
private fun ServerEntryCard(
    server: DiscoveredServer,
    isTethering: Boolean,
    onConnect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ConnPalette.DarkCard),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, ConnPalette.Cyan.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(ConnPalette.Cyan.copy(alpha = 0.12f))
                            .border(1.dp, ConnPalette.Cyan.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isTethering) Icons.Rounded.Link else Icons.Rounded.Computer,
                            contentDescription = null,
                            tint = ConnPalette.Cyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            server.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${server.ipAddress}:${server.port}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                BadgePill(
                    text = if (isTethering) "USB Tethering" else "Wi-Fi",
                    isPositive = true
                )
            }

            Button(
                onClick = onConnect,
                modifier = Modifier.fillMaxWidth().height(42.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ConnPalette.Cyan)
            ) {
                Text(
                    if (isTethering) "Connect (USB Tethering)" else "Connect (Wi-Fi)",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun BadgePill(text: String, isPositive: Boolean) {
    val tint = if (isPositive) ConnPalette.Green else Color.Gray
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(tint.copy(alpha = 0.12f))
            .border(1.dp, tint.copy(alpha = 0.4f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(tint))
        Text(text, style = MaterialTheme.typography.labelSmall, color = tint, fontWeight = FontWeight.SemiBold)
    }
}
