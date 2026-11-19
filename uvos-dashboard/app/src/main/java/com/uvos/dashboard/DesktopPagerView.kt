package com.uvos.dashboard

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.animation.PathInterpolator
import android.widget.OverScroller
import kotlin.math.abs

/**
 * UVOS DesktopPagerView - аппаратный пагинатор столов с физикой затухающей пружины (Damped Spring):
 * - Физическая модель Damped Harmonic Oscillator (k=230, c=26, zeta ~ 0.85)
 * - Rubber-banding (упругий резиновый отскок на границах)
 * - Исключены любые изменения child.alpha для предотвращения аллокаций off-screen RenderNodeLayer на Adreno 610
 * - Жесткий контроль геометрии (ровно 100% ширины экрана на каждый стол)
 */
class DesktopPagerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    var currentPage = 0
        private set

    var onPageSelectedListener: ((Int) -> Unit)? = null
    var onScrollProgressListener: ((Float) -> Unit)? = null

    // Резервный скроллер для стандартных смещений
    private val appleSpringInterpolator = PathInterpolator(0.20f, 1.0f, 0.32f, 1.0f)
    private val scroller = OverScroller(context, appleSpringInterpolator)

    // Параметры затухающей пружины (Damped Spring Physics)
    private val springStiffness = 230f
    private val springDamping = 26f
    private var isSpringActive = false
    private var springTargetX = 0f
    private var springCurrentX = 0f
    private var springVelocityX = 0f
    private var lastFrameTimeNanos = 0L

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val minFlingVelocity = ViewConfiguration.get(context).scaledMinimumFlingVelocity * 1.2f

    private var velocityTracker: VelocityTracker? = null
    private var isBeingDragged = false
    private var lastX = 0f
    private var lastY = 0f
    private var downX = 0f
    private var downY = 0f

    init {
        setWillNotDraw(false)
    }

    fun getTotalPages(): Int {
        return childCount.coerceAtLeast(1)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(width, height)

        val childWidthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY)
        val childHeightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)

        for (i in 0 until childCount) {
            getChildAt(i).measure(childWidthSpec, childHeightSpec)
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val width = r - l
        val height = b - t

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val childLeft = i * width
            child.layout(childLeft, 0, childLeft + width, height)
            child.pivotX = width / 2f
            child.pivotY = height / 2f
        }

        if (!scroller.isFinished) {
            scroller.abortAnimation()
        }
        isSpringActive = false
        val targetX = currentPage * width
        scrollTo(targetX, 0)
        applyAppleDepthTransform(targetX)
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
                isBeingDragged = !scroller.isFinished || isSpringActive
                if (isBeingDragged) {
                    scroller.abortAnimation()
                    isSpringActive = false
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
                isSpringActive = false
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
                    var deltaX = lastX - x
                    lastX = x

                    val maxScroll = ((childCount - 1) * width).coerceAtLeast(0)
                    val currentScroll = scrollX

                    // Apple Rubber-Banding: упругое сопротивление при выходе за границы
                    if (currentScroll < 0 || currentScroll > maxScroll) {
                        deltaX *= 0.38f
                    }

                    val newScrollX = (currentScroll + deltaX).coerceIn(-width * 0.28f, maxScroll + width * 0.28f)
                    scrollTo(newScrollX.toInt(), 0)
                    applyAppleDepthTransform(newScrollX.toInt())
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isBeingDragged) {
                    velocityTracker?.computeCurrentVelocity(1000)
                    val velocityX = velocityTracker?.xVelocity ?: 0f
                    snapToNearestPage(velocityX)
                    isBeingDragged = false
                } else if (!scroller.isFinished || isSpringActive) {
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
        val total = childCount
        val currentScrollX = scrollX

        var targetPage = Math.round(currentScrollX.toFloat() / pageWidth).coerceIn(0, total - 1)

        // Мягкий флинг с учетом направления скорости
        if (abs(velocityX) > minFlingVelocity) {
            targetPage = if (velocityX < 0) {
                (currentPage + 1).coerceAtMost(total - 1)
            } else {
                (currentPage - 1).coerceAtLeast(0)
            }
        }

        scrollToPage(targetPage, smooth = true, initialVelocity = velocityX)
    }

    fun scrollToPage(pageIndex: Int, smooth: Boolean = true, initialVelocity: Float = 0f) {
        val total = childCount
        val targetPage = pageIndex.coerceIn(0, total - 1)
        val targetX = targetPage * width

        if (smooth && width > 0) {
            // Инициализация Damped Spring (затухающий гармонический осциллятор)
            springTargetX = targetX.toFloat()
            springCurrentX = scrollX.toFloat()
            springVelocityX = -initialVelocity.coerceIn(-5000f, 5000f)
            lastFrameTimeNanos = System.nanoTime()
            isSpringActive = true
            if (!scroller.isFinished) {
                scroller.abortAnimation()
            }
            postInvalidateOnAnimation()
        } else {
            isSpringActive = false
            scrollTo(targetX, 0)
            applyAppleDepthTransform(targetX)
        }

        if (currentPage != targetPage) {
            currentPage = targetPage
            onPageSelectedListener?.invoke(currentPage)
        }
    }

    override fun computeScroll() {
        if (isSpringActive) {
            val now = System.nanoTime()
            var dt = ((now - lastFrameTimeNanos) / 1_000_000_000f).coerceIn(0.0005f, 0.032f)
            lastFrameTimeNanos = now

            // Численное интегрирование пружины с затуханием (Damped Spring Physics)
            val displacement = springCurrentX - springTargetX
            val springForce = -springStiffness * displacement
            val dampingForce = -springDamping * springVelocityX
            val acceleration = springForce + dampingForce

            springVelocityX += acceleration * dt
            springCurrentX += springVelocityX * dt

            scrollTo(springCurrentX.toInt(), 0)
            applyAppleDepthTransform(springCurrentX.toInt())

            if (abs(displacement) < 0.5f && abs(springVelocityX) < 15f) {
                isSpringActive = false
                scrollTo(springTargetX.toInt(), 0)
                applyAppleDepthTransform(springTargetX.toInt())
            } else {
                postInvalidateOnAnimation()
            }
        } else if (scroller.computeScrollOffset()) {
            scrollTo(scroller.currX, scroller.currY)
            applyAppleDepthTransform(scroller.currX)
            postInvalidateOnAnimation()
        }
    }

    /**
     * Плавная Apple-трансформация столов при листании:
     * - Легкое масштабирование уходящего стола (Depth Scale 1.0 -> 0.955)
     * - child.alpha ИСКЛЮЧЕН: на Adreno 610 прозрачность сложных контейнеров (>150 View)
     *   провоцирует создание off-screen RenderNodeLayer текстур в VRAM и дропы до 60 Гц.
     */
    private fun applyAppleDepthTransform(scrollXPos: Int) {
        val pageWidth = width.toFloat()
        if (pageWidth <= 0f) return

        val scrollProgress = scrollXPos.toFloat() / pageWidth
        onScrollProgressListener?.invoke(scrollProgress)

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val pageOffset = (i * pageWidth - scrollXPos) / pageWidth
            val absOffset = abs(pageOffset)

            if (absOffset <= 1.0f) {
                val scale = 1.0f - (0.045f * absOffset)
                child.scaleX = scale
                child.scaleY = scale
            } else {
                child.scaleX = 0.955f
                child.scaleY = 0.955f
            }
            if (child.alpha != 1.0f) {
                child.alpha = 1.0f
            }
        }
    }
}
