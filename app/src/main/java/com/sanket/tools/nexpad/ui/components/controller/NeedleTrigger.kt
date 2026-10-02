package com.sanket.tools.nexpad.ui.components.controller

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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
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
 * Native Jetpack Compose implementation of the NEXPAD Needle Meter Trigger (Trigger G — Needle).
 *
 * Operates with standard tactile trigger ergonomics matching console triggers:
 * - Immediate responsive press feedback via [detectTapGestures]
 * - Direct digital wire dispatch via [GamepadViewModel.updateButton]
 * - Damped harmonic spring kinematics: dynamic scale compression, vertical displacement plunge,
 *   and smooth progress arc & needle sweep from -70° to +70° on press
 * - Arched dome trigger contour (108dp x 94dp, top corners 54dp forming a smooth curved dome)
 * - Calibrated analog meter: radial graduation ticks and base track arc (200° to 340°, 140° span)
 * - Recessed optical window at bottom displaying tactical trigger label (LT / RT)
 */
@Composable
fun NeedleTrigger(
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
    // HTML source palette: Mint Green (#5CF29A) for LT, Coral Pink (#FF5C8A) for RT
    val glowColor = if (isLeft) Color(0xFF5CF29A) else Color(0xFFFF5C8A)

    val currentOnVibrate by rememberUpdatedState(onVibrate)
    val currentViewModel by rememberUpdatedState(viewModel)

    // Mobile ergonomic proportions: arched dome sitting flush below shoulder bumpers
    val widthDp = 108.dp
    val heightDp = 94.dp
    val chassisShape = remember {
        RoundedCornerShape(topStart = 54.dp, topEnd = 54.dp, bottomStart = 14.dp, bottomEnd = 14.dp)
    }
    val windowShape = remember { RoundedCornerShape(10.dp) }

    // Physical Spring Kinematics — standard console trigger spec
    val scaleAnim by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "needle_scale"
    )
    val pressOffsetYAnim by animateFloatAsState(
        targetValue = if (isPressed) 2.0f else 0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "needle_offset"
    )
    val rgbBloomAlpha by animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0.45f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "needle_bloom"
    )
    val fillProgress by animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 380f),
        label = "needle_fill"
    )

    val baseDomeGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF232527),
                Color(0xFF0C0D0E),
                Color(0xFF000000)
            ),
            center = Offset(0.50f, 0.45f),
            radius = 220f
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
            radius = 220f
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
            // Outer dynamic RGB aura
            .drawBehind {
                if (isRgbEnabled) {
                    val padX = 12.dp.toPx()
                    val padY = 8.dp.toPx()
                    val curPull = fillProgress

                    // 1. Arched Dome Chassis Aura (54dp top curve, 14dp bottom)
                    drawRoundRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                glowColor.copy(alpha = rgbBloomAlpha * (if (isPressed) 0.50f else 0.30f)),
                                glowColor.copy(alpha = rgbBloomAlpha * 0.12f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = size.width * 0.55f
                        ),
                        topLeft = Offset(-padX, -padY),
                        size = Size(size.width + padX * 2f, size.height + padY * 2f),
                        cornerRadius = CornerRadius(54.dp.toPx(), 14.dp.toPx())
                    )

                    // 2. Sweeping Radial Tachometer Sector Halo
                    val pivotX = size.width / 2f
                    val pivotY = size.height - 28.dp.toPx()
                    val arcR = 48.dp.toPx()
                    val startAngle = 200f
                    val sweepTotal = 140f

                    // Base tachometer track glow
                    drawArc(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                glowColor.copy(alpha = rgbBloomAlpha * 0.25f),
                                Color.Transparent
                            ),
                            center = Offset(pivotX, pivotY),
                            radius = arcR + 10.dp.toPx()
                        ),
                        startAngle = startAngle,
                        sweepAngle = sweepTotal,
                        useCenter = true,
                        topLeft = Offset(pivotX - arcR, pivotY - arcR),
                        size = Size(arcR * 2f, arcR * 2f)
                    )

                    // Active sweeping sector plume
                    if (curPull > 0.02f) {
                        val activeSweep = sweepTotal * curPull
                        drawArc(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = rgbBloomAlpha * curPull * 0.70f),
                                    glowColor.copy(alpha = rgbBloomAlpha * curPull * 0.85f),
                                    Color.Transparent
                                ),
                                center = Offset(pivotX, pivotY),
                                radius = arcR + 12.dp.toPx()
                            ),
                            startAngle = startAngle,
                            sweepAngle = activeSweep,
                            useCenter = true,
                            topLeft = Offset(pivotX - arcR - 6.dp.toPx(), pivotY - arcR - 6.dp.toPx()),
                            size = Size((arcR + 6.dp.toPx()) * 2f, (arcR + 6.dp.toPx()) * 2f)
                        )

                        // 3. Tachometer Needle Tip Beacon Flare
                        val currentAngleDeg = startAngle + activeSweep
                        val angleRad = Math.toRadians(currentAngleDeg.toDouble())
                        val cosN = cos(angleRad).toFloat()
                        val sinN = sin(angleRad).toFloat()
                        val tipX = pivotX + (arcR + 4.dp.toPx()) * cosN
                        val tipY = pivotY + (arcR + 4.dp.toPx()) * sinN

                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = rgbBloomAlpha * 0.90f),
                                    glowColor.copy(alpha = rgbBloomAlpha * 0.80f),
                                    Color.Transparent
                                ),
                                center = Offset(tipX, tipY),
                                radius = (if (isPressed) 16.dp else 10.dp).toPx()
                            ),
                            center = Offset(tipX, tipY),
                            radius = (if (isPressed) 16.dp else 10.dp).toPx()
                        )
                    }
                }
            }
            // RGB-coordinated shadow
            .shadow(
                elevation = if (isPressed) 2.dp else 6.dp,
                shape = chassisShape,
                ambientColor = if (isRgbEnabled) glowColor else Color.Black,
                spotColor = if (isRgbEnabled) glowColor else Color.Black
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

        // Layer 1: Needle Gauge Face (Ticks, Background Track, Progress Arc, Pivot & Needle)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Pivot coordinates at lower-middle section above the optical window
            val pivotX = w / 2f
            val pivotY = h - 28.dp.toPx()

            val arcRadius = 40.dp.toPx()
            val ticksRadius = 46.dp.toPx()

            // Meter arc geometry: 140° symmetrical arc from 200° (bottom-left) to 340° (bottom-right)
            val startAngle = 200f
            val sweepTotal = 140f

            // Graduation Ticks Arc (dashed stroke effect matching HTML .ndl-ticks)
            drawArc(
                color = Color.White.copy(alpha = 0.24f),
                startAngle = startAngle,
                sweepAngle = sweepTotal,
                useCenter = false,
                topLeft = Offset(pivotX - ticksRadius, pivotY - ticksRadius),
                size = Size(ticksRadius * 2f, ticksRadius * 2f),
                style = Stroke(
                    width = 4.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(1.5f, 6.5f), 0f)
                )
            )

            // Inactive Background Track Arc (.ndl-track)
            drawArc(
                color = Color.White.copy(alpha = 0.10f),
                startAngle = startAngle,
                sweepAngle = sweepTotal,
                useCenter = false,
                topLeft = Offset(pivotX - arcRadius, pivotY - arcRadius),
                size = Size(arcRadius * 2f, arcRadius * 2f),
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )

            // Active Glowing Progress Arc (.ndl-prog)
            if (currentFill > 0.005f) {
                val activeSweep = sweepTotal * currentFill

                // Outer neon bloom halo
                drawArc(
                    color = glowColor.copy(alpha = 0.40f * (0.6f + currentFill * 0.4f)),
                    startAngle = startAngle,
                    sweepAngle = activeSweep,
                    useCenter = false,
                    topLeft = Offset(pivotX - arcRadius, pivotY - arcRadius),
                    size = Size(arcRadius * 2f, arcRadius * 2f),
                    style = Stroke(width = 5.5.dp.toPx(), cap = StrokeCap.Round)
                )

                // Crisp neon core
                drawArc(
                    color = glowColor,
                    startAngle = startAngle,
                    sweepAngle = activeSweep,
                    useCenter = false,
                    topLeft = Offset(pivotX - arcRadius, pivotY - arcRadius),
                    size = Size(arcRadius * 2f, arcRadius * 2f),
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Analog Needle: rotates from -70° to +70° (relative to pointing straight up)
            val needleAngleDeg = -70f + currentFill * 140f
            val needleLength = arcRadius + 4.dp.toPx()
            val needleWidth = 3.dp.toPx()

            rotate(degrees = needleAngleDeg, pivot = Offset(pivotX, pivotY)) {
                // Emissive needle glow
                drawLine(
                    color = glowColor.copy(alpha = 0.50f + currentFill * 0.45f),
                    start = Offset(pivotX, pivotY),
                    end = Offset(pivotX, pivotY - needleLength),
                    strokeWidth = needleWidth + 3.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Core needle blade with gradient (white tip, neon root)
                drawLine(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White, glowColor),
                        startY = pivotY - needleLength,
                        endY = pivotY
                    ),
                    start = Offset(pivotX, pivotY),
                    end = Offset(pivotX, pivotY - needleLength),
                    strokeWidth = needleWidth,
                    cap = StrokeCap.Round
                )
            }

            // Pivot Center Hub (.ndl-cap)
            val hubRadius = 7.dp.toPx()
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF34373B), Color(0xFF0C0D0E)),
                    center = Offset(pivotX, pivotY),
                    radius = hubRadius
                ),
                radius = hubRadius,
                center = Offset(pivotX, pivotY)
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.7f),
                radius = hubRadius,
                center = Offset(pivotX, pivotY),
                style = Stroke(width = 1.dp.toPx())
            )
            // Center LED dot on the pivot cap
            drawCircle(
                color = glowColor,
                radius = 2.2.dp.toPx(),
                center = Offset(pivotX, pivotY)
            )
        }

        // Layer 2: Recessed Optical Window (.lx-window)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-6).dp)
                .size(48.dp, 20.dp)
                .clip(windowShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF050506),
                            Color(0xFF121314)
                        ),
                        center = Offset(0.50f, 0.60f),
                        radius = 60f
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.9f),
                            Color.White.copy(alpha = 0.08f)
                        )
                    ),
                    shape = windowShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.80f)),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = size.width * 0.55f
                    ),
                    size = size,
                    cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                )
            }

            val label = displayLabel ?: key
            Text(
                text = label,
                color = glowColor.copy(alpha = 0.75f + currentFill * 0.25f),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }

        // Layer 3: Specular Glass Lens & Outer Chassis Ring (.lx-lens & .lx-ring)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Top specular crescent highlight on arched dome
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.16f),
                        Color.White.copy(alpha = 0.04f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = h * 0.35f
                ),
                topLeft = Offset(1.5.dp.toPx(), 1.5.dp.toPx()),
                size = Size(w - 3.dp.toPx(), h * 0.35f),
                cornerRadius = CornerRadius(54.dp.toPx(), 54.dp.toPx())
            )

            // Outer chassis neon ring (.lx-ring)
            val ringInset = 2.dp.toPx()
            // Outer bloom
            drawRoundRect(
                color = glowColor.copy(alpha = rgbBloomAlpha * 0.55f),
                topLeft = Offset(ringInset - 1f, ringInset - 1f),
                size = Size(w - (ringInset - 1f) * 2f, h - (ringInset - 1f) * 2f),
                cornerRadius = CornerRadius(52.dp.toPx(), 52.dp.toPx()),
                style = Stroke(width = 2.8.dp.toPx())
            )
            // Crisp core
            drawRoundRect(
                color = glowColor.copy(alpha = if (isPressed) 0.95f else 0.60f),
                topLeft = Offset(ringInset, ringInset),
                size = Size(w - ringInset * 2f, h - ringInset * 2f),
                cornerRadius = CornerRadius(52.dp.toPx(), 52.dp.toPx()),
                style = Stroke(width = 1.3.dp.toPx())
            )
        }
    }
}
