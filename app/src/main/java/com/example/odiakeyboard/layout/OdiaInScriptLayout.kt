package com.example.odiakeyboard.layout

import com.example.odiakeyboard.model.KeyData
import com.example.odiakeyboard.model.KeyType

object OdiaInScriptLayout {

    val row1: List<KeyData> = listOf(
        KeyData("ୌ", "\u0B4C", "ଔ", "\u0B14"),
        KeyData("ୈ", "\u0B48", "ଐ", "\u0B10"),
        KeyData("ା", "\u0B3E", "ଆ", "\u0B06"),
        KeyData("ୀ", "\u0B40", "ଈ", "\u0B08"),
        KeyData("ୂ", "\u0B42", "ଊ", "\u0B0A"),
        KeyData("ବ", "\u0B2C", "ଭ", "\u0B2D"),
        KeyData("ହ", "\u0B39", "ଙ", "\u0B19"),
        KeyData("ଗ", "\u0B17", "ଘ", "\u0B18"),
        KeyData("ଦ", "\u0B26", "ଧ", "\u0B27"),
        KeyData("ଜ", "\u0B1C", "ଝ", "\u0B1D"),
        KeyData("ଡ", "\u0B21", "ଢ", "\u0B22")
    )

    val row2: List<KeyData> = listOf(
        KeyData("ୋ", "\u0B4B", "ଓ", "\u0B13"),
        KeyData("େ", "\u0B47", "ଏ", "\u0B0F"),
        KeyData("୍", "\u0B4D", "ଅ", "\u0B05", contentDescription = "Halanta / Vowel A"),
        KeyData("ି", "\u0B3F", "ଇ", "\u0B07"),
        KeyData("ୁ", "\u0B41", "ଉ", "\u0B09"),
        KeyData("ପ", "\u0B2A", "ଫ", "\u0B2B"),
        KeyData("ର", "\u0B30", "ଡ଼", "\u0B5C"),
        KeyData("କ", "\u0B15", "ଖ", "\u0B16"),
        KeyData("ତ", "\u0B24", "ଥ", "\u0B25"),
        KeyData("ଚ", "\u0B1A", "ଛ", "\u0B1B"),
        KeyData("ଟ", "\u0B1F", "ଠ", "\u0B20")
    )

    val row3: List<KeyData> = listOf(
        KeyData("ଁ", "\u0B01", "ଃ", "\u0B03", contentDescription = "Chandrabindu / Visarga"),
        KeyData("ଂ", "\u0B02", "ଁ", "\u0B01", contentDescription = "Anusvara"),
        KeyData("ମ", "\u0B2E", "ଣ", "\u0B23"),
        KeyData("ନ", "\u0B28", "ଵ", "\u0B35"),
        KeyData("ଵ", "\u0B35", "ଳ", "\u0B33"),
        KeyData("ଲ", "\u0B32", "ଶ", "\u0B36"),
        KeyData("ସ", "\u0B38", "ଷ", "\u0B37"),
        KeyData(",", ",", "।", "\u0964"),
        KeyData(".", ".", "॥", "\u0965"),
        KeyData("ଯ", "\u0B2F", "ଞ", "\u0B1E")
    )

    val numberRow: List<KeyData> = listOf(
        KeyData("1", "1"), KeyData("2", "2"), KeyData("3", "3"),
        KeyData("4", "4"), KeyData("5", "5"), KeyData("6", "6"),
        KeyData("7", "7"), KeyData("8", "8"), KeyData("9", "9"),
        KeyData("0", "0")
    )

    val symbolRow1: List<KeyData> = listOf(
        KeyData("!", "!"), KeyData("@", "@"), KeyData("#", "#"),
        KeyData("₹", "₹"), KeyData("%", "%"), KeyData("^", "^"),
        KeyData("&", "&"), KeyData("*", "*"), KeyData("(", "("),
        KeyData(")", ")")
    )
    
    val symbolRow2: List<KeyData> = listOf(
        KeyData("-", "-"), KeyData("_", "_"), KeyData("=", "="),
        KeyData("+", "+"), KeyData("[", "["), KeyData("]", "]"),
        KeyData("{", "{"), KeyData("}", "}"), KeyData(";", ";"),
        KeyData(":", ":")
    )
    
    val symbolRow3: List<KeyData> = listOf(
        KeyData("'", "'"), KeyData("\"", "\""), KeyData("/", "/"),
        KeyData("\\", "\\"), KeyData("|", "|"), KeyData("<", "<"),
        KeyData(">", ">"), KeyData("?", "?"), KeyData("`", "`"),
        KeyData("~", "~")
    )

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

    fun getRows(mode: com.example.odiakeyboard.model.KeyboardMode): List<List<KeyData>> =
        when (mode) {
            com.example.odiakeyboard.model.KeyboardMode.ALPHA -> listOf(row1, row2, row3)
            com.example.odiakeyboard.model.KeyboardMode.NUMBER -> listOf(numberRow, symbolRow1)
            com.example.odiakeyboard.model.KeyboardMode.SYMBOL -> listOf(symbolRow2, symbolRow3)
        }
}