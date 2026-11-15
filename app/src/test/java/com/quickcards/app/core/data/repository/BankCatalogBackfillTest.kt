package com.quickcards.app.core.data.repository

import com.quickcards.domain.BankCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BankCatalogBackfillTest {

    @Test
    fun aFreshVaultIsMissingTheWholeCatalog() {
        assertEquals(BankCatalog.all.map { it.name }, BankCatalogBackfill.missing(emptyList()))
    }

    @Test
    fun oldSpellingsAndAliasesCountAsPresent() {
        val missing = BankCatalogBackfill.missing(
            listOf("State Bank of India (SBI)", "hdfc bank", "Punjab National Bank (PNB)", "My Co-op Bank")
        )

        assertFalse(missing.contains("SBI Card"))
        assertFalse(missing.contains("HDFC Bank"))
        assertFalse(missing.contains("Punjab National Bank"))
        assertTrue(missing.contains("ICICI Bank"))
        assertEquals(BankCatalog.all.size - 3, missing.size)
    }

    @Test
    fun aVaultWithTheFullCatalogNeedsNothing() {
        assertTrue(BankCatalogBackfill.missing(BankCatalog.all.map { it.name }).isEmpty())
    }
}
