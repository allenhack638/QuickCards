package com.quickcards.app.core.autofill

import android.app.PendingIntent
import android.app.assist.AssistStructure
import android.content.Intent
import android.os.Build
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.Dataset
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import android.view.autofill.AutofillId
import android.widget.RemoteViews
import com.quickcards.app.QuickCardsApplication
import com.quickcards.app.BuildConfig
import com.quickcards.app.R
import com.quickcards.app.core.data.model.VaultCard
import com.quickcards.app.core.data.model.toDomain
import com.quickcards.domain.AutofillFields
import com.quickcards.domain.CardFieldKind
import com.quickcards.domain.CardNumberRules
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Offers the vault's cards to checkout forms in other apps. Every suggestion is authenticated:
 * the values are only decrypted inside [AutofillAuthActivity] after the biometric prompt, so
 * this service never hands plaintext to the framework. Nothing is ever saved from other apps.
 */
class QuickCardsAutofillService : AutofillService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onFillRequest(request: FillRequest, signal: CancellationSignal, callback: FillCallback) {
        val app = application as QuickCardsApplication
        if (!app.settings.autofillEnabled.value) return callback.onSuccess(null)
        val structure = request.fillContexts.lastOrNull()?.structure ?: return callback.onSuccess(null)
        val component = structure.activityComponent
        // Never fill our own screens; the debug demo form is the one exception.
        val ownForm = component?.packageName == packageName
        if (ownForm && !(BuildConfig.DEBUG && component?.className?.endsWith(".debug.AutofillDemoActivity") == true)) {
            return callback.onSuccess(null)
        }

        val form = AutofillStructureParser.parse(structure)
        if (form.fields[CardFieldKind.NUMBER] == null) return callback.onSuccess(null)

        scope.launch {
            val response = runCatching { buildResponse(app, form) }.getOrNull()
            if (signal.isCanceled) return@launch
            callback.onSuccess(response)
        }
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        // Card data entered elsewhere is never captured.
        callback.onSuccess()
    }

    private suspend fun buildResponse(app: QuickCardsApplication, form: AutofillForm): FillResponse? {
        val cards = app.repository.cards.first().filterNot { it.archived }
        if (cards.isEmpty()) return null
        val banks = app.repository.banks.first().associateBy { it.id }
        val networks = app.repository.networks.first().map { it.toDomain() }
        val ordered = cards.sortedWith(compareByDescending<VaultCard> { it.pinned }.thenBy { it.sortOrder })
            .take(MAX_SUGGESTIONS)
        val builder = FillResponse.Builder()
        ordered.forEachIndexed { index, card ->
            val network = if (card.networkAuto) CardNumberRules.detect(card.number, networks) else networks.firstOrNull { it.id == card.networkId }
            val bank = card.bankId?.let { banks[it]?.name }.orEmpty().ifBlank { getString(R.string.app_name) }
            val title = getString(
                R.string.autofill_suggestion,
                listOfNotNull(bank, network?.name?.takeIf { it.isNotBlank() }).joinToString(" "),
                card.number.takeLast(4)
            )
            val presentation = RemoteViews(packageName, R.layout.autofill_suggestion).apply {
                setTextViewText(R.id.autofill_title, title)
            }
            val dataset = Dataset.Builder(presentation)
            // Placeholders only: the real values arrive from the auth activity.
            form.fields.values.forEach { id -> dataset.setValue(id, null, presentation) }
            dataset.setAuthentication(authIntent(card.id, form, index))
            builder.addDataset(dataset.build())
        }
        return builder.build()
    }

    private fun authIntent(cardId: String, form: AutofillForm, requestCode: Int) = PendingIntent.getActivity(
        this,
        requestCode,
        AutofillAuthActivity.intent(this, cardId, form),
        PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
    ).intentSender

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val MAX_SUGGESTIONS = 5
    }
}

/** The card fields a form exposes, keyed by what they ask for. */
class AutofillForm(val fields: Map<CardFieldKind, AutofillId>, val twoDigitYear: Boolean)

object AutofillStructureParser {

    fun parse(structure: AssistStructure): AutofillForm {
        val fields = LinkedHashMap<CardFieldKind, AutofillId>()
        var twoDigitYear = false
        for (i in 0 until structure.windowNodeCount) {
            walk(structure.getWindowNodeAt(i).rootViewNode) { node ->
                val id = node.autofillId ?: return@walk
                val html = node.htmlInfo?.attributes?.firstOrNull { it.first.equals("autocomplete", ignoreCase = true) }?.second
                val kind = AutofillFields.classify(node.autofillHints?.toList().orEmpty(), html, node.idEntry, node.hint) ?: return@walk
                if (kind !in fields) {
                    fields[kind] = id
                    if (kind == CardFieldKind.EXPIRY_YEAR) {
                        val label = (node.hint ?: node.idEntry ?: "").lowercase()
                        val maxLength = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) node.maxTextLength else -1
                        twoDigitYear = maxLength == 2 || (label.contains("yy") && !label.contains("yyyy"))
                    }
                }
            }
        }
        return AutofillForm(fields, twoDigitYear)
    }

    private fun walk(node: AssistStructure.ViewNode, visit: (AssistStructure.ViewNode) -> Unit) {
        visit(node)
        for (i in 0 until node.childCount) walk(node.getChildAt(i), visit)
    }
}
