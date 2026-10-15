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
import com.sanket.tools.nexpad.ui.components.effects.CyberGrid
import com.sanket.tools.nexpad.ui.components.splash.MakeInIndiaWalkingLion
import com.sanket.tools.nexpad.ui.layout.AdaptivePosture
import com.sanket.tools.nexpad.ui.layout.adaptiveLayoutSpec
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
            .background(Color(0xFF070B14))
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
        val lionSize: Dp
        val lionBoxWidth: Dp
        val lionBoxHeight: Dp
        val titleFontSize: TextUnit
        val haloBloomSize: Dp

        when (spec.posture) {
            AdaptivePosture.TabletLandscape -> {
                lionSize = 340.dp
                lionBoxWidth = 340.dp
                lionBoxHeight = 170.dp
                titleFontSize = 42.sp
                haloBloomSize = 460.dp
            }
            AdaptivePosture.PhoneLandscape -> {
                lionSize = 220.dp
                lionBoxWidth = 220.dp
                lionBoxHeight = 110.dp
                titleFontSize = 30.sp
                haloBloomSize = 280.dp
            }
            AdaptivePosture.TabletPortrait -> {
                lionSize = 320.dp
                lionBoxWidth = 320.dp
                lionBoxHeight = 160.dp
                titleFontSize = 38.sp
                haloBloomSize = 420.dp
            }
            AdaptivePosture.PhonePortrait -> {
                if (isNarrowPhone) {
                    lionSize = 230.dp
                    lionBoxWidth = 230.dp
                    lionBoxHeight = 115.dp
                    titleFontSize = 28.sp
                    haloBloomSize = 280.dp
                } else {
                    lionSize = 280.dp
                    lionBoxWidth = 280.dp
                    lionBoxHeight = 140.dp
                    titleFontSize = 34.sp
                    haloBloomSize = 340.dp
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
                .size(haloBloomSize)
                .scale(pulseScale)
                .alpha(pulseAlpha)
                .blur(56.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFF9933).copy(alpha = 0.35f), // Saffron
                            Color(0xFF00E5FF).copy(alpha = 0.25f), // Cyber Cyan
                            Color(0xFFB400FF).copy(alpha = 0.15f), // Purple
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
                            .size(lionBoxWidth, lionBoxHeight)
                            .scale(lionScale.value)
                            .alpha(lionAlpha.value)
                    ) {
                        MakeInIndiaWalkingLion(
                            size = lionSize,
                            primaryColor = Color(0xFFFFFFFF),
                            accentColor = Color(0xFFFF9933),
                            secondaryAccent = Color(0xFF00E5FF)
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
                    NexpadTitle(fontSize = titleFontSize, textAlpha = textAlpha.value)

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
                NexpadTitle(fontSize = titleFontSize, textAlpha = textAlpha.value)

                Spacer(modifier = Modifier.height(6.dp))

                SubtitleBadge(textAlpha = textAlpha.value)

                Spacer(modifier = Modifier.height(if (isNarrowPhone) 20.dp else 28.dp))

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(lionBoxWidth, lionBoxHeight)
                        .scale(lionScale.value)
                        .alpha(lionAlpha.value)
                ) {
                    MakeInIndiaWalkingLion(
                        size = lionSize,
                        primaryColor = Color(0xFFFFFFFF),
                        accentColor = Color(0xFFFF9933),
                        secondaryAccent = Color(0xFF00E5FF)
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
                color = Color(0xFF859398),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp
            )
        }
    }
}

@Composable
private fun NexpadTitle(
    fontSize: TextUnit,
    textAlpha: Float,
    modifier: Modifier = Modifier
) {
    Text(
        text = "NEXPAD",
        style = MaterialTheme.typography.displayLarge.copy(
            fontSize = fontSize,
            fontWeight = FontWeight.Black,
            letterSpacing = 5.sp,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFFFFFF),
                    Color(0xFF00E5FF),
                    Color(0xFF00B4FF),
                    Color(0xFFB400FF)
                )
            )
        ),
        modifier = modifier.alpha(textAlpha)
    )
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
                color = Color(0xFF0E1524),
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        Color(0xFF00E5FF).copy(alpha = 0.5f),
                        Color(0xFFB400FF).copy(alpha = 0.5f)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        Text(
            text = "NEXT-GEN VIRTUAL GAMEPAD",
            color = Color(0xFF859398),
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
                .background(Color(0xFFFF9933), CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        // White Dot
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(Color(0xFFFFFFFF), CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        // Green Dot
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(Color(0xFF138808), CircleShape)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = "MAKE IN INDIA",
            color = Color(0xFFFFFFFF),
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 3.5.sp
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Green Dot
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(Color(0xFF138808), CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        // White Dot
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(Color(0xFFFFFFFF), CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        // Saffron Dot
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(Color(0xFFFF9933), CircleShape)
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
            color = Color(0xFF859398),
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
                        Color(0xFFFFFFFF),
                        Color(0xFF00E5FF),
                        Color(0xFFFF9933)
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
                            Color(0xFF00E5FF),
                            Color(0xFFFF9933),
                            Color(0xFFB400FF),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(2.dp)
                )
        )
    }
}
