package com.uvos.compass.core.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.uvos.compass.core.ui.theme.M4200Case
import com.uvos.compass.core.ui.theme.M4200CaseBezel
import com.uvos.compass.core.ui.theme.M4200DialAged
import com.uvos.compass.core.ui.theme.M4200DialIvory
import com.uvos.compass.core.ui.theme.M4200Ink
import com.uvos.compass.core.ui.theme.M4200MetalSilver
import com.uvos.compass.core.ui.theme.M4200ScrewHead
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * M4200-style aviation course instrument: an ivory rose window in a black
 * instrument case with four corner screws.
 *
 * - the dial is fixed: graduation every 5 degrees, majors every 30 degrees
 *   with three-digit aviation labels (000..330);
 * - the course needle is driven from the bottom center of the dial
 *   (like the M4200 pointer) and reads the heading relative to the
 *   fixed top lubber index;
 * - faint counter ray "burn" on the dial is pure paint, no animations.
 */
@Composable
fun CompassRoseView(
    headingDeg: Float,
    modifier: Modifier = Modifier
) {
    val labelPaint = remember {
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textSize = 30f
            color = M4200Ink.toArgb()
        }
    }
    val brandPaint = remember {
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.LEFT
            typeface = Typeface.MONOSPACE
            textSize = 18f
            color = M4200Ink.copy(alpha = 0.85f).toArgb()
        }
    }

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillCanvas()) {
            val w = size.width
            val h = size.height
            val corner = min(w, h) * 0.075f

            // 1. Case
            drawRoundRect(
                color = M4200Case,
                topLeft = Offset.Zero,
                size = size,
                cornerRadius = CornerRadius(corner, corner)
            )
            drawRoundRect(
                color = M4200CaseBezel,
                topLeft = Offset.Zero,
                size = size,
                cornerRadius = CornerRadius(corner, corner),
                style = Stroke(width = min(w, h) * 0.014f)
            )

            // 2. Four corner screws, slotted, orientation alternating per DESIGN.md
            val screwMargin = corner * 0.62f
            val screwRadius = corner * 0.16f
            val screws = listOf(
                Offset(screwMargin, screwMargin) to 35f,
                Offset(w - screwMargin, screwMargin) to 125f,
                Offset(screwMargin, h - screwMargin) to 125f,
                Offset(w - screwMargin, h - screwMargin) to 35f
            )
            screws.forEach { (center, slotAngle) ->
                drawScrew(center, screwRadius, slotAngle)
            }

            // 3. Dial face (ivory circle) with bezel
            val dialCenter = Offset(w / 2f, h / 2f)
            val dialRadius = min(w, h) * 0.415f
            drawCircle(color = M4200CaseBezel, radius = dialRadius + dialRadius * 0.035f, center = dialCenter)
            drawCircle(color = M4200DialIvory, radius = dialRadius, center = dialCenter)
            // Inner aged ring near the edge — matte depth without gradients
            drawCircle(
                color = M4200DialAged.copy(alpha = 0.6f),
                radius = dialRadius * 0.985f,
                center = dialCenter,
                style = Stroke(width = dialRadius * 0.05f)
            )

            // 4. Scale: 5-degree minors, 30-degree majors with aviation labels
            drawRoseScale(
                center = dialCenter,
                dialRadius = dialRadius,
                labelPaint = labelPaint
            )

            // 5. Top lubber index triangle (fixed bearing reference)
            drawLubberIndex(center = dialCenter, dialRadius = dialRadius)

            // 6. Instrument markings, bottom-left inside the dial
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawText(
                    "UVOS · M4200",
                    dialCenter.x - dialRadius * 0.72f,
                    dialCenter.y + dialRadius * 0.86f,
                    brandPaint
                )
            }

            // 7. Course needle from bottom-center pivot, rotated with heading
            drawCourseNeedle(
                center = dialCenter,
                dialRadius = dialRadius,
                headingDeg = headingDeg % 360f
            )

            // 8. Center hub over the needle
            drawCircle(color = M4200Case, radius = dialRadius * 0.085f, center = dialCenter)
            drawCircle(
                color = M4200MetalSilver.copy(alpha = 0.75f),
                radius = dialRadius * 0.045f,
                center = Offset(dialCenter.x - dialRadius * 0.02f, dialCenter.y - dialRadius * 0.02f)
            )
        }
    }
}

@Composable
private fun remember(init: () -> Paint): Paint = androidx.compose.runtime.remember(init)

private fun Modifier.fillCanvas(): Modifier = this.fillMaxSize()

