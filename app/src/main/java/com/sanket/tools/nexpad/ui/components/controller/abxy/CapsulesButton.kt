package com.sanket.tools.nexpad.ui.components.controller.abxy

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.category.CategoryManager
import com.sanket.tools.nexpad.category.ControlKey
import com.sanket.tools.nexpad.category.ControllerLabelStyle
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel
import kotlin.math.roundToInt

/**
 * Capsule Fill ABXY button variant.
 * Pill-shaped buttons: Y/A are vertical (52x84dp), X/B are horizontal (84x52dp).
 * On press a gradient floods from the outer end inward. Damped spring kinematics.
 */
@Composable
fun CapsulesButton(
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
    var isPressed by remember { mutableStateOf(false) }

    val scaleAnimState = animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "caps_scale"
    )
    val pressOffsetYAnimState = animateFloatAsState(
        targetValue = if (isPressed) 2f else 0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "caps_depth"
    )
    val floodAlphaState = animateFloatAsState(
        targetValue = if (isPressed) 0.5f else 0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "caps_flood"
    )
    val rgbBloomAlphaState = animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 0.45f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "caps_rgb_bloom"
    )
    val density = LocalDensity.current.density

    val currentOnVibrate by rememberUpdatedState(onVibrate)
    val currentViewModel by rememberUpdatedState(viewModel)
    val activeLabel = displayLabel ?: key
    val psShape = getPlayStationShape(activeLabel)
    val effectiveColor = if (psShape != null) getPlayStationColor(psShape) else buttonColor

    val pillShape = remember { RoundedCornerShape(26.dp) }
    val isVertical = key.uppercase() == ControlKey.Y.key || key.uppercase() == ControlKey.A.key
    val pillW = if (isVertical) 52.dp else 84.dp
    val pillH = if (isVertical) 84.dp else 52.dp

    val floodAlpha = floodAlphaState.value
    // Flood gradient direction based on key
    val floodBrush = when (key.uppercase()) {
        ControlKey.Y.key -> Brush.verticalGradient(
            colors = listOf(effectiveColor.copy(alpha = floodAlpha), Color.Transparent)
        )
        ControlKey.A.key -> Brush.verticalGradient(
            colors = listOf(Color.Transparent, effectiveColor.copy(alpha = floodAlpha))
        )
        ControlKey.X.key -> Brush.horizontalGradient(
            colors = listOf(effectiveColor.copy(alpha = floodAlpha), Color.Transparent)
        )
        else -> Brush.horizontalGradient(
            colors = listOf(Color.Transparent, effectiveColor.copy(alpha = floodAlpha))
        )
    }

    val domeBrush = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF232527),
                Color(0xFF0C0D0E),
                Color(0xFF000000)
            )
        )
    }

    val capsuleBloomStops = remember(effectiveColor) {
        arrayOf(
            0.00f to Color.Transparent,
            0.45f to Color.Transparent,
            1.00f to Color.Transparent
        )
    }
    val nodeBloomColors = remember(effectiveColor) {
        arrayListOf(Color.Transparent, Color.Transparent, Color.Transparent)
    }

    Box(
        modifier = modifier
            .size(80.dp)
            .graphicsLayer {
                // Root RenderNode isolation
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(pillW)
                .height(pillH)
                .drawBehind {
                    if (isRgbEnabled) {
                        val rgbBloomAlpha = rgbBloomAlphaState.value
                        val pad = 12.dp.toPx()
                        val capR = 26.dp.toPx()

                        // 1. Elongated stadium capsule aura
                        capsuleBloomStops[0] = 0.00f to effectiveColor.copy(alpha = rgbBloomAlpha * 0.55f)
                        capsuleBloomStops[1] = 0.45f to effectiveColor.copy(alpha = rgbBloomAlpha * 0.22f)
                        capsuleBloomStops[2] = 1.00f to Color.Transparent
                        drawRoundRect(
                            brush = Brush.radialGradient(
                                colorStops = capsuleBloomStops,
                                center = center,
                                radius = size.maxDimension * 0.70f
                            ),
                            topLeft = Offset(-pad, -pad),
                            size = Size(size.width + pad * 2, size.height + pad * 2),
                            cornerRadius = CornerRadius(capR + pad, capR + pad)
                        )

                        // 2. Dual focal node blooms at stadium endcaps
                        val node1 = if (isVertical) Offset(center.x, capR) else Offset(capR, center.y)
                        val node2 = if (isVertical) Offset(center.x, size.height - capR) else Offset(size.width - capR, center.y)
                        val nodeRadius = capR * 1.2f

                        nodeBloomColors[0] = (if (isPressed) Color.White else effectiveColor).copy(alpha = rgbBloomAlpha * 0.70f)
                        nodeBloomColors[1] = effectiveColor.copy(alpha = rgbBloomAlpha * 0.25f)
                        nodeBloomColors[2] = Color.Transparent
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = nodeBloomColors,
                                center = node1,
                                radius = nodeRadius
                            ),
                            radius = nodeRadius,
                            center = node1
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = nodeBloomColors,
                                center = node2,
                                radius = nodeRadius
                            ),
                            radius = nodeRadius,
                            center = node2
                        )
                    }
                }
                .shadow(
                    elevation = if (isPressed) 1.dp else 4.dp,
                    shape = pillShape,
                    spotColor = if (isRgbEnabled) effectiveColor else Color.Black,
                    ambientColor = if (isRgbEnabled) effectiveColor else Color.Black
                )
                .graphicsLayer {
                    scaleX = scaleAnimState.value
                    scaleY = scaleAnimState.value
                    translationY = pressOffsetYAnimState.value * density
                }
                .clip(pillShape)
                .background(domeBrush)
                .background(floodBrush)
                .border(
                    width = 2.dp,
                    color = effectiveColor.copy(alpha = if (isPressed) 0.9f else 0.6f),
                    shape = pillShape
                )
                .then(
                    if (isInteractive) {
                        Modifier.pointerInput(key) {
                            detectTapGestures(
                                onPress = {
                                    currentOnVibrate()
                                    isPressed = true
                                    currentViewModel?.updateButton(key, true)
                                    tryAwaitRelease()
                                    isPressed = false
                                    currentViewModel?.updateButton(key, false)
                                }
                            )
                        }
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (psShape != null) {
                PlayStationSymbol(
                    shape = psShape,
                    color = if (isPressed) Color.White else effectiveColor,
                    size = 24.dp
                )
            } else {
                Text(
                    text = activeLabel,
                    color = if (isPressed) Color.White else effectiveColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
            }
        }
    }
}

/**
 * Static non-interactive preview delegate for CapsulesButton with zero touch overhead.
 */
@Composable
fun StaticCapsulesButton(
    controlKey: String,
    labelStyle: ControllerLabelStyle = ControllerLabelStyle.XBOX,
    modifier: Modifier = Modifier
) {
    val displayLabel = CategoryManager.getLabelForStyle(controlKey, labelStyle)
    val buttonColor = resolveButtonColor(controlKey, labelStyle, displayLabel)
    CapsulesButton(
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
