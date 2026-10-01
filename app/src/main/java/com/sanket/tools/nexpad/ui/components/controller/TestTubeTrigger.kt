package com.sanket.tools.nexpad.ui.components.controller

import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.layout.padding
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
import kotlin.math.sin

/**
 * Native Jetpack Compose implementation of the NEXPAD Test Tube Trigger (Trigger H — Test Tube).
 *
 * Operates with standard tactile trigger ergonomics matching console triggers:
 * - Immediate responsive press feedback via [detectTapGestures]
 * - Direct digital wire dispatch via [GamepadViewModel.updateButton]
 * - Damped harmonic spring kinematics: dynamic scale compression, vertical displacement plunge,
 *   and smooth liquid fill surge to 100% on press
 * - Cylindrical glass test tube capsule contour (48dp x 98dp, r=24dp)
 * - Rocking meniscus crest at liquid surface with harmonic sway oscillation
 * - Rising floating bubbles animated inside the active liquid column while pressed
 * - Volumetric measurement graduation scale markings running along the right side
 * - Specular vertical glass gloss strip running along the left side
 * - Recessed optical window near top displaying tactical trigger label (LT / RT)
 */
@Composable
fun TestTubeTrigger(
    key: String,
    isConnected: Boolean,
    onVibrate: () -> Unit,
    viewModel: GamepadViewModel,
    isRgbEnabled: Boolean,
    modifier: Modifier = Modifier,
    displayLabel: String? = null
) {
    var isPressed by remember { mutableStateOf(false) }
    val isLeft = key.uppercase() == NexpadKeys.LT
    // HTML source palette: Amber Orange (#FF9F43) for LT, Violet Purple (#BD5CFF) for RT
    val glowColor = if (isLeft) Color(0xFFFF9F43) else Color(0xFFBD5CFF)

    val currentOnVibrate by rememberUpdatedState(onVibrate)
    val currentViewModel by rememberUpdatedState(viewModel)

    val widthDp = 48.dp
    val heightDp = 98.dp
    val chassisShape = remember { RoundedCornerShape(24.dp) }
    val chamberShape = remember { RoundedCornerShape(19.dp) }
    val windowShape = remember { RoundedCornerShape(10.dp) }

    // Physical Spring Kinematics — standard console trigger spec
    val scaleAnim by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "testtube_scale"
    )
    val pressOffsetYAnim by animateFloatAsState(
        targetValue = if (isPressed) 2.0f else 0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "testtube_offset"
    )
    val rgbBloomAlpha by animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0.45f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 800f),
        label = "testtube_bloom"
    )
    val fillProgress by animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 380f),
        label = "testtube_fill"
    )

    // Meniscus rocking cycle & bubble rising drive
    val infiniteTransition = rememberInfiniteTransition(label = "tube_effects")
    val swayProgress by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "meniscus_sway"
    )
    val bubbleTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bubble_time"
    )

    val baseDomeGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF232527),
                Color(0xFF0C0D0E),
                Color(0xFF000000)
            ),
            center = Offset(0.50f, 0.45f),
            radius = 200f
        )
    }
    val pressedDomeGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF1B1D1E),
                Color(0xFF070808),
                Color(0xFF000000)
            ),
            center = Offset(0.50f, 0.48f),
            radius = 200f
        )
    }

    Box(
        modifier = modifier
            .size(widthDp, heightDp)
            .graphicsLayer {
                scaleX = scaleAnim
                scaleY = scaleAnim
            }
            .offset { IntOffset(0, pressOffsetYAnim.dp.roundToPx()) }
            // Physical black drop shadow
            .shadow(
                elevation = if (isPressed) 2.dp else 6.dp,
                shape = chassisShape,
                ambientColor = Color.Black.copy(alpha = 0.40f),
                spotColor = Color.Black.copy(alpha = 0.55f)
            )
            .clip(chassisShape)
            .background(if (isPressed) pressedDomeGradient else baseDomeGradient)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.50f),
                        Color.Black.copy(alpha = 0.85f)
                    )
                ),
                shape = chassisShape
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
            }
    ) {
        val currentFill = fillProgress

        // Layer 1: Recessed Glass Chamber & Dynamic Rising Liquid
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(5.dp)
                .clip(chamberShape)
                .background(Color(0xFF07080A))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Liquid fill height from bottom
                val liquidHeight = h * currentFill

                if (liquidHeight > 1f) {
                    val liquidTopY = h - liquidHeight

                    // Liquid volume glowing body
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                glowColor.copy(alpha = 0.55f),
                                glowColor.copy(alpha = 0.85f)
                            ),
                            startY = liquidTopY,
                            endY = h
                        ),
                        topLeft = Offset(0f, liquidTopY),
                        size = Size(w, liquidHeight)
                    )

                    // Liquid emissive bloom core
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                glowColor.copy(alpha = 0.35f),
                                glowColor.copy(alpha = 0.70f)
                            ),
                            startY = liquidTopY,
                            endY = h
                        ),
                        topLeft = Offset(2.dp.toPx(), liquidTopY),
                        size = Size(w - 4.dp.toPx(), liquidHeight)
                    )

                    // Swaying Meniscus Crest (.tt-liq::before)
                    val swayOffsetPx = if (isPressed) swayProgress * 3.dp.toPx() else 0f
                    val meniscusHeight = 6.dp.toPx()
                    drawOval(
                        color = glowColor,
                        topLeft = Offset(-w * 0.15f + swayOffsetPx, liquidTopY - meniscusHeight / 2f),
                        size = Size(w * 1.3f, meniscusHeight)
                    )
                    // Meniscus bright specular reflection glint
                    drawOval(
                        color = Color.White.copy(alpha = 0.70f),
                        topLeft = Offset(w * 0.20f + swayOffsetPx, liquidTopY - meniscusHeight * 0.35f),
                        size = Size(w * 0.6f, meniscusHeight * 0.6f)
                    )

                    // Rising floating bubbles (.bubs)
                    if (isPressed || currentFill > 0.1f) {
                        val bubbleOffsetsX = floatArrayOf(0.20f, 0.42f, 0.65f, 0.32f, 0.78f)
                        val bubbleSpeeds = floatArrayOf(1.0f, 1.4f, 0.9f, 1.2f, 1.5f)
                        val bubbleDelays = floatArrayOf(0.0f, 0.25f, 0.5f, 0.75f, 0.1f)

                        for (i in bubbleOffsetsX.indices) {
                            val progress = ((bubbleTime * bubbleSpeeds[i] + bubbleDelays[i]) % 1.0f)
                            val bY = h - (progress * liquidHeight)
                            val wobble = sin(progress * 6.28f * 2f) * 1.8f
                            val bX = w * bubbleOffsetsX[i] + wobble
                            val bRadius = (1.5f + (i % 2) * 0.8f).dp.toPx()

                            if (bY in (liquidTopY + bRadius)..h) {
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.75f),
                                    radius = bRadius,
                                    center = Offset(bX, bY),
                                    style = Stroke(width = 1.dp.toPx())
                                )
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.40f),
                                    radius = bRadius * 0.6f,
                                    center = Offset(bX, bY)
                                )
                            }
                        }
                    }
                }

                // Volumetric Measurement Scale Ticks (.tt-scale) on right edge
                val numTicks = 6
                val scaleStartX = w - 8.dp.toPx()
                val scaleEndX = w - 3.dp.toPx()
                for (i in 0 until numTicks) {
                    val tickY = 16.dp.toPx() + i * ((h - 32.dp.toPx()) / (numTicks - 1))
                    drawLine(
                        color = Color.White.copy(alpha = 0.35f),
                        start = Offset(scaleStartX, tickY),
                        end = Offset(scaleEndX, tickY),
                        strokeWidth = 1.2.dp.toPx()
                    )
                }

                // Vertical Specular Glass Gloss Strip (.tt-gloss) on left edge
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.35f),
                            Color.White.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    ),
                    topLeft = Offset(3.dp.toPx(), 8.dp.toPx()),
                    size = Size(3.dp.toPx(), h - 16.dp.toPx()),
                    cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
                )
            }
        }

        // Layer 2: Recessed Optical Window centered (.lx-window)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (12).dp)
                .size(36.dp, 20.dp)
                .clip(windowShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF050506),
                            Color(0xFF121314)
                        ),
                        center = Offset(0.50f, 0.60f),
                        radius = 40f
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.9f),
                            Color.White.copy(alpha = 0.10f)
                        )
                    ),
                    shape = windowShape
                ),
            contentAlignment = Alignment.Center
        ) {
            val label = displayLabel ?: key
            Text(
                text = label,
                color = glowColor.copy(alpha = 0.85f + currentFill * 0.15f),
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }

        // Layer 3: Specular Glass Lens & Outer Chassis Ring (.lx-lens & .lx-ring)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val rPx = 24.dp.toPx()

            // Top specular crescent highlight
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.16f),
                        Color.White.copy(alpha = 0.04f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = h * 0.30f
                ),
                topLeft = Offset(1.5.dp.toPx(), 1.5.dp.toPx()),
                size = Size(w - 3.dp.toPx(), h * 0.30f),
                cornerRadius = CornerRadius(rPx, rPx)
            )

            // Outer chassis neon ring (.lx-ring)
            val ringInset = 2.dp.toPx()
            val ringCorner = rPx - ringInset
            drawRoundRect(
                color = glowColor.copy(alpha = if (isPressed) 0.55f else 0.22f),
                topLeft = Offset(ringInset - 1f, ringInset - 1f),
                size = Size(w - (ringInset - 1f) * 2f, h - (ringInset - 1f) * 2f),
                cornerRadius = CornerRadius(ringCorner, ringCorner),
                style = Stroke(width = 2.6.dp.toPx())
            )
            drawRoundRect(
                color = glowColor.copy(alpha = if (isPressed) 0.95f else 0.60f),
                topLeft = Offset(ringInset, ringInset),
                size = Size(w - ringInset * 2f, h - ringInset * 2f),
                cornerRadius = CornerRadius(ringCorner, ringCorner),
                style = Stroke(width = 1.2.dp.toPx())
            )
        }
    }
}
