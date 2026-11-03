package com.uvos.terminal.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uvos.terminal.ui.theme.UvosCrimson
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnalogGaugeView(
    currentValue: Float,
    minValue: Float,
    maxValue: Float,
    unitText: String,
    titleText: String,
    valueDisplayText: String,
    modifier: Modifier = Modifier,
    size: Dp = 100.dp
) {
    // Spring damped ballistic animation (damping ratio ~ 0.72)
    val animatedAngle = remember { Animatable(-135f) }

    LaunchedEffect(currentValue) {
        val clamped = currentValue.coerceIn(minValue, maxValue)
        val fraction = (clamped - minValue) / (maxValue - minValue)
        // Scale arc: from -135° to +45° (sweep = 180°)
        val targetAngle = -135f + (fraction * 180f)
        animatedAngle.animateTo(
            targetValue = targetAngle,
            animationSpec = spring(
                dampingRatio = 0.72f,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = titleText,
            color = Color(0xFFA0A5B5),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(2.dp))

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(size)
        ) {
            Canvas(modifier = Modifier.size(size)) {
                val w = this.size.width
                val h = this.size.height
                val center = Offset(w / 2f, h * 0.68f)
                val dialRadius = w * 0.44f

                // 1. Vintage ivory dial background (M4200 style)
                drawCircle(
                    color = Color(0xFFE8E4D8),
                    radius = dialRadius,
                    center = center
                )

                // 2. Bezel and screws
                drawCircle(
                    color = Color(0xFF1C1E26),
                    radius = dialRadius,
                    center = center,
                    style = Stroke(width = 3.5f)
                )

                // 3. Dial scale arc
                val arcRadius = dialRadius * 0.78f
                val startRad = Math.toRadians(-135.0)
                val endRad = Math.toRadians(45.0)

                // Ticks and scale marks
                val totalTicks = 8
                for (i in 0..totalTicks) {
                    val tickFraction = i.toFloat() / totalTicks.toFloat()
                    val tickAngleDeg = -135f + (tickFraction * 180f)
                    val tickRad = Math.toRadians(tickAngleDeg.toDouble())

                    val isMajor = i % 2 == 0
                    val tickLen = if (isMajor) dialRadius * 0.20f else dialRadius * 0.12f
                    val strokeW = if (isMajor) 2.2f else 1.2f

                    val innerR = arcRadius - tickLen
                    val outerR = arcRadius

                    val p1 = Offset(
                        (center.x + innerR * cos(tickRad)).toFloat(),
                        (center.y + innerR * sin(tickRad)).toFloat()
                    )
                    val p2 = Offset(
                        (center.x + outerR * cos(tickRad)).toFloat(),
                        (center.y + outerR * sin(tickRad)).toFloat()
                    )

                    // Warning zone (> 85% of scale)
                    val tickColor = if (tickFraction > 0.85f) Color(0xFFD32F2F) else Color(0xFF222222)

                    drawLine(
                        color = tickColor,
                        start = p1,
                        end = p2,
                        strokeWidth = strokeW,
                        cap = StrokeCap.Square
                    )
                }

                // 4. Calibrated pointer / needle (lacquered red #E02020)
                val rad = Math.toRadians(animatedAngle.value.toDouble())
                val pointerLen = dialRadius * 0.82f
                val endX = (center.x + pointerLen * cos(rad)).toFloat()
                val endY = (center.y + pointerLen * sin(rad)).toFloat()

                // Counterweight tail
                val tailLen = dialRadius * 0.22f
                val tailX = (center.x - tailLen * cos(rad)).toFloat()
                val tailY = (center.y - tailLen * sin(rad)).toFloat()

                drawLine(
                    color = Color(0xFFE02020),
                    start = Offset(tailX, tailY),
                    end = Offset(endX, endY),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )

                // 5. Central brass cap & axis
                drawCircle(
                    color = Color(0xFF1C1E26),
                    radius = 6.5f,
                    center = center
                )
                drawCircle(
                    color = Color(0xFFD4AF37),
                    radius = 2.5f,
                    center = center
                )
            }
        }

        // Digital telemetry readout
        Text(
            text = valueDisplayText,
            color = UvosCrimson,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
