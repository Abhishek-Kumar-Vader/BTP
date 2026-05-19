package com.example.odiakeyboard.ml

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.tensorflow.lite.Interpreter

class LstmInferenceEngine(private val assetLoader: ModelAssetLoader) {

    companion object {
        const val SEQUENCE_LENGTH = 5
        const val VOCABULARY_SIZE = 15_000
        const val PAD_INDEX = 0
        const val UNKNOWN_INDEX = 1
        const val BEGIN_SEQUENCE_INDEX = 2
        const val DEFAULT_TOP_K = 5
    }

    @Volatile private var modelInterpreter: Interpreter? = null
    private val inferenceLock = Mutex()

    private val inputBuffer = Array(1) { IntArray(SEQUENCE_LENGTH) }
    private val outputBuffer = Array(1) { FloatArray(VOCABULARY_SIZE) }

    suspend fun predictNextWords(
        recentWordIndices: List<Int>,
        indexToWordMap: Map<Int, String>,
        k: Int = DEFAULT_TOP_K
    ): List<String> = inferenceLock.withLock {
        val interpreter = getOrCreateInterpreter() ?: return@withLock emptyList()

        val lastNIndices = recentWordIndices.takeLast(SEQUENCE_LENGTH)
        val paddingSize = SEQUENCE_LENGTH - lastNIndices.size
        for (i in 0 until SEQUENCE_LENGTH) {
            inputBuffer[0][i] = if (i < paddingSize) {
                PAD_INDEX
            } else {
                lastNIndices[i - paddingSize]
            }
        }

        outputBuffer[0].fill(0f)

        try {
            interpreter.run(inputBuffer, outputBuffer)
        } catch (e: Exception) {
            return@withLock emptyList()
        }

        val probabilities = outputBuffer[0]
        val topCandidateIndices = probabilities.indices
            .asSequence()
            .filter { it > BEGIN_SEQUENCE_INDEX }
            .sortedByDescending { probabilities[it] }
            .take(k)
            .toList()

        topCandidateIndices.mapNotNull { indexToWordMap[it] }.filter { it.isNotBlank() }
    }

    fun wordsToIndices(words: List<String>, vocabulary: Map<String, Int>): List<Int> =
        words.map { vocabulary[it] ?: UNKNOWN_INDEX }

    private suspend fun getOrCreateInterpreter(): Interpreter? {
        if (modelInterpreter != null) return modelInterpreter
        return try {
            val buffer = assetLoader.modelBuffer()
            val interpreterOptions = Interpreter.Options().apply {
                numThreads = 2
                useNNAPI = false
            }
            Interpreter(buffer, interpreterOptions).also { modelInterpreter = it }
        } catch (e: Exception) {
            null
        }
    }

    fun close() {
        modelInterpreter?.close()
        modelInterpreter = null
    }
}