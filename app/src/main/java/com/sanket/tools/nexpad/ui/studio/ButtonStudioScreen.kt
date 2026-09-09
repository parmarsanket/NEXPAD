package com.sanket.tools.nexpad.ui.studio

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
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
 * High-performance, modular Button Studio Screen.
 * Supports dual modes:
 * - MANAGER: Browse, test in sandbox, import AI JSON, copy JSON, delete custom buttons.
 * - BUILDER (SELECTION): Pick one skin per control type, build custom layout, proceed to HUD.
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

    // Current Studio Mode
    var currentMode by remember { mutableStateOf(initialMode) }

    var selectedCategory by remember { mutableStateOf(STUDIO_CATEGORIES.first()) }
    var selectedSubFilter by remember { mutableStateOf<StudioSubFilter?>(null) }
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

    // Filter components matching the active category
    val filteredComponents = remember(components, selectedCategory, selectedSubFilter) {
        components.filter { def ->
            val control = def.manifest.defaultControl.uppercase()
            val category = def.manifest.category.uppercase()

            if (selectedCategory.id == "ALL") return@filter true

            // For grouped tabs (ABXY, TRIGGERS, BUMPERS, STICKS), built-in presets are grouped into themes.
            // Custom plugins and remote .nxprc buttons always remain visible.
            if (def.manifest.id.startsWith("builtin.") && control in setOf("A", "B", "X", "Y", "LT", "RT", "LB", "RB", "LS", "RS")) {
                return@filter false
            }

            val matchesCategory = selectedCategory.keys.any { k ->
                k.uppercase() == control || category == selectedCategory.id
            }
            if (!matchesCategory) return@filter false

            val sub = selectedSubFilter
            if (sub == null || sub.targetKey == null) {
                true
            } else {
                control == sub.targetKey.uppercase()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Button Studio",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    color = NeonPalette.Cyan
                                )
                            )
                            // Mode Badge
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (currentMode == ButtonStudioMode.SELECTION) NeonPalette.Cyan.copy(alpha = 0.2f) else NeonPalette.Purple.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (currentMode == ButtonStudioMode.SELECTION) NeonPalette.Cyan else NeonPalette.Purple
                                )
                            ) {
                                Text(
                                    if (currentMode == ButtonStudioMode.SELECTION) "LAYOUT BUILDER" else "MANAGER",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (currentMode == ButtonStudioMode.SELECTION) NeonPalette.Cyan else NeonPalette.Purple,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            if (currentMode == ButtonStudioMode.SELECTION) {
                                if (targetProfileName.isNotBlank()) "Selecting controls for '$targetProfileName'" else "Configure custom button layout"
                            } else {
                                "Pick, test, or import controller skins"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(
                            if (currentMode == ButtonStudioMode.MANAGE) Icons.Rounded.DesignServices else Icons.Rounded.Build,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = NeonPalette.Cyan
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (currentMode == ButtonStudioMode.MANAGE) "Switch to Builder" else "Switch to Manager",
                            fontSize = 11.sp
                        )
                    }

                    if (currentMode == ButtonStudioMode.MANAGE) {
                        Spacer(Modifier.width(6.dp))
                        Button(
                            onClick = {
                                filePickerLauncher.launch(arrayOf("*/*"))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9100)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Rounded.FileUpload, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Import .nxprc", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(Modifier.width(6.dp))
                        Button(
                            onClick = { showImportDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPalette.Purple),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .padding(end = 8.dp)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(2.dp))
                            Text("Import JSON", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)
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
                        .width(78.dp)
                        .fillMaxHeight()
                )

                VerticalDivider(color = Color.White.copy(alpha = 0.08f))

                // 2. Right Content Area: Sub-filters + Responsive Grid
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    // Sub-filter Chips Row (if category has multiple options)
                    if (selectedCategory.subFilters.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            selectedCategory.subFilters.forEach { filter ->
                                val isSelected = selectedSubFilter?.id == filter.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedSubFilter = filter },
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
                                    modifier = Modifier.height(30.dp)
                                )
                            }
                        }
                    }

                    // 2-Column Responsive Grid
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        when (selectedCategory.id) {
                            "ABXY" -> {
                                items(
                                    items = ABXY_GROUP_THEMES,
                                    key = { it.id },
                                    span = { GridItemSpan(2) }
                                ) { theme ->
                                    val isThemeSelected = chosenAbxyThemeId == theme.id &&
                                            listOf("A", "B", "X", "Y").all { activeControls[it] == true }

                                    AbxyDiamondGroupCard(
                                        theme = theme,
                                        mode = currentMode,
                                        isSelected = isThemeSelected,
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
                                        onUseInHud = {
                                            if (layoutManager != null) {
                                                val profile = layoutManager.getActiveProfile()
                                                val positions = profile.positions.toMutableMap()
                                                val defaults = defaultPositions()
                                                listOf("A", "B", "X", "Y").forEach { key ->
                                                    val pos = positions[key] ?: defaults[key] ?: Position(0.5f, 0.5f)
                                                    positions[key] = pos.copy(customComponentId = theme.skinMap[key])
                                                }
                                                layoutManager.saveProfile(profile.copy(positions = positions))
                                                layoutManager.pendingSelectedKey = "A"
                                                Toast.makeText(context, "Applied ${theme.name} (4 buttons) to '${profile.name}'", Toast.LENGTH_SHORT).show()
                                            }
                                            navController.navigate("editor")
                                        }
                                    )
                                }
                            }
                            "TRIGGERS" -> {
                                items(
                                    items = TRIGGER_GROUP_THEMES,
                                    key = { it.id },
                                    span = { GridItemSpan(2) }
                                ) { theme ->
                                    val isThemeSelected = chosenTriggerThemeId == theme.id &&
                                            listOf("LT", "RT").all { activeControls[it] == true }

                                    PairedGroupCard(
                                        theme = theme,
                                        category = "TRIGGERS",
                                        mode = currentMode,
                                        isSelected = isThemeSelected,
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
                                        onUseInHud = {
                                            if (layoutManager != null) {
                                                val profile = layoutManager.getActiveProfile()
                                                val positions = profile.positions.toMutableMap()
                                                val defaults = defaultPositions()
                                                listOf("LT", "RT").forEach { key ->
                                                    val pos = positions[key] ?: defaults[key] ?: Position(0.5f, 0.5f)
                                                    positions[key] = pos.copy(customComponentId = theme.skinMap[key])
                                                }
                                                layoutManager.saveProfile(profile.copy(positions = positions))
                                                layoutManager.pendingSelectedKey = "LT"
                                                Toast.makeText(context, "Applied ${theme.name} to '${profile.name}'", Toast.LENGTH_SHORT).show()
                                            }
                                            navController.navigate("editor")
                                        }
                                    )
                                }
                            }
                            "BUMPERS" -> {
                                items(
                                    items = BUMPER_GROUP_THEMES,
                                    key = { it.id },
                                    span = { GridItemSpan(2) }
                                ) { theme ->
                                    val isThemeSelected = chosenBumperThemeId == theme.id &&
                                            listOf("LB", "RB").all { activeControls[it] == true }

                                    PairedGroupCard(
                                        theme = theme,
                                        category = "BUMPERS",
                                        mode = currentMode,
                                        isSelected = isThemeSelected,
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
                                        onUseInHud = {
                                            if (layoutManager != null) {
                                                val profile = layoutManager.getActiveProfile()
                                                val positions = profile.positions.toMutableMap()
                                                val defaults = defaultPositions()
                                                listOf("LB", "RB").forEach { key ->
                                                    val pos = positions[key] ?: defaults[key] ?: Position(0.5f, 0.5f)
                                                    positions[key] = pos.copy(customComponentId = theme.skinMap[key])
                                                }
                                                layoutManager.saveProfile(profile.copy(positions = positions))
                                                layoutManager.pendingSelectedKey = "LB"
                                                Toast.makeText(context, "Applied ${theme.name} to '${profile.name}'", Toast.LENGTH_SHORT).show()
                                            }
                                            navController.navigate("editor")
                                        }
                                    )
                                }
                            }
                            "STICKS" -> {
                                items(
                                    items = STICK_GROUP_THEMES,
                                    key = { it.id },
                                    span = { GridItemSpan(2) }
                                ) { theme ->
                                    val isThemeSelected = chosenStickThemeId == theme.id &&
                                            listOf("LS", "RS").all { activeControls[it] == true }

                                    PairedGroupCard(
                                        theme = theme,
                                        category = "STICKS",
                                        mode = currentMode,
                                        isSelected = isThemeSelected,
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
                                        onUseInHud = {
                                            if (layoutManager != null) {
                                                val profile = layoutManager.getActiveProfile()
                                                val positions = profile.positions.toMutableMap()
                                                val defaults = defaultPositions()
                                                listOf("LS", "RS").forEach { key ->
                                                    val pos = positions[key] ?: defaults[key] ?: Position(0.5f, 0.5f)
                                                    positions[key] = pos.copy(customComponentId = theme.skinMap[key])
                                                }
                                                layoutManager.saveProfile(profile.copy(positions = positions))
                                                layoutManager.pendingSelectedKey = "LS"
                                                Toast.makeText(context, "Applied ${theme.name} to '${profile.name}'", Toast.LENGTH_SHORT).show()
                                            }
                                            navController.navigate("editor")
                                        }
                                    )
                                }
                            }
                            else -> {
                                // Non-Group Controls (D-Pad, Home, System, Macros, All)
                                items(filteredComponents, key = { it.manifest.id }) { def ->
                                    val targetKey = def.manifest.defaultControl.uppercase()
                                    val isControlActive = activeControls[targetKey] == true
                                    val isSkinSelected = chosenSkins[targetKey] == def.manifest.id ||
                                            (resolveButtonSourceType(def) == ButtonStudioType.DEFAULT && chosenSkins[targetKey] == null)

                                    StudioGridCard(
                                        def = def,
                                        mode = currentMode,
                                        isSelectedInBuilder = isControlActive && isSkinSelected,
                                        dummyViewModel = dummyViewModel,
                                        onToggleSelectInBuilder = {
                                            if (isControlActive && isSkinSelected) {
                                                activeControls[targetKey] = false
                                                Toast.makeText(context, "Excluded $targetKey from layout", Toast.LENGTH_SHORT).show()
                                            } else {
                                                activeControls[targetKey] = true
                                                chosenSkins[targetKey] = if (resolveButtonSourceType(def) == ButtonStudioType.DEFAULT) null else def.manifest.id
                                                Toast.makeText(context, "Selected ${def.manifest.name} for $targetKey", Toast.LENGTH_SHORT).show()
                                            }
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
 * Places the selected button with its skin onto the active profile,
 * selects it in LayoutManager, and opens HudEditorScreen.
 */
private fun applyButtonToHud(
    def: NxpComponentDef,
    layoutManager: LayoutManager?,
    navController: NavController,
    context: Context
) {
    val targetKey = def.manifest.defaultControl.uppercase()
    val type = resolveButtonSourceType(def)
    val customId = if (type == ButtonStudioType.DEFAULT) null else def.manifest.id

    if (layoutManager != null) {
        val activeProfile = layoutManager.getActiveProfile()
        val positions = activeProfile.positions.toMutableMap()
        val existingPos = positions[targetKey] ?: defaultPositions()[targetKey] ?: Position(0.5f, 0.5f)
        positions[targetKey] = existingPos.copy(customComponentId = customId)

        layoutManager.saveProfile(activeProfile.copy(positions = positions))
        layoutManager.pendingSelectedKey = targetKey

        Toast.makeText(
            context,
            "Added $targetKey to '${activeProfile.name}'. Place and size on HUD!",
            Toast.LENGTH_SHORT
        ).show()
    }

    navController.navigate("editor")
}
