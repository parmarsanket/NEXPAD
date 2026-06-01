package com.sanket.tools.nexpad.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.sanket.tools.nexpad.model.Position
import com.sanket.tools.nexpad.model.defaultPositions
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
    
    var selectedKey by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
            .pointerInput(Unit) {
                detectTapGestures { selectedKey = null } // Deselect if tapping background
            }
    ) {
        // Render all buttons
        positions.forEach { (key, position) ->
            var offsetX by remember { mutableFloatStateOf(position.xRatio * screenWidth) }
            var offsetY by remember { mutableFloatStateOf(position.yRatio * screenHeight) }
            val isSelected = selectedKey == key

            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                    // Apply scale and opacity
                    .scale(position.scale)
                    .alpha(position.opacity)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { selectedKey = key },
                            onDragEnd = {
                                positions[key] = position.copy(xRatio = offsetX / screenWidth, yRatio = offsetY / screenHeight)
                            }
                        ) { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(onPress = { selectedKey = key })
                    }
            ) {
                // The Button Component
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

                // Glass Shield Overlay to block game logic + show selection box
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(if (isSelected) Color(0x3300E676) else Color.Transparent)
                        .border(if (isSelected) 2.dp else 0.dp, if (isSelected) Color(0xFF00E676) else Color.Transparent)
                )
            }
        }

        // Top Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Text("⬅️", fontSize = 24.sp, color = Color.White)
            }
            
            Row {
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                    modifier = Modifier.padding(end = 16.dp)
                ) {
                    Text("⚙️ ADD BUTTONS", color = Color.White)
                }
                
                Button(
                    onClick = {
                        val updatedProfile = profile.copy(positions = positions.toMap())
                        layoutManager.saveProfile(updatedProfile)
                        navController.popBackStack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676))
                ) {
                    Text("SAVE LAYOUT", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Selected Control Panel
        if (selectedKey != null) {
            val position = positions[selectedKey]!!
            ElevatedCard(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp)
                    .width(320.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFF1A1A1A))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Editing Button: $selectedKey", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("Size", color = Color.LightGray, modifier = Modifier.width(60.dp))
                        Slider(
                            value = position.scale,
                            onValueChange = { positions[selectedKey!!] = position.copy(scale = it) },
                            valueRange = 0.5f..2.5f,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("Opacity", color = Color.LightGray, modifier = Modifier.width(60.dp))
                        Slider(
                            value = position.opacity,
                            onValueChange = { positions[selectedKey!!] = position.copy(opacity = it) },
                            valueRange = 0.1f..1.0f,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            positions.remove(selectedKey)
                            selectedKey = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD50000)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("REMOVE BUTTON", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Add/Remove Dialog (Material 3 with Scroll)
        if (showAddDialog) {
            val allKeys = defaultPositions().keys.toList()
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Manage Controller Buttons", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        allKeys.forEach { key ->
                            val isPresent = positions.containsKey(key)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isPresent) {
                                            positions.remove(key)
                                        } else {
                                            positions[key] = defaultPositions()[key]!!
                                        }
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isPresent, 
                                    onCheckedChange = null,
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF00C853))
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(key, fontSize = 18.sp, fontWeight = if (isPresent) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text("Done", color = Color(0xFF00C853), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                },
                containerColor = Color(0xFF1E1E1E),
                titleContentColor = Color.White,
                textContentColor = Color.LightGray
            )
        }
    }
}
