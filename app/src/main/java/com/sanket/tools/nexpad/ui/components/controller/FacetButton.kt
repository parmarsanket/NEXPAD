package com.sanket.tools.nexpad.ui.components.controller

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.category.CategoryManager
import com.sanket.tools.nexpad.category.ControllerLabelStyle
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel

/**
 * Facet (Rotated Gem) ABXY button variant.
 * Faithful reproduction of HTML/CSS #4 Facet:
 * A 64dp square rotated 45° with 14dp rounded corners forming a luminous gem.
 * Features:
 * - High-intensity multi-stage emissive neon bloom around gem perimeter.
 * - Flashing color fill (.f-fill) on press.
 * - Spinning + scaling inner diamond (.f-in): rotates 90° and scales to 0.85 with spring bounce.
 * - Undercut bottom shadow, top crescent glass highlight, counter-rotated upright letter.
 */
@Composable
fun FacetButton(
    key: String,
    buttonColor: Color,
    isConnected: Boolean,
    onVibrate: () -> Unit,
    viewModel: GamepadViewModel,
    isRgbEnabled: Boolean,
    modifier: Modifier = Modifier,
    displayLabel: String? = null
) {
    var isPressed by remember { mutableStateOf(false) }

    val scaleAnim by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1.0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "facet_scale"
    )
    // Inner diamond: rotate 90° + scale 0.85 on press (cubic-bezier .3, 1.5, .5, 1)
    val innerRotAnim by animateFloatAsState(
        targetValue = if (isPressed) 90f else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 360f),
        label = "facet_inner_rot"
    )
    val innerScaleAnim by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1.0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 360f),
        label = "facet_inner_scale"
    )
    val innerAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 0.30f,
        animationSpec = tween(durationMillis = 150),
        label = "facet_inner_alpha"
    )
    val fillAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.25f else 0f,
        animationSpec = tween(durationMillis = 100),
        label = "facet_fill_alpha"
    )
    val rgbBloomAlpha by animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0.50f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "facet_rgb_bloom"
    )

    val currentOnVibrate by rememberUpdatedState(onVibrate)
    val currentViewModel by rememberUpdatedState(viewModel)
    val activeLabel = displayLabel ?: key
    val gemShape = remember { RoundedCornerShape(14.dp) }

    Box(
        modifier = modifier.size(80.dp),
        contentAlignment = Alignment.Center
    ) {
        // ── GEM DIAMOND (64dp square rotated 45°) ──
        Box(
            modifier = Modifier
                .size(64.dp)
                .graphicsLayer {
                    rotationZ = 45f
                    scaleX = scaleAnim
                    scaleY = scaleAnim
                }
                .shadow(
                    elevation = if (isPressed) 2.dp else 6.dp,
                    shape = gemShape,
                    spotColor = Color.Black,
                    ambientColor = Color.Black
                )
                .clip(gemShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF232527), Color(0xFF0C0D0E), Color.Black),
                        center = Offset(0.5f, 0.55f),
                        radius = 180f
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.50f),
                            Color.Black.copy(alpha = 0.85f)
                        )
                    ),
                    shape = gemShape
                )
                .pointerInput(key) {
                    detectTapGestures(
                        onPress = {
                            currentOnVibrate()
                            isPressed = true
                            currentViewModel.updateButton(key, true)
                            tryAwaitRelease()
                            isPressed = false
                            currentViewModel.updateButton(key, false)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // ── 1. FLASH FILL LAYER (.f-fill) ──
            if (fillAlpha > 0.001f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(buttonColor.copy(alpha = fillAlpha))
                )
            }

            // ── 2. INNER SPINNING & SCALING DIAMOND (.f-in) + UNDERCUT SHADOW ──
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val center = Offset(w / 2f, h / 2f)

                // Undercut bottom shadow
                val insetH = h * 0.35f
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = if (isPressed) 0.85f else 0.70f)),
                        startY = h - insetH,
                        endY = h
                    ),
                    topLeft = Offset(0f, h - insetH),
                    size = Size(w, insetH)
                )

                // Inner diamond: inset 11px, corner radius 7px
                val inset = 11.dp.toPx()
                val innerW = w - inset * 2f
                val innerH = h - inset * 2f
                val cr = 7.dp.toPx()

                rotate(innerRotAnim, pivot = center) {
                    scale(scaleX = innerScaleAnim, scaleY = innerScaleAnim, pivot = center) {
                        // Inner diamond glow bloom
                        drawRoundRect(
                            color = buttonColor.copy(alpha = innerAlpha * 0.55f),
                            topLeft = Offset(inset - 1.5.dp.toPx(), inset - 1.5.dp.toPx()),
                            size = Size(innerW + 3.dp.toPx(), innerH + 3.dp.toPx()),
                            cornerRadius = CornerRadius(cr + 1.5.dp.toPx(), cr + 1.5.dp.toPx()),
                            style = Stroke(width = 4.dp.toPx())
                        )
                        // Inner diamond core stroke
                        drawRoundRect(
                            color = buttonColor.copy(alpha = innerAlpha),
                            topLeft = Offset(inset, inset),
                            size = Size(innerW, innerH),
                            cornerRadius = CornerRadius(cr, cr),
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }
                }
            }

            // ── 3. COUNTER-ROTATED GLYPH (rotated -45° to remain upright) ──
            Box(
                modifier = Modifier.graphicsLayer { rotationZ = -45f },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = activeLabel,
                    color = buttonColor,
                    fontWeight = FontWeight.Medium,
                    fontSize = 32.sp,
                    style = androidx.compose.ui.text.TextStyle(
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = buttonColor.copy(alpha = if (isPressed) 0.90f else 0.50f),
                            blurRadius = 14f
                        )
                    )
                )
            }

            // ── 4. SHELL OVERLAY (.lx-ring + .lx-lens) ──
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Neon ring around diamond perimeter (inset 3px, r=11px)
                val ringInset = 3.dp.toPx()
                val ringW = w - ringInset * 2f
                val ringH = h - ringInset * 2f
                val ringCr = 11.dp.toPx()

                // Outer soft bloom
                drawRoundRect(
                    color = buttonColor.copy(alpha = if (isPressed) 0.65f else 0.35f),
                    topLeft = Offset(ringInset - 1f, ringInset - 1f),
                    size = Size(ringW + 2f, ringH + 2f),
                    cornerRadius = CornerRadius(ringCr + 1f, ringCr + 1f),
                    style = Stroke(width = 4.dp.toPx())
                )
                // Core crisp ring
                drawRoundRect(
                    color = buttonColor.copy(alpha = if (isPressed) 1.0f else 0.70f),
                    topLeft = Offset(ringInset, ringInset),
                    size = Size(ringW, ringH),
                    cornerRadius = CornerRadius(ringCr, ringCr),
                    style = Stroke(width = 2.dp.toPx())
                )

                // Top specular highlight
                val lensInset = 1.dp.toPx()
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (isPressed) 0.08f else 0.16f),
                            Color.White.copy(alpha = if (isPressed) 0.02f else 0.05f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = h * 0.40f
                    ),
                    topLeft = Offset(lensInset, lensInset),
                    size = Size(w - lensInset * 2f, h * 0.40f),
                    cornerRadius = CornerRadius(13.dp.toPx(), 13.dp.toPx())
                )
            }
        }
    }
}

