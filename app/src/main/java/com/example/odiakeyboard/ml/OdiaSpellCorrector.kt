package com.example.odiakeyboard.ml

import kotlin.math.min

class OdiaSpellCorrector(private val validWords: List<String>) {

    // Define phonetic groups where substitutions should have a lower penalty
    private val phoneticGroups = listOf(
        setOf('ସ', 'ଶ', 'ଷ'),
        setOf('ନ', 'ଣ'),
        setOf('ର', 'ଡ', 'ଢ'), // Accounting for characters often typed with nuktas
        setOf('ଲ', 'ଳ'),
        setOf('ଜ', 'ଯ', 'ୟ'),
        setOf('ି', 'ୀ'),
        setOf('ୁ', 'ୂ')
    )

    private val dictionarySet = validWords.toSet()

    /**
     * Returns top K corrections for a given misspelled word.
     */
    fun getCorrections(target: String, maxDistance: Float = 2.0f, topK: Int = 3): List<String> {
        if (target in dictionarySet) return listOf(target)

        val candidates = mutableListOf<Pair<String, Float>>()

        // For a production IME, you would pre-compute a deletion dictionary (SymSpell)
        // to avoid scanning the entire word list. For this implementation, we filter by length first.
        val lengthFiltered = validWords.filter {
            Math.abs(it.length - target.length) <= maxDistance
        }

        for (word in lengthFiltered) {
            val dist = phoneticLevenshtein(target, word)
            if (dist <= maxDistance) {
                candidates.add(Pair(word, dist))
            }
        }

        return candidates
            .sortedBy { it.second }
            .take(topK)
            .map { it.first }
    }

    /**
     * Calculates edit distance with discounted costs for Odia phonetic similarities.
     */
    private fun phoneticLevenshtein(s1: String, s2: String): Float {
        if (s1 == s2) return 0f

        var prev = FloatArray(s2.length + 1) { it.toFloat() }
        for (i in s1.indices) {
            val curr = FloatArray(s2.length + 1)
            curr[0] = (i + 1).toFloat()
            for (j in s2.indices) {
                val cost = getSubstitutionCost(s1[i], s2[j])
                curr[j + 1] = min(
                    min(curr[j] + 1f, prev[j + 1] + 1f), // Insertion or Deletion
                    prev[j] + cost                        // Substitution
                )
            }
            prev = curr
        }
        return prev.last()
    }

    private fun getSubstitutionCost(c1: Char, c2: Char): Float {
        if (c1 == c2) return 0f

        for (group in phoneticGroups) {
            if (c1 in group && c2 in group) {
                // Discounted cost for phonetically similar characters
                return 0.4f
            }
        }
        return 1.0f
    }
}