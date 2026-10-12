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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-precision animated vector rendering of the iconic "Make in India"
 * mechanical gear lion.
 *
 * Features:
 *  - Mechanical lion silhouette (head, proud mane, torso, paws, curved tail)
 *  - Continuously rotating industrial drive cogs & gear trains in torso, shoulder, and hip
 *  - Dynamic feline walking gait (oscillating leg stride and predatory body bob)
 *  - Cyberpunk metallic glow with Indian Tricolor subtle accents (Saffron, White, Green)
 */
@Composable
fun MakeInIndiaWalkingLion(
    modifier: Modifier = Modifier,
    size: Dp = 180.dp,
    primaryColor: Color = Color.White,
    accentColor: Color = Color(0xFFFF9933), // Saffron
    secondaryAccent: Color = Color(0xFF00E5FF) // Cyber Cyan
) {
    val infiniteTransition = rememberInfiniteTransition(label = "LionMotion")

    // Continuous gear rotation (360 degrees)
    val gearAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "GearRotation"
    )

    // Feline walking stride cycle (0f to 1f)
    val walkCycle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WalkCycle"
    )

    // Breathing neon aura pulse
    val auraPulse by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AuraPulse"
    )

    val twoPi = (2 * PI).toFloat()
    val walkPhase = walkCycle * twoPi

    // Body rhythmic gait bobbing
    val bodyBob = sin(walkPhase * 2) * 3.5f

    // Leg stride angles (degrees)
    val frontLeftLegAngle = sin(walkPhase) * 16f
    val frontRightLegAngle = sin(walkPhase + PI.toFloat()) * 16f
    val rearLeftLegAngle = sin(walkPhase + PI.toFloat()) * 14f
    val rearRightLegAngle = sin(walkPhase) * 14f

    // Tail sway
    val tailSway = sin(walkPhase) * 8f

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height

            // Coordinate normalization: base design is in 300x180 box
            val scale = (w / 300f).coerceAtMost(h / 180f)
            val offsetX = (w - 300f * scale) / 2f
            val offsetY = (h - 180f * scale) / 2f + bodyBob

            withTransform({
                translate(left = offsetX, top = offsetY)
                scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
            }) {
                // 1. Ground contact shadow / futuristic laser guide line
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            secondaryAccent.copy(alpha = 0.15f * auraPulse),
                            accentColor.copy(alpha = 0.4f * auraPulse),
                            secondaryAccent.copy(alpha = 0.15f * auraPulse),
                            Color.Transparent
                        )
                    ),
                    start = Offset(20f, 162f),
                    end = Offset(280f, 162f),
                    strokeWidth = 2f
                )

                // 2. Far Legs (Back/Shadowed pair)
                drawLeg(
                    hipX = 85f,
                    hipY = 95f,
                    length = 62f,
                    swingAngle = rearRightLegAngle,
                    color = primaryColor.copy(alpha = 0.45f),
                    strokeWidth = 5f,
                    isRear = true
                )
                drawLeg(
                    hipX = 205f,
                    hipY = 95f,
                    length = 62f,
                    swingAngle = frontRightLegAngle,
                    color = primaryColor.copy(alpha = 0.45f),
                    strokeWidth = 5f,
                    isRear = false
                )

                // 3. Tail with animated sway
                drawTail(
                    rootX = 60f,
                    rootY = 82f,
                    swayAngle = tailSway,
                    color = primaryColor
                )

                // 4. Main Body & Torso Mechanical Structure
                drawLionBody(
                    primaryColor = primaryColor,
                    accentColor = accentColor
                )

                // 5. Rotating Industrial Gear Trains inside the Lion's Anatomy
                // Shoulder Primary Drive Gear (Largest)
                drawIndustrialGear(
                    center = Offset(200f, 88f),
                    radius = 22f,
                    teethCount = 12,
                    rotationAngle = gearAngle,
                    color = accentColor,
                    coreColor = primaryColor
                )

                // Abdominal Central Interlocking Cog (Rotates counter-direction)
                drawIndustrialGear(
                    center = Offset(155f, 92f),
                    radius = 16f,
                    teethCount = 10,
                    rotationAngle = -gearAngle * 1.375f,
                    color = secondaryAccent,
                    coreColor = primaryColor
                )

                // Pelvis / Hip Drive Cog
                drawIndustrialGear(
                    center = Offset(115f, 85f),
                    radius = 18f,
                    teethCount = 11,
                    rotationAngle = gearAngle * 1.22f,
                    color = accentColor,
                    coreColor = primaryColor
                )

                // Small Neck Pinion Cog
                drawIndustrialGear(
                    center = Offset(228f, 70f),
                    radius = 10f,
                    teethCount = 8,
                    rotationAngle = -gearAngle * 2.2f,
                    color = secondaryAccent,
                    coreColor = primaryColor
                )

                // 6. Proud Mane and Head (Mechanical Cog Plating)
                drawLionHeadAndMane(
                    primaryColor = primaryColor,
                    accentColor = accentColor,
                    glowAlpha = auraPulse
                )

                // 7. Near Legs (Front/Foreground pair with full opacity)
                drawLeg(
                    hipX = 85f,
                    hipY = 95f,
                    length = 62f,
                    swingAngle = rearLeftLegAngle,
                    color = primaryColor,
                    strokeWidth = 6.5f,
                    isRear = true
                )
                drawLeg(
                    hipX = 205f,
                    hipY = 95f,
                    length = 62f,
                    swingAngle = frontLeftLegAngle,
                    color = primaryColor,
                    strokeWidth = 6.5f,
                    isRear = false
                )
            }
        }
    }
}

