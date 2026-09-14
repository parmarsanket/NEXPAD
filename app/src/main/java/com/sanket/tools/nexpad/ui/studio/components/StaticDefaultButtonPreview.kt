package com.sanket.tools.nexpad.ui.studio.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.ui.theme.NeonPalette

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
    modifier: Modifier = Modifier
) {
    val key = controlKey.uppercase()

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        when (key) {
            "A" -> StaticRealisticButton(key = "A", buttonColor = Color(0xFF00C853))
            "B" -> StaticRealisticButton(key = "B", buttonColor = Color(0xFFD50000))
            "X" -> StaticRealisticButton(key = "X", buttonColor = Color(0xFF2962FF))
            "Y" -> StaticRealisticButton(key = "Y", buttonColor = Color(0xFFFFD600))

            "LS", "L3" -> StaticRealisticJoystick(isLeft = true)
            "RS", "R3" -> StaticRealisticJoystick(isLeft = false)

            "DPAD" -> StaticRealisticDPad()
            "UP", "DOWN", "LEFT", "RIGHT" -> StaticRealisticDPadButton(direction = key)

            "LT" -> StaticRealisticTrigger(key = "LT", isLeft = true)
            "RT" -> StaticRealisticTrigger(key = "RT", isLeft = false)

            "LB" -> StaticRealisticBumper(key = "LB", isLeft = true)
            "RB" -> StaticRealisticBumper(key = "RB", isLeft = false)

            "XBOX" -> StaticRealisticSystemButton(label = "⨂", textColor = NeonPalette.Cyan)
            "MENU" -> StaticRealisticSystemButton(label = "☰", textColor = Color.White)
            "VIEW" -> StaticRealisticSystemButton(label = "⧉", textColor = Color.White)
            "SHARE", "SCREENSHOT" -> StaticRealisticSystemButton(label = "⇪", textColor = Color.White)

            "M1", "M2", "M3", "M4", "PROFILE", "TURBO" -> StaticRealisticMacroButton(label = key)

            else -> StaticRealisticButton(key = key.take(3), buttonColor = Color.Gray)
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
    val baseGradient = Brush.radialGradient(
        colors = listOf(
            Color(0xFF3A3A3A),
            Color(0xFF151515)
        ),
        center = Offset(0.3f, 0.3f),
        radius = 150f
    )

    val rgbShadow = Modifier.shadow(
        elevation = 10.dp,
        shape = CircleShape,
        ambientColor = buttonColor,
        spotColor = buttonColor
    )

    Box(
        modifier = modifier
            .size(80.dp)
            .then(rgbShadow)
            .clip(CircleShape)
            .background(baseGradient),
        contentAlignment = Alignment.Center
    ) {
        // Draw the authentic glossy 3D highlight from RealisticButton
        Canvas(modifier = Modifier.fillMaxSize().padding(2.dp)) {
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(Color.White.copy(alpha = 0.4f), Color.Transparent),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height * 0.5f)
                ),
                radius = size.minDimension / 2f,
                style = Stroke(width = 4f)
            )
        }

        // Colored Text for the button (e.g. A, B, X, Y)
        Text(
            text = key,
            color = buttonColor,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )
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

    val shadow = Modifier.shadow(12.dp, CircleShape, ambientColor = NeonPalette.Cyan.copy(alpha = 0.5f), spotColor = NeonPalette.Cyan)

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
                text = if (isLeft) "LS" else "RS",
                color = Color.LightGray.copy(alpha = 0.7f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
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
    val shadow = Modifier.shadow(12.dp, crossShape, ambientColor = Color(0xFF4ADE80), spotColor = Color(0xFF00F0FF))

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
    val dirSymbol = when (direction.uppercase()) {
        "UP" -> "▲"
        "DOWN" -> "▼"
        "LEFT" -> "◀"
        "RIGHT" -> "▶"
        else -> direction
    }

    val themeColor = Color(0xFF00F0FF)
    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .size(72.dp)
            .shadow(6.dp, shape, ambientColor = themeColor, spotColor = themeColor)
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
 * Exact visual match for ui/components/controller/RealisticTrigger.kt
 */
@Composable
private fun StaticRealisticTrigger(
    key: String,
    isLeft: Boolean,
    modifier: Modifier = Modifier
) {
    val triggerShape = if (isLeft) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 8.dp, bottomStart = 50.dp, bottomEnd = 24.dp)
    } else {
        RoundedCornerShape(topStart = 8.dp, topEnd = 16.dp, bottomStart = 24.dp, bottomEnd = 50.dp)
    }

    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF262626), Color(0xFF0A0A0A)),
        startY = 0f,
        endY = 200f
    )

    val spot = if (isLeft) Color(0xFF00E5FF) else Color(0xFFFF0055)
    val ambient = if (isLeft) Color(0xFF0052CC) else Color(0xFFCC0044)

    Box(
        modifier = modifier
            .size(100.dp, 160.dp)
            .shadow(12.dp, triggerShape, spotColor = spot, ambientColor = ambient)
            .clip(triggerShape)
            .background(gradient),
        contentAlignment = Alignment.BottomCenter
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                color = Color.Black.copy(alpha = 0.5f),
                size = Size(size.width, size.height / 2),
                topLeft = Offset(0f, size.height / 2)
            )
        }
        val labelColor = if (isLeft) Color(0xFF00E5FF).copy(alpha = 0.85f) else Color(0xFFFF0055).copy(alpha = 0.85f)
        Text(
            text = key,
            color = labelColor,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            modifier = Modifier.padding(bottom = 20.dp)
        )
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
        RoundedCornerShape(topStart = 40.dp, topEnd = 14.dp, bottomStart = 14.dp, bottomEnd = 8.dp)
    } else {
        RoundedCornerShape(topStart = 14.dp, topEnd = 40.dp, bottomStart = 8.dp, bottomEnd = 14.dp)
    }

    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF424242), Color(0xFF1A1A1A)),
        startY = 0f,
        endY = 100f
    )

    val spot = if (isLeft) Color(0xFF7C3AED) else Color(0xFF00E5FF)
    val ambient = if (isLeft) Color(0xFFA78BFA) else Color(0xFF10B981)

    Box(
        modifier = modifier
            .size(160.dp, 60.dp)
            .shadow(8.dp, bumperShape, spotColor = spot, ambientColor = ambient)
            .clip(bumperShape)
            .background(gradient),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
            val startX = if (isLeft) 24f else 10f
            val endX = if (isLeft) size.width - 10f else size.width - 24f
            drawLine(
                color = Color.White.copy(alpha = 0.25f),
                start = Offset(startX, 10f),
                end = Offset(endX, 10f),
                strokeWidth = 2f
            )
        }
        val labelColor = if (isLeft) Color(0xFFA78BFA) else Color(0xFF00E5FF)
        Text(
            text = key,
            color = labelColor,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
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
    Box(
        modifier = modifier
            .size(60.dp)
            .shadow(6.dp, CircleShape, ambientColor = Color.White, spotColor = Color.White)
            .clip(CircleShape)
            .background(Color(0xFF2B2B2B)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
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

    Box(
        modifier = modifier
            .size(80.dp, 40.dp)
            .shadow(6.dp, RoundedCornerShape(16.dp), spotColor = Color.Yellow)
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
