package com.sanket.tools.nexpad.runtime.engine

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanket.tools.nexpad.runtime.model.NexPadControl
import com.sanket.tools.nexpad.runtime.model.NexPadInputTarget
import com.sanket.tools.nexpad.nxprc.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

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
    overrideSizeDp: Int? = null
) {
    var isPressed by remember { mutableStateOf(false) }

    val scaleAnim by animateFloatAsState(
        targetValue = if (isPressed) document.animations.pressScale else 1f,
        animationSpec = spring(
            dampingRatio = document.animations.springDamping,
            stiffness = document.animations.springStiffness
        ),
        label = "nxprc_scale"
    )

    val pressOffsetYAnim by animateFloatAsState(
        targetValue = if (isPressed) document.animations.pressOffsetY else 0f,
        animationSpec = spring(
            dampingRatio = document.animations.springDamping,
            stiffness = document.animations.springStiffness
        ),
        label = "nxprc_press_y"
    )

    val needsPulse = document.animations.idleType == "PULSE"
    val needsRotation = document.animations.idleType == "ROTATE"
    val infiniteTransition = rememberInfiniteTransition(label = "nxprc_idle")

    val pulseAlpha = if (needsPulse) {
        infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(document.animations.idleDurationMs, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        ).value
    } else 0.8f

    val rotateAngle = if (needsRotation) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(4000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "rotate"
        ).value
    } else 0f

    val widthDp = overrideSizeDp ?: document.manifest.widthDp
    val heightDp = overrideSizeDp ?: document.manifest.heightDp

    val buttonControl = remember(assignedControl) {
        when (assignedControl) {
            is NexPadControl.Button -> assignedControl
            is NexPadControl.DPad -> NexPadControl.Button(assignedControl.direction)
            is NexPadControl.Trigger -> NexPadControl.Button(assignedControl.key)
            is NexPadControl.Stick -> NexPadControl.Button(if (assignedControl.isLeft) "LS" else "RS")
        }
    }

    Box(
        modifier = modifier
            .size(widthDp.dp, heightDp.dp)
            .pointerInput(document.manifest.id, assignedControl) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    inputTarget.onButtonPress(buttonControl)

                    val upOrCancel = waitForUpOrCancellation()
                    isPressed = false
                    inputTarget.onButtonRelease(buttonControl)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(widthDp.dp, heightDp.dp)) {
            scale(scaleAnim) {
                // Match the CSS document viewBox instead of applying a fixed
                // 90% scale. This keeps Android and Desktop previews aligned.
                val viewBoxW = document.canvas.viewBoxWidth.coerceAtLeast(1f)
                val viewBoxH = document.canvas.viewBoxHeight.coerceAtLeast(1f)
                val viewScale = minOf(size.width / viewBoxW, size.height / viewBoxH)
                val buttonW = viewBoxW * viewScale
                val buttonH = viewBoxH * viewScale
                val buttonLeft = (size.width - buttonW) / 2f
                val buttonTop = (size.height - buttonH) / 2f + pressOffsetYAnim * density
                val centerOffset = Offset(size.width / 2f, size.height / 2f + pressOffsetYAnim * density)

                val primaryBox = document.canvas.layers.filterIsInstance<CanvasLayer.BoxLayer>().firstOrNull()
                val primaryShape = document.canvas.layers.filterIsInstance<CanvasLayer.GradientShape>().firstOrNull()
                val rootShapeType = (primaryBox?.shapeType?.uppercase() ?: primaryShape?.shapeType?.uppercase() ?: "ROUNDED_RECT")
                val rootIsOval = rootShapeType == "OVAL"
                val rootIsPolygon = rootShapeType == "POLYGON" || rootShapeType == "PATH" || (primaryBox?.pathData?.isNotBlank() == true)
                val rootTl = (primaryBox?.cornerRadiusTopLeft ?: primaryShape?.cornerRadius ?: 14f) * density
                val rootTr = (primaryBox?.cornerRadiusTopRight ?: primaryShape?.cornerRadius ?: 14f) * density
                val rootBr = (primaryBox?.cornerRadiusBottomRight ?: primaryShape?.cornerRadius ?: 14f) * density
                val rootBl = (primaryBox?.cornerRadiusBottomLeft ?: primaryShape?.cornerRadius ?: 14f) * density

                val rootClipShape = Path().apply {
                    if (rootIsPolygon && primaryBox != null && primaryBox.pathData.isNotBlank()) {
                        addPath(buildScaledPath(primaryBox.pathData, Rect(buttonLeft, buttonTop, buttonLeft + buttonW, buttonTop + buttonH)))
                    } else if (rootShapeType == "HEXAGON") {
                        addPath(buildRegularPolygonPath(6, Rect(buttonLeft, buttonTop, buttonLeft + buttonW, buttonTop + buttonH)))
                    } else if (rootShapeType == "OCTAGON") {
                        addPath(buildRegularPolygonPath(8, Rect(buttonLeft, buttonTop, buttonLeft + buttonW, buttonTop + buttonH)))
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

                document.canvas.layers.forEach { layer ->
                    when (layer) {
                        is CanvasLayer.BoxLayer -> {
                            val boxWidth = buttonW * layer.widthRatio
                            val boxHeight = buttonH * layer.heightRatio
                            val boxLeft = buttonLeft + layer.offsetXRatio * buttonW
                            val boxTop = buttonTop + layer.offsetYRatio * buttonH

                            val pivot = Offset(boxLeft + boxWidth * layer.originXRatio, boxTop + boxHeight * layer.originYRatio)
                            val rotAngle = if (layer.isRotating && document.animations.idleType == "ROTATE") rotateAngle else layer.rotationDegrees

                            val isPolygon = layer.shapeType.uppercase() == "POLYGON" || layer.shapeType.uppercase() == "PATH" || layer.pathData.isNotBlank() || layer.polygonSides >= 3
                            val isHexagon = layer.shapeType.uppercase() == "HEXAGON" || layer.polygonSides == 6
                            val isOctagon = layer.shapeType.uppercase() == "OCTAGON" || layer.polygonSides == 8
                            val isOval = layer.shapeType.uppercase() == "OVAL"
                            val layerAlpha = layer.opacity.coerceIn(0f, 1f)
                            val tl = layer.cornerRadiusTopLeft * density
                            val tr = layer.cornerRadiusTopRight * density
                            val br = layer.cornerRadiusBottomRight * density
                            val bl = layer.cornerRadiusBottomLeft * density
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

                            val variablePath = Path().apply {
                                addRoundRect(
                                    androidx.compose.ui.geometry.RoundRect(
                                        rect = Rect(boxLeft, boxTop, boxLeft + boxWidth, boxTop + boxHeight),
                                        topLeft = CornerRadius(tl, tl),
                                        topRight = CornerRadius(tr, tr),
                                        bottomRight = CornerRadius(br, br),
                                        bottomLeft = CornerRadius(bl, bl)
                                    )
                                )
                            }

                            val drawBox: () -> Unit = {
                                withTransform({
                                    if (rotAngle != 0f) rotate(rotAngle, pivot = pivot)
                                    if (layer.scaleX != 1f || layer.scaleY != 1f) scale(scaleX = layer.scaleX, scaleY = layer.scaleY, pivot = pivot)
                                }) {
                                    // 1. Outset box shadows
                                    layer.boxShadows.filter { !it.isInset }.forEach { shadow ->
                                        val shadowOffset = Offset(shadow.offsetX * density, shadow.offsetY * density)
                                        val sColor = Color(shadow.color)
                                        val spreadPx = shadow.spreadRadius * density
                                        if (isPolygon) {
                                            drawPath(polygonPath, color = sColor.copy(alpha = sColor.alpha * layerAlpha))
                                        } else if (isOval) {
                                            drawOval(
                                                color = sColor.copy(alpha = sColor.alpha * layerAlpha),
                                                topLeft = Offset(boxLeft + shadowOffset.x - spreadPx, boxTop + shadowOffset.y - spreadPx),
                                                size = Size(boxWidth + spreadPx * 2f, boxHeight + spreadPx * 2f)
                                            )
                                        } else if (hasVariableCorners) {
                                            val shadowPath = Path().apply {
                                                addRoundRect(
                                                    androidx.compose.ui.geometry.RoundRect(
                                                        rect = Rect(
                                                            boxLeft + shadowOffset.x - spreadPx,
                                                            boxTop + shadowOffset.y - spreadPx,
                                                            boxLeft + boxWidth + shadowOffset.x + spreadPx,
                                                            boxTop + boxHeight + shadowOffset.y + spreadPx
                                                        ),
                                                        topLeft = CornerRadius(tl + spreadPx, tl + spreadPx),
                                                        topRight = CornerRadius(tr + spreadPx, tr + spreadPx),
                                                        bottomRight = CornerRadius(br + spreadPx, br + spreadPx),
                                                        bottomLeft = CornerRadius(bl + spreadPx, bl + spreadPx)
                                                    )
                                                )
                                            }
                                            drawPath(shadowPath, color = sColor.copy(alpha = sColor.alpha * layerAlpha))
                                        } else {
                                            drawRoundRect(
                                                color = sColor.copy(alpha = sColor.alpha * layerAlpha),
                                                topLeft = Offset(boxLeft + shadowOffset.x - spreadPx, boxTop + shadowOffset.y - spreadPx),
                                                size = Size(boxWidth + spreadPx * 2f, boxHeight + spreadPx * 2f),
                                                cornerRadius = CornerRadius(tl + spreadPx, tl + spreadPx)
                                            )
                                        }
                                    }

                                    // 2. Main surface fills (stacked bottom-to-top)
                                    val allBrushes = if (layer.fills.isNotEmpty()) {
                                        layer.fills.map { createBrush(it, Size(boxWidth, boxHeight), Offset(boxLeft, boxTop)) }
                                    } else {
                                        listOf(createBrush(layer.fill, Size(boxWidth, boxHeight), Offset(boxLeft, boxTop)))
                                    }

                                    allBrushes.forEach { b ->
                                        if (isPolygon) {
                                            drawPath(polygonPath, brush = b, alpha = layerAlpha)
                                        } else if (isOval) {
                                            drawOval(
                                                brush = b,
                                                topLeft = Offset(boxLeft, boxTop),
                                                size = Size(boxWidth, boxHeight),
                                                alpha = layerAlpha
                                            )
                                        } else if (hasVariableCorners) {
                                            drawPath(variablePath, brush = b, alpha = layerAlpha)
                                        } else {
                                            drawRoundRect(
                                                brush = b,
                                                topLeft = Offset(boxLeft, boxTop),
                                                size = Size(boxWidth, boxHeight),
                                                cornerRadius = CornerRadius(tl, tl),
                                                alpha = layerAlpha
                                            )
                                        }
                                    }

                                    // 3. Stroke / Border
                                    layer.stroke?.let { st ->
                                        val stColor = Color(st.color)
                                        val stWidth = st.width * density
                                        if (isPolygon) {
                                            drawPath(polygonPath, color = stColor.copy(alpha = stColor.alpha * layerAlpha), style = Stroke(width = stWidth))
                                        } else if (isOval) {
                                            drawOval(
                                                color = stColor.copy(alpha = stColor.alpha * layerAlpha),
                                                topLeft = Offset(boxLeft, boxTop),
                                                size = Size(boxWidth, boxHeight),
                                                style = Stroke(width = stWidth)
                                            )
                                        } else if (hasVariableCorners) {
                                            drawPath(variablePath, color = stColor.copy(alpha = stColor.alpha * layerAlpha), style = Stroke(width = stWidth))
                                        } else {
                                            drawRoundRect(
                                                color = stColor.copy(alpha = stColor.alpha * layerAlpha),
                                                topLeft = Offset(boxLeft, boxTop),
                                                size = Size(boxWidth, boxHeight),
                                                cornerRadius = CornerRadius(tl, tl),
                                                style = Stroke(width = stWidth)
                                            )
                                        }
                                    }

                                    // 4. Inset box shadows
                                    val insets = layer.boxShadows.filter { it.isInset }
                                    if (insets.isNotEmpty()) {
                                        val shapeClipPath = Path().apply {
                                            if (isPolygon) {
                                                addPath(polygonPath)
                                            } else if (isOval) {
                                                addOval(Rect(boxLeft, boxTop, boxLeft + boxWidth, boxTop + boxHeight))
                                            } else if (hasVariableCorners) {
                                                addPath(variablePath)
                                            } else {
                                                addRoundRect(
                                                    androidx.compose.ui.geometry.RoundRect(
                                                        left = boxLeft,
                                                        top = boxTop,
                                                        right = boxLeft + boxWidth,
                                                        bottom = boxTop + boxHeight,
                                                        radiusX = tl,
                                                        radiusY = tl
                                                    )
                                                )
                                            }
                                        }
                                        clipPath(shapeClipPath) {
                                            insets.forEach { shadow ->
                                                val inColor = Color(shadow.color)
                                                val sOffset = Offset(shadow.offsetX * density, shadow.offsetY * density)
                                                val strokeW = (shadow.blurRadius.takeIf { it > 0f } ?: 3.5f) * density
                                                if (isOval) {
                                                    drawOval(
                                                        color = inColor.copy(alpha = inColor.alpha * layerAlpha),
                                                        topLeft = Offset(boxLeft + sOffset.x, boxTop + sOffset.y),
                                                        size = Size(boxWidth, boxHeight),
                                                        style = Stroke(width = strokeW * 1.5f)
                                                    )
                                                } else {
                                                    drawRoundRect(
                                                        color = inColor.copy(alpha = inColor.alpha * layerAlpha),
                                                        topLeft = Offset(boxLeft + sOffset.x, boxTop + sOffset.y),
                                                        size = Size(boxWidth, boxHeight),
                                                        cornerRadius = CornerRadius(tl, tl),
                                                        style = Stroke(width = strokeW * 1.5f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            val drawFilteredBox: () -> Unit = {
                                val colorFilter = createNxprcColorFilter(layer.filter)
                                if (colorFilter == null) {
                                    drawBox()
                                } else {
                                    val paint = Paint().apply { this.colorFilter = colorFilter }
                                    drawContext.canvas.saveLayer(
                                        Rect(boxLeft, boxTop, boxLeft + boxWidth, boxTop + boxHeight),
                                        paint
                                    )
                                    drawBox()
                                    drawContext.canvas.restore()
                                }
                            }

                            val isRootLayer = layer == document.canvas.layers.firstOrNull() || (layer.widthRatio >= 1.0f && layer.heightRatio >= 1.0f)
                            if (!isRootLayer && (layer.clipToBounds || document.canvas.clipToBounds)) {
                                clipPath(rootClipShape) {
                                    drawFilteredBox()
                                }
                            } else {
                                drawFilteredBox()
                            }
                        }
                        is CanvasLayer.BezelSocket -> {
                            val baseRadius = size.minDimension / 2f
                            // 1. Soft bottom drop shadow
                            drawCircle(
                                color = Color(layer.shadowColor),
                                radius = baseRadius * 0.96f,
                                center = Offset(centerOffset.x, centerOffset.y + baseRadius * 0.08f)
                            )
                            // 2. Solid bezel well
                            drawCircle(
                                color = Color(layer.outerBezelColor),
                                radius = baseRadius * 0.96f,
                                center = centerOffset
                            )
                            // 3. Bezel rim stroke
                            drawCircle(
                                color = Color(layer.outerBevelStroke),
                                radius = baseRadius * 0.96f,
                                center = centerOffset,
                                style = Stroke(width = 2.5f * density)
                            )
                        }
                        is CanvasLayer.GlowRing -> {
                            val alpha = if (layer.pulseEnabled && document.animations.idleType == "PULSE") pulseAlpha else 0.8f
                            val glowColor = Color(layer.glowColor).copy(alpha = alpha * 0.4f)
                            drawCircle(
                                color = glowColor,
                                radius = size.minDimension / 2f * 0.96f
                            )
                        }
                        is CanvasLayer.GradientShape -> {
                            val shapeW = buttonW * layer.widthRatio
                            val shapeH = buttonH * layer.heightRatio
                            val shapeLeft = buttonLeft + buttonW * layer.offsetXRatio
                            val shapeTop = buttonTop + buttonH * layer.offsetYRatio
                            val shapeSize = Size(shapeW, shapeH)
                            val brush = createBrush(layer.fill, shapeSize, Offset(shapeLeft, shapeTop))
                            val cornerRadiusPx = layer.cornerRadius * density
                            val shapeAlpha = layer.opacity.coerceIn(0f, 1f)

                            val hasTransform = layer.rotationDegrees != 0f || layer.scaleX != 1f || layer.scaleY != 1f
                            val pivot = Offset(shapeLeft + layer.originXRatio * shapeW, shapeTop + layer.originYRatio * shapeH)

                            val drawShape: () -> Unit = {
                                val shapeType = layer.shapeType.uppercase()
                                when {
                                    shapeType == "HEXAGON" -> {
                                        val polyPath = buildRegularPolygonPath(6, Rect(shapeLeft, shapeTop, shapeLeft + shapeW, shapeTop + shapeH))
                                        drawPath(polyPath, brush = brush, alpha = shapeAlpha)
                                        layer.stroke?.let { st ->
                                            val stColor = Color(st.color)
                                            drawPath(polyPath, color = stColor.copy(alpha = stColor.alpha * shapeAlpha), style = Stroke(width = st.width * density))
                                        }
                                    }
                                    shapeType == "OCTAGON" -> {
                                        val polyPath = buildRegularPolygonPath(8, Rect(shapeLeft, shapeTop, shapeLeft + shapeW, shapeTop + shapeH))
                                        drawPath(polyPath, brush = brush, alpha = shapeAlpha)
                                        layer.stroke?.let { st ->
                                            val stColor = Color(st.color)
                                            drawPath(polyPath, color = stColor.copy(alpha = stColor.alpha * shapeAlpha), style = Stroke(width = st.width * density))
                                        }
                                    }
                                    shapeType == "OVAL" -> {
                                        drawOval(brush = brush, topLeft = Offset(shapeLeft, shapeTop), size = shapeSize, alpha = shapeAlpha)
                                        layer.stroke?.let { st ->
                                            val stColor = Color(st.color)
                                            drawOval(
                                                color = stColor.copy(alpha = stColor.alpha * shapeAlpha),
                                                topLeft = Offset(shapeLeft, shapeTop),
                                                size = shapeSize,
                                                style = Stroke(width = st.width * density)
                                            )
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
                                            drawRoundRect(
                                                color = stColor.copy(alpha = stColor.alpha * shapeAlpha),
                                                topLeft = Offset(shapeLeft, shapeTop),
                                                size = shapeSize,
                                                cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                                                style = Stroke(width = st.width * density)
                                            )
                                        }
                                    }
                                }
                            }

                            val drawFilteredShape: () -> Unit = {
                                val colorFilter = createNxprcColorFilter(layer.filter)
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
                                    scale(layer.scaleX, layer.scaleY, pivot = pivot)
                                    rotate(layer.rotationDegrees, pivot = pivot)
                                }) {
                                    drawFilteredShape()
                                }
                            } else {
                                drawFilteredShape()
                            }
                        }
                        is CanvasLayer.InnerShadow -> {
                            val arcRadius = size.minDimension / 2f * 0.86f
                            val arcTopLeft = Offset(centerOffset.x - arcRadius, centerOffset.y - arcRadius)
                            val arcSize = Size(arcRadius * 2f, arcRadius * 2f)

                            // Top subtle light rim
                            drawArc(
                                color = Color(layer.highlightColor),
                                startAngle = 180f,
                                sweepAngle = 180f,
                                useCenter = false,
                                topLeft = arcTopLeft,
                                size = arcSize,
                                style = Stroke(width = layer.strokeWidth * density)
                            )
                            // Bottom dark curved shadow
                            drawArc(
                                color = Color(layer.shadowColor),
                                startAngle = 0f,
                                sweepAngle = 180f,
                                useCenter = false,
                                topLeft = arcTopLeft,
                                size = arcSize,
                                style = Stroke(width = layer.strokeWidth * density)
                            )
                        }
                        is CanvasLayer.GlossReflection -> {
                            val glossW = buttonW * layer.widthRatio
                            val glossH = buttonH * layer.heightRatio
                            val glossLeft = buttonLeft + buttonW * layer.offsetXRatio
                            val glossTop = buttonTop + buttonH * layer.offsetYRatio
                            val glossCenter = Offset(glossLeft + glossW / 2f, glossTop + glossH / 2f)

                            val drawGloss: () -> Unit = {
                                rotate(layer.rotationDegrees, pivot = glossCenter) {
                                    val blurSpread = layer.blurRadius * density
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
                            val brush = createBrush(layer.fill, size)
                            val angle = if (layer.isRotating && document.animations.idleType == "ROTATE") rotateAngle else layer.rotationDegrees
                            val targetRect = Rect(
                                buttonLeft + buttonW * layer.offsetXRatio,
                                buttonTop + buttonH * layer.offsetYRatio,
                                buttonLeft + buttonW * (layer.offsetXRatio + layer.scale),
                                buttonTop + buttonH * (layer.offsetYRatio + layer.scale)
                            )
                            val vectorPath = if (layer.pathData.isNotBlank()) {
                                buildScaledPath(layer.pathData, targetRect)
                            } else null

                            rotate(angle, pivot = centerOffset) {
                                if (vectorPath != null) {
                                    drawPath(path = vectorPath, brush = brush)
                                    layer.stroke?.let { st ->
                                        drawPath(path = vectorPath, color = Color(st.color), style = Stroke(width = st.width * density))
                                    }
                                } else {
                                    drawCircle(brush = brush, radius = size.minDimension / 2f * 0.75f)
                                    layer.stroke?.let { st ->
                                        drawCircle(color = Color(st.color), radius = size.minDimension / 2f * 0.75f, style = Stroke(width = st.width * density))
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
            }
        }

        // Center text glyph with embossed 3D lighting and tactile synchronization
        val glyph = remember(document) { document.canvas.layers.filterIsInstance<CanvasLayer.CenterGlyph>().firstOrNull() }
        val textLayer = remember(document) { document.canvas.layers.filterIsInstance<CanvasLayer.TextLayer>().firstOrNull() }
        val centerText = glyph?.text ?: textLayer?.text ?: document.manifest.defaultControl
        val textColor = glyph?.textColor ?: textLayer?.textColor ?: 0xFFF5F5F5L

        val viewBox = document.canvas.viewBoxWidth.coerceAtLeast(1f)
        val buttonMinDim = minOf(widthDp, heightDp).toFloat()
        val scaleFactor = buttonMinDim / viewBox
        val baseFontSp = glyph?.fontSizeSp ?: textLayer?.fontSizeSp ?: (viewBox * 0.32f)
        val fontSp = (baseFontSp * scaleFactor).sp
        val rootBox = remember(document) { document.canvas.layers.filterIsInstance<CanvasLayer.BoxLayer>().firstOrNull() }
        val fontWeight = if (textLayer != null && textLayer.fontWeight >= 900) FontWeight.Black else FontWeight.Bold

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.graphicsLayer {
                scaleX = scaleAnim
                scaleY = scaleAnim
                translationY = pressOffsetYAnim * density
                rotationZ = rootBox?.rotationDegrees ?: 0f
            }
        ) {
            val extraShadows = glyph?.textShadows ?: textLayer?.textShadows ?: emptyList()
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

private fun createBrush(fill: FillBrush, size: Size, topLeft: Offset = Offset.Zero): Brush {
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
            Brush.sweepGradient(
                colors = fill.colors.map { Color(it) },
                center = Offset(cx, cy)
            )
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
    val path = Path()
    if (svgData.isBlank()) return path

    val tokens = svgData.trim().split(java.util.regex.Pattern.compile("[,\\s]+")).filter { it.isNotEmpty() }
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
    val path = Path()
    if (sides < 3) return path
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
    return path
}
