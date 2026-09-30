package com.sanket.tools.nexpad

import com.sanket.tools.nexpad.category.CategoryManager
import com.sanket.tools.nexpad.category.ControlKey
import com.sanket.tools.nexpad.runtime.registry.DefaultNativeFamily
import com.sanket.tools.nexpad.runtime.registry.NativeComponentRegistry
import com.sanket.tools.nexpad.ui.studio.model.ButtonStudioType
import com.sanket.tools.nexpad.ui.studio.model.resolveButtonSourceType
import org.junit.Assert.*
import org.junit.Test

/**
 * Verification unit tests for the unified Default Native OOP architecture:
 * - DefaultNativeFamily registration and seed code determinism
 * - Fallback and resolution semantics of NativeComponentRegistry
 * - Studio categorization badges (all native variants map to DEFAULT)
 * - CategoryManager dynamic control resolution for new native variants
 */
class NativeComponentRegistryTest {

    @Test
    fun testBaselineVariantsRegistered() {
        val baselineControls = listOf(
            ControlKey.A, ControlKey.B, ControlKey.X, ControlKey.Y,
            ControlKey.DPAD, ControlKey.UP, ControlKey.DOWN, ControlKey.LEFT, ControlKey.RIGHT,
            ControlKey.LT, ControlKey.RT, ControlKey.LB, ControlKey.RB,
            ControlKey.LS, ControlKey.RS, ControlKey.LSB, ControlKey.RSB,
            ControlKey.LTP, ControlKey.RTP,
            ControlKey.GUIDE, ControlKey.START, ControlKey.BACK, ControlKey.SHARE,
            ControlKey.M1, ControlKey.M2, ControlKey.M3, ControlKey.M4
        )

        baselineControls.forEach { ctrl ->
            val defaultVariant = DefaultNativeFamily.getDefaultVariant(ctrl)
            assertNotNull("Baseline default variant must exist for ${ctrl.key}", defaultVariant)
            assertTrue("Variant for ${ctrl.key} must be marked isBaselineDefault", defaultVariant!!.isBaselineDefault)
            assertEquals(ctrl, defaultVariant.controlKey)
        }
    }

    @Test
    fun testFluxCyberVariantsRegistered() {
        val fluxLs = DefaultNativeFamily.getVariant("builtin.flux_ls")
        assertNotNull("Flux LS must be registered in DefaultNativeFamily", fluxLs)
        assertEquals(ControlKey.LS, fluxLs!!.controlKey)
        assertEquals(102, fluxLs.seedCode)
        assertFalse(fluxLs.isBaselineDefault)
        assertEquals("Flux Cyber", fluxLs.variantName)

        val fluxRs = DefaultNativeFamily.getVariant("builtin.flux_rs")
        assertNotNull("Flux RS must be registered in DefaultNativeFamily", fluxRs)
        assertEquals(ControlKey.RS, fluxRs!!.controlKey)
        assertEquals(202, fluxRs.seedCode)
        assertFalse(fluxRs.isBaselineDefault)

        val fluxLsb = DefaultNativeFamily.getVariant("builtin.flux_lsb")
        assertNotNull("Flux LSB must be registered in DefaultNativeFamily", fluxLsb)
        assertEquals(ControlKey.LSB, fluxLsb!!.controlKey)
        assertEquals(302, fluxLsb.seedCode)
        assertFalse(fluxLsb.isBaselineDefault)

        val fluxRsb = DefaultNativeFamily.getVariant("builtin.flux_rsb")
        assertNotNull("Flux RSB must be registered in DefaultNativeFamily", fluxRsb)
        assertEquals(ControlKey.RSB, fluxRsb!!.controlKey)
        assertEquals(402, fluxRsb.seedCode)
        assertFalse(fluxRsb.isBaselineDefault)
    }

