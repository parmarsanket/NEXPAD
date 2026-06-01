package com.sanket.tools.nexpad.ui.components

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
fun DPadLayout(isConnected: Boolean, onVibrate: () -> Unit, viewModel: GamepadViewModel) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        GamepadButton("UP", isConnected, onVibrate, viewModel)
        Row {
            GamepadButton("LEFT", isConnected, onVibrate, viewModel)
            Spacer(modifier = Modifier.width(64.dp))
            GamepadButton("RIGHT", isConnected, onVibrate, viewModel)
        }
        GamepadButton("DOWN", isConnected, onVibrate, viewModel)
    }
}
