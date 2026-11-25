package com.quickcards.app.feature.cardform

import com.quickcards.app.core.data.model.VaultCard
import com.quickcards.app.core.data.settings.AppSettingsDefaults
import com.quickcards.domain.ArtRules
import com.quickcards.domain.CardDraft
import com.quickcards.domain.CardError
import com.quickcards.domain.CardField
import com.quickcards.domain.CardNetwork
import com.quickcards.domain.CardNumberRules
import com.quickcards.domain.CardValidator
import com.quickcards.domain.DefaultNetworks
import com.quickcards.domain.ExistingCardRef
import com.quickcards.domain.Field
import com.quickcards.domain.NamedRef
import com.quickcards.domain.NetworkRule
import java.time.YearMonth
import java.util.Locale

enum class CardFormMode { ADD, EDIT }

private fun CardField.toFormField(): Field = when (this) {
    CardField.NUMBER -> Field.NUMBER
    CardField.EXPIRY -> Field.EXPIRY
    CardField.CVV -> Field.CVV
}

/**
 * Keeps an imported custom name-on-card, but re-syncs an auto-synced one to the current
 * holder. [baseOwnerName] is the holder name stored on the card at its last save, so a holder
 * rename can be told apart from a genuinely custom imported name.
 */
internal fun resolveNameOnCard(
    baseNameOnCard: String?,
    baseOwnerName: String?,
    currentHolderName: String
): String {
    val current = currentHolderName.trim().uppercase(Locale.ROOT)
    val custom = baseNameOnCard
        ?.takeIf { it.isNotBlank() }
        ?.takeIf { baseOwnerName.isNullOrBlank() || !it.equals(baseOwnerName, ignoreCase = true) }
    return custom ?: current
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

/** The one line above a disabled Save: the first missing or wrong item, in form order. */
enum class SaveIssue {
    HOLDER, NUMBER_MISSING, NUMBER_BAD, EXPIRY_MISSING, EXPIRY_BAD, CVV_MISSING, CVV_BAD, BANK, TYPE, STATEMENT_DAY, DUE_DAY
}

/** A saved card with the same number: its id (for Open) and bank name. */
data class DuplicateRef(val cardId: String, val bankName: String)

/** A changed built-in network rule that is blocking the card, and which field it blocks. */
data class BlockingRule(val network: CardNetwork, val kind: Kind) {
    enum class Kind { NUMBER, CVV }
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
    val bandColor: String = ArtRules.GRAPHITE,
    /** True once the colour was chosen by hand; otherwise it follows the bank. */
    val bandColorManual: Boolean = false,
    val note: String = "",
    val statementDay: String = "",
    val dueDay: String = "",
    val errors: Map<Field, CardError> = emptyMap(),
    val tried: Boolean = false
) {
    val isEdit: Boolean get() = mode == CardFormMode.EDIT

    /** Empty means no reminder; anything else must be a day of the month. */
    val dueDayValid: Boolean get() = dueDay.isEmpty() || dueDay.toIntOrNull() in 1..31

    val statementDayValid: Boolean get() = statementDay.isEmpty() || statementDay.toIntOrNull() in 1..31

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
        bandColorManual,
        note,
        statementDay,
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
            Field.BANK,
            Field.TYPE
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
            bandColorManual = card.bandColorManual,
            note = card.note,
            statementDay = card.statementDay?.toString().orEmpty(),
            dueDay = card.dueDay?.toString().orEmpty()
        )
    }
}

