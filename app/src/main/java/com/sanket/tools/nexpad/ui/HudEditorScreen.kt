package com.sanket.tools.nexpad.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.sanket.tools.nexpad.model.Position
import com.sanket.tools.nexpad.ui.components.*
import com.sanket.tools.nexpad.utils.LayoutManager
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel
import kotlin.math.roundToInt

@Composable
fun HudEditorScreen(navController: NavController, layoutManager: LayoutManager) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp.value
    val screenHeight = configuration.screenHeightDp.dp.value

    val profile = layoutManager.getActiveProfile()
    val positions = remember { mutableStateMapOf<String, Position>().apply { putAll(profile.positions) } }
    val dummyViewModel = androidx.lifecycle.viewmodel.compose.viewModel<GamepadViewModel>()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D)) // Solid sleek dark background, no image!
    ) {
        // Render all buttons at their current dragged positions
        positions.forEach { (key, position) ->
            var offsetX by remember { mutableFloatStateOf(position.xRatio * screenWidth) }
            var offsetY by remember { mutableFloatStateOf(position.yRatio * screenHeight) }

            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = {
                                positions[key] = Position(offsetX / screenWidth, offsetY / screenHeight)
                            }
                        ) { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y
                        }
                    }
                    .background(Color.White.copy(alpha = 0.05f)) // highlight draggable area
            ) {
                when {
                    key == "LS" -> RealisticJoystick(isLeft = true, isConnected = false, viewModel = dummyViewModel, isRgbEnabled = profile.isRgbEnabled)
                    key == "RS" -> RealisticJoystick(isLeft = false, isConnected = false, viewModel = dummyViewModel, isRgbEnabled = profile.isRgbEnabled)
                    key == "DPAD" -> RealisticDPad(isConnected = false, viewModel = dummyViewModel, isRgbEnabled = profile.isRgbEnabled)
                    key == "LT" || key == "RT" -> RealisticTrigger(key = key, isConnected = false, onVibrate = {}, viewModel = dummyViewModel, isRgbEnabled = profile.isRgbEnabled)
                    key == "LB" || key == "RB" -> RealisticBumper(key = key, isConnected = false, onVibrate = {}, viewModel = dummyViewModel, isRgbEnabled = profile.isRgbEnabled)
                    key == "A" -> RealisticButton(key = "A", buttonColor = Color(0xFF00C853), isConnected = false, onVibrate = {}, viewModel = dummyViewModel, isRgbEnabled = profile.isRgbEnabled)
                    key == "B" -> RealisticButton(key = "B", buttonColor = Color(0xFFD50000), isConnected = false, onVibrate = {}, viewModel = dummyViewModel, isRgbEnabled = profile.isRgbEnabled)
                    key == "X" -> RealisticButton(key = "X", buttonColor = Color(0xFF2962FF), isConnected = false, onVibrate = {}, viewModel = dummyViewModel, isRgbEnabled = profile.isRgbEnabled)
                    key == "Y" -> RealisticButton(key = "Y", buttonColor = Color(0xFFFFD600), isConnected = false, onVibrate = {}, viewModel = dummyViewModel, isRgbEnabled = profile.isRgbEnabled)
                    key in listOf("MENU", "VIEW", "XBOX", "SHARE", "SCREENSHOT") -> RealisticSystemButton(key = key, isConnected = false, onVibrate = {}, viewModel = dummyViewModel, isRgbEnabled = profile.isRgbEnabled)
                    key in listOf("M1", "M2", "M3", "M4", "PROFILE", "TURBO") -> RealisticMacroButton(key = key, isConnected = false, onVibrate = {}, viewModel = dummyViewModel, isRgbEnabled = profile.isRgbEnabled)
                    else -> RealisticButton(key = key, buttonColor = Color.Gray, isConnected = false, onVibrate = {}, viewModel = dummyViewModel, isRgbEnabled = profile.isRgbEnabled)
                }
            }
        }

        // Top bar for saving
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Text("⬅️", fontSize = 24.sp, color = Color.White)
            }
            Button(
                onClick = {
                    val updatedProfile = profile.copy(positions = positions.toMap())
                    layoutManager.saveProfile(updatedProfile)
                    navController.popBackStack()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676))
            ) {
                Text("SAVE LAYOUT")
            }
        }
    }
}
