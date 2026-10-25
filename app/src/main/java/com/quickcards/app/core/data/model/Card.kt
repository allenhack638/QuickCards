package com.quickcards.app.core.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "cards",
    indices = [Index(value = ["panHash"], unique = true)]
)
/**
 * cardNumber, expiryDate and cvv are Keystore-encrypted; bankName, owner, description,
 * nameOnCard and archiveReason use metadata encryption. Legacy text columns are still written.
 */
data class Card(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val cardNumber: String,
    val expiryDate: String,
    val cvv: String,
    val bankName: String,
    val cardType: String,
    val cardIssuer: String = "Visa",
    val cardVariant: String = "Standard",
    val owner: String = "me",
    val description: String = "",
    val cardColor: String = "#2196F3",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val panHash: String? = null,
    val holderId: String? = null,
    val bankId: String? = null,
    val networkId: String? = null,
    @ColumnInfo(defaultValue = "1")
    val networkAuto: Boolean = true,
    val typeId: String? = null,
    @ColumnInfo(defaultValue = "''")
    val nameOnCard: String = "",
    @ColumnInfo(defaultValue = "'navy'")
    val bandColor: String = "navy",
    @ColumnInfo(defaultValue = "0")
    val sortOrder: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val pinned: Boolean = false,
    val pinnedAt: Long? = null,
    @ColumnInfo(defaultValue = "0")
    val archived: Boolean = false,
    val archivedAt: Long? = null,
    val archiveReason: String? = null,
    @ColumnInfo(defaultValue = "0")
    val needsReview: Int = 0
)
