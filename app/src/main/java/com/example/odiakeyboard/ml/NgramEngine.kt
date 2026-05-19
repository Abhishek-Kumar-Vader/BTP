package com.example.odiakeyboard.ml

class NgramEngine {
    fun suggestNextWords(
        currentWord: String,
        bigrams: Map<String, List<String>>,
        k: Int = 3
    ): List<String> {
        if (currentWord.isBlank()) return emptyList()
        return bigrams[currentWord]?.take(k) ?: emptyList()
    }

    fun suggestWithContext(
        precedingWords: List<String>,
        bigrams: Map<String, List<String>>,
        k: Int = 3
    ): List<String> {
        if (precedingWords.isEmpty()) return emptyList()
        val lastWord = precedingWords.last()
        return suggestNextWords(lastWord, bigrams, k)
    }
}