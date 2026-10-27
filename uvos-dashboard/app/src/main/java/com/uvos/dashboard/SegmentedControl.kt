package com.uvos.dashboard

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SoundEffectConstants
import android.view.View
import android.view.animation.DecelerateInterpolator

/**
 * UVOS SegmentedControl - сегментный переключатель (DESIGN.md §3.10).
 *
 * Спецификация:
 * - Общий фон tile (#1C1E26), скругление 12dp, высота 44dp.
 * - Выбранный сегмент: фон dial (#E8E4D8), текст ink (#1A1A1A).
 * - Невыбранный: текст text_dim (#8A8A92).
 * - Плавный сдвиг подложки: 200 мс с DecelerateInterpolator.
 */
class SegmentedControl @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = context.resources.displayMetrics.density
    private val scaledDensity = context.resources.displayMetrics.scaledDensity

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1C1E26")
    }
    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E8E4D8")
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 13f * scaledDensity
        typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
    }

    private var items = listOf("СТЕРИЛЬНО", "СВЯЗЬ", "СВОЙ")
    private var selectedIndex = 0
    private var thumbLeftFraction = 0f
    private var animator: ValueAnimator? = null

    var onSegmentSelected: ((Int) -> Unit)? = null

    fun setItems(newItems: List<String>, defaultIndex: Int = 0) {
        items = newItems
        selectedIndex = defaultIndex.coerceIn(0, items.size - 1)
        thumbLeftFraction = selectedIndex.toFloat()
        invalidate()
    }

    fun setSelectedIndex(index: Int, animate: Boolean = true) {
        if (index == selectedIndex || index !in items.indices) return
        selectedIndex = index
        onSegmentSelected?.invoke(selectedIndex)
        if (animate) {
            animator?.cancel()
            animator = ValueAnimator.ofFloat(thumbLeftFraction, selectedIndex.toFloat()).apply {
                duration = 200
                interpolator = DecelerateInterpolator()
                addUpdateListener {
                    thumbLeftFraction = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
        } else {
            thumbLeftFraction = selectedIndex.toFloat()
            invalidate()
        }
    }

    fun getSelectedIndex(): Int = selectedIndex

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val h = (44f * density).toInt()
        val w = getDefaultSize((240f * density).toInt(), widthMeasureSpec)
        setMeasuredDimension(w, resolveSize(h, heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (items.isEmpty()) return
        val w = width.toFloat()
        val h = height.toFloat()
        val corner = 12f * density
        val pad = 3f * density

        // 1. Корпус (фон tile #1C1E26)
        canvas.drawRoundRect(0f, 0f, w, h, corner, corner, bgPaint)

        // 2. Ползунок подложки выбранного сегмента (фон dial #E8E4D8)
        val segW = (w - pad * 2) / items.size
        val thumbL = pad + thumbLeftFraction * segW
        val thumbR = thumbL + segW
        val thumbCorner = 9f * density
        canvas.drawRoundRect(thumbL, pad, thumbR, h - pad, thumbCorner, thumbCorner, thumbPaint)

        // 3. Текст всех сегментов
        val textY = h / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
        for (i in items.indices) {
            val cx = pad + i * segW + segW / 2f
            textPaint.color = if (i == selectedIndex) Color.parseColor("#1A1A1A") else Color.parseColor("#8A8A92")
            canvas.drawText(items[i], cx, textY, textPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val pad = 3f * density
            val segW = (width - pad * 2) / items.size
            val clickedIndex = ((event.x - pad) / segW).toInt().coerceIn(0, items.size - 1)
            setSelectedIndex(clickedIndex, animate = true)
            playSoundEffect(SoundEffectConstants.CLICK)
        }
        return true
    }
}
