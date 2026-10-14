package com.sanket.tools.nexpad

import androidx.compose.ui.graphics.Color
import com.sanket.tools.nexpad.category.ControlKey
import com.sanket.tools.nexpad.category.ControllerLabelStyle
import com.sanket.tools.nexpad.ui.components.controller.abxy.PlayStationColors
import com.sanket.tools.nexpad.ui.components.controller.abxy.PlayStationShape
import com.sanket.tools.nexpad.ui.components.controller.abxy.XboxColors
import com.sanket.tools.nexpad.ui.components.controller.abxy.getPlayStationColor
import com.sanket.tools.nexpad.ui.components.controller.abxy.getPlayStationShape
import com.sanket.tools.nexpad.ui.components.controller.abxy.isPlayStationSymbol
import com.sanket.tools.nexpad.ui.components.controller.abxy.resolveButtonColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import com.sanket.tools.nexpad.runtime.engine.adaptColorForConsole
import com.sanket.tools.nexpad.runtime.engine.isAchromatic
import com.sanket.tools.nexpad.runtime.engine.isDefaultConsoleAccent
import com.sanket.tools.nexpad.runtime.engine.toHsv

/**
 * Unit tests validating authentic DualShock 4 and Xbox face button color mappings:
 * Position | Xbox label | Xbox color | DS4 symbol | Classic DS4 symbol color
 * Top      | Y          | Yellow     | △ Triangle | Green
 * Right    | B          | Red        | ○ Circle   | Red
 * Left     | X          | Blue       | □ Square   | Pink
 * Bottom   | A          | Green      | × Cross    | Blue
 */
class PlayStationColorsTest {

    @Test
    fun testClassicPaletteConstants() {
        // DS4 classic palette
        assertEquals(Color(0xFF3F8FE0), PlayStationColors.Cross)    // Bottom / × (Blue)
        assertEquals(Color(0xFFE6474E), PlayStationColors.Circle)   // Right / ○ (Red)
        assertEquals(Color(0xFFE055B8), PlayStationColors.Square)   // Left / □ (Pink)
        assertEquals(Color(0xFF3FD25A), PlayStationColors.Triangle) // Top / △ (Green)

        // Xbox classic palette
        assertEquals(Color(0xFF3FD25A), XboxColors.A)               // Bottom / A (Green)
        assertEquals(Color(0xFFE6474E), XboxColors.B)               // Right / B (Red)
        assertEquals(Color(0xFF3F8FE0), XboxColors.X)               // Left / X (Blue)
        assertEquals(Color(0xFFE0A03F), XboxColors.Y)               // Top / Y (Yellow)
    }

    @Test
    fun testPlayStationShapeColors() {
        assertEquals(PlayStationColors.Cross, getPlayStationColor(PlayStationShape.CROSS))
        assertEquals(PlayStationColors.Circle, getPlayStationColor(PlayStationShape.CIRCLE))
        assertEquals(PlayStationColors.Square, getPlayStationColor(PlayStationShape.SQUARE))
        assertEquals(PlayStationColors.Triangle, getPlayStationColor(PlayStationShape.TRIANGLE))
    }

    @Test
    fun testResolveButtonColorInPlayStationStyle() {
        // Bottom / A -> Cross (Blue)
        assertEquals(
            PlayStationColors.Cross,
            resolveButtonColor("A", ControllerLabelStyle.PLAYSTATION)
        )
        // Right / B -> Circle (Red)
        assertEquals(
            PlayStationColors.Circle,
            resolveButtonColor("B", ControllerLabelStyle.PLAYSTATION)
        )
        // Left / X -> Square (Pink)
        assertEquals(
            PlayStationColors.Square,
            resolveButtonColor("X", ControllerLabelStyle.PLAYSTATION)
        )
        // Top / Y -> Triangle (Green)
        assertEquals(
            PlayStationColors.Triangle,
            resolveButtonColor("Y", ControllerLabelStyle.PLAYSTATION)
        )
    }

    @Test
    fun testResolveButtonColorInXboxStyle() {
        // Bottom / A -> Green
        assertEquals(
            XboxColors.A,
            resolveButtonColor("A", ControllerLabelStyle.XBOX)
        )
        // Right / B -> Red
        assertEquals(
            XboxColors.B,
            resolveButtonColor("B", ControllerLabelStyle.XBOX)
        )
        // Left / X -> Blue
        assertEquals(
            XboxColors.X,
            resolveButtonColor("X", ControllerLabelStyle.XBOX)
        )
        // Top / Y -> Yellow
        assertEquals(
            XboxColors.Y,
            resolveButtonColor("Y", ControllerLabelStyle.XBOX)
        )
    }

