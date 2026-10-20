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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.ui.layout.AdaptiveDeviceType
import com.sanket.tools.nexpad.ui.layout.AdaptiveLayoutSpec
import com.sanket.tools.nexpad.ui.theme.NeonPalette

private val cardAnimSpecDp = tween<Dp>(durationMillis = 280, easing = FastOutSlowInEasing)
private val cardAnimSpecFloat = tween<Float>(durationMillis = 280, easing = FastOutSlowInEasing)
private val cardAnimSpecColor = tween<Color>(durationMillis = 280, easing = FastOutSlowInEasing)

@Composable
fun InnerLayoutCard(
    modifier: Modifier = Modifier,
    layout: AdaptiveLayoutSpec,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    isDefault: Boolean = true
) {
    val targetWidth = when (layout.deviceType) {
        AdaptiveDeviceType.Tablet -> if (isSelected) 220.dp else 165.dp
        AdaptiveDeviceType.Foldable -> if (isSelected) 180.dp else 135.dp
        AdaptiveDeviceType.MidPhone -> if (isSelected) 152.dp else 114.dp
        AdaptiveDeviceType.SmallPhone -> if (isSelected) 130.dp else 96.dp
    }
    val targetHeight = when (layout.deviceType) {
        AdaptiveDeviceType.Tablet -> if (isSelected) 255.dp else 190.dp
        AdaptiveDeviceType.Foldable -> if (isSelected) 210.dp else 158.dp
        AdaptiveDeviceType.MidPhone -> if (isSelected) 176.dp else 132.dp
        AdaptiveDeviceType.SmallPhone -> if (isSelected) 148.dp else 110.dp
    }
    val targetTitleFontSize = when (layout.deviceType) {
        AdaptiveDeviceType.Tablet -> if (isSelected) 24f else 17f
        AdaptiveDeviceType.Foldable -> if (isSelected) 20f else 15f
        AdaptiveDeviceType.MidPhone -> if (isSelected) 17.5f else 13.5f
        AdaptiveDeviceType.SmallPhone -> if (isSelected) 15f else 11.5f
    }
    val subtitleFontSize = when (layout.deviceType) {
        AdaptiveDeviceType.Tablet -> 14.sp
        AdaptiveDeviceType.Foldable -> 12.5.sp
        AdaptiveDeviceType.MidPhone -> 11.5.sp
        AdaptiveDeviceType.SmallPhone -> 10.5.sp
    }

    val width by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = cardAnimSpecDp,
        label = "cardWidth"
    )
    val height by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = cardAnimSpecDp,
        label = "cardHeight"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (layout.isTablet) 2.5.dp else 2.dp,
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
        targetValue = if (isSelected) NeonPalette.SelectedGradStart else NeonPalette.CardIdleBg,
        animationSpec = cardAnimSpecColor,
        label = "bgStart"
    )
    val bgEnd by animateColorAsState(
        targetValue = if (isSelected) NeonPalette.SelectedGradEnd else NeonPalette.CardIdleBg,
        animationSpec = cardAnimSpecColor,
        label = "bgEnd"
    )
    val titleFontSize by animateFloatAsState(
        targetValue = targetTitleFontSize,
        animationSpec = cardAnimSpecFloat,
        label = "titleFontSize"
    )
    val subtitleAlpha by animateFloatAsState(
        targetValue = if (isSelected) 0.9f else 0f,
        animationSpec = cardAnimSpecFloat,
        label = "subtitleAlpha"
    )

    val cardCornerRadius = if (layout.isTablet) 22.dp else 16.dp

    Box(
        modifier = modifier
            .size(width, height)
            .clip(RoundedCornerShape(cardCornerRadius))
            .background(Brush.linearGradient(listOf(bgStart, bgEnd)))
            .border(borderWidth, borderColor, RoundedCornerShape(cardCornerRadius))
            .semantics { contentDescription = "$title layout${if (isSelected) ", selected" else ""}" },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(
                text = title,
                modifier = Modifier.padding(horizontal = (layout.horizontalPadding / 2).coerceAtLeast(8.dp)),
                color = textColor,
                fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                fontSize = titleFontSize.sp,
                textAlign = TextAlign.Center,
                style = if (isSelected) {
                    TextStyle(shadow = Shadow(color = Color.Black, offset = Offset(0f, 4f), blurRadius = 8f))
                } else {
                    TextStyle.Default
                }
            )
            Text(
                subtitle,
                color = textColor.copy(alpha = subtitleAlpha),
                fontSize = subtitleFontSize,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
