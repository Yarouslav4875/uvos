package com.uvos.downloader.ui.gauge

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uvos.downloader.domain.model.NetworkSpeedMetric
import com.uvos.downloader.ui.theme.GaugeDialBg
import com.uvos.downloader.ui.theme.GaugeDialInnerRim
import com.uvos.downloader.ui.theme.GaugeDialText
import com.uvos.downloader.ui.theme.GaugeNeedleRed
import com.uvos.downloader.ui.theme.GaugePivotCap
import com.uvos.downloader.ui.theme.GaugeRedZone
import com.uvos.downloader.ui.theme.IndustrialTextMuted
import com.uvos.downloader.ui.theme.IndustrialTextPrimary
import com.uvos.downloader.ui.theme.IndustrialTextSecondary
import com.uvos.downloader.ui.theme.LampAmber
import com.uvos.downloader.ui.theme.LampGreen
import com.uvos.downloader.ui.theme.UvosBorder
import com.uvos.downloader.ui.theme.UvosCard
import com.uvos.downloader.ui.theme.UvosPanel
import kotlin.math.cos
import kotlin.math.sin

private const val MAX_SPEED_MB = 50.0f
private const val START_ANGLE = 135f // Bottom-left
private const val SWEEP_ANGLE = 270f // Full sweep to bottom-right

