package com.quickcards.app.core.data.backup

import androidx.room.withTransaction
import com.quickcards.domain.NetworkLabelMax
import com.quickcards.app.core.data.database.QuickCardsDatabase
import com.quickcards.app.core.data.model.BandColor
import com.quickcards.app.core.data.model.Bank
import com.quickcards.app.core.data.model.Card
import com.quickcards.app.core.data.model.CardNetwork
import com.quickcards.app.core.data.model.CardRepairEntity
import com.quickcards.app.core.data.model.CardTag
import com.quickcards.app.core.data.model.CardType
import com.quickcards.app.core.data.model.Holder
import com.quickcards.app.core.data.model.SeedData
import com.quickcards.app.core.data.model.Tag
import com.quickcards.app.core.data.settings.AppSettings
import com.quickcards.app.core.security.EncryptionHelper
import com.quickcards.app.core.security.PanHasher
import com.quickcards.domain.BackupContent
import com.quickcards.domain.BackupPlanner
import com.quickcards.domain.CardDisposition
import com.quickcards.domain.CardFieldMask
import com.quickcards.domain.MergeMode
import com.quickcards.domain.PhoneCard
import com.quickcards.domain.ResolvedCard
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class BackupImportResult(
    val added: Int,
    val updated: Int,
    val kept: Int,
    val same: Int,
    val settingsRestored: Boolean,
    val formatsFixed: Int = 0,
    val needCheck: Int = 0
)

/**
 * Applies a backup in one transaction. Lists match by name ignoring case (phone items and
 * network rules win); cards match by normalized number (the PAN hash input).
 */
object BackupImporter {

    private val WHITESPACE = Regex("""\s+""")

    suspend fun import(
        database: QuickCardsDatabase,
        content: BackupContent,
        mode: MergeMode,
        settings: AppSettings? = null,
        restoreSettings: Boolean = false,
        encryption: EncryptionHelper = EncryptionHelper.getInstance(),
        now: Long = System.currentTimeMillis()
    ): BackupImportResult {
        val networks = database.networkDao().getAllNetworksSync().map { network ->
            com.quickcards.domain.CardNetwork(
                id = network.id,
                name = network.name,
                code = network.code,
                prefixes = network.prefixes,
                lengths = network.lengths,
                cvvLength = network.cvvLength,
                luhn = network.luhn,
                groups = network.groups
            )
        }
        val normalized = BackupNormalizer.normalize(content, networks)

        var counts = Counts()
        database.withTransaction {
            counts = merge(database, normalized, mode, encryption, now)
        }
        val restored = restoreSettings && content.settings != null && settings != null
        if (restored) {
            BackupSettings.restore(settings!!, content.settings!!)
        }
        return BackupImportResult(
            added = counts.added,
            updated = counts.updated,
            kept = counts.kept,
            same = counts.same,
            settingsRestored = restored,
            formatsFixed = counts.formatsFixed,
            needCheck = counts.needCheck
        )
    }

    suspend fun eraseAll(database: QuickCardsDatabase) {
        database.withTransaction {
            database.cardRepairDao().deleteAll()
            database.cardTagDao().deleteAllLinks()
            database.cardDao().deleteAllCards()
            database.holderDao().deleteAllHolders()
            database.bankDao().deleteAllBanks()
            database.cardTypeDao().deleteAllCardTypes()
            database.tagDao().deleteAllTags()
            database.networkDao().deleteAllNetworks()
            database.networkDao().insertNetworks(SeedData.networks)
        }
        // Deleted rows linger in free pages and the WAL until they are reused;
        // checkpoint and VACUUM so the erased ciphertext leaves the file now.
        withContext(Dispatchers.IO) {
            database.openHelper.writableDatabase.apply {
                query("PRAGMA wal_checkpoint(TRUNCATE)").close()
                execSQL("VACUUM")
            }
        }
    }

