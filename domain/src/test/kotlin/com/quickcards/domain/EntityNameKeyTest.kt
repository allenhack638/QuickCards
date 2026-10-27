package com.quickcards.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class EntityNameKeyTest {

    @Test
    fun anOlderLongNameMatchesItsTruncatedForm() {
        val longName = "A".repeat(50)
        assertEquals(
            NetworkRuleSanitizer.entityKey(EntityKind.HOLDER, longName),
            NetworkRuleSanitizer.entityKey(EntityKind.HOLDER, "A".repeat(40))
        )
    }

    @Test
    fun aTagKeyIgnoresTheLeadingHashAndCase() {
        assertEquals(
            NetworkRuleSanitizer.entityKey(EntityKind.TAG, " #Travel "),
            NetworkRuleSanitizer.entityKey(EntityKind.TAG, "travel")
        )
    }

    @Test
    fun truncationNeverSplitsASurrogatePair() {
        val value = "a".repeat(39) + "😀" + "tail"
        val name = NetworkRuleSanitizer.entityName(EntityKind.HOLDER, value)

        assertEquals(40, name.codePointCount(0, name.length))
        assertFalse(name.last().isHighSurrogate())
    }
}
