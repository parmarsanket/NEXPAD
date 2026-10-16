package com.sanket.tools.nexpad.ui.components.connection

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cable
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Usb
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.ui.components.badge.StatusBadgePill
import com.sanket.tools.nexpad.ui.components.common.ConnectionSubTitle
import com.sanket.tools.nexpad.ui.components.common.ConnectionTitle
import com.sanket.tools.nexpad.ui.components.common.NxIconBox
import com.sanket.tools.nexpad.ui.components.common.NxOutlinedButton
import com.sanket.tools.nexpad.ui.components.common.TitleColumn
import com.sanket.tools.nexpad.ui.layout.AdaptiveLayoutSpec
import com.sanket.tools.nexpad.ui.theme.NeonPalette

@Composable
fun UsbAoaCard(
    isAoaAttached: Boolean,
    isUsbCableConnected: Boolean,
    onConnectAoa: () -> Unit,
    modifier: Modifier = Modifier,
    layout: AdaptiveLayoutSpec? = null
) {
    val isAoaReady = isAoaAttached && isUsbCableConnected
    val accentColor = if (isAoaReady) NeonPalette.Cyan else Color.White
    val cardPadding = layout?.cardPadding ?: 16.dp
    val spacing = layout?.cardSpacing ?: 12.dp
    val iconBoxSize = layout?.iconBoxSize ?: 42.dp
    val iconSize = layout?.iconSize ?: 22.dp
    val titleFontSize = layout?.cardTitleFontSize ?: 14.sp
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
                    accentColor.copy(alpha = if (isAoaReady) 0.5f else 0.12f),
                    accentColor.copy(alpha = if (isAoaReady) 0.18f else 0.05f),
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
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NxIconBox(
                        imageVector = Icons.Rounded.Cable,
                        iconBoxSize =iconBoxSize,
                        iconSize = iconSize,
                        color = NeonPalette.Cyan,
                    )
                    TitleColumn(
                        modifier = Modifier.weight(1f),
                        title = "USB KERNEL DIRECT (AOA 2.0)",
                        titleFontSize = titleFontSize,
                        subtitle ="Sub-millisecond driverless hardware pipe",
                        subtitleFontSize = subtitleFontSize
                    )
                }
                StatusBadgePill(
                    text = when {
                        isAoaReady -> "Accessory Ready"
                        isUsbCableConnected -> "Waiting Desktop"
                        else -> "No USB Wire"
                    },
                    isPositive = isAoaReady
                )
            }

            if (isAoaReady) {
                NxOutlinedButton(
                    onClick = onConnectAoa,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(buttonHeight),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = NeonPalette.Cyan.copy(alpha = 0.08f),
                        contentColor = NeonPalette.Cyan
                    ),
                    borderColors = NeonPalette.Cyan,
                    text =  "CONNECT (USB AOA)",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        fontSize = buttonFontSize
                    )
                )
            } else {
                ConnectionSubTitle(
                    subtitle = if (!isUsbCableConnected) "Connect phone to PC via USB cable." else "Plug phone into PC via USB. When NEXPAD Desktop sends the AOA handshake, this option lights up.",
                    subtitleFontSize = subtitleFontSize
                )
            }
        }
    }
}

