package com.uvos.settings.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uvos.settings.presentation.theme.*
import kotlin.math.cos
import kotlin.math.sin

/**
 * Стрелочный индикатор базируется на дизайне милливольтметра/амперметра М4200.
 * Эмуляция физической инерционности магнитоэлектрической системы (spring damping 0.65).
 */
@Composable
fun M4200DialGauge(
    value: Float,
    minValue: Float,
    maxValue: Float,
    unitTitle: String,
    deviceLabel: String,
    modifier: Modifier = Modifier,
    dangerThreshold: Float? = null
) {
    val clampedValue = value.coerceIn(minValue, maxValue)
    val fraction = if (maxValue > minValue) (clampedValue - minValue) / (maxValue - minValue) else 0f
    val targetAngle = -45f + fraction * 90f

    // Эмуляция баллистики стрелки стрелочного механизма
    val animatedAngle by animateFloatAsState(
        targetValue = targetAngle,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 120f),
        label = "M4200NeedleSpring"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(UvosTile)
            .border(1.5.dp, UvosTileBorder, RoundedCornerShape(16.dp))
            .padding(8.dp)
            .height(136.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val pivot = Offset(w / 2f, h * 0.98f)
            val dialRadius = w * 0.72f

            // Подложка шкалы - цвет состаренной слоновой кости
            drawArc(
                color = UvosDial,
                startAngle = 215f,
                sweepAngle = 110f,
                useCenter = true,
                topLeft = Offset(pivot.x - dialRadius, pivot.y - dialRadius),
                size = Size(dialRadius * 2, dialRadius * 2)
            )

            // Основная дуговая линия шкалы
            drawArc(
                color = UvosDialText,
                startAngle = 225f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(pivot.x - dialRadius * 0.85f, pivot.y - dialRadius * 0.85f),
                size = Size(dialRadius * 1.7f, dialRadius * 1.7f),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )

            // Засечки шкалы (21 шаг)
            val totalTicks = 20
            for (i in 0..totalTicks) {
                val tickFraction = i.toFloat() / totalTicks
                val tickAngleDeg = 225f + tickFraction * 90f
                val tickAngleRad = Math.toRadians(tickAngleDeg.toDouble())
                val isMajor = i % 5 == 0
                val tickLen = if (isMajor) 9.dp.toPx() else 4.5.dp.toPx()

                val rOuter = dialRadius * 0.85f
                val rInner = rOuter - tickLen

                val currentTickVal = minValue + tickFraction * (maxValue - minValue)
                val isDanger = dangerThreshold != null && currentTickVal >= dangerThreshold

                val pOuter = Offset(
                    (pivot.x + rOuter * cos(tickAngleRad)).toFloat(),
                    (pivot.y + rOuter * sin(tickAngleRad)).toFloat()
                )
                val pInner = Offset(
                    (pivot.x + rInner * cos(tickAngleRad)).toFloat(),
                    (pivot.y + rInner * sin(tickAngleRad)).toFloat()
                )

                drawLine(
                    color = if (isDanger) UvosRed else UvosDialText,
                    start = pInner,
                    end = pOuter,
                    strokeWidth = if (isMajor) 1.8.dp.toPx() else 1.dp.toPx()
                )
            }

            // Стрелка с тенью
            rotate(degrees = animatedAngle, pivot = pivot) {
                // Мягкая тень стрелки
                drawLine(
                    color = Color(0x33000000),
                    start = Offset(pivot.x + 2.5f, pivot.y + 2.5f),
                    end = Offset(pivot.x + 2.5f, pivot.y - dialRadius * 0.88f + 2.5f),
                    strokeWidth = 2.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                // Алая стрелка индикатора М4200
                drawLine(
                    color = UvosRed,
                    start = pivot,
                    end = Offset(pivot.x, pivot.y - dialRadius * 0.88f),
                    strokeWidth = 1.8.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Центральный латунный болт поворотной рамки
            drawCircle(
                color = UvosBrassPivot,
                radius = 6.dp.toPx(),
                center = pivot
            )
            drawCircle(
                color = UvosBrassInner,
                radius = 2.5.dp.toPx(),
                center = pivot
            )
        }

        // Лейбл прибора в стиле заводской маркировки М4200
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = deviceLabel,
                color = Color(0xFF33353D),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            Text(
                text = "${clampedValue.toInt()} $unitTitle",
                color = UvosDialText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
