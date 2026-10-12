package com.sanket.tools.nexpad.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.R
import com.sanket.tools.nexpad.ui.components.effects.CyberGrid
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

    // Entrance Animations
    val logoScale = remember { Animatable(0.75f) }
    val logoAlpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }

    // Pulsing Neon Halo Animation
    val infiniteTransition = rememberInfiniteTransition(label = "SplashNeonPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    LaunchedEffect(Unit) {
        // Trigger subtle haptic entrance
        try { onVibrate?.invoke() } catch (_: Exception) {}

        // Animate emblem in
        logoScale.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(500, easing = FastOutSlowInEasing)
        )
        logoAlpha.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(400)
        )

        // Fade in title & badge
        textAlpha.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(400)
        )

        // Total display duration: ~1.2s then transition to Home
        delay(700)
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
        // Background Cyber Grid
        CyberGrid(
            modifier = Modifier.fillMaxSize()
        )

        // Radial Background Gradient Bloom
        Box(
            modifier = Modifier
                .size(320.dp)
                .scale(pulseScale)
                .alpha(pulseAlpha)
                .blur(48.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF00E5FF).copy(alpha = 0.4f),
                            Color(0xFFB400FF).copy(alpha = 0.25f),
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
            // Glowing Emblem Container
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(140.dp)
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value)
            ) {
                // Outer Cyan/Purple Pulsing Halo Ring
                Box(
                    modifier = Modifier
                        .size(136.dp)
                        .scale(pulseScale)
                        .alpha(pulseAlpha)
                        .border(
                            width = 2.dp,
                            brush = Brush.sweepGradient(
                                listOf(
                                    Color(0xFF00E5FF),
                                    Color(0xFFB400FF),
                                    Color(0xFF00E5FF)
                                )
                            ),
                            shape = CircleShape
                        )
                )

                // Neon Emblem Graphic
                Image(
                    painter = painterResource(id = R.drawable.ic_splash_logo),
                    contentDescription = "NEXPAD Logo",
                    modifier = Modifier.size(110.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Brand Title: NEXPAD with Cyber Gradient
            Text(
                text = "NEXPAD",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 4.sp,
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

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle Badge
            Box(
                modifier = Modifier
                    .alpha(textAlpha.value)
                    .background(
                        color = Color(0xFF0E1524),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            listOf(
                                Color(0xFF00E5FF).copy(alpha = 0.6f),
                                Color(0xFFB400FF).copy(alpha = 0.6f)
                            )
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(Color(0xFF00E5FF), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ULTRA-LOW LATENCY VIRTUAL GAMEPAD",
                        color = Color(0xFFC4C7D0),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                }
            }
        }

        // Tap to skip hint at bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp)
                .alpha(textAlpha.value * 0.5f)
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
