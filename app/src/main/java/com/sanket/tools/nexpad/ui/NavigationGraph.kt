package com.sanket.tools.nexpad.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sanket.tools.nexpad.utils.LayoutManager
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel

@Composable
fun NavigationGraph(viewModel: GamepadViewModel, layoutManager: LayoutManager) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "main_menu") {
        composable("main_menu") {
            MainMenuScreen(navController, layoutManager)
        }
        composable("hud_editor") {
            HudEditorScreen(navController, layoutManager)
        }
        composable("gamepad") {
            GamepadScreen(
                viewModel = viewModel,
                layoutManager = layoutManager,
                onBack = { navController.popBackStack() },
                onVibrate = { /* Handled in MainActivity */ }
            )
        }
    }
}
