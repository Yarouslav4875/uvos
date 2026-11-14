package com.uvos.camera.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.uvos.camera.ui.theme.AccentRed
import com.uvos.camera.ui.theme.TextIvory

@Composable
fun ShutterControl(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 84.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.92f else 1.0f, label = "shutter_scale")

    Box(
        modifier = modifier
            .size(size)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * scale)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val outerRadius = this.size.minDimension / 2f - 4.dp.toPx()
            val innerRadius = outerRadius - 10.dp.toPx()

            // Outer metallic knurled ring
            drawCircle(
                color = Color(0xFF2E3240),
                radius = outerRadius,
                center = center,
                style = Stroke(width = 4.dp.toPx())
            )

            // Slotted bezel ring notches
            val notchCount = 24
            for (i in 0 until notchCount) {
                val angle = Math.toRadians((i * (360.0 / notchCount))).toFloat()
                val r1 = outerRadius - 2.dp.toPx()
                val r2 = outerRadius + 2.dp.toPx()
                val start = Offset(center.x + (Math.cos(angle.toDouble()) * r1).toFloat(), center.y + (Math.sin(angle.toDouble()) * r1).toFloat())
                val end = Offset(center.x + (Math.cos(angle.toDouble()) * r2).toFloat(), center.y + (Math.sin(angle.toDouble()) * r2).toFloat())
                drawLine(
                    color = Color(0xFF5A6075),
                    start = start,
                    end = end,
                    strokeWidth = 2.dp.toPx()
                )
            }

            // Inner Red Core Trigger
            drawCircle(
                color = AccentRed,
                radius = innerRadius,
                center = center
            )

            // Center ivory micro-dot
            drawCircle(
                color = TextIvory,
                radius = 3.dp.toPx(),
                center = center
            )
        }
    }
}
