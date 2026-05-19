package com.example.odiakeyboard.model

import android.view.inputmethod.EditorInfo

/**
 * Snapshot of all mutable keyboard UI state.
 * StateFlow<KeyboardState> drives the entire Compose tree — one source of truth.
 */
data class KeyboardState(
    val keyboardMode: KeyboardMode = KeyboardMode.ALPHA,
    val shiftState: ShiftState = ShiftState.OFF,
    val inputType: InputType = InputType.TEXT,
    // Populated by SuggestionRepository in Phase 2; empty list in Phase 1.
    val suggestions: List<String> = emptyList(),
)

enum class KeyboardMode { ALPHA, NUMBER, SYMBOL }

enum class ShiftState {
    OFF,         // no shift
    ONE_SHOT,    // next character shifted, then resets
    CAPS_LOCK    // all characters shifted until toggled off
}

/**
 * Derived from EditorInfo.inputType at IME start.
 * Drives which keyboard panel the user sees initially.
 */
enum class InputType { TEXT, NUMBER, EMAIL, PASSWORD, URI }

fun Int.toKeyboardInputType(): InputType {
    val baseType = this and android.text.InputType.TYPE_MASK_CLASS
    return when (baseType) {
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