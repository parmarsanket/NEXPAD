package com.sanket.tools.nexpad.ui.components.controller

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
    isConnected: Boolean,
    onVibrate: () -> Unit,
    viewModel: GamepadViewModel,
    isRgbEnabled: Boolean,
    modifier: Modifier = Modifier,
    displayLabel: String? = null
) {
    var isPressed by remember { mutableStateOf(false) }

    val isLeft = remember(key) {
        val upper = key.uppercase()
        upper == NexpadKeys.LB || upper == "L1" || upper == "LEFT"
    }

    val bumperShape = remember { RoundedCornerShape(28.dp) }

    // Kinematic Physics Engine — Damped Harmonic Spring
    val scaleAnim by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1.0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "arc_scale"
    )
    val pressOffsetYAnim by animateFloatAsState(
        targetValue = if (isPressed) 2.0f else 0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "arc_offset"
    )
    val bloomAlphaAnim by animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0.70f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 800f),
        label = "arc_bloom_alpha"
    )
    val rgbBloomAlpha by animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0.45f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "arc_rgb_bloom"
    )

    // CSS: --glow: #a97cf0 (LB Purple / RB Cyan)
    val neonColor = remember(isRgbEnabled, isLeft) {
        if (isRgbEnabled) {
            if (isLeft) Color(0xFFA97CF0) else Color(0xFF00E5FF)
        } else Color(0xFFD8DEE9)
    }

    val currentOnVibrate by rememberUpdatedState(onVibrate)
    val currentViewModel by rememberUpdatedState(viewModel)

    Box(
        modifier = modifier
            .size(154.dp, 56.dp)
            .graphicsLayer {
                scaleX = scaleAnim
                scaleY = scaleAnim
            }
            .offset { IntOffset(0, pressOffsetYAnim.dp.roundToPx()) }
            .drawBehind {
                if (isRgbEnabled) {
                    val w = size.width
                    val h = size.height
                    val s = min(w / 230f, h / 84f)
                    val ox = (w - 230f * s) / 2f
                    val oy = (h - 84f * s) / 2f

                    val arcPath = Path().apply {
                        moveTo(ox + 24f * s, oy + 62f * s)
                        quadraticTo(ox + 115f * s, oy - 10f * s, ox + 206f * s, oy + 62f * s)
                    }

                    // 1. Broad outer crescent ribbon bloom
                    drawPath(
                        path = arcPath,
                        brush = Brush.radialGradient(
                            colors = listOf(
                                neonColor.copy(alpha = rgbBloomAlpha * (if (isPressed) 0.55f else 0.35f)),
                                neonColor.copy(alpha = rgbBloomAlpha * 0.15f),
                                Color.Transparent
                            ),
                            center = Offset(w / 2f, oy + 20f * s),
                            radius = w * 0.60f
                        ),
                        style = Stroke(
                            width = (if (isPressed) 76f else 64f) * s,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )

                    // 2. Focused parabolic neon beam
                    drawPath(
                        path = arcPath,
                        color = neonColor.copy(alpha = rgbBloomAlpha * (if (isPressed) 0.85f else 0.50f)),
                        style = Stroke(
                            width = (if (isPressed) 58f else 52f) * s,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )

                    // 3. Arc apex focal crown flare
                    val apexX = ox + 115f * s
                    val apexY = oy - 10f * s
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                neonColor.copy(alpha = rgbBloomAlpha * (if (isPressed) 0.90f else 0.55f)),
                                Color.Transparent
                            ),
                            center = Offset(apexX, apexY),
                            radius = if (isPressed) 24.dp.toPx() else 14.dp.toPx()
                        ),
                        center = Offset(apexX, apexY),
                        radius = if (isPressed) 24.dp.toPx() else 14.dp.toPx()
                    )
                }
            }
            .shadow(
                elevation = if (isPressed) 1.dp else 4.dp,
                shape = bumperShape,
                ambientColor = if (isRgbEnabled) neonColor else Color.Black,
                spotColor = if (isRgbEnabled) neonColor else Color.Black
            )
            .pointerInput(key) {
                detectTapGestures(
                    onPress = {
                        currentOnVibrate()
                        isPressed = true
                        currentViewModel.updateButton(key, true)
                        tryAwaitRelease()
                        isPressed = false
                        currentViewModel.updateButton(key, false)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val s = min(w / 230f, h / 84f)
            val ox = (w - 230f * s) / 2f
            val oy = (h - 84f * s) / 2f

            // Base SVG Path: M24 62 Q115 -10 206 62
            val arcPath = Path().apply {
                moveTo(ox + 24f * s, oy + 62f * s)
                quadraticTo(ox + 115f * s, oy - 10f * s, ox + 206f * s, oy + 62f * s)
            }

            // Specular Crescent Path: M40 56 Q115 -6 190 56 with translate(0, -9)
            val specPath = Path().apply {
                moveTo(ox + 40f * s, oy + (56f - 9f) * s)
                quadraticTo(ox + 115f * s, oy + (-6f - 9f) * s, ox + 190f * s, oy + (56f - 9f) * s)
            }

            // --- Layer 2: Drop Shadow Base (.a0) ---
            // Thicker shadow foundation: stroke-width 56
            drawPath(
                path = arcPath,
                color = Color.Black.copy(alpha = 0.60f),
                style = Stroke(width = 56f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // --- Layer 3: Emissive Neon Halo Ring (.a1) ---
            // Core crisp stroke: stroke-width 50
            drawPath(
                path = arcPath,
                color = neonColor.copy(alpha = bloomAlphaAnim),
                style = Stroke(width = 50f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // --- Layer 4: Tubular Body Gradient (.a2) ---
            // Thicker solid body: stroke-width 46 (increased from 33 for ample interior label space)
            val topY = oy + 6f * s
            val botY = oy + 80f * s
            val bodyGradient = Brush.verticalGradient(
                colors = listOf(Color(0xFF282A2E), Color(0xFF08090A)),
                startY = topY,
                endY = botY
            )
            drawPath(
                path = arcPath,
                brush = bodyGradient,
                style = Stroke(width = 46f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // --- Layer 5: Specular Crescent Highlight (.a3) ---
            drawPath(
                path = specPath,
                color = Color.White.copy(alpha = if (isPressed) 0.06f else 0.12f),
                style = Stroke(width = 6f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
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
