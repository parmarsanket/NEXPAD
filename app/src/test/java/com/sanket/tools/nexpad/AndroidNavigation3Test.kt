package com.sanket.tools.nexpad

import androidx.compose.runtime.mutableStateListOf
import androidx.navigation3.runtime.NavKey
import com.sanket.tools.nexpad.ui.Nav3AppNavigator
import com.sanket.tools.nexpad.ui.ScreenKey
import org.junit.Assert.*
import org.junit.Test

class AndroidNavigation3Test {

    @Test
    fun testInitialBackStack() {
        val backStack = mutableStateListOf<NavKey>(ScreenKey.Home)
        val navigator = Nav3AppNavigator(backStack)

        assertEquals(1, navigator.backStack.size)
        assertEquals(ScreenKey.Home, navigator.backStack.last())
    }

    @Test
    fun testTypedNavigation() {
        val backStack = mutableStateListOf<NavKey>(ScreenKey.Home)
        val navigator = Nav3AppNavigator(backStack)

        navigator.navigate(ScreenKey.Settings)
        assertEquals(2, navigator.backStack.size)
        assertEquals(ScreenKey.Settings, navigator.backStack.last())

        navigator.navigate(ScreenKey.ButtonStudio("manage", "Default"))
        assertEquals(3, navigator.backStack.size)
        val studioKey = navigator.backStack.last() as ScreenKey.ButtonStudio
        assertEquals("manage", studioKey.mode)
        assertEquals("Default", studioKey.profileName)
    }

    @Test
    fun testLegacyStringNavigation() {
        val backStack = mutableStateListOf<NavKey>(ScreenKey.Home)
        val navigator = Nav3AppNavigator(backStack)

        navigator.navigate("gamepad")
        assertEquals(ScreenKey.Gamepad, navigator.backStack.last())

        navigator.navigate("connections")
        assertEquals(ScreenKey.Connections, navigator.backStack.last())

        navigator.navigate("editor")
        assertEquals(ScreenKey.Editor(), navigator.backStack.last())

        navigator.navigate("virtual_controller")
        assertEquals(ScreenKey.VirtualController, navigator.backStack.last())
    }

    @Test
    fun testLegacyButtonStudioParamParsing() {
        val backStack = mutableStateListOf<NavKey>(ScreenKey.Home)
        val navigator = Nav3AppNavigator(backStack)

        navigator.navigate("button_studio?mode=select&profileName=Arcade_Fighter")
        val studioKey = navigator.backStack.last() as ScreenKey.ButtonStudio
        assertEquals("editor", studioKey.mode)
        assertEquals("Arcade_Fighter", studioKey.profileName)
    }

    @Test
    fun testPopBackStack() {
        val backStack = mutableStateListOf<NavKey>(ScreenKey.Home)
        val navigator = Nav3AppNavigator(backStack)

        navigator.navigate(ScreenKey.Connections)
        assertEquals(2, navigator.backStack.size)

        val popped1 = navigator.popBackStack()
        assertTrue(popped1)
        assertEquals(1, navigator.backStack.size)
        assertEquals(ScreenKey.Home, navigator.backStack.last())

        // Should not pop root Home
        val poppedRoot = navigator.popBackStack()
        assertFalse(poppedRoot)
        assertEquals(1, navigator.backStack.size)
    }
}
