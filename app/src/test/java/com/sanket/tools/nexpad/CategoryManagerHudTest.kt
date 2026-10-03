package com.sanket.tools.nexpad

import com.sanket.tools.nexpad.category.CategoryManager
import com.sanket.tools.nexpad.category.ControlKey
import com.sanket.tools.nexpad.category.ControllerLabelStyle
import com.sanket.tools.nexpad.category.ComponentType
import com.sanket.tools.nexpad.model.HudElement
import com.sanket.tools.nexpad.model.LayoutProfile
import com.sanket.tools.nexpad.model.LayoutSkin
import com.sanket.tools.nexpad.model.LayoutTransform
import com.sanket.tools.nexpad.model.Position
import com.sanket.tools.nexpad.model.getControlDefaultPosition
import com.sanket.tools.nexpad.model.standardElitePositions
import org.junit.Assert.*
import org.junit.Test
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlin.math.hypot
import kotlin.math.pow
import com.sanket.tools.nexpad.ui.components.controller.calculateGamingStickMagnitude
import com.sanket.tools.nexpad.ui.components.controller.VelocityRingBuffer
import com.sanket.tools.nexpad.ui.components.controller.PlayStationShape
import com.sanket.tools.nexpad.ui.components.controller.getPlayStationShape
import com.sanket.tools.nexpad.ui.components.controller.isPlayStationSymbol
import com.sanket.tools.nexpad.runtime.registry.NativeComponentRegistry

class CategoryManagerHudTest {

    @Test
    fun testResolveControlAliasesAndIds() {
        // Test LB resolution from various sources
        val lbFromKey = CategoryManager.resolveControl("LB")
        val lbFromAlias = CategoryManager.resolveControl("L1")
        val lbFromLabel = CategoryManager.resolveControl("Left Bumper")
        val lbFromDefaultId = CategoryManager.resolveControl("rc.bumper_lb")
        val lbFromBuiltin = CategoryManager.resolveControl("builtin.default_lb")

        assertNotNull(lbFromKey)
        assertEquals("LB", lbFromKey?.key)
        assertEquals("LB", lbFromAlias?.key)
        assertEquals("LB", lbFromLabel?.key)
        assertEquals("LB", lbFromDefaultId?.key)
        assertEquals("LB", lbFromBuiltin?.key)

        // Test RB resolution
        val rbFromKey = CategoryManager.resolveControl("RB")
        val rbFromAlias = CategoryManager.resolveControl("R1")
        val rbFromDefaultId = CategoryManager.resolveControl("rc.bumper_rb")
        assertEquals("RB", rbFromKey?.key)
        assertEquals("RB", rbFromAlias?.key)
        assertEquals("RB", rbFromDefaultId?.key)

        // Test Face Buttons & DPAD
        val aFromKey = CategoryManager.resolveControl("A")
        val aFromButtonA = CategoryManager.resolveControl("BUTTON_A")
        val aFromBtnA = CategoryManager.resolveControl("BTN_A")
        assertEquals("A", aFromKey?.key)
        assertEquals("A", aFromButtonA?.key)
        assertEquals("A", aFromBtnA?.key)

        val bFromKey = CategoryManager.resolveControl("B")
        val bFromButtonB = CategoryManager.resolveControl("BUTTON_B")
        assertEquals("B", bFromKey?.key)
        assertEquals("B", bFromButtonB?.key)

        val dpadFromCross = CategoryManager.resolveControl("CROSS")
        assertEquals("DPAD", dpadFromCross?.key)
    }

    @Test
    fun testHudElementCategoryDelegation() {
        val element = HudElement(
            controlKey = "LB",
            transform = LayoutTransform(0.1f, 0.2f, 1.0f, 0.9f),
            skinId = null
        )

        assertEquals("LB", element.controlKey)
        assertEquals("Left Bumper", element.displayName)
        assertEquals("Bumpers", element.categoryTitle)
        assertEquals("🛡️", element.emoji)
        assertNotNull(element.spec)

        // Position round-trip
        val pos = element.toPosition()
        assertEquals(0.1f, pos.xRatio)
        assertEquals(0.2f, pos.yRatio)
        assertEquals(1.0f, pos.scale)
        assertEquals(0.9f, pos.opacity)
        assertNull(pos.customComponentId)

        val restored = HudElement.fromPosition("LB", pos)
        assertEquals(element.controlKey, restored.controlKey)
        assertEquals(element.transform.xRatio, restored.transform.xRatio)
        assertEquals(element.skinId, restored.skinId)
    }

    @Test
    fun testDynamicSkinCompatibilityResolution() {
        fun isSkinCompatible(
            componentDefaultControl: String,
            componentCategory: String,
            componentId: String,
            targetControlKey: String
        ): Boolean {
            val targetSpec = CategoryManager.getControl(targetControlKey) ?: return false

            if (componentDefaultControl.isNotBlank()) {
                val resolved = CategoryManager.resolveControl(componentDefaultControl)
                if (resolved != null) return resolved.key == targetSpec.key
            }
            if (componentId.isNotBlank()) {
                val resolved = CategoryManager.resolveControl(componentId)
                if (resolved != null) return resolved.key == targetSpec.key
            }
            if (componentDefaultControl.isBlank() && componentCategory.isNotBlank()) {
                val cat = CategoryManager.getCategory(componentCategory)
                if (cat != null) return cat.type == targetSpec.categoryType
            }
            return false
        }

        // LB must match LB, L1, rc.bumper_lb, builtin.cyber_bumper_lb
        assertTrue(isSkinCompatible("LB", "BUMPER", "rc.bumper_lb", "LB"))
        assertTrue(isSkinCompatible("L1", "BUMPER", "rc.my_custom_bumper", "LB"))
        assertTrue(isSkinCompatible("", "BUMPER", "builtin.cyber_bumper_lb", "LB"))
        assertTrue(isSkinCompatible("Left Bumper", "BUMPER", "custom_123", "LB"))

        // Cross-button leakage must NEVER occur
        assertFalse(isSkinCompatible("RB", "BUMPER", "rc.bumper_rb", "LB"))
        assertFalse(isSkinCompatible("R1", "BUMPER", "rc.bumper_rb", "LB"))
        assertFalse(isSkinCompatible("A", "BUTTON", "rc.action_a", "B"))
        assertFalse(isSkinCompatible("B", "BUTTON", "rc.action_b", "A"))
        assertFalse(isSkinCompatible("LT", "TRIGGER", "rc.trigger_lt", "LB"))
    }

    @Test
    fun testInfiniteSkinCyclingLoop() {
        data class TestSkin(val id: String?, val name: String)

        // Simulates the skins available for LB
        val dummySkins = listOf(
            TestSkin(null, "Default 3D"), // index 0 (id = null)
            TestSkin("builtin.cyber_bumper_lb", "Tactical Bumper LB"),
            TestSkin("builtin.neon_cyan_bumper_lb", "Neon Cyan Bumper LB"),
            TestSkin("builtin.stealth_carbon_lb", "Stealth Carbon LB"),
            TestSkin("builtin.crimson_mecha_lb", "Crimson Mecha LB"),
            TestSkin("rc.bumper_lb", "Shoulder Bumper LB")
        )

        fun getNextSkinId(currentSkinId: String?): String? {
            val isCurrentDefault = currentSkinId == null || currentSkinId.startsWith("builtin.default_")
            val currentIndex = if (isCurrentDefault) {
                0
            } else {
                dummySkins.indexOfFirst { it.id == currentSkinId }
            }
            val nextIndex = if (currentIndex == -1) {
                0
            } else {
                (currentIndex + 1) % dummySkins.size
            }
            return dummySkins[nextIndex].id
        }

        var skin: String? = null // Starts at Default 3D

        // Cycle 1: 0 -> 1 (cyber_bumper_lb)
        skin = getNextSkinId(skin)
        assertEquals("builtin.cyber_bumper_lb", skin)

        // Cycle 2: 1 -> 2 (neon_cyan_bumper_lb)
        skin = getNextSkinId(skin)
        assertEquals("builtin.neon_cyan_bumper_lb", skin)

        // Cycle 3: 2 -> 3 (stealth_carbon_lb)
        skin = getNextSkinId(skin)
        assertEquals("builtin.stealth_carbon_lb", skin)

        // Cycle 4: 3 -> 4 (crimson_mecha_lb)
        skin = getNextSkinId(skin)
        assertEquals("builtin.crimson_mecha_lb", skin)

        // Cycle 5: 4 -> 5 ("Shoulder Bumper LB")
        skin = getNextSkinId(skin)
        assertEquals("rc.bumper_lb", skin)

        // Cycle 6: 5 -> 0 (LOOPS BACK TO DEFAULT!)
        skin = getNextSkinId(skin)
        assertNull(skin)

        // Cycle 7: 0 -> 1 (CONTINUES LOOPING FOREVER, NEVER STUCK)
        skin = getNextSkinId(skin)
        assertEquals("builtin.cyber_bumper_lb", skin)

        // Cycle 8: 1 -> 2
        skin = getNextSkinId(skin)
        assertEquals("builtin.neon_cyan_bumper_lb", skin)

        // Unknown or deleted skin cleanly resets to 0 (default)
        val resetSkin = getNextSkinId("deleted_unknown_skin_id")
        assertEquals(dummySkins[0].id, resetSkin) // null
    }

