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
import com.sanket.tools.nexpad.category.ControllerLabelStyle
import com.sanket.tools.nexpad.ui.components.controller.StaticCapsulesButton
import com.sanket.tools.nexpad.ui.components.controller.StaticEclipseButton
import com.sanket.tools.nexpad.ui.components.controller.StaticFacetButton
import com.sanket.tools.nexpad.ui.components.controller.StaticFlipButton
import com.sanket.tools.nexpad.ui.components.controller.StaticLiquidButton
import com.sanket.tools.nexpad.ui.components.controller.StaticOrbitButton
import com.sanket.tools.nexpad.ui.components.controller.StaticRippleButton
import com.sanket.tools.nexpad.ui.studio.components.StaticDefaultButtonPreview

/**
 * Compose Preview Screenshot Tests for NEXPAD Face Button Variants.
 */
class FaceButtonsScreenshotTest {

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF0D0E11)
    @Composable
    fun realisticButtonsXboxPreview() {
        Box(
            modifier = Modifier
                .background(Color(0xFF0D0E11))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StaticDefaultButtonPreview(controlKey = "X", labelStyle = ControllerLabelStyle.XBOX)
                StaticDefaultButtonPreview(controlKey = "Y", labelStyle = ControllerLabelStyle.XBOX)
                StaticDefaultButtonPreview(controlKey = "A", labelStyle = ControllerLabelStyle.XBOX)
                StaticDefaultButtonPreview(controlKey = "B", labelStyle = ControllerLabelStyle.XBOX)
            }
        }
    }

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF0D0E11)
    @Composable
    fun realisticButtonsPlayStationPreview() {
        Box(
            modifier = Modifier
                .background(Color(0xFF0D0E11))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StaticDefaultButtonPreview(controlKey = "X", labelStyle = ControllerLabelStyle.PLAYSTATION)
                StaticDefaultButtonPreview(controlKey = "Y", labelStyle = ControllerLabelStyle.PLAYSTATION)
                StaticDefaultButtonPreview(controlKey = "A", labelStyle = ControllerLabelStyle.PLAYSTATION)
                StaticDefaultButtonPreview(controlKey = "B", labelStyle = ControllerLabelStyle.PLAYSTATION)
            }
        }
    }

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF0D0E11)
    @Composable
    fun liquidButtonsPreview() {
        Box(
            modifier = Modifier
                .background(Color(0xFF0D0E11))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StaticLiquidButton(controlKey = "X", labelStyle = ControllerLabelStyle.XBOX)
                StaticLiquidButton(controlKey = "Y", labelStyle = ControllerLabelStyle.XBOX)
                StaticLiquidButton(controlKey = "A", labelStyle = ControllerLabelStyle.XBOX)
                StaticLiquidButton(controlKey = "B", labelStyle = ControllerLabelStyle.XBOX)
            }
        }
    }

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF0D0E11)
    @Composable
    fun facetButtonsPreview() {
        Box(
            modifier = Modifier
                .background(Color(0xFF0D0E11))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StaticFacetButton(controlKey = "X", labelStyle = ControllerLabelStyle.XBOX)
                StaticFacetButton(controlKey = "Y", labelStyle = ControllerLabelStyle.XBOX)
                StaticFacetButton(controlKey = "A", labelStyle = ControllerLabelStyle.XBOX)
                StaticFacetButton(controlKey = "B", labelStyle = ControllerLabelStyle.XBOX)
            }
        }
    }

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF0D0E11)
    @Composable
    fun flipButtonsPreview() {
        Box(
            modifier = Modifier
                .background(Color(0xFF0D0E11))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StaticFlipButton(controlKey = "X", labelStyle = ControllerLabelStyle.XBOX)
                StaticFlipButton(controlKey = "Y", labelStyle = ControllerLabelStyle.XBOX)
                StaticFlipButton(controlKey = "A", labelStyle = ControllerLabelStyle.XBOX)
                StaticFlipButton(controlKey = "B", labelStyle = ControllerLabelStyle.XBOX)
            }
        }
    }

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF0D0E11)
    @Composable
    fun rippleButtonsPreview() {
        Box(
            modifier = Modifier
                .background(Color(0xFF0D0E11))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StaticRippleButton(controlKey = "X", labelStyle = ControllerLabelStyle.XBOX)
                StaticRippleButton(controlKey = "Y", labelStyle = ControllerLabelStyle.XBOX)
                StaticRippleButton(controlKey = "A", labelStyle = ControllerLabelStyle.XBOX)
                StaticRippleButton(controlKey = "B", labelStyle = ControllerLabelStyle.XBOX)
            }
        }
    }


    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF0D0E11)
    @Composable
    fun orbitButtonsPreview() {
        Box(
            modifier = Modifier
                .background(Color(0xFF0D0E11))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StaticOrbitButton(controlKey = "X", labelStyle = ControllerLabelStyle.XBOX)
                StaticOrbitButton(controlKey = "Y", labelStyle = ControllerLabelStyle.XBOX)
                StaticOrbitButton(controlKey = "A", labelStyle = ControllerLabelStyle.XBOX)
                StaticOrbitButton(controlKey = "B", labelStyle = ControllerLabelStyle.XBOX)
            }
        }
    }

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF0D0E11)
    @Composable
    fun capsulesButtonsPreview() {
        Box(
            modifier = Modifier
                .background(Color(0xFF0D0E11))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StaticCapsulesButton(controlKey = "X", labelStyle = ControllerLabelStyle.XBOX)
                StaticCapsulesButton(controlKey = "Y", labelStyle = ControllerLabelStyle.XBOX)
                StaticCapsulesButton(controlKey = "A", labelStyle = ControllerLabelStyle.XBOX)
                StaticCapsulesButton(controlKey = "B", labelStyle = ControllerLabelStyle.XBOX)
            }
        }
    }

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF0D0E11)
    @Composable
    fun eclipseButtonsPreview() {
        Box(
            modifier = Modifier
                .background(Color(0xFF0D0E11))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StaticEclipseButton(controlKey = "X", labelStyle = ControllerLabelStyle.XBOX)
                StaticEclipseButton(controlKey = "Y", labelStyle = ControllerLabelStyle.XBOX)
                StaticEclipseButton(controlKey = "A", labelStyle = ControllerLabelStyle.XBOX)
                StaticEclipseButton(controlKey = "B", labelStyle = ControllerLabelStyle.XBOX)
            }
        }
    }
}
