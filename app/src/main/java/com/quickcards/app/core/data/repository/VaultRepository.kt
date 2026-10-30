package com.quickcards.app.core.data.repository

import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import com.quickcards.app.core.data.database.QuickCardsDatabase
import com.quickcards.app.core.data.model.Bank
import com.quickcards.app.core.data.model.BandColor
import com.quickcards.app.core.data.model.Card
import com.quickcards.app.core.data.model.CardNetwork
import com.quickcards.app.core.data.model.CardRepair
import com.quickcards.app.core.data.model.CardTag
import com.quickcards.app.core.data.model.CardType
import com.quickcards.app.core.data.model.Holder
import com.quickcards.app.core.data.model.Tag
import com.quickcards.app.core.data.model.VaultCard
import com.quickcards.app.core.data.model.toDomain
import com.quickcards.app.core.security.EncryptionHelper
import com.quickcards.app.core.security.PanHasher
import com.quickcards.domain.CardField
import com.quickcards.domain.CardFieldMask
import com.quickcards.domain.CardNumberRules
import com.quickcards.domain.LegacyCardNormalizer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.Locale

class DuplicateCardException(val existingCardId: String?) : Exception("Card already saved")

class VaultRepository(
    private val database: QuickCardsDatabase,
    private val encryption: EncryptionHelper = EncryptionHelper.getInstance(),
    private val repairSummaries: RepairSummaryStore = InMemoryRepairSummaryStore()
) {

    private val cardDao = database.cardDao()
    private val bankDao = database.bankDao()
    private val holderDao = database.holderDao()
    private val networkDao = database.networkDao()
    private val cardTypeDao = database.cardTypeDao()
    private val tagDao = database.tagDao()
    private val cardTagDao = database.cardTagDao()
    private val cardRepairDao = database.cardRepairDao()

    val cards: Flow<List<VaultCard>> = combine(
        cardDao.getOrderedVaultCards(),
        cardTagDao.getAllCardTags()
    ) { cards, links ->
        val tagsByCard = links.groupBy({ it.cardId }, { it.tagId })
        cards.mapNotNull { card ->
            runCatching { card.toDomain(tagsByCard[card.id].orEmpty()) }.getOrNull()
        }
    }.flowOn(Dispatchers.Default)

    /** Archived cards, newest archive first, decrypted with their tags. */
    fun observeArchived(): Flow<List<VaultCard>> = combine(
        cardDao.getArchivedVaultCards(),
        cardTagDao.getAllCardTags()
    ) { cards, links ->
        val tagsByCard = links.groupBy({ it.cardId }, { it.tagId })
        cards.mapNotNull { card ->
            runCatching { card.toDomain(tagsByCard[card.id].orEmpty()) }.getOrNull()
        }
    }.flowOn(Dispatchers.Default)

    /** The one-time repair banner, read and dismissed by the Cards screen. */
    val repairSummary: Flow<RepairSummary?> = repairSummaries.summary

    suspend fun dismissRepairSummary() {
        repairSummaries.dismiss()
    }

    /** Adds an import's fix counts to the same banner an upgrade may show. */
    suspend fun recordRepairSummary(summary: RepairSummary) {
        repairSummaries.record(summary)
    }

    /** Archives and unpins. [reason] is trimmed to 40 characters and encrypted at rest. */
    suspend fun archive(cardId: String, reason: String?) {
        val clean = reason?.trim()?.take(40)?.takeIf { it.isNotEmpty() }
        cardDao.archive(
            id = cardId,
            at = System.currentTimeMillis(),
            reason = clean?.let(encryption::encryptMetadata)
        )
    }

    /** Returns an archived card to the main list. The pin is not restored. */
    suspend fun restore(cardId: String) {
        cardDao.unarchive(cardId, System.currentTimeMillis())
    }

    /** Decrypted repairs from the last 30 days, newest first. */
    suspend fun repairsFor(
        cardId: String,
        now: Long = System.currentTimeMillis()
    ): List<CardRepair> = cardRepairDao
        .getRepairsForCard(cardId, LegacyRepair.windowStart(now))
        .mapNotNull { repair ->
            val field = CardField.entries.firstOrNull { it.name == repair.field }
                ?: return@mapNotNull null
            val before = runCatching { encryption.decrypt(repair.before) }.getOrNull()
                ?: return@mapNotNull null
            val after = runCatching { encryption.decrypt(repair.after) }.getOrNull()
                ?: return@mapNotNull null
            CardRepair(
                id = repair.id,
                cardId = repair.cardId,
                field = field,
                before = before,
                after = after,
                at = repair.at
            )
        }

    /** Undoes the latest repair of [field] and recomputes the review mask; false if none in 30 days. */
    suspend fun undoRepair(
        cardId: String,
        field: CardField,
        now: Long = System.currentTimeMillis()
    ): Boolean = LegacyRepair.undo(
        database = database,
        summaries = repairSummaries,
        cardId = cardId,
        field = field,
        now = now,
        encryption = encryption
    )

    val holders: Flow<List<Holder>> = holderDao.getAllHolders().map { holders ->
        holders.mapNotNull { holder ->
            runCatching { holder.copy(name = encryption.decryptMetadata(holder.name)) }.getOrNull()
        }.sortedBy { it.name.lowercase(Locale.ROOT) }
    }.flowOn(Dispatchers.Default)

    val banks: Flow<List<Bank>> = bankDao.getAllBanks()

    val networks: Flow<List<CardNetwork>> = networkDao.getAllNetworks()

    val cardTypes: Flow<List<CardType>> = cardTypeDao.getAllCardTypes()

    val tags: Flow<List<Tag>> = tagDao.getAllTags()

    suspend fun getCard(id: String): VaultCard? {
        val card = cardDao.getCardById(id) ?: return null
        val tagIds = cardTagDao.getTagIdsForCard(id)
        return runCatching { card.toDomain(tagIds) }.getOrNull()
    }

    suspend fun findDuplicate(number: String, excludingId: String? = null): VaultCard? {
        val card = cardDao.getCardByPanHash(PanHasher.hash(number)) ?: return null
        if (card.id == excludingId) return null
        val tagIds = cardTagDao.getTagIdsForCard(card.id)
        return runCatching { card.toDomain(tagIds) }.getOrNull()
    }

    suspend fun insertCard(card: VaultCard): Result<VaultCard> {
        return try {
            val duplicate = findDuplicate(card.number)
            if (duplicate != null) {
                return Result.failure(DuplicateCardException(duplicate.id))
            }
            val now = System.currentTimeMillis()
            val reviewMask = cardReviewMask(card)
            val entity = database.withTransaction {
                val entity = card.toEntity(
                    sortOrder = cardDao.maxSortOrder() + 1,
                    createdAt = now,
                    updatedAt = now,
                    reviewMask = reviewMask
                )
                cardDao.insertCard(entity)
                replaceTags(entity.id, card.tagIds)
                entity
            }
            Result.success(
                card.copy(
                    id = entity.id,
                    sortOrder = entity.sortOrder,
                    createdAt = now,
                    updatedAt = now,
                    needsReview = CardFieldMask.decode(reviewMask)
                )
            )
        } catch (error: Exception) {
            Result.failure(
                if (error is android.database.sqlite.SQLiteConstraintException) {
                    DuplicateCardException(null)
                } else {
                    error
                }
            )
        }
    }

    suspend fun updateCard(card: VaultCard): Result<VaultCard> {
        return try {
            val existing = cardDao.getCardById(card.id)
                ?: return Result.failure(IllegalArgumentException("Unknown card"))
            val duplicate = findDuplicate(card.number, card.id)
            if (duplicate != null) {
                return Result.failure(DuplicateCardException(duplicate.id))
            }
            val now = System.currentTimeMillis()
            val reviewMask = cardReviewMask(card)
            val entity = card.toEntity(
                sortOrder = existing.sortOrder,
                createdAt = existing.createdAt,
                updatedAt = now,
                reviewMask = reviewMask
            ).copy(
                archived = existing.archived,
                archivedAt = existing.archivedAt,
                archiveReason = existing.archiveReason,
                pinned = existing.pinned,
                pinnedAt = existing.pinnedAt
            )
            database.withTransaction {
                cardDao.updateCard(entity)
                replaceTags(entity.id, card.tagIds)
            }
            Result.success(
                card.copy(
                    sortOrder = existing.sortOrder,
                    createdAt = existing.createdAt,
                    updatedAt = now,
                    archived = existing.archived,
                    archivedAt = existing.archivedAt,
                    archiveReason = existing.archiveReason?.let { reason ->
                        runCatching { encryption.decryptMetadata(reason) }.getOrNull()
                    },
                    pinned = existing.pinned,
                    pinnedAt = existing.pinnedAt,
                    needsReview = CardFieldMask.decode(reviewMask)
                )
            )
        } catch (error: Exception) {
            Result.failure(
                if (error is android.database.sqlite.SQLiteConstraintException) {
                    DuplicateCardException(null)
                } else {
                    error
                }
            )
        }
    }

    suspend fun deleteCard(id: String) {
        database.withTransaction {
            cardTagDao.deleteLinksForCard(id)
            cardDao.deleteCardById(id)
        }
    }

    /** Re-inserts an undone deletion at its previous position. */
    suspend fun restoreCard(card: VaultCard): Result<VaultCard> {
        return try {
            val duplicate = findDuplicate(card.number, card.id)
            if (duplicate != null) {
                return Result.failure(DuplicateCardException(duplicate.id))
            }
            val now = System.currentTimeMillis()
            val entity = database.withTransaction {
                val entity = card.toEntity(
                    sortOrder = card.sortOrder,
                    createdAt = card.createdAt,
                    updatedAt = now
                )
                cardDao.insertCard(entity)
                replaceTags(entity.id, card.tagIds)
                entity
            }
            Result.success(card.copy(sortOrder = entity.sortOrder, updatedAt = now))
        } catch (error: Exception) {
            Result.failure(
                if (error is android.database.sqlite.SQLiteConstraintException) {
                    DuplicateCardException(null)
                } else {
                    error
                }
            )
        }
    }

    suspend fun getBank(id: String): Bank? = bankDao.getBankById(id)

    data class CardPinState(
        val id: String,
        val pinned: Boolean,
        val pinnedAt: Long?
    )

    suspend fun pinState(id: String): CardPinState? {
        val card = cardDao.getCardById(id) ?: return null
        return CardPinState(card.id, card.pinned, card.pinnedAt)
    }

    suspend fun setPinned(state: CardPinState) {
        cardDao.setPinned(state.id, state.pinned, state.pinnedAt, System.currentTimeMillis())
    }

    suspend fun nextPinnedAt(): Long = cardDao.maxPinnedAt() + 1

    suspend fun pin(id: String) {
        cardDao.setPinned(id, true, nextPinnedAt(), System.currentTimeMillis())
    }

    suspend fun unpin(id: String) {
        cardDao.setPinned(id, false, null, System.currentTimeMillis())
    }

    /** Unpins the most recently pinned cards above [limit]; returns how many. */
    suspend fun trimPinned(limit: Int): Int {
        val pinned = cardDao.getPinnedCardsSync()
        val extras = pinned
            .sortedByDescending { it.pinnedAt ?: 0L }
            .take((pinned.size - limit).coerceAtLeast(0))
        val now = System.currentTimeMillis()
        extras.forEach { cardDao.setPinned(it.id, false, null, now) }
        return extras.size
    }

    /** Counted in Kotlin: networkAuto cards use the detected network, which SQL cannot see. */
    suspend fun networkUsage(): Map<String, Int> {
        val networks = networkDao.getAllNetworksSync().map { it.toDomain() }
        val usage = networks.associate { it.id to 0 }.toMutableMap()
        for (card in cardDao.getVaultCardsSync()) {
            val networkId = if (card.networkAuto) {
                val number = runCatching { encryption.decrypt(card.cardNumber) }.getOrNull()
                number?.let { CardNumberRules.detect(it, networks)?.id }
            } else {
                card.networkId
            }
            if (networkId != null && usage.containsKey(networkId)) {
                usage[networkId] = (usage.getValue(networkId)) + 1
            }
        }
        return usage
    }

    suspend fun countCardsForNetwork(id: String): Int = networkUsage()[id] ?: 0

    suspend fun createHolder(name: String): Holder {
        val displayName = normalizeName(name)
        val existing = decryptHolders().firstOrNull { it.name.equals(displayName, true) }
        if (existing != null) return existing
        val holder = Holder(name = encryption.encryptMetadata(displayName))
        holderDao.insertHolder(holder)
        return holder.copy(name = displayName)
    }

    suspend fun renameHolder(id: String, name: String) {
        val holder = holderDao.getHolderById(id) ?: return
        holderDao.updateHolder(holder.copy(name = encryption.encryptMetadata(normalizeName(name))))
    }

    suspend fun deleteHolder(id: String): Boolean {
        if (holderDao.countCards(id) > 0) return false
        holderDao.deleteHolderById(id)
        return true
    }

    suspend fun createBank(name: String): Bank {
        val displayName = normalizeName(name)
        val existing = bankDao.findBankByName(displayName)
        if (existing != null) return existing
        val bank = Bank(name = displayName)
        bankDao.insertBank(bank)
        return bank
    }

    suspend fun renameBank(id: String, name: String) {
        val bank = bankDao.getBankById(id) ?: return
        bankDao.updateBank(bank.copy(name = normalizeName(name)))
    }

    suspend fun deleteBank(id: String): Boolean {
        if (bankDao.countCards(id) > 0) return false
        val bank = bankDao.getBankById(id) ?: return true
        bankDao.deleteBank(bank)
        return true
    }

    suspend fun createCardType(name: String): CardType {
        val displayName = normalizeName(name)
        val existing = cardTypeDao.findCardTypeByName(displayName)
        if (existing != null) return existing
        val cardType = CardType(name = displayName, sortOrder = cardTypeDao.maxSortOrder() + 1)
        cardTypeDao.insertCardType(cardType)
        return cardType
    }

    suspend fun renameCardType(id: String, name: String) {
        val cardType = cardTypeDao.getCardTypeById(id) ?: return
        cardTypeDao.updateCardType(cardType.copy(name = normalizeName(name)))
    }

    suspend fun deleteCardType(id: String): Boolean {
        if (cardTypeDao.countCards(id) > 0) return false
        cardTypeDao.deleteCardTypeById(id)
        return true
    }

    suspend fun createTag(name: String): Tag {
        val displayName = normalizeName(name)
        val existing = tagDao.findTagByName(displayName)
        if (existing != null) return existing
        val tag = Tag(name = displayName)
        tagDao.insertTag(tag)
        return tag
    }

    suspend fun renameTag(id: String, name: String) {
        val tag = tagDao.getTagById(id) ?: return
        tagDao.updateTag(tag.copy(name = normalizeName(name)))
    }

    suspend fun deleteTag(id: String): Boolean {
        if (tagDao.countCards(id) > 0) return false
        tagDao.deleteTagById(id)
        return true
    }

    suspend fun createNetwork(network: CardNetwork): CardNetwork {
        val created = network.copy(
            id = network.id.ifBlank { java.util.UUID.randomUUID().toString() },
            sortOrder = networkDao.maxSortOrder() + 1,
            builtIn = false
        )
        networkDao.insertNetwork(created)
        return created
    }

    suspend fun getNetwork(id: String): CardNetwork? = networkDao.getNetworkById(id)

    suspend fun updateNetwork(network: CardNetwork) {
        networkDao.updateNetwork(network)
        refreshReviewFlags()
    }

    /**
     * A rule change can make saved cards fit or stop fitting their network. Only the Review
     * flag follows; card values are never rewritten. Unreadable cards are skipped.
     */
    private suspend fun refreshReviewFlags() = database.withTransaction {
        val networks = networkDao.getAllNetworksSync().map { it.toDomain() }
        cardDao.getAllCardsSync().forEach { card ->
            val mask = runCatching {
                val normalized = LegacyCardNormalizer.normalize(
                    number = encryption.decrypt(card.cardNumber),
                    expiry = encryption.decrypt(card.expiryDate),
                    cvv = encryption.decrypt(card.cvv),
                    networks = networks
                )
                CardFieldMask.encode(normalized.needsReview)
            }.getOrNull() ?: return@forEach
            if (mask != card.needsReview) cardDao.setNeedsReview(card.id, mask)
        }
    }

    suspend fun deleteNetwork(id: String): Boolean {
        if (countCardsForNetwork(id) > 0) return false
        networkDao.deleteNetworkById(id)
        return true
    }

    suspend fun restoreHolder(holder: Holder) {
        holderDao.insertHolder(holder.copy(name = encryption.encryptMetadata(normalizeName(holder.name))))
    }

    suspend fun restoreBank(bank: Bank) {
        bankDao.insertBank(bank)
    }

    suspend fun restoreCardType(cardType: CardType) {
        cardTypeDao.insertCardType(cardType)
    }

    suspend fun restoreTag(tag: Tag) {
        tagDao.insertTag(tag)
    }

    suspend fun restoreNetwork(network: CardNetwork) {
        networkDao.insertNetwork(network)
    }

    private suspend fun replaceTags(cardId: String, tagIds: List<String>) {
        cardTagDao.deleteLinksForCard(cardId)
        val links = tagIds.distinct().map { CardTag(cardId = cardId, tagId = it) }
        if (links.isNotEmpty()) cardTagDao.insertLinks(links)
    }

    private suspend fun decryptHolders(): List<Holder> =
        holderDao.getAllHoldersSync().mapNotNull { holder ->
            runCatching { holder.copy(name = encryption.decryptMetadata(holder.name)) }.getOrNull()
        }

    private fun Card.toDomain(tagIds: List<String>) = VaultCard(
        id = id,
        number = encryption.decrypt(cardNumber),
        expiry = encryption.decrypt(expiryDate),
        cvv = encryption.decrypt(cvv),
        holderId = holderId,
        bankId = bankId,
        networkId = networkId,
        networkAuto = networkAuto,
        typeId = typeId,
        nameOnCard = encryption.decryptMetadata(nameOnCard),
        ownerName = encryption.decryptMetadata(owner),
        cardName = cardVariant,
        note = encryption.decryptMetadata(description),
        dueDay = dueDay,
        bandColor = bandColor,
        sortOrder = sortOrder,
        createdAt = createdAt,
        updatedAt = updatedAt,
        tagIds = tagIds,
        pinned = pinned,
        pinnedAt = pinnedAt,
        archived = archived,
        archivedAt = archivedAt,
        archiveReason = archiveReason?.let { reason ->
            runCatching { encryption.decryptMetadata(reason) }.getOrNull()
        }?.takeIf { it.isNotBlank() },
        needsReview = CardFieldMask.decode(needsReview)
    )

    private suspend fun VaultCard.toEntity(
        sortOrder: Int,
        createdAt: Long,
        updatedAt: Long,
        reviewMask: Int = CardFieldMask.encode(needsReview)
    ): Card {
        val holderName = holderId?.let { holderDao.getHolderById(it) }
            ?.let { runCatching { encryption.decryptMetadata(it.name) }.getOrNull() }
            .orEmpty()
        val bankName = bankId?.let { bankDao.getBankById(it)?.name }.orEmpty()
        val typeName = typeId?.let { cardTypeDao.getCardTypeById(it)?.name }.orEmpty()
        val networkName = networkId?.let { networkDao.getNetworkById(it)?.name }.orEmpty()
        return Card(
            id = id.ifBlank { java.util.UUID.randomUUID().toString() },
            cardNumber = encryption.encrypt(number),
            expiryDate = encryption.encrypt(expiry),
            cvv = encryption.encrypt(cvv),
            bankName = encryption.encryptMetadata(bankName),
            cardType = typeName,
            cardIssuer = networkName,
            cardVariant = cardName,
            owner = encryption.encryptMetadata(holderName),
            description = encryption.encryptMetadata(note),
            dueDay = dueDay,
            cardColor = BandColor.hexFor(bandColor),
            createdAt = createdAt,
            updatedAt = updatedAt,
            panHash = PanHasher.hash(number),
            holderId = holderId,
            bankId = bankId,
            networkId = networkId,
            networkAuto = networkAuto,
            typeId = typeId,
            nameOnCard = encryption.encryptMetadata(nameOnCard.trim().uppercase(Locale.ROOT)),
            bandColor = bandColor,
            sortOrder = sortOrder,
            pinned = pinned,
            pinnedAt = if (pinned) pinnedAt else null,
            archived = archived,
            archivedAt = if (archived) archivedAt else null,
            archiveReason = archiveReason
                ?.takeIf { it.isNotBlank() }
                ?.let(encryption::encryptMetadata),
            needsReview = reviewMask
        )
    }

    private suspend fun cardReviewMask(card: VaultCard): Int {
        val networks = networkDao.getAllNetworksSync().map { it.toDomain() }
        val normalized = LegacyCardNormalizer.normalize(
            number = card.number,
            expiry = card.expiry,
            cvv = card.cvv,
            networks = networks
        )
        return CardFieldMask.encode(normalized.needsReview)
    }

    private fun normalizeName(name: String): String =
        name.trim().replace(Regex("\\s+"), " ")
}
