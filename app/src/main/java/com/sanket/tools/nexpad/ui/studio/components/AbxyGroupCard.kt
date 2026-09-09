package com.sanket.tools.nexpad.ui.studio.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DashboardCustomize
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.ui.components.controller.ControllerElementRenderer
import com.sanket.tools.nexpad.ui.components.controller.GamepadButton
import com.sanket.tools.nexpad.ui.studio.model.AbxyGroupTheme
import com.sanket.tools.nexpad.ui.studio.model.AbxyPreviewStyle
import com.sanket.tools.nexpad.ui.studio.model.ButtonStudioMode
import com.sanket.tools.nexpad.ui.theme.NeonPalette
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel

/**
 * Preview stage rendering the 4-button ABXY diamond cluster in the requested visual style.
 */
@Composable
fun AbxyDiamondPreview(
    style: AbxyPreviewStyle,
    dummyViewModel: GamepadViewModel,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(105.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF040810))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        when (style) {
            AbxyPreviewStyle.REALISTIC_3D -> {
                // 3D Realistic Y, X, B, A in diamond cluster
                Box(modifier = Modifier.size(86.dp)) {
                    Box(modifier = Modifier.align(Alignment.TopCenter).graphicsLayer { scaleX = 0.48f; scaleY = 0.48f }) {
                        ControllerElementRenderer("Y", isConnected = false, isRgbEnabled = true, viewModel = dummyViewModel)
                    }
                    Box(modifier = Modifier.align(Alignment.CenterStart).graphicsLayer { scaleX = 0.48f; scaleY = 0.48f }) {
                        ControllerElementRenderer("X", isConnected = false, isRgbEnabled = true, viewModel = dummyViewModel)
                    }
                    Box(modifier = Modifier.align(Alignment.CenterEnd).graphicsLayer { scaleX = 0.48f; scaleY = 0.48f }) {
                        ControllerElementRenderer("B", isConnected = false, isRgbEnabled = true, viewModel = dummyViewModel)
                    }
                    Box(modifier = Modifier.align(Alignment.BottomCenter).graphicsLayer { scaleX = 0.48f; scaleY = 0.48f }) {
                        ControllerElementRenderer("A", isConnected = false, isRgbEnabled = true, viewModel = dummyViewModel)
                    }
                }
            }
            AbxyPreviewStyle.SCIFI_HEX -> {
                // Sci-Fi Hex Y, X, B, A
                Box(modifier = Modifier.size(86.dp)) {
                    Box(modifier = Modifier.align(Alignment.TopCenter).graphicsLayer { scaleX = 0.48f; scaleY = 0.48f }) {
                        ControllerElementRenderer("Y", isConnected = false, isRgbEnabled = true, viewModel = dummyViewModel, customComponentId = "builtin.scifi_hex_y")
                    }
                    Box(modifier = Modifier.align(Alignment.CenterStart).graphicsLayer { scaleX = 0.48f; scaleY = 0.48f }) {
                        ControllerElementRenderer("X", isConnected = false, isRgbEnabled = true, viewModel = dummyViewModel, customComponentId = "builtin.scifi_hex_x")
                    }
                    Box(modifier = Modifier.align(Alignment.CenterEnd).graphicsLayer { scaleX = 0.48f; scaleY = 0.48f }) {
                        ControllerElementRenderer("B", isConnected = false, isRgbEnabled = true, viewModel = dummyViewModel, customComponentId = "builtin.scifi_hex_b")
                    }
                    Box(modifier = Modifier.align(Alignment.BottomCenter).graphicsLayer { scaleX = 0.48f; scaleY = 0.48f }) {
                        ControllerElementRenderer("A", isConnected = false, isRgbEnabled = true, viewModel = dummyViewModel, customComponentId = "builtin.scifi_hex_a")
                    }
                }
            }
            AbxyPreviewStyle.CYBER_OCTA -> {
                // Cyber Octa Y, X, B, A
                Box(modifier = Modifier.size(86.dp)) {
                    Box(modifier = Modifier.align(Alignment.TopCenter).graphicsLayer { scaleX = 0.48f; scaleY = 0.48f }) {
                        ControllerElementRenderer("Y", isConnected = false, isRgbEnabled = true, viewModel = dummyViewModel, customComponentId = "builtin.cyber_octa_y")
                    }
                    Box(modifier = Modifier.align(Alignment.CenterStart).graphicsLayer { scaleX = 0.48f; scaleY = 0.48f }) {
                        ControllerElementRenderer("X", isConnected = false, isRgbEnabled = true, viewModel = dummyViewModel, customComponentId = "builtin.cyber_octa_x")
                    }
                    Box(modifier = Modifier.align(Alignment.CenterEnd).graphicsLayer { scaleX = 0.48f; scaleY = 0.48f }) {
                        ControllerElementRenderer("B", isConnected = false, isRgbEnabled = true, viewModel = dummyViewModel, customComponentId = "builtin.cyber_octa_b")
                    }
                    Box(modifier = Modifier.align(Alignment.BottomCenter).graphicsLayer { scaleX = 0.48f; scaleY = 0.48f }) {
                        ControllerElementRenderer("A", isConnected = false, isRgbEnabled = true, viewModel = dummyViewModel, customComponentId = "builtin.cyber_octa_a")
                    }
                }
            }
            AbxyPreviewStyle.GAMEPAD_BUTTON -> {
                // Minimal Flat GamepadButton from GamepadButton.kt / ABXYLayout.kt
                Box(modifier = Modifier.size(86.dp)) {
                    Box(modifier = Modifier.align(Alignment.TopCenter).graphicsLayer { scaleX = 0.46f; scaleY = 0.46f }) {
                        GamepadButton("Y", isConnected = false, onVibrate = {}, viewModel = dummyViewModel)
                    }
                    Box(modifier = Modifier.align(Alignment.CenterStart).graphicsLayer { scaleX = 0.46f; scaleY = 0.46f }) {
                        GamepadButton("X", isConnected = false, onVibrate = {}, viewModel = dummyViewModel)
                    }
                    Box(modifier = Modifier.align(Alignment.CenterEnd).graphicsLayer { scaleX = 0.46f; scaleY = 0.46f }) {
                        GamepadButton("B", isConnected = false, onVibrate = {}, viewModel = dummyViewModel)
                    }
                    Box(modifier = Modifier.align(Alignment.BottomCenter).graphicsLayer { scaleX = 0.46f; scaleY = 0.46f }) {
                        GamepadButton("A", isConnected = false, onVibrate = {}, viewModel = dummyViewModel)
                    }
                }
            }
        }
    }
}