@Composable
fun SpeedometerGauge(
    speedMetric: NetworkSpeedMetric,
    modifier: Modifier = Modifier
) {
    val speedMb = speedMetric.speedMb.toFloat().coerceIn(0f, MAX_SPEED_MB)
    val animatedNeedle = remember { Animatable(0f) }

    LaunchedEffect(speedMb) {
        animatedNeedle.animateTo(
            targetValue = speedMb,
            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(UvosPanel, RoundedCornerShape(16.dp))
            .border(1.dp, UvosBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Meter Header Inscription
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "СКОРОСТЬ ПЕРЕДАЧИ ДАННЫХ",
                color = IndustrialTextPrimary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            if (speedMetric.activeConnections > 0) LampGreen else LampAmber,
                            CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (speedMetric.activeConnections > 0) "ПОТОК АКТИВЕН" else "ОЖИДАНИЕ",
                    color = IndustrialTextSecondary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Analog M4200 Dial
        Canvas(modifier = Modifier.size(220.dp)) {
            drawM4200Dial(animatedNeedle.value)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Inset Digital Readout Box
        Row(
            modifier = Modifier
                .background(UvosCard, RoundedCornerShape(8.dp))
                .border(1.dp, UvosBorder, RoundedCornerShape(8.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = speedMetric.formattedSpeed(),
                    color = if (speedMb >= 40f) GaugeNeedleRed else LampGreen,
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "ПИК: ${speedMetric.formattedPeak()} | ПОТОКОВ: ${speedMetric.activeConnections}",
                    color = IndustrialTextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

private fun DrawScope.drawM4200Dial(currentSpeedMb: Float) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val radius = size.width * 0.46f

    // 1. Dial Outer Bezel Ring (Industrial Gunmetal)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF2E313D), Color(0xFF14151B)),
            center = Offset(cx, cy),
            radius = radius + 6f
        ),
        radius = radius + 6f
    )

    // 2. Bezel Border
    drawCircle(
        color = Color(0xFF3F4456),
        radius = radius + 2f,
        style = Stroke(width = 3f)
    )

    // 3. Dial Face Plate (Retro Ivory Paper texture)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(GaugeDialBg, GaugeDialInnerRim),
            center = Offset(cx, cy),
            radius = radius
        ),
        radius = radius
    )

    // 4. Critical Warning Red Zone Arc (> 40 MB/s to 50 MB/s)
    val arcInset = radius * 0.20f
    val arcRadius = radius - arcInset
    val redZoneStart = START_ANGLE + (40f / MAX_SPEED_MB) * SWEEP_ANGLE
    val redZoneSweep = (10f / MAX_SPEED_MB) * SWEEP_ANGLE

    drawArc(
        color = GaugeRedZone,
        startAngle = redZoneStart,
        sweepAngle = redZoneSweep,
        useCenter = false,
        topLeft = Offset(cx - arcRadius, cy - arcRadius),
        size = Size(arcRadius * 2, arcRadius * 2),
        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
    )

    // Safe / Nominal Arc (0 to 40 MB/s)
    val greenZoneSweep = (40f / MAX_SPEED_MB) * SWEEP_ANGLE
    drawArc(
        color = Color(0xFF4A4E4D),
        startAngle = START_ANGLE,
        sweepAngle = greenZoneSweep,
        useCenter = false,
        topLeft = Offset(cx - arcRadius, cy - arcRadius),
        size = Size(arcRadius * 2, arcRadius * 2),
        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
    )

    // 5. Scale Graduations and Numerals
    val textPaint = Paint().apply {
        color = android.graphics.Color.parseColor("#1A1A1A")
        textSize = 10.dp.toPx()
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    val inscriptionPaint = Paint().apply {
        color = android.graphics.Color.parseColor("#444444")
        textSize = 7.dp.toPx()
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    val totalTicks = 25 // Every 2 MB/s
    for (i in 0..totalTicks) {
        val frac = i.toFloat() / totalTicks
        val angleDeg = START_ANGLE + frac * SWEEP_ANGLE
        val angleRad = Math.toRadians(angleDeg.toDouble())

        val isMajor = i % 5 == 0 // 0, 10, 20, 30, 40, 50
        val tickLen = if (isMajor) 14.dp.toPx() else 7.dp.toPx()
        val tickWidth = if (isMajor) 2.5f else 1.2f

        val outerR = radius - 8.dp.toPx()
        val innerR = outerR - tickLen

        val startX = cx + innerR * cos(angleRad).toFloat()
        val startY = cy + innerR * sin(angleRad).toFloat()
        val endX = cx + outerR * cos(angleRad).toFloat()
        val endY = cy + outerR * sin(angleRad).toFloat()

        val isRedZone = (frac * MAX_SPEED_MB) >= 40f
        drawLine(
            color = if (isRedZone) GaugeRedZone else GaugeDialText,
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = tickWidth,
            cap = StrokeCap.Round
        )

        // Draw numbers on major ticks
        if (isMajor) {
            val numValue = (frac * MAX_SPEED_MB).toInt()
            val textR = innerR - 9.dp.toPx()
            val textX = cx + textR * cos(angleRad).toFloat()
            val textY = cy + textR * sin(angleRad).toFloat() + 3.dp.toPx()

            drawContext.canvas.nativeCanvas.drawText(
                numValue.toString(),
                textX,
                textY,
                textPaint
            )
        }
    }

    // Inscriptions on the ivory dial
    drawContext.canvas.nativeCanvas.drawText("M4200", cx, cy - radius * 0.32f, textPaint)
    drawContext.canvas.nativeCanvas.drawText("МБ/с  MB/s", cx, cy - radius * 0.18f, inscriptionPaint)
    drawContext.canvas.nativeCanvas.drawText("UVOS КЛ. 1.5", cx, cy + radius * 0.38f, inscriptionPaint)

    // 6. Gauge Needle
    val needleAngle = (START_ANGLE + (currentSpeedMb / MAX_SPEED_MB) * SWEEP_ANGLE) + 90f

    rotate(degrees = needleAngle, pivot = Offset(cx, cy)) {
        // Red Pointer Needle
        val needleLength = radius * 0.85f
        val counterBalance = radius * 0.22f

        // Tail
        drawLine(
            color = Color(0xFF222222),
            start = Offset(cx, cy),
            end = Offset(cx, cy + counterBalance),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Pointed needle blade
        drawLine(
            color = GaugeNeedleRed,
            start = Offset(cx, cy),
            end = Offset(cx, cy - needleLength),
            strokeWidth = 2.5.dp.toPx(),
            cap = StrokeCap.Round
        )
    }

    // 7. Center Hub Screw Cap
    drawCircle(
        color = GaugePivotCap,
        radius = 7.dp.toPx(),
        center = Offset(cx, cy)
    )
    drawCircle(
        color = Color(0xFF555555),
        radius = 3.dp.toPx(),
        center = Offset(cx, cy)
    )
}
