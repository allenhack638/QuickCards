package com.quickcards.domain

import java.util.Locale

/** Minimal network data needed to resolve a card's network. */
data class UsageNetwork(
    val id: String,
    val prefixes: String
)

/**
 * Card facts used by the Manage screens. Network ids are already resolved
 * (auto-detected or manual) so usage counting is a pure lookup.
 */
data class UsageCard(
    val id: String,
    val number: String,
    val bankName: String,
    val typeName: String = "",
    val holderId: String?,
    val holderName: String,
    val bankId: String?,
    val typeId: String?,
    val networkId: String?,
    val tagIds: List<String>,
    val bandColor: String
) {
    val lastFour: String get() = number.takeLast(4)

    val title: String
        get() = if (typeName.isBlank()) bankName else "$bankName $typeName"

    val holderFirstName: String
        get() = holderName.trim().substringBefore(' ')
}

/** The editable values of an existing entity, used for change detection. */
data class EntityBefore(
    val name: String,
    val code: String = "",
    val prefixes: String = "",
    val lengths: String = "",
    val cvvLength: Int = 3,
    val luhn: Boolean = true
)

enum class EntityDeleteDecision {
    ALLOWED,
    BLOCKED
}

/**
 * Pure port of KINDS.used, the list subtitles, confirmUpdate's change test and
 * entityDelete's guard from prototype/v2.html.
 */
object EntityUsage {

    /**
     * netOf(c): a card with networkAuto follows the longest matching prefix;
     * otherwise the explicit network id wins.
     */
    fun resolveNetworkId(
        number: String,
        networkAuto: Boolean,
        networkId: String?,
        networks: List<UsageNetwork>
    ): String? {
        if (!networkAuto) return networkId
        var best: String? = null
        var bestScore = 0
        for (network in networks) {
            val score = CardNumberRules.prefixScore(number, network.prefixes)
            if (score > bestScore) {
                bestScore = score
                best = network.id
            }
        }
        return best
    }

    fun usedCards(kind: EntityKind, id: String, cards: List<UsageCard>): List<UsageCard> =
        when (kind) {
            EntityKind.HOLDER -> cards.filter { it.holderId == id }
            EntityKind.BANK -> cards.filter { it.bankId == id }
            EntityKind.NETWORK -> cards.filter { it.networkId == id }
            EntityKind.TYPE -> cards.filter { it.typeId == id }
            EntityKind.TAG -> cards.filter { id in it.tagIds }
        }

    fun counts(kind: EntityKind, ids: List<String>, cards: List<UsageCard>): Map<String, Int> =
        ids.associateWith { id -> usedCards(kind, id, cards).size }

    /**
     * confirmUpdate: only an edit of an in-use item that actually changes its
     * name or a network rule asks first. As in the prototype, the Luhn flag is
     * not part of the change test.
     */
    fun requiresUpdateConfirmation(
        kind: EntityKind,
        before: EntityBefore?,
        draft: EntityDraft,
        usedCount: Int
    ): Boolean {
        if (before == null || usedCount <= 0) return false
        val name = EntityValidator.normalizeName(kind, draft.name)
        if (before.name != name) return true
        if (kind != EntityKind.NETWORK) return false
        val code = draft.code.trim().uppercase(Locale.ROOT)
        val prefixes = draft.prefixes.replace(WHITESPACE, "")
        val lengths = draft.lengths.replace(WHITESPACE, "")
        return before.code != code ||
            before.prefixes != prefixes ||
            before.lengths != lengths ||
            before.cvvLength != draft.cvvLength
    }

    fun deleteDecision(usedCount: Int): EntityDeleteDecision =
        if (usedCount > 0) EntityDeleteDecision.BLOCKED else EntityDeleteDecision.ALLOWED

    private val WHITESPACE = Regex("""\s+""")
}
