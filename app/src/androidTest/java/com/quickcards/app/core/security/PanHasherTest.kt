package com.quickcards.app.core.security

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PanHasherTest {

    @Test
    fun formattedAndPlainPanHashTheSame() {
        assertEquals(
            PanHasher.hash("4111 1111 1111 1111"),
            PanHasher.hash("4111111111111111")
        )
    }

    @Test
    fun differentPansHashDifferently() {
        assertNotEquals(
            PanHasher.hash("4111111111111111"),
            PanHasher.hash("4111111111111112")
        )
    }

    @Test
    fun hashDoesNotExposePan() {
        val hash = PanHasher.hash("4111111111111111")
        assertFalse(hash.contains("4111"))
        assertFalse(hash.contains("4111111111111111"))
    }

    @Test
    fun nonAsciiDigitsAreNotNormalizedIntoAscii() {
        assertNotEquals(
            PanHasher.hash("4111111111111111"),
            PanHasher.hash("４１１１１１１１１１１１１１１")
        )
    }
}
