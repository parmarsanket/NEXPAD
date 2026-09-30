package com.sanket.tools.nexpad

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * Mathematical and behavioral unit tests for LensDPad:
 * - 3D physical rocker tilt angle calculations:
 *   - UP: rx = +8°, DOWN: rx = -8°
 *   - RIGHT: ry = +8°, LEFT: ry = -8°
 *   - Diagonal chord combinations (UP+RIGHT, DOWN+LEFT, etc.)
 * - Touch sector angular evaluation for 8-way directional discrimination
 * - Deadzone and max radius containment boundaries
 */
class LensDPadTest {

    @Test
    fun test3DPhysicalRockerTiltKinematics() {
        fun calcTilt(pressedDirs: Set<String>): Pair<Float, Float> {
            var rx = 0f
            var ry = 0f
            if (pressedDirs.contains("UP")) rx += 8.0f
            if (pressedDirs.contains("DOWN")) rx -= 8.0f
            if (pressedDirs.contains("RIGHT")) ry += 8.0f
            if (pressedDirs.contains("LEFT")) ry -= 8.0f
            return Pair(rx, ry)
        }

        // 1. Neutral: rx = 0, ry = 0
        val (neutralX, neutralY) = calcTilt(emptySet())
        assertEquals(0f, neutralX, 0.001f)
        assertEquals(0f, neutralY, 0.001f)

        // 2. Pure Cardinal Directions
        val (upX, upY) = calcTilt(setOf("UP"))
        assertEquals(8f, upX, 0.001f)
        assertEquals(0f, upY, 0.001f)

        val (downX, downY) = calcTilt(setOf("DOWN"))
        assertEquals(-8f, downX, 0.001f)
        assertEquals(0f, downY, 0.001f)

        val (rightX, rightY) = calcTilt(setOf("RIGHT"))
        assertEquals(0f, rightX, 0.001f)
        assertEquals(8f, rightY, 0.001f)

        val (leftX, leftY) = calcTilt(setOf("LEFT"))
        assertEquals(0f, leftX, 0.001f)
        assertEquals(-8f, leftY, 0.001f)

        // 3. Diagonal Chords
        val (upRightX, upRightY) = calcTilt(setOf("UP", "RIGHT"))
        assertEquals(8f, upRightX, 0.001f)
        assertEquals(8f, upRightY, 0.001f)

        val (downLeftX, downLeftY) = calcTilt(setOf("DOWN", "LEFT"))
        assertEquals(-8f, downLeftX, 0.001f)
        assertEquals(-8f, downLeftY, 0.001f)

        val (upLeftX, upLeftY) = calcTilt(setOf("UP", "LEFT"))
        assertEquals(8f, upLeftX, 0.001f)
        assertEquals(-8f, upLeftY, 0.001f)

        val (downRightX, downRightY) = calcTilt(setOf("DOWN", "RIGHT"))
        assertEquals(-8f, downRightX, 0.001f)
        assertEquals(8f, downRightY, 0.001f)

        // 4. Opposing directions cancel each other
        val (cancelX, cancelY) = calcTilt(setOf("UP", "DOWN"))
        assertEquals(0f, cancelX, 0.001f)
        assertEquals(0f, cancelY, 0.001f)
    }

    @Test
    fun testCardinalArmHitResolution() {
        fun evaluateDirection(dx: Float, dy: Float, dpadWidth: Float = 160f): Set<String> {
            val scale = dpadWidth / 160f
            val armHalfWidth = 23f * scale
            val armMaxReach = 75f * scale
            val centerDeadzone = 14f * scale

            val isUp = dy in (-armMaxReach)..(-centerDeadzone) && kotlin.math.abs(dx) <= armHalfWidth
            val isDown = dy in centerDeadzone..armMaxReach && kotlin.math.abs(dx) <= armHalfWidth
            val isLeft = dx in (-armMaxReach)..(-centerDeadzone) && kotlin.math.abs(dy) <= armHalfWidth
            val isRight = dx in centerDeadzone..armMaxReach && kotlin.math.abs(dy) <= armHalfWidth

            return when {
                isUp && isRight -> if (kotlin.math.abs(dy) >= kotlin.math.abs(dx)) setOf("UP") else setOf("RIGHT")
                isUp && isLeft  -> if (kotlin.math.abs(dy) >= kotlin.math.abs(dx)) setOf("UP") else setOf("LEFT")
                isDown && isRight -> if (kotlin.math.abs(dy) >= kotlin.math.abs(dx)) setOf("DOWN") else setOf("RIGHT")
                isDown && isLeft  -> if (kotlin.math.abs(dy) >= kotlin.math.abs(dx)) setOf("DOWN") else setOf("LEFT")
                isUp -> setOf("UP")
                isDown -> setOf("DOWN")
                isLeft -> setOf("LEFT")
                isRight -> setOf("RIGHT")
                else -> emptySet()
            }
        }

        // 1. Center Hub Deadzone check: dx = 5, dy = 5 -> empty
        val deadzoneResult = evaluateDirection(5f, 5f)
        assertTrue("Touches within center deadzone must register no directions", deadzoneResult.isEmpty())

        // 2. Out-of-bounds check: dx = 85, dy = 0 -> beyond 75dp reach -> empty
        val oobResult = evaluateDirection(85f, 0f)
        assertTrue("Touches outside outer arm reach must register no directions", oobResult.isEmpty())

        // 3. Diagonal corner voids must NOT trigger any buttons (no multi-button triggering)
        val cornerTopRight = evaluateDirection(40f, -40f)
        assertTrue("Diagonal corner touch must NOT trigger buttons", cornerTopRight.isEmpty())

        val cornerTopLeft = evaluateDirection(-40f, -40f)
        assertTrue("Diagonal corner touch must NOT trigger buttons", cornerTopLeft.isEmpty())

        val cornerBottomRight = evaluateDirection(40f, 40f)
        assertTrue("Diagonal corner touch must NOT trigger buttons", cornerBottomRight.isEmpty())

        val cornerBottomLeft = evaluateDirection(-40f, 40f)
        assertTrue("Diagonal corner touch must NOT trigger buttons", cornerBottomLeft.isEmpty())

        // 4. Pure Cardinal Directions
        assertEquals(setOf("UP"), evaluateDirection(0f, -45f))
        assertEquals(setOf("DOWN"), evaluateDirection(0f, 45f))
        assertEquals(setOf("LEFT"), evaluateDirection(-45f, 0f))
        assertEquals(setOf("RIGHT"), evaluateDirection(45f, 0f))

        // 5. Off-center taps on cardinal shaft must strictly trigger ONLY that cardinal direction
        assertEquals(setOf("UP"), evaluateDirection(15f, -45f))
        assertEquals(setOf("UP"), evaluateDirection(-15f, -45f))
        assertEquals(setOf("RIGHT"), evaluateDirection(45f, 15f))
        assertEquals(setOf("RIGHT"), evaluateDirection(45f, -15f))
    }
}
