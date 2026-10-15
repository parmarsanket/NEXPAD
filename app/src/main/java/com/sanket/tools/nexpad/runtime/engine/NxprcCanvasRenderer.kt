package com.sanket.tools.nexpad.runtime.engine

import android.content.Context
import android.os.Build
import android.util.LruCache
import kotlin.math.roundToInt
import androidx.compose.animation.core.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.drawBehind
import com.sanket.tools.nexpad.category.CategoryManager
import com.sanket.tools.nexpad.category.ControlKey
import com.sanket.tools.nexpad.category.ControllerLabelStyle
import com.sanket.tools.nexpad.model.NexpadKeys
import com.sanket.tools.nexpad.runtime.model.NexPadControl
import com.sanket.tools.nexpad.runtime.model.NexPadInputTarget
import com.sanket.tools.nexpad.nxprc.*
import com.sanket.tools.nexpad.ui.components.controller.abxy.PlayStationSymbol
import com.sanket.tools.nexpad.ui.components.controller.abxy.getPlayStationShape
import com.sanket.tools.nexpad.ui.components.controller.abxy.getPlayStationColor
import com.sanket.tools.nexpad.ui.components.controller.special.calculateGamingStickMagnitude
import com.sanket.tools.nexpad.ui.components.controller.special.VelocityRingBuffer
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin

@Suppress("NOTHING_TO_INLINE")
internal inline fun Color.toArgbLong(): Long = (this.toArgb().toLong()) and 0xFFFFFFFFL

internal enum class NxprcShapeType {
    ROUNDED_RECT,
    OVAL,
    POLYGON,
    HEXAGON,
    OCTAGON,
    PATH;

    companion object {
        fun from(raw: String?): NxprcShapeType = when (raw?.trim()?.uppercase()) {
            "OVAL", "CIRCLE" -> OVAL
            "HEXAGON" -> HEXAGON
            "OCTAGON" -> OCTAGON
            "POLYGON" -> POLYGON
            "PATH" -> PATH
            else -> ROUNDED_RECT
        }
    }
}

private val hueRotateMatrixCache = arrayOfNulls<ColorMatrix>(360)
private val hueRotateFilterCache = arrayOfNulls<ColorFilter>(360)

private fun createHueRotateColorMatrix(degrees: Float): ColorMatrix {
    val rad = Math.toRadians(degrees.toDouble()).toFloat()
    val cosVal = kotlin.math.cos(rad)
    val sinVal = kotlin.math.sin(rad)
    val lumR = 0.213f
    val lumG = 0.715f
    val lumB = 0.072f
    return ColorMatrix(floatArrayOf(
        lumR + cosVal * (1f - lumR) + sinVal * (-lumR),
        lumG + cosVal * (-lumG) + sinVal * (-lumG),
        lumB + cosVal * (-lumB) + sinVal * (1f - lumB),
        0f, 0f,

        lumR + cosVal * (-lumR) + sinVal * 0.143f,
        lumG + cosVal * (1f - lumG) + sinVal * 0.140f,
        lumB + cosVal * (-lumB) + sinVal * (-0.283f),
        0f, 0f,

        lumR + cosVal * (-lumR) + sinVal * (-(1f - lumR)),
        lumG + cosVal * (-lumG) + sinVal * lumG,
        lumB + cosVal * (1f - lumB) + sinVal * lumB,
        0f, 0f,

        0f, 0f, 0f, 1f, 0f
    ))
}

private fun getHueRotateColorMatrix(degrees: Float): ColorMatrix {
    val degNorm = (((degrees % 360f) + 360f) % 360f).roundToInt() % 360
    var mat = hueRotateMatrixCache[degNorm]
    if (mat == null) {
        mat = createHueRotateColorMatrix(degNorm.toFloat())
        hueRotateMatrixCache[degNorm] = mat
    }
    return mat
}

internal fun getHueRotateColorFilter(degrees: Float): ColorFilter {
    val degNorm = (((degrees % 360f) + 360f) % 360f).roundToInt() % 360
    var filter = hueRotateFilterCache[degNorm]
    if (filter == null) {
        filter = ColorFilter.colorMatrix(getHueRotateColorMatrix(degNorm.toFloat()))
        hueRotateFilterCache[degNorm] = filter
    }
    return filter
}

private fun rotateHue(color: Color, degrees: Float): Color {
    val mat = getHueRotateColorMatrix(degrees).values
    val r = color.red
    val g = color.green
    val b = color.blue
    val newR = (mat[0] * r + mat[1] * g + mat[2] * b).coerceIn(0f, 1f)
    val newG = (mat[5] * r + mat[6] * g + mat[7] * b).coerceIn(0f, 1f)
    val newB = (mat[10] * r + mat[11] * g + mat[12] * b).coerceIn(0f, 1f)
    return Color(red = newR, green = newG, blue = newB, alpha = color.alpha)
}

/**
 * Converts a Compose Color to HSV components (Hue in 0..360, Saturation in 0..1, Value in 0..1).
 */
internal fun Color.toHsv(): FloatArray {
    val r = red
    val g = green
    val b = blue
    val max = maxOf(r, maxOf(g, b))
    val min = minOf(r, minOf(g, b))
    val delta = max - min

    val hue = when {
        delta == 0f -> 0f
        max == r -> ((g - b) / delta * 60f + 360f) % 360f
        max == g -> ((b - r) / delta * 60f + 120f) % 360f
        else -> ((r - g) / delta * 60f + 240f) % 360f
    }
    val saturation = if (max == 0f) 0f else delta / max
    val value = max

    return floatArrayOf(hue, saturation, value)
}

internal fun hsvToColor(alpha: Float, hue: Float, saturation: Float, value: Float): Color {
    val c = value * saturation
    val hPrime = ((hue % 360f + 360f) % 360f) / 60f
    val x = c * (1f - kotlin.math.abs(hPrime % 2f - 1f))
    val m = value - c

    val (r1, g1, b1) = when {
        hPrime < 1f -> Triple(c, x, 0f)
        hPrime < 2f -> Triple(x, c, 0f)
        hPrime < 3f -> Triple(0f, c, x)
        hPrime < 4f -> Triple(0f, x, c)
        hPrime < 5f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }

    return Color(
        red = (r1 + m).coerceIn(0f, 1f),
        green = (g1 + m).coerceIn(0f, 1f),
        blue = (b1 + m).coerceIn(0f, 1f),
        alpha = alpha.coerceIn(0f, 1f)
    )
}

/**
 * Returns true if the color is achromatic (neutral white, gray, black, silver, carbon, dark chassis)
 * where hue shifting would produce unnatural chromatic artifacts.
 */
internal fun Color.isAchromatic(): Boolean {
    val r = red
    val g = green
    val b = blue
    val max = maxOf(r, maxOf(g, b))
    val min = minOf(r, minOf(g, b))
    val delta = max - min
    return delta < 0.08f || max < 0.08f || (max > 0f && delta / max < 0.15f)
}

/**
 * Determines whether a color corresponds to the default console accent for a given control.
 * Used to distinguish un-specified / default colors from user-specified custom themes.
 */
internal fun isDefaultConsoleAccent(color: Color, controlKey: String): Boolean {
    if (color.isAchromatic()) return false
    val hsv = color.toHsv()
    val hue = hsv[0]
    return when (controlKey.trim().uppercase()) {
        "A" -> hue in 120f..175f // Xbox Default Emerald Green
        "X" -> hue in 180f..230f // Xbox Default Sapphire Blue
        "Y" -> hue in 30f..70f   // Xbox Default Solar Yellow
        "B" -> hue in 340f..360f || hue in 0f..20f // Xbox Default Crimson Red
        else -> false
    }
}

/**
 * Dynamically adapts an accent color for console platform mode (Xbox <-> PlayStation).
 * STRICT CONSTRAINTS:
 * 1. Only applies to ABXY face buttons (A, B, X, Y). All other categories are 100% untouched.
 * 2. Only shifts colors that match the canonical console default palette.
 * 3. User-specified custom colors (Pink, Gold, White, Carbon, etc.) are strictly preserved.
 */
private val ABXY_KEYS = setOf("A", "B", "X", "Y")

internal fun adaptColorForConsole(
    color: Color,
    controlKey: String,
    labelStyle: ControllerLabelStyle
): Color {
    val upper = controlKey.trim().uppercase()
    if (upper !in ABXY_KEYS) return color
    if (labelStyle != ControllerLabelStyle.PLAYSTATION) return color
    if (color.isAchromatic()) return color

    val hsv = color.toHsv()
    val hue = hsv[0]

    // Button A: Default Green (120°..175°) -> Shift +60° to PlayStation Blue (~210°..220°)
    if (upper == "A" && hue in 120f..175f) {
        val newHue = (hue + 60f) % 360f
        return hsvToColor(color.alpha, newHue, hsv[1], hsv[2])
    }

    // Button X: Default Blue (180°..230°) -> Shift +122° to PlayStation Pink (~315°..330°)
    if (upper == "X" && hue in 180f..230f) {
        val newHue = (hue + 122f) % 360f
        return hsvToColor(color.alpha, newHue, hsv[1], hsv[2])
    }

    // Button Y: Default Yellow (30°..70°) -> Shift +85° to PlayStation Green (~120°..145°)
    if (upper == "Y" && hue in 30f..70f) {
        val newHue = (hue + 85f) % 360f
        return hsvToColor(color.alpha, newHue, hsv[1], hsv[2])
    }

    // Button B: Xbox Red (340°..20°) and PlayStation Circle are both Red -> Keep authentic red
    return color
}

internal fun adaptFill(fill: FillBrush, controlKey: String, labelStyle: ControllerLabelStyle): FillBrush {
    if (labelStyle != ControllerLabelStyle.PLAYSTATION) return fill
    val upper = controlKey.trim().uppercase()
    if (upper !in ABXY_KEYS) return fill

    return when (fill) {
        is FillBrush.Solid -> {
            val adapted = adaptColorForConsole(Color(fill.color), controlKey, labelStyle)
            FillBrush.Solid(adapted.toArgb().toLong() and 0xFFFFFFFFL)
        }
        is FillBrush.LinearGradient -> {
            val newColors = fill.colors.map { c ->
                adaptColorForConsole(Color(c), controlKey, labelStyle).toArgb().toLong() and 0xFFFFFFFFL
            }
            fill.copy(colors = newColors)
        }
        is FillBrush.RadialGradient -> {
            val newColors = fill.colors.map { c ->
                adaptColorForConsole(Color(c), controlKey, labelStyle).toArgb().toLong() and 0xFFFFFFFFL
            }
            fill.copy(colors = newColors)
        }
        is FillBrush.SweepGradient -> {
            val newColors = fill.colors.map { c ->
                adaptColorForConsole(Color(c), controlKey, labelStyle).toArgb().toLong() and 0xFFFFFFFFL
            }
            fill.copy(colors = newColors)
        }
    }
}

internal fun adaptStroke(stroke: StrokeStyle?, controlKey: String, labelStyle: ControllerLabelStyle): StrokeStyle? {
    if (stroke == null || labelStyle != ControllerLabelStyle.PLAYSTATION) return stroke
    val upper = controlKey.trim().uppercase()
    if (upper !in ABXY_KEYS) return stroke
    val adapted = adaptColorForConsole(Color(stroke.color), controlKey, labelStyle)
    return stroke.copy(color = adapted.toArgb().toLong() and 0xFFFFFFFFL)
}

internal fun adaptBoxShadow(shadow: BoxShadowDef, controlKey: String, labelStyle: ControllerLabelStyle): BoxShadowDef {
    if (labelStyle != ControllerLabelStyle.PLAYSTATION) return shadow
    val upper = controlKey.trim().uppercase()
    if (upper !in ABXY_KEYS) return shadow
    val adapted = adaptColorForConsole(Color(shadow.color), controlKey, labelStyle)
    return shadow.copy(color = adapted.toArgb().toLong() and 0xFFFFFFFFL)
}

internal fun resolveAuraColor(
    document: NxprcDocument,
    controlKey: String = document.manifest.defaultControl,
    labelStyle: ControllerLabelStyle = ControllerLabelStyle.XBOX
): Color {
    // 1. Explicit GlowRing layer
    val explicitGlow = document.canvas.layers.filterIsInstance<CanvasLayer.GlowRing>().firstOrNull()
    if (explicitGlow != null) return adaptColorForConsole(Color(explicitGlow.glowColor), controlKey, labelStyle)

    // 2. Outset vibrant box-shadow
    for (layer in document.canvas.layers) {
        if (layer is CanvasLayer.BoxLayer) {
            val vibrantShadow = layer.boxShadows.firstOrNull { shadow ->
                if (shadow.isInset) false else {
                    val c = Color(shadow.color)
                    c.alpha > 0.05f && (kotlin.math.abs(c.red - c.green) > 0.08f || kotlin.math.abs(c.green - c.blue) > 0.08f)
                }
            }
            if (vibrantShadow != null) return adaptColorForConsole(Color(vibrantShadow.color), controlKey, labelStyle)
        }
    }

    // 3. Vibrant stroke
    for (layer in document.canvas.layers) {
        val strokeColor = when (layer) {
            is CanvasLayer.BoxLayer -> layer.stroke?.color
            is CanvasLayer.GradientShape -> layer.stroke?.color
            is CanvasLayer.VectorPath -> layer.stroke?.color
            else -> null
        }
        if (strokeColor != null) {
            val c = Color(strokeColor)
            if (c.alpha > 0.1f && (kotlin.math.abs(c.red - c.green) > 0.08f || kotlin.math.abs(c.green - c.blue) > 0.08f)) {
                return adaptColorForConsole(c, controlKey, labelStyle)
            }
        }
    }

    // 4. ControlKey accentColorArgb
    val controlKeyObj = ControlKey.fromIdentifier(controlKey.ifBlank { document.manifest.defaultControl })
        ?: ControlKey.fromIdentifier(document.manifest.id)
    if (controlKeyObj != null) {
        return adaptColorForConsole(Color(controlKeyObj.accentColorArgb), controlKey, labelStyle)
    }

    // 5. Default Emerald Fallback
    return adaptColorForConsole(Color(0xFF4ADE80L), controlKey, labelStyle)
}

