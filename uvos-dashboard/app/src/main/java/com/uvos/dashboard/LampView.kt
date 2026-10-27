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
 * Размер по спецификации: строго круг 12dp.
 * Статичный индикатор:
 * - ВКЛ: яркий красный #E02020
 * - ВЫКЛ: тусклый дежурный #7A1010
 * Никогда не мигает и не пульсирует (правило DESIGN.md §4).
 */
class LampView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = context.resources.displayMetrics.density

    private val lampOnColor = Color.parseColor("#E02020")
    private val lampOffColor = Color.parseColor("#7A1010")
    private val bezelColor = Color.parseColor("#2A2C36")

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val bezelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f * density
        color = bezelColor
    }

    private var isOn = false

    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.LampView, 0, 0).apply {
            try {
                isOn = getBoolean(R.styleable.LampView_isLampOn, false)
            } finally {
                recycle()
            }
        }
    }

    fun setOn(on: Boolean) {
        if (isOn != on) {
            isOn = on
            invalidate()
        }
    }

    fun isOn(): Boolean = isOn

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = (16f * density).toInt()
        val w = resolveSize(size, widthMeasureSpec)
        val h = resolveSize(size, heightMeasureSpec)
        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val radius = 6f * density // 12dp диаметр

        // Заливка лампы
        fillPaint.color = if (isOn) lampOnColor else lampOffColor
        canvas.drawCircle(cx, cy, radius, fillPaint)

        // Окантовка (фаска прибора)
        canvas.drawCircle(cx, cy, radius, bezelPaint)
    }
}
