package com.uvos.terminal.core

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.LinkedList

data class CursorPos(val row: Int, val col: Int, val isVisible: Boolean = true)

class TerminalEmulator(
    var rows: Int = 24,
    var columns: Int = 80,
    private val maxHistory: Int = 2000
) {
    private val history = LinkedList<TerminalRow>()
    private var screen = Array(rows) { TerminalRow(columns) }

    var cursorRow = 0
        private set
    var cursorCol = 0
        private set
    var isCursorVisible = true
        private set

    // Current active style
    private var currentFgColor: Color? = null
    private var currentBgColor: Color? = null
    private var currentBold = false

    // State flow to trigger UI recomposition when content updates
    private val _updateFlow = MutableStateFlow(0L)
    val updateFlow: StateFlow<Long> = _updateFlow.asStateFlow()

    private val lock = Any()

    // ANSI escape parsing state
    private enum class State {
        NORMAL,
        ESCAPE,
        CSI,
        OSC
    }

    private var parseState = State.NORMAL
    private val csiParams = StringBuilder()

    fun resize(newRows: Int, newCols: Int) {
        synchronized(lock) {
            if (newRows <= 0 || newCols <= 0) return
            val oldRows = rows
            val oldCols = columns
            rows = newRows
            columns = newCols

            val newScreen = Array(newRows) { TerminalRow(newCols) }
            val copyRows = minOf(oldRows, newRows)
            for (r in 0 until copyRows) {
                val oldRow = screen[r]
                val copyCols = minOf(oldCols, newCols)
                for (c in 0 until copyCols) {
                    newScreen[r].set(
                        c,
                        oldRow.getChar(c),
                        oldRow.getFgColor(c),
                        oldRow.getBgColor(c),
                        oldRow.isBold(c)
                    )
                }
            }
            screen = newScreen
            cursorRow = cursorRow.coerceIn(0, rows - 1)
            cursorCol = cursorCol.coerceIn(0, columns - 1)
            notifyUpdate()
        }
    }

    fun processBytes(bytes: ByteArray, offset: Int = 0, length: Int = bytes.size) {
        synchronized(lock) {
            val end = offset + length
            var i = offset
            while (i < end) {
                val b = bytes[i].toInt() and 0xFF
                val ch = b.toChar()

                when (parseState) {
                    State.NORMAL -> {
                        when (ch) {
                            '\u001B' -> parseState = State.ESCAPE
                            '\r' -> cursorCol = 0
                            '\n' -> lineFeed()
                            '\b' -> {
                                if (cursorCol > 0) cursorCol--
                            }
                            '\t' -> {
                                val nextTab = ((cursorCol / 8) + 1) * 8
                                cursorCol = minOf(nextTab, columns - 1)
                            }
                            '\u0007' -> { /* Bell - ignore or haptic */ }
                            else -> {
                                if (b in 32..126 || b >= 160) {
                                    putChar(ch)
                                }
                            }
                        }
                    }
                    State.ESCAPE -> {
                        when (ch) {
                            '[' -> {
                                parseState = State.CSI
                                csiParams.clear()
                            }
                            ']' -> {
                                parseState = State.OSC
                            }
                            'c' -> {
                                reset()
                                parseState = State.NORMAL
                            }
                            else -> {
                                parseState = State.NORMAL
                            }
                        }
                    }
                    State.CSI -> {
                        if (ch in '0'..'9' || ch == ';' || ch == '?' || ch == '=') {
                            csiParams.append(ch)
                        } else {
                            handleCsiSequence(ch, csiParams.toString())
                            parseState = State.NORMAL
                        }
                    }
                    State.OSC -> {
                        // Wait until ST (\e\\) or BEL (\u0007)
                        if (ch == '\u0007' || ch == '\u001B') {
                            parseState = State.NORMAL
                        }
                    }
                }
                i++
            }
            notifyUpdate()
        }
    }

    private fun putChar(ch: Char) {
        if (cursorCol >= columns) {
            cursorCol = 0
            lineFeed()
        }
        screen[cursorRow].set(cursorCol, ch, currentFgColor, currentBgColor, currentBold)
        cursorCol++
    }

    private fun lineFeed() {
        if (cursorRow < rows - 1) {
            cursorRow++
        } else {
            // Scroll up
            val scrolledRow = screen[0]
            if (history.size >= maxHistory) {
                history.removeFirst()
            }
            history.addLast(scrolledRow)

            for (r in 0 until rows - 1) {
                screen[r] = screen[r + 1]
            }
            screen[rows - 1] = TerminalRow(columns)
        }
    }

    private fun handleCsiSequence(finalChar: Char, params: String) {
        val args = if (params.startsWith("?")) {
            params.substring(1).split(";").mapNotNull { it.toIntOrNull() }
        } else {
            params.split(";").mapNotNull { it.toIntOrNull() }
        }

        when (finalChar) {
            'A' -> { // Cursor Up
                val count = args.firstOrNull() ?: 1
                cursorRow = maxOf(0, cursorRow - count)
            }
            'B' -> { // Cursor Down
                val count = args.firstOrNull() ?: 1
                cursorRow = minOf(rows - 1, cursorRow + count)
            }
            'C' -> { // Cursor Forward
                val count = args.firstOrNull() ?: 1
                cursorCol = minOf(columns - 1, cursorCol + count)
            }
            'D' -> { // Cursor Backward
                val count = args.firstOrNull() ?: 1
                cursorCol = maxOf(0, cursorCol - count)
            }
            'H', 'f' -> { // Cursor Position
                val r = (args.getOrNull(0) ?: 1) - 1
                val c = (args.getOrNull(1) ?: 1) - 1
                cursorRow = r.coerceIn(0, rows - 1)
                cursorCol = c.coerceIn(0, columns - 1)
            }
            'J' -> { // Erase in Display
                val mode = args.firstOrNull() ?: 0
                when (mode) {
                    0 -> { // Clear below cursor
                        screen[cursorRow].clear(cursorCol, columns)
                        for (r in cursorRow + 1 until rows) {
                            screen[r].clear()
                        }
                    }
                    1 -> { // Clear above cursor
                        for (r in 0 until cursorRow) {
                            screen[r].clear()
                        }
                        screen[cursorRow].clear(0, cursorCol + 1)
                    }
                    2, 3 -> { // Clear entire screen
                        for (r in 0 until rows) {
                            screen[r].clear()
                        }
                        cursorRow = 0
                        cursorCol = 0
                    }
                }
            }
            'K' -> { // Erase in Line
                val mode = args.firstOrNull() ?: 0
                when (mode) {
                    0 -> screen[cursorRow].clear(cursorCol, columns)
                    1 -> screen[cursorRow].clear(0, cursorCol + 1)
                    2 -> screen[cursorRow].clear(0, columns)
                }
            }
            'm' -> { // SGR (Select Graphic Rendition)
                handleSgr(args)
            }
            'h' -> {
                if (params.startsWith("?25")) isCursorVisible = true
            }
            'l' -> {
                if (params.startsWith("?25")) isCursorVisible = false
            }
        }
    }

    private fun handleSgr(args: List<Int>) {
        if (args.isEmpty()) {
            resetSgr()
            return
        }

        var i = 0
        while (i < args.size) {
            when (val code = args[i]) {
                0 -> resetSgr()
                1 -> currentBold = true
                22 -> currentBold = false
                30, 31, 32, 33, 34, 35, 36, 37 -> {
                    currentFgColor = getAnsiColor(code - 30)
                }
                38 -> { // Extended foreground
                    if (i + 2 < args.size && args[i + 1] == 5) {
                        // 256 color: 38;5;N
                        val colorIdx = args[i + 2]
                        currentFgColor = getAnsi256Color(colorIdx)
                        i += 2
                    } else if (i + 4 < args.size && args[i + 1] == 2) {
                        // TrueColor: 38;2;R;G;B
                        currentFgColor = Color(args[i + 2], args[i + 3], args[i + 4])
                        i += 4
                    }
                }
                39 -> currentFgColor = null
                40, 41, 42, 43, 44, 45, 46, 47 -> {
                    currentBgColor = getAnsiColor(code - 40)
                }
                48 -> { // Extended background
                    if (i + 2 < args.size && args[i + 1] == 5) {
                        val colorIdx = args[i + 2]
                        currentBgColor = getAnsi256Color(colorIdx)
                        i += 2
                    } else if (i + 4 < args.size && args[i + 1] == 2) {
                        currentBgColor = Color(args[i + 2], args[i + 3], args[i + 4])
                        i += 4
                    }
                }
                49 -> currentBgColor = null
                90, 91, 92, 93, 94, 95, 96, 97 -> {
                    currentFgColor = getAnsiColor(code - 90 + 8)
                }
                100, 101, 102, 103, 104, 105, 106, 107 -> {
                    currentBgColor = getAnsiColor(code - 100 + 8)
                }
            }
            i++
        }
    }

    private fun resetSgr() {
        currentFgColor = null
        currentBgColor = null
        currentBold = false
    }

    private fun getAnsiColor(idx: Int): Color {
        val palette = TerminalColors.CrimsonScheme.ansiPalette
        return palette.getOrElse(idx.coerceIn(0, palette.size - 1)) { Color.White }
    }

    private fun getAnsi256Color(idx: Int): Color {
        if (idx in 0..15) return getAnsiColor(idx)
        if (idx in 16..231) {
            val n = idx - 16
            val r = (n / 36) * 51
            val g = ((n % 36) / 6) * 51
            val b = (n % 6) * 51
            return Color(r, g, b)
        }
        if (idx in 232..255) {
            val gray = (idx - 232) * 10 + 8
            return Color(gray, gray, gray)
        }
        return Color.White
    }

    private fun reset() {
        history.clear()
        for (r in 0 until rows) {
            screen[r].clear()
        }
        cursorRow = 0
        cursorCol = 0
        resetSgr()
    }

    fun clearScreen() {
        synchronized(lock) {
            for (r in 0 until rows) {
                screen[r].clear()
            }
            cursorRow = 0
            cursorCol = 0
            notifyUpdate()
        }
    }

    fun getScreenSnapshot(): List<TerminalRow> {
        synchronized(lock) {
            return screen.toList()
        }
    }

    fun getHistorySnapshot(): List<TerminalRow> {
        synchronized(lock) {
            return history.toList()
        }
    }

    private fun notifyUpdate() {
        _updateFlow.value = System.currentTimeMillis()
    }
}
