package com.quickcards.app.core.ui

import com.quickcards.app.core.data.model.VaultCard
import com.quickcards.domain.CardNetwork
import com.quickcards.domain.CardQueryEntry
import com.quickcards.domain.ExpiryState

/** A decrypted [VaultCard] plus the display names the UI shows. */
data class HomeCard(
    val card: VaultCard,
    val bankName: String,
    val holderName: String,
    val typeName: String?,
    val network: CardNetwork?,
    val networkId: String?,
    val tagNames: List<String>,
    val expiryState: ExpiryState? = null,
    /** Days until the payment due date while its reminder window is open and unpaid. */
    val dueInDays: Int? = null,
    val dueKey: String? = null
) {
    val id: String get() = card.id

    val title: String
        get() = if (typeName.isNullOrBlank()) bankName else "$bankName $typeName"

    val entry: CardQueryEntry
        get() = CardQueryEntry(
            id = card.id,
            number = card.number,
            bankId = card.bankId,
            bankName = bankName,
            holderId = card.holderId,
            holderName = holderName,
            typeId = card.typeId,
            typeName = typeName.orEmpty(),
            networkId = networkId,
            networkName = network?.name.orEmpty(),
            networkCode = network?.code.orEmpty(),
            note = card.note,
            tagIds = card.tagIds,
            tagNames = tagNames
        )
}