/**
 * Draws a multi-segmented industrial gear with teeth and inner spokes.
 */
private fun DrawScope.drawIndustrialGear(
    center: Offset,
    radius: Float,
    teethCount: Int,
    rotationAngle: Float,
    color: Color,
    coreColor: Color
) {
    rotate(rotationAngle, pivot = center) {
        val toothDepth = radius * 0.28f
        val toothWidthRad = (PI.toFloat() / teethCount) * 0.5f

        val path = Path()
        for (i in 0 until teethCount) {
            val baseAngle = (i * 2 * PI / teethCount).toFloat()

            val a1 = baseAngle - toothWidthRad
            val a2 = baseAngle - toothWidthRad * 0.6f
            val a3 = baseAngle + toothWidthRad * 0.6f
            val a4 = baseAngle + toothWidthRad

            val rInner = radius - toothDepth
            val rOuter = radius

            val p1 = Offset(center.x + rInner * cos(a1), center.y + rInner * sin(a1))
            val p2 = Offset(center.x + rOuter * cos(a2), center.y + rOuter * sin(a2))
            val p3 = Offset(center.x + rOuter * cos(a3), center.y + rOuter * sin(a3))
            val p4 = Offset(center.x + rInner * cos(a4), center.y + rInner * sin(a4))

            if (i == 0) {
                path.moveTo(p1.x, p1.y)
            } else {
                path.lineTo(p1.x, p1.y)
            }
            path.lineTo(p2.x, p2.y)
            path.lineTo(p3.x, p3.y)
            path.lineTo(p4.x, p4.y)
        }
        path.close()

        // Outer Gear Ring
        drawPath(path = path, color = color, style = Stroke(width = 2.2f))

        // Pitch Circle
        drawCircle(
            color = color.copy(alpha = 0.5f),
            radius = radius * 0.65f,
            center = center,
            style = Stroke(width = 1.2f)
        )

        // Spokes (4 Cross Spokes)
        val spokeR = radius * 0.6f
        drawLine(
            color = color.copy(alpha = 0.7f),
            start = Offset(center.x - spokeR, center.y),
            end = Offset(center.x + spokeR, center.y),
            strokeWidth = 1.5f
        )
        drawLine(
            color = color.copy(alpha = 0.7f),
            start = Offset(center.x, center.y - spokeR),
            end = Offset(center.x, center.y + spokeR),
            strokeWidth = 1.5f
        )

        // Center Pin
        drawCircle(
            color = coreColor,
            radius = radius * 0.22f,
            center = center
        )
        drawCircle(
            color = Color(0xFF070B14),
            radius = radius * 0.11f,
            center = center
        )
    }
}

/**
 * Draws the mechanical lion's main body structure with aerodynamic gear plating.
 */
