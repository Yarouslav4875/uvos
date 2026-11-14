package com.uvos.clock.ui.components

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.uvos.clock.ui.theme.BrassGold
import com.uvos.clock.ui.theme.DialFaceIvory
import com.uvos.clock.ui.theme.DialMarkingBlack
import com.uvos.clock.ui.theme.NeedleRed
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun TimerKnobControl(
    selectedMinutes: Int,
    onMinutesChanged: (Int) -> Unit,
    remainingMillis: Long,
    totalMillis: Long,
    isRunning: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 270.dp
) {
    // Вычисление текущего угла индикации:
    // Если таймер работает — угол отражает оставшееся время относительно общего.
    // Если таймер в режиме настройки — угол соответствует selectedMinutes (0..60 мин -> 0..360 град).
    val displayAngle = if (isRunning && totalMillis > 0) {
        (remainingMillis.toFloat() / totalMillis.toFloat()) * 360f
    } else {
        (selectedMinutes / 60f) * 360f
    }

    Box(
        modifier = modifier
            .size(size)
            .pointerInput(isRunning) {
                if (!isRunning) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val center = Offset(this.size.width / 2f, this.size.height / 2f)
                        val touchPoint = change.position
                        // Вычисляем угол точки относительно центра (0 градусов сверху)
                        val dx = touchPoint.x - center.x
                        val dy = touchPoint.y - center.y
                        var angleDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
                        if (angleDeg < 0) angleDeg += 360f

                        val mins = ((angleDeg / 360f) * 60f).roundToInt().coerceIn(1, 60)
                        onMinutesChanged(mins)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val center = Offset(w / 2f, h / 2f)
            val outerRadius = this.size.minDimension / 2f - 4.dp.toPx()

            // 1. Металлическая панель ручки
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF333847), Color(0xFF14161F)),
                    center = center,
                    radius = outerRadius
                ),
                radius = outerRadius,
                center = center
            )
            drawCircle(
                color = Color(0xFF4C536B),
                radius = outerRadius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // 2. Внешняя шкала минут (0..60)
            val dialRadius = outerRadius * 0.90f
            drawCircle(
                color = DialFaceIvory,
                radius = dialRadius,
                center = center
            )
            drawCircle(
                color = Color(0xFF6E6858),
                radius = dialRadius,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            // Деления от 0 до 60
            for (i in 0 until 60) {
                val angleDeg = i * 6f
                val isMajor = i % 5 == 0
                val tickLen = if (isMajor) 10.dp.toPx() else 5.dp.toPx()

                rotate(degrees = angleDeg, pivot = center) {
                    drawLine(
                        color = DialMarkingBlack,
                        start = Offset(center.x, center.y - dialRadius),
                        end = Offset(center.x, center.y - dialRadius + tickLen),
                        strokeWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx()
                    )
                }
            }

            // Числа 0, 5, 10, ... 60
            drawIntoCanvas { canvas ->
                val numPaint = Paint().apply {
                    color = android.graphics.Color.BLACK
                    textSize = 10.dp.toPx()
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                    typeface = android.graphics.Typeface.MONOSPACE
                    isFakeBoldText = true
                }
                val numRadius = dialRadius * 0.77f
                for (m in 0..11) {
                    val valMin = m * 5
                    val label = if (valMin == 0) "60" else valMin.toString()
                    val angleRad = Math.toRadians((valMin * 6 - 90).toDouble())
                    val nx = center.x + (numRadius * cos(angleRad)).toFloat()
                    val ny = center.y + (numRadius * sin(angleRad)).toFloat() + 3.5.dp.toPx()
                    canvas.nativeCanvas.drawText(label, nx, ny, numPaint)
                }
            }

            // 3. Дуга оставшегося времени (прогресс)
            if (isRunning) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(NeedleRed, BrassGold, NeedleRed),
                        center = center
                    ),
                    startAngle = -90f,
                    sweepAngle = displayAngle,
                    useCenter = false,
                    topLeft = Offset(center.x - dialRadius * 0.68f, center.y - dialRadius * 0.68f),
                    size = androidx.compose.ui.geometry.Size(dialRadius * 1.36f, dialRadius * 1.36f),
                    style = Stroke(width = 6.dp.toPx())
                )
            }

            // 4. Массивная рифленая поворотная ручка (Knob)
            val knobRadius = dialRadius * 0.58f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF3E4353), Color(0xFF1E212A), Color(0xFF13151D)),
                    center = center,
                    radius = knobRadius
                ),
                radius = knobRadius,
                center = center
            )
            drawCircle(
                color = Color(0xFF5A627B),
                radius = knobRadius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // Рифление по окружности ручки (Knurling notches)
            for (k in 0 until 24) {
                val knotchAngle = k * 15f
                rotate(degrees = knotchAngle, pivot = center) {
                    drawLine(
                        color = Color(0xFF5A627B),
                        start = Offset(center.x, center.y - knobRadius),
                        end = Offset(center.x, center.y - knobRadius + 4.dp.toPx()),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }

            // Указатель на ручке (Красная метка / стрелка)
            rotate(degrees = displayAngle, pivot = center) {
                // Выступ указателя
                drawLine(
                    color = NeedleRed,
                    start = Offset(center.x, center.y - knobRadius * 0.45f),
                    end = Offset(center.x, center.y - knobRadius * 0.98f),
                    strokeWidth = 4.dp.toPx()
                )
                drawCircle(
                    color = BrassGold,
                    radius = 3.dp.toPx(),
                    center = Offset(center.x, center.y - knobRadius * 0.88f)
                )
            }

            // Центральный латунный диск
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(BrassGold, Color(0xFF4C380B)),
                    center = center,
                    radius = knobRadius * 0.35f
                ),
                radius = knobRadius * 0.35f,
                center = center
            )
            drawCircle(
                color = Color(0xFF16181F),
                radius = knobRadius * 0.15f,
                center = center
            )
        }
    }
}
