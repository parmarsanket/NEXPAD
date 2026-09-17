package com.sanket.tools.nexpad.ui

import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.navigation3.runtime.NavKey
import kotlin.collections.lastIndex

/**
 * Modern Navigation 3 Navigator for NEXPAD.
 * Provides type-safe navigation via [ScreenKey] and backward-compatible string route routing.
 */
interface AppNavigator {
    val backStack: List<NavKey>
    fun navigate(key: ScreenKey)
    fun popBackStack(): Boolean

    /**
     * Backward compatibility route mapper for legacy callers.
     */
    fun navigate(route: String) {
        when {
            route == "home" -> navigate(ScreenKey.Home)
            route == "settings" -> navigate(ScreenKey.Settings)
            route == "connections" -> navigate(ScreenKey.Connections)
            route == "editor" -> navigate(ScreenKey.Editor())
            route == "virtual_controller" -> navigate(ScreenKey.VirtualController)
            route == "gamepad" -> navigate(ScreenKey.Gamepad)
            route.startsWith("button_studio") -> {
                val mode = when {
                    route.contains("mode=editor") -> "editor"
                    route.contains("mode=select") -> "editor"
                    else -> "viewer"
                }
                val profileNameMatch = Regex("""profileName=([^&]+)""").find(route)
                val profileName = profileNameMatch?.groupValues?.get(1)?.let {
                    runCatching { java.net.URLDecoder.decode(it, "UTF-8") }.getOrDefault(it)
                } ?: ""
                navigate(ScreenKey.ButtonStudio(mode, profileName))
            }
            else -> navigate(ScreenKey.Home)
        }
    }
}

class Nav3AppNavigator(
    private val backStackState: MutableList<NavKey>
) : AppNavigator {
    override val backStack: List<NavKey> get() = backStackState

    override fun navigate(key: ScreenKey) {
        backStackState.add(key)
    }

    override fun popBackStack(): Boolean {
        if (backStackState.size > 1) {
            backStackState.removeAt(backStackState.lastIndex)
            return true
        }
        return false
    }
}
