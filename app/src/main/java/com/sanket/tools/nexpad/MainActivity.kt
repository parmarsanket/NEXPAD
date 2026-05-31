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
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                        onVibrate = { vibrateDevice() },
                        triggerRumble = { l, r -> triggerRumble(l, r) }
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

    private fun triggerRumble(leftMotor: Int, rightMotor: Int) {
        val intensity = maxOf(leftMotor, rightMotor)
        if (intensity == 0) {
            vibrator.cancel()
            return
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val amplitude = (intensity.toFloat() / 255f * 255).toInt()
            vibrator.vibrate(VibrationEffect.createOneShot(200, amplitude))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(200)
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
    onVibrate: () -> Unit,
    triggerRumble: (Int, Int) -> Unit = { _, _ -> }
) {
    val state by viewModel.inputState.collectAsState()
    var ipAddress by remember { mutableStateOf("10.204.233.238") }
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

@Composable
fun DPadLayout(isConnected: Boolean, onVibrate: () -> Unit, viewModel: GamepadViewModel) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        GamepadButton("UP", isConnected, onVibrate, viewModel)
        Row {
            GamepadButton("LEFT", isConnected, onVibrate, viewModel)
            Spacer(modifier = Modifier.width(64.dp))
            GamepadButton("RIGHT", isConnected, onVibrate, viewModel)
        }
        GamepadButton("DOWN", isConnected, onVibrate, viewModel)
    }
}

@Composable
fun ABXYLayout(isConnected: Boolean, onVibrate: () -> Unit, viewModel: GamepadViewModel) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        GamepadButton("Y", isConnected, onVibrate, viewModel)
        Row {
            GamepadButton("X", isConnected, onVibrate, viewModel)
            Spacer(modifier = Modifier.width(64.dp))
            GamepadButton("B", isConnected, onVibrate, viewModel)
        }
        GamepadButton("A", isConnected, onVibrate, viewModel)
    }
}

@Composable
fun GamepadButton(
    text: String,
    isConnected: Boolean,
    onVibrate: () -> Unit,
    viewModel: GamepadViewModel,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    
    Box(
        modifier = modifier
            .padding(4.dp)
            .size(64.dp)
            .background(
                color = if (isPressed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                shape = CircleShape
            )
            .pointerInput(isConnected) {
                detectTapGestures(
                    onPress = {
                        if (isConnected) onVibrate()
                        isPressed = true
                        viewModel.updateButton(text, true)
                        tryAwaitRelease()
                        isPressed = false
                        viewModel.updateButton(text, false)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text, 
            color = if (isPressed) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
    }
}