package com.sanket.tools.nexpad.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.sanket.tools.nexpad.ui.studio.ButtonStudioScreen
import com.sanket.tools.nexpad.ui.studio.model.ButtonStudioMode
import com.sanket.tools.nexpad.utils.LayoutManager
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel

import androidx.navigation3.runtime.rememberNavBackStack
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@Composable
fun NavigationGraph(
    viewModel: GamepadViewModel,
    layoutManager: LayoutManager,
    context: Context,
    onVibrate: () -> Unit
) {
    val backStack = rememberNavBackStack(
        configuration = SavedStateConfiguration {
            serializersModule = SerializersModule {
                polymorphic(NavKey::class) {
                    subclass(ScreenKey.Home::class, ScreenKey.Home.serializer())
                    subclass(ScreenKey.Settings::class, ScreenKey.Settings.serializer())
                    subclass(ScreenKey.Connections::class, ScreenKey.Connections.serializer())
                    subclass(ScreenKey.Editor::class, ScreenKey.Editor.serializer())
                    subclass(ScreenKey.VirtualController::class, ScreenKey.VirtualController.serializer())
                    subclass(ScreenKey.Gamepad::class, ScreenKey.Gamepad.serializer())
                    subclass(ScreenKey.ButtonStudio::class, ScreenKey.ButtonStudio.serializer())
                }
            }
        },
        ScreenKey.Home
    )
    val navigator = remember(backStack) { Nav3AppNavigator(backStack) }
    val sharedPref = remember(context) { context.getSharedPreferences("nexpad_prefs", Context.MODE_PRIVATE) }

    // Graph-scoped ViewModel: shared by all entries for cross-screen editing context.
    // This replaces LayoutManager.pendingSelectedKey.
    val navigationViewModel: NavigationViewModel = viewModel()

    val stateDecorator = rememberSaveableStateHolderNavEntryDecorator<NavKey>()
    val vmDecorator = rememberViewModelStoreNavEntryDecorator<NavKey>()

    // Intercept system/hardware back button when on nested screens
    BackHandler(enabled = backStack.size > 1) {
        navigator.popBackStack()
    }

    NavDisplay(
        backStack = backStack,
        entryDecorators = listOf(stateDecorator, vmDecorator),
        onBack = { navigator.popBackStack() },
        transitionSpec = {
            slideInHorizontally { it } + fadeIn() togetherWith
                    slideOutHorizontally { -it } + fadeOut()
        },
        popTransitionSpec = {
            slideInHorizontally { -it } + fadeIn() togetherWith
                    slideOutHorizontally { it } + fadeOut()
        },
        predictivePopTransitionSpec = {
            slideInHorizontally { -it } + fadeIn() togetherWith
                    slideOutHorizontally { it } + fadeOut()
        }
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
                    HudEditorScreen(
                        navController = navigator,
                        layoutManager = layoutManager,
                        navigationViewModel = navigationViewModel,
                        initialProfileName = key.profileName,
                        initialControlKey = key.controlKey,
                        gamepadViewModel = viewModel
                    )
                }
                is ScreenKey.VirtualController -> {
                    VirtualControllerScreen(
                        navController = navigator,
                        layoutManager = layoutManager,
                        navigationViewModel = navigationViewModel
                    )
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
                    val mode = when {
                        key.mode.equals("editor", ignoreCase = true) || key.mode.equals("select", ignoreCase = true) -> ButtonStudioMode.EDITOR
                        key.profileName.isNotBlank() -> ButtonStudioMode.EDITOR
                        else -> ButtonStudioMode.VIEWER
                    }
                    ButtonStudioScreen(
                        navController = navigator,
                        layoutManager = layoutManager,
                        navigationViewModel = navigationViewModel,
                        initialMode = mode,
                        targetProfileName = key.profileName,
                        targetControlKey = key.controlKey,
                        targetCurrentAssetId = key.currentAssetId,
                        gamepadViewModel = viewModel
                    )
                }
                else -> {
                    HomeScreen(navController = navigator, layoutManager = layoutManager, viewModel = viewModel)
                }
            }
        }
    }
}


