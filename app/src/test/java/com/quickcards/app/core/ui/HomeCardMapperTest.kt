package com.quickcards.app.core.ui

import com.quickcards.app.core.data.model.Bank
import com.quickcards.app.core.data.model.CardNetwork
import com.quickcards.app.core.data.model.CardType
import com.quickcards.app.core.data.model.Holder
import com.quickcards.app.core.data.model.Tag
import com.quickcards.app.core.data.model.VaultCard
import com.quickcards.domain.ExpiryState
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeCardMapperTest {

    private val visa = CardNetwork("visa", "Visa", "VISA", "4", "16", 3, true)
    private val rupay = CardNetwork("rupay", "RuPay", "RUPAY", "60", "16", 3, true)
    private val mapper = HomeCardMapper(
        networks = listOf(visa, rupay),
        holders = listOf(Holder(id = "h", name = "Aarav Mehta")),
        banks = listOf(Bank(id = "b", name = "HDFC Bank")),
        types = listOf(CardType(id = "t", name = "Credit")),
        tags = listOf(Tag(id = "fuel", name = "Fuel")),
        bankFallback = "Card"
    )
    private val today = LocalDate.of(2026, 11, 22)

    @Test
    fun namesAndAnAutoNetworkComeFromTheLists() {
        val card = VaultCard(
            number = "4532718022990366",
            expiry = "12/29",
            holderId = "h",
            bankId = "b",
            typeId = "t",
            tagIds = listOf("fuel", "gone")
        )

        val mapped = mapper.map(card, today, soonMonths = 1)

        assertEquals("HDFC Bank", mapped.bankName)
        assertEquals("Aarav Mehta", mapped.holderName)
        assertEquals("Credit", mapped.typeName)
        assertEquals("visa", mapped.networkId)
        assertEquals(listOf("Fuel"), mapped.tagNames)
    }

    @Test
    fun aManualNetworkIsKeptEvenWhenTheNumberSaysOtherwise() {
        val card = VaultCard(number = "4532718022990366", networkId = "rupay", networkAuto = false)

        assertEquals("rupay", mapper.map(card, today, soonMonths = 1).networkId)
    }

    @Test
    fun missingNamesFallBack() {
        val mapped = mapper.map(VaultCard(number = "4532718022990366", bankId = "deleted"), today, soonMonths = 1)

        assertEquals("Card", mapped.bankName)
        assertEquals("", mapped.holderName)
        assertNull(mapped.typeName)
    }

    @Test
    fun theExpiryStateFollowsTheGivenDay() {
        val card = VaultCard(number = "4532718022990366", expiry = "11/26")

        assertEquals(ExpiryState.EXPIRES_SOON, mapper.map(card, today, soonMonths = 1).expiryState)
        assertEquals(ExpiryState.EXPIRED, mapper.map(card, today.plusDays(9), soonMonths = 1).expiryState)
    }
}
