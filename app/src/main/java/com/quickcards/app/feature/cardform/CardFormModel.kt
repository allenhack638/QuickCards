package com.quickcards.app.feature.cardform

import com.quickcards.app.core.data.model.BandColor
import com.quickcards.app.core.data.model.VaultCard
import com.quickcards.app.core.data.settings.AppSettingsDefaults
import com.quickcards.domain.CardDraft
import com.quickcards.domain.CardError
import com.quickcards.domain.CardField
import com.quickcards.domain.CardNetwork
import com.quickcards.domain.CardNumberRules
import com.quickcards.domain.CardValidator
import com.quickcards.domain.ExistingCardRef
import com.quickcards.domain.Field
import com.quickcards.domain.NamedRef
import java.time.YearMonth
import java.util.Locale

enum class CardFormMode { ADD, EDIT }

private fun CardField.toFormField(): Field = when (this) {
    CardField.NUMBER -> Field.NUMBER
    CardField.EXPIRY -> Field.EXPIRY
    CardField.CVV -> Field.CVV
}

/**
 * Callers feed the TextFieldValue back from this, never the raw input, so rejected text
 * vanishes in the same frame and the caret stays at the end.
 */
object CardFieldInput {

    fun number(raw: String, networks: List<CardNetwork>, typing: Boolean): String {
        val digits = raw.filter { it in '0'..'9' }
        val network = CardNumberRules.detect(digits, networks)
        return CardNumberRules.format(
            digits.take(CardNumberRules.maxLength(network)),
            network,
            masked = false,
            typing = typing
        )
    }

    fun expiry(raw: String): String = CardValidator.normalizeExpiry(raw)

    fun cvv(raw: String, network: CardNetwork?): String =
        raw.filter { it in '0'..'9' }.take(network?.cvvLength ?: 4)

    fun note(raw: String): String = raw.take(CardValidator.NOTE_MAX_LENGTH)
}

/** Immutable form state with no Android imports, so it is JVM-tested. */
data class CardFormModel(
    val mode: CardFormMode = CardFormMode.ADD,
    val editingId: String? = null,
    val holderId: String? = null,
    val number: String = "",
    val expiry: String = "",
    val cvv: String = "",
    val bankId: String? = null,
    val typeId: String? = null,
    val tagIds: List<String> = emptyList(),
    val bandColor: String = BandColor.NAVY,
    val note: String = "",
    val dueDay: String = "",
    val errors: Map<Field, CardError> = emptyMap(),
    val tried: Boolean = false
) {
    val isEdit: Boolean get() = mode == CardFormMode.EDIT

    /** Empty means no reminder; anything else must be a day of the month. */
    val dueDayValid: Boolean get() = dueDay.isEmpty() || dueDay.toIntOrNull() in 1..31

    fun network(networks: List<CardNetwork>): CardNetwork? =
        CardNumberRules.detect(number, networks)

    fun cvvDigits(networks: List<CardNetwork>): Int = network(networks)?.cvvLength ?: 3

    fun formattedNumber(networks: List<CardNetwork>, typing: Boolean): String {
        if (number.isEmpty()) return ""
        return CardNumberRules.format(number, network(networks), masked = false, typing = typing)
    }

    fun toDraft(): CardDraft = CardDraft(
        holderId = holderId,
        number = number,
        expiry = expiry,
        cvv = cvv,
        bankId = bankId,
        typeId = typeId,
        tagIds = tagIds,
        note = note,
        dueDay = dueDay.toIntOrNull()
    )

    /** The values a user can edit. Meta fields never make a form dirty. */
    fun editableValues(): List<Any?> = listOf(
        holderId,
        number,
        expiry,
        cvv,
        bankId,
        typeId,
        tagIds,
        bandColor,
        note,
        dueDay
    )

    fun withError(field: Field, message: CardError?): CardFormModel = copy(
        errors = if (message == null) errors - field else errors + (field to message)
    )

    companion object {
        val FIELD_ORDER = listOf(
            Field.HOLDER,
            Field.NUMBER,
            Field.EXPIRY,
            Field.CVV,
            Field.BANK
        )

        fun fromCard(card: VaultCard): CardFormModel = CardFormModel(
            mode = CardFormMode.EDIT,
            editingId = card.id,
            holderId = card.holderId,
            number = card.number,
            expiry = card.expiry,
            cvv = card.cvv,
            bankId = card.bankId,
            typeId = card.typeId,
            tagIds = card.tagIds,
            bandColor = card.bandColor,
            note = card.note,
            dueDay = card.dueDay?.toString().orEmpty()
        )
    }
}

