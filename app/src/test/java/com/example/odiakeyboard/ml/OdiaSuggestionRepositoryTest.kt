package com.example.odiakeyboard.ml

import android.content.Context
import app.cash.turbine.test
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OdiaSuggestionRepositoryTest {

    private lateinit var repository: OdiaSuggestionRepository
    private val context: Context = mockk(relaxed = true)
    private val assetLoader: ModelAssetLoader = mockk()
    private val prefixEngine: PrefixEngine = mockk()
    private val ngramEngine: NgramEngine = mockk()
    private val spellCorrector: OdiaSpellCorrector = mockk()
    private val lstmEngine: LstmInferenceEngine = mockk()

    @BeforeEach
    fun setUp() {
        repository = OdiaSuggestionRepository(
            context = context,
            assetLoader = assetLoader,
            prefixEngine = prefixEngine,
            ngramEngine = ngramEngine,
            spellCorrector = spellCorrector,
            lstmEngine = lstmEngine
        )
    }

    @Test
    fun testGetSuggestionsReturnPrefixCompletions() = runTest {
        val currentWord = "ଓଡ"
        val prefixIndex = mapOf("ଓ" to listOf("ଓଡ଼ିଆ", "ଓଡ଼ିଶା", "ଓଡ଼ିଶୀ"))
        val completions = listOf("ଓଡ଼ିଆ", "ଓଡ଼ିଶା")

        coEvery { assetLoader.prefixIndex() } returns prefixIndex
        every { prefixEngine.findCompletions(currentWord, prefixIndex, 3) } returns completions

        repository.getSuggestions(currentWord, emptyList()).test {
            assertEquals(completions, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun testGetSuggestionsReturnNextWordPredictions() = runTest {
        val precedingWords = listOf("ଜାତୀୟ")
        val bigrams = mapOf("ଜାତୀୟ" to listOf("ରାଜପଥ", "ପତାକା", "ସଂଗୀତ"))
        val ngramPicks = listOf("ରାଜପଥ", "ପତାକା", "ସଂଗୀତ")

        coEvery { assetLoader.bigrams() } returns bigrams
        every { ngramEngine.suggestWithContext(precedingWords, bigrams, 3) } returns ngramPicks

        repository.getSuggestions("", precedingWords).test {
            assertEquals(ngramPicks, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun testGetSuggestionsFallbackToLstm() = runTest {
        val precedingWords = listOf("ମୋର")
        val bigrams = mapOf("ମୋର" to listOf("ନାମ"))
        val ngramPicks = listOf("ନାମ")
        val vocab = mapOf("ମୋର" to 10)
        val idx2word = mapOf(11 to "ଅଭିଷେକ")
        val indices = listOf(10)
        val lstmPicks = listOf("ଅଭିଷେକ")

        coEvery { assetLoader.bigrams() } returns bigrams
        every { ngramEngine.suggestWithContext(precedingWords, bigrams, 3) } returns ngramPicks
        coEvery { assetLoader.vocab() } returns vocab
        coEvery { assetLoader.idx2word() } returns idx2word
        every { lstmEngine.wordsToIndices(precedingWords, vocab) } returns indices
        coEvery { lstmEngine.predictNextWords(indices, idx2word, 6) } returns lstmPicks

        repository.getSuggestions("", precedingWords).test {
            assertEquals(ngramPicks, awaitItem())
            assertEquals(listOf("ନାମ", "ଅଭିଷେକ"), awaitItem())
            awaitComplete()
        }
    }
}