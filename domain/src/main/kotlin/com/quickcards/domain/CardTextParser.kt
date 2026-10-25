package com.quickcards.domain

import java.time.YearMonth

data class ParsedCardText(
    val number: String? = null,
    val expiry: String? = null
)

/**
 * Turns lines of card text into a card number and an expiry. The text can come
 * from a keyboard's own scan tool (which types into the focused field) or from
 * a plain paste; the parser does not know or care which. It never returns a CVV
 * or a card holder name: those must not be read from a card in the first place.
 *
 * The rules mirror the card form:
 * - a number is 12 to 19 digits and passes Luhn, unless a detected network
 *   turns Luhn off (UnionPay);
 * - a number that matches a network by prefix wins over an unknown number;
 * - expiry is MM/YY, MM/YYYY or MM-YY, not in the past and at most 20 years
 *   ahead; when a card prints "VALID FROM" and "VALID THRU", the later one is
 *   returned.
 */
object CardTextParser {

    private const val MIN_DIGITS = 12
    private const val MAX_DIGITS = 19
    private const val MAX_EXPIRY_YEARS = 20

    private val noise = Regex(
        "\\b(PHONE|TEL|TELEPHONE|CUSTOMER CARE|CUSTOMER SERVICE|IFSC|HELPLINE|MOBILE|CONTACT)\\b",
        RegexOption.IGNORE_CASE
    )

    private val digitGroup = Regex("""(?<![0-9A-Za-z|])[0-9OoIlSsBb|]{3,}(?![0-9A-Za-z|])""")

    private val expiryPattern = Regex(
        """(?<![\d/])(0[1-9]|1[0-2])\s*[/-]\s*(\d{4}|\d{2})(?![\d/])"""
    )

    private val afterDate = Regex(
        "\\b(THRU|THROUGH|EXP|EXPIRES|EXPIRY|TILL|UNTIL)\\b",
        RegexOption.IGNORE_CASE
    )

    private val fromDate = Regex("\\bFROM\\b", RegexOption.IGNORE_CASE)

    private val separator = Regex("""[\s.\-]+""")

    private data class Group(val digits: String, val start: Int, val end: Int)

    private data class Candidate(val number: String, val network: CardNetwork?, val position: Int)

    fun parse(
        lines: List<String>,
        networks: List<CardNetwork> = DefaultNetworks.all,
        today: YearMonth = YearMonth.now()
    ): ParsedCardText {
        val cleaned = lines.map { it.trim() }.filter { it.isNotEmpty() }
        return ParsedCardText(
            number = findNumber(cleaned, networks),
            expiry = findExpiry(cleaned, today)
        )
    }

    /**
     * Digits for a single card-number value, repairing the same OCR
     * confusions as [parse] inside a token that is mostly digits. Returns null
     * when the value is not made only of digit groups and separators.
     */
    fun numberDigits(value: String): String? {
        val tokens = value.trim().split(separator).filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return null
        val builder = StringBuilder()
        for (token in tokens) {
            val digits = digitsOf(token) ?: return null
            builder.append(digits)
        }
        return builder.toString()
    }

    private fun findNumber(lines: List<String>, networks: List<CardNetwork>): String? {
        val candidates = mutableListOf<Candidate>()
        var block = mutableListOf<Group>()

        fun flushBlock() {
            windows(block).forEach { addCandidate(it, lines, candidates, networks) }
            block = mutableListOf()
        }

        lines.forEach { line ->
            if (noise.containsMatchIn(line)) {
                flushBlock()
                return@forEach
            }
            val groups = groupsIn(line)
            if (groups.isEmpty()) {
                flushBlock()
                return@forEach
            }
            val pure = isPureNumberLine(line, groups)
            if (pure) {
                val digits = groups.sumOf { it.digits.length }
                if (digits >= MIN_DIGITS) {
                    // A complete number on its own line must not merge with the
                    // next line: only a short embossed fragment accumulates.
                    flushBlock()
                    windows(groups).forEach { addCandidate(it, lines, candidates, networks) }
                } else {
                    block.addAll(groups)
                }
            } else {
                flushBlock()
                runs(groups, line).forEach { run ->
                    windows(run).forEach { addCandidate(it, lines, candidates, networks) }
                }
            }
        }
        flushBlock()

        if (candidates.isEmpty()) return null
        return candidates
            .distinctBy { it.number }
            .sortedWith(
                compareByDescending<Candidate> { it.network != null }
                    .thenByDescending { candidate ->
                        candidate.network?.let {
                            CardNumberRules.prefixScore(candidate.number, it.prefixes)
                        } ?: 0
                    }
                    .thenByDescending { it.number.length }
                    .thenBy { it.position }
            )
            .first()
            .number
    }

