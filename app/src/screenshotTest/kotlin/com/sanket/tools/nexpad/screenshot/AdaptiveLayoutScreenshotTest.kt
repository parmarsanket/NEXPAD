package com.sanket.tools.nexpad.screenshot

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.tools.screenshot.PreviewTest
import com.sanket.tools.nexpad.category.ControllerLabelStyle
import com.sanket.tools.nexpad.ui.layout.adaptiveLayoutSpec
import com.sanket.tools.nexpad.ui.studio.components.StaticDefaultButtonPreview
import com.sanket.tools.nexpad.ui.theme.NeonPalette
import com.sanket.tools.nexpad.ui.theme.NEXPADTheme

/**
 * Compose Preview Screenshot Tests for NEXPAD Adaptive Layouts across screen dimensions and postures:
 * - Compact Phone Portrait (360x740 dp)
 * - Medium Phone Landscape / Short Screen Gamepad (840x390 dp)
 * - Expanded Tablet Landscape (1200x800 dp)
 * - Foldable Unfolded (680x800 dp)
 */
class AdaptiveLayoutScreenshotTest {

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF08090C, widthDp = 360, heightDp = 740)
    @Composable
    fun compactPhonePortraitPreview() {
        NEXPADTheme {
            AdaptiveScreenHarness()
        }
    }

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF08090C, widthDp = 840, heightDp = 390)
    @Composable
    fun mediumPhoneLandscapePreview() {
        NEXPADTheme {
            AdaptiveScreenHarness()
        }
    }

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF08090C, widthDp = 1200, heightDp = 800)
    @Composable
    fun expandedTabletLandscapePreview() {
        NEXPADTheme {
            AdaptiveScreenHarness()
        }
    }

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF08090C, widthDp = 680, heightDp = 800)
    @Composable
    fun foldableUnfoldedPortraitPreview() {
        NEXPADTheme {
            AdaptiveScreenHarness()
        }
    }

    @Composable
    private fun AdaptiveScreenHarness() {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF08090C))
        ) {
            val screenWidth = maxWidth
            val screenHeight = maxHeight
            val spec = adaptiveLayoutSpec(screenWidth, screenHeight)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = spec.horizontalPadding, vertical = spec.verticalPadding),
                verticalArrangement = Arrangement.spacedBy(spec.contentSpacing)
            ) {
                // Adaptive Header Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF13151A), RoundedCornerShape(12.dp))
                        .border(1.dp, NeonPalette.Cyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "NEXPAD ADAPTIVE CONTROLLER",
                                style = MaterialTheme.typography.titleMedium,
                                color = NeonPalette.Cyan,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Posture: ${spec.posture} | WidthClass: ${spec.widthClass} | Two-Pane: ${spec.useTwoPaneLayout}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                        Text(
                            text = "${screenWidth.value.toInt()}x${screenHeight.value.toInt()} dp",
                            color = NeonPalette.Purple,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                // Adaptive Content Layout: Split Two-Pane in Landscape vs Single Column in Portrait
                if (spec.useTwoPaneLayout) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(spec.paneSpacing)
                    ) {
                        // Left Pane: Left Stick + D-Pad
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .background(Color(0xFF0F1116), RoundedCornerShape(12.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                StaticDefaultButtonPreview(controlKey = "LS")
                                StaticDefaultButtonPreview(controlKey = "DPAD")
                            }
                        }

                        // Right Pane: ABXY + Right Stick
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .background(Color(0xFF0F1116), RoundedCornerShape(12.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    StaticDefaultButtonPreview(controlKey = "X", labelStyle = ControllerLabelStyle.XBOX)
                                    StaticDefaultButtonPreview(controlKey = "Y", labelStyle = ControllerLabelStyle.XBOX)
                                    StaticDefaultButtonPreview(controlKey = "A", labelStyle = ControllerLabelStyle.XBOX)
                                    StaticDefaultButtonPreview(controlKey = "B", labelStyle = ControllerLabelStyle.XBOX)
                                }
                                StaticDefaultButtonPreview(controlKey = "RS")
                            }
                        }
                    }
                } else {
                    // Portrait Single Column Stack
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(Color(0xFF0F1116), RoundedCornerShape(12.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            StaticDefaultButtonPreview(controlKey = "DPAD")
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(Color(0xFF0F1116), RoundedCornerShape(12.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                StaticDefaultButtonPreview(controlKey = "X", labelStyle = ControllerLabelStyle.XBOX)
                                StaticDefaultButtonPreview(controlKey = "Y", labelStyle = ControllerLabelStyle.XBOX)
                                StaticDefaultButtonPreview(controlKey = "A", labelStyle = ControllerLabelStyle.XBOX)
                                StaticDefaultButtonPreview(controlKey = "B", labelStyle = ControllerLabelStyle.XBOX)
                            }
                        }
                    }
                }
            }
        }
    }
}