    @Test
    fun testOrbGlassVariantsRegistered() {
        val orbLs = DefaultNativeFamily.getVariant("builtin.orb_ls")
        assertNotNull("Orb LS must be registered in DefaultNativeFamily", orbLs)
        assertEquals(ControlKey.LS, orbLs!!.controlKey)
        assertEquals(103, orbLs.seedCode)
        assertFalse(orbLs.isBaselineDefault)
        assertEquals("Orb Glass", orbLs.variantName)

        val orbRs = DefaultNativeFamily.getVariant("builtin.orb_rs")
        assertNotNull("Orb RS must be registered in DefaultNativeFamily", orbRs)
        assertEquals(ControlKey.RS, orbRs!!.controlKey)
        assertEquals(203, orbRs.seedCode)
        assertFalse(orbRs.isBaselineDefault)

        val orbLsb = DefaultNativeFamily.getVariant("builtin.orb_lsb")
        assertNotNull("Orb LSB must be registered in DefaultNativeFamily", orbLsb)
        assertEquals(ControlKey.LSB, orbLsb!!.controlKey)
        assertEquals(303, orbLsb.seedCode)
        assertFalse(orbLsb.isBaselineDefault)

        val orbRsb = DefaultNativeFamily.getVariant("builtin.orb_rsb")
        assertNotNull("Orb RSB must be registered in DefaultNativeFamily", orbRsb)
        assertEquals(ControlKey.RSB, orbRsb!!.controlKey)
        assertEquals(403, orbRsb.seedCode)
        assertFalse(orbRsb.isBaselineDefault)
    }

    @Test
    fun testCompassNavVariantsRegistered() {
        val compassLs = DefaultNativeFamily.getVariant("builtin.compass_ls")
        assertNotNull("Compass LS must be registered in DefaultNativeFamily", compassLs)
        assertEquals(ControlKey.LS, compassLs!!.controlKey)
        assertEquals(104, compassLs.seedCode)
        assertFalse(compassLs.isBaselineDefault)
        assertEquals("Compass Nav", compassLs.variantName)

        val compassRs = DefaultNativeFamily.getVariant("builtin.compass_rs")
        assertNotNull("Compass RS must be registered in DefaultNativeFamily", compassRs)
        assertEquals(ControlKey.RS, compassRs!!.controlKey)
        assertEquals(204, compassRs.seedCode)
        assertFalse(compassRs.isBaselineDefault)

        val compassLsb = DefaultNativeFamily.getVariant("builtin.compass_lsb")
        assertNotNull("Compass LSB must be registered in DefaultNativeFamily", compassLsb)
        assertEquals(ControlKey.LSB, compassLsb!!.controlKey)
        assertEquals(304, compassLsb.seedCode)
        assertFalse(compassLsb.isBaselineDefault)

        val compassRsb = DefaultNativeFamily.getVariant("builtin.compass_rsb")
        assertNotNull("Compass RSB must be registered in DefaultNativeFamily", compassRsb)
        assertEquals(ControlKey.RSB, compassRsb!!.controlKey)
        assertEquals(404, compassRsb.seedCode)
        assertFalse(compassRsb.isBaselineDefault)
    }

    @Test
    fun testGyroGimbalVariantsRegistered() {
        val gyroLs = DefaultNativeFamily.getVariant("builtin.gyro_ls")
        assertNotNull("Gyro LS must be registered in DefaultNativeFamily", gyroLs)
        assertEquals(ControlKey.LS, gyroLs!!.controlKey)
        assertEquals(105, gyroLs.seedCode)
        assertFalse(gyroLs.isBaselineDefault)
        assertEquals("Gyro Gimbal", gyroLs.variantName)

        val gyroRs = DefaultNativeFamily.getVariant("builtin.gyro_rs")
        assertNotNull("Gyro RS must be registered in DefaultNativeFamily", gyroRs)
        assertEquals(ControlKey.RS, gyroRs!!.controlKey)
        assertEquals(205, gyroRs.seedCode)
        assertFalse(gyroRs.isBaselineDefault)

        val gyroLsb = DefaultNativeFamily.getVariant("builtin.gyro_lsb")
        assertNotNull("Gyro LSB must be registered in DefaultNativeFamily", gyroLsb)
        assertEquals(ControlKey.LSB, gyroLsb!!.controlKey)
        assertEquals(305, gyroLsb.seedCode)
        assertFalse(gyroLsb.isBaselineDefault)

        val gyroRsb = DefaultNativeFamily.getVariant("builtin.gyro_rsb")
        assertNotNull("Gyro RSB must be registered in DefaultNativeFamily", gyroRsb)
        assertEquals(ControlKey.RSB, gyroRsb!!.controlKey)
        assertEquals(405, gyroRsb.seedCode)
        assertFalse(gyroRsb.isBaselineDefault)
    }

