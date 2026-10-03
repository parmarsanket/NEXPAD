package com.sanket.tools.nexpad.ui.components.connection

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        ConnectionType.USB -> "USB Hardware (AOA)"
        ConnectionType.BT -> "Bluetooth Classic"
        ConnectionType.UNKNOWN -> "Connected Stream"
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF0F1523).copy(alpha = 0.85f),
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                listOf(
                    NeonPalette.Green.copy(alpha = 0.6f),
                    NeonPalette.Green.copy(alpha = 0.2f),
                    Color.White.copy(alpha = 0.05f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row
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
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(NeonPalette.Green.copy(alpha = 0.15f))
                            .border(1.dp, NeonPalette.Green.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
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
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(NeonPalette.Green)
                            )
                            Text(
                                text = transportLabel,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = NeonPalette.Green
                                )
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = onDisconnect,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = NeonPalette.Red.copy(alpha = 0.08f),
                        contentColor = NeonPalette.Red
                    ),
                    border = BorderStroke(1.2.dp, NeonPalette.Red.copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "Disconnect",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            // Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                NeonPalette.Green.copy(alpha = 0.4f),
                                NeonPalette.Green.copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Real-time Metrics 4-grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF070B12).copy(alpha = 0.7f))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                MetricItem(label = "PING (RTT)", value = "${stats.latencyMs ?: "--"} ms", valueColor = NeonPalette.Green)
                MetricItem(label = "INPUT LAG", value = "${stats.inputLagMs ?: "--"} ms", valueColor = NeonPalette.Cyan)
                MetricItem(label = "JITTER", value = "${stats.oneWayJitterMs?.let { "%.1f".format(it) } ?: "--"} ms", valueColor = NeonPalette.Amber)
                MetricItem(
                    label = "LOSS",
                    value = "${stats.packetLossPercent?.let { "%.0f%%".format(it * 100) } ?: "0%"}",
                    valueColor = if ((stats.packetLossPercent ?: 0f) > 0.01f) NeonPalette.Red else Color.White
                )
            }
        }
    }
}

@Composable
fun MetricItem(
    label: String,
    value: String,
    valueColor: Color = Color.White
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Black,
                fontSize = 13.5.sp,
                color = valueColor
            )
        )
    }
}
