package com.uvos.music.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uvos.music.core.ui.theme.M4200AccentCyan
import com.uvos.music.core.ui.theme.M4200AmberGlow
import com.uvos.music.core.ui.theme.M4200ChassisBorder
import com.uvos.music.core.ui.theme.M4200MetalDark
import com.uvos.music.core.ui.theme.M4200MetalSilver
import com.uvos.music.core.ui.theme.M4200ScaleBlack
import com.uvos.music.core.ui.theme.M4200TextDim
import com.uvos.music.core.ui.theme.M4200TextSecondary
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Vintage Brushed Aluminum Rotary Potentiometer with Tactile Feedback.
 * Value range: 0.0f .. 1.0f (or customized min..max).
 */
@Composable
fun RotaryKnob(
    value: Float, // 0.0f .. 1.0f
    onValueChange: (Float) -> Unit,
    label: String,
    valueDisplay: String,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    tintColor: Color = M4200AmberGlow
) {
    val haptic = LocalHapticFeedback.current
    var lastHapticValue by remember { mutableFloatStateOf(value) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        // Vertical drag gesture: dragging up increases value, down decreases
                        val delta = -dragAmount.y * 0.006f
                        val newValue = (value + delta).coerceIn(0.0f, 1.0f)
                        if (abs(newValue - lastHapticValue) > 0.08f) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            lastHapticValue = newValue
                        }
                        onValueChange(newValue)
                    }
                }
        ) {
            Canvas(modifier = Modifier.size(size)) {
                val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
                val outerRadius = size.toPx() / 2f - 4f
                val knobRadius = outerRadius - 6f

                // 1. Background Arc Track (-135° to +135° = 270° sweep)
                val startAngle = 135f
                val sweepTotal = 270f
                drawArc(
                    color = M4200ChassisBorder,
                    startAngle = startAngle,
                    sweepAngle = sweepTotal,
                    useCenter = false,
                    topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                    size = androidx.compose.ui.geometry.Size(outerRadius * 2, outerRadius * 2),
                    style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                )

                // Active Filled Arc
                drawArc(
                    color = tintColor,
                    startAngle = startAngle,
                    sweepAngle = sweepTotal * value.coerceIn(0f, 1f),
                    useCenter = false,
                    topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                    size = androidx.compose.ui.geometry.Size(outerRadius * 2, outerRadius * 2),
                    style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                )

                // 2. Rotary Knob Body (Brushed metal texture / radial gradient)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(M4200MetalSilver, M4200MetalDark),
                        center = Offset(center.x - knobRadius * 0.25f, center.y - knobRadius * 0.25f),
                        radius = knobRadius
                    ),
                    radius = knobRadius,
                    center = center
                )

                // Knob edge ring
                drawCircle(
                    color = M4200ChassisBorder,
                    radius = knobRadius,
                    center = center,
                    style = Stroke(width = 1.8f)
                )

                // 3. Indicator Notch / Line on Knob Face
                val currentAngleDeg = startAngle + sweepTotal * value.coerceIn(0f, 1f)
                val angleRad = currentAngleDeg * (PI.toFloat() / 180f)

                val notchStart = Offset(
                    center.x + (knobRadius * 0.45f) * cos(angleRad),
                    center.y + (knobRadius * 0.45f) * sin(angleRad)
                )
                val notchEnd = Offset(
                    center.x + (knobRadius * 0.90f) * cos(angleRad),
                    center.y + (knobRadius * 0.90f) * sin(angleRad)
                )

                drawLine(
                    color = M4200ScaleBlack,
                    start = notchStart,
                    end = notchEnd,
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = tintColor,
                    start = notchStart,
                    end = notchEnd,
                    strokeWidth = 1.8f,
                    cap = StrokeCap.Round
                )
            }
        }

        Spacer(modifier = Modifier.height(3.dp))

        // Readout Text
        Text(
            text = valueDisplay,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = tintColor
        )

        // Knob Title
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            color = M4200TextSecondary
        )
    }
}
