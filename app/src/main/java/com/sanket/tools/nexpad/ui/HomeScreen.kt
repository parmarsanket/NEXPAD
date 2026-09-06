package com.sanket.tools.nexpad.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.os.Build
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import kotlin.math.abs
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.DashboardCustomize
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.ui.text.style.TextOverflow
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.material3.*
import androidx.compose.material3.carousel.HorizontalCenteredHeroCarousel
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.navigation.NavController
import com.sanket.tools.nexpad.utils.LayoutManager
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel
import com.sanket.tools.nexpad.network.DiscoveredServer
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.style.TextAlign
import com.sanket.tools.nexpad.viewmodel.ConnectionType

// ---------------------------------------------------------------------------
// Design tokens — pulling the cyberpunk palette out of the composables makes
// it reusable and gives us one place to retheme from.
// ---------------------------------------------------------------------------
private object NeonPalette {
    val Cyan = Color(0xFF00E5FF)
    val Purple = Color(0xFFB400FF)
    val Green = Color(0xFF39FF14)
    val PanelBgTop = Color(0xFF0E1524)
    val PanelBgBottom = Color(0xFF070B14)
    val CardIdleBg = Color(0xFF111111)
    val CardIdleBorder = Color(0xFF333333)
    val CardIdleText = Color(0xFF888888)
    val ConnectedDot = Color(0xFF34D399)
}

data class LayoutOption(
    val title: String,
    val subtitle: String
)

