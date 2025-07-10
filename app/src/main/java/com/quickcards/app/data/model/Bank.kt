package com.quickcards.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "banks")
data class Bank(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun getDefaultBanks(): List<Bank> {
            return listOf(
                // Major Private Sector Banks
                Bank(name = "HDFC Bank"),
                Bank(name = "ICICI Bank"),
                Bank(name = "Axis Bank"),
                Bank(name = "Kotak Mahindra Bank"),
                Bank(name = "IndusInd Bank"),
                
                // Major Public Sector Banks
                Bank(name = "State Bank of India (SBI)"),
                Bank(name = "Punjab National Bank (PNB)"),
                Bank(name = "Bank of Baroda"),
                
                // Small Finance Banks / Regional Banks
                Bank(name = "AU Small Finance Bank"),
                Bank(name = "Bandhan Bank"),
                Bank(name = "Federal Bank"),
                Bank(name = "IDFC FIRST Bank"),
                
                // International Banks with strong presence in India
                Bank(name = "Citibank"),
                Bank(name = "Standard Chartered"),
                Bank(name = "HSBC")
            )
        }
    }
}