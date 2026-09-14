package com.sanket.tools.nexpad.ui.layout

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Android Adaptive Width Classes modeled after Material 3 WindowWidthSizeClass.
 */
enum class AdaptiveWidthClass {
    /** < 600 dp — Phone portrait, very narrow window */
    Compact,
    /** 600–839 dp — Phone landscape, foldable unfolded, tablet portrait */
    Medium,
    /** ≥ 840 dp — Tablet landscape, desktop, external monitor */
    Expanded
}

/**
 * Describes the detected device posture and orientation ergonomics.
 */
enum class AdaptivePosture {
    /** Phone portrait — narrow and tall, single column */
    PhonePortrait,
    /** Phone landscape — wide but short screen (height < 480 dp), split two-pane with sticky hero */
    PhoneLandscape,
    /** Tablet portrait — medium-wide and tall, spacious single column */
    TabletPortrait,
    /** Tablet landscape — wide and tall, spacious two-pane split */
    TabletLandscape
}

@Immutable
data class AdaptiveLayoutSpec(
    val widthClass: AdaptiveWidthClass,
    val posture: AdaptivePosture,
    val horizontalPadding: Dp,
    val verticalPadding: Dp,
    val paneSpacing: Dp,
    val contentSpacing: Dp,
    /**
     * True when screen orientation or dimensions permit a two-pane layout
     * (e.g. phone in landscape, tablet in landscape).
     */
    val useTwoPaneLayout: Boolean,
    /**
     * True when screen height is compressed (< 480 dp), indicating phone landscape.
     * Components should adopt compact vertical density.
     */
    val isShortScreen: Boolean,
    val contentMaxWidth: Dp,
    val formMaxWidth: Dp
)

/**
 * Resolves layout metrics based on available width and height inside BoxWithConstraints.
 */
fun adaptiveLayoutSpec(maxWidth: Dp, maxHeight: Dp): AdaptiveLayoutSpec {
    val isLandscape = maxWidth > maxHeight
    val isShort = maxHeight < 480.dp

    val widthClass = when {
        maxWidth < 600.dp -> AdaptiveWidthClass.Compact
        maxWidth < 840.dp -> AdaptiveWidthClass.Medium
        else -> AdaptiveWidthClass.Expanded
    }

    val posture = when {
        isLandscape && isShort -> AdaptivePosture.PhoneLandscape
        isLandscape && !isShort -> AdaptivePosture.TabletLandscape
        !isLandscape && widthClass == AdaptiveWidthClass.Compact -> AdaptivePosture.PhonePortrait
        else -> AdaptivePosture.TabletPortrait
    }

    // Two pane is active whenever in landscape (phone landscape >= 540dp or tablet landscape),
    // or when width is Expanded (>= 840dp).
    val canUseTwoPane = (isLandscape && maxWidth >= 540.dp) || widthClass == AdaptiveWidthClass.Expanded

    return AdaptiveLayoutSpec(
        widthClass = widthClass,
        posture = posture,
        horizontalPadding = when (posture) {
            AdaptivePosture.PhonePortrait -> 16.dp
            AdaptivePosture.PhoneLandscape -> 20.dp
            AdaptivePosture.TabletPortrait -> 24.dp
            AdaptivePosture.TabletLandscape -> 28.dp
        },
        verticalPadding = if (isShort) 8.dp else 16.dp,
        paneSpacing = when (widthClass) {
            AdaptiveWidthClass.Compact -> 12.dp
            AdaptiveWidthClass.Medium -> 16.dp
            AdaptiveWidthClass.Expanded -> 24.dp
        },
        contentSpacing = if (isShort) 10.dp else 16.dp,
        useTwoPaneLayout = canUseTwoPane,
        isShortScreen = isShort,
        contentMaxWidth = when (widthClass) {
            AdaptiveWidthClass.Compact -> 560.dp
            AdaptiveWidthClass.Medium -> 840.dp
            AdaptiveWidthClass.Expanded -> 1200.dp
        },
        formMaxWidth = 560.dp
    )
}
