package com.sanket.tools.nexpad.ui.studio

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.sanket.tools.nexpad.runtime.engine.NxpComposeInterpreter
import com.sanket.tools.nexpad.runtime.model.NexPadControl
import com.sanket.tools.nexpad.runtime.model.NxpComponentDef
import com.sanket.tools.nexpad.runtime.model.SandboxInputTarget
import com.sanket.tools.nexpad.runtime.registry.ComponentRegistry
import com.sanket.tools.nexpad.ui.components.effects.CyberGrid
import com.sanket.tools.nexpad.ui.components.effects.ScanLine
import com.sanket.tools.nexpad.ui.theme.NeonPalette

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState

data class ControllerButtonCategory(
    val id: String,
    val label: String,
    val matchingControls: Set<String>,
    val matchingCategories: Set<String> = emptySet()
)

val CONTROLLER_CATEGORIES = listOf(
    ControllerButtonCategory("ALL", "All Controls", emptySet(), emptySet()),
    ControllerButtonCategory("ABXY", "ABXY Buttons", setOf("A", "B", "X", "Y"), setOf("BUTTON")),
    ControllerButtonCategory("BUMPERS", "Bumpers (LB / RB)", setOf("LB", "RB"), setOf("BUMPER")),
    ControllerButtonCategory("TRIGGERS", "Triggers (LT / RT)", setOf("LT", "RT"), setOf("TRIGGER")),
    ControllerButtonCategory("JOYSTICKS", "Joysticks (LS / RS)", setOf("LS", "RS"), setOf("JOYSTICK")),
    ControllerButtonCategory("DPAD", "D-Pad", setOf("DPAD"), setOf("DPAD")),
    ControllerButtonCategory("HOME", "Home / Guide", setOf("XBOX", "HOME", "GUIDE"), setOf("HOME")),
    ControllerButtonCategory("SYSTEM", "System (View / Menu / Share)", setOf("VIEW", "MENU", "SHARE", "BACK", "START"), setOf("SYSTEM")),
    ControllerButtonCategory("MACROS", "Macros (M1 - M4)", setOf("M1", "M2", "M3", "M4", "TURBO", "PROFILE"), setOf("MACRO"))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ButtonStudioScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val registry = remember { ComponentRegistry.getInstance(context) }
    val components by registry.installedComponents.collectAsState()

    var selectedFilter by remember { mutableStateOf("ALL") }
    var previewTarget by remember { mutableStateOf<NxpComponentDef?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }

    val filteredComponents = remember(components, selectedFilter) {
        if (selectedFilter == "ALL") {
            components
        } else {
            val cat = CONTROLLER_CATEGORIES.find { it.id == selectedFilter }
            if (cat != null) {
                components.filter { def ->
                    val control = def.manifest.defaultControl.uppercase()
                    val category = def.manifest.category.uppercase()
                    cat.matchingControls.contains(control) || cat.matchingCategories.contains(category)
                }
            } else {
                components.filter { it.manifest.category.equals(selectedFilter, ignoreCase = true) }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Button Studio",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = NeonPalette.Cyan
                        )
                    )
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
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Import AI", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Controller Button Category Slider
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CONTROLLER_CATEGORIES.forEach { category ->
                        val isSelected = selectedFilter == category.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFilter = category.id },
                            label = {
                                Text(
                                    category.label,
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

                // Empty State if no designs found for category
                if (filteredComponents.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Rounded.DesignServices,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                "No custom designs installed for this category yet.",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                            OutlinedButton(
                                onClick = { showImportDialog = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonPalette.Cyan),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NeonPalette.Cyan.copy(alpha = 0.5f))
                            ) {
                                Text("Import AI Component")
                            }
                        }
                    }
                }

                // Components List
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredComponents, key = { it.manifest.id }) { def ->
                        ComponentCard(
                            def = def,
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

    // Live Sandbox Modal
    if (previewTarget != null) {
        SandboxPreviewModal(
            componentDef = previewTarget!!,
            onDismiss = { previewTarget = null },
            onAddToHud = {
                previewTarget = null
                navController.navigate("editor")
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

@Composable
private fun ComponentCard(
    def: NxpComponentDef,
    onTest: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit
) {
    val isBuiltIn = def.manifest.id.startsWith("builtin.")
    val dummyTarget = remember { SandboxInputTarget() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1322))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live Mini-Preview Box
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF040810))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                val control = if (def.manifest.category.equals("JOYSTICK", ignoreCase = true)) {
                    NexPadControl.Stick(isLeft = true)
                } else {
                    NexPadControl.Button(def.manifest.defaultControl)
                }
                NxpComposeInterpreter(
                    definition = def,
                    assignedControl = control,
                    isConnected = false,
                    inputTarget = dummyTarget,
                    modifier = Modifier.padding(4.dp)
                )
            }

            // Info Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = def.manifest.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Text(
                    text = "${def.manifest.category} • Target: ${def.manifest.defaultControl}",
                    style = MaterialTheme.typography.bodySmall.copy(color = NeonPalette.Cyan)
                )
                Text(
                    text = if (isBuiltIn) "System Core Preset" else "by ${def.manifest.author}",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                )
            }

            // Action Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onTest) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = "Test in Sandbox", tint = NeonPalette.Cyan)
                }
                IconButton(onClick = onExport) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy JSON", tint = Color.LightGray)
                }
                if (!isBuiltIn) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFFF5252))
                    }
                }
            }
        }
    }
}
