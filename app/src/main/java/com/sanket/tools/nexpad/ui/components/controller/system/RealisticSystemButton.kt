package com.sanket.tools.nexpad.ui.components.controller.system

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.category.ControlKey
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel
import kotlin.math.roundToInt

/**
 * Draws the vector icons for NEXPAD system buttons in standard 24x24 normalized coordinate space.
 * Translates the authentic Lens HTML system SVGs into high-precision Compose DrawScope paths:
 * - BACK / VIEW: Overlapping rounded rectangles (x=4, y=8, w=12, h=12, rx=2.5 + top-right path)
 * - GUIDE / HOME: Concentric circle (r=8.5) with filled center core dot (r=3.0)
 * - START / MENU: Hamburger 3 horizontal lines (y=7, 12, 17)
 * - SHARE / CAPTURE: Xbox Series X|S Capture Tray (y=14..19.5) with Upward Arrow (x=12, y=4.5..15)
 */
internal fun DrawScope.drawSystemIcon(
    controlKey: ControlKey?,
    color: Color,
    iconSizePx: Float,
    center: Offset
) {
    val scale = iconSizePx / 24f
    val left = center.x - iconSizePx / 2f
    val top = center.y - iconSizePx / 2f

    when (controlKey) {
        ControlKey.BACK -> {
            // BACK / VIEW: Overlapping Rounded Rectangles
            val strokeW = 2.4f * scale
            val strokeStyle = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)

            // Foreground rounded rectangle
            drawRoundRect(
                color = color,
                topLeft = Offset(left + 4f * scale, top + 8f * scale),
                size = Size(12f * scale, 12f * scale),
                cornerRadius = CornerRadius(2.5f * scale, 2.5f * scale),
                style = strokeStyle
            )

            // Background open rectangle path (top & right edges)
            val backPath = Path().apply {
                moveTo(left + 8f * scale, top + 4.5f * scale)
                lineTo(left + 17.5f * scale, top + 4.5f * scale)
                quadraticTo(left + 20f * scale, top + 4.5f * scale, left + 20f * scale, top + 7f * scale)
                lineTo(left + 20f * scale, top + 16.5f * scale)
            }
            drawPath(path = backPath, color = color, style = strokeStyle)
        }

        ControlKey.GUIDE -> {
            // GUIDE / HOME: Concentric circle + filled center core dot
            drawCircle(
                color = color,
                radius = 8.5f * scale,
                center = center,
                style = Stroke(width = 3.2f * scale)
            )
            drawCircle(
                color = color,
                radius = 3.0f * scale,
                center = center
            )
        }

        ControlKey.START -> {
            // START / MENU: Hamburger 3 Horizontal Lines
            val strokeW = 2.6f * scale
            val x1 = left + 5f * scale
            val x2 = left + 19f * scale
            drawLine(color, Offset(x1, top + 7f * scale), Offset(x2, top + 7f * scale), strokeWidth = strokeW, cap = StrokeCap.Round)
            drawLine(color, Offset(x1, top + 12f * scale), Offset(x2, top + 12f * scale), strokeWidth = strokeW, cap = StrokeCap.Round)
            drawLine(color, Offset(x1, top + 17f * scale), Offset(x2, top + 17f * scale), strokeWidth = strokeW, cap = StrokeCap.Round)
        }

        ControlKey.SHARE -> {
            // SHARE / CAPTURE: Xbox Series X|S Tray + Upward Arrow
            val strokeW = 2.4f * scale
            val strokeStyle = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)

            // Bottom tray
            val trayPath = Path().apply {
                moveTo(left + 5f * scale, top + 14f * scale)
                lineTo(left + 5f * scale, top + 17.5f * scale)
                quadraticTo(left + 5f * scale, top + 19.5f * scale, left + 7f * scale, top + 19.5f * scale)
                lineTo(left + 17f * scale, top + 19.5f * scale)
                quadraticTo(left + 19f * scale, top + 19.5f * scale, left + 19f * scale, top + 17.5f * scale)
                lineTo(left + 19f * scale, top + 14f * scale)
            }
            drawPath(path = trayPath, color = color, style = strokeStyle)

            // Upward arrow stem
            drawLine(
                color = color,
                start = Offset(center.x, top + 15f * scale),
                end = Offset(center.x, top + 4.5f * scale),
                strokeWidth = strokeW,
                cap = StrokeCap.Round
            )

            // Chevron arrowhead
            val arrowHeadPath = Path().apply {
                moveTo(left + 7.5f * scale, top + 9f * scale)
                lineTo(center.x, top + 4.5f * scale)
                lineTo(left + 16.5f * scale, top + 9f * scale)
            }
            drawPath(path = arrowHeadPath, color = color, style = strokeStyle)
        }

        else -> {
            // Fallback for custom keys: small concentric indicator dot
            drawCircle(
                color = color,
                radius = 4f * scale,
                center = center
            )
        }
    }
}

