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

        // Arc Bumper custom ID -> Arc variants (seedCode 3101 & 3102)
        val arcLb = NativeComponentRegistry.resolveVariant("LB", "builtin.arc_lb")
        assertNotNull(arcLb)
        assertEquals(3101, arcLb!!.seedCode)

        val arcRb = NativeComponentRegistry.resolveVariant("RB", "builtin.arc_rb")
        assertNotNull(arcRb)
        assertEquals(3102, arcRb!!.seedCode)

        // LED Bar Bumper custom ID -> LED variants (seedCode 3201 & 3202)
        val ledLb = NativeComponentRegistry.resolveVariant("LB", "builtin.led_lb")
        assertNotNull(ledLb)
        assertEquals(3201, ledLb!!.seedCode)

        val ledRb = NativeComponentRegistry.resolveVariant("RB", "builtin.led_rb")
        assertNotNull(ledRb)
        assertEquals(3202, ledRb!!.seedCode)

        // Peek Bumper custom ID -> Peek variants (seedCode 3301 & 3302)
        val peekLb = NativeComponentRegistry.resolveVariant("LB", "builtin.peek_lb")
        assertNotNull(peekLb)
        assertEquals(3301, peekLb!!.seedCode)

        val peekRb = NativeComponentRegistry.resolveVariant("RB", "builtin.peek_rb")
        assertNotNull(peekRb)
        assertEquals(3302, peekRb!!.seedCode)

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
        assertTrue(NativeComponentRegistry.isNativeBuiltin("builtin.arc_lb"))
        assertTrue(NativeComponentRegistry.isNativeBuiltin("builtin.arc_rb"))
        assertTrue(NativeComponentRegistry.isNativeBuiltin("builtin.led_lb"))
        assertTrue(NativeComponentRegistry.isNativeBuiltin("builtin.led_rb"))
        assertTrue(NativeComponentRegistry.isNativeBuiltin("builtin.peek_lb"))
        assertTrue(NativeComponentRegistry.isNativeBuiltin("builtin.peek_rb"))

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
        assertEquals(ButtonStudioType.DEFAULT, resolveButtonSourceType("builtin.arc_lb"))
        assertEquals(ButtonStudioType.DEFAULT, resolveButtonSourceType("builtin.arc_rb"))
        assertEquals(ButtonStudioType.DEFAULT, resolveButtonSourceType("builtin.led_lb"))
        assertEquals(ButtonStudioType.DEFAULT, resolveButtonSourceType("builtin.led_rb"))
        assertEquals(ButtonStudioType.DEFAULT, resolveButtonSourceType("builtin.peek_lb"))
        assertEquals(ButtonStudioType.DEFAULT, resolveButtonSourceType("builtin.peek_rb"))

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

    @Test
    fun testArcBumperVariantsRegistered() {
        val arcLb = DefaultNativeFamily.getVariant("builtin.arc_lb")
        assertNotNull("Arc LB must be registered in DefaultNativeFamily", arcLb)
        assertEquals(ControlKey.LB, arcLb!!.controlKey)
        assertEquals(3101, arcLb.seedCode)
        assertFalse(arcLb.isBaselineDefault)
        assertEquals("Arc Bumper", arcLb.variantName)

        val arcRb = DefaultNativeFamily.getVariant("builtin.arc_rb")
        assertNotNull("Arc RB must be registered in DefaultNativeFamily", arcRb)
        assertEquals(ControlKey.RB, arcRb!!.controlKey)
        assertEquals(3102, arcRb.seedCode)
        assertFalse(arcRb.isBaselineDefault)
        assertEquals("Arc Bumper", arcRb.variantName)

        val lbVariants = DefaultNativeFamily.getVariantsFor(ControlKey.LB)
        assertTrue(lbVariants.any { it.id == "builtin.arc_lb" && it.seedCode == 3101 })

        val rbVariants = DefaultNativeFamily.getVariantsFor(ControlKey.RB)
        assertTrue(rbVariants.any { it.id == "builtin.arc_rb" && it.seedCode == 3102 })
    }

    @Test
    fun testLedBumperVariantsRegistered() {
        val ledLb = DefaultNativeFamily.getVariant("builtin.led_lb")
        assertNotNull("LED LB must be registered in DefaultNativeFamily", ledLb)
        assertEquals(ControlKey.LB, ledLb!!.controlKey)
        assertEquals(3201, ledLb.seedCode)
        assertFalse(ledLb.isBaselineDefault)
        assertEquals("LED Bar Bumper", ledLb.variantName)

        val ledRb = DefaultNativeFamily.getVariant("builtin.led_rb")
        assertNotNull("LED RB must be registered in DefaultNativeFamily", ledRb)
        assertEquals(ControlKey.RB, ledRb!!.controlKey)
        assertEquals(3202, ledRb.seedCode)
        assertFalse(ledRb.isBaselineDefault)
        assertEquals("LED Bar Bumper", ledRb.variantName)

        val lbVariants = DefaultNativeFamily.getVariantsFor(ControlKey.LB)
        assertTrue(lbVariants.any { it.id == "builtin.led_lb" && it.seedCode == 3201 })

        val rbVariants = DefaultNativeFamily.getVariantsFor(ControlKey.RB)
        assertTrue(rbVariants.any { it.id == "builtin.led_rb" && it.seedCode == 3202 })
    }

    @Test
    fun testPeekBumperVariantsRegistered() {
        val peekLb = DefaultNativeFamily.getVariant("builtin.peek_lb")
        assertNotNull("Peek LB must be registered in DefaultNativeFamily", peekLb)
        assertEquals(ControlKey.LB, peekLb!!.controlKey)
        assertEquals(3301, peekLb.seedCode)
        assertFalse(peekLb.isBaselineDefault)
        assertEquals("Peek Bumper", peekLb.variantName)

        val peekRb = DefaultNativeFamily.getVariant("builtin.peek_rb")
        assertNotNull("Peek RB must be registered in DefaultNativeFamily", peekRb)
        assertEquals(ControlKey.RB, peekRb!!.controlKey)
        assertEquals(3302, peekRb.seedCode)
        assertFalse(peekRb.isBaselineDefault)
        assertEquals("Peek Bumper", peekRb.variantName)

        val lbVariants = DefaultNativeFamily.getVariantsFor(ControlKey.LB)
        assertTrue(lbVariants.any { it.id == "builtin.peek_lb" && it.seedCode == 3301 })

        val rbVariants = DefaultNativeFamily.getVariantsFor(ControlKey.RB)
        assertTrue(rbVariants.any { it.id == "builtin.peek_rb" && it.seedCode == 3302 })
    }

    @Test
    fun testRibbedBumperVariantsRegistered() {
        val ribLb = DefaultNativeFamily.getVariant("builtin.rib_lb")
        assertNotNull("Ribbed LB must be registered in DefaultNativeFamily", ribLb)
        assertEquals(ControlKey.LB, ribLb!!.controlKey)
        assertEquals(3401, ribLb.seedCode)
        assertFalse(ribLb.isBaselineDefault)
        assertEquals("Ribbed Bumper", ribLb.variantName)

        val ribRb = DefaultNativeFamily.getVariant("builtin.rib_rb")
        assertNotNull("Ribbed RB must be registered in DefaultNativeFamily", ribRb)
        assertEquals(ControlKey.RB, ribRb!!.controlKey)
        assertEquals(3402, ribRb.seedCode)
        assertFalse(ribRb.isBaselineDefault)
        assertEquals("Ribbed Bumper", ribRb.variantName)

        val lbVariants = DefaultNativeFamily.getVariantsFor(ControlKey.LB)
        assertTrue(lbVariants.any { it.id == "builtin.rib_lb" && it.seedCode == 3401 })

        val rbVariants = DefaultNativeFamily.getVariantsFor(ControlKey.RB)
        assertTrue(rbVariants.any { it.id == "builtin.rib_rb" && it.seedCode == 3402 })

        assertTrue("builtin.rib_lb must be recognized as native builtin", NativeComponentRegistry.isNativeBuiltin("builtin.rib_lb"))
        assertTrue("builtin.rib_rb must be recognized as native builtin", NativeComponentRegistry.isNativeBuiltin("builtin.rib_rb"))
    }

    @Test
    fun testUnderglowBumperVariantsRegistered() {
        val underLb = DefaultNativeFamily.getVariant("builtin.under_lb")
        assertNotNull("Underglow LB must be registered in DefaultNativeFamily", underLb)
        assertEquals(ControlKey.LB, underLb!!.controlKey)
        assertEquals(3501, underLb.seedCode)
        assertFalse(underLb.isBaselineDefault)
        assertEquals("Underglow Bumper", underLb.variantName)

        val underRb = DefaultNativeFamily.getVariant("builtin.under_rb")
        assertNotNull("Underglow RB must be registered in DefaultNativeFamily", underRb)
        assertEquals(ControlKey.RB, underRb!!.controlKey)
        assertEquals(3502, underRb.seedCode)
        assertFalse(underRb.isBaselineDefault)
        assertEquals("Underglow Bumper", underRb.variantName)

        val lbVariants = DefaultNativeFamily.getVariantsFor(ControlKey.LB)
        assertTrue(lbVariants.any { it.id == "builtin.under_lb" && it.seedCode == 3501 })

        val rbVariants = DefaultNativeFamily.getVariantsFor(ControlKey.RB)
        assertTrue(rbVariants.any { it.id == "builtin.under_rb" && it.seedCode == 3502 })

        assertTrue("builtin.under_lb must be recognized as native builtin", NativeComponentRegistry.isNativeBuiltin("builtin.under_lb"))
        assertTrue("builtin.under_rb must be recognized as native builtin", NativeComponentRegistry.isNativeBuiltin("builtin.under_rb"))
    }

    @Test
    fun testTubeBumperVariantsRegistered() {
        val tubeLb = DefaultNativeFamily.getVariant("builtin.tube_lb")
        assertNotNull("Tube LB must be registered in DefaultNativeFamily", tubeLb)
        assertEquals(ControlKey.LB, tubeLb!!.controlKey)
        assertEquals(3601, tubeLb.seedCode)
        assertFalse(tubeLb.isBaselineDefault)
        assertEquals("Tube Bumper", tubeLb.variantName)

        val tubeRb = DefaultNativeFamily.getVariant("builtin.tube_rb")
        assertNotNull("Tube RB must be registered in DefaultNativeFamily", tubeRb)
        assertEquals(ControlKey.RB, tubeRb!!.controlKey)
        assertEquals(3602, tubeRb.seedCode)
        assertFalse(tubeRb.isBaselineDefault)
        assertEquals("Tube Bumper", tubeRb.variantName)

        val lbVariants = DefaultNativeFamily.getVariantsFor(ControlKey.LB)
        assertTrue(lbVariants.any { it.id == "builtin.tube_lb" && it.seedCode == 3601 })

        val rbVariants = DefaultNativeFamily.getVariantsFor(ControlKey.RB)
        assertTrue(rbVariants.any { it.id == "builtin.tube_rb" && it.seedCode == 3602 })

        assertTrue("builtin.tube_lb must be recognized as native builtin", NativeComponentRegistry.isNativeBuiltin("builtin.tube_lb"))
        assertTrue("builtin.tube_rb must be recognized as native builtin", NativeComponentRegistry.isNativeBuiltin("builtin.tube_rb"))
    }

    @Test
    fun testFlipBumperVariantsRegistered() {
        val flipLb = DefaultNativeFamily.getVariant("builtin.flip_lb")
        assertNotNull("Flip LB must be registered in DefaultNativeFamily", flipLb)
        assertEquals(ControlKey.LB, flipLb!!.controlKey)
        assertEquals(3701, flipLb.seedCode)
        assertFalse(flipLb.isBaselineDefault)
        assertEquals("Flip Bumper", flipLb.variantName)

        val flipRb = DefaultNativeFamily.getVariant("builtin.flip_rb")
        assertNotNull("Flip RB must be registered in DefaultNativeFamily", flipRb)
        assertEquals(ControlKey.RB, flipRb!!.controlKey)
        assertEquals(3702, flipRb.seedCode)
        assertFalse(flipRb.isBaselineDefault)
        assertEquals("Flip Bumper", flipRb.variantName)

        val lbVariants = DefaultNativeFamily.getVariantsFor(ControlKey.LB)
        assertTrue(lbVariants.any { it.id == "builtin.flip_lb" && it.seedCode == 3701 })

        val rbVariants = DefaultNativeFamily.getVariantsFor(ControlKey.RB)
        assertTrue(rbVariants.any { it.id == "builtin.flip_rb" && it.seedCode == 3702 })

        assertTrue("builtin.flip_lb must be recognized as native builtin", NativeComponentRegistry.isNativeBuiltin("builtin.flip_lb"))
        assertTrue("builtin.flip_rb must be recognized as native builtin", NativeComponentRegistry.isNativeBuiltin("builtin.flip_rb"))
    }

    @Test
    fun testDialTriggerVariantsRegistered() {
        val dialLt = DefaultNativeFamily.getVariant("builtin.dial_lt")
        assertNotNull("Dial LT must be registered in DefaultNativeFamily", dialLt)
        assertEquals(ControlKey.LT, dialLt!!.controlKey)
        assertEquals(2101, dialLt.seedCode)
        assertFalse(dialLt.isBaselineDefault)
        assertEquals("Dial Gauge", dialLt.variantName)

        val dialRt = DefaultNativeFamily.getVariant("builtin.dial_rt")
        assertNotNull("Dial RT must be registered in DefaultNativeFamily", dialRt)
        assertEquals(ControlKey.RT, dialRt!!.controlKey)
        assertEquals(2102, dialRt.seedCode)
        assertFalse(dialRt.isBaselineDefault)
        assertEquals("Dial Gauge", dialRt.variantName)

        val ltVariants = DefaultNativeFamily.getVariantsFor(ControlKey.LT)
        assertTrue(ltVariants.any { it.id == "builtin.dial_lt" && it.seedCode == 2101 })

        val rtVariants = DefaultNativeFamily.getVariantsFor(ControlKey.RT)
        assertTrue(rtVariants.any { it.id == "builtin.dial_rt" && it.seedCode == 2102 })

        assertTrue("builtin.dial_lt must be recognized as native builtin", NativeComponentRegistry.isNativeBuiltin("builtin.dial_lt"))
        assertTrue("builtin.dial_rt must be recognized as native builtin", NativeComponentRegistry.isNativeBuiltin("builtin.dial_rt"))
    }

    @Test
    fun testLiquidOrbTriggerVariantsRegistered() {
        val liquidLt = DefaultNativeFamily.getVariant("builtin.liquid_lt")
        assertNotNull("Liquid Orb LT must be registered in DefaultNativeFamily", liquidLt)
        assertEquals(ControlKey.LT, liquidLt!!.controlKey)
        assertEquals(2201, liquidLt.seedCode)
        assertFalse(liquidLt.isBaselineDefault)
        assertEquals("Liquid Orb", liquidLt.variantName)

        val liquidRt = DefaultNativeFamily.getVariant("builtin.liquid_rt")
        assertNotNull("Liquid Orb RT must be registered in DefaultNativeFamily", liquidRt)
        assertEquals(ControlKey.RT, liquidRt!!.controlKey)
        assertEquals(2202, liquidRt.seedCode)
        assertFalse(liquidRt.isBaselineDefault)
        assertEquals("Liquid Orb", liquidRt.variantName)

        val ltVariants = DefaultNativeFamily.getVariantsFor(ControlKey.LT)
        assertTrue(ltVariants.any { it.id == "builtin.liquid_lt" && it.seedCode == 2201 })

        val rtVariants = DefaultNativeFamily.getVariantsFor(ControlKey.RT)
        assertTrue(rtVariants.any { it.id == "builtin.liquid_rt" && it.seedCode == 2202 })

        assertTrue("builtin.liquid_lt must be recognized as native builtin", NativeComponentRegistry.isNativeBuiltin("builtin.liquid_lt"))
        assertTrue("builtin.liquid_rt must be recognized as native builtin", NativeComponentRegistry.isNativeBuiltin("builtin.liquid_rt"))
    }

    @Test
    fun testVuSlabsTriggerVariantsRegistered() {
        val vuLt = DefaultNativeFamily.getVariant("builtin.vu_lt")
        assertNotNull("VU Slabs LT must be registered in DefaultNativeFamily", vuLt)
        assertEquals(ControlKey.LT, vuLt!!.controlKey)
        assertEquals(2301, vuLt.seedCode)
        assertFalse(vuLt.isBaselineDefault)
        assertEquals("VU Slabs", vuLt.variantName)

        val vuRt = DefaultNativeFamily.getVariant("builtin.vu_rt")
        assertNotNull("VU Slabs RT must be registered in DefaultNativeFamily", vuRt)
        assertEquals(ControlKey.RT, vuRt!!.controlKey)
        assertEquals(2302, vuRt.seedCode)
        assertFalse(vuRt.isBaselineDefault)
        assertEquals("VU Slabs", vuRt.variantName)

        val ltVariants = DefaultNativeFamily.getVariantsFor(ControlKey.LT)
        assertTrue(ltVariants.any { it.id == "builtin.vu_lt" && it.seedCode == 2301 })

        val rtVariants = DefaultNativeFamily.getVariantsFor(ControlKey.RT)
        assertTrue(rtVariants.any { it.id == "builtin.vu_rt" && it.seedCode == 2302 })

        assertTrue("builtin.vu_lt must be recognized as native builtin", NativeComponentRegistry.isNativeBuiltin("builtin.vu_lt"))
        assertTrue("builtin.vu_rt must be recognized as native builtin", NativeComponentRegistry.isNativeBuiltin("builtin.vu_rt"))
    }

    @Test
    fun testTargetTriggerVariantsRegistered() {
        val targetLt = DefaultNativeFamily.getVariant("builtin.target_lt")
        assertNotNull("Target Crosshair LT must be registered in DefaultNativeFamily", targetLt)
        assertEquals(ControlKey.LT, targetLt!!.controlKey)
        assertEquals(2401, targetLt.seedCode)
        assertFalse(targetLt.isBaselineDefault)
        assertEquals("Target Crosshair", targetLt.variantName)

        val targetRt = DefaultNativeFamily.getVariant("builtin.target_rt")
        assertNotNull("Target Crosshair RT must be registered in DefaultNativeFamily", targetRt)
        assertEquals(ControlKey.RT, targetRt!!.controlKey)
        assertEquals(2402, targetRt.seedCode)
        assertFalse(targetRt.isBaselineDefault)
        assertEquals("Target Crosshair", targetRt.variantName)

        val ltVariants = DefaultNativeFamily.getVariantsFor(ControlKey.LT)
        assertTrue(ltVariants.any { it.id == "builtin.target_lt" && it.seedCode == 2401 })

        val rtVariants = DefaultNativeFamily.getVariantsFor(ControlKey.RT)
        assertTrue(rtVariants.any { it.id == "builtin.target_rt" && it.seedCode == 2402 })

        assertTrue("builtin.target_lt must be recognized as native builtin", NativeComponentRegistry.isNativeBuiltin("builtin.target_lt"))
        assertTrue("builtin.target_rt must be recognized as native builtin", NativeComponentRegistry.isNativeBuiltin("builtin.target_rt"))
    }

    @Test
    fun testSliderTriggerVariantsRegistered() {
        val sliderLt = DefaultNativeFamily.getVariant("builtin.slider_lt")
        assertNotNull("Analog Slider LT must be registered in DefaultNativeFamily", sliderLt)
        assertEquals(ControlKey.LT, sliderLt!!.controlKey)
        assertEquals(2501, sliderLt.seedCode)
        assertFalse(sliderLt.isBaselineDefault)
        assertEquals("Analog Slider", sliderLt.variantName)

        val sliderRt = DefaultNativeFamily.getVariant("builtin.slider_rt")
        assertNotNull("Analog Slider RT must be registered in DefaultNativeFamily", sliderRt)
        assertEquals(ControlKey.RT, sliderRt!!.controlKey)
        assertEquals(2502, sliderRt.seedCode)
        assertFalse(sliderRt.isBaselineDefault)
        assertEquals("Analog Slider", sliderRt.variantName)

        val ltVariants = DefaultNativeFamily.getVariantsFor(ControlKey.LT)
        assertTrue(ltVariants.any { it.id == "builtin.slider_lt" && it.seedCode == 2501 })

        val rtVariants = DefaultNativeFamily.getVariantsFor(ControlKey.RT)
        assertTrue(rtVariants.any { it.id == "builtin.slider_rt" && it.seedCode == 2502 })

        assertTrue("builtin.slider_lt must be recognized as native builtin", NativeComponentRegistry.isNativeBuiltin("builtin.slider_lt"))
        assertTrue("builtin.slider_rt must be recognized as native builtin", NativeComponentRegistry.isNativeBuiltin("builtin.slider_rt"))
    }

    @Test
    fun testNeedleTriggerVariantsRegistered() {
        val needleLt = DefaultNativeFamily.getVariant("builtin.needle_lt")
        assertNotNull("Needle Meter LT must be registered in DefaultNativeFamily", needleLt)
        assertEquals(ControlKey.LT, needleLt!!.controlKey)
        assertEquals(2601, needleLt.seedCode)
        assertFalse(needleLt.isBaselineDefault)
        assertEquals("Needle Meter", needleLt.variantName)

        val needleRt = DefaultNativeFamily.getVariant("builtin.needle_rt")
        assertNotNull("Needle Meter RT must be registered in DefaultNativeFamily", needleRt)
        assertEquals(ControlKey.RT, needleRt!!.controlKey)
        assertEquals(2602, needleRt.seedCode)
        assertFalse(needleRt.isBaselineDefault)
        assertEquals("Needle Meter", needleRt.variantName)

        val ltVariants = DefaultNativeFamily.getVariantsFor(ControlKey.LT)
        assertTrue(ltVariants.any { it.id == "builtin.needle_lt" && it.seedCode == 2601 })

        val rtVariants = DefaultNativeFamily.getVariantsFor(ControlKey.RT)
        assertTrue(rtVariants.any { it.id == "builtin.needle_rt" && it.seedCode == 2602 })

        assertTrue("builtin.needle_lt must be recognized as native builtin", NativeComponentRegistry.isNativeBuiltin("builtin.needle_lt"))
        assertTrue("builtin.needle_rt must be recognized as native builtin", NativeComponentRegistry.isNativeBuiltin("builtin.needle_rt"))
    }

    @Test
    fun testTestTubeTriggerVariantsRegistered() {
        val tubeLt = DefaultNativeFamily.getVariant("builtin.testtube_lt")
        assertNotNull("Test Tube LT must be registered in DefaultNativeFamily", tubeLt)
        assertEquals(ControlKey.LT, tubeLt!!.controlKey)
        assertEquals(2701, tubeLt.seedCode)
        assertFalse(tubeLt.isBaselineDefault)
        assertEquals("Test Tube", tubeLt.variantName)

        val tubeRt = DefaultNativeFamily.getVariant("builtin.testtube_rt")
        assertNotNull("Test Tube RT must be registered in DefaultNativeFamily", tubeRt)
        assertEquals(ControlKey.RT, tubeRt!!.controlKey)
        assertEquals(2702, tubeRt.seedCode)
        assertFalse(tubeRt.isBaselineDefault)
        assertEquals("Test Tube", tubeRt.variantName)

        val ltVariants = DefaultNativeFamily.getVariantsFor(ControlKey.LT)
        assertTrue(ltVariants.any { it.id == "builtin.testtube_lt" && it.seedCode == 2701 })

        val rtVariants = DefaultNativeFamily.getVariantsFor(ControlKey.RT)
        assertTrue(rtVariants.any { it.id == "builtin.testtube_rt" && it.seedCode == 2702 })

        assertTrue("builtin.testtube_lt must be recognized as native builtin", NativeComponentRegistry.isNativeBuiltin("builtin.testtube_lt"))
        assertTrue("builtin.testtube_rt must be recognized as native builtin", NativeComponentRegistry.isNativeBuiltin("builtin.testtube_rt"))
    }

    @Test
    fun testBloomTriggerVariantsRegistered() {
        val bloomLt = DefaultNativeFamily.getVariant("builtin.bloom_lt")
        assertNotNull("Bloom Light LT must be registered in DefaultNativeFamily", bloomLt)
        assertEquals(ControlKey.LT, bloomLt!!.controlKey)
        assertEquals(2801, bloomLt.seedCode)
        assertFalse(bloomLt.isBaselineDefault)
        assertEquals("Bloom Light", bloomLt.variantName)

        val bloomRt = DefaultNativeFamily.getVariant("builtin.bloom_rt")
        assertNotNull("Bloom Light RT must be registered in DefaultNativeFamily", bloomRt)
        assertEquals(ControlKey.RT, bloomRt!!.controlKey)
        assertEquals(2802, bloomRt.seedCode)
        assertFalse(bloomRt.isBaselineDefault)
        assertEquals("Bloom Light", bloomRt.variantName)

        val ltVariants = DefaultNativeFamily.getVariantsFor(ControlKey.LT)
        assertTrue(ltVariants.any { it.id == "builtin.bloom_lt" && it.seedCode == 2801 })

        val rtVariants = DefaultNativeFamily.getVariantsFor(ControlKey.RT)
        assertTrue(rtVariants.any { it.id == "builtin.bloom_rt" && it.seedCode == 2802 })

        assertTrue("builtin.bloom_lt must be recognized as native builtin", NativeComponentRegistry.isNativeBuiltin("builtin.bloom_lt"))
        assertTrue("builtin.bloom_rt must be recognized as native builtin", NativeComponentRegistry.isNativeBuiltin("builtin.bloom_rt"))
    }

    @Test
    fun testLiquidButtonVariantsRegistered() {
        val liqA = DefaultNativeFamily.getVariant("builtin.liq_a")
        assertNotNull("Liquid Fill A must be registered", liqA)
        assertEquals(ControlKey.A, liqA!!.controlKey)
        assertEquals(1101, liqA.seedCode)
        assertFalse(liqA.isBaselineDefault)
        assertEquals("Liquid Fill", liqA.variantName)
        assertTrue("builtin.liq_a must be recognized as native builtin", NativeComponentRegistry.isNativeBuiltin("builtin.liq_a"))

        val liqY = DefaultNativeFamily.getVariant("builtin.liq_y")
        assertNotNull("Liquid Fill Y must be registered", liqY)
        assertEquals(ControlKey.Y, liqY!!.controlKey)
        assertEquals(1104, liqY.seedCode)
        assertTrue(NativeComponentRegistry.isNativeBuiltin("builtin.liq_y"))
    }

    @Test
    fun testFacetButtonVariantsRegistered() {
        val facetA = DefaultNativeFamily.getVariant("builtin.facet_a")
        assertNotNull("Facet Gem A must be registered", facetA)
        assertEquals(ControlKey.A, facetA!!.controlKey)
        assertEquals(1201, facetA.seedCode)
        assertFalse(facetA.isBaselineDefault)
        assertEquals("Facet Gem", facetA.variantName)
        assertTrue("builtin.facet_a must be recognized as native builtin", NativeComponentRegistry.isNativeBuiltin("builtin.facet_a"))

        val facetY = DefaultNativeFamily.getVariant("builtin.facet_y")
        assertNotNull("Facet Gem Y must be registered", facetY)
        assertEquals(ControlKey.Y, facetY!!.controlKey)
        assertEquals(1204, facetY.seedCode)
        assertTrue(NativeComponentRegistry.isNativeBuiltin("builtin.facet_y"))
    }

    @Test
    fun testFlipButtonVariantsRegistered() {
        val flipA = DefaultNativeFamily.getVariant("builtin.flipbtn_a")
        assertNotNull("Flip Card A must be registered", flipA)
        assertEquals(ControlKey.A, flipA!!.controlKey)
        assertEquals(1301, flipA.seedCode)
        assertFalse(flipA.isBaselineDefault)
        assertEquals("Flip Card", flipA.variantName)
        assertTrue("builtin.flipbtn_a must be recognized as native builtin", NativeComponentRegistry.isNativeBuiltin("builtin.flipbtn_a"))

        val flipY = DefaultNativeFamily.getVariant("builtin.flipbtn_y")
        assertNotNull("Flip Card Y must be registered", flipY)
        assertEquals(ControlKey.Y, flipY!!.controlKey)
        assertEquals(1304, flipY.seedCode)
        assertTrue(NativeComponentRegistry.isNativeBuiltin("builtin.flipbtn_y"))
    }

    @Test
    fun testRippleButtonVariantsRegistered() {
        val rippleA = DefaultNativeFamily.getVariant("builtin.ripple_a")
        assertNotNull("Ripple Rings A must be registered", rippleA)
        assertEquals(ControlKey.A, rippleA!!.controlKey)
        assertEquals(1401, rippleA.seedCode)
        assertFalse(rippleA.isBaselineDefault)
        assertEquals("Ripple Rings", rippleA.variantName)
        assertTrue("builtin.ripple_a must be recognized as native builtin", NativeComponentRegistry.isNativeBuiltin("builtin.ripple_a"))

        val rippleY = DefaultNativeFamily.getVariant("builtin.ripple_y")
        assertNotNull("Ripple Rings Y must be registered", rippleY)
        assertEquals(ControlKey.Y, rippleY!!.controlKey)
        assertEquals(1404, rippleY.seedCode)
        assertTrue(NativeComponentRegistry.isNativeBuiltin("builtin.ripple_y"))
    }

    @Test
    fun testOrbitButtonVariantsRegistered() {
        val orbitA = DefaultNativeFamily.getVariant("builtin.orbit_a")
        assertNotNull("Orbit Rings A must be registered", orbitA)
        assertEquals(ControlKey.A, orbitA!!.controlKey)
        assertEquals(1601, orbitA.seedCode)
        assertFalse(orbitA.isBaselineDefault)
        assertEquals("Orbit Rings", orbitA.variantName)
        assertTrue("builtin.orbit_a must be recognized as native builtin", NativeComponentRegistry.isNativeBuiltin("builtin.orbit_a"))

        val orbitY = DefaultNativeFamily.getVariant("builtin.orbit_y")
        assertNotNull("Orbit Rings Y must be registered", orbitY)
        assertEquals(ControlKey.Y, orbitY!!.controlKey)
        assertEquals(1604, orbitY.seedCode)
        assertTrue(NativeComponentRegistry.isNativeBuiltin("builtin.orbit_y"))
    }

    @Test
    fun testCapsulesButtonVariantsRegistered() {
        val capsA = DefaultNativeFamily.getVariant("builtin.caps_a")
        assertNotNull("Capsule Fill A must be registered", capsA)
        assertEquals(ControlKey.A, capsA!!.controlKey)
        assertEquals(1701, capsA.seedCode)
        assertFalse(capsA.isBaselineDefault)
        assertEquals("Capsule Fill", capsA.variantName)
        assertTrue("builtin.caps_a must be recognized as native builtin", NativeComponentRegistry.isNativeBuiltin("builtin.caps_a"))

        val capsY = DefaultNativeFamily.getVariant("builtin.caps_y")
        assertNotNull("Capsule Fill Y must be registered", capsY)
        assertEquals(ControlKey.Y, capsY!!.controlKey)
        assertEquals(1704, capsY.seedCode)
        assertTrue(NativeComponentRegistry.isNativeBuiltin("builtin.caps_y"))
    }

    @Test
    fun testEclipseButtonVariantsRegistered() {
        val eclA = DefaultNativeFamily.getVariant("builtin.ecl_a")
        assertNotNull("Eclipse Disc A must be registered", eclA)
        assertEquals(ControlKey.A, eclA!!.controlKey)
        assertEquals(1801, eclA.seedCode)
        assertFalse(eclA.isBaselineDefault)
        assertEquals("Eclipse Disc", eclA.variantName)
        assertTrue("builtin.ecl_a must be recognized as native builtin", NativeComponentRegistry.isNativeBuiltin("builtin.ecl_a"))

        val eclY = DefaultNativeFamily.getVariant("builtin.ecl_y")
        assertNotNull("Eclipse Disc Y must be registered", eclY)
        assertEquals(ControlKey.Y, eclY!!.controlKey)
        assertEquals(1804, eclY.seedCode)
        assertTrue(NativeComponentRegistry.isNativeBuiltin("builtin.ecl_y"))
    }

    @Test
    fun testInbuildTouchPadVariantsRegistered() {
        val ltpVariant = DefaultNativeFamily.getVariant("builtin.inbuild_ltp")
        assertNotNull("Inbuild Surface LTP must be registered", ltpVariant)
        assertEquals(ControlKey.LTP, ltpVariant!!.controlKey)
        assertEquals(6003, ltpVariant.seedCode)
        assertFalse(ltpVariant.isBaselineDefault)
        assertEquals("Inbuild Surface", ltpVariant.variantName)
        assertTrue("builtin.inbuild_ltp must be recognized as native builtin", NativeComponentRegistry.isNativeBuiltin("builtin.inbuild_ltp"))

        val rtpVariant = DefaultNativeFamily.getVariant("builtin.inbuild_rtp")
        assertNotNull("Inbuild Surface RTP must be registered", rtpVariant)
        assertEquals(ControlKey.RTP, rtpVariant!!.controlKey)
        assertEquals(6004, rtpVariant.seedCode)
        assertFalse(rtpVariant.isBaselineDefault)
        assertEquals("Inbuild Surface", rtpVariant.variantName)
        assertTrue("builtin.inbuild_rtp must be recognized as native builtin", NativeComponentRegistry.isNativeBuiltin("builtin.inbuild_rtp"))
    }
}


