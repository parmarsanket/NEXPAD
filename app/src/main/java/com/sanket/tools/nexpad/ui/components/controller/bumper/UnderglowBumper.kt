package com.sanket.tools.nexpad.ui.components.controller.bumper

import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.model.NexpadKeys
import com.sanket.tools.nexpad.ui.theme.NeonPalette
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel
import kotlin.math.roundToInt

/**
 * NEXPAD Bumper E — Underglow (LB / RB / L1 / R1)
 *
 * Implements the Bumper E "Underglow" design from HTML_TO_COMPOSE_CONTROLLER_BLUEPRINT.md:
 * - Mobile-ergonomic contour (154dp × 54dp): 12dp top outer corner, 14dp top inner,
 *   with an aerodynamic 27dp rounded bottom hull.
 * - Bottom Neon Underglow Bar: Horizontal ground strip (3dp thick, 12dp margin) emitting
 *   radiant downward neon ground illumination.
 * - Dynamic Neon Surge Flood: Linear color wash surges upwards (0% to 100% height) on press,
 *   flooding the dark acrylic body with glowing light.
 * - High-Contrast Centered Typography: Bold glyph (24sp) centered directly on the actuator body.
 * - Optical Glass Lens: Top crescent highlight, specular edge line, and bottom-right sheen.
 * - Physical spring kinematics (stiffness 440, damping 0.68) with haptic feedback.
 */
