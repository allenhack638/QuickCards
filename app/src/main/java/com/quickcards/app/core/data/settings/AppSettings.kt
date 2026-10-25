package com.quickcards.app.core.data.settings

import android.content.Context
import android.content.SharedPreferences
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

    const val COPY_WINDOW_SEC = 60
    val COPY_WINDOW_OPTIONS = listOf(30, 60, 120)

    const val CLIP_CLEAR_SEC = 60
    val CLIP_CLEAR_OPTIONS = listOf(0, 30, 60, 120)

    const val MASK_NUMBERS = true

    const val PINNED_MAX = 10

    val EXPIRED_CARDS = ExpiredPlacement.END
    val EXPIRED_CARDS_OPTIONS = listOf(
        ExpiredPlacement.KEEP,
        ExpiredPlacement.END,
        ExpiredPlacement.HIDE
    )

    const val EXPIRES_SOON_MONTHS = 1
    val EXPIRES_SOON_MONTHS_OPTIONS = listOf(1, 2, 3)

    const val SHOW_NOTES = true

    const val NOTIF_ENABLED = true
    const val NOTIF_SEC = 30
    val NOTIF_SEC_OPTIONS = listOf(15, 30, 60)

    const val TAGS_SHOW = true
    const val TAGS_MAX = 10
    const val TAGS_MAX_LEN = 16

    const val APP_LOCK_ENABLED = true
    const val APP_LOCK_AFTER_SEC = 60
    val APP_LOCK_AFTER_OPTIONS = listOf(10, 30, 60, 120, 300, 600, 1800, 0)

    const val LOCK_ON_LEAVE = true
    const val AUTOFILL_ENABLED = true
    const val NFC_ADD_ENABLED = true
    const val EXPIRY_REMINDERS = true
    const val PAYMENT_REMINDERS = true
}

class AppSettings(private val preferences: SharedPreferences) {

    init {
        // 2.2 merged "show expiry" and "show CVV" into one switch; whoever hid either keeps it off.
        if (preferences.contains(LEGACY_NOTIF_SHOW_EXPIRY) || preferences.contains(LEGACY_NOTIF_SHOW_CVV)) {
            val hidden = !preferences.getBoolean(LEGACY_NOTIF_SHOW_EXPIRY, true) ||
                !preferences.getBoolean(LEGACY_NOTIF_SHOW_CVV, true)
            preferences.edit().apply {
                if (hidden) putBoolean(KEY_NOTIF_ENABLED, false)
                remove(LEGACY_NOTIF_SHOW_EXPIRY)
                remove(LEGACY_NOTIF_SHOW_CVV)
            }.apply()
        }
    }

    private val _theme = MutableStateFlow(readTheme())
    val theme: StateFlow<AppTheme> = _theme.asStateFlow()

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

    private val _tagsShow = MutableStateFlow(
        preferences.getBoolean(KEY_TAGS_SHOW, AppSettingsDefaults.TAGS_SHOW)
    )
    val tagsShow: StateFlow<Boolean> = _tagsShow.asStateFlow()

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

    private val _autofillEnabled = MutableStateFlow(
        preferences.getBoolean(KEY_AUTOFILL_ENABLED, AppSettingsDefaults.AUTOFILL_ENABLED)
    )
    val autofillEnabled: StateFlow<Boolean> = _autofillEnabled.asStateFlow()

    private val _nfcAddEnabled = MutableStateFlow(
        preferences.getBoolean(KEY_NFC_ADD_ENABLED, AppSettingsDefaults.NFC_ADD_ENABLED)
    )
    val nfcAddEnabled: StateFlow<Boolean> = _nfcAddEnabled.asStateFlow()

    private val _expiryReminders = MutableStateFlow(
        preferences.getBoolean(KEY_EXPIRY_REMINDERS, AppSettingsDefaults.EXPIRY_REMINDERS)
    )
    val expiryReminders: StateFlow<Boolean> = _expiryReminders.asStateFlow()

    private val _paymentReminders = MutableStateFlow(
        preferences.getBoolean(KEY_PAYMENT_REMINDERS, AppSettingsDefaults.PAYMENT_REMINDERS)
    )
    val paymentReminders: StateFlow<Boolean> = _paymentReminders.asStateFlow()

