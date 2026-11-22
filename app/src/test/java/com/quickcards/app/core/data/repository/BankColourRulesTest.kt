package com.quickcards.app.core.data.repository

import com.quickcards.app.core.data.model.Bank
import com.quickcards.domain.ArtRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BankColourRulesTest {

    @Test
    fun `a stored bank colour wins over the built-in default`() {
        assertEquals("teal", BankColourRules.colourOf(Bank(name = "HDFC Bank", color = "teal")))
        assertEquals("#0F5E5A", BankColourRules.colourOf(Bank(name = "Corner Bank", color = "#0F5E5A")))
    }

    @Test
    fun `a bank without a stored colour uses its built-in default`() {
        assertEquals("navy", BankColourRules.colourOf(Bank(name = "HDFC Bank")))
        assertEquals("ocean", BankColourRules.colourOf(Bank(name = "amex")))
    }

    @Test
    fun `an unknown bank without a stored colour has none`() {
        assertNull(BankColourRules.colourOf(Bank(name = "Corner Bank")))
    }

    @Test
    fun `a manual card colour is kept`() {
        assertEquals("plum", BankColourRules.cardColour(manual = true, own = "plum", bankColour = "navy"))
    }

    @Test
    fun `a card that follows its bank takes the bank colour`() {
        assertEquals("navy", BankColourRules.cardColour(manual = false, own = "plum", bankColour = "navy"))
    }

    @Test
    fun `a card whose bank has no colour keeps its own`() {
        assertEquals("plum", BankColourRules.cardColour(manual = false, own = "plum", bankColour = null))
    }

    @Test
    fun `a new catalog bank keeps the built-in default`() {
        assertNull(BankColourRules.pickFor("Citi", emptyList()))
    }

    @Test
    fun `a new bank gets the first colour no bank uses`() {
        val banks = listOf(Bank(name = "HDFC Bank"), Bank(name = "SBI Card"), Bank(name = "Mine", color = "teal"))

        assertEquals("forest", BankColourRules.pickFor("Corner Bank", banks))
    }

    @Test
    fun `banks without any colour are not counted`() {
        assertEquals("navy", BankColourRules.pickFor("Corner Bank", listOf(Bank(name = "Old Co-op"))))
    }

    private val others = listOf(Bank(name = "HDFC Bank"), Bank(name = "SBI Card"))

    @Test
    fun `a new unknown bank saves the colour the editor shows`() {
        val shown = BankColourRules.editorColour("Corner Bank", null, null, others)
        assertEquals("teal", shown)
        assertEquals(shown, BankColourRules.colourToStore("Corner Bank", null, null, others))
    }

    @Test
    fun `a new catalog bank follows its built-in default`() {
        assertEquals("navy", BankColourRules.editorColour("HDFC Bank", null, null, emptyList()))
        assertNull(BankColourRules.colourToStore("HDFC Bank", null, null, emptyList()))
    }

    @Test
    fun `an old unknown bank without a stored colour saves the auto-pick it shows`() {
        val before = Bank(name = "Old Co-op")
        val shown = BankColourRules.editorColour("Old Co-op", null, before, others)
        assertEquals("teal", shown)
        assertEquals(shown, BankColourRules.colourToStore("Old Co-op", null, before, others))
    }

    @Test
    fun `a stored colour stays when the name keeps the same default`() {
        val before = Bank(name = "Corner Bank", color = "teal")
        assertEquals("teal", BankColourRules.editorColour("Corner Bank Ltd", null, before, others))
        assertEquals("teal", BankColourRules.colourToStore("Corner Bank Ltd", null, before, others))
    }

    @Test
    fun `renaming onto a built-in bank takes that bank's default`() {
        val before = Bank(name = "Corner Bank", color = "teal")
        assertEquals("navy", BankColourRules.editorColour("HDFC Bank", null, before, others))
        assertNull(BankColourRules.colourToStore("HDFC Bank", null, before, others))
    }

    @Test
    fun `renaming a built-in bank away from the catalog keeps the colour it showed`() {
        val before = Bank(name = "HDFC Bank")
        assertEquals("navy", BankColourRules.editorColour("Corner Bank", null, before, others))
        assertEquals("navy", BankColourRules.colourToStore("Corner Bank", null, before, others))
    }

    @Test
    fun `a colour picked by hand always wins`() {
        val before = Bank(name = "Corner Bank", color = "teal")
        assertEquals("plum", BankColourRules.editorColour("HDFC Bank", "plum", before, others))
        assertEquals("plum", BankColourRules.colourToStore("HDFC Bank", "plum", before, others))
    }

    @Test
    fun `the bank editor shows the colour saving would give`() {
        val banks = listOf(Bank(id = "1", name = "HDFC Bank"), Bank(id = "2", name = "SBI Card"))
        assertEquals("navy", BankColourRules.editorColour("HDFC", null, null, banks))
        assertEquals(ArtRules.PALETTE_ORDER[2], BankColourRules.editorColour("Saraswat Bank", null, null, banks))
        assertEquals("plum", BankColourRules.editorColour("Saraswat Bank", "plum", null, banks))
    }
}
