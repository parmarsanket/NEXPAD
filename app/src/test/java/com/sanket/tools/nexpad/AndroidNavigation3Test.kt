package com.sanket.tools.nexpad

import androidx.compose.runtime.mutableStateListOf
import androidx.navigation3.runtime.NavKey
import com.sanket.tools.nexpad.ui.Nav3AppNavigator
import com.sanket.tools.nexpad.ui.NavigationViewModel
import com.sanket.tools.nexpad.ui.Route
import com.sanket.tools.nexpad.ui.ScreenKey
import org.junit.Assert.*
import org.junit.Test

class AndroidNavigation3Test {

    @Test
    fun testInitialBackStack() {
        val backStack = mutableStateListOf<NavKey>(Route.Home)
        val navigator = Nav3AppNavigator(backStack)

        assertEquals(1, navigator.backStack.size)
        assertEquals(Route.Home, navigator.backStack.last())
    }

    @Test
    fun testTypedNavigation() {
        val backStack = mutableStateListOf<NavKey>(Route.Home)
        val navigator = Nav3AppNavigator(backStack)

        navigator.navigate(Route.Settings)
        assertEquals(2, navigator.backStack.size)
        assertEquals(Route.Settings, navigator.backStack.last())

        navigator.navigate(Route.ButtonStudio("manage", "Default"))
        assertEquals(3, navigator.backStack.size)
        val studioKey = navigator.backStack.last() as Route.ButtonStudio
        assertEquals("manage", studioKey.mode)
        assertEquals("Default", studioKey.profileName)
    }

    @Test
    fun testRouteTypeSafeNavigation() {
        val backStack = mutableStateListOf<NavKey>(Route.Home)
        val navigator = Nav3AppNavigator(backStack)

        navigator.navigate(Route.Gamepad())
        assertEquals(Route.Gamepad(), navigator.backStack.last())

        navigator.navigate(Route.Gamepad(layoutProfileName = "Racing"))
        assertEquals(Route.Gamepad("Racing"), navigator.backStack.last())

        navigator.navigate(Route.Connections)
        assertEquals(Route.Connections, navigator.backStack.last())

        navigator.navigate(Route.Editor())
        assertEquals(Route.Editor(), navigator.backStack.last())

        navigator.navigate(Route.VirtualController)
        assertEquals(Route.VirtualController, navigator.backStack.last())
    }

    @Test
    fun testNavigationViewModelGamepadSession() {
        val navVm = NavigationViewModel()
        assertNull(navVm.sessionProfileName.value)

        navVm.startGamepadSession("Racing Custom")
        assertEquals("Racing Custom", navVm.sessionProfileName.value)

        navVm.clearGamepadSession()
        assertNull(navVm.sessionProfileName.value)
    }

    @Test
    fun testTypedButtonStudioNavigation() {
        val backStack = mutableStateListOf<NavKey>(Route.Home)
        val navigator = Nav3AppNavigator(backStack)

        navigator.navigate(Route.ButtonStudio(mode = "select", profileName = "Arcade_Fighter"))
        val studioKey = navigator.backStack.last() as Route.ButtonStudio
        assertEquals("select", studioKey.mode)
        assertEquals("Arcade_Fighter", studioKey.profileName)
    }

    @Test
    fun testPopBackStack() {
        val backStack = mutableStateListOf<NavKey>(Route.Home)
        val navigator = Nav3AppNavigator(backStack)

        navigator.navigate(Route.Connections)
        assertEquals(2, navigator.backStack.size)

        val popped1 = navigator.popBackStack()
        assertTrue(popped1)
        assertEquals(1, navigator.backStack.size)
        assertEquals(Route.Home, navigator.backStack.last())

        // Should not pop root Home
        val poppedRoot = navigator.popBackStack()
        assertFalse(poppedRoot)
        assertEquals(1, navigator.backStack.size)
    }

    @Test
    fun testButtonStudioMode3ButtonEditorResolution() {
        val backStack = mutableStateListOf<NavKey>(Route.Home)
        val navigator = Nav3AppNavigator(backStack)

        navigator.navigate(
            Route.ButtonStudio(
                mode = "button_editor",
                profileName = "Default",
                controlKey = "B",
                currentAssetId = "builtin.cyber_octa_b"
            )
        )
        assertEquals(2, navigator.backStack.size)
        val studioKey = navigator.backStack.last() as Route.ButtonStudio
        assertEquals("button_editor", studioKey.mode)
        assertEquals("B", studioKey.controlKey)
        assertEquals("builtin.cyber_octa_b", studioKey.currentAssetId)

        // Verify routing logic resolves to BUTTON_EDITOR
        val resolvedMode = when {
            studioKey.mode.equals("button_editor", ignoreCase = true) -> com.sanket.tools.nexpad.ui.studio.model.ButtonStudioMode.BUTTON_EDITOR
            studioKey.mode.equals("editor", ignoreCase = true) || studioKey.mode.equals("select", ignoreCase = true) -> com.sanket.tools.nexpad.ui.studio.model.ButtonStudioMode.EDITOR
            studioKey.profileName.isNotBlank() -> com.sanket.tools.nexpad.ui.studio.model.ButtonStudioMode.EDITOR
            else -> com.sanket.tools.nexpad.ui.studio.model.ButtonStudioMode.VIEWER
        }
        assertEquals(com.sanket.tools.nexpad.ui.studio.model.ButtonStudioMode.BUTTON_EDITOR, resolvedMode)
    }

    @Test
    fun testHudEditorRouteProfileIsolation() {
        val backStack = mutableStateListOf<NavKey>(Route.Home)
        val navigator = Nav3AppNavigator(backStack)

        // Navigating to HUD Editor for a specific profile (e.g. "FPS Pro Custom")
        navigator.navigate(Route.Editor(profileName = "FPS Pro Custom", controlKey = "RT"))
        assertEquals(2, navigator.backStack.size)

        val editorKey = navigator.backStack.last() as Route.Editor
        assertEquals("FPS Pro Custom", editorKey.profileName)
        assertEquals("RT", editorKey.controlKey)
    }
}