    fun setTheme(theme: AppTheme) {
        preferences.edit().putString(KEY_THEME, theme.name).apply()
        _theme.value = theme
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

    fun setExpiredCards(placement: ExpiredPlacement) {
        preferences.edit().putString(KEY_EXPIRED_CARDS, placement.name).apply()
        _expiredCards.value = placement
    }

    fun setExpiresSoonMonths(months: Int) {
        if (months !in AppSettingsDefaults.EXPIRES_SOON_MONTHS_OPTIONS) return
        preferences.edit().putInt(KEY_EXPIRES_SOON_MONTHS, months).apply()
        _expiresSoonMonths.value = months
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

    fun setTagsShow(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_TAGS_SHOW, enabled).apply()
        _tagsShow.value = enabled
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

    fun setAutofillEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_AUTOFILL_ENABLED, enabled).apply()
        _autofillEnabled.value = enabled
    }

    fun setNfcAddEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_NFC_ADD_ENABLED, enabled).apply()
        _nfcAddEnabled.value = enabled
    }

    fun setExpiryReminders(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_EXPIRY_REMINDERS, enabled).apply()
        _expiryReminders.value = enabled
    }

    fun setPaymentReminders(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_PAYMENT_REMINDERS, enabled).apply()
        _paymentReminders.value = enabled
    }

    fun toMap(): Map<String, Any> = linkedMapOf(
        KEY_THEME_NAME to theme.value.name.lowercase(Locale.ROOT),
        KEY_COPY_WINDOW_SEC_NAME to copyWindowSec.value,
        KEY_CLIP_CLEAR_SEC_NAME to clipClearSec.value,
        KEY_MASK_NUMBERS_NAME to maskNumbers.value,
        KEY_EXPIRED_CARDS_NAME to expiredCards.value.name.lowercase(Locale.ROOT),
        KEY_EXPIRES_SOON_MONTHS_NAME to expiresSoonMonths.value,
        KEY_SHOW_NOTES_NAME to showNotes.value,
        KEY_NOTIF_ENABLED_NAME to notifEnabled.value,
        KEY_NOTIF_SEC_NAME to notifSec.value,
        KEY_TAGS_SHOW_NAME to tagsShow.value,
        KEY_APP_LOCK_ENABLED_NAME to appLockEnabled.value,
        KEY_APP_LOCK_AFTER_SEC_NAME to appLockAfterSec.value,
        KEY_LOCK_ON_LEAVE_NAME to lockOnLeave.value,
        KEY_AUTOFILL_ENABLED_NAME to autofillEnabled.value,
        KEY_NFC_ADD_ENABLED_NAME to nfcAddEnabled.value,
        KEY_EXPIRY_REMINDERS_NAME to expiryReminders.value,
        KEY_PAYMENT_REMINDERS_NAME to paymentReminders.value
    )

    fun applyMap(values: Map<String, Any?>) {
        if (values[LEGACY_NOTIF_SHOW_EXPIRY_NAME] == false || values[LEGACY_NOTIF_SHOW_CVV_NAME] == false) {
            setNotifEnabled(false)
        }
        (values[KEY_THEME_NAME] as? String)?.let { raw ->
            runCatching { AppTheme.valueOf(raw.uppercase(Locale.ROOT)) }
                .getOrNull()
                ?.let(::setTheme)
        }
        (values[KEY_COPY_WINDOW_SEC_NAME] as? Number)?.toInt()?.let(::setCopyWindowSec)
        (values[KEY_CLIP_CLEAR_SEC_NAME] as? Number)?.toInt()?.let(::setClipClearSec)
        (values[KEY_MASK_NUMBERS_NAME] as? Boolean)?.let(::setMaskNumbers)
        (values[KEY_EXPIRED_CARDS_NAME] as? String)?.let { raw ->
            runCatching { ExpiredPlacement.valueOf(raw.uppercase(Locale.ROOT)) }
                .getOrNull()
                ?.let(::setExpiredCards)
        }
        (values[KEY_EXPIRES_SOON_MONTHS_NAME] as? Number)?.toInt()
            ?.let(::setExpiresSoonMonths)
        (values[KEY_SHOW_NOTES_NAME] as? Boolean)?.let(::setShowNotes)
        (values[KEY_NOTIF_ENABLED_NAME] as? Boolean)?.let(::setNotifEnabled)
        (values[KEY_NOTIF_SEC_NAME] as? Number)?.toInt()?.let(::setNotifSec)
        (values[KEY_TAGS_SHOW_NAME] as? Boolean)?.let(::setTagsShow)
        (values[KEY_APP_LOCK_ENABLED_NAME] as? Boolean)?.let(::setAppLockEnabled)
        (values[KEY_APP_LOCK_AFTER_SEC_NAME] as? Number)?.toInt()?.let(::setAppLockAfterSec)
        (values[KEY_LOCK_ON_LEAVE_NAME] as? Boolean)?.let(::setLockOnLeave)
        (values[KEY_AUTOFILL_ENABLED_NAME] as? Boolean)?.let(::setAutofillEnabled)
        (values[KEY_NFC_ADD_ENABLED_NAME] as? Boolean)?.let(::setNfcAddEnabled)
        (values[KEY_EXPIRY_REMINDERS_NAME] as? Boolean)?.let(::setExpiryReminders)
        (values[KEY_PAYMENT_REMINDERS_NAME] as? Boolean)?.let(::setPaymentReminders)
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

    companion object {
        const val PREFS_NAME = "quickcards_settings"

        const val KEY_THEME = "theme_mode"
        const val KEY_COPY_WINDOW_SEC = "copy_window_sec"
        const val KEY_CLIP_CLEAR_SEC = "clip_clear_sec"
        const val KEY_MASK_NUMBERS = "mask_numbers"
        const val KEY_EXPIRED_CARDS = "expired_cards"
        const val KEY_EXPIRES_SOON_MONTHS = "expires_soon_months"
        const val KEY_SHOW_NOTES = "show_notes"
        const val KEY_NOTIF_ENABLED = "notif_enabled"
        const val KEY_NOTIF_SEC = "notif_sec"
        const val KEY_TAGS_SHOW = "tags_show"
        const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
        const val KEY_APP_LOCK_AFTER_SEC = "app_lock_after_sec"
        const val KEY_LOCK_ON_LEAVE = "lock_on_leave"
        const val KEY_AUTOFILL_ENABLED = "autofill_enabled"
        private const val LEGACY_NOTIF_SHOW_EXPIRY = "notif_show_expiry"
        private const val LEGACY_NOTIF_SHOW_CVV = "notif_show_cvv"
        private const val LEGACY_NOTIF_SHOW_EXPIRY_NAME = "notifShowExpiry"
        private const val LEGACY_NOTIF_SHOW_CVV_NAME = "notifShowCvv"
        const val KEY_NFC_ADD_ENABLED = "nfc_add_enabled"
        const val KEY_EXPIRY_REMINDERS = "expiry_reminders"
        const val KEY_PAYMENT_REMINDERS = "payment_reminders"

        const val KEY_THEME_NAME = "theme"
        const val KEY_COPY_WINDOW_SEC_NAME = "copyWindowSec"
        const val KEY_CLIP_CLEAR_SEC_NAME = "clipClearSec"
        const val KEY_MASK_NUMBERS_NAME = "maskNumbers"
        const val KEY_EXPIRED_CARDS_NAME = "expiredCards"
        const val KEY_EXPIRES_SOON_MONTHS_NAME = "expiresSoonMonths"
        const val KEY_SHOW_NOTES_NAME = "showNotes"
        const val KEY_NOTIF_ENABLED_NAME = "notifEnabled"
        const val KEY_NOTIF_SEC_NAME = "notifSec"
        const val KEY_TAGS_SHOW_NAME = "tagsShow"
        const val KEY_APP_LOCK_ENABLED_NAME = "appLockEnabled"
        const val KEY_APP_LOCK_AFTER_SEC_NAME = "appLockAfterSec"
        const val KEY_LOCK_ON_LEAVE_NAME = "lockOnLeave"
        const val KEY_AUTOFILL_ENABLED_NAME = "autofillEnabled"
        const val KEY_NFC_ADD_ENABLED_NAME = "nfcAddEnabled"
        const val KEY_EXPIRY_REMINDERS_NAME = "expiryReminders"
        const val KEY_PAYMENT_REMINDERS_NAME = "paymentReminders"

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
