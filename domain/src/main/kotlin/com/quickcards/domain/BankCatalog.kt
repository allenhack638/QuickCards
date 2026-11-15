package com.quickcards.domain

import java.util.Locale

/** A bank QuickCards knows out of the box; [aliases] are other names people type for it. */
data class CatalogBank(
    val name: String,
    val aliases: List<String> = emptyList(),
    /** Shown in brackets in the import sheet's list to help people find it; never required. */
    val hint: String? = null
) {
    val label: String get() = if (hint == null) name else "$name ($hint)"
}

/**
 * The bundled list of card-issuing banks: 16 Indian issuers (RBI cards-outstanding leaders)
 * and 10 large international ones. It seeds a fresh install, is added to older installs once
 * per [VERSION], fills the import sheet's Bank list and matches typed bank names on import.
 * Adding a bank: append it here, bump [VERSION], and the backfill adds it on next start.
 */
object BankCatalog {

    const val VERSION = "2026.11"

    // Declared before `all`/`byKey`: object initialisers run in order and `key()` needs them.
    private val BRACKETS = Regex("""\([^)]*\)""")
    private val BRACKET_TEXT = Regex("""\(([^)]*)\)""")
    private val PUNCTUATION = Regex("""[^\p{L}\p{N}]+""")
    private val SPACES = Regex("""\s+""")

    val all: List<CatalogBank> = listOf(
        CatalogBank("HDFC Bank", listOf("HDFC")),
        CatalogBank("SBI Card", listOf("SBI", "SBI Cards", "State Bank of India"), hint = "State Bank of India"),
        CatalogBank("ICICI Bank", listOf("ICICI")),
        CatalogBank("Axis Bank", listOf("Axis")),
        CatalogBank("Kotak Mahindra Bank", listOf("Kotak", "Kotak Bank")),
        CatalogBank("IndusInd Bank", listOf("IndusInd")),
        CatalogBank("RBL Bank", listOf("RBL", "Ratnakar Bank")),
        CatalogBank("IDFC FIRST Bank", listOf("IDFC", "IDFC First", "IDFC Bank")),
        CatalogBank("Bank of Baroda", listOf("BOB", "BOBCARD", "BOB Card"), hint = "BOBCARD"),
        CatalogBank("Yes Bank", listOf("Yes")),
        CatalogBank("AU Small Finance Bank", listOf("AU Bank", "AU SFB", "AU")),
        CatalogBank("Federal Bank", listOf("Federal")),
        CatalogBank("Bandhan Bank", listOf("Bandhan")),
        CatalogBank("Punjab National Bank", listOf("PNB"), hint = "PNB"),
        CatalogBank("Canara Bank", listOf("Canara")),
        CatalogBank("Union Bank of India", listOf("Union Bank", "UBI")),
        CatalogBank("American Express", listOf("Amex"), hint = "Amex"),
        CatalogBank("HSBC", listOf("HSBC Bank")),
        CatalogBank("Standard Chartered", listOf("Standard Chartered Bank", "StanChart", "SC Bank")),
        CatalogBank("Citibank", listOf("Citi", "Citi Bank"), hint = "Citi"),
        CatalogBank("Chase", listOf("JPMorgan Chase", "JP Morgan Chase", "JPMorgan", "Chase Bank"), hint = "JPMorgan Chase"),
        CatalogBank("Capital One", listOf("Capital One Bank")),
        CatalogBank("Bank of America", listOf("BofA", "BoA")),
        CatalogBank("Barclays", listOf("Barclays Bank", "Barclaycard")),
        CatalogBank("Wells Fargo", listOf("Wells Fargo Bank")),
        CatalogBank("DBS Bank", listOf("DBS"))
    )

    private val byKey: Map<String, CatalogBank> = buildMap {
        all.forEach { bank ->
            put(key(bank.name), bank)
            bank.aliases.forEach { alias -> put(key(alias), bank) }
        }
    }

    /**
     * The matching key for a typed bank name: case-insensitive, text in brackets dropped
     * ("SBI Card (State Bank of India)" and "SBI Card" match), punctuation ignored, spaces
     * collapsed. Empty when nothing is left.
     */
    fun key(raw: String): String = raw
        .replace(BRACKETS, " ")
        .lowercase(Locale.ROOT)
        .replace(PUNCTUATION, " ")
        .trim()
        .replace(SPACES, " ")

    /** The catalog bank a typed name refers to, by name or alias, or null. */
    fun find(raw: String): CatalogBank? {
        val k = key(raw)
        if (k.isEmpty()) return null
        return byKey[k] ?: bracketed(raw)?.let { byKey[key(it)] }
    }

    /** "SBI Card (State Bank of India)" -> "State Bank of India", so the hint alone also matches. */
    private fun bracketed(raw: String): String? = BRACKET_TEXT.find(raw)?.groupValues?.get(1)

}
