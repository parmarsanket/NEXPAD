package com.sanket.tools.nexpad.ui

import android.content.SharedPreferences
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.ui.AppNavigator
import com.sanket.tools.nexpad.ui.components.common.NexpadTopAppBar
import com.sanket.tools.nexpad.ui.components.effects.CyberGrid
import com.sanket.tools.nexpad.ui.components.effects.ScanLine
import com.sanket.tools.nexpad.ui.layout.adaptiveLayoutSpec
import com.sanket.tools.nexpad.ui.theme.NeonPalette
import com.sanket.tools.nexpad.utils.HapticFeedbackHelper
import com.sanket.tools.nexpad.utils.LayoutManager
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel

@Composable
fun SettingsScreen(
    navController: AppNavigator,
    layoutManager: LayoutManager,
    viewModel: GamepadViewModel,
    sharedPref: SharedPreferences
) {
    val isConnected by viewModel.isConnected.collectAsState()
    val diagnosticLog by viewModel.diagnosticLog.collectAsState()
    val profiles by layoutManager.profilesFlow.collectAsState()
    val activeProfileName by layoutManager.activeProfileNameFlow.collectAsState()
    val currentProfile = remember(profiles, activeProfileName) {
        profiles.find { it.name.equals(activeProfileName, ignoreCase = true) } ?: layoutManager.getActiveProfile()
    }

    var ipAddress by remember { mutableStateOf(sharedPref.getString("LAST_IP", "") ?: "") }
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            NexpadTopAppBar(
                title = "Settings",
                subtitle = "App & Controller Configuration",
                onBack = { navController.popBackStack() }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            CyberGrid(modifier = Modifier.matchParentSize())
            ScanLine(modifier = Modifier.matchParentSize())

            val layout = adaptiveLayoutSpec(maxWidth, maxHeight)

            Column(
                modifier = Modifier
                    .widthIn(max = layout.formMaxWidth)
                    .fillMaxWidth()
                    .padding(horizontal = layout.horizontalPadding, vertical = layout.verticalPadding)
                    .verticalScroll(scrollState)
            ) {
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
                            focusedBorderColor = NeonPalette.Green,
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
                            containerColor = if (isConnected) NeonPalette.Red else Color(0xFF00C853)
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
                        checked = currentProfile.isRgbEnabled,
                        onCheckedChange = {
                            layoutManager.saveProfile(currentProfile.copy(isRgbEnabled = it), activate = true)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonPalette.Green, checkedTrackColor = Color.DarkGray)
                    )
                }

                var rightStickCameraMode by remember {
                    mutableStateOf(sharedPref.getBoolean("RIGHT_STICK_CAMERA_MODE", false))
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Right Stick Free-Look Camera Mode", color = Color.White)
                        Text(
                            "Relative swipe touchpad (Genshin / FPS style) instead of fixed anchor stick",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = rightStickCameraMode,
                        onCheckedChange = {
                            rightStickCameraMode = it
                            sharedPref.edit().putBoolean("RIGHT_STICK_CAMERA_MODE", it).apply()
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonPalette.Green, checkedTrackColor = Color.DarkGray)
                    )
                }

                var cameraSensitivity by remember {
                    mutableFloatStateOf(sharedPref.getFloat("CAMERA_SENSITIVITY", 1.0f))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Camera Look Sensitivity: ${String.format(java.util.Locale.US, "%.1fx", cameraSensitivity)}",
                        color = Color.White
                    )
                }
                Slider(
                    value = cameraSensitivity,
                    onValueChange = {
                        cameraSensitivity = it
                        sharedPref.edit().putFloat("CAMERA_SENSITIVITY", it).apply()
                    },
                    valueRange = 0.2f..3.0f,
                    steps = 27,
                    colors = SliderDefaults.colors(thumbColor = NeonPalette.Green, activeTrackColor = NeonPalette.Green)
                )

                // ── Controller Touch Haptics ──────────────────────────────────────────
                val context = LocalContext.current
                val hapticHelper = remember(context) { HapticFeedbackHelper(context) }

                var buttonHapticsEnabled by remember {
                    mutableStateOf(sharedPref.getBoolean(HapticFeedbackHelper.PREF_BUTTON_HAPTICS_ENABLED, true))
                }
                var vibrateOfflineEnabled by remember {
                    mutableStateOf(sharedPref.getBoolean(HapticFeedbackHelper.PREF_HAPTICS_OFFLINE_ENABLED, false))
                }
                var hapticClickStrength by remember {
                    mutableFloatStateOf(sharedPref.getFloat(HapticFeedbackHelper.PREF_HAPTICS_CLICK_STRENGTH, 0.1f))
                }
                var hapticStyle by remember {
                    mutableStateOf(
                        sharedPref.getString(HapticFeedbackHelper.PREF_HAPTICS_STYLE, HapticFeedbackHelper.STYLE_SOFT)
                            ?: HapticFeedbackHelper.STYLE_SOFT
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text("Controller Touch Haptics", color = Color.LightGray)

                // Master Toggle: Button Haptic Feedback
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Button Haptic Feedback", color = Color.White)
                        Text(
                            "Tactile mechanical click on controller button press",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = buttonHapticsEnabled,
                        onCheckedChange = {
                            buttonHapticsEnabled = it
                            sharedPref.edit().putBoolean(HapticFeedbackHelper.PREF_BUTTON_HAPTICS_ENABLED, it).apply()
                            if (it) {
                                hapticHelper.performPreviewClick(style = hapticStyle, strength = hapticClickStrength)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeonPalette.Green,
                            checkedTrackColor = Color(0xFF1E3A2B)
                        )
                    )
                }

                // Sub-Toggle: Vibrate While Disconnected
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Vibrate While Disconnected",
                            color = if (buttonHapticsEnabled) Color.White else Color.Gray
                        )
                        Text(
                            "Enable button clicks even when not connected to laptop",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = vibrateOfflineEnabled,
                        enabled = buttonHapticsEnabled,
                        onCheckedChange = {
                            vibrateOfflineEnabled = it
                            sharedPref.edit().putBoolean(HapticFeedbackHelper.PREF_HAPTICS_OFFLINE_ENABLED, it).apply()
                            hapticHelper.performPreviewClick(style = hapticStyle, strength = hapticClickStrength)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeonPalette.Green,
                            checkedTrackColor = Color(0xFF1E3A2B)
                        )
                    )
                }

                // Slider: Haptic Click Strength
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Haptic Click Strength: ${(hapticClickStrength * 100).toInt()}%",
                        color = if (buttonHapticsEnabled) Color.White else Color.Gray
                    )
                }
                Slider(
                    value = hapticClickStrength,
                    enabled = buttonHapticsEnabled,
                    onValueChange = {
                        hapticClickStrength = it
                        sharedPref.edit().putFloat(HapticFeedbackHelper.PREF_HAPTICS_CLICK_STRENGTH, it).apply()
                    },
                    onValueChangeFinished = {
                        hapticHelper.performPreviewClick(style = hapticStyle, strength = hapticClickStrength)
                    },
                    valueRange = 0.1f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = NeonPalette.Green,
                        activeTrackColor = NeonPalette.Green
                    )
                )

                // Buttons: Haptic Style
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "Mechanical Click Profile",
                    color = if (buttonHapticsEnabled) Color.LightGray else Color.Gray,
                    fontSize = 12.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val styles = listOf(
                        HapticFeedbackHelper.STYLE_CRISP to "Crisp Click",
                        HapticFeedbackHelper.STYLE_HEAVY to "Heavy Snap",
                        HapticFeedbackHelper.STYLE_SOFT to "Soft Tick"
                    )
                    styles.forEach { (id, label) ->
                        val isSelected = hapticStyle == id
                        Button(
                            onClick = {
                                hapticStyle = id
                                sharedPref.edit().putString(HapticFeedbackHelper.PREF_HAPTICS_STYLE, id).apply()
                                hapticHelper.performPreviewClick(style = id, strength = hapticClickStrength)
                            },
                            enabled = buttonHapticsEnabled,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) NeonPalette.Green else Color.DarkGray,
                                contentColor = if (isSelected) Color.Black else Color.White
                            )
                        ) {
                            Text(label, fontSize = 11.5.sp)
                        }
                    }
                }

                // ── PC Game Rumble ────────────────────────────────────────────────────
                var rumbleIntensity by remember { mutableFloatStateOf(sharedPref.getFloat("RUMBLE_INTENSITY", 1.0f)) }

                Spacer(modifier = Modifier.height(32.dp))

                Text("PC Game Rumble (Motor Stream)", color = Color.LightGray)
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
                    colors = SliderDefaults.colors(thumbColor = NeonPalette.Green, activeTrackColor = NeonPalette.Green)
                )

                Spacer(modifier = Modifier.height(16.dp))

                var rumbleMode by remember { mutableStateOf(sharedPref.getString("RUMBLE_MODE", "min") ?: "min") }

                Text("Rumble Mode (Stereo Mix)", color = Color.LightGray)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val modes = listOf(
                        "smart" to "Smart",
                        "max" to "Max",
                        "avg" to "Avg",
                        "min" to "Min"
                    )

                    modes.forEach { (id, label) ->
                        val isSelected = rumbleMode == id
                        Button(
                            onClick = {
                                rumbleMode = id
                                sharedPref.edit().putString("RUMBLE_MODE", id).apply()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) NeonPalette.Green else Color.DarkGray,
                                contentColor = if (isSelected) Color.Black else Color.White
                            )
                        ) {
                            Text(label)
                        }
                    }
                }
            }
        }
    }
}
