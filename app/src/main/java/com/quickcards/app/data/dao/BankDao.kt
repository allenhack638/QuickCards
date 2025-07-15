package com.quickcards.app.data.dao

import androidx.room.*
import com.quickcards.app.data.model.Bank
import kotlinx.coroutines.flow.Flow

@Dao
interface BankDao {
    
    @Query("SELECT * FROM banks ORDER BY name ASC")
    fun getAllBanks(): Flow<List<Bank>>
    
    @Query("SELECT * FROM banks ORDER BY name ASC")
    suspend fun getAllBanksSync(): List<Bank>
    
    @Query("SELECT * FROM banks WHERE id = :id")
    suspend fun getBankById(id: String): Bank?
    
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
    
    @Query("SELECT COUNT(*) FROM banks")
    suspend fun getBankCount(): Int
}