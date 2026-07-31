package com.sanket.tools.nexpad.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.sanket.tools.nexpad.utils.LayoutManager


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
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(28.dp)),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, layoutManager: LayoutManager) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = @Composable {
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
                }
                        ,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text("Ready to Play", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
                
                // Connection Chip
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF34D399)))
                    Text("Connected", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                }
            }

            // Master V-Shaped Layout Selector Panel
            var selectedLayout by remember { mutableStateOf("Classic Pro") }
            
            VShapedPanel(
                selectedLayout = selectedLayout,
                onLayoutSelected = { selectedLayout = it },
                onPlayClick = { navController.navigate("gamepad") }
            )

            // Hero Card
            GlassCard(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                Column(modifier = Modifier.padding(24.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("💻", fontSize = 32.sp)
                        }
                        Column {
                            Text("Windows PC", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
                            Text("Main Rig • Local Network", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        StatBox("Signal", "98%", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                        StatBox("Latency", "2ms", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                        StatBox("Battery", "85%", MaterialTheme.colorScheme.onBackground, Modifier.weight(1f))
                    }
                }
            }

            // Command Center
            Text("Command Center", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                    CommandButton("Connect Device", "🔗", MaterialTheme.colorScheme.primary, { navController.navigate("gamepad") }, Modifier.weight(1f))
                    CommandButton("Virtual Controller", "🎮", MaterialTheme.colorScheme.tertiary, { navController.navigate("gamepad") }, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                    CommandButton("HUD Editor", "🎛️", MaterialTheme.colorScheme.secondary, { navController.navigate("editor") }, Modifier.weight(1f))
                    CommandButton("Settings", "⚙️", MaterialTheme.colorScheme.primaryContainer, { navController.navigate("settings") }, Modifier.weight(1f))
                }
            }
        }
    }
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
fun CommandButton(label: String, icon: String, iconColor: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.height(112.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(icon, fontSize = 28.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun VShapedPanel(
    selectedLayout: String,
    onLayoutSelected: (String) -> Unit,
    onPlayClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(380.dp)
            .padding(vertical = 16.dp)
    ) {
        // Draw the Glowing V-Shape Container Border
        Canvas(modifier = Modifier.fillMaxSize()) {
            val path = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width, size.height * 0.75f)
                lineTo(size.width * 0.5f, size.height)
                lineTo(0f, size.height * 0.75f)
                close()
            }
            // Draw background fill (Deep Cyberpunk Dark)
            drawPath(
                path = path,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF0E1524), Color(0xFF070B14))
                )
            )
            // Draw neon glow (thick, low alpha)
            drawPath(
                path = path,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF00E5FF).copy(alpha = 0.3f), Color(0xFFB400FF).copy(alpha = 0.3f), Color(0xFF00E5FF).copy(alpha = 0.3f))
                ),
                style = Stroke(width = 12.dp.toPx())
            )
            // Draw core neon border (thick, high alpha)
            drawPath(
                path = path,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF00E5FF), Color(0xFFB400FF), Color(0xFF00E5FF))
                ),
                style = Stroke(width = 4.dp.toPx())
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // The LazyRow Carousel Inside the Panel
            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 30.dp, bottom = 20.dp)
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(8.dp))
                InnerLayoutCard(
                    title = "Classic Pro",
                    subtitle = "Layout 1",
                    isSelected = selectedLayout == "Classic Pro",
                    onClick = { onLayoutSelected("Classic Pro") }
                )
                InnerLayoutCard(
                    title = "FPS Master",
                    subtitle = "Layout 2",
                    isSelected = selectedLayout == "FPS Master",
                    onClick = { onLayoutSelected("FPS Master") }
                )
                InnerLayoutCard(
                    title = "Racing Sim",
                    subtitle = "Layout 3",
                    isSelected = selectedLayout == "Racing Sim",
                    onClick = { onLayoutSelected("Racing Sim") }
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
        }

        // The Play Button inside the V-pocket
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-40).dp)
                .clickable { onPlayClick() }
        ) {
            Canvas(modifier = Modifier.size(50.dp, 40.dp)) {
                val path = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width, size.height / 2f)
                    lineTo(0f, size.height)
                    close()
                }
                // Outer Glow
                drawPath(path = path, color = Color(0xFF39FF14).copy(alpha = 0.4f), style = Stroke(width = 16.dp.toPx()))
                // Inner Glow
                drawPath(path = path, color = Color(0xFF39FF14).copy(alpha = 0.7f), style = Stroke(width = 8.dp.toPx()))
                // Core
                drawPath(path = path, color = Color(0xFF39FF14))
            }
        }
    }
}

@Composable
fun InnerLayoutCard(title: String, subtitle: String, isSelected: Boolean, onClick: () -> Unit) {
    val width = if (isSelected) 140.dp else 100.dp
    val height = if (isSelected) 160.dp else 120.dp
    
    // Cyberpunk specific colors
    val bgColor = if (isSelected) Brush.linearGradient(listOf(Color(0xFF005577), Color(0xFF660088))) else Brush.linearGradient(listOf(Color(0xFF111111), Color(0xFF111111)))
    val borderColor = if (isSelected) Color.White else Color(0xFF333333)
    val borderWidth = if (isSelected) 3.dp else 2.dp
    val textColor = if (isSelected) Color.White else Color(0xFF888888)
    
    Box(
        modifier = Modifier
            .size(width, height)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(borderWidth, borderColor, RoundedCornerShape(16.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(
                text = title, 
                color = textColor, 
                fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold, 
                fontSize = if (isSelected) 16.sp else 12.sp,
                // Add text shadow for selected state
                style = if (isSelected) androidx.compose.ui.text.TextStyle(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = Color.Black,
                        offset = androidx.compose.ui.geometry.Offset(0f, 4f),
                        blurRadius = 8f
                    )
                ) else androidx.compose.ui.text.TextStyle.Default
            )
            if (isSelected) {
                Text(subtitle, color = textColor.copy(alpha = 0.9f), fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}
