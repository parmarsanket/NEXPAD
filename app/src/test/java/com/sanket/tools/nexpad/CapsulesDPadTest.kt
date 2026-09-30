package com.sanket.tools.nexpad

import androidx.compose.ui.geometry.Offset
import com.sanket.tools.nexpad.category.ControlKey
import com.sanket.tools.nexpad.model.NexpadKeys as K
import com.sanket.tools.nexpad.runtime.registry.DefaultNativeFamily
import com.sanket.tools.nexpad.runtime.registry.NativeComponentRegistry
import com.sanket.tools.nexpad.ui.components.controller.calculateCapsulesTilt
import com.sanket.tools.nexpad.ui.components.controller.resolveCapsulesTouch
import com.sanket.tools.nexpad.ui.studio.model.ButtonStudioType
import com.sanket.tools.nexpad.ui.studio.model.resolveButtonSourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Mathematical, kinematic, and behavioral unit tests for Capsules D-Pad (CapsulesDPad):
 * - 3D physical rocker tilt kinematics:
 *   - UP: rx = +8°, DOWN: rx = -8°
 *   - RIGHT: ry = +8°, LEFT: ry = -8°
 *   - Compound diagonal tilts and opposing cancellations
 * - 8-Way directional touch resolution (cardinals + diagonals)
 * - Central hub deadzone (16dp) and outer boundary containment (85dp)
 * - Registry integration, native classification, and variant resolution (seed 4104)
 */
class CapsulesDPadTest {

    @Test
    fun test3DPhysicalRockerTiltKinematics() {
        // 1. Neutral: rx = 0, ry = 0
        val (neutralX, neutralY) = calculateCapsulesTilt(emptySet())
        assertEquals(0f, neutralX, 0.001f)
        assertEquals(0f, neutralY, 0.001f)

        // 2. Pure Cardinal Directions
        val (upX, upY) = calculateCapsulesTilt(setOf(K.UP))
        assertEquals(8f, upX, 0.001f)
        assertEquals(0f, upY, 0.001f)

        val (downX, downY) = calculateCapsulesTilt(setOf(K.DOWN))
        assertEquals(-8f, downX, 0.001f)
        assertEquals(0f, downY, 0.001f)

        val (rightX, rightY) = calculateCapsulesTilt(setOf(K.RIGHT))
        assertEquals(0f, rightX, 0.001f)
        assertEquals(8f, rightY, 0.001f)

        val (leftX, leftY) = calculateCapsulesTilt(setOf(K.LEFT))
        assertEquals(0f, leftX, 0.001f)
        assertEquals(-8f, leftY, 0.001f)

        // 3. Diagonal Chords (Compound Tilt)
        val (upRightX, upRightY) = calculateCapsulesTilt(setOf(K.UP, K.RIGHT))
        assertEquals(8f, upRightX, 0.001f)
        assertEquals(8f, upRightY, 0.001f)

        val (downLeftX, downLeftY) = calculateCapsulesTilt(setOf(K.DOWN, K.LEFT))
        assertEquals(-8f, downLeftX, 0.001f)
        assertEquals(-8f, downLeftY, 0.001f)

        val (upLeftX, upLeftY) = calculateCapsulesTilt(setOf(K.UP, K.LEFT))
        assertEquals(8f, upLeftX, 0.001f)
        assertEquals(-8f, upLeftY, 0.001f)

        val (downRightX, downRightY) = calculateCapsulesTilt(setOf(K.DOWN, K.RIGHT))
        assertEquals(-8f, downRightX, 0.001f)
        assertEquals(8f, downRightY, 0.001f)

        // 4. Opposing directions cancel each other
        val (cancelX, cancelY) = calculateCapsulesTilt(setOf(K.UP, K.DOWN))
        assertEquals(0f, cancelX, 0.001f)
        assertEquals(0f, cancelY, 0.001f)

        val (cancelHorizontalX, cancelHorizontalY) = calculateCapsulesTilt(setOf(K.LEFT, K.RIGHT))
        assertEquals(0f, cancelHorizontalX, 0.001f)
        assertEquals(0f, cancelHorizontalY, 0.001f)
    }

    @Test
    fun testCapsulesTouchResolution() {
        val stageSize = 170f
        val center = 85f

        // 1. Central Hub Deadzone check (< 16dp from center)
        val deadzoneTouch = resolveCapsulesTouch(Offset(center + 5f, center + 5f), stageSize)
        assertTrue("Touches within center deadzone must register no directions", deadzoneTouch.isEmpty())

        val deadzoneCenterTouch = resolveCapsulesTouch(Offset(center, center), stageSize)
        assertTrue("Exact center touch must register no directions", deadzoneCenterTouch.isEmpty())

        // 2. Out-of-bounds check (> 85dp from center)
        val oobTouch = resolveCapsulesTouch(Offset(center + 90f, center), stageSize)
        assertTrue("Touches outside 170dp boundary must register no directions", oobTouch.isEmpty())

        // 3. Pure Cardinal Keys Taps (centers of the capsules)
        // UP capsule center: (85, 34)
        assertEquals(setOf(K.UP), resolveCapsulesTouch(Offset(center, 34f), stageSize))

        // DOWN capsule center: (85, 136)
        assertEquals(setOf(K.DOWN), resolveCapsulesTouch(Offset(center, 136f), stageSize))

        // LEFT capsule center: (34, 85)
        assertEquals(setOf(K.LEFT), resolveCapsulesTouch(Offset(34f, center), stageSize))

        // RIGHT capsule center: (136, 85)
        assertEquals(setOf(K.RIGHT), resolveCapsulesTouch(Offset(136f, center), stageSize))

        // 4. Diagonal Zone Taps
        assertEquals(setOf(K.UP, K.RIGHT), resolveCapsulesTouch(Offset(center + 40f, center - 40f), stageSize))
        assertEquals(setOf(K.DOWN, K.RIGHT), resolveCapsulesTouch(Offset(center + 40f, center + 40f), stageSize))
        assertEquals(setOf(K.DOWN, K.LEFT), resolveCapsulesTouch(Offset(center - 40f, center + 40f), stageSize))
        assertEquals(setOf(K.UP, K.LEFT), resolveCapsulesTouch(Offset(center - 40f, center - 40f), stageSize))
    }

    @Test
    fun testCapsulesDPadRegistryIntegration() {
        // 1. Registered in DefaultNativeFamily
        val capDpad = DefaultNativeFamily.getVariant("builtin.capsules_dpad")
        assertNotNull("Capsules D-Pad must be registered in DefaultNativeFamily", capDpad)
        assertEquals(ControlKey.DPAD, capDpad!!.controlKey)
        assertEquals(4104, capDpad.seedCode)
        assertFalse(capDpad.isBaselineDefault)
        assertEquals("Lens Capsules", capDpad.variantName)

        // 2. Resolved via NativeComponentRegistry
        val resolved = NativeComponentRegistry.resolveVariant("DPAD", "builtin.capsules_dpad")
        assertNotNull(resolved)
        assertEquals(4104, resolved!!.seedCode)

        // 3. Built-in check
        assertTrue(NativeComponentRegistry.isNativeBuiltin("builtin.capsules_dpad"))

        // 4. Studio Type resolution
        assertEquals(ButtonStudioType.DEFAULT, resolveButtonSourceType("builtin.capsules_dpad"))
    }
}
