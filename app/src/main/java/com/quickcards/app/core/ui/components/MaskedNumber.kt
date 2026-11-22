package com.quickcards.app.core.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import com.quickcards.app.core.ui.theme.QcText
import androidx.compose.ui.unit.em

const val MASK_DOTS = "••"

/** A masked group of four digits, as on the card art and the full-width rows. */
const val MASK_GROUP = "••••"

/**
 * Sets the masked "•• 1234" inside [text] (a dialog title, a sheet header, a list row) in Geist
 * Mono with the tile's tracked dots, so every compact surface shows the masked number one way.
 * The space after the dots is narrowed to the frame's gap (the dots' tracking plus 4).
 */
fun maskedNumberText(text: String): AnnotatedString {
    val start = text.lastIndexOf(MASK_DOTS)
    if (start < 0) return AnnotatedString(text)
    val dotsEnd = start + MASK_DOTS.length
    val gap = text.getOrNull(dotsEnd) == ' '
    var end = if (gap) dotsEnd + 1 else dotsEnd
    while (end < text.length && text[end].isDigit()) end++
    return buildAnnotatedString {
        append(text)
        addStyle(SpanStyle(fontFamily = QcText.MonoDotsF3.fontFamily, letterSpacing = QcText.MonoDotsF3.letterSpacing), start, dotsEnd)
        if (gap) {
            addStyle(SpanStyle(fontFamily = QcText.MonoF3.fontFamily, fontSize = GAP_SIZE, letterSpacing = 0.em), dotsEnd, dotsEnd + 1)
        }
        addStyle(
            SpanStyle(
                fontFamily = QcText.MonoF3.fontFamily,
                letterSpacing = QcText.MonoF3.letterSpacing,
                fontFeatureSettings = QcText.MonoF3.fontFeatureSettings
            ),
            if (gap) dotsEnd + 1 else dotsEnd,
            end
        )
    }
}

/** A Geist Mono space is 0.6 em; at half size it leaves the frame's 4 + tracking. */
private val GAP_SIZE = 0.5.em
