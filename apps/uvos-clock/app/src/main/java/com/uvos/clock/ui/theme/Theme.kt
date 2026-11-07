package com.uvos.clock.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NeedleRed,
    secondary = BrassGold,
    tertiary = TextPhosphor,
    background = GunmetalBlack,
    surface = IndustrialPanel,
    surfaceVariant = SlateCard,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color(0xFFE2E4EB),
    onSurface = Color(0xFFE2E4EB),
    outline = MetalBorder
)

@Composable
fun UVOSClockTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
