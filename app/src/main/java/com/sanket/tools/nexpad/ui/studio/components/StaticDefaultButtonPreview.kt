package com.sanket.tools.nexpad.ui.studio.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.ui.theme.NeonPalette

/**
 * High-performance, zero-overhead static preview for default controller elements.
 * Renders an authentic console-grade visual snapshot with zero touch listeners,
 * zero animations, and zero ViewModels for butter-smooth list scrolling.
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
            "A" -> StaticFaceButton(label = "A", color = Color(0xFF00C853))
            "B" -> StaticFaceButton(label = "B", color = Color(0xFFD50000))
            "X" -> StaticFaceButton(label = "X", color = Color(0xFF2962FF))
            "Y" -> StaticFaceButton(label = "Y", color = Color(0xFFFFD600))

            "LS", "L3" -> StaticJoystickPreview(isLeft = true)
            "RS", "R3" -> StaticJoystickPreview(isLeft = false)

            "DPAD" -> StaticDPadCrossPreview()
            "UP" -> StaticDPadDirectionPreview(direction = "UP")
            "DOWN" -> StaticDPadDirectionPreview(direction = "DOWN")
            "LEFT" -> StaticDPadDirectionPreview(direction = "LEFT")
            "RIGHT" -> StaticDPadDirectionPreview(direction = "RIGHT")

            "LT" -> StaticTriggerPreview(label = "LT")
            "RT" -> StaticTriggerPreview(label = "RT")

            "LB" -> StaticBumperPreview(label = "LB")
            "RB" -> StaticBumperPreview(label = "RB")

            "XBOX" -> StaticSystemCircle(label = "⨂", iconColor = NeonPalette.Cyan)
            "MENU" -> StaticSystemCircle(label = "☰", iconColor = Color.White)
            "VIEW" -> StaticSystemCircle(label = "⧉", iconColor = Color.White)
            "SHARE", "SCREENSHOT" -> StaticSystemCircle(label = "⇪", iconColor = Color.White)

            "M1", "M2", "M3", "M4", "PROFILE", "TURBO" -> StaticMacroPreview(label = key)

            else -> StaticFaceButton(label = key.take(3), color = Color.Gray)
        }
    }
}

@Composable
private fun StaticFaceButton(label: String, color: Color) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF2E2E2E), Color(0xFF141414)),
                    center = Offset(0.35f, 0.35f),
                    radius = 80f
                )
            )
            .border(1.5.dp, color.copy(alpha = 0.55f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun StaticJoystickPreview(isLeft: Boolean) {
    Box(
        modifier = Modifier
            .size(54.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF2C2C2C), Color(0xFF121212)),
                    center = Offset(0.5f, 0.5f),
                    radius = 90f
                )
            )
            .border(1.5.dp, Color.White.copy(alpha = 0.2f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // Inner thumb cup ring
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF1E1E1E), Color(0xFF0D0D0D)),
                        center = Offset(0.4f, 0.4f),
                        radius = 60f
                    )
                )
                .border(1.dp, NeonPalette.Cyan.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isLeft) "LS" else "RS",
                color = NeonPalette.Cyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun StaticDPadCrossPreview() {
    Canvas(modifier = Modifier.size(50.dp)) {
        val w = size.width
        val h = size.height
        val armW = w / 3f
        val armH = h / 3f
        val corner = 4.dp.toPx()

        val armColor = Color(0xFF1C1C1C)
        val strokeColor = NeonPalette.Cyan.copy(alpha = 0.4f)
        val strokeWidth = 1.dp.toPx()

        // Vertical bar
        drawRoundRect(
            color = armColor,
            topLeft = Offset(armW, 0f),
            size = Size(armW, h),
            cornerRadius = CornerRadius(corner, corner)
        )
        // Horizontal bar
        drawRoundRect(
            color = armColor,
            topLeft = Offset(0f, armH),
            size = Size(w, armH),
            cornerRadius = CornerRadius(corner, corner)
        )

        // Center outline accent
        drawRoundRect(
            color = strokeColor,
            topLeft = Offset(armW, 0f),
            size = Size(armW, h),
            cornerRadius = CornerRadius(corner, corner),
            style = Stroke(strokeWidth)
        )
        drawRoundRect(
            color = strokeColor,
            topLeft = Offset(0f, armH),
            size = Size(w, armH),
            cornerRadius = CornerRadius(corner, corner),
            style = Stroke(strokeWidth)
        )
    }
}

@Composable
private fun StaticDPadDirectionPreview(direction: String) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1A1A1A))
            .border(1.dp, NeonPalette.Cyan.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        val rotation = when (direction) {
            "UP" -> 0f
            "DOWN" -> 180f
            "LEFT" -> 270f
            else -> 90f
        }
        Icon(
            imageVector = Icons.Rounded.ArrowDropUp,
            contentDescription = direction,
            tint = NeonPalette.Cyan,
            modifier = Modifier
                .size(30.dp)
                .rotate(rotation)
        )
    }
}

@Composable
private fun StaticTriggerPreview(label: String) {
    Box(
        modifier = Modifier
            .width(52.dp)
            .height(34.dp)
            .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 6.dp, bottomEnd = 6.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF333333), Color(0xFF151515))
                )
            )
            .border(
                1.dp,
                NeonPalette.Cyan.copy(alpha = 0.5f),
                RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 6.dp, bottomEnd = 6.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun StaticBumperPreview(label: String) {
    Box(
        modifier = Modifier
            .width(56.dp)
            .height(26.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF2E2E2E), Color(0xFF181818))
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun StaticSystemCircle(label: String, iconColor: Color) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(Color(0xFF1A1A1A))
            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = iconColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun StaticMacroPreview(label: String) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF141A24))
            .border(1.dp, NeonPalette.Cyan.copy(alpha = 0.45f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = NeonPalette.Cyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
