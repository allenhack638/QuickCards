package com.quickcards.app.feature.cards

import com.quickcards.app.core.data.model.VaultCard
import com.quickcards.app.core.ui.HomeCard
import com.quickcards.domain.ArchivedVisibility
import com.quickcards.domain.CardField
import com.quickcards.domain.CardFilters
import com.quickcards.domain.ExpiredPlacement
import com.quickcards.domain.ExpiryState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CardsListStateTest {

    private fun card(
        id: String,
        expiry: String = "12/29",
        state: ExpiryState? = ExpiryState.VALID,
        archived: Boolean = false,
        pinned: Boolean = false,
        needsReview: Boolean = false,
        bank: String = "HDFC Bank"
    ): HomeCard = HomeCard(
        card = VaultCard(
            id = id,
            number = "4532778103492211",
            expiry = expiry,
            cvv = "381",
            bankId = "bank-1",
            pinned = pinned,
            archived = archived,
            archiveReason = if (archived) "Card closed" else null,
            needsReview = if (needsReview) setOf(CardField.EXPIRY) else emptySet()
        ),
        bankName = bank,
        holderName = "Aarav Mehta",
        typeName = "Credit",
        network = null,
        networkId = null,
        tagNames = emptyList(),
        expiryState = state
    )

    private fun options(
        placement: ExpiredPlacement = ExpiredPlacement.END,
        archived: ArchivedVisibility = ArchivedVisibility.ONLY_ARCHIVE,
        searching: Boolean = false,
        status: Set<ExpiryState> = emptySet(),
        reviewOnly: Boolean = false
    ) = CardsListOptions(placement, archived, searching, status, reviewOnly)

    private fun ids(cards: List<HomeCard>) = cards.map { it.id }

    @Test
    fun mainListExcludesArchivedCards() {
        val cards = listOf(
            card("active"),
            card("archived", archived = true)
        )

        val visible = CardsListState.visibleCards(
            cards, CardFilters(), "", true, options()
        )

        assertEquals(listOf("active"), ids(visible))
    }

    @Test
    fun archivedCardsAppearOnlyWhenSearchingWithTheSearchSetting() {
        val cards = listOf(
            card("active"),
            card("archived", archived = true, bank = "Closed Bank")
        )

        val onlyArchive = CardsListState.visibleCards(
            cards, CardFilters(), "closed", true, options(searching = true)
        )
        assertEquals(emptyList<String>(), ids(onlyArchive))

        val inSearch = CardsListState.visibleCards(
            cards,
            CardFilters(),
            "closed",
            true,
            options(
                archived = ArchivedVisibility.IN_SEARCH,
                searching = true
            )
        )
        assertEquals(listOf("archived"), ids(inSearch))
    }

    @Test
    fun endMovesExpiredAfterEveryOtherUnpinnedCard() {
        val cards = listOf(
            card("expired", state = ExpiryState.EXPIRED),
            card("valid"),
            card("soon", state = ExpiryState.EXPIRES_SOON)
        )

        val visible = CardsListState.visibleCards(
            cards, CardFilters(), "", true, options(placement = ExpiredPlacement.END)
        )

        assertEquals(listOf("valid", "soon", "expired"), ids(visible))
    }

    @Test
    fun pinnedExpiredCardStaysInThePinnedBlock() {
        val cards = listOf(
            card("pinnedExpired", state = ExpiryState.EXPIRED, pinned = true),
            card("expired", state = ExpiryState.EXPIRED),
            card("valid")
        )

        val visible = CardsListState.visibleCards(
            cards, CardFilters(), "", true, options(placement = ExpiredPlacement.END)
        )

        assertEquals(listOf("pinnedExpired", "valid", "expired"), ids(visible))
    }

    @Test
    fun keepLeavesTheOrderAlone() {
        val cards = listOf(
            card("one"),
            card("expired", state = ExpiryState.EXPIRED),
            card("two")
        )

        val visible = CardsListState.visibleCards(
            cards, CardFilters(), "", true, options(placement = ExpiredPlacement.KEEP)
        )

        assertEquals(listOf("one", "expired", "two"), ids(visible))
    }

    @Test
    fun hideRemovesExpiredUnlessSearchingOrFiltered() {
        val cards = listOf(
            card("valid"),
            card("expired", state = ExpiryState.EXPIRED, bank = "Federal Bank")
        )

        val hidden = CardsListState.visibleCards(
            cards, CardFilters(), "", true,
            options(placement = ExpiredPlacement.HIDE)
        )
        assertEquals(listOf("valid"), ids(hidden))

        val searched = CardsListState.visibleCards(
            cards, CardFilters(), "federal", true,
            options(placement = ExpiredPlacement.HIDE, searching = true)
        )
        assertEquals(listOf("expired"), ids(searched))

        val filtered = CardsListState.visibleCards(
            cards, CardFilters(), "", true,
            options(
                placement = ExpiredPlacement.HIDE,
                status = setOf(ExpiryState.EXPIRED)
            )
        )
        assertEquals(listOf("expired"), ids(filtered))
    }

    @Test
    fun statusFilterCombinesWithOrAndIgnoresExpiresSoonOrder() {
        val cards = listOf(
            card("expired", state = ExpiryState.EXPIRED),
            card("soon", state = ExpiryState.EXPIRES_SOON),
            card("valid")
        )

        val visible = CardsListState.visibleCards(
            cards, CardFilters(), "", true,
            options(
                status = setOf(ExpiryState.EXPIRED, ExpiryState.EXPIRES_SOON)
            )
        )

        assertEquals(listOf("soon", "expired"), ids(visible))
    }

    @Test
    fun reviewFilterKeepsOnlyCardsThatNeedACheck() {
        val cards = listOf(
            card("review", needsReview = true),
            card("clean"),
            card("archivedReview", archived = true, needsReview = true)
        )

        val visible = CardsListState.visibleCards(
            cards, CardFilters(), "", true, options(reviewOnly = true)
        )

        assertEquals(listOf("review"), ids(visible))
    }

    @Test
    fun statusCountsIgnoreArchivedCardsAndHideEmptyGroups() {
        val cards = listOf(
            card("expired", state = ExpiryState.EXPIRED),
            card("soon", state = ExpiryState.EXPIRES_SOON),
            card("archived", state = ExpiryState.EXPIRED, archived = true),
            card("valid")
        )

        val options = CardsListState.statusOptions(cards, "Expired", "Expires soon")

        assertEquals(2, options.size)
        assertEquals(CardsStatusFilter.EXPIRED, options[0].id)
        assertEquals("Expired", options[0].label)
        assertEquals(1, options[0].count)
        assertEquals(CardsStatusFilter.SOON, options[1].id)
        assertEquals(1, options[1].count)

        assertEquals(
            emptyList<Any>(),
            CardsListState.statusOptions(
                listOf(card("valid"), card("archived", state = ExpiryState.EXPIRED, archived = true)),
                "Expired",
                "Expires soon"
            )
        )
    }

    @Test
    fun needsCheckCountOnlyCountsMainListCards() {
        val cards = listOf(
            card("review", needsReview = true),
            card("archived", archived = true, needsReview = true),
            card("clean")
        )

        assertEquals(1, CardsListState.needsCheckCount(cards))
    }

    @Test
    fun emptyNoteExplainsHiddenExpiredCards() {
        val cards = listOf(
            card("expired", state = ExpiryState.EXPIRED),
            card("archived", state = ExpiryState.EXPIRED, archived = true)
        )

        assertEquals(
            CardsEmptyNote.HIDDEN_EXPIRED,
            CardsListState.emptyNote(cards, ExpiredPlacement.HIDE, false, false)
        )
    }

    @Test
    fun emptyNoteExplainsAnArchiveOnlyVault() {
        val cards = listOf(
            card("archived", archived = true),
            card("archived2", archived = true)
        )

        assertEquals(
            CardsEmptyNote.ONLY_ARCHIVED,
            CardsListState.emptyNote(cards, ExpiredPlacement.END, false, false)
        )
    }

    @Test
    fun emptyNoteStaysQuietWhenCardsAreVisibleOrNarrowed() {
        val cards = listOf(card("valid"), card("archived", archived = true))

        assertEquals(
            CardsEmptyNote.NONE,
            CardsListState.emptyNote(cards, ExpiredPlacement.HIDE, false, false)
        )
        assertEquals(
            CardsEmptyNote.NONE,
            CardsListState.emptyNote(
                listOf(card("expired", state = ExpiryState.EXPIRED)),
                ExpiredPlacement.HIDE,
                false,
                true
            )
        )
        assertEquals(
            CardsEmptyNote.NONE,
            CardsListState.emptyNote(
                listOf(card("archived", archived = true)),
                ExpiredPlacement.END,
                true,
                false
            )
        )
    }

    @Test
    fun statusIdsRoundTripToExpiryStates() {
        assertEquals(
            setOf(ExpiryState.EXPIRED, ExpiryState.EXPIRES_SOON),
            CardsStatusFilter.states(
                setOf(CardsStatusFilter.EXPIRED, CardsStatusFilter.SOON)
            )
        )
        assertTrue(CardsStatusFilter.matches(ExpiryState.EXPIRED, setOf("expired")))
        assertFalse(CardsStatusFilter.matches(ExpiryState.VALID, setOf("expired")))
        assertTrue(CardsStatusFilter.matches(null, emptySet()))
    }
}
