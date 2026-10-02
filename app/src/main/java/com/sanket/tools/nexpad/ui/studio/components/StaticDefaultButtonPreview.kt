package com.sanket.tools.nexpad.ui.studio.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.category.CategoryManager
import com.sanket.tools.nexpad.category.CategoryType
import com.sanket.tools.nexpad.category.ControlKey
import com.sanket.tools.nexpad.category.ControllerLabelStyle
import com.sanket.tools.nexpad.ui.components.controller.PlayStationSymbol
import com.sanket.tools.nexpad.ui.components.controller.getPlayStationShape
import com.sanket.tools.nexpad.ui.components.controller.ButtonPerspectiveDirection
import com.sanket.tools.nexpad.ui.components.controller.BottomHorizonClipShape
import com.sanket.tools.nexpad.ui.components.controller.TopHorizonClipShape
import com.sanket.tools.nexpad.ui.components.controller.LeftHorizonClipShape
import com.sanket.tools.nexpad.ui.components.controller.RightHorizonClipShape
import com.sanket.tools.nexpad.ui.components.controller.drawSystemIcon
import androidx.compose.ui.unit.IntOffset
import com.sanket.tools.nexpad.ui.theme.NeonPalette
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

private val crossShape = GenericShape { size, _ ->
    val thirdW = size.width / 3f
    val thirdH = size.height / 3f
    val twoThirdW = thirdW * 2f
    val twoThirdH = thirdH * 2f

    moveTo(thirdW, 0f)
    lineTo(twoThirdW, 0f)
    lineTo(twoThirdW, thirdH)
    lineTo(size.width, thirdH)
    lineTo(size.width, twoThirdH)
    lineTo(twoThirdW, twoThirdH)
    lineTo(twoThirdW, size.height)
    lineTo(thirdW, size.height)
    lineTo(thirdW, twoThirdH)
    lineTo(0f, twoThirdH)
    lineTo(0f, thirdH)
    lineTo(thirdW, thirdH)
    close()
}

/**
 * Pixel-perfect static preview for native controller elements.
 * Exactly mirrors the visuals, gradients, 3D glossy highlights, and shadows
 * of ui/components/controller/Realistic*.kt without interactive overhead.
 */
@Composable
fun StaticDefaultButtonPreview(
    controlKey: String,
    modifier: Modifier = Modifier,
    labelStyle: ControllerLabelStyle = ControllerLabelStyle.XBOX
) {
    val ctrl = ControlKey.fromIdentifier(controlKey)
    val key = ctrl?.key ?: controlKey.uppercase()
    val displayLabel = CategoryManager.getLabelForStyle(key, labelStyle)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        when (ctrl) {
            ControlKey.A -> StaticRealisticButton(key = displayLabel, buttonColor = Color(0xFF3FD25A))
            ControlKey.B -> StaticRealisticButton(key = displayLabel, buttonColor = Color(0xFFE6474E))
            ControlKey.X -> StaticRealisticButton(key = displayLabel, buttonColor = Color(0xFF3F8FE0))
            ControlKey.Y -> StaticRealisticButton(key = displayLabel, buttonColor = Color(0xFFE0A03F))

            ControlKey.LS -> StaticRealisticJoystick(isLeft = true)
            ControlKey.RS -> StaticRealisticJoystick(isLeft = false)
            ControlKey.LSB -> StaticRealisticStickButton(isLeft = true, label = displayLabel)
            ControlKey.RSB -> StaticRealisticStickButton(isLeft = false, label = displayLabel)
            ControlKey.LTP -> StaticRealisticTouchPad(isLeft = true)
            ControlKey.RTP -> StaticRealisticTouchPad(isLeft = false)

            ControlKey.DPAD -> StaticRealisticDPad()
            ControlKey.UP, ControlKey.DOWN, ControlKey.LEFT, ControlKey.RIGHT -> StaticRealisticDPadButton(direction = key)

            ControlKey.LT -> StaticRealisticTrigger(key = displayLabel, isLeft = true)
            ControlKey.RT -> StaticRealisticTrigger(key = displayLabel, isLeft = false)

            ControlKey.LB -> StaticRealisticBumper(key = displayLabel, isLeft = true)
            ControlKey.RB -> StaticRealisticBumper(key = displayLabel, isLeft = false)

            ControlKey.GUIDE -> StaticRealisticSystemButton(label = "⨂", textColor = NeonPalette.Cyan)
            ControlKey.START -> StaticRealisticSystemButton(label = "☰", textColor = Color.White)
            ControlKey.BACK  -> StaticRealisticSystemButton(label = "⧉", textColor = Color.White)
            ControlKey.SHARE -> StaticRealisticSystemButton(label = "⇪", textColor = Color.White)

            ControlKey.M1, ControlKey.M2, ControlKey.M3, ControlKey.M4 -> StaticRealisticMacroButton(label = key)

            else -> when (ctrl?.categoryType) {
                CategoryType.SYSTEM -> StaticRealisticSystemButton(label = key.take(2), textColor = Color.White)
                CategoryType.MACROS -> StaticRealisticMacroButton(label = key)
                else -> StaticRealisticButton(key = displayLabel.take(3), buttonColor = Color.Gray)
            }
        }
    }
}

/**
 * Exact visual match for ui/components/controller/RealisticButton.kt
 */
@Composable
private fun StaticRealisticButton(
    key: String,
    buttonColor: Color,
    modifier: Modifier = Modifier
) {
    val perspectiveDirection = when {
        key.equals("A", ignoreCase = true) || key.equals("CROSS", ignoreCase = true) || key.equals("DOWN", ignoreCase = true) -> ButtonPerspectiveDirection.BOTTOM
        key.equals("Y", ignoreCase = true) || key.equals("TRIANGLE", ignoreCase = true) || key.equals("UP", ignoreCase = true) -> ButtonPerspectiveDirection.TOP
        key.equals("X", ignoreCase = true) || key.equals("SQUARE", ignoreCase = true) || key.equals("LEFT", ignoreCase = true) -> ButtonPerspectiveDirection.LEFT
        key.equals("B", ignoreCase = true) || key.equals("CIRCLE", ignoreCase = true) || key.equals("RIGHT", ignoreCase = true) -> ButtonPerspectiveDirection.RIGHT
        else -> ButtonPerspectiveDirection.NONE
    }

    val horizonClipShape: androidx.compose.ui.graphics.Shape? = when (perspectiveDirection) {
        ButtonPerspectiveDirection.BOTTOM -> BottomHorizonClipShape
        ButtonPerspectiveDirection.TOP -> TopHorizonClipShape
        ButtonPerspectiveDirection.LEFT -> LeftHorizonClipShape
        ButtonPerspectiveDirection.RIGHT -> RightHorizonClipShape
        ButtonPerspectiveDirection.NONE -> null
    }

    val baseDomeGradient = Brush.radialGradient(
        colors = listOf(
            Color(0xFF232527),
            Color(0xFF0C0D0E),
            Color(0xFF000000)
        ),
        center = Offset(0.50f, 0.55f),
        radius = 200f
    )

    Box(
        modifier = modifier
            .size(80.dp)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            buttonColor.copy(alpha = 0.45f * 0.55f),
                            buttonColor.copy(alpha = 0.45f * 0.22f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.minDimension * 1f
                    ),
                    radius = size.minDimension * 1f
                )
            }
            .shadow(
                elevation = 4.dp,
                shape = CircleShape,
                ambientColor = buttonColor,
                spotColor = buttonColor
            )
            .clip(CircleShape)
            .background(baseDomeGradient),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val r = size.minDimension / 2f

            // 0 0 0 1px rgba(0, 0, 0, 0.5) cap outline
            drawCircle(
                color = Color.Black.copy(alpha = 0.50f),
                radius = r - 0.5.dp.toPx(),
                style = Stroke(width = 1.dp.toPx())
            )

            // Inset bottom shadow: inset 0 -6px 9px rgba(0, 0, 0, 0.70)
            val insetShadowH = h * 0.35f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.70f)),
                    startY = h - insetShadowH,
                    endY = h
                ),
                topLeft = Offset(0f, h - insetShadowH),
                size = Size(w, insetShadowH)
            )

            // .cap::after: Directional Optical Recess Ellipse
            when (perspectiveDirection) {
                ButtonPerspectiveDirection.BOTTOM -> {
                    val recessW = w * 0.80f
                    val recessH = h * 0.26f
                    val recessLeft = (w - recessW) / 2f
                    val recessBottom = h - (h * 0.04f)
                    val recessTop = recessBottom - recessH

                    drawOval(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.07f),
                                Color.White.copy(alpha = 0.018f),
                                Color(0xFF080A0B).copy(alpha = 0.18f),
                                Color.Black.copy(alpha = 0.52f),
                                Color.Black.copy(alpha = 0.86f)
                            ),
                            center = Offset(w * 0.5f, recessTop + recessH * 0.05f),
                            radius = recessW * 0.5f
                        ),
                        topLeft = Offset(recessLeft, recessTop),
                        size = Size(recessW, recessH)
                    )
                }
                ButtonPerspectiveDirection.TOP -> {
                    val recessW = w * 0.80f
                    val recessH = h * 0.26f
                    val recessLeft = (w - recessW) / 2f
                    val recessTop = h * 0.04f

                    drawOval(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.07f),
                                Color.White.copy(alpha = 0.018f),
                                Color(0xFF080A0B).copy(alpha = 0.18f),
                                Color.Black.copy(alpha = 0.52f),
                                Color.Black.copy(alpha = 0.86f)
                            ),
                            center = Offset(w * 0.5f, recessTop + recessH * 0.95f),
                            radius = recessW * 0.5f
                        ),
                        topLeft = Offset(recessLeft, recessTop),
                        size = Size(recessW, recessH)
                    )
                }
                ButtonPerspectiveDirection.LEFT -> {
                    val recessW = w * 0.26f
                    val recessH = h * 0.80f
                    val recessLeft = w * 0.04f
                    val recessTop = (h - recessH) / 2f

                    drawOval(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.07f),
                                Color.White.copy(alpha = 0.018f),
                                Color(0xFF080A0B).copy(alpha = 0.18f),
                                Color.Black.copy(alpha = 0.52f),
                                Color.Black.copy(alpha = 0.86f)
                            ),
                            center = Offset(recessLeft + recessW * 0.95f, h * 0.5f),
                            radius = recessH * 0.5f
                        ),
                        topLeft = Offset(recessLeft, recessTop),
                        size = Size(recessW, recessH)
                    )
                }
                ButtonPerspectiveDirection.RIGHT -> {
                    val recessW = w * 0.26f
                    val recessH = h * 0.80f
                    val recessLeft = w - recessW - (w * 0.04f)
                    val recessTop = (h - recessH) / 2f

                    drawOval(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.07f),
                                Color.White.copy(alpha = 0.018f),
                                Color(0xFF080A0B).copy(alpha = 0.18f),
                                Color.Black.copy(alpha = 0.52f),
                                Color.Black.copy(alpha = 0.86f)
                            ),
                            center = Offset(recessLeft + recessW * 0.05f, h * 0.5f),
                            radius = recessH * 0.5f
                        ),
                        topLeft = Offset(recessLeft, recessTop),
                        size = Size(recessW, recessH)
                    )
                }
                ButtonPerspectiveDirection.NONE -> {
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.35f),
                        radius = r - 2.dp.toPx(),
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
            }

            // .ring: Inner Glowing Neon Color Ring
            val ringRadius = r - 3.dp.toPx()
            // Outer bloom
            drawCircle(
                color = buttonColor.copy(alpha = 0.25f),
                radius = ringRadius,
                style = Stroke(width = 4.dp.toPx())
            )
            // Core crisp ring
            drawCircle(
                color = buttonColor.copy(alpha = 0.70f),
                radius = ringRadius,
                style = Stroke(width = 2.dp.toPx())
            )

            // Layer #6: Socket Bevel Rim — buttonColor outside neon ring
            drawCircle(
                color = buttonColor.copy(alpha = 0.12f),
                radius = ringRadius + 1.5.dp.toPx(),
                style = Stroke(width = 2.dp.toPx())
            )

            // .lens: Acrylic Glass Lens Specular Reflections
            val lensInset = 1.dp.toPx()
            drawArc(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.14f),
                        Color.White.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = h * 0.40f
                ),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(lensInset, lensInset),
                size = Size(w - lensInset * 2f, h - lensInset * 2f),
                style = Stroke(width = 1.dp.toPx())
            )

            // Bottom-right specular sheen
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.06f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.70f, h * 0.78f),
                    radius = w * 0.25f
                ),
                center = Offset(w * 0.70f, h * 0.78f),
                radius = w * 0.25f
            )
        }

        // .glyph: Magnified Optical Glyph with Directional Shift & Horizon Clipping
        val psShape = getPlayStationShape(key)
        val glyphOffset = when (perspectiveDirection) {
            ButtonPerspectiveDirection.BOTTOM -> IntOffset(0, 7)
            ButtonPerspectiveDirection.TOP    -> IntOffset(0, -7)
            ButtonPerspectiveDirection.LEFT   -> IntOffset(-7, 0)
            ButtonPerspectiveDirection.RIGHT  -> IntOffset(7, 0)
            ButtonPerspectiveDirection.NONE   -> IntOffset(0, 0)
        }

        val glyphModifier = Modifier
            .offset { glyphOffset }
            .then(if (horizonClipShape != null) Modifier.clip(horizonClipShape) else Modifier)

        if (psShape != null) {
            PlayStationSymbol(
                shape = psShape,
                color = buttonColor,
                size = 40.dp,
                modifier = glyphModifier
            )
        } else {
            Text(
                text = key,
                color = buttonColor,
                fontSize = 46.sp,
                fontWeight = FontWeight.Bold,
                modifier = glyphModifier
            )
        }
    }
}

/**
 * Exact visual match for ui/components/controller/RealisticJoystick.kt
 */

@Composable
private fun StaticRealisticJoystick(
    isLeft: Boolean,
    modifier: Modifier = Modifier
) {
    val baseGradient = Brush.radialGradient(
        colors = listOf(Color(0xFF1F1F1F), Color(0xFF080808)),
        center = Offset(0.5f, 0.5f),
        radius = 250f
    )

    val thumbGradient = Brush.radialGradient(
        colors = listOf(Color(0xFF3D3D3D), Color(0xFF1A1A1A)),
        center = Offset(0.3f, 0.3f),
        radius = 150f
    )

    val neonColor = if (isLeft) Color(0xFF00E5FF) else Color(0xFFFF007F)
    val shadow = Modifier
        .drawBehind {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        neonColor.copy(alpha = 0.40f * 0.45f),
                        neonColor.copy(alpha = 0.40f * 0.18f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.minDimension * 0.95f
                ),
                radius = size.minDimension * 0.95f
            )
        }
        .shadow(10.dp, CircleShape, ambientColor = neonColor.copy(alpha = 0.5f), spotColor = neonColor)

    Box(
        modifier = modifier
            .size(140.dp)
            .then(shadow)
            .clip(CircleShape)
            .background(baseGradient),
        contentAlignment = Alignment.Center
    ) {
        // Base plate canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color(0xFF333333),
                radius = size.minDimension / 2f - 2f,
                style = Stroke(width = 4f)
            )
        }

        // Thumbstick cap
        Box(
            modifier = Modifier
                .size(70.dp)
                .shadow(10.dp, CircleShape)
                .clip(CircleShape)
                .background(thumbGradient),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                drawCircle(
                    color = Color(0xFF111111),
                    radius = size.minDimension / 2f,
                    style = Stroke(width = 3f)
                )
            }
            Text(
                text = if (isLeft) com.sanket.tools.nexpad.model.NexpadKeys.LS
                       else com.sanket.tools.nexpad.model.NexpadKeys.RS,
                color = Color.LightGray.copy(alpha = 0.7f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Exact visual match for ui/components/controller/FluxJoystick.kt
 */
@Preview
@Composable
internal fun StaticFluxJoystick(
    isLeft: Boolean = true,
    modifier: Modifier = Modifier
) {
    val glowColor = if (isLeft) Color(0xFF2FD4B6) else Color(0xFFFF3185)

    val socketGradient = Brush.radialGradient(
        colors = listOf(Color(0xFF030304), Color(0xFF090A0B), Color(0xFF191B1E)),
        center = Offset(0.5f, 0.5f),
        radius = 280f
    )

    val capDomeGradient = Brush.radialGradient(
        colors = listOf(Color(0xFF3D4145), Color(0xFF202327), Color(0xFF111316), Color(0xFF050506)),
        center = Offset(0.50f, 0.36f),
        radius = 220f
    )

    val dishGradient = Brush.radialGradient(
        colors = listOf(Color(0xFF020203), Color(0xFF0B0C0D), Color(0xFF161719)),
        center = Offset(0.50f, 0.58f),
        radius = 100f
    )

    Box(
        modifier = modifier
            .size(150.dp)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glowColor.copy(alpha = 0.40f * 0.45f),
                            glowColor.copy(alpha = 0.40f * 0.18f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.minDimension * 0.95f
                    ),
                    radius = size.minDimension * 0.95f
                )
            }
            .shadow(12.dp, CircleShape, ambientColor = glowColor.copy(alpha = 0.5f), spotColor = glowColor)
            .clip(CircleShape)
            .background(socketGradient)
            .border(1.dp, Color.Black.copy(alpha = 0.75f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val r = size.minDimension / 2f
            val center = Offset(w / 2f, h / 2f)

            // Deep top inset shadow
            drawRect(
                brush = Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.96f), Color.Transparent), 0f, h * 0.38f),
                topLeft = Offset.Zero,
                size = Size(w, h * 0.38f)
            )

            // 12 Graduation Ticks
            val tickOuterR = r - 12.dp.toPx()
            val tickInnerR = tickOuterR - 6.dp.toPx()
            val tickColor = Color.White.copy(alpha = 0.20f)
            for (i in 0 until 12) {
                val tickAngle = (i * 30.0 - 90.0) * (Math.PI / 180.0)
                val cosA = Math.cos(tickAngle).toFloat()
                val sinA = Math.sin(tickAngle).toFloat()
                drawLine(
                    color = tickColor,
                    start = Offset(center.x + cosA * tickInnerR, center.y + sinA * tickInnerR),
                    end = Offset(center.x + cosA * tickOuterR, center.y + sinA * tickOuterR),
                    strokeWidth = 1.8.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Baseline drop shadow behind cap
            val capR = 48.dp.toPx()
            drawCircle(
                brush = Brush.radialGradient(listOf(Color.Black.copy(alpha = 0.82f), Color.Transparent), center = Offset(center.x, center.y + 8.dp.toPx()), radius = capR + 10.dp.toPx()),
                center = Offset(center.x, center.y + 8.dp.toPx()),
                radius = capR + 10.dp.toPx()
            )
        }

        // Cap
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(capDomeGradient)
                .border(1.dp, Color.Black.copy(alpha = 0.68f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val capW = size.width
                val capH = size.height
                val capR = size.minDimension / 2f

                // Inset top specular arc
                drawArc(Color.White.copy(alpha = 0.12f), 180f, 180f, false, Offset(1.5.dp.toPx(), 1.5.dp.toPx()), Size(capW - 3.dp.toPx(), capH - 3.dp.toPx()), style = Stroke(1.5.dp.toPx()))

                // Inset bottom shadow
                drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.84f)), capH * 0.65f, capH), Offset(0f, capH * 0.65f), Size(capW, capH * 0.35f))

                // Knurled grip dashed ring
                drawCircle(Color.White.copy(alpha = 0.075f), radius = capR - 5.dp.toPx(), style = Stroke(width = 4.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.5f, 5f), 0f)))

                // Cap neon ring
                val capRingRadius = capR - 13.dp.toPx()
                drawCircle(glowColor.copy(alpha = 0.35f), radius = capRingRadius, style = Stroke(width = 4.dp.toPx()))
                drawCircle(glowColor.copy(alpha = 0.80f), radius = capRingRadius, style = Stroke(width = 2.dp.toPx()))

                // Specular lens reflection
                drawOval(
                    brush = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.22f), Color.Transparent), Offset(capW * 0.50f, capH * 0.20f), capW * 0.30f),
                    topLeft = Offset(capW * 0.25f, capH * 0.06f),
                    size = Size(capW * 0.50f, capH * 0.28f)
                )
            }

            // Center Dish
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(dishGradient)
                    .border(1.dp, Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.95f), Color.White.copy(alpha = 0.055f))), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Neon Glyph (.lx-g): "L" / "R" with dual-layer neon bloom matching font-size: 40px
                Text(
                    text = if (isLeft) "L" else "R",
                    color = glowColor.copy(alpha = 0.35f),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.offset(0.dp, (-0.5).dp)
                )
                Text(
                    text = if (isLeft) "L" else "R",
                    color = glowColor,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Top Overlay Canvas: Outer Neon Ring (.lx-ring, z-index: 10) & Glass Lens (.lx-lens, z-index: 20)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val r = size.minDimension / 2f

            // Outer Neon Ring (.lx-ring, z-index: 10)
            val outerRingRadius = r - 3.dp.toPx()
            drawCircle(glowColor.copy(alpha = 0.22f), radius = outerRingRadius, style = Stroke(width = 6.dp.toPx()))
            drawCircle(glowColor.copy(alpha = 0.18f), radius = outerRingRadius - 2.dp.toPx(), style = Stroke(width = 3.dp.toPx()))
            drawCircle(glowColor.copy(alpha = 0.48f), radius = outerRingRadius, style = Stroke(width = 2.dp.toPx()))

            // Outer Glass Lens (.lx-lens, z-index: 20): Top chamfer & specular sheen
            drawArc(Color.White.copy(alpha = 0.11f), 180f, 180f, false, Offset(1.dp.toPx(), 1.dp.toPx()), Size(w - 2.dp.toPx(), h - 2.dp.toPx()), style = Stroke(1.dp.toPx()))
            drawArc(Color.Black.copy(alpha = 0.30f), 0f, 180f, false, Offset(1.dp.toPx(), 1.dp.toPx()), Size(w - 2.dp.toPx(), h - 2.dp.toPx()), style = Stroke(1.dp.toPx()))
            drawCircle(
                brush = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.055f), Color.Transparent), Offset(w * 0.70f, h * 0.78f), w * 0.20f),
                center = Offset(w * 0.70f, h * 0.78f),
                radius = w * 0.20f
            )
        }
    }
}

/**
 * Exact visual match for ui/components/controller/FluxStickButton (in FluxJoystick.kt)
 */
