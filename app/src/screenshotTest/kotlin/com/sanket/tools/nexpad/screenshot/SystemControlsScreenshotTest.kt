package com.sanket.tools.nexpad.screenshot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.sanket.tools.nexpad.ui.components.controller.StaticOrbitHomeButton
import com.sanket.tools.nexpad.ui.studio.components.StaticDefaultButtonPreview

/**
 * Compose Preview Screenshot Tests for NEXPAD System, Macro, Home & Touchpad Controls.
 */
class SystemControlsScreenshotTest {

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF0D0E11)
    @Composable
    fun systemButtonsPreview() {
        Box(
            modifier = Modifier
                .background(Color(0xFF0D0E11))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StaticDefaultButtonPreview(controlKey = "GUIDE")
                StaticDefaultButtonPreview(controlKey = "START")
                StaticDefaultButtonPreview(controlKey = "BACK")
                StaticDefaultButtonPreview(controlKey = "SHARE")
            }
        }
    }

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF0D0E11)
    @Composable
    fun orbitHomeButtonPreview() {
        Box(
            modifier = Modifier
                .background(Color(0xFF0D0E11))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                StaticOrbitHomeButton(isRgbEnabled = true)
                StaticOrbitHomeButton(isRgbEnabled = false)
            }
        }
    }

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF0D0E11)
    @Composable
    fun macroButtonsPreview() {
        Box(
            modifier = Modifier
                .background(Color(0xFF0D0E11))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StaticDefaultButtonPreview(controlKey = "M1")
                StaticDefaultButtonPreview(controlKey = "M2")
                StaticDefaultButtonPreview(controlKey = "M3")
                StaticDefaultButtonPreview(controlKey = "M4")
            }
        }
    }

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF0D0E11)
    @Composable
    fun touchPadPreview() {
        Box(
            modifier = Modifier
                .background(Color(0xFF0D0E11))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                StaticDefaultButtonPreview(controlKey = "LTP")
                StaticDefaultButtonPreview(controlKey = "RTP")
            }
        }
    }
}
