package com.uvos.notes.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uvos.notes.ui.theme.DialCenterPinColor
import com.uvos.notes.ui.theme.DialFaceColor
import com.uvos.notes.ui.theme.DialNeedleColor
import com.uvos.notes.ui.theme.DialScaleColor
import com.uvos.notes.ui.theme.UVOSAmber
import com.uvos.notes.ui.theme.UVOSPanel
import com.uvos.notes.ui.theme.UVOSTextPrimary
import com.uvos.notes.ui.theme.UVOSTextSecondary
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TextVolumeGauge(
    wordCount: Int,
    maxTargetWords: Int = 2000,
    modifier: Modifier = Modifier
) {
    val progress = (wordCount.toFloat() / maxTargetWords).coerceIn(0f, 1f)
    val targetAngle = -135f + progress * 270f
    val animatedAngle by animateFloatAsState(
        targetValue = targetAngle,
        animationSpec = tween(durationMillis = 350),
        label = "gaugeNeedle"
    )

    Canvas(modifier = modifier.size(120.dp)) {
        val radius = size.minDimension / 2f
        val strokeWidthPx = 2.dp.toPx()

        // 1. Dial Face
        drawCircle(
            color = DialFaceColor,
            radius = radius
        )
        // Vintage outer border
        drawCircle(
            color = Color(0xFF6B6559),
            radius = radius - 1.dp.toPx(),
            style = Stroke(width = strokeWidthPx)
        )

        // 2. Dial Scale Marks
        val tickCount = 9
        for (i in 0 until tickCount) {
            val tickProgress = i.toFloat() / (tickCount - 1)
            val tickAngleDeg = -135f + tickProgress * 270f
            val rad = Math.toRadians((tickAngleDeg - 90).toDouble())

            val isMajor = i % 2 == 0
            val tickLength = if (isMajor) radius * 0.18f else radius * 0.10f
            val outerX = center.x + (radius * 0.88f) * cos(rad).toFloat()
            val outerY = center.y + (radius * 0.88f) * sin(rad).toFloat()
            val innerX = center.x + (radius * 0.88f - tickLength) * cos(rad).toFloat()
            val innerY = center.y + (radius * 0.88f - tickLength) * sin(rad).toFloat()

            val markColor = if (i >= tickCount - 2) Color(0xFFD32F2F) else DialScaleColor
            drawLine(
                color = markColor,
                start = Offset(innerX, innerY),
                end = Offset(outerX, outerY),
                strokeWidth = if (isMajor) 2.dp.toPx() else 1.2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // 3. Dial Center Legend Arc
        drawArc(
            color = Color(0x332E2E2E),
            startAngle = 135f,
            sweepAngle = 270f,
            useCenter = false,
            topLeft = Offset(center.x - radius * 0.65f, center.y - radius * 0.65f),
            size = androidx.compose.ui.geometry.Size(radius * 1.3f, radius * 1.3f),
            style = Stroke(width = 1.dp.toPx())
        )

        // 4. Indicator Needle
        rotate(degrees = animatedAngle, pivot = center) {
            // Needle tail
            drawLine(
                color = DialNeedleColor,
                start = Offset(center.x, center.y + size.height * 0.12f),
                end = Offset(center.x, center.y - size.height * 0.42f),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // 5. Central Pivot Cap
        drawCircle(color = DialCenterPinColor, radius = 5.dp.toPx())
        drawCircle(color = Color(0xFF757575), radius = 2.dp.toPx())
    }
}

@Composable
fun TextVolumeGaugeCard(
    wordCount: Int,
    charCount: Int,
    maxTargetWords: Int = 2000,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(UVOSPanel)
            .border(1.dp, Color(0xFF2E3244), RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TextVolumeGauge(
            wordCount = wordCount,
            maxTargetWords = maxTargetWords,
            modifier = Modifier.size(80.dp)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "M4200 VOLUMETER",
                color = UVOSAmber,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            Text(
                text = "$wordCount / $maxTargetWords слов",
                color = UVOSTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "$charCount символов  •  ~${((wordCount + 199) / 200).coerceAtLeast(1)} мин чт.",
                color = UVOSTextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
