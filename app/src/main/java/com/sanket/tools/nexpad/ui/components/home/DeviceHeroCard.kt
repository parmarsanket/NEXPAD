package com.sanket.tools.nexpad.ui.components.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sanket.tools.nexpad.viewmodel.ConnectionType
import com.sanket.tools.nexpad.network.DiscoveredServer
import com.sanket.tools.nexpad.ui.components.card.GlassCard
import com.sanket.tools.nexpad.ui.components.effects.RadarScanner
import com.sanket.tools.nexpad.ui.theme.NeonPalette
import com.sanket.tools.nexpad.viewmodel.ConnectionStats
import java.util.Locale

data class HeroConnectionOption(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val buttonText: String,
    val onConnect: () -> Unit
)

sealed class DeviceCardState {
    object Searching : DeviceCardState()
    data class Available(val options: List<HeroConnectionOption>) : DeviceCardState()
    data class Connected(val name: String, val stats: ConnectionStats) : DeviceCardState()

    // Coarse key for AnimatedContent — stops the transition from re-firing
    // when e.g. the server list reorders but we're still "Available".
    val phase: Int get() = when (this) {
        is Searching -> 0
        is Available -> 1
        is Connected -> 2
    }
}

@Composable
fun DeviceHeroCard(
    isConnected: Boolean,
    servers: List<DiscoveredServer>,
    stats: ConnectionStats,
    isAoaAttached: Boolean,
    isAdbAvailable: Boolean,
    adbServerName: String?,
    onConnectServer: (DiscoveredServer) -> Unit,
    onConnectAoa: () -> Unit,
    onConnectAdb: () -> Unit,
    onDisconnectClick: () -> Unit,
    onOpenConnectionHub: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state: DeviceCardState = remember(isConnected, servers, stats, isAoaAttached, isAdbAvailable, adbServerName) {
        when {
            isConnected -> {
                val resolvedName = stats.serverDeviceName
                    ?: servers.firstOrNull { it.isUsbTethering == (stats.transport == ConnectionType.USB_TETHERING) }?.name
                    ?: servers.firstOrNull()?.name
                    ?: "Windows PC"
                DeviceCardState.Connected(
                    name = resolvedName,
                    stats = stats
                )
            }
            else -> {
                val options = mutableListOf<HeroConnectionOption>()

                // 1. USB Direct AOA (Hardware kernel pipe)
                if (isAoaAttached) {
                    options.add(
                        HeroConnectionOption(
                            id = "usb_aoa",
                            title = "NEXPAD PC (Direct USB)",
                            subtitle = "Kernel Direct Pipe • AOA",
                            icon = Icons.Rounded.Usb,
                            buttonText = "Connect (AOA)",
                            onConnect = onConnectAoa
                        )
                    )
                }

                // 2. Discovered Network PCs (USB Tethering prioritized over Wi-Fi)
                servers.forEach { server ->
                    if (server.isUsbTethering) {
                        options.add(
                            HeroConnectionOption(
                                id = "tether_${server.ipAddress}",
                                title = server.name,
                                subtitle = "${server.ipAddress} • USB Tethering",
                                icon = Icons.Rounded.Link,
                                buttonText = "Connect (USB Tethering)",
                                onConnect = { onConnectServer(server) }
                            )
                        )
                    } else {
                        options.add(
                            HeroConnectionOption(
                                id = "wifi_${server.ipAddress}",
                                title = server.name,
                                subtitle = "${server.ipAddress} • Wi-Fi",
                                icon = Icons.Rounded.Wifi,
                                buttonText = "Connect (Wi-Fi)",
                                onConnect = { onConnectServer(server) }
                            )
                        )
                    }
                }

                // 3. USB ADB Port Forwarding (ONLY when physical USB connected and Desktop ADB handshake valid)
                if (isAdbAvailable) {
                    val displayName = adbServerName ?: "PC via USB ADB"
                    options.add(
                        HeroConnectionOption(
                            id = "usb_adb",
                            title = displayName,
                            subtitle = "Port Forwarding (127.0.0.1:9999) • USB ADB",
                            icon = Icons.Rounded.Usb,
                            buttonText = "Connect (ADB)",
                            onConnect = onConnectAdb
                        )
                    )
                }

                if (options.isNotEmpty()) {
                    DeviceCardState.Available(options)
                } else {
                    DeviceCardState.Searching
                }
            }
        }
    }

    GlassCard(modifier = modifier.fillMaxWidth()) {
        AnimatedContent(
            targetState = state,
            contentKey = { it.phase },
            transitionSpec = {
                (fadeIn(tween(320, delayMillis = 90)) +
                        scaleIn(initialScale = 0.92f, animationSpec = tween(320, delayMillis = 90)))
                    .togetherWith(
                        fadeOut(tween(150)) + scaleOut(targetScale = 1.05f, animationSpec = tween(150))
                    )
                    .using(SizeTransform(clip = false) { _, _ -> tween(320, easing = FastOutSlowInEasing) })
            },
            label = "deviceCardState",
            modifier = Modifier.fillMaxWidth()
        ) { target ->
            when (target) {
                is DeviceCardState.Searching ->
                    SearchingContent(onOpenConnectionHub = onOpenConnectionHub)
                is DeviceCardState.Available ->
                    AvailableConnectionsContent(
                        options = target.options,
                        onOpenConnectionHub = onOpenConnectionHub
                    )
                is DeviceCardState.Connected ->
                    ConnectedContent(name = target.name, stats = target.stats, onDisconnectClick = onDisconnectClick)
            }
        }
    }
}

