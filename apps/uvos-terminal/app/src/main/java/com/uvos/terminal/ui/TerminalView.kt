package com.uvos.terminal.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp
import com.uvos.terminal.core.TerminalColorScheme
import com.uvos.terminal.core.TerminalSession

@Composable
fun TerminalView(
    session: TerminalSession,
    colorScheme: TerminalColorScheme,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Collect updates from terminal emulator
    val updateTick by session.emulator.updateFlow.collectAsState()

    // Blinking cursor animation
    val infiniteTransition = rememberInfiniteTransition(label = "cursorBlink")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )

    val density = LocalDensity.current
    val fontSizeSp = 13.sp
    val fontSizePx = with(density) { fontSizeSp.toPx() }

    val paint = remember(fontSizePx) {
        Paint().apply {
            isAntiAlias = true
            textSize = fontSizePx
            typeface = Typeface.MONOSPACE
        }
    }

    // Font metrics for monospace character grid
    val charWidth = remember(paint) { paint.measureText("M") }
    val fontMetrics = remember(paint) { paint.fontMetrics }
    val charHeight = remember(fontMetrics) { fontMetrics.descent - fontMetrics.ascent }
    val baselineOffset = remember(fontMetrics) { -fontMetrics.ascent }

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(colorScheme.background)) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        // Calculate maximum columns and rows based on container dimensions
        val measuredCols = if (charWidth > 0f) maxOf(20, (widthPx / charWidth).toInt()) else 80
        val measuredRows = if (charHeight > 0f) maxOf(5, (heightPx / charHeight).toInt()) else 24

        var lastCols by remember { mutableIntStateOf(0) }
        var lastRows by remember { mutableIntStateOf(0) }

        LaunchedEffect(measuredCols, measuredRows) {
            if (measuredCols != lastCols || measuredRows != lastRows) {
                lastCols = measuredCols
                lastRows = measuredRows
                session.resize(measuredRows, measuredCols)
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures {
                        onTap()
                    }
                }
        ) {
            // Read updateTick to ensure recomposition on terminal changes
            val _ = updateTick

            val emulator = session.emulator
            val rows = emulator.getScreenSnapshot()
            val totalRows = minOf(rows.size, measuredRows)

            // Draw each terminal cell
            for (r in 0 until totalRows) {
                val row = rows[r]
                val yTop = r * charHeight
                val yBaseline = yTop + baselineOffset

                for (c in 0 until minOf(row.columns, measuredCols)) {
                    val ch = row.getChar(c)
                    val x = c * charWidth

                    // Draw custom background if present
                    val bg = row.getBgColor(c)
                    if (bg != null) {
                        drawRect(
                            color = bg,
                            topLeft = Offset(x, yTop),
                            size = Size(charWidth, charHeight)
                        )
                    }

                    // Draw character
                    if (ch != ' ' && ch != '\u0000') {
                        val fg = row.getFgColor(c) ?: if (row.isBold(c)) colorScheme.textBright else colorScheme.text
                        paint.color = fg.toArgb()
                        paint.isFakeBoldText = row.isBold(c)
                        drawContext.canvas.nativeCanvas.drawText(ch.toString(), x, yBaseline, paint)
                    }
                }
            }

            // Draw Blinking Cursor
            if (emulator.isCursorVisible && cursorAlpha > 0.4f) {
                val cursorR = emulator.cursorRow.coerceIn(0, measuredRows - 1)
                val cursorC = emulator.cursorCol.coerceIn(0, measuredCols - 1)

                val cursorX = cursorC * charWidth
                val cursorY = cursorR * charHeight

                drawRect(
                    color = colorScheme.cursor,
                    topLeft = Offset(cursorX, cursorY),
                    size = Size(charWidth, charHeight)
                )

                // Render inverse character underneath cursor block
                if (cursorR < rows.size) {
                    val ch = rows[cursorR].getChar(cursorC)
                    if (ch != ' ' && ch != '\u0000') {
                        paint.color = colorScheme.background.toArgb()
                        paint.isFakeBoldText = true
                        drawContext.canvas.nativeCanvas.drawText(
                            ch.toString(),
                            cursorX,
                            cursorY + baselineOffset,
                            paint
                        )
                    }
                }
            }
        }
    }
}
