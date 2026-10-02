package com.sanket.tools.nexpad.screenshot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.sanket.tools.nexpad.ui.studio.components.StaticCompassJoystick
import com.sanket.tools.nexpad.ui.studio.components.StaticDefaultButtonPreview
import com.sanket.tools.nexpad.ui.studio.components.StaticFluxJoystick
import com.sanket.tools.nexpad.ui.studio.components.StaticGyroJoystick
import com.sanket.tools.nexpad.ui.studio.components.StaticOrbJoystick
import com.sanket.tools.nexpad.ui.studio.components.StaticSpotlightJoystick

/**
 * Compose Preview Screenshot Tests for NEXPAD Analog Joystick Variants.
 */
class JoysticksScreenshotTest {

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF0D0E11)
    @Composable
    fun realisticJoysticksPreview() {
        Box(
            modifier = Modifier
                .background(Color(0xFF0D0E11))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                StaticDefaultButtonPreview(controlKey = "LS")
                StaticDefaultButtonPreview(controlKey = "RS")
            }
        }
    }

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF0D0E11)
    @Composable
    fun fluxJoysticksPreview() {
        Box(
            modifier = Modifier
                .background(Color(0xFF0D0E11))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                StaticFluxJoystick(isLeft = true)
                StaticFluxJoystick(isLeft = false)
            }
        }
    }

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF0D0E11)
    @Composable
    fun orbJoysticksPreview() {
        Box(
            modifier = Modifier
                .background(Color(0xFF0D0E11))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                StaticOrbJoystick(isLeft = true)
                StaticOrbJoystick(isLeft = false)
            }
        }
    }

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF0D0E11)
    @Composable
    fun compassJoysticksPreview() {
        Box(
            modifier = Modifier
                .background(Color(0xFF0D0E11))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                StaticCompassJoystick(isLeft = true)
                StaticCompassJoystick(isLeft = false)
            }
        }
    }

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF0D0E11)
    @Composable
    fun gyroJoysticksPreview() {
        Box(
            modifier = Modifier
                .background(Color(0xFF0D0E11))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                StaticGyroJoystick(isLeft = true)
                StaticGyroJoystick(isLeft = false)
            }
        }
    }

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF0D0E11)
    @Composable
    fun spotlightJoysticksPreview() {
        Box(
            modifier = Modifier
                .background(Color(0xFF0D0E11))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                StaticSpotlightJoystick(isLeft = true)
                StaticSpotlightJoystick(isLeft = false)
            }
        }
    }
}
