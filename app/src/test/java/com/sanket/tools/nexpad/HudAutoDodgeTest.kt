package com.sanket.tools.nexpad

import androidx.compose.animation.core.Animatable
import androidx.compose.ui.unit.Density
import com.sanket.tools.nexpad.model.HudElement
import com.sanket.tools.nexpad.model.LayoutTransform
import com.sanket.tools.nexpad.ui.hud.HudAutoDodgeCoordinator
import com.sanket.tools.nexpad.ui.hud.InspectorDockTarget
import com.sanket.tools.nexpad.ui.hud.TopBarDockTarget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class HudAutoDodgeTest {

    private val testScope = CoroutineScope(Dispatchers.Unconfined)
    private val density = Density(density = 2.0f, fontScale = 1.0f)
    private lateinit var coordinator: HudAutoDodgeCoordinator

    @Before
    fun setUp() {
        val topBarAnim = Animatable(0f)
        val inspectorAnim = Animatable(600f)

        coordinator = HudAutoDodgeCoordinator(
            topBarAnimatable = topBarAnim,
            inspectorAnimatable = inspectorAnim,
            coroutineScope = testScope,
            density = density
        )
        // Simulate a 1080p landscape screen (1920 x 1080)
        // TopBar height: 56dp = 112px
        // Inspector height: 120dp = 240px
        coordinator.updateDimensions(
            width = 1920f,
            height = 1080f,
            topBarH = 112f,
            inspectorH = 240f
        )
    }

    @Test
    fun testInitialRestPositions() {
        assertEquals(TopBarDockTarget.TOP, coordinator.topBarDockTarget)
        assertEquals(InspectorDockTarget.BOTTOM, coordinator.inspectorDockTarget)
    }

    @Test
    fun testTopBarDodgesWhenButtonNearTop() {
        // Button placed at top edge (yRatio = 0.05 -> center = 54px, buttonTop < 112px + threshold)
        val topElement = HudElement(
            controlKey = "LB",
            transform = LayoutTransform(xRatio = 0.15f, yRatio = 0.05f)
        )

        coordinator.onElementPositionChanged(topElement)

        // Top bar should have dodged out of the way
        assertEquals(TopBarDockTarget.DODGED, coordinator.topBarDockTarget)
        // Inspector should remain at bottom
        assertEquals(InspectorDockTarget.BOTTOM, coordinator.inspectorDockTarget)
    }

    @Test
    fun testTopBarReturnsWhenButtonMovesAway() {
        // First, move button to top to trigger dodge
        val topElement = HudElement(
            controlKey = "LB",
            transform = LayoutTransform(xRatio = 0.15f, yRatio = 0.05f)
        )
        coordinator.onElementPositionChanged(topElement)
        assertEquals(TopBarDockTarget.DODGED, coordinator.topBarDockTarget)

        // Now move button safely down into middle zone (yRatio = 0.40 -> center = 432px)
        val middleElement = HudElement(
            controlKey = "LB",
            transform = LayoutTransform(xRatio = 0.15f, yRatio = 0.40f)
        )
        coordinator.onElementPositionChanged(middleElement)

        // Top bar should smoothly return to TOP
        assertEquals(TopBarDockTarget.TOP, coordinator.topBarDockTarget)
    }

    @Test
    fun testInspectorDodgesWhenButtonNearBottom() {
        // Button placed at bottom edge (yRatio = 0.88 -> center = 950px, bottom edge = 1080 - 240 = 840px)
        val bottomElement = HudElement(
            controlKey = "A",
            transform = LayoutTransform(xRatio = 0.85f, yRatio = 0.88f)
        )

        coordinator.onElementPositionChanged(bottomElement)

        // Inspector must dodge up to TOP so the bottom is 100% open
        assertEquals(InspectorDockTarget.TOP, coordinator.inspectorDockTarget)
        // Top bar stays at TOP (no collision at top)
        assertEquals(TopBarDockTarget.TOP, coordinator.topBarDockTarget)
    }

    @Test
    fun testInspectorReturnsWhenButtonNearTop() {
        // First, trigger inspector to TOP by moving to bottom
        val bottomElement = HudElement(
            controlKey = "A",
            transform = LayoutTransform(xRatio = 0.85f, yRatio = 0.88f)
        )
        coordinator.onElementPositionChanged(bottomElement)
        assertEquals(InspectorDockTarget.TOP, coordinator.inspectorDockTarget)

        // Now move button to the top (yRatio = 0.10f)
        val topElement = HudElement(
            controlKey = "A",
            transform = LayoutTransform(xRatio = 0.85f, yRatio = 0.10f)
        )
        coordinator.onElementPositionChanged(topElement)

        // Inspector must dodge back down to BOTTOM to open the top
        assertEquals(InspectorDockTarget.BOTTOM, coordinator.inspectorDockTarget)
    }

    @Test
    fun testManualDragOverridesAutoDodge() {
        coordinator.isDraggingTopBar = true

        val topElement = HudElement(
            controlKey = "LB",
            transform = LayoutTransform(xRatio = 0.15f, yRatio = 0.05f)
        )
        coordinator.onElementPositionChanged(topElement)

        // Because manual dragging is active on Top Bar, auto-dodge does not override it
        assertEquals(TopBarDockTarget.TOP, coordinator.topBarDockTarget)
    }
}
