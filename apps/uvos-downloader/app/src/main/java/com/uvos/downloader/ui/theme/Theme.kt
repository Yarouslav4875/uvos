package com.uvos.downloader.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = GaugeNeedleRed,
    onPrimary = Color.White,
    secondary = LampGreen,
    onSecondary = Color.Black,
    background = UvosBg,
    onBackground = IndustrialTextPrimary,
    surface = UvosPanel,
    onSurface = IndustrialTextPrimary,
    surfaceVariant = UvosCard,
    onSurfaceVariant = IndustrialTextSecondary,
    outline = UvosBorder
)

@Composable
fun UVOSDownloaderTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
