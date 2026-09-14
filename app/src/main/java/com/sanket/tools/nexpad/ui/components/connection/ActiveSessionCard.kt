package com.sanket.tools.nexpad.ui.components.connection

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sanket.tools.nexpad.ui.theme.NeonPalette
import com.sanket.tools.nexpad.viewmodel.ConnectionStats
import com.sanket.tools.nexpad.viewmodel.ConnectionType

@Composable
fun ActiveSessionCard(
    stats: ConnectionStats,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transportLabel = when (stats.transport) {
        ConnectionType.WIFI -> "Wi-Fi LAN"
        ConnectionType.USB_TETHERING -> "USB Tethering"
        ConnectionType.USB -> "USB Hardware"
        ConnectionType.BT -> "Bluetooth Classic"
        ConnectionType.UNKNOWN -> "Connected"
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NeonPalette.DarkCard),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, NeonPalette.Green.copy(alpha = 0.6f))
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
                            .background(NeonPalette.Green.copy(alpha = 0.15f))
                            .border(1.dp, NeonPalette.Green.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Computer,
                            contentDescription = null,
                            tint = NeonPalette.Green,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = stats.serverDeviceName ?: "Connected PC",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = transportLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = NeonPalette.Green
                        )
                    }
                }

                Button(
                    onClick = onDisconnect,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPalette.Red.copy(alpha = 0.2f)),
                    border = BorderStroke(1.dp, NeonPalette.Red.copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Disconnect", color = NeonPalette.Red, fontWeight = FontWeight.Bold)
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
fun MetricItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
    }
}
