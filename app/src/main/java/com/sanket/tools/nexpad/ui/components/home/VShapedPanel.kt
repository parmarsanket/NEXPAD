package com.sanket.tools.nexpad.ui.components.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.sanket.tools.nexpad.ui.components.button.PlayButton
import com.sanket.tools.nexpad.ui.components.card.InnerLayoutCard
import com.sanket.tools.nexpad.ui.theme.NeonPalette

import com.sanket.tools.nexpad.model.LayoutProfile
import com.sanket.tools.nexpad.model.getDefaultLayoutProfiles
import com.sanket.tools.nexpad.ui.layout.AdaptiveLayoutSpec

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VShapedPanel(
    layout: AdaptiveLayoutSpec,
    profiles: List<LayoutProfile> = emptyList(),
    activeProfileName: String = "",
    onProfileSelected: (LayoutProfile) -> Unit = {},
    isCompact: Boolean = false,
    modifier: Modifier = Modifier,
    onPlayClick: (LayoutProfile) -> Unit = {}
) {
    val effectiveProfiles = remember(profiles) {
        if (profiles.isNotEmpty()) profiles else getDefaultLayoutProfiles()
    }
    val targetIndex = remember(effectiveProfiles, activeProfileName) {
        val idx = effectiveProfiles.indexOfFirst { it.name.equals(activeProfileName, ignoreCase = true) }
        if (idx >= 0) idx else 0
    }
    val state = rememberCarouselState(initialItem = targetIndex) { effectiveProfiles.size }
    val coroutineScope = rememberCoroutineScope()

    var isUserGesture by remember { mutableStateOf(false) }
    var isProgrammaticScroll by remember { mutableStateOf(false) }

    // Synchronize external changes (e.g. from VirtualControllerScreen) into carousel
    LaunchedEffect(targetIndex) {
        if (effectiveProfiles.isNotEmpty() && state.currentItem != targetIndex) {
            isProgrammaticScroll = true
            try {
                state.scrollToItem(targetIndex)
            } finally {
                isProgrammaticScroll = false
            }
        }
    }

    // Flag when scroll is from user gesture
    LaunchedEffect(state.isScrollInProgress) {
        if (state.isScrollInProgress && !isProgrammaticScroll) {
            isUserGesture = true
        }
    }

    // Emit selection only when user gestured and scroll finished
    LaunchedEffect(state.currentItem, state.isScrollInProgress) {
        if (!state.isScrollInProgress && isUserGesture && effectiveProfiles.isNotEmpty()) {
            isUserGesture = false
            val validIdx = state.currentItem.coerceIn(0, effectiveProfiles.lastIndex)
            val selected = effectiveProfiles[validIdx]
            if (!selected.name.equals(activeProfileName, ignoreCase = true)) {
                onProfileSelected(selected)
            }
        }
    }

    val carouselHeight = when (layout.deviceType) {
        com.sanket.tools.nexpad.ui.layout.AdaptiveDeviceType.Tablet -> if (isCompact) 200.dp else 295.dp
        com.sanket.tools.nexpad.ui.layout.AdaptiveDeviceType.Foldable -> if (isCompact) 170.dp else 250.dp
        com.sanket.tools.nexpad.ui.layout.AdaptiveDeviceType.MidPhone -> if (isCompact) 145.dp else 225.dp
        com.sanket.tools.nexpad.ui.layout.AdaptiveDeviceType.SmallPhone -> if (isCompact) 130.dp else 195.dp
    }

    val playButtonSize = when (layout.deviceType) {
        com.sanket.tools.nexpad.ui.layout.AdaptiveDeviceType.Tablet -> 100.dp
        com.sanket.tools.nexpad.ui.layout.AdaptiveDeviceType.Foldable -> 90.dp
        com.sanket.tools.nexpad.ui.layout.AdaptiveDeviceType.MidPhone -> 80.dp
        com.sanket.tools.nexpad.ui.layout.AdaptiveDeviceType.SmallPhone -> 72.dp
    }

    val carouselHorizPadding = when (layout.deviceType) {
        com.sanket.tools.nexpad.ui.layout.AdaptiveDeviceType.Tablet -> if (isCompact) 16.dp else 32.dp
        com.sanket.tools.nexpad.ui.layout.AdaptiveDeviceType.Foldable -> if (isCompact) 14.dp else 28.dp
        com.sanket.tools.nexpad.ui.layout.AdaptiveDeviceType.MidPhone -> if (isCompact) 12.dp else 24.dp
        com.sanket.tools.nexpad.ui.layout.AdaptiveDeviceType.SmallPhone -> if (isCompact) 10.dp else 16.dp
    }

    val boxModifier = if (isCompact) {
        Modifier.fillMaxWidth().then(modifier)
    } else {
        Modifier.fillMaxWidth().aspectRatio(1.1f).then(modifier)
    }

    Box(
        modifier = boxModifier
    ) {
        VPanelBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = if (isCompact) Arrangement.SpaceBetween else Arrangement.Top
        ) {
            HorizontalCenteredHeroCarousel(
                state = state,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(carouselHeight)
                    .padding(
                        horizontal = carouselHorizPadding,
                        vertical = if (isCompact) 6.dp else 20.dp
                    ),
                itemSpacing = 8.dp,
                contentPadding = PaddingValues(horizontal = if (isCompact) 8.dp else 16.dp)
            ) { index ->
                val profile = effectiveProfiles[index]
                val subtitle = if (profile.isDefault) "DEFAULT ${index + 1}" else "CUSTOM"
                InnerLayoutCard(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp)
                        .clickable {
                            if (state.currentItem != index) {
                                onProfileSelected(profile)
                                coroutineScope.launch {
                                    isUserGesture = true
                                    try {
                                        state.scrollToItem(index)
                                    } catch (_: Exception) {}
                                }
                            }
                        },
                    layout = layout,
                    title = profile.name,
                    subtitle = subtitle,
                    isSelected = state.currentItem == index,
                    isDefault = profile.isDefault
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                contentAlignment = Alignment.Center
            ) {
                PlayButton(
                    modifier = Modifier
                        .padding(bottom = if (isCompact) 10.dp else 24.dp),
                    buttonSize = playButtonSize,
                    onClick = {
                        val validIdx = state.currentItem.coerceIn(0, effectiveProfiles.lastIndex)
                        val selected = effectiveProfiles.getOrNull(validIdx) ?: effectiveProfiles.firstOrNull()
                        if (selected != null) {
                            onProfileSelected(selected)
                            onPlayClick(selected)
                        }
                    }
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