private fun DrawScope.drawLionBody(
    primaryColor: Color,
    accentColor: Color
) {
    // Upper Spine / Backline Path
    val backPath = Path().apply {
        moveTo(60f, 85f) // Tail root
        cubicTo(75f, 72f, 105f, 68f, 140f, 70f) // Lower back
        cubicTo(165f, 71f, 185f, 65f, 215f, 60f) // Shoulder hump
        lineTo(235f, 64f) // Neck base
    }
    drawPath(path = backPath, color = primaryColor, style = Stroke(width = 3.5f, cap = StrokeCap.Round))

    // Underbelly Path with Cog Cutouts
    val bellyPath = Path().apply {
        moveTo(80f, 98f)
        cubicTo(110f, 108f, 150f, 110f, 190f, 105f)
        lineTo(215f, 102f)
    }
    drawPath(path = bellyPath, color = primaryColor.copy(alpha = 0.8f), style = Stroke(width = 2.5f, cap = StrokeCap.Round))

    // Rib cage mechanical slots
    for (i in 0..4) {
        val slotX = 135f + i * 14f
        drawLine(
            color = accentColor.copy(alpha = 0.7f),
            start = Offset(slotX, 76f),
            end = Offset(slotX - 4f, 96f),
            strokeWidth = 2f,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Draws the lion's iconic roaring head, fierce muzzle, and layered gear-tooth mane.
 */
private fun DrawScope.drawLionHeadAndMane(
    primaryColor: Color,
    accentColor: Color,
    glowAlpha: Float
) {
    // Layered Mane Cogs (Gear teeth radiating backwards like the official logo)
    val manePath = Path().apply {
        moveTo(215f, 52f)
        lineTo(205f, 40f)
        lineTo(220f, 44f)
        lineTo(220f, 32f)
        lineTo(232f, 38f)
        lineTo(238f, 26f)
        lineTo(248f, 35f)
        lineTo(258f, 24f)
        lineTo(262f, 38f)
        lineTo(274f, 32f)
        lineTo(270f, 46f)
        lineTo(282f, 44f)
        lineTo(274f, 56f)
        lineTo(285f, 60f)
        lineTo(272f, 68f)
        lineTo(280f, 76f)
        lineTo(265f, 82f)
        lineTo(270f, 92f)
        lineTo(252f, 90f)
        lineTo(248f, 102f)
        lineTo(232f, 94f)
        lineTo(225f, 102f)
        lineTo(215f, 88f)
    }
    drawPath(
        path = manePath,
        color = accentColor.copy(alpha = 0.85f),
        style = Stroke(width = 2.5f, cap = StrokeCap.Round)
    )

    // Forehead and Muzzle / Jaw Profile
    val headPath = Path().apply {
        moveTo(252f, 42f)
        lineTo(275f, 52f) // Forehead slope
        lineTo(288f, 58f) // Nose bridge
        lineTo(292f, 66f) // Nose tip
        lineTo(284f, 70f) // Upper lip
        lineTo(278f, 68f) // Mouth gap
        lineTo(280f, 75f) // Lower jaw / chin
        lineTo(265f, 76f) // Throat
    }
    drawPath(
        path = headPath,
        color = primaryColor,
        style = Stroke(width = 3f, cap = StrokeCap.Round)
    )

    // Glowing Cyber Eye (Diamond / Slit)
    drawCircle(
        color = Color(0xFF00E5FF),
        radius = 2.5f,
        center = Offset(272f, 56f)
    )
    drawCircle(
        color = Color(0xFF00E5FF).copy(alpha = 0.4f * glowAlpha),
        radius = 5.5f,
        center = Offset(272f, 56f)
    )
}

/**
 * Draws an articulated mechanical walking leg with knee joint and paw.
 */
private fun DrawScope.drawLeg(
    hipX: Float,
    hipY: Float,
    length: Float,
    swingAngle: Float,
    color: Color,
    strokeWidth: Float,
    isRear: Boolean
) {
    val rad = (swingAngle * PI / 180f).toFloat()

    // Thigh segment
    val thighLen = length * 0.52f
    val kneeX = hipX + thighLen * sin(rad)
    val kneeY = hipY + thighLen * cos(rad)

    // Shin/Calf segment
    val shinAngle = if (isRear) swingAngle * 0.7f - 12f else swingAngle * 0.85f + 10f
    val shinRad = (shinAngle * PI / 180f).toFloat()
    val shinLen = length * 0.48f
    val pawX = kneeX + shinLen * sin(shinRad)
    val pawY = (kneeY + shinLen * cos(shinRad)).coerceAtMost(162f)

    // Thigh
    drawLine(
        color = color,
        start = Offset(hipX, hipY),
        end = Offset(kneeX, kneeY),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )

    // Knee Cog Joint
    drawCircle(
        color = color,
        radius = strokeWidth * 0.8f,
        center = Offset(kneeX, kneeY)
    )

    // Shin
    drawLine(
        color = color,
        start = Offset(kneeX, kneeY),
        end = Offset(pawX, pawY),
        strokeWidth = strokeWidth * 0.85f,
        cap = StrokeCap.Round
    )

    // Mechanical Paw / Footplate (horizontal grounding)
    drawLine(
        color = color,
        start = Offset(pawX - 4f, pawY),
        end = Offset(pawX + 8f, pawY),
        strokeWidth = strokeWidth * 0.75f,
        cap = StrokeCap.Round
    )
}

/**
 * Draws the lion's proud curved tail with animated swaying tuft.
 */
private fun DrawScope.drawTail(
    rootX: Float,
    rootY: Float,
    swayAngle: Float,
    color: Color
) {
    val swayRad = (swayAngle * PI / 180f).toFloat()
    val tipX = 32f + 8f * sin(swayRad)
    val tipY = 52f + 6f * cos(swayRad)

    val tailPath = Path().apply {
        moveTo(rootX, rootY)
        cubicTo(
            45f, 92f,
            24f, 75f,
            tipX, tipY
        )
    }
    drawPath(path = tailPath, color = color, style = Stroke(width = 2.8f, cap = StrokeCap.Round))

    // Tail Tuft (Flame-shaped end)
    drawCircle(
        color = Color(0xFFFF9933),
        radius = 4f,
        center = Offset(tipX, tipY)
    )
}
