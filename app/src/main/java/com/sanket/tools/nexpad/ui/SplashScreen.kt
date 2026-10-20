package com.sanket.tools.nexpad.ui

import android.annotation.SuppressLint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.ui.components.common.NexpadTitle
import com.sanket.tools.nexpad.ui.components.effects.CyberGrid
import com.sanket.tools.nexpad.ui.components.splash.MakeInIndiaWalkingLion
import com.sanket.tools.nexpad.ui.layout.AdaptivePosture
import com.sanket.tools.nexpad.ui.layout.adaptiveLayoutSpec
import com.sanket.tools.nexpad.ui.theme.NeonPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun SplashScreen(
    modifier: Modifier = Modifier,
    onSplashFinished: () -> Unit,
    onVibrate: (() -> Unit)? = null,
) {
    var hasFinished by remember { mutableStateOf(false) }

    fun finishSplash() {
        if (!hasFinished) {
            hasFinished = true
            onSplashFinished()
        }
    }

    // Entrance and Underline Animators
    val lionScale = remember { Animatable(0.8f) }
    val lionAlpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val underlineWidthFraction = remember { Animatable(0f) }

    // Pulsing Neon Ambient Halo
    val infiniteTransition = rememberInfiniteTransition(label = "SplashNeonPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    LaunchedEffect(Unit) {
        // Trigger subtle haptic entrance
        try { onVibrate?.invoke() } catch (_: Exception) {}

        // 1. Entrance of Make in India Lion
        launch {
            lionScale.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(450, easing = FastOutSlowInEasing)
            )
        }
        launch {
            lionAlpha.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(400)
            )
        }

        // 2. Fade in Brand & Developer Name
        launch {
            delay(120.milliseconds)
            textAlpha.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(400)
            )
        }

        // 3. Animate the glowing underline expanding beneath "Sanket Dev"
        launch {
            delay(250.milliseconds)
            underlineWidthFraction.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(650, easing = FastOutSlowInEasing)
            )
        }

        // 4. Exactly 2 seconds total splash screen duration while shine sweeps left-to-right
        delay(1500L.milliseconds)
        finishSplash()
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(NeonPalette.PanelBgBottom)
            .pointerInput(Unit) {
                detectTapGestures {
                    finishSplash()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val spec = adaptiveLayoutSpec(maxWidth, maxHeight)
        val isLandscape = maxWidth > maxHeight
        val isNarrowPhone = spec.isShortScreen || maxWidth < 360.dp

        // Adaptive scaling metrics driven directly by AdaptivePosture & AdaptiveLayoutSpec
        val metrics = when (spec.posture) {
            AdaptivePosture.TabletLandscape -> SplashLayoutMetrics(340.dp, 42.sp, 460.dp)
            AdaptivePosture.PhoneLandscape -> SplashLayoutMetrics(220.dp, 30.sp, 280.dp)
            AdaptivePosture.TabletPortrait -> SplashLayoutMetrics(320.dp, 38.sp, 420.dp)
            AdaptivePosture.PhonePortrait -> {
                if (isNarrowPhone) {
                    SplashLayoutMetrics(230.dp, 28.sp, 280.dp)
                } else {
                    SplashLayoutMetrics(280.dp, 34.sp, 340.dp)
                }
            }
        }

        // Futuristic Cyber Grid Matrix
        CyberGrid(
            modifier = Modifier.fillMaxSize()
        )

        // Radial Background Gradient Bloom (Saffron / Cyan / Purple)
        Box(
            modifier = Modifier
                .size(metrics.haloBloomSize)
                .scale(pulseScale)
                .alpha(pulseAlpha)
                .blur(56.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            NeonPalette.TricolorSaffron.copy(alpha = 0.35f),
                            NeonPalette.Cyan.copy(alpha = 0.25f),
                            NeonPalette.Purple.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        if (isLandscape) {
            // ─────────────────────────────────────────────────────────────
            // LANDSCAPE MODE (Two-Pane Ergonomic Split)
            // Left Side: Lion Logo + "MAKE IN INDIA" + Tricolor
            // Right Side: NEXPAD Title + Subtitle Badge + Developed By
            // ─────────────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = if (spec.posture == AdaptivePosture.TabletLandscape) 64.dp else 40.dp,
                        vertical = if (spec.isShortScreen) 16.dp else 32.dp
                    ),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Side: Lion Logo & Make In India
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(metrics.lionBoxWidth, metrics.lionBoxHeight)
                            .scale(lionScale.value)
                            .alpha(lionAlpha.value)
                    ) {
                        MakeInIndiaWalkingLion(
                            size = metrics.lionSize,
                            primaryColor = NeonPalette.PureWhite,
                            accentColor = NeonPalette.TricolorSaffron,
                            secondaryAccent = NeonPalette.Cyan
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    MakeInIndiaBadge(textAlpha = textAlpha.value)
                }

                // Right Side: NEXPAD Branding & Developer Credit
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    NexpadTitle(
                        fontSize = metrics.titleFontSize,
                        letterSpacing = 5.sp,
                        textAlpha = textAlpha.value
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    SubtitleBadge(textAlpha = textAlpha.value)

                    Spacer(modifier = Modifier.height(if (spec.isShortScreen) 16.dp else 24.dp))

                    DeveloperCredit(
                        textAlpha = textAlpha.value,
                        underlineFraction = underlineWidthFraction.value
                    )
                }
            }
        } else {
            // ─────────────────────────────────────────────────────────────
            // PORTRAIT MODE (Responsive Centered Vertical Stack)
            // ─────────────────────────────────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                NexpadTitle(
                    fontSize = metrics.titleFontSize,
                    letterSpacing = 5.sp,
                    textAlpha = textAlpha.value
                )

                Spacer(modifier = Modifier.height(6.dp))

                SubtitleBadge(textAlpha = textAlpha.value)

                Spacer(modifier = Modifier.height(if (isNarrowPhone) 20.dp else 28.dp))

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(metrics.lionBoxWidth, metrics.lionBoxHeight)
                        .scale(lionScale.value)
                        .alpha(lionAlpha.value)
                ) {
                    MakeInIndiaWalkingLion(
                        size = metrics.lionSize,
                        primaryColor = NeonPalette.PureWhite,
                        accentColor = NeonPalette.TricolorSaffron,
                        secondaryAccent = NeonPalette.Cyan
                    )
                }

                Spacer(modifier = Modifier.height(if (isNarrowPhone) 12.dp else 16.dp))

                MakeInIndiaBadge(textAlpha = textAlpha.value)

                Spacer(modifier = Modifier.height(if (isNarrowPhone) 18.dp else 26.dp))

                DeveloperCredit(
                    textAlpha = textAlpha.value,
                    underlineFraction = underlineWidthFraction.value
                )
            }
        }

        // Tap to skip hint at bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (isLandscape && spec.isShortScreen) 12.dp else 28.dp)
                .alpha(textAlpha.value * 0.45f)
        ) {
            Text(
                text = "TAP ANYWHERE TO SKIP",
                color = NeonPalette.MutedText,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp
            )
        }
    }
}

