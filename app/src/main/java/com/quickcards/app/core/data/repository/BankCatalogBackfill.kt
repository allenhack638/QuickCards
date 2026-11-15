package com.quickcards.app.core.data.repository

import android.content.Context
import com.quickcards.app.core.data.database.QuickCardsDatabase
import com.quickcards.app.core.data.model.Bank
import com.quickcards.domain.BankCatalog

/** Remembers which [BankCatalog.VERSION] an install has already received. */
interface CatalogVersionStore {
    fun appliedBankCatalog(): String?
    fun markBankCatalog(version: String)
}

class CatalogVersionPrefs(context: Context) : CatalogVersionStore {

    private val preferences = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun appliedBankCatalog(): String? = preferences.getString(KEY_BANK_CATALOG, null)

    override fun markBankCatalog(version: String) {
        preferences.edit().putString(KEY_BANK_CATALOG, version).apply()
    }

    private companion object {
        const val PREFS_NAME = "quickcards_catalog"
        const val KEY_BANK_CATALOG = "bank_catalog_version"
    }
}

/**
 * Adds the catalog banks an older install does not have yet, once per catalog version. A bank
 * the user already has under another spelling or alias ("State Bank of India (SBI)" for
 * "SBI Card") counts as present, so nothing is duplicated and nothing is renamed or deleted.
 * Running once per version means a bank the user deleted afterwards stays deleted.
 */
object BankCatalogBackfill {

    /** The catalog names missing from [existingNames], matched by [BankCatalog.find]. */
    fun missing(existingNames: Collection<String>): List<String> {
        val present = existingNames.mapNotNull { BankCatalog.find(it)?.name }.toSet()
        return BankCatalog.all.map { it.name }.filterNot { it in present }
    }

    suspend fun run(database: QuickCardsDatabase, versions: CatalogVersionStore): Int {
        if (versions.appliedBankCatalog() == BankCatalog.VERSION) return 0
        val bankDao = database.bankDao()
        val toAdd = missing(bankDao.getAllBanksSync().map { it.name })
        if (toAdd.isNotEmpty()) bankDao.insertBanks(toAdd.map { Bank(name = it) })
        versions.markBankCatalog(BankCatalog.VERSION)
        return toAdd.size
    }
}
