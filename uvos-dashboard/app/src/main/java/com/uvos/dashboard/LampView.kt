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
 * - ВКЛ: цвет в соответствии с типом прибора (изумруд #10B981, янтарь #F59E0B, алый #FF3B30 / #E02020)
 * - ВЫКЛ: выключенная лампа и приглушенный цвет (#71768E / дежурный)
 * Никогда не мигает и не пульсирует (правило DESIGN.md §4).
 */
class LampView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = context.resources.displayMetrics.density

    var lampOnColor: Int = Color.parseColor("#E02020")
        set(value) {
            field = value
            invalidate()
        }

    var lampOffColor: Int = Color.parseColor("#71768E")
        set(value) {
            field = value
            invalidate()
        }

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
                if (hasValue(R.styleable.LampView_lampOnColor)) {
                    lampOnColor = getColor(R.styleable.LampView_lampOnColor, lampOnColor)
                }
                if (hasValue(R.styleable.LampView_lampOffColor)) {
                    lampOffColor = getColor(R.styleable.LampView_lampOffColor, lampOffColor)
                }
            } finally {
                recycle()
            }
        }
    }

    fun setLampColors(onColor: Int, offColor: Int = Color.parseColor("#71768E")) {
        this.lampOnColor = onColor
        this.lampOffColor = offColor
        invalidate()
    }

    fun setLampColors(onColorHex: String, offColorHex: String = "#71768E") {
        setLampColors(Color.parseColor(onColorHex), Color.parseColor(offColorHex))
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
