package com.quickcards.app.core.data.settings

import android.content.SharedPreferences
import com.quickcards.domain.ExpiredPlacement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsTest {

    @Test
    fun defaultsMatchPrototypeConfiguration() {
        val settings = AppSettings(FakePreferences())

        assertEquals(AppTheme.DARK, settings.theme.value)
        assertFalse(settings.dynamicColor.value)
        assertEquals(60, settings.copyWindowSec.value)
        assertEquals(60, settings.clipClearSec.value)
        assertTrue(settings.maskNumbers.value)
        assertEquals(ExpiredPlacement.END, settings.expiredCards.value)
        assertEquals(1, settings.expiresSoonMonths.value)
        assertTrue(settings.showNotes.value)
        assertTrue(settings.notifEnabled.value)
        assertEquals(30, settings.notifSec.value)
        assertTrue(settings.tagsShow.value)
        assertTrue(settings.appLockEnabled.value)
        assertEquals(60, settings.appLockAfterSec.value)
        assertTrue(settings.lockOnLeave.value)

        assertEquals(10, AppSettingsDefaults.PINNED_MAX)
        assertEquals(10, AppSettingsDefaults.TAGS_MAX)
        assertEquals(16, AppSettingsDefaults.TAGS_MAX_LEN)
    }

    @Test
    fun allowedValuesMatchPrototypeOptions() {
        assertEquals(listOf(30, 60, 120), AppSettingsDefaults.COPY_WINDOW_OPTIONS)
        assertEquals(listOf(0, 30, 60, 120), AppSettingsDefaults.CLIP_CLEAR_OPTIONS)
        assertEquals(listOf(15, 30, 60), AppSettingsDefaults.NOTIF_SEC_OPTIONS)
        assertEquals(
            listOf(ExpiredPlacement.KEEP, ExpiredPlacement.END, ExpiredPlacement.HIDE),
            AppSettingsDefaults.EXPIRED_CARDS_OPTIONS
        )
        assertEquals(listOf(1, 2, 3), AppSettingsDefaults.EXPIRES_SOON_MONTHS_OPTIONS)
        assertEquals(
            listOf(10, 30, 60, 120, 300, 600, 1800, 0),
            AppSettingsDefaults.APP_LOCK_AFTER_OPTIONS
        )
    }

    @Test
    fun invalidStoredValuesFallBackToDefaults() {
        val preferences = FakePreferences()
        preferences.edit()
            .putString(AppSettings.KEY_THEME, "PURPLE")
            .putInt(AppSettings.KEY_COPY_WINDOW_SEC, 45)
            .putInt(AppSettings.KEY_APP_LOCK_AFTER_SEC, 7)
            .putString(AppSettings.KEY_EXPIRED_CARDS, "SOMETIMES")
            .putInt(AppSettings.KEY_EXPIRES_SOON_MONTHS, 4)
            .apply()

        val settings = AppSettings(preferences)

        assertEquals(AppTheme.DARK, settings.theme.value)
        assertEquals(60, settings.copyWindowSec.value)
        assertEquals(60, settings.appLockAfterSec.value)
        assertEquals(ExpiredPlacement.END, settings.expiredCards.value)
        assertEquals(1, settings.expiresSoonMonths.value)
    }

    @Test
    fun settersPersistAndRejectUnsupportedValues() {
        val preferences = FakePreferences()
        val settings = AppSettings(preferences)

        settings.setTheme(AppTheme.LIGHT)
        settings.setDynamicColor(true)
        settings.setCopyWindowSec(120)
        settings.setClipClearSec(0)
        settings.setNotifSec(15)
        settings.setNotifEnabled(false)
        settings.setAppLockAfterSec(0)
        settings.setLockOnLeave(false)
        settings.setExpiredCards(ExpiredPlacement.HIDE)
        settings.setExpiresSoonMonths(4)
        settings.setExpiresSoonMonths(2)
        settings.setShowNotes(false)

        assertEquals(AppTheme.LIGHT, settings.theme.value)
        assertTrue(settings.dynamicColor.value)
        assertEquals(120, settings.copyWindowSec.value)
        assertEquals(0, settings.clipClearSec.value)
        assertEquals(15, settings.notifSec.value)
        assertFalse(settings.notifEnabled.value)
        assertEquals(0, settings.appLockAfterSec.value)
        assertFalse(settings.lockOnLeave.value)
        assertEquals(ExpiredPlacement.HIDE, settings.expiredCards.value)
        assertEquals(2, settings.expiresSoonMonths.value)
        assertFalse(settings.showNotes.value)

        val reloaded = AppSettings(preferences)
        assertEquals(AppTheme.LIGHT, reloaded.theme.value)
        assertTrue(reloaded.dynamicColor.value)
        assertEquals(120, reloaded.copyWindowSec.value)
        assertFalse(reloaded.notifEnabled.value)
        assertEquals(0, reloaded.appLockAfterSec.value)
        assertFalse(reloaded.lockOnLeave.value)
        assertEquals(ExpiredPlacement.HIDE, reloaded.expiredCards.value)
        assertEquals(2, reloaded.expiresSoonMonths.value)
        assertFalse(reloaded.showNotes.value)
    }

    @Test
    fun mapRoundTripsAllValues() {
        val settings = AppSettings(FakePreferences())
        settings.setTheme(AppTheme.SYSTEM)
        settings.setDynamicColor(true)
        settings.setCopyWindowSec(30)
        settings.setClipClearSec(120)
        settings.setMaskNumbers(false)
        settings.setNotifEnabled(false)
        settings.setNotifSec(60)
        settings.setTagsShow(false)
        settings.setAppLockEnabled(false)
        settings.setAppLockAfterSec(1800)
        settings.setLockOnLeave(false)
        settings.setExpiredCards(ExpiredPlacement.KEEP)
        settings.setExpiresSoonMonths(3)
        settings.setShowNotes(false)

        val restored = AppSettings(FakePreferences())
        restored.applyMap(settings.toMap())

        assertEquals(settings.toMap(), restored.toMap())
    }

    @Test
    fun applyMapAcceptsJsonNumbersAndLowercaseTheme() {
        val settings = AppSettings(FakePreferences())

        settings.applyMap(
            mapOf(
                "theme" to "light",
                "copyWindowSec" to 120.0,
                "appLockAfterSec" to 300.0,
                "notifEnabled" to false,
                "expiredCards" to "hide",
                "expiresSoonMonths" to 3.0,
                "showNotes" to false
            )
        )

        assertEquals(AppTheme.LIGHT, settings.theme.value)
        assertEquals(120, settings.copyWindowSec.value)
        assertEquals(300, settings.appLockAfterSec.value)
        assertFalse(settings.notifEnabled.value)
        assertEquals(ExpiredPlacement.HIDE, settings.expiredCards.value)
        assertEquals(3, settings.expiresSoonMonths.value)
        assertFalse(settings.showNotes.value)
    }

    @Test
    fun applyMapIgnoresUnknownAndInvalidValues() {
        val settings = AppSettings(FakePreferences())

        settings.applyMap(
            mapOf(
                "theme" to "neon",
                "copyWindowSec" to 45,
                "notifSec" to "soon",
                "unknown" to true
            )
        )

        assertEquals(AppTheme.DARK, settings.theme.value)
        assertEquals(60, settings.copyWindowSec.value)
        assertEquals(30, settings.notifSec.value)
    }

    @Test
    fun removedSettingsKeysAreIgnoredOnImport() {
        val settings = AppSettings(FakePreferences())
        com.quickcards.app.core.data.backup.BackupSettings.restore(
            settings,
            mapOf(
                "theme" to "light",
                "expiresSoonMonths" to 3,
                "pinnedMax" to 1,
                "tagsMax" to 1,
                "tagsMaxLen" to 8,
                "notifShowExpiry" to false,
                "notifShowCvv" to false,
                "tagsSearch" to false,
                "archivedCards" to "only_archive",
                "showCardType" to false
            )
        )

        assertEquals(AppTheme.LIGHT, settings.theme.value)
        assertEquals(3, settings.expiresSoonMonths.value)
        assertTrue(settings.notifEnabled.value)
        assertFalse(settings.toMap().keys.any { key -> key in REMOVED_KEYS })
    }

    @Test
    fun backupConfigCarriesTheSettingsAndRestoresDefaults() {
        val defaults = com.quickcards.app.core.data.backup.BackupSettings.defaultMap()

        assertEquals("end", defaults[AppSettings.KEY_EXPIRED_CARDS_NAME])
        assertEquals(1, defaults[AppSettings.KEY_EXPIRES_SOON_MONTHS_NAME])
        assertEquals(true, defaults[AppSettings.KEY_NOTIF_ENABLED_NAME])
        assertEquals(true, defaults[AppSettings.KEY_SHOW_NOTES_NAME])

        val settings = AppSettings(FakePreferences())
        settings.setExpiredCards(ExpiredPlacement.HIDE)
        settings.setExpiresSoonMonths(3)
        settings.setShowNotes(false)

        com.quickcards.app.core.data.backup.BackupSettings.restore(
            settings,
            mapOf("theme" to "light")
        )

        assertEquals(AppTheme.LIGHT, settings.theme.value)
        assertEquals(ExpiredPlacement.END, settings.expiredCards.value)
        assertEquals(1, settings.expiresSoonMonths.value)
        assertTrue(settings.showNotes.value)
    }

    private companion object {
        val REMOVED_KEYS = setOf(
            "pinnedMax",
            "tagsMax",
            "tagsMaxLen",
            "notifShowExpiry",
            "notifShowCvv",
            "tagsSearch",
            "archivedCards",
            "showCardType"
        )
    }

    private class FakePreferences : SharedPreferences {

        private val values = mutableMapOf<String, Any?>()
        private val listeners = mutableSetOf<SharedPreferences.OnSharedPreferenceChangeListener>()

        override fun getAll(): MutableMap<String, *> = values.toMutableMap()

        override fun getString(key: String?, defValue: String?): String? =
            values[key] as? String ?: defValue

        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? {
            @Suppress("UNCHECKED_CAST")
            return values[key] as? MutableSet<String> ?: defValues
        }

        override fun getInt(key: String?, defValue: Int): Int =
            values[key] as? Int ?: defValue

        override fun getLong(key: String?, defValue: Long): Long =
            values[key] as? Long ?: defValue

        override fun getFloat(key: String?, defValue: Float): Float =
            values[key] as? Float ?: defValue

        override fun getBoolean(key: String?, defValue: Boolean): Boolean =
            values[key] as? Boolean ?: defValue

        override fun contains(key: String?): Boolean = values.containsKey(key)

        override fun edit(): SharedPreferences.Editor = Editor()

        override fun registerOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?
        ) {
            if (listener != null) listeners.add(listener)
        }

        override fun unregisterOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?
        ) {
            if (listener != null) listeners.remove(listener)
        }

        private inner class Editor : SharedPreferences.Editor {
            private val updates = mutableMapOf<String, Any?>()
            private val removals = mutableSetOf<String>()
            private var clearAll = false

            override fun putString(key: String?, value: String?): SharedPreferences.Editor = apply {
                if (key != null) updates[key] = value
            }

            override fun putStringSet(
                key: String?,
                values: MutableSet<String>?
            ): SharedPreferences.Editor = apply {
                if (key != null) updates[key] = values
            }

            override fun putInt(key: String?, value: Int): SharedPreferences.Editor = apply {
                if (key != null) updates[key] = value
            }

            override fun putLong(key: String?, value: Long): SharedPreferences.Editor = apply {
                if (key != null) updates[key] = value
            }

            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = apply {
                if (key != null) updates[key] = value
            }

            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = apply {
                if (key != null) updates[key] = value
            }

            override fun remove(key: String?): SharedPreferences.Editor = apply {
                if (key != null) removals.add(key)
            }

            override fun clear(): SharedPreferences.Editor = apply {
                clearAll = true
            }

            override fun commit(): Boolean {
                if (clearAll) values.clear()
                removals.forEach(values::remove)
                values.putAll(updates)
                return true
            }

            override fun apply() {
                commit()
            }
        }
    }
}
