package com.quickcards.domain.sheet

import com.quickcards.domain.BankCatalog
import com.quickcards.domain.CardNumberRules
import com.quickcards.domain.DefaultNetworks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth

class SheetCheckerTest {

    private val visa = "4111111111111111"
    private val amex = "378282246310005"
    private val mastercard = "5500000000000004"
    private val maestro = ("5018" + "1".repeat(11)).let { it + CardNumberRules.luhnCheckDigit(it) }

    private val existing = mutableMapOf<String, ExistingCardDetails>()
    private val context = SheetContext(
        networks = DefaultNetworks.all,
        banks = BankCatalog.all.map { it.name } + "My Co-op Bank",
        types = listOf("Credit", "Debit", "Prepaid", "Forex"),
        holders = listOf("Priya S"),
        existing = { existing[it] },
        today = YearMonth.of(2026, 11)
    )

    private val headers = ImportSchema.headers
    private val banner = listOf("First make your own copy: File > Make a copy.") + List(10) { "" }

    private fun row(
        holder: String = "Priya S", number: String = visa, month: String = "08 - August", year: String = "2029",
        cvv: String = "123", network: String = "Visa", bank: String = "HDFC Bank", type: String = "Credit",
        statement: String = "", due: String = "5", note: String = "Fuel card"
    ) = listOf(holder, number, month, year, cvv, network, bank, type, statement, due, note)

    private fun check(vararg rows: List<String>, header: List<String> = headers, withBanner: Boolean = true): SheetReport =
        SheetChecker.check(listOfNotNull(banner.takeIf { withBanner }, header) + rows, context)

    private fun problems(vararg rows: List<String>): List<RowProblem> = check(*rows).rows.single().problems

    @Test
    fun `a correct row is ready and keeps the phone's spelling of the holder`() {
        val report = check(row(holder = "priya s", bank = "hdfc", type = "credit", network = "VISA"))
        val result = report.rows.single()
        assertEquals(RowStatus.READY, result.status)
        assertEquals(3, result.row)
        assertEquals(
            ImportedCard("Priya S", visa, "08/29", "123", "visa", "HDFC Bank", "Credit", null, 5, "Fuel card"),
            result.card
        )
        assertTrue(report.newHolders.isEmpty())
    }

    @Test
    fun `headers may be in any order, without stars, with a banner above or not`() {
        val shuffled = listOf("Note", "Card type", "BANK", "network", "cvv", "expiry year", "Expiry Month", "card number", "Card Holder", "payment due day")
        val cells = listOf("", "Credit", "HDFC Bank", "Visa", "123", "29", "8", visa, "Priya S", "")
        val report = check(cells, header = shuffled, withBanner = false)
        assertEquals(RowStatus.READY, report.rows.single().status)
        assertEquals(2, report.rows.single().row)
        assertNull(report.rows.single().card?.dueDay)
    }

    @Test
    fun `a sheet from before the statement day column still imports`() {
        val oldHeaders = headers.filterNot { it == "Statement day" }
        val report = check(row().filterIndexed { i, _ -> i != 8 }, header = oldHeaders)
        assertEquals(RowStatus.READY, report.rows.single().status)
        assertNull(report.rows.single().card?.statementDay)
        assertEquals(5, report.rows.single().card?.dueDay)
    }

    @Test
    fun `the whole file is refused when columns are missing, unknown or there are no rows`() {
        assertEquals(
            SheetFileProblem.MissingColumns(listOf("CVV*", "Network*")),
            check(row(), header = headers.filterNot { it == "CVV*" || it == "Network*" }).fileProblem
        )
        assertEquals(
            SheetFileProblem.UnknownColumns(listOf("Name on card")),
            check(row() + "X", header = headers + "Name on card").fileProblem
        )
        assertEquals(SheetFileProblem.NoHeaderRow, SheetChecker.check(listOf(banner, row()), context).fileProblem)
        assertEquals(SheetFileProblem.NoRows, check(List(11) { "" }).fileProblem)
        assertEquals(
            SheetFileProblem.TooManyRows(ImportSchema.MAX_ROWS + 1, ImportSchema.MAX_ROWS),
            check(*Array(ImportSchema.MAX_ROWS + 1) { row() }).fileProblem
        )
    }

    @Test
    fun `every empty required cell is named`() {
        val problems = problems(row(holder = "", number = "", month = "", year = "", cvv = "", network = "", bank = "", type = "", due = "", note = "x"))
        assertEquals(
            ImportColumn.entries.filter { it.required }.map { RowProblem.Missing(it) },
            problems
        )
    }

