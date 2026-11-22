package com.quickcards.app.core.notifications

/** The copy notification's text: the card's expiry and CVV, leaving out a blank one. */
internal object CopyNotificationText {

    fun text(
        expiry: String,
        cvv: String,
        expiryText: (String) -> String,
        cvvText: (String) -> String,
        separator: String
    ): String = listOfNotNull(
        expiry.takeIf { it.isNotBlank() }?.let(expiryText),
        cvv.takeIf { it.isNotBlank() }?.let(cvvText)
    ).joinToString(separator)
}
