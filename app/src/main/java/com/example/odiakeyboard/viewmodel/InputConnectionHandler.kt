package com.example.odiakeyboard.viewmodel

interface InputConnectionHandler {
    fun commitText(text: String)

    fun deleteSurroundingText(beforeLength: Int = 1, afterLength: Int = 0)

    fun performEditorAction(actionId: Int)

    fun sendKeyEvent(keyCode: Int, metaState: Int = 0)
}