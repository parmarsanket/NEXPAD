package com.sanket.tools.nexpad.ui.hud

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.model.HudElement
import com.sanket.tools.nexpad.model.LayoutSkin
import com.sanket.tools.nexpad.ui.theme.NeonPalette
import kotlin.math.roundToInt

/**
 * Docked Control Panel (Inspector) for the selected HUD element.
 * Provides fine-tuning nudge arrows, scale, opacity, category-safe skins, and reset/remove.
 * Draggable and movable vertically (up/down) with snap-to-dock toggle.
 */
@Composable
fun HudDockedInspector(
    element: HudElement,
    compatibleSkins: List<LayoutSkin>,
    screenWidthPx: Float,
    screenHeightPx: Float,
    onClose: () -> Unit,
    onNudge: (Float, Float) -> Unit,
    onScaleChange: (Float) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onCycleSkin: () -> Unit,
    onOpenStudio: () -> Unit,
    onResetPos: () -> Unit,
    onRemove: () -> Unit,
    isDragging: Boolean = false,
    onDragStart: () -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDragY: (Float) -> Unit = {},
    onToggleDock: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val transform = element.transform
    val borderColor = if (isDragging) NeonPalette.Cyan else NeonPalette.Cyan.copy(alpha = 0.5f)
    val borderWidth = if (isDragging) 1.5.dp else 1.dp
    val shadowElevation = if (isDragging) 20.dp else 12.dp

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF0F172A).copy(alpha = if (isDragging) 0.98f else 0.95f),
        border = androidx.compose.foundation.BorderStroke(borderWidth, borderColor),
        shadowElevation = shadowElevation
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Top Drag Handle Pill Strip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = { onDragStart() },
                            onDragEnd = { onDragEnd() },
                            onDragCancel = { onDragEnd() },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                onDragY(dragAmount)
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(44.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(if (isDragging) NeonPalette.Cyan else Color.White.copy(alpha = 0.35f))
                )
            }

            // Row 1: Header + Move Handle + Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragStart = { onDragStart() },
                                onDragEnd = { onDragEnd() },
                                onDragCancel = { onDragEnd() },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    onDragY(dragAmount)
                                }
                            )
                        }
                ) {
                    val emoji = element.emoji
                    Text(
                        text = "${if (emoji.isNotBlank()) "$emoji " else ""}${element.displayName} (${element.categoryTitle})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = NeonPalette.Cyan)
                    )

                    val currentSkin = compatibleSkins.firstOrNull { it.id == element.skinId }
                    val isCustom = element.skinId != null && !element.skinId.startsWith("builtin.default_")
                    val isMissingAsset = isCustom && currentSkin == null
                    val skinLabel = when {
                        !isCustom -> "Default"
                        isMissingAsset -> "Missing"
                        else -> currentSkin?.name ?: "Custom"
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = NeonPalette.Purple.copy(alpha = 0.18f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonPalette.Purple.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "Skin: $skinLabel",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NeonPalette.Purple,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (isMissingAsset) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFE65100).copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE65100))
                        ) {
                            Text(
                                text = "⚠ Missing asset — using fallback",
                                fontSize = 10.sp,
                                color = Color(0xFFFFB703),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        Text(
                            text = "X: ${(transform.xRatio * 100).roundToInt()}%  Y: ${(transform.yRatio * 100).roundToInt()}%",
                            fontSize = 11.sp,
                            color = Color.LightGray
                        )
                    }
                }

                // Center: Dedicated Drag Handle Affordance ("MOVE" pill)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDragging) NeonPalette.Cyan.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.08f))
                        .border(
                            1.dp,
                            if (isDragging) NeonPalette.Cyan else Color.White.copy(alpha = 0.15f),
                            RoundedCornerShape(8.dp)
                        )
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { onToggleDock() }
                            )
                        }
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragStart = { onDragStart() },
                                onDragEnd = { onDragEnd() },
                                onDragCancel = { onDragEnd() },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    onDragY(dragAmount)
                                }
                            )
                        }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        Icons.Rounded.DragHandle,
                        contentDescription = "Drag up/down or tap to flip",
                        tint = if (isDragging) NeonPalette.Cyan else Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "MOVE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isDragging) NeonPalette.Cyan else Color.White.copy(alpha = 0.8f)
                    )
                }

                IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close inspector", tint = Color.White)
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            // Row 2: Controls (Nudge, Scale, Opacity, Skin, Actions)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Nudge Arrows
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = { onNudge(-1f / screenWidthPx, 0f) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Nudge Left", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                        onClick = { onNudge(0f, -1f / screenHeightPx) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Rounded.ArrowUpward, contentDescription = "Nudge Up", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                        onClick = { onNudge(0f, 1f / screenHeightPx) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Rounded.ArrowDownward, contentDescription = "Nudge Down", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                        onClick = { onNudge(1f / screenWidthPx, 0f) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "Nudge Right", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }

                VerticalDivider(modifier = Modifier.height(28.dp), color = Color.White.copy(alpha = 0.1f))

                // 2. Scale Controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Scale: ${(transform.scale * 100).roundToInt()}%", fontSize = 11.sp, color = Color.White)
                    IconButton(
                        onClick = { onScaleChange(transform.scale - 0.05f) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Rounded.Remove, contentDescription = "Decrease Scale", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    Slider(
                        value = transform.scale,
                        onValueChange = onScaleChange,
                        valueRange = 0.5f..2.5f,
                        modifier = Modifier.width(90.dp),
                        colors = SliderDefaults.colors(thumbColor = NeonPalette.Cyan, activeTrackColor = NeonPalette.Cyan)
                    )
                    IconButton(
                        onClick = { onScaleChange(transform.scale + 0.05f) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "Increase Scale", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }

                VerticalDivider(modifier = Modifier.height(28.dp), color = Color.White.copy(alpha = 0.1f))

                // 3. Opacity Controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Alpha: ${(transform.opacity * 100).roundToInt()}%", fontSize = 11.sp, color = Color.White)
                    Slider(
                        value = transform.opacity,
                        onValueChange = onOpacityChange,
                        valueRange = 0.1f..1.0f,
                        modifier = Modifier.width(80.dp),
                        colors = SliderDefaults.colors(thumbColor = NeonPalette.Purple, activeTrackColor = NeonPalette.Purple)
                    )
                }

                VerticalDivider(modifier = Modifier.height(28.dp), color = Color.White.copy(alpha = 0.1f))

                // 4. Category-Safe Skin Selector
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val currentSkin = compatibleSkins.firstOrNull { it.id == element.skinId }
                    val isCustom = element.skinId != null && !element.skinId.startsWith("builtin.default_")
                    val isMissingAsset = isCustom && currentSkin == null

                    OutlinedButton(
                        onClick = onCycleSkin,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isMissingAsset) Color(0xFFFFB703) else Color.White
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isMissingAsset) Color(0xFFFFB703) else NeonPalette.Purple.copy(alpha = 0.6f)
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(
                            if (isMissingAsset) Icons.Rounded.Warning else Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = if (isMissingAsset) Color(0xFFFFB703) else NeonPalette.Purple,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Skin Change", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }

                    IconButton(
                        onClick = onOpenStudio,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Rounded.Palette, contentDescription = "Change Appearance", tint = NeonPalette.Purple, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(Modifier.weight(1f))

                // 5. Actions (Reset, Remove)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onResetPos,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Rounded.RestartAlt, contentDescription = "Reset default position", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                    }

                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove button", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
