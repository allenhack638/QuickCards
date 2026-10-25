package com.quickcards.app.core.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "networks")
data class CardNetwork(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val code: String,
    val prefixes: String,
    val lengths: String,
    val cvvLength: Int = 3,
    val luhn: Boolean = true,
    @ColumnInfo(defaultValue = "''")
    val groups: String = "",
    val sortOrder: Int = 0,
    val builtIn: Boolean = true
)
