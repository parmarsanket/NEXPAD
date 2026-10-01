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
    isConnected: Boolean,
    onVibrate: () -> Unit,
    viewModel: GamepadViewModel,
    isRgbEnabled: Boolean,
    modifier: Modifier = Modifier,
    displayLabel: String? = null
) {
    var isPressed by remember { mutableStateOf(false) }
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
    val scaleAnim by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "rib_bumper_scale"
    )
    val pressOffsetYAnim by animateFloatAsState(
        targetValue = if (isPressed) 2.0f else 0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "rib_bumper_offset"
    )
    val ringBloomAlpha by animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0.70f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 800f),
        label = "rib_ring_bloom"
    )
    val stripAlphaAnim by animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0.22f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 600f),
        label = "rib_strip_alpha"
    )

    // CSS: --glow: #3fd2ff (Cyan neon)
    val neonColor = remember(isRgbEnabled, isLeft) {
        if (isRgbEnabled) {
            if (isLeft) Color(0xFF3FD2FF) else Color(0xFFFF007F)
        } else {
            Color(0xFFD8DEE9)
        }
    }

    val currentOnVibrate by rememberUpdatedState(onVibrate)
    val currentViewModel by rememberUpdatedState(viewModel)

    // Convex dark dome background gradient
    val baseDomeGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF232527),
                Color(0xFF0C0D0E),
                Color(0xFF000000)
            ),
            center = Offset(0.50f, 0.55f),
            radius = 260f
        )
    }

    val pressedDomeGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF1B1D1E),
                Color(0xFF070808),
                Color(0xFF000000)
            ),
            center = Offset(0.50f, 0.58f),
            radius = 260f
        )
    }

    // Recessed 3D window background
    val windowGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF050506),
                Color(0xFF121314)
            ),
            center = Offset(0.50f, 0.60f),
            radius = 120f
        )
    }

    Box(
        modifier = modifier
            .size(154.dp, 54.dp)
            // Emissive halo bloom behind housing
            .drawBehind {
                val haloColor = neonColor.copy(alpha = if (isPressed) 0.45f else 0.18f)
                val haloOffset = if (isPressed) 8.dp.toPx() else 5.dp.toPx()
                val rOuter = 27.dp.toPx() + haloOffset
                val rInner = 12.dp.toPx() + haloOffset
                val haloPath = Path().apply {
                    addRoundRect(
                        RoundRect(
                            rect = Rect(-haloOffset, -haloOffset, size.width + haloOffset, size.height + haloOffset),
                            topLeft = CornerRadius(if (isLeft) rInner else rOuter),
                            topRight = CornerRadius(if (isLeft) rOuter else rInner),
                            bottomRight = CornerRadius(if (isLeft) rOuter else rInner),
                            bottomLeft = CornerRadius(if (isLeft) rInner else rOuter)
                        )
                    )
                }
                drawPath(path = haloPath, color = haloColor)
            }
            .graphicsLayer {
                scaleX = scaleAnim
                scaleY = scaleAnim
            }
            .offset { IntOffset(0, pressOffsetYAnim.dp.roundToPx()) }
            // Outer drop shadow
            .shadow(
                elevation = if (isPressed) 2.dp else 7.dp,
                shape = bumperShape,
                ambientColor = Color.Black.copy(alpha = 0.55f),
                spotColor = Color.Black
            )
            .clip(bumperShape)
            .background(if (isPressed) pressedDomeGradient else baseDomeGradient)
            // 1px casing rim
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.50f),
                        Color.Black.copy(alpha = 0.80f)
                    )
                ),
                shape = bumperShape
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
        // Multi-Layer Canvas: Undercut shadow, Ribbed texture lines, Neon Ring, Lens highlights
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Inset bottom shadow
            val insetH = h * 0.40f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = if (isPressed) 0.85f else 0.70f)),
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

            // Neon color ring — inset 2.5dp with per-corner radii
            val ringInset = 2.5.dp.toPx()
            val ringROuter = 27.dp.toPx() - ringInset
            val ringRInner = 12.dp.toPx() - ringInset
            val ringPath = Path().apply {
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

            // Outer bloom pass
            drawPath(
                path = ringPath,
                color = neonColor.copy(alpha = (if (isPressed) 0.60f else 0.28f) * ringBloomAlpha),
                style = Stroke(width = 4.dp.toPx())
            )
            // Core crisp stroke
            drawPath(
                path = ringPath,
                color = neonColor.copy(alpha = (if (isPressed) 1.0f else 0.70f) * ringBloomAlpha),
                style = Stroke(width = 1.8.dp.toPx())
            )

            // Top crescent specular sheen (glass lens)
            val sheenH = h * 0.40f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isPressed) 0.05f else 0.14f),
                        Color.White.copy(alpha = if (isPressed) 0.01f else 0.04f),
                        Color.Transparent
                    ),
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
                    colors = listOf(
                        Color.White.copy(alpha = if (isPressed) 0.12f else 0.45f),
                        Color.White.copy(alpha = if (isPressed) 0.04f else 0.16f)
                    ),
                    startX = specStartX,
                    endX = specEndX
                ),
                start = Offset(specStartX, 4f),
                end = Offset(specEndX, 4f),
                strokeWidth = 1.5f
            )

            // Bottom-right specular sheen circle
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.06f), Color.Transparent),
                    center = Offset(w * 0.70f, h * 0.78f),
                    radius = w * 0.20f
                ),
                center = Offset(w * 0.70f, h * 0.78f),
                radius = w * 0.20f
            )
        }

        // Elevated Recessed Magnifier Window (.lx-window: 74dp × 30dp, top: 42%)
        val windowShape = RoundedCornerShape(15.dp)
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
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.90f), Color.Transparent),
                        startY = 0f,
                        endY = 6.dp.toPx()
                    ),
                    size = Size(winW, 6.dp.toPx())
                )

                // Deep vignette
                drawRect(
                    brush = Brush.radialGradient(
                        0.0f to Color.Transparent,
                        0.32f to Color.Transparent,
                        1.0f to Color.Black.copy(alpha = 0.82f),
                        center = Offset(winW / 2f, winH / 2f),
                        radius = winW * 0.50f
                    )
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
        val stripShape = RoundedCornerShape(2.dp)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 9.dp)
                .size(width = 110.dp, height = 4.dp)
                // Pressed bloom aura
                .drawBehind {
                    if (isPressed) {
                        drawRoundRect(
                            color = neonColor.copy(alpha = 0.65f),
                            size = Size(size.width + 12.dp.toPx(), size.height + 8.dp.toPx()),
                            topLeft = Offset(-6.dp.toPx(), -4.dp.toPx()),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }
                }
                .clip(stripShape)
                .background(neonColor.copy(alpha = stripAlphaAnim))
        )
    }
}
