package com.quickcards.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BankCatalogTest {

    @Test
    fun `the catalog has 26 banks with unique names and aliases`() {
        assertEquals(26, BankCatalog.all.size)
        val keys = BankCatalog.all.flatMap { listOf(it.name) + it.aliases }.map(BankCatalog::key)
        assertEquals(keys.toString(), keys.size, keys.toSet().size)
    }

    @Test
    fun `every name fits the bank name limit`() {
        BankCatalog.all.forEach { bank ->
            assertTrue(bank.name, bank.name.length in 2..EntityValidator.BANK_NAME_MAX)
        }
    }

    @Test
    fun `names match ignoring case, spaces, punctuation and bracketed hints`() {
        assertEquals("HDFC Bank", BankCatalog.find("  hdfc   bank ")?.name)
        assertEquals("HDFC Bank", BankCatalog.find("HDFC")?.name)
        assertEquals("SBI Card", BankCatalog.find("SBI Card (State Bank of India)")?.name)
        assertEquals("SBI Card", BankCatalog.find("State Bank of India (SBI)")?.name)
        assertEquals("American Express", BankCatalog.find("American Express (Amex)")?.name)
        assertEquals("American Express", BankCatalog.find("AMEX")?.name)
        assertEquals("Chase", BankCatalog.find("JPMorgan Chase")?.name)
        assertEquals("Punjab National Bank", BankCatalog.find("Punjab National Bank (PNB)")?.name)
    }

    @Test
    fun `unknown and empty names do not match`() {
        assertNull(BankCatalog.find("My Local Co-op"))
        assertNull(BankCatalog.find(""))
        assertNull(BankCatalog.find("   "))
        assertNull(BankCatalog.find("()"))
    }

    @Test
    fun `the old default bank names map onto the catalog`() {
        listOf(
            "HDFC Bank", "ICICI Bank", "Axis Bank", "Kotak Mahindra Bank", "IndusInd Bank",
            "State Bank of India (SBI)", "Punjab National Bank (PNB)", "Bank of Baroda",
            "AU Small Finance Bank", "Bandhan Bank", "Federal Bank", "IDFC FIRST Bank",
            "Citibank", "Standard Chartered", "HSBC"
        ).forEach { old -> assertTrue(old, BankCatalog.find(old) != null) }
    }
}
