package com.sanket.tools.nexpad.screenshot

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.sanket.tools.nexpad.model.getDefaultLayoutProfiles
import com.sanket.tools.nexpad.ui.GamepadScreenContent
import com.sanket.tools.nexpad.ui.studio.components.StaticDefaultButtonPreview
import com.sanket.tools.nexpad.ui.theme.NEXPADTheme

/**
 * Compose Preview Screenshot Tests for [GamepadScreenContent] and gamepad controls:
 * - Standard Elite Xbox Gamepad layout (Disconnected & Connected)
 * - Standard PlayStation Layout (Cross, Circle, Square, Triangle glyphs)
 * - Cyber FPS tactical layout with macro paddles
 */
class GamepadScreenScreenshotTest {

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF08090C, widthDp = 840, heightDp = 390)
    @Composable
    fun gamepadScreenStandardEliteXboxIdlePreview() {
        val profile = getDefaultLayoutProfiles().first { it.name.contains("Standard", ignoreCase = true) }
        NEXPADTheme {
            GamepadScreenContent(
                profile = profile,
                isConnected = false,
                renderElement = { key, _ ->
                    StaticDefaultButtonPreview(
                        controlKey = key,
                        labelStyle = profile.controllerLabelStyle
                    )
                }
            )
        }
    }

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF08090C, widthDp = 840, heightDp = 390)
    @Composable
    fun gamepadScreenStandardEliteXboxConnectedPreview() {
        val profile = getDefaultLayoutProfiles().first { it.name.contains("Standard", ignoreCase = true) }
        NEXPADTheme {
            GamepadScreenContent(
                profile = profile,
                isConnected = true,
                renderElement = { key, _ ->
                    StaticDefaultButtonPreview(
                        controlKey = key,
                        labelStyle = profile.controllerLabelStyle
                    )
                }
            )
        }
    }

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF08090C, widthDp = 840, heightDp = 390)
    @Composable
    fun gamepadScreenPlayStationStylePreview() {
        val psProfile = getDefaultLayoutProfiles().find { it.name.contains("PlayStation", ignoreCase = true) }
            ?: getDefaultLayoutProfiles().first().copy(
                labelStyle = "PLAYSTATION"
            )
        NEXPADTheme {
            GamepadScreenContent(
                profile = psProfile,
                isConnected = true,
                renderElement = { key, _ ->
                    StaticDefaultButtonPreview(
                        controlKey = key,
                        labelStyle = psProfile.controllerLabelStyle
                    )
                }
            )
        }
    }

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF08090C, widthDp = 840, heightDp = 390)
    @Composable
    fun gamepadScreenCyberFpsPreview() {
        val fpsProfile = getDefaultLayoutProfiles().find { it.name.contains("FPS", ignoreCase = true) }
            ?: getDefaultLayoutProfiles().last()
        NEXPADTheme {
            GamepadScreenContent(
                profile = fpsProfile,
                isConnected = true,
                renderElement = { key, _ ->
                    StaticDefaultButtonPreview(
                        controlKey = key,
                        labelStyle = fpsProfile.controllerLabelStyle
                    )
                }
            )
        }
    }
}
