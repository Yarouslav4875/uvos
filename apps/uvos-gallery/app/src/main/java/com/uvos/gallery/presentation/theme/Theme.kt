package com.uvos.gallery.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val UvosDarkColorScheme = darkColorScheme(
    primary = UvosGreenSterile,
    onPrimary = UvosBackground,
    primaryContainer = UvosTileSurfaceVariant,
    onPrimaryContainer = UvosDialIvory,
    secondary = UvosBlueEncrypted,
    onSecondary = UvosDialIvory,
    background = UvosBackground,
    onBackground = UvosDialIvory,
    surface = UvosTileSurface,
    onSurface = UvosDialIvory,
    surfaceVariant = UvosTileSurfaceVariant,
    onSurfaceVariant = UvosMuted,
    error = UvosRedDanger,
    onError = UvosDialIvory
)

@Composable
fun UVOSGalleryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // UnderVoltOS always adheres to industrial dark AMOLED theme (#0E0E12)
    MaterialTheme(
        colorScheme = UvosDarkColorScheme,
        typography = M4200Typography,
        content = content
    )
}
