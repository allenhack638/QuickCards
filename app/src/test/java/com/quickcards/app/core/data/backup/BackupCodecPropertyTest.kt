package com.quickcards.app.core.data.backup

import com.quickcards.app.core.data.model.BandColor
import com.quickcards.domain.BACKUP_FORMAT
import com.quickcards.domain.BackupBank
import com.quickcards.domain.BackupCard
import com.quickcards.domain.BackupContent
import com.quickcards.domain.BackupFormatException
import com.quickcards.domain.BackupMeta
import com.quickcards.domain.BackupNamed
import com.quickcards.domain.BackupNetwork
import com.quickcards.domain.BackupSection
import com.quickcards.domain.CardValidator
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import kotlin.random.Random
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCodecPropertyTest {

    private val config = PropTestConfig(seed = 4_301L)
    private val seeds = Arb.int(0..Int.MAX_VALUE)

    /** MAINTAINING.md "Backup": format 2 carries every list and card field, so it round-trips. */
    @Test
    fun `format 2 round trip returns the exported vault`() = runBlocking<Unit> {
        checkAll(1_000, config, seeds) { seed ->
            val content = vault(Random(seed), maxCards = 50)

            val decoded = BackupCodec.decodePayload(BackupCodec.encode(content))

            assertEquals("seed=$seed", content, decoded.content)
            assertEquals(BACKUP_FORMAT, decoded.format)
            assertEquals(BackupSection.entries.toSet(), decoded.sections)
        }
    }

    /** AppSettings.applyMap reads cfg numbers through `as? Number`, so a restore keeps the value. */
    @Test
    fun `numeric settings keep their value`() = runBlocking<Unit> {
        checkAll(1_000, config, seeds) { seed ->
            val random = Random(seed)
            val settings = linkedMapOf<String, Any?>(
                "copyWindowSec" to random.nextInt(0, 3_600),
                "clipClearSec" to random.nextInt(0, 3_600),
                "appLockAfterSec" to random.nextInt(0, 3_600)
            )
            val content = BackupContent(meta = BackupMeta(), settings = settings)

            val decoded = requireNotNull(
                BackupCodec.decodePayload(BackupCodec.encode(content)).content.settings
            )

            settings.forEach { (key, value) ->
                assertEquals("seed=$seed key=$key", value, (decoded[key] as Number).toInt())
            }
        }
    }

    /**
     * DESIGN.md "Card colour": slate left the grid but stored slate cards still render, so an
     * export must keep it. The import normaliser maps it to navy.
     */
    @Test
    fun `bug_stored_slate_colour_is_restored_as_navy`() = runBlocking<Unit> {
        val storedColours = BandColor.ALL + BandColor.SLATE
        checkAll(1_000, config, seeds) { seed ->
            val random = Random(seed)
            val colour = storedColours[seed % storedColours.size]
            val card = BackupCard(
                id = "card-${random.nextInt(1_000)}",
                number = digits(random, random.nextInt(1, 20)),
                color = colour
            )
            val content = BackupContent(meta = BackupMeta(), cards = listOf(card))

            val decoded = BackupCodec.decodePayload(BackupCodec.encode(content)).content

            assertEquals("seed=$seed colour=$colour", colour, decoded.cards.single().color)
        }
    }

    /** BackupCodec.decodePayload reports bad input as BackupFormatException, never another throw. */
    @Test
    fun `random bytes only ever fail as a format error`() = runBlocking<Unit> {
        checkAll(1_000, config, seeds) { seed ->
            val random = Random(seed)
            val payload = buildString {
                repeat(random.nextInt(0, 121)) {
                    append(GARBAGE_UNITS.random(random))
                }
            }

            val failure = runCatching { BackupCodec.decodePayload(payload) }.exceptionOrNull()

            if (failure != null) {
                assertTrue(
                    "seed=$seed payload=<$payload> threw $failure",
                    failure is BackupFormatException
                )
            }
        }
    }

    /** A cut-off export is not valid JSON, so it must be rejected as a format error. */
    @Test
    fun `a truncated export fails cleanly`() = runBlocking<Unit> {
        checkAll(1_000, config, seeds) { seed ->
            val random = Random(seed)
            val encoded = BackupCodec.encode(vault(random, maxCards = 3))
            val cut = random.nextInt(0, encoded.length)

            val failure = runCatching {
                BackupCodec.decodePayload(encoded.substring(0, cut))
            }.exceptionOrNull()

            assertTrue("seed=$seed cut=$cut", failure is BackupFormatException)
        }
    }

    private fun vault(random: Random, maxCards: Int): BackupContent {
        val holders = List(random.nextInt(0, 4)) { BackupNamed("h$it", name(random)) }
        val banks = List(random.nextInt(0, 4)) {
            BackupBank("b$it", name(random), if (random.nextBoolean()) word(random) else "")
        }
        val types = List(random.nextInt(0, 4)) { BackupNamed("t$it", name(random)) }
        val tags = List(random.nextInt(0, 4)) { BackupNamed("g$it", name(random)) }
        val networks = List(random.nextInt(0, 4)) { network(random, it) }

        return BackupContent(
            meta = BackupMeta(
                version = if (random.nextBoolean()) "" else word(random),
                exportedAt = if (random.nextBoolean()) "" else name(random)
            ),
            holders = holders,
            banks = banks,
            types = types,
            tags = tags,
            networks = networks,
            cards = List(random.nextInt(0, maxCards + 1)) {
                card(random, it, holders, banks, types, tags, networks)
            },
            settings = settings(random)
        )
    }

    private fun card(
        random: Random,
        index: Int,
        holders: List<BackupNamed>,
        banks: List<BackupBank>,
        types: List<BackupNamed>,
        tags: List<BackupNamed>,
        networks: List<BackupNetwork>
    ): BackupCard {
        val pinned = random.nextBoolean()
        val archived = random.nextBoolean()
        return BackupCard(
            id = "card-$index",
            number = digits(random, random.nextInt(1, 20)),
            exp = if (random.nextBoolean()) "" else word(random),
            cvv = if (random.nextBoolean()) "" else digits(random, random.nextInt(3, 5)),
            nameOnCard = if (random.nextBoolean()) "" else name(random),
            holderId = holders.pick(random)?.id,
            bankId = banks.pick(random)?.id,
            networkAuto = random.nextBoolean(),
            networkId = networks.pick(random)?.id,
            typeId = types.pick(random)?.id,
            variant = if (random.nextBoolean()) "" else word(random),
            note = note(random),
            dueDay = if (random.nextBoolean()) null else random.nextInt(1, 32),
            color = BandColor.ALL.random(random),
            tagIds = tags.filter { random.nextBoolean() }.map { it.id },
            sortOrder = random.nextInt(-5, 6),
            pinned = pinned,
            pinnedAt = if (pinned && random.nextBoolean()) {
                random.nextLong(0, 2_000_000_000_000L)
            } else {
                null
            },
            archived = archived,
            archivedAt = if (archived && random.nextBoolean()) {
                random.nextLong(0, 2_000_000_000_000L)
            } else {
                null
            },
            archiveReason = if (random.nextBoolean()) "" else word(random),
            needsReview = random.nextInt(0, 8)
        )
    }

    private fun network(random: Random, index: Int) = BackupNetwork(
        id = "n$index",
        name = NETWORK_NAMES.random(random),
        code = "NET$index",
        prefixes = PREFIXES.random(random),
        lengths = LENGTHS.random(random),
        cvv = if (random.nextBoolean()) 3 else 4,
        luhn = random.nextBoolean(),
        groups = GROUPS.random(random),
        cvvOptional = random.nextBoolean()
    )

    private fun settings(random: Random): Map<String, Any?>? {
        if (random.nextInt(3) == 0) return null
        val entries = SETTINGS.shuffled(random)
            .distinctBy { it.first }
            .take(random.nextInt(0, 4))
        return entries.associateTo(linkedMapOf()) { it }
    }

    private fun note(random: Random): String {
        if (random.nextInt(4) == 0) return ""
        val length = random.nextInt(1, CardValidator.NOTE_MAX_LENGTH + 1)
        return buildString {
            while (this.length < length) append(NOTE_UNITS.random(random))
        }
    }

    private fun name(random: Random): String =
        List(random.nextInt(1, 4)) { word(random) }.joinToString(" ")

    private fun word(random: Random): String =
        buildString {
            repeat(random.nextInt(1, 7)) { append(WORD_UNITS.random(random)) }
        }

    private fun digits(random: Random, length: Int): String =
        (1..length).map { '0' + random.nextInt(10) }.joinToString("")

    private fun <T> List<T>.pick(random: Random): T? =
        if (isEmpty() || random.nextBoolean()) null else this[random.nextInt(size)]

    private companion object {
        val WORD_UNITS = listOf(
            "a", "B", "n", "7", "é", "ß", "Ж", "अ", "漢", "カ", "Ω", "👍", "—", "’", "&", "-"
        )
        val NOTE_UNITS = listOf(
            "a", "Z", "3", " ", "é", "ß", "Ж", "अ", "漢", "カ", "Ω", "—", "’", "\"", "\\", "\n", "\t"
        )
        val GARBAGE_UNITS = listOf(
            "{", "}", "[", "]", ":", ",", "\"", "0", "1", "9", "a", "Z", " ", "\n", "\\", "/", "-", "_"
        )
        val NETWORK_NAMES = listOf(
            "Visa", "Mastercard", "American Express", "Maestro", "RuPay", "Local Debit", "UnionPay"
        )
        val PREFIXES = listOf("4", "5", "34,37", "6759", "6521", "88", "62")
        val LENGTHS = listOf("13,16,19", "16", "15", "12-19", "16,19")
        val GROUPS = listOf("", "4-6-5", "4-4-4-4", "6-13")
        val SETTINGS = listOf(
            "theme" to "light",
            "theme" to "dark",
            "maskNumbers" to true,
            "maskNumbers" to false,
            "notifyEnabled" to true,
            "notifyEnabled" to false
        )
    }
}
