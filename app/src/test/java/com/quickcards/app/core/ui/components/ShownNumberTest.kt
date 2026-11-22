package com.quickcards.app.core.ui.components

import com.quickcards.app.core.data.model.VaultCard
import com.quickcards.app.core.ui.HomeCard
import com.quickcards.domain.CardNetwork
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShownNumberTest {

    private fun card(number: String, network: CardNetwork) = HomeCard(
        card = VaultCard(id = "c", number = number, expiry = "12/29", cvv = "482"),
        bankName = "HDFC Bank",
        holderName = "Aarav Mehta",
        typeName = "Credit",
        network = network,
        networkId = network.id,
        tagNames = emptyList()
    )

    private val visa = CardNetwork("visa", "Visa", "VISA", "4", "16", 3, true)
    private val amex = CardNetwork("amex", "American Express", "AMEX", "34,37", "15", 4, true, groups = "4-6-5")

    @Test
    fun hideCardNumbersOnMasks() {
        assertNull(shownNumber(card("4532718022990366", visa), maskNumber = true))
    }

    @Test
    fun hideCardNumbersOffShowsTheGroupedNumber() {
        assertEquals("4532 7180 2299 0366", shownNumber(card("4532718022990366", visa), maskNumber = false))
    }

    @Test
    fun groupingFollowsTheNetwork() {
        assertEquals("3782 822463 10005", shownNumber(card("378282246310005", amex), maskNumber = false))
    }
}
