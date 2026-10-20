package com.sanket.tools.nexpad

import com.sanket.tools.nexpad.ui.NavigationViewModel
import com.sanket.tools.nexpad.ui.OriginScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class NavigationViewModelTest {

    private lateinit var viewModel: NavigationViewModel

    @Before
    fun setUp() {
        viewModel = NavigationViewModel()
    }

    @Test
    fun testBeginAssetSelectionTrimsAndInitializesContext() {
        assertNull(viewModel.editingContext.value)

        viewModel.beginAssetSelection(
            profileName = "  Standard Elite  ",
            controlKey = "  A  ",
            currentAssetId = "  rc.anime_a  ",
            originScreen = OriginScreen.HUD_EDITOR
        )

        val ctx = viewModel.editingContext.value
        assertNotNull(ctx)
        assertEquals("Standard Elite", ctx!!.profileName)
        assertEquals("A", ctx.controlKey)
        assertEquals("rc.anime_a", ctx.currentAssetId)
        assertEquals(OriginScreen.HUD_EDITOR, ctx.originScreen)
        assertNull(ctx.pendingAssetResult)
    }

    @Test
    fun testCommitAndConsumeAssetResult() {
        viewModel.beginAssetSelection(
            profileName = "Standard Elite",
            controlKey = "A",
            currentAssetId = null,
            originScreen = OriginScreen.VIRTUAL_CONTROLLER
        )

        viewModel.commitAssetSelection("  rc.neon_a  ")

        assertEquals("rc.neon_a", viewModel.editingContext.value?.pendingAssetResult)

        // Consuming must return the result and atomically null the context
        val result = viewModel.consumeAssetResult()
        assertEquals("rc.neon_a", result)
        assertNull(viewModel.editingContext.value)

        // Second consume returns null
        assertNull(viewModel.consumeAssetResult())
    }

    @Test
    fun testCancelAssetSelectionNullsContext() {
        viewModel.beginAssetSelection(
            profileName = "Standard Elite",
            controlKey = "B",
            currentAssetId = null,
            originScreen = OriginScreen.HUD_EDITOR
        )

        viewModel.cancelAssetSelection()
        assertNull(viewModel.editingContext.value)
    }

    @Test
    fun testTransientGamepadSession() {
        assertNull(viewModel.sessionProfileName.value)

        viewModel.startGamepadSession("  Racing Pro  ")
        assertEquals("Racing Pro", viewModel.sessionProfileName.value)

        viewModel.clearGamepadSession()
        assertNull(viewModel.sessionProfileName.value)
    }
}
