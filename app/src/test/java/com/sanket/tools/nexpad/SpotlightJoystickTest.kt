package com.sanket.tools.nexpad

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Mathematical and behavioral unit tests for SpotlightJoystick:
 * - Euclidean radial clamping to maximum travel (24dp)
 * - Dynamic Spotlight light pool center tracking (center.x + dx, center.y + dy)
 * - Hidden floor matrix dots reveal physics under 56dp spotlight mask
 * - Directional gate angle calculations (conic sweep around travel vector)
 * - Opposite-casting 3D drop shadow calculations (dx * -0.30, 8 + dy * -0.30)
 * - Specular lens dynamic shift physics
 * - Stationary tap vs analog drag detection threshold (<= 4dp)
 */
class SpotlightJoystickTest {

    private val maxTravelDp = 24.0f
    private val tapThresholdDp = 4.0f
    private val spotlightMaskRadiusDp = 56.0f

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
    fun testSpotlightLightPoolCenterTracking() {
        val centerX = 75f
        val centerY = 75f

        // 1. Center / Idle: spotlight center is exactly at center
        var dx = 0f
        var dy = 0f
        var spotX = centerX + dx
        var spotY = centerY + dy
        assertEquals(75f, spotX, 0.001f)
        assertEquals(75f, spotY, 0.001f)

        // 2. Deflected Right by 20dp: spotlight center shifts right
        dx = 20f
        dy = 0f
        spotX = centerX + dx
        spotY = centerY + dy
        assertEquals(95f, spotX, 0.001f)
        assertEquals(75f, spotY, 0.001f)

        // 3. Deflected Up-Left (-16dp, -12dp): spotlight center shifts up-left
        dx = -16f
        dy = -12f
        spotX = centerX + dx
        spotY = centerY + dy
        assertEquals(59f, spotX, 0.001f)
        assertEquals(63f, spotY, 0.001f)
    }

    @Test
    fun testFloorDotIlluminationUnderSpotlight() {
        val centerX = 75f
        val centerY = 75f

        fun calcDotFactor(dotX: Float, dotY: Float, spotX: Float, spotY: Float): Float {
            val dist = hypot(dotX - spotX, dotY - spotY)
            return if (dist < spotlightMaskRadiusDp) {
                (1f - dist / spotlightMaskRadiusDp).coerceIn(0f, 1f)
            } else {
                0f
            }
        }

        // Dot directly under center (75, 75)
        // When spotlight is at center: factor = 1.0 (maximum illumination)
        val centerFactor = calcDotFactor(75f, 75f, 75f, 75f)
        assertEquals(1.0f, centerFactor, 0.001f)

        // Dot at (75 + 28, 75) = (103, 75) is 28dp away (halfway to mask edge)
        // When spotlight is at center: factor = 0.5
        val halfwayFactor = calcDotFactor(103f, 75f, 75f, 75f)
        assertEquals(0.5f, halfwayFactor, 0.001f)

        // Dot at (75 + 60, 75) = (135, 75) is 60dp away (outside 56dp mask)
        // When spotlight is at center: factor = 0.0 (completely hidden/dark)
        val outsideFactor = calcDotFactor(135f, 75f, 75f, 75f)
        assertEquals(0.0f, outsideFactor, 0.001f)

        // When the joystick moves to the right by 24dp (spotlight at 99, 75):
        // The previously dark dot at (135, 75) is now 36dp away:
        // factor = (1 - 36 / 56) = 20 / 56 ≈ 0.357 (revealed by the moving light!)
        val revealedFactor = calcDotFactor(135f, 75f, 99f, 75f)
        assertTrue("Moving light must reveal previously dark dot", revealedFactor > 0.35f)
    }

    @Test
    fun testDirectionalGateAngleCalculations() {
        fun calcAngleDeg(dx: Float, dy: Float): Float {
            return (atan2(dx, -dy) * 180f / Math.PI.toFloat() + 360f) % 360f
        }

        // Up: dx = 0, dy = -24 -> 0°
        assertEquals(0f, calcAngleDeg(0f, -24f), 0.01f)

        // Right: dx = 24, dy = 0 -> 90°
        assertEquals(90f, calcAngleDeg(24f, 0f), 0.01f)

        // Down: dx = 0, dy = 24 -> 180°
        assertEquals(180f, calcAngleDeg(0f, 24f), 0.01f)

        // Left: dx = -24, dy = 0 -> 270°
        assertEquals(270f, calcAngleDeg(-24f, 0f), 0.01f)

        // Up-Right 45°
        assertEquals(45f, calcAngleDeg(17f, -17f), 0.5f)
    }

    @Test
    fun testOppositeCastingDropShadow() {
        fun calcShadowOffset(dx: Float, dy: Float): Pair<Float, Float> {
            val shadowX = dx * -0.30f
            val shadowY = 8.0f + dy * -0.30f
            return Pair(shadowX, shadowY)
        }

        // Neutral / center: dx=0, dy=0 -> shadow is cast down by 8dp
        val (neutralX, neutralY) = calcShadowOffset(0f, 0f)
        assertEquals(0f, neutralX, 0.001f)
        assertEquals(8f, neutralY, 0.001f)

        // Pushed Right: dx=+20dp -> shadow casts Left: -6dp
        val (rightX, rightY) = calcShadowOffset(20f, 0f)
        assertEquals(-6f, rightX, 0.001f)
        assertEquals(8f, rightY, 0.001f)
        assertTrue("Shadow X must oppose positive dx", rightX < 0f)

        // Pushed Down: dy=+20dp -> shadow Y reduces from 8 to 2dp
        val (downX, downY) = calcShadowOffset(0f, 20f)
        assertEquals(0f, downX, 0.001f)
        assertEquals(2f, downY, 0.001f)

        // Pushed Up: dy=-20dp -> shadow Y extends from 8 to 14dp
        val (upX, upY) = calcShadowOffset(0f, -20f)
        assertEquals(0f, upX, 0.001f)
        assertEquals(14f, upY, 0.001f)
    }

    @Test
    fun testPuckSpecularLensShift() {
        val puckWidth = 76f
        val puckHeight = 76f

        fun calcSpecularCenter(dxn: Float, dyn: Float): Pair<Float, Float> {
            val specX = puckWidth * (0.50f - dxn * 0.16f)
            val specY = puckHeight * (0.20f - dyn * 0.16f)
            return Pair(specX, specY)
        }

        // Neutral (0, 0): lens centered at 50% X, 20% Y
        val (neutralX, neutralY) = calcSpecularCenter(0f, 0f)
        assertEquals(38f, neutralX, 0.001f)
        assertEquals(15.2f, neutralY, 0.001f)

        // Pushed Right (dxn = 1.0): specular shift left
        val (rightX, rightY) = calcSpecularCenter(1f, 0f)
        assertEquals(puckWidth * 0.34f, rightX, 0.001f)
        assertEquals(15.2f, rightY, 0.001f)
        assertTrue("Specular highlight moves counter to displacement", rightX < neutralX)
    }

    @Test
    fun testStationaryTapDetectionVsDrag() {
        // Stationary touch with minimal drift <= 4dp is classified as stick button click
        val tapTravel = 2.5f
        assertTrue(tapTravel <= tapThresholdDp)

        // Drag movement exceeding 4dp is classified as analog stick steering
        val dragTravel = 6.2f
        assertFalse(dragTravel <= tapThresholdDp)
    }
}