private fun createNxprcColorFilter(filter: FilterDef): ColorFilter? {
    val brightness = filter.brightness.coerceAtLeast(0f)
    val saturation = filter.saturation.coerceAtLeast(0f)
    if (brightness == 1f && saturation == 1f) return null

    val inv = 1f - saturation
    val r = 0.213f * inv
    val g = 0.715f * inv
    val b = 0.072f * inv
    return ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
        brightness * (r + saturation), brightness * g, brightness * b, 0f, 0f,
        brightness * r, brightness * (g + saturation), brightness * b, 0f, 0f,
        brightness * r, brightness * g, brightness * (b + saturation), 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    )))
}

internal fun evaluateAnimationTrack(track: AnimationTrack, progress: Float): Float {
    if (track.keyframes.isEmpty()) return 0f
    if (track.keyframes.size == 1) return track.keyframes[0].value

    val p = progress.coerceIn(0f, 1f)
    val sorted = track.keyframes

    val afterIdx = sorted.indexOfFirst { it.fraction >= p }
    if (afterIdx <= 0) {
        return if (afterIdx == 0) sorted[0].value else sorted.last().value
    }

    val before = sorted[afterIdx - 1]
    val after = sorted[afterIdx]

    val span = after.fraction - before.fraction
    if (span <= 0.00001f) return before.value

    val localFraction = ((p - before.fraction) / span).coerceIn(0f, 1f)

    val easedT = when (track.easing.uppercase()) {
        "EASE_IN_OUT" -> localFraction * localFraction * (3f - 2f * localFraction)
        "EASE_IN" -> localFraction * localFraction
        "EASE_OUT" -> localFraction * (2f - localFraction)
        else -> localFraction
    }

    return before.value + (after.value - before.value) * easedT
}

/**
 * Decoupled Sizing Policy for Nxprc rendering:
 * Guarantees distinct, non-interfering dimensions between Game Screen HUD and Studio Previews.
 */
sealed interface NxprcSizingPolicy {
    /**
     * Standard Game Screen Controller HUD sizing:
     * Calibrated at 0.82x so a 96dp Nxprc button renders at 78.7dp ~ 80dp,
     * maintaining exact 1:1 physical parity with native 80dp controller buttons (RealisticButton).
     */
    object GamepadHud : NxprcSizingPolicy

    /**
     * Button Studio Catalog Grid tile preview:
     * Calibrated at 0.70x with tile bounds for safe display within Studio cards.
     */
    data class StudioGrid(val scale: Float = 0.70f) : NxprcSizingPolicy

    /**
     * Button Studio Interactive Test Sandbox Modal preview:
     * Calibrated specifically for the sandbox testing arena (115dp).
     */
    data class StudioModal(val targetSizeDp: Int = 115, val scale: Float = 0.85f) : NxprcSizingPolicy

    /**
     * Custom sizing.
     */
    data class Custom(val scale: Float = 1.0f, val sizeDp: Int? = null) : NxprcSizingPolicy
}

/**
 * Unlimited Vector Canvas & Animation Renderer for .nxprc packages.
 * Play Store 100% compliant: Pure native Compose vector canvas rendering
 * with photorealistic 3D lighting, specular gloss reflections, and microsecond input latency.
 */
