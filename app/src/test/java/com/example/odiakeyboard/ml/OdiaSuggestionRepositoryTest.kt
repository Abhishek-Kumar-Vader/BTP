package com.example.odiakeyboard.ml

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OdiaSuggestionRepositoryTest {

    private lateinit var repository: OdiaSuggestionRepository
    private val loader: ModelAssetLoader = mockk()
    private val ngramEngine: NgramEngine = mockk()
    private val prefixEngine: PrefixEngine = mockk()
    private val lstmEngine: LstmInferenceEngine = mockk()

    @BeforeEach
    fun setUp() {
        repository = OdiaSuggestionRepository(
            loader = loader,
            ngramEngine = ngramEngine,
            prefixEngine = prefixEngine,
            lstmEngine = lstmEngine
        )
    }

    @Test
    fun `getSuggestions should return prefix completions when currentWord is not empty`() = runTest {
        val currentWord = "ଓଡ"
        val prefixIndex = mapOf("ଓ" to listOf("ଓଡ଼ିଆ", "ଓଡ଼ିଶା", "ଓଡ଼ିଶୀ"))
        val completions = listOf("ଓଡ଼ିଆ", "ଓଡ଼ିଶା")

        coEvery { loader.prefixIndex() } returns prefixIndex
        coEvery { prefixEngine.findCompletions(currentWord, prefixIndex, 3) } returns completions

        repository.getSuggestions(currentWord, emptyList()).test {
            assertEquals(completions, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `getSuggestions should return next-word predictions when currentWord is empty`() = runTest {
        val precedingWords = listOf("ଜାତୀୟ")
        val bigrams = mapOf("ଜାତୀୟ" to listOf("ରାଜପଥ", "ପତାକା", "ସଂଗୀତ"))
        val ngramPicks = listOf("ରାଜପଥ", "ପତାକା", "ସଂଗୀତ")

        coEvery { loader.bigrams() } returns bigrams
        coEvery { ngramEngine.suggestWithContext(precedingWords, bigrams, 3) } returns ngramPicks

        repository.getSuggestions("", precedingWords).test {
            assertEquals(ngramPicks, awaitItem())
            // Since it got 3 suggestions, it shouldn't trigger LSTM
            awaitComplete()
        }
    }

    @Test
    fun `getSuggestions should fallback to LSTM when bigrams return fewer than MAX_SUGGESTIONS`() = runTest {
        val precedingWords = listOf("ମୋର")
        val bigrams = mapOf("ମୋର" to listOf("ନାମ"))
        val ngramPicks = listOf("ନାମ")
        val vocab = mapOf("ମୋର" to 10)
        val idx2word = mapOf(11 to "ଅଭିଷେକ")
        val indices = listOf(10)
        val lstmPicks = listOf("ଅଭିଷେକ")

        coEvery { loader.bigrams() } returns bigrams
        coEvery { ngramEngine.suggestWithContext(precedingWords, bigrams, 3) } returns ngramPicks
        coEvery { loader.vocab() } returns vocab
        coEvery { loader.idx2word() } returns idx2word
        coEvery { lstmEngine.wordsToIndices(precedingWords, vocab) } returns indices
        coEvery { lstmEngine.predictNextWords(indices, idx2word, 6) } returns lstmPicks

        repository.getSuggestions("", precedingWords).test {
            assertEquals(ngramPicks, awaitItem())
            assertEquals(listOf("ନାମ", "ଅଭିଷେକ"), awaitItem())
            awaitComplete()
        }
    }
}