// Single source of truth for the carousel content — was previously
// duplicated with copy-paste "Layout 1/2/3" subtitles on the advanced tier.
private val layoutOptions = listOf(
    LayoutOption("Classic Pro", "Layout 1"),
    LayoutOption("FPS Master", "Layout 2"),
    LayoutOption("Racing Sim", "Layout 3"),
    LayoutOption("Advance 1", "Layout 4"),
    LayoutOption("Advance 2", "Layout 5"),
    LayoutOption("Advance 3", "Layout 6"),
)



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, layoutManager: LayoutManager, viewModel: GamepadViewModel) {
    val scrollState = rememberScrollState()
    
    val isConnected by viewModel.isConnected.collectAsState()
    val discoveredServers by viewModel.discoveredServers.collectAsState()
    val connectionStats by viewModel.connectionStats.collectAsState()

    LaunchedEffect(isConnected) {
        if (!isConnected) {
            viewModel.startDiscovery()
        } else {
            viewModel.stopDiscovery()
        }
    }
    DisposableEffect(Unit) {
        onDispose { viewModel.stopDiscovery() }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "NEXPAD",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.onBackground,
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.secondaryContainer,
                                    MaterialTheme.colorScheme.tertiaryContainer,
                                    MaterialTheme.colorScheme.tertiary
                                )
                            )
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(

        ) {
            CyberGrid(
                Modifier.matchParentSize()
            )

            ScanLine(
                Modifier.matchParentSize()
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                HeaderRow(isConnected = isConnected)

                VShapedPanel(
                    onPlayClick = { navController.navigate("gamepad") }
                )
                Text(
                    "Online Device",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val isAoaAttached by viewModel.isAoaAttached.collectAsState()
                val isUsbCableConnected by viewModel.isUsbCableConnected.collectAsState()
                val isAdbAvailable by viewModel.isAdbAvailable.collectAsState()
                val adbServerName by viewModel.adbServerName.collectAsState()

                DeviceHeroCard(
                    isConnected = isConnected,
                    servers = discoveredServers,
                    stats = connectionStats,
                    isAoaAttached = isAoaAttached && isUsbCableConnected,
                    isAdbAvailable = isAdbAvailable,
                    adbServerName = adbServerName,
                    onConnectServer = { server ->
                        android.util.Log.d("NEXPAD", "⏱️ [BENCHMARK] User CLICKED Connect (${if (server.isUsbTethering) "USB Tethering" else "Wi-Fi"}) button for ${server.name} (${server.ipAddress}:${server.port})")
                        viewModel.connect(server.ipAddress, server.port, server.name)
                    },
                    onConnectAoa = { viewModel.switchToAoaConnection() },
                    onConnectAdb = { viewModel.switchToAdbConnection() },
                    onDisconnectClick = { viewModel.disconnect() },
                    onOpenConnectionHub = { navController.navigate("connections") }
                )

                Text(
                    "Command Center",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CommandButton(
                            "Virtual Controller",
                            Icons.Rounded.SportsEsports,
                            MaterialTheme.colorScheme.primary,
                            { navController.navigate("gamepad") },
                            Modifier.weight(1f)
                        )
                        CommandButton(
                            "Connections",
                            Icons.Rounded.Hub,
                            NeonPalette.Cyan,
                            { navController.navigate("connections") },
                            Modifier.weight(1f)
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CommandButton(
                            "HUD Editor",
                            Icons.Rounded.DashboardCustomize,
                            MaterialTheme.colorScheme.secondary,
                            { navController.navigate("editor") },
                            Modifier.weight(1f)
                        )
                        CommandButton(
                            "Settings",
                            Icons.Rounded.Settings,
                            MaterialTheme.colorScheme.primaryContainer,
                            { navController.navigate("settings") },
                            Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderRow(isConnected: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            "Ready to Play",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(50))
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .semantics { contentDescription = "Connection status: connected" },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isConnected) NeonPalette.ConnectedDot else Color.Red)
            )
            Text(if (isConnected) "Connected" else "Disconnected", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

// ---------------------------------------------------------------------------
// Device Hero Card — driven by one sealed state so Searching/Found/Connected
// are separate composables that crossfade+scale into each other, instead of
// one big if/else blob that just snapped between layouts.
// ---------------------------------------------------------------------------

private data class HeroConnectionOption(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val buttonText: String,
    val onConnect: () -> Unit
)

private sealed class DeviceCardState {
    object Searching : DeviceCardState()
    data class Available(val options: List<HeroConnectionOption>) : DeviceCardState()
    data class Connected(val name: String, val stats: com.sanket.tools.nexpad.viewmodel.ConnectionStats) : DeviceCardState()

    // Coarse key for AnimatedContent — stops the transition from re-firing
    // when e.g. the server list reorders but we're still "Available".
    val phase: Int get() = when (this) {
        is Searching -> 0
        is Available -> 1
        is Connected -> 2
    }
}

@Composable
private fun DeviceHeroCard(
    isConnected: Boolean,
    servers: List<DiscoveredServer>,
    stats: com.sanket.tools.nexpad.viewmodel.ConnectionStats,
    isAoaAttached: Boolean,
    isAdbAvailable: Boolean,
    adbServerName: String?,
    onConnectServer: (DiscoveredServer) -> Unit,
    onConnectAoa: () -> Unit,
    onConnectAdb: () -> Unit,
    onDisconnectClick: () -> Unit,
    onOpenConnectionHub: () -> Unit
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

    GlassCard(modifier = Modifier.fillMaxWidth()) {
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
private fun SearchingContent(onOpenConnectionHub: () -> Unit) {
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
private fun RadarScanner(size: Dp) {
    val infinite = rememberInfiniteTransition(label = "radar")
    val sweepAngle by infinite.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing)),
        label = "sweep"
    )
    val pulse by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing)),
        label = "pulse"
    )

    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.toPx() / 2f
            repeat(3) { ring ->
                val phase = (pulse + ring / 3f) % 1f
                drawCircle(
                    color = NeonPalette.Cyan.copy(alpha = (1f - phase) * 0.35f),
                    radius = radius * phase,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
            drawCircle(color = NeonPalette.Cyan.copy(alpha = 0.08f), radius = radius)
            drawCircle(color = NeonPalette.Cyan.copy(alpha = 0.3f), radius = radius, style = Stroke(width = 1.5.dp.toPx()))
            rotate(sweepAngle) {
                drawArc(
                    brush = Brush.sweepGradient(listOf(Color.Transparent, NeonPalette.Cyan.copy(alpha = 0.5f))),
                    startAngle = 0f,
                    sweepAngle = 90f,
                    useCenter = true,
                    size = this.size
                )
            }
        }
        Icon(
            imageVector = Icons.Rounded.Computer,
            contentDescription = null,
            tint = NeonPalette.Cyan.copy(alpha = 0.7f),
            modifier = Modifier.size(size * 0.32f)
        )
    }
}

@Composable
private fun AvailableConnectionsContent(
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
private fun HeroOptionCard(option: HeroConnectionOption) {
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
private fun ConnectedContent(name: String, stats: com.sanket.tools.nexpad.viewmodel.ConnectionStats, onDisconnectClick: () -> Unit) {
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
            LiveStat(modifier = Modifier.weight(1.0f), label = "Jitter", value = stats.oneWayJitterMs?.let { String.format(java.util.Locale.US, "±%.1f", it) } ?: "--", unit = "ms")
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
private fun LiveStat(modifier: Modifier = Modifier, label: String, value: String, unit: String = "") {
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
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
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
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
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
private fun StatDivider() {
    Box(
        modifier = Modifier
            .height(28.dp)
            .width(1.dp)
            .background(Color.White.copy(alpha = 0.1f))
    )
}
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)
                    )
                )
            )
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
            .border(2.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(28.dp)),
        content = content
    )
}

