package com.uvos.dashboard

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * UVOS GaugeView - стрелочный прибор в стиле ретро-VU-метра.
 *
 * Дизайн по референсу: бледный циферблат, чёрные деления,
 * красная зона в конце шкалы, тёмная стрелка, «лампочка» сверху.
 *
 * Уровень задаётся числом 0..1 (setLevel) и стрелка плавно едет к нему.
 */
class GaugeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // --- Настройки с XML (минимум для прототипа) ---
    private var title = "VU"
    private var minLabel = "MIN"
    private var maxLabel = "MAX"

    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.GaugeView, 0, 0).apply {
            try {
                title = getString(R.styleable.GaugeView_title) ?: title
                minLabel = getString(R.styleable.GaugeView_minLabel) ?: minLabel
                maxLabel = getString(R.styleable.GaugeView_maxLabel) ?: maxLabel
            } finally {
                recycle()
            }
        }
    }

    // --- Цвета (палитра UVOS, продублирована кодом для Canvas) ---
    private val dialColor = Color.parseColor("#E8E4D8")
    private val inkColor = Color.parseColor("#1A1A1A")
    private val redColor = Color.parseColor("#E02020")
    private val lampColor = Color.parseColor("#7A1010")

    // --- Кисти ---
    private val dialPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = dialColor
        style = Paint.Style.FILL
    }
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = inkColor
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val needlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = inkColor
        style = Paint.Style.STROKE
        strokeWidth = 6f
        strokeCap = Paint.Cap.ROUND
    }
    private val redZonePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = redColor
        style = Paint.Style.STROKE
        strokeWidth = 14f
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = inkColor
        textSize = 40f
        textAlign = Paint.Align.CENTER
    }
    private val lampPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = lampColor
        style = Paint.Style.FILL
    }

    // --- Геометрия шкалы ---
    private val startAngle = 135f   // нижний-левый край шкалы (градусы)
    private val sweepAngle = 270f   // полный размах 270 градусов

    // --- Текущее значение стрелки (0..1) ---
    private var needleLevel = 0f
    private var animator: ValueAnimator? = null

    /** Плавно перевести стрелку на новый уровень [0..1]. */
    fun setLevel(newLevel: Float) {
        val target = newLevel.coerceIn(0f, 1f)
        animator?.cancel()
        animator = ValueAnimator.ofFloat(needleLevel, target).apply {
            duration = 600
            interpolator = DecelerateInterpolator()
            addUpdateListener { anim ->
                needleLevel = anim.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val pad = 24f
        val size = min(w, h) - pad * 2

        // Корпус прибора (бледный циферблат)
        canvas.drawRoundRect(pad, pad, w - pad, h - pad, 28f, 28f, dialPaint)

        val cx = w / 2f
        val cy = h * 0.62f
        val radius = size * 0.42f

        // Красная зона: последние 20% шкалы
        val redStart = startAngle + sweepAngle * 0.8f
        val rect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        canvas.drawArc(rect, redStart, sweepAngle * 0.2f, false, redZonePaint)

        // Деления: каждые 10%
        for (i in 0..10) {
            val a = Math.toRadians((startAngle + sweepAngle * i / 10f).toDouble())
            val tickOuter = radius - 12f
            val tickInner = radius - (if (i % 5 == 0) 26f else 18f)
            canvas.drawLine(
                cx + tickInner * cos(a).toFloat(), cy + tickInner * sin(a).toFloat(),
                cx + tickOuter * cos(a).toFloat(), cy + tickOuter * sin(a).toFloat(),
                tickPaint
            )
        }

        // Подписи MIN / MAX и название прибора
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText(minLabel, pad + 18f, h - pad - 14f, textPaint)
        textPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(maxLabel, w - pad - 18f, h - pad - 14f, textPaint)
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(title, cx, cy - radius * 0.35f, textPaint)

        // «Лампочка» подсветки
        canvas.drawCircle(w - pad - 34f, pad + 34f, 10f, lampPaint)

        // Стрелка
        val angle = Math.toRadians((startAngle + sweepAngle * needleLevel).toDouble())
        val needleLen = radius - 24f
        canvas.drawLine(
            cx, cy,
            cx + needleLen * cos(angle).toFloat(),
            cy + needleLen * sin(angle).toFloat(),
            needlePaint
        )
        // Ось стрелки
        canvas.drawCircle(cx, cy, 8f, needlePaint)
    }
}
