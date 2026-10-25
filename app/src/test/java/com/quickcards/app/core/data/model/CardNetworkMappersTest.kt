package com.quickcards.app.core.data.model

import com.quickcards.domain.CardNumberRules
import com.quickcards.domain.DefaultNetworks
import org.junit.Assert.assertEquals
import org.junit.Test

class CardNetworkMappersTest {

    /** Room seeds SeedData while JVM code and tests use DefaultNetworks; they must not drift. */
    @Test
    fun seededNetworksMatchDomainDefaults() {
        assertEquals(DefaultNetworks.all, SeedData.networks.map { it.toDomain() })
        assertEquals(SeedData.networks.indices.toList(), SeedData.networks.map { it.sortOrder })
    }

    @Test
    fun mapperCopiesEveryDetectionField() {
        val custom = CardNetwork(
            id = "custom",
            name = "Store card",
            code = "STORE",
            prefixes = "9900-9999",
            lengths = "16",
            cvvLength = 4,
            luhn = false,
            groups = "4-4-4-4",
            sortOrder = 12,
            builtIn = false
        )
        val domain = custom.toDomain()

        assertEquals("custom", domain.id)
        assertEquals("Store card", domain.name)
        assertEquals("STORE", domain.code)
        assertEquals("9900-9999", domain.prefixes)
        assertEquals("16", domain.lengths)
        assertEquals(4, domain.cvvLength)
        assertEquals(false, domain.luhn)
        assertEquals("4-4-4-4", domain.groups)
    }

    /** Network usage counts in VaultRepository rely on this longest-prefix detection. */
    @Test
    fun detectionOverSeededNetworksPrefersTheLongestPrefix() {
        val networks = SeedData.networks.map { it.toDomain() }
        assertEquals("visa", CardNumberRules.detect("4111111111111111", networks)?.id)
        assertEquals("mc", CardNumberRules.detect("2221000000000009", networks)?.id)
        assertEquals("rupay", CardNumberRules.detect("6521500000000000", networks)?.id)
        assertEquals("discover", CardNumberRules.detect("6531500000000000", networks)?.id)
        assertEquals("maestro", CardNumberRules.detect("6759000000000000", networks)?.id)
        assertEquals(null, CardNumberRules.detect("1234567890123456", networks)?.id)
    }
}