    @Test
    fun testNexpadKeysAllResolveToCategoryManager() {
        val K = com.sanket.tools.nexpad.model.NexpadKeys
        val keys = listOf(
            K.A, K.B, K.X, K.Y,
            K.UP, K.DOWN, K.LEFT, K.RIGHT, K.DPAD,
            K.LT, K.RT,
            K.LB, K.RB,
            K.LS, K.RS,
            K.GUIDE, K.START, K.BACK, K.SHARE,
            K.M1, K.M2, K.M3, K.M4
        )

        for (k in keys) {
            val control = CategoryManager.getControl(k)
            assertNotNull("Control key '$k' in NexpadKeys must exist in CategoryManager", control)
            assertEquals("Control key '$k' in NexpadKeys must match canonical CategoryManager key", k, control?.key)
        }
    }

    @Test
    fun testLayoutProfileCanonicalPositions() {
        val legacyProfile = com.sanket.tools.nexpad.model.LayoutProfile(
            name = "Legacy Test",
            positions = mapOf(
                "XBOX" to Position(0.5f, 0.1f),
                "VIEW" to Position(0.4f, 0.2f),
                "MENU" to Position(0.6f, 0.2f),
                "L1" to Position(0.1f, 0.3f),
                "L2" to Position(0.1f, 0.1f),
                "HOME" to Position(0.5f, 0.1f) // duplicate of XBOX -> GUIDE
            )
        )

        val canonical = legacyProfile.canonicalPositions()

        // XBOX and HOME must both normalize to GUIDE with no duplication
        assertTrue(canonical.containsKey("GUIDE"))
        assertFalse(canonical.containsKey("XBOX"))
        assertFalse(canonical.containsKey("HOME"))

        // VIEW normalizes to BACK
        assertTrue(canonical.containsKey("BACK"))
        assertFalse(canonical.containsKey("VIEW"))

        // MENU normalizes to START
        assertTrue(canonical.containsKey("START"))
        assertFalse(canonical.containsKey("MENU"))

        // L1 normalizes to LB, L2 normalizes to LT
        assertTrue(canonical.containsKey("LB"))
        assertTrue(canonical.containsKey("LT"))
        assertFalse(canonical.containsKey("L1"))
        assertFalse(canonical.containsKey("L2"))

        // Count should be exactly 5 distinct controls: GUIDE, BACK, START, LB, LT
        assertEquals(5, canonical.size)
    }

    @Test
    fun testHudPaletteDialogDynamicMatching() {
        val K = com.sanket.tools.nexpad.model.NexpadKeys
        // Elements on HUD using legacy or alias keys
        val currentElements = mapOf(
            "XBOX" to HudElement("XBOX", LayoutTransform(0.5f, 0.1f)),
            "VIEW" to HudElement("VIEW", LayoutTransform(0.4f, 0.2f)),
            "MENU" to HudElement("MENU", LayoutTransform(0.6f, 0.2f)),
            K.A to HudElement(K.A, LayoutTransform(0.8f, 0.8f))
        )

        // Palette checks using dynamic ControlKey resolution
        fun isPresentInPalette(spec: com.sanket.tools.nexpad.category.ControlKey): Boolean =
            currentElements.keys.any { elemKey ->
                com.sanket.tools.nexpad.category.ControlKey.fromIdentifier(elemKey) == spec
            }

        // Even though elements has "XBOX", "VIEW", "MENU", the palette specs GUIDE, BACK, START match TRUE!
        assertTrue(isPresentInPalette(com.sanket.tools.nexpad.category.ControlKey.GUIDE))
        assertTrue(isPresentInPalette(com.sanket.tools.nexpad.category.ControlKey.BACK))
        assertTrue(isPresentInPalette(com.sanket.tools.nexpad.category.ControlKey.START))
        assertTrue(isPresentInPalette(com.sanket.tools.nexpad.category.ControlKey.A))

        // SHARE is not on HUD -> matches FALSE
        assertFalse(isPresentInPalette(com.sanket.tools.nexpad.category.ControlKey.SHARE))
        assertFalse(isPresentInPalette(com.sanket.tools.nexpad.category.ControlKey.B))
    }

    @Test
    fun testFpsTacticalPositionsHasCanonicalButtonsAndZeroDuplicates() {
        val positions = com.sanket.tools.nexpad.model.fpsTacticalPositions()

        // Verify the 3 system buttons are present under canonical keys
        assertTrue("GUIDE must be present", positions.containsKey("GUIDE"))
        assertTrue("BACK must be present", positions.containsKey("BACK"))
        assertTrue("START must be present", positions.containsKey("START"))

        // Verify face buttons are present
        assertTrue("A must be present", positions.containsKey("A"))
        assertTrue("B must be present", positions.containsKey("B"))
        assertTrue("X must be present", positions.containsKey("X"))
        assertTrue("Y must be present", positions.containsKey("Y"))

        // Verify no duplicate keys exist after canonicalization
        val profile = com.sanket.tools.nexpad.model.LayoutProfile(
            name = "FPS Tactical Pro",
            positions = positions
        )
        val canonical = profile.canonicalPositions()
        assertEquals(positions.size, canonical.size)
    }

    @Test
    fun testAddControlDuplicatePrevention() {
        val elements = mutableMapOf<String, HudElement>(
            "GUIDE" to HudElement("GUIDE", LayoutTransform(0.5f, 0.1f))
        )

        fun addControl(controlKey: String) {
            val targetCtrl = com.sanket.tools.nexpad.category.ControlKey.fromIdentifier(controlKey)
            val canonicalKey = targetCtrl?.key ?: controlKey.uppercase()

            val existingEntry = elements.entries.firstOrNull { (k, _) ->
                if (targetCtrl != null) com.sanket.tools.nexpad.category.ControlKey.fromIdentifier(k) == targetCtrl
                else k.equals(canonicalKey, ignoreCase = true)
            }

            if (existingEntry != null) {
                // Duplicate prevention: do not add!
                return
            }

            elements[canonicalKey] = HudElement(canonicalKey, LayoutTransform(0.5f, 0.5f))
        }

        // Attempt to add "GUIDE" again
        addControl("GUIDE")
        assertEquals(1, elements.size)

        // Attempt to add "XBOX" (alias of GUIDE)
        addControl("XBOX")
        assertEquals(1, elements.size)

        // Attempt to add "HOME" (alias of GUIDE)
        addControl("HOME")
        assertEquals(1, elements.size)

        // Attempt to add "PS" (alias of GUIDE)
        addControl("PS")
        assertEquals(1, elements.size)

        // Adding a distinct control like "SHARE" succeeds
        addControl("SHARE")
        assertEquals(2, elements.size)
        assertTrue(elements.containsKey("SHARE"))
    }

    @Test
    fun testStandardElitePositionsHasOnlyCompositeDpad() {
        val positions = standardElitePositions()
        assertTrue("Standard Elite must contain integrated DPAD", positions.containsKey("DPAD"))
        assertFalse("Standard Elite must NOT contain discrete UP", positions.containsKey("UP"))
        assertFalse("Standard Elite must NOT contain discrete DOWN", positions.containsKey("DOWN"))
        assertFalse("Standard Elite must NOT contain discrete LEFT", positions.containsKey("LEFT"))
        assertFalse("Standard Elite must NOT contain discrete RIGHT", positions.containsKey("RIGHT"))
    }

    @Test
    fun testCanonicalPositionsDpadMutualExclusivitySelfHeal() {
        // Given a legacy profile with BOTH composite DPAD and discrete directional buttons
        val legacyPositions = mapOf(
            "DPAD" to Position(0.32f, 0.74f),
            "UP" to Position(0.32f, 0.65f),
            "DOWN" to Position(0.32f, 0.83f),
            "LEFT" to Position(0.26f, 0.74f),
            "RIGHT" to Position(0.38f, 0.74f),
            "A" to Position(0.7f, 0.7f)
        )
        val profile = LayoutProfile(name = "Legacy With Overlapping DPad", positions = legacyPositions)
        val canonical = profile.canonicalPositions()

        assertTrue("Canonical positions must keep composite DPAD", canonical.containsKey("DPAD"))
        assertTrue("Canonical positions must keep A button", canonical.containsKey("A"))
        assertFalse("Canonical positions must self-heal and drop discrete UP", canonical.containsKey("UP"))
        assertFalse("Canonical positions must self-heal and drop discrete DOWN", canonical.containsKey("DOWN"))
        assertFalse("Canonical positions must self-heal and drop discrete LEFT", canonical.containsKey("LEFT"))
        assertFalse("Canonical positions must self-heal and drop discrete RIGHT", canonical.containsKey("RIGHT"))
        assertEquals(2, canonical.size)
    }

    @Test
    fun testCanonicalPositionsPreservesDiscreteDpadWhenNoComposite() {
        // A layout with ONLY discrete buttons and NO composite DPAD should retain all discrete buttons
        val discreteOnly = mapOf(
            "UP" to Position(0.32f, 0.65f),
            "DOWN" to Position(0.32f, 0.83f),
            "LEFT" to Position(0.26f, 0.74f),
            "RIGHT" to Position(0.38f, 0.74f),
            "A" to Position(0.7f, 0.7f)
        )
        val profile = LayoutProfile(name = "Discrete Only", positions = discreteOnly)
        val canonical = profile.canonicalPositions()

        assertFalse(canonical.containsKey("DPAD"))
        assertTrue(canonical.containsKey("UP"))
        assertTrue(canonical.containsKey("DOWN"))
        assertTrue(canonical.containsKey("LEFT"))
        assertTrue(canonical.containsKey("RIGHT"))
        assertTrue(canonical.containsKey("A"))
        assertEquals(5, canonical.size)
    }

