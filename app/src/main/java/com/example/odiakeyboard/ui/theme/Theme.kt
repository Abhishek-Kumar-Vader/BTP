package com.example.odiakeyboard.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    background = KeyboardBgLight,
    surface    = KeySurfaceLight,
    primary    = AccentLight,
    onPrimary  = KeyLight,
    onSurface  = TextLight,
)

private val DarkColorScheme = darkColorScheme(
    background = KeyboardBgDark,
    surface    = KeySurfaceDark,
    primary    = AccentDark,
    onPrimary  = TextDark,
    onSurface  = TextDark,
)

@Composable
fun OdiaKeyboardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        content = content,
    )
}