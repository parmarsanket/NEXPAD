package com.sanket.tools.nexpad.ui.components.controller.bumper

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
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
import com.sanket.tools.nexpad.ui.theme.NeonPalette
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel
import kotlin.math.roundToInt

/**
 * NEXPAD Bumper D — Ribbed (LB / RB / L1 / R1)
 *
 * Implements the Bumper D "Ribbed" design from HTML_TO_COMPOSE_CONTROLLER_BLUEPRINT.md:
 * - Mobile-ergonomic contour (154dp × 54dp): 12dp squarer corner hugs outer screen bezel,
 *   27dp aerodynamic rounded curve points inward toward thumb reach.
 * - Tactile ribbed knurling: fine 2dp vertical micro-grooves repeating across actuator body.
 * - Elevated optical window (74dp × 30dp) with recessed vignette well and bold glyph.
 * - Lower illuminated neon lightbar strip (110dp × 4dp) that ignites on press.
 * - Emissive neon perimeter ring with dual-pass halo bloom and optical glass lens.
 * - Physical spring kinematics (stiffness 440, damping 0.68) with haptic feedback.
 */
@Composable
fun RibbedBumper(
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
        upper == NexpadKeys.LB || upper == "L1" || upper == "LEFT"
    }

    // Outer contour: Flipped mobile ergonomics — 12dp squarer curve hugs outer phone bezel, 27dp curve points inward
    val bumperShape = remember(isLeft) {
        if (isLeft) {
            RoundedCornerShape(topStart = 12.dp, topEnd = 27.dp, bottomEnd = 27.dp, bottomStart = 12.dp)
        } else {
            RoundedCornerShape(topStart = 27.dp, topEnd = 12.dp, bottomEnd = 12.dp, bottomStart = 27.dp)
        }
    }

    // Kinematic Physics Engine — Damped Harmonic Spring
    val scaleAnimState = animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "rib_bumper_scale"
    )
    val pressOffsetYAnimState = animateFloatAsState(
        targetValue = if (isPressed) 2.0f else 0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "rib_bumper_offset"
    )
    val ringBloomAlphaState = animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0.70f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 800f),
        label = "rib_ring_bloom"
    )
    val rgbBloomAlphaState = animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0.45f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "rib_bumper_rgb_bloom"
    )
    val stripAlphaAnimState = animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0.22f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 600f),
        label = "rib_strip_alpha"
    )

    // CSS: --glow: #3fd2ff (Cyan neon)
    val neonColor = remember(isRgbEnabled, isLeft) {
        if (isRgbEnabled) {
            if (isLeft) NeonPalette.SkyBlue else NeonPalette.Magenta
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

    // Recessed 3D window background
    val windowGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                NeonPalette.MetalDeep,
                NeonPalette.MetalDark
            ),
            center = Offset(0.50f, 0.60f),
            radius = 120f
        )
    }

    val density = androidx.compose.ui.platform.LocalDensity.current.density
    val strokeRingBloom = remember(density) { Stroke(width = 4f * density) }
    val strokeRingCore = remember(density) { Stroke(width = 1.8f * density) }
    val borderBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color.Black.copy(alpha = 0.50f),
                Color.Black.copy(alpha = 0.80f)
            )
        )
    }
    val undercutShadowColorsNormal = remember { listOf(Color.Transparent, Color.Black.copy(alpha = 0.70f)) }
    val undercutShadowColorsPressed = remember { listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)) }
    val lensColorsNormal = remember {
        listOf(
            Color.White.copy(alpha = 0.14f),
            Color.White.copy(alpha = 0.04f),
            Color.Transparent
        )
    }
    val lensColorsPressed = remember {
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
    val sheenCircleBrush = remember(density) {
        val w = 154f * density
        val h = 54f * density
        Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.06f), Color.Transparent),
            center = Offset(w * 0.70f, h * 0.78f),
            radius = w * 0.20f
        )
    }
    val windowShape = remember { RoundedCornerShape(15.dp) }
    val stripShape = remember { RoundedCornerShape(2.dp) }
    val winTopShadowBrush = remember(density) {
        Brush.verticalGradient(
            colors = listOf(Color.Black.copy(alpha = 0.90f), Color.Transparent),
            startY = 0f,
            endY = 6f * density
        )
    }
    val winVignetteBrush = remember(density) {
        val winW = 74f * density
        Brush.radialGradient(
            0.0f to Color.Transparent,
            0.32f to Color.Transparent,
            1.0f to Color.Black.copy(alpha = 0.82f),
            center = Offset(winW / 2f, 15f * density),
            radius = winW * 0.50f
        )
    }

    val ringPath = remember(density, isLeft) {
        val w = 154f * density
        val h = 54f * density
        val ringInset = 2.5f * density
        val ringROuter = 27f * density - ringInset
        val ringRInner = 12f * density - ringInset
        Path().apply {
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
    }

    val stadiumBloomColors = remember(neonColor) {
        arrayListOf(Color.Transparent, Color.Transparent, Color.Transparent)
    }
    val washBloomColors = remember(neonColor) {
        arrayListOf(Color.Transparent, Color.Transparent)
    }
    val teethColorsList = remember(neonColor) {
        arrayListOf(Color.Transparent, Color.Transparent, Color.Transparent)
    }

    Box(
        modifier = modifier
            .size(154.dp, 54.dp)
            .graphicsLayer {
                // Root RenderNode isolation
            }
            .drawBehind {
                if (isRgbEnabled) {
                    val rgbBloomAlpha = rgbBloomAlphaState.value
                    val stripAlphaAnim = stripAlphaAnimState.value
                    val padX = 14.dp.toPx()
                    val padY = 8.dp.toPx()

                    // 1. Base stadium glow
                    stadiumBloomColors[0] = neonColor.copy(alpha = rgbBloomAlpha * (if (isPressed) 0.52f else 0.32f))
                    stadiumBloomColors[1] = neonColor.copy(alpha = rgbBloomAlpha * 0.12f)
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

                    // 2. Striated diffraction grating light curtain along bottom strip
                    val stripWidth = 110.dp.toPx()
                    val stripStartX = center.x - stripWidth / 2f
                    val stripY = size.height - 9.dp.toPx()
                    val ribCount = 14
                    val ribStep = stripWidth / (ribCount - 1)
                    val ribSpikeH = (if (isPressed) 14.dp else 7.dp).toPx()

                    // Diffuse bottom light curtain wash
                    washBloomColors[0] = neonColor.copy(alpha = rgbBloomAlpha * stripAlphaAnim * (if (isPressed) 0.70f else 0.35f))
                    washBloomColors[1] = Color.Transparent
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = washBloomColors,
                            startY = stripY,
                            endY = stripY + ribSpikeH * 1.5f
                        ),
                        topLeft = Offset(stripStartX - 6.dp.toPx(), stripY),
                        size = Size(stripWidth + 12.dp.toPx(), ribSpikeH * 1.5f)
                    )

                    // Striated vertical diffraction teeth
                    for (i in 0 until ribCount) {
                        val rx = stripStartX + i * ribStep
                        val isMajorRib = i % 2 == 0
                        val teethH = if (isMajorRib) ribSpikeH else ribSpikeH * 0.65f
                        val teethAlpha = rgbBloomAlpha * stripAlphaAnim * (if (isMajorRib) 0.85f else 0.50f)

                        teethColorsList[0] = Color.White.copy(alpha = teethAlpha)
                        teethColorsList[1] = neonColor.copy(alpha = teethAlpha * 0.80f)
                        teethColorsList[2] = Color.Transparent
                        drawLine(
                            brush = Brush.verticalGradient(
                                colors = teethColorsList,
                                startY = stripY,
                                endY = stripY + teethH
                            ),
                            start = Offset(rx, stripY),
                            end = Offset(rx, stripY + teethH),
                            strokeWidth = if (isMajorRib) 2.5f else 1.5f
                        )
                    }
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
                translationY = pressOffsetYAnimState.value * density
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
        // Multi-Layer Canvas: Undercut shadow, Ribbed texture lines, Neon Ring, Lens highlights
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

            // Tactile Ribbed knurling (repeating vertical micro-grooves: 2px rib every 8px)
            val ribSpacing = 8.dp.toPx()
            val ribWidth = 2.dp.toPx()
            val ribAlpha = if (isPressed) 0.035f else 0.055f
            var curX = 6.dp.toPx()
            while (curX < w - 6.dp.toPx()) {
                drawLine(
                    color = Color.White.copy(alpha = ribAlpha),
                    start = Offset(curX, 0f),
                    end = Offset(curX, h),
                    strokeWidth = ribWidth
                )
                curX += ribSpacing
            }

            val ringBloomAlpha = ringBloomAlphaState.value
            // Outer bloom pass
            drawPath(
                path = ringPath,
                color = neonColor.copy(alpha = (if (isPressed) 0.60f else 0.28f) * ringBloomAlpha),
                style = strokeRingBloom
            )
            // Core crisp stroke
            drawPath(
                path = ringPath,
                color = neonColor.copy(alpha = (if (isPressed) 1.0f else 0.70f) * ringBloomAlpha),
                style = strokeRingCore
            )

            // Top crescent specular sheen (glass lens)
            val ringInset = 2.5.dp.toPx()
            val sheenH = h * 0.40f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = if (isPressed) lensColorsPressed else lensColorsNormal,
                    startY = 0f,
                    endY = sheenH
                ),
                topLeft = Offset(ringInset, ringInset),
                size = Size(w - ringInset * 2f, sheenH)
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
                start = Offset(specStartX, 4f),
                end = Offset(specEndX, 4f),
                strokeWidth = 1.5f
            )

            // Bottom-right specular sheen circle
            drawCircle(
                brush = sheenCircleBrush,
                center = Offset(w * 0.70f, h * 0.78f),
                radius = w * 0.20f
            )
        }

        // Elevated Recessed Magnifier Window (.lx-window: 74dp × 30dp, top: 42%)
        Box(
            modifier = Modifier
                .offset(y = (-4).dp)
                .size(74.dp, 30.dp)
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
                    color = Color.White.copy(alpha = 0.05f),
                    shape = windowShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val winW = size.width
                val winH = size.height

                // Inset top shadow
                drawRect(
                    brush = winTopShadowBrush,
                    size = Size(winW, 6.dp.toPx())
                )

                // Deep vignette
                drawRect(
                    brush = winVignetteBrush
                )

                // Bottom specular edge line
                drawLine(
                    color = Color.White.copy(alpha = 0.05f),
                    start = Offset(4.dp.toPx(), winH - 0.5f),
                    end = Offset(winW - 4.dp.toPx(), winH - 0.5f),
                    strokeWidth = 1.dp.toPx()
                )
            }

            val labelText = displayLabel ?: key
            Text(
                text = labelText,
                color = neonColor,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )
        }

        // Lower Neon Lightbar Accent Strip (.strip: height 4dp, border-radius 2dp, bottom 9dp)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 9.dp)
                .size(width = 110.dp, height = 4.dp)
                .graphicsLayer {
                    alpha = stripAlphaAnimState.value
                }
                .clip(stripShape)
                .background(neonColor)
        )
    }
}

/**
 * Static preview delegate for RibbedBumper with 0 touch overhead and 0 coroutines.
 */
@Composable
fun StaticRibbedBumper(
    key: String = "LB",
    isRgbEnabled: Boolean = true,
    displayLabel: String? = null,
    modifier: Modifier = Modifier
) {
    RibbedBumper(
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
