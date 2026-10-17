package com.sanket.tools.nexpad.ui

import android.content.Context
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
    onVibrate: () -> Unit
) {
    val backStack = rememberNavBackStack(
        configuration = SavedStateConfiguration {
            serializersModule = SerializersModule {
                polymorphic(NavKey::class) {
                    subclass(Route.Splash::class, Route.Splash.serializer())
                    subclass(Route.Home::class, Route.Home.serializer())
                    subclass(Route.Settings::class, Route.Settings.serializer())
                    subclass(Route.Connections::class, Route.Connections.serializer())
                    subclass(Route.Editor::class, Route.Editor.serializer())
                    subclass(Route.VirtualController::class, Route.VirtualController.serializer())
                    subclass(Route.Gamepad::class, Route.Gamepad.serializer())
                    subclass(Route.ButtonStudio::class, Route.ButtonStudio.serializer())
                }
            }
        },
        Route.Splash
    )
    val navigator = remember(backStack) { Nav3AppNavigator(backStack) }

    // Graph-scoped ViewModel: shared by all entries for cross-screen editing context.
    // This replaces LayoutManager.pendingSelectedKey.
    val navigationViewModel: NavigationViewModel = viewModel()

    val stateDecorator = rememberSaveableStateHolderNavEntryDecorator<NavKey>()
    val vmDecorator = rememberViewModelStoreNavEntryDecorator<NavKey>()

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
                is Route.Splash -> {
                    SplashScreen(
                        onSplashFinished = {
                            navigator.replaceRoot(Route.Home)
                        },
                        onVibrate = onVibrate
                    )
                }
                is Route.Home -> {
                    HomeScreen(
                        navController = navigator,
                        layoutManager = layoutManager,
                        viewModel = viewModel,
                        navigationViewModel = navigationViewModel
                    )
                }
                is Route.Settings -> {
                    SettingsScreen(navController = navigator, layoutManager = layoutManager, viewModel = viewModel)
                }
                is Route.Connections -> {
                    ConnectionScreen(navController = navigator, viewModel = viewModel)
                }
                is Route.Editor -> {
                    HudEditorScreen(
                        navController = navigator,
                        layoutManager = layoutManager,
                        navigationViewModel = navigationViewModel,
                        initialProfileName = key.profileName,
                        initialControlKey = key.controlKey,
                        gamepadViewModel = viewModel
                    )
                }
                is Route.VirtualController -> {
                    VirtualControllerScreen(
                        navController = navigator,
                        layoutManager = layoutManager,
                        navigationViewModel = navigationViewModel
                    )
                }
                is Route.Gamepad -> {
                    GamepadScreen(
                        viewModel = viewModel,
                        layoutManager = layoutManager,
                        navigationViewModel = navigationViewModel,
                        overrideProfileName = key.layoutProfileName,
                        onBack = { navigator.popBackStack() }
                    )
                }
                is Route.ButtonStudio -> {
                    ButtonStudioScreen(
                        navController = navigator,
                        layoutManager = layoutManager,
                        navigationViewModel = navigationViewModel,
                        initialMode = key.mode,
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


