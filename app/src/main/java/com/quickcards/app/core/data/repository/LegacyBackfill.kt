package com.quickcards.app.core.data.repository

import androidx.room.withTransaction
import com.quickcards.app.core.data.database.QuickCardsDatabase
import com.quickcards.app.core.data.model.BandColor
import com.quickcards.app.core.data.model.CardType
import com.quickcards.app.core.data.model.Holder
import com.quickcards.app.core.security.EncryptionHelper
import java.util.Locale

/**
 * Backfills the v10 card links from the legacy plaintext/ciphertext columns.
 * Runs once, inside a transaction, and never deletes a row.
 */
object LegacyBackfill {

    private const val LEGACY_OWNER = "me"
    private const val HOUSEHOLD_HOLDER = "Me"

    suspend fun run(database: QuickCardsDatabase): Int {
        val cardDao = database.cardDao()
        if (cardDao.countCardsNeedingBackfill() == 0) return 0

        val encryption = EncryptionHelper.getInstance()
        val holderDao = database.holderDao()
        val bankDao = database.bankDao()
        val cardTypeDao = database.cardTypeDao()
        val networkDao = database.networkDao()

        var updated = 0
        database.withTransaction {
            val cards = cardDao.getCardsNeedingBackfill()
            if (cards.isEmpty()) return@withTransaction

            val holdersByKey = holderDao.getAllHoldersSync()
                .mapNotNull { holder ->
                    runCatching { holder.copy(name = encryption.decryptMetadata(holder.name)) }
                        .getOrNull()
                }
                .associateBy { it.name.trim().lowercase(Locale.ROOT) }
                .toMutableMap()
            val banksByName = bankDao.getAllBanksSync()
                .associateBy { it.name.trim().lowercase(Locale.ROOT) }
                .toMutableMap()
            val typesByName = cardTypeDao.getAllCardTypesSync()
                .associateBy { it.name.trim().lowercase(Locale.ROOT) }
                .toMutableMap()

            for (card in cards) {
                val owner = decrypt(encryption, card.owner)
                val holderName = normalizeOwner(owner)
                val holder = holdersByKey[holderName.lowercase(Locale.ROOT)] ?: run {
                    val created = Holder(name = encryption.encryptMetadata(holderName))
                    holderDao.insertHolder(created)
                    created
                }.also { holdersByKey[holderName.lowercase(Locale.ROOT)] = it }

                val bankName = decrypt(encryption, card.bankName).trim()
                val bank = if (bankName.isEmpty()) {
                    null
                } else {
                    banksByName[bankName.lowercase(Locale.ROOT)] ?: run {
                        val created = com.quickcards.app.core.data.model.Bank(name = bankName)
                        bankDao.insertBank(created)
                        created
                    }.also { banksByName[bankName.lowercase(Locale.ROOT)] = it }
                }

                val typeName = card.cardType.trim()
                val cardType = if (typeName.isEmpty()) {
                    null
                } else {
                    typesByName[typeName.lowercase(Locale.ROOT)] ?: run {
                        val created = CardType(
                            name = typeName,
                            sortOrder = cardTypeDao.maxSortOrder() + 1
                        )
                        cardTypeDao.insertCardType(created)
                        created
                    }.also { typesByName[typeName.lowercase(Locale.ROOT)] = it }
                }

                val issuer = card.cardIssuer.trim()
                val network = if (issuer.isEmpty()) null else networkDao.findNetworkByName(issuer)

                val nameOnCard = if (owner.trim().equals(LEGACY_OWNER, ignoreCase = true)) {
                    ""
                } else {
                    owner.trim().uppercase(Locale.ROOT)
                }

                cardDao.updateCard(
                    card.copy(
                        holderId = holder.id,
                        bankId = bank?.id,
                        networkId = network?.id,
                        networkAuto = network == null,
                        typeId = cardType?.id,
                        nameOnCard = encryption.encryptMetadata(nameOnCard),
                        bandColor = BandColor.importValue(card.cardColor)
                    )
                )
                updated++
            }
        }
        return updated
    }

    private fun normalizeOwner(owner: String): String {
        val trimmed = owner.trim()
        return when {
            trimmed.isEmpty() -> HOUSEHOLD_HOLDER
            trimmed.equals(LEGACY_OWNER, ignoreCase = true) -> HOUSEHOLD_HOLDER
            else -> trimmed
        }
    }

    private fun decrypt(encryption: EncryptionHelper, value: String): String =
        runCatching { encryption.decryptMetadata(value) }.getOrDefault("")
}
