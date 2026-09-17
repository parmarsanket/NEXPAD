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
}
