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
 * - Custom tuned configuration in Portrait
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
                activeProfileName = "Standard Elite",
                isRgbEnabled = true,
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
        NEXPADTheme {
            SettingsScreenContent(
                isConnected = true,
                activeProfileName = "Cyber FPS",
                isRgbEnabled = true,
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
                activeProfileName = "Sim Racing",
                isRgbEnabled = true,
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
