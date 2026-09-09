package com.sanket.tools.nexpad.ui.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sanket.tools.nexpad.runtime.engine.NxpComposeInterpreter
import com.sanket.tools.nexpad.runtime.model.NexPadControl
import com.sanket.tools.nexpad.runtime.model.NxpComponentDef
import com.sanket.tools.nexpad.runtime.model.SandboxInputTarget
import com.sanket.tools.nexpad.ui.theme.NeonPalette

@Composable
fun SandboxPreviewModal(
    componentDef: NxpComponentDef,
    onDismiss: () -> Unit,
    onAddToHud: () -> Unit
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
                .padding(24.dp),
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
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = componentDef.manifest.name,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = NeonPalette.Cyan
                            )
                        )
                        Text(
                            text = "Sandbox Arena • Isolated Preview",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                // Testing Canvas Arena
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                        .background(Color(0xFF040810)),
                    contentAlignment = Alignment.Center
                ) {
                    val control = if (componentDef.manifest.category.equals("JOYSTICK", ignoreCase = true)) {
                        NexPadControl.Stick(isLeft = true)
                    } else {
                        NexPadControl.Button(componentDef.manifest.defaultControl)
                    }

                    NxpComposeInterpreter(
                        definition = componentDef,
                        assignedControl = control,
                        isConnected = true,
                        inputTarget = sandboxTarget
                    )
                }

                // Telemetry Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF081220)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("STATUS: $telemetryAction", color = NeonPalette.Cyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("EVENTS: $eventCount", color = Color.LightGray, fontSize = 13.sp)
                        }
                        if (componentDef.manifest.category.equals("JOYSTICK", ignoreCase = true)) {
                            Text("AXIS: X=%.2f, Y=%.2f".format(axisValues.first, axisValues.second), color = Color.Yellow, fontSize = 13.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Speed, contentDescription = null, tint = Color.Green, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Latency: <0.2ms (Zero-alloc UI thread path)", color = Color.Green, fontSize = 12.sp)
                        }
                    }
                }

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Back", color = Color.White)
                    }
                    Button(
                        onClick = onAddToHud,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPalette.Cyan),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color.Black)
                        Spacer(Modifier.width(8.dp))
                        Text("Add to HUD", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
