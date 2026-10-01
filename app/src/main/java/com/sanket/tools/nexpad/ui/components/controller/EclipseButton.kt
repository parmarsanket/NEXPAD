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
        label = "ecl_scale"
    )
    val pressOffsetYAnim by animateFloatAsState(
        targetValue = if (isPressed) 2f else 0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "ecl_depth"
    )
    // Dark disc: translate(-5.1dp, -5.1dp) in default state, slides to (-20.5dp, -20.5dp) on press
    val slideOffset by animateFloatAsState(
        targetValue = if (isPressed) -20.5f else -5.1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 380f),
        label = "ecl_slide"
    )
    // Corona: opacity 0.90 at rest, blooms to 1.0 on press
    val coronaAlpha by animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0.90f,
        animationSpec = tween(durationMillis = 200),
        label = "ecl_corona"
    )
    val rgbBloomAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 0.45f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "ecl_rgb_bloom"
    )

    val currentOnVibrate by rememberUpdatedState(onVibrate)
    val currentViewModel by rememberUpdatedState(viewModel)
    val activeLabel = displayLabel ?: key

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
            // .lx-body background #040405
            .background(Color(0xFF040405))
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
        // ── 1. SOLAR CORONA LAYER (.e-light) ──
        // Always present at bottom-right (70%, 70%)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val coronaCenter = Offset(w * 0.70f, h * 0.70f)
            val coronaRadius = w * 0.80f
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.00f to Color.White.copy(alpha = 0.95f * coronaAlpha),
                        0.35f to buttonColor.copy(alpha = 0.90f * coronaAlpha),
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
                .offset { IntOffset(slideOffset.dp.roundToPx(), slideOffset.dp.roundToPx()) }
                .shadow(
                    elevation = 10.dp,
                    shape = CircleShape,
                    spotColor = Color.Black,
                    ambientColor = Color.Black
                )
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF202224), Color(0xFF08090A)),
                        center = Offset(0.50f, 0.40f),
                        radius = 240f
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = activeLabel,
                color = buttonColor,
                fontWeight = FontWeight.Medium,
                fontSize = 44.sp
            )
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
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = if (isPressed) 0.85f else 0.70f)),
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
                style = Stroke(width = 1.dp.toPx())
            )

            // Neon ring (.lx-ring): inset 3px, 2px core + 4px bloom
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

            // Glass lens (.lx-lens): top crescent arc highlight
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

            // Bottom-right specular glint
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
 * Static non-interactive preview of EclipseButton (idle default state).
 * Perfectly centered and symmetrical circular shape.
 */
@Composable
internal fun StaticEclipseButton(
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
            .background(Color(0xFF040405))
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
        // ── 1. SOLAR CORONA LAYER (.e-light) ──
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val coronaCenter = Offset(w * 0.70f, h * 0.70f)
            val coronaRadius = w * 0.80f
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.00f to Color.White.copy(alpha = 0.95f * 0.90f),
                        0.35f to buttonColor.copy(alpha = 0.90f * 0.90f),
                        0.80f to Color.Transparent
                    ),
                    center = coronaCenter,
                    radius = coronaRadius
                ),
                radius = coronaRadius,
                center = coronaCenter
            )
        }

        // ── 2. DARK MOON DISC (.e-dark) translated (-5.1dp, -5.1dp) ──
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(-5.1.dp, -5.1.dp)
                .shadow(
                    elevation = 10.dp,
                    shape = CircleShape,
                    spotColor = Color.Black,
                    ambientColor = Color.Black
                )
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF202224), Color(0xFF08090A)),
                        center = Offset(0.50f, 0.40f),
                        radius = 240f
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = displayLabel,
                color = buttonColor,
                fontWeight = FontWeight.Medium,
                fontSize = 44.sp
            )
        }

        // Shell
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val r = size.minDimension / 2f

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
