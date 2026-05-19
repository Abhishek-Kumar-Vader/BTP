package com.example.odiakeyboard.viewmodel

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface SuggestionRepository {
    fun getSuggestions(
        currentWord: String,
        precedingWords: List<String>
    ): Flow<List<String>>
}

class NoOpSuggestionRepository : SuggestionRepository {
    override fun getSuggestions(
        currentWord: String,
        precedingWords: List<String>
    ): Flow<List<String>> = flowOf(emptyList())
}