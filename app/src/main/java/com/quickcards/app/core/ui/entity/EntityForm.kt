package com.quickcards.app.core.ui.entity

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.quickcards.app.R
import com.quickcards.domain.CardNetwork as RuleNetwork
import com.quickcards.domain.CardNumberRules
import com.quickcards.domain.DefaultNetworks
import com.quickcards.domain.EntityBefore
import com.quickcards.domain.EntityDraft
import com.quickcards.domain.EntityField
import com.quickcards.domain.EntityKind
import com.quickcards.domain.EntityValidationError
import com.quickcards.domain.EntityValidator
import com.quickcards.domain.NetworkLabelMax
import com.quickcards.domain.NetworkRule
import com.quickcards.domain.NetworkRules
import java.util.Locale

private fun field(text: String) = TextFieldValue(text, TextRange(text.length))

/** The editable values of one list item, with change, validity and standard-rule checks. */
internal class EntityForm(
    val kind: EntityKind,
    val editingId: String?,
    val before: EntityBefore?,
    private val nameMax: Int,
    /** A bank's colour when the editor opened (stored or built-in default). */
    val beforeColour: String? = null
) {
    var name by mutableStateOf(field(before?.name.orEmpty()))
        private set
    var code by mutableStateOf(field(before?.code.orEmpty()))
        private set
    var prefixes by mutableStateOf(field(before?.prefixes.orEmpty()))
    var lengths by mutableStateOf(field(before?.lengths ?: DEFAULT_LENGTHS))
    var cvvLength by mutableIntStateOf(before?.cvvLength ?: DEFAULT_CVV)
    var luhn by mutableStateOf(before?.luhn ?: true)
    var cvvOptional by mutableStateOf(before?.cvvOptional ?: false)

    /** A bank colour picked by hand; null keeps the stored or automatic one. */
    var colour by mutableStateOf<String?>(null)

    /** The built-in network this item started as, or null for every other item. */
    val standard: RuleNetwork? = if (kind == EntityKind.NETWORK) DefaultNetworks.standard(editingId) else null

    fun updateName(value: TextFieldValue) {
        name = field(value.text.take(nameMax))
    }

    fun updateCode(value: TextFieldValue) {
        code = field(value.text.uppercase(Locale.ROOT).take(NetworkLabelMax))
    }

    fun draft() = EntityDraft(
        kind = kind,
        name = name.text,
        code = code.text,
        prefixes = prefixes.text,
        lengths = lengths.text,
        cvvLength = cvvLength,
        luhn = luhn,
        cvvOptional = cvvOptional
    )

    val dirty: Boolean
        get() = before == null ||
            name.text != before.name ||
            code.text != before.code ||
            prefixes.text != before.prefixes ||
            lengths.text != before.lengths ||
            cvvLength != before.cvvLength ||
            luhn != before.luhn ||
            cvvOptional != before.cvvOptional ||
            colour != null && colour != beforeColour

    private fun rules() = NetworkRules(prefixes.text, lengths.text, cvvLength, luhn, cvvOptional)

    private fun changed(rules: NetworkRules): Set<NetworkRule> =
        standard?.let { DefaultNetworks.changedRules(it.id, rules) }.orEmpty()

    /** True while a built-in network differs from its standard rules. */
    val custom: Boolean get() = changed(rules()).isNotEmpty()

    /** True when saving moves a built-in network further from its standard rules. */
    val newlyCustom: Boolean
        get() {
            val saved = before?.let { NetworkRules(it.prefixes, it.lengths, it.cvvLength, it.luhn, it.cvvOptional) }
            return (changed(rules()) - (saved?.let(::changed).orEmpty())).isNotEmpty()
        }

    fun resetToStandard() {
        val network = standard ?: return
        prefixes = field(network.prefixes)
        lengths = field(network.lengths)
        cvvLength = network.cvvLength
        luhn = network.luhn
        cvvOptional = network.cvvOptional
    }

    /** Only a new name or card label needs the cards-affected confirm after the rules warning. */
    fun keepsNameAndCode(): Boolean =
        EntityValidator.normalizeName(kind, name.text) == before?.name &&
            code.text.trim().uppercase(Locale.ROOT) == before?.code

    private companion object {
        const val DEFAULT_LENGTHS = "16"
        const val DEFAULT_CVV = 3
    }
}

@Composable
internal fun rememberEntityForm(
    kind: EntityKind,
    editingId: String?,
    before: EntityBefore?,
    nameMax: Int,
    beforeColour: String? = null
): EntityForm = remember(kind, editingId) { EntityForm(kind, editingId, before, nameMax, beforeColour) }

@Composable
internal fun entityErrorText(error: EntityValidationError): String = stringResource(
    when (error) {
        EntityValidationError.NameRequired -> R.string.manage_error_name_required
        EntityValidationError.NameTooShort -> R.string.manage_error_name_too_short
        EntityValidationError.NameTaken -> R.string.manage_error_name_taken
        EntityValidationError.CodeFormat -> R.string.manage_error_code_format
        EntityValidationError.CodeTaken -> R.string.manage_error_code_taken
        EntityValidationError.PrefixesFormat -> R.string.manage_error_prefixes_format
        EntityValidationError.LengthsRange -> R.string.manage_error_lengths_range
    }
)

/** Errors worth showing on a field: never for an empty one (the Save line names it). */
internal fun Map<EntityField, EntityValidationError>.visible(form: EntityForm): Map<EntityField, EntityValidationError> =
    filter { (field, _) ->
        when (field) {
            EntityField.NAME -> form.name.text.isNotBlank()
            EntityField.CODE -> form.code.text.isNotBlank()
            EntityField.PREFIXES -> form.prefixes.text.isNotBlank()
            EntityField.LENGTHS -> form.lengths.text.isNotBlank()
        }
    }

/** "starts with 4; 13, 16 or 19 digits; 3‑digit CVV; last‑digit check". */
@Composable
internal fun standardRulesText(standard: RuleNetwork): String {
    val parts = NetworkRule.entries.map { rule ->
        when (rule) {
            NetworkRule.PREFIXES -> stringResource(R.string.manage_rule_prefixes, standard.prefixes.replace(",", ", "))
            NetworkRule.LENGTHS -> stringResource(R.string.manage_rule_lengths, CardNumberRules.lengthsText(standard.lengths))
            NetworkRule.CVV -> pluralStringResource(
                if (standard.cvvOptional) R.plurals.manage_rule_cvv_optional else R.plurals.manage_rule_cvv,
                standard.cvvLength,
                standard.cvvLength
            )
            NetworkRule.LUHN -> stringResource(
                if (standard.luhn) R.string.manage_rule_luhn_on else R.string.manage_rule_luhn_off
            )
        }
    }
    return parts.joinToString(stringResource(R.string.manage_rule_join))
}
