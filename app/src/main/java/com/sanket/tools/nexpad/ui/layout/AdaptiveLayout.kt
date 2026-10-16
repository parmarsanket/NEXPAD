package com.sanket.tools.nexpad.ui.layout

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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

/**
 * Device classifications tailored for font sizes, card dimensions, and controller scaling.
 */
enum class AdaptiveDeviceType {
    /** Small narrow phone (< 380 dp width) — compact typography and padding to avoid clipping */
    SmallPhone,
    /** Standard modern phone (380–599 dp width, or phone in landscape) — comfortable standard typography */
    MidPhone,
    /** Foldables unfolded & medium phablets (600–839 dp width in portrait/square) — expanded cards & medium-large text */
    Foldable,
    /** Tablets & desktop displays (≥ 840 dp width, or tablet portrait ≥ 720 dp) — generous layouts, large typography */
    Tablet
}

@Immutable
data class AdaptiveLayoutSpec(
    val widthClass: AdaptiveWidthClass,
    val posture: AdaptivePosture,
    val deviceType: AdaptiveDeviceType = AdaptiveDeviceType.MidPhone,
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
    val formMaxWidth: Dp,
    // Adaptive Typography Tokens
    val titleFontSize: TextUnit = 20.sp,
    val subtitleFontSize: TextUnit = 12.5.sp,
    val sectionTitleFontSize: TextUnit = 14.5.sp,
    val cardTitleFontSize: TextUnit = 15.sp,
    val bodyFontSize: TextUnit = 13.sp,
    val buttonFontSize: TextUnit = 13.sp,
    val badgeFontSize: TextUnit = 11.5.sp,
    // Adaptive Card & Component Dimensions
    val cardPadding: Dp = 16.dp,
    val cardSpacing: Dp = 14.dp,
    val iconBoxSize: Dp = 40.dp,
    val iconSize: Dp = 20.dp,
    val buttonHeight: Dp = 44.dp,
    val controllerScale: Float = 1.0f
) {
    val isSmallPhone: Boolean get() = deviceType == AdaptiveDeviceType.SmallPhone
    val isMidPhone: Boolean get() = deviceType == AdaptiveDeviceType.MidPhone
    val isFoldable: Boolean get() = deviceType == AdaptiveDeviceType.Foldable
    val isTablet: Boolean get() = deviceType == AdaptiveDeviceType.Tablet
}

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

    val shortest = minOf(maxWidth, maxHeight)

    val deviceType = when {
        // Tablet: smallest screen dimension is >= 600dp (standard Android sw600dp)
        shortest >= 600.dp -> AdaptiveDeviceType.Tablet
        // Foldable unfolded / phablet: smallest screen dimension is 460..599dp
        shortest >= 460.dp -> AdaptiveDeviceType.Foldable
        // Small phone: smallest screen dimension < 375dp (narrow/compact phones)
        shortest < 375.dp -> AdaptiveDeviceType.SmallPhone
        // Standard / Mid phone: 375..459dp (e.g. 1080x2400 @ 400dpi = 432dp)
        else -> AdaptiveDeviceType.MidPhone
    }

    // Two pane is active whenever in landscape (phone landscape >= 540dp or tablet landscape),
    // or when width is Expanded (>= 840dp).
    val canUseTwoPane = (isLandscape && maxWidth >= 540.dp) || widthClass == AdaptiveWidthClass.Expanded

    val formMaxWidth = when (deviceType) {
        AdaptiveDeviceType.SmallPhone -> 540.dp
        AdaptiveDeviceType.MidPhone -> 580.dp
        AdaptiveDeviceType.Foldable -> 720.dp
        AdaptiveDeviceType.Tablet -> if (isLandscape) 1100.dp else 860.dp
    }

    val titleFontSize = when (deviceType) {
        AdaptiveDeviceType.SmallPhone -> 18.sp
        AdaptiveDeviceType.MidPhone -> 20.sp
        AdaptiveDeviceType.Foldable -> 22.sp
        AdaptiveDeviceType.Tablet -> 25.sp
    }

    val subtitleFontSize = when (deviceType) {
        AdaptiveDeviceType.SmallPhone -> 11.sp
        AdaptiveDeviceType.MidPhone -> 12.5.sp
        AdaptiveDeviceType.Foldable -> 13.5.sp
        AdaptiveDeviceType.Tablet -> 14.5.sp
    }

    val sectionTitleFontSize = when (deviceType) {
        AdaptiveDeviceType.SmallPhone -> 13.sp
        AdaptiveDeviceType.MidPhone -> 14.5.sp
        AdaptiveDeviceType.Foldable -> 16.sp
        AdaptiveDeviceType.Tablet -> 17.5.sp
    }

    val cardTitleFontSize = when (deviceType) {
        AdaptiveDeviceType.SmallPhone -> 13.5.sp
        AdaptiveDeviceType.MidPhone -> 15.sp
        AdaptiveDeviceType.Foldable -> 16.5.sp
        AdaptiveDeviceType.Tablet -> 18.sp
    }

    val bodyFontSize = when (deviceType) {
        AdaptiveDeviceType.SmallPhone -> 11.5.sp
        AdaptiveDeviceType.MidPhone -> 13.sp
        AdaptiveDeviceType.Foldable -> 14.sp
        AdaptiveDeviceType.Tablet -> 15.sp
    }

    val buttonFontSize = when (deviceType) {
        AdaptiveDeviceType.SmallPhone -> 11.5.sp
        AdaptiveDeviceType.MidPhone -> 13.sp
        AdaptiveDeviceType.Foldable -> 14.sp
        AdaptiveDeviceType.Tablet -> 15.sp
    }

    val badgeFontSize = when (deviceType) {
        AdaptiveDeviceType.SmallPhone -> 10.5.sp
        AdaptiveDeviceType.MidPhone -> 11.5.sp
        AdaptiveDeviceType.Foldable -> 12.5.sp
        AdaptiveDeviceType.Tablet -> 13.5.sp
    }

    val cardPadding = when (deviceType) {
        AdaptiveDeviceType.SmallPhone -> if (isShort) 12.dp else 14.dp
        AdaptiveDeviceType.MidPhone -> if (isShort) 14.dp else 16.dp
        AdaptiveDeviceType.Foldable -> 20.dp
        AdaptiveDeviceType.Tablet -> 24.dp
    }

    val cardSpacing = when (deviceType) {
        AdaptiveDeviceType.SmallPhone -> if (isShort) 10.dp else 12.dp
        AdaptiveDeviceType.MidPhone -> if (isShort) 12.dp else 14.dp
        AdaptiveDeviceType.Foldable -> 16.dp
        AdaptiveDeviceType.Tablet -> 20.dp
    }

    val iconBoxSize = when (deviceType) {
        AdaptiveDeviceType.SmallPhone -> 36.dp
        AdaptiveDeviceType.MidPhone -> 40.dp
        AdaptiveDeviceType.Foldable -> 46.dp
        AdaptiveDeviceType.Tablet -> 50.dp
    }

    val iconSize = when (deviceType) {
        AdaptiveDeviceType.SmallPhone -> 18.dp
        AdaptiveDeviceType.MidPhone -> 20.dp
        AdaptiveDeviceType.Foldable -> 24.dp
        AdaptiveDeviceType.Tablet -> 26.dp
    }

    val buttonHeight = when (deviceType) {
        AdaptiveDeviceType.SmallPhone -> 40.dp
        AdaptiveDeviceType.MidPhone -> 44.dp
        AdaptiveDeviceType.Foldable -> 48.dp
        AdaptiveDeviceType.Tablet -> 52.dp
    }

    val controllerScale = when (deviceType) {
        AdaptiveDeviceType.SmallPhone -> 0.90f
        AdaptiveDeviceType.MidPhone -> 1.0f
        AdaptiveDeviceType.Foldable -> 1.15f
        AdaptiveDeviceType.Tablet -> 1.30f
    }

    return AdaptiveLayoutSpec(
        widthClass = widthClass,
        posture = posture,
        deviceType = deviceType,
        horizontalPadding = when (posture) {
            AdaptivePosture.PhonePortrait -> if (deviceType == AdaptiveDeviceType.SmallPhone) 12.dp else 16.dp
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
        formMaxWidth = formMaxWidth,
        titleFontSize = titleFontSize,
        subtitleFontSize = subtitleFontSize,
        sectionTitleFontSize = sectionTitleFontSize,
        cardTitleFontSize = cardTitleFontSize,
        bodyFontSize = bodyFontSize,
        buttonFontSize = buttonFontSize,
        badgeFontSize = badgeFontSize,
        cardPadding = cardPadding,
        cardSpacing = cardSpacing,
        iconBoxSize = iconBoxSize,
        iconSize = iconSize,
        buttonHeight = buttonHeight,
        controllerScale = controllerScale
    )
}
