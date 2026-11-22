package com.quickcards.app.flow

import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

/** The Filter sheet's chips toggle, narrow the count on the Show button, and Clear all resets them. */
@RunWith(AndroidJUnit4::class)
class FilterSheetFlowTest : FlowTestBase() {

    private fun diyaChip() = compose.onNode(hasText("Diya Mehta") and isToggleable())

    @Test
    fun aChipNarrowsTheCountAndClearAllResetsIt() {
        val aarav = runBlocking { app.repository.createHolder("Aarav Mehta") }
        val diya = runBlocking { app.repository.createHolder("Diya Mehta") }
        val bank = runBlocking { app.repository.createBank("HDFC Bank") }
        seedCard("4532778103492211", aarav.id, bank.id)
        seedCard("4532015112830366", diya.id, bank.id)
        waitForContentDescription("Filter cards")
        compose.onNodeWithContentDescription("Filter cards").performClick()
        waitForText("Show 2 cards")

        diyaChip().performClick()
        diyaChip().assertIsOn()
        waitForText("Show 1 card")

        compose.onNodeWithText("Clear all").performClick()
        diyaChip().assertIsOff()
        waitForText("Show 2 cards")
    }
}
