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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cable
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Usb
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val accentColor = if (isAoaReady) NeonPalette.Cyan else Color.White

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0F1523).copy(alpha = 0.85f),
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                listOf(
                    accentColor.copy(alpha = if (isAoaReady) 0.5f else 0.12f),
                    accentColor.copy(alpha = if (isAoaReady) 0.18f else 0.05f),
                    Color.White.copy(alpha = 0.04f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(NeonPalette.Cyan.copy(alpha = 0.12f))
                            .border(1.dp, NeonPalette.Cyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Cable,
                            contentDescription = null,
                            tint = NeonPalette.Cyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = "USB KERNEL DIRECT (AOA 2.0)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Sub-millisecond driverless hardware pipe",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                StatusBadgePill(
                    text = when {
                        isAoaReady -> "Accessory Ready"
                        isUsbCableConnected -> "Waiting Desktop"
                        else -> "No USB Wire"
                    },
                    isPositive = isAoaReady
                )
            }

            if (isAoaReady) {
                OutlinedButton(
                    onClick = onConnectAoa,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = NeonPalette.Cyan.copy(alpha = 0.08f),
                        contentColor = NeonPalette.Cyan
                    ),
                    border = BorderStroke(1.5.dp, NeonPalette.Cyan.copy(alpha = 0.7f))
                ) {
                    Text(
                        text = "CONNECT (USB AOA)",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        fontSize = 12.sp
                    )
                }
            } else {
                Text(
                    text = if (!isUsbCableConnected) "Connect phone to PC via USB cable." else "Plug phone into PC via USB. When NEXPAD Desktop sends the AOA handshake, this option lights up.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
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
    val accentColor = if (isAdbAvailable) NeonPalette.Green else Color.White

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0F1523).copy(alpha = 0.85f),
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                listOf(
                    accentColor.copy(alpha = if (isAdbAvailable) 0.5f else 0.12f),
                    accentColor.copy(alpha = if (isAdbAvailable) 0.18f else 0.05f),
                    Color.White.copy(alpha = 0.04f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(NeonPalette.Green.copy(alpha = 0.12f))
                            .border(1.dp, NeonPalette.Green.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Usb,
                            contentDescription = null,
                            tint = NeonPalette.Green,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = "USB ADB PORT BRIDGE",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isAdbAvailable) "${adbServerName ?: "Desktop PC"} • Port 9999" else "Port forwarding (127.0.0.1:9999)",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                StatusBadgePill(
                    text = when {
                        !isUsbCableConnected -> "No USB Wire"
                        !isAdbOn -> "Debugging OFF"
                        isAdbAvailable -> "Ready (${adbServerName ?: "PC"})"
                        else -> "Waiting Bridge"
                    },
                    isPositive = isAdbAvailable
                )
            }

            when {
                !isUsbCableConnected -> {
                    Text(
                        text = "Connect phone to PC via USB cable to enable ADB port forwarding.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                !isAdbOn -> {
                    OutlinedButton(
                        onClick = onOpenDeveloperSettings,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = NeonPalette.Amber,
                            containerColor = NeonPalette.Amber.copy(alpha = 0.08f)
                        ),
                        border = BorderStroke(1.dp, NeonPalette.Amber.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "Enable USB Debugging in Settings",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
                isAdbAvailable -> {
                    OutlinedButton(
                        onClick = onConnectAdb,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = NeonPalette.Green.copy(alpha = 0.08f),
                            contentColor = NeonPalette.Green
                        ),
                        border = BorderStroke(1.5.dp, NeonPalette.Green.copy(alpha = 0.7f))
                    ) {
                        Text(
                            text = "CONNECT (USB ADB)",
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            fontSize = 12.sp
                        )
                    }
                }
                else -> {
                    Text(
                        text = "USB cable connected & debugging active. Waiting for NEXPAD Desktop to start ADB bridge...",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
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
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0F1523).copy(alpha = 0.85f),
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                listOf(
                    NeonPalette.Purple.copy(alpha = 0.35f),
                    NeonPalette.Purple.copy(alpha = 0.12f),
                    Color.White.copy(alpha = 0.04f)
                )
            )
        )
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
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(NeonPalette.Purple.copy(alpha = 0.12f))
                        .border(1.dp, NeonPalette.Purple.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Link,
                        contentDescription = null,
                        tint = NeonPalette.Purple,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = "USB TETHERING ADAPTER",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Private low-jitter 100Mbps virtual LAN",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            OutlinedButton(
                onClick = onOpenTetheringSettings,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White,
                    containerColor = Color.White.copy(alpha = 0.04f)
                ),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}
