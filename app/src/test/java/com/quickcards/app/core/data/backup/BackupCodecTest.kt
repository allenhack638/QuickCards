package com.quickcards.app.core.data.backup

import com.google.gson.Gson
import com.quickcards.app.core.security.ExportCrypto
import com.quickcards.domain.BACKUP_FORMAT
import com.quickcards.domain.BackupBank
import com.quickcards.domain.BackupCard
import com.quickcards.domain.BackupContent
import com.quickcards.domain.BackupFormatException
import com.quickcards.domain.BackupMeta
import com.quickcards.domain.BackupNamed
import com.quickcards.domain.BackupNetwork
import com.quickcards.domain.BackupSection
import com.quickcards.domain.DefaultNetworks
import com.quickcards.domain.WrongPassphraseException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCodecTest {

    private val passphrase = "correct horse".toCharArray()

    private fun sampleContent(settings: Map<String, Any?>? = null): BackupContent = BackupContent(
        meta = BackupMeta(version = "2.0", exportedAt = "2026-10-23"),
        holders = listOf(
            BackupNamed("h1", "Aarav Mehta"),
            BackupNamed("h2", "Priya Mehta")
        ),
        banks = listOf(
            BackupBank("b1", "HDFC Bank", "HD"),
            BackupBank("b2", "American Express", "AE")
        ),
        types = listOf(BackupNamed("t1", "Credit"), BackupNamed("t2", "Debit")),
        tags = listOf(BackupNamed("g1", "Travel"), BackupNamed("g2", "Online")),
        networks = listOf(
            BackupNetwork(
                id = "visa",
                name = "Visa",
                code = "VISA",
                prefixes = "4",
                lengths = "13,16,19",
                cvv = 3,
                luhn = true,
                groups = ""
            ),
            BackupNetwork(
                id = "amex",
                name = "American Express",
                code = "AMEX",
                prefixes = "34,37",
                lengths = "15",
                cvv = 4,
                luhn = true,
                groups = "4-6-5"
            )
        ),
        cards = listOf(
            BackupCard(
                id = "c1",
                number = "4532778103492219",
                exp = "12/29",
                cvv = "381",
                nameOnCard = "AARAV MEHTA",
                holderId = "h1",
                bankId = "b1",
                networkAuto = true,
                networkId = null,
                typeId = "t1",
                variant = "Regalia",
                note = "Everyday spending",
                color = "navy",
                tagIds = listOf("g1", "g2"),
                sortOrder = 0,
                pinned = true,
                pinnedAt = 1_700_000_000_000L
            ),
            BackupCard(
                id = "c2",
                number = "378282246310005",
                exp = "09/28",
                cvv = "4821",
                nameOnCard = "PRIYA MEHTA",
                holderId = "h2",
                bankId = "b2",
                networkAuto = false,
                networkId = "amex",
                typeId = "t2",
                variant = "Platinum",
                note = "",
                color = "plum",
                tagIds = emptyList(),
                sortOrder = 1
            )
        ),
        settings = settings
    )

    @Test
    fun `format 2 round trip preserves every field`() {
        val content = sampleContent(
            settings = mapOf(
                "theme" to "light",
                "dynamicColor" to false,
                "copyWindowSec" to 120,
                "maskNumbers" to true
            )
        )

        val decoded = BackupCodec.decodePayload(BackupCodec.encode(content))

        assertEquals(BACKUP_FORMAT, decoded.format)
        assertEquals(BackupSection.entries.toSet(), decoded.sections)
        assertEquals(content.meta, decoded.content.meta)
        assertEquals(content.holders, decoded.content.holders)
        assertEquals(content.banks, decoded.content.banks)
        assertEquals(content.types, decoded.content.types)
        assertEquals(content.tags, decoded.content.tags)
        assertEquals(content.networks, decoded.content.networks)
        assertEquals(content.cards, decoded.content.cards)

        val settings = decoded.content.settings
        requireNotNull(settings)
        assertEquals("light", settings["theme"])
        assertEquals(false, settings["dynamicColor"])
        assertEquals(120, (settings["copyWindowSec"] as Number).toInt())
        assertEquals(true, settings["maskNumbers"])
    }

    @Test
    fun `format 2 round trip keeps the payment due day`() {
        val base = sampleContent()
        val withDue = base.copy(cards = base.cards.mapIndexed { i, card -> if (i == 0) card.copy(dueDay = 27) else card })
        val decoded = BackupCodec.decodePayload(BackupCodec.encode(withDue))

        assertEquals(27, decoded.content.cards[0].dueDay)
        assertNull(decoded.content.cards[1].dueDay)
    }

    @Test
    fun `format 2 without settings decodes cfg as absent`() {
        val decoded = BackupCodec.decodePayload(BackupCodec.encode(sampleContent()))

        assertNull(decoded.content.settings)
        assertEquals(2, decoded.content.cards.size)
    }

    @Test
    fun `format 2 round trip keeps pinned cards and their order`() {
        val decoded = BackupCodec.decodePayload(BackupCodec.encode(sampleContent()))

        val pinned = decoded.content.cards.first()
        assertTrue(pinned.pinned)
        assertEquals(1_700_000_000_000L, pinned.pinnedAt)

        val unpinned = decoded.content.cards.last()
        assertFalse(unpinned.pinned)
        assertNull(unpinned.pinnedAt)
    }

    @Test
    fun `format 2 backups from before pinning import as unpinned`() {
        val json = """
            {"meta":{"app":"QuickCards","format":2,"version":"2.0","exportedAt":"2026-10-20"},
             "cards":[{"number":"4111111111111111","exp":"12/29","cvv":"123"}]}
        """.trimIndent()

        val card = BackupCodec.decodePayload(json).content.cards.single()

        assertFalse(card.pinned)
        assertNull(card.pinnedAt)
    }

    @Test
    fun `format 2 survives the encrypted envelope`() {
        val json = BackupCodec.encode(sampleContent())
        val envelope = ExportCrypto.encrypt(json, passphrase)

        assertTrue(BackupCodec.isEncrypted(envelope))

        val decoded = BackupCodec.decode(envelope, passphrase)
        assertEquals(2, decoded.content.cards.size)
        assertEquals("Aarav Mehta", decoded.content.holders.first().name)
        assertEquals("378282246310005", decoded.content.cards.last().number)
    }

    @Test
    fun `wrong passphrase is reported without returning data`() {
        val envelope = ExportCrypto.encrypt(BackupCodec.encode(sampleContent()), passphrase)

        assertThrows(WrongPassphraseException::class.java) {
            BackupCodec.decode(envelope, "wrong passphrase".toCharArray())
        }
        assertThrows(WrongPassphraseException::class.java) {
            BackupCodec.decode(envelope, null)
        }
    }

    @Test
    fun `format 1 maps cards and banks like the legacy backfill`() {
        val json = Gson().toJson(
            listOf(
                mapOf(
                    "id" to "old-1",
                    "cardNumber" to "4532 7781 0349 2219",
                    "expiryDate" to "12/29",
                    "cvv" to "381",
                    "bankName" to "HDFC Bank",
                    "cardType" to "Credit",
                    "cardIssuer" to "Visa",
                    "cardVariant" to "Regalia",
                    "owner" to "me",
                    "description" to "Everyday spending",
                    "cardColor" to "#1E3A8A",
                    "panHash" to null
                ),
                mapOf(
                    "id" to "old-2",
                    "cardNumber" to "378282246310005",
                    "expiryDate" to "09/28",
                    "cvv" to "4821",
                    "bankName" to "American Express",
                    "cardType" to "Credit",
                    "cardIssuer" to "Visa",
                    "cardVariant" to "Platinum",
                    "owner" to " Priya   Mehta ",
                    "description" to "",
                    "cardColor" to "#4C1D95"
                )
            )
        )

        val decoded = BackupCodec.decodePayload(json)

        assertEquals(1, decoded.format)
        assertEquals(setOf(BackupSection.BANKS), decoded.sections)
        assertNull(decoded.content.meta)
        assertNull(decoded.content.settings)

        assertEquals(listOf("Me", "Priya Mehta"), decoded.content.holders.map { it.name })
        assertEquals(listOf("HDFC Bank", "American Express"), decoded.content.banks.map { it.name })
        assertEquals(listOf("Credit"), decoded.content.types.map { it.name })
        assertEquals(2, decoded.content.cards.size)

        val me = decoded.content.cards[0]
        assertEquals("4532778103492219", me.number)
        assertEquals("12/29", me.exp)
        assertEquals("381", me.cvv)
        assertEquals("", me.nameOnCard)
        assertEquals("Regalia", me.variant)
        assertEquals("Everyday spending", me.note)
        assertEquals("#1E3A8A", me.color)
        assertTrue(me.networkAuto)
        assertNull(me.networkId)
        assertEquals("Visa", me.legacyIssuer)
        assertEquals(decoded.content.holders[0].id, me.holderId)
        assertEquals(decoded.content.banks[0].id, me.bankId)
        assertEquals(decoded.content.types[0].id, me.typeId)
        assertEquals(emptyList<String>(), me.tagIds)

        val priya = decoded.content.cards[1]
        assertEquals("PRIYA MEHTA", priya.nameOnCard)
        assertEquals("", priya.note)
        assertEquals("#4C1D95", priya.color)
        assertEquals("378282246310005", priya.number)
        assertEquals(decoded.content.holders[1].id, priya.holderId)
        assertEquals(decoded.content.banks[1].id, priya.bankId)
    }

    @Test
    fun `format 1 imports from an encrypted envelope`() {
        val json = Gson().toJson(
            listOf(
                mapOf(
                    "cardNumber" to "431581026674093",
                    "expiryDate" to "08/28",
                    "cvv" to "902",
                    "bankName" to "ICICI Bank",
                    "cardType" to "Credit",
                    "cardIssuer" to "",
                    "cardVariant" to "Amazon Pay",
                    "owner" to "Aarav Mehta",
                    "description" to "",
                    "cardColor" to "#7C2D12"
                )
            )
        )
        val envelope = ExportCrypto.encrypt(json, passphrase)

        val decoded = BackupCodec.decode(envelope, passphrase)

        assertEquals(1, decoded.format)
        assertEquals(1, decoded.content.cards.size)
        assertEquals("#7C2D12", decoded.content.cards.single().color)
        assertEquals("AARAV MEHTA", decoded.content.cards.single().nameOnCard)
        assertEquals("Aarav Mehta", decoded.content.holders.single().name)
    }

    @Test
    fun `plaintext format 1 is accepted without a passphrase`() {
        val json = Gson().toJson(
            listOf(
                mapOf(
                    "cardNumber" to "524199001137448",
                    "expiryDate" to "11/30",
                    "cvv" to "645",
                    "bankName" to "Axis Bank",
                    "cardType" to "Credit",
                    "cardIssuer" to "Mastercard",
                    "cardVariant" to "Flipkart",
                    "owner" to "me",
                    "cardColor" to "#7C2D12"
                )
            )
        )

        val decoded = BackupCodec.decode(json, null)
        assertEquals(1, decoded.format)
        assertEquals("#7C2D12", decoded.content.cards.single().color)
    }

    @Test
    fun `format 2 round trip keeps archive state and the review mask`() {
        val content = sampleContent().copy(
            cards = listOf(
                sampleContent().cards.first().copy(
                    archived = true,
                    archivedAt = 1_700_000_000_000L,
                    archiveReason = "Card closed",
                    needsReview = 5
                )
            )
        )

        val card = BackupCodec.decodePayload(BackupCodec.encode(content)).content.cards.single()

        assertTrue(card.archived)
        assertEquals(1_700_000_000_000L, card.archivedAt)
        assertEquals("Card closed", card.archiveReason)
        assertEquals(5, card.needsReview)
    }

    @Test
    fun `format 2 cards from before archiving import with defaults`() {
        val json = """
            {"meta":{"app":"QuickCards","format":2,"version":"2.0","exportedAt":"2026-10-20"},
             "cards":[{"number":"4111111111111111","exp":"12/29","cvv":"123"}]}
        """.trimIndent()

        val card = BackupCodec.decodePayload(json).content.cards.single()

        assertFalse(card.archived)
        assertNull(card.archivedAt)
        assertEquals("", card.archiveReason)
        assertEquals(0, card.needsReview)
    }

    @Test
    fun `format 1 malformed expiry is repaired before the review`() {
        val json = Gson().toJson(
            listOf(
                mapOf(
                    "cardNumber" to "4111 1111 1111 1111",
                    "expiryDate" to "11//22",
                    "cvv" to " 12 3",
                    "bankName" to "HDFC Bank",
                    "cardType" to "Credit",
                    "cardIssuer" to "Visa",
                    "cardVariant" to "Regalia",
                    "owner" to "me",
                    "description" to "",
                    "cardColor" to "#1E3A8A"
                )
            )
        )
        val decoded = BackupCodec.decodePayload(json)
        val normalized = BackupNormalizer.normalize(decoded.content, DefaultNetworks.all)

        assertEquals(1, normalized.formatsFixed)
        assertEquals(0, normalized.needCheck)
        val card = normalized.content.cards.single()
        assertEquals("4111111111111111", card.number)
        assertEquals("11/22", card.exp)
        assertEquals("123", card.cvv)
        assertEquals(0, card.needsReview)
        assertEquals("11//22", decoded.content.cards.single().exp)
    }

    @Test
    fun `format 2 round trip keeps a custom hex colour`() {
        val content = sampleContent().copy(
            cards = listOf(
                sampleContent().cards.first().copy(color = "#0F5E5A")
            )
        )

        val card = BackupCodec.decodePayload(BackupCodec.encode(content)).content.cards.single()

        assertEquals("#0F5E5A", card.color)
    }

    @Test
    fun `format 2 keeps curated keys and unknown values fall back to navy`() {
        val json = """
            {"meta":{"app":"QuickCards","format":2,"version":"2.0","exportedAt":"2026-10-23"},
             "cards":[
               {"number":"4111111111111111","exp":"12/29","cvv":"123","color":"teal"},
               {"number":"4111111111111112","exp":"12/29","cvv":"123","color":"ochre"}
             ]}
        """.trimIndent()

        val cards = BackupCodec.decodePayload(json).content.cards

        assertEquals("teal", cards[0].color)
        assertEquals("navy", cards[1].color)
    }

    @Test
    fun `format 1 keeps a readable custom hex and snaps a light one`() {
        val json = Gson().toJson(
            listOf(
                mapOf(
                    "cardNumber" to "4111111111111111",
                    "expiryDate" to "12/29",
                    "cvv" to "123",
                    "bankName" to "HDFC Bank",
                    "cardType" to "Credit",
                    "cardIssuer" to "Visa",
                    "cardVariant" to "Regalia",
                    "owner" to "me",
                    "description" to "",
                    "cardColor" to "#123456"
                ),
                mapOf(
                    "cardNumber" to "4111111111111112",
                    "expiryDate" to "12/29",
                    "cvv" to "123",
                    "bankName" to "HDFC Bank",
                    "cardType" to "Credit",
                    "cardIssuer" to "Visa",
                    "cardVariant" to "Regalia",
                    "owner" to "me",
                    "description" to "",
                    "cardColor" to "#FFFF00"
                )
            )
        )

        val cards = BackupCodec.decodePayload(json).content.cards

        assertEquals("#123456", cards[0].color)
        assertEquals("burnt-orange", cards[1].color)
    }

    @Test
    fun `foreign and future files are rejected`() {
        assertThrows(BackupFormatException::class.java) {
            BackupCodec.decodePayload(
                """{"meta":{"app":"Other","format":2},"cards":[]}"""
            )
        }
        assertThrows(BackupFormatException::class.java) {
            BackupCodec.decodePayload(
                """{"meta":{"app":"QuickCards","format":3},"cards":[]}"""
            )
        }
        assertThrows(BackupFormatException::class.java) {
            BackupCodec.decodePayload("not json")
        }
    }

    @Test
    fun `format 2 cards without numbers are skipped`() {
        val json = """
            {"meta":{"app":"QuickCards","format":2,"version":"2.0","exportedAt":"2026-10-23"},
             "cards":[{"number":""},{"number":"4111 1111 1111 1111"}]}
        """.trimIndent()

        val decoded = BackupCodec.decodePayload(json)

        assertEquals(1, decoded.content.cards.size)
        assertEquals("4111111111111111", decoded.content.cards.single().number)
        assertTrue(decoded.content.cards.single().networkAuto)
        assertFalse(decoded.sections.isEmpty())
    }
}
