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
import com.sanket.tools.nexpad.bluetooth.BluetoothPermissionHelper
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
    var isBluetoothMode by remember { mutableStateOf(sharedPref.getBoolean("BLUETOOTH_MODE", false)) }
    val isConnected by viewModel.isConnected.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val diagnosticLog by viewModel.diagnosticLog.collectAsState()
    
    var profile by remember { mutableStateOf(layoutManager.getActiveProfile()) }

    val scrollState = rememberScrollState()
    val context = LocalContext.current
    var bluetoothPermissionsGranted by remember {
        mutableStateOf(BluetoothPermissionHelper.hasAllPermissions(context))
    }

    val discoverabilityLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != 0) {
            viewModel.startAdvertising()
        }
    }

    fun requestDiscoverability() {
        discoverabilityLauncher.launch(
            Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        bluetoothPermissionsGranted = permissions.values.all { it }
        if (bluetoothPermissionsGranted) {
            requestDiscoverability()
        }
    }

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

        // Connection Mode Toggle
        Text("Connection Mode", color = Color.LightGray)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = {
                    isBluetoothMode = false
                    sharedPref.edit().putBoolean("BLUETOOTH_MODE", false).apply()
                    viewModel.setConnectionMode(false)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!isBluetoothMode) Color(0xFF00C853) else Color.DarkGray
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp, topEnd = 0.dp, bottomEnd = 0.dp)
            ) {
                Text("Desktop Mode (WiFi)")
            }
            Button(
                onClick = {
                    isBluetoothMode = true
                    sharedPref.edit().putBoolean("BLUETOOTH_MODE", true).apply()
                    viewModel.setConnectionMode(true)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isBluetoothMode) Color(0xFF00C853) else Color.DarkGray
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 16.dp, bottomEnd = 16.dp)
            ) {
                Text("Bluetooth Mode (HID)")
            }
        }

        if (!isBluetoothMode) {
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
        } else {
            // Bluetooth Connection
            Text("Universal Bluetooth Controller", color = Color.LightGray)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Turn your phone into a native Bluetooth gamepad.", color = Color.Gray, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(16.dp))
                Button(
                    onClick = {
                        if (isConnected) {
                            viewModel.disconnect()
                        } else {
                            if (BluetoothPermissionHelper.hasAllPermissions(context)) {
                                requestDiscoverability()
                            } else {
                                permissionLauncher.launch(BluetoothPermissionHelper.REQUIRED_PERMISSIONS)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isConnected) Color.Red else Color(0xFF00B0FF)
                    )
                ) {
                    Text(if (isConnected) "Stop Advertising" else "Start Advertising")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(connectionStatus, color = if (isConnected) Color(0xFF00E676) else Color.LightGray, fontSize = 13.sp)
            Text(
                "Pair from the TV/PC Bluetooth screen while this phone is discoverable. Select a saved host below only when reconnecting.",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )

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
