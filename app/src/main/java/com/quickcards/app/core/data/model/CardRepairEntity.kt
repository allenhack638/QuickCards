package com.quickcards.app.core.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** [before] and [after] are card data, so they use full `encrypt`. Pruned after 30 days. */
@Entity(
    tableName = "card_repairs",
    foreignKeys = [
        ForeignKey(
            entity = Card::class,
            parentColumns = ["id"],
            childColumns = ["cardId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("cardId")]
)
data class CardRepairEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cardId: String,
    val field: String,
    val before: String,
    val after: String,
    val at: Long
)
