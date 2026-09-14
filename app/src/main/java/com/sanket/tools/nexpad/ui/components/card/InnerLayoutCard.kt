package com.sanket.tools.nexpad.ui.components.card

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.ui.theme.NeonPalette

private val cardAnimSpecDp = tween<Dp>(durationMillis = 280, easing = FastOutSlowInEasing)
private val cardAnimSpecFloat = tween<Float>(durationMillis = 280, easing = FastOutSlowInEasing)
private val cardAnimSpecColor = tween<Color>(durationMillis = 280, easing = FastOutSlowInEasing)

@Composable
fun InnerLayoutCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    isDefault: Boolean = true
) {
    val width by animateDpAsState(
        targetValue = if (isSelected) 140.dp else 100.dp,
        animationSpec = cardAnimSpecDp,
        label = "cardWidth"
    )
    val height by animateDpAsState(
        targetValue = if (isSelected) 160.dp else 120.dp,
        animationSpec = cardAnimSpecDp,
        label = "cardHeight"
    )
    val borderWidth by animateDpAsState(
        targetValue = 2.dp,
        animationSpec = cardAnimSpecDp,
        label = "borderWidth"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) NeonPalette.Cyan else NeonPalette.CardIdleBorder,
        animationSpec = cardAnimSpecColor,
        label = "borderColor"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else NeonPalette.CardIdleText,
        animationSpec = cardAnimSpecColor,
        label = "textColor"
    )
    val bgStart by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF005577) else NeonPalette.CardIdleBg,
        animationSpec = cardAnimSpecColor,
        label = "bgStart"
    )
    val bgEnd by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF660088) else NeonPalette.CardIdleBg,
        animationSpec = cardAnimSpecColor,
        label = "bgEnd"
    )
    val titleFontSize by animateFloatAsState(
        targetValue = if (isSelected) 16f else 12f,
        animationSpec = cardAnimSpecFloat,
        label = "titleFontSize"
    )
    val subtitleAlpha by animateFloatAsState(
        targetValue = if (isSelected) 0.9f else 0f,
        animationSpec = cardAnimSpecFloat,
        label = "subtitleAlpha"
    )

    Box(
        modifier = modifier
            .size(width, height)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(bgStart, bgEnd)))
            .border(borderWidth, borderColor, RoundedCornerShape(16.dp))
            .semantics { contentDescription = "$title layout${if (isSelected) ", selected" else ""}" },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(
                text = title,
                color = textColor,
                fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                fontSize = titleFontSize.sp,
                style = if (isSelected) {
                    TextStyle(shadow = Shadow(color = Color.Black, offset = Offset(0f, 4f), blurRadius = 8f))
                } else {
                    TextStyle.Default
                }
            )
            Text(
                subtitle,
                color = textColor.copy(alpha = subtitleAlpha),
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
