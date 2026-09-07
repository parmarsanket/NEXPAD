package com.sanket.tools.nexpad.ui

import android.content.pm.ActivityInfo
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
import com.sanket.tools.nexpad.ui.components.controller.RealisticBumper
import com.sanket.tools.nexpad.ui.components.controller.RealisticButton
import com.sanket.tools.nexpad.ui.components.controller.RealisticDPad
import com.sanket.tools.nexpad.ui.components.controller.RealisticJoystick
import com.sanket.tools.nexpad.ui.components.controller.RealisticMacroButton
import com.sanket.tools.nexpad.ui.components.controller.RealisticSystemButton
import com.sanket.tools.nexpad.ui.components.controller.RealisticTrigger
import com.sanket.tools.nexpad.utils.LayoutManager
import com.sanket.tools.nexpad.utils.LockScreenOrientation
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

    // Auto-save when leaving the editor
    DisposableEffect(Unit) {
        onDispose {
            val updatedProfile = profile.copy(positions = positions.toMap())
            layoutManager.saveProfile(updatedProfile)
        }
    }
    LockScreenOrientation(
        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {
                detectTapGestures { selectedKey = null } // Deselect if tapping background
            }
    ) {

        // Render all buttons
        positions.forEach { (key, position) ->
            var offsetX by remember(screenWidth, screenHeight) { mutableFloatStateOf(position.xRatio * screenWidth) }
            var offsetY by remember(screenWidth, screenHeight) { mutableFloatStateOf(position.yRatio * screenHeight) }
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
                                val currentPos = positions[key] ?: return@detectDragGestures
                                positions[key] = currentPos.copy(xRatio = offsetX / screenWidth, yRatio = offsetY / screenHeight)
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
                LockScreenOrientation(
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                )
                // The Button Component
                when {
                    key == "LS" -> RealisticJoystick(
                        isLeft = true,
                        isConnected = false,
                        viewModel = dummyViewModel,
                        isRgbEnabled = profile.isRgbEnabled
                    )
                    key == "RS" -> RealisticJoystick(
                        isLeft = false,
                        isConnected = false,
                        viewModel = dummyViewModel,
                        isRgbEnabled = profile.isRgbEnabled
                    )
                    key == "DPAD" -> RealisticDPad(
                        isConnected = false,
                        viewModel = dummyViewModel,
                        isRgbEnabled = profile.isRgbEnabled
                    )
                    key == "LT" || key == "RT" -> RealisticTrigger(
                        key = key,
                        isConnected = false,
                        onVibrate = {},
                        viewModel = dummyViewModel,
                        isRgbEnabled = profile.isRgbEnabled
                    )
                    key == "LB" || key == "RB" -> RealisticBumper(
                        key = key,
                        isConnected = false,
                        onVibrate = {},
                        viewModel = dummyViewModel,
                        isRgbEnabled = profile.isRgbEnabled
                    )
                    key == "A" -> RealisticButton(
                        key = "A",
                        buttonColor = Color(0xFF00C853),
                        isConnected = false,
                        onVibrate = {},
                        viewModel = dummyViewModel,
                        isRgbEnabled = profile.isRgbEnabled
                    )
                    key == "B" -> RealisticButton(
                        key = "B",
                        buttonColor = Color(0xFFD50000),
                        isConnected = false,
                        onVibrate = {},
                        viewModel = dummyViewModel,
                        isRgbEnabled = profile.isRgbEnabled
                    )
                    key == "X" -> RealisticButton(
                        key = "X",
                        buttonColor = Color(0xFF2962FF),
                        isConnected = false,
                        onVibrate = {},
                        viewModel = dummyViewModel,
                        isRgbEnabled = profile.isRgbEnabled
                    )
                    key == "Y" -> RealisticButton(
                        key = "Y",
                        buttonColor = Color(0xFFFFD600),
                        isConnected = false,
                        onVibrate = {},
                        viewModel = dummyViewModel,
                        isRgbEnabled = profile.isRgbEnabled
                    )
                    key in listOf("MENU", "VIEW", "XBOX", "SHARE", "SCREENSHOT") -> RealisticSystemButton(
                        key = key,
                        isConnected = false,
                        onVibrate = {},
                        viewModel = dummyViewModel,
                        isRgbEnabled = profile.isRgbEnabled
                    )
                    key in listOf("M1", "M2", "M3", "M4", "PROFILE", "TURBO") -> RealisticMacroButton(
                        key = key,
                        isConnected = false,
                        onVibrate = {},
                        viewModel = dummyViewModel,
                        isRgbEnabled = profile.isRgbEnabled
                    )
                    else -> RealisticButton(
                        key = key,
                        buttonColor = Color.Gray,
                        isConnected = false,
                        onVibrate = {},
                        viewModel = dummyViewModel,
                        isRgbEnabled = profile.isRgbEnabled
                    )
                }

                // Glass Shield Overlay to block game logic + show selection box
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent)
                        .border(if (isSelected) 2.dp else 0.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
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
                Text("⬅️", fontSize = 24.sp, color = MaterialTheme.colorScheme.onBackground)
            }
            
            Row {
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.padding(end = 16.dp),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text("⚙️ ADD BUTTONS", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                Button(
                    onClick = {
                        val updatedProfile = profile.copy(positions = positions.toMap())
                        layoutManager.saveProfile(updatedProfile)
                        navController.popBackStack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text("SAVE LAYOUT", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }

        // Selected Control Panel
        val currentKey = selectedKey
        if (currentKey != null && positions.containsKey(currentKey)) {
            val position = positions[currentKey]!!
            ElevatedCard(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp)
                    .width(320.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(28.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Editing Button: $currentKey", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("Size", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(60.dp))
                        Slider(
                            value = position.scale,
                            onValueChange = { positions[currentKey] = position.copy(scale = it) },
                            valueRange = 0.5f..2.5f,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("Opacity", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(60.dp))
                        Slider(
                            value = position.opacity,
                            onValueChange = { positions[currentKey] = position.copy(opacity = it) },
                            valueRange = 0.1f..1.0f,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            positions.remove(currentKey)
                            selectedKey = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Text("REMOVE BUTTON", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onError)
                    }
                }
            }
        }

        // Add/Remove Dialog (Material 3 with Scroll)
        if (showAddDialog) {
            val allKeys = defaultPositions().keys.toList()
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Manage Controller Buttons", style = MaterialTheme.typography.titleLarge) },
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
                                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(key, fontSize = 18.sp, fontWeight = if (isPresent) FontWeight.Bold else FontWeight.Normal, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text("Done", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
