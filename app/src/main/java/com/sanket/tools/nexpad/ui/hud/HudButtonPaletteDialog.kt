package com.sanket.tools.nexpad.ui.hud

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.category.CategoryManager
import com.sanket.tools.nexpad.category.CategoryType
import com.sanket.tools.nexpad.category.ControlKey
import com.sanket.tools.nexpad.model.HudElement
import com.sanket.tools.nexpad.ui.theme.NeonPalette

/**
 * Dialog enabling/disabling any of the standard controller buttons on this HUD layout.
 */
@Composable
fun HudButtonPaletteDialog(
    currentElements: Map<String, HudElement>,
    onToggleControl: (String) -> Unit,
    onRestoreAll: () -> Unit,
    onStandardOnly: () -> Unit,
    onClearAll: () -> Unit,
    onOpenStudio: () -> Unit,
    onDismiss: () -> Unit
) {
    val categories = remember { CategoryManager.getAllCategories() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Controller Buttons Palette",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = NeonPalette.Cyan
                )
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Quick preset buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onRestoreAll,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Add All", fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = onStandardOnly,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Standard (14)", fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = onClearAll,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Clear All", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                // Grouped by Category via CategoryManager
                categories.forEach { category ->
                    val controlList = category.controls
                    if (controlList.isEmpty()) return@forEach
                    val catHeader = "${category.title} (${controlList.size})"

                    Text(
                        catHeader,
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = NeonPalette.Cyan,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    if (category.type == CategoryType.DPAD) {
                        Text(
                            "💡 4-Way Cross Pad and discrete directional buttons are mutually exclusive.",
                            fontSize = 11.sp,
                            color = NeonPalette.Cyan.copy(alpha = 0.8f),
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.04f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                    ) {
                        Column {
                            controlList.forEachIndexed { index, spec ->
                                val canonicalKey = spec.key
                                val isPresent = currentElements.keys.any { elemKey ->
                                    ControlKey.fromIdentifier(elemKey) == spec
                                }
                                val emoji = spec.emoji
                                val label = spec.label
                                val desc = spec.description
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onToggleControl(canonicalKey) }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isPresent,
                                        onCheckedChange = null,
                                        colors = CheckboxDefaults.colors(checkedColor = NeonPalette.Cyan)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    if (emoji.isNotBlank()) {
                                        Text(emoji, fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                canonicalKey,
                                                fontSize = 14.sp,
                                                fontWeight = if (isPresent) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isPresent) Color.White else Color.Gray
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                "• $label",
                                                fontSize = 12.sp,
                                                color = if (isPresent) NeonPalette.Cyan.copy(alpha = 0.8f) else Color.Gray.copy(alpha = 0.6f)
                                            )
                                        }
                                        if (desc.isNotBlank()) {
                                            Text(
                                                desc,
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                if (index < controlList.lastIndex) {
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onOpenStudio) {
                    Text("Button Studio 🎨", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPalette.Cyan),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Done", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
