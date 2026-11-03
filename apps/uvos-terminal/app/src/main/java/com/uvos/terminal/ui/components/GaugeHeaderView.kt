package com.uvos.terminal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import com.uvos.terminal.core.ColorProfile
import com.uvos.terminal.monitor.TerminalSystemMetrics
import java.util.Locale

@Composable
fun GaugeHeaderView(
    metrics: TerminalSystemMetrics,
    currentProfile: ColorProfile,
    onCycleProfile: () -> Unit,
    onToggleRecording: () -> Unit,
    onOpenMacros: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(132.dp)
            .background(Color(0xFF14161E))
            .border(width = 1.dp, color = Color(0xFF2C2F3D))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Left: CPU Analog Gauge
        AnalogGaugeView(
            currentValue = metrics.cpuFrequencyGhz,
            minValue = 0.5f,
            maxValue = 2.8f,
            unitText = "GHz",
            titleText = "CPU FREQ",
            valueDisplayText = String.format(Locale.US, "%.2f GHz", metrics.cpuFrequencyGhz),
            size = 96.dp
        )

        // 2. Center: Status Lamps & Control Switches
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            StatusLampsView(
                isRoot = metrics.isRoot,
                isPtyActive = metrics.isPtyActive,
                isRecording = metrics.isRecording
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile selector pill
                HeaderActionButton(
                    text = when (currentProfile) {
                        ColorProfile.CRIMSON -> "CRT: CRMS"
                        ColorProfile.AMBER -> "CRT: AMBR"
                        ColorProfile.GREEN -> "CRT: GREN"
                    },
                    color = when (currentProfile) {
                        ColorProfile.CRIMSON -> Color(0xFFE02020)
                        ColorProfile.AMBER -> Color(0xFFFFB000)
                        ColorProfile.GREEN -> Color(0xFF33FF33)
                    },
                    onClick = onCycleProfile
                )

                // Macros sheet button
                HeaderActionButton(
                    text = "MACROS",
                    color = Color(0xFFFFB000),
                    onClick = onOpenMacros
                )

                // Rec button
                HeaderActionButton(
                    text = if (metrics.isRecording) "REC: ON" else "REC: OFF",
                    color = if (metrics.isRecording) Color(0xFFFF2424) else Color(0xFF7E849A),
                    onClick = onToggleRecording
                )
            }
        }

        // 3. Right: RAM Analog Gauge
        val maxRam = if (metrics.ramTotalGb > 0f) metrics.ramTotalGb else 8.0f
        AnalogGaugeView(
            currentValue = metrics.ramUsedGb,
            minValue = 0f,
            maxValue = maxRam,
            unitText = "GB",
            titleText = "RAM USAGE",
            valueDisplayText = String.format(Locale.US, "%.1f / %.1f GB", metrics.ramUsedGb, maxRam),
            size = 96.dp
        )
    }
}

@Composable
private fun HeaderActionButton(
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF1E212B))
            .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
