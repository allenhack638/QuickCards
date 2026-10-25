package com.quickcards.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EntityUsageTest {

    private val networks = listOf(
        UsageNetwork("visa", "4"),
        UsageNetwork("mc", "51-55,2221-2720"),
        UsageNetwork("amex", "34,37")
    )

    private fun card(
        id: String,
        number: String,
        holderId: String? = "h1",
        bankId: String? = "b1",
        typeId: String? = "t1",
        networkAuto: Boolean = true,
        networkId: String? = null,
        tagIds: List<String> = emptyList(),
        bankName: String = "HDFC Bank",
        typeName: String = "Credit",
        holderName: String = "Aarav Mehta",
        bandColor: String = "navy"
    ): UsageCard = UsageCard(
        id = id,
        number = number,
        bankName = bankName,
        typeName = typeName,
        holderId = holderId,
        holderName = holderName,
        bankId = bankId,
        typeId = typeId,
        networkId = EntityUsage.resolveNetworkId(number, networkAuto, networkId, networks),
        tagIds = tagIds,
        bandColor = bandColor
    )

    private val cards = listOf(
        card("c1", "4111111111111111", tagIds = listOf("tg1", "tg2")),
        card(
            "c2",
            "378282246310005",
            holderId = "h2",
            bankId = "b2",
            typeId = "t2",
            bankName = "American Express",
            typeName = "Credit",
            holderName = "Priya Mehta",
            bandColor = "plum"
        ),
        card(
            "c3",
            "4111111111111111",
            networkAuto = false,
            networkId = "mc",
            holderId = "h2",
            bankId = "b2",
            typeId = null,
            tagIds = listOf("tg2"),
            bankName = "ICICI Bank",
            typeName = "",
            holderName = "Priya Mehta",
            bandColor = "rust"
        )
    )

    @Test
    fun autoDetectedNetworksCountTowardsTheirUsage() {
        assertEquals(1, EntityUsage.usedCards(EntityKind.NETWORK, "visa", cards).size)
        assertEquals(1, EntityUsage.usedCards(EntityKind.NETWORK, "amex", cards).size)
        assertEquals(listOf("c3"), EntityUsage.usedCards(EntityKind.NETWORK, "mc", cards).map { it.id })
    }

    @Test
    fun manualNetworkNeverCountsTowardsTheDetectedOne() {
        val manualCard = card("c9", "4111111111111111", networkAuto = false, networkId = "mc")
        assertEquals(emptyList<UsageCard>(), EntityUsage.usedCards(EntityKind.NETWORK, "visa", listOf(manualCard)))
        assertEquals(listOf("c9"), EntityUsage.usedCards(EntityKind.NETWORK, "mc", listOf(manualCard)).map { it.id })
    }

    @Test
    fun networkResolutionUsesTheLongestMatchingPrefix() {
        assertEquals("visa", EntityUsage.resolveNetworkId("4111111111111111", true, null, networks))
        assertEquals("amex", EntityUsage.resolveNetworkId("378282246310005", true, null, networks))
        assertEquals("mc", EntityUsage.resolveNetworkId("5100000000000000", true, null, networks))
        assertNull(EntityUsage.resolveNetworkId("9999999999999999", true, null, networks))
        assertNull(EntityUsage.resolveNetworkId("4111111111111111", true, null, emptyList()))
    }

    @Test
    fun everyKindCountsTheCardsThatUseIt() {
        assertEquals(1, EntityUsage.usedCards(EntityKind.HOLDER, "h1", cards).size)
        assertEquals(2, EntityUsage.usedCards(EntityKind.HOLDER, "h2", cards).size)
        assertEquals(1, EntityUsage.usedCards(EntityKind.BANK, "b1", cards).size)
        assertEquals(2, EntityUsage.usedCards(EntityKind.BANK, "b2", cards).size)
        assertEquals(1, EntityUsage.usedCards(EntityKind.TYPE, "t2", cards).size)
        assertEquals(1, EntityUsage.usedCards(EntityKind.TYPE, "t1", cards).size)
        assertEquals(2, EntityUsage.usedCards(EntityKind.TAG, "tg2", cards).size)
        assertEquals(1, EntityUsage.usedCards(EntityKind.TAG, "tg1", cards).size)
    }

    @Test
    fun countsMapMatchesUsedCards() {
        assertEquals(
            mapOf("h1" to 1, "h2" to 2, "h3" to 0),
            EntityUsage.counts(EntityKind.HOLDER, listOf("h1", "h2", "h3"), cards)
        )
    }

    @Test
    fun cardsCarryTheirPrototypeTitleAndLastFour() {
        val card = cards[1]
        assertEquals("American Express Credit", card.title)
        assertEquals("0005", card.lastFour)
        assertEquals("Priya", card.holderFirstName)
        assertEquals("ICICI Bank", cards[2].title)
    }

    @Test
    fun aNewEntityNeverAsksForConfirmation() {
        assertFalse(
            EntityUsage.requiresUpdateConfirmation(
                EntityKind.BANK,
                before = null,
                draft = EntityDraft(EntityKind.BANK, "New Bank"),
                usedCount = 4
            )
        )
    }

    @Test
    fun anUnusedEditNeverAsksForConfirmation() {
        val before = EntityBefore(name = "HDFC Bank")
        assertFalse(
            EntityUsage.requiresUpdateConfirmation(
                EntityKind.BANK,
                before,
                EntityDraft(EntityKind.BANK, "HDFC Bank Two"),
                usedCount = 0
            )
        )
    }

    @Test
    fun renamingAnInUseEntityAsksForConfirmation() {
        val before = EntityBefore(name = "HDFC Bank")
        assertTrue(
            EntityUsage.requiresUpdateConfirmation(
                EntityKind.BANK,
                before,
                EntityDraft(EntityKind.BANK, "HDFC Bank Two"),
                usedCount = 2
            )
        )
        assertFalse(
            EntityUsage.requiresUpdateConfirmation(
                EntityKind.BANK,
                before,
                EntityDraft(EntityKind.BANK, "  HDFC   Bank "),
                usedCount = 2
            )
        )
    }

    @Test
    fun changingANetworkRuleAsksForConfirmation() {
        val before = EntityBefore(
            name = "Visa",
            code = "VISA",
            prefixes = "4",
            lengths = "13,16,19",
            cvvLength = 3
        )
        fun confirm(draft: EntityDraft) = EntityUsage.requiresUpdateConfirmation(
            EntityKind.NETWORK,
            before,
            draft,
            usedCount = 1
        )

        assertFalse(confirm(EntityDraft(EntityKind.NETWORK, "Visa", "VISA", "4", "13,16,19")))
        assertTrue(confirm(EntityDraft(EntityKind.NETWORK, "Visa", "VISA", "4", "16")))
        assertTrue(confirm(EntityDraft(EntityKind.NETWORK, "Visa", "VISA2", "4", "13,16,19")))
        assertTrue(confirm(EntityDraft(EntityKind.NETWORK, "Visa", "VISA", "41", "13,16,19")))
        assertTrue(confirm(EntityDraft(EntityKind.NETWORK, "Visa", "VISA", "4", "13,16,19", cvvLength = 4)))
        assertTrue(confirm(EntityDraft(EntityKind.NETWORK, "Visa Two", "VISA", "4", "13,16,19")))
    }

    @Test
    fun whitespaceInRulesIsNotAChange() {
        val before = EntityBefore(
            name = "Visa",
            code = "VISA",
            prefixes = "4",
            lengths = "13,16,19",
            cvvLength = 3
        )
        assertFalse(
            EntityUsage.requiresUpdateConfirmation(
                EntityKind.NETWORK,
                before,
                EntityDraft(EntityKind.NETWORK, "Visa", " visa ", " 4 ", " 13, 16, 19 "),
                usedCount = 3
            )
        )
    }

    @Test
    fun theLuhnFlagIsNotPartOfThePrototypeChangeTest() {
        val before = EntityBefore(
            name = "UnionPay",
            code = "UPI",
            prefixes = "62",
            lengths = "16-19",
            cvvLength = 3
        )
        assertFalse(
            EntityUsage.requiresUpdateConfirmation(
                EntityKind.NETWORK,
                before,
                EntityDraft(EntityKind.NETWORK, "UnionPay", "UPI", "62", "16-19", luhn = true),
                usedCount = 2
            )
        )
    }

    @Test
    fun deletingAnInUseEntityIsBlocked() {
        assertEquals(EntityDeleteDecision.BLOCKED, EntityUsage.deleteDecision(1))
        assertEquals(EntityDeleteDecision.BLOCKED, EntityUsage.deleteDecision(9))
        assertEquals(EntityDeleteDecision.ALLOWED, EntityUsage.deleteDecision(0))
    }
}
