package com.uvos.camera.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.uvos.camera.ui.theme.ReticleCyan
import com.uvos.camera.ui.theme.TextIvory

@Composable
fun OscilloscopeGridOverlay(
    modifier: Modifier = Modifier,
    reticleColor: Color = ReticleCyan.copy(alpha = 0.55f),
    bracketColor: Color = TextIvory.copy(alpha = 0.65f)
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        // 1. Four corner technical brackets
        val bracketLen = 28.dp.toPx()
        val margin = 20.dp.toPx()
        val strokeW = 1.5.dp.toPx()

        // Top-Left
        drawLine(bracketColor, Offset(margin, margin), Offset(margin + bracketLen, margin), strokeW)
        drawLine(bracketColor, Offset(margin, margin), Offset(margin, margin + bracketLen), strokeW)

        // Top-Right
        drawLine(bracketColor, Offset(w - margin, margin), Offset(w - margin - bracketLen, margin), strokeW)
        drawLine(bracketColor, Offset(w - margin, margin), Offset(w - margin, margin + bracketLen), strokeW)

        // Bottom-Left
        drawLine(bracketColor, Offset(margin, h - margin), Offset(margin + bracketLen, h - margin), strokeW)
        drawLine(bracketColor, Offset(margin, h - margin), Offset(margin, h - margin - bracketLen), strokeW)

        // Bottom-Right
        drawLine(bracketColor, Offset(w - margin, h - margin), Offset(w - margin - bracketLen, h - margin), strokeW)
        drawLine(bracketColor, Offset(w - margin, h - margin), Offset(w - margin, h - margin - bracketLen), strokeW)

        // 2. Central crosshair with circle
        val crosshairRadius = 36.dp.toPx()
        drawCircle(
            color = reticleColor,
            radius = crosshairRadius,
            center = Offset(cx, cy),
            style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
        )

        val crosshairLen = 18.dp.toPx()
        drawLine(reticleColor, Offset(cx - crosshairRadius - crosshairLen, cy), Offset(cx - crosshairRadius, cy), 1.dp.toPx())
        drawLine(reticleColor, Offset(cx + crosshairRadius, cy), Offset(cx + crosshairRadius + crosshairLen, cy), 1.dp.toPx())
        drawLine(reticleColor, Offset(cx, cy - crosshairRadius - crosshairLen), Offset(cx, cy - crosshairRadius), 1.dp.toPx())
        drawLine(reticleColor, Offset(cx, cy + crosshairRadius), Offset(cx, cy + crosshairRadius + crosshairLen), 1.dp.toPx())

        // Center optical point
        drawCircle(
            color = reticleColor,
            radius = 2.dp.toPx(),
            center = Offset(cx, cy)
        )

        // 3. Horizontal alignment guide lines
        val horizonWidth = 60.dp.toPx()
        drawLine(
            color = reticleColor.copy(alpha = 0.35f),
            start = Offset(cx - horizonWidth, cy),
            end = Offset(cx - crosshairRadius - crosshairLen - 10.dp.toPx(), cy),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = reticleColor.copy(alpha = 0.35f),
            start = Offset(cx + crosshairRadius + crosshairLen + 10.dp.toPx(), cy),
            end = Offset(cx + horizonWidth, cy),
            strokeWidth = 1.dp.toPx()
        )
    }
}
