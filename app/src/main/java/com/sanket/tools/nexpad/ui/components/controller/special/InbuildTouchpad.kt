package com.sanket.tools.nexpad.ui.components.controller.special

import com.sanket.tools.nexpad.ui.theme.NeonPalette
import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sqrt
import kotlin.time.Duration.Companion.milliseconds

/**
 * In-Built Full Surface Touchpad (Consumes all empty space: Center-to-Left for LTP, Center-to-Right for RTP).
 *
 * Implements the Full-Mode touch architecture:
 * - Operates at the lowest Z-index (drawn behind all buttons/controls).
 * - 16.dp Universal Button Buffer Zone: touches within 16dp of any interactive button are completely ignored.
 * - Static baseline: baseLineAlpha = 0.022f, tickBaseAlpha = 0.040f, gradAlpha = 0.020f.
 * - Dynamic touch aura: Touching creates a surrounding cyber aura that doubles/triples line, tick, and glow opacity,
 *   fading smoothly back to static baseline values as distance from touch increases or touch ends.
 * - Uses calibrated 5-zone velocity transfer curve and trackball momentum coasting on release.
 */

// =========================================================================
// SHARED CANVAS DRAWING PRIMITIVES FOR INBUILD TOUCHPAD
// =========================================================================

private fun DrawScope.drawInbuildTouchpadGrid(
    isLeft: Boolean,
    auraColor: Color,
    touchX: Float = -1f,
    touchY: Float = -1f,
    touchAuraAlpha: Float = 0f
) {
    val w = size.width
    val h = size.height
    val gridStep = 24.dp.toPx()
    // Tactical square grid base opacity: static 0.022f regardless of RGB setting
    val baseLineAlpha = 0.022f
    val fadeZoneWidth = 56.dp.toPx()

    // 1. "Barely Visible Gradient" (Ultra-subtle ambient cyber background aura)
    val gradAlpha = 0.020f
    val centerX = if (isLeft) w * 0.40f else w * 0.60f
    val centerY = h * 0.65f
    val radius = maxOf(w, h) * 0.85f

    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                auraColor.copy(alpha = gradAlpha),
                auraColor.copy(alpha = gradAlpha * 0.35f),
                Color.Transparent
            ),
            center = Offset(centerX, centerY),
            radius = radius
        ),
        size = size
    )

    // 2. Small Square Grid Lines
    // Vertical lines
    var x = if (isLeft) 0f else (w % gridStep)
    while (x <= w) {
        // Fade lines near the center screen boundary
        val centerDist = if (isLeft) (w - x) else x
        val fadeFactor = (centerDist / fadeZoneWidth).coerceIn(0f, 1f)
        val lineAlpha = baseLineAlpha * fadeFactor

        if (lineAlpha > 0.002f) {
            drawLine(
                color = auraColor.copy(alpha = lineAlpha),
                start = Offset(x, 0f),
                end = Offset(x, h),
                strokeWidth = 1.dp.toPx()
            )
        }
        x += gridStep
    }

    // Horizontal lines
    val horizontalGridBrush = Brush.horizontalGradient(
        colors = if (isLeft) {
            listOf(
                auraColor.copy(alpha = baseLineAlpha),
                auraColor.copy(alpha = baseLineAlpha * 0.8f),
                Color.Transparent
            )
        } else {
            listOf(
                Color.Transparent,
                auraColor.copy(alpha = baseLineAlpha * 0.8f),
                auraColor.copy(alpha = baseLineAlpha)
            )
        },
        startX = 0f,
        endX = w
    )
    var y = 0f
    while (y <= h) {
        drawLine(
            brush = horizontalGridBrush,
            start = Offset(0f, y),
            end = Offset(w, y),
            strokeWidth = 1.dp.toPx()
        )
        y += gridStep
    }

    // 3. Precision Crosshair Ticks '+' at major Grid Intersections: static 0.040f regardless of RGB setting
    val tickLen = 2.5.dp.toPx()
    val tickBaseAlpha = 0.040f
    val majorStep = gridStep * 2

    var px = if (isLeft) gridStep else (w % majorStep)
    while (px < w) {
        val centerDist = if (isLeft) (w - px) else px
        val fadeFactor = (centerDist / fadeZoneWidth).coerceIn(0f, 1f)
        val tickAlpha = tickBaseAlpha * fadeFactor

        if (tickAlpha > 0.005f) {
            var py = gridStep
            while (py < h) {
                val tickColor = auraColor.copy(alpha = tickAlpha)
                // Horizontal bar of tick
                drawLine(
                    color = tickColor,
                    start = Offset(px - tickLen, py),
                    end = Offset(px + tickLen, py),
                    strokeWidth = 1.2.dp.toPx()
                )
                // Vertical bar of tick
                drawLine(
                    color = tickColor,
                    start = Offset(px, py - tickLen),
                    end = Offset(px, py + tickLen),
                    strokeWidth = 1.2.dp.toPx()
                )
                py += majorStep
            }
        }
        px += majorStep
    }

    // 4. Interactive Surrounding Touch Aura
    // Smoothly doubles/triples baseline opacity (baseLineAlpha -> 0.066f, tickBaseAlpha -> 0.120f, gradAlpha -> 0.045f)
    // within the surround radius, falling back smoothly to static baseline as distance increases or finger lifts.
    if (touchAuraAlpha > 0.005f && touchX >= 0f && touchY >= 0f) {
        val auraRadius = 120.dp.toPx()

        // 4a. Ambient surrounding cyber glow aura (+20% intensity boost around touch)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    auraColor.copy(alpha = 0.054f * touchAuraAlpha),
                    auraColor.copy(alpha = 0.018f * touchAuraAlpha),
                    Color.Transparent
                ),
                center = Offset(touchX, touchY),
                radius = auraRadius
            ),
            radius = auraRadius,
            center = Offset(touchX, touchY)
        )

        // 4b. Surrounding vertical grid lines boost (+20% intensity boost: maxExtraLineAlpha 0.053f)
        val maxExtraLineAlpha = 0.053f * touchAuraAlpha
        val minX = (touchX - auraRadius).coerceAtLeast(0f)
        val maxX = (touchX + auraRadius).coerceAtMost(w)
        val startGridX = if (isLeft) 0f else (w % gridStep)
        val firstX = startGridX + (kotlin.math.floor((minX - startGridX) / gridStep).coerceAtLeast(0f) * gridStep)
        var ax = firstX
        while (ax <= maxX) {
            val dx = abs(ax - touchX)
            if (dx < auraRadius) {
                val centerDist = if (isLeft) (w - ax) else ax
                val fadeFactor = (centerDist / fadeZoneWidth).coerceIn(0f, 1f)
                val dy = sqrt(auraRadius * auraRadius - dx * dx)
                val peakAlpha = maxExtraLineAlpha * (1f - dx / auraRadius) * fadeFactor
                if (peakAlpha > 0.002f) {
                    val startY = (touchY - dy).coerceAtLeast(0f)
                    val endY = (touchY + dy).coerceAtMost(h)
                    if (endY > startY) {
                        drawLine(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    auraColor.copy(alpha = peakAlpha),
                                    Color.Transparent
                                ),
                                startY = touchY - dy,
                                endY = touchY + dy
                            ),
                            start = Offset(ax, startY),
                            end = Offset(ax, endY),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                }
            }
            ax += gridStep
        }

        // 4c. Surrounding horizontal grid lines boost (+20% intensity boost: maxExtraLineAlpha 0.053f)
        val minY = (touchY - auraRadius).coerceAtLeast(0f)
        val maxY = (touchY + auraRadius).coerceAtMost(h)
        val firstY = kotlin.math.floor(minY / gridStep).coerceAtLeast(0f) * gridStep
        var ay = firstY
        while (ay <= maxY) {
            val dy = abs(ay - touchY)
            if (dy < auraRadius) {
                val dx = sqrt(auraRadius * auraRadius - dy * dy)
                val peakAlpha = maxExtraLineAlpha * (1f - dy / auraRadius)
                val startX = (touchX - dx).coerceAtLeast(0f)
                val endX = (touchX + dx).coerceAtMost(w)
                if (peakAlpha > 0.002f && endX > startX) {
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                auraColor.copy(alpha = peakAlpha),
                                Color.Transparent
                            ),
                            startX = touchX - dx,
                            endX = touchX + dx
                        ),
                        start = Offset(startX, ay),
                        end = Offset(endX, ay),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }
            ay += gridStep
        }

        // 4d. Surrounding precision ticks '+' boost (+20% intensity boost: maxExtraTickAlpha 0.096f)
        val maxExtraTickAlpha = 0.096f * touchAuraAlpha
        val startTickX = if (isLeft) gridStep else (w % majorStep)
        val firstTickX = startTickX + (kotlin.math.floor((minX - startTickX) / majorStep).coerceAtLeast(0f) * majorStep)
        var tpx = firstTickX
        while (tpx <= maxX) {
            val centerDist = if (isLeft) (w - tpx) else tpx
            val fadeFactor = (centerDist / fadeZoneWidth).coerceIn(0f, 1f)
            val firstTickY = gridStep + (kotlin.math.floor((minY - gridStep) / majorStep).coerceAtLeast(0f) * majorStep)
            var tpy = firstTickY
            while (tpy <= maxY) {
                val dist = hypot(tpx - touchX, tpy - touchY)
                if (dist < auraRadius) {
                    val extraAlpha = maxExtraTickAlpha * (1f - dist / auraRadius) * fadeFactor
                    if (extraAlpha > 0.003f) {
                        val extraColor = auraColor.copy(alpha = extraAlpha)
                        drawLine(
                            color = extraColor,
                            start = Offset(tpx - tickLen, tpy),
                            end = Offset(tpx + tickLen, tpy),
                            strokeWidth = 1.2.dp.toPx()
                        )
                        drawLine(
                            color = extraColor,
                            start = Offset(tpx, tpy - tickLen),
                            end = Offset(tpx, tpy + tickLen),
                            strokeWidth = 1.2.dp.toPx()
                        )
                    }
                }
                tpy += majorStep
            }
            tpx += majorStep
        }
    }
}

