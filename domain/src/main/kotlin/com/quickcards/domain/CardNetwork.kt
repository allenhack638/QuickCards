package com.quickcards.domain

const val NetworkLabelMax = 15

data class CardNetwork(
    val id: String,
    val name: String,
    val code: String,
    val prefixes: String,
    val lengths: String,
    val cvvLength: Int,
    val luhn: Boolean,
    val groups: String = ""
)

/** The parts of a network that decide which numbers and CVVs a card accepts. */
enum class NetworkRule {
    PREFIXES,
    LENGTHS,
    CVV,
    LUHN
}

object DefaultNetworks {

    /** Many Maestro cards have no printed CVV, so the field may stay blank. */
    private val cvvOptionalIds = setOf("maestro")

    fun standard(id: String?): CardNetwork? = all.firstOrNull { it.id == id }

    fun cvvOptional(network: CardNetwork?): Boolean = network?.id in cvvOptionalIds

    /** Empty for custom networks and for built-in networks still on their standard rules. */
    fun changedRules(network: CardNetwork): Set<NetworkRule> {
        val standard = standard(network.id) ?: return emptySet()
        return buildSet {
            if (compact(network.prefixes) != compact(standard.prefixes)) add(NetworkRule.PREFIXES)
            if (compact(network.lengths) != compact(standard.lengths)) add(NetworkRule.LENGTHS)
            if (network.cvvLength != standard.cvvLength) add(NetworkRule.CVV)
            if (network.luhn != standard.luhn) add(NetworkRule.LUHN)
        }
    }

    private fun compact(spec: String): String = spec.filterNot(Char::isWhitespace)

    val all: List<CardNetwork> = listOf(
        CardNetwork(
            id = "visa",
            name = "Visa",
            code = "VISA",
            prefixes = "4",
            lengths = "13,16,19",
            cvvLength = 3,
            luhn = true
        ),
        CardNetwork(
            id = "mc",
            name = "Mastercard",
            code = "MC",
            prefixes = "51-55,2221-2720",
            lengths = "16",
            cvvLength = 3,
            luhn = true
        ),
        CardNetwork(
            id = "amex",
            name = "American Express",
            code = "AMEX",
            prefixes = "34,37",
            lengths = "15",
            cvvLength = 4,
            luhn = true,
            groups = "4-6-5"
        ),
        CardNetwork(
            id = "rupay",
            name = "RuPay",
            code = "RUPAY",
            prefixes = "508500-508999,606985-607984,608001-608500,652150-653149,817200-820199",
            lengths = "16",
            cvvLength = 3,
            luhn = true
        ),
        CardNetwork(
            id = "diners",
            name = "Diners Club",
            code = "DINERS",
            prefixes = "300-305,36,38,39",
            lengths = "14-19",
            cvvLength = 3,
            luhn = true,
            groups = "4-6-4"
        ),
        CardNetwork(
            id = "discover",
            name = "Discover",
            code = "DISC",
            prefixes = "6011,644-649,65",
            lengths = "16-19",
            cvvLength = 3,
            luhn = true
        ),
        CardNetwork(
            id = "jcb",
            name = "JCB",
            code = "JCB",
            prefixes = "3528-3589",
            lengths = "16-19",
            cvvLength = 3,
            luhn = true
        ),
        CardNetwork(
            id = "maestro",
            name = "Maestro",
            code = "MAESTRO",
            prefixes = "5018,5020,5038,5893,6304,6759,6761-6763",
            lengths = "12-19",
            cvvLength = 3,
            luhn = true
        ),
        CardNetwork(
            id = "unionpay",
            name = "UnionPay",
            code = "UPI",
            prefixes = "62",
            lengths = "16-19",
            cvvLength = 3,
            luhn = false
        ),
        CardNetwork(
            id = "mir",
            name = "Mir",
            code = "MIR",
            prefixes = "2200-2204",
            lengths = "16-19",
            cvvLength = 3,
            luhn = true
        )
    )
}
