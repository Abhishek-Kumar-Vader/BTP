package com.example.odiakeyboard.model

import android.view.inputmethod.EditorInfo

data class KeyboardState(
    val keyboardMode: KeyboardMode = KeyboardMode.ALPHA,
    val shiftState: ShiftState = ShiftState.OFF,
    val inputType: InputType = InputType.TEXT,
    val suggestions: List<String> = emptyList()
)

enum class KeyboardMode { ALPHA, NUMBER, SYMBOL }

enum class ShiftState {
    OFF,
    ONE_SHOT,
    CAPS_LOCK
}

enum class InputType { TEXT, NUMBER, EMAIL, PASSWORD, URI }

fun Int.toKeyboardInputType(): InputType {
    val baseClass = this and android.text.InputType.TYPE_MASK_CLASS
    return when (baseClass) {
        android.text.InputType.TYPE_CLASS_NUMBER,
        android.text.InputType.TYPE_CLASS_PHONE -> InputType.NUMBER
        android.text.InputType.TYPE_CLASS_TEXT -> {
            val variation = this and android.text.InputType.TYPE_MASK_VARIATION
            when (variation) {
                android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
                android.text.InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS -> InputType.EMAIL
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD,
                android.text.InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
                android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD -> InputType.PASSWORD
                android.text.InputType.TYPE_TEXT_VARIATION_URI -> InputType.URI
                else -> InputType.TEXT
            }
        }
        else -> InputType.TEXT
    }
}