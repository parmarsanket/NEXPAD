package com.sanket.tools.nexpad.ui

import android.content.pm.ActivityInfo
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.sanket.tools.nexpad.ui.AppNavigator
import com.sanket.tools.nexpad.category.CategoryManager
import com.sanket.tools.nexpad.category.CategoryType
import com.sanket.tools.nexpad.category.ControlKey
import com.sanket.tools.nexpad.model.*
import com.sanket.tools.nexpad.runtime.plugin.RemoteComponentRegistry
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
    navController: AppNavigator,
    layoutManager: LayoutManager,
    navigationViewModel: NavigationViewModel? = null,
    initialProfileName: String? = null,
    initialControlKey: String? = null,
    gamepadViewModel: GamepadViewModel? = null,
    viewModel: HudEditorViewModel = viewModel(
        factory = HudEditorViewModelFactory(
            layoutManager = layoutManager,
            componentRegistry = ComponentRegistry.getInstance(LocalContext.current),
            remoteComponentRegistry = RemoteComponentRegistry.getInstance(LocalContext.current)
        )
    )
) {
    val context = LocalContext.current
    val profile by viewModel.currentProfile.collectAsState()
    val elements by viewModel.elements.collectAsState()
    val selectedControl by viewModel.selectedControl.collectAsState()
    val compatibleSkins by viewModel.compatibleSkins.collectAsState()
    val hasUnsavedChanges by viewModel.hasUnsavedChanges.collectAsState()

    val effectiveGamepadViewModel: GamepadViewModel = gamepadViewModel ?: remember(context) {
        (context as? androidx.activity.ComponentActivity)?.let { activity ->
            androidx.lifecycle.ViewModelProvider(activity)[GamepadViewModel::class.java]
        }
    } ?: viewModel<GamepadViewModel>(context as androidx.lifecycle.ViewModelStoreOwner)
    var showAddDialog by remember { mutableStateOf(false) }

    // Unsaved-changes guard: show dialog before leaving if edits exist
    var showUnsavedDialog by remember { mutableStateOf(false) }

    LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)

    // Load the correct profile on entry.
    // If initialProfileName is provided (e.g. launched from VirtualController),
    // load that specific profile. Otherwise fall back to the current active profile.
    LaunchedEffect(initialProfileName) {
        if (!initialProfileName.isNullOrBlank()) {
            viewModel.loadProfileByName(initialProfileName)
        } else {
            viewModel.loadActiveProfile()
        }
        // Pre-select a control if one was specified in the ScreenKey (replaces pendingSelectedKey)
        if (!initialControlKey.isNullOrBlank()) {
            viewModel.selectControl(initialControlKey)
        }
    }

    // Observe NavigationViewModel for an asset selection result returned from Button Studio.
    // When Button Studio pops with a confirmed selection, apply the skin to the selected control.
    val editingContext by navigationViewModel?.editingContext?.collectAsState(initial = null)
        ?: remember { mutableStateOf<AppEditingContext?>(null) }

    LaunchedEffect(editingContext?.pendingAssetResult) {
        val result = editingContext?.pendingAssetResult ?: return@LaunchedEffect
        val ctrlKey = editingContext?.controlKey ?: return@LaunchedEffect
        // Apply the chosen asset to the control (marks hasUnsavedChanges = true)
        viewModel.setSkin(ctrlKey, result)
        // Re-select the control so the inspector stays open showing the new skin
        viewModel.selectControl(ctrlKey)
        // Consume the result so it doesn't re-trigger on recomposition
        navigationViewModel?.consumeAssetResult()
    }

    // Step 10: Back handler — guards against losing unsaved changes
    val handleBack: () -> Unit = {
        if (hasUnsavedChanges) {
            showUnsavedDialog = true
        } else {
            navController.popBackStack()
        }
    }

    BackHandler(enabled = true) {
        handleBack()
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
        val density = LocalDensity.current
        val coroutineScope = rememberCoroutineScope()

        // Top Navigation Bar drag & dock state
        val defaultTopBarHeightPx = with(density) { 56.dp.toPx() }
        var topBarHeightPx by remember { mutableFloatStateOf(defaultTopBarHeightPx) }
        val topBarAnimatable = remember { Animatable(0f) }
        var isDraggingTopBar by remember { mutableStateOf(false) }
        val maxTopBarOffsetY = (screenHeightPx - topBarHeightPx).coerceAtLeast(0f)

        // Docked Control Panel (Inspector) drag & dock state
        val defaultInspectorHeightPx = with(density) { 120.dp.toPx() }
        var inspectorHeightPx by remember { mutableFloatStateOf(defaultInspectorHeightPx) }
        val maxInspectorOffsetY = (screenHeightPx - inspectorHeightPx).coerceAtLeast(0f)
        val inspectorAnimatable = remember { Animatable(maxInspectorOffsetY) }
        var isDraggingInspector by remember { mutableStateOf(false) }
        var userHasDraggedInspector by remember { mutableStateOf(false) }

        // Smart initial placement when selecting elements: auto-dock opposite to element if user hasn't dragged
        LaunchedEffect(selectedControl) {
            if (selectedControl != null && !userHasDraggedInspector) {
                val elem = elements[selectedControl]
                if (elem != null) {
                    val isTopHalf = elem.transform.yRatio < 0.48f
                    val targetY = if (isTopHalf) {
                        (screenHeightPx - inspectorHeightPx).coerceAtLeast(0f)
                    } else {
                        val topOffset = topBarAnimatable.value
                        if (topOffset < 80f) {
                            (topBarHeightPx + with(density) { 8.dp.toPx() }).coerceAtMost(maxInspectorOffsetY)
                        } else {
                            with(density) { 8.dp.toPx() }
                        }
                    }
                    inspectorAnimatable.animateTo(
                        targetValue = targetY,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = 500f
                        )
                    )
                }
            }
        }

        // 1. Fullscreen Touch Canvas (Zero-Recomposition GPU Rendering)
        HudCanvas(
            elements = elements,
            selectedControl = selectedControl,
            screenWidthPx = screenWidthPx,
            screenHeightPx = screenHeightPx,
            isRgbEnabled = profile.isRgbEnabled,
            dummyViewModel = effectiveGamepadViewModel,
            onSelect = { viewModel.selectControl(it) },
            onDragDelta = { control, dx, dy -> viewModel.nudge(control, dx, dy) }
        )

        // 2. Top Navigation Bar (Draggable up/down with snap-to-dock)
        HudTopBar(
            profileName = profile.name,
            isDefault = profile.isDefault,
            hasUnsavedChanges = hasUnsavedChanges,
            onBack = handleBack,
            onOpenPalette = { showAddDialog = true },
            onSave = {
                viewModel.saveProfile {
                    Toast.makeText(context, "Layout '${profile.name}' saved!", Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                }
            },
            isDragging = isDraggingTopBar,
            onDragStart = { isDraggingTopBar = true },
            onDragEnd = { isDraggingTopBar = false },
            onDragY = { deltaY ->
                coroutineScope.launch {
                    topBarAnimatable.snapTo((topBarAnimatable.value + deltaY).coerceIn(0f, maxTopBarOffsetY))
                }
            },
            onToggleDock = {
                val currentY = topBarAnimatable.value
                val targetY = if (currentY < maxTopBarOffsetY / 2f) maxTopBarOffsetY else 0f
                coroutineScope.launch {
                    topBarAnimatable.animateTo(
                        targetValue = targetY,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = 500f)
                    )
                }
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset { IntOffset(0, topBarAnimatable.value.roundToInt()) }
                .onGloballyPositioned { coordinates ->
                    topBarHeightPx = coordinates.size.height.toFloat()
                }
        )

        // 3. Docked Control Panel for Selected Element (Draggable up/down with snap-to-dock)
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
                onOpenStudio = {
                    // Begin a contextual selection session so Button Studio knows
                    // which control and profile it is serving.
                    navigationViewModel?.beginAssetSelection(
                        profileName = profile.name,
                        controlKey = selectedControl!!,
                        currentAssetId = selectedElement.skinId,
                        originScreen = OriginScreen.HUD_EDITOR
                    )
                    navController.navigate(
                        ScreenKey.ButtonStudio(
                            mode = "editor",
                            profileName = profile.name,
                            controlKey = selectedControl!!,
                            currentAssetId = selectedElement.skinId
                        )
                    )
                },
                onResetPos = { viewModel.resetControlToDefault(selectedControl!!) },
                onRemove = { viewModel.removeControl(selectedControl!!) },
                isDragging = isDraggingInspector,
                onDragStart = { isDraggingInspector = true },
                onDragEnd = { isDraggingInspector = false },
                onDragY = { deltaY ->
                    userHasDraggedInspector = true
                    coroutineScope.launch {
                        inspectorAnimatable.snapTo(
                            (inspectorAnimatable.value + deltaY).coerceIn(0f, maxInspectorOffsetY)
                        )
                    }
                },
                onToggleDock = {
                    userHasDraggedInspector = true
                    val currentY = inspectorAnimatable.value
                    val targetY = if (currentY < maxInspectorOffsetY / 2f) {
                        maxInspectorOffsetY
                    } else {
                        val topOffset = topBarAnimatable.value
                        if (topOffset < 80f) {
                            (topBarHeightPx + with(density) { 8.dp.toPx() }).coerceAtMost(maxInspectorOffsetY)
                        } else {
                            with(density) { 8.dp.toPx() }
                        }
                    }
                    coroutineScope.launch {
                        inspectorAnimatable.animateTo(
                            targetValue = targetY,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = 500f)
                        )
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset { IntOffset(0, inspectorAnimatable.value.roundToInt()) }
                    .onGloballyPositioned { coordinates ->
                        val newHeight = coordinates.size.height.toFloat()
                        if (inspectorHeightPx != newHeight) {
                            val wasAtBottom = kotlin.math.abs(maxInspectorOffsetY - inspectorAnimatable.value) < 15f
                            inspectorHeightPx = newHeight
                            if (wasAtBottom && !userHasDraggedInspector) {
                                coroutineScope.launch {
                                    inspectorAnimatable.snapTo((screenHeightPx - newHeight).coerceAtLeast(0f))
                                }
                            }
                        }
                    }
            )
        }

        // 4. Manage / Toggle Buttons Dialog
        if (showAddDialog) {
            HudButtonPaletteDialog(
                currentElements = elements,
                onToggleControl = { controlKey ->
                    val targetCtrl = ControlKey.fromIdentifier(controlKey)
                    val canonical = targetCtrl?.key ?: controlKey.uppercase()
                    val existingKey = elements.keys.firstOrNull { elemKey ->
                        if (targetCtrl != null) ControlKey.fromIdentifier(elemKey) == targetCtrl
                        else elemKey.equals(canonical, ignoreCase = true)
                    }
                    if (existingKey != null) {
                        viewModel.removeControl(existingKey)
                    } else {
                        viewModel.addControl(canonical)
                    }
                },
                onRestoreAll = { viewModel.restoreAllDefaultButtons() },
                onStandardOnly = {
                    val standardControls = setOf(
                        ControlKey.LT, ControlKey.RT, ControlKey.LB, ControlKey.RB,
                        ControlKey.LS, ControlKey.RS, ControlKey.DPAD,
                        ControlKey.A, ControlKey.B, ControlKey.X, ControlKey.Y,
                        ControlKey.GUIDE, ControlKey.BACK, ControlKey.START
                    )
                    CategoryManager.getAllControls().forEach { spec ->
                        val isStandard = spec in standardControls
                        val hasControl = elements.keys.any { ControlKey.fromIdentifier(it) == spec }
                        if (isStandard) {
                            if (!hasControl) viewModel.addControl(spec.key)
                        } else {
                            if (hasControl) viewModel.removeControl(spec.key)
                        }
                    }
                },
                onClearAll = {
                    elements.keys.toList().forEach { viewModel.removeControl(it) }
                },
                onOpenStudio = {
                    showAddDialog = false
                    // Palette dialog → Viewer Mode (no context — user is browsing assets)
                    navController.navigate(ScreenKey.ButtonStudio(mode = "viewer"))
                },
                onDismiss = { showAddDialog = false }
            )
        }

        // Step 10: Unsaved Changes Dialog (Save & Exit / Discard / Cancel)
        if (showUnsavedDialog) {
            AlertDialog(
                onDismissRequest = { showUnsavedDialog = false },
                title = {
                    Text(
                        "Unsaved Changes",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        "You have unsaved changes in '${profile.name}'. Would you like to save them before leaving?",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.saveProfile {
                                Toast.makeText(context, "Layout '${profile.name}' saved!", Toast.LENGTH_SHORT).show()
                                showUnsavedDialog = false
                                navController.popBackStack()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPalette.Cyan)
                    ) {
                        Text("Save & Exit", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = {
                                showUnsavedDialog = false
                                navController.popBackStack()
                            }
                        ) {
                            Text("Discard", color = Color(0xFFFF5252))
                        }
                        TextButton(onClick = { showUnsavedDialog = false }) {
                            Text("Cancel", color = Color.White)
                        }
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface
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
    elements: Map<String, HudElement>,
    selectedControl: String?,
    screenWidthPx: Float,
    screenHeightPx: Float,
    isRgbEnabled: Boolean,
    dummyViewModel: GamepadViewModel,
    onSelect: (String) -> Unit,
    onDragDelta: (String, Float, Float) -> Unit
) {
    val context = LocalContext.current
    val registry = remember { ComponentRegistry.getInstance(context) }
    val remoteRegistry = remember { RemoteComponentRegistry.getInstance(context) }
    val installedComponents by registry.installedComponents.collectAsState()
    val remoteDocs by remoteRegistry.loadedComponents.collectAsState()

    elements.forEach { (controlKey, element) ->
        key(controlKey) {
            val isSelected = selectedControl == controlKey

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
                    key = controlKey,
                    isConnected = false,
                    isRgbEnabled = isRgbEnabled,
                    viewModel = dummyViewModel,
                    onVibrate = {},
                    customComponentId = element.skinId
                )

                // Step 9: Broken asset warning badge when custom component is missing
                val isCustomSkin = element.skinId != null && !element.skinId.startsWith("builtin.default_")
                val isMissingSkin = remember(element.skinId, installedComponents, remoteDocs) {
                    if (!isCustomSkin) false
                    else {
                        installedComponents.none { it.manifest.id == element.skinId } &&
                                remoteDocs.none { it.manifest.id == element.skinId }
                    }
                }
                if (isMissingSkin) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFE65100),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(2.dp)
                            .size(18.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "⚠",
                                fontSize = 11.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

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
                        .pointerInput(controlKey, screenWidthPx, screenHeightPx) {
                            detectTapGestures(
                                onTap = { onSelect(controlKey) }
                            )
                        }
                        .pointerInput(controlKey, screenWidthPx, screenHeightPx) {
                            detectDragGestures(
                                onDragStart = { onSelect(controlKey) },
                                onDragEnd = {},
                                onDragCancel = {}
                            ) { change, dragAmount ->
                                change.consume()
                                val dx = dragAmount.x / screenWidthPx
                                val dy = dragAmount.y / screenHeightPx
                                onDragDelta(controlKey, dx, dy)
                            }
                        }
                )
            }
        }
    }
}

