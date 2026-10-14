package com.sanket.tools.nexpad.ui.components.controller.abxy

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.category.CategoryManager
import com.sanket.tools.nexpad.category.ControllerLabelStyle
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel
import kotlin.math.roundToInt

/**
 * Liquid Fill ABXY button variant.
 * Faithful reproduction of HTML/CSS #2 Liquid:
 * On press, vibrant neon liquid surges upward from the bottom (.lq) with hydrodynamic overshoot.
 * An animated sinusoidal meniscus wave (.lq::before) sways at the crest.
 * A 180° vertical glass gradient (.lq::after) coats the liquid surface.
 * Once submerged, the letter turns from neon to deep ink (#0B0C0D) with delay.
 * Damped harmonic spring kinematics.
 */
@Composable
fun LiquidButton(
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
        label = "liq_scale"
    )
    val pressOffsetYAnimState = animateFloatAsState(
        targetValue = if (isPressed) 2f else 0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "liq_depth"
    )
    // Liquid fill fraction: spring with overshoot replicating cubic-bezier(.3, 1.2, .5, 1)
    val fillFractionState = animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0.0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 280f),
        label = "liq_fill"
    )
    // Label ink color transition with 150ms delay
    val labelInkAlphaState = animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 150, delayMillis = 150),
        label = "liq_ink_alpha"
    )
    val rgbBloomAlphaState = animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 0.45f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "liq_rgb_bloom"
    )

    // Meniscus wave horizontal sway and vertical wobble (sway 0.7s alternate)
    val infiniteTransition = rememberInfiniteTransition(label = "liq_wave")
    val swayXPercentState = infiniteTransition.animateFloat(
        initialValue = -0.09f,
        targetValue = 0.09f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "liq_sway_x"
    )
    val waveScaleYState = infiniteTransition.animateFloat(
        initialValue = 0.70f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "liq_wave_scale_y"
    )

    val currentOnVibrate by rememberUpdatedState(onVibrate)
    val currentViewModel by rememberUpdatedState(viewModel)
    val activeLabel = displayLabel ?: key
    val psShape = getPlayStationShape(activeLabel)
    val effectiveColor = if (psShape != null) getPlayStationColor(psShape) else buttonColor

    val baseDomeGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF232527),
                Color(0xFF0C0D0E),
                Color(0xFF000000)
            ),
            center = Offset(0.50f, 0.55f),
            radius = 240f
        )
    }

    val density = androidx.compose.ui.platform.LocalDensity.current.density
    val borderBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color.Black.copy(alpha = 0.50f),
                Color.Black.copy(alpha = 0.85f)
            )
        )
    }
    val stroke1dp = remember(density) { Stroke(width = 1f * density) }
    val stroke2dp = remember(density) { Stroke(width = 2f * density) }
    val stroke4dp = remember(density) { Stroke(width = 4f * density) }
    val liquidSheenColors = remember {
        listOf(
            Color.White.copy(alpha = 0.35f),
            Color.Transparent
        )
    }
    val insetShadowColorsPressed = remember { listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)) }
    val insetShadowColorsNormal = remember { listOf(Color.Transparent, Color.Black.copy(alpha = 0.70f)) }
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

    val poolBloomStops = remember(effectiveColor) {
        arrayOf(
            0.00f to Color.Transparent,
            0.35f to Color.Transparent,
            0.75f to Color.Transparent,
            1.00f to Color.Transparent
        )
    }

    Box(
        modifier = modifier
            .size(80.dp)
            .graphicsLayer {
                // Root RenderNode isolation
            }
            .drawBehind {
                if (isRgbEnabled) {
                    val fillFraction = fillFractionState.value
                    val rgbBloomAlpha = rgbBloomAlphaState.value
                    val swayXPercent = swayXPercentState.value
                    val clampedFill = fillFraction.coerceIn(0f, 1f)
                    // 1. Buoyant fluid reservoir pool that rises and expands upward with fillFraction
                    val poolCenterY = center.y + (1f - clampedFill * 0.75f) * (size.height * 0.26f)
                    val poolCenterX = center.x + (if (isPressed) swayXPercent * size.width * 0.5f else 0f)
                    val poolCenter = Offset(poolCenterX, poolCenterY)
                    val poolRadius = size.minDimension * (0.68f + 0.32f * clampedFill)

                    poolBloomStops[0] = 0.00f to (if (isPressed) Color.White else effectiveColor).copy(alpha = rgbBloomAlpha * 0.55f)
                    poolBloomStops[1] = 0.35f to effectiveColor.copy(alpha = rgbBloomAlpha * (0.35f + 0.35f * clampedFill))
                    poolBloomStops[2] = 0.75f to effectiveColor.copy(alpha = rgbBloomAlpha * 0.15f)
                    poolBloomStops[3] = 1.00f to Color.Transparent
                    drawCircle(
                        brush = Brush.radialGradient(
                            colorStops = poolBloomStops,
                            center = poolCenter,
                            radius = poolRadius
                        ),
                        radius = poolRadius,
                        center = poolCenter
                    )

                    // 2. Rising effervescent micro-bubble droplets on press
                    if (clampedFill > 0.15f) {
                        val bubbleAlpha = (clampedFill * rgbBloomAlpha).coerceIn(0f, 1f)
                        drawCircle(
                            color = Color.White.copy(alpha = bubbleAlpha * 0.85f),
                            radius = 2.8f,
                            center = Offset(center.x - 16f, poolCenterY - 14f * clampedFill)
                        )
                        drawCircle(
                            color = effectiveColor.copy(alpha = bubbleAlpha * 0.90f),
                            radius = 3.6f,
                            center = Offset(center.x + 18f, poolCenterY - 24f * clampedFill)
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = bubbleAlpha * 0.65f),
                            radius = 2.2f,
                            center = Offset(center.x + 2f, poolCenterY - 34f * clampedFill)
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
            .background(baseDomeGradient)
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
        // ── 1. LIQUID FILL RESERVOIR (.lq-wrap, inset 6px inside 80dp -> 68dp) ──
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val fillFraction = fillFractionState.value
                val waveScaleY = waveScaleYState.value
                val swayXPercent = swayXPercentState.value
                val w = size.width
                val h = size.height

                if (fillFraction > 0.005f) {
                    val clampedFill = fillFraction.coerceIn(0f, 1f)
                    val fillH = h * clampedFill
                    val fillTop = h - fillH

                    // Main rectangular liquid mass
                    drawRect(
                        color = effectiveColor,
                        topLeft = Offset(0f, fillTop),
                        size = Size(w, fillH)
                    )

                    // Sinusoidal meniscus wave ellipse at fill boundary
                    if (clampedFill < 0.99f) {
                        val ellipseW = w * 1.40f
                        val ellipseH = 12.dp.toPx() * (if (isPressed) waveScaleY else 1.0f)
                        val swayOffsetPx = if (isPressed) swayXPercent * w else 0f
                        val ellipseLeft = (w - ellipseW) / 2f + swayOffsetPx
                        val ellipseTop = fillTop - ellipseH / 2f

                        drawOval(
                            color = effectiveColor,
                            topLeft = Offset(ellipseLeft, ellipseTop),
                            size = Size(ellipseW, ellipseH)
                        )
                    }

                    // Liquid volumetric surface sheen (.lq::after): linear-gradient(180deg, rgba(255,255,255,0.35), transparent 50%)
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = liquidSheenColors,
                            startY = fillTop,
                            endY = fillTop + fillH * 0.55f
                        ),
                        topLeft = Offset(0f, fillTop),
                        size = Size(w, fillH)
                    )
                }
            }
        }

        // ── 2. INSET BOTTOM SHADOW ──
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

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
        }

        // ── 3. GLYPH (TRANSITIONS TO DARK INK WHEN SUBMERGED) ──
        Box(contentAlignment = Alignment.Center) {
            if (psShape != null) {
                PlayStationSymbol(
                    shape = psShape,
                    color = effectiveColor,
                    size = 38.dp,
                    modifier = Modifier.graphicsLayer {
                        alpha = (1f - labelInkAlphaState.value).coerceIn(0f, 1f)
                    }
                )
                PlayStationSymbol(
                    shape = psShape,
                    color = Color(0xFF0B0C0D),
                    size = 38.dp,
                    modifier = Modifier.graphicsLayer {
                        alpha = labelInkAlphaState.value.coerceIn(0f, 1f)
                    }
                )
            } else {
                // Neon glyph (fades out as ink alpha rises)
                Text(
                    text = activeLabel,
                    color = effectiveColor,
                    fontWeight = FontWeight.Medium,
                    fontSize = 44.sp,
                    modifier = Modifier.graphicsLayer {
                        alpha = (1f - labelInkAlphaState.value).coerceIn(0f, 1f)
                    }
                )
                // Dark ink glyph (.ab.pressed .g { color: #0b0c0d })
                Text(
                    text = activeLabel,
                    color = Color(0xFF0B0C0D),
                    fontWeight = FontWeight.Medium,
                    fontSize = 44.sp,
                    modifier = Modifier.graphicsLayer {
                        alpha = labelInkAlphaState.value.coerceIn(0f, 1f)
                    }
                )
            }
        }

        // ── 4. SHELL OVERLAY (.lx-ring + .lx-lens) ──
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val r = size.minDimension / 2f

            // Cap outline
            drawCircle(
                color = Color.Black.copy(alpha = 0.50f),
                radius = r - 0.5.dp.toPx(),
                style = stroke1dp
            )

            // Neon ring
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

            // Glass lens
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

            // Specular sheen
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
 * Static non-interactive preview delegate for LiquidButton with zero touch overhead.
 */
@Composable
fun StaticLiquidButton(
    controlKey: String,
    labelStyle: ControllerLabelStyle = ControllerLabelStyle.XBOX,
    modifier: Modifier = Modifier
) {
    val displayLabel = CategoryManager.getLabelForStyle(controlKey, labelStyle)
    val buttonColor = resolveButtonColor(controlKey, labelStyle, displayLabel)
    LiquidButton(
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


