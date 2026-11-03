package com.uvos.terminal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class KeyModifierState {
    OFF,
    LATCHED,
    LOCKED;

    fun next(): KeyModifierState = when (this) {
        OFF -> LATCHED
        LATCHED -> LOCKED
        LOCKED -> OFF
    }

    val isActive: Boolean
        get() = this != OFF
}

@Composable
fun AccessoryBarView(
    ctrlState: KeyModifierState,
    altState: KeyModifierState,
    onCtrlToggle: () -> Unit,
    onAltToggle: () -> Unit,
    onKeyClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(Color(0xFF14161E))
            .border(width = 1.dp, color = Color(0xFF2B2E3D))
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Special Key: ESC
        AccessoryKeyButton(label = "ESC", onClick = { onKeyClick("ESC") })
        // Special Key: TAB
        AccessoryKeyButton(label = "TAB", onClick = { onKeyClick("TAB") })

        // Modifier: CTRL
        ModifierKeyButton(
            label = "CTRL",
            state = ctrlState,
            onClick = onCtrlToggle
        )

        // Modifier: ALT
        ModifierKeyButton(
            label = "ALT",
            state = altState,
            onClick = onAltToggle
        )

        // Quick characters
        AccessoryKeyButton(label = "|", onClick = { onKeyClick("PIPE") })
        AccessoryKeyButton(label = "/", onClick = { onKeyClick("SLASH") })
        AccessoryKeyButton(label = "-", onClick = { onKeyClick("DASH") })
        AccessoryKeyButton(label = "_", onClick = { onKeyClick("UNDERSCORE") })
        AccessoryKeyButton(label = "~", onClick = { onKeyClick("TILDE") })

        // Arrow keys
        AccessoryKeyButton(label = "↑", onClick = { onKeyClick("UP") })
        AccessoryKeyButton(label = "↓", onClick = { onKeyClick("DOWN") })
        AccessoryKeyButton(label = "←", onClick = { onKeyClick("LEFT") })
        AccessoryKeyButton(label = "→", onClick = { onKeyClick("RIGHT") })
    }
}

@Composable
private fun AccessoryKeyButton(
    label: String,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .widthIn(min = 34.dp)
            .height(34.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF1E212B))
            .border(1.dp, Color(0xFF383C4F), RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp)
    ) {
        Text(
            text = label,
            color = Color(0xFFDCDFEA),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun ModifierKeyButton(
    label: String,
    state: KeyModifierState,
    onClick: () -> Unit
) {
    val (bgColor, borderColor, textColor) = when (state) {
        KeyModifierState.OFF -> Triple(Color(0xFF1E212B), Color(0xFF383C4F), Color(0xFFA0A5B5))
        KeyModifierState.LATCHED -> Triple(Color(0xFF3B2B00), Color(0xFFFFB000), Color(0xFFFFB000))
        KeyModifierState.LOCKED -> Triple(Color(0xFF450A0A), Color(0xFFE02020), Color(0xFFFF4D4D))
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .widthIn(min = 44.dp)
            .height(34.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