    @Test
    fun testAddControlDpadMutualExclusivitySimulation() {
        val elements = mutableMapOf<String, HudElement>()

        fun addControl(controlKey: String) {
            val targetCtrl = ControlKey.fromIdentifier(controlKey)
            val canonicalKey = targetCtrl?.key ?: controlKey.uppercase()

            val existingEntry = elements.entries.firstOrNull { (k, _) ->
                if (targetCtrl != null) ControlKey.fromIdentifier(k) == targetCtrl
                else k.equals(canonicalKey, ignoreCase = true)
            }
            if (existingEntry != null) return

            var updatedElements = elements.toMap()
            if (targetCtrl?.isDpadComposite == true) {
                updatedElements = updatedElements.filterKeys { k ->
                    ControlKey.fromIdentifier(k)?.isDpadDiscrete != true
                }
            } else if (targetCtrl?.isDpadDiscrete == true) {
                updatedElements = updatedElements.filterKeys { k ->
                    ControlKey.fromIdentifier(k)?.isDpadComposite != true
                }
            }

            elements.clear()
            elements.putAll(updatedElements)
            val defPos = getControlDefaultPosition(canonicalKey)
            elements[canonicalKey] = HudElement(
                controlKey = canonicalKey,
                transform = LayoutTransform(defPos?.xRatio ?: 0.5f, defPos?.yRatio ?: 0.5f)
            )
        }

        // 1. Add DPAD
        addControl("DPAD")
        assertTrue(elements.containsKey("DPAD"))
        assertEquals(1, elements.size)

        // 2. Add UP (discrete button) -> removes DPAD, adds UP
        addControl("UP")
        assertFalse("Adding UP must remove composite DPAD", elements.containsKey("DPAD"))
        assertTrue("Adding UP must add UP", elements.containsKey("UP"))
        assertEquals(1, elements.size)

        // 3. Add DOWN, LEFT, RIGHT -> all discrete buttons coexist
        addControl("DOWN")
        addControl("LEFT")
        addControl("RIGHT")
        assertEquals(4, elements.size)
        assertTrue(elements.containsKey("UP"))
        assertTrue(elements.containsKey("DOWN"))
        assertTrue(elements.containsKey("LEFT"))
        assertTrue(elements.containsKey("RIGHT"))

        // 4. Add CROSS (alias of DPAD) -> removes UP, DOWN, LEFT, RIGHT, leaves only DPAD
        addControl("CROSS")
        assertEquals(1, elements.size)
        assertTrue("Adding CROSS must resolve to canonical DPAD", elements.containsKey("DPAD"))
        assertFalse(elements.containsKey("UP"))
        assertFalse(elements.containsKey("DOWN"))
        assertFalse(elements.containsKey("LEFT"))
        assertFalse(elements.containsKey("RIGHT"))
    }

    @Test
    fun testControlKeyDpadClassificationHelpers() {
        assertTrue(ControlKey.DPAD.isDpadComposite)
        assertFalse(ControlKey.DPAD.isDpadDiscrete)

        assertTrue(ControlKey.UP.isDpadDiscrete)
        assertFalse(ControlKey.UP.isDpadComposite)

        assertTrue(ControlKey.DOWN.isDpadDiscrete)
        assertTrue(ControlKey.LEFT.isDpadDiscrete)
        assertTrue(ControlKey.RIGHT.isDpadDiscrete)

        assertEquals(4, ControlKey.DISCRETE_DPAD_KEYS.size)
        assertTrue(ControlKey.DISCRETE_DPAD_KEYS.contains(ControlKey.UP))
        assertTrue(ControlKey.DISCRETE_DPAD_KEYS.contains(ControlKey.DOWN))
        assertTrue(ControlKey.DISCRETE_DPAD_KEYS.contains(ControlKey.LEFT))
        assertTrue(ControlKey.DISCRETE_DPAD_KEYS.contains(ControlKey.RIGHT))

        assertFalse(ControlKey.A.isDpadComposite)
        assertFalse(ControlKey.A.isDpadDiscrete)
    }

    @Test
    fun testProfileOrderSortingAndUnlistedFallback() {
        val json = Json { ignoreUnknownKeys = true }
        val sampleProfiles = listOf(
            LayoutProfile(name = "Standard Elite", isDefault = true),
            LayoutProfile(name = "FPS Tactical", isDefault = true),
            LayoutProfile(name = "MOBA Arena", isDefault = true),
            LayoutProfile(name = "My Custom 1", isDefault = false),
            LayoutProfile(name = "My Custom 2", isDefault = false)
        )

        val customOrder = listOf("My Custom 2", "FPS Tactical", "NonExistentProfile", "Standard Elite")
        val savedOrderJson = json.encodeToString(customOrder)

        // Decode and apply order as done in LayoutManager
        val orderList = json.decodeFromString<List<String>>(savedOrderJson)
        val profileMap = sampleProfiles.associateBy { it.name }
        val orderedResult = mutableListOf<LayoutProfile>()
        for (name in orderList) {
            profileMap[name]?.let { orderedResult.add(it) }
        }
        for (profile in sampleProfiles) {
            if (orderedResult.none { it.name.equals(profile.name, ignoreCase = true) }) {
                orderedResult.add(profile)
            }
        }

        assertEquals(5, orderedResult.size)
        assertEquals("My Custom 2", orderedResult[0].name)
        assertEquals("FPS Tactical", orderedResult[1].name)
        assertEquals("Standard Elite", orderedResult[2].name)
        assertEquals("MOBA Arena", orderedResult[3].name)
        assertEquals("My Custom 1", orderedResult[4].name)
    }

    @Test
    fun testProfileOrderReorderingSwap() {
        val list = mutableListOf("A", "B", "C", "D")
        // Move "C" (index 2) to index 0
        val fromIndex = 2
        val toIndex = 0
        list.add(toIndex, list.removeAt(fromIndex))
        assertEquals(listOf("C", "A", "B", "D"), list)

        // Move "A" (index 1) to end (index 3)
        val from2 = 1
        val to2 = 3
        list.add(to2, list.removeAt(from2))
        assertEquals(listOf("C", "B", "D", "A"), list)
    }

    @Test
    fun testProfileOrderSyncOnDeleteAndRename() {
        val order = mutableListOf("Standard Elite", "Custom Beta", "Racing Master")

        // Rename "Custom Beta" to "Custom Pro"
        val renameIdx = order.indexOfFirst { it.equals("Custom Beta", ignoreCase = true) }
        assertTrue(renameIdx != -1)
        order[renameIdx] = "Custom Pro"
        assertEquals(listOf("Standard Elite", "Custom Pro", "Racing Master"), order)

        // Delete "Custom Pro"
        val deleted = order.removeAll { it.equals("Custom Pro", ignoreCase = true) }
        assertTrue(deleted)
        assertEquals(listOf("Standard Elite", "Racing Master"), order)
    }

    @Test
    fun testDragDropCoordinateCompensationUnderScroll() {
        // Initial state
        val initialItemOffset = androidx.compose.ui.unit.IntOffset(0, 300)
        var totalDragDelta = androidx.compose.ui.geometry.Offset(0f, 50f)
        var currentItemOffset = initialItemOffset

        // Visual position before scroll
        var translation = totalDragDelta + androidx.compose.ui.geometry.Offset(
            (initialItemOffset.x - currentItemOffset.x).toFloat(),
            (initialItemOffset.y - currentItemOffset.y).toFloat()
        )
        var renderedY = currentItemOffset.y + translation.y
        assertEquals(350f, renderedY, 0.001f)

        // Simulate grid scrolling by 30px (content shifts up in viewport, offset decreases)
        currentItemOffset = androidx.compose.ui.unit.IntOffset(0, 270)
        // User finger didn't move on the glass
        translation = totalDragDelta + androidx.compose.ui.geometry.Offset(
            (initialItemOffset.x - currentItemOffset.x).toFloat(),
            (initialItemOffset.y - currentItemOffset.y).toFloat()
        )
        // Translation automatically compensated from 50 to 80
        assertEquals(80f, translation.y, 0.001f)
        renderedY = currentItemOffset.y + translation.y
        // Rendered position on glass remains rock-solid at 350!
        assertEquals(350f, renderedY, 0.001f)
    }

    @Test
    fun testDragDropCoordinateCompensationUnderItemSwap() {
        val initialItemOffset = androidx.compose.ui.unit.IntOffset(0, 200)
        val totalDragDelta = androidx.compose.ui.geometry.Offset(0f, 150f)

        // Dragged card at slot 0 (y = 200)
        var currentItemOffset = initialItemOffset
        var translation = totalDragDelta + androidx.compose.ui.geometry.Offset(
            (initialItemOffset.x - currentItemOffset.x).toFloat(),
            (initialItemOffset.y - currentItemOffset.y).toFloat()
        )
        assertEquals(350f, currentItemOffset.y + translation.y, 0.001f)

        // Items swap: dragged card is now placed at slot 1 (y = 400)
        currentItemOffset = androidx.compose.ui.unit.IntOffset(0, 400)
        translation = totalDragDelta + androidx.compose.ui.geometry.Offset(
            (initialItemOffset.x - currentItemOffset.x).toFloat(),
            (initialItemOffset.y - currentItemOffset.y).toFloat()
        )
        // Translation instantly compensates from 150 to -50
        assertEquals(-50f, translation.y, 0.001f)
        // Rendered position remains exactly 350 without even a 1-pixel jump!
        assertEquals(350f, currentItemOffset.y + translation.y, 0.001f)
    }

