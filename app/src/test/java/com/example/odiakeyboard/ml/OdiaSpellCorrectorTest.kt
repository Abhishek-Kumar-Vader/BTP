package com.example.odiakeyboard.ml

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OdiaSpellCorrectorTest {

    private lateinit var spellCorrector: OdiaSpellCorrector

    @BeforeEach
    fun setup() {
        // Mocking a tiny dictionary for isolated testing
        val validWords = listOf("ଓଡ଼ିଆ", "ସରକାର", "ଭାଷା", "ପାଣି", "ଓଡ଼ିଶା")

        // The new engine only requires the valid dictionary words!
        spellCorrector = OdiaSpellCorrector(
            validWords = validWords
        )
    }

    @Test
    fun `test identity returns exact match`() {
        // If the user types a word that is perfectly correct, it should just return that word.
        val correctWord = "ଓଡ଼ିଆ"
        val corrections = spellCorrector.getCorrections(target = correctWord, topK = 3)

        assertEquals(1, corrections.size)
        assertEquals("ଓଡ଼ିଆ", corrections[0])
    }

    @Test
    fun `test spell correction for phonetic sibilant typo`() {
        // User types "ଶରକାର" (misspelled with 'ଶ') instead of "ସରକାର" (correct with 'ସ')
        val typo = "ଶରକାର"
        val corrections = spellCorrector.getCorrections(target = typo, topK = 3)

        // The first correction should be the valid dictionary word "ସରକାର"
        assertTrue(corrections.isNotEmpty())
        assertEquals("ସରକାର", corrections[0])
    }

    @Test
    fun `test spell correction for phonetic vowel typo`() {
        // User types "ପାଣୀ" (misspelled with long vowel 'ୀ') instead of "ପାଣି" (correct with short vowel 'ି')
        val typo = "ପାଣୀ"

        val corrections = spellCorrector.getCorrections(target = typo, topK = 3)

        assertTrue(corrections.isNotEmpty())
        assertEquals("ପାଣି", corrections[0])
    }

    @Test
    fun `test edit distance filters out complete gibberish`() {
        // User types something completely unrelated to any word in the dictionary
        val typo = "ଟଟଟଟଟ"

        // We set the maxDistance threshold so it doesn't suggest random unrelated words
        val corrections = spellCorrector.getCorrections(target = typo, maxDistance = 2.0f, topK = 3)

        // Should return empty because it's too far from any valid word
        assertTrue(corrections.isEmpty())
    }

    @Test
    fun `test topK returns multiple suggestions`() {
        // Testing if the corrector can return multiple candidates if they are within distance
        val typo = "ଓଡିଶା" // Missing dot under 'ଡ' and using 'ି'

        val corrections = spellCorrector.getCorrections(target = typo, topK = 2)

        // It should at least find "ଓଡ଼ିଶା"
        assertTrue(corrections.size <= 2)
        assertTrue(corrections.contains("ଓଡ଼ିଶା"))
    }

    @Test
    fun `test maxDistance constraint blocks phonetic typo when threshold is strictly low`() {
        // "ଭାସା" and "ଭାଷା" have a phonetic distance of 0.4f
        val typo = "ଭାସା"

        // Set maxDistance to 0.3f. Since 0.4f > 0.3f, it should be filtered out.
        val corrections = spellCorrector.getCorrections(target = typo, maxDistance = 0.3f)

        assertTrue(corrections.isEmpty(), "Should be empty because distance (0.4) exceeds 0.3")
    }
}