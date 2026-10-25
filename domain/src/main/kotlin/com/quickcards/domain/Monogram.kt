package com.quickcards.domain

import java.util.Locale

object Monogram {

    private val OF_WORD = Regex("""\bof\b""", RegexOption.IGNORE_CASE)
    private val WHITESPACE = Regex("""\s+""")
    private val UPPERCASE_WORD = Regex("""^[A-Z]{2,}$""")
    private val BANK_WORD = Regex("""^bank$""", RegexOption.IGNORE_CASE)

    fun monoFor(bankName: String): String {
        val words = OF_WORD.replace(bankName, "").split(WHITESPACE).filter { it.isNotEmpty() }
        if (words.isEmpty()) return "?"
        if (UPPERCASE_WORD.matches(words[0])) return words[0].take(2)
        if (words.size > 1 && !BANK_WORD.matches(words[1])) {
            return (words[0].first().toString() + words[1].first())
                .uppercase(Locale.ROOT)
        }
        return words[0].take(2).uppercase(Locale.ROOT)
    }

    fun initials(name: String): String = name
        .split(WHITESPACE)
        .filter { it.isNotEmpty() }
        .take(2)
        .map { it.first() }
        .joinToString("")
        .uppercase(Locale.ROOT)
}
