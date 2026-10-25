package com.quickcards.app.flow

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quickcards.app.core.data.backup.BackupCodec
import com.quickcards.app.core.data.backup.BackupImporter
import com.quickcards.app.core.data.backup.BackupSnapshot
import com.quickcards.app.core.data.model.SeedData
import com.quickcards.app.core.security.ExportCrypto
import com.quickcards.app.core.security.UnlockSession
import com.quickcards.domain.MergeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Flow: export (captured through the same snapshot/codec the dialog
 * uses), erase through the real Settings dialog, then import the encrypted
 * backup and see the card again on Home. The system file picker is the one part
 * that cannot be driven from an instrumentation test, so the file itself is
 * produced and decoded with BackupSnapshot + BackupCodec + ExportCrypto;
 * BackupFlowTest covers the same round trip at repository level.
 */
@RunWith(AndroidJUnit4::class)
class BackupEraseImportFlowTest : FlowTestBase() {

    @After
    fun clearTestAuthenticator() {
        UnlockSession.testAuthenticator = null
    }

    private val visa = "4532778103492211"
    private val passphrase = "correct horse".toCharArray()

    @Test
    fun eraseAsksAgainInsideTheCopyWindowAndStopsWhenRefused() {
        UnlockSession.unlockFor(60)
        var asked = false
        UnlockSession.testAuthenticator = {
            asked = true
            false
        }

        compose.onNode(hasText("Settings") and isSelectable()).performClick()
        compose.waitForIdle()
        compose.waitUntil(10_000) {
            compose.onAllNodes(hasScrollAction()).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNode(hasScrollAction())
            .performScrollToNode(hasText("Erase all data"))
        waitForText("Erase all data")
        compose.onNodeWithText("Erase all data").performClick()

        compose.waitUntil(10_000) { asked }
        compose.waitForIdle()
        assertTrue(compose.onAllNodesWithText("Erase all data?").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun eraseDialogEmptiesTheVaultAndImportRestoresIt() {
        val holder = runBlocking { app.repository.createHolder("Aarav Mehta") }
        val bank = runBlocking { app.repository.createBank("HDFC Bank") }
        seedCard(visa, holderId = holder.id, bankId = bank.id)
        waitForContentDescription("More for HDFC Bank")

        val content = runBlocking {
            BackupSnapshot.capture(
                repository = app.repository,
                settings = app.settings,
                includeSettings = true,
                version = "2.0",
                exportedAt = "2026-10-24"
            )
        }
        val encrypted = ExportCrypto.encrypt(BackupCodec.encode(content), passphrase)

        compose.onNode(hasText("Settings") and isSelectable()).performClick()
        compose.waitForIdle()
        compose.waitUntil(10_000) {
            compose.onAllNodes(hasScrollAction()).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNode(hasScrollAction())
            .performScrollToNode(hasText("Erase all data"))
        waitForText("Erase all data")
        // Erase and import always re-ask, even inside the copy window; the test
        // stands in for the user at the system prompt.
        UnlockSession.testAuthenticator = { true }
        compose.onNodeWithText("Erase all data").performClick()

        waitForText("Erase all data?")
        compose.onNodeWithText(
            "Every card, holder, bank, type and tag will be erased from this phone. " +
                "Card networks go back to the defaults. Export a backup first if you might need them."
        ).assertExists()
        compose.onNodeWithText("Erase").assertIsNotEnabled()

        compose.onNode(hasText("I understand this can’t be undone") and isToggleable())
            .performClick()
        compose.onNodeWithText("Erase").performClick()

        compose.waitUntil(10_000) {
            cardsNow().isEmpty() && runBlocking { app.repository.holders.first() }.isEmpty()
        }
        assertTrue(runBlocking { app.repository.banks.first() }.isEmpty())
        assertEquals(
            SeedData.networks.size,
            runBlocking { app.database.networkDao().getAllNetworksSync().size }
        )

        val decoded = BackupCodec.decode(encrypted, passphrase)
        val result = runBlocking {
            BackupImporter.import(
                database = app.database,
                content = decoded.content,
                mode = MergeMode.UPDATE,
                settings = app.settings,
                restoreSettings = true
            )
        }
        assertEquals(1, result.added)

        compose.onNode(hasText("Cards") and isSelectable()).performClick()
        waitForContentDescription("More for HDFC Bank")
        val restored = cardsNow().single()
        assertEquals(visa, restored.number)
        assertEquals("12/29", restored.expiry)
        assertEquals("381", restored.cvv)
        assertEquals(
            "Aarav Mehta",
            runBlocking { app.repository.holders.first() }.single().name
        )
        assertEquals(
            "HDFC Bank",
            runBlocking { app.repository.banks.first() }.single().name
        )
    }
}
