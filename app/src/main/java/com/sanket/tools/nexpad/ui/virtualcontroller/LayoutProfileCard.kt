package com.sanket.tools.nexpad.ui.virtualcontroller

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.model.LayoutProfile
import com.sanket.tools.nexpad.ui.theme.NeonPalette

/**
 * 60 FPS animated, reorderable profile card with pick-up elevation, drag handle icon,
 * preset lock badges, dynamic button count chips, and contextual action buttons.
 */
@Composable
fun LayoutProfileCard(
    modifier: Modifier = Modifier,
    dragHandleModifier: Modifier = Modifier,
    profile: LayoutProfile,
    isActive: Boolean,
    isDragging: Boolean = false,
    onSetActive: () -> Unit,
    onPlay: () -> Unit,
    onEditHud: () -> Unit,
    onOpenStudio: () -> Unit,
    onDuplicate: () -> Unit,
    onRename: () -> Unit,
    onShare: () -> Unit,
    onReset: () -> Unit,
    onDelete: () -> Unit
) {
    val borderColor = if (isDragging) NeonPalette.Cyan else if (isActive) NeonPalette.Cyan else Color.White.copy(alpha = 0.12f)
    val borderWidth = if (isDragging) 2.5.dp else if (isActive) 2.dp else 1.dp

    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.025f else 1.0f,
        animationSpec = spring(stiffness = 400f),
        label = "cardDragScale"
    )
    val elevation by animateDpAsState(
        targetValue = if (isDragging) 12.dp else 0.dp,
        animationSpec = spring(stiffness = 400f),
        label = "cardDragElevation"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                shadowElevation = elevation.toPx()
                shape = RoundedCornerShape(16.dp)
                clip = false
            }
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isDragging) MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
                else MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)
            )
            .border(borderWidth, borderColor, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header Row: Title + Badges + Drag Handle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        profile.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    if (!profile.isDefault) {
                        IconButton(
                            onClick = onRename,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Edit,
                                contentDescription = "Rename layout",
                                tint = NeonPalette.Cyan.copy(alpha = 0.7f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    // Protection badge
                    if (profile.isDefault) {
                        Surface(
                            shape = CircleShape,
                            color = NeonPalette.Cyan.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonPalette.Cyan.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Lock,
                                    contentDescription = "Default layout locked",
                                    tint = NeonPalette.Cyan,
                                    modifier = Modifier.size(10.dp)
                                )
                                Text(
                                    "DEFAULT",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonPalette.Cyan
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFFB703).copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB703).copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Star,
                                    contentDescription = "Custom layout",
                                    tint = Color(0xFFFFB703),
                                    modifier = Modifier.size(10.dp)
                                )
                                Text(
                                    "CUSTOM",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFB703)
                                )
                            }
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Active badge
                    if (isActive) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF00FF66).copy(alpha = 0.18f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00FF66))
                        ) {
                            Text(
                                "ACTIVE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF00FF66),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Drag Handle affordance icon with generous touch target
                    Box(
                        modifier = dragHandleModifier.size(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.DragHandle,
                            contentDescription = "Drag to reorder",
                            tint = if (isDragging) NeonPalette.Cyan else Color.White.copy(alpha = 0.45f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Description
            if (profile.description.isNotBlank()) {
                Text(
                    profile.description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            // Button Pills Preview Row
            val buttonKeys = profile.positions.keys.toList()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.White.copy(alpha = 0.06f)
                ) {
                    Text(
                        "${buttonKeys.size} Buttons",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                buttonKeys.take(12).forEach { key ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.White.copy(alpha = 0.04f)
                    ) {
                        Text(
                            key,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }
                }
                if (buttonKeys.size > 12) {
                    Text(
                        "+${buttonKeys.size - 12} more",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            // Action Buttons
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isActive) {
                    item {
                        OutlinedButton(
                            onClick = onSetActive,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonPalette.Cyan),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonPalette.Cyan.copy(alpha = 0.7f)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Set Active", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                item {
                    Button(
                        onClick = onPlay,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPalette.Cyan),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Rounded.SportsEsports, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Play", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                item {
                    OutlinedButton(
                        onClick = onEditHud,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Rounded.DashboardCustomize, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("HUD", fontSize = 12.sp)
                    }
                }

                if (!profile.isDefault) {
                    item {
                        OutlinedButton(
                            onClick = onOpenStudio,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonPalette.Purple),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonPalette.Purple.copy(alpha = 0.6f)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(Icons.Rounded.Palette, contentDescription = null, tint = NeonPalette.Purple, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Studio", fontSize = 12.sp, color = NeonPalette.Purple, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                item {
                    IconButton(
                        onClick = onDuplicate,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Rounded.ContentCopy,
                            contentDescription = "Duplicate layout",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Custom Layouts only: Rename & Share
                if (!profile.isDefault) {
                    item {
                        IconButton(
                            onClick = onRename,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Edit,
                                contentDescription = "Rename custom layout",
                                tint = NeonPalette.Cyan.copy(alpha = 0.85f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    item {
                        IconButton(
                            onClick = onShare,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Share,
                                contentDescription = "Share custom layout",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                if (profile.isDefault) {
                    item {
                        IconButton(
                            onClick = onReset,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Rounded.RestartAlt,
                                contentDescription = "Reset factory default",
                                tint = NeonPalette.Purple.copy(alpha = 0.85f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                item {
                    // Delete button
                    if (profile.isDefault) {
                        // Protected - Disabled with Lock icon
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(36.dp),
                            colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White.copy(alpha = 0.25f))
                        ) {
                            Icon(
                                Icons.Rounded.Lock,
                                contentDescription = "Protected default layout cannot be deleted",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = "Delete custom layout",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
