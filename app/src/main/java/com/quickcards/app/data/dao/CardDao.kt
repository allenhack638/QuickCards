package com.quickcards.app.data.dao

import androidx.room.*
import com.quickcards.app.data.model.Card
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {
    
    @Query("SELECT * FROM cards ORDER BY updatedAt DESC")
    fun getAllCards(): Flow<List<Card>>
    
    @Query("SELECT * FROM cards ORDER BY updatedAt DESC")
    suspend fun getAllCardsSync(): List<Card>
    
    @Query("SELECT * FROM cards WHERE id = :id")
    suspend fun getCardById(id: String): Card?
    
    @Query("SELECT * FROM cards WHERE bankName LIKE '%' || :query || '%' " +
           "OR description LIKE '%' || :query || '%' " +
           "ORDER BY updatedAt DESC")
    fun searchCards(query: String): Flow<List<Card>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: Card)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<Card>)
    
    @Update
    suspend fun updateCard(card: Card)
    
    @Update
    suspend fun updateCards(cards: List<Card>)
    
    @Delete
    suspend fun deleteCard(card: Card)
    
    @Query("DELETE FROM cards WHERE id = :id")
    suspend fun deleteCardById(id: String)
    
    @Query("DELETE FROM cards")
    suspend fun deleteAllCards()
    
    @Query("SELECT COUNT(*) FROM cards")
    suspend fun getCardCount(): Int
}