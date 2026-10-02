package com.sanket.tools.nexpad.screenshot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.tools.screenshot.PreviewTest
import com.sanket.tools.nexpad.category.ControllerLabelStyle
import com.sanket.tools.nexpad.ui.studio.components.StaticDefaultButtonPreview
import com.sanket.tools.nexpad.ui.theme.NeonPalette
import com.sanket.tools.nexpad.ui.theme.NEXPADTheme

/**
 * Compose Preview Screenshot Tests for NEXPAD Typography, Font Scaling, and Label Styles.
 * Verifies font scale 1.0x vs 1.5x (accessibility scaling) to prevent UI truncation.
 */
class FontAndTypographyScreenshotTest {

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF0D0E11, fontScale = 1.0f, widthDp = 500, heightDp = 260)
    @Composable
    fun standardFontScalePreview() {
        NEXPADTheme {
            Box(
                modifier = Modifier
                    .background(Color(0xFF0D0E11))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "NEXPAD ULTRA LOW LATENCY",
                        style = MaterialTheme.typography.titleLarge,
                        color = NeonPalette.Cyan,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Real One-Way Input Lag: 0.8 ms | Polling Rate: 1000 Hz",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        StaticDefaultButtonPreview(controlKey = "X", labelStyle = ControllerLabelStyle.XBOX)
                        StaticDefaultButtonPreview(controlKey = "Y", labelStyle = ControllerLabelStyle.XBOX)
                        StaticDefaultButtonPreview(controlKey = "A", labelStyle = ControllerLabelStyle.XBOX)
                        StaticDefaultButtonPreview(controlKey = "B", labelStyle = ControllerLabelStyle.XBOX)
                    }
                }
            }
        }
    }

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF0D0E11, fontScale = 1.5f, widthDp = 500, heightDp = 320)
    @Composable
    fun scaledFontAccessibilityPreview() {
        NEXPADTheme {
            Box(
                modifier = Modifier
                    .background(Color(0xFF0D0E11))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "NEXPAD ULTRA LOW LATENCY (1.5x FONT)",
                        style = MaterialTheme.typography.titleLarge,
                        color = NeonPalette.Cyan,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Real One-Way Input Lag: 0.8 ms | Polling Rate: 1000 Hz",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        StaticDefaultButtonPreview(controlKey = "X", labelStyle = ControllerLabelStyle.XBOX)
                        StaticDefaultButtonPreview(controlKey = "Y", labelStyle = ControllerLabelStyle.XBOX)
                        StaticDefaultButtonPreview(controlKey = "A", labelStyle = ControllerLabelStyle.XBOX)
                        StaticDefaultButtonPreview(controlKey = "B", labelStyle = ControllerLabelStyle.XBOX)
                    }
                }
            }
        }
    }

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF0D0E11, widthDp = 500, heightDp = 300)
    @Composable
    fun controllerLabelStylesMatrixPreview() {
        NEXPADTheme {
            Box(
                modifier = Modifier
                    .background(Color(0xFF0D0E11))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Xbox (ABXY)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("XBOX STYLE", color = NeonPalette.Cyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StaticDefaultButtonPreview(controlKey = "X", labelStyle = ControllerLabelStyle.XBOX)
                            StaticDefaultButtonPreview(controlKey = "Y", labelStyle = ControllerLabelStyle.XBOX)
                            StaticDefaultButtonPreview(controlKey = "A", labelStyle = ControllerLabelStyle.XBOX)
                            StaticDefaultButtonPreview(controlKey = "B", labelStyle = ControllerLabelStyle.XBOX)
                        }
                    }

                    // PlayStation (Symbols)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("PLAYSTATION STYLE", color = NeonPalette.Purple, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StaticDefaultButtonPreview(controlKey = "X", labelStyle = ControllerLabelStyle.PLAYSTATION)
                            StaticDefaultButtonPreview(controlKey = "Y", labelStyle = ControllerLabelStyle.PLAYSTATION)
                            StaticDefaultButtonPreview(controlKey = "A", labelStyle = ControllerLabelStyle.PLAYSTATION)
                            StaticDefaultButtonPreview(controlKey = "B", labelStyle = ControllerLabelStyle.PLAYSTATION)
                        }
                    }
                }
            }
        }
    }
}
