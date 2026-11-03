package com.uvos.files.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.uvos.files.ui.theme.LampGreen
import com.uvos.files.ui.theme.LampOff
import com.uvos.files.ui.theme.LampRed
import com.uvos.files.ui.theme.LampYellow

enum class LampStatus { OFF, NORMAL, WARNING, CRITICAL }

@Composable
fun LampIndicator(status: LampStatus, modifier: Modifier = Modifier) {
    val color = when (status) {
        LampStatus.OFF -> LampOff
        LampStatus.NORMAL -> LampGreen
        LampStatus.WARNING -> LampYellow
        LampStatus.CRITICAL -> LampRed
    }
    Box(
        modifier = modifier
            .size(9.dp)
            .border(1.dp, Color(0xFF111111), CircleShape)
            .background(color, CircleShape)
    )
}
