package com.sanket.tools.nexpad.ui.components.common

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

@Composable
fun ConnectionTitle(
    modifier: Modifier = Modifier,
    title: String,
    titleFontSize : TextUnit
) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.Black,
            fontSize = titleFontSize,
            letterSpacing = 0.5.sp
        ),
        modifier = modifier,
        color = Color.White,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun ConnectionSubTitle(
    modifier: Modifier = Modifier,
    subtitle: String,
    maxLines: Int = 1,
    subtitleFontSize : TextUnit
) {
    Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall.copy(
            fontSize = subtitleFontSize
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = maxLines,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
fun TitleColumn(
    modifier: Modifier = Modifier,
    title: String,
    titleFontSize : TextUnit,
    subtitle: String,
    subtitleFontSize : TextUnit
){
    Column(modifier = modifier) {
        ConnectionTitle(
            title = title,
            titleFontSize = titleFontSize
        )
        ConnectionSubTitle(
            subtitle = subtitle,
            subtitleFontSize = subtitleFontSize
        )
    }
}
