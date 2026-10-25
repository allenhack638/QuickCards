package com.quickcards.app.core.data.backup

import com.quickcards.domain.BACKUP_FORMAT
import com.quickcards.domain.BACKUP_APP_NAME
import com.quickcards.app.core.data.repository.VaultRepository
import com.quickcards.app.core.data.settings.AppSettings
import com.quickcards.domain.BackupBank
import com.quickcards.domain.BackupCard
import com.quickcards.domain.BackupContent
import com.quickcards.domain.BackupMeta
import com.quickcards.domain.BackupNamed
import com.quickcards.domain.BackupNetwork
import com.quickcards.domain.CardFieldMask
import com.quickcards.domain.Monogram
import com.quickcards.domain.PhoneCard
import com.quickcards.domain.PhoneSnapshot
import com.quickcards.domain.SnapshotItem
import java.time.LocalDate
import java.util.Locale
import kotlinx.coroutines.flow.first

/** Builds the format 2 document from the vault and, optionally, the settings. */
object BackupSnapshot {

    suspend fun capture(
        repository: VaultRepository,
        settings: AppSettings,
        includeSettings: Boolean,
        version: String,
        exportedAt: String = today()
    ): BackupContent {
        val holders = repository.holders.first()
        val banks = repository.banks.first()
        val networks = repository.networks.first()
        val types = repository.cardTypes.first()
        val tags = repository.tags.first()
        val cards = repository.cards.first()

        return BackupContent(
            meta = BackupMeta(
                app = BACKUP_APP_NAME,
                format = BACKUP_FORMAT,
                version = version,
                exportedAt = exportedAt
            ),
            holders = holders.map { BackupNamed(it.id, it.name) },
            banks = banks.map { BackupBank(it.id, it.name, Monogram.monoFor(it.name)) },
            types = types.map { BackupNamed(it.id, it.name) },
            tags = tags.map { BackupNamed(it.id, it.name) },
            networks = networks.map { network ->
                BackupNetwork(
                    id = network.id,
                    name = network.name,
                    code = network.code,
                    prefixes = network.prefixes,
                    lengths = network.lengths,
                    cvv = network.cvvLength,
                    luhn = network.luhn,
                    groups = network.groups
                )
            },
            cards = cards.map { card ->
                val holderName = card.holderId
                    ?.let { id -> holders.firstOrNull { it.id == id }?.name }
                    .orEmpty()
                BackupCard(
                    id = card.id,
                    number = card.number,
                    exp = card.expiry,
                    cvv = card.cvv,
                    nameOnCard = holderName.uppercase(Locale.ROOT),
                    holderId = card.holderId,
                    bankId = card.bankId,
                    networkAuto = card.networkAuto,
                    networkId = if (card.networkAuto) null else card.networkId,
                    typeId = card.typeId,
                    variant = card.cardName,
                    note = card.note,
                    color = card.bandColor,
                    tagIds = card.tagIds,
                    sortOrder = card.sortOrder,
                    pinned = card.pinned,
                    pinnedAt = card.pinnedAt,
                    archived = card.archived,
                    archivedAt = card.archivedAt,
                    archiveReason = card.archiveReason.orEmpty(),
                    needsReview = CardFieldMask.encode(card.needsReview)
                )
            },
            settings = if (includeSettings) settings.toMap() else null
        )
    }

    suspend fun phoneSnapshot(repository: VaultRepository): PhoneSnapshot {
        val holders = repository.holders.first()
        val banks = repository.banks.first()
        val networks = repository.networks.first()
        val types = repository.cardTypes.first()
        val tags = repository.tags.first()
        val cards = repository.cards.first()

        return PhoneSnapshot(
            holders = holders.map { SnapshotItem(it.id, it.name) },
            banks = banks.map { SnapshotItem(it.id, it.name) },
            networks = networks.map { SnapshotItem(it.id, it.name) },
            types = types.map { SnapshotItem(it.id, it.name) },
            tags = tags.map { SnapshotItem(it.id, it.name) },
            cards = cards.map { card ->
                val holderName = card.holderId
                    ?.let { id -> holders.firstOrNull { it.id == id }?.name }
                    .orEmpty()
                PhoneCard(
                    id = card.id,
                    number = card.number,
                    holderId = card.holderId,
                    bankId = card.bankId,
                    networkAuto = card.networkAuto,
                    networkId = card.networkId,
                    typeId = card.typeId,
                    variant = card.cardName,
                    exp = card.expiry,
                    cvv = card.cvv,
                    nameOnCard = holderName.uppercase(Locale.ROOT),
                    color = card.bandColor,
                    note = card.note,
                    tagIds = card.tagIds,
                    pinned = card.pinned,
                    pinnedAt = card.pinnedAt,
                    archived = card.archived,
                    archivedAt = card.archivedAt,
                    archiveReason = card.archiveReason.orEmpty()
                )
            }
        )
    }

    fun fileName(date: LocalDate = LocalDate.now()): String = "quickcards-backup-$date.qcb"

    fun today(): String = LocalDate.now().toString()
}
