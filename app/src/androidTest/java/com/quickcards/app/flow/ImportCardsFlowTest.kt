package com.quickcards.app.flow

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.quickcards.app.core.security.UnlockSession
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Settings > Import cards > Cards from a spreadsheet > Proceed > the system picker (stubbed to
 * return the fixture) > check screen > Import > cards in the vault. The picker is the only
 * part a test cannot drive for real.
 */
@RunWith(AndroidJUnit4::class)
class ImportCardsFlowTest : FlowTestBase() {

    @After
    fun tearDownIntents() {
        runCatching { Intents.release() }
        UnlockSession.testAuthenticator = null
    }

    private fun stubPickerWith(assetName: String) {
        val bytes = InstrumentationRegistry.getInstrumentation().context.assets.open("sheet/$assetName").use { it.readBytes() }
        val file = File(app.cacheDir, assetName).apply { writeBytes(bytes) }
        Intents.init()
        Intents.intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWith(
            Instrumentation.ActivityResult(Activity.RESULT_OK, Intent().setData(Uri.fromFile(file)))
        )
    }

    private fun openImportCards() {
        compose.onNode(hasText("Settings") and isSelectable()).performClick()
        compose.waitForIdle()
        compose.waitUntil(10_000) {
            compose.onAllNodes(hasScrollAction()).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Import cards"))
        waitForText("Import cards")
        UnlockSession.testAuthenticator = { true }
        compose.onNodeWithText("Import cards").performClick()
        waitForText("Cards from a spreadsheet")
    }

    @Test
    fun aFilledSheetIsCheckedThenImported() {
        stubPickerWith("generated-rows.xlsx")
        openImportCards()
        compose.onNodeWithText("Cards from a spreadsheet").performClick()
        waitForText("Download the Excel sheet")
        compose.onNodeWithText("Proceed").performClick()

        waitForText("Check your cards")
        waitForText("6 rows checked")
        compose.onNodeWithText("Ready to import").assertExists()
        compose.onNodeWithText("Need fixing").assertExists()
        compose.onNodeWithText("Priya S, Arjun").assertExists()
        compose.onNodeWithText("Row 6").assertExists()
        compose.onNodeWithText("This card number does not look right. Check the digits.").assertExists()
        compose.onNodeWithText("Row 7").assertExists()
        compose.onNodeWithText("This number looks like Visa, but Network says RuPay.").assertExists()
        compose.onNodeWithText("Save rows to fix").assertExists()

        compose.onNodeWithText("Import 3 cards").performClick()
        waitForText("3 cards added.")
        compose.onNodeWithText("Done").performClick()

        compose.waitUntil(10_000) { cardsNow().size == 3 }
        val numbers = cardsNow().map { it.number }.toSet()
        assertEquals(setOf("4111111111111111", "378282246310005", "5500000000000004"), numbers)
        val holders = runBlocking { app.repository.holders.first() }.map { it.name }
        assertTrue(holders.toString(), holders.containsAll(listOf("Priya S", "Arjun")))
    }

    @Test
    fun importingTheSameSheetAgainAddsNothing() {
        stubPickerWith("filled-semicolon.csv")
        openImportCards()
        compose.onNodeWithText("Cards from a spreadsheet").performClick()
        waitForText("Proceed")
        compose.onNodeWithText("Proceed").performClick()
        waitForText("3 rows checked")
        compose.onNodeWithText("Import 3 cards").performClick()
        waitForText("3 cards added.")
        compose.onNodeWithText("Done").performClick()
        compose.waitUntil(10_000) { cardsNow().size == 3 }

        openImportCards()
        compose.onNodeWithText("Cards from a spreadsheet").performClick()
        waitForText("Proceed")
        compose.onNodeWithText("Proceed").performClick()
        waitForText("Every card in this sheet is already in QuickCards. Nothing to import.")
        compose.onNodeWithText("Import 0 cards").assertIsNotEnabled()
        compose.onNodeWithText("Cancel").performClick()
        assertEquals(3, cardsNow().size)
    }
}
