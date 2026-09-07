package com.sanket.tools.nexpad.ui.components.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.carousel.HorizontalCenteredHeroCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.sanket.tools.nexpad.ui.components.button.PlayButton
import com.sanket.tools.nexpad.ui.components.card.InnerLayoutCard
import com.sanket.tools.nexpad.ui.theme.NeonPalette

data class LayoutOption(
    val title: String,
    val subtitle: String
)

val defaultLayoutOptions = listOf(
    LayoutOption("Classic Pro", "Layout 1"),
    LayoutOption("FPS Master", "Layout 2"),
    LayoutOption("Racing Sim", "Layout 3"),
    LayoutOption("Advance 1", "Layout 4"),
    LayoutOption("Advance 2", "Layout 5"),
    LayoutOption("Advance 3", "Layout 6"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VShapedPanel(
    options: List<LayoutOption> = defaultLayoutOptions,
    onPlayClick: () -> Unit
) {
    var selectedIndex by remember { mutableIntStateOf(0) }
    val state = rememberCarouselState { options.size }

    LaunchedEffect(state.currentItem) {
        selectedIndex = state.currentItem
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.1f)
    ) {
        VPanelBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            HorizontalCenteredHeroCarousel(
                state = state,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(221.dp)
                    .padding(24.dp),
                itemSpacing = 8.dp,
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) { index ->
                val item = options[index]
                InnerLayoutCard(
                    modifier = Modifier.fillMaxSize().padding(4.dp),
                    title = item.title,
                    subtitle = item.subtitle,
                    isSelected = selectedIndex == index,
                )
            }
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                PlayButton(
                    modifier = Modifier
                        .align(alignment = Alignment.Center)
                        .padding(bottom = 24.dp),
                    onClick = onPlayClick
                )
            }
        }
    }
}

/** Draws the glowing V-shaped chassis behind the carousel. */
@Composable
private fun VPanelBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width, size.height * 0.75f)
            lineTo(size.width * 0.5f, size.height)
            lineTo(0f, size.height * 0.75f)
            close()
        }
        drawPath(
            path = path,
            brush = Brush.linearGradient(colors = listOf(NeonPalette.PanelBgTop, NeonPalette.PanelBgBottom))
        )
        drawPath(
            path = path,
            brush = Brush.linearGradient(
                colors = listOf(
                    NeonPalette.Cyan.copy(alpha = 0.3f),
                    NeonPalette.Purple.copy(alpha = 0.3f),
                    NeonPalette.Cyan.copy(alpha = 0.3f)
                )
            ),
            style = Stroke(width = 8.dp.toPx())
        )
        drawPath(
            path = path,
            brush = Brush.linearGradient(colors = listOf(NeonPalette.Cyan, NeonPalette.Purple, NeonPalette.Cyan)),
            style = Stroke(width = 2.dp.toPx())
        )
    }
}
