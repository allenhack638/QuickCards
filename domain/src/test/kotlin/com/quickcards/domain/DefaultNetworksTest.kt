package com.quickcards.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultNetworksTest {

    private val visa = DefaultNetworks.standard("visa")!!

    @Test
    fun `standard networks have no changed rules`() {
        DefaultNetworks.all.forEach { network ->
            assertEquals(network.id, emptySet<NetworkRule>(), DefaultNetworks.changedRules(network))
        }
    }

    @Test
    fun `a Visa set to a 4 digit CVV reports the CVV rule`() {
        assertEquals(setOf(NetworkRule.CVV), DefaultNetworks.changedRules(visa.copy(cvvLength = 4)))
    }

    @Test
    fun `every rule is compared and spacing is ignored`() {
        val changed = visa.copy(prefixes = "4, 5", lengths = "16", luhn = false)
        assertEquals(
            setOf(NetworkRule.PREFIXES, NetworkRule.LENGTHS, NetworkRule.LUHN),
            DefaultNetworks.changedRules(changed)
        )
        assertTrue(DefaultNetworks.changedRules(visa.copy(lengths = "13, 16, 19")).isEmpty())
    }

    @Test
    fun `custom networks never report changes`() {
        val custom = visa.copy(id = "custom-1", cvvLength = 4)
        assertTrue(DefaultNetworks.changedRules(custom).isEmpty())
    }

    @Test
    fun `only Maestro has an optional CVV`() {
        assertTrue(DefaultNetworks.cvvOptional(DefaultNetworks.standard("maestro")))
        DefaultNetworks.all.filter { it.id != "maestro" }.forEach {
            assertFalse(it.id, DefaultNetworks.cvvOptional(it))
        }
        assertFalse(DefaultNetworks.cvvOptional(null))
    }

    @Test
    fun `CVV lengths match the published scheme rules`() {
        DefaultNetworks.all.forEach { network ->
            val expected = if (network.id == "amex") 4 else 3
            assertEquals(network.id, expected, network.cvvLength)
        }
    }
}
