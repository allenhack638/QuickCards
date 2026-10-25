package com.quickcards.domain

/** What a contactless chip is willing to tell a reader: the number and expiry, never the CVV. */
data class EmvCardData(val number: String, val expiry: String?)

/**
 * The small part of EMV needed to read a card's number: BER-TLV parsing and the tags that
 * carry the PAN (5A), Track 2 equivalent (57), expiry (5F24), the PPSE directory (4F/61),
 * the PDOL (9F38) and the AFL (94 / response template 80). Pure Kotlin so it is unit-tested
 * against known APDU responses; the NFC transport lives in the app.
 */
object EmvCard {

    class Tlv(val tag: Int, val value: ByteArray, val children: List<Tlv>)

    fun parseTlv(bytes: ByteArray): List<Tlv> {
        val out = ArrayList<Tlv>()
        var i = 0
        while (i < bytes.size) {
            val b = bytes[i].toInt() and 0xFF
            if (b == 0x00 || b == 0xFF) { i++; continue }
            var tag = b
            val constructed = (b and 0x20) != 0
            i++
            if ((b and 0x1F) == 0x1F) {
                do {
                    if (i >= bytes.size) return out
                    val n = bytes[i].toInt() and 0xFF
                    tag = (tag shl 8) or n
                    i++
                } while ((n and 0x80) != 0)
            }
            if (i >= bytes.size) return out
            var len = bytes[i].toInt() and 0xFF
            i++
            if ((len and 0x80) != 0) {
                val count = len and 0x7F
                len = 0
                repeat(count) {
                    if (i >= bytes.size) return out
                    len = (len shl 8) or (bytes[i].toInt() and 0xFF)
                    i++
                }
            }
            if (i + len > bytes.size) return out
            val value = bytes.copyOfRange(i, i + len)
            i += len
            out.add(Tlv(tag, value, if (constructed) parseTlv(value) else emptyList()))
        }
        return out
    }

    fun find(tlvs: List<Tlv>, tag: Int): Tlv? {
        for (t in tlvs) {
            if (t.tag == tag) return t
            find(t.children, tag)?.let { return it }
        }
        return null
    }

    fun findAll(tlvs: List<Tlv>, tag: Int, into: MutableList<Tlv> = ArrayList()): List<Tlv> {
        for (t in tlvs) {
            if (t.tag == tag) into.add(t)
            findAll(t.children, tag, into)
        }
        return into
    }

    /** Application identifiers listed by the PPSE, in priority order. */
    fun aidsFromPpse(fci: ByteArray): List<ByteArray> =
        findAll(parseTlv(fci), 0x4F).map { it.value }

    /**
     * Builds the GPO data from the PDOL: every requested tag is answered with zeros except the
     * terminal transaction qualifiers, which say "contactless, no CVM" so the card hands over
     * its records.
     */
    fun gpoData(fci: ByteArray): ByteArray {
        val pdol = find(parseTlv(fci), 0x9F38)?.value ?: return byteArrayOf(0x83.toByte(), 0x00)
        val body = ArrayList<Byte>()
        var i = 0
        while (i < pdol.size) {
            var tag = pdol[i].toInt() and 0xFF
            i++
            if ((tag and 0x1F) == 0x1F) {
                while (i < pdol.size) {
                    val n = pdol[i].toInt() and 0xFF
                    tag = (tag shl 8) or n
                    i++
                    if ((n and 0x80) == 0) break
                }
            }
            if (i >= pdol.size) break
            val len = pdol[i].toInt() and 0xFF
            i++
            val value = when (tag) {
                0x9F66 -> byteArrayOf(0x36, 0x00, 0x00, 0x00)
                0x9F02 -> byteArrayOf(0, 0, 0, 0, 0, 1)
                0x5F2A -> byteArrayOf(0x03, 0x56)
                0x9A -> byteArrayOf(0x25, 0x01, 0x01)
                0x9C -> byteArrayOf(0x00)
                0x9F37 -> byteArrayOf(0x12, 0x34, 0x56, 0x78)
                0x9F1A -> byteArrayOf(0x03, 0x56)
                else -> ByteArray(len)
            }
            val fitted = ByteArray(len).also { System.arraycopy(value, 0, it, 0, minOf(len, value.size)) }
            fitted.forEach { body.add(it) }
        }
        return byteArrayOf(0x83.toByte(), body.size.toByte()) + body.toByteArray()
    }

    /** (sfi, firstRecord, lastRecord) triples from a GPO response, either format. */
    fun aflEntries(gpo: ByteArray): List<Triple<Int, Int, Int>> {
        val tlvs = parseTlv(gpo)
        val afl: ByteArray = find(tlvs, 0x94)?.value
            ?: find(tlvs, 0x80)?.value?.let { if (it.size > 2) it.copyOfRange(2, it.size) else null }
            ?: return emptyList()
        val out = ArrayList<Triple<Int, Int, Int>>()
        var i = 0
        while (i + 3 < afl.size) {
            val sfi = (afl[i].toInt() and 0xFF) shr 3
            out.add(Triple(sfi, afl[i + 1].toInt() and 0xFF, afl[i + 2].toInt() and 0xFF))
            i += 4
        }
        return out
    }

    /** Looks for the card data in any response (records, or a GPO that already carried it). */
    fun extract(bytes: ByteArray): EmvCardData? {
        val tlvs = parseTlv(bytes)
        find(tlvs, 0x57)?.let { track2 ->
            val hex = toHex(track2.value).uppercase()
            val sep = hex.indexOf('D')
            if (sep in 12..19) {
                val pan = hex.substring(0, sep)
                val rest = hex.substring(sep + 1)
                val expiry = if (rest.length >= 4) "${rest.substring(2, 4)}/${rest.substring(0, 2)}" else null
                if (CardNumberRules.isLuhnValid(pan)) return EmvCardData(pan, expiry)
            }
        }
        val pan = find(tlvs, 0x5A)?.value?.let { toHex(it).uppercase().trimEnd('F') }
        if (pan != null && pan.length in 12..19 && pan.all(Char::isDigit) && CardNumberRules.isLuhnValid(pan)) {
            val expiry = find(tlvs, 0x5F24)?.value?.takeIf { it.size >= 2 }?.let { "%02X/%02X".format(it[1], it[0]) }
            return EmvCardData(pan, expiry)
        }
        return null
    }

    fun toHex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it) }

    fun fromHex(hex: String): ByteArray {
        val clean = hex.replace(" ", "")
        return ByteArray(clean.length / 2) { clean.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
    }
}
