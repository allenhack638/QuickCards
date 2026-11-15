package com.quickcards.app.core.data.sheet

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.quickcards.app.core.data.database.QuickCardsDatabase
import com.quickcards.app.core.data.model.Bank
import com.quickcards.app.core.data.model.SeedData
import com.quickcards.app.core.data.repository.VaultRepository
import com.quickcards.domain.sheet.RowProblem
import com.quickcards.domain.sheet.RowStatus
import com.quickcards.domain.sheet.SheetFileProblem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.YearMonth

/** End to end at the data level: file bytes -> report -> cards in the vault, on the API 26 target. */
@RunWith(AndroidJUnit4::class)
class SheetImporterTest {

    private lateinit var database: QuickCardsDatabase
    private lateinit var repository: VaultRepository
    private val today = YearMonth.of(2026, 11)

    private fun asset(name: String): ByteArray =
        InstrumentationRegistry.getInstrumentation().context.assets.open("sheet/$name").use { it.readBytes() }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, QuickCardsDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = VaultRepository(database)
        runBlocking {
            database.bankDao().insertBanks(Bank.getDefaultBanks())
            database.networkDao().insertNetworks(SeedData.networks)
            database.cardTypeDao().insertCardTypes(SeedData.cardTypes)
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun theGeneratedSheetImportsItsGoodRowsAndReportsTheRest() = runBlocking {
        val opened = SheetImporter.open(asset("generated-rows.xlsx")) as SheetOpenResult.Opened
        assertEquals(SheetFileKind.XLSX, opened.kind)

        val report = SheetImporter.check(database, opened.grid, today = today)
        assertNull(report.fileProblem)
        assertEquals(listOf(3, 4, 5), report.ready.map { it.row })
        assertEquals(listOf(6, 7, 8), report.needsFixing.map { it.row })
        assertEquals(listOf(RowProblem.NumberLuhn), report.needsFixing[0].problems)
        assertEquals(listOf("Priya S", "Arjun"), report.newHolders)

        val result = SheetImporter.import(database, report)
        assertEquals(3, result.added)

        val cards = repository.cards.first().sortedBy { it.sortOrder }
        assertEquals(3, cards.size)
        val holders = repository.holders.first().map { it.name }
        assertTrue(holders.toString(), holders.containsAll(listOf("Priya S", "Arjun")))
        val amex = cards.first { it.number == "378282246310005" }
        assertEquals("01/30", amex.expiry)
        assertEquals("1234", amex.cvv)
        assertEquals("American Express", database.bankDao().getBankById(amex.bankId!!)?.name)
        assertEquals("Credit", database.cardTypeDao().getCardTypeById(amex.typeId!!)?.name)
        val sbi = cards.first { it.number == "5500000000000004" }
        assertEquals("SBI Card", database.bankDao().getBankById(sbi.bankId!!)?.name)
        assertEquals(15, sbi.dueDay)
        assertEquals("Salary account", sbi.note)

        // Importing the same file again: nothing is added, every good row is already in the app.
        val again = SheetImporter.check(database, opened.grid, today = today)
        assertEquals(0, again.ready.size)
        assertEquals(listOf(RowStatus.ALREADY_IN_APP, RowStatus.ALREADY_IN_APP, RowStatus.ALREADY_IN_APP), again.alreadyInApp.map { it.status })
        assertEquals(0, SheetImporter.import(database, again).added)
        assertEquals(3, repository.cards.first().size)
    }

    @Test
    fun aCsvWithAnotherSeparatorImportsToo() = runBlocking {
        val opened = SheetImporter.open(asset("filled-semicolon.csv")) as SheetOpenResult.Opened
        assertEquals(SheetFileKind.CSV, opened.kind)
        val report = SheetImporter.check(database, opened.grid, today = today)
        assertEquals(3, report.ready.size)
        assertEquals(3, SheetImporter.import(database, report).added)
    }

    @Test
    fun otherFilesAreRefusedByContent() {
        assertEquals(SheetOpenResult.Refused(SheetOpenProblem.NotASheet), SheetImporter.open(byteArrayOf(0x25, 0x50, 0x44, 0x46, 0, 1)))
        assertEquals(SheetOpenResult.Refused(SheetOpenProblem.Unreadable), SheetImporter.open("PK\u0003\u0004 not really a zip".toByteArray()))
        val report = runBlocking { SheetImporter.check(database, listOf(listOf("Name", "Number")), today = today) }
        assertEquals(SheetFileProblem.NoHeaderRow, report.fileProblem)
    }
}
