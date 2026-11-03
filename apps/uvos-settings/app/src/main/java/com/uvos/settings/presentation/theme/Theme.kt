package com.uvos.settings.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = UvosRed,
    secondary = UvosAmberLamp,
    tertiary = UvosGreenLamp,
    background = UvosBackground,
    surface = UvosTile,
    onPrimary = UvosTextPrimary,
    onSecondary = UvosDialText,
    onBackground = UvosTextPrimary,
    onSurface = UvosTextPrimary
)

@Composable
fun UvosSettingsTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = UvosTypography,
        content = content
    )
}
