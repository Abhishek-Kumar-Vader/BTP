package com.example.odiakeyboard.ml

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.tensorflow.lite.Interpreter

/**
 * Thread-safe wrapper around the TFLite LSTM next-word prediction model.
 *
 * ─── Model contract (confirmed from binary inspection) ───────────────────────
 *  Input  tensor: "input_sequence"  shape=[1, 5]     dtype=INT32
 *         → The 5 most recent word indices (padded with 0 = <PAD> if fewer)
 *
 *  Output tensor: "output"          shape=[1, 15000]  dtype=FLOAT32
 *         → Softmax probability for each of the 15,000 vocabulary words
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * Threading: [Interpreter] is NOT thread-safe. All inference calls are
 * serialised behind [interpreterMutex]. Multiple coroutines can call
 * [predictNextWords] safely; they will queue behind the mutex.
 */
class LstmInferenceEngine(
    private val assetLoader: ModelAssetLoader,
) {
    companion object {
        const val SEQUENCE_LENGTH = 5
        const val VOCAB_SIZE      = 15_000
        const val PAD_INDEX       = 0
        const val UNK_INDEX       = 1
        const val BOS_INDEX       = 2
        const val TOP_K           = 5  // candidates returned per call
    }

    @Volatile private var interpreter: Interpreter? = null
    private val interpreterMutex = Mutex()

    // Pre-allocated buffers — reused across calls to avoid GC pressure
    private val inputBuffer  = Array(1) { IntArray(SEQUENCE_LENGTH) }
    private val outputBuffer = Array(1) { FloatArray(VOCAB_SIZE) }

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns the top-[k] predicted next words given [recentWordIndices].
     *
     * @param recentWordIndices  Up to 5 vocabulary indices of recent words,
     *                           most-recent LAST. Shorter sequences are
     *                           left-padded with [PAD_INDEX].
     * @param idx2word           Index → word reverse-lookup map.
     * @param k                  Number of top predictions to return.
     */
    suspend fun predictNextWords(
        recentWordIndices: List<Int>,
        idx2word: Map<Int, String>,
        k: Int = TOP_K,
    ): List<String> = interpreterMutex.withLock {
        val interp = getOrCreateInterpreter() ?: return@withLock emptyList()

        // Fill input: left-pad with PAD_INDEX
        val padded = recentWordIndices.takeLast(SEQUENCE_LENGTH)
        val padCount = SEQUENCE_LENGTH - padded.size
        for (i in 0 until SEQUENCE_LENGTH) {
            inputBuffer[0][i] = if (i < padCount) PAD_INDEX else padded[i - padCount]
        }

        // Clear output buffer
        outputBuffer[0].fill(0f)

        // Run inference — this is the hot path, stays on calling dispatcher
        try {
            interp.run(inputBuffer, outputBuffer)
        } catch (e: Exception) {
            return@withLock emptyList()
        }

        // Find top-k indices by probability
        // Exclude special tokens (PAD=0, UNK=1, BOS=2)
        val probs = outputBuffer[0]
        val topIndices = probs.indices
            .asSequence()
            .filter { it > BOS_INDEX }           // skip special tokens
            .sortedByDescending { probs[it] }
            .take(k)
            .toList()

        topIndices.mapNotNull { idx2word[it] }.filter { it.isNotBlank() }
    }

    /**
     * Converts a list of words to their vocabulary indices.
     * Unknown words map to [UNK_INDEX].
     */
    fun wordsToIndices(words: List<String>, vocab: Map<String, Int>): List<Int> =
        words.map { vocab[it] ?: UNK_INDEX }

    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun getOrCreateInterpreter(): Interpreter? {
        if (interpreter != null) return interpreter
        return try {
            val buffer = assetLoader.modelBuffer()
            val options = Interpreter.Options().apply {
                numThreads = 2          // 2 threads is optimal for this model size
                useNNAPI   = false      // NNAPI can have high init latency; keep disabled
            }
            Interpreter(buffer, options).also { interpreter = it }
        } catch (e: Exception) {
            null
        }
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}