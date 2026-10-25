package com.quickcards.app.core.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "card_types")
data class CardType(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val sortOrder: Int = 0
)
