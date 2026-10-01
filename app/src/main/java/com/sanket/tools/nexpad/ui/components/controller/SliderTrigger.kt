package com.sanket.tools.nexpad.ui.components.controller

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.model.NexpadKeys
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Native Jetpack Compose implementation of the NEXPAD Analog Slider Trigger (Trigger F — Slider).
 *
 * Designed for continuous analog input (full 0..255 resolution over NEXPAD binary protocol),
 * physical trigger clips that contact and drag the screen, and custom ergonomics:
 * - Vertical track with glowing active fill bar
 * - Sliding 46dp glass puck handle with dual-layer neon ring and central glowing LED dot
 * - Continuous analog updates: directly invokes [GamepadViewModel.updateTrigger] with exact [0f, 1f]
 * - Damped harmonic spring return: smoothly springs back to 0.0 on release, broadcasting real-time decay
 * - Freedom of sizing: custom height scaling via [heightScale] (controllable from HUD Inspector)
 * - Inversion switch: toggleable pull direction via [isFlipped] (default Top-to-Bottom, or Bottom-to-Top)
 * - Recessed optical window well with bold tactical typography
 * - Multi-pass emissive neon chassis bloom and glass specular crescent highlight
 */
@Composable
fun SliderTrigger(
    key: String,
    isConnected: Boolean,
    onVibrate: () -> Unit,
    viewModel: GamepadViewModel,
    isRgbEnabled: Boolean,
    modifier: Modifier = Modifier,
    displayLabel: String? = null,
    heightScale: Float = 1.0f,
    isFlipped: Boolean = false
) {
    val isLeft = key.uppercase() == NexpadKeys.LT
    // HTML source palette: Amber Gold (#FFB13F) for LT, Hot Coral (#FF5A7A) for RT
    val glowColor = if (isLeft) Color(0xFFFFB13F) else Color(0xFFFF5A7A)

    val currentOnVibrate by rememberUpdatedState(onVibrate)
    val currentViewModel by rememberUpdatedState(viewModel)
    val coroutineScope = rememberCoroutineScope()

    // Base dimensions scaled 45% smaller for compact phone ergonomics (42dp x 94dp)
    val widthDp = 42.dp
    val effectiveHeightScale = heightScale.coerceIn(0.5f, 2.5f)
    val baseHeightDp = 94.dp
    val totalHeightDp = (baseHeightDp.value * effectiveHeightScale).dp

    val chassisShape = remember { RoundedCornerShape(21.dp) }
    val windowShape = remember { RoundedCornerShape(7.dp) }

    // Physical travel metrics in Dp (proportional 0.55x)
    val puckDiameterDp = 26.dp
    val puckRadiusDp = puckDiameterDp / 2f
    val windowHeightDp = 14.dp
    val windowWidthDp = 30.dp
    val edgeMarginDp = 7.dp
    val windowClearanceDp = 6.dp

    // Available travel path for the puck center
    val topRestCenterDp = 9.dp + puckRadiusDp
    val bottomRestCenterDp = totalHeightDp - edgeMarginDp - windowHeightDp - windowClearanceDp - puckRadiusDp
    val travelSpanDp = (bottomRestCenterDp - topRestCenterDp).coerceAtLeast(8.dp)

    // Current pull value [0f, 1f]
    var isDragging by remember { mutableStateOf(false) }
    val fillAnim = remember { Animatable(0f) }

    val baseDomeGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF232527),
                Color(0xFF0C0D0E),
                Color(0xFF000000)
            ),
            center = Offset(0.50f, 0.55f),
            radius = 200f
        )
    }

    val density = LocalDensity.current

    Box(
        modifier = modifier
            .size(widthDp, totalHeightDp)
            // Physical black drop shadow
            .shadow(
                elevation = 6.dp,
                shape = chassisShape,
                ambientColor = Color.Black.copy(alpha = 0.40f),
                spotColor = Color.Black.copy(alpha = 0.55f)
            )
            .clip(chassisShape)
            .background(baseDomeGradient)
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
            .pointerInput(key, isFlipped, travelSpanDp, topRestCenterDp, bottomRestCenterDp) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    isDragging = true
                    currentOnVibrate()

                    val topPx = with(density) { topRestCenterDp.toPx() }
                    val spanPx = with(density) { travelSpanDp.toPx() }

                    fun calculateFill(yPos: Float): Float {
                        val normalized = ((yPos - topPx) / spanPx).coerceIn(0f, 1f)
                        return if (isFlipped) (1.0f - normalized) else normalized
                    }

                    val initialFill = calculateFill(down.position.y)
                    coroutineScope.launch {
                        fillAnim.snapTo(initialFill)
                        currentViewModel.updateTrigger(key, initialFill)
                    }

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id }
                        if (change == null || !change.pressed) break

                        change.consume()
                        val newFill = calculateFill(change.position.y)
                        coroutineScope.launch {
                            fillAnim.snapTo(newFill)
                            currentViewModel.updateTrigger(key, newFill)
                        }
                    }

                    // On release: Damped harmonic spring back to idle (0f)
                    isDragging = false
                    coroutineScope.launch {
                        fillAnim.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        ) {
                            currentViewModel.updateTrigger(key, value)
                        }
                        currentViewModel.updateTrigger(key, 0f)
                    }
                }
            }
    ) {
        val currentFill = fillAnim.value

        // Layer 1: Track and Glowing Active Fill Bar
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val centerX = w / 2f
            val topPx = topRestCenterDp.toPx()
            val bottomPx = bottomRestCenterDp.toPx()
            val spanPx = travelSpanDp.toPx()

            val trackWidthPx = 5.dp.toPx()
            val trackRadiusPx = 2.5.dp.toPx()

            // Outer recessed track groove
            drawRoundRect(
                color = Color.White.copy(alpha = 0.08f),
                topLeft = Offset(centerX - trackWidthPx / 2f, topPx - trackRadiusPx),
                size = Size(trackWidthPx, spanPx + trackRadiusPx * 2f),
                cornerRadius = CornerRadius(trackRadiusPx, trackRadiusPx)
            )

            // Inner dark track depth
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.70f),
                topLeft = Offset(centerX - trackWidthPx / 2f + 1f, topPx - trackRadiusPx + 1f),
                size = Size(trackWidthPx - 2f, spanPx + trackRadiusPx * 2f - 2f),
                cornerRadius = CornerRadius(trackRadiusPx - 1f, trackRadiusPx - 1f)
            )

            // Current puck center Y
            val puckCenterY = if (isFlipped) {
                bottomPx - currentFill * spanPx
            } else {
                topPx + currentFill * spanPx
            }

            // Glowing Active Fill Bar
            if (currentFill > 0.005f) {
                val fillTopY = if (isFlipped) puckCenterY else topPx
                val fillHeight = if (isFlipped) (bottomPx - puckCenterY) else (puckCenterY - topPx)

                // Emissive halo bloom
                drawRoundRect(
                    color = glowColor.copy(alpha = 0.45f),
                    topLeft = Offset(centerX - trackWidthPx / 2f - 2.dp.toPx(), fillTopY),
                    size = Size(trackWidthPx + 4.dp.toPx(), fillHeight),
                    cornerRadius = CornerRadius(trackRadiusPx, trackRadiusPx)
                )

                // Crisp neon core
                drawRoundRect(
                    color = glowColor,
                    topLeft = Offset(centerX - trackWidthPx / 2f, fillTopY),
                    size = Size(trackWidthPx, fillHeight),
                    cornerRadius = CornerRadius(trackRadiusPx, trackRadiusPx)
                )
            }
        }

        // Layer 2: Sliding Puck Handle
        val puckCenterDp = if (isFlipped) {
            bottomRestCenterDp - travelSpanDp * currentFill
        } else {
            topRestCenterDp + travelSpanDp * currentFill
        }

        Box(
            modifier = Modifier
                .size(puckDiameterDp)
                .offset {
                    IntOffset(
                        x = ((widthDp - puckDiameterDp) / 2f).roundToPx(),
                        y = (puckCenterDp - puckRadiusDp).roundToPx()
                    )
                }
                .shadow(
                    elevation = if (isDragging) 6.dp else 3.dp,
                    shape = CircleShape,
                    ambientColor = Color.Black.copy(alpha = 0.40f),
                    spotColor = Color.Black.copy(alpha = 0.55f)
                )
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF34373B),
                            Color(0xFF17181B),
                            Color(0xFF050506)
                        ),
                        center = Offset(0.50f, 0.38f),
                        radius = 50f
                    )
                )
                .border(1.dp, Color.Black.copy(alpha = 0.6f), CircleShape)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val r = size.minDimension / 2f
                val center = Offset(size.width / 2f, size.height / 2f)

                // Specular lens highlight at top
                drawArc(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.22f),
                            Color.White.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    ),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(1.5.dp.toPx(), 1.5.dp.toPx()),
                    size = Size(size.width - 3.dp.toPx(), size.height - 3.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx())
                )

                // Puck Neon Ring (.puck-ring)
                val ringRadius = r - 2.5.dp.toPx()
                // Outer bloom
                drawCircle(
                    color = glowColor.copy(alpha = 0.40f + currentFill * 0.45f),
                    radius = ringRadius,
                    center = center,
                    style = Stroke(width = 2.2.dp.toPx())
                )
                // Crisp core
                drawCircle(
                    color = glowColor.copy(alpha = 0.85f),
                    radius = ringRadius,
                    center = center,
                    style = Stroke(width = 1.2.dp.toPx())
                )

                // Central glowing LED dot (.puck-dot)
                val dotRadius = 2.5.dp.toPx()
                // Bloom
                drawCircle(
                    color = glowColor.copy(alpha = 0.50f + currentFill * 0.50f),
                    radius = dotRadius + 2.dp.toPx(),
                    center = center
                )
                // Core
                drawCircle(
                    color = Color.White.copy(alpha = 0.90f),
                    radius = dotRadius * 0.65f,
                    center = center
                )
                drawCircle(
                    color = glowColor,
                    radius = dotRadius,
                    center = center,
                    style = Stroke(width = 0.8.dp.toPx())
                )
            }
        }

        // Layer 3: Recessed Optical Window (.lx-window)
        val windowAlignment = if (isFlipped) Alignment.TopCenter else Alignment.BottomCenter
        val windowOffset = if (isFlipped) {
            Modifier.offset(y = edgeMarginDp)
        } else {
            Modifier.offset(y = -edgeMarginDp)
        }

        Box(
            modifier = Modifier
                .align(windowAlignment)
                .then(windowOffset)
                .size(windowWidthDp, windowHeightDp)
                .clip(windowShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF050506),
                            Color(0xFF121314)
                        ),
                        center = Offset(0.50f, 0.60f),
                        radius = 50f
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
            // Optical window edge vignette
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.80f)
                        ),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = size.width * 0.55f
                    ),
                    size = size,
                    cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx())
                )
            }

            val label = displayLabel ?: key
            Text(
                text = label,
                color = glowColor.copy(alpha = 0.75f + currentFill * 0.25f),
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }

        // Layer 4: Specular Glass Lens & Outer Rim (.lx-lens & .lx-ring)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val rPx = 21.dp.toPx()

            // Top specular crescent highlight
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.16f),
                        Color.White.copy(alpha = 0.04f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = h * 0.28f
                ),
                topLeft = Offset(1.5.dp.toPx(), 1.5.dp.toPx()),
                size = Size(w - 3.dp.toPx(), h * 0.28f),
                cornerRadius = CornerRadius(rPx, rPx)
            )

            // Outer chassis neon ring (.lx-ring)
            val ringInset = 2.dp.toPx()
            val ringCorner = rPx - ringInset
            // Bloom
            drawRoundRect(
                color = glowColor.copy(alpha = if (isDragging) 0.50f else 0.20f),
                topLeft = Offset(ringInset - 1f, ringInset - 1f),
                size = Size(w - (ringInset - 1f) * 2f, h - (ringInset - 1f) * 2f),
                cornerRadius = CornerRadius(ringCorner, ringCorner),
                style = Stroke(width = 2.5.dp.toPx())
            )
            // Core
            drawRoundRect(
                color = glowColor.copy(alpha = if (isDragging) 0.90f else 0.55f),
                topLeft = Offset(ringInset, ringInset),
                size = Size(w - ringInset * 2f, h - ringInset * 2f),
                cornerRadius = CornerRadius(ringCorner, ringCorner),
                style = Stroke(width = 1.2.dp.toPx())
            )
        }
    }
}
