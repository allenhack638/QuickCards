package com.quickcards.app.core.autofill

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.autofill.Dataset
import android.view.autofill.AutofillId
import android.view.autofill.AutofillManager
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import androidx.lifecycle.lifecycleScope
import com.quickcards.app.QuickCardsApplication
import com.quickcards.app.R
import com.quickcards.app.core.security.UnlockSession
import com.quickcards.app.core.ui.base.BaseActivity
import com.quickcards.domain.AutofillFields
import com.quickcards.domain.CardFieldKind
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Transparent activity the framework opens when a suggestion is tapped. It runs the biometric
 * prompt, decrypts the one card and returns the filled dataset; a cancelled prompt returns
 * nothing, so the form stays empty (fail-closed).
 */
class AutofillAuthActivity : BaseActivity() {

    override fun onSecureCreate(savedInstanceState: Bundle?) {
        val cardId = intent.getStringExtra(EXTRA_CARD_ID)
        val kinds = intent.getStringArrayExtra(EXTRA_KINDS)
        val ids = intent.getParcelableArrayExtra(EXTRA_IDS)?.filterIsInstance<AutofillId>()
        if (cardId == null || kinds == null || ids == null || kinds.size != ids.size) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }
        val twoDigitYear = intent.getBooleanExtra(EXTRA_TWO_DIGIT_YEAR, false)
        lifecycleScope.launch {
            val granted = UnlockSession.requireUnlock(this@AutofillAuthActivity, getString(R.string.autofill_unlock_reason), fresh = true)
            val dataset = if (granted) buildDataset(cardId, kinds, ids, twoDigitYear) else null
            if (dataset == null) {
                setResult(Activity.RESULT_CANCELED)
            } else {
                setResult(Activity.RESULT_OK, Intent().putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, dataset))
            }
            finish()
        }
    }

    private suspend fun buildDataset(cardId: String, kinds: Array<String>, ids: List<AutofillId>, twoDigitYear: Boolean): Dataset? {
        val app = application as QuickCardsApplication
        val card = app.repository.cards.first().firstOrNull { it.id == cardId } ?: return null
        val holder = card.holderId?.let { id -> app.repository.holders.first().firstOrNull { it.id == id }?.name }
        val name = card.nameOnCard.ifBlank { holder.orEmpty() }
        val presentation = RemoteViews(packageName, R.layout.autofill_suggestion).apply {
            setTextViewText(R.id.autofill_title, getString(R.string.app_name))
        }
        val builder = Dataset.Builder(presentation)
        var filled = 0
        kinds.forEachIndexed { index, kindName ->
            val kind = CardFieldKind.valueOf(kindName)
            val value = AutofillFields.valueFor(kind, card.number, card.expiry, card.cvv, name, twoDigitYear) ?: return@forEachIndexed
            builder.setValue(ids[index], AutofillValue.forText(value))
            filled++
        }
        return if (filled == 0) null else builder.build()
    }

    companion object {
        private const val EXTRA_CARD_ID = "card_id"
        private const val EXTRA_KINDS = "kinds"
        private const val EXTRA_IDS = "ids"
        private const val EXTRA_TWO_DIGIT_YEAR = "two_digit_year"

        fun intent(context: Context, cardId: String, form: AutofillForm): Intent =
            Intent(context, AutofillAuthActivity::class.java)
                .putExtra(EXTRA_CARD_ID, cardId)
                .putExtra(EXTRA_KINDS, form.fields.keys.map { it.name }.toTypedArray())
                .putExtra(EXTRA_IDS, form.fields.values.toTypedArray())
                .putExtra(EXTRA_TWO_DIGIT_YEAR, form.twoDigitYear)
    }
}
