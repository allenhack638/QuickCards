package com.quickcards.app.core.data.backup

import com.quickcards.domain.BackupCard
import com.quickcards.domain.BackupContent
import com.quickcards.domain.BackupNetwork
import com.quickcards.domain.CardNetwork
import com.quickcards.domain.CardNumberRules
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import kotlin.random.Random
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupNormalizerPropertyTest {

    private val config = PropTestConfig(seed = 4_302L)
    private val seeds = Arb.int(0..Int.MAX_VALUE)

    /** LegacyCardNormalizer is documented idempotent; a second import must not rewrite again. */
    @Test
    fun `normalizing twice equals normalizing once`() = runBlocking<Unit> {
        checkAll(1_000, config, seeds) { seed ->
            val random = Random(seed)
            val content = content(random)
            val phoneNetworks = List(random.nextInt(0, 5)) { phoneNetwork(random, it) }

            val once = BackupNormalizer.normalize(content, phoneNetworks)
            val twice = BackupNormalizer.normalize(once.content, phoneNetworks)

            assertEquals("seed=$seed", once.content, twice.content)
        }
    }

    private fun content(random: Random): BackupContent = BackupContent(
        networks = List(random.nextInt(0, 5)) { backupNetwork(random, it) },
        cards = List(random.nextInt(0, 11)) { index ->
            BackupCard(
                id = "c$index",
                number = NUMBERS.random(random),
                exp = EXPIRIES.random(random),
                cvv = CVVS.random(random),
                nameOnCard = "",
                note = ""
            )
        }
    )

    private fun backupNetwork(random: Random, index: Int) = BackupNetwork(
        id = "n$index",
        name = NETWORK_NAMES.random(random),
        code = "N$index",
        prefixes = PREFIXES.random(random),
        lengths = LENGTHS.random(random),
        cvv = if (random.nextBoolean()) 3 else 4,
        luhn = random.nextBoolean(),
        groups = GROUPS.random(random),
        cvvOptional = random.nextBoolean()
    )

    private fun phoneNetwork(random: Random, index: Int) = CardNetwork(
        id = "p$index",
        name = NETWORK_NAMES.random(random),
        code = "P$index",
        prefixes = PREFIXES.random(random),
        lengths = PHONE_LENGTHS.random(random),
        cvvLength = if (random.nextBoolean()) 3 else 4,
        luhn = random.nextBoolean(),
        groups = GROUPS.random(random),
        cvvOptional = random.nextBoolean()
    )

    private companion object {
        val LOCAL_NUMBER = ("88" + "1".repeat(13)).let {
            it + CardNumberRules.luhnCheckDigit(it)
        }
        val NUMBERS = listOf(
            "4111111111111111",
            "4012888888881881",
            "5555555555554444",
            "378282246310005",
            "6759649826438453",
            "4111 1111 1111 1111",
            "4111-1111-1111-1111",
            LOCAL_NUMBER,
            "",
            "1234",
            "not a number",
            "0000000000000000",
            "4111111111111112"
        )
        val EXPIRIES = listOf(
            "", "12/29", "11//22", "1/2", "13/12", "12-30", "12.30",
            "1229", "122029", "2030/12", "abc", "00/00", "12/2030"
        )
        val CVVS = listOf("", "123", "12 3", "1234", "12", "abc", "  ")
        val NETWORK_NAMES = listOf(
            "Visa", "Mastercard", "American Express", "Maestro", "Local Debit", "RuPay"
        )
        val PREFIXES = listOf("", "4", "5", "88", "34,37", "6759")
        val LENGTHS = listOf("", "16", "15", "13,16,19", "12-19")
        val PHONE_LENGTHS = listOf("16", "15", "13,16,19", "12-19")
        val GROUPS = listOf("", "4-6-5", "4-4-4-4")
    }
}
