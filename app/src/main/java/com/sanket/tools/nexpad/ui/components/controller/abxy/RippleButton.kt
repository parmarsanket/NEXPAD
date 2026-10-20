package com.sanket.tools.nexpad.ui.components.controller.abxy

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.category.CategoryManager
import com.sanket.tools.nexpad.category.ControllerLabelStyle
import com.sanket.tools.nexpad.ui.theme.NeonPalette
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    isConnected: Boolean = true,
    onVibrate: () -> Unit = {},
    viewModel: GamepadViewModel? = null,
    isRgbEnabled: Boolean = true,
    modifier: Modifier = Modifier,
    displayLabel: String? = null,
    isInteractive: Boolean = true
) {
    var isPressedLocally by remember { mutableStateOf(false) }
    val externalPressed = com.sanket.tools.nexpad.ui.components.controller.isControlPressed(key)
    val isPressed = isPressedLocally || externalPressed

    val scaleAnimState = animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "rip_scale"
    )
    val pressOffsetYAnimState = animateFloatAsState(
        targetValue = if (isPressed) 2f else 0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "rip_depth"
    )
    val rgbBloomAlphaState = animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 0.45f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "rip_rgb_bloom"
    )

    // Three independent ripple animatables
    val ring1Progress = remember { Animatable(1f) } // 0f = start, 1f = finished
    val ring2Progress = remember { Animatable(1f) }
    val ring3Progress = remember { Animatable(1f) }

    var rippleTrigger by remember { mutableLongStateOf(0L) }

    LaunchedEffect(externalPressed) {
        if (externalPressed) {
            rippleTrigger = System.nanoTime()
        }
    }

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
    val psShape = getPlayStationShape(activeLabel)
    val effectiveColor = if (psShape != null) getPlayStationColor(psShape) else buttonColor

    val density = androidx.compose.ui.platform.LocalDensity.current.density
    val stroke1dp = remember(density) { Stroke(width = 1f * density) }
    val stroke2dp = remember(density) { Stroke(width = 2f * density) }
    val stroke4dp = remember(density) { Stroke(width = 4f * density) }
    val stroke4_5dp = remember(density) { Stroke(width = 4.5f * density) }

    val baseDomeGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                NeonPalette.MetalBase,
                NeonPalette.MetalShadow,
                NeonPalette.PureBlack
            ),
            center = Offset(0.50f, 0.55f),
            radius = 240f
        )
    }

    val borderBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color.Black.copy(alpha = 0.50f),
                Color.Black.copy(alpha = 0.85f)
            )
        )
    }

    val epicenterBloomBrush = remember(effectiveColor, density) {
        val outerRadius = 46f * density
        Brush.radialGradient(
            colors = listOf(
                effectiveColor.copy(alpha = 0.45f),
                effectiveColor.copy(alpha = 0.16f),
                Color.Transparent
            ),
            center = Offset(outerRadius, outerRadius),
            radius = outerRadius
        )
    }

    val insetShadowBrushPressed = remember(density) {
        val h = 80f * density
        val insetH = h * 0.32f
        Brush.verticalGradient(
            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
            startY = h - insetH,
            endY = h
        )
    }
    val insetShadowBrushNormal = remember(density) {
        val h = 80f * density
        val insetH = h * 0.32f
        Brush.verticalGradient(
            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.70f)),
            startY = h - insetH,
            endY = h
        )
    }

    val lensBrushPressed = remember(density) {
        val h = 80f * density
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.06f),
                Color.White.copy(alpha = 0.02f),
                Color.Transparent
            ),
            startY = 0f,
            endY = h * 0.40f
        )
    }
    val lensBrushNormal = remember(density) {
        val h = 80f * density
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.14f),
                Color.White.copy(alpha = 0.05f),
                Color.Transparent
            ),
            startY = 0f,
            endY = h * 0.40f
        )
    }

    val specularSheenBrush = remember(density) {
        val w = 80f * density
        val h = 80f * density
        Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.06f), Color.Transparent),
            center = Offset(w * 0.70f, h * 0.78f),
            radius = w * 0.25f
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
                    val rgbBloomAlpha = rgbBloomAlphaState.value
                    // 1. Acoustic shockwave epicenter bloom
                    val epicenterRadius = size.minDimension * 0.48f
                    drawCircle(
                        brush = epicenterBloomBrush,
                        radius = epicenterRadius,
                        center = center,
                        alpha = rgbBloomAlpha
                    )
                }

                // ── THREE RIPPLE SHOCKWAVE WAVEFRONTS PULSING OUTWARD ──
                val rippleCenter = center
                val baseR = 40.dp.toPx()

                fun drawRipple(progress: Float) {
                    if (progress < 0.999f) {
                        val currentScale = 1.0f + 0.90f * progress // 1.0f to 1.9f
                        val currentAlpha = 0.85f * (1.0f - progress) // 0.85f down to 0f
                        val ringR = baseR * currentScale
                        // Soft bloom outer wavefront
                        drawCircle(
                            color = effectiveColor.copy(alpha = currentAlpha * 0.35f),
                            radius = ringR,
                            center = rippleCenter,
                            style = stroke4_5dp
                        )
                        // Sharp energetic core wavefront
                        drawCircle(
                            color = (if (progress < 0.25f) Color.White else effectiveColor).copy(alpha = currentAlpha),
                            radius = ringR,
                            center = rippleCenter,
                            style = stroke2dp
                        )
                    }
                }

                drawRipple(ring1Progress.value)
                drawRipple(ring2Progress.value)
                drawRipple(ring3Progress.value)
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
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
                                    isPressedLocally = true
                                    currentViewModel?.updateButton(key, true)
                                    rippleTrigger = System.nanoTime()
                                    tryAwaitRelease()
                                    isPressedLocally = false
                                    currentViewModel?.updateButton(key, false)
                                }
                            )
                        }
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            // ── 1. BODY UNDERCUT SHADOW ──
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                val insetH = h * 0.32f
                drawRect(
                    brush = if (isPressed) insetShadowBrushPressed else insetShadowBrushNormal,
                    topLeft = Offset(0f, h - insetH),
                    size = Size(w, insetH)
                )
            }

            // ── 2. GLYPH WITH NEON SHADOW BLOOM ON PRESS ──
            if (psShape != null) {
                PlayStationSymbol(
                    shape = psShape,
                    color = effectiveColor,
                    size = 42.dp
                )
            } else {
                Text(
                    text = activeLabel,
                    color = effectiveColor,
                    fontWeight = FontWeight.Medium,
                    fontSize = 44.sp,
                    style = TextStyle(
                        shadow = if (isPressed) Shadow(
                            color = effectiveColor,
                            blurRadius = 24f
                        ) else null
                    )
                )
            }

            // ── 3. SHELL OVERLAY (.lx-ring + .lx-lens) ──
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
                    brush = if (isPressed) lensBrushPressed else lensBrushNormal,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(lensInset, lensInset),
                    size = Size(w - lensInset * 2f, h - lensInset * 2f),
                    style = stroke1dp
                )

                // Specular sheen
                drawCircle(
                    brush = specularSheenBrush,
                    center = Offset(w * 0.70f, h * 0.78f),
                    radius = w * 0.25f
                )
            }
        }
    }
}

/**
 * Static non-interactive preview delegate for RippleButton with zero touch overhead.
 */
@Composable
fun StaticRippleButton(
    controlKey: String,
    labelStyle: ControllerLabelStyle = ControllerLabelStyle.XBOX,
    modifier: Modifier = Modifier
) {
    val displayLabel = CategoryManager.getLabelForStyle(controlKey, labelStyle)
    val buttonColor = resolveButtonColor(controlKey, labelStyle, displayLabel)
    RippleButton(
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



