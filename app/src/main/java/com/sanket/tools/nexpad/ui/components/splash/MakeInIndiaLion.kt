package com.sanket.tools.nexpad.ui.components.splash

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/**
 * Authentic vector rendering of the iconic "Make in India" / "Aatmanirbhar Bharat"
 * walking lion logo in cyber-metallic outline theme.
 *
 * Features the official walking lion silhouette and its authentic natural
 * defining lines (sweeping curved mane locks, feline ear contour, roaring open jaw,
 * shoulder blade arc, chest bib, ribcage curve, and rear thigh muscle).
 *
 * Constructed with Catmull-Rom cubic Bézier splines for 100% mathematically smooth,
 * non-pixelated rendering, styled with the futuristic cyber gradient outline and
 * glowing accent theme from the NEXPAD aesthetic.
 */
@Composable
fun MakeInIndiaWalkingLion(
    modifier: Modifier = Modifier,
    size: Dp = 230.dp,
    primaryColor: Color = Color.White,
    accentColor: Color = Color(0xFFFF9933), // Saffron
    secondaryAccent: Color = Color(0xFF00E5FF) // Cyber Cyan
) {
    val infiniteTransition = rememberInfiniteTransition(label = "LionMotion")

    // Left-to-right metallic shine beam sweep (2000ms duration)
    val shineOffset by infiniteTransition.animateFloat(
        initialValue = -120f,
        targetValue = 420f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ShineSweep"
    )

    // Predatory gait bobbing and subtle breathing pulse
    val walkCycle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WalkCycle"
    )

    val auraPulse by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AuraPulse"
    )

    val walkPhase = walkCycle * 2f * PI.toFloat()
    val bodyBob = sin(walkPhase * 2f) * 2.5f

    // Pre-build the exact authentic silhouette Path and inner anatomical paths once
    val lionSilhouettePath = remember { buildAuthenticLionPath() }
    val anatomicalLines = remember { buildAuthenticInnerLines() }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = this.size.width
            val canvasH = this.size.height

            // Base coordinate box is 300x150
            val scaleFactor = (canvasW / 300f).coerceAtMost(canvasH / 150f)
            val offsetX = (canvasW - 300f * scaleFactor) / 2f
            val offsetY = (canvasH - 150f * scaleFactor) / 2f + bodyBob

            withTransform({
                translate(left = offsetX, top = offsetY)
                scale(scaleX = scaleFactor, scaleY = scaleFactor, pivot = Offset.Zero)
            }) {
                // 1. Futuristic laser guide ground line under paws (y=146)
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            secondaryAccent.copy(alpha = 0.2f * auraPulse),
                            accentColor.copy(alpha = 0.5f * auraPulse),
                            secondaryAccent.copy(alpha = 0.2f * auraPulse),
                            Color.Transparent
                        )
                    ),
                    start = Offset(10f, 146f),
                    end = Offset(290f, 146f),
                    strokeWidth = 2.2f
                )

                // 2. Fill the authentic silhouette with the deep metallic cyber base
                drawPath(
                    path = lionSilhouettePath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF131D31),
                            Color(0xFF090E1A)
                        ),
                        startY = 14f,
                        endY = 145f
                    )
                )

                // 3. Left-to-right sweeping metallic shine gleam clipped inside the lion (narrow, upright beam)
                clipPath(lionSilhouettePath) {
                    val beamWidth = 35f
                    val tiltDeltaY = 7.5f
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                secondaryAccent.copy(alpha = 0.2f),
                                Color.White.copy(alpha = 0.92f),
                                accentColor.copy(alpha = 0.25f),
                                Color.Transparent
                            ),
                            start = Offset(shineOffset - beamWidth, 75f - tiltDeltaY),
                            end = Offset(shineOffset + beamWidth, 75f + tiltDeltaY)
                        )
                    )
                }

                // 4. Draw the authentic outer silhouette outline with glowing cyber gradient (Previous Code Theme)
                drawPath(
                    path = lionSilhouettePath,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            accentColor,      // Saffron (Tail)
                            primaryColor,     // White (Body)
                            secondaryAccent,  // Cyan (Chest & Head)
                            accentColor       // Saffron (Mane Tip)
                        ),
                        startX = 0f,
                        endX = 300f
                    ),
                    style = Stroke(
                        width = 2.4f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // 4. Draw the smooth natural defining anatomical lines from the reference artwork
                for (line in anatomicalLines) {
                    val lineColor = when (line.styleKey) {
                        LineStyle.PRIMARY_WHITE -> primaryColor.copy(alpha = 0.9f * auraPulse)
                        LineStyle.ACCENT_SAFFRON -> accentColor.copy(alpha = 0.85f * auraPulse)
                        LineStyle.ACCENT_CYAN -> secondaryAccent.copy(alpha = 0.85f * auraPulse)
                    }
                    drawPath(
                        path = line.path,
                        color = lineColor,
                        style = Stroke(
                            width = line.strokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }

                // 5. Roaring Jaw & Eye Cyber Details
                // Glowing Cyber Eye (diamond slit)
                drawCircle(
                    color = secondaryAccent,
                    radius = 2.2f,
                    center = Offset(266f, 43f)
                )
                drawCircle(
                    color = secondaryAccent.copy(alpha = 0.45f * auraPulse),
                    radius = 5.0f,
                    center = Offset(266f, 43f)
                )

                // Tooth / Fang Highlight
                drawLine(
                    color = primaryColor,
                    start = Offset(272f, 73f),
                    end = Offset(271f, 76f),
                    strokeWidth = 2f,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

/**
 * Styling categories for natural anatomical lines.
 */
private enum class LineStyle {
    PRIMARY_WHITE,
    ACCENT_SAFFRON,
    ACCENT_CYAN
}

/**
 * Holds a pre-built smooth Path and its theme style.
 */
private class AnatomicalLine(
    val path: Path,
    val styleKey: LineStyle,
    val strokeWidth: Float
)

/**
 * Builds the ultra-smooth, continuous vector path of the authentic
 * Make in India / Aatmanirbhar Bharat walking lion silhouette.
 *
 * Catmull-Rom cubic Bézier splines guarantee zero pixelation or facets,
 * providing mathematically smooth, organic curves across all displays.
 */
private fun buildAuthenticLionPath(): Path {
    val points = listOf(
        Offset(237.2f, 14.0f),
        Offset(226.0f, 19.2f),
        Offset(216.1f, 18.7f),
        Offset(209.8f, 20.0f),
        Offset(199.1f, 28.7f),
        Offset(194.7f, 30.8f),
        Offset(184.2f, 32.9f),
        Offset(155.0f, 37.1f),
        Offset(103.8f, 35.5f),
        Offset(75.4f, 43.4f),
        Offset(46.9f, 45.7f),
        Offset(35.2f, 47.8f),
        Offset(22.1f, 53.3f),
        Offset(12.7f, 61.2f),
        Offset(8.8f, 69.8f),
        Offset(9.1f, 83.7f),
        Offset(12.7f, 90.5f),
        Offset(16.9f, 94.5f),
        Offset(21.4f, 96.6f),
        Offset(24.0f, 101.8f),
        Offset(26.6f, 104.2f),
        Offset(34.4f, 106.8f),
        Offset(44.8f, 105.7f),
        Offset(46.4f, 104.2f),
        Offset(46.2f, 101.8f),
        Offset(38.6f, 94.5f),
        Offset(31.5f, 90.5f),
        Offset(25.3f, 91.3f),
        Offset(17.7f, 84.5f),
        Offset(15.9f, 80.6f),
        Offset(15.6f, 74.5f),
        Offset(18.2f, 66.7f),
        Offset(25.0f, 59.9f),
        Offset(36.5f, 54.9f),
        Offset(44.3f, 53.6f),
        Offset(75.1f, 54.1f),
        Offset(72.3f, 61.4f),
        Offset(62.3f, 77.7f),
        Offset(56.1f, 94.2f),
        Offset(55.0f, 102.3f),
        Offset(48.2f, 106.5f),
        Offset(47.5f, 108.6f),
        Offset(48.8f, 114.4f),
        Offset(48.0f, 126.4f),
        Offset(51.1f, 132.2f),
        Offset(53.5f, 141.1f),
        Offset(57.6f, 143.7f),
        Offset(69.1f, 143.5f),
        Offset(74.3f, 141.1f),
        Offset(74.9f, 136.1f),
        Offset(72.0f, 132.2f),
        Offset(68.9f, 130.4f),
        Offset(62.9f, 129.8f),
        Offset(64.9f, 118.6f),
        Offset(69.6f, 112.8f),
        Offset(77.7f, 109.1f),
        Offset(83.2f, 102.6f),
        Offset(92.1f, 100.2f),
        Offset(95.0f, 97.9f),
        Offset(96.8f, 98.7f),
        Offset(98.6f, 103.1f),
        Offset(98.9f, 120.7f),
        Offset(107.2f, 129.3f),
        Offset(123.4f, 141.6f),
        Offset(130.7f, 141.9f),
        Offset(143.5f, 139.8f),
        Offset(145.1f, 136.1f),
        Offset(142.0f, 131.4f),
        Offset(131.0f, 129.8f),
        Offset(120.5f, 119.9f),
        Offset(120.5f, 116.5f),
        Offset(124.7f, 108.9f),
        Offset(127.6f, 96.8f),
        Offset(132.6f, 90.5f),
        Offset(135.9f, 90.8f),
        Offset(150.6f, 96.6f),
        Offset(167.3f, 96.6f),
        Offset(168.6f, 97.6f),
        Offset(164.7f, 118.6f),
        Offset(161.0f, 128.5f),
        Offset(161.3f, 130.9f),
        Offset(163.6f, 133.8f),
        Offset(162.8f, 140.1f),
        Offset(167.3f, 143.5f),
        Offset(175.4f, 144.8f),
        Offset(183.2f, 143.0f),
        Offset(185.8f, 141.1f),
        Offset(185.5f, 137.2f),
        Offset(177.2f, 131.7f),
        Offset(176.4f, 128.3f),
        Offset(188.7f, 107.0f),
        Offset(194.2f, 101.0f),
        Offset(201.2f, 105.7f),
        Offset(209.0f, 106.8f),
        Offset(211.1f, 108.1f),
        Offset(218.7f, 120.1f),
        Offset(224.7f, 125.9f),
        Offset(227.0f, 130.6f),
        Offset(233.1f, 134.8f),
        Offset(236.4f, 141.6f),
        Offset(248.5f, 143.2f),
        Offset(253.7f, 142.7f),
        Offset(256.5f, 140.1f),
        Offset(256.3f, 136.4f),
        Offset(253.4f, 131.9f),
        Offset(245.6f, 129.3f),
        Offset(239.3f, 117.3f),
        Offset(238.0f, 106.3f),
        Offset(242.4f, 101.8f),
        Offset(244.8f, 96.8f),
        Offset(247.4f, 98.7f),
        Offset(255.0f, 90.5f),
        Offset(260.2f, 96.0f),
        Offset(266.2f, 89.5f),
        Offset(267.2f, 84.8f),
        Offset(269.6f, 88.2f),
        Offset(275.6f, 87.9f),
        Offset(277.4f, 85.3f),
        Offset(278.7f, 75.9f),
        Offset(275.3f, 73.0f),
        Offset(271.2f, 74.3f),
        Offset(269.9f, 71.7f),
        Offset(272.5f, 70.1f),
        Offset(286.8f, 68.0f),
        Offset(291.8f, 63.0f),
        Offset(292.6f, 59.6f),
        Offset(280.3f, 45.2f),
        Offset(279.3f, 41.0f),
        Offset(275.1f, 35.2f),
        Offset(276.1f, 29.7f),
        Offset(270.6f, 31.3f),
        Offset(267.8f, 23.7f),
        Offset(265.2f, 20.6f),
        Offset(260.7f, 19.0f),
        Offset(256.5f, 14.0f)
    )
    return buildSmoothClosedPath(points)
}

/**
 * Builds the authentic natural defining anatomical lines:
 * - Sweeping curved mane locks
 * - Ear contour
 * - Snout / cheek / roaring jaw outline
 * - Neck crest curve
 * - Front leg tendon & shoulder muscle
 * - Pectoral / chest bib curve
 * - Ribcage & flank curve
 * - Muscular rear thigh & pelvis arch
 */
private fun buildAuthenticInnerLines(): List<AnatomicalLine> {
    return listOf(
        // Mane Lock 1 (Lower sweeping S-curve from crest through bib)
        AnatomicalLine(
            path = buildSmoothOpenPath(
                listOf(
                    Offset(205.4f, 60.1f),
                    Offset(206.4f, 65.4f),
                    Offset(209.6f, 72.7f),
                    Offset(217.4f, 80.0f),
                    Offset(226.8f, 87.4f),
                    Offset(232.5f, 94.7f),
                    Offset(233.6f, 102.1f),
                    Offset(233.6f, 104.2f)
                )
            ),
            styleKey = LineStyle.PRIMARY_WHITE,
            strokeWidth = 2.2f
        ),
        // Mane Lock 2 (Upper sweeping S-curve parallel lock)
        AnatomicalLine(
            path = buildSmoothOpenPath(
                listOf(
                    Offset(210.6f, 42.3f),
                    Offset(211.6f, 47.5f),
                    Offset(213.7f, 53.8f),
                    Offset(219.0f, 60.1f),
                    Offset(226.8f, 66.4f),
                    Offset(234.6f, 72.7f),
                    Offset(240.9f, 79.0f),
                    Offset(243.0f, 81.1f)
                )
            ),
            styleKey = LineStyle.ACCENT_SAFFRON,
            strokeWidth = 2.0f
        ),
        // Roaring Cheek & Jaw contour
        AnatomicalLine(
            path = buildSmoothOpenPath(
                listOf(
                    Offset(257.6f, 51.7f),
                    Offset(255.5f, 57.0f),
                    Offset(253.4f, 62.2f),
                    Offset(254.5f, 67.5f),
                    Offset(258.6f, 72.7f),
                    Offset(263.8f, 78.0f),
                    Offset(264.9f, 79.0f)
                )
            ),
            styleKey = LineStyle.ACCENT_CYAN,
            strokeWidth = 2.0f
        ),
        // Ear contour
        AnatomicalLine(
            path = buildSmoothOpenPath(
                listOf(
                    Offset(264.9f, 38.1f),
                    Offset(260.7f, 39.2f),
                    Offset(257.6f, 40.2f),
                    Offset(254.5f, 42.3f),
                    Offset(253.4f, 46.5f)
                )
            ),
            styleKey = LineStyle.PRIMARY_WHITE,
            strokeWidth = 1.8f
        ),
        // Neck crest lock
        AnatomicalLine(
            path = buildSmoothOpenPath(
                listOf(
                    Offset(191.8f, 38.1f),
                    Offset(189.7f, 44.4f),
                    Offset(187.6f, 50.7f),
                    Offset(186.6f, 54.9f)
                )
            ),
            styleKey = LineStyle.PRIMARY_WHITE,
            strokeWidth = 1.8f
        ),
        // Front leg tendon / shoulder
        AnatomicalLine(
            path = buildSmoothOpenPath(
                listOf(
                    Offset(172.0f, 86.3f),
                    Offset(172.0f, 92.6f),
                    Offset(172.0f, 98.9f),
                    Offset(172.0f, 101.0f)
                )
            ),
            styleKey = LineStyle.ACCENT_CYAN,
            strokeWidth = 2.0f
        ),
        // Lower chest bib
        AnatomicalLine(
            path = buildSmoothOpenPath(
                listOf(
                    Offset(205.4f, 90.5f),
                    Offset(203.3f, 93.7f),
                    Offset(200.7f, 95.8f),
                    Offset(197.5f, 97.9f)
                )
            ),
            styleKey = LineStyle.PRIMARY_WHITE,
            strokeWidth = 1.8f
        ),
        // Ribcage & flank curve
        AnatomicalLine(
            path = buildSmoothOpenPath(
                listOf(
                    Offset(120.8f, 61.2f),
                    Offset(120.8f, 67.5f),
                    Offset(120.8f, 70.6f),
                    Offset(121.9f, 76.9f),
                    Offset(123.9f, 81.1f),
                    Offset(126.0f, 84.2f)
                )
            ),
            styleKey = LineStyle.ACCENT_CYAN,
            strokeWidth = 2.0f
        ),
        // Rear thigh & pelvis muscle
        AnatomicalLine(
            path = buildSmoothOpenPath(
                listOf(
                    Offset(108.3f, 83.2f),
                    Offset(104.6f, 85.3f),
                    Offset(102.0f, 87.4f),
                    Offset(98.9f, 90.5f)
                )
            ),
            styleKey = LineStyle.ACCENT_SAFFRON,
            strokeWidth = 2.0f
        )
    )
}

/**
 * Converts a sequence of closed contour points into a mathematically smooth,
 * continuous cubic Bézier closed Path using Catmull-Rom tangent conversion.
 */
private fun buildSmoothClosedPath(points: List<Offset>): Path {
    val path = Path()
    val n = points.size
    if (n < 3) return path

    path.moveTo(points[0].x, points[0].y)
    for (i in 0 until n) {
        val p0 = points[(i - 1 + n) % n]
        val p1 = points[i]
        val p2 = points[(i + 1) % n]
        val p3 = points[(i + 2) % n]

        // Catmull-Rom to Cubic Bézier conversion
        val c1x = p1.x + (p2.x - p0.x) / 6f
        val c1y = p1.y + (p2.y - p0.y) / 6f
        val c2x = p2.x - (p3.x - p1.x) / 6f
        val c2y = p2.y - (p3.y - p1.y) / 6f

        path.cubicTo(c1x, c1y, c2x, c2y, p2.x, p2.y)
    }
    path.close()
    return path
}

/**
 * Converts an open point sequence into a smooth continuous cubic Bézier open Path.
 */
private fun buildSmoothOpenPath(points: List<Offset>): Path {
    val path = Path()
    val n = points.size
    if (n < 2) return path
    if (n == 2) {
        path.moveTo(points[0].x, points[0].y)
        path.lineTo(points[1].x, points[1].y)
        return path
    }

    path.moveTo(points[0].x, points[0].y)
    for (i in 0 until n - 1) {
        val p0 = points[maxOf(0, i - 1)]
        val p1 = points[i]
        val p2 = points[i + 1]
        val p3 = points[minOf(n - 1, i + 2)]

        val c1x = p1.x + (p2.x - p0.x) / 6f
        val c1y = p1.y + (p2.y - p0.y) / 6f
        val c2x = p2.x - (p3.x - p1.x) / 6f
        val c2y = p2.y - (p3.y - p1.y) / 6f

        path.cubicTo(c1x, c1y, c2x, c2y, p2.x, p2.y)
    }
    return path
}
