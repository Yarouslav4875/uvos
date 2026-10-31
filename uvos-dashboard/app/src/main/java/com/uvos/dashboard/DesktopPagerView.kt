package com.uvos.dashboard

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import kotlin.math.abs

/**
 * UVOS DesktopPagerView - аппаратный горизонтальный пагинатор рабочих столов (Столы 01, 02, 03, 04).
 * Обеспечивает плавное перелистывание свайпом и фиксацию (snap) на активном столе.
 */
class DesktopPagerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : HorizontalScrollView(context, attrs, defStyleAttr) {

    var currentPage = 0
        private set

    var onPageSelectedListener: ((Int) -> Unit)? = null

    private var velocityTracker: VelocityTracker? = null
    private var isSettling = false

    init {
        isHorizontalScrollBarEnabled = false
        isFillViewport = true
        overScrollMode = OVER_SCROLL_NEVER
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val parentWidth = measuredWidth
        if (childCount > 0) {
            val container = getChildAt(0) as? ViewGroup
            if (container != null) {
                for (i in 0 until container.childCount) {
                    val child = container.getChildAt(i)
                    val childHeightSpec = MeasureSpec.makeMeasureSpec(measuredHeight, MeasureSpec.EXACTLY)
                    val childWidthSpec = MeasureSpec.makeMeasureSpec(parentWidth, MeasureSpec.EXACTLY)
                    child.measure(childWidthSpec, childHeightSpec)
                }
            }
        }
    }

    fun scrollToPage(pageIndex: Int, smooth: Boolean = true) {
        val totalPages = getTotalPages()
        val targetPage = pageIndex.coerceIn(0, totalPages - 1)
        val targetX = targetPage * width
        if (smooth) {
            smoothScrollTo(targetX, 0)
        } else {
            scrollTo(targetX, 0)
        }
        if (currentPage != targetPage) {
            currentPage = targetPage
            onPageSelectedListener?.invoke(currentPage)
        }
    }

    private fun getTotalPages(): Int {
        val container = getChildAt(0) as? ViewGroup ?: return 1
        return container.childCount.coerceAtLeast(1)
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain()
        }
        velocityTracker?.addMovement(ev)

        val handled = super.onTouchEvent(ev)

        if (ev.action == MotionEvent.ACTION_UP || ev.action == MotionEvent.ACTION_CANCEL) {
            velocityTracker?.computeCurrentVelocity(1000)
            val velocityX = velocityTracker?.xVelocity ?: 0f
            snapToNearestPage(velocityX)
            velocityTracker?.recycle()
            velocityTracker = null
        }
        return handled
    }

    private fun snapToNearestPage(velocityX: Float) {
        val pageWidth = width.toFloat()
        if (pageWidth <= 0) return

        val scrollX = scrollX
        var targetPage = Math.round(scrollX / pageWidth)

        // Быстрый свайп
        if (abs(velocityX) > 600f) {
            targetPage = if (velocityX < 0) {
                (currentPage + 1).coerceAtMost(getTotalPages() - 1)
            } else {
                (currentPage - 1).coerceAtLeast(0)
            }
        }

        scrollToPage(targetPage, smooth = true)
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        val pageWidth = width
        if (pageWidth > 0) {
            val newPage = Math.round(l.toFloat() / pageWidth).coerceIn(0, getTotalPages() - 1)
            if (newPage != currentPage) {
                currentPage = newPage
                onPageSelectedListener?.invoke(currentPage)
            }
        }
    }
}
