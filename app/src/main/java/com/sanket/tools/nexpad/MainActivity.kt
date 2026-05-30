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

        // Auto-connect to a default IP for testing (can be changed later)
        viewModel.connect("192.168.1.100", 9999)

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

    Row(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Side: Basic representation
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Left Stick/DPad Area")
        }

        // Center: Live Sensor Data
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Networking: UDP @ 60Hz")
            Text("Gyro: X:${"%.1f".format(state.gyroX)} Y:${"%.1f".format(state.gyroY)}")
            Text("Button A: ${if (state.btnA) "PRESSED" else "IDLE"}")
        }

        // Right Side: Action Buttons with press state
        Column {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                onVibrate()
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