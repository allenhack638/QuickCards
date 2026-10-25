package com.quickcards.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class CardListOrderTest {

    private data class TestCard(
        val id: String,
        val pinned: Boolean = false,
        val state: ExpiryState? = ExpiryState.VALID,
        val archived: Boolean = false
    )

    private fun arrange(
        cards: List<TestCard>,
        placement: ExpiredPlacement = ExpiredPlacement.END,
        searching: Boolean = false,
        statusFilter: Set<ExpiryState> = emptySet()
    ): List<String> = CardListOrder.arrangeCards(
        cards = cards,
        isPinned = { it.pinned },
        state = { it.state },
        isArchived = { it.archived },
        placement = placement,
        searching = searching,
        statusFilter = statusFilter
    ).map { it.id }

    private val pinnedExpired = TestCard("pinned-expired", pinned = true, state = ExpiryState.EXPIRED)
    private val pinnedValid = TestCard("pinned-valid", pinned = true)
    private val expiredA = TestCard("expired-a", state = ExpiryState.EXPIRED)
    private val expiredB = TestCard("expired-b", state = ExpiryState.EXPIRED)
    private val soon = TestCard("soon", state = ExpiryState.EXPIRES_SOON)
    private val validA = TestCard("valid-a")
    private val validB = TestCard("valid-b")
    private val archivedCard = TestCard("archived", archived = true)

    @Test
    fun `keep leaves the input order unchanged`() {
        val cards = listOf(expiredA, validA, soon, expiredB)

        assertEquals(
            listOf("expired-a", "valid-a", "soon", "expired-b"),
            arrange(cards, placement = ExpiredPlacement.KEEP)
        )
    }

    @Test
    fun `end moves expired unpinned cards after every other card`() {
        val cards = listOf(validA, expiredA, soon, validB, expiredB)

        assertEquals(
            listOf("valid-a", "soon", "valid-b", "expired-a", "expired-b"),
            arrange(cards)
        )
    }

    @Test
    fun `a pinned expired card stays in the pinned block at the top`() {
        val cards = listOf(pinnedExpired, pinnedValid, validA, expiredA)

        assertEquals(
            listOf("pinned-expired", "pinned-valid", "valid-a", "expired-a"),
            arrange(cards)
        )
        assertEquals(
            listOf("pinned-expired", "pinned-valid", "valid-a", "expired-a"),
            arrange(cards, placement = ExpiredPlacement.KEEP)
        )
    }

    @Test
    fun `hide removes expired cards unless searching`() {
        val cards = listOf(validA, expiredA, soon)

        assertEquals(
            listOf("valid-a", "soon"),
            arrange(cards, placement = ExpiredPlacement.HIDE)
        )
        assertEquals(
            listOf("valid-a", "expired-a", "soon"),
            arrange(cards, placement = ExpiredPlacement.HIDE, searching = true)
        )
    }

    @Test
    fun `hide keeps expired cards when the Expired filter is on`() {
        val cards = listOf(validA, expiredA, soon)

        assertEquals(
            listOf("expired-a"),
            arrange(
                cards,
                placement = ExpiredPlacement.HIDE,
                statusFilter = setOf(ExpiryState.EXPIRED)
            )
        )
    }

    @Test
    fun `a status filter keeps only matching cards and never reorders`() {
        val cards = listOf(soon, validA, expiredA, validB)

        assertEquals(
            listOf("soon"),
            arrange(cards, statusFilter = setOf(ExpiryState.EXPIRES_SOON))
        )
        assertEquals(
            listOf("expired-a"),
            arrange(cards, statusFilter = setOf(ExpiryState.EXPIRED))
        )
        assertEquals(
            listOf("soon", "valid-a", "valid-b"),
            arrange(
                cards,
                placement = ExpiredPlacement.KEEP,
                statusFilter = setOf(ExpiryState.VALID, ExpiryState.EXPIRES_SOON)
            )
        )
    }

    @Test
    fun `expires soon never changes the order`() {
        val cards = listOf(soon, validA, soon, validB)

        assertEquals(
            listOf("soon", "valid-a", "soon", "valid-b"),
            arrange(cards)
        )
    }

    @Test
    fun `a card without an expiry state is kept in place but excluded by a status filter`() {
        val unknown = TestCard("unknown", state = null)
        val cards = listOf(validA, unknown, validB)

        assertEquals(listOf("valid-a", "unknown", "valid-b"), arrange(cards))
        assertEquals(
            listOf("valid-a", "valid-b"),
            arrange(cards, statusFilter = setOf(ExpiryState.VALID))
        )
    }

    @Test
    fun `archived cards appear only in search`() {
        val cards = listOf(validA, archivedCard)

        assertEquals(listOf("valid-a"), arrange(cards))
        assertEquals(
            listOf("valid-a", "archived"),
            arrange(cards, searching = true)
        )
    }

    @Test
    fun `an archived card matches the expired filter while searching`() {
        val archivedExpired = TestCard("archived-expired", state = ExpiryState.EXPIRED, archived = true)
        val cards = listOf(validA, archivedExpired)

        assertEquals(
            listOf("archived-expired"),
            arrange(
                cards,
                searching = true,
                statusFilter = setOf(ExpiryState.EXPIRED)
            )
        )
    }
}
