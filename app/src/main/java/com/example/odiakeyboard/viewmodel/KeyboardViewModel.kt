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

/**
 * Single source of truth for keyboard state and key-press handling.
 *
 * ─── Phase 2 additions over Phase 1 ─────────────────────────────────────────
 * • [wordHistory]     — rolling deque of last 5 completed words.
 * Fed to [SuggestionRepository] as the LSTM context window.
 * • [currentWordBuffer] — characters typed since the last space; used for
 * both prefix matching (non-empty) and next-word
 * prediction (empty, after space).
 * • [suggestionJob]   — cancels the previous fetch before starting a new one
 * so rapid typing never queues up stale requests.
 * • Error boundary    — suggestion flow errors are caught and silently dropped;
 * the keyboard never crashes due to ML failure.
 *
 * ─── Threading model ─────────────────────────────────────────────────────────
 * Key events arrive on Main. Suggestion fetches are launched on
 * Dispatchers.Default (inside the repository's flowOn). The ViewModel only
 * touches _uiState.update{} from the flow collect, which is always safe.
 * ─────────────────────────────────────────────────────────────────────────────
 */
class KeyboardViewModel(
    private val suggestionRepository: SuggestionRepository = NoOpSuggestionRepository(),
) : ViewModel() {

    // ── UI State ──────────────────────────────────────────────────────────────

    private val _uiState = MutableStateFlow(KeyboardState())
    val uiState: StateFlow<KeyboardState> = _uiState.asStateFlow()

    // ── InputConnection handler ───────────────────────────────────────────────

    @Volatile private var icHandler: InputConnectionHandler? = null

    // ── Word tracking ─────────────────────────────────────────────────────────

    /** Characters typed since the last word boundary (space/enter). */
    private val currentWordBuffer = StringBuilder()

    /**
     * The last [MAX_WORD_HISTORY] completed words, oldest at index 0.
     * Fed to the LSTM as the 5-word context window.
     */
    private val wordHistory = ArrayDeque<String>(MAX_WORD_HISTORY + 1)

    companion object {
        private const val MAX_WORD_HISTORY = 5
    }

    // ── Suggestion job ────────────────────────────────────────────────────────

    /** Always cancelled before starting a new fetch — prevents stale updates. */
    private var suggestionJob: Job? = null

    // ── Rapid-delete guard ────────────────────────────────────────────────────

    @Volatile private var isLongPressingBackspace = false

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    fun onStartInput(inputType: InputType) {
        _uiState.update { state ->
            state.copy(
                inputType    = inputType,
                keyboardMode = if (inputType == InputType.NUMBER) KeyboardMode.NUMBER
                else KeyboardMode.ALPHA,
                shiftState   = ShiftState.OFF,
                suggestions  = emptyList(),
            )
        }
        currentWordBuffer.clear()
        wordHistory.clear()
    }

    fun attachInputConnectionHandler(handler: InputConnectionHandler) { icHandler = handler }
    fun detachInputConnectionHandler() { icHandler = null }

    // ─────────────────────────────────────────────────────────────────────────
    // Key event processing
    // ─────────────────────────────────────────────────────────────────────────

    fun onKeyPressed(key: KeyData) {
        when (key.keyType) {
            KeyType.CHARACTER       -> handleCharacterKey(key)
            KeyType.BACKSPACE       -> handleBackspace()
            KeyType.SHIFT           -> handleShift()
            KeyType.ENTER           -> handleEnter()
            KeyType.SPACE           -> handleSpace()
            KeyType.SYMBOL_TOGGLE   -> handleSymbolToggle()
            KeyType.LANGUAGE_SWITCH -> { /* Phase 3 */ }
            KeyType.TAB             -> icHandler?.commitText("\t")
        }
    }

    /**
     * Tap-to-complete: user selects a word from the suggestion strip.
     *
     * Replaces the current partial word with the full [suggestion],
     * commits a trailing space, and adds it to [wordHistory].
     */
    fun onSuggestionSelected(suggestion: String) {
        // Delete the partial word currently in the buffer
        val deleteCount = currentWordBuffer.length
        if (deleteCount > 0) {
            icHandler?.deleteSurroundingText(deleteCount, 0)
        }
        // Commit the full suggestion + space
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

    fun onBackspaceLongPressEnd() { isLongPressingBackspace = false }

    // ─────────────────────────────────────────────────────────────────────────
    // Private handlers
    // ─────────────────────────────────────────────────────────────────────────

    private fun handleCharacterKey(key: KeyData) {
        val state       = _uiState.value
        val textToCommit = when (state.shiftState) {
            ShiftState.OFF       -> key.code
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

    private fun handleBackspace() { doBackspace(); fetchSuggestions() }

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
                    ShiftState.OFF       -> ShiftState.ONE_SHOT
                    ShiftState.ONE_SHOT  -> ShiftState.CAPS_LOCK
                    ShiftState.CAPS_LOCK -> ShiftState.OFF
                }
            )
        }
    }

    private fun handleEnter() {
        // Commit current buffer word to history before Enter
        val word = currentWordBuffer.toString().trim()
        if (word.isNotEmpty()) addToWordHistory(word)
        currentWordBuffer.clear()

        icHandler?.performEditorAction(
            android.view.inputmethod.EditorInfo.IME_ACTION_UNSPECIFIED
        )
        clearSuggestions()
    }

    private fun handleSpace() {
        val completedWord = currentWordBuffer.toString().trim()
        val state = _uiState.value
        val topSuggestion = state.suggestions.firstOrNull()

        if (completedWord.isNotEmpty()) {
            // ── AUTOCORRECT HEURISTIC ─────────────────────────────────────────
            // If the top suggestion does NOT start with what the user typed,
            // it means the ML engine provided a typo correction (Levenshtein)
            // rather than a prefix autocomplete. In this case, we Auto-Correct.
            val isCorrection = topSuggestion != null &&
                    !topSuggestion.startsWith(completedWord, ignoreCase = true)

            if (isCorrection) {
                // 1. Delete the misspelled word from the input field
                val deleteCount = completedWord.length
                icHandler?.deleteSurroundingText(deleteCount, 0)

                // 2. Commit the corrected word plus the trailing space
                icHandler?.commitText("$topSuggestion ")
                addToWordHistory(topSuggestion!!)
            } else {
                // Not a correction (either exact match or just an autocomplete prefix),
                // so we just commit the space and keep what the user typed.
                icHandler?.commitText(" ")
                addToWordHistory(completedWord)
            }
        } else {
            icHandler?.commitText(" ")
        }

        currentWordBuffer.clear()
        fetchSuggestions()
    }

    private fun handleSymbolToggle() {
        _uiState.update { state ->
            state.copy(
                keyboardMode = when (state.keyboardMode) {
                    KeyboardMode.ALPHA   -> KeyboardMode.NUMBER
                    KeyboardMode.NUMBER  -> KeyboardMode.SYMBOL
                    KeyboardMode.SYMBOL  -> KeyboardMode.ALPHA
                },
                shiftState = ShiftState.OFF,
            )
        }
        clearSuggestions()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Word history management
    // ─────────────────────────────────────────────────────────────────────────

    private fun addToWordHistory(word: String) {
        if (wordHistory.size >= MAX_WORD_HISTORY) wordHistory.removeFirst()
        wordHistory.addLast(word)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Suggestion fetching
    // ─────────────────────────────────────────────────────────────────────────

    private fun fetchSuggestions() {
        // Only fetch suggestions in ALPHA mode — not during number/symbol input
        if (_uiState.value.keyboardMode != KeyboardMode.ALPHA) {
            clearSuggestions()
            return
        }

        // Cancel any in-flight fetch — rapid typing would otherwise queue requests
        suggestionJob?.cancel()
        suggestionJob = viewModelScope.launch(Dispatchers.Default) {
            suggestionRepository
                .getSuggestions(
                    currentWord    = currentWordBuffer.toString(),
                    precedingWords = wordHistory.toList(),
                )
                .catch { /* silently ignore ML errors; keyboard must never crash */ }
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