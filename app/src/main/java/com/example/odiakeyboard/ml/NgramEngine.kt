package com.example.odiakeyboard.ml

/**
 * Provides next-word suggestions from the pre-computed bigram model.
 *
 * The bigram model encodes: given word W, what are the most likely following words?
 * This is faster than LSTM inference (pure map lookup) and works well for
 * common two-word sequences that appear frequently in Odia text.
 *
 * ─── Data contract ────────────────────────────────────────────────────────────
 *  bigrams: Map<String, List<String>>
 *    key   = current word
 *    value = candidate next words, sorted descending by co-occurrence count
 *            (top-10 per word, pre-truncated during loading to save memory)
 * ─────────────────────────────────────────────────────────────────────────────
 */
class NgramEngine {

    /**
     * Returns up to [k] next-word suggestions for [currentWord].
     *
     * @param currentWord  The most recently completed word (after a space).
     * @param bigrams      The loaded bigram map from [ModelAssetLoader].
     * @param k            Maximum number of suggestions to return.
     */
    fun suggestNextWords(
        currentWord: String,
        bigrams: Map<String, List<String>>,
        k: Int = 3,
    ): List<String> {
        if (currentWord.isBlank()) return emptyList()
        return bigrams[currentWord]?.take(k) ?: emptyList()
    }

    /**
     * Returns up to [k] next-word suggestions using a sliding window of
     * [precedingWords] — falls back from the longest available context
     * to unigram if no bigram is found.
     *
     * Strategy: try last word first; if not in bigrams, return empty.
     */
    fun suggestWithContext(
        precedingWords: List<String>,
        bigrams: Map<String, List<String>>,
        k: Int = 3,
    ): List<String> {
        if (precedingWords.isEmpty()) return emptyList()
        val lastWord = precedingWords.last()
        return suggestNextWords(lastWord, bigrams, k)
    }
}