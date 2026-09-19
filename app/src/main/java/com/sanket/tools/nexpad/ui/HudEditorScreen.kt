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
import com.sanket.tools.nexpad.ui.components.common.NexpadConfirmDialog
import com.sanket.tools.nexpad.ui.hud.*
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

        // Auto-Dodge Coordinator for intelligent collision-avoidance overlay physics
        val defaultTopBarHeightPx = with(density) { 56.dp.toPx() }
        val defaultInspectorHeightPx = with(density) { 120.dp.toPx() }
        val topBarAnimatable = remember { Animatable(0f) }
        val inspectorAnimatable = remember { Animatable((screenHeightPx - defaultInspectorHeightPx).coerceAtLeast(0f)) }

        val autoDodgeCoordinator = remember {
            HudAutoDodgeCoordinator(
                topBarAnimatable = topBarAnimatable,
                inspectorAnimatable = inspectorAnimatable,
                coroutineScope = coroutineScope,
                density = density
            )
        }

        // Keep screen and overlay dimensions up-to-date in coordinator
        LaunchedEffect(screenWidthPx, screenHeightPx) {
            autoDodgeCoordinator.updateDimensions(
                width = screenWidthPx,
                height = screenHeightPx,
                topBarH = defaultTopBarHeightPx,
                inspectorH = defaultInspectorHeightPx
            )
        }

        // Real-time button tracking: when element is selected or moves during drag/nudge, auto-dodge
        val selectedElement = selectedControl?.let { elements[it] }
        LaunchedEffect(
            selectedControl,
            selectedElement?.transform?.xRatio,
            selectedElement?.transform?.yRatio,
            selectedElement?.transform?.scale
        ) {
            autoDodgeCoordinator.onElementPositionChanged(selectedElement)
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

        // 2. Top Navigation Bar (Draggable up/down with auto-dodge & snap-to-dock)
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
            isDragging = autoDodgeCoordinator.isDraggingTopBar,
            onDragStart = { autoDodgeCoordinator.isDraggingTopBar = true },
            onDragEnd = { autoDodgeCoordinator.onTopBarDragEnd() },
            onDragY = { deltaY ->
                coroutineScope.launch {
                    topBarAnimatable.snapTo((topBarAnimatable.value + deltaY).coerceIn(0f, autoDodgeCoordinator.maxTopBarOffsetY))
                }
            },
            onToggleDock = { autoDodgeCoordinator.toggleTopBarDock() },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset { IntOffset(0, topBarAnimatable.value.roundToInt()) }
                .onGloballyPositioned { coordinates ->
                    val h = coordinates.size.height.toFloat()
                    if (h > 0f && autoDodgeCoordinator.topBarHeightPx != h) {
                        autoDodgeCoordinator.topBarHeightPx = h
                    }
                }
        )

        // 3. Docked Control Panel for Selected Element (Draggable up/down with auto-dodge & snap-to-dock)
        if (selectedControl != null && elements.containsKey(selectedControl)) {
            val element = elements[selectedControl]!!
            HudDockedInspector(
                element = element,
                compatibleSkins = compatibleSkins,
                screenWidthPx = screenWidthPx,
                screenHeightPx = screenHeightPx,
                onClose = { viewModel.selectControl(null) },
                onNudge = { dx, dy -> viewModel.nudge(selectedControl!!, dx, dy) },
                onScaleChange = { viewModel.setScale(selectedControl!!, it) },
                onOpacityChange = { viewModel.setOpacity(selectedControl!!, it) },
                onCycleSkin = { viewModel.cycleNextSkin(selectedControl!!) },
                onOpenStudio = {
                    navigationViewModel?.beginAssetSelection(
                        profileName = profile.name,
                        controlKey = selectedControl!!,
                        currentAssetId = element.skinId,
                        originScreen = OriginScreen.HUD_EDITOR
                    )
                    navController.navigate(
                        ScreenKey.ButtonStudio(
                            mode = "editor",
                            profileName = profile.name,
                            controlKey = selectedControl!!,
                            currentAssetId = element.skinId
                        )
                    )
                },
                onResetPos = { viewModel.resetControlToDefault(selectedControl!!) },
                onRemove = { viewModel.removeControl(selectedControl!!) },
                isDragging = autoDodgeCoordinator.isDraggingInspector,
                onDragStart = { autoDodgeCoordinator.isDraggingInspector = true },
                onDragEnd = { autoDodgeCoordinator.onInspectorDragEnd() },
                onDragY = { deltaY ->
                    coroutineScope.launch {
                        inspectorAnimatable.snapTo(
                            (inspectorAnimatable.value + deltaY).coerceIn(0f, autoDodgeCoordinator.maxInspectorOffsetY)
                        )
                    }
                },
                onToggleDock = { autoDodgeCoordinator.toggleInspectorDock() },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset { IntOffset(0, inspectorAnimatable.value.roundToInt()) }
                    .onGloballyPositioned { coordinates ->
                        val newHeight = coordinates.size.height.toFloat()
                        if (newHeight > 0f && autoDodgeCoordinator.inspectorHeightPx != newHeight) {
                            autoDodgeCoordinator.inspectorHeightPx = newHeight
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