@Composable
fun NxprcCanvasRenderer(
    document: NxprcDocument,
    assignedControl: NexPadControl,
    isConnected: Boolean,
    inputTarget: NexPadInputTarget,
    modifier: Modifier = Modifier,
    overrideSizeDp: Int? = null,
    rumbleIntensity: Float = 0f,
    isInteractive: Boolean = true,
    labelStyle: ControllerLabelStyle = ControllerLabelStyle.XBOX,
    sizingPolicy: NxprcSizingPolicy = NxprcSizingPolicy.GamepadHud
) {
    var isPressed by remember { mutableStateOf(false) }
    val currentInputTarget by rememberUpdatedState(inputTarget)
    val coroutineScope = rememberCoroutineScope()

    val scaleAnimState = if (isInteractive) {
        animateFloatAsState(
            targetValue = if (isPressed) document.animations.pressScale else 1f,
            animationSpec = spring(
                dampingRatio = document.animations.springDamping,
                stiffness = document.animations.springStiffness
            ),
            label = "nxprc_scale"
        )
    } else null

    val pressOffsetYAnimState = if (isInteractive) {
        animateFloatAsState(
            targetValue = if (isPressed) document.animations.pressOffsetY else 0f,
            animationSpec = spring(
                dampingRatio = document.animations.springDamping,
                stiffness = document.animations.springStiffness
            ),
            label = "nxprc_press_y"
        )
    } else null

    val density = androidx.compose.ui.platform.LocalDensity.current.density

    val isStick = assignedControl is NexPadControl.Stick ||
            document.manifest.category.equals("JOYSTICK", ignoreCase = true) ||
            document.manifest.category.equals("TOUCHPAD", ignoreCase = true) ||
            document.manifest.defaultControl.uppercase() in listOf(NexpadKeys.LS, NexpadKeys.RS, NexpadKeys.LTP, NexpadKeys.RTP)
    val stick = run {
        val defCtrl = document.manifest.defaultControl.uppercase()
        val docId = document.manifest.id.lowercase()
        when {
            defCtrl == NexpadKeys.RTP || defCtrl == NexpadKeys.RS || defCtrl == "R3" || docId.contains("rtp") ->
                NexPadControl.Stick(isLeft = false)
            defCtrl == NexpadKeys.LTP || defCtrl == NexpadKeys.LS || defCtrl == "L3" || docId.contains("ltp") ->
                NexPadControl.Stick(isLeft = true)
            assignedControl is NexPadControl.Stick ->
                assignedControl
            else ->
                NexPadControl.Stick(isLeft = defCtrl != NexpadKeys.RS && defCtrl != "R3" && defCtrl != NexpadKeys.RTP && !defCtrl.contains("RIGHT"))
        }
    }
    var thumbOffsetX by remember { mutableFloatStateOf(0f) }
    var thumbOffsetY by remember { mutableFloatStateOf(0f) }

    val context = LocalContext.current
    val (isCameraMode, cameraSensitivity) = remember(context, stick.isLeft, document.manifest) {
        val isTouchpad = document.manifest.category.equals("TOUCHPAD", ignoreCase = true) ||
                document.manifest.defaultControl.equals(NexpadKeys.RTP, ignoreCase = true) ||
                document.manifest.defaultControl.equals(NexpadKeys.LTP, ignoreCase = true)
        if (isTouchpad) {
            val sp = context.getSharedPreferences("nexpad_prefs", Context.MODE_PRIVATE)
            val padKey = if (stick.isLeft) "LTP" else "RTP"
            val specificSens = sp.getFloat("TOUCHPAD_SENSITIVITY_$padKey", -1f)
            val sens = if (specificSens > 0f) specificSens else {
                val globalPadSens = sp.getFloat("TOUCHPAD_SENSITIVITY", -1f)
                if (globalPadSens > 0f) globalPadSens else sp.getFloat("CAMERA_SENSITIVITY", 1.0f) * 2.0f
            }
            Pair(true, sens)
        } else if (!stick.isLeft) {
            val sp = context.getSharedPreferences("nexpad_prefs", Context.MODE_PRIVATE)
            Pair(
                sp.getBoolean("RIGHT_STICK_CAMERA_MODE", false),
                sp.getFloat("CAMERA_SENSITIVITY", 1.0f)
            )
        } else {
            Pair(false, 1.0f)
        }
    }

    val isTrigger = assignedControl is NexPadControl.Trigger
    val trigger = assignedControl as? NexPadControl.Trigger
    val pullProgress = remember { androidx.compose.animation.core.Animatable(0f) }

    val isButtonActive by remember(isInteractive, isStick, isTrigger) {
        derivedStateOf {
            isInteractive && (
                isPressed ||
                (isStick && (thumbOffsetX != 0f || thumbOffsetY != 0f)) ||
                (isTrigger && pullProgress.value > 0.01f)
            )
        }
    }

    val needsPulse = isInteractive && isButtonActive && (document.animations.idleType == "PULSE" || document.canvas.layers.any { it is CanvasLayer.GlowRing && it.pulseEnabled })
    val needsRotation = isInteractive && isButtonActive && document.animations.idleType == "ROTATE"
    val needsRgbCycle = isInteractive && isButtonActive && document.animations.idleType == "RGB_CYCLE"

    val hasDynamicTracks = isInteractive && isButtonActive && document.animations.tracks.isNotEmpty()
    val tracksByProperty = remember(document) {
        document.animations.tracks.associateBy { it.property }
    }
    val scaleTrack = remember(tracksByProperty) { tracksByProperty[AnimatedProperty.SCALE] }
    val rotationTrack = remember(tracksByProperty) { tracksByProperty[AnimatedProperty.ROTATION] }
    val opacityTrack = remember(tracksByProperty) { tracksByProperty[AnimatedProperty.OPACITY] }
    val transXTrack = remember(tracksByProperty) { tracksByProperty[AnimatedProperty.TRANSLATE_X] }
    val transYTrack = remember(tracksByProperty) { tracksByProperty[AnimatedProperty.TRANSLATE_Y] }
    val hueTrack = remember(tracksByProperty) { tracksByProperty[AnimatedProperty.HUE_ROTATE] }

    val trackDurationMs = document.animations.tracks.firstOrNull()?.durationMs ?: document.animations.idleDurationMs
    val rumbleActive = isInteractive && rumbleIntensity > 0f && document.animations.enableGameRumble

    val infiniteTransition = if (needsPulse || needsRotation || needsRgbCycle || hasDynamicTracks || rumbleActive) {
        rememberInfiniteTransition(label = "nxprc_active")
    } else null

    val pulseAlphaState = if (needsPulse && infiniteTransition != null) {
        infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(document.animations.idleDurationMs, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )
    } else null

    val rotateAngleState = if (needsRotation && infiniteTransition != null) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(4000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "rotate"
        )
    } else null

    val rgbHueAngleState = if (needsRgbCycle && infiniteTransition != null) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(maxOf(document.animations.idleDurationMs, 2000), easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "rgb_cycle"
        )
    } else null

    val timelineProgressState = if (hasDynamicTracks && infiniteTransition != null) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(trackDurationMs.coerceAtLeast(200), easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "timeline_progress"
        )
    } else null

    val rumblePhaseState = if (rumbleActive && infiniteTransition != null) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 6.28318f,
            animationSpec = infiniteRepeatable(
                animation = tween(70, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "rumble"
        )
    } else null

    val (rawWidthDp, rawHeightDp, renderScale) = remember(document, overrideSizeDp, sizingPolicy) {
        when (sizingPolicy) {
            is NxprcSizingPolicy.GamepadHud -> {
                val w = (overrideSizeDp ?: document.manifest.widthDp).toFloat()
                val h = (overrideSizeDp ?: document.manifest.heightDp).toFloat()
                Triple(w * 0.82f, h * 0.82f, 1.0f)
            }
            is NxprcSizingPolicy.StudioGrid -> {
                val w = (overrideSizeDp ?: document.manifest.widthDp).toFloat()
                val h = (overrideSizeDp ?: document.manifest.heightDp).toFloat()
                Triple(w, h, sizingPolicy.scale)
            }
            is NxprcSizingPolicy.StudioModal -> {
                val s = (overrideSizeDp ?: sizingPolicy.targetSizeDp).toFloat()
                Triple(s, s, sizingPolicy.scale)
            }
            is NxprcSizingPolicy.Custom -> {
                val w = (sizingPolicy.sizeDp ?: overrideSizeDp ?: document.manifest.widthDp).toFloat()
                val h = (sizingPolicy.sizeDp ?: overrideSizeDp ?: document.manifest.heightDp).toFloat()
                Triple(w, h, sizingPolicy.scale)
            }
        }
    }

    val widthDp = rawWidthDp.roundToInt()
    val heightDp = rawHeightDp.roundToInt()

    val buttonControl = remember(assignedControl) {
        when (assignedControl) {
            is NexPadControl.Button -> assignedControl
            is NexPadControl.DPad -> NexPadControl.Button(assignedControl.direction)
            is NexPadControl.Trigger -> NexPadControl.Button(assignedControl.key)
            is NexPadControl.Stick -> NexPadControl.Button(if (assignedControl.isLeft) NexpadKeys.LS else NexpadKeys.RS)
        }
    }

    val effectiveControlKey = remember(buttonControl, document.manifest.defaultControl) {
        val key = buttonControl.key.ifBlank { document.manifest.defaultControl }
        key.trim().uppercase()
    }

    val isDpadCross = (assignedControl is NexPadControl.Button && (assignedControl.key.equals(NexpadKeys.DPAD, ignoreCase = true) || document.manifest.defaultControl.equals(NexpadKeys.DPAD, ignoreCase = true))) ||
            (document.manifest.category.equals(NexpadKeys.DPAD, ignoreCase = true) && assignedControl is NexPadControl.Button && assignedControl.key.equals(NexpadKeys.DPAD, ignoreCase = true)) ||
            (document.manifest.id.contains("dpad_cross", ignoreCase = true))

    val isTouchpad = document.manifest.category.equals("TOUCHPAD", ignoreCase = true) ||
            document.manifest.defaultControl.uppercase() in listOf(NexpadKeys.LTP, NexpadKeys.RTP)

    val rawAuraColor = remember(document, effectiveControlKey, labelStyle) {
        resolveAuraColor(document, effectiveControlKey, labelStyle)
    }
    val auraBloomAlphaState = if (isInteractive) {
        animateFloatAsState(
            targetValue = if (isPressed) 0.90f else 0.40f,
            animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
            label = "nxprc_aura_alpha"
        )
    } else null

    val adaptedLayers = remember(document, effectiveControlKey, labelStyle) {
        if (labelStyle != ControllerLabelStyle.PLAYSTATION || effectiveControlKey !in ABXY_KEYS) {
            document.canvas.layers
        } else {
            val recolor: (Long) -> Long = { colorLong ->
                adaptColorForConsole(Color(colorLong), effectiveControlKey, labelStyle).toArgbLong()
            }
            document.canvas.layers.map { layer ->
                when (layer) {
                    is CanvasLayer.BoxLayer -> layer.copy(
                        fills = layer.fills.map { adaptFill(it, effectiveControlKey, labelStyle) },
                        stroke = adaptStroke(layer.stroke, effectiveControlKey, labelStyle),
                        boxShadows = layer.boxShadows.map { adaptBoxShadow(it, effectiveControlKey, labelStyle) }
                    )
                    is CanvasLayer.GlowRing -> layer.copy(
                        glowColor = recolor(layer.glowColor)
                    )
                    is CanvasLayer.GradientShape -> layer.copy(
                        fill = adaptFill(layer.fill, effectiveControlKey, labelStyle),
                        stroke = adaptStroke(layer.stroke, effectiveControlKey, labelStyle)
                    )
                    is CanvasLayer.VectorPath -> layer.copy(
                        fill = adaptFill(layer.fill, effectiveControlKey, labelStyle),
                        stroke = adaptStroke(layer.stroke, effectiveControlKey, labelStyle)
                    )
                    is CanvasLayer.InnerShadow -> layer.copy(
                        highlightColor = recolor(layer.highlightColor),
                        shadowColor = recolor(layer.shadowColor)
                    )
                    is CanvasLayer.TextLayer -> layer.copy(
                        textColor = recolor(layer.textColor),
                        textShadows = layer.textShadows.map { ts -> ts.copy(color = recolor(ts.color)) }
                    )
                    is CanvasLayer.CenterGlyph -> layer.copy(
                        textColor = recolor(layer.textColor),
                        shadowColor = layer.shadowColor?.let(recolor),
                        highlightColor = layer.highlightColor?.let(recolor),
                        textShadows = layer.textShadows.map { ts -> ts.copy(color = recolor(ts.color)) }
                    )
                    else -> layer
                }
            }
        }
    }

    val primaryBox = remember(adaptedLayers) { adaptedLayers.filterIsInstance<CanvasLayer.BoxLayer>().firstOrNull() }
    val primaryShape = remember(adaptedLayers) { adaptedLayers.filterIsInstance<CanvasLayer.GradientShape>().firstOrNull() }
    val glowRing = remember(adaptedLayers) { adaptedLayers.filterIsInstance<CanvasLayer.GlowRing>().firstOrNull() }
    val glyph = remember(adaptedLayers) { adaptedLayers.filterIsInstance<CanvasLayer.CenterGlyph>().firstOrNull() }
    val textLayers = remember(adaptedLayers) { adaptedLayers.filterIsInstance<CanvasLayer.TextLayer>() }
    val rootParsedShapeType = remember(primaryBox, primaryShape) {
        NxprcShapeType.from(primaryBox?.shapeType ?: primaryShape?.shapeType)
    }
    val rootIsOval = rootParsedShapeType == NxprcShapeType.OVAL
    val rootShapeType = rootParsedShapeType.name
    val layerShapeTypes = remember(adaptedLayers) {
        adaptedLayers.map { layer ->
            when (layer) {
                is CanvasLayer.BoxLayer -> NxprcShapeType.from(layer.shapeType)
                is CanvasLayer.GradientShape -> NxprcShapeType.from(layer.shapeType)
                else -> NxprcShapeType.ROUNDED_RECT
            }
        }
    }

    val gestureModifier = if (!isInteractive) {
        Modifier
    } else when {
        isStick -> {
            Modifier.pointerInput(document.manifest.id, assignedControl, isCameraMode, cameraSensitivity, density) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    val maxRadius = widthDp * 0.28f * density
                    val deadzoneRadius = 6f * density
                    var previousTouchX = down.position.x
                    var previousTouchY = down.position.y
                    var previousTimeMs = System.currentTimeMillis()
                    var currentStickX = 0f
                    var currentStickY = 0f
                    var lastSpeed = 0f
                    var decayJob: kotlinx.coroutines.Job? = null
                    val velocityBuffer = VelocityRingBuffer(8)

                    val centerX = size.width / 2f
                    val centerY = size.height / 2f

                    if (isCameraMode) {
                        // Relative camera look touch: zero stick on down, do not anchor to center
                        inputTarget.onStickMove(stick, 0f, 0f)
                    } else {
                        // Traditional joystick deflection calculation
                        fun updateStickDeflection(rawPos: Offset) {
                            val vecX = rawPos.x - centerX
                            val vecY = rawPos.y - centerY
                            val dist = hypot(vecX, vecY)

                            val (clampedX, clampedY) = if (dist > maxRadius) {
                                val angle = kotlin.math.atan2(vecY, vecX)
                                Pair(cos(angle) * maxRadius, sin(angle) * maxRadius)
                            } else {
                                Pair(vecX, vecY)
                            }

                            thumbOffsetX = clampedX
                            thumbOffsetY = clampedY

                            val normX = if (dist < deadzoneRadius) 0f else (clampedX / maxRadius).coerceIn(-1f, 1f)
                            val normY = if (dist < deadzoneRadius) 0f else (-clampedY / maxRadius).coerceIn(-1f, 1f)
                            inputTarget.onStickMove(stick, normX, normY)
                        }
                        updateStickDeflection(down.position)
                    }

                    // Continuous 60/120Hz tracking loop
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id }
                        if (change == null || !change.pressed) break
                        change.consume()

                        val currentTouchX = change.position.x
                        val currentTouchY = change.position.y
                        val deltaX = currentTouchX - previousTouchX
                        val deltaY = currentTouchY - previousTouchY
                        previousTouchX = currentTouchX
                        previousTouchY = currentTouchY

                        if (isCameraMode) {
                            var finalDeltaX = deltaX
                            var finalDeltaY = deltaY
                            val absX = kotlin.math.abs(deltaX)
                            val absY = kotlin.math.abs(deltaY)
                            if (absX > 3.0f * absY) {
                                finalDeltaY *= 0.5f // Suppress vertical wobble during horizontal turns
                            } else if (absY > 3.0f * absX) {
                                finalDeltaX *= 0.5f // Suppress horizontal wobble during vertical looks
                            }

                            val distPx = hypot(finalDeltaX, finalDeltaY)
                            val distDp = distPx / density

                            if (distDp > 0.15f) {
                                val currentTimeMs = System.currentTimeMillis()
                                val dtSec = ((currentTimeMs - previousTimeMs).coerceAtLeast(1L)) / 1000f
                                previousTimeMs = currentTimeMs

                                // Push sample into ring buffer for windowed average (eliminates frame-timing jitter)
                                velocityBuffer.push(distDp, dtSec)
                                val smoothedSpeed = velocityBuffer.averageSpeed()
                                lastSpeed = smoothedSpeed

                                val stickMagnitude = calculateGamingStickMagnitude(smoothedSpeed, cameraSensitivity)

                                if (stickMagnitude > 0f) {
                                    val dirX = finalDeltaX / distPx
                                    val dirY = finalDeltaY / distPx

                                    val targetStickX = (dirX * stickMagnitude).coerceIn(-1f, 1f)
                                    val targetStickY = (-dirY * stickMagnitude).coerceIn(-1f, 1f)

                                    // Smooth response (EMA) — heavier smoothing absorbs remaining per-frame noise
                                    currentStickX = 0.55f * targetStickX + 0.45f * currentStickX
                                    currentStickY = 0.55f * targetStickY + 0.45f * currentStickY

                                    inputTarget.onStickMove(stick, currentStickX, currentStickY)

                                    thumbOffsetX = (currentStickX * maxRadius).coerceIn(-maxRadius, maxRadius)
                                    thumbOffsetY = (-currentStickY * maxRadius).coerceIn(-maxRadius, maxRadius)

                                    // Adaptive stationary watchdog: timeout scales with speed
                                    val decayTimeoutMs = (120L - (smoothedSpeed / 20f).toLong()).coerceIn(50L, 120L)
                                    decayJob?.cancel()
                                    decayJob = coroutineScope.launch {
                                        kotlinx.coroutines.delay(decayTimeoutMs)
                                        // 3-stage gentle decay
                                        currentStickX *= 0.5f
                                        currentStickY *= 0.5f
                                        inputTarget.onStickMove(stick, currentStickX, currentStickY)
                                        thumbOffsetX = (currentStickX * maxRadius)
                                        thumbOffsetY = (-currentStickY * maxRadius)
                                        kotlinx.coroutines.delay(30)
                                        currentStickX *= 0.2f
                                        currentStickY *= 0.2f
                                        inputTarget.onStickMove(stick, currentStickX, currentStickY)
                                        thumbOffsetX = (currentStickX * maxRadius)
                                        thumbOffsetY = (-currentStickY * maxRadius)
                                        kotlinx.coroutines.delay(25)
                                        currentStickX = 0f
                                        currentStickY = 0f
                                        inputTarget.onStickMove(stick, 0f, 0f)
                                        thumbOffsetX = 0f
                                        thumbOffsetY = 0f
                                        lastSpeed = 0f
                                    }
                                }
                            }
                        } else {
                            val vecX = change.position.x - centerX
                            val vecY = change.position.y - centerY
                            val currentDist = hypot(vecX, vecY)
                            val (clampedX, clampedY) = if (currentDist > maxRadius) {
                                val angle = kotlin.math.atan2(vecY, vecX)
                                Pair(cos(angle) * maxRadius, sin(angle) * maxRadius)
                            } else {
                                Pair(vecX, vecY)
                            }

                            thumbOffsetX = clampedX
                            thumbOffsetY = clampedY

                            val normX = if (currentDist < deadzoneRadius) 0f else (clampedX / maxRadius).coerceIn(-1f, 1f)
                            val normY = if (currentDist < deadzoneRadius) 0f else (-clampedY / maxRadius).coerceIn(-1f, 1f)
                            inputTarget.onStickMove(stick, normX, normY)
                        }
                    }

                    // Release: reset input and spring back to center
                    isPressed = false
                    decayJob?.cancel()
                    val releaseSpeed = velocityBuffer.peakSpeed()
                    if (isCameraMode && releaseSpeed > 400f) {
                        val coastSteps = ((releaseSpeed / 200f).toInt()).coerceIn(3, 7)
                        coroutineScope.launch {
                            var coastX = currentStickX
                            var coastY = currentStickY
                            repeat(coastSteps) {
                                coastX *= 0.68f
                                coastY *= 0.68f
                                inputTarget.onStickMove(stick, coastX, coastY)
                                thumbOffsetX = (coastX * maxRadius)
                                thumbOffsetY = (-coastY * maxRadius)
                                kotlinx.coroutines.delay(16)
                            }
                            inputTarget.onStickMove(stick, 0f, 0f)
                            thumbOffsetX = 0f
                            thumbOffsetY = 0f
                        }
                    } else {
                        coroutineScope.launch {
                            val animX = androidx.compose.animation.core.Animatable(thumbOffsetX)
                            val animY = androidx.compose.animation.core.Animatable(thumbOffsetY)
                            val springSpec = spring<Float>(
                                stiffness = document.animations.joystickSpringTension,
                                dampingRatio = 0.65f
                            )
                            launch {
                                animX.animateTo(0f, springSpec) {
                                    thumbOffsetX = value
                                }
                            }
                            launch {
                                animY.animateTo(0f, springSpec) {
                                    thumbOffsetY = value
                                }
                            }
                        }
                        inputTarget.onStickMove(stick, 0f, 0f)
                    }
                }
            }
        }
        isTrigger && trigger != null -> {
            Modifier.pointerInput(document.manifest.id, assignedControl) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    coroutineScope.launch {
                        pullProgress.animateTo(1f, spring(stiffness = 900f, dampingRatio = 0.7f))
                    }
                    currentInputTarget.onTriggerMove(trigger, 1f)

                    val upOrCancel = waitForUpOrCancellation()
                    isPressed = false
                    coroutineScope.launch {
                        pullProgress.animateTo(0f, spring(stiffness = 700f, dampingRatio = 0.7f))
                    }
                    currentInputTarget.onTriggerMove(trigger, 0f)
                }
            }
        }
        isDpadCross -> {
            Modifier.pointerInput(document.manifest.id, assignedControl) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    isPressed = true
                    var activeDirs = emptySet<String>()

                    fun evaluateOffset(pos: Offset) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val dx = pos.x - center.x
                        val dy = pos.y - center.y
                        val dist = kotlin.math.hypot(dx, dy)
                        val deadzone = size.width * 0.12f
                        val maxRadius = size.width * 0.70f

                        val newDirs = if (dist < deadzone || dist > maxRadius) {
                            emptySet()
                        } else {
                            val angle = Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble()))
                            val dirs = mutableSetOf<String>()
                            if (angle in -157.5..-22.5) dirs.add(NexpadKeys.UP)
                            if (angle in 22.5..157.5) dirs.add(NexpadKeys.DOWN)
                            if (angle in -67.5..67.5) dirs.add(NexpadKeys.RIGHT)
                            if (angle < -112.5 || angle > 112.5) dirs.add(NexpadKeys.LEFT)
                            dirs
                        }

                        if (newDirs != activeDirs) {
                            val added = newDirs - activeDirs
                            val removed = activeDirs - newDirs
                            removed.forEach { dir -> currentInputTarget.onButtonRelease(NexPadControl.Button(dir)) }
                            added.forEach { dir ->
                                currentInputTarget.triggerHaptic()
                                currentInputTarget.onButtonPress(NexPadControl.Button(dir))
                            }
                            activeDirs = newDirs
                        }
                    }

                    try {
                        evaluateOffset(down.position)
                        while (true) {
                            val event = awaitPointerEvent()
                            val pointer = event.changes.firstOrNull { it.id == down.id }
                            if (pointer == null || !pointer.pressed) break
                            pointer.consume()
                            evaluateOffset(pointer.position)
                        }
                    } finally {
                        activeDirs.forEach { dir -> currentInputTarget.onButtonRelease(NexPadControl.Button(dir)) }
                        isPressed = false
                    }
                }
            }
        }
        else -> {
            Modifier.pointerInput(document.manifest.id, assignedControl, rawWidthDp, rawHeightDp, density) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val isSymmetricCircle = rootIsOval && kotlin.math.abs(rawWidthDp - rawHeightDp) < 4f
                    val isInside = if (isSymmetricCircle) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val btnRad = (minOf(rawWidthDp, rawHeightDp) / 2f * density) * renderScale * 1.25f
                        hypot(down.position.x - center.x, down.position.y - center.y) <= btnRad
                    } else {
                        down.position.x in 0f..size.width.toFloat() && down.position.y in 0f..size.height.toFloat()
                    }
                    if (isInside) {
                        isPressed = true
                        currentInputTarget.onButtonPress(buttonControl)

                        val upOrCancel = waitForUpOrCancellation()
                        isPressed = false
                        currentInputTarget.onButtonRelease(buttonControl)
                    }
                }
            }
        }
    }

    class NxprcAuraBrushes {
        private var buttonColor: Color = Color.Unspecified
        private var buttonCenter: Offset = Offset.Unspecified
        private var buttonRadius: Float = 0f
        private var buttonBrush: Brush? = null

        private var stickBaseColor: Color = Color.Unspecified
        private var stickBaseCenter: Offset = Offset.Unspecified
        private var stickBaseRadius: Float = 0f
        private var stickBaseBrush: Brush? = null

        private var stickCapColor: Color = Color.Unspecified
        private var stickCapRadius: Float = 0f
        private var stickCapBrush: Brush? = null

        fun getButtonAuraBrush(baseColor: Color, center: Offset, radius: Float): Brush {
            val existing = buttonBrush
            if (existing != null && buttonColor == baseColor && buttonCenter == center && buttonRadius == radius) {
                return existing
            }
            val newBrush = Brush.radialGradient(
                0.00f to baseColor.copy(alpha = 0.50f),
                0.40f to baseColor.copy(alpha = 0.20f),
                1.00f to Color.Transparent,
                center = center,
                radius = radius
            )
            buttonColor = baseColor
            buttonCenter = center
            buttonRadius = radius
            buttonBrush = newBrush
            return newBrush
        }

        fun getStickBaseBrush(baseColor: Color, center: Offset, radius: Float): Brush {
            val existing = stickBaseBrush
            if (existing != null && stickBaseColor == baseColor && stickBaseCenter == center && stickBaseRadius == radius) {
                return existing
            }
            val newBrush = Brush.radialGradient(
                0.00f to baseColor.copy(alpha = 0.35f),
                0.40f to baseColor.copy(alpha = 0.12f),
                1.00f to Color.Transparent,
                center = center,
                radius = radius
            )
            stickBaseColor = baseColor
            stickBaseCenter = center
            stickBaseRadius = radius
            stickBaseBrush = newBrush
            return newBrush
        }

        fun getStickCapBrush(baseColor: Color, radius: Float): Brush {
            val existing = stickCapBrush
            if (existing != null && stickCapColor == baseColor && stickCapRadius == radius) {
                return existing
            }
            val newBrush = Brush.radialGradient(
                0.00f to baseColor.copy(alpha = 0.45f),
                0.40f to baseColor.copy(alpha = 0.15f),
                1.00f to Color.Transparent,
                center = Offset.Zero,
                radius = radius
            )
            stickCapColor = baseColor
            stickCapRadius = radius
            stickCapBrush = newBrush
            return newBrush
        }
    }
    val auraBrushes = remember { NxprcAuraBrushes() }
    val stickPlumeStops = remember {
        arrayOf(
            0.00f to Color.Transparent,
            0.40f to Color.Transparent,
            1.00f to Color.Transparent
        )
    }

    Box(
        modifier = modifier
            .size(widthDp.dp, heightDp.dp)
            .graphicsLayer {
                // Root RenderNode isolation
            }
            .drawBehind {
                val progress = timelineProgressState?.value ?: 0f
                val activeHueDegrees = if (hasDynamicTracks) {
                    hueTrack?.let { evaluateAnimationTrack(it, progress) } ?: 0f
                } else if (needsRgbCycle && isButtonActive) {
                    rgbHueAngleState?.value ?: 0f
                } else 0f

                val baseColor = if (activeHueDegrees != 0f) {
                    rotateHue(rawAuraColor, activeHueDegrees)
                } else rawAuraColor

                val auraBloomAlpha = auraBloomAlphaState?.value ?: 0.40f
                val effAlpha = if (isButtonActive) {
                    val stickMaxR = size.minDimension * 0.35f
                    val stickDispFraction = (hypot(thumbOffsetX, thumbOffsetY) / stickMaxR.coerceAtLeast(1f)).coerceIn(0f, 1f)
                    val stickBloomAlpha = if (isInteractive) 0.40f + 0.55f * stickDispFraction else 0.40f
                    (if (needsPulse) (pulseAlphaState?.value ?: 1.0f) else 1.0f) * (if (isStick) stickBloomAlpha else auraBloomAlpha)
                } else {
                    auraBloomAlpha * 0.35f
                }

                if (isStick && !isTouchpad) {
                    val maxR = size.minDimension * 0.35f
                    val dispFraction = (hypot(thumbOffsetX, thumbOffsetY) / maxR).coerceIn(0f, 1f)

                    // 1. Base socket containment bloom
                    val baseBloomR = size.minDimension * 0.75f
                    val baseBrush = auraBrushes.getStickBaseBrush(baseColor, center, baseBloomR)
                    drawCircle(
                        brush = baseBrush,
                        radius = baseBloomR,
                        center = center,
                        alpha = effAlpha
                    )

                    // 2. Deflection comet-plume trailing wake aura
                    val plumeCenter = Offset(
                        center.x + thumbOffsetX * 0.65f,
                        center.y + thumbOffsetY * 0.65f
                    )
                    val plumeRadius = size.minDimension * (0.40f + 0.35f * dispFraction)
                    stickPlumeStops[0] = 0.00f to (if (dispFraction > 0.4f) Color.White else baseColor).copy(alpha = effAlpha * (0.50f + 0.35f * dispFraction))
                    stickPlumeStops[1] = 0.40f to baseColor.copy(alpha = effAlpha * (0.30f + 0.25f * dispFraction))
                    stickPlumeStops[2] = 1.00f to Color.Transparent
                    drawCircle(
                        brush = Brush.radialGradient(
                            colorStops = stickPlumeStops,
                            center = plumeCenter,
                            radius = plumeRadius
                        ),
                        radius = plumeRadius,
                        center = plumeCenter
                    )

                    // 3. High-energy thumbstick cap beacon bloom
                    val capPos = Offset(center.x + thumbOffsetX, center.y + thumbOffsetY)
                    val capBloomR = ((size.minDimension * 0.32f) + (size.minDimension * 0.08f * dispFraction)).roundToInt().toFloat()
                    val capBrush = auraBrushes.getStickCapBrush(baseColor, capBloomR)
                    translate(capPos.x, capPos.y) {
                        drawCircle(
                            brush = capBrush,
                            radius = capBloomR,
                            center = Offset.Zero,
                            alpha = effAlpha
                        )
                    }
                } else {
                    // Atmospheric ambient bloom behind button: blooms outward beyond layout bounds via .drawBehind (matching RealisticButton)
                    val isSymmetricCircle = rootIsOval && kotlin.math.abs(size.width - size.height) < 4f
                    if (isSymmetricCircle) {
                        val auraRadius = size.minDimension * 0.85f
                        val auraBrush = auraBrushes.getButtonAuraBrush(baseColor, center, auraRadius)
                        drawCircle(
                            brush = auraBrush,
                            radius = auraRadius,
                            center = center,
                            alpha = effAlpha
                        )
                    } else {
                        // Universal outward bloom for rectangular, stadium/pill, trigger, bumper, macro & polygon shapes
                        val padX = size.width * 0.20f + 6f * density
                        val padY = size.height * 0.20f + 6f * density
                        val cornerRadius = (primaryBox?.cornerRadiusTopLeft ?: primaryShape?.cornerRadius ?: 16f) * (minOf(size.width, size.height) / document.canvas.viewBoxWidth.coerceAtLeast(1f))

                        // 1. Soft atmospheric outer perimeter
                        drawRoundRect(
                            color = baseColor.copy(alpha = effAlpha * 0.12f),
                            topLeft = Offset(-padX, -padY),
                            size = Size(size.width + padX * 2f, size.height + padY * 2f),
                            cornerRadius = CornerRadius(cornerRadius + padY * 0.8f, cornerRadius + padY * 0.8f)
                        )
                        // 2. Mid ambient halo
                        drawRoundRect(
                            color = baseColor.copy(alpha = effAlpha * 0.25f),
                            topLeft = Offset(-padX * 0.65f, -padY * 0.65f),
                            size = Size(size.width + padX * 1.30f, size.height + padY * 1.30f),
                            cornerRadius = CornerRadius(cornerRadius + padY * 0.5f, cornerRadius + padY * 0.5f)
                        )
                        // 3. Inner core bloom
                        drawRoundRect(
                            color = baseColor.copy(alpha = effAlpha * 0.40f),
                            topLeft = Offset(-padX * 0.35f, -padY * 0.35f),
                            size = Size(size.width + padX * 0.70f, size.height + padY * 0.70f),
                            cornerRadius = CornerRadius(cornerRadius + padY * 0.2f, cornerRadius + padY * 0.2f)
                        )
                    }
                }
            }
            .then(gestureModifier),
        contentAlignment = Alignment.Center
    ) {
        val isTwoStageStick = isStick && !isTouchpad && document.canvas.capLayerIndices.isNotEmpty()
        val capIndicesSet = remember(document) { if (isTouchpad) emptySet() else document.canvas.capLayerIndices.toSet() }

        Box(
            modifier = Modifier
                .size(widthDp.dp, heightDp.dp)
                .graphicsLayer {
                    val progress = timelineProgressState?.value ?: 0f
                    val currentTrackScale = if (hasDynamicTracks) {
                        scaleTrack?.let { evaluateAnimationTrack(it, progress) } ?: 1f
                    } else 1f
                    val currentTrackRot = if (hasDynamicTracks) {
                        rotationTrack?.let { evaluateAnimationTrack(it, progress) } ?: 0f
                    } else 0f
                    val currentTrackOpacity = if (hasDynamicTracks) {
                        opacityTrack?.let { evaluateAnimationTrack(it, progress) } ?: 1f
                    } else 1f
                    val currentTrackTransX = if (hasDynamicTracks) {
                        transXTrack?.let { evaluateAnimationTrack(it, progress) } ?: 0f
                    } else 0f
                    val currentTrackTransY = if (hasDynamicTracks) {
                        transYTrack?.let { evaluateAnimationTrack(it, progress) } ?: 0f
                    } else 0f

                    val rumblePhase = rumblePhaseState?.value ?: 0f
                    val rumbleShakeX = if (rumbleActive) (sin(rumblePhase * 3f) * 3.5f * rumbleIntensity * document.animations.rumbleIntensity) else 0f
                    val rumbleShakeY = if (rumbleActive) (cos(rumblePhase * 4f) * 3.5f * rumbleIntensity * document.animations.rumbleIntensity) else 0f

                    val currentScale = scaleAnimState?.value ?: 1f
                    val currentPressY = pressOffsetYAnimState?.value ?: 0f
                    val finalScale = currentScale * currentTrackScale
                    scaleX = if (isTwoStageStick) 1f else finalScale
                    scaleY = if (isTwoStageStick) 1f else finalScale
                    rotationZ = currentTrackRot
                    alpha = currentTrackOpacity.coerceIn(0f, 1f)
                    translationX = (if (isTwoStageStick || isTouchpad) 0f else thumbOffsetX) + rumbleShakeX + (currentTrackTransX * density)
                    translationY = (if (isTwoStageStick || isTouchpad) 0f else thumbOffsetY) + (currentPressY * density) + (pullProgress.value * document.animations.triggerMaxPullDepth * density) + rumbleShakeY + (currentTrackTransY * density)

                    val activeHueAngle = if (hasDynamicTracks) {
                        hueTrack?.let { evaluateAnimationTrack(it, progress) } ?: 0f
                    } else if (needsRgbCycle) {
                        rgbHueAngleState?.value ?: 0f
                    } else 0f

                    if (activeHueAngle != 0f) {
                        colorFilter = getHueRotateColorFilter(activeHueAngle)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(widthDp.dp, heightDp.dp)) {
                val viewBoxW = document.canvas.viewBoxWidth.coerceAtLeast(1f)
                val viewBoxH = document.canvas.viewBoxHeight.coerceAtLeast(1f)

                val baseScale = minOf((rawWidthDp * density) / viewBoxW, (rawHeightDp * density) / viewBoxH)
                val viewScale = baseScale * renderScale
                val buttonW = viewBoxW * viewScale
                val buttonH = viewBoxH * viewScale
                val buttonLeft = (size.width - buttonW) / 2f
                val buttonTop = (size.height - buttonH) / 2f
                val centerOffset = Offset(size.width / 2f, size.height / 2f)

                val pxPerUnit = viewScale
                val scaleRatio = (pxPerUnit / density).coerceAtLeast(0.01f)

                val rootIsPolygon = rootParsedShapeType == NxprcShapeType.POLYGON || rootParsedShapeType == NxprcShapeType.PATH || (primaryBox?.pathData?.isNotBlank() == true)
                val rootTl = (primaryBox?.cornerRadiusTopLeft ?: primaryShape?.cornerRadius ?: 14f) * pxPerUnit
                val rootTr = (primaryBox?.cornerRadiusTopRight ?: primaryShape?.cornerRadius ?: 14f) * pxPerUnit
                val rootBr = (primaryBox?.cornerRadiusBottomRight ?: primaryShape?.cornerRadius ?: 14f) * pxPerUnit
                val rootBl = (primaryBox?.cornerRadiusBottomLeft ?: primaryShape?.cornerRadius ?: 14f) * pxPerUnit

                val rootEffectiveSides = when {
                    (primaryBox?.polygonSides ?: 0) >= 3 -> primaryBox!!.polygonSides
                    rootParsedShapeType == NxprcShapeType.HEXAGON -> 6
                    rootParsedShapeType == NxprcShapeType.OCTAGON -> 8
                    else -> 0
                }
                val clipKey = RootClipKey(
                    primaryBox?.pathData ?: "",
                    rootEffectiveSides,
                    rootIsOval,
                    buttonLeft, buttonTop, buttonW, buttonH,
                    rootTl, rootTr, rootBr, rootBl
                )
                val rootClipShape = getCachedRootClipShape(clipKey) {
                    Path().apply {
                        if (primaryBox != null && primaryBox.pathData.isNotBlank()) {
                            addPath(buildScaledPath(primaryBox.pathData, Rect(buttonLeft, buttonTop, buttonLeft + buttonW, buttonTop + buttonH)))
                        } else if (rootEffectiveSides >= 3) {
                            addPath(buildRegularPolygonPath(rootEffectiveSides, Rect(buttonLeft, buttonTop, buttonLeft + buttonW, buttonTop + buttonH)))
                        } else if (rootIsOval) {
                            addOval(Rect(buttonLeft, buttonTop, buttonLeft + buttonW, buttonTop + buttonH))
                        } else {
                            addRoundRect(
                                androidx.compose.ui.geometry.RoundRect(
                                    rect = Rect(buttonLeft, buttonTop, buttonLeft + buttonW, buttonTop + buttonH),
                                    topLeft = CornerRadius(rootTl, rootTl),
                                    topRight = CornerRadius(rootTr, rootTr),
                                    bottomRight = CornerRadius(rootBr, rootBr),
                                    bottomLeft = CornerRadius(rootBl, rootBl)
                                )
                            )
                        }
                    }
                }

                val currentTrackScale = if (hasDynamicTracks) {
                    scaleTrack?.let { evaluateAnimationTrack(it, timelineProgressState?.value ?: 0f) } ?: 1f
                } else 1f
                val finalScale = (scaleAnimState?.value ?: 1f) * currentTrackScale
                val drawSingleLayer: androidx.compose.ui.graphics.drawscope.DrawScope.(CanvasLayer) -> Unit = { layer ->
                    when (layer) {
                        is CanvasLayer.BoxLayer -> {
                            val transform = layer.effectiveTransform
                            val effects = layer.effectiveEffects
                            val boxWidth = buttonW * layer.widthRatio
                            val boxHeight = buttonH * layer.heightRatio
                            val boxLeft = buttonLeft + transform.offsetXRatio * buttonW
                            val boxTop = buttonTop + transform.offsetYRatio * buttonH

                            val pivot = Offset(boxLeft + boxWidth * transform.originXRatio, boxTop + boxHeight * transform.originYRatio)
                            val rotAngle = if (transform.isRotating && document.animations.idleType == "ROTATE") (rotateAngleState?.value ?: 0f) else transform.rotationDegrees

                            val shapeEnum = NxprcShapeType.from(layer.shapeType)
                            val isPolygon = shapeEnum == NxprcShapeType.POLYGON || shapeEnum == NxprcShapeType.PATH || layer.pathData.isNotBlank() || layer.polygonSides >= 3
                            val isHexagon = shapeEnum == NxprcShapeType.HEXAGON || layer.polygonSides == 6
                            val isOctagon = shapeEnum == NxprcShapeType.OCTAGON || layer.polygonSides == 8
                            val isOval = shapeEnum == NxprcShapeType.OVAL
                            val layerAlpha = effects.opacity.coerceIn(0f, 1f)
                            val tl = layer.cornerRadiusTopLeft * pxPerUnit
                            val tr = layer.cornerRadiusTopRight * pxPerUnit
                            val br = layer.cornerRadiusBottomRight * pxPerUnit
                            val bl = layer.cornerRadiusBottomLeft * pxPerUnit
                            val hasVariableCorners = !isOval && !isPolygon && (tr != tl || br != tl || bl != tl)

                            val polygonPath = if (layer.pathData.isNotBlank()) {
                                buildScaledPath(layer.pathData, Rect(boxLeft, boxTop, boxLeft + boxWidth, boxTop + boxHeight))
                            } else if (isHexagon) {
                                buildRegularPolygonPath(6, Rect(boxLeft, boxTop, boxLeft + boxWidth, boxTop + boxHeight))
                            } else if (isOctagon) {
                                buildRegularPolygonPath(8, Rect(boxLeft, boxTop, boxLeft + boxWidth, boxTop + boxHeight))
                            } else if (layer.polygonSides >= 3) {
                                buildRegularPolygonPath(layer.polygonSides, Rect(boxLeft, boxTop, boxLeft + boxWidth, boxTop + boxHeight))
                            } else {
                                Path()
                            }

                            val variablePath = if (hasVariableCorners) {
                                getCachedVariablePath(boxLeft, boxTop, boxWidth, boxHeight, tl, tr, br, bl)
                            } else {
                                Path()
                            }

                            val needsOffscreen = effects.compositingStrategy == com.sanket.tools.nexpad.nxprc.CompositingStrategy.OFFSCREEN ||
                                (layerAlpha < 1.0f && (layer.fills.size > 1 || layer.boxShadows.isNotEmpty()))
                            val subAlpha = if (needsOffscreen) 1.0f else layerAlpha

                            val drawBox: () -> Unit = {
                                withTransform({
                                    if (rotAngle != 0f) rotate(rotAngle, pivot = pivot)
                                    if (transform.scaleX != 1f || transform.scaleY != 1f) scale(scaleX = transform.scaleX, scaleY = transform.scaleY, pivot = pivot)
                                }) {
                                    val ovalClipPath = getCachedOvalPath(Rect(boxLeft, boxTop, boxLeft + boxWidth, boxTop + boxHeight))

                                    val elementPath = getOrCreateBoxPath(boxLeft, boxTop, boxWidth, boxHeight, tl, tr, br, bl, isOval)

                                    // 1. Outset box shadows (drawn bottom-to-top per CSS spec)
                                    for (i in layer.boxShadows.indices.reversed()) {
                                        val shadow = layer.boxShadows[i]
                                        if (shadow.isInset) continue
                                        if (glowRing != null && shadow.color == glowRing.glowColor && shadow.blurRadius == glowRing.blurRadius) {
                                            continue
                                        }
                                        val shadowOffset = Offset(shadow.offsetX * pxPerUnit, shadow.offsetY * pxPerUnit)
                                        val sColor = Color(shadow.color)
                                        val spreadPx = shadow.spreadRadius * pxPerUnit
                                        val blurPx = shadow.blurRadius * pxPerUnit
                                        val effAlpha = (sColor.alpha * subAlpha).coerceIn(0f, 1f)
                                        if (effAlpha <= 0.001f) continue
                                        val shadowColorArgb = sColor.copy(alpha = effAlpha).toArgb()

                                        val sLeft = boxLeft + shadowOffset.x - spreadPx
                                        val sTop = boxTop + shadowOffset.y - spreadPx
                                        val sWidth = (boxWidth + spreadPx * 2f).coerceAtLeast(0f)
                                        val sHeight = (boxHeight + spreadPx * 2f).coerceAtLeast(0f)
                                        val sTl = (tl + spreadPx).coerceAtLeast(0f)
                                        val sTr = (tr + spreadPx).coerceAtLeast(0f)
                                        val sBr = (br + spreadPx).coerceAtLeast(0f)
                                        val sBl = (bl + spreadPx).coerceAtLeast(0f)

                                        val shadowPath = getOrCreateBoxPath(sLeft, sTop, sWidth, sHeight, sTl, sTr, sBr, sBl, isOval)

                                        val nativeCanvas = drawContext.canvas.nativeCanvas
                                        val paint = localShadowPaint.get()!!.apply {
                                            reset()
                                            isAntiAlias = true
                                            color = shadowColorArgb
                                            maskFilter = if (blurPx > 0f) getCachedBlurMaskFilter(blurPx / 2f) else null
                                        }

                                        nativeCanvas.save()
                                        try {
                                            if (!isPolygon) {
                                                nativeCanvas.clipOutPath(elementPath)
                                                nativeCanvas.drawPath(shadowPath, paint)
                                            } else {
                                                nativeCanvas.drawPath(shadowPath, paint)
                                            }
                                        } finally {
                                            nativeCanvas.restore()
                                        }
                                    }

                                    // 2. Main surface fills (stacked bottom-to-top per CSS painter's algorithm)
                                    val renderFill: (Brush) -> Unit = { b ->
                                        if (isPolygon && isOval) {
                                            clipPath(ovalClipPath) {
                                                drawPath(polygonPath, brush = b, alpha = subAlpha)
                                            }
                                        } else if (isPolygon && hasVariableCorners) {
                                            clipPath(variablePath) {
                                                drawPath(polygonPath, brush = b, alpha = subAlpha)
                                            }
                                        } else if (isPolygon) {
                                            drawPath(polygonPath, brush = b, alpha = subAlpha)
                                        } else if (isOval) {
                                            drawOval(
                                                brush = b,
                                                topLeft = Offset(boxLeft, boxTop),
                                                size = Size(boxWidth, boxHeight),
                                                alpha = subAlpha
                                            )
                                        } else if (hasVariableCorners) {
                                            drawPath(variablePath, brush = b, alpha = subAlpha)
                                        } else {
                                            drawRoundRect(
                                                brush = b,
                                                topLeft = Offset(boxLeft, boxTop),
                                                size = Size(boxWidth, boxHeight),
                                                cornerRadius = CornerRadius(tl, tl),
                                                alpha = subAlpha
                                            )
                                        }
                                    }

                                    if (layer.fills.isNotEmpty()) {
                                        for (f in layer.fills) {
                                            renderFill(createBrush(f, Size(boxWidth, boxHeight), Offset(boxLeft, boxTop)))
                                        }
                                    } else {
                                        renderFill(createBrush(layer.fill, Size(boxWidth, boxHeight), Offset(boxLeft, boxTop)))
                                    }

                                    // 3. Stroke / Border
                                    layer.stroke?.let { st ->
                                        val stColor = Color(st.color)
                                        val stWidth = st.width * pxPerUnit
                                        val strokeStyle = if (st.isDashed) {
                                            Stroke(width = stWidth, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f * pxPerUnit, 6f * pxPerUnit), 0f))
                                        } else {
                                            Stroke(width = stWidth)
                                        }
                                        if (isPolygon && isOval) {
                                            clipPath(ovalClipPath) {
                                                drawPath(polygonPath, color = stColor.copy(alpha = stColor.alpha * layerAlpha), style = strokeStyle)
                                            }
                                        } else if (isPolygon && hasVariableCorners) {
                                            clipPath(variablePath) {
                                                drawPath(polygonPath, color = stColor.copy(alpha = stColor.alpha * layerAlpha), style = strokeStyle)
                                            }
                                        } else if (isPolygon) {
                                            drawPath(polygonPath, color = stColor.copy(alpha = stColor.alpha * layerAlpha), style = strokeStyle)
                                        } else if (isOval) {
                                            if (st.isTopOnly) {
                                                drawArc(
                                                    color = stColor.copy(alpha = stColor.alpha * layerAlpha),
                                                    startAngle = 180f,
                                                    sweepAngle = 180f,
                                                    useCenter = false,
                                                    topLeft = Offset(boxLeft, boxTop),
                                                    size = Size(boxWidth, boxHeight),
                                                    style = strokeStyle
                                                )
                                            } else {
                                                drawOval(
                                                    color = stColor.copy(alpha = stColor.alpha * layerAlpha),
                                                    topLeft = Offset(boxLeft, boxTop),
                                                    size = Size(boxWidth, boxHeight),
                                                    style = strokeStyle
                                                )
                                            }
                                        } else if (hasVariableCorners) {
                                            drawPath(variablePath, color = stColor.copy(alpha = stColor.alpha * layerAlpha), style = strokeStyle)
                                        } else {
                                            drawRoundRect(
                                                color = stColor.copy(alpha = stColor.alpha * layerAlpha),
                                                topLeft = Offset(boxLeft, boxTop),
                                                size = Size(boxWidth, boxHeight),
                                                cornerRadius = CornerRadius(tl, tl),
                                                style = strokeStyle
                                            )
                                        }
                                    }

                                    // 4. Inset box shadows
                                    for (shadow in layer.boxShadows) {
                                        if (!shadow.isInset) continue
                                        val inColor = Color(shadow.color)
                                        val inAlpha = (inColor.alpha * subAlpha).coerceIn(0f, 1f)
                                        if (inAlpha <= 0.001f) continue
                                        val shadowColorArgb = inColor.copy(alpha = inAlpha).toArgb()

                                        val sOffset = Offset(shadow.offsetX * pxPerUnit, shadow.offsetY * pxPerUnit)
                                        val spreadPx = shadow.spreadRadius * pxPerUnit
                                        val blurPx = shadow.blurRadius * pxPerUnit

                                        val hLeft = boxLeft + sOffset.x + spreadPx
                                        val hTop = boxTop + sOffset.y + spreadPx
                                        val hWidth = (boxWidth - spreadPx * 2f).coerceAtLeast(0f)
                                        val hHeight = (boxHeight - spreadPx * 2f).coerceAtLeast(0f)
                                        val hTl = (tl - spreadPx).coerceAtLeast(0f)
                                        val hTr = (tr - spreadPx).coerceAtLeast(0f)
                                        val hBr = (br - spreadPx).coerceAtLeast(0f)
                                        val hBl = (bl - spreadPx).coerceAtLeast(0f)

                                        val margin = blurPx * 3f + kotlin.math.abs(sOffset.x) + kotlin.math.abs(sOffset.y) + 32f
                                        val holeKey = BoxPathKey(hLeft, hTop, hWidth, hHeight, hTl, hTr, hBr, hBl, isOval)
                                        val insetPath = getOrCreateInsetPath(boxLeft, boxTop, boxWidth, boxHeight, margin, holeKey)

                                        val nativeCanvas = drawContext.canvas.nativeCanvas
                                        val paint = localShadowPaint.get()!!.apply {
                                            reset()
                                            isAntiAlias = true
                                            color = shadowColorArgb
                                            maskFilter = if (blurPx > 0f) getCachedBlurMaskFilter(blurPx / 2f) else null
                                        }

                                        nativeCanvas.save()
                                        try {
                                            nativeCanvas.clipPath(elementPath)
                                            nativeCanvas.drawPath(insetPath, paint)
                                        } finally {
                                            nativeCanvas.restore()
                                        }
                                    }
                                }
                            }

                            val drawFilteredBox: () -> Unit = {
                                val colorFilter = createNxprcColorFilter(effects.filter)
                                if (colorFilter == null && !needsOffscreen) {
                                    drawBox()
                                } else {
                                    val paint = Paint().apply {
                                        if (colorFilter != null) this.colorFilter = colorFilter
                                        if (needsOffscreen) this.alpha = layerAlpha
                                    }
                                    drawContext.canvas.saveLayer(
                                        Rect(boxLeft, boxTop, boxLeft + boxWidth, boxTop + boxHeight),
                                        paint
                                    )
                                    drawBox()
                                    drawContext.canvas.restore()
                                }
                            }

                            val isRootLayer = layer === adaptedLayers.firstOrNull() || (layer.widthRatio >= 1.0f && layer.heightRatio >= 1.0f)
                            if (!isRootLayer && (layer.clipToBounds || document.canvas.clipToBounds)) {
                                clipPath(rootClipShape) {
                                    drawFilteredBox()
                                }
                            } else {
                                drawFilteredBox()
                            }
                        }
                        is CanvasLayer.BezelSocket -> {
                            val baseRadius = minOf(buttonW, buttonH) / 2f
                            // 1. Soft bottom drop shadow
                            drawCircle(
                                color = Color(layer.shadowColor),
                                radius = baseRadius * 0.98f,
                                center = Offset(centerOffset.x, centerOffset.y + baseRadius * 0.05f)
                            )
                            // 2. Solid bezel well
                            drawCircle(
                                color = Color(layer.outerBezelColor),
                                radius = baseRadius * 0.98f,
                                center = centerOffset
                            )
                            // 3. Bezel rim stroke
                            drawCircle(
                                color = Color(layer.outerBevelStroke),
                                radius = baseRadius * 0.98f,
                                center = centerOffset,
                                style = Stroke(width = 2f * pxPerUnit)
                            )
                        }
                        is CanvasLayer.GlowRing -> {
                            val alpha = if (isButtonActive) {
                                if (layer.pulseEnabled && document.animations.idleType == "PULSE") {
                                    pulseAlphaState?.value ?: 1.0f
                                } else {
                                    1.0f
                                }
                            } else {
                                0.35f
                            }
                            val glowColor = Color(layer.glowColor)
                            val buttonRadius = minOf(buttonW, buttonH) / 2f
                            val blurPx = (layer.blurRadius * pxPerUnit).coerceAtLeast(8f * scaleRatio)
                            val totalRadius = buttonRadius + blurPx
                            val innerRatio = (buttonRadius / totalRadius).coerceIn(0.1f, 0.85f)
                            if (rootIsOval) {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colorStops = arrayOf(
                                            0.0f to glowColor.copy(alpha = alpha * 0.35f),
                                            innerRatio to glowColor.copy(alpha = alpha * 0.28f),
                                            (innerRatio + (1f - innerRatio) * 0.5f) to glowColor.copy(alpha = alpha * 0.10f),
                                            1.0f to Color.Transparent
                                        ),
                                        center = centerOffset,
                                        radius = totalRadius
                                    ),
                                    radius = totalRadius,
                                    center = centerOffset
                                )
                            } else {
                                val nativeCanvas = drawContext.canvas.nativeCanvas
                                val glowPaint = localShadowPaint.get()!!.apply {
                                    reset()
                                    isAntiAlias = true
                                    color = glowColor.copy(alpha = alpha * 0.35f).toArgb()
                                    maskFilter = getCachedBlurMaskFilter(blurPx * 0.6f)
                                }
                                val glowPath = if (rootIsPolygon) {
                                    rootClipShape.asAndroidPath()
                                } else {
                                    getOrCreateBoxPath(
                                        buttonLeft - blurPx * 0.4f,
                                        buttonTop - blurPx * 0.4f,
                                        buttonW + blurPx * 0.8f,
                                        buttonH + blurPx * 0.8f,
                                        rootTl + blurPx * 0.4f,
                                        rootTr + blurPx * 0.4f,
                                        rootBr + blurPx * 0.4f,
                                        rootBl + blurPx * 0.4f,
                                        false
                                    )
                                }
                                nativeCanvas.save()
                                try {
                                    nativeCanvas.drawPath(glowPath, glowPaint)
                                } finally {
                                    nativeCanvas.restore()
                                }
                            }
                        }
                        is CanvasLayer.GradientShape -> {
                            val transform = layer.effectiveTransform
                            val effects = layer.effectiveEffects
                            val shapeW = buttonW * layer.widthRatio
                            val shapeH = buttonH * layer.heightRatio
                            val shapeLeft = buttonLeft + buttonW * transform.offsetXRatio
                            val shapeTop = buttonTop + buttonH * transform.offsetYRatio
                            val shapeSize = Size(shapeW, shapeH)
                            val brush = createBrush(layer.fill, shapeSize, Offset(shapeLeft, shapeTop))
                            val cornerRadiusPx = layer.cornerRadius * pxPerUnit
                            val shapeAlpha = effects.opacity.coerceIn(0f, 1f)

                            val hasTransform = transform.hasTransform
                            val pivot = Offset(shapeLeft + transform.originXRatio * shapeW, shapeTop + transform.originYRatio * shapeH)

                            val drawShape: () -> Unit = {
                                val shapeEnum = NxprcShapeType.from(layer.shapeType)
                                val effectiveSides = when (shapeEnum) {
                                    NxprcShapeType.HEXAGON -> 6
                                    NxprcShapeType.OCTAGON -> 8
                                    NxprcShapeType.POLYGON -> 6
                                    else -> 0
                                }
                                when {
                                    effectiveSides >= 3 -> {
                                        val polyPath = buildRegularPolygonPath(effectiveSides, Rect(shapeLeft, shapeTop, shapeLeft + shapeW, shapeTop + shapeH))
                                        drawPath(polyPath, brush = brush, alpha = shapeAlpha)
                                        layer.stroke?.let { st ->
                                            val stColor = Color(st.color)
                                            val stWidth = st.width * pxPerUnit
                                            val strokeStyle = if (st.isDashed) {
                                                Stroke(width = stWidth, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f * pxPerUnit, 6f * pxPerUnit), 0f))
                                            } else {
                                                Stroke(width = stWidth)
                                            }
                                            drawPath(polyPath, color = stColor.copy(alpha = stColor.alpha * shapeAlpha), style = strokeStyle)
                                        }
                                    }
                                    shapeEnum == NxprcShapeType.OVAL -> {
                                        drawOval(brush = brush, topLeft = Offset(shapeLeft, shapeTop), size = shapeSize, alpha = shapeAlpha)
                                        layer.stroke?.let { st ->
                                            val stColor = Color(st.color)
                                            val stWidth = st.width * pxPerUnit
                                            val strokeStyle = if (st.isDashed) {
                                                Stroke(width = stWidth, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f * pxPerUnit, 6f * pxPerUnit), 0f))
                                            } else {
                                                Stroke(width = stWidth)
                                            }
                                            if (st.isTopOnly) {
                                                drawArc(
                                                    color = stColor.copy(alpha = stColor.alpha * shapeAlpha),
                                                    startAngle = 180f,
                                                    sweepAngle = 180f,
                                                    useCenter = false,
                                                    topLeft = Offset(shapeLeft, shapeTop),
                                                    size = shapeSize,
                                                    style = strokeStyle
                                                )
                                            } else {
                                                drawOval(
                                                    color = stColor.copy(alpha = stColor.alpha * shapeAlpha),
                                                    topLeft = Offset(shapeLeft, shapeTop),
                                                    size = shapeSize,
                                                    style = strokeStyle
                                                )
                                            }
                                        }
                                    }
                                    else -> {
                                        drawRoundRect(
                                            brush = brush,
                                            topLeft = Offset(shapeLeft, shapeTop),
                                            size = shapeSize,
                                            cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                                            alpha = shapeAlpha
                                        )
                                        layer.stroke?.let { st ->
                                            val stColor = Color(st.color)
                                            val stWidth = st.width * pxPerUnit
                                            val strokeStyle = if (st.isDashed) {
                                                Stroke(width = stWidth, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f * pxPerUnit, 6f * pxPerUnit), 0f))
                                            } else {
                                                Stroke(width = stWidth)
                                            }
                                            drawRoundRect(
                                                color = stColor.copy(alpha = stColor.alpha * shapeAlpha),
                                                topLeft = Offset(shapeLeft, shapeTop),
                                                size = shapeSize,
                                                cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                                                style = strokeStyle
                                            )
                                        }
                                    }
                                }
                            }

                            val drawFilteredShape: () -> Unit = {
                                val colorFilter = createNxprcColorFilter(effects.filter)
                                if (colorFilter == null) {
                                    drawShape()
                                } else {
                                    val paint = Paint().apply { this.colorFilter = colorFilter }
                                    drawContext.canvas.saveLayer(
                                        Rect(shapeLeft, shapeTop, shapeLeft + shapeW, shapeTop + shapeH),
                                        paint
                                    )
                                    drawShape()
                                    drawContext.canvas.restore()
                                }
                            }

                            if (hasTransform) {
                                withTransform({
                                    scale(transform.scaleX, transform.scaleY, pivot = pivot)
                                    rotate(transform.rotationDegrees, pivot = pivot)
                                }) {
                                    drawFilteredShape()
                                }
                            } else {
                                drawFilteredShape()
                            }
                        }
                        is CanvasLayer.InnerShadow -> {
                            val createInnerPath = { l: Float, t: Float, w: Float, h: Float, rad: Float, oval: Boolean ->
                                getOrCreateBoxPath(l, t, w, h, rad, rad, rad, rad, oval)
                            }

                            val elementPath = createInnerPath(buttonLeft, buttonTop, buttonW, buttonH, rootTl, rootIsOval)
                            val nativeCanvas = drawContext.canvas.nativeCanvas

                            // 1. Highlight pass
                            val hiColor = Color(layer.highlightColor)
                            if (hiColor.alpha > 0.001f) {
                                val hiOffset = Offset(0f, layer.strokeWidth * pxPerUnit * 0.6f)
                                val hiBlurPx = layer.strokeWidth * pxPerUnit * 1.0f
                                val hLeft = buttonLeft + hiOffset.x
                                val hTop = buttonTop + hiOffset.y
                                val margin = hiBlurPx * 3f + kotlin.math.abs(hiOffset.y) + 32f
                                val holeKey = BoxPathKey(hLeft, hTop, buttonW, buttonH, rootTl, rootTl, rootTl, rootTl, rootIsOval)
                                val insetPath = getOrCreateInsetPath(buttonLeft, buttonTop, buttonW, buttonH, margin, holeKey)

                                val paint = localShadowPaint.get()!!.apply {
                                    reset()
                                    isAntiAlias = true
                                    this.color = hiColor.toArgb()
                                    maskFilter = if (hiBlurPx > 0f) getCachedBlurMaskFilter(hiBlurPx / 2f) else null
                                }
                                nativeCanvas.save()
                                try {
                                    nativeCanvas.clipPath(elementPath)
                                    nativeCanvas.drawPath(insetPath, paint)
                                } finally {
                                    nativeCanvas.restore()
                                }
                            }

                            // 2. Shadow pass
                            val shColor = Color(layer.shadowColor)
                            if (shColor.alpha > 0.001f) {
                                val shOffset = Offset(0f, -layer.strokeWidth * pxPerUnit * 1.4f)
                                val shBlurPx = layer.strokeWidth * pxPerUnit * 2.0f
                                val hLeft = buttonLeft + shOffset.x
                                val hTop = buttonTop + shOffset.y
                                val margin = shBlurPx * 3f + kotlin.math.abs(shOffset.y) + 32f
                                val holeKey = BoxPathKey(hLeft, hTop, buttonW, buttonH, rootTl, rootTl, rootTl, rootTl, rootIsOval)
                                val insetPath = getOrCreateInsetPath(buttonLeft, buttonTop, buttonW, buttonH, margin, holeKey)

                                val paint = localShadowPaint.get()!!.apply {
                                    reset()
                                    isAntiAlias = true
                                    this.color = shColor.toArgb()
                                    maskFilter = if (shBlurPx > 0f) getCachedBlurMaskFilter(shBlurPx / 2f) else null
                                }
                                nativeCanvas.save()
                                try {
                                    nativeCanvas.clipPath(elementPath)
                                    nativeCanvas.drawPath(insetPath, paint)
                                } finally {
                                    nativeCanvas.restore()
                                }
                            }
                        }
                        is CanvasLayer.GlossReflection -> {
                            val glossW = buttonW * layer.widthRatio
                            val glossH = buttonH * layer.heightRatio
                            val glossLeft = buttonLeft + buttonW * layer.offsetXRatio
                            val glossTop = buttonTop + buttonH * layer.offsetYRatio
                            val glossCenter = Offset(glossLeft + glossW / 2f, glossTop + glossH / 2f)

                            val drawGloss: () -> Unit = {
                                rotate(layer.rotationDegrees, pivot = glossCenter) {
                                    val blurSpread = layer.blurRadius * pxPerUnit
                                    val effectiveRadius = (glossW / 2f + blurSpread).coerceAtLeast(1f)
                                    val glossBrush = Brush.radialGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.55f * layer.alpha),
                                            Color.White.copy(alpha = 0.25f * layer.alpha),
                                            Color.White.copy(alpha = 0.05f * layer.alpha),
                                            Color.Transparent
                                        ),
                                        center = glossCenter,
                                        radius = effectiveRadius
                                    )
                                    drawOval(
                                        brush = glossBrush,
                                        topLeft = Offset(glossLeft - blurSpread * 0.5f, glossTop - blurSpread * 0.5f),
                                        size = Size(glossW + blurSpread, glossH + blurSpread)
                                    )
                                }
                            }

                            if (document.canvas.clipToBounds) {
                                clipPath(rootClipShape) {
                                    drawGloss()
                                }
                            } else {
                                drawGloss()
                            }
                        }
                        is CanvasLayer.VectorPath -> {
                            val targetRect = Rect(
                                buttonLeft + buttonW * layer.offsetXRatio,
                                buttonTop + buttonH * layer.offsetYRatio,
                                buttonLeft + buttonW * (layer.offsetXRatio + layer.scale),
                                buttonTop + buttonH * (layer.offsetYRatio + layer.scale)
                            )
                            val brush = createBrush(layer.fill, targetRect.size, targetRect.topLeft)
                            val angle = if (layer.isRotating && document.animations.idleType == "ROTATE") (rotateAngleState?.value ?: 0f) else layer.rotationDegrees
                            val vectorPath = if (layer.pathData.isNotBlank()) {
                                buildScaledPath(layer.pathData, targetRect)
                            } else null

                            rotate(angle, pivot = centerOffset) {
                                if (vectorPath != null) {
                                    drawPath(path = vectorPath, brush = brush)
                                    layer.stroke?.let { st ->
                                        drawPath(path = vectorPath, color = Color(st.color), style = Stroke(width = st.width * pxPerUnit))
                                    }
                                } else {
                                    drawCircle(brush = brush, radius = size.minDimension / 2f * 0.75f)
                                    layer.stroke?.let { st ->
                                        drawCircle(color = Color(st.color), radius = size.minDimension / 2f * 0.75f, style = Stroke(width = st.width * pxPerUnit))
                                    }
                                }
                            }
                        }
                        is CanvasLayer.CenterGlyph -> {
                            // Text glyph rendered in outer Box overlay for crisp typography
                        }
                        is CanvasLayer.TextLayer -> {
                            // Text layer rendered in outer Box overlay for crisp typography
                        }
                    }
                }

                adaptedLayers.forEachIndexed { layerIdx, layer ->
                    val isCap = isTwoStageStick && layerIdx in capIndicesSet
                    if (isCap) {
                        withTransform({
                            translate(left = thumbOffsetX, top = thumbOffsetY)
                            scale(scaleX = finalScale, scaleY = finalScale, pivot = centerOffset)
                        }) {
                            drawSingleLayer(layer)
                        }
                    } else {
                        drawSingleLayer(layer)
                    }
                }
            }

        // Center text glyph or multi-text layers with embossed 3D lighting and tactile synchronization
        val viewBox = document.canvas.viewBoxWidth.coerceAtLeast(1f)
        val viewBoxH = document.canvas.viewBoxHeight.coerceAtLeast(1f)
        val baseScale = minOf(rawWidthDp / viewBox, rawHeightDp / viewBoxH)
        val viewScale = baseScale * renderScale
        val buttonW = viewBox * viewScale
        val buttonH = viewBoxH * viewScale
        val scaleFactor = viewScale
        val rootBox = primaryBox

        if (textLayers.isNotEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(widthDp.dp, heightDp.dp)
                    .graphicsLayer {
                        rotationZ = rootBox?.effectiveTransform?.rotationDegrees ?: 0f
                    }
            ) {
                textLayers.forEach { tl ->
                    val resolvedText = CategoryManager.resolveGlyphForStyle(tl.text, document.manifest.defaultControl, labelStyle)
                    val fontSp = (tl.fontSizeSp * scaleFactor).sp
                    val fontWeight = if (tl.fontWeight >= 900) FontWeight.Black else if (tl.fontWeight >= 700) FontWeight.Bold else FontWeight.Normal
                    val isLayerCap = isTwoStageStick && adaptedLayers.indexOf(tl).let { it != -1 && it in capIndicesSet }
                    val baseOffX = (tl.offsetXRatio * buttonW).dp
                    val baseOffY = (tl.offsetYRatio * buttonH).dp
                    val capOffsetModifier = Modifier.offset {
                        if (isLayerCap) IntOffset(thumbOffsetX.roundToInt(), thumbOffsetY.roundToInt()) else IntOffset.Zero
                    }

                    val psShape = getPlayStationShape(resolvedText)
                    val glyphSizeDp = (tl.fontSizeSp * scaleFactor * 1.20f).dp
                    if (psShape != null) {
                        if (tl.textShadows.isNotEmpty()) {
                            tl.textShadows.forEach { ts ->
                                PlayStationSymbol(
                                    shape = psShape,
                                    color = Color(ts.color),
                                    size = glyphSizeDp,
                                    modifier = Modifier
                                        .offset(
                                            x = baseOffX + (ts.offsetX * scaleFactor).dp,
                                            y = baseOffY + (ts.offsetY * scaleFactor).dp
                                        )
                                        .then(capOffsetModifier)
                                )
                            }
                        }
                        val rawSymbolColor = Color(tl.textColor)
                        val symbolColor = if (rawSymbolColor.isAchromatic()) {
                            getPlayStationColor(psShape)
                        } else if (isDefaultConsoleAccent(rawSymbolColor, effectiveControlKey)) {
                            adaptColorForConsole(rawSymbolColor, effectiveControlKey, labelStyle)
                        } else {
                            rawSymbolColor
                        }
                        PlayStationSymbol(
                            shape = psShape,
                            color = symbolColor,
                            size = glyphSizeDp,
                            modifier = Modifier
                                .offset(x = baseOffX, y = baseOffY)
                                .then(capOffsetModifier)
                        )
                    } else {
                        if (tl.textShadows.isNotEmpty()) {
                            tl.textShadows.forEach { ts ->
                                Text(
                                    text = resolvedText,
                                    color = Color(ts.color),
                                    fontSize = fontSp,
                                    fontWeight = fontWeight,
                                    modifier = Modifier
                                        .offset(
                                            x = baseOffX + (ts.offsetX * scaleFactor).dp,
                                            y = baseOffY + (ts.offsetY * scaleFactor).dp
                                        )
                                        .then(capOffsetModifier)
                                )
                            }
                        }
                        Text(
                            text = resolvedText,
                            color = Color(tl.textColor),
                            fontSize = fontSp,
                            fontWeight = fontWeight,
                            modifier = Modifier
                                .offset(x = baseOffX, y = baseOffY)
                                .then(capOffsetModifier)
                        )
                    }
                }
            }
        } else if (glyph != null || adaptedLayers.any { it is CanvasLayer.CenterGlyph }) {
            val rawCenterText = glyph?.text ?: document.manifest.defaultControl
            val centerText = CategoryManager.resolveGlyphForStyle(rawCenterText, document.manifest.defaultControl, labelStyle)
            val textColor = glyph?.textColor ?: 0xFFF5F5F5L
            val baseFontSp = glyph?.fontSizeSp ?: (viewBox * 0.32f)
            val fontSp = (baseFontSp * scaleFactor).sp
            val fontWeight = FontWeight.Bold

            val baseOffX = ((glyph?.offsetXRatio ?: 0f) * buttonW).dp
            val baseOffY = ((glyph?.offsetYRatio ?: 0f) * buttonH).dp

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .offset(x = baseOffX, y = baseOffY)
                    .offset {
                        if (isTwoStageStick) IntOffset(thumbOffsetX.roundToInt(), thumbOffsetY.roundToInt()) else IntOffset.Zero
                    }
                    .graphicsLayer {
                        rotationZ = rootBox?.effectiveTransform?.rotationDegrees ?: 0f
                        if (isTwoStageStick) {
                            val progress = timelineProgressState?.value ?: 0f
                            val currentTrackScale = if (hasDynamicTracks) {
                                scaleTrack?.let { evaluateAnimationTrack(it, progress) } ?: 1f
                            } else 1f
                            val finalScale = (scaleAnimState?.value ?: 1f) * currentTrackScale
                            scaleX = finalScale
                            scaleY = finalScale
                        }
                    }
            ) {
                val psShape = getPlayStationShape(centerText)
                val glyphSizeDp = (baseFontSp * scaleFactor * 1.20f).dp
                if (psShape != null) {
                    val extraShadows = glyph?.textShadows ?: emptyList()
                    if (extraShadows.isNotEmpty()) {
                        extraShadows.forEach { ts ->
                            PlayStationSymbol(
                                shape = psShape,
                                color = Color(ts.color),
                                size = glyphSizeDp,
                                modifier = Modifier.offset(
                                    x = (ts.offsetX * scaleFactor).dp,
                                    y = (ts.offsetY * scaleFactor).dp
                                )
                            )
                        }
                    } else {
                        glyph?.shadowColor?.let { sc ->
                            PlayStationSymbol(
                                shape = psShape,
                                color = Color(sc),
                                size = glyphSizeDp,
                                modifier = Modifier.offset(y = (glyph.shadowOffsetY * scaleFactor).dp)
                            )
                        }
                        glyph?.highlightColor?.let { hc ->
                            PlayStationSymbol(
                                shape = psShape,
                                color = Color(hc),
                                size = glyphSizeDp,
                                modifier = Modifier.offset(y = (-1f * scaleFactor).dp)
                            )
                        }
                    }
                    // Foreground PlayStation symbol
                    val rawSymbolColor = Color(textColor)
                    val symbolColor = if (rawSymbolColor.isAchromatic()) {
                        getPlayStationColor(psShape)
                    } else if (isDefaultConsoleAccent(rawSymbolColor, effectiveControlKey)) {
                        adaptColorForConsole(rawSymbolColor, effectiveControlKey, labelStyle)
                    } else {
                        rawSymbolColor
                    }
                    PlayStationSymbol(
                        shape = psShape,
                        color = symbolColor,
                        size = glyphSizeDp
                    )
                } else {
                    val extraShadows = glyph?.textShadows ?: emptyList()
                    if (extraShadows.isNotEmpty()) {
                        extraShadows.forEach { ts ->
                            Text(
                                text = centerText,
                                color = Color(ts.color),
                                fontSize = fontSp,
                                fontWeight = fontWeight,
                                modifier = Modifier.offset(
                                    x = (ts.offsetX * scaleFactor).dp,
                                    y = (ts.offsetY * scaleFactor).dp
                                )
                            )
                        }
                    } else {
                        glyph?.shadowColor?.let { sc ->
                            Text(
                                text = centerText,
                                color = Color(sc),
                                fontSize = fontSp,
                                fontWeight = fontWeight,
                                modifier = Modifier.offset(y = (glyph.shadowOffsetY * scaleFactor).dp)
                            )
                        }
                        glyph?.highlightColor?.let { hc ->
                            Text(
                                text = centerText,
                                color = Color(hc),
                                fontSize = fontSp,
                                fontWeight = fontWeight,
                                modifier = Modifier.offset(y = (-1f * scaleFactor).dp)
                            )
                        }
                    }
                    // Foreground text
                    Text(
                        text = centerText,
                        color = Color(textColor),
                        fontSize = fontSp,
                        fontWeight = fontWeight
                    )
                }
            }
        }
    }
}
}

