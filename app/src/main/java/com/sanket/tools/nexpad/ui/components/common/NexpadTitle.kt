package com.sanket.tools.nexpad.ui.components.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.ui.theme.NeonPalette

/**
 * Universal brand title for "NEXPAD" with the cyber neon linear gradient.
 * Shared between SplashScreen, HomeScreen TopAppBar, and other branding surfaces.
 * Colors are directly sourced from [NeonPalette.BrandGradient].
 */
@Composable
fun NexpadTitle(
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 34.sp,
    letterSpacing: TextUnit = 3.sp,
    textAlpha: Float = 1f,
    colors: List<Color> = NeonPalette.BrandGradient
) {
    val gradientBrush = remember(colors) {
        Brush.linearGradient(colors)
    }

    Text(
        text = "NEXPAD",
        style = MaterialTheme.typography.displayLarge.copy(
            fontSize = fontSize,
            fontWeight = FontWeight.Black,
            letterSpacing = letterSpacing,
            brush = gradientBrush
        ),
        modifier = modifier.alpha(textAlpha)
    )
}
