package com.sanket.tools.nexpad.ui.components.controller

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.model.NexpadKeys
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel
import kotlin.math.roundToInt

/**
 * NEXPAD Bumper B — LED Bar (LB / RB)
 *
 * Faithfully implements the LED Bar Bumper design:
 * - Asymmetrical ergonomic contour (outer 27dp pill cap, inner 12dp soft curve)
 * - Proportioned and scaled for mobile gamepad ergonomics (154dp × 54dp)
 * - Deep recessed acrylic dome body (#232527 -> #0c0d0e -> #000)
 * - Inset 2.5dp glowing neon border ring with asymmetric contour matching and emissive bloom
 * - Recessed optical magnifier window (48dp × 28dp) with 3D vignette and specular edge
 * - 6-segment illuminated neon LED bar graph with cascading wave animation on press (35ms stagger)
 * - Top specular crescent lens highlights
 * - Kinematic plunging spring travel (translateY 2dp, scale 0.96)
 */
@Composable
fun LedBumper(
    key: String,
    isConnected: Boolean,
    onVibrate: () -> Unit,
    viewModel: GamepadViewModel,
    isRgbEnabled: Boolean,
    modifier: Modifier = Modifier,
    displayLabel: String? = null
) {
    var isPressed by remember { mutableStateOf(false) }
    val isLeft = remember(key) {
        val upper = key.uppercase()
        upper == NexpadKeys.LB || upper == "L1" || upper == "LEFT"
    }

    // Outer contour: Flipped mobile ergonomics — 12dp squarer curve hugs outer phone bezel, 27dp pill curve points inward
    val bumperShape = remember(isLeft) {
        if (isLeft) {
            RoundedCornerShape(topStart = 12.dp, topEnd = 27.dp, bottomEnd = 27.dp, bottomStart = 12.dp)
        } else {
            RoundedCornerShape(topStart = 27.dp, topEnd = 12.dp, bottomEnd = 12.dp, bottomStart = 27.dp)
        }
    }

    // Kinematic Physics Engine — Damped Harmonic Spring
    val scaleAnim by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "led_bumper_scale"
    )
    val pressOffsetYAnim by animateFloatAsState(
        targetValue = if (isPressed) 2.0f else 0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "led_bumper_offset"
    )
    val ringBloomAlpha by animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0.70f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 800f),
        label = "led_ring_bloom"
    )

    // CSS: --glow: #a97cf0
    val neonColor = remember(isRgbEnabled) {
        if (isRgbEnabled) Color(0xFFA97CF0) else Color(0xFFD8DEE9)
    }

    // Cascading 6-LED Bar illumination wave on press (35ms stagger per LED)
    val led0Alpha by animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0.18f,
        animationSpec = tween(durationMillis = 120, delayMillis = if (isPressed) 0 else 0),
        label = "led_0"
    )
    val led1Alpha by animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0.18f,
        animationSpec = tween(durationMillis = 120, delayMillis = if (isPressed) 35 else 0),
        label = "led_1"
    )
    val led2Alpha by animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0.18f,
        animationSpec = tween(durationMillis = 120, delayMillis = if (isPressed) 70 else 0),
        label = "led_2"
    )
    val led3Alpha by animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0.18f,
        animationSpec = tween(durationMillis = 120, delayMillis = if (isPressed) 105 else 0),
        label = "led_3"
    )
    val led4Alpha by animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0.18f,
        animationSpec = tween(durationMillis = 120, delayMillis = if (isPressed) 140 else 0),
        label = "led_4"
    )
    val led5Alpha by animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0.18f,
        animationSpec = tween(durationMillis = 120, delayMillis = if (isPressed) 175 else 0),
        label = "led_5"
    )
    val ledAlphas = listOf(led0Alpha, led1Alpha, led2Alpha, led3Alpha, led4Alpha, led5Alpha)

    val currentOnVibrate by rememberUpdatedState(onVibrate)
    val currentViewModel by rememberUpdatedState(viewModel)

    // Convex dark dome background gradient
    val baseDomeGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF232527),
                Color(0xFF0C0D0E),
                Color(0xFF000000)
            ),
            center = Offset(0.50f, 0.55f),
            radius = 260f
        )
    }

    val pressedDomeGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF1B1D1E),
                Color(0xFF070808),
                Color(0xFF000000)
            ),
            center = Offset(0.50f, 0.58f),
            radius = 260f
        )
    }

    // Recessed magnifier window gradient
    val windowGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF050506),
                Color(0xFF121314)
            ),
            center = Offset(0.50f, 0.60f),
            radius = 70f
        )
    }

    Box(
        modifier = modifier
            .size(154.dp, 54.dp)
            .graphicsLayer {
                scaleX = scaleAnim
                scaleY = scaleAnim
            }
            .offset { IntOffset(0, pressOffsetYAnim.dp.roundToPx()) }
            // Outer drop shadow
            .shadow(
                elevation = if (isPressed) 2.dp else 6.dp,
                shape = bumperShape,
                ambientColor = Color.Black.copy(alpha = 0.40f),
                spotColor = Color.Black.copy(alpha = 0.55f)
            )
            .clip(bumperShape)
            // Convex dark body
            .background(if (isPressed) pressedDomeGradient else baseDomeGradient)
            // 1px metallic rim outline
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.50f),
                        Color.Black.copy(alpha = 0.80f)
                    )
                ),
                shape = bumperShape
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
        // Multi-Layer Canvas: Undercut shadow, Asymmetric Neon Ring, Glass Specular Sheen
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Inset bottom shadow
            val insetH = h * 0.40f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = if (isPressed) 0.85f else 0.70f)),
                    startY = h - insetH,
                    endY = h
                ),
                topLeft = Offset(0f, h - insetH),
                size = Size(w, insetH)
            )

            // Neon color ring — inset 2.5dp with exact asymmetric per-corner radii
            val ringInset = 2.5.dp.toPx()
            val ringROuter = 27.dp.toPx() - ringInset
            val ringRInner = 12.dp.toPx() - ringInset
            val ringPath = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = Rect(ringInset, ringInset, w - ringInset, h - ringInset),
                        topLeft = CornerRadius(if (isLeft) ringRInner else ringROuter),
                        topRight = CornerRadius(if (isLeft) ringROuter else ringRInner),
                        bottomRight = CornerRadius(if (isLeft) ringROuter else ringRInner),
                        bottomLeft = CornerRadius(if (isLeft) ringRInner else ringROuter)
                    )
                )
            }

            // Outer bloom pass
            drawPath(
                path = ringPath,
                color = neonColor.copy(alpha = (if (isPressed) 0.60f else 0.28f) * ringBloomAlpha),
                style = Stroke(width = 3.5.dp.toPx())
            )

            // Core crisp stroke
            drawPath(
                path = ringPath,
                color = neonColor.copy(alpha = ringBloomAlpha),
                style = Stroke(width = 1.8.dp.toPx())
            )

            // Glass lens top specular crescent highlight
            val sheenH = h * 0.42f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isPressed) 0.05f else 0.14f),
                        Color.White.copy(alpha = if (isPressed) 0.01f else 0.04f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = sheenH
                ),
                topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                size = Size(w - 2.dp.toPx(), sheenH)
            )

            // Top edge specular line
            val specStartX = if (isLeft) w * 0.10f else w * 0.18f
            val specEndX = if (isLeft) w * 0.82f else w * 0.90f
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isPressed) 0.12f else 0.45f),
                        Color.White.copy(alpha = if (isPressed) 0.04f else 0.16f)
                    ),
                    startX = specStartX,
                    endX = specEndX
                ),
                start = Offset(specStartX, 4f),
                end = Offset(specEndX, 4f),
                strokeWidth = 1.5f
            )
        }

        // Content Row: Symmetrical arrangement (LB: Window + LEDs / RB: LEDs + Window)
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            val labelText = displayLabel ?: key

            if (isLeft) {
                // LB: Magnifier Window first, then LED bars
                RecessedLabelWindow(
                    label = labelText,
                    neonColor = neonColor,
                    windowGradient = windowGradient
                )

                Box(modifier = Modifier.size(8.dp, 1.dp))

                LedBarGroup(
                    neonColor = neonColor,
                    alphas = ledAlphas
                )
            } else {
                // RB: LED bars first, then Magnifier Window
                LedBarGroup(
                    neonColor = neonColor,
                    alphas = ledAlphas
                )

                Box(modifier = Modifier.size(8.dp, 1.dp))

                RecessedLabelWindow(
                    label = labelText,
                    neonColor = neonColor,
                    windowGradient = windowGradient
                )
            }
        }
    }
}

