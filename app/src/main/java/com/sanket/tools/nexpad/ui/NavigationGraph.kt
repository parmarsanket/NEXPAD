package com.sanket.tools.nexpad.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.sanket.tools.nexpad.ui.studio.ButtonStudioScreen
import com.sanket.tools.nexpad.ui.studio.model.ButtonStudioMode
import com.sanket.tools.nexpad.utils.LayoutManager
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel

@Composable
fun NavigationGraph(
    viewModel: GamepadViewModel,
    layoutManager: LayoutManager,
    context: Context,
    onVibrate: () -> Unit
) {
    val backStack = remember { mutableStateListOf<NavKey>(ScreenKey.Home) }
    val navigator = remember(backStack) { Nav3AppNavigator(backStack) }
    val sharedPref = remember(context) { context.getSharedPreferences("nexpad_prefs", Context.MODE_PRIVATE) }

    val stateDecorator = rememberSaveableStateHolderNavEntryDecorator<NavKey>()
    val vmDecorator = rememberViewModelStoreNavEntryDecorator<NavKey>()

    // Intercept system/hardware back button when on nested screens
    BackHandler(enabled = backStack.size > 1) {
        navigator.popBackStack()
    }

    NavDisplay(
        backStack = backStack,
        entryDecorators = listOf(stateDecorator, vmDecorator),
        onBack = { navigator.popBackStack() }
    ) { key ->
        NavEntry(key) {
            when (key) {
                is ScreenKey.Home -> {
                    HomeScreen(navController = navigator, layoutManager = layoutManager, viewModel = viewModel)
                }
                is ScreenKey.Settings -> {
                    SettingsScreen(navController = navigator, layoutManager = layoutManager, viewModel = viewModel, sharedPref = sharedPref)
                }
                is ScreenKey.Connections -> {
                    ConnectionScreen(navController = navigator, viewModel = viewModel)
                }
                is ScreenKey.Editor -> {
                    HudEditorScreen(navController = navigator, layoutManager = layoutManager)
                }
                is ScreenKey.VirtualController -> {
                    VirtualControllerScreen(navController = navigator, layoutManager = layoutManager)
                }
                is ScreenKey.Gamepad -> {
                    GamepadScreen(
                        viewModel = viewModel,
                        layoutManager = layoutManager,
                        onBack = { navigator.popBackStack() },
                        onVibrate = onVibrate
                    )
                }
                is ScreenKey.ButtonStudio -> {
                    ButtonStudioScreen(
                        navController = navigator,
                        layoutManager = layoutManager,
                        initialMode = if (key.mode.equals("select", ignoreCase = true)) {
                            ButtonStudioMode.SELECTION
                        } else {
                            ButtonStudioMode.MANAGE
                        },
                        targetProfileName = key.profileName
                    )
                }
                else -> {
                    HomeScreen(navController = navigator, layoutManager = layoutManager, viewModel = viewModel)
                }
            }
        }
    }
}



