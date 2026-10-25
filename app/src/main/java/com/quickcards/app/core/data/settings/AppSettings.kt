package com.quickcards.app.core.data.settings

import android.content.Context
import android.content.SharedPreferences
import com.quickcards.domain.ArchivedVisibility
import com.quickcards.domain.ExpiredPlacement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class AppTheme {
    SYSTEM,
    LIGHT,
    DARK
}

/** Defaults and allowed values; must match CFG0 in prototype/v2.html. */
object AppSettingsDefaults {
    val THEME = AppTheme.DARK
    const val DYNAMIC_COLOR = false

    const val COPY_WINDOW_SEC = 60
    val COPY_WINDOW_OPTIONS = listOf(30, 60, 120)

    const val CLIP_CLEAR_SEC = 60
    val CLIP_CLEAR_OPTIONS = listOf(0, 30, 60, 120)

    const val MASK_NUMBERS = true
    const val SHOW_CARD_TYPE = true

    const val PINNED_MAX = 3
    val PINNED_MAX_OPTIONS = listOf(1, 3, 5, 10)

    val EXPIRED_CARDS = ExpiredPlacement.END
    val EXPIRED_CARDS_OPTIONS = listOf(
        ExpiredPlacement.KEEP,
        ExpiredPlacement.END,
        ExpiredPlacement.HIDE
    )

    const val EXPIRES_SOON_MONTHS = 1
    val EXPIRES_SOON_MONTHS_OPTIONS = listOf(1, 2, 3)

    val ARCHIVED_CARDS = ArchivedVisibility.ONLY_ARCHIVE
    val ARCHIVED_CARDS_OPTIONS = listOf(
        ArchivedVisibility.ONLY_ARCHIVE,
        ArchivedVisibility.IN_SEARCH
    )

    const val SHOW_NOTES = true

    const val NOTIF_ENABLED = true
    const val NOTIF_SEC = 30
    val NOTIF_SEC_OPTIONS = listOf(15, 30, 60)
    const val NOTIF_SHOW_EXPIRY = true
    const val NOTIF_SHOW_CVV = true

    const val TAGS_SHOW = true
    const val TAGS_SEARCH = true
    const val TAGS_MAX = 3
    val TAGS_MAX_OPTIONS = listOf(1, 2, 3, 5, 8, 10)
    const val TAGS_MAX_LEN = 16
    val TAGS_MAX_LEN_OPTIONS = listOf(8, 12, 16, 24)

    const val APP_LOCK_ENABLED = true
    const val APP_LOCK_AFTER_SEC = 60
    val APP_LOCK_AFTER_OPTIONS = listOf(10, 30, 60, 120, 300, 600, 1800, 0)

    const val LOCK_ON_LEAVE = true
}

class AppSettings(private val preferences: SharedPreferences) {

    private val _theme = MutableStateFlow(readTheme())
    val theme: StateFlow<AppTheme> = _theme.asStateFlow()

    private val _dynamicColor = MutableStateFlow(
        preferences.getBoolean(KEY_DYNAMIC_COLOR, AppSettingsDefaults.DYNAMIC_COLOR)
    )
    val dynamicColor: StateFlow<Boolean> = _dynamicColor.asStateFlow()

    private val _copyWindowSec = MutableStateFlow(
        readInt(KEY_COPY_WINDOW_SEC, AppSettingsDefaults.COPY_WINDOW_SEC, AppSettingsDefaults.COPY_WINDOW_OPTIONS)
    )
    val copyWindowSec: StateFlow<Int> = _copyWindowSec.asStateFlow()

    private val _clipClearSec = MutableStateFlow(
        readInt(KEY_CLIP_CLEAR_SEC, AppSettingsDefaults.CLIP_CLEAR_SEC, AppSettingsDefaults.CLIP_CLEAR_OPTIONS)
    )
    val clipClearSec: StateFlow<Int> = _clipClearSec.asStateFlow()

