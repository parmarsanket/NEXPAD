package com.sanket.tools.nexpad.ui.virtualcontroller

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.category.CategoryManager
import com.sanket.tools.nexpad.category.ControllerLabelStyle
import com.sanket.tools.nexpad.model.LayoutProfile
import com.sanket.tools.nexpad.model.shortDisplayName
import com.sanket.tools.nexpad.ui.components.controller.abxy.PlayStationSymbol
import com.sanket.tools.nexpad.ui.components.controller.abxy.getPlayStationShape
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
    onDelete: () -> Unit,
    onUpdateLabelStyle: (ControllerLabelStyle) -> Unit = {}
) {
    val cardShape = remember { RoundedCornerShape(16.dp) }
    val borderColor = when {
        isDragging -> NeonPalette.Purple
        isActive -> NeonPalette.Cyan
        else -> Color.White.copy(alpha = 0.12f)
    }
    val borderColorBtn = when {
        isDragging -> NeonPalette.Purple
        isActive -> NeonPalette.Cyan
        else -> Color.White.copy(alpha = 0.45f)
    }
    val borderWidth = when {
        isDragging -> 2.5.dp
        isActive -> 2.dp
        else -> 1.dp
    }

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
                shape = cardShape
                clip = false
            }
            .clip(cardShape)
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = if (isDragging) 0.98f else 0.90f)
            )
            .border(borderWidth, borderColor, cardShape)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ProfileCardHeader(
                name = profile.name,
                isDefault = profile.isDefault,
                isActive = isActive,
                borderColorBtn = borderColorBtn,
                dragHandleModifier = dragHandleModifier
            )

            if (profile.description.isNotBlank()) {
                Text(
                    text = profile.description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            ProfileButtonPillsRow(
                positions = profile.positions,
                labelStyle = profile.controllerLabelStyle
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            ProfileCardActionsRow(
                profile = profile,
                isActive = isActive,
                onSetActive = onSetActive,
                onPlay = onPlay,
                onEditHud = onEditHud,
                onOpenStudio = onOpenStudio,
                onDuplicate = onDuplicate,
                onRename = onRename,
                onShare = onShare,
                onReset = onReset,
                onDelete = onDelete,
                onUpdateLabelStyle = onUpdateLabelStyle
            )
        }
    }
}

// ============================================================================
// Sub-Components (Internal Section Composables)
// ============================================================================

