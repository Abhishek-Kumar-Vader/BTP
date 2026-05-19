package com.example.odiakeyboard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.odiakeyboard.model.InputType
import com.example.odiakeyboard.model.KeyData
import com.example.odiakeyboard.model.KeyType
import com.example.odiakeyboard.model.KeyboardMode
import com.example.odiakeyboard.model.KeyboardState
import com.example.odiakeyboard.model.ShiftState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class KeyboardViewModel(
    private val suggestionRepository: SuggestionRepository = NoOpSuggestionRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(KeyboardState())
    val uiState: StateFlow<KeyboardState> = _uiState.asStateFlow()

    @Volatile private var icHandler: InputConnectionHandler? = null

    private val currentWordBuffer = StringBuilder()
    private val wordHistory = ArrayDeque<String>(MAX_WORD_HISTORY + 1)

    companion object {
        private const val MAX_WORD_HISTORY = 5
    }

    private var suggestionJob: Job? = null

    @Volatile private var isLongPressingBackspace = false

    fun onStartInput(inputType: InputType) {
        _uiState.update { state ->
            state.copy(
                inputType = inputType,
                keyboardMode = if (inputType == InputType.NUMBER) KeyboardMode.NUMBER else KeyboardMode.ALPHA,
                shiftState = ShiftState.OFF,
                suggestions = emptyList()
            )
        }
        currentWordBuffer.clear()
        wordHistory.clear()
    }

    fun attachInputConnectionHandler(handler: InputConnectionHandler) {
        icHandler = handler
    }

    fun detachInputConnectionHandler() {
        icHandler = null
    }

    fun onKeyPressed(key: KeyData) {
        when (key.keyType) {
            KeyType.CHARACTER -> handleCharacterKey(key)
            KeyType.BACKSPACE -> handleBackspace()
            KeyType.SHIFT -> handleShift()
            KeyType.ENTER -> handleEnter()
            KeyType.SPACE -> handleSpace()
            KeyType.SYMBOL_TOGGLE -> handleSymbolToggle()
            KeyType.LANGUAGE_SWITCH -> { }
            KeyType.TAB -> icHandler?.commitText("\t")
        }
    }

    fun onSuggestionSelected(suggestion: String) {
        val deleteCount = currentWordBuffer.length
        if (deleteCount > 0) {
            icHandler?.deleteSurroundingText(deleteCount, 0)
        }
        icHandler?.commitText("$suggestion ")
        addToWordHistory(suggestion)
        currentWordBuffer.clear()
        fetchSuggestions()
    }

    fun onBackspaceLongPressStart() {
        isLongPressingBackspace = true
        viewModelScope.launch(Dispatchers.Main) {
            delay(300L)
            while (isLongPressingBackspace) {
                doBackspace()
                delay(50L)
            }
        }
    }

    fun onBackspaceLongPressEnd() {
        isLongPressingBackspace = false
    }

    private fun handleCharacterKey(key: KeyData) {
        val state = _uiState.value
        val textToCommit = when (state.shiftState) {
            ShiftState.OFF -> key.code
            ShiftState.ONE_SHOT,
            ShiftState.CAPS_LOCK -> key.shiftedCode
        }

        icHandler?.commitText(textToCommit)
        currentWordBuffer.append(textToCommit)

        if (state.shiftState == ShiftState.ONE_SHOT) {
            _uiState.update { it.copy(shiftState = ShiftState.OFF) }
        }
        fetchSuggestions()
    }

    private fun handleBackspace() {
        doBackspace()
        fetchSuggestions()
    }

    private fun doBackspace() {
        icHandler?.deleteSurroundingText(1, 0)
        if (currentWordBuffer.isNotEmpty()) {
            currentWordBuffer.deleteCharAt(currentWordBuffer.lastIndex)
        }
    }

    private fun handleShift() {
        _uiState.update { state ->
            state.copy(
                shiftState = when (state.shiftState) {
                    ShiftState.OFF -> ShiftState.ONE_SHOT
                    ShiftState.ONE_SHOT -> ShiftState.CAPS_LOCK
                    ShiftState.CAPS_LOCK -> ShiftState.OFF
                }
            )
        }
    }

    private fun handleSpace() {
        val typedText = currentWordBuffer.toString().trim()
        val topSuggestion = _uiState.value.suggestions.firstOrNull()

        val isCorrection = typedText.isNotEmpty() &&
                topSuggestion != null &&
                topSuggestion != typedText &&
                !topSuggestion.startsWith(typedText)

        if (isCorrection && topSuggestion != null) {
            icHandler?.deleteSurroundingText(typedText.length, 0)
            icHandler?.commitText("$topSuggestion ")
            addToWordHistory(topSuggestion)
        } else {
            icHandler?.commitText(" ")
            if (typedText.isNotEmpty()) {
                addToWordHistory(typedText)
            }
        }

        currentWordBuffer.clear()
        fetchSuggestions()
    }

    private fun handleEnter() {
        val word = currentWordBuffer.toString().trim()
        if (word.isNotEmpty()) {
            addToWordHistory(word)
        }
        currentWordBuffer.clear()
        icHandler?.performEditorAction(android.view.inputmethod.EditorInfo.IME_ACTION_UNSPECIFIED)
        clearSuggestions()
    }

    private fun handleSymbolToggle() {
        _uiState.update { state ->
            state.copy(
                keyboardMode = when (state.keyboardMode) {
                    KeyboardMode.ALPHA -> KeyboardMode.NUMBER
                    KeyboardMode.NUMBER -> KeyboardMode.SYMBOL
                    KeyboardMode.SYMBOL -> KeyboardMode.ALPHA
                },
                shiftState = ShiftState.OFF
            )
        }
        clearSuggestions()
    }

    private fun addToWordHistory(word: String) {
        if (wordHistory.size >= MAX_WORD_HISTORY) {
            wordHistory.removeFirst()
        }
        wordHistory.addLast(word)
    }

    private fun fetchSuggestions() {
        if (_uiState.value.keyboardMode != KeyboardMode.ALPHA) {
            clearSuggestions()
            return
        }
        suggestionJob?.cancel()
        suggestionJob = viewModelScope.launch(Dispatchers.Default) {
            suggestionRepository
                .getSuggestions(
                    currentWord = currentWordBuffer.toString(),
                    precedingWords = wordHistory.toList()
                )
                .catch { }
                .collect { suggestions ->
                    _uiState.update { it.copy(suggestions = suggestions) }
                }
        }
    }

    private fun clearSuggestions() {
        suggestionJob?.cancel()
        _uiState.update { it.copy(suggestions = emptyList()) }
    }
}