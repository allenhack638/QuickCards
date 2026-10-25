package com.quickcards.app.core.data.model

import com.quickcards.domain.CardField

/**
 * A decrypted automatic repair, for the Edit screen's "Undo fix" action and
 * the 30-day repair window. [before] and [after] are plain values only in
 * memory; Room stores them encrypted.
 */
data class CardRepair(
    val id: Long,
    val cardId: String,
    val field: CardField,
    val before: String,
    val after: String,
    val at: Long
)
