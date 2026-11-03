package com.uvos.terminal.core

import androidx.compose.ui.graphics.Color

enum class ColorProfile(val title: String) {
    CRIMSON("UVOS Crimson"),
    AMBER("Amber CRT (P3)"),
    GREEN("Green CRT (P1)")
}

data class TerminalColorScheme(
    val profile: ColorProfile,
    val background: Color,
    val text: Color,
    val textBright: Color,
    val cursor: Color,
    val gaugeDial: Color,
    val pointer: Color,
    val bezel: Color,
    val scanlineColor: Color,
    val glowColor: Color,
    val ansiPalette: List<Color>
)

object TerminalColors {
    // Vintage Ivory dial & bezel
    val GaugeIvory = Color(0xFFE8E4D8)
    val IndustrialBezel = Color(0xFF1C1E26)
    val PointerCrimson = Color(0xFFE02020)

    val CrimsonScheme = TerminalColorScheme(
        profile = ColorProfile.CRIMSON,
        background = Color(0xFF0E0E12),
        text = Color(0xFFE02020),
        textBright = Color(0xFFFF6B6B),
        cursor = Color(0xFFFF2424),
        gaugeDial = GaugeIvory,
        pointer = PointerCrimson,
        bezel = IndustrialBezel,
        scanlineColor = Color(0x22000000),
        glowColor = Color(0x33E02020),
        ansiPalette = generateAnsiPalette(Color(0xFFE02020), Color(0xFFFF6B6B))
    )

    val AmberScheme = TerminalColorScheme(
        profile = ColorProfile.AMBER,
        background = Color(0xFF0B0905),
        text = Color(0xFFFFB000),
        textBright = Color(0xFFFFD266),
        cursor = Color(0xFFFFAA00),
        gaugeDial = GaugeIvory,
        pointer = PointerCrimson,
        bezel = IndustrialBezel,
        scanlineColor = Color(0x22000000),
        glowColor = Color(0x33FFB000),
        ansiPalette = generateAnsiPalette(Color(0xFFFFB000), Color(0xFFFFD266))
    )

    val GreenScheme = TerminalColorScheme(
        profile = ColorProfile.GREEN,
        background = Color(0xFF050B06),
        text = Color(0xFF33FF33),
        textBright = Color(0xFF80FF80),
        cursor = Color(0xFF00FF00),
        gaugeDial = GaugeIvory,
        pointer = PointerCrimson,
        bezel = IndustrialBezel,
        scanlineColor = Color(0x22000000),
        glowColor = Color(0x3333FF33),
        ansiPalette = generateAnsiPalette(Color(0xFF33FF33), Color(0xFF80FF80))
    )

    private fun generateAnsiPalette(base: Color, bright: Color): List<Color> {
        return listOf(
            Color(0xFF121212), // Black
            Color(0xFFCC2222), // Red
            Color(0xFF22CC22), // Green
            Color(0xFFCCCC22), // Yellow
            Color(0xFF3366EE), // Blue
            Color(0xFFCC22CC), // Magenta
            Color(0xFF22CCCC), // Cyan
            Color(0xFFCCCCCC), // White
            Color(0xFF555555), // Bright Black
            Color(0xFFFF4444), // Bright Red
            Color(0xFF44FF44), // Bright Green
            Color(0xFFFFFF44), // Bright Yellow
            Color(0xFF5588FF), // Bright Blue
            Color(0xFFFF55FF), // Bright Magenta
            Color(0xFF55FFFF), // Bright Cyan
            Color(0xFFFFFFFF)  // Bright White
        )
    }

    fun getScheme(profile: ColorProfile): TerminalColorScheme {
        return when (profile) {
            ColorProfile.CRIMSON -> CrimsonScheme
            ColorProfile.AMBER -> AmberScheme
            ColorProfile.GREEN -> GreenScheme
        }
    }
}
