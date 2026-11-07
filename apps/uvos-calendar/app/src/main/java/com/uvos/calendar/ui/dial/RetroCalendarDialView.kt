package com.uvos.calendar.ui.dial

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.uvos.calendar.ui.theme.M4200Bezel
import com.uvos.calendar.ui.theme.M4200DialAged
import com.uvos.calendar.ui.theme.M4200DialIvory
import com.uvos.calendar.ui.theme.M4200Ink
import com.uvos.calendar.ui.theme.M4200InkMuted
import com.uvos.calendar.ui.theme.M4200NeedleRed
import com.uvos.calendar.ui.theme.M4200ScrewHead
import com.uvos.calendar.ui.theme.M4200ScrewSlot
import com.uvos.calendar.ui.theme.UvosBg
import com.uvos.calendar.ui.theme.UvosPanel
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

data class MoonPhaseInfo(
    val phaseRatio: Float, // 0.0..1.0
    val phaseName: String,
    val illuminationPercent: Int
)

fun calculateMoonPhase(timestamp: Long): MoonPhaseInfo {
    // Reference new moon: Jan 11, 2024 11:57 UTC
    val refTime = 1704974220000L
    val synodicPeriod = 29.530588853
    val diffDays = (timestamp - refTime).toDouble() / (86400.0 * 1000.0)
    var phase = diffDays % synodicPeriod
    if (phase < 0) phase += synodicPeriod
    val ratio = (phase / synodicPeriod).toFloat()

    // Illumination 0% at 0 and 1, 100% at 0.5
    val illumination = ((1.0 - cos(ratio * 2.0 * Math.PI)) / 2.0 * 100.0).toInt().coerceIn(0, 100)

    val name = when {
        ratio < 0.03f || ratio > 0.97f -> "Новолуние"
        ratio < 0.22f -> "Молодая луна"
        ratio < 0.28f -> "Первая четверть"
        ratio < 0.47f -> "Прибывающая луна"
        ratio < 0.53f -> "Полнолуние"
        ratio < 0.72f -> "Убывающая луна"
        ratio < 0.78f -> "Последняя четверть"
        else -> "Старая луна"
    }

    return MoonPhaseInfo(
        phaseRatio = ratio,
        phaseName = name,
        illuminationPercent = illumination
    )
}

/**
 * M4200 Analog Aviation Instrument displaying:
 * - Current date (day of month 1..31) with red indicator needle
 * - Day of week circular sector subdial (ПН..ВС) with pointer
 * - Lunar phase astronomical aperture with illuminated moon
 * - Event tick markers along the dial rim
 */
