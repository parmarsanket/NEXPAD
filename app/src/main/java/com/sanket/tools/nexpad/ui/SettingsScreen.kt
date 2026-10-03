package com.sanket.tools.nexpad.ui

import android.content.SharedPreferences
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val profiles by layoutManager.profilesFlow.collectAsState()
    val activeProfileName by layoutManager.activeProfileNameFlow.collectAsState()
    val currentProfile = remember(profiles, activeProfileName) {
        profiles.find { it.name.equals(activeProfileName, ignoreCase = true) } ?: layoutManager.getActiveProfile()
    }

    var buttonHapticsEnabled by remember {
        mutableStateOf(sharedPref.getBoolean(HapticFeedbackHelper.PREF_BUTTON_HAPTICS_ENABLED, true))
    }
    var vibrateOfflineEnabled by remember {
        mutableStateOf(sharedPref.getBoolean(HapticFeedbackHelper.PREF_HAPTICS_OFFLINE_ENABLED, false))
    }
    var hapticClickStrength by remember {
        mutableFloatStateOf(sharedPref.getFloat(HapticFeedbackHelper.PREF_HAPTICS_CLICK_STRENGTH, 0.3f))
    }
    var hapticStyle by remember {
        mutableStateOf(
            sharedPref.getString(HapticFeedbackHelper.PREF_HAPTICS_STYLE, HapticFeedbackHelper.STYLE_CRISP)
                ?: HapticFeedbackHelper.STYLE_CRISP
        )
    }
    var rumbleMode by remember { mutableStateOf(sharedPref.getString("RUMBLE_MODE", "min") ?: "min") }

    val context = LocalContext.current
    val hapticHelper = remember(context) { HapticFeedbackHelper(context) }

    SettingsScreenContent(
        isConnected = isConnected,
        activeProfileName = currentProfile.name,
        isRgbEnabled = currentProfile.isRgbEnabled,
        buttonHapticsEnabled = buttonHapticsEnabled,
        vibrateOfflineEnabled = vibrateOfflineEnabled,
        hapticClickStrength = hapticClickStrength,
        hapticStyle = hapticStyle,
        rumbleMode = rumbleMode,
        onActiveProfileChange = { layoutManager.setActiveProfile(it) },
        onRgbChange = {
            layoutManager.saveProfile(currentProfile.copy(isRgbEnabled = it), activate = true)
        },
        onButtonHapticsChange = {
            buttonHapticsEnabled = it
            sharedPref.edit().putBoolean(HapticFeedbackHelper.PREF_BUTTON_HAPTICS_ENABLED, it).apply()
            if (it) {
                hapticHelper.performPreviewClick(style = hapticStyle, strength = hapticClickStrength)
            }
        },
        onVibrateOfflineChange = {
            vibrateOfflineEnabled = it
            sharedPref.edit().putBoolean(HapticFeedbackHelper.PREF_HAPTICS_OFFLINE_ENABLED, it).apply()
            hapticHelper.performPreviewClick(style = hapticStyle, strength = hapticClickStrength)
        },
        onHapticClickStrengthChange = {
            hapticClickStrength = it
            sharedPref.edit().putFloat(HapticFeedbackHelper.PREF_HAPTICS_CLICK_STRENGTH, it).apply()
        },
        onHapticClickStrengthFinished = {
            hapticHelper.performPreviewClick(style = hapticStyle, strength = hapticClickStrength)
        },
        onHapticStyleChange = {
            hapticStyle = it
            sharedPref.edit().putString(HapticFeedbackHelper.PREF_HAPTICS_STYLE, it).apply()
            hapticHelper.performPreviewClick(style = it, strength = hapticClickStrength)
        },
        onRumbleModeChange = {
            rumbleMode = it
            sharedPref.edit().putString("RUMBLE_MODE", it).apply()
        },
        onBack = { navController.popBackStack() }
    )
}

