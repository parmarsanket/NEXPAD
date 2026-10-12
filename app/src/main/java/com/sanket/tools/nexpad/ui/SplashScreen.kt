package com.sanket.tools.nexpad.ui

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.ui.components.effects.CyberGrid
import com.sanket.tools.nexpad.ui.components.splash.MakeInIndiaWalkingLion
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
    onVibrate: (() -> Unit)? = null,
    modifier: Modifier = Modifier
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
        lionScale.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(500, easing = FastOutSlowInEasing)
        )
        lionAlpha.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(450)
        )

        // 2. Fade in Brand & Developer Name
        textAlpha.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(400)
        )

        // 3. Animate the glowing underline expanding beneath "Sanket Dev"
        underlineWidthFraction.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(650, easing = FastOutSlowInEasing)
        )

        // 4. Hold for user appreciation (~2.0s total) then proceed to Home
        delay(850)
        finishSplash()
    }

    Box(
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
        // Futuristic Cyber Grid Matrix
        CyberGrid(
            modifier = Modifier.fillMaxSize()
        )

        // Radial Background Gradient Bloom (Saffron / Cyan / Purple)
        Box(
            modifier = Modifier
                .size(340.dp)
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

        // Central Content Column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            // App Title Header
            Text(
                text = "NEXPAD",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 34.sp,
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
                modifier = Modifier.alpha(textAlpha.value)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Subtitle Badge
            Box(
                modifier = Modifier
                    .alpha(textAlpha.value)
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

            Spacer(modifier = Modifier.height(28.dp))

            // ── Make in India Mechanical Walking Lion ──────────────────────
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(240.dp, 150.dp)
                    .scale(lionScale.value)
                    .alpha(lionAlpha.value)
            ) {
                MakeInIndiaWalkingLion(
                    size = 230.dp,
                    primaryColor = Color(0xFFFFFFFF),
                    accentColor = Color(0xFFFF9933), // Authentic Saffron
                    secondaryAccent = Color(0xFF00E5FF) // High-Tech Cyan
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── "MAKE IN INDIA" Text with Tricolor Accents ──────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.alpha(textAlpha.value)
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

            Spacer(modifier = Modifier.height(26.dp))

            // ── "Sanket Dev" with Animated Underline ────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.alpha(textAlpha.value)
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

                // Dynamic Animated Underline (expanding smoothly under name)
                Box(
                    modifier = Modifier
                        .width(140.dp * underlineWidthFraction.value)
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

        // Tap to skip hint at bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp)
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