@Composable
fun RetroCalendarDialView(
    currentDay: Int,
    daysInMonth: Int,
    dayOfWeek: Int, // Calendar.MONDAY..Calendar.SUNDAY
    monthName: String,
    year: Int,
    daysWithEvents: Set<Int>,
    currentTimestamp: Long = System.currentTimeMillis(),
    modifier: Modifier = Modifier
) {
    val moonInfo = remember(currentTimestamp) { calculateMoonPhase(currentTimestamp) }

    val dialNumPaint = remember {
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            color = M4200Ink.toArgb()
        }
    }

    val labelPaint = remember {
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            color = M4200InkMuted.toArgb()
        }
    }

    val brandPaint = remember {
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            color = M4200Ink.toArgb()
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val side = min(w, h)
            val center = Offset(w / 2f, h / 2f)

            // 1. Industrial Rectangular Housing with Bezel
            val corner = side * 0.08f
            drawRoundRect(
                color = UvosPanel,
                topLeft = Offset((w - side) / 2f, (h - side) / 2f),
                size = Size(side, side),
                cornerRadius = CornerRadius(corner, corner)
            )

            // Outer Bezel Ring
            drawRoundRect(
                color = M4200Bezel,
                topLeft = Offset((w - side) / 2f + 6.dp.toPx(), (h - side) / 2f + 6.dp.toPx()),
                size = Size(side - 12.dp.toPx(), side - 12.dp.toPx()),
                cornerRadius = CornerRadius(corner * 0.9f, corner * 0.9f),
                style = Stroke(width = 3.dp.toPx())
            )

            // 2. Corner Screws
            val screwInset = side * 0.07f
            val screwRadius = side * 0.026f
            val leftX = (w - side) / 2f + screwInset
            val rightX = (w + side) / 2f - screwInset
            val topY = (h - side) / 2f + screwInset
            val bottomY = (h + side) / 2f - screwInset

            drawScrew(Offset(leftX, topY), screwRadius)
            drawScrew(Offset(rightX, topY), screwRadius)
            drawScrew(Offset(leftX, bottomY), screwRadius)
            drawScrew(Offset(rightX, bottomY), screwRadius)

            // 3. Vintage Ivory Dial Plate
            val dialRadius = side * 0.40f
            drawCircle(
                color = M4200DialIvory,
                radius = dialRadius,
                center = center
            )

            // Outer Aged Ring
            drawCircle(
                color = M4200DialAged,
                radius = dialRadius,
                center = center,
                style = Stroke(width = 8.dp.toPx())
            )

            // Inner Boundary Ring
            drawCircle(
                color = M4200Ink,
                radius = dialRadius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = M4200Ink.copy(alpha = 0.35f),
                radius = dialRadius * 0.82f,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            // 4. Dial Stamp & Calibration Info
            brandPaint.textSize = side * 0.034f
            labelPaint.textSize = side * 0.024f
            dialNumPaint.textSize = side * 0.030f

            drawIntoCanvas { canvas ->
                val native = canvas.nativeCanvas
                native.drawText("M4200-КАЛЕНДАРЬ", center.x, center.y - dialRadius * 0.48f, brandPaint)
                native.drawText("$monthName $year", center.x, center.y - dialRadius * 0.36f, labelPaint)
            }

            // 5. Day-of-Month Scale (1..daysInMonth)
            val angleStep = 360f / daysInMonth
            for (day in 1..daysInMonth) {
                val dayAngle = -90f + (day - 1) * angleStep
                val isMajor = day == 1 || day % 5 == 0 || day == daysInMonth
                val tickLength = if (isMajor) side * 0.040f else side * 0.022f
                val tickWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx()

                rotate(degrees = dayAngle, pivot = center) {
                    val tickColor = if (day in daysWithEvents) M4200NeedleRed else M4200Ink
                    drawLine(
                        color = tickColor,
                        start = Offset(center.x, center.y - dialRadius),
                        end = Offset(center.x, center.y - dialRadius + tickLength),
                        strokeWidth = tickWidth
                    )

                    // Event indicator badge on rim
                    if (day in daysWithEvents) {
                        drawCircle(
                            color = M4200NeedleRed,
                            radius = 2.5.dp.toPx(),
                            center = Offset(center.x, center.y - dialRadius + tickLength + 4.dp.toPx())
                        )
                    }
                }

                // Render number labels for major ticks
                if (isMajor) {
                    val rad = Math.toRadians((dayAngle).toDouble())
                    val textDist = dialRadius * 0.74f
                    val tx = center.x + (textDist * cos(rad)).toFloat()
                    val ty = center.y + (textDist * sin(rad)).toFloat() + (dialNumPaint.textSize * 0.35f)

                    drawIntoCanvas { canvas ->
                        canvas.nativeCanvas.drawText(day.toString(), tx, ty, dialNumPaint)
                    }
                }
            }

            // 6. Sub-Dial: Day of Week Sector (ПН..ВС)
            drawDayOfWeekSubdial(
                center = Offset(center.x - dialRadius * 0.35f, center.y + dialRadius * 0.30f),
                radius = dialRadius * 0.30f,
                dayOfWeek = dayOfWeek,
                labelPaint = labelPaint,
                brandPaint = brandPaint
            )

            // 7. Sub-Dial: Astronomical Moon Phase Aperture
            drawMoonPhaseSubdial(
                center = Offset(center.x + dialRadius * 0.35f, center.y + dialRadius * 0.30f),
                radius = dialRadius * 0.30f,
                moonInfo = moonInfo,
                labelPaint = labelPaint,
                brandPaint = brandPaint
            )

            // 8. Main Red Instrument Pointer (Needle) pointing to current day
            val needleAngle = -90f + (currentDay - 1) * angleStep
            rotate(degrees = needleAngle, pivot = center) {
                // Needle shadow
                drawLine(
                    color = Color.Black.copy(alpha = 0.25f),
                    start = Offset(center.x + 2.dp.toPx(), center.y + 2.dp.toPx()),
                    end = Offset(center.x + 2.dp.toPx(), center.y - dialRadius * 0.94f + 2.dp.toPx()),
                    strokeWidth = 3.dp.toPx()
                )

                // Needle body
                val needlePath = Path().apply {
                    moveTo(center.x - 3.5.dp.toPx(), center.y + dialRadius * 0.18f)
                    lineTo(center.x - 1.dp.toPx(), center.y - dialRadius * 0.94f)
                    lineTo(center.x, center.y - dialRadius * 0.98f)
                    lineTo(center.x + 1.dp.toPx(), center.y - dialRadius * 0.94f)
                    lineTo(center.x + 3.5.dp.toPx(), center.y + dialRadius * 0.18f)
                    close()
                }
                drawPath(path = needlePath, color = M4200NeedleRed)

                // Needle counterweight circle
                drawCircle(
                    color = M4200NeedleRed,
                    radius = 5.dp.toPx(),
                    center = Offset(center.x, center.y + dialRadius * 0.14f)
                )
            }

            // Central Pivot Bolt
            drawCircle(color = Color(0xFF111111), radius = 6.dp.toPx(), center = center)
            drawCircle(color = Color(0xFF555555), radius = 3.dp.toPx(), center = center)
        }
    }
}

