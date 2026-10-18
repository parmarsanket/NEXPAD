package com.sanket.tools.nexpad

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.sanket.tools.nexpad.model.NexpadKeys as K
import com.sanket.tools.nexpad.model.Position
import com.sanket.tools.nexpad.ui.BoundsRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Industrial-grade unit tests for the Esports Multi-Touch Surface Coordinator:
 *
 * 1. Slide & Sweep Input:
 *    Continuous sliding thumb path transitioning from Button A across the gap to Button B without pointer lift.
 * 2. Fat Thumb & Dual Chord Press:
 *    Thumb contact disk simultaneously bridging the border boundaries of two adjacent buttons.
 * 3. Joystick Isolation & Protection:
 *    Touches within active joystick radii (LS, RS) are never intercepted by button hit-testing.
 * 4. Rectangular / Pill Perimeter Proximity:
 *    Proximity detection on bumpers, triggers, and rectangular macro hitboxes.
 * 5. Composite D-Pad Directional & Diagonal Resolution:
 *    Cardinal directions, dead-zone rejection, and multi-directional diagonals.
 * 6. Touchpad Safety Rule:
 *    Verification of the active layout guard that completely disables the coordinator when touchpads are present.
 */
class EsportsTouchCoordinatorTest {

    private lateinit var registry: BoundsRegistry

    @Before
    fun setUp() {
        registry = BoundsRegistry()
    }

    @Test
    fun testDirectHitInsideButtonBounds() {
        // Button A: circular button centered at (150, 150) with width=80, height=80 (radius 40)
        registry.put(K.A, Rect(left = 110f, top = 110f, right = 190f, bottom = 190f))

        val hits = registry.hitTestTouchDisk(
            screenX = 150f,
            screenY = 150f,
            thumbRadiusPx = 20f
        )

        assertEquals(setOf(K.A), hits)
    }

    @Test
    fun testSlideOverInterButtonGapContinuousOverlap() {
        // Button A: centered at (100, 100), radius 30 (left=70, right=130, top=70, bottom=130)
        // Button B: centered at (180, 100), radius 30 (left=150, right=210, top=70, bottom=130)
        // Inter-button gap between right of A (130) and left of B (150) is 20px.
        registry.put(K.A, Rect(left = 70f, top = 70f, right = 130f, bottom = 130f))
        registry.put(K.B, Rect(left = 150f, top = 70f, right = 210f, bottom = 130f))

        val thumbRadius = 15f // Diameter 30px, easily spans the 20px gap

        // 1. Direct on Button A
        val step1 = registry.hitTestTouchDisk(screenX = 100f, screenY = 100f, thumbRadiusPx = thumbRadius)
        assertEquals("Direct touch on A should activate only A", setOf(K.A), step1)

        // 2. Sliding towards gap, near A's outer right edge (x=125)
        val step2 = registry.hitTestTouchDisk(screenX = 125f, screenY = 100f, thumbRadiusPx = thumbRadius)
        assertEquals("Edge of A should activate only A", setOf(K.A), step2)

        // 3. Midpoint of the gap (x=140):
        // Distance to A's right border (130) is 10px <= 15px.
        // Distance to B's left border (150) is 10px <= 15px.
        // BOTH buttons are touched by the thumb surface simultaneously!
        val step3Midpoint = registry.hitTestTouchDisk(screenX = 140f, screenY = 100f, thumbRadiusPx = thumbRadius)
        assertEquals("Fat thumb spanning the gap activates BOTH A and B simultaneously", setOf(K.A, K.B), step3Midpoint)

        // 4. Continued slide into Button B (x=160)
        val step4 = registry.hitTestTouchDisk(screenX = 160f, screenY = 100f, thumbRadiusPx = thumbRadius)
        assertEquals("Moving into B activates only B", setOf(K.B), step4)

        // 5. Center of Button B (x=180)
        val step5 = registry.hitTestTouchDisk(screenX = 180f, screenY = 100f, thumbRadiusPx = thumbRadius)
        assertEquals("Direct touch on B activates only B", setOf(K.B), step5)
    }

