package com.sanket.tools.nexpad.ui.components.controller

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    isConnected: Boolean,
    onVibrate: () -> Unit,
    viewModel: GamepadViewModel,
    isRgbEnabled: Boolean,
    modifier: Modifier = Modifier,
    displayLabel: String? = null
) {
    var isPressed by remember { mutableStateOf(false) }

    val scaleAnim by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "caps_scale"
    )
    val pressOffsetYAnim by animateFloatAsState(
        targetValue = if (isPressed) 2f else 0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "caps_depth"
    )
    val floodAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.5f else 0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "caps_flood"
    )
    val rgbBloomAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 0.45f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "caps_rgb_bloom"
    )

    val currentOnVibrate by rememberUpdatedState(onVibrate)
    val currentViewModel by rememberUpdatedState(viewModel)

    val pillShape = RoundedCornerShape(26.dp)
    val isVertical = key.uppercase() == ControlKey.Y.key || key.uppercase() == ControlKey.A.key
    val pillW = if (isVertical) 52.dp else 84.dp
    val pillH = if (isVertical) 84.dp else 52.dp

    // Flood gradient direction based on key
    val floodBrush = when (key.uppercase()) {
        ControlKey.Y.key -> Brush.verticalGradient(
            colors = listOf(buttonColor.copy(alpha = floodAlpha), Color.Transparent)
        )
        ControlKey.A.key -> Brush.verticalGradient(
            colors = listOf(Color.Transparent, buttonColor.copy(alpha = floodAlpha))
        )
        ControlKey.X.key -> Brush.horizontalGradient(
            colors = listOf(buttonColor.copy(alpha = floodAlpha), Color.Transparent)
        )
        else -> Brush.horizontalGradient(
            colors = listOf(Color.Transparent, buttonColor.copy(alpha = floodAlpha))
        )
    }

    val domeBrush = Brush.radialGradient(
        colors = listOf(
            Color(0xFF232527),
            Color(0xFF0C0D0E),
            Color(0xFF000000)
        )
    )

    Box(
        modifier = modifier
            .size(80.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(pillW)
                .height(pillH)
                .drawBehind {
                    if (isRgbEnabled) {
                        val pad = 10.dp.toPx()
                        drawRoundRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    buttonColor.copy(alpha = rgbBloomAlpha * 0.50f),
                                    buttonColor.copy(alpha = rgbBloomAlpha * 0.20f),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = size.minDimension * 0.95f
                            ),
                            topLeft = Offset(-pad, -pad),
                            size = Size(size.width + pad * 2, size.height + pad * 2),
                            cornerRadius = CornerRadius(26.dp.toPx() + pad, 26.dp.toPx() + pad)
                        )
                    }
                }
                .graphicsLayer {
                    scaleX = scaleAnim
                    scaleY = scaleAnim
                }
                .offset { IntOffset(0, pressOffsetYAnim.dp.roundToPx()) }
                .shadow(
                    elevation = if (isPressed) 1.dp else 4.dp,
                    shape = pillShape,
                    spotColor = if (isRgbEnabled) buttonColor else Color.Black,
                    ambientColor = if (isRgbEnabled) buttonColor else Color.Black
                )
                .clip(pillShape)
                .background(domeBrush)
                .background(floodBrush)
                .border(
                    width = 2.dp,
                    color = buttonColor.copy(alpha = if (isPressed) 0.9f else 0.6f),
                    shape = pillShape
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
            Text(
                text = displayLabel ?: key,
                color = if (isPressed) Color.White else buttonColor,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            )
        }
    }
}

@Composable
internal fun StaticCapsulesButton(
    controlKey: String,
    labelStyle: ControllerLabelStyle,
    modifier: Modifier = Modifier
) {
    val buttonColor = when (controlKey.uppercase()) {
        ControlKey.A.key -> Color(0xFF3FD25A)
        ControlKey.B.key -> Color(0xFFE6474E)
        ControlKey.X.key -> Color(0xFF3F8FE0)
        else             -> Color(0xFFE0A03F)
    }

    val pillShape = RoundedCornerShape(26.dp)
    val isVertical = controlKey.uppercase() == ControlKey.Y.key || controlKey.uppercase() == ControlKey.A.key
    val pillW = if (isVertical) 52.dp else 84.dp
    val pillH = if (isVertical) 84.dp else 52.dp

    val domeBrush = Brush.radialGradient(
        colors = listOf(
            Color(0xFF232527),
            Color(0xFF0C0D0E),
            Color(0xFF000000)
        )
    )

    Box(
        modifier = modifier.size(80.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(pillW)
                .height(pillH)
                .drawBehind {
                    val pad = 10.dp.toPx()
                    drawRoundRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                buttonColor.copy(alpha = 0.45f * 0.50f),
                                buttonColor.copy(alpha = 0.45f * 0.20f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = size.minDimension * 0.95f
                        ),
                        topLeft = Offset(-pad, -pad),
                        size = Size(size.width + pad * 2, size.height + pad * 2),
                        cornerRadius = CornerRadius(26.dp.toPx() + pad, 26.dp.toPx() + pad)
                    )
                }
                .shadow(elevation = 4.dp, shape = pillShape, spotColor = buttonColor, ambientColor = buttonColor)
                .clip(pillShape)
                .background(domeBrush)
                .border(
                    width = 2.dp,
                    color = buttonColor.copy(alpha = 0.6f),
                    shape = pillShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = controlKey,
                color = buttonColor,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            )
        }
    }
}
