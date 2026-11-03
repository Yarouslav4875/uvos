package com.uvos.music.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.uvos.music.core.ui.theme.M4200AccentCyan
import com.uvos.music.core.ui.theme.M4200ChassisBezel
import com.uvos.music.core.ui.theme.M4200ChassisBorder
import com.uvos.music.core.ui.theme.M4200ChassisCard
import com.uvos.music.core.ui.theme.M4200OverloadRed
import com.uvos.music.core.ui.theme.M4200TextDim
import com.uvos.music.core.ui.theme.M4200TextPrimary
import com.uvos.music.core.ui.theme.M4200TextSecondary
import com.uvos.music.domain.model.StereoVuLevels

/**
 * Studio Rack Mount Stereo VU Panel featuring dual M4200 electromechanical gauges.
 */
@Composable
fun StereoVuPanel(
    levels: StereoVuLevels,
    padDb: Float = 0.0f,
    onTogglePad: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(M4200ChassisCard)
            .border(1.5.dp, M4200ChassisBorder, RoundedCornerShape(14.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Rack Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "M4200 STEREO BALLISTIC VU MONITOR",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = M4200TextSecondary,
                letterSpacing = 1.sp
            )

            // VU PAD Button (0 dB / +6 dB)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (padDb > 0f) M4200OverloadRed.copy(alpha = 0.25f) else M4200ChassisBezel)
                    .border(
                        1.dp,
                        if (padDb > 0f) M4200OverloadRed else M4200ChassisBorder,
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = if (padDb > 0f) "PAD: +6 dB" else "PAD: 0 dB",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (padDb > 0f) M4200OverloadRed else M4200AccentCyan
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Dual M4200 Gauges Side-by-Side
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Left Channel Gauge
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                GaugeView(
                    normalizedLevel = levels.leftNormalized,
                    isClipping = levels.isLeftClipping,
                    channelLabel = "LEFT CH"
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${"%.1f".format(levels.leftRmsDb)} dBFS",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = if (levels.isLeftClipping) M4200OverloadRed else M4200TextDim
                )
            }

            // Right Channel Gauge
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                GaugeView(
                    normalizedLevel = levels.rightNormalized,
                    isClipping = levels.isRightClipping,
                    channelLabel = "RIGHT CH"
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${"%.1f".format(levels.rightRmsDb)} dBFS",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = if (levels.isRightClipping) M4200OverloadRed else M4200TextDim
                )
            }
        }
    }
}
