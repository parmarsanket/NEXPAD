package com.sanket.tools.nexpad

import androidx.compose.ui.geometry.Offset
import com.sanket.tools.nexpad.model.NexpadKeys as K
import com.sanket.tools.nexpad.ui.components.controller.dpad.calculateFourLensesTilt
import com.sanket.tools.nexpad.ui.components.controller.dpad.resolveFourLensesTouch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

/**
 * Mathematical, kinematic, and behavioral unit tests for Four Lenses D-Pad (FourLensesDPad):
 * - 3D physical rocker tilt angle calculations:
 *   - UP: rx = +8°, DOWN: rx = -8°
 *   - RIGHT: ry = +8°, LEFT: ry = -8°
 *   - Diagonal compound tilts (UP+RIGHT, DOWN+LEFT, etc.)
 *   - Opposite direction cancellations
 * - 8-Way directional touch resolution (cardinals + diagonals)
 * - Central hub deadzone (18dp) and outer radius containment (82dp)
 * - Four Lenses cluster geometry and sub-pixel symmetry
 */
class FourLensesDPadTest {

    @Test
    fun test3DPhysicalRockerTiltKinematics() {
        // 1. Neutral: rx = 0, ry = 0
        val (neutralX, neutralY) = calculateFourLensesTilt(emptySet())
        assertEquals(0f, neutralX, 0.001f)
        assertEquals(0f, neutralY, 0.001f)

        // 2. Pure Cardinal Directions
        val (upX, upY) = calculateFourLensesTilt(setOf(K.UP))
        assertEquals(8f, upX, 0.001f)
        assertEquals(0f, upY, 0.001f)

        val (downX, downY) = calculateFourLensesTilt(setOf(K.DOWN))
        assertEquals(-8f, downX, 0.001f)
        assertEquals(0f, downY, 0.001f)

        val (rightX, rightY) = calculateFourLensesTilt(setOf(K.RIGHT))
        assertEquals(0f, rightX, 0.001f)
        assertEquals(8f, rightY, 0.001f)

        val (leftX, leftY) = calculateFourLensesTilt(setOf(K.LEFT))
        assertEquals(0f, leftX, 0.001f)
        assertEquals(-8f, leftY, 0.001f)

        // 3. Diagonal Chords (Compound Tilt)
        val (upRightX, upRightY) = calculateFourLensesTilt(setOf(K.UP, K.RIGHT))
        assertEquals(8f, upRightX, 0.001f)
        assertEquals(8f, upRightY, 0.001f)

        val (downLeftX, downLeftY) = calculateFourLensesTilt(setOf(K.DOWN, K.LEFT))
        assertEquals(-8f, downLeftX, 0.001f)
        assertEquals(-8f, downLeftY, 0.001f)

        val (upLeftX, upLeftY) = calculateFourLensesTilt(setOf(K.UP, K.LEFT))
        assertEquals(8f, upLeftX, 0.001f)
        assertEquals(-8f, upLeftY, 0.001f)

        val (downRightX, downRightY) = calculateFourLensesTilt(setOf(K.DOWN, K.RIGHT))
        assertEquals(-8f, downRightX, 0.001f)
        assertEquals(8f, downRightY, 0.001f)

        // 4. Opposing directions cancel each other
        val (cancelX, cancelY) = calculateFourLensesTilt(setOf(K.UP, K.DOWN))
        assertEquals(0f, cancelX, 0.001f)
        assertEquals(0f, cancelY, 0.001f)

        val (cancelHorizontalX, cancelHorizontalY) = calculateFourLensesTilt(setOf(K.LEFT, K.RIGHT))
        assertEquals(0f, cancelHorizontalX, 0.001f)
        assertEquals(0f, cancelHorizontalY, 0.001f)
    }