data class CardFormLists(
    val holders: List<NamedRef> = emptyList(),
    val banks: List<NamedRef> = emptyList(),
    val networks: List<CardNetwork> = emptyList(),
    val types: List<NamedRef> = emptyList(),
    val tags: List<NamedRef> = emptyList(),
    val tagLimit: Int = AppSettingsDefaults.TAGS_MAX,
    val tagMaxLen: Int = AppSettingsDefaults.TAGS_MAX_LEN,
    val duplicates: Map<String, ExistingCardRef> = emptyMap()
) {
    fun holderName(id: String?): String? = holders.firstOrNull { it.id == id }?.name
    fun bankName(id: String?): String? = banks.firstOrNull { it.id == id }?.name
    fun typeName(id: String?): String? = types.firstOrNull { it.id == id }?.name
    fun tagName(id: String): String? = tags.firstOrNull { it.id == id }?.name

    fun withHolder(id: String, name: String): CardFormLists = copy(
        holders = (holders.filterNot { it.id == id } + NamedRef(id, name)).sortedBy { it.name.lowercase(Locale.ROOT) }
    )

    fun withBank(id: String, name: String): CardFormLists = copy(
        banks = (banks.filterNot { it.id == id } + NamedRef(id, name)).sortedBy { it.name.lowercase(Locale.ROOT) }
    )

    fun withType(id: String, name: String): CardFormLists = copy(
        types = if (types.any { it.id == id }) types else types + NamedRef(id, name)
    )

    fun withNetwork(network: CardNetwork): CardFormLists = copy(
        networks = networks.filterNot { it.id == network.id } + network
    )

    fun withTag(id: String, name: String): CardFormLists = copy(
        tags = (tags.filterNot { it.id == id } + NamedRef(id, name)).sortedBy { it.name.lowercase(Locale.ROOT) }
    )
}

data class CardFormOptions(
    val tagsShow: Boolean = true,
    val showNotes: Boolean = true,
    val expiresSoonMonths: Int = 1
)

