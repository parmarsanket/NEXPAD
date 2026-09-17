package com.sanket.tools.nexpad

import com.sanket.tools.nexpad.category.CategoryManager
import com.sanket.tools.nexpad.model.HudElement
import com.sanket.tools.nexpad.model.LayoutSkin
import com.sanket.tools.nexpad.model.LayoutTransform
import com.sanket.tools.nexpad.model.Position
import org.junit.Assert.*
import org.junit.Test

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
}
