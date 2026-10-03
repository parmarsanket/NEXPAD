package com.sanket.tools.nexpad.ui.components.connection

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Refresh
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

/**
 * Status overview card displaying the real-time state of physical hardware links (USB cable, AOA accessory, ADB server).
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
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF0F1523).copy(alpha = 0.85f),
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                listOf(
                    NeonPalette.Cyan.copy(alpha = 0.45f),
                    NeonPalette.Cyan.copy(alpha = 0.15f),
                    Color.White.copy(alpha = 0.04f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(NeonPalette.Cyan.copy(alpha = 0.12f))
                            .border(1.dp, NeonPalette.Cyan.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Hub,
                            contentDescription = null,
                            tint = NeonPalette.Cyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "LINK HARDWARE STATUS",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.2.sp
                            )
                        )
                        Text(
                            text = "Real-time transport detection",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.5.sp
                            )
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = NeonPalette.Amber.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, NeonPalette.Amber.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "STANDBY",
                        color = NeonPalette.Amber,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.5.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
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
                                NeonPalette.Cyan.copy(alpha = 0.35f),
                                NeonPalette.Cyan.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Status indicator items
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

            Spacer(Modifier.height(2.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onRescan,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = NeonPalette.Cyan,
                        containerColor = NeonPalette.Cyan.copy(alpha = 0.06f)
                    ),
                    border = BorderStroke(1.dp, NeonPalette.Cyan.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Rescan", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }

                OutlinedButton(
                    onClick = onOpenBluetoothSettings,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White,
                        containerColor = Color.White.copy(alpha = 0.04f)
                    ),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Bluetooth, contentDescription = null, tint = NeonPalette.Purple, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("BT Pair", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}

@Composable
private fun StatusIndicatorRow(label: String, status: String, isGood: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF070B12).copy(alpha = 0.6f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isGood) NeonPalette.Green else Color(0xFF64748B))
            )
            Text(
                label,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    color = Color(0xFFCBD5E1)
                )
            )
        }
        Text(
            status,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 11.5.sp,
                color = if (isGood) NeonPalette.Green else Color(0xFF94A3B8)
            )
        )
    }
}