@Preview
@Composable
internal fun StaticFluxStickButton(
    isLeft: Boolean = true,
    modifier: Modifier = Modifier,
    label: String? = null
) {
    val key = label ?: if (isLeft) "LSB" else "RSB"
    val glowColor = if (isLeft) Color(0xFF2FD4B6) else Color(0xFFFF3185)

    val capDomeGradient = Brush.radialGradient(
        colors = listOf(
            Color(0xFF3D4145),
            Color(0xFF202327),
            Color(0xFF111316),
            Color(0xFF050506)
        ),
        center = Offset(0.50f, 0.36f),
        radius = 180f
    )

    val dishGradient = Brush.radialGradient(
        colors = listOf(
            Color(0xFF020203),
            Color(0xFF0B0C0D),
            Color(0xFF161719)
        ),
        center = Offset(0.50f, 0.58f),
        radius = 80f
    )

    Box(
        modifier = modifier
            .size(70.dp)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glowColor.copy(alpha = 0.45f * 0.55f),
                            glowColor.copy(alpha = 0.45f * 0.22f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.minDimension * 1f
                    ),
                    radius = size.minDimension * 1f
                )
            }
            .shadow(
                elevation = 6.dp,
                shape = CircleShape,
                ambientColor = glowColor,
                spotColor = glowColor
            )
            .clip(CircleShape)
            .background(capDomeGradient)
            .border(
                width = 1.dp,
                color = Color.Black.copy(alpha = 0.75f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f

            // Knurled grip dashed ring
            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                radius = r - 4.dp.toPx(),
                style = Stroke(
                    width = 3.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 4f), 0f)
                )
            )

            // Neon ring bloom
            val ringRadius = r - 10.dp.toPx()
            drawCircle(
                color = glowColor.copy(alpha = 0.25f),
                radius = ringRadius,
                style = Stroke(width = 4.dp.toPx())
            )
            drawCircle(
                color = glowColor.copy(alpha = 0.70f),
                radius = ringRadius,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // Center Dish with Glyph
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(dishGradient)
                .border(
                    width = 1.dp,
                    color = Color.Black.copy(alpha = 0.85f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = key,
                color = glowColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Exact visual match for ui/components/controller/OrbJoystick.kt
 */
@Preview
@Composable
internal fun StaticOrbJoystick(
    isLeft: Boolean = true,
    modifier: Modifier = Modifier
) {
    val glowColor = if (isLeft) Color(0xFF2FD4B6) else Color(0xFFFF3185)

    val socketGradient = Brush.radialGradient(
        colors = listOf(Color(0xFF030304), Color(0xFF0A0B0C), Color(0xFF1A1C1E)),
        center = Offset(0.5f, 0.5f),
        radius = 280f
    )

    val orbCavityGradient = Brush.radialGradient(
        colors = listOf(Color(0xFF15171A), Color(0xFF060607)),
        center = Offset(0.50f, 0.50f),
        radius = 220f
    )

    Box(
        modifier = modifier
            .size(150.dp)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glowColor.copy(alpha = 0.40f * 0.45f),
                            glowColor.copy(alpha = 0.40f * 0.18f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.minDimension * 0.95f
                    ),
                    radius = size.minDimension * 0.95f
                )
            }
            .shadow(12.dp, CircleShape, ambientColor = glowColor.copy(alpha = 0.5f), spotColor = glowColor)
            .background(socketGradient, shape = CircleShape)
            .border(1.dp, Color.Black.copy(alpha = 0.60f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().clip(CircleShape)) {
            val w = size.width
            val h = size.height
            val r = size.minDimension / 2f
            val center = Offset(w / 2f, h / 2f)

            // Deep top inset shadow: inset 0 7px 14px rgba(0,0,0,0.95)
            drawRect(
                brush = Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.95f), Color.Transparent), 0f, h * 0.38f),
                topLeft = Offset.Zero,
                size = Size(w, h * 0.38f)
            )

            // Baseline drop shadow behind cap: calc(dx * -0.3) calc(9px + dy * -0.3) 14px
            val capR = 50.dp.toPx()
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Black.copy(alpha = 0.80f), Color.Transparent),
                    center = Offset(center.x, center.y + 9.dp.toPx()),
                    radius = capR + 14.dp.toPx()
                ),
                center = Offset(center.x, center.y + 9.dp.toPx()),
                radius = capR + 14.dp.toPx()
            )
        }

        // Spherical Glass Moving Cap (.cap-orb): 100dp
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(orbCavityGradient)
                .border(1.dp, Color.White.copy(alpha = 0.06f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val capW = size.width
                val capH = size.height

                // Top specular arc: inset 0 3px 3px rgba(255,255,255,0.10)
                drawArc(
                    color = Color.White.copy(alpha = 0.12f),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                    size = Size(capW - 2.dp.toPx(), capH - 2.dp.toPx()),
                    style = Stroke(1.2.dp.toPx())
                )

                // Inset bottom shadow: inset 0 -8px 14px rgba(0,0,0,0.85)
                drawRect(
                    brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)), capH * 0.60f, capH),
                    topLeft = Offset(0f, capH * 0.60f),
                    size = Size(capW, capH * 0.40f)
                )
            }

            // Glowing Liquid Core (.orb-core): 60dp with continuous Gaussian blur
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .blur(radius = 5.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                    .clip(CircleShape)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val coreR = size.minDimension / 2f
                    val coreCenter = Offset(size.width / 2f, size.height / 2f)

                    // Multi-stop Gaussian diffuse radial gradient replicating filter: blur(4px)
                    // background: radial-gradient(circle, var(--glow) 0%, transparent 68%) + blur(4px)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0.00f to glowColor.copy(alpha = 0.95f),
                                0.16f to glowColor.copy(alpha = 0.85f),
                                0.32f to glowColor.copy(alpha = 0.68f),
                                0.48f to glowColor.copy(alpha = 0.48f),
                                0.62f to glowColor.copy(alpha = 0.28f),
                                0.74f to glowColor.copy(alpha = 0.14f),
                                0.88f to glowColor.copy(alpha = 0.04f),
                                1.00f to Color.Transparent
                            ),
                            center = coreCenter,
                            radius = coreR
                        ),
                        center = coreCenter,
                        radius = coreR
                    )

                    // Secondary ambient glow diffusion expanding softly past the core perimeter
                    drawCircle(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0.00f to glowColor.copy(alpha = 0.50f),
                                0.40f to glowColor.copy(alpha = 0.28f),
                                0.70f to glowColor.copy(alpha = 0.10f),
                                1.00f to Color.Transparent
                            ),
                            center = coreCenter,
                            radius = coreR * 1.25f
                        ),
                        center = coreCenter,
                        radius = coreR * 1.25f
                    )
                }
            }

            // Inner Orb Containment Ring (.orb-ring) & Specular Reflections (.orb-spec)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val capW = size.width
                val capH = size.height
                val capR = size.minDimension / 2f

                // Inner Containment Ring (.orb-ring): inset 5px
                val orbRingRadius = capR - 5.dp.toPx()
                drawCircle(glowColor.copy(alpha = 0.16f), radius = orbRingRadius, style = Stroke(3.dp.toPx()))
                drawCircle(glowColor.copy(alpha = 0.70f), radius = orbRingRadius, style = Stroke(1.2.dp.toPx()))

                // Primary top-left light catch: ellipse 34% 20% at (34%, 22%)
                drawOval(
                    brush = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.38f), Color.Transparent), Offset(capW * 0.34f, capH * 0.22f), capW * 0.28f),
                    topLeft = Offset(capW * 0.12f, capH * 0.08f),
                    size = Size(capW * 0.44f, capH * 0.28f)
                )

                // Secondary bottom rim bounce light: ellipse 40% 14% at (55%, 94%)
                drawOval(
                    brush = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.10f), Color.Transparent), Offset(capW * 0.55f, capH * 0.94f), capW * 0.25f),
                    topLeft = Offset(capW * 0.31f, capH * 0.84f),
                    size = Size(capW * 0.48f, capH * 0.20f)
                )
            }
        }

        // Top Overlay Canvas: Outer Neon Ring (.lx-ring, z-index: 6) & Glass Lens (.lx-lens, z-index: 8)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val r = size.minDimension / 2f

            // Outer Neon Rim (.lx-ring): single delicate glowing rim at inset 3px (opacity 0.45)
            val outerRingRadius = r - 3.dp.toPx()
            drawCircle(glowColor.copy(alpha = 0.10f), radius = outerRingRadius, style = Stroke(3.5.dp.toPx()))
            drawCircle(glowColor.copy(alpha = 0.38f), radius = outerRingRadius, style = Stroke(1.2.dp.toPx()))

            // Outer Glass Lens (.lx-lens): Top chamfer, left reflection, bottom shadow, specular sheen
            drawArc(Color.White.copy(alpha = 0.12f), 180f, 180f, false, Offset(1.dp.toPx(), 1.dp.toPx()), Size(w - 2.dp.toPx(), h - 2.dp.toPx()), style = Stroke(1.dp.toPx()))
            drawArc(Color.White.copy(alpha = 0.06f), 90f, 90f, false, Offset(1.dp.toPx(), 1.dp.toPx()), Size(w - 2.dp.toPx(), h - 2.dp.toPx()), style = Stroke(1.dp.toPx()))
            drawArc(Color.Black.copy(alpha = 0.30f), 0f, 180f, false, Offset(1.dp.toPx(), 1.dp.toPx()), Size(w - 2.dp.toPx(), h - 2.dp.toPx()), style = Stroke(1.dp.toPx()))
            drawCircle(
                brush = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.06f), Color.Transparent), Offset(w * 0.70f, h * 0.78f), w * 0.20f),
                center = Offset(w * 0.70f, h * 0.78f),
                radius = w * 0.20f
            )
        }
    }
}

/**
 * Exact visual match for ui/components/controller/OrbStickButton (in OrbJoystick.kt)
 */
@Preview
@Composable
internal fun StaticOrbStickButton(
    isLeft: Boolean = true,
    modifier: Modifier = Modifier,
    label: String? = null
) {
    val key = label ?: if (isLeft) "LSB" else "RSB"
    val glowColor = if (isLeft) Color(0xFF2FD4B6) else Color(0xFFFF3185)

    val orbCavityGradient = Brush.radialGradient(
        colors = listOf(Color(0xFF15171A), Color(0xFF060607)),
        center = Offset(0.50f, 0.50f),
        radius = 180f
    )

    Box(
        modifier = modifier
            .size(70.dp)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glowColor.copy(alpha = 0.45f * 0.55f),
                            glowColor.copy(alpha = 0.45f * 0.22f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.minDimension * 1f
                    ),
                    radius = size.minDimension * 1f
                )
            }
            .shadow(
                elevation = 6.dp,
                shape = CircleShape,
                ambientColor = glowColor,
                spotColor = glowColor
            )
            .clip(CircleShape)
            .background(orbCavityGradient)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val r = size.minDimension / 2f
            val center = Offset(w / 2f, h / 2f)

            // Top specular arc
            drawArc(
                color = Color.White.copy(alpha = 0.16f),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                size = Size(w - 2.dp.toPx(), h - 2.dp.toPx()),
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Bottom inset shadow
            val botShadowH = h * 0.40f
            drawRect(
                brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.80f)), h - botShadowH, h),
                topLeft = Offset(0f, h - botShadowH),
                size = Size(w, botShadowH)
            )

            // Liquid Core multi-pass diffuse blur
            val coreR = r * 0.65f
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.00f to glowColor.copy(alpha = 0.95f),
                        0.20f to glowColor.copy(alpha = 0.80f),
                        0.40f to glowColor.copy(alpha = 0.58f),
                        0.60f to glowColor.copy(alpha = 0.32f),
                        0.78f to glowColor.copy(alpha = 0.12f),
                        1.00f to Color.Transparent
                    ),
                    center = center,
                    radius = coreR
                ),
                center = center,
                radius = coreR
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.00f to glowColor.copy(alpha = 0.45f),
                        0.50f to glowColor.copy(alpha = 0.20f),
                        1.00f to Color.Transparent
                    ),
                    center = center,
                    radius = coreR * 1.25f
                ),
                center = center,
                radius = coreR * 1.25f
            )

            // Inner Containment Ring
            val ringRadius = r - 4.dp.toPx()
            drawCircle(glowColor.copy(alpha = 0.25f), radius = ringRadius, style = Stroke(width = 4.dp.toPx()))
            drawCircle(glowColor.copy(alpha = 0.60f), radius = ringRadius, style = Stroke(width = 1.8.dp.toPx()))

            // Specular lens highlight
            drawOval(
                brush = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.35f), Color.Transparent), Offset(w * 0.36f, h * 0.24f), w * 0.28f),
                topLeft = Offset(w * 0.16f, h * 0.10f),
                size = Size(w * 0.40f, h * 0.28f)
            )
        }

        // Center Label
        Text(
            text = key,
            color = glowColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Exact visual match for ui/components/controller/CompassJoystick.kt
 */
@Preview
@Composable
internal fun StaticCompassJoystick(
    isLeft: Boolean = true,
    modifier: Modifier = Modifier
) {
    val glowColor = if (isLeft) Color(0xFF5AA8FF) else Color(0xFFFF5A88)

    val socketGradient = Brush.radialGradient(
        colors = listOf(Color(0xFF030304), Color(0xFF0A0B0C), Color(0xFF1A1C1E)),
        center = Offset(0.5f, 0.5f),
        radius = 280f
    )

    val capDomeGradient = Brush.radialGradient(
        colors = listOf(Color(0xFF34373B), Color(0xFF17181B), Color(0xFF050506)),
        center = Offset(0.50f, 0.38f),
        radius = 200f
    )

    val dishGradient = Brush.radialGradient(
        colors = listOf(Color(0xFF030304), Color(0xFF121314)),
        center = Offset(0.50f, 0.60f),
        radius = 70f
    )

    Box(
        modifier = modifier
            .size(150.dp)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glowColor.copy(alpha = 0.40f * 0.45f),
                            glowColor.copy(alpha = 0.40f * 0.18f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.minDimension * 0.95f
                    ),
                    radius = size.minDimension * 0.95f
                )
            }
            .shadow(12.dp, CircleShape, ambientColor = glowColor.copy(alpha = 0.5f), spotColor = glowColor)
            .background(socketGradient, shape = CircleShape)
            .border(1.dp, Color.Black.copy(alpha = 0.60f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().clip(CircleShape)) {
            val w = size.width
            val h = size.height
            val center = Offset(w / 2f, h / 2f)

            // Deep top inset shadow
            drawRect(
                brush = Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.95f), Color.Transparent), 0f, h * 0.38f),
                topLeft = Offset.Zero,
                size = Size(w, h * 0.38f)
            )

            // Eight Directional Compass Pips (.pips i)
            val pipDist = 62.dp.toPx()
            val pipRadius = 3.dp.toPx()
            for (i in 0 until 8) {
                val angleRad = Math.toRadians((i * 45.0) - 90.0)
                val pipCenter = Offset(
                    center.x + (pipDist * cos(angleRad)).toFloat(),
                    center.y + (pipDist * sin(angleRad)).toFloat()
                )
                drawCircle(
                    color = glowColor.copy(alpha = 0.22f),
                    radius = pipRadius,
                    center = pipCenter
                )
            }

            // Cap baseline shadow
            val capR = 46.dp.toPx()
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Black.copy(alpha = 0.80f), Color.Transparent),
                    center = Offset(center.x, center.y + 8.dp.toPx()),
                    radius = capR + 12.dp.toPx()
                ),
                center = Offset(center.x, center.y + 8.dp.toPx()),
                radius = capR + 12.dp.toPx()
            )
        }

        // Spherical Moving Cap (.cap-compass): 92dp
        Box(
            modifier = Modifier
                .size(92.dp)
                .shadow(10.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.40f), spotColor = Color.Black.copy(alpha = 0.55f))
                .clip(CircleShape)
                .background(capDomeGradient)
                .border(1.dp, Color.Black.copy(alpha = 0.60f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val capW = size.width
                val capH = size.height
                val capR = size.minDimension / 2f

                // Inset top specular arc
                drawArc(
                    color = Color.White.copy(alpha = 0.10f),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(1.5.dp.toPx(), 1.5.dp.toPx()),
                    size = Size(capW - 3.dp.toPx(), capH - 3.dp.toPx()),
                    style = Stroke(width = 1.5.dp.toPx())
                )

                // Inset bottom shadow
                drawRect(
                    brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.80f)), capH * 0.62f, capH),
                    topLeft = Offset(0f, capH * 0.62f),
                    size = Size(capW, capH * 0.38f)
                )

                // Cap Neon Ring (.lx-cap-ring): inset 19px
                val capRingRadius = capR - 19.dp.toPx()
                drawCircle(glowColor.copy(alpha = 0.16f), radius = capRingRadius, style = Stroke(3.dp.toPx()))
                drawCircle(glowColor.copy(alpha = 0.85f), radius = capRingRadius, style = Stroke(1.2.dp.toPx()))

                // Dynamic Cap Specular Lens (.lx-cap-lens)
                val specCenterX = capW * 0.50f
                val specCenterY = capH * 0.20f
                drawOval(
                    brush = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.20f), Color.Transparent), Offset(specCenterX, specCenterY), capW * 0.32f),
                    topLeft = Offset(specCenterX - capW * 0.21f, specCenterY - capH * 0.12f),
                    size = Size(capW * 0.42f, capH * 0.24f)
                )

                // Bottom-right secondary sheen
                drawCircle(
                    brush = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.05f), Color.Transparent), Offset(capW * 0.70f, capH * 0.82f), capW * 0.20f),
                    center = Offset(capW * 0.70f, capH * 0.82f),
                    radius = capW * 0.20f
                )
            }

            // Center Dish (.lx-dish): 38dp
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(dishGradient)
                    .border(1.dp, Color.White.copy(alpha = 0.06f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val dishW = size.width
                    val dishH = size.height

                    // Inset top shadow
                    drawRect(
                        brush = Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.90f), Color.Transparent), 0f, dishH * 0.50f),
                        topLeft = Offset.Zero,
                        size = Size(dishW, dishH * 0.50f)
                    )

                    // Vignette circle
                    drawCircle(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0.00f to Color.Transparent,
                                0.38f to Color.Transparent,
                                1.00f to Color.Black.copy(alpha = 0.85f)
                            ),
                            center = Offset(dishW / 2f, dishH / 2f),
                            radius = dishW / 2f
                        ),
                        center = Offset(dishW / 2f, dishH / 2f),
                        radius = dishW / 2f
                    )
                }

                // Center Glyph (.lx-g): "L" / "R" elegant optical typography inside 38dp dish
                Text(
                    text = if (isLeft) "L" else "R",
                    color = glowColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
            }
        }

        // Top Overlay Canvas: Outer Neon Ring & Glass Lens
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val r = size.minDimension / 2f

            // Outer Neon Ring (.lx-ring): single delicate glowing rim at inset 3px (opacity 0.45)
            val outerRingRadius = r - 3.dp.toPx()
            drawCircle(glowColor.copy(alpha = 0.10f), radius = outerRingRadius, style = Stroke(3.5.dp.toPx()))
            drawCircle(glowColor.copy(alpha = 0.38f), radius = outerRingRadius, style = Stroke(1.2.dp.toPx()))

            // Outer Glass Lens (.lx-lens)
            drawArc(Color.White.copy(alpha = 0.12f), 180f, 180f, false, Offset(1.dp.toPx(), 1.dp.toPx()), Size(w - 2.dp.toPx(), h - 2.dp.toPx()), style = Stroke(1.dp.toPx()))
            drawArc(Color.White.copy(alpha = 0.06f), 90f, 90f, false, Offset(1.dp.toPx(), 1.dp.toPx()), Size(w - 2.dp.toPx(), h - 2.dp.toPx()), style = Stroke(1.dp.toPx()))
            drawArc(Color.Black.copy(alpha = 0.30f), 0f, 180f, false, Offset(1.dp.toPx(), 1.dp.toPx()), Size(w - 2.dp.toPx(), h - 2.dp.toPx()), style = Stroke(1.dp.toPx()))
            drawCircle(
                brush = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.06f), Color.Transparent), Offset(w * 0.70f, h * 0.78f), w * 0.20f),
                center = Offset(w * 0.70f, h * 0.78f),
                radius = w * 0.20f
            )
        }
    }
}

/**
 * Exact visual match for ui/components/controller/CompassStickButton (in CompassJoystick.kt)
 */
@Preview
@Composable
internal fun StaticCompassStickButton(
    isLeft: Boolean = true,
    modifier: Modifier = Modifier,
    label: String? = null
) {
    val key = label ?: if (isLeft) "LSB" else "RSB"
    val glowColor = if (isLeft) Color(0xFF5AA8FF) else Color(0xFFFF5A88)

    val capDomeGradient = Brush.radialGradient(
        colors = listOf(Color(0xFF34373B), Color(0xFF17181B), Color(0xFF050506)),
        center = Offset(0.50f, 0.38f),
        radius = 160f
    )

    val dishGradient = Brush.radialGradient(
        colors = listOf(Color(0xFF030304), Color(0xFF121314)),
        center = Offset(0.50f, 0.60f),
        radius = 60f
    )

    Box(
        modifier = modifier
            .size(70.dp)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glowColor.copy(alpha = 0.45f * 0.55f),
                            glowColor.copy(alpha = 0.45f * 0.22f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.minDimension * 1f
                    ),
                    radius = size.minDimension * 1f
                )
            }
            .shadow(6.dp, CircleShape, ambientColor = glowColor, spotColor = glowColor)
            .clip(CircleShape)
            .background(capDomeGradient)
            .border(1.dp, Color.Black.copy(alpha = 0.60f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val r = size.minDimension / 2f
            val center = Offset(w / 2f, h / 2f)

            // Top specular arc
            drawArc(
                color = Color.White.copy(alpha = 0.14f),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                size = Size(w - 2.dp.toPx(), h - 2.dp.toPx()),
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Bottom inset shadow
            val botShadowH = h * 0.38f
            drawRect(
                brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.80f)), h - botShadowH, h),
                topLeft = Offset(0f, h - botShadowH),
                size = Size(w, botShadowH)
            )

            // 8 Compass Pips around perimeter
            val pipDist = r - 6.dp.toPx()
            val pipRadius = 1.8.dp.toPx()
            for (i in 0 until 8) {
                val angleRad = Math.toRadians((i * 45.0) - 90.0)
                val pipCenter = Offset(
                    center.x + (pipDist * cos(angleRad)).toFloat(),
                    center.y + (pipDist * sin(angleRad)).toFloat()
                )
                drawCircle(
                    color = glowColor.copy(alpha = 0.30f),
                    radius = pipRadius,
                    center = pipCenter
                )
            }

            // Cap Neon Ring: inset 12px
            val ringRadius = r - 12.dp.toPx()
            drawCircle(glowColor.copy(alpha = 0.25f), radius = ringRadius, style = Stroke(width = 4.dp.toPx()))
            drawCircle(glowColor.copy(alpha = 0.70f), radius = ringRadius, style = Stroke(width = 1.8.dp.toPx()))

            // Specular lens highlight
            drawOval(
                brush = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.25f), Color.Transparent), Offset(w * 0.36f, h * 0.24f), w * 0.28f),
                topLeft = Offset(w * 0.16f, h * 0.10f),
                size = Size(w * 0.40f, h * 0.28f)
            )
        }

        // Center Dish with Glyph
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(dishGradient)
                .border(1.dp, Color.White.copy(alpha = 0.06f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = key,
                color = glowColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Exact visual match for ui/components/controller/GyroJoystick.kt
 */
@Preview
@Composable
internal fun StaticGyroJoystick(
    isLeft: Boolean = true,
    modifier: Modifier = Modifier
) {
    val glowColor = if (isLeft) Color(0xFF3FD2FF) else Color(0xFFFF3F85)

    val socketGradient = Brush.radialGradient(
        colors = listOf(Color(0xFF030304), Color(0xFF0A0B0C), Color(0xFF1A1C1E)),
        center = Offset(0.5f, 0.5f),
        radius = 280f
    )

    val puckDomeGradient = Brush.radialGradient(
        colors = listOf(Color(0xFF34373B), Color(0xFF17181B), Color(0xFF050506)),
        center = Offset(0.50f, 0.38f),
        radius = 120f
    )

    Box(
        modifier = modifier
            .size(150.dp)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glowColor.copy(alpha = 0.40f * 0.45f),
                            glowColor.copy(alpha = 0.40f * 0.18f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.minDimension * 0.95f
                    ),
                    radius = size.minDimension * 0.95f
                )
            }
            .shadow(12.dp, CircleShape, ambientColor = glowColor.copy(alpha = 0.5f), spotColor = glowColor)
            .background(socketGradient, shape = CircleShape)
            .border(1.dp, Color.Black.copy(alpha = 0.60f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().clip(CircleShape)) {
            val w = size.width
            val h = size.height
            val center = Offset(w / 2f, h / 2f)

            // Deep top inset shadow
            drawRect(
                brush = Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.95f), Color.Transparent), 0f, h * 0.38f),
                topLeft = Offset.Zero,
                size = Size(w, h * 0.38f)
            )

            // Puck baseline shadow
            val puckR = 28.dp.toPx()
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Black.copy(alpha = 0.80f), Color.Transparent),
                    center = Offset(center.x, center.y + 8.dp.toPx()),
                    radius = puckR + 12.dp.toPx()
                ),
                center = Offset(center.x, center.y + 8.dp.toPx()),
                radius = puckR + 12.dp.toPx()
            )
        }

        // OUTER GIMBAL RING (.gim.o): 120dp
        Box(modifier = Modifier.size(120.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val r = size.minDimension / 2f - 2.dp.toPx()
                // Soft atmospheric haze
                drawCircle(glowColor.copy(alpha = 0.06f), radius = r, style = Stroke(width = 6.dp.toPx()))
                // Diffuse glow halo
                drawCircle(glowColor.copy(alpha = 0.20f), radius = r, style = Stroke(width = 3.dp.toPx()))
                // Crisp core filament
                drawCircle(glowColor.copy(alpha = 0.90f), radius = r, style = Stroke(width = 1.2.dp.toPx()))
            }
        }

        // INNER GIMBAL RING (.gim.i): 92dp
        Box(modifier = Modifier.size(92.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val r = size.minDimension / 2f - 2.dp.toPx()
                // Soft atmospheric haze
                drawCircle(glowColor.copy(alpha = 0.06f), radius = r, style = Stroke(width = 6.dp.toPx()))
                // Diffuse glow halo
                drawCircle(glowColor.copy(alpha = 0.20f), radius = r, style = Stroke(width = 3.dp.toPx()))
                // Crisp core filament
                drawCircle(glowColor.copy(alpha = 0.90f), radius = r, style = Stroke(width = 1.2.dp.toPx()))
            }
        }

        // COMPACT CENTER PUCK (.cap-puck .cap-gyro): 56dp
        Box(
            modifier = Modifier
                .size(56.dp)
                .shadow(8.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.40f), spotColor = Color.Black.copy(alpha = 0.55f))
                .clip(CircleShape)
                .background(puckDomeGradient)
                .border(1.dp, Color.Black.copy(alpha = 0.60f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val puckW = size.width
                val puckH = size.height
                val puckR = size.minDimension / 2f
                val puckCenter = Offset(puckW / 2f, puckH / 2f)

                // Top specular rim
                drawArc(
                    color = Color.White.copy(alpha = 0.12f),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                    size = Size(puckW - 2.dp.toPx(), puckH - 2.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx())
                )

                // Inset bottom shadow
                drawRect(
                    brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.80f)), puckH * 0.65f, puckH),
                    topLeft = Offset(0f, puckH * 0.65f),
                    size = Size(puckW, puckH * 0.35f)
                )

                // Puck Neon Ring (.puck-ring): inset 4px
                val ringRadius = puckR - 4.dp.toPx()
                drawCircle(glowColor.copy(alpha = 0.16f), radius = ringRadius, style = Stroke(width = 3.dp.toPx()))
                drawCircle(glowColor.copy(alpha = 0.85f), radius = ringRadius, style = Stroke(width = 1.2.dp.toPx()))

                // Central Luminous Core Dot (.puck-dot): 9dp
                val dotRadius = 4.5.dp.toPx()
                drawCircle(
                    brush = Brush.radialGradient(listOf(glowColor.copy(alpha = 0.85f), glowColor.copy(alpha = 0.35f), Color.Transparent), puckCenter, dotRadius + 6.dp.toPx()),
                    center = puckCenter,
                    radius = dotRadius + 6.dp.toPx()
                )
                drawCircle(glowColor, dotRadius, puckCenter)

                // Dynamic Cap Specular Lens (.puck-lens)
                drawOval(
                    brush = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.22f), Color.Transparent), Offset(puckW * 0.50f, puckH * 0.20f), puckW * 0.30f),
                    topLeft = Offset(puckW * 0.29f, puckH * 0.08f),
                    size = Size(puckW * 0.42f, puckH * 0.24f)
                )
            }
        }

        // Top Glass Lens Overlay (.lx-lens & delicate .lx-ring)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val r = size.minDimension / 2f

            // Outer Neon Rim (.lx-ring): single delicate glowing rim at inset 3px (opacity 0.45)
            val outerRingRadius = r - 3.dp.toPx()
            drawCircle(glowColor.copy(alpha = 0.10f), radius = outerRingRadius, style = Stroke(3.5.dp.toPx()))
            drawCircle(glowColor.copy(alpha = 0.38f), radius = outerRingRadius, style = Stroke(1.2.dp.toPx()))

            // Outer Glass Lens (.lx-lens)
            drawArc(Color.White.copy(alpha = 0.12f), 180f, 180f, false, Offset(1.dp.toPx(), 1.dp.toPx()), Size(w - 2.dp.toPx(), h - 2.dp.toPx()), style = Stroke(1.dp.toPx()))
            drawArc(Color.White.copy(alpha = 0.06f), 90f, 90f, false, Offset(1.dp.toPx(), 1.dp.toPx()), Size(w - 2.dp.toPx(), h - 2.dp.toPx()), style = Stroke(1.dp.toPx()))
            drawArc(Color.Black.copy(alpha = 0.30f), 0f, 180f, false, Offset(1.dp.toPx(), 1.dp.toPx()), Size(w - 2.dp.toPx(), h - 2.dp.toPx()), style = Stroke(1.dp.toPx()))
            drawCircle(
                brush = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.06f), Color.Transparent), Offset(w * 0.70f, h * 0.78f), w * 0.20f),
                center = Offset(w * 0.70f, h * 0.78f),
                radius = w * 0.20f
            )
        }
    }
}

