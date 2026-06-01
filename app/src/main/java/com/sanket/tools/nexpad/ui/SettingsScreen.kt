package com.sanket.tools.nexpad.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.sanket.tools.nexpad.utils.LayoutManager
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel

@Composable
fun SettingsScreen(
    navController: NavController,
    layoutManager: LayoutManager,
    viewModel: GamepadViewModel,
    sharedPref: android.content.SharedPreferences
) {
    var ipAddress by remember { mutableStateOf(sharedPref.getString("LAST_IP", "10.204.233.238") ?: "") }
    var isConnected by remember { mutableStateOf(false) }
    var activeProfile by remember { mutableStateOf(layoutManager.getActiveProfile()) }
    val isGyroSteeringEnabled by viewModel.isGyroSteeringEnabled.collectAsState()
    val state by viewModel.inputState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A))
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            IconButton(onClick = { navController.popBackStack() }) {
                Text("⬅️", color = Color.White, fontSize = 24.sp)
            }
            Text("Settings & Connection", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
        }

        Spacer(modifier = Modifier.height(32.dp))

        // PC Connection
        Text("PC Connection", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = ipAddress,
                onValueChange = { ipAddress = it },
                label = { Text("PC IP Address (Wi-Fi or Hotspot)", color = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                enabled = !isConnected
            )
            Button(
                onClick = {
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
                },
                colors = ButtonDefaults.buttonColors(containerColor = if (isConnected) Color.Red else Color.Green)
            ) {
                Text(if (isConnected) "Disconnect" else "Connect")
            }
        }
        
        Text(if (isConnected) "🟢 Connected" else "🔴 Disconnected", fontWeight = FontWeight.Bold, color = if (isConnected) Color.Green else Color.Red)
        Spacer(modifier = Modifier.height(32.dp))

        // Aesthetic Settings
        Text("Aesthetics", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("RGB Lighting (Buttons & Joysticks)", color = Color.White, fontSize = 18.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Switch(
                checked = activeProfile.isRgbEnabled,
                onCheckedChange = { enabled -> 
                    activeProfile = activeProfile.copy(isRgbEnabled = enabled)
                    layoutManager.saveProfile(activeProfile)
                }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Gyro Settings
        Text("Sensors", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Gyro Steering (Forza Horizon)", color = Color.White, fontSize = 18.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Switch(
                checked = isGyroSteeringEnabled,
                onCheckedChange = { viewModel.toggleGyroSteering() }
            )
        }
        Text("Gyro X: ${"%.2f".format(state.gyroX)} | Y: ${"%.2f".format(state.gyroY)}", color = Color.Gray)
    }
}
