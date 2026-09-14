package com.sanket.tools.nexpad.ui.components.effects

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sanket.tools.nexpad.ui.theme.NeonPalette

@Composable
fun RadarScanner(size: Dp) {
    val infinite = rememberInfiniteTransition(label = "radar")
    val sweepAngle by infinite.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing)),
        label = "sweep"
    )
    val pulse by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing)),
        label = "pulse"
    )

    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.toPx() / 2f
            repeat(3) { ring ->
                val phase = (pulse + ring / 3f) % 1f
                drawCircle(
                    color = NeonPalette.Cyan.copy(alpha = (1f - phase) * 0.35f),
                    radius = radius * phase,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
            drawCircle(color = NeonPalette.Cyan.copy(alpha = 0.08f), radius = radius)
            drawCircle(color = NeonPalette.Cyan.copy(alpha = 0.3f), radius = radius, style = Stroke(width = 1.5.dp.toPx()))
            rotate(sweepAngle) {
                drawArc(
                    brush = Brush.sweepGradient(listOf(Color.Transparent, NeonPalette.Cyan.copy(alpha = 0.5f))),
                    startAngle = 0f,
                    sweepAngle = 90f,
                    useCenter = true,
                    size = this.size
                )
            }
        }
        Icon(
            imageVector = Icons.Rounded.Computer,
            contentDescription = null,
            tint = NeonPalette.Cyan.copy(alpha = 0.7f),
            modifier = Modifier.size(size * 0.32f)
        )
    }
}
