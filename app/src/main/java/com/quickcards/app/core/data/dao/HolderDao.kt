package com.quickcards.app.core.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.quickcards.app.core.data.model.Holder
import kotlinx.coroutines.flow.Flow

@Dao
interface HolderDao {

    @Query("SELECT * FROM holders ORDER BY name ASC")
    fun getAllHolders(): Flow<List<Holder>>

    @Query("SELECT * FROM holders ORDER BY name ASC")
    suspend fun getAllHoldersSync(): List<Holder>

    @Query("SELECT * FROM holders WHERE id = :id")
    suspend fun getHolderById(id: String): Holder?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHolder(holder: Holder)

    @Update
    suspend fun updateHolder(holder: Holder)

    @Delete
    suspend fun deleteHolder(holder: Holder)

    @Query("DELETE FROM holders WHERE id = :id")
    suspend fun deleteHolderById(id: String)

    @Query("SELECT COUNT(*) FROM cards WHERE holderId = :id")
    suspend fun countCards(id: String): Int

    @Query("DELETE FROM holders")
    suspend fun deleteAllHolders()
}
