package com.quickcards.app.core.security

import android.content.SharedPreferences
import com.quickcards.app.core.data.dao.CardDao

/**
 * Detects a lost Keystore key via an encrypted sentinel. A missing sentinel is armed only if
 * the vault is empty or a card still decrypts, so a lost-key vault never looks healthy.
 */
class KeySentinel(
    private val prefs: SharedPreferences,
    private val cardDao: CardDao,
    private val encryption: () -> EncryptionHelper = { EncryptionHelper.getInstance() }
) {

    suspend fun isKeyAvailable(): Boolean {
        val helper = runCatching { encryption() }.getOrNull() ?: return false
        val stored = prefs.getString(PREF_KEY, null)
        if (!stored.isNullOrBlank()) {
            return runCatching { helper.decrypt(stored) == SENTINEL_VALUE }.getOrDefault(false)
        }

        val cards = runCatching { cardDao.getAllCardsSync() }.getOrNull() ?: return false
        if (cards.isNotEmpty() && cards.none { runCatching { helper.decrypt(it.cardNumber) }.isSuccess }) {
            return false
        }

        return runCatching {
            prefs.edit().putString(PREF_KEY, helper.encrypt(SENTINEL_VALUE)).commit()
        }.getOrDefault(false)
    }

    companion object {
        const val PREFS_NAME = "quickcards_security"
        const val PREF_KEY = "key_check_v1"
        const val SENTINEL_VALUE = "quickcards-key-check-v1"
    }
}