    @Test
    fun testDragDropAutoScrollDeltaCalculations() {
        val viewportHeight = 1000f
        val edgeThreshold = 160f // 16% of 1000

        fun calculateDelta(pointerY: Float): Float {
            return when {
                pointerY < edgeThreshold -> {
                    val factor = ((edgeThreshold - pointerY) / edgeThreshold).coerceIn(0.1f, 1.2f)
                    val speed = 10f + (24f * factor)
                    -speed
                }
                pointerY > (viewportHeight - edgeThreshold) -> {
                    val factor = ((pointerY - (viewportHeight - edgeThreshold)) / edgeThreshold).coerceIn(0.1f, 1.2f)
                    val speed = 10f + (24f * factor)
                    speed
                }
                else -> 0f
            }
        }

        // Center safe zone: no scroll
        assertEquals(0f, calculateDelta(500f), 0.001f)
        assertEquals(0f, calculateDelta(200f), 0.001f)
        assertEquals(0f, calculateDelta(800f), 0.001f)

        // Near top threshold (pointer at 80px): scroll up (negative)
        val topScroll = calculateDelta(80f)
        assertTrue(topScroll < 0f)
        assertEquals(-22f, topScroll, 0.5f)

        // Beyond top edge (pointer at -20px): max scroll up
        val beyondTopScroll = calculateDelta(-20f)
        assertTrue(beyondTopScroll < topScroll)

        // Near bottom threshold (pointer at 920px): scroll down (positive)
        val bottomScroll = calculateDelta(920f)
        assertTrue(bottomScroll > 0f)
        assertEquals(22f, bottomScroll, 0.5f)

        // Beyond bottom edge (pointer at 1050px): max scroll down
        val beyondBottomScroll = calculateDelta(1050f)
        assertTrue(beyondBottomScroll > bottomScroll)
    }

    @Test
    fun testDragDropRectCenterContainmentCollision() {
        val draggedRect = androidx.compose.ui.geometry.Rect(
            left = 10f,
            top = 220f,
            right = 390f,
            bottom = 420f
        )

        // Stationary card A: top 50, height 150 (center y = 125) -> not contained
        val cardACenter = androidx.compose.ui.geometry.Offset(200f, 125f)
        assertFalse(draggedRect.contains(cardACenter))

        // Stationary card B: top 240, height 150 (center y = 315) -> contained!
        val cardBCenter = androidx.compose.ui.geometry.Offset(200f, 315f)
        assertTrue(draggedRect.contains(cardBCenter))

        // Stationary card C: top 430, height 150 (center y = 505) -> not contained
        val cardCCenter = androidx.compose.ui.geometry.Offset(200f, 505f)
        assertFalse(draggedRect.contains(cardCCenter))
    }

    @Test
    fun testStickButtonControlResolutionAndPositions() {
        val lsb = CategoryManager.resolveControl("LSB")
        assertNotNull(lsb)
        assertEquals("LSB", lsb?.key)
        assertEquals(com.sanket.tools.nexpad.category.CategoryType.STICKS, lsb?.categoryType)
        assertEquals(com.sanket.tools.nexpad.category.ComponentType.BUTTON, lsb?.componentType)

        val rsb = CategoryManager.resolveControl("RSB")
        assertNotNull(rsb)
        assertEquals("RSB", rsb?.key)
        assertEquals(com.sanket.tools.nexpad.category.CategoryType.STICKS, rsb?.categoryType)
        assertEquals(com.sanket.tools.nexpad.category.ComponentType.BUTTON, rsb?.componentType)

        // Synonyms L3 and R3 resolve to LSB and RSB
        val l3 = CategoryManager.resolveControl("L3")
        assertEquals("LSB", l3?.key)
        val r3 = CategoryManager.resolveControl("R3")
        assertEquals("RSB", r3?.key)

        // Fallback default positions
        val lsbPos = getControlDefaultPosition("LSB")
        assertNotNull(lsbPos)
        assertEquals(0.210f, lsbPos!!.xRatio, 0.001f)
        assertEquals(0.540f, lsbPos.yRatio, 0.001f)

        val rsbPos = getControlDefaultPosition("RSB")
        assertNotNull(rsbPos)
        assertEquals(0.790f, rsbPos!!.xRatio, 0.001f)
        assertEquals(0.540f, rsbPos.yRatio, 0.001f)
    }

    @Test
    fun testTouchpadControlResolutionAndPositions() {
        val ltp = CategoryManager.resolveControl("LTP")
        assertNotNull(ltp)
        assertEquals("LTP", ltp?.key)
        assertEquals(com.sanket.tools.nexpad.category.CategoryType.STICKS, ltp?.categoryType)
        assertEquals(com.sanket.tools.nexpad.category.ComponentType.TOUCHPAD, ltp?.componentType)

        val rtp = CategoryManager.resolveControl("RTP")
        assertNotNull(rtp)
        assertEquals("RTP", rtp?.key)
        assertEquals(com.sanket.tools.nexpad.category.CategoryType.STICKS, rtp?.categoryType)
        assertEquals(com.sanket.tools.nexpad.category.ComponentType.TOUCHPAD, rtp?.componentType)

        // Aliases
        val touchL = CategoryManager.resolveControl("TOUCHPAD_L")
        assertEquals("LTP", touchL?.key)
        val movePad = CategoryManager.resolveControl("MOVE_PAD")
        assertEquals("LTP", movePad?.key)
        val cameraPad = CategoryManager.resolveControl("CAMERA_PAD")
        assertEquals("RTP", cameraPad?.key)
        val swipeLook = CategoryManager.resolveControl("SWIPE_LOOK")
        assertEquals("RTP", swipeLook?.key)

        // Fallback default positions
        val ltpPos = getControlDefaultPosition("LTP")
        assertNotNull(ltpPos)
        assertEquals(0.180f, ltpPos!!.xRatio, 0.001f)
        assertEquals(0.680f, ltpPos.yRatio, 0.001f)

        val rtpPos = getControlDefaultPosition("RTP")
        assertNotNull(rtpPos)
        assertEquals(0.820f, rtpPos!!.xRatio, 0.001f)
        assertEquals(0.680f, rtpPos.yRatio, 0.001f)
    }

    @Test
    fun testGamingSpeedToDistanceSweetSpots() {
        val sensitivity = 1.0f

        // 1. Noise gate (< 8 dp/s) -> must be exactly 0.0f
        assertEquals("Sub-8 dp/s noise gate must produce 0 stick deflection", 0f, calculateGamingStickMagnitude(0f, sensitivity), 0.0001f)
        assertEquals("Sub-8 dp/s noise gate must produce 0 stick deflection", 0f, calculateGamingStickMagnitude(5f, sensitivity), 0.0001f)
        assertEquals("Sub-8 dp/s noise gate boundary must produce 0 stick deflection", 0f, calculateGamingStickMagnitude(7.99f, sensitivity), 0.0001f)

        // 2. Precision Aiming Zone: 8 dp/s starts at anti-deadzone floor 0.16f
        assertEquals("8 dp/s must start at anti-deadzone floor 0.16f", 0.16f, calculateGamingStickMagnitude(8f, sensitivity), 0.0001f)

        // Precision Aiming Zone ends at 120 dp/s with 0.25f
        assertEquals("120 dp/s precision zone boundary must produce 0.25f stick deflection", 0.25f, calculateGamingStickMagnitude(120f, sensitivity), 0.0001f)

        // 3. THE GOLDEN SWEET SPOT: 400 dp/s MUST produce exactly 0.50f (half deflection)
        assertEquals("400 dp/s golden sweet spot must produce exactly 0.50f half deflection", 0.50f, calculateGamingStickMagnitude(400f, sensitivity), 0.0001f)

        // 4. Exponential Acceleration Zone: 1200 dp/s MUST reach full 1.00f deflection
        assertEquals("1200 dp/s must reach full 1.00f deflection", 1.00f, calculateGamingStickMagnitude(1200f, sensitivity), 0.0001f)

        // 5. Saturation Zone: > 1200 dp/s must clamp at 1.00f
        assertEquals("Flick speeds > 1200 dp/s must clamp at 1.00f", 1.00f, calculateGamingStickMagnitude(1800f, sensitivity), 0.0001f)
        assertEquals("Extreme flick speeds must clamp at 1.00f", 1.00f, calculateGamingStickMagnitude(3000f, sensitivity), 0.0001f)
    }

    @Test
    fun testDirectionalAxisStabilization() {
        // Cross-talk suppression: when primary axis > 3x secondary axis, dampen off-axis by 50%
        fun stabilizeAxes(deltaX: Float, deltaY: Float): Pair<Float, Float> {
            var finalDeltaX = deltaX
            var finalDeltaY = deltaY
            val absX = kotlin.math.abs(deltaX)
            val absY = kotlin.math.abs(deltaY)
            if (absX > 3.0f * absY) {
                finalDeltaY *= 0.5f
            } else if (absY > 3.0f * absX) {
                finalDeltaX *= 0.5f
            }
            return Pair(finalDeltaX, finalDeltaY)
        }

        // Horizontal turn with minor vertical drift (dx=30, dy=4 -> 30 > 3*4=12)
        val (hX, hY) = stabilizeAxes(30f, 4f)
        assertEquals(30f, hX, 0.0001f)
        assertEquals(2f, hY, 0.0001f) // dampened from 4 to 2

        // Vertical look with minor horizontal drift (dx=3, dy=25 -> 25 > 3*3=9)
        val (vX, vY) = stabilizeAxes(3f, 25f)
        assertEquals(1.5f, vX, 0.0001f) // dampened from 3 to 1.5
        assertEquals(25f, vY, 0.0001f)

        // Diagonal swipe (dx=20, dy=15 -> not > 3x) remains unaltered
        val (diagX, diagY) = stabilizeAxes(20f, 15f)
        assertEquals(20f, diagX, 0.0001f)
        assertEquals(15f, diagY, 0.0001f)
    }

