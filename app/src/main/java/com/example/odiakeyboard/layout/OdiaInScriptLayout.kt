package com.example.odiakeyboard.layout

import com.example.odiakeyboard.model.KeyData
import com.example.odiakeyboard.model.KeyType

/**
 * Standard Odia InScript keyboard layout (Unicode block: U+0B00–U+0B7F).
 *
 * Layout mirrors the Government of India's InScript standard for Odia.
 * Each list represents one visual row, left-to-right.
 *
 * This object is PURE DATA — no logic, no Android imports, fully unit-testable.
 */
object OdiaInScriptLayout {

    // ─── Vowel Matras & Consonants (QWERTY mapping) ──────────────────────────

    /**
     * Top character row — maps to keyboard row starting at 'Q'.
     *
     * Unshifted: vowel matras (dependent vowels)
     * Shifted:   independent vowels / voiced consonants
     */
    val row1: List<KeyData> = listOf(
        // Q        → ୌ  (matra AU) / ଔ (vowel AU)
        KeyData("ୌ", "\u0B4C", "ଔ", "\u0B14"),
        // W        → ୈ  (matra AI) / ଐ (vowel AI)
        KeyData("ୈ", "\u0B48", "ଐ", "\u0B10"),
        // E        → ା  (matra AA) / ଆ (vowel AA)
        KeyData("ା", "\u0B3E", "ଆ", "\u0B06"),
        // R        → ୀ  (matra II) / ଈ (vowel II)
        KeyData("ୀ", "\u0B40", "ଈ", "\u0B08"),
        // T        → ୂ  (matra UU) / ଊ (vowel UU)
        KeyData("ୂ", "\u0B42", "ଊ", "\u0B0A"),
        // Y        → ବ  (Ba)       / ଭ (Bha)
        KeyData("ବ", "\u0B2C", "ଭ", "\u0B2D"),
        // U        → ହ  (Ha)       / ଙ (Nga)
        KeyData("ହ", "\u0B39", "ଙ", "\u0B19"),
        // I        → ଗ  (Ga)       / ଘ (Gha)
        KeyData("ଗ", "\u0B17", "ଘ", "\u0B18"),
        // O        → ଦ  (Da)       / ଧ (Dha)
        KeyData("ଦ", "\u0B26", "ଧ", "\u0B27"),
        // P        → ଜ  (Ja)       / ଝ (Jha)
        KeyData("ଜ", "\u0B1C", "ଝ", "\u0B1D"),
        // [        → ଡ  (ḍa)       / ଢ (ḍha)
        KeyData("ଡ", "\u0B21", "ଢ", "\u0B22"),
    )

    /**
     * Home row — maps to keyboard row starting at 'A'.
     */
    val row2: List<KeyData> = listOf(
        // A        → ୋ  (matra O)  / ଓ (vowel O)
        KeyData("ୋ", "\u0B4B", "ଓ", "\u0B13"),
        // S        → େ  (matra E)  / ଏ (vowel E)
        KeyData("େ", "\u0B47", "ଏ", "\u0B0F"),
        // D        → ୍  (halant)   / ଅ (vowel A)
        KeyData("୍", "\u0B4D", "ଅ", "\u0B05", contentDescription = "Halanta / Vowel A"),
        // F        → ି  (matra I)  / ଇ (vowel I)
        KeyData("ି", "\u0B3F", "ଇ", "\u0B07"),
        // G        → ୁ  (matra U)  / ଉ (vowel U)
        KeyData("ୁ", "\u0B41", "ଉ", "\u0B09"),
        // H        → ପ  (Pa)       / ଫ (Pha)
        KeyData("ପ", "\u0B2A", "ଫ", "\u0B2B"),
        // J        → ର  (Ra)       / ଡ଼ (Ṛa nukta)
        KeyData("ର", "\u0B30", "ଡ଼", "\u0B5C"),
        // K        → କ  (Ka)       / ଖ (Kha)
        KeyData("କ", "\u0B15", "ଖ", "\u0B16"),
        // L        → ତ  (Ta)       / ଥ (Tha)
        KeyData("ତ", "\u0B24", "ଥ", "\u0B25"),
        // ;        → ଚ  (Ca)       / ଛ (Cha)
        KeyData("ଚ", "\u0B1A", "ଛ", "\u0B1B"),
        // '        → ଟ  (ṭa)       / ଠ (ṭha)
        KeyData("ଟ", "\u0B1F", "ଠ", "\u0B20"),
    )

