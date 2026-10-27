package com.quickcards.app.feature.cardform

import com.quickcards.app.core.data.model.VaultCard
import com.quickcards.domain.CardNetwork
import org.junit.Assert.assertEquals
import org.junit.Test

class CardFormSessionNetworkTest {

    private val visa = CardNetwork("visa", "Visa", "VISA", "4", "16", 3, true)
    private val mc = CardNetwork("mc", "Mastercard", "MC", "51-55", "16", 3, true)
    private val custom = CardNetwork("custom", "Custom", "CUST", "", "16", 3, true)

    @Test
    fun anUnchangedNumberKeepsTheStoredNetwork() {
        val card = VaultCard(
            number = "4111111111111111",
            networkId = "custom",
            networkAuto = false
        )
        val session = CardFormSession.forEdit(card, CardFormLists(networks = listOf(visa, mc, custom)), CardFormOptions())

        assertEquals("custom", session.network(session.lists.networks)?.id)
    }

    @Test
    fun aNumberEditFallsBackToDetection() {
        val card = VaultCard(
            number = "4111111111111111",
            networkId = "custom",
            networkAuto = false
        )
        val session = CardFormSession.forEdit(card, CardFormLists(networks = listOf(visa, mc, custom)), CardFormOptions())
            .updateNumber("5500000000000004")

        assertEquals("mc", session.network(session.lists.networks)?.id)
    }
}