    @Test
    fun testSpotlightVariantsRegistered() {
        val spotLs = DefaultNativeFamily.getVariant("builtin.spotlight_ls")
        assertNotNull("Spotlight LS must be registered in DefaultNativeFamily", spotLs)
        assertEquals(ControlKey.LS, spotLs!!.controlKey)
        assertEquals(106, spotLs.seedCode)
        assertFalse(spotLs.isBaselineDefault)
        assertEquals("Spotlight", spotLs.variantName)

        val spotRs = DefaultNativeFamily.getVariant("builtin.spotlight_rs")
        assertNotNull("Spotlight RS must be registered in DefaultNativeFamily", spotRs)
        assertEquals(ControlKey.RS, spotRs!!.controlKey)
        assertEquals(206, spotRs.seedCode)
        assertFalse(spotRs.isBaselineDefault)

        val spotLsb = DefaultNativeFamily.getVariant("builtin.spotlight_lsb")
        assertNotNull("Spotlight LSB must be registered in DefaultNativeFamily", spotLsb)
        assertEquals(ControlKey.LSB, spotLsb!!.controlKey)
        assertEquals(306, spotLsb.seedCode)
        assertFalse(spotLsb.isBaselineDefault)

        val spotRsb = DefaultNativeFamily.getVariant("builtin.spotlight_rsb")
        assertNotNull("Spotlight RSB must be registered in DefaultNativeFamily", spotRsb)
        assertEquals(ControlKey.RSB, spotRsb!!.controlKey)
        assertEquals(406, spotRsb.seedCode)
        assertFalse(spotRsb.isBaselineDefault)
    }

    @Test
    fun testLensDPadVariantsRegistered() {
        val lensDpad = DefaultNativeFamily.getVariant("builtin.lens_dpad")
        assertNotNull("Lens D-Pad must be registered in DefaultNativeFamily", lensDpad)
        assertEquals(ControlKey.DPAD, lensDpad!!.controlKey)
        assertEquals(4101, lensDpad.seedCode)
        assertFalse(lensDpad.isBaselineDefault)
        assertEquals("Lens Cross", lensDpad.variantName)

        val fourLensesDpad = DefaultNativeFamily.getVariant("builtin.four_lenses_dpad")
        assertNotNull("Four Lenses D-Pad must be registered in DefaultNativeFamily", fourLensesDpad)
        assertEquals(ControlKey.DPAD, fourLensesDpad!!.controlKey)
        assertEquals(4102, fourLensesDpad.seedCode)
        assertFalse(fourLensesDpad.isBaselineDefault)
        assertEquals("Four Lenses", fourLensesDpad.variantName)

        val discDpad = DefaultNativeFamily.getVariant("builtin.disc_dpad")
        assertNotNull("Disc D-Pad must be registered in DefaultNativeFamily", discDpad)
        assertEquals(ControlKey.DPAD, discDpad!!.controlKey)
        assertEquals(4103, discDpad.seedCode)
        assertFalse(discDpad.isBaselineDefault)
        assertEquals("Lens Disc", discDpad.variantName)

        val capsulesDpad = DefaultNativeFamily.getVariant("builtin.capsules_dpad")
        assertNotNull("Capsules D-Pad must be registered in DefaultNativeFamily", capsulesDpad)
        assertEquals(ControlKey.DPAD, capsulesDpad!!.controlKey)
        assertEquals(4104, capsulesDpad.seedCode)
        assertFalse(capsulesDpad.isBaselineDefault)
        assertEquals("Lens Capsules", capsulesDpad.variantName)

        val metaballsDpad = DefaultNativeFamily.getVariant("builtin.metaballs_dpad")
        assertNotNull("Metaballs D-Pad must be registered in DefaultNativeFamily", metaballsDpad)
        assertEquals(ControlKey.DPAD, metaballsDpad!!.controlKey)
        assertEquals(4105, metaballsDpad.seedCode)
        assertFalse(metaballsDpad.isBaselineDefault)
        assertEquals("Lens Metaballs", metaballsDpad.variantName)

        val railsDpad = DefaultNativeFamily.getVariant("builtin.rails_dpad")
        assertNotNull("Rails D-Pad must be registered in DefaultNativeFamily", railsDpad)
        assertEquals(ControlKey.DPAD, railsDpad!!.controlKey)
        assertEquals(4106, railsDpad.seedCode)
        assertFalse(railsDpad.isBaselineDefault)
        assertEquals("Lens Rails", railsDpad.variantName)
    }

