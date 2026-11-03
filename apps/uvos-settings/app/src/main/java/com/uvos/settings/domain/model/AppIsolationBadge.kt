package com.uvos.settings.domain.model

enum class AppIsolationBadge(val label: String, val colorHex: Long) {
    SYSTEM("СИСТЕМА", 0xFF4C5061),
    ISOLATED("ИЗОЛИРОВАНО", 0xFF00FF66),
    SINGLE_CHANNEL_NET("1 КАНАЛ СЕТИ", 0xFFFFB000),
    EXPOSED("ОТКРЫТО", 0xFFE02020)
}
