package com.quickcards.domain.property

import com.quickcards.domain.BackupCard
import com.quickcards.domain.BackupContent
import com.quickcards.domain.BackupPlanner
import com.quickcards.domain.PhoneCard
import com.quickcards.domain.PhoneSnapshot
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import kotlin.random.Random
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupPlannerPropertyTest {

    private val config = PropTestConfig(seed = 4_303L)
    private val seeds = Arb.int(0..Int.MAX_VALUE)

    /** MAINTAINING.md "Duplicate detection": the first copy of a number wins, later ones are skipped. */
    @Test
    fun `the plan never holds two cards with the same number`() = runBlocking<Unit> {
        checkAll(1_000, config, seeds) { seed ->
            val random = Random(seed)
            val cards = List(random.nextInt(0, 31)) { index ->
                BackupCard(id = "c$index", number = NUMBERS.random(random))
            }
            val phone = PhoneSnapshot(
                cards = List(random.nextInt(0, 6)) { index ->
                    PhoneCard(id = "p$index", number = NUMBERS.random(random))
                }
            )

            val plan = BackupPlanner.plan(BackupContent(cards = cards), phone)
            val numbers = plan.cards.map { BackupPlanner.normalizeNumber(it.card.number) }

            assertEquals("seed=$seed duplicate number in plan", numbers.distinct().size, numbers.size)
            assertEquals("seed=$seed", cards.size, plan.cards.size + plan.duplicateCards)

            val seen = mutableSetOf<String>()
            val firstCopies = cards
                .filter { seen.add(BackupPlanner.normalizeNumber(it.number)) }
                .map { it.id }
            assertEquals("seed=$seed first copy wins", firstCopies, plan.cards.map { it.card.id })
        }
    }

    private companion object {
        val NUMBERS = listOf(
            "4111111111111111",
            "4012888888881881",
            "5555555555554444",
            "378282246310005",
            "6759649826438453",
            " 4111 1111 1111 1111 ",
            "4111-1111-1111-1111",
            "4111111111111112",
            "0000000000000000"
        )
    }
}