    @Test
    fun testResolveButtonColorFromExplicitSymbols() {
        // When displayLabel contains the geometric glyph, it should always resolve to the authentic DS4 color
        assertEquals(PlayStationColors.Cross, resolveButtonColor("A", ControllerLabelStyle.XBOX, "✕"))
        assertEquals(PlayStationColors.Cross, resolveButtonColor("A", ControllerLabelStyle.XBOX, "×"))
        assertEquals(PlayStationColors.Circle, resolveButtonColor("B", ControllerLabelStyle.XBOX, "○"))
        assertEquals(PlayStationColors.Square, resolveButtonColor("X", ControllerLabelStyle.XBOX, "□"))
        assertEquals(PlayStationColors.Triangle, resolveButtonColor("Y", ControllerLabelStyle.XBOX, "△"))
    }

    @Test
    fun testPlayStationShapeDetection() {
        assertEquals(PlayStationShape.CROSS, getPlayStationShape("✕"))
        assertEquals(PlayStationShape.CROSS, getPlayStationShape("×"))
        assertEquals(PlayStationShape.CROSS, getPlayStationShape("CROSS"))

        assertEquals(PlayStationShape.CIRCLE, getPlayStationShape("○"))
        assertEquals(PlayStationShape.CIRCLE, getPlayStationShape("◯"))
        assertEquals(PlayStationShape.CIRCLE, getPlayStationShape("CIRCLE"))

        assertEquals(PlayStationShape.SQUARE, getPlayStationShape("□"))
        assertEquals(PlayStationShape.SQUARE, getPlayStationShape("◻"))
        assertEquals(PlayStationShape.SQUARE, getPlayStationShape("SQUARE"))

        assertEquals(PlayStationShape.TRIANGLE, getPlayStationShape("△"))
        assertEquals(PlayStationShape.TRIANGLE, getPlayStationShape("▲"))
        assertEquals(PlayStationShape.TRIANGLE, getPlayStationShape("TRIANGLE"))

        assertTrue(isPlayStationSymbol("✕"))
        assertTrue(isPlayStationSymbol("○"))
        assertTrue(isPlayStationSymbol("□"))
        assertTrue(isPlayStationSymbol("△"))
    }

    @Test
    fun testControlKeyOverload() {
        assertEquals(PlayStationColors.Cross, resolveButtonColor(ControlKey.A, ControllerLabelStyle.PLAYSTATION))
        assertEquals(PlayStationColors.Circle, resolveButtonColor(ControlKey.B, ControllerLabelStyle.PLAYSTATION))
        assertEquals(PlayStationColors.Square, resolveButtonColor(ControlKey.X, ControllerLabelStyle.PLAYSTATION))
        assertEquals(PlayStationColors.Triangle, resolveButtonColor(ControlKey.Y, ControllerLabelStyle.PLAYSTATION))

        assertEquals(XboxColors.A, resolveButtonColor(ControlKey.A, ControllerLabelStyle.XBOX))
        assertEquals(XboxColors.B, resolveButtonColor(ControlKey.B, ControllerLabelStyle.XBOX))
        assertEquals(XboxColors.X, resolveButtonColor(ControlKey.X, ControllerLabelStyle.XBOX))
        assertEquals(XboxColors.Y, resolveButtonColor(ControlKey.Y, ControllerLabelStyle.XBOX))
    }

    @Test
    fun testDefaultAccentAdaptationForPlayStation() {
        // Button A: Default Emerald Green (#00FFA3 / hue ~158°) shifts +60° -> Blue (hue ~218°)
        val greenA = Color(0xFF00FFA3)
        val adaptedA = adaptColorForConsole(greenA, "A", ControllerLabelStyle.PLAYSTATION)
        val hueA = adaptedA.toHsv()[0]
        assertTrue("Adapted Button A hue should be PlayStation Blue (~210°-225°), was: $hueA", hueA in 210f..225f)

        // Button X: Default Blue (#3F8FE0 / hue ~210°) shifts +122° -> Pink (hue ~332°)
        val blueX = XboxColors.X
        val adaptedX = adaptColorForConsole(blueX, "X", ControllerLabelStyle.PLAYSTATION)
        val hueX = adaptedX.toHsv()[0]
        assertTrue("Adapted Button X hue should be PlayStation Pink (~320°-335°), was: $hueX", hueX in 320f..335f)

        // Button Y: Default Yellow (#E0A03F / hue ~36°) shifts +85° -> Green (hue ~121°)
        val yellowY = XboxColors.Y
        val adaptedY = adaptColorForConsole(yellowY, "Y", ControllerLabelStyle.PLAYSTATION)
        val hueY = adaptedY.toHsv()[0]
        assertTrue("Adapted Button Y hue should be PlayStation Green (~120°-145°), was: $hueY", hueY in 120f..145f)

        // Button B: Authentic Red is preserved identically across platforms
        val redB = XboxColors.B
        val adaptedB = adaptColorForConsole(redB, "B", ControllerLabelStyle.PLAYSTATION)
        assertEquals("Button B authentic red should be preserved", redB, adaptedB)

        // Under Xbox labelStyle, no adaptation should occur
        assertEquals(greenA, adaptColorForConsole(greenA, "A", ControllerLabelStyle.XBOX))
        assertEquals(blueX, adaptColorForConsole(blueX, "X", ControllerLabelStyle.XBOX))
        assertEquals(yellowY, adaptColorForConsole(yellowY, "Y", ControllerLabelStyle.XBOX))
    }

