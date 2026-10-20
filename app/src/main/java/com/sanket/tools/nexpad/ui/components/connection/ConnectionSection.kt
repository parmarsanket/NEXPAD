package com.sanket.tools.nexpad.ui.components.connection

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.ui.components.common.ConnectionTitle
import com.sanket.tools.nexpad.ui.components.common.NxDivider
import com.sanket.tools.nexpad.ui.components.common.NxIconBox
import com.sanket.tools.nexpad.ui.components.common.TitleColumn
import com.sanket.tools.nexpad.ui.layout.AdaptiveLayoutSpec
import com.sanket.tools.nexpad.ui.theme.NeonPalette

@Composable
fun ConnectionSection(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    accentColor: Color = NeonPalette.Cyan,
    layout: AdaptiveLayoutSpec? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val iconBoxSize = layout?.iconBoxSize ?: 34.dp
    val iconSize = layout?.iconSize ?: 18.dp
    val titleFontSize = layout?.sectionTitleFontSize ?: 13.5.sp
    val subtitleFontSize = layout?.subtitleFontSize ?: 11.sp
    val spacing = layout?.cardSpacing ?: 12.dp

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            NxIconBox(
                imageVector = icon,
                iconBoxSize =iconBoxSize,
                iconSize = iconSize,
                color = accentColor,
            )
            TitleColumn(
                modifier = Modifier.weight(1f),
                title = title,
                titleFontSize = titleFontSize,
                subtitle = subtitle,
                subtitleFontSize = subtitleFontSize
            )
        }

        NxDivider(color = accentColor)

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}
