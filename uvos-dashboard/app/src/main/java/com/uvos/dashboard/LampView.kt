package com.uvos.dashboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/**
 * UVOS LampView - индикаторная лампа (DESIGN.md §3.6).
 *
 * Статичный индикатор: горит (красный) или погашена (тусклая).
 * Никогда не мигает - правило UVOS.
 */
class LampView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val lampOn = Color.parseColor("#E02020")
    private val lampOff = Color.parseColor("#7A1010")
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var isOn = false

    fun setOn(on: Boolean) {
        isOn = on
        invalidate()
    }

    fun isOn(): Boolean = isOn

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val r = minOf(width, height) / 2f * 0.7f
        glowPaint.color = if (isOn) lampOn else lampOff
        canvas.drawCircle(cx, cy, r, glowPaint)
    }
}