/**
 * Exact visual match for ui/components/controller/GyroStickButton (in GyroJoystick.kt)
 */
@Preview
@Composable
internal fun StaticGyroStickButton(
    isLeft: Boolean = true,
    modifier: Modifier = Modifier,
    label: String? = null
) {
    val key = label ?: if (isLeft) "LSB" else "RSB"
    val glowColor = if (isLeft) Color(0xFF3FD2FF) else Color(0xFFFF3F85)

    val puckDomeGradient = Brush.radialGradient(
        colors = listOf(Color(0xFF34373B), Color(0xFF17181B), Color(0xFF050506)),
        center = Offset(0.50f, 0.38f),
        radius = 140f
    )

    Box(
        modifier = modifier
            .size(70.dp)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glowColor.copy(alpha = 0.45f * 0.55f),
                            glowColor.copy(alpha = 0.45f * 0.22f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.minDimension * 1f
                    ),
                    radius = size.minDimension * 1f
                )
            }
            .shadow(6.dp, CircleShape, ambientColor = glowColor, spotColor = glowColor)
            .clip(CircleShape)
            .background(puckDomeGradient)
            .border(1.dp, Color.Black.copy(alpha = 0.60f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val r = size.minDimension / 2f
            val center = Offset(w / 2f, h / 2f)

            // Top specular arc
            drawArc(
                color = Color.White.copy(alpha = 0.14f),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                size = Size(w - 2.dp.toPx(), h - 2.dp.toPx()),
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Bottom inset shadow
            val botShadowH = h * 0.38f
            drawRect(
                brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.80f)), h - botShadowH, h),
                topLeft = Offset(0f, h - botShadowH),
                size = Size(w, botShadowH)
            )

            // Outer Concentric Gimbal Ring
            val outerGimbalR = r - 5.dp.toPx()
            drawCircle(glowColor.copy(alpha = 0.20f), radius = outerGimbalR, style = Stroke(width = 4.dp.toPx()))
            drawCircle(glowColor.copy(alpha = 0.65f), radius = outerGimbalR, style = Stroke(width = 1.8.dp.toPx()))

            // Inner Concentric Gimbal Ring
            val innerGimbalR = r - 13.dp.toPx()
            drawCircle(glowColor.copy(alpha = 0.18f), radius = innerGimbalR, style = Stroke(width = 3.dp.toPx()))
            drawCircle(glowColor.copy(alpha = 0.50f), radius = innerGimbalR, style = Stroke(width = 1.5.dp.toPx()))

            // Specular lens highlight
            drawOval(
                brush = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.25f), Color.Transparent), Offset(w * 0.36f, h * 0.24f), w * 0.28f),
                topLeft = Offset(w * 0.16f, h * 0.10f),
                size = Size(w * 0.40f, h * 0.28f)
            )
        }

        // Center Puck with Label
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF030304), Color(0xFF121314)),
                        center = Offset(0.50f, 0.60f),
                        radius = 60f
                    )
                )
                .border(1.dp, glowColor.copy(alpha = 0.60f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = key,
                color = glowColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Exact visual match for ui/components/controller/SpotlightJoystick.kt
 */
@Preview
@Composable
internal fun StaticSpotlightJoystick(
    isLeft: Boolean = true,
    modifier: Modifier = Modifier
) {
    val glowColor = if (isLeft) Color(0xFFFFD23F) else Color(0xFFFF3F85)

    val socketGradient = Brush.radialGradient(
        colors = listOf(Color(0xFF030304), Color(0xFF0A0B0C), Color(0xFF1A1C1E)),
        center = Offset(0.5f, 0.5f),
        radius = 280f
    )

    val puckDomeGradient = Brush.radialGradient(
        colors = listOf(Color(0xFF34373B), Color(0xFF17181B), Color(0xFF050506)),
        center = Offset(0.50f, 0.38f),
        radius = 160f
    )

    Box(
        modifier = modifier
            .size(150.dp)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glowColor.copy(alpha = 0.40f * 0.45f),
                            glowColor.copy(alpha = 0.40f * 0.18f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.minDimension * 0.95f
                    ),
                    radius = size.minDimension * 0.95f
                )
            }
            .shadow(12.dp, CircleShape, ambientColor = glowColor.copy(alpha = 0.5f), spotColor = glowColor)
            .background(socketGradient, shape = CircleShape)
            .border(1.dp, Color.Black.copy(alpha = 0.60f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().clip(CircleShape)) {
            val w = size.width
            val h = size.height
            val r = size.minDimension / 2f
            val center = Offset(w / 2f, h / 2f)

            // Deep top inset shadow
            drawRect(
                brush = Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.95f), Color.Transparent), 0f, h * 0.38f),
                topLeft = Offset.Zero,
                size = Size(w, h * 0.38f)
            )

            // 1. Moving Light Pool (.lamp-pool): 62dp centered
            val poolRadius = 62.dp.toPx()
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glowColor.copy(alpha = 0.35f), glowColor.copy(alpha = 0.15f), Color.Transparent),
                    center = center,
                    radius = poolRadius
                ),
                center = center,
                radius = poolRadius
            )

            // 2. Hidden Floor Dot Matrix (.lamp-floor): 14dp grid within 56dp spotlight circle
            val dotSpacingPx = 14.dp.toPx()
            val maskRadiusPx = 56.dp.toPx()
            val maxSocketDistPx = r - 5.dp.toPx()
            val minGrid = (-maskRadiusPx / dotSpacingPx).toInt() - 1
            val maxGrid = (maskRadiusPx / dotSpacingPx).toInt() + 1

            for (gx in minGrid..maxGrid) {
                for (gy in minGrid..maxGrid) {
                    val dotX = center.x + gx * dotSpacingPx
                    val dotY = center.y + gy * dotSpacingPx
                    val distFromSocket = hypot(dotX - center.x, dotY - center.y)
                    if (distFromSocket > maxSocketDistPx) continue

                    val dist = hypot(dotX - center.x, dotY - center.y)
                    if (dist < maskRadiusPx) {
                        val factor = (1f - dist / maskRadiusPx).coerceIn(0f, 1f)
                        drawCircle(
                            color = glowColor.copy(alpha = factor * 0.90f),
                            radius = 1.3.dp.toPx(),
                            center = Offset(dotX, dotY)
                        )
                        if (factor > 0.35f) {
                            drawCircle(
                                color = glowColor.copy(alpha = factor * 0.40f),
                                radius = 2.4.dp.toPx(),
                                center = Offset(dotX, dotY)
                            )
                        }
                    }
                }
            }

            // Puck baseline drop shadow
            val puckR = 38.dp.toPx()
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Black.copy(alpha = 0.80f), Color.Transparent),
                    center = Offset(center.x, center.y + 8.dp.toPx()),
                    radius = puckR + 12.dp.toPx()
                ),
                center = Offset(center.x, center.y + 8.dp.toPx()),
                radius = puckR + 12.dp.toPx()
            )
        }

        // 76dp CENTER PUCK CAP (.cap-puck)
        Box(
            modifier = Modifier
                .size(76.dp)
                .shadow(10.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.40f), spotColor = Color.Black.copy(alpha = 0.55f))
                .clip(CircleShape)
                .background(puckDomeGradient)
                .border(1.dp, Color.Black.copy(alpha = 0.60f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val puckW = size.width
                val puckH = size.height
                val puckR = size.minDimension / 2f
                val puckCenter = Offset(puckW / 2f, puckH / 2f)

                // Top specular rim
                drawArc(
                    color = Color.White.copy(alpha = 0.12f),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                    size = Size(puckW - 2.dp.toPx(), puckH - 2.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx())
                )

                // Inset bottom shadow
                val botShadowH = puckH * 0.35f
                drawRect(
                    brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.80f)), puckH - botShadowH, puckH),
                    topLeft = Offset(0f, puckH - botShadowH),
                    size = Size(puckW, botShadowH)
                )

                // Puck Neon Containment Ring (.puck-ring): inset 5px
                val ringRadius = puckR - 5.dp.toPx()
                drawCircle(glowColor.copy(alpha = 0.16f), radius = ringRadius, style = Stroke(width = 3.dp.toPx()))
                drawCircle(glowColor.copy(alpha = 0.85f), radius = ringRadius, style = Stroke(width = 1.2.dp.toPx()))

                // Central Luminous Core Dot (.puck-dot): 9dp
                val dotRadius = 4.5.dp.toPx()
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glowColor.copy(alpha = 0.85f), glowColor.copy(alpha = 0.35f), Color.Transparent),
                        center = puckCenter,
                        radius = dotRadius + 6.dp.toPx()
                    ),
                    center = puckCenter,
                    radius = dotRadius + 6.dp.toPx()
                )
                drawCircle(color = glowColor, center = puckCenter, radius = dotRadius)

                // Static Specular Lens (.puck-lens)
                val specCenterX = puckW * 0.50f
                val specCenterY = puckH * 0.20f
                drawOval(
                    brush = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.22f), Color.Transparent), Offset(specCenterX, specCenterY), puckW * 0.30f),
                    topLeft = Offset(specCenterX - puckW * 0.21f, specCenterY - puckH * 0.12f),
                    size = Size(puckW * 0.42f, puckH * 0.24f)
                )
            }
        }

        // Top Glass Lens Overlay (.lx-lens & delicate .lx-ring)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val r = size.minDimension / 2f

            // Outer Neon Rim (.lx-ring): single delicate glowing rim at inset 3px (opacity 0.45)
            val outerRingRadius = r - 3.dp.toPx()
            drawCircle(glowColor.copy(alpha = 0.10f), radius = outerRingRadius, style = Stroke(3.5.dp.toPx()))
            drawCircle(glowColor.copy(alpha = 0.38f), radius = outerRingRadius, style = Stroke(1.2.dp.toPx()))

            // Outer Glass Lens (.lx-lens)
            drawArc(Color.White.copy(alpha = 0.12f), 180f, 180f, false, Offset(1.dp.toPx(), 1.dp.toPx()), Size(w - 2.dp.toPx(), h - 2.dp.toPx()), style = Stroke(1.dp.toPx()))
            drawArc(Color.White.copy(alpha = 0.06f), 90f, 90f, false, Offset(1.dp.toPx(), 1.dp.toPx()), Size(w - 2.dp.toPx(), h - 2.dp.toPx()), style = Stroke(1.dp.toPx()))
            drawArc(Color.Black.copy(alpha = 0.30f), 0f, 180f, false, Offset(1.dp.toPx(), 1.dp.toPx()), Size(w - 2.dp.toPx(), h - 2.dp.toPx()), style = Stroke(1.dp.toPx()))
            drawCircle(
                brush = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.06f), Color.Transparent), Offset(w * 0.70f, h * 0.78f), w * 0.20f),
                center = Offset(w * 0.70f, h * 0.78f),
                radius = w * 0.20f
            )
        }
    }
}

/**
 * Exact visual match for ui/components/controller/SpotlightStickButton (in SpotlightJoystick.kt)
 */
@Preview
@Composable
internal fun StaticSpotlightStickButton(
    isLeft: Boolean = true,
    modifier: Modifier = Modifier,
    label: String? = null
) {
    val key = label ?: if (isLeft) "LSB" else "RSB"
    val glowColor = if (isLeft) Color(0xFFFFD23F) else Color(0xFFFF3F85)

    val puckDomeGradient = Brush.radialGradient(
        colors = listOf(Color(0xFF34373B), Color(0xFF17181B), Color(0xFF050506)),
        center = Offset(0.50f, 0.38f),
        radius = 140f
    )

    Box(
        modifier = modifier
            .size(70.dp)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glowColor.copy(alpha = 0.45f * 0.55f),
                            glowColor.copy(alpha = 0.45f * 0.22f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.minDimension * 1f
                    ),
                    radius = size.minDimension * 1f
                )
            }
            .shadow(6.dp, CircleShape, ambientColor = glowColor, spotColor = glowColor)
            .clip(CircleShape)
            .background(puckDomeGradient)
            .border(1.dp, Color.Black.copy(alpha = 0.60f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val r = size.minDimension / 2f
            val center = Offset(w / 2f, h / 2f)

            // Top specular arc
            drawArc(
                color = Color.White.copy(alpha = 0.14f),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                size = Size(w - 2.dp.toPx(), h - 2.dp.toPx()),
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Bottom inset shadow
            val botShadowH = h * 0.38f
            drawRect(
                brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.80f)), h - botShadowH, h),
                topLeft = Offset(0f, h - botShadowH),
                size = Size(w, botShadowH)
            )

            // Centered Spotlight Light Pool: 32dp
            val poolRadius = 32.dp.toPx()
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glowColor.copy(alpha = 0.30f), glowColor.copy(alpha = 0.12f), Color.Transparent),
                    center = center,
                    radius = poolRadius
                ),
                center = center,
                radius = poolRadius
            )

            // Floor Matrix Dots
            val dotSpacingPx = 10.dp.toPx()
            val maskRadiusPx = 28.dp.toPx()
            val minGrid = (-maskRadiusPx / dotSpacingPx).toInt() - 1
            val maxGrid = (maskRadiusPx / dotSpacingPx).toInt() + 1

            for (gx in minGrid..maxGrid) {
                for (gy in minGrid..maxGrid) {
                    val dotX = center.x + gx * dotSpacingPx
                    val dotY = center.y + gy * dotSpacingPx
                    val dist = hypot(dotX - center.x, dotY - center.y)
                    if (dist < maskRadiusPx) {
                        val factor = (1f - dist / maskRadiusPx).coerceIn(0f, 1f)
                        drawCircle(
                            color = glowColor.copy(alpha = factor * 0.80f),
                            radius = 1.1.dp.toPx(),
                            center = Offset(dotX, dotY)
                        )
                    }
                }
            }

            // Outer Neon Containment Ring
            val outerRingR = r - 5.dp.toPx()
            drawCircle(glowColor.copy(alpha = 0.20f), radius = outerRingR, style = Stroke(width = 4.dp.toPx()))
            drawCircle(glowColor.copy(alpha = 0.70f), radius = outerRingR, style = Stroke(width = 1.8.dp.toPx()))

            // Specular lens highlight
            drawOval(
                brush = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.25f), Color.Transparent), Offset(w * 0.36f, h * 0.24f), w * 0.28f),
                topLeft = Offset(w * 0.16f, h * 0.10f),
                size = Size(w * 0.40f, h * 0.28f)
            )
        }

        // Center Puck with Label
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF030304), Color(0xFF121314)),
                        center = Offset(0.50f, 0.60f),
                        radius = 60f
                    )
                )
                .border(1.dp, glowColor.copy(alpha = 0.60f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = key,
                color = glowColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}



/**
 * Exact visual match for ui/components/controller/RealisticStickButton (in RealisticJoystick.kt)
 */
@Composable
private fun StaticRealisticStickButton(
    isLeft: Boolean,
    modifier: Modifier = Modifier,
    label: String? = null
) {
    val key = label ?: if (isLeft) "LSB" else "RSB"
    val accentColor = if (isLeft) Color.Cyan else Color(0xFFFF007F)

    val baseGradient = Brush.radialGradient(
        colors = listOf(Color(0xFF333333), Color(0xFF141414)),
        center = Offset(0.35f, 0.35f),
        radius = 160f
    )

    val dishGradient = Brush.radialGradient(
        colors = listOf(Color(0xFF252525), Color(0xFF121212)),
        center = Offset(0.5f, 0.5f),
        radius = 90f
    )

    val shadow = Modifier
        .drawBehind {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.45f * 0.55f),
                        accentColor.copy(alpha = 0.45f * 0.22f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.minDimension * 0.95f
                ),
                radius = size.minDimension * 0.95f
            )
        }
        .shadow(
            elevation = 4.dp,
            shape = CircleShape,
            ambientColor = accentColor,
            spotColor = accentColor
        )

    Box(
        modifier = modifier
            .size(70.dp)
            .then(shadow)
            .clip(CircleShape)
            .background(baseGradient),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val minDim = size.minDimension
            // Outer bevel rim
            drawCircle(
                color = Color.White.copy(alpha = 0.35f),
                radius = minDim / 2f - 2f,
                style = Stroke(width = 3f)
            )

            // Dashed knurled grip ring
            drawCircle(
                color = Color.Black.copy(alpha = 0.6f),
                radius = minDim / 2f - 6f,
                style = Stroke(
                    width = 3.5f,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                )
            )

            // Inner concave dish ring
            drawCircle(
                color = accentColor.copy(alpha = 0.35f),
                radius = minDim / 2f - 12f,
                style = Stroke(width = 2f)
            )
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(dishGradient),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = key,
                color = accentColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
        }
    }
}

/**
 * Exact visual match for ui/components/controller/RealisticTouchPad.kt
 */
@Composable
private fun StaticRealisticTouchPad(
    isLeft: Boolean,
    modifier: Modifier = Modifier
) {
    val accentColor = if (isLeft) Color(0xFF00E5FF) else Color(0xFFFF007F)
    val shape = RoundedCornerShape(26.dp)
    val innerShape = RoundedCornerShape(18.dp)

    val surfaceGradient = Brush.radialGradient(
        colors = listOf(Color(0xFF23252B), Color(0xFF131418), Color(0xFF0B0C0E)),
        center = Offset(0.4f, 0.4f),
        radius = 280f
    )

    Box(
        modifier = modifier
            .size(180.dp)
            .drawBehind {
                val pad = 12.dp.toPx()
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.40f * 0.45f),
                            accentColor.copy(alpha = 0.40f * 0.18f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.width * 0.70f
                    ),
                    topLeft = Offset(-pad, -pad),
                    size = Size(size.width + pad * 2, size.height + pad * 2),
                    cornerRadius = CornerRadius(26.dp.toPx() + pad, 26.dp.toPx() + pad)
                )
            }
            .shadow(6.dp, shape, ambientColor = accentColor, spotColor = accentColor)
            .clip(shape)
            .background(surfaceGradient)
            .border(BorderStroke(2.dp, Color(0xFF353C4A)), shape)
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        // Flat stationary trackpad surface
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(innerShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.03f), Color.Transparent),
                        radius = 200f
                    )
                )
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.07f)), innerShape)
        )

        Text(
            text = if (isLeft) "TOUCH MOVE • LTP" else "TOUCH LOOK • RTP",
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = accentColor.copy(alpha = 0.65f),
            letterSpacing = 1.2.sp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 4.dp)
        )

        Text(
            text = "2.0X BALLISTICS",
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Normal,
            color = Color.White.copy(alpha = 0.35f),
            letterSpacing = 0.8.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 4.dp)
        )
    }
}

/**
 * Exact visual match for ui/components/controller/RealisticDPad.kt
 */
@Composable
private fun StaticRealisticDPad(
    modifier: Modifier = Modifier
) {
    val gradient = Brush.linearGradient(
        colors = listOf(Color(0xFF2C2C2C), Color(0xFF141414)),
        start = Offset(0f, 0f),
        end = Offset(200f, 200f)
    )
    val neonColor = Color(0xFF00F0FF)
    val shadow = Modifier
        .drawBehind {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        neonColor.copy(alpha = 0.45f * 0.50f),
                        neonColor.copy(alpha = 0.45f * 0.20f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.minDimension * 0.95f
                ),
                radius = size.minDimension * 0.95f
            )
        }
        .shadow(8.dp, crossShape, ambientColor = neonColor, spotColor = neonColor)

    Box(
        modifier = modifier
            .size(140.dp)
            .then(shadow)
            .clip(crossShape)
            .background(gradient),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val armW = w / 3f
            val armH = h / 3f

            // Cross indentations / bevel grooves
            drawLine(
                color = Color.Black.copy(alpha = 0.5f),
                start = Offset(w / 2f, 0f),
                end = Offset(w / 2f, h),
                strokeWidth = 3f
            )
            drawLine(
                color = Color.Black.copy(alpha = 0.5f),
                start = Offset(0f, h / 2f),
                end = Offset(w, h / 2f),
                strokeWidth = 3f
            )

            // Directional Triangle Arrows
            val arrowColor = Color.White.copy(alpha = 0.70f)
            val arrowSize = 6.dp.toPx()

            // UP Arrow
            val upPath = Path().apply {
                moveTo(w / 2f, armH * 0.45f - arrowSize)
                lineTo(w / 2f + arrowSize, armH * 0.45f + arrowSize)
                lineTo(w / 2f - arrowSize, armH * 0.45f + arrowSize)
                close()
            }
            drawPath(path = upPath, color = arrowColor)

            // DOWN Arrow
            val downPath = Path().apply {
                moveTo(w / 2f, h - armH * 0.45f + arrowSize)
                lineTo(w / 2f + arrowSize, h - armH * 0.45f - arrowSize)
                lineTo(w / 2f - arrowSize, h - armH * 0.45f - arrowSize)
                close()
            }
            drawPath(path = downPath, color = arrowColor)

            // LEFT Arrow
            val leftPath = Path().apply {
                moveTo(armW * 0.45f - arrowSize, h / 2f)
                lineTo(armW * 0.45f + arrowSize, h / 2f - arrowSize)
                lineTo(armW * 0.45f + arrowSize, h / 2f + arrowSize)
                close()
            }
            drawPath(path = leftPath, color = arrowColor)

            // RIGHT Arrow
            val rightPath = Path().apply {
                moveTo(w - armW * 0.45f + arrowSize, h / 2f)
                lineTo(w - armW * 0.45f - arrowSize, h / 2f - arrowSize)
                lineTo(w - armW * 0.45f - arrowSize, h / 2f + arrowSize)
                close()
            }
            drawPath(path = rightPath, color = arrowColor)

            // Center concave disc
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF1F1F1F), Color(0xFF0D0D0D)),
                    center = Offset(w / 2f, h / 2f),
                    radius = 50f
                ),
                radius = armW * 0.45f,
                center = Offset(w / 2f, h / 2f)
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.6f),
                radius = armW * 0.45f,
                center = Offset(w / 2f, h / 2f),
                style = Stroke(width = 2f)
            )
        }
    }
}

/**
 * Exact visual match for ui/components/controller/RealisticDPadButton (in RealisticDPad.kt)
 */