data class CardFormLists(
    val holders: List<NamedRef> = emptyList(),
    val banks: List<NamedRef> = emptyList(),
    /** Each bank's colour key (stored, else its built-in default). */
    val bankColours: Map<String, String> = emptyMap(),
    val networks: List<CardNetwork> = emptyList(),
    val types: List<NamedRef> = emptyList(),
    val tags: List<NamedRef> = emptyList(),
    val tagLimit: Int = AppSettingsDefaults.TAGS_MAX,
    val tagMaxLen: Int = AppSettingsDefaults.TAGS_MAX_LEN,
    val duplicates: Map<String, DuplicateRef> = emptyMap(),
    /** Custom colours on other cards, most recently saved first, for the Custom view. */
    val recentColours: List<String> = emptyList()
) {
    fun holderName(id: String?): String? = holders.firstOrNull { it.id == id }?.name
    fun bankName(id: String?): String? = banks.firstOrNull { it.id == id }?.name
    fun typeName(id: String?): String? = types.firstOrNull { it.id == id }?.name
    fun tagName(id: String): String? = tags.firstOrNull { it.id == id }?.name

    fun withHolder(id: String, name: String): CardFormLists = copy(
        holders = (holders.filterNot { it.id == id } + NamedRef(id, name)).sortedBy { it.name.lowercase(Locale.ROOT) }
    )

    fun withBank(id: String, name: String, colour: String?): CardFormLists = copy(
        banks = (banks.filterNot { it.id == id } + NamedRef(id, name)).sortedBy { it.name.lowercase(Locale.ROOT) },
        bankColours = if (colour == null) bankColours else bankColours + (id to colour)
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
    private val baseNumber: String? = null,
    private val baseNetworkId: String? = null,
    private val baseNetworkAuto: Boolean = true,
    private val initialForm: CardFormModel? = null
) {

    /**
     * The network shown and reported by the form. An unchanged number keeps an imported
     * network override; editing the number falls back to detection.
     */
    fun network(networks: List<CardNetwork>): CardNetwork? =
        assignedNetwork(networks) ?: form.network(networks)

    /** The stored explicit network while the number is unchanged; display, cap and checks share it. */
    private fun assignedNetwork(networks: List<CardNetwork>): CardNetwork? =
        if (form.number == baseNumber && !baseNetworkAuto) {
            networks.firstOrNull { it.id == baseNetworkId }
        } else {
            null
        }

    private fun withForm(form: CardFormModel): CardFormSession = copy(form = form)

    /** True once an editable value differs from the state the screen opened with. */
    fun isDirty(): Boolean {
        val initial = initialForm ?: return false
        return form.editableValues() != initial.editableValues()
    }

    private fun allErrors(): Map<Field, CardError> = CardValidator.validate(
        draft = form.toDraft(),
        networks = lists.networks,
        today = today,
        existing = { number -> lists.duplicates[number]?.let { ExistingCardRef(it.bankName) } },
        assigned = assignedNetwork(lists.networks)
    )

    fun validate(field: Field): CardFormSession = withForm(form.withError(field, allErrors()[field]))

    fun saveAttempt(): CardFormSession = withForm(form.copy(errors = allErrors(), tried = true))

    /** A past expiry is a warning ("Expired"), never a block. */
    fun firstInvalid(): Field? = CardFormModel.FIELD_ORDER.firstOrNull { blocks(form.errors[it]) }

    val expiryWarning: Boolean get() = form.errors[Field.EXPIRY] == CardError.Expired

    /** The saved card this number duplicates, for the error's Open action. */
    fun duplicateOf(): DuplicateRef? =
        if (form.errors[Field.NUMBER] is CardError.Duplicate) lists.duplicates[form.number] else null

    /** Null when every required field is valid; else the first problem in form order. */
    fun saveIssue(): SaveIssue? {
        val errors = allErrors()
        val cvvWanted = network(lists.networks)?.cvvLength ?: 3
        val cvvOptional = DefaultNetworks.cvvOptional(network(lists.networks))
        return when {
            Field.HOLDER in errors -> SaveIssue.HOLDER
            form.number.isEmpty() -> SaveIssue.NUMBER_MISSING
            Field.NUMBER in errors -> SaveIssue.NUMBER_BAD
            form.expiry.isEmpty() -> SaveIssue.EXPIRY_MISSING
            blocks(errors[Field.EXPIRY]) -> SaveIssue.EXPIRY_BAD
            !cvvOptional && form.cvv.length < cvvWanted -> SaveIssue.CVV_MISSING
            Field.CVV in errors -> SaveIssue.CVV_BAD
            Field.BANK in errors -> SaveIssue.BANK
            Field.TYPE in errors -> SaveIssue.TYPE
            !form.statementDayValid -> SaveIssue.STATEMENT_DAY
            !form.dueDayValid -> SaveIssue.DUE_DAY
            else -> null
        }
    }

    /** Save is on once the form is valid; Edit also needs a real change. */
    fun canSave(): Boolean = saveIssue() == null && (!form.isEdit || isDirty())

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
        session = if (next.number.length >= usualLength(network)) {
            session.validate(Field.NUMBER)
        } else {
            session.revalidate(Field.NUMBER)
        }
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
        val session = withForm(next)
        return if (next.expiry.length == EXPIRY_LENGTH) session.validate(Field.EXPIRY) else session.revalidate(Field.EXPIRY)
    }

    fun expiryBlur(): CardFormSession =
        if (form.expiry.isNotBlank() || form.tried) validate(Field.EXPIRY) else this

    fun updateCvv(raw: String): CardFormSession {
        val max = network(lists.networks)?.cvvLength ?: 4
        val next = form.copy(cvv = raw.filter { it in '0'..'9' }.take(max))
        val session = withForm(next)
        return if (next.cvv.length == max) session.validate(Field.CVV) else session.revalidate(Field.CVV)
    }

    fun cvvBlur(): CardFormSession =
        if (form.cvv.isNotBlank() || form.tried) validate(Field.CVV) else this

    /** Digits only, at most two; an out-of-range day is kept and flagged so typing never vanishes. */
    fun updateDueDay(raw: String): CardFormSession =
        withForm(form.copy(dueDay = raw.filter(Char::isDigit).take(2)))

    fun updateStatementDay(raw: String): CardFormSession =
        withForm(form.copy(statementDay = raw.filter(Char::isDigit).take(2)))

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
            Field.HOLDER, Field.BANK, Field.TYPE -> form
        }
        return withForm(next).validate(field)
    }

    fun restoreRepair(field: CardField, before: String): CardFormSession =
        restoreField(field.toFormField(), before)

    fun selectHolder(id: String): CardFormSession =
        withForm(form.copy(holderId = id)).validate(Field.HOLDER)

    /** Picking a bank applies its colour unless this card's colour was chosen by hand. */
    fun selectBank(id: String): CardFormSession {
        val colour = if (form.bandColorManual) form.bandColor else lists.bankColours[id] ?: form.bandColor
        return withForm(form.copy(bankId = id, bandColor = colour)).validate(Field.BANK)
    }

    fun bankColour(): String? = form.bankId?.let { lists.bankColours[it] }

    fun selectType(id: String?): CardFormSession =
        withForm(form.copy(typeId = id)).validate(Field.TYPE)

    /** A colour picked in the sheet sticks to the card, unless it is the bank's own colour. */
    fun selectBand(key: String): CardFormSession =
        withForm(form.copy(bandColor = key, bandColorManual = key != bankColour()))

    /** "Bank colour": follow the bank again. */
    fun followBankColour(): CardFormSession =
        withForm(form.copy(bandColor = bankColour() ?: form.bandColor, bandColorManual = false))

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
        // The bank was recoloured elsewhere: a card that follows it follows the new colour.
        val bankColour = form.bankId?.let { lists.bankColours[it] }
        val before = form.bankId?.let { this.lists.bankColours[it] }
        if (!form.bandColorManual && bankColour != null && before != null && bankColour != before) {
            next = next.copy(form = next.form.copy(bandColor = bankColour))
        }
        // A network rule changed elsewhere (for example Reset to standard): refresh shown errors.
        if (lists.networks != this.lists.networks) {
            next = next.revalidate(Field.NUMBER).revalidate(Field.CVV)
        }
        return next
    }

    /**
     * A changed built-in rule is exactly why the number or CVV fails (a typo or duplicate never
     * counts), so the form can name it and offer Reset to standard.
     */
    fun changedRulesBlocking(): BlockingRule? {
        val network = network(lists.networks) ?: return null
        val changed = DefaultNetworks.changedRules(network)
        if (changed.isEmpty()) return null
        val cvvBlocked = form.errors[Field.CVV] is CardError.CvvLength && NetworkRule.CVV in changed
        val numberBlocked = when (form.errors[Field.NUMBER]) {
            is CardError.NetworkLength -> NetworkRule.LENGTHS in changed
            CardError.Luhn -> NetworkRule.LUHN in changed
            else -> false
        }
        return when {
            cvvBlocked -> BlockingRule(network, BlockingRule.Kind.CVV)
            numberBlocked -> BlockingRule(network, BlockingRule.Kind.NUMBER)
            else -> null
        }
    }

    private fun usualLength(network: CardNetwork?): Int {
        val lengths = network?.let { CardNumberRules.parseLengths(it.lengths) }.orEmpty()
        return when {
            lengths.isEmpty() || USUAL_LENGTH in lengths -> USUAL_LENGTH
            else -> lengths.max()
        }
    }

    companion object {
        private const val USUAL_LENGTH = 16
        private const val EXPIRY_LENGTH = 5

        private fun blocks(error: CardError?): Boolean = error != null && error != CardError.Expired

        fun forAdd(
            lists: CardFormLists,
            options: CardFormOptions,
            today: YearMonth = YearMonth.now(),
            preferredHolderId: String? = null
        ): CardFormSession {
            val holder = lists.holders.firstOrNull { it.id == preferredHolderId }
                ?: lists.holders.firstOrNull()
            val form = CardFormModel(mode = CardFormMode.ADD, holderId = holder?.id)
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
                baseNumber = card.number,
                baseNetworkId = card.networkId,
                baseNetworkAuto = card.networkAuto,
                initialForm = form
            )
        }
    }
}