    @Test
    fun testCameraTouchRelativeDeltaCalculationAndStationaryZeroReset() {
        // Density-independent gaming ballistics algorithm used in RealisticTouchPad, RealisticJoystick, NxprcCanvasRenderer
        val density = 2.75f // Typical FHD mobile display
        val sensitivity = 1.0f

        fun calculateStick(deltaX: Float, deltaY: Float, dtSec: Float): Pair<Float, Float> {
            val distPx = hypot(deltaX, deltaY)
            val distDp = distPx / density
            if (distDp <= 0.15f) return Pair(0f, 0f)

            val speedDpPerSec = distDp / dtSec.coerceAtLeast(0.001f)
            val stickMagnitude = calculateGamingStickMagnitude(speedDpPerSec, sensitivity)
            if (stickMagnitude <= 0f) return Pair(0f, 0f)

            val dirX = deltaX / distPx
            val dirY = deltaY / distPx

            val targetStickX = (dirX * stickMagnitude).coerceIn(-1f, 1f)
            val targetStickY = (-dirY * stickMagnitude).coerceIn(-1f, 1f)
            return Pair(targetStickX, targetStickY)
        }

        var previousTouchX = 500f
        var previousTouchY = 500f
        var stickX = 0f
        var stickY = 0f

        // Step 1: Touch down at (500, 500)
        assertEquals("On touch down, camera stick must be exactly 0f", 0f, stickX, 0.0001f)
        assertEquals("On touch down, camera stick must be exactly 0f", 0f, stickY, 0.0001f)

        // Step 2: Finger moves to (500, 510) in 16ms (60fps) - Gentle drag downward
        val move1X = 500f
        val move1Y = 510f
        val delta1X = move1X - previousTouchX
        val delta1Y = move1Y - previousTouchY
        previousTouchX = move1X
        previousTouchY = move1Y

        val (s1X, s1Y) = calculateStick(delta1X, delta1Y, 0.016f)
        stickX = s1X
        stickY = s1Y

        assertEquals(0f, delta1X, 0.0001f)
        assertEquals(10f, delta1Y, 0.0001f)
        assertEquals(0f, stickX, 0.0001f)
        assertTrue("Down swipe produces negative stick deflection (look down)", stickY < 0f)
        assertTrue("Stick magnitude must respect deadzone floor 0.16 so PC games respond immediately", -stickY >= 0.16f)

        // Step 3: Fast swipe downward
        val move2X = 500f
        val move2Y = 560f
        val delta2X = move2X - previousTouchX
        val delta2Y = move2Y - previousTouchY
        previousTouchX = move2X
        previousTouchY = move2Y

        val (s2X, s2Y) = calculateStick(delta2X, delta2Y, 0.016f)
        stickX = s2X
        stickY = s2Y

        assertTrue("Fast swipe gives higher deflection than gentle swipe", -stickY > -s1Y)

        // Step 4: Finger rests stationary at (500, 560)
        // In laptop touchpad logic: no motion -> watchdog smoothly decays to 0
        stickX = 0f
        stickY = 0f
        assertEquals("Stationary resting finger must produce zero stick output", 0f, stickX, 0.0001f)
        assertEquals("Stationary resting finger must produce zero stick output, stopping camera spin", 0f, stickY, 0.0001f)

        // Step 5: Finger lifts - releases to (0, 0)
        stickX = 0f
        stickY = 0f
        assertEquals(0f, stickX, 0.0001f)
        assertEquals(0f, stickY, 0.0001f)
    }

    @Test
    fun testBothLtpAndRtpUseLaptopTouchpadSpeedToDistanceMechanics() {
        val density = 3.0f
        val sensitivity = 1.0f

        fun calculateTouchpadStick(isLeft: Boolean, deltaX: Float, deltaY: Float, dtSec: Float): Pair<Float, Float> {
            val distPx = hypot(deltaX, deltaY)
            val distDp = distPx / density
            if (distDp <= 0.15f) return Pair(0f, 0f)

            val speedDpPerSec = distDp / dtSec.coerceAtLeast(0.001f)
            val stickMagnitude = calculateGamingStickMagnitude(speedDpPerSec, sensitivity)
            if (stickMagnitude <= 0f) return Pair(0f, 0f)

            val dirX = deltaX / distPx
            val dirY = deltaY / distPx

            val targetStickX = (dirX * stickMagnitude).coerceIn(-1f, 1f)
            val targetStickY = (-dirY * stickMagnitude).coerceIn(-1f, 1f)
            return Pair(targetStickX, targetStickY)
        }

        // Both LTP (Left Touchpad) and RTP (Right Touchpad) yield identical speed-to-distance ballistics
        for (isLeft in listOf(true, false)) {
            val padName = if (isLeft) "LTP (Left Touchpad)" else "RTP (Right Touchpad)"

            // Gentle downward drag in 16ms
            val (s1X, s1Y) = calculateTouchpadStick(isLeft, 0f, 12f, 0.016f)
            assertEquals("0 horizontal delta produces 0 X stick on $padName", 0f, s1X, 0.0001f)
            assertTrue("Downward drag on $padName produces negative Y stick", s1Y < 0f)
            assertTrue("Stick magnitude on $padName respects deadzone floor 0.16", -s1Y >= 0.16f)

            // High speed swipe in 16ms
            val (s2X, s2Y) = calculateTouchpadStick(isLeft, 0f, 60f, 0.016f)
            assertTrue("Fast swipe on $padName produces greater deflection than gentle swipe", -s2Y > -s1Y)

            // Rightward swipe
            val (s3X, s3Y) = calculateTouchpadStick(isLeft, 30f, 0f, 0.016f)
            assertTrue("Rightward swipe on $padName produces positive X stick", s3X > 0f)
            assertEquals("0 vertical delta produces 0 Y stick on $padName", 0f, s3Y, 0.0001f)
        }
    }

    @Test
    fun testTouchpad2xDefaultSensitivityAndDockedInspectorAdjustment() {
        // 1. Default sensitivity verification:
        // HudElement LayoutTransform defaults to 2.0f (2x default sensitivity)
        val defaultTransform = LayoutTransform(xRatio = 0.5f, yRatio = 0.5f)
        assertEquals(2.0f, defaultTransform.sensitivity, 0.0001f)

        // 2. Ballistics response comparison: 1.0f base vs 2.0f (2x default sensitivity)
        // At 200 dp/s thumb speed:
        val magBase200 = calculateGamingStickMagnitude(200f, 1.0f)
        val mag2x200 = calculateGamingStickMagnitude(200f, 2.0f)
        // With 2.0x sensitivity: 200 * 2.0 = 400 dp/s, which hits the 0.50 sweet spot!
        assertEquals("2.0x sensitivity hits 0.50 deflection at 200 dp/s", 0.50f, mag2x200, 0.001f)
        assertTrue("Base 1.0x sensitivity gives lower deflection than 2.0x", mag2x200 > magBase200)

        // At 600 dp/s thumb speed:
        val magBase600 = calculateGamingStickMagnitude(600f, 1.0f)
        val mag2x600 = calculateGamingStickMagnitude(600f, 2.0f)
        // With 2.0x sensitivity: 600 * 2.0 = 1200 dp/s, reaching full 1.00 maximum deflection!
        assertEquals("2.0x sensitivity reaches full 1.00 saturation at 600 dp/s", 1.00f, mag2x600, 0.0001f)
        assertTrue("Base 1.0x is not saturated yet at 600 dp/s", magBase600 < 1.00f)

        // 3. HudElement & Position serialization round-trip with custom sensitivity
        val posWithSens = Position(xRatio = 0.2f, yRatio = 0.8f, scale = 1.2f, opacity = 0.9f, sensitivity = 2.4f)
        val element = HudElement.fromPosition("LTP", posWithSens)
        assertEquals("LTP", element.controlKey)
        assertEquals(2.4f, element.transform.sensitivity, 0.0001f)

        val backToPos = element.toPosition()
        assertEquals(2.4f, backToPos.sensitivity!!, 0.0001f)

        // JSON serialization round-trip
        val jsonStr = Json.encodeToString(backToPos)
        assertTrue("JSON contains sensitivity field", jsonStr.contains("\"sensitivity\":2.4"))
        val decodedPos = Json.decodeFromString<Position>(jsonStr)
        assertEquals(2.4f, decodedPos.sensitivity!!, 0.0001f)

        // Null sensitivity in JSON defaults to 2.0f in HudElement
        val legacyPos = Position(xRatio = 0.7f, yRatio = 0.7f, sensitivity = null)
        val legacyElement = HudElement.fromPosition("RTP", legacyPos)
        assertEquals("Unspecified sensitivity defaults to 2.0f", 2.0f, legacyElement.transform.sensitivity, 0.0001f)

        // 4. Docked Inspector slider step and range clamping (0.5f to 4.0f)
        var sens = 2.0f
        val stepUp = ((sens + 0.1f) * 10f).let { kotlin.math.round(it) / 10f }.coerceIn(0.5f, 4.0f)
        assertEquals(2.1f, stepUp, 0.0001f)

        val stepDown = ((sens - 0.1f) * 10f).let { kotlin.math.round(it) / 10f }.coerceIn(0.5f, 4.0f)
        assertEquals(1.9f, stepDown, 0.0001f)

        // Clamping bounds
        val overMax = (4.0f + 0.5f).coerceIn(0.5f, 4.0f)
        assertEquals(4.0f, overMax, 0.0001f)
        val underMin = (0.5f - 0.5f).coerceIn(0.5f, 4.0f)
        assertEquals(0.5f, underMin, 0.0001f)
    }