@Composable
private fun StaticRealisticDPadButton(
    direction: String,
    modifier: Modifier = Modifier
) {
    val K = com.sanket.tools.nexpad.model.NexpadKeys
    val dirSymbol = when (direction.uppercase()) {
        K.UP    -> "▲"
        K.DOWN  -> "▼"
        K.LEFT  -> "◀"
        K.RIGHT -> "▶"
        else    -> direction
    }

    val themeColor = Color(0xFF00F0FF)
    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .size(72.dp)
            .drawBehind {
                val pad = 10.dp.toPx()
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            themeColor.copy(alpha = 0.45f * 0.50f),
                            themeColor.copy(alpha = 0.45f * 0.20f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.minDimension * 0.95f
                    ),
                    topLeft = Offset(-pad, -pad),
                    size = Size(size.width + pad * 2, size.height + pad * 2),
                    cornerRadius = CornerRadius(16.dp.toPx() + pad, 16.dp.toPx() + pad)
                )
            }
            .shadow(4.dp, shape, ambientColor = themeColor, spotColor = themeColor)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF262626), Color(0xFF141414))
                )
            )
            .border(2.dp, themeColor.copy(alpha = 0.85f), shape),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = dirSymbol,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = themeColor
            )
            Text(
                text = direction.uppercase(),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color.LightGray.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * Exact visual match for ui/components/controller/LensDPad.kt
 */
@Preview
@Composable
internal fun StaticLensDPad(
    isRgbEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val glowColor = if (isRgbEnabled) Color(0xFF00E5FF) else Color(0xFFD8DEE9)

    val socketGradient = Brush.radialGradient(
        colors = listOf(
            Color(0xFF232527),
            Color(0xFF0C0D0E),
            Color(0xFF000000)
        ),
        center = Offset(0.50f, 0.55f),
        radius = 320f
    )

    val crossBodyGradient = Brush.radialGradient(
        colors = listOf(
            Color(0xFF26282B),
            Color(0xFF0C0D0E),
            Color(0xFF000000)
        ),
        center = Offset(0.50f, 0.53f),
        radius = 180f
    )

    val glossGradient = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.10f),
            Color.White.copy(alpha = 0.00f),
            Color.Black.copy(alpha = 0.28f)
        )
    )

    val hubGradient = Brush.radialGradient(
        colors = listOf(
            Color(0xFF030304),
            Color(0xFF151617)
        ),
        center = Offset(0.50f, 0.60f),
        radius = 40f
    )

    Box(
        modifier = modifier
            .size(160.dp)
            .drawBehind {
                if (isRgbEnabled) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                glowColor.copy(alpha = 0.40f * 0.45f),
                                glowColor.copy(alpha = 0.40f * 0.18f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = size.minDimension * 0.95f
                        ),
                        radius = size.minDimension * 0.95f
                    )
                }
            }
            .shadow(12.dp, CircleShape, ambientColor = if (isRgbEnabled) glowColor else Color.Black, spotColor = if (isRgbEnabled) glowColor else Color.Black)
            .clip(CircleShape)
            .background(socketGradient)
            .border(1.dp, Color.Black.copy(alpha = 0.50f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // Socket Inset Bottom Shadow & Inset Neon Ring
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val insetH = h * 0.35f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.70f)),
                    startY = h - insetH,
                    endY = h
                ),
                topLeft = Offset(0f, h - insetH),
                size = Size(w, insetH)
            )

            val r = size.minDimension / 2f
            val ringRadius = r - 3.dp.toPx()
            drawCircle(glowColor.copy(alpha = 0.12f), radius = ringRadius, style = Stroke(width = 5.dp.toPx()))
            drawCircle(glowColor.copy(alpha = 0.35f), radius = ringRadius, style = Stroke(width = 2.dp.toPx()))
        }

        // Cross Box (150dp)
        Box(
            modifier = Modifier.size(150.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val crossPath = com.sanket.tools.nexpad.ui.components.controller.createContouredCrossPath(size, insetDp = 0f)
                val insetNeonPath = com.sanket.tools.nexpad.ui.components.controller.createContouredCrossPath(size, insetDp = 3.6f)
                val scale = min(size.width, size.height) / 150f

                // 1. Soft Multi-pass Drop Shadow (translate 0 5)
                for (step in 0..4) {
                    val offY = (2.5f + step * 0.9f) * scale
                    val shadowPath = Path().apply {
                        addPath(com.sanket.tools.nexpad.ui.components.controller.createContouredCrossPath(size, insetDp = step * 0.4f), Offset(0f, offY))
                    }
                    drawPath(path = shadowPath, color = Color.Black.copy(alpha = 0.14f))
                }

                // 2. Cross Body
                drawPath(path = crossPath, brush = crossBodyGradient)
                drawPath(path = crossPath, color = Color.Black.copy(alpha = 0.50f), style = Stroke(width = 1.dp.toPx()))

                // 3. Inset Neon Ribbon Contour
                drawPath(path = insetNeonPath, color = glowColor.copy(alpha = 0.22f), style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawPath(path = insetNeonPath, color = glowColor.copy(alpha = 0.85f), style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

                // 4. Gloss Sheen
                drawPath(path = crossPath, brush = glossGradient)

                // 5. Directional Chevrons
                fun sx(x: Float) = x * scale
                fun sy(y: Float) = y * scale
                val strokeW = 4.dp.toPx()

                // UP Chevron
                val upChevron = Path().apply {
                    moveTo(sx(64f), sy(36f))
                    lineTo(sx(75f), sy(25f))
                    lineTo(sx(86f), sy(36f))
                }
                drawPath(upChevron, glowColor.copy(alpha = 0.85f), style = Stroke(strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))

                // DOWN Chevron
                val downChevron = Path().apply {
                    moveTo(sx(64f), sy(114f))
                    lineTo(sx(75f), sy(125f))
                    lineTo(sx(86f), sy(114f))
                }
                drawPath(downChevron, glowColor.copy(alpha = 0.85f), style = Stroke(strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))

                // LEFT Chevron
                val leftChevron = Path().apply {
                    moveTo(sx(36f), sy(64f))
                    lineTo(sx(25f), sy(75f))
                    lineTo(sx(36f), sy(86f))
                }
                drawPath(leftChevron, glowColor.copy(alpha = 0.85f), style = Stroke(strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))

                // RIGHT Chevron
                val rightChevron = Path().apply {
                    moveTo(sx(114f), sy(64f))
                    lineTo(sx(125f), sy(75f))
                    lineTo(sx(114f), sy(86f))
                }
                drawPath(rightChevron, glowColor.copy(alpha = 0.85f), style = Stroke(strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))

                // 6. Center Hub Dish & Pip
                val hubCenter = Offset(sx(75f), sy(75f))
                val hubRadius = sx(12f)
                val dotRadius = sx(3f)
                drawCircle(brush = hubGradient, radius = hubRadius, center = hubCenter)
                drawCircle(color = glowColor.copy(alpha = 0.35f), radius = hubRadius, center = hubCenter, style = Stroke(width = 1.dp.toPx()))
                drawCircle(color = glowColor.copy(alpha = 0.70f), radius = dotRadius, center = hubCenter)
            }
        }

        // Top Glass Lens Overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawArc(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.04f), Color.Transparent),
                    startY = 0f,
                    endY = h * 0.40f
                ),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                size = Size(w - 2.dp.toPx(), h - 2.dp.toPx()),
                style = Stroke(width = 1.2.dp.toPx())
            )
            drawArc(Color.Black.copy(alpha = 0.30f), 0f, 180f, false, Offset(1.dp.toPx(), 1.dp.toPx()), Size(w - 2.dp.toPx(), h - 2.dp.toPx()), style = Stroke(1.dp.toPx()))
            drawCircle(
                brush = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.06f), Color.Transparent), Offset(w * 0.70f, h * 0.78f), w * 0.22f),
                center = Offset(w * 0.70f, h * 0.78f),
                radius = w * 0.22f
            )
        }
    }
}

/**
 * Exact visual match for ui/components/controller/FourLensesDPad.kt (Four Lenses D-Pad .dp-len)
 */
@Preview
@Composable
internal fun StaticFourLensesDPad(
    isRgbEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val glowColor = if (isRgbEnabled) Color(0xFF00E5FF) else Color(0xFFD8DEE9)

    val hubGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF030304),
                Color(0xFF151617)
            ),
            center = Offset(0.50f, 0.60f),
            radius = 60f
        )
    }

    Box(
        modifier = modifier.size(164.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer Ambient Halo
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, glowColor.copy(alpha = 0.16f), CircleShape)
        )

        // Stage containing central hub and 4 discrete keys
        Box(modifier = Modifier.fillMaxSize()) {
            // Central Hub (40dp at 62dp, 62dp)
            Box(
                modifier = Modifier
                    .offset(62.dp, 62.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(hubGradient)
                    .border(1.dp, Color.Black.copy(alpha = 0.60f), CircleShape)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val r = size.minDimension / 2f
                    val c = Offset(size.width / 2f, size.height / 2f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                            center = c,
                            radius = r
                        ),
                        radius = r
                    )
                    drawCircle(
                        color = glowColor.copy(alpha = 0.20f),
                        radius = 5.dp.toPx(),
                        center = c
                    )
                    drawCircle(
                        color = glowColor.copy(alpha = 0.60f),
                        radius = 3.dp.toPx(),
                        center = c
                    )
                }
            }

            // Four Keys (54dp each)
            StaticFourLensesKey(
                direction = "UP",
                glowColor = glowColor,
                rotationAngle = 0f,
                modifier = Modifier.offset(55.dp, 0.dp)
            )
            StaticFourLensesKey(
                direction = "LEFT",
                glowColor = glowColor,
                rotationAngle = 270f,
                modifier = Modifier.offset(0.dp, 55.dp)
            )
            StaticFourLensesKey(
                direction = "RIGHT",
                glowColor = glowColor,
                rotationAngle = 90f,
                modifier = Modifier.offset(110.dp, 55.dp)
            )
            StaticFourLensesKey(
                direction = "DOWN",
                glowColor = glowColor,
                rotationAngle = 180f,
                modifier = Modifier.offset(55.dp, 110.dp)
            )
        }
    }
}

@Composable
private fun StaticFourLensesKey(
    direction: String,
    glowColor: Color,
    rotationAngle: Float,
    modifier: Modifier = Modifier
) {
    val baseDomeGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF232527),
                Color(0xFF0C0D0E),
                Color(0xFF000000)
            ),
            center = Offset(0.50f, 0.55f),
            radius = 120f
        )
    }

    val dishGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF050506),
                Color(0xFF121314)
            ),
            center = Offset(0.50f, 0.60f),
            radius = 80f
        )
    }

    Box(
        modifier = modifier
            .size(54.dp)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glowColor.copy(alpha = 0.45f * 0.50f),
                            glowColor.copy(alpha = 0.45f * 0.20f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.minDimension * 0.85f
                    ),
                    radius = size.minDimension * 0.85f
                )
            }
            .shadow(4.dp, CircleShape, spotColor = glowColor, ambientColor = glowColor)
            .clip(CircleShape)
            .background(baseDomeGradient)
            .border(1.dp, Color.Black.copy(alpha = 0.50f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val r = size.minDimension / 2f
            val center = Offset(w / 2f, h / 2f)

            // Inset bottom shadow
            val insetH = h * 0.35f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.70f)),
                    startY = h - insetH,
                    endY = h
                ),
                topLeft = Offset(0f, h - insetH),
                size = Size(w, insetH)
            )

            // Recessed Dish (34dp)
            val dishRadius = 17.dp.toPx()
            drawCircle(brush = dishGradient, radius = dishRadius, center = center)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)),
                    center = center,
                    radius = dishRadius
                ),
                radius = dishRadius,
                center = center
            )
            drawArc(
                color = Color.White.copy(alpha = 0.05f),
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(center.x - dishRadius, center.y - dishRadius),
                size = Size(dishRadius * 2f, dishRadius * 2f),
                style = Stroke(width = 1.dp.toPx())
            )

            // Directional Chevron
            val s = (34.dp.toPx()) / 44f
            val chevronPath = Path().apply {
                moveTo(center.x - 14f * s, center.y + 7f * s)
                lineTo(center.x, center.y - 7f * s)
                lineTo(center.x + 14f * s, center.y + 7f * s)
            }
            withTransform({
                rotate(degrees = rotationAngle, pivot = center)
            }) {
                drawPath(
                    path = chevronPath,
                    color = glowColor.copy(alpha = 0.90f),
                    style = Stroke(
                        width = 6f * s,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            // Inset Neon Ring
            val ringRadius = r - 3.dp.toPx()
            drawCircle(
                color = glowColor.copy(alpha = 0.22f),
                radius = ringRadius,
                center = center,
                style = Stroke(width = 4.dp.toPx())
            )
            drawCircle(
                color = glowColor.copy(alpha = 0.70f),
                radius = ringRadius,
                center = center,
                style = Stroke(width = 1.8.dp.toPx())
            )

            // Top Specular Glass Lens
            val lensInset = 1.dp.toPx()
            drawArc(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.14f),
                        Color.White.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = h * 0.40f
                ),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(lensInset, lensInset),
                size = Size(w - lensInset * 2f, h - lensInset * 2f),
                style = Stroke(width = 1.2.dp.toPx())
            )
            drawArc(
                color = Color.Black.copy(alpha = 0.30f),
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(lensInset, lensInset),
                size = Size(w - lensInset * 2f, h - lensInset * 2f),
                style = Stroke(width = 1.dp.toPx())
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.06f), Color.Transparent),
                    center = Offset(w * 0.70f, h * 0.78f),
                    radius = w * 0.22f
                ),
                center = Offset(w * 0.70f, h * 0.78f),
                radius = w * 0.22f
            )
        }
    }
}

/**
 * Exact visual match for ui/components/controller/DiscDPad.kt (Disc D-Pad .dp-disc)
 */
@Preview
@Composable
internal fun StaticDiscDPad(
    modifier: Modifier = Modifier
) {
    val glowColor = Color(0xFFE055B8)

    val domeGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF232527),
                Color(0xFF0C0D0E),
                Color(0xFF000000)
            ),
            center = Offset(0.50f, 0.55f),
            radius = 320f
        )
    }

    val puckGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF34373B),
                Color(0xFF161718),
                Color(0xFF0C0D0E)
            ),
            center = Offset(0.50f, 0.38f),
            radius = 60f
        )
    }

    Box(
        modifier = modifier
            .size(160.dp)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glowColor.copy(alpha = 0.45f * 0.50f),
                            glowColor.copy(alpha = 0.45f * 0.20f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.minDimension * 0.95f
                    ),
                    radius = size.minDimension * 0.95f
                )
            }
            .shadow(6.dp, CircleShape, ambientColor = glowColor, spotColor = glowColor)
            .clip(CircleShape)
            .background(domeGradient)
            .border(1.dp, Color.Black.copy(alpha = 0.50f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val r = size.minDimension / 2f
            val center = Offset(w / 2f, h / 2f)

            // A. Concentric machined grooves: repeating every 10dp from center outwards
            val grooveIntervalPx = 10.dp.toPx()
            var currentRadius = grooveIntervalPx
            while (currentRadius < r - 4.dp.toPx()) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.035f),
                    radius = currentRadius,
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )
                currentRadius += grooveIntervalPx
            }

            // B. Inset bottom undercut shadow: inset 0 -6px 9px rgba(0,0,0,.70)
            val insetH = h * 0.35f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.70f)),
                    startY = h - insetH,
                    endY = h
                ),
                topLeft = Offset(0f, h - insetH),
                size = Size(w, insetH)
            )

            // C. Directional Triangles (.tri)
            val triWidthPx = 16.dp.toPx()
            val triHeightPx = 13.dp.toPx()
            val triMarginPx = 16.dp.toPx()
            val halfWidthPx = triWidthPx / 2f

            // UP
            val upPath = Path().apply {
                moveTo(center.x, triMarginPx)
                lineTo(center.x - halfWidthPx, triMarginPx + triHeightPx)
                lineTo(center.x + halfWidthPx, triMarginPx + triHeightPx)
                close()
            }
            drawPath(upPath, color = glowColor.copy(alpha = 0.35f))

            // DOWN
            val downPath = Path().apply {
                moveTo(center.x, h - triMarginPx)
                lineTo(center.x - halfWidthPx, h - triMarginPx - triHeightPx)
                lineTo(center.x + halfWidthPx, h - triMarginPx - triHeightPx)
                close()
            }
            drawPath(downPath, color = glowColor.copy(alpha = 0.35f))

            // LEFT
            val leftPath = Path().apply {
                moveTo(triMarginPx, center.y)
                lineTo(triMarginPx + triHeightPx, center.y - halfWidthPx)
                lineTo(triMarginPx + triHeightPx, center.y + halfWidthPx)
                close()
            }
            drawPath(leftPath, color = glowColor.copy(alpha = 0.35f))

            // RIGHT
            val rightPath = Path().apply {
                moveTo(w - triMarginPx, center.y)
                lineTo(w - triMarginPx - triHeightPx, center.y - halfWidthPx)
                lineTo(w - triMarginPx - triHeightPx, center.y + halfWidthPx)
                close()
            }
            drawPath(rightPath, color = glowColor.copy(alpha = 0.35f))

            // D. Outer Neon Ring (.lx-ring): border 2px solid var(--glow)
            val ringInset = 3.dp.toPx()
            val ringR = r - ringInset
            drawCircle(
                color = glowColor.copy(alpha = 0.15f),
                radius = ringR,
                center = center,
                style = Stroke(width = 4.dp.toPx())
            )
            drawCircle(
                color = glowColor.copy(alpha = 0.40f),
                radius = ringR,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // E. Specular Optical Glass Lens (.lx-lens)
            drawArc(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.14f),
                        Color.White.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = h * 0.42f
                ),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                size = Size(w - 2.dp.toPx(), h - 2.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.06f), Color.Transparent),
                    center = Offset(w * 0.70f, h * 0.78f),
                    radius = w * 0.28f
                ),
                center = Offset(w * 0.70f, h * 0.78f),
                radius = w * 0.28f
            )
            drawArc(
                color = Color.White.copy(alpha = 0.12f),
                startAngle = 200f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(1.5.dp.toPx(), 1.5.dp.toPx()),
                size = Size(w - 3.dp.toPx(), h - 3.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )
            drawArc(
                color = Color.Black.copy(alpha = 0.30f),
                startAngle = 20f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(1.5.dp.toPx(), 1.5.dp.toPx()),
                size = Size(w - 3.dp.toPx(), h - 3.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )
        }

        // F. Central Puck (.disc-puck)
        Box(
            modifier = Modifier
                .size(34.dp)
                .shadow(6.dp, CircleShape, spotColor = Color.Black, ambientColor = Color.Black)
                .clip(CircleShape)
                .background(puckGradient)
                .border(1.dp, Color.Black.copy(alpha = 0.60f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val pw = size.width
                val ph = size.height
                val pr = size.minDimension / 2f
                val pcenter = Offset(pw / 2f, ph / 2f)

                // Puck Inset highlight
                drawArc(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.20f), Color.Transparent),
                        startY = 0f,
                        endY = ph * 0.45f
                    ),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                    size = Size(pw - 2.dp.toPx(), ph - 2.dp.toPx()),
                    style = Stroke(width = 1.5.dp.toPx())
                )

                // Inset 4dp neon ring (.disc-puck::before)
                val puckRingInset = 4.dp.toPx()
                val puckRingR = pr - puckRingInset
                drawCircle(
                    color = glowColor.copy(alpha = 0.25f),
                    radius = puckRingR,
                    center = pcenter,
                    style = Stroke(width = 4.dp.toPx())
                )
                drawCircle(
                    color = glowColor.copy(alpha = 0.75f),
                    radius = puckRingR,
                    center = pcenter,
                    style = Stroke(width = 2.dp.toPx())
                )

                // Center 6dp glowing neon pip (.disc-puck::after)
                val pipRadius = 3.dp.toPx()
                drawCircle(
                    color = glowColor.copy(alpha = 0.40f),
                    radius = pipRadius + 2.dp.toPx(),
                    center = pcenter
                )
                drawCircle(
                    color = glowColor,
                    radius = pipRadius,
                    center = pcenter
                )
            }
        }
    }
}

/**
 * Exact visual match for ui/components/controller/CapsulesDPad.kt (Capsules D-Pad .dp-cap)
 */
@Preview
@Composable
internal fun StaticCapsulesDPad(
    modifier: Modifier = Modifier
) {
    val glowColor = Color(0xFFB58CFF)

    val hubGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF030304),
                Color(0xFF151617)
            ),
            center = Offset(0.50f, 0.60f),
            radius = 60f
        )
    }

    Box(
        modifier = modifier.size(170.dp),
        contentAlignment = Alignment.Center
    ) {
        // Central Stationary/Pivot Hub (.cp-hub, 38dp x 38dp at 66dp, 66dp)
        Box(
            modifier = Modifier
                .offset(66.dp, 66.dp)
                .size(38.dp)
                .shadow(4.dp, CircleShape, spotColor = Color.Black, ambientColor = Color.Black)
                .clip(CircleShape)
                .background(hubGradient),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val h = size.height
                val center = Offset(size.width / 2f, size.height / 2f)

                // Inset deep shadow
                drawCircle(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent),
                        startY = 0f,
                        endY = h * 0.6f
                    ),
                    radius = size.minDimension / 2f
                )

                // Inner glowing concentric ring
                val ringRadius = (38.dp.toPx() / 2f) - 11.dp.toPx()
                drawCircle(
                    color = glowColor.copy(alpha = 0.50f),
                    radius = ringRadius,
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )
            }
        }

        // Four Capsule Keys
        StaticCapsuleKey(
            isVertical = true,
            glowColor = glowColor,
            rotationAngle = 0f,
            modifier = Modifier.offset(62.dp, 0.dp)
        )
        StaticCapsuleKey(
            isVertical = true,
            glowColor = glowColor,
            rotationAngle = 180f,
            modifier = Modifier.offset(62.dp, 102.dp)
        )
        StaticCapsuleKey(
            isVertical = false,
            glowColor = glowColor,
            rotationAngle = 270f,
            modifier = Modifier.offset(0.dp, 62.dp)
        )
        StaticCapsuleKey(
            isVertical = false,
            glowColor = glowColor,
            rotationAngle = 90f,
            modifier = Modifier.offset(102.dp, 62.dp)
        )
    }
}

@Composable
private fun StaticCapsuleKey(
    isVertical: Boolean,
    glowColor: Color,
    rotationAngle: Float,
    modifier: Modifier = Modifier
) {
    val capsuleShape = remember { RoundedCornerShape(23.dp) }
    val baseDomeGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF232527),
                Color(0xFF0C0D0E),
                Color(0xFF000000)
            ),
            center = Offset(0.50f, 0.55f),
            radius = 140f
        )
    }

    val capsuleWidth = if (isVertical) 46.dp else 68.dp
    val capsuleHeight = if (isVertical) 68.dp else 46.dp

    Box(
        modifier = modifier
            .size(capsuleWidth, capsuleHeight)
            .drawBehind {
                val pad = 8.dp.toPx()
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glowColor.copy(alpha = 0.45f * 0.48f),
                            glowColor.copy(alpha = 0.45f * 0.18f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.minDimension * 0.95f
                    ),
                    topLeft = Offset(-pad, -pad),
                    size = Size(size.width + pad * 2, size.height + pad * 2),
                    cornerRadius = CornerRadius(23.dp.toPx() + pad, 23.dp.toPx() + pad)
                )
            }
            .shadow(
                elevation = 6.dp,
                shape = capsuleShape,
                ambientColor = glowColor,
                spotColor = glowColor
            )
            .clip(capsuleShape)
            .background(baseDomeGradient)
            .border(
                width = 1.dp,
                color = Color.Black.copy(alpha = 0.50f),
                shape = capsuleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val center = Offset(w / 2f, h / 2f)

            // 1. Inset bottom shadow
            val insetH = h * 0.35f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.70f)),
                    startY = h - insetH,
                    endY = h
                ),
                topLeft = Offset(0f, h - insetH),
                size = Size(w, insetH)
            )

            // 2. Vector Directional Chevron: M6 15 L12 9 L18 15
            val chevronScale = 24.dp.toPx() / 24f
            val chevronPath = Path().apply {
                moveTo(center.x + (-6f) * chevronScale, center.y + 3f * chevronScale)
                lineTo(center.x, center.y + (-3f) * chevronScale)
                lineTo(center.x + 6f * chevronScale, center.y + 3f * chevronScale)
            }

            withTransform({
                rotate(degrees = rotationAngle, pivot = center)
            }) {
                drawPath(
                    path = chevronPath,
                    color = glowColor,
                    style = Stroke(
                        width = 3.4f * chevronScale,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            // 3. Inset 3dp Glowing Neon Ring
            val ringInset = 3.dp.toPx()
            val ringCorner = 20.dp.toPx()
            val ringW = w - ringInset * 2f
            val ringH = h - ringInset * 2f

            drawRoundRect(
                color = glowColor.copy(alpha = 0.25f),
                topLeft = Offset(ringInset, ringInset),
                size = Size(ringW, ringH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(ringCorner, ringCorner),
                style = Stroke(width = 3.dp.toPx())
            )
            drawRoundRect(
                color = glowColor.copy(alpha = 0.70f),
                topLeft = Offset(ringInset, ringInset),
                size = Size(ringW, ringH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(ringCorner, ringCorner),
                style = Stroke(width = 2.dp.toPx())
            )

            // 4. Specular Optical Glass Lens
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.14f),
                        Color.White.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = h * 0.45f
                ),
                topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                size = Size(w - 2.dp.toPx(), h * 0.45f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(22.dp.toPx(), 22.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.06f), Color.Transparent),
                    center = Offset(w * 0.70f, h * 0.78f),
                    radius = minOf(w, h) * 0.35f
                ),
                center = Offset(w * 0.70f, h * 0.78f),
                radius = minOf(w, h) * 0.35f
            )

            drawLine(
                color = Color.White.copy(alpha = 0.12f),
                start = Offset(ringCorner, 1.dp.toPx()),
                end = Offset(w - ringCorner, 1.dp.toPx()),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = Color.Black.copy(alpha = 0.30f),
                start = Offset(ringCorner, h - 1.dp.toPx()),
                end = Offset(w - ringCorner, h - 1.dp.toPx()),
                strokeWidth = 1.dp.toPx()
            )
        }
    }
}

