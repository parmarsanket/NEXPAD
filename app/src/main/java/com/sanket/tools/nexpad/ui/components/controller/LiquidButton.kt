package com.sanket.tools.nexpad.ui.components.controller

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
    isConnected: Boolean,
    onVibrate: () -> Unit,
    viewModel: GamepadViewModel,
    isRgbEnabled: Boolean,
    modifier: Modifier = Modifier,
    displayLabel: String? = null
) {
    var isPressed by remember { mutableStateOf(false) }

    val scaleAnim by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "liq_scale"
    )
    val pressOffsetYAnim by animateFloatAsState(
        targetValue = if (isPressed) 2f else 0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "liq_depth"
    )
    // Liquid fill fraction: spring with overshoot replicating cubic-bezier(.3, 1.2, .5, 1)
    val fillFraction by animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0.0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 280f),
        label = "liq_fill"
    )
    // Label ink color transition with 150ms delay
    val labelInkAlpha by animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 150, delayMillis = 150),
        label = "liq_ink_alpha"
    )

    // Meniscus wave horizontal sway and vertical wobble (sway 0.7s alternate)
    val infiniteTransition = rememberInfiniteTransition(label = "liq_wave")
    val swayXPercent by infiniteTransition.animateFloat(
        initialValue = -0.09f,
        targetValue = 0.09f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "liq_sway_x"
    )
    val waveScaleY by infiniteTransition.animateFloat(
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

    Box(
        modifier = modifier
            .size(80.dp)
            .graphicsLayer {
                scaleX = scaleAnim
                scaleY = scaleAnim
            }
            .offset { IntOffset(0, pressOffsetYAnim.dp.roundToPx()) }
            .shadow(
                elevation = if (isPressed) 2.dp else 6.dp,
                shape = CircleShape,
                spotColor = Color.Black,
                ambientColor = Color.Black
            )
            .clip(CircleShape)
            .background(baseDomeGradient)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.50f),
                        Color.Black.copy(alpha = 0.85f)
                    )
                ),
                shape = CircleShape
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
        // ── 1. LIQUID FILL RESERVOIR (.lq-wrap, inset 6px inside 80dp -> 68dp) ──
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                if (fillFraction > 0.005f) {
                    val clampedFill = fillFraction.coerceIn(0f, 1f)
                    val fillH = h * clampedFill
                    val fillTop = h - fillH

                    // Main rectangular liquid mass
                    drawRect(
                        color = buttonColor,
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
                            color = buttonColor,
                            topLeft = Offset(ellipseLeft, ellipseTop),
                            size = Size(ellipseW, ellipseH)
                        )
                    }

                    // Liquid volumetric surface sheen (.lq::after): linear-gradient(180deg, rgba(255,255,255,0.35), transparent 50%)
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.35f),
                                Color.Transparent
                            ),
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
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = if (isPressed) 0.85f else 0.70f)),
                    startY = h - insetH,
                    endY = h
                ),
                topLeft = Offset(0f, h - insetH),
                size = Size(w, insetH)
            )
        }

        // ── 3. GLYPH (TRANSITIONS TO DARK INK WHEN SUBMERGED) ──
        Box(contentAlignment = Alignment.Center) {
            // Neon glyph (fades out as ink alpha rises)
            Text(
                text = activeLabel,
                color = buttonColor.copy(alpha = (1f - labelInkAlpha).coerceIn(0f, 1f)),
                fontWeight = FontWeight.Medium,
                fontSize = 44.sp
            )
            // Dark ink glyph (.ab.pressed .g { color: #0b0c0d })
            Text(
                text = activeLabel,
                color = Color(0xFF0B0C0D).copy(alpha = labelInkAlpha.coerceIn(0f, 1f)),
                fontWeight = FontWeight.Medium,
                fontSize = 44.sp
            )
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
                style = Stroke(width = 1.dp.toPx())
            )

            // Neon ring
            val ringRadius = r - 3.dp.toPx()
            drawCircle(
                color = buttonColor.copy(alpha = if (isPressed) 0.55f else 0.25f),
                radius = ringRadius,
                style = Stroke(width = 4.dp.toPx())
            )
            drawCircle(
                color = buttonColor.copy(alpha = if (isPressed) 1.0f else 0.70f),
                radius = ringRadius,
                style = Stroke(width = 2.dp.toPx())
            )

            // Glass lens
            val lensInset = 1.dp.toPx()
            drawArc(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isPressed) 0.06f else 0.14f),
                        Color.White.copy(alpha = if (isPressed) 0.02f else 0.05f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = h * 0.40f
                ),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(lensInset, lensInset),
                size = Size(w - lensInset * 2f, h - lensInset * 2f),
                style = Stroke(width = 1.dp.toPx())
            )

            // Specular sheen
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.06f), Color.Transparent),
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
 * Static non-interactive preview of LiquidButton (idle state).
 */
@Composable
internal fun StaticLiquidButton(
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

    Box(
        modifier = modifier
            .size(80.dp)
            .shadow(
                elevation = 6.dp,
                shape = CircleShape,
                spotColor = Color.Black,
                ambientColor = Color.Black
            )
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF232527), Color(0xFF0C0D0E), Color.Black),
                    center = Offset(0.50f, 0.55f),
                    radius = 240f
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
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val insetH = h * 0.32f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.70f)),
                    startY = h - insetH,
                    endY = h
                ),
                topLeft = Offset(0f, h - insetH),
                size = Size(w, insetH)
            )
        }

        Text(
            text = displayLabel,
            color = buttonColor,
            fontWeight = FontWeight.Medium,
            fontSize = 44.sp
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val r = size.minDimension / 2f

            drawCircle(
                color = Color.Black.copy(alpha = 0.50f),
                radius = r - 0.5.dp.toPx(),
                style = Stroke(width = 1.dp.toPx())
            )

            val ringRadius = r - 3.dp.toPx()
            drawCircle(
                color = buttonColor.copy(alpha = 0.25f),
                radius = ringRadius,
                style = Stroke(width = 4.dp.toPx())
            )
            drawCircle(
                color = buttonColor.copy(alpha = 0.70f),
                radius = ringRadius,
                style = Stroke(width = 2.dp.toPx())
            )

            val lensInset = 1.dp.toPx()
            drawArc(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.14f),
                        Color.White.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = h * 0.40f
                ),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(lensInset, lensInset),
                size = Size(w - lensInset * 2f, h - lensInset * 2f),
                style = Stroke(width = 1.dp.toPx())
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.06f), Color.Transparent),
                    center = Offset(w * 0.70f, h * 0.78f),
                    radius = w * 0.25f
                ),
                center = Offset(w * 0.70f, h * 0.78f),
                radius = w * 0.25f
            )
        }
    }
}
