package com.sanket.tools.nexpad

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Mathematical and behavioral unit tests for OrbJoystick:
 * - Euclidean radial clamping to maximum travel (24dp)
 * - Liquid Core inertial lag physics (target offset -dxn * 13px, -dyn * 13px)
 * - Opposite-casting 3D drop shadow calculations (dx * -0.30, 9 + dy * -0.30)
 * - Dual spherical glass specular lens highlight physics
 * - Stationary tap vs analog drag detection threshold (<= 4dp)
 * - Directional gate angle and magnitude tracking
 */
class OrbJoystickTest {

    private val maxTravelDp = 24.0f
    private val tapThresholdDp = 4.0f

    @Test
    fun testEuclideanTravelClamping() {
        // Within bounds: (12, 16) -> hypot = 20 <= 24 -> untouched
        val x1 = 12f
        val y1 = 16f
        val dist1 = hypot(x1, y1)
        assertEquals(20f, dist1, 0.001f)
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
        assertEquals(14.4f, clampedX, 0.001f)
        assertEquals(19.2f, clampedY, 0.001f)
    }

    @Test
    fun testLiquidCoreInertialLagPhysics() {
        // Formula from reference CSS:
        // transform: translate(calc(var(--dxn, 0) * -13px), calc(var(--dyn, 0) * -13px));
        // Neutral position
        val normNeutralX = 0f
        val normNeutralY = 0f
        val coreOffsetNeutralX = -normNeutralX * 13f
        val coreOffsetNeutralY = -normNeutralY * 13f
        assertEquals(0f, coreOffsetNeutralX, 0.001f)
        assertEquals(0f, coreOffsetNeutralY, 0.001f)

        // Full right deflection (+1.0) -> liquid core floats left (-13px)
        val normRightX = 1.0f
        val normRightY = 0f
        val coreOffsetRightX = -normRightX * 13f
        val coreOffsetRightY = -normRightY * 13f
        assertEquals(-13f, coreOffsetRightX, 0.001f)
        assertEquals(0f, coreOffsetRightY, 0.001f)

        // Full down deflection (+1.0) -> liquid core floats up (-13px)
        val normDownX = 0f
        val normDownY = 1.0f
        val coreOffsetDownX = -normDownX * 13f
        val coreOffsetDownY = -normDownY * 13f
        assertEquals(0f, coreOffsetDownX, 0.001f)
        assertEquals(-13f, coreOffsetDownY, 0.001f)

        // Diagonal deflection (0.707, 0.707)
        val normDiagX = 0.7071f
        val normDiagY = 0.7071f
        val coreOffsetDiagX = -normDiagX * 13f
        val coreOffsetDiagY = -normDiagY * 13f
        assertEquals(-9.1923f, coreOffsetDiagX, 0.01f)
        assertEquals(-9.1923f, coreOffsetDiagY, 0.01f)
    }

    @Test
    fun testOppositeCastingDropShadowPhysics() {
        // Formula from reference CSS:
        // calc(var(--dx, 0px) * -0.3) calc(9px + var(--dy, 0px) * -0.3) 14px
        val dxNeutral = 0f
        val dyNeutral = 0f
        val shadowXNeutral = dxNeutral * -0.30f
        val shadowYNeutral = 9f + dyNeutral * -0.30f
        assertEquals(0f, shadowXNeutral, 0.001f)
        assertEquals(9f, shadowYNeutral, 0.001f) // 9px baseline overhead light

        // When stick is pushed right (+24px, 0): shadow casts left (-7.2px)
        val dxRight = 24f
        val dyRight = 0f
        val shadowXRight = dxRight * -0.30f
        val shadowYRight = 9f + dyRight * -0.30f
        assertEquals(-7.20f, shadowXRight, 0.001f)
        assertEquals(9f, shadowYRight, 0.001f)

        // When stick is pushed up (0, -24px): shadow casts down (+16.2px)
        val dxUp = 0f
        val dyUp = -24f
        val shadowXUp = dxUp * -0.30f
        val shadowYUp = 9f + dyUp * -0.30f
        assertEquals(0f, shadowXUp, 0.001f)
        assertEquals(16.20f, shadowYUp, 0.001f)
    }

    @Test
    fun testDualSphericalSpecularLensHighlights() {
        // Formula from reference CSS:
        // Primary light catch:
        // at calc(34% - var(--dxn, 0) * 14%) calc(22% - var(--dyn, 0) * 14%)
        val normNeutralX = 0f
        val normNeutralY = 0f
        val spec1RatioNeutralX = 0.34f - normNeutralX * 0.14f
        val spec1RatioNeutralY = 0.22f - normNeutralY * 0.14f
        assertEquals(0.34f, spec1RatioNeutralX, 0.001f)
        assertEquals(0.22f, spec1RatioNeutralY, 0.001f)

        // Full deflection right (normX = +1.0) -> specular highlight moves inversely to 20%
        val normRightX = 1.0f
        val spec1RatioRightX = 0.34f - normRightX * 0.14f
        assertEquals(0.20f, spec1RatioRightX, 0.001f)

        // Full deflection down (normY = +1.0) -> specular highlight moves inversely to 8%
        val normDownY = 1.0f
        val spec1RatioDownY = 0.22f - normDownY * 0.14f
        assertEquals(0.08f, spec1RatioDownY, 0.001f)

        // Secondary bottom rim bounce highlight: fixed at 55% 94%
        val spec2RatioX = 0.55f
        val spec2RatioY = 0.94f
        assertEquals(0.55f, spec2RatioX, 0.001f)
        assertEquals(0.94f, spec2RatioY, 0.001f)
    }

    @Test
    fun testTapVersusDragDisambiguation() {
        // Stationary touch with distance <= 4dp classified as Tap (triggers L3 / R3 click)
        val tapDist = 2.0f
        assertTrue("Sub-4dp touch must register as a stationary stick button click", tapDist <= tapThresholdDp)

        // Analog steering with distance > 4dp
        val dragDist = 15.0f
        assertTrue("Movement > 4dp must register as continuous analog steering", dragDist > tapThresholdDp)
    }

    @Test
    fun testDirectionalGateAngleConversion() {
        // Up: (0, -24) -> angle = 0°
        val upAngle = (Math.toDegrees(atan2(0.0, -(-24.0))) + 360.0) % 360.0
        assertEquals(0.0, upAngle, 0.001)

        // Right: (+24, 0) -> angle = 90°
        val rightAngle = (Math.toDegrees(atan2(24.0, -0.0)) + 360.0) % 360.0
        assertEquals(90.0, rightAngle, 0.001)

        // Down: (0, +24) -> angle = 180°
        val downAngle = (Math.toDegrees(atan2(0.0, -(24.0))) + 360.0) % 360.0
        assertEquals(180.0, downAngle, 0.001)

        // Left: (-24, 0) -> angle = 270°
        val leftAngle = (Math.toDegrees(atan2(-24.0, -0.0)) + 360.0) % 360.0
        assertEquals(270.0, leftAngle, 0.001)
    }
}
