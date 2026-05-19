package com.example.odiakeyboard.viewmodel

import app.cash.turbine.test
import com.example.odiakeyboard.model.KeyData
import com.example.odiakeyboard.model.KeyType
import com.example.odiakeyboard.model.KeyboardMode
import com.example.odiakeyboard.model.KeyboardState
import com.example.odiakeyboard.model.ShiftState
import com.example.odiakeyboard.model.InputType
import io.mockk.mockk
import io.mockk.verify
import io.mockk.every
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KeyboardViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: KeyboardViewModel
    private lateinit var mockHandler: InputConnectionHandler

    @BeforeAll
    fun setupAll() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterAll
    fun tearDownAll() {
        Dispatchers.resetMain()
    }

    @BeforeEach
    fun setup() {
        mockHandler = mockk(relaxed = true)
        viewModel = KeyboardViewModel()
        viewModel.attachInputConnectionHandler(mockHandler)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Initial state
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `initial state is ALPHA mode with shift OFF`() {
        val state = viewModel.uiState.value
        assertEquals(KeyboardMode.ALPHA, state.keyboardMode)
        assertEquals(ShiftState.OFF, state.shiftState)
        assertTrue(state.suggestions.isEmpty())
    }

    // ─────────────────────────────────────────────────────────────────────────
    // onStartInput
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `onStartInput with NUMBER type switches to NUMBER mode`() {
        viewModel.onStartInput(InputType.NUMBER)
        assertEquals(KeyboardMode.NUMBER, viewModel.uiState.value.keyboardMode)
    }

    @Test
    fun `onStartInput with TEXT type stays in ALPHA mode`() {
        viewModel.onStartInput(InputType.TEXT)
        assertEquals(KeyboardMode.ALPHA, viewModel.uiState.value.keyboardMode)
    }

    @Test
    fun `onStartInput resets shift state`() = runTest {
        viewModel.onKeyPressed(shiftKey())
        viewModel.onStartInput(InputType.TEXT)
        assertEquals(ShiftState.OFF, viewModel.uiState.value.shiftState)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Character key
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `character key commits unshifted code when shift is OFF`() {
        val key = KeyData("କ", "\u0B15", "ଖ", "\u0B16")
        viewModel.onKeyPressed(key)
        verify(exactly = 1) { mockHandler.commitText("\u0B15") }
    }

    @Test
    fun `character key commits shifted code when shift is ONE_SHOT`() {
        viewModel.onKeyPressed(shiftKey())          // shift ON
        val key = KeyData("କ", "\u0B15", "ଖ", "\u0B16")
        viewModel.onKeyPressed(key)
        verify(exactly = 1) { mockHandler.commitText("\u0B16") }
    }

    @Test
    fun `ONE_SHOT shift resets to OFF after one character`() {
        viewModel.onKeyPressed(shiftKey())
        assertEquals(ShiftState.ONE_SHOT, viewModel.uiState.value.shiftState)

        viewModel.onKeyPressed(KeyData("କ", "\u0B15", "ଖ", "\u0B16"))
        assertEquals(ShiftState.OFF, viewModel.uiState.value.shiftState)
    }

    @Test
    fun `CAPS_LOCK does not reset after character`() {
        viewModel.onKeyPressed(shiftKey())  // ONE_SHOT
        viewModel.onKeyPressed(shiftKey())  // CAPS_LOCK
        assertEquals(ShiftState.CAPS_LOCK, viewModel.uiState.value.shiftState)

        viewModel.onKeyPressed(KeyData("କ", "\u0B15", "ଖ", "\u0B16"))
        assertEquals(ShiftState.CAPS_LOCK, viewModel.uiState.value.shiftState)
    }

    @Test
    fun `shift cycles OFF → ONE_SHOT → CAPS_LOCK → OFF`() {
        assertEquals(ShiftState.OFF,       viewModel.uiState.value.shiftState)
        viewModel.onKeyPressed(shiftKey())
        assertEquals(ShiftState.ONE_SHOT,  viewModel.uiState.value.shiftState)
        viewModel.onKeyPressed(shiftKey())
        assertEquals(ShiftState.CAPS_LOCK, viewModel.uiState.value.shiftState)
        viewModel.onKeyPressed(shiftKey())
        assertEquals(ShiftState.OFF,       viewModel.uiState.value.shiftState)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Backspace
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `backspace calls deleteSurroundingText(1, 0)`() {
        viewModel.onKeyPressed(backspaceKey())
        verify(exactly = 1) { mockHandler.deleteSurroundingText(1, 0) }
    }

    @Test
    fun `rapid backspace presses do not crash`() {
        // Simulates rapid deletion — the key contract is no exception is thrown
        assertDoesNotThrow {
            repeat(200) { viewModel.onKeyPressed(backspaceKey()) }
        }
        verify(exactly = 200) { mockHandler.deleteSurroundingText(1, 0) }
    }

    @Test
    fun `backspace on empty word buffer does not crash`() {
        // No characters typed — buffer is empty, delete should still propagate
        assertDoesNotThrow { viewModel.onKeyPressed(backspaceKey()) }
        verify { mockHandler.deleteSurroundingText(1, 0) }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Space
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `space commits a space character`() {
        viewModel.onKeyPressed(spaceKey())
        verify(exactly = 1) { mockHandler.commitText(" ") }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Mode toggle
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `symbol toggle cycles ALPHA → NUMBER → SYMBOL → ALPHA`() {
        assertEquals(KeyboardMode.ALPHA,  viewModel.uiState.value.keyboardMode)
        viewModel.onKeyPressed(symbolToggleKey())
        assertEquals(KeyboardMode.NUMBER, viewModel.uiState.value.keyboardMode)
        viewModel.onKeyPressed(symbolToggleKey())
        assertEquals(KeyboardMode.SYMBOL, viewModel.uiState.value.keyboardMode)
        viewModel.onKeyPressed(symbolToggleKey())
        assertEquals(KeyboardMode.ALPHA,  viewModel.uiState.value.keyboardMode)
    }

    @Test
    fun `symbol toggle resets shift to OFF`() {
        viewModel.onKeyPressed(shiftKey())
        assertEquals(ShiftState.ONE_SHOT, viewModel.uiState.value.shiftState)
        viewModel.onKeyPressed(symbolToggleKey())
        assertEquals(ShiftState.OFF, viewModel.uiState.value.shiftState)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // StateFlow emissions (Turbine)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `uiState emits correct sequence for shift cycling`() = runTest {
        viewModel.uiState.test {
            val initial = awaitItem()
            assertEquals(ShiftState.OFF, initial.shiftState)

            viewModel.onKeyPressed(shiftKey())
            assertEquals(ShiftState.ONE_SHOT, awaitItem().shiftState)

            viewModel.onKeyPressed(shiftKey())
            assertEquals(ShiftState.CAPS_LOCK, awaitItem().shiftState)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Detach safety
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `key press after detach does not crash`() {
        viewModel.detachInputConnectionHandler()
        assertDoesNotThrow {
            viewModel.onKeyPressed(KeyData("କ", "\u0B15", "ଖ", "\u0B16"))
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Rapid typing stress test
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `rapid typing of 500 characters does not corrupt state`() {
        val chars = listOf(
            KeyData("କ", "\u0B15"), KeyData("ର", "\u0B30"), KeyData("ଣ", "\u0B23")
        )
        assertDoesNotThrow {
            repeat(500) { viewModel.onKeyPressed(chars[it % chars.size]) }
        }
        // State must still be valid after stress
        val state = viewModel.uiState.value
        assertNotNull(state)
        assertTrue(state.suggestions.isEmpty()) // no ML in Phase 1
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun shiftKey()        = KeyData("⇧", "", keyType = KeyType.SHIFT)
    private fun backspaceKey()    = KeyData("⌫", "", keyType = KeyType.BACKSPACE)
    private fun spaceKey()        = KeyData(" ", " ", keyType = KeyType.SPACE)
    private fun symbolToggleKey() = KeyData("?123", "", keyType = KeyType.SYMBOL_TOGGLE)
}
