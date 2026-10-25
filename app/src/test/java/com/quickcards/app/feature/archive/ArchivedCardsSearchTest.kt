package com.quickcards.app.feature.archive

import com.quickcards.app.core.data.model.VaultCard
import com.quickcards.app.core.ui.HomeCard
import com.quickcards.domain.CardNetwork
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchivedCardsSearchTest {

    private fun network(
        name: String = "Visa",
        code: String = "VISA"
    ) = CardNetwork(
        id = "visa",
        name = name,
        code = code,
        prefixes = "4",
        lengths = "16",
        cvvLength = 3,
        luhn = true
    )

    private fun item(
        id: String = "card-1",
        number: String = "4111111111112211",
        bankName: String = "HDFC Bank",
        holderName: String = "Aarav Mehta",
        typeName: String? = "Credit",
        networkName: String = "Visa",
        networkCode: String = "VISA",
        tags: List<String> = listOf("Travel"),
        note: String = "Renewal in June",
        reason: String? = "Card closed"
    ): ArchivedCardItem = ArchivedCardItem(
        card = HomeCard(
            card = VaultCard(id = id, number = number, note = note),
            bankName = bankName,
            holderName = holderName,
            typeName = typeName,
            network = network(networkName, networkCode),
            networkId = "visa",
            tagNames = tags
        ),
        reason = reason
    )

    private fun matches(query: String, cards: List<ArchivedCardItem> = listOf(item())): List<String> =
        ArchivedCardsSearch.filter(cards, query).map { it.id }

    @Test
    fun findsTheLastFourDigitsOnly() {
        assertEquals(listOf("card-1"), matches("2211"))
        assertEquals(listOf("card-1"), matches("22 11"))
        assertTrue(matches("4111 1111").isEmpty())
    }

    @Test
    fun findsTheBank() {
        assertEquals(listOf("card-1"), matches("hdfc"))
        assertEquals(listOf("card-1"), matches("HDFC BANK"))
    }

    @Test
    fun findsTheHolder() {
        assertEquals(listOf("card-1"), matches("aarav"))
    }

    @Test
    fun findsTheNetworkNameAndCode() {
        assertEquals(listOf("card-1"), matches("visa"))
        assertEquals(
            listOf("card-1"),
            matches("visa", listOf(item(networkName = "Some network", networkCode = "VISA")))
        )
    }

    @Test
    fun findsTheCardType() {
        assertEquals(listOf("card-1"), matches("credit"))
    }

    @Test
    fun findsTheTags() {
        assertEquals(
            listOf("card-1"),
            matches("travel", listOf(item(tags = listOf("Travel", "Work"))))
        )
    }

    @Test
    fun findsTheNote() {
        assertEquals(listOf("card-1"), matches("renewal"))
    }

    @Test
    fun findsTheArchiveReason() {
        assertEquals(
            listOf("card-1"),
            matches("closed", listOf(item(reason = "Card closed")))
        )
        assertEquals(
            listOf("card-2"),
            matches(
                "replaced",
                listOf(item(id = "card-1", reason = "Card closed"), item(id = "card-2", reason = "Replaced"))
            )
        )
    }

    @Test
    fun isCaseInsensitiveAndTrimsTheQuery() {
        assertEquals(listOf("card-1"), matches("  AARAV  "))
    }

    @Test
    fun blankQueryKeepsEveryArchivedCard() {
        val cards = listOf(item(id = "card-1"), item(id = "card-2", reason = null))
        assertEquals(listOf("card-1", "card-2"), matches("   ", cards))
    }

    @Test
    fun unmatchedQueryHasNoResults() {
        assertTrue(matches("does not exist").isEmpty())
    }

    @Test
    fun missingReasonDoesNotMatchReasonText() {
        assertEquals(emptyList<String>(), matches("closed", listOf(item(reason = null))))
    }

    @Test
    fun emptyVaultStateHasNoCardsAndNoResults() {
        val state = buildArchivedCardsState(emptyList(), "")

        assertFalse(state.loading)
        assertTrue(state.cards.isEmpty())
        assertTrue(state.visibleCards.isEmpty())
    }

    @Test
    fun stateKeepsCardsWhileNoResultsAreShown() {
        val state = buildArchivedCardsState(listOf(item()), "nothing", searchOn = true)

        assertFalse(state.loading)
        assertEquals(1, state.cards.size)
        assertTrue(state.visibleCards.isEmpty())
        assertEquals("nothing", state.query)
        assertTrue(state.searchOn)
    }

    @Test
    fun stateFiltersVisibleCardsByTheQuery() {
        val cards = listOf(
            item(id = "card-1", reason = "Card closed"),
            item(id = "card-2", reason = "Replaced")
        )

        val state = buildArchivedCardsState(cards, "replaced")

        assertEquals(2, state.cards.size)
        assertEquals(listOf("card-2"), state.visibleCards.map { it.id })
    }
}