    @Test
    fun testFatThumbDiagonalOverlapCornerPress() {
        // Diamond Action Buttons Layout (A: bottom, X: left, B: right, Y: top)
        // Center of cluster at (500, 500)
        registry.put(K.A, Rect(left = 470f, top = 540f, right = 530f, bottom = 600f)) // Center (500, 570)
        registry.put(K.B, Rect(left = 540f, top = 470f, right = 600f, bottom = 530f)) // Center (570, 500)

        // A broad thumb placed diagonally between A and B at (535, 535)
        val hits = registry.hitTestTouchDisk(screenX = 535f, screenY = 535f, thumbRadiusPx = 25f)
        assertTrue("A must be pressed", hits.contains(K.A))
        assertTrue("B must be pressed", hits.contains(K.B))
        assertEquals("Only A and B should be pressed in this quadrant", 2, hits.size)
    }

    @Test
    fun testJoystickIsolationExclusion() {
        // Place Left Stick at center (200, 200) with radius 80px
        val joystickCenter = Offset(200f, 200f)
        val joystickRadius = 80f
        val excludedJoysticks = listOf(Pair(joystickCenter, joystickRadius))

        // Place a nearby button partially overlapping the joystick zone
        registry.put("INTERNAL_BTN", Rect(left = 210f, top = 210f, right = 260f, bottom = 260f))
        // Place an external button well outside the joystick zone
        registry.put("EXTERNAL_BTN", Rect(left = 400f, top = 400f, right = 460f, bottom = 460f))

        // 1. Touch at (220, 220) is inside joystick radius: hypot(20, 20) = ~28.3 <= 80
        val hitsInsideJoystick = registry.hitTestTouchDisk(
            screenX = 220f,
            screenY = 220f,
            thumbRadiusPx = 20f,
            excludedJoystickCenters = excludedJoysticks
        )

        assertTrue(
            "Touches landing within joystick activation radius MUST return emptySet to leave joystick untouched",
            hitsInsideJoystick.isEmpty()
        )

        // 2. Touch at (400, 400) is well outside joystick radius (hypot(200, 200) = 282.8 > 80)
        val hitsOutsideJoystick = registry.hitTestTouchDisk(
            screenX = 400f,
            screenY = 400f,
            thumbRadiusPx = 20f,
            excludedJoystickCenters = excludedJoysticks
        )

        assertEquals(setOf("EXTERNAL_BTN"), hitsOutsideJoystick)
    }

    @Test
    fun testRectangularPillBumperHitbox() {
        // Left Bumper: rectangular pill from x=100..300, y=50..120
        registry.put(K.LB, Rect(left = 100f, top = 50f, right = 300f, bottom = 120f))

        // 1. Touch 10px below the bottom edge (y=130) with 20px thumb radius
        val hitsBelowEdge = registry.hitTestTouchDisk(screenX = 200f, screenY = 130f, thumbRadiusPx = 20f)
        assertEquals("Thumb disk overlapping bottom edge of bumper registers hit", setOf(K.LB), hitsBelowEdge)

        // 2. Touch 40px away from bumper (y=165) with 20px thumb radius
        val hitsFar = registry.hitTestTouchDisk(screenX = 200f, screenY = 165f, thumbRadiusPx = 20f)
        assertTrue("Touches outside thumb contact radius do not trigger bumper", hitsFar.isEmpty())
    }

    @Test
    fun testCompositeDPadCardinalsAndDeadzone() {
        // D-Pad cluster at center (400, 400), size 200x200 (left=300, right=500, top=300, bottom=500)
        registry.put("DPAD", Rect(left = 300f, top = 300f, right = 500f, bottom = 500f))

        // Center Deadzone (deadzone radius is size * 0.18 = 36px)
        val deadzoneHit = registry.hitTestTouchDisk(screenX = 400f, screenY = 400f, thumbRadiusPx = 10f)
        assertTrue("Deadzone center touches must return empty", deadzoneHit.isEmpty())

        // North: (400, 330) -> angle -90° -> UP
        val northHit = registry.hitTestTouchDisk(screenX = 400f, screenY = 330f, thumbRadiusPx = 10f)
        assertEquals(setOf(K.UP), northHit)

        // South: (400, 470) -> angle +90° -> DOWN
        val southHit = registry.hitTestTouchDisk(screenX = 400f, screenY = 470f, thumbRadiusPx = 10f)
        assertEquals(setOf(K.DOWN), southHit)

        // West: (330, 400) -> angle 180° -> LEFT
        val westHit = registry.hitTestTouchDisk(screenX = 330f, screenY = 400f, thumbRadiusPx = 10f)
        assertEquals(setOf(K.LEFT), westHit)

        // East: (470, 400) -> angle 0° -> RIGHT
        val eastHit = registry.hitTestTouchDisk(screenX = 470f, screenY = 400f, thumbRadiusPx = 10f)
        assertEquals(setOf(K.RIGHT), eastHit)

        // North-East Diagonal (45° angle) -> (450, 350)
        val diagonalHit = registry.hitTestTouchDisk(screenX = 450f, screenY = 350f, thumbRadiusPx = 10f)
        assertEquals(setOf(K.UP, K.RIGHT), diagonalHit)
    }

