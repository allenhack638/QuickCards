package com.quickcards.app.core.data.backup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.gson.Gson
import com.quickcards.app.core.data.database.QuickCardsDatabase
import com.quickcards.app.core.data.model.SeedData
import com.quickcards.app.core.data.model.VaultCard
import com.quickcards.app.core.data.repository.VaultRepository
import com.quickcards.app.core.data.settings.AppSettings
import com.quickcards.app.core.data.settings.AppTheme
import com.quickcards.app.core.security.EncryptionHelper
import com.quickcards.app.core.security.ExportCrypto
import com.quickcards.domain.BackupCard
import com.quickcards.domain.BackupContent
import com.quickcards.domain.BackupMeta
import com.quickcards.domain.BackupNamed
import com.quickcards.domain.BackupPlanner
import com.quickcards.domain.CardField
import com.quickcards.domain.MergeMode
import com.quickcards.domain.WrongPassphraseException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupFlowTest {

    private lateinit var database: QuickCardsDatabase
    private lateinit var repository: VaultRepository
    private lateinit var settings: AppSettings

    private val passphrase = "correct horse".toCharArray()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, QuickCardsDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = VaultRepository(database)
        settings = AppSettings.get(context)
        settings.applyMap(BackupSettings.defaultMap())
        runBlocking {
            database.networkDao().insertNetworks(SeedData.networks)
            database.cardTypeDao().insertCardTypes(SeedData.cardTypes)
        }
    }

    @After
    fun tearDown() {
        settings.applyMap(BackupSettings.defaultMap())
        database.close()
    }

    @Test
    fun exportEraseImportRestoresEverything() = runBlocking {
        val holders = listOf("Aarav Mehta", "Priya Mehta").map { repository.createHolder(it) }
        val hdfc = repository.createBank("HDFC Bank")
        val axis = repository.createBank("Axis Bank")
        val credit = repository.createCardType("Credit")
        val travel = repository.createTag("Travel")
        val online = repository.createTag("Online")

        repository.insertCard(
            VaultCard(
                number = "4532778103492219",
                expiry = "12/29",
                cvv = "381",
                holderId = holders[0].id,
                bankId = hdfc.id,
                networkAuto = true,
                typeId = credit.id,
                nameOnCard = "AARAV MEHTA",
                cardName = "Regalia",
                note = "Everyday spending",
                bandColor = "navy",
                tagIds = listOf(travel.id, online.id),
                pinned = true,
                pinnedAt = 1_700_000_000_000L
            )
        ).getOrThrow()
        repository.insertCard(
            VaultCard(
                number = "378282246310005",
                expiry = "09/28",
                cvv = "4821",
                holderId = holders[1].id,
                bankId = axis.id,
                networkAuto = true,
                typeId = credit.id,
                nameOnCard = "PRIYA MEHTA",
                cardName = "Platinum",
                note = "",
                bandColor = "plum",
                tagIds = listOf(travel.id)
            )
        ).getOrThrow()

        settings.setTheme(AppTheme.LIGHT)
        settings.setCopyWindowSec(120)
        settings.setPinnedMax(5)

        val content = BackupSnapshot.capture(
            repository = repository,
            settings = settings,
            includeSettings = true,
            version = "2.0",
            exportedAt = "2026-10-23"
        )
        val file = ExportCrypto.encrypt(BackupCodec.encode(content), passphrase)
        val decoded = BackupCodec.decode(file, passphrase)

        val plan = BackupPlanner.plan(decoded.content, BackupSnapshot.phoneSnapshot(repository))
        assertEquals(2, plan.sameCards)
        assertEquals(0, plan.newCards)
        assertEquals(0, plan.changedCards)

        BackupImporter.eraseAll(database)

        assertTrue(repository.cards.first().isEmpty())
        assertTrue(repository.holders.first().isEmpty())
        assertTrue(repository.banks.first().isEmpty())
        assertTrue(repository.cardTypes.first().isEmpty())
        assertTrue(repository.tags.first().isEmpty())
        assertEquals(SeedData.networks.size, database.networkDao().getAllNetworksSync().size)

        settings.setTheme(AppTheme.DARK)
        settings.setCopyWindowSec(30)

        val result = BackupImporter.import(
            database = database,
            content = decoded.content,
            mode = MergeMode.UPDATE,
            settings = settings,
            restoreSettings = true
        )

        assertEquals(2, result.added)
        assertEquals(0, result.updated)
        assertEquals(0, result.kept)
        assertEquals(0, result.same)
        assertTrue(result.settingsRestored)
        assertEquals(AppTheme.LIGHT, settings.theme.value)
        // Protection never loosens on restore: the phone's 30 s beats the backup's 120 s.
        assertEquals(30, settings.copyWindowSec.value)
        assertEquals(5, settings.pinnedMax.value)

        val restored = repository.cards.first()
        assertEquals(2, restored.size)
        val regalia = restored.first { it.cardName == "Regalia" }
        assertEquals("4532778103492219", regalia.number)
        assertEquals("12/29", regalia.expiry)
        assertEquals("381", regalia.cvv)
        assertEquals("AARAV MEHTA", regalia.nameOnCard)
        assertEquals("Everyday spending", regalia.note)
        assertEquals("navy", regalia.bandColor)
        assertTrue(regalia.networkAuto)
        assertEquals(2, regalia.tagIds.size)
        assertTrue(regalia.pinned)
        assertEquals(1_700_000_000_000L, regalia.pinnedAt)

        val restoredHolders = repository.holders.first().map { it.name }
        assertEquals(listOf("Aarav Mehta", "Priya Mehta"), restoredHolders)
        val restoredBanks = repository.banks.first().map { it.name }
        assertEquals(listOf("Axis Bank", "HDFC Bank"), restoredBanks)
        assertEquals(
            listOf("Credit", "Debit", "Forex", "Prepaid"),
            repository.cardTypes.first().map { it.name }
        )
        assertEquals(listOf("Online", "Travel"), repository.tags.first().map { it.name })
    }

    @Test
    fun updateOrKeepIsHonouredForChangedCards() = runBlocking {
        val holder = repository.createHolder("Aarav Mehta")
        val bank = repository.createBank("HDFC Bank")
        val type = repository.createCardType("Credit")
        repository.insertCard(
            VaultCard(
                number = "4532778103492219",
                expiry = "12/29",
                cvv = "381",
                holderId = holder.id,
                bankId = bank.id,
                networkAuto = true,
                typeId = type.id,
                nameOnCard = "AARAV MEHTA",
                cardName = "Regalia",
                note = "Everyday spending",
                bandColor = "navy"
            )
        ).getOrThrow()

        val snapshot = BackupSnapshot.capture(
            repository = repository,
            settings = settings,
            includeSettings = false,
            version = "2.0",
            exportedAt = "2026-10-23"
        )
        val changed = snapshot.copy(
            cards = snapshot.cards.map {
                it.copy(exp = "01/30", note = "From the backup", color = "rust")
            }
        )

        val plan = BackupPlanner.plan(changed, BackupSnapshot.phoneSnapshot(repository))
        assertEquals(1, plan.changedCards)

        val kept = BackupImporter.import(
            database = database,
            content = changed,
            mode = MergeMode.KEEP
        )
        assertEquals(1, kept.kept)
        val untouched = repository.cards.first().single()
        assertEquals("12/29", untouched.expiry)
        assertEquals("Everyday spending", untouched.note)
        assertEquals("navy", untouched.bandColor)

        val updated = BackupImporter.import(
            database = database,
            content = changed,
            mode = MergeMode.UPDATE
        )
        assertEquals(1, updated.updated)
        val replaced = repository.cards.first().single()
        assertEquals("01/30", replaced.expiry)
        assertEquals("From the backup", replaced.note)
        assertEquals("rust", replaced.bandColor)
    }

    @Test
    fun format1LegacyFileImportsWithBackfillMapping() = runBlocking {
        val json = Gson().toJson(
            listOf(
                mapOf(
                    "id" to "legacy-id",
                    "cardNumber" to "524199001137448",
                    "expiryDate" to "11/30",
                    "cvv" to "645",
                    "bankName" to "Axis Bank",
                    "cardType" to "Credit",
                    "cardIssuer" to "Visa",
                    "cardVariant" to "Flipkart",
                    "owner" to "Priya Mehta",
                    "description" to "Online",
                    "cardColor" to "#4C1D95"
                )
            )
        )
        val decoded = BackupCodec.decodePayload(json)
        assertEquals(1, decoded.format)

        val result = BackupImporter.import(
            database = database,
            content = decoded.content,
            mode = MergeMode.UPDATE
        )

        assertEquals(1, result.added)
        val card = repository.cards.first().single()
        assertEquals("524199001137448", card.number)
        assertEquals("11/30", card.expiry)
        assertEquals("645", card.cvv)
        assertEquals("PRIYA MEHTA", card.nameOnCard)
        assertEquals("Flipkart", card.cardName)
        assertEquals("Online", card.note)
        assertEquals("#4C1D95", card.bandColor)
        assertEquals(false, card.networkAuto)
        val visa = database.networkDao().findNetworkByName("Visa")
        assertNotNull(visa)
        assertEquals(visa!!.id, card.networkId)

        val holder = repository.holders.first().single()
        assertEquals("Priya Mehta", holder.name)
        assertEquals(holder.id, card.holderId)
        assertNotNull(card.bankId)
        assertNotNull(card.typeId)
        assertEquals("Axis Bank", repository.banks.first().single().name)
        assertEquals(
            "Credit",
            repository.cardTypes.first().first { it.id == card.typeId!! }.name
        )
    }

    @Test
    fun format1MalformedValuesAreRepairedAndRecorded() = runBlocking {
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

        val result = BackupImporter.import(
            database = database,
            content = decoded.content,
            mode = MergeMode.UPDATE,
            now = 1_700_000_000_000L
        )

        assertEquals(1, result.formatsFixed)
        assertEquals(0, result.needCheck)

        val card = repository.cards.first().single()
        assertEquals("4111111111111111", card.number)
        assertEquals("11/22", card.expiry)
        assertEquals("123", card.cvv)
        assertTrue(card.needsReview.isEmpty())

        val repairs = database.cardRepairDao()
            .getRepairsForCard(card.id, 0)
        assertEquals(2, repairs.size)
        val encryption = EncryptionHelper.getInstance()
        val expiry = repairs.first { it.field == CardField.EXPIRY.name }
        assertEquals("11//22", encryption.decrypt(expiry.before))
        assertEquals("11/22", encryption.decrypt(expiry.after))
        val cvv = repairs.first { it.field == CardField.CVV.name }
        assertEquals(" 12 3", encryption.decrypt(cvv.before))
        assertEquals("123", encryption.decrypt(cvv.after))
    }

    @Test
    fun wrongPassphraseLeavesTheVaultUntouched() = runBlocking {
        val holder = repository.createHolder("Aarav Mehta")
        repository.insertCard(
            VaultCard(
                number = "4532778103492219",
                expiry = "12/29",
                cvv = "381",
                holderId = holder.id,
                networkAuto = true,
                nameOnCard = "AARAV MEHTA",
                cardName = "Regalia",
                bandColor = "navy"
            )
        ).getOrThrow()

        val content = BackupSnapshot.capture(
            repository = repository,
            settings = settings,
            includeSettings = true,
            version = "2.0",
            exportedAt = "2026-10-23"
        )
        val file = ExportCrypto.encrypt(BackupCodec.encode(content), passphrase)

        var failed = false
        try {
            BackupCodec.decode(file, "not the passphrase".toCharArray())
        } catch (_: WrongPassphraseException) {
            failed = true
        }

        assertTrue(failed)
        assertEquals(1, repository.cards.first().size)
    }

    @Test
    fun settingsRestoreKeepsDefaultsForMissingKeys() = runBlocking {
        settings.setTheme(AppTheme.LIGHT)
        settings.setCopyWindowSec(120)
        settings.setMaskNumbers(false)
        settings.setTagsMax(10)

        val content = BackupSnapshot.capture(
            repository = repository,
            settings = settings,
            includeSettings = true,
            version = "2.0",
            exportedAt = "2026-10-23"
        )
        val partial = content.copy(settings = mapOf("theme" to "light"))

        settings.setTheme(AppTheme.DARK)
        settings.setCopyWindowSec(30)
        settings.setMaskNumbers(true)
        settings.setTagsMax(1)

        BackupImporter.import(
            database = database,
            content = partial,
            mode = MergeMode.UPDATE,
            settings = settings,
            restoreSettings = true
        )

        assertEquals(AppTheme.LIGHT, settings.theme.value)
        // Missing keys use defaults, but protection never loosens: 30 s beats the 60 s default.
        assertEquals(30, settings.copyWindowSec.value)
        assertEquals(true, settings.maskNumbers.value)
        assertEquals(3, settings.tagsMax.value)
    }

    @Test
    fun importAppendsNewCardsAndKeepsUpdatedCardPlace() = runBlocking {
        val holder = repository.createHolder("Aarav Mehta")
        val first = repository.insertCard(
            VaultCard(
                number = "4532778103492219",
                expiry = "12/29",
                cvv = "381",
                holderId = holder.id,
                networkAuto = true,
                nameOnCard = "AARAV MEHTA",
                cardName = "Regalia",
                bandColor = "navy"
            )
        ).getOrThrow()

        val content = BackupContent(
            meta = BackupMeta(version = "2.0", exportedAt = "2026-10-23"),
            holders = listOf(BackupNamed(holder.id, holder.name)),
            cards = listOf(
                BackupCard(
                    id = first.id,
                    number = first.number,
                    exp = "01/30",
                    cvv = first.cvv,
                    nameOnCard = first.nameOnCard,
                    holderId = holder.id,
                    networkAuto = true,
                    variant = first.cardName,
                    color = "navy",
                    sortOrder = 0
                ),
                BackupCard(
                    id = "new",
                    number = "378282246310005",
                    exp = "09/28",
                    cvv = "4821",
                    nameOnCard = "AARAV MEHTA",
                    holderId = holder.id,
                    networkAuto = true,
                    variant = "Platinum",
                    color = "plum",
                    sortOrder = 99
                )
            )
        )

        val result = BackupImporter.import(database, content, MergeMode.UPDATE)

        assertEquals(1, result.added)
        assertEquals(1, result.updated)
        val cards = repository.cards.first()
        assertEquals(2, cards.size)
        assertEquals(first.id, cards.first().id)
        assertEquals("01/30", cards.first().expiry)
        assertEquals("Platinum", cards.last().cardName)
    }
}
