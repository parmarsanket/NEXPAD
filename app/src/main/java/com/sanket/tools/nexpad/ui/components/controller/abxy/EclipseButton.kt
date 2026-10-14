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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.category.CategoryManager
import com.sanket.tools.nexpad.category.ControllerLabelStyle
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel
import kotlin.math.roundToInt

/**
 * Eclipse Disc ABXY button variant.
 * Faithful reproduction of HTML/CSS #13 Eclipse with strict shape preservation:
 * - Default Shape (Idle): Perfectly centered, symmetrical circular disc. The dark disc covers the
 *   corona completely at rest (offset = 0dp), with the letter dead-center and concentric neon ring.
 * - Press Shape (Pressed): The dark disc slides diagonally to (-12dp, -12dp), cleanly unveiling
 *   the radiant solar corona at bottom-right, while the circular button boundary is strictly maintained.
 * - Damped harmonic spring kinematics return smoothly to the default shape on release.
 */
@Composable
fun EclipseButton(
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
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "ecl_scale"
    )
    val pressOffsetYAnimState = animateFloatAsState(
        targetValue = if (isPressed) 2f else 0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "ecl_depth"
    )
    val slideOffsetState = animateFloatAsState(
        targetValue = if (isPressed) -20.5f else -5.1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 380f),
        label = "ecl_slide"
    )
    // Corona: opacity 0.90 at rest, blooms to 1.0 on press
    val coronaAlphaState = animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0.90f,
        animationSpec = tween(durationMillis = 200),
        label = "ecl_corona"
    )
    val rgbBloomAlphaState = animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 0.45f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "ecl_rgb_bloom"
    )

    val currentOnVibrate by rememberUpdatedState(onVibrate)
    val currentViewModel by rememberUpdatedState(viewModel)
    val activeLabel = displayLabel ?: key
    val psShape = getPlayStationShape(activeLabel)
    val effectiveColor = if (psShape != null) getPlayStationColor(psShape) else buttonColor

    val density = LocalDensity.current.density
    val stroke2_5 = remember { Stroke(width = 2.5f) }
    val stroke1dp = remember(density) { Stroke(width = 1f * density) }
    val stroke2dp = remember(density) { Stroke(width = 2f * density) }
    val stroke4dp = remember(density) { Stroke(width = 4f * density) }

    val borderBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color.Black.copy(alpha = 0.50f),
                Color.Black.copy(alpha = 0.85f)
            )
        )
    }

    val moonDiscBrush = remember {
        Brush.radialGradient(
            colors = listOf(Color(0xFF202224), Color(0xFF08090A)),
            center = Offset(0.50f, 0.40f),
            radius = 240f
        )
    }

    val insetShadowColorsPressed = remember {
        listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
    }
    val insetShadowColorsNormal = remember {
        listOf(Color.Transparent, Color.Black.copy(alpha = 0.70f))
    }
    val lensColorsPressed = remember {
        listOf(
            Color.White.copy(alpha = 0.06f),
            Color.White.copy(alpha = 0.02f),
            Color.Transparent
        )
    }
    val lensColorsNormal = remember {
        listOf(
            Color.White.copy(alpha = 0.14f),
            Color.White.copy(alpha = 0.05f),
            Color.Transparent
        )
    }
    val specularSheenColors = remember {
        listOf(Color.White.copy(alpha = 0.06f), Color.Transparent)
    }

    val penumbraStops = remember(effectiveColor) {
        arrayOf(
            0.00f to Color.Transparent,
            0.25f to Color.Transparent,
            0.65f to Color.Transparent,
            1.00f to Color.Transparent
        )
    }
    val diamondFlareColors = remember(effectiveColor) {
        listOf(Color.White, effectiveColor, Color.Transparent)
    }

    Box(
        modifier = modifier
            .size(80.dp)
            .graphicsLayer {
                // Root RenderNode isolation
            }
            .drawBehind {
                if (isRgbEnabled) {
                    val rgbBloomAlpha = rgbBloomAlphaState.value
                    val slideOffset = slideOffsetState.value
                    // 1. Asymmetric Solar Corona & Penumbra shifted opposite to moon slide
                    val penumbraCenter = Offset(
                        center.x - slideOffset * 0.55f,
                        center.y - slideOffset * 0.55f
                    )
                    val penumbraRadius = size.minDimension * 0.95f
                    penumbraStops[0] = 0.00f to Color.White.copy(alpha = if (isPressed) 0.65f else 0.20f)
                    penumbraStops[1] = 0.25f to effectiveColor.copy(alpha = rgbBloomAlpha * 0.70f)
                    penumbraStops[2] = 0.65f to effectiveColor.copy(alpha = rgbBloomAlpha * 0.20f)
                    penumbraStops[3] = 1.00f to Color.Transparent
                    drawCircle(
                        brush = Brush.radialGradient(
                            colorStops = penumbraStops,
                            center = penumbraCenter,
                            radius = penumbraRadius
                        ),
                        radius = penumbraRadius,
                        center = penumbraCenter
                    )

                    // 2. Annular limb ring
                    drawCircle(
                        color = effectiveColor.copy(alpha = if (isPressed) 0.55f else 0.22f),
                        radius = size.minDimension * 0.52f,
                        center = center,
                        style = stroke2_5
                    )

                    // 3. Diamond Ring flare spike on press at 135° limb
                    if (isPressed) {
                        val limbR = size.minDimension * 0.50f
                        val fx = center.x + limbR * 0.7071f
                        val fy = center.y + limbR * 0.7071f
                        val flareCenter = Offset(fx, fy)
                        // Brilliant diamond core
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = diamondFlareColors,
                                center = flareCenter,
                                radius = 22f
                            ),
                            radius = 22f,
                            center = flareCenter
                        )
                        // Cross rays
                        drawLine(
                            color = Color.White.copy(alpha = 0.85f),
                            start = Offset(fx - 20f, fy),
                            end = Offset(fx + 20f, fy),
                            strokeWidth = 2.5f
                        )
                        drawLine(
                            color = Color.White.copy(alpha = 0.85f),
                            start = Offset(fx, fy - 20f),
                            end = Offset(fx, fy + 20f),
                            strokeWidth = 2.5f
                        )
                    }
                }
            }
            .shadow(
                elevation = if (isPressed) 1.dp else 4.dp,
                shape = CircleShape,
                ambientColor = if (isRgbEnabled) effectiveColor else Color.Black,
                spotColor = if (isRgbEnabled) effectiveColor else Color.Black
            )
            .graphicsLayer {
                scaleX = scaleAnimState.value
                scaleY = scaleAnimState.value
                translationY = pressOffsetYAnimState.value * density
            }
            .clip(CircleShape)
            // .lx-body background #040405
            .background(Color(0xFF040405))
            .border(
                width = 1.dp,
                brush = borderBrush,
                shape = CircleShape
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
        // ── 1. SOLAR CORONA LAYER (.e-light) ──
        // Always present at bottom-right (70%, 70%)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val coronaAlpha = coronaAlphaState.value
            val w = size.width
            val h = size.height
            val coronaCenter = Offset(w * 0.70f, h * 0.70f)
            val coronaRadius = w * 0.80f
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.00f to Color.White.copy(alpha = 0.95f * coronaAlpha),
                        0.35f to effectiveColor.copy(alpha = 0.90f * coronaAlpha),
                        0.80f to Color.Transparent
                    ),
                    center = coronaCenter,
                    radius = coronaRadius
                ),
                radius = coronaRadius,
                center = coronaCenter
            )
        }

        // ── 2. DARK MOON DISC (.e-dark) ──
        // Translated (-5.1dp, -5.1dp) at rest, smoothly slides to (-20.5dp, -20.5dp) on press
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset {
                    val s = (slideOffsetState.value * density).roundToInt()
                    IntOffset(s, s)
                }
                .shadow(
                    elevation = 10.dp,
                    shape = CircleShape,
                    spotColor = Color.Black,
                    ambientColor = Color.Black
                )
                .clip(CircleShape)
                .background(moonDiscBrush),
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
                    fontSize = 44.sp
                )
            }
        }

        // ── 3. SHELL OVERLAY (.lx-ring + .lx-lens + undercut shadow) ──
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val r = size.minDimension / 2f

            // Undercut bottom shadow
            val insetH = h * 0.32f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = if (isPressed) insetShadowColorsPressed else insetShadowColorsNormal,
                    startY = h - insetH,
                    endY = h
                ),
                topLeft = Offset(0f, h - insetH),
                size = Size(w, insetH)
            )

            // Cap outline
            drawCircle(
                color = Color.Black.copy(alpha = 0.50f),
                radius = r - 0.5.dp.toPx(),
                style = stroke1dp
            )

            // Neon ring (.lx-ring): inset 3px, 2px core + 4px bloom
            val ringRadius = r - 3.dp.toPx()
            drawCircle(
                color = effectiveColor.copy(alpha = if (isPressed) 0.55f else 0.25f),
                radius = ringRadius,
                style = stroke4dp
            )
            drawCircle(
                color = effectiveColor.copy(alpha = if (isPressed) 1.0f else 0.70f),
                radius = ringRadius,
                style = stroke2dp
            )

            // Glass lens (.lx-lens): top crescent arc highlight
            val lensInset = 1.dp.toPx()
            drawArc(
                brush = Brush.verticalGradient(
                    colors = if (isPressed) lensColorsPressed else lensColorsNormal,
                    startY = 0f,
                    endY = h * 0.40f
                ),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(lensInset, lensInset),
                size = Size(w - lensInset * 2f, h - lensInset * 2f),
                style = stroke1dp
            )

            // Bottom-right specular glint
            drawCircle(
                brush = Brush.radialGradient(
                    colors = specularSheenColors,
                    center = Offset(w * 0.70f, h * 0.78f),
                    radius = w * 0.25f
                ),
                center = Offset(w * 0.70f, h * 0.78f),
                radius = w * 0.25f
            )
        }
    }
}

/**
 * Static non-interactive preview delegate for EclipseButton with zero touch overhead.
 */
@Composable
fun StaticEclipseButton(
    controlKey: String,
    labelStyle: ControllerLabelStyle = ControllerLabelStyle.XBOX,
    modifier: Modifier = Modifier
) {
    val displayLabel = CategoryManager.getLabelForStyle(controlKey, labelStyle)
    val buttonColor = resolveButtonColor(controlKey, labelStyle, displayLabel)
    EclipseButton(
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
