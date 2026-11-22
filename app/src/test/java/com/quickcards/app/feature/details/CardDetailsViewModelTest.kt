package com.quickcards.app.feature.details

import com.quickcards.app.core.data.model.VaultCard
import com.quickcards.app.core.ui.CardCopyField
import com.quickcards.app.core.ui.CardFieldCopier
import com.quickcards.app.core.ui.HomeCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CardDetailsViewModelTest {

    private val copies = mutableListOf<Pair<CardCopyField, String>>()
    private val asked = mutableListOf<UnlockReason>()
    private val locked = MutableStateFlow(false)
    private lateinit var viewModel: CardDetailsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = CardDetailsViewModel(locked)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun card(id: String = "c1", cvv: String = "482", note: String = "Use for fuel") = HomeCard(
        card = VaultCard(id = id, number = "4532778103492211", expiry = "12/29", cvv = cvv, note = note),
        bankName = "HDFC Bank",
        holderName = "Aarav Mehta",
        typeName = "Credit",
        network = null,
        networkId = null,
        tagNames = emptyList()
    )

    private val copier = CardFieldCopier { card, field ->
        copies += field to CardCopyField.valueOf(card, field).orEmpty()
        "copied"
    }

    private fun unlock(granted: Boolean) = Unlock { reason -> asked += reason; granted }

    @Test
    fun copyAfterUnlockPutsTheValueOnTheClipboardAndMarksTheRow() = runBlocking {
        assertEquals("copied", viewModel.copy(card(), CardCopyField.CVV, unlock(true), copier))
        assertEquals(listOf(CardCopyField.CVV to "482"), copies)
        assertEquals(FieldMark("c1", CardCopyField.CVV), viewModel.copied.value)
        assertEquals(listOf(UnlockReason.COPY), asked)
    }

    @Test
    fun cancelledUnlockCopiesNothing() = runBlocking {
        assertNull(viewModel.copy(card(), CardCopyField.NUMBER, unlock(false), copier))
        assertTrue(copies.isEmpty())
        assertNull(viewModel.copied.value)
    }

    @Test
    fun noteCopyAsksWithItsOwnReason() = runBlocking {
        viewModel.copy(card(), CardCopyField.NOTE, unlock(true), copier)
        assertEquals(listOf(UnlockReason.COPY_NOTE), asked)
        assertEquals(CardCopyField.NOTE to "Use for fuel", copies.single())
    }

    @Test
    fun missingValueNeverPromptsOrCopies() = runBlocking {
        assertNull(viewModel.copy(card(cvv = ""), CardCopyField.CVV, unlock(true), copier))
        assertNull(viewModel.copy(card(note = ""), CardCopyField.NOTE, unlock(true), copier))
        assertTrue(asked.isEmpty())
        assertTrue(copies.isEmpty())
    }

    @Test
    fun revealNeedsUnlockAndOnlyAppliesToNumberAndCvv() = runBlocking {
        assertTrue(viewModel.reveal(card(), CardCopyField.NUMBER, unlock(true)))
        assertEquals(FieldMark("c1", CardCopyField.NUMBER), viewModel.revealed.value)
        assertEquals(listOf(UnlockReason.SHOW_NUMBER), asked)

        assertFalse(viewModel.reveal(card(), CardCopyField.EXPIRY, unlock(true)))
        assertEquals(FieldMark("c1", CardCopyField.NUMBER), viewModel.revealed.value)

        viewModel.hide()
        assertNull(viewModel.revealed.value)
    }

    @Test
    fun cancelledRevealStaysMasked() = runBlocking {
        assertFalse(viewModel.reveal(card(), CardCopyField.CVV, unlock(false)))
        assertNull(viewModel.revealed.value)
        assertEquals(listOf(UnlockReason.SHOW_CVV), asked)
    }

    @Test
    fun clearCopiedOnlyClearsTheSameRow() = runBlocking {
        viewModel.copy(card(), CardCopyField.EXPIRY, unlock(true), copier)
        viewModel.clearCopied(FieldMark("c1", CardCopyField.NUMBER))
        assertEquals(FieldMark("c1", CardCopyField.EXPIRY), viewModel.copied.value)
        viewModel.clearCopied(FieldMark("c1", CardCopyField.EXPIRY))
        assertNull(viewModel.copied.value)
    }

    @Test
    fun aRevealIsClearedWhenTheCardChanges() = runBlocking {
        viewModel.show("c1")
        viewModel.reveal(card(), CardCopyField.NUMBER, unlock(true))
        viewModel.copy(card(), CardCopyField.CVV, unlock(true), copier)

        viewModel.show("c2")
        assertNull(viewModel.revealed.value)
        assertNull(viewModel.copied.value)

        viewModel.show("c1")
        assertNull("Coming back to a card must ask again", viewModel.revealed.value)
    }

    @Test
    fun showingTheSameCardKeepsItsReveal() = runBlocking {
        viewModel.show("c1")
        viewModel.reveal(card(), CardCopyField.CVV, unlock(true))
        viewModel.show("c1")
        assertEquals(FieldMark("c1", CardCopyField.CVV), viewModel.revealed.value)
    }

    @Test
    fun aRevealOnAnotherCardReplacesTheShownCard() = runBlocking {
        viewModel.show("c1")
        viewModel.reveal(card(id = "c2"), CardCopyField.NUMBER, unlock(true))
        assertEquals(FieldMark("c2", CardCopyField.NUMBER), viewModel.revealed.value)
    }

    @Test
    fun aRevealIsClearedWhenTheAppLocks() = runBlocking {
        viewModel.reveal(card(), CardCopyField.NUMBER, unlock(true))
        viewModel.copy(card(), CardCopyField.EXPIRY, unlock(true), copier)

        locked.value = true
        assertNull(viewModel.revealed.value)
        assertNull(viewModel.copied.value)

        locked.value = false
        assertNull(viewModel.revealed.value)
    }

    @Test
    fun aRevealWhileLockedIsRefused() = runBlocking {
        locked.value = true
        assertFalse(viewModel.reveal(card(), CardCopyField.NUMBER, unlock(true)))
        assertNull(viewModel.revealed.value)
    }

    @Test
    fun leavingThePaneClearsEverything() = runBlocking {
        viewModel.reveal(card(), CardCopyField.CVV, unlock(true))
        viewModel.copy(card(), CardCopyField.NUMBER, unlock(true), copier)
        viewModel.clear()
        assertNull(viewModel.revealed.value)
        assertNull(viewModel.copied.value)
    }
}
