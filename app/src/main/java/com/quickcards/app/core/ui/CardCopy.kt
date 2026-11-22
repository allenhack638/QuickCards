package com.quickcards.app.core.ui

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.quickcards.app.R
import com.quickcards.app.core.autofill.CardUsage
import com.quickcards.app.core.notifications.CardNotifications
import com.quickcards.app.core.notifications.rememberCardNotifications
import com.quickcards.app.core.security.ClipboardHelper
import com.quickcards.domain.CardNumberRules
import com.quickcards.domain.TimeText

/** A card value that can be copied; [DETAILS] is number, expiry and name on three lines. */
enum class CardCopyField(@StringRes val nameRes: Int) {
    NUMBER(R.string.cards_field_number),
    EXPIRY(R.string.cards_field_expiry),
    CVV(R.string.cards_field_cvv),
    HOLDER(R.string.cards_field_name),
    NOTE(R.string.cards_clipboard_note),
    DETAILS(R.string.cards_field_details);

    companion object {
        fun valueOf(card: HomeCard, field: CardCopyField): String? = when (field) {
            NUMBER -> card.card.number
            EXPIRY -> card.card.expiry
            CVV -> card.card.cvv
            HOLDER -> card.holderName
            NOTE -> card.card.note
            DETAILS -> listOf(
                CardNumberRules.format(card.card.number, card.network, masked = false),
                card.card.expiry,
                card.holderName
            ).joinToString("\n")
        }.takeIf { it.isNotBlank() }
    }
}

/** Puts one card value on the clipboard; returns what to announce, or null when there was nothing to copy. */
fun interface CardFieldCopier {
    fun copy(card: HomeCard, field: CardCopyField): String?
}

/**
 * The one copy path for a card value (Home, Card details, Archive, the actions sheet): a
 * sensitive clip with the Settings clear timer, the card marked as used, and after a number
 * copy the Expiry/CVV notification. Callers unlock first and own their visual feedback.
 */
class CardCopier(
    private val context: Context,
    private val notifications: CardNotifications
) : CardFieldCopier {

    override fun copy(card: HomeCard, field: CardCopyField): String? {
        val value = CardCopyField.valueOf(card, field) ?: return null
        val name = context.getString(field.nameRes)
        val label = if (field == CardCopyField.DETAILS) {
            context.getString(R.string.cards_clipboard_details, card.title)
        } else {
            context.getString(R.string.cards_clipboard_field, card.title, name)
        }
        val clearSec = ClipboardHelper.copyTimed(context, label, value)
        CardUsage.record(context, card.id)
        if (field == CardCopyField.NUMBER) notifications.numberCopied(card)
        return copiedMessage(context, name, clearSec)
    }

    companion object {
        fun copiedMessage(context: Context, name: String, clearSec: Int): String = if (clearSec > 0) {
            context.getString(R.string.cards_copied_clears, name, TimeText.clipText(clearSec))
        } else {
            context.getString(R.string.cards_copied, name)
        }
    }
}

@Composable
fun rememberCardCopier(): CardCopier {
    val context = LocalContext.current.applicationContext
    val notifications = rememberCardNotifications()
    return remember(context, notifications) { CardCopier(context, notifications) }
}
