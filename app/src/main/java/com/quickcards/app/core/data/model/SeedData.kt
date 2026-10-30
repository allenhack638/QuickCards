package com.quickcards.app.core.data.model

import com.quickcards.domain.DefaultNetworks

/** Initial networks and card types; card types verbatim from prototype/v2.html. */
object SeedData {

    /** The built-in networks, in list order; the rules come from the domain's single source. */
    val networks: List<CardNetwork> = DefaultNetworks.all.mapIndexed { index, network ->
        CardNetwork(
            id = network.id,
            name = network.name,
            code = network.code,
            prefixes = network.prefixes,
            lengths = network.lengths,
            cvvLength = network.cvvLength,
            luhn = network.luhn,
            groups = network.groups,
            sortOrder = index,
            builtIn = true,
            cvvOptional = network.cvvOptional
        )
    }

    val cardTypes: List<CardType> = listOf(
        CardType(id = "b0a1c2d3-0001-4e5f-8a9b-000000000001", name = "Credit", sortOrder = 0),
        CardType(id = "b0a1c2d3-0002-4e5f-8a9b-000000000002", name = "Debit", sortOrder = 1),
        CardType(id = "b0a1c2d3-0003-4e5f-8a9b-000000000003", name = "Prepaid", sortOrder = 2),
        CardType(id = "b0a1c2d3-0004-4e5f-8a9b-000000000004", name = "Forex", sortOrder = 3)
    )
}
