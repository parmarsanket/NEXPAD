package com.sanket.tools.nexpad.ui.components.controller

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.category.ControllerLabelStyle
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel
import kotlin.math.roundToInt

/**
 * Modern, tactile Gyroscope Sensor Toggle Button for mobile NEXPAD controllers.
 * Allows the user to toggle phone motion sensing ON or OFF with a single tap during gameplay,
 * instantly muting sensor updates on the device to prevent unwanted input during pauses or hand rest.
 *
 * Implements the 7-Layer Display List Pipeline with dynamic status aura, animated gimbal rings,
 * and high-contrast ON/OFF state indicators.
 */
@Composable
fun GyroToggleButton(
    isConnected: Boolean,
    onVibrate: () -> Unit,
    viewModel: GamepadViewModel,
    isRgbEnabled: Boolean,
    modifier: Modifier = Modifier,
    displayLabel: String? = null
) {
    var isPressed by remember { mutableStateOf(false) }
    val isGyroEnabled by viewModel.isGyroEnabled.collectAsState()

    val scaleAnim by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "gyro_scale"
    )
    val pressOffsetYAnim by animateFloatAsState(
        targetValue = if (isPressed) 2.5f else 0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "gyro_depth"
    )
    val rgbBloomAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else if (isGyroEnabled) 0.50f else 0.15f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "gyro_bloom"
    )

    val activeAccentColor by animateColorAsState(
        targetValue = if (isGyroEnabled) Color(0xFF00F0FF) else Color(0xFF64748B),
        animationSpec = tween(durationMillis = 200),
        label = "gyro_accent_color"
    )

    val statusDotColor by animateColorAsState(
        targetValue = if (isGyroEnabled) Color(0xFF3FD25A) else Color(0xFFEF4444),
        animationSpec = tween(durationMillis = 200),
        label = "gyro_dot_color"
    )

    val currentOnVibrate by rememberUpdatedState(onVibrate)
    val currentViewModel by rememberUpdatedState(viewModel)

    val domeBrush = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF23272D),
                Color(0xFF131518),
                Color(0xFF08090A)
            ),
            center = Offset(0.50f, 0.42f),
            radius = 180f
        )
    }

    Box(
        modifier = modifier
            .size(76.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer interactive container with aura & shadow
        Box(
            modifier = Modifier
                .size(66.dp)
                .offset { IntOffset(0, pressOffsetYAnim.roundToInt()) }
                .graphicsLayer {
                    scaleX = scaleAnim
                    scaleY = scaleAnim
                }
                .drawBehind {
                    if (isRgbEnabled && isGyroEnabled) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    activeAccentColor.copy(alpha = rgbBloomAlpha * 0.45f),
                                    activeAccentColor.copy(alpha = rgbBloomAlpha * 0.15f),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = size.minDimension * 0.95f
                            ),
                            radius = size.minDimension * 0.95f
                        )
                    }
                }
                .shadow(
                    elevation = if (isPressed) 1.dp else if (isGyroEnabled) 6.dp else 2.dp,
                    shape = CircleShape,
                    ambientColor = if (isRgbEnabled && isGyroEnabled) activeAccentColor else Color.Black,
                    spotColor = if (isRgbEnabled && isGyroEnabled) activeAccentColor else Color.Black
                )
                .clip(CircleShape)
                .background(domeBrush)
                .border(
                    width = 1.5.dp,
                    brush = Brush.verticalGradient(
                        colors = if (isGyroEnabled) {
                            listOf(
                                activeAccentColor.copy(alpha = 0.85f),
                                activeAccentColor.copy(alpha = 0.30f)
                            )
                        } else {
                            listOf(
                                Color.White.copy(alpha = 0.20f),
                                Color.Black.copy(alpha = 0.60f)
                            )
                        }
                    ),
                    shape = CircleShape
                )
                .pointerInput(isConnected) {
                    if (!isConnected) return@pointerInput
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            currentOnVibrate()
                            currentViewModel.toggleGyro()
                            tryAwaitRelease()
                            isPressed = false
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // Layer 4 & 5: Gimbal Rings, Reticle, and Acrylic Glare Arc
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val r = size.minDimension / 2f

                // Outer Gimbal Ring (Tick notched)
                val gimbalRadius = r - 5.dp.toPx()
                drawCircle(
                    color = activeAccentColor.copy(alpha = if (isGyroEnabled) 0.65f else 0.25f),
                    radius = gimbalRadius,
                    center = Offset(cx, cy),
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(6.dp.toPx(), 4.dp.toPx()), 0f
                        )
                    )
                )

                // Inner Crosshairs / Horizon Guidelines
                val crossHairLength = 8.dp.toPx()
                val alphaGuideline = if (isGyroEnabled) 0.50f else 0.20f
                drawLine(
                    color = activeAccentColor.copy(alpha = alphaGuideline),
                    start = Offset(cx - crossHairLength - 12.dp.toPx(), cy),
                    end = Offset(cx - 12.dp.toPx(), cy),
                    strokeWidth = 1.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = activeAccentColor.copy(alpha = alphaGuideline),
                    start = Offset(cx + 12.dp.toPx(), cy),
                    end = Offset(cx + crossHairLength + 12.dp.toPx(), cy),
                    strokeWidth = 1.2.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Specular Acrylic Lens Glare Arc (Top Edge)
                drawArc(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (isPressed) 0.08f else 0.22f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = size.height * 0.35f
                    ),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(2.dp.toPx(), 2.dp.toPx()),
                    size = Size(size.width - 4.dp.toPx(), size.height - 4.dp.toPx()),
                    style = Stroke(width = 1.2.dp.toPx())
                )
            }

            // Layer 6: Dynamic Glyph & Status Label
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = displayLabel ?: "GYRO",
                    color = if (isGyroEnabled) activeAccentColor else Color(0xFF94A3B8),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(statusDotColor)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (isGyroEnabled) "ON" else "OFF",
                        color = if (isGyroEnabled) Color(0xFFE2E8F0) else Color(0xFF64748B),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}

