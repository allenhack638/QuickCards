package com.quickcards.app.core.data.repository

import com.quickcards.domain.CardField
import com.quickcards.domain.CardNetwork
import com.quickcards.domain.DefaultNetworks
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacyRepairTest {

    private val now = 1_700_000_000_000L

    private class FakeBackend : LegacyRepairBackend {
        val cards = linkedMapOf<String, RepairCardSnapshot>()
        val repairs = mutableListOf<RepairRecord>()
        var summary: RepairSummary? = null
        var pruneCutoff: Long? = null
        private var completed = false
        private var nextId = 1L

        override suspend fun cards(): List<RepairCardSnapshot> = cards.values.toList()

        override suspend fun networks(): List<CardNetwork> = DefaultNetworks.all

        override suspend fun saveCard(card: RepairCardSnapshot, at: Long): Boolean {
            cards[card.id] = card
            return true
        }

        override suspend fun addRepair(
            cardId: String,
            field: CardField,
            before: String,
            after: String,
            at: Long
        ) {
            repairs += RepairRecord(
                id = nextId++,
                cardId = cardId,
                field = field,
                before = before,
                after = after,
                at = at
            )
        }

        override suspend fun repairsFor(cardId: String, since: Long): List<RepairRecord> =
            repairs.filter { it.cardId == cardId && it.at >= since }
                .sortedWith(compareByDescending<RepairRecord> { it.at }.thenByDescending { it.id })

        override suspend fun deleteRepair(id: Long) {
            repairs.removeAll { it.id == id }
        }

        override suspend fun prune(cutoff: Long) {
            pruneCutoff = cutoff
            repairs.removeAll { it.at < cutoff }
        }

        override fun completed(): Boolean = completed

        override fun markCompleted() {
            completed = true
        }

        override fun recordSummary(summary: RepairSummary) {
            this.summary = summary
        }
    }

    private class FakeCrypto : RepairCrypto {
        override fun encrypt(value: String): String = "enc:$value"
        override fun decrypt(value: String): String =
            if (value.startsWith("enc:")) value.removePrefix("enc:") else value
    }

    private fun engine(): Pair<LegacyRepairEngine, FakeBackend> {
        val backend = FakeBackend()
        return LegacyRepairEngine(backend, FakeCrypto()) to backend
    }

    private fun backendWithCards(): FakeBackend {
        val backend = FakeBackend()
        backend.cards["card-fix"] = RepairCardSnapshot(
            id = "card-fix",
            number = "4111 1111 1111 1111",
            expiry = "11//22",
            cvv = " 12 3",
            needsReview = 0
        )
        backend.cards["card-check"] = RepairCardSnapshot(
            id = "card-check",
            number = "4111 1111 1111 1112",
            expiry = "12/29",
            cvv = "",
            needsReview = 0
        )
        backend.cards["card-clean"] = RepairCardSnapshot(
            id = "card-clean",
            number = "4111111111111111",
            expiry = "12/29",
            cvv = "381",
            needsReview = 0
        )
        return backend
    }

    @Test
    fun `repair runs once and records the summary`() = runBlockingTest {
        val backend = backendWithCards()
        val engine = LegacyRepairEngine(backend, FakeCrypto())

        val summary = engine.run(now)

        assertEquals(RepairSummary(fixedCards = 1, needsCheck = 1), summary)
        assertTrue(backend.completed())
        assertEquals(summary, backend.summary)
        assertEquals(now - TimeUnit.DAYS.toMillis(30), backend.pruneCutoff)

        assertEquals("4111111111111111", backend.cards.getValue("card-fix").number)
        assertEquals("11/22", backend.cards.getValue("card-fix").expiry)
        assertEquals("123", backend.cards.getValue("card-fix").cvv)
        assertEquals(0, backend.cards.getValue("card-fix").needsReview)

        assertEquals("4111 1111 1111 1112", backend.cards.getValue("card-check").number)
        assertEquals(5, backend.cards.getValue("card-check").needsReview)

        assertEquals("4111111111111111", backend.cards.getValue("card-clean").number)
        assertEquals(0, backend.cards.getValue("card-clean").needsReview)

        val again = engine.run(now + 1)
        assertNull(again)
        assertEquals(3, backend.repairs.size)
    }

    @Test
    fun `repairs store encrypted before and after values`() = runBlockingTest {
        val backend = backendWithCards()
        val engine = LegacyRepairEngine(backend, FakeCrypto())

        engine.run(now)

        assertEquals(3, backend.repairs.size)
        backend.repairs.forEach { repair ->
            assertEquals("card-fix", repair.cardId)
            assertTrue(repair.before.startsWith("enc:"))
            assertTrue(repair.after.startsWith("enc:"))
        }

        val expiryRepair = backend.repairs.single { it.field == CardField.EXPIRY }
        assertEquals("enc:11//22", expiryRepair.before)
        assertEquals("enc:11/22", expiryRepair.after)
        assertEquals("11//22", FakeCrypto().decrypt(expiryRepair.before))
        assertEquals("11/22", FakeCrypto().decrypt(expiryRepair.after))
    }

    @Test
    fun `undo restores the original value and recomputes the review mask`() = runBlockingTest {
        val backend = backendWithCards()
        val engine = LegacyRepairEngine(backend, FakeCrypto())
        engine.run(now)

        val undone = engine.undo("card-fix", CardField.EXPIRY, now + 1)

        assertTrue(undone)
        assertEquals("11//22", backend.cards.getValue("card-fix").expiry)
        assertEquals("4111111111111111", backend.cards.getValue("card-fix").number)
        assertEquals("123", backend.cards.getValue("card-fix").cvv)
        assertEquals(2, backend.cards.getValue("card-fix").needsReview)
        assertEquals(2, backend.repairs.size)
        assertTrue(backend.repairs.none { it.field == CardField.EXPIRY })

        assertFalse(engine.undo("card-fix", CardField.EXPIRY, now + 2))
    }

    @Test
    fun `an expired repair cannot be undone`() = runBlockingTest {
        val backend = backendWithCards()
        val engine = LegacyRepairEngine(backend, FakeCrypto())
        engine.run(now)

        val later = now + TimeUnit.DAYS.toMillis(31)
        assertFalse(engine.undo("card-fix", CardField.NUMBER, later))
        assertEquals("4111111111111111", backend.cards.getValue("card-fix").number)
    }

    @Test
    fun `a clean vault marks the job complete without a summary`() = runBlockingTest {
        val backend = FakeBackend()
        backend.cards["card-clean"] = RepairCardSnapshot(
            id = "card-clean",
            number = "4111111111111111",
            expiry = "12/29",
            cvv = "381",
            needsReview = 0
        )
        val engine = LegacyRepairEngine(backend, FakeCrypto())

        assertNull(engine.run(now))
        assertTrue(backend.completed())
        assertNull(backend.summary)
        assertTrue(backend.repairs.isEmpty())
    }
}

/** Small coroutine runner so this test needs no extra test dependency. */
private fun runBlockingTest(block: suspend () -> Unit) {
    kotlinx.coroutines.runBlocking { block() }
}
