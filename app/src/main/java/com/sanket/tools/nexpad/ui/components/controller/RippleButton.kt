package com.sanket.tools.nexpad.ui.components.controller

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.category.CategoryManager
import com.sanket.tools.nexpad.category.ControllerLabelStyle
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Ripple Rings ABXY button variant.
 * Faithful reproduction of HTML/CSS #6 Ripple:
 * On every press, three neon rings pulse outward (scale 1.0 -> 1.9, opacity 0.85 -> 0, 800ms)
 * staggered by 140ms and 280ms. The animation runs to FULL 100% completion in a detached coroutine
 * even if the button is instantly unpressed.
 * The label gains an intense neon bloom shadow on press.
 */
@Composable
fun RippleButton(
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
        label = "rip_scale"
    )
    val pressOffsetYAnim by animateFloatAsState(
        targetValue = if (isPressed) 2f else 0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "rip_depth"
    )
    val rgbBloomAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 0.45f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "rip_rgb_bloom"
    )

    // Three independent ripple animatables
    val ring1Progress = remember { Animatable(1f) } // 0f = start, 1f = finished
    val ring2Progress = remember { Animatable(1f) }
    val ring3Progress = remember { Animatable(1f) }

    var rippleTrigger by remember { mutableLongStateOf(0L) }

    LaunchedEffect(rippleTrigger) {
        if (rippleTrigger == 0L) return@LaunchedEffect
        launch {
            ring1Progress.snapTo(0f)
            ring1Progress.animateTo(1f, tween(800, easing = LinearOutSlowInEasing))
        }
        launch {
            delay(140L)
            ring2Progress.snapTo(0f)
            ring2Progress.animateTo(1f, tween(800, easing = LinearOutSlowInEasing))
        }
        launch {
            delay(280L)
            ring3Progress.snapTo(0f)
            ring3Progress.animateTo(1f, tween(800, easing = LinearOutSlowInEasing))
        }
    }

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
            .drawBehind {
                // ── THREE RIPPLE RINGS PULSING OUTWARD (UN-CANCELABLE) ──
                val center = Offset(size.width / 2f, size.height / 2f)
                val baseR = size.minDimension / 2f

                fun drawRipple(progress: Float) {
                    if (progress < 0.999f) {
                        val currentScale = 1.0f + 0.90f * progress // 1.0f to 1.9f
                        val currentAlpha = 0.85f * (1.0f - progress) // 0.85f down to 0f
                        drawCircle(
                            color = buttonColor.copy(alpha = currentAlpha),
                            radius = baseR * currentScale,
                            center = center,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }

                drawRipple(ring1Progress.value)
                drawRipple(ring2Progress.value)
                drawRipple(ring3Progress.value)
            }
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
                        rippleTrigger = System.nanoTime()
                        tryAwaitRelease()
                        isPressed = false
                        currentViewModel.updateButton(key, false)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // ── 1. BODY UNDERCUT SHADOW ──
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

        // ── 2. GLYPH WITH NEON SHADOW BLOOM ON PRESS ──
        Text(
            text = activeLabel,
            color = buttonColor,
            fontWeight = FontWeight.Medium,
            fontSize = 44.sp,
            style = TextStyle(
                shadow = if (isPressed) Shadow(
                    color = buttonColor,
                    blurRadius = 24f
                ) else null
            )
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
 * Static non-interactive preview of RippleButton (idle state).
 */
@Composable
internal fun StaticRippleButton(
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
