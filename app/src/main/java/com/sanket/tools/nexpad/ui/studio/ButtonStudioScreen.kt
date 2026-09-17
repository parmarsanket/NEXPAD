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
    initialMode: ButtonStudioMode = ButtonStudioMode.VIEWER,
    targetProfileName: String = "",
    /** The GamepadControl.key being edited contextually (e.g. "RT"). Null = Viewer Mode. */
    targetControlKey: String? = null,
    /** The asset ID currently applied to targetControlKey. Used for "✓ Current" badge. */
    targetCurrentAssetId: String? = null
) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val registry = remember { ComponentRegistry.getInstance(context) }
    val components by registry.installedComponents.collectAsState()

    // Current Studio Mode (VIEWER vs EDITOR)
    var currentMode by remember { mutableStateOf(initialMode) }

    // Whether we are in a contextual selection session (launched from HUD Editor)
    val isContextual = initialMode == ButtonStudioMode.EDITOR && targetControlKey != null

    // Target/Active Profile State (refreshes on apply)
    var currentProfileName by remember(targetProfileName) { mutableStateOf(targetProfileName) }
    var activeProfile by remember(layoutManager, targetProfileName) {
        mutableStateOf(
            if (targetProfileName.isNotBlank() && layoutManager != null) {
                layoutManager.loadProfile(targetProfileName) ?: layoutManager.getActiveProfile()
            } else {
                layoutManager?.getActiveProfile()
            }
        )
    }
    var contextualSelectedAssetId by remember(targetCurrentAssetId) { mutableStateOf(targetCurrentAssetId) }

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

                            // Active Profile Badge — ONLY in EDITOR mode! In VIEWER mode, hidden.
                            if (currentMode == ButtonStudioMode.EDITOR) {
                                val profileTitle = currentProfileName.ifBlank { activeProfile?.name ?: "Default" }
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
                        }

                        if (currentMode == ButtonStudioMode.EDITOR) {
                            if (isContextual) {
                                val controlLabel = GamepadControl.fromKey(targetControlKey)?.displayName
                                    ?: targetControlKey
                                val currentLabel = when {
                                    contextualSelectedAssetId == null -> "Default"
                                    else -> contextualSelectedAssetId!!.substringAfterLast(".").replace("_", " ")
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
                                val profileTitle = currentProfileName.ifBlank { activeProfile?.name ?: "Default" }
                                Text(
                                    text = "Customizing layout: $profileTitle",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 10.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        } else {
                            Text(
                                text = "Button Catalog • Browse, test & add buttons",
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
                    // Contextual Done button
                    if (isContextual) {
                        IconButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = "Done",
                                tint = NeonPalette.Cyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Import Menu — ONLY in VIEWER mode! (In EDITOR mode, import icon is removed)
                    if (currentMode == ButtonStudioMode.VIEWER) {
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
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                )
            )
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
                        if (currentMode == ButtonStudioMode.EDITOR) {
                            cat.keys.count { key ->
                                activeProfile?.positions?.get(key)?.customComponentId != null
                            }
                        } else {
                            0
                        }
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

                            // In VIEWER mode, NEVER glow/highlight as applied.
                            // In EDITOR mode, glow if applied to that particular layout.
                            val isAppliedToProfile = if (currentMode == ButtonStudioMode.VIEWER) {
                                false
                            } else if (isContextual) {
                                val selectedId = contextualSelectedAssetId
                                if (type == ButtonStudioType.DEFAULT) {
                                    selectedId == null || selectedId.isBlank()
                                } else {
                                    def.manifest.id == selectedId
                                }
                            } else {
                                val currentCustomId = activeProfile?.positions?.get(targetKey)?.customComponentId
                                if (type == ButtonStudioType.DEFAULT) {
                                    currentCustomId == null
                                } else {
                                    currentCustomId == def.manifest.id
                                }
                            }

                            StudioGridCard(
                                def = def,
                                mode = currentMode,
                                isAppliedToActiveProfile = isAppliedToProfile,
                                onClick = {
                                    if (currentMode == ButtonStudioMode.VIEWER) {
                                        // VIEWER MODE: Open sandbox test & preview popup
                                        previewTarget = def
                                    } else if (isContextual) {
                                        // CONTEXTUAL EDITOR MODE: Select or deselect for targetControlKey
                                        if (isAppliedToProfile) {
                                            contextualSelectedAssetId = null
                                            navigationViewModel?.commitAssetSelection("")
                                            Toast.makeText(context, "Deselected skin for $targetKey", Toast.LENGTH_SHORT).show()
                                        } else {
                                            val newAssetId = if (type == ButtonStudioType.DEFAULT) null else def.manifest.id
                                            contextualSelectedAssetId = newAssetId
                                            navigationViewModel?.commitAssetSelection(newAssetId ?: "")
                                            Toast.makeText(context, "Selected ${def.manifest.name} for $targetKey", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        // EDITOR MODE: Direct select / deselect on layout without preview popup
                                        val effectiveName = currentProfileName.ifBlank { activeProfile?.name ?: "" }
                                        if (isAppliedToProfile) {
                                            // Already selected -> Deselect! If custom skin, revert to Default
                                            if (type != ButtonStudioType.DEFAULT) {
                                                val updated = removeCustomSkinFromProfile(targetKey, effectiveName, layoutManager, context)
                                                if (updated != null) {
                                                    activeProfile = updated
                                                    currentProfileName = updated.name
                                                    Toast.makeText(context, "Deselected ${def.manifest.name} (reverted to Default)", Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                Toast.makeText(context, "Default skin is active for $targetKey", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            // Unselected -> Select this skin!
                                            // The previous skin for targetKey automatically un-glows on recomposition
                                            val updated = applyButtonSkinToProfile(def, effectiveName, layoutManager, context)
                                            if (updated != null) {
                                                activeProfile = updated
                                                currentProfileName = updated.name
                                                Toast.makeText(context, "Selected ${def.manifest.name} for $targetKey", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Live Sandbox Modal — ONLY in VIEWER mode!
    if (currentMode == ButtonStudioMode.VIEWER && previewTarget != null) {
        val target = previewTarget!!
        SandboxPreviewModal(
            componentDef = target,
            isAppliedToActiveProfile = false,
            applyButtonLabel = null, // Viewer mode: test & preview only
            onDismiss = { previewTarget = null },
            onApplyToProfile = {},
            onAddToHud = {},
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
 * Applies button skin to a specific layout profile (or active profile).
 * Used in Editor Mode only. Returns updated LayoutProfile.
 */
private fun applyButtonSkinToProfile(
    def: NxpComponentDef,
    targetProfileName: String,
    layoutManager: LayoutManager?,
    context: Context
): com.sanket.tools.nexpad.model.LayoutProfile? {
    val targetKey = def.manifest.defaultControl.uppercase()
    val type = resolveButtonSourceType(def)
    val customId = if (type == ButtonStudioType.DEFAULT) null else def.manifest.id

    if (layoutManager == null) return null

    val target = (if (targetProfileName.isNotBlank()) layoutManager.loadProfile(targetProfileName) else null)
        ?: layoutManager.getActiveProfile()
    val effectiveProfile = if (target.isDefault) {
        // Preset protection: copy-on-write so default presets are never mutated
        val customCopy = layoutManager.createCustomProfile(
            name = "${target.name} Custom",
            baseProfile = target
        )
        layoutManager.setActiveProfile(customCopy.name)
        Toast.makeText(
            context,
            "Created '${customCopy.name}' (preset protected)",
            Toast.LENGTH_SHORT
        ).show()
        customCopy
    } else {
        target
    }
    val posMap = effectiveProfile.positions.toMutableMap()
    val currentPos = posMap[targetKey] ?: defaultPositions()[targetKey] ?: Position(0.5f, 0.5f)
    posMap[targetKey] = currentPos.copy(customComponentId = customId)
    val updated = effectiveProfile.copy(positions = posMap)
    layoutManager.saveProfile(updated, activate = true)
    return updated
}

/**
 * Removes custom skin from a control (reverting it to the default native skin).
 * Returns updated LayoutProfile.
 */
private fun removeCustomSkinFromProfile(
    targetKey: String,
    targetProfileName: String,
    layoutManager: LayoutManager?,
    context: Context
): com.sanket.tools.nexpad.model.LayoutProfile? {
    if (layoutManager == null) return null

    val target = (if (targetProfileName.isNotBlank()) layoutManager.loadProfile(targetProfileName) else null)
        ?: layoutManager.getActiveProfile()
    val effectiveProfile = if (target.isDefault) {
        val customCopy = layoutManager.createCustomProfile(
            name = "${target.name} Custom",
            baseProfile = target
        )
        layoutManager.setActiveProfile(customCopy.name)
        Toast.makeText(
            context,
            "Created '${customCopy.name}' (preset protected)",
            Toast.LENGTH_SHORT
        ).show()
        customCopy
    } else {
        target
    }
    val posMap = effectiveProfile.positions.toMutableMap()
    val currentPos = posMap[targetKey] ?: defaultPositions()[targetKey] ?: Position(0.5f, 0.5f)
    posMap[targetKey] = currentPos.copy(customComponentId = null)
    val updated = effectiveProfile.copy(positions = posMap)
    layoutManager.saveProfile(updated, activate = true)
    return updated
}

/**
 * Places the selected button with its skin onto the target profile and opens HudEditorScreen.
 */
private fun applyButtonToHud(
    def: NxpComponentDef,
    targetProfileName: String,
    layoutManager: LayoutManager?,
    navController: AppNavigator,
    context: Context
) {
    applyButtonSkinToProfile(def, targetProfileName, layoutManager, context)
    val controlKey = def.manifest.defaultControl.uppercase()
    navController.navigate(ScreenKey.Editor(profileName = targetProfileName.ifBlank { null }, controlKey = controlKey))
}