/**
 * Exact visual match for ui/components/controller/MetaballsDPad.kt (Lens Metaballs D-Pad .dp-meta)
 */
@Preview
@Composable
internal fun StaticMetaballsDPad(
    modifier: Modifier = Modifier
) {
    val glowColor = Color(0xFF3FD2C4)

    val domeGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF232527),
                Color(0xFF0C0D0E),
                Color(0xFF000000)
            ),
            center = Offset(0.50f, 0.55f),
            radius = 340f
        )
    }

    val capGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF2F3134),
                Color(0xFF141517),
                Color(0xFF0A0B0C)
            ),
            center = Offset(0.50f, 0.38f),
            radius = 60f
        )
    }

    Box(
        modifier = modifier
            .size(172.dp)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glowColor.copy(alpha = 0.45f * 0.50f),
                            glowColor.copy(alpha = 0.45f * 0.20f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.minDimension * 0.95f
                    ),
                    radius = size.minDimension * 0.95f
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Base Convex Acrylic Dome Body
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(
                    elevation = 6.dp,
                    shape = CircleShape,
                    ambientColor = glowColor,
                    spotColor = glowColor
                )
                .clip(CircleShape)
                .background(domeGradient)
                .border(
                    width = 1.dp,
                    color = Color.Black.copy(alpha = 0.50f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Background Inset Undercut Shadow
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                val insetH = h * 0.35f
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.70f)),
                        startY = h - insetH,
                        endY = h
                    ),
                    topLeft = Offset(0f, h - insetH),
                    size = Size(w, insetH)
                )
            }

            // Liquid Neon Metaballs Layer (.goo2)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val baseDistPx = 58.dp.toPx()
                val rCenterPx = 19.dp.toPx() // c0 radius (diameter 38dp)
                val rSatPx = 23.dp.toPx()    // sb radius (diameter 46dp)

                val satPositions = listOf(
                    Offset(center.x, center.y - baseDistPx),
                    Offset(center.x + baseDistPx, center.y),
                    Offset(center.x, center.y + baseDistPx),
                    Offset(center.x - baseDistPx, center.y)
                )

                // Central fluid circle (.c0, diameter 38dp)
                drawCircle(
                    color = glowColor.copy(alpha = 0.30f),
                    radius = rCenterPx + 2.5.dp.toPx(),
                    center = center
                )
                drawCircle(
                    color = glowColor.copy(alpha = 0.92f),
                    radius = rCenterPx,
                    center = center
                )

                // Satellite fluid circles (.sb, diameter 46dp)
                satPositions.forEach { satPos ->
                    drawCircle(
                        color = glowColor.copy(alpha = 0.30f),
                        radius = rSatPx + 2.5.dp.toPx(),
                        center = satPos
                    )
                    drawCircle(
                        color = glowColor.copy(alpha = 0.92f),
                        radius = rSatPx,
                        center = satPos
                    )
                }
            }

            // Central tactile cap (.cp2.c0, 24dp x 24dp at center)
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .shadow(4.dp, CircleShape, spotColor = Color.Black, ambientColor = Color.Black)
                    .clip(CircleShape)
                    .background(capGradient)
                    .border(1.dp, Color.Black.copy(alpha = 0.60f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Central 6dp glowing neon pip
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val pcenter = Offset(size.width / 2f, size.height / 2f)
                    val pipRadius = 3.dp.toPx()
                    drawCircle(
                        color = glowColor.copy(alpha = 0.40f),
                        radius = pipRadius + 2.dp.toPx(),
                        center = pcenter
                    )
                    drawCircle(
                        color = glowColor,
                        radius = pipRadius,
                        center = pcenter
                    )
                }
            }

            // Four Satellite Caps (.cp2.sat, 36dp x 36dp with directional chevrons)
            StaticSatelliteCap(
                glowColor = glowColor,
                capGradient = capGradient,
                rotationAngle = 0f,
                modifier = Modifier.offset(0.dp, (-58).dp)
            )
            StaticSatelliteCap(
                glowColor = glowColor,
                capGradient = capGradient,
                rotationAngle = 90f,
                modifier = Modifier.offset(58.dp, 0.dp)
            )
            StaticSatelliteCap(
                glowColor = glowColor,
                capGradient = capGradient,
                rotationAngle = 180f,
                modifier = Modifier.offset(0.dp, 58.dp)
            )
            StaticSatelliteCap(
                glowColor = glowColor,
                capGradient = capGradient,
                rotationAngle = 270f,
                modifier = Modifier.offset((-58).dp, 0.dp)
            )
        }

        // Outer Neon Ring (.lx-ring) & Top Specular Glass Lens (.lx-lens)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val r = size.minDimension / 2f
            val center = Offset(w / 2f, h / 2f)
            val ringInset = 3.dp.toPx()
            val ringR = r - ringInset

            val ringAlpha = 0.30f
            drawCircle(
                color = glowColor.copy(alpha = ringAlpha * 0.40f),
                radius = ringR,
                center = center,
                style = Stroke(width = 3.dp.toPx())
            )
            drawCircle(
                color = glowColor.copy(alpha = ringAlpha),
                radius = ringR,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // Top specular optical glass lens (.lx-lens)
            drawArc(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.14f),
                        Color.White.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = h * 0.42f
                ),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                size = Size(w - 2.dp.toPx(), h - 2.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )

            // Bottom-right specular reflection sheen
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.06f), Color.Transparent),
                    center = Offset(w * 0.70f, h * 0.78f),
                    radius = w * 0.28f
                ),
                center = Offset(w * 0.70f, h * 0.78f),
                radius = w * 0.28f
            )

            // Chamfer highlights
            drawArc(
                color = Color.White.copy(alpha = 0.12f),
                startAngle = 200f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(1.5.dp.toPx(), 1.5.dp.toPx()),
                size = Size(w - 3.dp.toPx(), h - 3.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )
            drawArc(
                color = Color.Black.copy(alpha = 0.30f),
                startAngle = 20f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(1.5.dp.toPx(), 1.5.dp.toPx()),
                size = Size(w - 3.dp.toPx(), h - 3.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }
}

@Composable
private fun StaticSatelliteCap(
    glowColor: Color,
    capGradient: Brush,
    rotationAngle: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(36.dp)
            .shadow(
                elevation = 4.dp,
                shape = CircleShape,
                spotColor = Color.Black,
                ambientColor = Color.Black
            )
            .clip(CircleShape)
            .background(capGradient)
            .border(
                width = 1.dp,
                color = Color.Black.copy(alpha = 0.60f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)

            val chevronScale = 18.dp.toPx() / 24f
            val chevronPath = Path().apply {
                moveTo(center.x + (-6f) * chevronScale, center.y + 3f * chevronScale)
                lineTo(center.x, center.y + (-3f) * chevronScale)
                lineTo(center.x + 6f * chevronScale, center.y + 3f * chevronScale)
            }

            withTransform({
                rotate(degrees = rotationAngle, pivot = center)
            }) {
                drawPath(
                    path = chevronPath,
                    color = glowColor,
                    style = Stroke(
                        width = 3.4f * chevronScale,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            drawArc(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.22f), Color.Transparent),
                    startY = 0f,
                    endY = size.height * 0.45f
                ),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                size = Size(size.width - 2.dp.toPx(), size.height - 2.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }
}

/**
 * Exact visual match for ui/components/controller/RailsDPad.kt (Rails D-Pad .dp-rail)
 */
@Preview
@Composable
internal fun StaticRailsDPad(
    modifier: Modifier = Modifier
) {
    val glowColor = Color(0xFFFFB13F)

    val domeGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF232527),
                Color(0xFF0C0D0E),
                Color(0xFF000000)
            ),
            center = Offset(0.50f, 0.55f),
            radius = 320f
        )
    }

    val puckGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF34373B),
                Color(0xFF16181A),
                Color(0xFF0C0D0E)
            ),
            center = Offset(0.50f, 0.38f),
            radius = 80f
        )
    }

    val railHGradient = remember {
        Brush.verticalGradient(
            colors = listOf(Color(0xFF000000), Color(0xFF0B0C0D))
        )
    }
    val railVGradient = remember {
        Brush.horizontalGradient(
            colors = listOf(Color(0xFF000000), Color(0xFF0B0C0D))
        )
    }

    Box(
        modifier = modifier
            .size(160.dp)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glowColor.copy(alpha = 0.45f * 0.50f),
                            glowColor.copy(alpha = 0.45f * 0.20f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.minDimension * 0.95f
                    ),
                    radius = size.minDimension * 0.95f
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Base Convex Acrylic Dome Body
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(
                    elevation = 6.dp,
                    shape = CircleShape,
                    ambientColor = glowColor,
                    spotColor = glowColor
                )
                .clip(CircleShape)
                .background(domeGradient)
                .border(
                    width = 1.dp,
                    color = Color.Black.copy(alpha = 0.50f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Background Inset Undercut Shadow
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                val insetH = h * 0.35f
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.70f)),
                        startY = h - insetH,
                        endY = h
                    ),
                    topLeft = Offset(0f, h - insetH),
                    size = Size(w, insetH)
                )
            }

            // Recessed Orthogonal Guide Rails
            // Horizontal Rail: 132dp x 28dp
            Box(
                modifier = Modifier
                    .size(132.dp, 28.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(railHGradient)
                    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(104.dp, 2.dp)
                        .background(glowColor.copy(alpha = 0.20f))
                )
            }

            // Vertical Rail: 28dp x 132dp
            Box(
                modifier = Modifier
                    .size(28.dp, 132.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(railVGradient)
                    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(2.dp, 104.dp)
                        .background(glowColor.copy(alpha = 0.20f))
                )
            }

            // Four End Target LEDs (.ed, 8px diameter at 55dp from center)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val ledRadius = 4.dp.toPx()
                val ledDist = 55.dp.toPx()

                val ledPositions = listOf(
                    Offset(center.x, center.y - ledDist),
                    Offset(center.x, center.y + ledDist),
                    Offset(center.x - ledDist, center.y),
                    Offset(center.x + ledDist, center.y)
                )

                ledPositions.forEach { pos ->
                    drawCircle(
                        color = glowColor.copy(alpha = 0.28f),
                        radius = ledRadius,
                        center = pos
                    )
                }
            }

            // Center Tactile Puck (.rail-puck, 40dp x 40dp)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .shadow(
                        elevation = 6.dp,
                        shape = CircleShape,
                        ambientColor = Color.Black.copy(alpha = 0.40f),
                        spotColor = Color.Black.copy(alpha = 0.55f)
                    )
                    .clip(CircleShape)
                    .background(puckGradient)
                    .border(1.dp, Color.Black.copy(alpha = 0.60f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val center = Offset(w / 2f, h / 2f)

                    val ringInset = 4.dp.toPx()
                    val ringRadius = (w / 2f) - ringInset

                    drawCircle(
                        color = glowColor.copy(alpha = 0.35f),
                        radius = ringRadius,
                        center = center,
                        style = Stroke(width = 4.dp.toPx())
                    )
                    drawCircle(
                        color = glowColor.copy(alpha = 0.80f),
                        radius = ringRadius,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )

                    // Central 7dp amber neon pip
                    val pipRadius = 3.5.dp.toPx()
                    drawCircle(
                        color = glowColor.copy(alpha = 0.40f),
                        radius = pipRadius + 2.dp.toPx(),
                        center = center
                    )
                    drawCircle(
                        color = glowColor,
                        radius = pipRadius,
                        center = center
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.60f),
                        radius = pipRadius * 0.45f,
                        center = center
                    )

                    // Top specular highlight arc
                    drawArc(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.White.copy(alpha = 0.20f), Color.Transparent),
                            startY = 0f,
                            endY = h * 0.45f
                        ),
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                        size = Size(w - 2.dp.toPx(), h - 2.dp.toPx()),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }
        }

        // Outer Neon Ring (.lx-ring) & Top Specular Glass Lens (.lx-lens)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val r = size.minDimension / 2f
            val center = Offset(w / 2f, h / 2f)
            val ringInset = 3.dp.toPx()
            val ringR = r - ringInset

            val ringAlpha = 0.35f
            drawCircle(
                color = glowColor.copy(alpha = ringAlpha * 0.40f),
                radius = ringR,
                center = center,
                style = Stroke(width = 3.dp.toPx())
            )
            drawCircle(
                color = glowColor.copy(alpha = ringAlpha),
                radius = ringR,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // Top specular optical glass lens (.lx-lens)
            drawArc(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.14f),
                        Color.White.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = h * 0.42f
                ),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                size = Size(w - 2.dp.toPx(), h - 2.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )

            // Bottom-right specular reflection sheen
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.06f), Color.Transparent),
                    center = Offset(w * 0.70f, h * 0.78f),
                    radius = w * 0.28f
                ),
                center = Offset(w * 0.70f, h * 0.78f),
                radius = w * 0.28f
            )

            // Chamfer highlights
            drawArc(
                color = Color.White.copy(alpha = 0.12f),
                startAngle = 200f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(1.5.dp.toPx(), 1.5.dp.toPx()),
                size = Size(w - 3.dp.toPx(), h - 3.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )
            drawArc(
                color = Color.Black.copy(alpha = 0.30f),
                startAngle = 20f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(1.5.dp.toPx(), 1.5.dp.toPx()),
                size = Size(w - 3.dp.toPx(), h - 3.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }
}


/**
 * Exact visual match for ui/components/controller/RealisticTrigger.kt
 */
@Composable
private fun StaticRealisticTrigger(
    key: String,
    isLeft: Boolean,
    modifier: Modifier = Modifier
) {
    val triggerShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 46.dp, bottomEnd = 46.dp)
    val windowShape = RoundedCornerShape(15.dp)

    val neonColor = if (isLeft) Color(0xFF00E5FF) else Color(0xFFE055B8)

    val baseDomeGradient = Brush.radialGradient(
        colors = listOf(
            Color(0xFF232527),
            Color(0xFF0C0D0E),
            Color(0xFF000000)
        ),
        center = Offset(0.50f, 0.45f),
        radius = 210f
    )

    val windowGradient = Brush.radialGradient(
        colors = listOf(
            Color(0xFF050506),
            Color(0xFF121314)
        ),
        center = Offset(0.50f, 0.60f),
        radius = 105f
    )

    Box(
        modifier = modifier
            .size(100.dp, 92.dp)
            .drawBehind {
                val pad = 10.dp.toPx()
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            neonColor.copy(alpha = 0.45f * 0.50f),
                            neonColor.copy(alpha = 0.45f * 0.20f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.minDimension * 0.95f
                    ),
                    topLeft = Offset(-pad, -pad),
                    size = Size(size.width + pad * 2, size.height + pad * 2),
                    cornerRadius = CornerRadius(26.dp.toPx(), 26.dp.toPx())
                )
            }
            .shadow(
                elevation = 4.dp,
                shape = triggerShape,
                ambientColor = neonColor,
                spotColor = neonColor
            )
            .clip(triggerShape)
            .background(baseDomeGradient)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.50f),
                        Color.Black.copy(alpha = 0.80f)
                    )
                ),
                shape = triggerShape
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Inset bottom shadow along bottom hull
            val insetH = h * 0.32f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.70f)),
                    startY = h - insetH,
                    endY = h
                ),
                topLeft = Offset(0f, h - insetH),
                size = Size(w, insetH)
            )

            // Neon ring
            val ringInset = 2.5.dp.toPx()
            val ringTopR = 13.5.dp.toPx()
            val ringBotR = 43.5.dp.toPx()
            val ringPath = androidx.compose.ui.graphics.Path().apply {
                addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
                        left = ringInset,
                        top = ringInset,
                        right = w - ringInset,
                        bottom = h - ringInset,
                        topLeftCornerRadius = CornerRadius(ringTopR, ringTopR),
                        topRightCornerRadius = CornerRadius(ringTopR, ringTopR),
                        bottomLeftCornerRadius = CornerRadius(ringBotR, ringBotR),
                        bottomRightCornerRadius = CornerRadius(ringBotR, ringBotR)
                    )
                )
            }
            drawPath(
                path = ringPath,
                color = neonColor.copy(alpha = 0.28f),
                style = Stroke(width = 3.5.dp.toPx())
            )
            drawPath(
                path = ringPath,
                color = neonColor.copy(alpha = 0.70f),
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Top specular line highlight
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.22f),
                        Color.Transparent
                    )
                ),
                start = Offset(w * 0.15f, 2.dp.toPx()),
                end = Offset(w * 0.85f, 2.dp.toPx()),
                strokeWidth = 1.5.dp.toPx()
            )

            // Specular sheen circle
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.06f), Color.Transparent),
                    center = Offset(w * 0.68f, h * 0.75f),
                    radius = w * 0.24f
                ),
                center = Offset(w * 0.68f, h * 0.75f),
                radius = w * 0.24f
            )
        }

        // Recessed optical window
        Box(
            modifier = Modifier
                .offset(y = 12.dp)
                .size(68.dp, 30.dp)
                .shadow(elevation = 3.dp, shape = windowShape, spotColor = Color.Black)
                .clip(windowShape)
                .background(windowGradient)
                .border(width = 1.dp, color = Color.White.copy(alpha = 0.06f), shape = windowShape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val winW = size.width
                val winH = size.height
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent),
                        startY = 0f,
                        endY = winH * 0.45f
                    ),
                    topLeft = Offset(0f, 0f),
                    size = Size(winW, winH * 0.45f)
                )
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)),
                        center = Offset(winW / 2f, winH / 2f),
                        radius = winW / 2f
                    ),
                    topLeft = Offset(0f, 0f),
                    size = Size(winW, winH)
                )
            }
            Text(
                text = key,
                color = neonColor,
                fontWeight = FontWeight.Medium,
                fontSize = 18.sp,
                letterSpacing = 1.5.sp
            )
        }
    }
}

/**
 * Exact visual match for ui/components/controller/RealisticBumper.kt
 */
@Composable
private fun StaticRealisticBumper(
    key: String,
    isLeft: Boolean,
    modifier: Modifier = Modifier
) {
    val bumperShape = if (isLeft) {
        RoundedCornerShape(
            topStart = 10.dp,
            topEnd = 26.dp,
            bottomEnd = 26.dp,
            bottomStart = 10.dp
        )
    } else {
        RoundedCornerShape(
            topStart = 26.dp,
            topEnd = 10.dp,
            bottomEnd = 10.dp,
            bottomStart = 26.dp
        )
    }

    val neonColor = Color(0xFFA97CF0)

    val baseDomeGradient = Brush.radialGradient(
        colors = listOf(
            Color(0xFF232527),
            Color(0xFF0C0D0E),
            Color(0xFF000000)
        ),
        center = Offset(0.50f, 0.55f),
        radius = 280f
    )

    Box(
        modifier = modifier
            .size(154.dp, 48.dp)
            .drawBehind {
                val pad = 12.dp.toPx()
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            neonColor.copy(alpha = 0.45f * 0.48f),
                            neonColor.copy(alpha = 0.45f * 0.18f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.width * 0.65f
                    ),
                    topLeft = Offset(-pad, -pad),
                    size = Size(size.width + pad * 2, size.height + pad * 2),
                    cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx())
                )
            }
            .shadow(
                elevation = 4.dp,
                shape = bumperShape,
                ambientColor = neonColor,
                spotColor = neonColor
            )
            .border(
                width = 1.dp,
                color = Color.Black.copy(alpha = 0.50f),
                shape = bumperShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(bumperShape)
                .background(baseDomeGradient),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Top edge highlight
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.07f), Color.Transparent),
                        startY = 0f,
                        endY = h * 0.42f
                    ),
                    size = Size(w, h * 0.42f)
                )

                // Bottom undercut shadow
                val undercutH = h * 0.40f
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.70f)),
                        startY = h - undercutH,
                        endY = h
                    ),
                    topLeft = Offset(0f, h - undercutH),
                    size = Size(w, undercutH)
                )
            }

            // Magnifier Window — 78dp × 30dp
            val windowShape = RoundedCornerShape(15.dp)
            Box(
                modifier = Modifier
                    .size(78.dp, 30.dp)
                    .clip(windowShape),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val winW = size.width
                    val winH = size.height

                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF050506), Color(0xFF121314)),
                            center = Offset(winW * 0.50f, winH * 0.60f),
                            radius = winW * 0.60f
                        ),
                        size = Size(winW, winH)
                    )

                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.90f), Color.Transparent),
                            startY = 0f,
                            endY = 8.dp.toPx()
                        ),
                        size = Size(winW, 8.dp.toPx())
                    )

                    drawRect(
                        brush = Brush.radialGradient(
                            0.0f to Color.Transparent,
                            0.32f to Color.Transparent,
                            1.0f to Color.Black.copy(alpha = 0.82f),
                            center = Offset(winW / 2f, winH / 2f),
                            radius = winW / 2f
                        ),
                        size = Size(winW, winH)
                    )

                    drawLine(
                        color = Color.White.copy(alpha = 0.05f),
                        start = Offset(3.dp.toPx(), winH - 0.5f),
                        end = Offset(winW - 3.dp.toPx(), winH - 0.5f),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                Text(
                    text = key,
                    color = neonColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    letterSpacing = 1.5.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val ringInset = 2.5.dp.toPx()
            val ringPath = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = Rect(ringInset, ringInset, w - ringInset, h - ringInset),
                        topLeft = CornerRadius(if (isLeft) 7.5.dp.toPx() else 23.5.dp.toPx()),
                        topRight = CornerRadius(if (isLeft) 23.5.dp.toPx() else 7.5.dp.toPx()),
                        bottomRight = CornerRadius(if (isLeft) 23.5.dp.toPx() else 7.5.dp.toPx()),
                        bottomLeft = CornerRadius(if (isLeft) 7.5.dp.toPx() else 23.5.dp.toPx())
                    )
                )
            }

            drawPath(
                path = ringPath,
                color = neonColor.copy(alpha = 0.25f),
                style = Stroke(width = 5.dp.toPx())
            )
            drawPath(
                path = ringPath,
                color = neonColor.copy(alpha = 0.18f),
                style = Stroke(width = 3.dp.toPx())
            )
            drawPath(
                path = ringPath,
                color = neonColor.copy(alpha = 0.70f),
                style = Stroke(width = 2.dp.toPx())
            )

            // Specular sheen circle
            drawCircle(
                brush = Brush.radialGradient(
                    0.0f to Color.White.copy(alpha = 0.06f),
                    0.40f to Color.Transparent,
                    1.0f to Color.Transparent,
                    center = Offset(w * 0.70f, h * 0.78f),
                    radius = w * 0.30f
                ),
                center = Offset(w * 0.70f, h * 0.78f),
                radius = w * 0.30f
            )

            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.10f), Color.Transparent),
                    startY = 0f,
                    endY = 3.5.dp.toPx()
                ),
                size = Size(w, 3.5.dp.toPx())
            )

            val rtl = if (isLeft) 10.dp.toPx() else 26.dp.toPx()
            val rtr = if (isLeft) 26.dp.toPx() else 10.dp.toPx()
            val rbl = if (isLeft) 10.dp.toPx() else 26.dp.toPx()
            val rbr = if (isLeft) 26.dp.toPx() else 10.dp.toPx()

            drawLine(
                color = Color.White.copy(alpha = 0.12f),
                start = Offset(rtl, 0.5f),
                end = Offset(w - rtr, 0.5f),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = Color.Black.copy(alpha = 0.30f),
                start = Offset(rbl, h - 0.5f),
                end = Offset(w - rbr, h - 0.5f),
                strokeWidth = 1.dp.toPx()
            )
        }
    }
}

/**
 * Exact visual match for ui/components/controller/RealisticSystemButton.kt
 */
