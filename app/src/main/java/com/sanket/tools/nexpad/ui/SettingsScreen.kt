package com.sanket.tools.nexpad.ui

import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    sharedPref: SharedPreferences
) {
    var ipAddress by remember { mutableStateOf(sharedPref.getString("LAST_IP", "192.168.1.100") ?: "") }
    val isConnected by viewModel.isConnected.collectAsState()
    
    var profile by remember { mutableStateOf(layoutManager.getActiveProfile()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { navController.popBackStack() }) {
                Text("⬅️", fontSize = 24.sp, color = Color.White)
            }
            Text("Settings", fontSize = 24.sp, color = Color.White)
        }

        Spacer(modifier = Modifier.height(32.dp))

        // PC Connection
        Text("PC Connection (UDP)", color = Color.LightGray)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = ipAddress,
                onValueChange = { ipAddress = it },
                label = { Text("IP Address", color = Color.Gray) },
                modifier = Modifier.weight(1f),
                enabled = !isConnected,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF00E676),
                    unfocusedBorderColor = Color.DarkGray
                )
            )
            Spacer(modifier = Modifier.width(16.dp))
            Button(
                onClick = {
                    if (isConnected) {
                        viewModel.disconnect()
                    } else {
                        if (ipAddress.isNotBlank()) {
                            sharedPref.edit().putString("LAST_IP", ipAddress).apply()
                            viewModel.connect(ipAddress, 9999)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isConnected) Color.Red else Color(0xFF00C853)
                )
            ) {
                Text(if (isConnected) "Disconnect" else "Connect")
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))

        // Elite Customization Options
        Text("Elite Controller Settings", color = Color.LightGray)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("RGB Lighting", color = Color.White)
            Spacer(modifier = Modifier.weight(1f))
            Switch(
                checked = profile.isRgbEnabled,
                onCheckedChange = { 
                    profile = profile.copy(isRgbEnabled = it)
                    layoutManager.saveProfile(profile)
                },
                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E676), checkedTrackColor = Color.DarkGray)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Gyroscope Settings
        Text("Gyroscope Steering", color = Color.LightGray)
        
        val isGyroEnabled by viewModel.isGyroSteeringEnabled.collectAsState()
        val isGyroInverted by viewModel.isGyroInverted.collectAsState()
        val is6AxisEnabled by viewModel.is6AxisEnabled.collectAsState()
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Enable Gyro Steering (2D Joystick)", color = Color.White)
            Spacer(modifier = Modifier.weight(1f))
            Switch(
                checked = isGyroEnabled,
                onCheckedChange = { 
                    viewModel.isGyroSteeringEnabled.value = it
                    if (it) viewModel.is6AxisEnabled.value = false // Mutually exclusive
                    sharedPref.edit().putBoolean("ENABLE_GYRO", it).putBoolean("ENABLE_6AXIS", viewModel.is6AxisEnabled.value).apply()
                },
                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E676), checkedTrackColor = Color.DarkGray)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Invert Gyro Steering", color = Color.White)
            Spacer(modifier = Modifier.weight(1f))
            Switch(
                checked = isGyroInverted,
                onCheckedChange = { 
                    viewModel.isGyroInverted.value = it
                    sharedPref.edit().putBoolean("INVERT_GYRO", it).apply()
                },
                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E676), checkedTrackColor = Color.DarkGray),
                enabled = isGyroEnabled
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Enable 6-Axis Motion Data (PC Emulators)", color = Color.White)
            Spacer(modifier = Modifier.weight(1f))
            Switch(
                checked = is6AxisEnabled,
                onCheckedChange = { 
                    viewModel.is6AxisEnabled.value = it
                    if (it) viewModel.isGyroSteeringEnabled.value = false // Mutually exclusive
                    sharedPref.edit().putBoolean("ENABLE_6AXIS", it).putBoolean("ENABLE_GYRO", viewModel.isGyroSteeringEnabled.value).apply()
                },
                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E676), checkedTrackColor = Color.DarkGray)
            )
        }
    }
}
