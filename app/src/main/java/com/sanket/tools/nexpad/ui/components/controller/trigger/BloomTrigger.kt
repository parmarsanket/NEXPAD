package com.sanket.tools.nexpad.ui.components.controller.trigger

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Native Jetpack Compose implementation of the NEXPAD Bloom Trigger (Trigger I — Bloom).
 *
 * Operates with standard tactile trigger ergonomics matching console triggers:
 * - Immediate responsive press feedback via [detectTapGestures]
 * - Direct digital wire dispatch via [GamepadViewModel.updateButton]
 * - Damped harmonic spring kinematics: dynamic scale compression, vertical displacement plunge,
 *   and smooth light bloom expansion to 100% on press
 * - Circular trigger contour (92dp x 92dp, CircleShape, matching uniform mobile trigger clearance)
 * - Central pupil core emitting an expanding radial bloom of radiant light as trigger is pressed
 * - Expanding bright circular bloom rim (halo ring) growing outward with pull depth
 * - Central optical eye window with radial edge vignette displaying trigger label (LT / RT)
 * - Multi-pass chassis neon ring and top specular crescent lens highlight
 */
@Composable
fun BloomTrigger(
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
    // HTML source palette: Electric Periwinkle (#7C9CFF) for LT, Neon Rose (#FF6584) for RT
    val glowColor = if (isLeft) Color(0xFF7C9CFF) else Color(0xFFFF6584)

    val currentOnVibrate by rememberUpdatedState(onVibrate)
    val currentViewModel by rememberUpdatedState(viewModel)

    val buttonSize = 92.dp

    // Physical Spring Kinematics — standard console trigger spec
    val scaleAnim by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "bloom_scale"
    )
    val pressOffsetYAnim by animateFloatAsState(
        targetValue = if (isPressed) 2.0f else 0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "bloom_offset"
    )
    val rgbBloomAlpha by animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0.45f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "bloom_bloom"
    )
    val fillProgress by animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 380f),
        label = "bloom_fill"
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
            .size(buttonSize)
            .graphicsLayer {
                scaleX = scaleAnim
                scaleY = scaleAnim
            }
            .offset { IntOffset(0, pressOffsetYAnim.dp.roundToPx()) }
            // Outer dynamic RGB aura
            .drawBehind {
                if (isRgbEnabled) {
                    val baseR = size.minDimension * 0.5f
                    val bloomExpansion = 0.55f + 0.35f * fillProgress
                    val outerBloomR = size.minDimension * bloomExpansion

                    // 1. Radiant central pupil bloom
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = rgbBloomAlpha * (if (isPressed) 0.80f else 0.40f)),
                                glowColor.copy(alpha = rgbBloomAlpha * (if (isPressed) 0.65f else 0.30f)),
                                glowColor.copy(alpha = rgbBloomAlpha * 0.15f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = outerBloomR
                        ),
                        radius = outerBloomR,
                        center = center
                    )

                    // 2. 8 Expanding Mechanical Iris Petal Lobes
                    val petalCount = 8
                    val petalRot = 45f * fillProgress
                    val petalLength = (10.dp + 16.dp * fillProgress).toPx()
                    val petalStartR = baseR - 4.dp.toPx()

                    for (i in 0 until petalCount) {
                        val angleDeg = i * (360.0 / petalCount) + petalRot
                        val angleRad = Math.toRadians(angleDeg)
                        val cosP = cos(angleRad).toFloat()
                        val sinP = sin(angleRad).toFloat()

                        val startPt = Offset(center.x + petalStartR * cosP, center.y + petalStartR * sinP)
                        val endPt = Offset(center.x + (petalStartR + petalLength) * cosP, center.y + (petalStartR + petalLength) * sinP)
                        val petalAlpha = rgbBloomAlpha * (0.35f + 0.55f * fillProgress)

                        drawLine(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    glowColor.copy(alpha = petalAlpha),
                                    Color.White.copy(alpha = petalAlpha * 0.90f),
                                    Color.Transparent
                                ),
                                start = startPt,
                                end = endPt
                            ),
                            start = startPt,
                            end = endPt,
                            strokeWidth = (2.5f + 2f * fillProgress).dp.toPx()
                        )
                    }

                    // 3. Expanding circular iris perimeter rim
                    drawCircle(
                        color = glowColor.copy(alpha = rgbBloomAlpha * (if (isPressed) 0.90f else 0.50f)),
                        radius = baseR + 4.dp.toPx() + 8.dp.toPx() * fillProgress,
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
            }
            // RGB-coordinated shadow
            .shadow(
                elevation = if (isPressed) 2.dp else 6.dp,
                shape = CircleShape,
                ambientColor = if (isRgbEnabled) glowColor else Color.Black,
                spotColor = if (isRgbEnabled) glowColor else Color.Black
            )
            .clip(CircleShape)
            .background(if (isPressed) pressedDomeGradient else baseDomeGradient)
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
        val currentFill = fillProgress

        // Layer 1: Radiant Expanding Light Bloom & Expanding Bloom Rim (.bloom & .bloom-rim)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = size.minDimension / 2f - 4.dp.toPx()

            if (currentFill > 0.005f) {
                val bloomRadius = (currentFill * maxRadius).coerceAtLeast(4f)
                val bloomAlpha = (currentFill * 2.5f).coerceIn(0f, 1f)

                // Radiant Light Bloom expanding from the center pupil
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.90f * bloomAlpha),
                            glowColor.copy(alpha = 0.70f * bloomAlpha),
                            glowColor.copy(alpha = 0.30f * bloomAlpha),
                            Color.Transparent
                        ),
                        center = center,
                        radius = bloomRadius + 8.dp.toPx()
                    ),
                    radius = bloomRadius + 8.dp.toPx(),
                    center = center
                )

                // Expanding Bloom Rim Halo (.bloom-rim)
                val rimRadius = currentFill * (maxRadius - 2.dp.toPx())
                if (rimRadius > 2f) {
                    // Outer neon halo
                    drawCircle(
                        color = glowColor.copy(alpha = 0.55f * bloomAlpha),
                        radius = rimRadius,
                        center = center,
                        style = Stroke(width = 4.dp.toPx())
                    )
                    // Crisp core white ring
                    drawCircle(
                        color = Color.White.copy(alpha = 0.85f * bloomAlpha),
                        radius = rimRadius,
                        center = center,
                        style = Stroke(width = 1.8.dp.toPx())
                    )
                }
            }
        }

        // Layer 2: Central Recessed Optical Eye Window (.lx-window)
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF050506),
                            Color(0xFF121314)
                        ),
                        center = Offset(0.50f, 0.60f),
                        radius = 45f
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.9f),
                            Color.White.copy(alpha = 0.12f)
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = size.minDimension * 0.55f
                    ),
                    radius = size.minDimension / 2f,
                    center = Offset(size.width / 2f, size.height / 2f)
                )
            }

            val label = displayLabel ?: key
            Text(
                text = label,
                color = glowColor.copy(alpha = 0.85f + currentFill * 0.15f),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }

        // Layer 3: Specular Glass Lens & Outer Chassis Ring (.lx-lens & .lx-ring)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            // Top specular crescent arc highlight
            drawArc(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.16f),
                        Color.White.copy(alpha = 0.04f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = size.height * 0.38f
                ),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(1.5.dp.toPx(), 1.5.dp.toPx()),
                size = Size(size.width - 3.dp.toPx(), size.height - 3.dp.toPx()),
                style = Stroke(width = 1.2.dp.toPx())
            )

            // Outer chassis neon ring (.lx-ring)
            val ringRadius = r - 2.5.dp.toPx()
            drawCircle(
                color = glowColor.copy(alpha = rgbBloomAlpha * 0.55f),
                radius = ringRadius,
                center = center,
                style = Stroke(width = 2.8.dp.toPx())
            )
            drawCircle(
                color = glowColor.copy(alpha = if (isPressed) 0.95f else 0.60f),
                radius = ringRadius,
                center = center,
                style = Stroke(width = 1.3.dp.toPx())
            )
        }
    }
}