    private val _maskNumbers = MutableStateFlow(
        preferences.getBoolean(KEY_MASK_NUMBERS, AppSettingsDefaults.MASK_NUMBERS)
    )
    val maskNumbers: StateFlow<Boolean> = _maskNumbers.asStateFlow()

    private val _showCardType = MutableStateFlow(
        preferences.getBoolean(KEY_SHOW_CARD_TYPE, AppSettingsDefaults.SHOW_CARD_TYPE)
    )
    val showCardType: StateFlow<Boolean> = _showCardType.asStateFlow()

    private val _pinnedMax = MutableStateFlow(
        readInt(KEY_PINNED_MAX, AppSettingsDefaults.PINNED_MAX, AppSettingsDefaults.PINNED_MAX_OPTIONS)
    )
    val pinnedMax: StateFlow<Int> = _pinnedMax.asStateFlow()

    private val _expiredCards = MutableStateFlow(readExpiredPlacement())
    val expiredCards: StateFlow<ExpiredPlacement> = _expiredCards.asStateFlow()

    private val _expiresSoonMonths = MutableStateFlow(
        readInt(
            KEY_EXPIRES_SOON_MONTHS,
            AppSettingsDefaults.EXPIRES_SOON_MONTHS,
            AppSettingsDefaults.EXPIRES_SOON_MONTHS_OPTIONS
        )
    )
    val expiresSoonMonths: StateFlow<Int> = _expiresSoonMonths.asStateFlow()

    private val _archivedCards = MutableStateFlow(readArchivedVisibility())
    val archivedCards: StateFlow<ArchivedVisibility> = _archivedCards.asStateFlow()

    private val _showNotes = MutableStateFlow(
        preferences.getBoolean(KEY_SHOW_NOTES, AppSettingsDefaults.SHOW_NOTES)
    )
    val showNotes: StateFlow<Boolean> = _showNotes.asStateFlow()

    private val _notifEnabled = MutableStateFlow(
        preferences.getBoolean(KEY_NOTIF_ENABLED, AppSettingsDefaults.NOTIF_ENABLED)
    )
    val notifEnabled: StateFlow<Boolean> = _notifEnabled.asStateFlow()

    private val _notifSec = MutableStateFlow(
        readInt(KEY_NOTIF_SEC, AppSettingsDefaults.NOTIF_SEC, AppSettingsDefaults.NOTIF_SEC_OPTIONS)
    )
    val notifSec: StateFlow<Int> = _notifSec.asStateFlow()

    private val _notifShowExpiry = MutableStateFlow(
        preferences.getBoolean(KEY_NOTIF_SHOW_EXPIRY, AppSettingsDefaults.NOTIF_SHOW_EXPIRY)
    )
    val notifShowExpiry: StateFlow<Boolean> = _notifShowExpiry.asStateFlow()

    private val _notifShowCvv = MutableStateFlow(
        preferences.getBoolean(KEY_NOTIF_SHOW_CVV, AppSettingsDefaults.NOTIF_SHOW_CVV)
    )
    val notifShowCvv: StateFlow<Boolean> = _notifShowCvv.asStateFlow()

    private val _tagsShow = MutableStateFlow(
        preferences.getBoolean(KEY_TAGS_SHOW, AppSettingsDefaults.TAGS_SHOW)
    )
    val tagsShow: StateFlow<Boolean> = _tagsShow.asStateFlow()

    private val _tagsSearch = MutableStateFlow(
        preferences.getBoolean(KEY_TAGS_SEARCH, AppSettingsDefaults.TAGS_SEARCH)
    )
    val tagsSearch: StateFlow<Boolean> = _tagsSearch.asStateFlow()

    private val _tagsMax = MutableStateFlow(
        readInt(KEY_TAGS_MAX, AppSettingsDefaults.TAGS_MAX, AppSettingsDefaults.TAGS_MAX_OPTIONS)
    )
    val tagsMax: StateFlow<Int> = _tagsMax.asStateFlow()

