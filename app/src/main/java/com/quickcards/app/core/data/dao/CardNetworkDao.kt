package com.quickcards.app.core.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.quickcards.app.core.data.model.CardNetwork
import kotlinx.coroutines.flow.Flow

@Dao
interface CardNetworkDao {

    @Query("SELECT * FROM networks ORDER BY sortOrder ASC, name ASC")
    fun getAllNetworks(): Flow<List<CardNetwork>>

    @Query("SELECT * FROM networks ORDER BY sortOrder ASC, name ASC")
    suspend fun getAllNetworksSync(): List<CardNetwork>

    @Query("SELECT * FROM networks WHERE id = :id")
    suspend fun getNetworkById(id: String): CardNetwork?

    @Query("SELECT * FROM networks WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun findNetworkByName(name: String): CardNetwork?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNetwork(network: CardNetwork)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNetworks(networks: List<CardNetwork>)

    @Update
    suspend fun updateNetwork(network: CardNetwork)

    @Delete
    suspend fun deleteNetwork(network: CardNetwork)

    @Query("DELETE FROM networks WHERE id = :id")
    suspend fun deleteNetworkById(id: String)

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM networks")
    suspend fun maxSortOrder(): Int

    @Query("DELETE FROM networks")
    suspend fun deleteAllNetworks()
}
