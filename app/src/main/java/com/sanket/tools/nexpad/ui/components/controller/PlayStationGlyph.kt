package com.sanket.tools.nexpad.ui.components.controller

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Supported PlayStation geometric button shapes.
 */
enum class PlayStationShape {
    CROSS,
    CIRCLE,
    SQUARE,
    TRIANGLE
}

/**
 * Determines whether a string identifier or label corresponds to an authentic PlayStation face shape.
 */
fun getPlayStationShape(label: String?): PlayStationShape? {
    if (label == null) return null
    return when (label.trim()) {
        "✕", "×", "╳", "CROSS" -> PlayStationShape.CROSS
        "○", "◯", "CIRCLE" -> PlayStationShape.CIRCLE
        "□", "◻", "⬜", "SQUARE" -> PlayStationShape.SQUARE
        "△", "▲", "∆", "TRIANGLE" -> PlayStationShape.TRIANGLE
        else -> null
    }
}

/**
 * Returns true if the string is any recognizable PlayStation face button symbol.
 */
fun isPlayStationSymbol(label: String?): Boolean = getPlayStationShape(label) != null

/**
 * High-fidelity vector glyph renderer for PlayStation button faces.
 *
 * Ensures all four shapes share identical stroke thickness, optical bounding boxes,
 * and smooth rounded corners/caps regardless of the system font or Android OEM skin.
 */
@Composable
fun PlayStationSymbol(
    shape: PlayStationShape,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    strokeWidth: Dp? = null
) {
    Canvas(modifier = modifier.size(size)) {
        val boxSize = this.size.minDimension
        val strokePx = strokeWidth?.toPx() ?: (boxSize * 0.11f).coerceAtLeast(1.5f * density)
        val center = Offset(this.size.width / 2f, this.size.height / 2f)

        // Master optical envelope radius: every shape is mathematically harmonized around R
        val R = (boxSize * 0.44f).coerceAtLeast(strokePx)

        when (shape) {
            PlayStationShape.CROSS -> {
                // 45-degree cross whose four outer arm tips (including round cap) reach exactly radius R
                val armDist = (R - (strokePx / 2f)).coerceAtLeast(1f)
                val arm = armDist * 0.7071f
                drawLine(
                    color = color,
                    start = Offset(center.x - arm, center.y - arm),
                    end = Offset(center.x + arm, center.y + arm),
                    strokeWidth = strokePx,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = color,
                    start = Offset(center.x + arm, center.y - arm),
                    end = Offset(center.x - arm, center.y + arm),
                    strokeWidth = strokePx,
                    cap = StrokeCap.Round
                )
            }
            PlayStationShape.CIRCLE -> {
                // Circular ring whose outer perimeter reaches exactly radius R
                val radius = (R - (strokePx / 2f)).coerceAtLeast(1f)
                drawCircle(
                    color = color,
                    radius = radius,
                    center = center,
                    style = Stroke(width = strokePx)
                )
            }
            PlayStationShape.SQUARE -> {
                // Rounded square whose visual area, line mass, and rounded corners match radius R
                val halfSide = R * 0.75f
                val cornerRadius = CornerRadius(R * 0.18f)
                drawRoundRect(
                    color = color,
                    topLeft = Offset(center.x - halfSide, center.y - halfSide),
                    size = Size(halfSide * 2f, halfSide * 2f),
                    cornerRadius = cornerRadius,
                    style = Stroke(width = strokePx, join = StrokeJoin.Round)
                )
            }
            PlayStationShape.TRIANGLE -> {
                // Equilateral triangle harmonized with R and optically centered
                val Rtri = R * 1.06f
                val effR = (Rtri - (strokePx / 2f)).coerceAtLeast(1f)
                val yShift = R * 0.08f // Optical center compensation for apex vs flat base

                val path = Path().apply {
                    // Top apex
                    moveTo(center.x, center.y - effR + yShift)
                    // Bottom-right vertex
                    lineTo(center.x + (effR * 0.8660f), center.y + (effR * 0.50f) + yShift)
                    // Bottom-left vertex
                    lineTo(center.x - (effR * 0.8660f), center.y + (effR * 0.50f) + yShift)
                    close()
                }
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(
                        width = strokePx,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    }
}

/**
 * Overload that resolves the string label into a PlayStation symbol, or renders nothing if unknown.
 */
@Composable
fun PlayStationSymbol(
    symbol: String,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    strokeWidth: Dp? = null
) {
    val shape = getPlayStationShape(symbol) ?: return
    PlayStationSymbol(
        shape = shape,
        color = color,
        modifier = modifier,
        size = size,
        strokeWidth = strokeWidth
    )
}
