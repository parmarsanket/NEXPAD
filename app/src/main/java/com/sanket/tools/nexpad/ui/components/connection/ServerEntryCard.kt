package com.sanket.tools.nexpad.ui.components.connection

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.network.DiscoveredServer
import com.sanket.tools.nexpad.ui.components.badge.StatusBadgePill
import com.sanket.tools.nexpad.ui.layout.AdaptiveLayoutSpec
import com.sanket.tools.nexpad.ui.theme.NeonPalette

@Composable
fun ServerEntryCard(
    server: DiscoveredServer,
    isTethering: Boolean,
    onConnect: () -> Unit,
    modifier: Modifier = Modifier,
    layout: AdaptiveLayoutSpec? = null
) {
    val accentColor = if (isTethering) NeonPalette.Green else NeonPalette.Cyan
    val cardPadding = layout?.cardPadding ?: 16.dp
    val spacing = layout?.cardSpacing ?: 14.dp
    val iconBoxSize = layout?.iconBoxSize ?: 42.dp
    val iconSize = layout?.iconSize ?: 22.dp
    val titleFontSize = layout?.cardTitleFontSize ?: 16.sp
    val subtitleFontSize = layout?.subtitleFontSize ?: 11.5.sp
    val buttonHeight = layout?.buttonHeight ?: 44.dp
    val buttonFontSize = layout?.buttonFontSize ?: 12.sp

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = NeonPalette.CardSurface.copy(alpha = 0.85f),
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                listOf(
                    accentColor.copy(alpha = 0.45f),
                    accentColor.copy(alpha = 0.15f),
                    Color.White.copy(alpha = 0.04f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(cardPadding),
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(iconBoxSize)
                            .clip(RoundedCornerShape(12.dp))
                            .background(accentColor.copy(alpha = 0.12f))
                            .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isTethering) Icons.Rounded.Link else Icons.Rounded.Computer,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(iconSize)
                        )
                    }
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = server.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = titleFontSize
                            ),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${server.ipAddress}:${server.port}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = subtitleFontSize,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                StatusBadgePill(
                    text = if (isTethering) "USB Tether" else "Wi-Fi LAN",
                    isPositive = true
                )
            }

            OutlinedButton(
                onClick = onConnect,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(buttonHeight),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = accentColor.copy(alpha = 0.08f),
                    contentColor = accentColor
                ),
                border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.7f))
            ) {
                Text(
                    text = if (isTethering) "CONNECT (USB TETHERING)" else "CONNECT (WI-FI)",
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    fontSize = buttonFontSize
                )
            }
        }
    }
}