private fun sampleGradientColor(colors: List<Long>, stops: List<Float>, pos: Float): Color {
    if (colors.isEmpty()) return Color.Transparent
    if (colors.size == 1) return Color(colors[0])
    val clamped = pos.coerceIn(0f, 1f)
    var idx = 0
    while (idx < stops.size - 1 && stops[idx + 1] < clamped) {
        idx++
    }
    if (idx >= stops.size - 1) return Color(colors.last())
    val s0 = stops[idx]
    val s1 = stops[idx + 1]
    val range = (s1 - s0).coerceAtLeast(0.0001f)
    val t = ((clamped - s0) / range).coerceIn(0f, 1f)
    val c1 = Color(colors[idx])
    val c2 = Color(colors[idx + 1])
    return Color(
        red = c1.red + (c2.red - c1.red) * t,
        green = c1.green + (c2.green - c1.green) * t,
        blue = c1.blue + (c2.blue - c1.blue) * t,
        alpha = c1.alpha + (c2.alpha - c1.alpha) * t
    )
}

// --- LRU RENDER CACHES & POOLS (Zero-allocation memoization) ---

internal data class ScaledPathKey(
    val svgData: String,
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float
)

@Immutable
internal data class BaseSvgPathEntry(
    val path: android.graphics.Path,
    val bounds: android.graphics.RectF,
    val isNormalized100: Boolean
)