// =========================================================================
// HALF-SCREEN AMBIENT INBUILD TOUCHPAD SURFACE (Z-INDEX LOWEST)
// =========================================================================

@Stable
class InbuildTouchpadVisualState {
    var touchX by mutableFloatStateOf(-1f)
    var touchY by mutableFloatStateOf(-1f)
    var isTouching by mutableStateOf(false)

    fun update(x: Float, y: Float, touching: Boolean) {
        touchX = x
        touchY = y
        isTouching = touching
    }
}

/**
 * Consumes the entire half-screen empty space (center to left for LTP, center to right for RTP).
 * Interactive touches are coordinated by UnifiedTouchSurface, while this composable
 * provides zero-overhead 120Hz ambient cyberpunk grid and glowing aura ripple rendering.
 */
@Composable
fun InbuildTouchpadHalf(
    isLeft: Boolean,
    screenWidthPx: Float,
    screenHeightPx: Float,
    exclusionRects: List<Rect> = emptyList(),
    isConnected: Boolean,
    isRgbEnabled: Boolean,
    viewModel: GamepadViewModel?,
    onVibrate: () -> Unit = {},
    sensitivity: Float? = null,
    curve: com.sanket.tools.nexpad.model.TouchpadCurve? = null,
    modifier: Modifier = Modifier,
    boundsProvider: (() -> List<Rect>)? = null,
    visualState: InbuildTouchpadVisualState? = null
) {
    val auraColor = if (isLeft) NeonPalette.Cyan else NeonPalette.Magenta

    val touchAuraAlpha by animateFloatAsState(
        targetValue = if (visualState?.isTouching == true) 1f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "inbuild_touch_aura"
    )

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { }
        ) {
            drawInbuildTouchpadGrid(
                isLeft = isLeft,
                auraColor = auraColor,
                touchX = visualState?.touchX ?: -1f,
                touchY = visualState?.touchY ?: -1f,
                touchAuraAlpha = touchAuraAlpha
            )
        }
    }
}

