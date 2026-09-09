package com.sanket.tools.nexpad.ui.studio

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.sanket.tools.nexpad.model.Position
import com.sanket.tools.nexpad.model.defaultPositions
import com.sanket.tools.nexpad.runtime.engine.NxpComposeInterpreter
import com.sanket.tools.nexpad.runtime.model.NexPadControl
import com.sanket.tools.nexpad.runtime.model.NxpComponentDef
import com.sanket.tools.nexpad.runtime.model.SandboxInputTarget
import com.sanket.tools.nexpad.runtime.registry.ComponentRegistry
import com.sanket.tools.nexpad.ui.components.controller.ControllerElementRenderer
import com.sanket.tools.nexpad.ui.components.effects.CyberGrid
import com.sanket.tools.nexpad.ui.components.effects.ScanLine
import com.sanket.tools.nexpad.ui.theme.NeonPalette
import com.sanket.tools.nexpad.utils.LayoutManager
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel

/**
 * Categorization for Button Studio buttons:
 * - DEFAULT: Native realistic controller component from ui/components/controller/
 * - SVG: Cyber / Vector / Geometric polygon skin
 * - PLUGIN: Dynamic Compose / AI imported JSON plugin
 */
enum class ButtonStudioType(val label: String, val badgeColor: Color, val badgeBg: Color) {
    DEFAULT("DEFAULT", Color(0xFF00E5FF), Color(0x2200E5FF)),
    SVG("SVG / VECTOR", Color(0xFF39FF14), Color(0x2239FF14)),
    PLUGIN("PLUGIN / NXP", Color(0xFFB400FF), Color(0x22B400FF))
}

fun getButtonStudioType(def: NxpComponentDef): ButtonStudioType {
    val id = def.manifest.id
    return when {
        id.startsWith("builtin.default_") -> ButtonStudioType.DEFAULT
        id.startsWith("builtin.") -> ButtonStudioType.SVG
        else -> ButtonStudioType.PLUGIN
    }
}

data class StudioSubFilter(
    val id: String,
    val label: String,
    val targetKey: String?
)

data class StudioCategory(
    val id: String,
    val title: String,
    val keys: Set<String>,
    val subFilters: List<StudioSubFilter>
)