internal data class PolygonKey(
    val sides: Int,
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float
)

internal data class BoxPathKey(
    val l: Float,
    val t: Float,
    val w: Float,
    val h: Float,
    val tlR: Float,
    val trR: Float,
    val brR: Float,
    val blR: Float,
    val isOval: Boolean
)

internal data class InsetPathKey(
    val boxLeft: Float,
    val boxTop: Float,
    val boxWidth: Float,
    val boxHeight: Float,
    val margin: Float,
    val holeKey: BoxPathKey
)

@Immutable
internal data class BrushKey(
    val fill: FillBrush,
    val width: Float,
    val height: Float,
    val left: Float,
    val top: Float
)

internal data class VariablePathKey(
    val l: Float,
    val t: Float,
    val w: Float,
    val h: Float,
    val tl: Float,
    val tr: Float,
    val br: Float,
    val bl: Float
)

internal data class RootClipKey(
    val pathData: String,
    val effectiveSides: Int,
    val isOval: Boolean,
    val buttonLeft: Float,
    val buttonTop: Float,
    val buttonW: Float,
    val buttonH: Float,
    val tl: Float,
    val tr: Float,
    val br: Float,
    val bl: Float
)

private val scaledPathCache = LruCache<ScaledPathKey, Path>(512)
private val baseSvgPathCache = LruCache<String, BaseSvgPathEntry>(256)
private val polygonPathCache = LruCache<PolygonKey, Path>(128)
private val androidBoxPathCache = LruCache<BoxPathKey, android.graphics.Path>(256)
private val insetPathCache = LruCache<InsetPathKey, android.graphics.Path>(128)
private val brushCache = LruCache<BrushKey, Brush>(512)
private val blurFilterCache = LruCache<Int, android.graphics.BlurMaskFilter>(64)
private val variablePathCache = LruCache<VariablePathKey, Path>(256)
private val rootClipShapeCache = LruCache<RootClipKey, Path>(128)
private val ovalClipPathCache = LruCache<Rect, Path>(128)

