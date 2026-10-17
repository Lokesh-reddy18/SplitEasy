package com.split.easy.ui.components

/**
 * Two letters for an avatar, matching the designs: "Coorg trip" → "CT", "Flat 4B" → "FB",
 * "Home" → "HM". Digits and symbols are skipped.
 * - Two or more words: first letter of the first and last words.
 * - One word: its first letter plus the next consonant, or the second letter if there isn't one.
 */
fun initialsOf(name: String): String {
    val words = name.split(Regex("\\s+"))
        .map { word -> word.filter { it.isLetter() } }
        .filter { it.isNotEmpty() }
    if (words.isEmpty()) return "?"
    if (words.size > 1) return "${words.first().first()}${words.last().first()}".uppercase()

    val word = words.single()
    val second = word.drop(1).firstOrNull { it.lowercaseChar() !in VOWELS } ?: word.getOrNull(1)
    return (word.first().toString() + (second?.toString() ?: "")).uppercase()
}

private val VOWELS = setOf('a', 'e', 'i', 'o', 'u')
