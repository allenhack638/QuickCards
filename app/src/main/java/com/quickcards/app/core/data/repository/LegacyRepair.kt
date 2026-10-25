package com.quickcards.app.core.data.repository

import com.quickcards.app.core.data.database.QuickCardsDatabase
import com.quickcards.app.core.data.model.CardRepairEntity
import com.quickcards.app.core.data.model.toDomain
import com.quickcards.app.core.security.EncryptionHelper
import com.quickcards.app.core.security.PanHasher
import com.quickcards.domain.CardField
import com.quickcards.domain.CardFieldMask
import com.quickcards.domain.CardNetwork
import com.quickcards.domain.LegacyCardNormalizer
import java.util.concurrent.TimeUnit

/**
 * Normalizes pre-v2 cards and records each change so a fix can be undone for
 * 30 days. Runs once (guarded by [RepairSummaryStore.isCompleted]) and is
 * idempotent. Repair rows use full `encrypt` because both sides are card data;
 * nothing here logs a value.
 */
object LegacyRepair {

    const val WINDOW_DAYS = 30L

    internal fun windowStart(now: Long): Long =
        now - TimeUnit.DAYS.toMillis(WINDOW_DAYS)

    suspend fun run(
        database: QuickCardsDatabase,
        summaries: RepairSummaryStore,
        now: Long = System.currentTimeMillis(),
        encryption: EncryptionHelper = EncryptionHelper.getInstance()
    ): RepairSummary? {
        val backend = RoomLegacyRepairBackend(database, summaries, encryption)
        return LegacyRepairEngine(backend, EncryptionRepairCrypto(encryption)).run(now)
    }

    suspend fun undo(
        database: QuickCardsDatabase,
        summaries: RepairSummaryStore,
        cardId: String,
        field: CardField,
        now: Long = System.currentTimeMillis(),
        encryption: EncryptionHelper = EncryptionHelper.getInstance()
    ): Boolean {
        val backend = RoomLegacyRepairBackend(database, summaries, encryption)
        return LegacyRepairEngine(backend, EncryptionRepairCrypto(encryption))
            .undo(cardId, field, now)
    }

    /** Drops repair rows older than 30 days. Called at app start. */
    suspend fun prune(database: QuickCardsDatabase, now: Long = System.currentTimeMillis()) {
        database.cardRepairDao().deleteOlderThan(windowStart(now))
    }
}

internal data class RepairCardSnapshot(
    val id: String,
    val number: String,
    val expiry: String,
    val cvv: String,
    val needsReview: Int
)

internal data class RepairRecord(
    val id: Long,
    val cardId: String,
    val field: CardField,
    val before: String,
    val after: String,
    val at: Long
)

internal interface RepairCrypto {
    fun encrypt(value: String): String
    fun decrypt(value: String): String
}

internal class EncryptionRepairCrypto(
    private val encryption: EncryptionHelper
) : RepairCrypto {
    override fun encrypt(value: String): String = encryption.encrypt(value)
    override fun decrypt(value: String): String = encryption.decrypt(value)
}

/** Storage seam for [LegacyRepairEngine] so its semantics are JVM-testable. */
internal interface LegacyRepairBackend {
    suspend fun cards(): List<RepairCardSnapshot>
    suspend fun networks(): List<CardNetwork>
    suspend fun saveCard(card: RepairCardSnapshot, at: Long): Boolean
    suspend fun addRepair(
        cardId: String,
        field: CardField,
        before: String,
        after: String,
        at: Long
    )

    suspend fun repairsFor(cardId: String, since: Long): List<RepairRecord>
    suspend fun deleteRepair(id: Long)
    suspend fun prune(cutoff: Long)
    fun completed(): Boolean
    fun markCompleted()
    fun recordSummary(summary: RepairSummary)
}

