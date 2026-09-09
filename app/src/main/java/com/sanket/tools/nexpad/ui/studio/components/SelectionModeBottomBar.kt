package com.sanket.tools.nexpad.ui.studio.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.ui.theme.NeonPalette

/**
 * Sticky Bottom Confirmation Bar in Layout Selection Mode.
 */
@Composable
fun SelectionModeBottomBar(
    activeCount: Int,
    totalCount: Int,
    onProceedToHud: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFF070D18).copy(alpha = 0.96f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        shadowElevation = 16.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = NeonPalette.Cyan.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonPalette.Cyan)
                    ) {
                        Text(
                            "$activeCount Included",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonPalette.Cyan,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        "• ${totalCount - activeCount} omitted",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                Text(
                    "Only selected buttons will appear in this layout.",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onProceedToHud,
                colors = ButtonDefaults.buttonColors(containerColor = NeonPalette.Cyan),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Create Layout & Open HUD ➔", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 12.sp)
            }
        }
    }
}
