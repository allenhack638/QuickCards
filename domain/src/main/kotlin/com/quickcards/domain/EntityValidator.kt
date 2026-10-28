package com.quickcards.domain

import java.util.Locale

enum class EntityKind {
    HOLDER,
    BANK,
    NETWORK,
    TYPE,
    TAG
}

enum class EntityField {
    NAME,
    CODE,
    PREFIXES,
    LENGTHS
}

/** A validation failure without copy; the UI maps each case to a string resource. */
sealed interface EntityValidationError {
    data object NameRequired : EntityValidationError
    data object NameTooShort : EntityValidationError
    data object NameTaken : EntityValidationError
    data object CodeFormat : EntityValidationError
    data object CodeTaken : EntityValidationError
    data object PrefixesFormat : EntityValidationError
    data object LengthsRange : EntityValidationError
}

data class EntityRef(
    val name: String,
    val id: String? = null,
    val code: String? = null
)

data class EntityDraft(
    val kind: EntityKind,
    val name: String,
    val code: String = "",
    val prefixes: String = "",
    val lengths: String = "16",
    val cvvLength: Int = 3,
    val luhn: Boolean = true
)

object EntityValidator {

    const val HOLDER_NAME_MAX = 40
    const val BANK_NAME_MAX = 40
    const val NETWORK_NAME_MAX = 24
    const val TYPE_NAME_MAX = 20
    const val DEFAULT_TAG_MAX_LENGTH = 16

    private val SPACES = Regex("""\s+""")
    private val LEADING_HASHES = Regex("""^#+""")
    private val CODE = Regex("""^[A-Z0-9 ]{2,$NetworkLabelMax}$""")
    private val NUMBERS_AND_RANGES = Regex("""^\d+(-\d+)?(,\d+(-\d+)?)*$""")

    fun nameLimit(kind: EntityKind, tagMaxLength: Int = DEFAULT_TAG_MAX_LENGTH): Int = when (kind) {
        EntityKind.HOLDER -> HOLDER_NAME_MAX
        EntityKind.BANK -> BANK_NAME_MAX
        EntityKind.NETWORK -> NETWORK_NAME_MAX
        EntityKind.TYPE -> TYPE_NAME_MAX
        EntityKind.TAG -> tagMaxLength
    }

    fun normalizeName(kind: EntityKind, raw: String): String {
        val collapsed = raw.trim().replace(SPACES, " ")
        return if (kind == EntityKind.TAG) collapsed.replace(LEADING_HASHES, "") else collapsed
    }

    fun validate(
        draft: EntityDraft,
        existing: List<EntityRef> = emptyList(),
        editingId: String? = null
    ): Map<EntityField, EntityValidationError> {
        val errors = linkedMapOf<EntityField, EntityValidationError>()
        val name = normalizeName(draft.kind, draft.name)

        when {
            name.isEmpty() -> errors[EntityField.NAME] = EntityValidationError.NameRequired
            name.length < 2 -> errors[EntityField.NAME] = EntityValidationError.NameTooShort
            existing.any { isOtherItem(it, editingId) && it.name.equals(name, ignoreCase = true) } ->
                errors[EntityField.NAME] = EntityValidationError.NameTaken
        }

        if (draft.kind == EntityKind.NETWORK) {
            val code = draft.code.trim().uppercase(Locale.ROOT)
            when {
                !CODE.matches(code) ->
                    errors[EntityField.CODE] = EntityValidationError.CodeFormat

                existing.any { isOtherItem(it, editingId) && it.code == code } ->
                    errors[EntityField.CODE] = EntityValidationError.CodeTaken
            }

            val prefixes = draft.prefixes.replace(SPACES, "")
            if (prefixes.length > NetworkRuleSanitizer.MAX_PREFIX_SPEC ||
                !NUMBERS_AND_RANGES.matches(prefixes)
            ) {
                errors[EntityField.PREFIXES] = EntityValidationError.PrefixesFormat
            }

            val lengths = draft.lengths.replace(SPACES, "")
            if (!NetworkRuleSanitizer.isValidLengths(lengths)) {
                errors[EntityField.LENGTHS] = EntityValidationError.LengthsRange
            }
        }

        return errors
    }

    private fun isOtherItem(ref: EntityRef, editingId: String?): Boolean =
        editingId == null || ref.id != editingId
}