internal class LegacyRepairEngine(
    private val backend: LegacyRepairBackend,
    private val crypto: RepairCrypto
) {

    suspend fun run(now: Long): RepairSummary? {
        if (backend.completed()) return null

        val networks = backend.networks()
        var fixedCards = 0
        var needsCheck = 0

        for (card in backend.cards()) {
            val normalized = LegacyCardNormalizer.normalize(
                card.number,
                card.expiry,
                card.cvv,
                networks
            )
            val mask = CardFieldMask.encode(normalized.needsReview)
            val updated = card.copy(
                number = normalized.number,
                expiry = normalized.expiry,
                cvv = normalized.cvv,
                needsReview = mask
            )
            if (updated == card) continue
            if (!backend.saveCard(updated, now)) continue

            if (normalized.fixed.isNotEmpty()) fixedCards++
            if (normalized.needsReview.isNotEmpty()) needsCheck++

            normalized.fixed.forEach { (field, values) ->
                backend.addRepair(
                    cardId = card.id,
                    field = field,
                    before = crypto.encrypt(values.first),
                    after = crypto.encrypt(values.second),
                    at = now
                )
            }
        }

        backend.prune(LegacyRepair.windowStart(now))
        backend.markCompleted()

        val summary = if (fixedCards > 0 || needsCheck > 0) {
            RepairSummary(fixedCards = fixedCards, needsCheck = needsCheck)
        } else {
            null
        }
        summary?.let(backend::recordSummary)
        return summary
    }

    suspend fun undo(cardId: String, field: CardField, now: Long): Boolean {
        val repair = backend.repairsFor(cardId, LegacyRepair.windowStart(now))
            .firstOrNull { it.field == field }
            ?: return false
        val card = backend.cards().firstOrNull { it.id == cardId } ?: return false
        val before = runCatching { crypto.decrypt(repair.before) }.getOrNull() ?: return false

        val restored = when (field) {
            CardField.NUMBER -> card.copy(number = before)
            CardField.EXPIRY -> card.copy(expiry = before)
            CardField.CVV -> card.copy(cvv = before)
        }
        val normalized = LegacyCardNormalizer.normalize(
            restored.number,
            restored.expiry,
            restored.cvv,
            backend.networks()
        )
        // The restored value is the pre-fix one by definition, so it needs a
        // check even when the normalizer could repair it again.
        val reviewFields = normalized.needsReview + field
        val updated = restored.copy(
            needsReview = CardFieldMask.encode(reviewFields)
        )
        if (!backend.saveCard(updated, now)) return false
        backend.deleteRepair(repair.id)
        return true
    }
}

internal class RoomLegacyRepairBackend(
    private val database: QuickCardsDatabase,
    private val summaries: RepairSummaryStore,
    private val encryption: EncryptionHelper
) : LegacyRepairBackend {

    private val cardDao = database.cardDao()
    private val repairDao = database.cardRepairDao()
    private val networkDao = database.networkDao()

    override suspend fun cards(): List<RepairCardSnapshot> =
        cardDao.getAllCardsSync().mapNotNull { card ->
            val number = runCatching { encryption.decrypt(card.cardNumber) }.getOrNull()
                ?: return@mapNotNull null
            val expiry = runCatching { encryption.decrypt(card.expiryDate) }.getOrNull()
                ?: return@mapNotNull null
            val cvv = runCatching { encryption.decrypt(card.cvv) }.getOrNull()
                ?: return@mapNotNull null
            RepairCardSnapshot(
                id = card.id,
                number = number,
                expiry = expiry,
                cvv = cvv,
                needsReview = card.needsReview
            )
        }

    override suspend fun networks(): List<CardNetwork> =
        networkDao.getAllNetworksSync().map { it.toDomain() }

    override suspend fun saveCard(card: RepairCardSnapshot, at: Long): Boolean {
        return try {
            val entity = cardDao.getCardById(card.id) ?: return false
            val hash = PanHasher.hash(card.number)
            val duplicate = cardDao.getCardByPanHash(hash)
            if (duplicate != null && duplicate.id != card.id) return false
            cardDao.updateCard(
                entity.copy(
                    cardNumber = encryption.encrypt(card.number),
                    expiryDate = encryption.encrypt(card.expiry),
                    cvv = encryption.encrypt(card.cvv),
                    needsReview = card.needsReview,
                    panHash = hash,
                    updatedAt = at
                )
            )
            true
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun addRepair(
        cardId: String,
        field: CardField,
        before: String,
        after: String,
        at: Long
    ) {
        repairDao.insert(
            CardRepairEntity(
                cardId = cardId,
                field = field.name,
                before = before,
                after = after,
                at = at
            )
        )
    }

    override suspend fun repairsFor(cardId: String, since: Long): List<RepairRecord> =
        repairDao.getRepairsForCard(cardId, since).mapNotNull { repair ->
            val field = CardField.entries.firstOrNull { it.name == repair.field }
                ?: return@mapNotNull null
            RepairRecord(
                id = repair.id,
                cardId = repair.cardId,
                field = field,
                before = repair.before,
                after = repair.after,
                at = repair.at
            )
        }

    override suspend fun deleteRepair(id: Long) {
        repairDao.deleteById(id)
    }

    override suspend fun prune(cutoff: Long) {
        repairDao.deleteOlderThan(cutoff)
    }

    override fun completed(): Boolean = summaries.isCompleted()

    override fun markCompleted() {
        summaries.markCompleted()
    }

    override fun recordSummary(summary: RepairSummary) {
        summaries.record(summary)
    }
}