    private suspend fun merge(
        database: QuickCardsDatabase,
        normalized: NormalizedBackup,
        mode: MergeMode,
        encryption: EncryptionHelper,
        now: Long
    ): Counts {
        val content = normalized.content
        val holderDao = database.holderDao()
        val bankDao = database.bankDao()
        val typeDao = database.cardTypeDao()
        val tagDao = database.tagDao()
        val networkDao = database.networkDao()
        val cardDao = database.cardDao()
        val cardTagDao = database.cardTagDao()

        val holderNamesById = linkedMapOf<String, String>()
        val holderIdsByName = linkedMapOf<String, String>()
        holderDao.getAllHoldersSync().forEach { holder ->
            val name = runCatching { encryption.decryptMetadata(holder.name) }.getOrNull() ?: return@forEach
            holderNamesById[holder.id] = name
            holderIdsByName.putIfAbsent(normalizeName(name), holder.id)
        }
        val holderMap = linkedMapOf<String, String?>()
        content.holders.forEach { item ->
            val displayName = cleanName(item.name)
            if (displayName.isEmpty()) return@forEach
            val name = normalizeName(displayName)
            val id = holderIdsByName[name] ?: run {
                val created = Holder(name = encryption.encryptMetadata(displayName))
                holderDao.insertHolder(created)
                holderNamesById[created.id] = displayName
                holderIdsByName[name] = created.id
                created.id
            }
            holderMap[item.id] = id
        }

        val bankNamesById = linkedMapOf<String, String>()
        val bankIdsByName = linkedMapOf<String, String>()
        bankDao.getAllBanksSync().forEach { bank ->
            val name = normalizeName(bank.name)
            bankNamesById[bank.id] = bank.name
            bankIdsByName.putIfAbsent(name, bank.id)
        }
        val bankMap = linkedMapOf<String, String?>()
        content.banks.forEach { item ->
            val displayName = cleanName(item.name)
            if (displayName.isEmpty()) return@forEach
            val name = normalizeName(displayName)
            val id = bankIdsByName[name] ?: run {
                val created = Bank(name = displayName)
                bankDao.insertBank(created)
                bankNamesById[created.id] = displayName
                bankIdsByName[name] = created.id
                created.id
            }
            bankMap[item.id] = id
        }

        val typeNamesById = linkedMapOf<String, String>()
        val typeIdsByName = linkedMapOf<String, String>()
        var nextTypeSort = typeDao.maxSortOrder() + 1
        typeDao.getAllCardTypesSync().forEach { type ->
            val name = normalizeName(type.name)
            typeNamesById[type.id] = type.name
            typeIdsByName.putIfAbsent(name, type.id)
        }
        val typeMap = linkedMapOf<String, String?>()
        content.types.forEach { item ->
            val displayName = cleanName(item.name)
            if (displayName.isEmpty()) return@forEach
            val name = normalizeName(displayName)
            val id = typeIdsByName[name] ?: run {
                val created = CardType(name = displayName, sortOrder = nextTypeSort++)
                typeDao.insertCardType(created)
                typeNamesById[created.id] = displayName
                typeIdsByName[name] = created.id
                created.id
            }
            typeMap[item.id] = id
        }

        val tagIdsByName = linkedMapOf<String, String>()
        tagDao.getAllTagsSync().forEach { tag ->
            tagIdsByName.putIfAbsent(normalizeName(tag.name), tag.id)
        }
        val tagMap = linkedMapOf<String, String?>()
        content.tags.forEach { item ->
            val displayName = cleanName(item.name)
            if (displayName.isEmpty()) return@forEach
            val name = normalizeName(displayName)
            val id = tagIdsByName[name] ?: run {
                val created = Tag(name = displayName)
                tagDao.insertTag(created)
                tagIdsByName[name] = created.id
                created.id
            }
            tagMap[item.id] = id
        }

        val networkNamesById = linkedMapOf<String, String>()
        val networkIdsByName = linkedMapOf<String, String>()
        var nextNetworkSort = networkDao.maxSortOrder() + 1
        networkDao.getAllNetworksSync().forEach { network ->
            val name = normalizeName(network.name)
            networkNamesById[network.id] = network.name
            networkIdsByName.putIfAbsent(name, network.id)
        }
        val existingNetworkIdsByName = networkIdsByName.toMap()
        val networkMap = linkedMapOf<String, String?>()
        content.networks.forEach { item ->
            val displayName = cleanName(item.name)
            if (displayName.isEmpty()) return@forEach
            val name = normalizeName(displayName)
            val id = networkIdsByName[name] ?: run {
                val created = CardNetwork(
                    id = UUID.randomUUID().toString(),
                    name = displayName,
                    code = item.code.take(NetworkLabelMax),
                    prefixes = item.prefixes,
                    lengths = item.lengths,
                    cvvLength = if (item.cvv == 4) 4 else 3,
                    luhn = item.luhn,
                    groups = item.groups,
                    sortOrder = nextNetworkSort++,
                    builtIn = false
                )
                networkDao.insertNetwork(created)
                networkNamesById[created.id] = displayName
                networkIdsByName[name] = created.id
                created.id
            }
            networkMap[item.id] = id
        }

        val linksByCard = cardTagDao.getAllCardTagsSync().groupBy({ it.cardId }, { it.tagId })
        val cardsByNumber = linkedMapOf<String, Card>()
        cardDao.getVaultCardsSync().forEach { entity ->
            val number = runCatching { encryption.decrypt(entity.cardNumber) }.getOrNull()
                ?.let(BackupPlanner::normalizeNumber)
                ?.takeIf { it.isNotEmpty() }
                ?: return@forEach
            cardsByNumber[number] = entity
        }

        var nextCardSort = cardDao.maxSortOrder() + 1
        val counts = Counts()
        val cardRepairDao = database.cardRepairDao()

        val seenInFile = HashSet<String>()
        normalized.cards.forEach { entry ->
            val backupCard = entry.card
            val number = BackupPlanner.normalizeNumber(backupCard.number)
            if (number.isEmpty()) return@forEach
            // A second copy of a number in the same file is rejected, never merged over the first.
            if (!seenInFile.add(number)) return@forEach
            val resolved = BackupPlanner.resolve(
                backupCard,
                holderMap,
                bankMap,
                typeMap,
                tagMap,
                networkMap,
                existingNetworkIdsByName
            )
            val existing = cardsByNumber[number]
            val existingPhone = existing?.toPhoneCard(encryption, linksByCard[existing.id].orEmpty())
            val changed = existingPhone != null && BackupPlanner.differs(existingPhone, resolved)
            val reviewMask = CardFieldMask.encode(entry.result.needsReview)
            when (BackupPlanner.disposition(existing?.id, changed, mode)) {
                CardDisposition.ADD -> {
                    val entity = toEntity(
                        resolved = resolved,
                        number = number,
                        holderName = resolved.holderId?.let { holderNamesById[it] }.orEmpty(),
                        bankName = resolved.bankId?.let { bankNamesById[it] }.orEmpty(),
                        typeName = resolved.typeId?.let { typeNamesById[it] }.orEmpty(),
                        networkName = resolved.networkId?.let { networkNamesById[it] }.orEmpty(),
                        sortOrder = nextCardSort++,
                        createdAt = now,
                        updatedAt = now,
                        encryption = encryption,
                        id = UUID.randomUUID().toString(),
                        reviewMask = reviewMask
                    )
                    cardDao.insertCard(entity)
                    replaceTags(cardTagDao, entity.id, resolved.tagIds)
                    insertRepairs(cardRepairDao, entity.id, entry, now, encryption)
                    cardsByNumber[number] = entity
                    counts.record(entry)
                    counts.added++
                }

                CardDisposition.UPDATE -> {
                    val entity = toEntity(
                        resolved = resolved,
                        number = number,
                        holderName = resolved.holderId?.let { holderNamesById[it] }.orEmpty(),
                        bankName = resolved.bankId?.let { bankNamesById[it] }.orEmpty(),
                        typeName = resolved.typeId?.let { typeNamesById[it] }.orEmpty(),
                        networkName = resolved.networkId?.let { networkNamesById[it] }.orEmpty(),
                        sortOrder = existing!!.sortOrder,
                        createdAt = existing.createdAt,
                        updatedAt = now,
                        encryption = encryption,
                        id = existing.id,
                        reviewMask = reviewMask
                    )
                    cardDao.updateCard(entity)
                    replaceTags(cardTagDao, entity.id, resolved.tagIds)
                    insertRepairs(cardRepairDao, entity.id, entry, now, encryption)
                    cardsByNumber[number] = entity
                    counts.record(entry)
                    counts.updated++
                }

                CardDisposition.KEEP -> counts.kept++

                CardDisposition.SAME -> counts.same++
            }
        }

        return counts
    }

