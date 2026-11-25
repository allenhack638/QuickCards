package com.quickcards.app.core.ui

import android.text.format.DateFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

/** "22 Nov" (or the locale's own day-month order): due strips, reminders and status sheets. */
fun dayMonth(): DateTimeFormatter {
    val locale = Locale.getDefault()
    return DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "dMMM"), locale)
}
