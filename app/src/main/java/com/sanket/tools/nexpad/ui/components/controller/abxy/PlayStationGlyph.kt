package com.sanket.tools.nexpad.ui.components.controller.abxy

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
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
import com.sanket.tools.nexpad.category.ControlKey
import com.sanket.tools.nexpad.category.ControllerLabelStyle

/**
 * Authentic Classic PlayStation controller button colors (DualShock / PlayStation iconic 4-color palette).
 * - Top: Triangle △ (Green)
 * - Right: Circle ○ (Red)
 * - Left: Square □ (Pink)
 * - Bottom: Cross × (Blue)
 */
object PlayStationColors {
    /** Classic PlayStation Cross (Blue #3F8FE0, bottom position / A) */
    val Cross: Color = Color(0xFF3F8FE0)

    /** Classic PlayStation Circle (Red #E6474E, right position / B) */
    val Circle: Color = Color(0xFFE6474E)

    /** Classic PlayStation Square (Pink #E055B8, left position / X) */
    val Square: Color = Color(0xFFE055B8)

    /** Classic PlayStation Triangle (Green #3FD25A, top position / Y) */
    val Triangle: Color = Color(0xFF3FD25A)
}

/**
 * Classic Xbox controller button colors.
 * - Top: Y (Yellow)
 * - Right: B (Red)
 * - Left: X (Blue)
 * - Bottom: A (Green)
 */
object XboxColors {
    /** Xbox A button (Green #3FD25A, bottom position) */
    val A: Color = Color(0xFF3FD25A)

    /** Xbox B button (Red #E6474E, right position) */
    val B: Color = Color(0xFFE6474E)

    /** Xbox X button (Blue #3F8FE0, left position) */
    val X: Color = Color(0xFF3F8FE0)

    /** Xbox Y button (Yellow #E0A03F, top position) */
    val Y: Color = Color(0xFFE0A03F)
}

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
 * Returns the classic authentic PlayStation color for a given [PlayStationShape].
 * - Cross: Blue (#3F8FE0)
 * - Circle: Red (#E6474E)
 * - Square: Pink (#E055B8)
 * - Triangle: Green (#3FD25A)
 */
fun getPlayStationColor(shape: PlayStationShape): Color = when (shape) {
    PlayStationShape.CROSS -> PlayStationColors.Cross
    PlayStationShape.CIRCLE -> PlayStationColors.Circle
    PlayStationShape.SQUARE -> PlayStationColors.Square
    PlayStationShape.TRIANGLE -> PlayStationColors.Triangle
}

/**
 * Resolves the authentic face button color based on its control identity, active [ControllerLabelStyle],
 * and/or explicit glyph label.
 *
 * When [labelStyle] is [ControllerLabelStyle.PLAYSTATION] or [displayLabel] is a PlayStation symbol:
 * - Cross × / A  -> Blue (#3F8FE0)
 * - Circle ○ / B -> Red (#E6474E)
 * - Square □ / X -> Pink (#E055B8)
 * - Triangle △ / Y -> Green (#3FD25A)
 *
 * When [labelStyle] is [ControllerLabelStyle.XBOX]:
 * - A -> Green (#3FD25A)
 * - B -> Red (#E6474E)
 * - X -> Blue (#3F8FE0)
 * - Y -> Yellow (#E0A03F)
 */
fun resolveButtonColor(
    controlKey: String,
    labelStyle: ControllerLabelStyle = ControllerLabelStyle.XBOX,
    displayLabel: String? = null,
    fallbackColor: Color = Color(0xFF3FD25A)
): Color {
    // 1. If explicit displayLabel or controlKey represents a PlayStation shape, use its authentic color
    val psShape = getPlayStationShape(displayLabel) ?: getPlayStationShape(controlKey)
    if (psShape != null) {
        return getPlayStationColor(psShape)
    }

    // 2. If active profile style is PLAYSTATION, map the cardinal button position
    if (labelStyle == ControllerLabelStyle.PLAYSTATION) {
        return when (controlKey.trim().uppercase()) {
            "A", "CROSS", "✕", "×"    -> PlayStationColors.Cross
            "B", "CIRCLE", "○", "◯"   -> PlayStationColors.Circle
            "X", "SQUARE", "□", "◻"   -> PlayStationColors.Square
            "Y", "TRIANGLE", "△", "▲" -> PlayStationColors.Triangle
            else -> fallbackColor
        }
    }

    // 3. Default Xbox Face Button Colors
    return when (controlKey.trim().uppercase()) {
        "A" -> XboxColors.A
        "B" -> XboxColors.B
        "X" -> XboxColors.X
        "Y" -> XboxColors.Y
        else -> fallbackColor
    }
}

/**
 * Overload resolving button color from a type-safe [ControlKey].
 */
fun resolveButtonColor(
    controlKey: ControlKey,
    labelStyle: ControllerLabelStyle = ControllerLabelStyle.XBOX
): Color = resolveButtonColor(controlKey.key, labelStyle)

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
    val density = LocalDensity.current
    val strokeStyle = remember(size, strokeWidth, density) {
        val boxPx = with(density) { size.toPx() }
        val strokePx = strokeWidth?.let { with(density) { it.toPx() } } ?: (boxPx * 0.11f).coerceAtLeast(1.5f * density.density)
        Stroke(width = strokePx)
    }
    val roundSquareStrokeStyle = remember(size, strokeWidth, density) {
        val boxPx = with(density) { size.toPx() }
        val strokePx = strokeWidth?.let { with(density) { it.toPx() } } ?: (boxPx * 0.11f).coerceAtLeast(1.5f * density.density)
        Stroke(width = strokePx, join = StrokeJoin.Round)
    }
    val triangleStrokeStyle = remember(size, strokeWidth, density) {
        val boxPx = with(density) { size.toPx() }
        val strokePx = strokeWidth?.let { with(density) { it.toPx() } } ?: (boxPx * 0.11f).coerceAtLeast(1.5f * density.density)
        Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
    }
    val trianglePath = remember(size, strokeWidth, density) {
        if (shape == PlayStationShape.TRIANGLE) {
            val boxPx = with(density) { size.toPx() }
            val strokePx = strokeWidth?.let { with(density) { it.toPx() } } ?: (boxPx * 0.11f).coerceAtLeast(1.5f * density.density)
            val center = Offset(boxPx / 2f, boxPx / 2f)
            val R = (boxPx * 0.44f).coerceAtLeast(strokePx)
            val Rtri = R * 1.06f
            val effR = (Rtri - (strokePx / 2f)).coerceAtLeast(1f)
            val yShift = R * 0.08f
            Path().apply {
                moveTo(center.x, center.y - effR + yShift)
                lineTo(center.x + (effR * 0.8660f), center.y + (effR * 0.50f) + yShift)
                lineTo(center.x - (effR * 0.8660f), center.y + (effR * 0.50f) + yShift)
                close()
            }
        } else null
    }

    Canvas(modifier = modifier.size(size)) {
        val boxSize = this.size.minDimension
        val strokePx = strokeWidth?.toPx() ?: (boxSize * 0.11f).coerceAtLeast(1.5f * density.density)
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
                    style = strokeStyle
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
                    style = roundSquareStrokeStyle
                )
            }
            PlayStationShape.TRIANGLE -> {
                trianglePath?.let {
                    drawPath(
                        path = it,
                        color = color,
                        style = triangleStrokeStyle
                    )
                }
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