@Composable
fun StatBox(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
        Text(value, style = MaterialTheme.typography.titleLarge, color = valueColor)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandButton(label: String, icon: ImageVector, iconColor: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.height(112.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VShapedPanel(onPlayClick: () -> Unit) {
    var selectedIndex by remember { mutableIntStateOf(0) }
    val state = rememberCarouselState { layoutOptions.size }

    // No need for a separate CoroutineScope + launch here — LaunchedEffect
    // already gives us a coroutine, and the assignment itself is synchronous.
    LaunchedEffect(state.currentItem) {
        selectedIndex = state.currentItem
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            //.height(350.dp)
            .aspectRatio(1.1f)
    ) {
        VPanelBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            HorizontalCenteredHeroCarousel(
                state = state,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(221.dp)
                    .padding(24.dp),
                itemSpacing = 8.dp,
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) { index ->
                val item = layoutOptions[index]
                InnerLayoutCard(
                    modifier = Modifier.fillMaxSize().padding(4.dp),
                    title = item.title,
                    subtitle = item.subtitle,
                    isSelected = selectedIndex == index,
                )
            }
          Box(
              modifier = Modifier.fillMaxSize()
          ) {
              PlayButton(
                  modifier = Modifier
                      .align(alignment = Alignment.Center)
                      .padding(bottom = 24.dp),
                  onClick = onPlayClick
              )
          }

        }
    }
}

/** Draws the glowing V-shaped chassis behind the carousel. */
@Composable
private fun VPanelBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width, size.height * 0.75f)
            lineTo(size.width * 0.5f, size.height)
            lineTo(0f, size.height * 0.75f)
            close()
        }
        drawPath(
            path = path,
            brush = Brush.linearGradient(colors = listOf(NeonPalette.PanelBgTop, NeonPalette.PanelBgBottom))
        )
        drawPath(
            path = path,
            brush = Brush.linearGradient(
                colors = listOf(
                    NeonPalette.Cyan.copy(alpha = 0.3f),
                    NeonPalette.Purple.copy(alpha = 0.3f),
                    NeonPalette.Cyan.copy(alpha = 0.3f)
                )
            ),
            style = Stroke(width = 8.dp.toPx())
        )
        drawPath(
            path = path,
            brush = Brush.linearGradient(colors = listOf(NeonPalette.Cyan, NeonPalette.Purple, NeonPalette.Cyan)),
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

@Composable
fun PlayButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {

    val infinite = rememberInfiniteTransition(label = "")

    val glowAlpha by infinite.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 900,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = ""
    )

    val scale by infinite.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = ""
    )
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                onClick() },
        contentAlignment = Alignment.Center
    )
    {

        Canvas(
            modifier = Modifier.size(80.dp)
        ) {

            drawRoundRect(
                color = NeonPalette.Green.copy(alpha = 0.08f * glowAlpha),
                topLeft = Offset(
                    x = -size.width * 0.25f,
                    y = -size.height * 0.10f
                ),
                size = Size(
                    width = size.width * 1.5f ,
                    height = size.height * 1.2f
                ),
                cornerRadius = CornerRadius(50.dp.toPx())
            )
            //-----------------------------------
            // Triangle
            //-----------------------------------

            val path = Path().apply {
                moveTo(size.width * .3f, size.height * .22f)

                lineTo(size.width * .85f, size.height * .5f)

                lineTo(size.width * .3f, size.height * .78f)
                close()
            }

            drawPath(path = path, color = NeonPalette.Green.copy(alpha = 0.4f), style = Stroke(width = 16.dp.toPx()))
            drawPath(path = path, color = NeonPalette.Green.copy(alpha = 0.7f), style = Stroke(width = 8.dp.toPx()))
            drawPath(path = path, color = NeonPalette.Green)
        }
    }
}
private val cardAnimSpecDp = tween<Dp>(durationMillis = 280, easing = FastOutSlowInEasing)
private val cardAnimSpecFloat = tween<Float>(durationMillis = 280, easing = FastOutSlowInEasing)
private val cardAnimSpecColor = tween<Color>(durationMillis = 280, easing = FastOutSlowInEasing)

