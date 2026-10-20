package com.sanket.tools.nexpad.ui.components.connection

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Lan
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.ui.components.common.NxIconBox
import com.sanket.tools.nexpad.ui.components.common.NxOutlinedButton
import com.sanket.tools.nexpad.ui.layout.AdaptiveLayoutSpec
import com.sanket.tools.nexpad.ui.theme.NeonPalette
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ManualIpCard(
    manualIp: String,
    manualPort: String,
    onIpChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onConnect: () -> Unit,
    modifier: Modifier = Modifier,
    layout: AdaptiveLayoutSpec? = null
) {
    val cardPadding = layout?.cardPadding ?: 16.dp
    val spacing = layout?.cardSpacing ?: 14.dp
    val iconBoxSize = layout?.iconBoxSize ?: 42.dp
    val iconSize = layout?.iconSize ?: 22.dp
    val titleFontSize = layout?.cardTitleFontSize ?: 15.sp
    val bodyFontSize = layout?.bodyFontSize ?: 11.5.sp
    val buttonHeight = layout?.buttonHeight ?: 46.dp
    val buttonFontSize = layout?.buttonFontSize ?: 12.sp

    val focusManager = LocalFocusManager.current
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoViewRequester),
        shape = RoundedCornerShape(16.dp),
        color = NeonPalette.CardSurface.copy(alpha = 0.85f),
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                listOf(
                    NeonPalette.Cyan.copy(alpha = 0.40f),
                    NeonPalette.Purple.copy(alpha = 0.20f),
                    Color.White.copy(alpha = 0.05f)
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
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NxIconBox(
                    iconBoxSize = iconBoxSize,
                    iconSize = iconSize,
                    imageVector = Icons.Rounded.Lan,
                    color = NeonPalette.Cyan
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Direct IP / Port Connection",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = titleFontSize,
                            letterSpacing = 0.5.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Freely change PC IP & port when network broadcast discovery is blocked",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = NeonPalette.SlateText,
                            fontSize = bodyFontSize
                        )
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = manualIp,
                    onValueChange = onIpChange,
                    label = { Text("PC IP Address", color = NeonPalette.SlateText) },
                    placeholder = { Text("192.168.1.10", color = NeonPalette.SlateMuted) },
                    modifier = Modifier
                        .weight(2.2f)
                        .onFocusEvent { focusState ->
                            if (focusState.isFocused) {
                                coroutineScope.launch {
                                    bringIntoViewRequester.bringIntoView()
                                }
                            }
                        },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            focusManager.moveFocus(FocusDirection.Next)
                            coroutineScope.launch {
                                bringIntoViewRequester.bringIntoView()
                            }
                        }
                    ),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonPalette.Cyan,
                        unfocusedBorderColor = NeonPalette.SlateBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = NeonPalette.FieldBg,
                        unfocusedContainerColor = NeonPalette.FieldBg
                    )
                )

                OutlinedTextField(
                    value = manualPort,
                    onValueChange = onPortChange,
                    label = { Text("Port", color = NeonPalette.SlateText) },
                    placeholder = { Text("9999", color = NeonPalette.SlateMuted) },
                    modifier = Modifier
                        .weight(1f)
                        .onFocusEvent { focusState ->
                            if (focusState.isFocused) {
                                coroutineScope.launch {
                                    bringIntoViewRequester.bringIntoView()
                                }
                            }
                        },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            if (manualIp.isNotBlank()) {
                                onConnect()
                            }
                        }
                    ),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonPalette.Cyan,
                        unfocusedBorderColor = NeonPalette.SlateBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = NeonPalette.FieldBg,
                        unfocusedContainerColor = NeonPalette.FieldBg
                    )
                )
            }

            val isIpValid = manualIp.isNotBlank()
            NxOutlinedButton(
                onClick = {
                    focusManager.clearFocus()
                    onConnect()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(buttonHeight),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isIpValid) NeonPalette.Cyan.copy(alpha = 0.12f) else NeonPalette.DisabledBg,
                    contentColor = if (isIpValid) NeonPalette.Cyan else NeonPalette.SlateDisabled,
                    disabledContainerColor = NeonPalette.DisabledBg,
                    disabledContentColor = NeonPalette.SlateDisabled
                ),
                borderColors = if (isIpValid) NeonPalette.Cyan.copy(alpha = 0.7f) else NeonPalette.SlateBorder.copy(alpha = 0.5f),
                imageVector = Icons.Rounded.Bolt,
                iconModifier = Modifier.size(iconSize),
                space = 6.dp,
                enabled = isIpValid,
                text = "CONNECT VIA DIRECT IP",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    fontSize = buttonFontSize
                ),
            )
        }
    }
}
