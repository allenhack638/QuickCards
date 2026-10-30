package com.quickcards.app.core.data.model

import com.quickcards.domain.DefaultNetworks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SeedDataTest {

    @Test
    fun builtInNetworksMirrorTheDomainRulesInOrder() {
        assertEquals(DefaultNetworks.all, SeedData.networks.map { it.toDomain() })
        assertEquals(DefaultNetworks.all.indices.toList(), SeedData.networks.map { it.sortOrder })
        assertTrue(SeedData.networks.all { it.builtIn })
    }
}
