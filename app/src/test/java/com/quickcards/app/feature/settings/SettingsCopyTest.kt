package com.quickcards.app.feature.settings

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/**
 * Settings descriptions fit one line at 360dp and read in plain language: no special
 * characters, no trailing full stop and no more than 38 characters at the longest value
 * each row can show.
 */
class SettingsCopyTest {

    private val banned = listOf('·', '…', '—', '–', '+', '/', '(', ')', ';', ':', '"', '&', '\'')

    // Longest value each row can insert, from AppSettingsDefaults options and TimeText.seconds.
    private val longestValues = mapOf(
        "settings_clipboard_after" to "30 seconds",
        "settings_notification_time_subtitle" to "30 seconds",
        "settings_inactivity_subtitle" to "30 minutes",
        "settings_copy_window_subtitle" to "30 seconds",
        "settings_expires_soon_value" to "3"
    )

    private val descriptionNames = listOf(
        "settings_mask_subtitle",
        "settings_notes_subtitle",
        "settings_clipboard_after",
        "settings_notification_subtitle",
        "settings_notification_time_subtitle",
        "settings_tags_show_subtitle",
        "settings_app_lock_subtitle",
        "settings_inactivity_subtitle",
        "settings_inactivity_never",
        "settings_lock_on_leave_subtitle",
        "settings_copy_window_subtitle",
        "settings_lock_now_subtitle",
        "settings_backup_export_subtitle",
        "settings_backup_import_subtitle",
        "settings_backup_erase_subtitle",
        "settings_offline_subtitle",
        "settings_autofill_subtitle_off",
        "settings_autofill_subtitle_choose",
        "settings_autofill_subtitle_on",
        "settings_autofill_choose_subtitle",
        "settings_nfc_subtitle",
        "settings_expiry_reminders_subtitle",
        "settings_payment_reminders_subtitle",
        "settings_expired_keep",
        "settings_expired_end",
        "settings_expired_hide"
    )

    @Test
    fun descriptionsUseOnlyPlainCharacters() {
        val failures = texts().flatMap { (name, variants) ->
            variants.mapNotNull { text ->
                val found = banned.filter { it in text }
                if (found.isEmpty()) null else "$name contains ${found.joinToString(" ")}: $text"
            }
        }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun descriptionsHaveNoTrailingFullStop() {
        val failures = texts().flatMap { (name, variants) ->
            variants
                .filter { it.endsWith(".") }
                .map { "$name ends with a full stop: $it" }
        }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun descriptionsFitOneLineAtTheLongestValue() {
        val failures = texts().flatMap { (name, variants) ->
            variants.mapNotNull { text ->
                val expanded = expanded(name, text)
                if (expanded.length <= MAX_LENGTH) {
                    null
                } else {
                    "$name is ${expanded.length} characters: $expanded"
                }
            }
        }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    private fun texts(): List<Pair<String, List<String>>> {
        val root = settingsDocument().documentElement
        val strings = descriptionNames.map { name ->
            name to listOf(string(root, name))
        }
        val plurals = listOf("settings_expires_soon_value" to pluralItems(root, "settings_expires_soon_value"))
        return strings + plurals
    }

    private fun expanded(name: String, text: String): String {
        if ("%1\$s" !in text && "%1\$d" !in text) return text
        val value = longestValues[name] ?: throw AssertionError("No longest value for $name")
        return text.replace("%1\$s", value).replace("%1\$d", value)
    }

    private fun string(root: Element, name: String): String {
        val nodes = root.getElementsByTagName("string")
        for (index in 0 until nodes.length) {
            val element = nodes.item(index) as Element
            if (element.getAttribute("name") == name) return element.textContent
        }
        throw AssertionError("Missing string $name")
    }

    private fun pluralItems(root: Element, name: String): List<String> {
        val nodes = root.getElementsByTagName("plurals")
        for (index in 0 until nodes.length) {
            val element = nodes.item(index) as Element
            if (element.getAttribute("name") != name) continue
            val items = element.getElementsByTagName("item")
            return (0 until items.length).map { (items.item(it) as Element).textContent }
        }
        throw AssertionError("Missing plurals $name")
    }

    private fun settingsDocument() =
        DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(File(repositoryRoot(), "app/src/main/res/values/strings_settings.xml"))

    private fun repositoryRoot(): File {
        var directory: File? = File(System.getProperty("user.dir") ?: ".").absoluteFile
        while (directory != null) {
            if (File(directory, "settings.gradle").isFile) return directory
            directory = directory.parentFile
        }
        throw AssertionError("Could not find settings.gradle from ${System.getProperty("user.dir")}")
    }

    private companion object {
        const val MAX_LENGTH = 38
    }
}
