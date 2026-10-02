package com.sanket.tools.nexpad.screenshot

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.sanket.tools.nexpad.ui.SettingsScreenContent
import com.sanket.tools.nexpad.ui.theme.NEXPADTheme
import com.sanket.tools.nexpad.utils.HapticFeedbackHelper

/**
 * Compose Preview Screenshot Tests for [SettingsScreenContent] and its configuration controls:
 * - Default settings in Portrait
 * - Settings with active Network Diagnostics
 * - Custom tuned configuration (Camera mode, mechanical click, rumble modes)
 * - Adaptive layout centering in Landscape
 */
class SettingsScreenScreenshotTest {

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF08090C, widthDp = 390, heightDp = 844)
    @Composable
    fun settingsScreenPortraitDefaultPreview() {
        NEXPADTheme {
            SettingsScreenContent(
                isConnected = false,
                ipAddress = "192.168.1.50",
                isRgbEnabled = true,
                rightStickCameraMode = false,
                cameraSensitivity = 1.0f,
                buttonHapticsEnabled = true,
                vibrateOfflineEnabled = false,
                hapticClickStrength = 0.5f,
                hapticStyle = HapticFeedbackHelper.STYLE_CRISP,
                rumbleIntensity = 1.0f,
                rumbleMode = "min"
            )
        }
    }

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF08090C, widthDp = 390, heightDp = 844)
    @Composable
    fun settingsScreenWithDiagnosticsPreview() {
        val diagnostics = listOf(
            "[UDP] Socket bound to 0.0.0.0:9999",
            "[AOA] Accessory handshake successful (Google WinUSB)",
            "[SYNC] Clock drift: 0.04ms | Jitter: 0.12ms",
            "[POLL] Input polling rate locked at 1000Hz",
            "[HAPTIC] LRA actuator initialized with 25 perceptual bands",
            "[NET] Ping round-trip: 0.42ms"
        )

        NEXPADTheme {
            SettingsScreenContent(
                isConnected = true,
                diagnosticLog = diagnostics,
                ipAddress = "192.168.1.100",
                isRgbEnabled = true,
                rightStickCameraMode = true,
                cameraSensitivity = 1.8f,
                buttonHapticsEnabled = true,
                vibrateOfflineEnabled = true,
                hapticClickStrength = 0.8f,
                hapticStyle = HapticFeedbackHelper.STYLE_HEAVY,
                rumbleIntensity = 0.75f,
                rumbleMode = "smart"
            )
        }
    }

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF08090C, widthDp = 840, heightDp = 390)
    @Composable
    fun settingsScreenLandscapePreview() {
        NEXPADTheme {
            SettingsScreenContent(
                isConnected = true,
                ipAddress = "192.168.1.100",
                isRgbEnabled = true,
                rightStickCameraMode = true,
                cameraSensitivity = 2.0f,
                buttonHapticsEnabled = true,
                vibrateOfflineEnabled = true,
                hapticClickStrength = 0.6f,
                hapticStyle = HapticFeedbackHelper.STYLE_SOFT,
                rumbleIntensity = 0.9f,
                rumbleMode = "max"
            )
        }
    }
}