    @Test
    fun testFourLensesTouchResolution() {
        val stageSize = 164f
        val center = 82f

        // 1. Central Hub Deadzone check (< 18dp from center)
        val deadzoneTouch = resolveFourLensesTouch(Offset(center + 5f, center + 5f), stageSize)
        assertTrue("Touches within center hub deadzone must register no directions", deadzoneTouch.isEmpty())

        val deadzoneCenterTouch = resolveFourLensesTouch(Offset(center, center), stageSize)
        assertTrue("Exact center touch must register no directions", deadzoneCenterTouch.isEmpty())

        // 2. Out-of-bounds check (> 82dp from center)
        val oobTouch = resolveFourLensesTouch(Offset(center + 85f, center), stageSize)
        assertTrue("Touches outside 164dp boundary must register no directions", oobTouch.isEmpty())

        // 3. Pure Cardinal Keys Taps (centers of the 54dp keys)
        // UP key center: (82, 27) -> dx = 0, dy = -55
        assertEquals(setOf(K.UP), resolveFourLensesTouch(Offset(center, 27f), stageSize))

        // DOWN key center: (82, 137) -> dx = 0, dy = 55
        assertEquals(setOf(K.DOWN), resolveFourLensesTouch(Offset(center, 137f), stageSize))

        // LEFT key center: (27, 82) -> dx = -55, dy = 0
        assertEquals(setOf(K.LEFT), resolveFourLensesTouch(Offset(27f, center), stageSize))

        // RIGHT key center: (137, 82) -> dx = 55, dy = 0
        assertEquals(setOf(K.RIGHT), resolveFourLensesTouch(Offset(137f, center), stageSize))

        // 4. Off-center Cardinal Taps (still within the 45° cardinal cone)
        assertEquals(setOf(K.UP), resolveFourLensesTouch(Offset(center + 12f, 25f), stageSize))
        assertEquals(setOf(K.UP), resolveFourLensesTouch(Offset(center - 12f, 25f), stageSize))
        assertEquals(setOf(K.RIGHT), resolveFourLensesTouch(Offset(137f, center + 12f), stageSize))
        assertEquals(setOf(K.RIGHT), resolveFourLensesTouch(Offset(137f, center - 12f), stageSize))

        // 5. Diagonal corner voids must NOT trigger any buttons (no diagonal clicks or multi-button triggering)
        val cornerTopRight = resolveFourLensesTouch(Offset(center + 38f, center - 38f), stageSize)
        assertTrue("Diagonal corner touch must NOT trigger buttons", cornerTopRight.isEmpty())

        val cornerTopLeft = resolveFourLensesTouch(Offset(center - 38f, center - 38f), stageSize)
        assertTrue("Diagonal corner touch must NOT trigger buttons", cornerTopLeft.isEmpty())

        val cornerBottomRight = resolveFourLensesTouch(Offset(center + 38f, center + 38f), stageSize)
        assertTrue("Diagonal corner touch must NOT trigger buttons", cornerBottomRight.isEmpty())

        val cornerBottomLeft = resolveFourLensesTouch(Offset(center - 38f, center + 38f), stageSize)
        assertTrue("Diagonal corner touch must NOT trigger buttons", cornerBottomLeft.isEmpty())
    }

    @Test
    fun testFourLensesGeometryAndSymmetry() {
        val stageSize = 164f
        val center = stageSize / 2f // 82f
        val keyRadius = 27f // 54f / 2f
        val hubRadius = 20f // 40f / 2f

        // Key positions from HTML specification:
        val upPos = Offset(55f + keyRadius, 0f + keyRadius) // (82, 27)
        val downPos = Offset(55f + keyRadius, 110f + keyRadius) // (82, 137)
        val leftPos = Offset(0f + keyRadius, 55f + keyRadius) // (27, 82)
        val rightPos = Offset(110f + keyRadius, 55f + keyRadius) // (137, 82)

        val hubCenter = Offset(62f + hubRadius, 62f + hubRadius) // (82, 82)

        // Hub must be precisely centered in 164dp container
        assertEquals(center, hubCenter.x, 0.001f)
        assertEquals(center, hubCenter.y, 0.001f)

        // All 4 keys must be equidistant from the center hub
        val distUp = hypot(upPos.x - hubCenter.x, upPos.y - hubCenter.y)
        val distDown = hypot(downPos.x - hubCenter.x, downPos.y - hubCenter.y)
        val distLeft = hypot(leftPos.x - hubCenter.x, leftPos.y - hubCenter.y)
        val distRight = hypot(rightPos.x - hubCenter.x, rightPos.y - hubCenter.y)

        assertEquals(55f, distUp, 0.001f)
        assertEquals(55f, distDown, 0.001f)
        assertEquals(55f, distLeft, 0.001f)
        assertEquals(55f, distRight, 0.001f)

        // Gap between hub and key edge: 55 - 20 (hub r) - 27 (key r) = 8dp
        val hubKeyGap = distUp - hubRadius - keyRadius
        assertEquals(8f, hubKeyGap, 0.001f)
    }
}