val STUDIO_CATEGORIES = listOf(
    StudioCategory(
        id = "ABXY",
        title = "ABXY Buttons",
        keys = setOf("A", "B", "X", "Y"),
        subFilters = listOf(
            StudioSubFilter("ALL", "All ABXY", null),
            StudioSubFilter("A", "A (Green)", "A"),
            StudioSubFilter("B", "B (Red)", "B"),
            StudioSubFilter("X", "X (Blue)", "X"),
            StudioSubFilter("Y", "Y (Yellow)", "Y")
        )
    ),
    StudioCategory(
        id = "DPAD",
        title = "D-Pad",
        keys = setOf("DPAD"),
        subFilters = listOf(
            StudioSubFilter("ALL", "Directional Cross", "DPAD")
        )
    ),
    StudioCategory(
        id = "STICKS",
        title = "Thumbsticks",
        keys = setOf("LS", "RS"),
        subFilters = listOf(
            StudioSubFilter("ALL", "Both Sticks", null),
            StudioSubFilter("LS", "Left Stick (LS)", "LS"),
            StudioSubFilter("RS", "Right Stick (RS)", "RS")
        )
    ),
    StudioCategory(
        id = "TRIGGERS",
        title = "Triggers",
        keys = setOf("LT", "RT"),
        subFilters = listOf(
            StudioSubFilter("ALL", "Both Triggers", null),
            StudioSubFilter("LT", "Left Trigger (LT)", "LT"),
            StudioSubFilter("RT", "Right Trigger (RT)", "RT")
        )
    ),
    StudioCategory(
        id = "BUMPERS",
        title = "Bumpers",
        keys = setOf("LB", "RB"),
        subFilters = listOf(
            StudioSubFilter("ALL", "Both Bumpers", null),
            StudioSubFilter("LB", "Left Bumper (LB)", "LB"),
            StudioSubFilter("RB", "Right Bumper (RB)", "RB")
        )
    ),
    StudioCategory(
        id = "HOME",
        title = "Home (Xbox)",
        keys = setOf("XBOX", "HOME", "GUIDE"),
        subFilters = listOf(
            StudioSubFilter("ALL", "Xbox Guide", "XBOX")
        )
    ),
    StudioCategory(
        id = "SYSTEM",
        title = "System",
        keys = setOf("VIEW", "MENU", "SHARE", "BACK", "START"),
        subFilters = listOf(
            StudioSubFilter("ALL", "All System", null),
            StudioSubFilter("VIEW", "View / Back", "VIEW"),
            StudioSubFilter("MENU", "Menu / Pause", "MENU"),
            StudioSubFilter("SHARE", "Share / Capture", "SHARE")
        )
    ),
    StudioCategory(
        id = "MACROS",
        title = "Macro Paddles",
        keys = setOf("M1", "M2", "M3", "M4", "PROFILE", "TURBO"),
        subFilters = listOf(
            StudioSubFilter("ALL", "All Macros", null),
            StudioSubFilter("M1", "Paddle M1", "M1"),
            StudioSubFilter("M2", "Paddle M2", "M2"),
            StudioSubFilter("M3", "Paddle M3", "M3"),
            StudioSubFilter("M4", "Paddle M4", "M4")
        )
    ),
    StudioCategory(
        id = "ALL",
        title = "All Controls",
        keys = emptySet(),
        subFilters = emptyList()
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ButtonStudioScreen(
    navController: NavController,
    layoutManager: LayoutManager? = null
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val registry = remember { ComponentRegistry.getInstance(context) }
    val components by registry.installedComponents.collectAsState()
    val dummyViewModel = viewModel<GamepadViewModel>()

    // Default selected category is ABXY buttons
    var selectedCategory by remember { mutableStateOf("ABXY") }
    var selectedSubFilter by remember { mutableStateOf("ALL") }

    var previewTarget by remember { mutableStateOf<NxpComponentDef?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }

    // Reset subfilter when category changes
    LaunchedEffect(selectedCategory) {
        selectedSubFilter = "ALL"
    }

    val activeCategory = remember(selectedCategory) {
        STUDIO_CATEGORIES.find { it.id == selectedCategory } ?: STUDIO_CATEGORIES.first()
    }

    val filteredComponents = remember(components, selectedCategory, selectedSubFilter) {
        val targetKey = if (selectedSubFilter != "ALL") selectedSubFilter else null

        if (selectedCategory == "ALL") {
            components
        } else {
            components.filter { def ->
                val control = def.manifest.defaultControl.uppercase()
                if (targetKey != null) {
                    control == targetKey
                } else {
                    activeCategory.keys.contains(control)
                }
            }
        }
    }

    val activeProfileName = remember(layoutManager) {
        layoutManager?.getActiveProfile()?.name ?: "Active Layout"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Button Studio",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = NeonPalette.Cyan
                            )
                        )
                        Text(
                            "Select & customize buttons • Place directly on HUD",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
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
                    Button(
                        onClick = { showImportDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPalette.Cyan),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Import AI", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            CyberGrid(modifier = Modifier.matchParentSize())
            ScanLine(modifier = Modifier.matchParentSize())

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Controller Button Name Slider (Horizontal Scroll, ABXY default)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    STUDIO_CATEGORIES.forEach { category ->
                        val isSelected = selectedCategory == category.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = category.id },
                            label = {
                                Text(
                                    category.title,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonPalette.Cyan.copy(alpha = 0.2f),
                                selectedLabelColor = NeonPalette.Cyan
                            )
                        )
                    }
                }

                // 2. Sub-filter chips row (e.g. A, B, X, Y buttons)
                if (activeCategory.subFilters.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        activeCategory.subFilters.forEach { sub ->
                            val isSubSelected = selectedSubFilter == sub.id
                            AssistChip(
                                onClick = { selectedSubFilter = sub.id },
                                label = {
                                    Text(
                                        sub.label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSubSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (isSubSelected) NeonPalette.Cyan.copy(alpha = 0.25f) else Color(0xFF141923),
                                    labelColor = if (isSubSelected) NeonPalette.Cyan else Color.LightGray
                                ),
                                border = AssistChipDefaults.assistChipBorder(
                                    borderColor = if (isSubSelected) NeonPalette.Cyan else Color.White.copy(alpha = 0.15f),
                                    enabled = true
                                )
                            )
                        }
                    }
                }

                // 3. Status Bar showing active layout target
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0F172A))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Target Layout: $activeProfileName",
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "${filteredComponents.size} available styles",
                        color = NeonPalette.Cyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // 4. Empty State if no buttons match
                if (filteredComponents.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Rounded.DesignServices, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                            Text("No buttons available for this category yet.", color = Color.Gray, fontSize = 14.sp)
                            OutlinedButton(
                                onClick = { showImportDialog = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonPalette.Cyan)
                            ) {
                                Text("Import AI Component")
                            }
                        }
                    }
                } else {
                    // 5. Grid View: 2 columns showing button previews
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(filteredComponents, key = { it.manifest.id }) { def ->
                            StudioGridCard(
                                def = def,
                                dummyViewModel = dummyViewModel,
                                onUseInHud = {
                                    applyButtonToHud(def, layoutManager, navController, context)
                                },
                                onTest = { previewTarget = def },
                                onExport = {
                                    val json = registry.exportToJson(def.manifest.id)
                                    if (json != null) {
                                        clipboardManager.setText(AnnotatedString(json))
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
 * Grid Card displaying button preview, DEFAULT/SVG/PLUGIN badge, and Use in HUD button
 */
@Composable
private fun StudioGridCard(
    def: NxpComponentDef,
    dummyViewModel: GamepadViewModel,
    onUseInHud: () -> Unit,
    onTest: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit
) {
    val isBuiltIn = def.manifest.id.startsWith("builtin.")
    val type = remember(def.manifest.id) { getButtonStudioType(def) }
    val dummyTarget = remember { SandboxInputTarget() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1322))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Row: Title + Type Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = def.manifest.name,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 12.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Type Badge (DEFAULT, SVG, PLUGIN)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(type.badgeBg)
                        .border(1.dp, type.badgeColor.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = type.label,
                        color = type.badgeColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            // Interactive Live Preview Stage
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF040810))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                val controlKey = def.manifest.defaultControl.uppercase()
                val maxDim = maxOf(def.size.widthDp, def.size.heightDp).toFloat()
                val previewScale = if (maxDim > 64f) 64f / maxDim else 1.0f

                Box(
                    modifier = Modifier.graphicsLayer {
                        scaleX = previewScale
                        scaleY = previewScale
                    },
                    contentAlignment = Alignment.Center
                ) {
                    if (type == ButtonStudioType.DEFAULT) {
                        // Render authentic native controller button from ui/components/controller
                        ControllerElementRenderer(
                            key = controlKey,
                            isConnected = false,
                            isRgbEnabled = true,
                            viewModel = dummyViewModel,
                            onVibrate = {},
                            customComponentId = null
                        )
                    } else {
                        // Render NXP / SVG / Plugin definition
                        val control = when {
                            def.manifest.category.equals("JOYSTICK", ignoreCase = true) ->
                                NexPadControl.Stick(isLeft = !controlKey.contains("R"))
                            def.manifest.category.equals("TRIGGER", ignoreCase = true) ->
                                NexPadControl.Trigger(key = controlKey)
                            else ->
                                NexPadControl.Button(controlKey)
                        }
                        NxpComposeInterpreter(
                            definition = def,
                            assignedControl = control,
                            isConnected = false,
                            inputTarget = dummyTarget
                        )
                    }
                }
            }

            // Target Control Information
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Target: ${def.manifest.defaultControl}",
                    color = NeonPalette.Cyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = if (isBuiltIn) "Core Preset" else "by ${def.manifest.author}",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Action: USE IN HUD (Places button on layout and opens HUD Editor)
            Button(
                onClick = onUseInHud,
                colors = ButtonDefaults.buttonColors(containerColor = NeonPalette.Cyan),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
            ) {
                Icon(
                    Icons.Rounded.DashboardCustomize,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "Use in HUD",
                    color = Color.Black,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Auxiliary Actions (Sandbox, Copy JSON, Delete)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(
                        onClick = onTest,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Rounded.PlayArrow,
                            contentDescription = "Test in Sandbox",
                            tint = NeonPalette.Cyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onExport,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Rounded.ContentCopy,
                            contentDescription = "Copy JSON",
                            tint = Color.LightGray,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                if (!isBuiltIn) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Rounded.DeleteOutline,
                            contentDescription = "Delete",
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
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
    val type = getButtonStudioType(def)
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
