package com.quickcards.app.core.notifications

/**
 * The copy notification shows one masked placeholder per field it can copy, never a value:
 * the expiry and CVV leave only through its Copy actions.
 */
internal object CopyNotificationText {

    fun fields(expiry: String, cvv: String): List<CopyField> = buildList {
        if (expiry.isNotBlank()) add(CopyField.EXPIRY)
        if (cvv.isNotBlank()) add(CopyField.CVV)
    }

    fun text(fields: List<CopyField>, masked: (CopyField) -> String, separator: String): String =
        fields.joinToString(separator) { masked(it) }
}