    private val _tagsMaxLen = MutableStateFlow(
        readInt(KEY_TAGS_MAX_LEN, AppSettingsDefaults.TAGS_MAX_LEN, AppSettingsDefaults.TAGS_MAX_LEN_OPTIONS)
    )
    val tagsMaxLen: StateFlow<Int> = _tagsMaxLen.asStateFlow()

    private val _appLockEnabled = MutableStateFlow(
        preferences.getBoolean(KEY_APP_LOCK_ENABLED, AppSettingsDefaults.APP_LOCK_ENABLED)
    )
    val appLockEnabled: StateFlow<Boolean> = _appLockEnabled.asStateFlow()

    private val _appLockAfterSec = MutableStateFlow(
        readInt(KEY_APP_LOCK_AFTER_SEC, AppSettingsDefaults.APP_LOCK_AFTER_SEC, AppSettingsDefaults.APP_LOCK_AFTER_OPTIONS)
    )
    val appLockAfterSec: StateFlow<Int> = _appLockAfterSec.asStateFlow()

    private val _lockOnLeave = MutableStateFlow(
        preferences.getBoolean(KEY_LOCK_ON_LEAVE, AppSettingsDefaults.LOCK_ON_LEAVE)
    )
    val lockOnLeave: StateFlow<Boolean> = _lockOnLeave.asStateFlow()

    fun setTheme(theme: AppTheme) {
        preferences.edit().putString(KEY_THEME, theme.name).apply()
        _theme.value = theme
    }

