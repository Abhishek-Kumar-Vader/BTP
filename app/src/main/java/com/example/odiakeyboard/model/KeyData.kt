package com.example.odiakeyboard.model

data class KeyData(
    val displayLabel: String,
    val code: String,
    val shiftedLabel: String = displayLabel,
    val shiftedCode: String = code,
    val keyType: KeyType = KeyType.CHARACTER,
    val widthWeight: Float = 1f,
    val contentDescription: String = displayLabel
)

enum class KeyType {
    CHARACTER,
    BACKSPACE,
    ENTER,
    SHIFT,
    SPACE,
    SYMBOL_TOGGLE,
    LANGUAGE_SWITCH,
    TAB
}