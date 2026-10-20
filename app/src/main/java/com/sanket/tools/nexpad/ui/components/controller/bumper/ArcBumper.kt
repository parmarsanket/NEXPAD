package com.sanket.tools.nexpad.ui.components.controller.bumper

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.model.NexpadKeys
import com.sanket.tools.nexpad.ui.theme.NeonPalette
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * NEXPAD Bumper A — Arc (LB / RB)
 *
 * Faithfully implements the Arc Bumper design:
 * - Geometric curved bridge/arc contour (M24 62 Q115 -10 206 62)
 * - Proportioned and scaled for smartphone gamepad ergonomics (154dp × 56dp)
 * - Multi-pass emissive neon glow ring with drop-shadow halo
 * - Physical tubular body with vertical dark linear gradient (#26282b -> #08090a)
 * - Top specular highlight crescent (M40 56 Q115 -6 190 56 translate(0, -9))
 * - Kinematic plunging spring travel (translateY 2px, scale 0.97)
 * - Centered glowing glyph label with 2px letter-spacing
 */
@Composable
fun ArcBumper(
    key: String,
    isConnected: Boolean = true,
    onVibrate: () -> Unit = {},
    viewModel: GamepadViewModel? = null,
    isRgbEnabled: Boolean = true,
    modifier: Modifier = Modifier,
    displayLabel: String? = null,
    isInteractive: Boolean = true
) {
    var isPressedLocally by remember { mutableStateOf(false) }
    val isPressed = isPressedLocally || com.sanket.tools.nexpad.ui.components.controller.isControlPressed(key)

    val isLeft = remember(key) {
        val upper = key.uppercase()
        upper == NexpadKeys.LB || upper == "L1" || upper == "LEFT"
    }

    val bumperShape = remember { RoundedCornerShape(28.dp) }

    // Kinematic Physics Engine — Damped Harmonic Spring
    val scaleAnimState = animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1.0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "arc_scale"
    )
    val pressOffsetYAnimState = animateFloatAsState(
        targetValue = if (isPressed) 2.0f else 0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "arc_offset"
    )
    val bloomAlphaAnimState = animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0.70f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 800f),
        label = "arc_bloom_alpha"
    )
    val rgbBloomAlphaState = animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0.45f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "arc_rgb_bloom"
    )

    // CSS: --glow: #a97cf0 (LB Purple / RB Cyan)
    val neonColor = remember(isRgbEnabled, isLeft) {
        if (isRgbEnabled) {
            if (isLeft) NeonPalette.SoftPurple else NeonPalette.Cyan
        } else NeonPalette.MetalLight
    }

    val currentOnVibrate by rememberUpdatedState(onVibrate)
    val currentViewModel by rememberUpdatedState(viewModel)

    val density = androidx.compose.ui.platform.LocalDensity.current.density
    val s = remember(density) {
        val w = 154f * density
        val h = 56f * density
        min(w / 230f, h / 84f)
    }
    val ox = remember(density, s) {
        val w = 154f * density
        (w - 230f * s) / 2f
    }
    val oy = remember(density, s) {
        val h = 56f * density
        (h - 84f * s) / 2f
    }

    val arcPath = remember(density, s, ox, oy) {
        Path().apply {
            moveTo(ox + 24f * s, oy + 62f * s)
            quadraticTo(ox + 115f * s, oy - 10f * s, ox + 206f * s, oy + 62f * s)
        }
    }
    val specPath = remember(density, s, ox, oy) {
        Path().apply {
            moveTo(ox + 40f * s, oy + (56f - 9f) * s)
            quadraticTo(ox + 115f * s, oy + (-6f - 9f) * s, ox + 190f * s, oy + (56f - 9f) * s)
        }
    }
    val bodyGradient = remember(density, s, oy) {
        val topY = oy + 6f * s
        val botY = oy + 80f * s
        Brush.verticalGradient(
            colors = listOf(NeonPalette.MetalGraphite, NeonPalette.MetalVoid),
            startY = topY,
            endY = botY
        )
    }

    val strokeRibbonPressed = remember(s) { Stroke(width = 76f * s, cap = StrokeCap.Round, join = StrokeJoin.Round) }
    val strokeRibbonNormal = remember(s) { Stroke(width = 64f * s, cap = StrokeCap.Round, join = StrokeJoin.Round) }
    val strokeBeamPressed = remember(s) { Stroke(width = 58f * s, cap = StrokeCap.Round, join = StrokeJoin.Round) }
    val strokeBeamNormal = remember(s) { Stroke(width = 52f * s, cap = StrokeCap.Round, join = StrokeJoin.Round) }
    val strokeShadow = remember(s) { Stroke(width = 56f * s, cap = StrokeCap.Round, join = StrokeJoin.Round) }
    val strokeHalo = remember(s) { Stroke(width = 50f * s, cap = StrokeCap.Round, join = StrokeJoin.Round) }
    val strokeBody = remember(s) { Stroke(width = 46f * s, cap = StrokeCap.Round, join = StrokeJoin.Round) }
    val strokeSpec = remember(s) { Stroke(width = 6f * s, cap = StrokeCap.Round, join = StrokeJoin.Round) }
    val apexRadiusPressed = remember(density) { 24f * density }
    val apexRadiusNormal = remember(density) { 14f * density }

    val ribbonBloomColors = remember(neonColor) {
        arrayListOf(Color.Transparent, Color.Transparent, Color.Transparent)
    }
    val apexBloomColors = remember(neonColor) {
        arrayListOf(Color.Transparent, Color.Transparent)
    }

    Box(
        modifier = modifier
            .size(154.dp, 56.dp)
            .graphicsLayer {
                // Root RenderNode isolation
            }
            .drawBehind {
                if (isRgbEnabled) {
                    val rgbBloomAlpha = rgbBloomAlphaState.value
                    val w = size.width

                    // 1. Broad outer crescent ribbon bloom
                    ribbonBloomColors[0] = neonColor.copy(alpha = rgbBloomAlpha * (if (isPressed) 0.55f else 0.35f))
                    ribbonBloomColors[1] = neonColor.copy(alpha = rgbBloomAlpha * 0.15f)
                    ribbonBloomColors[2] = Color.Transparent
                    drawPath(
                        path = arcPath,
                        brush = Brush.radialGradient(
                            colors = ribbonBloomColors,
                            center = Offset(w / 2f, oy + 20f * s),
                            radius = w * 0.60f
                        ),
                        style = if (isPressed) strokeRibbonPressed else strokeRibbonNormal
                    )

                    // 2. Focused parabolic neon beam
                    drawPath(
                        path = arcPath,
                        color = neonColor.copy(alpha = rgbBloomAlpha * (if (isPressed) 0.85f else 0.50f)),
                        style = if (isPressed) strokeBeamPressed else strokeBeamNormal
                    )

                    // 3. Arc apex focal crown flare
                    val apexX = ox + 115f * s
                    val apexY = oy - 10f * s
                    val apexRadius = if (isPressed) apexRadiusPressed else apexRadiusNormal
                    apexBloomColors[0] = neonColor.copy(alpha = rgbBloomAlpha * (if (isPressed) 0.90f else 0.55f))
                    apexBloomColors[1] = Color.Transparent
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = apexBloomColors,
                            center = Offset(apexX, apexY),
                            radius = apexRadius
                        ),
                        center = Offset(apexX, apexY),
                        radius = apexRadius
                    )
                }
            }
            .shadow(
                elevation = if (isPressed) 1.dp else 4.dp,
                shape = bumperShape,
                ambientColor = if (isRgbEnabled) neonColor else Color.Black,
                spotColor = if (isRgbEnabled) neonColor else Color.Black
            )
            .graphicsLayer {
                scaleX = scaleAnimState.value
                scaleY = scaleAnimState.value
                translationY = pressOffsetYAnimState.value * density
            }
            .then(
                if (isInteractive) {
                    Modifier.pointerInput(key) {
                        detectTapGestures(
                            onPress = {
                                currentOnVibrate()
                                isPressedLocally = true
                                currentViewModel?.updateButton(key, true)
                                tryAwaitRelease()
                                isPressedLocally = false
                                currentViewModel?.updateButton(key, false)
                            }
                        )
                    }
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val bloomAlphaAnim = bloomAlphaAnimState.value

            // --- Layer 2: Drop Shadow Base (.a0) ---
            // Thicker shadow foundation: stroke-width 56
            drawPath(
                path = arcPath,
                color = Color.Black.copy(alpha = 0.60f),
                style = strokeShadow
            )

            // --- Layer 3: Emissive Neon Halo Ring (.a1) ---
            // Core crisp stroke: stroke-width 50
            drawPath(
                path = arcPath,
                color = neonColor.copy(alpha = bloomAlphaAnim),
                style = strokeHalo
            )

            // --- Layer 4: Tubular Body Gradient (.a2) ---
            // Thicker solid body: stroke-width 46 (increased from 33 for ample interior label space)
            drawPath(
                path = arcPath,
                brush = bodyGradient,
                style = strokeBody
            )

            // --- Layer 5: Specular Crescent Highlight (.a3) ---
            drawPath(
                path = specPath,
                color = Color.White.copy(alpha = if (isPressed) 0.06f else 0.12f),
                style = strokeSpec
            )
        }

        // --- Layer 6: Center Glyph Label (<text x="115" y="35">) ---
        // Offset (-10.5).dp places the glyph directly on the arc curve apex (Y=26 in 84 unit scale)
        val labelText = displayLabel ?: key
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = labelText,
                color = neonColor,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.offset(y = (-10.5).dp)
            )
        }
    }
}

/**
 * Static preview delegate for ArcBumper with 0 touch overhead and 0 coroutines.
 */
@Composable
fun StaticArcBumper(
    key: String = "LB",
    isRgbEnabled: Boolean = true,
    displayLabel: String? = null,
    modifier: Modifier = Modifier
) {
    ArcBumper(
        key = key,
        isConnected = false,
        onVibrate = {},
        viewModel = null,
        isRgbEnabled = isRgbEnabled,
        modifier = modifier,
        displayLabel = displayLabel,
        isInteractive = false
    )
}