    @Test
    fun testCenterClickSeparatedIntoStandaloneStickButtons() {
        // Dedicated standalone buttons (LSB, RSB) handle stick click (L3, R3)
        val lsb = CategoryManager.getControl("LSB")
        val rsb = CategoryManager.getControl("RSB")
        assertNotNull("LSB exists in category manager", lsb)
        assertNotNull("RSB exists in category manager", rsb)
        assertEquals("LSB is in STICKS category", com.sanket.tools.nexpad.category.CategoryType.STICKS, lsb?.categoryType)
        assertEquals("RSB is in STICKS category", com.sanket.tools.nexpad.category.CategoryType.STICKS, rsb?.categoryType)
        assertEquals("LSB component type is BUTTON", com.sanket.tools.nexpad.category.ComponentType.BUTTON, lsb?.componentType)
        assertEquals("RSB component type is BUTTON", com.sanket.tools.nexpad.category.ComponentType.BUTTON, rsb?.componentType)

        // Touchpads (LTP, RTP) are pure TOUCHPAD components with no accidental click mechanism
        val ltp = CategoryManager.getControl("LTP")
        val rtp = CategoryManager.getControl("RTP")
        assertNotNull("LTP exists in category manager", ltp)
        assertNotNull("RTP exists in category manager", rtp)
        assertEquals("LTP component type is TOUCHPAD", com.sanket.tools.nexpad.category.ComponentType.TOUCHPAD, ltp?.componentType)
        assertEquals("RTP component type is TOUCHPAD", com.sanket.tools.nexpad.category.ComponentType.TOUCHPAD, rtp?.componentType)
    }

    @Test
    fun testVelocityRingBufferSmoothing() {
        val buffer = VelocityRingBuffer(8)
        assertEquals(0f, buffer.averageSpeed(), 0.0001f)
        assertEquals(0f, buffer.peakSpeed(), 0.0001f)

        // Simulate finger moving at 200 dp/s, with touch events arriving at jittery intervals
        // (4ms, 24ms, 8ms, 16ms, 12ms, 20ms, 6ms, 18ms)
        val durations = floatArrayOf(0.004f, 0.024f, 0.008f, 0.016f, 0.012f, 0.020f, 0.006f, 0.018f)
        val expectedSpeed = 200f // dp/s

        for (dt in durations) {
            val dist = expectedSpeed * dt
            buffer.push(dist, dt)
        }

        // The windowed average across all 8 samples should match exactly 200 dp/s
        assertEquals("Windowed average must filter out timing jitter and match true speed", expectedSpeed, buffer.averageSpeed(), 0.05f)
        assertEquals("Peak speed for constant physical motion should equal expected speed", expectedSpeed, buffer.peakSpeed(), 0.05f)

        // Push a flick spike
        buffer.push(100f, 0.016f) // 100 / 0.016 = 6250 dp/s flick
        assertTrue("Peak speed must capture flick spike", buffer.peakSpeed() > 6000f)

        // Buffer clear resets state
        buffer.clear()
        assertEquals(0f, buffer.averageSpeed(), 0.0001f)
        assertEquals(0f, buffer.peakSpeed(), 0.0001f)
    }

    @Test
    fun testConstantSpeedProducesStableOutput() {
        val buffer = VelocityRingBuffer(8)
        val sensitivity = 1.0f

        // Jittery dt times simulating real Android touch timestamps (vsync/scheduling jitter)
        val jitterDts = floatArrayOf(
            0.012f, 0.020f, 0.008f, 0.024f, 0.016f, 0.014f, 0.018f, 0.010f,
            0.022f, 0.011f, 0.019f, 0.015f, 0.017f, 0.013f, 0.021f, 0.016f
        )
        val constantPhysicalSpeed = 300f // dp/s

        var currentStickX = 0f
        val stickOutputs = mutableListOf<Float>()

        for (i in 0 until 32) {
            val dt = jitterDts[i % jitterDts.size]
            val dist = constantPhysicalSpeed * dt
            buffer.push(dist, dt)

            val avgSpeed = buffer.averageSpeed()
            val magnitude = calculateGamingStickMagnitude(avgSpeed, sensitivity)

            // Direction is purely horizontal (+X)
            val targetStickX = magnitude
            currentStickX = 0.55f * targetStickX + 0.45f * currentStickX

            // Record stick values once ring buffer is filled (index >= 8)
            if (i >= 8) {
                stickOutputs.add(currentStickX)
            }
        }

        // Check stability: the stick magnitude at 300 dp/s should be close to expected curve value:
        // 120..400 dp/s zone -> 0.25 + 0.25 * ((300 - 120) / 280) = 0.25 + 0.25 * (180/280) = ~0.4107
        val expectedMag = calculateGamingStickMagnitude(constantPhysicalSpeed, sensitivity)
        for (output in stickOutputs) {
            assertEquals("Output must remain stable under jittery touch delivery", expectedMag, output, 0.015f)
        }

        val minVal = stickOutputs.minOrNull() ?: 0f
        val maxVal = stickOutputs.maxOrNull() ?: 0f
        val delta = maxVal - minVal
        assertTrue("Stick deflection ripple under constant speed must be < 0.02 (no flickering), was $delta", delta < 0.02f)
    }

    @Test
    fun testPlayStationGlyphShapeResolution() {
        // Canonical PlayStation face symbols
        assertEquals(PlayStationShape.CROSS, getPlayStationShape("✕"))
        assertEquals(PlayStationShape.CIRCLE, getPlayStationShape("○"))
        assertEquals(PlayStationShape.SQUARE, getPlayStationShape("□"))
        assertEquals(PlayStationShape.TRIANGLE, getPlayStationShape("△"))

        // Accepted variations
        assertEquals(PlayStationShape.CROSS, getPlayStationShape("×"))
        assertEquals(PlayStationShape.CROSS, getPlayStationShape("CROSS"))
        assertEquals(PlayStationShape.CIRCLE, getPlayStationShape("◯"))
        assertEquals(PlayStationShape.CIRCLE, getPlayStationShape("CIRCLE"))
        assertEquals(PlayStationShape.SQUARE, getPlayStationShape("◻"))
        assertEquals(PlayStationShape.SQUARE, getPlayStationShape("SQUARE"))
        assertEquals(PlayStationShape.TRIANGLE, getPlayStationShape("▲"))
        assertEquals(PlayStationShape.TRIANGLE, getPlayStationShape("∆"))
        assertEquals(PlayStationShape.TRIANGLE, getPlayStationShape("TRIANGLE"))

        // Boolean helper
        assertTrue(isPlayStationSymbol("✕"))
        assertTrue(isPlayStationSymbol("○"))
        assertTrue(isPlayStationSymbol("□"))
        assertTrue(isPlayStationSymbol("△"))

        // Non-PlayStation symbols and words must return null & false
        assertNull(getPlayStationShape("A"))
        assertNull(getPlayStationShape("B"))
        assertNull(getPlayStationShape("X"))
        assertNull(getPlayStationShape("Y"))
        assertNull(getPlayStationShape("LB"))
        assertNull(getPlayStationShape("L1"))
        assertNull(getPlayStationShape("RB"))
        assertNull(getPlayStationShape("R1"))
        assertNull(getPlayStationShape("ATTACK"))
        assertNull(getPlayStationShape("DASH"))
        assertNull(getPlayStationShape("FIRE"))
        assertNull(getPlayStationShape("JUMP"))
        assertNull(getPlayStationShape(null))
        assertNull(getPlayStationShape(""))

        assertFalse(isPlayStationSymbol("A"))
        assertFalse(isPlayStationSymbol("ATTACK"))
        assertFalse(isPlayStationSymbol(null))
    }

