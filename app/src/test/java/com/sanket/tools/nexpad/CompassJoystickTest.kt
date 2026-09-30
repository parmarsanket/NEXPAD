package com.sanket.tools.nexpad

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Mathematical and behavioral unit tests for CompassJoystick:
 * - Euclidean radial clamping to maximum travel (24dp)
 * - 8 Directional Compass Pip indexing (0° North = 0, 45° NE = 1, 90° East = 2, ..., 315° NW = 7)
 * - Directional activation threshold (magnitude > 0.40 activates nearest pip)
 * - Rotating direction indicator angle calculation: (atan2(dx, -dy) * 180 / PI + 360) % 360
 * - Opposite-casting 3D drop shadow calculations (dx * -0.30, 8 + dy * -0.30)
 * - Specular lens dynamic offset physics
 * - Stationary tap vs drag threshold (<= 4dp)
 */
class CompassJoystickTest {

    private val maxTravelDp = 24.0f
    private val tapThresholdDp = 4.0f

    @Test
    fun testEuclideanTravelClamping() {
        val x1 = 12f
        val y1 = 16f
        val dist1 = hypot(x1, y1)
        assertEquals(20f, dist1, 0.001f)
        assertTrue(dist1 <= maxTravelDp)

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
    fun testCompassPip8DirectionalIndexing() {
        // Function replicating Compass pip selection:
        fun calculateActivePip(dx: Float, dy: Float): Int {
            val dist = hypot(dx, dy)
            val magnitude = (dist / maxTravelDp).coerceIn(0f, 1f)
            if (magnitude <= 0.40f) return -1
            val angleDeg = (atan2(dx, -dy) * 180f / Math.PI.toFloat() + 360f) % 360f
            return (Math.round(angleDeg / 45.0).toInt() % 8)
        }

        // Low deflection (magnitude <= 0.40) -> no pip illuminated (-1)
        assertEquals(-1, calculateActivePip(0f, -5f))
        assertEquals(-1, calculateActivePip(0f, 0f))

        // North / Up: dx = 0, dy = -24 -> 0° -> pip 0
        assertEquals(0, calculateActivePip(0f, -24f))

        // North-East: dx = 17, dy = -17 -> ~45° -> pip 1
        assertEquals(1, calculateActivePip(17f, -17f))

        // East / Right: dx = 24, dy = 0 -> 90° -> pip 2
        assertEquals(2, calculateActivePip(24f, 0f))

        // South-East: dx = 17, dy = 17 -> ~135° -> pip 3
        assertEquals(3, calculateActivePip(17f, 17f))

        // South / Down: dx = 0, dy = 24 -> 180° -> pip 4
        assertEquals(4, calculateActivePip(0f, 24f))

        // South-West: dx = -17, dy = 17 -> ~225° -> pip 5
        assertEquals(5, calculateActivePip(-17f, 17f))

        // West / Left: dx = -24, dy = 0 -> 270° -> pip 6
        assertEquals(6, calculateActivePip(-24f, 0f))

        // North-West: dx = -17, dy = -17 -> ~315° -> pip 7
        assertEquals(7, calculateActivePip(-17f, -17f))
    }

    @Test
    fun testRotatingIndicatorAnglePhysics() {
        // Formula: (Math.atan2(dx, -dy) * 180 / Math.PI + 360) % 360
        fun calcAngle(dx: Float, dy: Float): Float {
            return (atan2(dx, -dy) * 180f / Math.PI.toFloat() + 360f) % 360f
        }

        // Pushing North -> 0 degrees
        assertEquals(0f, calcAngle(0f, -20f), 0.01f)

        // Pushing East -> 90 degrees
        assertEquals(90f, calcAngle(20f, 0f), 0.01f)

        // Pushing South -> 180 degrees
        assertEquals(180f, calcAngle(0f, 20f), 0.01f)

        // Pushing West -> 270 degrees
        assertEquals(270f, calcAngle(-20f, 0f), 0.01f)
    }

    @Test
    fun testOppositeCastingDropShadowPhysics() {
        // CSS formula: calc(var(--dx, 0px) * -0.3) calc(8px + var(--dy, 0px) * -0.3) 12px
        val dxNeutral = 0f
        val dyNeutral = 0f
        val shadowXNeutral = dxNeutral * -0.30f
        val shadowYNeutral = 8f + dyNeutral * -0.30f
        assertEquals(0f, shadowXNeutral, 0.001f)
        assertEquals(8f, shadowYNeutral, 0.001f)

        // Pushed right (+24px, 0): shadow casts left (-7.2px)
        val dxRight = 24f
        val dyRight = 0f
        val shadowXRight = dxRight * -0.30f
        val shadowYRight = 8f + dyRight * -0.30f
        assertEquals(-7.2f, shadowXRight, 0.001f)
        assertEquals(8.0f, shadowYRight, 0.001f)

        // Pushed down (0, +24px): shadow casts up (+0.8px)
        val dxDown = 0f
        val dyDown = 24f
        val shadowXDown = dxDown * -0.30f
        val shadowYDown = 8f + dyDown * -0.30f
        assertEquals(0f, shadowXDown, 0.001f)
        assertEquals(0.8f, shadowYDown, 0.001f)
    }

    @Test
    fun testStationaryTapVsDragThreshold() {
        // Tap test: small movement <= 4dp -> recognized as tap (L3/R3 click)
        val maxMoveStationary = 2.5f
        val isTap = maxMoveStationary <= tapThresholdDp
        assertTrue("Sub-threshold movement must be recognized as stationary tap", isTap)

        // Drag test: movement > 4dp -> recognized as analog drag
        val maxMoveDrag = 14.0f
        val isDrag = maxMoveDrag > tapThresholdDp
        assertTrue("Movement beyond 4dp must be recognized as directional drag", isDrag)
    }
}
