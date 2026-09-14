package com.sanket.tools.nexpad.ui.studio.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.Check
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
 * Uses bounded 34dp parent containers so the 80dp buttons do not overlap each other.
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
        Box(
            modifier = Modifier.size(96.dp),
            contentAlignment = Alignment.Center
        ) {
            // Y at Top
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(34.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .requiredSize(80.dp)
                        .graphicsLayer { scaleX = 0.40f; scaleY = 0.40f },
                    contentAlignment = Alignment.Center
                ) {
                    when (style) {
                        AbxyPreviewStyle.REALISTIC_3D -> ControllerElementRenderer("Y", isConnected = false, isRgbEnabled = true, viewModel = dummyViewModel)
                        AbxyPreviewStyle.SCIFI_HEX -> ControllerElementRenderer("Y", isConnected = false, isRgbEnabled = true, viewModel = dummyViewModel, customComponentId = "builtin.scifi_hex_y")
                        AbxyPreviewStyle.CYBER_OCTA -> ControllerElementRenderer("Y", isConnected = false, isRgbEnabled = true, viewModel = dummyViewModel, customComponentId = "builtin.cyber_octa_y")
                        AbxyPreviewStyle.GAMEPAD_BUTTON -> GamepadButton("Y", isConnected = false, onVibrate = {}, viewModel = dummyViewModel)
                    }
                }
            }

            // X at Left
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(34.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .requiredSize(80.dp)
                        .graphicsLayer { scaleX = 0.40f; scaleY = 0.40f },
                    contentAlignment = Alignment.Center
                ) {
                    when (style) {
                        AbxyPreviewStyle.REALISTIC_3D -> ControllerElementRenderer("X", isConnected = false, isRgbEnabled = true, viewModel = dummyViewModel)
                        AbxyPreviewStyle.SCIFI_HEX -> ControllerElementRenderer("X", isConnected = false, isRgbEnabled = true, viewModel = dummyViewModel, customComponentId = "builtin.scifi_hex_x")
                        AbxyPreviewStyle.CYBER_OCTA -> ControllerElementRenderer("X", isConnected = false, isRgbEnabled = true, viewModel = dummyViewModel, customComponentId = "builtin.cyber_octa_x")
                        AbxyPreviewStyle.GAMEPAD_BUTTON -> GamepadButton("X", isConnected = false, onVibrate = {}, viewModel = dummyViewModel)
                    }
                }
            }

            // B at Right
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(34.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .requiredSize(80.dp)
                        .graphicsLayer { scaleX = 0.40f; scaleY = 0.40f },
                    contentAlignment = Alignment.Center
                ) {
                    when (style) {
                        AbxyPreviewStyle.REALISTIC_3D -> ControllerElementRenderer("B", isConnected = false, isRgbEnabled = true, viewModel = dummyViewModel)
                        AbxyPreviewStyle.SCIFI_HEX -> ControllerElementRenderer("B", isConnected = false, isRgbEnabled = true, viewModel = dummyViewModel, customComponentId = "builtin.scifi_hex_b")
                        AbxyPreviewStyle.CYBER_OCTA -> ControllerElementRenderer("B", isConnected = false, isRgbEnabled = true, viewModel = dummyViewModel, customComponentId = "builtin.cyber_octa_b")
                        AbxyPreviewStyle.GAMEPAD_BUTTON -> GamepadButton("B", isConnected = false, onVibrate = {}, viewModel = dummyViewModel)
                    }
                }
            }

            // A at Bottom
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .size(34.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .requiredSize(80.dp)
                        .graphicsLayer { scaleX = 0.40f; scaleY = 0.40f },
                    contentAlignment = Alignment.Center
                ) {
                    when (style) {
                        AbxyPreviewStyle.REALISTIC_3D -> ControllerElementRenderer("A", isConnected = false, isRgbEnabled = true, viewModel = dummyViewModel)
                        AbxyPreviewStyle.SCIFI_HEX -> ControllerElementRenderer("A", isConnected = false, isRgbEnabled = true, viewModel = dummyViewModel, customComponentId = "builtin.scifi_hex_a")
                        AbxyPreviewStyle.CYBER_OCTA -> ControllerElementRenderer("A", isConnected = false, isRgbEnabled = true, viewModel = dummyViewModel, customComponentId = "builtin.cyber_octa_a")
                        AbxyPreviewStyle.GAMEPAD_BUTTON -> GamepadButton("A", isConnected = false, onVibrate = {}, viewModel = dummyViewModel)
                    }
                }
            }
        }
    }
}

/**
 * Cohesive 4-Button ABXY Diamond Group Card.
 * Represents ABXY as one unified cluster theme.
 */
@Composable
fun AbxyDiamondGroupCard(
    theme: AbxyGroupTheme,
    mode: ButtonStudioMode,
    isSelected: Boolean,
    isAppliedToActiveProfile: Boolean = false,
    dummyViewModel: GamepadViewModel,
    onSelectGroup: () -> Unit,
    onApplyToProfile: () -> Unit = {},
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
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Left: Diamond Cluster Preview
            AbxyDiamondPreview(
                style = theme.previewStyle,
                dummyViewModel = dummyViewModel
            )

            // Right: Information & Action Button
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        theme.name,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color.White),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Spacer(Modifier.width(6.dp))

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
                    text = "4-Button Unified Cluster • Y, X, B, A",
                    color = NeonPalette.Cyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    theme.description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 14.sp,
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
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
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
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Apply to Profile button (stays in studio)
                        Button(
                            onClick = onApplyToProfile,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isAppliedToActiveProfile) NeonPalette.Cyan.copy(alpha = 0.25f) else NeonPalette.Cyan
                            ),
                            border = if (isAppliedToActiveProfile) androidx.compose.foundation.BorderStroke(1.dp, NeonPalette.Cyan) else null,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(
                                if (isAppliedToActiveProfile) Icons.Rounded.Check else Icons.Rounded.DashboardCustomize,
                                contentDescription = null,
                                tint = if (isAppliedToActiveProfile) NeonPalette.Cyan else Color.Black,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                if (isAppliedToActiveProfile) "Active on Profile" else "Apply 4 to Profile",
                                color = if (isAppliedToActiveProfile) NeonPalette.Cyan else Color.Black,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Use in HUD button (navigates to editor)
                        OutlinedButton(
                            onClick = onUseInHud,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Open in HUD ➔", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
