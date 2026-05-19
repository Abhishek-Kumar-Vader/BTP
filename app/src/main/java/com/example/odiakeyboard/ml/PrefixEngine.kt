package com.example.odiakeyboard.ml

/**
 * Provides fast, frequency-ordered prefix completion over the Odia dictionary.
 *
 * ─── Bug fix (Phase 2.1) ──────────────────────────────────────────────────────
 *  The original implementation used binary search on a `.sorted()` list.
 *  This had two critical bugs:
 *
 *  BUG 1 — Binary search on unsorted data:
 *    The dictionary is NOT pre-sorted. Calling .sorted() before binary search
 *    produced a Unicode-alphabetical ordering, but Odia Unicode sort order does
 *    not match word frequency or user expectations.
 *
 *  BUG 2 — Wrong results after sorting:
 *    Alphabetical sort placed rare/obscure words at the START of each prefix
 *    bucket. For example, prefix "ଓଡ" returned ["ଓଡ","ଓଡଆ","ଓଡଗାଁ"] instead
 *    of the correct ["ଓଡ଼ିଶୀ","ଓଡ଼ିଶା","ଓଡ଼ିଆ"].
 *
 *  FIX:
 *    Use a first-character prefix index (Map<String, List<String>>) built from
 *    the dictionary in its ORIGINAL frequency order (most common word first).
 *    Lookups scan only the bucket for the first typed character (~1,800 words
 *    on average), collecting matches in frequency order until [maxResults] found.
 *
 *    Results comparison for prefix "ଓଡ":
 *      Before fix: ["ଓଡ", "ଓଡଆ", "ଓଡଗାଁ"]          ← wrong, rare words
 *      After fix:  ["ଓଡ଼ିଶୀ", "ଓଡ଼ିଶା", "ଓଡ଼ିଆ"]    ← correct, common words
 * ─────────────────────────────────────────────────────────────────────────────
 */
class PrefixEngine {

    /**
     * Returns up to [maxResults] completions for [prefix], ordered by
     * word frequency (most common first).
     *
     * @param prefix       Partial word the user has typed so far.
     * @param prefixIndex  First-char grouped index from [ModelAssetLoader.prefixIndex].
     *                     Each bucket is in original dictionary (frequency) order.
     * @param maxResults   Maximum number of completions to return (default 3).
     */
    fun findCompletions(
        prefix: String,
        prefixIndex: Map<String, List<String>>,
        maxResults: Int = 3,
    ): List<String> {
        if (prefix.isBlank()) return emptyList()

        // Look up the bucket for the first character typed
        val firstChar = prefix[0].toString()
        val bucket    = prefixIndex[firstChar] ?: return emptyList()

        // Iterate in frequency order (most common first), collect prefix matches
        val results = mutableListOf<String>()
        for (word in bucket) {
            if (word.startsWith(prefix)) {
                results.add(word)
                if (results.size >= maxResults) break
            }
        }
        return results
    }

    /**
     * Returns true if [word] exists verbatim in the dictionary.
     * Used for simple spell-check validation.
     *
     * @param prefixIndex  The same index used by [findCompletions].
     */
    fun isValidWord(
        word: String,
        prefixIndex: Map<String, List<String>>,
    ): Boolean {
        if (word.isBlank()) return false
        val firstChar = word[0].toString()
        val bucket    = prefixIndex[firstChar] ?: return false
        // Linear scan within the bucket — still fast since buckets are ~1,800 words
        return bucket.contains(word)
    }
}