package com.example.odiakeyboard.ml

class PrefixEngine {
    fun findCompletions(
        prefix: String,
        prefixIndex: Map<String, List<String>>,
        maxResults: Int = 3
    ): List<String> {
        if (prefix.isBlank()) return emptyList()

        val firstChar = prefix[0].toString()
        val bucket = prefixIndex[firstChar] ?: return emptyList()

        val results = mutableListOf<String>()
        for (word in bucket) {
            if (word.startsWith(prefix)) {
                results.add(word)
                if (results.size >= maxResults) {
                    break
                }
            }
        }
        return results
    }

    fun isValidWord(
        word: String,
        prefixIndex: Map<String, List<String>>
    ): Boolean {
        if (word.isBlank()) return false
        val firstChar = word[0].toString()
        val bucket = prefixIndex[firstChar] ?: return false
        return bucket.contains(word)
    }
}