/**
 * Cohesive 4-Button ABXY Diamond Group Card.
 * Represents ABXY as one unified cluster theme (no individual buttons).
 */
@Composable
fun AbxyDiamondGroupCard(
    theme: AbxyGroupTheme,
    mode: ButtonStudioMode,
    isSelected: Boolean,
    dummyViewModel: GamepadViewModel,
    onSelectGroup: () -> Unit,
    onUseInHud: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = if (mode == ButtonStudioMode.SELECTION && isSelected) 2.dp else 1.dp,
                color = if (mode == ButtonStudioMode.SELECTION && isSelected) NeonPalette.Cyan else Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(14.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left: Diamond Cluster Preview
            AbxyDiamondPreview(
                style = theme.previewStyle,
                dummyViewModel = dummyViewModel
            )

            // Right: Information & Action Button
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        theme.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                    )

                    // Type Badge
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = theme.type.badgeBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, theme.type.badgeColor.copy(alpha = 0.6f))
                    ) {
                        Text(
                            theme.type.label,
                            color = theme.type.badgeColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = "4-Button Unified Cluster • Y (Top), X (Left), B (Right), A (Bottom)",
                    color = NeonPalette.Cyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    theme.description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (mode == ButtonStudioMode.SELECTION) {
                    Button(
                        onClick = onSelectGroup,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) NeonPalette.Cyan else Color.White.copy(alpha = 0.12f)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(
                            if (isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.AddCircleOutline,
                            contentDescription = null,
                            tint = if (isSelected) Color.Black else Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (isSelected) "Cluster Active (Y, X, B, A) ✓" else "Select Cluster (All 4)",
                            color = if (isSelected) Color.Black else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Button(
                        onClick = onUseInHud,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPalette.Cyan),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Rounded.DashboardCustomize, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Use All 4 in HUD", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
