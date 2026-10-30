package com.quickcards.app.core.data.model

import com.quickcards.domain.CardNetwork as DomainCardNetwork

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
