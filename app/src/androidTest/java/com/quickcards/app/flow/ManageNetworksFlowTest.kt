package com.quickcards.app.flow

import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** A long network label is accepted in Manage and shows on the pass. */
@RunWith(AndroidJUnit4::class)
class ManageNetworksFlowTest : FlowTestBase() {

    @Test
    fun aLongNetworkLabelShowsOnThePass() {
        compose.onNodeWithText("Manage").performClick()
        waitForText("Card networks")
        compose.onNodeWithText("Card networks").performClick()

        waitForContentDescription("Add card network")
        compose.onNodeWithContentDescription("Add card network").performClick()
        waitForText("New card network")

        field("Name").performTextInput("Mastercard Plus")
        field("Label on card").performScrollTo().performTextInput("MASTERCARD")
        field("Starting digits").performScrollTo().performTextInput("4111")
        field("Number lengths").performScrollTo().performTextReplacement("16")
        Espresso.closeSoftKeyboard()
        compose.waitForIdle()
        compose.onNodeWithText("Add").performClick()

        waitForText("Card network added")
        val networks = runBlocking { app.repository.networks.first() }
        assertTrue(networks.any { it.code == "MASTERCARD" })

        compose.onNodeWithContentDescription("Back").performClick()
        seedCard("4111111111111111")
        compose.onNodeWithText("Cards").performClick()

        waitForText("MASTERCARD", unmerged = true)
    }
}