    fun setDynamicColor(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_DYNAMIC_COLOR, enabled).apply()
        _dynamicColor.value = enabled
    }

    fun setCopyWindowSec(seconds: Int) {
        if (seconds !in AppSettingsDefaults.COPY_WINDOW_OPTIONS) return
        preferences.edit().putInt(KEY_COPY_WINDOW_SEC, seconds).apply()
        _copyWindowSec.value = seconds
    }

    fun setClipClearSec(seconds: Int) {
        if (seconds !in AppSettingsDefaults.CLIP_CLEAR_OPTIONS) return
        preferences.edit().putInt(KEY_CLIP_CLEAR_SEC, seconds).apply()
        _clipClearSec.value = seconds
    }

    fun setMaskNumbers(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_MASK_NUMBERS, enabled).apply()
        _maskNumbers.value = enabled
    }

    fun setShowCardType(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_SHOW_CARD_TYPE, enabled).apply()
        _showCardType.value = enabled
    }

    fun setPinnedMax(count: Int) {
        if (count !in AppSettingsDefaults.PINNED_MAX_OPTIONS) return
        preferences.edit().putInt(KEY_PINNED_MAX, count).apply()
        _pinnedMax.value = count
    }

    fun setExpiredCards(placement: ExpiredPlacement) {
        preferences.edit().putString(KEY_EXPIRED_CARDS, placement.name).apply()
        _expiredCards.value = placement
    }

    fun setExpiresSoonMonths(months: Int) {
        if (months !in AppSettingsDefaults.EXPIRES_SOON_MONTHS_OPTIONS) return
        preferences.edit().putInt(KEY_EXPIRES_SOON_MONTHS, months).apply()
        _expiresSoonMonths.value = months
    }

    fun setArchivedCards(visibility: ArchivedVisibility) {
        preferences.edit().putString(KEY_ARCHIVED_CARDS, visibility.name).apply()
        _archivedCards.value = visibility
    }

    fun setShowNotes(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_SHOW_NOTES, enabled).apply()
        _showNotes.value = enabled
    }

    fun setNotifEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_NOTIF_ENABLED, enabled).apply()
        _notifEnabled.value = enabled
    }

    fun setNotifSec(seconds: Int) {
        if (seconds !in AppSettingsDefaults.NOTIF_SEC_OPTIONS) return
        preferences.edit().putInt(KEY_NOTIF_SEC, seconds).apply()
        _notifSec.value = seconds
    }

    fun setNotifShowExpiry(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_NOTIF_SHOW_EXPIRY, enabled).apply()
        _notifShowExpiry.value = enabled
    }

    fun setNotifShowCvv(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_NOTIF_SHOW_CVV, enabled).apply()
        _notifShowCvv.value = enabled
    }

    fun setTagsShow(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_TAGS_SHOW, enabled).apply()
        _tagsShow.value = enabled
    }

    fun setTagsSearch(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_TAGS_SEARCH, enabled).apply()
        _tagsSearch.value = enabled
    }

    fun setTagsMax(count: Int) {
        if (count !in AppSettingsDefaults.TAGS_MAX_OPTIONS) return
        preferences.edit().putInt(KEY_TAGS_MAX, count).apply()
        _tagsMax.value = count
    }

    fun setTagsMaxLen(length: Int) {
        if (length !in AppSettingsDefaults.TAGS_MAX_LEN_OPTIONS) return
        preferences.edit().putInt(KEY_TAGS_MAX_LEN, length).apply()
        _tagsMaxLen.value = length
    }

    fun setAppLockEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_APP_LOCK_ENABLED, enabled).apply()
        _appLockEnabled.value = enabled
    }

    fun setAppLockAfterSec(seconds: Int) {
        if (seconds !in AppSettingsDefaults.APP_LOCK_AFTER_OPTIONS) return
        preferences.edit().putInt(KEY_APP_LOCK_AFTER_SEC, seconds).apply()
        _appLockAfterSec.value = seconds
    }

    fun setLockOnLeave(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_LOCK_ON_LEAVE, enabled).apply()
        _lockOnLeave.value = enabled
    }

    fun toMap(): Map<String, Any> = linkedMapOf(
        KEY_THEME_NAME to theme.value.name.lowercase(Locale.ROOT),
        KEY_DYNAMIC_COLOR_NAME to dynamicColor.value,
        KEY_COPY_WINDOW_SEC_NAME to copyWindowSec.value,
        KEY_CLIP_CLEAR_SEC_NAME to clipClearSec.value,
        KEY_MASK_NUMBERS_NAME to maskNumbers.value,
        KEY_SHOW_CARD_TYPE_NAME to showCardType.value,
        KEY_PINNED_MAX_NAME to pinnedMax.value,
        KEY_EXPIRED_CARDS_NAME to expiredCards.value.name.lowercase(Locale.ROOT),
        KEY_EXPIRES_SOON_MONTHS_NAME to expiresSoonMonths.value,
        KEY_ARCHIVED_CARDS_NAME to archivedCards.value.name.lowercase(Locale.ROOT),
        KEY_SHOW_NOTES_NAME to showNotes.value,
        KEY_NOTIF_ENABLED_NAME to notifEnabled.value,
        KEY_NOTIF_SEC_NAME to notifSec.value,
        KEY_NOTIF_SHOW_EXPIRY_NAME to notifShowExpiry.value,
        KEY_NOTIF_SHOW_CVV_NAME to notifShowCvv.value,
        KEY_TAGS_SHOW_NAME to tagsShow.value,
        KEY_TAGS_SEARCH_NAME to tagsSearch.value,
        KEY_TAGS_MAX_NAME to tagsMax.value,
        KEY_TAGS_MAX_LEN_NAME to tagsMaxLen.value,
        KEY_APP_LOCK_ENABLED_NAME to appLockEnabled.value,
        KEY_APP_LOCK_AFTER_SEC_NAME to appLockAfterSec.value,
        KEY_LOCK_ON_LEAVE_NAME to lockOnLeave.value
    )

    fun applyMap(values: Map<String, Any?>) {
        (values[KEY_THEME_NAME] as? String)?.let { raw ->
            runCatching { AppTheme.valueOf(raw.uppercase(Locale.ROOT)) }
                .getOrNull()
                ?.let(::setTheme)
        }
        (values[KEY_DYNAMIC_COLOR_NAME] as? Boolean)?.let(::setDynamicColor)
        (values[KEY_COPY_WINDOW_SEC_NAME] as? Number)?.toInt()?.let(::setCopyWindowSec)
        (values[KEY_CLIP_CLEAR_SEC_NAME] as? Number)?.toInt()?.let(::setClipClearSec)
        (values[KEY_MASK_NUMBERS_NAME] as? Boolean)?.let(::setMaskNumbers)
        (values[KEY_SHOW_CARD_TYPE_NAME] as? Boolean)?.let(::setShowCardType)
        (values[KEY_PINNED_MAX_NAME] as? Number)?.toInt()?.let(::setPinnedMax)
        (values[KEY_EXPIRED_CARDS_NAME] as? String)?.let { raw ->
            runCatching { ExpiredPlacement.valueOf(raw.uppercase(Locale.ROOT)) }
                .getOrNull()
                ?.let(::setExpiredCards)
        }
        (values[KEY_EXPIRES_SOON_MONTHS_NAME] as? Number)?.toInt()
            ?.let(::setExpiresSoonMonths)
        (values[KEY_ARCHIVED_CARDS_NAME] as? String)?.let { raw ->
            runCatching { ArchivedVisibility.valueOf(raw.uppercase(Locale.ROOT)) }
                .getOrNull()
                ?.let(::setArchivedCards)
        }
        (values[KEY_SHOW_NOTES_NAME] as? Boolean)?.let(::setShowNotes)
        (values[KEY_NOTIF_ENABLED_NAME] as? Boolean)?.let(::setNotifEnabled)
        (values[KEY_NOTIF_SEC_NAME] as? Number)?.toInt()?.let(::setNotifSec)
        (values[KEY_NOTIF_SHOW_EXPIRY_NAME] as? Boolean)?.let(::setNotifShowExpiry)
        (values[KEY_NOTIF_SHOW_CVV_NAME] as? Boolean)?.let(::setNotifShowCvv)
        (values[KEY_TAGS_SHOW_NAME] as? Boolean)?.let(::setTagsShow)
        (values[KEY_TAGS_SEARCH_NAME] as? Boolean)?.let(::setTagsSearch)
        (values[KEY_TAGS_MAX_NAME] as? Number)?.toInt()?.let(::setTagsMax)
        (values[KEY_TAGS_MAX_LEN_NAME] as? Number)?.toInt()?.let(::setTagsMaxLen)
        (values[KEY_APP_LOCK_ENABLED_NAME] as? Boolean)?.let(::setAppLockEnabled)
        (values[KEY_APP_LOCK_AFTER_SEC_NAME] as? Number)?.toInt()?.let(::setAppLockAfterSec)
        (values[KEY_LOCK_ON_LEAVE_NAME] as? Boolean)?.let(::setLockOnLeave)
    }

    private fun readTheme(): AppTheme {
        val stored = preferences.getString(KEY_THEME, AppSettingsDefaults.THEME.name)
            ?: AppSettingsDefaults.THEME.name
        return runCatching { AppTheme.valueOf(stored) }.getOrDefault(AppSettingsDefaults.THEME)
    }

    private fun readInt(key: String, default: Int, options: List<Int>): Int {
        val stored = preferences.getInt(key, default)
        return if (stored in options) stored else default
    }

    private fun readExpiredPlacement(): ExpiredPlacement {
        val stored = preferences.getString(KEY_EXPIRED_CARDS, null)
        return stored
            ?.let { runCatching { ExpiredPlacement.valueOf(it.uppercase(Locale.ROOT)) }.getOrNull() }
            ?: AppSettingsDefaults.EXPIRED_CARDS
    }

    private fun readArchivedVisibility(): ArchivedVisibility {
        val stored = preferences.getString(KEY_ARCHIVED_CARDS, null)
        return stored
            ?.let { runCatching { ArchivedVisibility.valueOf(it.uppercase(Locale.ROOT)) }.getOrNull() }
            ?: AppSettingsDefaults.ARCHIVED_CARDS
    }

    companion object {
        const val PREFS_NAME = "quickcards_settings"

        const val KEY_THEME = "theme_mode"
        const val KEY_DYNAMIC_COLOR = "dynamic_color"
        const val KEY_COPY_WINDOW_SEC = "copy_window_sec"
        const val KEY_CLIP_CLEAR_SEC = "clip_clear_sec"
        const val KEY_MASK_NUMBERS = "mask_numbers"
        const val KEY_SHOW_CARD_TYPE = "show_card_type"
        const val KEY_PINNED_MAX = "pinned_max"
        const val KEY_EXPIRED_CARDS = "expired_cards"
        const val KEY_EXPIRES_SOON_MONTHS = "expires_soon_months"
        const val KEY_ARCHIVED_CARDS = "archived_cards"
        const val KEY_SHOW_NOTES = "show_notes"
        const val KEY_NOTIF_ENABLED = "notif_enabled"
        const val KEY_NOTIF_SEC = "notif_sec"
        const val KEY_NOTIF_SHOW_EXPIRY = "notif_show_expiry"
        const val KEY_NOTIF_SHOW_CVV = "notif_show_cvv"
        const val KEY_TAGS_SHOW = "tags_show"
        const val KEY_TAGS_SEARCH = "tags_search"
        const val KEY_TAGS_MAX = "tags_max"
        const val KEY_TAGS_MAX_LEN = "tags_max_len"
        const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
        const val KEY_APP_LOCK_AFTER_SEC = "app_lock_after_sec"
        const val KEY_LOCK_ON_LEAVE = "lock_on_leave"

        const val KEY_THEME_NAME = "theme"
        const val KEY_DYNAMIC_COLOR_NAME = "dynamicColor"
        const val KEY_COPY_WINDOW_SEC_NAME = "copyWindowSec"
        const val KEY_CLIP_CLEAR_SEC_NAME = "clipClearSec"
        const val KEY_MASK_NUMBERS_NAME = "maskNumbers"
        const val KEY_SHOW_CARD_TYPE_NAME = "showCardType"
        const val KEY_PINNED_MAX_NAME = "pinnedMax"
        const val KEY_EXPIRED_CARDS_NAME = "expiredCards"
        const val KEY_EXPIRES_SOON_MONTHS_NAME = "expiresSoonMonths"
        const val KEY_ARCHIVED_CARDS_NAME = "archivedCards"
        const val KEY_SHOW_NOTES_NAME = "showNotes"
        const val KEY_NOTIF_ENABLED_NAME = "notifEnabled"
        const val KEY_NOTIF_SEC_NAME = "notifSec"
        const val KEY_NOTIF_SHOW_EXPIRY_NAME = "notifShowExpiry"
        const val KEY_NOTIF_SHOW_CVV_NAME = "notifShowCvv"
        const val KEY_TAGS_SHOW_NAME = "tagsShow"
        const val KEY_TAGS_SEARCH_NAME = "tagsSearch"
        const val KEY_TAGS_MAX_NAME = "tagsMax"
        const val KEY_TAGS_MAX_LEN_NAME = "tagsMaxLen"
        const val KEY_APP_LOCK_ENABLED_NAME = "appLockEnabled"
        const val KEY_APP_LOCK_AFTER_SEC_NAME = "appLockAfterSec"
        const val KEY_LOCK_ON_LEAVE_NAME = "lockOnLeave"

        @Volatile
        private var instance: AppSettings? = null

        fun get(context: Context): AppSettings {
            instance?.let { return it }
            synchronized(this) {
                instance?.let { return it }
                val preferences = context.applicationContext
                    .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                return AppSettings(preferences).also { instance = it }
            }
        }

        fun from(preferences: SharedPreferences): AppSettings {
            instance?.let { return it }
            synchronized(this) {
                instance?.let { return it }
                return AppSettings(preferences).also { instance = it }
            }
        }
    }
}
