package com.uvos.terminal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StatusLampsView(
    isRoot: Boolean,
    isPtyActive: Boolean,
    isRecording: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF14161E))
            .border(1.dp, Color(0xFF2B2E3D), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(
            text = "SUBSYSTEM LAMPS",
            color = Color(0xFF7E849A),
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LampIndicator(
                label = "ROOT",
                isOn = true,
                activeColor = if (isRoot) Color(0xFF22DD22) else Color(0xFFDD2222),
                labelColor = if (isRoot) Color(0xFF88FF88) else Color(0xFFFF8888)
            )

            LampIndicator(
                label = "PTY",
                isOn = isPtyActive,
                activeColor = Color(0xFF22DD22),
                labelColor = if (isPtyActive) Color(0xFF88FF88) else Color(0xFF555566)
            )

            LampIndicator(
                label = "REC",
                isOn = isRecording,
                activeColor = Color(0xFFFFB000),
                labelColor = if (isRecording) Color(0xFFFFD266) else Color(0xFF555566)
            )
        }
    }
}

@Composable
private fun LampIndicator(
    label: String,
    isOn: Boolean,
    activeColor: Color,
    labelColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(if (isOn) activeColor else Color(0xFF2E3240))
                .border(1.dp, Color(0xFF0E0E12), CircleShape)
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = label,
            color = labelColor,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
