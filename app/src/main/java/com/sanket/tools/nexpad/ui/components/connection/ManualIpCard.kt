package com.sanket.tools.nexpad.ui.components.connection

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.ui.theme.NeonPalette

@Composable
fun ManualIpCard(
    manualIp: String,
    manualPort: String,
    onIpChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onConnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NeonPalette.DarkCard),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, NeonPalette.Cyan.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Direct IP Fallback",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = manualIp,
                    onValueChange = onIpChange,
                    label = { Text("PC IP Address") },
                    placeholder = { Text("192.168.1.10") },
                    modifier = Modifier.weight(2f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonPalette.Cyan,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                OutlinedTextField(
                    value = manualPort,
                    onValueChange = onPortChange,
                    label = { Text("Port") },
                    placeholder = { Text("9999") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonPalette.Cyan,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }
            OutlinedButton(
                onClick = onConnect,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = NeonPalette.Cyan.copy(alpha = 0.08f),
                    contentColor = NeonPalette.Cyan
                ),
                border = BorderStroke(1.5.dp, NeonPalette.Cyan.copy(alpha = 0.7f))
            ) {
                Text(
                    text = "CONNECT VIA DIRECT IP",
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    fontSize = 12.sp
                )
            }
        }
    }
}