@Composable
private fun SubtitleBadge(
    textAlpha: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .alpha(textAlpha)
            .background(
                color = NeonPalette.PanelBgTop,
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        NeonPalette.Cyan.copy(alpha = 0.5f),
                        NeonPalette.Purple.copy(alpha = 0.5f)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        Text(
            text = "NEXT-GEN VIRTUAL GAMEPAD",
            color = NeonPalette.MutedText,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )
    }
}

@Composable
private fun MakeInIndiaBadge(
    textAlpha: Float,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier.alpha(textAlpha)
    ) {
        // Saffron Dot
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(NeonPalette.TricolorSaffron, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        // White Dot
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(NeonPalette.PureWhite, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        // Green Dot
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(NeonPalette.TricolorGreen, CircleShape)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = "MAKE IN INDIA",
            color = NeonPalette.PureWhite,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 3.5.sp
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Green Dot
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(NeonPalette.TricolorGreen, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        // White Dot
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(NeonPalette.PureWhite, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        // Saffron Dot
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(NeonPalette.TricolorSaffron, CircleShape)
        )
    }
}

@Composable
private fun DeveloperCredit(
    textAlpha: Float,
    underlineFraction: Float,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.alpha(textAlpha)
    ) {
        Text(
            text = "DEVELOPED BY",
            color = NeonPalette.MutedText,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Sanket Dev",
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        NeonPalette.PureWhite,
                        NeonPalette.Cyan,
                        NeonPalette.TricolorSaffron
                    )
                )
            )
        )

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .width(140.dp * underlineFraction)
                .height(2.5.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            NeonPalette.Cyan,
                            NeonPalette.TricolorSaffron,
                            NeonPalette.Purple,
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(2.dp)
                )
        )
    }
}

@Immutable
private data class SplashLayoutMetrics(
    val lionSize: Dp,
    val titleFontSize: TextUnit,
    val haloBloomSize: Dp
) {
    val lionBoxWidth: Dp get() = lionSize
    val lionBoxHeight: Dp get() = lionSize * 0.5f
}

