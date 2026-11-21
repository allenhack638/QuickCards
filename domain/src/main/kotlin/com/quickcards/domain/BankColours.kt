package com.quickcards.domain

/** Default card colours per bank (DESIGN.md › Bank default colours). Graphite is never a default. */
object BankColours {

    private val builtIn: Map<String, String> = mapOf(
        "HDFC Bank" to "navy",
        "SBI Card" to "ocean",
        "ICICI Bank" to "rust",
        "Axis Bank" to "wine",
        "Kotak Mahindra Bank" to "crimson",
        "IndusInd Bank" to "plum",
        "RBL Bank" to "violet",
        "IDFC FIRST Bank" to "magenta",
        "Bank of Baroda" to "rust",
        "Yes Bank" to "indigo",
        "AU Small Finance Bank" to "burnt-orange",
        "Federal Bank" to "olive",
        "Bandhan Bank" to "moss",
        "Punjab National Bank" to "forest",
        "Canara Bank" to "teal",
        "Union Bank of India" to "slate",
        "American Express" to "ocean",
        "HSBC" to "crimson",
        "Standard Chartered" to "teal",
        "Citibank" to "navy",
        "Chase" to "indigo",
        "Capital One" to "slate",
        "Bank of America" to "wine",
        "Barclays" to "ocean",
        "Wells Fargo" to "burnt-orange",
        "DBS Bank" to "crimson"
    )

    /** The built-in colour key for a typed bank name (aliases included), or null for other banks. */
    fun defaultFor(bankName: String): String? = BankCatalog.find(bankName)?.let { builtIn[it.name] }

    /**
     * The colour for a new bank, given one colour per bank already in the list: the first palette
     * colour nobody uses, else the least used (ties go to the earlier colour).
     */
    fun autoPick(usedByBanks: List<String>): String {
        val counts = ArtRules.PALETTE_ORDER.associateWith { key -> usedByBanks.count { it == key } }
        return ArtRules.PALETTE_ORDER.minBy { counts.getValue(it) }
    }
}