@Composable
fun UnderglowBumper(
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
    val isLeft = remember(key) {
        val upper = key.uppercase()
        upper == NexpadKeys.LB || upper == NexpadKeys.L1 || upper == com.sanket.tools.nexpad.category.ControlKey.LEFT.key
    }

    // Outer contour: 14dp top shoulder, 27dp aerodynamic bottom hull
    val bumperShape = remember(isLeft) {
        if (isLeft) {
            RoundedCornerShape(topStart = 12.dp, topEnd = 14.dp, bottomEnd = 27.dp, bottomStart = 20.dp)
        } else {
            RoundedCornerShape(topStart = 14.dp, topEnd = 12.dp, bottomEnd = 20.dp, bottomStart = 27.dp)
        }
    }

    // Kinematic Physics Engine — Damped Harmonic Spring
    val scaleAnimState = animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "under_bumper_scale"
    )
    val pressOffsetYAnimState = animateFloatAsState(
        targetValue = if (isPressed) 2.0f else 0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "under_bumper_offset"
    )

    // Dynamic Neon Flood surge: sweeps from 0% to 100% height on press
    val floodHeightProgressState = animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "under_flood_height"
    )

    // Underglow ground bar bloom intensity
    val underglowBloomAlphaState = animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0.85f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 600f),
        label = "underglow_bloom"
    )

    val rgbBloomAlphaState = animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0.45f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "under_bumper_rgb_bloom"
    )

    // CSS: --glow: #5cf29a (Neon Mint) for LB, Hot Coral for RB
    val neonColor = remember(isRgbEnabled, isLeft) {
        if (isRgbEnabled) {
            if (isLeft) NeonPalette.MintGreen else NeonPalette.ErrorRed
        } else {
            NeonPalette.MetalLight
        }
    }

    val currentOnVibrate by rememberUpdatedState(onVibrate)
    val currentViewModel by rememberUpdatedState(viewModel)

    // Convex dark dome background gradient
    val baseDomeGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                NeonPalette.MetalBase,
                NeonPalette.MetalShadow,
                NeonPalette.PureBlack
            ),
            center = Offset(0.50f, 0.55f),
            radius = 260f
        )
    }

    val pressedDomeGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                NeonPalette.MetalMid,
                NeonPalette.MetalAbyss,
                NeonPalette.PureBlack
            ),
            center = Offset(0.50f, 0.58f),
            radius = 260f
        )
    }

    val stadiumBloomColors = remember(neonColor) {
        arrayListOf(Color.Transparent, Color.Transparent, Color.Transparent)
    }
    val puddleBloomColors = remember(neonColor) {
        arrayListOf(Color.Transparent, Color.Transparent, Color.Transparent)
    }
    val coreSlitColors = remember(neonColor) {
        arrayListOf(Color.Transparent, Color.Transparent, Color.Transparent, Color.Transparent, Color.Transparent)
    }

    val density = LocalDensity.current
    val borderBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color.Black.copy(alpha = 0.50f),
                Color.Black.copy(alpha = 0.80f)
            )
        )
    }

    val undercutShadowColorsNormal = remember {
        listOf(Color.Transparent, Color.Black.copy(alpha = 0.70f))
    }
    val undercutShadowColorsPressed = remember {
        listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
    }

    val floodBloomColors = remember(neonColor) {
        arrayListOf(Color.Transparent, Color.Transparent)
    }

    val sheenColorsNormal = remember {
        listOf(
            Color.White.copy(alpha = 0.14f),
            Color.White.copy(alpha = 0.04f),
            Color.Transparent
        )
    }
    val sheenColorsPressed = remember {
        listOf(
            Color.White.copy(alpha = 0.05f),
            Color.White.copy(alpha = 0.01f),
            Color.Transparent
        )
    }

    val specLineColorsNormal = remember {
        listOf(
            Color.White.copy(alpha = 0.45f),
            Color.White.copy(alpha = 0.16f)
        )
    }
    val specLineColorsPressed = remember {
        listOf(
            Color.White.copy(alpha = 0.12f),
            Color.White.copy(alpha = 0.04f)
        )
    }

    val sheenCircleColors = remember {
        listOf(Color.White.copy(alpha = 0.06f), Color.Transparent)
    }

    val barCornerRadiusOuter = remember(density) {
        val r = with(density) { 4.dp.toPx() }
        CornerRadius(r, r)
    }
    val barCornerRadiusInner = remember(density) {
        val r = with(density) { 2.dp.toPx() }
        CornerRadius(r, r)
    }

    Box(
        modifier = modifier
            .size(154.dp, 54.dp)
            .graphicsLayer {
                // 1. Root RenderNode isolation: prevents screen-wide invalidation
            }
            .drawBehind {
                if (isRgbEnabled) {
                    val rgbBloomAlpha = rgbBloomAlphaState.value
                    val floodHeightProgress = floodHeightProgressState.value
                    val underglowBloomAlpha = underglowBloomAlphaState.value
                    val padX = 14.dp.toPx()
                    val padY = 6.dp.toPx()

                    // 1. Ambient upper hull aura
                    stadiumBloomColors[0] = neonColor.copy(alpha = rgbBloomAlpha * (if (isPressed) 0.45f else 0.25f))
                    stadiumBloomColors[1] = neonColor.copy(alpha = rgbBloomAlpha * 0.10f)
                    stadiumBloomColors[2] = Color.Transparent
                    drawRoundRect(
                        brush = Brush.radialGradient(
                            colors = stadiumBloomColors,
                            center = center,
                            radius = size.width * 0.55f
                        ),
                        topLeft = Offset(-padX, -padY),
                        size = Size(size.width + padX * 2f, size.height + padY * 2f),
                        cornerRadius = CornerRadius(22.dp.toPx(), 22.dp.toPx())
                    )

                    // 2. Automotive Ground-Effect Floor Wash Puddle beneath bottom hull
                    val puddleW = size.width * (0.85f + 0.25f * floodHeightProgress)
                    val puddleH = (22.dp + 12.dp * floodHeightProgress).toPx()
                    val puddleCenterY = size.height + 4.dp.toPx()
                    val puddleLeft = center.x - puddleW / 2f
                    val puddleAlpha = rgbBloomAlpha * underglowBloomAlpha * (if (isPressed) 0.90f else 0.60f)

                    // Soft diffuse ground reflection oval
                    puddleBloomColors[0] = neonColor.copy(alpha = puddleAlpha * 0.85f)
                    puddleBloomColors[1] = neonColor.copy(alpha = puddleAlpha * 0.35f)
                    puddleBloomColors[2] = Color.Transparent
                    drawOval(
                        brush = Brush.radialGradient(
                            colors = puddleBloomColors,
                            center = Offset(center.x, puddleCenterY),
                            radius = puddleW * 0.55f
                        ),
                        topLeft = Offset(puddleLeft, puddleCenterY - puddleH / 2f),
                        size = Size(puddleW, puddleH)
                    )

                    // High-intensity core underglow slit reflection
                    val coreSlitW = puddleW * 0.65f
                    val coreSlitLeft = center.x - coreSlitW / 2f
                    coreSlitColors[0] = Color.Transparent
                    coreSlitColors[1] = Color.White.copy(alpha = puddleAlpha * 0.90f)
                    coreSlitColors[2] = neonColor.copy(alpha = puddleAlpha * 0.95f)
                    coreSlitColors[3] = Color.White.copy(alpha = puddleAlpha * 0.90f)
                    coreSlitColors[4] = Color.Transparent
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = coreSlitColors,
                            startX = coreSlitLeft,
                            endX = coreSlitLeft + coreSlitW
                        ),
                        start = Offset(coreSlitLeft, size.height),
                        end = Offset(coreSlitLeft + coreSlitW, size.height),
                        strokeWidth = (if (isPressed) 3.5.dp else 2.dp).toPx()
                    )
                }
            }
            // Outer drop shadow
            .shadow(
                elevation = if (isPressed) 1.dp else 4.dp,
                shape = bumperShape,
                ambientColor = if (isRgbEnabled) neonColor else Color.Black,
                spotColor = if (isRgbEnabled) neonColor else Color.Black
            )
            .graphicsLayer {
                scaleX = scaleAnimState.value
                scaleY = scaleAnimState.value
                translationY = pressOffsetYAnimState.value * density.density
            }
            .clip(bumperShape)
            .background(if (isPressed) pressedDomeGradient else baseDomeGradient)
            // 1px casing rim
            .border(
                width = 1.dp,
                brush = borderBrush,
                shape = bumperShape
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
        // Multi-Layer Canvas: Undercut shadow, Dynamic Neon Surge Flood, Optical Glass highlights
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Inset bottom shadow
            val insetH = h * 0.40f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = if (isPressed) undercutShadowColorsPressed else undercutShadowColorsNormal,
                    startY = h - insetH,
                    endY = h
                ),
                topLeft = Offset(0f, h - insetH),
                size = Size(w, insetH)
            )

            // Dynamic Neon Surge Flood: Linear gradient surging upwards from the bottom ground bar
            val floodHeightProgress = floodHeightProgressState.value
            if (floodHeightProgress > 0f) {
                val currentFloodH = h * floodHeightProgress
                floodBloomColors[0] = Color.Transparent
                floodBloomColors[1] = neonColor.copy(alpha = 0.55f * floodHeightProgress)
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = floodBloomColors,
                        startY = h - currentFloodH,
                        endY = h
                    ),
                    topLeft = Offset(0f, h - currentFloodH),
                    size = Size(w, currentFloodH)
                )
            }

            // Top crescent specular sheen (optical glass lens)
            val sheenH = h * 0.42f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = if (isPressed) sheenColorsPressed else sheenColorsNormal,
                    startY = 0f,
                    endY = sheenH
                ),
                topLeft = Offset(2.dp.toPx(), 2.dp.toPx()),
                size = Size(w - 4.dp.toPx(), sheenH)
            )

            // Top edge specular line
            val specStartX = if (isLeft) w * 0.15f else w * 0.12f
            val specEndX = if (isLeft) w * 0.88f else w * 0.85f
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = if (isPressed) specLineColorsPressed else specLineColorsNormal,
                    startX = specStartX,
                    endX = specEndX
                ),
                start = Offset(specStartX, 3.5f),
                end = Offset(specEndX, 3.5f),
                strokeWidth = 1.5f
            )

            // Bottom-right specular sheen circle
            drawCircle(
                brush = Brush.radialGradient(
                    colors = sheenCircleColors,
                    center = Offset(w * 0.70f, h * 0.78f),
                    radius = w * 0.20f
                ),
                center = Offset(w * 0.70f, h * 0.78f),
                radius = w * 0.20f
            )
        }

        // Direct Centered Typography (.lx-g: 24sp, bold, high contrast over flood wash)
        val labelText = displayLabel ?: key
        Text(
            text = labelText,
            color = neonColor,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            letterSpacing = 2.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.graphicsLayer {
                val glyphScale = if (isPressed) 1.05f else 1.0f
                scaleX = glyphScale
                scaleY = glyphScale
            }
        )

        // Bottom Neon Underglow Bar (.lx-ring in Underglow mode: inset bottom 3dp, left/right 12dp, height 3dp)
        Canvas(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxSize()
        ) {
            val underglowBloomAlpha = underglowBloomAlphaState.value
            val barInsetX = 12.dp.toPx()
            val barBottomY = size.height - 3.dp.toPx()
            val barH = 3.dp.toPx()
            val barW = size.width - barInsetX * 2f

            // Outer wide ground bloom
            drawRoundRect(
                color = neonColor.copy(alpha = (if (isPressed) 0.65f else 0.35f) * underglowBloomAlpha),
                topLeft = Offset(barInsetX - 4.dp.toPx(), barBottomY - 2.dp.toPx()),
                size = Size(barW + 8.dp.toPx(), barH + 4.dp.toPx()),
                cornerRadius = barCornerRadiusOuter
            )

            // Core crisp ground bar
            drawRoundRect(
                color = neonColor.copy(alpha = (if (isPressed) 1.0f else 0.85f) * underglowBloomAlpha),
                topLeft = Offset(barInsetX, barBottomY),
                size = Size(barW, barH),
                cornerRadius = barCornerRadiusInner
            )
        }
    }
}

/**
 * Static preview delegate for UnderglowBumper with 0 touch overhead and 0 coroutines.
 */
@Composable
fun StaticUnderglowBumper(
    key: String = com.sanket.tools.nexpad.category.ControlKey.LB.key,
    isRgbEnabled: Boolean = true,
    displayLabel: String? = null,
    modifier: Modifier = Modifier
) {
    UnderglowBumper(
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