@Composable
private fun RecessedLabelWindow(
    label: String,
    neonColor: Color,
    windowGradient: Brush,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(48.dp, 28.dp)
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(14.dp),
                ambientColor = Color.Black,
                spotColor = Color.Black
            )
            .clip(RoundedCornerShape(14.dp))
            .background(windowGradient)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.06f),
                shape = RoundedCornerShape(14.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        // Recessed inner vignette overlay and 3D bevel
        Canvas(modifier = Modifier.fillMaxSize()) {
            val winW = size.width
            val winH = size.height

            // Inset top shadow
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Black.copy(alpha = 0.92f), Color.Transparent),
                    startY = 0f,
                    endY = 7.dp.toPx()
                ),
                size = Size(winW, 7.dp.toPx())
            )

            // Vignette
            drawRect(
                brush = Brush.radialGradient(
                    0.0f to Color.Transparent,
                    0.32f to Color.Transparent,
                    1.0f to Color.Black.copy(alpha = 0.85f),
                    center = Offset(winW / 2f, winH / 2f),
                    radius = winW * 0.55f
                )
            )

            // Bottom specular edge line
            drawLine(
                color = Color.White.copy(alpha = 0.06f),
                start = Offset(4.dp.toPx(), winH - 0.5f),
                end = Offset(winW - 4.dp.toPx(), winH - 0.5f),
                strokeWidth = 1.dp.toPx()
            )
        }

        Text(
            text = label,
            color = neonColor,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            letterSpacing = 1.5.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LedBarGroup(
    neonColor: Color,
    alphas: List<Float>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        alphas.forEach { alpha ->
            Box(
                modifier = Modifier
                    .size(9.dp, 22.dp)
                    .clip(RoundedCornerShape(3.5.dp))
                    .background(neonColor.copy(alpha = if (alpha > 0.5f) alpha else 0.22f))
            )
        }
    }
}
