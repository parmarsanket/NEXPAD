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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
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
 * Native Jetpack Compose implementation of the NEXPAD Liquid Orb Trigger (Trigger B — Liquid Orb).
 *
 * Translates the fluid-filled glass sphere HTML blueprint into high-performance Compose:
 * - Sealed glass orb housing (CircleShape) scaled to compact 92dp x 92dp mobile standard
 * - Recessed spherical fluid chamber (.liq-wrap) with deep dark liquid well gradient
 * - Progressive rising fluid level (8% ambient baseline -> 100% full on full press)
 * - Dynamic swaying fluid meniscus crest (.liq::before) that rocks back and forth while held
 * - Recessed optical window (.lx-window) elevated at 34% top position above fluid reservoir
 * - Pure optical layout: strictly omits percentage text readout per mobile design directive
 * - Multi-pass emissive neon lens rings, specular glass reflections, and spring kinematics
 */
@Composable
fun LiquidOrbTrigger(
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

    // Physical Spring Kinematics
    val scaleAnim by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "liquid_orb_scale"
    )
    val pressOffsetYAnim by animateFloatAsState(
        targetValue = if (isPressed) 2.0f else 0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "liquid_orb_offset"
    )
    val rgbBloomAlpha by animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0.55f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 800f),
        label = "liquid_orb_rgb_bloom"
    )
    // Rising liquid fill level: starts at 14% ambient fluid, capped at 80% (0.80f) on full pull
    // so the fluid meniscus remains clearly visible rocking beneath the top dome!
    val fillProgress by animateFloatAsState(
        targetValue = if (isPressed) 0.80f else 0.14f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 380f),
        label = "liquid_orb_fill"
    )

    // Fluid Meniscus Sway Animation (simulating fluid surface rocking back and forth)
    val infiniteTransition = rememberInfiniteTransition(label = "liquid_sway")
    val swayOffsetX by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sway_offset_x"
    )
    val swayScaleY by infiniteTransition.animateFloat(
        initialValue = 0.70f,
        targetValue = 1.30f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sway_scale_y"
    )

    // LT = Turquoise / Teal (#3FD2C4), RT = Coral Pink (#FF5376) harmonic neon contrast pair
    val neonColor = if (isLeft) Color(0xFF3FD2C4) else Color(0xFFFF5376)

    // Acrylic convex dome background (#232527 -> #0c0d0e -> #000)
    val baseDomeGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF232527),
                Color(0xFF0C0D0E),
                Color(0xFF000000)
            ),
            center = Offset(0.50f, 0.45f),
            radius = 210f
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
            radius = 210f
        )
    }

    // Recessed optical window gradient (#050506 -> #121314)
    val windowGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF050506),
                Color(0xFF121314)
            ),
            center = Offset(0.50f, 0.60f),
            radius = 100f
        )
    }

    val currentOnVibrate by rememberUpdatedState(onVibrate)
    val currentViewModel by rememberUpdatedState(viewModel)

    val triggerShape = CircleShape
    val windowShape = RoundedCornerShape(13.dp)

    Box(
        modifier = modifier
            .size(92.dp, 92.dp)
            // Ambient RGB Bloom aura behind orb housing
            .drawBehind {
                if (isRgbEnabled) {
                    drawCircle(
                        color = neonColor.copy(alpha = rgbBloomAlpha * 0.40f),
                        radius = size.minDimension / 2f + 8.dp.toPx()
                    )
                }
            }
            .graphicsLayer {
                scaleX = scaleAnim
                scaleY = scaleAnim
            }
            .offset { IntOffset(0, pressOffsetYAnim.dp.roundToPx()) }
            // Drop shadow with RGB tint
            .shadow(
                elevation = if (isPressed) 2.dp else 6.dp,
                shape = triggerShape,
                spotColor = if (isRgbEnabled) neonColor else Color.Black,
                ambientColor = if (isRgbEnabled) neonColor.copy(alpha = 0.5f) else Color.Black
            )
            .clip(triggerShape)
            .background(if (isPressed) pressedDomeGradient else baseDomeGradient)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.50f),
                        Color.Black.copy(alpha = 0.80f)
                    )
                ),
                shape = triggerShape
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
        // LAYER STACK CANVAS: Fluid Chamber + Rising Liquid + Swaying Meniscus + Lens Arc
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Inset bottom shadow along base
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

            // Outer 1px rim outline
            drawCircle(
                color = Color.Black.copy(alpha = 0.50f),
                radius = (w / 2f) - 0.5f,
                style = Stroke(width = 1.dp.toPx())
            )

            // Multi-pass emissive neon ring (.lx-ring)
            val ringInset = 3.dp.toPx()
            val ringRadius = (w / 2f) - ringInset
            drawCircle(
                color = neonColor.copy(alpha = if (isPressed) 0.55f else 0.25f),
                radius = ringRadius,
                style = Stroke(width = 4.dp.toPx())
            )
            drawCircle(
                color = neonColor.copy(alpha = if (isPressed) 1.0f else 0.70f),
                radius = ringRadius,
                style = Stroke(width = 2.dp.toPx())
            )

            // =========================================================================
            // RECESSED SPHERICAL LIQUID CHAMBER (.liq-wrap)
            // =========================================================================
            val chamberInset = 7.dp.toPx()
            val chamberRadius = (w / 2f) - chamberInset
            val chamberCenter = Offset(w / 2f, h / 2f)

            // Circular clip path isolating liquid inside the chamber
            val chamberPath = Path().apply {
                addOval(
                    androidx.compose.ui.geometry.Rect(
                        center = chamberCenter,
                        radius = chamberRadius
                    )
                )
            }

            clipPath(chamberPath) {
                // Dark internal cavity well
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF0D0E12), Color(0xFF040405)),
                        center = chamberCenter,
                        radius = chamberRadius
                    ),
                    radius = chamberRadius,
                    center = chamberCenter
                )

                // ---------------------------------------------------------------------
                // RISING FLUID BODY (.liq)
                // ---------------------------------------------------------------------
                val chamberDiameter = chamberRadius * 2f
                val liquidHeight = chamberDiameter * fillProgress
                val liquidTopY = (chamberCenter.y + chamberRadius) - liquidHeight
                val liquidBottomY = chamberCenter.y + chamberRadius

                // Layer 1: Ambient deep fluid bloom
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            neonColor.copy(alpha = 0.75f),
                            neonColor.copy(alpha = 0.35f),
                            Color.Transparent
                        ),
                        startY = liquidTopY,
                        endY = liquidBottomY
                    ),
                    topLeft = Offset(chamberCenter.x - chamberRadius, liquidTopY),
                    size = Size(chamberDiameter, liquidHeight)
                )

                // Layer 2: Core luminous fluid radiance
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            neonColor.copy(alpha = 0.50f),
                            Color.Transparent
                        ),
                        startY = liquidTopY,
                        endY = liquidTopY + liquidHeight * 0.60f
                    ),
                    topLeft = Offset(chamberCenter.x - chamberRadius, liquidTopY),
                    size = Size(chamberDiameter, liquidHeight * 0.60f)
                )

                // ---------------------------------------------------------------------
                // GLOWING MENISCUS SURFACE CREST (.liq::before with sway animation)
                // ---------------------------------------------------------------------
                val activeSwayX = if (isPressed) swayOffsetX.dp.toPx() else 0f
                val activeSwayScaleY = if (isPressed) swayScaleY else 1.0f
                val meniscusWidth = chamberDiameter * 1.30f
                val meniscusHeight = 11.dp.toPx() * activeSwayScaleY

                val meniscusLeft = (chamberCenter.x + activeSwayX) - (meniscusWidth / 2f)
                val meniscusTop = liquidTopY - (meniscusHeight / 2f)

                // Meniscus diffuse bloom halo
                drawOval(
                    color = neonColor.copy(alpha = 0.50f),
                    topLeft = Offset(meniscusLeft, meniscusTop - 3.dp.toPx()),
                    size = Size(meniscusWidth, meniscusHeight + 6.dp.toPx())
                )

                // Meniscus core glowing surface oval
                drawOval(
                    color = neonColor.copy(alpha = 0.95f),
                    topLeft = Offset(meniscusLeft, meniscusTop),
                    size = Size(meniscusWidth, meniscusHeight)
                )

                // Meniscus specular wave crest glint
                val glintWidth = meniscusWidth * 0.50f
                val glintHeight = 3.5.dp.toPx() * activeSwayScaleY
                drawOval(
                    color = Color.White.copy(alpha = 0.85f),
                    topLeft = Offset(chamberCenter.x + activeSwayX * 0.6f - (glintWidth / 2f), liquidTopY - (glintHeight / 2f)),
                    size = Size(glintWidth, glintHeight)
                )
            }

            // Top crescent specular highlight arc (.lx-lens)
            val lensInset = 1.dp.toPx()
            drawArc(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isPressed) 0.06f else 0.14f),
                        Color.White.copy(alpha = if (isPressed) 0.02f else 0.05f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = h * 0.38f
                ),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(lensInset, lensInset),
                size = Size(w - lensInset * 2f, h - lensInset * 2f),
                style = Stroke(width = 1.dp.toPx())
            )

            // Bottom-right specular sheen oval
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.06f), Color.Transparent),
                    center = Offset(w * 0.70f, h * 0.78f),
                    radius = w * 0.22f
                ),
                center = Offset(w * 0.70f, h * 0.78f),
                radius = w * 0.22f
            )
        }

        // =========================================================================
        // ELEVATED OPTICAL WINDOW (.lx-window top: 34%)
        // =========================================================================
        Box(
            modifier = Modifier
                .offset { IntOffset(0, (-13).dp.roundToPx()) }
                .size(48.dp, 26.dp)
                .shadow(
                    elevation = 2.dp,
                    shape = windowShape,
                    ambientColor = Color.Black,
                    spotColor = Color.Black
                )
                .clip(windowShape)
                .background(windowGradient)
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.06f),
                    shape = windowShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val winW = size.width
                val winH = size.height

                // Top lip shadow inside optical cavity
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent),
                        startY = 0f,
                        endY = 5.dp.toPx()
                    ),
                    size = Size(winW, 5.dp.toPx())
                )

                // Perimeter shadow vignette (.lx-window::after)
                drawRect(
                    brush = Brush.radialGradient(
                        0.0f to Color.Transparent,
                        0.32f to Color.Transparent,
                        1.0f to Color.Black.copy(alpha = 0.82f),
                        center = Offset(winW / 2f, winH / 2f),
                        radius = winW * 0.50f
                    )
                )
            }

            // High-contrast bold tactical label (LT / RT)
            Text(
                text = displayLabel ?: (if (isLeft) "LT" else "RT"),
                color = neonColor,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                letterSpacing = 1.5.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
