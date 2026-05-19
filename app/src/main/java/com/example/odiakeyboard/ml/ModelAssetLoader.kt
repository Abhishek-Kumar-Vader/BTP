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

    @Volatile private var vocabulary: Map<String, Int>? = null
    suspend fun vocab(): Map<String, Int> = vocabulary ?: withContext(Dispatchers.IO) {
        loadJsonObjectAsIntMap("vocab.json").also { vocabulary = it }
    }

    @Volatile private var indexToWord: Map<Int, String>? = null
    suspend fun idx2word(): Map<Int, String> = indexToWord ?: withContext(Dispatchers.IO) {
        loadJsonObjectAsStringMap("idx2word.json")
            .mapKeys { it.key.toInt() }
            .also { indexToWord = it }
    }

    @Volatile private var isNgramDataLoaded = false
    @Volatile private var unigramCounts: Map<String, Int> = emptyMap()
    @Volatile private var totalTokensCount: Int = 0
    @Volatile private var bigramsForSuggestion: Map<String, List<String>> = emptyMap()
    @Volatile private var bigramsForCorrection: Map<String, Map<String, Int>> = emptyMap()

    private suspend fun loadNgramData() = withContext(Dispatchers.IO) {
        if (isNgramDataLoaded) return@withContext
        val rawJson = readAssetText("ngram_model.json")
        val rootObject = JSONObject(rawJson)

        val wordsObject = rootObject.getJSONObject("words")
        val uMap = HashMap<String, Int>(wordsObject.length())
        var tTokens = 0
        val uKeys = wordsObject.keys()
        while (uKeys.hasNext()) {
            val key = uKeys.next()
            val count = wordsObject.getInt(key)
            uMap[key] = count
            tTokens += count
        }
        unigramCounts = uMap
        totalTokensCount = tTokens

        val bigramsObject = rootObject.getJSONObject("bigrams")
        val suggestionMap = HashMap<String, List<String>>(bigramsObject.length())
        val correctionMap = HashMap<String, Map<String, Int>>(bigramsObject.length())
        val bKeys = bigramsObject.keys()

        while (bKeys.hasNext()) {
            val word = bKeys.next()
            val nextWordsArray = bigramsObject.getJSONArray(word)

            val nextWordsList = ArrayList<String>(minOf(nextWordsArray.length(), 10))
            val nextWordsWithCounts = HashMap<String, Int>(nextWordsArray.length())

            for (i in 0 until nextWordsArray.length()) {
                val entry = nextWordsArray.getJSONArray(i)
                val nextWord = entry.getString(0)
                val count = entry.getInt(1)

                if (i < 10) {
                    nextWordsList.add(nextWord)
                }
                nextWordsWithCounts[nextWord] = count
            }
            suggestionMap[word] = nextWordsList
            correctionMap[word] = nextWordsWithCounts
        }
        bigramsForSuggestion = suggestionMap
        bigramsForCorrection = correctionMap
        isNgramDataLoaded = true
    }

    suspend fun unigrams(): Map<String, Int> {
        loadNgramData()
        return unigramCounts
    }

    suspend fun totalTokens(): Int {
        loadNgramData()
        return totalTokensCount
    }

    suspend fun bigrams(): Map<String, List<String>> {
        loadNgramData()
        return bigramsForSuggestion
    }

    suspend fun bigramsWithCounts(): Map<String, Map<String, Int>> {
        loadNgramData()
        return bigramsForCorrection
    }

    @Volatile private var prefixIndexMap: Map<String, List<String>>? = null
    @Volatile private var validWordsList: List<String>? = null
    @Volatile private var validWordsSet: Set<String>? = null

    suspend fun wordSet(): Set<String> = validWordsSet ?: withContext(Dispatchers.IO) {
        validWords().toSet().also { validWordsSet = it }
    }

    suspend fun prefixIndex(): Map<String, List<String>> = prefixIndexMap ?: withContext(Dispatchers.IO) {
        buildPrefixIndexAndDictionary().also { prefixIndexMap = it }
    }

    suspend fun validWords(): List<String> = validWordsList ?: withContext(Dispatchers.IO) {
        buildPrefixIndexAndDictionary()
        validWordsList!!
    }

    private fun buildPrefixIndexAndDictionary(): Map<String, List<String>> {
        val rawJson = readAssetText("odia_dictionary.json")
        val index = HashMap<String, MutableList<String>>()
        val stripped = rawJson.trim().removePrefix("[").removeSuffix("]")
        val tokens = stripped.split("\",\"")
        val flatList = ArrayList<String>(tokens.size)

        for (token in tokens) {
            val word = token.trim().removePrefix("\"").removeSuffix("\"")
            if (word.isBlank()) continue

            flatList.add(word)
            val key = word[0].toString()
            index.getOrPut(key) { mutableListOf() }.add(word)
        }

        validWordsList = flatList
        return index.mapValues { it.value.toList() }
    }

    @Volatile private var modelByteBuffer: MappedByteBuffer? = null
    suspend fun modelBuffer(): MappedByteBuffer = modelByteBuffer ?: withContext(Dispatchers.IO) {
        context.assets.openFd("lstm_model.tflite").use { afd ->
            afd.createInputStream().channel.use { channel ->
                channel.map(
                    FileChannel.MapMode.READ_ONLY,
                    afd.startOffset,
                    afd.declaredLength
                ).also { modelByteBuffer = it }
            }
        }
    }

    private fun readAssetText(fileName: String): String {
        val stringBuilder = StringBuilder()
        context.assets.open(fileName).use { stream ->
            BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    stringBuilder.append(line)
                }
            }
        }
        return stringBuilder.toString()
    }

    private fun loadJsonObjectAsStringMap(fileName: String): Map<String, String> {
        val jsonObject = JSONObject(readAssetText(fileName))
        val resultMap = HashMap<String, String>(jsonObject.length())
        val keys = jsonObject.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            resultMap[key] = jsonObject.getString(key)
        }
        return resultMap
    }

    private fun loadJsonObjectAsIntMap(fileName: String): Map<String, Int> {
        val jsonObject = JSONObject(readAssetText(fileName))
        val resultMap = HashMap<String, Int>(jsonObject.length())
        val keys = jsonObject.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            resultMap[key] = jsonObject.getInt(key)
        }
        return resultMap
    }
}