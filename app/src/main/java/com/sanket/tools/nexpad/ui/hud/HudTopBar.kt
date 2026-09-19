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
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Save
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
import com.sanket.tools.nexpad.ui.theme.NeonPalette

/**
 * Top Navigation Bar for HUD Editor.
 * Draggable and movable vertically (up/down) with snap-to-dock toggle.
 */
@Composable
fun HudTopBar(
    profileName: String,
    isDefault: Boolean,
    hasUnsavedChanges: Boolean,
    onBack: () -> Unit,
    onOpenPalette: () -> Unit,
    onSave: () -> Unit,
    isDragging: Boolean = false,
    onDragStart: () -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDragY: (Float) -> Unit = {},
    onToggleDock: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val borderColor = if (isDragging) NeonPalette.Cyan else Color.White.copy(alpha = 0.15f)
    val borderWidth = if (isDragging) 1.5.dp else 1.dp
    val shadowElevation = if (isDragging) 16.dp else 8.dp

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = if (isDragging) 0.98f else 0.92f),
        border = androidx.compose.foundation.BorderStroke(borderWidth, borderColor),
        shadowElevation = shadowElevation
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Back button + Profile Name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }

                    Column(
                        modifier = Modifier.pointerInput(Unit) {
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = profileName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                            )
                            if (isDefault) {
                                Surface(
                                    shape = CircleShape,
                                    color = NeonPalette.Cyan.copy(alpha = 0.15f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonPalette.Cyan.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        "DEFAULT",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonPalette.Cyan,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = if (hasUnsavedChanges) "• Unsaved changes" else "Touch button to customize",
                            fontSize = 10.sp,
                            color = if (hasUnsavedChanges) Color(0xFFFFB703) else MaterialTheme.colorScheme.onSurfaceVariant
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
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Icon(
                        Icons.Rounded.DragHandle,
                        contentDescription = "Drag up/down or tap to flip",
                        tint = if (isDragging) NeonPalette.Cyan else Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "MOVE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isDragging) NeonPalette.Cyan else Color.White.copy(alpha = 0.8f)
                    )
                }

                // Right: Actions (Buttons Palette, Save)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenPalette,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Rounded.AddCircleOutline, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Buttons", fontSize = 11.sp)
                    }

                    Button(
                        onClick = onSave,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPalette.Cyan),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Rounded.Save, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("SAVE", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Bottom edge drag indicator strip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
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
                        .width(36.dp)
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(if (isDragging) NeonPalette.Cyan else Color.White.copy(alpha = 0.25f))
                )
            }
        }
    }
}
