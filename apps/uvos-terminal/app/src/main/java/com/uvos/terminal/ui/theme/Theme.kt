package com.uvos.terminal.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = UvosCrimson,
    onPrimary = Color.White,
    primaryContainer = UvosDarkGray,
    secondary = UvosAmber,
    onSecondary = Color.Black,
    background = UvosBgMatte,
    onBackground = UvosCrimson,
    surface = UvosIndustrialBezel,
    onSurface = Color.White
)

@Composable
fun UVOSTerminalTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
