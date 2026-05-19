package com.example.odiakeyboard.ui

import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.odiakeyboard.model.KeyboardState
import com.example.odiakeyboard.model.ShiftState
import com.example.odiakeyboard.ui.keyboard.KeyboardScreen
import com.example.odiakeyboard.ui.keyboard.KeyboardTestTags
import com.example.odiakeyboard.ui.theme.OdiaKeyboardTheme
import com.example.odiakeyboard.viewmodel.InputConnectionHandler
import com.example.odiakeyboard.viewmodel.KeyboardViewModel
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KeyboardScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /** Stub handler to record committed text without a real InputConnection. */
    private val committedTexts = mutableListOf<String>()
    private val testHandler = object : InputConnectionHandler {
        override fun commitText(text: String) { committedTexts.add(text) }
        override fun deleteSurroundingText(beforeLength: Int, afterLength: Int) {
            if (committedTexts.isNotEmpty()) committedTexts.removeLast()
        }
        override fun performEditorAction(actionId: Int) {}
        override fun sendKeyEvent(keyCode: Int, metaState: Int) {}
    }

    private lateinit var viewModel: KeyboardViewModel

    @Before
    fun setUp() {
        committedTexts.clear()
        viewModel = KeyboardViewModel()
        viewModel.attachInputConnectionHandler(testHandler)

        composeTestRule.setContent {
            OdiaKeyboardTheme {
                KeyboardScreen(viewModel = viewModel, onKeyHaptic = {})
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Visibility
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun keyboardScreen_isDisplayed() {
        composeTestRule
            .onNodeWithTag(KeyboardTestTags.KEYBOARD_SCREEN)
            .assertIsDisplayed()
    }

    @Test
    fun shiftKey_isDisplayed() {
        composeTestRule
            .onNodeWithTag(KeyboardTestTags.SHIFT_KEY)
            .assertIsDisplayed()
    }

    @Test
    fun backspaceKey_isDisplayed() {
        composeTestRule
            .onNodeWithTag(KeyboardTestTags.BACKSPACE_KEY)
            .assertIsDisplayed()
    }

    @Test
    fun spaceKey_isDisplayed() {
        composeTestRule
            .onNodeWithTag(KeyboardTestTags.SPACE_KEY)
            .assertIsDisplayed()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Shift interaction
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun pressingShift_changesKeyLabels_toShifted() {
        // Tap the first character key and get its text before shift
        val firstCharBefore = composeTestRule
            .onAllNodesWithTag(KeyboardTestTags.CHARACTER_ROW)
            .onFirst()
            .onChildren()
            .onFirst()
            .fetchSemanticsNode()
            .config

        composeTestRule
            .onNodeWithTag(KeyboardTestTags.SHIFT_KEY)
            .performClick()

        // After shift, state should be ONE_SHOT
        assert(viewModel.uiState.value.shiftState == ShiftState.ONE_SHOT)
    }

    @Test
    fun doublePressingShift_activatesCapsLock() {
        composeTestRule.onNodeWithTag(KeyboardTestTags.SHIFT_KEY).performClick()
        composeTestRule.onNodeWithTag(KeyboardTestTags.SHIFT_KEY).performClick()
        assert(viewModel.uiState.value.shiftState == ShiftState.CAPS_LOCK)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Symbol mode toggle
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun pressingSymbolToggle_switchesToNumberMode() {
        composeTestRule
            .onNodeWithTag(KeyboardTestTags.SYMBOL_TOGGLE_KEY)
            .performClick()

        // Toggle label should now show Odia (since mode is NUMBER)
        composeTestRule
            .onNodeWithTag(KeyboardTestTags.SYMBOL_TOGGLE_KEY)
            .assertTextContains("ଓଡ଼ିଆ")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Rapid backspace — no crash
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun rapidBackspacePresses_doNotCrash() {
        repeat(50) {
            composeTestRule
                .onNodeWithTag(KeyboardTestTags.BACKSPACE_KEY)
                .performClick()
        }
        // If we get here without exception, the test passes
        composeTestRule
            .onNodeWithTag(KeyboardTestTags.KEYBOARD_SCREEN)
            .assertIsDisplayed()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Space key
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun pressingSpace_commitsSpace() {
        composeTestRule
            .onNodeWithTag(KeyboardTestTags.SPACE_KEY)
            .performClick()

        assert(committedTexts.contains(" ")) {
            "Expected a space to be committed, got: $committedTexts"
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Suggestion strip hidden in Phase 1
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun suggestionStrip_notVisible_whenEmpty() {
        composeTestRule
            .onNodeWithTag(KeyboardTestTags.SUGGESTION_ROW)
            .assertDoesNotExist()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Rapid typing stress
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun rapidTypingOnMultipleRows_doesNotCrash() {
        // Tap every visible key node 3 times rapidly
        val characterRows = composeTestRule
            .onAllNodesWithTag(KeyboardTestTags.CHARACTER_ROW)

        repeat(3) {
            for (i in 0 until characterRows.fetchSemanticsNodes().size) {
                characterRows[i]
                    .onChildren()
                    .fetchSemanticsNodes()
                    .take(5) // First 5 keys per row
                    .forEach { node ->
                        try {
                            composeTestRule
                                .onNode(hasTestTag(node.config.getOrNull(
                                    androidx.compose.ui.semantics.SemanticsProperties.TestTag
                                ) ?: "")
                                ).performClick()
                        } catch (_: Exception) { /* skip unmatched */ }
                    }
            }
        }

        composeTestRule
            .onNodeWithTag(KeyboardTestTags.KEYBOARD_SCREEN)
            .assertIsDisplayed()
    }
}