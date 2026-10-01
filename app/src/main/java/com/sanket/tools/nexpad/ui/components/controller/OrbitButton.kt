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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.category.CategoryManager
import com.sanket.tools.nexpad.category.ControllerLabelStyle
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel
import kotlin.math.PI
import kotlin.math.roundToInt

/**
 * Orbit Rings ABXY button variant.
 * Faithful reproduction of HTML/CSS #9 Orbit:
 * Two concentric SVG arc rings that rotate in opposite directions around the letter:
 * - Orbit 1 (outer, r=30.8dp, width=3.5dp): 3 large arcs (dash 26, gap 7.33) rotating +120° on press.
 * - Orbit 2 (inner, r=24.6dp, width=2dp): fine dotted pips (dash 1.2, gap 4.8) rotating -120° on press.
 * Damped harmonic spring kinematics.
 */
@Composable
fun OrbitButton(
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
        label = "orbit_scale"
    )
    val pressOffsetYAnim by animateFloatAsState(
        targetValue = if (isPressed) 2f else 0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "orbit_depth"
    )
    // Counter-rotating angles with elastic spring rebound (cubic-bezier .3, 1.5, .5, 1)
    val rotation1 by animateFloatAsState(
        targetValue = if (isPressed) 120f else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 320f),
        label = "orbit_rot1"
    )
    val rotation2 by animateFloatAsState(
        targetValue = if (isPressed) -120f else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 320f),
        label = "orbit_rot2"
    )
    val orbit1Alpha by animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0.75f,
        animationSpec = tween(durationMillis = 150),
        label = "orbit_o1_alpha"
    )
    val orbit2Alpha by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 0.45f,
        animationSpec = tween(durationMillis = 150),
        label = "orbit_o2_alpha"
    )
    val rgbBloomAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 0.45f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "orbit_rgb_bloom"
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
            .drawBehind {
                if (isRgbEnabled) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                buttonColor.copy(alpha = rgbBloomAlpha * 0.55f),
                                buttonColor.copy(alpha = rgbBloomAlpha * 0.22f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = size.minDimension * 1f
                        ),
                        radius = size.minDimension * 1f
                    )
                }
            }
            .shadow(
                elevation = if (isPressed) 1.dp else 4.dp,
                shape = CircleShape,
                ambientColor = if (isRgbEnabled) buttonColor else Color.Black,
                spotColor = if (isRgbEnabled) buttonColor else Color.Black
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
        // ── 1. ORBIT SVG RINGS & UNDERCUT SHADOW ──
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f
            val maxR = size.minDimension / 2f

            // Undercut bottom shadow
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

            // Outer Orbit 1: r = 30 / 39 * maxR, stroke-width = 3.5, dasharray = 26 7.33 (3 arcs)
            val r1 = (30f / 39f) * maxR
            val c1 = (2f * PI * r1).toFloat()
            val stroke1Px = (3.5f / 39f) * maxR
            val dash1 = (0.26f * c1 - stroke1Px).coerceAtLeast(1f)
            val gap1 = 0.0733f * c1 + stroke1Px

            rotate(degrees = rotation1, pivot = Offset(cx, cy)) {
                drawCircle(
                    color = buttonColor.copy(alpha = orbit1Alpha),
                    radius = r1,
                    center = Offset(cx, cy),
                    style = Stroke(
                        width = stroke1Px,
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash1, gap1), 0f)
                    )
                )
            }

            // Inner Orbit 2: r = 24 / 39 * maxR, stroke-width = 2, dasharray = 1.2 4.8 (dotted pips)
            val r2 = (24f / 39f) * maxR
            val c2 = (2f * PI * r2).toFloat()
            val stroke2Px = (2f / 39f) * maxR
            val pipGap = (c2 / 16.67f) - 0.1f

            rotate(degrees = rotation2, pivot = Offset(cx, cy)) {
                drawCircle(
                    color = buttonColor.copy(alpha = orbit2Alpha),
                    radius = r2,
                    center = Offset(cx, cy),
                    style = Stroke(
                        width = stroke2Px,
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(0.1f, pipGap), 0f)
                    )
                )
            }
        }

        // ── 2. CENTER GLYPH (font-size: 34px) ──
        Text(
            text = activeLabel,
            color = buttonColor,
            fontWeight = FontWeight.Medium,
            fontSize = 34.sp
        )

        // ── 3. SHELL OVERLAY (.lx-ring + .lx-lens) ──
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

            // Neon ring (.lx-ring)
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

            // Glass lens (.lx-lens)
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
 * Static non-interactive preview of OrbitButton (idle state).
 */
@Composable
internal fun StaticOrbitButton(
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
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            buttonColor.copy(alpha = 0.45f * 0.55f),
                            buttonColor.copy(alpha = 0.45f * 0.22f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.minDimension * 1f
                    ),
                    radius = size.minDimension * 1f
                )
            }
            .shadow(
                elevation = 4.dp,
                shape = CircleShape,
                ambientColor = buttonColor,
                spotColor = buttonColor
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
            val cx = w / 2f
            val cy = h / 2f
            val maxR = size.minDimension / 2f

            // Undercut shadow
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

            // Outer Orbit 1: r = 30 / 39 * maxR, stroke-width = 3.5, dasharray = 26 7.33 (3 arcs)
            val r1 = (30f / 39f) * maxR
            val c1 = (2f * PI * r1).toFloat()
            val stroke1Px = (3.5f / 39f) * maxR
            val dash1 = (0.26f * c1 - stroke1Px).coerceAtLeast(1f)
            val gap1 = 0.0733f * c1 + stroke1Px
            drawCircle(
                color = buttonColor.copy(alpha = 0.75f),
                radius = r1,
                center = Offset(cx, cy),
                style = Stroke(
                    width = stroke1Px,
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash1, gap1), 0f)
                )
            )

            // Inner Orbit 2: r = 24 / 39 * maxR, stroke-width = 2, dasharray = 1.2 4.8 (dotted pips)
            val r2 = (24f / 39f) * maxR
            val c2 = (2f * PI * r2).toFloat()
            val stroke2Px = (2f / 39f) * maxR
            val pipGap = (c2 / 16.67f) - 0.1f
            drawCircle(
                color = buttonColor.copy(alpha = 0.45f),
                radius = r2,
                center = Offset(cx, cy),
                style = Stroke(
                    width = stroke2Px,
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(0.1f, pipGap), 0f)
                )
            )
        }

        Text(
            text = displayLabel,
            color = buttonColor,
            fontWeight = FontWeight.Medium,
            fontSize = 34.sp
        )

        // Shell
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
