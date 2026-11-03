package com.uvos.music.core.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.uvos.music.core.ui.theme.M4200AmberGlow
import com.uvos.music.core.ui.theme.M4200BrassCap
import com.uvos.music.core.ui.theme.M4200ChassisBezel
import com.uvos.music.core.ui.theme.M4200ChassisBorder
import com.uvos.music.core.ui.theme.M4200ChassisCard
import com.uvos.music.core.ui.theme.M4200ClipOff
import com.uvos.music.core.ui.theme.M4200DialIvory
import com.uvos.music.core.ui.theme.M4200DialWarm
import com.uvos.music.core.ui.theme.M4200MetalDark
import com.uvos.music.core.ui.theme.M4200MetalSilver
import com.uvos.music.core.ui.theme.M4200OverloadGlow
import com.uvos.music.core.ui.theme.M4200OverloadRed
import com.uvos.music.core.ui.theme.M4200ScaleBlack
import com.uvos.music.core.ui.theme.M4200ScrewHead
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Authentic M4200 Analog VU-Meter Canvas Component.
 * Faithfully reproduces the ivory dial, scale geometry, incandescent illumination,
 * red overload region, corner chassis screws, and calibrated needle sweep.
 */
@Composable
fun GaugeView(
    normalizedLevel: Float, // 0.0 (-20 VU) .. 1.0 (0 VU) .. 1.15 (+3 VU)
    isClipping: Boolean,
    channelLabel: String = "LEFT CHANNEL",
    modifier: Modifier = Modifier
) {
    val textPaint = remember {
        Paint().apply {
            isAntiAlias = true
            textSize = 24f
            color = M4200ScaleBlack.toArgb()
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
    }

    val redTextPaint = remember {
        Paint().apply {
            isAntiAlias = true
            textSize = 24f
            color = M4200OverloadRed.toArgb()
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
    }

    val brandPaint = remember {
        Paint().apply {
            isAntiAlias = true
            textSize = 32f
            color = M4200ScaleBlack.toArgb()
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
    }

    val labelPaint = remember {
        Paint().apply {
            isAntiAlias = true
            textSize = 20f
            color = M4200ScaleBlack.toArgb()
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.22f)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // 1. Draw Outer Instrument Case (Black satin composite)
            drawRoundRect(
                color = M4200ChassisCard,
                size = size,
                cornerRadius = CornerRadius(16f, 16f)
            )
            drawRoundRect(
                color = M4200ChassisBorder,
                size = size,
                cornerRadius = CornerRadius(16f, 16f),
                style = Stroke(width = 3f)
            )

            // 2. Corner Screws (4 corner slotted screw heads)
            val screwMargin = 14f
            val screwRadius = 6.5f
            val screwPositions = listOf(
                Offset(screwMargin, screwMargin),
                Offset(width - screwMargin, screwMargin),
                Offset(screwMargin, height - screwMargin),
                Offset(width - screwMargin, height - screwMargin)
            )
            for (pos in screwPositions) {
                drawScrew(pos, screwRadius)
            }

            // 3. Dial Face Window (Ivory / Vintage warm beige)
            val dialPaddingH = width * 0.065f
            val dialPaddingTop = height * 0.08f
            val dialHeight = height * 0.76f
            val dialWidth = width - dialPaddingH * 2

            val dialRect = Size(dialWidth, dialHeight)
            val dialOffset = Offset(dialPaddingH, dialPaddingTop)

            // Dial Ivory base with soft gradient
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(M4200DialIvory, M4200DialWarm),
                    startY = dialPaddingTop,
                    endY = dialPaddingTop + dialHeight
                ),
                topLeft = dialOffset,
                size = dialRect,
                cornerRadius = CornerRadius(12f, 12f)
            )

            // Warm incandescent lamp glow from top
            drawRoundRect(
                brush = Brush.radialGradient(
                    colors = listOf(M4200AmberGlow.copy(alpha = 0.22f), Color.Transparent),
                    center = Offset(width / 2f, dialPaddingTop + 20f),
                    radius = dialWidth * 0.65f
                ),
                topLeft = dialOffset,
                size = dialRect,
                cornerRadius = CornerRadius(12f, 12f)
            )

            // Dial bezel inner shadow/border
            drawRoundRect(
                color = M4200ChassisBezel,
                topLeft = dialOffset,
                size = dialRect,
                cornerRadius = CornerRadius(12f, 12f),
                style = Stroke(width = 2.5f)
            )

            // 4. Overload / Clip LED Indicator (Top Right inside Dial)
            val ledCenter = Offset(dialOffset.x + dialWidth - 28f, dialOffset.y + 24f)
            drawClipLed(ledCenter, isClipping)

            // 5. Dial Scales & Tick Marks
            // Needle Pivot Point is located near bottom center of the dial
            val pivotX = width / 2f
            val pivotY = dialPaddingTop + dialHeight * 0.94f
            val arcRadius = dialHeight * 0.72f

            drawMeterScales(
                pivotX = pivotX,
                pivotY = pivotY,
                radius = arcRadius,
                textPaint = textPaint,
                redTextPaint = redTextPaint,
                brandPaint = brandPaint,
                labelPaint = labelPaint,
                channelLabel = channelLabel
            )

            // 6. Draw Analog Needle with Rotation
            // Sweep range: -42° for -20 VU, +16° for 0 VU, +38° for +3 VU
            val clampedNormalized = normalizedLevel.coerceIn(0f, 1.25f)
            val needleAngleDeg = if (clampedNormalized <= 1.0f) {
                -42f + (16f - (-42f)) * clampedNormalized
            } else {
                16f + (38f - 16f) * ((clampedNormalized - 1.0f) / 0.15f)
            }

            drawNeedle(
                pivotX = pivotX,
                pivotY = pivotY,
                needleLength = arcRadius + 14f,
                angleDeg = needleAngleDeg
            )

            // 7. Mechanical Zero-Adjust Screw below dial
            drawScrew(Offset(width / 2f, height - 12f), 5.5f)
        }
    }
}

