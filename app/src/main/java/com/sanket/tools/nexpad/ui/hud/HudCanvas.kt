package com.sanket.tools.nexpad.ui.hud

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.category.ControllerLabelStyle
import com.sanket.tools.nexpad.model.HudElement
import com.sanket.tools.nexpad.runtime.plugin.RemoteComponentRegistry
import com.sanket.tools.nexpad.runtime.registry.ComponentRegistry
import com.sanket.tools.nexpad.runtime.registry.NativeComponentRegistry
import com.sanket.tools.nexpad.ui.components.controller.ControllerElementRenderer
import com.sanket.tools.nexpad.ui.theme.NeonPalette
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel
import kotlin.math.roundToInt

/**
 * High-performance Touch Canvas for the HUD Editor.
 * Uses center-based coordinate layout and Modifier.graphicsLayer for zero-recomposition GPU dragging.
 */
@Composable
fun HudCanvas(
    elements: Map<String, HudElement>,
    selectedControl: String?,
    screenWidthPx: Float,
    screenHeightPx: Float,
    isRgbEnabled: Boolean,
    dummyViewModel: GamepadViewModel,
    onSelect: (String) -> Unit,
    onDragDelta: (String, Float, Float) -> Unit,
    modifier: Modifier = Modifier,
    labelStyle: ControllerLabelStyle = ControllerLabelStyle.XBOX
) {
    val context = LocalContext.current
    val registry = remember { ComponentRegistry.getInstance(context) }
    val remoteRegistry = remember { RemoteComponentRegistry.getInstance(context) }
    val installedComponents by registry.installedComponents.collectAsState()
    val remoteDocs by remoteRegistry.loadedComponents.collectAsState()
    val density = LocalDensity.current.density
    val widthDp = screenWidthPx / density
    val heightDp = screenHeightPx / density
    val responsiveScale = com.sanket.tools.nexpad.model.LayoutMetrics.calculateResponsiveScale(widthDp, heightDp)

    val currentOnSelect by rememberUpdatedState(onSelect)
    val currentOnDragDelta by rememberUpdatedState(onDragDelta)

    Box(modifier = modifier) {
        elements.forEach { (controlKey, element) ->
            key(controlKey) {
                val isSelected = selectedControl == controlKey
                val isJoystick = controlKey.equals("LS", ignoreCase = true) ||
                        controlKey.equals("RS", ignoreCase = true) ||
                        element.categoryTitle.equals("JOYSTICK", ignoreCase = true)

                // 1. Hitbox Box for BOX mode (white border 50% alpha, visible in HUD screen for setting joystick)
                if (isJoystick && element.transform.joystickMode == "BOX") {
                    val boxSizeDp = 150.dp * element.transform.hitboxScale * element.transform.scale * responsiveScale
                    Box(
                        modifier = Modifier
                            .layout { measurable, childConstraints ->
                                val placeable = measurable.measure(childConstraints)
                                val x = (element.transform.xRatio * screenWidthPx - placeable.width / 2f).roundToInt()
                                val y = (element.transform.yRatio * screenHeightPx - placeable.height / 2f).roundToInt()
                                layout(placeable.width, placeable.height) {
                                    placeable.placeRelative(x, y)
                                }
                            }
                            .size(boxSizeDp)
                            .border(
                                width = 1.5.dp,
                                color = Color.White.copy(alpha = 0.50f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .background(Color.White.copy(alpha = 0.04f))
                    ) {
                        Text(
                            text = "HITBOX ${(element.transform.hitboxScale * 100).roundToInt()}%",
                            color = Color.White.copy(alpha = 0.50f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 6.dp)
                        )
                    }
                }

                // 2. Full-Screen Zone Indicator for FULL mode (visible when selected in HUD editor)
                if (isJoystick && element.transform.joystickMode == "FULL" && isSelected) {
                    val isLeftStick = controlKey.equals("LS", ignoreCase = true)
                    val density = LocalDensity.current.density
                    val halfWidthDp = (screenWidthPx / 2f / density).dp
                    val fullHeightDp = (screenHeightPx / density).dp
                    Box(
                        modifier = Modifier
                            .layout { measurable, childConstraints ->
                                val placeable = measurable.measure(childConstraints)
                                val x = if (isLeftStick) 0 else (screenWidthPx / 2f).roundToInt()
                                layout(placeable.width, placeable.height) {
                                    placeable.placeRelative(x, 0)
                                }
                            }
                            .size(width = halfWidthDp, height = fullHeightDp)
                            .border(
                                width = 1.dp,
                                color = NeonPalette.Purple.copy(alpha = 0.45f)
                            )
                            .background(NeonPalette.Purple.copy(alpha = 0.05f))
                    ) {
                        Text(
                            text = if (isLeftStick) "◄ FULL SCREEN LEFT ZONE" else "FULL SCREEN RIGHT ZONE ►",
                            color = NeonPalette.Purple.copy(alpha = 0.70f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 16.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .layout { measurable, childConstraints ->
                            val placeable = measurable.measure(childConstraints)
                            val x = (element.transform.xRatio * screenWidthPx - placeable.width / 2f).roundToInt()
                            val y = (element.transform.yRatio * screenHeightPx - placeable.height / 2f).roundToInt()
                            layout(placeable.width, placeable.height) {
                                placeable.placeRelative(x, y)
                            }
                        }
                        // GPU Layer: Zero recomposition during scaling, opacity changes, and rotation
                        .graphicsLayer {
                            scaleX = element.transform.scale * responsiveScale
                            scaleY = element.transform.scale * responsiveScale
                            alpha = element.transform.opacity
                        }
                ) {
                    // Controller Element Visual Renderer
                    ControllerElementRenderer(
                        key = controlKey,
                        isConnected = false,
                        isRgbEnabled = isRgbEnabled,
                        viewModel = dummyViewModel,
                        onVibrate = {},
                        customComponentId = element.skinId,
                        sensitivity = element.transform.sensitivity,
                        heightScale = element.transform.heightScale,
                        isFlipped = element.transform.isFlipped,
                        isLocked = element.transform.isLocked,
                        labelStyle = labelStyle
                    )

                    // Broken asset warning badge when custom component is missing
                    val isCustomSkin = element.skinId != null && !NativeComponentRegistry.isNativeBuiltin(element.skinId)
                    val isMissingSkin = remember(element.skinId, installedComponents, remoteDocs) {
                        if (!isCustomSkin) false
                        else {
                            installedComponents.none { it.manifest.id == element.skinId } &&
                                    remoteDocs.none { it.manifest.id == element.skinId }
                        }
                    }
                    if (isMissingSkin) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFE65100),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(2.dp)
                                .size(18.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    "⚠",
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Selection Box Indicator
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(if (isSelected) NeonPalette.Cyan.copy(alpha = 0.25f) else Color.Transparent)
                            .border(
                                width = if (isSelected) 2.dp else 0.dp,
                                color = if (isSelected) NeonPalette.Cyan else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                    )

                    // Touch & Drag Interceptor (Isolated from button tap events)
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .pointerInput(controlKey, screenWidthPx, screenHeightPx) {
                                detectTapGestures(
                                    onTap = { currentOnSelect(controlKey) }
                                )
                            }
                            .pointerInput(controlKey, screenWidthPx, screenHeightPx) {
                                detectDragGestures(
                                    onDragStart = { currentOnSelect(controlKey) },
                                    onDragEnd = {},
                                    onDragCancel = {}
                                ) { change, dragAmount ->
                                    change.consume()
                                    val dx = dragAmount.x / screenWidthPx
                                    val dy = dragAmount.y / screenHeightPx
                                    currentOnDragDelta(controlKey, dx, dy)
                                }
                            }
                    )
                }
            }
        }
    }
}
