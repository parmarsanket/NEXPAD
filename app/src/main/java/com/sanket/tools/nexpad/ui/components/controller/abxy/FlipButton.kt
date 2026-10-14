package com.sanket.tools.nexpad.ui.components.controller.abxy

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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.category.CategoryManager
import com.sanket.tools.nexpad.category.ControllerLabelStyle
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel
import kotlin.math.abs

/**
 * Flip Card ABXY button variant.
 * Faithful reproduction of HTML/CSS #5 Flip:
 * In idle state, shows the dark acrylic face with neon letter.
 * On press, the inner card flips in 3D (perspective 420px, rotateY 180° with overshoot bounce)
 * revealing a solid lit neon back face with dark ink lettering.
 * Text is un-mirrored on the back face so it remains perfectly legible and upright.
 * Outer housing, neon ring and glass lens remain stationary.
 */
@Composable
fun FlipButton(
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

    // 3D rotation around Y axis with spring overshoot bounce (cubic-bezier .3, 1.4, .5, 1)
    val flipAngleState = animateFloatAsState(
        targetValue = if (isPressed) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.60f, stiffness = 340f),
        label = "flip_angle"
    )
    val rgbBloomAlphaState = animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 0.45f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "flip_rgb_bloom"
    )

    val currentOnVibrate by rememberUpdatedState(onVibrate)
    val currentViewModel by rememberUpdatedState(viewModel)
    val activeLabel = displayLabel ?: key
    val psShape = getPlayStationShape(activeLabel)
    val effectiveColor = if (psShape != null) getPlayStationColor(psShape) else buttonColor

    val density = androidx.compose.ui.platform.LocalDensity.current.density
    val borderBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color.Black.copy(alpha = 0.50f),
                Color.Black.copy(alpha = 0.85f)
            )
        )
    }
    val frontFaceBrush = remember {
        Brush.radialGradient(
            colors = listOf(Color(0xFF232527), Color(0xFF0C0D0E), Color.Black),
            center = Offset(0.50f, 0.55f),
            radius = 240f
        )
    }
    val stroke1dp = remember(density) { Stroke(width = 1f * density) }
    val stroke2dp = remember(density) { Stroke(width = 2f * density) }
    val stroke4dp = remember(density) { Stroke(width = 4f * density) }
    val insetShadowColorsPressed = remember { listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)) }
    val insetShadowColorsNormal = remember { listOf(Color.Transparent, Color.Black.copy(alpha = 0.70f)) }
    val lensColorsPressed = remember {
        listOf(
            Color.White.copy(alpha = 0.06f),
            Color.White.copy(alpha = 0.02f),
            Color.Transparent
        )
    }
    val lensColorsNormal = remember {
        listOf(
            Color.White.copy(alpha = 0.14f),
            Color.White.copy(alpha = 0.05f),
            Color.Transparent
        )
    }
    val backFaceSheenColors = remember {
        listOf(Color.White.copy(alpha = 0.45f), Color.Transparent)
    }
    val specularSheenColors = remember {
        listOf(Color.White.copy(alpha = 0.06f), Color.Transparent)
    }

    val flipBloomStops = remember(effectiveColor) {
        arrayOf(
            0.00f to Color.Transparent,
            0.40f to Color.Transparent,
            1.00f to Color.Transparent
        )
    }

    val isBackFace by remember {
        androidx.compose.runtime.derivedStateOf { flipAngleState.value > 90f }
    }

    Box(
        modifier = modifier
            .size(80.dp)
            .graphicsLayer {
                // Root RenderNode isolation
            }
            .drawBehind {
                if (isRgbEnabled) {
                    val flipAngle = flipAngleState.value
                    val rgbBloomAlpha = rgbBloomAlphaState.value
                    val isBackFaceDraw = flipAngle > 90f
                    val cosFactor = kotlin.math.abs(kotlin.math.cos(Math.toRadians(flipAngle.toDouble()))).toFloat()
                    val squashedWidth = (size.width * 1.15f) * cosFactor.coerceAtLeast(0.08f)
                    val auraHeight = size.height * 1.10f
                    val auraTopLeft = Offset(center.x - squashedWidth / 2f, center.y - auraHeight / 2f)
                    val auraSize = Size(squashedWidth, auraHeight)

                    // 1. 3D squashed anamorphic aura oval tracking flipAngle
                    flipBloomStops[0] = 0.00f to (if (isBackFaceDraw) Color.White else effectiveColor).copy(alpha = rgbBloomAlpha * 0.60f)
                    flipBloomStops[1] = 0.40f to effectiveColor.copy(alpha = rgbBloomAlpha * 0.30f)
                    flipBloomStops[2] = 1.00f to Color.Transparent
                    drawOval(
                        brush = Brush.radialGradient(
                            colorStops = flipBloomStops,
                            center = center,
                            radius = (auraHeight / 2f).coerceAtLeast(1f)
                        ),
                        topLeft = auraTopLeft,
                        size = auraSize
                    )

                    // 2. Vertical laser slit blade when near 90° edge-on
                    if (cosFactor < 0.38f) {
                        val bladeIntensity = (1.0f - cosFactor / 0.38f) * rgbBloomAlpha
                        val bladeHalfH = size.height * 0.65f
                        drawLine(
                            color = Color.White.copy(alpha = bladeIntensity * 0.95f),
                            start = Offset(center.x, center.y - bladeHalfH),
                            end = Offset(center.x, center.y + bladeHalfH),
                            strokeWidth = 2.5f
                        )
                        drawLine(
                            color = effectiveColor.copy(alpha = bladeIntensity * 0.70f),
                            start = Offset(center.x, center.y - bladeHalfH * 1.15f),
                            end = Offset(center.x, center.y + bladeHalfH * 1.15f),
                            strokeWidth = 5.0f
                        )
                    }
                }
            }
            .shadow(
                elevation = if (isPressed) 2.dp else 6.dp,
                shape = CircleShape,
                spotColor = Color.Black,
                ambientColor = Color.Black
            )
            .clip(CircleShape)
            .background(Color(0xFF0C0D0E))
            .border(
                width = 1.dp,
                brush = borderBrush,
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
        // ── 1. FLIPPING CARD (.fl-in) ──
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val angle = flipAngleState.value
                    rotationY = if (angle > 90f) angle - 180f else angle
                    cameraDistance = 16f
                }
                .clip(CircleShape)
                .background(
                    if (isBackFace) SolidColor(effectiveColor)
                    else frontFaceBrush
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isBackFace) {
                // BACK FACE (.fl-f.b): specular white highlight at top + dark ink upright letter
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = backFaceSheenColors,
                            center = Offset(w * 0.50f, h * 0.35f),
                            radius = w * 0.45f
                        ),
                        radius = w * 0.45f,
                        center = Offset(w * 0.50f, h * 0.35f)
                    )
                }

                if (psShape != null) {
                    PlayStationSymbol(
                        shape = psShape,
                        color = Color(0xFF0A0B0C),
                        size = 38.dp
                    )
                } else {
                    Text(
                        text = activeLabel,
                        color = Color(0xFF0A0B0C),
                        fontWeight = FontWeight.Medium,
                        fontSize = 44.sp
                    )
                }
            } else {
                // FRONT FACE (.fl-f.a): neon glowing letter
                if (psShape != null) {
                    PlayStationSymbol(
                        shape = psShape,
                        color = effectiveColor,
                        size = 38.dp
                    )
                } else {
                    Text(
                        text = activeLabel,
                        color = effectiveColor,
                        fontWeight = FontWeight.Medium,
                        fontSize = 44.sp
                    )
                }
            }
        }

        // ── 2. STATIONARY SHELL OVERLAY (.lx-ring + .lx-lens + undercut shadow) ──
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val r = size.minDimension / 2f

            // Undercut bottom shadow
            val insetH = h * 0.32f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = if (isPressed) insetShadowColorsPressed else insetShadowColorsNormal,
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
                brush = Brush.verticalGradient(
                    colors = if (isPressed) lensColorsPressed else lensColorsNormal,
                    startY = 0f,
                    endY = h * 0.40f
                ),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(lensInset, lensInset),
                size = Size(w - lensInset * 2f, h - lensInset * 2f),
                style = stroke1dp
            )

            // Specular sheen
            drawCircle(
                brush = Brush.radialGradient(
                    colors = specularSheenColors,
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
 * Static non-interactive preview of FlipButton (idle state).
 */
@Composable
internal fun StaticFlipButton(
    controlKey: String,
    labelStyle: ControllerLabelStyle,
    modifier: Modifier = Modifier
) {
    val displayLabel = CategoryManager.getLabelForStyle(controlKey, labelStyle)
    val buttonColor = resolveButtonColor(controlKey, labelStyle, displayLabel)
    val psShape = getPlayStationShape(displayLabel)

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

        if (psShape != null) {
            PlayStationSymbol(
                shape = psShape,
                color = buttonColor,
                size = 38.dp
            )
        } else {
            Text(
                text = displayLabel,
                color = buttonColor,
                fontWeight = FontWeight.Medium,
                fontSize = 44.sp
            )
        }

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
