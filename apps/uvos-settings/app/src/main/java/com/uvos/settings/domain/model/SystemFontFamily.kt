package com.uvos.settings.domain.model

enum class SystemFontFamily(
    val fontName: String,
    val displayName: String = fontName,
    val familyCode: String = fontName
) {
    JETBRAINS_MONO("JetBrains Mono (UVOS Default)", "JetBrains Mono", "monospace-jb"),
    ROBOTO_MONO("Roboto Mono", "Roboto Mono", "monospace-roboto"),
    TERMINAL_RETRO("M4200 Industrial Console", "M4200 Console", "terminal-retro")
}

