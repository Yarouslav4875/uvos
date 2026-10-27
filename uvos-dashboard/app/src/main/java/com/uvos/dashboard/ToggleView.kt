package com.uvos.dashboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import android.view.View

/**
 * UVOS ToggleView - тумблер («кнопка прибора», DESIGN.md §3.3).
 *
 * Спецификация:
 * - Форма: прямоугольник 88×44dp, скругление 12dp.
 * - ВЫКЛ: фон tile (#1C1E26), текст text_dim (#8A8A92), встроенная лампа 12dp слева lamp_dim (#7A1010).
 * - ВКЛ: фон red (#E02020), текст белый (#FFFFFF), встроенная лампа светлая (#FFFFFF).
 * - Переход: мгновенный щелчок (без анимации стрелки).
 */
class ToggleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = context.resources.displayMetrics.density
    private val scaledDensity = context.resources.displayMetrics.scaledDensity

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val lampPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 14f * scaledDensity
        typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
    }

    private var isChecked = false
    var onToggleChanged: ((Boolean) -> Unit)? = null

    init {
        isClickable = true
        context.theme.obtainStyledAttributes(attrs, R.styleable.ToggleView, 0, 0).apply {
            try {
                isChecked = getBoolean(R.styleable.ToggleView_checked, false)
            } finally {
                recycle()
            }
        }
    }

    fun setChecked(checked: Boolean) {
        if (isChecked != checked) {
            isChecked = checked
            invalidate()
            onToggleChanged?.invoke(isChecked)
        }
    }

    fun isChecked(): Boolean = isChecked

    override fun performClick(): Boolean {
        super.performClick()
        isChecked = !isChecked
        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        playSoundEffect(SoundEffectConstants.CLICK)
        invalidate()
        onToggleChanged?.invoke(isChecked)
        return true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = (88f * density).toInt()
        val h = (44f * density).toInt()
        setMeasuredDimension(resolveSize(w, widthMeasureSpec), resolveSize(h, heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val corner = 12f * density

        // 1. Фон корпуса тумблера
        bgPaint.color = if (isChecked) Color.parseColor("#E02020") else Color.parseColor("#1C1E26")
        canvas.drawRoundRect(0f, 0f, w, h, corner, corner, bgPaint)

        // 2. Встроенная лампа слева (диаметр 12dp, радиус 6dp)
        val lampR = 6f * density
        val lampX = 18f * density
        val lampY = h / 2f
        lampPaint.color = if (isChecked) Color.parseColor("#FFFFFF") else Color.parseColor("#7A1010")
        canvas.drawCircle(lampX, lampY, lampR, lampPaint)

        // 3. Текст состояния
        textPaint.color = if (isChecked) Color.parseColor("#FFFFFF") else Color.parseColor("#8A8A92")
        val label = if (isChecked) "ВКЛ" else "ВЫКЛ"
        val textX = w * 0.62f
        val textY = h / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(label, textX, textY, textPaint)
    }
}
