package com.quickcards.app.core.data.model

import com.quickcards.domain.CardNetwork as DomainCardNetwork
import com.quickcards.domain.NetworkRules

fun CardNetwork.toDomain(): DomainCardNetwork = DomainCardNetwork(
    id = id,
    name = name,
    code = code,
    prefixes = prefixes,
    lengths = lengths,
    cvvLength = cvvLength,
    luhn = luhn,
    groups = groups,
    cvvOptional = cvvOptional
)

fun CardNetwork.withRules(rules: NetworkRules): CardNetwork = copy(
    prefixes = rules.prefixes,
    lengths = rules.lengths,
    cvvLength = rules.cvvLength,
    luhn = rules.luhn,
    cvvOptional = rules.cvvOptional
)
