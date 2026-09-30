package com.sanket.tools.nexpad

import androidx.compose.ui.geometry.Offset
import com.sanket.tools.nexpad.category.ControlKey
import com.sanket.tools.nexpad.model.NexpadKeys as K
import com.sanket.tools.nexpad.runtime.registry.DefaultNativeFamily
import com.sanket.tools.nexpad.runtime.registry.NativeComponentRegistry
import com.sanket.tools.nexpad.ui.components.controller.calculateMetaballsTilt
import com.sanket.tools.nexpad.ui.components.controller.calculateMetaballSatelliteOffset
import com.sanket.tools.nexpad.ui.components.controller.resolveMetaballsTouch
import com.sanket.tools.nexpad.ui.studio.model.ButtonStudioType
import com.sanket.tools.nexpad.ui.studio.model.resolveButtonSourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Mathematical, kinematic, and behavioral unit tests for Metaballs D-Pad (MetaballsDPad):
 * - 3D physical rocker tilt kinematics:
 *   - UP: rx = +8°, DOWN: rx = -8°
 *   - RIGHT: ry = +8°, LEFT: ry = -8°
 *   - Compound diagonal tilts and opposing cancellations
 * - Dynamic satellite inward spring retraction (58dp -> ~31.9dp on press)
 * - 8-Way directional touch resolution (cardinals + diagonals)
 * - Central hub deadzone (14dp) and outer boundary containment (86dp)
 * - Registry integration, native classification, and variant resolution (seed 4105)
 */
class MetaballsDPadTest {

    @Test
    fun test3DPhysicalRockerTiltKinematics() {
        // 1. Neutral: rx = 0, ry = 0
        val (neutralX, neutralY) = calculateMetaballsTilt(emptySet())
        assertEquals(0f, neutralX, 0.001f)
        assertEquals(0f, neutralY, 0.001f)

        // 2. Pure Cardinal Directions
        val (upX, upY) = calculateMetaballsTilt(setOf(K.UP))
        assertEquals(8f, upX, 0.001f)
        assertEquals(0f, upY, 0.001f)

        val (downX, downY) = calculateMetaballsTilt(setOf(K.DOWN))
        assertEquals(-8f, downX, 0.001f)
        assertEquals(0f, downY, 0.001f)

        val (rightX, rightY) = calculateMetaballsTilt(setOf(K.RIGHT))
        assertEquals(0f, rightX, 0.001f)
        assertEquals(8f, rightY, 0.001f)

        val (leftX, leftY) = calculateMetaballsTilt(setOf(K.LEFT))
        assertEquals(0f, leftX, 0.001f)
        assertEquals(-8f, leftY, 0.001f)

        // 3. Diagonal Chords (Compound Tilt)
        val (upRightX, upRightY) = calculateMetaballsTilt(setOf(K.UP, K.RIGHT))
        assertEquals(8f, upRightX, 0.001f)
        assertEquals(8f, upRightY, 0.001f)

        val (downLeftX, downLeftY) = calculateMetaballsTilt(setOf(K.DOWN, K.LEFT))
        assertEquals(-8f, downLeftX, 0.001f)
        assertEquals(-8f, downLeftY, 0.001f)

        val (upLeftX, upLeftY) = calculateMetaballsTilt(setOf(K.UP, K.LEFT))
        assertEquals(8f, upLeftX, 0.001f)
        assertEquals(-8f, upLeftY, 0.001f)

        val (downRightX, downRightY) = calculateMetaballsTilt(setOf(K.DOWN, K.RIGHT))
        assertEquals(-8f, downRightX, 0.001f)
        assertEquals(8f, downRightY, 0.001f)

        // 4. Opposing directions cancel each other
        val (cancelX, cancelY) = calculateMetaballsTilt(setOf(K.UP, K.DOWN))
        assertEquals(0f, cancelX, 0.001f)
        assertEquals(0f, cancelY, 0.001f)

        val (cancelHorizontalX, cancelHorizontalY) = calculateMetaballsTilt(setOf(K.LEFT, K.RIGHT))
        assertEquals(0f, cancelHorizontalX, 0.001f)
        assertEquals(0f, cancelHorizontalY, 0.001f)
    }

