package com.sanket.tools.nexpad.ui.components.connection

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cable
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sanket.tools.nexpad.ui.components.badge.StatusBadgePill
import com.sanket.tools.nexpad.ui.theme.NeonPalette

@Composable
fun UsbAoaCard(
    isAoaAttached: Boolean,
    isUsbCableConnected: Boolean,
    onConnectAoa: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isAoaReady = isAoaAttached && isUsbCableConnected
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NeonPalette.DarkCard),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (isAoaReady) NeonPalette.Cyan.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f))
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
                    Icon(
                        imageVector = Icons.Rounded.Cable,
                        contentDescription = null,
                        tint = NeonPalette.Cyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "USB Kernel Direct (AOA)",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Text(
                            text = "Sub-millisecond driverless USB pipe",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                StatusBadgePill(
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
                    onClick = onConnectAoa,
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPalette.Cyan)
                ) {
                    Text("Connect (AOA)", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            } else {
                Text(
                    text = if (!isUsbCableConnected) "Connect phone to PC via USB cable." else "Plug phone into PC via USB. When NEXPAD Desktop sends the AOA handshake, this option lights up.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun UsbAdbCard(
    isUsbCableConnected: Boolean,
    isAdbOn: Boolean,
    isAdbAvailable: Boolean,
    adbServerName: String?,
    onConnectAdb: () -> Unit,
    onOpenDeveloperSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NeonPalette.DarkCard),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (isAdbAvailable) NeonPalette.Cyan.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f))
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
                    Icon(
                        imageVector = Icons.Rounded.Usb,
                        contentDescription = null,
                        tint = NeonPalette.Green,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "USB ADB Port Bridge",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Text(
                            text = if (isAdbAvailable) "${adbServerName ?: "Desktop PC"} • Port 9999" else "Port forwarding (127.0.0.1:9999)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                StatusBadgePill(
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
                        text = "Connect phone to PC via USB cable to enable ADB port forwarding.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                !isAdbOn -> {
                    OutlinedButton(
                        onClick = onOpenDeveloperSettings,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonPalette.Amber),
                        border = BorderStroke(1.dp, NeonPalette.Amber.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Enable USB Debugging in Settings", style = MaterialTheme.typography.labelMedium)
                    }
                }
                isAdbAvailable -> {
                    Button(
                        onClick = onConnectAdb,
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPalette.Cyan)
                    ) {
                        Text("Connect (ADB)", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
                else -> {
                    Text(
                        text = "USB cable connected & debugging active. Waiting for NEXPAD Desktop to start ADB bridge...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun UsbTetheringCard(
    onOpenTetheringSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NeonPalette.DarkCard),
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
                Icon(
                    imageVector = Icons.Rounded.Link,
                    contentDescription = null,
                    tint = NeonPalette.Cyan,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = "USB Tethering Adapter",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        text = "Private low-jitter 100Mbps virtual LAN",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            OutlinedButton(
                onClick = onOpenTetheringSettings,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Settings", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