    @Test
    fun testMultipleVariantsPerControlKey() {
        val lsVariants = DefaultNativeFamily.getVariantsFor(ControlKey.LS)
        assertTrue("LS must have at least 6 variants (Realistic, Flux, Orb, Compass, Gyro, Spotlight)", lsVariants.size >= 6)
        assertTrue(lsVariants.any { it.id == "builtin.default_ls" && it.seedCode == 101 })
        assertTrue(lsVariants.any { it.id == "builtin.flux_ls" && it.seedCode == 102 })
        assertTrue(lsVariants.any { it.id == "builtin.orb_ls" && it.seedCode == 103 })
        assertTrue(lsVariants.any { it.id == "builtin.compass_ls" && it.seedCode == 104 })
        assertTrue(lsVariants.any { it.id == "builtin.gyro_ls" && it.seedCode == 105 })
        assertTrue(lsVariants.any { it.id == "builtin.spotlight_ls" && it.seedCode == 106 })

        val rsVariants = DefaultNativeFamily.getVariantsFor(ControlKey.RS)
        assertTrue("RS must have at least 6 variants", rsVariants.size >= 6)
        assertTrue(rsVariants.any { it.id == "builtin.default_rs" && it.seedCode == 201 })
        assertTrue(rsVariants.any { it.id == "builtin.flux_rs" && it.seedCode == 202 })
        assertTrue(rsVariants.any { it.id == "builtin.orb_rs" && it.seedCode == 203 })
        assertTrue(rsVariants.any { it.id == "builtin.compass_rs" && it.seedCode == 204 })
        assertTrue(rsVariants.any { it.id == "builtin.gyro_rs" && it.seedCode == 205 })
        assertTrue(rsVariants.any { it.id == "builtin.spotlight_rs" && it.seedCode == 206 })

        val dpadVariants = DefaultNativeFamily.getVariantsFor(ControlKey.DPAD)
        assertTrue("DPAD must have at least 7 variants (Realistic, Lens Cross, Four Lenses, Lens Disc, Lens Capsules, Lens Metaballs, Lens Rails)", dpadVariants.size >= 7)
        assertTrue(dpadVariants.any { it.id == "builtin.default_dpad" && it.seedCode == 4001 })
        assertTrue(dpadVariants.any { it.id == "builtin.lens_dpad" && it.seedCode == 4101 })
        assertTrue(dpadVariants.any { it.id == "builtin.four_lenses_dpad" && it.seedCode == 4102 })
        assertTrue(dpadVariants.any { it.id == "builtin.disc_dpad" && it.seedCode == 4103 })
        assertTrue(dpadVariants.any { it.id == "builtin.capsules_dpad" && it.seedCode == 4104 })
        assertTrue(dpadVariants.any { it.id == "builtin.metaballs_dpad" && it.seedCode == 4105 })
        assertTrue(dpadVariants.any { it.id == "builtin.rails_dpad" && it.seedCode == 4106 })
    }

