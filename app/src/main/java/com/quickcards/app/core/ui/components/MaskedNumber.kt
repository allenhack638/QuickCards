package com.quickcards.app.core.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import com.quickcards.app.core.ui.theme.QcText

const val MASK_DOTS = "••"

/**
 * Sets the masked "•• 1234" inside [text] (a dialog title, a sheet header) in Geist Mono with
 * the tile's tracked dots, so every compact surface shows the masked number one way.
 */
fun maskedNumberText(text: String): AnnotatedString {
    val start = text.lastIndexOf(MASK_DOTS)
    if (start < 0) return AnnotatedString(text)
    val dotsEnd = start + MASK_DOTS.length
    var end = dotsEnd
    while (end < text.length && (text[end].isDigit() || (end == dotsEnd && text[end] == ' '))) end++
    return buildAnnotatedString {
        append(text)
        addStyle(SpanStyle(fontFamily = QcText.MonoDotsF3.fontFamily, letterSpacing = QcText.MonoDotsF3.letterSpacing), start, dotsEnd)
        addStyle(
            SpanStyle(
                fontFamily = QcText.MonoF3.fontFamily,
                letterSpacing = QcText.MonoF3.letterSpacing,
                fontFeatureSettings = QcText.MonoF3.fontFeatureSettings
            ),
            dotsEnd,
            end
        )
    }
}