    @Test
    fun `number rules`() {
        assertEquals(listOf(RowProblem.NumberChangedBySpreadsheet), problems(row(number = "4.11111E+15")))
        assertEquals(listOf(RowProblem.NumberNotDigits), problems(row(number = "4111 1111 1111 111x")))
        assertEquals(RowStatus.READY, check(row(number = "4111 1111 1111 1111")).rows.single().status)
        assertEquals(listOf(RowProblem.NumberLength("Visa", "13, 16 or 19")), problems(row(number = "41111111111111")))
        assertEquals(listOf(RowProblem.NumberLuhn), problems(row(number = "4111111111111112")))
        assertEquals(listOf(RowProblem.NumberUnknownNetwork), problems(row(number = "1234567890123")))
    }

    @Test
    fun `network must be in the list and must match the number`() {
        assertEquals(listOf(RowProblem.NetworkUnknown("Foo")), problems(row(network = "Foo")))
        assertEquals(listOf(RowProblem.NetworkMismatch("Visa", "RuPay")), problems(row(network = "RuPay")))
        assertEquals(RowStatus.READY, check(row(number = amex, cvv = "1234", network = "American Express (AMEX)")).rows.single().status)
        assertEquals(RowStatus.READY, check(row(number = amex, cvv = "1234", network = "amex")).rows.single().status)
        assertEquals(RowStatus.READY, check(row(number = mastercard, network = "Mastercard")).rows.single().status)
    }

    @Test
    fun `expiry rules`() {
        assertEquals(listOf(RowProblem.MonthInvalid), problems(row(month = "13")))
        assertEquals(listOf(RowProblem.YearInvalid), problems(row(year = "abc")))
        assertEquals(listOf(RowProblem.Expired), problems(row(month = "1", year = "2020")))
        assertEquals(listOf(RowProblem.ExpiryTooFar), problems(row(year = "2060")))
        assertEquals("08/29", check(row(month = "Aug", year = "29")).rows.single().card?.expiry)
    }

    @Test
    fun `cvv follows the picked network`() {
        assertEquals(listOf(RowProblem.CvvLength(3)), problems(row(cvv = "12")))
        assertEquals(listOf(RowProblem.CvvLength(4)), problems(row(number = amex, cvv = "123", network = "American Express")))
        assertEquals(listOf(RowProblem.CvvLength(3)), problems(row(cvv = "12a")))
        // The sheet requires a CVV even for Maestro; such cards are added in the app by hand.
        assertEquals(listOf(RowProblem.Missing(ImportColumn.CVV)), problems(row(number = maestro, cvv = "", network = "Maestro")))
    }

    @Test
    fun `bank, type, due day, note and holder rules`() {
        assertEquals("HDFC Bank", check(row(bank = "HDFC")).rows.single().card?.bankName)
        assertEquals("SBI Card", check(row(bank = "SBI Card (State Bank of India)")).rows.single().card?.bankName)
        assertEquals("My Co-op Bank", check(row(bank = "my co-op bank")).rows.single().card?.bankName)
        assertEquals(listOf(RowProblem.BankUnknown("Foo Bank")), problems(row(bank = "Foo Bank")))
        assertEquals(listOf(RowProblem.TypeUnknown("Gold")), problems(row(type = "Gold")))
        assertEquals(listOf(RowProblem.DueDayInvalid), problems(row(due = "32")))
        assertEquals(18, check(row(statement = "18")).rows.single().card?.statementDay)
        assertEquals(listOf(RowProblem.StatementDayInvalid), problems(row(statement = "0")))
        assertEquals(listOf(RowProblem.NoteTooLong(80)), problems(row(note = "n".repeat(81))))
        assertEquals(listOf(RowProblem.HolderTooLong(40)), problems(row(holder = "h".repeat(41))))
        assertEquals(listOf("Arjun"), check(row(holder = " Arjun  "), row(number = mastercard, network = "Mastercard", holder = "arjun")).newHolders)
    }

    @Test
    fun `duplicates in the file and on the phone`() {
        val report = check(row(), row(number = "4111 1111 1111 1111"))
        assertEquals(RowStatus.READY, report.rows[0].status)
        assertEquals(listOf(RowProblem.RepeatedInFile(3)), report.rows[1].problems)

        existing[visa] = ExistingCardDetails("08/29", "123")
        assertEquals(RowStatus.ALREADY_IN_APP, check(row()).rows.single().status)
        assertEquals(listOf(RowProblem.AlreadyInAppWithDifferentDetails), problems(row(cvv = "999")))
        // A broken row is reported as broken, not as a duplicate.
        assertEquals(listOf(RowProblem.NetworkMismatch("Visa", "RuPay")), problems(row(network = "RuPay")))
        existing.clear()
    }

    @Test
    fun `blank rows are skipped and row numbers follow the sheet`() {
        val report = check(List(11) { "" }, row(), List(11) { "" }, row(number = mastercard, network = "Mastercard"))
        assertEquals(listOf(4, 6), report.rows.map { it.row })
        assertEquals(2, report.ready.size)
    }
}
