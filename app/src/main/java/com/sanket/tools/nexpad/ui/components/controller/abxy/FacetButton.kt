package com.sanket.tools.nexpad.ui.components.controller.abxy

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
import androidx.compose.ui.draw.drawBehind
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.category.CategoryManager
import com.sanket.tools.nexpad.category.ControllerLabelStyle
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel

/**
 * Facet (Rotated Gem) ABXY button variant.
 * Faithful reproduction of HTML/CSS #4 Facet:
 * Proportionally calibrated diamond (54dp square rotated 45° with 12dp rounded corners)
 * ensuring its 76.4dp diagonal fits perfectly inside the standard 80dp container without any cropping.
 *
 * Features:
 * - High-intensity multi-stage emissive neon bloom around gem perimeter without bounds clipping.
 * - Flashing color fill (.f-fill) on press.
 * - Spinning + scaling inner diamond (.f-in): rotates 90° and scales to 0.85 with spring bounce.
 * - Undercut bottom shadow, top crescent glass highlight, counter-rotated upright letter/glyph.
 * - Unified interactive/static preview pipeline with zero gesture overhead when [isInteractive] is false.
 */
@Composable
fun FacetButton(
    key: String,
    buttonColor: Color,
    isConnected: Boolean = true,
    onVibrate: () -> Unit = {},
    viewModel: GamepadViewModel? = null,
    isRgbEnabled: Boolean = true,
    modifier: Modifier = Modifier,
    displayLabel: String? = null,
    isInteractive: Boolean = true
) {
    var isPressed by remember { mutableStateOf(false) }

    val scaleAnimState = animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1.0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "facet_scale"
    )
    // Inner diamond: rotate 90° + scale 0.85 on press
    val innerRotAnimState = animateFloatAsState(
        targetValue = if (isPressed) 90f else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 360f),
        label = "facet_inner_rot"
    )
    val innerScaleAnimState = animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1.0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 360f),
        label = "facet_inner_scale"
    )
    val innerAlphaState = animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 0.30f,
        animationSpec = tween(durationMillis = 150),
        label = "facet_inner_alpha"
    )
    val fillAlphaState = animateFloatAsState(
        targetValue = if (isPressed) 0.25f else 0f,
        animationSpec = tween(durationMillis = 100),
        label = "facet_fill_alpha"
    )
    val rgbBloomAlphaState = animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0.50f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "facet_rgb_bloom"
    )

    val currentOnVibrate by rememberUpdatedState(onVibrate)
    val currentViewModel by rememberUpdatedState(viewModel)
    val activeLabel = displayLabel ?: key
    val psShape = getPlayStationShape(activeLabel)
    val effectiveColor = if (psShape != null) getPlayStationColor(psShape) else buttonColor

    // 64dp square rotated 45° has diagonal 64 * sqrt(2) ≈ 90.5dp, fitting cleanly in 92dp
    val gemShape = remember { RoundedCornerShape(14.dp) }
    val density = LocalDensity.current.density
    val insetColorsPressed = remember { listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)) }
    val insetColorsNormal = remember { listOf(Color.Transparent, Color.Black.copy(alpha = 0.70f)) }
    val crystalGlintColors = remember { listOf(Color.White.copy(alpha = 0.8f), Color.Transparent) }
    val innerGlowStroke = remember(density) { Stroke(width = 3.5f * density) }
    val innerCoreStroke = remember(density) { Stroke(width = 1.4f * density) }
    val stroke2dp = remember(density) { Stroke(width = 2f * density) }
    val stroke4dp = remember(density) { Stroke(width = 4f * density) }

    val lensColorsPressed = remember {
        listOf(
            Color.White.copy(alpha = 0.08f),
            Color.White.copy(alpha = 0.02f),
            Color.Transparent
        )
    }
    val lensColorsNormal = remember {
        listOf(
            Color.White.copy(alpha = 0.16f),
            Color.White.copy(alpha = 0.05f),
            Color.Transparent
        )
    }

    val gemBackgroundBrush = remember {
        Brush.radialGradient(
            colors = listOf(Color(0xFF232527), Color(0xFF0C0D0E), Color.Black),
            center = Offset(0.5f, 0.55f),
            radius = 180f
        )
    }
    val gemBorderBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color.Black.copy(alpha = 0.50f),
                Color.Black.copy(alpha = 0.85f)
            )
        )
    }

    val diamondBloomStops = remember(effectiveColor) {
        arrayOf(
            0.00f to Color.Transparent,
            0.45f to Color.Transparent,
            1.00f to Color.Transparent
        )
    }

    Box(
        modifier = modifier
            .size(92.dp)
            .graphicsLayer {
                // Root RenderNode isolation
            }
            .drawBehind {
                if (isRgbEnabled) {
                    val cr = 14.dp.toPx()
                    val halfSize = (64.dp.toPx() * 1.4142f) / 2f
                    val pad = 5.dp.toPx()
                    val innerRotAnim = innerRotAnimState.value
                    val rgbBloomAlpha = rgbBloomAlphaState.value

                    // 1. Rotating crystalline diamond aura (tracks 45° chassis + innerRotAnim spring)
                    rotate(degrees = 45f + innerRotAnim * 0.5f, pivot = center) {
                        diamondBloomStops[0] = 0.00f to effectiveColor.copy(alpha = rgbBloomAlpha * 0.60f)
                        diamondBloomStops[1] = 0.45f to effectiveColor.copy(alpha = rgbBloomAlpha * 0.22f)
                        diamondBloomStops[2] = 1.00f to Color.Transparent
                        drawRoundRect(
                            brush = Brush.radialGradient(
                                colorStops = diamondBloomStops,
                                center = center,
                                radius = size.minDimension * 0.52f
                            ),
                            topLeft = Offset(center.x - halfSize - pad, center.y - halfSize - pad),
                            size = Size((halfSize + pad) * 2f, (halfSize + pad) * 2f),
                            cornerRadius = CornerRadius(cr, cr)
                        )

                        // 8-point crystalline diffraction spikes contained within 92dp bounds
                        val spikeLen = size.minDimension * 0.46f
                        val spikeColor = effectiveColor.copy(alpha = if (isPressed) 0.85f else 0.40f)
                        // Cardinal axes
                        drawLine(
                            color = spikeColor,
                            start = Offset(center.x - spikeLen, center.y),
                            end = Offset(center.x + spikeLen, center.y),
                            strokeWidth = if (isPressed) 2.2f else 1.4f
                        )
                        drawLine(
                            color = spikeColor,
                            start = Offset(center.x, center.y - spikeLen),
                            end = Offset(center.x, center.y + spikeLen),
                            strokeWidth = if (isPressed) 2.2f else 1.4f
                        )
                        // Diagonal 45° cross spikes
                        rotate(degrees = 45f, pivot = center) {
                            val diagLen = spikeLen * 0.72f
                            drawLine(
                                color = spikeColor.copy(alpha = spikeColor.alpha * 0.7f),
                                start = Offset(center.x - diagLen, center.y),
                                end = Offset(center.x + diagLen, center.y),
                                strokeWidth = 1.2f
                            )
                            drawLine(
                                color = spikeColor.copy(alpha = spikeColor.alpha * 0.7f),
                                start = Offset(center.x - diagLen, center.y),
                                end = Offset(center.x + diagLen, center.y),
                                strokeWidth = 1.2f
                            )
                        }
                    }

                    // 2. High-intensity crystal core glint on press
                    if (isPressed) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = crystalGlintColors,
                                center = center,
                                radius = 18f
                            ),
                            radius = 18f,
                            center = center
                        )
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // ── GEM DIAMOND (64dp square rotated 45°, diagonal 90.5dp inside 92dp) ──
        Box(
            modifier = Modifier
                .size(64.dp)
                .graphicsLayer {
                    rotationZ = 45f
                    scaleX = scaleAnimState.value
                    scaleY = scaleAnimState.value
                }
                .shadow(
                    elevation = if (isPressed) 1.dp else 4.dp,
                    shape = gemShape,
                    spotColor = if (isRgbEnabled) effectiveColor else Color.Black,
                    ambientColor = if (isRgbEnabled) effectiveColor else Color.Black
                )
                .clip(gemShape)
                .background(gemBackgroundBrush)
                .border(
                    width = 1.dp,
                    brush = gemBorderBrush,
                    shape = gemShape
                )
                .then(
                    if (isInteractive) {
                        Modifier.pointerInput(key) {
                            detectTapGestures(
                                onPress = {
                                    currentOnVibrate()
                                    isPressed = true
                                    currentViewModel?.updateButton(key, true)
                                    tryAwaitRelease()
                                    isPressed = false
                                    currentViewModel?.updateButton(key, false)
                                }
                            )
                        }
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            // ── 1. FLASH FILL LAYER (.f-fill) ──
            if (fillAlphaState.value > 0.001f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(effectiveColor.copy(alpha = fillAlphaState.value))
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
                        colors = if (isPressed) insetColorsPressed else insetColorsNormal,
                        startY = h - insetH,
                        endY = h
                    ),
                    topLeft = Offset(0f, h - insetH),
                    size = Size(w, insetH)
                )

                // Inner diamond: inset 11dp, corner radius 7dp
                val inset = 11.dp.toPx()
                val innerW = w - inset * 2f
                val innerH = h - inset * 2f
                val cr = 7.dp.toPx()

                rotate(innerRotAnimState.value, pivot = center) {
                    scale(scaleX = innerScaleAnimState.value, scaleY = innerScaleAnimState.value, pivot = center) {
                        // Inner diamond glow bloom
                        drawRoundRect(
                            color = effectiveColor.copy(alpha = innerAlphaState.value * 0.55f),
                            topLeft = Offset(inset - 1.5.dp.toPx(), inset - 1.5.dp.toPx()),
                            size = Size(innerW + 3.dp.toPx(), innerH + 3.dp.toPx()),
                            cornerRadius = CornerRadius(cr + 1.5.dp.toPx(), cr + 1.5.dp.toPx()),
                            style = innerGlowStroke
                        )
                        // Inner diamond core stroke
                        drawRoundRect(
                            color = effectiveColor.copy(alpha = innerAlphaState.value),
                            topLeft = Offset(inset, inset),
                            size = Size(innerW, innerH),
                            cornerRadius = CornerRadius(cr, cr),
                            style = innerCoreStroke
                        )
                    }
                }
            }

            // ── 3. COUNTER-ROTATED GLYPH (rotated -45° to remain upright) ──
            Box(
                modifier = Modifier.graphicsLayer { rotationZ = -45f },
                contentAlignment = Alignment.Center
            ) {
                if (psShape != null) {
                    PlayStationSymbol(
                        shape = psShape,
                        color = effectiveColor,
                        size = 38.dp
                    )
                } else {
                    Text(
                        text = activeLabel,
                        color = effectiveColor,
                        fontWeight = FontWeight.Medium,
                        fontSize = 38.sp,
                        style = androidx.compose.ui.text.TextStyle(
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = effectiveColor.copy(alpha = if (isPressed) 0.90f else 0.50f),
                                blurRadius = 14f
                            )
                        )
                    )
                }
            }

            // ── 4. SHELL OVERLAY (.lx-ring + .lx-lens) ──
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Neon ring around diamond perimeter (inset 3dp, r=11dp)
                val ringInset = 3.dp.toPx()
                val ringW = w - ringInset * 2f
                val ringH = h - ringInset * 2f
                val ringCr = 11.dp.toPx()

                // Outer soft bloom
                drawRoundRect(
                    color = effectiveColor.copy(alpha = if (isPressed) 0.65f else 0.35f),
                    topLeft = Offset(ringInset - 1f, ringInset - 1f),
                    size = Size(ringW + 2f, ringH + 2f),
                    cornerRadius = CornerRadius(ringCr + 1f, ringCr + 1f),
                    style = stroke4dp
                )
                // Core crisp ring
                drawRoundRect(
                    color = effectiveColor.copy(alpha = if (isPressed) 1.0f else 0.70f),
                    topLeft = Offset(ringInset, ringInset),
                    size = Size(ringW, ringH),
                    cornerRadius = CornerRadius(ringCr, ringCr),
                    style = stroke2dp
                )

                // Top specular highlight
                val lensInset = 1.dp.toPx()
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = if (isPressed) lensColorsPressed else lensColorsNormal,
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
 * Static non-interactive preview delegate for FacetButton with zero touch overhead.
 */
@Composable
fun StaticFacetButton(
    controlKey: String,
    labelStyle: ControllerLabelStyle = ControllerLabelStyle.XBOX,
    modifier: Modifier = Modifier
) {
    val displayLabel = CategoryManager.getLabelForStyle(controlKey, labelStyle)
    val buttonColor = resolveButtonColor(controlKey, labelStyle, displayLabel)
    FacetButton(
        key = controlKey,
        buttonColor = buttonColor,
        isConnected = false,
        onVibrate = {},
        viewModel = null,
        isRgbEnabled = true,
        modifier = modifier,
        displayLabel = displayLabel,
        isInteractive = false
    )
}
