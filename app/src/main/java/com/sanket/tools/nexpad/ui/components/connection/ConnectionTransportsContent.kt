package com.sanket.tools.nexpad.ui.components.connection

import android.bluetooth.BluetoothDevice
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.network.DiscoveredServer
import com.sanket.tools.nexpad.ui.theme.NeonPalette

/**
 * Modular container for connection transports: Network Discovery (Wi-Fi / Tethering),
 * USB Hardware Links (AOA, ADB, Tethering), and Bluetooth Classic.
 */
@androidx.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
@Composable
fun ConnectionTransportsContent(
    discoveredServers: List<DiscoveredServer>,
    pairedDevices: List<BluetoothDevice>,
    isAoaAttached: Boolean,
    isUsbCableConnected: Boolean,
    isAdbAvailable: Boolean,
    adbServerName: String? = null,
    isUsbDebuggingEnabled: Boolean,
    onConnectServer: (ip: String, port: Int, name: String) -> Unit,
    onConnectAoa: () -> Unit,
    onConnectAdb: () -> Unit,
    onConnectBluetooth: (address: String, name: String) -> Unit,
    onRescanNetwork: () -> Unit,
    onOpenDeveloperSettings: () -> Unit,
    onOpenTetheringSettings: () -> Unit,
    onReloadBluetoothDevices: () -> Unit,
    onOpenBluetoothSettings: () -> Unit,
    modifier: Modifier = Modifier,
    // Optional compatibility parameters (kept with default values)
    manualIp: String = "",
    manualPort: String = "9999",
    showManualIpCard: Boolean = false,
    onManualIpChange: (String) -> Unit = {},
    onManualPortChange: (String) -> Unit = {},
    onToggleManualIpCard: () -> Unit = {}
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
            subtitle = "Automatic broadcast discovery on local network",
            accentColor = NeonPalette.Cyan
        ) {
            val wifiServers = discoveredServers.filter { !it.isUsbTethering }
            val tetherServers = discoveredServers.filter { it.isUsbTethering }

            if (discoveredServers.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF0F1523).copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "No computers discovered on LAN yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = onRescanNetwork,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = NeonPalette.Cyan,
                                containerColor = NeonPalette.Cyan.copy(alpha = 0.06f)
                            ),
                            border = BorderStroke(1.dp, NeonPalette.Cyan.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Rescan Network", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
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
        }

        // -------------------------------------------------------------
        // 2. USB CONNECTIONS (AOA, ADB, Tethering)
        // -------------------------------------------------------------
        ConnectionSection(
            icon = Icons.Rounded.Usb,
            title = "USB Hardware Links",
            subtitle = "Zero-latency physical cable connections",
            accentColor = NeonPalette.Green
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
            subtitle = "RFCOMM serial wireless link",
            accentColor = NeonPalette.Purple
        ) {
            if (pairedDevices.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF0F1523).copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "No paired Bluetooth computers found.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = onReloadBluetoothDevices,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = NeonPalette.Purple,
                                    containerColor = NeonPalette.Purple.copy(alpha = 0.06f)
                                ),
                                border = BorderStroke(1.dp, NeonPalette.Purple.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Reload Paired", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            }

                            OutlinedButton(
                                onClick = onOpenBluetoothSettings,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color.White,
                                    containerColor = Color.White.copy(alpha = 0.04f)
                                ),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Pair in Settings", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            } else {
                pairedDevices.forEach { device ->
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
