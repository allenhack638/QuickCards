package com.quickcards.app.feature.cards

import android.content.Context
import com.quickcards.app.R
import com.quickcards.app.core.ui.HomeCard
import com.quickcards.domain.CardNumberRules

/** The plain-text body handed to the system share sheet. */
object CardShareText {
    fun build(context: Context, card: HomeCard, includeCvv: Boolean): String = buildString {
        appendLine(card.title)
        appendLine(
            context.getString(
                R.string.cards_share_number,
                CardNumberRules.format(card.card.number, card.network, masked = false)
            )
        )
        appendLine(context.getString(R.string.cards_share_expiry, card.card.expiry))
        if (card.holderName.isNotBlank()) {
            appendLine(context.getString(R.string.cards_share_name, card.holderName))
        }
        if (includeCvv) appendLine(context.getString(R.string.cards_share_cvv, card.card.cvv))
    }.trimEnd()
}
