package com.quickcards.app.data.dao

import androidx.room.*
import com.quickcards.app.data.model.Tag
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    
    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<Tag>>
    
    @Query("SELECT * FROM tags WHERE id = :id")
    suspend fun getTagById(id: String): Tag?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: Tag)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTags(tags: List<Tag>)
    
    @Update
    suspend fun updateTag(tag: Tag)
    
    @Delete
    suspend fun deleteTag(tag: Tag)
    
    @Query("DELETE FROM tags")
    suspend fun deleteAllTags()
    
    @Query("SELECT COUNT(*) FROM tags")
    suspend fun getTagCount(): Int
}