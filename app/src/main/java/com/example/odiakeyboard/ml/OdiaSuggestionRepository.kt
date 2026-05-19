package com.example.odiakeyboard.ml

import android.content.Context
import com.example.odiakeyboard.viewmodel.SuggestionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class OdiaSuggestionRepository(
    private val context: Context,
    private val assetLoader: ModelAssetLoader = ModelAssetLoader(context.applicationContext),
    private val prefixEngine: PrefixEngine = PrefixEngine(),
    private val ngramEngine: NgramEngine = NgramEngine(),
    private val spellCorrector: OdiaSpellCorrector = OdiaSpellCorrector(),
    private val lstmEngine: LstmInferenceEngine = LstmInferenceEngine(assetLoader)
) : SuggestionRepository {

    companion object {
        const val MAX_SUGGESTIONS = 3
    }

    override fun getSuggestions(
        currentWord: String,
        precedingWords: List<String>
    ): Flow<List<String>> = flow {

        if (currentWord.isNotEmpty()) {
            val prefixIndex = assetLoader.prefixIndex()

            val prefixCompletions = prefixEngine.findCompletions(
                prefix = currentWord,
                prefixIndex = prefixIndex,
                maxResults = MAX_SUGGESTIONS
            )

            if (prefixCompletions.isNotEmpty()) {
                emit(prefixCompletions)
            } else {
                val validWords = assetLoader.wordSet()
                val spellCorrections = spellCorrector.getCorrections(
                    input = currentWord,
                    prefixIndex = prefixIndex,
                    wordSet = validWords,
                    maxResults = MAX_SUGGESTIONS
                )
                emit(spellCorrections)
            }
            return@flow
        }

        val bigramData = assetLoader.bigrams()
        val bigramSuggestions = ngramEngine.suggestWithContext(
            precedingWords = precedingWords,
            bigrams = bigramData,
            k = MAX_SUGGESTIONS
        )

        if (bigramSuggestions.isNotEmpty()) {
            emit(bigramSuggestions)
        }

        if (bigramSuggestions.size < MAX_SUGGESTIONS) {
            val vocabulary = assetLoader.vocab()
            val indexToWordMap = assetLoader.idx2word()
            val contextIndices = lstmEngine.wordsToIndices(precedingWords, vocabulary)

            val neuralPredictions = lstmEngine.predictNextWords(
                recentWordIndices = contextIndices,
                indexToWordMap = indexToWordMap,
                k = MAX_SUGGESTIONS * 2
            )

            val combinedSuggestions = (bigramSuggestions + neuralPredictions).distinct().take(MAX_SUGGESTIONS)
            if (combinedSuggestions != bigramSuggestions) {
                emit(combinedSuggestions)
            }
        }
    }.flowOn(Dispatchers.Default)
}