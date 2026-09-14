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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.ui.theme.NeonPalette

@Composable
fun StatusBadgePill(
    text: String,
    isPositive: Boolean,
    modifier: Modifier = Modifier
) {
    val bg = if (isPositive) NeonPalette.Green.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.08f)
    val border = if (isPositive) NeonPalette.Green.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.15f)
    val textColor = if (isPositive) NeonPalette.Green else Color.Gray
    val dotColor = if (isPositive) NeonPalette.Green else Color.Gray

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(text, style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = textColor)
    }
}
