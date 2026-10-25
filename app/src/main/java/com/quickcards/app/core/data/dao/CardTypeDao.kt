package com.quickcards.app.core.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.quickcards.app.core.data.model.CardType
import kotlinx.coroutines.flow.Flow

@Dao
interface CardTypeDao {

    @Query("SELECT * FROM card_types ORDER BY name ASC")
    fun getAllCardTypes(): Flow<List<CardType>>

    @Query("SELECT * FROM card_types ORDER BY name ASC")
    suspend fun getAllCardTypesSync(): List<CardType>

    @Query("SELECT * FROM card_types WHERE id = :id")
    suspend fun getCardTypeById(id: String): CardType?

    @Query("SELECT * FROM card_types WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun findCardTypeByName(name: String): CardType?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCardType(cardType: CardType)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCardTypes(cardTypes: List<CardType>)

    @Update
    suspend fun updateCardType(cardType: CardType)

    @Delete
    suspend fun deleteCardType(cardType: CardType)

    @Query("DELETE FROM card_types WHERE id = :id")
    suspend fun deleteCardTypeById(id: String)

    @Query("SELECT COUNT(*) FROM cards WHERE typeId = :id")
    suspend fun countCards(id: String): Int

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM card_types")
    suspend fun maxSortOrder(): Int

    @Query("DELETE FROM card_types")
    suspend fun deleteAllCardTypes()
}
