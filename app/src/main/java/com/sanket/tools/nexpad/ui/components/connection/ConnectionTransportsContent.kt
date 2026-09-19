package com.sanket.tools.nexpad.ui.components.connection

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sanket.tools.nexpad.network.DiscoveredServer
import com.sanket.tools.nexpad.ui.theme.NeonPalette

/**
 * Modular container for connection transports: Network Discovery (Wi-Fi / Tethering),
 * USB Hardware Links (AOA, ADB, Tethering), and Bluetooth Classic.
 */
@Composable
fun ConnectionTransportsContent(
    discoveredServers: List<DiscoveredServer>,
    pairedDevices: List<BluetoothDevice>,
    manualIp: String,
    manualPort: String,
    showManualIpCard: Boolean,
    isAoaAttached: Boolean,
    isUsbCableConnected: Boolean,
    isAdbAvailable: Boolean,
    adbServerName: String? = null,
    isUsbDebuggingEnabled: Boolean,
    onManualIpChange: (String) -> Unit,
    onManualPortChange: (String) -> Unit,
    onToggleManualIpCard: () -> Unit,
    onConnectServer: (ip: String, port: Int, name: String) -> Unit,
    onConnectAoa: () -> Unit,
    onConnectAdb: () -> Unit,
    onConnectBluetooth: (address: String, name: String) -> Unit,
    onRescanNetwork: () -> Unit,
    onOpenDeveloperSettings: () -> Unit,
    onOpenTetheringSettings: () -> Unit,
    onReloadBluetoothDevices: () -> Unit,
    onOpenBluetoothSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // -------------------------------------------------------------
        // 1. DISCOVERED COMPUTERS (Wi-Fi & USB Tethering)
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
                            onClick = onRescanNetwork,
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
                            Log.d("NEXPAD", "⏱️ [BENCHMARK] User CLICKED Connect (USB Tethering) for ${server.name} (${server.ipAddress}:${server.port})")
                            onConnectServer(server.ipAddress, server.port, server.name)
                        }
                    )
                }
                // Wi-Fi Servers
                wifiServers.forEach { server ->
                    ServerEntryCard(
                        server = server,
                        isTethering = false,
                        onConnect = {
                            Log.d("NEXPAD", "⏱️ [BENCHMARK] User CLICKED Connect (Wi-Fi) for ${server.name} (${server.ipAddress}:${server.port})")
                            onConnectServer(server.ipAddress, server.port, server.name)
                        }
                    )
                }
            }

            // Manual IP Toggle Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onToggleManualIpCard) {
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
                    onIpChange = onManualIpChange,
                    onPortChange = onManualPortChange,
                    onConnect = {
                        val port = manualPort.toIntOrNull() ?: 9999
                        onConnectServer(manualIp.trim(), port, "Manual PC")
                    }
                )
            }
        }

        // -------------------------------------------------------------
        // 2. USB CONNECTIONS (AOA, ADB, Tethering)
        // -------------------------------------------------------------
        ConnectionSection(
            icon = Icons.Rounded.Usb,
            title = "USB Hardware Links",
            subtitle = "Zero-latency physical cable connections"
        ) {
            UsbAoaCard(
                isAoaAttached = isAoaAttached,
                isUsbCableConnected = isUsbCableConnected,
                onConnectAoa = onConnectAoa
            )

            UsbAdbCard(
                isUsbCableConnected = isUsbCableConnected,
                isAdbOn = isUsbDebuggingEnabled,
                isAdbAvailable = isAdbAvailable,
                adbServerName = adbServerName,
                onConnectAdb = onConnectAdb,
                onOpenDeveloperSettings = onOpenDeveloperSettings
            )

            UsbTetheringCard(
                onOpenTetheringSettings = onOpenTetheringSettings
            )
        }

        // -------------------------------------------------------------
        // 3. BLUETOOTH CLASSIC
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
                                onClick = onReloadBluetoothDevices,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonPalette.Cyan),
                                border = BorderStroke(1.dp, NeonPalette.Cyan.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Reload Paired", style = MaterialTheme.typography.labelMedium)
                            }

                            OutlinedButton(
                                onClick = onOpenBluetoothSettings,
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
                        onConnect = { onConnectBluetooth(devAddress, devName) }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}
