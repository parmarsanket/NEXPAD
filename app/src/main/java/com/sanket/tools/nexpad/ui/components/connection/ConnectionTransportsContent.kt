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
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.text.style.TextOverflow
import com.sanket.tools.nexpad.network.DiscoveredServer
import com.sanket.tools.nexpad.ui.components.common.NxOutlinedButton
import com.sanket.tools.nexpad.ui.layout.AdaptiveLayoutSpec
import com.sanket.tools.nexpad.ui.theme.NeonPalette

/**
 * Modular container for connection transports: Network Discovery (Wi-Fi / Tethering),
 * USB Hardware Links (AOA, ADB, Tethering), and Bluetooth Classic.
 */
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
    onDownloadDesktop: () -> Unit = {},
    modifier: Modifier = Modifier,
    layout: AdaptiveLayoutSpec? = null,
    // Optional compatibility parameters (kept with default values)
    manualIp: String = "",
    manualPort: String = "9999",
    showManualIpCard: Boolean = false,
    onManualIpChange: (String) -> Unit = {},
    onManualPortChange: (String) -> Unit = {},
    onToggleManualIpCard: () -> Unit = {}
) {
    val sectionSpacing = if (layout?.isShortScreen == true) 16.dp else 24.dp

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(sectionSpacing)
    ) {
        // -------------------------------------------------------------
        // 1. DISCOVERED COMPUTERS (Wi-Fi & USB Tethering)
        // -------------------------------------------------------------
        ConnectionSection(
            icon = Icons.Rounded.Wifi,
            title = "Network Discovery (Wi-Fi / Tethering)",
            subtitle = "Automatic broadcast discovery on local network",
            accentColor = NeonPalette.Cyan,
            layout = layout
        ) {
            val wifiServers = discoveredServers.filter { !it.isUsbTethering }
            val tetherServers = discoveredServers.filter { it.isUsbTethering }

            val emptyPadding = layout?.cardPadding ?: 16.dp
            val emptyBodySize = layout?.bodyFontSize ?: 13.sp
            val emptyBtnHeight = layout?.buttonHeight ?: 40.dp
            val emptyBtnFontSize = layout?.buttonFontSize ?: 12.sp

            if (discoveredServers.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = NeonPalette.CardSurface.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(emptyPadding),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "No computers discovered on LAN yet",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = emptyBodySize),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                                NxOutlinedButton(
                                    onClick = onRescanNetwork,
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = NeonPalette.Cyan,
                                        containerColor = NeonPalette.Cyan.copy(alpha = 0.06f)
                                    ),
                                    borderColors = NeonPalette.Cyan.copy(alpha = 0.5f),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                                    modifier = Modifier.weight(1f).height(emptyBtnHeight),
                                    imageVector = Icons.Rounded.Refresh,
                                    iconModifier = Modifier.size(15.dp),
                                    space = 6.dp,
                                    text = "Rescan",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = emptyBtnFontSize,
                                    ),
                                )
                                NxOutlinedButton(
                                    onClick = onDownloadDesktop,
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color.White,
                                        containerColor = Color.White.copy(alpha = 0.04f)
                                    ),
                                    borderColors =  Color.White.copy(alpha = 0.25f),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                                    modifier = Modifier.weight(1f).height(emptyBtnHeight),
                                    imageVector = Icons.Rounded.Refresh,
                                    iconModifier = Modifier.size(15.dp),
                                    space = 6.dp,
                                    text = "Get PC Server",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = emptyBtnFontSize
                                    ),
                                )
