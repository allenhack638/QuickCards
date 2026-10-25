package com.quickcards.app.flow

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SheetDismissFlowTest : FlowTestBase() {

    private fun openFilter() {
        val holder = runBlocking { app.repository.createHolder("Aarav Mehta") }
        val bank = runBlocking { app.repository.createBank("HDFC Bank") }
        seedCard("4532778103492211", holder.id, bank.id)
        waitForContentDescription("Filter cards")
        compose.onNodeWithContentDescription("Filter cards").performClick()
        waitForText("Show 1 card")
    }

    @Test
    fun scrimCloseActionDismissesTheSheet() {
        openFilter()
        compose.onNodeWithContentDescription("Close sheet")
            .performSemanticsAction(SemanticsActions.OnClick)
        waitForTextGone("Show 1 card")
    }

    @Test
    fun tappingOutsideDismissesTheSheet() {
        openFilter()
        val sheetTop = compose.onNodeWithText("Filter cards").fetchSemanticsNode().boundsInRoot.top
        compose.onNodeWithContentDescription("Close sheet")
            .performTouchInput { click(Offset(centerX, sheetTop / 2f)) }
        waitForTextGone("Show 1 card")
    }
}
