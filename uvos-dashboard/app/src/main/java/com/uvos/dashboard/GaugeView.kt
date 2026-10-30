package com.uvos.dashboard

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * UVOS GaugeView - аналоговый ретро-вольтметр в стиле приборов авиационного/советского
 * кокпита и панельных измерителей М4200 (DESIGN.md §3.1).
 *
 * Архитектура:
 * - Сатинированный глубокий чёрный корпус (#08090D) со скруглением и фаской.
 * - Винты со шлицами под минусовую отвёртку строго по углам корпуса.
 * - Внутри - круглый циферблат цвета состаренной бумаги (#E8E4D8) с металлическим безелем.
 * - Точная верхняя дуга развёртки (215° .. 325°) с радиальными засечками внутрь.
 * - Лампочка тревоги в правом верхнем углу вынесена с безопасным отступом от винтов.
 * - Для BATT красная зона опасности слева (0..20%), для TEMP/CPU - справа (80..100%).
 * - В центре прибора только чистая маркировка (BATT / TEMP / CPU / MEM).
 */
class GaugeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = context.resources.displayMetrics.density
    private val scaledDensity = context.resources.displayMetrics.scaledDensity

    // --- Настройки из XML ---
    private var title = "BATT"
    private var minLabel = "0%"
    private var maxLabel = "100%"
    private var unit = "%"
    private var isCompact = false
    private var showValueInside = false

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

    // --- Цвета палитры UVOS ---
    private val caseColor = Color.parseColor("#08090D")
    private val caseBorderColor = Color.parseColor("#1F2230")
    private val dialColor = Color.parseColor("#E8E4D8")
    private val bezelOuterColor = Color.parseColor("#13151D")
    private val bezelRingColor = Color.parseColor("#262938")
    private val inkColor = Color.parseColor("#1A1A1A")
    private val redColor = Color.parseColor("#E02020")
    private val redAlertColor = Color.parseColor("#FF2222")
    private val lampDimColor = Color.parseColor("#7A1010")
    private val screwHeadColor = Color.parseColor("#5A5F72")
    private val screwSlotColor = Color.parseColor("#14151D")

    // --- Кисти ---
    private val casePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = caseColor
        style = Paint.Style.FILL
    }
    private val caseBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = caseBorderColor
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * density
    }
    private val bezelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bezelOuterColor
        style = Paint.Style.FILL
    }
    private val bezelRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bezelRingColor
        style = Paint.Style.STROKE
        strokeWidth = 2.5f * density
    }
    private val dialPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = dialColor
        style = Paint.Style.FILL
    }
    private val dialInnerShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#22000000")
        style = Paint.Style.STROKE
        strokeWidth = 2.5f * density
    }
    private val scaleArcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = inkColor
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
    }
    private val redZonePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = redColor
        style = Paint.Style.STROKE
        strokeWidth = 4f * density
        strokeCap = Paint.Cap.BUTT
    }
    private val tickMajorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = inkColor
        style = Paint.Style.STROKE
        strokeWidth = 2.4f * density
    }
    private val tickMinorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = inkColor
        style = Paint.Style.STROKE
        strokeWidth = 1.4f * density
    }
    private val needlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = inkColor
        style = Paint.Style.FILL
    }
    private val needleLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = redColor
        style = Paint.Style.STROKE
        strokeWidth = 1f * density
    }
    private val pivotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#222430")
        style = Paint.Style.FILL
    }
    private val pivotCapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = inkColor
        style = Paint.Style.FILL
    }
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = inkColor
        textSize = 14f * scaledDensity
        typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.08f
    }
    private val valueInsidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = inkColor
        textSize = 12f * scaledDensity
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    private val lampCorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val lampGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val lampHousingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#161822")
        style = Paint.Style.FILL
    }
    private val lampRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2E3345")
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * density
    }
    private val screwPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = screwHeadColor
        style = Paint.Style.FILL
    }
    private val screwBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#181A24")
        style = Paint.Style.STROKE
        strokeWidth = 1f * density
    }
    private val screwSlotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = screwSlotColor
        style = Paint.Style.STROKE
        strokeWidth = 1.4f * density
    }

    // --- Геометрия дуги шкалы (верхняя концентрическая дуга: 215° .. 325°) ---
    private val startAngle = 215f
    private val sweepAngle = 110f

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

    fun setShowValueInside(show: Boolean) {
        showValueInside = show
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val defaultSize = (160f * density).toInt()
        val w = resolveSize(defaultSize, widthMeasureSpec)
        val h = resolveSize(defaultSize, heightMeasureSpec)
        // Для квадратных вольтметров виджета 2x2 поддерживаем баланс
        val finalSize = if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED) w else h
        setMeasuredDimension(w, finalSize)
    }

    /** Винт корпуса со шлицем под минусовую отвёртку */
    private fun drawScrew(canvas: Canvas, cx: Float, cy: Float, r: Float, angleDeg: Float) {
        canvas.drawCircle(cx, cy, r, screwPaint)
        canvas.drawCircle(cx, cy, r, screwBorderPaint)
        val rad = Math.toRadians(angleDeg.toDouble())
        val dx = r * 0.72f * cos(rad).toFloat()
        val dy = r * 0.72f * sin(rad).toFloat()
        canvas.drawLine(cx - dx, cy - dy, cx + dx, cy + dy, screwSlotPaint)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()

        val pad = 4f * density
        val corner = 20f * density

        // 1. Внешний квадратный корпус прибора (глубокий чёрный сатин)
        val caseRect = RectF(pad, pad, w - pad, h - pad)
        canvas.drawRoundRect(caseRect, corner, corner, casePaint)
        canvas.drawRoundRect(caseRect, corner, corner, caseBorderPaint)

        // 2. Круглый циферблат внутри корпуса с безелем
        val cx = w / 2f
        val cy = h / 2f
        val minDim = min(w, h)
        val dialRadius = (minDim / 2f) - (14f * density)

        // Безель
        canvas.drawCircle(cx, cy, dialRadius + 2.5f * density, bezelPaint)
        canvas.drawCircle(cx, cy, dialRadius + 2.5f * density, bezelRingPaint)

        // Круглый циферблат цвета слоновой кости (#E8E4D8)
        canvas.drawCircle(cx, cy, dialRadius, dialPaint)
        canvas.drawCircle(cx, cy, dialRadius, dialInnerShadowPaint)

        // 3. Дуга шкалы и засечки (центр вращения стрелки немного смещён вниз)
        val pivotY = cy + (dialRadius * 0.38f)
        val arcRadius = dialRadius * 0.74f

        val arcRect = RectF(cx - arcRadius, pivotY - arcRadius, cx + arcRadius, pivotY + arcRadius)

        // Определение опасной зоны:
        // Для аккумулятора (BATT) тревога на разряде: 0..20% (слева)
        // Для температуры (TEMP) / CPU / MEM тревога на перегреве/нагрузке: 80..100% (справа)
        val isBattery = title.contains("BATT", ignoreCase = true)
        val inDanger = if (isBattery) (needleLevel <= 0.20f) else (needleLevel >= 0.80f)

        if (isBattery) {
            // Красная зона слева (0% .. 20%)
            canvas.drawArc(arcRect, startAngle, sweepAngle * 0.20f, false, redZonePaint)
            // Базовая дуга справа (20% .. 100%)
            canvas.drawArc(arcRect, startAngle + sweepAngle * 0.20f, sweepAngle * 0.80f, false, scaleArcPaint)
        } else {
            // Базовая дуга слева (0% .. 80%)
            canvas.drawArc(arcRect, startAngle, sweepAngle * 0.80f, false, scaleArcPaint)
            // Красная зона справа (80% .. 100%)
            canvas.drawArc(arcRect, startAngle + sweepAngle * 0.80f, sweepAngle * 0.20f, false, redZonePaint)
        }

        // 4. Радиальные насечки внутрь (Major=10dp, Minor=6dp)
        for (i in 0..10) {
            val frac = i / 10f
            val angle = Math.toRadians((startAngle + sweepAngle * frac).toDouble())
            val isMajor = (i % 2 == 0)
            val tickLen = if (isMajor) 9.5f * density else 5.5f * density

            val outerX = cx + arcRadius * cos(angle).toFloat()
            val outerY = pivotY + arcRadius * sin(angle).toFloat()
            val innerX = cx + (arcRadius - tickLen) * cos(angle).toFloat()
            val innerY = pivotY + (arcRadius - tickLen) * sin(angle).toFloat()

            val paint = if (isMajor) tickMajorPaint else tickMinorPaint
            canvas.drawLine(outerX, outerY, innerX, innerY, paint)
        }

        // 5. Маркировка прибора в центре циферблата (BATT / TEMP / CPU / MEM)
        val titleY = cy - (dialRadius * 0.05f)
        canvas.drawText(title, cx, titleY, titlePaint)

        if (showValueInside) {
            val valStr = customValueText ?: "${(needleLevel * 100).toInt()}$unit"
            valueInsidePaint.color = if (inDanger) redColor else inkColor
            canvas.drawText(valStr, cx, titleY + 16f * density, valueInsidePaint)
        }

        // 6. Стрелка прибора с плавным ходом и противовесом
        val currentAngleDeg = startAngle + sweepAngle * needleLevel
        val needleRad = Math.toRadians(currentAngleDeg.toDouble())
        val needleLen = arcRadius - 4f * density
        val tailLen = 9f * density

        val tipX = cx + needleLen * cos(needleRad).toFloat()
        val tipY = pivotY + needleLen * sin(needleRad).toFloat()
        val tailX = cx - tailLen * cos(needleRad).toFloat()
        val tailY = pivotY - tailLen * sin(needleRad).toFloat()

        // Отрисовка стрелки в виде клиновидного указателя
        val normalRad = needleRad + Math.PI / 2
        val halfW = 2.2f * density
        val wX = (halfW * cos(normalRad)).toFloat()
        val wY = (halfW * sin(normalRad)).toFloat()

        val needlePath = Path().apply {
            moveTo(tipX, tipY)
            lineTo(cx + wX, pivotY + wY)
            lineTo(tailX, tailY)
            lineTo(cx - wX, pivotY - wY)
            close()
        }
        canvas.drawPath(needlePath, needlePaint)
        canvas.drawLine(tailX, tailY, tipX, tipY, needleLinePaint)

        // Ось и противовес стрелки
        canvas.drawCircle(cx, pivotY, 6.5f * density, pivotPaint)
        canvas.drawCircle(cx, pivotY, 3f * density, pivotCapPaint)

        // 7. Сигнальная лампа (справа вверху, с гарантированным зазором от углового винта!)
        // Винт находится в (w - pad - 12dp, pad + 12dp).
        // Смещаем лампу влево и вниз: lampX = w - pad - 26dp, lampY = pad + 18dp.
        val lampR = 4.5f * density
        val lampHousingR = 7f * density
        val lampX = w - pad - 26f * density
        val lampY = pad + 18f * density

        canvas.drawCircle(lampX, lampY, lampHousingR, lampHousingPaint)
        canvas.drawCircle(lampX, lampY, lampHousingR, lampRingPaint)

        if (inDanger) {
            // Яркий красный свет с ореолом тревоги
            lampGlowPaint.color = Color.parseColor("#44FF2222")
            canvas.drawCircle(lampX, lampY, lampR * 2.2f, lampGlowPaint)
            lampCorePaint.color = redAlertColor
        } else {
            lampCorePaint.color = lampDimColor
        }
        canvas.drawCircle(lampX, lampY, lampR, lampCorePaint)

        // 8. Четыре угловых металлических винта со шлицами
        val screwR = 4.8f * density
        val sMargin = pad + 12f * density
        drawScrew(canvas, sMargin, sMargin, screwR, 35f)               // Верх-лево
        drawScrew(canvas, w - sMargin, sMargin, screwR, -45f)          // Верх-право
        drawScrew(canvas, sMargin, h - sMargin, screwR, 15f)           // Низ-лево
        drawScrew(canvas, w - sMargin, h - sMargin, screwR, 70f)       // Низ-право

        // Центральный юстировочный винт снизу
        drawScrew(canvas, cx, h - pad - 8f * density, 3.6f * density, 90f)
    }
}
