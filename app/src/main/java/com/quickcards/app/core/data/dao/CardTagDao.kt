package com.quickcards.app.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quickcards.app.core.data.model.CardTag
import kotlinx.coroutines.flow.Flow

@Dao
interface CardTagDao {

    @Query("SELECT * FROM card_tags")
    fun getAllCardTags(): Flow<List<CardTag>>

    @Query("SELECT * FROM card_tags")
    suspend fun getAllCardTagsSync(): List<CardTag>

    @Query("SELECT tagId FROM card_tags WHERE cardId = :cardId")
    suspend fun getTagIdsForCard(cardId: String): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLinks(links: List<CardTag>)

    @Query("DELETE FROM card_tags WHERE cardId = :cardId")
    suspend fun deleteLinksForCard(cardId: String)

    @Query("DELETE FROM card_tags")
    suspend fun deleteAllLinks()
}