/**
 * High-performance, non-interactive static preview of GyroToggleButton for Button Studio grid.
 */
@Composable
fun StaticGyroToggleButton(
    isRgbEnabled: Boolean = true,
    labelStyle: ControllerLabelStyle = ControllerLabelStyle.XBOX,
    modifier: Modifier = Modifier
) {
    val activeAccentColor = Color(0xFF00F0FF)
    val statusDotColor = Color(0xFF3FD25A)

    val domeBrush = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF23272D),
                Color(0xFF131518),
                Color(0xFF08090A)
            ),
            center = Offset(0.50f, 0.42f),
            radius = 180f
        )
    }

    Box(
        modifier = modifier.size(76.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(66.dp)
                .drawBehind {
                    if (isRgbEnabled) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    activeAccentColor.copy(alpha = 0.25f),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = size.minDimension * 0.95f
                            ),
                            radius = size.minDimension * 0.95f
                        )
                    }
                }
                .shadow(4.dp, CircleShape, ambientColor = activeAccentColor, spotColor = activeAccentColor)
                .clip(CircleShape)
                .background(domeBrush)
                .border(
                    width = 1.5.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            activeAccentColor.copy(alpha = 0.85f),
                            activeAccentColor.copy(alpha = 0.30f)
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val r = size.minDimension / 2f
                val gimbalRadius = r - 5.dp.toPx()

                drawCircle(
                    color = activeAccentColor.copy(alpha = 0.65f),
                    radius = gimbalRadius,
                    center = Offset(cx, cy),
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(6.dp.toPx(), 4.dp.toPx()), 0f
                        )
                    )
                )

                val crossHairLength = 8.dp.toPx()
                drawLine(
                    color = activeAccentColor.copy(alpha = 0.50f),
                    start = Offset(cx - crossHairLength - 12.dp.toPx(), cy),
                    end = Offset(cx - 12.dp.toPx(), cy),
                    strokeWidth = 1.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = activeAccentColor.copy(alpha = 0.50f),
                    start = Offset(cx + 12.dp.toPx(), cy),
                    end = Offset(cx + crossHairLength + 12.dp.toPx(), cy),
                    strokeWidth = 1.2.dp.toPx(),
                    cap = StrokeCap.Round
                )

                drawArc(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.22f), Color.Transparent),
                        startY = 0f,
                        endY = size.height * 0.35f
                    ),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(2.dp.toPx(), 2.dp.toPx()),
                    size = Size(size.width - 4.dp.toPx(), size.height - 4.dp.toPx()),
                    style = Stroke(width = 1.2.dp.toPx())
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "GYRO",
                    color = activeAccentColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(statusDotColor)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "ON",
                        color = Color(0xFFE2E8F0),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}
