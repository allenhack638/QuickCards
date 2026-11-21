package com.quickcards.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BankColoursTest {

    @Test
    fun `every built-in bank has its documented colour`() {
        val expected = mapOf(
            "HDFC Bank" to "navy", "SBI Card" to "ocean", "ICICI Bank" to "rust",
            "Axis Bank" to "wine", "Kotak Mahindra Bank" to "crimson", "IndusInd Bank" to "plum",
            "RBL Bank" to "violet", "IDFC FIRST Bank" to "magenta", "Bank of Baroda" to "rust",
            "Yes Bank" to "indigo", "AU Small Finance Bank" to "burnt-orange",
            "Federal Bank" to "olive", "Bandhan Bank" to "moss", "Punjab National Bank" to "forest",
            "Canara Bank" to "teal", "Union Bank of India" to "slate",
            "American Express" to "ocean", "HSBC" to "crimson", "Standard Chartered" to "teal",
            "Citibank" to "navy", "Chase" to "indigo", "Capital One" to "slate",
            "Bank of America" to "wine", "Barclays" to "ocean", "Wells Fargo" to "burnt-orange",
            "DBS Bank" to "crimson"
        )
        assertEquals(26, expected.size)
        expected.forEach { (bank, colour) -> assertEquals(bank, colour, BankColours.defaultFor(bank)) }
    }

    @Test
    fun `the whole catalog is covered and graphite is never a default`() {
        BankCatalog.all.forEach { bank ->
            val colour = BankColours.defaultFor(bank.name)
            assertEquals(bank.name, true, colour in ArtRules.PALETTE_ORDER)
        }
    }

    @Test
    fun `aliases and typed variants resolve to the same bank colour`() {
        assertEquals("navy", BankColours.defaultFor("hdfc"))
        assertEquals("ocean", BankColours.defaultFor("State Bank of India"))
        assertEquals("ocean", BankColours.defaultFor("amex"))
        assertEquals("navy", BankColours.defaultFor("Citi"))
        assertEquals("rust", BankColours.defaultFor("  icici   BANK "))
    }

    @Test
    fun `unknown or blank names have no built-in colour`() {
        assertNull(BankColours.defaultFor("My Local Co-op"))
        assertNull(BankColours.defaultFor(""))
    }

    @Test
    fun `auto-pick takes the first palette colour no bank uses`() {
        assertEquals("navy", BankColours.autoPick(emptyList()))
        assertEquals("teal", BankColours.autoPick(listOf("navy", "ocean", "rust")))
        assertEquals("ocean", BankColours.autoPick(listOf("navy", "teal")))
    }

    @Test
    fun `auto-pick ignores graphite, custom hex and unknown keys`() {
        assertEquals("navy", BankColours.autoPick(listOf("graphite", "#123456", "ochre")))
    }

    @Test
    fun `a new bank next to the built-in catalog gets forest`() {
        // The built-ins use every palette colour; forest is the first of those used only once.
        val builtIns = BankCatalog.all.mapNotNull { BankColours.defaultFor(it.name) }
        assertEquals("forest", BankColours.autoPick(builtIns))
    }

    @Test
    fun `when every colour is used the least used wins, ties to the earlier one`() {
        val all = ArtRules.PALETTE_ORDER
        assertEquals("navy", BankColours.autoPick(all))
        assertEquals("ocean", BankColours.autoPick(all + "navy"))
        assertEquals("slate", BankColours.autoPick(all + all.dropLast(1)))
    }
}
