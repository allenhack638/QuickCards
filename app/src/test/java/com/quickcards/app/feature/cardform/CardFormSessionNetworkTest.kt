package com.quickcards.app.feature.cardform

import com.quickcards.app.core.data.model.VaultCard
import com.quickcards.domain.CardNetwork
import com.quickcards.domain.Field
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    /** H2 open question 1: the CVV cap and the CVV check follow the network the form shows. */
    private val amexLike = CardNetwork("store4", "Store", "STORE", "", "16", 4, true)

    private fun storeSession() = CardFormSession.forEdit(
        VaultCard(number = "4111111111111111", expiry = "12/29", cvv = "1234", networkId = "store4", networkAuto = false),
        CardFormLists(networks = listOf(visa, mc, amexLike)),
        CardFormOptions()
    )

    @Test
    fun theCvvCapFollowsTheStoredNetwork() {
        val session = storeSession().updateCvv("4321")

        assertEquals("4321", session.form.cvv)
    }

    @Test
    fun theCvvCheckFollowsTheStoredNetwork() {
        val session = storeSession().updateCvv("4321").validate(Field.CVV)

        assertNull(session.form.errors[Field.CVV])
    }
}
