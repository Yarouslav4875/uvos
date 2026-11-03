package com.uvos.dashboard

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

/**
 * UVOS GaugeView — точная нативная репликация авиационного/кокпитного вольтметра
 * из интерактивной презентации UnderVoltOS (docs/presentations/uvos_ui_presentation.html,
 * функция `renderAviationVoltmeter`).
 *
 * Особенности:
 * - Тёмный сатинированный корпус с градиентом #262832 -> #191A22 -> #111218 -> #0B0C10
 *   и рамкой #3A3C4C.
 * - 4 угловых кокпитных винта со шлицами + юстировочный винт снизу.
 * - Металлический безель с фасками и градиентом #5A5E70 -> #0C0D12.
 * - Тёмный циферблат с радиальным градиентом #191B22 -> #101116 -> #08080B.
 * - Двойная дуговая шкала цвета слоновой кости (#F2E8A6) с аварийной красной зоной (#E02020).
 * - 20 делений с удлинёнными мажорными засечками каждые 4 шага.
 * - Клиновидная стрелка цвета слоновой кости (#F4EEDA) с тёмным противовесом (#383B46).
 * - Нижняя механическая шторка с градиентом и полусферическим колпачком оси.
 * - Сигнальная лампа в верхнем правом углу (тёмно-бордовая в норме, ярко-алая с ореолом свечения при тревоге).
 */
class GaugeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = context.resources.displayMetrics.density

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

    // --- Уровень (0..1) и форматированное значение ---
    private var needleLevel = 0f
    private var customValueText: String? = null
    private var animator: ValueAnimator? = null

    // --- Базовые переиспользуемые кисти ---
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F2E8A6")
        typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.06f
    }

    private val valueInsidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F2E8A6")
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    // Вспомогательные пути для клиппинга и стрелки
    private val dialClipPath = Path()
    private val needlePointerPath = Path()
    private val needleCounterPath = Path()

    /**
     * Плавно перевести стрелку на новый уровень [0..1].
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
        val modeH = MeasureSpec.getMode(heightMeasureSpec)
        val h = MeasureSpec.getSize(heightMeasureSpec)
        val finalSize = if (modeH == MeasureSpec.EXACTLY && h > 0) min(w, h) else w
        setMeasuredDimension(finalSize, finalSize)
    }

    /**
     * Отрисовка кокпитного винта со шлицем под отвертку (точно как в renderAviationVoltmeter)
     */
    private fun drawCockpitScrew(canvas: Canvas, cx: Float, cy: Float, r: Float, angleDeg: Float, scale: Float) {
        // 1. Теневое углубление винта (гнездо)
        fillPaint.shader = null
        fillPaint.color = Color.parseColor("#06070A")
        canvas.drawCircle(cx, cy + 0.8f * scale, r + 1.2f * scale, fillPaint)

        // 2. Радиальный градиент головки винта
        val screwGrad = RadialGradient(
            cx - r * 0.35f, cy - r * 0.35f, r,
            intArrayOf(
                Color.parseColor("#989BA8"),
                Color.parseColor("#565A68"),
                Color.parseColor("#282A34"),
                Color.parseColor("#15161C")
            ),
            floatArrayOf(0.0f, 0.35f, 0.75f, 1.0f),
            Shader.TileMode.CLAMP
        )
        fillPaint.shader = screwGrad
        canvas.drawCircle(cx, cy, r, fillPaint)
        fillPaint.shader = null

        // 3. Ободок головки винта
        strokePaint.shader = null
        strokePaint.color = Color.parseColor("#484B5A")
        strokePaint.strokeWidth = 0.8f * scale
        canvas.drawCircle(cx, cy, r, strokePaint)

        // 4. Шлиц винта с фаской
        val rad = Math.toRadians(angleDeg.toDouble())
        val dx = r * 0.72f * cos(rad).toFloat()
        val dy = r * 0.72f * sin(rad).toFloat()

        // Светлая фаска шлица
        strokePaint.color = Color.parseColor("#747788")
        strokePaint.strokeWidth = 1.6f * scale
        canvas.drawLine(cx - dx, cy - dy + 0.6f * scale, cx + dx, cy + dy + 0.6f * scale, strokePaint)

        // Глубокий теневой пропил
        strokePaint.color = Color.parseColor("#050608")
        strokePaint.strokeWidth = 1.3f * scale
        canvas.drawLine(cx - dx, cy - dy, cx + dx, cy + dy, strokePaint)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        // Референсный размер в презентации — 176x176px.
        // Масштабируем все элементы строго пропорционально.
        val scale = w / 176f

        val pad = 2f * scale
        val caseRadius = 24f * scale

        // Гарантируем сброс прозрачности и шейдеров перед каждым кадром отрисовки
        fillPaint.shader = null
        fillPaint.alpha = 255
        strokePaint.shader = null
        strokePaint.alpha = 255

        // =====================================================================
        // 1. Корпус прибора (тёмный градиент #262832 -> #0B0C10)
        // =====================================================================
        val bodyGrad = LinearGradient(
            0f, 0f, w, h,
            intArrayOf(
                Color.parseColor("#262832"),
                Color.parseColor("#191A22"),
                Color.parseColor("#111218"),
                Color.parseColor("#0B0C10")
            ),
            floatArrayOf(0.0f, 0.30f, 0.70f, 1.0f),
            Shader.TileMode.CLAMP
        )
        fillPaint.shader = bodyGrad
        val caseRect = RectF(pad, pad, w - pad, h - pad)
        canvas.drawRoundRect(caseRect, caseRadius, caseRadius, fillPaint)
        fillPaint.shader = null

        strokePaint.shader = null
        strokePaint.color = Color.parseColor("#3A3C4C")
        strokePaint.strokeWidth = 1f * scale
        canvas.drawRoundRect(caseRect, caseRadius, caseRadius, strokePaint)

        // =====================================================================
        // 2. Четыре угловых кокпитных винта со шлицами
        // =====================================================================
        val screwR = 5.0f * scale
        val screwMargin = 14f * scale
        val trScrewX = w - pad - screwMargin
        val trScrewY = pad + screwMargin

        drawCockpitScrew(canvas, pad + screwMargin, pad + screwMargin, screwR, 42f, scale)
        drawCockpitScrew(canvas, trScrewX, trScrewY, screwR, 132f, scale)
        drawCockpitScrew(canvas, pad + screwMargin, h - pad - screwMargin, screwR, 78f, scale)
        drawCockpitScrew(canvas, w - pad - screwMargin, h - pad - screwMargin, screwR, 25f, scale)

        // Центральный юстировочный винт снизу
        val adjX = w / 2f
        val adjY = h - pad - 12f * scale
        val adjR = 4.0f * scale

        fillPaint.color = Color.parseColor("#060609")
        canvas.drawCircle(adjX, adjY, adjR + 1.5f * scale, fillPaint)
        strokePaint.color = Color.parseColor("#22232E")
        strokePaint.strokeWidth = 1f * scale
        canvas.drawCircle(adjX, adjY, adjR + 1.5f * scale, strokePaint)
        drawCockpitScrew(canvas, adjX, adjY, adjR, 0f, scale)

        // =====================================================================
        // 3. Металлический безель циферблата
        // =====================================================================
        val dialCenterX = w / 2f
        val dialCenterY = h * 0.46f
        val dialRadius = min(w, h) * 0.36f

        val bezelGrad = LinearGradient(
            dialCenterX - dialRadius, dialCenterY - dialRadius,
            dialCenterX + dialRadius, dialCenterY + dialRadius,
            intArrayOf(
                Color.parseColor("#5A5E70"),
                Color.parseColor("#2A2C38"),
                Color.parseColor("#161720"),
                Color.parseColor("#0C0D12")
            ),
            floatArrayOf(0.0f, 0.30f, 0.70f, 1.0f),
            Shader.TileMode.CLAMP
        )
        fillPaint.shader = bezelGrad
        canvas.drawCircle(dialCenterX, dialCenterY, dialRadius + 5.5f * scale, fillPaint)
        fillPaint.shader = null

        // Внутренние кольца безеля
        strokePaint.color = Color.parseColor("#08080C")
        strokePaint.strokeWidth = 1.2f * scale
        canvas.drawCircle(dialCenterX, dialCenterY, dialRadius + 3.2f * scale, strokePaint)

        strokePaint.color = Color.parseColor("#444758")
        strokePaint.strokeWidth = 1.0f * scale
        canvas.drawCircle(dialCenterX, dialCenterY, dialRadius + 1.2f * scale, strokePaint)

        // =====================================================================
        // 4. Тёмный циферблат и шкала (с клиппингом)
        // =====================================================================
        canvas.save()
        dialClipPath.reset()
        dialClipPath.addCircle(dialCenterX, dialCenterY, dialRadius, Path.Direction.CW)
        canvas.clipPath(dialClipPath)

        val dialFaceGrad = RadialGradient(
            dialCenterX, dialCenterY, dialRadius,
            intArrayOf(
                Color.parseColor("#191B22"),
                Color.parseColor("#101116"),
                Color.parseColor("#08080B")
            ),
            floatArrayOf(0.10f, 0.80f, 1.0f),
            Shader.TileMode.CLAMP
        )
        fillPaint.shader = dialFaceGrad
        canvas.drawCircle(dialCenterX, dialCenterY, dialRadius, fillPaint)
        fillPaint.shader = null

        strokePaint.color = Color.argb(153, 0, 0, 0)
        strokePaint.strokeWidth = 2.5f * scale
        canvas.drawCircle(dialCenterX, dialCenterY, dialRadius, strokePaint)
        strokePaint.alpha = 255

        // =====================================================================
        // 5. Дуги и засечки шкалы
        // =====================================================================
        val startAngle = 215f
        val sweepAngle = 110f
        val scaleRadiusOuter = dialRadius * 0.85f
        val scaleRadiusInner = dialRadius * 0.70f

        val isBattery = title.contains("BATT", ignoreCase = true) || title.contains("АКБ", ignoreCase = true)
        val redThreshold = if (isBattery) 0.20f else 0.80f
        val inDanger = if (isBattery) (needleLevel <= redThreshold) else (needleLevel >= redThreshold)

        val rectOuter = RectF(
            dialCenterX - scaleRadiusOuter, dialCenterY - scaleRadiusOuter,
            dialCenterX + scaleRadiusOuter, dialCenterY + scaleRadiusOuter
        )
        val rectInner = RectF(
            dialCenterX - scaleRadiusInner, dialCenterY - scaleRadiusInner,
            dialCenterX + scaleRadiusInner, dialCenterY + scaleRadiusInner
        )

        val colorIvory = Color.parseColor("#F2E8A6")
        val colorRed = Color.parseColor("#E02020")

        if (isBattery) {
            // Красная зона слева (0% .. 20%)
            val redSweep = sweepAngle * redThreshold
            val safeSweep = sweepAngle * (1f - redThreshold)

            strokePaint.color = colorRed
            strokePaint.strokeWidth = 2.0f * scale
            canvas.drawArc(rectOuter, startAngle, redSweep, false, strokePaint)

            strokePaint.color = colorIvory
            strokePaint.strokeWidth = 1.4f * scale
            canvas.drawArc(rectOuter, startAngle + redSweep, safeSweep, false, strokePaint)

            strokePaint.color = colorRed
            strokePaint.strokeWidth = 1.5f * scale
            canvas.drawArc(rectInner, startAngle, redSweep, false, strokePaint)

            strokePaint.color = colorIvory
            strokePaint.strokeWidth = 1.1f * scale
            canvas.drawArc(rectInner, startAngle + redSweep, safeSweep, false, strokePaint)
        } else {
            // Обычный прибор: безопасная зона слева (0% .. 80%), красная справа (80% .. 100%)
            val safeSweep = sweepAngle * redThreshold
            val redSweep = sweepAngle * (1f - redThreshold)

            strokePaint.color = colorIvory
            strokePaint.strokeWidth = 1.4f * scale
            canvas.drawArc(rectOuter, startAngle, safeSweep, false, strokePaint)

            strokePaint.color = colorRed
            strokePaint.strokeWidth = 2.0f * scale
            canvas.drawArc(rectOuter, startAngle + safeSweep, redSweep, false, strokePaint)

            strokePaint.color = colorIvory
            strokePaint.strokeWidth = 1.1f * scale
            canvas.drawArc(rectInner, startAngle, safeSweep, false, strokePaint)

            strokePaint.color = colorRed
            strokePaint.strokeWidth = 1.5f * scale
            canvas.drawArc(rectInner, startAngle + safeSweep, redSweep, false, strokePaint)
        }

        // Засечки (20 делений = 21 засечка)
        val totalTicks = 20
        for (i in 0..totalTicks) {
            val frac = i.toFloat() / totalTicks.toFloat()
            val aDeg = startAngle + sweepAngle * frac
            val aRad = Math.toRadians(aDeg.toDouble())
            val isRed = if (isBattery) frac <= redThreshold else frac >= redThreshold
            val isMajor = (i % 4 == 0)

            val rOuter = scaleRadiusOuter + (if (isMajor) 2.5f * scale else 0f)
            val rInner = scaleRadiusInner - (if (isMajor) 2.5f * scale else 0f)

            strokePaint.color = if (isRed) colorRed else colorIvory
            strokePaint.strokeWidth = if (isMajor) 1.8f * scale else 0.9f * scale

            val cosA = cos(aRad).toFloat()
            val sinA = sin(aRad).toFloat()
            canvas.drawLine(
                dialCenterX + rInner * cosA, dialCenterY + rInner * sinA,
                dialCenterX + rOuter * cosA, dialCenterY + rOuter * sinA,
                strokePaint
            )
        }

        // =====================================================================
        // 6. Подпись прибора (CPU, MEM, BATT, NET)
        // =====================================================================
        titlePaint.textSize = 15f * scale
        val titleY = dialCenterY - dialRadius * 0.12f
        canvas.drawText(title, dialCenterX, titleY, titlePaint)

        if (showValueInside) {
            val valStr = customValueText ?: "${(needleLevel * 100).toInt()}$unit"
            valueInsidePaint.textSize = 11f * scale
            valueInsidePaint.color = if (inDanger) colorRed else colorIvory
            canvas.drawText(valStr, dialCenterX, titleY + 14f * scale, valueInsidePaint)
        }

        // =====================================================================
        // 7. Стрелка прибора и ось
        // =====================================================================
        val pivotY = dialCenterY + dialRadius * 0.38f
        val level = needleLevel.coerceIn(0f, 1f)
        val targetScaleAngle = Math.toRadians((startAngle + sweepAngle * level).toDouble())
        val targetX = dialCenterX + scaleRadiusOuter * 0.96f * cos(targetScaleAngle).toFloat()
        val targetY = dialCenterY + scaleRadiusOuter * 0.96f * sin(targetScaleAngle).toFloat()

        val needleAngleRad = kotlin.math.atan2(targetY - pivotY, targetX - dialCenterX)
        val needleLen = hypot(targetX - dialCenterX, targetY - pivotY)

        canvas.save()
        canvas.translate(dialCenterX, pivotY)
        canvas.rotate(Math.toDegrees(needleAngleRad.toDouble()).toFloat())

        // Клиновидный указатель цвета слоновой кости (#F4EEDA)
        needlePointerPath.reset()
        needlePointerPath.moveTo(0f, -1.8f * scale)
        needlePointerPath.lineTo(needleLen, -0.5f * scale)
        needlePointerPath.lineTo(needleLen, 0.5f * scale)
        needlePointerPath.lineTo(0f, 1.8f * scale)
        needlePointerPath.close()

        fillPaint.color = Color.parseColor("#F4EEDA")
        canvas.drawPath(needlePointerPath, fillPaint)

        // Тёмный противовес стрелки (#383B46)
        needleCounterPath.reset()
        needleCounterPath.moveTo(0f, -2.0f * scale)
        needleCounterPath.lineTo(-7f * scale, -1.5f * scale)
        needleCounterPath.lineTo(-7f * scale, 1.5f * scale)
        needleCounterPath.lineTo(0f, 2.0f * scale)
        needleCounterPath.close()

        fillPaint.color = Color.parseColor("#383B46")
        canvas.drawPath(needleCounterPath, fillPaint)

        canvas.restore()

        // =====================================================================
        // 8. Нижняя шторка механизма
        // =====================================================================
        val maskY = dialCenterY + dialRadius * 0.30f
        val maskRect = RectF(
            dialCenterX - dialRadius, maskY,
            dialCenterX + dialRadius, dialCenterY + dialRadius
        )
        val maskGrad = LinearGradient(
            0f, maskY, 0f, dialCenterY + dialRadius,
            intArrayOf(
                Color.parseColor("#1B1D26"),
                Color.parseColor("#101117"),
                Color.parseColor("#08080C")
            ),
            floatArrayOf(0.0f, 0.30f, 1.0f),
            Shader.TileMode.CLAMP
        )
        fillPaint.shader = maskGrad
        canvas.drawRect(maskRect, fillPaint)
        fillPaint.shader = null

        // Разделительная линия шторки
        strokePaint.color = Color.parseColor("#2F3140")
        strokePaint.strokeWidth = 1f * scale
        canvas.drawLine(dialCenterX - dialRadius, maskY, dialCenterX + dialRadius, maskY, strokePaint)

        // Полусферический колпачок оси
        val capRect = RectF(
            dialCenterX - 6.5f * scale, maskY - 6.5f * scale,
            dialCenterX + 6.5f * scale, maskY + 6.5f * scale
        )
        fillPaint.color = Color.parseColor("#0B0C10")
        canvas.drawArc(capRect, 180f, 180f, true, fillPaint)

        strokePaint.color = Color.parseColor("#252734")
        strokePaint.strokeWidth = 0.8f * scale
        canvas.drawArc(capRect, 180f, 180f, false, strokePaint)

        canvas.restore() // восстанавливаем клип циферблата

        // =====================================================================
        // 9. Сигнальная лампа (в верхнем правом углу)
        // =====================================================================
        val lampX = w - pad - 32f * scale
        val lampY = pad + 15f * scale
        val lampR = 4.0f * scale

        // Гнездо лампы
        fillPaint.color = Color.parseColor("#060608")
        canvas.drawCircle(lampX, lampY, lampR + 1.6f * scale, fillPaint)

        strokePaint.color = Color.parseColor("#343644")
        strokePaint.strokeWidth = 1f * scale
        canvas.drawCircle(lampX, lampY, lampR + 1.6f * scale, strokePaint)

        if (inDanger) {
            // Аварийный режим: яркая лампа #FF2424 с ореолом свечения
            fillPaint.color = Color.parseColor("#FF2424")
            canvas.drawCircle(lampX, lampY, lampR, fillPaint)

            val glow = RadialGradient(
                lampX, lampY, 11f * scale,
                intArrayOf(
                    Color.argb((0.85f * 255).toInt(), 255, 36, 36),
                    Color.argb((0.35f * 255).toInt(), 224, 32, 32),
                    Color.argb(0, 224, 32, 32)
                ),
                floatArrayOf(0.1f, 0.5f, 1.0f),
                Shader.TileMode.CLAMP
            )
            fillPaint.shader = glow
            canvas.drawCircle(lampX, lampY, 11f * scale, fillPaint)
            fillPaint.shader = null
        } else {
            // Норма: глубокий тёмно-бордовый с бликом
            fillPaint.color = Color.parseColor("#260505")
            canvas.drawCircle(lampX, lampY, lampR, fillPaint)

            fillPaint.color = Color.argb((0.25f * 255).toInt(), 255, 255, 255)
            canvas.drawCircle(lampX - 1f * scale, lampY - 1f * scale, 1f * scale, fillPaint)
            fillPaint.alpha = 255
        }
    }
}
