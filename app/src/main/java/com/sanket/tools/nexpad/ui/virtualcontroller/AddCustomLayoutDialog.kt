package com.sanket.tools.nexpad.ui.virtualcontroller

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.category.CategoryManager
import com.sanket.tools.nexpad.category.ControlKey
import com.sanket.tools.nexpad.model.LayoutProfile
import com.sanket.tools.nexpad.ui.theme.NeonPalette

/**
 * Multi-step dialog to create custom layouts with base template selector,
 * discrete/composite D-Pad mutual exclusivity filters, and Button Studio launcher.
 */
@Composable
fun AddCustomLayoutDialog(
    defaults: List<LayoutProfile>,
    onDismiss: () -> Unit,
    onCreate: (name: String, baseTemplate: LayoutProfile, selectedButtons: Set<String>, openStudio: Boolean) -> Unit,
    onDesignInStudio: (name: String) -> Unit
) {
    var layoutName by remember { mutableStateOf("") }
    var selectedTemplateIndex by remember { mutableIntStateOf(0) }
    var openInStudio by remember { mutableStateOf(false) }
    val currentTemplate = defaults.getOrElse(selectedTemplateIndex) { defaults.first() }

    val allTemplateKeys = remember(currentTemplate) { currentTemplate.canonicalPositions().keys.toList() }
    val selectedButtons = remember(currentTemplate) {
        mutableStateMapOf<String, Boolean>().apply {
            allTemplateKeys.forEach { put(it, true) }
        }
    }

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
                                    .clickable {
                                        allTemplateKeys.forEach { selectedButtons[it] = true }
                                        if (selectedButtons.containsKey(ControlKey.DPAD.key) && selectedButtons[ControlKey.DPAD.key] == true) {
                                            ControlKey.DISCRETE_DPAD_KEYS.forEach { discrete ->
                                                selectedButtons[discrete.key] = false
                                            }
                                        }
                                    }
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
                    val toggleButton = { key: String, newVal: Boolean ->
                        selectedButtons[key] = newVal
                        if (newVal) {
                            val ctrl = ControlKey.fromIdentifier(key)
                            if (ctrl?.isDpadComposite == true) {
                                ControlKey.DISCRETE_DPAD_KEYS.forEach { discrete ->
                                    selectedButtons[discrete.key] = false
                                }
                            } else if (ctrl?.isDpadDiscrete == true) {
                                selectedButtons[ControlKey.DPAD.key] = false
                            }
                        }
                    }

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
                                            .clickable { toggleButton(key, !isChecked) }
                                            .padding(vertical = 4.dp, horizontal = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = { toggleButton(key, it) },
                                            colors = CheckboxDefaults.colors(checkedColor = NeonPalette.Cyan)
                                        )
                                        val ctrlSpec = CategoryManager.getControl(key)
                                        val displayLabel = if (ctrlSpec != null && ctrlSpec.emoji.isNotBlank()) "${ctrlSpec.emoji} $key" else key
                                        Text(displayLabel, fontSize = 13.sp, color = Color.White)
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