@Composable
private fun ProfileCardHeader(
    name: String,
    isDefault: Boolean,
    isActive: Boolean,
    borderColorBtn: Color,
    dragHandleModifier: Modifier = Modifier
) {
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
                text = name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )

            if (isDefault) {
                ProfileStatusBadge(
                    text = "DEFAULT",
                    icon = Icons.Rounded.Lock,
                    contentDescription = "Default layout locked",
                    color = NeonPalette.Cyan
                )
            } else {
                ProfileStatusBadge(
                    text = "CUSTOM",
                    icon = Icons.Rounded.Star,
                    contentDescription = "Custom layout",
                    color = NeonPalette.WarningAmber
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isActive) {
                Surface(
                    shape = CircleShape,
                    color = NeonPalette.NeonGreen.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, NeonPalette.NeonGreen)
                ) {
                    Text(
                        text = "ACTIVE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = NeonPalette.NeonGreen,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }

            // Drag Handle affordance icon with generous touch target
            Box(
                modifier = dragHandleModifier
                    .size(30.dp)
                    .border(width = 1.dp, color = borderColorBtn, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "Drag to reorder",
                    tint = borderColorBtn,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun ProfileButtonPillsRow(
    positions: Map<String, *>,
    labelStyle: ControllerLabelStyle
) {
    val buttonKeys = remember(positions) { positions.keys.toList() }
    val previewKeys = remember(buttonKeys) { buttonKeys.take(6) }
    val remainingCount = remember(buttonKeys) { maxOf(0, buttonKeys.size - 6) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color.White.copy(alpha = 0.06f)
        ) {
            Text(
                text = "${buttonKeys.size} Buttons",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        previewKeys.forEach { key ->
            val displayKey = CategoryManager.getLabelForStyle(key, labelStyle)
            val psShape = getPlayStationShape(displayKey)

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color.White.copy(alpha = 0.04f)
            ) {
                if (psShape != null) {
                    Box(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        PlayStationSymbol(
                            shape = psShape,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            size = 11.dp,
                            strokeWidth = 1.6.dp
                        )
                    }
                } else {
                    Text(
                        text = displayKey,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
            }
        }

        if (remainingCount > 0) {
            Text(
                text = "+$remainingCount more",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }
    }
}

@Composable
private fun ProfileCardActionsRow(
    profile: LayoutProfile,
    isActive: Boolean,
    onSetActive: () -> Unit,
    onPlay: () -> Unit,
    onEditHud: () -> Unit,
    onOpenStudio: () -> Unit,
    onDuplicate: () -> Unit,
    onRename: () -> Unit,
    onShare: () -> Unit,
    onReset: () -> Unit,
    onDelete: () -> Unit,
    onUpdateLabelStyle: (ControllerLabelStyle) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isActive) {
            ProfileOutlinedActionBtn(
                text = "Set Active",
                icon = Icons.Rounded.CheckCircle,
                contentColor = NeonPalette.Cyan,
                borderColor = NeonPalette.Cyan.copy(alpha = 0.7f),
                fontWeight = FontWeight.SemiBold,
                onClick = onSetActive
            )
        }

        Button(
            onClick = onPlay,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NeonPalette.Cyan),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            modifier = Modifier.height(36.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.SportsEsports,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "Play",
                color = Color.Black,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
        }

        ControllerLabelStyleSelector(
            currentStyle = profile.controllerLabelStyle,
            onUpdateLabelStyle = onUpdateLabelStyle
        )

        ProfileOutlinedActionBtn(
            text = "HUD",
            icon = Icons.Rounded.DashboardCustomize,
            contentColor = Color.White,
            borderColor = Color.White.copy(alpha = 0.25f),
            fontWeight = null,
            onClick = onEditHud
        )

        if (!profile.isDefault) {
            ProfileOutlinedActionBtn(
                text = "Studio",
                icon = Icons.Rounded.Palette,
                contentColor = NeonPalette.Purple,
                borderColor = NeonPalette.Purple.copy(alpha = 0.6f),
                fontWeight = FontWeight.SemiBold,
                onClick = onOpenStudio
            )
        }

        ProfileIconActionBtn(
            icon = Icons.Rounded.ContentCopy,
            contentDescription = "Duplicate layout",
            tint = Color.White.copy(alpha = 0.7f),
            onClick = onDuplicate
        )

        if (!profile.isDefault) {
            ProfileIconActionBtn(
                icon = Icons.Rounded.Edit,
                contentDescription = "Rename custom layout",
                tint = NeonPalette.Cyan.copy(alpha = 0.85f),
                onClick = onRename
            )
            ProfileIconActionBtn(
                icon = Icons.Rounded.Share,
                contentDescription = "Share custom layout",
                tint = Color.White.copy(alpha = 0.7f),
                onClick = onShare
            )
        } else {
            ProfileIconActionBtn(
                icon = Icons.Rounded.RestartAlt,
                contentDescription = "Reset factory default",
                tint = NeonPalette.Purple.copy(alpha = 0.85f),
                onClick = onReset
            )
        }

        // Delete button (Protected vs Custom)
        if (profile.isDefault) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = Color.White.copy(alpha = 0.25f)
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = "Protected default layout cannot be deleted",
                    modifier = Modifier.size(16.dp)
                )
            }
        } else {
            ProfileIconActionBtn(
                icon = Icons.Rounded.Delete,
                contentDescription = "Delete custom layout",
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                onClick = onDelete
            )
        }
    }
}

// ============================================================================
// Reusable Atomic UI Primitives
// ============================================================================

@Composable
private fun ProfileStatusBadge(
    text: String,
    icon: ImageVector,
    contentDescription: String,
    color: Color
) {
    Surface(
        shape = CircleShape,
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = color,
                modifier = Modifier.size(10.dp)
            )
            Text(
                text = text,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun ControllerLabelStyleSelector(
    currentStyle: ControllerLabelStyle,
    onUpdateLabelStyle: (ControllerLabelStyle) -> Unit
) {
    var showStyleDropdown by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(
            onClick = { showStyleDropdown = true },
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            modifier = Modifier.height(36.dp)
        ) {
            val isXbox = currentStyle == ControllerLabelStyle.XBOX
            Text(
                text = currentStyle.shortDisplayName,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isXbox) NeonPalette.Green else NeonPalette.Cyan,
                maxLines = 1,
                softWrap = false
            )
            Icon(
                imageVector = Icons.Rounded.ArrowDropDown,
                contentDescription = "Button Label Style",
                modifier = Modifier.size(16.dp),
                tint = Color.White.copy(alpha = 0.7f)
            )
        }

        DropdownMenu(
            expanded = showStyleDropdown,
            onDismissRequest = { showStyleDropdown = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            LabelStyleDropdownItem(
                label = "Xbox Style",
                isSelected = currentStyle == ControllerLabelStyle.XBOX,
                onClick = {
                    onUpdateLabelStyle(ControllerLabelStyle.XBOX)
                    showStyleDropdown = false
                }
            )
            LabelStyleDropdownItem(
                label = "PlayStation Style",
                isSelected = currentStyle == ControllerLabelStyle.PLAYSTATION,
                onClick = {
                    onUpdateLabelStyle(ControllerLabelStyle.PLAYSTATION)
                    showStyleDropdown = false
                }
            )
        }
    }
}

@Composable
private fun LabelStyleDropdownItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = { Text(label) },
        onClick = onClick,
        leadingIcon = {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = NeonPalette.Cyan
                )
            }
        }
    )
}

@Composable
private fun ProfileOutlinedActionBtn(
    text: String,
    icon: ImageVector,
    contentColor: Color,
    borderColor: Color,
    fontWeight: FontWeight?,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = contentColor),
        border = BorderStroke(1.dp, borderColor),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
        modifier = Modifier.height(36.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            color = contentColor,
            fontWeight = fontWeight,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun ProfileIconActionBtn(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    iconSize: Dp = 18.dp,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(36.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}