@Composable
private fun StaticRealisticSystemButton(
    label: String,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    val ctrl = when (label) {
        "⨂", "GUIDE", "HOME" -> ControlKey.GUIDE
        "☰", "START", "MENU" -> ControlKey.START
        "⧉", "BACK", "VIEW" -> ControlKey.BACK
        "⇪", "SHARE", "CAPTURE" -> ControlKey.SHARE
        else -> null
    }
    val isGuide = ctrl == ControlKey.GUIDE
    val buttonSize = if (isGuide) 74.dp else 60.dp
    val iconSize = if (isGuide) 36.dp else 24.dp

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
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            textColor.copy(alpha = 0.40f * 0.55f),
                            textColor.copy(alpha = 0.40f * 0.22f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.minDimension * 0.85f
                    ),
                    radius = size.minDimension * 0.85f
                )
            }
            .shadow(4.dp, CircleShape, ambientColor = textColor, spotColor = textColor)
            .clip(CircleShape)
            .background(domeGradient)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Black.copy(alpha = 0.50f), Color.Black.copy(alpha = 0.85f))
                ),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val r = size.minDimension / 2f

            // Recessed undercut shadow
            val botShadowH = h * 0.38f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.70f)),
                    startY = h - botShadowH,
                    endY = h
                ),
                topLeft = Offset(0f, h - botShadowH),
                size = Size(w, botShadowH)
            )

            // Emissive neon ring
            val ringR = r - 3.dp.toPx()
            drawCircle(color = textColor.copy(alpha = 0.30f), radius = ringR, style = Stroke(width = 4.dp.toPx()))
            drawCircle(color = textColor.copy(alpha = 0.70f), radius = ringR, style = Stroke(width = 2.dp.toPx()))

            // Home secondary ring
            if (isGuide) {
                val ring2R = r - 12.dp.toPx()
                drawCircle(color = textColor.copy(alpha = 0.30f), radius = ring2R, style = Stroke(width = 1.dp.toPx()))
            }

            // Vector icon
            if (ctrl != null) {
                val iconPx = iconSize.toPx()
                drawSystemIcon(
                    controlKey = ctrl,
                    color = textColor.copy(alpha = 0.20f),
                    iconSizePx = iconPx + 2.dp.toPx(),
                    center = center
                )
                drawSystemIcon(
                    controlKey = ctrl,
                    color = textColor,
                    iconSizePx = iconPx,
                    center = center
                )
            }

            // Top specular arc
            drawArc(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.16f), Color.Transparent),
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

            // Lens reflection sheen
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

        if (ctrl == null) {
            Text(
                text = label.take(2),
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
    }
}

/**
 * Exact visual match for ui/components/controller/RealisticMacroButton.kt
 */
@Composable
private fun StaticRealisticMacroButton(
    label: String,
    modifier: Modifier = Modifier
) {
    val gradient = Brush.linearGradient(
        colors = listOf(Color(0xFF333333), Color(0xFF111111))
    )
    val macroColor = Color(0xFFFFD600)

    Box(
        modifier = modifier
            .size(80.dp, 40.dp)
            .drawBehind {
                val pad = 12.dp.toPx()
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            macroColor.copy(alpha = 0.40f * 0.50f),
                            macroColor.copy(alpha = 0.40f * 0.20f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.width * 0.70f
                    ),
                    topLeft = Offset(-pad, -pad),
                    size = Size(size.width + pad * 2, size.height + pad * 2),
                    cornerRadius = CornerRadius(16.dp.toPx() + pad, 16.dp.toPx() + pad)
                )
            }
            .shadow(4.dp, RoundedCornerShape(16.dp), ambientColor = macroColor, spotColor = macroColor)
            .clip(RoundedCornerShape(16.dp))
            .background(gradient),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

/**
 * Exact visual match for ui/components/controller/ArcBumper.kt
 */
@Composable
internal fun StaticArcBumper(
    key: String = "LB",
    modifier: Modifier = Modifier
) {
    val isLeft = remember(key) {
        val upper = key.uppercase()
        upper == com.sanket.tools.nexpad.model.NexpadKeys.LB || upper == "L1" || upper == "LEFT"
    }
    val neonColor = if (isLeft) Color(0xFFA97CF0) else Color(0xFF00E5FF)
    val bumperShape = RoundedCornerShape(24.dp)

    Box(
        modifier = modifier
            .size(154.dp, 56.dp)
            .drawBehind {
                val pad = 12.dp.toPx()
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            neonColor.copy(alpha = 0.45f * 0.48f),
                            neonColor.copy(alpha = 0.45f * 0.18f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.width * 0.65f
                    ),
                    topLeft = Offset(-pad, -pad),
                    size = Size(size.width + pad * 2, size.height + pad * 2),
                    cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx())
                )
            }
            .shadow(4.dp, bumperShape, ambientColor = neonColor, spotColor = neonColor),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val s = kotlin.math.min(w / 230f, h / 84f)
            val ox = (w - 230f * s) / 2f
            val oy = (h - 84f * s) / 2f

            val arcPath = Path().apply {
                moveTo(ox + 24f * s, oy + 62f * s)
                quadraticTo(ox + 115f * s, oy - 10f * s, ox + 206f * s, oy + 62f * s)
            }

            val specPath = Path().apply {
                moveTo(ox + 40f * s, oy + (56f - 9f) * s)
                quadraticTo(ox + 115f * s, oy + (-6f - 9f) * s, ox + 190f * s, oy + (56f - 9f) * s)
            }

            // Layer a0: Drop shadow base
            drawPath(
                path = arcPath,
                color = Color.Black.copy(alpha = 0.60f),
                style = Stroke(width = 56f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Layer a1: Emissive neon halo - core crisp stroke
            drawPath(
                path = arcPath,
                color = neonColor.copy(alpha = 0.70f),
                style = Stroke(width = 50f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Layer a2: Tubular body gradient
            val topY = oy + 6f * s
            val botY = oy + 80f * s
            val bodyGradient = Brush.verticalGradient(
                colors = listOf(Color(0xFF282A2E), Color(0xFF08090A)),
                startY = topY,
                endY = botY
            )
            drawPath(
                path = arcPath,
                brush = bodyGradient,
                style = Stroke(width = 46f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Layer a3: Specular crescent highlight
            drawPath(
                path = specPath,
                color = Color.White.copy(alpha = 0.12f),
                style = Stroke(width = 6f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = key,
                color = neonColor,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
                letterSpacing = 2.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.offset(y = (-10.5).dp)
            )
        }
    }
}

/**
 * Static preview for Bumper B — LED Bar (LB / RB).
 * Exact visual match for ui/components/controller/LedBumper.kt in idle state.
 */
@Composable
internal fun StaticLedBumper(
    key: String = "LB",
    modifier: Modifier = Modifier
) {
    val isLeft = remember(key) {
        val upper = key.uppercase()
        upper == com.sanket.tools.nexpad.model.NexpadKeys.LB || upper == "L1" || upper == "LEFT"
    }
    val neonColor = if (isLeft) Color(0xFFA97CF0) else Color(0xFF00E5FF)

    val bumperShape = remember(isLeft) {
        if (isLeft) {
            androidx.compose.foundation.shape.RoundedCornerShape(
                topStart = 12.dp, topEnd = 27.dp, bottomEnd = 27.dp, bottomStart = 12.dp
            )
        } else {
            androidx.compose.foundation.shape.RoundedCornerShape(
                topStart = 27.dp, topEnd = 12.dp, bottomEnd = 12.dp, bottomStart = 27.dp
            )
        }
    }

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

    val windowGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF050506),
                Color(0xFF121314)
            ),
            center = Offset(0.50f, 0.60f),
            radius = 70f
        )
    }

    Box(
        modifier = modifier
            .size(154.dp, 54.dp)
            .drawBehind {
                val pad = 12.dp.toPx()
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            neonColor.copy(alpha = 0.45f * 0.48f),
                            neonColor.copy(alpha = 0.45f * 0.18f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.width * 0.65f
                    ),
                    topLeft = Offset(-pad, -pad),
                    size = Size(size.width + pad * 2, size.height + pad * 2),
                    cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx())
                )
            }
            .shadow(
                elevation = 4.dp,
                shape = bumperShape,
                ambientColor = neonColor,
                spotColor = neonColor
            )
            .clip(bumperShape)
            .background(baseDomeGradient)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.50f),
                        Color.Black.copy(alpha = 0.80f)
                    )
                ),
                shape = bumperShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Inset bottom shadow
            val insetH = h * 0.40f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.70f)),
                    startY = h - insetH,
                    endY = h
                ),
                topLeft = Offset(0f, h - insetH),
                size = Size(w, insetH)
            )

            // Neon color ring — inset 2.5dp with exact asymmetric per-corner radii
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
                color = neonColor.copy(alpha = 0.28f * 0.70f),
                style = Stroke(width = 3.5.dp.toPx())
            )

            // Core crisp stroke
            drawPath(
                path = ringPath,
                color = neonColor.copy(alpha = 0.70f),
                style = Stroke(width = 1.8.dp.toPx())
            )

            // Glass lens highlight
            val sheenH = h * 0.42f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.14f),
                        Color.White.copy(alpha = 0.04f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = sheenH
                ),
                topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                size = Size(w - 2.dp.toPx(), sheenH)
            )

            val specStartX = if (isLeft) w * 0.10f else w * 0.18f
            val specEndX = if (isLeft) w * 0.82f else w * 0.90f
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.45f),
                        Color.White.copy(alpha = 0.16f)
                    ),
                    startX = specStartX,
                    endX = specEndX
                ),
                start = Offset(specStartX, 4f),
                end = Offset(specEndX, 4f),
                strokeWidth = 1.5f
            )
        }

        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            if (isLeft) {
                StaticRecessedWindow(key = key, neonColor = neonColor, windowGradient = windowGradient)
                Box(modifier = Modifier.size(8.dp, 1.dp))
                StaticLedBarGroup(neonColor = neonColor)
            } else {
                StaticLedBarGroup(neonColor = neonColor)
                Box(modifier = Modifier.size(8.dp, 1.dp))
                StaticRecessedWindow(key = key, neonColor = neonColor, windowGradient = windowGradient)
            }
        }
    }
}

@Composable
private fun StaticRecessedWindow(
    key: String,
    neonColor: Color,
    windowGradient: Brush
) {
    Box(
        modifier = Modifier
            .size(48.dp, 28.dp)
            .shadow(
                elevation = 2.dp,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                ambientColor = Color.Black,
                spotColor = Color.Black
            )
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
            .background(windowGradient)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.06f),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val winW = size.width
            val winH = size.height

            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Black.copy(alpha = 0.92f), Color.Transparent),
                    startY = 0f,
                    endY = 7.dp.toPx()
                ),
                size = Size(winW, 7.dp.toPx())
            )

            drawRect(
                brush = Brush.radialGradient(
                    0.0f to Color.Transparent,
                    0.32f to Color.Transparent,
                    1.0f to Color.Black.copy(alpha = 0.85f),
                    center = Offset(winW / 2f, winH / 2f),
                    radius = winW * 0.55f
                )
            )

            drawLine(
                color = Color.White.copy(alpha = 0.06f),
                start = Offset(4.dp.toPx(), winH - 0.5f),
                end = Offset(winW - 4.dp.toPx(), winH - 0.5f),
                strokeWidth = 1.dp.toPx()
            )
        }

        Text(
            text = key,
            color = neonColor,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            letterSpacing = 1.5.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun StaticLedBarGroup(neonColor: Color) {
    androidx.compose.foundation.layout.Row(
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(6) {
            Box(
                modifier = Modifier
                    .size(9.dp, 22.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(3.5.dp))
                    .background(neonColor.copy(alpha = 0.22f))
            )
        }
    }
}

/**
 * Static preview for Bumper C — Peek (LB / RB).
 * Exact visual match for ui/components/controller/PeekBumper.kt in idle state.
 */
@Composable
internal fun StaticPeekBumper(
    key: String = "LB",
    modifier: Modifier = Modifier
) {
    val isLeft = remember(key) {
        val upper = key.uppercase()
        upper == com.sanket.tools.nexpad.model.NexpadKeys.LB || upper == "L1" || upper == "LEFT"
    }
    val neonColor = if (isLeft) Color(0xFFA97CF0) else Color(0xFF00E5FF)
    val bumperShape = remember { androidx.compose.foundation.shape.RoundedCornerShape(27.dp) }

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

    val windowGradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF050506),
                Color(0xFF121314)
            ),
            center = Offset(0.50f, 0.60f),
            radius = 90f
        )
    }

    Box(
        modifier = modifier
            .size(154.dp, 54.dp)
            .drawBehind {
                val pad = 12.dp.toPx()
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            neonColor.copy(alpha = 0.45f * 0.48f),
                            neonColor.copy(alpha = 0.45f * 0.18f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.width * 0.65f
                    ),
                    topLeft = Offset(-pad, -pad),
                    size = Size(size.width + pad * 2, size.height + pad * 2),
                    cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx())
                )
            }
            .shadow(
                elevation = 4.dp,
                shape = bumperShape,
                ambientColor = neonColor,
                spotColor = neonColor
            )
            .clip(bumperShape)
            .background(baseDomeGradient)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.50f),
                        Color.Black.copy(alpha = 0.80f)
                    )
                ),
                shape = bumperShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Inset bottom shadow
            val insetH = h * 0.40f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.70f)),
                    startY = h - insetH,
                    endY = h
                ),
                topLeft = Offset(0f, h - insetH),
                size = Size(w, insetH)
            )

            // Neon color ring — inset 2.5dp with pill corner radius
            val ringInset = 2.5.dp.toPx()
            val ringRadius = 27.dp.toPx() - ringInset

            // Outer bloom pass
            drawRoundRect(
                color = neonColor.copy(alpha = 0.28f * 0.70f),
                topLeft = Offset(ringInset, ringInset),
                size = Size(w - ringInset * 2, h - ringInset * 2),
                cornerRadius = CornerRadius(ringRadius, ringRadius),
                style = Stroke(width = 3.5.dp.toPx())
            )

            // Core crisp stroke
            drawRoundRect(
                color = neonColor.copy(alpha = 0.70f),
                topLeft = Offset(ringInset, ringInset),
                size = Size(w - ringInset * 2, h - ringInset * 2),
                cornerRadius = CornerRadius(ringRadius, ringRadius),
                style = Stroke(width = 1.8.dp.toPx())
            )

            // Glass lens highlight
            val sheenH = h * 0.42f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.14f),
                        Color.White.copy(alpha = 0.04f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = sheenH
                ),
                topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                size = Size(w - 2.dp.toPx(), sheenH)
            )

            val specStartX = w * 0.15f
            val specEndX = w * 0.85f
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.45f),
                        Color.White.copy(alpha = 0.16f)
                    ),
                    startX = specStartX,
                    endX = specEndX
                ),
                start = Offset(specStartX, 4f),
                end = Offset(specEndX, 4f),
                strokeWidth = 1.5f
            )
        }

        // Oversized Recessed Magnifier Window (.lx-window)
        val windowShape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)
        Box(
            modifier = Modifier
                .size(124.dp, 36.dp)
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
                    color = Color.White.copy(alpha = 0.06f),
                    shape = windowShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val winW = size.width
                val winH = size.height

                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.92f), Color.Transparent),
                        startY = 0f,
                        endY = 8.dp.toPx()
                    ),
                    size = Size(winW, 8.dp.toPx())
                )

                drawRect(
                    brush = Brush.radialGradient(
                        0.0f to Color.Transparent,
                        0.28f to Color.Transparent,
                        1.0f to Color.Black.copy(alpha = 0.88f),
                        center = Offset(winW / 2f, winH / 2f),
                        radius = winW * 0.55f
                    )
                )

                drawLine(
                    color = Color.White.copy(alpha = 0.06f),
                    start = Offset(6.dp.toPx(), winH - 0.5f),
                    end = Offset(winW - 6.dp.toPx(), winH - 0.5f),
                    strokeWidth = 1.dp.toPx()
                )
            }

            Text(
                text = key,
                color = neonColor,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                letterSpacing = 3.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

/**
 * Static preview for Bumper D — Ribbed (LB / RB).
 * Exact visual match for ui/components/controller/RibbedBumper.kt in idle state.
 */
@Composable
internal fun StaticRibbedBumper(
    key: String = "LB",
    modifier: Modifier = Modifier
) {
    val isLeft = remember(key) {
        val upper = key.uppercase()
        upper == "LB" || upper == "L1" || upper == "LEFT"
    }

    val neonColor = if (isLeft) Color(0xFF3FD2FF) else Color(0xFFFF007F)
    val bumperShape = remember(isLeft) {
        if (isLeft) {
            androidx.compose.foundation.shape.RoundedCornerShape(topStart = 12.dp, topEnd = 27.dp, bottomEnd = 27.dp, bottomStart = 12.dp)
        } else {
            androidx.compose.foundation.shape.RoundedCornerShape(topStart = 27.dp, topEnd = 12.dp, bottomEnd = 12.dp, bottomStart = 27.dp)
        }
    }

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
            .drawBehind {
                val pad = 12.dp.toPx()
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            neonColor.copy(alpha = 0.45f * 0.48f),
                            neonColor.copy(alpha = 0.45f * 0.18f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.width * 0.65f
                    ),
                    topLeft = Offset(-pad, -pad),
                    size = Size(size.width + pad * 2, size.height + pad * 2),
                    cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx())
                )
            }
            .shadow(
                elevation = 4.dp,
                shape = bumperShape,
                ambientColor = neonColor,
                spotColor = neonColor
            )
            .clip(bumperShape)
            .background(baseDomeGradient)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.50f),
                        Color.Black.copy(alpha = 0.80f)
                    )
                ),
                shape = bumperShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val insetH = h * 0.40f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.70f)),
                    startY = h - insetH,
                    endY = h
                ),
                topLeft = Offset(0f, h - insetH),
                size = Size(w, insetH)
            )

            // Tactile Ribbed knurling
            val ribSpacing = 8.dp.toPx()
            val ribWidth = 2.dp.toPx()
            var curX = 6.dp.toPx()
            while (curX < w - 6.dp.toPx()) {
                drawLine(
                    color = Color.White.copy(alpha = 0.055f),
                    start = Offset(curX, 0f),
                    end = Offset(curX, h),
                    strokeWidth = ribWidth
                )
                curX += ribSpacing
            }

            // Neon color ring
            val ringInset = 2.5.dp.toPx()
            val ringROuter = 27.dp.toPx() - ringInset
            val ringRInner = 12.dp.toPx() - ringInset
            val ringPath = androidx.compose.ui.graphics.Path().apply {
                addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
                        rect = androidx.compose.ui.geometry.Rect(ringInset, ringInset, w - ringInset, h - ringInset),
                        topLeft = CornerRadius(if (isLeft) ringRInner else ringROuter),
                        topRight = CornerRadius(if (isLeft) ringROuter else ringRInner),
                        bottomRight = CornerRadius(if (isLeft) ringROuter else ringRInner),
                        bottomLeft = CornerRadius(if (isLeft) ringRInner else ringROuter)
                    )
                )
            }

            drawPath(
                path = ringPath,
                color = neonColor.copy(alpha = 0.28f * 0.70f),
                style = Stroke(width = 4.dp.toPx())
            )
            drawPath(
                path = ringPath,
                color = neonColor.copy(alpha = 0.70f * 0.70f),
                style = Stroke(width = 1.8.dp.toPx())
            )

            val sheenH = h * 0.40f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.14f),
                        Color.White.copy(alpha = 0.04f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = sheenH
                ),
                topLeft = Offset(ringInset, ringInset),
                size = Size(w - ringInset * 2f, sheenH)
            )

            val specStartX = if (isLeft) w * 0.15f else w * 0.12f
            val specEndX = if (isLeft) w * 0.88f else w * 0.85f
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.45f),
                        Color.White.copy(alpha = 0.16f)
                    ),
                    startX = specStartX,
                    endX = specEndX
                ),
                start = Offset(specStartX, 4f),
                end = Offset(specEndX, 4f),
                strokeWidth = 1.5f
            )

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

        // Elevated Recessed Magnifier Window
        val windowShape = androidx.compose.foundation.shape.RoundedCornerShape(15.dp)
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

                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.90f), Color.Transparent),
                        startY = 0f,
                        endY = 6.dp.toPx()
                    ),
                    size = Size(winW, 6.dp.toPx())
                )

                drawRect(
                    brush = Brush.radialGradient(
                        0.0f to Color.Transparent,
                        0.32f to Color.Transparent,
                        1.0f to Color.Black.copy(alpha = 0.82f),
                        center = Offset(winW / 2f, winH / 2f),
                        radius = winW * 0.50f
                    )
                )

                drawLine(
                    color = Color.White.copy(alpha = 0.05f),
                    start = Offset(4.dp.toPx(), winH - 0.5f),
                    end = Offset(winW - 4.dp.toPx(), winH - 0.5f),
                    strokeWidth = 1.dp.toPx()
                )
            }

            Text(
                text = key,
                color = neonColor,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                letterSpacing = 2.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        // Lower Neon Lightbar Accent Strip
        val stripShape = androidx.compose.foundation.shape.RoundedCornerShape(2.dp)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 9.dp)
                .size(width = 110.dp, height = 4.dp)
                .clip(stripShape)
                .background(neonColor.copy(alpha = 0.22f))
        )
    }
}

/**
 * Static preview for Bumper E — Underglow (LB / RB).
 * Exact visual match for ui/components/controller/UnderglowBumper.kt in idle state.
 */
@Composable
internal fun StaticUnderglowBumper(
    key: String = "LB",
    modifier: Modifier = Modifier
) {
    val isLeft = remember(key) {
        val upper = key.uppercase()
        upper == "LB" || upper == "L1" || upper == "LEFT"
    }

    val neonColor = if (isLeft) Color(0xFF5CF29A) else Color(0xFFFF5252)
    val bumperShape = remember(isLeft) {
        if (isLeft) {
            androidx.compose.foundation.shape.RoundedCornerShape(topStart = 12.dp, topEnd = 14.dp, bottomEnd = 27.dp, bottomStart = 20.dp)
        } else {
            androidx.compose.foundation.shape.RoundedCornerShape(topStart = 14.dp, topEnd = 12.dp, bottomEnd = 20.dp, bottomStart = 27.dp)
        }
    }

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

    Box(
        modifier = modifier
            .size(154.dp, 54.dp)
            .drawBehind {
                val pad = 12.dp.toPx()
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            neonColor.copy(alpha = 0.45f * 0.48f),
                            neonColor.copy(alpha = 0.45f * 0.18f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.width * 0.65f
                    ),
                    topLeft = Offset(-pad, -pad),
                    size = Size(size.width + pad * 2, size.height + pad * 2),
                    cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx())
                )
            }
            .shadow(
                elevation = 4.dp,
                shape = bumperShape,
                ambientColor = neonColor,
                spotColor = neonColor
            )
            .clip(bumperShape)
            .background(baseDomeGradient)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.50f),
                        Color.Black.copy(alpha = 0.80f)
                    )
                ),
                shape = bumperShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val insetH = h * 0.40f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.70f)),
                    startY = h - insetH,
                    endY = h
                ),
                topLeft = Offset(0f, h - insetH),
                size = Size(w, insetH)
            )

            // Top crescent specular sheen
            val sheenH = h * 0.42f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.14f),
                        Color.White.copy(alpha = 0.04f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = sheenH
                ),
                topLeft = Offset(2.dp.toPx(), 2.dp.toPx()),
                size = Size(w - 4.dp.toPx(), sheenH)
            )

            // Top edge specular line
            val specStartX = if (isLeft) w * 0.15f else w * 0.12f
            val specEndX = if (isLeft) w * 0.88f else w * 0.85f
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.45f),
                        Color.White.copy(alpha = 0.16f)
                    ),
                    startX = specStartX,
                    endX = specEndX
                ),
                start = Offset(specStartX, 3.5f),
                end = Offset(specEndX, 3.5f),
                strokeWidth = 1.5f
            )

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

        // Direct Centered Typography
        Text(
            text = key,
            color = neonColor,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            letterSpacing = 2.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        // Bottom Neon Underglow Bar
        Canvas(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxSize()
        ) {
            val barInsetX = 12.dp.toPx()
            val barBottomY = size.height - 3.dp.toPx()
            val barH = 3.dp.toPx()
            val barW = size.width - barInsetX * 2f

            drawRoundRect(
                color = neonColor.copy(alpha = 0.35f * 0.85f),
                topLeft = Offset(barInsetX - 4.dp.toPx(), barBottomY - 2.dp.toPx()),
                size = Size(barW + 8.dp.toPx(), barH + 4.dp.toPx()),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )

            drawRoundRect(
                color = neonColor.copy(alpha = 0.85f),
                topLeft = Offset(barInsetX, barBottomY),
                size = Size(barW, barH),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
        }
    }
}

/**
 * Static preview for Bumper F — Tube (LB / RB).
 * Exact visual match for ui/components/controller/TubeBumper.kt in idle state.
 */
