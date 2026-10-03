package com.sanket.tools.nexpad.ui.components.controller

import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
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
import kotlin.time.Duration.Companion.milliseconds

/**
 * In-Built Full Surface Touchpad (Consumes all empty space: Center-to-Left for LTP, Center-to-Right for RTP).
 *
 * Implements the Full-Mode touch architecture:
 * - Operates at the lowest Z-index (drawn behind all buttons/controls).
 * - 16.dp Universal Button Buffer Zone: touches within 16dp of any interactive button are completely ignored.
 * - Displays a barely visible, subtle ambient cyber gradient so the user can easily recognize active touchpad coverage.
 * - Dynamic capacitive touch puck with outer corona bloom, ripple ring, and aim vector following finger gestures.
 * - Uses calibrated 5-zone velocity transfer curve and trackball momentum coasting on release.
 */

// =========================================================================
// SHARED CANVAS DRAWING PRIMITIVES FOR INBUILD TOUCHPAD
// =========================================================================

private fun DrawScope.drawInbuildTouchpadGrid(
    isLeft: Boolean,
    auraColor: Color,
    isRgbEnabled: Boolean,
    touchX: Float = -1f,
    touchY: Float = -1f,
    activeAlpha: Float = 0f
) {
    val w = size.width
    val h = size.height
    val gridStep = 24.dp.toPx()
    val baseLineAlpha = if (isRgbEnabled) 0.080f else 0.045f
    val fadeZoneWidth = 56.dp.toPx()

    // 1. "Barely Visible Gradient" (Ultra-subtle ambient cyber background aura)
    val gradAlpha = if (isRgbEnabled) 0.040f else 0.020f
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

    // 2. Interactive Localized Grid Illumination
    if (activeAlpha > 0.01f && touchX >= 0f && touchY >= 0f) {
        val touchGlowRadius = 120.dp.toPx()
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    auraColor.copy(alpha = activeAlpha * 0.16f),
                    auraColor.copy(alpha = activeAlpha * 0.05f),
                    Color.Transparent
                ),
                center = Offset(touchX, touchY),
                radius = touchGlowRadius
            ),
            radius = touchGlowRadius,
            center = Offset(touchX, touchY)
        )
    }

    // 3. Small Square Grid Lines
    // Vertical lines
    var x = if (isLeft) 0f else (w % gridStep)
    while (x <= w) {
        // Fade lines near the center screen boundary
        val centerDist = if (isLeft) (w - x) else x
        val fadeFactor = (centerDist / fadeZoneWidth).coerceIn(0f, 1f)
        val lineAlpha = baseLineAlpha * fadeFactor

        if (lineAlpha > 0.005f) {
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
    var y = 0f
    while (y <= h) {
        val lineAlpha = baseLineAlpha
        val brush = Brush.horizontalGradient(
            colors = if (isLeft) {
                listOf(
                    auraColor.copy(alpha = lineAlpha),
                    auraColor.copy(alpha = lineAlpha * 0.8f),
                    Color.Transparent
                )
            } else {
                listOf(
                    Color.Transparent,
                    auraColor.copy(alpha = lineAlpha * 0.8f),
                    auraColor.copy(alpha = lineAlpha)
                )
            },
            startX = 0f,
            endX = w
        )

        drawLine(
            brush = brush,
            start = Offset(0f, y),
            end = Offset(w, y),
            strokeWidth = 1.dp.toPx()
        )
        y += gridStep
    }

    // 4. Precision Crosshair Ticks '+' at major Grid Intersections
    val tickLen = 2.5.dp.toPx()
    val tickBaseAlpha = if (isRgbEnabled) 0.14f else 0.08f
    val majorStep = gridStep * 2

    var px = if (isLeft) gridStep else (w % majorStep)
    while (px < w) {
        val centerDist = if (isLeft) (w - px) else px
        val fadeFactor = (centerDist / fadeZoneWidth).coerceIn(0f, 1f)
        val tickAlpha = tickBaseAlpha * fadeFactor

        if (tickAlpha > 0.01f) {
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
}

private fun DrawScope.drawInbuildActiveCapacitiveTouch(
    touchX: Float,
    touchY: Float,
    anchorX: Float,
    anchorY: Float,
    activeAlpha: Float,
    rgbBloomAlpha: Float,
    auraColor: Color,
    isRgbEnabled: Boolean
) {
    if (activeAlpha <= 0.01f) return

    val puckCenter = Offset(touchX, touchY)
    val anchorCenter = Offset(anchorX, anchorY)
    val glowColor = if (isRgbEnabled) auraColor else Color.White

    // 1. Aiming / Motion Vector Line (Connecting anchor to touch point)
    val vectorDist = hypot(touchX - anchorX, touchY - anchorY)
    if (vectorDist > 6.dp.toPx()) {
        drawLine(
            color = glowColor.copy(alpha = activeAlpha * 0.35f),
            start = anchorCenter,
            end = puckCenter,
            strokeWidth = 1.5.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawCircle(
            color = glowColor.copy(alpha = activeAlpha * 0.30f),
            radius = 3.dp.toPx(),
            center = anchorCenter
        )
    }

    // 2. Outer Corona Touch Bloom Glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                glowColor.copy(alpha = activeAlpha * rgbBloomAlpha * 0.40f),
                glowColor.copy(alpha = activeAlpha * 0.12f),
                Color.Transparent
            ),
            center = puckCenter,
            radius = 42.dp.toPx()
        ),
        radius = 42.dp.toPx(),
        center = puckCenter
    )

    // 3. Concentric Tactile Puck Rings
    drawCircle(
        color = glowColor.copy(alpha = activeAlpha * 0.85f),
        radius = 18.dp.toPx(),
        center = puckCenter,
        style = Stroke(width = 2.dp.toPx())
    )
    drawCircle(
        color = glowColor.copy(alpha = activeAlpha * 0.40f),
        radius = 28.dp.toPx(),
        center = puckCenter,
        style = Stroke(width = 1.2.dp.toPx())
    )

    // 4. Specular Core Contact Dot
    drawCircle(
        color = Color.White.copy(alpha = activeAlpha * 0.95f),
        radius = 3.5.dp.toPx(),
        center = puckCenter
    )
}

// =========================================================================
// HALF-SCREEN AMBIENT INBUILD TOUCHPAD SURFACE (Z-INDEX LOWEST)
// =========================================================================

/**
 * Consumes the entire half-screen empty space (center to left for LTP, center to right for RTP).
 * Applies 16.dp button exclusion buffer zone so all higher Z-order buttons are never interfered with.
 */
@Composable
fun InbuildTouchpadHalf(
    isLeft: Boolean,
    screenWidthPx: Float,
    screenHeightPx: Float,
    exclusionRects: List<Rect>,
    isConnected: Boolean,
    isRgbEnabled: Boolean,
    viewModel: GamepadViewModel?,
    onVibrate: () -> Unit = {},
    sensitivity: Float? = null,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val auraColor = if (isLeft) Color(0xFF00E5FF) else Color(0xFFFF007F)

    var isDragging by remember { mutableStateOf(false) }
    var touchX by remember { mutableFloatStateOf(0f) }
    var touchY by remember { mutableFloatStateOf(0f) }
    var anchorX by remember { mutableFloatStateOf(0f) }
    var anchorY by remember { mutableFloatStateOf(0f) }
    var decayJob by remember { mutableStateOf<Job?>(null) }

    val rgbBloomAlpha by animateFloatAsState(
        targetValue = if (isDragging) 0.95f else 0.40f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "inbuild_tp_rgb_bloom"
    )

    val activeAlpha by animateFloatAsState(
        targetValue = if (isDragging) 1.0f else 0.0f,
        animationSpec = tween(150),
        label = "inbuild_tp_active_alpha"
    )

    val context = LocalContext.current
    val density = LocalDensity.current.density
    val padKey = if (isLeft) "LTP" else "RTP"
    val effectiveSensitivity = remember(context, sensitivity, padKey) {
        if (sensitivity != null && sensitivity > 0f) {
            sensitivity
        } else {
            val sp = context.getSharedPreferences("nexpad_prefs", Context.MODE_PRIVATE)
            val specificSens = sp.getFloat("TOUCHPAD_SENSITIVITY_$padKey", -1f)
            if (specificSens > 0f) {
                specificSens
            } else {
                val globalPadSens = sp.getFloat("TOUCHPAD_SENSITIVITY", -1f)
                if (globalPadSens > 0f) {
                    globalPadSens
                } else {
                    val camSens = sp.getFloat("CAMERA_SENSITIVITY", 1.0f)
                    camSens * 2.0f
                }
            }
        }
    }

    val marginPx = with(LocalDensity.current) { 16.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(isLeft, effectiveSensitivity, density, exclusionRects, screenWidthPx) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)

                    // 1. Skip if already claimed by any higher Z-order control
                    if (down.isConsumed) return@awaitEachGesture

                    // Convert local half-box coords (0 .. screenWidth/2) to full container coords
                    val screenX = if (isLeft) down.position.x else down.position.x + screenWidthPx / 2f
                    val screenY = down.position.y

                    // 2. Universal element boundary exclusion check:
                    // Dilates any shape uniformly by marginPx (16dp safety buffer zone)
                    val marginSq = marginPx * marginPx
                    val nearButton = exclusionRects.any { rect ->
                        val dx = maxOf(abs(screenX - rect.center.x) - rect.width / 2f, 0f)
                        val dy = maxOf(abs(screenY - rect.center.y) - rect.height / 2f, 0f)
                        (dx * dx + dy * dy) <= marginSq
                    }
                    if (nearButton) return@awaitEachGesture

                    // Claim touch
                    down.consume()
                    onVibrate()

                    var previousTouchX = down.position.x
                    var previousTouchY = down.position.y
                    var previousTimeMs = System.currentTimeMillis()
                    var currentStickX = 0f
                    var currentStickY = 0f
                    val velocityBuffer = VelocityRingBuffer(8)

                    isDragging = true
                    touchX = down.position.x
                    touchY = down.position.y
                    anchorX = down.position.x
                    anchorY = down.position.y

                    if (isLeft) {
                        viewModel?.updateLeftStick(0f, 0f)
                    } else {
                        viewModel?.updateRightStick(0f, 0f)
                    }

                    // Drag loop
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id }
                        if (change == null || !change.pressed) break

                        val currentTouchX = change.position.x
                        val currentTouchY = change.position.y
                        val deltaX = currentTouchX - previousTouchX
                        val deltaY = currentTouchY - previousTouchY

                        previousTouchX = currentTouchX
                        previousTouchY = currentTouchY
                        change.consume()

                        touchX = currentTouchX
                        touchY = currentTouchY

                        // Directional axis stabilization: suppress minor diagonal cross-talk
                        var finalDeltaX = deltaX
                        var finalDeltaY = deltaY
                        val absX = abs(deltaX)
                        val absY = abs(deltaY)
                        if (absX > 3.0f * absY) {
                            finalDeltaY *= 0.5f
                        } else if (absY > 3.0f * absX) {
                            finalDeltaX *= 0.5f
                        }

                        val distPx = hypot(finalDeltaX, finalDeltaY)
                        val distDp = distPx / density

                        if (distDp > 0.15f) {
                            val currentTimeMs = System.currentTimeMillis()
                            val dtSec = ((currentTimeMs - previousTimeMs).coerceAtLeast(1L)) / 1000f
                            previousTimeMs = currentTimeMs

                            velocityBuffer.push(distDp, dtSec)
                            val smoothedSpeed = velocityBuffer.averageSpeed()

                            val stickMagnitude = calculateGamingStickMagnitude(smoothedSpeed, effectiveSensitivity)

                            if (stickMagnitude > 0f) {
                                val dirX = finalDeltaX / distPx
                                val dirY = finalDeltaY / distPx

                                val targetStickX = (dirX * stickMagnitude).coerceIn(-1f, 1f)
                                val targetStickY = (-dirY * stickMagnitude).coerceIn(-1f, 1f) // Up is positive Y

                                currentStickX = 0.55f * targetStickX + 0.45f * currentStickX
                                currentStickY = 0.55f * targetStickY + 0.45f * currentStickY

                                if (isLeft) {
                                    viewModel?.updateLeftStick(currentStickX, currentStickY)
                                } else {
                                    viewModel?.updateRightStick(currentStickX, currentStickY)
                                }

                                val decayTimeoutMs = (120L - (smoothedSpeed / 20f).toLong()).coerceIn(50L, 120L)
                                decayJob?.cancel()
                                decayJob = coroutineScope.launch {
                                    delay(decayTimeoutMs)
                                    currentStickX *= 0.5f
                                    currentStickY *= 0.5f
                                    if (isLeft) {
                                        viewModel?.updateLeftStick(currentStickX, currentStickY)
                                    } else {
                                        viewModel?.updateRightStick(currentStickX, currentStickY)
                                    }
                                    delay(30.milliseconds)
                                    currentStickX *= 0.2f
                                    currentStickY *= 0.2f
                                    if (isLeft) {
                                        viewModel?.updateLeftStick(currentStickX, currentStickY)
                                    } else {
                                        viewModel?.updateRightStick(currentStickX, currentStickY)
                                    }
                                    delay(25.milliseconds)
                                    currentStickX = 0f
                                    currentStickY = 0f
                                    if (isLeft) {
                                        viewModel?.updateLeftStick(0f, 0f)
                                    } else {
                                        viewModel?.updateRightStick(0f, 0f)
                                    }
                                }
                            }
                        }
                    }

                    // Gesture released
                    isDragging = false
                    val peakReleaseSpeed = velocityBuffer.peakSpeed()
                    if (peakReleaseSpeed < 80f) {
                        decayJob?.cancel()
                        if (isLeft) {
                            viewModel?.updateLeftStick(0f, 0f)
                        } else {
                            viewModel?.updateRightStick(0f, 0f)
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Gaming small square grid lines covering the entire half-screen
            drawInbuildTouchpadGrid(
                isLeft = isLeft,
                auraColor = auraColor,
                isRgbEnabled = isRgbEnabled,
                touchX = touchX,
                touchY = touchY,
                activeAlpha = activeAlpha
            )

            // Dynamic capacitive touch puck & aim vector
            drawInbuildActiveCapacitiveTouch(
                touchX = touchX,
                touchY = touchY,
                anchorX = anchorX,
                anchorY = anchorY,
                activeAlpha = activeAlpha,
                rgbBloomAlpha = rgbBloomAlpha,
                auraColor = auraColor,
                isRgbEnabled = isRgbEnabled
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
    isConnected: Boolean,
    viewModel: GamepadViewModel,
    onVibrate: () -> Unit = {},
    isRgbEnabled: Boolean = false,
    sensitivity: Float? = null,
    modifier: Modifier = Modifier
) {
    val auraColor = if (isLeft) Color(0xFF00E5FF) else Color(0xFFFF007F)
    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .size(180.dp)
            .clip(shape)
            .background(Color(0xFF06080C))
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
                auraColor = auraColor,
                isRgbEnabled = isRgbEnabled
            )
        }
    }
}

/**
 * Static non-interactive preview of the Inbuild Touchpad for Button Studio card grids.
 * Displays the gaming small square grid lines with central capacitive puck bloom.
 */
@Composable
fun StaticInbuildTouchpad(
    isLeft: Boolean,
    isRgbEnabled: Boolean = false,
    modifier: Modifier = Modifier
) {
    val auraColor = if (isLeft) Color(0xFF00E5FF) else Color(0xFFFF007F)
    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .size(180.dp)
            .clip(shape)
            .background(Color(0xFF06080C))
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
                auraColor = auraColor,
                isRgbEnabled = isRgbEnabled
            )

            // Static puck hint at center
            drawInbuildActiveCapacitiveTouch(
                touchX = size.width / 2f,
                touchY = size.height / 2f,
                anchorX = size.width / 2f,
                anchorY = size.height / 2f,
                activeAlpha = 0.70f,
                rgbBloomAlpha = 0.85f,
                auraColor = auraColor,
                isRgbEnabled = isRgbEnabled
            )
        }
    }
}
