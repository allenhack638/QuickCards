package com.quickcards.domain.sheet

/**
 * What the public Google Sheet (and so its committed exports) carries today. It trails the schema
 * until the owner republishes it: then set [VERSION] to [ImportSchema.VERSION], [headers] to
 * [ImportSchema.headers] and re-export the google-sheets-export fixtures.
 */
object PublishedSheet {
    const val VERSION = 1
    val headers: List<String> = ImportSchema.headers.filterNot { it == ImportColumn.STATEMENT_DAY.sheetHeader }
}
