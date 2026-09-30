package com.sanket.tools.nexpad

import androidx.compose.ui.geometry.Offset
import com.sanket.tools.nexpad.category.ControlKey
import com.sanket.tools.nexpad.model.NexpadKeys as K
import com.sanket.tools.nexpad.runtime.registry.DefaultNativeFamily
import com.sanket.tools.nexpad.runtime.registry.NativeComponentRegistry
import com.sanket.tools.nexpad.ui.components.controller.calculateRailsPuckOffset
import com.sanket.tools.nexpad.ui.components.controller.calculateRailsTilt
import com.sanket.tools.nexpad.ui.components.controller.resolveRailsTouch
import com.sanket.tools.nexpad.ui.studio.model.ButtonStudioType
import com.sanket.tools.nexpad.ui.studio.model.resolveButtonSourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Mathematical, kinematic, and behavioral unit tests for Rails D-Pad (RailsDPad):
 * - 3D physical rocker tilt kinematics:
 *   - UP: rx = +8°, DOWN: rx = -8°
 *   - RIGHT: ry = +8°, LEFT: ry = -8°
 *   - Compound diagonal tilts and opposing cancellations
 * - Sliding puck displacement offset calculation (52dp along active rail axes)
 * - 8-Way directional touch resolution (cardinals + diagonals)
 * - Central hub deadzone (14dp) and outer boundary containment (80dp)
 * - Registry integration, native classification, and variant resolution (seed 4106)
 */
class RailsDPadTest {

    @Test
    fun test3DPhysicalRockerTiltKinematics() {
        // 1. Neutral: rx = 0, ry = 0
        val (neutralX, neutralY) = calculateRailsTilt(emptySet())
        assertEquals(0f, neutralX, 0.001f)
        assertEquals(0f, neutralY, 0.001f)

        // 2. Pure Cardinal Directions
        val (upX, upY) = calculateRailsTilt(setOf(K.UP))
        assertEquals(8f, upX, 0.001f)
        assertEquals(0f, upY, 0.001f)

        val (downX, downY) = calculateRailsTilt(setOf(K.DOWN))
        assertEquals(-8f, downX, 0.001f)
        assertEquals(0f, downY, 0.001f)

        val (rightX, rightY) = calculateRailsTilt(setOf(K.RIGHT))
        assertEquals(0f, rightX, 0.001f)
        assertEquals(8f, rightY, 0.001f)

        val (leftX, leftY) = calculateRailsTilt(setOf(K.LEFT))
        assertEquals(0f, leftX, 0.001f)
        assertEquals(-8f, leftY, 0.001f)

        // 3. Diagonal Chords (Compound Tilt)
        val (upRightX, upRightY) = calculateRailsTilt(setOf(K.UP, K.RIGHT))
        assertEquals(8f, upRightX, 0.001f)
        assertEquals(8f, upRightY, 0.001f)

        val (downLeftX, downLeftY) = calculateRailsTilt(setOf(K.DOWN, K.LEFT))
        assertEquals(-8f, downLeftX, 0.001f)
        assertEquals(-8f, downLeftY, 0.001f)

        val (upLeftX, upLeftY) = calculateRailsTilt(setOf(K.UP, K.LEFT))
        assertEquals(8f, upLeftX, 0.001f)
        assertEquals(-8f, upLeftY, 0.001f)

        val (downRightX, downRightY) = calculateRailsTilt(setOf(K.DOWN, K.RIGHT))
        assertEquals(-8f, downRightX, 0.001f)
        assertEquals(8f, downRightY, 0.001f)

        // 4. Opposing directions cancel each other
        val (cancelX, cancelY) = calculateRailsTilt(setOf(K.UP, K.DOWN))
        assertEquals(0f, cancelX, 0.001f)
        assertEquals(0f, cancelY, 0.001f)

        val (cancelHorizontalX, cancelHorizontalY) = calculateRailsTilt(setOf(K.LEFT, K.RIGHT))
        assertEquals(0f, cancelHorizontalX, 0.001f)
        assertEquals(0f, cancelHorizontalY, 0.001f)
    }

