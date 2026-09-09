package com.sanket.tools.nexpad.ui.studio.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.ui.studio.model.ButtonStudioMode
import com.sanket.tools.nexpad.ui.studio.model.StudioCategory
import com.sanket.tools.nexpad.ui.theme.NeonPalette

/**
 * Vertical Navigation Rail / Sidebar on the left of Button Studio.
 * Organizes categories vertically with icon, title, and selection count indicator.
 */
@Composable
fun StudioVerticalRail(
    categories: List<StudioCategory>,
    selectedCategoryId: String,
    onSelectCategory: (StudioCategory) -> Unit,
    mode: ButtonStudioMode,
    activeCountForCategory: (StudioCategory) -> Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color(0xFF070D18),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            categories.forEach { cat ->
                val isSelected = cat.id == selectedCategoryId
                val activeCount = activeCountForCategory(cat)

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelectCategory(cat) },
                    shape = RoundedCornerShape(10.dp),
                    color = when {
                        isSelected -> NeonPalette.Cyan.copy(alpha = 0.18f)
                        else -> Color.Transparent
                    },
                    border = if (isSelected) {
                        androidx.compose.foundation.BorderStroke(1.dp, NeonPalette.Cyan.copy(alpha = 0.8f))
                    } else null
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Box(contentAlignment = Alignment.TopEnd) {
                            Icon(
                                cat.icon,
                                contentDescription = cat.title,
                                tint = if (isSelected) NeonPalette.Cyan else Color.LightGray.copy(alpha = 0.7f),
                                modifier = Modifier.size(22.dp)
                            )
                            // Count badge in Selection mode
                            if (mode == ButtonStudioMode.SELECTION && activeCount > 0 && cat.keys.isNotEmpty()) {
                                Surface(
                                    shape = CircleShape,
                                    color = NeonPalette.Cyan,
                                    modifier = Modifier.offset(x = 6.dp, y = (-4).dp)
                                ) {
                                    Text(
                                        text = "$activeCount",
                                        color = Color.Black,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 3.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = cat.title,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Color.Gray,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
