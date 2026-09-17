package com.sanket.tools.nexpad.ui.studio

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
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
import com.sanket.tools.nexpad.ui.AppNavigator
import com.sanket.tools.nexpad.ui.NavigationViewModel
import com.sanket.tools.nexpad.ui.ScreenKey
import com.sanket.tools.nexpad.category.CategoryManager
import com.sanket.tools.nexpad.model.GamepadControl
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

/**
 * Modern, high-performance Button Studio Screen.
 * Unifies component skin browsing, sandbox testing, individual button customization,
 * cohesive cluster themes, and direct integration with active HUD layout profiles.
 *
 * When opened in SELECTION Mode with a [targetControlKey], acts as a contextual
 * asset picker: the user selects a skin and returns the result to the calling screen
 * via [navigationViewModel] without directly mutating the layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ButtonStudioScreen(
    navController: AppNavigator,
    layoutManager: LayoutManager?,
    navigationViewModel: NavigationViewModel? = null,
    initialMode: ButtonStudioMode = ButtonStudioMode.MANAGE,
    targetProfileName: String = "",
    /** The GamepadControl.key being edited contextually (e.g. "RT"). Null = Manage Mode. */
    targetControlKey: String? = null,
    /** The asset ID currently applied to targetControlKey. Used for "✓ Current" badge. */
    targetCurrentAssetId: String? = null
) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val registry = remember { ComponentRegistry.getInstance(context) }
    val components by registry.installedComponents.collectAsState()

    // Current Studio Mode (MANAGE vs SELECTION)
    var currentMode by remember { mutableStateOf(initialMode) }

    // Whether we are in a contextual selection session (launched from HUD Editor / VirtualController)
    val isContextual = initialMode == ButtonStudioMode.SELECTION && targetControlKey != null

    // Active Profile State (refreshes on apply)
    var activeProfile by remember(layoutManager) {
        mutableStateOf(layoutManager?.getActiveProfile())
    }

    // --- Step 5: Auto-navigate to the correct category when opened contextually ---
    val initialCategory = remember(targetControlKey) {
        if (targetControlKey != null) {
            val gc = GamepadControl.fromKey(targetControlKey)
            if (gc != null) {
                // Find the StudioCategory whose keys include this control
                STUDIO_CATEGORIES.find { cat ->
                    cat.keys.any { k -> k.equals(targetControlKey, ignoreCase = true) }
                } ?: STUDIO_CATEGORIES.first()
            } else STUDIO_CATEGORIES.first()
        } else STUDIO_CATEGORIES.first()
    }

    val initialSubFilter = remember(targetControlKey, initialCategory) {
        if (targetControlKey != null) {
            // Try to find the sub-filter matching the exact control key
            initialCategory.subFilters.find { sub ->
                (sub.targetKey ?: sub.id).equals(targetControlKey, ignoreCase = true)
            } ?: initialCategory.subFilters.firstOrNull()
        } else null
    }

    var selectedCategory by remember { mutableStateOf(initialCategory) }
    var selectedSubFilter by remember { mutableStateOf<StudioSubFilter?>(initialSubFilter) }
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
            if (sub == null || sub.id == "ALL") {
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

                        // --- Step 4: Context Banner for Selection Mode ---
                        if (isContextual && targetControlKey != null) {
                            // Show which control and profile the user is editing
                            val controlLabel = GamepadControl.fromKey(targetControlKey)?.displayName
                                ?: targetControlKey
                            val currentLabel = when {
                                targetCurrentAssetId == null -> "Default"
                                else -> targetCurrentAssetId.substringAfterLast(".").replace("_", " ")
                                    .replaceFirstChar { it.uppercase() }
                            }
                            Text(
                                text = "Changing appearance for $controlLabel  •  Currently: $currentLabel",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = NeonPalette.Cyan.copy(alpha = 0.85f),
                                    fontSize = 10.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
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
                    // Mode Switcher Toggle — hidden when in a contextual selection session
                    if (!isContextual) {
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

                    // Open HUD Editor Button — hidden in contextual selection mode (use Back to return)
                    if (!isContextual) {
                        Button(
                            onClick = { navController.navigate(ScreenKey.Editor()) },
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
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                )
            )
        },
        bottomBar = {
            if (currentMode == ButtonStudioMode.SELECTION && !isContextual) {
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
                        navController.navigate(ScreenKey.Editor())
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

                    // Responsive Grid of Individual Button Skins
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredComponents, key = { it.manifest.id }) { def ->
                            val targetKey = def.manifest.defaultControl.uppercase()
                            val isControlActive = activeControls[targetKey] == true
                            val type = resolveButtonSourceType(def)
                            val expectedCustomId = if (type == ButtonStudioType.DEFAULT) null else def.manifest.id
                            val isSkinSelected = chosenSkins[targetKey] == def.manifest.id ||
                                    (type == ButtonStudioType.DEFAULT && chosenSkins[targetKey] == null)

                            // In contextual selection mode, "applied" means it matches targetCurrentAssetId
                            val isAppliedToProfile = if (isContextual) {
                                def.manifest.id == (targetCurrentAssetId ?: "") ||
                                        (type == ButtonStudioType.DEFAULT && targetCurrentAssetId == null)
                            } else {
                                activeProfile?.positions?.get(targetKey)?.customComponentId == expectedCustomId
                            }

                            StudioGridCard(
                                def = def,
                                mode = currentMode,
                                isSelectedInBuilder = isControlActive && isSkinSelected,
                                isAppliedToActiveProfile = isAppliedToProfile,
                                onClick = { previewTarget = def },
                                onToggleSelectInBuilder = {
                                    if (isControlActive && isSkinSelected) {
                                        activeControls[targetKey] = false
                                        Toast.makeText(context, "Excluded $targetKey from layout", Toast.LENGTH_SHORT).show()
                                    } else {
                                        activeControls[targetKey] = true
                                        chosenSkins[targetKey] = if (type == ButtonStudioType.DEFAULT) null else def.manifest.id
                                        Toast.makeText(context, "Selected ${def.manifest.name} for $targetKey", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Live Sandbox Modal
    if (previewTarget != null) {
        val target = previewTarget!!
        val targetKey = target.manifest.defaultControl.uppercase()
        val type = resolveButtonSourceType(target)
        val expectedCustomId = if (type == ButtonStudioType.DEFAULT) null else target.manifest.id
        val isApplied = activeProfile?.positions?.get(targetKey)?.customComponentId == expectedCustomId

        SandboxPreviewModal(
            componentDef = target,
            isAppliedToActiveProfile = isApplied,
            // In contextual Selection Mode, the primary action label changes to "Use This"
            applyButtonLabel = if (isContextual) "Use This" else "Apply to Profile",
            onDismiss = { previewTarget = null },
            onApplyToProfile = {
                if (isContextual) {
                    // --- Step 4 Core: Return result via NavigationViewModel, do NOT mutate layout ---
                    val assetId = if (type == ButtonStudioType.DEFAULT) null else target.manifest.id
                    if (assetId != null) {
                        navigationViewModel?.commitAssetSelection(assetId)
                    } else {
                        // Default asset: commit a sentinel that clears the custom id
                        navigationViewModel?.commitAssetSelection("")
                    }
                    previewTarget = null
                    navController.popBackStack()
                } else {
                    // Manage Mode: direct mutation (existing behavior)
                    applyButtonSkinToProfile(target, layoutManager, context)
                    activeProfile = layoutManager?.getActiveProfile()
                }
            },
            onAddToHud = {
                previewTarget = null
                // In contextual mode onAddToHud is mapped to onApplyToProfile above.
                // In manage mode, this is the explicit "Add to HUD" quick-launch:
                if (!isContextual) {
                    applyButtonToHud(target, layoutManager, navController, context)
                }
            },
            onExportJson = {
                val json = registry.exportToJson(target.manifest.id)
                if (json != null) {
                    clipboard.nativeClipboard.setPrimaryClip(android.content.ClipData.newPlainText("NXP JSON", json))
                    Toast.makeText(context, "JSON copied to clipboard!", Toast.LENGTH_SHORT).show()
                }
            },
            onDelete = {
                val deleted = registry.deleteComponent(target.manifest.id)
                if (deleted) {
                    previewTarget = null
                    Toast.makeText(context, "Deleted ${target.manifest.name}", Toast.LENGTH_SHORT).show()
                }
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
 * Used in Manage Mode only.
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
        val active = layoutManager.getActiveProfile()
        if (active.isDefault) {
            // Preset protection: copy-on-write so default presets are never mutated
            val customCopy = layoutManager.createCustomProfile(
                name = "${active.name} Custom",
                baseProfile = active
            )
            layoutManager.setActiveProfile(customCopy.name)
            Toast.makeText(
                context,
                "Created '${customCopy.name}' (preset protected)",
                Toast.LENGTH_SHORT
            ).show()
        }
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
 * Places the selected button with its skin onto the active profile and opens HudEditorScreen.
 * Used only in Manage Mode (not in contextual Selection Mode).
 * Note: pendingSelectedKey has been removed; the HUD Editor now receives the control key
 * via ScreenKey.Editor(controlKey = ...) instead.
 */
private fun applyButtonToHud(
    def: NxpComponentDef,
    layoutManager: LayoutManager?,
    navController: AppNavigator,
    context: Context
) {
    applyButtonSkinToProfile(def, layoutManager, context)
    val controlKey = def.manifest.defaultControl.uppercase()
    // Navigate to HUD Editor and pre-select the control that was just skinned
    navController.navigate(ScreenKey.Editor(controlKey = controlKey))
}