    @Test
    fun testCustomColorPreservation() {
        // Custom user-selected colors must NEVER be altered regardless of platform style
        val customPink = Color(0xFFFF1493)   // Deep Pink
        val customGold = Color(0xFFFFD700)   // Gold
        val customPurple = Color(0xFF8A2BE2) // BlueViolet
        val customCyan = Color(0xFF00FFFF)   // Cyan

        assertEquals(customPink, adaptColorForConsole(customPink, "A", ControllerLabelStyle.PLAYSTATION))
        assertEquals(customGold, adaptColorForConsole(customGold, "X", ControllerLabelStyle.PLAYSTATION))
        assertEquals(customPurple, adaptColorForConsole(customPurple, "Y", ControllerLabelStyle.PLAYSTATION))
        assertEquals(customCyan, adaptColorForConsole(customCyan, "B", ControllerLabelStyle.PLAYSTATION))
    }

    @Test
    fun testAchromaticNeutralProtection() {
        // Achromatic chassis and socket colors must NEVER be shifted
        val pureWhite = Color(0xFFFFFFFF)
        val silver = Color(0xFFC0C0C0)
        val darkChassis = Color(0xFF181D26)
        val socketDark = Color(0xFF05070A)
        val pureBlack = Color(0xFF000000)

        assertEquals(pureWhite, adaptColorForConsole(pureWhite, "A", ControllerLabelStyle.PLAYSTATION))
        assertEquals(silver, adaptColorForConsole(silver, "A", ControllerLabelStyle.PLAYSTATION))
        assertEquals(darkChassis, adaptColorForConsole(darkChassis, "A", ControllerLabelStyle.PLAYSTATION))
        assertEquals(socketDark, adaptColorForConsole(socketDark, "X", ControllerLabelStyle.PLAYSTATION))
        assertEquals(pureBlack, adaptColorForConsole(pureBlack, "Y", ControllerLabelStyle.PLAYSTATION))
    }

    @Test
    fun testNonAbxyCategoryIsolation() {
        // Non-ABXY controls must be 100% untouched
        val greenColor = Color(0xFF00FFA3)
        val blueColor = Color(0xFF3F8FE0)

        val nonAbxyKeys = listOf("LT", "RT", "LB", "RB", "DPAD_UP", "DPAD_DOWN", "LS", "RS", "START", "SELECT", "HOME", "M1")
        for (key in nonAbxyKeys) {
            assertEquals("Key $key must remain untouched", greenColor, adaptColorForConsole(greenColor, key, ControllerLabelStyle.PLAYSTATION))
            assertEquals("Key $key must remain untouched", blueColor, adaptColorForConsole(blueColor, key, ControllerLabelStyle.PLAYSTATION))
        }
    }

    @Test
    fun testAchromaticDetection() {
        assertTrue(Color(0xFFFFFFFF).isAchromatic())
        assertTrue(Color(0xFF000000).isAchromatic())
        assertTrue(Color(0xFFC0C0C0).isAchromatic())
        assertTrue(Color(0xFF181D26).isAchromatic())
        assertTrue(Color(0xFF05070A).isAchromatic())

        assertFalse(Color(0xFF00FFA3).isAchromatic())
        assertFalse(Color(0xFF3F8FE0).isAchromatic())
        assertFalse(Color(0xFFE0A03F).isAchromatic())
        assertFalse(Color(0xFFE6474E).isAchromatic())
        assertFalse(Color(0xFFFF1493).isAchromatic())
    }

    @Test
    fun testDefaultConsoleAccentDetection() {
        assertTrue(isDefaultConsoleAccent(Color(0xFF00FFA3), "A"))
        assertTrue(isDefaultConsoleAccent(XboxColors.A, "A"))
        assertTrue(isDefaultConsoleAccent(XboxColors.X, "X"))
        assertTrue(isDefaultConsoleAccent(XboxColors.Y, "Y"))
        assertTrue(isDefaultConsoleAccent(XboxColors.B, "B"))

        // Custom colors or wrong button controls
        assertFalse(isDefaultConsoleAccent(Color(0xFFFF1493), "A"))
        assertFalse(isDefaultConsoleAccent(Color(0xFFFFD700), "A"))
        assertFalse(isDefaultConsoleAccent(Color(0xFFFFFFFF), "A"))
        assertFalse(isDefaultConsoleAccent(Color(0xFF00FFA3), "LT"))
        assertFalse(isDefaultConsoleAccent(Color(0xFF00FFA3), "X"))
    }
}
