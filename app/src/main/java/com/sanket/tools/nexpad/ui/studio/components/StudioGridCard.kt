package com.sanket.tools.nexpad.ui.studio.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.runtime.engine.NxpComposeInterpreter
import com.sanket.tools.nexpad.runtime.model.NexPadControl
import com.sanket.tools.nexpad.runtime.model.NxpComponentDef
import com.sanket.tools.nexpad.runtime.model.SandboxInputTarget
import com.sanket.tools.nexpad.ui.components.controller.ControllerElementRenderer
import com.sanket.tools.nexpad.ui.studio.model.ButtonStudioMode
import com.sanket.tools.nexpad.ui.studio.model.ButtonStudioType
import com.sanket.tools.nexpad.ui.studio.model.resolveButtonSourceType
import com.sanket.tools.nexpad.ui.theme.NeonPalette
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel

/**
 * Grid Card displaying button preview with Dual Mode behavior for non-ABXY controls.
 */
@Composable
fun StudioGridCard(
    def: NxpComponentDef,
    mode: ButtonStudioMode,
    isSelectedInBuilder: Boolean,
    dummyViewModel: GamepadViewModel,
    onToggleSelectInBuilder: () -> Unit,
    onUseInHud: () -> Unit,
    onTest: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isBuiltIn = def.manifest.id.startsWith("builtin.")
    val type = remember(def.manifest.id) { resolveButtonSourceType(def) }
    val dummyTarget = remember { SandboxInputTarget() }

    val borderColor = if (mode == ButtonStudioMode.SELECTION && isSelectedInBuilder) NeonPalette.Cyan else Color.White.copy(alpha = 0.10f)
    val borderWidth = if (mode == ButtonStudioMode.SELECTION && isSelectedInBuilder) 2.dp else 1.dp

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(borderWidth, borderColor, RoundedCornerShape(14.dp)),
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
                    .height(90.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF040810))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                val controlKey = def.manifest.defaultControl.uppercase()
                val maxDim = maxOf(def.size.widthDp, def.size.heightDp).toFloat()
                val previewScale = if (maxDim > 60f) 60f / maxDim else 1.0f

                Box(
                    modifier = Modifier.graphicsLayer {
                        scaleX = previewScale
                        scaleY = previewScale
                    },
                    contentAlignment = Alignment.Center
                ) {
                    if (type == ButtonStudioType.DEFAULT) {
                        ControllerElementRenderer(
                            key = controlKey,
                            isConnected = false,
                            isRgbEnabled = true,
                            viewModel = dummyViewModel,
                            onVibrate = {},
                            customComponentId = null
                        )
                    } else {
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
                    text = "Control: ${def.manifest.defaultControl}",
                    color = NeonPalette.Cyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = if (isBuiltIn) "Core" else "by ${def.manifest.author}",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Primary Action: Mode-dependent
            if (mode == ButtonStudioMode.SELECTION) {
                Button(
                    onClick = onToggleSelectInBuilder,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelectedInBuilder) NeonPalette.Cyan else Color.White.copy(alpha = 0.12f)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                ) {
                    Icon(
                        if (isSelectedInBuilder) Icons.Rounded.CheckCircle else Icons.Rounded.AddCircleOutline,
                        contentDescription = null,
                        tint = if (isSelectedInBuilder) Color.Black else Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (isSelectedInBuilder) "Active in Layout ✓" else "Select for Layout",
                        color = if (isSelectedInBuilder) Color.Black else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Button(
                    onClick = onUseInHud,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPalette.Cyan),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                ) {
                    Icon(Icons.Rounded.DashboardCustomize, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Use in HUD", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Secondary Action Row (Test Sandbox, Copy JSON, Delete)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(onClick = onTest, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = "Test in Sandbox", tint = NeonPalette.Cyan, modifier = Modifier.size(16.dp))
                    }

                    IconButton(onClick = onExport, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy JSON", tint = Color.LightGray, modifier = Modifier.size(14.dp))
                    }
                }

                if (!isBuiltIn && mode == ButtonStudioMode.MANAGE) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
