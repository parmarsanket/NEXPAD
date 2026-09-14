package com.sanket.tools.nexpad.ui.studio

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.sanket.tools.nexpad.category.CategoryManager
import com.sanket.tools.nexpad.model.Position
import com.sanket.tools.nexpad.model.defaultPositions
import com.sanket.tools.nexpad.runtime.model.NxpComponentDef
import com.sanket.tools.nexpad.runtime.plugin.RemoteComponentRegistry
import com.sanket.tools.nexpad.runtime.registry.ComponentRegistry
import com.sanket.tools.nexpad.ui.components.effects.CyberGrid
import com.sanket.tools.nexpad.ui.components.effects.ScanLine
import com.sanket.tools.nexpad.ui.studio.components.*
import com.sanket.tools.nexpad.ui.studio.model.*
import com.sanket.tools.nexpad.ui.theme.NeonPalette
import com.sanket.tools.nexpad.utils.LayoutManager
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel

/**
 * Modern, high-performance Button Studio Screen.
 * Unifies component skin browsing, sandbox testing, individual button customization,
 * cohesive cluster themes, and direct integration with active HUD layout profiles.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ButtonStudioScreen(
    navController: NavController,
    layoutManager: LayoutManager?,
    initialMode: ButtonStudioMode = ButtonStudioMode.MANAGE,
    targetProfileName: String = ""
) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val registry = remember { ComponentRegistry.getInstance(context) }
    val components by registry.installedComponents.collectAsState()
    val dummyViewModel = viewModel<GamepadViewModel>()

    // Current Studio Mode (MANAGE vs SELECTION)
    var currentMode by remember { mutableStateOf(initialMode) }

    // Active Profile State (refreshes on apply)
    var activeProfile by remember(layoutManager) {
        mutableStateOf(layoutManager?.getActiveProfile())
    }

    var selectedCategory by remember { mutableStateOf(STUDIO_CATEGORIES.first()) }
    var selectedSubFilter by remember { mutableStateOf<StudioSubFilter?>(null) }
    var showImportMenu by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var previewTarget by remember { mutableStateOf<NxpComponentDef?>(null) }

    // Map of active controls (controlKey -> Boolean) in Builder mode
    val activeControls = remember {
        mutableStateMapOf<String, Boolean>().apply {
            listOf("A", "B", "X", "Y", "LS", "RS", "DPAD", "LT", "RT", "LB", "RB", "XBOX", "VIEW", "MENU").forEach {
                put(it, true)
            }
        }
    }

    // Map of chosen skin IDs (controlKey -> skinId)
    val chosenSkins = remember { mutableStateMapOf<String, String?>() }

    // Selected Group theme IDs (defaults to Classic Xbox)
    var chosenAbxyThemeId by remember { mutableStateOf("group.classic_xbox") }
    var chosenTriggerThemeId by remember { mutableStateOf("group.triggers_classic") }
    var chosenBumperThemeId by remember { mutableStateOf("group.bumpers_classic") }
    var chosenStickThemeId by remember { mutableStateOf("group.sticks_classic") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val result = RemoteComponentRegistry.getInstance(context).importFromUri(context, uri)
            result.onSuccess { doc ->
                Toast.makeText(context, "Imported ${doc.manifest.name} (.nxprc)!", Toast.LENGTH_SHORT).show()
            }.onFailure { err ->
                Toast.makeText(context, "Failed to import .nxprc: ${err.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Filter components matching the active category & sub-filter
    val filteredComponents = remember(components, selectedCategory, selectedSubFilter) {
        components.filter { def ->
            val control = def.manifest.defaultControl.uppercase()
            val category = def.manifest.category.uppercase()

            if (selectedCategory.id == "ALL") return@filter true

            val matchesCategory = selectedCategory.keys.any { k ->
                k.uppercase() == control || category == selectedCategory.id
            }
            if (!matchesCategory) return@filter false

            val sub = selectedSubFilter
            if (sub == null || sub.id == "ALL" || sub.id == "GROUP_THEMES") {
                true
            } else {
                val target = (sub.targetKey ?: sub.id).uppercase()
                control == target || CategoryManager.getControl(control)?.key?.uppercase() == target
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Button Studio",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = NeonPalette.Cyan,
                                    fontSize = 17.sp
                                ),
                                maxLines = 1
                            )

                            // Active Profile Badge
                            val profileTitle = activeProfile?.name ?: targetProfileName.ifBlank { "Default" }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = NeonPalette.Cyan.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NeonPalette.Cyan.copy(alpha = 0.6f))
                            ) {
                                Text(
                                    text = profileTitle,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonPalette.Cyan,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = if (currentMode == ButtonStudioMode.SELECTION) "Select buttons for custom layout" else "Pick skins, test in sandbox, or apply to HUD",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Mode Switcher Toggle
                    OutlinedButton(
                        onClick = {
                            currentMode = if (currentMode == ButtonStudioMode.MANAGE) {
                                ButtonStudioMode.SELECTION
                            } else {
                                ButtonStudioMode.MANAGE
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonPalette.Cyan.copy(alpha = 0.45f)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(
                            if (currentMode == ButtonStudioMode.MANAGE) Icons.Rounded.Build else Icons.Rounded.Palette,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = NeonPalette.Cyan
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (currentMode == ButtonStudioMode.MANAGE) "Builder" else "Studio",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Import Menu (dropdown keeps top bar clean!)
                    Box {
                        IconButton(
                            onClick = { showImportMenu = true },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                Icons.Rounded.FileUpload,
                                contentDescription = "Import",
                                tint = Color(0xFFFF9100),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showImportMenu,
                            onDismissRequest = { showImportMenu = false },
                            modifier = Modifier.background(Color(0xFF0F172A))
                        ) {
                            DropdownMenuItem(
                                text = { Text("⚡ Import .nxprc (Remote)", color = Color.White, fontSize = 12.sp) },
                                onClick = {
                                    showImportMenu = false
                                    filePickerLauncher.launch(arrayOf("*/*"))
                                },
                                leadingIcon = {
                                    Icon(Icons.Rounded.FlashOn, contentDescription = null, tint = Color(0xFFFF9100), modifier = Modifier.size(16.dp))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("📄 Import JSON (NXP Spec)", color = Color.White, fontSize = 12.sp) },
                                onClick = {
                                    showImportMenu = false
                                    showImportDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Code, contentDescription = null, tint = NeonPalette.Purple, modifier = Modifier.size(16.dp))
                                }
                            )
                        }
                    }

                    // Open HUD Editor Button
                    Button(
                        onClick = { navController.navigate("editor") },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPalette.Cyan),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .padding(end = 4.dp)
                    ) {
                        Icon(Icons.Rounded.DashboardCustomize, contentDescription = null, tint = Color.Black, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("HUD", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                )
            )
        },
        bottomBar = {
            if (currentMode == ButtonStudioMode.SELECTION) {
                SelectionModeBottomBar(
                    activeCount = activeControls.values.count { it },
                    totalCount = 19,
                    onProceedToHud = {
                        val layoutName = targetProfileName.trim().ifEmpty {
                            "Custom Layout ${System.currentTimeMillis() % 1000}"
                        }
                        if (layoutManager != null) {
                            val activeKeys = activeControls.filterValues { it }.keys
                            val defaults = defaultPositions()

                            val positions = activeKeys.associateWith { key ->
                                val basePos = defaults[key] ?: Position(0.5f, 0.5f)
                                basePos.copy(customComponentId = chosenSkins[key])
                            }

                            val base = layoutManager.getActiveProfile()
                            val newProfile = layoutManager.createCustomProfile(
                                name = layoutName,
                                baseProfile = base,
                                selectedButtons = activeKeys
                            ).copy(
                                positions = positions,
                                description = "Custom Layout designed in Button Studio (${activeKeys.size} controls)"
                            )

                            layoutManager.saveProfile(newProfile)
                            layoutManager.setActiveProfile(newProfile.name)
                            activeProfile = newProfile

                            Toast.makeText(
                                context,
                                "Created '$layoutName' with ${activeKeys.size} controls. Place and size on HUD!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        navController.navigate("editor")
                    }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            CyberGrid(modifier = Modifier.matchParentSize())
            ScanLine(modifier = Modifier.matchParentSize())

            Row(modifier = Modifier.fillMaxSize()) {
                // 1. Sleek Vertical Navigation Rail (Slider on Left)
                StudioVerticalRail(
                    categories = STUDIO_CATEGORIES,
                    selectedCategoryId = selectedCategory.id,
                    onSelectCategory = { cat ->
                        selectedCategory = cat
                        selectedSubFilter = cat.subFilters.firstOrNull()
                    },
                    mode = currentMode,
                    activeCountForCategory = { cat ->
                        cat.keys.count { activeControls[it] == true }
                    },
                    modifier = Modifier
                        .width(74.dp)
                        .fillMaxHeight()
                )

                VerticalDivider(color = Color.White.copy(alpha = 0.08f))

                // 2. Right Content Area: Sub-filters + Responsive Grid
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    // Sub-filter Chips Row
                    if (selectedCategory.subFilters.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            selectedCategory.subFilters.forEach { filter ->
                                val isSelected = selectedSubFilter?.id == filter.id ||
                                        (selectedSubFilter == null && filter.id == "ALL")
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedSubFilter = if (filter.id == "ALL") null else filter
                                    },
                                    label = {
                                        Text(
                                            filter.label,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NeonPalette.Cyan.copy(alpha = 0.22f),
                                        selectedLabelColor = NeonPalette.Cyan,
                                        containerColor = Color.White.copy(alpha = 0.05f),
                                        labelColor = Color.LightGray
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = Color.White.copy(alpha = 0.15f),
                                        selectedBorderColor = NeonPalette.Cyan
                                    ),
                                    modifier = Modifier.height(28.dp)
                                )
                            }
                        }
                    }

                    // Responsive Grid (Adaptive minSize = 145dp ensures cards never clip)
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 145.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isGroupThemesFilter = selectedSubFilter?.id == "GROUP_THEMES"
                        val isAllFilter = selectedSubFilter == null || selectedSubFilter?.id == "ALL"

                        // Group Themes Section for Cluster Categories
                        if (isGroupThemesFilter || isAllFilter) {
                            when (selectedCategory.id) {
                                "ABXY" -> {
                                    items(
                                        items = if (isGroupThemesFilter) ABXY_GROUP_THEMES else ABXY_GROUP_THEMES.take(1),
                                        key = { "group_${it.id}" },
                                        span = { GridItemSpan(maxLineSpan) }
                                    ) { theme ->
                                        val isThemeSelected = chosenAbxyThemeId == theme.id &&
                                                listOf("A", "B", "X", "Y").all { activeControls[it] == true }
                                        val isApplied = activeProfile?.positions?.let { pos ->
                                            listOf("A", "B", "X", "Y").all { k -> pos[k]?.customComponentId == theme.skinMap[k] }
                                        } ?: false

                                        AbxyDiamondGroupCard(
                                            theme = theme,
                                            mode = currentMode,
                                            isSelected = isThemeSelected,
                                            isAppliedToActiveProfile = isApplied,
                                            dummyViewModel = dummyViewModel,
                                            onSelectGroup = {
                                                if (isThemeSelected) {
                                                    listOf("A", "B", "X", "Y").forEach { key -> activeControls[key] = false }
                                                    Toast.makeText(context, "Excluded ABXY Group from layout", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    chosenAbxyThemeId = theme.id
                                                    listOf("A", "B", "X", "Y").forEach { key ->
                                                        activeControls[key] = true
                                                        chosenSkins[key] = theme.skinMap[key]
                                                    }
                                                    Toast.makeText(context, "Selected ${theme.name} (All 4 Buttons)", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            onApplyToProfile = {
                                                applyGroupThemeToProfile(theme.name, theme.skinMap, layoutManager, context)
                                                activeProfile = layoutManager?.getActiveProfile()
                                            },
                                            onUseInHud = {
                                                applyGroupThemeToProfile(theme.name, theme.skinMap, layoutManager, context)
                                                layoutManager?.pendingSelectedKey = "A"
                                                navController.navigate("editor")
                                            }
                                        )
                                    }
                                }
                                "TRIGGERS" -> {
                                    items(
                                        items = if (isGroupThemesFilter) TRIGGER_GROUP_THEMES else TRIGGER_GROUP_THEMES.take(1),
                                        key = { "group_${it.id}" },
                                        span = { GridItemSpan(maxLineSpan) }
                                    ) { theme ->
                                        val isThemeSelected = chosenTriggerThemeId == theme.id &&
                                                listOf("LT", "RT").all { activeControls[it] == true }
                                        val isApplied = activeProfile?.positions?.let { pos ->
                                            listOf("LT", "RT").all { k -> pos[k]?.customComponentId == theme.skinMap[k] }
                                        } ?: false

                                        PairedGroupCard(
                                            theme = theme,
                                            category = "TRIGGERS",
                                            mode = currentMode,
                                            isSelected = isThemeSelected,
                                            isAppliedToActiveProfile = isApplied,
                                            dummyViewModel = dummyViewModel,
                                            onSelectGroup = {
                                                if (isThemeSelected) {
                                                    listOf("LT", "RT").forEach { key -> activeControls[key] = false }
                                                    Toast.makeText(context, "Excluded Triggers (LT & RT) from layout", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    chosenTriggerThemeId = theme.id
                                                    listOf("LT", "RT").forEach { key ->
                                                        activeControls[key] = true
                                                        chosenSkins[key] = theme.skinMap[key]
                                                    }
                                                    Toast.makeText(context, "Selected ${theme.name} (LT & RT)", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            onApplyToProfile = {
                                                applyGroupThemeToProfile(theme.name, theme.skinMap, layoutManager, context)
                                                activeProfile = layoutManager?.getActiveProfile()
                                            },
                                            onUseInHud = {
                                                applyGroupThemeToProfile(theme.name, theme.skinMap, layoutManager, context)
                                                layoutManager?.pendingSelectedKey = "LT"
                                                navController.navigate("editor")
                                            }
                                        )
                                    }
                                }
                                "BUMPERS" -> {
                                    items(
                                        items = if (isGroupThemesFilter) BUMPER_GROUP_THEMES else BUMPER_GROUP_THEMES.take(1),
                                        key = { "group_${it.id}" },
                                        span = { GridItemSpan(maxLineSpan) }
                                    ) { theme ->
                                        val isThemeSelected = chosenBumperThemeId == theme.id &&
                                                listOf("LB", "RB").all { activeControls[it] == true }
                                        val isApplied = activeProfile?.positions?.let { pos ->
                                            listOf("LB", "RB").all { k -> pos[k]?.customComponentId == theme.skinMap[k] }
                                        } ?: false

                                        PairedGroupCard(
                                            theme = theme,
                                            category = "BUMPERS",
                                            mode = currentMode,
                                            isSelected = isThemeSelected,
                                            isAppliedToActiveProfile = isApplied,
                                            dummyViewModel = dummyViewModel,
                                            onSelectGroup = {
                                                if (isThemeSelected) {
                                                    listOf("LB", "RB").forEach { key -> activeControls[key] = false }
                                                    Toast.makeText(context, "Excluded Bumpers (LB & RB) from layout", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    chosenBumperThemeId = theme.id
                                                    listOf("LB", "RB").forEach { key ->
                                                        activeControls[key] = true
                                                        chosenSkins[key] = theme.skinMap[key]
                                                    }
                                                    Toast.makeText(context, "Selected ${theme.name} (LB & RB)", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            onApplyToProfile = {
                                                applyGroupThemeToProfile(theme.name, theme.skinMap, layoutManager, context)
                                                activeProfile = layoutManager?.getActiveProfile()
                                            },
                                            onUseInHud = {
                                                applyGroupThemeToProfile(theme.name, theme.skinMap, layoutManager, context)
                                                layoutManager?.pendingSelectedKey = "LB"
                                                navController.navigate("editor")
                                            }
                                        )
                                    }
                                }
                                "STICKS" -> {
                                    items(
                                        items = if (isGroupThemesFilter) STICK_GROUP_THEMES else STICK_GROUP_THEMES.take(1),
                                        key = { "group_${it.id}" },
                                        span = { GridItemSpan(maxLineSpan) }
                                    ) { theme ->
                                        val isThemeSelected = chosenStickThemeId == theme.id &&
                                                listOf("LS", "RS").all { activeControls[it] == true }
                                        val isApplied = activeProfile?.positions?.let { pos ->
                                            listOf("LS", "RS").all { k -> pos[k]?.customComponentId == theme.skinMap[k] }
                                        } ?: false

                                        PairedGroupCard(
                                            theme = theme,
                                            category = "STICKS",
                                            mode = currentMode,
                                            isSelected = isThemeSelected,
                                            isAppliedToActiveProfile = isApplied,
                                            dummyViewModel = dummyViewModel,
                                            onSelectGroup = {
                                                if (isThemeSelected) {
                                                    listOf("LS", "RS").forEach { key -> activeControls[key] = false }
                                                    Toast.makeText(context, "Excluded Sticks (LS & RS) from layout", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    chosenStickThemeId = theme.id
                                                    listOf("LS", "RS").forEach { key ->
                                                        activeControls[key] = true
                                                        chosenSkins[key] = theme.skinMap[key]
                                                    }
                                                    Toast.makeText(context, "Selected ${theme.name} (LS & RS)", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            onApplyToProfile = {
                                                applyGroupThemeToProfile(theme.name, theme.skinMap, layoutManager, context)
                                                activeProfile = layoutManager?.getActiveProfile()
                                            },
                                            onUseInHud = {
                                                applyGroupThemeToProfile(theme.name, theme.skinMap, layoutManager, context)
                                                layoutManager?.pendingSelectedKey = "LS"
                                                navController.navigate("editor")
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Individual Component Cards (shown when not exclusively viewing group themes)
                        if (!isGroupThemesFilter) {
                            items(filteredComponents, key = { it.manifest.id }) { def ->
                                val targetKey = def.manifest.defaultControl.uppercase()
                                val isControlActive = activeControls[targetKey] == true
                                val type = resolveButtonSourceType(def)
                                val expectedCustomId = if (type == ButtonStudioType.DEFAULT) null else def.manifest.id
                                val isSkinSelected = chosenSkins[targetKey] == def.manifest.id ||
                                        (type == ButtonStudioType.DEFAULT && chosenSkins[targetKey] == null)

                                val isAppliedToProfile = activeProfile?.positions?.get(targetKey)?.customComponentId == expectedCustomId

                                StudioGridCard(
                                    def = def,
                                    mode = currentMode,
                                    isSelectedInBuilder = isControlActive && isSkinSelected,
                                    isAppliedToActiveProfile = isAppliedToProfile,
                                    dummyViewModel = dummyViewModel,
                                    onToggleSelectInBuilder = {
                                        if (isControlActive && isSkinSelected) {
                                            activeControls[targetKey] = false
                                            Toast.makeText(context, "Excluded $targetKey from layout", Toast.LENGTH_SHORT).show()
                                        } else {
                                            activeControls[targetKey] = true
                                            chosenSkins[targetKey] = if (type == ButtonStudioType.DEFAULT) null else def.manifest.id
                                            Toast.makeText(context, "Selected ${def.manifest.name} for $targetKey", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onApplyToProfile = {
                                        applyButtonSkinToProfile(def, layoutManager, context)
                                        activeProfile = layoutManager?.getActiveProfile()
                                    },
                                    onUseInHud = {
                                        applyButtonToHud(def, layoutManager, navController, context)
                                    },
                                    onTest = { previewTarget = def },
                                    onExport = {
                                        val json = registry.exportToJson(def.manifest.id)
                                        if (json != null) {
                                            clipboard.nativeClipboard.setPrimaryClip(android.content.ClipData.newPlainText("NXP JSON", json))
                                            Toast.makeText(context, "JSON copied to clipboard!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onDelete = {
                                        val deleted = registry.deleteComponent(def.manifest.id)
                                        if (deleted) {
                                            Toast.makeText(context, "Deleted ${def.manifest.name}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Live Sandbox Modal
    if (previewTarget != null) {
        SandboxPreviewModal(
            componentDef = previewTarget!!,
            onDismiss = { previewTarget = null },
            onAddToHud = {
                val target = previewTarget!!
                previewTarget = null
                applyButtonToHud(target, layoutManager, navController, context)
            }
        )
    }

    // AI JSON Import Dialog
    if (showImportDialog) {
        ComponentImportDialog(
            onDismiss = { showImportDialog = false },
            onImportSuccess = { def ->
                showImportDialog = false
                previewTarget = def
                Toast.makeText(context, "Imported ${def.manifest.name}!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

/**
 * Applies button skin to active profile immediately without navigating away.
 */
private fun applyButtonSkinToProfile(
    def: NxpComponentDef,
    layoutManager: LayoutManager?,
    context: Context
) {
    val targetKey = def.manifest.defaultControl.uppercase()
    val type = resolveButtonSourceType(def)
    val customId = if (type == ButtonStudioType.DEFAULT) null else def.manifest.id

    if (layoutManager != null) {
        layoutManager.applyButtonSkinToActiveProfile(targetKey, customId)
        val profileName = layoutManager.getActiveProfile().name
        Toast.makeText(
            context,
            "Applied ${def.manifest.name} to $targetKey on '$profileName'",
            Toast.LENGTH_SHORT
        ).show()
    }
}

/**
 * Applies a full group theme preset to active profile immediately.
 */
private fun applyGroupThemeToProfile(
    themeName: String,
    skinMap: Map<String, String?>,
    layoutManager: LayoutManager?,
    context: Context
) {
    if (layoutManager != null) {
        val activeProfile = layoutManager.getActiveProfile()
        val positions = activeProfile.positions.toMutableMap()
        val defaults = defaultPositions()
        skinMap.forEach { (key, customId) ->
            val existingPos = positions[key] ?: defaults[key] ?: Position(0.5f, 0.5f)
            positions[key] = existingPos.copy(customComponentId = customId)
        }
        layoutManager.saveProfile(activeProfile.copy(positions = positions))
        Toast.makeText(
            context,
            "Applied $themeName (${skinMap.keys.joinToString(", ")}) to '${activeProfile.name}'",
            Toast.LENGTH_SHORT
        ).show()
    }
}

/**
 * Places the selected button with its skin onto the active profile,
 * selects it in LayoutManager, and opens HudEditorScreen.
 */
private fun applyButtonToHud(
    def: NxpComponentDef,
    layoutManager: LayoutManager?,
    navController: NavController,
    context: Context
) {
    applyButtonSkinToProfile(def, layoutManager, context)
    layoutManager?.pendingSelectedKey = def.manifest.defaultControl.uppercase()
    navController.navigate("editor")
}

