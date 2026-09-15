package com.sanket.tools.nexpad.ui.studio.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import com.sanket.tools.nexpad.category.CategoryManager
import com.sanket.tools.nexpad.runtime.engine.NxprcCanvasRenderer
import com.sanket.tools.nexpad.runtime.engine.NxpComposeInterpreter
import com.sanket.tools.nexpad.runtime.model.NexPadControl
import com.sanket.tools.nexpad.runtime.model.NoOpInputTarget
import com.sanket.tools.nexpad.runtime.model.NxpComponentDef
import com.sanket.tools.nexpad.runtime.plugin.RemoteComponentRegistry
import com.sanket.tools.nexpad.ui.studio.model.ButtonStudioMode
import com.sanket.tools.nexpad.ui.studio.model.ButtonStudioType
import com.sanket.tools.nexpad.ui.studio.model.resolveButtonSourceType
import com.sanket.tools.nexpad.ui.theme.NeonPalette

/**
 * Minimalist, ultra-clean Grid Card displaying a high-performance button preview tile.
 * All detailed metadata, control assignments, and action buttons (Apply, HUD, Export, Delete)
 * are hosted in the SandboxPreviewModal opened upon clicking this card.
 */
@Composable
fun StudioGridCard(
    def: NxpComponentDef,
    isAppliedToActiveProfile: Boolean = false,
    isSelectedInBuilder: Boolean = false,
    mode: ButtonStudioMode = ButtonStudioMode.MANAGE,
    scale: Float = 0.50f, // <-- Adjust size from 0.0f to 1.0f according to your preference
    onClick: () -> Unit,
    onToggleSelectInBuilder: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val type = remember(def.manifest.id) { resolveButtonSourceType(def) }

    val isHighlight = isAppliedToActiveProfile || (mode == ButtonStudioMode.SELECTION && isSelectedInBuilder)
    val borderColor = if (isHighlight) NeonPalette.Cyan else Color.White.copy(alpha = 0.08f)
    val borderWidth = if (isHighlight) 1.5.dp else 1.dp

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(borderWidth, borderColor, RoundedCornerShape(14.dp))
            .clickable {
                if (mode == ButtonStudioMode.SELECTION) {
                    onToggleSelectInBuilder()
                } else {
                    onClick()
                }
            },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1322))
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(Color(0xFF040810)),
            contentAlignment = Alignment.Center
        ) {
            // Selection Mode Indicator Badge
            if (mode == ButtonStudioMode.SELECTION && isSelectedInBuilder) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(NeonPalette.Cyan.copy(alpha = 0.2f))
                        .border(1.dp, NeonPalette.Cyan, RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = NeonPalette.Cyan,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            // High-Performance Static Button Preview - Scaled according to 'scale' (0.0f to 1.0f)
            val controlKey = def.manifest.defaultControl.uppercase()
            val availableDim = minOf(maxWidth.value, maxHeight.value)
            
            // Adjust scale factor (0.0f to 1.0f): e.g. 0.75f = compact, 0.85f = balanced, 1.0f = full tile edge-to-edge
            val targetDim = (if (availableDim > 0f) availableDim else 92f) * scale

            // Centralized intrinsic dimension from protocol CategoryManager
            val intrinsicMaxDim = if (type == ButtonStudioType.DEFAULT) {
                CategoryManager.resolveIntrinsicMaxDim(controlKey)
            } else {
                CategoryManager.resolveIntrinsicMaxDim(controlKey, def.size.widthDp, def.size.heightDp)
            }

            val previewScale = targetDim / intrinsicMaxDim

            Box(
                modifier = Modifier.graphicsLayer {
                    scaleX = previewScale
                    scaleY = previewScale
                },
                contentAlignment = Alignment.Center
            ) {
                if (type == ButtonStudioType.DEFAULT) {
                    StaticDefaultButtonPreview(controlKey = controlKey)
                } else if (type == ButtonStudioType.REMOTE_COMPOSE) {
                    val remoteRegistry = remember { RemoteComponentRegistry.getInstance(context) }
                    val doc = remember(def.manifest.id) { remoteRegistry.getComponent(def.manifest.id) }
                    if (doc != null) {
                        val targetControl = when {
                            controlKey.equals("LS", ignoreCase = true) || controlKey.equals("L3", ignoreCase = true) -> NexPadControl.Stick(isLeft = true)
                            controlKey.equals("RS", ignoreCase = true) || controlKey.equals("R3", ignoreCase = true) -> NexPadControl.Stick(isLeft = false)
                            doc.manifest.category.equals("JOYSTICK", ignoreCase = true) ->
                                NexPadControl.Stick(isLeft = !controlKey.contains("R", ignoreCase = true))
                            controlKey.equals("LT", ignoreCase = true) || controlKey.equals("RT", ignoreCase = true) -> NexPadControl.Trigger(controlKey)
                            doc.manifest.category.equals("TRIGGER", ignoreCase = true) ->
                                NexPadControl.Trigger(controlKey)
                            else -> NexPadControl.Button(controlKey)
                        }
                        NxprcCanvasRenderer(
                            document = doc,
                            assignedControl = targetControl,
                            isConnected = false,
                            inputTarget = NoOpInputTarget,
                            isInteractive = false
                        )
                    } else {
                        val control = when {
                            def.manifest.category.equals("JOYSTICK", ignoreCase = true) ->
                                NexPadControl.Stick(isLeft = !controlKey.contains("R"))
                            def.manifest.category.equals("TRIGGER", ignoreCase = true) ->
                                NexPadControl.Trigger(key = controlKey)
                            else ->
                                NexPadControl.Button(controlKey)
                        }
                        NxpComposeInterpreter(
                            definition = def,
                            assignedControl = control,
                            isConnected = false,
                            inputTarget = NoOpInputTarget,
                            isInteractive = false
                        )
                    }
                } else {
                    val control = when {
                        def.manifest.category.equals("JOYSTICK", ignoreCase = true) ->
                            NexPadControl.Stick(isLeft = !controlKey.contains("R"))
                        def.manifest.category.equals("TRIGGER", ignoreCase = true) ->
                            NexPadControl.Trigger(key = controlKey)
                        else ->
                            NexPadControl.Button(controlKey)
                    }
                    NxpComposeInterpreter(
                        definition = def,
                        assignedControl = control,
                        isConnected = false,
                        inputTarget = NoOpInputTarget,
                        isInteractive = false
                    )
                }
            }
        }
    }
}

