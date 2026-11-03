package com.uvos.settings.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import com.uvos.settings.presentation.theme.*

@Composable
fun CpuFrequencyCluster(
    coreFrequenciesMhz: List<Int>,
    cpuLoadPercent: Int,
    modifier: Modifier = Modifier
) {
    InstrumentTile(
        title = "ТАКТОВЫЕ ЧАСТОТЫ ЯДЕР (SM6225)",
        modifier = modifier
    ) {
        // Total CPU Load Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "НАГРУЗКА ЦПУ",
                color = UvosTextSecondary,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "$cpuLoadPercent %",
                color = if (cpuLoadPercent > 80) UvosRed else UvosTextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Segmented bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF0F1015))
                .border(1.dp, UvosTileBorder, RoundedCornerShape(4.dp))
        ) {
            val fraction = (cpuLoadPercent / 100f).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .background(if (cpuLoadPercent > 80) UvosRed else UvosAmberLamp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 8 CPU cores breakdown
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val paddedFrequencies = if (coreFrequenciesMhz.size >= 8) {
                coreFrequenciesMhz.take(8)
            } else {
                coreFrequenciesMhz + List(8 - coreFrequenciesMhz.size) { 0 }
            }

            paddedFrequencies.forEachIndexed { index, mhz ->
                val isGold = index >= 4
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "C$index",
                        color = if (isGold) UvosAmberLamp else Color(0xFF8E93A6),
                        fontSize = 9.sp,
                        fontWeight = if (isGold) FontWeight.Bold else FontWeight.Normal,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$mhz",
                        color = UvosTextPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = if (isGold) "A73" else "A53",
                        color = UvosTextSecondary,
                        fontSize = 7.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
