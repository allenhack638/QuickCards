package com.quickcards.app.feature.cardform

import com.quickcards.domain.DefaultNetworks
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import java.time.YearMonth
import kotlin.random.Random
import kotlinx.coroutines.runBlocking
import org.junit.Test

/**
 * Generated-input checks for smart paste. Fixed seeds so a failure reproduces.
 * Contract: MAINTAINING.md §5.12 rule 5 - junk text keeps the digit filter and never crashes.
 */
class SmartPastePropertyTest {

    private val today = YearMonth.of(2026, 10)

    private val pieces = listOf(
        "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
        "a", "Z", " ", "\n", "\r\n", "\t", "\u0000", "\u0007", "\u001B",
        "/", "-", ".",
        "😀", "🧑🏽‍🚀", "שלום", "مرحبا", "٤١١١", "４１１１", "\uD83D",
        "1".repeat(40), "9".repeat(200), "1234567890".repeat(500)
    )

    private fun anyText(random: Random): String {
        val builder = StringBuilder()
        val target = random.nextInt(0, 5_001)
        while (builder.length < target) builder.append(pieces.random(random))
        return builder.toString().take(5_000)
    }

    // Contract: every insert, including junk, is handled without throwing (§5.12 rule 5).
    @Test
    fun applyNeverThrowsOnAnyFieldTextUpTo5000Chars() = runBlocking<Unit> {
        checkAll(1_000, PropTestConfig(seed = 3_110L), Arb.int(0..Int.MAX_VALUE)) { seed ->
            val random = Random(seed)
            SmartPaste.apply(
                previous = anyText(random),
                incoming = anyText(random),
                field = SmartPasteField.entries[random.nextInt(SmartPasteField.entries.size)],
                networks = DefaultNetworks.all,
                today = today,
                expiryEmpty = random.nextBoolean()
            )
        }
    }
}
