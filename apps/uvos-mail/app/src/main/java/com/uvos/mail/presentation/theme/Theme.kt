package com.uvos.mail.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val UvosBg = Color(0xFF0E0E12)
val UvosPanel = Color(0xFF1C1E26)
val UvosCard = Color(0xFF242732)
val UvosBorder = Color(0xFF2D313F)

val GaugeDialBg = Color(0xFFE8E4D8)
val GaugeNeedleRed = Color(0xFFE02020)

val LampGreen = Color(0xFF2ECC71)
val LampYellow = Color(0xFFF39C12)
val LampRed = Color(0xFFE74C3C)

private val DarkColorScheme = darkColorScheme(
    primary = GaugeNeedleRed,
    background = UvosBg,
    surface = UvosPanel,
    onPrimary = Color.White
)

@Composable
fun UVOSMailTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
