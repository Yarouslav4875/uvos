package com.uvos.notes.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = UVOSAmber,
    onPrimary = UVOSBackground,
    secondary = UVOSCyan,
    onSecondary = UVOSBackground,
    tertiary = UVOSGreen,
    background = UVOSBackground,
    onBackground = UVOSTextPrimary,
    surface = UVOSSurface,
    onSurface = UVOSTextPrimary,
    surfaceVariant = UVOSPanel,
    onSurfaceVariant = UVOSTextSecondary,
    outline = UVOSBorder,
    error = UVOSRed
)

@Composable
fun UVOSNotesTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
