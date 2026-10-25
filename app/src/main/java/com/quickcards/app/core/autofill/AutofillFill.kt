package com.quickcards.app.core.autofill

import android.content.Context
import android.content.Intent
import android.service.autofill.Dataset
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import com.quickcards.app.QuickCardsApplication
import com.quickcards.app.R
import com.quickcards.domain.AutofillFields
import com.quickcards.domain.CardFieldKind
import kotlinx.coroutines.flow.first

/** The form being filled, carried to the auth and picker activities as intent extras. */
class FillTarget(
    val kinds: List<CardFieldKind>,
    val ids: List<AutofillId>,
    val twoDigitYear: Boolean,
    val fourDigitYear: Boolean,
    val requester: String?
) {
    fun writeTo(intent: Intent): Intent = intent
        .putExtra(EXTRA_KINDS, kinds.map { it.name }.toTypedArray())
        .putExtra(EXTRA_IDS, ids.toTypedArray())
        .putExtra(EXTRA_TWO_DIGIT_YEAR, twoDigitYear)
        .putExtra(EXTRA_FOUR_DIGIT_YEAR, fourDigitYear)
        .putExtra(EXTRA_REQUESTER, requester)

    companion object {
        private const val EXTRA_KINDS = "kinds"
        private const val EXTRA_IDS = "ids"
        private const val EXTRA_TWO_DIGIT_YEAR = "two_digit_year"
        private const val EXTRA_FOUR_DIGIT_YEAR = "four_digit_year"
        private const val EXTRA_REQUESTER = "requester"

        fun of(form: AutofillForm, requester: String?) = FillTarget(
            kinds = form.fields.keys.toList(),
            ids = form.fields.values.toList(),
            twoDigitYear = form.twoDigitYear,
            fourDigitYear = form.fourDigitYear,
            requester = requester
        )

        fun from(intent: Intent): FillTarget? {
            val kinds = intent.getStringArrayExtra(EXTRA_KINDS)
                ?.mapNotNull { runCatching { CardFieldKind.valueOf(it) }.getOrNull() } ?: return null
            val ids = intent.getParcelableArrayExtra(EXTRA_IDS)?.filterIsInstance<AutofillId>() ?: return null
            if (kinds.isEmpty() || kinds.size != ids.size) return null
            return FillTarget(
                kinds,
                ids,
                intent.getBooleanExtra(EXTRA_TWO_DIGIT_YEAR, false),
                intent.getBooleanExtra(EXTRA_FOUR_DIGIT_YEAR, false),
                intent.getStringExtra(EXTRA_REQUESTER)
            )
        }
    }
}

/** Builds the filled dataset for one card; only ever called after a successful unlock. */
object AutofillFill {

    fun unlockReason(context: Context, requester: String?): String =
        if (requester.isNullOrBlank()) {
            context.getString(R.string.autofill_unlock_reason)
        } else {
            context.getString(R.string.autofill_unlock_reason_for, requester)
        }

    suspend fun dataset(context: Context, cardId: String, target: FillTarget): Dataset? {
        val app = context.applicationContext as QuickCardsApplication
        val card = app.repository.cards.first().firstOrNull { it.id == cardId && !it.archived } ?: return null
        val holder = card.holderId?.let { id -> app.repository.holders.first().firstOrNull { it.id == id }?.name }
        val name = card.nameOnCard.ifBlank { holder.orEmpty() }
        // Never shown: the framework fills the fields straight away, but a dataset needs one.
        val presentation = RemoteViews(context.packageName, R.layout.autofill_suggestion).apply {
            setTextViewText(R.id.autofill_title, context.getString(R.string.app_name))
        }
        val builder = Dataset.Builder(presentation)
        var filled = 0
        target.kinds.forEachIndexed { index, kind ->
            val value = AutofillFields.valueFor(
                kind, card.number, card.expiry, card.cvv, name, target.twoDigitYear, target.fourDigitYear
            ) ?: return@forEachIndexed
            builder.setValue(target.ids[index], AutofillValue.forText(value))
            filled++
        }
        if (filled == 0) return null
        CardUsage.record(context, card.id)
        return builder.build()
    }
}