internal val localShadowPaint = ThreadLocal.withInitial {
    android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
}

internal fun getCachedBlurMaskFilter(blurRadiusPx: Float): android.graphics.BlurMaskFilter? {
    if (blurRadiusPx <= 0f) return null
    val key = (blurRadiusPx * 10f).toInt().coerceAtLeast(1)
    synchronized(blurFilterCache) {
        val cached = blurFilterCache.get(key)
        if (cached != null) return cached
        val created = android.graphics.BlurMaskFilter(
            blurRadiusPx.coerceAtLeast(0.5f),
            android.graphics.BlurMaskFilter.Blur.NORMAL
        )
        blurFilterCache.put(key, created)
        return created
    }
}

internal fun getOrCreateBoxPath(
    l: Float, t: Float, w: Float, h: Float,
    tlR: Float, trR: Float, brR: Float, blR: Float,
    isOval: Boolean
): android.graphics.Path {
    val key = BoxPathKey(l, t, w, h, tlR, trR, brR, blR, isOval)
    synchronized(androidBoxPathCache) {
        val cached = androidBoxPathCache.get(key)
        if (cached != null) return cached
    }
    val path = android.graphics.Path().apply {
        if (isOval) {
            addOval(android.graphics.RectF(l, t, l + w, t + h), android.graphics.Path.Direction.CW)
        } else {
            val radii = floatArrayOf(tlR, tlR, trR, trR, brR, brR, blR, blR)
            addRoundRect(android.graphics.RectF(l, t, l + w, t + h), radii, android.graphics.Path.Direction.CW)
        }
    }
    synchronized(androidBoxPathCache) {
        androidBoxPathCache.put(key, path)
    }
    return path
}

