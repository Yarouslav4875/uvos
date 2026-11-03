package com.uvos.terminal.core

import androidx.compose.ui.graphics.Color

data class StyledChar(
    val char: Char = ' ',
    val foregroundColor: Color? = null,
    val backgroundColor: Color? = null,
    val bold: Boolean = false,
    val underline: Boolean = false,
    val inverse: Boolean = false
)

class TerminalRow(val columns: Int) {
    private val chars = CharArray(columns) { ' ' }
    private val fgColors = arrayOfNulls<Color>(columns)
    private val bgColors = arrayOfNulls<Color>(columns)
    private val boldFlags = BooleanArray(columns)

    fun getChar(col: Int): Char {
        return if (col in 0 until columns) chars[col] else ' '
    }

    fun getFgColor(col: Int): Color? {
        return if (col in 0 until columns) fgColors[col] else null
    }

    fun getBgColor(col: Int): Color? {
        return if (col in 0 until columns) bgColors[col] else null
    }

    fun isBold(col: Int): Boolean {
        return if (col in 0 until columns) boldFlags[col] else false
    }

    fun set(
        col: Int,
        char: Char,
        fg: Color?,
        bg: Color?,
        bold: Boolean
    ) {
        if (col in 0 until columns) {
            chars[col] = char
            fgColors[col] = fg
            bgColors[col] = bg
            boldFlags[col] = bold
        }
    }

    fun clear(colStart: Int = 0, colEnd: Int = columns) {
        val start = colStart.coerceIn(0, columns)
        val end = colEnd.coerceIn(start, columns)
        for (i in start until end) {
            chars[i] = ' '
            fgColors[i] = null
            bgColors[i] = null
            boldFlags[i] = false
        }
    }

    fun getAsText(): String {
        return String(chars).trimEnd()
    }
}
