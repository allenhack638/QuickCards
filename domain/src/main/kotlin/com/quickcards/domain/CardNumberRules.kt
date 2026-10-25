package com.quickcards.domain

object CardNumberRules {

    private val RANGE = Regex("""^(\d+)\s*-\s*(\d+)$""")
    private val DIGITS = Regex("""^\d+$""")
    private val DEFAULT_GROUPS = listOf(4, 4, 4, 4, 3)
    private val GROUPS_BY_LENGTH = mapOf(
        12 to listOf(4, 4, 4),
        13 to listOf(4, 4, 5),
        14 to listOf(4, 6, 4),
        15 to listOf(4, 6, 5),
        16 to listOf(4, 4, 4, 4),
        17 to listOf(4, 4, 4, 5),
        18 to listOf(4, 4, 4, 6),
        19 to listOf(4, 4, 4, 4, 3)
    )

    fun parseLengths(spec: String): List<Int> {
        val lengths = sortedSetOf<Int>()
        spec.split(",").forEach { raw ->
            val token = raw.trim()
            val range = RANGE.matchEntire(token)
            if (range != null) {
                val start = range.groupValues[1].toIntOrNull()
                val end = range.groupValues[2].toIntOrNull()
                if (start != null && end != null) {
                    for (length in start..end) lengths.add(length)
                }
            } else if (DIGITS.matches(token)) {
                token.toIntOrNull()?.let(lengths::add)
            }
        }
        return lengths.toList()
    }

    fun lengthsText(spec: String): String {
        val lengths = parseLengths(spec)
        if (lengths.isEmpty()) return ""

        val runs = mutableListOf<String>()
        var start = lengths.first()
        var previous = start
        for (length in lengths.drop(1)) {
            if (length == previous + 1) {
                previous = length
                continue
            }
            runs.add(if (start == previous) "$start" else "$start to $previous")
            start = length
            previous = length
        }
        runs.add(if (start == previous) "$start" else "$start to $previous")

        return if (runs.size > 1) {
            runs.dropLast(1).joinToString(", ") + " or " + runs.last()
        } else {
            runs.single()
        }
    }

    fun prefixScore(number: String, spec: String): Int {
        var best = 0
        spec.split(",").forEach { raw ->
            val token = raw.trim()
            val range = RANGE.matchEntire(token)
            if (range != null) {
                val length = range.groupValues[1].length
                if (number.length >= length) {
                    val candidate = number.take(length).toIntOrNull()
                    val start = range.groupValues[1].toIntOrNull()
                    val end = range.groupValues[2].toIntOrNull()
                    if (candidate != null && start != null && end != null && candidate in start..end) {
                        best = maxOf(best, length)
                    }
                }
            } else if (token.isNotEmpty() && number.startsWith(token)) {
                best = maxOf(best, token.length)
            }
        }
        return best
    }

    fun detect(number: String, networks: List<CardNetwork>): CardNetwork? {
        var best: CardNetwork? = null
        var bestScore = 0
        for (network in networks) {
            val score = prefixScore(number, network.prefixes)
            if (score > bestScore) {
                bestScore = score
                best = network
            }
        }
        return best
    }

    fun groupsFor(network: CardNetwork?, length: Int): List<Int>? {
        val custom = network?.groups
        if (!custom.isNullOrEmpty()) {
            val parts = custom.split("-")
            val groups = parts.mapNotNull { it.toIntOrNull() }
            if (groups.size == parts.size && groups.sum() == length) return groups
        }
        return GROUPS_BY_LENGTH[length]
    }

    fun maxLength(network: CardNetwork?): Int {
        if (network == null) return 19
        return maxOf(parseLengths(network.lengths).maxOrNull() ?: 12, 12)
    }

    fun format(
        number: String,
        network: CardNetwork?,
        masked: Boolean = false,
        typing: Boolean = false
    ): String {
        if (number.isEmpty()) return ""

        val length = number.length
        val lengths = network?.let { parseLengths(it.lengths) } ?: emptyList()
        val exact = groupsFor(network, length) != null && (network == null || length in lengths)
        val target = when {
            !typing && exact -> length
            typing && (network == null || 16 in lengths) && length <= 16 -> 16
            else -> lengths.firstOrNull { it >= length } ?: length
        }
        val groups = groupsFor(network, target) ?: DEFAULT_GROUPS
        val source = if (masked) {
            "•".repeat(maxOf(0, length - 4)) + number.takeLast(4)
        } else {
            number
        }

        val chunks = mutableListOf<String>()
        var index = 0
        for (group in groups) {
            if (index >= source.length) break
            chunks.add(source.substring(index, minOf(index + group, source.length)))
            index += group
        }
        if (index < source.length) chunks.add(source.substring(index))
        return chunks.joinToString(" ")
    }

    fun luhnCheckDigit(partial: String): Char {
        var sum = 0
        for (i in partial.indices) {
            var digit = partial[partial.length - 1 - i].digitToInt()
            if (i % 2 == 0) {
                digit *= 2
                if (digit > 9) digit -= 9
            }
            sum += digit
        }
        return ((10 - sum % 10) % 10).digitToChar()
    }

    fun isLuhnValid(number: String): Boolean {
        if (number.length <= 1) return false
        if (number.any { it !in '0'..'9' }) return false
        return luhnCheckDigit(number.dropLast(1)) == number.last()
    }

    fun cvvLength(network: CardNetwork?): IntRange = when (network?.cvvLength) {
        3 -> 3..3
        4 -> 4..4
        else -> 3..4
    }
}
