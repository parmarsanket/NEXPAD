package com.sanket.tools.nexpad.ui

import android.widget.Toast
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.model.LayoutProfile
import com.sanket.tools.nexpad.model.getDefaultLayoutProfiles
import com.sanket.tools.nexpad.ui.components.common.NexpadTopAppBar
import com.sanket.tools.nexpad.ui.components.effects.CyberGrid
import com.sanket.tools.nexpad.ui.components.effects.ScanLine
import com.sanket.tools.nexpad.ui.layout.adaptiveLayoutSpec
import com.sanket.tools.nexpad.ui.theme.NeonPalette
import com.sanket.tools.nexpad.ui.virtualcontroller.*
import com.sanket.tools.nexpad.utils.LayoutManager
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState
import sh.calvin.reorderable.rememberScroller

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VirtualControllerScreen(
    navController: AppNavigator,
    layoutManager: LayoutManager,
    navigationViewModel: NavigationViewModel? = null
) {
    val context = LocalContext.current
    val profiles by layoutManager.profilesFlow.collectAsState()
    val activeProfileName by layoutManager.activeProfileNameFlow.collectAsState()

    val gridState = rememberLazyGridState()
    val haptic = LocalHapticFeedback.current

    var localProfiles by remember { mutableStateOf(profiles) }
    var isAnyItemDragging by remember { mutableStateOf(false) }

    var showAddDialog by remember { mutableStateOf(false) }
    var profileToDuplicate by remember { mutableStateOf<LayoutProfile?>(null) }
    var profileToRename by remember { mutableStateOf<LayoutProfile?>(null) }
    var profileToDelete by remember { mutableStateOf<LayoutProfile?>(null) }
    var profileToReset by remember { mutableStateOf<LayoutProfile?>(null) }
    var profileToEditAsPreset by remember { mutableStateOf<LayoutProfile?>(null) }

    Scaffold(
        topBar = {
            NexpadTopAppBar(
                title = "Virtual Controller",
                subtitle = "Layouts & Button Management",
                onBack = { navController.popBackStack() },
                isNavigationEnabled = !isAnyItemDragging,
                actions = {
                    OutlinedButton(
                        onClick = { navController.navigate(ScreenKey.ButtonStudio(mode = "viewer")) },
                        enabled = !isAnyItemDragging,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonPalette.Purple),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonPalette.Purple.copy(alpha = if (isAnyItemDragging) 0.3f else 0.7f)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.padding(end = 6.dp).height(36.dp)
                    ) {
                        Icon(Icons.Rounded.Palette, contentDescription = null, tint = if (isAnyItemDragging) NeonPalette.Purple.copy(alpha = 0.4f) else NeonPalette.Purple, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Button Studio", color = if (isAnyItemDragging) NeonPalette.Purple.copy(alpha = 0.4f) else NeonPalette.Purple, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    Button(
                        onClick = { showAddDialog = true },
                        enabled = !isAnyItemDragging,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPalette.Cyan),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.padding(end = 8.dp).height(36.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Custom", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        // Calibrated dynamic scroller: ramps velocity smoothly from fine precision at 140dp to fast scroll at the edge
        val reorderableLazyGridState = rememberReorderableLazyGridState(
            lazyGridState = gridState,
            scrollThreshold = 140.dp,
            scrollThresholdPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding()
            ),
            scroller = rememberScroller(
                scrollableState = gridState,
                pixelAmountProvider = {
                    (gridState.layoutInfo.viewportSize.height * 0.04f).coerceIn(45f, 95f)
                }
            )
        ) { from, to ->
            val fromKey = from.key as? String ?: return@rememberReorderableLazyGridState
            val toKey = to.key as? String ?: return@rememberReorderableLazyGridState
            if (fromKey == "hub_banner" || toKey == "hub_banner") return@rememberReorderableLazyGridState

            val fromIdx = localProfiles.indexOfFirst { it.name == fromKey }
            val toIdx = localProfiles.indexOfFirst { it.name == toKey }
            if (fromIdx != -1 && toIdx != -1 && fromIdx != toIdx) {
                localProfiles = localProfiles.toMutableList().apply {
                    add(toIdx, removeAt(fromIdx))
                }
            }
        }

        LaunchedEffect(reorderableLazyGridState.isAnyItemDragging) {
            val dragging = reorderableLazyGridState.isAnyItemDragging
            if (isAnyItemDragging && !dragging) {
                // Drag completed, persist new order
                val newOrder = localProfiles.map { it.name }
                if (newOrder != profiles.map { it.name }) {
                    layoutManager.saveProfileOrder(newOrder)
                    Toast.makeText(context, "Layout order updated", Toast.LENGTH_SHORT).show()
                }
            }
            isAnyItemDragging = dragging
        }

        LaunchedEffect(profiles) {
            if (!reorderableLazyGridState.isAnyItemDragging) {
                localProfiles = profiles
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val layout = remember(maxWidth, maxHeight) { adaptiveLayoutSpec(maxWidth, maxHeight) }

            CyberGrid(modifier = Modifier.matchParentSize())
            ScanLine(modifier = Modifier.matchParentSize())

            // Full-screen touch capture area with contentPadding so the pointer never disconnects at the top bar
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(if (layout.useTwoPaneLayout) 2 else 1),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + layout.verticalPadding,
                    bottom = padding.calculateBottomPadding() + layout.verticalPadding,
                    start = layout.horizontalPadding,
                    end = layout.horizontalPadding
                ),
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(layout.contentSpacing),
                horizontalArrangement = Arrangement.spacedBy(layout.paneSpacing)
            ) {
                // Info banner spans full width
                item(
                    key = "hub_banner",
                    span = { GridItemSpan(maxLineSpan) },
                    contentType = "hub_banner"
                ) {
                    LayoutHubBanner()
                }

                // Layout Profiles List
                items(
                    items = localProfiles,
                    key = { it.name },
                    contentType = { "layout_profile_card" }
                ) { profile ->
                    ReorderableItem(
                        state = reorderableLazyGridState,
                        key = profile.name,
                        animateItemModifier = Modifier.animateItem(
                            placementSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = 300f
                            )
                        )
                    ) { isDragging ->
                        val isActive = profile.name.equals(activeProfileName, ignoreCase = true)

                        LayoutProfileCard(
                            modifier = Modifier
                                .longPressDraggableHandle(
                                    onDragStarted = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                ),
                            dragHandleModifier = Modifier
                                .draggableHandle(
                                    onDragStarted = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                ),
                            profile = profile,
                            isActive = isActive,
                            isDragging = isDragging,
                            onSetActive = {
                                layoutManager.setActiveProfile(profile.name)
                                Toast.makeText(context, "Activated ${profile.name}", Toast.LENGTH_SHORT).show()
                            },
                            onPlay = {
                                layoutManager.setActiveProfile(profile.name)
                                navController.navigate("gamepad")
                            },
                            onEditHud = {
                                if (profile.isDefault) {
                                    // Preset protection: require creating a custom copy first
                                    profileToEditAsPreset = profile
                                } else {
                                    layoutManager.setActiveProfile(profile.name)
                                    navController.navigate(ScreenKey.Editor(profileName = profile.name))
                                }
                            },
                            onOpenStudio = {
                                layoutManager.setActiveProfile(profile.name)
                                navController.navigate(ScreenKey.ButtonStudio(mode = "editor", profileName = profile.name))
                            },
                            onDuplicate = {
                                profileToDuplicate = profile
                            },
                            onRename = {
                                profileToRename = profile
                            },
                            onShare = {
                                val sendIntent = android.content.Intent().apply {
                                    action = android.content.Intent.ACTION_SEND
                                    putExtra(android.content.Intent.EXTRA_TITLE, "NEXPAD Layout: ${profile.name}")
                                    putExtra(
                                        android.content.Intent.EXTRA_TEXT,
                                        "🎮 NEXPAD Controller Layout: ${profile.name} (${profile.positions.size} controls)\nDesigned with NEXPAD."
                                    )
                                    type = "text/plain"
                                }
                                val shareIntent = android.content.Intent.createChooser(sendIntent, "Share '${profile.name}'")
                                context.startActivity(shareIntent)
                            },
                            onReset = {
                                profileToReset = profile
                            },
                            onDelete = {
                                if (profile.isDefault) {
                                    Toast.makeText(context, "Default layouts are protected and cannot be deleted", Toast.LENGTH_LONG).show()
                                } else {
                                    profileToDelete = profile
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Add Custom Layout Dialog
    if (showAddDialog) {
        AddCustomLayoutDialog(
            defaults = getDefaultLayoutProfiles(),
            onDismiss = { showAddDialog = false },
            onCreate = { name, baseTemplate, selectedButtons, openStudioImmediately ->
                val newProfile = layoutManager.createCustomProfile(
                    name = name,
                    baseProfile = baseTemplate,
                    selectedButtons = selectedButtons
                )
                showAddDialog = false
                layoutManager.setActiveProfile(newProfile.name)
                Toast.makeText(context, "Created layout '$name'", Toast.LENGTH_SHORT).show()
                if (openStudioImmediately) {
                    navController.navigate("button_studio?mode=select&profileName=${android.net.Uri.encode(name)}")
                }
            },
            onDesignInStudio = { name ->
                showAddDialog = false
                navController.navigate("button_studio?mode=select&profileName=${android.net.Uri.encode(name)}")
            }
        )
    }

    // Duplicate Dialog
    profileToDuplicate?.let { profile ->
        DuplicateLayoutDialog(
            profile = profile,
            onConfirm = { name ->
                layoutManager.createCustomProfile(
                    name = name,
                    baseProfile = profile
                )
                profileToDuplicate = null
                Toast.makeText(context, "Duplicated to '$name'", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { profileToDuplicate = null }
        )
    }

    // Rename Custom Layout Dialog
    profileToRename?.let { profile ->
        RenameLayoutDialog(
            profile = profile,
            onConfirm = { newName ->
                if (newName != profile.name) {
                    val success = layoutManager.renameProfile(profile.name, newName)
                    if (success) {
                        Toast.makeText(context, "Renamed to '$newName'", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Could not rename layout", Toast.LENGTH_SHORT).show()
                    }
                }
                profileToRename = null
            },
            onDismiss = { profileToRename = null }
        )
    }

    // Reset Default Layout Dialog
    profileToReset?.let { profile ->
        ResetLayoutDialog(
            profile = profile,
            onConfirm = {
                layoutManager.resetDefaultProfile(profile.name)
                Toast.makeText(context, "Reset '${profile.name}' to factory default", Toast.LENGTH_SHORT).show()
                profileToReset = null
            },
            onDismiss = { profileToReset = null }
        )
    }

    // Delete Confirmation Dialog
    profileToDelete?.let { profile ->
        DeleteLayoutDialog(
            profile = profile,
            onConfirm = {
                val deleted = layoutManager.deleteProfile(profile.name)
                if (deleted) {
                    Toast.makeText(context, "Deleted '${profile.name}'", Toast.LENGTH_SHORT).show()
                }
                profileToDelete = null
            },
            onDismiss = { profileToDelete = null }
        )
    }

    // Preset Protection Dialog
    profileToEditAsPreset?.let { preset ->
        PresetProtectionDialog(
            preset = preset,
            onConfirm = { customName ->
                val copy = layoutManager.createCustomProfile(name = customName, baseProfile = preset)
                layoutManager.setActiveProfile(copy.name)
                profileToEditAsPreset = null
                navController.navigate(ScreenKey.Editor(profileName = copy.name))
            },
            onDismiss = { profileToEditAsPreset = null }
        )
    }
}
