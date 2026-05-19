package com.example.odiakeyboard.ml

import android.content.Context
import com.example.odiakeyboard.viewmodel.SuggestionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * Production implementation of [SuggestionRepository].
 */
class OdiaSuggestionRepository(
    private val loader: ModelAssetLoader,
    private val ngramEngine: NgramEngine,
    private val prefixEngine: PrefixEngine,
    private val lstmEngine: LstmInferenceEngine
) : SuggestionRepository {

    constructor(context: Context) : this(ModelAssetLoader(context.applicationContext))

    // Note: The yellow warning about this constructor not being used is safe to ignore,
    // it's an internal delegation for the constructor above.
    private constructor(loader: ModelAssetLoader) : this(
        loader = loader,
        ngramEngine = NgramEngine(),
        prefixEngine = PrefixEngine(),
        lstmEngine = LstmInferenceEngine(loader)
    )

    companion object {
        const val MAX_SUGGESTIONS = 3
    }

    // Lazy initialization of the Spell Corrector
    @Volatile private var spellCorrector: OdiaSpellCorrector? = null

    private suspend fun getSpellCorrector(): OdiaSpellCorrector {
        return spellCorrector ?: run {
            // FIXED: We now only pass validWords, matching the new Spell Correction Engine
            val sc = OdiaSpellCorrector(
                validWords = loader.validWords()
            )
            spellCorrector = sc
            sc
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    override fun getSuggestions(
        currentWord: String,
        precedingWords: List<String>,
    ): Flow<List<String>> = flow {

        // ── Case A: Mid-word → prefix completions or spell correction ─────────
        if (currentWord.isNotEmpty()) {
            val index = loader.prefixIndex()
            var completions = prefixEngine.findCompletions(
                prefix = currentWord,
                prefixIndex = index,
                maxResults = MAX_SUGGESTIONS,
            )

            // If prefix engine finds nothing, it's likely a typo.
            if (completions.isEmpty()) {
                val corrector = getSpellCorrector()

                // FIXED: Call getCorrections instead of correct().
                // It now returns a List<String> directly, so we don't need .map { it.first } anymore!
                completions = corrector.getCorrections(
                    target = currentWord,
                    topK = MAX_SUGGESTIONS
                )
            }

            emit(completions)
            return@flow
        }

        // ── Case B: After space → next-word prediction ────────────────────────

        // Step 1: Fast bigram lookup — emit immediately
        val bigrams    = loader.bigrams()
        val ngramPicks = ngramEngine.suggestWithContext(
            precedingWords = precedingWords,
            bigrams        = bigrams,
            k              = MAX_SUGGESTIONS,
        )

        if (ngramPicks.isNotEmpty()) {
            emit(ngramPicks)
        }

        // Step 2: If bigrams gave < MAX_SUGGESTIONS, pad with LSTM
        if (ngramPicks.size < MAX_SUGGESTIONS) {
            val vocab    = loader.vocab()
            val idx2word = loader.idx2word()
            val indices  = lstmEngine.wordsToIndices(precedingWords, vocab)

            val lstmPicks = lstmEngine.predictNextWords(
                recentWordIndices = indices,
                idx2word          = idx2word,
                k                 = MAX_SUGGESTIONS * 2,
            )

            val merged = (ngramPicks + lstmPicks)
                .distinct()
                .take(MAX_SUGGESTIONS)

            if (merged != ngramPicks) {
                emit(merged)
            }
        }

    }.flowOn(Dispatchers.Default)
}