/**
 * Top Navigation Bar for HUD Editor.
 * Draggable and movable vertically (up/down) with snap-to-dock toggle.
 */
@Composable
private fun HudTopBar(
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

/**
 * Docked Control Panel (Inspector) for the selected HUD element.
 * Provides fine-tuning nudge arrows, scale, opacity, category-safe skins, and reset/remove.
 * Draggable and movable vertically (up/down) with snap-to-dock toggle.
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

/**
 * Dialog enabling/disabling any of the 19 standard controller buttons on this HUD layout.
 */
@Composable
private fun HudButtonPaletteDialog(
    currentElements: Map<String, HudElement>,
    onToggleControl: (String) -> Unit,
    onRestoreAll: () -> Unit,
    onStandardOnly: () -> Unit,
    onClearAll: () -> Unit,
    onOpenStudio: () -> Unit,
    onDismiss: () -> Unit
) {
    val categories = remember { CategoryManager.getAllCategories() }

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
                        Text("Add All", fontSize = 11.sp)
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

                // Grouped by Category via CategoryManager
                categories.forEach { category ->
                    val controlList = category.controls
                    if (controlList.isEmpty()) return@forEach
                    val catHeader = "${category.title} (${controlList.size})"

                    Text(
                        catHeader,
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = NeonPalette.Cyan,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    if (category.type == CategoryType.DPAD) {
                        Text(
                            "💡 4-Way Cross Pad and discrete directional buttons are mutually exclusive.",
                            fontSize = 11.sp,
                            color = NeonPalette.Cyan.copy(alpha = 0.8f),
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.04f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                    ) {
                        Column {
                            controlList.forEachIndexed { index, spec ->
                                val canonicalKey = spec.key
                                val isPresent = currentElements.keys.any { elemKey ->
                                    ControlKey.fromIdentifier(elemKey) == spec
                                }
                                val emoji = spec.emoji
                                val label = spec.label
                                val desc = spec.description
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onToggleControl(canonicalKey) }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isPresent,
                                        onCheckedChange = null,
                                        colors = CheckboxDefaults.colors(checkedColor = NeonPalette.Cyan)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    if (emoji.isNotBlank()) {
                                        Text(emoji, fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                canonicalKey,
                                                fontSize = 14.sp,
                                                fontWeight = if (isPresent) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isPresent) Color.White else Color.Gray
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                "• $label",
                                                fontSize = 12.sp,
                                                color = if (isPresent) NeonPalette.Cyan.copy(alpha = 0.8f) else Color.Gray.copy(alpha = 0.6f)
                                            )
                                        }
                                        if (desc.isNotBlank()) {
                                            Text(
                                                desc,
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
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
