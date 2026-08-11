package com.sanket.tools.nexpad.ui

import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.sanket.tools.nexpad.utils.LayoutManager
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel

@Composable
fun SettingsScreen(
    navController: NavController,
    layoutManager: LayoutManager,
    viewModel: GamepadViewModel,
    sharedPref: SharedPreferences
) {
    val isConnected by viewModel.isConnected.collectAsState()
    val diagnosticLog by viewModel.diagnosticLog.collectAsState()
    
    var ipAddress by remember { mutableStateOf(sharedPref.getString("LAST_IP", "") ?: "") }
    var profile by remember { mutableStateOf(layoutManager.getActiveProfile()) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(24.dp)
            .verticalScroll(scrollState)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { navController.popBackStack() }) {
                Text("⬅️", fontSize = 24.sp, color = Color.White)
            }
            Text("Settings", fontSize = 24.sp, color = Color.White)
        }

        Spacer(modifier = Modifier.height(32.dp))

        // PC Connection
        Text("Manual PC Connection (Fallback)", color = Color.LightGray)
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

        if (diagnosticLog.isNotEmpty()) {
            Text("Network Diagnostics", color = Color.LightGray, fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp))
            Text(
                diagnosticLog.takeLast(8).joinToString("\n"),
                color = Color(0xFF9E9E9E),
                fontSize = 10.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            )
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



        var rumbleIntensity by remember { mutableFloatStateOf(sharedPref.getFloat("RUMBLE_INTENSITY", 1.0f)) }

        Spacer(modifier = Modifier.height(32.dp))

        Text("Haptics & Vibration", color = Color.LightGray)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Rumble Intensity Master Volume: ${(rumbleIntensity * 100).toInt()}%", color = Color.White)
        }
        Slider(
            value = rumbleIntensity,
            onValueChange = { 
                rumbleIntensity = it 
                sharedPref.edit().putFloat("RUMBLE_INTENSITY", it).apply()
            },
            valueRange = 0f..1.0f,
            colors = SliderDefaults.colors(thumbColor = Color(0xFF00E676), activeTrackColor = Color(0xFF00E676))
        )
        
        // Gyro settings have been moved to the Desktop app.
    }
}