    private fun addCandidate(
        raw: String,
        lines: List<String>,
        candidates: MutableList<Candidate>,
        networks: List<CardNetwork>
    ) {
        if (raw.length < MIN_DIGITS || raw.length > MAX_DIGITS) return
        val network = CardNumberRules.detect(raw, networks)
        val lengthOk = if (network == null) {
            raw.length in MIN_DIGITS..MAX_DIGITS
        } else {
            raw.length in CardNumberRules.parseLengths(network.lengths)
        }
        if (!lengthOk) return
        val luhnOk = if (network != null && !network.luhn) true else CardNumberRules.isLuhnValid(raw)
        if (!luhnOk) return
        val position = lines.indexOfFirst { it.contains(raw.take(4)) }
        candidates += Candidate(raw, network, if (position < 0) Int.MAX_VALUE else position)
    }

    private fun groupsIn(line: String): List<Group> =
        digitGroup.findAll(line).mapNotNull { match ->
            val token = match.value
            val digits = digitsOf(token) ?: return@mapNotNull null
            Group(digits, match.range.first, match.range.last + 1)
        }.toList()

    /** OCR confusions are only repaired inside a token that is mostly digits. */
    private fun digitsOf(token: String): String? {
        var digits = 0
        val builder = StringBuilder(token.length)
        token.forEach { char ->
            when (char) {
                in '0'..'9' -> {
                    digits++
                    builder.append(char)
                }

                'O', 'o' -> builder.append('0')
                'I', 'i', 'l', '|' -> builder.append('1')
                'S', 's' -> builder.append('5')
                'B', 'b' -> builder.append('8')
                else -> return null
            }
        }
        if (digits < 2 || digits * 2 < token.length) return null
        return builder.toString()
    }

    private fun isPureNumberLine(line: String, groups: List<Group>): Boolean {
        var index = 0
        groups.forEach { group ->
            if (index < group.start) {
                val gap = line.substring(index, group.start)
                if (!separator.matches(gap)) return false
            }
            index = group.end
        }
        val tail = line.substring(index)
        return tail.isEmpty() || separator.matches(tail)
    }

    private fun runs(groups: List<Group>, line: String): List<List<Group>> {
        if (groups.isEmpty()) return emptyList()
        val runs = mutableListOf<MutableList<Group>>()
        var current = mutableListOf(groups.first())
        for (index in 1 until groups.size) {
            val previous = groups[index - 1]
            val next = groups[index]
            val gap = line.substring(previous.end, next.start)
            if (separator.matches(gap)) {
                current.add(next)
            } else {
                runs.add(current)
                current = mutableListOf(next)
            }
        }
        runs.add(current)
        return runs
    }

    /**
     * A run of adjacent groups is one number. Taking shorter windows would let
     * a 12-digit slice of an invalid 16-digit number pass Luhn, so only the
     * complete run is a candidate.
     */
    private fun windows(groups: List<Group>): List<String> {
        if (groups.isEmpty()) return emptyList()
        val digits = groups.joinToString(separator = "") { it.digits }
        return if (digits.length in MIN_DIGITS..MAX_DIGITS) listOf(digits) else emptyList()
    }

    private fun findExpiry(lines: List<String>, today: YearMonth): String? {
        var best: Expiry? = null
        lines.forEach { line ->
            var previousEnd = 0
            expiryPattern.findAll(line).forEach { match ->
                val month = match.groupValues[1].toInt()
                val yearText = match.groupValues[2]
                val year = if (yearText.length == 4) yearText.toInt() else 2000 + yearText.toInt()
                val value = YearMonth.of(year, month)
                if (value >= today && value <= today.plusYears(MAX_EXPIRY_YEARS.toLong())) {
                    // Only the words since the previous date belong to this one,
                    // so a stray "EXP" earlier on the line cannot upgrade a
                    // later printed date.
                    val prefix = line.substring(previousEnd, match.range.first)
                    val priority = when {
                        afterDate.containsMatchIn(prefix) -> 2
                        fromDate.containsMatchIn(prefix) -> 0
                        else -> 1
                    }
                    val candidate = Expiry(value, priority, month, year)
                    val current = best
                    if (current == null ||
                        candidate.priority > current.priority ||
                        (candidate.priority == current.priority && candidate.value > current.value)
                    ) {
                        best = candidate
                    }
                }
                previousEnd = match.range.last + 1
            }
        }
        return best?.let {
            String.format(java.util.Locale.ROOT, "%02d/%02d", it.month, it.year % 100)
        }
    }

    private data class Expiry(val value: YearMonth, val priority: Int, val month: Int, val year: Int)
}
