package com.quickcards.app.core.nfc

import android.nfc.Tag
import android.nfc.tech.IsoDep
import com.quickcards.domain.EmvCard
import com.quickcards.domain.EmvCardData

/**
 * Reads the number and expiry from a contactless card the way a terminal does: select the
 * payment directory, select the first application, ask for its records and read them. The
 * CVV is never on the chip. Everything happens on the NFC reader thread and nothing is kept.
 */
object NfcCardReader {

    private val PPSE = "2PAY.SYS.DDF01".toByteArray(Charsets.US_ASCII)

    fun read(tag: Tag): EmvCardData? {
        val iso = IsoDep.get(tag) ?: return null
        return try {
            iso.connect()
            iso.timeout = 3_000
            readCard(iso)
        } catch (e: Throwable) {
            // A card can answer anything; a hostile one must not take the app down.
            null
        } finally {
            runCatching { iso.close() }
        }
    }

    private fun readCard(iso: IsoDep): EmvCardData? {
        val ppse = transmit(iso, select(PPSE)) ?: return null
        val aids = EmvCard.aidsFromPpse(ppse).ifEmpty { return null }
        for (aid in aids) {
            val fci = transmit(iso, select(aid)) ?: continue
            val gpo = transmit(iso, gpo(EmvCard.gpoData(fci))) ?: continue
            EmvCard.extract(gpo)?.let { return it }
            for ((sfi, first, last) in EmvCard.aflEntries(gpo)) {
                for (record in first..last) {
                    val data = transmit(iso, readRecord(sfi, record)) ?: continue
                    EmvCard.extract(data)?.let { return it }
                }
            }
        }
        return null
    }

    /** Returns the response body on 9000, or null on any error status. */
    private fun transmit(iso: IsoDep, apdu: ByteArray): ByteArray? {
        var command = apdu
        repeat(MAX_GET_RESPONSE + 1) {
            val response = iso.transceive(command)
            if (response.size < 2) return null
            val sw1 = response[response.size - 2].toInt() and 0xFF
            val sw2 = response[response.size - 1].toInt() and 0xFF
            if (sw1 != 0x61) {
                if (sw1 != 0x90 || sw2 != 0x00) return null
                return response.copyOfRange(0, response.size - 2)
            }
            // Response pending: fetch it, a bounded number of times.
            command = byteArrayOf(0x00, 0xC0.toByte(), 0x00, 0x00, sw2.toByte())
        }
        return null
    }

    private const val MAX_GET_RESPONSE = 8

    private fun select(name: ByteArray): ByteArray =
        byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00, name.size.toByte()) + name + byteArrayOf(0x00)

    private fun gpo(data: ByteArray): ByteArray =
        byteArrayOf(0x80.toByte(), 0xA8.toByte(), 0x00, 0x00, data.size.toByte()) + data + byteArrayOf(0x00)

    private fun readRecord(sfi: Int, record: Int): ByteArray =
        byteArrayOf(0x00, 0xB2.toByte(), record.toByte(), ((sfi shl 3) or 4).toByte(), 0x00)
}