@Composable
fun SettingsScreenContent(
    isConnected: Boolean = false,
    activeProfileName: String = "Standard Elite",
    isRgbEnabled: Boolean = true,
    buttonHapticsEnabled: Boolean = true,
    vibrateOfflineEnabled: Boolean = false,
    hapticClickStrength: Float = 0.3f,
    hapticStyle: String = HapticFeedbackHelper.STYLE_CRISP,
    rumbleMode: String = "min",
    onActiveProfileChange: (String) -> Unit = {},
    onRgbChange: (Boolean) -> Unit = {},
    onButtonHapticsChange: (Boolean) -> Unit = {},
    onVibrateOfflineChange: (Boolean) -> Unit = {},
    onHapticClickStrengthChange: (Float) -> Unit = {},
    onHapticClickStrengthFinished: () -> Unit = {},
    onHapticStyleChange: (String) -> Unit = {},
    onRumbleModeChange: (String) -> Unit = {},
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = modifier,
        topBar = {
            NexpadTopAppBar(
                title = "SETTINGS",
                subtitle = "Controller & Engine Configuration",
                onBack = onBack
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
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // ── Card 1: Controller Profile & Lighting ────────────────────────
                SettingsSectionCard(
                    accentColor = NeonPalette.Cyan,
                    title = "CONTROLLER PROFILE & LIGHTING",
                    subtitle = "Active layout architecture & RGB aura bloom",
                    icon = Icons.Rounded.SportsEsports
                ) {
                    SettingsSegmentedSelector(
                        title = "Active Layout Profile",
                        description = "Switch primary controller architecture between Xbox and PlayStation ergonomics",
                        options = listOf(
                            Triple("Standard Elite", "Xbox Style", "Asymmetric Sticks • A/B/X/Y"),
                            Triple("PlayStation DualSense Pro", "PlayStation Style", "Symmetric Sticks • △/◯/✕/□")
                        ),
                        selectedValue = if (activeProfileName.contains("PlayStation", ignoreCase = true)) {
                            "PlayStation DualSense Pro"
                        } else if (activeProfileName.contains("Standard", ignoreCase = true)) {
                            "Standard Elite"
                        } else {
                            ""
                        },
                        onSelect = onActiveProfileChange,
                        accentColor = NeonPalette.Cyan
                    )

                    SettingsToggleRow(
                        title = "RGB Aura & Glow",
                        description = "Dynamic neon bloom and outer lighting radiating behind controller buttons, d-pads, and joysticks",
                        checked = isRgbEnabled,
                        onCheckedChange = onRgbChange,
                        accentColor = NeonPalette.Cyan
                    )
                }

                // ── Card 2: Touch Haptic Engine ───────────────────────────────────
                SettingsSectionCard(
                    accentColor = NeonPalette.Green,
                    title = "TOUCH HAPTIC ENGINE",
                    subtitle = "Linear Resonant Actuator (LRA) mechanical click emulation",
                    icon = Icons.Rounded.Vibration
                ) {
                    SettingsToggleRow(
                        title = "Tactile Button Haptics",
                        description = "Instant tactile click impulse delivered on every physical controller button and trigger press",
                        checked = buttonHapticsEnabled,
                        onCheckedChange = onButtonHapticsChange,
                        accentColor = NeonPalette.Green
                    )

                    SettingsToggleRow(
                        title = "Offline Haptic Feedback",
                        description = "Keep mechanical clicks active during offline sandbox and practice sessions",
                        checked = vibrateOfflineEnabled,
                        enabled = buttonHapticsEnabled,
                        onCheckedChange = onVibrateOfflineChange,
                        accentColor = NeonPalette.Green
                    )

                    SettingsSliderRow(
                        title = "Haptic Click Strength",
                        description = "Linear Resonant Actuator impulse power on button contact",
                        value = hapticClickStrength,
                        valueText = "${(hapticClickStrength * 100).toInt()}%",
                        valueRange = 0.1f..1.0f,
                        enabled = buttonHapticsEnabled,
                        onValueChange = onHapticClickStrengthChange,
                        onValueChangeFinished = onHapticClickStrengthFinished,
                        accentColor = NeonPalette.Green
                    )

                    SettingsSegmentedSelector(
                        title = "Mechanical Click Profile",
                        description = "Acoustic and tactile impulse waveform tuning",
                        options = listOf(
                            Triple(HapticFeedbackHelper.STYLE_CRISP, "Crisp Click", "Fast & Sharp"),
                            Triple(HapticFeedbackHelper.STYLE_HEAVY, "Heavy Snap", "Tactile Bump"),
                            Triple(HapticFeedbackHelper.STYLE_SOFT, "Soft Tick", "Quiet Tone")
                        ),
                        selectedValue = hapticStyle,
                        onSelect = onHapticStyleChange,
                        accentColor = NeonPalette.Green,
                        enabled = buttonHapticsEnabled
                    )
                }

                // ── Card 3: Controller Rumble Feedback ────────────────────────────
                SettingsSectionCard(
                    accentColor = NeonPalette.Magenta,
                    title = "PC GAME RUMBLE FEEDBACK",
                    subtitle = "Dual-motor force telemetry streaming from PC game engine",
                    icon = Icons.Rounded.Vibration
                ) {
                    SettingsSegmentedSelector(
                        title = "Stereo Mix Routing",
                        description = "Blends Left (low frequency / heavy) and Right (high frequency / sharp) motor channels",
                        options = listOf(
                            Triple("min", "Soft Min", "Subtle Floor (Default)"),
                            Triple("max", "Peak Force", "1:1 Peak Impact"),
                            Triple("smart", "Smart Intent", "Dynamic Stereo Blend"),
                            Triple("avg", "Balanced Average", "50/50 Channel Mean")
                        ),
                        selectedValue = rumbleMode,
                        onSelect = onRumbleModeChange,
                        accentColor = NeonPalette.Magenta
                    )

                    SettingsInfoRow(
                        label = "In-Game Rumble Volume",
                        value = "Controlled by PC Game (0–255 UInt8)",
                        accentColor = NeonPalette.Magenta
                    )
                }

                // ── Card 4: Engine Architecture & Hardware ────────────────────────
                SettingsSectionCard(
                    accentColor = NeonPalette.Purple,
                    title = "ENGINE ARCHITECTURE & HARDWARE",
                    subtitle = "Ultra-low latency streaming protocol and pipeline",
                    icon = Icons.Rounded.Shield
                ) {
                    SettingsInfoRow(label = "Active Layout Profile", value = activeProfileName, accentColor = NeonPalette.Cyan)
                    SettingsInfoRow(label = "Input Polling Rate", value = "1000 Hz Locked", accentColor = NeonPalette.Green)
                    SettingsInfoRow(label = "Transports Supported", value = "USB AOA 2.0 • ADB • Wi-Fi UDP", accentColor = NeonPalette.Purple)
                    SettingsInfoRow(label = "Haptic Pipeline", value = "Direct LRA Primitive Composition", accentColor = NeonPalette.Amber)
                    SettingsInfoRow(label = "Touch Surface", value = "Dedicated Multitouch Trackpad Engine", accentColor = NeonPalette.Magenta)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    accentColor: Color,
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF0F1523).copy(alpha = 0.85f),
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                listOf(
                    accentColor.copy(alpha = 0.45f),
                    accentColor.copy(alpha = 0.15f),
                    Color.White.copy(alpha = 0.04f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.12f))
                        .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp
                        )
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.5.sp
                        )
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                accentColor.copy(alpha = 0.35f),
                                accentColor.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        )
                    )
            )

            content()
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    accentColor: Color,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = if (enabled) Color.White else Color.Gray
                )
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = if (enabled) Color(0xFF94A3B8) else Color(0xFF556070),
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp
                )
            )
        }
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = accentColor,
                checkedTrackColor = accentColor.copy(alpha = 0.25f),
                checkedBorderColor = accentColor.copy(alpha = 0.6f),
                uncheckedThumbColor = Color(0xFF64748B),
                uncheckedTrackColor = Color(0xFF1E293B),
                uncheckedBorderColor = Color(0xFF334155),
                disabledCheckedThumbColor = accentColor.copy(alpha = 0.5f),
                disabledCheckedTrackColor = accentColor.copy(alpha = 0.12f)
            )
        )
    }
}

