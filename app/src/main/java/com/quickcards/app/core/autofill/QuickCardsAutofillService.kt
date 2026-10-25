package com.quickcards.app.core.autofill

import android.app.PendingIntent
import android.app.assist.AssistStructure
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
import android.view.View
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
            val response = runCatching { buildResponse(app, form, component?.packageName) }.getOrNull()
            if (signal.isCanceled) return@launch
            callback.onSuccess(response)
        }
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        // Card data entered elsewhere is never captured.
        callback.onSuccess()
    }

    private suspend fun buildResponse(app: QuickCardsApplication, form: AutofillForm, packageForRequest: String?): FillResponse? {
        val cards = app.repository.cards.first().filterNot { it.archived }
        if (cards.isEmpty()) return null
        val banks = app.repository.banks.first().associateBy { it.id }
        val networks = app.repository.networks.first().map { it.toDomain() }
        val ordered = cards.sortedWith(compareByDescending<VaultCard> { it.pinned }.thenBy { it.sortOrder })
            .take(MAX_SUGGESTIONS)
        val requester = form.webDomain ?: appLabel(packageForRequest)
        val builder = FillResponse.Builder()
        ordered.forEachIndexed { index, card ->
            val network = if (card.networkAuto) CardNumberRules.detect(card.number, networks) else networks.firstOrNull { it.id == card.networkId }
            val bank = card.bankId?.let { banks[it]?.name }.orEmpty().ifBlank { getString(R.string.app_name) }
            val title = getString(
                R.string.autofill_suggestion,
                // "American Express ···0005", not "American Express American Express ···0005".
                listOfNotNull(
                    bank,
                    network?.name?.takeIf { it.isNotBlank() && !bank.contains(it, ignoreCase = true) }
                ).joinToString(" "),
                card.number.takeLast(4)
            )
            val presentation = RemoteViews(packageName, R.layout.autofill_suggestion).apply {
                setTextViewText(R.id.autofill_title, title)
            }
            val dataset = Dataset.Builder(presentation)
            // Placeholders only: the real values arrive from the auth activity.
            form.fields.values.forEach { id -> dataset.setValue(id, null, presentation) }
            dataset.setAuthentication(authIntent(card.id, form, requester, index))
            builder.addDataset(dataset.build())
        }
        return builder.build()
    }

    private fun appLabel(pkg: String?): String? = pkg?.let {
        runCatching { packageManager.getApplicationLabel(packageManager.getApplicationInfo(it, 0)).toString() }
            .getOrNull()
    }

    private fun authIntent(cardId: String, form: AutofillForm, requester: String?, requestCode: Int) = PendingIntent.getActivity(
        this,
        requestCode,
        AutofillAuthActivity.intent(this, cardId, form, requester),
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
class AutofillForm(
    val fields: Map<CardFieldKind, AutofillId>,
    val twoDigitYear: Boolean,
    val fourDigitYear: Boolean,
    /** The site the number field belongs to, if it is a web form. */
    val webDomain: String?
)

object AutofillStructureParser {

    /**
     * Only visible, non-empty fields count, and every field must come from the same site as the
     * card number field, so a page cannot hide a CVV field and collect it with a visible one.
     */
    fun parse(structure: AssistStructure): AutofillForm {
        val found = ArrayList<Triple<CardFieldKind, AssistStructure.ViewNode, String?>>()
        for (i in 0 until structure.windowNodeCount) {
            walk(structure.getWindowNodeAt(i).rootViewNode, null) { node, domain ->
                if (node.autofillId == null || node.visibility != View.VISIBLE) return@walk
                if (node.width <= 0 || node.height <= 0) return@walk
                val html = node.htmlInfo?.attributes?.firstOrNull { it.first.equals("autocomplete", ignoreCase = true) }?.second
                val kind = AutofillFields.classify(node.autofillHints?.toList().orEmpty(), html, node.idEntry, node.hint) ?: return@walk
                found.add(Triple(kind, node, domain))
            }
        }
        val numberDomain = found.firstOrNull { it.first == CardFieldKind.NUMBER }?.third
        val fields = LinkedHashMap<CardFieldKind, AutofillId>()
        var twoDigitYear = false
        var fourDigitYear = false
        for ((kind, node, domain) in found) {
            if (domain != numberDomain) continue
            val id = node.autofillId ?: continue
            if (kind in fields) continue
            fields[kind] = id
            val label = (node.hint ?: node.idEntry ?: "").lowercase()
            val maxLength = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) node.maxTextLength else -1
            when (kind) {
                CardFieldKind.EXPIRY_DATE -> fourDigitYear = label.contains("yyyy") || maxLength >= 7
                CardFieldKind.EXPIRY_YEAR -> twoDigitYear = maxLength == 2 || (label.contains("yy") && !label.contains("yyyy"))
                else -> Unit
            }
        }
        return AutofillForm(fields, twoDigitYear, fourDigitYear, numberDomain)
    }

    /** Web views report their domain on an ancestor node; children inherit it. */
    private fun walk(
        node: AssistStructure.ViewNode,
        domain: String?,
        visit: (AssistStructure.ViewNode, String?) -> Unit
    ) {
        val here = node.webDomain?.takeIf { it.isNotBlank() } ?: domain
        visit(node, here)
        for (i in 0 until node.childCount) walk(node.getChildAt(i), here, visit)
    }
}
