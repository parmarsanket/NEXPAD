package com.sanket.tools.nexpad.ui.components.controller

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel

@Composable
fun RealisticSystemButton(
    key: String,
    isConnected: Boolean,
    onVibrate: () -> Unit,
    viewModel: GamepadViewModel,
    isRgbEnabled: Boolean
) {
    var isPressed by remember { mutableStateOf(false) }

    val shadow = if (isRgbEnabled) {
        Modifier.shadow(10.dp, CircleShape, ambientColor = Color.White, spotColor = Color.White)
    } else {
        Modifier.shadow(4.dp, CircleShape)
    }

    Box(
        modifier = Modifier
            .size(60.dp)
            .then(shadow)
            .clip(CircleShape)
            .background(if (isPressed) Color.DarkGray else Color(0xFF2B2B2B))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        if (isConnected) onVibrate()
                        isPressed = true
                        viewModel.updateButton(key, true)
                        tryAwaitRelease()
                        isPressed = false
                        viewModel.updateButton(key, false)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(key.take(1), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
    }
}
