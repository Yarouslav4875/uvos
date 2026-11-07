package com.uvos.clock.ui.components

import android.graphics.Paint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.uvos.clock.ui.theme.BrassGold
import com.uvos.clock.ui.theme.DialFaceIvory
import com.uvos.clock.ui.theme.DialMarkingBlack
import com.uvos.clock.ui.theme.NeedleRed
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnalogStopwatchDial(
    elapsedMillis: Long,
    isRunning: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 260.dp
) {
    val totalSeconds = elapsedMillis / 1000f
    val currentSecond = totalSeconds % 60f
    val targetSecondAngle = currentSecond * 6f
    val totalMinutes = (elapsedMillis / (1000f * 60f)) % 30f
    val minuteAngle = totalMinutes * 12f // 30 минут на 360 градусов

    // Баллистика стрелки при сбросе (Snap-back spring animation)
    val animatedSecondAngle = remember { Animatable(0f) }

    LaunchedEffect(elapsedMillis, isRunning) {
        if (!isRunning && elapsedMillis == 0L) {
            // Анимация упругого отскока стрелки в нулевое положение
            animatedSecondAngle.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        } else {
            animatedSecondAngle.snapTo(targetSecondAngle)
        }
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val center = Offset(w / 2f, h / 2f)
            val outerRadius = this.size.minDimension / 2f - 4.dp.toPx()

            // 1. Корпус секундомера
            drawRoundRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF2C313E), Color(0xFF13151D)),
                    center = center,
                    radius = outerRadius * 1.05f
                ),
                topLeft = Offset(2.dp.toPx(), 2.dp.toPx()),
                size = Size(w - 4.dp.toPx(), h - 4.dp.toPx()),
                cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx())
            )
            drawRoundRect(
                color = Color(0xFF485064),
                topLeft = Offset(2.dp.toPx(), 2.dp.toPx()),
                size = Size(w - 4.dp.toPx(), h - 4.dp.toPx()),
                cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx()),
                style = Stroke(width = 2.dp.toPx())
            )

            // 2. Металлический безель
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF8893AD), Color(0xFF20232D)),
                    center = center,
                    radius = outerRadius
                ),
                radius = outerRadius * 0.95f,
                center = center
            )

            // 3. Диск шкалы М4200
            val dialRadius = outerRadius * 0.88f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(DialFaceIvory, Color(0xFFDDD7C4)),
                    center = Offset(center.x - dialRadius * 0.1f, center.y - dialRadius * 0.1f),
                    radius = dialRadius
                ),
                radius = dialRadius,
                center = center
            )

            // 4. Дополнительный циферблат минут (30-минутный накопитель)
            val subDialCenter = Offset(center.x, center.y - dialRadius * 0.35f)
            val subDialRadius = dialRadius * 0.28f

            drawCircle(
                color = Color(0xFFDFD9C8),
                radius = subDialRadius,
                center = subDialCenter
            )
            drawCircle(
                color = Color(0xFF6B6455),
                radius = subDialRadius,
                center = subDialCenter,
                style = Stroke(width = 1.dp.toPx())
            )

            // Деления дополнительного минутного циферблата (0..30 мин)
            for (m in 0 until 30) {
                val subAngle = m * 12f
                val isSubMajor = m % 5 == 0
                val subLen = if (isSubMajor) 5.dp.toPx() else 3.dp.toPx()

                rotate(degrees = subAngle, pivot = subDialCenter) {
                    drawLine(
                        color = DialMarkingBlack,
                        start = Offset(subDialCenter.x, subDialCenter.y - subDialRadius),
                        end = Offset(subDialCenter.x, subDialCenter.y - subDialRadius + subLen),
                        strokeWidth = if (isSubMajor) 1.5.dp.toPx() else 0.8.dp.toPx()
                    )
                }
            }

            // Стрелка дополнительного минутного циферблата
            rotate(degrees = minuteAngle, pivot = subDialCenter) {
                drawLine(
                    color = Color(0xFF1E2129),
                    start = subDialCenter,
                    end = Offset(subDialCenter.x, subDialCenter.y - subDialRadius * 0.75f),
                    strokeWidth = 2.dp.toPx()
                )
                drawCircle(color = BrassGold, radius = 2.5.dp.toPx(), center = subDialCenter)
            }

            // 5. Текстовая гравировка
            drawIntoCanvas { canvas ->
                val paint = Paint().apply {
                    color = android.graphics.Color.argb(200, 30, 30, 30)
                    textSize = 9.dp.toPx()
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                    typeface = android.graphics.Typeface.MONOSPACE
                }
                val paintSmall = Paint().apply {
                    color = android.graphics.Color.argb(160, 60, 60, 60)
                    textSize = 7.dp.toPx()
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                    typeface = android.graphics.Typeface.MONOSPACE
                }
                canvas.nativeCanvas.drawText("ХРОНОГРАФ-4200", center.x, center.y + dialRadius * 0.30f, paint)
                canvas.nativeCanvas.drawText("1/100 СЕКУНДЫ", center.x, center.y + dialRadius * 0.42f, paintSmall)
            }

            // 6. Основная 60-секундная шкала с 300 делениями (по 1/5 секунды)
            val tickStartRadius = dialRadius * 0.90f
            for (i in 0 until 60) {
                val angleDeg = i * 6f
                val isMajor = i % 5 == 0
                val tickLen = if (isMajor) 12.dp.toPx() else 6.dp.toPx()
                val tickWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx()

                rotate(degrees = angleDeg, pivot = center) {
                    drawLine(
                        color = DialMarkingBlack,
                        start = Offset(center.x, center.y - tickStartRadius),
                        end = Offset(center.x, center.y - tickStartRadius + tickLen),
                        strokeWidth = tickWidth
                    )
                }
            }

            // Числа 0, 5, 10, ... 55, 60
            drawIntoCanvas { canvas ->
                val numPaint = Paint().apply {
                    color = android.graphics.Color.BLACK
                    textSize = 11.dp.toPx()
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                    isFakeBoldText = true
                    typeface = android.graphics.Typeface.SANS_SERIF
                }
                val numRadius = dialRadius * 0.72f
                for (sNum in 0..11) {
                    val secVal = sNum * 5
                    val label = if (secVal == 0) "60" else secVal.toString()
                    val angleRad = Math.toRadians((secVal * 6 - 90).toDouble())
                    val nx = center.x + (numRadius * cos(angleRad)).toFloat()
                    val ny = center.y + (numRadius * sin(angleRad)).toFloat() + 4.dp.toPx()
                    canvas.nativeCanvas.drawText(label, nx, ny, numPaint)
                }
            }

            // 7. Основная красная секундная стрелка с противовесом
            rotate(degrees = animatedSecondAngle.value, pivot = center) {
                val needleLen = dialRadius * 0.88f
                val counterWeightLen = 22.dp.toPx()

                // Стрелка
                drawLine(
                    color = NeedleRed,
                    start = Offset(center.x, center.y + counterWeightLen),
                    end = Offset(center.x, center.y - needleLen),
                    strokeWidth = 2.dp.toPx()
                )

                // Диск противовеса
                drawCircle(
                    color = NeedleRed,
                    radius = 4.5.dp.toPx(),
                    center = Offset(center.x, center.y + counterWeightLen * 0.65f)
                )
            }

            // 8. Центральная ось
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(BrassGold, Color(0xFF4E3A0B)),
                    center = center,
                    radius = 6.dp.toPx()
                ),
                radius = 6.dp.toPx(),
                center = center
            )
            drawCircle(
                color = Color(0xFF1E2128),
                radius = 2.5.dp.toPx(),
                center = center
            )
        }
    }
}
