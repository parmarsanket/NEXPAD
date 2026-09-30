package com.sanket.tools.nexpad.ui.components.connection

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sanket.tools.nexpad.ui.theme.NeonPalette

/**
 * Status overview card displaying the state of physical links (USB cable, AOA accessory, ADB server).
 */
@Composable
fun ConnectionHubStatusCard(
    isUsbCableConnected: Boolean,
    isAoaAttached: Boolean,
    isAdbAvailable: Boolean,
    onRescan: () -> Unit,
    onOpenBluetoothSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NeonPalette.DarkCard),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, NeonPalette.Cyan.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Link Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "Standby",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Yellow,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                "Connect via local Wi-Fi, USB cable (AOA / ADB / Tethering), or Bluetooth Classic.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusIndicatorRow(
                    label = "USB Cable",
                    status = if (isUsbCableConnected) "Plugged In" else "Disconnected",
                    isGood = isUsbCableConnected
                )
                StatusIndicatorRow(
                    label = "AOA Accessory",
                    status = if (isAoaAttached) "Attached" else "Ready",
                    isGood = isAoaAttached
                )
                StatusIndicatorRow(
                    label = "ADB Server",
                    status = if (isAdbAvailable) "Online" else "Offline",
                    isGood = isAdbAvailable
                )
            }

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onRescan,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonPalette.Cyan),
                    border = BorderStroke(1.dp, NeonPalette.Cyan.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Rescan", style = MaterialTheme.typography.labelMedium)
                }

                OutlinedButton(
                    onClick = onOpenBluetoothSettings,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Bluetooth, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("BT Pair", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun StatusIndicatorRow(label: String, status: String, isGood: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            status,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isGood) NeonPalette.Green else Color.Gray
        )
    }
}
