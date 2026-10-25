package com.quickcards.app.core.data.dao

import androidx.room.*
import com.quickcards.app.core.data.model.Card
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {
    
    @Query("SELECT * FROM cards ORDER BY updatedAt DESC")
    fun getAllCards(): Flow<List<Card>>
    
    @Query("SELECT * FROM cards ORDER BY updatedAt DESC")
    suspend fun getAllCardsSync(): List<Card>

    @Query(
        "SELECT * FROM cards ORDER BY pinned DESC, " +
            "CASE WHEN pinned = 1 THEN pinnedAt ELSE 0 END ASC, sortOrder ASC, createdAt ASC"
    )
    fun getOrderedVaultCards(): Flow<List<Card>>

    @Query("SELECT * FROM cards ORDER BY sortOrder ASC, createdAt ASC")
    suspend fun getVaultCardsSync(): List<Card>

    @Query(
        "SELECT * FROM cards WHERE archived = 1 " +
            "ORDER BY archivedAt DESC, sortOrder ASC, createdAt ASC"
    )
    fun getArchivedVaultCards(): Flow<List<Card>>

    @Query(
        "UPDATE cards SET archived = 1, archivedAt = :at, archiveReason = :reason, " +
            "pinned = 0, pinnedAt = NULL, updatedAt = :at WHERE id = :id"
    )
    suspend fun archive(id: String, at: Long, reason: String?)

    @Query(
        "UPDATE cards SET archived = 0, archivedAt = NULL, archiveReason = NULL, " +
            "updatedAt = :at WHERE id = :id"
    )
    suspend fun unarchive(id: String, at: Long)

    @Query("SELECT * FROM cards WHERE pinned = 1 ORDER BY pinnedAt ASC")
    suspend fun getPinnedCardsSync(): List<Card>

    @Query("SELECT COALESCE(MAX(pinnedAt), 0) FROM cards WHERE pinned = 1")
    suspend fun maxPinnedAt(): Long

    @Query("UPDATE cards SET pinned = :pinned, pinnedAt = :pinnedAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setPinned(id: String, pinned: Boolean, pinnedAt: Long?, updatedAt: Long)

    @Query("SELECT * FROM cards WHERE holderId IS NULL")
    suspend fun getCardsNeedingBackfill(): List<Card>

    @Query("SELECT COUNT(*) FROM cards WHERE holderId IS NULL")
    suspend fun countCardsNeedingBackfill(): Int

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM cards")
    suspend fun maxSortOrder(): Int

    @Query("SELECT * FROM cards WHERE id = :id")
    suspend fun getCardById(id: String): Card?

    @Query("SELECT * FROM cards WHERE panHash = :hash LIMIT 1")
    suspend fun getCardByPanHash(hash: String): Card?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCard(card: Card)

    @Update
    suspend fun updateCard(card: Card)

    @Delete
    suspend fun deleteCard(card: Card)

    @Query("DELETE FROM cards WHERE id = :id")
    suspend fun deleteCardById(id: String)

    @Query("DELETE FROM cards")
    suspend fun deleteAllCards()
}