internal fun getOrCreateInsetPath(
    boxLeft: Float, boxTop: Float, boxWidth: Float, boxHeight: Float,
    margin: Float,
    holeKey: BoxPathKey
): android.graphics.Path {
    val key = InsetPathKey(boxLeft, boxTop, boxWidth, boxHeight, margin, holeKey)
    synchronized(insetPathCache) {
        val cached = insetPathCache.get(key)
        if (cached != null) return cached
    }
    val holePath = getOrCreateBoxPath(holeKey.l, holeKey.t, holeKey.w, holeKey.h, holeKey.tlR, holeKey.trR, holeKey.brR, holeKey.blR, holeKey.isOval)
    val insetPath = android.graphics.Path().apply {
        fillType = android.graphics.Path.FillType.EVEN_ODD
        addRect(
            boxLeft - margin,
            boxTop - margin,
            boxLeft + boxWidth + margin,
            boxTop + boxHeight + margin,
            android.graphics.Path.Direction.CW
        )
        addPath(holePath)
    }
    synchronized(insetPathCache) {
        insetPathCache.put(key, insetPath)
    }
    return insetPath
}

internal fun getCachedVariablePath(
    l: Float, t: Float, w: Float, h: Float,
    tl: Float, tr: Float, br: Float, bl: Float
): Path {
    val key = VariablePathKey(l, t, w, h, tl, tr, br, bl)
    synchronized(variablePathCache) {
        val cached = variablePathCache.get(key)
        if (cached != null) return cached
    }
    val created = Path().apply {
        addRoundRect(
            androidx.compose.ui.geometry.RoundRect(
                rect = Rect(l, t, l + w, t + h),
                topLeft = CornerRadius(tl, tl),
                topRight = CornerRadius(tr, tr),
                bottomRight = CornerRadius(br, br),
                bottomLeft = CornerRadius(bl, bl)
            )
        )
    }
    synchronized(variablePathCache) {
        variablePathCache.put(key, created)
    }
    return created
}

