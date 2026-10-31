package com.uvos.dashboard

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.OverScroller
import kotlin.math.abs

/**
 * UVOS DesktopPagerView - аппаратный горизонтальный пагинатор рабочих столов (Столы 01, 02, 03, 04).
 * Чистый ViewGroup с жестким контролем геометрии страниц: ровно 100% ширины экрана на каждый стол.
 */
class DesktopPagerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    var currentPage = 0
        private set

    var onPageSelectedListener: ((Int) -> Unit)? = null

    private val scroller = OverScroller(context, DecelerateInterpolator(1.2f))
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val minFlingVelocity = ViewConfiguration.get(context).scaledMinimumFlingVelocity * 1.5f

    private var velocityTracker: VelocityTracker? = null
    private var isBeingDragged = false
    private var lastX = 0f
    private var lastY = 0f
    private var downX = 0f
    private var downY = 0f

    init {
        setWillNotDraw(false)
    }

    private fun getPages(): List<View> {
        val list = mutableListOf<View>()
        if (childCount == 1 && getChildAt(0) is ViewGroup) {
            val container = getChildAt(0) as ViewGroup
            for (i in 0 until container.childCount) {
                list.add(container.getChildAt(i))
            }
        } else {
            for (i in 0 until childCount) {
                list.add(getChildAt(i))
            }
        }
        return list
    }

    fun getTotalPages(): Int {
        return getPages().size.coerceAtLeast(1)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(width, height)

        val childWidthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY)
        val childHeightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)

        if (childCount == 1 && getChildAt(0) is ViewGroup) {
            val container = getChildAt(0) as ViewGroup
            val count = container.childCount
            for (i in 0 until count) {
                container.getChildAt(i).measure(childWidthSpec, childHeightSpec)
            }
            container.measure(
                MeasureSpec.makeMeasureSpec(width * count, MeasureSpec.EXACTLY),
                childHeightSpec
            )
            // Повторно жестко возвращаем каждому столу ровно width x height!
            for (i in 0 until count) {
                container.getChildAt(i).measure(childWidthSpec, childHeightSpec)
            }
        } else {
            for (i in 0 until childCount) {
                getChildAt(i).measure(childWidthSpec, childHeightSpec)
            }
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val width = r - l
        val height = b - t

        if (childCount == 1 && getChildAt(0) is ViewGroup) {
            val container = getChildAt(0) as ViewGroup
            val count = container.childCount
            container.layout(0, 0, width * count, height)
            for (i in 0 until count) {
                val child = container.getChildAt(i)
                val childLeft = i * width
                child.layout(childLeft, 0, childLeft + width, height)
            }
        } else {
            for (i in 0 until childCount) {
                val child = getChildAt(i)
                val childLeft = i * width
                child.layout(childLeft, 0, childLeft + width, height)
            }
        }

        if (!scroller.isFinished) {
            scroller.abortAnimation()
        }
        val targetX = currentPage * width
        scrollTo(targetX, 0)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        val x = ev.x
        val y = ev.y

        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastX = x
                lastY = y
                downX = x
                downY = y
                isBeingDragged = !scroller.isFinished
                if (isBeingDragged) {
                    scroller.abortAnimation()
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = abs(x - downX)
                val dy = abs(y - downY)
                if (dx > touchSlop && dx > dy * 1.15f) {
                    isBeingDragged = true
                    lastX = x
                    lastY = y
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isBeingDragged = false
            }
        }
        return isBeingDragged
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain()
        }
        velocityTracker?.addMovement(ev)

        val x = ev.x
        val y = ev.y

        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (!scroller.isFinished) {
                    scroller.abortAnimation()
                }
                lastX = x
                lastY = y
                downX = x
                downY = y
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = abs(x - downX)
                val dy = abs(y - downY)
                if (!isBeingDragged && dx > touchSlop && dx > dy * 1.15f) {
                    isBeingDragged = true
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
                if (isBeingDragged) {
                    val deltaX = lastX - x
                    lastX = x

                    val maxScroll = ((getTotalPages() - 1) * width).coerceAtLeast(0)
                    val newScrollX = (scrollX + deltaX).coerceIn(-width * 0.15f, maxScroll + width * 0.15f)
                    scrollTo(newScrollX.toInt(), 0)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isBeingDragged) {
                    velocityTracker?.computeCurrentVelocity(1000)
                    val velocityX = velocityTracker?.xVelocity ?: 0f
                    snapToNearestPage(velocityX)
                    isBeingDragged = false
                } else if (!scroller.isFinished) {
                    snapToNearestPage(0f)
                }
                velocityTracker?.recycle()
                velocityTracker = null
            }
        }
        return true
    }

    private fun snapToNearestPage(velocityX: Float) {
        val pageWidth = width
        if (pageWidth <= 0) return
        val total = getTotalPages()
        val currentScrollX = scrollX

        var targetPage = Math.round(currentScrollX.toFloat() / pageWidth).coerceIn(0, total - 1)

        if (abs(velocityX) > minFlingVelocity) {
            targetPage = if (velocityX < 0) {
                (currentPage + 1).coerceAtMost(total - 1)
            } else {
                (currentPage - 1).coerceAtLeast(0)
            }
        }

        scrollToPage(targetPage, smooth = true)
    }

    fun scrollToPage(pageIndex: Int, smooth: Boolean = true) {
        val total = getTotalPages()
        val targetPage = pageIndex.coerceIn(0, total - 1)
        val targetX = targetPage * width
        val dx = targetX - scrollX

        if (smooth && width > 0) {
            val duration = (abs(dx).toFloat() / width * 260f).toInt().coerceIn(180, 320)
            scroller.startScroll(scrollX, 0, dx, 0, duration)
            postInvalidateOnAnimation()
        } else {
            scrollTo(targetX, 0)
        }

        if (currentPage != targetPage) {
            currentPage = targetPage
            onPageSelectedListener?.invoke(currentPage)
        }
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollTo(scroller.currX, scroller.currY)
            postInvalidateOnAnimation()
        }
    }
}
