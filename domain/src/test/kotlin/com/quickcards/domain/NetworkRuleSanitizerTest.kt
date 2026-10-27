package com.quickcards.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkRuleSanitizerTest {

    @Test
    fun aHugeLengthRangeIsClampedToCardLengths() {
        assertEquals((12..19).toList(), CardNumberRules.parseLengths("1-2147483647"))
        assertEquals("12,13,14,15,16,17,18,19", NetworkRuleSanitizer.lengths("1-99"))
        assertEquals("", NetworkRuleSanitizer.lengths("1-2147483647"))
    }

    @Test
    fun onlyRealCardLengthsAreValid() {
        assertTrue(NetworkRuleSanitizer.isValidLengths("13,16,19"))
        assertTrue(NetworkRuleSanitizer.isValidLengths("14-19"))
        assertFalse(NetworkRuleSanitizer.isValidLengths("1-2147483647"))
        assertFalse(NetworkRuleSanitizer.isValidLengths("16-20"))
        assertFalse(NetworkRuleSanitizer.isValidLengths("19-12"))
        assertFalse(NetworkRuleSanitizer.isValidLengths("11"))
        assertFalse(NetworkRuleSanitizer.isValidLengths(""))
    }

    @Test
    fun prefixesDropJunkAndAreBounded() {
        assertEquals("4,51-55", NetworkRuleSanitizer.prefixes("4, 51-55, abc, 123456789"))
        assertEquals("12-19", NetworkRuleSanitizer.prefixes("19-12"))
        assertEquals("", NetworkRuleSanitizer.prefixes("abc"))
        assertTrue(NetworkRuleSanitizer.prefixes("1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17").length <= 128)
    }

    @Test
    fun codesAreSafeForTheEditor() {
        assertEquals("VISA", NetworkRuleSanitizer.code("visa"))
        assertEquals("CARD", NetworkRuleSanitizer.code("!"))
        assertTrue(NetworkRuleSanitizer.code("a!b").matches(Regex("[A-Z0-9 ]{2,15}")))
    }

    @Test
    fun groupsAreKeptOnlyWhenTheyCoverASupportedLength() {
        assertEquals("4-6-5", NetworkRuleSanitizer.groups("4-6-5", "15"))
        assertEquals("", NetworkRuleSanitizer.groups("4-6-5", "16"))
        assertEquals("", NetworkRuleSanitizer.groups("4-6-x", "15"))
        assertEquals("", NetworkRuleSanitizer.groups("4-6-5-1", "15"))
    }

    @Test
    fun entityNamesAreBoundedAndNormalized() {
        assertEquals("Test Bank", NetworkRuleSanitizer.entityName(EntityKind.BANK, "  Test   Bank  "))
        assertEquals(
            EntityValidator.NETWORK_NAME_MAX,
            NetworkRuleSanitizer.entityName(EntityKind.NETWORK, "N".repeat(80)).length
        )
        assertEquals(
            EntityValidator.DEFAULT_TAG_MAX_LENGTH,
            NetworkRuleSanitizer.entityName(EntityKind.TAG, "#" + "t".repeat(80)).length
        )
    }
}
