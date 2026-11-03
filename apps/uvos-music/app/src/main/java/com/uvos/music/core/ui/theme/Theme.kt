package com.uvos.music.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val StudioDarkColorScheme = darkColorScheme(
    primary = M4200AmberGlow,
    onPrimary = M4200ChassisBg,
    primaryContainer = M4200ChassisCard,
    secondary = M4200AccentCyan,
    background = M4200ChassisBg,
    surface = M4200ChassisCard,
    error = M4200OverloadRed,
    onBackground = M4200TextPrimary,
    onSurface = M4200TextPrimary
)

@Composable
fun UvosMusicTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = StudioDarkColorScheme,
        typography = StudioTypography,
        content = content
    )
}