    /**
     * Bottom character row — maps to keyboard row starting at 'Z'.
     */
    val row3: List<KeyData> = listOf(
        // Z        → ଁ  (chandrabindu) / ଃ (visarga)
        KeyData("ଁ", "\u0B01", "ଃ", "\u0B03", contentDescription = "Chandrabindu / Visarga"),
        // X        → ଂ  (anusvara)     / ଁ (chandrabindu)
        KeyData("ଂ", "\u0B02", "ଁ", "\u0B01", contentDescription = "Anusvara"),
        // C        → ମ  (Ma)           / ଣ (Ṇa)
        KeyData("ମ", "\u0B2E", "ଣ", "\u0B23"),
        // V        → ନ  (Na)           / ଵ (Va alt)
        KeyData("ନ", "\u0B28", "ଵ", "\u0B35"),
        // B        → ଵ  (Va)           / ଳ (Ḷa)
        KeyData("ଵ", "\u0B35", "ଳ", "\u0B33"),
        // N        → ଲ  (La)           / ଶ (Śa)
        KeyData("ଲ", "\u0B32", "ଶ", "\u0B36"),
        // M        → ସ  (Sa)           / ଷ (Ṣa)
        KeyData("ସ", "\u0B38", "ଷ", "\u0B37"),
        // ,        → ,                 / ।  (Danda)
        KeyData(",", ",", "।", "\u0964"),
        // .        → .                 / ॥  (Double Danda)
        KeyData(".", ".", "॥", "\u0965"),
        // /        → ଯ  (Ya)           / ଞ (Ña)
        KeyData("ଯ", "\u0B2F", "ଞ", "\u0B1E"),
    )

    /** Number row shown when mode == NUMBER */
    val numberRow: List<KeyData> = listOf(
        KeyData("1", "1"), KeyData("2", "2"), KeyData("3", "3"),
        KeyData("4", "4"), KeyData("5", "5"), KeyData("6", "6"),
        KeyData("7", "7"), KeyData("8", "8"), KeyData("9", "9"),
        KeyData("0", "0"),
    )

    /** Symbol row shown when mode == SYMBOL */
    val symbolRow1: List<KeyData> = listOf(
        KeyData("!", "!"),  KeyData("@", "@"),  KeyData("#", "#"),
        KeyData("₹", "₹"), KeyData("%", "%"),  KeyData("^", "^"),
        KeyData("&", "&"),  KeyData("*", "*"),  KeyData("(", "("),
        KeyData(")", ")"),
    )
    val symbolRow2: List<KeyData> = listOf(
        KeyData("-", "-"), KeyData("_", "_"), KeyData("=", "="),
        KeyData("+", "+"), KeyData("[", "["), KeyData("]", "]"),
        KeyData("{", "{"), KeyData("}", "}"), KeyData(";", ";"),
        KeyData(":", ":"),
    )
    val symbolRow3: List<KeyData> = listOf(
        KeyData("'", "'"), KeyData("\"","\""), KeyData("/", "/"),
        KeyData("\\","\\"),KeyData("|", "|"), KeyData("<", "<"),
        KeyData(">", ">"), KeyData("?", "?"), KeyData("`", "`"),
        KeyData("~", "~"),
    )

    // ─── Functional keys ──────────────────────────────────────────────────────

    val shiftKey = KeyData(
        displayLabel = "⇧", code = "", keyType = KeyType.SHIFT, widthWeight = 1.5f,
        contentDescription = "Shift"
    )
    val backspaceKey = KeyData(
        displayLabel = "⌫", code = "", keyType = KeyType.BACKSPACE, widthWeight = 1.5f,
        contentDescription = "Backspace"
    )
    val enterKey = KeyData(
        displayLabel = "↵", code = "\n", keyType = KeyType.ENTER, widthWeight = 2f,
        contentDescription = "Enter"
    )
    val spaceKey = KeyData(
        displayLabel = "ସ୍ପେସ୍", code = " ", keyType = KeyType.SPACE, widthWeight = 5f,
        contentDescription = "Space"
    )
    val symbolToggleKey = KeyData(
        displayLabel = "?123", code = "", keyType = KeyType.SYMBOL_TOGGLE, widthWeight = 1.5f,
        contentDescription = "Switch to numbers and symbols"
    )
    val alphaToggleKey = KeyData(
        displayLabel = "ଓଡ଼ିଆ", code = "", keyType = KeyType.SYMBOL_TOGGLE, widthWeight = 1.5f,
        contentDescription = "Switch to Odia"
    )

    /**
     * Returns the complete row-based layout for the given mode.
     * Each inner list is one keyboard row (top to bottom).
     */
    fun getRows(mode: com.example.odiakeyboard.model.KeyboardMode): List<List<KeyData>> =
        when (mode) {
            com.example.odiakeyboard.model.KeyboardMode.ALPHA -> listOf(
                row1,
                row2,
                row3,
            )
            com.example.odiakeyboard.model.KeyboardMode.NUMBER -> listOf(
                numberRow,
                symbolRow1,
            )
            com.example.odiakeyboard.model.KeyboardMode.SYMBOL -> listOf(
                symbolRow2,
                symbolRow3,
            )
        }
}