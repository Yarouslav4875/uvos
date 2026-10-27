package com.uvos.dashboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/**
 * UVOS SliderView - линейная шкала («ползунок прибора», DESIGN.md §3.5).
 *
 * Спецификация:
 * - Высота: 48dp на фоне tile.
 * - Дорожка: 4dp, цвет divider (#2A2C36).
 * - Заполненная часть: text (#D8D8DC), при >80% (красная зона) — red (#E02020).
 * - Стрелка-указатель: треугольник 12dp над дорожкой.
 * - Цифровое значение справа: 18sp моноширинный.
 */
class SliderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = context.resources.displayMetrics.density
    private val scaledDensity = context.resources.displayMetrics.scaledDensity

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2A2C36")
        strokeCap = Paint.Cap.ROUND
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeCap = Paint.Cap.ROUND
    }
    private val pointerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 17f * scaledDensity
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        textAlign = Paint.Align.RIGHT
    }

    private var progress = 0.5f // 0..1
    var onProgressChanged: ((Float) -> Unit)? = null
    var valueFormatter: ((Float) -> String) = { p -> "${(p * 1000).toInt()} МБ" }

    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.SliderView, 0, 0).apply {
            try {
                progress = getFloat(R.styleable.SliderView_progress, 0.5f).coerceIn(0f, 1f)
            } finally {
                recycle()
            }
        }
    }

    fun setProgress(value: Float) {
        progress = value.coerceIn(0f, 1f)
        invalidate()
    }

    fun getProgress(): Float = progress

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val h = (48f * density).toInt()
        val w = getDefaultSize((260f * density).toInt(), widthMeasureSpec)
        setMeasuredDimension(w, resolveSize(h, heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()

        val textWidth = 90f * density
        val trackStart = 16f * density
        val trackEnd = w - textWidth - 12f * density
        val trackY = h * 0.65f
        val trackThickness = 4f * density

        // 1. Базовая дорожка (divider #2A2C36)
        trackPaint.strokeWidth = trackThickness
        canvas.drawLine(trackStart, trackY, trackEnd, trackY, trackPaint)

        // 2. Заполненная часть
        val currentX = trackStart + (trackEnd - trackStart) * progress
        val isRedZone = progress >= 0.8f
        val accentColor = if (isRedZone) Color.parseColor("#E02020") else Color.parseColor("#D8D8DC")

        fillPaint.color = accentColor
        fillPaint.strokeWidth = trackThickness
        canvas.drawLine(trackStart, trackY, currentX, trackY, fillPaint)

        // 3. Стрелка-указатель (треугольник 12dp вершиной вниз)
        val arrowSize = 10f * density
        val arrowBottom = trackY - 3f * density
        val arrowTop = arrowBottom - arrowSize
        pointerPaint.color = accentColor

        val path = Path().apply {
            moveTo(currentX, arrowBottom)
            lineTo(currentX - arrowSize * 0.6f, arrowTop)
            lineTo(currentX + arrowSize * 0.6f, arrowTop)
            close()
        }
        canvas.drawPath(path, pointerPaint)

        // 4. Текст значения справа
        textPaint.color = accentColor
        val textY = h / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(valueFormatter(progress), w - 8f * density, textY, textPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val textWidth = 90f * density
                val trackStart = 16f * density
                val trackEnd = width - textWidth - 12f * density
                val newProgress = ((event.x - trackStart) / (trackEnd - trackStart)).coerceIn(0f, 1f)
                if (newProgress != progress) {
                    progress = newProgress
                    invalidate()
                    onProgressChanged?.invoke(progress)
                }
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
