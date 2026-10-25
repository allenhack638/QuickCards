package com.quickcards.domain

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EmvCardTest {

    private val visaAid = "A0000000031010"

    @Test
    fun ppseListsApplicationsInOrder() {
        // 6F (FCI) > A5 > BF0C > 61 { 4F aid, 87 priority } x2
        val fci = EmvCard.fromHex(
            "6F 2A 84 0E 32 50 41 59 2E 53 59 53 2E 44 44 46 30 31 A5 18 BF 0C 15 " +
                "61 09 4F 07 A0 00 00 00 03 10 10 61 08 4F 06 A0 00 00 00 04 10"
        )
        val aids = EmvCard.aidsFromPpse(fci).map(EmvCard::toHex)
        assertEquals(listOf(visaAid.lowercase(), "a00000000410"), aids)
    }

    @Test
    fun gpoDataAnswersThePdol() {
        // 9F38 (PDOL) asks for 9F66 (4 bytes) and 9F02 (6 bytes)
        val fci = EmvCard.fromHex("6F 0E A5 0C 9F 38 06 9F 66 04 9F 02 06 BF 0C 00")
        val data = EmvCard.gpoData(fci)
        assertArrayEquals(EmvCard.fromHex("83 0A 36 00 00 00 00 00 00 00 00 01"), data)
    }

    @Test
    fun gpoWithoutPdolIsEmpty() {
        assertArrayEquals(EmvCard.fromHex("83 00"), EmvCard.gpoData(EmvCard.fromHex("6F 02 A5 00")))
    }

    @Test
    fun aflIsReadFromEitherGpoFormat() {
        val template77 = EmvCard.fromHex("77 0E 82 02 20 00 94 08 08 01 01 00 10 01 02 00")
        assertEquals(listOf(Triple(1, 1, 1), Triple(2, 1, 2)), EmvCard.aflEntries(template77))
        val format80 = EmvCard.fromHex("80 06 20 00 08 01 01 00")
        assertEquals(listOf(Triple(1, 1, 1)), EmvCard.aflEntries(format80))
    }

    @Test
    fun track2GivesNumberAndExpiry() {
        // 70 { 57 track2: PAN 4532778103492211, 'D', YYMM 2912 }
        val record = EmvCard.fromHex("70 14 57 12 45 32 77 81 03 49 22 11 D2 91 22 01 00 00 00 00 00 0F")
        val data = EmvCard.extract(record)
        assertEquals("4532778103492211", data?.number)
        assertEquals("12/29", data?.expiry)
    }

    @Test
    fun panAndExpiryTagsAreAFallback() {
        // 70 { 5A PAN (padded F), 5F24 expiry YYMMDD 29 12 31 }
        val record = EmvCard.fromHex("70 10 5A 08 45 32 77 81 03 49 22 11 5F 24 03 29 12 31")
        val data = EmvCard.extract(record)
        assertEquals("4532778103492211", data?.number)
        assertEquals("12/29", data?.expiry)
    }

    @Test
    fun recordsWithoutCardDataAreIgnored() {
        assertNull(EmvCard.extract(EmvCard.fromHex("70 04 9F 08 01 02")))
        assertNull(EmvCard.extract(EmvCard.fromHex("70 0C 57 0A 12 34 56 78 90 12 34 D2 91 2F")))
    }
}
