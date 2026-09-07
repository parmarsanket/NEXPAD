package com.sanket.tools.nexpad.ui.components.effects

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sanket.tools.nexpad.ui.theme.NeonPalette

@Composable
fun ScanLine(
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "scanline")

    val offset by transition.animateFloat(
        initialValue = -200f,
        targetValue = 2000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 6000,
                easing = LinearEasing
            )
        ),
        label = "scanlineOffset"
    )

    Canvas(modifier) {
        drawRect(
            brush = Brush.verticalGradient(
                listOf(
                    Color.Transparent,
                    NeonPalette.Cyan.copy(alpha = 0.12f),
                    Color.Transparent
                ),
                startY = offset,
                endY = offset + 120.dp.toPx()
            )
        )
    }
}
