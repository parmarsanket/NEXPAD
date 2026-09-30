package com.sanket.tools.nexpad.ui.components.controller

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel

@Composable
fun ABXYLayout(isConnected: Boolean, onVibrate: () -> Unit, viewModel: GamepadViewModel) {
    val K = com.sanket.tools.nexpad.model.NexpadKeys
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        GamepadButton(K.Y, isConnected, onVibrate, viewModel)
        Row {
            GamepadButton(K.X, isConnected, onVibrate, viewModel)
            Spacer(modifier = Modifier.width(64.dp))
            GamepadButton(K.B, isConnected, onVibrate, viewModel)
        }
        GamepadButton(K.A, isConnected, onVibrate, viewModel)
    }
}