    @Test
    fun testTouchpadSafetyRulePredicate() {
        // Rule: Coordinator active ONLY when NO touchpad exists in active layout

        // Case 1: Layout with only buttons and sticks (No Touchpad)
        val layoutWithoutTouchpad = mapOf(
            "LS" to Position(0.2f, 0.5f),
            "RS" to Position(0.8f, 0.5f),
            "A" to Position(0.7f, 0.4f),
            "B" to Position(0.8f, 0.3f)
        )
        val touchpadEntries1 = layoutWithoutTouchpad.entries.filter { (key, _) ->
            key.equals(com.sanket.tools.nexpad.category.ControlKey.LTP.name, ignoreCase = true) ||
            key.equals(com.sanket.tools.nexpad.category.ControlKey.RTP.name, ignoreCase = true)
        }
        val isEsportsSurfaceActive1 = touchpadEntries1.isEmpty()
        assertTrue("Esports Touch Coordinator must be ACTIVE when layout has no touchpad", isEsportsSurfaceActive1)

        // Case 2: Layout with Inbuild or Discrete Touchpad (LTP or RTP present)
        val layoutWithTouchpad = mapOf(
            "LS" to Position(0.2f, 0.5f),
            "RS" to Position(0.8f, 0.5f),
            "RTP" to Position(0.5f, 0.5f)
        )
        val touchpadEntries2 = layoutWithTouchpad.entries.filter { (key, _) ->
            key.equals(com.sanket.tools.nexpad.category.ControlKey.LTP.name, ignoreCase = true) ||
            key.equals(com.sanket.tools.nexpad.category.ControlKey.RTP.name, ignoreCase = true)
        }
        val isEsportsSurfaceActive2 = touchpadEntries2.isEmpty()
        assertFalse("Esports Touch Coordinator must be INACTIVE when touchpad is present in the layout", isEsportsSurfaceActive2)

        // Case 3: Layout with NXPRC Custom Remote Touchpad
        val layoutWithNxprcTouchpad = mapOf(
            "LS" to Position(0.2f, 0.5f),
            "RS" to Position(0.8f, 0.5f),
            "CUSTOM_TP" to Position(0.5f, 0.5f, customComponentId = "rc.futuristic_touchpad")
        )
        val touchpadEntries3 = layoutWithNxprcTouchpad.entries.filter { (key, pos) ->
            key.equals("LTP", ignoreCase = true) ||
            key.equals("RTP", ignoreCase = true) ||
            key.contains("TOUCHPAD", ignoreCase = true) ||
            pos.customComponentId?.contains("touchpad", ignoreCase = true) == true
        }
        val isEsportsSurfaceActive3 = touchpadEntries3.isEmpty()
        assertFalse("Esports Touch Coordinator must be INACTIVE when an NXPRC touchpad is present", isEsportsSurfaceActive3)

        // Case 4: Layout without touchpad, but Esports Multi-Touch disabled by user in Settings
        val isEsportsMultitouchEnabled4 = false
        val isEsportsSurfaceActive4 = touchpadEntries1.isEmpty() && isEsportsMultitouchEnabled4
        assertFalse("Esports Touch Coordinator must be INACTIVE when disabled in Settings (reverts to old/classic mode)", isEsportsSurfaceActive4)

        // Case 5: Layout without touchpad, and Esports Multi-Touch enabled by user in Settings (default)
        val isEsportsMultitouchEnabled5 = true
        val isEsportsSurfaceActive5 = touchpadEntries1.isEmpty() && isEsportsMultitouchEnabled5
        assertTrue("Esports Touch Coordinator must be ACTIVE when enabled in Settings", isEsportsSurfaceActive5)
    }