/**
 * Optical Lens System Button (Guide/Home, Start/Menu, Back/View, Share/Capture).
 * Faithfully translates the NEXPAD Optical Lens System Button HTML/CSS blueprint:
 * - Multi-stop dark convex acrylic dome (.lx-body)
 * - Inset emissive neon lens ring (.lx-ring) igniting with high bloom on press
 * - Guide/Home secondary concentric telemetry ring (.lx-home .lx-ring2)
 * - Top 180° crescent specular arc (.lx-lens) and bottom-right sheen
 * - High-precision vector SVG icons matching console navigation conventions
 * - Damped harmonic spring kinematics (offsetY 2px, scale 0.95 on press)
 */
@Composable
fun RealisticSystemButton(
    key: String,
    isConnected: Boolean,
    onVibrate: () -> Unit,
    viewModel: GamepadViewModel,
    isRgbEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    val scaleAnim by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "sys_btn_scale"
    )
    val pressOffsetYAnim by animateFloatAsState(
        targetValue = if (isPressed) 2f else 0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "sys_btn_offset"
    )

    val currentOnVibrate by rememberUpdatedState(onVibrate)
    val currentViewModel by rememberUpdatedState(viewModel)

    val ctrl = remember(key) { ControlKey.fromIdentifier(key) }
    val isGuide = ctrl == ControlKey.GUIDE

    val rgbBloomAlpha by animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0.70f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "sys_btn_rgb_bloom"
    )

    // Palette: HTML specifies #d8dee9 for Back/Start and #f0f3f8 for Home
    val defaultGlow = if (isGuide) Color(0xFFF0F3F8) else Color(0xFFD8DEE9)
    val auraColor = remember(isGuide, isRgbEnabled) {
        if (isRgbEnabled) {
            if (isGuide) Color(0xFF00E5FF) else Color(0xFFD8DEE9)
        } else {
            defaultGlow
        }
    }

    val buttonSize = if (isGuide) 74.dp else 60.dp
    val iconSize = if (isGuide) 36.dp else 24.dp

    // Optical Lens radial dome: #232527 0%, #0c0d0e 75%, #000 100%
    val domeGradient = remember {
        Brush.radialGradient(
            colors = listOf(Color(0xFF232527), Color(0xFF0C0D0E), Color(0xFF000000)),
            center = Offset(0.50f, 0.55f),
            radius = 160f
        )
    }

    Box(
        modifier = modifier
            .size(buttonSize)
            .graphicsLayer {
                scaleX = scaleAnim
                scaleY = scaleAnim
            }
            .offset { IntOffset(0, pressOffsetYAnim.dp.roundToPx()) }
            .drawBehind {
                if (isRgbEnabled) {
                    val baseR = size.minDimension * 0.5f

                    // 1. Precision tactical circular halo
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                auraColor.copy(alpha = rgbBloomAlpha * (if (isPressed) 0.65f else 0.35f)),
                                auraColor.copy(alpha = rgbBloomAlpha * 0.15f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = size.minDimension * 0.75f
                        ),
                        radius = size.minDimension * 0.75f,
                        center = center
                    )

                    // 2. Dual Precision Telemetry Guide Rings
                    drawCircle(
                        color = auraColor.copy(alpha = rgbBloomAlpha * (if (isPressed) 0.85f else 0.45f)),
                        radius = baseR + 3.dp.toPx(),
                        center = center,
                        style = Stroke(width = 1.2.dp.toPx())
                    )
                    drawCircle(
                        color = auraColor.copy(alpha = rgbBloomAlpha * (if (isPressed) 0.50f else 0.20f)),
                        radius = baseR + 7.dp.toPx(),
                        center = center,
                        style = Stroke(width = 1.dp.toPx())
                    )

                    // 3. 4 Cardinal Telemetry Calibration Pips
                    val pipLen = (if (isPressed) 6.dp else 3.5.dp).toPx()
                    val pipStart = baseR + 2.dp.toPx()
                    val pipEnd = pipStart + pipLen
                    val pipAlpha = rgbBloomAlpha * (if (isPressed) 0.95f else 0.55f)

                    drawLine(auraColor.copy(alpha = pipAlpha), Offset(center.x, center.y - pipEnd), Offset(center.x, center.y - pipStart), strokeWidth = 2f)
                    drawLine(auraColor.copy(alpha = pipAlpha), Offset(center.x, center.y + pipStart), Offset(center.x, center.y + pipEnd), strokeWidth = 2f)
                    drawLine(auraColor.copy(alpha = pipAlpha), Offset(center.x - pipEnd, center.y), Offset(center.x - pipStart, center.y), strokeWidth = 2f)
                    drawLine(auraColor.copy(alpha = pipAlpha), Offset(center.x + pipStart, center.y), Offset(center.x + pipEnd, center.y), strokeWidth = 2f)
                }
            }
            .shadow(
                elevation = if (isPressed) 2.dp else 6.dp,
                shape = CircleShape,
                ambientColor = if (isRgbEnabled) auraColor else Color.Black,
                spotColor = if (isRgbEnabled) auraColor else Color.Black
            )
            .clip(CircleShape)
            .background(domeGradient)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.50f),
                        Color.Black.copy(alpha = 0.85f)
                    )
                ),
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
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val r = size.minDimension / 2f

            // Layer 1: Recessed undercut shadow (inset 0 -6px 9px rgba(0,0,0,0.70))
            val botShadowH = h * 0.38f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = if (isPressed) 0.85f else 0.70f)),
                    startY = h - botShadowH,
                    endY = h
                ),
                topLeft = Offset(0f, h - botShadowH),
                size = Size(w, botShadowH)
            )

            // Layer 2: Emissive neon lens ring (.lx-ring, inset 3px)
            val ringR = r - 3.dp.toPx()
            drawCircle(
                color = auraColor.copy(alpha = if (isPressed) 0.60f else 0.30f),
                radius = ringR,
                style = Stroke(width = 4.dp.toPx())
            )
            drawCircle(
                color = auraColor.copy(alpha = rgbBloomAlpha),
                radius = ringR,
                style = Stroke(width = 2.dp.toPx())
            )

            // Layer 3: Home secondary inner telemetry ring (.lx-home .lx-ring2, inset 14px)
            if (isGuide) {
                val ring2R = r - 12.dp.toPx()
                drawCircle(
                    color = auraColor.copy(alpha = if (isPressed) 0.70f else 0.30f),
                    radius = ring2R,
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // Layer 4: Vector SVG Icon
            if (ctrl in listOf(ControlKey.BACK, ControlKey.GUIDE, ControlKey.START, ControlKey.SHARE)) {
                val iconPx = iconSize.toPx()
                // Emissive glow shadow pass
                drawSystemIcon(
                    controlKey = ctrl,
                    color = auraColor.copy(alpha = if (isPressed) 0.50f else 0.20f),
                    iconSizePx = iconPx + 2.dp.toPx(),
                    center = center
                )
                // Core crisp stroke pass
                drawSystemIcon(
                    controlKey = ctrl,
                    color = if (isPressed) Color.White else auraColor,
                    iconSizePx = iconPx,
                    center = center
                )
            }

            // Layer 5: Top Specular Glass Crescent Arc (.lx-lens)
            drawArc(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = if (isPressed) 0.08f else 0.16f), Color.Transparent),
                    startY = 0f,
                    endY = h * 0.38f
                ),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(1.5.dp.toPx(), 1.5.dp.toPx()),
                size = Size(w - 3.dp.toPx(), h - 3.dp.toPx()),
                style = Stroke(width = 1.2.dp.toPx())
            )

            // Layer 6: Lens Sheen Reflection Oval (radial-gradient at 70% 78%)
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.07f), Color.Transparent),
                    center = Offset(w * 0.70f, h * 0.78f),
                    radius = w * 0.28f
                ),
                topLeft = Offset(w * 0.50f, h * 0.60f),
                size = Size(w * 0.38f, h * 0.32f)
            )
        }

        // Fallback for non-standard keys outside the 4 canonical system controls
        if (ctrl !in listOf(ControlKey.BACK, ControlKey.GUIDE, ControlKey.START, ControlKey.SHARE)) {
            Text(
                text = key.take(2),
                color = if (isPressed) Color.White else auraColor,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
    }
}
