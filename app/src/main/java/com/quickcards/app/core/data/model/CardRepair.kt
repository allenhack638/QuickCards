package com.quickcards.app.core.data.model

import com.quickcards.domain.CardField

/** A decrypted repair for "Undo fix"; plain only in memory, encrypted in Room. */
data class CardRepair(
    val id: Long,
    val cardId: String,
    val field: CardField,
    val before: String,
    val after: String,
    val at: Long
)