/**
 * Static non-interactive preview of FacetButton (idle state).
 */
@Composable
internal fun StaticFacetButton(
    controlKey: String,
    labelStyle: ControllerLabelStyle,
    modifier: Modifier = Modifier
) {
    val displayLabel = CategoryManager.getLabelForStyle(controlKey, labelStyle)
    val buttonColor = when (controlKey.uppercase()) {
        "A" -> Color(0xFF3FD25A)
        "B" -> Color(0xFFE6474E)
        "X" -> Color(0xFF3F8FE0)
        "Y" -> Color(0xFFE0A03F)
        else -> Color(0xFF3FD25A)
    }
    val gemShape = remember { RoundedCornerShape(14.dp) }

    Box(
        modifier = modifier.size(80.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .graphicsLayer { rotationZ = 45f }
                .shadow(
                    elevation = 6.dp,
                    shape = gemShape,
                    spotColor = Color.Black,
                    ambientColor = Color.Black
                )
                .clip(gemShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF232527), Color(0xFF0C0D0E), Color.Black),
                        center = Offset(0.5f, 0.55f),
                        radius = 180f
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.50f),
                            Color.Black.copy(alpha = 0.85f)
                        )
                    ),
                    shape = gemShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                val insetH = h * 0.35f
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.70f)),
                        startY = h - insetH,
                        endY = h
                    ),
                    topLeft = Offset(0f, h - insetH),
                    size = Size(w, insetH)
                )

                // Inner diamond idle with glow bloom
                val inset = 11.dp.toPx()
                val innerW = w - inset * 2f
                val innerH = h - inset * 2f
                val cr = 7.dp.toPx()

                drawRoundRect(
                    color = buttonColor.copy(alpha = 0.30f * 0.55f),
                    topLeft = Offset(inset - 1.5.dp.toPx(), inset - 1.5.dp.toPx()),
                    size = Size(innerW + 3.dp.toPx(), innerH + 3.dp.toPx()),
                    cornerRadius = CornerRadius(cr + 1.5.dp.toPx(), cr + 1.5.dp.toPx()),
                    style = Stroke(width = 4.dp.toPx())
                )
                drawRoundRect(
                    color = buttonColor.copy(alpha = 0.30f),
                    topLeft = Offset(inset, inset),
                    size = Size(innerW, innerH),
                    cornerRadius = CornerRadius(cr, cr),
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }

            Box(
                modifier = Modifier.graphicsLayer { rotationZ = -45f },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayLabel,
                    color = buttonColor,
                    fontWeight = FontWeight.Medium,
                    fontSize = 32.sp,
                    style = androidx.compose.ui.text.TextStyle(
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = buttonColor.copy(alpha = 0.50f),
                            blurRadius = 14f
                        )
                    )
                )
            }

            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                val ringInset = 3.dp.toPx()
                val ringW = w - ringInset * 2f
                val ringH = h - ringInset * 2f
                val ringCr = 11.dp.toPx()

                drawRoundRect(
                    color = buttonColor.copy(alpha = 0.35f),
                    topLeft = Offset(ringInset - 1f, ringInset - 1f),
                    size = Size(ringW + 2f, ringH + 2f),
                    cornerRadius = CornerRadius(ringCr + 1f, ringCr + 1f),
                    style = Stroke(width = 4.dp.toPx())
                )
                drawRoundRect(
                    color = buttonColor.copy(alpha = 0.70f),
                    topLeft = Offset(ringInset, ringInset),
                    size = Size(ringW, ringH),
                    cornerRadius = CornerRadius(ringCr, ringCr),
                    style = Stroke(width = 2.dp.toPx())
                )

                val lensInset = 1.dp.toPx()
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.16f),
                            Color.White.copy(alpha = 0.05f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = h * 0.40f
                    ),
                    topLeft = Offset(lensInset, lensInset),
                    size = Size(w - lensInset * 2f, h * 0.40f),
                    cornerRadius = CornerRadius(13.dp.toPx(), 13.dp.toPx())
                )
            }
        }
    }
}
