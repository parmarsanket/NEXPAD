package com.sanket.tools.nexpad.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.sanket.tools.nexpad.model.LayoutProfile
import com.sanket.tools.nexpad.model.getDefaultLayoutProfiles
import com.sanket.tools.nexpad.ui.components.effects.CyberGrid
import com.sanket.tools.nexpad.ui.components.effects.ScanLine
import com.sanket.tools.nexpad.ui.theme.NeonPalette
import com.sanket.tools.nexpad.utils.LayoutManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VirtualControllerScreen(
    navController: NavController,
    layoutManager: LayoutManager
) {
    val context = LocalContext.current
    val profiles by layoutManager.profilesFlow.collectAsState()
    val activeProfile = layoutManager.getActiveProfile()

    var showAddDialog by remember { mutableStateOf(false) }
    var profileToDuplicate by remember { mutableStateOf<LayoutProfile?>(null) }
    var profileToDelete by remember { mutableStateOf<LayoutProfile?>(null) }
    var profileToReset by remember { mutableStateOf<LayoutProfile?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Virtual Controller",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = NeonPalette.Cyan
                            )
                        )
                        Text(
                            "Layouts & Button Management",
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
                    OutlinedButton(
                        onClick = { navController.navigate("button_studio?mode=select") },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonPalette.Purple),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonPalette.Purple.copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.padding(end = 6.dp).height(36.dp)
                    ) {
                        Icon(Icons.Rounded.Palette, contentDescription = null, tint = NeonPalette.Purple, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Studio Builder", color = NeonPalette.Purple, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPalette.Cyan),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.padding(end = 8.dp).height(36.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Custom", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Info banner
                item {
                    LayoutHubBanner()
                }

                // Layout Profiles List
                items(profiles, key = { it.name }) { profile ->
                    val isActive = profile.name == activeProfile.name
                    LayoutProfileCard(
                        profile = profile,
                        isActive = isActive,
                        onSetActive = {
                            layoutManager.setActiveProfile(profile.name)
                            Toast.makeText(context, "Activated ${profile.name}", Toast.LENGTH_SHORT).show()
                        },
                        onPlay = {
                            layoutManager.setActiveProfile(profile.name)
                            navController.navigate("gamepad")
                        },
                        onEditHud = {
                            layoutManager.setActiveProfile(profile.name)
                            navController.navigate("editor")
                        },
                        onOpenStudio = {
                            layoutManager.setActiveProfile(profile.name)
                            navController.navigate("button_studio")
                        },
                        onDuplicate = {
                            profileToDuplicate = profile
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
    if (profileToDuplicate != null) {
        var duplicateName by remember { mutableStateOf("${profileToDuplicate!!.name} Copy") }
        AlertDialog(
            onDismissRequest = { profileToDuplicate = null },
            title = { Text("Duplicate Layout", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Enter a name for the new custom layout:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = duplicateName,
                        onValueChange = { duplicateName = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonPalette.Cyan,
                            unfocusedBorderColor = Color.Gray
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val base = profileToDuplicate!!
                        layoutManager.createCustomProfile(
                            name = duplicateName.trim().ifEmpty { "${base.name} Copy" },
                            baseProfile = base
                        )
                        profileToDuplicate = null
                        Toast.makeText(context, "Duplicated to '$duplicateName'", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPalette.Cyan)
                ) {
                    Text("Duplicate", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { profileToDuplicate = null }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Reset Default Layout Dialog
    if (profileToReset != null) {
        AlertDialog(
            onDismissRequest = { profileToReset = null },
            title = { Text("Reset to Factory Default", color = Color.White) },
            text = {
                Text(
                    "Are you sure you want to reset '${profileToReset!!.name}' to its original factory positions? Any HUD edits made to this profile will be restored.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        layoutManager.resetDefaultProfile(profileToReset!!.name)
                        Toast.makeText(context, "Reset '${profileToReset!!.name}' to factory default", Toast.LENGTH_SHORT).show()
                        profileToReset = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPalette.Purple)
                ) {
                    Text("Reset Default", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { profileToReset = null }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Delete Confirmation Dialog
    if (profileToDelete != null) {
        AlertDialog(
            onDismissRequest = { profileToDelete = null },
            title = { Text("Delete Custom Layout", color = Color.White) },
            text = {
                Text(
                    "Are you sure you want to permanently delete '${profileToDelete!!.name}'?",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val deleted = layoutManager.deleteProfile(profileToDelete!!.name)
                        if (deleted) {
                            Toast.makeText(context, "Deleted '${profileToDelete!!.name}'", Toast.LENGTH_SHORT).show()
                        }
                        profileToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.onError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { profileToDelete = null }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
private fun LayoutHubBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E293B)
                    )
                )
            )
            .border(1.dp, NeonPalette.Cyan.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Rounded.Shield,
                    contentDescription = null,
                    tint = NeonPalette.Cyan,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    "CONTROLLER ARCHITECTURE",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = NeonPalette.Cyan,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp
                    )
                )
            }
            Text(
                "NEXPAD features 5 factory default layouts tailored for Elite, FPS, MOBA, Racing, and Arcade playstyles. Default layouts are protected against deletion and can be customized in the HUD Editor or cloned into custom layouts.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            )
        }
    }
}

@Composable
private fun LayoutProfileCard(
    profile: LayoutProfile,
    isActive: Boolean,
    onSetActive: () -> Unit,
    onPlay: () -> Unit,
    onEditHud: () -> Unit,
    onOpenStudio: () -> Unit,
    onDuplicate: () -> Unit,
    onReset: () -> Unit,
    onDelete: () -> Unit
) {
    val borderColor = if (isActive) NeonPalette.Cyan else Color.White.copy(alpha = 0.12f)
    val borderWidth = if (isActive) 2.dp else 1.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.90f))
            .border(borderWidth, borderColor, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header Row: Title + Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        profile.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    // Protection badge
                    if (profile.isDefault) {
                        Surface(
                            shape = CircleShape,
                            color = NeonPalette.Cyan.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonPalette.Cyan.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Lock,
                                    contentDescription = "Default layout locked",
                                    tint = NeonPalette.Cyan,
                                    modifier = Modifier.size(10.dp)
                                )
                                Text(
                                    "DEFAULT",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonPalette.Cyan
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFFB703).copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB703).copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Star,
                                    contentDescription = "Custom layout",
                                    tint = Color(0xFFFFB703),
                                    modifier = Modifier.size(10.dp)
                                )
                                Text(
                                    "CUSTOM",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFB703)
                                )
                            }
                        }
                    }
                }

                // Active badge
                if (isActive) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF00FF66).copy(alpha = 0.18f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00FF66))
                    ) {
                        Text(
                            "ACTIVE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF00FF66),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Description
            if (profile.description.isNotBlank()) {
                Text(
                    profile.description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            // Button Pills Preview Row
            val buttonKeys = profile.positions.keys.toList()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.White.copy(alpha = 0.06f)
                ) {
                    Text(
                        "${buttonKeys.size} Buttons",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                buttonKeys.take(12).forEach { key ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.White.copy(alpha = 0.04f)
                    ) {
                        Text(
                            key,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }
                }
                if (buttonKeys.size > 12) {
                    Text(
                        "+${buttonKeys.size - 12} more",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isActive) {
                    OutlinedButton(
                        onClick = onSetActive,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonPalette.Cyan),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonPalette.Cyan.copy(alpha = 0.7f)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Set Active", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Button(
                    onClick = onPlay,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPalette.Cyan),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Rounded.SportsEsports, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Play", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onEditHud,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Rounded.DashboardCustomize, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("HUD", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onOpenStudio,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonPalette.Purple),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonPalette.Purple.copy(alpha = 0.6f)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Rounded.Palette, contentDescription = null, tint = NeonPalette.Purple, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Studio", fontSize = 12.sp, color = NeonPalette.Purple, fontWeight = FontWeight.SemiBold)
                }

                IconButton(
                    onClick = onDuplicate,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Rounded.ContentCopy,
                        contentDescription = "Duplicate layout",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (profile.isDefault) {
                    IconButton(
                        onClick = onReset,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Rounded.RestartAlt,
                            contentDescription = "Reset factory default",
                            tint = NeonPalette.Purple.copy(alpha = 0.85f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Delete button
                if (profile.isDefault) {
                    // Protected - Disabled with Lock icon
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White.copy(alpha = 0.25f))
                    ) {
                        Icon(
                            Icons.Rounded.Lock,
                            contentDescription = "Protected default layout cannot be deleted",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Delete,
                            contentDescription = "Delete custom layout",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddCustomLayoutDialog(
    defaults: List<LayoutProfile>,
    onDismiss: () -> Unit,
    onCreate: (name: String, baseTemplate: LayoutProfile, selectedButtons: Set<String>, openStudio: Boolean) -> Unit,
    onDesignInStudio: (name: String) -> Unit
) {
    var layoutName by remember { mutableStateOf("") }
    var selectedTemplateIndex by remember { mutableIntStateOf(0) }
    var openInStudio by remember { mutableStateOf(false) }
    val currentTemplate = defaults.getOrElse(selectedTemplateIndex) { defaults.first() }

    val allTemplateKeys = remember(currentTemplate) { currentTemplate.positions.keys.toList() }
    val selectedButtons = remember(currentTemplate) { mutableStateMapOf<String, Boolean>().apply {
        allTemplateKeys.forEach { put(it, true) }
    }}

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Create Custom Layout",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = NeonPalette.Cyan)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Name Field
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Layout Name", style = MaterialTheme.typography.labelMedium, color = Color.White)
                    OutlinedTextField(
                        value = layoutName,
                        onValueChange = { layoutName = it },
                        placeholder = { Text("e.g. My Competitive COD", color = Color.Gray) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonPalette.Cyan,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        )
                    )
                }

                // Interactive Studio Builder Launch Option
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            val finalName = layoutName.trim().ifEmpty { "Custom Layout ${System.currentTimeMillis() % 1000}" }
                            onDesignInStudio(finalName)
                        },
                    shape = RoundedCornerShape(12.dp),
                    color = NeonPalette.Purple.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonPalette.Purple.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Rounded.Palette, contentDescription = null, tint = NeonPalette.Purple, modifier = Modifier.size(22.dp))
                            Column {
                                Text("Interactive Studio Builder 🎨", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                Text("Pick button skins with Vertical Navigation Rail", fontSize = 10.sp, color = Color.LightGray)
                            }
                        }
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = NeonPalette.Purple, modifier = Modifier.size(16.dp))
                    }
                }

                // Base Template Picker
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Base Template", style = MaterialTheme.typography.labelMedium, color = Color.White)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        defaults.forEachIndexed { index, def ->
                            val isSelected = selectedTemplateIndex == index
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedTemplateIndex = index },
                                label = { Text(def.name, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonPalette.Cyan.copy(alpha = 0.2f),
                                    selectedLabelColor = NeonPalette.Cyan
                                )
                            )
                        }
                    }
                }

                // Button Selection Checklist
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Select Active Buttons", style = MaterialTheme.typography.labelMedium, color = Color.White)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "All",
                                fontSize = 11.sp,
                                color = NeonPalette.Cyan,
                                modifier = Modifier
                                    .clickable { allTemplateKeys.forEach { selectedButtons[it] = true } }
                                    .padding(4.dp)
                            )
                            Text(
                                "Clear",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                modifier = Modifier
                                    .clickable { allTemplateKeys.forEach { selectedButtons[it] = false } }
                                    .padding(4.dp)
                            )
                        }
                    }

                    // Button Checkboxes in Grid
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.04f))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        allTemplateKeys.chunked(2).forEach { pair ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                pair.forEach { key ->
                                    val isChecked = selectedButtons[key] ?: true
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { selectedButtons[key] = !isChecked }
                                            .padding(vertical = 4.dp, horizontal = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = { selectedButtons[key] = it },
                                            colors = CheckboxDefaults.colors(checkedColor = NeonPalette.Cyan)
                                        )
                                        Text(key, fontSize = 13.sp, color = Color.White)
                                    }
                                }
                                if (pair.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                // Option to customize in Button Studio
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { openInStudio = !openInStudio }
                        .padding(vertical = 4.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = openInStudio,
                        onCheckedChange = { openInStudio = it },
                        colors = CheckboxDefaults.colors(checkedColor = NeonPalette.Purple)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Open Button Studio to customize skins after creation",
                        fontSize = 12.sp,
                        color = Color.LightGray
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalName = layoutName.trim().ifEmpty { "Custom Layout ${System.currentTimeMillis() % 1000}" }
                    val activeKeys = selectedButtons.filterValues { it }.keys.toSet()
                    onCreate(finalName, currentTemplate, activeKeys, openInStudio)
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonPalette.Cyan)
            ) {
                Text("Create Layout", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

