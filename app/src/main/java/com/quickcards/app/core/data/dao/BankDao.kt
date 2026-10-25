package com.quickcards.app.core.data.dao

import androidx.room.*
import com.quickcards.app.core.data.model.Bank
import kotlinx.coroutines.flow.Flow

@Dao
interface BankDao {
    
    @Query("SELECT * FROM banks ORDER BY name ASC")
    fun getAllBanks(): Flow<List<Bank>>
    
    @Query("SELECT * FROM banks ORDER BY name ASC")
    suspend fun getAllBanksSync(): List<Bank>
    
    @Query("SELECT * FROM banks WHERE id = :id")
    suspend fun getBankById(id: String): Bank?

    @Query("SELECT * FROM banks WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun findBankByName(name: String): Bank?

    @Query("SELECT COUNT(*) FROM cards WHERE bankId = :id")
    suspend fun countCards(id: String): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBank(bank: Bank)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBanks(banks: List<Bank>)
    
    @Update
    suspend fun updateBank(bank: Bank)
    
    @Delete
    suspend fun deleteBank(bank: Bank)
    
    @Query("DELETE FROM banks")
    suspend fun deleteAllBanks()
}