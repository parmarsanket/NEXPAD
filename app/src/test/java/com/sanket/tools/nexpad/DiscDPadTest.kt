package com.sanket.tools.nexpad

import androidx.compose.ui.geometry.Offset
import com.sanket.tools.nexpad.category.ControlKey
import com.sanket.tools.nexpad.model.NexpadKeys as K
import com.sanket.tools.nexpad.runtime.registry.DefaultNativeFamily
import com.sanket.tools.nexpad.runtime.registry.NativeComponentRegistry
import com.sanket.tools.nexpad.ui.components.controller.calculateDiscGateAngle
import com.sanket.tools.nexpad.ui.components.controller.calculateDiscPuckOffset
import com.sanket.tools.nexpad.ui.components.controller.calculateDiscTilt
import com.sanket.tools.nexpad.ui.components.controller.resolveDiscTouch
import com.sanket.tools.nexpad.ui.studio.model.ButtonStudioType
import com.sanket.tools.nexpad.ui.studio.model.resolveButtonSourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Mathematical, kinematic, and behavioral unit tests for Disc D-Pad (DiscDPad):
 * - 3D physical rocker tilt kinematics:
 *   - UP: rx = +8°, DOWN: rx = -8°
 *   - RIGHT: ry = +8°, LEFT: ry = -8°
 *   - Compound diagonal tilts and opposing cancellations
 * - Sliding puck displacement offset: (vx * 32dp, vy * 32dp)
 * - Dynamic directional gate arc angle: 0° UP, 90° RIGHT, 180° DOWN, 270° LEFT, diagonals
 * - 8-Way directional touch resolution (cardinals + diagonals)
 * - Central deadzone (10dp) and outer boundary containment (80dp)
 * - Registry integration, native classification, and variant resolution
 */
class DiscDPadTest {

    @Test
    fun test3DPhysicalRockerTiltKinematics() {
        // 1. Neutral: rx = 0, ry = 0
        val (neutralX, neutralY) = calculateDiscTilt(emptySet())
        assertEquals(0f, neutralX, 0.001f)
        assertEquals(0f, neutralY, 0.001f)

        // 2. Pure Cardinal Directions
        val (upX, upY) = calculateDiscTilt(setOf(K.UP))
        assertEquals(8f, upX, 0.001f)
        assertEquals(0f, upY, 0.001f)

        val (downX, downY) = calculateDiscTilt(setOf(K.DOWN))
        assertEquals(-8f, downX, 0.001f)
        assertEquals(0f, downY, 0.001f)

        val (rightX, rightY) = calculateDiscTilt(setOf(K.RIGHT))
        assertEquals(0f, rightX, 0.001f)
        assertEquals(8f, rightY, 0.001f)

        val (leftX, leftY) = calculateDiscTilt(setOf(K.LEFT))
        assertEquals(0f, leftX, 0.001f)
        assertEquals(-8f, leftY, 0.001f)

        // 3. Diagonal Chords (Compound Tilt)
        val (upRightX, upRightY) = calculateDiscTilt(setOf(K.UP, K.RIGHT))
        assertEquals(8f, upRightX, 0.001f)
        assertEquals(8f, upRightY, 0.001f)

        val (downLeftX, downLeftY) = calculateDiscTilt(setOf(K.DOWN, K.LEFT))
        assertEquals(-8f, downLeftX, 0.001f)
        assertEquals(-8f, downLeftY, 0.001f)

        val (upLeftX, upLeftY) = calculateDiscTilt(setOf(K.UP, K.LEFT))
        assertEquals(8f, upLeftX, 0.001f)
        assertEquals(-8f, upLeftY, 0.001f)

        val (downRightX, downRightY) = calculateDiscTilt(setOf(K.DOWN, K.RIGHT))
        assertEquals(-8f, downRightX, 0.001f)
        assertEquals(8f, downRightY, 0.001f)

        // 4. Opposing directions cancel each other
        val (cancelX, cancelY) = calculateDiscTilt(setOf(K.UP, K.DOWN))
        assertEquals(0f, cancelX, 0.001f)
        assertEquals(0f, cancelY, 0.001f)

        val (cancelHorizontalX, cancelHorizontalY) = calculateDiscTilt(setOf(K.LEFT, K.RIGHT))
        assertEquals(0f, cancelHorizontalX, 0.001f)
        assertEquals(0f, cancelHorizontalY, 0.001f)
    }

