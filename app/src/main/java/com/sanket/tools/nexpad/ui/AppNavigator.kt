package com.sanket.tools.nexpad.ui

import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.navigation3.runtime.NavKey
import kotlin.collections.lastIndex

/**
 * Modern Navigation 3 Navigator for NEXPAD.
 * 100% Type-Safe Navigation via [Route]. No strings, no regexes, no query parameters.
 */
interface AppNavigator {
    val backStack: List<NavKey>
    fun navigate(route: Route)
    fun popBackStack(): Boolean
}

class Nav3AppNavigator(
    private val backStackState: MutableList<NavKey>
) : AppNavigator {
    override val backStack: List<NavKey> get() = backStackState

    override fun navigate(route: Route) {
        backStackState.add(route)
    }

    override fun popBackStack(): Boolean {
        if (backStackState.size > 1) {
            backStackState.removeAt(backStackState.lastIndex)
            return true
        }
        return false
    }
}
