package com.sanket.tools.nexpad.screenshot

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.sanket.tools.nexpad.category.ControlKey
import com.sanket.tools.nexpad.category.ControllerLabelStyle
import com.sanket.tools.nexpad.model.HudElement
import com.sanket.tools.nexpad.model.LayoutSkin
import com.sanket.tools.nexpad.model.LayoutTransform
import com.sanket.tools.nexpad.ui.hud.HudDockedInspector
import com.sanket.tools.nexpad.ui.hud.HudTopBar
import com.sanket.tools.nexpad.ui.studio.components.StaticDefaultButtonPreview
import com.sanket.tools.nexpad.ui.theme.NEXPADTheme
import com.sanket.tools.nexpad.ui.theme.NeonPalette
import kotlin.math.roundToInt

/**
 * Compose Preview Screenshot Tests for HUD Editor Screen and its relative components:
 * - Full HUD Screen in Landscape mode with TopBar & Canvas
 * - Full HUD Screen with Docked Inspector active
 * - Relative components: HudTopBar (Default & Unsaved states), HudDockedInspector
 */
class HudScreenScreenshotTest {

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF08090C, widthDp = 840, heightDp = 390)
    @Composable
    fun hudScreenFullEditorPreview() {
        NEXPADTheme {
            HudEditorHarness(showInspector = false)
        }
    }

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF08090C, widthDp = 840, heightDp = 390)
    @Composable
    fun hudScreenWithDockedInspectorPreview() {
        NEXPADTheme {
            HudEditorHarness(showInspector = true)
        }
    }

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF08090C, widthDp = 840, heightDp = 64)
    @Composable
    fun hudTopBarUnsavedPreview() {
        NEXPADTheme {
            HudTopBar(
                profileName = "Custom Cyber Layout",
                isDefault = false,
                hasUnsavedChanges = true,
                onBack = {},
                onOpenPalette = {},
                onSave = {},
                labelStyle = ControllerLabelStyle.XBOX
            )
        }
    }

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF08090C, widthDp = 840, heightDp = 64)
    @Composable
    fun hudTopBarDefaultPresetPreview() {
        NEXPADTheme {
            HudTopBar(
                profileName = "Standard Elite",
                isDefault = true,
                hasUnsavedChanges = false,
                onBack = {},
                onOpenPalette = {},
                onSave = {},
                labelStyle = ControllerLabelStyle.PLAYSTATION
            )
        }
    }

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFF08090C, widthDp = 700, heightDp = 130)
    @Composable
    fun hudDockedInspectorAlonePreview() {
        val element = HudElement(
            controlKey = ControlKey.A.key,
            transform = LayoutTransform(xRatio = 0.85f, yRatio = 0.65f, scale = 1.0f, opacity = 1.0f)
        )
        val skins = listOf<LayoutSkin>(LayoutSkin.NativeDefault)

        NEXPADTheme {
            Box(
                modifier = Modifier
                    .background(Color(0xFF08090C))
                    .padding(8.dp)
            ) {
                HudDockedInspector(
                    element = element,
                    compatibleSkins = skins,
                    screenWidthPx = 1920f,
                    screenHeightPx = 1080f,
                    onClose = {},
                    onNudge = { _, _ -> },
                    onScaleChange = {},
                    onOpacityChange = {},
                    onCycleSkin = {},
                    onOpenStudio = {},
                    onResetPos = {},
                    onRemove = {}
                )
            }
        }
    }

    @Composable
    private fun HudEditorHarness(showInspector: Boolean) {
        val selectedElement = HudElement(
            controlKey = ControlKey.A.key,
            transform = LayoutTransform(xRatio = 0.82f, yRatio = 0.68f, scale = 1.0f, opacity = 1.0f)
        )
        val sampleSkins = listOf<LayoutSkin>(LayoutSkin.NativeDefault)

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF08090C))
        ) {
            val screenW = maxWidth
            val screenH = maxHeight

            // HUD Top Bar
            HudTopBar(
                profileName = "Esports Tactical",
                isDefault = false,
                hasUnsavedChanges = showInspector,
                onBack = {},
                onOpenPalette = {},
                onSave = {},
                labelStyle = ControllerLabelStyle.XBOX,
                modifier = Modifier.align(Alignment.TopCenter)
            )

            // Canvas: Left Stick
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (0.18f * screenW.toPx() - 65.dp.toPx()).roundToInt(),
                            (0.55f * screenH.toPx() - 65.dp.toPx()).roundToInt()
                        )
                    }
                    .size(130.dp),
                contentAlignment = Alignment.Center
            ) {
                StaticDefaultButtonPreview(controlKey = ControlKey.LS.key)
            }

            // Canvas: D-Pad
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (0.35f * screenW.toPx() - 70.dp.toPx()).roundToInt(),
                            (0.70f * screenH.toPx() - 70.dp.toPx()).roundToInt()
                        )
                    }
                    .size(140.dp),
                contentAlignment = Alignment.Center
            ) {
                StaticDefaultButtonPreview(controlKey = ControlKey.DPAD.key)
            }

            // Canvas: Right Stick
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (0.65f * screenW.toPx() - 65.dp.toPx()).roundToInt(),
                            (0.70f * screenH.toPx() - 65.dp.toPx()).roundToInt()
                        )
                    }
                    .size(130.dp),
                contentAlignment = Alignment.Center
            ) {
                StaticDefaultButtonPreview(controlKey = ControlKey.RS.key)
            }

            // Canvas: Action Button A (Selected with Cyan Halo)
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (0.82f * screenW.toPx() - 40.dp.toPx()).roundToInt(),
                            (0.68f * screenH.toPx() - 40.dp.toPx()).roundToInt()
                        )
                    }
                    .size(80.dp)
                    .border(2.dp, NeonPalette.Cyan, RoundedCornerShape(12.dp))
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                StaticDefaultButtonPreview(controlKey = ControlKey.A.key)
            }

            // Canvas: Action Button B
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (0.91f * screenW.toPx() - 40.dp.toPx()).roundToInt(),
                            (0.52f * screenH.toPx() - 40.dp.toPx()).roundToInt()
                        )
                    }
                    .size(80.dp),
                contentAlignment = Alignment.Center
            ) {
                StaticDefaultButtonPreview(controlKey = ControlKey.B.key)
            }

            // Canvas: Action Button X
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (0.73f * screenW.toPx() - 40.dp.toPx()).roundToInt(),
                            (0.52f * screenH.toPx() - 40.dp.toPx()).roundToInt()
                        )
                    }
                    .size(80.dp),
                contentAlignment = Alignment.Center
            ) {
                StaticDefaultButtonPreview(controlKey = ControlKey.X.key)
            }

            // Canvas: Action Button Y
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (0.82f * screenW.toPx() - 40.dp.toPx()).roundToInt(),
                            (0.36f * screenH.toPx() - 40.dp.toPx()).roundToInt()
                        )
                    }
                    .size(80.dp),
                contentAlignment = Alignment.Center
            ) {
                StaticDefaultButtonPreview(controlKey = ControlKey.Y.key)
            }

            // Docked Inspector at bottom
            if (showInspector) {
                HudDockedInspector(
                    element = selectedElement,
                    compatibleSkins = sampleSkins,
                    screenWidthPx = 840f,
                    screenHeightPx = 390f,
                    onClose = {},
                    onNudge = { _, _ -> },
                    onScaleChange = {},
                    onOpacityChange = {},
                    onCycleSkin = {},
                    onOpenStudio = {},
                    onResetPos = {},
                    onRemove = {},
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}
