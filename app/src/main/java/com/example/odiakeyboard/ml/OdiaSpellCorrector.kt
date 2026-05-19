package com.example.odiakeyboard.ml

import kotlin.math.abs

class OdiaSpellCorrector {
    companion object {
        private const val MAX_EDIT_DIST = 2
        private const val PHONETIC_COST = 0.4f
        private const val PROXIMITY_COST = 0.5f
        private const val NUKTA = '\u0B3C'

        val PHONETIC_GROUPS: List<List<String>> = listOf(
            listOf("ସ", "ଶ", "ଷ"),
            listOf("ନ", "ଣ"),
            listOf("ର", "ଡ଼", "ଢ଼"),
            listOf("ଲ", "ଳ"),
            listOf("ଦ", "ଧ"),
            listOf("ବ", "ଭ"),
            listOf("ପ", "ଫ"),
            listOf("କ", "ଖ"),
            listOf("ଗ", "ଘ"),
            listOf("ଚ", "ଛ"),
            listOf("ଜ", "ଝ"),
            listOf("ଟ", "ଠ"),
            listOf("ଡ", "ଢ"),
            listOf("ତ", "ଥ"),
            listOf("ି", "ୀ"),
            listOf("ୁ", "ୂ")
        )

        val KEYBOARD_PROXIMITY_GROUPS: List<List<String>> = listOf(
            listOf("କ", "ଖ", "ଗ", "ଘ"),
            listOf("ଚ", "ଛ", "ଜ", "ଝ"),
            listOf("ଟ", "ଠ", "ଡ", "ଢ"),
            listOf("ତ", "ଥ", "ଦ", "ଧ", "ନ"),
            listOf("ପ", "ଫ", "ବ", "ଭ", "ମ"),
            listOf("ଯ", "ର", "ଲ", "ଳ", "ୱ"),
            listOf("ା", "ି", "ୀ"),
            listOf("ୁ", "ୂ", "ୃ"),
            listOf("େ", "ୈ", "ୋ", "ୌ")
        )

        val clusterToPhoneticGroup: Map<String, Int> by lazy {
            buildMap { PHONETIC_GROUPS.forEachIndexed { i, g -> g.forEach { put(it, i) } } }
        }

        val clusterToProximityGroup: Map<String, Int> by lazy {
            buildMap { KEYBOARD_PROXIMITY_GROUPS.forEachIndexed { i, g -> g.forEach { put(it, i) } } }
        }
    }

    fun clusterSplit(word: String): List<String> {
        if (word.isEmpty()) return emptyList()
        val out = mutableListOf<String>()
        var i = 0
        while (i < word.length) {
            val next = if (i + 1 < word.length && word[i + 1] == NUKTA) {
                val substring = word.substring(i, i + 2)
                i += 2
                substring
            } else {
                val char = word[i].toString()
                i++
                char
            }
            out.add(next)
        }
        return out
    }

    fun phoneticLevenshtein(s1: String, s2: String): Float {
        val c1 = clusterSplit(s1)
        val c2 = clusterSplit(s2)
        if (c1 == c2) return 0f
        if (c1.isEmpty()) return c2.size.toFloat()
        if (c2.isEmpty()) return c1.size.toFloat()

        var previousDistances = FloatArray(c2.size + 1) { it.toFloat() }
        for (i in c1.indices) {
            val currentDistances = FloatArray(c2.size + 1) { (i + 1).toFloat() }
            for (j in c2.indices) {
                currentDistances[j + 1] = minOf(
                    currentDistances[j] + 1f,
                    previousDistances[j + 1] + 1f,
                    previousDistances[j] + substitutionCost(c1[i], c2[j])
                )
            }
            previousDistances = currentDistances
        }
        return previousDistances.last()
    }

    private fun substitutionCost(a: String, b: String): Float {
        if (a == b) return 0f
        val phoneticGroupA = clusterToPhoneticGroup[a]
        val phoneticGroupB = clusterToPhoneticGroup[b]
        if (phoneticGroupA != null && phoneticGroupA == phoneticGroupB) return PHONETIC_COST
        
        val proximityGroupA = clusterToProximityGroup[a]
        val proximityGroupB = clusterToProximityGroup[b]
        if (proximityGroupA != null && proximityGroupA == proximityGroupB) return PROXIMITY_COST
        
        return 1f
    }

    fun getCorrections(
        input: String,
        prefixIndex: Map<String, List<String>>,
        wordSet: Set<String>,
        maxResults: Int = 3
    ): List<String> {
        if (input.isBlank()) return emptyList()

        val inputClusters = clusterSplit(input)
        val inputLength = inputClusters.size
        
        val identityResult = if (wordSet.contains(input)) {
            listOf(input)
        } else {
            emptyList()
        }

        if (identityResult.isNotEmpty()) return identityResult

        val firstCluster = inputClusters.firstOrNull() ?: return emptyList()
        val bucketsToSearch = mutableSetOf(firstCluster[0].toString())

        clusterToPhoneticGroup[firstCluster]?.let { groupId ->
            PHONETIC_GROUPS[groupId].forEach { substitute ->
                bucketsToSearch.add(substitute[0].toString())
            }
        }

        data class ScoredCandidate(val word: String, val distance: Float)
        val candidates = mutableListOf<ScoredCandidate>()
        val distanceThreshold = MAX_EDIT_DIST + 0.5f

        for (bucketKey in bucketsToSearch) {
            val bucket = prefixIndex[bucketKey] ?: continue
            for (word in bucket) {
                val wordLength = clusterSplit(word).size
                if (abs(wordLength - inputLength) > MAX_EDIT_DIST) continue

                val distance = phoneticLevenshtein(input, word)
                if (distance <= distanceThreshold) {
                    candidates.add(ScoredCandidate(word, distance))
                }
            }
        }

        return (identityResult + candidates
            .sortedBy { it.distance }
            .map { it.word })
            .distinct()
            .take(maxResults)
    }
}