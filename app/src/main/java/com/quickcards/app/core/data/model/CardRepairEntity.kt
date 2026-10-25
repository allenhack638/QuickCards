package com.quickcards.app.core.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One automatic legacy-repair entry. [before] and [after] are encrypted with
 * [com.quickcards.app.core.security.EncryptionHelper.encrypt] because both are
 * card data. Rows older than 30 days are pruned at app start.
 */
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
