package com.quickcards.app.core.security

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.quickcards.app.core.data.database.QuickCardsDatabase
import com.quickcards.app.core.data.model.Card
import com.quickcards.app.feature.cards.HomeViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import javax.crypto.Cipher
import javax.crypto.KeyGenerator

@RunWith(AndroidJUnit4::class)
class KeySentinelTest {

    private lateinit var application: Application
    private lateinit var database: QuickCardsDatabase
    private lateinit var prefs: SharedPreferences
    private lateinit var sentinel: KeySentinel
    private var originalSentinel: String? = null

    @Before
    fun setUp() {
        application = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as Application
        database = QuickCardsDatabase.getDatabase(application, CoroutineScope(Dispatchers.IO))
        prefs = application.getSharedPreferences(KeySentinel.PREFS_NAME, Context.MODE_PRIVATE)
        sentinel = KeySentinel(prefs, database.cardDao())
        originalSentinel = prefs.getString(KeySentinel.PREF_KEY, null)
        runBlocking { database.cardDao().deleteAllCards() }
    }

    @After
    fun tearDown() {
        runBlocking { database.cardDao().deleteAllCards() }
        if (originalSentinel == null) {
            prefs.edit().remove(KeySentinel.PREF_KEY).commit()
        } else {
            prefs.edit().putString(KeySentinel.PREF_KEY, originalSentinel).commit()
        }
    }

    @Test
    fun emptyVaultArmsSentinel() {
        prefs.edit().remove(KeySentinel.PREF_KEY).commit()
        assertTrue(runBlocking { sentinel.isKeyAvailable() })
        assertNotNull(prefs.getString(KeySentinel.PREF_KEY, null))
        assertTrue(runBlocking { sentinel.isKeyAvailable() })
    }

    @Test
    fun invalidSentinelMarksKeyUnavailable() {
        prefs.edit().putString(KeySentinel.PREF_KEY, "invalid-sentinel").commit()
        assertFalse(runBlocking { sentinel.isKeyAvailable() })
    }

    @Test
    fun sentinelEncryptedWithDifferentKeyMarksUnavailable() {
        val foreignKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, foreignKey)
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(KeySentinel.SENTINEL_VALUE.toByteArray(Charsets.UTF_8))
        prefs.edit()
            .putString(KeySentinel.PREF_KEY, Base64.encodeToString(iv + ciphertext, Base64.NO_WRAP))
            .commit()

        assertFalse(runBlocking { sentinel.isKeyAvailable() })
    }

    /** A vault written with a lost key must never re-arm the sentinel as healthy. */
    @Test
    fun missingSentinelWithUndecryptableCardsStaysUnavailable() {
        insertUndecryptableCard()
        prefs.edit().remove(KeySentinel.PREF_KEY).commit()

        assertFalse(runBlocking { sentinel.isKeyAvailable() })
        assertEquals(null, prefs.getString(KeySentinel.PREF_KEY, null))
    }

    @Test
    fun cardsScreenSurfacesKeyUnavailable() {
        prefs.edit().putString(KeySentinel.PREF_KEY, "invalid-sentinel").commit()
        var viewModel: HomeViewModel? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            viewModel = HomeViewModel(application)
        }
        val unavailable = runBlocking {
            withTimeout(15_000) { viewModel!!.keyUnavailable.first { it } }
        }
        assertTrue(unavailable)
    }

    private fun insertUndecryptableCard() = runBlocking {
        database.cardDao().insertCard(
            Card(
                cardNumber = "invalid-number",
                expiryDate = "invalid-expiry",
                cvv = "invalid-cvv",
                bankName = "Test Bank",
                cardType = "Credit"
            )
        )
    }
}
