package com.uvos.dashboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * SafeDialView - механический вращающийся лимб сейфа UVOS.
 *
 * Особенности:
 * - Круговой лимб на 40 делений (0..39).
 * - Вращение пальцем с расчетом углового смещения.
 * - Тактильный отклик (Haptic Tick) при прохождении каждого деления.
 * - Указатель на 12 часов (верхняя риска).
 * - Нажатие на центральную ручку подтверждает текущую цифру.
 */
class SafeDialView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val totalNumbers = 40
    private val degreesPerUnit = 360f / totalNumbers

    // Текущий угол поворота лимба в градусах
    private var rotationAngle = 0f
    private var lastTouchAngle = 0f
    private var isDragging = false
    private var lastVibratedNumber = -1

    var onNumberChanged: ((Int) -> Unit)? = null
    var onNumberConfirmed: ((Int) -> Unit)? = null

    // Краски
    private val paintBezel = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintDialFace = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintKnob = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintKnobRim = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintTicks = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintText = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintPointer = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintCenterText = Paint(Paint.ANTI_ALIAS_FLAG)

    private val rectBezel = RectF()
    private val pathPointer = Path()

    init {
        paintBezel.color = Color.parseColor("#08090D")
        paintBezel.style = Paint.Style.FILL

        paintDialFace.color = Color.parseColor("#13151D")
        paintDialFace.style = Paint.Style.FILL

        paintKnob.color = Color.parseColor("#1A1D27")
        paintKnob.style = Paint.Style.FILL

        paintKnobRim.color = Color.parseColor("#2E3344")
        paintKnobRim.style = Paint.Style.STROKE
        paintKnobRim.strokeWidth = 3f

        paintTicks.color = Color.parseColor("#E8E4D8")
        paintTicks.style = Paint.Style.STROKE
        paintTicks.strokeCap = Paint.Cap.ROUND

        paintText.color = Color.parseColor("#E8E4D8")
        paintText.textAlign = Paint.Align.CENTER
        paintText.isFakeBoldText = true

        paintPointer.color = Color.parseColor("#E02020")
        paintPointer.style = Paint.Style.FILL

        paintCenterText.color = Color.parseColor("#06B6D4")
        paintCenterText.textAlign = Paint.Align.CENTER
        paintCenterText.isFakeBoldText = true
    }

    /**
     * Получить число, находящееся сейчас ровно под верхним указателем.
     */
    fun getCurrentNumber(): Int {
        val normalized = ((-rotationAngle % 360f) + 360f) % 360f
        return (normalized / degreesPerUnit).roundToInt() % totalNumbers
    }

    /**
     * Программно повернуть лимб к заданному числу.
     */
    fun setNumber(number: Int) {
        val targetNumber = number.coerceIn(0, totalNumbers - 1)
        rotationAngle = -targetNumber * degreesPerUnit
        invalidate()
        onNumberChanged?.invoke(targetNumber)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f
        val radius = (minOf(width, height) / 2f) * 0.95f

        // 1. Внешний безель с винтами
        rectBezel.set(cx - radius, cy - radius, cx + radius, cy + radius)
        canvas.drawCircle(cx, cy, radius, paintBezel)

        // Декоративные металлические винты по окружности безеля
        val screwCount = 8
        val screwPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2A2E3D")
            style = Paint.Style.FILL
        }
        val screwSlotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0A0B0F")
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }
        for (i in 0 until screwCount) {
            val angle = (i * 360f / screwCount) * PI / 180f
            val sx = cx + (radius * 0.93f) * cos(angle).toFloat()
            val sy = cy + (radius * 0.93f) * sin(angle).toFloat()
            canvas.drawCircle(sx, sy, radius * 0.035f, screwPaint)
            canvas.drawLine(sx - 4, sy, sx + 4, sy, screwSlotPaint)
        }

        // 2. Вращающийся диск с делениями и цифрами
        val dialRadius = radius * 0.85f
        canvas.save()
        canvas.rotate(rotationAngle, cx, cy)
        canvas.drawCircle(cx, cy, dialRadius, paintDialFace)

        paintText.textSize = dialRadius * 0.11f

        for (i in 0 until totalNumbers) {
            val tickAngleDeg = i * degreesPerUnit - 90f // 0 начинается на 12 часов
            val tickAngleRad = tickAngleDeg * PI / 180f
            val isMajor = i % 5 == 0

            val outerR = dialRadius * 0.95f
            val innerR = if (isMajor) dialRadius * 0.78f else dialRadius * 0.86f

            paintTicks.strokeWidth = if (isMajor) 3.5f else 1.8f
            paintTicks.color = if (isMajor) Color.parseColor("#E8E4D8") else Color.parseColor("#71768E")

            val x1 = cx + outerR * cos(tickAngleRad).toFloat()
            val y1 = cy + outerR * sin(tickAngleRad).toFloat()
            val x2 = cx + innerR * cos(tickAngleRad).toFloat()
            val y2 = cy + innerR * sin(tickAngleRad).toFloat()

            canvas.drawLine(x1, y1, x2, y2, paintTicks)

            // Подпись каждого 5-го деления (0, 5, 10, ...)
            if (isMajor) {
                val textR = dialRadius * 0.65f
                val tx = cx + textR * cos(tickAngleRad).toFloat()
                val ty = cy + textR * sin(tickAngleRad).toFloat() + (paintText.textSize * 0.35f)
                canvas.drawText(i.toString(), tx, ty, paintText)
            }
        }
        canvas.restore()

        // 3. Центральная рифленая ручка
        val knobRadius = dialRadius * 0.44f
        canvas.drawCircle(cx, cy, knobRadius, paintKnob)
        canvas.drawCircle(cx, cy, knobRadius, paintKnobRim)

        // Рифление центрального диска
        val knurlPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#252A38")
            strokeWidth = 1.5f
            style = Paint.Style.STROKE
        }
        for (r in listOf(0.7f, 0.85f)) {
            canvas.drawCircle(cx, cy, knobRadius * r, knurlPaint)
        }

        // Число в центре ручки
        val currentNum = getCurrentNumber()
        paintCenterText.textSize = knobRadius * 0.55f
        canvas.drawText(
            String.format("%02d", currentNum),
            cx,
            cy + (paintCenterText.textSize * 0.35f),
            paintCenterText
        )

        // 4. Верхний неподвижный указатель (Красный треугольник на 12 часов)
        val pointerTop = cy - radius * 0.88f
        val pointerSize = radius * 0.09f
        pathPointer.reset()
        pathPointer.moveTo(cx, pointerTop + pointerSize) // острие вниз
        pathPointer.lineTo(cx - pointerSize * 0.6f, pointerTop)
        pathPointer.lineTo(cx + pointerSize * 0.6f, pointerTop)
        pathPointer.close()
        canvas.drawPath(pathPointer, paintPointer)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val cx = width / 2f
        val cy = height / 2f
        val x = event.x
        val y = event.y

        val dx = x - cx
        val dy = y - cy
        val dist = kotlin.math.sqrt(dx * dx + dy * dy)
        val knobRadius = (minOf(width, height) / 2f) * 0.85f * 0.44f

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // Если тапнули прямо по центру ручки — подтверждение цифры
                if (dist <= knobRadius) {
                    performHapticClick(true)
                    onNumberConfirmed?.invoke(getCurrentNumber())
                    return true
                }
                lastTouchAngle = calculateAngleDegrees(dx, dy)
                isDragging = true
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    val currentAngle = calculateAngleDegrees(dx, dy)
                    var delta = currentAngle - lastTouchAngle

                    // Коррекция перехода через ±180°
                    if (delta > 180f) delta -= 360f
                    if (delta < -180f) delta += 360f

                    rotationAngle = (rotationAngle + delta) % 360f
                    lastTouchAngle = currentAngle
                    invalidate()

                    val currentNum = getCurrentNumber()
                    if (currentNum != lastVibratedNumber) {
                        performHapticClick(false)
                        lastVibratedNumber = currentNum
                        onNumberChanged?.invoke(currentNum)
                    }
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    isDragging = false
                    // Мягкая доводка до ближайшего целого деления
                    val currentNum = getCurrentNumber()
                    rotationAngle = -currentNum * degreesPerUnit
                    invalidate()
                    onNumberChanged?.invoke(currentNum)
                    parent?.requestDisallowInterceptTouchEvent(false)
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun calculateAngleDegrees(dx: Float, dy: Float): Float {
        val rad = atan2(dy.toDouble(), dx.toDouble())
        val deg = Math.toDegrees(rad).toFloat()
        return (deg + 360f) % 360f
    }

    private fun performHapticClick(heavy: Boolean) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val duration = if (heavy) 25L else 5L
                    val amp = if (heavy) VibrationEffect.DEFAULT_AMPLITUDE else 80
                    vibrator.vibrate(VibrationEffect.createOneShot(duration, amp))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(if (heavy) 25L else 5L)
                }
            }
        } catch (_: Exception) {
            // Игнорируем ошибки вибромотора
        }
    }
}
