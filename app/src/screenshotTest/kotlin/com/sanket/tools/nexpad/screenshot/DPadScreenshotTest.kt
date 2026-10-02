package com.sanket.tools.nexpad.screenshot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.sanket.tools.nexpad.ui.studio.components.StaticCapsulesDPad
import com.sanket.tools.nexpad.ui.studio.components.StaticDefaultButtonPreview
import com.sanket.tools.nexpad.ui.studio.components.StaticDiscDPad
import com.sanket.tools.nexpad.ui.studio.components.StaticFourLensesDPad
import com.sanket.tools.nexpad.ui.studio.components.StaticLensDPad
import com.sanket.tools.nexpad.ui.studio.components.StaticMetaballsDPad
import com.sanket.tools.nexpad.ui.studio.components.StaticRailsDPad

/**
 * Compose Preview Screenshot Tests for NEXPAD D-Pad Directional Pad Variants.
 */
class DPadScreenshotTest {

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF0D0E11)
    @Composable
    fun realisticDPadPreview() {
        Box(
            modifier = Modifier
                .background(Color(0xFF0D0E11))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            StaticDefaultButtonPreview(controlKey = "DPAD")
        }
    }

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF0D0E11)
    @Composable
    fun lensDPadPreview() {
        Box(
            modifier = Modifier
                .background(Color(0xFF0D0E11))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            StaticLensDPad()
        }
    }

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF0D0E11)
    @Composable
    fun fourLensesDPadPreview() {
        Box(
            modifier = Modifier
                .background(Color(0xFF0D0E11))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            StaticFourLensesDPad()
        }
    }

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF0D0E11)
    @Composable
    fun discDPadPreview() {
        Box(
            modifier = Modifier
                .background(Color(0xFF0D0E11))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            StaticDiscDPad()
        }
    }

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF0D0E11)
    @Composable
    fun capsulesDPadPreview() {
        Box(
            modifier = Modifier
                .background(Color(0xFF0D0E11))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            StaticCapsulesDPad()
        }
    }

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF0D0E11)
    @Composable
    fun metaballsDPadPreview() {
        Box(
            modifier = Modifier
                .background(Color(0xFF0D0E11))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            StaticMetaballsDPad()
        }
    }

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF0D0E11)
    @Composable
    fun railsDPadPreview() {
        Box(
            modifier = Modifier
                .background(Color(0xFF0D0E11))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            StaticRailsDPad()
        }
    }
}
