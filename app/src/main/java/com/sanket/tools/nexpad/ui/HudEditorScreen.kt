package com.sanket.tools.nexpad.ui

import android.content.pm.ActivityInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.sanket.tools.nexpad.model.Position
import com.sanket.tools.nexpad.model.defaultPositions
import com.sanket.tools.nexpad.runtime.registry.ComponentRegistry
import com.sanket.tools.nexpad.ui.theme.NeonPalette
import com.sanket.tools.nexpad.utils.LayoutManager
import com.sanket.tools.nexpad.utils.LockScreenOrientation
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel
import androidx.compose.ui.layout.layout
import kotlin.math.roundToInt

@Composable
fun HudEditorScreen(navController: NavController, layoutManager: LayoutManager) {
    val profile = layoutManager.getActiveProfile()
    val positions = remember(profile.name) {
        mutableStateMapOf<String, Position>().apply { putAll(profile.positions) }
    }
    val dummyViewModel = androidx.lifecycle.viewmodel.compose.viewModel<GamepadViewModel>()

    var selectedKey by remember {
        val initial = layoutManager.pendingSelectedKey
        layoutManager.pendingSelectedKey = null
        if (initial != null && !positions.containsKey(initial)) {
            val defPos = defaultPositions()[initial] ?: Position(0.5f, 0.5f)
            positions[initial] = defPos
        }
        mutableStateOf<String?>(initial)
    }
    var showAddDialog by remember { mutableStateOf(false) }

    LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)

    // Auto-save when leaving the editor
    DisposableEffect(Unit) {
        onDispose {
            val updatedProfile = profile.copy(positions = positions.toMap())
            layoutManager.saveProfile(updatedProfile)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {
                detectTapGestures {
                    selectedKey = null
                }
            }
    ) {
        val screenWidthPx = maxOf(constraints.maxWidth, constraints.maxHeight).toFloat()
        val screenHeightPx = minOf(constraints.maxWidth, constraints.maxHeight).toFloat()

        // Render all buttons with strict keying and center-based placement
        positions.keys.toList().forEach { key ->
            key(key) {
                val position = positions[key]
                if (position != null) {
                    val isSelected = selectedKey == key

                    Box(
                        modifier = Modifier
                            .layout { measurable, childConstraints ->
                                val placeable = measurable.measure(childConstraints)
                                val x = (position.xRatio * screenWidthPx - placeable.width / 2f).roundToInt()
                                val y = (position.yRatio * screenHeightPx - placeable.height / 2f).roundToInt()
                                layout(placeable.width, placeable.height) {
                                    placeable.placeRelative(x, y)
                                }
                            }
                            .scale(position.scale)
                            .alpha(position.opacity)
                    ) {
                        // Controller Element (Visual)
                        com.sanket.tools.nexpad.ui.components.controller.ControllerElementRenderer(
                            key = key,
                            isConnected = false,
                            isRgbEnabled = profile.isRgbEnabled,
                            viewModel = dummyViewModel,
                            onVibrate = {},
                            customComponentId = position.customComponentId
                        )

                        // Selection box indicator
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(if (isSelected) NeonPalette.Cyan.copy(alpha = 0.25f) else Color.Transparent)
                                .border(if (isSelected) 2.dp else 0.dp, if (isSelected) NeonPalette.Cyan else Color.Transparent)
                        )

                        // Touch & Drag Interceptor Overlay (Guarantees HUD gestures are never intercepted by inner buttons)
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .pointerInput(key, screenWidthPx, screenHeightPx) {
                                    detectTapGestures(
                                        onTap = {
                                            selectedKey = key
                                        }
                                    )
                                }
                                .pointerInput(key, screenWidthPx, screenHeightPx) {
                                    detectDragGestures(
                                        onDragStart = {
                                            selectedKey = key
                                        },
                                        onDragEnd = { },
                                        onDragCancel = { }
                                    ) { change, dragAmount ->
                                        change.consume()
                                        val cur = positions[key] ?: return@detectDragGestures
                                        val newX = cur.xRatio + (dragAmount.x / screenWidthPx)
                                        val newY = cur.yRatio + (dragAmount.y / screenHeightPx)
                                        positions[key] = cur.copy(
                                            xRatio = newX.coerceIn(0.0f, 1.0f),
                                            yRatio = newY.coerceIn(0.0f, 1.0f)
                                        )
                                    }
                                }
                        )
                    }
                }
            }
        }

        // Top Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
            ) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Text(
                    text = if (profile.isDefault) "Default Profile" else "Custom Profile",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (profile.isDefault) NeonPalette.Cyan else Color(0xFFFFB703),
                        fontSize = 11.sp
                    )
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.height(38.dp)
                ) {
                    Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("BUTTONS", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = {
                        val updatedProfile = profile.copy(positions = positions.toMap())
                        layoutManager.saveProfile(updatedProfile)
                        navController.popBackStack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPalette.Cyan),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.height(38.dp)
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("SAVE", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Selected Control Panel (Fine Logic HUD Manager)
        // Automatically docks to bottom if button is at top, or top if button is at bottom
        val currentKey = selectedKey
        if (currentKey != null && positions.containsKey(currentKey)) {
            val position = positions[currentKey]!!
            val isButtonAtTop = position.yRatio < 0.48f
            val cardAlignment = if (isButtonAtTop) Alignment.BottomCenter else Alignment.TopCenter
            val cardPadding = if (isButtonAtTop) PaddingValues(bottom = 12.dp) else PaddingValues(top = 64.dp)

            OutlinedCard(
                modifier = Modifier
                    .align(cardAlignment)
                    .padding(cardPadding)
                    .widthIn(min = 340.dp, max = 380.dp),
                elevation = CardDefaults.outlinedCardElevation(defaultElevation = 10.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonPalette.Cyan.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Header: Button Key + Close
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = NeonPalette.Cyan.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NeonPalette.Cyan)
                            ) {
                                Text(
                                    currentKey,
                                    color = NeonPalette.Cyan,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                "Position: ${(position.xRatio * 100).roundToInt()}% , ${(position.yRatio * 100).roundToInt()}%",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = { selectedKey = null },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "Deselect", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }

                    // Fine Position Nudge Controls (Arrow buttons)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Nudge", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            // Left
                            FilledTonalIconButton(
                                onClick = {
                                    val newX = (position.xRatio - 8f / screenWidthPx).coerceIn(0.0f, 1.0f)
                                    positions[currentKey] = position.copy(xRatio = newX)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Rounded.ArrowBack, contentDescription = "Nudge Left", modifier = Modifier.size(14.dp))
                            }
                            // Right
                            FilledTonalIconButton(
                                onClick = {
                                    val newX = (position.xRatio + 8f / screenWidthPx).coerceIn(0.0f, 1.0f)
                                    positions[currentKey] = position.copy(xRatio = newX)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Rounded.ArrowForward, contentDescription = "Nudge Right", modifier = Modifier.size(14.dp))
                            }
                            // Up
                            FilledTonalIconButton(
                                onClick = {
                                    val newY = (position.yRatio - 8f / screenHeightPx).coerceIn(0.0f, 1.0f)
                                    positions[currentKey] = position.copy(yRatio = newY)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Rounded.ArrowUpward, contentDescription = "Nudge Up", modifier = Modifier.size(14.dp))
                            }
                            // Down
                            FilledTonalIconButton(
                                onClick = {
                                    val newY = (position.yRatio + 8f / screenHeightPx).coerceIn(0.0f, 1.0f)
                                    positions[currentKey] = position.copy(yRatio = newY)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Rounded.ArrowDownward, contentDescription = "Nudge Down", modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    // Size / Scale Row with Steppers
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Size: ${(position.scale * 100).roundToInt()}%",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            modifier = Modifier.width(68.dp)
                        )

                        FilledTonalIconButton(
                            onClick = {
                                val newScale = (position.scale - 0.05f).coerceIn(0.5f, 2.5f)
                                positions[currentKey] = position.copy(scale = (newScale * 100).roundToInt() / 100f)
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Rounded.Remove, contentDescription = "Decrease Scale", modifier = Modifier.size(14.dp))
                        }

                        Slider(
                            value = position.scale,
                            onValueChange = { positions[currentKey] = position.copy(scale = (it * 100).roundToInt() / 100f) },
                            valueRange = 0.5f..2.5f,
                            modifier = Modifier.weight(1f)
                        )

                        FilledTonalIconButton(
                            onClick = {
                                val newScale = (position.scale + 0.05f).coerceIn(0.5f, 2.5f)
                                positions[currentKey] = position.copy(scale = (newScale * 100).roundToInt() / 100f)
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = "Increase Scale", modifier = Modifier.size(14.dp))
                        }
                    }

                    // Opacity Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Alpha: ${(position.opacity * 100).roundToInt()}%",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            modifier = Modifier.width(68.dp)
                        )
                        Slider(
                            value = position.opacity,
                            onValueChange = { positions[currentKey] = position.copy(opacity = (it * 100).roundToInt() / 100f) },
                            valueRange = 0.1f..1.0f,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Skin Selector
                    val context = LocalContext.current
                    val registry = remember { ComponentRegistry.getInstance(context) }
                    val customComponents by registry.installedComponents.collectAsState()
                    val currentSkin = customComponents.find { it.manifest.id == position.customComponentId }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Skin", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        TextButton(
                            onClick = {
                                val available = listOf(null) + customComponents.map { it.manifest.id }
                                val currentIndex = available.indexOf(position.customComponentId)
                                val nextIndex = (currentIndex + 1) % available.size
                                positions[currentKey] = position.copy(customComponentId = available[nextIndex])
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = currentSkin?.manifest?.name ?: "Default Realistic",
                                color = NeonPalette.Cyan,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                    }

                    // Action Row: Reset Single Button Pos + Remove Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val def = defaultPositions()[currentKey]
                                if (def != null) {
                                    positions[currentKey] = def
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Reset Pos", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                positions.remove(currentKey)
                                selectedKey = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Remove", fontSize = 11.sp, color = MaterialTheme.colorScheme.onError)
                        }
                    }
                }
            }
        }

        // Add/Remove Dialog (Material 3 with Scroll)
        if (showAddDialog) {
            val buttonGroups = listOf(
                "Face Buttons" to listOf(
                    "A" to "Action / Jump",
                    "B" to "Crouch / Cancel",
                    "X" to "Reload / Interact",
                    "Y" to "Switch Weapon"
                ),
                "Shoulder & Triggers" to listOf(
                    "LT" to "Left Trigger (Aim / Brake)",
                    "RT" to "Right Trigger (Shoot / Gas)",
                    "LB" to "Left Bumper (Tactical / Shift Down)",
                    "RB" to "Right Bumper (Lethal / Shift Up)"
                ),
                "Sticks & D-Pad" to listOf(
                    "LS" to "Left Thumbstick (Move)",
                    "RS" to "Right Thumbstick (Aim / Look)",
                    "DPAD" to "Directional D-Pad (Equipment)"
                ),
                "System Buttons" to listOf(
                    "XBOX" to "Guide / Home Core",
                    "VIEW" to "Select / Map / Scoreboard",
                    "MENU" to "Start / Pause Menu",
                    "SHARE" to "Share / Capture"
                ),
                "Elite Macro Paddles" to listOf(
                    "M1" to "Rear Paddle 1",
                    "M2" to "Rear Paddle 2",
                    "M3" to "Rear Paddle 3",
                    "M4" to "Rear Paddle 4"
                )
            )

            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Manage Controller Buttons",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = NeonPalette.Cyan
                            )
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Quick Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    defaultPositions().forEach { (k, v) ->
                                        if (!positions.containsKey(k)) positions[k] = v
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("All (18)", fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    val standardKeys = setOf("LT", "RT", "LB", "RB", "LS", "RS", "DPAD", "A", "B", "X", "Y", "XBOX", "VIEW", "MENU")
                                    // Remove macros
                                    listOf("M1", "M2", "M3", "M4", "SHARE").forEach {
                                        positions.remove(it)
                                        if (selectedKey == it) selectedKey = null
                                    }
                                    // Ensure standard are present
                                    standardKeys.forEach { k ->
                                        if (!positions.containsKey(k)) {
                                            positions[k] = defaultPositions()[k] ?: Position(0.5f, 0.5f)
                                        }
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("Standard Only (14)", fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    positions.clear()
                                    selectedKey = null
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("Clear All", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                            }
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                        // Category Groups
                        buttonGroups.forEach { (categoryName, buttonList) ->
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    categoryName.uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = NeonPalette.Cyan,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp
                                    )
                                )

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White.copy(alpha = 0.04f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                                ) {
                                    Column {
                                        buttonList.forEachIndexed { index, (key, desc) ->
                                            val isPresent = positions.containsKey(key)
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        if (isPresent) {
                                                            positions.remove(key)
                                                            if (selectedKey == key) {
                                                                selectedKey = null
                                                            }
                                                        } else {
                                                            positions[key] = defaultPositions()[key] ?: Position(0.5f, 0.5f)
                                                        }
                                                    }
                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Checkbox(
                                                    checked = isPresent,
                                                    onCheckedChange = null,
                                                    colors = CheckboxDefaults.colors(checkedColor = NeonPalette.Cyan)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        key,
                                                        fontSize = 14.sp,
                                                        fontWeight = if (isPresent) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isPresent) Color.White else Color.Gray
                                                    )
                                                    Text(
                                                        desc,
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            if (index < buttonList.lastIndex) {
                                                HorizontalDivider(color = Color.White.copy(alpha = 0.04f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = {
                            showAddDialog = false
                            navController.navigate("button_studio")
                        }) {
                            Text("Button Studio 🎨", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelMedium)
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = { showAddDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPalette.Cyan),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Done", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
