package com.uvos.terminal.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.uvos.terminal.core.TerminalColorScheme

@Composable
fun CrtShaderRenderer(
    scheme: TerminalColorScheme,
    scanlinesEnabled: Boolean = true,
    vignetteEnabled: Boolean = true,
    glowEnabled: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        // Underlying terminal content
        content()

        // CRT Post-processing overlay Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            if (width <= 0 || height <= 0) return@Canvas

            // 1. Scanlines
            if (scanlinesEnabled) {
                val step = 4f
                var y = 0f
                val scanColor = Color(0x28000000)
                while (y < height) {
                    drawLine(
                        color = scanColor,
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.5f
                    )
                    y += step
                }
            }

            // 2. Phosphor Ambient Edge Glow
            if (glowEnabled) {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            scheme.glowColor.copy(alpha = 0.05f),
                            scheme.glowColor.copy(alpha = 0.15f)
                        ),
                        center = Offset(width / 2f, height / 2f),
                        radius = maxOf(width, height) * 0.7f
                    ),
                    size = size
                )
            }

            // 3. Vignette (Tube Curvature darkening)
            if (vignetteEnabled) {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Transparent,
                            Color(0x33000000),
                            Color(0x99000000)
                        ),
                        center = Offset(width / 2f, height / 2f),
                        radius = maxOf(width, height) * 0.72f
                    ),
                    size = size
                )

                // Bezel border frame
                drawRect(
                    color = scheme.bezel,
                    style = Stroke(width = 2f)
                )
            }
        }
    }
}
