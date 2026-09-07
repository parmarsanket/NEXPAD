package com.sanket.tools.nexpad.ui.components.controller

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel

@Composable
fun GamepadButton(
    text: String,
    isConnected: Boolean,
    onVibrate: () -> Unit,
    viewModel: GamepadViewModel,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    
    Box(
        modifier = modifier
            .padding(4.dp)
            .size(64.dp)
            .background(
                color = if (isPressed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                shape = CircleShape
            )
            .pointerInput(isConnected) {
                detectTapGestures(
                    onPress = {
                        if (isConnected) onVibrate()
                        isPressed = true
                        viewModel.updateButton(text, true)
                        tryAwaitRelease()
                        isPressed = false
                        viewModel.updateButton(text, false)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text, 
            color = if (isPressed) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
    }
}