    @Test
    fun testResolveVariantWithCustomIdAndFallback() {
        // Null or blank ID -> baseline default (seedCode 101)
        val defaultLs = NativeComponentRegistry.resolveVariant("LS", null)
        assertNotNull(defaultLs)
        assertEquals(101, defaultLs!!.seedCode)

        val blankLs = NativeComponentRegistry.resolveVariant("LS", "")
        assertNotNull(blankLs)
        assertEquals(101, blankLs!!.seedCode)

        // Flux custom ID -> Flux variant (seedCode 102)
        val fluxLs = NativeComponentRegistry.resolveVariant("LS", "builtin.flux_ls")
        assertNotNull(fluxLs)
        assertEquals(102, fluxLs!!.seedCode)

        // Orb custom ID -> Orb variant (seedCode 103)
        val orbLs = NativeComponentRegistry.resolveVariant("LS", "builtin.orb_ls")
        assertNotNull(orbLs)
        assertEquals(103, orbLs!!.seedCode)

        // Compass custom ID -> Compass variant (seedCode 104)
        val compassLs = NativeComponentRegistry.resolveVariant("LS", "builtin.compass_ls")
        assertNotNull(compassLs)
        assertEquals(104, compassLs!!.seedCode)

        // Gyro custom ID -> Gyro variant (seedCode 105)
        val gyroLs = NativeComponentRegistry.resolveVariant("LS", "builtin.gyro_ls")
        assertNotNull(gyroLs)
        assertEquals(105, gyroLs!!.seedCode)

        // Spotlight custom ID -> Spotlight variant (seedCode 106)
        val spotLs = NativeComponentRegistry.resolveVariant("LS", "builtin.spotlight_ls")
        assertNotNull(spotLs)
        assertEquals(106, spotLs!!.seedCode)

        // Lens D-Pad custom ID -> Lens variant (seedCode 4101)
        val lensDpad = NativeComponentRegistry.resolveVariant("DPAD", "builtin.lens_dpad")
        assertNotNull(lensDpad)
        assertEquals(4101, lensDpad!!.seedCode)

        // Four Lenses D-Pad custom ID -> Four Lenses variant (seedCode 4102)
        val fourLensesDpad = NativeComponentRegistry.resolveVariant("DPAD", "builtin.four_lenses_dpad")
        assertNotNull(fourLensesDpad)
        assertEquals(4102, fourLensesDpad!!.seedCode)

        // Disc D-Pad custom ID -> Disc variant (seedCode 4103)
        val discDpad = NativeComponentRegistry.resolveVariant("DPAD", "builtin.disc_dpad")
        assertNotNull(discDpad)
        assertEquals(4103, discDpad!!.seedCode)

        // Capsules D-Pad custom ID -> Capsules variant (seedCode 4104)
        val capsulesDpad = NativeComponentRegistry.resolveVariant("DPAD", "builtin.capsules_dpad")
        assertNotNull(capsulesDpad)
        assertEquals(4104, capsulesDpad!!.seedCode)

        // Metaballs D-Pad custom ID -> Metaballs variant (seedCode 4105)
        val metaballsDpad = NativeComponentRegistry.resolveVariant("DPAD", "builtin.metaballs_dpad")
        assertNotNull(metaballsDpad)
        assertEquals(4105, metaballsDpad!!.seedCode)

        // Rails D-Pad custom ID -> Rails variant (seedCode 4106)
        val railsDpad = NativeComponentRegistry.resolveVariant("DPAD", "builtin.rails_dpad")
        assertNotNull(railsDpad)
        assertEquals(4106, railsDpad!!.seedCode)

        // Non-existent custom ID -> falls back gracefully to baseline default
        val fallbackLs = NativeComponentRegistry.resolveVariant("LS", "non_existent_custom_id")
        assertNotNull(fallbackLs)
        assertEquals(101, fallbackLs!!.seedCode)
    }

