package com.sanket.tools.nexpad.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import com.sanket.tools.nexpad.R
import com.sanket.tools.nexpad.model.Position
import com.sanket.tools.nexpad.ui.components.RealisticButton
import com.sanket.tools.nexpad.ui.components.RealisticDPad
import com.sanket.tools.nexpad.ui.components.RealisticJoystick
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
    // We pass a dummy GamepadViewModel because editor shouldn't send actual events
    val dummyViewModel = androidx.lifecycle.viewmodel.compose.viewModel<GamepadViewModel>()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Base controller image as the background shell
        Image(
            painter = painterResource(id = R.drawable.blank_controller),
            contentDescription = "Controller Shell",
            modifier = Modifier.fillMaxSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.FillBounds
        )

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
                    .background(Color.White.copy(alpha = 0.1f)) // highlight draggable area
            ) {
                when {
                    key == "L3" -> RealisticJoystick(isLeft = true, isConnected = false, viewModel = dummyViewModel, isRgbEnabled = profile.isRgbEnabled)
                    key == "R3" -> RealisticJoystick(isLeft = false, isConnected = false, viewModel = dummyViewModel, isRgbEnabled = profile.isRgbEnabled)
                    key == "DPAD" -> RealisticDPad(isConnected = false, viewModel = dummyViewModel, isRgbEnabled = profile.isRgbEnabled)
                    key == "A" -> RealisticButton(key = "A", buttonColor = Color(0xFF00C853), isConnected = false, onVibrate = {}, viewModel = dummyViewModel, isRgbEnabled = profile.isRgbEnabled)
                    key == "B" -> RealisticButton(key = "B", buttonColor = Color(0xFFD50000), isConnected = false, onVibrate = {}, viewModel = dummyViewModel, isRgbEnabled = profile.isRgbEnabled)
                    key == "X" -> RealisticButton(key = "X", buttonColor = Color(0xFF2962FF), isConnected = false, onVibrate = {}, viewModel = dummyViewModel, isRgbEnabled = profile.isRgbEnabled)
                    key == "Y" -> RealisticButton(key = "Y", buttonColor = Color(0xFFFFD600), isConnected = false, onVibrate = {}, viewModel = dummyViewModel, isRgbEnabled = profile.isRgbEnabled)
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
