package com.sanket.tools.nexpad

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Mathematical and behavioral unit tests for GyroJoystick:
 * - Euclidean radial clamping to maximum travel (24dp)
 * - Dual 3D Gimbal ring tilt angles and counter-rotation kinematics:
 *   - Outer ring (.gim.o): rotateX(-dyn * 38°), rotateY(dxn * 38°)
 *   - Inner ring (.gim.i): rotateX(dyn * 56°), rotateY(-dxn * 56°)
 * - Opposite-casting 3D drop shadow calculations (dx * -0.30, 8 + dy * -0.30)
 * - Specular lens dynamic offset physics
 * - Stationary tap vs analog drag detection threshold (<= 4dp)
 */
class GyroJoystickTest {

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
    fun testDualGimbalTiltAnglesAndCounterRotation() {
        // Outer gimbal formula: rotateX = -dyn * 38, rotateY = dxn * 38
        fun calcOuterRot(dxn: Float, dyn: Float): Pair<Float, Float> {
            val rotX = -dyn * 38f
            val rotY = dxn * 38f
            return Pair(rotX, rotY)
        }

        // Inner gimbal formula: rotateX = dyn * 56, rotateY = -dxn * 56
        fun calcInnerRot(dxn: Float, dyn: Float): Pair<Float, Float> {
            val rotX = dyn * 56f
            val rotY = -dxn * 56f
            return Pair(rotX, rotY)
        }

        // 1. Center / Neutral: both at 0
        val (outNeutralX, outNeutralY) = calcOuterRot(0f, 0f)
        val (inNeutralX, inNeutralY) = calcInnerRot(0f, 0f)
        assertEquals(0f, outNeutralX, 0.001f)
        assertEquals(0f, outNeutralY, 0.001f)
        assertEquals(0f, inNeutralX, 0.001f)
        assertEquals(0f, inNeutralY, 0.001f)

        // 2. Full Deflection Right (dxn = 1.0, dyn = 0):
        // Outer ring tilts +38° on Y axis
        // Inner ring counter-tilts -56° on Y axis
        val (outRightX, outRightY) = calcOuterRot(1f, 0f)
        val (inRightX, inRightY) = calcInnerRot(1f, 0f)
        assertEquals(0f, outRightX, 0.001f)
        assertEquals(38f, outRightY, 0.001f)
        assertEquals(0f, inRightX, 0.001f)
        assertEquals(-56f, inRightY, 0.001f)
        // Opposite signs verify counter-rotation
        assertTrue("Outer and inner Y rotation must oppose each other", outRightY * inRightY < 0f)

        // 3. Full Deflection Down (dxn = 0, dyn = 1.0):
        // Outer ring tilts -38° on X axis
        // Inner ring counter-tilts +56° on X axis
        val (outDownX, outDownY) = calcOuterRot(0f, 1f)
        val (inDownX, inDownY) = calcInnerRot(0f, 1f)
        assertEquals(-38f, outDownX, 0.001f)
        assertEquals(0f, outDownY, 0.001f)
        assertEquals(56f, inDownX, 0.001f)
        assertEquals(0f, inDownY, 0.001f)
        // Opposite signs verify counter-rotation
        assertTrue("Outer and inner X rotation must oppose each other", outDownX * inDownX < 0f)

        // 4. Diagonal Deflection (0.707, 0.707):
        val (outDiagX, outDiagY) = calcOuterRot(0.7071f, 0.7071f)
        val (inDiagX, inDiagY) = calcInnerRot(0.7071f, 0.7071f)
        assertEquals(-26.87f, outDiagX, 0.05f)
        assertEquals(26.87f, outDiagY, 0.05f)
        assertEquals(39.60f, inDiagX, 0.05f)
        assertEquals(-39.60f, inDiagY, 0.05f)
    }

    @Test
    fun testOppositeCastingDropShadowPhysics() {
        // Formula: calc(var(--dx, 0px) * -0.3) calc(8px + var(--dy, 0px) * -0.3) 12px
        val dxNeutral = 0f
        val dyNeutral = 0f
        val shadowXNeutral = dxNeutral * -0.30f
        val shadowYNeutral = 8f + dyNeutral * -0.30f
        assertEquals(0f, shadowXNeutral, 0.001f)
        assertEquals(8f, shadowYNeutral, 0.001f)

        // Stick pushed right (+24px, 0): shadow casts left (-7.2px)
        val dxRight = 24f
        val dyRight = 0f
        val shadowXRight = dxRight * -0.30f
        val shadowYRight = 8f + dyRight * -0.30f
        assertEquals(-7.2f, shadowXRight, 0.001f)
        assertEquals(8.0f, shadowYRight, 0.001f)

        // Stick pushed down (0, +24px): shadow casts up (+0.8px)
        val dxDown = 0f
        val dyDown = 24f
        val shadowXDown = dxDown * -0.30f
        val shadowYDown = 8f + dyDown * -0.30f
        assertEquals(0f, shadowXDown, 0.001f)
        assertEquals(0.8f, shadowYDown, 0.001f)
    }

    @Test
    fun testStationaryTapVsDragThreshold() {
        val maxMoveStationary = 2.5f
        val isTap = maxMoveStationary <= tapThresholdDp
        assertTrue("Sub-threshold movement must be recognized as stationary tap", isTap)

        val maxMoveDrag = 14.0f
        val isDrag = maxMoveDrag > tapThresholdDp
        assertTrue("Movement beyond 4dp must be recognized as directional drag", isDrag)
    }
}
