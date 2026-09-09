package com.sanket.tools.nexpad.ui

import android.content.pm.ActivityInfo
import android.widget.Toast
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.sanket.tools.nexpad.model.*
import com.sanket.tools.nexpad.runtime.registry.ComponentRegistry
import com.sanket.tools.nexpad.ui.components.controller.ControllerElementRenderer
import com.sanket.tools.nexpad.ui.theme.NeonPalette
import com.sanket.tools.nexpad.utils.LayoutManager
import com.sanket.tools.nexpad.utils.LockScreenOrientation
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel
import com.sanket.tools.nexpad.viewmodel.HudEditorViewModel
import com.sanket.tools.nexpad.viewmodel.HudEditorViewModelFactory
import kotlin.math.roundToInt

@Composable
fun HudEditorScreen(
    navController: NavController,
    layoutManager: LayoutManager,
    viewModel: HudEditorViewModel = viewModel(
        factory = HudEditorViewModelFactory(
            layoutManager = layoutManager,
            componentRegistry = ComponentRegistry.getInstance(LocalContext.current)
        )
    )
) {
    val context = LocalContext.current
    val profile by viewModel.currentProfile.collectAsState()
    val elements by viewModel.elements.collectAsState()
    val selectedControl by viewModel.selectedControl.collectAsState()
    val compatibleSkins by viewModel.compatibleSkins.collectAsState()
    val hasUnsavedChanges by viewModel.hasUnsavedChanges.collectAsState()

    val dummyGamepadViewModel = viewModel<GamepadViewModel>()
    var showAddDialog by remember { mutableStateOf(false) }

    LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)

    // Reload active profile and consume any pending selected key from Button Studio
    LaunchedEffect(Unit) {
        viewModel.loadActiveProfile()
    }

    // Auto-save on exit if there are changes
    DisposableEffect(Unit) {
        onDispose {
            if (viewModel.hasUnsavedChanges.value) {
                viewModel.saveProfile()
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {
                detectTapGestures {
                    viewModel.selectControl(null)
                }
            }
    ) {
        val screenWidthPx = maxOf(constraints.maxWidth, constraints.maxHeight).toFloat()
        val screenHeightPx = minOf(constraints.maxWidth, constraints.maxHeight).toFloat()

        // 1. Fullscreen Touch Canvas (Zero-Recomposition GPU Rendering)
        HudCanvas(
            elements = elements,
            selectedControl = selectedControl,
            screenWidthPx = screenWidthPx,
            screenHeightPx = screenHeightPx,
            isRgbEnabled = profile.isRgbEnabled,
            dummyViewModel = dummyGamepadViewModel,
            onSelect = { viewModel.selectControl(it) },
            onDragDelta = { control, dx, dy -> viewModel.nudge(control, dx, dy) }
        )

        // 2. Top Navigation Bar
        HudTopBar(
            profileName = profile.name,
            isDefault = profile.isDefault,
            hasUnsavedChanges = hasUnsavedChanges,
            onBack = {
                if (hasUnsavedChanges) {
                    viewModel.saveProfile { navController.popBackStack() }
                } else {
                    navController.popBackStack()
                }
            },
            onOpenPalette = { showAddDialog = true },
            onSave = {
                viewModel.saveProfile {
                    Toast.makeText(context, "Layout '${profile.name}' saved!", Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                }
            },
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // 3. Docked Control Panel for Selected Element
        if (selectedControl != null && elements.containsKey(selectedControl)) {
            val selectedElement = elements[selectedControl]!!
            HudDockedInspector(
                element = selectedElement,
                compatibleSkins = compatibleSkins,
                screenWidthPx = screenWidthPx,
                screenHeightPx = screenHeightPx,
                onClose = { viewModel.selectControl(null) },
                onNudge = { dx, dy -> viewModel.nudge(selectedControl!!, dx, dy) },
                onScaleChange = { viewModel.setScale(selectedControl!!, it) },
                onOpacityChange = { viewModel.setOpacity(selectedControl!!, it) },
                onCycleSkin = { viewModel.cycleNextSkin(selectedControl!!) },
                onOpenStudio = { navController.navigate("button_studio") },
                onResetPos = { viewModel.resetControlToDefault(selectedControl!!) },
                onRemove = { viewModel.removeControl(selectedControl!!) },
                modifier = Modifier.align(
                    if (selectedElement.transform.yRatio < 0.48f) Alignment.BottomCenter else Alignment.TopCenter
                )
            )
        }

        // 4. Manage / Toggle Buttons Dialog
        if (showAddDialog) {
            HudButtonPaletteDialog(
                currentElements = elements,
                onToggleControl = { control ->
                    if (elements.containsKey(control)) {
                        viewModel.removeControl(control)
                    } else {
                        viewModel.addControl(control)
                    }
                },
                onRestoreAll = { viewModel.restoreAllDefaultButtons() },
                onStandardOnly = {
                    val standardKeys = setOf(
                        GamepadControl.LT, GamepadControl.RT, GamepadControl.LB, GamepadControl.RB,
                        GamepadControl.LS, GamepadControl.RS, GamepadControl.DPAD,
                        GamepadControl.A, GamepadControl.B, GamepadControl.X, GamepadControl.Y,
                        GamepadControl.XBOX, GamepadControl.VIEW, GamepadControl.MENU
                    )
                    GamepadControl.entries.forEach { ctrl ->
                        if (ctrl in standardKeys) {
                            if (!elements.containsKey(ctrl)) viewModel.addControl(ctrl)
                        } else {
                            viewModel.removeControl(ctrl)
                        }
                    }
                },
                onClearAll = {
                    elements.keys.toList().forEach { viewModel.removeControl(it) }
                },
                onOpenStudio = {
                    showAddDialog = false
                    navController.navigate("button_studio")
                },
                onDismiss = { showAddDialog = false }
            )
        }
    }
}

/**
 * High-performance Touch Canvas.
 * Uses center-based coordinate layout and Modifier.graphicsLayer for zero-recomposition GPU dragging.
 */
@Composable
private fun HudCanvas(
    elements: Map<GamepadControl, HudElement>,
    selectedControl: GamepadControl?,
    screenWidthPx: Float,
    screenHeightPx: Float,
    isRgbEnabled: Boolean,
    dummyViewModel: GamepadViewModel,
    onSelect: (GamepadControl) -> Unit,
    onDragDelta: (GamepadControl, Float, Float) -> Unit
) {
    elements.forEach { (control, element) ->
        key(control) {
            val isSelected = selectedControl == control

            Box(
                modifier = Modifier
                    .layout { measurable, childConstraints ->
                        val placeable = measurable.measure(childConstraints)
                        val x = (element.transform.xRatio * screenWidthPx - placeable.width / 2f).roundToInt()
                        val y = (element.transform.yRatio * screenHeightPx - placeable.height / 2f).roundToInt()
                        layout(placeable.width, placeable.height) {
                            placeable.placeRelative(x, y)
                        }
                    }
                    // GPU Layer: Zero recomposition during scaling, opacity changes, and rotation
                    .graphicsLayer {
                        scaleX = element.transform.scale
                        scaleY = element.transform.scale
                        alpha = element.transform.opacity
                    }
            ) {
                // Controller Element Visual Renderer
                ControllerElementRenderer(
                    key = control.key,
                    isConnected = false,
                    isRgbEnabled = isRgbEnabled,
                    viewModel = dummyViewModel,
                    onVibrate = {},
                    customComponentId = element.skinId
                )

                // Selection Box Indicator
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(if (isSelected) NeonPalette.Cyan.copy(alpha = 0.25f) else Color.Transparent)
                        .border(
                            width = if (isSelected) 2.dp else 0.dp,
                            color = if (isSelected) NeonPalette.Cyan else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                )

                // Touch & Drag Interceptor (Isolated from button tap events)
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .pointerInput(control, screenWidthPx, screenHeightPx) {
                            detectTapGestures(
                                onTap = { onSelect(control) }
                            )
                        }
                        .pointerInput(control, screenWidthPx, screenHeightPx) {
                            detectDragGestures(
                                onDragStart = { onSelect(control) },
                                onDragEnd = {},
                                onDragCancel = {}
                            ) { change, dragAmount ->
                                change.consume()
                                val dx = dragAmount.x / screenWidthPx
                                val dy = dragAmount.y / screenHeightPx
                                onDragDelta(control, dx, dy)
                            }
                        }
                )
            }
        }
    }
}

/**
 * Top Navigation Bar for HUD Editor.
 */
@Composable
private fun HudTopBar(
    profileName: String,
    isDefault: Boolean,
    hasUnsavedChanges: Boolean,
    onBack: () -> Unit,
    onOpenPalette: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
        shadowElevation = 8.dp
    ) {
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

                Column {
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
    }
}

/**
 * Docked Control Panel (Inspector) for the selected HUD element.
 * Provides fine-tuning nudge arrows, scale, opacity, category-safe skins, and reset/remove.
 */
@Composable
private fun HudDockedInspector(
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
    modifier: Modifier = Modifier
) {
    val control = element.control
    val transform = element.transform

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF0F172A).copy(alpha = 0.95f),
        border = androidx.compose.foundation.BorderStroke(1.dp, NeonPalette.Cyan.copy(alpha = 0.5f)),
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Row 1: Header + Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${control.displayName} (${control.category.displayName})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = NeonPalette.Cyan)
                    )

                    Text(
                        text = "X: ${(transform.xRatio * 100).roundToInt()}%  Y: ${(transform.yRatio * 100).roundToInt()}%",
                        fontSize = 11.sp,
                        color = Color.LightGray
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
                    val skinName = when {
                        element.skinId == null || element.skinId.startsWith("builtin.default_") -> "Default 3D"
                        else -> {
                            val custom = compatibleSkins.filterIsInstance<LayoutSkin.CustomComponent>()
                                .firstOrNull { it.def.manifest.id == element.skinId }
                            custom?.def?.manifest?.name ?: "Custom"
                        }
                    }

                    OutlinedButton(
                        onClick = onCycleSkin,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonPalette.Purple.copy(alpha = 0.6f)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = NeonPalette.Purple, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Skin: $skinName", fontSize = 10.sp)
                    }

                    IconButton(
                        onClick = onOpenStudio,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Rounded.Palette, contentDescription = "Button Studio", tint = NeonPalette.Purple, modifier = Modifier.size(16.dp))
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

/**
 * Dialog enabling/disabling any of the 19 standard controller buttons on this HUD layout.
 */
@Composable
private fun HudButtonPaletteDialog(
    currentElements: Map<GamepadControl, HudElement>,
    onToggleControl: (GamepadControl) -> Unit,
    onRestoreAll: () -> Unit,
    onStandardOnly: () -> Unit,
    onClearAll: () -> Unit,
    onOpenStudio: () -> Unit,
    onDismiss: () -> Unit
) {
    val controlDescriptions = mapOf(
        GamepadControl.A to "Face Button A (Bottom)",
        GamepadControl.B to "Face Button B (Right)",
        GamepadControl.X to "Face Button X (Left)",
        GamepadControl.Y to "Face Button Y (Top)",
        GamepadControl.LS to "Left Analog Thumbstick",
        GamepadControl.RS to "Right Analog Thumbstick",
        GamepadControl.DPAD to "4-Way Directional Pad",
        GamepadControl.UP to "D-Pad Directional Up (▲)",
        GamepadControl.DOWN to "D-Pad Directional Down (▼)",
        GamepadControl.LEFT to "D-Pad Directional Left (◀)",
        GamepadControl.RIGHT to "D-Pad Directional Right (▶)",
        GamepadControl.LT to "Left Analog Trigger",
        GamepadControl.RT to "Right Analog Trigger",
        GamepadControl.LB to "Left Bumper",
        GamepadControl.RB to "Right Bumper",
        GamepadControl.XBOX to "Guide / Home Button",
        GamepadControl.VIEW to "View / Back Button",
        GamepadControl.MENU to "Menu / Start Button",
        GamepadControl.SHARE to "Share / Capture Button",
        GamepadControl.M1 to "Rear Macro Paddle 1",
        GamepadControl.M2 to "Rear Macro Paddle 2",
        GamepadControl.M3 to "Rear Macro Paddle 3",
        GamepadControl.M4 to "Rear Macro Paddle 4"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Controller Buttons Palette",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = NeonPalette.Cyan
                )
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Quick preset buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onRestoreAll,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Add All 19", fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = onStandardOnly,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Standard (14)", fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = onClearAll,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Clear All", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                // Grouped by Category
                val groupedControls = GamepadControl.entries.groupBy { it.category }

                groupedControls.forEach { (category, controlList) ->
                    Text(
                        category.displayName,
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = NeonPalette.Cyan,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.04f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                    ) {
                        Column {
                            controlList.forEachIndexed { index, control ->
                                val isPresent = currentElements.containsKey(control)
                                val desc = controlDescriptions[control] ?: ""
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onToggleControl(control) }
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
                                            control.key,
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
                                if (index < controlList.lastIndex) {
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onOpenStudio) {
                    Text("Button Studio 🎨", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = onDismiss,
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
