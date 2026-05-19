package com.example.odiakeyboard.ml

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OdiaSpellCorrectorTest {

    private lateinit var spellCorrector: OdiaSpellCorrector
    private lateinit var prefixIndex: Map<String, List<String>>
    private lateinit var wordSet: Set<String>

    @BeforeEach
    fun setup() {
        val validWords = listOf("ଓଡ଼ିଆ", "ସରକାର", "ଭାଷା", "ପାଣି", "ଓଡ଼ିଶା")
        wordSet = validWords.toSet()
        prefixIndex = validWords.groupBy { it[0].toString() }
        spellCorrector = OdiaSpellCorrector()
    }

    @Test
    fun testIdentityReturnsExactMatch() {
        val correctWord = "ଓଡ଼ିଆ"
        val corrections = spellCorrector.getCorrections(
            input = correctWord,
            prefixIndex = prefixIndex,
            wordSet = wordSet,
            maxResults = 3
        )

        println("Identity corrections: $corrections")
        println("WordSet contains ଓଡ଼ିଆ: ${wordSet.contains("ଓଡ଼ିଆ")}")
        assertEquals(1, corrections.size)
        assertEquals("ଓଡ଼ିଆ", corrections[0])
    }

    @Test
    fun testSpellCorrectionForPhoneticSibilantTypo() {
        val typo = "ଶରକାର"
        val corrections = spellCorrector.getCorrections(
            input = typo,
            prefixIndex = prefixIndex,
            wordSet = wordSet,
            maxResults = 3
        )

        assertTrue(corrections.isNotEmpty())
        assertEquals("ସରକାର", corrections[0])
    }

    @Test
    fun testSpellCorrectionForPhoneticVowelTypo() {
        val typo = "ପାଣୀ"
        val corrections = spellCorrector.getCorrections(
            input = typo,
            prefixIndex = prefixIndex,
            wordSet = wordSet,
            maxResults = 3
        )

        assertTrue(corrections.isNotEmpty())
        assertEquals("ପାଣି", corrections[0])
    }

    @Test
    fun testTopKReturnsMultipleSuggestions() {
        val typo = "ଓଡିଶା"
        val corrections = spellCorrector.getCorrections(
            input = typo,
            prefixIndex = prefixIndex,
            wordSet = wordSet,
            maxResults = 2
        )

        assertTrue(corrections.size <= 2)
        assertTrue(corrections.contains("ଓଡ଼ିଶା"))
    }
}