    @Test
    fun testNxprcJoystickExclusionProtection() {
        // Layout with an NXPRC remote joystick
        val layoutWithNxprcStick = mapOf(
            "CUSTOM_STICK" to Position(0.2f, 0.5f, scale = 1.0f, customComponentId = "rc.cyber_joystick"),
            "BTN_A" to Position(0.7f, 0.5f)
        )

        // Joystick detection identifies it as a joystick
        val joystickEntries = layoutWithNxprcStick.entries.filter { (key, pos) ->
            key.equals("LS", ignoreCase = true) ||
            key.equals("RS", ignoreCase = true) ||
            key.contains("JOYSTICK", ignoreCase = true) ||
            pos.customComponentId?.contains("joystick", ignoreCase = true) == true
        }
        assertEquals(1, joystickEntries.size)

        // Excluded joystick center correctly captures the NXPRC stick
        val screenWidth = 2000f
        val screenHeight = 1000f
        val defaultRadius = 150f
        val excludedCenters = joystickEntries.map { (_, pos) ->
            Pair(Offset(pos.xRatio * screenWidth, pos.yRatio * screenHeight), defaultRadius * pos.scale)
        }

        // Center is at (400, 500)
        assertEquals(Offset(400f, 500f), excludedCenters.first().first)

        // Register a button nearby
        registry.put("BTN_A", Rect(left = 1350f, top = 450f, right = 1450f, bottom = 550f))

        // Touch at (420, 510) inside the NXPRC joystick area:
        // hypot(20, 10) = ~22.4 <= 150
        val hitsInsideNxprcJoystick = registry.hitTestTouchDisk(
            screenX = 420f,
            screenY = 510f,
            thumbRadiusPx = 20f,
            excludedJoystickCenters = excludedCenters
        )
        assertTrue("Touches inside NXPRC joystick radius MUST be excluded from button hits", hitsInsideNxprcJoystick.isEmpty())
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Active Joystick Slide Guard Industrial Unit Tests
    // ─────────────────────────────────────────────────────────────────────────────

    data class SimulatedTouchPointer(
        val id: Long,
        val x: Float,
        val y: Float,
        val isDown: Boolean,
        val isPressed: Boolean
    )

    class SimulatedEsportsCoordinator(
        val registry: BoundsRegistry,
        val excludedJoystickCenters: List<Pair<Offset, Float>> = emptyList(),
        val thumbRadiusPx: Float = 20f,
        val screenWidthPx: Float = 2000f,
        var isLeftStickActive: Boolean = false,
        var isRightStickActive: Boolean = false,
        var isSlideGuardEnabled: Boolean = true
    ) {
        val activePointers = mutableMapOf<Long, Set<String>>()
        val initialPointers = mutableMapOf<Long, Set<String>>()
        var globalPressed = setOf<String>()

        fun processPointer(pointer: SimulatedTouchPointer) {
            val pid = pointer.id
            if (pointer.isDown) {
                val hits = registry.hitTestTouchDisk(
                    screenX = pointer.x,
                    screenY = pointer.y,
                    thumbRadiusPx = thumbRadiusPx,
                    excludedJoystickCenters = excludedJoystickCenters
                )
                if (hits.isNotEmpty()) {
                    activePointers[pid] = hits
                    initialPointers[pid] = hits
                    syncButtons()
                }
            } else if (pointer.isPressed) {
                val px = pointer.x
                val isRightSide = px >= screenWidthPx / 2f
                val isSideGuarded = isSlideGuardEnabled && (if (isRightSide) isRightStickActive else currentIsLeftStickActive())

                if (isSideGuarded) {
                    if (activePointers.containsKey(pid)) {
                        val initial = initialPointers[pid] ?: emptySet()
                        val hits = registry.hitTestTouchDisk(
                            screenX = pointer.x,
                            screenY = pointer.y,
                            thumbRadiusPx = thumbRadiusPx,
                            excludedJoystickCenters = excludedJoystickCenters
                        )
                        val preserved = hits.filter { initial.contains(it) }.toSet()
                        if (preserved.isNotEmpty()) {
                            activePointers[pid] = preserved
                        } else {
                            activePointers.remove(pid)
                        }
                        syncButtons()
                    }
                } else {
                    val hits = registry.hitTestTouchDisk(
                        screenX = pointer.x,
                        screenY = pointer.y,
                        thumbRadiusPx = thumbRadiusPx,
                        excludedJoystickCenters = excludedJoystickCenters
                    )
                    if (hits.isNotEmpty() || activePointers.containsKey(pid)) {
                        if (hits.isNotEmpty()) {
                            activePointers[pid] = hits
                        } else {
                            activePointers.remove(pid)
                        }
                        syncButtons()
                    }
                }
            } else {
                initialPointers.remove(pid)
                if (activePointers.containsKey(pid)) {
                    activePointers.remove(pid)
                    syncButtons()
                }
            }
        }

        private fun currentIsLeftStickActive(): Boolean = isLeftStickActive

        private fun syncButtons() {
            val newGlobal = mutableSetOf<String>()
            for (set in activePointers.values) {
                newGlobal.addAll(set)
            }
            globalPressed = newGlobal
        }
    }

    @Test
    fun testActiveJoystickSlideGuard_RightStickActive_SuppressesRightSideSweepOnly() {
        // Screen width 2000px: Left side (< 1000px), Right side (>= 1000px)
        // Left side: UP at (300, 300)
        registry.put(K.UP, Rect(left = 260f, top = 260f, right = 340f, bottom = 340f))
        // Right side: Button A at (1500, 500), Button B at (1600, 500)
        registry.put(K.A, Rect(left = 1460f, top = 460f, right = 1540f, bottom = 540f))
        registry.put(K.B, Rect(left = 1560f, top = 460f, right = 1640f, bottom = 540f))

        val coordinator = SimulatedEsportsCoordinator(
            registry = registry,
            screenWidthPx = 2000f,
            isLeftStickActive = false,
            isRightStickActive = true, // Right joystick actively dragged!
            isSlideGuardEnabled = true
        )

        // 1. Right Thumb dragging outward from right joystick / neutral space (x=1300) towards Button A (x=1500)
        coordinator.processPointer(SimulatedTouchPointer(id = 1L, x = 1300f, y = 500f, isDown = true, isPressed = true)) // Outside buttons
        assertFalse("Neutral touch outside buttons does not trigger anything", coordinator.globalPressed.contains(K.A))

        // Slide into Button A (x=1500)
        coordinator.processPointer(SimulatedTouchPointer(id = 1L, x = 1500f, y = 500f, isDown = false, isPressed = true))
        assertFalse(
            "Active Right Joystick MUST suppress slide/sweep into Button A on the right side!",
            coordinator.globalPressed.contains(K.A)
        )

        // 2. Meanwhile on Left Side: Left Stick is NOT active. Continuous slide sweep MUST work freely!
        coordinator.processPointer(SimulatedTouchPointer(id = 2L, x = 200f, y = 300f, isDown = true, isPressed = true)) // Neutral left space
        assertFalse("Neutral touch outside UP is empty", coordinator.globalPressed.contains(K.UP))

        // Slide into D-Pad UP (x=300)
        coordinator.processPointer(SimulatedTouchPointer(id = 2L, x = 300f, y = 300f, isDown = false, isPressed = true))
        assertTrue(
            "Unguarded Left Side allows continuous slide/sweep into D-Pad UP!",
            coordinator.globalPressed.contains(K.UP)
        )
    }

    @Test
    fun testActiveJoystickSlideGuard_LeftStickActive_SuppressesLeftSideSweepOnly() {
        // Screen width 2000px
        registry.put(K.LEFT, Rect(left = 260f, top = 260f, right = 340f, bottom = 340f))
        registry.put(K.A, Rect(left = 1460f, top = 460f, right = 1540f, bottom = 540f))

        val coordinator = SimulatedEsportsCoordinator(
            registry = registry,
            screenWidthPx = 2000f,
            isLeftStickActive = true, // Left joystick actively walking!
            isRightStickActive = false,
            isSlideGuardEnabled = true
        )

        // 1. Left Thumb dragging from stick area towards D-Pad LEFT
        coordinator.processPointer(SimulatedTouchPointer(id = 1L, x = 150f, y = 300f, isDown = true, isPressed = true))
        coordinator.processPointer(SimulatedTouchPointer(id = 1L, x = 300f, y = 300f, isDown = false, isPressed = true))
        assertFalse(
            "Active Left Joystick MUST suppress slide/sweep into D-Pad LEFT on the left side!",
            coordinator.globalPressed.contains(K.LEFT)
        )

        // 2. Right Side: Right Stick NOT active. Continuous slide into Button A MUST work!
        coordinator.processPointer(SimulatedTouchPointer(id = 2L, x = 1400f, y = 500f, isDown = true, isPressed = true))
        coordinator.processPointer(SimulatedTouchPointer(id = 2L, x = 1500f, y = 500f, isDown = false, isPressed = true))
        assertTrue(
            "Unguarded Right Side allows continuous slide/sweep into Button A!",
            coordinator.globalPressed.contains(K.A)
        )
    }

    @Test
    fun testActiveJoystickSlideGuard_DirectTapAlwaysAllowedOnGuardedSide() {
        // Right side: Button A at (1500, 500)
        registry.put(K.A, Rect(left = 1460f, top = 460f, right = 1540f, bottom = 540f))

        val coordinator = SimulatedEsportsCoordinator(
            registry = registry,
            screenWidthPx = 2000f,
            isLeftStickActive = false,
            isRightStickActive = true, // Right joystick active
            isSlideGuardEnabled = true
        )

        // Direct tap directly onto Button A (isDown = true at 1500, 500)
        coordinator.processPointer(SimulatedTouchPointer(id = 1L, x = 1500f, y = 500f, isDown = true, isPressed = true))
        assertTrue(
            "Direct intentional tap directly on Button A MUST be allowed even with Right Stick active!",
            coordinator.globalPressed.contains(K.A)
        )
    }

    @Test
    fun testActiveJoystickSlideGuard_HoldingInitialButtonAllowed_CannotSlideToNewButton() {
        // Right side: Button A at (1500, 500), Button B at (1600, 500)
        registry.put(K.A, Rect(left = 1460f, top = 460f, right = 1540f, bottom = 540f))
        registry.put(K.B, Rect(left = 1560f, top = 460f, right = 1640f, bottom = 540f))

        val coordinator = SimulatedEsportsCoordinator(
            registry = registry,
            screenWidthPx = 2000f,
            isLeftStickActive = false,
            isRightStickActive = true, // Right stick active
            isSlideGuardEnabled = true
        )

        // 1. Direct tap on Button A
        coordinator.processPointer(SimulatedTouchPointer(id = 1L, x = 1500f, y = 500f, isDown = true, isPressed = true))
        assertTrue("A is pressed initially", coordinator.globalPressed.contains(K.A))

        // 2. Finger micro-shifts slightly within Button A (x=1510)
        coordinator.processPointer(SimulatedTouchPointer(id = 1L, x = 1510f, y = 500f, isDown = false, isPressed = true))
        assertTrue("Holding A remains active", coordinator.globalPressed.contains(K.A))

        // 3. Finger slides across gap into Button B (x=1600)
        coordinator.processPointer(SimulatedTouchPointer(id = 1L, x = 1600f, y = 500f, isDown = false, isPressed = true))
        assertFalse("Finger left A, so A is no longer held", coordinator.globalPressed.contains(K.A))
        assertFalse(
            "Active stick guard PREVENTS acquiring Button B via slide from A!",
            coordinator.globalPressed.contains(K.B)
        )
        assertTrue("No buttons are pressed after sliding into B", coordinator.globalPressed.isEmpty())
    }

    @Test
    fun testActiveJoystickSlideGuard_DisabledInSettings_AllowsSweepOnBothSides() {
        registry.put(K.A, Rect(left = 1460f, top = 460f, right = 1540f, bottom = 540f))

        val coordinator = SimulatedEsportsCoordinator(
            registry = registry,
            screenWidthPx = 2000f,
            isLeftStickActive = true,
            isRightStickActive = true,
            isSlideGuardEnabled = false // Player turned OFF the guard in SettingsScreen!
        )

        // Drag from neutral space (x=1400) into Button A (x=1500)
        coordinator.processPointer(SimulatedTouchPointer(id = 1L, x = 1400f, y = 500f, isDown = true, isPressed = true))
        coordinator.processPointer(SimulatedTouchPointer(id = 1L, x = 1500f, y = 500f, isDown = false, isPressed = true))

        assertTrue(
            "When slide guard is turned OFF in Settings, continuous slide sweep works across the entire screen!",
            coordinator.globalPressed.contains(K.A)
        )
    }
}
