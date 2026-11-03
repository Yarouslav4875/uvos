package com.uvos.files.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = IndustrialAccent,
    onPrimary = UvosBg,
    primaryContainer = UvosCard,
    onPrimaryContainer = IndustrialTextPrimary,
    secondary = LampGreen,
    onSecondary = UvosBg,
    background = UvosBg,
    onBackground = IndustrialTextPrimary,
    surface = UvosPanel,
    onSurface = IndustrialTextPrimary,
    surfaceVariant = UvosSurface,
    onSurfaceVariant = IndustrialTextSecondary,
    outline = UvosBorder
)

@Composable
fun UvosFilesTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = IndustrialTypography,
        content = content
    )
}
