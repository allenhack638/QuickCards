package com.quickcards.app.feature.archive

import com.quickcards.app.core.data.settings.AppSettingsDefaults
import com.quickcards.app.core.ui.HomeCard
import java.util.Locale

/** One archived card with its optional, decrypted archive reason. */
data class ArchivedCardItem(
    val card: HomeCard,
    val reason: String?
) {
    val id: String get() = card.id
}

data class ArchivedCardsOptions(
    val maskNumbers: Boolean = AppSettingsDefaults.MASK_NUMBERS,
    val tagsShow: Boolean = AppSettingsDefaults.TAGS_SHOW,
    val showNotes: Boolean = AppSettingsDefaults.SHOW_NOTES,
    val clipClearSec: Int = AppSettingsDefaults.CLIP_CLEAR_SEC,
    val notifEnabled: Boolean = AppSettingsDefaults.NOTIF_ENABLED,
    val notifSec: Int = AppSettingsDefaults.NOTIF_SEC,
    val expiresSoonMonths: Int = AppSettingsDefaults.EXPIRES_SOON_MONTHS
)

data class ArchivedCardsUiState(
    val loading: Boolean = true,
    val cards: List<ArchivedCardItem> = emptyList(),
    val visibleCards: List<ArchivedCardItem> = emptyList(),
    val query: String = "",
    val searchOn: Boolean = false,
    val options: ArchivedCardsOptions = ArchivedCardsOptions()
)

/** Case-insensitive search over every card field, including the archive reason. */
object ArchivedCardsSearch {

    fun filter(cards: List<ArchivedCardItem>, query: String): List<ArchivedCardItem> {
        val text = query.trim().lowercase(Locale.ROOT)
        if (text.isEmpty()) return cards
        val digits = text.replace(Regex("\\s+"), "")
        val numeric = digits.isNotEmpty() && digits.all { it in '0'..'9' }
        return cards.filter { item ->
            val lastFour = item.card.card.number.takeLast(4)
            if (numeric && lastFour.contains(digits)) return@filter true
            val fields = buildList {
                add(item.card.bankName)
                add(item.card.holderName)
                add(item.card.card.note)
                add(item.card.typeName.orEmpty())
                add(item.card.network?.name.orEmpty())
                add(item.card.network?.code.orEmpty())
                add(item.reason.orEmpty())
                addAll(item.card.tagNames)
            }
            fields.any { it.lowercase(Locale.ROOT).contains(text) }
        }
    }
}

fun buildArchivedCardsState(
    cards: List<ArchivedCardItem>,
    query: String,
    searchOn: Boolean = false,
    options: ArchivedCardsOptions = ArchivedCardsOptions()
): ArchivedCardsUiState = ArchivedCardsUiState(
    loading = false,
    cards = cards,
    visibleCards = ArchivedCardsSearch.filter(cards, query),
    query = query,
    searchOn = searchOn,
    options = options
)
