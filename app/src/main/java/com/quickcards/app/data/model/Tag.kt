package com.quickcards.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "tags")
data class Tag(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val color: String = "#2196F3", // Default blue color
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun getDefaultTags(): List<Tag> {
            return listOf(
                Tag(name = "Personal", color = "#2196F3"),
                Tag(name = "Business", color = "#FF9800"),
                Tag(name = "Travel", color = "#4CAF50"),
                Tag(name = "Shopping", color = "#E91E63"),
                Tag(name = "Emergency", color = "#F44336"),
                Tag(name = "Primary", color = "#9C27B0"),
                Tag(name = "Secondary", color = "#607D8B"),
                Tag(name = "Backup", color = "#795548")
            )
        }
    }
}