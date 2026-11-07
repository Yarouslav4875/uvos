package com.uvos.compass.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CompassDarkColorScheme = darkColorScheme(
    primary = UvosRed,
    onPrimary = UvosText,
    primaryContainer = UvosTile,
    secondary = UvosText,
    background = UvosBackground,
    surface = UvosTile,
    error = UvosRed,
    onBackground = UvosText,
    onSurface = UvosText
)

@Composable
fun UvosCompassTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CompassDarkColorScheme,
        typography = InstrumentTypography,
        content = content
    )
}