/**
 * Draws the scale arcs, tick marks and labels according to ANSI C16.5.
 */
private fun DrawScope.drawMeterScales(
    pivotX: Float,
    pivotY: Float,
    radius: Float,
    textPaint: Paint,
    redTextPaint: Paint,
    brandPaint: Paint,
    labelPaint: Paint,
    channelLabel: String
) {
    // Primary scale arc geometry:
    // Left boundary: -42° (-20 VU)
    // 0 VU boundary: +16° (0 VU)
    // Right boundary: +38° (+3 VU)
    val startAngleDeg = -42f
    val zeroAngleDeg = 16f
    val endAngleDeg = 38f

    // Scale arcs
    val strokeWidth = 3f
    val redStrokeWidth = 5f

    // Black Arc: -42° to +16°
    drawArc(
        color = M4200ScaleBlack,
        startAngle = 270f + startAngleDeg,
        sweepAngle = zeroAngleDeg - startAngleDeg,
        useCenter = false,
        topLeft = Offset(pivotX - radius, pivotY - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )

    // Red Overload Arc: +16° to +38°
    drawArc(
        color = M4200OverloadRed,
        startAngle = 270f + zeroAngleDeg,
        sweepAngle = endAngleDeg - zeroAngleDeg,
        useCenter = false,
        topLeft = Offset(pivotX - radius, pivotY - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = redStrokeWidth, cap = StrokeCap.Round)
    )

    // Calibrated Tick Marks: (VU Value, isMajor, isRed)
    val tickValues = listOf(
        Pair(-20f, true),
        Pair(-10f, true),
        Pair(-7f, true),
        Pair(-5f, true),
        Pair(-3f, true),
        Pair(-2f, false),
        Pair(-1f, false),
        Pair(0f, true),
        Pair(1f, false),
        Pair(2f, false),
        Pair(3f, true)
    )

    for (tick in tickValues) {
        val vu = tick.first
        val isMajor = tick.second
        val isRed = vu > 0f || (vu == 0f && isMajor)

        // Angle interpolation
        val angleDeg = if (vu <= 0f) {
            -42f + (zeroAngleDeg - (-42f)) * ((vu + 20f) / 20f)
        } else {
            zeroAngleDeg + (endAngleDeg - zeroAngleDeg) * (vu / 3f)
        }

        val angleRad = (angleDeg - 90f) * (PI.toFloat() / 180f)
        val tickLength = if (isMajor) 16f else 9f
        val startR = radius
        val endR = radius - tickLength

        val p1 = Offset(
            x = pivotX + startR * cos(angleRad),
            y = pivotY + startR * sin(angleRad)
        )
        val p2 = Offset(
            x = pivotX + endR * cos(angleRad),
            y = pivotY + endR * sin(angleRad)
        )

        drawLine(
            color = if (vu >= 0f) M4200OverloadRed else M4200ScaleBlack,
            start = p1,
            end = p2,
            strokeWidth = if (isMajor) 2.5f else 1.5f,
            cap = StrokeCap.Round
        )

        // Draw numbers for major ticks
        if (isMajor) {
            val textR = radius - tickLength - 16f
            val textX = pivotX + textR * cos(angleRad)
            val textY = pivotY + textR * sin(angleRad) + 6f
            val labelText = if (vu > 0) "+${vu.toInt()}" else "${vu.toInt()}"

            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawText(
                    labelText,
                    textX,
                    textY,
                    if (vu >= 0) redTextPaint else textPaint
                )
            }
        }
    }

    // Dial Branding & Labels
    drawIntoCanvas { canvas ->
        // Brand "VU"
        canvas.nativeCanvas.drawText(
            "VU",
            pivotX,
            pivotY - radius * 0.45f,
            brandPaint
        )
        // Channel plate
        canvas.nativeCanvas.drawText(
            channelLabel,
            pivotX,
            pivotY - radius * 0.25f,
            labelPaint
        )
    }
}

