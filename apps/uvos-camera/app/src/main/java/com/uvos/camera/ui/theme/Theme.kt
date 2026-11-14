package com.uvos.camera.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = AccentRed,
    onPrimary = TextIvory,
    secondary = ReticleCyan,
    background = BgDark,
    surface = PanelDark,
    onBackground = TextIvory,
    onSurface = TextIvory
)

@Composable
fun UvosCameraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
