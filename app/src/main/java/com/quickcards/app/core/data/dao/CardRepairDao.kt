package com.quickcards.app.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.quickcards.app.core.data.model.CardRepairEntity

@Dao
interface CardRepairDao {

    @Insert
    suspend fun insert(repair: CardRepairEntity): Long

    @Query(
        "SELECT * FROM card_repairs WHERE cardId = :cardId AND at >= :since " +
            "ORDER BY at DESC, id DESC"
    )
    suspend fun getRepairsForCard(cardId: String, since: Long): List<CardRepairEntity>

    @Query("DELETE FROM card_repairs WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM card_repairs WHERE at < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)

    @Query("DELETE FROM card_repairs")
    suspend fun deleteAll()
}
