package com.quickcards.app.core.data.backup

import com.quickcards.app.core.data.settings.AppSettings
import com.quickcards.app.core.data.settings.AppSettingsDefaults
import java.util.Locale

/** Format 2 restore: defaults first, then the backup's `cfg`, so a missing key keeps its default. */
object BackupSettings {

    fun defaultMap(): Map<String, Any> = linkedMapOf(
        AppSettings.KEY_THEME_NAME to AppSettingsDefaults.THEME.name.lowercase(Locale.ROOT),
        AppSettings.KEY_DYNAMIC_COLOR_NAME to AppSettingsDefaults.DYNAMIC_COLOR,
        AppSettings.KEY_COPY_WINDOW_SEC_NAME to AppSettingsDefaults.COPY_WINDOW_SEC,
        AppSettings.KEY_CLIP_CLEAR_SEC_NAME to AppSettingsDefaults.CLIP_CLEAR_SEC,
        AppSettings.KEY_MASK_NUMBERS_NAME to AppSettingsDefaults.MASK_NUMBERS,
        AppSettings.KEY_SHOW_CARD_TYPE_NAME to AppSettingsDefaults.SHOW_CARD_TYPE,
        AppSettings.KEY_PINNED_MAX_NAME to AppSettingsDefaults.PINNED_MAX,
        AppSettings.KEY_EXPIRED_CARDS_NAME to
            AppSettingsDefaults.EXPIRED_CARDS.name.lowercase(Locale.ROOT),
        AppSettings.KEY_EXPIRES_SOON_MONTHS_NAME to AppSettingsDefaults.EXPIRES_SOON_MONTHS,
        AppSettings.KEY_ARCHIVED_CARDS_NAME to
            AppSettingsDefaults.ARCHIVED_CARDS.name.lowercase(Locale.ROOT),
        AppSettings.KEY_SHOW_NOTES_NAME to AppSettingsDefaults.SHOW_NOTES,
        AppSettings.KEY_NOTIF_ENABLED_NAME to AppSettingsDefaults.NOTIF_ENABLED,
        AppSettings.KEY_NOTIF_SEC_NAME to AppSettingsDefaults.NOTIF_SEC,
        AppSettings.KEY_NOTIF_SHOW_EXPIRY_NAME to AppSettingsDefaults.NOTIF_SHOW_EXPIRY,
        AppSettings.KEY_NOTIF_SHOW_CVV_NAME to AppSettingsDefaults.NOTIF_SHOW_CVV,
        AppSettings.KEY_TAGS_SHOW_NAME to AppSettingsDefaults.TAGS_SHOW,
        AppSettings.KEY_TAGS_SEARCH_NAME to AppSettingsDefaults.TAGS_SEARCH,
        AppSettings.KEY_TAGS_MAX_NAME to AppSettingsDefaults.TAGS_MAX,
        AppSettings.KEY_TAGS_MAX_LEN_NAME to AppSettingsDefaults.TAGS_MAX_LEN,
        AppSettings.KEY_APP_LOCK_ENABLED_NAME to AppSettingsDefaults.APP_LOCK_ENABLED,
        AppSettings.KEY_APP_LOCK_AFTER_SEC_NAME to AppSettingsDefaults.APP_LOCK_AFTER_SEC,
        AppSettings.KEY_LOCK_ON_LEAVE_NAME to AppSettingsDefaults.LOCK_ON_LEAVE
    )

    /**
     * A restore may tighten protection but never loosen it, so a crafted file cannot bypass
     * the Settings re-ask: the stricter of phone and backup wins.
     */
    fun restore(settings: AppSettings, values: Map<String, Any?>) {
        val before = Protection.of(settings)
        settings.applyMap(defaultMap())
        settings.applyMap(values)
        before.stricterWith(Protection.of(settings)).applyTo(settings)
    }

    internal data class Protection(
        val appLockEnabled: Boolean,
        val lockOnLeave: Boolean,
        val appLockAfterSec: Int,
        val copyWindowSec: Int,
        val clipClearSec: Int,
        val maskNumbers: Boolean,
        val notifShowExpiry: Boolean,
        val notifShowCvv: Boolean
    ) {
        fun stricterWith(other: Protection) = Protection(
            appLockEnabled = appLockEnabled || other.appLockEnabled,
            lockOnLeave = lockOnLeave || other.lockOnLeave,
            appLockAfterSec = shorterTimeout(appLockAfterSec, other.appLockAfterSec),
            copyWindowSec = minOf(copyWindowSec, other.copyWindowSec),
            clipClearSec = shorterTimeout(clipClearSec, other.clipClearSec),
            maskNumbers = maskNumbers || other.maskNumbers,
            notifShowExpiry = notifShowExpiry && other.notifShowExpiry,
            notifShowCvv = notifShowCvv && other.notifShowCvv
        )

        fun applyTo(settings: AppSettings) {
            settings.setAppLockEnabled(appLockEnabled)
            settings.setLockOnLeave(lockOnLeave)
            settings.setAppLockAfterSec(appLockAfterSec)
            settings.setCopyWindowSec(copyWindowSec)
            settings.setClipClearSec(clipClearSec)
            settings.setMaskNumbers(maskNumbers)
            settings.setNotifShowExpiry(notifShowExpiry)
            settings.setNotifShowCvv(notifShowCvv)
        }

        companion object {
            fun of(settings: AppSettings) = Protection(
                appLockEnabled = settings.appLockEnabled.value,
                lockOnLeave = settings.lockOnLeave.value,
                appLockAfterSec = settings.appLockAfterSec.value,
                copyWindowSec = settings.copyWindowSec.value,
                clipClearSec = settings.clipClearSec.value,
                maskNumbers = settings.maskNumbers.value,
                notifShowExpiry = settings.notifShowExpiry.value,
                notifShowCvv = settings.notifShowCvv.value
            )

            /** 0 means "never", the weakest choice. */
            internal fun shorterTimeout(a: Int, b: Int): Int = when {
                a == 0 -> b
                b == 0 -> a
                else -> minOf(a, b)
            }
        }
    }
}
