package com.uvos.dashboard

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * UVOS GaugeView - стрелочный прибор в стиле ретро-VU-метра (DESIGN.md §3.1).
 *
 * Спецификация:
 * - Размах шкалы 270°, деления каждые 10%, крупные каждые 50%.
 * - Красная зона: последние 20% шкалы.
 * - Плавная инерция стрелки 600 мс с DecelerateInterpolator.
 * - Лампочка в правом верхнем углу: горит красным (#E02020) при входе в красную зону (>=80%).
 * - Цифровое значение по центру циферблата (28-34sp).
 * - Все размеры вычисляются через density (dp/sp), корпус со скруглением 28dp.
 */
class GaugeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = context.resources.displayMetrics.density
    private val scaledDensity = context.resources.displayMetrics.scaledDensity

    // --- Настройки из XML ---
    private var title = "VU"
    private var minLabel = "MIN"
    private var maxLabel = "MAX"
    private var unit = ""
    private var isCompact = false

    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.GaugeView, 0, 0).apply {
            try {
                title = getString(R.styleable.GaugeView_title) ?: title
                minLabel = getString(R.styleable.GaugeView_minLabel) ?: minLabel
                maxLabel = getString(R.styleable.GaugeView_maxLabel) ?: maxLabel
                unit = getString(R.styleable.GaugeView_unit) ?: unit
                isCompact = getBoolean(R.styleable.GaugeView_isCompact, false)
            } finally {
                recycle()
            }
        }
    }

    // --- Цвета палитры UVOS DESIGN.md §2.1 ---
    private val dialColor = Color.parseColor("#E8E4D8")
    private val inkColor = Color.parseColor("#1A1A1A")
    private val redColor = Color.parseColor("#E02020")
    private val lampDimColor = Color.parseColor("#7A1010")

    // --- Кисти ---
    private val dialPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = dialColor
        style = Paint.Style.FILL
    }
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = inkColor
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
    }
    private val tickHeavyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = inkColor
        style = Paint.Style.STROKE
        strokeWidth = 3.5f * density
    }
    private val needlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = inkColor
        style = Paint.Style.STROKE
        strokeWidth = 3f * density
        strokeCap = Paint.Cap.ROUND
    }
    private val pivotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = inkColor
        style = Paint.Style.FILL
    }
    private val redZonePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = redColor
        style = Paint.Style.STROKE
        strokeWidth = 6f * density
        strokeCap = Paint.Cap.BUTT
    }
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = inkColor
        textSize = 14f * scaledDensity
        typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = inkColor
        textSize = 11f * scaledDensity
        typeface = Typeface.create("sans-serif-condensed", Typeface.NORMAL)
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = inkColor
        textSize = 28f * scaledDensity
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    private val lampPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    // --- Геометрия шкалы (размах 270°) ---
    private val startAngle = 135f
    private val sweepAngle = 270f

    // --- Уровень (0..1) и форматированное значение ---
    private var needleLevel = 0f
    private var customValueText: String? = null
    private var animator: ValueAnimator? = null

    /**
     * Плавно перевести стрелку на новый уровень [0..1].
     * Инерция стрелки: 600 мс с DecelerateInterpolator.
     */
    fun setLevel(newLevel: Float, displayValue: String? = null) {
        val target = newLevel.coerceIn(0f, 1f)
        customValueText = displayValue
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

    fun setTitle(newTitle: String) {
        title = newTitle
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val defaultH = if (isCompact) (160f * density).toInt() else (220f * density).toInt()
        val defaultW = (320f * density).toInt()
        val w = getDefaultSize(defaultW, widthMeasureSpec)
        val h = resolveSize(defaultH, heightMeasureSpec)
        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()

        val corner = 28f * density
        val pad = 4f * density

        // 1. Корпус прибора (бледный циферблат #E8E4D8)
        canvas.drawRoundRect(pad, pad, w - pad, h - pad, corner, corner, dialPaint)

        val cx = w / 2f
        val cy = if (isCompact) h * 0.68f else h * 0.62f
        val size = min(w, h)
        val radius = size * (if (isCompact) 0.44f else 0.40f)

        // 2. Красная зона шкалы (последние 20%: с 80% до 100%)
        val redStart = startAngle + sweepAngle * 0.8f
        val arcRect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        canvas.drawArc(arcRect, redStart, sweepAngle * 0.2f, false, redZonePaint)

        // 3. Деления шкалы каждые 10% (жирные каждые 50%)
        for (i in 0..10) {
            val a = Math.toRadians((startAngle + sweepAngle * i / 10f).toDouble())
            val tickOuter = radius - 2f * density
            val isMajor = (i % 5 == 0)
            val tickLen = if (isMajor) 10f * density else 6f * density
            val tickInner = radius - tickLen

            val paint = if (isMajor) tickHeavyPaint else tickPaint
            canvas.drawLine(
                cx + tickInner * cos(a).toFloat(), cy + tickInner * sin(a).toFloat(),
                cx + tickOuter * cos(a).toFloat(), cy + tickOuter * sin(a).toFloat(),
                paint
            )
        }

        // 4. Подписи MIN / MAX
        labelPaint.textAlign = Paint.Align.LEFT
        canvas.drawText(minLabel, pad + 16f * density, h - pad - 12f * density, labelPaint)
        labelPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(maxLabel, w - pad - 16f * density, h - pad - 12f * density, labelPaint)

        // 5. Заголовок прибора сверху
        canvas.drawText(title, cx, pad + 24f * density, titlePaint)

        // 6. Цифровое значение по центру циферблата (28-34sp)
        val valStr = customValueText ?: "${(needleLevel * 100).toInt()}$unit"
        val isCritical = needleLevel >= 0.95f
        valuePaint.color = if (isCritical) redColor else inkColor
        val textY = cy - radius * 0.30f
        canvas.drawText(valStr, cx, textY, valuePaint)

        // 7. Индикаторная лампа в правом верхнем углу корпуса (12dp, радиус 6dp)
        val inRedZone = needleLevel >= 0.80f
        lampPaint.color = if (inRedZone) redColor else lampDimColor
        val lampR = 6f * density
        val lampX = w - pad - 20f * density
        val lampY = pad + 20f * density
        canvas.drawCircle(lampX, lampY, lampR, lampPaint)

        // 8. Стрелка прибора с инерцией
        val angle = Math.toRadians((startAngle + sweepAngle * needleLevel).toDouble())
        val needleLen = radius - 10f * density
        canvas.drawLine(
            cx, cy,
            cx + needleLen * cos(angle).toFloat(),
            cy + needleLen * sin(angle).toFloat(),
            needlePaint
        )

        // Ось стрелки
        canvas.drawCircle(cx, cy, 5f * density, pivotPaint)
    }
}
