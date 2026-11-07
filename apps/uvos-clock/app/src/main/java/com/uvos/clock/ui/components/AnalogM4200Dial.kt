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
fun AnalogM4200Dial(
    hours: Int,
    minutes: Int,
    seconds: Int,
    millis: Int = 0,
    isSmoothSweep: Boolean = false,
    modifier: Modifier = Modifier,
    size: Dp = 280.dp
) {
    // Демпфированная баллистика секундной стрелки:
    // При тиканье секундная стрелка совершает микро-колебание (settling bounce) вокруг целевого деления
    val targetSecondAngle = if (isSmoothSweep) {
        (seconds + millis / 1000f) * 6f
    } else {
        seconds * 6f
    }

    val animatedSecondAngle = remember { Animatable(targetSecondAngle) }

    LaunchedEffect(targetSecondAngle) {
        if (!isSmoothSweep) {
            // При переходе через 360 градусов сбрасываем плавно
            if (targetSecondAngle == 0f && animatedSecondAngle.value > 350f) {
                animatedSecondAngle.snapTo(animatedSecondAngle.value - 360f)
            }
            animatedSecondAngle.animateTo(
                targetValue = targetSecondAngle,
                animationSpec = spring(
                    dampingRatio = 0.55f, // Легкий пружинный отскок советского прибора
                    stiffness = Spring.StiffnessHigh
                )
            )
        } else {
            animatedSecondAngle.snapTo(targetSecondAngle)
        }
    }

    val minuteAngle = (minutes + seconds / 60f) * 6f
    val hourAngle = ((hours % 12) + minutes / 60f) * 30f

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val center = Offset(w / 2f, h / 2f)
            val outerRadius = this.size.minDimension / 2f - 4.dp.toPx()

            // 1. Внешний корпус советского прибора М4200 (Карболитовый/металлический безель)
            drawRoundRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF2E3342), Color(0xFF13151D)),
                    center = center,
                    radius = outerRadius * 1.05f
                ),
                topLeft = Offset(2.dp.toPx(), 2.dp.toPx()),
                size = Size(w - 4.dp.toPx(), h - 4.dp.toPx()),
                cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
            )
            drawRoundRect(
                color = Color(0xFF454C60),
                topLeft = Offset(2.dp.toPx(), 2.dp.toPx()),
                size = Size(w - 4.dp.toPx(), h - 4.dp.toPx()),
                cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                style = Stroke(width = 2.dp.toPx())
            )

            // 4 винта со шлицем по углам корпуса
            val screwRadius = 3.5.dp.toPx()
            val screwOffset = 14.dp.toPx()
            val screwCorners = listOf(
                Offset(screwOffset, screwOffset),
                Offset(w - screwOffset, screwOffset),
                Offset(screwOffset, h - screwOffset),
                Offset(w - screwOffset, h - screwOffset)
            )
            for (screw in screwCorners) {
                drawCircle(color = Color(0xFF1E212B), radius = screwRadius, center = screw)
                drawLine(
                    color = Color(0xFF8B94AB),
                    start = Offset(screw.x - screwRadius * 0.7f, screw.y - screwRadius * 0.7f),
                    end = Offset(screw.x + screwRadius * 0.7f, screw.y + screwRadius * 0.7f),
                    strokeWidth = 1.2.dp.toPx()
                )
            }

            // 2. Металлическое прижимное кольцо циферблата
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF7B85A1), Color(0xFF1F222B)),
                    center = center,
                    radius = outerRadius
                ),
                radius = outerRadius * 0.94f,
                center = center
            )

            // 3. Диск шкалы прибора (Светло-бежевый фон циферблата М4200)
            val dialRadius = outerRadius * 0.88f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(DialFaceIvory, Color(0xFFDED8C7)),
                    center = Offset(center.x - dialRadius * 0.1f, center.y - dialRadius * 0.1f),
                    radius = dialRadius
                ),
                radius = dialRadius,
                center = center
            )
            drawCircle(
                color = Color(0xFF5A554A),
                radius = dialRadius,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            // 4. Текстовая маркировка прибора по ГОСТ
            drawIntoCanvas { canvas ->
                val paint = Paint().apply {
                    color = android.graphics.Color.argb(200, 30, 30, 30)
                    textSize = 10.dp.toPx()
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                    typeface = android.graphics.Typeface.MONOSPACE
                }
                val paintSmall = Paint().apply {
                    color = android.graphics.Color.argb(160, 60, 60, 60)
                    textSize = 7.5.dp.toPx()
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                    typeface = android.graphics.Typeface.MONOSPACE
                }

                canvas.nativeCanvas.drawText("М4200-ЧАСЫ", center.x, center.y - dialRadius * 0.40f, paint)
                canvas.nativeCanvas.drawText("ГОСТ 8.401-80", center.x, center.y - dialRadius * 0.28f, paintSmall)
                canvas.nativeCanvas.drawText("UnderVoltOS", center.x, center.y + dialRadius * 0.38f, paintSmall)
                canvas.nativeCanvas.drawText("QUARTZ 1/100s", center.x, center.y + dialRadius * 0.48f, paint)
            }

            // 5. 60 делений шкалы времени и часовые цифры
            val tickStartRadius = dialRadius * 0.88f
            for (i in 0 until 60) {
                val angleDeg = i * 6f
                val isMajor = i % 5 == 0
                val tickLen = if (isMajor) 12.dp.toPx() else 6.dp.toPx()
                val tickWidth = if (isMajor) 2.2.dp.toPx() else 1.dp.toPx()

                rotate(degrees = angleDeg, pivot = center) {
                    drawLine(
                        color = DialMarkingBlack,
                        start = Offset(center.x, center.y - tickStartRadius),
                        end = Offset(center.x, center.y - tickStartRadius + tickLen),
                        strokeWidth = tickWidth
                    )
                }
            }

            // Часовые цифры 1..12
            drawIntoCanvas { canvas ->
                val numPaint = Paint().apply {
                    color = android.graphics.Color.BLACK
                    textSize = 14.dp.toPx()
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                    isFakeBoldText = true
                    typeface = android.graphics.Typeface.SANS_SERIF
                }
                val numRadius = dialRadius * 0.65f
                for (hNum in 1..12) {
                    val angleRad = Math.toRadians((hNum * 30 - 90).toDouble())
                    val nx = center.x + (numRadius * cos(angleRad)).toFloat()
                    val ny = center.y + (numRadius * sin(angleRad)).toFloat() + 5.dp.toPx()
                    canvas.nativeCanvas.drawText(hNum.toString(), nx, ny, numPaint)
                }
            }

            // 6. Часовая стрелка (Массивная вороненая сталь)
            rotate(degrees = hourAngle, pivot = center) {
                val hLen = dialRadius * 0.50f
                drawLine(
                    color = Color(0xFF1E222A),
                    start = Offset(center.x, center.y + 12.dp.toPx()),
                    end = Offset(center.x, center.y - hLen),
                    strokeWidth = 5.dp.toPx()
                )
                // Контур/грань
                drawLine(
                    color = Color(0xFF4C556A),
                    start = Offset(center.x, center.y + 10.dp.toPx()),
                    end = Offset(center.x, center.y - hLen + 4.dp.toPx()),
                    strokeWidth = 1.5.dp.toPx()
                )
            }

            // 7. Минутная стрелка
            rotate(degrees = minuteAngle, pivot = center) {
                val mLen = dialRadius * 0.74f
                drawLine(
                    color = Color(0xFF1E222A),
                    start = Offset(center.x, center.y + 16.dp.toPx()),
                    end = Offset(center.x, center.y - mLen),
                    strokeWidth = 3.5.dp.toPx()
                )
                drawLine(
                    color = Color(0xFF6B7693),
                    start = Offset(center.x, center.y + 14.dp.toPx()),
                    end = Offset(center.x, center.y - mLen + 4.dp.toPx()),
                    strokeWidth = 1.2.dp.toPx()
                )
            }

            // 8. Секундная стрелка с демпфированной баллистикой (Ярко-красная со стреловидным противовесом)
            rotate(degrees = animatedSecondAngle.value, pivot = center) {
                val sLen = dialRadius * 0.85f
                val counterLen = 22.dp.toPx()

                // Хвостовик противовеса
                drawLine(
                    color = NeedleRed,
                    start = Offset(center.x, center.y + counterLen),
                    end = Offset(center.x, center.y - sLen),
                    strokeWidth = 1.8.dp.toPx()
                )
                // Шарик противовеса
                drawCircle(
                    color = NeedleRed,
                    radius = 4.dp.toPx(),
                    center = Offset(center.x, center.y + counterLen * 0.65f)
                )
            }

            // 9. Центральная крепежная втулка (Латунный колпачок с черным винтом)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(BrassGold, Color(0xFF5A440E)),
                    center = center,
                    radius = 7.dp.toPx()
                ),
                radius = 7.dp.toPx(),
                center = center
            )
            drawCircle(
                color = Color(0xFF1A1A1A),
                radius = 3.dp.toPx(),
                center = center
            )
        }
    }
}
