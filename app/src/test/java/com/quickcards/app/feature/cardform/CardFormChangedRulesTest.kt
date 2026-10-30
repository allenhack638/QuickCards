package com.quickcards.app.feature.cardform

import com.quickcards.domain.DefaultNetworks
import com.quickcards.domain.Field
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CardFormChangedRulesTest {

    private val standard = DefaultNetworks.all
    private val visaFourDigits = standard.map { if (it.id == "visa") it.copy(cvvLength = 4) else it }

    private fun session(networks: List<com.quickcards.domain.CardNetwork>) =
        CardFormSession.forAdd(CardFormLists(networks = networks), CardFormOptions())
            .updateNumber("4111111111111111")

    @Test
    fun aChangedVisaRuleThatBlocksTheCvvIsNamed() {
        val blocked = session(visaFourDigits).updateCvv("123").cvvBlur()

        assertTrue(Field.CVV in blocked.form.errors)
        assertEquals("visa", blocked.changedRulesBlocking()?.network?.id)
        assertEquals(BlockingRule.Kind.CVV, blocked.changedRulesBlocking()?.kind)
    }

    @Test
    fun resettingTheRuleClearsTheErrorAndTheNotice() {
        val blocked = session(visaFourDigits).updateCvv("123").cvvBlur()

        val reset = blocked.withLists(blocked.lists.copy(networks = standard), blocked.options)

        assertFalse(Field.CVV in reset.form.errors)
        assertNull(reset.changedRulesBlocking())
    }

    @Test
    fun aChangedRuleWithoutAnErrorShowsNoNotice() {
        val fine = session(visaFourDigits).updateCvv("1234").cvvBlur()

        assertNull(fine.changedRulesBlocking())
    }

    @Test
    fun aTypoOnANetworkWithChangedRulesShowsNoNotice() {
        val typo = session(visaFourDigits).updateNumber("4111111111111112").numberBlur()

        assertTrue(Field.NUMBER in typo.form.errors)
        assertNull(typo.changedRulesBlocking())
    }

    @Test
    fun maestroSavesWithoutACvv() {
        val maestro = CardFormSession.forAdd(CardFormLists(networks = standard), CardFormOptions())
            .updateNumber("6759649826438453")
            .saveAttempt()

        assertFalse(Field.CVV in maestro.form.errors)
        assertTrue(Field.EXPIRY in maestro.form.errors)
    }
}