    private suspend fun insertRepairs(
        cardRepairDao: com.quickcards.app.core.data.dao.CardRepairDao,
        cardId: String,
        entry: NormalizedBackupCard,
        at: Long,
        encryption: EncryptionHelper
    ) {
        entry.result.fixed.forEach { (field, values) ->
            cardRepairDao.insert(
                CardRepairEntity(
                    cardId = cardId,
                    field = field.name,
                    before = encryption.encrypt(values.first),
                    after = encryption.encrypt(values.second),
                    at = at
                )
            )
        }
    }

    private suspend fun replaceTags(
        cardTagDao: com.quickcards.app.core.data.dao.CardTagDao,
        cardId: String,
        tagIds: List<String>
    ) {
        cardTagDao.deleteLinksForCard(cardId)
        val links = tagIds.distinct().map { CardTag(cardId, it) }
        if (links.isNotEmpty()) cardTagDao.insertLinks(links)
    }

    private fun toEntity(
        resolved: ResolvedCard,
        number: String,
        holderName: String,
        bankName: String,
        typeName: String,
        networkName: String,
        sortOrder: Int,
        createdAt: Long,
        updatedAt: Long,
        encryption: EncryptionHelper,
        id: String,
        reviewMask: Int
    ): Card = Card(
        id = id,
        cardNumber = encryption.encrypt(number),
        expiryDate = encryption.encrypt(resolved.exp),
        cvv = encryption.encrypt(resolved.cvv),
        bankName = encryption.encryptMetadata(bankName),
        cardType = typeName,
        cardIssuer = networkName,
        cardVariant = resolved.variant,
        owner = encryption.encryptMetadata(holderName),
        description = encryption.encryptMetadata(resolved.note),
        dueDay = resolved.dueDay,
        cardColor = BandColor.hexFor(resolved.color),
        createdAt = createdAt,
        updatedAt = updatedAt,
        panHash = PanHasher.hash(number),
        holderId = resolved.holderId,
        bankId = resolved.bankId,
        networkId = resolved.networkId,
        networkAuto = resolved.networkAuto,
        typeId = resolved.typeId,
        nameOnCard = encryption.encryptMetadata(
            resolved.nameOnCard.trim().uppercase(Locale.ROOT)
        ),
        bandColor = resolved.color,
        sortOrder = sortOrder,
        pinned = resolved.pinned,
        pinnedAt = if (resolved.pinned) resolved.pinnedAt else null,
        archived = resolved.archived,
        archivedAt = if (resolved.archived) resolved.archivedAt else null,
        archiveReason = resolved.archiveReason
            .takeIf { it.isNotBlank() }
            ?.let(encryption::encryptMetadata),
        needsReview = reviewMask
    )

