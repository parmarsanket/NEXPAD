package com.sanket.tools.nexpad.ui.components.connection

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Refresh
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.ui.theme.NeonPalette

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.text.style.TextOverflow
import com.sanket.tools.nexpad.ui.components.common.ConnectionSubTitle
import com.sanket.tools.nexpad.ui.components.common.NxDivider
import com.sanket.tools.nexpad.ui.components.common.NxIconBox
import com.sanket.tools.nexpad.ui.components.common.NxOutlinedButton
import com.sanket.tools.nexpad.ui.components.common.TitleColumn
import com.sanket.tools.nexpad.ui.layout.AdaptiveLayoutSpec

/**
 * Status overview card displaying the real-time state of physical hardware links (USB cable, AOA accessory, ADB server).
 */
@Composable
fun ConnectionHubStatusCard(
    isUsbCableConnected: Boolean,
    isAoaAttached: Boolean,
    isAdbAvailable: Boolean,
    onRescan: () -> Unit,
    onOpenBluetoothSettings: () -> Unit,
    modifier: Modifier = Modifier,
    layout: AdaptiveLayoutSpec? = null,
    aoaDeviceName: String? = null,
    adbServerPort: Int = 9999,
    adbServerName: String? = null
) {
    val isShort = layout?.isShortScreen == true
    val cardPadding = layout?.cardPadding ?: if (isShort) 12.dp else 16.dp
    val spacing = layout?.cardSpacing ?: if (isShort) 10.dp else 14.dp
    val iconBoxSize = layout?.iconBoxSize ?: 36.dp
    val iconSize = layout?.iconSize ?: 18.dp
    val titleFontSize = layout?.cardTitleFontSize ?: 13.5.sp
    val subtitleFontSize = layout?.subtitleFontSize ?: 11.sp
    val badgeFontSize = layout?.badgeFontSize ?: 10.5.sp
    val buttonHeight = layout?.buttonHeight ?: 40.dp
    val buttonFontSize = layout?.buttonFontSize ?: 12.sp

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = NeonPalette.CardSurface.copy(alpha = 0.85f),
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                listOf(
                    NeonPalette.Cyan.copy(alpha = 0.45f),
                    NeonPalette.Cyan.copy(alpha = 0.15f),
                    Color.White.copy(alpha = 0.04f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(cardPadding),
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NxIconBox(
                        imageVector = Icons.Rounded.Hub,
                        iconBoxSize =iconBoxSize,
                        iconSize = iconSize,
                        color = NeonPalette.Cyan,
                    )
                    TitleColumn(
                        title = "LINK HARDWARE STATUS",
                        subtitle = "Real-time transport detection",
                        titleFontSize = titleFontSize,
                        subtitleFontSize = subtitleFontSize
                    )

                }

                Spacer(Modifier.width(8.dp))

                val isAnyActive = isUsbCableConnected || isAoaAttached || isAdbAvailable
                val statusText = if (isAnyActive) "ACTIVE" else "STANDBY"
                val statusColor = if (isAnyActive) NeonPalette.Green else NeonPalette.Amber

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.45f))
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = badgeFontSize,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.5.dp)
                    )
                }
            }

            // Divider
            NxDivider(color = NeonPalette.Cyan)

            // Status indicator items
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusIndicatorRow(
                    label = "USB Cable",
                    status = if (isUsbCableConnected) "Plugged In" else "Disconnected",
                    isGood = isUsbCableConnected
                )
                StatusIndicatorRow(
                    label = "AOA Accessory",
                    status = if (isAoaAttached) (aoaDeviceName?.let { "Attached ($it)" } ?: "Attached") else "Ready",
                    isGood = isAoaAttached
                )
                StatusIndicatorRow(
                    label = "ADB Server",
                    status = if (isAdbAvailable) "${adbServerName ?: "Online"} (Port $adbServerPort)" else "Offline",
                    isGood = isAdbAvailable
                )
            }

            Spacer(Modifier.height(2.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                NxOutlinedButton(
                    onClick = onRescan,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = NeonPalette.Cyan,
                        containerColor = NeonPalette.Cyan.copy(alpha = 0.06f)
                    ),
                    borderColors = NeonPalette.Cyan.copy(alpha = 0.5f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    modifier = Modifier.weight(1f).height(buttonHeight),
                    imageVector = Icons.Rounded.Refresh,
                    iconModifier = Modifier.size(iconSize),
                    space = 6.dp,
                    text = "Rescan",
                    style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = buttonFontSize,
                        ),
                )
                NxOutlinedButton(
                    onClick = onOpenBluetoothSettings,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White,
                        containerColor = Color.White.copy(alpha = 0.04f)
                    ),
                    borderColors =  Color.White.copy(alpha = 0.25f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    modifier = Modifier.weight(1f).height(buttonHeight),
                    imageVector = Icons.Rounded.Bluetooth,
                    iconModifier = Modifier.size(iconSize),
                    space = 6.dp,
                    text = "BT Pair",
                    tint = NeonPalette.Purple,
                    style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = buttonFontSize
                        ),
                )


            }
        }
    }
}

@Composable
private fun StatusIndicatorRow(label: String, status: String, isGood: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(NeonPalette.CardRecessed.copy(alpha = 0.6f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isGood) NeonPalette.Green else NeonPalette.SlateMuted)
            )
            Text(
                label,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    color = NeonPalette.SlateLight
                )
            )
        }
        Text(
            status,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 11.5.sp,
                color = if (isGood) NeonPalette.Green else NeonPalette.SlateText
            )
        )
    }
}
