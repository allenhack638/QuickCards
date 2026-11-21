package com.quickcards.app.feature.details

import com.quickcards.app.core.data.model.VaultCard
import com.quickcards.app.core.ui.HomeCard
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CardDetailsViewModelTest {

    private val copies = mutableListOf<Pair<DetailsField, String>>()
    private val asked = mutableListOf<UnlockReason>()
    private val viewModel = CardDetailsViewModel { _, field, value -> copies += field to value }

    private fun card(cvv: String = "482", note: String = "Use for fuel") = HomeCard(
        card = VaultCard(id = "c1", number = "4532778103492211", expiry = "12/29", cvv = cvv, note = note),
        bankName = "HDFC Bank",
        holderName = "Aarav Mehta",
        typeName = "Credit",
        network = null,
        networkId = null,
        tagNames = emptyList()
    )

    private fun unlock(granted: Boolean) = Unlock { reason -> asked += reason; granted }

    @Test
    fun copyAfterUnlockPutsTheValueOnTheClipboardAndMarksTheRow() = runBlocking {
        assertTrue(viewModel.copy(card(), DetailsField.CVV, unlock(true)))
        assertEquals(listOf(DetailsField.CVV to "482"), copies)
        assertEquals(DetailsField.CVV, viewModel.copied.value)
        assertEquals(listOf(UnlockReason.COPY), asked)
    }

    @Test
    fun cancelledUnlockCopiesNothing() = runBlocking {
        assertFalse(viewModel.copy(card(), DetailsField.NUMBER, unlock(false)))
        assertTrue(copies.isEmpty())
        assertNull(viewModel.copied.value)
    }

    @Test
    fun noteCopyAsksWithItsOwnReason() = runBlocking {
        assertTrue(viewModel.copy(card(), DetailsField.NOTE, unlock(true)))
        assertEquals(listOf(UnlockReason.COPY_NOTE), asked)
        assertEquals(DetailsField.NOTE to "Use for fuel", copies.single())
    }

    @Test
    fun missingValueNeverPromptsOrCopies() = runBlocking {
        assertFalse(viewModel.copy(card(cvv = ""), DetailsField.CVV, unlock(true)))
        assertFalse(viewModel.copy(card(note = ""), DetailsField.NOTE, unlock(true)))
        assertTrue(asked.isEmpty())
        assertTrue(copies.isEmpty())
    }

    @Test
    fun revealNeedsUnlockAndOnlyAppliesToNumberAndCvv() = runBlocking {
        assertTrue(viewModel.reveal(card(), DetailsField.NUMBER, unlock(true)))
        assertEquals(DetailsField.NUMBER, viewModel.revealed.value)
        assertEquals(listOf(UnlockReason.SHOW_NUMBER), asked)

        assertFalse(viewModel.reveal(card(), DetailsField.EXPIRY, unlock(true)))
        assertEquals(DetailsField.NUMBER, viewModel.revealed.value)

        viewModel.hide()
        assertNull(viewModel.revealed.value)
    }

    @Test
    fun cancelledRevealStaysMasked() = runBlocking {
        assertFalse(viewModel.reveal(card(), DetailsField.CVV, unlock(false)))
        assertNull(viewModel.revealed.value)
        assertEquals(listOf(UnlockReason.SHOW_CVV), asked)
    }

    @Test
    fun clearCopiedOnlyClearsTheSameRow() = runBlocking {
        viewModel.copy(card(), DetailsField.EXPIRY, unlock(true))
        viewModel.clearCopied(DetailsField.NUMBER)
        assertEquals(DetailsField.EXPIRY, viewModel.copied.value)
        viewModel.clearCopied(DetailsField.EXPIRY)
        assertNull(viewModel.copied.value)
    }
}
