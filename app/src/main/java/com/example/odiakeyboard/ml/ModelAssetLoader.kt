package com.example.odiakeyboard.ml

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.channels.FileChannel
import java.nio.MappedByteBuffer

class ModelAssetLoader(private val context: Context) {

    // ── vocab: word → index ───────────────────────────────────────────────────
    @Volatile private var _vocab: Map<String, Int>? = null
    suspend fun vocab(): Map<String, Int> = _vocab ?: withContext(Dispatchers.IO) {
        loadJsonObjectAsIntMap("vocab.json").also { _vocab = it }
    }

    // ── idx2word: index → word ────────────────────────────────────────────────
    @Volatile private var _idx2word: Map<Int, String>? = null
    suspend fun idx2word(): Map<Int, String> = _idx2word ?: withContext(Dispatchers.IO) {
        loadJsonObjectAsStringMap("idx2word.json")
            .mapKeys { it.key.toInt() }
            .also { _idx2word = it }
    }

    // ── N-Gram Data (Unigrams, Total Tokens, and Bigrams) ─────────────────────
    @Volatile private var _ngramDataLoaded = false
    @Volatile private var _unigrams: Map<String, Int> = emptyMap()
    @Volatile private var _totalTokens: Int = 0
    @Volatile private var _bigramsForEngine: Map<String, List<String>> = emptyMap()
    @Volatile private var _bigramsForCorrector: Map<String, Map<String, Int>> = emptyMap()

    private suspend fun loadNgramData() = withContext(Dispatchers.IO) {
        if (_ngramDataLoaded) return@withContext
        val raw = readAssetText("ngram_model.json")
        val root = JSONObject(raw)

        // 1. Parse Unigrams & Total Tokens
        val wordsObj = root.getJSONObject("words")
        val uMap = HashMap<String, Int>(wordsObj.length())
        var tTokens = 0
        val uKeys = wordsObj.keys()
        while (uKeys.hasNext()) {
            val k = uKeys.next()
            val count = wordsObj.getInt(k)
            uMap[k] = count
            tTokens += count
        }
        _unigrams = uMap
        _totalTokens = tTokens

        // 2. Parse Bigrams (Creating both formats needed by the app)
        val bigramObj = root.getJSONObject("bigrams")
        val engineMap = HashMap<String, List<String>>(bigramObj.length())
        val correctorMap = HashMap<String, Map<String, Int>>(bigramObj.length())
        val bKeys = bigramObj.keys()

        while (bKeys.hasNext()) {
            val word = bKeys.next()
            val arr = bigramObj.getJSONArray(word)

            val nextWordsList = ArrayList<String>(minOf(arr.length(), 10))
            val nextWordsCounts = HashMap<String, Int>(arr.length())

            for (i in 0 until arr.length()) {
                val entry = arr.getJSONArray(i)
                val nextWord = entry.getString(0)
                val count = entry.getInt(1)

                if (i < 10) nextWordsList.add(nextWord)
                nextWordsCounts[nextWord] = count
            }
            engineMap[word] = nextWordsList
            correctorMap[word] = nextWordsCounts
        }
        _bigramsForEngine = engineMap
        _bigramsForCorrector = correctorMap
        _ngramDataLoaded = true
    }

    // Public Getters for N-gram data
    suspend fun unigrams(): Map<String, Int> { loadNgramData(); return _unigrams }
    suspend fun totalTokens(): Int { loadNgramData(); return _totalTokens }
    suspend fun bigrams(): Map<String, List<String>> { loadNgramData(); return _bigramsForEngine }
    suspend fun bigramsWithCounts(): Map<String, Map<String, Int>> { loadNgramData(); return _bigramsForCorrector }


    // ── Dictionary & Prefix index ─────────────────────────────────────────────
    @Volatile private var _prefixIndex: Map<String, List<String>>? = null
    @Volatile private var _validWords: List<String>? = null

    suspend fun prefixIndex(): Map<String, List<String>> = _prefixIndex
        ?: withContext(Dispatchers.IO) {
            buildPrefixIndexAndDictionary().also { _prefixIndex = it }
        }

    suspend fun validWords(): List<String> = _validWords
        ?: withContext(Dispatchers.IO) {
            buildPrefixIndexAndDictionary()
            _validWords!!
        }

    private fun buildPrefixIndexAndDictionary(): Map<String, List<String>> {
        val raw      = readAssetText("odia_dictionary.json")
        val index    = HashMap<String, MutableList<String>>()
        val stripped = raw.trim().removePrefix("[").removeSuffix("]")
        val tokens   = stripped.split("\",\"")
        val flatList = ArrayList<String>(tokens.size)

        for (token in tokens) {
            val word = token.trim().removePrefix("\"").removeSuffix("\"")
            if (word.isBlank()) continue

            flatList.add(word) // Save for the Spell Corrector

            val key = word[0].toString()
            index.getOrPut(key) { mutableListOf() }.add(word)
        }

        _validWords = flatList
        return index.mapValues { it.value.toList() }
    }

    // ── TFLite model as MappedByteBuffer ──────────────────────────────────────
    @Volatile private var _modelBuffer: MappedByteBuffer? = null
    suspend fun modelBuffer(): MappedByteBuffer = _modelBuffer ?: withContext(Dispatchers.IO) {
        context.assets.openFd("lstm_model.tflite").use { afd ->
            afd.createInputStream().channel.use { channel ->
                channel.map(
                    FileChannel.MapMode.READ_ONLY,
                    afd.startOffset,
                    afd.declaredLength,
                ).also { _modelBuffer = it }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun readAssetText(fileName: String): String {
        val sb = StringBuilder()
        context.assets.open(fileName).use { stream ->
            BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) sb.append(line)
            }
        }
        return sb.toString()
    }

    private fun loadJsonObjectAsStringMap(fileName: String): Map<String, String> {
        val obj = JSONObject(readAssetText(fileName))
        val map = HashMap<String, String>(obj.length())
        val keys = obj.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            map[k] = obj.getString(k)
        }
        return map
    }

    private fun loadJsonObjectAsIntMap(fileName: String): Map<String, Int> {
        val obj = JSONObject(readAssetText(fileName))
        val map = HashMap<String, Int>(obj.length())
        val keys = obj.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            map[k] = obj.getInt(k)
        }
        return map
    }
}