@Composable
internal fun StaticTubeBumper(
    key: String = "LB",
    modifier: Modifier = Modifier
) {
    val isLeft = remember(key) {
        val upper = key.uppercase()
        upper == "LB" || upper == "L1" || upper == "LEFT"
    }

    val neonColor = if (isLeft) Color(0xFF4FA8FF) else Color(0xFFFF4F81)
    val bumperShape = remember { androidx.compose.foundation.shape.RoundedCornerShape(27.dp) }

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
            .drawBehind {
                val pad = 12.dp.toPx()
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            neonColor.copy(alpha = 0.45f * 0.48f),
                            neonColor.copy(alpha = 0.45f * 0.18f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.width * 0.65f
                    ),
                    topLeft = Offset(-pad, -pad),
                    size = Size(size.width + pad * 2, size.height + pad * 2),
                    cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx())
                )
            }
            .shadow(
                elevation = 4.dp,
                shape = bumperShape,
                ambientColor = neonColor,
                spotColor = neonColor
            )
            .clip(bumperShape)
            .background(baseDomeGradient)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.50f),
                        Color.Black.copy(alpha = 0.80f)
                    )
                ),
                shape = bumperShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val insetH = h * 0.40f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.70f)),
                    startY = h - insetH,
                    endY = h
                ),
                topLeft = Offset(0f, h - insetH),
                size = Size(w, insetH)
            )

            // Calibration ruler ticks along bottom
            val tickYStart = h - 9.dp.toPx()
            val tickYEnd = h - 3.dp.toPx()
            val tickSpacing = 10.dp.toPx()
            var curTickX = 16.dp.toPx()
            while (curTickX < w - 16.dp.toPx()) {
                drawLine(
                    color = Color.White.copy(alpha = 0.22f),
                    start = Offset(curTickX, tickYStart),
                    end = Offset(curTickX, tickYEnd),
                    strokeWidth = 1.dp.toPx()
                )
                curTickX += tickSpacing
            }

            // Neon perimeter ring
            val ringInset = 2.5.dp.toPx()
            val ringRadius = 27.dp.toPx() - ringInset

            drawRoundRect(
                color = neonColor.copy(alpha = 0.28f * 0.70f),
                topLeft = Offset(ringInset, ringInset),
                size = Size(w - ringInset * 2f, h - ringInset * 2f),
                cornerRadius = CornerRadius(ringRadius, ringRadius),
                style = Stroke(width = 4.dp.toPx())
            )
            drawRoundRect(
                color = neonColor.copy(alpha = 0.70f * 0.70f),
                topLeft = Offset(ringInset, ringInset),
                size = Size(w - ringInset * 2f, h - ringInset * 2f),
                cornerRadius = CornerRadius(ringRadius, ringRadius),
                style = Stroke(width = 1.8.dp.toPx())
            )

            // Top crescent specular sheen
            val sheenH = h * 0.40f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.14f),
                        Color.White.copy(alpha = 0.04f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = sheenH
                ),
                topLeft = Offset(ringInset, ringInset),
                size = Size(w - ringInset * 2f, sheenH)
            )

            val specStartX = w * 0.15f
            val specEndX = w * 0.85f
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.45f),
                        Color.White.copy(alpha = 0.16f)
                    ),
                    startX = specStartX,
                    endX = specEndX
                ),
                start = Offset(specStartX, 4f),
                end = Offset(specEndX, 4f),
                strokeWidth = 1.5f
            )

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

        // Recessed Optical Window
        val windowShape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
        Box(
            modifier = Modifier
                .size(74.dp, 28.dp)
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

                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.90f), Color.Transparent),
                        startY = 0f,
                        endY = 6.dp.toPx()
                    ),
                    size = Size(winW, 6.dp.toPx())
                )

                drawRect(
                    brush = Brush.radialGradient(
                        0.0f to Color.Transparent,
                        0.32f to Color.Transparent,
                        1.0f to Color.Black.copy(alpha = 0.82f),
                        center = Offset(winW / 2f, winH / 2f),
                        radius = winW * 0.50f
                    )
                )

                drawLine(
                    color = Color.White.copy(alpha = 0.05f),
                    start = Offset(4.dp.toPx(), winH - 0.5f),
                    end = Offset(winW - 4.dp.toPx(), winH - 0.5f),
                    strokeWidth = 1.dp.toPx()
                )
            }

            Text(
                text = key,
                color = neonColor,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                letterSpacing = 2.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

/**
 * Static preview for Bumper G — Flip (LB / RB).
 * Exact visual match for ui/components/controller/FlipBumper.kt in resting idle state (Face A).
 */
@Composable
internal fun StaticFlipBumper(
    key: String = "LB",
    modifier: Modifier = Modifier
) {
    val isLeft = remember(key) {
        val upper = key.uppercase()
        upper == "LB" || upper == "L1" || upper == "LEFT"
    }

    val neonColor = if (isLeft) Color(0xFFFFD23F) else Color(0xFFFF6B6B)
    val bumperShape = remember { androidx.compose.foundation.shape.RoundedCornerShape(27.dp) }

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
            .drawBehind {
                val pad = 12.dp.toPx()
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            neonColor.copy(alpha = 0.45f * 0.48f),
                            neonColor.copy(alpha = 0.45f * 0.18f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.width * 0.65f
                    ),
                    topLeft = Offset(-pad, -pad),
                    size = Size(size.width + pad * 2, size.height + pad * 2),
                    cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx())
                )
            }
            .shadow(
                elevation = 4.dp,
                shape = bumperShape,
                ambientColor = neonColor,
                spotColor = neonColor
            )
            .clip(bumperShape)
            .background(baseDomeGradient)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.50f),
                        Color.Black.copy(alpha = 0.80f)
                    )
                ),
                shape = bumperShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val insetH = h * 0.40f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.70f)),
                    startY = h - insetH,
                    endY = h
                ),
                topLeft = Offset(0f, h - insetH),
                size = Size(w, insetH)
            )

            // Top crescent specular sheen
            val sheenH = h * 0.40f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.14f),
                        Color.White.copy(alpha = 0.04f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = sheenH
                ),
                topLeft = Offset(2.dp.toPx(), 2.dp.toPx()),
                size = Size(w - 4.dp.toPx(), sheenH)
            )

            // Neon perimeter ring
            val ringInset = 2.5.dp.toPx()
            val ringRadius = 27.dp.toPx() - ringInset

            drawRoundRect(
                color = neonColor.copy(alpha = 0.28f * 0.70f),
                topLeft = Offset(ringInset, ringInset),
                size = Size(w - ringInset * 2f, h - ringInset * 2f),
                cornerRadius = CornerRadius(ringRadius, ringRadius),
                style = Stroke(width = 4.dp.toPx())
            )
            drawRoundRect(
                color = neonColor.copy(alpha = 0.70f * 0.70f),
                topLeft = Offset(ringInset, ringInset),
                size = Size(w - ringInset * 2f, h - ringInset * 2f),
                cornerRadius = CornerRadius(ringRadius, ringRadius),
                style = Stroke(width = 1.8.dp.toPx())
            )
        }

        // Optical Window (96dp × 32dp)
        val windowShape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
        Box(
            modifier = Modifier
                .size(96.dp, 32.dp)
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

                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.90f), Color.Transparent),
                        startY = 0f,
                        endY = 6.dp.toPx()
                    ),
                    size = Size(winW, 6.dp.toPx())
                )

                drawRect(
                    brush = Brush.radialGradient(
                        0.0f to Color.Transparent,
                        0.32f to Color.Transparent,
                        1.0f to Color.Black.copy(alpha = 0.82f),
                        center = Offset(winW / 2f, winH / 2f),
                        radius = winW * 0.50f
                    )
                )
            }

            Text(
                text = key,
                color = neonColor,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                letterSpacing = 2.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

/**
 * Exact static visual preview for the Dial Trigger (LT / RT).
 * Renders the circular 92dp x 92dp housing, inactive 270° radial gauge track with idle tip beacon,
 * recessed optical window, and specular optical lens sheens.
 */
@Composable
internal fun StaticDialTrigger(
    key: String,
    modifier: Modifier = Modifier
) {
    val isLeft = key.uppercase().startsWith("L")
    val neonColor = if (isLeft) Color(0xFF00E5FF) else Color(0xFFE055B8)

    val triggerShape = CircleShape
    val windowShape = RoundedCornerShape(13.dp)

    val baseDomeGradient = Brush.radialGradient(
        colors = listOf(
            Color(0xFF232527),
            Color(0xFF0C0D0E),
            Color(0xFF000000)
        ),
        center = Offset(0.50f, 0.45f),
        radius = 210f
    )

    val windowGradient = Brush.radialGradient(
        colors = listOf(
            Color(0xFF050506),
            Color(0xFF121314)
        ),
        center = Offset(0.50f, 0.60f),
        radius = 100f
    )

    Box(
        modifier = modifier
            .size(92.dp, 92.dp)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            neonColor.copy(alpha = 0.45f * 0.50f),
                            neonColor.copy(alpha = 0.45f * 0.20f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.minDimension * 0.95f
                    ),
                    radius = size.minDimension * 0.95f
                )
            }
            .shadow(
                elevation = 4.dp,
                shape = triggerShape,
                ambientColor = neonColor,
                spotColor = neonColor
            )
            .clip(triggerShape)
            .background(baseDomeGradient)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.50f),
                        Color.Black.copy(alpha = 0.80f)
                    )
                ),
                shape = triggerShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Inset bottom shadow along bottom base
            val insetH = h * 0.32f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.70f)),
                    startY = h - insetH,
                    endY = h
                ),
                topLeft = Offset(0f, h - insetH),
                size = Size(w, insetH)
            )

            // Outer 1px rim outline
            drawCircle(
                color = Color.Black.copy(alpha = 0.50f),
                radius = (w / 2f) - 0.5f,
                style = Stroke(width = 1.dp.toPx())
            )

            // Multi-pass emissive neon ring (.lx-ring)
            val ringInset = 3.dp.toPx()
            val ringRadius = (w / 2f) - ringInset
            drawCircle(
                color = neonColor.copy(alpha = 0.25f),
                radius = ringRadius,
                style = Stroke(width = 4.dp.toPx())
            )
            drawCircle(
                color = neonColor.copy(alpha = 0.70f),
                radius = ringRadius,
                style = Stroke(width = 2.dp.toPx())
            )

            // =========================================================================
            // 270° RADIAL GAUGE TRACK (HTML SVG Dial Gauge Translation)
            // =========================================================================
            val gaugeRadius = (w / 2f) - 10.dp.toPx()
            val gaugeCenter = Offset(w / 2f, h / 2f)
            val gaugeTopLeft = Offset(gaugeCenter.x - gaugeRadius, gaugeCenter.y - gaugeRadius)
            val gaugeSize = Size(gaugeRadius * 2f, gaugeRadius * 2f)

            // Inactive track: 270° arc starting at 135° (South-West) sweeping clockwise
            drawArc(
                color = Color.White.copy(alpha = 0.12f),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = gaugeTopLeft,
                size = gaugeSize,
                style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
            )

            // Subtle starting needle tip dot / beacon at 135° origin
            drawArc(
                color = neonColor,
                startAngle = 135f,
                sweepAngle = 2f,
                useCenter = false,
                topLeft = gaugeTopLeft,
                size = gaugeSize,
                style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
            )

            // Top crescent specular highlight arc (.lx-lens)
            val lensInset = 1.dp.toPx()
            drawArc(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.14f),
                        Color.White.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = h * 0.38f
                ),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(lensInset, lensInset),
                size = Size(w - lensInset * 2f, h - lensInset * 2f),
                style = Stroke(width = 1.dp.toPx())
            )

            // Bottom-right specular sheen oval
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.06f), Color.Transparent),
                    center = Offset(w * 0.70f, h * 0.78f),
                    radius = w * 0.22f
                ),
                center = Offset(w * 0.70f, h * 0.78f),
                radius = w * 0.22f
            )
        }

        // =========================================================================
        // RECESSED OPTICAL WINDOW (.lx-window)
        // =========================================================================
        Box(
            modifier = Modifier
                .offset(y = (-2).dp)
                .size(48.dp, 26.dp)
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
                    color = Color.White.copy(alpha = 0.06f),
                    shape = windowShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val winW = size.width
                val winH = size.height

                // Top lip shadow inside optical cavity
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent),
                        startY = 0f,
                        endY = 5.dp.toPx()
                    ),
                    size = Size(winW, 5.dp.toPx())
                )

                // Perimeter shadow vignette (.lx-window::after)
                drawRect(
                    brush = Brush.radialGradient(
                        0.0f to Color.Transparent,
                        0.32f to Color.Transparent,
                        1.0f to Color.Black.copy(alpha = 0.82f),
                        center = Offset(winW / 2f, winH / 2f),
                        radius = winW * 0.50f
                    )
                )
            }

            // High-contrast bold tactical label (LT / RT)
            Text(
                text = key,
                color = neonColor,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                letterSpacing = 1.5.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

/**
 * Exact static visual preview for the Liquid Orb Trigger (LT / RT).
 * Renders the circular 92dp x 92dp housing, recessed spherical chamber, ambient baseline
 * liquid level with glowing meniscus crest, elevated optical window, and specular glass reflections.
 */
@Composable
internal fun StaticLiquidOrbTrigger(
    key: String,
    modifier: Modifier = Modifier
) {
    val isLeft = key.uppercase().startsWith("L")
    val neonColor = if (isLeft) Color(0xFF3FD2C4) else Color(0xFFFF5376)

    val triggerShape = CircleShape
    val windowShape = RoundedCornerShape(13.dp)

    val baseDomeGradient = Brush.radialGradient(
        colors = listOf(
            Color(0xFF232527),
            Color(0xFF0C0D0E),
            Color(0xFF000000)
        ),
        center = Offset(0.50f, 0.45f),
        radius = 210f
    )

    val windowGradient = Brush.radialGradient(
        colors = listOf(
            Color(0xFF050506),
            Color(0xFF121314)
        ),
        center = Offset(0.50f, 0.60f),
        radius = 100f
    )

    Box(
        modifier = modifier
            .size(92.dp, 92.dp)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            neonColor.copy(alpha = 0.45f * 0.50f),
                            neonColor.copy(alpha = 0.45f * 0.20f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.minDimension * 0.95f
                    ),
                    radius = size.minDimension * 0.95f
                )
            }
            .shadow(
                elevation = 4.dp,
                shape = triggerShape,
                ambientColor = neonColor,
                spotColor = neonColor
            )
            .clip(triggerShape)
            .background(baseDomeGradient)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.50f),
                        Color.Black.copy(alpha = 0.80f)
                    )
                ),
                shape = triggerShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Inset bottom shadow along base
            val insetH = h * 0.32f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.70f)),
                    startY = h - insetH,
                    endY = h
                ),
                topLeft = Offset(0f, h - insetH),
                size = Size(w, insetH)
            )

            // Outer 1px rim outline
            drawCircle(
                color = Color.Black.copy(alpha = 0.50f),
                radius = (w / 2f) - 0.5f,
                style = Stroke(width = 1.dp.toPx())
            )

            // Multi-pass emissive neon ring (.lx-ring)
            val ringInset = 3.dp.toPx()
            val ringRadius = (w / 2f) - ringInset
            drawCircle(
                color = neonColor.copy(alpha = 0.25f),
                radius = ringRadius,
                style = Stroke(width = 4.dp.toPx())
            )
            drawCircle(
                color = neonColor.copy(alpha = 0.70f),
                radius = ringRadius,
                style = Stroke(width = 2.dp.toPx())
            )

            // Recessed spherical chamber (.liq-wrap)
            val chamberInset = 7.dp.toPx()
            val chamberRadius = (w / 2f) - chamberInset
            val chamberCenter = Offset(w / 2f, h / 2f)

            val chamberPath = Path().apply {
                addOval(
                    androidx.compose.ui.geometry.Rect(
                        center = chamberCenter,
                        radius = chamberRadius
                    )
                )
            }

            clipPath(chamberPath) {
                // Dark internal cavity well
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF0D0E12), Color(0xFF040405)),
                        center = chamberCenter,
                        radius = chamberRadius
                    ),
                    radius = chamberRadius,
                    center = chamberCenter
                )

                // Rich liquid fill level (45% fluid height for vivid Button Studio preview)
                val chamberDiameter = chamberRadius * 2f
                val liquidHeight = chamberDiameter * 0.45f
                val liquidTopY = (chamberCenter.y + chamberRadius) - liquidHeight
                val liquidBottomY = chamberCenter.y + chamberRadius

                // Layer 1: Ambient deep fluid bloom
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            neonColor.copy(alpha = 0.75f),
                            neonColor.copy(alpha = 0.35f),
                            Color.Transparent
                        ),
                        startY = liquidTopY,
                        endY = liquidBottomY
                    ),
                    topLeft = Offset(chamberCenter.x - chamberRadius, liquidTopY),
                    size = Size(chamberDiameter, liquidHeight)
                )

                // Layer 2: Core luminous fluid radiance
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            neonColor.copy(alpha = 0.50f),
                            Color.Transparent
                        ),
                        startY = liquidTopY,
                        endY = liquidTopY + liquidHeight * 0.60f
                    ),
                    topLeft = Offset(chamberCenter.x - chamberRadius, liquidTopY),
                    size = Size(chamberDiameter, liquidHeight * 0.60f)
                )

                // Meniscus glowing wave crest
                val meniscusWidth = chamberDiameter * 1.30f
                val meniscusHeight = 11.dp.toPx()
                val meniscusLeft = chamberCenter.x - (meniscusWidth / 2f)
                val meniscusTop = liquidTopY - (meniscusHeight / 2f)

                drawOval(
                    color = neonColor.copy(alpha = 0.50f),
                    topLeft = Offset(meniscusLeft, meniscusTop - 3.dp.toPx()),
                    size = Size(meniscusWidth, meniscusHeight + 6.dp.toPx())
                )
                drawOval(
                    color = neonColor.copy(alpha = 0.95f),
                    topLeft = Offset(meniscusLeft, meniscusTop),
                    size = Size(meniscusWidth, meniscusHeight)
                )

                // Meniscus specular wave crest glint
                val glintWidth = meniscusWidth * 0.50f
                val glintHeight = 3.5.dp.toPx()
                drawOval(
                    color = Color.White.copy(alpha = 0.85f),
                    topLeft = Offset(chamberCenter.x - (glintWidth / 2f), liquidTopY - (glintHeight / 2f)),
                    size = Size(glintWidth, glintHeight)
                )
            }

            // Top crescent specular highlight arc (.lx-lens)
            val lensInset = 1.dp.toPx()
            drawArc(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.14f),
                        Color.White.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = h * 0.38f
                ),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(lensInset, lensInset),
                size = Size(w - lensInset * 2f, h - lensInset * 2f),
                style = Stroke(width = 1.dp.toPx())
            )

            // Bottom-right specular sheen oval
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.06f), Color.Transparent),
                    center = Offset(w * 0.70f, h * 0.78f),
                    radius = w * 0.22f
                ),
                center = Offset(w * 0.70f, h * 0.78f),
                radius = w * 0.22f
            )
        }

        // Elevated optical window (.lx-window top: 34%)
        Box(
            modifier = Modifier
                .offset(y = (-13).dp)
                .size(48.dp, 26.dp)
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
                    color = Color.White.copy(alpha = 0.06f),
                    shape = windowShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val winW = size.width
                val winH = size.height

                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent),
                        startY = 0f,
                        endY = 5.dp.toPx()
                    ),
                    size = Size(winW, 5.dp.toPx())
                )

                drawRect(
                    brush = Brush.radialGradient(
                        0.0f to Color.Transparent,
                        0.32f to Color.Transparent,
                        1.0f to Color.Black.copy(alpha = 0.82f),
                        center = Offset(winW / 2f, winH / 2f),
                        radius = winW * 0.50f
                    )
                )
            }

            Text(
                text = key,
                color = neonColor,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                letterSpacing = 1.5.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

/**
 * Exact static visual preview for the VU Slabs Trigger (LT / RT).
 * Renders the 100dp x 92dp mobile trigger contour, 7 audio meter slabs with bottom 4 illuminated
 * and top overdrive slabs visible in ghost styling, top optical window, and lens reflections.
 */
@Composable
internal fun StaticVuSlabsTrigger(
    key: String,
    modifier: Modifier = Modifier
) {
    val isLeft = key.uppercase().startsWith("L")
    val neonColor = if (isLeft) Color(0xFF3FD25A) else Color(0xFFE055B8)

    val orangeOverdrive = Color(0xFFFF8A3D)
    val redOverdrive = Color(0xFFFF5A4D)

    val triggerShape = RoundedCornerShape(
        topStart = 22.dp,
        topEnd = 22.dp,
        bottomStart = 34.dp,
        bottomEnd = 34.dp
    )
    val windowShape = RoundedCornerShape(12.dp)

    val baseDomeGradient = Brush.radialGradient(
        colors = listOf(
            Color(0xFF232527),
            Color(0xFF0C0D0E),
            Color(0xFF000000)
        ),
        center = Offset(0.50f, 0.45f),
        radius = 210f
    )

    val windowGradient = Brush.radialGradient(
        colors = listOf(
            Color(0xFF050506),
            Color(0xFF121314)
        ),
        center = Offset(0.50f, 0.60f),
        radius = 100f
    )

    Box(
        modifier = modifier
            .size(100.dp, 92.dp)
            .drawBehind {
                val pad = 10.dp.toPx()
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            neonColor.copy(alpha = 0.45f * 0.50f),
                            neonColor.copy(alpha = 0.45f * 0.20f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.minDimension * 0.95f
                    ),
                    topLeft = Offset(-pad, -pad),
                    size = Size(size.width + pad * 2, size.height + pad * 2),
                    cornerRadius = CornerRadius(26.dp.toPx(), 26.dp.toPx())
                )
            }
            .shadow(
                elevation = 4.dp,
                shape = triggerShape,
                ambientColor = neonColor,
                spotColor = neonColor
            )
            .clip(triggerShape)
            .background(baseDomeGradient)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.50f),
                        Color.Black.copy(alpha = 0.80f)
                    )
                ),
                shape = triggerShape
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Inset bottom shadow along pedal hull
            val insetH = h * 0.32f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.70f)),
                    startY = h - insetH,
                    endY = h
                ),
                topLeft = Offset(0f, h - insetH),
                size = Size(w, insetH)
            )

            // Multi-pass emissive neon ring (.lx-ring) mapped accurately to 22dp/34dp hull
            val ringInset = 2.5.dp.toPx()
            val ringTopR = 19.5.dp.toPx()
            val ringBotR = 31.5.dp.toPx()
            val ringPath = Path().apply {
                addRoundRect(
                    RoundRect(
                        left = ringInset,
                        top = ringInset,
                        right = w - ringInset,
                        bottom = h - ringInset,
                        topLeftCornerRadius = CornerRadius(ringTopR, ringTopR),
                        topRightCornerRadius = CornerRadius(ringTopR, ringTopR),
                        bottomLeftCornerRadius = CornerRadius(ringBotR, ringBotR),
                        bottomRightCornerRadius = CornerRadius(ringBotR, ringBotR)
                    )
                )
            }

            // Outer atmospheric bloom stroke
            drawPath(
                path = ringPath,
                color = neonColor.copy(alpha = 0.28f),
                style = Stroke(width = 3.5.dp.toPx())
            )
            // Core crisp filament stroke
            drawPath(
                path = ringPath,
                color = neonColor.copy(alpha = 0.70f),
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Seven audio meter slabs (showcase preview: bottom 4 lit at 100%, top 3 ghosted)
            val slabWidths = floatArrayOf(
                54.dp.toPx(),
                58.dp.toPx(),
                62.dp.toPx(),
                65.dp.toPx(),
                68.dp.toPx(),
                70.dp.toPx(),
                72.dp.toPx()
            )
            val slabHeight = 4.dp.toPx()
            val slabGap = 2.5.dp.toPx()
            val slabRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            val bottomMargin = 10.dp.toPx()

            for (i in 0..6) {
                val slabColor = when (i) {
                    5 -> orangeOverdrive
                    6 -> redOverdrive
                    else -> neonColor
                }

                val isLit = i <= 3
                val slabOpacity = if (isLit) 1.0f else 0.13f

                val slabW = slabWidths[i]
                val slabLeft = (w - slabW) / 2f
                val slabTop = (h - bottomMargin - slabHeight) - i * (slabHeight + slabGap)

                if (isLit) {
                    drawRoundRect(
                        color = slabColor.copy(alpha = 0.40f),
                        topLeft = Offset(slabLeft - 2.dp.toPx(), slabTop - 1.5.dp.toPx()),
                        size = Size(slabW + 4.dp.toPx(), slabHeight + 3.dp.toPx()),
                        cornerRadius = CornerRadius(3.5.dp.toPx(), 3.5.dp.toPx())
                    )
                }

                drawRoundRect(
                    color = slabColor.copy(alpha = slabOpacity),
                    topLeft = Offset(slabLeft, slabTop),
                    size = Size(slabW, slabHeight),
                    cornerRadius = slabRadius
                )
            }

            // Top crescent specular highlight arc (.lx-lens)
            val lensInset = 1.dp.toPx()
            drawArc(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.14f),
                        Color.White.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = h * 0.38f
                ),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(lensInset, lensInset),
                size = Size(w - lensInset * 2f, h - lensInset * 2f),
                style = Stroke(width = 1.dp.toPx())
            )

            // Bottom-right specular sheen oval
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.06f), Color.Transparent),
                    center = Offset(w * 0.70f, h * 0.78f),
                    radius = w * 0.22f
                ),
                center = Offset(w * 0.70f, h * 0.78f),
                radius = w * 0.22f
            )
        }

        // Recessed optical window (.lx-window top: 10dp)
        Box(
            modifier = Modifier
                .offset(y = 10.dp)
                .size(54.dp, 24.dp)
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
                    color = Color.White.copy(alpha = 0.06f),
                    shape = windowShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val winW = size.width
                val winH = size.height

                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent),
                        startY = 0f,
                        endY = 5.dp.toPx()
                    ),
                    size = Size(winW, 5.dp.toPx())
                )

                drawRect(
                    brush = Brush.radialGradient(
                        0.0f to Color.Transparent,
                        0.32f to Color.Transparent,
                        1.0f to Color.Black.copy(alpha = 0.82f),
                        center = Offset(winW / 2f, winH / 2f),
                        radius = winW * 0.50f
                    )
                )
            }

            Text(
                text = key,
                color = neonColor,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                letterSpacing = 1.5.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

/**
 * Static preview renderer for the Target Trigger (Trigger E — Target / Concentric Rings).
 * Showcases the active state with outer 2 target rings ignited and inner ring primed for studio browsing.
 */
@Composable
internal fun StaticTargetTrigger(
    key: String,
    modifier: Modifier = Modifier
) {
    val isLeft = key.uppercase().startsWith("L")
    val neonColor = if (isLeft) Color(0xFF00E5FF) else Color(0xFFFF5A7A)

    val triggerShape = CircleShape
    val windowShape = CircleShape

    val baseDomeGradient = Brush.radialGradient(
        colors = listOf(
            Color(0xFF232527),
            Color(0xFF0C0D0E),
            Color(0xFF000000)
        ),
        center = Offset(0.50f, 0.45f),
        radius = 210f
    )

    val windowGradient = Brush.radialGradient(
        colors = listOf(
            Color(0xFF050506),
            Color(0xFF121314)
        ),
        center = Offset(0.50f, 0.60f),
        radius = 70f
    )

    Box(
        modifier = modifier
            .size(92.dp, 92.dp)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            neonColor.copy(alpha = 0.45f * 0.50f),
                            neonColor.copy(alpha = 0.45f * 0.20f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.minDimension * 0.95f
                    ),
                    radius = size.minDimension * 0.95f
                )
            }
            .shadow(
                elevation = 4.dp,
                shape = triggerShape,
                ambientColor = neonColor,
                spotColor = neonColor
            )
            .clip(triggerShape)
            .background(baseDomeGradient)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.50f),
                        Color.Black.copy(alpha = 0.80f)
                    )
                ),
                shape = triggerShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val center = Offset(w / 2f, h / 2f)
            val outerRadius = size.minDimension / 2f

            // Inset bottom shadow along trigger dome
            val insetH = h * 0.32f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.70f)),
                    startY = h - insetH,
                    endY = h
                ),
                topLeft = Offset(0f, h - insetH),
                size = Size(w, insetH)
            )

            // Outer chassis neon ring (.lx-ring)
            val ringRadius = outerRadius - 2.5.dp.toPx()
            drawCircle(
                color = neonColor.copy(alpha = 0.22f),
                center = center,
                radius = ringRadius,
                style = Stroke(width = 3.5.dp.toPx())
            )
            drawCircle(
                color = neonColor.copy(alpha = 0.70f),
                center = center,
                radius = ringRadius,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Three concentric target rings (showcase: outer two rings fully lit at 100%, inner primed at 40%)
            val targetRadii = floatArrayOf(
                37.dp.toPx(),
                28.dp.toPx(),
                19.dp.toPx()
            )

            for (i in 0..2) {
                val ringOpacity = when (i) {
                    0 -> 1.0f
                    1 -> 0.85f
                    else -> 0.25f
                }
                val r = targetRadii[i]

                if (ringOpacity > 0.25f) {
                    drawCircle(
                        color = neonColor.copy(alpha = 0.40f),
                        center = center,
                        radius = r,
                        style = Stroke(width = 4.dp.toPx())
                    )
                }

                drawCircle(
                    color = neonColor.copy(alpha = ringOpacity),
                    center = center,
                    radius = r,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }

            // Top crescent specular highlight arc (.lx-lens)
            val lensInset = 1.dp.toPx()
            drawArc(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.14f),
                        Color.White.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = h * 0.38f
                ),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(lensInset, lensInset),
                size = Size(w - lensInset * 2f, h - lensInset * 2f),
                style = Stroke(width = 1.dp.toPx())
            )

            // Bottom-right specular sheen oval
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.06f), Color.Transparent),
                    center = Offset(w * 0.70f, h * 0.78f),
                    radius = w * 0.22f
                ),
                center = Offset(w * 0.70f, h * 0.78f),
                radius = w * 0.22f
            )
        }

        // Recessed circular optical eye window (32dp x 32dp CircleShape)
        Box(
            modifier = Modifier
                .size(32.dp)
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
                    color = Color.White.copy(alpha = 0.06f),
                    shape = windowShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val winW = size.width
                val winH = size.height

                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent),
                        startY = 0f,
                        endY = 5.dp.toPx()
                    ),
                    size = Size(winW, 5.dp.toPx())
                )

                drawRect(
                    brush = Brush.radialGradient(
                        0.0f to Color.Transparent,
                        0.38f to Color.Transparent,
                        1.0f to Color.Black.copy(alpha = 0.85f),
                        center = Offset(winW / 2f, winH / 2f),
                        radius = winW * 0.50f
                    )
                )
            }

            Text(
                text = key,
                color = neonColor,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                letterSpacing = 0.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

/**
 * Static preview renderer for the Slider Trigger (Trigger F — Slider).
 * Showcases the slider partially pulled (50% active state) with illuminated active track,
 * central glowing puck handle with neon ring and LED dot, and optical window.
 */
@Composable
internal fun StaticSliderTrigger(
    key: String,
    modifier: Modifier = Modifier
) {
    val isLeft = key.uppercase().startsWith("L")
    val glowColor = if (isLeft) Color(0xFFFFB13F) else Color(0xFFFF5A7A)

    val widthDp = 42.dp
    val totalHeightDp = 94.dp
    val chassisShape = RoundedCornerShape(21.dp)
    val windowShape = RoundedCornerShape(7.dp)

    val puckDiameterDp = 26.dp
    val puckRadiusDp = puckDiameterDp / 2f
    val windowHeightDp = 14.dp
    val windowWidthDp = 30.dp
    val edgeMarginDp = 7.dp
    val windowClearanceDp = 6.dp

    val topRestCenterDp = 9.dp + puckRadiusDp
    val bottomRestCenterDp = totalHeightDp - edgeMarginDp - windowHeightDp - windowClearanceDp - puckRadiusDp
    val travelSpanDp = bottomRestCenterDp - topRestCenterDp
    val previewFill = 0.50f

    val baseDomeGradient = Brush.radialGradient(
        colors = listOf(
            Color(0xFF232527),
            Color(0xFF0C0D0E),
            Color(0xFF000000)
        ),
        center = Offset(0.50f, 0.55f),
        radius = 200f
    )

    Box(
        modifier = modifier
            .size(widthDp, totalHeightDp)
            .drawBehind {
                val pad = 10.dp.toPx()
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glowColor.copy(alpha = 0.45f * 0.48f),
                            glowColor.copy(alpha = 0.45f * 0.18f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.minDimension * 0.95f
                    ),
                    topLeft = Offset(-pad, -pad),
                    size = Size(size.width + pad * 2, size.height + pad * 2),
                    cornerRadius = CornerRadius(21.dp.toPx() + pad, 21.dp.toPx() + pad)
                )
            }
            .shadow(5.dp, chassisShape, ambientColor = glowColor, spotColor = glowColor)
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
    ) {
        // Track & Glowing active fill
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2f
            val topPx = topRestCenterDp.toPx()
            val spanPx = travelSpanDp.toPx()
            val trackWidthPx = 5.dp.toPx()
            val trackRadiusPx = 2.5.dp.toPx()

            // Outer recessed track
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

            // Glowing fill up to 50%
            val puckCenterY = topPx + previewFill * spanPx
            val fillHeight = puckCenterY - topPx

            // Halo bloom
            drawRoundRect(
                color = glowColor.copy(alpha = 0.45f),
                topLeft = Offset(centerX - trackWidthPx / 2f - 2.dp.toPx(), topPx),
                size = Size(trackWidthPx + 4.dp.toPx(), fillHeight),
                cornerRadius = CornerRadius(trackRadiusPx, trackRadiusPx)
            )
            // Core
            drawRoundRect(
                color = glowColor,
                topLeft = Offset(centerX - trackWidthPx / 2f, topPx),
                size = Size(trackWidthPx, fillHeight),
                cornerRadius = CornerRadius(trackRadiusPx, trackRadiusPx)
            )
        }

        // Sliding puck at 50%
        val puckCenterDp = topRestCenterDp + travelSpanDp * previewFill
        Box(
            modifier = Modifier
                .size(puckDiameterDp)
                .offset {
                    IntOffset(
                        x = ((widthDp - puckDiameterDp) / 2f).roundToPx(),
                        y = (puckCenterDp - puckRadiusDp).roundToPx()
                    )
                }
                .shadow(5.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.40f), spotColor = Color.Black.copy(alpha = 0.55f))
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

                // Lens arc highlight
                drawArc(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.22f), Color.Transparent)
                    ),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(1.5.dp.toPx(), 1.5.dp.toPx()),
                    size = Size(size.width - 3.dp.toPx(), size.height - 3.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx())
                )

                // Puck Neon Ring
                val ringRadius = r - 2.5.dp.toPx()
                drawCircle(
                    color = glowColor.copy(alpha = 0.40f),
                    radius = ringRadius,
                    center = center,
                    style = Stroke(width = 2.2.dp.toPx())
                )
                drawCircle(
                    color = glowColor.copy(alpha = 0.85f),
                    radius = ringRadius,
                    center = center,
                    style = Stroke(width = 1.2.dp.toPx())
                )

                // Central LED dot
                val dotRadius = 2.5.dp.toPx()
                drawCircle(color = glowColor.copy(alpha = 0.50f), radius = dotRadius + 2.dp.toPx(), center = center)
                drawCircle(color = Color.White.copy(alpha = 0.90f), radius = dotRadius * 0.65f, center = center)
                drawCircle(color = glowColor, radius = dotRadius, center = center, style = Stroke(width = 0.8.dp.toPx()))
            }
        }

        // Optical Window
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = -edgeMarginDp)
                .size(windowWidthDp, windowHeightDp)
                .clip(windowShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF050506), Color(0xFF121314)),
                        center = Offset(0.50f, 0.60f),
                        radius = 50f
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.9f), Color.White.copy(alpha = 0.08f))
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
                    cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx())
                )
            }

            Text(
                text = key,
                color = glowColor,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        // Outer chassis neon ring and lens
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

            // Outer chassis neon ring
            val ringInset = 2.dp.toPx()
            val ringCorner = rPx - ringInset
            drawRoundRect(
                color = glowColor.copy(alpha = 0.25f),
                topLeft = Offset(ringInset - 1f, ringInset - 1f),
                size = Size(w - (ringInset - 1f) * 2f, h - (ringInset - 1f) * 2f),
                cornerRadius = CornerRadius(ringCorner, ringCorner),
                style = Stroke(width = 2.5.dp.toPx())
            )
            drawRoundRect(
                color = glowColor.copy(alpha = 0.70f),
                topLeft = Offset(ringInset, ringInset),
                size = Size(w - ringInset * 2f, h - ringInset * 2f),
                cornerRadius = CornerRadius(ringCorner, ringCorner),
                style = Stroke(width = 1.2.dp.toPx())
            )
        }
    }
}

