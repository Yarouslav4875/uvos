package com.uvos.dashboard

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * UVOS GaugeView - стрелочный прибор в стиле ретро-панельных измерителей
 * (DESIGN.md §3.1, референс: панельный вольтметр М4200, docs/references/).
 *
 * Внешний вид по референсу:
 * - Чёрный корпус со скруглением 20dp и четырьмя винтами по углам
 *   (плюс юстировочный винт снизу по центру).
 * - Бледный циферблат #E8E4D8 с арочным верхом (как окошко М4200).
 * - Шкала: деления каждые 10%, крупные с цифрами каждые 50%,
 *   красная зона - последние 20% шкалы (правило UVOS).
 * - Размах 270°, инерция стрелки 600 мс (DecelerateInterpolator).
 * - Цифровое значение по центру (крупно, как буква «V» у М4200).
 * - Маркировка прибора (title) внизу слева, как «М4200» у референса.
 * - Лампочка в правом верхнем углу циферблата: горит #E02020 в красной зоне.
 * Все размеры через density/scaledDensity (dp/sp).
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
    private val caseColor = inkColor
    private val screwColor = Color.parseColor("#0E0E12")
    private val screwSlotColor = Color.parseColor("#2A2C36")
    private val redColor = Color.parseColor("#E02020")
    private val lampDimColor = Color.parseColor("#7A1010")

    // --- Кисти ---
    private val casePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = caseColor
        style = Paint.Style.FILL
    }
    private val dialPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = dialColor
        style = Paint.Style.FILL
    }
    private val bevelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = screwSlotColor
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * density
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
    private val screwPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = screwColor
        style = Paint.Style.FILL
    }
    private val screwSlotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = screwSlotColor
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * density
    }
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = inkColor
        textSize = 10f * scaledDensity
        typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
        textAlign = Paint.Align.LEFT
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = inkColor
        textSize = 11f * scaledDensity
        typeface = Typeface.create("sans-serif-condensed", Typeface.NORMAL)
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = inkColor
        textSize = 30f * scaledDensity
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

    /** Арочное окошко циферблата, как у М4200: верх скруглён сильно, низ слегка. */
    private fun buildDialPath(l: Float, t: Float, r: Float, b: Float): Path {
        val radii = floatArrayOf(
            42f * density, 42f * density, // верх-лево (арка)
            42f * density, 42f * density, // верх-право (арка)
            16f * density, 16f * density, // низ-право
            16f * density, 16f * density  // низ-лево
        )
        val rect = RectF(l, t, r, b)
        return Path().apply { addRoundRect(rect, radii, Path.Direction.CW) }
    }

    /** Винт корпуса: тёмная головка со шлицем под углом. */
    private fun drawScrew(canvas: Canvas, cx: Float, cy: Float, r: Float, angleDeg: Float) {
        canvas.drawCircle(cx, cy, r, screwPaint)
        canvas.drawCircle(cx, cy, r, screwSlotPaint)
        val a = Math.toRadians(angleDeg.toDouble())
        val dx = r * 0.75f * cos(a).toFloat()
        val dy = r * 0.75f * sin(a).toFloat()
        canvas.drawLine(cx - dx, cy - dy, cx + dx, cy + dy, screwSlotPaint)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()

        val pad = 4f * density
        val corner = 20f * density

        // 1. Корпус прибора: чёрный, скругление 20dp (референс: корпус М4200)
        canvas.drawRoundRect(pad, pad, w - pad, h - pad, corner, corner, casePaint)

        // 2. Окошко циферблата: арка сверху (референс: форма окна М4200)
        val dialLeft = pad + 13f * density
        val dialTop = pad + 12f * density
        val dialRight = w - pad - 13f * density
        val dialBottom = h - pad - 12f * density
        val dialPath = buildDialPath(dialLeft, dialTop, dialRight, dialBottom)
        canvas.drawPath(dialPath, dialPaint)
        canvas.drawPath(dialPath, bevelPaint)

        val cx = w / 2f
        val cy = if (isCompact) h * 0.68f else h * 0.62f
        val size = min(w, h)
        val radius = size * (if (isCompact) 0.44f else 0.40f)

        // 3. Красная зона шкалы (последние 20%: с 80% до 100%)
        val redStart = startAngle + sweepAngle * 0.8f
        val arcRect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        canvas.drawArc(arcRect, redStart, sweepAngle * 0.2f, false, redZonePaint)

        // 4. Деления шкалы каждые 10% (жирные каждые 50%)
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

        // 5. Подписи MIN / MAX по нижним углам циферблата
        labelPaint.textAlign = Paint.Align.LEFT
        canvas.drawText(minLabel, dialLeft + 10f * density, dialBottom - 10f * density, labelPaint)
        labelPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(maxLabel, dialRight - 10f * density, dialBottom - 10f * density, labelPaint)

        // 6. Маркировка прибора внизу слева, как «М4200» у референса
        titlePaint.textAlign = Paint.Align.LEFT
        val titleText = if (title.isNotBlank()) "UVOS · $title" else "UVOS"
        val titleX = dialLeft + 10f * density
        val titleY = dialBottom - 24f * density
        if (titleY < cy + radius * 0.2f) {
            canvas.drawText(titleText, titleX, titleY, titlePaint)
        } else {
            canvas.drawText(titleText, titleX, cy + radius * 0.2f, titlePaint)
        }

        // 7. Цифровое значение по центру циферблата (крупно, как «V» у М4200)
        val valStr = customValueText ?: "${(needleLevel * 100).toInt()}$unit"
        val isCritical = needleLevel >= 0.95f
        valuePaint.color = if (isCritical) redColor else inkColor
        val textY = cy - radius * 0.30f
        canvas.drawText(valStr, cx, textY, valuePaint)

        // 8. Индикаторная лампа в правом верхнем углу циферблата
        val inRedZone = needleLevel >= 0.80f
        lampPaint.color = if (inRedZone) redColor else lampDimColor
        val lampR = 6f * density
        val lampX = dialRight - 16f * density
        val lampY = dialTop + 20f * density
        canvas.drawCircle(lampX, lampY, lampR, lampPaint)

        // 9. Стрелка с инерцией: черенок от оси + короткий хвостик за осью
        val angle = Math.toRadians((startAngle + sweepAngle * needleLevel).toDouble())
        val needleLen = radius - 10f * density
        val tail = 8f * density
        canvas.drawLine(
            cx - tail * cos(angle).toFloat(), cy - tail * sin(angle).toFloat(),
            cx + needleLen * cos(angle).toFloat(), cy + needleLen * sin(angle).toFloat(),
            needlePaint
        )

        // Ось стрелки
        canvas.drawCircle(cx, cy, 5f * density, pivotPaint)

        // 10. Винты корпуса: четыре по углам (референс) + юстировочный снизу по центру.
        // Сл orientation шлица чередуется, как на настоящих приборах.
        val screwR = 4.5f * density
        val sx = pad + 10f * density
        val sy = pad + 10f * density
        val ex = w - pad - 10f * density
        val ey = h - pad - 10f * density
        drawScrew(canvas, sx, sy, screwR, 45f)
        drawScrew(canvas, ex, sy, screwR, -30f)
        drawScrew(canvas, sx, ey, screwR, 15f)
        drawScrew(canvas, ex, ey, screwR, 60f)
        drawScrew(canvas, cx, h - pad - 8f * density, 3.5f * density, 90f)
    }
}
