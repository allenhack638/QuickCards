package com.quickcards.app.core.data.backup

import com.quickcards.app.core.data.settings.AppSettings
import com.quickcards.app.core.data.settings.AppSettingsDefaults
import java.util.Locale

/** Format 2 restore: defaults first, then the backup's `cfg`, so a missing key keeps its default. */
object BackupSettings {

    fun defaultMap(): Map<String, Any> = linkedMapOf(
        AppSettings.KEY_THEME_NAME to AppSettingsDefaults.THEME.name.lowercase(Locale.ROOT),
        AppSettings.KEY_COPY_WINDOW_SEC_NAME to AppSettingsDefaults.COPY_WINDOW_SEC,
        AppSettings.KEY_CLIP_CLEAR_SEC_NAME to AppSettingsDefaults.CLIP_CLEAR_SEC,
        AppSettings.KEY_MASK_NUMBERS_NAME to AppSettingsDefaults.MASK_NUMBERS,
        AppSettings.KEY_EXPIRED_CARDS_NAME to
            AppSettingsDefaults.EXPIRED_CARDS.name.lowercase(Locale.ROOT),
        AppSettings.KEY_EXPIRES_SOON_MONTHS_NAME to AppSettingsDefaults.EXPIRES_SOON_MONTHS,
        AppSettings.KEY_SHOW_NOTES_NAME to AppSettingsDefaults.SHOW_NOTES,
        AppSettings.KEY_NOTIF_ENABLED_NAME to AppSettingsDefaults.NOTIF_ENABLED,
        AppSettings.KEY_NOTIF_SEC_NAME to AppSettingsDefaults.NOTIF_SEC,
        AppSettings.KEY_TAGS_SHOW_NAME to AppSettingsDefaults.TAGS_SHOW,
        AppSettings.KEY_APP_LOCK_ENABLED_NAME to AppSettingsDefaults.APP_LOCK_ENABLED,
        AppSettings.KEY_APP_LOCK_AFTER_SEC_NAME to AppSettingsDefaults.APP_LOCK_AFTER_SEC,
        AppSettings.KEY_LOCK_ON_LEAVE_NAME to AppSettingsDefaults.LOCK_ON_LEAVE,
        AppSettings.KEY_AUTOFILL_ENABLED_NAME to AppSettingsDefaults.AUTOFILL_ENABLED,
        AppSettings.KEY_NFC_ADD_ENABLED_NAME to AppSettingsDefaults.NFC_ADD_ENABLED,
        AppSettings.KEY_EXPIRY_REMINDERS_NAME to AppSettingsDefaults.EXPIRY_REMINDERS,
        AppSettings.KEY_PAYMENT_REMINDERS_NAME to AppSettingsDefaults.PAYMENT_REMINDERS
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
        val notifEnabled: Boolean,
        val autofillEnabled: Boolean
    ) {
        fun stricterWith(other: Protection) = Protection(
            appLockEnabled = appLockEnabled || other.appLockEnabled,
            lockOnLeave = lockOnLeave || other.lockOnLeave,
            appLockAfterSec = shorterTimeout(appLockAfterSec, other.appLockAfterSec),
            copyWindowSec = minOf(copyWindowSec, other.copyWindowSec),
            clipClearSec = shorterTimeout(clipClearSec, other.clipClearSec),
            maskNumbers = maskNumbers || other.maskNumbers,
            notifEnabled = notifEnabled && other.notifEnabled,
            autofillEnabled = autofillEnabled && other.autofillEnabled
        )

        fun applyTo(settings: AppSettings) {
            settings.setAppLockEnabled(appLockEnabled)
            settings.setLockOnLeave(lockOnLeave)
            settings.setAppLockAfterSec(appLockAfterSec)
            settings.setCopyWindowSec(copyWindowSec)
            settings.setClipClearSec(clipClearSec)
            settings.setMaskNumbers(maskNumbers)
            settings.setNotifEnabled(notifEnabled)
            settings.setAutofillEnabled(autofillEnabled)
        }

        companion object {
            fun of(settings: AppSettings) = Protection(
                appLockEnabled = settings.appLockEnabled.value,
                lockOnLeave = settings.lockOnLeave.value,
                appLockAfterSec = settings.appLockAfterSec.value,
                copyWindowSec = settings.copyWindowSec.value,
                clipClearSec = settings.clipClearSec.value,
                maskNumbers = settings.maskNumbers.value,
                notifEnabled = settings.notifEnabled.value,
                autofillEnabled = settings.autofillEnabled.value
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
