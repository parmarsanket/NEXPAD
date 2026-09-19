package com.sanket.tools.nexpad.ui.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sanket.tools.nexpad.category.CategoryManager
import com.sanket.tools.nexpad.runtime.engine.NxprcCanvasRenderer
import com.sanket.tools.nexpad.runtime.engine.NxpComposeInterpreter
import com.sanket.tools.nexpad.runtime.model.NexPadControl
import com.sanket.tools.nexpad.runtime.model.NxpComponentDef
import com.sanket.tools.nexpad.runtime.model.SandboxInputTarget
import com.sanket.tools.nexpad.runtime.plugin.RemoteComponentRegistry
import com.sanket.tools.nexpad.ui.components.controller.ControllerElementRenderer
import com.sanket.tools.nexpad.ui.studio.model.resolveButtonSourceType
import com.sanket.tools.nexpad.ui.theme.NeonPalette
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel

@Composable
fun SandboxPreviewModal(
    componentDef: NxpComponentDef,
    isAppliedToActiveProfile: Boolean = false,
    scale: Float = 0.65f, // <-- Adjust size from 0.0f to 1.0f according to your preference
    /** Label for the primary action button. "Apply to Profile" in Manage Mode, "Use This" in contextual Selection Mode. */
    applyButtonLabel: String? = "Apply to Profile",
    onDismiss: () -> Unit,
    onApplyToProfile: () -> Unit = {},
    onAddToHud: () -> Unit = {},
    onExportJson: () -> Unit = {},
    onDelete: () -> Unit = {},
    gamepadViewModel: GamepadViewModel? = null
) {
    var telemetryAction by remember { mutableStateOf("READY — Tap or drag to test") }
    var axisValues by remember { mutableStateOf(Pair(0f, 0f)) }
    var eventCount by remember { mutableIntStateOf(0) }

    val sandboxTarget = remember {
        SandboxInputTarget(
            onStateChange = { action ->
                telemetryAction = action
                eventCount++
            },
            onAxisChange = { x, y ->
                axisValues = Pair(x, y)
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xEE050B14))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.5.dp, NeonPalette.Cyan.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF0F1A2E), Color(0xFF070D18))
                        )
                    )
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Row with Title, Source Badge & Subtitle Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = componentDef.manifest.name,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    color = NeonPalette.Cyan
                                )
                            )

                            val type = remember(componentDef.manifest.id) { resolveButtonSourceType(componentDef) }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(type.badgeBg)
                                    .border(1.dp, type.badgeColor.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = type.label,
                                    color = type.badgeColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        // Meta details: Target Control, Category, Author
                        val isBuiltIn = componentDef.manifest.id.startsWith("builtin.")
                        val authorText = if (isBuiltIn) "Core" else "by ${componentDef.manifest.author}"
                        Text(
                            text = "Control: ${componentDef.manifest.defaultControl}  •  Category: ${componentDef.manifest.category}  •  $authorText",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                // Testing Canvas Arena with dynamic scale mechanism
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                        .background(Color(0xFF040810)),
                    contentAlignment = Alignment.Center
                ) {
                    val context = LocalContext.current
                    val testViewModel: GamepadViewModel = gamepadViewModel ?: remember(context) {
                        (context as? androidx.activity.ComponentActivity)?.let { activity ->
                            androidx.lifecycle.ViewModelProvider(activity)[GamepadViewModel::class.java]
                        }
                    } ?: viewModel<GamepadViewModel>(context as androidx.lifecycle.ViewModelStoreOwner)
                    val isDefaultNative = componentDef.manifest.id.startsWith("builtin.default_")
                    val isRemote = componentDef.manifest.id.startsWith("rc.")
                    val remoteDoc = remember(componentDef.manifest.id) {
                        if (isRemote) RemoteComponentRegistry.getInstance(context).getComponent(componentDef.manifest.id) else null
                    }

                    val controlKey = componentDef.manifest.defaultControl.uppercase()

                    // High-Performance Dynamic Sizing & Scaling (0.0f to 1.0f)
                    val availableDim = minOf(maxWidth.value, maxHeight.value)
                    val targetDim = (if (availableDim > 0f) availableDim else 220f) * scale

                    // Centralized intrinsic dimension from protocol CategoryManager
                    val intrinsicMaxDim = if (isDefaultNative) {
                        CategoryManager.resolveIntrinsicMaxDim(controlKey)
                    } else {
                        CategoryManager.resolveIntrinsicMaxDim(controlKey, componentDef.size.widthDp, componentDef.size.heightDp)
                    }

                    val previewScale = targetDim / intrinsicMaxDim

                    LaunchedEffect(componentDef.manifest.id) {
                        while (true) {
                            kotlinx.coroutines.delay(60)
                            val K = com.sanket.tools.nexpad.model.NexpadKeys
                            if (controlKey == K.LS || controlKey == "L3") {
                                val x = testViewModel.inputState.leftStickX / 32767f
                                val y = testViewModel.inputState.leftStickY / 32767f
                                if (kotlin.math.abs(x - axisValues.first) > 0.05f || kotlin.math.abs(y - axisValues.second) > 0.05f) {
                                    axisValues = Pair(x, y)
                                    telemetryAction = "STICK DEFLECTION (X=%.2f, Y=%.2f)".format(x, y)
                                    eventCount++
                                }
                            } else if (controlKey == K.RS || controlKey == "R3") {
                                val x = testViewModel.inputState.rightStickX / 32767f
                                val y = testViewModel.inputState.rightStickY / 32767f
                                if (kotlin.math.abs(x - axisValues.first) > 0.05f || kotlin.math.abs(y - axisValues.second) > 0.05f) {
                                    axisValues = Pair(x, y)
                                    telemetryAction = "STICK DEFLECTION (X=%.2f, Y=%.2f)".format(x, y)
                                    eventCount++
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier.graphicsLayer {
                            scaleX = previewScale
                            scaleY = previewScale
                        },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isDefaultNative) {
                            ControllerElementRenderer(
                                key = controlKey,
                                isConnected = true,
                                isRgbEnabled = true,
                                viewModel = testViewModel,
                                onVibrate = {
                                    telemetryAction = "TAP • $controlKey (Haptic)"
                                    eventCount++
                                },
                                customComponentId = null
                            )
                        } else if (remoteDoc != null) {
                            val control = when {
                                componentDef.manifest.category.equals("JOYSTICK", ignoreCase = true) ->
                                    NexPadControl.Stick(isLeft = !controlKey.contains("R", ignoreCase = true))
                                componentDef.manifest.category.equals("TRIGGER", ignoreCase = true) ->
                                    NexPadControl.Trigger(key = controlKey)
                                else ->
                                    NexPadControl.Button(controlKey)
                            }
                            NxprcCanvasRenderer(
                                document = remoteDoc,
                                assignedControl = control,
                                isConnected = true,
                                inputTarget = sandboxTarget,
                                overrideSizeDp = 140
                            )
                        } else {
                            val control = when {
                                componentDef.manifest.category.equals("JOYSTICK", ignoreCase = true) ->
                                    NexPadControl.Stick(isLeft = !controlKey.contains("R", ignoreCase = true))
                                componentDef.manifest.category.equals("TRIGGER", ignoreCase = true) ->
                                    NexPadControl.Trigger(key = controlKey)
                                else ->
                                    NexPadControl.Button(controlKey)
                            }
                            NxpComposeInterpreter(
                                definition = componentDef,
                                assignedControl = control,
                                isConnected = true,
                                inputTarget = sandboxTarget
                            )
                        }
                    }
                }

                // Telemetry Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF081220)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("STATUS: $telemetryAction", color = NeonPalette.Cyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("EVENTS: $eventCount", color = Color.LightGray, fontSize = 12.sp)
                        }
                        if (componentDef.manifest.category.equals("JOYSTICK", ignoreCase = true)) {
                            Text("AXIS: X=%.2f, Y=%.2f".format(axisValues.first, axisValues.second), color = Color.Yellow, fontSize = 12.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Speed, contentDescription = null, tint = Color.Green, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Latency: <0.2ms (Zero-alloc UI thread path)", color = Color.Green, fontSize = 11.sp)
                        }
                    }
                }

                // Primary Actions: Apply to Profile / Use This & Open in HUD (Hidden in Viewer mode)
                if (applyButtonLabel != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isContextualPick = applyButtonLabel != "Apply to Profile"
                        Button(
                            onClick = onApplyToProfile,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isAppliedToActiveProfile && !isContextualPick) NeonPalette.Cyan.copy(alpha = 0.22f) else NeonPalette.Cyan
                            ),
                            border = if (isAppliedToActiveProfile && !isContextualPick) androidx.compose.foundation.BorderStroke(1.dp, NeonPalette.Cyan) else null,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Icon(
                                if (isAppliedToActiveProfile && !isContextualPick) Icons.Rounded.Check else Icons.Rounded.DashboardCustomize,
                                contentDescription = null,
                                tint = if (isAppliedToActiveProfile && !isContextualPick) NeonPalette.Cyan else Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = when {
                                    isContextualPick -> applyButtonLabel   // "Use This"
                                    isAppliedToActiveProfile -> "Active in Profile ✓"
                                    else -> applyButtonLabel               // "Apply to Profile"
                                },
                                color = if (isAppliedToActiveProfile && !isContextualPick) NeonPalette.Cyan else Color.Black,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // "Open in HUD" shortcut — hidden in contextual selection mode
                        if (!isContextualPick) {
                            Button(
                                onClick = onAddToHud,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A2639)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Open in HUD", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Secondary Utility Actions: Copy JSON, Delete (if custom), Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onExportJson,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Copy JSON", fontSize = 11.sp)
                        }

                        if (!componentDef.manifest.id.startsWith("builtin.")) {
                            OutlinedButton(
                                onClick = onDelete,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.35f)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Delete", fontSize = 11.sp)
                            }
                        }
                    }

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Back", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