//                            OutlinedButton(
//                                onClick = onRescanNetwork,
//                                modifier = Modifier.weight(1f).height(emptyBtnHeight),
//                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
//                                colors = ButtonDefaults.outlinedButtonColors(
//                                    contentColor = NeonPalette.Cyan,
//                                    containerColor = NeonPalette.Cyan.copy(alpha = 0.06f)
//                                ),
//                                border = BorderStroke(1.dp, NeonPalette.Cyan.copy(alpha = 0.5f)),
//                                shape = RoundedCornerShape(10.dp)
//                            ) {
//                                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
//                                Spacer(Modifier.width(5.dp))
//                                Text(
//                                    text = "Rescan",
//                                    style = MaterialTheme.typography.labelMedium.copy(
//                                        fontWeight = FontWeight.Bold,
//                                        fontSize = emptyBtnFontSize
//                                    ),
//                                    maxLines = 1,
//                                    softWrap = false
//                                )
//                            }
//
//                            OutlinedButton(
//                                onClick = onDownloadDesktop,
//                                modifier = Modifier.weight(1f).height(emptyBtnHeight),
//                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
//                                colors = ButtonDefaults.outlinedButtonColors(
//                                    contentColor = Color.White,
//                                    containerColor = Color.White.copy(alpha = 0.04f)
//                                ),
//                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
//                                shape = RoundedCornerShape(10.dp)
//                            ) {
//                                Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(13.dp))
//                                Spacer(Modifier.width(5.dp))
//                                Text(
//                                    text = "Get PC Server",
//                                    style = MaterialTheme.typography.labelMedium.copy(
//                                        fontWeight = FontWeight.Bold,
//                                        fontSize = emptyBtnFontSize
//                                    ),
//                                    maxLines = 1,
//                                    softWrap = false,
//                                    overflow = TextOverflow.Ellipsis
//                                )
//                            }
                        }
                    }
                }
            } else {
                // USB Tethering Servers First
                tetherServers.forEach { server ->
                    ServerEntryCard(
                        server = server,
                        isTethering = true,
                        layout = layout,
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
                        layout = layout,
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
            accentColor = NeonPalette.Green,
            layout = layout
        ) {
            UsbAoaCard(
                isAoaAttached = isAoaAttached,
                isUsbCableConnected = isUsbCableConnected,
                onConnectAoa = onConnectAoa,
                layout = layout
            )

            UsbAdbCard(
                isUsbCableConnected = isUsbCableConnected,
                isAdbOn = isUsbDebuggingEnabled,
                isAdbAvailable = isAdbAvailable,
                adbServerName = adbServerName,
                onConnectAdb = onConnectAdb,
                onOpenDeveloperSettings = onOpenDeveloperSettings,
                layout = layout
            )

            UsbTetheringCard(
                onOpenTetheringSettings = onOpenTetheringSettings,
                layout = layout
            )
        }

        // -------------------------------------------------------------
        // 3. BLUETOOTH CLASSIC
        // -------------------------------------------------------------
        ConnectionSection(
            icon = Icons.Rounded.Bluetooth,
            title = "Bluetooth Classic",
            subtitle = "RFCOMM serial wireless link",
            accentColor = NeonPalette.Purple,
            layout = layout
        ) {
            val emptyPadding = layout?.cardPadding ?: 16.dp
            val emptyBodySize = layout?.bodyFontSize ?: 13.sp
            val emptyBtnHeight = layout?.buttonHeight ?: 40.dp
            val emptyBtnFontSize = layout?.buttonFontSize ?: 12.sp

            if (pairedDevices.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = NeonPalette.CardSurface.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(emptyPadding),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "No paired Bluetooth computers found.",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = emptyBodySize),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NxOutlinedButton(
                                onClick = onReloadBluetoothDevices,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = NeonPalette.Purple,
                                    containerColor = NeonPalette.Purple.copy(alpha = 0.06f)
                                ),
                                borderColors = NeonPalette.Purple.copy(alpha = 0.5f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                                modifier = Modifier.weight(1f).height(emptyBtnHeight),
                                imageVector = Icons.Rounded.Refresh,
                                iconModifier = Modifier.size(15.dp),
                                space = 6.dp,
                                text = "Reload Paired",
                                style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = emptyBtnFontSize
                                    ),
                            )
                            NxOutlinedButton(
                                onClick = onOpenBluetoothSettings,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color.White,
                                    containerColor = Color.White.copy(alpha = 0.04f)
                                ),
                                borderColors =  Color.White.copy(alpha = 0.25f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                                modifier = Modifier.weight(1f).height(emptyBtnHeight),
                                imageVector = Icons.Rounded.Refresh,
                                iconModifier =Modifier.size(15.dp),
                                space = 6.dp,
                                text = "BT Pair",
                                tint = NeonPalette.Purple,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = emptyBtnFontSize
                                ),
                            )
                        }
                    }
                }
            } else {
                pairedDevices.forEach { device ->
                    val devName = try { device.name } catch (_: SecurityException) { null } ?: "Unknown Device"
                    val devAddress = device.address

                    BluetoothDeviceCard(
                        name = devName,
                        address = devAddress,
                        layout = layout,
                        onConnect = { onConnectBluetooth(devAddress, devName) }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}
