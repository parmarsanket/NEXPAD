package com.sanket.tools.nexpad.ui

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
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.DashboardCustomize
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SportsEsports
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
fun HomeScreen(navController: NavController, layoutManager: LayoutManager) {
    val scrollState = rememberScrollState()

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
                HeaderRow()

                VShapedPanel(
                    onPlayClick = { navController.navigate("gamepad") }
                )
                Text(
                    "Online Device",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                DeviceHeroCard()

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
                            "Connect Device",
                            Icons.Rounded.Link,
                            MaterialTheme.colorScheme.primary,
                            { navController.navigate("device_scan") },
                            Modifier.weight(1f)
                        )
                        CommandButton(
                            "Virtual Controller",
                            Icons.Rounded.SportsEsports,
                            MaterialTheme.colorScheme.primary,
                            { navController.navigate("gamepad") },
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
private fun HeaderRow() {
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
                    .background(NeonPalette.ConnectedDot)
            )
            Text("Connected", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun DeviceHeroCard() {
    GlassCard(modifier = Modifier.fillMaxWidth().height(220.dp)) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Computer,
                        contentDescription = "PC",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Column {
                    Text("Windows PC", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
                    Text(
                        "Main Rig • Local Network",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatBox("Signal", "98%", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                StatBox("Latency", "2ms", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                StatBox("Battery", "85%", MaterialTheme.colorScheme.onBackground, Modifier.weight(1f))
            }
        }
    }
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