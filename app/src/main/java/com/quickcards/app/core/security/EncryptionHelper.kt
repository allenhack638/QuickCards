package com.quickcards.app.core.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.annotation.VisibleForTesting
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Envelope encryption. A hardware-backed Keystore key (the KEK) wraps one random AES-256 data
 * key (the DEK); fields are encrypted with the DEK in software, so a decrypt costs microseconds
 * instead of a Keystore round-trip. The DEK is unwrapped once per process and never stored
 * unwrapped. Values written before 2.1 (no `v2:` prefix) still decrypt through the KEK directly
 * and are re-wrapped by [com.quickcards.app.core.data.repository.DataMaintenance].
 */
class EncryptionHelper private constructor(private val prefs: SharedPreferences) {

    companion object {
        private const val KEYSTORE_ALIAS = "QuickCardsSecretKey"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val AES_MODE = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 16
        private const val DEK_BYTES = 32
        private const val METADATA_PREFIX = "md1:"
        private const val ENVELOPE_PREFIX = "v2:"
        const val PREFS_NAME = "quickcards_vault_key"
        const val PREF_WRAPPED_DEK = "dek_v1"

        @Volatile
        private var INSTANCE: EncryptionHelper? = null

        @Volatile
        private var appContext: Context? = null

        /** Called once from the Application before anything touches the vault. */
        fun init(context: Context) {
            appContext = context.applicationContext
        }

        fun getInstance(): EncryptionHelper {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: run {
                    val context = checkNotNull(appContext) { "EncryptionHelper.init was not called" }
                    EncryptionHelper(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE))
                        .also { INSTANCE = it }
                }
            }
        }

        fun isEnvelope(value: String): Boolean = value.startsWith(ENVELOPE_PREFIX)
    }

    private val random = SecureRandom()

    @Volatile
    private var keystoreKey: SecretKey? = null

    @Volatile
    private var dataKey: SecretKey? = null

    init {
        generateSecretKey()
    }

    private fun generateSecretKey() {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)

            if (!keyStore.containsAlias(KEYSTORE_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
                val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                    KEYSTORE_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setUserAuthenticationRequired(false)
                    .build()

                keyGenerator.init(keyGenParameterSpec)
                keyGenerator.generateKey()
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to generate secret key", e)
        }
    }

    private fun keystoreKey(): SecretKey {
        keystoreKey?.let { return it }
        return synchronized(this) {
            keystoreKey ?: run {
                val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
                keyStore.load(null)
                (keyStore.getKey(KEYSTORE_ALIAS, null) as SecretKey).also { keystoreKey = it }
            }
        }
    }

    /**
     * The in-memory data key. A stored wrapper that no longer unwraps means the Keystore key is
     * gone; a fresh key is never generated in that case, so the vault is reported unreadable
     * instead of silently restarting empty.
     */
    private fun dataKey(): SecretKey {
        dataKey?.let { return it }
        return synchronized(this) {
            dataKey ?: run {
                val wrapped = prefs.getString(PREF_WRAPPED_DEK, null)
                val raw = if (wrapped.isNullOrBlank()) {
                    ByteArray(DEK_BYTES).also { random.nextBytes(it) }.also { bytes ->
                        val wrap = keystoreEncrypt(Base64.encodeToString(bytes, Base64.NO_WRAP))
                        check(prefs.edit().putString(PREF_WRAPPED_DEK, wrap).commit()) {
                            "Failed to store the wrapped data key"
                        }
                    }
                } else {
                    Base64.decode(keystoreDecrypt(wrapped), Base64.NO_WRAP)
                }
                SecretKeySpec(raw, KeyProperties.KEY_ALGORITHM_AES).also { dataKey = it }
            }
        }
    }

    fun encrypt(plaintext: String): String {
        try {
            return ENVELOPE_PREFIX + seal(plaintext, dataKey())
        } catch (e: Exception) {
            throw RuntimeException("Failed to encrypt data", e)
        }
    }

    fun decrypt(encryptedText: String): String {
        try {
            return if (encryptedText.startsWith(ENVELOPE_PREFIX)) {
                open(encryptedText.removePrefix(ENVELOPE_PREFIX), dataKey())
            } else {
                keystoreDecrypt(encryptedText)
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to decrypt data", e)
        }
    }

    /** Re-wraps a legacy value under the data key; envelope values are returned unchanged. */
    fun reencrypt(value: String): String =
        if (value.isBlank() || isEnvelope(value)) value else encrypt(decrypt(value))

    fun encryptMetadata(plaintext: String): String {
        if (plaintext.isBlank() || plaintext.startsWith(METADATA_PREFIX)) return plaintext
        return METADATA_PREFIX + encrypt(plaintext)
    }

    fun decryptMetadata(value: String): String {
        if (value.isBlank() || !value.startsWith(METADATA_PREFIX)) return value
        return decrypt(value.removePrefix(METADATA_PREFIX))
    }

    fun reencryptMetadata(value: String): String {
        if (!value.startsWith(METADATA_PREFIX)) return value
        val inner = value.removePrefix(METADATA_PREFIX)
        return if (isEnvelope(inner)) value else METADATA_PREFIX + encrypt(decrypt(inner))
    }

    private fun keystoreEncrypt(plaintext: String): String = seal(plaintext, keystoreKey())

    /** Produces a pre-2.1 value (Keystore key, no prefix) so upgrade tests can seed legacy rows. */
    @VisibleForTesting
    fun encryptLegacy(plaintext: String): String = keystoreEncrypt(plaintext)

    private fun keystoreDecrypt(encryptedText: String): String = open(encryptedText, keystoreKey())

    private fun seal(plaintext: String, key: SecretKey): String {
        val cipher = Cipher.getInstance(AES_MODE)
        if (key is SecretKeySpec) {
            val iv = ByteArray(GCM_IV_LENGTH).also { random.nextBytes(it) }
            cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH * 8, iv))
        } else {
            cipher.init(Cipher.ENCRYPT_MODE, key)
        }
        val iv = cipher.iv
        val encryptedData = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(iv.size + encryptedData.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encryptedData, 0, combined, iv.size, encryptedData.size)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun open(encryptedText: String, key: SecretKey): String {
        val combined = Base64.decode(encryptedText, Base64.NO_WRAP)
        if (combined.size < GCM_IV_LENGTH + GCM_TAG_LENGTH) {
            throw IllegalArgumentException("Invalid encrypted data length")
        }
        val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
        val encryptedData = combined.copyOfRange(GCM_IV_LENGTH, combined.size)
        val cipher = Cipher.getInstance(AES_MODE)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH * 8, iv))
        return String(cipher.doFinal(encryptedData), Charsets.UTF_8)
    }
}
