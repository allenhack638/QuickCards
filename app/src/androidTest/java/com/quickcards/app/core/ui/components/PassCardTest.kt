package com.quickcards.app.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quickcards.app.core.data.model.VaultCard
import com.quickcards.app.core.ui.HomeCard
import com.quickcards.app.core.ui.theme.QuickCardsTheme
import com.quickcards.domain.CardNetwork
import com.quickcards.domain.CardStatus
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PassCardTest {

    @get:Rule
    val compose = createComposeRule()

    private val card = HomeCard(
        card = VaultCard(id = "hdfc", number = "4532718022990366", expiry = "12/29", cvv = "482", note = NOTE),
        bankName = "HDFC Bank",
        holderName = "Aarav Mehta",
        typeName = "Credit",
        network = CardNetwork("visa", "Visa", "VISA", "4", "16", 3, true),
        networkId = "visa",
        tagNames = emptyList()
    )

    private class Taps {
        var open = 0
        var actions = 0
        var copy = 0
        var note = 0
    }

    private fun show(
        shown: HomeCard = card,
        status: CardStatus? = null,
        copied: Boolean = false,
        copyEnabled: Boolean = true,
        maskNumber: Boolean = true,
        taps: Taps = Taps()
    ): Taps {
        compose.setContent {
            QuickCardsTheme(darkTheme = false) {
                Box(modifier = Modifier.width(328.dp)) {
                    PassCard(
                        card = shown,
                        maskNumber = maskNumber,
                        status = status,
                        note = NOTE,
                        copied = copied,
                        copyEnabled = copyEnabled,
                        onOpen = { taps.open++ },
                        onActions = { taps.actions++ },
                        onCopy = { taps.copy++ },
                        onCopyNote = { taps.note++ }
                    )
                }
            }
        }
        compose.waitForIdle()
        return taps
    }

    @Test
    fun tapOpensAndHoldOpensActions() {
        val taps = show()
        val tile = compose.onNodeWithContentDescription("HDFC Bank, ending 0366. Hold for card actions")
        tile.performClick()
        // Hold the bank row: the tile's centre is the note, whose hold copies the note.
        tile.performTouchInput { longClick(Offset(centerX, height * HEAD_FRACTION)) }
        compose.waitForIdle()

        assertEquals(1, taps.open)
        assertEquals(1, taps.actions)
        assertEquals(0, taps.copy)
    }

    @Test
    fun copyPillIsItsOwnTargetAndCopiesTheNumber() {
        val taps = show()
        compose.onNodeWithContentDescription("Copy HDFC Bank card number ending 0366").performClick()
        compose.waitForIdle()

        assertEquals(1, taps.copy)
        assertEquals(0, taps.open)
    }

    @Test
    fun maskedTileShowsOnlyTheLastFour() {
        show()
        compose.onAllNodesWithText(FULL, useUnmergedTree = true).assertCountEquals(0)
        compose.onAllNodesWithText("0366", useUnmergedTree = true).assertCountEquals(1)
    }

    @Test
    fun hideCardNumbersOffShowsTheFullNumberButSpeaksOnlyTheLastFour() {
        val taps = show(maskNumber = false)
        compose.onAllNodesWithText(FULL, useUnmergedTree = true).assertCountEquals(1)
        compose.onNodeWithContentDescription("HDFC Bank, ending 0366. Hold for card actions").assertExists()
        compose.onNodeWithContentDescription("Copy HDFC Bank card number ending 0366").performClick()
        compose.waitForIdle()
        assertEquals(1, taps.copy)
    }

    @Test
    fun copiedPillSaysCopied() {
        show(copied = true)
        compose.onNodeWithContentDescription("HDFC Bank card number copied").assertExists()
    }

    @Test
    fun copyIsDisabledWithoutAScreenLock() {
        show(copyEnabled = false)
        compose.onNodeWithContentDescription("Copy HDFC Bank card number ending 0366").assertIsNotEnabled()
    }

    @Test
    fun statusChipJoinsTheTileName() {
        show(status = CardStatus.CHECK)
        compose.onNodeWithContentDescription("HDFC Bank, ending 0366, Check details. Hold for card actions").assertExists()
    }

    @Test
    fun pinnedJoinsTheTileName() {
        show(shown = card.copy(card = card.card.copy(pinned = true)))
        compose.onNodeWithContentDescription("HDFC Bank, ending 0366, Pinned. Hold for card actions").assertExists()
    }

    @Test
    fun holdingTheNoteCopiesIt() {
        val taps = show()
        compose.onNodeWithContentDescription(NOTE).performTouchInput { longClick() }
        compose.waitForIdle()

        assertEquals(1, taps.note)
        assertEquals(0, taps.actions)
    }

    private companion object {
        const val HEAD_FRACTION = 0.12f
        const val FULL = "4532 7180 2299 0366"
        const val NOTE = "Travel card. 2 free lounge visits a quarter, 1% forex markup. Call the bank before a trip abroad."
    }
}
