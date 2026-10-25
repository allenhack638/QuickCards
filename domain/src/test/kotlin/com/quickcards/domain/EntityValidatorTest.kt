package com.quickcards.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EntityValidatorTest {

    private fun networkDraft(
        name: String = "Visa Two",
        code: String = "VISA2",
        prefixes: String = "4",
        lengths: String = "16"
    ): EntityDraft = EntityDraft(
        kind = EntityKind.NETWORK,
        name = name,
        code = code,
        prefixes = prefixes,
        lengths = lengths
    )

    @Test
    fun everyKindRequiresAName() {
        for (kind in EntityKind.values()) {
            assertEquals(
                EntityValidationError.NameRequired,
                EntityValidator.validate(EntityDraft(kind, "   "))[EntityField.NAME]
            )
        }
    }

    @Test
    fun everyKindRequiresAtLeastTwoCharacters() {
        for (kind in EntityKind.values()) {
            assertEquals(
                EntityValidationError.NameTooShort,
                EntityValidator.validate(EntityDraft(kind, "A"))[EntityField.NAME]
            )
        }
    }

    @Test
    fun everyKindRejectsDuplicateNamesIgnoringCase() {
        for (kind in EntityKind.values()) {
            val result = EntityValidator.validate(
                EntityDraft(kind, "existing"),
                existing = listOf(EntityRef("Existing", id = "1"))
            )
            assertEquals(EntityValidationError.NameTaken, result[EntityField.NAME])
        }
    }

    @Test
    fun namesAreTrimmedAndSpacesCollapsed() {
        assertEquals("HDFC Bank", EntityValidator.normalizeName(EntityKind.BANK, "  HDFC   Bank  "))
        assertEquals("Aarav Mehta", EntityValidator.normalizeName(EntityKind.HOLDER, " Aarav  Mehta "))
        val result = EntityValidator.validate(
            EntityDraft(EntityKind.BANK, "  hdfc   bank "),
            existing = listOf(EntityRef("HDFC Bank"))
        )
        assertEquals(EntityValidationError.NameTaken, result[EntityField.NAME])
    }

    @Test
    fun editingTheSameEntityDoesNotCountAsADuplicate() {
        assertTrue(
            EntityValidator.validate(
                EntityDraft(EntityKind.TAG, "travel"),
                existing = listOf(EntityRef("Travel", id = "t1")),
                editingId = "t1"
            ).isEmpty()
        )
    }

    @Test
    fun tagsStripLeadingHashes() {
        assertEquals("Travel", EntityValidator.normalizeName(EntityKind.TAG, "  ##Travel  "))
        assertEquals("Travel", EntityValidator.normalizeName(EntityKind.TAG, "#Travel"))
        assertEquals("", EntityValidator.normalizeName(EntityKind.TAG, "###"))
        assertEquals(
            EntityValidationError.NameRequired,
            EntityValidator.validate(EntityDraft(EntityKind.TAG, "###"))[EntityField.NAME]
        )
        assertEquals(
            EntityValidationError.NameTooShort,
            EntityValidator.validate(EntityDraft(EntityKind.TAG, "##A"))[EntityField.NAME]
        )
        assertEquals(
            EntityValidationError.NameTaken,
            EntityValidator.validate(
                EntityDraft(EntityKind.TAG, "##Travel"),
                existing = listOf(EntityRef("travel"))
            )[EntityField.NAME]
        )
    }

    @Test
    fun nameLimitsMatchTheManagedLists() {
        assertEquals(40, EntityValidator.nameLimit(EntityKind.HOLDER))
        assertEquals(40, EntityValidator.nameLimit(EntityKind.BANK))
        assertEquals(24, EntityValidator.nameLimit(EntityKind.NETWORK))
        assertEquals(20, EntityValidator.nameLimit(EntityKind.TYPE))
        assertEquals(16, EntityValidator.nameLimit(EntityKind.TAG))
        assertEquals(8, EntityValidator.nameLimit(EntityKind.TAG, 8))
        assertEquals(12, EntityValidator.nameLimit(EntityKind.TAG, 12))
        assertEquals(16, EntityValidator.nameLimit(EntityKind.TAG, 16))
        assertEquals(24, EntityValidator.nameLimit(EntityKind.TAG, 24))
    }

    @Test
    fun aCompleteNetworkDraftIsValid() {
        assertTrue(EntityValidator.validate(networkDraft()).isEmpty())
    }

    @Test
    fun networkLabelsMustBeTwoToSevenUppercaseLettersOrDigits() {
        assertEquals(
            EntityValidationError.CodeFormat,
            EntityValidator.validate(networkDraft(code = "A"))[EntityField.CODE]
        )
        assertEquals(
            EntityValidationError.CodeFormat,
            EntityValidator.validate(networkDraft(code = "TOOLONG8"))[EntityField.CODE]
        )
        assertEquals(
            EntityValidationError.CodeFormat,
            EntityValidator.validate(networkDraft(code = "VISA!"))[EntityField.CODE]
        )
        assertFalse(EntityValidator.validate(networkDraft(code = "visa")).containsKey(EntityField.CODE))
    }

    @Test
    fun networkLabelsAreUnique() {
        val result = EntityValidator.validate(
            networkDraft(code = "visa"),
            existing = listOf(EntityRef("Visa", id = "n1", code = "VISA"))
        )
        assertEquals(EntityValidationError.CodeTaken, result[EntityField.CODE])
        assertTrue(
            EntityValidator.validate(
                networkDraft(name = "Visa", code = "VISA"),
                existing = listOf(EntityRef("Visa", id = "n1", code = "VISA")),
                editingId = "n1"
            ).isEmpty()
        )
    }

    @Test
    fun startingDigitsAcceptNumbersRangesAndCommas() {
        for (prefixes in listOf("", "abc", "51-55,", "51--55", "51-55;62")) {
            assertEquals(
                EntityValidationError.PrefixesFormat,
                EntityValidator.validate(networkDraft(prefixes = prefixes))[EntityField.PREFIXES]
            )
        }
        for (prefixes in listOf("4", "34,37", "51-55,2221-2720", " 34 , 37 ")) {
            assertFalse(EntityValidator.validate(networkDraft(prefixes = prefixes)).containsKey(EntityField.PREFIXES))
        }
    }

    @Test
    fun networkLengthsStayWithinTwelveToNineteen() {
        for (lengths in listOf("", "abc", "11", "20", "16-20", "19-12")) {
            assertEquals(
                EntityValidationError.LengthsRange,
                EntityValidator.validate(networkDraft(lengths = lengths))[EntityField.LENGTHS]
            )
        }
        for (lengths in listOf("16", "14-19", "13, 16, 19", "12-19")) {
            assertFalse(EntityValidator.validate(networkDraft(lengths = lengths)).containsKey(EntityField.LENGTHS))
        }
    }

    @Test
    fun networkFieldsOnlyApplyToNetworks() {
        for (kind in listOf(EntityKind.HOLDER, EntityKind.BANK, EntityKind.TYPE, EntityKind.TAG)) {
            val result = EntityValidator.validate(
                EntityDraft(kind = kind, name = "Valid Name", code = "", prefixes = "", lengths = "")
            )
            assertTrue(result.isEmpty())
        }
    }

    @Test
    fun aNameOfTwoCharactersIsAccepted() {
        assertNull(EntityValidator.validate(EntityDraft(EntityKind.HOLDER, "Jo"))[EntityField.NAME])
        assertNull(EntityValidator.validate(EntityDraft(EntityKind.BANK, "AU"))[EntityField.NAME])
    }
}
