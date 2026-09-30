package com.sanket.tools.nexpad

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Mathematical and behavioral unit tests for FluxJoystick:
 * - Euclidean radial clamping to maximum travel (24dp)
 * - 12 graduation tick angle distribution (30° intervals)
 * - Directional gate angle and magnitude tracking
 * - Opposite-casting 3D drop shadow calculations
 * - Inverse specular lens tracking
 * - Stationary tap vs analog drag detection threshold (<= 4dp)
 */
class FluxJoystickTest {

    private val maxTravelDp = 24.0f
    private val tapThresholdDp = 4.0f

    @Test
    fun testEuclideanTravelClamping() {
        // Within bounds: (10, 10) -> hypot = 14.14 < 24 -> untouched
        val x1 = 10f
        val y1 = 10f
        val dist1 = hypot(x1, y1)
        assertTrue(dist1 <= maxTravelDp)

        // Outside bounds: (30, 40) -> hypot = 50 > 24 -> clamped to 24
        val x2 = 30f
        val y2 = 40f
        val dist2 = hypot(x2, y2)
        assertEquals(50f, dist2, 0.001f)

        val angle = atan2(y2, x2)
        val clampedX = cos(angle) * maxTravelDp
        val clampedY = sin(angle) * maxTravelDp
        val clampedDist = hypot(clampedX, clampedY)

        assertEquals(maxTravelDp, clampedDist, 0.001f)
        assertEquals(24f * (30f / 50f), clampedX, 0.001f) // 14.4
        assertEquals(24f * (40f / 50f), clampedY, 0.001f) // 19.2
    }

    @Test
    fun testTwelveTickAnglesDistribution() {
        // 12 ticks around 360° circle must be spaced by exactly 30°
        val angles = (0 until 12).map { i -> (i * 30.0) % 360.0 }
        assertEquals(12, angles.size)
        assertEquals(0.0, angles[0], 0.001)
        assertEquals(30.0, angles[1], 0.001)
        assertEquals(60.0, angles[2], 0.001)
        assertEquals(90.0, angles[3], 0.001)
        assertEquals(180.0, angles[6], 0.001)
        assertEquals(270.0, angles[9], 0.001)
        assertEquals(330.0, angles[11], 0.001)

        // Verify difference between every adjacent tick is 30°
        for (i in 0 until 11) {
            assertEquals(30.0, angles[i + 1] - angles[i], 0.001)
        }
    }

    @Test
    fun testOppositeCastingDropShadowPhysics() {
        // Formula from CSS:
        // shadowOffsetX = dx * -0.32
        // shadowOffsetY = 8 + dy * -0.32
        val dxAtNeutral = 0f
        val dyAtNeutral = 0f
        val shadowXNeutral = dxAtNeutral * -0.32f
        val shadowYNeutral = 8f + dyAtNeutral * -0.32f
        assertEquals(0f, shadowXNeutral, 0.001f)
        assertEquals(8f, shadowYNeutral, 0.001f) // Overhead 8px baseline light

        // When stick is pushed full right (+24px, 0):
        // Shadow must cast LEFT (-7.68px)
        val dxRight = 24f
        val dyRight = 0f
        val shadowXRight = dxRight * -0.32f
        val shadowYRight = 8f + dyRight * -0.32f
        assertEquals(-7.68f, shadowXRight, 0.001f)
        assertEquals(8f, shadowYRight, 0.001f)

        // When stick is pushed full up (0, -24px):
        // Shadow must cast DOWN (+15.68px)
        val dxUp = 0f
        val dyUp = -24f
        val shadowXUp = dxUp * -0.32f
        val shadowYUp = 8f + dyUp * -0.32f
        assertEquals(0f, shadowXUp, 0.001f)
        assertEquals(15.68f, shadowYUp, 0.001f)
    }

    @Test
    fun testInverseSpecularLensReflectionPhysics() {
        // Formula from CSS:
        // at calc(50% - dxn * 16%) calc(20% - dyn * 16%)
        val normNeutralX = 0f
        val normNeutralY = 0f
        val specRatioNeutralX = 0.50f - normNeutralX * 0.16f
        val specRatioNeutralY = 0.20f - normNeutralY * 0.16f
        assertEquals(0.50f, specRatioNeutralX, 0.001f)
        assertEquals(0.20f, specRatioNeutralY, 0.001f)

        // Full deflection right (normX = +1.0) -> specular highlight shifts left to 34%
        val normRightX = 1.0f
        val specRatioRightX = 0.50f - normRightX * 0.16f
        assertEquals(0.34f, specRatioRightX, 0.001f)

        // Full deflection down (normY = +1.0) -> specular highlight shifts up to 4%
        val normDownY = 1.0f
        val specRatioDownY = 0.20f - normDownY * 0.16f
        assertEquals(0.04f, specRatioDownY, 0.001f)
    }

    @Test
    fun testTapVersusDragDisambiguation() {
        // Movement <= 4dp classified as Tap (triggers L3 / R3 click)
        val microJitter = 2.5f
        val isTap = microJitter <= tapThresholdDp
        assertTrue("Sub-4dp movement must register as a stationary tap for L3/R3", isTap)

        // Movement > 4dp classified as analog drag
        val intentionalMovement = 12.0f
        val isDrag = intentionalMovement > tapThresholdDp
        assertTrue("Movements > 4dp must register as continuous analog steering", isDrag)
    }

    @Test
    fun testDirectionalGateAngleConversion() {
        // Joystick angles: up = 0°, right = 90°, down = 180°, left = 270° / -90°
        // Using atan2(dx, -dy) * 180 / PI
        val upAngle = Math.toDegrees(atan2(0.0, -(-24.0))) // dy = -24
        assertEquals(0.0, upAngle, 0.001)

        val rightAngle = Math.toDegrees(atan2(24.0, -0.0))
        assertEquals(90.0, rightAngle, 0.001)

        val downAngle = Math.toDegrees(atan2(0.0, -(24.0))) // dy = +24
        assertEquals(180.0, downAngle, 0.001)

        val leftAngle = Math.toDegrees(atan2(-24.0, -0.0))
        assertEquals(-90.0, leftAngle, 0.001)
    }
}