    @Test
    fun testStarterAndAiPromptGeneratedCodeResolutionPipeline() {
        // 1. Starter Code Buttons (defaultControl = "A", "B", "X", "Y")
        val starterButtons = listOf("A", "B", "X", "Y", "LB", "RB", "LT", "RT", "LSB", "RSB")

        for (btn in starterButtons) {
            // Under Xbox Style
            val xboxLabel = CategoryManager.resolveGlyphForStyle(btn, btn, ControllerLabelStyle.XBOX)
            assertEquals(btn, xboxLabel)
            assertNull("Xbox label should not be a PlayStation vector shape", getPlayStationShape(xboxLabel))

            // Under PlayStation Style
            val psLabel = CategoryManager.resolveGlyphForStyle(btn, btn, ControllerLabelStyle.PLAYSTATION)
            when (btn) {
                "A" -> {
                    assertEquals("✕", psLabel)
                    assertEquals(PlayStationShape.CROSS, getPlayStationShape(psLabel))
                }
                "B" -> {
                    assertEquals("○", psLabel)
                    assertEquals(PlayStationShape.CIRCLE, getPlayStationShape(psLabel))
                }
                "X" -> {
                    assertEquals("□", psLabel)
                    assertEquals(PlayStationShape.SQUARE, getPlayStationShape(psLabel))
                }
                "Y" -> {
                    assertEquals("△", psLabel)
                    assertEquals(PlayStationShape.TRIANGLE, getPlayStationShape(psLabel))
                }
                "LB" -> {
                    assertEquals("L1", psLabel)
                    assertNull("L1 is rendered as text", getPlayStationShape(psLabel))
                }
                "RB" -> {
                    assertEquals("R1", psLabel)
                    assertNull("R1 is rendered as text", getPlayStationShape(psLabel))
                }
                "LT" -> {
                    assertEquals("L2", psLabel)
                    assertNull("L2 is rendered as text", getPlayStationShape(psLabel))
                }
                "RT" -> {
                    assertEquals("R2", psLabel)
                    assertNull("R2 is rendered as text", getPlayStationShape(psLabel))
                }
                "LSB" -> {
                    assertEquals("L3", psLabel)
                    assertNull("L3 is rendered as text", getPlayStationShape(psLabel))
                }
                "RSB" -> {
                    assertEquals("R3", psLabel)
                    assertNull("R3 is rendered as text", getPlayStationShape(psLabel))
                }
            }
        }

        // 2. AI Prompt Generated Buttons with Custom Action Words
        val aiActionWords = listOf("ATTACK", "DASH", "FIRE", "JUMP", "SPRINT", "CROUCH", "RELOAD", "BLOCK")
        for (word in aiActionWords) {
            val xboxRes = CategoryManager.resolveGlyphForStyle(word, "A", ControllerLabelStyle.XBOX)
            val psRes = CategoryManager.resolveGlyphForStyle(word, "A", ControllerLabelStyle.PLAYSTATION)
            assertEquals("Custom action words must remain 100% untouched under Xbox", word, xboxRes)
            assertEquals("Custom action words must remain 100% untouched under PlayStation", word, psRes)
            assertNull("Custom action words must never resolve to PlayStation shape", getPlayStationShape(xboxRes))
            assertNull("Custom action words must never resolve to PlayStation shape", getPlayStationShape(psRes))
        }

        // 3. AI Prompt Generated Buttons where user authored with PlayStation symbols
        assertEquals("A", CategoryManager.resolveGlyphForStyle("✕", "A", ControllerLabelStyle.XBOX))
        assertEquals("✕", CategoryManager.resolveGlyphForStyle("✕", "A", ControllerLabelStyle.PLAYSTATION))
        assertEquals(PlayStationShape.CROSS, getPlayStationShape(CategoryManager.resolveGlyphForStyle("✕", "A", ControllerLabelStyle.PLAYSTATION)))

        assertEquals("B", CategoryManager.resolveGlyphForStyle("○", "B", ControllerLabelStyle.XBOX))
        assertEquals("○", CategoryManager.resolveGlyphForStyle("○", "B", ControllerLabelStyle.PLAYSTATION))
        assertEquals(PlayStationShape.CIRCLE, getPlayStationShape(CategoryManager.resolveGlyphForStyle("○", "B", ControllerLabelStyle.PLAYSTATION)))
    }

    @Test
    fun testJoystickRsDefaultsToAnalogStickAndTouchpadRtpHandlesCamera() {
        // 1. Category and ComponentType classification
        val rsDef = CategoryManager.resolveControl("RS")
        assertNotNull(rsDef)
        assertEquals("RS must strictly be classified as JOYSTICK", ComponentType.JOYSTICK, rsDef?.componentType)

        val rtpDef = CategoryManager.resolveControl("RTP")
        assertNotNull(rtpDef)
        assertEquals("RTP must strictly be classified as TOUCHPAD", ComponentType.TOUCHPAD, rtpDef?.componentType)

        // 2. Default camera mode policy: Joysticks (RS) must default to false (analog stick), NOT touchpad mode
        fun resolveCameraMode(category: String, defaultControl: String, isLeft: Boolean, spCameraMode: Boolean?): Boolean {
            val isTouchpad = category.equals("TOUCHPAD", ignoreCase = true) ||
                    defaultControl.equals("RTP", ignoreCase = true) ||
                    defaultControl.equals("LTP", ignoreCase = true)
            return if (isTouchpad) {
                true
            } else if (!isLeft) {
                spCameraMode ?: false // Default MUST be false!
            } else {
                false
            }
        }

        // By default (no preference set, null):
        assertFalse("RS joystick must default to false (analog stick mode)", resolveCameraMode("JOYSTICK", "RS", false, null))
        assertFalse("LS joystick must always be false (analog stick mode)", resolveCameraMode("JOYSTICK", "LS", true, null))
        assertTrue("RTP touchpad must always be true (camera swipe mode)", resolveCameraMode("TOUCHPAD", "RTP", false, null))
        assertTrue("LTP touchpad must always be true (touch surface mode)", resolveCameraMode("TOUCHPAD", "LTP", true, null))

        // 3. Behavior Verification:
        // Analog Joystick: holding thumb deflected at (clampedX, clampedY) maintains non-zero deflection continuously
        val maxRadius = 80f
        val clampedX = 60f
        val clampedY = -40f
        val normX = (clampedX / maxRadius).coerceIn(-1f, 1f)
        val normY = (-clampedY / maxRadius).coerceIn(-1f, 1f)
        assertTrue("Analog stick X deflection must remain non-zero while held", normX > 0.7f)
        assertTrue("Analog stick Y deflection must remain non-zero while held", normY > 0.4f)
    }

    @Test
    fun testAnalogJoystickSteeringDirectionMatchesFingerVectorWhenDraggingBack() {
        // Test verifying the fix for the bug where dragging a stick right and then partially moving
        // the finger back (while still to the right of the center) flipped the stick to the left.
        val centerX = 200f
        val centerY = 200f
        val maxRadius = 80f
        val deadzone = 5f

        fun computeStickOutput(fingerX: Float, fingerY: Float): Pair<Float, Float> {
            val vecX = fingerX - centerX
            val vecY = fingerY - centerY
            val dist = kotlin.math.hypot(vecX, vecY)
            val (clampedX, clampedY) = if (dist > maxRadius) {
                val angle = kotlin.math.atan2(vecY, vecX)
                Pair(kotlin.math.cos(angle) * maxRadius, kotlin.math.sin(angle) * maxRadius)
            } else {
                Pair(vecX, vecY)
            }
            val normX = if (dist < deadzone) 0f else (clampedX / maxRadius).coerceIn(-1f, 1f)
            val normY = if (dist < deadzone) 0f else (-clampedY / maxRadius).coerceIn(-1f, 1f)
            return Pair(normX, normY)
        }

        // 1. Drag finger far to the right (150px right of center)
        val (step1X, step1Y) = computeStickOutput(350f, 200f)
        assertEquals("Full deflection to the right", 1.0f, step1X, 0.0001f)
        assertEquals(0.0f, step1Y, 0.0001f)

        // 2. Move finger partially back towards center, but STILL 60px to the right of center
        val (step2X, step2Y) = computeStickOutput(260f, 200f)
        assertTrue("Finger is to the right of center, stick MUST point to the right (positive X)", step2X > 0f)
        assertEquals("Proportional deflection at 60px / 80px", 0.75f, step2X, 0.0001f)

        // 3. Move finger to 20px to the right of center
        val (step3X, step3Y) = computeStickOutput(220f, 200f)
        assertTrue("Finger is still to the right of center, stick MUST point to the right", step3X > 0f)
        assertEquals("Proportional deflection at 20px / 80px", 0.25f, step3X, 0.0001f)

        // 4. Move finger across center to the left (60px to the left of center)
        val (step4X, step4Y) = computeStickOutput(140f, 200f)
        assertTrue("Only when finger crosses to the left should stick point left (negative X)", step4X < 0f)
    }

    @Test
    fun testCleanStyleDropdownLabelsAndLayoutActivation() {
        val xboxLabel = "Xbox Style"
        val psLabel = "PlayStation Style"

        // Verify clean strings without button parentheses
        assertFalse("Xbox label should not contain parentheses", xboxLabel.contains("("))
        assertFalse("PlayStation label should not contain parentheses", psLabel.contains("("))
        assertEquals("Xbox Style", xboxLabel)
        assertEquals("PlayStation Style", psLabel)
    }

