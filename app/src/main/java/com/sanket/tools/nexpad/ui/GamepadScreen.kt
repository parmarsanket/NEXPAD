package com.sanket.tools.nexpad.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.ui.components.ABXYLayout
import com.sanket.tools.nexpad.ui.components.DPadLayout
import com.sanket.tools.nexpad.ui.components.GamepadButton
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel

@Composable
fun GamepadScreen(
    viewModel: GamepadViewModel, 
    modifier: Modifier = Modifier,
    onVibrate: () -> Unit,
    triggerRumble: (Int, Int) -> Unit = { _, _ -> }
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedPref = remember { context.getSharedPreferences("NEXPAD_PREFS", Context.MODE_PRIVATE) }
    
    val state by viewModel.inputState.collectAsState()
    var ipAddress by remember { mutableStateOf(sharedPref.getString("LAST_IP", "10.204.233.238") ?: "") }
    var isConnected by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.feedbackFlow.collect { feedback ->
            triggerRumble(feedback.leftMotorSpeed, feedback.rightMotorSpeed)
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp).verticalScroll(state = rememberScrollState())) {
        // Top Connection Bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = ipAddress,
                onValueChange = { ipAddress = it },
                label = { Text("PC IP Address (Wi-Fi or Hotspot)") },
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                enabled = !isConnected
            )
            Button(onClick = {
                if (isConnected) {
                    viewModel.disconnect()
                    isConnected = false
                } else {
                    if (ipAddress.isNotBlank()) {
                        sharedPref.edit().putString("LAST_IP", ipAddress).apply()
                        viewModel.connect(ipAddress, 9999)
                        isConnected = true
                    }
                }
            }) {
                Text(if (isConnected) "Disconnect" else "Connect")
            }
        }

        // Gamepad Area
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Side: L2, L1, D-Pad, L3
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                GamepadButton("L2", isConnected, onVibrate, viewModel)
                Spacer(modifier = Modifier.height(8.dp))
                GamepadButton("L1", isConnected, onVibrate, viewModel)
                Spacer(modifier = Modifier.height(16.dp))
                DPadLayout(isConnected, onVibrate, viewModel)
                Spacer(modifier = Modifier.height(16.dp))
                GamepadButton("L3", isConnected, onVibrate, viewModel)
            }

            // Center: Menu Buttons and Live Sensor Data
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row {
                    GamepadButton("SELECT", isConnected, onVibrate, viewModel)
                    Spacer(modifier = Modifier.width(16.dp))
                    GamepadButton("GUIDE", isConnected, onVibrate, viewModel)
                    Spacer(modifier = Modifier.width(16.dp))
                    GamepadButton("START", isConnected, onVibrate, viewModel)
                }
                Spacer(modifier = Modifier.height(32.dp))
                Text(if (isConnected) "🟢 Connected" else "🔴 Disconnected", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                
                val isGyroSteeringEnabled by viewModel.isGyroSteeringEnabled.collectAsState()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Gyro Steering (Forza): ", fontWeight = FontWeight.Bold)
                    androidx.compose.material3.Switch(
                        checked = isGyroSteeringEnabled,
                        onCheckedChange = { viewModel.toggleGyroSteering() }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("Gyroscope (Gravity)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("X: ${"%.2f".format(state.gyroX)}")
                Text("Y: ${"%.2f".format(state.gyroY)}")
                Text("Z: ${"%.2f".format(state.gyroZ)}")
                
                if (isGyroSteeringEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Steering Output: ${"%.2f".format(state.leftStickX)}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }

            // Right Side: R2, R1, ABXY, R3
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                GamepadButton("R2", isConnected, onVibrate, viewModel)
                Spacer(modifier = Modifier.height(8.dp))
                GamepadButton("R1", isConnected, onVibrate, viewModel)
                Spacer(modifier = Modifier.height(16.dp))
                ABXYLayout(isConnected, onVibrate, viewModel)
                Spacer(modifier = Modifier.height(16.dp))
                GamepadButton("R3", isConnected, onVibrate, viewModel)
            }
        }
    }
}
