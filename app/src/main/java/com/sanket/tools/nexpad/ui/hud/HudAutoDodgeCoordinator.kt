package com.sanket.tools.nexpad.ui.hud

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.*
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.sanket.tools.nexpad.model.HudElement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

enum class InspectorDockTarget {
    TOP,
    BOTTOM
}

enum class TopBarDockTarget {
    TOP,
    DODGED
}

/**
 * Intelligent Collision-Avoidance & Auto-Dodge Coordinator for the HUD Editor.
 *
 * Dynamically computes element bounding envelopes against the Top Navigation Bar
 * and Docked Inspector overlays. Automatically displaces panels with smooth spring
 * physics and hysteresis dead-bands so buttons are never obscured during placement.
 */
class HudAutoDodgeCoordinator(
    val topBarAnimatable: Animatable<Float, AnimationVector1D>,
    val inspectorAnimatable: Animatable<Float, AnimationVector1D>,
    val coroutineScope: CoroutineScope,
    val density: Density
) {
    var inspectorDockTarget by mutableStateOf(InspectorDockTarget.BOTTOM)
    var topBarDockTarget by mutableStateOf(TopBarDockTarget.TOP)

    var isDraggingTopBar by mutableStateOf(false)
    var isDraggingInspector by mutableStateOf(false)
    var userHasManuallyDraggedInspector by mutableStateOf(false)
    var userHasManuallyDraggedTopBar by mutableStateOf(false)

    var screenWidthPx by mutableFloatStateOf(0f)
    var screenHeightPx by mutableFloatStateOf(0f)
    var topBarHeightPx by mutableFloatStateOf(0f)
    var inspectorHeightPx by mutableFloatStateOf(0f)

    val maxTopBarOffsetY: Float
        get() = (screenHeightPx - topBarHeightPx).coerceAtLeast(0f)

    val maxInspectorOffsetY: Float
        get() = (screenHeightPx - inspectorHeightPx).coerceAtLeast(0f)

    fun updateDimensions(width: Float, height: Float, topBarH: Float, inspectorH: Float) {
        screenWidthPx = width
        screenHeightPx = height
        if (topBarH > 0f) topBarHeightPx = topBarH
        if (inspectorH > 0f) inspectorHeightPx = inspectorH
    }

    /**
     * Evaluates real-time button bounds and triggers auto-dodge transitions
     * with hysteresis dead-bands to prevent thrashing or rapid oscillations.
     */
    fun onElementPositionChanged(element: HudElement?) {
        if (screenHeightPx <= 0f || topBarHeightPx <= 0f) return

        if (element == null) {
            onElementDeselected()
            return
        }

        val buttonCenterY = element.transform.yRatio * screenHeightPx
        val bufferPx = with(density) {
            (32.dp.toPx() * element.transform.scale).coerceIn(24.dp.toPx(), 72.dp.toPx())
        }
        val buttonTopPx = buttonCenterY - bufferPx
        val buttonBottomPx = buttonCenterY + bufferPx

        var inspectorTargetChanged = false
        var topBarTargetChanged = false

        // 1. Inspector Collision Detection (Only if user hasn't locked it with an active manual drag)
        if (inspectorHeightPx > 0f && !isDraggingInspector) {
            val bottomDockY = (screenHeightPx - inspectorHeightPx).coerceAtLeast(0f)
            val topDockY = if (topBarDockTarget == TopBarDockTarget.TOP) {
                topBarHeightPx + with(density) { 8.dp.toPx() }
            } else {
                with(density) { 8.dp.toPx() }
            }

            val prevInspectorTarget = inspectorDockTarget
            when (inspectorDockTarget) {
                InspectorDockTarget.BOTTOM -> {
                    // Encroaching on bottom: button encroaches into the bottom dock zone
                    val bottomDangerThreshold = bottomDockY - with(density) { 16.dp.toPx() }
                    if (buttonBottomPx > bottomDangerThreshold) {
                        inspectorDockTarget = InspectorDockTarget.TOP
                    }
                }
                InspectorDockTarget.TOP -> {
                    // Encroaching on top: button encroaches into the top dock zone
                    val topDangerThreshold = topDockY + inspectorHeightPx + with(density) { 20.dp.toPx() }
                    if (buttonTopPx < topDangerThreshold) {
                        inspectorDockTarget = InspectorDockTarget.BOTTOM
                    }
                }
            }
            if (inspectorDockTarget != prevInspectorTarget) {
                inspectorTargetChanged = true
                userHasManuallyDraggedInspector = false
            }
        }

        // 2. Top Navigation Bar Collision Detection
        if (!isDraggingTopBar) {
            val topDangerThreshold = topBarHeightPx + with(density) { 18.dp.toPx() }
            val topSafeClearanceThreshold = topBarHeightPx + with(density) { 64.dp.toPx() }

            val prevTopBarTarget = topBarDockTarget
            when (topBarDockTarget) {
                TopBarDockTarget.TOP -> {
                    // Button moves under or near Top Bar
                    if (buttonTopPx < topDangerThreshold) {
                        topBarDockTarget = TopBarDockTarget.DODGED
                    }
                }
                TopBarDockTarget.DODGED -> {
                    // Button moves safely down and away from the top area
                    if (buttonTopPx > topSafeClearanceThreshold) {
                        topBarDockTarget = TopBarDockTarget.TOP
                    }
                }
            }
            if (topBarDockTarget != prevTopBarTarget) {
                topBarTargetChanged = true
                userHasManuallyDraggedTopBar = false
            }
        }

        // 3. Dispatch smooth spring animations if dock targets changed
        if (inspectorTargetChanged && !isDraggingInspector) {
            animateInspector()
        }
        if (topBarTargetChanged && !isDraggingTopBar) {
            animateTopBar(hasActiveElement = true)
        }
    }

    fun onElementDeselected() {
        if (topBarDockTarget != TopBarDockTarget.TOP && !isDraggingTopBar && !userHasManuallyDraggedTopBar) {
            topBarDockTarget = TopBarDockTarget.TOP
            animateTopBar(hasActiveElement = false)
        }
    }

    fun toggleTopBarDock() {
        userHasManuallyDraggedTopBar = true
        topBarDockTarget = if (topBarDockTarget == TopBarDockTarget.TOP) {
            TopBarDockTarget.DODGED
        } else {
            TopBarDockTarget.TOP
        }
        animateTopBar(hasActiveElement = false)
    }

    fun toggleInspectorDock() {
        userHasManuallyDraggedInspector = true
        inspectorDockTarget = if (inspectorDockTarget == InspectorDockTarget.BOTTOM) {
            InspectorDockTarget.TOP
        } else {
            InspectorDockTarget.BOTTOM
        }
        animateInspector()
    }

    fun onTopBarDragEnd() {
        isDraggingTopBar = false
        userHasManuallyDraggedTopBar = true
        topBarDockTarget = if (topBarAnimatable.value > maxTopBarOffsetY / 2f) {
            TopBarDockTarget.DODGED
        } else {
            TopBarDockTarget.TOP
        }
        animateTopBar(hasActiveElement = false)
    }

    fun onInspectorDragEnd() {
        isDraggingInspector = false
        userHasManuallyDraggedInspector = true
        inspectorDockTarget = if (inspectorAnimatable.value < maxInspectorOffsetY / 2f) {
            InspectorDockTarget.TOP
        } else {
            InspectorDockTarget.BOTTOM
        }
        animateInspector()
    }

    fun animateInspector() {
        val targetY = when (inspectorDockTarget) {
            InspectorDockTarget.BOTTOM -> (screenHeightPx - inspectorHeightPx).coerceAtLeast(0f)
            InspectorDockTarget.TOP -> {
                val topOffset = if (topBarDockTarget == TopBarDockTarget.TOP) {
                    topBarHeightPx + with(density) { 8.dp.toPx() }
                } else {
                    with(density) { 8.dp.toPx() }
                }
                topOffset.coerceAtMost(maxInspectorOffsetY)
            }
        }

        coroutineScope.launch {
            inspectorAnimatable.animateTo(
                targetValue = targetY,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = 500f
                )
            )
        }
    }

    fun animateTopBar(hasActiveElement: Boolean = false) {
        val targetY = when (topBarDockTarget) {
            TopBarDockTarget.TOP -> 0f
            TopBarDockTarget.DODGED -> {
                if (hasActiveElement && inspectorHeightPx > 0f && inspectorDockTarget == InspectorDockTarget.BOTTOM) {
                    val inspectorTop = screenHeightPx - inspectorHeightPx
                    (inspectorTop - topBarHeightPx - with(density) { 8.dp.toPx() }).coerceAtLeast(0f)
                } else {
                    (screenHeightPx - topBarHeightPx).coerceAtLeast(0f)
                }
            }
        }

        coroutineScope.launch {
            topBarAnimatable.animateTo(
                targetValue = targetY,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = 500f
                )
            )
        }
    }
}