    @Test
    fun testSatelliteOffsetAndInwardRetraction() {
        // Idle satellites at base distance 58dp
        val (upIdleX, upIdleY) = calculateMetaballSatelliteOffset(K.UP, isPressed = false)
        assertEquals(0f, upIdleX, 0.001f)
        assertEquals(-58f, upIdleY, 0.001f)

        val (downIdleX, downIdleY) = calculateMetaballSatelliteOffset(K.DOWN, isPressed = false)
        assertEquals(0f, downIdleX, 0.001f)
        assertEquals(58f, downIdleY, 0.001f)

        val (leftIdleX, leftIdleY) = calculateMetaballSatelliteOffset(K.LEFT, isPressed = false)
        assertEquals(-58f, leftIdleX, 0.001f)
        assertEquals(0f, leftIdleY, 0.001f)

        val (rightIdleX, rightIdleY) = calculateMetaballSatelliteOffset(K.RIGHT, isPressed = false)
        assertEquals(58f, rightIdleX, 0.001f)
        assertEquals(0f, rightIdleY, 0.001f)

        // Pressed satellites retract inward by 45% (to 58 * 0.55 = 31.9dp)
        val expectedRetractedDist = 58f * 0.55f

        val (upPressedX, upPressedY) = calculateMetaballSatelliteOffset(K.UP, isPressed = true)
        assertEquals(0f, upPressedX, 0.001f)
        assertEquals(-expectedRetractedDist, upPressedY, 0.001f)

        val (downPressedX, downPressedY) = calculateMetaballSatelliteOffset(K.DOWN, isPressed = true)
        assertEquals(0f, downPressedX, 0.001f)
        assertEquals(expectedRetractedDist, downPressedY, 0.001f)

        val (leftPressedX, leftPressedY) = calculateMetaballSatelliteOffset(K.LEFT, isPressed = true)
        assertEquals(-expectedRetractedDist, leftPressedX, 0.001f)
        assertEquals(0f, leftPressedY, 0.001f)

        val (rightPressedX, rightPressedY) = calculateMetaballSatelliteOffset(K.RIGHT, isPressed = true)
        assertEquals(expectedRetractedDist, rightPressedX, 0.001f)
        assertEquals(0f, rightPressedY, 0.001f)
    }

    @Test
    fun testMetaballsTouchResolution() {
        val stageSize = 172f
        val center = 86f

        // 1. Central Deadzone check (< 14dp from center)
        val deadzoneTouch = resolveMetaballsTouch(Offset(center + 5f, center + 5f), stageSize)
        assertTrue("Touches within center deadzone must register no directions", deadzoneTouch.isEmpty())

        val deadzoneCenterTouch = resolveMetaballsTouch(Offset(center, center), stageSize)
        assertTrue("Exact center touch must register no directions", deadzoneCenterTouch.isEmpty())

        // 2. Out-of-bounds check (> 86dp from center)
        val oobTouch = resolveMetaballsTouch(Offset(center + 90f, center), stageSize)
        assertTrue("Touches outside 172dp boundary must register no directions", oobTouch.isEmpty())

        // 3. Pure Cardinal Keys Taps (centers of the satellites at 58dp distance)
        // UP satellite center: (86, 28)
        assertEquals(setOf(K.UP), resolveMetaballsTouch(Offset(center, center - 58f), stageSize))

        // DOWN satellite center: (86, 144)
        assertEquals(setOf(K.DOWN), resolveMetaballsTouch(Offset(center, center + 58f), stageSize))

        // LEFT satellite center: (28, 86)
        assertEquals(setOf(K.LEFT), resolveMetaballsTouch(Offset(center - 58f, center), stageSize))

        // RIGHT satellite center: (144, 86)
        assertEquals(setOf(K.RIGHT), resolveMetaballsTouch(Offset(center + 58f, center), stageSize))

        // 4. Diagonal Zone Taps
        assertEquals(setOf(K.UP, K.RIGHT), resolveMetaballsTouch(Offset(center + 40f, center - 40f), stageSize))
        assertEquals(setOf(K.DOWN, K.RIGHT), resolveMetaballsTouch(Offset(center + 40f, center + 40f), stageSize))
        assertEquals(setOf(K.DOWN, K.LEFT), resolveMetaballsTouch(Offset(center - 40f, center + 40f), stageSize))
        assertEquals(setOf(K.UP, K.LEFT), resolveMetaballsTouch(Offset(center - 40f, center - 40f), stageSize))
    }

    @Test
    fun testMetaballsDPadRegistryIntegration() {
        // 1. Registered in DefaultNativeFamily
        val metaDpad = DefaultNativeFamily.getVariant("builtin.metaballs_dpad")
        assertNotNull("Metaballs D-Pad must be registered in DefaultNativeFamily", metaDpad)
        assertEquals(ControlKey.DPAD, metaDpad!!.controlKey)
        assertEquals(4105, metaDpad.seedCode)
        assertFalse(metaDpad.isBaselineDefault)
        assertEquals("Lens Metaballs", metaDpad.variantName)

        // 2. Resolved via NativeComponentRegistry
        val resolved = NativeComponentRegistry.resolveVariant("DPAD", "builtin.metaballs_dpad")
        assertNotNull(resolved)
        assertEquals(4105, resolved!!.seedCode)

        // 3. Built-in check
        assertTrue(NativeComponentRegistry.isNativeBuiltin("builtin.metaballs_dpad"))

        // 4. Studio Type resolution
        assertEquals(ButtonStudioType.DEFAULT, resolveButtonSourceType("builtin.metaballs_dpad"))
    }
}