@Composable
fun UsbAdbCard(
    isUsbCableConnected: Boolean,
    isAdbOn: Boolean,
    isAdbAvailable: Boolean,
    adbServerName: String?,
    onConnectAdb: () -> Unit,
    onOpenDeveloperSettings: () -> Unit,
    modifier: Modifier = Modifier,
    layout: AdaptiveLayoutSpec? = null
) {
    val accentColor = if (isAdbAvailable) NeonPalette.Green else Color.White
    val cardPadding = layout?.cardPadding ?: 16.dp
    val spacing = layout?.cardSpacing ?: 12.dp
    val iconBoxSize = layout?.iconBoxSize ?: 42.dp
    val iconSize = layout?.iconSize ?: 22.dp
    val titleFontSize = layout?.cardTitleFontSize ?: 14.sp
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
                    accentColor.copy(alpha = if (isAdbAvailable) 0.5f else 0.12f),
                    accentColor.copy(alpha = if (isAdbAvailable) 0.18f else 0.05f),
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
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NxIconBox(
                        imageVector = Icons.Rounded.Usb,
                        iconBoxSize =iconBoxSize,
                        iconSize = iconSize,
                        color = NeonPalette.Green,
                    )
                    TitleColumn(
                        modifier = Modifier.weight(1f),
                        title = "USB ADB PORT BRIDGE",
                        titleFontSize = titleFontSize,
                        subtitle = if (isAdbAvailable) "${adbServerName ?: "Desktop PC"} • Port 9999" else "Port forwarding (127.0.0.1:9999)",
                        subtitleFontSize = subtitleFontSize
                    )
//
                }
                StatusBadgePill(
                    text = when {
                        !isUsbCableConnected -> "No USB Wire"
                        !isAdbOn -> "Debugging OFF"
                        isAdbAvailable -> "Ready (${adbServerName ?: "PC"})"
                        else -> "Waiting Bridge"
                    },
                    isPositive = isAdbAvailable
                )
            }

            when {
                !isUsbCableConnected -> {
//
                    ConnectionSubTitle(
                        subtitle = "Connect phone to PC via USB cable to enable ADB port forwarding.",
                        subtitleFontSize = subtitleFontSize
                    )
                }
                !isAdbOn -> {
                    NxOutlinedButton(
                        onClick = onOpenDeveloperSettings,
                        modifier = Modifier.fillMaxWidth().height(buttonHeight),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = NeonPalette.Amber,
                            containerColor = NeonPalette.Amber.copy(alpha = 0.08f)
                        ),
                        borderColors = NeonPalette.Amber,
                        text = "Enable USB Debugging in Settings",
                        style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = buttonFontSize
                            ),
                    )
                }
                isAdbAvailable -> {
                    NxOutlinedButton(
                        onClick = onConnectAdb,
                        modifier = Modifier.fillMaxWidth().height(buttonHeight),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = NeonPalette.Green.copy(alpha = 0.08f),
                            contentColor = NeonPalette.Green
                        ),
                        borderColors = NeonPalette.Green.copy(alpha = 0.7f),
                        text = "CONNECT (USB ADB)",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = buttonFontSize,
                            letterSpacing = 0.8.sp
                        ),
                    )
                }
                else -> {
                    ConnectionSubTitle(
                        subtitle = "USB cable connected & debugging active. Waiting for NEXPAD Desktop to start ADB bridge...",
                        subtitleFontSize = subtitleFontSize
                    )
                }
            }
        }
    }
}

@Composable
fun UsbTetheringCard(
    onOpenTetheringSettings: () -> Unit,
    modifier: Modifier = Modifier,
    layout: AdaptiveLayoutSpec? = null
) {
    val cardPadding = layout?.cardPadding ?: 16.dp
    val iconBoxSize = layout?.iconBoxSize ?: 42.dp
    val iconSize = layout?.iconSize ?: 22.dp
    val titleFontSize = layout?.cardTitleFontSize ?: 14.sp
    val subtitleFontSize = layout?.subtitleFontSize ?: 11.5.sp
    val buttonHeight = layout?.buttonHeight ?: 36.dp
    val buttonFontSize = layout?.buttonFontSize ?: 12.sp

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = NeonPalette.CardSurface.copy(alpha = 0.85f),
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                listOf(
                    NeonPalette.Purple.copy(alpha = 0.35f),
                    NeonPalette.Purple.copy(alpha = 0.12f),
                    Color.White.copy(alpha = 0.04f)
                )
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(cardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                NxIconBox(
                    imageVector = Icons.Rounded.Link,
                    iconBoxSize =iconBoxSize,
                    iconSize = iconSize,
                    color = NeonPalette.Purple,
                )
                TitleColumn(
                    modifier = Modifier.weight(1f),
                    title = "USB TETHERING ADAPTER",
                    titleFontSize = titleFontSize,
                    subtitle = "Private low-jitter 100Mbps virtual LAN",
                    subtitleFontSize = subtitleFontSize
                )
            }
            Spacer(Modifier.width(8.dp))

            NxOutlinedButton(
                onClick = onOpenTetheringSettings,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White,
                    containerColor = Color.White.copy(alpha = 0.04f)
                ),
                borderColors = Color.White.copy(alpha = 0.25f),
                modifier = Modifier.height(buttonHeight),
                text = "Settings",
                style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = buttonFontSize
                    ),
            )
        }
    }
}