@Composable
private fun SettingsSliderRow(
    title: String,
    description: String,
    value: Float,
    valueText: String,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    accentColor: Color,
    steps: Int = 0,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = if (enabled) Color.White else Color.Gray
                )
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (enabled) accentColor.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.05f),
                border = BorderStroke(
                    1.dp,
                    if (enabled) accentColor.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.1f)
                )
            ) {
                Text(
                    text = valueText,
                    color = if (enabled) accentColor else Color.Gray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
        if (description.isNotBlank()) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = if (enabled) Color(0xFF94A3B8) else Color(0xFF556070),
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp
                )
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            steps = steps,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = Color(0xFF1E293B),
                disabledThumbColor = accentColor.copy(alpha = 0.4f),
                disabledActiveTrackColor = accentColor.copy(alpha = 0.2f),
                disabledInactiveTrackColor = Color(0xFF1E293B).copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
private fun <T> SettingsSegmentedSelector(
    title: String,
    description: String,
    options: List<Triple<T, String, String?>>,
    selectedValue: T,
    onSelect: (T) -> Unit,
    accentColor: Color,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) Color.White else Color.Gray
            )
        )
        if (description.isNotBlank()) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = if (enabled) Color(0xFF94A3B8) else Color(0xFF556070),
                    fontSize = 11.5.sp
                )
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { (value, label, sub) ->
                val isSelected = value == selectedValue
                Surface(
                    onClick = { if (enabled) onSelect(value) },
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = when {
                        !enabled -> Color(0xFF0C1019)
                        isSelected -> accentColor.copy(alpha = 0.18f)
                        else -> Color(0xFF0F172A).copy(alpha = 0.6f)
                    },
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = when {
                            !enabled -> Color.White.copy(alpha = 0.05f)
                            isSelected -> accentColor
                            else -> Color.White.copy(alpha = 0.1f)
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                color = when {
                                    !enabled -> Color.Gray
                                    isSelected -> accentColor
                                    else -> Color.White
                                },
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        )
                        if (sub != null) {
                            Text(
                                text = sub,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (isSelected) accentColor.copy(alpha = 0.8f) else Color(0xFF64748B),
                                    fontSize = 9.5.sp,
                                    textAlign = TextAlign.Center
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsInfoRow(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF070B12).copy(alpha = 0.6f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color(0xFF94A3B8),
                fontSize = 11.5.sp
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                color = accentColor,
                fontWeight = FontWeight.Bold,
                fontSize = 11.5.sp
            )
        )
    }
}
