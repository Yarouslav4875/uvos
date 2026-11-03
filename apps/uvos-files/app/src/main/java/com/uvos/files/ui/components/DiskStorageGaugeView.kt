package com.uvos.files.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.uvos.files.ui.theme.GaugeDialBg
import com.uvos.files.ui.theme.GaugeDialText
import com.uvos.files.ui.theme.GaugeNeedleRed
import com.uvos.files.ui.theme.UvosPanel
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun DiskStorageGaugeView(
    title: String,
    usedPercentage: Float, // 0.0f..1.0f
    freeSpaceText: String,
    totalSpaceText: String,
    isMounted: Boolean,
    modifier: Modifier = Modifier
) {
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(usedPercentage, isMounted) {
        val target = if (isMounted) usedPercentage.coerceIn(0f, 1f) else 0f
        animatedProgress.animateTo(
            targetValue = target,
            animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing)
        )
    }

    Column(
        modifier = modifier
            .background(UvosPanel, RoundedCornerShape(16.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title and Lamp Status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title.uppercase(),
                color = Color.LightGray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
            )
            LampIndicator(
                status = when {
                    !isMounted -> LampStatus.OFF
                    usedPercentage > 0.95f -> LampStatus.CRITICAL
                    usedPercentage > 0.80f -> LampStatus.WARNING
                    else -> LampStatus.NORMAL
                }
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Analog Dial M4200
        Box(
            modifier = Modifier
                .size(width = 100.dp, height = 65.dp)
                .background(GaugeDialBg, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawM4200Dial(animatedProgress.value, isMounted)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Text metric in monospace
        Text(
            text = if (isMounted) "$freeSpaceText free" else "OFFLINE",
            color = if (isMounted) Color.White else Color.Gray,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
        Text(
            text = if (isMounted) "of $totalSpaceText" else "---",
            color = Color.DarkGray,
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 1
        )
    }
}

private fun DrawScope.drawM4200Dial(progress: Float, isMounted: Boolean) {
    val w = size.width
    val h = size.height
    val pivot = Offset(w / 2f, h * 0.95f)
    val radius = w * 0.42f

    val startAngle = 180f + 25f
    val sweepAngle = 130f

    // Dial background arc
    drawArc(
        color = Color(0xFFD0CCC0),
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = Offset(pivot.x - radius, pivot.y - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
    )

    // Critical zone (red sector last 15%)
    drawArc(
        color = GaugeNeedleRed,
        startAngle = startAngle + sweepAngle * 0.85f,
        sweepAngle = sweepAngle * 0.15f,
        useCenter = false,
        topLeft = Offset(pivot.x - radius, pivot.y - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
    )

    // Ticks
    val tickCount = 10
    for (i in 0..tickCount) {
        val angleDeg = startAngle + (sweepAngle / tickCount) * i
        val angleRad = Math.toRadians(angleDeg.toDouble())
        val isMajor = i % 5 == 0
        val tickLen = if (isMajor) 6.dp.toPx() else 3.5.dp.toPx()

        val p1 = Offset(
            (pivot.x + (radius - 2.dp.toPx()) * cos(angleRad)).toFloat(),
            (pivot.y + (radius - 2.dp.toPx()) * sin(angleRad)).toFloat()
        )
        val p2 = Offset(
            (pivot.x + (radius - 2.dp.toPx() - tickLen) * cos(angleRad)).toFloat(),
            (pivot.y + (radius - 2.dp.toPx() - tickLen) * sin(angleRad)).toFloat()
        )
        drawLine(
            color = GaugeDialText,
            start = p1,
            end = p2,
            strokeWidth = if (isMajor) 1.5.dp.toPx() else 1.dp.toPx()
        )
    }

    // Needle
    val currentAngleDeg = startAngle + (sweepAngle * progress)
    val currentAngleRad = Math.toRadians(currentAngleDeg.toDouble())
    val needleLength = radius * 0.90f

    val needleTip = Offset(
        (pivot.x + needleLength * cos(currentAngleRad)).toFloat(),
        (pivot.y + needleLength * sin(currentAngleRad)).toFloat()
    )

    // Needle shadow
    drawLine(
        color = Color(0x33000000),
        start = Offset(pivot.x + 1f, pivot.y + 1f),
        end = Offset(needleTip.x + 1f, needleTip.y + 1f),
        strokeWidth = 2.dp.toPx(),
        cap = StrokeCap.Round
    )

    // Needle body
    drawLine(
        color = if (isMounted) GaugeNeedleRed else Color.Gray,
        start = pivot,
        end = needleTip,
        strokeWidth = 2.dp.toPx(),
        cap = StrokeCap.Round
    )

    // Axis cap
    drawCircle(
        color = Color(0xFF111111),
        radius = 4.dp.toPx(),
        center = pivot
    )
}
