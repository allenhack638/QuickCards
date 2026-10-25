package com.quickcards.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class MonogramTest {

    @Test
    fun bankMonogramsMatchThePrototype() {
        assertEquals("AE", Monogram.monoFor("American Express"))
        assertEquals("HD", Monogram.monoFor("HDFC Bank"))
        assertEquals("IC", Monogram.monoFor("ICICI Bank"))
        assertEquals("AU", Monogram.monoFor("AU Small Finance Bank"))
        assertEquals("ID", Monogram.monoFor("IDFC FIRST Bank"))
        assertEquals("KM", Monogram.monoFor("Kotak Mahindra Bank"))
        assertEquals("AX", Monogram.monoFor("Axis Bank"))
        assertEquals("IN", Monogram.monoFor("IndusInd Bank"))
        assertEquals("FE", Monogram.monoFor("Federal Bank"))
        assertEquals("YE", Monogram.monoFor("Yes Bank"))
    }

    @Test
    fun bankMonogramsIgnoreTheWordOfAndTheWordBank() {
        assertEquals("ST", Monogram.monoFor("State Bank of India"))
        assertEquals("BB", Monogram.monoFor("Bank of Baroda"))
        assertEquals("MY", Monogram.monoFor("my bank"))
        assertEquals("VI", Monogram.monoFor("Visa"))
        assertEquals("HD", Monogram.monoFor("HDFC"))
        assertEquals("?", Monogram.monoFor(""))
        assertEquals("?", Monogram.monoFor("of"))
    }

    @Test
    fun initialsUseTheFirstTwoWordsAndCapitals() {
        assertEquals("AM", Monogram.initials("Aarav Mehta"))
        assertEquals("AM", Monogram.initials("aarav  mehta"))
        assertEquals("MJ", Monogram.initials("Mary Jane Watson"))
        assertEquals("P", Monogram.initials("Priya"))
        assertEquals("", Monogram.initials("   "))
    }
}
