package com.sanket.tools.nexpad.ui.studio.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector

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

fun CategorySymbol.asImageVector(): ImageVector = when (this) {
    CategorySymbol.GAMEPAD -> Icons.Rounded.SportsEsports
    CategorySymbol.DPAD -> Icons.Rounded.ControlCamera
    CategorySymbol.STICK -> Icons.Rounded.Album
    CategorySymbol.TRIGGER -> Icons.Rounded.Tune
    CategorySymbol.BUMPER -> Icons.Rounded.HorizontalRule
    CategorySymbol.HOME -> Icons.Rounded.Home
    CategorySymbol.SYSTEM -> Icons.Rounded.Settings
    CategorySymbol.MACRO -> Icons.Rounded.Bolt
    CategorySymbol.ALL -> Icons.Rounded.Widgets
}

fun CategoryDefinition.toStudioCategory(): StudioCategory {
    val filters = if (isGroupCluster) {
        emptyList()
    } else {
        val allFilter = StudioSubFilter("ALL", "All $title", null)
        val controlFilters = controls.map { ctrl ->
            StudioSubFilter(
                id = ctrl.key,
                label = ctrl.label,
                targetKey = ctrl.key
            )
        }
        listOf(allFilter) + controlFilters
    }
    return StudioCategory(
        id = id,
        title = title,
        icon = symbol.asImageVector(),
        keys = keys,
        subFilters = filters
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
