package com.quickcards.app.core.security

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.crypto.AEADBadTagException

class ExportCryptoTest {

    private val passphrase = "correct horse battery staple".toCharArray()
    private val plaintext = """[{"cardNumber":"4111111111111111","cvv":"123","owner":"TEST USER"}]"""

    @Test
    fun roundTrip() {
        val envelope = ExportCrypto.encrypt(plaintext, passphrase)
        assertEquals(plaintext, ExportCrypto.decrypt(envelope, passphrase))
    }

    @Test
    fun envelopeDoesNotContainPlaintext() {
        val envelope = ExportCrypto.encrypt(plaintext, passphrase)
        assertFalse(envelope.contains("4111111111111111"))
        assertFalse(envelope.contains("TEST USER"))
        assertTrue(ExportCrypto.isEnvelope(envelope))
    }

    @Test
    fun wrongPassphraseFails() {
        val envelope = ExportCrypto.encrypt(plaintext, passphrase)
        assertThrows(AEADBadTagException::class.java) {
            ExportCrypto.decrypt(envelope, "wrong passphrase".toCharArray())
        }
    }

    @Test
    fun tamperedCiphertextFails() {
        val envelope = ExportCrypto.encrypt(plaintext, passphrase)
        val root = JsonParser.parseString(envelope).asJsonObject
        val ciphertext = java.util.Base64.getDecoder().decode(root.get("ciphertext").asString)
        ciphertext[0] = (ciphertext[0].toInt() xor 0x01).toByte()
        root.addProperty("ciphertext", java.util.Base64.getEncoder().encodeToString(ciphertext))
        assertThrows(AEADBadTagException::class.java) {
            ExportCrypto.decrypt(root.toString(), passphrase)
        }
    }

    @Test
    fun tamperedHeaderFails() {
        val envelope = ExportCrypto.encrypt(plaintext, passphrase)
        val root = JsonParser.parseString(envelope).asJsonObject
        root.addProperty("iterations", 300_000)
        assertThrows(AEADBadTagException::class.java) {
            ExportCrypto.decrypt(root.toString(), passphrase)
        }
    }

    @Test
    fun legacyPlaintextIsNotAnEnvelope() {
        assertFalse(ExportCrypto.isEnvelope(plaintext))
    }
}