/**
 * Static preview renderer for the Needle Meter Trigger (Trigger G — Needle).
 * Showcases the analog meter at 50% pull (needle pointing straight up) with illuminated progress arc,
 * radial graduation ticks, and recessed optical window.
 */
@Composable
internal fun StaticNeedleTrigger(
    key: String,
    modifier: Modifier = Modifier
) {
    val isLeft = key.uppercase().startsWith("L")
    val glowColor = if (isLeft) Color(0xFF5CF29A) else Color(0xFFFF5C8A)

    val widthDp = 108.dp
    val heightDp = 94.dp
    val chassisShape = RoundedCornerShape(topStart = 54.dp, topEnd = 54.dp, bottomStart = 14.dp, bottomEnd = 14.dp)
    val windowShape = RoundedCornerShape(10.dp)
    val previewFill = 0.50f

    val baseDomeGradient = Brush.radialGradient(
        colors = listOf(
            Color(0xFF232527),
            Color(0xFF0C0D0E),
            Color(0xFF000000)
        ),
        center = Offset(0.50f, 0.45f),
        radius = 220f
    )

    Box(
        modifier = modifier
            .size(widthDp, heightDp)
            .drawBehind {
                val pad = 10.dp.toPx()
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glowColor.copy(alpha = 0.45f * 0.48f),
                            glowColor.copy(alpha = 0.45f * 0.18f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.minDimension * 0.95f
                    ),
                    topLeft = Offset(-pad, -pad),
                    size = Size(size.width + pad * 2, size.height + pad * 2),
                    cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx())
                )
            }
            .shadow(5.dp, chassisShape, ambientColor = glowColor, spotColor = glowColor)
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
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val pivotX = w / 2f
            val pivotY = h - 28.dp.toPx()
            val arcRadius = 40.dp.toPx()
            val ticksRadius = 46.dp.toPx()
            val startAngle = 200f
            val sweepTotal = 140f

            // Graduation Ticks Arc
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

            // Background Track Arc
            drawArc(
                color = Color.White.copy(alpha = 0.10f),
                startAngle = startAngle,
                sweepAngle = sweepTotal,
                useCenter = false,
                topLeft = Offset(pivotX - arcRadius, pivotY - arcRadius),
                size = Size(arcRadius * 2f, arcRadius * 2f),
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )

            // Active Glowing Progress Arc (50%)
            val activeSweep = sweepTotal * previewFill
            drawArc(
                color = glowColor.copy(alpha = 0.45f),
                startAngle = startAngle,
                sweepAngle = activeSweep,
                useCenter = false,
                topLeft = Offset(pivotX - arcRadius, pivotY - arcRadius),
                size = Size(arcRadius * 2f, arcRadius * 2f),
                style = Stroke(width = 5.5.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                color = glowColor,
                startAngle = startAngle,
                sweepAngle = activeSweep,
                useCenter = false,
                topLeft = Offset(pivotX - arcRadius, pivotY - arcRadius),
                size = Size(arcRadius * 2f, arcRadius * 2f),
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )

            // Needle at 50% (pointing straight up at 0°)
            val needleAngleDeg = -70f + previewFill * 140f
            val needleLength = arcRadius + 4.dp.toPx()
            val needleWidth = 3.dp.toPx()

            rotate(degrees = needleAngleDeg, pivot = Offset(pivotX, pivotY)) {
                drawLine(
                    color = glowColor.copy(alpha = 0.65f),
                    start = Offset(pivotX, pivotY),
                    end = Offset(pivotX, pivotY - needleLength),
                    strokeWidth = needleWidth + 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
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

            // Pivot Center Hub
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
                color = glowColor,
                radius = 2.2.dp.toPx(),
                center = Offset(pivotX, pivotY)
            )
        }

        // Recessed Optical Window
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-6).dp)
                .size(48.dp, 20.dp)
                .clip(windowShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF050506), Color(0xFF121314)),
                        center = Offset(0.50f, 0.60f),
                        radius = 60f
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.9f), Color.White.copy(alpha = 0.08f))
                    ),
                    shape = windowShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = key,
                color = glowColor,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        // Lens & Outer Chassis Ring
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.04f), Color.Transparent),
                    startY = 0f,
                    endY = h * 0.35f
                ),
                topLeft = Offset(1.5.dp.toPx(), 1.5.dp.toPx()),
                size = Size(w - 3.dp.toPx(), h * 0.35f),
                cornerRadius = CornerRadius(54.dp.toPx(), 54.dp.toPx())
            )

            val ringInset = 2.dp.toPx()
            drawRoundRect(
                color = glowColor.copy(alpha = 0.30f),
                topLeft = Offset(ringInset - 1f, ringInset - 1f),
                size = Size(w - (ringInset - 1f) * 2f, h - (ringInset - 1f) * 2f),
                cornerRadius = CornerRadius(52.dp.toPx(), 52.dp.toPx()),
                style = Stroke(width = 2.8.dp.toPx())
            )
            drawRoundRect(
                color = glowColor.copy(alpha = 0.75f),
                topLeft = Offset(ringInset, ringInset),
                size = Size(w - ringInset * 2f, h - ringInset * 2f),
                cornerRadius = CornerRadius(52.dp.toPx(), 52.dp.toPx()),
                style = Stroke(width = 1.3.dp.toPx())
            )
        }
    }
}

/**
 * Static preview renderer for the Test Tube Trigger (Trigger H — Test Tube).
 * Showcases the glass tube with liquid filled to 50%, meniscus crest, bubbles, and graduation scale.
 */
@Composable
internal fun StaticTestTubeTrigger(
    key: String,
    modifier: Modifier = Modifier
) {
    val isLeft = key.uppercase().startsWith("L")
    val glowColor = if (isLeft) Color(0xFFFF9F43) else Color(0xFFBD5CFF)

    val widthDp = 48.dp
    val heightDp = 98.dp
    val chassisShape = RoundedCornerShape(24.dp)
    val chamberShape = RoundedCornerShape(19.dp)
    val windowShape = RoundedCornerShape(10.dp)
    val previewFill = 0.50f

    val baseDomeGradient = Brush.radialGradient(
        colors = listOf(
            Color(0xFF232527),
            Color(0xFF0C0D0E),
            Color(0xFF000000)
        ),
        center = Offset(0.50f, 0.45f),
        radius = 200f
    )

    Box(
        modifier = modifier
            .size(widthDp, heightDp)
            .drawBehind {
                val pad = 10.dp.toPx()
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glowColor.copy(alpha = 0.45f * 0.48f),
                            glowColor.copy(alpha = 0.45f * 0.18f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.minDimension * 0.95f
                    ),
                    topLeft = Offset(-pad, -pad),
                    size = Size(size.width + pad * 2, size.height + pad * 2),
                    cornerRadius = CornerRadius(24.dp.toPx() + pad, 24.dp.toPx() + pad)
                )
            }
            .shadow(5.dp, chassisShape, ambientColor = glowColor, spotColor = glowColor)
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
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(5.dp)
                .clip(chamberShape)
                .background(Color(0xFF07080A))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val liquidHeight = h * previewFill
                val liquidTopY = h - liquidHeight

                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(glowColor.copy(alpha = 0.55f), glowColor.copy(alpha = 0.85f)),
                        startY = liquidTopY,
                        endY = h
                    ),
                    topLeft = Offset(0f, liquidTopY),
                    size = Size(w, liquidHeight)
                )

                val meniscusHeight = 6.dp.toPx()
                drawOval(
                    color = glowColor,
                    topLeft = Offset(-w * 0.15f, liquidTopY - meniscusHeight / 2f),
                    size = Size(w * 1.3f, meniscusHeight)
                )
                drawOval(
                    color = Color.White.copy(alpha = 0.70f),
                    topLeft = Offset(w * 0.20f, liquidTopY - meniscusHeight * 0.35f),
                    size = Size(w * 0.6f, meniscusHeight * 0.6f)
                )

                // Static sample bubbles
                val bubbleOffsets = listOf(Pair(0.30f, 0.35f), Pair(0.60f, 0.65f), Pair(0.45f, 0.80f))
                for (b in bubbleOffsets) {
                    val bX = w * b.first
                    val bY = liquidTopY + (h - liquidTopY) * b.second
                    drawCircle(
                        color = Color.White.copy(alpha = 0.75f),
                        radius = 2.dp.toPx(),
                        center = Offset(bX, bY),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }

                // Volumetric ticks on right edge
                val numTicks = 6
                val scaleStartX = w - 8.dp.toPx()
                val scaleEndX = w - 3.dp.toPx()
                for (i in 0 until numTicks) {
                    val tickY = 16.dp.toPx() + i * ((h - 32.dp.toPx()) / (numTicks - 1))
                    drawLine(
                        color = Color.White.copy(alpha = 0.35f),
                        start = Offset(scaleStartX, tickY),
                        end = Offset(scaleEndX, tickY),
                        strokeWidth = 1.2.dp.toPx()
                    )
                }

                // Gloss strip on left edge
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0.08f), Color.Transparent)
                    ),
                    topLeft = Offset(3.dp.toPx(), 8.dp.toPx()),
                    size = Size(3.dp.toPx(), h - 16.dp.toPx()),
                    cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
                )
            }
        }

        // Optical Window centered
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(36.dp, 20.dp)
                .clip(windowShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF050506), Color(0xFF121314)),
                        center = Offset(0.50f, 0.60f),
                        radius = 40f
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.9f), Color.White.copy(alpha = 0.10f))
                    ),
                    shape = windowShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = key,
                color = glowColor,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        // Outer chassis neon ring and lens
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val rPx = 24.dp.toPx()

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.04f), Color.Transparent),
                    startY = 0f,
                    endY = h * 0.30f
                ),
                topLeft = Offset(1.5.dp.toPx(), 1.5.dp.toPx()),
                size = Size(w - 3.dp.toPx(), h * 0.30f),
                cornerRadius = CornerRadius(rPx, rPx)
            )

            val ringInset = 2.dp.toPx()
            val ringCorner = rPx - ringInset
            drawRoundRect(
                color = glowColor.copy(alpha = 0.30f),
                topLeft = Offset(ringInset - 1f, ringInset - 1f),
                size = Size(w - (ringInset - 1f) * 2f, h - (ringInset - 1f) * 2f),
                cornerRadius = CornerRadius(ringCorner, ringCorner),
                style = Stroke(width = 2.6.dp.toPx())
            )
            drawRoundRect(
                color = glowColor.copy(alpha = 0.75f),
                topLeft = Offset(ringInset, ringInset),
                size = Size(w - ringInset * 2f, h - ringInset * 2f),
                cornerRadius = CornerRadius(ringCorner, ringCorner),
                style = Stroke(width = 1.2.dp.toPx())
            )
        }
    }
}

/**
 * Static preview renderer for the Bloom Trigger (Trigger I — Bloom).
 * Showcases the circular trigger with expanding light bloom at 50% radius and illuminated halo rim.
 */
@Composable
internal fun StaticBloomTrigger(
    key: String,
    modifier: Modifier = Modifier
) {
    val isLeft = key.uppercase().startsWith("L")
    val glowColor = if (isLeft) Color(0xFF7C9CFF) else Color(0xFFFF6584)
    val buttonSize = 92.dp
    val previewFill = 0.50f

    val baseDomeGradient = Brush.radialGradient(
        colors = listOf(
            Color(0xFF232527),
            Color(0xFF0C0D0E),
            Color(0xFF000000)
        ),
        center = Offset(0.50f, 0.45f),
        radius = 200f
    )

    Box(
        modifier = modifier
            .size(buttonSize)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glowColor.copy(alpha = 0.45f * 0.50f),
                            glowColor.copy(alpha = 0.45f * 0.20f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.minDimension * 0.95f
                    ),
                    radius = size.minDimension * 0.95f
                )
            }
            .shadow(5.dp, CircleShape, ambientColor = glowColor, spotColor = glowColor)
            .clip(CircleShape)
            .background(baseDomeGradient)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.50f),
                        Color.Black.copy(alpha = 0.85f)
                    )
                ),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = size.minDimension / 2f - 4.dp.toPx()
            val bloomRadius = previewFill * maxRadius

            // Radiant Light Bloom
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.85f),
                        glowColor.copy(alpha = 0.65f),
                        glowColor.copy(alpha = 0.25f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = bloomRadius + 8.dp.toPx()
                ),
                radius = bloomRadius + 8.dp.toPx(),
                center = center
            )

            // Expanding Bloom Rim Halo
            val rimRadius = previewFill * (maxRadius - 2.dp.toPx())
            drawCircle(
                color = glowColor.copy(alpha = 0.55f),
                radius = rimRadius,
                center = center,
                style = Stroke(width = 4.dp.toPx())
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                radius = rimRadius,
                center = center,
                style = Stroke(width = 1.8.dp.toPx())
            )
        }

        // Central Recessed Optical Eye Window
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF050506), Color(0xFF121314)),
                        center = Offset(0.50f, 0.60f),
                        radius = 45f
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.9f), Color.White.copy(alpha = 0.12f))
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = key,
                color = glowColor,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        // Lens & Outer Chassis Ring
        Canvas(modifier = Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            drawArc(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.04f), Color.Transparent),
                    startY = 0f,
                    endY = size.height * 0.38f
                ),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(1.5.dp.toPx(), 1.5.dp.toPx()),
                size = Size(size.width - 3.dp.toPx(), size.height - 3.dp.toPx()),
                style = Stroke(width = 1.2.dp.toPx())
            )

            val ringRadius = r - 2.5.dp.toPx()
            drawCircle(
                color = glowColor.copy(alpha = 0.30f),
                radius = ringRadius,
                center = center,
                style = Stroke(width = 2.8.dp.toPx())
            )
            drawCircle(
                color = glowColor.copy(alpha = 0.75f),
                radius = ringRadius,
                center = center,
                style = Stroke(width = 1.3.dp.toPx())
            )
        }
    }
}










