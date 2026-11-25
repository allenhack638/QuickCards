package com.quickcards.app.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
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
import com.quickcards.domain.CardNumberRules
import com.quickcards.domain.CardStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
        var paid = 0
    }

    private fun show(
        shown: HomeCard = card,
        status: CardStatus? = null,
        copied: Boolean = false,
        copyEnabled: Boolean = true,
        maskNumber: Boolean = true,
        fontScale: Float = 1f,
        dueText: String? = null,
        taps: Taps = Taps()
    ): Taps {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
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
                        onCopyNote = { taps.note++ },
                        dueText = dueText,
                        onMarkPaid = { taps.paid++ }
                    )
                    }
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

    @Test
    fun fullNumberSitsBesideCopyAtNormalSize() {
        show(maskNumber = false)
        val number = numberBounds()
        val copy = copyBounds()
        assertTrue(copy.left >= number.right)
        assertTrue(copy.top < number.bottom)
    }

    @Test
    fun sixteenDigitsAtDoubleTextStayWholeBesideCopy() {
        show(maskNumber = false, fontScale = 2f)
        val layout = numberLayout()
        assertFalse(layout.hasVisualOverflow)
        assertEquals(FULL.length, layout.getLineEnd(layout.lineCount - 1, visibleEnd = true))
        assertTrue(copyBounds().left >= numberBounds().right)
    }

    @Test
    fun nineteenDigitsAtDoubleTextAreWholeWithCopyBelowOnTheRight() {
        val long = card.copy(card = card.card.copy(number = LONG), network = null)
        show(shown = long, maskNumber = false, fontScale = 2f)
        val shown = CardNumberRules.format(LONG, null, masked = false)
        val layout = numberLayout(shown)
        assertFalse(layout.hasVisualOverflow)
        assertEquals(shown.length, layout.getLineEnd(layout.lineCount - 1, visibleEnd = true))
        val number = numberBounds(shown)
        val copy = copyBounds()
        assertTrue(copy.top >= number.bottom)
        assertTrue(copy.right >= number.right)
    }

    @Test
    fun dueStripNestsInTheCardAndMarkPaidIsItsOwnTarget() {
        val taps = show(dueText = "Due in 2 days")
        val mark = compose.onNodeWithContentDescription("Mark this payment as paid")
        mark.performClick()
        compose.waitForIdle()
        assertEquals(1, taps.paid)
        assertEquals(0, taps.open)

        val px = compose.density.density
        val card = compose.onNodeWithContentDescription("HDFC Bank, ending 0366", substring = true).fetchSemanticsNode().boundsInRoot
        val hit = mark.fetchSemanticsNode().boundsInRoot
        assertEquals(card.bottom - DUE_EDGE_GAP * px, hit.bottom, 1f)
        assertEquals(card.right - (DUE_EDGE_GAP + DUE_ACTION_INSET) * px, hit.right, 1f)
    }

    private fun numberNode(text: String) = compose.onNodeWithText(text, useUnmergedTree = true)

    private fun numberBounds(text: String = FULL) = numberNode(text).fetchSemanticsNode().boundsInRoot

    private fun numberLayout(text: String = FULL): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        numberNode(text).fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
        return results.single()
    }

    private fun copyBounds() =
        compose.onNodeWithContentDescription("Copy HDFC Bank card number ending", substring = true).fetchSemanticsNode().boundsInRoot

    private companion object {
        const val HEAD_FRACTION = 0.12f
        const val DUE_EDGE_GAP = 8f
        const val DUE_ACTION_INSET = 4f
        const val FULL = "4532 7180 2299 0366"
        const val LONG = "6011000990139424123"
        const val NOTE = "Travel card. 2 free lounge visits a quarter, 1% forex markup. Call the bank before a trip abroad."
    }
}