private fun DrawScope.drawScrew(center: Offset, radius: Float) {
    drawCircle(color = M4200ScrewHead, radius = radius, center = center)
    drawCircle(
        color = Color.Black.copy(alpha = 0.3f),
        radius = radius,
        center = center,
        style = Stroke(width = 1.dp.toPx())
    )
    // Screw slot
    drawLine(
        color = M4200ScrewSlot,
        start = Offset(center.x - radius * 0.7f, center.y - radius * 0.7f),
        end = Offset(center.x + radius * 0.7f, center.y + radius * 0.7f),
        strokeWidth = 1.5.dp.toPx()
    )
}

private fun DrawScope.drawDayOfWeekSubdial(
    center: Offset,
    radius: Float,
    dayOfWeek: Int, // Calendar.MONDAY..Calendar.SUNDAY
    labelPaint: Paint,
    brandPaint: Paint
) {
    // Background dial disc
    drawCircle(color = M4200DialAged, radius = radius, center = center)
    drawCircle(color = M4200Ink, radius = radius, center = center, style = Stroke(width = 1.dp.toPx()))

    val daysRu = listOf("ПН", "ВТ", "СР", "ЧТ", "ПТ", "СБ", "ВС")
    // Map Calendar.DAY_OF_WEEK (SUNDAY=1, MONDAY=2...) to 0..6 (ПН=0)
    val dayIdx = when (dayOfWeek) {
        Calendar.MONDAY -> 0
        Calendar.TUESDAY -> 1
        Calendar.WEDNESDAY -> 2
        Calendar.THURSDAY -> 3
        Calendar.FRIDAY -> 4
        Calendar.SATURDAY -> 5
        Calendar.SUNDAY -> 6
        else -> 0
    }

    val dowPaint = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        textSize = radius * 0.28f
    }

    val anglePerSector = 360f / 7f
    for (i in 0 until 7) {
        val angle = -90f + i * anglePerSector
        val isSelected = i == dayIdx
        val rad = Math.toRadians(angle.toDouble())
        val dist = radius * 0.65f
        val tx = center.x + (dist * cos(rad)).toFloat()
        val ty = center.y + (dist * sin(rad)).toFloat() + dowPaint.textSize * 0.35f

        dowPaint.color = if (isSelected) M4200NeedleRed.toArgb() else M4200Ink.toArgb()
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawText(daysRu[i], tx, ty, dowPaint)
        }
    }

    // Subdial center label
    val centerPaint = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = Typeface.MONOSPACE
        textSize = radius * 0.22f
        color = M4200InkMuted.toArgb()
    }
    drawIntoCanvas { canvas ->
        canvas.nativeCanvas.drawText("ДЕНЬ", center.x, center.y + centerPaint.textSize * 0.35f, centerPaint)
    }

    // Indicator needle pointing to selected day of week
    val dowNeedleAngle = -90f + dayIdx * anglePerSector
    rotate(degrees = dowNeedleAngle, pivot = center) {
        drawLine(
            color = M4200NeedleRed,
            start = center,
            end = Offset(center.x, center.y - radius * 0.88f),
            strokeWidth = 2.dp.toPx()
        )
    }
    drawCircle(color = M4200Ink, radius = 3.dp.toPx(), center = center)
}

private fun DrawScope.drawMoonPhaseSubdial(
    center: Offset,
    radius: Float,
    moonInfo: MoonPhaseInfo,
    labelPaint: Paint,
    brandPaint: Paint
) {
    // Astronomical lunar aperture disc
    drawCircle(color = Color(0xFF141720), radius = radius, center = center)
    drawCircle(color = M4200Ink, radius = radius, center = center, style = Stroke(width = 1.dp.toPx()))

    // Moon circle
    val moonRadius = radius * 0.45f
    val moonCenter = Offset(center.x, center.y - radius * 0.12f)

    // Dark base of moon
    drawCircle(color = Color(0xFF282B38), radius = moonRadius, center = moonCenter)

    // Illuminated phase of the moon
    val ratio = moonInfo.phaseRatio
    val illuminatedColor = Color(0xFFE8E4D8)

    if (ratio in 0.05f..0.95f) {
        // Approximate waxing/waning illumination
        val sweepAngle = 180f
        val startAngle = if (ratio <= 0.5f) -90f else 90f
        drawArc(
            color = illuminatedColor,
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = true,
            topLeft = Offset(moonCenter.x - moonRadius, moonCenter.y - moonRadius),
            size = Size(moonRadius * 2, moonRadius * 2)
        )
    } else if (ratio in 0.45f..0.55f) {
        // Full moon
        drawCircle(color = illuminatedColor, radius = moonRadius, center = moonCenter)
    }

    // Border ring for moon
    drawCircle(
        color = Color(0xFF6B655C),
        radius = moonRadius,
        center = moonCenter,
        style = Stroke(width = 1.dp.toPx())
    )

    // Lunar phase text
    val moonPaint = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = Typeface.MONOSPACE
        textSize = radius * 0.20f
        color = M4200DialIvory.toArgb()
    }
    drawIntoCanvas { canvas ->
        canvas.nativeCanvas.drawText("${moonInfo.illuminationPercent}% ЛУНА", center.x, center.y + radius * 0.70f, moonPaint)
    }
}