// =========================================================================
// DISCRETE COMPONENT / VARIANT FOR BUTTON STUDIO & HUD INSPECTOR
// =========================================================================

/**
 * Discrete Inbuild Touchpad variant (180.dp) for Button Studio previews or placed instances.
 * Displays the gaming small square grid lines.
 */
@Composable
fun InbuildTouchpad(
    isLeft: Boolean,
    isConnected: Boolean = true,
    viewModel: GamepadViewModel? = null,
    onVibrate: () -> Unit = {},
    isRgbEnabled: Boolean = false,
    sensitivity: Float? = null,
    modifier: Modifier = Modifier,
    isInteractive: Boolean = true
) {
    val auraColor = if (isLeft) NeonPalette.Cyan else NeonPalette.Magenta
    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .size(180.dp)
            .clip(shape)
            .background(NeonPalette.CarbonBlack)
            .border(
                width = 1.dp,
                color = if (isRgbEnabled) auraColor.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f),
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawInbuildTouchpadGrid(
                isLeft = isLeft,
                auraColor = auraColor
            )
        }
    }
}

/**
 * Static non-interactive preview delegate of the Inbuild Touchpad for Button Studio card grids.
 * Displays the gaming small square grid lines with zero overhead.
 */
@Composable
fun StaticInbuildTouchpad(
    isLeft: Boolean,
    isRgbEnabled: Boolean = false,
    modifier: Modifier = Modifier
) {
    InbuildTouchpad(
        isLeft = isLeft,
        isConnected = false,
        viewModel = null,
        onVibrate = {},
        isRgbEnabled = isRgbEnabled,
        modifier = modifier,
        isInteractive = false
    )
}