    @Test
    fun testIsNativeBuiltin() {
        assertTrue(NativeComponentRegistry.isNativeBuiltin(null))
        assertTrue(NativeComponentRegistry.isNativeBuiltin(""))
        assertTrue(NativeComponentRegistry.isNativeBuiltin("builtin.default_a"))
        assertTrue(NativeComponentRegistry.isNativeBuiltin("builtin.default_ls"))
        assertTrue(NativeComponentRegistry.isNativeBuiltin("builtin.flux_ls"))
        assertTrue(NativeComponentRegistry.isNativeBuiltin("builtin.flux_rsb"))
        assertTrue(NativeComponentRegistry.isNativeBuiltin("builtin.orb_ls"))
        assertTrue(NativeComponentRegistry.isNativeBuiltin("builtin.orb_rsb"))
        assertTrue(NativeComponentRegistry.isNativeBuiltin("builtin.lens_dpad"))
        assertTrue(NativeComponentRegistry.isNativeBuiltin("builtin.four_lenses_dpad"))
        assertTrue(NativeComponentRegistry.isNativeBuiltin("builtin.disc_dpad"))
        assertTrue(NativeComponentRegistry.isNativeBuiltin("builtin.capsules_dpad"))
        assertTrue(NativeComponentRegistry.isNativeBuiltin("builtin.metaballs_dpad"))
        assertTrue(NativeComponentRegistry.isNativeBuiltin("builtin.rails_dpad"))

        assertFalse(NativeComponentRegistry.isNativeBuiltin("rc.action_a"))
        assertFalse(NativeComponentRegistry.isNativeBuiltin("custom_imported_skin_123"))
    }

    @Test
    fun testResolveButtonSourceTypeAlwaysDefaultBadgeForNative() {
        assertEquals(ButtonStudioType.DEFAULT, resolveButtonSourceType(null))
        assertEquals(ButtonStudioType.DEFAULT, resolveButtonSourceType("builtin.default_a"))
        assertEquals(ButtonStudioType.DEFAULT, resolveButtonSourceType("builtin.flux_ls"))
        assertEquals(ButtonStudioType.DEFAULT, resolveButtonSourceType("builtin.flux_rs"))
        assertEquals(ButtonStudioType.DEFAULT, resolveButtonSourceType("builtin.flux_lsb"))
        assertEquals(ButtonStudioType.DEFAULT, resolveButtonSourceType("builtin.flux_rsb"))
        assertEquals(ButtonStudioType.DEFAULT, resolveButtonSourceType("builtin.orb_ls"))
        assertEquals(ButtonStudioType.DEFAULT, resolveButtonSourceType("builtin.orb_rs"))
        assertEquals(ButtonStudioType.DEFAULT, resolveButtonSourceType("builtin.orb_lsb"))
        assertEquals(ButtonStudioType.DEFAULT, resolveButtonSourceType("builtin.orb_rsb"))
        assertEquals(ButtonStudioType.DEFAULT, resolveButtonSourceType("builtin.lens_dpad"))
        assertEquals(ButtonStudioType.DEFAULT, resolveButtonSourceType("builtin.four_lenses_dpad"))
        assertEquals(ButtonStudioType.DEFAULT, resolveButtonSourceType("builtin.disc_dpad"))
        assertEquals(ButtonStudioType.DEFAULT, resolveButtonSourceType("builtin.capsules_dpad"))
        assertEquals(ButtonStudioType.DEFAULT, resolveButtonSourceType("builtin.metaballs_dpad"))
        assertEquals(ButtonStudioType.DEFAULT, resolveButtonSourceType("builtin.rails_dpad"))

        assertEquals(ButtonStudioType.REMOTE_COMPOSE, resolveButtonSourceType("rc.action_a"))
        assertEquals(ButtonStudioType.PLUGIN, resolveButtonSourceType("user_custom_a"))
    }

    @Test
    fun testCategoryManagerResolvesFluxAndOrbIdentifiers() {
        assertEquals(ControlKey.LS, CategoryManager.resolveControl("builtin.flux_ls"))
        assertEquals(ControlKey.RS, CategoryManager.resolveControl("builtin.flux_rs"))
        assertEquals(ControlKey.LSB, CategoryManager.resolveControl("builtin.flux_lsb"))
        assertEquals(ControlKey.RSB, CategoryManager.resolveControl("builtin.flux_rsb"))

        assertEquals(ControlKey.LS, CategoryManager.resolveControl("builtin.orb_ls"))
        assertEquals(ControlKey.RS, CategoryManager.resolveControl("builtin.orb_rs"))
        assertEquals(ControlKey.LSB, CategoryManager.resolveControl("builtin.orb_lsb"))
        assertEquals(ControlKey.RSB, CategoryManager.resolveControl("builtin.orb_rsb"))
    }
}