@Composable
fun SearchingContent(onOpenConnectionHub: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        RadarScanner(size = 96.dp)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Searching for PC…",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "Make sure NEXPAD is running on your computer",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        OutlinedButton(
            onClick = onOpenConnectionHub,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonPalette.Cyan),
            border = BorderStroke(1.dp, NeonPalette.Cyan.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Rounded.Hub, contentDescription = null, modifier = Modifier.size(18.dp), tint = NeonPalette.Cyan)
            Spacer(Modifier.width(8.dp))
            Text("Connection Manager", style = MaterialTheme.typography.labelMedium, color = NeonPalette.Cyan, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun AvailableConnectionsContent(
    options: List<HeroConnectionOption>,
    onOpenConnectionHub: () -> Unit
) {
    val infinite = rememberInfiniteTransition(label = "availablePulse")
    val glow by infinite.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow"
    )

    Column(
        modifier = Modifier.fillMaxWidth().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(NeonPalette.Cyan.copy(alpha = glow))
                )
                Text(
                    "Ready to Connect",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
            }
            TextButton(onClick = onOpenConnectionHub) {
                Text(
                    "Manage All",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonPalette.Cyan
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            options.forEach { option ->
                HeroOptionCard(option = option)
            }
        }
    }
}

@Composable
fun HeroOptionCard(option: HeroConnectionOption) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, NeonPalette.Cyan.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(NeonPalette.Cyan.copy(alpha = 0.12f))
                        .border(1.dp, NeonPalette.Cyan.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(option.icon, contentDescription = null, tint = NeonPalette.Cyan, modifier = Modifier.size(22.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        option.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        option.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = option.onConnect,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonPalette.Cyan)
            ) {
                Text(
                    option.buttonText,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
fun ConnectedContent(name: String, stats: ConnectionStats, onDisconnectClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(NeonPalette.ConnectedDot.copy(alpha = 0.15f))
                        .border(1.dp, NeonPalette.ConnectedDot.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Computer, contentDescription = "Connected", tint = NeonPalette.ConnectedDot, modifier = Modifier.size(28.dp))
                }
                Column {
                    Text(name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(NeonPalette.ConnectedDot))
                        Text("Connected", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (stats.packetLossPercent != null) {
                val lossPct = (stats.packetLossPercent * 100).toInt()
                val chipColor = when {
                    lossPct < 5 -> NeonPalette.Green
                    lossPct < 15 -> Color(0xFFF59E0B) // Amber
                    else -> Color(0xFFEF4444) // Red
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(chipColor.copy(alpha = 0.15f))
                        .border(1.dp, chipColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Loss $lossPct%",
                        style = MaterialTheme.typography.labelSmall,
                        color = chipColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .padding(vertical = 14.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LiveStat(modifier = Modifier.weight(1.1f), label = "Input Lag", value = stats.inputLagMs?.toString() ?: "--", unit = "ms")
            StatDivider()
            LiveStat(modifier = Modifier.weight(0.9f), label = "Ping", value = stats.latencyMs?.toString() ?: "--", unit = "ms")
            StatDivider()
            LiveStat(modifier = Modifier.weight(1.0f), label = "Jitter", value = stats.oneWayJitterMs?.let { String.format(Locale.US, "±%.1f", it) } ?: "--", unit = "ms")
            StatDivider()
            LiveStat(modifier = Modifier.weight(1.0f), label = "Type", value = stats.transport.displayName)
        }

        OutlinedButton(
            onClick = onDisconnectClick,
            modifier = Modifier.fillMaxWidth().height(44.dp),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
        ) {
            Text("Disconnect", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun LiveStat(modifier: Modifier = Modifier, label: String, value: String, unit: String = "") {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        AnimatedContent(
            targetState = value,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
            label = "statValue"
        ) { v ->
            if (unit.isEmpty()) {
                Text(
                    text = v,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = v,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.padding(bottom = 1.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StatDivider() {
    Box(
        modifier = Modifier
            .height(28.dp)
            .width(1.dp)
            .background(Color.White.copy(alpha = 0.1f))
    )
}
