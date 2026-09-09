package com.sanket.tools.nexpad.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sanket.tools.nexpad.utils.LayoutManager
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel

@Composable
fun NavigationGraph(
    viewModel: GamepadViewModel,
    layoutManager: LayoutManager,
    context: Context,
    onVibrate: () -> Unit
) {
    val navController = rememberNavController()
    val sharedPref = context.getSharedPreferences("nexpad_prefs", Context.MODE_PRIVATE)

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(navController = navController, layoutManager = layoutManager, viewModel = viewModel)
        }
        composable("settings") {
            SettingsScreen(navController = navController, layoutManager = layoutManager, viewModel = viewModel, sharedPref = sharedPref)
        }
        composable("connections") {
            ConnectionScreen(navController = navController, viewModel = viewModel)
        }
        composable("editor") {
            HudEditorScreen(navController = navController, layoutManager = layoutManager)
        }
        composable(
            route = "button_studio?mode={mode}&profileName={profileName}",
            arguments = listOf(
                androidx.navigation.navArgument("mode") {
                    type = androidx.navigation.NavType.StringType
                    defaultValue = "manage"
                },
                androidx.navigation.navArgument("profileName") {
                    type = androidx.navigation.NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val modeArg = backStackEntry.arguments?.getString("mode") ?: "manage"
            val profileNameArg = backStackEntry.arguments?.getString("profileName") ?: ""
            com.sanket.tools.nexpad.ui.studio.ButtonStudioScreen(
                navController = navController,
                layoutManager = layoutManager,
                initialMode = if (modeArg.equals("select", ignoreCase = true)) {
                    com.sanket.tools.nexpad.ui.studio.model.ButtonStudioMode.SELECTION
                } else {
                    com.sanket.tools.nexpad.ui.studio.model.ButtonStudioMode.MANAGE
                },
                targetProfileName = profileNameArg
            )
        }
        composable("virtual_controller") {
            VirtualControllerScreen(navController = navController, layoutManager = layoutManager)
        }
        composable("gamepad") {
            GamepadScreen(
                viewModel = viewModel,
                layoutManager = layoutManager,
                onBack = { navController.popBackStack() },
                onVibrate = onVibrate
            )
        }
    }
}
