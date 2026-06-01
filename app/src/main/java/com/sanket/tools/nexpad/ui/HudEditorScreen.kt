package com.sanket.tools.nexpad.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import com.sanket.tools.nexpad.R
import com.sanket.tools.nexpad.model.Position
import com.sanket.tools.nexpad.ui.components.CroppedImage
import com.sanket.tools.nexpad.utils.LayoutManager
import kotlin.math.roundToInt

@Composable
fun HudEditorScreen(navController: NavController, layoutManager: LayoutManager) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp.value
    val screenHeight = configuration.screenHeightDp.dp.value

    var profile by remember { mutableStateOf(layoutManager.getActiveProfile()) }
    var positions by remember { mutableStateOf(profile.positions.toMutableMap()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Base controller image as the background shell
        Image(
            painter = painterResource(id = R.drawable.realistic_controller),
            contentDescription = "Controller Shell",
            modifier = Modifier.fillMaxSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.FillBounds,
            alpha = 0.3f // Dim the background so the draggable buttons pop out
        )

        // Render all buttons at their current dragged positions
        positions.forEach { (key, position) ->
            var offsetX by remember { mutableFloatStateOf(position.xRatio * screenWidth) }
            var offsetY by remember { mutableFloatStateOf(position.yRatio * screenHeight) }

            val crop = buttonCrops[key]

            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = {
                                // Save the new ratio to state
                                positions[key] = Position(offsetX / screenWidth, offsetY / screenHeight)
                            }
                        ) { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y
                        }
                    }
            ) {
                if (crop != null) {
                    CroppedImage(
                        imageRes = R.drawable.realistic_controller,
                        srcOffsetX = crop.x,
                        srcOffsetY = crop.y,
                        cropWidth = crop.w,
                        cropHeight = crop.h,
                        targetWidthDp = (crop.w * 0.4f).toInt(),
                        targetHeightDp = (crop.h * 0.4f).toInt(),
                        modifier = Modifier.background(Color.White.copy(alpha=0.1f)) // Show box in editor mode
                    )
                } else {
                    Box(modifier = Modifier.background(Color.White.copy(alpha = 0.2f)).padding(8.dp)) {
                        Text(key, color = Color.White)
                    }
                }
            }
        }

        // Top Bar for saving
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("DRAG TO MOVE - HUD EDITOR", color = Color.White)
            Button(onClick = {
                val updatedProfile = profile.copy(positions = positions)
                layoutManager.saveProfile(updatedProfile)
                navController.popBackStack()
            }) {
                Text("Save & Exit")
            }
        }
    }
}
