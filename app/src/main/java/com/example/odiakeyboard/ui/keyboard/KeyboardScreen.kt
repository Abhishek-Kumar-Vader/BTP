package com.example.odiakeyboard.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.odiakeyboard.layout.OdiaInScriptLayout
import com.example.odiakeyboard.model.KeyboardMode
import com.example.odiakeyboard.model.ShiftState
import com.example.odiakeyboard.viewmodel.KeyboardViewModel

object KeyboardTestTags {
    const val KEYBOARD_SCREEN = "keyboard_screen"
    const val SUGGESTION_ROW = "suggestion_row"
    const val CHARACTER_ROW = "character_row"
    const val SHIFT_KEY = "shift_key"
    const val BACKSPACE_KEY = "backspace_key"
    const val SPACE_KEY = "space_key"
    const val SYMBOL_TOGGLE_KEY = "symbol_toggle_key"
    const val ENTER_KEY = "enter_key"
}

@Composable
fun KeyboardScreen(
    viewModel: KeyboardViewModel,
    onKeyHaptic: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.background)
            .testTag(KeyboardTestTags.KEYBOARD_SCREEN)
            .imePadding()
    ) {
        if (state.suggestions.isNotEmpty()) {
            SuggestionStrip(
                suggestions = state.suggestions,
                onSuggestionClick = { word -> viewModel.onSuggestionSelected(word) }
            )
            HorizontalDivider(color = colors.outline.copy(alpha = 0.3f))
        }

        val rows = OdiaInScriptLayout.getRows(state.keyboardMode)
        rows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .testTag(KeyboardTestTags.CHARACTER_ROW),
                horizontalArrangement = Arrangement.Center
            ) {
                row.forEach { key ->
                    KeyboardKey(
                        key = key,
                        shiftState = state.shiftState,
                        onKeyPress = { pressed ->
                            onKeyHaptic()
                            viewModel.onKeyPressed(pressed)
                        },
                        modifier = Modifier
                            .weight(key.widthWeight)
                            .height(56.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            KeyboardKey(
                key = OdiaInScriptLayout.shiftKey,
                shiftState = state.shiftState,
                onKeyPress = { onKeyHaptic(); viewModel.onKeyPressed(it) },
                modifier = Modifier
                    .weight(OdiaInScriptLayout.shiftKey.widthWeight)
                    .height(56.dp)
                    .testTag(KeyboardTestTags.SHIFT_KEY)
            )

            val toggleKey = if (state.keyboardMode == KeyboardMode.ALPHA) {
                OdiaInScriptLayout.symbolToggleKey
            } else {
                OdiaInScriptLayout.alphaToggleKey
            }

            KeyboardKey(
                key = toggleKey,
                shiftState = ShiftState.OFF,
                onKeyPress = { onKeyHaptic(); viewModel.onKeyPressed(it) },
                modifier = Modifier
                    .weight(toggleKey.widthWeight)
                    .height(56.dp)
                    .testTag(KeyboardTestTags.SYMBOL_TOGGLE_KEY)
            )

            KeyboardKey(
                key = OdiaInScriptLayout.spaceKey,
                shiftState = ShiftState.OFF,
                onKeyPress = { onKeyHaptic(); viewModel.onKeyPressed(it) },
                modifier = Modifier
                    .weight(OdiaInScriptLayout.spaceKey.widthWeight)
                    .height(56.dp)
                    .testTag(KeyboardTestTags.SPACE_KEY)
            )

            KeyboardKey(
                key = OdiaInScriptLayout.backspaceKey,
                shiftState = ShiftState.OFF,
                onKeyPress = { onKeyHaptic(); viewModel.onKeyPressed(it) },
                onBackspaceLongPressStart = { viewModel.onBackspaceLongPressStart() },
                onBackspaceLongPressEnd = { viewModel.onBackspaceLongPressEnd() },
                modifier = Modifier
                    .weight(OdiaInScriptLayout.backspaceKey.widthWeight)
                    .height(56.dp)
                    .testTag(KeyboardTestTags.BACKSPACE_KEY)
            )

            KeyboardKey(
                key = OdiaInScriptLayout.enterKey,
                shiftState = ShiftState.OFF,
                onKeyPress = { onKeyHaptic(); viewModel.onKeyPressed(it) },
                modifier = Modifier
                    .weight(OdiaInScriptLayout.enterKey.widthWeight)
                    .height(56.dp)
                    .testTag(KeyboardTestTags.ENTER_KEY)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SuggestionStrip(
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .testTag(KeyboardTestTags.SUGGESTION_ROW),
        verticalAlignment = Alignment.CenterVertically,
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        items(suggestions) { suggestion ->
            TextButton(
                onClick = { onSuggestionClick(suggestion) },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Text(
                    text = suggestion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            VerticalDivider(
                modifier = Modifier
                    .height(18.dp)
                    .padding(horizontal = 2.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            )
        }
    }
}