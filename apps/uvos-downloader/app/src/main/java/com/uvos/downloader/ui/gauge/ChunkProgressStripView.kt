package com.uvos.downloader.ui.gauge

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uvos.downloader.domain.model.DownloadChunk
import com.uvos.downloader.ui.theme.IndustrialTextMuted
import com.uvos.downloader.ui.theme.IndustrialTextPrimary
import com.uvos.downloader.ui.theme.IndustrialTextSecondary
import com.uvos.downloader.ui.theme.LampGreen
import com.uvos.downloader.ui.theme.SegmentColors
import com.uvos.downloader.ui.theme.UvosBorder
import com.uvos.downloader.ui.theme.UvosSurface
import java.util.Locale

@Composable
fun ChunkProgressStripView(
    chunks: List<DownloadChunk>,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    if (chunks.isEmpty()) return

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(UvosSurface, RoundedCornerShape(8.dp))
            .border(1.dp, UvosBorder, RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "СЕГМЕНТЫ (ПОТОКОВ: ${chunks.size})",
                color = IndustrialTextSecondary,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            val completedCount = chunks.count { it.isCompleted }
            Text(
                text = "$completedCount/${chunks.size} ГОТОВО",
                color = if (completedCount == chunks.size) LampGreen else IndustrialTextMuted,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Segmented Strip Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF101217))
                .border(0.5.dp, UvosBorder, RoundedCornerShape(4.dp)),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            chunks.forEachIndexed { index, chunk ->
                val color = SegmentColors[index % SegmentColors.size]
                val weight = if (chunk.totalBytes > 0) chunk.totalBytes.toFloat() else 1f

                Box(
                    modifier = Modifier
                        .weight(weight.coerceAtLeast(0.01f))
                        .fillMaxHeight()
                        .background(Color(0xFF1E212B))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(chunk.progress)
                            .background(
                                color.copy(
                                    alpha = if (isActive && !chunk.isCompleted) pulseAlpha else 1f
                                )
                            )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Thread Details Mini Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            chunks.take(8).forEachIndexed { index, chunk ->
                val color = SegmentColors[index % SegmentColors.size]
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFF12141B), RoundedCornerShape(4.dp))
                        .padding(vertical = 4.dp, horizontal = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(color, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "T${index + 1}",
                            color = IndustrialTextPrimary,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = String.format(Locale.US, "%.0f%%", chunk.progress * 100),
                        color = if (chunk.isCompleted) LampGreen else IndustrialTextSecondary,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
