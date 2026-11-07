package com.uvos.vpn.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uvos.vpn.presentation.theme.GaugeDialBg
import com.uvos.vpn.presentation.theme.GaugeDialText
import com.uvos.vpn.presentation.theme.GaugeNeedleRed
import com.uvos.vpn.presentation.theme.UvosPanel
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SpeedometerGaugeView(
    title: String,
    speedKbSec: Float,
    maxSpeedKb: Float = 50000f, // 50 MB/s scale
    modifier: Modifier = Modifier
) {
    val progress = (speedKbSec / maxSpeedKb).coerceIn(0f, 1f)
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(progress) {
        animatedProgress.animateTo(
            targetValue = progress,
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
        )
    }

    Box(
        modifier = modifier
            .background(UvosPanel, RoundedCornerShape(16.dp))
            .padding(10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Canvas(modifier = Modifier.weight(1f).fillMaxSize()) {
                drawGaugeDial(animatedProgress.value)
            }

            Text(
                text = String.format("%.1f КБ/с", speedKbSec),
                color = if (speedKbSec > 0) GaugeNeedleRed else Color.Gray,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun DrawScope.drawGaugeDial(progress: Float) {
    val cx = size.width / 2f
    val cy = size.height * 0.85f
    val radius = size.width * 0.44f

    drawArc(
        color = GaugeDialBg,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(cx - radius, cy - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = 14f, cap = StrokeCap.Round)
    )

    drawArc(
        color = GaugeNeedleRed,
        startAngle = 180f + 180f * 0.80f,
        sweepAngle = 180f * 0.20f,
        useCenter = false,
        topLeft = Offset(cx - radius, cy - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = 14f, cap = StrokeCap.Round)
    )

    for (i in 0..10) {
        val angleDeg = 180f + i * 18f
        val angleRad = Math.toRadians(angleDeg.toDouble())
        val outerX = cx + (radius + 2f) * cos(angleRad).toFloat()
        val outerY = cy + (radius + 2f) * sin(angleRad).toFloat()
        val innerLen = if (i % 5 == 0) 16f else 8f
        val innerX = cx + (radius - innerLen) * cos(angleRad).toFloat()
        val innerY = cy + (radius - innerLen) * sin(angleRad).toFloat()

        drawLine(
            color = if (i >= 8) GaugeNeedleRed else GaugeDialText,
            start = Offset(innerX, innerY),
            end = Offset(outerX, outerY),
            strokeWidth = if (i % 5 == 0) 4f else 2f
        )
    }

    val needleAngleDeg = 180f + progress * 180f
    val needleAngleRad = Math.toRadians(needleAngleDeg.toDouble())
    val needleLen = radius * 0.88f
    val tipX = cx + needleLen * cos(needleAngleRad).toFloat()
    val tipY = cy + needleLen * sin(needleAngleRad).toFloat()

    drawLine(
        color = GaugeNeedleRed,
        start = Offset(cx, cy),
        end = Offset(tipX, tipY),
        strokeWidth = 5f,
        cap = StrokeCap.Round
    )

    drawCircle(
        color = Color(0xFF1A1A1A),
        radius = 12f,
        center = Offset(cx, cy)
    )
    drawCircle(
        color = GaugeNeedleRed,
        radius = 5f,
        center = Offset(cx, cy)
    )
}
