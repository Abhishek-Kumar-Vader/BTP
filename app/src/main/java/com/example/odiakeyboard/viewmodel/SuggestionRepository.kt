package com.example.odiakeyboard.viewmodel

import kotlinx.coroutines.flow.Flow

/**
 * PHASE 2 INTEGRATION POINT.
 *
 * Implement this interface with an offline ML model (e.g. TensorFlow Lite n-gram
 * or transformer) to provide next-word suggestions without touching any existing code.
 *
 * Contract:
 *  - MUST emit on a background dispatcher (never block Main thread).
 *  - MUST be entirely offline — no network calls.
 *  - Returns an empty list until the model is ready.
 */
interface SuggestionRepository {
    /**
     * @param currentWord  The word currently being typed (may be partial)
     * @param precedingWords  Recent words for context (n-gram window)
     * @return  Cold flow of suggestion lists; each emission replaces the previous.
     */
    fun getSuggestions(
        currentWord: String,
        precedingWords: List<String>,
    ): Flow<List<String>>
}

/** No-op implementation used in Phase 1. */
class NoOpSuggestionRepository : SuggestionRepository {
    override fun getSuggestions(
        currentWord: String,
        precedingWords: List<String>,
    ): Flow<List<String>> = kotlinx.coroutines.flow.flowOf(emptyList())
}