internal fun getCachedRootClipShape(
    key: RootClipKey,
    builder: () -> Path
): Path {
    synchronized(rootClipShapeCache) {
        val cached = rootClipShapeCache.get(key)
        if (cached != null) return cached
    }
    val created = builder()
    synchronized(rootClipShapeCache) {
        rootClipShapeCache.put(key, created)
    }
    return created
}

internal fun getCachedOvalPath(rect: Rect): Path {
    synchronized(ovalClipPathCache) {
        val cached = ovalClipPathCache.get(rect)
        if (cached != null) return cached
    }
    val created = Path().apply { addOval(rect) }
    synchronized(ovalClipPathCache) {
        ovalClipPathCache.put(rect, created)
    }
    return created
}

fun clearNxprcRenderCaches() {
    synchronized(scaledPathCache) { scaledPathCache.evictAll() }
    synchronized(baseSvgPathCache) { baseSvgPathCache.evictAll() }
    synchronized(polygonPathCache) { polygonPathCache.evictAll() }
    synchronized(androidBoxPathCache) { androidBoxPathCache.evictAll() }
    synchronized(insetPathCache) { insetPathCache.evictAll() }
    synchronized(brushCache) { brushCache.evictAll() }
    synchronized(blurFilterCache) { blurFilterCache.evictAll() }
    synchronized(variablePathCache) { variablePathCache.evictAll() }
    synchronized(rootClipShapeCache) { rootClipShapeCache.evictAll() }
    synchronized(ovalClipPathCache) { ovalClipPathCache.evictAll() }
}

internal fun createBrush(fill: FillBrush, size: Size, topLeft: Offset = Offset.Zero): Brush {
    val key = BrushKey(fill, size.width, size.height, topLeft.x, topLeft.y)
    synchronized(brushCache) {
        val cached = brushCache.get(key)
        if (cached != null) return cached
    }
    val created = computeBrush(fill, size, topLeft)
    synchronized(brushCache) {
        brushCache.put(key, created)
    }
    return created
}

private fun computeBrush(fill: FillBrush, size: Size, topLeft: Offset = Offset.Zero): Brush {
    return when (fill) {
        is FillBrush.Solid -> SolidColor(Color(fill.color))
        is FillBrush.LinearGradient -> {
            val angleRad = Math.toRadians((fill.angleDegrees - 90.0))
            val cx = topLeft.x + size.width / 2f
            val cy = topLeft.y + size.height / 2f
            val halfLen = Math.hypot(size.width.toDouble(), size.height.toDouble()).toFloat() / 2f
            val cos = Math.cos(angleRad).toFloat()
            val sin = Math.sin(angleRad).toFloat()
            val start = Offset(cx - cos * halfLen, cy - sin * halfLen)
            val end = Offset(cx + cos * halfLen, cy + sin * halfLen)

            if (fill.stops.isNotEmpty() && fill.stops.size == fill.colors.size) {
                val colorStops = fill.colors.indices.map { i ->
                    fill.stops[i] to Color(fill.colors[i])
                }.toTypedArray()
                Brush.linearGradient(colorStops = colorStops, start = start, end = end)
            } else {
                Brush.linearGradient(fill.colors.map { Color(it) }, start = start, end = end)
            }
        }
        is FillBrush.RadialGradient -> {
            val cx = topLeft.x + size.width * fill.centerXRatio
            val cy = topLeft.y + size.height * fill.centerYRatio
            // radiusRatio already includes CSS background-size scaling.
            val maxR = size.minDimension * fill.radiusRatio
            if (fill.stops.isNotEmpty() && fill.stops.size == fill.colors.size) {
                val colorStops = fill.colors.indices.map { i ->
                    fill.stops[i] to Color(fill.colors[i])
                }.toTypedArray()
                Brush.radialGradient(
                    colorStops = colorStops,
                    center = Offset(cx, cy),
                    radius = maxR.coerceAtLeast(1f)
                )
            } else {
                Brush.radialGradient(
                    colors = fill.colors.map { Color(it) },
                    center = Offset(cx, cy),
                    radius = maxR.coerceAtLeast(1f)
                )
            }
        }
        is FillBrush.SweepGradient -> {
            val cx = topLeft.x + size.width * fill.centerXRatio
            val cy = topLeft.y + size.height * fill.centerYRatio
            if (fill.colors.size >= 2) {
                // In CSS conic-gradient, 0deg points North (12 o'clock / -Y).
                // Compose Brush.sweepGradient starts at East (3 o'clock / +X).
                // Therefore: standard_angle = css_angle - 90deg.
                val startPos = (((fill.startAngleDegrees - 90f) % 360f + 360f) % 360f) / 360f
                val n = fill.colors.size
                val rawStops = if (fill.stops.size == n) fill.stops else List(n) { it.toFloat() / (n - 1) }
                val samples = 72
                val sampleStops = FloatArray(samples + 1) { it.toFloat() / samples }
                val colorStops = sampleStops.map { s ->
                    val origPos = (s - startPos + 1.0f) % 1.0f
                    s to sampleGradientColor(fill.colors, rawStops, origPos)
                }.toTypedArray()
                Brush.sweepGradient(
                    colorStops = colorStops,
                    center = Offset(cx, cy)
                )
            } else if (fill.colors.isNotEmpty()) {
                Brush.sweepGradient(
                    colors = fill.colors.map { Color(it) },
                    center = Offset(cx, cy)
                )
            } else {
                SolidColor(Color.Transparent)
            }
        }
    }
}

private fun isDarkColor(color: Long): Boolean {
    val r = (color shr 16) and 0xFF
    val g = (color shr 8) and 0xFF
    val b = color and 0xFF
    val brightness = (r * 299 + g * 587 + b * 114) / 1000
    return brightness < 50
}

internal fun buildScaledPath(svgData: String, targetRect: Rect): Path {
    val trimmed = svgData.trim()
    if (trimmed.isBlank()) return Path()

    val key = ScaledPathKey(trimmed, targetRect.left, targetRect.top, targetRect.width, targetRect.height)
    synchronized(scaledPathCache) {
        val cached = scaledPathCache.get(key)
        if (cached != null) return cached
    }

    val calculated = computeScaledPath(trimmed, targetRect)
    synchronized(scaledPathCache) {
        scaledPathCache.put(key, calculated)
    }
    return calculated
}

private fun computeScaledPath(trimmed: String, targetRect: Rect): Path {
    // 1. Android core graphics PathParser with exact Matrix transformation
    try {
        val baseEntry = synchronized(baseSvgPathCache) {
            baseSvgPathCache.get(trimmed)
        } ?: run {
            val androidPath = androidx.core.graphics.PathParser.createPathFromPathData(trimmed)
            val bounds = android.graphics.RectF()
            androidPath.computeBounds(bounds, true)
            if (bounds.width() > 0.001f && bounds.height() > 0.001f) {
                val isNormalized100 = bounds.left >= -5f && bounds.top >= -5f && bounds.right <= 105f && bounds.bottom <= 105f
                val entry = BaseSvgPathEntry(androidPath, bounds, isNormalized100)
                synchronized(baseSvgPathCache) {
                    baseSvgPathCache.put(trimmed, entry)
                }
                entry
            } else null
        }

        if (baseEntry != null) {
            val transformedPath = android.graphics.Path(baseEntry.path)
            val matrix = android.graphics.Matrix().apply {
                if (baseEntry.isNormalized100) {
                    postScale(targetRect.width / 100f, targetRect.height / 100f)
                    postTranslate(targetRect.left, targetRect.top)
                } else {
                    postTranslate(-baseEntry.bounds.left, -baseEntry.bounds.top)
                    postScale(targetRect.width / baseEntry.bounds.width(), targetRect.height / baseEntry.bounds.height())
                    postTranslate(targetRect.left, targetRect.top)
                }
            }
            transformedPath.transform(matrix)
            return transformedPath.asComposePath()
        }
    } catch (_: Throwable) {
        // Fall back to manual token parser below
    }

    // 2. Percentage tokenizer fallback (supports M, L, Z normalized 0..100)
    val path = Path()
    val tokens = trimmed.split(java.util.regex.Pattern.compile("[,\\s]+")).filter { it.isNotEmpty() }
    var i = 0
    while (i < tokens.size) {
        val tok = tokens[i].uppercase()
        when (tok) {
            "M" -> {
                if (i + 2 < tokens.size) {
                    val x = targetRect.left + ((tokens[i + 1].toFloatOrNull() ?: 0f) / 100f) * targetRect.width
                    val y = targetRect.top + ((tokens[i + 2].toFloatOrNull() ?: 0f) / 100f) * targetRect.height
                    path.moveTo(x, y)
                    i += 3
                } else i++
            }
            "L" -> {
                if (i + 2 < tokens.size) {
                    val x = targetRect.left + ((tokens[i + 1].toFloatOrNull() ?: 0f) / 100f) * targetRect.width
                    val y = targetRect.top + ((tokens[i + 2].toFloatOrNull() ?: 0f) / 100f) * targetRect.height
                    path.lineTo(x, y)
                    i += 3
                } else i++
            }
            "Z" -> {
                path.close()
                i++
            }
            else -> i++
        }
    }
    return path
}

internal fun buildRegularPolygonPath(sides: Int, targetRect: Rect): Path {
    if (sides < 3) return Path()
    val key = PolygonKey(sides, targetRect.left, targetRect.top, targetRect.width, targetRect.height)
    synchronized(polygonPathCache) {
        val cached = polygonPathCache.get(key)
        if (cached != null) return cached
    }

    val path = Path()
    val cx = targetRect.center.x
    val cy = targetRect.center.y
    val rx = targetRect.width / 2f
    val ry = targetRect.height / 2f
    val angleStep = (2.0 * Math.PI / sides)
    val startAngle = -Math.PI / 2.0 // start at top

    for (i in 0 until sides) {
        val a = startAngle + i * angleStep
        val x = (cx + rx * Math.cos(a)).toFloat()
        val y = (cy + ry * Math.sin(a)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()

    synchronized(polygonPathCache) {
        polygonPathCache.put(key, path)
    }
    return path
}
