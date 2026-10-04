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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.category.ControllerLabelStyle
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

    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var duplicateProfileName by rememberSaveable { mutableStateOf<String?>(null) }
    var renameProfileName by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteProfileName by rememberSaveable { mutableStateOf<String?>(null) }
    var resetProfileName by rememberSaveable { mutableStateOf<String?>(null) }
    var editAsPresetProfileName by rememberSaveable { mutableStateOf<String?>(null) }

    val profileToDuplicate = profiles.firstOrNull { it.name == duplicateProfileName }
    val profileToRename = profiles.firstOrNull { it.name == renameProfileName }
    val profileToDelete = profiles.firstOrNull { it.name == deleteProfileName }
    val profileToReset = profiles.firstOrNull { it.name == resetProfileName }
    val profileToEditAsPreset = profiles.firstOrNull { it.name == editAsPresetProfileName }

    VirtualControllerScreenContent(
        profiles = profiles,
        activeProfileName = activeProfileName,
        onBack = { navController.popBackStack() },
        onOpenButtonStudio = { navController.navigate(Route.ButtonStudio(mode = "viewer")) },
        onAddCustom = { showAddDialog = true },
        onSetActive = { profile ->
            layoutManager.setActiveProfile(profile.name)
            Toast.makeText(context, "Activated ${profile.name}", Toast.LENGTH_SHORT).show()
        },
        onPlay = { profile ->
            navigationViewModel?.startGamepadSession(profile.name)
            navController.navigate(Route.Gamepad(profile.name))
        },
        onEditHud = { profile ->
            if (profile.isDefault) {
                // Preset protection: require creating a custom copy first
                editAsPresetProfileName = profile.name
            } else {
                navController.navigate(Route.Editor(profileName = profile.name))
            }
        },
        onOpenStudio = { profile ->
            navController.navigate(Route.ButtonStudio(mode = "editor", profileName = profile.name))
        },
        onDuplicate = { profile ->
            duplicateProfileName = profile.name
        },
        onRename = { profile ->
            renameProfileName = profile.name
        },
        onShare = { profile ->
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
        onReset = { profile ->
            resetProfileName = profile.name
        },
        onDelete = { profile ->
            if (profile.isDefault) {
                Toast.makeText(context, "Default layouts are protected and cannot be deleted", Toast.LENGTH_LONG).show()
            } else {
                deleteProfileName = profile.name
            }
        },
        onUpdateLabelStyle = { profile, newStyle ->
            layoutManager.setProfileLabelStyle(profile.name, newStyle)
            Toast.makeText(context, "${profile.name}: ${newStyle.displayName}", Toast.LENGTH_SHORT).show()
        },
        onProfileOrderChange = { newOrder ->
            layoutManager.saveProfileOrder(newOrder)
            Toast.makeText(context, "Layout order updated", Toast.LENGTH_SHORT).show()
        }
    )

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
                    navController.navigate(Route.ButtonStudio(mode = "select", profileName = name))
                }
            },
            onDesignInStudio = { name ->
                showAddDialog = false
                navController.navigate(Route.ButtonStudio(mode = "select", profileName = name))
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
                duplicateProfileName = null
                Toast.makeText(context, "Duplicated to '$name'", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { duplicateProfileName = null }
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
                renameProfileName = null
            },
            onDismiss = { renameProfileName = null }
        )
    }

    // Reset Default Layout Dialog
    profileToReset?.let { profile ->
        ResetLayoutDialog(
            profile = profile,
            onConfirm = {
                layoutManager.resetDefaultProfile(profile.name)
                Toast.makeText(context, "Reset '${profile.name}' to factory default", Toast.LENGTH_SHORT).show()
                resetProfileName = null
            },
            onDismiss = { resetProfileName = null }
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
                deleteProfileName = null
            },
            onDismiss = { deleteProfileName = null }
        )
    }

    // Preset Protection Dialog
    profileToEditAsPreset?.let { preset ->
        PresetProtectionDialog(
            preset = preset,
            onConfirm = { customName ->
                val copy = layoutManager.createCustomProfile(name = customName, baseProfile = preset, activate = false)
                editAsPresetProfileName = null
                navController.navigate(Route.Editor(profileName = copy.name))
            },
            onDismiss = { editAsPresetProfileName = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VirtualControllerScreenContent(
    profiles: List<LayoutProfile> = getDefaultLayoutProfiles(),
    activeProfileName: String = "Standard Elite",
    isAnyItemDragging: Boolean = false,
    onBack: () -> Unit = {},
    onOpenButtonStudio: () -> Unit = {},
    onAddCustom: () -> Unit = {},
    onSetActive: (LayoutProfile) -> Unit = {},
    onPlay: (LayoutProfile) -> Unit = {},
    onEditHud: (LayoutProfile) -> Unit = {},
    onOpenStudio: (LayoutProfile) -> Unit = {},
    onDuplicate: (LayoutProfile) -> Unit = {},
    onRename: (LayoutProfile) -> Unit = {},
    onShare: (LayoutProfile) -> Unit = {},
    onReset: (LayoutProfile) -> Unit = {},
    onDelete: (LayoutProfile) -> Unit = {},
    onUpdateLabelStyle: (LayoutProfile, ControllerLabelStyle) -> Unit = { _, _ -> },
    onProfileOrderChange: (List<String>) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val gridState = rememberLazyGridState()
    val haptic = LocalHapticFeedback.current

    var localProfiles by remember(profiles) { mutableStateOf(profiles) }
    var dragging by remember { mutableStateOf(isAnyItemDragging) }

    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val isVeryCompact = screenWidthDp < 380
    val isCompact = screenWidthDp < 600

    Scaffold(
        modifier = modifier,
        topBar = {
            NexpadTopAppBar(
                title = "Virtual Controller",
                subtitle = if (isCompact) "Layouts & Buttons" else "Layouts & Button Management",
                onBack = onBack,
                isNavigationEnabled = !dragging,
                actions = {
                    if (isVeryCompact) {
                        IconButton(
                            onClick = onOpenButtonStudio,
                            enabled = !dragging,
                            modifier = Modifier.padding(end = 2.dp).size(36.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Palette,
                                contentDescription = "Button Studio",
                                tint = if (dragging) NeonPalette.Purple.copy(alpha = 0.4f) else NeonPalette.Purple,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        FilledIconButton(
                            onClick = onAddCustom,
                            enabled = !dragging,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = NeonPalette.Cyan,
                                contentColor = Color.Black
                            ),
                            modifier = Modifier.padding(end = 8.dp).size(36.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Add,
                                contentDescription = "Add Custom",
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        OutlinedButton(
                            onClick = onOpenButtonStudio,
                            enabled = !dragging,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonPalette.Purple),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonPalette.Purple.copy(alpha = if (dragging) 0.3f else 0.7f)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.padding(end = 6.dp).height(36.dp)
                        ) {
                            Icon(Icons.Rounded.Palette, contentDescription = null, tint = if (dragging) NeonPalette.Purple.copy(alpha = 0.4f) else NeonPalette.Purple, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (isCompact) "Studio" else "Button Studio", color = if (dragging) NeonPalette.Purple.copy(alpha = 0.4f) else NeonPalette.Purple, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        Button(
                            onClick = onAddCustom,
                            enabled = !dragging,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPalette.Cyan),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.padding(end = 8.dp).height(36.dp)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (isCompact) "Add" else "Add Custom", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
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
            val isNowDragging = reorderableLazyGridState.isAnyItemDragging
            if (dragging && !isNowDragging) {
                val newOrder = localProfiles.map { it.name }
                if (newOrder != profiles.map { it.name }) {
                    onProfileOrderChange(newOrder)
                }
            }
            dragging = isNowDragging
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
                item(
                    key = "hub_banner",
                    span = { GridItemSpan(maxLineSpan) },
                    contentType = "hub_banner"
                ) {
                    LayoutHubBanner()
                }

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
                    ) { isItemDragging ->
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
                            isDragging = isItemDragging,
                            onSetActive = { onSetActive(profile) },
                            onPlay = { onPlay(profile) },
                            onEditHud = { onEditHud(profile) },
                            onOpenStudio = { onOpenStudio(profile) },
                            onDuplicate = { onDuplicate(profile) },
                            onRename = { onRename(profile) },
                            onShare = { onShare(profile) },
                            onReset = { onReset(profile) },
                            onDelete = { onDelete(profile) },
                            onUpdateLabelStyle = { newStyle -> onUpdateLabelStyle(profile, newStyle) }
                        )
                    }
                }
            }
        }
    }
}