    @Test
    fun testPuckDisplacementKinematics() {
        // Neutral: puck rests at center (0, 0)
        val (neutralX, neutralY) = calculateDiscPuckOffset(emptySet())
        assertEquals(0f, neutralX, 0.001f)
        assertEquals(0f, neutralY, 0.001f)

        // UP: slides 32dp UP (vy = -1 -> y = -32dp)
        val (upX, upY) = calculateDiscPuckOffset(setOf(K.UP))
        assertEquals(0f, upX, 0.001f)
        assertEquals(-32f, upY, 0.001f)

        // DOWN: slides 32dp DOWN (vy = +1 -> y = +32dp)
        val (downX, downY) = calculateDiscPuckOffset(setOf(K.DOWN))
        assertEquals(0f, downX, 0.001f)
        assertEquals(32f, downY, 0.001f)

        // RIGHT: slides 32dp RIGHT (vx = +1 -> x = +32dp)
        val (rightX, rightY) = calculateDiscPuckOffset(setOf(K.RIGHT))
        assertEquals(32f, rightX, 0.001f)
        assertEquals(0f, rightY, 0.001f)

        // LEFT: slides 32dp LEFT (vx = -1 -> x = -32dp)
        val (leftX, leftY) = calculateDiscPuckOffset(setOf(K.LEFT))
        assertEquals(-32f, leftX, 0.001f)
        assertEquals(0f, leftY, 0.001f)

        // Diagonal: UP + RIGHT -> (+32dp, -32dp)
        val (upRightX, upRightY) = calculateDiscPuckOffset(setOf(K.UP, K.RIGHT))
        assertEquals(32f, upRightX, 0.001f)
        assertEquals(-32f, upRightY, 0.001f)

        // Diagonal: DOWN + LEFT -> (-32dp, +32dp)
        val (downLeftX, downLeftY) = calculateDiscPuckOffset(setOf(K.DOWN, K.LEFT))
        assertEquals(-32f, downLeftX, 0.001f)
        assertEquals(32f, downLeftY, 0.001f)
    }

    @Test
    fun testDirectionalGateAngleCalculations() {
        // Neutral: no gate angle
        assertNull(calculateDiscGateAngle(emptySet()))

        // UP: 0°
        assertEquals(0f, calculateDiscGateAngle(setOf(K.UP))!!, 0.001f)

        // RIGHT: 90°
        assertEquals(90f, calculateDiscGateAngle(setOf(K.RIGHT))!!, 0.001f)

        // DOWN: 180°
        assertEquals(180f, calculateDiscGateAngle(setOf(K.DOWN))!!, 0.001f)

        // LEFT: 270°
        assertEquals(270f, calculateDiscGateAngle(setOf(K.LEFT))!!, 0.001f)

        // UP + RIGHT: 45°
        assertEquals(45f, calculateDiscGateAngle(setOf(K.UP, K.RIGHT))!!, 0.001f)

        // DOWN + RIGHT: 135°
        assertEquals(135f, calculateDiscGateAngle(setOf(K.DOWN, K.RIGHT))!!, 0.001f)

        // DOWN + LEFT: 225°
        assertEquals(225f, calculateDiscGateAngle(setOf(K.DOWN, K.LEFT))!!, 0.001f)

        // UP + LEFT: 315°
        assertEquals(315f, calculateDiscGateAngle(setOf(K.UP, K.LEFT))!!, 0.001f)
    }

    @Test
    fun testDiscTouchResolution() {
        val stageSize = 160f
        val center = 80f

        // 1. Central Hub Deadzone check (< 10dp from center)
        val deadzoneTouch = resolveDiscTouch(Offset(center + 3f, center + 3f), stageSize)
        assertTrue("Touches within center deadzone must register no directions", deadzoneTouch.isEmpty())

        val deadzoneCenterTouch = resolveDiscTouch(Offset(center, center), stageSize)
        assertTrue("Exact center touch must register no directions", deadzoneCenterTouch.isEmpty())

        // 2. Out-of-bounds check (> 80dp from center)
        val oobTouch = resolveDiscTouch(Offset(center + 85f, center), stageSize)
        assertTrue("Touches outside 160dp boundary must register no directions", oobTouch.isEmpty())

        // 3. Pure Cardinal Keys Taps
        assertEquals(setOf(K.UP), resolveDiscTouch(Offset(center, 25f), stageSize))
        assertEquals(setOf(K.DOWN), resolveDiscTouch(Offset(center, 135f), stageSize))
        assertEquals(setOf(K.LEFT), resolveDiscTouch(Offset(25f, center), stageSize))
        assertEquals(setOf(K.RIGHT), resolveDiscTouch(Offset(135f, center), stageSize))

        // 4. Diagonal Zone Taps
        assertEquals(setOf(K.UP, K.RIGHT), resolveDiscTouch(Offset(center + 35f, center - 35f), stageSize))
        assertEquals(setOf(K.DOWN, K.RIGHT), resolveDiscTouch(Offset(center + 35f, center + 35f), stageSize))
        assertEquals(setOf(K.DOWN, K.LEFT), resolveDiscTouch(Offset(center - 35f, center + 35f), stageSize))
        assertEquals(setOf(K.UP, K.LEFT), resolveDiscTouch(Offset(center - 35f, center - 35f), stageSize))
    }

    @Test
    fun testDiscDPadRegistryIntegration() {
        // 1. Registered in DefaultNativeFamily
        val discDpad = DefaultNativeFamily.getVariant("builtin.disc_dpad")
        assertNotNull("Disc D-Pad must be registered in DefaultNativeFamily", discDpad)
        assertEquals(ControlKey.DPAD, discDpad!!.controlKey)
        assertEquals(4103, discDpad.seedCode)
        assertFalse(discDpad.isBaselineDefault)
        assertEquals("Lens Disc", discDpad.variantName)

        // 2. Resolved via NativeComponentRegistry
        val resolved = NativeComponentRegistry.resolveVariant("DPAD", "builtin.disc_dpad")
        assertNotNull(resolved)
        assertEquals(4103, resolved!!.seedCode)

        // 3. Built-in check
        assertTrue(NativeComponentRegistry.isNativeBuiltin("builtin.disc_dpad"))

        // 4. Studio Type resolution
        assertEquals(ButtonStudioType.DEFAULT, resolveButtonSourceType("builtin.disc_dpad"))
    }
}
