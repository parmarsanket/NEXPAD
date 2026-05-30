package com.sanket.tools.nexpad

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.sanket.tools.nexpad.sensors.GyroSensor
import com.sanket.tools.nexpad.ui.theme.NEXPADTheme
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel

class MainActivity : ComponentActivity() {
    
    private lateinit var viewModel: GamepadViewModel
    private lateinit var gyroSensor: GyroSensor
    private lateinit var vibrator: Vibrator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        viewModel = ViewModelProvider(this)[GamepadViewModel::class.java]

        // Setup Vibrator
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        // Setup Gyro
        gyroSensor = GyroSensor(this) { x, y, z ->
            viewModel.updateGyro(x, y, z)
        }

        enableEdgeToEdge()
        setContent {
            NEXPADTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    GamepadScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding),
                        onVibrate = { vibrateDevice() }
                    )
                }
            }
        }
    }

    private fun vibrateDevice() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(50)
        }
    }

    override fun onResume() {
        super.onResume()
        gyroSensor.start()
    }

    override fun onPause() {
        super.onPause()
        gyroSensor.stop()
    }
}

@Composable
fun GamepadScreen(
    viewModel: GamepadViewModel, 
    modifier: Modifier = Modifier,
    onVibrate: () -> Unit
) {
    val state by viewModel.inputState.collectAsState()
    var ipAddress by remember { mutableStateOf("10.204.233.238") }
    var isConnected by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
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
                        viewModel.connect(ipAddress, 9999)
                        isConnected = true
                    }
                }
            }) {
                Text(if (isConnected) "Disconnect" else "Connect")
            }
        }

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Side: Basic representation
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Left Stick Area")
            }

            // Center: Live Sensor Data
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (isConnected) "🟢 Connected to $ipAddress" else "🔴 Disconnected")
                Spacer(modifier = Modifier.height(8.dp))
                Text("Gyro: X:${"%.1f".format(state.gyroX)} Y:${"%.1f".format(state.gyroY)}")
                Text("Button A: ${if (state.btnA) "PRESSED" else "IDLE"}")
            }

            // Right Side: Action Buttons with press state
            Column {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .pointerInput(isConnected) {
                            detectTapGestures(
                                onPress = {
                                    if (isConnected) onVibrate()
                                    viewModel.updateButton("A", true)
                                    tryAwaitRelease()
                                    viewModel.updateButton("A", false)
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Button(onClick = { }) {
                        Text("A")
                    }
                }
            }
        }
    }
}