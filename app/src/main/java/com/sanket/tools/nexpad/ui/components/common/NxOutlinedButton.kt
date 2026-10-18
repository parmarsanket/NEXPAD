package com.sanket.tools.nexpad.ui.components.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NxOutlinedButton(
    modifier: Modifier = Modifier,
    iconModifier: Modifier = Modifier,
    onClick: () -> Unit,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    borderColors: Color,
    imageVector: ImageVector? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
    enabled: Boolean = true,
    tint: Color? = null,
    text: String,
    space: Dp = 4.dp,
    style: TextStyle = LocalTextStyle.current,
) {
    OutlinedButton(
        onClick = onClick,
        contentPadding = contentPadding,
        colors = colors,
        border = BorderStroke(1.dp, borderColors),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier,
        enabled = enabled
    ) {
        if (imageVector != null) {
            Icon(
                imageVector = imageVector,
                contentDescription = null,
                modifier = iconModifier,
                tint = tint ?: LocalContentColor.current
            )
            Spacer(Modifier.width(space))
        }
        Text(
            text = text,
            style = style,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            softWrap = false
        )
    }
}

@Composable
fun NexPadOutlinedButton(
    text: String,
    icon: ImageVector,
    color: Color,
    isDragging: Boolean,
    isCompact: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    endPadding: Dp = 8.dp,
    horizontalPadding: Dp = 10.dp,
    fontSize: TextUnit = 12.sp
) {
    val activeAlpha = if (isDragging) 0.3f else 1f
    val borderAndTextAlpha = if (isDragging) 0.3f else 0.7f

    OutlinedButton(
        onClick = onClick,
        enabled = !isDragging,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = color,
            disabledContentColor = color.copy(alpha = 0.3f)
        ),
        border = BorderStroke(1.dp, color.copy(alpha = borderAndTextAlpha)),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = horizontalPadding, vertical = 4.dp),
        modifier = modifier
            .padding(end = endPadding)
            .height(36.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text, // Better accessibility than null
            tint = color.copy(alpha = activeAlpha),
            modifier = Modifier.size(15.dp)
        )

        // Only compose Spacer and Text when not in compact mode to prevent ghost spacing
        if (!isCompact) {
            Spacer(Modifier.width(4.dp))
            Text(
                text = text,
                color = color.copy(alpha = borderAndTextAlpha),
                fontWeight = FontWeight.Bold,
                fontSize = fontSize
            )
        }
    }
}