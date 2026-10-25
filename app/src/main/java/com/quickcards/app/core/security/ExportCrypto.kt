package com.quickcards.app.core.security

import com.google.gson.Gson
import com.google.gson.JsonParser
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object ExportCrypto {
    const val FORMAT = "quickcards-export"
    private const val VERSION = 1
    private const val KDF = "PBKDF2WithHmacSHA256"
    private const val CIPHER = "AES-256-GCM"
    private const val ITERATIONS = 600_000
    private const val KEY_BITS = 256
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val TAG_BITS = 128
    private const val MIN_ITERATIONS = 100_000
    private const val MAX_ITERATIONS = 600_000

    data class Envelope(
        val format: String,
        val version: Int,
        val kdf: String,
        val iterations: Int,
        val salt: String,
        val cipher: String,
        val iv: String,
        val ciphertext: String
    )

    fun isEnvelope(json: String): Boolean {
        return try {
            val root = JsonParser.parseString(json)
            root.isJsonObject && root.asJsonObject.get("format")?.asString == FORMAT
        } catch (_: Exception) {
            false
        } catch (_: StackOverflowError) {
            false
        }
    }

    fun encrypt(plaintext: String, passphrase: CharArray): String {
        val random = SecureRandom()
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val iv = ByteArray(IV_BYTES).also(random::nextBytes)
        val saltB64 = Base64.getEncoder().encodeToString(salt)
        val ivB64 = Base64.getEncoder().encodeToString(iv)
        val key = deriveKey(passphrase, salt, ITERATIONS)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        cipher.updateAAD(aad(saltB64, ivB64, ITERATIONS))
        val plaintextBytes = plaintext.toByteArray(Charsets.UTF_8)
        val ciphertext = try {
            cipher.doFinal(plaintextBytes)
        } finally {
            plaintextBytes.fill(0)
        }
        val envelope = Envelope(
            format = FORMAT,
            version = VERSION,
            kdf = KDF,
            iterations = ITERATIONS,
            salt = saltB64,
            cipher = CIPHER,
            iv = ivB64,
            ciphertext = Base64.getEncoder().encodeToString(ciphertext)
        )
        return Gson().toJson(envelope)
    }

    fun decrypt(envelopeJson: String, passphrase: CharArray): String {
        val envelope = Gson().fromJson(envelopeJson, Envelope::class.java)
            ?: throw IllegalArgumentException("Invalid export file")
        require(envelope.format == FORMAT) { "Not a QuickCards export" }
        require(envelope.version == VERSION) { "Unsupported export version" }
        require(envelope.kdf == KDF) { "Unsupported key derivation" }
        require(envelope.cipher == CIPHER) { "Unsupported cipher" }
        require(envelope.iterations in MIN_ITERATIONS..MAX_ITERATIONS) { "Invalid iteration count" }

        val salt = Base64.getDecoder().decode(envelope.salt)
        val iv = Base64.getDecoder().decode(envelope.iv)
        val ciphertext = Base64.getDecoder().decode(envelope.ciphertext)
        require(salt.size == SALT_BYTES) { "Invalid salt" }
        require(iv.size == IV_BYTES) { "Invalid IV" }

        val key = deriveKey(passphrase, salt, envelope.iterations)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        cipher.updateAAD(aad(envelope.salt, envelope.iv, envelope.iterations))
        val plaintextBytes = cipher.doFinal(ciphertext)
        return try {
            String(plaintextBytes, Charsets.UTF_8)
        } finally {
            plaintextBytes.fill(0)
        }
    }

    private fun deriveKey(passphrase: CharArray, salt: ByteArray, iterations: Int): SecretKeySpec {
        val spec = PBEKeySpec(passphrase, salt, iterations, KEY_BITS)
        return try {
            val factory = SecretKeyFactory.getInstance(KDF)
            val encoded = factory.generateSecret(spec).encoded
            try {
                SecretKeySpec(encoded, "AES")
            } finally {
                encoded.fill(0)
            }
        } finally {
            spec.clearPassword()
        }
    }

    private fun aad(saltB64: String, ivB64: String, iterations: Int): ByteArray {
        return "$FORMAT|$VERSION|$KDF|$iterations|$saltB64|$CIPHER|$ivB64".toByteArray(Charsets.UTF_8)
    }
}