/**
 * Draws the tapered needle, counterweight, and polished brass cap.
 */
private fun DrawScope.drawNeedle(
    pivotX: Float,
    pivotY: Float,
    needleLength: Float,
    angleDeg: Float
) {
    rotate(degrees = angleDeg, pivot = Offset(pivotX, pivotY)) {
        // Needle Path (Tapered fine pointer)
        val needlePath = Path().apply {
            moveTo(pivotX - 2.5f, pivotY)
            lineTo(pivotX - 0.7f, pivotY - needleLength)
            lineTo(pivotX + 0.7f, pivotY - needleLength)
            lineTo(pivotX + 2.5f, pivotY)
            close()
        }
        drawPath(needlePath, color = M4200ScaleBlack)

        // Circular rear counterweight
        drawCircle(
            color = M4200ScaleBlack,
            radius = 7f,
            center = Offset(pivotX, pivotY + 16f)
        )

        // Pivot center brass hemisphere cap
        drawCircle(
            color = M4200BrassCap,
            radius = 10f,
            center = Offset(pivotX, pivotY)
        )
        drawCircle(
            color = M4200MetalSilver,
            radius = 4f,
            center = Offset(pivotX - 2f, pivotY - 2f)
        )
    }
}

/**
 * Draws overload clip LED indicator.
 */
private fun DrawScope.drawClipLed(center: Offset, isClipping: Boolean) {
    // Outer Bezel
    drawCircle(
        color = M4200MetalDark,
        radius = 8.5f,
        center = center
    )
    drawCircle(
        color = M4200ChassisBorder,
        radius = 8.5f,
        center = center,
        style = Stroke(width = 1.5f)
    )

    if (isClipping) {
        // Glowing halo
        drawCircle(
            color = M4200OverloadGlow.copy(alpha = 0.5f),
            radius = 14f,
            center = center
        )
        // Active bright core
        drawCircle(
            color = M4200OverloadRed,
            radius = 6.5f,
            center = center
        )
    } else {
        // Inactive dark LED
        drawCircle(
            color = M4200ClipOff,
            radius = 6.5f,
            center = center
        )
    }
}

/**
 * Draws a slotted metal screw head with highlight.
 */
private fun DrawScope.drawScrew(center: Offset, radius: Float) {
    drawCircle(
        color = M4200ScrewHead,
        radius = radius,
        center = center
    )
    drawCircle(
        color = M4200MetalSilver.copy(alpha = 0.4f),
        radius = radius,
        center = center,
        style = Stroke(width = 1.2f)
    )
    // 45-degree screw slot
    val slotOffset = radius * 0.7f
    drawLine(
        color = M4200ScaleBlack,
        start = Offset(center.x - slotOffset, center.y - slotOffset),
        end = Offset(center.x + slotOffset, center.y + slotOffset),
        strokeWidth = 1.5f
    )
}
