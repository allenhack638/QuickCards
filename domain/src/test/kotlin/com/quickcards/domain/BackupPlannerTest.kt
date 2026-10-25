package com.quickcards.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupPlannerTest {

    private val phone = PhoneSnapshot(
        holders = listOf(
            SnapshotItem("phone-aarav", "Aarav Mehta"),
            SnapshotItem("phone-priya", "Priya Mehta")
        ),
        banks = listOf(
            SnapshotItem("phone-hdfc", "HDFC Bank"),
            SnapshotItem("phone-icici", "ICICI Bank")
        ),
        networks = listOf(
            SnapshotItem("phone-visa", "Visa"),
            SnapshotItem("phone-mc", "Mastercard")
        ),
        types = listOf(
            SnapshotItem("phone-credit", "Credit"),
            SnapshotItem("phone-debit", "Debit")
        ),
        tags = listOf(
            SnapshotItem("phone-travel", "Travel"),
            SnapshotItem("phone-online", "Online")
        ),
        cards = listOf(
            PhoneCard(
                id = "phone-card-1",
                number = "4532778103492219",
                holderId = "phone-aarav",
                bankId = "phone-hdfc",
                networkAuto = true,
                networkId = null,
                typeId = "phone-credit",
                variant = "Regalia",
                exp = "12/29",
                cvv = "381",
                nameOnCard = "AARAV MEHTA",
                color = "navy",
                note = "Everyday spending",
                tagIds = listOf("phone-travel")
            ),
            PhoneCard(
                id = "phone-card-2",
                number = "431581026674093",
                holderId = "phone-aarav",
                bankId = "phone-icici",
                networkAuto = true,
                typeId = "phone-credit",
                variant = "Amazon Pay",
                exp = "08/28",
                cvv = "902",
                nameOnCard = "AARAV MEHTA",
                color = "rust",
                note = "",
                tagIds = listOf("phone-online")
            )
        )
    )

    private fun backupContent(): BackupContent = BackupContent(
        meta = BackupMeta(version = "2.0", exportedAt = "2026-10-23"),
        holders = listOf(
            BackupNamed("bh1", "aarav mehta"),
            BackupNamed("bh2", "New Holder")
        ),
        banks = listOf(
            BackupBank("bb1", "hdfc bank", "HD"),
            BackupBank("bb2", "Axis Bank", "AB")
        ),
        networks = listOf(
            BackupNetwork("bn1", "VISA", "VISA", "4", "13,16,19"),
            BackupNetwork("bn2", "RuPay", "RUPAY", "6521", "16")
        ),
        types = listOf(
            BackupNamed("bt1", "credit"),
            BackupNamed("bt2", "Forex")
        ),
        tags = listOf(
            BackupNamed("bg1", "travel"),
            BackupNamed("bg2", "Fuel")
        ),
        cards = listOf(
            BackupCard(
                id = "bc1",
                number = "4532 7781 0349 2219",
                exp = "12/29",
                cvv = "381",
                nameOnCard = "AARAV MEHTA",
                holderId = "bh1",
                bankId = "bb1",
                typeId = "bt1",
                variant = "Regalia",
                note = "Everyday spending",
                color = "navy",
                tagIds = listOf("bg1")
            ),
            BackupCard(
                id = "bc2",
                number = "431581026674093",
                exp = "09/29",
                cvv = "902",
                nameOnCard = "AARAV MEHTA",
                holderId = "bh1",
                bankId = "bb2",
                typeId = "bt1",
                variant = "Amazon Pay",
                note = "",
                color = "rust",
                tagIds = listOf("bg2")
            ),
            BackupCard(
                id = "bc3",
                number = "524199001137448",
                exp = "11/30",
                cvv = "645",
                nameOnCard = "NEW HOLDER",
                holderId = "bh2",
                bankId = "bb2",
                typeId = "bt2",
                variant = "Flipkart",
                note = "",
                color = "rust",
                tagIds = listOf("bg2")
            )
        )
    )

    @Test
    fun `planner counts new changed and same cards`() {
        val plan = BackupPlanner.plan(backupContent(), phone)

        assertEquals(1, plan.newCards)
        assertEquals(1, plan.changedCards)
        assertEquals(1, plan.sameCards)
        assertEquals(3, plan.cards.size)
    }

    @Test
    fun `planner counts new list items ignoring name case`() {
        val plan = BackupPlanner.plan(backupContent(), phone)

        assertEquals(1, plan.newCount(BackupSection.HOLDERS))
        assertEquals(1, plan.newCount(BackupSection.BANKS))
        assertEquals(1, plan.newCount(BackupSection.NETWORKS))
        assertEquals(1, plan.newCount(BackupSection.TYPES))
        assertEquals(1, plan.newCount(BackupSection.TAGS))
    }

    @Test
    fun `matching cards resolve the phone's list ids`() {
        val plan = BackupPlanner.plan(backupContent(), phone)
        val same = plan.cards.first { !it.isNew && !it.changed }

        assertEquals("phone-card-1", same.existingId)
        assertEquals("phone-aarav", same.resolved.holderId)
        assertEquals("phone-hdfc", same.resolved.bankId)
        assertEquals("phone-credit", same.resolved.typeId)
        assertEquals(listOf("phone-travel"), same.resolved.tagIds)
        assertNull(same.resolved.networkId)
        assertEquals("navy", same.resolved.color)
    }

    @Test
    fun `a card whose holder name is new is changed even when other fields match`() {
        val content = backupContent()
        val moved = content.cards[0].copy(holderId = "bh2")
        val plan = BackupPlanner.plan(content.copy(cards = listOf(moved)), phone)
        val card = plan.cards.single()

        assertTrue(card.changed)
        assertNull(card.resolved.holderId)
    }

    @Test
    fun `tag order does not count as a change`() {
        val content = backupContent()
        val reordered = content.cards[0].copy(tagIds = listOf("bg1"))
        val existing = phone.cards[0].copy(tagIds = listOf("phone-travel"))
        val plan = BackupPlanner.plan(
            content.copy(cards = listOf(reordered)),
            phone.copy(cards = listOf(existing))
        )

        assertFalse(plan.cards.single().changed)
        assertEquals(1, plan.sameCards)
    }

    @Test
    fun `update and keep are decided per changed card`() {
        val plan = BackupPlanner.plan(backupContent(), phone)
        val changed = plan.cards.first { !it.isNew && it.changed }
        val same = plan.cards.first { !it.isNew && !it.changed }
        val added = plan.cards.first { it.isNew }

        assertEquals(CardDisposition.UPDATE, changed.disposition(MergeMode.UPDATE))
        assertEquals(CardDisposition.KEEP, changed.disposition(MergeMode.KEEP))
        assertEquals(CardDisposition.SAME, same.disposition(MergeMode.UPDATE))
        assertEquals(CardDisposition.SAME, same.disposition(MergeMode.KEEP))
        assertEquals(CardDisposition.ADD, added.disposition(MergeMode.UPDATE))
        assertEquals(CardDisposition.ADD, added.disposition(MergeMode.KEEP))
    }

    @Test
    fun `legacy issuer maps to an existing network and stays auto when unknown`() {
        val known = BackupCard(
            number = "4532778103492219",
            legacyIssuer = "VISA"
        )
        val unknown = BackupCard(
            number = "431581026674093",
            legacyIssuer = "Some Local Network"
        )

        val knownResolved = BackupPlanner.resolve(
            known,
            emptyMap(),
            emptyMap(),
            emptyMap(),
            emptyMap(),
            emptyMap(),
            mapOf("visa" to "phone-visa")
        )
        assertEquals(false, knownResolved.networkAuto)
        assertEquals("phone-visa", knownResolved.networkId)

        val unknownResolved = BackupPlanner.resolve(
            unknown,
            emptyMap(),
            emptyMap(),
            emptyMap(),
            emptyMap(),
            emptyMap(),
            mapOf("visa" to "phone-visa")
        )
        assertEquals(true, unknownResolved.networkAuto)
        assertNull(unknownResolved.networkId)
    }

    @Test
    fun `pinning a saved card counts as a change`() {
        val content = backupContent()
        val pinned = content.cards[0].copy(pinned = true, pinnedAt = 1_700_000_000_000L)
        val plan = BackupPlanner.plan(content.copy(cards = listOf(pinned)), phone)
        val card = plan.cards.single()

        assertTrue(card.changed)
        assertTrue(card.resolved.pinned)
        assertEquals(1_700_000_000_000L, card.resolved.pinnedAt)
    }

    @Test
    fun `archiving a saved card counts as a change`() {
        val content = backupContent()
        val archived = content.cards[0].copy(
            archived = true,
            archivedAt = 1_700_000_000_000L,
            archiveReason = "Card closed"
        )

        val plan = BackupPlanner.plan(content.copy(cards = listOf(archived)), phone)
        val card = plan.cards.single()

        assertTrue(card.changed)
        assertTrue(card.resolved.archived)
        assertEquals(1_700_000_000_000L, card.resolved.archivedAt)
        assertEquals("Card closed", card.resolved.archiveReason)
    }

    @Test
    fun `backups from before archiving stay active and unreviewed`() {
        val plan = BackupPlanner.plan(backupContent(), phone)
        val card = plan.cards.first { !it.isNew && !it.changed }.resolved

        assertFalse(card.archived)
        assertNull(card.archivedAt)
        assertEquals("", card.archiveReason)
    }

    @Test
    fun `backups from before pinning stay unpinned`() {
        val plan = BackupPlanner.plan(backupContent(), phone)

        assertFalse(plan.cards.first { !it.isNew && !it.changed }.resolved.pinned)
        assertNull(plan.cards.first { !it.isNew && !it.changed }.resolved.pinnedAt)
    }

    @Test
    fun `an empty backup produces no changes`() {
        val plan = BackupPlanner.plan(BackupContent(), phone)

        assertEquals(0, plan.newCards)
        assertEquals(0, plan.changedCards)
        assertEquals(0, plan.sameCards)
        assertEquals(0, plan.newCount(BackupSection.HOLDERS))
    }


    @Test
    fun aRepeatedNumberInTheFileIsSkippedAndCounted() {
        val content = BackupContent(
            cards = listOf(
                BackupCard(id = "a", number = "4532 7781 0349 2211", note = "first"),
                BackupCard(id = "b", number = "4532778103492211", note = "second"),
                BackupCard(id = "c", number = "5555555555554444")
            )
        )
        val plan = BackupPlanner.plan(content, PhoneSnapshot())
        assertEquals(listOf("a", "c"), plan.cards.map { it.card.id })
        assertEquals(1, plan.duplicateCards)
        assertEquals(2, plan.newCards)
    }

}