@Composable
fun InnerLayoutCard(modifier: Modifier, title: String, subtitle: String, isSelected: Boolean) {
    // Every visual property below is animated off the SAME isSelected flag, so
    // swiping left vs right produces identical motion either way — the carousel
    // just decides which index gets isSelected = true, this composable only
    // reacts to that boolean and doesn't care which direction it came from.
    val width by animateDpAsState(
        targetValue = if (isSelected) 140.dp else 100.dp,
        animationSpec = cardAnimSpecDp,
        label = "cardWidth"
    )
    val height by animateDpAsState(
        targetValue = if (isSelected) 160.dp else 120.dp,
        animationSpec = cardAnimSpecDp,
        label = "cardHeight"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (isSelected) 2.dp else 2.dp,
        animationSpec = cardAnimSpecDp,
        label = "borderWidth"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) NeonPalette.Cyan else NeonPalette.CardIdleBorder,
        animationSpec = cardAnimSpecColor,
        label = "borderColor"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else NeonPalette.CardIdleText,
        animationSpec = cardAnimSpecColor,
        label = "textColor"
    )
    val bgStart by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF005577) else NeonPalette.CardIdleBg,
        animationSpec = cardAnimSpecColor,
        label = "bgStart"
    )
    val bgEnd by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF660088) else NeonPalette.CardIdleBg,
        animationSpec = cardAnimSpecColor,
        label = "bgEnd"
    )
    val titleFontSize by animateFloatAsState(
        targetValue = if (isSelected) 16f else 12f,
        animationSpec = cardAnimSpecFloat,
        label = "titleFontSize"
    )
    // Subtitle stays composed at all times and just fades — swapping it in/out
    // with an if() is what caused the old abrupt pop when selection changed.
    val subtitleAlpha by animateFloatAsState(
        targetValue = if (isSelected) 0.9f else 0f,
        animationSpec = cardAnimSpecFloat,
        label = "subtitleAlpha"
    )

    Box(
        modifier = modifier
            .size(width, height)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(bgStart, bgEnd)))
            .border(borderWidth, borderColor, RoundedCornerShape(16.dp))
            .semantics { contentDescription = "$title layout${if (isSelected) ", selected" else ""}" },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(
                text = title,
                color = textColor,
                fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                fontSize = titleFontSize.sp,
                style = if (isSelected) {
                    TextStyle(shadow = Shadow(color = Color.Black, offset = Offset(0f, 4f), blurRadius = 8f))
                } else {
                    TextStyle.Default
                }
            )
            Text(
                subtitle,
                color = textColor.copy(alpha = subtitleAlpha),
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
@Composable
fun CyberGrid(
    modifier: Modifier = Modifier,
    gridSize: Dp = 28.dp,
    lineColor: Color = Color(0xFF00E5FF).copy(alpha = 0.08f)
) {
    Canvas(modifier) {

        val step = gridSize.toPx()

        // Vertical
        var x = 0f
        while (x <= size.width) {
            drawLine(
                color = lineColor,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1.dp.toPx()
            )
            x += step
        }

        // Horizontal
        var y = 0f
        while (y <= size.height) {
            drawLine(
                color = lineColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx()
            )
            y += step
        }
    }
}
@Composable
fun ScanLine(
    modifier: Modifier = Modifier
) {

    val transition = rememberInfiniteTransition()

    val offset by transition.animateFloat(
        initialValue = -200f,
        targetValue = 2000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 6000,
                easing = LinearEasing
            )
        )
    )

    Canvas(modifier) {

        drawRect(

            brush = Brush.verticalGradient(

                listOf(
                    Color.Transparent,
                    Color(0xFF00E5FF).copy(alpha = .12f),
                    Color.Transparent
                ),

                startY = offset,
                endY = offset + 120.dp.toPx()

            )

        )

    }

}