    private fun Card.toPhoneCard(encryption: EncryptionHelper, tagIds: List<String>): PhoneCard? =
        runCatching {
            PhoneCard(
                id = id,
                number = encryption.decrypt(cardNumber),
                holderId = holderId,
                bankId = bankId,
                networkAuto = networkAuto,
                networkId = networkId,
                typeId = typeId,
                variant = cardVariant,
                exp = encryption.decrypt(expiryDate),
                cvv = encryption.decrypt(cvv),
                nameOnCard = encryption.decryptMetadata(nameOnCard),
                color = bandColor,
                note = encryption.decryptMetadata(description),
                dueDay = dueDay,
                tagIds = tagIds,
                pinned = pinned,
                pinnedAt = pinnedAt,
                archived = archived,
                archivedAt = archivedAt,
                archiveReason = archiveReason
                    ?.let { runCatching { encryption.decryptMetadata(it) }.getOrNull() }
                    .orEmpty()
            )
        }.getOrNull()

    private fun cleanName(name: String): String =
        name.trim().replace(WHITESPACE, " ")

    private fun normalizeName(name: String): String =
        cleanName(name).lowercase(Locale.ROOT)

    private data class Counts(
        var added: Int = 0,
        var updated: Int = 0,
        var kept: Int = 0,
        var same: Int = 0,
        var formatsFixed: Int = 0,
        var needCheck: Int = 0
    ) {
        fun record(entry: NormalizedBackupCard) {
            if (entry.result.fixed.isNotEmpty()) formatsFixed++
            if (entry.result.needsReview.isNotEmpty()) needCheck++
        }
    }
}
