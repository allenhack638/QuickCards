package com.quickcards.app.core.data.model

/**
 * Initial networks and card types, taken verbatim from NETWORKS0 in
 * prototype/v2.html and the four types seeded by the prototype.
 */
object SeedData {

    val networks: List<CardNetwork> = listOf(
        CardNetwork(
            id = "visa",
            name = "Visa",
            code = "VISA",
            prefixes = "4",
            lengths = "13,16,19",
            cvvLength = 3,
            luhn = true,
            groups = "",
            sortOrder = 0,
            builtIn = true
        ),
        CardNetwork(
            id = "mc",
            name = "Mastercard",
            code = "MC",
            prefixes = "51-55,2221-2720",
            lengths = "16",
            cvvLength = 3,
            luhn = true,
            groups = "",
            sortOrder = 1,
            builtIn = true
        ),
        CardNetwork(
            id = "amex",
            name = "American Express",
            code = "AMEX",
            prefixes = "34,37",
            lengths = "15",
            cvvLength = 4,
            luhn = true,
            groups = "4-6-5",
            sortOrder = 2,
            builtIn = true
        ),
        CardNetwork(
            id = "rupay",
            name = "RuPay",
            code = "RUPAY",
            prefixes = "508500-508999,606985-607984,608001-608500,652150-653149,817200-820199",
            lengths = "16",
            cvvLength = 3,
            luhn = true,
            groups = "",
            sortOrder = 3,
            builtIn = true
        ),
        CardNetwork(
            id = "diners",
            name = "Diners Club",
            code = "DINERS",
            prefixes = "300-305,36,38,39",
            lengths = "14-19",
            cvvLength = 3,
            luhn = true,
            groups = "4-6-4",
            sortOrder = 4,
            builtIn = true
        ),
        CardNetwork(
            id = "discover",
            name = "Discover",
            code = "DISC",
            prefixes = "6011,644-649,65",
            lengths = "16-19",
            cvvLength = 3,
            luhn = true,
            groups = "",
            sortOrder = 5,
            builtIn = true
        ),
        CardNetwork(
            id = "jcb",
            name = "JCB",
            code = "JCB",
            prefixes = "3528-3589",
            lengths = "16-19",
            cvvLength = 3,
            luhn = true,
            groups = "",
            sortOrder = 6,
            builtIn = true
        ),
        CardNetwork(
            id = "maestro",
            name = "Maestro",
            code = "MAESTRO",
            prefixes = "5018,5020,5038,5893,6304,6759,6761-6763",
            lengths = "12-19",
            cvvLength = 3,
            luhn = true,
            groups = "",
            sortOrder = 7,
            builtIn = true
        ),
        CardNetwork(
            id = "unionpay",
            name = "UnionPay",
            code = "UPI",
            prefixes = "62",
            lengths = "16-19",
            cvvLength = 3,
            luhn = false,
            groups = "",
            sortOrder = 8,
            builtIn = true
        ),
        CardNetwork(
            id = "mir",
            name = "Mir",
            code = "MIR",
            prefixes = "2200-2204",
            lengths = "16-19",
            cvvLength = 3,
            luhn = true,
            groups = "",
            sortOrder = 9,
            builtIn = true
        )
    )

    val cardTypes: List<CardType> = listOf(
        CardType(id = "b0a1c2d3-0001-4e5f-8a9b-000000000001", name = "Credit", sortOrder = 0),
        CardType(id = "b0a1c2d3-0002-4e5f-8a9b-000000000002", name = "Debit", sortOrder = 1),
        CardType(id = "b0a1c2d3-0003-4e5f-8a9b-000000000003", name = "Prepaid", sortOrder = 2),
        CardType(id = "b0a1c2d3-0004-4e5f-8a9b-000000000004", name = "Forex", sortOrder = 3)
    )
}
