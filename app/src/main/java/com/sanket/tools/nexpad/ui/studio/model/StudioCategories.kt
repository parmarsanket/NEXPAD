package com.sanket.tools.nexpad.ui.studio.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector

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

val STUDIO_CATEGORIES = listOf(
    StudioCategory(
        id = "ABXY",
        title = "ABXY",
        icon = Icons.Rounded.SportsEsports,
        keys = setOf("A", "B", "X", "Y"),
        subFilters = emptyList() // Group-only: 4-Button Cluster
    ),
    StudioCategory(
        id = "DPAD",
        title = "D-Pad",
        icon = Icons.Rounded.ControlCamera,
        keys = setOf("DPAD", "UP", "DOWN", "LEFT", "RIGHT"),
        subFilters = listOf(
            StudioSubFilter("ALL", "All D-Pad", null),
            StudioSubFilter("CROSS", "Cross Pad", "DPAD"),
            StudioSubFilter("UP", "D-Pad Up", "UP"),
            StudioSubFilter("DOWN", "D-Pad Down", "DOWN"),
            StudioSubFilter("LEFT", "D-Pad Left", "LEFT"),
            StudioSubFilter("RIGHT", "D-Pad Right", "RIGHT")
        )
    ),
    StudioCategory(
        id = "STICKS",
        title = "Sticks",
        icon = Icons.Rounded.Album,
        keys = setOf("LS", "RS"),
        subFilters = emptyList() // Group-only: Left & Right Pair
    ),
    StudioCategory(
        id = "TRIGGERS",
        title = "Triggers",
        icon = Icons.Rounded.Tune,
        keys = setOf("LT", "RT"),
        subFilters = emptyList() // Group-only: Left & Right Pair
    ),
    StudioCategory(
        id = "BUMPERS",
        title = "Bumpers",
        icon = Icons.Rounded.HorizontalRule,
        keys = setOf("LB", "RB"),
        subFilters = emptyList() // Group-only: Left & Right Pair
    ),
    StudioCategory(
        id = "HOME",
        title = "Home",
        icon = Icons.Rounded.Home,
        keys = setOf("XBOX", "HOME", "GUIDE"),
        subFilters = listOf(
            StudioSubFilter("ALL", "Xbox Guide", "XBOX")
        )
    ),
    StudioCategory(
        id = "SYSTEM",
        title = "System",
        icon = Icons.Rounded.Settings,
        keys = setOf("VIEW", "MENU", "SHARE", "BACK", "START"),
        subFilters = listOf(
            StudioSubFilter("ALL", "All System", null),
            StudioSubFilter("VIEW", "View / Back", "VIEW"),
            StudioSubFilter("MENU", "Menu / Pause", "MENU"),
            StudioSubFilter("SHARE", "Share / Capture", "SHARE")
        )
    ),
    StudioCategory(
        id = "MACROS",
        title = "Macros",
        icon = Icons.Rounded.Bolt,
        keys = setOf("M1", "M2", "M3", "M4"),
        subFilters = listOf(
            StudioSubFilter("ALL", "All Macros", null),
            StudioSubFilter("M1", "Paddle M1", "M1"),
            StudioSubFilter("M2", "Paddle M2", "M2"),
            StudioSubFilter("M3", "Paddle M3", "M3"),
            StudioSubFilter("M4", "Paddle M4", "M4")
        )
    ),
    StudioCategory(
        id = "ALL",
        title = "All",
        icon = Icons.Rounded.Widgets,
        keys = emptySet(),
        subFilters = emptyList()
    )
)
