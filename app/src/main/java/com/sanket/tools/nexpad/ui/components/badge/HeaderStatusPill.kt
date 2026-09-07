package com.sanket.tools.nexpad.ui.components.badge

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sanket.tools.nexpad.ui.theme.NeonPalette

@Composable
fun HeaderStatusPill(
    isConnected: Boolean,
    modifier: Modifier = Modifier,
    connectedText: String = "Connected",
    disconnectedText: String = "Disconnected"
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (isConnected) NeonPalette.Green.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
            )
            .border(
                1.dp,
                if (isConnected) NeonPalette.Green.copy(alpha = 0.6f)
                else Color.White.copy(alpha = 0.15f),
                RoundedCornerShape(50)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .semantics {
                contentDescription = "Connection status: " + if (isConnected) connectedText else disconnectedText
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (isConnected) NeonPalette.ConnectedDot else Color.Red)
        )
        Text(
            if (isConnected) connectedText else disconnectedText,
            style = MaterialTheme.typography.labelMedium,
            color = if (isConnected) NeonPalette.Green else MaterialTheme.colorScheme.onSurface
        )
    }
}
