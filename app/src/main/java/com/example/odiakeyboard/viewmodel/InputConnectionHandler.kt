package com.example.odiakeyboard.viewmodel

/**
 * Decouples the ViewModel from Android's [android.view.inputmethod.InputConnection].
 *
 * The Service implements this interface and routes calls to the live connection.
 * The ViewModel receives a reference to this interface — never to InputConnection directly.
 *
 * Benefits:
 *  - ViewModel stays pure Kotlin (no Android framework import for IC)
 *  - Easily mockable in unit tests (no Robolectric needed for IC logic)
 *  - IC lifecycle (becomes stale on field change) is managed exclusively in the Service
 */
interface InputConnectionHandler {

    /** Commit a single character or composed string to the text field. */
    fun commitText(text: String)

    /** Delete the character immediately before the cursor. */
    fun deleteSurroundingText(beforeLength: Int = 1, afterLength: Int = 0)

    /** Send the action associated with the current editor (e.g. Done, Search, Send). */
    fun performEditorAction(actionId: Int)

    /** Send a raw key event (used for hardware-style key simulation, e.g. Enter). */
    fun sendKeyEvent(keyCode: Int, metaState: Int = 0)
}