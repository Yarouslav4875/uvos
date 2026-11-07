package com.uvos.downloader.ui.theme

import androidx.compose.ui.graphics.Color

val UvosBg = Color(0xFF0E0E12)
val UvosPanel = Color(0xFF1C1E26)
val UvosCard = Color(0xFF242732)
val UvosSurface = Color(0xFF161820)
val UvosBorder = Color(0xFF2D313F)
val UvosBorderActive = Color(0xFF434A61)

// M4200 Analog Gauge Colors
val GaugeDialBg = Color(0xFFE8E4D8)      // Retro Ivory face
val GaugeDialInnerRim = Color(0xFFC7C2B2)
val GaugeDialText = Color(0xFF1A1A1A)
val GaugeNeedleRed = Color(0xFFE02020)
val GaugePivotCap = Color(0xFF141414)
val GaugeRedZone = Color(0xFFE02020)
val GaugeGreenZone = Color(0xFF2ECC71)

// Status Lamps
val LampGreen = Color(0xFF2ECC71)
val LampAmber = Color(0xFFF39C12)
val LampRed = Color(0xFFE74C3C)
val LampBlue = Color(0xFF3498DB)

// Text Colors
val IndustrialTextPrimary = Color(0xFFE2E2E6)
val IndustrialTextSecondary = Color(0xFF8E919E)
val IndustrialTextMuted = Color(0xFF5A5D6B)

// Multi-thread Segment Palette (up to 8 parallel streams)
val SegmentColors = listOf(
    Color(0xFF3498DB), // Thread 1: Electric Blue
    Color(0xFF2ECC71), // Thread 2: Emerald Green
    Color(0xFFF1C40F), // Thread 3: Amber Yellow
    Color(0xFFE67E22), // Thread 4: Industrial Orange
    Color(0xFF9B59B6), // Thread 5: Tactical Purple
    Color(0xFF1ABC9C), // Thread 6: Cyan Teal
    Color(0xFFE91E63), // Thread 7: Signal Magenta
    Color(0xFF00E676)  // Thread 8: Bright Mint
)
