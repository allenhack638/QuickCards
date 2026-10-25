package com.quickcards.app.feature.backup

/** The import review shows the repair line only when the file changed anything. */
internal fun showsFormatSummary(formatsFixed: Int, needCheck: Int): Boolean =
    formatsFixed + needCheck > 0
