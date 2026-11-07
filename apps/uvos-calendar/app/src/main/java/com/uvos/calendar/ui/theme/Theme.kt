package com.uvos.calendar.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = M4200NeedleRed,
    background = UvosBg,
    surface = UvosPanel,
    surfaceVariant = UvosCard,
    onPrimary = Color.White,
    onBackground = Color(0xFFECEFF4),
    onSurface = Color(0xFFECEFF4),
    outline = UvosBorder
)

@Composable
fun UVOSCalendarTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
