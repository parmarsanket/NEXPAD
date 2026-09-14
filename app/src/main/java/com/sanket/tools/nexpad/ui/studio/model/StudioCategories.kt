package com.sanket.tools.nexpad.ui.studio.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

import com.sanket.tools.nexpad.category.CategoryDefinition
import com.sanket.tools.nexpad.category.CategoryManager
import com.sanket.tools.nexpad.category.CategorySymbol

data class StudioSubFilter(
    val id: String,
    val label: String,
    val targetKey: String?
)

data class StudioCategory(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val keys: Set<String>,
    val subFilters: List<StudioSubFilter>
)

private val iconCache = mutableMapOf<CategorySymbol, ImageVector>()

/**
 * Builds a native Compose ImageVector directly from CategorySymbol's SVG path data.
 * Completely self-contained — zero external icon library dependencies!
 */
fun CategorySymbol.asImageVector(fillColor: Color = Color.White): ImageVector {
    return iconCache.getOrPut(this) {
        val nodes = PathParser().parsePathString(svgPath).toNodes()
        ImageVector.Builder(
            name = iconName,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).addPath(
            pathData = nodes,
            fill = SolidColor(fillColor)
        ).build()
    }
}

fun CategoryDefinition.toStudioCategory(): StudioCategory {
    val filterList = mutableListOf<StudioSubFilter>()
    filterList.add(StudioSubFilter("ALL", "All $title", null))
    controls.forEach { ctrl ->
        filterList.add(
            StudioSubFilter(
                id = ctrl.key,
                label = if (ctrl.emoji.isNotBlank()) "${ctrl.emoji} ${ctrl.label}" else ctrl.label,
                targetKey = ctrl.key
            )
        )
    }
    return StudioCategory(
        id = id,
        title = title,
        icon = symbol.asImageVector(),
        keys = keys,
        subFilters = filterList
    )
}

val STUDIO_CATEGORIES: List<StudioCategory> = CategoryManager.getAllCategories().map { it.toStudioCategory() } + listOf(
    StudioCategory(
        id = "ALL",
        title = "All",
        icon = CategorySymbol.ALL.asImageVector(),
        keys = emptySet(),
        subFilters = emptyList()
    )
)
