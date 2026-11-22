package com.quickcards.app.core.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.quickcards.domain.BankCatalog
import java.util.UUID

@Entity(tableName = "banks")
data class Bank(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    /** Palette key or `#RRGGBB`; null means the built-in default for the bank. */
    val color: String? = null
) {
    companion object {
        /** A fresh install starts with the bundled catalog; older installs get it from the backfill. */
        fun getDefaultBanks(): List<Bank> = BankCatalog.all.map { Bank(name = it.name) }
    }
}
