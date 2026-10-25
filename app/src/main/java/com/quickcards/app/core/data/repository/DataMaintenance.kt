package com.quickcards.app.core.data.repository

import com.quickcards.app.core.data.database.QuickCardsDatabase
import com.quickcards.app.core.security.EncryptionHelper
import com.quickcards.app.core.security.PanHasher

/** Runs at app start, before any screen reads the vault. Every step is idempotent. */
object DataMaintenance {

    suspend fun run(
        database: QuickCardsDatabase,
        repairSummaries: RepairSummaryStore
    ): Int {
        encryptLegacyMetadata(database)
        wrapUnderDataKey(database)
        backfillPanHashes(database)
        val backfilled = LegacyBackfill.run(database)
        LegacyRepair.prune(database)
        LegacyRepair.run(database, repairSummaries)
        return backfilled
    }

    /** Idempotent: encryptMetadata leaves already-wrapped values untouched. */
    private suspend fun encryptLegacyMetadata(database: QuickCardsDatabase) {
        val encryption = EncryptionHelper.getInstance()
        val cardDao = database.cardDao()
        for (card in cardDao.getAllCardsSync()) {
            runCatching {
                val migrated = card.copy(
                    bankName = encryption.encryptMetadata(card.bankName),
                    owner = encryption.encryptMetadata(card.owner),
                    description = encryption.encryptMetadata(card.description)
                )
                if (migrated != card) cardDao.updateCard(migrated)
            }
        }
    }

    /** One-off after 2.1: re-wraps Keystore-encrypted fields under the data key. Idempotent. */
    private suspend fun wrapUnderDataKey(database: QuickCardsDatabase) {
        val encryption = EncryptionHelper.getInstance()
        val cardDao = database.cardDao()
        for (card in cardDao.getAllCardsSync()) {
            runCatching {
                val wrapped = card.copy(
                    cardNumber = encryption.reencrypt(card.cardNumber),
                    expiryDate = encryption.reencrypt(card.expiryDate),
                    cvv = encryption.reencrypt(card.cvv),
                    bankName = encryption.reencryptMetadata(card.bankName),
                    owner = encryption.reencryptMetadata(card.owner),
                    description = encryption.reencryptMetadata(card.description),
                    nameOnCard = encryption.reencryptMetadata(card.nameOnCard),
                    archiveReason = card.archiveReason?.let(encryption::reencryptMetadata)
                )
                if (wrapped != card) cardDao.updateCard(wrapped)
            }
        }
        val holderDao = database.holderDao()
        for (holder in holderDao.getAllHoldersSync()) {
            runCatching {
                val wrapped = holder.copy(name = encryption.reencryptMetadata(holder.name))
                if (wrapped != holder) holderDao.updateHolder(wrapped)
            }
        }
    }

    private suspend fun backfillPanHashes(database: QuickCardsDatabase) {
        val encryption = EncryptionHelper.getInstance()
        val cardDao = database.cardDao()
        for (card in cardDao.getAllCardsSync()) {
            if (!card.panHash.isNullOrBlank()) continue
            val pan = runCatching { encryption.decrypt(card.cardNumber) }.getOrNull() ?: continue
            val hash = PanHasher.hash(pan)
            val existing = cardDao.getCardByPanHash(hash)
            if (existing != null && existing.id != card.id) continue
            runCatching { cardDao.updateCard(card.copy(panHash = hash)) }
        }
    }
}
