package com.sanket.tools.nexpad.ui.components.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sanket.tools.nexpad.ui.theme.NeonPalette

/**
 * Universal, reusable single-field text input dialog styled with NEXPAD's Cyberpunk neon theme.
 * Use for renaming profiles, duplicating layouts, and naming custom layouts.
 */
@Composable
fun NexpadInputDialog(
    title: String,
    initialValue: String = "",
    label: String = "Name",
    placeholder: String = "",
    description: String? = null,
    confirmText: String = "Save",
    cancelText: String = "Cancel",
    confirmButtonColor: Color = NeonPalette.Cyan,
    confirmTextColor: Color = Color.Black,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var textValue by remember(initialValue) { mutableStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!description.isNullOrBlank()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    label = { Text(label, color = confirmButtonColor) },
                    placeholder = if (placeholder.isNotBlank()) {
                        { Text(placeholder, color = Color.Gray) }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = confirmButtonColor,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = textValue.trim()
                    if (trimmed.isNotEmpty()) {
                        onConfirm(trimmed)
                    }
                },
                enabled = textValue.trim().isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = confirmButtonColor),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = confirmText,
                    color = confirmTextColor,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = cancelText,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    )
}
