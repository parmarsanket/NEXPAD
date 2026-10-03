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

private fun DrawScope.drawInbuildTouchpadAesthetics(
    isLeft: Boolean,
    auraColor: Color,
    isRgbEnabled: Boolean
) {
    val w = size.width
    val h = size.height

    // 1. "Barely Visible Gradient" (Ultra-subtle ambient cyber aura)
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

    // 2. Subtle Precision Corner Registration L-Brackets
    val bracketAlpha = if (isRgbEnabled) 0.12f else 0.06f
    val bracketLen = 14.dp.toPx()
    val inset = 8.dp.toPx()
    val strokeWidth = 1.5.dp.toPx()

    // Top-Left
    drawLine(auraColor.copy(alpha = bracketAlpha), Offset(inset, inset), Offset(inset + bracketLen, inset), strokeWidth, StrokeCap.Square)
    drawLine(auraColor.copy(alpha = bracketAlpha), Offset(inset, inset), Offset(inset, inset + bracketLen), strokeWidth, StrokeCap.Square)

    // Top-Right
    drawLine(auraColor.copy(alpha = bracketAlpha), Offset(w - inset, inset), Offset(w - inset - bracketLen, inset), strokeWidth, StrokeCap.Square)
    drawLine(auraColor.copy(alpha = bracketAlpha), Offset(w - inset, inset), Offset(w - inset, inset + bracketLen), strokeWidth, StrokeCap.Square)

    // Bottom-Left
    drawLine(auraColor.copy(alpha = bracketAlpha), Offset(inset, h - inset), Offset(inset + bracketLen, h - inset), strokeWidth, StrokeCap.Square)
    drawLine(auraColor.copy(alpha = bracketAlpha), Offset(inset, h - inset), Offset(inset, h - inset - bracketLen), strokeWidth, StrokeCap.Square)

    // Bottom-Right
    drawLine(auraColor.copy(alpha = bracketAlpha), Offset(w - inset, h - inset), Offset(w - inset - bracketLen, h - inset), strokeWidth, StrokeCap.Square)
    drawLine(auraColor.copy(alpha = bracketAlpha), Offset(w - inset, h - inset), Offset(w - inset, h - inset - bracketLen), strokeWidth, StrokeCap.Square)

    // 3. Subtle Ergonomic Thumb Arc Guide (Dashed Range Sweep)
    val thumbPivot = Offset(if (isLeft) 0f else w, h)
    val arcRadius1 = minOf(w, h) * 0.60f
    val arcRadius2 = minOf(w, h) * 0.90f
    val arcAlpha = if (isRgbEnabled) 0.035f else 0.018f
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 6.dp.toPx()), 0f)

    drawCircle(
        color = auraColor.copy(alpha = arcAlpha),
        radius = arcRadius1,
        center = thumbPivot,
        style = Stroke(width = 1.dp.toPx(), pathEffect = dashEffect)
    )
    drawCircle(
        color = auraColor.copy(alpha = arcAlpha * 0.7f),
        radius = arcRadius2,
        center = thumbPivot,
        style = Stroke(width = 1.dp.toPx(), pathEffect = dashEffect)
    )
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
            // Background subtle gradient & cyber telemetry
            drawInbuildTouchpadAesthetics(
                isLeft = isLeft,
                auraColor = auraColor,
                isRgbEnabled = isRgbEnabled
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
    val shape = RoundedCornerShape(26.dp)

    Box(
        modifier = modifier
            .size(180.dp)
            .clip(shape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1E2127),
                        Color(0xFF101216),
                        Color(0xFF07080A)
                    ),
                    center = Offset(0.4f, 0.4f),
                    radius = 280f
                )
            )
            .border(
                width = 1.dp,
                color = if (isRgbEnabled) auraColor.copy(alpha = 0.40f) else Color.White.copy(alpha = 0.15f),
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawInbuildTouchpadAesthetics(
                isLeft = isLeft,
                auraColor = auraColor,
                isRgbEnabled = isRgbEnabled
            )
        }
    }
}

/**
 * Static non-interactive preview of the Inbuild Touchpad for Button Studio card grids.
 */
@Composable
fun StaticInbuildTouchpad(
    isLeft: Boolean,
    isRgbEnabled: Boolean = false,
    modifier: Modifier = Modifier
) {
    val auraColor = if (isLeft) Color(0xFF00E5FF) else Color(0xFFFF007F)
    val shape = RoundedCornerShape(26.dp)

    Box(
        modifier = modifier
            .size(180.dp)
            .clip(shape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1E2127),
                        Color(0xFF101216),
                        Color(0xFF07080A)
                    ),
                    center = Offset(0.4f, 0.4f),
                    radius = 280f
                )
            )
            .border(
                width = 1.dp,
                color = if (isRgbEnabled) auraColor.copy(alpha = 0.40f) else Color.White.copy(alpha = 0.15f),
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawInbuildTouchpadAesthetics(
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