    @Test
    fun testPuckDisplacementKinematics() {
        // 1. Neutral: (0, 0)
        val (neutralX, neutralY) = calculateRailsPuckOffset(emptySet())
        assertEquals(0f, neutralX, 0.001f)
        assertEquals(0f, neutralY, 0.001f)

        // 2. Cardinal Travel (52dp along rail)
        val (upX, upY) = calculateRailsPuckOffset(setOf(K.UP))
        assertEquals(0f, upX, 0.001f)
        assertEquals(-52f, upY, 0.001f)

        val (downX, downY) = calculateRailsPuckOffset(setOf(K.DOWN))
        assertEquals(0f, downX, 0.001f)
        assertEquals(52f, downY, 0.001f)

        val (leftX, leftY) = calculateRailsPuckOffset(setOf(K.LEFT))
        assertEquals(-52f, leftX, 0.001f)
        assertEquals(0f, leftY, 0.001f)

        val (rightX, rightY) = calculateRailsPuckOffset(setOf(K.RIGHT))
        assertEquals(52f, rightX, 0.001f)
        assertEquals(0f, rightY, 0.001f)

        // 3. Diagonal Travel (52dp on both axes)
        val (upRightX, upRightY) = calculateRailsPuckOffset(setOf(K.UP, K.RIGHT))
        assertEquals(52f, upRightX, 0.001f)
        assertEquals(-52f, upRightY, 0.001f)

        val (downLeftX, downLeftY) = calculateRailsPuckOffset(setOf(K.DOWN, K.LEFT))
        assertEquals(-52f, downLeftX, 0.001f)
        assertEquals(52f, downLeftY, 0.001f)

        // 4. Opposing directions cancel each other
        val (cancelX, cancelY) = calculateRailsPuckOffset(setOf(K.UP, K.DOWN))
        assertEquals(0f, cancelX, 0.001f)
        assertEquals(0f, cancelY, 0.001f)

        val (cancelHX, cancelHY) = calculateRailsPuckOffset(setOf(K.LEFT, K.RIGHT))
        assertEquals(0f, cancelHX, 0.001f)
        assertEquals(0f, cancelHY, 0.001f)
    }

    @Test
    fun testRailsTouchResolution() {
        val stageSize = 160f
        val center = 80f

        // 1. Central Deadzone check (< 14dp from center)
        val deadzoneTouch = resolveRailsTouch(Offset(center + 5f, center + 5f), stageSize)
        assertTrue("Touches within center deadzone must register no directions", deadzoneTouch.isEmpty())

        val deadzoneCenterTouch = resolveRailsTouch(Offset(center, center), stageSize)
        assertTrue("Exact center touch must register no directions", deadzoneCenterTouch.isEmpty())

        // 2. Out-of-bounds check (> 80dp from center)
        val oobTouch = resolveRailsTouch(Offset(center + 85f, center), stageSize)
        assertTrue("Touches outside 160dp boundary must register no directions", oobTouch.isEmpty())

        // 3. Pure Cardinal Keys Taps (along the rails)
        // UP touch: (80, 30)
        assertEquals(setOf(K.UP), resolveRailsTouch(Offset(center, 30f), stageSize))

        // DOWN touch: (80, 130)
        assertEquals(setOf(K.DOWN), resolveRailsTouch(Offset(center, 130f), stageSize))

        // LEFT touch: (30, 80)
        assertEquals(setOf(K.LEFT), resolveRailsTouch(Offset(30f, center), stageSize))

        // RIGHT touch: (130, 80)
        assertEquals(setOf(K.RIGHT), resolveRailsTouch(Offset(130f, center), stageSize))

        // 4. Diagonal Zone Taps
        assertEquals(setOf(K.UP, K.RIGHT), resolveRailsTouch(Offset(center + 40f, center - 40f), stageSize))
        assertEquals(setOf(K.DOWN, K.RIGHT), resolveRailsTouch(Offset(center + 40f, center + 40f), stageSize))
        assertEquals(setOf(K.DOWN, K.LEFT), resolveRailsTouch(Offset(center - 40f, center + 40f), stageSize))
        assertEquals(setOf(K.UP, K.LEFT), resolveRailsTouch(Offset(center - 40f, center - 40f), stageSize))
    }

    @Test
    fun testRailsDPadRegistryIntegration() {
        // 1. Registered in DefaultNativeFamily
        val railsDpad = DefaultNativeFamily.getVariant("builtin.rails_dpad")
        assertNotNull("Rails D-Pad must be registered in DefaultNativeFamily", railsDpad)
        assertEquals(ControlKey.DPAD, railsDpad!!.controlKey)
        assertEquals(4106, railsDpad.seedCode)
        assertFalse(railsDpad.isBaselineDefault)
        assertEquals("Lens Rails", railsDpad.variantName)

        // 2. Resolved via NativeComponentRegistry
        val resolved = NativeComponentRegistry.resolveVariant("DPAD", "builtin.rails_dpad")
        assertNotNull(resolved)
        assertEquals(4106, resolved!!.seedCode)

        // 3. Built-in check
        assertTrue(NativeComponentRegistry.isNativeBuiltin("builtin.rails_dpad"))

        // 4. Studio Type resolution
        assertEquals(ButtonStudioType.DEFAULT, resolveButtonSourceType("builtin.rails_dpad"))
    }
}
