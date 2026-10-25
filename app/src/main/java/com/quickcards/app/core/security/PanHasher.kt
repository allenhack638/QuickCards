package com.quickcards.app.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey

object PanHasher {

    private const val KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "QuickCardsPanHashKey"
    private const val MAC_ALGORITHM = "HmacSHA256"

    @Volatile
    private var hmacKey: SecretKey? = null

    fun hash(cardNumber: String): String {
        val mac = Mac.getInstance(MAC_ALGORITHM)
        mac.init(getKey())
        val digest = mac.doFinal(normalize(cardNumber).toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(digest, Base64.NO_WRAP)
    }

    internal fun normalize(cardNumber: String): String = cardNumber.filter { it in '0'..'9' }

    private fun getKey(): SecretKey {
        hmacKey?.let { return it }
        synchronized(this) {
            hmacKey?.let { return it }
            val keyStore = KeyStore.getInstance(KEYSTORE)
            keyStore.load(null)
            val key = if (keyStore.containsAlias(KEY_ALIAS)) {
                keyStore.getKey(KEY_ALIAS, null) as SecretKey
            } else {
                val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, KEYSTORE)
                generator.init(
                    KeyGenParameterSpec.Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_SIGN
                    )
                        .setDigests(KeyProperties.DIGEST_SHA256)
                        .build()
                )
                generator.generateKey()
            }
            hmacKey = key
            return key
        }
    }
}