data class CardFormSession(
    val form: CardFormModel,
    val lists: CardFormLists,
    val options: CardFormOptions = CardFormOptions(),
    val today: YearMonth = YearMonth.now(),
    val reviewFields: Set<CardField> = emptySet(),
    val archived: Boolean = false,
    val archiveReason: String? = null,
    private val initialForm: CardFormModel? = null
) {

    private fun withForm(form: CardFormModel): CardFormSession = copy(form = form)

    /** True once an editable value differs from the state the screen opened with. */
    fun isDirty(): Boolean {
        val initial = initialForm ?: return false
        return form.editableValues() != initial.editableValues()
    }

    fun validate(field: Field): CardFormSession {
        val message = CardValidator.validate(
            draft = form.toDraft(),
            networks = lists.networks,
            today = today,
            existing = { number -> lists.duplicates[number] }
        )[field]
        return withForm(form.withError(field, message))
    }

    fun saveAttempt(): CardFormSession {
        val errors = CardValidator.validate(
            draft = form.toDraft(),
            networks = lists.networks,
            today = today,
            existing = { number -> lists.duplicates[number] }
        )
        return withForm(form.copy(errors = errors, tried = true))
    }

    fun firstInvalid(): Field? = CardFormModel.FIELD_ORDER.firstOrNull { it in form.errors }

    fun withError(field: Field, message: CardError?): CardFormSession =
        withForm(form.withError(field, message))

    private fun revalidate(field: Field): CardFormSession =
        if (form.errors.containsKey(field)) validate(field) else this

    fun updateNumber(raw: String): CardFormSession {
        val digits = raw.filter { it in '0'..'9' }.take(19)
        val network = CardNumberRules.detect(digits, lists.networks)
        val next = form.copy(
            number = digits.take(CardNumberRules.maxLength(network)),
            cvv = form.cvv.take(network?.cvvLength ?: 4)
        )
        var session = withForm(next)
        session = session.revalidate(Field.NUMBER)
        if (next.cvv.isNotEmpty() || form.errors.containsKey(Field.CVV)) {
            session = session.validate(Field.CVV)
        }
        return session
    }

    fun numberBlur(): CardFormSession {
        if (form.number.isEmpty() && !form.tried) return this
        return validate(Field.NUMBER)
    }

    fun updateExpiry(raw: String): CardFormSession {
        val next = form.copy(expiry = CardValidator.normalizeExpiry(raw))
        return withForm(next).revalidate(Field.EXPIRY)
    }

    fun expiryBlur(): CardFormSession =
        if (form.expiry.isNotBlank() || form.tried) validate(Field.EXPIRY) else this

    fun updateCvv(raw: String): CardFormSession {
        val max = form.network(lists.networks)?.cvvLength ?: 4
        val next = form.copy(cvv = raw.filter { it in '0'..'9' }.take(max))
        return withForm(next).revalidate(Field.CVV)
    }

    fun cvvBlur(): CardFormSession =
        if (form.cvv.isNotBlank() || form.tried) validate(Field.CVV) else this

    /** Digits only, at most two; an out-of-range day is kept and flagged so typing never vanishes. */
    fun updateDueDay(raw: String): CardFormSession =
        withForm(form.copy(dueDay = raw.filter(Char::isDigit).take(2)))

    fun updateNote(raw: String): CardFormSession = withForm(
        form.copy(note = raw.take(CardValidator.NOTE_MAX_LENGTH))
    )

    /** Unrepaired legacy values open with their validation error so "Check details" leads there. */
    fun showReviewErrors(fields: Set<CardField>): CardFormSession {
        var session = this
        fields.forEach { field -> session = session.validate(field.toFormField()) }
        return session
    }

    /**
     * Mirrors an undone repair in the open form. Numbers stay digits-only; expiry and CVV keep
     * the raw old text so the validation error shows what is wrong.
     */
    fun restoreField(field: Field, value: String): CardFormSession {
        val next = when (field) {
            Field.NUMBER -> form.copy(number = value.filter { it in '0'..'9' })
            Field.EXPIRY -> form.copy(expiry = value)
            Field.CVV -> form.copy(cvv = value)
            Field.HOLDER, Field.BANK -> form
        }
        return withForm(next).validate(field)
    }

    fun restoreRepair(field: CardField, before: String): CardFormSession =
        restoreField(field.toFormField(), before)

    fun selectHolder(id: String): CardFormSession =
        withForm(form.copy(holderId = id)).validate(Field.HOLDER)

    fun selectBank(id: String): CardFormSession =
        withForm(form.copy(bankId = id)).validate(Field.BANK)

    fun selectType(id: String?): CardFormSession =
        withForm(form.copy(typeId = id))

    fun selectBand(key: String): CardFormSession =
        withForm(form.copy(bandColor = key))

    fun toggleTag(id: String): CardFormSession {
        val ids = when {
            id in form.tagIds -> form.tagIds - id
            form.tagIds.size < lists.tagLimit -> form.tagIds + id
            else -> form.tagIds
        }
        return withForm(form.copy(tagIds = ids))
    }

    fun addTag(id: String): CardFormSession {
        if (id in form.tagIds || form.tagIds.size >= lists.tagLimit) return this
        return withForm(form.copy(tagIds = form.tagIds + id))
    }

    fun removeTag(id: String): CardFormSession =
        withForm(form.copy(tagIds = form.tagIds - id))

    fun canAddTag(): Boolean = form.tagIds.size < lists.tagLimit

    /**
     * List updates never replace the chosen holder or bank, except a new form opened before
     * holders loaded, which adopts the first one and stays clean.
     */
    fun withLists(lists: CardFormLists, options: CardFormOptions): CardFormSession {
        var next = copy(lists = lists, options = options)
        if (form.mode == CardFormMode.ADD && form.holderId == null && lists.holders.isNotEmpty()) {
            val holderId = lists.holders.first().id
            next = next.copy(
                form = next.form.copy(holderId = holderId),
                initialForm = next.initialForm?.copy(holderId = holderId)
            )
        }
        return next
    }

    companion object {
        fun forAdd(
            lists: CardFormLists,
            options: CardFormOptions,
            today: YearMonth = YearMonth.now(),
            preferredHolderId: String? = null
        ): CardFormSession {
            val holder = lists.holders.firstOrNull { it.id == preferredHolderId }
                ?: lists.holders.firstOrNull()
            val form = CardFormModel(
                mode = CardFormMode.ADD,
                holderId = holder?.id,
                bandColor = BandColor.NAVY
            )
            return CardFormSession(
                form = form,
                lists = lists,
                options = options,
                today = today,
                initialForm = form
            )
        }

        fun forEdit(
            card: VaultCard,
            lists: CardFormLists,
            options: CardFormOptions,
            today: YearMonth = YearMonth.now()
        ): CardFormSession {
            val form = CardFormModel.fromCard(card)
            return CardFormSession(
                form = form,
                lists = lists,
                options = options,
                today = today,
                reviewFields = card.needsReview,
                archived = card.archived,
                archiveReason = card.archiveReason,
                initialForm = form
            )
        }
    }
}
