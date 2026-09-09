package com.sanket.tools.nexpad.ui.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sanket.tools.nexpad.runtime.model.NxpComponentDef
import com.sanket.tools.nexpad.runtime.registry.ComponentRegistry
import com.sanket.tools.nexpad.ui.theme.NeonPalette

@Composable
fun ComponentImportDialog(
    onDismiss: () -> Unit,
    onImportSuccess: (NxpComponentDef) -> Unit
) {
    val context = LocalContext.current
    var jsonText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val sampleTemplate = """
{
  "manifest": {
    "id": "custom.cyber_fire_x",
    "name": "Cyber Fire X Button",
    "author": "AI Assistant",
    "version": "1.0.0",
    "category": "BUTTON",
    "defaultControl": "X",
    "description": "Cyberpunk fiery neon X button."
  },
  "geometry": {
    "type": "Polygon",
    "sides": 6,
    "cornerRadius": 12.0
  },
  "visual": {
    "fillColor": "#1E0524",
    "opacity": 0.92,
    "borderColor": "#FF007F",
    "borderWidth": 2.5
  },
  "pressed": {
    "scale": 0.85,
    "rotation": 5.0,
    "fillColor": "#FF007F",
    "borderColor": "#FFFFFF",
    "springDamping": 0.55,
    "springStiffness": 750.0
  },
  "label": {
    "text": "X",
    "color": "#FF007F",
    "pressedColor": "#FFFFFF",
    "fontSize": 24.0
  },
  "size": {
    "widthDp": 76,
    "heightDp": 76
  }
}
    """.trimIndent()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xEE050B14))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.9f)
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.5.dp, NeonPalette.Cyan.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF0F1A2E), Color(0xFF070D18))
                        )
                    )
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Import Component",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = NeonPalette.Cyan
                            )
                        )
                        Text(
                            text = "Paste AI JSON or load a sample template",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color.LightGray)
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                // Quick Action: Paste Sample Template
                OutlinedButton(
                    onClick = {
                        jsonText = sampleTemplate
                        errorMessage = null
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonPalette.Cyan)
                ) {
                    Icon(Icons.Rounded.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Insert AI Sample Template", fontSize = 12.sp)
                }

                // JSON Text Editor
                OutlinedTextField(
                    value = jsonText,
                    onValueChange = {
                        jsonText = it
                        errorMessage = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    placeholder = { Text("Paste .nxpcomponent JSON here...", color = Color.Gray, fontSize = 12.sp) },
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = Color.White,
                        fontSize = 12.sp
                    ),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonPalette.Cyan,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedContainerColor = Color(0xFF040810),
                        unfocusedContainerColor = Color(0xFF040810)
                    )
                )

                // Error Banner if validation fails
                if (errorMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel", color = Color.White)
                    }
                    Button(
                        onClick = {
                            if (jsonText.isBlank()) {
                                errorMessage = "Please paste JSON content first."
                                return@Button
                            }
                            val registry = ComponentRegistry.getInstance(context)
                            val result = registry.importFromJson(jsonText)
                            result.fold(
                                onSuccess = { def ->
                                    onImportSuccess(def)
                                },
                                onFailure = { ex ->
                                    errorMessage = "Import Error: ${ex.message ?: "Invalid JSON syntax"}"
                                }
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPalette.Cyan),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Validate & Save", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