private fun DrawScope.drawRoseScale(
    center: Offset,
    dialRadius: Float,
    labelPaint: Paint
) {
    val labelRadius = dialRadius * 0.76f
    for (deg in 0 until 360 step 5) {
        val isMajor = deg % 30 == 0
        val tickLength = if (isMajor) dialRadius * 0.10f else dialRadius * 0.045f
        val strokeWidth = if (isMajor) dialRadius * 0.014f else dialRadius * 0.005f
        val angleRad = Math.toRadians(deg.toDouble())
        val dirX = cos(angleRad).toFloat()
        val dirY = sin(angleRad).toFloat()

        // Ticks drawn from the edge inward
        val start = Offset(center.x + dirX * dialRadius * 0.97f, center.y + dirY * dialRadius * 0.97f)
        val end = Offset(center.x + dirX * (dialRadius * 0.97f - tickLength), center.y + dirY * (dialRadius * 0.97f - tickLength))
        drawLine(
            color = M4200Ink,
            start = start,
            end = end,
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        if (isMajor) {
            val textPos = Offset(
                center.x + dirX * labelRadius,
                center.y + dirY * labelRadius
            )
            drawIntoCanvas { canvas ->
                // Center the glyph block on the tick direction
                val fm = labelPaint.fontMetrics
                canvas.nativeCanvas.drawText(
                    String.format("%03d", deg),
                    textPos.x,
                    textPos.y - (fm.ascent + fm.descent) / 2f,
                    labelPaint
                )
            }
        }
    }
}

private fun DrawScope.drawLubberIndex(center: Offset, dialRadius: Float) {
    val tipY = center.y - dialRadius * 0.84f
    val half = dialRadius * 0.055f
    val baseY = tipY - dialRadius * 0.09f
    val path = Path().apply {
        moveTo(center.x, tipY + dialRadius * 0.02f)
        lineTo(center.x - half, baseY)
        lineTo(center.x + half, baseY)
        close()
    }
    drawPath(path, color = M4200Ink)
}

/**
 * M4200 needle: tapered pointer with a thin tail, pivot at the bottom center
 * of the dial. A small red tip marks the reading end of the pointer.
 */
private fun DrawScope.drawCourseNeedle(
    center: Offset,
    dialRadius: Float,
    headingDeg: Float
) {
    rotate(degrees = headingDeg, pivot = center) {
        val tipY = center.y - dialRadius * 0.80f
        val tailY = center.y + dialRadius * 0.55f
        val halfW = dialRadius * 0.030f

        // Tail (thin counterweight stem)
        val tailPath = Path().apply {
            moveTo(center.x - halfW * 0.45f, center.y)
            lineTo(center.x + halfW * 0.45f, center.y)
            lineTo(center.x + halfW * 0.30f, tailY)
            lineTo(center.x - halfW * 0.30f, tailY)
            close()
        }
        drawPath(tailPath, color = M4200Ink)

        // Main tapered pointer to the tip
        val needlePath = Path().apply {
            moveTo(center.x, tipY - dialRadius * 0.06f)
            lineTo(center.x - halfW, center.y)
            lineTo(center.x + halfW, center.y)
            close()
        }
        drawPath(needlePath, color = M4200Ink)

        // Red reading tip on the heading edge
        val tipPath = Path().apply {
            moveTo(center.x, tipY - dialRadius * 0.06f)
            lineTo(center.x - halfW * 0.9f, tipY + dialRadius * 0.16f)
            lineTo(center.x + halfW * 0.9f, tipY + dialRadius * 0.16f)
            close()
        }
        drawPath(tipPath, color = RED_TIP)
    }
}

private val RED_TIP = Color(0xFFE02020)

/**
 * Slotted metal screw head with a light highlight rim.
 */
private fun DrawScope.drawScrew(center: Offset, radius: Float, slotAngleDeg: Float) {
    drawCircle(color = M4200ScrewHead, radius = radius, center = center)
    drawCircle(
        color = M4200MetalSilver.copy(alpha = 0.4f),
        radius = radius,
        center = center,
        style = Stroke(width = radius * 0.18f)
    )
    rotate(degrees = slotAngleDeg, pivot = center) {
        drawLine(
            color = Color(0xFF1A1A1A),
            start = Offset(center.x - radius * 0.68f, center.y),
            end = Offset(center.x + radius * 0.68f, center.y),
            strokeWidth = radius * 0.22f
        )
    }
}

private fun Modifier.fillCanvas2(): Modifier = this
