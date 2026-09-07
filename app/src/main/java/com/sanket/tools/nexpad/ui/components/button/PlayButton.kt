package com.sanket.tools.nexpad.ui.components.button

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.sanket.tools.nexpad.ui.theme.NeonPalette

@Composable
fun PlayButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val infinite = rememberInfiniteTransition(label = "playBtn")

    val glowAlpha by infinite.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 900,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val scale by infinite.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.size(80.dp)
        ) {
            drawRoundRect(
                color = NeonPalette.Green.copy(alpha = 0.08f * glowAlpha),
                topLeft = Offset(
                    x = -size.width * 0.25f,
                    y = -size.height * 0.10f
                ),
                size = Size(
                    width = size.width * 1.5f,
                    height = size.height * 1.2f
                ),
                cornerRadius = CornerRadius(50.dp.toPx())
            )

            // Play Triangle
            val path = Path().apply {
                moveTo(size.width * .3f, size.height * .22f)
                lineTo(size.width * .85f, size.height * .5f)
                lineTo(size.width * .3f, size.height * .78f)
                close()
            }

            drawPath(path = path, color = NeonPalette.Green.copy(alpha = 0.4f), style = Stroke(width = 16.dp.toPx()))
            drawPath(path = path, color = NeonPalette.Green.copy(alpha = 0.7f), style = Stroke(width = 8.dp.toPx()))
            drawPath(path = path, color = NeonPalette.Green)
        }
    }
}