    @Test
    fun testStickTouchpadMutualExclusivityAndCenterPositionInheritance() {
        // 1. Verify LayoutProfile.canonicalPositions() self-healing mutual exclusivity
        val conflictingProfile = LayoutProfile(
            name = "Test Conflict",
            positions = mapOf(
                ControlKey.LS.key to Position(xRatio = 0.18f, yRatio = 0.72f, scale = 1.2f, opacity = 0.9f),
                ControlKey.LTP.key to Position(xRatio = 0.20f, yRatio = 0.70f, scale = 1.0f, opacity = 1.0f),
                ControlKey.RS.key to Position(xRatio = 0.82f, yRatio = 0.75f, scale = 1.2f, opacity = 0.9f),
                ControlKey.RTP.key to Position(xRatio = 0.80f, yRatio = 0.70f, scale = 1.0f, opacity = 1.0f),
                ControlKey.A.key to Position(xRatio = 0.85f, yRatio = 0.65f, scale = 1.0f, opacity = 1.0f)
            )
        )

        val healedPositions = conflictingProfile.canonicalPositions()
        assertTrue("LS must be kept in healed profile", healedPositions.containsKey(ControlKey.LS.key))
        assertFalse("LTP must be removed when LS is present", healedPositions.containsKey(ControlKey.LTP.key))
        assertTrue("RS must be kept in healed profile", healedPositions.containsKey(ControlKey.RS.key))
        assertFalse("RTP must be removed when RS is present", healedPositions.containsKey(ControlKey.RTP.key))
        assertTrue("A must remain unaffected", healedPositions.containsKey(ControlKey.A.key))

        // 2. Profile with only Touchpads retains both without removal
        val touchpadOnlyProfile = LayoutProfile(
            name = "Touchpad Layout",
            positions = mapOf(
                ControlKey.LTP.key to Position(xRatio = 0.18f, yRatio = 0.72f, scale = 1.1f, opacity = 0.95f),
                ControlKey.RTP.key to Position(xRatio = 0.82f, yRatio = 0.75f, scale = 1.1f, opacity = 0.95f)
            )
        )
        val touchpadHealed = touchpadOnlyProfile.canonicalPositions()
        assertTrue("LTP is retained when LS is absent", touchpadHealed.containsKey(ControlKey.LTP.key))
        assertTrue("RTP is retained when RS is absent", touchpadHealed.containsKey(ControlKey.RTP.key))

        // 3. Test Position Inheritance Simulation
        fun simulateAddControl(
            currentMap: Map<String, LayoutTransform>,
            newKey: String
        ): Map<String, LayoutTransform> {
            val canonicalKey = ControlKey.fromIdentifier(newKey)?.key ?: newKey.uppercase()
            var elements = currentMap
            var inheritedTransform: LayoutTransform? = null

            if (canonicalKey == ControlKey.LTP.key) {
                val lsEntry = elements.entries.firstOrNull { it.key == ControlKey.LS.key }
                if (lsEntry != null) {
                    inheritedTransform = lsEntry.value
                    elements = elements - lsEntry.key
                }
            } else if (canonicalKey == ControlKey.LS.key) {
                val ltpEntry = elements.entries.firstOrNull { it.key == ControlKey.LTP.key }
                if (ltpEntry != null) {
                    inheritedTransform = ltpEntry.value
                    elements = elements - ltpEntry.key
                }
            } else if (canonicalKey == ControlKey.RTP.key) {
                val rsEntry = elements.entries.firstOrNull { it.key == ControlKey.RS.key }
                if (rsEntry != null) {
                    inheritedTransform = rsEntry.value
                    elements = elements - rsEntry.key
                }
            } else if (canonicalKey == ControlKey.RS.key) {
                val rtpEntry = elements.entries.firstOrNull { it.key == ControlKey.RTP.key }
                if (rtpEntry != null) {
                    inheritedTransform = rtpEntry.value
                    elements = elements - rtpEntry.key
                }
            }

            val finalTransform = inheritedTransform?.copy() ?: LayoutTransform(0.5f, 0.5f, 1f, 1f)
            return elements + (canonicalKey to finalTransform)
        }

        // LS replaced with LTP inherits exact position
        val initialLayout = mapOf(
            ControlKey.LS.key to LayoutTransform(xRatio = 0.165f, yRatio = 0.735f, scale = 1.25f, opacity = 0.88f),
            ControlKey.RS.key to LayoutTransform(xRatio = 0.835f, yRatio = 0.735f, scale = 1.25f, opacity = 0.88f)
        )

        val afterAddingLtp = simulateAddControl(initialLayout, "LTP")
        assertFalse("LS must be removed", afterAddingLtp.containsKey("LS"))
        assertTrue("LTP must be present", afterAddingLtp.containsKey("LTP"))
        val ltpTransform = afterAddingLtp["LTP"]!!
        assertEquals(0.165f, ltpTransform.xRatio, 0.0001f)
        assertEquals(0.735f, ltpTransform.yRatio, 0.0001f)
        assertEquals(1.25f, ltpTransform.scale, 0.0001f)
        assertEquals(0.88f, ltpTransform.opacity, 0.0001f)

        // Switching back from LTP to LS inherits exact position
        val afterSwitchingBackToLs = simulateAddControl(afterAddingLtp, "LS")
        assertFalse("LTP must be removed", afterSwitchingBackToLs.containsKey("LTP"))
        assertTrue("LS must be present", afterSwitchingBackToLs.containsKey("LS"))
        val lsRestoredTransform = afterSwitchingBackToLs["LS"]!!
        assertEquals(0.165f, lsRestoredTransform.xRatio, 0.0001f)
        assertEquals(0.735f, lsRestoredTransform.yRatio, 0.0001f)
        assertEquals(1.25f, lsRestoredTransform.scale, 0.0001f)
        assertEquals(0.88f, lsRestoredTransform.opacity, 0.0001f)

        // RS replaced with RTP inherits exact position
        val afterAddingRtp = simulateAddControl(afterSwitchingBackToLs, "RTP")
        assertFalse("RS must be removed", afterAddingRtp.containsKey("RS"))
        assertTrue("RTP must be present", afterAddingRtp.containsKey("RTP"))
        val rtpTransform = afterAddingRtp["RTP"]!!
        assertEquals(0.835f, rtpTransform.xRatio, 0.0001f)
        assertEquals(0.735f, rtpTransform.yRatio, 0.0001f)
        assertEquals(1.25f, rtpTransform.scale, 0.0001f)
        assertEquals(0.88f, rtpTransform.opacity, 0.0001f)
    }

    @Test
    fun testInbuildTouchpadVariantsRegistration() {
        val ltpVariant = com.sanket.tools.nexpad.runtime.registry.DefaultNativeFamily.getVariant("builtin.inbuild_ltp")
        assertNotNull("Inbuild LTP variant must be registered", ltpVariant)
        assertEquals(ControlKey.LTP, ltpVariant?.controlKey)
        assertEquals("Inbuild Surface", ltpVariant?.variantName)
        assertEquals(6003, ltpVariant?.seedCode)

        val rtpVariant = com.sanket.tools.nexpad.runtime.registry.DefaultNativeFamily.getVariant("builtin.inbuild_rtp")
        assertNotNull("Inbuild RTP variant must be registered", rtpVariant)
        assertEquals(ControlKey.RTP, rtpVariant?.controlKey)
        assertEquals("Inbuild Surface", rtpVariant?.variantName)
        assertEquals(6004, rtpVariant?.seedCode)
    }

    @Test
    fun testInbuildTouchpadButtonBufferZoneExclusion() {
        // Universal 16.dp buffer zone Euclidean distance check
        val marginPx = 16f
        val marginSq = marginPx * marginPx

        // Simulated button placed at (100, 200) with size 80x80 -> Center = (140, 240)
        val buttonCenterX = 140f
        val buttonCenterY = 240f
        val buttonHalfW = 40f
        val buttonHalfH = 40f

        fun isNearButton(touchX: Float, touchY: Float): Boolean {
            val dx = maxOf(kotlin.math.abs(touchX - buttonCenterX) - buttonHalfW, 0f)
            val dy = maxOf(kotlin.math.abs(touchY - buttonCenterY) - buttonHalfH, 0f)
            return (dx * dx + dy * dy) <= marginSq
        }

        // 1. Direct hit on the button (e.g. center) -> must be excluded (touchpad ignored)
        assertTrue("Direct button tap must be excluded from touchpad", isNearButton(140f, 240f))

        // 2. Touch 8px outside the right edge of button -> within 16px buffer -> must be excluded
        assertTrue("Touch 8px right of button edge must be excluded", isNearButton(188f, 240f))

        // 3. Touch 15px outside the bottom edge -> within 16px buffer -> must be excluded
        assertTrue("Touch 15px below button edge must be excluded", isNearButton(140f, 295f))

        // 4. Touch 10px right and 10px down from corner -> Euclidean dist = sqrt(200) = 14.14px <= 16px -> excluded
        assertTrue("Touch 10px from corner is within Euclidean buffer zone", isNearButton(190f, 290f))

        // 5. Touch 20px outside button edge -> outside 16px buffer -> NOT excluded (touchpad triggers!)
        assertFalse("Touch 20px outside button is clear of buffer zone", isNearButton(200f, 240f))

        // 6. Touch far away in empty space -> NOT excluded (touchpad triggers!)
        assertFalse("Touch in empty space triggers touchpad smoothly", isNearButton(500f, 500f))
    }

    @Test
    fun testInbuildTouchpadHalfScreenPartition() {
        val screenWidthPx = 2400f
        val halfWidthPx = screenWidthPx / 2f // 1200f

        fun isTouchpadRegionLeft(touchX: Float): Boolean = touchX < halfWidthPx

        // Left screen touches belong to LTP
        assertTrue("X=100 is in left (LTP) region", isTouchpadRegionLeft(100f))
        assertTrue("X=1199 is in left (LTP) region", isTouchpadRegionLeft(1199f))

        // Right screen touches belong to RTP
        assertFalse("X=1200 is in right (RTP) region", isTouchpadRegionLeft(1200f))
        assertFalse("X=2200 is in right (RTP) region", isTouchpadRegionLeft(2200f))
    }
}




