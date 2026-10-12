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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

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
}
