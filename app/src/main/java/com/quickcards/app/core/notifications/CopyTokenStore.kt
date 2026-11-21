package com.quickcards.app.core.notifications

import android.os.SystemClock
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

enum class CopyField { EXPIRY, CVV }

/**
 * Holds the one live copy notification's expiry and CVV in memory only, behind a random token,
 * so its actions carry the token and the field, never a value. The entry dies with the
 * notification's timeout, a new copy, [clear] or the process.
 */
class CopyTokenStore(
    private val now: () -> Long = SystemClock::elapsedRealtime,
    private val random: SecureRandom = SecureRandom()
) {

    private class Entry(
        val token: ByteArray,
        val title: String,
        val expiry: String,
        val cvv: String,
        val expiresAt: Long
    )

    class Value(val title: String, val text: String)

    @Volatile
    private var entry: Entry? = null

    fun issue(title: String, expiry: String, cvv: String, ttlMillis: Long): String {
        val token = ByteArray(TOKEN_BYTES).also(random::nextBytes)
        entry = Entry(token, title, expiry, cvv, now() + ttlMillis)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(token)
    }

    fun value(token: String?, field: CopyField): Value? {
        val current = entry ?: return null
        if (now() >= current.expiresAt) {
            clear()
            return null
        }
        val given = token?.let { runCatching { Base64.getUrlDecoder().decode(it) }.getOrNull() } ?: return null
        if (!MessageDigest.isEqual(given, current.token)) return null
        val text = when (field) {
            CopyField.EXPIRY -> current.expiry
            CopyField.CVV -> current.cvv
        }
        return text.takeIf { it.isNotBlank() }?.let { Value(current.title, it) }
    }

    fun clear() {
        entry = null
    }

    private companion object {
        const val TOKEN_BYTES = 16
    }
}
