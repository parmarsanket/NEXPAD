package com.sanket.tools.nexpad.ui.components.controller

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel

@Composable
fun RealisticMacroButton(
    key: String,
    isConnected: Boolean,
    onVibrate: () -> Unit,
    viewModel: GamepadViewModel,
    isRgbEnabled: Boolean
) {
    var isPressed by remember { mutableStateOf(false) }

    val gradient = Brush.linearGradient(
        colors = listOf(Color(0xFF333333), Color(0xFF111111))
    )

    val shadow = if (isRgbEnabled) {
        Modifier.shadow(6.dp, RoundedCornerShape(16.dp), spotColor = Color.Yellow)
    } else {
        Modifier.shadow(4.dp, RoundedCornerShape(16.dp))
    }

    Box(
        modifier = Modifier
            .size(80.dp, 40.dp)
            .then(shadow)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isPressed) Color.Black else Color.Transparent)
            .background(gradient)
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
        Text(key, color = Color.LightGray, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}
