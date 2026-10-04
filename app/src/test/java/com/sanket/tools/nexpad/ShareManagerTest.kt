package com.sanket.tools.nexpad

import com.sanket.tools.nexpad.model.LayoutProfile
import com.sanket.tools.nexpad.model.Position
import com.sanket.tools.nexpad.share.ShareManager
import org.junit.Assert.*
import org.junit.Test

class ShareManagerTest {

    @Test
    fun testEncodeLayoutHasMagicBytesAndDecodesCorrectly() {
        val testProfile = LayoutProfile(
            name = "Test Claw Layout",
            isDefault = true,
            labelStyle = "XBOX",
            positions = mapOf(
                "A" to Position(0.7f, 0.7f, scale = 1.0f, customComponentId = "rc.custom_anime_a"),
                "B" to Position(0.8f, 0.6f, scale = 0.9f, customComponentId = "rc.custom_rgb_b")
            ),
            description = "Competitive test layout"
        )

        // 1. Encode
        val encodedBytes = ShareManager.encodeLayout(testProfile)

        // Verify "NXLY" magic header (0x4E, 0x58, 0x4C, 0x59)
        assertTrue("Encoded bytes must have at least magic header length", encodedBytes.size >= 4)
        assertArrayEquals(ShareManager.MAGIC_NXLAYOUT, encodedBytes.copyOfRange(0, 4))

        // 2. Decode
        val decodeResult = ShareManager.decodeLayout(encodedBytes)
        assertTrue("Decoding valid .nxlayout must succeed", decodeResult.isSuccess)

        val decodedProfile = decodeResult.getOrThrow()
        assertEquals("Test Claw Layout", decodedProfile.name)
        assertFalse("Imported layouts must always be marked custom (isDefault = false)", decodedProfile.isDefault)
        assertEquals("XBOX", decodedProfile.labelStyle)

        // 3. Verify positions: customComponentId MUST BE STRIPPED (null) per spec
        assertEquals(2, decodedProfile.positions.size)
        val posA = decodedProfile.positions["A"]
        assertNotNull(posA)
        assertEquals(0.7f, posA!!.xRatio, 0.001f)
        assertEquals(0.7f, posA.yRatio, 0.001f)
        assertNull("customComponentId must be stripped so receiving phone auto-detects default button", posA.customComponentId)

        val posB = decodedProfile.positions["B"]
        assertNotNull(posB)
        assertEquals(0.8f, posB!!.xRatio, 0.001f)
        assertNull("customComponentId must be stripped so receiving phone auto-detects default button", posB.customComponentId)
    }

    @Test
    fun testFallbackDecodingPlainJson() {
        // Plain JSON string without NXLY header
        val plainJson = """
            {
                "version": 1,
                "name": "Legacy JSON Layout",
                "labelStyle": "PLAYSTATION",
                "isRgbEnabled": false,
                "description": "Legacy backup",
                "positions": {
                    "X": {
                        "xRatio": 0.5,
                        "yRatio": 0.5,
                        "scale": 1.2,
                        "customComponentId": "rc.some_skin"
                    }
                }
            }
        """.trimIndent().toByteArray(Charsets.UTF_8)

        val result = ShareManager.decodeLayout(plainJson)
        assertTrue(result.isSuccess)
        val decoded = result.getOrThrow()
        assertEquals("Legacy JSON Layout", decoded.name)
        assertEquals("PLAYSTATION", decoded.labelStyle)
        assertFalse(decoded.isRgbEnabled)
        assertNull(decoded.positions["X"]?.customComponentId)
    }

    @Test
    fun testMagicHeaderDistinction() {
        val layoutMagic = ShareManager.MAGIC_NXLAYOUT
        val nxprcMagic = com.sanket.tools.nexpad.runtime.plugin.NxprcDocument.MAGIC

        assertEquals("NXLY", String(layoutMagic, Charsets.UTF_8))
        assertEquals("NXRC", String(nxprcMagic, Charsets.UTF_8))
        assertFalse("Layout and Nxprc magic headers must be distinctly different", layoutMagic.contentEquals(nxprcMagic))
    }
}
