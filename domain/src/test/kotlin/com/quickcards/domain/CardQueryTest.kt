package com.quickcards.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CardQueryTest {

    private fun entry(
        id: String,
        number: String,
        bankId: String = "bank-$id",
        bankName: String = "HDFC Bank",
        holderId: String = "h1",
        holderName: String = "Aarav Mehta",
        typeId: String? = "t1",
        typeName: String = "Credit",
        networkId: String? = "visa",
        networkName: String = "Visa",
        networkCode: String = "VISA",
        note: String = "",
        tagIds: List<String> = emptyList(),
        tagNames: List<String> = emptyList()
    ) = CardQueryEntry(
        id = id,
        number = number,
        bankId = bankId,
        bankName = bankName,
        holderId = holderId,
        holderName = holderName,
        typeId = typeId,
        typeName = typeName,
        networkId = networkId,
        networkName = networkName,
        networkCode = networkCode,
        note = note,
        tagIds = tagIds,
        tagNames = tagNames
    )

    private val visaHdfc = entry(
        id = "1",
        number = "4532778103492211",
        bankName = "HDFC Bank",
        typeId = "credit",
        typeName = "Credit",
        tagIds = listOf("travel", "online"),
        tagNames = listOf("Travel", "Online"),
        note = "Everyday spending"
    )

    private val visaIcici = entry(
        id = "2",
        number = "4315810266740939",
        bankId = "icici",
        bankName = "ICICI Bank",
        typeId = "credit",
        typeName = "Credit",
        tagIds = listOf("online"),
        tagNames = listOf("Online")
    )

    private val amexPriya = entry(
        id = "3",
        number = "378282246310005",
        bankId = "amex",
        bankName = "American Express",
        holderId = "h2",
        holderName = "Priya Mehta",
        typeId = "credit",
        typeName = "Credit",
        networkId = "amex",
        networkName = "American Express",
        networkCode = "AMEX",
        tagIds = listOf("business", "travel"),
        tagNames = listOf("Business", "Travel")
    )

    private val cards = listOf(visaHdfc, visaIcici, amexPriya)

    @Test
    fun `empty filters pass everything`() {
        cards.forEach { assertTrue(CardQuery.passes(it, CardFilters())) }
    }

    @Test
    fun `holder filter is single select through the chip`() {
        val filters = CardFilters().withHolder("h2")
        assertEquals(listOf(amexPriya), cards.filter { CardQuery.passes(it, filters) })
        assertTrue(CardFilters().withHolder(null).holders.isEmpty())
    }

    @Test
    fun `groups are ANDed and values inside a group are ORed`() {
        val twoBanks = CardFilters(banks = setOf("bank-1", "icici"))
        assertEquals(setOf("1", "2"), cards.filter { CardQuery.passes(it, twoBanks) }.map { it.id }.toSet())

        val conflicting = CardFilters(banks = setOf("icici"), holders = setOf("h2"))
        assertTrue(cards.none { CardQuery.passes(it, conflicting) })
    }

    @Test
    fun `network and type filters use resolved ids`() {
        val network = CardFilters(networks = setOf("amex"))
        assertEquals(listOf("3"), cards.filter { CardQuery.passes(it, network) }.map { it.id })

        val type = CardFilters(types = setOf("credit"))
        assertEquals(3, cards.count { CardQuery.passes(it, type) })

        val missingType = CardFilters(types = setOf("debit"))
        assertTrue(cards.none { CardQuery.passes(it, missingType) })
    }

    @Test
    fun `tag filter matches any tag on the card`() {
        val travel = CardFilters(tags = setOf("travel"))
        assertEquals(setOf("1", "3"), cards.filter { CardQuery.passes(it, travel) }.map { it.id }.toSet())

        val online = CardFilters(tags = setOf("online"))
        assertEquals(setOf("1", "2"), cards.filter { CardQuery.passes(it, online) }.map { it.id }.toSet())
    }

    @Test
    fun `filter count ignores the holder chips`() {
        val filters = CardFilters(
            holders = setOf("h1"),
            banks = setOf("b"),
            networks = setOf("n"),
            types = setOf("t"),
            tags = setOf("g")
        )
        assertEquals(4, CardQuery.filterCount(filters))
        assertEquals(4, filters.activeCount)
        assertFalse(filters.isEmpty)
        assertTrue(CardFilters(holders = setOf("h1")).activeCount == 0)
    }

    @Test
    fun `digit search matches anywhere in the number and ignores spaces`() {
        assertEquals(listOf("1"), CardQuery.visibleCards(cards, CardFilters(), "2211").map { it.id })
        assertEquals(listOf("1"), CardQuery.visibleCards(cards, CardFilters(), " 2211 ").map { it.id })
        assertEquals(listOf("1"), CardQuery.visibleCards(cards, CardFilters(), "2778 1034").map { it.id })
        assertTrue(CardQuery.visibleCards(cards, CardFilters(), "9999").isEmpty())
    }

    @Test
    fun `text search covers bank holder note type and network`() {
        fun ids(query: String) = CardQuery.visibleCards(cards, CardFilters(), query).map { it.id }
        assertEquals(listOf("1"), ids("hdfc"))
        assertEquals(listOf("2"), ids("icici"))
        assertEquals(listOf("3"), ids("priya"))
        assertEquals(listOf("1"), ids("everyday"))
        assertEquals(listOf("3"), ids("amex"))
        assertEquals(listOf("3"), ids("american express"))
        assertEquals("network name", listOf("3"), ids("american"))
        assertEquals(listOf("1", "2", "3"), ids("credit"))
        assertTrue("card names are gone", ids("regalia").isEmpty())
        assertTrue("card names are gone", ids("amazon").isEmpty())
    }

    @Test
    fun `title is bank plus type when a type is set`() {
        assertEquals("HDFC Bank Credit", visaHdfc.title)
        assertEquals(
            "HDFC Bank",
            entry(id = "4", number = "4111111111111111", typeId = null, typeName = "").title
        )
    }

    @Test
    fun `tags are always searched`() {
        assertEquals(listOf("1", "3"), CardQuery.visibleCards(cards, CardFilters(), "travel").map { it.id })
    }

    @Test
    fun `filters and search compose`() {
        val filters = CardFilters(banks = setOf("icici"))
        assertEquals(
            listOf("2"),
            CardQuery.visibleCards(cards, filters, "icici").map { it.id }
        )
        assertTrue(CardQuery.visibleCards(cards, filters, "hdfc").isEmpty())
    }

    @Test
    fun `holder chips list only holders with cards and expose first names`() {
        val holders = listOf(
            NamedRef("h1", "Aarav Mehta"),
            NamedRef("h2", "Priya Mehta"),
            NamedRef("h3", "Rohan Mehta")
        )
        val chips = CardQuery.holdersWithCards(holders, cards)
        assertEquals(listOf("h1", "h2"), chips.map { it.id })
        assertEquals("Aarav", CardQuery.holderFirstName(chips[0].name))
        assertEquals("Priya", CardQuery.holderFirstName(chips[1].name))
    }

    @Test
    fun `filter sheet groups only values in use with counts in repository order`() {
        val holders = listOf(
            NamedRef("h1", "Aarav Mehta"),
            NamedRef("h2", "Priya Mehta"),
            NamedRef("h3", "Rohan Mehta")
        )
        val banks = listOf(
            NamedRef("amex", "American Express"),
            NamedRef("bank-1", "HDFC Bank"),
            NamedRef("icici", "ICICI Bank"),
            NamedRef("unused", "Yes Bank")
        )
        val networks = listOf(
            NamedRef("amex", "American Express"),
            NamedRef("visa", "Visa"),
            NamedRef("mc", "Mastercard")
        )
        val types = listOf(NamedRef("credit", "Credit"), NamedRef("debit", "Debit"))
        val tags = listOf(
            NamedRef("business", "Business"),
            NamedRef("online", "Online"),
            NamedRef("travel", "Travel")
        )

        val groups = CardQuery.filterGroups(cards, holders, banks, networks, types, tags)
        assertEquals(
            listOf(
                FilterGroup.HOLDER,
                FilterGroup.BANK,
                FilterGroup.NETWORK,
                FilterGroup.TYPE,
                FilterGroup.TAG
            ),
            groups.map { it.group }
        )

        val holderOptions = groups.first { it.group == FilterGroup.HOLDER }.options
        assertEquals(listOf("h1", "h2"), holderOptions.map { it.id })
        assertEquals(listOf(2, 1), holderOptions.map { it.count })

        val bankOptions = groups.first { it.group == FilterGroup.BANK }.options
        assertEquals(listOf("American Express", "HDFC Bank", "ICICI Bank"), bankOptions.map { it.label })
        assertEquals(listOf(1, 1, 1), bankOptions.map { it.count })

        val networkOptions = groups.first { it.group == FilterGroup.NETWORK }.options
        assertEquals(listOf("American Express", "Visa"), networkOptions.map { it.label })
        assertEquals(listOf(1, 2), networkOptions.map { it.count })

        val typeOptions = groups.first { it.group == FilterGroup.TYPE }.options
        assertEquals(listOf("Credit"), typeOptions.map { it.label })
        assertEquals(listOf(3), typeOptions.map { it.count })

        val tagOptions = groups.first { it.group == FilterGroup.TAG }.options
        assertEquals(listOf("Business", "Online", "Travel"), tagOptions.map { it.label })
        assertEquals(listOf(1, 2, 2), tagOptions.map { it.count })
    }

    @Test
    fun `filter sheet drops groups with no options`() {
        val empty = CardQuery.filterGroups(
            cards = emptyList(),
            holders = listOf(NamedRef("h1", "Aarav Mehta")),
            banks = emptyList(),
            networks = emptyList(),
            types = emptyList(),
            tags = emptyList()
        )
        assertTrue(empty.isEmpty())
    }

    @Test
    fun `toggling and removing filter values`() {
        var filters = CardFilters()
        filters = filters.toggle(FilterGroup.BANK, "hdfc")
        filters = filters.toggle(FilterGroup.BANK, "icici")
        filters = filters.toggle(FilterGroup.TAG, "travel")
        assertEquals(setOf("hdfc", "icici"), filters.ids(FilterGroup.BANK))
        assertEquals(3, filters.activeCount)

        filters = filters.toggle(FilterGroup.BANK, "hdfc")
        filters = filters.without(FilterGroup.TAG, "travel")
        assertEquals(setOf("icici"), filters.ids(FilterGroup.BANK))
        assertTrue(filters.ids(FilterGroup.TAG).isEmpty())

        filters = filters.without(FilterGroup.BANK, "missing")
        assertEquals(setOf("icici"), filters.ids(FilterGroup.BANK))
    }

    @Test
    fun `time text matches the prototype`() {
        assertEquals("Off", TimeText.seconds(0))
        assertEquals("30 seconds", TimeText.seconds(30))
        assertEquals("1 minute", TimeText.seconds(60))
        assertEquals("2 minutes", TimeText.seconds(120))
        assertEquals("30s", TimeText.clipText(30))
        assertEquals("1 min", TimeText.clipText(60))
        assertEquals("2 min", TimeText.clipText(120))
    }
}
