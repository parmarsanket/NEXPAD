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
import com.sanket.tools.nexpad.ui.components.controller.bumper.*
import com.sanket.tools.nexpad.ui.components.controller.trigger.*
import com.sanket.tools.nexpad.ui.studio.components.StaticDefaultButtonPreview

/**
 * Compose Preview Screenshot Tests for NEXPAD Shoulder Bumpers & Analog Triggers.
 */
class BumpersTriggersScreenshotTest {

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF0D0E11)
    @Composable
    fun realisticBumpersPreview() {
        Box(
            modifier = Modifier
                .background(Color(0xFF0D0E11))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                StaticDefaultButtonPreview(controlKey = "LB")
                StaticDefaultButtonPreview(controlKey = "RB")
            }
        }
    }

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF0D0E11)
    @Composable
    fun realisticTriggersPreview() {
        Box(
            modifier = Modifier
                .background(Color(0xFF0D0E11))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                StaticDefaultButtonPreview(controlKey = "LT")
                StaticDefaultButtonPreview(controlKey = "RT")
            }
        }
    }

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF0D0E11)
    @Composable
    fun bumpersShowcasePreview() {
        Box(
            modifier = Modifier
                .background(Color(0xFF0D0E11))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StaticArcBumper(key = "LB")
                    StaticLedBumper(key = "LB")
                    StaticPeekBumper(key = "LB")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StaticRibbedBumper(key = "LB")
                    StaticUnderglowBumper(key = "LB")
                    StaticTubeBumper(key = "LB")
                    StaticFlipBumper(key = "LB")
                }
            }
        }
    }

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF0D0E11)
    @Composable
    fun triggersShowcasePreview() {
        Box(
            modifier = Modifier
                .background(Color(0xFF0D0E11))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StaticDialTrigger(key = "LT")
                    StaticLiquidOrbTrigger(key = "LT")
                    StaticVuSlabsTrigger(key = "LT")
                    StaticTargetTrigger(key = "LT")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StaticSliderTrigger(key = "LT")
                    StaticNeedleTrigger(key = "LT")
                    StaticTestTubeTrigger(key = "LT")
                    StaticBloomTrigger(key = "LT")
                }
            }
        }
    }
}
