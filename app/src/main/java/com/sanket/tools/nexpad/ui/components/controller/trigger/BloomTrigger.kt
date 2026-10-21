package com.sanket.tools.nexpad.ui.components.controller.trigger

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.model.NexpadKeys
import com.sanket.tools.nexpad.ui.theme.NeonPalette
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel
import kotlin.math.cos
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
    isConnected: Boolean = true,
    onVibrate: () -> Unit = {},
    viewModel: GamepadViewModel? = null,
    isRgbEnabled: Boolean = true,
    modifier: Modifier = Modifier,
    displayLabel: String? = null,
    isInteractive: Boolean = true
) {
    var isPressedLocally by remember { mutableStateOf(false) }
    val isPressed = isPressedLocally || com.sanket.tools.nexpad.ui.components.controller.isControlPressed(key)
    val isLeft = key.uppercase() == NexpadKeys.LT
    // HTML source palette: Electric Periwinkle (#7C9CFF) for LT, Neon Rose (#FF6584) for RT
    val glowColor = if (isLeft) NeonPalette.Periwinkle else NeonPalette.NeonRose

    val currentOnVibrate by rememberUpdatedState(onVibrate)
    val currentViewModel by rememberUpdatedState(viewModel)

    val buttonSize = 92.dp

    // Physical Spring Kinematics — standard console trigger spec
    val scaleAnimState = animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "bloom_scale"
    )
    val rgbBloomAlphaState = animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0.45f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "bloom_bloom"
    )
    val fillProgressState = animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 380f),
        label = "bloom_fill"
    )

    val baseDomeGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                NeonPalette.MetalBase,
                NeonPalette.MetalShadow,
                NeonPalette.PureBlack
            ),
            center = Offset(0.50f, 0.45f),
            radius = 200f
        )
    }
    val pressedDomeGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                NeonPalette.MetalMid,
                NeonPalette.MetalAbyss,
                NeonPalette.PureBlack
            ),
            center = Offset(0.50f, 0.48f),
            radius = 200f
        )
    }

    val chassisBorderBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color.Black.copy(alpha = 0.50f),
                Color.Black.copy(alpha = 0.85f)
            )
        )
    }

    val windowBackgroundBrush = remember {
        Brush.radialGradient(
            colors = listOf(
                NeonPalette.MetalDeep,
                NeonPalette.MetalDark
            ),
            center = Offset(0.50f, 0.60f),
            radius = 45f
        )
    }

    val windowBorderBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color.Black.copy(alpha = 0.9f),
                Color.White.copy(alpha = 0.12f)
            )
        )
    }

    val windowVignetteColors = remember {
        listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
    }

    val lensHighlightColors = remember {
        listOf(
            Color.White.copy(alpha = 0.16f),
            Color.White.copy(alpha = 0.04f),
            Color.Transparent
        )
    }

    val density = LocalDensity.current.density
    val strokeRim = remember(density) { Stroke(width = 1.5f * density) }
    val strokeBloomRimOuter = remember(density) { Stroke(width = 4f * density) }
    val strokeBloomRimCore = remember(density) { Stroke(width = 1.8f * density) }
    val strokeLensArc = remember(density) { Stroke(width = 1.2f * density) }
    val strokeRingBloom = remember(density) { Stroke(width = 2.8f * density) }
    val strokeRingCore = remember(density) { Stroke(width = 1.3f * density) }

    val pupilBloomColors = remember(glowColor) {
        arrayListOf(Color.Transparent, Color.Transparent, Color.Transparent, Color.Transparent)
    }
    val petalColors = remember(glowColor) {
        arrayListOf(Color.Transparent, Color.Transparent, Color.Transparent)
    }
    val bloomRadialColors = remember(glowColor) {
        arrayListOf(Color.Transparent, Color.Transparent, Color.Transparent, Color.Transparent)
    }

    Box(
        modifier = modifier
            .size(buttonSize)
            .graphicsLayer {
                // Root RenderNode isolation
            }
            // Outer dynamic RGB aura
            .drawBehind {
                if (isRgbEnabled) {
                    val fillProgress = fillProgressState.value
                    val rgbBloomAlpha = rgbBloomAlphaState.value
                    val baseR = size.minDimension * 0.5f
                    val bloomExpansion = 0.55f + 0.35f * fillProgress
                    val outerBloomR = size.minDimension * bloomExpansion

                    // 1. Radiant central pupil bloom
                    pupilBloomColors[0] = Color.White.copy(alpha = rgbBloomAlpha * (if (isPressed) 0.80f else 0.40f))
                    pupilBloomColors[1] = glowColor.copy(alpha = rgbBloomAlpha * (if (isPressed) 0.65f else 0.30f))
                    pupilBloomColors[2] = glowColor.copy(alpha = rgbBloomAlpha * 0.15f)
                    pupilBloomColors[3] = Color.Transparent
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = pupilBloomColors,
                            center = center,
                            radius = outerBloomR
                        ),
                        radius = outerBloomR,
                        center = center
                    )

                    // 2. 8 Expanding Mechanical Iris Petal Lobes
                    val petalCount = 8
                    val petalRot = 45f * fillProgress
                    val petalLength = (10f + 16f * fillProgress) * density
                    val petalStartR = baseR - 4f * density

                    for (i in 0 until petalCount) {
                        val angleDeg = i * (360.0 / petalCount) + petalRot
                        val angleRad = Math.toRadians(angleDeg)
                        val cosP = cos(angleRad).toFloat()
                        val sinP = sin(angleRad).toFloat()

                        val startPt = Offset(center.x + petalStartR * cosP, center.y + petalStartR * sinP)
                        val endPt = Offset(center.x + (petalStartR + petalLength) * cosP, center.y + (petalStartR + petalLength) * sinP)
                        val petalAlpha = rgbBloomAlpha * (0.35f + 0.55f * fillProgress)

                        petalColors[0] = glowColor.copy(alpha = petalAlpha)
                        petalColors[1] = Color.White.copy(alpha = petalAlpha * 0.90f)
                        petalColors[2] = Color.Transparent
                        drawLine(
                            brush = Brush.linearGradient(
                                colors = petalColors,
                                start = startPt,
                                end = endPt
                            ),
                            start = startPt,
                            end = endPt,
                            strokeWidth = (2.5f + 2f * fillProgress) * density
                        )
                    }

                    // 3. Expanding circular iris perimeter rim
                    drawCircle(
                        color = glowColor.copy(alpha = rgbBloomAlpha * (if (isPressed) 0.90f else 0.50f)),
                        radius = baseR + 4f * density + 8f * density * fillProgress,
                        center = center,
                        style = strokeRim
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
            .graphicsLayer {
                scaleX = scaleAnimState.value
                scaleY = scaleAnimState.value
            }
            .clip(CircleShape)
            .background(if (isPressed) pressedDomeGradient else baseDomeGradient)
            .border(
                width = 1.dp,
                brush = chassisBorderBrush,
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
        // Layer 1: Radiant Expanding Light Bloom & Expanding Bloom Rim (.bloom & .bloom-rim)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val currentFill = fillProgressState.value
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = size.minDimension / 2f - 4f * density

            if (currentFill > 0.005f) {
                val bloomRadius = (currentFill * maxRadius).coerceAtLeast(4f)
                val bloomAlpha = (currentFill * 2.5f).coerceIn(0f, 1f)

                // Radiant Light Bloom expanding from the center pupil
                bloomRadialColors[0] = Color.White.copy(alpha = 0.90f * bloomAlpha)
                bloomRadialColors[1] = glowColor.copy(alpha = 0.70f * bloomAlpha)
                bloomRadialColors[2] = glowColor.copy(alpha = 0.30f * bloomAlpha)
                bloomRadialColors[3] = Color.Transparent
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = bloomRadialColors,
                        center = center,
                        radius = bloomRadius + 8f * density
                    ),
                    radius = bloomRadius + 8f * density,
                    center = center
                )

                // Expanding Bloom Rim Halo (.bloom-rim)
                val rimRadius = currentFill * (maxRadius - 2f * density)
                if (rimRadius > 2f) {
                    // Outer neon halo
                    drawCircle(
                        color = glowColor.copy(alpha = 0.55f * bloomAlpha),
                        radius = rimRadius,
                        center = center,
                        style = strokeBloomRimOuter
                    )
                    // Crisp core white ring
                    drawCircle(
                        color = Color.White.copy(alpha = 0.85f * bloomAlpha),
                        radius = rimRadius,
                        center = center,
                        style = strokeBloomRimCore
                    )
                }
            }
        }

        // Layer 2: Central Recessed Optical Eye Window (.lx-window)
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(windowBackgroundBrush)
                .border(
                    width = 1.dp,
                    brush = windowBorderBrush,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = windowVignetteColors,
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
                color = glowColor,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer {
                    alpha = 0.85f + fillProgressState.value * 0.15f
                }
            )
        }

        // Layer 3: Specular Glass Lens & Outer Chassis Ring (.lx-lens & .lx-ring)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val rgbBloomAlpha = rgbBloomAlphaState.value
            val r = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            // Top specular crescent arc highlight
            drawArc(
                brush = Brush.verticalGradient(
                    colors = lensHighlightColors,
                    startY = 0f,
                    endY = size.height * 0.38f
                ),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(1.5f * density, 1.5f * density),
                size = Size(size.width - 3f * density, size.height - 3f * density),
                style = strokeLensArc
            )

            // Outer chassis neon ring (.lx-ring)
            val ringRadius = r - 2.5f * density
            drawCircle(
                color = glowColor.copy(alpha = rgbBloomAlpha * 0.55f),
                radius = ringRadius,
                center = center,
                style = strokeRingBloom
            )
            drawCircle(
                color = glowColor.copy(alpha = if (isPressed) 0.95f else 0.60f),
                radius = ringRadius,
                center = center,
                style = strokeRingCore
            )
        }
    }
}

/**
 * Static preview delegate for BloomTrigger with 0 touch overhead and 0 coroutines.
 */
@Composable
fun StaticBloomTrigger(
    key: String = com.sanket.tools.nexpad.category.ControlKey.LT.key,
    isRgbEnabled: Boolean = true,
    displayLabel: String? = null,
    modifier: Modifier = Modifier
) {
    BloomTrigger(
        key = key,
        isConnected = false,
        onVibrate = {},
        viewModel = null,
        isRgbEnabled = isRgbEnabled,
        modifier = modifier,
        displayLabel = displayLabel,
        isInteractive = false
    )
}
